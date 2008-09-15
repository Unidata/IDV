/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.sql.Clause;
import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;

import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.*;

import java.io.*;

import java.io.File;
import java.io.InputStream;

import java.lang.reflect.*;



import java.net.*;

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


import javax.swing.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DatabaseManager extends RepositoryManager {

    /** _more_ */
    private String db;

    /** _more_ */
    private Connection theConnection;

    /** _more_          */
    private int connectionCnt = 0;

    /** _more_          */
    private Object CONNECTION_MUTEX = new Object();


    /** _more_ */
    private static final String DB_MYSQL = "mysql";

    /** _more_ */
    private static final String DB_DERBY = "derby";

    /** _more_ */
    private static final String DB_POSTGRES = "postgres";

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


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void init() throws Exception {
        releaseConnection(getConnection());
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getConnection() throws Exception {
        return getConnection(false);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getNewConnection() throws Exception {
        return getConnection(true);
    }


    /** _more_          */
    private List<ConnectionWrapper> connectionsToClose =
        new ArrayList<ConnectionWrapper>();

    /**
     * Class ConnectionWrapper _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class ConnectionWrapper {

        /** _more_          */
        long now = System.currentTimeMillis();

        /** _more_          */
        Connection connection;

        /**
         * _more_
         *
         * @param connection _more_
         */
        ConnectionWrapper(Connection connection) {
            this.connection = connection;
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Statement createStatement() throws Exception {
        Connection connection = getConnection();
        Statement  stmt       = connection.createStatement();
        releaseConnection(connection);
        return stmt;
    }


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
        Connection        connection = getConnection();
        PreparedStatement stmt       = connection.prepareStatement(query);
        releaseConnection(connection);
        return stmt;
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
        Connection        connection = getConnection();
        String            query      = SqlUtil.makeUpdate(table, colId,
                                           names);
        PreparedStatement stmt       = connection.prepareStatement(query);
        for (int i = 0; i < values.length; i++) {
            SqlUtil.setValue(stmt, values[i], i + 1);
        }
        stmt.setString(values.length + 1, id);
        stmt.execute();
        stmt.close();
        releaseConnection(connection);
    }



    /**
     * _more_
     *
     * @param connection _more_
     */
    public void releaseConnection(Connection connection) {}

    /**
     * _more_
     *
     *
     * @param makeNewOne _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Connection getConnection(boolean makeNewOne) throws Exception {
        if (makeNewOne) {
            Connection connection = makeConnection();
            return connection;
        }
        synchronized (CONNECTION_MUTEX) {
            if (connectionCnt++ > 100) {
                //                closeConnection();
                //                connectionCnt = 0;
            }
            if (theConnection == null) {
                theConnection = makeConnection();
            }
            try {
                //check if the connection is OK
                Statement statement = theConnection.createStatement();
                statement.execute("select * from dummy");
            } catch (Exception exc) {
                try {
                    closeConnection();
                } catch (Exception ignore) {}
                theConnection = makeConnection();
            }
            return theConnection;
        }
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
        SqlUtil.delete(connection, table, clause);
        releaseConnection(connection);
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void closeConnection() throws Exception {
        if (theConnection != null) {
            theConnection.close();
        }
        theConnection = null;
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
        releaseConnection(connection);
        return connected;
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
        if ( !results.next()) {
            return 0;
        }
        return results.getInt(1);
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
     * @return _more_
     *
     * @throws Exception _more_
     */
    public DatabaseMetaData getMetadata() throws Exception {
        Connection       connection = getNewConnection();
        DatabaseMetaData dbmd       = connection.getMetaData();
        connection.close();
        return dbmd;
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



        DatabaseMetaData dbmd     = getMetadata();
        ResultSet        catalogs = dbmd.getCatalogs();
        ResultSet tables = dbmd.getTables(null, null, null,
                                          new String[] { "TABLE" });

        int totalRowCnt = 0;
        while (tables.next()) {
            String tableName = tables.getString("TABLE_NAME");
            String tableType = tables.getString("TABLE_TYPE");
            if ((tableType == null) || Misc.equals(tableType, "INDEX")
                    || tableType.startsWith("SYSTEM")) {
                continue;
            }


            String tn = tableName.toLowerCase();
            if ( !all) {
                if (tn.equals(TABLE_GLOBALS) || tn.equals(TABLE_USERS)
                        || tn.equals(TABLE_PERMISSIONS)
                        || tn.equals(TABLE_HARVESTERS)
                        || tn.equals(TABLE_USERROLES)) {
                    continue;
                }
            }


            ResultSet cols     = dbmd.getColumns(null, null, tableName, null);

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
            System.err.println("table:" + tableName);

            Statement stmt = execute("select * from " + tableName, 10000000,
                                     0);
            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
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
                        int type = ((Integer) types.get(i - 1)).intValue();
                        if (i > 1) {
                            value.append(",");
                        }
                        if (type == java.sql.Types.TIMESTAMP) {
                            Timestamp ts = results.getTimestamp(i);
                            //                            sb.append(SqlUtil.format(new Date(ts.getTime())));
                            value.append("'" + ts.toString() + "'");
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
                                     "insert into " + tableName.toLowerCase()
                                     + colNames + " values ");
                        IOUtil.write(os, StringUtil.join(",", valueList));
                        IOUtil.write(os, ";\n");
                        valueList = new ArrayList();
                    }
                }
            }
            if (valueList.size() > 0) {
                System.err.println("\tdoing last bit");
                if ( !didDelete) {
                    didDelete = true;
                    IOUtil.write(os,
                                 "delete from  " + tableName.toLowerCase()
                                 + ";\n");
                }
                IOUtil.write(os,
                             "insert into " + tableName.toLowerCase()
                             + colNames + " values ");
                IOUtil.write(os, StringUtil.join(",", valueList));
                IOUtil.write(os, ";\n");
            }
            System.err.println("\twrote:" + rowCnt + " rows");
        }

    }


    /**
     * _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected Connection makeConnection() throws Exception {

        String userName = (String) getRepository().getProperty(
                              PROP_DB_USER.replace("${db}", db));
        String password = (String) getRepository().getProperty(
                              PROP_DB_PASSWORD.replace("${db}", db));
        String connectionURL =
            (String) getRepository().getProperty(PROP_DB_URL.replace("${db}",
                db));
        //        System.err.println(connectionURL);
        Misc.findClass(
            (String) getRepository().getProperty(
                PROP_DB_DRIVER.replace("${db}", db)));


        //        getRepository().log("making connection:" + connectionURL);
        Connection connection;
        if (userName != null) {
            connection = DriverManager.getConnection(connectionURL, userName,
                    password);
        } else {
            connection = DriverManager.getConnection(connectionURL);
        }
        initConnection(connection);
        return connection;
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
    protected Statement execute(String sql, int max, int timeout)
            throws Exception {
        Connection connection = getConnection();
        Statement  stmt       = execute(connection, sql, max, timeout);
        releaseConnection(connection);
        return stmt;
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
    protected Statement execute(Connection connection, String sql, int max,
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
            //            System.err.println("query:" + sql);
            statement.execute(sql);
        } catch (Exception exc) {
            getRepository().log("Error executing sql:" + sql);
            throw exc;
        }
        long t2 = System.currentTimeMillis();
        if (getRepository().debug || (t2 - t1 > 300)) {
            System.err.println("query:" + sql);
            System.err.println("query time:" + (t2 - t1));
        }
        if (t2 - t1 > 2000) {
            //            Misc.printStack("query:" + sql);
        }
        return statement;
    }





    /**
     * _more_
     *
     * @param insert _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    protected void executeInsert(String insert, Object[] values)
            throws Exception {
        PreparedStatement pstmt = getPreparedStatement(insert);
        for (int i = 0; i < values.length; i++) {
            //Assume null is a string
            if (values[i] == null) {
                pstmt.setNull(i + 1, java.sql.Types.VARCHAR);
            } else if (values[i] instanceof Date) {
                pstmt.setTimestamp(
                    i + 1,
                    new java.sql.Timestamp(((Date) values[i]).getTime()),
                    Repository.calendar);
            } else if (values[i] instanceof Boolean) {
                boolean b = ((Boolean) values[i]).booleanValue();
                pstmt.setInt(i + 1, (b
                                     ? 1
                                     : 0));
            } else {
                pstmt.setObject(i + 1, values[i]);
            }
        }
        try {
            pstmt.execute();
            pstmt.close();
        } catch (Exception exc) {
            System.err.println("Error:" + insert);
            throw exc;
        }
    }





    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     */
    protected String convertSql(String sql) {
        if (db.equals(DB_MYSQL)) {
            sql = sql.replace("float8", "double");
        } else if (db.equals(DB_DERBY)) {
            sql = sql.replace("float8", "double");
        }
        return sql;
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
    protected String convertType(String type) {
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
    protected String getLimitString(int skip, int max) {
        if (db.equals(DB_MYSQL)) {
            return " LIMIT " + max + " OFFSET " + skip + " ";
        } else if (db.equals(DB_DERBY)) {
            return "";
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
            return false;
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
        Connection connection = getConnection();
        Statement stmt = SqlUtil.select(connection, what,
                                        Misc.newList(table),
                                        new Clause[] { clause }, extra);
        releaseConnection(connection);
        return stmt;
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
    public Statement select(String what, List tables, Clause clause,
                            String extra, int max)
            throws Exception {
        Connection connection = getConnection();
        Statement stmt = SqlUtil.select(connection, what, tables, clause,
                                        extra, max);
        releaseConnection(connection);
        return stmt;
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
        return select(what, Misc.newList(table), new Clause[] { clause });
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
        Connection connection = getConnection();
        Statement  stmt = SqlUtil.select(connection, what, tables, clauses);
        releaseConnection(connection);
        return stmt;
    }




}

