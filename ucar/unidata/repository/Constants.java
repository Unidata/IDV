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

    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_TYPE = "type";


    /** _more_ */
    public static final String TAG_NODE = "node";

    /** _more_ */
    public static final String TAG_EDGE = "edge";

    /** _more_          */
    public static final String TAG_TAG = "tag";

    /** _more_          */
    public static final String TAG_TAGS = "tags";

    /** _more_          */
    public static final String TAG_METHOD = "method";

    /** _more_          */
    public static final String ATTR_API_REQUEST = "request";

    /** _more_          */
    public static final String ATTR_API_METHOD = "method";

    /** _more_          */
    public static final String ATTR_API_ADMIN = "admin";

    /** _more_          */
    public static final String ATTR_API_CANCACHE = "cancache";

    /** _more_          */
    public static final String ATTR_RSS_VERSION = "version";

    /** _more_          */
    public static final String TAG_RSS_RSS = "rss";

    /** _more_          */
    public static final String TAG_RSS_CHANNEL = "channel";

    /** _more_          */
    public static final String TAG_RSS_ITEM = "item";

    /** _more_          */
    public static final String TAG_RSS_TITLE = "title";

    /** _more_          */
    public static final String TAG_RSS_PUBDATE = "pubDate";

    /** _more_          */
    public static final String TAG_RSS_DESCRIPTION = "description";


    /** _more_ */
    public static final String ATTR_FROM = "from";

    /** _more_ */
    public static final String ATTR_TO = "to";


    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_URLPATH = "urlPath";

    /** _more_ */
    public static final String TAG_CATALOG = "catalog";

    /** _more_ */
    public static final String TAG_DATASET = "dataset";

    /** _more_ */
    public static final String TAG_GROUPS = "groups";

    /** _more_ */
    public static final String TAG_GROUP = "group";

    /** _more_ */
    public static final String TAG_TYPES = "types";

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String ARG_TYPE = "type";

    /** _more_          */
    public static final String ARG_APPLET = "applet";

    /** _more_          */
    public static final String ARG_USER = "user";

    /** _more_          */
    public static final String ARG_TAG = "tag";

    /** _more_ */
    public static final String ARG_WHAT = "what";

    /** _more_ */
    public static final String WHAT_ENTRIES = "entries";

    /** _more_          */
    public static final String WHAT_TYPE = "type";

    /** _more_          */
    public static final String WHAT_TAG = "tag";

    /** _more_ */
    public static final String WHAT_USER = "user";

    /** _more_ */
    public static final String WHAT_GROUP = "group";

    /** _more_ */
    public static final String ARG_MAX = "max";

    /** _more_ */
    public static final String ARG_OUTPUT = "output";

    /** _more_ */
    public static final String ARG_NAME = "name";

    /** _more_ */
    public static final String ARG_ID = "id";
    public static final String ARG_IDS = "ids";

    /** _more_ */
    public static final String ARG_GROUP = "group";

    /** _more_ */
    public static final String ARG_GROUPID = "groupid";

    /** _more_ */
    public static final String ARG_GROUP_CHILDREN = "group_children";

    /** _more_ */
    public static final String ARG_CREATEDATE = "createdate";

    /** _more_          */
    public static final String ARG_QUERY = "query";

    /** _more_ */
    public static final String ARG_TODATE = "todate";

    /** _more_ */
    public static final String ARG_FROMDATE = "fromdate";

    /** _more_ */
    public static final String ARG_PRODUCT = "product";

    /** _more_ */
    public static final String ARG_STATION = "station";


    /** _more_ */
    public static final String OUTPUT_HTML = "html";

    /** _more_          */
    public static final String OUTPUT_RSS = "rss";

    /** _more_ */
    public static final String OUTPUT_XML = "xml";

    /** _more_          */
    public static final String OUTPUT_CATALOG = "xml";

    /** _more_ */
    public static final String OUTPUT_CSV = "csv";

    /** _more_ */
    public static final String OUTPUT_GRAPH = "graph";

    /** _more_ */
    public static final String OUTPUT_CLOUD = "cloud";

    /** _more_          */
    public static final String OUTPUT_ZIP = "zip";


    /** _more_          */
    public static final String TYPE_TAG = "tag";

    /** _more_ */
    public static final String TYPE_GROUP = "group";

    /** _more_          */
    public static final String PROP_NAVLINKS = "udms.navlinks";

    /** _more_          */
    public static final String PROP_NAVSUBLINKS = "udms.navsublinks";

    /** _more_          */
    public static final String PROP_SHOW_APPLET = "udms.html.showapplet";

    /** _more_          */
    public static final String PROP_HTML_URLBASE = "udms.html.urlbase";

    /** _more_          */
    public static final String PROP_HTML_TEMPLATE = "udms.html.template";

    /** _more_          */
    public static final String PROP_DB_CANCACHE = "udms.db.cancache";

    /** _more_          */
    public static final String PROP_DB_DRIVER = "udms.db.driver";

    /** _more_          */
    public static final String PROP_DB_URL = "udms.db.url";

    /** _more_          */
    public static final String PROP_DB_USER = "udms.db.user";

    /** _more_          */
    public static final String PROP_DB_PASSWORD = "udms.db.password";

    /** _more_          */
    public static final String PROP_HTML_DOWNLOADENTRIESASFILES =
        "udms.html.downloadentriesasfiles";


    /** _more_          */
    public static final String TAG_DB_ENTRY = "entry";

    public static final String TAG_DB_HANDLER = "handler";

    /** _more_          */
    public static final String TAG_DB_COLUMN = "column";

    /** _more_          */
    public static final String ATTR_DB_NAME = "name";

    /** _more_          */
    public static final String ATTR_DB_DESCRIPTION = "description";



    public static final String ARG_NODETYPE = "nodetype";

    /** _more_ */
    public static final String NODETYPE_ENTRY = "entry";

    /** _more_ */
    public static final String NODETYPE_GROUP = "group";

    public static final String TAG_CATALOGREF= "catalogRef";
    public static final String ATTR_XLINKTITLE="xlink:title";
    public static final String ATTR_XLINKHREF = "xlink:href";
}

