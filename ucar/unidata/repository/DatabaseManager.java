/**
 * $Id: v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.repository;


import org.w3c.dom.*;
import ucar.unidata.repository.type.*;




import ucar.unidata.sql.Clause;
import ucar.unidata.sql.SqlUtil;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;




import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

import org.apache.commons.dbcp.BasicDataSource;
import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;





/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DatabaseManager extends RepositoryManager implements SqlUtil.ConnectionManager {

    /** _more_ */
    private static final int TIMEOUT = 5000;

    /** _more_ */
    private String db;

    /** _more_ */
    private static final String DB_MYSQL = "mysql";

    /** _more_ */
    private static final String DB_DERBY = "derby";

    /** _more_ */
    private static final String DB_POSTGRES = "postgres";

    private DataSource dataSource;

    /** Keeps track of active connections */
    private Hashtable<Connection,ConnectionInfo> connectionMap = new Hashtable<Connection,ConnectionInfo>();

    private List<String> scourMessages = new ArrayList<String>();

    /**
     * _more_
     *
     * @param repository _more_
     */
    public DatabaseManager(Repository repository) {
        super(repository);
        db = (String) getRepository().getProperty(PROP_DB);
        if (db == null) {
            throw new IllegalStateException("Must have a " + PROP_DB
                                            + " property defined");
        }
    }


    public void checkConnections() {
        while(true) {
            try {
                Misc.sleep(5000);
                Hashtable<Connection,ConnectionInfo> tmp = new Hashtable<Connection,ConnectionInfo>();
                synchronized(connectionMap) {
                    tmp.putAll(connectionMap);
                }
                long now = System.currentTimeMillis();
                int seconds = getRepository().getProperty(PROP_DB_POOL_TIMEUNTILCLOSED,60);
                for (Enumeration keys = tmp.keys(); keys.hasMoreElements(); ) {
                    Connection connection = (Connection) keys.nextElement();
                    ConnectionInfo info  = tmp.get(connection);
                    //If a connection has been out for more than a minute then close it
                    if(now-info.time > seconds*1000) {
                        logInfo ("A connection has been open for more than "+seconds +" seconds:\n" + info.where);
                        synchronized(scourMessages) {
                            while(scourMessages.size()>100)
                                scourMessages.remove(0);
                            scourMessages.add("SCOURED @"+new Date() +" info.date: " +new Date(info.time) +" Where:" +info.where);
                        }
                        closeConnection(connection);
                    }
                }
            } catch(Exception exc) {
                scourMessages.add("Error checking connection:" + exc);
            }
        }
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void init() throws Exception {
        SqlUtil.setConnectionManager(this);
        if(dataSource!=null) return;
        dataSource = doMakeDataSource();
        BasicDataSource bds  = (BasicDataSource) dataSource;
        if (db.equals(DB_MYSQL)) {
            Statement statement = getConnection().createStatement();
            statement.execute("set time_zone = '+0:00'");
            closeAndReleaseConnection(statement);
        }
        Misc.run(this,"checkConnections", null);
    }


    protected DataSource doMakeDataSource() throws Exception {
        BasicDataSource ds = new BasicDataSource();
        ds.setMaxActive(getRepository().getProperty(PROP_DB_POOL_MAXACTIVE,100));
        ds.setMaxIdle(getRepository().getProperty(PROP_DB_POOL_MAXIDLE,100));

        //        ds.setValidationQuery("select * from dummy");
        String userName = (String) getRepository().getProperty(
                              PROP_DB_USER.replace("${db}", db));
        String password = (String) getRepository().getProperty(
                              PROP_DB_PASSWORD.replace("${db}", db));
        String connectionURL =
            (String) getRepository().getProperty(PROP_DB_URL.replace("${db}",
                db));
        String driverClassName =  (String) getRepository().getProperty(
                                                                       PROP_DB_DRIVER.replace("${db}", db));
        Misc.findClass(driverClassName);


        ds.setDriverClassName(driverClassName);
        ds.setUsername(userName);
        ds.setPassword(password);
        ds.setUrl(connectionURL);
        return ds;
    }



    public void addStatistics(Request request,StringBuffer dbSB) throws Exception {

	BasicDataSource bds = (BasicDataSource)dataSource;

	StringBuffer msgb = new StringBuffer();
	synchronized(scourMessages) {
	    for(String msg: scourMessages) {
		msgb.append("<pre>" + msg +"</pre>");
		msgb.append("<hr>");
	    }
	    if(scourMessages.size()>0) {
		dbSB.append(HtmlUtil.makeShowHideBlock(msg("Connection Problems"),
						       msgb.toString(), false));
	    }
	}


	dbSB.append("Connection Pool:<br>&nbsp;&nbsp;#active:" + bds.getNumActive() +"<br>&nbsp;&nbsp;#idle:" +bds.getNumIdle()  +"<br>&nbsp;&nbsp;max active: " +
		    bds.getMaxActive() +"<br>&nbsp;&nbsp;max idle:" + bds.getMaxIdle());
	dbSB.append(HtmlUtil.br());
        dbSB.append("<table>\n");
        String[] names = { msg("Users"), msg("Associations"),
                           msg("Metadata Items") };
        String[] tables = { Tables.USERS.NAME, Tables.ASSOCIATIONS.NAME,
                            Tables.METADATA.NAME };
        for (int i = 0; i < tables.length; i++) {
            dbSB.append(HtmlUtil.row(HtmlUtil.cols(""
                    + getDatabaseManager().getCount(tables[i].toLowerCase(),
                        new Clause()), names[i])));
        }


        dbSB.append(
            HtmlUtil.row(
                HtmlUtil.colspan(HtmlUtil.bold(msgLabel("Types")), 2)));
        int total = 0;
        dbSB.append(HtmlUtil.row(HtmlUtil.cols(""
                + getDatabaseManager().getCount(Tables.ENTRIES.NAME,
                    new Clause()), msg("Total entries"))));
        for (TypeHandler typeHandler : getRepository().getTypeHandlers()) {
            if (typeHandler.isType(TypeHandler.TYPE_ANY)) {
                continue;
            }
            int cnt = getCount(Tables.ENTRIES.NAME,
			       Clause.eq("type", typeHandler.getType()));

            String url =
                HtmlUtil.href(
                    request.url(
                        getRepository().URL_SEARCH_FORM, ARG_TYPE,
                        typeHandler.getType()), typeHandler.getLabel());
            dbSB.append(HtmlUtil.row(HtmlUtil.cols("" + cnt, url)));
        }


        dbSB.append("</table>\n");
    }




    /**
     * Class ConnectionWrapper _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class ConnectionInfo {

        /** _more_ */
        long time;

        String where;

        /**
         * _more_
         *
         * @param connection _more_
         */
        ConnectionInfo() {
            this.time  = System.currentTimeMillis();
            where = Misc.getStackTrace();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */



    /**
     * _more_
     *
     * @param query _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public PreparedStatement getPreparedStatement(String query)
            throws Exception {
        return  getConnection().prepareStatement(query);
    }


    /**
     * _more_
     *
     * @param table _more_
     * @param colId _more_
     * @param id _more_
     * @param names _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void update(String table, String colId, String id, String[] names,
                       Object[] values)
            throws Exception {
	PreparedStatement statement       = getPreparedStatement(SqlUtil.makeUpdate(table, colId, names));
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value == null) {
                statement.setNull(i + 1, java.sql.Types.VARCHAR);
            } else if (value instanceof Date) {
                setDate(statement, i + 1, (Date) value);
            } else if (value instanceof Boolean) {
                boolean b = ((Boolean) value).booleanValue();
                statement.setInt(i + 1, (b
                                    ? 1
                                    : 0));
            } else {
                statement.setObject(i + 1, value);
            }
        }
        statement.setString(values.length + 1, id);
        statement.execute();
        closeAndReleaseConnection(statement);
    }

    /**
     * _more_
     *
     * @param table _more_
     * @param clause _more_
     * @param names _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void update(String table, Clause clause, String[] names,
                       Object[] values)
            throws Exception {
        Connection connection = getConnection();
	try {
	    SqlUtil.update(connection, table, clause, names, values);
	} finally {
	    closeConnection(connection);
	}
    }




    public Statement createStatement() throws Exception {
        return getConnection().createStatement();
    }


    /**
     * _more_
     *
     * @param table _more_
     * @param clause _more_
     *
     * @throws Exception _more_
     */
    public void delete(String table, Clause clause) throws Exception {
        Connection connection = getConnection();
	try {
	    SqlUtil.delete(connection, table, clause);
	} finally {
	    closeConnection(connection);
	}
    }



    public void shutdown() throws Exception {
        //TODO: Close the datasource
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean hasConnection() throws Exception {
        Connection connection = getConnection();
        boolean    connected  = connection != null;
        closeConnection(connection);
        return connected;
    }



    /**
     * _more_
     *
     *
     * @param makeNewOne _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getConnection() throws Exception {
        Connection connection = dataSource.getConnection();
	synchronized(connectionMap) {
	    connectionMap.put(connection,new ConnectionInfo());
	}
        return connection;
    }




    /**
     * _more_
     *
     * @param connection _more_
     */
    public void closeConnection(Connection connection) {
        try {
	    synchronized(connectionMap) {
		connectionMap.remove(connection);
	    }
            connection.setAutoCommit(true);
            connection.close();
            BasicDataSource bds = (BasicDataSource)dataSource;
            if(bds.getNumActive()>3) {
                //                System.err.println("closeConnection  active:" + bds.getNumActive() +" idle:" +bds.getNumIdle() +" max: " +
                //                                   bds.getMaxActive() +" " + bds.getMaxIdle());
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }



    /**
     * _more_
     *
     * @param statement _more_
     */
    public void closeStatement(Statement statement) {
        if(statement==null) return;
        try {
            statement.close();
        } catch (Exception ignore) {}
    }



    public void closeAndReleaseConnection(Statement statement) throws SQLException {
        if(statement==null) return;
        Connection connection = null;
        try {
            connection = statement.getConnection();
            statement.close();
        } catch (Throwable ignore) {}

        if(connection!=null) {
            closeConnection(connection);
        } else {
	    synchronized(scourMessages) {
		while(scourMessages.size()>100)
		    scourMessages.remove(0);
		scourMessages.add("Trying to close a statement with no connection:" +LogUtil.getStackTrace());
	    }
        }
    }


    /**
     * _more_
     *
     * @param table _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int getCount(String table, Clause clause) throws Exception {
        Statement statement = select("count(*)", table, clause);
        ResultSet results   = statement.getResultSet();
        int       result;
        if ( !results.next()) {
            result = 0;
        } else {
            result = results.getInt(1);
        }
        closeAndReleaseConnection(statement);
        return result;
    }




    /**
     * _more_
     *
     * @param sb _more_
     */
    protected void addInfo(StringBuffer sb) {
        String dbUrl = "" + (String) getRepository().getProperty(
                           PROP_DB_URL.replace("${db}", db));
        sb.append(HtmlUtil.formEntry("Database:", db));
        sb.append(HtmlUtil.formEntry("JDBC URL:", dbUrl));
    }



    /**
     * _more_
     *
     * @param os _more_
     * @param all _more_
     *
     * @throws Exception _more_
     */
    public void makeDatabaseCopy(OutputStream os, boolean all)
            throws Exception {
        Connection connection = getConnection();
        try {
            DatabaseMetaData dbmd     = connection.getMetaData();
            ResultSet        catalogs = dbmd.getCatalogs();
            ResultSet tables = dbmd.getTables(null, null, null,
                                   new String[] { "TABLE" });

            int totalRowCnt = 0;
            while (tables.next()) {
                String tableName = tables.getString("Tables.NAME.NAME");
                String tableType = tables.getString("Tables.TYPE.NAME");
                if ((tableType == null) || Misc.equals(tableType, "INDEX")
                        || tableType.startsWith("SYSTEM")) {
                    continue;
                }


                String tn = tableName.toLowerCase();
                if ( !all) {
                    if (tn.equals(Tables.GLOBALS.NAME)
                            || tn.equals(Tables.USERS.NAME)
                            || tn.equals(Tables.PERMISSIONS.NAME)
                            || tn.equals(Tables.HARVESTERS.NAME)
                            || tn.equals(Tables.USERROLES.NAME)) {
                        continue;
                    }
                }


                ResultSet cols = dbmd.getColumns(null, null, tableName, null);

                int       colCnt   = 0;

                String    colNames = null;
                List      types    = new ArrayList();
                while (cols.next()) {
                    String colName = cols.getString("COLUMN_NAME");
                    if (colNames == null) {
                        colNames = " (";
                    } else {
                        colNames += ",";
                    }
                    colNames += colName;
                    int type = cols.getInt("DATA_TYPE");
                    types.add(type);
                    colCnt++;
                }
                colNames += ") ";

                Statement statement = execute("select * from " + tableName,
                                         10000000, 0);
                SqlUtil.Iterator iter = getIterator(statement);
                ResultSet        results;
                int              rowCnt    = 0;
                List             valueList = new ArrayList();
                boolean          didDelete = false;
                while ((results = iter.next()) != null) {
                    while (results.next()) {
                        if ( !didDelete) {
                            didDelete = true;
                            IOUtil.write(os,
                                         "delete from  "
                                         + tableName.toLowerCase() + ";\n");
                        }
                        totalRowCnt++;
                        rowCnt++;
                        StringBuffer value = new StringBuffer("(");
                        for (int i = 1; i <= colCnt; i++) {
                            int type = ((Integer) types.get(i
                                           - 1)).intValue();
                            if (i > 1) {
                                value.append(",");
                            }
                            if (type == java.sql.Types.TIMESTAMP) {
                                Timestamp ts = results.getTimestamp(i);
                                //                            sb.append(SqlUtil.format(new Date(ts.getTime())));
                                value.append(HtmlUtil.squote(ts.toString()));
                            } else if (type == java.sql.Types.VARCHAR) {
                                String s = results.getString(i);
                                if (s != null) {
                                    //If the target isn't mysql:
                                    //s = s.replace("'", "''");
                                    //If the target is mysql:
                                    s = s.replace("'", "\\'");
                                    s = s.replace("\r", "\\r");
                                    s = s.replace("\n", "\\n");
                                    value.append("'" + s + "'");
                                } else {
                                    value.append("null");
                                }
                            } else {
                                String s = results.getString(i);
                                value.append(s);
                            }
                        }
                        value.append(")");
                        valueList.add(value.toString());
                        if (valueList.size() > 50) {
                            IOUtil.write(os,
                                         "insert into "
                                         + tableName.toLowerCase() + colNames
                                         + " values ");
                            IOUtil.write(os, StringUtil.join(",", valueList));
                            IOUtil.write(os, ";\n");
                            valueList = new ArrayList();
                        }
                    }
                }
                if (valueList.size() > 0) {
                    if ( !didDelete) {
                        didDelete = true;
                        IOUtil.write(os,
                                     "delete from  "
                                     + tableName.toLowerCase() + ";\n");
                    }
                    IOUtil.write(os,
                                 "insert into " + tableName.toLowerCase()
                                 + colNames + " values ");
                    IOUtil.write(os, StringUtil.join(",", valueList));
                    IOUtil.write(os, ";\n");
                }
            }
        } finally {
            closeConnection(connection);
        }

    }


    public SqlUtil.Iterator getIterator(Statement statement) {
        return  new Iterator(this, statement);
    }



    /**
     * _more_
     *
     * @param connection _more_
     *
     * @throws Exception _more_
     */
    protected void initConnection(Connection connection) throws Exception {
        if (db.equals(DB_MYSQL)) {
            Statement statement = connection.createStatement();
            statement.execute("set time_zone = '+0:00'");
            closeStatement(statement);
        }
    }




    public void executeAndClose(String sql)
            throws Exception {
        executeAndClose(sql,10000000,0);
    }





    public void executeAndClose(String sql, int max, int timeout)
        throws Exception {
        Connection connection = getConnection();
        try {
            Statement statement= execute(connection, sql, max, timeout);
            closeStatement(statement);
        } finally {
            closeConnection(connection);
        }
    }


    /**
     * _more_
     *
     * @param sql _more_
     * @param max _more_
     * @param timeout _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement execute(String sql, int max, int timeout)
            throws Exception {
        return execute(getConnection(), sql, max, timeout);
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param sql _more_
     * @param max _more_
     * @param timeout _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement execute(Connection connection, String sql, int max,
                             int timeout)
            throws Exception {
        Statement statement = connection.createStatement();
        if (timeout > 0) {
            statement.setQueryTimeout(timeout);
        }

        if (max > 0) {
            statement.setMaxRows(max);
        }

        long t1 = System.currentTimeMillis();
        try {
            statement.execute(sql);
        } catch (Exception exc) {
            //            logError("Error executing sql:" + sql, exc);
            throw exc;
        }
        long t2 = System.currentTimeMillis();
        if (getRepository().debug || (t2 - t1 > 300)) {
            logInfo("query took:" + (t2 - t1) + " " + sql);
        }
        if (t2 - t1 > 2000) {
            //            Misc.printStack("query:" + sql);
        }
        return statement;
    }





    /**
     * _more_
     *
     * @param oldTable _more_
     * @param newTable _more_
     * @param connection _more_
     *
     * @throws Exception _more_
     */
    public void copyTable(String oldTable, String newTable,
                          Connection connection)
            throws Exception {
        String copySql = "INSERT INTO  " + newTable + " SELECT * from "
                         + oldTable;
        execute(connection, copySql, -1, -1);
    }



    /**
     * _more_
     *
     * @param statement _more_
     * @param col _more_
     * @param date _more_
     *
     * @throws Exception _more_
     */
    public void setTimestamp(PreparedStatement statement, int col, Date date)
            throws Exception {
        if (date == null) {
            statement.setTimestamp(col, null);
        } else {
            statement.setTimestamp(col, new java.sql.Timestamp(date.getTime()),
                              Repository.calendar);
        }
    }


    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getTimestamp(ResultSet results, int col) throws Exception {
        Date date = results.getTimestamp(col, Repository.calendar);
        if (date != null) {
            return date;
        }
        return new Date();
    }



    /**
     * _more_
     *
     * @param statement _more_
     * @param col _more_
     * @param time _more_
     *
     * @throws Exception _more_
     */
    public void setDate(PreparedStatement statement, int col, long time)
            throws Exception {
        setDate(statement, col, new Date(time));
    }


    /**
     * _more_
     *
     * @param statement _more_
     * @param col _more_
     * @param date _more_
     *
     * @throws Exception _more_
     */
    public void setDate(PreparedStatement statement, int col, Date date)
            throws Exception {
        //        if (!db.equals(DB_MYSQL)) {
        if (true || !db.equals(DB_MYSQL)) {
            setTimestamp(statement, col, date);
        } else {
            if (date == null) {
                statement.setTime(col, null);
            } else {
                statement.setTime(col, new java.sql.Time(date.getTime()),
                             Repository.calendar);
            }
        }
    }


    /**
     * _more_
     *
     * @param results _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Date getDate(ResultSet results, int col) throws Exception {
        //        if (!db.equals(DB_MYSQL)) {
        if (true || !db.equals(DB_MYSQL)) {
            return getTimestamp(results, col);
        }
        Date date = results.getTime(col, Repository.calendar);
        if (date != null) {
            return date;
        }
        return new Date();
    }



    /**
     * _more_
     *
     * @param statement _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void setValues(PreparedStatement statement, Object[] values)
            throws Exception {
        setValues(statement, values, 1);
    }

    /**
     * _more_
     *
     * @param statement _more_
     * @param values _more_
     * @param startIdx _more_
     *
     * @throws Exception _more_
     */
    public void setValues(PreparedStatement statement, Object[] values,
                          int startIdx)
            throws Exception {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                statement.setNull(i + startIdx, java.sql.Types.VARCHAR);
            } else if (values[i] instanceof Date) {
                setDate(statement, i + startIdx, (Date) values[i]);
            } else if (values[i] instanceof Boolean) {
                boolean b = ((Boolean) values[i]).booleanValue();
                statement.setInt(i + startIdx, (b
                                           ? 1
                                           : 0));
            } else {
                statement.setObject(i + startIdx, values[i]);
            }
        }
    }


    /**
     * _more_
     *
     * @param insert _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void executeInsert(String insert, Object[] values)
            throws Exception {
        List<Object[]> valueList = new ArrayList<Object[]>();
        valueList.add(values);
        executeInsert(insert, valueList);
    }



    /**
     * _more_
     *
     * @param insert _more_
     *
     * @throws Exception _more_
     */
    public void executeInsert(String insert,
                              List<Object[]> valueList)
            throws Exception {
        PreparedStatement pstatement = getPreparedStatement(insert);
        for(Object[] values: valueList) {
            setValues(pstatement, values);
            try {
                pstatement.executeUpdate();
            } catch (Exception exc) {
                logError("Error:" + insert, exc);
            }
        }
        closeAndReleaseConnection(pstatement);
    }

    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     */
    public String convertSql(String sql) {
        if (db.equals(DB_MYSQL)) {
            sql = sql.replace("ramadda.double", "double");
            sql = sql.replace("ramadda.datetime", "datetime");
            sql = sql.replace("ramadda.clob", "mediumtext");
            sql = sql.replace("ramadda.bigclob", "longtext");
            //sql = sql.replace("ramadda.datetime", "timestamp");
        } else if (db.equals(DB_DERBY)) {
            sql = sql.replace("ramadda.double", "double");
            sql = sql.replace("ramadda.datetime", "timestamp");
            sql = sql.replace("ramadda.clob", "clob(64000)");
            sql = sql.replace("ramadda.bigclob", "clob(256000)");
        } else if (db.equals(DB_POSTGRES)) {
            //TODO: handle ramadda.clob and ramadda.bigclob
            sql = sql.replace("ramadda.double", "float8");
            sql = sql.replace("ramadda.datetime", "timestamp");
        }
        return sql;
    }

    public void loadSql(String sql, 
                               boolean ignoreErrors, boolean printStatus)
            throws Exception {
        Connection connection = getConnection();
	try {
        connection.setAutoCommit(false);
        Statement  statement       = connection.createStatement();
        SqlUtil.loadSql(sql, statement, ignoreErrors, printStatus);
        connection.commit();
        connection.setAutoCommit(true);
        closeStatement(statement);
	} finally {
	    closeConnection(connection);
	}
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public String escapeString(String value) {
        if (db.equals(DB_MYSQL)) {
            value = value.replace("'", "\\'");
        } else {
            value = value.replace("'", "''");
        }
        return value;
    }


    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public String convertType(String type) {
        return convertType(type, -1);
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param size _more_
     *
     * @return _more_
     */
    public String convertType(String type, int size) {
        if (type.equals("clob")) {
            if (db.equals(DB_DERBY)) {
                return "clob(" + size + ") ";
            }
            if (db.equals(DB_MYSQL)) {
                return "mediumtext";
            }
            if (db.equals(DB_POSTGRES)) {
                //TODO:
                return "clob";
            }

        }
        if (type.equals("double")) {
            if (db.equals(DB_POSTGRES)) {
                return "float8";
            }
        } else if (type.equals("float8")) {
            if (db.equals(DB_MYSQL) || db.equals(DB_DERBY)) {
                return "double";
            }

        }
        return type;
    }


    /**
     * _more_
     *
     * @param skip _more_
     * @param max _more_
     *
     * @return _more_
     */
    public String getLimitString(int skip, int max) {
        if (skip < 0) {
            skip = 0;
        }
        if (max < 0) {
            max = DB_MAX_ROWS;
        }
        if (db.equals(DB_MYSQL)) {
            return " LIMIT " + max + " OFFSET " + skip + " ";
        } else if (db.equals(DB_DERBY)) {
            return " OFFSET " + skip + " ROWS ";
        } else if (db.equals(DB_POSTGRES)) {
            return " LIMIT " + max + " OFFSET " + skip + " ";
        }
        return "";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean canDoSelectOffset() {
        if (db.equals(DB_MYSQL)) {
            return true;
        } else if (db.equals(DB_DERBY)) {
            return true;
        } else if (db.equals(DB_POSTGRES)) {
            return true;
        }
        return false;
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param clause _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, String table, Clause clause,
                            String extra)
            throws Exception {
        return select(what, Misc.newList(table), clause, extra, -1);
    }



    /**
     * Class SelectInfo _more_
     *
     *
     * @author IDV Development Team
     */
    private static class SelectInfo {

        /** _more_ */
        long time;

        /** _more_ */
        String what;

        /** _more_ */
        List tables;

        /** _more_ */
        Clause clause;

        /** _more_ */
        String extra;

        /** _more_ */
        int max;

        /**
         * _more_
         *
         * @param what _more_
         * @param tables _more_
         * @param clause _more_
         * @param extra _more_
         * @param max _more_
         */
        public SelectInfo(String what, List tables, Clause clause,
                          String extra, int max) {
            time        = System.currentTimeMillis();
            this.what   = what;
            this.tables = tables;
            this.clause = clause;
            this.extra  = extra;
            this.max    = max;
        }

    }


    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param extra _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(final String what, final List tables,
                            final Clause clause, final String extra,
                            final int max)
            throws Exception {
        SelectInfo selectInfo = new SelectInfo(what, tables, clause, extra,
                                    max);
        final boolean[] done = { false };
        /*
        Misc.run(new Runnable() {
                public void run() {
                    //Wait 20 seconds
                    Misc.sleep(1000*10);
                    if(!done[0]) {
                        System.err.println("Select is taking too long\nwhat:" + what + "\ntables:" +
                                           tables +
                                           "\nclause:" + clause +
                                           "\nextra:" + extra+
                                           "max:" + max);
                        Misc.printStack("select",20);
                    }
                }
            });
        */
        Statement statement = SqlUtil.select(getConnection(), what, tables, clause,
                                        extra, max, TIMEOUT);

        done[0] = true;
        return statement;
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, String table, Clause clause)
            throws Exception {
        return select(what, Misc.newList(table), ((clause == null)
                ? null
                : new Clause[] { clause }));
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param clauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, String table, Clause[] clauses)
            throws Exception {
        return select(what, Misc.newList(table), clauses);
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param clauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, String table, List<Clause> clauses)
            throws Exception {
        return select(what, Misc.newList(table), Clause.toArray(clauses));
    }

    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param clauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement select(String what, List tables, Clause[] clauses)
            throws Exception {
        return select(what, tables, Clause.and(clauses), null, -1);
    }




    /**
     * _more_
     *
     * @param id _more_
     * @param tableName _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected boolean tableContains(String id, String tableName,
                                    String column)
            throws Exception {
        Statement statement = select(column, tableName,
                                     Clause.eq(column, id));

        ResultSet results = statement.getResultSet();
        boolean   result  = results.next();
        closeAndReleaseConnection(statement);
        return result;
    }



    public static class Iterator extends SqlUtil.Iterator {
        Statement statement;
        DatabaseManager databaseManager;
        
        public Iterator(DatabaseManager databaseManager, Statement statement) {
            super(statement);
            this.statement = statement;
            this.databaseManager = databaseManager;
        }

        protected void close(Statement statement) throws SQLException {
            databaseManager.closeAndReleaseConnection(statement);
        }

    }

}

