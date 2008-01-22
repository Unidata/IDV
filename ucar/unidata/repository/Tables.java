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


    /** _more_ */
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







    public static final String TABLE_ENTRIES              = "entries";
    public static final String COL_ENTRIES_ID             = TABLE_ENTRIES + ".id";
    public static final String COL_ENTRIES_TYPE           = TABLE_ENTRIES + ".type";
    public static final String COL_ENTRIES_NAME           = TABLE_ENTRIES + ".name";
    public static final String COL_ENTRIES_DESCRIPTION    = TABLE_ENTRIES + ".description";
    public static final String COL_ENTRIES_PARENT_GROUP_ID       = TABLE_ENTRIES + ".parent_group_id";
    public static final String COL_ENTRIES_USER_ID        = TABLE_ENTRIES + ".user_id";
    public static final String COL_ENTRIES_RESOURCE       = TABLE_ENTRIES + ".resource";
    public static final String COL_ENTRIES_RESOURCE_TYPE   = TABLE_ENTRIES + ".resource_type";
    public static final String COL_ENTRIES_CREATEDATE     = TABLE_ENTRIES + ".createdate";
    public static final String COL_ENTRIES_FROMDATE       = TABLE_ENTRIES + ".fromdate";
    public static final String COL_ENTRIES_TODATE         = TABLE_ENTRIES + ".todate";
    public static final String COL_ENTRIES_SOUTH         = TABLE_ENTRIES +".south";    
    public static final String COL_ENTRIES_NORTH         = TABLE_ENTRIES +".north";
    public static final String COL_ENTRIES_EAST         = TABLE_ENTRIES +".east";    
    public static final String COL_ENTRIES_WEST         = TABLE_ENTRIES +".west";



    public static final String []ARRAY_ENTRIES = new String[]{
        COL_ENTRIES_ID,
        COL_ENTRIES_TYPE,
        COL_ENTRIES_NAME,
        COL_ENTRIES_DESCRIPTION,
        COL_ENTRIES_PARENT_GROUP_ID,
        COL_ENTRIES_USER_ID,
        COL_ENTRIES_RESOURCE,
        COL_ENTRIES_RESOURCE_TYPE,
        COL_ENTRIES_CREATEDATE,
        COL_ENTRIES_FROMDATE,
        COL_ENTRIES_TODATE,
        COL_ENTRIES_SOUTH,
        COL_ENTRIES_NORTH,
        COL_ENTRIES_EAST,
        COL_ENTRIES_WEST
    };


    public static final String COLUMNS_ENTRIES = SqlUtil.comma(ARRAY_ENTRIES);
    public static final String INSERT_ENTRIES =
        SqlUtil.makeInsert(
            TABLE_ENTRIES,
            COLUMNS_ENTRIES,
            SqlUtil.getQuestionMarks(ARRAY_ENTRIES.length));

    public static final String UPDATE_ENTRIES =
        SqlUtil.makeUpdate(
            TABLE_ENTRIES,
            COL_ENTRIES_ID,
            ARRAY_ENTRIES);





    public static final String TABLE_GLOBALS = "globals";
    public static final String COL_GLOBALS_NAME = TABLE_GLOBALS + ".name";
    public static final String COL_GLOBALS_VALUE = TABLE_GLOBALS + ".value";
    public static final String []ARRAY_GLOBALS = new String[]{COL_GLOBALS_NAME,
                                                              COL_GLOBALS_VALUE};

    public static final String COLUMNS_GLOBALS = SqlUtil.comma(ARRAY_GLOBALS);
    public static final String INSERT_GLOBALS =
        SqlUtil.makeInsert(
            TABLE_GLOBALS,
            COLUMNS_GLOBALS, 
            SqlUtil.getQuestionMarks(ARRAY_GLOBALS.length));



    public static final String TABLE_USERS = "users";
    public static final String COL_USERS_ID = TABLE_USERS + ".id";
    public static final String COL_USERS_NAME = TABLE_USERS + ".name";
    public static final String COL_USERS_EMAIL = TABLE_USERS + ".email";
    public static final String COL_USERS_QUESTION = TABLE_USERS + ".question";
    public static final String COL_USERS_ANSWER = TABLE_USERS + ".answer";
    public static final String COL_USERS_PASSWORD = TABLE_USERS + ".password";
    public static final String COL_USERS_ADMIN = TABLE_USERS + ".admin";
    public static final String []ARRAY_USERS = new String[]{COL_USERS_ID,
                                                          COL_USERS_NAME,
                                                          COL_USERS_EMAIL,
                                                          COL_USERS_QUESTION,
                                                          COL_USERS_ANSWER,
                                                          COL_USERS_PASSWORD,
                                                          COL_USERS_ADMIN};

    public static final String COLUMNS_USERS = SqlUtil.comma(ARRAY_USERS);
    public static final String INSERT_USERS =
        SqlUtil.makeInsert(
            TABLE_USERS,
            COLUMNS_USERS, 
            SqlUtil.getQuestionMarks(ARRAY_USERS.length));





    public static final String TABLE_USERROLES = "userroles";
    public static final String COL_USERROLES_USER_ID = TABLE_USERROLES + ".user_id";
    public static final String COL_USERROLES_ROLE = TABLE_USERROLES + ".role";
    public static final String []ARRAY_USERROLES = new String[]{COL_USERROLES_USER_ID,
                                                                COL_USERROLES_ROLE};


    public static final String COLUMNS_USERROLES = SqlUtil.comma(ARRAY_USERROLES);
    public static final String INSERT_USERROLES =
        SqlUtil.makeInsert(
            TABLE_USERROLES,
            COLUMNS_USERROLES, 
            SqlUtil.getQuestionMarks(ARRAY_USERROLES.length));





    public static final String TABLE_METADATA = "metadata";
    public static final String COL_METADATA_ID = TABLE_METADATA + ".id";
    public static final String COL_METADATA_TYPE = TABLE_METADATA + ".type";
    public static final String COL_METADATA_NAME = TABLE_METADATA + ".name";
    public static final String COL_METADATA_CONTENT = TABLE_METADATA + ".content";
    public static final String []ARRAY_METADATA = new String[]{COL_METADATA_ID,
                                                               COL_METADATA_TYPE,
                                                               COL_METADATA_NAME,
                                                               COL_METADATA_CONTENT};

    public static final String COLUMNS_METADATA = SqlUtil.comma(ARRAY_METADATA);
    public static final String INSERT_METADATA =
        SqlUtil.makeInsert(
            TABLE_METADATA,
            COLUMNS_METADATA, 
            SqlUtil.getQuestionMarks(ARRAY_METADATA.length));



    public static final String TABLE_TAGS = "tags";
    public static final String COL_TAGS_NAME = TABLE_TAGS + ".name";
    public static final String COL_TAGS_ENTRY_ID= TABLE_TAGS + ".entry_id";
    public static final String []ARRAY_TAGS =  new String[]{COL_TAGS_NAME,COL_TAGS_ENTRY_ID};
    public static final String COLUMNS_TAGS = SqlUtil.comma(ARRAY_TAGS);
    
    public static final String INSERT_TAGS =
        SqlUtil.makeInsert(TABLE_TAGS,
                           COLUMNS_TAGS,
                           SqlUtil.getQuestionMarks(ARRAY_TAGS.length));



    public static final String TABLE_ASSOCIATIONS = "associations";
    public static final String COL_ASSOCIATIONS_NAME = TABLE_ASSOCIATIONS + ".name";
    public static final String COL_ASSOCIATIONS_FROM_ENTRY_ID= TABLE_ASSOCIATIONS + ".from_entry_id";
    public static final String COL_ASSOCIATIONS_TO_ENTRY_ID= TABLE_ASSOCIATIONS + ".to_entry_id";
    public static final String []ARRAY_ASSOCIATIONS =  new String[]{COL_ASSOCIATIONS_NAME,COL_ASSOCIATIONS_FROM_ENTRY_ID,COL_ASSOCIATIONS_TO_ENTRY_ID};
    public static final String COLUMNS_ASSOCIATIONS = SqlUtil.comma(ARRAY_ASSOCIATIONS);
    
    public static final String INSERT_ASSOCIATIONS =
        SqlUtil.makeInsert(TABLE_ASSOCIATIONS,
                           COLUMNS_ASSOCIATIONS,
                           SqlUtil.getQuestionMarks(ARRAY_ASSOCIATIONS.length));




    public static final String TABLE_COMMENTS = "comments";
    public static final String COL_COMMENTS_ID = TABLE_COMMENTS + ".id";
    public static final String COL_COMMENTS_ENTRY_ID= TABLE_COMMENTS + ".entry_id";
    public static final String COL_COMMENTS_USER_ID= TABLE_COMMENTS + ".user_id";
    public static final String COL_COMMENTS_DATE= TABLE_COMMENTS + ".date";
    public static final String COL_COMMENTS_SUBJECT= TABLE_COMMENTS + ".subject";
    public static final String COL_COMMENTS_COMMENT= TABLE_COMMENTS + ".comment";

    public static final String []ARRAY_COMMENTS =  new String[]{COL_COMMENTS_ID,
                                                                COL_COMMENTS_ENTRY_ID,
                                                                COL_COMMENTS_USER_ID,
                                                                COL_COMMENTS_DATE,
                                                                COL_COMMENTS_SUBJECT,
                                                                COL_COMMENTS_COMMENT};

    public static final String COLUMNS_COMMENTS = SqlUtil.comma(ARRAY_COMMENTS);
    
    public static final String INSERT_COMMENTS =
        SqlUtil.makeInsert(TABLE_COMMENTS,
                           COLUMNS_COMMENTS,
                           SqlUtil.getQuestionMarks(ARRAY_COMMENTS.length));






    //J+


}

