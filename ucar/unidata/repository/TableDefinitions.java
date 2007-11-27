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


import ucar.unidata.data.SqlUtils;
import ucar.unidata.xml.XmlUtil;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.StringUtil;



import java.net.*;
import java.io.File;
import java.io.InputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Date;
import java.util.List;



import java.util.regex.*;


/**
 * Class SqlUtils _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public interface TableDefinitions {

    public static final String TABLE_FILES = "files";
    public static final String TABLE_LEVEL3RADAR = "level3radar";
    public static final String TABLE_GROUPS = "groups";


    public static final String COL_FILES_ID = TABLE_FILES +"." +"id";
    public static final String COL_FILES_GROUP_ID = TABLE_FILES +"." +"group_id";
    public static final String COL_FILES_FILE = TABLE_FILES +"." +"file";
    public static final String COL_FILES_FROMDATE=TABLE_FILES +"." +"fromdate";
    public static final String COL_FILES_TODATE=TABLE_FILES +"." +"todate";


    public static final String COL_LEVEL3RADAR_ID = TABLE_LEVEL3RADAR +"." +"id";
    public static final String COL_LEVEL3RADAR_GROUP_ID = TABLE_LEVEL3RADAR +"." +"group_id";
    public static final String COL_LEVEL3RADAR_FILE = TABLE_LEVEL3RADAR +"." +"file";
    public static final String COL_LEVEL3RADAR_STATION= TABLE_LEVEL3RADAR +"." +"station";
    public static final String COL_LEVEL3RADAR_PRODUCT = TABLE_LEVEL3RADAR +"." +"product";
    public static final String COL_LEVEL3RADAR_DATE=TABLE_LEVEL3RADAR +"." +"date";



    public static final String SELECT_FILES_GROUPS=
        SqlUtils.makeSelect(SqlUtils.distinct(COL_LEVEL3RADAR_GROUP_ID), TABLE_LEVEL3RADAR);
    public static final String SELECT_LEVEL3RADAR_PRODUCTS=
        SqlUtils.makeSelect(SqlUtils.distinct(COL_LEVEL3RADAR_PRODUCT), TABLE_LEVEL3RADAR);
    public static final String SELECT_LEVEL3RADAR_STATIONS=
        SqlUtils.makeSelect(SqlUtils.distinct(COL_LEVEL3RADAR_STATION),TABLE_LEVEL3RADAR);

    public static final String SELECT_FILES_MAXDATE=
        SqlUtils.makeSelect(SqlUtils.max(COL_LEVEL3RADAR_DATE), TABLE_LEVEL3RADAR);
    public static final String SELECT_FILES_MINDATE=
        SqlUtils.makeSelect(SqlUtils.min(COL_LEVEL3RADAR_DATE), TABLE_LEVEL3RADAR);





}

