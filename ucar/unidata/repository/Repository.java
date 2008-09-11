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


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.sql.Clause;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedMapPanel;
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
public class Repository extends RepositoryBase implements Tables,
        RequestHandler {

    /** _more_ */
    public static final String GROUP_TOP = "Top";



    /** _more_ */
    protected RequestUrl[] entryEditUrls = {
        URL_ENTRY_FORM, getMetadataManager().URL_METADATA_FORM,
        getMetadataManager().URL_METADATA_ADDFORM,
        URL_ACCESS_FORM  //,
        //        URL_ENTRY_DELETE
        //        URL_ENTRY_SHOW
    };



    /** _more_ */
    private static final int PAGE_CACHE_LIMIT = 100;

    /** _more_ */
    private static final int ENTRY_CACHE_LIMIT = 5000;



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
    private Properties dbProperties = new Properties();


    /** _more_ */
    private long baseTime = System.currentTimeMillis();

    /** _more_ */
    private int keyCnt = 0;


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



    /** _more_ */
    private Hashtable resources = new Hashtable();


    /** _more_ */
    private Group topGroup;


    /** _more_ */
    private Object MUTEX_GROUP = new Object();




    /** _more_ */
    private Object MUTEX_KEY = new Object();



    /** _more_ */
    List<String> typeDefinitionFiles;

    /** _more_ */
    List<String> metadataDefFiles;



    /** _more_ */
    List<String> apiDefFiles;

    /** _more_ */
    List<String> outputDefFiles;


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
    private Hashtable<String, Group> groupCache = new Hashtable<String,
                                                      Group>();

    /** _more_ */
    private Hashtable entryCache = new Hashtable();

    /** _more_ */
    private List dataTypeList = null;

    /** _more_ */
    private List<String> htdocRoots = new ArrayList<String>();


    /** _more_ */
    private List<String> localFilePaths = new ArrayList<String>();

    /** _more_ */
    public static final String ID_PREFIX_LOCAL_FILE = "file:";

    /** _more_ */
    public static final String ID_PREFIX_CATALOG = "catalog:";

    /** _more_ */
    public static final String ID_PREFIX_GENERATED = "generated:";


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
    public Repository(String[] args, String hostname, int port,
                      boolean inTomcat)
            throws Exception {
        super(hostname, port);
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
     * @param entry _more_
     *
     * @return _more_
     */
    public String getIconUrl(Entry entry) {
        Resource resource = entry.getResource();
        String   path     = resource.getPath();
        if (entry.isGroup()) {
            return fileUrl(ICON_FOLDER_CLOSED);
        }
        String img = ICON_FILE;
        if (path != null) {
            String suffix = IOUtil.getFileExtension(path.toLowerCase());
            String prop   = getProperty("icon" + suffix);
            if (prop != null) {
                img = prop;
            }
        }
        return fileUrl(img);
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
        return "<div class=\"pageheading\">" + h + "</div>";
    }


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String note(String h) {
        return getMessage(h, ICON_INFORMATION, true);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String progress(String h) {
        return getMessage(h, ICON_PROGRESS, false);
    }


    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String warning(String h) {
        return getMessage(h, ICON_WARNING, true);
    }


    /**
     * _more_
     *
     * @param h _more_
     * @param buttons _more_
     *
     * @return _more_
     */
    protected String question(String h, String buttons) {
        return getMessage(h + "<p><hr>" + buttons, ICON_QUESTION, false);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    protected String error(String h) {
        return getMessage(h, ICON_ERROR, true);
    }


    /**
     * _more_
     *
     * @param h _more_
     * @param icon _more_
     * @param showClose _more_
     *
     * @return _more_
     */
    protected String getMessage(String h, String icon, boolean showClose) {
        String close =
            HtmlUtil.jsLink(HtmlUtil.onMouseClick("hide('messageblock')"),
                            HtmlUtil.img(fileUrl(ICON_CLOSE)));
        if ( !showClose) {
            close = "&nbsp;";
        }
        h = "<div class=\"innernote\"><table cellspacing=\"0\" cellpadding=\"0\" border=\"0\"><tr><td valign=\"top\">"
            + HtmlUtil.img(fileUrl(icon)) + HtmlUtil.space(2)
            + "</td><td valign=\"bottom\"><span class=\"notetext\">" + h
            + "</span></td></tr></table></div>";
        return "\n<table border=\"0\" id=\"messageblock\"><tr><td><div class=\"note\"><table><tr valign=top><td>"
               + h + "</td><td>" + close + "</td></tr></table>"
               + "</div></td></tr></table>\n";
    }

    /**
     * _more_
     *
     * @param b1 _more_
     * @param b2 _more_
     *
     * @return _more_
     */
    public String buttons(String b1, String b2) {
        return b1 + HtmlUtil.space(2) + b2;
    }

    /**
     * _more_
     *
     * @param b1 _more_
     * @param b2 _more_
     * @param b3 _more_
     *
     * @return _more_
     */
    public String buttons(String b1, String b2, String b3) {
        return b1 + HtmlUtil.space(2) + b2 + HtmlUtil.space(2) + b3;
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

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initServer() throws Exception {
        getConnection();
        initTypeHandlers();
        initSchema();

        for (String sqlFile : (List<String>) loadFiles) {
            System.err.println("loading");
            String     sql        = IOUtil.readContents(sqlFile, getClass());
            Connection connection = getConnection();
            connection.setAutoCommit(false);
            Statement statement = connection.createStatement();
            SqlUtil.loadSql(sql, statement, false, true);
            connection.commit();
            connection.setAutoCommit(true);
            System.err.println("done");
        }
        readGlobals();


        initOutputHandlers();
        getMetadataManager().initMetadataHandlers(metadataDefFiles);
        initApi();

        initUsers();
        initGroups();
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

        List<String> argEntryDefFiles    = new ArrayList();
        List<String> argApiDefFiles      = new ArrayList();
        List<String> argOutputDefFiles   = new ArrayList();
        List<String> argMetadataDefFiles = new ArrayList();


        for (int i = 0; i < args.length; i++) {
            if (args[i].endsWith(".properties")) {
                argProperties.load(IOUtil.getInputStream(args[i],
                        getClass()));
            } else if (args[i].indexOf("api.xml") >= 0) {
                argApiDefFiles.add(args[i]);
            } else if (args[i].indexOf("types.xml") >= 0) {
                argEntryDefFiles.add(args[i]);
            } else if (args[i].indexOf("outputhandlers.xml") >= 0) {
                argOutputDefFiles.add(args[i]);
            } else if (args[i].indexOf("metadatahandlers.xml") >= 0) {
                argMetadataDefFiles.add(args[i]);
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

        apiDefFiles = getResourcePaths(PROP_API);
        apiDefFiles.addAll(argApiDefFiles);

        typeDefinitionFiles = getResourcePaths(PROP_TYPES);
        typeDefinitionFiles.addAll(argEntryDefFiles);

        outputDefFiles = getResourcePaths(PROP_OUTPUTHANDLERS);
        outputDefFiles.addAll(argOutputDefFiles);

        metadataDefFiles = getResourcePaths(PROP_METADATAHANDLERS);
        metadataDefFiles.addAll(argMetadataDefFiles);

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

        sdf = makeDateFormat(getProperty(PROP_DATEFORMAT,
                                         DEFAULT_TIME_FORMAT));
        TimeZone.setDefault(DateUtil.TIMEZONE_GMT);


        //This will end up being from the properties
        htdocRoots.addAll(
            StringUtil.split(
                getProperty("ramadda.html.htdocroots", BLANK), ";", true,
                true));


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
        Statement statement = getDatabaseManager().select(COLUMNS_GLOBALS,
                                  TABLE_GLOBALS, new Clause[] {});
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
     *
     * @param entry _more_
     */
    protected void clearCache(Entry entry) {
        //        System.err.println ("Clear cache " + entry.getId());
        entryCache.remove(entry.getId());
        if (entry.isGroup()) {
            Group group = (Group) entry;
            groupCache.remove(group.getId());
            groupCache.remove(group.getFullName());
        }
    }


    /**
     * _more_
     */
    protected void clearCache() {
        //        System.err.println ("Clear full cache ");
        pageCache     = new Hashtable();
        pageCacheList = new ArrayList();
        entryCache    = new Hashtable();
        groupCache    = new Hashtable();
        dataTypeList  = null;
        topGroups     = null;
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
            } else if (handlerName.equals("accessmanager")) {
                handler = getAccessManager();
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
        OutputHandler outputHandler = new OutputHandler(getRepository()) {
            public boolean canHandle(String output) {
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
                types.add(new OutputType("Delete", OUTPUT_DELETER));
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
                return new Result(request.url(URL_ENTRY_DELETELIST, ARG_IDS,
                        idBuffer.toString()));
            }
        };
        addOutputHandler(outputHandler);
    }

    /** _more_ */
    public static final String OUTPUT_DELETER = "repository.delete";

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processEntryListDelete(Request request) throws Exception {
        List<Entry> entries = new ArrayList<Entry>();
        for (String id : StringUtil.split(request.getString(ARG_IDS, ""),
                                          ",", true, true)) {
            Entry entry = getEntry(request, id, false);
            if (entry == null) {
                throw new IllegalArgumentException("Could not find entry:"
                        + id);
            }
            if (entry.isTopGroup()) {
                StringBuffer sb = new StringBuffer();
                sb.append(note(msg("Cannot delete top-level group")));
                return new Result(msg("Entry Delete"), sb);
            }
            entries.add(entry);
        }
        return processEntryListDelete(request, entries);

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
    public Result processEntryListDelete(Request request, List<Entry> entries)
            throws Exception {
        StringBuffer sb = new StringBuffer();

        if (request.exists(ARG_CANCEL)) {
            if (entries.size() == 0) {
                return new Result(request.url(URL_ENTRY_SHOW));
            }
            String id = entries.get(0).getParentGroupId();
            return new Result(request.url(URL_ENTRY_SHOW, ARG_ID, id));
        }

        if (request.exists(ARG_DELETE_CONFIRM)) {
            return asynchDeleteEntries(request, entries);
        }


        if (entries.size() == 0) {
            return new Result(
                "", new StringBuffer(warning(msg("No entries selected"))));
        }

        StringBuffer msgSB    = new StringBuffer();
        StringBuffer idBuffer = new StringBuffer();
        for (Entry entry : entries) {
            idBuffer.append(",");
            idBuffer.append(entry.getId());
        }
        msgSB.append(
            msg("Are you sure you want to delete all of the entries?"));
        sb.append(request.form(URL_ENTRY_DELETELIST));
        String hidden = HtmlUtil.hidden(ARG_IDS, idBuffer.toString());
        String form = makeOkCancelForm(request, URL_ENTRY_DELETELIST,
                                       ARG_DELETE_CONFIRM, hidden);
        sb.append(question(msgSB.toString(), form));
        sb.append("<ul>");
        new OutputHandler(this).getEntryHtml(sb, entries, request, false,
                          false, true);
        sb.append("</ul>");
        return new Result(msg("Delete Confirm"), sb);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param url _more_
     * @param okArg _more_
     * @param extra _more_
     *
     * @return _more_
     */
    public String makeOkCancelForm(Request request, RequestUrl url,
                                   String okArg, String extra) {
        StringBuffer fb = new StringBuffer();
        fb.append(request.form(url));
        fb.append(extra);
        String okButton     = HtmlUtil.submit(msg("OK"), okArg);
        String cancelButton = HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
        String buttons      = buttons(okButton, cancelButton);
        fb.append(buttons);
        fb.append(HtmlUtil.formClose());
        return fb.toString();
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
            boolean      badAccess = inner instanceof AccessException;
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
                log("Error handling request:" + request, exc);
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


        List<Group> topGroups = new ArrayList<Group>(getTopGroups(request));
        topGroups.add(topGroup);
        //        System.err.println ("incoming:" + incoming);
        for (Group group : topGroups) {
            String name = "/" + getPathFromEntry(group);
            //            System.err.println ("\t" + name);
            if (incoming.startsWith(name + "/")) {
                request.setCollectionEntry(group);
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

        if ( !getDbProperty(ARG_ADMIN_INSTALLCOMPLETE, false)) {
            return getAdmin().doInitialization(request);
        }



        if ( !getUserManager().isRequestOk(request)
                || !apiMethod.isRequestOk(request, this)) {
            throw new AccessException(
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
            checkFilePath(fullPath);
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
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getPathFromEntry(Entry entry) {
        String name = entry.getName();
        name = name.toLowerCase();
        name = name.replace(" ", "_");
        name = name.replace(">", "_");
        return name;
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
        Metadata metadata =
            getMetadataManager().findMetadata((request.getCollectionEntry()
                != null)
                ? request.getCollectionEntry()
                : topGroup, AdminMetadataHandler.TYPE_TEMPLATE, true);
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
            HtmlUtil.div("", " id=\"tooltipdiv\" class=\"tooltip\" ")
            + HtmlUtil.div("", " id=\"output\"")
            + HtmlUtil.div("", " id=\"floatdiv\" class=\"floatdiv\" ");


        String content = new String(result.getContent());

        String html = StringUtil.replace(template, "${content}",
                                         content + jsContent);


        String userLink    = getUserManager().getUserLinks(request);

        String headerImage = fileUrl("/header.jpg");
        String headerTitle = getProperty(PROP_REPOSITORY_NAME, "Repository");
        html = StringUtil.replace(html, "${header.image}", headerImage);
        html = StringUtil.replace(html, "${header.title}", headerTitle);

        html = StringUtil.replace(html, "${userlink}", userLink);
        html = StringUtil.replace(html, "${repository_name}",
                                  getProperty(PROP_REPOSITORY_NAME,
                                      "Repository"));
        html = StringUtil.replace(html, "${footer}",
                                  getProperty(PROP_HTML_FOOTER, BLANK));
        html = StringUtil.replace(html, "${title}", result.getTitle());
        html = StringUtil.replace(html, "${root}", getUrlBase());

        html = StringUtil.replace(html, "${bottom}", result.getBottomHtml());



        List   links     = (List) result.getProperty(PROP_NAVLINKS);
        String linksHtml = HtmlUtil.space(1);
        if (links != null) {
            linksHtml = StringUtil.join(
                "<span class=\"separator\">&nbsp;|&nbsp;</span>", links);
        }
        List   sublinks     = (List) result.getProperty(PROP_NAVSUBLINKS);
        String sublinksHtml = BLANK;
        if (sublinks != null) {
            //            sublinksHtml = StringUtil.join("\n&nbsp;|&nbsp;\n", sublinks);
            sublinksHtml = StringUtil.join("", sublinks);
        }

        html = StringUtil.replace(html, "${links}", linksHtml);
        if (sublinksHtml.length() > 0) {
            html = StringUtil.replace(html, "${sublinks}",
                                      "<div class=\"subnav\">" + sublinksHtml
                                      + "</div>");
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
        Map<String, String> env  = System.getenv();
        String              prop = env.get(name);
        if (prop != null) {
            return prop;
        }
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
    public boolean getDbProperty(String name, boolean dflt) {
        return Misc.getProperty(dbProperties, name, dflt);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getConnection() throws Exception {
        return getConnection(false);
    }

    /**
     * _more_
     *
     * @param makeNewOne _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Connection getConnection(boolean makeNewOne) throws Exception {
        return getDatabaseManager().getConnection(makeNewOne);
    }




    /**
     * _more_
     *
     * @throws Exception _more_
     *
     */
    protected void initSchema() throws Exception {
        //Force a connection
        getConnection();
        String sql = IOUtil.readContents(getProperty(PROP_DB_SCRIPT),
                                         getClass());
        sql = getDatabaseManager().convertSql(sql);

        Statement statement = getConnection().createStatement();
        SqlUtil.loadSql(sql, statement, true);

        for (String file : typeDefinitionFiles) {
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
        SqlUtil.delete(getConnection(), TABLE_GLOBALS,
                       Clause.eq(COL_GLOBALS_NAME, name));
        getDatabaseManager().executeInsert(INSERT_GLOBALS,
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
        List<OutputType> list = new ArrayList<OutputType>();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.addOutputTypes(request, state, list);
        }
        return list;
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputHandler getOutputHandler(Request request) throws Exception {
        for (OutputHandler outputHandler : outputHandlers) {
            if (outputHandler.canHandle(request)) {
                return outputHandler;
            }
        }
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
        for (OutputHandler outputHandler : outputHandlers) {
            if (outputHandler.canHandle(type)) {
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

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Group getDummyGroup() throws Exception {
        Group dummyGroup = new Group(groupTypeHandler, true);
        dummyGroup.setId(getGUID());
        dummyGroup.setUser(getUserManager().getAnonymousUser());
        return dummyGroup;
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    public String getEntryResourceUrl(Request request, Entry entry) {
        String fileTail = getStorageManager().getFileTail(entry);
        return HtmlUtil.url(request.url(getRepository().URL_ENTRY_GET) + "/"
                            + fileTail, ARG_ID, entry.getId());
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
     *
     * @param request _more_
     *
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(Request request, Clause clause)
            throws Exception {

        List<Clause> clauses = new ArrayList<Clause>();
        if (clause != null) {
            clauses.add(clause);
        }
        clauses.add(Clause.eq(COL_ENTRIES_TYPE, TypeHandler.TYPE_GROUP));
        Statement statement = getDatabaseManager().select(COL_ENTRIES_ID,
                                  TABLE_ENTRIES, clauses);
        return getGroups(request, SqlUtil.readString(statement, 1));
    }

    /**
     * _more_
     *
     *
     *
     * @param request _more_
     * @param groupIds _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getGroups(Request request, String[] groupIds)
            throws Exception {
        List<Group> groupList = new ArrayList<Group>();
        for (int i = 0; i < groupIds.length; i++) {
            Group group = findGroup(request, groupIds[i]);
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
     * @param request _more_
     * @param entry _more_
     * @param forMenu _more_
     * @param forHeader _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Link> getEntryLinks(Request request, Entry entry,
                                       boolean forHeader)
            throws Exception {
        List<Link> links = new ArrayList<Link>();
        if ( !forHeader) {
            entry.getTypeHandler().getEntryLinks(request, entry, links,
                    forHeader);
            //            if(!forHeader)
            //                links.add(new Link(true));
            for (OutputHandler outputHandler : getOutputHandlers()) {
                outputHandler.getEntryLinks(request, entry, links, forHeader);
            }
            //            if(!forHeader)
            //                links.add(new Link(true));
        }
        OutputHandler outputHandler = getOutputHandler(request);
        if ( !entry.isTopGroup()) {
            links.addAll(outputHandler.getNextPrevLinks(request, entry,
                    request.getOutput()));
        }
        return links;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param forHeader _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getEntryLinksHtml(Request request, Entry entry,
                                       boolean forHeader)
            throws Exception {
        return StringUtil.join(HtmlUtil.space(1),
                               getEntryLinks(request, entry, forHeader));
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
    protected String getEntryLinksList(Request request, Entry entry)
            throws Exception {
        List<Link>   links = getEntryLinks(request, entry, false);
        StringBuffer menu  = new StringBuffer();
        menu.append("<table cellspacing=\"0\" cellpadding=\"0\">");
        for (Link link : links) {
            if (link.hr) {
                menu.append("<tr><td colspan=2><hr class=menuseparator>");
            } else {
                menu.append("<tr><td>");
                menu.append(HtmlUtil.img(link.getIcon()));
                menu.append(HtmlUtil.space(1));
                menu.append("</td><td>");
                menu.append(HtmlUtil.href(link.getUrl(), link.getLabel(),
                                          HtmlUtil.cssClass("menulink")));
            }
            menu.append("</td></tr>");
        }
        menu.append("</table>");
        return menu.toString();

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


    /** _more_ */
    static int blockCnt = 0;

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
                           String tabContentClass) {

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
                       HtmlUtil.cssClass("tabcontents")) + HtmlUtil.script(
                           "var " + id + "=new Tab(" + ids + ");\n");
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
        return xprocessEntrySearchForm(request, false);
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
    public Result processEntryBrowseSearchForm(Request request)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        getMetadataManager().addToBrowseSearchForm(request, sb);
        return makeResult(request, msg("Search Form"), sb, searchUrls);
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
    public Result processEntryTextSearchForm(Request request)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        sb.append(
            HtmlUtil.form(
                request.url(URL_ENTRY_SEARCH, ARG_NAME, WHAT_ENTRIES),
                " name=\"searchform\" "));


        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HtmlUtil.submitImage(getUrlBase() + ICON_BLANK, "submit"));
        TypeHandler typeHandler = getTypeHandler(request);
        String      output      = (String) request.getOutput(BLANK);
        String      buttons     = HtmlUtil.submit(msg("Search"), "submit");
        sb.append("<table width=\"90%\" border=0><tr><td>");
        typeHandler.addTextSearch(request, sb);
        sb.append("</table>");
        sb.append(HtmlUtil.p());
        sb.append(buttons);
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.formClose());
        return makeResult(request, msg("Search Form"), sb, searchUrls);
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
    public Result xprocessEntrySearchForm(Request request,
                                          boolean typeSpecific)
            throws Exception {

        StringBuffer sb = new StringBuffer();

        sb.append(
            HtmlUtil.form(
                request.url(URL_ENTRY_SEARCH, ARG_NAME, WHAT_ENTRIES),
                " name=\"searchform\" "));


        //Put in an empty submit button so when the user presses return 
        //it acts like a regular submit (not a submit to change the type)
        sb.append(HtmlUtil.submitImage(getUrlBase() + ICON_BLANK, "submit"));
        TypeHandler typeHandler = getTypeHandler(request);

        String      what        = (String) request.getWhat(BLANK);
        if (what.length() == 0) {
            what = WHAT_ENTRIES;
        }


        String output = (String) request.getOutput(BLANK);
        String buttons = buttons(HtmlUtil.submit(msg("Search"), "submit"),
                                 HtmlUtil.submit(msg("Search Subset"),
                                     "submit_subset"));

        sb.append(HtmlUtil.p());
        sb.append(buttons);
        sb.append(HtmlUtil.p());
        sb.append("<table width=\"90%\" border=0><tr><td>");

        Object       oldValue = request.remove(ARG_RELATIVEDATE);
        List<Clause> where    = typeHandler.assembleWhereClause(request);
        if (oldValue != null) {
            request.put(ARG_RELATIVEDATE, oldValue);
        }

        typeHandler.addToSearchForm(request, sb, where, true);


        StringBuffer metadataSB = new StringBuffer();
        metadataSB.append(HtmlUtil.formTable());
        getMetadataManager().addToSearchForm(request, metadataSB);
        metadataSB.append(HtmlUtil.formTableClose());
        sb.append(makeShowHideBlock(request, msg("Metadata"), metadataSB,
                                    false));


        StringBuffer outputForm = new StringBuffer(HtmlUtil.formTable());
        if (output.length() == 0) {}
        else {
            outputForm.append(HtmlUtil.hidden(ARG_OUTPUT, output));
        }

        List orderByList = new ArrayList();
        orderByList.add(new TwoFacedObject(msg("None"), "none"));
        orderByList.add(new TwoFacedObject(msg("From Date"), "fromdate"));
        orderByList.add(new TwoFacedObject(msg("To Date"), "todate"));
        orderByList.add(new TwoFacedObject(msg("Create Date"), "createdate"));
        orderByList.add(new TwoFacedObject(msg("Name"), "name"));

        String orderBy =
            HtmlUtil.select(ARG_ORDERBY, orderByList,
                            request.getString(ARG_ORDERBY,
                                "none")) + HtmlUtil.checkbox(ARG_ASCENDING,
                                    "true",
                                    request.get(ARG_ASCENDING,
                                        false)) + HtmlUtil.space(1)
                                            + msg("ascending");
        outputForm.append(HtmlUtil.formEntry(msgLabel("Order By"), orderBy));
        outputForm.append(HtmlUtil.formTableClose());



        sb.append(makeShowHideBlock(request, msg("Output"), outputForm,
                                    false));

        sb.append(HtmlUtil.p());
        sb.append(buttons);
        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.formClose());

        return makeResult(request, msg("Search Form"), sb, searchUrls);

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
        String offextra = " class=\"subnavoffcomp\" ";
        String onextra  = " class=\"subnavoncomp\" ";
        String type     = request.getRequestPath();

        String l = HtmlUtil.img(fileUrl(ICON_LCURVE), "",
                                "  class=\"curve\"");
        String r = HtmlUtil.img(fileUrl(ICON_RCURVE), "",
                                " class=\"curve\"  ");

        for (int i = 0; i < urls.length; i++) {
            String label = urls[i].getLabel();
            label = msg(label);
            if (label == null) {
                label = urls[i].toString();
            }

            if (type.endsWith(urls[i].getPath())) {
                links.add(HtmlUtil.span(l + label + r, onextra));
            } else {
                links.add(HtmlUtil.href(request.url(urls[i]) + arg, label,
                                        offextra));
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
                item = HtmlUtil.href(request.url(URL_SEARCH_FORM, ARG_WHAT,
                        whats[i], ARG_FORM_TYPE, formType), names[i], extra2);
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
                links.add(HtmlUtil.href(request.url(URL_SEARCH_FORM,
                        ARG_WHAT, BLANK + tfo.getId(), ARG_TYPE,
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
            User user = request.getUser();
            isAdmin = user.getAdmin();
        }

        for (ApiMethod apiMethod : topLevelMethods) {
            if (apiMethod.getMustBeAdmin() && !isAdmin) {
                continue;
            }
            if (apiMethod == homeApi) {
                links.add(HtmlUtil.href(fileUrl(apiMethod.getRequest()),
                                        msg(apiMethod.getName()), extra));
            } else {
                links.add(HtmlUtil.href(request.url(apiMethod.getUrl()),
                                        msg(apiMethod.getName()), extra));
            }
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
                nmp.setProjectionImpl(new LatLonProjection(BLANK,
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
            //        GuiUtils.showOkCancelDialog(null,BLANK,new JLabel(new ImageIcon(image)),null);
            String                path = "foo.png";
            ByteArrayOutputStream bos  = new ByteArrayOutputStream();
            ImageUtils.writeImageToFile(image, path, bos, 1.0f);
            byte[] imageBytes = bos.toByteArray();
            return new Result(BLANK, imageBytes,
                              getMimeTypeFromSuffix(".png"));
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
     * @param from _more_
     * @param parent _more_
     * @param actionId _more_
     *
     * @throws Exception _more_
     */
    protected void copyEntry(Entry from, Group parent, Object actionId)
            throws Exception {
        Entry  newEntry = (Entry) from.clone();
        String newId    = getGUID();
    }




    /**
     * _more_
     *
     * @param fromEntry _more_
     * @param toEntry _more_
     *
     * @return _more_
     */
    protected boolean okToMove(Entry fromEntry, Entry toEntry) {
        if ( !toEntry.isGroup()) {
            return false;
        }

        if (toEntry.getId().equals(fromEntry.getId())) {
            return false;
        }
        if (toEntry.getParentGroup() == null) {
            return true;
        }
        return okToMove(fromEntry, toEntry.getParentGroup());
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
    public Result processEntryCopy(Request request) throws Exception {

        String fromId = request.getString(ARG_FROM, "");
        if (fromId == null) {
            throw new IllegalArgumentException("No " + ARG_FROM + " given");
        }
        Entry fromEntry = getEntry(request, fromId);
        if (fromEntry == null) {
            throw new IllegalArgumentException("Could not find entry "
                    + fromId);
        }


        if (request.exists(ARG_CANCEL)) {
            return new Result(request.entryUrl(URL_ENTRY_SHOW, fromEntry));
        }


        if ( !request.exists(ARG_TO)) {
            StringBuffer sb     = new StringBuffer();
            List<Entry>  cart   = getUserManager().getCart(request);
            boolean      didOne = false;
            sb.append(makeEntryHeader(request, fromEntry));
            for (Entry entry : cart) {
                if ( !getAccessManager().canDoAction(request, entry,
                        Permission.ACTION_NEW)) {
                    continue;
                }
                if ( !entry.isGroup()) {
                    continue;
                }
                if ( !okToMove(fromEntry, entry)) {
                    continue;
                }
                if ( !didOne) {
                    sb.append(header("Move to:"));
                    sb.append("<ul>");
                }
                sb.append("<li> ");
                sb.append(HtmlUtil.href(request.url(URL_ENTRY_COPY, ARG_FROM,
                        fromEntry.getId(), ARG_TO, entry.getId(), ARG_ACTION,
                        ACTION_MOVE), entry.getLabel()));
                sb.append(HtmlUtil.br());
                didOne = true;

            }
            if ( !didOne) {
                sb.append(
                    note(msg(
                        "You need to add a destination group to your cart")));
            } else {
                sb.append("</ul>");
            }

            return new Result(msg("Entry Move/Copy"), sb);
        }


        String toId = request.getString(ARG_TO, "");
        if (toId == null) {
            throw new IllegalArgumentException("No " + ARG_TO + " given");
        }

        Entry toEntry = getEntry(request, toId);
        if (toEntry == null) {
            throw new IllegalArgumentException("Could not find entry "
                    + toId);
        }
        if ( !toEntry.isGroup()) {
            throw new IllegalArgumentException(
                "Can only copy/move to a group");
        }
        Group toGroup = (Group) toEntry;


        if ( !getAccessManager().canDoAction(request, fromEntry,
                                             Permission.ACTION_EDIT)) {
            throw new AccessException("Cannot move:" + fromEntry.getLabel());
        }


        if ( !getAccessManager().canDoAction(request, toEntry,
                                             Permission.ACTION_NEW)) {
            throw new AccessException("Cannot copy to:" + toEntry.getLabel());
        }


        if ( !okToMove(fromEntry, toEntry)) {
            StringBuffer sb = new StringBuffer();
            sb.append(makeEntryHeader(request, fromEntry));
            sb.append(error(msg("Cannot move a group to its descendent")));
            return new Result("", sb);
        }



        String action = request.getString(ARG_ACTION, ACTION_COPY);



        if ( !request.exists(ARG_MOVE_CONFIRM)) {
            StringBuffer sb = new StringBuffer();
            sb.append(msgLabel("Are you sure you want to move"));
            sb.append(HtmlUtil.br());
            sb.append(HtmlUtil.space(3));
            sb.append(fromEntry.getLabel());
            sb.append(HtmlUtil.br());
            sb.append(msgLabel("To"));
            sb.append(HtmlUtil.br());
            sb.append(HtmlUtil.space(3));
            sb.append(toEntry.getLabel());
            sb.append(HtmlUtil.br());

            String hidden = HtmlUtil.hidden(ARG_FROM, fromEntry.getId())
                            + HtmlUtil.hidden(ARG_TO, toEntry.getId())
                            + HtmlUtil.hidden(ARG_ACTION, action);
            String form = makeOkCancelForm(request, URL_ENTRY_COPY,
                                           ARG_MOVE_CONFIRM, hidden);
            return new Result(msg("Move confirm"),
                              new StringBuffer(question(sb.toString(),
                                  form)));
        }


        Connection connection = getConnection(true);
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        try {
            if (action.equals(ACTION_MOVE)) {
                fromEntry.setParentGroup(toGroup);

                String oldId = fromEntry.getId();
                String newId = oldId;
                //TODO: critical section around new group id
                if (fromEntry.isGroup()) {
                    newId = getGroupId(toGroup);
                    fromEntry.setId(newId);
                    String[] info = {
                        TABLE_ENTRIES, COL_ENTRIES_ID, TABLE_ENTRIES,
                        COL_ENTRIES_PARENT_GROUP_ID, TABLE_METADATA,
                        COL_METADATA_ENTRY_ID, TABLE_COMMENTS,
                        COL_COMMENTS_ENTRY_ID, TABLE_ASSOCIATIONS,
                        COL_ASSOCIATIONS_FROM_ENTRY_ID, TABLE_ASSOCIATIONS,
                        COL_ASSOCIATIONS_TO_ENTRY_ID, TABLE_PERMISSIONS,
                        COL_PERMISSIONS_ENTRY_ID
                    };


                    for (int i = 0; i < info.length; i += 2) {
                        String sql = "UPDATE  " + info[i] + " SET "
                                     + SqlUtil.unDot(info[i + 1]) + " = "
                                     + SqlUtil.quote(newId) + " WHERE "
                                     + SqlUtil.eq(info[i + 1],
                                         SqlUtil.quote(oldId));
                        //                        System.err.println (sql);
                        statement.execute(sql);
                    }

                    //TODO: we also cache the group full names
                    entryCache.remove(oldId);
                    entryCache.put(fromEntry.getId(), fromEntry);
                    groupCache.remove(fromEntry.getId());
                    groupCache.put(fromEntry.getId(), (Group) fromEntry);
                }

                //Change the parent
                String sql = "UPDATE  " + TABLE_ENTRIES + " SET "
                             + SqlUtil.unDot(COL_ENTRIES_PARENT_GROUP_ID)
                             + " = "
                             + SqlUtil.quote(fromEntry.getParentGroupId())
                             + " WHERE "
                             + SqlUtil.eq(COL_ENTRIES_ID,
                                          SqlUtil.quote(fromEntry.getId()));
                statement.execute(sql);
                connection.commit();
                connection.setAutoCommit(true);
                return new Result(request.url(URL_ENTRY_SHOW, ARG_ID,
                        fromEntry.getId()));
            }
        } finally {
            try {
                connection.close();
            } catch (Exception exc) {}
        }


        String       title = (action.equals(ACTION_COPY)
                              ? "Entry Copy"
                              : "Entry Move");
        StringBuffer sb    = new StringBuffer();
        return new Result(msg(title), sb);
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
        Entry entry = getEntry(request, entryId);
        if (entry == null) {
            throw new IllegalArgumentException(
                "Could not find entry with id:" + entryId);
        }

        if ( !entry.getResource().isUrl()) {
            if ( !getAccessManager().canDownload(request, entry)) {
                throw new IllegalArgumentException(
                    "Cannot download file with id:" + entryId);
            }
        }
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
            byte[] bytes = IOUtil.readBytes(IOUtil.getInputStream(thumb,
                               getClass()));
            return new Result(
                BLANK, bytes,
                IOUtil.getFileExtension(entry.getResource().getPath()));
        } else {
            return new Result(BLANK,
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
    protected Entry getEntry(Request request, String entryId)
            throws Exception {
        return getEntry(request, entryId, true);
    }

    /**
     * _more_
     *
     * @param entryId _more_
     * @param request _more_
     * @param andFilter _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry getEntry(Request request, String entryId,
                             boolean andFilter)
            throws Exception {
        return getEntry(request, entryId, andFilter, false);
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
    protected Entry getEntry(Request request) throws Exception {
        String entryId = request.getString(ARG_ID, BLANK);
        Entry  entry   = getEntry(request, entryId);
        if (entry == null) {
            Entry tmp = getEntry(request, request.getString(ARG_ID, BLANK),
                                 false);
            if (tmp != null) {
                throw new AccessException(
                    "You do not have access to this entry");
            }
            throw new IllegalArgumentException("Could not find entry:"
                    + request.getString(ARG_ID, BLANK));
        }
        return entry;
    }




    /**
     * _more_
     *
     * @param entryId _more_
     * @param request _more_
     * @param andFilter _more_
     * @param abbreviated _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Entry getEntry(Request request, String entryId,
                             boolean andFilter, boolean abbreviated)
            throws Exception {

        if (entryId == null) {
            return null;
        }
        if (entryId.equals(topGroup.getId())) {
            return topGroup;
        }

        Entry entry = (Entry) entryCache.get(entryId);
        if (entry != null) {
            if ( !andFilter) {
                return entry;
            }
            return getAccessManager().filterEntry(request, entry);
        }

        if (isLocalFileEntry(entryId)) {
            File f          = new File(getFileFromId(entryId));
            File parentFile = f.getParentFile();
            if ((parentFile != null)
                    && (parentFile.getParentFile() == null)) {
                parentFile = null;
            }
            Group parent = (Group) ((parentFile != null)
                                    ? getEntry(request,
                                        getIdFromFile(parentFile), andFilter,
                                        abbreviated)
                                    : null);
            if (parent == null) {
                parent = topGroup;
            }
            Metadata patternMetadata =
                getMetadataManager().findMetadata(parent,
                    AdminMetadataHandler.TYPE_LOCALFILE_PATTERN, true);
            PatternFileFilter filter = null;
            if ((patternMetadata != null)
                    && (patternMetadata.getAttr1().trim().length() > 0)) {
                try {
                    filter =
                        new PatternFileFilter(patternMetadata.getAttr1());
                    if ( !filter.accept(f)) {
                        return null;
                    }
                } catch (Exception exc) {
                    throw new IllegalStateException(
                        "Bad local files pattern:"
                        + patternMetadata.getAttr1());
                }
            }


            TypeHandler handler = (f.isDirectory()
                                   ? getTypeHandler(TypeHandler.TYPE_GROUP)
                                   : getTypeHandler(TypeHandler.TYPE_FILE));
            if ( !f.exists()) {
                return null;
            }
            entry = (f.isDirectory()
                     ? (Entry) new Group(entryId, handler)
                     : new Entry(entryId, handler));
            String name = f.toString();

            if ( !localFilePaths.contains(name.replace("\\", "/"))) {
                name = IOUtil.getFileTail(name);
            }
            entry.setIsLocalFile(true);
            entry.initEntry(name, "", parent, "",
                            getUserManager().localFileUser,
                            new Resource(f, (f.isDirectory()
                                             ? Resource.TYPE_LOCAL_DIRECTORY
                                             : Resource
                                             .TYPE_LOCAL_FILE)), "",
                                                 f.lastModified(),
                                                     f.lastModified(),
                                                         f.lastModified(),
                                                             null);
        } else if (isCatalogEntry(entryId)) {
            String url    = getCatalogFromId(entryId);
            Group  parent = null;
            if (parent == null) {
                parent = topGroup;
            }
            TypeHandler handler = getTypeHandler(TypeHandler.TYPE_GROUP);
            entry = new Group(entryId, handler);
            String name = IOUtil.getFileTail(url);
            entry.setIsLocalFile(true);
            Date now = new Date();
            entry.initEntry(name, "", parent, "",
                            getUserManager().localFileUser,
                            new Resource(url, Resource.TYPE_URL), "",
                            now.getTime(), now.getTime(), now.getTime(),
                            null);
        } else if (isGeneratedEntry(entryId)) {
            String[] ids = getGeneratedIdPair(entryId);
            if (ids.length != 2) {
                throw new IllegalArgumentException("Bad generated id:"
                        + entryId);
            }
            Entry parentEntry = getEntry(request, ids[0]);
            if (parentEntry == null) {
                return null;
            }
            entry = parentEntry.createGeneratedEntry(request, ids[1]);
        } else {
            Statement entryStmt =
                getDatabaseManager().select(COLUMNS_ENTRIES, TABLE_ENTRIES,
                                            Clause.eq(COL_ENTRIES_ID,
                                                entryId));

            ResultSet results = entryStmt.getResultSet();
            if ( !results.next()) {
                entryStmt.close();
                return null;
            }

            TypeHandler typeHandler = getTypeHandler(results.getString(2));
            entry = typeHandler.getEntry(results, abbreviated);
            entryStmt.close();

        }
        if (andFilter) {
            entry = getAccessManager().filterEntry(request, entry);
        }

        if ( !abbreviated && (entry != null)) {
            if (entryCache.size() > ENTRY_CACHE_LIMIT) {
                entryCache = new Hashtable();
            }
            //            System.err.println ("caching " + entryId);
            entryCache.put(entryId, entry);
        }
        return entry;

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
        if ((parentGroup != null) && request.getUser().getAdmin()) {
            sb.append("<p>&nbsp;");
            sb.append(
                request.form(
                    getHarvesterManager().URL_HARVESTERS_IMPORTCATALOG));
            sb.append(HtmlUtil.hidden(ARG_GROUP, parentGroup.getId()));
            sb.append(HtmlUtil.submit(msg("Import a catalog:")));
            sb.append(HtmlUtil.space(1));
            sb.append(HtmlUtil.input(ARG_CATALOG, BLANK, HtmlUtil.SIZE_70)
                      + HtmlUtil.space(1)
                      + HtmlUtil.checkbox(ARG_RECURSE, "true", false)
                      + " Recurse");


            sb.append(HtmlUtil.formClose());
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
    public Result processEntryNew(Request request) throws Exception {
        Group        group = findGroup(request);
        StringBuffer sb    = new StringBuffer();
        sb.append(makeEntryHeader(request, group));
        sb.append(HtmlUtil.p());
        sb.append(request.form(URL_ENTRY_FORM));
        sb.append(HtmlUtil.submit("Create a:"));
        sb.append(HtmlUtil.space(1));
        sb.append(makeTypeSelect(request, false));
        sb.append(HtmlUtil.hidden(ARG_GROUP, group.getId()));
        sb.append(HtmlUtil.formClose());
        sb.append(makeNewGroupForm(request, group, BLANK));

        /*
        sb.append(request.uploadForm(URL_ENTRY_XMLCREATE));
        sb.append("File:" + HtmlUtil.fileInput(ARG_FILE, ""));
        sb.append("<br>" + HtmlUtil.submit("Submit"));
        sb.append(HtmlUtil.formClose());
        */


        return new Result("New Form", sb, Result.TYPE_HTML);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param node _more_
     * @param entries _more_
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Entry processEntryXml(Request request, Element node,
                                  Hashtable entries, Hashtable files)
            throws Exception {
        String name = XmlUtil.getAttribute(node, ATTR_NAME);
        String type = XmlUtil.getAttribute(node, ATTR_TYPE,
                                           TypeHandler.TYPE_FILE);
        String dataType    = XmlUtil.getAttribute(node, ATTR_DATATYPE, "");
        String description = XmlUtil.getAttribute(node, ATTR_DESCRIPTION, "");
        String file = XmlUtil.getAttribute(node, ATTR_FILE, (String) null);
        if (file != null) {
            String tmp = (String) files.get(file);
            String newFile = getStorageManager().moveToStorage(request,
                                 new File(tmp)).toString();
            file = newFile;
        }
        String url   = XmlUtil.getAttribute(node, ATTR_URL, (String) null);
        String tmpid = XmlUtil.getAttribute(node, ATTR_ID, (String) null);
        String parentId = XmlUtil.getAttribute(node, ATTR_PARENT,
                              topGroup.getId());
        Group parentGroup = (Group) entries.get(parentId);
        if (parentGroup == null) {
            parentGroup = (Group) getEntry(request, parentId);
            if (parentGroup == null) {
                throw new IllegalArgumentException("Could not find parent:"
                        + parentId);
            }
        }
        if ( !getAccessManager().canDoAction(request, parentGroup,
                                             Permission.ACTION_NEW)) {
            throw new IllegalArgumentException("Cannot add to parent group:"
                    + parentId);
        }

        TypeHandler typeHandler = getTypeHandler(type);
        if (typeHandler == null) {
            throw new IllegalArgumentException("Could not find type:" + type);
        }
        String   id = (typeHandler.isType(TypeHandler.TYPE_GROUP)
                       ? getGroupId(parentGroup)
                       : getGUID());

        Resource resource;

        if (file != null) {
            resource = new Resource(file, Resource.TYPE_STOREDFILE);
        } else if (url != null) {
            resource = new Resource(url, Resource.TYPE_URL);
        } else {
            resource = new Resource("", Resource.TYPE_UNKNOWN);
        }
        Date createDate = new Date();
        Date fromDate   = createDate;
        //        System.err.println("node:" + XmlUtil.toString(node));
        if (XmlUtil.hasAttribute(node, ATTR_FROMDATE)) {
            fromDate = parseDate(XmlUtil.getAttribute(node, ATTR_FROMDATE));
        }
        Date toDate = fromDate;
        if (XmlUtil.hasAttribute(node, ATTR_TODATE)) {
            toDate = parseDate(XmlUtil.getAttribute(node, ATTR_TODATE));
        }

        Entry entry = typeHandler.createEntry(id);
        entry.initEntry(name, description, parentGroup, "",
                        request.getUser(), resource, dataType,
                        createDate.getTime(), fromDate.getTime(),
                        toDate.getTime(), null);


        entry.setNorth(XmlUtil.getAttribute(node, ATTR_NORTH,
                                            entry.getNorth()));
        entry.setSouth(XmlUtil.getAttribute(node, ATTR_SOUTH,
                                            entry.getSouth()));
        entry.setEast(XmlUtil.getAttribute(node, ATTR_EAST, entry.getEast()));
        entry.setWest(XmlUtil.getAttribute(node, ATTR_WEST, entry.getWest()));

        entry.getTypeHandler().initializeEntry(request, entry, node);


        if (tmpid != null) {
            entries.put(tmpid, entry);
        }
        return entry;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param node _more_
     * @param entries _more_
     * @param files _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String processAssociationXml(Request request, Element node,
                                         Hashtable entries, Hashtable files)
            throws Exception {

        String fromId    = XmlUtil.getAttribute(node, ATTR_FROM);
        String toId      = XmlUtil.getAttribute(node, ATTR_TO);
        Entry  fromEntry = (Entry) entries.get(fromId);
        Entry  toEntry   = (Entry) entries.get(toId);
        if (fromEntry == null) {
            fromEntry = getEntry(request, fromId);
            if (fromEntry == null) {
                throw new IllegalArgumentException(
                    "Could not find from entry:" + fromId);
            }
        }
        if (toEntry == null) {
            toEntry = getEntry(request, toId);
            if (toEntry == null) {
                throw new IllegalArgumentException("Could not find to entry:"
                        + toId);
            }
        }
        return addAssociation(request, fromEntry, toEntry,
                              XmlUtil.getAttribute(node, ATTR_NAME));
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
    public Result processEntryXmlCreate(Request request) throws Exception {
        try {
            return processEntryXmlCreateInner(request);
        } catch (Exception exc) {
            exc.printStackTrace();
            if (request.getOutput().equals("xml")) {
                return new Result(XmlUtil.tag(TAG_RESPONSE,
                        XmlUtil.attr(ATTR_CODE, "error"),
                        "" + exc.getMessage()), MIME_XML);
            }
            return new Result("Error:" + exc, Result.TYPE_XML);
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
    private Result processEntryXmlCreateInner(Request request)
            throws Exception {

        String file = request.getUploadedFile(ARG_FILE);
        if (file == null) {
            throw new IllegalArgumentException("No file argument given");
        }
        String    entriesXml        = null;
        Hashtable origFileToStorage = new Hashtable();
        //        System.err.println ("\nprocessing");
        if (file.endsWith(".zip")) {
            ZipInputStream zin =
                new ZipInputStream(IOUtil.getInputStream(file));
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                String entryName = ze.getName();
                //                System.err.println ("ZIP: " + ze.getName());
                if (entryName.equals("entries.xml")) {
                    entriesXml = new String(IOUtil.readBytes(zin, null,
                            false));
                } else {
                    String name =
                        IOUtil.getFileTail(ze.getName().toLowerCase());
                    File f = getStorageManager().getTmpFile(request, name);
                    FileOutputStream fos = new FileOutputStream(f);
                    IOUtil.writeTo(zin, fos);
                    fos.close();
                    //                    System.err.println ("orig file:" + ze.getName() + " tmp file:" + f);
                    origFileToStorage.put(ze.getName(), f.toString());
                }
            }
            if (entriesXml == null) {
                throw new IllegalArgumentException(
                    "No entries.xml file provided");
            }
        }

        if (entriesXml == null) {
            entriesXml = IOUtil.readContents(file, getClass());
        }

        //        System.err.println ("xml:" + entriesXml);

        List      newEntries = new ArrayList();
        Hashtable entries    = new Hashtable();
        Element   root       = XmlUtil.getRoot(entriesXml);
        NodeList  children   = XmlUtil.getElements(root);

        Document  resultDoc  = XmlUtil.makeDocument();
        Element resultRoot = XmlUtil.create(resultDoc, TAG_RESPONSE, null,
                                            new String[] { ATTR_CODE,
                "ok" });
        for (int i = 0; i < children.getLength(); i++) {
            Element node = (Element) children.item(i);
            if (node.getTagName().equals(TAG_ENTRY)) {
                Entry entry = processEntryXml(request, node, entries,
                                  origFileToStorage);
                XmlUtil.create(resultDoc, TAG_ENTRY, resultRoot,
                               new String[] { ATTR_ID,
                        entry.getId() });
                newEntries.add(entry);
                NodeList entryChildren = XmlUtil.getElements(node);
                for (Element entryChild : (List<Element>) entryChildren) {
                    if (entryChild.getTagName().equals(TAG_METADATA)) {
                        entry.addMetadata(
                            new Metadata(
                                getGUID(), entry.getId(),
                                XmlUtil.getAttribute(entryChild, ATTR_TYPE),
                                XmlUtil.getAttribute(
                                    entryChild, ATTR_INHERITED,
                                    DFLT_INHERITED), XmlUtil.getAttribute(
                                        entryChild, ATTR_ATTR1,
                                        ""), XmlUtil.getAttribute(
                                            entryChild, ATTR_ATTR2,
                                            ""), XmlUtil.getAttribute(
                                                entryChild, ATTR_ATTR3,
                                                    ""), XmlUtil.getAttribute(
                                                        entryChild,
                                                            ATTR_ATTR4, "")));
                    } else {
                        throw new IllegalArgumentException("Unknown tag:"
                                + node.getTagName());
                    }
                }
                if (XmlUtil.getAttribute(node, ATTR_ADDMETADATA, false)) {
                    List<Entry> tmpEntries =
                        (List<Entry>) Misc.newList(entry);
                    addInitialMetadata(request, tmpEntries);
                }

            } else if (node.getTagName().equals(TAG_ASSOCIATION)) {
                String id = processAssociationXml(request, node, entries,
                                origFileToStorage);
                XmlUtil.create(resultDoc, TAG_ASSOCIATION, resultRoot,
                               new String[] { ATTR_ID,
                        id });
            } else {
                throw new IllegalArgumentException("Unknown tag:"
                        + node.getTagName());
            }
        }


        insertEntries(newEntries, true);

        if (request.getOutput().equals("xml")) {
            //TODO: Return a list of the newly created entries
            String xml = XmlUtil.toString(resultRoot);
            return new Result(xml, MIME_XML);
        }

        StringBuffer sb = new StringBuffer("OK");
        return new Result("", sb);

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


        Group        group = null;
        String       type  = null;
        Entry        entry = null;
        StringBuffer sb    = new StringBuffer();
        //        sb.append(makeTabs(Misc.newList("title1","title2","title3"),
        //                           Misc.newList("contents1","contents2","contents3")));
        if (request.defined(ARG_ID)) {
            entry = getEntry(request);
            /*
            if (entry.isTopGroup()) {
                sb.append(makeEntryHeader(request, entry));
                sb.append(note("Cannot edit top-level group"));
                return makeEntryEditResult(request, entry, "Edit Entry", sb);
                }*/
            type = entry.getTypeHandler().getType();
            if ( !entry.isTopGroup()) {
                group = findGroup(request, entry.getParentGroupId());
            }
        }
        boolean isEntryTop = ((entry != null) && entry.isTopGroup());


        if ( !isEntryTop && (group == null)) {
            group = findGroup(request);
        }
        if (type == null) {
            type = request.getString(ARG_TYPE, (String) null);
        }

        if (entry == null) {
            sb.append(makeEntryHeader(request, group));
        } else {
            sb.append(makeEntryHeader(request, entry));
        }

        if (((entry != null) && entry.getIsLocalFile())) {
            sb.append("This is a local file and cannot be edited");
            return makeEntryEditResult(request, entry, "Entry Edit", sb);
        }


        if (type == null) {
            sb.append(request.form(URL_ENTRY_FORM, " name=\"entryform\" "));
        } else {
            sb.append(request.uploadForm(URL_ENTRY_CHANGE,
                                         " name=\"entryform\" "));
        }

        sb.append(HtmlUtil.formTable());
        String title = BLANK;

        if (type == null) {
            sb.append(HtmlUtil.formEntry("Type:",
                                         makeTypeSelect(request, false)));

            sb.append(
                HtmlUtil.formEntry(
                    BLANK, HtmlUtil.submit("Select Type to Add")));
            sb.append(HtmlUtil.hidden(ARG_GROUP, group.getId()));
        } else {
            TypeHandler typeHandler = ((entry == null)
                                       ? getTypeHandler(type)
                                       : entry.getTypeHandler());


            String submitButton = HtmlUtil.submit(title = ((entry == null)
                    ? "Add " + typeHandler.getLabel()
                    : "Edit " + typeHandler.getLabel()));

            List<Metadata> metadataList = ((entry == null)
                                           ? (List<Metadata>) new ArrayList<Metadata>()
                                           : getMetadataManager().getMetadata(
                                               entry));
            String metadataButton = HtmlUtil.submit("Edit Metadata",
                                        ARG_EDIT_METADATA);

            String deleteButton = (((entry != null) && entry.isTopGroup())
                                   ? ""
                                   : HtmlUtil.submit(msg("Delete"),
                                       ARG_DELETE));

            String cancelButton = HtmlUtil.submit(msg("Cancel"), ARG_CANCEL);
            String buttons      = ((entry != null)
                                   ? buttons(submitButton, deleteButton,
                                             cancelButton)
                                   : buttons(submitButton, cancelButton));


            String topLevelCheckbox = "";
            if ((entry == null) && request.getUser().getAdmin()) {
                topLevelCheckbox = HtmlUtil.space(1)
                                   + HtmlUtil.checkbox(ARG_TOPLEVEL, "true",
                                       false) + HtmlUtil.space(1)
                                           + msg("Make top level");

            }
            topLevelCheckbox = "";
            sb.append(HtmlUtil.row(HtmlUtil.colspan(buttons
                    + topLevelCheckbox, 2)));
            if (entry != null) {
                sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
            } else {
                sb.append(HtmlUtil.hidden(ARG_TYPE, type));
                sb.append(HtmlUtil.hidden(ARG_GROUP, group.getId()));
            }
            //            sb.append(HtmlUtil.formEntry("Type:", typeHandler.getLabel()));
            typeHandler.addToEntryForm(request, sb, entry);
            sb.append(HtmlUtil.row(HtmlUtil.colspan(buttons, 2)));
        }
        sb.append(HtmlUtil.formTableClose());
        if (entry == null) {
            return new Result(title, sb);
        }
        return makeEntryEditResult(request, entry, title, sb);
    }








    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param title _more_
     * @param sb _more_
     *
     * @return _more_
     */
    protected Result makeEntryEditResult(Request request, Entry entry,
                                         String title, StringBuffer sb) {
        Result result = new Result(title, sb);
        result.putProperty(PROP_NAVSUBLINKS,
                           getSubNavLinks(request, entryEditUrls,
                                          "?" + ARG_ID + "="
                                          + entry.getId()));
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
    public String getCommentHtml(Request request, Entry entry)
            throws Exception {
        boolean canEdit = getAccessManager().canDoAction(request, entry,
                              Permission.ACTION_EDIT);
        boolean canComment = getAccessManager().canDoAction(request, entry,
                                 Permission.ACTION_COMMENT);

        StringBuffer  sb       = new StringBuffer();
        List<Comment> comments = getComments(request, entry);

        if (canComment) {
            sb.append(request.form(URL_COMMENTS_ADD, BLANK));
            sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
            sb.append(HtmlUtil.formEntry(BLANK,
                                         HtmlUtil.submit("Add Comment",
                                             ARG_ADD)));
            sb.append(HtmlUtil.formClose());
        }

        if (comments.size() == 0) {
            sb.append("<br>");
            sb.append(msg("No comments"));
        }
        //        sb.append("<table>");
        int rowNum = 1;
        for (Comment comment : comments) {
            //            sb.append(HtmlUtil.formEntry(BLANK, HtmlUtil.hr()));
            //TODO: Check for access
            String deleteLink = ( !canEdit
                                  ? ""
                                  : HtmlUtil
                                      .href(request
                                          .url(URL_COMMENTS_EDIT, ARG_DELETE,
                                              "true", ARG_ID, entry.getId(),
                                              ARG_COMMENT_ID,
                                              comment.getId()), HtmlUtil
                                                  .img(fileUrl(ICON_DELETE),
                                                      msg(
                                                      "Delete comment"))));
            if (canEdit) {
                //                sb.append(HtmlUtil.formEntry(BLANK, deleteLink));
            }
            //            sb.append(HtmlUtil.formEntry("Subject:", comment.getSubject()));


            String theClass = HtmlUtil.cssClass("listrow" + rowNum);
            rowNum++;
            if (rowNum > 2) {
                rowNum = 1;
            }
            StringBuffer content = new StringBuffer();
            content.append("<table>");
            String byLine = "By: " + comment.getUser().getLabel() + " @ "
                            + formatDate(request, comment.getDate())
                            + HtmlUtil.space(1) + deleteLink;
            //            content.append(HtmlUtil.formEntry("By:",
            //                                         ));
            content.append(HtmlUtil.formEntryTop("", comment.getComment()));
            content.append("</table>");
            sb.append(HtmlUtil.div(makeShowHideBlock(request,
                    "<b>Subject</b>:" + comment.getSubject()
                    + HtmlUtil.space(2) + byLine, content, true,
                        ""), theClass));
        }
        //        sb.append("</table>");
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
    public Result processCommentsShow(Request request) throws Exception {
        Entry        entry = getEntry(request);
        StringBuffer sb    = new StringBuffer();
        if (request.exists(ARG_MESSAGE)) {
            sb.append(note(request.getUnsafeString(ARG_MESSAGE, BLANK)));
        }
        sb.append(makeEntryHeader(request, entry));
        sb.append("<p>");
        //        sb.append(msg("Comments"));
        sb.append("<p>");
        sb.append(getCommentHtml(request, entry));
        return new Result(msg("Entry Comments"), sb, Result.TYPE_HTML);
    }

    /** _more_ */
    public static final String MSG_PREFIX = "<msg ";

    /** _more_ */
    public static final String MSG_SUFFIX = " msg>";

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

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processCommentsEdit(Request request) throws Exception {
        Entry entry = getEntry(request);
        SqlUtil.delete(getConnection(), TABLE_COMMENTS,
                       Clause.eq(COL_COMMENTS_ID,
                                 request.getUnsafeString(ARG_COMMENT_ID,
                                     BLANK)));
        entry.setComments(null);
        return new Result(request.url(URL_COMMENTS_SHOW, ARG_ID,
                                      entry.getId(), ARG_MESSAGE,
                                      "Comment deleted"));
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
    public Result processCommentsAdd(Request request) throws Exception {
        Entry        entry = getEntry(request);
        StringBuffer sb    = new StringBuffer();

        if (request.exists(ARG_MESSAGE)) {
            sb.append(note(request.getUnsafeString(ARG_MESSAGE, BLANK)));
        }


        if (request.exists(ARG_CANCEL)) {
            return new Result(request.url(URL_COMMENTS_SHOW, ARG_ID,
                                          entry.getId()));
        }

        String subject = BLANK;
        String comment = BLANK;
        subject = request.getString(ARG_SUBJECT, BLANK).trim();
        comment = request.getString(ARG_COMMENT, BLANK).trim();
        if (comment.length() == 0) {
            sb.append(warning(msg("Please enter a comment")));
        } else {
            PreparedStatement insert =
                getConnection().prepareStatement(INSERT_COMMENTS);
            int col = 1;
            insert.setString(col++, getGUID());
            insert.setString(col++, entry.getId());
            insert.setString(col++, request.getUser().getId());
            insert.setTimestamp(col++, new java.sql.Timestamp(currentTime()),
                                calendar);
            insert.setString(col++, subject);
            insert.setString(col++, request.getString(ARG_COMMENT, BLANK));
            insert.execute();
            insert.close();
            entry.setComments(null);
            return new Result(request.url(URL_COMMENTS_SHOW, ARG_ID,
                                          entry.getId(), ARG_MESSAGE,
                                          "Comment added"));
        }

        sb.append(msgLabel("Add comment for") + getEntryLink(request, entry));
        sb.append(request.form(URL_COMMENTS_ADD, BLANK));
        sb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
        sb.append(HtmlUtil.formTable());
        sb.append(HtmlUtil.formEntry(msgLabel("Subject"),
                                     HtmlUtil.input(ARG_SUBJECT, subject,
                                         HtmlUtil.SIZE_40)));
        sb.append(HtmlUtil.formEntryTop(msgLabel("Comment"),
                                        HtmlUtil.textArea(ARG_COMMENT,
                                            comment, 5, 40)));
        sb.append(
            HtmlUtil.formEntry(
                BLANK,
                buttons(
                    HtmlUtil.submit(msg("Add Comment")),
                    HtmlUtil.submit(msg("Cancel"), ARG_CANCEL))));
        sb.append(HtmlUtil.formTableClose());
        sb.append(HtmlUtil.formClose());
        return new Result(msg("Entry Comments"), sb, Result.TYPE_HTML);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param fromEntry _more_
     * @param toEntry _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String addAssociation(Request request, Entry fromEntry,
                                  Entry toEntry, String name)
            throws Exception {
        if ( !getAccessManager().canDoAction(request, fromEntry,
                                             Permission.ACTION_NEW)) {
            throw new IllegalArgumentException("Cannot add association to "
                    + fromEntry);
        }
        if ( !getAccessManager().canDoAction(request, toEntry,
                                             Permission.ACTION_NEW)) {
            throw new IllegalArgumentException("Cannot add association to "
                    + toEntry);
        }


        PreparedStatement assocInsert =
            getConnection().prepareStatement(INSERT_ASSOCIATIONS);
        int    col = 1;
        String id  = getGUID();
        assocInsert.setString(col++, id);
        assocInsert.setString(col++, name);
        assocInsert.setString(col++, "");
        assocInsert.setString(col++, fromEntry.getId());
        assocInsert.setString(col++, toEntry.getId());
        assocInsert.execute();
        assocInsert.close();
        fromEntry.setAssociations(null);
        toEntry.setAssociations(null);
        return id;
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
        Entry fromEntry = getEntry(request,
                                   request.getString(ARG_FROM, BLANK));
        Entry toEntry = getEntry(request, request.getString(ARG_TO, BLANK));
        if (fromEntry == null) {
            throw new IllegalArgumentException("Could not find entry:"
                    + request.getString(ARG_FROM, BLANK));
        }
        if (toEntry == null) {
            throw new IllegalArgumentException("Could not find entry:"
                    + request.getString(ARG_TO, BLANK));
        }
        String name = request.getString(ARG_NAME, (String) null);
        if (name != null) {
            addAssociation(request, fromEntry, toEntry, name);
            return new Result(request.entryUrl(URL_ENTRY_SHOW, fromEntry));
        }

        StringBuffer sb = new StringBuffer();
        sb.append(header("Add assocation"));
        sb.append("Add association between " + fromEntry.getLabel());
        sb.append(" and  " + toEntry.getLabel());
        sb.append(request.form(URL_ASSOCIATION_ADD, BLANK));
        sb.append(HtmlUtil.br());
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
    public Result processAssociationDelete(Request request) throws Exception {
        String associationId = request.getString(ARG_ASSOCIATION, "");
        Clause clause = Clause.eq(COL_ASSOCIATIONS_ID, associationId);
        List<Association> associations = getAssociations(request, clause);
        if (associations.size() == 0) {
            return new Result(
                msg("Delete Associations"),
                new StringBuffer(error("Could not find assocation")));
        }

        Entry fromEntry = getEntry(request, associations.get(0).getFromId());
        Entry toEntry   = getEntry(request, associations.get(0).getToId());

        if (request.exists(ARG_CANCEL)) {
            return new Result(request.entryUrl(URL_ENTRY_SHOW, fromEntry));
        }


        if (request.exists(ARG_DELETE_CONFIRM)) {
            SqlUtil.delete(getConnection(), TABLE_ASSOCIATIONS, clause);
            fromEntry.setAssociations(null);
            toEntry.setAssociations(null);
            return new Result(request.entryUrl(URL_ENTRY_SHOW, fromEntry));
        }
        StringBuffer sb = new StringBuffer();
        String form = makeOkCancelForm(request, URL_ASSOCIATION_DELETE,
                                       ARG_DELETE_CONFIRM,
                                       HtmlUtil.hidden(ARG_ASSOCIATION,
                                           associationId));
        sb.append(
            question(
                msg("Are you sure you want to delete the assocation?"),
                form));

        sb.append(associations.get(0).getName());
        sb.append(HtmlUtil.br());
        sb.append(fromEntry.getLabel());
        sb.append(HtmlUtil.pad(HtmlUtil.img(fileUrl(ICON_ARROW))));
        sb.append(toEntry.getLabel());
        return new Result(msg("Delete Associations"), sb);
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @return _more_
     */
    private Result asynchDeleteEntries(Request request,
                                       final List<Entry> entries) {
        final Request theRequest = request;
        Entry         entry      = entries.get(0);
        if (request.getCollectionEntry() != null) {
            if (Misc.equals(entry.getId(),
                            request.getCollectionEntry().getId())) {
                request.setCollectionEntry(null);
            }
        }
        Entry                group   = entries.get(0).getParentGroup();
        final String         groupId = entries.get(0).getParentGroupId();

        ActionManager.Action action  = new ActionManager.Action() {
            public void run(Object actionId) throws Exception {
                deleteEntries(theRequest, entries, actionId);
            }
        };
        String href = HtmlUtil.href(request.entryUrl(URL_ENTRY_SHOW, group),
                                    "Continue");
        return getActionManager().doAction(request, action, "Deleting entry",
                                           "Continue: " + href);
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
        Entry        entry = getEntry(request);
        StringBuffer sb    = new StringBuffer();
        sb.append(makeEntryHeader(request, entry));
        if (entry.isTopGroup()) {
            sb.append(note("Cannot delete top-level group"));
            return makeEntryEditResult(request, entry, "Delete Entry", sb);
        }

        if (request.exists(ARG_CANCEL)) {
            return new Result(request.entryUrl(URL_ENTRY_FORM, entry));
        }


        if (request.exists(ARG_DELETE_CONFIRM)) {
            List<Entry> entries = new ArrayList<Entry>();
            entries.add(entry);
            Group group = findGroup(request, entry.getParentGroupId());
            if (entry.isGroup()) {
                return asynchDeleteEntries(request, entries);
            } else {
                deleteEntries(request, entries, null);
                return new Result(request.entryUrl(URL_ENTRY_SHOW, group));
            }
        }



        StringBuffer inner = new StringBuffer();
        if (entry.isGroup()) {
            inner.append(
                msgLabel("Are you sure you want to delete the group"));
            inner.append(entry.getLabel());
            inner.append(HtmlUtil.p());
            inner.append(
                msg(
                "Note: This will also delete all of the descendents of the group"));
        } else {
            inner.append(
                msgLabel("Are you sure you want to delete the entry"));
            inner.append(entry.getLabel());
        }

        StringBuffer fb = new StringBuffer();
        fb.append(request.form(URL_ENTRY_DELETE, BLANK));
        fb.append(buttons(HtmlUtil.submit(msg("OK"), ARG_DELETE_CONFIRM),
                          HtmlUtil.submit(msg("Cancel"), ARG_CANCEL)));
        fb.append(HtmlUtil.hidden(ARG_ID, entry.getId()));
        fb.append(HtmlUtil.formClose());
        sb.append(question(inner.toString(), fb.toString()));
        return makeEntryEditResult(request, entry,
                                   msg("Entry delete confirm"), sb);
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
    public String makeEntryHeader(Request request, Entry entry)
            throws Exception {
        String crumbs = getBreadCrumbs(request, entry, false, BLANK)[1];
        return crumbs;
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
    public Result processEntryChange(final Request request) throws Exception {
        boolean download = request.get(ARG_RESOURCE_DOWNLOAD, false);
        if (download) {
            ActionManager.Action action = new ActionManager.Action() {
                public void run(Object actionId) throws Exception {
                    Result result = doProcessEntryChange(request, actionId);
                    getActionManager().setContinueHtml(actionId,
                            HtmlUtil.href(result.getRedirectUrl(),
                                          msg("Continue")));
                }
            };
            return getActionManager().doAction(request, action,
                    "Downloading file", "");

        }
        return doProcessEntryChange(request, null);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param actionId _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result doProcessEntryChange(Request request, Object actionId)
            throws Exception {

        boolean     download    = request.get(ARG_RESOURCE_DOWNLOAD, false);

        Entry       entry       = null;
        TypeHandler typeHandler = null;
        boolean     newEntry    = true;
        if (request.defined(ARG_ID)) {
            entry = getEntry(request);
            if (entry.getIsLocalFile()) {
                return new Result(request.entryUrl(URL_ENTRY_SHOW, entry,
                        ARG_MESSAGE, "Cannot edit local files"));

            }
            typeHandler = entry.getTypeHandler();
            newEntry    = false;


            if (request.exists(ARG_CANCEL)) {
                return new Result(request.entryUrl(URL_ENTRY_FORM, entry));
            }


            if (request.exists(ARG_DELETE_CONFIRM)) {
                if (entry.isTopGroup()) {
                    return new Result(request.entryUrl(URL_ENTRY_SHOW, entry,
                            ARG_MESSAGE, "Cannot delete top-level group"));
                }

                List<Entry> entries = new ArrayList<Entry>();
                entries.add(entry);
                deleteEntries(request, entries, null);
                Group group = findGroup(request, entry.getParentGroupId());
                return new Result(request.entryUrl(URL_ENTRY_SHOW, group,
                        ARG_MESSAGE, "Entry is deleted"));
            }


            if (request.exists(ARG_DELETE)) {
                return new Result(request.entryUrl(URL_ENTRY_DELETE, entry));
            }
        } else {
            typeHandler = getTypeHandler(request.getString(ARG_TYPE,
                    TypeHandler.TYPE_ANY));

        }


        List<Entry> entries = new ArrayList<Entry>();

        //Synchronize  in case we need to create a group
        //There is a possible case where we can get two groups with the same id
        Object mutex = new Object();
        if (typeHandler.isType(TypeHandler.TYPE_GROUP)) {
            mutex = MUTEX_GROUP;
        }
        String dataType = "";
        if (request.defined(ARG_DATATYPE)) {
            dataType = request.getString(ARG_DATATYPE, "");
        } else {
            dataType = request.getString(ARG_DATATYPE_SELECT, "");
        }
        synchronized (mutex) {
            if (entry == null) {
                List<String> resources    = new ArrayList();
                List<String> origNames    = new ArrayList();
                String       resource = request.getString(ARG_RESOURCE,
                                            BLANK);
                String       filename     = request.getUploadedFile(ARG_FILE);
                boolean      unzipArchive = false;
                boolean      isFile       = false;
                String       resourceName = request.getString(ARG_FILE,
                                                BLANK);
                if (resourceName.length() == 0) {
                    resourceName = IOUtil.getFileTail(resource);
                }

                String groupId = request.getString(ARG_GROUP, (String) null);
                if (groupId == null) {
                    throw new IllegalArgumentException(
                        "You must specify a parent group");
                }
                Group parentGroup = findGroup(request);
                if (filename != null) {
                    isFile       = true;
                    unzipArchive = request.get(ARG_FILE_UNZIP, false);
                    resource     = filename;
                } else if (download) {
                    String url = resource;
                    if ( !url.startsWith("http:")
                            && !url.startsWith("https:")
                            && !url.startsWith("ftp:")) {
                        throw new IllegalArgumentException(
                            "Cannot download url:" + url);
                    }
                    isFile = true;
                    String tail = IOUtil.getFileTail(resource);
                    File newFile = getStorageManager().getTmpFile(request,
                                       tail);
                    checkFilePath(newFile.toString());
                    resourceName = tail;
                    resource     = newFile.toString();
                    URL           fromUrl    = new URL(url);
                    URLConnection connection = fromUrl.openConnection();
                    InputStream   fromStream = connection.getInputStream();
                    //                Object startLoad(String name) {
                    if (actionId != null) {
                        JobManager.getManager().startLoad("File copy",
                                actionId);
                    }
                    int length = connection.getContentLength();
                    if (length > 0 & actionId != null) {
                        getActionManager().setActionMessage(actionId,
                                msg("Downloading") + " " + length + " "
                                + msg("bytes"));
                    }
                    FileOutputStream toStream = new FileOutputStream(newFile);
                    try {
                        if (IOUtil.writeTo(fromStream, toStream, actionId,
                                           length) < 0) {
                            return new Result(
                                request.entryUrl(
                                    URL_ENTRY_SHOW, parentGroup));
                        }
                    } finally {
                        try {
                            toStream.close();
                            fromStream.close();
                        } catch (Exception exc) {}
                    }
                }

                if ( !unzipArchive) {
                    resources.add(resource);
                    origNames.add(resourceName);
                } else {
                    ZipInputStream zin =
                        new ZipInputStream(new FileInputStream(resource));
                    ZipEntry ze = null;
                    while ((ze = zin.getNextEntry()) != null) {
                        if (ze.isDirectory()) {
                            continue;
                        }
                        String name =
                            IOUtil.getFileTail(ze.getName().toLowerCase());
                        File f = getStorageManager().getTmpFile(request,
                                     name);
                        checkFilePath(f.toString());
                        FileOutputStream fos = new FileOutputStream(f);

                        IOUtil.writeTo(zin, fos);
                        fos.close();
                        resources.add(f.toString());
                        origNames.add(name);
                    }
                }

                if (request.exists(ARG_CANCEL)) {
                    return new Result(request.entryUrl(URL_ENTRY_SHOW,
                            parentGroup));
                }


                String description = request.getString(ARG_DESCRIPTION,
                                         BLANK);

                Date createDate = new Date();
                Date[] dateRange = request.getDateRange(ARG_FROMDATE,
                                       ARG_TODATE, createDate);
                if (dateRange[0] == null) {
                    dateRange[0] = ((dateRange[1] == null)
                                    ? createDate
                                    : dateRange[1]);
                }
                if (dateRange[1] == null) {
                    dateRange[1] = dateRange[0];
                }


                for (int resourceIdx = 0; resourceIdx < resources.size();
                        resourceIdx++) {
                    String theResource = (String) resources.get(resourceIdx);
                    String origName    = (String) origNames.get(resourceIdx);
                    if (isFile) {
                        theResource =
                            getStorageManager().moveToStorage(request,
                                new File(theResource)).toString();
                    }
                    String name = request.getString(ARG_NAME, BLANK);
                    if (name.indexOf("${") >= 0) {}

                    if (name.trim().length() == 0) {
                        name = IOUtil.getFileTail(origName);
                    }
                    if (name.trim().length() == 0) {
                        //                        throw new IllegalArgumentException(
                        //                            "You must specify a name");
                    }

                    if (typeHandler.isType(TypeHandler.TYPE_GROUP)) {
                        if (name.indexOf("/") >= 0) {
                            throw new IllegalArgumentException(
                                "Cannot have a '/' in group name: '" + name
                                + "'");
                        }
                        Entry existing = findEntryWithName(request,
                                             parentGroup, name);
                        if ((existing != null) && existing.isGroup()) {
                            throw new IllegalArgumentException(
                                "A group with the given name already exists");

                        }
                    }

                    Date[] theDateRange = { dateRange[0], dateRange[1] };

                    if (request.defined(ARG_DATE_PATTERN)) {
                        String format =
                            request.getUnsafeString(ARG_DATE_PATTERN, BLANK);
                        String pattern = null;
                        for (int i = 0; i < DateUtil.DATE_PATTERNS.length;
                                i++) {
                            if (format.equals(DateUtil.DATE_FORMATS[i])) {
                                pattern = DateUtil.DATE_PATTERNS[i];
                                break;
                            }
                        }
                        //                    System.err.println("format:" + format);
                        //                    System.err.println("orignName:" + origName);
                        //                    System.err.println("pattern:" + pattern);

                        if (pattern != null) {
                            Pattern datePattern = Pattern.compile(pattern);
                            Matcher matcher = datePattern.matcher(origName);
                            if (matcher.find()) {
                                String dateString = matcher.group(0);
                                Date dttm =
                                    makeDateFormat(format).parse(dateString);
                                theDateRange[0] = dttm;
                                theDateRange[1] = dttm;
                                //                            System.err.println("got it");
                            } else {
                                //                            System.err.println("not found");
                            }
                        }
                    }

                    String id = (typeHandler.isType(TypeHandler.TYPE_GROUP)
                                 ? getGroupId(parentGroup)
                                 : getGUID());

                    String resourceType = Resource.TYPE_UNKNOWN;
                    if (isFile) {
                        resourceType = Resource.TYPE_STOREDFILE;
                    } else {
                        try {
                            new URL(theResource);
                            resourceType = Resource.TYPE_URL;
                        } catch (Exception exc) {}

                    }

                    entry = typeHandler.createEntry(id);
                    entry.initEntry(name, description, parentGroup, "",
                                    request.getUser(),
                                    new Resource(theResource, resourceType),
                                    dataType, createDate.getTime(),
                                    theDateRange[0].getTime(),
                                    theDateRange[1].getTime(), null);
                    setEntryState(request, entry);
                    entries.add(entry);
                }
            } else {
                if (entry.isTopGroup()) {
                    //                    throw new IllegalArgumentException(
                    //                        "Cannot edit top-level group");
                }
                Date[] dateRange = request.getDateRange(ARG_FROMDATE,
                                       ARG_TODATE, new Date());
                String newName = request.getString(ARG_NAME,
                                     entry.getLabel());


                if (entry.isGroup()) {
                    if (newName.indexOf(Group.IDDELIMITER) >= 0) {
                        throw new IllegalArgumentException(
                            "Cannot have a '/' in group name:" + newName);
                    }

                    /**
                     * TODO Do we want to not allow 2 or more groups with the same name?
                     * Entry existing = findEntryWithName(request,
                     *                                  entry.getParentGroup(), newName);
                     * if ((existing != null) && existing.isGroup()
                     *       && !existing.getId().equals(entry.getId())) {
                     *   throw new IllegalArgumentException(
                     *       "A group with the given name already exists");
                     * }
                     */
                }

                entry.setName(newName);
                entry.setDescription(request.getString(ARG_DESCRIPTION,
                        entry.getDescription()));
                entry.setDataType(dataType);
                if (request.defined(ARG_RESOURCE)) {
                    entry.setResource(
                        new Resource(request.getString(ARG_RESOURCE, BLANK)));
                }

                //                System.err.println("dateRange:" + dateRange[0] + " " + dateRange[1]);

                if (dateRange[0] != null) {
                    entry.setStartDate(dateRange[0].getTime());
                }
                if (dateRange[1] == null) {
                    dateRange[1] = dateRange[0];
                }

                if (dateRange[1] != null) {
                    entry.setEndDate(dateRange[1].getTime());
                }
                setEntryState(request, entry);
                entries.add(entry);
            }

            if (newEntry && request.get(ARG_ADDMETADATA, false)) {
                addInitialMetadata(request, entries);
            }
            insertEntries(entries, newEntry);
        }
        if (entries.size() == 1) {
            entry = (Entry) entries.get(0);
            return new Result(request.entryUrl(URL_ENTRY_SHOW, entry));
        } else if (entries.size() > 1) {
            entry = (Entry) entries.get(0);
            return new Result(request.entryUrl(URL_ENTRY_SHOW,
                    entry.getParentGroup(), ARG_MESSAGE,
                    entries.size() + HtmlUtil.pad(msg("files uploaded"))));
        } else {
            return new Result(BLANK,
                              new StringBuffer(msg("No entries created")));
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     *
     * @throws Exception _more_
     */
    public void addInitialMetadata(Request request, List<Entry> entries)
            throws Exception {
        for (Entry theEntry : entries) {
            Hashtable extra = new Hashtable();
            getMetadataManager().getMetadata(theEntry);
            getMetadataManager().addInitialMetadata(request, theEntry, extra);
            if ( !theEntry.hasAreaDefined()
                    && (extra.get(ARG_MINLAT) != null)) {
                theEntry.setSouth(Misc.getProperty(extra, ARG_MINLAT, 0.0));
                theEntry.setNorth(Misc.getProperty(extra, ARG_MAXLAT, 0.0));
                theEntry.setWest(Misc.getProperty(extra, ARG_MINLON, 0.0));
                theEntry.setEast(Misc.getProperty(extra, ARG_MAXLON, 0.0));
            }
            if ((extra.get(ARG_FROMDATE) != null)
                    && (theEntry.getStartDate()
                        == theEntry.getCreateDate())) {
                //                System.err.println ("got dttm:" + extra.get(ARG_FROMDATE));
                theEntry.setStartDate(
                    ((Date) extra.get(ARG_FROMDATE)).getTime());
                theEntry.setEndDate(((Date) extra.get(ARG_TODATE)).getTime());
            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void setEntryState(Request request, Entry entry)
            throws Exception {
        entry.setSouth(request.get(ARG_AREA + "_south", entry.getSouth()));
        entry.setNorth(request.get(ARG_AREA + "_north", entry.getNorth()));
        entry.setWest(request.get(ARG_AREA + "_west", entry.getWest()));
        entry.setEast(request.get(ARG_AREA + "_east", entry.getEast()));


        entry.getTypeHandler().initializeEntry(request, entry);
    }



    /** _more_ */
    List<Group> topGroups;

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Group> getTopGroups(Request request) throws Exception {
        if (topGroups != null) {
            return topGroups;
        }
        //        System.err.println("ramadda: getTopGroups " + topGroup);

        Statement statement = getDatabaseManager().select(COL_ENTRIES_ID,
                                  TABLE_ENTRIES,
                                  Clause.eq(COL_ENTRIES_PARENT_GROUP_ID,
                                            topGroup.getId()));
        String[]    ids    = SqlUtil.readString(statement, 1);
        List<Group> groups = new ArrayList<Group>();
        for (int i = 0; i < ids.length; i++) {
            //Get the entry but don't check for access control
            Entry e = getEntry(request, ids[i], false);
            if (e == null) {
                continue;
            }
            if ( !e.isGroup()) {
                continue;
            }
            Group g = (Group) e;
            groups.add(g);
        }
        //For now don't check for access control
        //        return topGroups = new ArrayList<Group>(
        //            toGroupList(getAccessManager().filterEntries(request, groups)));
        return topGroups = new ArrayList<Group>(groups);
    }

    /**
     * _more_
     *
     * @param entries _more_
     *
     * @return _more_
     */
    private List<Group> toGroupList(List<Entry> entries) {
        List<Group> groups = new ArrayList<Group>();
        for (Entry entry : entries) {
            groups.add((Group) entry);
        }
        return groups;
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
        Entry entry;
        if (request.defined(ARG_ID)) {
            entry = getEntry(request);
            if (entry == null) {
                Entry tmp = getEntry(request,
                                     request.getString(ARG_ID, BLANK), false);
                if (tmp != null) {
                    throw new IllegalArgumentException(
                        "You do not have access to this entry");
                }
            }
        } else if (request.defined(ARG_GROUP)) {
            entry = findGroup(request);
        } else {
            entry = topGroup;
        }
        if (entry == null) {
            throw new IllegalArgumentException("No entry specified");
        }

        //System.err.println (request);
        if (request.get(ARG_NEXT, false)
                || request.get(ARG_PREVIOUS, false)) {
            boolean next = request.get(ARG_NEXT, false);
            List<String> ids =
                getEntryIdsInGroup(
                    request, findGroup(request, entry.getParentGroupId()),
                    new ArrayList<Clause>());
            String nextId = null;
            for (int i = 0; (i < ids.size()) && (nextId == null); i++) {
                String id = ids.get(i);
                if (id.equals(entry.getId())) {
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
                return new Result(request.url(URL_ENTRY_SHOW, ARG_ID, nextId,
                        ARG_OUTPUT,
                        request.getString(ARG_OUTPUT,
                                          OutputHandler.OUTPUT_HTML)));
            }
        }

        if (entry.isGroup()) {
            return processGroupShow(request, (Group) entry);
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
            Entry entry = getEntry(request, id);
            if (entry != null) {
                entries.add(entry);
            }
        }
        String ids = request.getIds((String) null);
        if (ids != null) {
            List<String> idList = StringUtil.split(ids, ",", true, true);
            for (String id : idList) {
                Entry entry = getEntry(request, id);
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }
        entries = getAccessManager().filterEntries(request, entries);

        return getOutputHandler(request).outputGroup(request,
                                getDummyGroup(), new ArrayList<Group>(),
                                entries);

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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Group findGroup(Request request) throws Exception {
        String groupNameOrId = (String) request.getString(ARG_GROUP,
                                   (String) null);
        if (groupNameOrId == null) {
            throw new IllegalArgumentException("No group specified");
        }
        Entry entry = getEntry(request, groupNameOrId, false);
        if (entry != null) {
            if ( !entry.isGroup()) {
                throw new IllegalArgumentException("Not a group:"
                        + groupNameOrId);
            }
            return (Group) entry;
        }
        throw new IllegalArgumentException("Could not find group:"
                                           + groupNameOrId);
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param makeLinkForLastGroup _more_
     * @param extraArgs _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String[] getBreadCrumbs(Request request, Entry entry,
                                   boolean makeLinkForLastGroup,
                                   String extraArgs)
            throws Exception {
        return getBreadCrumbs(request, entry, makeLinkForLastGroup,
                              extraArgs, null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param makeLinkForLastGroup _more_
     * @param extraArgs _more_
     * @param stopAt _more_
     *
     * @return A 2 element array.  First element is the title to use. Second is the links
     *
     * @throws Exception _more_
     */
    public String[] getBreadCrumbs(Request request, Entry entry,
                                   boolean makeLinkForLastGroup,
                                   String extraArgs, Group stopAt)
            throws Exception {
        if (request == null) {
            request = new Request(this, "", new Hashtable());
        }

        List breadcrumbs = new ArrayList();
        List titleList   = new ArrayList();
        if (entry == null) {
            return new String[] { BLANK, BLANK };
        }
        Group  parent = findGroup(request, entry.getParentGroupId());
        String output = ((request == null)
                         ? OutputHandler.OUTPUT_HTML
                         : request.getOutput());
        output = OutputHandler.OUTPUT_HTML;
        int length = 0;
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
            String link;
            if (request != null) {
                link = HtmlUtil.href(request.entryUrl(URL_ENTRY_SHOW, parent,
                        ARG_OUTPUT, output) + extraArgs, name);
            } else {
                link = HtmlUtil.href(HtmlUtil.url(URL_ENTRY_SHOW.toString(),
                        ARG_OUTPUT, output) + extraArgs, name);
            }
            breadcrumbs.add(0, link);
            parent = findGroup(request, parent.getParentGroupId());
        }
        titleList.add(entry.getLabel());
        String nav;

        String separator = HtmlUtil.span(HtmlUtil.pad("&gt;"),
                                         "class=separator");

        String entryLink = HtmlUtil.href(request.entryUrl(URL_ENTRY_SHOW,
                               entry, ARG_OUTPUT, output), entry.getLabel());
        if (makeLinkForLastGroup) {
            breadcrumbs.add(entryLink);
            nav = StringUtil.join(separator, breadcrumbs);
            nav = HtmlUtil.div(nav, HtmlUtil.cssClass("breadcrumbs"));
        } else {
            nav = StringUtil.join(separator, breadcrumbs);
            StringBuffer menu = new StringBuffer();
            menu.append(
                HtmlUtil.div(
                    getEntryLinksList(request, entry),
                    HtmlUtil.id("entrylinksmenu" + entry.getId())
                    + HtmlUtil.cssClass("menu")));
            String compId = "menubutton" + entry.getId();
            String events = HtmlUtil.onMouseOver(
                                "setImage(" + HtmlUtil.squote(compId) + ",'"
                                + fileUrl(ICON_GRAYRECTARROW)
                                + "')") + HtmlUtil.onMouseOut(
                                    "setImage(" + HtmlUtil.squote(compId)
                                    + ",'" + fileUrl(ICON_GRAYRECT)
                                    + "')") + HtmlUtil.onMouseClick(
                                        "showMenu(event, "
                                        + HtmlUtil.squote(compId) + ", "
                                        + HtmlUtil.squote(
                                            "entrylinksmenu"
                                            + entry.getId()) + ")");
            String menuLink = HtmlUtil.space(1)
                              + HtmlUtil.jsLink(events,
                                  HtmlUtil.img(fileUrl(ICON_GRAYRECT),
                                      msg("Show menu"), HtmlUtil.id(compId)));

            //            String linkHtml = "";
            String linkHtml = getEntryLinksHtml(request, entry, true);
            String header =
                "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
                + HtmlUtil.rowBottom("<td class=\"entryname\" >" + entryLink
                                     + menuLink + "</td><td align=\"right\">"
                                     + linkHtml + "</td>") + "</table>";
            nav = HtmlUtil.div(
                HtmlUtil.div(nav, HtmlUtil.cssClass("breadcrumbs")) + header,
                HtmlUtil.cssClass("entryheader")) + menu;

        }
        String title = StringUtil.join(HtmlUtil.pad("&gt;"), titleList);
        return new String[] { title, nav };
    }







    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return A 2 element array.  First element is the title to use. Second is the links
     *
     * @throws Exception _more_
     */
    public String getBreadCrumbs(Request request, Entry entry)
            throws Exception {
        List breadcrumbs = new ArrayList();
        if (entry == null) {
            return BLANK;
        }
        Group parent = findGroup(request, entry.getParentGroupId());
        int   length = 0;
        while (parent != null) {
            if (length > 100) {
                breadcrumbs.add(0, "...");
                break;
            }
            String name = parent.getName();
            if (name.length() > 20) {
                name = name.substring(0, 19) + "...";
            }
            length += name.length();
            breadcrumbs.add(0, HtmlUtil.href(request.entryUrl(URL_ENTRY_SHOW,
                    parent), name));
            parent = findGroup(request, parent.getParentGroupId());
        }
        breadcrumbs.add(HtmlUtil.href(request.entryUrl(URL_ENTRY_SHOW,
                entry), entry.getLabel()));
        return StringUtil.join(HtmlUtil.pad("&gt;"), breadcrumbs);
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
        boolean       doLatest      = request.get(ARG_LATEST, false);


        OutputHandler outputHandler = getOutputHandler(request);


        TypeHandler   typeHandler   = getTypeHandler(request);
        List<Clause>  where         =
            typeHandler.assembleWhereClause(request);

        List<Entry>   entries       = new ArrayList<Entry>();
        List<Group>   subGroups     = new ArrayList<Group>();
        try {
            List<String> ids = getEntryIdsInGroup(request, group, where);
            for (String id : ids) {
                Entry entry = getEntry(request, id);

                if (entry == null) {
                    continue;
                }
                if (entry.isGroup()) {
                    subGroups.add((Group) entry);
                } else {
                    entries.add(entry);
                }
            }
        } catch (Exception exc) {
            request.put(ARG_MESSAGE,
                        "Error finding children:" + exc.getMessage());
        }

        if (doLatest) {
            if (entries.size() > 0) {
                entries = sortEntriesOnDate(entries, true);
                return outputHandler.outputEntry(request, entries.get(0));
            }
        }


        return outputHandler.outputGroup(request, group, subGroups, entries);
    }



    /**
     * _more_
     *
     * @param entries _more_
     * @param descending _more_
     *
     * @return _more_
     */
    protected List<Entry> sortEntriesOnDate(List<Entry> entries,
                                            final boolean descending) {
        Comparator comp = new Comparator() {
            public int compare(Object o1, Object o2) {
                Entry e1 = (Entry) o1;
                Entry e2 = (Entry) o2;
                if (e1.getStartDate() < e2.getStartDate()) {
                    return (descending
                            ? 1
                            : -1);
                }
                if (e1.getStartDate() > e2.getStartDate()) {
                    return (descending
                            ? -1
                            : 1);
                }
                return 0;
            }
            public boolean equals(Object obj) {
                return obj == this;
            }
        };
        Object[] array = entries.toArray();
        Arrays.sort(array, comp);
        return (List<Entry>) Misc.toList(array);
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
            List<Clause> where)
            throws Exception {
        List<String> ids = new ArrayList<String>();
        if (isLocalFileEntry(group.getId())) {
            Metadata patternMetadata =
                getMetadataManager().findMetadata(group,
                    AdminMetadataHandler.TYPE_LOCALFILE_PATTERN, true);
            PatternFileFilter filter = null;
            if ((patternMetadata != null)
                    && (patternMetadata.getAttr1().trim().length() > 0)) {
                try {
                    filter =
                        new PatternFileFilter(patternMetadata.getAttr1());
                } catch (Exception exc) {
                    throw new IllegalStateException(
                        "Bad local files pattern:"
                        + patternMetadata.getAttr1());
                }
            }
            File   f     = new File(getFileFromId(group.getId()));
            File[] files = f.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isHidden() || files[i].isDirectory()) {}
                else {
                    if ((filter != null) && !filter.accept(files[i])) {
                        continue;
                    }
                }
                ids.add(getIdFromFile(files[i]));
            }
            ids = Misc.sort(ids);
            return ids;
        }


        where = new ArrayList<Clause>(where);
        where.add(Clause.eq(COL_ENTRIES_PARENT_GROUP_ID, group.getId()));
        TypeHandler typeHandler = getTypeHandler(request);
        int         skipCnt     = request.get(ARG_SKIP, 0);
        Statement statement = typeHandler.select(request, COL_ENTRIES_ID,
                                  where,
                                  getQueryOrderAndLimit(request, true));
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
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

        //Add in any local file directories
        if (topGroup.equals(group)) {
            for (String path : localFilePaths) {
                ids.add(getIdFromFile(path));
            }
        }

        group.addChildrenIds(ids);


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
     * @param id _more_
     * @param sb _more_
     *
     * @throws Exception _more_
     */
    protected void getAssociationsGraph(Request request, String id,
                                        StringBuffer sb)
            throws Exception {
        List<Association> associations = getAssociations(request, id);
        for (Association association : associations) {
            Entry   other  = null;
            boolean isTail = true;
            if (association.getFromId().equals(id)) {
                other  = getEntry(request, association.getToId());
                isTail = true;
            } else {
                other  = getEntry(request, association.getFromId());
                isTail = false;
            }

            if (other != null) {
                sb.append(XmlUtil.tag(TAG_NODE,
                                      XmlUtil.attrs(ATTR_TYPE,
                                          (other.isGroup()
                                           ? NODETYPE_GROUP
                                           : other.getTypeHandler()
                                           .getNodeType()), ATTR_ID,
                                               other.getId(), ATTR_TITLE,
                                                   other.getName())));
                sb.append(XmlUtil.tag(TAG_EDGE,
                                      XmlUtil.attrs(ATTR_TYPE, "association",
                                          ATTR_FROM, (isTail
                        ? id
                        : other.getId()), ATTR_TO, (isTail
                        ? other.getId()
                        : id))));
            }
        }


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
        if ( !type.equals(TYPE_GROUP)) {
            Statement stmt = typeHandler.select(
                                 request,
                                 SqlUtil.comma(
                                     COL_ENTRIES_ID, COL_ENTRIES_NAME,
                                     COL_ENTRIES_TYPE,
                                     COL_ENTRIES_PARENT_GROUP_ID,
                                     COL_ENTRIES_RESOURCE), Clause.eq(
                                         COL_ENTRIES_ID, id), "");

            ResultSet results = stmt.getResultSet();
            if ( !results.next()) {
                throw new IllegalArgumentException("Unknown entry id:" + id);
            }
            sb.append(getEntryNodeXml(request, results));
            getAssociationsGraph(request, id, sb);

            Group group = findGroup(request, results.getString(4));
            sb.append(XmlUtil.tag(TAG_NODE,
                                  XmlUtil.attrs(ATTR_TYPE, NODETYPE_GROUP,
                                      ATTR_ID, group.getId(), ATTR_TOOLTIP,
                                      group.getName(), ATTR_TITLE,
                                      getGraphNodeTitle(group.getName()))));
            sb.append(XmlUtil.tag(TAG_EDGE,
                                  XmlUtil.attrs(ATTR_TYPE, "groupedby",
                                      ATTR_FROM, group.getId(), ATTR_TO,
                                      results.getString(1))));



            String xml = StringUtil.replace(graphXmlTemplate, "${content}",
                                            sb.toString());

            xml = StringUtil.replace(xml, "${root}", getUrlBase());
            //            System.err.println(xml);
            return new Result(BLANK, new StringBuffer(xml),
                              getMimeTypeFromSuffix(".xml"));
        }

        Group group = findGroup(request, id);
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
        getAssociationsGraph(request, id, sb);
        List<Group> subGroups =
            getGroups(request,
                      Clause.eq(COL_ENTRIES_PARENT_GROUP_ID, group.getId()));


        Group parent = findGroup(request, group.getParentGroupId());
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


        Statement stmt =
            getDatabaseManager().select(SqlUtil.comma(COL_ENTRIES_ID,
                COL_ENTRIES_NAME, COL_ENTRIES_TYPE,
                COL_ENTRIES_PARENT_GROUP_ID,
                COL_ENTRIES_RESOURCE), TABLE_ENTRIES,
                                       Clause.eq(COL_ENTRIES_PARENT_GROUP_ID,
                                           group.getId()));
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
        //        System.err.println(xml);
        return new Result(BLANK, new StringBuffer(xml),
                          getMimeTypeFromSuffix(".xml"));

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param s _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getEntryText(Request request, Entry entry, String s)
            throws Exception {
        //<attachment name>
        if (s.indexOf("<attachment") >= 0) {
            List<Association> associations = getAssociations(request, entry);
            for (Association association : associations) {
                if ( !association.getFromId().equals(entry.getId())) {
                    continue;
                }
            }
        }
        return s;
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
        return makeTypeSelect(request, includeAny, "");
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
                                 String selected)
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
            typeHandler.select(request, SqlUtil.distinct(COL_ENTRIES_TYPE),
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
                             SqlUtil.distinct(COL_ENTRIES_DATATYPE),
                             TABLE_ENTRIES, new Clause[] {});
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
     *           typeHandler.addOr(COL_ENTRIES_TYPE, type, where, true);
     *       }
     *   }
     *   if (where.size() > 0) {
     *       where.add(SqlUtil.eq(COL_TAGS_ENTRY_ID, COL_ENTRIES_ID));
     *   }
     *
     *   Statement stmt;
     *   String[] tags =
     *       SqlUtil.readString(stmt =
     *           typeHandler.select(request,
     *                                     SqlUtil.distinct(COL_TAGS_NAME),
     *                                     where,
     *                                     " order by " + COL_TAGS_NAME), 1);
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
     *                             Misc.newList(SqlUtil.eq(COL_TAGS_NAME,
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
     *
     * @throws Exception _more_
     */
    private void initGroups() throws Exception {
        topGroup = findGroupFromName(GROUP_TOP,
                                     getUserManager().getDefaultUser(),
                                     false);
        //Make the top group if needed
        if (topGroup == null) {
            topGroup = findGroupFromName(GROUP_TOP,
                                         getUserManager().getDefaultUser(),
                                         true, true);

            getAccessManager().initTopGroup(topGroup);
        }
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
                groupCache.put(group.getId(), group);
            }
        }
        for (Group group : groups) {
            if (group.getParentGroupId() != null) {
                Group parentGroup =
                    (Group) groupCache.get(group.getParentGroupId());
                group.setParentGroup(parentGroup);
            }
            groupCache.put(group.getFullName(), group);
        }

        if (groupCache.size() > ENTRY_CACHE_LIMIT) {
            groupCache = new Hashtable();
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
        Statement statement = getDatabaseManager().select(column, tableName,
                                  Clause.eq(column, id));

        ResultSet results = statement.getResultSet();
        return results.next();
    }



    /**
     * _more_
     */
    public void setLocalFilePaths() {
        localFilePaths =
            (List<String>) StringUtil.split(getProperty(PROP_LOCALFILEPATHS,
                ""), "\n", true, true);
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public String getFileFromId(String id) {
        return id.substring(ID_PREFIX_LOCAL_FILE.length());
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public String getCatalogFromId(String id) {
        return id.substring(ID_PREFIX_CATALOG.length());
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public String getIdFromFile(File file) {
        return getIdFromFile(file.toString());
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    public String getIdFromFile(String file) {
        file = file.replace("\\", "/");
        return ID_PREFIX_LOCAL_FILE + file;
    }


    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     */
    public String getIdFromCatalog(String url) {
        return ID_PREFIX_CATALOG + url;
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public boolean isGeneratedEntry(String id) {
        return id.startsWith(ID_PREFIX_GENERATED);
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public String[] getGeneratedIdPair(String id) {
        id = id.substring(ID_PREFIX_GENERATED.length());
        return StringUtil.split(id, ";", 2);
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public boolean isLocalFileEntry(String id) {
        if (id.startsWith(ID_PREFIX_LOCAL_FILE)) {
            id = id.substring(ID_PREFIX_LOCAL_FILE.length());
            for (String path : localFilePaths) {
                if (id.startsWith(path)) {
                    checkFilePath(id);
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public boolean isCatalogEntry(String id) {
        if (id.startsWith(ID_PREFIX_CATALOG)) {
            return true;
        }
        return false;
    }


    /**
     * _more_
     *
     *
     * @param request _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    //    protected Group findGroup(String id) throws Exception {


    protected Group findGroup(Request request, String id) throws Exception {
        if ((id == null) || (id.length() == 0)) {
            return null;
        }
        Group group = groupCache.get(id);
        if (group != null) {
            return group;
        }

        if (isLocalFileEntry(id)) {
            return (Group) getEntry(request, id);
        }

        if (isCatalogEntry(id)) {
            return (Group) getEntry(request, id);
        }


        Statement statement = getDatabaseManager().select(COLUMNS_ENTRIES,
                                  TABLE_ENTRIES,
                                  Clause.eq(COL_ENTRIES_ID, id));

        List<Group> groups = readGroups(statement);
        if (groups.size() > 0) {
            return groups.get(0);
        }
        return null;
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
     *
     * @param request _more_
     * @param parent _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry findEntryWithName(Request request, Group parent, String name)
            throws Exception {
        String groupName = ((parent == null)
                            ? ""
                            : parent.getFullName()) + Group.IDDELIMITER
                                + name;
        Group group = groupCache.get(groupName);
        if (group != null) {
            return group;
        }
        String[] ids = SqlUtil.readString(
                           getDatabaseManager().select(
                               COL_ENTRIES_ID, TABLE_ENTRIES,
                               Clause.and(
                                   Clause.eq(
                                       COL_ENTRIES_PARENT_GROUP_ID,
                                       parent.getId()), Clause.eq(
                                           COL_ENTRIES_NAME, name))));
        if (ids.length == 0) {
            return null;
        }
        return getEntry(request, ids[0], false);
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
                    && !name.startsWith(GROUP_TOP + Group.IDDELIMITER)) {
                name = GROUP_TOP + Group.IDDELIMITER + name;
            }
            Group group = groupCache.get(name);
            if (group != null) {
                return group;
            }
            //            System.err.println("Looking for:" + name);

            List<String> toks = (List<String>) StringUtil.split(name,
                                    Group.IDDELIMITER, true, true);
            Group  parent = null;
            String lastName;
            if ((toks.size() == 0) || (toks.size() == 1)) {
                lastName = name;
            } else {
                lastName = toks.get(toks.size() - 1);
                toks.remove(toks.size() - 1);
                parent = findGroupFromName(StringUtil.join(Group.IDDELIMITER,
                        toks), user, createIfNeeded);
                if (parent == null) {
                    if ( !isTop) {
                        return null;
                    }
                    return topGroup;
                }
            }
            List<Clause> clauses = new ArrayList<Clause>();
            clauses.add(Clause.eq(COL_ENTRIES_TYPE, TypeHandler.TYPE_GROUP));
            if (parent != null) {
                clauses.add(Clause.eq(COL_ENTRIES_PARENT_GROUP_ID,
                                      parent.getId()));
            } else {
                clauses.add(Clause.isNull(COL_ENTRIES_PARENT_GROUP_ID));
            }
            clauses.add(Clause.eq(COL_ENTRIES_NAME, lastName));
            Statement statement =
                getDatabaseManager().select(COLUMNS_ENTRIES, TABLE_ENTRIES,
                                            clauses);
            List<Group> groups = readGroups(statement);
            statement.close();
            if (groups.size() > 0) {
                group = groups.get(0);
            } else {
                if ( !createIfNeeded) {
                    return null;
                }
                return makeNewGroup(parent, lastName, user);
            }
            groupCache.put(group.getId(), group);
            groupCache.put(group.getFullName(), group);
            return group;
        }
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param name _more_
     * @param user _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Group makeNewGroup(Group parent, String name, User user)
            throws Exception {
        synchronized (MUTEX_GROUP) {
            TypeHandler typeHandler = getTypeHandler(TypeHandler.TYPE_GROUP);
            Group       group = new Group(getGroupId(parent), typeHandler);
            group.setName(name);
            group.setParentGroup(parent);
            group.setUser(user);
            group.setDate(new Date().getTime());
            addNewEntry(group);
            groupCache.put(group.getId(), group);
            groupCache.put(group.getFullName(), group);
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
        Clause idClause;
        String idWhere;
        if (parent == null) {
            idClause = Clause.isNull(COL_ENTRIES_PARENT_GROUP_ID);
        } else {
            idClause = Clause.eq(COL_ENTRIES_PARENT_GROUP_ID, parent.getId());
        }
        String newId = null;
        while (true) {
            if (parent == null) {
                newId = BLANK + baseId;
            } else {
                newId = parent.getId() + Group.IDDELIMITER + baseId;
            }

            Statement stmt = getDatabaseManager().select(COL_ENTRIES_ID,
                                 TABLE_ENTRIES, new Clause[] { idClause,
                    Clause.eq(COL_ENTRIES_ID, newId) });
            ResultSet idResults = stmt.getResultSet();

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
     * @param request _more_
     * @param addOrderBy _more_
     *
     * @return _more_
     */
    protected String getQueryOrderAndLimit(Request request,
                                           boolean addOrderBy) {
        String order = " DESC ";
        if (request.get(ARG_ASCENDING, false)) {
            order = " ASC ";
        }
        int    skipCnt     = request.get(ARG_SKIP, 0);
        String limitString = BLANK;
        //        if (request.defined(ARG_SKIP)) {
        //            if (skipCnt > 0) {
        int max = request.get(ARG_MAX, MAX_ROWS);
        limitString = getDatabaseManager().getLimitString(skipCnt, max);
        String orderBy = BLANK;
        if (addOrderBy) {
            orderBy = " ORDER BY " + COL_ENTRIES_FROMDATE + order;
        }
        if (request.defined(ARG_ORDERBY)) {
            String by = request.getString(ARG_ORDERBY, BLANK);
            if (by.equals("fromdate")) {
                orderBy = " ORDER BY " + COL_ENTRIES_FROMDATE + order;
            } else if (by.equals("todate")) {
                orderBy = " ORDER BY " + COL_ENTRIES_TODATE + order;
            } else if (by.equals("createdate")) {
                orderBy = " ORDER BY " + COL_ENTRIES_CREATEDATE + order;
            } else if (by.equals("name")) {
                orderBy = " ORDER BY " + COL_ENTRIES_NAME + order;
            }
        }

        return orderBy + limitString;
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
        return getEntries(request, new StringBuffer());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param searchCriteriaSB _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List[] getEntries(Request request,
                                StringBuffer searchCriteriaSB)
            throws Exception {
        TypeHandler typeHandler = getTypeHandler(request);
        List<Clause> where = typeHandler.assembleWhereClause(request,
                                 searchCriteriaSB);
        int skipCnt = request.get(ARG_SKIP, 0);

        Statement statement = typeHandler.select(request, COLUMNS_ENTRIES,
                                  where,
                                  getQueryOrderAndLimit(request, false));


        SqlUtil.debug = false;

        List<Entry>      entries = new ArrayList<Entry>();
        List<Entry>      groups  = new ArrayList<Entry>();
        ResultSet        results;
        SqlUtil.Iterator iter       = SqlUtil.getIterator(statement);
        boolean canDoSelectOffset   =
            getDatabaseManager().canDoSelectOffset();
        Hashtable        seen       = new Hashtable();
        List<Entry>      allEntries = new ArrayList<Entry>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                if ( !canDoSelectOffset && (skipCnt-- > 0)) {
                    continue;
                }
                String id    = results.getString(1);
                Entry  entry = (Entry) entryCache.get(id);
                if (entry == null) {
                    //id,type,name,desc,group,user,file,createdata,fromdate,todate
                    TypeHandler localTypeHandler =
                        getTypeHandler(results.getString(2));
                    entry = localTypeHandler.getEntry(results);
                    entryCache.put(entry.getId(), entry);
                }
                if (seen.get(entry.getId()) != null) {
                    continue;
                }
                seen.put(entry.getId(), BLANK);
                allEntries.add(entry);
            }
        }



        for (Entry entry : allEntries) {
            if (entry.isGroup()) {
                groups.add(entry);
            } else {
                entries.add(entry);
            }
        }

        entries = getAccessManager().filterEntries(request, entries);
        groups  = getAccessManager().filterEntries(request, groups);


        return new List[] { groups, entries };
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
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     */
    protected String getEntryLink(Request request, Entry entry) {
        return getEntryLink(request, entry, new ArrayList());
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param args _more_
     *
     * @return _more_
     */
    protected String getEntryLink(Request request, Entry entry, List args) {
        return HtmlUtil.href(request.entryUrl(URL_ENTRY_SHOW, entry, args),
                             entry.getLabel());
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
        Entry entry = getEntry(request, entryId);
        if (entry == null) {
            System.err.println("Entry is null:" + entryId);
        }
        return getAssociations(request, entry);
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
    protected List<Association> getAssociations(Request request, Entry entry)
            throws Exception {
        if (entry.getAssociations() != null) {
            return entry.getAssociations();
        }
        if (entry.isDummy()) {
            return new ArrayList<Association>();
        }

        entry.setAssociations(
            getAssociations(
                request,
                Clause.or(
                    Clause.eq(COL_ASSOCIATIONS_FROM_ENTRY_ID, entry.getId()),
                    Clause.eq(COL_ASSOCIATIONS_TO_ENTRY_ID, entry.getId()))));
        return entry.getAssociations();
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<Association> getAssociations(Request request,
            Clause clause)
            throws Exception {
        Statement stmt = getDatabaseManager().select(COLUMNS_ASSOCIATIONS,
                             TABLE_ASSOCIATIONS, clause);
        List<Association> associations = new ArrayList();
        SqlUtil.Iterator  iter         = SqlUtil.getIterator(stmt);
        ResultSet         results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                associations.add(new Association(results.getString(1),
                        results.getString(2), results.getString(3),
                        results.getString(4), results.getString(5)));
            }
        }
        return associations;
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
    public String[] getAssociations(Request request) throws Exception {
        TypeHandler  typeHandler = getRepository().getTypeHandler(request);
        List<Clause> where       = typeHandler.assembleWhereClause(request);
        if (where.size() > 0) {
            where.add(0, Clause.eq(COL_ASSOCIATIONS_FROM_ENTRY_ID,
                                   COL_ENTRIES_ID));
            where.add(0, Clause.eq(COL_ASSOCIATIONS_TO_ENTRY_ID,
                                   COL_ENTRIES_ID));
        }

        return SqlUtil.readString(typeHandler.select(request,
                SqlUtil.distinct(COL_ASSOCIATIONS_NAME), where, ""), 1);
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
        if (entry.isDummy()) {
            return new ArrayList<Comment>();
        }
        Statement stmt = getDatabaseManager().select(COLUMNS_COMMENTS,
                             TABLE_COMMENTS,
                             Clause.eq(COL_COMMENTS_ENTRY_ID, entry.getId()),
                             " order by " + COL_COMMENTS_DATE + " asc ");
        SqlUtil.Iterator iter     = SqlUtil.getIterator(stmt);
        List<Comment>    comments = new ArrayList();
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
     * @param association _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getAssociationLinks(Request request, String association)
            throws Exception {
        if (true) {
            return BLANK;
        }
        String search = HtmlUtil.href(
                            request.url(
                                URL_SEARCH_FORM, ARG_ASSOCIATION,
                                encode(association)), HtmlUtil.img(
                                    fileUrl(ICON_SEARCH),
                                    msg("Search in association")));

        return search;
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
        if (request.defined("submit_type.x")
                || request.defined("submit_subset")) {
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
            //            result.putProperty(PROP_NAVSUBLINKS,
            //                               getSearchFormLinks(request, what));
            return result;
        }

        StringBuffer searchCriteriaSB = new StringBuffer();

        Group        theGroup         = null;
        List[]       pair             = getEntries(request, searchCriteriaSB);
        if (request.defined(ARG_GROUP)) {
            String groupId = (String) request.getString(ARG_GROUP, "").trim();
            //            System.err.println("group:" + groupId);
            theGroup = getRepository().findGroup(request, groupId);
        }


        String s = searchCriteriaSB.toString();
        if (s.length() > 0) {
            request.remove("submit");
            String url = request.getUrl(URL_SEARCH_FORM);
            s = "<table>" + s + "</table>";
            String header = HtmlUtil.href(
                                url,
                                HtmlUtil.img(
                                    fileUrl(ICON_SEARCH),
                                    "Search Again")) + "Search Criteria";
            request.setLeftMessage(HtmlUtil.br(header) + s);
        }
        if (theGroup == null) {
            theGroup = getDummyGroup();
        }
        return getOutputHandler(request).outputGroup(request, theGroup,
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
        statement.setString(col++, entry.getCollectionGroupId());
        statement.setString(col++, entry.getUser().getId());
        if (entry.getResource() == null) {
            entry.setResource(new Resource());
        }
        statement.setString(col++, entry.getResource().getPath());
        statement.setString(col++, entry.getResource().getType());
        statement.setString(col++, entry.getDataType());
        statement.setTimestamp(col++, new java.sql.Timestamp(currentTime()),
                               calendar);
        //        System.err.println (entry.getName() + " " + new Date(entry.getStartDate()));
        try {
            statement.setTimestamp(
                col, new java.sql.Timestamp(entry.getStartDate()), calendar);
            statement.setTimestamp(
                col + 1, new java.sql.Timestamp(entry.getEndDate()),
                calendar);

        } catch (Exception exc) {
            System.err.println("Error: Bad date " + entry.getResource() + " "
                               + new Date(entry.getStartDate()));
            statement.setTimestamp(
                col, new java.sql.Timestamp(new Date().getTime()), calendar);
            statement.setTimestamp(
                col + 1, new java.sql.Timestamp(new Date().getTime()),
                calendar);
        }
        col += 2;
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
     *
     * @param request _more_
     * @param entries _more_
     * @param connection _more_
     * @param firstCall _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    private List<String[]> getDescendents(Request request,
                                          List<Entry> entries,
                                          Connection connection,
                                          boolean firstCall)
            throws Exception {
        List<String[]> children = new ArrayList();
        for (Entry entry : entries) {
            if (firstCall) {
                children.add(new String[] { entry.getId(),
                                            entry.getTypeHandler().getType(),
                                            entry.getResource().getPath(),
                                            entry.getResource().getType() });
            }
            if ( !entry.isGroup()) {
                continue;
            }
            Statement stmt = SqlUtil.select(connection,
                                            SqlUtil.comma(new String[] {
                                                COL_ENTRIES_ID,
                    COL_ENTRIES_TYPE, COL_ENTRIES_RESOURCE,
                    COL_ENTRIES_RESOURCE_TYPE }), Misc.newList(
                        TABLE_ENTRIES), Clause.eq(
                        COL_ENTRIES_PARENT_GROUP_ID, entry.getId()));

            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSet        results;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    int    col          = 1;
                    String childId      = results.getString(col++);
                    String childType    = results.getString(col++);
                    String resource     = results.getString(col++);
                    String resourceType = results.getString(col++);
                    children.add(new String[] { childId, childType, resource,
                            resourceType });
                    if (childType.equals(TYPE_GROUP)) {
                        children.addAll(getDescendents(request,
                                (List<Entry>) Misc.newList(findGroup(request,
                                    childId)), connection, false));
                    }
                }
            }
        }
        return children;
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param asynchId _more_
     *
     * @throws Exception _more_
     */
    protected void deleteEntries(Request request, List<Entry> entries,
                                 Object asynchId)
            throws Exception {

        if (entries.size() == 0) {
            return;
        }
        delCnt = 0;
        Connection connection = getConnection(true);
        try {
            deleteEntriesInner(request, entries, connection, asynchId);
        } finally {
            try {
                connection.close();
            } catch (Exception exc) {}
        }
        clearCache();
    }




    /** _more_ */
    int delCnt = 0;



    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param connection _more_
     * @param actionId _more_
     *
     * @throws Exception _more_
     */
    private void deleteEntriesInner(Request request, List<Entry> entries,
                                    Connection connection, Object actionId)
            throws Exception {

        List<String[]> found = getDescendents(request, entries, connection,
                                   true);
        String query;
        query = SqlUtil.makeDelete(TABLE_PERMISSIONS,
                                   SqlUtil.eq(COL_PERMISSIONS_ENTRY_ID, "?"));

        PreparedStatement permissionsStmt =
            connection.prepareStatement(query);

        query = SqlUtil.makeDelete(
            TABLE_ASSOCIATIONS,
            SqlUtil.makeOr(
                Misc.newList(
                    SqlUtil.eq(COL_ASSOCIATIONS_FROM_ENTRY_ID, "?"),
                    SqlUtil.eq(COL_ASSOCIATIONS_TO_ENTRY_ID, "?"))));
        PreparedStatement assocStmt = connection.prepareStatement(query);

        query = SqlUtil.makeDelete(TABLE_COMMENTS,
                                   SqlUtil.eq(COL_COMMENTS_ENTRY_ID, "?"));
        PreparedStatement commentsStmt = connection.prepareStatement(query);

        query = SqlUtil.makeDelete(TABLE_METADATA,
                                   SqlUtil.eq(COL_METADATA_ENTRY_ID, "?"));
        PreparedStatement metadataStmt = connection.prepareStatement(query);


        PreparedStatement entriesStmt =
            connection.prepareStatement(SqlUtil.makeDelete(TABLE_ENTRIES,
                COL_ENTRIES_ID, "?"));

        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        int       deleteCnt = 0;
        int       totalDeleteCnt = 0;
        //Go backwards so we go up the tree and hit the children first
        for (int i = found.size() - 1; i >= 0; i--) {
            String[] tuple = found.get(i);
            String   id    = tuple[0];
            //            System.err.println ("id:" + id + " type:" + tuple[1] +" resource:" +tuple[2]);
            deleteCnt++;
            totalDeleteCnt++;
            if ((actionId != null)
                    && !getActionManager().getActionOk(actionId)) {
                getActionManager().setActionMessage(actionId,
                        "Delete canceled");
                connection.rollback();
                permissionsStmt.close();
                metadataStmt.close();
                commentsStmt.close();
                assocStmt.close();
                entriesStmt.close();
                return;
            }
            getActionManager().setActionMessage(actionId,
                    "Deleted:" + totalDeleteCnt + "/" + found.size() + " entries");
            if (totalDeleteCnt % 100 == 0) {
                System.err.println("Deleted:" + deleteCnt);
            }
            getStorageManager().removeFile(new Resource(new File(tuple[2]),
                    tuple[3]));

            permissionsStmt.setString(1, id);
            permissionsStmt.addBatch();

            metadataStmt.setString(1, id);
            metadataStmt.addBatch();

            commentsStmt.setString(1, id);
            commentsStmt.addBatch();

            assocStmt.setString(1, id);
            assocStmt.setString(2, id);
            assocStmt.addBatch();

            entriesStmt.setString(1, id);
            entriesStmt.addBatch();

            //TODO: Batch up the specific type deletes
            TypeHandler typeHandler = getTypeHandler(tuple[1]);
            typeHandler.deleteEntry(request, statement, id);
            if (deleteCnt > 1000) {
                permissionsStmt.executeBatch();
                metadataStmt.executeBatch();
                commentsStmt.executeBatch();
                assocStmt.executeBatch();
                entriesStmt.executeBatch();
                deleteCnt = 0;
            }
        }

        permissionsStmt.executeBatch();
        metadataStmt.executeBatch();
        commentsStmt.executeBatch();
        assocStmt.executeBatch();
        entriesStmt.executeBatch();

        connection.commit();
        connection.setAutoCommit(true);

        permissionsStmt.close();
        metadataStmt.close();
        commentsStmt.close();
        assocStmt.close();
        entriesStmt.close();
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
        if ( !isNew) {
            clearCache();
        }


        //We have our own connection
        Connection connection = getConnection(true);
        try {
            insertEntriesInner(entries, connection, isNew, canBeBatched);
        } finally {
            try {
                connection.close();
            } catch (Exception exc) {}
        }
    }


    /**
     * _more_
     *
     * @param entries _more_
     * @param connection _more_
     * @param isNew _more_
     * @param canBeBatched _more_
     *
     * @throws Exception _more_
     */
    private void insertEntriesInner(List<Entry> entries,
                                    Connection connection, boolean isNew,
                                    boolean canBeBatched)
            throws Exception {

        if (entries.size() == 0) {
            return;
        }
        if ( !isNew) {
            clearCache();
        }

        long              t1          = System.currentTimeMillis();
        int               cnt         = 0;
        int               metadataCnt = 0;

        PreparedStatement entryStmt   = connection.prepareStatement(isNew
                ? INSERT_ENTRIES
                : UPDATE_ENTRIES);

        PreparedStatement metadataStmt =
            connection.prepareStatement(INSERT_METADATA);


        Hashtable typeStatements = new Hashtable();

        int       batchCnt       = 0;
        connection.setAutoCommit(false);
        for (Entry entry : entries) {
            if (entry.isCollectionGroup()) {
                topGroups = null;
            }
            TypeHandler typeHandler = entry.getTypeHandler();
            String      sql         = typeHandler.getInsertSql(isNew);
            //            System.err.println("sql:" + sql);
            PreparedStatement typeStatement = null;
            if (sql != null) {
                typeStatement = (PreparedStatement) typeStatements.get(sql);
                if (typeStatement == null) {
                    typeStatement = connection.prepareStatement(sql);
                    typeStatements.put(sql, typeStatement);
                }
            }

            setStatement(entry, entryStmt, isNew);
            batchCnt++;
            entryStmt.addBatch();

            if (typeStatement != null) {
                batchCnt++;
                typeHandler.setStatement(entry, typeStatement, isNew);
                typeStatement.addBatch();
            }


            List<Metadata> metadataList = entry.getMetadata();
            if (metadataList != null) {
                if ( !isNew) {
                    SqlUtil.delete(getConnection(), TABLE_METADATA,
                                   Clause.eq(COL_METADATA_ENTRY_ID,
                                             entry.getId()));
                }
                for (Metadata metadata : metadataList) {
                    int col = 1;
                    metadataCnt++;
                    metadataStmt.setString(col++, metadata.getId());
                    metadataStmt.setString(col++, metadata.getEntryId());
                    metadataStmt.setString(col++, metadata.getType());
                    metadataStmt.setInt(col++, metadata.getInherited()
                            ? 1
                            : 0);
                    metadataStmt.setString(col++, metadata.getAttr1());
                    metadataStmt.setString(col++, metadata.getAttr2());
                    metadataStmt.setString(col++, metadata.getAttr3());
                    metadataStmt.setString(col++, metadata.getAttr4());
                    metadataStmt.addBatch();
                    batchCnt++;

                }
            }

            if (batchCnt > 1000) {
                //                    if(isNew)
                entryStmt.executeBatch();
                //                    else                        entryStmt.executeUpdate();
                if (metadataCnt > 0) {
                    metadataStmt.executeBatch();
                }
                for (Enumeration keys = typeStatements.keys();
                        keys.hasMoreElements(); ) {
                    typeStatement = (PreparedStatement) typeStatements.get(
                        keys.nextElement());
                    //                        if(isNew)
                    typeStatement.executeBatch();
                    //                        else                            typeStatement.executeUpdate();
                }
                batchCnt    = 0;
                metadataCnt = 0;
            }
        }
        if (batchCnt > 0) {
            entryStmt.executeBatch();
            metadataStmt.executeBatch();
            for (Enumeration keys = typeStatements.keys();
                    keys.hasMoreElements(); ) {
                PreparedStatement typeStatement =
                    (PreparedStatement) typeStatements.get(
                        keys.nextElement());
                typeStatement.executeBatch();
            }
        }
        connection.commit();
        connection.setAutoCommit(true);


        long t2 = System.currentTimeMillis();
        totalTime    += (t2 - t1);
        totalEntries += entries.size();
        if (t2 > t1) {
            //System.err.println("added:" + entries.size() + " entries in " + (t2-t1) + " ms  Rate:" + (entries.size()/(t2-t1)));
            double seconds = totalTime / 1000.0;
            if ((totalEntries % 100 == 0) && (seconds > 0)) {
                System.err.println(totalEntries + " average rate:"
                                   + (int) (totalEntries / seconds)
                                   + "/second");
            }
        }


        entryStmt.close();
        metadataStmt.close();
        for (Enumeration keys =
                typeStatements.keys(); keys.hasMoreElements(); ) {
            PreparedStatement typeStatement =
                (PreparedStatement) typeStatements.get(keys.nextElement());
            typeStatement.close();
        }

        connection.close();
        Misc.run(this, "checkNewEntries", entries);
    }

    /** _more_ */
    long totalTime = 0;

    /** _more_ */
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
        String      query     = BLANK;
        try {
            if (entries.size() == 0) {
                return needToAdd;
            }
            if (seenResources.size() > 50000) {
                seenResources = new Hashtable();
            }
            PreparedStatement select =
                SqlUtil.getSelectStatement(
                    getConnection(), "count(" + COL_ENTRIES_ID + ")",
                    Misc.newList(TABLE_ENTRIES),
                    Clause.and(
                        Clause.eq(COL_ENTRIES_RESOURCE, ""),
                        Clause.eq(COL_ENTRIES_PARENT_GROUP_ID, "?")), "");
            long t1 = System.currentTimeMillis();
            for (Entry entry : entries) {
                String path = entry.getResource().getPath();
                String key  = entry.getParentGroup().getId() + "_" + path;
                if (seenResources.get(key) != null) {
                    continue;
                }
                seenResources.put(key, key);
                select.setString(1, path);
                select.setString(2, entry.getParentGroup().getId());
                //                select.addBatch();
                ResultSet results = select.executeQuery();
                if (results.next()) {
                    int found = results.getInt(1);
                    if (found == 0) {
                        needToAdd.add(entry);
                    }
                }
            }
            select.close();
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
     * Class AccessException _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class AccessException extends RuntimeException {

        /**
         * _more_
         *
         * @param message _more_
         */
        public AccessException(String message) {
            super(message);
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
            String[] toks = StringUtil.split(line, ":", 2);
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

