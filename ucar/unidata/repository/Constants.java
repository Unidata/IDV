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
public interface Constants {


    /** _more_          */
    public static final int MAX_ROWS = 1000;


    /** _more_ */
    public static final String TABLE_FILES = "files";

    /** _more_ */
    public static final String TABLE_LEVEL3RADAR = "level3radar";

    /** _more_ */
    public static final String TABLE_GROUPS = "groups";


    /** _more_ */
    public static final String COL_FILES_ID = TABLE_FILES + "." + "id";

    /** _more_ */
    public static final String COL_FILES_NAME = TABLE_FILES + "." + "name";

    /** _more_ */
    public static final String COL_FILES_DESCRIPTION = TABLE_FILES + "."
                                                       + "description";

    /** _more_ */
    public static final String COL_FILES_TYPE = TABLE_FILES + "." + "type";

    /** _more_ */
    public static final String COL_FILES_GROUP_ID = TABLE_FILES + "."
                                                    + "group_id";

    /** _more_ */
    public static final String COL_FILES_FILE = TABLE_FILES + "." + "file";

    /** _more_ */
    public static final String COL_FILES_FROMDATE = TABLE_FILES + "."
                                                    + "fromdate";

    /** _more_ */
    public static final String COL_FILES_TODATE = TABLE_FILES + "."
                                                  + "todate";


    /** _more_ */
    public static final String COL_LEVEL3RADAR_ID = TABLE_LEVEL3RADAR + "."
                                                    + "id";

    /** _more_ */
    public static final String COL_LEVEL3RADAR_STATION = TABLE_LEVEL3RADAR
                                                         + "." + "station";

    /** _more_ */
    public static final String COL_LEVEL3RADAR_PRODUCT = TABLE_LEVEL3RADAR
                                                         + "." + "product";


    /** _more_ */
    public static final String COL_GROUPS_ID = TABLE_GROUPS + "." + "id";

    /** _more_ */
    public static final String COL_GROUPS_PARENT = TABLE_GROUPS + "."
                                                   + "parent";

    /** _more_ */
    public static final String COL_GROUPS_NAME = TABLE_GROUPS + "." + "name";

    /** _more_ */
    public static final String COL_GROUPS_DESCRIPTION = TABLE_GROUPS + "."
                                                        + "description";


    /** _more_ */
    public static final String COLUMNS_GROUPS = SqlUtil.comma(COL_GROUPS_ID,
                                                    COL_GROUPS_PARENT,
                                                    COL_GROUPS_NAME,
                                                    COL_GROUPS_DESCRIPTION);


    /** _more_ */
    public static final String SELECT_FILES_GROUPS =
        SqlUtil.makeSelect(SqlUtil.distinct(COL_FILES_GROUP_ID), TABLE_FILES);

    /** _more_ */
    public static final String SELECT_LEVEL3RADAR_PRODUCTS =
        SqlUtil.makeSelect(SqlUtil.distinct(COL_LEVEL3RADAR_PRODUCT),
                           TABLE_FILES + "," + TABLE_LEVEL3RADAR);

    /** _more_ */
    public static final String SELECT_LEVEL3RADAR_STATIONS =
        SqlUtil.makeSelect(SqlUtil.distinct(COL_LEVEL3RADAR_STATION),
                           TABLE_FILES + "," + TABLE_LEVEL3RADAR);

    /** _more_ */
    public static final String SELECT_FILES_MAXDATE =
        SqlUtil.makeSelect(SqlUtil.max(COL_FILES_FROMDATE), TABLE_FILES);

    /** _more_ */
    public static final String SELECT_FILES_MINDATE =
        SqlUtil.makeSelect(SqlUtil.min(COL_FILES_FROMDATE), TABLE_FILES);

    /** _more_ */
    public static final String INSERT_FILES =
        SqlUtil.makeInsert(
            TABLE_FILES,
            SqlUtil.comma(
                COL_FILES_ID, COL_FILES_NAME, COL_FILES_DESCRIPTION,
                COL_FILES_TYPE, COL_FILES_GROUP_ID, COL_FILES_FILE,
                COL_FILES_FROMDATE, COL_FILES_TODATE), "?,?,?,?,?,?,?,?");

    /** _more_ */
    public static final String INSERT_LEVEL3RADAR =
        SqlUtil.makeInsert(TABLE_LEVEL3RADAR,
                           SqlUtil.comma(COL_LEVEL3RADAR_ID,
                                         COL_LEVEL3RADAR_STATION,
                                         COL_LEVEL3RADAR_PRODUCT), "?,?,?");


    /** _more_ */
    public static final String SELECT_GROUP =
        SqlUtil.makeSelect(COLUMNS_GROUPS, TABLE_GROUPS);

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_TYPE = "type";


    /** _more_          */
    public static final String TAG_NODE = "node";

    /** _more_          */
    public static final String TAG_EDGE = "edge";

    /** _more_          */
    public static final String ATTR_FROM = "from";

    /** _more_          */
    public static final String ATTR_TO = "to";


    /** _more_          */
    public static final String ATTR_TITLE = "title";

    /** _more_          */
    public static final String ATTR_URLPATH = "urlPath";

    /** _more_          */
    public static final String TAG_CATALOG = "catalog";

    /** _more_          */
    public static final String TAG_DATASET = "dataset";

    /** _more_          */
    public static final String TAG_GROUPS = "groups";

    /** _more_          */
    public static final String TAG_GROUP = "group";

    /** _more_          */
    public static final String TAG_TYPES = "types";

    /** _more_          */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String ARG_TYPE = "type";

    /** _more_          */
    public static final String ARG_WHAT = "what";

    /** _more_          */
    public static final String WHAT_TYPE = "type";

    /** _more_          */
    public static final String WHAT_GROUP = "group";

    /** _more_          */
    public static final String WHAT_PRODUCT = "product";

    /** _more_          */
    public static final String WHAT_STATION = "station";



    /** _more_          */
    public static final String ARG_MAX = "max";

    /** _more_          */
    public static final String ARG_OUTPUT = "output";

    /** _more_          */
    public static final String ARG_NAME = "name";

    /** _more_          */
    public static final String ARG_ID = "id";

    /** _more_ */
    public static final String ARG_GROUP = "group";

    /** _more_          */
    public static final String ARG_GROUPID = "groupid";

    /** _more_          */
    public static final String ARG_GROUP_CHILDREN = "group_children";

    /** _more_ */
    public static final String ARG_TODATE = "todate";

    /** _more_ */
    public static final String ARG_FROMDATE = "fromdate";

    /** _more_ */
    public static final String ARG_PRODUCT = "product";

    /** _more_ */
    public static final String ARG_STATION = "station";


    /** _more_          */
    public static final String OUTPUT_HTML = "html";

    /** _more_          */
    public static final String OUTPUT_XML = "xml";

    /** _more_          */
    public static final String OUTPUT_CSV = "csv";

    /** _more_          */
    public static final String OUTPUT_GRAPH = "graph";

    /** _more_          */
    public static final String TYPE_FILE = "file";

    /** _more_          */
    public static final String TYPE_GROUP = "group";


}

