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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
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
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public interface Tables {


    /** _more_          */
    public static final int MAX_ROWS = 1000;


    /*
      For each of the tables in the database we have the following defs.
      The TABLE_<TABLE NAME> ... is the name of the table.
      The COL_<TABLE NAME>_<COLUMN NAME> is the name of the column in the table
      The ARRAY_<TABLE NAME> is the array of column names
      The COLUMNS_<TABLE NAME> is the comma separated list of columns in the table
      The J- and J+ turn off jindent formatting
     */

    //J-
    public static final String TABLE_FILES = "files";
    public static final String COL_FILES_ID             = TABLE_FILES + ".id";
    public static final String COL_FILES_NAME           = TABLE_FILES + ".name";
    public static final String COL_FILES_DESCRIPTION    = TABLE_FILES + ".description";
    public static final String COL_FILES_TYPE           = TABLE_FILES + ".type";
    public static final String COL_FILES_GROUP_ID       = TABLE_FILES + ".group_id";
    public static final String COL_FILES_USER_ID        = TABLE_FILES + ".user_id";
    public static final String COL_FILES_FILE           = TABLE_FILES + ".file";
    public static final String COL_FILES_CREATEDATE     = TABLE_FILES + ".createdate";
    public static final String COL_FILES_FROMDATE       = TABLE_FILES + ".fromdate";
    public static final String COL_FILES_TODATE         = TABLE_FILES + ".todate";

    public static final String []ARRAY_FILES = new String[]{
        COL_FILES_ID,
        COL_FILES_NAME,
        COL_FILES_DESCRIPTION,
        COL_FILES_TYPE,
        COL_FILES_GROUP_ID,
        COL_FILES_USER_ID,
        COL_FILES_FILE,
        COL_FILES_CREATEDATE,
        COL_FILES_FROMDATE,
        COL_FILES_TODATE
    };


    public static final String COLUMNS_FILES = SqlUtil.comma(ARRAY_FILES);
    public static final String INSERT_FILES =
        SqlUtil.makeInsert(
            TABLE_FILES,
            COLUMNS_FILES,
            SqlUtil.getQuestionMarks(ARRAY_FILES.length));


    public static final String TABLE_USERS = "users";
    public static final String COL_USERS_ID = TABLE_USERS + ".id";
    public static final String COL_USERS_NAME = TABLE_USERS + ".name";
    public static final String COL_USERS_ADMIN = TABLE_USERS + ".admin";
    public static final String []ARRAY_USERS = new String[]{COL_USERS_ID,
                                                          COL_USERS_NAME,
                                                          COL_USERS_ADMIN};

    public static final String COLUMNS_USERS = SqlUtil.comma(ARRAY_USERS);
    public static final String INSERT_USERS =
        SqlUtil.makeInsert(
            TABLE_USERS,
            COLUMNS_USERS, 
            SqlUtil.getQuestionMarks(ARRAY_USERS.length));



    /** _more_ */
    public static final String TABLE_GROUPS = "groups";
    public static final String COL_GROUPS_ID = TABLE_GROUPS + ".id";
    public static final String COL_GROUPS_PARENT = TABLE_GROUPS + ".parent";
    public static final String COL_GROUPS_NAME = TABLE_GROUPS + ".name";
    public static final String COL_GROUPS_DESCRIPTION = TABLE_GROUPS + ".description";
    public static final String []ARRAY_GROUPS = new String[]{COL_GROUPS_ID,
                                                           COL_GROUPS_PARENT,
                                                           COL_GROUPS_NAME,
                                                           COL_GROUPS_DESCRIPTION};

    public static final String COLUMNS_GROUPS = SqlUtil.comma(ARRAY_GROUPS);
    public static final String INSERT_GROUPS =
        SqlUtil.makeInsert(
            TABLE_GROUPS,
            COLUMNS_GROUPS, 
            SqlUtil.getQuestionMarks(ARRAY_GROUPS.length));


    public static final String TABLE_TAGS = "tags";
    public static final String COL_TAGS_NAME = TABLE_TAGS + ".name";
    public static final String COL_TAGS_FILE_ID= TABLE_TAGS + ".file_id";
    public static final String []ARRAY_TAGS =  new String[]{COL_TAGS_NAME,COL_TAGS_FILE_ID};
    public static final String COLUMNS_TAGS = SqlUtil.comma(ARRAY_TAGS);
    
    public static final String INSERT_TAGS =
        SqlUtil.makeInsert(TABLE_TAGS,
                           COLUMNS_TAGS,
                           SqlUtil.getQuestionMarks(ARRAY_TAGS.length));



    public static final String TABLE_LEVEL3RADAR = "level3radar";
    public static final String COL_LEVEL3RADAR_ID = TABLE_LEVEL3RADAR + ".id";
    public static final String COL_LEVEL3RADAR_STATION = TABLE_LEVEL3RADAR + ".station";
    public static final String COL_LEVEL3RADAR_PRODUCT = TABLE_LEVEL3RADAR + ".product";
    public static final String []ARRAY_LEVEL3RADAR = new String[]{COL_LEVEL3RADAR_ID,
                                                                COL_LEVEL3RADAR_STATION,
                                                                COL_LEVEL3RADAR_PRODUCT};


    public static final String COLUMNS_LEVEL3RADAR = SqlUtil.comma(ARRAY_LEVEL3RADAR);
    
    public static final String INSERT_LEVEL3RADAR =
        SqlUtil.makeInsert(TABLE_LEVEL3RADAR,
                           COLUMNS_LEVEL3RADAR,
                           SqlUtil.getQuestionMarks(ARRAY_LEVEL3RADAR.length));




    public static final String TABLE_SATELLITE = "satellite";
    public static final String COL_SATELLITE_ID = TABLE_SATELLITE + ".id";
    public static final String COL_SATELLITE_PLATFORM = TABLE_SATELLITE + ".platform";
    public static final String COL_SATELLITE_RESOLUTION = TABLE_SATELLITE + ".resolution";
    public static final String COL_SATELLITE_PRODUCT = TABLE_SATELLITE + ".product";
    public static final String []ARRAY_SATELLITE = new String[]{COL_SATELLITE_ID,
                                                                COL_SATELLITE_PLATFORM,
                                                                COL_SATELLITE_RESOLUTION,
                                                                COL_SATELLITE_PRODUCT};


    public static final String COLUMNS_SATELLITE = SqlUtil.comma(ARRAY_SATELLITE);
    
    public static final String INSERT_SATELLITE =
        SqlUtil.makeInsert(TABLE_SATELLITE,
                           COLUMNS_SATELLITE,
                           SqlUtil.getQuestionMarks(ARRAY_SATELLITE.length));




    //J+
}

