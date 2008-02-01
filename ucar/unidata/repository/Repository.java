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

import java.util.regex.*;
import java.util.zip.*;
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
public class Repository implements Constants, Tables, RequestHandler,
                                   RepositorySource {

    /** _more_ */
    public static final String GROUP_TOP = "Top";

    /** _more_ */
    public RequestUrl URL_GETMAP = new RequestUrl(this, "/getmap");

    /** _more_ */
    public RequestUrl URL_MESSAGE = new RequestUrl(this, "/message");


    /** _more_ */
    public RequestUrl URL_GROUP_SHOW = new RequestUrl(this, "/group/show");

    /** _more_ */
    public RequestUrl URL_GROUP_ADD = new RequestUrl(this, "/group/add");

    /** _more_ */
    public RequestUrl URL_GROUP_FORM = new RequestUrl(this, "/group/form");

    /** _more_ */
    public RequestUrl URL_ENTRY_SEARCHFORM = new RequestUrl(this,
                                                 "/entry/searchform");

    /** _more_ */
    public RequestUrl URL_ENTRY_COMMENTS = new RequestUrl(this,
                                               "/entry/comments");

    /** _more_ */
    public RequestUrl URL_ENTRY_SEARCH = new RequestUrl(this,
                                             "/entry/search");

    /** _more_ */
    public RequestUrl URL_ASSOCIATION_ADD = new RequestUrl(this,
                                                "/association/add");


    /** _more_ */
    public RequestUrl URL_LIST_HOME = new RequestUrl(this, "/list/home");

    /** _more_ */
    public RequestUrl URL_LIST_SHOW = new RequestUrl(this, "/list/show");

    /** _more_ */
    public RequestUrl URL_GRAPH_VIEW = new RequestUrl(this, "/graph/view");

    /** _more_ */
    public RequestUrl URL_GRAPH_GET = new RequestUrl(this, "/graph/get");

    /** _more_ */
    public RequestUrl URL_ENTRY_SHOW = new RequestUrl(this, "/entry/show");

    /** _more_ */
    public RequestUrl URL_ENTRY_DELETE = new RequestUrl(this,
                                             "/entry/delete");

    /** _more_ */
    public RequestUrl URL_ENTRY_CHANGE = new RequestUrl(this, "/entry/change");

    /** _more_ */
    public RequestUrl URL_ENTRY_FORM = new RequestUrl(this, "/entry/form");

    /** _more_ */
    public RequestUrl URL_ENTITY_FORM = new RequestUrl(this, "/entity/form");

    /** _more_ */
    public RequestUrl URL_GETENTRIES = new RequestUrl(this, "/getentries");

    /** _more_ */
    public RequestUrl URL_ENTRY_GET = new RequestUrl(this, "/entry/get");


    /** _more_ */
    private static final int PAGE_CACHE_LIMIT = 100;

    /** _more_ */
    private static final int ENTRY_CACHE_LIMIT = 100000;


    /** _more_ */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);


    /** _more_ */
    private List<EntryListener> entryListeners =
        new ArrayList<EntryListener>();


    /** _more_ */
    private Properties mimeTypes;

    /** _more_ */
    private Properties namesMap;



    /** _more_ */
    private Properties properties = new Properties();


    /** _more_ */
    Properties argProperties = new Properties();


    /** _more_ */
    private Properties localProperties = new Properties();

    /** _more_ */
    private String urlBase = "/repository";

    /** _more_ */
    private long baseTime = System.currentTimeMillis();

    /** _more_ */
    private int keyCnt = 0;





    /** _more_ */
    private Hashtable typeHandlersMap = new Hashtable();

    /** _more_ */
    private List<TypeHandler> typeHandlers = new ArrayList<TypeHandler>();

    /** _more_ */
    private List<OutputHandler> outputHandlers =
        new ArrayList<OutputHandler>();


    /** _more_ */
    private List<MetadataHandler> metadataHandlers =
        new ArrayList<MetadataHandler>();


    /** _more_ */
    private Hashtable resources = new Hashtable();



    /** _more_ */
    private Hashtable<String, Group> groupMap = new Hashtable<String,
                                                    Group>();


    /** _more_ */
    private Group topGroup;


    /** _more_ */
    private Object MUTEX_GROUP = new Object();

    /** _more_ */
    private Object MUTEX_KEY = new Object();

    /** _more_ */
    private Object MUTEX_INSERT = new Object();


    /** _more_ */
    List<String> typeDefinitionFiles;

    /** _more_ */
    List<String> apiDefFiles;

    /** _more_ */
    List<String> outputDefFiles;

    /** _more_ */
    List<String> metadataDefFiles;

    /** _more_ */
    private List<User> cmdLineUsers = new ArrayList();


    /** _more_ */
    String[] args;


    /** _more_ */
    private String hostname;

    /** _more_          */
    private int port;


    /** _more_ */
    private boolean clientMode = false;

    /** _more_ */
    private File logFile;

    /** _more_ */
    private OutputStream logFOS;

    /** _more_ */
    private boolean debug = false;

    /** _more_ */
    private UserManager userManager;

    private StorageManager storageManager;



    /** _more_ */
    private DatabaseManager databaseManager;

    /** _more_ */
    private Admin admin;

    /** _more_ */
    private GroupTypeHandler groupTypeHandler;

    /** _more_ */
    private Group dummyGroup;


    /** _more_ */
    private Hashtable pageCache = new Hashtable();

    /** _more_ */
    private List pageCacheList = new ArrayList();


    /** _more_ */
    private Hashtable entryCache = new Hashtable();


    /**
     * _more_
     *
     * @param args _more_
     * @param hostname _more_
     * @param port _more_
     *
     * @throws Exception _more_
     */
    public Repository(String[] args, String hostname, int port)
            throws Exception {
        this(args, hostname, port, false);
    }

    /**
     * _more_
     *
     * @param args _more_
     * @param hostname _more_
     * @param port _more_
     * @param clientMode _more_
     *
     * @throws Exception _more_
     */
    public Repository(String[] args, String hostname, int port,
                      boolean clientMode)
            throws Exception {
        this.clientMode = clientMode;
        this.args       = args;
        this.hostname   = hostname;
        this.port       = port;
    }

    /** _more_ */
    public static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    /** _more_ */
    private static SimpleDateFormat sdf;



    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     */
    public static String fmt(long dttm) {
        return fmt(new Date(dttm));
    }

    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     */
    public static String fmt(Date dttm) {
        if (sdf == null) {
            sdf = new SimpleDateFormat();
            sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
            sdf.applyPattern(DEFAULT_TIME_FORMAT);
        }

        if (dttm == null) {
            return "";
        }
        return sdf.format(dttm);
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void init() throws Exception {
        initProperties();
        if ( !clientMode) {
            initServer();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return this;
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected static String header(String h) {
        return "<div class=\"heading\">" + h + "</div>";
    }


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String note(String h) {
        return "<div class=\"notewrapper\"><span class=\"note\">" + h
               + "</span></div>";
    }


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String warning(String h) {
        return "<div class=\"notewrapper\"><table border=\"0\"><tr valign=\"bottom\"><td>"
               + HtmlUtil.img(fileUrl("/warning.jpg")) + HtmlUtil.space(1)
               + "</td><td><div class=\"note\">" + h
               + "</div></td></tr></table></div>";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected UserManager doMakeUserManager() {
        return new UserManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected StorageManager doMakeStorageManager() {
        return new StorageManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected DatabaseManager doMakeDatabaseManager() {
        return new DatabaseManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected Admin doMakeAdmin() {
        return new Admin(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected UserManager getUserManager() {
        if (userManager == null) {
            userManager = doMakeUserManager();
        }
        return userManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected StorageManager getStorageManager() {
        if (storageManager == null) {
            storageManager = doMakeStorageManager();
            storageManager.init();
        }
        return storageManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected DatabaseManager getDatabaseManager() {
        if (databaseManager == null) {
            databaseManager = doMakeDatabaseManager();
        }
        return databaseManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected Admin getAdmin() {
        if (admin == null) {
            admin = doMakeAdmin();
        }
        return admin;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initServer() throws Exception {
        getConnection();
        initTypeHandlers();
        initTable();
        initOutputHandlers();
        initMetadataHandlers();
        initApi();
        initUsers();
        initGroups();
        getAdmin().initHarvesters();
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initProperties() throws Exception {

        properties = new Properties();
        properties.load(
            IOUtil.getInputStream(
                "/ucar/unidata/repository/resources/repository.properties",
                getClass()));
        List<String> argEntryDefFiles    = new ArrayList();
        List<String> argApiDefFiles      = new ArrayList();
        List<String> argOutputDefFiles   = new ArrayList();
        List<String> argMetadataDefFiles = new ArrayList();

        for (int i = 0; i < args.length; i++) {
            if (args[i].endsWith(".properties")) {
                properties.load(IOUtil.getInputStream(args[i], getClass()));
            } else if (args[i].indexOf("api.xml") >= 0) {
                argApiDefFiles.add(args[i]);
            } else if (args[i].indexOf("types.xml") >= 0) {
                argEntryDefFiles.add(args[i]);
            } else if (args[i].indexOf("outputhandlers.xml") >= 0) {
                argOutputDefFiles.add(args[i]);
            } else if (args[i].indexOf("metadatahandlers.xml") >= 0) {
                argMetadataDefFiles.add(args[i]);
            } else if (args[i].equals("-admin")) {
                User user = new User(args[i + 1], true);
                user.setPassword(UserManager.hashPassword(args[i + 2]));
                cmdLineUsers.add(user);
                i += 2;
            } else if (args[i].equals("-port")) {
                //skip
                i++;
            } else if (args[i].startsWith("-D")) {
                String       s    = args[i].substring(2);
                List<String> toks = StringUtil.split(s, "=", true, true);
                if (toks.size() != 2) {
                    throw new IllegalArgumentException("Bad argument:"
                            + args[i]);
                }
                argProperties.put(toks.get(0), toks.get(1));
            } else {
                throw new IllegalArgumentException("Unknown argument: "
                        + args[i]);
            }
        }

        localProperties = new Properties();
        try {
            localProperties.load(
                IOUtil.getInputStream(
                    IOUtil.joinDir(getStorageManager().getRepositoryDir(), "repository.properties"),
                    getClass()));
        } catch (Exception exc) {}

        properties.putAll(localProperties);
        properties.putAll(argProperties);

        apiDefFiles = StringUtil.split(getProperty(PROP_API), ";", true,
                                       true);
        apiDefFiles.addAll(argApiDefFiles);

        typeDefinitionFiles = StringUtil.split(getProperty(PROP_TYPES));
        typeDefinitionFiles.addAll(argEntryDefFiles);

        outputDefFiles = StringUtil.split(getProperty(PROP_OUTPUT_FILES));
        outputDefFiles.addAll(argOutputDefFiles);

        metadataDefFiles = StringUtil.split(getProperty(PROP_METADATA_FILES));
        metadataDefFiles.addAll(argMetadataDefFiles);

        debug = getProperty(PROP_DEBUG, false);
        //        System.err.println ("debug:" + debug);

        urlBase = (String) properties.get(PROP_HTML_URLBASE);
        if (urlBase == null) {
            urlBase = "";
        }

        logFile = new File(IOUtil.joinDir(getStorageManager().getRepositoryDir(), "repository.log"));
        //TODO: Roll the log file
        logFOS = new FileOutputStream(logFile, true);


        String derbyHome = (String) properties.get(PROP_DB_DERBY_HOME);
        if (derbyHome != null) {
            derbyHome = derbyHome.replace("%userhome%",
                                          Misc.getSystemProperty("user.home",
                                              "."));
            File dir = new File(derbyHome);
            IOUtil.makeDirRecursive(dir);
            System.setProperty("derby.system.home", derbyHome);
        }

        mimeTypes = new Properties();
        for (Object mimeFile : StringUtil.split(
                getProperty(PROP_HTML_MIMEPROPERTIES), ";", true, true)) {
            mimeTypes.load(IOUtil.getInputStream((String) mimeFile,
                    getClass()));
        }

        sdf = new SimpleDateFormat();
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern(getProperty(PROP_DATEFORMAT, DEFAULT_TIME_FORMAT));
        TimeZone.setDefault(DateUtil.TIMEZONE_GMT);


    }





    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void readGlobals() throws Exception {
        Statement statement = execute(SqlUtil.makeSelect("*",
                                  Misc.newList(TABLE_GLOBALS)));

        ResultSet results = statement.getResultSet();
        while (results.next()) {
            properties.put(results.getString(1), results.getString(2));
        }


    }




    /**
     * _more_
     */
    protected void clearCache() {
        pageCache     = new Hashtable();
        pageCacheList = new ArrayList();
        entryCache    = new Hashtable();
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    protected void debug(String message) {
        if (debug) {
            log(message, null);
        }
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param message _more_
     */
    protected void log(Request request, String message) {
        log("user:" + request.getRequestContext().getUser() + " -- "
            + message);
    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    protected void log(String message, Throwable exc) {
        System.err.println(message);
        Throwable thr = null;
        if (exc != null) {
            thr = LogUtil.getInnerException(exc);
            thr.printStackTrace();
        }
        try {
            String line = new Date() + " -- " + message;
            logFOS.write(line.getBytes());
            logFOS.write("\n".getBytes());
            if (thr != null) {
                logFOS.write(LogUtil.getStackTrace(thr).getBytes());
                logFOS.write("\n".getBytes());
            }
            logFOS.flush();
        } catch (Exception exc2) {
            System.err.println("Error writing log:" + exc2);
        }
    }


    /**
     * _more_
     *
     * @param message _more_
     */
    protected void log(String message) {
        log(message, null);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initUsers() throws Exception {
        for (User user : cmdLineUsers) {
            getUserManager().makeOrUpdateUser(user, true);
        }
    }






    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    protected boolean isAppletEnabled(Request request) {
        if ( !getProperty(PROP_SHOW_APPLET, true)) {
            return false;
        }
        if (request != null) {
            return request.get(ARG_APPLET, true);
        }
        return true;
    }


    /** _more_ */
    Hashtable<String, ApiMethod> requestMap = new Hashtable();

    /** _more_ */
    ApiMethod homeApi;

    /** _more_ */
    ArrayList<ApiMethod> apiMethods = new ArrayList();

    /** _more_ */
    ArrayList<ApiMethod> topLevelMethods = new ArrayList();


    /**
     * _more_
     *
     * @param node _more_
     *
     * @throws Exception _more_
     */
    protected void addRequest(Element node) throws Exception {
        String  request = XmlUtil.getAttribute(node, ApiMethod.ATTR_REQUEST);
        String  methodName = XmlUtil.getAttribute(node,
                                 ApiMethod.ATTR_METHOD);
        boolean admin = XmlUtil.getAttribute(node, ApiMethod.ATTR_ADMIN,
                                             true);



        RequestHandler handler = this;
        if (XmlUtil.hasAttribute(node, ApiMethod.ATTR_HANDLER)) {
            String handlerName = XmlUtil.getAttribute(node,
                                     ApiMethod.ATTR_HANDLER);

            if (handlerName.equals("usermanager")) {
                handler = getUserManager();
            } else if (handlerName.equals("admin")) {
                handler = getAdmin();
            } else {
                Class c = Misc.findClass(handlerName);
                Constructor ctor = Misc.findConstructor(c,
                                       new Class[] { Repository.class,
                        Element.class });
                handler = (RequestHandler) ctor.newInstance(new Object[] {
                    this,
                    node });
            }
        }

        String    url       = getUrlBase() + request;
        ApiMethod oldMethod = requestMap.get(url);
        if (oldMethod != null) {
            requestMap.remove(url);
        }


        Class[] paramTypes = new Class[] { Request.class };
        Method method = Misc.findMethod(handler.getClass(), methodName,
                                        paramTypes);
        if (method == null) {
            throw new IllegalArgumentException("Unknown request method:"
                    + methodName);
        }
        ApiMethod apiMethod = new ApiMethod(
                                  handler, request,
                                  XmlUtil.getAttribute(
                                      node, ApiMethod.ATTR_NAME,
                                      request), method, admin,
                                          XmlUtil.getAttribute(
                                              node, ApiMethod.ATTR_CANCACHE,
                                              false), XmlUtil.getAttribute(
                                                  node,
                                                  ApiMethod.ATTR_TOPLEVEL,
                                                  false));
        if (XmlUtil.getAttribute(node, ApiMethod.ATTR_ISHOME, false)) {
            homeApi = apiMethod;
        }
        requestMap.put(url, apiMethod);
        if (oldMethod != null) {
            int index = apiMethods.indexOf(oldMethod);
            apiMethods.remove(index);
            apiMethods.add(index, apiMethod);
        } else {
            apiMethods.add(apiMethod);
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initApi() throws Exception {
        for (String file : apiDefFiles) {
            Element apiRoot  = XmlUtil.getRoot(file, getClass());
            List    children = XmlUtil.findChildren(apiRoot, TAG_METHOD);
            for (int i = 0; i < children.size(); i++) {
                Element node = (Element) children.get(i);
                addRequest(node);
            }
        }
        for (ApiMethod apiMethod : apiMethods) {
            if (apiMethod.getIsTopLevel()) {
                topLevelMethods.add(apiMethod);
            }
        }


    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initOutputHandlers() throws Exception {
        for (String file : outputDefFiles) {
            try {
                Element root  = XmlUtil.getRoot(file, getClass());
                List children = XmlUtil.findChildren(root, TAG_OUTPUTHANDLER);
                for (int i = 0; i < children.size(); i++) {
                    Element node = (Element) children.get(i);
                    Class c = Misc.findClass(XmlUtil.getAttribute(node,
                                  ATTR_CLASS));
                    Constructor ctor = Misc.findConstructor(c,
                                           new Class[] { Repository.class,
                            Element.class });
                    addOutputHandler(
                        (OutputHandler) ctor.newInstance(new Object[] { this,
                            node }));
                }
            } catch (Exception exc) {
                System.err.println("Error loading output handler file:"
                                   + file);
                throw exc;
            }

        }

        getUserManager().initOutputHandlers();
    }

    /**
     * _more_
     *
     * @param outputHandler _more_
     */
    public void addOutputHandler(OutputHandler outputHandler) {
        outputHandlers.add(outputHandler);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initMetadataHandlers() throws Exception {
        for (String file : metadataDefFiles) {
            try {
                Element root = XmlUtil.getRoot(file, getClass());
                List children = XmlUtil.findChildren(root,
                                    TAG_METADATAHANDLER);
                for (int i = 0; i < children.size(); i++) {
                    Element node = (Element) children.get(i);
                    Class c = Misc.findClass(XmlUtil.getAttribute(node,
                                  ATTR_CLASS));
                    Constructor ctor = Misc.findConstructor(c,
                                           new Class[] { Repository.class,
                            Element.class });
                    metadataHandlers.add(
                        (MetadataHandler) ctor.newInstance(
                            new Object[] { this,
                                           node }));
                }
            } catch (Exception exc) {
                System.err.println("Error loading metadata handler file:"
                                   + file);
                throw exc;
            }

        }
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleRequest(Request request) throws Exception {

        getUserManager().checkSession(request);


        long   t1 = System.currentTimeMillis();
        Result result;
        if (debug) {
            debug("user:" + request.getRequestContext().getUser() + " -- "
                  + request.toString());
        }
        try {
            result = getResult(request);
        } catch (Exception exc) {
            //TODO: For non-html outputs come up with some error format
            Throwable inner = LogUtil.getInnerException(exc);
            StringBuffer sb = new StringBuffer("An error has occurred:<pre>"
                                  + inner.getMessage() + "</pre>");
            if (request.getRequestContext().getUser().getAdmin()) {
                sb.append("<pre>" + LogUtil.getStackTrace(inner) + "</pre>");
            }
            result = new Result("Error", sb);
            log("Error handling request:" + request, exc);
        }

        if ((result != null) && (result.getInputStream() == null)
                && result.isHtml() && result.getShouldDecorate()) {
            result.putProperty(PROP_NAVLINKS, getNavLinks(request));
            decorateResult(request, result);
        }


        long t2 = System.currentTimeMillis();
        if ((result != null) && (t2 != t1)
                && (true || request.get("debug", false))) {
            if ((t2 - t1) > 100) {
                System.err.println("Time:" + request.getRequestPath() + " "
                                   + (t2 - t1));
            }
        }
        if ((result != null) && (request.getSessionId() != null)) {
            result.addCookie("repository-session", request.getSessionId());
        }
        return result;


    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result getResult(Request request) throws Exception {
        String incoming = request.getRequestPath().trim();
        if (incoming.endsWith("/")) {
            incoming = incoming.substring(0, incoming.length() - 1);
        }
        ApiMethod apiMethod = (ApiMethod) requestMap.get(incoming);
        if (apiMethod == null) {
            for (ApiMethod tmp : apiMethods) {
                String path = tmp.getRequest();
                if (path.endsWith("/*")) {
                    path = path.substring(0, path.length() - 2);
                    if (incoming.startsWith(getUrlBase() + path)) {
                        apiMethod = tmp;
                        break;
                    }
                }
            }
        }
        if ((apiMethod == null) && incoming.equals(getUrlBase())) {
            apiMethod = homeApi;
        }

        if (apiMethod == null) {
            return getHtdocsFile(request);
        }

        if ( !getUserManager().isRequestOk(request)
                || ((apiMethod != null)
                    && !apiMethod.isRequestOk(request, this))) {
            StringBuffer sb = new StringBuffer();
            sb.append("You cannot access this page<p>");
            sb.append(getUserManager().makeLoginForm(request,
                    HtmlUtil.hidden(ARG_REDIRECT, request.getFullUrl())));
            return new Result("Error", sb);
        }




        Result result = null;
        if (canCache() && apiMethod.getCanCache()) {
            result = (Result) pageCache.get(request);
            if (result != null) {
                pageCacheList.remove(request);
                pageCacheList.add(request);
                result.setShouldDecorate(false);
                return result;
            }
        }

        boolean cachingOk = canCache();

        if ( !getDatabaseManager().hasConnection()
                && !incoming.startsWith(getUrlBase() + "/admin")) {
            cachingOk = false;
            result = new Result("No Database",
                                new StringBuffer("Database is shutdown"));
        } else {
            result = (Result) apiMethod.invoke(request);
        }
        if (result == null) {
            return null;
        }

        if ((result.getInputStream() == null) && cachingOk
                && apiMethod.getCanCache()) {
            pageCache.put(request, result);
            pageCacheList.add(request);
            while (pageCacheList.size() > PAGE_CACHE_LIMIT) {
                Request tmp = (Request) pageCacheList.remove(0);
                pageCache.remove(tmp);
            }
        }

        return result;
    }

    /**
     * _more_
     *
     * @param path _more_
     */
    public static void checkFilePath(String path) {
        if (path.indexOf("..") >= 0) {
            throw new IllegalArgumentException("bad file path:" + path);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result getHtdocsFile(Request request) throws Exception {
        String path = request.getRequestPath();
        String type = getMimeTypeFromSuffix(IOUtil.getFileExtension(path));
        path = StringUtil.replace(path, getUrlBase(), "");
        if ((path.trim().length() == 0) || path.equals("/")) {
            log(request, "Unknown request: \"" + path + "\"");
            return new Result("Error",
                              new StringBuffer("Unknown request:\"" + path
                                  + "\""));
        }

        try {
            //Make sure no one is trying to access other files
            checkFilePath(path);
            InputStream is =
                IOUtil.getInputStream("/ucar/unidata/repository/htdocs"
                                      + path, getClass());
            Result result = new Result("", is, type);
            result.setCacheOk(true);
            return result;
        } catch (IOException fnfe) {
            log(request, "Unknown request:" + path);
            return new Result("Error",
                              new StringBuffer("Unknown request:" + path));
        }
    }

    /**
     * _more_
     *
     *
     * @param request _more_
     * @param result _more_
     *
     * @throws Exception _more_
     */
    protected void decorateResult(Request request, Result result)
            throws Exception {
        String template = getResource(PROP_HTML_TEMPLATE);
        String html = StringUtil.replace(template, "${content}",
                                         new String(result.getContent()));
        String userLink = getUserManager().getUserLinks(request);
        html = StringUtil.replace(html, "${userlink}", userLink);
        html = StringUtil.replace(html, "${repository_name}",
                                  getProperty(PROP_REPOSITORY_NAME,
                                      "Repository"));
        html = StringUtil.replace(html, "${footer}",
                                  getProperty(PROP_HTML_FOOTER, ""));
        html = StringUtil.replace(html, "${title}", result.getTitle());
        html = StringUtil.replace(html, "${root}", getUrlBase());


        List   links     = (List) result.getProperty(PROP_NAVLINKS);
        String linksHtml = HtmlUtil.space(1);
        if (links != null) {
            linksHtml = StringUtil.join("&nbsp;|&nbsp;", links);
        }
        List   sublinks     = (List) result.getProperty(PROP_NAVSUBLINKS);
        String sublinksHtml = "";
        if (sublinks != null) {
            sublinksHtml = StringUtil.join("\n&nbsp;|&nbsp;\n", sublinks);
        }

        html = StringUtil.replace(html, "${links}", linksHtml);
        if (sublinksHtml.length() > 0) {
            html = StringUtil.replace(html, "${sublinks}",
                                      "<div class=\"subnav\">" + sublinksHtml
                                      + "</div>");
        } else {
            html = StringUtil.replace(html, "${sublinks}", "");
        }
        result.setContent(html.getBytes());
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean canCache() {
        if (true) {
            return false;
        }
        return getProperty(PROP_DB_CANCACHE, true);
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getProperty(String name) {
        return (String) properties.get(name);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getProperty(String name, String dflt) {
        return Misc.getProperty(properties, name, dflt);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public boolean getProperty(String name, boolean dflt) {
        return Misc.getProperty(properties, name, dflt);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getConnection() throws Exception {
        return getDatabaseManager().getConnection();
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     *
     */
    protected void initTable() throws Exception {
        //Force a connection
        getConnection();
        String sql = IOUtil.readContents(getProperty(PROP_DB_SCRIPT),
                                         getClass());
        sql = getDatabaseManager().convertSql(sql);

        Statement statement = getConnection().createStatement();
        SqlUtil.loadSql(sql, statement, false);

        for (String file : typeDefinitionFiles) {
            Element entriesRoot = XmlUtil.getRoot(file, getClass());
            List children = XmlUtil.findChildren(entriesRoot,
                                GenericTypeHandler.TAG_TYPE);
            for (int i = 0; i < children.size(); i++) {
                Element entryNode = (Element) children.get(i);
                Class handlerClass =
                    Misc.findClass(XmlUtil.getAttribute(entryNode,
                        GenericTypeHandler.TAG_HANDLER,
                        "ucar.unidata.repository.GenericTypeHandler"));
                Constructor ctor = Misc.findConstructor(handlerClass,
                                       new Class[] { Repository.class,
                        Element.class });
                GenericTypeHandler typeHandler =
                    (GenericTypeHandler) ctor.newInstance(new Object[] { this,
                        entryNode });
                addTypeHandler(typeHandler.getType(), typeHandler);
            }
        }

        getUserManager().makeUserIfNeeded(new User("default", "Default User",
                false));
        getUserManager().makeUserIfNeeded(new User("anonymous", "Anonymous",
                false));

        readGlobals();

    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @throws Exception _more_
     */
    protected void writeGlobal(String name, String value) throws Exception {
        execute(SqlUtil.makeDelete(TABLE_GLOBALS, COL_GLOBALS_NAME,
                                   SqlUtil.quote(name)));
        execute(INSERT_GLOBALS, new Object[] { name, value });
        properties.put(name, value);
    }

    /**
     * _more_
     *
     * @param table _more_
     * @param where _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int getCount(String table, String where) throws Exception {
        Statement statement = execute(SqlUtil.makeSelect("count(*)",
                                  Misc.newList(table), where));

        ResultSet results = statement.getResultSet();
        if ( !results.next()) {
            return 0;
        }
        return results.getInt(1);
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param what _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getOutputTypesFor(Request request, String what)
            throws Exception {
        List types = new ArrayList();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.getOutputTypesFor(request, what, types);
        }
        return types;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getOutputTypesForGroup(Request request, Group group,
                                          List<Group> subGroups,
                                          List<Entry> entries)
            throws Exception {
        List types = new ArrayList();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.getOutputTypesForGroup(request, group, subGroups,
                    entries, types);
        }
        return types;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected List<OutputHandler> getOutputHandlers() {
        return outputHandlers;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getOutputTypesForEntries(Request request,
                                            List<Entry> entries)
            throws Exception {
        List list = new ArrayList();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.getOutputTypesForEntries(request, entries, list);
        }
        return list;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected OutputHandler getOutputHandler(Request request)
            throws Exception {
        for (OutputHandler outputHandler : outputHandlers) {
            if (outputHandler.canHandle(request)) {
                return outputHandler;
            }
        }
        throw new IllegalArgumentException(
            "Could not find output handler for: " + request.getOutput());
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initTypeHandlers() throws Exception {
        addTypeHandler(TypeHandler.TYPE_ANY,
                       new TypeHandler(this, TypeHandler.TYPE_ANY,
                                       "Any file type"));
        addTypeHandler(TypeHandler.TYPE_GROUP,
                       groupTypeHandler = new GroupTypeHandler(this));
        groupTypeHandler.putProperty("form.show." + ARG_RESOURCE, "false");
        addTypeHandler(TypeHandler.TYPE_FILE,
                       new TypeHandler(this, "file", "File"));
        dummyGroup = new Group(groupTypeHandler, true);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getGUID() {
        synchronized (MUTEX_KEY) {
            int key = keyCnt++;
            return baseTime + "_" + Math.random() + "_" + key;
        }
    }






    /**
     * _more_
     *
     * @param typeName _more_
     * @param typeHandler _more_
     */
    protected void addTypeHandler(String typeName, TypeHandler typeHandler) {
        typeHandlersMap.put(typeName, typeHandler);
        typeHandlers.add(typeHandler);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TypeHandler getTypeHandler(Request request) throws Exception {
        String type = request.getType(TypeHandler.TYPE_ANY).trim();
        return getTypeHandler(type, false, true);
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TypeHandler getTypeHandler(String type) throws Exception {
        return getTypeHandler(type, true, true);
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param makeNewOneIfNeeded _more_
     * @param useDefaultIfNotFound _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected TypeHandler getTypeHandler(String type,
                                         boolean makeNewOneIfNeeded,
                                         boolean useDefaultIfNotFound)
            throws Exception {
        TypeHandler typeHandler = (TypeHandler) typeHandlersMap.get(type);
        if (typeHandler == null) {
            if ( !useDefaultIfNotFound) {
                return null;
            }
            try {
                Class c = Misc.findClass("ucar.unidata.repository." + type);
                Constructor ctor = Misc.findConstructor(c,
                                       new Class[] { Repository.class,
                        String.class });
                typeHandler = (TypeHandler) ctor.newInstance(new Object[] {
                    this,
                    type });
            } catch (Throwable cnfe) {}
        }


        if (typeHandler == null) {
            if ( !makeNewOneIfNeeded) {
                return getTypeHandler(TypeHandler.TYPE_ANY);
            }
            typeHandler = new TypeHandler(this, type);
            addTypeHandler(type, typeHandler);
        }
        return typeHandler;
    }

    /**
     * _more_
     *
     *
     * @param andList _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(List andList) throws Exception {
        andList.add(SqlUtil.eq(COL_ENTRIES_TYPE,
                               SqlUtil.quote(TypeHandler.TYPE_GROUP)));
        String sql = SqlUtil.makeSelect(COL_ENTRIES_ID,
                                        Misc.newList(TABLE_ENTRIES),
                                        SqlUtil.makeAnd(andList));
        Statement statement = execute(sql);
        return getGroups(SqlUtil.readString(statement, 1));
    }

    /**
     * _more_
     *
     *
     * @param groupIds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(String[] groupIds) throws Exception {
        List<Group> groupList = new ArrayList<Group>();
        for (int i = 0; i < groupIds.length; i++) {
            Group group = findGroup(groupIds[i]);
            if (group != null) {
                groupList.add(group);
            }
        }
        return groupList;
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processMessage(Request request) throws Exception {
        return new Result(
            "",
            new StringBuffer(note(request.getUnsafeString(ARG_MESSAGE, ""))));
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processListHome(Request request) throws Exception {
        StringBuffer         sb           = new StringBuffer();
        List                 links        = getListLinks(request, "", false);
        TypeHandler          typeHandler  = getTypeHandler(request);
        List<TwoFacedObject> typeList     = new ArrayList<TwoFacedObject>();
        List<TwoFacedObject> specialTypes = typeHandler.getListTypes(false);
        if (specialTypes.size() > 0) {
            sb.append(HtmlUtil.bold(typeHandler.getDescription() + ":"));
        }
        typeList.addAll(specialTypes);
        /*
          if(typeList.size()>0) {
          sb.append("<ul>");
          for(TwoFacedObject tfo: typeList) {
          sb.append("<li>");
          sb.append(HtmlUtil.href(HtmlUtil.url(URL_LIST_SHOW,ARG_WHAT, tfo.getId(),ARG_TYPE,,typeHandler.getType()) , tfo.toString())));
          sb.append("\n");
          }
          sb.append("</ul>");
          }
          sb.append("<p><b>Basic:</b><ul><li>");
        */
        sb.append("<ul><li>");
        sb.append(StringUtil.join("<li>", links));
        sb.append("</ul>");


        Result result = new Result("Lists", sb);
        result.putProperty(PROP_NAVSUBLINKS, getListLinks(request, "", true));
        return result;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @param what _more_
     * @param includeExtra _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getListLinks(Request request, String what,
                                boolean includeExtra)
            throws Exception {
        List                 links       = new ArrayList();
        TypeHandler          typeHandler = getTypeHandler(request);
        List<TwoFacedObject> typeList    = typeHandler.getListTypes(false);
        String               extra1      = " class=subnavnolink ";
        String               extra2      = " class=subnavlink ";
        if ( !includeExtra) {
            extra1 = "";
            extra2 = "";
        }
        if (typeList.size() > 0) {
            for (TwoFacedObject tfo : typeList) {
                if (what.equals(tfo.getId())) {
                    links.add(HtmlUtil.span(tfo.toString(), extra1));
                } else {
                    links.add(HtmlUtil.href(HtmlUtil.url(URL_LIST_SHOW,
                            ARG_WHAT, (String) tfo.getId(), ARG_TYPE,
                            (String) typeHandler.getType()), tfo.toString(),
                                extra2));
                }
            }
        }
        String typeAttr = "";
        if ( !typeHandler.getType().equals(TypeHandler.TYPE_ANY)) {
            typeAttr = "&type=" + typeHandler.getType();
        }


        String[] whats = { WHAT_TYPE, WHAT_TAG, WHAT_ASSOCIATION };
        String[] names = { "Types", "Tags", "Associations" };
        for (int i = 0; i < whats.length; i++) {
            if (what.equals(whats[i])) {
                links.add(HtmlUtil.span(names[i], extra1));
            } else {
                links.add(HtmlUtil.href(HtmlUtil.url(URL_LIST_SHOW, ARG_WHAT,
                        whats[i]) + typeAttr, names[i], extra2));
            }
        }

        return links;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntrySearchForm(Request request) throws Exception {
        return processEntrySearchForm(request, false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param typeSpecific _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntrySearchForm(Request request,
                                         boolean typeSpecific)
            throws Exception {

        String       formType     = request.getString(ARG_FORM_TYPE, "basic");
        boolean      basicForm    = formType.equals("basic");




        StringBuffer sb           = new StringBuffer();
        StringBuffer headerBuffer = new StringBuffer();
        //        headerBuffer.append(header("Search Form"));
        request.remove(ARG_FORM_TYPE);
        String urlArgs = request.getUrlArgs();
        request.put(ARG_FORM_TYPE, formType);
        headerBuffer.append("<table cellpadding=\"5\">");
        String formLinks = "";
        if (basicForm) {
            formLinks =
                HtmlUtil.bold("Basic") + "&nbsp;|&nbsp;"
                + HtmlUtil.href(HtmlUtil.url(URL_ENTRY_SEARCHFORM,
                                             ARG_FORM_TYPE, "advanced") + "&"
                                                 + urlArgs, "Advanced");

        } else {
            formLinks = HtmlUtil.href(
                HtmlUtil.url(URL_ENTRY_SEARCHFORM, ARG_FORM_TYPE, "basic")
                + "&" + urlArgs, "Basic") + "&nbsp;|&nbsp;"
                                          + HtmlUtil.bold("Advanced");
        }

        headerBuffer.append(HtmlUtil.formEntry("Search:", formLinks));
        sb.append(HtmlUtil.form(HtmlUtil.url(URL_ENTRY_SEARCH, ARG_NAME,
                                             WHAT_ENTRIES)));

        sb.append(HtmlUtil.hidden(ARG_FORM_TYPE, formType));

        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HtmlUtil.submitImage(getUrlBase() + "/blank.gif",
                                       "submit"));
        TypeHandler typeHandler = getTypeHandler(request);

        String      what        = (String) request.getWhat("");
        if (what.length() == 0) {
            what = WHAT_ENTRIES;
        }

        List whatList = Misc.toList(new Object[] {
                            new TwoFacedObject("Entries", WHAT_ENTRIES),
                            new TwoFacedObject("Data Types", WHAT_TYPE),
                            new TwoFacedObject("Tags", WHAT_TAG),
                            new TwoFacedObject("Associations",
                                WHAT_ASSOCIATION) });
        whatList.addAll(typeHandler.getListTypes(true));

        String output     = (String) request.getOutput("");
        String outputHtml = "";
        if ( !basicForm) {
            outputHtml = HtmlUtil.span("Output Type: ",
                                       "class=\"formlabel\"");
            if (output.length() == 0) {
                outputHtml += HtmlUtil.select(ARG_OUTPUT,
                        getOutputTypesFor(request, what));
            } else {
                outputHtml += sb.append(HtmlUtil.hidden(ARG_OUTPUT, output));
            }
            String orderBy = HtmlUtil.space(2)
                             + HtmlUtil.checkbox(ARG_ASCENDING, "true",
                                 request.get(ARG_ASCENDING,
                                             false)) + " Sort ascending";
            outputHtml += orderBy;

        }




        if (what.length() == 0) {
            sb.append(HtmlUtil.formEntry("Search For:",
                                         HtmlUtil.select(ARG_WHAT,
                                             whatList)));

        } else {
            String label = TwoFacedObject.findLabel(what, whatList);
            label = StringUtil.padRight(label, 40, HtmlUtil.space(1));
            sb.append(HtmlUtil.formEntry("Search For:", label));
            sb.append(HtmlUtil.hidden(ARG_WHAT, what));
        }

        Object oldValue = request.remove(ARG_RELATIVEDATE);
        List   where    = typeHandler.assembleWhereClause(request);
        if (oldValue != null) {
            request.put(ARG_RELATIVEDATE, oldValue);
        }

        typeHandler.addToSearchForm(request, sb, where, basicForm);

        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit("Search", "submit")
                                     + " "
                                     + HtmlUtil.submit("Search Subset",
                                         "submit_subset") + HtmlUtil.space(2)
                                             + outputHtml));
        sb.append("</table>");
        sb.append("</form>");
        //        sb.append(IOUtil.readContents("/ucar/unidata/repository/resources/map.js",
        //                                         getClass()));


        headerBuffer.append(sb.toString());

        Result result = new Result("Search Form", headerBuffer);
        result.putProperty(PROP_NAVSUBLINKS,
                           getSearchFormLinks(request, what));
        return result;

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param urls _more_
     *
     * @return _more_
     */
    protected List getSubNavLinks(Request request, RequestUrl[] urls) {
        List   links    = new ArrayList();
        String extra    = " class=\"subnavlink\" ";
        String notextra = " class=\"subnavnolink\" ";
        String type     = request.getRequestPath();
        for (int i = 0; i < urls.length; i++) {
            String label = urls[i].getLabel();
            if (label == null) {
                label = urls[i].toString();
            }
            if (urls[i].toString().equals(type)) {
                links.add(HtmlUtil.span(label, notextra));
            } else {
                links.add(HtmlUtil.href(urls[i].toString(), label, extra));
            }
        }
        return links;
    }




    /**
     * _more_
     *
     *
     * @param request _more_
     * @param what _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getSearchFormLinks(Request request, String what)
            throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        links       = new ArrayList();
        String      extra1      = " class=subnavnolink ";
        String      extra2      = " class=subnavlink ";
        String[]    whats       = { WHAT_ENTRIES, WHAT_TAG,
                                    WHAT_ASSOCIATION };
        String[]    names       = { "Entries", "Tags", "Associations" };
        String      formType    = request.getString(ARG_FORM_TYPE, "basic");

        for (int i = 0; i < whats.length; i++) {
            String item;
            if (what.equals(whats[i])) {
                item = HtmlUtil.span(names[i], extra1);
            } else {
                item = HtmlUtil.href(HtmlUtil.url(URL_ENTRY_SEARCHFORM,
                        ARG_WHAT, whats[i], ARG_FORM_TYPE,
                        formType), names[i], extra2);
            }
            if (i == 0) {
                item = "<span " + extra1
                       + ">Search For:&nbsp;&nbsp;&nbsp; </span>" + item;
            }
            links.add(item);
        }

        List<TwoFacedObject> whatList = typeHandler.getListTypes(false);
        for (TwoFacedObject tfo : whatList) {
            if (tfo.getId().equals(what)) {
                links.add(HtmlUtil.span(tfo.toString(), extra1));
            } else {
                links.add(HtmlUtil.href(HtmlUtil.url(URL_ENTRY_SEARCHFORM,
                        ARG_WHAT, "" + tfo.getId(), ARG_TYPE,
                        typeHandler.getType()), tfo.toString(), extra2));
            }
        }

        return links;
    }



    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    protected List getNavLinks(Request request) {
        List    links    = new ArrayList();
        String  extra    = " class=navlink ";
        String  notextra = " class=navnolink ";
        boolean isAdmin  = false;
        if (request != null) {
            RequestContext context = request.getRequestContext();
            User           user    = context.getUser();
            isAdmin = user.getAdmin();
        }

        for (ApiMethod apiMethod : topLevelMethods) {
            if (apiMethod.getMustBeAdmin() && !isAdmin) {
                continue;
            }
            links.add(HtmlUtil.href(fileUrl(apiMethod.getRequest()),
                                    apiMethod.getName(), extra));
        }
        return links;
    }


    /** _more_ */
    private NavigatedMapPanel nmp;




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processMap(Request request) throws Exception {
        if (nmp == null) {
            nmp = new NavigatedMapPanel(
                Misc.newList(
                    NavigatedMapPanel.DEFAULT_MAP,
                    "/auxdata/maps/OUTLSUPU"), false);

        }

        synchronized (nmp) {
            double south  = request.get(ARG_SOUTH, 0.0);
            double north  = request.get(ARG_NORTH, 90.0);
            double east   = request.get(ARG_EAST, 180.0);
            double west   = request.get(ARG_WEST, -180.0);

            double width  = 4 * Math.abs(east - west);
            double height = 4 * Math.abs(north - south);
            if ( !request.get("noprojection", false)) {
                nmp.setProjectionImpl(new LatLonProjection("",
                        new ProjectionRect(west - width / 2,
                                           south - height / 2,
                                           east + width / 2,
                                           north + height / 2)));
            }
            nmp.getNavigatedPanel().setSize(request.get(ARG_IMAGEWIDTH, 200),
                                            request.get(ARG_IMAGEHEIGHT,
                                                200));
            nmp.getNavigatedPanel().setPreferredSize(new Dimension(200, 200));
            nmp.getNavigatedPanel().setBorder(
                BorderFactory.createLineBorder(Color.black));
            nmp.getNavigatedPanel().setEnabled(true);
            nmp.setDrawBounds(new LatLonPointImpl(north, west),
                              new LatLonPointImpl(south, east));

            Image image = ImageUtils.getImage(nmp.getNavigatedPanel());
            //        GuiUtils.showOkCancelDialog(null,"",new JLabel(new ImageIcon(image)),null);
            String                path = "foo.png";
            ByteArrayOutputStream bos  = new ByteArrayOutputStream();
            ImageUtils.writeImageToFile(image, path, bos, 1.0f);
            byte[] imageBytes = bos.toByteArray();
            return new Result("", imageBytes, getMimeTypeFromSuffix(".png"));
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public int getMax(Request request) {
        if (request.defined(ARG_SKIP)) {
            return request.get(ARG_SKIP, 0) + request.get(ARG_MAX, MAX_ROWS);
        }
        return request.get(ARG_MAX, MAX_ROWS);
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processListShow(Request request) throws Exception {
        String what   = request.getWhat(WHAT_TYPE);
        Result result = null;
        if (what.equals(WHAT_TAG)) {
            result = listTags(request);
        } else if (what.equals(WHAT_ASSOCIATION)) {
            result = listAssociations(request);
        } else if (what.equals(WHAT_TYPE)) {
            result = listTypes(request);
        } else {
            TypeHandler typeHandler = getTypeHandler(request);
            result = typeHandler.processList(request, what);
        }
        result.putProperty(PROP_NAVSUBLINKS,
                           getListLinks(request, what, true));
        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean canDownload(Request request, Entry entry)
            throws Exception {
        if ( !getProperty(PROP_DOWNLOAD_OK, false)) {
            return false;
        }
        entry = filterEntry(request, entry);
        if (entry == null) {
            return false;
        }
        if ( !entry.getTypeHandler().canDownload(request, entry)) {
            return false;
        }
        return getStorageManager().canDownload(request, entry);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryGet(Request request) throws Exception {
        String entryId = (String) request.getId((String) null);
        if (entryId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        Entry entry = getEntry(entryId, request);
        if (entry == null) {
            throw new IllegalArgumentException(
                "Could not find entry with id:" + entryId);
        }

        if ( !entry.getResource().isUrl()) {
            if ( !canDownload(request, entry)) {
                throw new IllegalArgumentException(
                    "Cannot download file with id:" + entryId);
            }
        }

        byte[] bytes;
        //        System.err.println("request:" + request);

        if (request.defined(ARG_IMAGEWIDTH)
                && ImageUtils.isImage(entry.getResource().getPath())) {
            int    width    = request.get(ARG_IMAGEWIDTH, 75);
            String thumbDir = getStorageManager().getThumbDir();
            String thumb = IOUtil.joinDir(thumbDir,
                                          "entry" + entry.getId() + "_"
                                          + width + ".jpg");
            if ( !new File(thumb).exists()) {
                Image image =
                    ImageUtils.readImage(entry.getResource().getPath());
                Image resizedImage = image.getScaledInstance(width, -1,
                                         Image.SCALE_AREA_AVERAGING);
                ImageUtils.waitOnImage(resizedImage);
                ImageUtils.writeImageToFile(resizedImage, thumb);
            }
            bytes = IOUtil.readBytes(IOUtil.getInputStream(thumb,
                    getClass()));
            return new Result(
                "", bytes,
                IOUtil.getFileExtension(entry.getResource().getPath()));
        } else {
            return new Result("",
                              IOUtil.getInputStream(entry.getResource()
                                  .getPath(), getClass()), IOUtil
                                      .getFileExtension(entry.getResource()
                                          .getPath()));
        }

    }



    /**
     * _more_
     *
     *
     * @param entryId _more_
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry getEntry(String entryId, Request request)
            throws Exception {
        Entry entry = (Entry) entryCache.get(entryId);
        if (entry != null) {
            return entry;
        }


        PreparedStatement entryStmt = null;
        if (entryStmt == null) {
            String query = SqlUtil.makeSelect(COLUMNS_ENTRIES,
                               Misc.newList(TABLE_ENTRIES),
                               SqlUtil.eq(COL_ENTRIES_ID, "?"));
            entryStmt = getConnection().prepareStatement(query);

        }

        /*
          String query = SqlUtil.makeSelect(COLUMNS_ENTRIES,
          Misc.newList(TABLE_ENTRIES),
          SqlUtil.eq(COL_ENTRIES_ID,
          SqlUtil.quote(entryId)));*/
        //        ResultSet results = execute(query).getResultSet();
        entryStmt.setString(1, entryId);
        entryStmt.execute();
        ResultSet results = entryStmt.getResultSet();
        if ( !results.next()) {
            return null;
        }
        TypeHandler typeHandler = getTypeHandler(results.getString(2));
        entry = filterEntry(request, typeHandler.getEntry(results));

        if (entry != null) {
            if (entryCache.size() > ENTRY_CACHE_LIMIT) {
                entryCache = new Hashtable();
            }
            entryCache.put(entryId, entry);
        }
        return entry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String makeGroupHeader(Request request, Group group)
            throws Exception {
        return HtmlUtil.bold("Group:") + HtmlUtil.space(1)
               + getBreadCrumbs(request, group, true, "")[1];
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntityForm(Request request) throws Exception {
        Group        group = findGroup(request, true);
        StringBuffer sb    = new StringBuffer();
        sb.append(makeGroupHeader(request, group));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.form(URL_ENTRY_FORM, ""));
        sb.append(HtmlUtil.formEntry(HtmlUtil.submit("Create new entry:"),
                                     makeTypeSelect(request, false)));
        sb.append(HtmlUtil.hidden(ARG_GROUP, group.getFullName()));
        sb.append("</form>");

        sb.append(makeNewGroupForm(request, group, ""));
        sb.append("</table>");

        return new Result("New Form", sb, Result.TYPE_HTML);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentGroup _more_
     * @param name _more_
     *
     * @return _more_
     */
    protected String makeNewGroupForm(Request request, Group parentGroup,
                                      String name) {
        StringBuffer sb = new StringBuffer();
        if ((parentGroup != null)
                && request.getRequestContext().getUser().getAdmin()) {
            sb.append(HtmlUtil.row(HtmlUtil.cols("<p>&nbsp;")));
            sb.append(
                HtmlUtil.form(
                    getAdmin().URL_ADMIN_IMPORT_CATALOG.toString()));
            sb.append(HtmlUtil.hidden(ARG_GROUP, parentGroup.getFullName()));
            sb.append(
                HtmlUtil.formEntry(
                    HtmlUtil.submit("Import catalog:"),
                    HtmlUtil.input(ARG_CATALOG, "", " size=\"75\"")
                    + HtmlUtil.space(1)
                    + HtmlUtil.checkbox(ARG_RECURSE, "true", false)
                    + " Recurse"));

            sb.append("</form>");
        }
        return sb.toString();
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryForm(Request request) throws Exception {

        Group  group = null;
        String type  = null;
        Entry  entry = null;
        if (request.defined(ARG_ID)) {
            entry = getEntry(request.getString(ARG_ID, ""), request);
            if (entry == null) {
                throw new IllegalArgumentException("Could not find entry:"
                        + request.getString(ARG_ID, ""));
            }
            if ((entry instanceof Group)
                    && (entry.getParentGroup() == null)) {
                throw new IllegalArgumentException(
                    "Cannot edit top-level group");
            }

            type  = entry.getTypeHandler().getType();
            group = entry.getParentGroup();
        }
        if (group == null) {
            group = findGroup(request, true);
        }
        if (type == null) {
            type = request.getType((String) null);
        }
        StringBuffer sb = new StringBuffer();
        sb.append(makeGroupHeader(request, group));
        sb.append(HtmlUtil.formTable());
        if (type == null) {
            sb.append(HtmlUtil.form(URL_ENTRY_FORM, ""));
        } else {
            sb.append(HtmlUtil.uploadForm(URL_ENTRY_CHANGE, ""));
        }

        String title = "";

        if (type == null) {
            sb.append(HtmlUtil.formEntry("Type:",
                                         makeTypeSelect(request, false)));

            sb.append(
                HtmlUtil.formEntry(
                    "", HtmlUtil.submit("Select Type to Add")));
            sb.append(HtmlUtil.hidden(ARG_GROUP, group.getFullName()));
        } else {
            TypeHandler typeHandler = ((entry == null)
                                       ? getTypeHandler(type)
                                       : entry.getTypeHandler());


            String submitButton = HtmlUtil.submit(title = ((entry == null)
                    ? "Add " + typeHandler.getLabel()
                    : "Edit " + typeHandler.getLabel()));

            String deleteButton = HtmlUtil.submit("Delete", ARG_DELETE);
            sb.append(HtmlUtil.formEntry("", submitButton+HtmlUtil.space(2)+deleteButton));
            if (entry != null) {
                sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
            } else {
                sb.append(HtmlUtil.hidden(ARG_TYPE, type));
                sb.append(HtmlUtil.hidden(ARG_GROUP, group.getFullName()));
            }
            sb.append(HtmlUtil.formEntry("Type:", typeHandler.getLabel()));

            String size = " size=\"75\" ";
            sb.append(HtmlUtil.formEntry("Name:",
                                         HtmlUtil.input(ARG_NAME,
                                             ((entry != null)
                    ? entry.getName()
                    : ""), size)));
            int rows = typeHandler.getProperty("form.rows.desc", 3);
            sb.append(
                HtmlUtil.formEntryTop(
                    "Description:",
                    HtmlUtil.textArea(ARG_DESCRIPTION, ((entry != null)
                    ? entry.getDescription()
                    : ""), rows, 50)));

            if (typeHandler.okToShowInForm(ARG_RESOURCE)) {
                if (entry == null) {
                    sb.append(HtmlUtil.formEntry("File:",
                            HtmlUtil.fileInput(ARG_FILE, size) +
                                                 HtmlUtil.checkbox(ARG_FILE_UNZIP,"true",false) +
                                                 " Unzip archive"));
                    sb.append(HtmlUtil.formEntry("Or URL:",
                            HtmlUtil.input(ARG_RESOURCE, "", size)));
                } else {
                    sb.append(HtmlUtil.formEntry("Resource:",
                            entry.getResource().getPath()));
                }
            }

            String dateHelp = " (e.g., 2007-12-11 00:00:00)";
            String fromDate = ((entry != null)
                               ? fmt(new Date(entry.getStartDate()))
                               : "");
            String toDate   = ((entry != null)
                               ? fmt(new Date(entry.getEndDate()))
                               : "");
            if (typeHandler.okToShowInForm(ARG_DATE)) {
                sb.append(
                    HtmlUtil.formEntry(
                        "Date Range:",
                        HtmlUtil.input(ARG_FROMDATE, fromDate, " size=30 ")
                        + " -- "
                        + HtmlUtil.input(ARG_TODATE, toDate, " size=30 ")
                        + dateHelp));
                if (entry == null) {
                    List datePatterns =  new ArrayList();

                    datePatterns.add(new TwoFacedObject("None",""));
                    for(int i=0;i<DateUtil.DATE_PATTERNS.length;i++) {
                        datePatterns.add(DateUtil.DATE_FORMATS[i]);
                    }

                    sb.append(HtmlUtil.formEntry(
                                                 "Date Pattern:",
                                                 HtmlUtil.select(ARG_DATE_PATTERN, datePatterns) +" (use file name)"));

                }
            }

            String tags = "";
            if (entry != null) {
                List<Tag> tagList = getTags(request, entry.getId());
                tags = StringUtil.join(",", tagList);
            }

            if (typeHandler.okToShowInForm(ARG_TAG)) {
                sb.append(
                    HtmlUtil.formEntry(
                        "Tags:",
                        HtmlUtil.input(ARG_TAG, tags, " size=\"20\" ")
                        + " (comma separated)"));
            }

            if (typeHandler.okToShowInForm(ARG_AREA)) {
                sb.append(HtmlUtil.formEntry("Location:",
                                             HtmlUtil.makeLatLonBox(ARG_AREA,
                                                 ((entry != null)
                                                     && entry.hasSouth())
                        ? entry.getSouth()
                        : Double.NaN, ((entry != null) && entry.hasNorth())
                                      ? entry.getNorth()
                                      : Double.NaN, ((entry != null)
                                      && entry.hasEast())
                        ? entry.getEast()
                        : Double.NaN, ((entry != null) && entry.hasWest())
                                      ? entry.getWest()
                                      : Double.NaN)));
            }

            typeHandler.addToEntryForm(request, sb, entry);
            sb.append(HtmlUtil.formEntry("", submitButton+HtmlUtil.space(2)+deleteButton));
        }
        sb.append("</table>\n");
        return new Result(title, sb, Result.TYPE_HTML);

    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getCommentHtml(Request request, Entry entry)
            throws Exception {
        StringBuffer  sb       = new StringBuffer();
        List<Comment> comments = getComments(request, entry);
        sb.append(HtmlUtil.form(URL_ENTRY_COMMENTS, ""));
        sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit("Add Comment",
                                         ARG_ADD)));
        sb.append("<table>");
        for (Comment comment : comments) {
            sb.append(HtmlUtil.formEntry("", "<hr>"));
            //TODO: Check for access
            String deleteLink = HtmlUtil.href(
                                    HtmlUtil.url(
                                        URL_ENTRY_COMMENTS, ARG_DELETE,
                                        "true", ARG_ID, entry.getId(),
                                        ARG_COMMENT_ID,
                                        comment.getId()), HtmlUtil.img(
                                            fileUrl("/Delete.gif"),
                                            "Delete comment"));
            sb.append(HtmlUtil.formEntry("", deleteLink));
            sb.append(HtmlUtil.formEntry("Subject:", comment.getSubject()));
            sb.append(HtmlUtil.formEntry("By:",
                                         comment.getUser().getLabel() + " @ "
                                         + fmt(comment.getDate())));
            sb.append(HtmlUtil.formEntryTop("Comment:",
                                            comment.getComment()));
        }
        return sb.toString();
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryComments(Request request) throws Exception {
        if ( !request.defined(ARG_ID)) {
            throw new IllegalArgumentException("Could not find entry:"
                    + request.getString(ARG_ID, ""));
        }
        Entry entry = getEntry(request.getString(ARG_ID, ""), request);
        if (entry == null) {
            throw new IllegalArgumentException("Could not find entry:"
                    + request.getString(ARG_ID, ""));
        }
        StringBuffer sb = new StringBuffer();

        if (request.exists(ARG_MESSAGE)) {
            sb.append(note(request.getUnsafeString(ARG_MESSAGE, "")));
        }

        if (request.exists(ARG_DELETE)) {
            execute(
                SqlUtil.makeDelete(
                    TABLE_COMMENTS, COL_COMMENTS_ID,
                    SqlUtil.quote(request.getString(ARG_COMMENT_ID, ""))));
            entry.setComments(null);
            return new Result(HtmlUtil.url(URL_ENTRY_COMMENTS, ARG_ID,
                                           entry.getId(), ARG_MESSAGE,
                                           "Comment deleted"));
        }

        if (request.exists(ARG_CANCEL)
                || ( !request.exists(ARG_SUBJECT)
                     && !request.exists(ARG_ADD))) {
            sb.append("Comments for: " + getEntryUrl(entry));
            sb.append("<p>");
            sb.append(getCommentHtml(request, entry));
            return new Result("Entry Comments", sb, Result.TYPE_HTML);
        }



        String subject = "";
        String comment = "";
        if (request.exists(ARG_SUBJECT)) {
            subject = request.getString(ARG_SUBJECT, "").trim();
            comment = request.getString(ARG_COMMENT, "").trim();
            if (comment.length() == 0) {
                sb.append(warning("Please enter a comment"));
            } else {
                PreparedStatement insert =
                    getConnection().prepareStatement(INSERT_COMMENTS);
                int col = 1;
                insert.setString(col++, getGUID());
                insert.setString(col++, entry.getId());
                insert.setString(
                    col++, request.getRequestContext().getUser().getId());
                insert.setTimestamp(col++,
                                    new java.sql.Timestamp(currentTime()),
                                    calendar);
                insert.setString(col++, subject);
                insert.setString(col++, request.getString(ARG_COMMENT, ""));
                insert.execute();
                entry.setComments(null);
                return new Result(HtmlUtil.url(URL_ENTRY_COMMENTS, ARG_ID,
                        entry.getId(), ARG_MESSAGE, "Comment added"));
            }
        }

        sb.append("Add comment for: " + getEntryUrl(entry));
        sb.append(HtmlUtil.form(URL_ENTRY_COMMENTS, ""));
        sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry("Subject:",
                                     HtmlUtil.input(ARG_SUBJECT, subject,
                                         " size=\"40\" ")));
        sb.append(HtmlUtil.formEntryTop("Comment:",
                                        HtmlUtil.textArea(ARG_COMMENT,
                                            comment, 5, 40)));
        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit("Add Comment")
                                     + HtmlUtil.space(2)
                                     + HtmlUtil.submit("Cancel",
                                         ARG_CANCEL)));
        sb.append("</table>");
        sb.append("</form>");
        return new Result("Entry Comments", sb, Result.TYPE_HTML);
    }






    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryDelete(Request request) throws Exception {
        String entryId = (String) request.getId((String) null);
        if (entryId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        //TODO: Check access here
        Entry entry = getEntry(entryId, request);
        if (entry == null) {
            throw new IllegalArgumentException("Could not find entry");
        }

        //TODO: Check if this entry is a group


        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        deleteEntries(request, entries);


        StringBuffer sb = new StringBuffer();
        sb.append("Entry Deleted");
        return new Result("Entry Deleted", sb, Result.TYPE_HTML);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processAssociationAdd(Request request) throws Exception {
        Entry fromEntry = getEntry(request.getString(ARG_FROM, ""), request);
        Entry toEntry   = getEntry(request.getString(ARG_TO, ""), request);
        if (fromEntry == null) {
            throw new IllegalArgumentException("Could not find entry:"
                    + request.getString(ARG_FROM, ""));
        }
        if (toEntry == null) {
            throw new IllegalArgumentException("Could not find entry:"
                    + request.getString(ARG_TO, ""));
        }
        String name = request.getString(ARG_NAME, (String) null);
        if (name != null) {
            PreparedStatement assocInsert =
                getConnection().prepareStatement(INSERT_ASSOCIATIONS);
            int col = 1;
            assocInsert.setString(col++, name);
            assocInsert.setString(col++, fromEntry.getId());
            assocInsert.setString(col++, toEntry.getId());
            assocInsert.execute();
            return new Result(HtmlUtil.url(URL_ENTRY_SHOW, ARG_ID,
                                           fromEntry.getId()));
        }

        StringBuffer sb = new StringBuffer();
        sb.append("Add association between " + fromEntry.getName());
        sb.append(" and  " + toEntry.getName());
        sb.append(HtmlUtil.form(URL_ASSOCIATION_ADD, ""));
        sb.append("Association Name: ");
        sb.append(HtmlUtil.input(ARG_NAME));
        sb.append(HtmlUtil.hidden(ARG_FROM, fromEntry.getId()));
        sb.append(HtmlUtil.hidden(ARG_TO, toEntry.getId()));
        sb.append(HtmlUtil.space(1));
        sb.append(HtmlUtil.submit("Add Association"));
        sb.append("</form>");

        return new Result("Add Association", sb);

    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryChange(Request request) throws Exception {

        Entry       entry = null;
        TypeHandler typeHandler;
        boolean     newEntry = true;
        if (request.defined(ARG_ID)) {
            entry    = getEntry(request.getString(ARG_ID, ""), request);
            newEntry = false;
            if (entry == null) {
                throw new IllegalArgumentException("Could not find entry:"
                        + request.getString(ARG_ID, ""));
            }



            if (request.exists(ARG_CANCEL)) {
                return new Result(HtmlUtil.url(URL_ENTRY_FORM, ARG_ID, entry.getId()));
            }


            if (request.exists(ARG_DELETE_CONFIRM)) {
                List<Entry> entries = new ArrayList<Entry>();
                entries.add(entry);
                deleteEntries(request, entries);
                Group group = entry.getParentGroup();
                return new Result(HtmlUtil.url(URL_ENTRY_SHOW, ARG_ID,
                                               group.getId(), ARG_MESSAGE,"Entry is deleted"));
            }

            if (request.exists(ARG_DELETE)) {
                StringBuffer sb = new StringBuffer();
                sb.append(HtmlUtil.form(URL_ENTRY_CHANGE, ""));
                sb.append("Are you sure you want to delete the entry: ");
                sb.append(entry.getName());
                sb.append("<p>");
                sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
                sb.append(HtmlUtil.submit("Yes", ARG_DELETE_CONFIRM));
                sb.append(HtmlUtil.space(3));
                sb.append(HtmlUtil.submit("Cancel", ARG_CANCEL));
                sb.append(HtmlUtil.formClose());
                return new Result("Entry delete confirm", sb);
            }
        }
        List<Entry> entries = new ArrayList<Entry>();

        if (entry == null) {
            List<String> resources = new ArrayList();
            List<String> origNames = new ArrayList();
            typeHandler =
                getTypeHandler(request.getType(TypeHandler.TYPE_ANY));
            String resource = request.getString(ARG_RESOURCE, "");
            String filename = request.getUploadedFile(ARG_FILE);
            boolean unzipArchive = false;
            boolean isFile = false;
            if (filename != null) {
                isFile = true;
                unzipArchive = request.get(ARG_FILE_UNZIP,false);
                resource = filename;
            }
            if(!unzipArchive) {
                resources.add(resource);
                origNames.add(request.getString(ARG_FILE, ""));
            } else {
                ZipInputStream zin =
                    new ZipInputStream(new FileInputStream(resource));
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    if(ze.isDirectory()) continue;
                    String name = IOUtil.getFileTail(ze.getName().toLowerCase());
                    File f = getStorageManager().getTmpFile(request, name);
                    FileOutputStream fos = new FileOutputStream(f);
                    IOUtil.writeTo(zin, fos);
                    fos.close();
                    resources.add(f.toString());
                    origNames.add(name);
                }
            }
            String groupName = request.getString(ARG_GROUP, (String) null);
            if (groupName == null) {
                throw new IllegalArgumentException(
                    "Must specify a parent group");
            }
            Group parentGroup = findGroupFromName(groupName,
                                    request.getRequestContext().getUser(),
                                    true);
            String description = request.getString(ARG_DESCRIPTION, "");

            Date createDate = new Date();
            Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE,
                                                    createDate);
            if (dateRange[0] == null) {
                dateRange[0] = ((dateRange[1] == null)
                                ? createDate
                                : dateRange[1]);
            }
            if (dateRange[1] == null) {
                dateRange[1] = dateRange[0];
            }

            for(int resourceIdx=0;resourceIdx<resources.size();resourceIdx++) {
                String theResource = (String) resources.get(resourceIdx);
                String origName = (String) origNames.get(resourceIdx);
                if(isFile) {
                    theResource = getStorageManager().moveToStorage(request, new File(theResource)).toString();
                }
                String name = request.getString(ARG_NAME,"");
                if (name.trim().length() == 0) {
                    name =  IOUtil.getFileTail(origName);
                }
                if (name.trim().length() == 0) {
                    throw new IllegalArgumentException("Must specify a name");
                }

                if (typeHandler.isType(TypeHandler.TYPE_GROUP)) {
                    if (name.indexOf("/") >= 0) {
                        throw new IllegalArgumentException(
                                                           "Cannot have a '/' in group name: '" + name + "'");
                    }

                    String tmp      = parentGroup.getFullName() + "/" + name;
                    Group  existing = findGroupFromName(tmp);
                    if (existing != null) {
                        throw new IllegalArgumentException(
                                                           "A group with the name: '" + tmp
                                                           + "' already exists");

                    }
                }

                String id = (typeHandler.isType(TypeHandler.TYPE_GROUP)
                             ? getGroupId(parentGroup)
                             : getGUID());

                Date[]theDateRange = {dateRange[0], dateRange[1]};

                if(request.defined(ARG_DATE_PATTERN)) {
                    String format = request.getUnsafeString(ARG_DATE_PATTERN,"");
                    String pattern = null;
                    for(int i=0;i<DateUtil.DATE_PATTERNS.length;i++) {
                        if(format.equals(DateUtil.DATE_FORMATS[i])) {
                            pattern = DateUtil.DATE_PATTERNS[i];
                            break;
                        }
                    }
                    System.err.println("format:" + format);
                    System.err.println("orignName:" + origName);
                    System.err.println("pattern:" + pattern);


                    if(pattern!=null) {
                        Pattern datePattern = Pattern.compile(pattern);
                        Matcher matcher = datePattern.matcher(origName);
                        if (matcher.find()) {
                            String dateString   = matcher.group(0);
                            SimpleDateFormat sdf = new SimpleDateFormat(format);
                            Date dttm  = sdf.parse(dateString);
                            theDateRange[0] = dttm;
                            theDateRange[1] = dttm;
                            System.err.println("got it");
                        } else {
                            System.err.println("not found");
                        }
                    }
                }


                entry = typeHandler.createEntry(id);
                entry.init(name, description, parentGroup,
                           request.getRequestContext().getUser(),
                           new Resource(theResource,Resource.TYPE_LOCALFILE), createDate.getTime(),
                           theDateRange[0].getTime(), theDateRange[1].getTime(), null);
                setEntryState(request, entry);
                entries.add(entry);
            }
        } else {
            Date createDate = new Date();
            Date[] dateRange = request.getDateRange(ARG_FROMDATE, ARG_TODATE,
                                                    createDate);
            String newName = request.getString(ARG_NAME, entry.getName());
            if (entry.getParentGroup() == null) {
                throw new IllegalArgumentException(
                    "Cannot edit top-level group");
            }
            if (entry instanceof Group) {
                if (newName.indexOf("/") >= 0) {
                    throw new IllegalArgumentException(
                        "Cannot have a '/' in group name:" + newName);
                }
                String tmp = entry.getParentGroup().getFullName() + "/"
                             + newName;
                Group existing = findGroupFromName(tmp);
                if ((existing != null)
                        && !existing.getId().equals(entry.getId())) {
                    throw new IllegalArgumentException(
                        "A group with the name:" + tmp + " already exists");

                }
            }

            entry.setName(newName);
            entry.setDescription(request.getString(ARG_DESCRIPTION,
                    entry.getDescription()));
            if (request.defined(ARG_RESOURCE)) {
                entry.setResource(
                    new Resource(request.getString(ARG_RESOURCE, "")));
            }
            if (dateRange[0] != null) {
                entry.setStartDate(dateRange[0].getTime());
            }
            if (dateRange[1] == null) {
                dateRange[1] = dateRange[0];
            }
            if (dateRange[1] != null) {
                entry.setEndDate(dateRange[1].getTime());
            }
            if (request.defined(ARG_TAG)) {
                //Get rid of the tags
                execute(SqlUtil.makeDelete(TABLE_TAGS, COL_TAGS_ENTRY_ID,
                                           SqlUtil.quote(entry.getId())));
            }
            setEntryState(request, entry);
            entries.add(entry);
        }


        insertEntries(entries, newEntry);
        if(entries.size()==1) {
            entry = (Entry) entries.get(0);
            return new Result(HtmlUtil.url(URL_ENTRY_SHOW, ARG_ID,
                                           entry.getId()));
        } else if(entries.size()>1) {
            entry = (Entry) entries.get(0);
            return new Result(HtmlUtil.url(URL_GROUP_SHOW, ARG_GROUP,
                                           entry.getParentGroup().getFullName(), ARG_MESSAGE, entries.size()+" files uploaded"));
        } else {
            return new Result("",new StringBuffer("No entries created"));
        }
    }



    private void setEntryState(Request request, Entry entry) throws Exception {
        entry.setSouth(request.get(ARG_AREA + "_south", entry.getSouth()));
        entry.setNorth(request.get(ARG_AREA + "_north", entry.getNorth()));
        entry.setWest(request.get(ARG_AREA + "_west", entry.getWest()));
        entry.setEast(request.get(ARG_AREA + "_east", entry.getEast()));

        if (request.defined(ARG_TAG)) {
            String tags = request.getString(ARG_TAG, "");
            entry.setTags(StringUtil.split(tags, ",", true, true));
        }

        entry.getTypeHandler().initializeEntry(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryShow(Request request) throws Exception {
        String entryId = (String) request.getId((String) null);
        if (entryId == null) {
            throw new IllegalArgumentException("No " + ARG_ID + " given");
        }
        Entry entry = getEntry(entryId, request);
        if (entry == null) {
            throw new IllegalArgumentException("Could not find entry");
        }

        if (entry instanceof Group) {
            return processGroupShow(request, (Group) entry);
        }


        //        System.err.println (request);
        if (request.get(ARG_NEXT, false)
                || request.get(ARG_PREVIOUS, false)) {
            boolean next = request.get(ARG_NEXT, false);
            List<String> ids = getEntryIdsInGroup(request,
                                   entry.getParentGroup(), new ArrayList());
            String nextId = null;
            for (int i = 0; (i < ids.size()) && (nextId == null); i++) {
                String id = ids.get(i);
                if (id.equals(entryId)) {
                    if (next) {
                        if (i == ids.size() - 1) {
                            nextId = ids.get(0);
                        } else {
                            nextId = ids.get(i + 1);
                        }
                    } else {
                        if (i == 0) {
                            nextId = ids.get(ids.size() - 1);
                        } else {
                            nextId = ids.get(i - 1);
                        }
                    }
                }
            }
            //Do a redirect
            if (nextId != null) {
                return new Result(HtmlUtil.url(URL_ENTRY_SHOW, ARG_ID,
                        nextId, ARG_OUTPUT,
                        request.getString(ARG_OUTPUT,
                                          OutputHandler.OUTPUT_HTML)));
            }
        }
        return getOutputHandler(request).outputEntry(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry filterEntry(Request request, Entry entry) throws Exception {
        if (entry.getResource().getType().equals(Resource.TYPE_FILE)) {
            if ( !entry.getResource().getFile().exists()) {
                //TODO                return null;
            }
        }
        //TODO: Check for access
        return entry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> filterEntries(Request request, List<Entry> entries)
            throws Exception {
        List<Entry> filtered = new ArrayList();
        for (Entry entry : entries) {
            entry = filterEntry(request, entry);
            if (entry != null) {
                filtered.add(entry);
            }
        }
        return filtered;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGetEntries(Request request) throws Exception {
        List<Entry> entries    = new ArrayList();
        boolean     doAll      = request.defined("getall");
        boolean     doSelected = request.defined("getselected");
        String      prefix     = (doAll
                                  ? "all_"
                                  : "entry_");

        for (Enumeration keys = request.keys(); keys.hasMoreElements(); ) {
            String id = (String) keys.nextElement();
            if (doSelected) {
                if ( !request.get(id, false)) {
                    continue;
                }
            }
            if ( !id.startsWith(prefix)) {
                continue;
            }
            id = id.substring(prefix.length());
            Entry entry = getEntry(id, request);
            if (entry != null) {
                entries.add(entry);
            }
        }
        String ids = request.getIds((String) null);
        if (ids != null) {
            List<String> idList = StringUtil.split(ids, ",", true, true);
            for (String id : idList) {
                Entry entry = getEntry(id, request);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        entries = filterEntries(request, entries);
        return getOutputHandler(request).outputEntries(request, entries);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected long currentTime() {
        return new Date().getTime();

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param checkEditAccess _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Group findGroup(Request request, boolean checkEditAccess)
            throws Exception {
        String groupNameOrId = (String) request.getString(ARG_GROUP,
                                   (String) null);
        if (groupNameOrId == null) {
            throw new IllegalArgumentException("No group specified");
        }
        Group group = findGroupFromName(groupNameOrId);
        if (group == null) {
            group = findGroup(groupNameOrId);
        }

        if (group == null) {
            throw new IllegalArgumentException("Could not find group:"
                    + groupNameOrId);
        }
        return group;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGroupAdd(Request request) throws Exception {
        StringBuffer sb       = new StringBuffer();
        Group        group    = null;
        String       fullName = null;
        String       newName  = request.getString(ARG_NAME, "");
        if ( !request.defined(ARG_GROUP)) {
            if ( !request.getRequestContext().getUser().getAdmin()) {
                throw new IllegalArgumentException(
                    "Cannot create a top level group");
            }
            fullName = newName;
        } else {
            group = findGroup(request, true);
            if (newName.length() == 0) {
                sb.append(makeGroupHeader(request, group));
                sb.append("<p>Need to specify a group name");
                sb.append(HtmlUtil.formTable());
                sb.append(makeNewGroupForm(request, group, ""));
                sb.append("</table>");
                return new Result("Add Group", sb);
            }
            fullName = group.getFullName() + "/" + newName;
        }
        Group newGroup = findGroupFromName(fullName);
        if (newGroup != null) {
            if (group != null) {
                sb.append(makeGroupHeader(request, group));
            }
            sb.append("<p>Given group name already exists");
            sb.append(HtmlUtil.formTable());
            sb.append(makeNewGroupForm(request, group, newName));
            sb.append("</table>");
            return new Result("Add Group", sb);
        }

        newGroup = findGroupFromName(fullName,
                                     request.getRequestContext().getUser(),
                                     true);
        return new Result(HtmlUtil.url(URL_GROUP_SHOW, ARG_GROUP,
                                       newGroup.getFullName()));
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGroupForm(Request request) throws Exception {
        Group        group = findGroup(request, true);
        StringBuffer sb    = new StringBuffer();

        if (request.defined(ACTION_EDIT)) {
            //TODO: put the change into the DB
            group.setName(request.getString(ARG_NAME, group.getName()));
        }

        sb.append(getBreadCrumbs(request, group, true, "")[1]);
        sb.append(HtmlUtil.space(2));
        sb.append(getAllGroupLinks(request, group));
        sb.append("<p>");
        sb.append("<table cellpadding=\"5\">");
        sb.append(HtmlUtil.form(URL_GROUP_FORM, ""));
        sb.append(HtmlUtil.hidden(ARG_GROUP, group.getFullName()));
        sb.append(HtmlUtil.formEntry("Name:",
                                     HtmlUtil.input(ARG_NAME,
                                         group.getName())));

        sb.append(HtmlUtil.formEntry("",
                                     HtmlUtil.submit("Edit Group",
                                         ACTION_EDIT)));
        sb.append("</form>");
        sb.append("</table>");
        return new Result("Group Form:" + group.getFullName(), sb,
                          Result.TYPE_HTML);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param makeLinkForLastGroup _more_
     * @param extraArgs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String[] getBreadCrumbs(Request request, Group group,
                                      boolean makeLinkForLastGroup,
                                      String extraArgs)
            throws Exception {
        return getBreadCrumbs(request, group, makeLinkForLastGroup,
                              extraArgs, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param makeLinkForLastGroup _more_
     * @param extraArgs _more_
     * @param stopAt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String[] getBreadCrumbs(Request request, Group group,
                                      boolean makeLinkForLastGroup,
                                      String extraArgs, Group stopAt)
            throws Exception {
        List   breadcrumbs = new ArrayList();
        List   titleList   = new ArrayList();
        Group  parent      = group.getParentGroup();
        String output      = ((request == null)
                              ? OutputHandler.OUTPUT_HTML
                              : request.getOutput());
        int    length      = 0;
        if (extraArgs.length() > 0) {
            extraArgs = "&" + extraArgs;
        }
        while (parent != null) {
            if ((stopAt != null)
                    && parent.getFullName().equals(stopAt.getFullName())) {
                break;
            }
            if (length > 100) {
                titleList.add(0, "...");
                breadcrumbs.add(0, "...");
                break;
            }
            String name = parent.getName();
            if (name.length() > 20) {
                name = name.substring(0, 19) + "...";
            }
            length += name.length();
            titleList.add(0, name);
            breadcrumbs.add(0, HtmlUtil.href(HtmlUtil.url(URL_GROUP_SHOW,
                    ARG_GROUP, parent.getFullName(), ARG_OUTPUT,
                    output) + extraArgs, name));
            parent = parent.getParentGroup();
        }
        titleList.add(group.getName());
        if (makeLinkForLastGroup) {
            breadcrumbs.add(HtmlUtil.href(HtmlUtil.url(URL_GROUP_SHOW,
                    ARG_GROUP, group.getFullName(), ARG_OUTPUT,
                    output), group.getName()));
        } else {
            breadcrumbs.add(HtmlUtil.bold(group.getName()) + "&nbsp;"
                            + getAllGroupLinks(request, group));
        }
        String title = "Group: "
                       + StringUtil.join("&nbsp;&gt;&nbsp;", titleList);
        return new String[] { title,
                              StringUtil.join("&nbsp;&gt;&nbsp;",
                              breadcrumbs) };
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getAllGroupLinks(Request request, Group group)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        for (OutputHandler outputHandler : getOutputHandlers()) {
            String links = outputHandler.getGroupLinks(request, group);
            if (links.length() > 0) {
                sb.append(links);
                sb.append(HtmlUtil.space(1));
            }
        }

        /*        sb.append(HtmlUtil.href(HtmlUtil.url(URL_GROUP_FORM, ARG_GROUP,
                  group.getFullName()),
                  HtmlUtil.img(fileUrl("/Edit16.gif"),"Edit Group")));
                  sb.append(HtmlUtil.space(1));*/




        return sb.toString();
    }






    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGroupShow(Request request) throws Exception {
        Group group = null;
        String groupName = (String) request.getString(ARG_GROUP,
                               (String) null);
        if (groupName != null) {
            group = findGroupFromName(groupName);
        }
        if (group == null) {
            group = topGroup;
        }

        return processGroupShow(request, group);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGroupShow(Request request, Group group)
            throws Exception {
        OutputHandler outputHandler = getOutputHandler(request);
        TypeHandler   typeHandler   = getTypeHandler(request);
        List          where         =
            typeHandler.assembleWhereClause(request);
        List<Group> subGroups =
            getGroups(Misc.newList(SqlUtil.eq(COL_ENTRIES_PARENT_GROUP_ID,
                SqlUtil.quote(group.getId()))));


        List<String> ids     = getEntryIdsInGroup(request, group, where);
        List<Entry>  entries = new ArrayList();
        for (String id : ids) {
            Entry entry = getEntry(id, request);
            if (entry != null) {
                entries.add(entry);
            }
        }
        entries = filterEntries(request, entries);
        return outputHandler.outputGroup(request, group, subGroups, entries);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param where _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<String> getEntryIdsInGroup(Request request, Group group,
            List where)
            throws Exception {
        where = new ArrayList(where);
        where.add(SqlUtil.eq(COL_ENTRIES_PARENT_GROUP_ID,
                             SqlUtil.quote(group.getId())));
        where.add(SqlUtil.neq(COL_ENTRIES_TYPE,
                              SqlUtil.quote(TypeHandler.TYPE_GROUP)));
        TypeHandler typeHandler = getTypeHandler(request);
        int    skipCnt     =  request.get(ARG_SKIP, 0);
        Statement statement = typeHandler.executeSelect(request,
                                                        COL_ENTRIES_ID, where,
                                                        getQueryOrderAndLimit(request));
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        List<String>     ids  = new ArrayList<String>();
        ResultSet        results;
        boolean canDoSelectOffset = getDatabaseManager().canDoSelectOffset();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                if ( !canDoSelectOffset && (skipCnt-- > 0)) {
                    continue;
                }
                ids.add(results.getString(1));
            }
        }
        return ids;
    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getResource(String id) throws Exception {
        String resource = (String) resources.get(id);
        if (resource != null) {
            return resource;
        }
        String fromProperties = getProperty(id);
        if (fromProperties != null) {
            resource = IOUtil.readContents(fromProperties, getClass());
        } else {
            resource = IOUtil.readContents(id, getClass());
        }
        if (resource != null) {
            //            resources.put(id,resource);
        }
        return resource;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGraphView(Request request) throws Exception {
        String graphAppletTemplate = getResource(PROP_HTML_GRAPHAPPLET);
        String type = request.getString(ARG_NODETYPE, NODETYPE_GROUP);
        String id                  = request.getId((String) null);

        if ((type == null) || (id == null)) {
            throw new IllegalArgumentException(
                "no type or id argument specified");
        }
        String html = StringUtil.replace(graphAppletTemplate, "${id}",
                                         encode(id));
        html = StringUtil.replace(html, "${root}", getUrlBase());
        html = StringUtil.replace(html, "${type}", encode(type));
        return new Result("Graph View", html.getBytes(), Result.TYPE_HTML);
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param results _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryNodeXml(Request request, ResultSet results)
            throws Exception {
        int         col         = 1;
        String      entryId     = results.getString(col++);
        String      name        = results.getString(col++);
        String      fileType    = results.getString(col++);
        String      groupId     = results.getString(col++);
        String      file        = results.getString(col++);
        TypeHandler typeHandler = getTypeHandler(request);
        String      nodeType    = typeHandler.getNodeType();
        if (ImageUtils.isImage(file)) {
            nodeType = "imageentry";
        }
        String attrs = XmlUtil.attrs(ATTR_TYPE, nodeType, ATTR_ID, entryId,
                                     ATTR_TITLE, name);
        if (ImageUtils.isImage(file)) {
            String imageUrl =
                HtmlUtil.url(URL_ENTRY_GET + entryId
                             + IOUtil.getFileExtension(file), ARG_ID,
                                 entryId, ARG_IMAGEWIDTH, "75");
            attrs = attrs + " " + XmlUtil.attr("image", imageUrl);
        }
        //        System.err.println (XmlUtil.tag(TAG_NODE,attrs));
        return XmlUtil.tag(TAG_NODE, attrs);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Tag> getTags(Request request, String entryId)
            throws Exception {
        String tagQuery = SqlUtil.makeSelect(COL_TAGS_NAME,
                                             Misc.newList(TABLE_TAGS),
                                             SqlUtil.eq(COL_TAGS_ENTRY_ID,
                                                 SqlUtil.quote(entryId)));
        String[]  tags    = SqlUtil.readString(execute(tagQuery), 1);
        List<Tag> tagList = new ArrayList();
        for (int i = 0; i < tags.length; i++) {
            tagList.add(new Tag(tags[i]));
        }
        return tagList;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processGraphGet(Request request) throws Exception {

        String  graphXmlTemplate = getResource(PROP_HTML_GRAPHTEMPLATE);
        String  id               = (String) request.getId((String) null);
        String  originalId       = id;
        String  type = (String) request.getString(ARG_NODETYPE,
                           (String) null);
        int     cnt              = 0;
        int     actualCnt        = 0;

        int     skip             = request.get(ARG_SKIP, 0);
        boolean haveSkip         = false;
        if (id.startsWith("skip_")) {
            haveSkip = true;
            //skip_tag_" +(cnt+skip)+"_"+id;
            List toks = StringUtil.split(id, "_", true, true);
            type = (String) toks.get(1);
            skip = new Integer((String) toks.get(2)).intValue();
            toks.remove(0);
            toks.remove(0);
            toks.remove(0);
            id = StringUtil.join("_", toks);
        }

        int MAX_EDGES = 15;
        if (id == null) {
            throw new IllegalArgumentException("Could not find id:"
                    + request);
        }
        if (type == null) {
            type = NODETYPE_GROUP;
        }
        TypeHandler  typeHandler = getTypeHandler(request);
        StringBuffer sb          = new StringBuffer();
        if (type.equals(TYPE_TAG)) {
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, TYPE_TAG, ATTR_ID,
                                      originalId, ATTR_TITLE, originalId)));

            String order = " DESC ";
            if (request.get(ARG_ASCENDING, false)) {
                order = " ASC ";
            }

            Statement stmt =
                typeHandler.executeSelect(
                    request,
                    SqlUtil.comma(
                        COL_ENTRIES_ID, COL_ENTRIES_NAME, COL_ENTRIES_TYPE,
                        COL_ENTRIES_PARENT_GROUP_ID,
                        COL_ENTRIES_RESOURCE), Misc.newList(
                            SqlUtil.eq(COL_TAGS_ENTRY_ID, COL_ENTRIES_ID),
                            SqlUtil.eq(
                                COL_TAGS_NAME,
                                SqlUtil.quote(id))), " order by " + COL_ENTRIES_FROMDATE + order);

            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            cnt       = 0;
            actualCnt = 0;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    cnt++;
                    if (cnt <= skip) {
                        continue;
                    }
                    actualCnt++;
                    sb.append(getEntryNodeXml(request, results));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE,
                                              "taggedby", ATTR_FROM,
                                                  originalId, ATTR_TO,
                                                      results.getString(1))));

                    if (actualCnt >= MAX_EDGES) {
                        String skipId = "skip_" + type + "_"
                                        + (actualCnt + skip) + "_" + id;
                        sb.append(XmlUtil.tag(TAG_NODE,
                                XmlUtil.attrs(ATTR_TYPE, "skip", ATTR_ID,
                                    skipId, ATTR_TITLE, "...")));
                        sb.append(XmlUtil.tag(TAG_EDGE,
                                XmlUtil.attrs(ATTR_TYPE, "etc", ATTR_FROM,
                                    originalId, ATTR_TO, skipId)));
                        break;
                    }
                }
            }
            String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                            sb.toString());
            xml = StringUtil.replace(xml, "${root}", getUrlBase());
            return new Result("", new StringBuffer(xml),
                              getMimeTypeFromSuffix(".xml"));
        }


        if ( !type.equals(TYPE_GROUP)) {
            Statement stmt = typeHandler.executeSelect(
                                 request,
                                 SqlUtil.comma(
                                     COL_ENTRIES_ID, COL_ENTRIES_NAME,
                                     COL_ENTRIES_TYPE,
                                     COL_ENTRIES_PARENT_GROUP_ID,
                                     COL_ENTRIES_RESOURCE), Misc.newList(
                                         SqlUtil.eq(
                                             COL_ENTRIES_ID,
                                             SqlUtil.quote(id))));

            ResultSet results = stmt.getResultSet();
            if ( !results.next()) {
                throw new IllegalArgumentException("Unknown entry id:" + id);
            }

            sb.append(getEntryNodeXml(request, results));

            List<Association> associations = getAssociations(request, id);
            for (Association association : associations) {
                Entry   other  = null;
                boolean isTail = true;
                if (association.getFromId().equals(id)) {
                    other  = getEntry(association.getToId(), request);
                    isTail = true;
                } else {
                    other  = getEntry(association.getFromId(), request);
                    isTail = false;
                }

                if (other != null) {
                    sb.append(
                        XmlUtil.tag(
                            TAG_NODE,
                            XmlUtil.attrs(
                                ATTR_TYPE,
                                other.getTypeHandler().getNodeType(),
                                ATTR_ID, other.getId(), ATTR_TITLE,
                                other.getName())));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE,
                                              "association", ATTR_FROM,
                                                  (isTail
                            ? id
                            : other.getId()), ATTR_TO, (isTail
                            ? other.getId()
                            : id))));
                }
            }


            Group group = findGroup(results.getString(4));
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                      ATTR_ID, group.getId(), ATTR_TOOLTIP,
                                      group.getName(), ATTR_TITLE,
                                      getGraphNodeTitle(group.getName()))));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, group.getId(), ATTR_TO,
                                      results.getString(1))));

            for (Tag tag : getTags(request, id)) {
                sb.append(XmlUtil.tag(TAG_NODE,
                                      XmlUtil.attrs(ATTR_TYPE, TYPE_TAG,
                                          ATTR_ID, tag.getName(), ATTR_TITLE,
                                          tag.getName())));
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "taggedby",
                                          ATTR_FROM, tag.getName(), ATTR_TO,
                                          id)));
            }




            String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                            sb.toString());

            xml = StringUtil.replace(xml, "${root}", getUrlBase());
            return new Result("", new StringBuffer(xml),
                              getMimeTypeFromSuffix(".xml"));
        }

        Group group = findGroup(id);
        if (group == null) {
            throw new IllegalArgumentException("Could not find group:" + id);
        }
        sb.append(
            XmlUtil.tag(
                TAG_NODE,
                XmlUtil.attrs(
                    ATTR_TYPE, NODETYPE_GROUP, ATTR_ID, group.getId(),
                    ATTR_TOOLTIP, group.getName(), ATTR_TITLE,
                    getGraphNodeTitle(group.getName()))));
        List<Group> subGroups =
            getGroups(Misc.newList(SqlUtil.eq(COL_ENTRIES_PARENT_GROUP_ID,
                SqlUtil.quote(group.getId()))));


        Group parent = group.getParentGroup();
        if (parent != null) {
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                      ATTR_ID, parent.getId(), ATTR_TOOLTIP,
                                      parent.getName(), ATTR_TITLE,
                                      getGraphNodeTitle(parent.getName()))));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, parent.getId(), ATTR_TO,
                                      group.getId())));
        }


        cnt       = 0;
        actualCnt = 0;
        for (Group subGroup : subGroups) {
            if (++cnt <= skip) {
                continue;
            }
            actualCnt++;

            sb.append(
                XmlUtil.tag(
                    TAG_NODE,
                    XmlUtil.attrs(
                        ATTR_TYPE, NODETYPE_GROUP, ATTR_ID, subGroup.getId(),
                        ATTR_TOOLTIP, subGroup.getName(), ATTR_TITLE,
                        getGraphNodeTitle(subGroup.getName()))));

            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, (haveSkip
                    ? originalId
                    : group.getId()), ATTR_TO, subGroup.getId())));

            if (actualCnt >= MAX_EDGES) {
                String skipId = "skip_" + type + "_" + (actualCnt + skip)
                                + "_" + id;
                sb.append(XmlUtil.tag(TAG_NODE,
                                      XmlUtil.attrs(ATTR_TYPE, "skip",
                                          ATTR_ID, skipId, ATTR_TITLE,
                                          "...")));
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "etc",
                                          ATTR_FROM, originalId, ATTR_TO,
                                          skipId)));
                break;
            }
        }

        String query = SqlUtil.makeSelect(
                           SqlUtil.comma(
                               COL_ENTRIES_ID, COL_ENTRIES_NAME,
                               COL_ENTRIES_TYPE, COL_ENTRIES_PARENT_GROUP_ID,
                               COL_ENTRIES_RESOURCE), Misc.newList(
                                   TABLE_ENTRIES), SqlUtil.eq(
                                   COL_ENTRIES_PARENT_GROUP_ID,
                                   SqlUtil.quote(group.getId())));
        SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
        ResultSet        results;
        cnt       = 0;
        actualCnt = 0;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                cnt++;
                if (cnt <= skip) {
                    continue;
                }
                actualCnt++;
                sb.append(getEntryNodeXml(request, results));
                String entryId = results.getString(1);
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                          ATTR_FROM, (haveSkip
                        ? originalId
                        : group.getId()), ATTR_TO, entryId)));
                sb.append("\n");
                if (actualCnt >= MAX_EDGES) {
                    String skipId = "skip_" + type + "_" + (actualCnt + skip)
                                    + "_" + id;
                    sb.append(XmlUtil.tag(TAG_NODE,
                                          XmlUtil.attrs(ATTR_TYPE, "skip",
                                              ATTR_ID, skipId, ATTR_TITLE,
                                                  "...")));
                    sb.append(XmlUtil.tag(TAG_EDGE,
                                          XmlUtil.attrs(ATTR_TYPE, "etc",
                                              ATTR_FROM, originalId, ATTR_TO,
                                                  skipId)));
                    break;
                }
            }
        }
        String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                        sb.toString());
        xml = StringUtil.replace(xml, "${root}", getUrlBase());
        return new Result("", new StringBuffer(xml),
                          getMimeTypeFromSuffix(".xml"));

    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    private String getGraphNodeTitle(String s) {
        if (s.length() > 40) {
            s = s.substring(0, 39) + "...";
        }
        return s;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param includeAny _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeTypeSelect(Request request, boolean includeAny)
            throws Exception {
        List<TypeHandler> typeHandlers = getTypeHandlers();
        List              tmp          = new ArrayList();
        for (TypeHandler typeHandler : typeHandlers) {
            if (typeHandler.isAnyHandler() && !includeAny) {
                continue;
            }
            tmp.add(new TwoFacedObject(typeHandler.getLabel(),
                                       typeHandler.getType()));
        }
        return HtmlUtil.select(ARG_TYPE, tmp);
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<TypeHandler> getTypeHandlers(Request request)
            throws Exception {
        List<TypeHandler> typeHandlers = new ArrayList<TypeHandler>();
        TypeHandler       typeHandler  = getTypeHandler(request);
        if ( !typeHandler.isAnyHandler()) {
            typeHandlers.add(typeHandler);
            return typeHandlers;
        }
        List where = typeHandler.assembleWhereClause(request);
        Statement stmt = typeHandler.executeSelect(request,
                             SqlUtil.distinct(COL_ENTRIES_TYPE), where);
        String[] types = SqlUtil.readString(stmt, 1);
        for (int i = 0; i < types.length; i++) {
            TypeHandler theTypeHandler = getTypeHandler(types[i]);

            if (types[i].equals(TypeHandler.TYPE_ANY)) {
                typeHandlers.add(0, theTypeHandler);

            } else {
                typeHandlers.add(theTypeHandler);
            }
        }
        return typeHandlers;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<TypeHandler> getTypeHandlers() throws Exception {
        return typeHandlers;
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTypes(Request request) throws Exception {
        List<TypeHandler> typeHandlers = getTypeHandlers(request);
        return getOutputHandler(request).listTypes(request, typeHandlers);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listTags(Request request) throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        if (where.size() == 0) {
            String type = (String) request.getType("").trim();
            if ((type.length() > 0) && !type.equals(TypeHandler.TYPE_ANY)) {
                typeHandler.addOr(COL_ENTRIES_TYPE, type, where, true);
            }
        }
        if (where.size() > 0) {
            where.add(SqlUtil.eq(COL_TAGS_ENTRY_ID, COL_ENTRIES_ID));
        }

        String[] tags = SqlUtil.readString(typeHandler.executeSelect(request,
                            SqlUtil.distinct(COL_TAGS_NAME), where,
                            " order by " + COL_TAGS_NAME), 1);

        List<Tag>     tagList = new ArrayList();
        List<String>  names   = new ArrayList<String>();
        List<Integer> counts  = new ArrayList<Integer>();
        ResultSet     results;
        int           max = -1;
        int           min = -1;
        for (int i = 0; i < tags.length; i++) {
            String tag = tags[i];
            Statement stmt2 = typeHandler.executeSelect(request,
                                  SqlUtil.count("*"),
                                  Misc.newList(SqlUtil.eq(COL_TAGS_NAME,
                                      SqlUtil.quote(tag))));

            ResultSet results2 = stmt2.getResultSet();
            if ( !results2.next()) {
                continue;
            }
            int count = results2.getInt(1);
            if ((max < 0) || (count > max)) {
                max = count;
            }
            if ((min < 0) || (count < min)) {
                min = count;
            }
            tagList.add(new Tag(tag, count));
        }

        return getOutputHandler(request).listTags(request, tagList);
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result listAssociations(Request request) throws Exception {
        return getOutputHandler(request).listAssociations(request);
    }




    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    protected String getMimeTypeFromSuffix(String suffix) {
        String type = (String) mimeTypes.get(suffix);
        if (type == null) {
            if (suffix.startsWith(".")) {
                suffix = suffix.substring(1);
            }
            type = (String) mimeTypes.get(suffix);
        }
        if (type == null) {
            type = "unknown";
        }
        return type;
    }




    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List assembleWhereClause(Request request) throws Exception {
        return getTypeHandler(request).assembleWhereClause(request);
    }







    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void initGroups() throws Exception {
        topGroup = findGroupFromName(GROUP_TOP,
                                     getUserManager().getDefaultUser(), true,
                                     true);

        Statement statement = execute(
                                  SqlUtil.makeSelect(
                                      COLUMNS_ENTRIES,
                                      Misc.newList(TABLE_ENTRIES),
                                      SqlUtil.eq(
                                          COL_ENTRIES_TYPE,
                                          SqlUtil.quote(
                                              TypeHandler.TYPE_GROUP))));
        readGroups(statement);
    }


    /**
     * _more_
     *
     * @param statement _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<Group> readGroups(Statement statement) throws Exception {
        ResultSet        results;
        SqlUtil.Iterator iter        = SqlUtil.getIterator(statement);
        List<Group>      groups      = new ArrayList<Group>();
        TypeHandler      typeHandler = getTypeHandler(TypeHandler.TYPE_GROUP);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                Group group = (Group) typeHandler.getEntry(results);
                groups.add(group);
                groupMap.put(group.getId(), group);
            }
        }
        for (Group group : groups) {
            if (group.getParentGroupId() != null) {
                Group parentGroup =
                    (Group) groupMap.get(group.getParentGroupId());
                group.setParentGroup(parentGroup);
            }
            groupMap.put(group.getFullName(), group);
        }
        return groups;
    }



    /**
     * _more_
     *
     * @param id _more_
     * @param tableName _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected boolean tableContains(String id, String tableName,
                                    String column)
            throws Exception {
        String query = SqlUtil.makeSelect(column, Misc.newList(tableName),
                                          SqlUtil.eq(column,
                                              SqlUtil.quote(id)));
        ResultSet results = execute(query).getResultSet();
        return results.next();
    }





    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroup(String id) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return null;
        }
        Group group = groupMap.get(id);
        if (group != null) {
            return group;
        }
        String query = SqlUtil.makeSelect(COLUMNS_ENTRIES,
                                          Misc.newList(TABLE_ENTRIES),
                                          SqlUtil.eq(COL_ENTRIES_ID,
                                              SqlUtil.quote(id)));
        Statement   statement = execute(query);
        List<Group> groups    = readGroups(statement);
        if (groups.size() > 0) {
            group = groups.get(0);
        } else {
            //????
            return null;
        }
        return group;
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroupFromName(String name) throws Exception {
        return findGroupFromName(name, null, false);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Group findGroupFromName(String name, User user,
                                      boolean createIfNeeded)
            throws Exception {
        return findGroupFromName(name, user, createIfNeeded, false);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param user _more_
     * @param createIfNeeded _more_
     * @param isTop _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Group findGroupFromName(String name, User user,
                                    boolean createIfNeeded, boolean isTop)
            throws Exception {
        synchronized (MUTEX_GROUP) {
            if ( !name.equals(GROUP_TOP)
                    && !name.startsWith(GROUP_TOP + "/")) {
                name = GROUP_TOP + "/" + name;
            }
            Group group = groupMap.get(name);
            if (group != null) {
                return group;
            }
            //            System.err.println("Looking for:" + name);

            List<String> toks = (List<String>) StringUtil.split(name, "/",
                                    true, true);
            Group  parent = null;
            String lastName;
            if ((toks.size() == 0) || (toks.size() == 1)) {
                lastName = name;
            } else {
                lastName = toks.get(toks.size() - 1);
                toks.remove(toks.size() - 1);
                parent = findGroupFromName(StringUtil.join("/", toks), user,
                                           createIfNeeded);
                if (parent == null) {
                    if ( !isTop) {
                        return null;
                    }
                    return topGroup;
                }
            }
            List where = new ArrayList();
            where.add(SqlUtil.eq(COL_ENTRIES_TYPE,
                                 SqlUtil.quote(TypeHandler.TYPE_GROUP)));
            if (parent != null) {
                where.add(SqlUtil.eq(COL_ENTRIES_PARENT_GROUP_ID,
                                     SqlUtil.quote(parent.getId())));
            } else {
                where.add(COL_ENTRIES_PARENT_GROUP_ID + " is null");
            }
            where.add(SqlUtil.eq(COL_ENTRIES_NAME, SqlUtil.quote(lastName)));


            String query = SqlUtil.makeSelect(COLUMNS_ENTRIES,
                               Misc.newList(TABLE_ENTRIES),
                               SqlUtil.makeAnd(where));
            Statement   statement = execute(query);
            List<Group> groups    = readGroups(statement);
            if (groups.size() > 0) {
                group = groups.get(0);
            } else {
                if ( !createIfNeeded) {
                    return null;
                }
                TypeHandler typeHandler =
                    getTypeHandler(TypeHandler.TYPE_GROUP);
                group = new Group(getGroupId(parent), typeHandler);
                group.setName(lastName);
                group.setParentGroup(parent);
                group.setUser(user);
                group.setDate(new Date().getTime());
                addNewEntry(group);
            }
            groupMap.put(group.getId(), group);
            groupMap.put(group.getFullName(), group);
            return group;
        }
    }

    /**
     * _more_
     *
     * @param parent _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getGroupId(Group parent) throws Exception {

        int    baseId = 0;
        String idWhere;
        if (parent == null) {
            idWhere = COL_ENTRIES_PARENT_GROUP_ID + " IS NULL ";
        } else {
            idWhere = SqlUtil.eq(COL_ENTRIES_PARENT_GROUP_ID,
                                 SqlUtil.quote(parent.getId()));
        }
        String newId = null;
        while (true) {
            if (parent == null) {
                newId = "" + baseId;
            } else {
                newId = parent.getId() + Group.IDDELIMITER + baseId;
            }
            ResultSet idResults = execute(
                                      SqlUtil.makeSelect(
                                          COL_ENTRIES_ID,
                                          Misc.newList(TABLE_ENTRIES),
                                          idWhere + " AND "
                                          + SqlUtil.eq(
                                              COL_ENTRIES_ID,
                                              SqlUtil.quote(
                                                  newId)))).getResultSet();

            if ( !idResults.next()) {
                break;
            }
            baseId++;
        }
        return newId;

    }


    /**
     * _more_
     *
     * @param insert _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    protected void execute(String insert, Object[] values) throws Exception {
        PreparedStatement pstmt = getConnection().prepareStatement(insert);
        for (int i = 0; i < values.length; i++) {
            //Assume null is a string
            if (values[i] == null) {
                pstmt.setNull(i + 1, java.sql.Types.VARCHAR);
            } else if (values[i] instanceof Date) {
                pstmt.setTimestamp(
                    i + 1,
                    new java.sql.Timestamp(((Date) values[i]).getTime()),
                    calendar);
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
        } catch (Exception exc) {
            System.err.println("Error:" + insert);
            throw exc;
        }
    }


    protected String getQueryOrderAndLimit(Request request) {
        String      order       = " DESC ";
        if (request.get(ARG_ASCENDING, false)) {
            order = " ASC ";
        }
        int    skipCnt     = request.get(ARG_SKIP, 0);
        String limitString = "";
        //        if (request.defined(ARG_SKIP)) {
        //            if (skipCnt > 0) {
        int max = request.get(ARG_MAX, MAX_ROWS);
        limitString = getDatabaseManager().getLimitString(skipCnt, max);
        String orderBy = "";
        if(request.defined(ARG_ORDERBY)) {
            String by  = request.getString(ARG_ORDERBY,"");
            if(by.equals("fromdate")) {
                orderBy = " ORDER BY " + COL_ENTRIES_FROMDATE + order;
            } else if(by.equals("todate")) {
                orderBy = " ORDER BY " + COL_ENTRIES_FROMDATE + order;
            } else if(by.equals("name")) {
                orderBy = " ORDER BY " + COL_ENTRIES_NAME + order;
            }
        } else {
            orderBy = " ORDER BY " + COL_ENTRIES_FROMDATE + order;
        }
        System.err.println ("limit:" + limitString);


        return orderBy   + limitString;
        //            }
        //        }

    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List[] getEntries(Request request) throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List        where       = typeHandler.assembleWhereClause(request);
        int    skipCnt     = request.get(ARG_SKIP, 0);
        Statement statement = typeHandler.executeSelect(request,
                                                        COLUMNS_ENTRIES, where,
                                                        getQueryOrderAndLimit(request));



        List<Entry>      entries = new ArrayList<Entry>();
        List<Entry>      groups  = new ArrayList<Entry>();
        ResultSet        results;
        SqlUtil.Iterator iter     = SqlUtil.getIterator(statement);
        boolean canDoSelectOffset = getDatabaseManager().canDoSelectOffset();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                if ( !canDoSelectOffset && (skipCnt-- > 0)) {
                    continue;
                }
                //id,type,name,desc,group,user,file,createdata,fromdate,todate
                TypeHandler localTypeHandler =
                    getTypeHandler(results.getString(2));
                Entry entry = localTypeHandler.getEntry(results);
                if (entry instanceof Group) {
                    groups.add(entry);
                } else {
                    entries.add(entry);
                }
            }
        }
        return new List[] { filterEntries(request, groups),
                            filterEntries(request, entries) };
    }




    /** _more_ */
    private Hashtable namesHolder = new Hashtable();


    /**
     * _more_
     *
     * @param fieldValue _more_
     * @param namesFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getFieldDescription(String fieldValue, String namesFile)
            throws Exception {
        return getFieldDescription(fieldValue, namesFile, null);
    }



    /**
     * _more_
     *
     * @param namesFile _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Properties getFieldProperties(String namesFile)
            throws Exception {
        if (namesFile == null) {
            return null;
        }
        Properties names = (Properties) namesHolder.get(namesFile);
        if (names == null) {
            try {
                names = new Properties();
                InputStream s = IOUtil.getInputStream(namesFile, getClass());
                names.load(s);
                namesHolder.put(namesFile, names);
            } catch (Exception exc) {
                System.err.println("err:" + exc);
                throw exc;
            }
        }
        return names;
    }


    /**
     * _more_
     *
     * @param fieldValue _more_
     * @param namesFile _more_
     * @param dflt _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getFieldDescription(String fieldValue, String namesFile,
                                         String dflt)
            throws Exception {
        if (namesFile == null) {
            return dflt;
        }
        String s = (String) getFieldProperties(namesFile).get(fieldValue);
        if (s == null) {
            return dflt;
        }
        return s;
    }



    /**
     * _more_
     *
     * @param product _more_
     *
     * @return _more_
     */
    protected String getLongName(String product) {
        return getLongName(product, product);
    }

    /**
     * _more_
     *
     * @param product _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    protected String getLongName(String product, String dflt) {
        if (namesMap == null) {
            namesMap = new Properties();
            try {
                InputStream s =
                    IOUtil.getInputStream(
                        "/ucar/unidata/repository/resources/names.properties",
                        getClass());
                namesMap.load(s);
            } catch (Exception exc) {
                System.err.println("err:" + exc);
            }
        }
        String name = (String) namesMap.get(product);
        if (name != null) {
            return name;
        }
        //        System.err.println("not there:" + product+":");
        return dflt;
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String encode(String s) throws Exception {
        return java.net.URLEncoder.encode(s, "UTF-8");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param tag _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getTagLinks(Request request, String tag)
            throws Exception {
        String search = HtmlUtil.href(
                            HtmlUtil.url(
                                URL_ENTRY_SEARCHFORM, ARG_TAG,
                                java.net.URLEncoder.encode(
                                    tag, "UTF-8")), HtmlUtil.img(
                                        fileUrl("/Search16.gif"),
                                        "Search in tag"));

        if (isAppletEnabled(request)) {
            search += HtmlUtil.href(HtmlUtil.url(URL_GRAPH_VIEW, ARG_ID, tag,
                    ARG_NODETYPE,
                    TYPE_TAG), HtmlUtil.img(fileUrl("/tree.gif"),
                                            "Show tag in graph"));
        }
        return search;
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryUrl(Entry entry) {
        return HtmlUtil.href(HtmlUtil.url(URL_ENTRY_SHOW, ARG_ID,
                                          entry.getId()), entry.getName());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entryId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Association> getAssociations(Request request,
            String entryId)
            throws Exception {
        String query = SqlUtil.makeSelect(
                           COLUMNS_ASSOCIATIONS,
                           Misc.newList(TABLE_ASSOCIATIONS),
                           SqlUtil.eq(
                               COL_ASSOCIATIONS_FROM_ENTRY_ID,
                               SqlUtil.quote(entryId)) + " OR "
                                   + SqlUtil.eq(
                                       COL_ASSOCIATIONS_TO_ENTRY_ID,
                                       SqlUtil.quote(entryId)));
        List<Association> associations = new ArrayList();
        SqlUtil.Iterator  iter         = SqlUtil.getIterator(execute(query));
        ResultSet         results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                associations.add(new Association(results.getString(1),
                        results.getString(2), results.getString(3)));
            }
        }
        return associations;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Comment> getComments(Request request, Entry entry)
            throws Exception {
        if (entry.getComments() != null) {
            return entry.getComments();
        }
        String query = SqlUtil.makeSelect(
                           COLUMNS_COMMENTS, Misc.newList(
                               TABLE_COMMENTS), SqlUtil.eq(
                               COL_COMMENTS_ENTRY_ID, SqlUtil.quote(
                                   entry.getId())), " order by "
                                       + COL_COMMENTS_DATE + " asc ");
        List<Comment>    comments = new ArrayList();
        SqlUtil.Iterator iter     = SqlUtil.getIterator(execute(query));
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                comments
                    .add(new Comment(results
                        .getString(1), entry, getUserManager()
                        .findUser(results
                            .getString(3), true), new Date(results
                            .getTimestamp(4, Repository.calendar)
                            .getTime()), results.getString(5), results
                                .getString(6)));
            }
        }
        entry.setComments(comments);
        return comments;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Permission> getPermissions(Request request, Entry entry)
            throws Exception {
        if (entry.getPermissions() != null) {
            return entry.getPermissions();
        }
        String query =
            SqlUtil.makeSelect(COLUMNS_PERMISSIONS,
                               Misc.newList(TABLE_PERMISSIONS),
                               SqlUtil.eq(COL_PERMISSIONS_ENTRY_ID,
                                          SqlUtil.quote(entry.getId())));

        List<Permission> permissions = new ArrayList();
        SqlUtil.Iterator iter        = SqlUtil.getIterator(execute(query));
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {

                /**
                 * *                permissions.add(new Permission(results.getString(1),entry,
                 *                        getUserManager().findUser(results.getString(3),true),
                 *                        new Date(results.getTimestamp(4 , Repository.calendar ).getTime()),
                 *                        results.getString(5),
                 *                        results.getString(6)));
                 */
            }
        }
        entry.setPermissions(permissions);
        return permissions;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param association _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getAssociationLinks(Request request, String association)
            throws Exception {
        if (true) {
            return "";
        }
        String search = HtmlUtil.href(
                            HtmlUtil.url(
                                URL_ENTRY_SEARCHFORM, ARG_ASSOCIATION,
                                encode(association)), HtmlUtil.img(
                                    fileUrl("/Search16.gif"),
                                    "Search in association"));

        return search;
    }



    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String absoluteUrl(String url) {
        return "http://" + hostname + ":" + port + url;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getHostname() {
        return hostname;
    }


    /**
     * _more_
     *
     * @param hostname _more_
     * @param port _more_
     */
    public void setHostname(String hostname, int port) {
        this.hostname = hostname;
        this.port     = port;
    }


    /**
     * _more_
     *
     * @param f _more_
     *
     * @return _more_
     */
    public String fileUrl(String f) {
        return urlBase + f;
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     *
     * @return _more_
     */
    protected String getGraphLink(Request request, Group group) {
        if ( !isAppletEnabled(request)) {
            return "";
        }
        return HtmlUtil
            .href(HtmlUtil
                .url(URL_GRAPH_VIEW, ARG_ID, group.getFullName(),
                     ARG_NODETYPE, NODETYPE_GROUP), HtmlUtil
                         .img(fileUrl("/tree.gif"), "Show group in graph"));
    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryListen(Request request) throws Exception {
        EntryListener entryListener = new EntryListener(this, request);
        synchronized (entryListeners) {
            entryListeners.add(entryListener);
        }
        synchronized (entryListener) {
            entryListener.wait();
        }
        Entry entry = entryListener.getEntry();
        if (entry == null) {
            return new Result("", new StringBuffer("No match"),
                              getMimeTypeFromSuffix(".html"));
        }
        return getOutputHandler(request).outputEntry(request, entry);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntrySearch(Request request) throws Exception {

        if (request.get(ARG_WAIT, false)) {
            return processEntryListen(request);
        }

        //        System.err.println("submit:" + request.getString("submit","YYY"));
        if (request.defined("submit_type.x")) {
            //            System.err.println("request:" + request.getString("submit_type.x","XXX"));
            request.remove(ARG_OUTPUT);
            return processEntrySearchForm(request);
        }
        if (request.defined("submit_subset")) {
            //            System.err.println("request:" + request.getString("submit_type.x","XXX"));
            request.remove(ARG_OUTPUT);
            return processEntrySearchForm(request);
        }

        String what = request.getWhat(WHAT_ENTRIES);
        if ( !what.equals(WHAT_ENTRIES)) {
            Result result = processListShow(request);
            if (result == null) {
                throw new IllegalArgumentException("Unknown list request: "
                        + what);
            }
            result.putProperty(PROP_NAVSUBLINKS,
                               getSearchFormLinks(request, what));
            return result;
        }

        List[] pair = getEntries(request);
        return getOutputHandler(request).outputGroup(request, dummyGroup,
                                (List<Group>) pair[0], (List<Entry>) pair[1]);
    }






    /**
     * _more_
     *
     * @param entry _more_
     * @param statement _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    protected void setStatement(Entry entry, PreparedStatement statement,
                                boolean isNew)
            throws Exception {
        int col = 1;
        //id,type,name,desc,group,user,file,createdata,fromdate,todate
        statement.setString(col++, entry.getId());
        statement.setString(col++, entry.getType());
        statement.setString(col++, entry.getName());
        statement.setString(col++, entry.getDescription());
        statement.setString(col++, entry.getParentGroupId());
        statement.setString(col++, entry.getUser().getId());
        if (entry.getResource() == null) {
            entry.setResource(new Resource());
        }
        statement.setString(col++, entry.getResource().getPath());
        statement.setString(col++, entry.getResource().getType());
        statement.setTimestamp(col++, new java.sql.Timestamp(currentTime()),
                               calendar);
        //        System.err.println (entry.getName() + " " + new Date(entry.getStartDate()));
        statement.setTimestamp(col++,
                               new java.sql.Timestamp(entry.getStartDate()),
                               calendar);
        statement.setTimestamp(col++,
                               new java.sql.Timestamp(entry.getEndDate()),
                               calendar);
        statement.setDouble(col++, entry.getSouth());
        statement.setDouble(col++, entry.getNorth());
        statement.setDouble(col++, entry.getEast());
        statement.setDouble(col++, entry.getWest());
        if ( !isNew) {
            statement.setString(col++, entry.getId());
        }
    }


    /**
     * _more_
     *
     * @param group _more_
     * @param type _more_
     * @param name _more_
     * @param content _more_
     *
     * @throws Exception _more_
     */
    public void insertMetadata(Entry group, String type, String name,
                               String content)
            throws Exception {
        insertMetadata(new Metadata(group.getId(), type, name, content));
    }

    /**
     * _more_
     *
     * @param metadata _more_
     *
     * @throws Exception _more_
     */
    public void insertMetadata(Metadata metadata) throws Exception {
        PreparedStatement metadataInsert =
            getConnection().prepareStatement(INSERT_METADATA);
        int col = 1;
        metadataInsert.setString(col++, metadata.getId());
        metadataInsert.setString(col++, metadata.getType());
        metadataInsert.setString(col++, metadata.getName());
        metadataInsert.setString(col++, metadata.getContent());
        metadataInsert.execute();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntries(Request request, List<Entry> entries)
            throws Exception {
        clearCache();
        String query;

        PreparedStatement tagsStmt =
            getConnection().prepareStatement("delete from " + TABLE_TAGS
                                             + " where " + COL_TAGS_ENTRY_ID
                                             + "=?");
        query = SqlUtil.makeDelete(
            TABLE_ASSOCIATIONS,
            SqlUtil.makeOr(
                Misc.newList(
                    SqlUtil.eq(COL_ASSOCIATIONS_FROM_ENTRY_ID, "?"),
                    SqlUtil.eq(COL_ASSOCIATIONS_TO_ENTRY_ID, "?"))));
        PreparedStatement assStmt = getConnection().prepareStatement(query);

        query = SqlUtil.makeDelete(TABLE_COMMENTS,
                                   SqlUtil.eq(COL_COMMENTS_ENTRY_ID, "?"));
        PreparedStatement commentsStmt =
            getConnection().prepareStatement(query);



        PreparedStatement entriesStmt = getConnection().prepareStatement(
                                            SqlUtil.makeDelete(
                                                TABLE_ENTRIES,
                                                COL_ENTRIES_ID, "?"));
        //        PreparedStatement genericStmt = getConnection().prepareStatement("delete from ? where id=?");
        synchronized (MUTEX_INSERT) {
            getConnection().setAutoCommit(false);
            Statement statement = getConnection().createStatement();
            for (Entry entry : entries) {
                getStorageManager().removeFile(entry);
                tagsStmt.setString(1, entry.getId());
                tagsStmt.addBatch();
                commentsStmt.setString(1, entry.getId());
                assStmt.setString(1, entry.getId());
                assStmt.setString(2, entry.getId());
                tagsStmt.addBatch();
                entriesStmt.setString(1, entry.getId());
                entriesStmt.addBatch();
                //                entry.getTypeHandler().deleteEntry(request,genericStmt,entry);
                entry.getTypeHandler().deleteEntry(request, statement, entry);
            }
            tagsStmt.executeBatch();
            commentsStmt.executeBatch();
            assStmt.executeBatch();
            entriesStmt.executeBatch();
            //            genericStmt.executeBatch();
            getConnection().commit();
            getConnection().setAutoCommit(true);
        }
    }



    /**
     * _more_
     *
     * @param entry _more_
     *
     * @throws Exception _more_b
     */
    public void addNewEntry(Entry entry) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(entry);
        insertEntries(entries, true);
    }


    public Result processFile(Request request) throws Exception {
        List<Harvester> harvesters = getAdmin().getHarvesters();
        TypeHandler typeHandler = getTypeHandler(request);
        String filepath = request.getUnsafeString(ARG_FILE,"");
        //Check to  make sure we can access this file
        if(!getStorageManager().isInDownloadArea(filepath)) {
            return new Result("", new StringBuffer("Cannot load file:" + filepath),"text/plain");
        }
        for (Harvester harvester : harvesters) {
            Entry entry = harvester.processFile(typeHandler, filepath);
            if(entry!=null) {
                addNewEntry(entry);
                return new Result("", new StringBuffer("OK"),"text/plain");
            }
        }
        return new Result("", new StringBuffer("Could not create entry"),"text/plain");
    }



    /**
     * _more_
     *
     * @param entries _more_
     * @param isNew _more_
     *
     * @throws Exception _more_
     */
    public void insertEntries(List<Entry> entries, boolean isNew)
            throws Exception {
        insertEntries(entries, isNew, false);
    }

    /**
     * _more_
     *
     * @param entries _more_
     * @param isNew _more_
     * @param canBeBatched _more_
     *
     * @throws Exception _more_
     */
    public void insertEntries(List<Entry> entries, boolean isNew,
                              boolean canBeBatched)
            throws Exception {

        if (entries.size() == 0) {
            return;
        }
        clearCache();
        //        System.err.println("Inserting:" + entries.size() + " entries");
        long t1     = System.currentTimeMillis();
        int  cnt    = 0;
        int  tagCnt = 0;
        PreparedStatement entryStatement =
            getConnection().prepareStatement(isNew
                                             ? INSERT_ENTRIES
                                             : UPDATE_ENTRIES);

        Hashtable typeStatements = new Hashtable();

        PreparedStatement tagsInsert =
            getConnection().prepareStatement(INSERT_TAGS);

        int batchCnt = 0;
        synchronized (MUTEX_INSERT) {
            getConnection().setAutoCommit(false);
            for (Entry entry : entries) {
                TypeHandler       typeHandler   = entry.getTypeHandler();
                String            sql = typeHandler.getInsertSql(isNew);
                PreparedStatement typeStatement = null;

                if (sql != null) {
                    typeStatement =
                        (PreparedStatement) typeStatements.get(sql);
                    if (typeStatement == null) {
                        typeStatement = getConnection().prepareStatement(sql);
                        typeStatements.put(sql, typeStatement);
                    }
                }

                setStatement(entry, entryStatement, isNew);
                batchCnt++;
                entryStatement.addBatch();

                if (typeStatement != null) {
                    batchCnt++;
                    typeHandler.setStatement(entry, typeStatement, isNew);
                    typeStatement.addBatch();
                }
                List<String> tags = entry.getTags();
                if (tags != null) {
                    for (String tag : tags) {
                        tagCnt++;
                        tagsInsert.setString(1, tag);
                        tagsInsert.setString(2, entry.getId());
                        batchCnt++;
                        tagsInsert.addBatch();
                    }
                }

                if (batchCnt > 1000) {
                    //                    if(isNew)
                    entryStatement.executeBatch();
                    //                    else                        entryStatement.executeUpdate();
                    if (tagCnt > 0) {
                        tagsInsert.executeBatch();
                    }
                    for (Enumeration keys = typeStatements.keys();
                            keys.hasMoreElements(); ) {
                        typeStatement =
                            (PreparedStatement) typeStatements.get(
                                keys.nextElement());
                        //                        if(isNew)
                        typeStatement.executeBatch();
                        //                        else                            typeStatement.executeUpdate();
                    }
                    batchCnt = 0;
                    tagCnt   = 0;
                }
                for (Metadata metadata : entry.getMetadata()) {
                    insertMetadata(metadata);
                }
            }
            if (batchCnt > 0) {
                entryStatement.executeBatch();
                tagsInsert.executeBatch();
                for (Enumeration keys = typeStatements.keys();
                        keys.hasMoreElements(); ) {
                    PreparedStatement typeStatement =
                        (PreparedStatement) typeStatements.get(
                            keys.nextElement());
                    typeStatement.executeBatch();
                }
            }
            getConnection().commit();
            getConnection().setAutoCommit(true);
        }


        long t2 = System.currentTimeMillis();
        totalTime    += (t2 - t1);
        totalEntries += entries.size();
        if (t2 > t1) {
            //System.err.println("added:" + entries.size() + " entries in " + (t2-t1) + " ms  Rate:" + (entries.size()/(t2-t1)));
            double seconds = totalTime / 1000.0;
            if (totalEntries%100==0 && seconds > 0) {
                System.err.println(totalEntries + " average rate:"
                                   + (int) (totalEntries / seconds)
                                   + "/second");
            }
        }

        tagsInsert.close();
        entryStatement.close();
        for (Enumeration keys =
                typeStatements.keys(); keys.hasMoreElements(); ) {
            PreparedStatement typeStatement =
                (PreparedStatement) typeStatements.get(keys.nextElement());
            typeStatement.close();
        }

        Misc.run(this, "checkNewEntries", entries);
    }

    /** _more_          */
    long totalTime = 0;

    /** _more_          */
    int totalEntries = 0;



    /**
     * _more_
     *
     * @param entries _more_
     */
    public void checkNewEntries(List<Entry> entries) {
        synchronized (entryListeners) {
            List<EntryListener> listeners =
                new ArrayList<EntryListener>(entryListeners);
            for (Entry entry : entries) {
                for (EntryListener entryListener : listeners) {
                    if (entryListener.checkEntry(entry)) {
                        synchronized (entryListener) {
                            entryListeners.remove(entryListener);
                        }
                    }
                }
            }
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
        Statement statement = getConnection().createStatement();
        if (max > 0) {
            statement.setMaxRows(max);
        }
        long t1 = System.currentTimeMillis();
        try {
            //            System.err.println("query:" + sql);
            statement.execute(sql);
        } catch (Exception exc) {
            log("Error executing sql:" + sql);
            throw exc;
        }
        long t2 = System.currentTimeMillis();
        if (debug || (t2 - t1 > 300)) {
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
     * @param sql _more_
     *
     * @throws Exception _more_
     */
    public void eval(String sql) throws Exception {
        Statement statement = execute(sql);
        String[]  results   = SqlUtil.readString(statement, 1);
        for (int i = 0; (i < results.length) && (i < 10); i++) {
            System.err.print(results[i] + " ");
            if (i == 9) {
                System.err.print("...");
            }
        }
    }



    /**
     * Set the UrlBase property.
     *
     * @param value The new value for UrlBase
     */
    public void setUrlBase(String value) {
        urlBase = value;
    }

    /**
     * Get the UrlBase property.
     *
     * @return The UrlBase
     */
    public String getUrlBase() {
        return urlBase;
    }





    /** _more_ */
    private Hashtable seenResources = new Hashtable();







    /**
     * _more_
     *
     * @param harvester _more_
     * @param typeHandler _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean processEntries(Harvester harvester,
                                  TypeHandler typeHandler,
                                  List<Entry> entries)
            throws Exception {
        System.err.print(harvester.getName() + "  process entries: ");
        insertEntries(getUniqueEntries(entries), true, true);
        return true;
    }


    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Entry> getUniqueEntries(List<Entry> entries)
            throws Exception {
        List<Entry> needToAdd = new ArrayList();
        String      query     = "";
        try {
            if (entries.size() == 0) {
                return needToAdd;
            }
            if (seenResources.size() > 500000) {
                seenResources = new Hashtable();
            }
            PreparedStatement select =
                getConnection().prepareStatement(query =
                    SqlUtil.makeSelect("count(" + COL_ENTRIES_ID + ")",
                                       Misc.newList(TABLE_ENTRIES),
                                       SqlUtil.eq(COL_ENTRIES_RESOURCE,
                                           "?")));
            long t1 = System.currentTimeMillis();
            for (Entry entry : entries) {
                String path = entry.getResource().getPath();
                if (seenResources.get(path) != null) {
                    continue;
                }
                seenResources.put(path, path);
                select.setString(1, path);
                //                select.addBatch();
                ResultSet results = select.executeQuery();
                if (results.next()) {
                    int found = results.getInt(1);
                    if (found == 0) {
                        needToAdd.add(entry);
                    }
                }
            }
            long t2 = System.currentTimeMillis();
            //            System.err.println("Took:" + (t2 - t1) + "ms to check: "
            //                               + entries.size() + " entries");
        } catch (Exception exc) {
            log("Processing:" + query, exc);
            throw exc;
        }
        return needToAdd;
    }



    /**
     * _more_
     *
     * @param dirs _more_
     */
    public void listen(List<FileInfo> dirs) {
        while (true) {
            for (FileInfo f : dirs) {
                if (f.hasChanged()) {
                    System.err.println("changed:" + f);
                }
            }
            Misc.sleep(1000);
        }
    }







    /**
     * _more_
     *
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(Entry entry) throws Exception {
        return getMetadata(entry.getId());
    }



    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Metadata> getMetadata(String id) throws Exception {
        String query = SqlUtil.makeSelect(
                           COLUMNS_METADATA, Misc.newList(
                               TABLE_METADATA), SqlUtil.makeAnd(
                               Misc.newList(
                                   SqlUtil.eq(
                                       COL_METADATA_ID, SqlUtil.quote(
                                           id)))), " order by "
                                               + COL_METADATA_TYPE);

        SqlUtil.Iterator iter = SqlUtil.getIterator(execute(query));
        ResultSet        results;
        List<Metadata>   metadata = new ArrayList();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                metadata.add(new Metadata(results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++),
                                          results.getString(col++)));
            }
        }
        return metadata;
    }




}

