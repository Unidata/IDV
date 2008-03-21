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

import ucar.unidata.sql.Clause;
import ucar.unidata.sql.SqlUtil;



import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

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
import ucar.unidata.util.TwoFacedObject;

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
 * Class SqlUtil _more_
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
     *
     * @param makeNewOne _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getConnection(boolean makeNewOne) throws Exception {

        if (makeNewOne) {
            return makeConnection();
        }
        if (theConnection == null) {
            theConnection = makeConnection();
        }

        try {
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
     */
    public boolean hasConnection() {
        return theConnection != null;
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement execute(String sql) throws Exception {
        return execute(sql, -1);
    }

    /**
     * _more_
     *
     * @param sql _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement execute(String sql, int max) throws Exception {
        return execute(sql, max, 0);
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
        return execute(getConnection(), sql, max, timeout);
    }



    /**
     * _more_
     *
     * @param connection _more_
     * @param sql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Statement execute(Connection connection, String sql)
            throws Exception {
        return execute(connection, sql, 0, 0);
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
        PreparedStatement pstmt = getConnection().prepareStatement(insert);
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
     * @param table _more_
     * @param column _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    protected void executeDelete(String table, String column, String value)
            throws Exception {
        Statement stmt = execute(SqlUtil.makeDelete(table, column, value));
        stmt.close();
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

    public String escapeString(String value) {
        if (db.equals(DB_MYSQL)) {
            value = value.replace("'","\\'");
        } else {
            value = value.replace("'","''");
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


    public  Statement select(String what, String table, Clause clause,String extra) throws Exception {
        return SqlUtil.select(getConnection(),what, Misc.newList(table), new Clause[]{clause},extra);
    }


    public  Statement select(String what, String table, Clause clause) throws Exception {
        return select(what, Misc.newList(table), new Clause[]{clause});
    }


    public  Statement select(String what, String table, Clause[]clauses) throws Exception {
        return select(what, Misc.newList(table), clauses);
    }


    public  Statement select(String what, String table, List<Clause>clauses) throws Exception {
        return select(what, Misc.newList(table), Clause.toArray(clauses));
    }

    public  Statement select(String what, List tables, Clause[]clauses) throws Exception {
        return SqlUtil.select(getConnection(), what, tables, clauses);
    }




}

