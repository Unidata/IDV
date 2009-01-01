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


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


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
    public static final int MAX_ROWS = 100;


    /** _more_ */
    public static final String ICON_LCURVE = "/icons/blc.gif";

    public static final String ICON_LOG = "/icons/log.gif";

    /** _more_          */
    public static final String ICON_CATALOG = "/icons/catalog.gif";

    /** _more_ */
    public static final String ICON_CHAT = "/icons/chat.gif";

    /** _more_ */
    public static final String ICON_HEADER = "/images/header.jpg";

    /** _more_ */
    public static final String ICON_RCURVE = "/icons/brc.gif";

    /** _more_ */
    public static final String ICON_MAP = "/icons/map.gif";


    /** _more_ */
    public static final String ICON_CSV = "/icons/xls.png";

    /** _more_ */
    public static final String ICON_KML = "/icons/kml.png";

    /** _more_ */
    public static final String ICON_OPENDAP = "/icons/opendap.gif";

    /** _more_ */
    public static final String ICON_CART = "/icons/cart.gif";

    /** _more_ */
    public static final String ICON_CLOUD = "/icons/cloud.gif";

    public static final String ICON_CLOCK = "/icons/clock.gif";

    /** _more_ */
    public static final String ICON_LIST = "/icons/list.gif";

    /** _more_ */
    public static final String ICON_RANGE = "/icons/range.gif";

    /** _more_ */
    public static final String ICON_CALENDAR = "/icons/calendar.gif";

    public static final String ICON_DATEGRID = "/icons/dategrid.gif";

    /** _more_ */
    public static final String ICON_RSS = "/icons/rss.gif";

    /** _more_ */
    public static final String ICON_ZIP = "/icons/zip.png";

    /** _more_ */
    public static final String ICON_METADATA = "/icons/metadata.gif";

    /** _more_ */
    public static final String ICON_CLOSE = "/icons/close.gif";

    /** _more_ */
    public static final String ICON_MOVE = "/icons/move.gif";

    /** _more_ */
    public static final String ICON_COMMENTS = "/icons/comments.gif";

    /** _more_ */
    public static final String ICON_DOWNARROW = "/icons/downarrow.gif";

    /** _more_ */
    public static final String ICON_GRAYRECT = "/icons/grayrect.gif";

    /** _more_ */
    public static final String ICON_GRAYRECTARROW =
        "/icons/grayrectarrow.gif";

    /** _more_ */
    public static final String ICON_TOOLS = "/icons/tools.gif";

    /** _more_ */
    public static final String ICON_ERROR = "/icons/error.png";

    /** _more_ */
    public static final String ICON_PLUS = "/icons/plus.gif";

    /** _more_ */
    public static final String ICON_QUESTION = "/icons/question.png";

    /** _more_          */
    public static final String ICON_WIKI = "/icons/wiki.gif";

    /** _more_ */
    public static final String ICON_WARNING = "/icons/warning.png";

    /** _more_ */
    public static final String ICON_PROGRESS = "/icons/progress.gif";

    /** _more_ */
    public static final String ICON_INFORMATION = "/icons/information.gif";

    /** _more_ */
    public static final String ICON_RIGHTARROW = "/icons/rightarrow.gif";

    /** _more_ */
    public static final String ICON_FOLDER = "/icons/folder.gif";

    /** _more_ */
    public static final String ICON_FOLDER_OPEN = "/icons/folderopen.gif";

    /** _more_ */
    public static final String ICON_FOLDER_CLOSED = "/icons/folderclosed.gif";

    /** _more_ */
    public static final String ICON_FOLDER_CLOSED_LOCKED = "/icons/folderclosedlocked.gif";

    /** _more_ */
    public static final String ICON_FILE = "/icons/file.gif";

    /** _more_ */
    public static final String ICON_IMAGE = "/icons/image.gif";

    /** _more_ */
    public static final String ICON_DATA = "/icons/data.gif";

    /** _more_ */
    public static final String ICON_SUBSET = "/icons/subset.gif";


    /** _more_ */
    public static final String ICON_MINUS = "/icons/minus.gif";

    /** _more_ */
    public static final String ICON_ADD = "/icons/add.gif";

    /** _more_ */
    public static final String ICON_DELETE = "/icons/delete.gif";

    /** _more_ */
    public static final String ICON_FETCH = "/icons/fetch.gif";

    /** _more_ */
    public static final String ICON_SEARCH = "/icons/search.gif";

    /** _more_ */
    public static final String ICON_RIGHT = "/icons/right.gif";

    /** _more_ */
    public static final String ICON_ASSOCIATION = "/icons/association.gif";

    /** _more_ */
    public static final String ICON_GRAPH = "/icons/graph.gif";

    /** _more_ */
    public static final String ICON_EDIT = "/icons/edit.gif";

    /** _more_ */
    public static final String ICON_NEW = "/icons/new.gif";

    /** _more_ */
    public static final String ICON_ARROW = "/icons/arrow.gif";


    /** _more_ */
    public static final String ICON_BLANK = "/icons/blank.gif";

    /** _more_ */
    public static final String ICON_LEFT = "/icons/left.gif";


    /** _more_ */
    public static final String ATTR_PARENT = "parent";

    /** _more_ */
    public static final String ATTR_URL = "url";



    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_ */
    public static final String ATTR_FROMDATE = "fromdate";

    /** _more_          */
    public static final String ATTR_SELECTTYPE = "selecttype";


    /** _more_ */
    public static final String ATTR_TODATE = "todate";

    /** _more_ */
    public static final String ATTR_TARGET = "target";

    /** _more_ */
    public static final String ATTR_INPUTID = "inputid";

    /** _more_ */
    public static final String ATTR_NORTH = "north";

    /** _more_ */
    public static final String ATTR_SOUTH = "south";

    /** _more_ */
    public static final String ATTR_EAST = "east";

    /** _more_ */
    public static final String ATTR_WEST = "west";

    /** _more_ */
    public static final String ATTR_DATATYPE = "datatype";


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
    public static final String ATTR_RSS_VERSION = "version";



    /** _more_ */
    public static final String ATTR_FROM = "from";

    /** _more_ */
    public static final String ATTR_TO = "to";


    /** _more_ */
    public static final String ATTR_TITLE = "title";

    /** _more_ */
    public static final String ATTR_TOOLTIP = "tooltip";



    /** _more_ */
    public static final String TAG_TYPES = "types";

    /** _more_ */
    public static final String TAG_TYPE = "type";

    /** _more_ */
    public static final String ARG_DATASET = "dataset";


    public static final String ARG_HEIGHT = "height";

    /** _more_ */
    public static final String ARG_WIKI_CREATE = "wiki.create";
    public static final String ARG_WIKI_RAW = "wiki.raw";

    public static final String ARG_WIKI_DETAILS = "wiki.details";
    public static final String ARG_WIKI_VERSION = "wiki.version";
    public static final String ARG_WIKI_COMPARE1 = "wiki.compare1";
    public static final String ARG_WIKI_COMPARE2 = "wiki.compare2";


    public static final String ARG_WIKI_CHANGEDESCRIPTION = "wiki.changedescription";

    /** _more_ */
    public static final String ARG_WIKI_TEXT = "wiki.text";

    public static final String ARG_WIKI_EDITWITH = "wiki.editwith";

    /** _more_ */
    public static final String ARG_TYPE = "type";

    public static final String ARG_THUMBNAIL = "thumbnail";
    /** _more_ */
    public static final String ARG_VARIABLE = "variable";

    /** _more_ */
    public static final String ARG_ADDMETADATA = "addmetadata";

    /** _more_ */
    public static final String ATTR_ADDMETADATA = "addmetadata";

    /** _more_ */
    public static final String ARG_STEP = "step";

    /** _more_ */
    public static final String ARG_COLLECTION = "collection";

    /** _more_ */
    public static final String ARG_TEXT = "text";

    /** _more_ */
    public static final String ARG_EXACT = "exact";

    /** _more_ */
    public static final String ARG_DATATYPE = "datatype";

    /** _more_ */
    public static final String ARG_DATATYPE_SELECT = "datatype.select";

    /** _more_ */
    public static final String ARG_TYPE_EXCLUDE = "type.exclude";



    /** _more_ */
    public static final String ARG_RECURSE = "recurse";

    /** _more_ */
    public static final String ARG_REDIRECT = "redirect";

    /** _more_ */
    public static final String ARG_CATALOG = "catalog";

    /** _more_ */
    public static final String ARG_REQUIRED = "required";

    /** _more_ */
    public static final String ARG_COMMENT = "comment";

    /** _more_ */
    public static final String ARG_MOVE_CONFIRM = "move.confirm";

    /** _more_ */
    public static final String ARG_SUBJECT = "subject";

    /** _more_ */
    public static final String ARG_SUBMIT = "submit";



    /** _more_ */
    public static final String ARG_SOUTH = "south";

    /** _more_ */
    public static final String ARG_NORTH = "north";

    /** _more_ */
    public static final String ARG_EAST = "east";

    /** _more_ */
    public static final String ARG_WEST = "west";

    /** _more_ */
    public static final String ARG_WAIT = "wait";

    /** _more_ */
    public static final String ARG_SKIP = "skip";

    /** _more_ */
    public static final String ARG_ORDERBY = "orderby";

    /** _more_ */
    public static final String ARG_OK = "ok";

    /** _more_ */
    public static final String ARG_ASCENDING = "ascending";

    /** _more_ */
    public static final String ARG_AREA = "area";

    /** _more_ */
    public static final String ARG_AREA_SOUTH = ARG_AREA + "_south";

    /** _more_ */
    public static final String ARG_AREA_NORTH = ARG_AREA + "_north";

    /** _more_ */
    public static final String ARG_AREA_EAST = ARG_AREA + "_east";

    /** _more_ */
    public static final String ARG_AREA_WEST = ARG_AREA + "_west";


    /** _more_ */
    public static final String ARG_DATE = "date";

    /** _more_ */
    public static final String ARG_DATE_PATTERN = "date.pattern";

    /** _more_ */
    public static final String ARG_FORM_TYPE = "form.type";

    /** _more_ */
    public static final String ARG_FORM_ADVANCED = "form.advanced";

    /** _more_ */
    public static final String ARG_FORM_METADATA = "form.metadata";

    /** _more_ */
    public static final String ARG_CHANGE = "change";

    /** _more_ */
    public static final String ARG_ADMIN = "admin";



    /** _more_ */
    public static final String ARG_DELETE = "delete";

    /** _more_ */
    public static final String ARG_ADD = "add";

    /** _more_ */
    public static final String ARG_NEW = "new";

    /** _more_ */
    public static final String ARG_DELETE_CONFIRM = "delete.confirm";

    /** _more_ */
    public static final String ARG_CANCEL = "cancel";

    /** _more_ */
    public static final String ARG_CANCEL_DELETE = "canceldelete";

    /** _more_ */
    public static final String ARG_MESSAGE = "message";

    /** _more_ */
    public static final String ARG_MESSAGELEFT = "messageleft";

    /** _more_ */
    public static final String ARG_INCLUDENONGEO = "includenongeo";


    /** _more_ */
    public static final String ARG_NEXT = "next";

    /** _more_ */
    public static final String ARG_PREVIOUS = "previous";


    /** _more_ */
    public static final String ARG_APPLET = "applet";


    /** _more_ */
    public static final String ARG_AUTH_USER = "auth.user";

    /** _more_ */
    public static final String ARG_AUTH_PASSWORD = "auth.password";


    /** _more_ */
    public static final String ARG_SHOW_ASSOCIATIONS = "showassociations";


    public static final String ARG_FAVORITE_ADD = "user.favorite.add";
    public static final String ARG_FAVORITE_DELETE = "user.favorite.delete";
    public static final String ARG_FAVORITE_ID = "user.favorite.id";


    /** _more_ */
    public static final String ARG_USER_ID = "user.id";

    /** _more_ */
    public static final String ARG_SESSIONID = "sessionid";

    /** _more_ */
    public static final String ARG_ASSOCIATION = "association";

    /** _more_ */
    public static final String ARG_WHAT = "what";

    /** _more_ */
    public static final String ARG_ACTION_ID = "actionid";


    /** _more_ */
    public static final String ARG_ACTION = "action";

    /** _more_ */
    public static final String ARG_ROLES = "roles";

    /** _more_ */
    public static final String WHAT_ENTRIES = "entries";



    /** _more_ */
    public static final String WHAT_TYPE = "type";

    /** _more_ */
    public static final String WHAT_TAG = "tag";


    /** _more_ */
    public static final String WHAT_METADATA = "metadata";

    /** _more_ */
    public static final String WHAT_ASSOCIATION = "association";

    /** _more_ */
    public static final String WHAT_USER = "user";


    /** _more_ */
    public static final String ARG_MAX = "max";

    /** _more_ */
    public static final String ARG_SHOWMETADATA = "showmetadata";

    /** _more_ */
    public static final String ARG_COMMENTS = "showcomments";

    /** _more_ */
    public static final String ARG_OUTPUT = "output";

    /** _more_ */
    public static final String ARG_EMBEDDED = "embedded";

    /** _more_ */
    public static final String ARG_NAME = "name";

    /** _more_ */
    public static final String ARG_SEARCHMETADATA = "searchmetadata";

    /** _more_ */
    public static final String ARG_ENTRYID = "entryid";

    /** _more_ */
    public static final String ARG_ENTRYIDS = "entryids";

    /** _more_ */
    public static final String ARG_METADATA_ID = "metadata.id";

    /** _more_ */
    public static final String ARG_METADATA_INHERITED = "metadata.inherited";

    /** _more_ */
    public static final String ARG_METADATA_ATTR1 = "metadata.attr1";

    /** _more_ */
    public static final String ARG_METADATA_ATTR2 = "metadata.attr2";

    /** _more_ */
    public static final String ARG_METADATA_ATTR3 = "metadata.attr3";

    /** _more_ */
    public static final String ARG_METADATA_ATTR4 = "metadata.attr4";

    /** _more_ */
    public static final String ARG_METADATA_TYPE = "metadatatype";

    /** _more_ */
    public static final String ARG_EDIT_METADATA = "edit.metadata";

    /** _more_ */
    public static final String ARG_COMMENT_ID = "comment_id";

    /** _more_ */
    public static final String ARG_FROM = "from";

    /** _more_ */
    public static final String ARG_TO = "to";


    /** _more_ */


    /** _more_ */
    public static final String ARG_GROUP = "group";

    /** _more_ */
    public static final String ARG_TOPLEVEL = "toplevel";

    /** _more_ */
    public static final String ARG_GROUPID = "groupid";

    /** _more_ */
    public static final String ARG_GROUP_CHILDREN = "group_children";

    /** _more_ */
    public static final String ARG_CREATEDATE = "createdate";

    /** _more_ */
    public static final String ARG_QUERY = "query";

    /** _more_ */
    public static final String ARG_SQLFILE = "sqlfile";

    /** _more_ */
    public static final String ARG_TODATE = "todate";

    /** _more_ */
    public static final String ARG_CREATOR = "creator";

    /** _more_ */
    public static final String ARG_LABEL = "label";

    /** _more_ */
    public static final String ARG_LATEST = "latest";

    /** _more_ */
    public static final String ARG_LATESTOPENDAP = "latestopendap";

    /** _more_ */
    public static final String ARG_LAYOUT = "layout";

    public static final String ARG_LIMIT = "limit";

    /** _more_ */
    public static final String ARG_MINLAT = "minlat";

    /** _more_ */
    public static final String ARG_MINLON = "minlon";

    /** _more_ */
    public static final String ARG_MAXLAT = "maxlat";

    /** _more_ */
    public static final String ARG_MAXLON = "maxlon";


    /** _more_ */
    public static final String ARG_RELATIVEDATE = "relativedate";

    /** _more_ */
    public static final String ARG_FROMDATE = "fromdate";

    /** _more_ */
    public static final String ARG_FROMDATE_TIME = ARG_FROMDATE + ".time";

    /** _more_ */
    public static final String ARG_TODATE_TIME = ARG_TODATE + ".time";


    /** _more_ */
    public static final String ARG_FILESUFFIX = "filesuffix";

    /** _more_ */
    public static final String ARG_PRODUCT = "product";

    /** _more_ */
    public static final String ARG_STATION = "station";




    /** _more_ */
    public static final String TYPE_TAG = "tag";

    /** _more_ */
    public static final String TYPE_ASSOCIATION = "association";


    public static final String PROP_SSL_PORT = "ramadda.ssl.port";
    public static final String PROP_SSL_IGNORE = "ramadda.ssl.ignore";

    public static final String ARG_SSLOK = "sslok";


    /** _more_ */
    public static final String PROP_ADMIN_PHRASES = "ramadda.admin.phrases";

    /** _more_ */
    public static final String PROP_ADMIN_EMAIL = "ramadda.admin.email";

    /** _more_ */
    public static final String PROP_ADMIN_SMTP = "ramadda.admin.smtp";


    /** _more_ */
    public static final String PROP_VERSION = "ramadda.version";

    /** _more_ */
    public static final String PROP_LOCALFILEPATHS = "ramadda.localfilepaths";

    /** _more_ */
    public static final String PROP_HOSTNAME = "ramadda.hostname";

    /** _more_ */
    public static final String PROP_GOOGLEAPIKEYS = "ramadda.googleapikeys";

    /** _more_ */
    public static final String PROP_DOWNLOAD_OK = "ramadda.download.ok";

    /** _more_ */
    public static final String PROP_REPOSITORY_HOME = "ramadda_home";


    /** _more_ */
    public static final String PROP_DEBUG = "ramadda.debug";

    /** _more_ */
    public static final String PROP_DATEFORMAT = "ramadda.dateformat";


    /** _more_ */
    public static final String PROP_ACCESS_REQUIRELOGIN =
        "ramadda.access.requirelogin";

    /** _more_ */
    public static final String PROP_ACCESS_ADMINONLY =
        "ramadda.access.adminonly";

    /** _more_ */
    public static final String PROP_LANGUAGE = "ramadda.language";


    /** _more_ */
    public static final String PROP_HTML_FOOTER = "ramadda.html.footer";

    /** _more_ */
    public static final String PROP_REQUEST_PATTERN =
        "ramadda.request.pattern";

    /** _more_ */
    public static final String PROP_REPOSITORY_NAME =
        "ramadda.repository.name";


    /** _more_ */
    public static final String PROP_DB_CANCACHE = "ramadda.db.cancache";

    /** _more_ */
    public static final String PROP_DB = "ramadda.db";


    /** _more_ */
    public static final String PROP_DB_DERBY_HOME = "ramadda.db.derby.home";

    /** _more_ */
    public static final String PROP_DB_DRIVER = "ramadda.db.${db}.driver";

    /** _more_ */
    public static final String PROP_DB_URL = "ramadda.db.${db}.url";

    /** _more_ */
    public static final String PROP_DB_USER = "ramadda.db.${db}.user";

    /** _more_ */
    public static final String PROP_DB_PASSWORD = "ramadda.db.${db}.password";





    /** _more_ */
    public static final String PROP_HARVESTERS_ACTIVE =
        "ramadda.harvesters.active";

    /** _more_ */
    public static final String PROP_HARVESTERS = "ramadda.harvesters";

    /** _more_ */
    public static final String PROP_DB_SCRIPT = "ramadda.db.script";

    /** _more_ */
    public static final String PROP_TYPES = "ramadda.types";

    /** _more_ */
    public static final String PROP_OUTPUTHANDLERS = "ramadda.outputhandlers";

    /** _more_ */
    public static final String PROP_METADATAHANDLERS =
        "ramadda.metadatahandlers";

    /** _more_ */
    public static final String PROP_NAVLINKS = "ramadda.navlinks";

    /** _more_ */
    public static final String PROP_API = "ramadda.api";



    /** _more_ */
    public static final String PROP_NAVSUBLINKS = "ramadda.navsublinks";

    public static final String PROP_ENTRY_HEADER = "ramadda.entryheader";

    /** _more_ */
    public static final String PROP_SHOW_APPLET = "ramadda.html.showapplet";

    /** _more_ */
    public static final String PROP_HTML_URLBASE = "ramadda.html.urlbase";

    /** _more_ */
    public static final String PROP_HTML_TEMPLATE = "ramadda.html.template";

    public static final String PROP_HTML_TEMPLATES = "ramadda.html.templates";

    /** _more_ */
    public static final String PROP_HTML_IMAGEPLAYER =
        "ramadda.html.imageplayer";

    /** _more_ */
    public static final String PROP_HTML_SLIDESHOW = "ramadda.html.slideshow";

    /** _more_ */
    public static final String PROP_HTML_TIMELINEAPPLET =
        "ramadda.html.timelineapplet";

    /** _more_ */
    public static final String PROP_HTML_MIMEPROPERTIES =
        "ramadda.html.mimeproperties";

    /** _more_ */
    public static final String PROP_HTML_GRAPHAPPLET =
        "ramadda.html.graphapplet";

    /** _more_ */
    public static final String PROP_HTML_CHATAPPLET =
        "ramadda.html.chatapplet";

    /** _more_ */
    public static final String PROP_HTML_GRAPHTEMPLATE =
        "ramadda.html.graphtemplate";



    /** _more_ */
    public static final String PROP_DOWNLOAD_ASFILES =
        "ramadda.download.asfiles";





    /** _more_ */
    public static final String ATTR_DESCRIPTION = "description";

    /** _more_ */
    public static final String TAG_DESCRIPTION = "description";

    /** _more_ */
    public static final String ATTR_FILE = "file";

    /** _more_ */
    public static final String ATTR_CANDONEW = "candonew";

    /** _more_ */
    public static final String TAG_METADATA = "metadata";



    /** _more_ */
    public static final String TAG_OUTPUTHANDLER = "outputhandler";

    /** _more_ */
    public static final String TAG_METADATAHANDLER = "metadatahandler";


    /** _more_ */
    public static final String ATTR_DB_NAME = "name";

    /** _more_ */
    public static final String ATTR_CLASS = "class";

    /** _more_ */
    public static final String ATTR_DB_DESCRIPTION = "description";

    /** _more_ */
    public static final String ARG_NODETYPE = "nodetype";

    public static final String ARG_TEMPLATE = "template";

    /** _more_ */
    public static final String ARG_IMAGEWIDTH = "imagewidth";

    /** _more_ */
    public static final String ARG_IMAGEHEIGHT = "imageheight";

    /** _more_ */
    public static final String NODETYPE_ENTRY = "entry";

    /** _more_ */
    public static final String NODETYPE_GROUP = "group";


    /** _more_ */
    public static final String ARG_WIDTH = "width";

    /** _more_ */
    public static final String ARG_RESOURCE = "resource";

    /** _more_ */
    public static final String ARG_URL = "url";

    /** _more_ */
    public static final String ARG_RESOURCE_DOWNLOAD = "resource.download";

    /** _more_ */
    public static final String ARG_FILE = "file";

    /** _more_ */
    public static final String ARG_FILE_UNZIP = "file.unzip";

    /** _more_ */
    public static final String ARG_DESCRIPTION = "description";


    /** _more_ */
    public static final String ARG_ADMIN_INSTALLNOTICESHOWN =
        "admin.installnoticeshown";


    /** _more_ */
    public static final String ARG_ADMIN_LICENSEREAD = "admin.licenseread";

    /** _more_ */
    public static final String ARG_ADMIN_ADMINCREATED = "admin.admincreated";

    /** _more_ */
    public static final String ARG_ADMIN_INSTALLCOMPLETE =
        "admin.installcomplete";


    /** _more_ */
    public static final String ARG_ADMIN_WHAT = "what";


    /** _more_ */
    public static final String ACTION_EDIT = "action.edit";

    /** _more_ */
    public static final String ACTION_COPY = "action.copy";

    /** _more_ */
    public static final String ACTION_MOVE = "action.move";

    /** _more_ */
    public static final String ACTION_CLEAR = "action.clear";

    /** _more_ */
    public static final String ACTION_DELETE_ASK = "action.delete.ask";

    /** _more_ */
    public static final String ACTION_DELETE_DOIT = "action.delete.doit";

    /** _more_ */
    public static final String ACTION_START = "action.start";

    /** _more_ */
    public static final String ACTION_CLEARCACHE = "action.clearcache";

    /** _more_ */
    public static final String ACTION_STOP = "action.stop";

    /** _more_ */
    public static final String ACTION_ADD = "action.add";

    /** _more_ */
    public static final String ACTION_REMOVE = "action.remove";





    /** _more_ */
    public static final String NEWLINE = "\n";

    /** _more_ */
    public static final String BR = "<br>";

    /** _more_ */
    public static final String HR = "<hr>";

    /** _more_ */
    public static final String BLANK = "";


    /** _more_ */
    public static final String MIME_XML = "text/xml";


    /** _more_ */
    public static final String ATTR_CODE = "code";

    /** _more_ */
    public static final String TAG_RESPONSE = "response";


    /** _more_ */
    public static final String ARG_USER_DELETE_CONFIRM =
        "user.delete.confirm";

    /** _more_ */
    public static final String ARG_USER_DELETE = "user.delete";

    /** _more_ */
    public static final String ARG_FROMLOGIN = "user.fromlogin";


    /** _more_ */
    public static final String ARG_USER_CANCEL = "user.cancel";

    /** _more_ */
    public static final String ARG_USER_CHANGE = "user.change";

    /** _more_ */
    public static final String ARG_USER_NEW = "user.new";


    /** _more_ */
    public static final String ARG_USER_NAME = "user.name";

    /** _more_ */
    public static final String ARG_USER_ROLES = "user.roles";


    /** _more_ */
    public static final String ARG_USER_PASSWORDKEY = "user.passwordkey";


    /** _more_ */
    public static final String ARG_USER_PASSWORD = "user.password";

    /** _more_ */
    public static final String ARG_USER_PASSWORD1 = "user.password1";

    /** _more_ */
    public static final String ARG_USER_PASSWORD2 = "user.password2";

    /** _more_ */
    public static final String ARG_USER_EMAIL = "user.email";

    /** _more_ */
    public static final String ARG_USER_LANGUAGE = "user.language";

    /** _more_ */
    public static final String ARG_USER_QUESTION = "user.question";

    /** _more_ */
    public static final String ARG_USER_ANSWER = "user.answer";

    /** _more_ */
    public static final String ARG_USER_ADMIN = "user.admin";

    /** _more_ */
    public static final String ARG_USER_BULK = "user.bulk";




    /** _more_ */
    public static final String TAG_GROUPS = "groups";

    /** _more_ */
    public static final String TAG_GROUP = "group";

    /** _more_ */
    public static final String TAG_ENTRY = "entry";

    /** _more_ */
    public static final String TAG_ENTRIES = "entries";


    /** _more_ */
    public static final String ATTR_ID = "id";

    /** _more_ */
    public static final String ATTR_GROUP = "group";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_INHERITED = "inherited";

    /** _more_ */
    public static final String ATTR_ATTR1 = "attr1";

    /** _more_ */
    public static final String ATTR_ATTR2 = "attr2";

    /** _more_ */
    public static final String ATTR_ATTR3 = "attr3";

    /** _more_ */
    public static final String ATTR_ATTR4 = "attr4";

    /** _more_ */
    public static final String ATTR_RESOURCE = "resource";

    /** _more_ */
    public static final String ATTR_RESOURCE_TYPE = "resource_type";




    /** _more_ */
    public static final boolean DFLT_INHERITED = false;

    /** _more_ */
    public static final String ARG_YEAR = "year";

    /** _more_ */
    public static final String ARG_MONTH = "month";

    /** _more_ */
    public static final String ARG_DAY = "day";

    /** _more_ */
    public static final String ARG_SHOWYEAR = "showyear";


    /** _more_ */
    public static final String ARG_HARVESTER_ID = "harvester.id";

    /** _more_ */
    public static final String ARG_HARVESTER_CLASS = "harvester.class";

    /** _more_ */
    public static final String ARG_HARVESTER_REDIRECTTOEDIT =
        "harvester.redirecttoedit";

    /** _more_ */
    public static final String ARG_HARVESTER_GETXML = "harvester.getxml";

    /** _more_ */
    public static final String ARG_HARVESTER_XMLFILE = "harvester.xmlfile";



    /** _more_ */
    public static final String ARG_RESPONSETYPE = "responsetype";


    /** _more_ */
    public static final String ID_PREFIX_SYNTH = "synth:";

    /** _more_ */
    public static final int ENTRY_CACHE_LIMIT = 5000;




    /** _more_ */
    public static final String TYPE_ANY = "any";

    /** _more_ */
    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_GROUP = "group";

    /** _more_ */
    public static final String TYPE_CATALOG = "catalog";





    /** _more_ */
    public static final String PROP_USER_RESET_PASSWORD_TEMPLATE =
        "ramadda.user.reset.password.template";

    /** _more_ */
    public static final String PROP_USER_RESET_PASSWORD_SUBJECT =
        "ramadda.user.reset.password.subject";

    /** _more_ */
    public static final String PROP_USER_RESET_ID_TEMPLATE =
        "ramadda.user.reset.id.template";

    /** _more_ */
    public static final String PROP_USER_RESET_ID_SUBJECT =
        "ramadda.user.reset.id.subject";




}

