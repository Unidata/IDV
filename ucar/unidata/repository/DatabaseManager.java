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

import ucar.unidata.data.SqlUtil;



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



    /**
     * _more_
     *
     * @param repository _more_
     */
    public DatabaseManager(Repository repository) {
        super(repository);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getConnection() throws Exception {
        if (theConnection != null) {
            try {
                Statement statement = theConnection.createStatement();
                statement.execute("select * from dummy");
            } catch (Exception exc) {
                theConnection = makeConnection();
            }
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
        db = (String) getRepository().getProperty(PROP_DB);
        if (db == null) {
            throw new IllegalStateException("Must have a " + PROP_DB
                                            + " property defined");
        }


        String userName = (String) getRepository().getProperty(
                              PROP_DB_USER.replace("${db}", db));
        String password = (String) getRepository().getProperty(
                              PROP_DB_PASSWORD.replace("${db}", db));
        String connectionURL =
            (String) getRepository().getProperty(PROP_DB_URL.replace("${db}",
                db));
        Misc.findClass(
            (String) getRepository().getProperty(
                PROP_DB_DRIVER.replace("${db}", db)));


        getRepository().log("making connection:" + connectionURL);
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
        if (db.equals("mysql")) {
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
     */
    protected String convertSql(String sql) {
        if (db.equals("mysql")) {
            sql = sql.replace("float8", "double");

        } else if (db.equals("derby")) {
            sql = sql.replace("float8", "double");
        }
        return sql;
    }



}

