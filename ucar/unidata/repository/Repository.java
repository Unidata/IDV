/**
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

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.PluginClassLoader;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;


import java.awt.*;
import java.awt.Image;

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

import java.util.jar.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;



import java.util.regex.*;
import java.util.zip.*;


import javax.swing.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Repository extends RepositoryBase implements 
        RequestHandler {


    public static final String MACRO_LINKS          =   "links";
    public static final String MACRO_HEADER_IMAGE   =   "header.image";
    public static final String MACRO_HEADER_TITLE   =   "header.title";
    public static final String MACRO_USERLINK       =   "userlink";
    public static final String MACRO_REPOSITORY_NAME=   "repository_name";
    public static final String MACRO_FOOTER=            "footer";
    public static final String MACRO_TITLE=             "title"; 
    public static final String MACRO_ROOT=              "root"; 
    public static final String MACRO_BOTTOM=            "bottom"; 
    public static final String MACRO_CONTENT=           "content";

    /** _more_ */
    public static int blockCnt = 0;


    /** _more_ */
    public static final String MSG_PREFIX = "<msg ";

    /** _more_ */
    public static final String MSG_SUFFIX = " msg>";

    /** _more_ */
    protected RequestUrl[] entryEditUrls = {
        URL_ENTRY_FORM, getMetadataManager().URL_METADATA_FORM,
        getMetadataManager().URL_METADATA_ADDFORM,
        URL_ACCESS_FORM  //,
        //        URL_ENTRY_DELETE
        //        URL_ENTRY_SHOW
    };

    protected RequestUrl[] groupEditUrls = {
        URL_ENTRY_NEW, URL_ENTRY_FORM, getMetadataManager().URL_METADATA_FORM,
        getMetadataManager().URL_METADATA_ADDFORM,
        URL_ACCESS_FORM  //,
        //        URL_ENTRY_DELETE
        //        URL_ENTRY_SHOW
    };



    /** _more_ */
    private static final int PAGE_CACHE_LIMIT = 100;



    /** _more_ */
    public static final OutputType OUTPUT_DELETER = new OutputType("Delete Entry", "repository.delete");


    /** _more_ */
    private List<EntryListener> entryListeners =
        new ArrayList<EntryListener>();

    /** _more_ */
    private Properties mimeTypes;

    /** _more_ */
    private Properties namesMap;


    /** _more_ */
    private Properties properties = new Properties();

    private Map<String, String> systemEnv;

    /** _more_ */
    private Properties dbProperties = new Properties();


    /** _more_ */
    private long baseTime = System.currentTimeMillis();


    /** _more_ */
    private List<String> loadFiles = new ArrayList<String>();

    /** _more_ */
    private String dumpFile;


    /** _more_ */
    private Hashtable languageMap = new Hashtable();

    /** _more_ */
    private List<TwoFacedObject> languages = new ArrayList<TwoFacedObject>();


    /** _more_ */
    private Hashtable theTypeHandlersMap = new Hashtable();

    /** _more_ */
    private List<TypeHandler> theTypeHandlers = new ArrayList<TypeHandler>();

    /** _more_ */
    private List<OutputHandler> outputHandlers =
        new ArrayList<OutputHandler>();

    private List<OutputHandler> allOutputHandlers =
        new ArrayList<OutputHandler>();



    /** _more_ */
    private Hashtable resources = new Hashtable();


    /** _more_ */
    private Hashtable namesHolder = new Hashtable();



    private  List<String> typeDefFiles   = new ArrayList<String>();
    private  List<String> apiDefFiles    = new ArrayList<String>();
    private  List<String> outputDefFiles       = new ArrayList<String>();
    private  List<String> metadataDefFiles     = new ArrayList<String>();


    /** _more_ */
    private List<User> cmdLineUsers = new ArrayList();


    /** _more_ */
    String[] args;


    /** _more_ */
    private boolean inTomcat = false;



    /** _more_ */
    private File logFile;

    /** _more_ */
    private OutputStream logFOS;

    /** _more_ */
    protected boolean debug = true;

    /** _more_ */
    private UserManager userManager;

    private EntryManager entryManager;

    private SearchManager searchManager;

    /** _more_ */
    private HarvesterManager harvesterManager;

    /** _more_ */
    private ActionManager actionManager;

    /** _more_ */
    private AccessManager accessManager;

    /** _more_ */
    private MetadataManager metadataManager;

    /** _more_ */
    private StorageManager storageManager;

    /** _more_ */
    private DatabaseManager databaseManager;

    /** _more_ */
    private Admin admin;

    /** _more_ */
    private GroupTypeHandler groupTypeHandler;


    /** _more_ */
    private Hashtable pageCache = new Hashtable();

    /** _more_ */
    private List pageCacheList = new ArrayList();



    /** _more_ */
    private List dataTypeList = null;

    /** _more_ */
    private List<String> htdocRoots = new ArrayList<String>();


    /** _more_ */
    private List<File> localFilePaths = new ArrayList<File>();



    /**
     * _more_
     *
     * @param args _more_
     * @param hostname _more_
     * @param port _more_
     * @param inTomcat _more_
     *
     * @throws Exception _more_
    */
    public Repository(String[] args, int port,
                      boolean inTomcat)
            throws Exception {
        super(port);
        java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
        this.hostname =localMachine.getHostName();
        this.inTomcat = inTomcat;
        this.args     = args;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSSLEnabled() {
        return inTomcat;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param ms _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, long ms) {
        return formatDate(new Date(ms));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param d _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Date d) {
        return formatDate(d);
    }







    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void close() throws Exception {
        getDatabaseManager().closeConnection();
    }




    /**
     * _more_
     *
     *
     * @param properties _more_
     * @throws Exception _more_
     */
    protected void init(Properties properties) throws Exception {
        initProperties(properties);
        initServer();
    }


    /**
     * _more_
     *
     *
     * @param contextProperties _more_
     * @throws Exception _more_
     */
    protected void initProperties(Properties contextProperties)
            throws Exception {

        /*
          order in which we load properties files
          system
          context (e.g., from tomcat web-inf)
          cmd line args (both -Dname=value and .properties files)
          (We load the above so we can define an alternate repository dir)
          local repository directory
          (Now load in the cmd line again because they have precedence over anything else);
          cmd line
         */

        properties = new Properties();
        properties.load(
            IOUtil.getInputStream(
                "/ucar/unidata/repository/resources/repository.properties",
                getClass()));

        Properties   argProperties       = new Properties();
        for (int i = 0; i < args.length; i++) {
            if(checkFile(args[i])) {
                continue;
            }
            if (args[i].endsWith(".properties")) {
                argProperties.load(IOUtil.getInputStream(args[i],
                        getClass()));
            } else if (args[i].equals("-dump")) {
                dumpFile = args[i + 1];
                i++;
            } else if (args[i].equals("-load")) {
                loadFiles.add(args[i + 1]);
                i++;
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
                usage("Unknown argument: " + args[i]);
            }
        }

        //Load the context and the command line properties now 
        //so the storage manager can get to them
        if (contextProperties != null) {
            properties.putAll(contextProperties);
        }
        properties.putAll(argProperties);



        //Call the storage manager so it can figure out the home dir
        getStorageManager();

        try {
            //Now load in the local properties file
            String localPropertyFile =
                IOUtil.joinDir(getStorageManager().getRepositoryDir(),
                               "repository.properties");
            properties.load(IOUtil.getInputStream(localPropertyFile,
                    getClass()));
        } catch (Exception exc) {}
        //Now put back any of the cmd line arg properties because they have precedence
        properties.putAll(argProperties);

        initPlugins();

        apiDefFiles.addAll(0,getResourcePaths(PROP_API));
        typeDefFiles.addAll(0, getResourcePaths(PROP_TYPES));
        outputDefFiles.addAll(0, getResourcePaths(PROP_OUTPUTHANDLERS));
        metadataDefFiles.addAll(0, getResourcePaths(PROP_METADATAHANDLERS));

        debug = getProperty(PROP_DEBUG, false);
        //        System.err.println ("debug:" + debug);

        setUrlBase((String) properties.get(PROP_HTML_URLBASE));
        if (getUrlBase() == null) {
            setUrlBase(BLANK);
        }

        logFile =
            new File(IOUtil.joinDir(getStorageManager().getRepositoryDir(),
                                    "repository.log"));
        //TODO: Roll the log file
        logFOS = new FileOutputStream(logFile, true);

        String derbyHome = (String) properties.get(PROP_DB_DERBY_HOME);
        if (derbyHome != null) {
            derbyHome = getStorageManager().localizePath(derbyHome);
            File dir = new File(derbyHome);
            IOUtil.makeDirRecursive(dir);
            System.setProperty("derby.system.home", derbyHome);
        }

        mimeTypes = new Properties();
        for (String path : getResourcePaths(PROP_HTML_MIMEPROPERTIES)) {
            try {
                InputStream is = IOUtil.getInputStream(path, getClass());
                mimeTypes.load(is);
            } catch (Exception exc) {
                //noop
            }
        }

        sdf = RepositoryUtil.makeDateFormat(getProperty(PROP_DATEFORMAT,
                                         DEFAULT_TIME_FORMAT));
        TimeZone.setDefault(DateUtil.TIMEZONE_GMT);


        //This will end up being from the properties
        htdocRoots.addAll(
            StringUtil.split(
                getProperty("ramadda.html.htdocroots", BLANK), ";", true,
                true));

        String hostname = getProperty(PROP_HOSTNAME,(String)null);
        if(hostname!=null) {
            this.hostname = hostname;
        }

    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initServer() throws Exception {
        getDatabaseManager().init();
        initTypeHandlers();
        initSchema();

        for (String sqlFile : (List<String>) loadFiles) {
            String     sql        = IOUtil.readContents(sqlFile, getClass());
            Connection connection = getDatabaseManager().getNewConnection();
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            SqlUtil.loadSql(sql, statement, false, true);
            connection.commit();
            connection.setAutoCommit(true);
            connection.close();
        }
        readGlobals();

        checkVersion();

        initOutputHandlers();
        getMetadataManager().initMetadataHandlers(metadataDefFiles);
        initApi();

        initUsers();
        getEntryManager().initGroups();
        getHarvesterManager().initHarvesters();
        initLanguages();

        setLocalFilePaths();

        if (dumpFile != null) {
            FileOutputStream fos = new FileOutputStream(dumpFile);
            getDatabaseManager().makeDatabaseCopy(fos, true);
            fos.close();
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
     * @return _more_
     */
    protected UserManager doMakeUserManager() {
        return new UserManager(this);
    }

    protected EntryManager doMakeEntryManager() {
        return new EntryManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected HarvesterManager doMakeHarvesterManager() {
        return new HarvesterManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected ActionManager doMakeActionManager() {
        return new ActionManager(this);
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

    protected EntryManager getEntryManager() {
        if (entryManager == null) {
            entryManager = doMakeEntryManager();
        }
        return entryManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected HarvesterManager getHarvesterManager() {
        if (harvesterManager == null) {
            harvesterManager = doMakeHarvesterManager();
        }
        return harvesterManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected ActionManager getActionManager() {
        if (actionManager == null) {
            actionManager = doMakeActionManager();
        }
        return actionManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected AccessManager doMakeAccessManager() {
        return new AccessManager(this);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected AccessManager getAccessManager() {
        if (accessManager == null) {
            accessManager = doMakeAccessManager();
        }
        return accessManager;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected SearchManager doMakeSearchManager() {
        return new SearchManager(this);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected SearchManager getSearchManager() {
        if (searchManager == null) {
            searchManager = doMakeSearchManager();
        }
        return searchManager;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected MetadataManager doMakeMetadataManager() {
        return new MetadataManager(this);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    protected MetadataManager getMetadataManager() {
        if (metadataManager == null) {
            metadataManager = doMakeMetadataManager();
        }
        return metadataManager;
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


    public static final double VERSION = 1.0;

    private void  updateToVersion1_0() throws Exception {

    }

    protected void checkVersion() throws Exception {
        double version = getDbProperty(PROP_VERSION,0.0);
        if(version==VERSION) return;
        updateToVersion1_0();
        //        writeGlobal(PROP_VERSION,""+VERSION);
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initLanguages() throws Exception {
        List sourcePaths =
            Misc.newList(
                getStorageManager().getSystemResourcePath() + "/languages",
                getStorageManager().getRepositoryDir());
        for (int i = 0; i < sourcePaths.size(); i++) {
            String       dir     = (String) sourcePaths.get(i);
            List<String> listing = IOUtil.getListing(dir, getClass());
            for (String path : listing) {
                if ( !path.endsWith(".pack")) {
                    continue;
                }
                String content = IOUtil.readContents(path, getClass(),
                                     (String) null);
                if (content == null) {
                    continue;
                }
                List<String> lines = StringUtil.split(content, "\n", true,
                                         true);
                Properties properties = new Properties();
                String     type       = null;
                String     name       = null;
                for (String line : lines) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    List toks = StringUtil.split(line, "=", true, true);
                    if (toks.size() != 2) {
                        continue;
                    }
                    String key   = (String) toks.get(0);
                    String value = (String) toks.get(1);
                    if (key.equals("language.type")) {
                        type = value;
                    } else if (key.equals("language.name")) {
                        name = value;
                    } else {
                        properties.put(key, value);
                    }
                }
                if (type != null) {
                    if (name == null) {
                        name = type;
                    }
                    languages.add(new TwoFacedObject(name, type));
                    languageMap.put(type, properties);
                } else {
                    System.err.println("No _type_ found in: " + path);
                }
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<TwoFacedObject> getLanguages() {
        return languages;
    }

    /**
     * _more_
     *
     * @param message _more_
     */
    protected void usage(String message) {
        throw new IllegalArgumentException(
            message
            + "\nusage: repository\n\t-admin <admin name> <admin password>\n\t-port <http port>\n\t-Dname=value (e.g., -Dramadda.db=derby to specify the derby database)");
    }


    protected void loadPluginFile(File f) {
        
    }

    private class MyClassLoader extends PluginClassLoader {
        public MyClassLoader(String path, ClassLoader parent) throws Exception {
            super(path, parent);
        }
        protected void checkClass(Class c) throws Exception {
        }
        protected String defineResource(JarEntry jarEntry) {
            String path = super.defineResource(jarEntry);
            checkFile(path);
            return path;
        }
    }


    protected void  initPlugins() throws Exception  {
        File dir = new File(getStorageManager().getPluginsDir());
        File[]plugins = dir.listFiles();
        for(int i=0;i<plugins.length;i++) {
            if(plugins[i].isDirectory()) continue;
            if(plugins[i].toString().endsWith(".jar")) {
                PluginClassLoader cl = new MyClassLoader(plugins[i].toString(),
                                                             getClass().getClassLoader());

                Misc.addClassLoader(cl);
                List entries = cl.getEntryNames();
                for (int entryIdx = 0; entryIdx < entries.size(); entryIdx++) {
                    String entry = (String) entries.get(entryIdx);
                    if(!checkFile(entry)) {
                        System.err.println ("Don't know how to handle plugin resource:" + entry +" from plugin:" + plugins[i]);
                    }
                }
            } else {
                checkFile(plugins[i].toString());
            }
        }
    }


    protected boolean checkFile(String file) {
        if (file.indexOf("api.xml") >= 0) {
            apiDefFiles.add(file);
        } else if (file.indexOf("types.xml") >= 0) {
            typeDefFiles.add(file);
        } else if (file.indexOf("outputhandlers.xml") >= 0) {
            outputDefFiles.add(file);
        } else if (file.indexOf("metadatahandlers.xml") >= 0) {
            metadataDefFiles.add(file);
        } else {
            return false;
        }
        return true;
    }


    /**
     * _more_
     *
     * @param propertyName _more_
     *
     * @return _more_
     */
    protected List<String> getResourcePaths(String propertyName) {
        List<String> tmp = StringUtil.split(getProperty(propertyName, BLANK),
                                            ";", true, true);
        List<String> paths = new ArrayList<String>();
        for (String path : tmp) {
            path = getStorageManager().localizePath(path);
            paths.add(path);
        }
        return paths;
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void readGlobals() throws Exception {
        Statement statement = getDatabaseManager().select(Tables.GLOBALS.COLUMNS,
                                  Tables.GLOBALS.NAME, new Clause[] {});
        dbProperties = new Properties();
        ResultSet results = statement.getResultSet();
        while (results.next()) {
            String name  = results.getString(1);
            String value = results.getString(2);
            properties.put(name, value);
            dbProperties.put(name, value);
        }
        statement.close();
    }



    /**
     * _more_
     */
    protected void clearCache() {
        //        System.err.println ("Clear full cache ");
        pageCache     = new Hashtable();
        pageCacheList = new ArrayList();
        getEntryManager().clearCache();
        dataTypeList  = null;
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
        log("user:" + request.getUser() + " -- " + message);
    }


    /**
     * _more_
     *
     * @param message _more_
     * @param exc _more_
     */
    protected void log(String message, Throwable exc) {
        System.err.println("Error:" + message);
        Throwable thr = null;
        if (exc != null) {
            //            exc.printStackTrace();
            thr = LogUtil.getInnerException(exc);
            if(thr!=null) {
                thr.printStackTrace();
            }
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
     * @param props _more_
     * @param handlers _more_
     * @param defaultHandler _more_
     *
     * @throws Exception _more_
     */
    protected void addRequest(Element node, Hashtable props,
                              Hashtable handlers, String defaultHandler)
            throws Exception {
        String request    = XmlUtil.getAttribute(node,
                                ApiMethod.ATTR_REQUEST);
        String methodName = XmlUtil.getAttribute(node, ApiMethod.ATTR_METHOD);
        boolean admin = XmlUtil.getAttribute(node, ApiMethod.ATTR_ADMIN,
                                             Misc.getProperty(props,
                                                 ApiMethod.ATTR_ADMIN, true));

        boolean canCache = XmlUtil.getAttribute(node,
                               ApiMethod.ATTR_CANCACHE,
                               Misc.getProperty(props,
                                   ApiMethod.ATTR_CANCACHE, true));



        String handlerName = XmlUtil.getAttribute(node,
                                 ApiMethod.ATTR_HANDLER,
                                 Misc.getProperty(props,
                                     ApiMethod.ATTR_HANDLER, defaultHandler));


        RequestHandler handler = (RequestHandler) handlers.get(handlerName);

        if (handler == null) {
            handler = this;
            if (handlerName.equals("usermanager")) {
                handler = getUserManager();
            } else if (handlerName.equals("admin")) {
                handler = getAdmin();
            } else if (handlerName.equals("harvestermanager")) {
                handler = getHarvesterManager();
            } else if (handlerName.equals("actionmanager")) {
                handler = getActionManager();
            } else if (handlerName.equals("graphmanager")) {
                handler = getOutputHandler(GraphOutputHandler.OUTPUT_GRAPH);
            } else if (handlerName.equals("accessmanager")) {
                handler = getAccessManager();
            } else if (handlerName.equals("searchmanager")) {
                handler = getSearchManager();
            } else if (handlerName.equals("entrymanager")) {
                handler = getEntryManager();
            } else if (handlerName.equals("metadatamanager")) {
                handler = getMetadataManager();
            } else if (handlerName.equals("repository")) {
                handler = this;
            } else {
                Constructor ctor =
                    Misc.findConstructor(Misc.findClass(handlerName),
                                         new Class[] { Repository.class,
                        Element.class });
                handler = (RequestHandler) ctor.newInstance(new Object[] {
                    this,
                    node });
            }
            handlers.put(handlerName, handler);
        }


        String    url       = request;
        ApiMethod oldMethod = requestMap.get(url);
        if (oldMethod != null) {
            requestMap.remove(url);
        }


        Class[] paramTypes = new Class[] { Request.class };
        Method method = Misc.findMethod(handler.getClass(), methodName,
                                        paramTypes);
        if (method == null) {
            System.err.println("props:" + props);
            throw new IllegalArgumentException("Unknown request method:"
                    + methodName);
        }


        ApiMethod apiMethod =
            new ApiMethod(this, handler, request,
                          XmlUtil.getAttribute(node, ApiMethod.ATTR_NAME,
                              request), method, admin, canCache,
                                        XmlUtil.getAttribute(node,
                                            ApiMethod.ATTR_TOPLEVEL, false));
        List actions = StringUtil.split(XmlUtil.getAttribute(node,
                           ApiMethod.ATTR_ACTIONS, BLANK), ",", true, true);
        if ( !Permission.isValidActions(actions)) {
            throw new IllegalArgumentException("Bad actions:" + actions
                    + " for api method:" + apiMethod.getName());
        }
        apiMethod.setActions(actions);
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
     * @return _more_
     */
    public List<ApiMethod> getApiMethods() {
        return apiMethods;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initApi() throws Exception {
        Hashtable handlers = new Hashtable();

        for (String file : apiDefFiles) {
            file = getStorageManager().localizePath(file);
            Element   apiRoot = XmlUtil.getRoot(file, getClass());
            Hashtable props   = new Hashtable();
            processApiNode(apiRoot, handlers, props, "repository");
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
     * @param apiRoot _more_
     * @param handlers _more_
     * @param props _more_
     * @param defaultHandler _more_
     *
     * @throws Exception _more_
     */
    private void processApiNode(Element apiRoot, Hashtable handlers,
                                Hashtable props, String defaultHandler)
            throws Exception {
        if (apiRoot == null) {
            return;
        }
        NodeList children = XmlUtil.getElements(apiRoot);
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            String  tag  = node.getTagName();
            if (tag.equals(ApiMethod.TAG_PROPERTY)) {
                props.put(XmlUtil.getAttribute(node, ApiMethod.ATTR_NAME),
                          XmlUtil.getAttribute(node, ApiMethod.ATTR_VALUE));
            } else if (tag.equals(ApiMethod.TAG_API)) {
                addRequest(node, props, handlers, defaultHandler);
            } else if (tag.equals(ApiMethod.TAG_GROUP)) {
                processApiNode(node, handlers, props,
                               XmlUtil.getAttribute(node,
                                   ApiMethod.ATTR_HANDLER, defaultHandler));
            } else {
                throw new IllegalArgumentException("Unknown api.xml tag:"
                        + tag);
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
            file = getStorageManager().localizePath(file);
            Element root = XmlUtil.getRoot(file, getClass());
            if (root == null) {
                continue;
            }
            List children = XmlUtil.findChildren(root, TAG_OUTPUTHANDLER);
            for (int i = 0; i < children.size(); i++) {
                Element node = (Element) children.get(i);
                boolean required = XmlUtil.getAttribute(node, ARG_REQUIRED,
                                       true);
                try {
                    Class c = Misc.findClass(XmlUtil.getAttribute(node,
                                  ATTR_CLASS));
                    Constructor ctor = Misc.findConstructor(c,
                                           new Class[] { Repository.class,
                            Element.class });
                    addOutputHandler(
                        (OutputHandler) ctor.newInstance(new Object[] { this,
                            node }));
                } catch (Exception exc) {
                    if ( !required) {
                        System.err.println(
                            "Couldn't load optional output handler:"
                            + XmlUtil.toString(node));
                    } else {
                        System.err.println(
                            "Error loading output handler file:" + file);
                        throw exc;
                    }
                }
            }

        }

        getUserManager().initOutputHandlers();
        OutputHandler outputHandler = new OutputHandler(getRepository(),"Entry Deleter") {
            public boolean canHandleOutput(OutputType output) {
                return output.equals(OUTPUT_DELETER);
            }
            protected void addOutputTypes(Request request, State state,
                                          List<OutputType> types)
                    throws Exception {
                if ((state.group != null) || (state.entry != null)) {
                    return;
                }
                for (Entry entry : state.getAllEntries()) {
                    if ( !getAccessManager().canDoAction(request, entry,
                            Permission.ACTION_DELETE)) {
                        return;
                    }
                }
                types.add(OUTPUT_DELETER);
            }


            public Result outputGroup(Request request, Group group,
                                      List<Group> subGroups,
                                      List<Entry> entries)
                    throws Exception {

                StringBuffer idBuffer = new StringBuffer();
                entries.addAll(subGroups);
                for (Entry entry : entries) {
                    idBuffer.append(",");
                    idBuffer.append(entry.getId());
                }
                return new Result(request.url(URL_ENTRY_DELETELIST, ARG_ENTRYIDS,
                        idBuffer.toString()));
            }
        };
        outputHandler.addType(OUTPUT_DELETER);
        addOutputHandler(outputHandler);
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
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result handleRequest(Request request) throws Exception {

        long   t1 = System.currentTimeMillis();
        Result result;

        if (debug) {
            debug("user:" + request.getUser() + " -- " + request.toString());
        }
        //        log("request:" + request);
        try {
            getUserManager().checkSession(request);
            result = getResult(request);
        } catch (Throwable exc) {
            //In case the session checking didn't set the user
            if (request.getUser() == null) {
                request.setUser(getUserManager().getAnonymousUser());
            }

            //TODO: For non-html outputs come up with some error format
            Throwable    inner     = LogUtil.getInnerException(exc);
            boolean      badAccess = inner instanceof RepositoryUtil.AccessException;
            StringBuffer sb        = new StringBuffer();
            if ( !badAccess) {
                sb.append(error(msgLabel("An error has occurred")
                                + HtmlUtil.p() + inner.getMessage()));
            } else {
                sb.append(error(inner.getMessage()));
                String redirect =
                    XmlUtil.encodeBase64(request.getUrl().getBytes());
                sb.append(getUserManager().makeLoginForm(request,
                        HtmlUtil.hidden(ARG_REDIRECT, redirect)));
            }

            if ((request.getUser() != null) && request.getUser().getAdmin()) {
                sb.append("<pre>" + LogUtil.getStackTrace(inner) + "</pre>");
            }

            result = new Result(msg("Error"), sb);
            if (badAccess) {
                result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
                //                result.addHttpHeader("WWW-Authenticate","Basic realm=\"repository\"");
            } else {
                result.setResponseCode(Result.RESPONSE_INTERNALERROR);
                String userAgent = request.getHeaderArg("User-Agent");
                if(userAgent==null)
                    userAgent = "Unknown";
                log("Error handling request:" + request + " user-agent:" + userAgent +" ip:" + request.getIp(), exc);
            }
        }

        boolean okToAddCookie = false;


        if ((result != null) && (result.getInputStream() == null)
                && result.isHtml() && result.getShouldDecorate()) {
            result.putProperty(PROP_NAVLINKS, getNavLinks(request));
            okToAddCookie = result.getResponseCode() == Result.RESPONSE_OK;
            decorateResult(request, result);
        }

        if (result.getRedirectUrl() != null) {
            okToAddCookie = true;
        }

        long t2 = System.currentTimeMillis();
        if ((result != null) && (t2 != t1)
                && (true || request.get("debug", false))) {
            if ((t2 - t1) > 100) {
                //                System.err.println("Time:" + request.getRequestPath() + " "
                //                                   + (t2 - t1));
            }
        }
        if (okToAddCookie
                && (request.getSessionIdWasSet()
                    || (request.getSessionId() == null)) && (result
                       != null)) {
            if (request.getSessionId() == null) {
                request.setSessionId(getUserManager().getSessionId());
            }
            String sessionId = request.getSessionId();
            //            result.addCookie("repositorysession", sessionId+"; path=" + getUrlBase() + "; expires=Fri, 31-Dec-2010 23:59:59 GMT;");
            result.addCookie(UserManager.COOKIE_NAME,
                             sessionId + "; path=" + getUrlBase());
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
    protected ApiMethod findMethod(Request request) throws Exception {
        String incoming = request.getRequestPath().trim();
        if (incoming.endsWith("/")) {
            incoming = incoming.substring(0, incoming.length() - 1);
        }
        String urlBase = getUrlBase();
        if ( !incoming.startsWith(urlBase)) {
            return null;
        }
        incoming = incoming.substring(urlBase.length());
        if (incoming.length() == 0) {
            return homeApi;
        }


        List<Group> topGroups = new ArrayList<Group>(getEntryManager().getTopGroups(request));
        topGroups.add(getEntryManager().getTopGroup());
        //        System.err.println ("incoming:" + incoming);
        for (Group group : topGroups) {
            String name = "/" + getEntryManager().getPathFromEntry(group);
            //            System.err.println ("\t" + name);
            if (incoming.startsWith(name + "/")) {
                //                request.setCollectionEntry(group);
                incoming = incoming.substring(name.length());
                break;
            }
        }



        ApiMethod apiMethod = (ApiMethod) requestMap.get(incoming);
        if (apiMethod == null) {
            for (ApiMethod tmp : apiMethods) {
                String path = tmp.getRequest();
                if (path.endsWith("/*")) {
                    path = path.substring(0, path.length() - 2);
                    if (incoming.startsWith(path)) {
                        apiMethod = tmp;
                        break;
                    }
                }
            }
        }
        if ((apiMethod == null) && incoming.equals(urlBase)) {
            apiMethod = homeApi;
        }

        return apiMethod;
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
        ApiMethod apiMethod = findMethod(request);
        if (apiMethod == null) {
            return getHtdocsFile(request);
        }

        String userAgent = request.getHeaderArg("User-Agent");
        if(userAgent==null)
            userAgent = "Unknown";
        //        System.err.println(request + " user-agent:" + userAgent +" ip:" + request.getIp());

        if ( !getDbProperty(ARG_ADMIN_INSTALLCOMPLETE, false)) {
            return getAdmin().doInitialization(request);
        }



        if ( !getUserManager().isRequestOk(request)
                || !apiMethod.isRequestOk(request, this)) {
            throw new RepositoryUtil.AccessException(
                msg("You do not have permission to access this page"));
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

        //TODO: how to handle when the DB is shutdown
        if ( !getDatabaseManager().hasConnection()) {
            //                && !incoming.startsWith(getUrlBase() + "/admin")) {
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
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Result getHtdocsFile(Request request) throws Exception {
        String path = request.getRequestPath();
        if ( !path.startsWith(getUrlBase())) {
            log(request, "Unknown request" + " \"" + path + "\"");
            Result result =
                new Result(msg("Error"),
                           new StringBuffer(msgLabel("Unknown request")
                                            + "\"" + path + "\""));
            result.setResponseCode(Result.RESPONSE_NOTFOUND);
            return result;
        }


        String type   = getMimeTypeFromSuffix(IOUtil.getFileExtension(path));
        int    length = getUrlBase().length();
        //        path = StringUtil.replace(path, getUrlBase(), BLANK);
        path = path.substring(length);


        //Go through all of the htdoc roots
        for (String root : htdocRoots) {
            root = getStorageManager().localizePath(root);
            String fullPath = root + path;
            //Make sure no one is trying to access other files
            RepositoryUtil.checkFilePath(fullPath);
            try {
                InputStream is = IOUtil.getInputStream(fullPath, getClass());
                if (path.endsWith(".js") || path.endsWith(".css")) {
                    String js = IOUtil.readContents(is);
                    js = js.replace("${urlroot}", getUrlBase());
                    is = new ByteArrayInputStream(js.getBytes());
                }
                Result result = new Result(BLANK, is, type);
                result.setCacheOk(true);
                return result;
            } catch (IOException fnfe) {
                //noop
            }
        }
        String userAgent = request.getHeaderArg("User-Agent");
        if(userAgent==null)
            userAgent = "Unknown";

        log(request, "Unknown request:" + request.getUrl() + " user-agent:" + userAgent +" ip:" + request.getIp());
        Result result =
            new Result(msg("Error"),
                       new StringBuffer(msgLabel("Unknown request") + path));
        result.setResponseCode(Result.RESPONSE_NOTFOUND);
        return result;
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
        String template = null;
        Metadata metadata =null;
            /*
//            getMetadataManager().findMetadata((request.getCollectionEntry()
//                != null)
//                ? request.getCollectionEntry()
//                : topGroup, AdminMetadataHandler.TYPE_TEMPLATE, true);
            */
        if (metadata != null) {
            template = metadata.getAttr1();
            if (template.startsWith("file:")) {
                template = getStorageManager().localizePath(template.trim());
                template =
                    IOUtil.readContents(template.substring("file:".length()),
                                        getClass());
            }
            if (template.indexOf("${content}") < 0) {
                template = null;
            }
        }


        if (template == null) {
            template = getResource(PROP_HTML_TEMPLATE);
        }


        String jsContent =
            HtmlUtil.div("", " id=\"tooltipdiv\" class=\"tooltip-outer\" ")
            + HtmlUtil.div("", " id=\"output\"")
            + HtmlUtil.div("", " id=\"selectdiv\" class=\"selectdiv\" ")
            + HtmlUtil.div("", " id=\"floatdiv\" class=\"floatdiv\" ");

        List   links     = (List) result.getProperty(PROP_NAVLINKS);
        String linksHtml = HtmlUtil.space(1);
        if (links != null) {
            linksHtml = StringUtil.join(getProperty("ramadda.html.link.separator",""),
                                        links);
        }
        List   sublinks     = (List) result.getProperty(PROP_NAVSUBLINKS);
        String sublinksHtml = "";
        if (sublinks != null) {
            String sublinksTemplate = getProperty("ramadda.html.sublink.wrapper","");
            sublinksHtml = StringUtil.join(getProperty("ramadda.html.sublink.separator",""), sublinks);
            sublinksHtml = sublinksTemplate.replace("${sublinks}", sublinksHtml);
        }


        String content = new String(result.getContent());
        String html = template;
        String[]macros = new String[]{
            MACRO_HEADER_IMAGE,
            fileUrl(ICON_HEADER),
            MACRO_HEADER_TITLE,
            getProperty(PROP_REPOSITORY_NAME, "Repository"),
            MACRO_USERLINK,
            getUserManager().getUserLinks(request),
            MACRO_REPOSITORY_NAME,
            getProperty(PROP_REPOSITORY_NAME, "Repository"),
            MACRO_FOOTER,
            getProperty(PROP_HTML_FOOTER, BLANK),
            MACRO_TITLE, 
            result.getTitle(),
            MACRO_BOTTOM, 
            result.getBottomHtml(),
            MACRO_LINKS, 
            linksHtml,
            MACRO_CONTENT,
            content + jsContent,
            MACRO_ROOT, 
            getUrlBase()
        };

        for(int i=0;i<macros.length;i+=2) {
            html = html.replace("${" + macros[i] +"}", macros[i+1]);
        }

        if (sublinksHtml.length() > 0) {
            html = StringUtil.replace(html, "${sublinks}", sublinksHtml);
        } else {
            html = StringUtil.replace(html, "${sublinks}", BLANK);
        }
        html = translate(request, html);

        result.setContent(html.getBytes());
    }


    /** _more_ */
    private Hashtable seenMsg = new Hashtable();

    /** _more_ */
    private boolean trackMsg = false;

    /**
     * _more_
     *
     * @param request _more_
     * @param s _more_
     *
     * @return _more_
     */
    public String translate(Request request, String s) {
        User       user     = request.getUser();
        String     language = user.getLanguage();
        Properties map      = (Properties) languageMap.get(language);
        if (map == null) {
            map = (Properties) languageMap.get(getProperty(PROP_LANGUAGE,
                    BLANK));
        }
        StringBuffer stripped     = new StringBuffer();
        int          prefixLength = MSG_PREFIX.length();
        int          suffixLength = MSG_PREFIX.length();
        //        System.out.println(s);
        while (s.length() > 0) {
            String tmp  = s;
            int    idx1 = s.indexOf(MSG_PREFIX);
            if (idx1 < 0) {
                stripped.append(s);
                break;
            }
            String text = s.substring(0, idx1);
            if (text.length() > 0) {
                stripped.append(text);
            }
            s = s.substring(idx1 + 1);

            int idx2 = s.indexOf(MSG_SUFFIX);
            if (idx2 < 0) {
                //Should never happen
                throw new IllegalArgumentException(
                    "No closing message suffix:" + s);
            }
            String key   = s.substring(prefixLength - 1, idx2);

            String value = null;
            if (map != null) {
                value = (String) map.get(key);
            }
            if (value == null) {
                value = key;
                if (trackMsg) {
                    if (seenMsg.get(key) == null) {
                        System.out.println(key + "=" + value);
                        seenMsg.put(key, key);
                    }
                }
            }
            stripped.append(value);
            s = s.substring(idx2 + suffixLength);
        }
        return stripped.toString();
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
        if(systemEnv==null) {
            systemEnv  = System.getenv();
        }
        String prop = null;
        
        String test = (String)properties.get("ramadda.testproperties");
        if(test!=null && test.equals("true")) {
            try {
                properties.load(
                                IOUtil.getInputStream(
                                                      "/ucar/unidata/repository/resources/repository.properties",
                                                      getClass()));
            } catch(Exception exc) {
                
            }
        }

        //Look at the repository.properties first
        if(prop == null)
            prop =(String) properties.get(name);

        //Then look at system properties
        if(prop == null)
            prop = System.getProperty(name);

        //Then env vars
        if(prop == null)
            prop = systemEnv.get(name);

        return prop;
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
        String prop = getProperty(name);
        if (prop != null) {
            return prop;
        }
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
        String prop = getProperty(name);
        if (prop != null) {
            return new Boolean(prop).booleanValue();
        }
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
    public int getProperty(String name, int dflt) {
        String prop = getProperty(name);
        if (prop != null) {
            return new Integer(prop).intValue();
        }
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
    public boolean getDbProperty(String name, boolean dflt) {
        return Misc.getProperty(dbProperties, name, dflt);
    }


    public double getDbProperty(String name, double dflt) {
        return Misc.getProperty(dbProperties, name, dflt);
    }






    /**
     * _more_
     *
     * @throws Exception _more_
     *
     */
    protected void initSchema() throws Exception {
        //Force a connection
        getDatabaseManager().init();
        String sql = IOUtil.readContents(getProperty(PROP_DB_SCRIPT),
                                         getClass());
        sql = getDatabaseManager().convertSql(sql);

        Statement statement = getDatabaseManager().createStatement();
        SqlUtil.loadSql(sql, statement, true);

        for (String file : typeDefFiles) {
            file = getStorageManager().localizePath(file);
            Element entriesRoot = XmlUtil.getRoot(file, getClass());
            if (entriesRoot == null) {
                continue;
            }
            List children = XmlUtil.findChildren(entriesRoot,
                                TypeHandler.TAG_TYPE);
            for (int i = 0; i < children.size(); i++) {
                Element entryNode = (Element) children.get(i);
                Class handlerClass =
                    Misc.findClass(XmlUtil.getAttribute(entryNode,
                        TypeHandler.TAG_HANDLER,
                        "ucar.unidata.repository.GenericTypeHandler"));
                Constructor ctor = Misc.findConstructor(handlerClass,
                                       new Class[] { Repository.class,
                        Element.class });
                TypeHandler typeHandler =
                    (TypeHandler) ctor.newInstance(new Object[] { this,
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
    protected void writeGlobal(String name, boolean value) throws Exception {
        writeGlobal(name, BLANK + value);
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
        getDatabaseManager().delete(Tables.GLOBALS.NAME,
                                    Clause.eq(Tables.GLOBALS.COL_NAME, name));
        getDatabaseManager().executeInsert(Tables.GLOBALS.INSERT,
                                           new Object[] { name,
                value });
        dbProperties.put(name, value);
        properties.put(name, value);
    }



    /**
     *  _more_
     *
     *  @param request _more_
     *  @param entries _more_
     * @param state _more_
     *
     *  @return _more_
     *
     *  @throws Exception _more_
     */
    public List<OutputType> getOutputTypes(Request request,
                                           OutputHandler.State state)
            throws Exception {
        List<OutputType>  allTypes= new ArrayList<OutputType>();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.addOutputTypes(request, state, allTypes);
        }
        List<OutputType> okTypes= new ArrayList<OutputType>();
        for(OutputType outputType: allTypes) {
            if(isOutputTypeOK(outputType)) {
                okTypes.add(outputType);
            }
        }
        return okTypes;
    }

    public List<OutputType> getOutputTypes() 
            throws Exception {
        List<OutputType>  allTypes= new ArrayList<OutputType>();
        for (OutputHandler outputHandler : outputHandlers) {
            allTypes.addAll(outputHandler.getTypes());
        }
        return allTypes;
    }


    public boolean isOutputTypeOK(OutputType outputType) {
        String prop = getProperty(outputType.getId()+".ok");
        if(prop == null || prop.equals("true")) {
            return true;
        }
        return false;
    }

    public void setOutputTypeOK(OutputType outputType, boolean ok) throws Exception {
        String prop = outputType.getId()+".ok";
        writeGlobal(prop, ""+ok);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected List<OutputHandler> getOutputHandlers() {
        return outputHandlers;
    }


    public OutputHandler getOutputHandler(OutputType outputType) throws Exception {
        if(!isOutputTypeOK(outputType)) return null;
        return getOutputHandler(outputType.getId());
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
    public OutputHandler getOutputHandler(Request request) throws Exception {
        OutputHandler handler  =  getOutputHandler(request.getOutput());
        if(handler!=null) return handler;
        throw new IllegalArgumentException(
            msgLabel("Could not find output handler for")
            + request.getOutput());
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
    public OutputHandler getOutputHandler(String type) throws Exception {
        if(type == null || type.length()==0) {
            type = OutputHandler.OUTPUT_HTML.getId();
        }
        OutputType output = new OutputType("",type);
        for (OutputHandler outputHandler : outputHandlers) {
            if (outputHandler.canHandleOutput(output)) {
                return outputHandler;
            }
        }
        return null;
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
    }

    public TypeHandler getGroupTypeHandler() {
        return groupTypeHandler;
    }



    /**
     * _more_
     *
     * @param typeName _more_
     * @param typeHandler _more_
     */
    protected void addTypeHandler(String typeName, TypeHandler typeHandler) {
        theTypeHandlersMap.put(typeName, typeHandler);
        theTypeHandlers.add(typeHandler);
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<TypeHandler> getTypeHandlers() throws Exception {
        return new ArrayList<TypeHandler>(theTypeHandlers);
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
        String type = request.getString(ARG_TYPE,
                                        TypeHandler.TYPE_ANY).trim();
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
        TypeHandler typeHandler = (TypeHandler) theTypeHandlersMap.get(type);
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
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processMessage(Request request) throws Exception {
        return new Result(
            BLANK,
            new StringBuffer(
                note(request.getUnsafeString(ARG_MESSAGE, BLANK))));
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
    public Result processDummy(Request request) throws Exception {
        return new Result(BLANK, new StringBuffer(BLANK));
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
            extra1 = BLANK;
            extra2 = BLANK;
        }
        if (typeList.size() > 0) {
            for (TwoFacedObject tfo : typeList) {
                if (what.equals(tfo.getId())) {
                    links.add(HtmlUtil.span(tfo.toString(), extra1));
                } else {
                    links.add(HtmlUtil.href(request.url(URL_LIST_SHOW,
                            ARG_WHAT, (String) tfo.getId(), ARG_TYPE,
                            (String) typeHandler.getType()), tfo.toString(),
                                extra2));
                }
            }
        }
        String typeAttr = BLANK;
        if ( !typeHandler.getType().equals(TypeHandler.TYPE_ANY)) {
            typeAttr = "&type=" + typeHandler.getType();
        }


        String[] whats = { WHAT_TYPE, WHAT_TAG, WHAT_ASSOCIATION };
        String[] names = { "Types", "Tags", "Associations" };
        for (int i = 0; i < whats.length; i++) {
            if (what.equals(whats[i])) {
                links.add(HtmlUtil.span(names[i], extra1));
            } else {
                links.add(HtmlUtil.href(request.url(URL_LIST_SHOW, ARG_WHAT,
                        whats[i]) + typeAttr, names[i], extra2));
            }
        }

        return links;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param id _more_
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     *
     * @return _more_
     */
    public String makeShowHideBlock(Request request, String label,
                                    StringBuffer content, boolean visible) {
        return makeShowHideBlock(request, label, content, visible,
                                 "class=\"pagesubheading\"");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param label _more_
     * @param content _more_
     * @param visible _more_
     * @param headerExtra _more_
     *
     * @return _more_
     */
    public String makeShowHideBlock(Request request, String label,
                                    StringBuffer content, boolean visible,
                                    String headerExtra) {
        String       id      = "block_" + (blockCnt++);
        StringBuffer sb      = new StringBuffer();
        String       hideImg = fileUrl(ICON_MINUS);
        String       showImg = fileUrl(ICON_PLUS);
        String link =
            HtmlUtil.jsLink(HtmlUtil.onMouseClick("toggleBlockVisibility('"
                + id + "','" + id + "img','" + hideImg + "','" + showImg
                + "')"), HtmlUtil.img(visible
                                      ? hideImg
                                      : showImg, "",
                                          " id='" + id
                                          + "img' ") + HtmlUtil.space(1)
                                              + label, HtmlUtil.cssClass(
                                                  "pagesubheadinglink"));

        //        sb.append(RepositoryManager.tableSubHeader(link));
        sb.append("<div class=\"block\">");
        sb.append(HtmlUtil.div(link, headerExtra));
        sb.append("<div class=\"hideshowblock\" id=\"" + id
                  + "\" style=\"display:block;visibility:visible\">");
        if ( !visible) {
            sb.append("\n<SCRIPT LANGUAGE=\"JavaScript\">hide('" + id
                      + "');</script>\n");
        }

        sb.append(content.toString());
        sb.append("</div>");
        sb.append("</div>");
        return sb.toString();
    }


    /** _more_ */
    static int tabCnt = 0;

    /**
     * _more_
     *
     * @param titles _more_
     * @param contents _more_
     * @param skipEmpty _more_
     *
     * @return _more_
     */
    public String makeTabs(List titles, List contents, boolean skipEmpty) {
        return makeTabs(titles, contents, skipEmpty, "tabcontent");
    }

    public String makeTabs(List titles, List contents, boolean skipEmpty,
                           String tabContentClass) {
        return makeTabs(titles, contents, skipEmpty, tabContentClass, "tabcontents");
    }

    /**
     * _more_
     *
     * @param titles _more_
     * @param contents _more_
     * @param skipEmpty _more_
     * @param tabContentClass _more_
     *
     * @return _more_
     */
    public String makeTabs(List titles, List contents, boolean skipEmpty,
                           String tabContentClass,
                           String wrapperClass) {

        String id  = "tab_" + (tabCnt++);
        String ids = "tab_" + (tabCnt++) + "_ids";
        StringBuffer titleSB =
            new StringBuffer(
                "<table cellspacing=\"0\" cellpadding=\"0\"><tr>");

        titleSB = new StringBuffer("");
        StringBuffer contentSB = new StringBuffer();
        StringBuffer jsSB      = new StringBuffer("var " + ids + "=[");
        boolean      didone    = false;
        for (int i = 0; i < titles.size(); i++) {
            String title   = titles.get(i).toString();
            String content = contents.get(i).toString();
            if (skipEmpty && (content.length() == 0)) {
                continue;
            }
            String tabId = id + "_" + i;
            contentSB.append("\n");
            contentSB.append(
                HtmlUtil.div(
                    content,
                    HtmlUtil.cssClass(tabContentClass)
                    + HtmlUtil.id("content_" + tabId)
                    + HtmlUtil.style("display:block;visibility:" + ( !didone
                    ? "visible"
                    : "hidden"))));
            String link = HtmlUtil.href("javascript:" + id + ".toggleTab("
                                        + HtmlUtil.squote(tabId)
                                        + ")", title);
            //            titleSB.append("<td>\n");
            titleSB.append(HtmlUtil.span(link,
                                         HtmlUtil.cssClass("tabtitle")
                                         + HtmlUtil.id("title_" + tabId)));
            //            titleSB.append("\n</td>\n");
            if (didone) {
                jsSB.append(",");
            }
            jsSB.append(HtmlUtil.squote(tabId));
            didone = true;
        }
        jsSB.append("];\n");

        //        titleSB.append("</tr></table>");
        return HtmlUtil.script(jsSB.toString())
               + HtmlUtil.div(
                   titleSB.toString(),
                   HtmlUtil.cssClass("tabtitles")) + HtmlUtil.div(
                       contentSB.toString(),
                       HtmlUtil.cssClass(wrapperClass)) + HtmlUtil.script(
                           "var " + id + "=new Tab(" + ids + ");\n");
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
        return getSubNavLinks(request, urls, BLANK);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param title _more_
     * @param sb _more_
     * @param links _more_
     *
     * @return _more_
     */
    public Result makeResult(Request request, String title, StringBuffer sb,
                             RequestUrl[] links) {
        Result result = new Result(title, sb);
        if (links != null) {
            result.putProperty(PROP_NAVSUBLINKS,
                               getSubNavLinks(request, links));
        }
        return result;
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param urls _more_
     * @param arg _more_
     *
     * @return _more_
     */
    protected List getSubNavLinks(Request request, RequestUrl[] urls,
                                  String arg) {
        List   links    = new ArrayList();
        String type     = request.getRequestPath();
        String onLinkTemplate = getProperty("ramadda.html.sublink.on","");
        String offLinkTemplate = getProperty("ramadda.html.sublink.off","");
        for (int i = 0; i < urls.length; i++) {
            String label = urls[i].getLabel();
            label = msg(label);
            if (label == null) {
                label = urls[i].toString();
            }
            String url = request.url(urls[i]) + arg;
            String template;

            if (type.endsWith(urls[i].getPath())) {
                template = onLinkTemplate;
            } else {
                template = offLinkTemplate;
            }
            String html = template.replace("${label}", label);
            html = html.replace("${url}", url);
            html = html.replace("${root}", getRepository().getUrlBase());
            links.add(html);
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
        boolean isAdmin  = false;
        if (request != null) {
            User user = request.getUser();
            isAdmin = user.getAdmin();
        }

        String template = getProperty("ramadda.html.link.wrapper","");

        for (ApiMethod apiMethod : topLevelMethods) {
            if (apiMethod.getMustBeAdmin() && !isAdmin) {
                continue;
            }
            String url; 
            if (apiMethod == homeApi) {
                url = fileUrl(apiMethod.getRequest());
            } else {
                url = request.url(apiMethod.getUrl());
            }
            String html = template.replace("${url}",url);
            html = html.replace("${label}",apiMethod.getName());
            links.add(html);
        }
        return links;
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
    public Result processListHome(Request request) throws Exception {
        StringBuffer         sb           = new StringBuffer();
        List                 links = getListLinks(request, BLANK, false);
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
          sb.append(HtmlUtil.href(request.url(URL_LIST_SHOW,ARG_WHAT, tfo.getId(),ARG_TYPE,,typeHandler.getType()) , tfo.toString())));
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
        result.putProperty(PROP_NAVSUBLINKS,
                           getListLinks(request, BLANK, true));
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
    public Result processListShow(Request request) throws Exception {
        String what   = request.getWhat(WHAT_TYPE);
        Result result = null;
        if (what.equals(WHAT_TAG)) {
            //            result = listTags(request);
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
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msg(String msg) {
        if (msg.indexOf(MSG_PREFIX) >= 0) {
            throw new IllegalArgumentException("bad msg:" + msg);
        }
        return MSG_PREFIX + msg + MSG_SUFFIX;
    }

    /**
     * _more_
     *
     * @param msg _more_
     *
     * @return _more_
     */
    public static String msgLabel(String msg) {
        return msg(msg) + ":" + HtmlUtil.space(1);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected static String msgHeader(String h) {
        return HtmlUtil.div(msg(h), "class=\"pageheading\"");
    }






    public void createMonthNav(StringBuffer sb, Date date, String url,Hashtable dayLinks) {
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        cal.setTime(date);
        int[]  theDate = CalendarOutputHandler.getDayMonthYear(cal);
        int    theDay = cal.get(cal.DAY_OF_MONTH);
        int    theMonth = cal.get(cal.MONTH);
        int    theYear = cal.get(cal.YEAR);
        while (cal.get(cal.DAY_OF_MONTH)>1) {
            cal.add(cal.DAY_OF_MONTH, -1);
        }
        GregorianCalendar prev = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        prev.setTime(date);
        prev.add(cal.MONTH, -1);
        GregorianCalendar next = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        next.setTime(date);
        next.add(cal.MONTH, 1);

        sb.append(
                  "<table border=\"1\" cellspacing=\"0\" cellpadding=\"0\">");
        String[] dayNames = {
            "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"
        };
        String prevUrl = HtmlUtil.space(1)+HtmlUtil.href(url +"&" +CalendarOutputHandler.getUrlArgs(prev),"&lt;");
        String nextUrl = HtmlUtil.href(url +"&" +CalendarOutputHandler.getUrlArgs(next),"&gt;")+HtmlUtil.space(1);
        sb.append("<tr valign=top><td colspan=\"7\" align=\"center\"  class=\"calnavmonthheader\">");
        sb.append("<table cellspacing=0 cellpadding=0 width=100%><tr>");
        sb.append("<td width=1  class=\"calnavmonthheader\">");
        sb.append(prevUrl);
        sb.append("</td>");        
        sb.append("<td  class=\"calnavmonthheader\">");
        sb.append(DateUtil.MONTH_NAMES[cal.get(cal.MONTH)]);
        sb.append(HtmlUtil.space(1));
        sb.append(""+theYear);
        sb.append("</td>");
        sb.append("<td width=1  class=\"calnavmonthheader\">");
        sb.append(nextUrl);
        sb.append("</td>");        
        sb.append("</table>");

        sb.append("</tr>");
        sb.append("<tr>");
        for (int colIdx = 0; colIdx < 7; colIdx++) {
            sb.append("<td width=\"14%\" class=\"calnavdayheader\">"
                      + dayNames[colIdx] + "</td>");
        }
        sb.append("</tr>");
        int startDow = cal.get(cal.DAY_OF_WEEK);
        while (startDow > 1) {
            cal.add(cal.DAY_OF_MONTH, -1);
            startDow--;
        }
        for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
            sb.append("<tr valign=top>");
            for (int colIdx = 0; colIdx < 7; colIdx++) {
                int    thisDay   = cal.get(cal.DAY_OF_MONTH);
                int    thisMonth = cal.get(cal.MONTH);
                int    thisYear  = cal.get(cal.YEAR);
                String dayClass = "calnavday";
                if (thisMonth != theMonth) {
                    dayClass = "calnavoffday";
                } else if(theMonth == thisMonth && theYear == thisYear && theDay == thisDay) {
                    dayClass = "calnavtheday";                    
                }
                String content;
                if(dayLinks!=null) {
                    String key = thisYear+"/" + thisMonth +"/" + thisDay;
                    if(dayLinks.get(key)!=null) {
                        content = HtmlUtil.href(url+"&" + CalendarOutputHandler.getUrlArgs(cal),""+thisDay);
                        dayClass = "calnavtheday";                    
                    } else {
                        content = ""+thisDay;
                        dayClass = "calnavday";
                    }
                } else {
                    content = HtmlUtil.href(url+"&" + CalendarOutputHandler.getUrlArgs(cal),""+thisDay);
                }

                sb.append("<td " + HtmlUtil.cssClass(dayClass)+ " >" + content
                          + "</td>");
                sb.append("\n");
                cal.add(cal.DAY_OF_MONTH, 1);
            }
            if (cal.get(cal.MONTH) > theMonth)
                break;
            if (cal.get(cal.YEAR) > theYear)
                break;
        }
        sb.append("</table>");
    }


    public Result processModelProducts(Request request) throws Exception {
        Date date = request.get(ARG_DATE,new Date());
        //        System.err.println(date);
        StringBuffer sb = new StringBuffer();
        request.remove(ARG_DATE);
        Hashtable dayLinks = new Hashtable();
        dayLinks.put("2008/10/2","");
        dayLinks.put("2008/10/3","");
        createMonthNav(sb, date,request.getUrl(),dayLinks);
        return new Result("Model Products", sb);
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
            List<String> paths = getResourcePaths(id);
            for (String path : paths) {
                try {
                    resource = IOUtil.readContents(path, getClass());
                } catch (Exception exc) {
                    //noop
                }
                if (resource != null) {
                    break;
                }
            }
        } else {
            resource =
                IOUtil.readContents(getStorageManager().localizePath(id),
                                    getClass());
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
     * @param includeAny _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeTypeSelect(Request request, boolean includeAny)
            throws Exception {
        return makeTypeSelect(request, includeAny, "",false);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param includeAny _more_
     * @param selected _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeTypeSelect(Request request, boolean includeAny,
                                 String selected, boolean checkAddOk)
            throws Exception {
        List<TypeHandler> typeHandlers = getTypeHandlers();
        List              tmp          = new ArrayList();
        for (TypeHandler typeHandler : typeHandlers) {
            if (typeHandler.isAnyHandler() && !includeAny) {
                continue;
            }
            if(checkAddOk && !typeHandler.canBeCreatedBy(request)) {
                continue;
            }
            tmp.add(new TwoFacedObject(typeHandler.getLabel(),
                                       typeHandler.getType()));
        }
        return HtmlUtil.select(ARG_TYPE, tmp, selected);
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
        TypeHandler       typeHandler = getTypeHandler(request);
        List<TypeHandler> tmp         = new ArrayList<TypeHandler>();
        if ( !typeHandler.isAnyHandler()) {
            tmp.add(typeHandler);
            return tmp;
        }
        //For now don't do the db query to find the type handlers
        if (true) {
            return getTypeHandlers();
        }


        List<Clause> where = typeHandler.assembleWhereClause(request);
        Statement stmt =
            typeHandler.select(request, SqlUtil.distinct(Tables.ENTRIES.COL_TYPE),
                               where, "");
        String[] types = SqlUtil.readString(stmt, 1);
        for (int i = 0; i < types.length; i++) {
            TypeHandler theTypeHandler = getTypeHandler(types[i]);

            if (types[i].equals(TypeHandler.TYPE_ANY)) {
                tmp.add(0, theTypeHandler);

            } else {
                tmp.add(theTypeHandler);
            }
        }
        return tmp;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List getDefaultDataTypes() throws Exception {
        if (dataTypeList != null) {
            return dataTypeList;
        }
        Statement stmt = getDatabaseManager().select(
                             SqlUtil.distinct(Tables.ENTRIES.COL_DATATYPE),
                             Tables.ENTRIES.NAME, new Clause[] {});
        String[]  types = SqlUtil.readString(stmt, 1);
        List      tmp   = new ArrayList();
        Hashtable seen  = new Hashtable();
        for (TypeHandler typeHandler : getTypeHandlers()) {
            if (typeHandler.hasDefaultDataType()
                    && (seen.get(typeHandler.getDefaultDataType()) == null)) {
                tmp.add(typeHandler.getDefaultDataType());
                seen.put(typeHandler.getDefaultDataType(), "");
            }
        }

        for (int i = 0; i < types.length; i++) {
            if ((types[i] != null) && (types[i].length() > 0)
                    && (seen.get(types[i]) == null)) {
                tmp.add(types[i]);
            }
        }

        tmp.add(0, "");
        return dataTypeList = tmp;
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
        List<TypeHandler> tmp = getTypeHandlers(request);
        return getOutputHandler(request).listTypes(request, tmp);
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

    /**
     * protected Result listTags(Request request) throws Exception {
     *   TypeHandler typeHandler = getTypeHandler(request);
     *   List        where       = typeHandler.assembleWhereClause(request);
     *   if (where.size() == 0) {
     *       String type = (String) request.getString(ARG_TYPE,(BLANK).trim();
     *       if ((type.length() > 0) && !type.equals(TypeHandler.TYPE_ANY)) {
     *           typeHandler.addOr(Tables.ENTRIES.COL_TYPE, type, where, true);
     *       }
     *   }
     *   if (where.size() > 0) {
     *       where.add(SqlUtil.eq(Tables.TAGS.COL_ENTRY_ID, Tables.ENTRIES.COL_ID));
     *   }
     *
     *   Statement stmt;
     *   String[] tags =
     *       SqlUtil.readString(stmt =
     *           typeHandler.select(request,
     *                                     SqlUtil.distinct(Tables.TAGS.COL_NAME),
     *                                     where,
     *                                     " order by " + Tables.TAGS.COL_NAME), 1);
     *   stmt.close();
     *
     *   List<Tag>     tagList = new ArrayList();
     *   List<String>  names   = new ArrayList<String>();
     *   List<Integer> counts  = new ArrayList<Integer>();
     *   ResultSet     results;
     *   int           max = -1;
     *   int           min = -1;
     *   for (int i = 0; i < tags.length; i++) {
     *       String tag = tags[i];
     *       Statement stmt2 = typeHandler.select(request,
     *                             SqlUtil.count("*"),
     *                             Misc.newList(SqlUtil.eq(Tables.TAGS.COL_NAME,
     *                                 SqlUtil.quote(tag))));
     *
     *       ResultSet results2 = stmt2.getResultSet();
     *       if ( !results2.next()) {
     *           continue;
     *       }
     *       int count = results2.getInt(1);
     *       stmt2.close();
     *       if ((max < 0) || (count > max)) {
     *           max = count;
     *       }
     *       if ((min < 0) || (count < min)) {
     *           min = count;
     *       }
     *       tagList.add(new Tag(tag, count));
     *   }
     *
     *   return getOutputHandler(request).listTags(request, tagList);
     * }
     *
     */


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
     */
    public void setLocalFilePaths() {
        localFilePaths = (List<File>)Misc.toList(IOUtil.toFiles(
            (List<String>) StringUtil.split(getProperty(PROP_LOCALFILEPATHS,
                ""), "\n", true, true)));
    }

    public List<File> getLocalFilePaths() {
        return localFilePaths;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param addOrderBy _more_
     *
     * @return _more_
     */
    protected String getQueryOrderAndLimit(Request request,
                                           boolean addOrderBy) {
        String order = " DESC ";
        boolean haveOrder = false;
        if (request.get(ARG_ASCENDING, false)) {
            order = " ASC ";
            haveOrder = true;
        }
        int    skipCnt     = request.get(ARG_SKIP, 0);
        String limitString = BLANK;
        //        if (request.defined(ARG_SKIP)) {
        //            if (skipCnt > 0) {
        int max = request.get(ARG_MAX, MAX_ROWS);
        limitString = getDatabaseManager().getLimitString(skipCnt, max);
        String orderBy = BLANK;
        if (addOrderBy) {
            orderBy = " ORDER BY " + Tables.ENTRIES.COL_FROMDATE + order;
        }
        if (request.defined(ARG_ORDERBY)) {
            String by = request.getString(ARG_ORDERBY, BLANK);
            if (by.equals("fromdate")) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_FROMDATE + order;
            } else if (by.equals("todate")) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_TODATE + order;
            } else if (by.equals("createdate")) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_CREATEDATE + order;
            } else if (by.equals("name")) {
                if(!haveOrder) order = " ASC ";
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_NAME + order;
            }
        }

        return orderBy + limitString;
        //            }
        //        }

    }




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
        return BLANK;

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
            return new Result(BLANK, new StringBuffer("No match"),
                              getMimeTypeFromSuffix(".html"));
        }
        return getOutputHandler(request).outputEntry(request, entry);
    }


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
     * @param formName _more_
     * @param fieldName _more_
     *
     * @return _more_
     */
    public String getCalendarSelector(String formName, String fieldName) {
        String anchorName = "anchor." + fieldName;
        String divName    = "div." + fieldName;
        return "<A HREF=\"#\"   onClick=\"selectDate('" + divName
               + "',document.forms['" + formName + "']." + fieldName + ",'"
               + anchorName + "','yyyy-MM-dd'); return false;\"   NAME=\""
               + anchorName + "\" ID=\"" + anchorName + "\">"
               + HtmlUtil.img(fileUrl(ICON_CALENDAR), " Choose date", " border=0")
               + "</A>" + "<DIV ID=\"" + divName
               + "\" STYLE=\"position:absolute;visibility:hidden;background-color:white;layer-background-color:white;\"></DIV>";
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param name _more_
     * @param formName _more_
     * @param date _more_
     *
     * @return _more_
     */
    public String makeDateInput(Request request, String name,
                                String formName, Date date) {
        String dateHelp   = "e.g., yyyy-mm-dd,  now, -1 week, +3 days, etc.";
        String timeHelp   = "hh:mm:ss Z, e.g. 20:15:00 MST";

        String dateArg    = request.getString(name, "");
        String timeArg    = request.getString(name + ".time", "");
        String dateString = ((date == null)
                             ? dateArg
                             : dateSdf.format(date));
        String timeString = ((date == null)
                             ? timeArg
                             : timeSdf.format(date));

        return HtmlUtil.input(name, dateString,
                              HtmlUtil.SIZE_10 + " title=\"" + dateHelp
                              + "\"") + getCalendarSelector(formName, name)
                                      + " T:"
                                      + HtmlUtil.input(name + ".time",
                                          timeString,
                                          HtmlUtil.SIZE_15 + " title=\""
                                          + timeHelp + "\"");
    }


    /** _more_          */
    private static final String MAP_JS_MICROSOFT =
        "http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6.1";

    /** _more_          */
    private static final String MAP_ID_MICROSOFT = "microsoft";

    /** _more_          */
    private static final String MAP_JS_YAHOO =
        "http://api.maps.yahoo.com/ajaxymap?v=3.8&appid=idvunidata";

    /** _more_          */
    private static final String MAP_ID_YAHOO = "yahoo";


    /**
     * _more_
     *
     * @param request _more_
     * @param sb _more_
     * @param width _more_
     * @param height _more_
     * @param normalControls _more_
     *
     * @return _more_
     */
    public String initMap(Request request, StringBuffer sb, int width,
                          int height, boolean normalControls) {
        String userAgent = request.getHeaderArg("User-Agent");
        String host      = request.getHeaderArg("Host");
        if (host == null) {
            host = "localhost";
        }
        host = (String) StringUtil.split(host, ":", true, true).get(0);
        String googleMapsKey = null;

        if (userAgent == null) {
            userAgent = "Mozilla";
        }
        //        System.err.println ("agent:" + userAgent);
        String mapProvider = MAP_ID_MICROSOFT;
        String mapJS       = MAP_JS_MICROSOFT;
        //        googleMapsKey = "ABQIAAAA-JXA0-ozvUKU42oQp1aOZxT2yXp_ZAY8_ufC3CFXhHIE1NvwkxSig5NmAvzXxoX1Ly0QJZMRxtiLIg";        
        String googleKeys = getProperty(PROP_GOOGLEAPIKEYS, "");
        googleMapsKey = null;
        for (String line : (List<String>) StringUtil.split(googleKeys, "\n",
                true, true)) {
            if(line.length()==0) continue;
            String[] toks = StringUtil.split(line, ":", 2);
            if(toks == null) {
                continue;
            }
            if (toks.length != 2) {
                continue;
            }
            if (toks[0].equals(host)) {
                googleMapsKey = toks[1];
                break;
            }
        }


        if (userAgent.indexOf("MSIE") >= 0) {
            mapProvider = MAP_ID_YAHOO;
            mapJS       = MAP_JS_YAHOO;
        }

        if (googleMapsKey != null) {
            mapJS = "http://maps.google.com/maps?file=api&v=2&key="
                    + googleMapsKey;
            mapProvider = "google";
        }


        sb.append(HtmlUtil.importJS(mapJS));
        sb.append(HtmlUtil.importJS(fileUrl("/mapstraction.js")));
        sb.append(HtmlUtil.importJS(fileUrl("/mymap.js")));
        sb.append("<div style=\"width:" + width + "px; height:" + height
                  + "px\" id=\"mapstraction\"></div>\n");
        sb.append(HtmlUtil.script("MapInitialize(" + normalControls + ","
                                  + HtmlUtil.squote(mapProvider) + ");"));
        return "";
    }


    





}

