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

    /** _more_ */
    public static final String TAG_TAG = "tag";

    /** _more_ */
    public static final String TAG_ASSOCIATION = "association";

    /** _more_ */
    public static final String TAG_TAGS = "tags";

    /** _more_ */
    public static final String TAG_ASSOCIATIONS = "associations";

    /** _more_ */
    public static final String TAG_METHOD = "method";

    /** _more_ */
    public static final String ATTR_RSS_VERSION = "version";

    /** _more_ */
    public static final String TAG_RSS_RSS = "rss";

    /** _more_          */
    public static final String TAG_RSS_LINK = "link";

    /** _more_          */
    public static final String TAG_RSS_GUID = "guid";

    /** _more_ */
    public static final String TAG_RSS_CHANNEL = "channel";

    /** _more_ */
    public static final String TAG_RSS_ITEM = "item";

    /** _more_ */
    public static final String TAG_RSS_TITLE = "title";

    /** _more_ */
    public static final String TAG_RSS_PUBDATE = "pubDate";

    /** _more_ */
    public static final String TAG_RSS_DESCRIPTION = "description";


    /** _more_ */
    public static final String ATTR_FROM = "from";

    /** _more_ */
    public static final String ATTR_TO = "to";


    /** _more_ */
    public static final String ATTR_TITLE = "title";

    public static final String ATTR_TOOLTIP = "tooltip";



    /** _more_ */
    public static final String TAG_TYPES = "types";

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String ARG_TYPE = "type";

    public static final String ARG_RECURSE = "recurse";

    public static final String ARG_CATALOG = "catalog";


    public static final String ARG_SOUTH = "south";
    public static final String ARG_NORTH = "north";
    public static final String ARG_EAST = "east";
    public static final String ARG_WEST = "west";

    /** _more_          */
    public static final String ARG_WAIT = "wait";

    /** _more_ */
    public static final String ARG_SKIP = "skip";

    /** _more_          */
    public static final String ARG_ORDERBY = "orderby";

    /** _more_          */
    public static final String ARG_ASCENDING = "ascending";

    /** _more_          */
    public static final String ARG_AREA = "area";

    /** _more_          */
    public static final String ARG_FORM_TYPE = "form.type";

    public static final String ARG_CHANGE = "change";
    public static final String ARG_ADMIN = "admin";

    public static final String ARG_DELETE = "delete";
    public static final String ARG_DELETE_CONFIRM = "delete.confirm";
    public static final String ARG_CANCEL = "cancel";

    /** _more_          */
    public static final String ARG_INCLUDENONGEO = "includenongeo";


    /** _more_          */
    public static final String ARG_NEXT = "next";

    /** _more_          */
    public static final String ARG_PREVIOUS = "previous";

    /** _more_ */
    public static final String ARG_APPLET = "applet";


    /** _more_ */
    public static final String ARG_USER = "user";

    /** _more_ */
    public static final String ARG_TAG = "tag";

    /** _more_ */
    public static final String ARG_ASSOCIATION = "association";

    /** _more_ */
    public static final String ARG_WHAT = "what";

    /** _more_          */
    public static final String ARG_ACTION = "action";

    /** _more_ */
    public static final String WHAT_ENTRIES = "entries";



    /** _more_ */
    public static final String WHAT_TYPE = "type";

    /** _more_ */
    public static final String WHAT_TAG = "tag";


    public static final String WHAT_METADATA = "metadata";

    /** _more_ */
    public static final String WHAT_ASSOCIATION = "association";

    /** _more_ */
    public static final String WHAT_USER = "user";

    /** _more_ */
    public static final String WHAT_GROUP = "group";

    /** _more_ */
    public static final String ARG_MAX = "max";

    public static final String ARG_SHOWMETADATA = "showmetadata";

    /** _more_ */
    public static final String ARG_OUTPUT = "output";

    /** _more_ */
    public static final String ARG_NAME = "name";

    /** _more_ */
    public static final String ARG_SEARCHMETADATA = "searchmetadata";

    /** _more_ */
    public static final String ARG_ID = "id";

    /** _more_ */
    public static final String ARG_IDS = "ids";

    /** _more_ */
    public static final String ARG_GROUP = "group";

    /** _more_ */
    public static final String ARG_GROUPID = "groupid";

    /** _more_ */
    public static final String ARG_GROUP_CHILDREN = "group_children";

    /** _more_ */
    public static final String ARG_CREATEDATE = "createdate";

    /** _more_ */
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
    public static final String TYPE_TAG = "tag";

    /** _more_ */
    public static final String TYPE_ASSOCIATION = "association";

    /** _more_ */
    public static final String TYPE_GROUP = "group";


    /** _more_          */
    public static final String PROP_DOWNLOAD_OK = "jdms.download.ok";

    public static final String PROP_DEBUG = "jdms.debug";


    public static final String PROP_USER_REQUIRELOGIN = "jdms.user.requirelogin";

    /** _more_ */
    public static final String PROP_REQUEST_PATTERN = "jdms.request.pattern";

    public static final String PROP_REPOSITORY_NAME = "jdms.repository.name";


    /** _more_ */
    public static final String PROP_DB_CANCACHE = "jdms.db.cancache";

    /** _more_          */
    public static final String PROP_DB = "jdms.db";


    /** _more_ */
    public static final String PROP_DB_DERBY_HOME = "jdms.db.derby.home";

    /** _more_ */
    public static final String PROP_DB_DRIVER = "jdms.db.${db}.driver";

    /** _more_ */
    public static final String PROP_DB_URL = "jdms.db.${db}.url";

    /** _more_ */
    public static final String PROP_DB_USER = "jdms.db.${db}.user";

    /** _more_ */
    public static final String PROP_DB_PASSWORD = "jdms.db.${db}.password";





    /** _more_          */
    public static final String PROP_HARVESTERS_ACTIVE =
        "jdms.harvesters.active";

    /** _more_          */
    public static final String PROP_HARVESTERS_FILE = "jdms.harvesters.file";

    /** _more_ */
    public static final String PROP_DB_SCRIPT = "jdms.db.script";

    /** _more_ */
    public static final String PROP_TYPES = "jdms.types";

    /** _more_ */
    public static final String PROP_OUTPUT_FILES = "jdms.output.files";

    public static final String PROP_METADATA_FILES = "jdms.metadata.files";

    /** _more_ */
    public static final String PROP_NAVLINKS = "jdms.navlinks";

    /** _more_ */
    public static final String PROP_API = "jdms.api";



    /** _more_ */
    public static final String PROP_NAVSUBLINKS = "jdms.navsublinks";

    /** _more_ */
    public static final String PROP_SHOW_APPLET = "jdms.html.showapplet";

    /** _more_ */
    public static final String PROP_HTML_URLBASE = "jdms.html.urlbase";

    /** _more_ */
    public static final String PROP_HTML_TEMPLATE = "jdms.html.template";

    /** _more_ */
    public static final String PROP_HTML_IMAGEPLAYER =
        "jdms.html.imageplayer";

    /** _more_          */
    public static final String PROP_HTML_SLIDESHOW = "jdms.html.slideshow";

    /** _more_ */
    public static final String PROP_HTML_TIMELINEAPPLET =
        "jdms.html.timelineapplet";

    /** _more_ */
    public static final String PROP_HTML_MIMEPROPERTIES =
        "jdms.html.mimeproperties";

    /** _more_ */
    public static final String PROP_HTML_GRAPHAPPLET =
        "jdms.html.graphapplet";

    /** _more_ */
    public static final String PROP_HTML_GRAPHTEMPLATE =
        "jdms.html.graphtemplate";



    /** _more_ */
    public static final String PROP_DOWNLOAD_ASFILES =
        "jdms.download.asfiles";




    /** _more_ */
    public static final String TAG_OUTPUTHANDLER = "outputhandler";

    public static final String TAG_METADATAHANDLER = "metadatahandler";


    /** _more_ */
    public static final String ATTR_DB_NAME = "name";

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String ATTR_DB_DESCRIPTION = "description";





    /** _more_ */
    public static final String ARG_NODETYPE = "nodetype";

    /** _more_ */
    public static final String ARG_IMAGEWIDTH = "imagewidth";

    /** _more_          */
    public static final String ARG_IMAGEHEIGHT = "imageheight";

    /** _more_ */
    public static final String NODETYPE_ENTRY = "entry";

    /** _more_ */
    public static final String NODETYPE_GROUP = "group";

    /** _more_ */
    public static final String TAG_CATALOGREF = "catalogRef";

    /** _more_ */
    public static final String ATTR_XLINKTITLE = "xlink:title";

    /** _more_ */
    public static final String ATTR_XLINKHREF = "xlink:href";


    /** _more_ */
    public static final String ARG_WIDTH = "width";

    /** _more_          */
    public static final String ARG_RESOURCE = "resource";

    /** _more_          */
    public static final String ARG_DESCRIPTION = "description";


    /** _more_ */
    public static final String ARG_ADMIN_WHAT = "what";


    /** _more_          */
    public static final String ACTION_EDIT = "action.edit";

    /** _more_          */
    public static final String ACTION_DELETE_ASK = "action.delete.ask";

    /** _more_          */
    public static final String ACTION_DELETE_DOIT = "action.delete.doit";

    /** _more_          */
    public static final String ACTION_START = "start";

    /** _more_          */
    public static final String ACTION_STOP = "stop";







}

