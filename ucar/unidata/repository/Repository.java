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

import ucar.unidata.repository.auth.*;

import ucar.unidata.repository.data.*;

import ucar.unidata.repository.ftp.FtpManager;
import ucar.unidata.repository.harvester.*;

import ucar.unidata.repository.metadata.*;
import ucar.unidata.repository.monitor.*;

import ucar.unidata.repository.output.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.repository.util.*;







import ucar.unidata.sql.Clause;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.Cache;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.PluginClassLoader;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;



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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import java.util.jar.*;



import java.util.regex.*;
import java.util.zip.*;





/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Repository extends RepositoryBase implements RequestHandler {


    /** _more_ */
    public static final String MACRO_LINKS = "links";

    /** _more_ */
    public static final String MACRO_LOGO_URL = "logo.url";

    /** _more_ */
    public static final String MACRO_LOGO_IMAGE = "logo.image";



    /** _more_ */
    public static final String MACRO_ENTRY_HEADER = "entry.header";

    /** _more_ */
    public static final String MACRO_ENTRY_BREADCRUMBS = "entry.breadcrumbs";




    /** _more_ */
    public static final String MACRO_HEADER_IMAGE = "header.image";

    /** _more_ */
    public static final String MACRO_HEADER_TITLE = "header.title";

    /** _more_ */
    public static final String MACRO_USERLINK = "userlink";

    /** _more_ */
    public static final String MACRO_FAVORITES = "favorites";


    /** _more_ */
    public static final String MACRO_REPOSITORY_NAME = "repository_name";

    /** _more_ */
    public static final String MACRO_FOOTER = "footer";

    /** _more_ */
    public static final String MACRO_TITLE = "title";

    /** _more_ */
    public static final String MACRO_ROOT = "root";

    /** _more_ */
    public static final String MACRO_HEADFINAL = "headfinal";




    /** _more_ */
    public static final String MACRO_BOTTOM = "bottom";

    /** _more_ */
    public static final String MACRO_CONTENT = "content";



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

    /** _more_ */
    protected RequestUrl[] groupEditUrls = {
        URL_ENTRY_NEW, URL_ENTRY_FORM, getMetadataManager().URL_METADATA_FORM,
        getMetadataManager().URL_METADATA_ADDFORM,
        URL_ACCESS_FORM  //,
        //        URL_ENTRY_DELETE
        //        URL_ENTRY_SHOW
    };


    /** _more_ */
    List<RequestUrl> initializedUrls = new ArrayList<RequestUrl>();

    /** _more_ */
    private static final int PAGE_CACHE_LIMIT = 100;



    /** _more_ */
    public static final OutputType OUTPUT_DELETER =
        new OutputType("Delete Entry", "repository.delete",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_DELETE);


    /** _more_ */
    public static final OutputType OUTPUT_TYPECHANGE =
        new OutputType("Change Type", "repository.typechange",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       null);

    /** _more_ */
    public static final OutputType OUTPUT_PUBLISH =
        new OutputType("Make Public", "repository.makepublic",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_PUBLISH);


    /** _more_ */
    public static final OutputType OUTPUT_METADATA_FULL =
        new OutputType("Add full metadata", "repository.metadata.full",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_METADATA_ADD);

    /** _more_ */
    public static final OutputType OUTPUT_METADATA_SHORT =
        new OutputType("Add short metadata", "repository.metadata.short",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_METADATA_ADD);


    /** _more_ */
    public static final OutputType OUTPUT_COPY =
        new OutputType("Copy/Move Entry", "repository.copy",
                       OutputType.TYPE_ACTION | OutputType.TYPE_EDIT, "",
                       ICON_MOVE);


    /** _more_ */
    private Properties mimeTypes;


    /** _more_ */
    private Properties properties = new Properties();

    /** _more_ */
    private Properties cmdLineProperties = new Properties();

    /** _more_ */
    private Map<String, String> systemEnv;

    /** _more_ */
    private Properties dbProperties = new Properties();



    /** _more_ */
    private Properties phraseMap;



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

    /** _more_ */
    private List<Class> entryMonitorClasses = new ArrayList<Class>();


    /** _more_ */
    private List<OutputHandler> allOutputHandlers =
        new ArrayList<OutputHandler>();



    /** _more_ */
    private Hashtable resources = new Hashtable();


    /** _more_ */
    private Hashtable namesHolder = new Hashtable();


    /** _more_ */
    private List<String> typeDefFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> apiDefFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> outputDefFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> entryMonitorDefFiles = new ArrayList<String>();

    /** _more_ */
    private List<String> metadataDefFiles = new ArrayList<String>();


    /** _more_ */
    private List<String> pythonLibs = new ArrayList<String>();



    /** _more_ */
    private List<User> cmdLineUsers = new ArrayList();


    /** _more_ */
    String[] args;


    /** _more_ */
    private boolean inTomcat = false;



    /** _more_ */
    public static boolean debug = true;

    /** _more_ */
    private UserManager userManager;

    /** _more_ */
    private OaiManager oaiManager;

    /** _more_ */
    private MonitorManager monitorManager;

    /** _more_ */
    private SessionManager sessionManager;

    /** _more_ */
    private LogManager logManager;

    /** _more_ */
    private EntryManager entryManager;

    /** _more_ */
    private AssociationManager associationManager;

    /** _more_ */
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
    private RegistryManager registryManager;

    /** _more_ */
    private StorageManager storageManager;

    /** _more_ */
    private DatabaseManager databaseManager;

    /** _more_          */
    private FtpManager ftpManager;

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




    /** _more_ */
    Hashtable<String, ApiMethod> requestMap = new Hashtable();

    /** _more_ */
    ApiMethod homeApi;

    /** _more_ */
    ArrayList<ApiMethod> apiMethods = new ArrayList();

    /** _more_ */
    ArrayList<ApiMethod> wildCardApiMethods = new ArrayList();

    /** _more_ */
    ArrayList<ApiMethod> topLevelMethods = new ArrayList();



    /**
     * _more_
     *
     * @param args _more_
     * @param port _more_
     * @param inTomcat _more_
     *
     * @throws Exception _more_
     */
    public Repository(String[] args, int port, boolean inTomcat)
            throws Exception {
        super(port);

        LogUtil.setTestMode(true);
        java.net.InetAddress localMachine =
            java.net.InetAddress.getLocalHost();
        setHostname(localMachine.getHostName());
        this.inTomcat = inTomcat;
        this.args     = args;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public String getHostname() {
        String hostname = getProperty(PROP_HOSTNAME, (String) null);
        if ((hostname != null) && (hostname.trim().length() > 0)) {
            return hostname;
        }
        return super.getHostname();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getPort() {
        String port = getProperty(PROP_PORT, (String) null);

        if (port != null) {
            port = port.trim();
            if (port.length() > 0) {
                return Integer.decode(port).intValue();
            }
        }
        return super.getPort();
    }


    /** _more_ */
    private boolean ignoreSSL = false;

    /**
     * _more_
     *
     *
     * @param request _more_
     * @return _more_
     */
    public boolean isSSLEnabled(Request request) {
        if (ignoreSSL) {
            return false;
        }
        if (getProperty(PROP_SSL_IGNORE, false)) {
            return false;
        }
        return getHttpsPort() >= 0;
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
     * @param ms _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, long ms, String timezone) {
        return formatDate(new Date(ms), timezone);
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
     * @param request _more_
     * @param d _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String formatDate(Request request, Date d, String timezone) {
        return formatDate(d, timezone);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param d _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String formatDateShort(Request request, Date d, String timezone) {
        return formatDateShort(request, d, timezone, "");
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param d _more_
     * @param timezone _more_
     * @param extraAlt _more_
     *
     * @return _more_
     */
    public String formatDateShort(Request request, Date d, String timezone,
                                  String extraAlt) {
        SimpleDateFormat sdf = getSDF(getProperty(PROP_DATE_SHORTFORMAT,
                                   DEFAULT_TIME_SHORTFORMAT), timezone);
        if (d == null) {
            return BLANK;
        }

        Date   now      = new Date();
        long   diff     = now.getTime() - d.getTime();
        double minutes  = DateUtil.millisToMinutes(diff);
        String fullDate = formatDate(d, timezone);
        String result;
        if ((minutes > 0) && (minutes < 65) && (minutes > 55)) {
            result = "about an hour ago";
        } else if ((diff > 0) && (diff < DateUtil.minutesToMillis(1))) {
            result = (int) (diff / (1000)) + " seconds ago";
        } else if ((diff > 0) && (diff < DateUtil.hoursToMillis(1))) {
            int value = (int) DateUtil.millisToMinutes(diff);
            result = value + " minute" + ((value > 1)
                                          ? "s"
                                          : "") + " ago";
        } else if ((diff > 0) && (diff < DateUtil.hoursToMillis(24))) {
            int value = (int) (diff / (1000 * 60 * 60));
            result = value + " hour" + ((value > 1)
                                        ? "s"
                                        : "") + " ago";
        } else if ((diff > 0) && (diff < DateUtil.daysToMillis(6))) {
            int value = (int) (diff / (1000 * 60 * 60 * 24));
            result = value + " day" + ((value > 1)
                                       ? "s"
                                       : "") + " ago";
        } else {
            result = sdf.format(d);
        }
        return HtmlUtil.span(result,
                             HtmlUtil.cssClass("time")
                             + HtmlUtil.attr(HtmlUtil.ATTR_TITLE,
                                             fullDate + extraAlt));
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void close() throws Exception {
        getDatabaseManager().shutdown();
        getFtpManager().shutdown();
    }


    /**
     * _more_
     *
     * @param port _more_
     */
    protected void setHttpsPort(int port) {
        super.setHttpsPort(port);
        reinitializeRequestUrls();
    }


    /**
     * _more_
     *
     *
     * @param properties _more_
     * @throws Exception _more_
     */
    protected void init(Properties properties) throws Exception {
        /*
        final PrintStream oldErr = System.err;
        final PrintStream oldOut = System.out;
        System.setErr(new PrintStream(oldOut){
                public void     println(String x) {
                    if(x.indexOf("Fatal")>=0) {
                        Misc.printStack("got it");
                    }
                    oldErr.println(x);
                }
            });
        */

        initProperties(properties);
        initServer();
        getLogManager().logInfoAndPrint("RAMADDA started");
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
        try {
            properties.load(
                IOUtil.getInputStream(
                    "/ucar/unidata/repository/resources/build.properties",
                    getClass()));
        } catch (Exception exc) {}

        for (int i = 0; i < args.length; i++) {
            if (checkFile(args[i])) {
                continue;
            }
            if (args[i].endsWith(".properties")) {
                cmdLineProperties.load(IOUtil.getInputStream(args[i],
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
                if (toks.size() == 0) {
                    throw new IllegalArgumentException("Bad argument:"
                            + args[i]);
                } else if (toks.size() == 1) {
                    cmdLineProperties.put(toks.get(0), "");
                } else {
                    cmdLineProperties.put(toks.get(0), toks.get(1));
                }
            } else {
                usage("Unknown argument: " + args[i]);
            }
        }

        //Load the context and the command line properties now 
        //so the storage manager can get to them
        if (contextProperties != null) {
            properties.putAll(contextProperties);
        }


        //Call the storage manager so it can figure out the home dir
        getStorageManager();

        try {
            //Now load in the local properties file
            //First load in the repository.properties file
            String localPropertyFile =
                IOUtil.joinDir(getStorageManager().getRepositoryDir(),
                               "repository.properties");
            if (new File(localPropertyFile).exists()) {
                properties.load(IOUtil.getInputStream(localPropertyFile,
                        getClass()));
            }

            File[] localFiles =
                getStorageManager().getRepositoryDir().listFiles();
            for (File f : localFiles) {
                if ( !f.toString().endsWith(".properties")) {
                    continue;
                }
                if (f.getName().equals("repository.properties")) {
                    continue;
                }
                properties.load(IOUtil.getInputStream(f.toString(),
                        getClass()));

            }

        } catch (Exception exc) {}


        //create the log dir
        getStorageManager().getLogDir();

        initPlugins();

        apiDefFiles.addAll(0, getResourcePaths(PROP_API));
        typeDefFiles.addAll(0, getResourcePaths(PROP_TYPES));
        outputDefFiles.addAll(0, getResourcePaths(PROP_OUTPUTHANDLERS));
        metadataDefFiles.addAll(0, getResourcePaths(PROP_METADATA));

        debug = getProperty(PROP_DEBUG, false);
        //        System.err.println ("debug:" + debug);

        setUrlBase((String) properties.get(PROP_HTML_URLBASE));
        if (getUrlBase() == null) {
            setUrlBase(BLANK);
        }

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
        defaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(DateUtil.TIMEZONE_GMT);


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
     * @throws Exception _more_
     */
    protected void initServer() throws Exception {

        getDatabaseManager().init();
        initTypeHandlers();
        initSchema();

        for (String sqlFile : (List<String>) loadFiles) {
            String sql =
                getStorageManager().readUncheckedSystemResource(sqlFile);
            getDatabaseManager().loadSql(sql, false, true);
        }
        readGlobals();

        checkVersion();

        initOutputHandlers();
        getMetadataManager().initMetadataHandlers(metadataDefFiles);
        initApi();
        getRegistryManager().checkApi();

        getUserManager().initUsers(cmdLineUsers);
        getEntryManager().initGroups();
        getHarvesterManager().initHarvesters();
        initLanguages();

        setLocalFilePaths();

        if (dumpFile != null) {
            FileOutputStream fos = new FileOutputStream(dumpFile);
            getDatabaseManager().makeDatabaseCopy(fos, true);
            fos.close();
        }

        HtmlUtil.setBlockHideShowImage(iconUrl(ICON_MINUS),
                                       iconUrl(ICON_PLUS));
        HtmlUtil.setInlineHideShowImage(iconUrl(ICON_MINUS),
                                        iconUrl(ICON_ELLIPSIS));
        getLogManager().logInfo("RAMADDA started");


        getStorageManager().doFinalInitialization();
        if (getInstallationComplete()) {
            getRegistryManager().doFinalInitialization();
        }
        getFtpManager();
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
    protected SessionManager doMakeSessionManager() {
        return new SessionManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected EntryManager doMakeEntryManager() {
        return new EntryManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected AssociationManager doMakeAssociationManager() {
        return new AssociationManager(this);
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
    protected FtpManager doMakeFtpManager() {
        return new FtpManager(this);
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
    protected UserManager doMakeUserManager() {
        return new UserManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public UserManager getUserManager() {
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
    protected OaiManager doMakeOaiManager() {
        return new OaiManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public OaiManager getOaiManager() {
        if (oaiManager == null) {
            oaiManager = doMakeOaiManager();
        }
        return oaiManager;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected MonitorManager doMakeMonitorManager() {
        return new MonitorManager(this);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public MonitorManager getMonitorManager() {
        if (monitorManager == null) {
            monitorManager = doMakeMonitorManager();
        }
        return monitorManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public SessionManager getSessionManager() {
        if (sessionManager == null) {
            sessionManager = doMakeSessionManager();
            sessionManager.init();
        }
        return sessionManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public LogManager getLogManager() {
        if (logManager == null) {
            logManager = doMakeLogManager();
            logManager.init();
        }
        return logManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected LogManager doMakeLogManager() {
        return new LogManager(this);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public EntryManager getEntryManager() {
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
    public AssociationManager getAssociationManager() {
        if (associationManager == null) {
            associationManager = doMakeAssociationManager();
        }
        return associationManager;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public HarvesterManager getHarvesterManager() {
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
    public ActionManager getActionManager() {
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
    public AccessManager getAccessManager() {
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
    public SearchManager getSearchManager() {
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
    public List<String> getPythonLibs() {
        return pythonLibs;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public MetadataManager getMetadataManager() {
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
    protected RegistryManager doMakeRegistryManager() {
        return new RegistryManager(this);
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public RegistryManager getRegistryManager() {
        if (registryManager == null) {
            registryManager = doMakeRegistryManager();
        }
        return registryManager;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public StorageManager getStorageManager() {
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
    public DatabaseManager getDatabaseManager() {
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
    public FtpManager getFtpManager() {
        if (ftpManager == null) {
            ftpManager = doMakeFtpManager();
        }
        return ftpManager;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Admin getAdmin() {
        if (admin == null) {
            admin = doMakeAdmin();
        }
        return admin;
    }


    /** _more_ */
    public static final double VERSION = 1.0;

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    private void updateToVersion1_0() throws Exception {}

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void checkVersion() throws Exception {
        double version = getDbProperty(PROP_VERSION, 0.0);
        if (version == VERSION) {
            return;
        }
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
                getStorageManager().getRepositoryDir().toString());
        for (int i = 0; i < sourcePaths.size(); i++) {
            String       dir     = (String) sourcePaths.get(i);
            List<String> listing = IOUtil.getListing(dir, getClass());
            for (String path : listing) {
                if ( !path.endsWith(".pack")) {
                    continue;
                }
                String content =
                    getStorageManager().readUncheckedSystemResource(path,
                        (String) null);
                if (content == null) {
                    continue;
                }



                Object[]   result     = parsePhrases(content);
                String     type       = (String) result[0];
                String     name       = (String) result[1];
                Properties properties = (Properties) result[2];
                if (type != null) {
                    if (name == null) {
                        name = type;
                    }
                    languages.add(new TwoFacedObject(name, type));
                    languageMap.put(type, properties);
                } else {
                    getLogManager().logError("No _type_ found in: " + path);
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param content _more_
     *
     * @return _more_
     */
    private Object[] parsePhrases(String content) {
        List<String> lines   = StringUtil.split(content, "\n", true, true);
        Properties   phrases = new Properties();
        String       type    = null;
        String       name    = null;
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
                phrases.put(key, value);
            }
        }
        return new Object[] { type, name, phrases };
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
     * @param f _more_
     */
    protected void loadPluginFile(File f) {}

    /**
     * Class MyClassLoader _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class MyClassLoader extends PluginClassLoader {

        /**
         * _more_
         *
         * @param path _more_
         * @param parent _more_
         *
         * @throws Exception _more_
         */
        public MyClassLoader(String path, ClassLoader parent)
                throws Exception {
            super(path, parent);
        }

        /**
         * _more_
         *
         * @param c _more_
         *
         * @throws Exception _more_
         */
        protected void checkClass(Class c) throws Exception {
            if (UserAuthenticator.class.isAssignableFrom(c)) {
                getLogManager().logInfo("Adding authenticator:"
                                        + c.getName());
                getUserManager().addUserAuthenticator(
                    (UserAuthenticator) c.newInstance());
            }
        }

        /**
         * _more_
         *
         * @param jarEntry _more_
         *
         * @return _more_
         */
        protected String defineResource(JarEntry jarEntry) {
            String path = super.defineResource(jarEntry);
            checkFile(path);
            return path;
        }
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void initPlugins() throws Exception {
        File   dir     = new File(getStorageManager().getPluginsDir());
        File[] plugins = dir.listFiles();
        for (int i = 0; i < plugins.length; i++) {
            if (plugins[i].isDirectory()) {
                continue;
            }
            String pluginFile = plugins[i].toString();
            if (pluginFile.endsWith(".jar")) {
                PluginClassLoader cl = new MyClassLoader(pluginFile,
                                           getClass().getClassLoader());

                Misc.addClassLoader(cl);
                List entries = cl.getEntryNames();
                for (int entryIdx = 0; entryIdx < entries.size();
                        entryIdx++) {
                    String entry = (String) entries.get(entryIdx);
                    //                    if ( !checkFile(entry)) {
                    //                        getLogManager().logError("Don't know how to handle plugin resource:"
                    //                                 + entry + " from plugin:" + plugins[i]);
                    //                    }
                }
            } else {
                checkFile(pluginFile);
            }
        }
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    protected boolean checkFile(String file) {
        if (file.indexOf("api.xml") >= 0) {
            apiDefFiles.add(file);
        } else if ((file.indexOf("types.xml") >= 0)
                   || (file.indexOf("type.xml") >= 0)) {
            typeDefFiles.add(file);
        } else if (file.indexOf("outputhandlers.xml") >= 0) {
            outputDefFiles.add(file);
        } else if (file.indexOf("metadata.xml") >= 0) {
            metadataDefFiles.add(file);
        } else if (file.endsWith(".py")) {
            pythonLibs.add(file);
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
    public List<String> getResourcePaths(String propertyName) {
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
        Statement statement =
            getDatabaseManager().select(Tables.GLOBALS.COLUMNS,
                                        Tables.GLOBALS.NAME, new Clause[] {});
        dbProperties = new Properties();
        ResultSet results = statement.getResultSet();
        while (results.next()) {
            String name  = results.getString(1);
            String value = results.getString(2);
            dbProperties.put(name, value);
        }
        getDatabaseManager().closeAndReleaseConnection(statement);
    }



    /**
     * _more_
     */
    protected void clearAllCaches() {
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.clearCache();
        }
        clearCache();
    }

    /**
     * _more_
     */
    protected void clearCache() {
        //        System.err.println ("Clear full cache ");
        pageCache     = new Hashtable();
        pageCacheList = new ArrayList();
        getEntryManager().clearCache();
        dataTypeList = null;
    }




    /**
     * _more_
     *
     * @param requestUrl _more_
     */
    public void initRequestUrl(RequestUrl requestUrl) {
        try {
            if ( !initializedUrls.contains(requestUrl)) {
                initializedUrls.add(requestUrl);
            }
            Request request = new Request(this, null,
                                          getUrlBase()
                                          + requestUrl.getPath());
            super.initRequestUrl(requestUrl);
            ApiMethod apiMethod = findApiMethod(request);
            if (apiMethod == null) {
                getLogManager().logError("Could not find api for: "
                                         + requestUrl.getPath());
                return;
            }
            if (isSSLEnabled(null) && apiMethod.getNeedsSsl()) {
                requestUrl.setNeedsSsl(true);
            }
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * _more_
     */
    protected void reinitializeRequestUrls() {
        for (RequestUrl requestUrl : initializedUrls) {
            initRequestUrl(requestUrl);
        }
    }


    /**
     * _more_
     *
     * @param requestUrl _more_
     *
     * @return _more_
     */
    public String getUrlPath(RequestUrl requestUrl) {
        if (requestUrl.getNeedsSsl()) {
            return httpsUrl(getUrlBase() + requestUrl.getPath());
        }
        return getUrlBase() + requestUrl.getPath();
    }

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
        boolean needsSsl = XmlUtil.getAttributeFromTree(node,
                               ApiMethod.ATTR_NEEDS_SSL, false);
        boolean checkAuthMethod = XmlUtil.getAttributeFromTree(node,
                                      ApiMethod.ATTR_CHECKAUTHMETHOD, false);

        String authMethod = XmlUtil.getAttributeFromTree(node,
                                ApiMethod.ATTR_AUTHMETHOD, "");

        boolean admin = XmlUtil.getAttributeFromTree(node,
                            ApiMethod.ATTR_ADMIN,
                            Misc.getProperty(props, ApiMethod.ATTR_ADMIN,
                                             true));

        boolean canCache = XmlUtil.getAttributeFromTree(node,
                               ApiMethod.ATTR_CANCACHE,
                               Misc.getProperty(props,
                                   ApiMethod.ATTR_CANCACHE, true));



        String handlerName = XmlUtil.getAttributeFromTree(node,
                                 ApiMethod.ATTR_HANDLER,
                                 Misc.getProperty(props,
                                     ApiMethod.ATTR_HANDLER, defaultHandler));


        RequestHandler handler = (RequestHandler) handlers.get(handlerName);

        if (handler == null) {
            handler = this;
            if (handlerName.equals("usermanager")) {
                handler = getUserManager();
            } else if (handlerName.equals("monitormanager")) {
                handler = getMonitorManager();
            } else if (handlerName.equals("oaimanager")) {
                handler = getOaiManager();
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
            } else if (handlerName.equals("associationmanager")) {
                handler = getAssociationManager();
            } else if (handlerName.equals("metadatamanager")) {
                handler = getMetadataManager();
            } else if (handlerName.equals("registrymanager")) {
                handler = getRegistryManager();
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
            throw new IllegalArgumentException("Unknown request method:"
                    + methodName);
        }


        ApiMethod apiMethod =
            new ApiMethod(this, handler, request,
                          XmlUtil.getAttribute(node, ApiMethod.ATTR_NAME,
                              request), method, admin, needsSsl, authMethod,
                                        checkAuthMethod, canCache,
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
            if (apiMethod.isWildcard()) {
                index = wildCardApiMethods.indexOf(oldMethod);
                wildCardApiMethods.remove(index);
                wildCardApiMethods.add(index, apiMethod);
            }
        } else {
            apiMethods.add(apiMethod);
            if (apiMethod.isWildcard()) {
                wildCardApiMethods.add(apiMethod);
            }
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
                        getLogManager().logWarning(
                            "Couldn't load optional output handler:"
                            + XmlUtil.toString(node));
                        getLogManager().logWarning(exc.toString());
                    } else {
                        getLogManager().logError(
                            "Error loading output handler file:" + file, exc);
                        throw exc;
                    }
                }
            }

        }


        OutputHandler outputHandler = new OutputHandler(getRepository(),
                                          "Entry Deleter") {

            public boolean canHandleOutput(OutputType output) {
                return output.equals(OUTPUT_DELETER)
                /*                    || output.equals(OUTPUT_TYPECHANGE)*/
                || output.equals(OUTPUT_METADATA_SHORT) || output.equals(
                    OUTPUT_PUBLISH) || output.equals(OUTPUT_METADATA_FULL);
            }
            public void getEntryLinks(Request request, State state,
                                      List<Link> links)
                    throws Exception {
                if ( !state.isDummyGroup()) {
                    return;
                }

                /*                if(request.getUser().getAdmin()) {
                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_TYPECHANGE));

                                       }*/
                for (Entry entry : state.getAllEntries()) {
                    if (getAccessManager().canDoAction(request, entry,
                            Permission.ACTION_EDIT)) {
                        if (getEntryManager().isAnonymousUpload(entry)) {
                            links.add(makeLink(request, state.getEntry(),
                                    OUTPUT_PUBLISH));
                            break;
                        }
                    }
                }
                boolean metadataOk = true;
                for (Entry entry : state.getAllEntries()) {
                    if ( !getAccessManager().canDoAction(request, entry,
                            Permission.ACTION_EDIT)) {
                        metadataOk = false;
                        break;
                    }
                }
                if (metadataOk) {
                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_METADATA_SHORT));

                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_METADATA_FULL));
                }

                boolean deleteOk = true;
                for (Entry entry : state.getAllEntries()) {
                    if ( !getAccessManager().canDoAction(request, entry,
                            Permission.ACTION_DELETE)) {
                        deleteOk = false;
                        break;
                    }
                }
                if (deleteOk) {
                    links.add(makeLink(request, state.getEntry(),
                                       OUTPUT_DELETER));
                }
            }


            public Result outputGroup(Request request, Group group,
                                      List<Group> subGroups,
                                      List<Entry> entries)
                    throws Exception {

                OutputType output = request.getOutput();
                if (output.equals(OUTPUT_PUBLISH)) {
                    return getEntryManager().publishEntries(request, entries);
                }


                if (output.equals(OUTPUT_METADATA_SHORT)) {
                    return getEntryManager().addInitialMetadataToEntries(
                        request, entries, true);
                }

                /*                if (output.equals(OUTPUT_TYPECHANGE)) {
                    return getEntryManager().changeType(
                                                        request, subGroups, entries);
                                                        }*/
                if (output.equals(OUTPUT_METADATA_FULL)) {
                    return getEntryManager().addInitialMetadataToEntries(
                        request, entries, false);
                }

                StringBuffer idBuffer = new StringBuffer();
                entries.addAll(subGroups);
                for (Entry entry : entries) {
                    idBuffer.append(",");
                    idBuffer.append(entry.getId());
                }
                return new Result(request.url(URL_ENTRY_DELETELIST,
                        ARG_ENTRYIDS, idBuffer.toString()));
            }
        };
        outputHandler.addType(OUTPUT_DELETER);
        addOutputHandler(outputHandler);



        OutputHandler copyHandler = new OutputHandler(getRepository(),
                                        "Entry Copier") {
            public boolean canHandleOutput(OutputType output) {
                return output.equals(OUTPUT_COPY);
            }
            public void getEntryLinks(Request request, State state,
                                      List<Link> links)
                    throws Exception {
                if ((request.getUser() == null)
                        || request.getUser().getAnonymous()) {
                    return;
                }
                if ( !state.isDummyGroup()) {
                    return;
                }
                links.add(makeLink(request, state.getEntry(), OUTPUT_COPY));
            }

            public Result outputEntry(Request request, Entry entry)
                    throws Exception {
                if (request.getUser().getAnonymous()) {
                    return new Result("", "");
                }
                return new Result(request.url(URL_ENTRY_COPY, ARG_FROM,
                        entry.getId()));
            }

            public Result outputGroup(Request request, Group group,
                                      List<Group> subGroups,
                                      List<Entry> entries)
                    throws Exception {
                if (request.getUser().getAnonymous()) {
                    return new Result("", "");
                }
                if ( !group.isDummy()) {
                    return outputEntry(request, group);
                }
                StringBuffer idBuffer = new StringBuffer();
                entries.addAll(subGroups);
                for (Entry entry : entries) {
                    idBuffer.append(",");
                    idBuffer.append(entry.getId());
                }
                return new Result(request.url(URL_ENTRY_COPY, ARG_FROM,
                        idBuffer.toString()));
            }
        };
        copyHandler.addType(OUTPUT_COPY);
        addOutputHandler(copyHandler);

        getUserManager().initOutputHandlers();


    }


    /** _more_ */
    private DataOutputHandler dataOutputHandler;

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public DataOutputHandler getDataOutputHandler() throws Exception {
        if (dataOutputHandler == null) {
            dataOutputHandler =
                (DataOutputHandler) getRepository().getOutputHandler(
                    DataOutputHandler.OUTPUT_OPENDAP.toString());
        }
        return dataOutputHandler;
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

        //        System.err.println("request:" + request);
        if (debug) {
            getLogManager().debug("user:" + request.getUser() + " -- "
                                  + request.toString());
        }

        //        logInfo("request:" + request);
        try {
            getSessionManager().checkSession(request);
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
                sb.append(showDialogError(translate(request,
                        "An error has occurred") + ":" + HtmlUtil.p()
                            + inner.getMessage()));
            } else {
                AccessException     ae         = (AccessException) inner;
                AuthorizationMethod authMethod =
                    AuthorizationMethod.AUTH_HTML;
                if (request.getApiMethod() != null) {
                    ApiMethod apiMethod = request.getApiMethod();
                    if (apiMethod.getCheckAuthMethod()) {
                        request.setCheckingAuthMethod(true);
                        Result authResult =
                            (Result) apiMethod.invoke(request);
                        authMethod = authResult.getAuthorizationMethod();
                    } else {
                        authMethod = AuthorizationMethod.getMethod(
                            apiMethod.getAuthMethod());
                    }
                }
                //              System.err.println ("auth:" + authMethod);
                if (authMethod.equals(AuthorizationMethod.AUTH_HTML)) {
                    sb.append(showDialogError(inner.getMessage()));
                    String redirect =
                        XmlUtil.encodeBase64(request.getUrl().getBytes());
                    sb.append(getUserManager().makeLoginForm(request,
                            HtmlUtil.hidden(ARG_REDIRECT, redirect)));
                } else {
                    sb.append(inner.getMessage());
                    //If the authmethod is basic http auth then, if ssl is enabled, we 
                    //want to have the authentication go over ssl. Else we do it clear text
                    if ( !request.getSecure() && isSSLEnabled(null)) {
                        /*
                        If ssl then we are a little tricky here. We redirect the request to the generic ssl based SSLREDIRCT url
                        passing the actual request as an argument. The processSslRedirect method checks for authentication. If
                        not authenticated then it throws an access exception which triggers a auth request back to the client
                        If authenticated then it redirects the client back to the original non ssl request
                        */
                        String redirectUrl =
                            XmlUtil.encodeBase64(request.getUrl().getBytes());
                        String url = HtmlUtil.url(URL_SSLREDIRECT.toString(),
                                         ARG_REDIRECT, redirectUrl);
                        result = new Result(url);
                    } else {
                        result = new Result("Error", sb);
                        result.addHttpHeader(HtmlUtil.HTTP_WWW_AUTHENTICATE,
                                             "Basic realm=\"ramadda\"");
                        result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
                    }
                    return result;
                }
            }

            if ((request.getUser() != null) && request.getUser().getAdmin()) {
                sb.append(
                    HtmlUtil.pre(
                        HtmlUtil.entityEncode(LogUtil.getStackTrace(inner))));
            }

            result = new Result(msg("Error"), sb);
            if (badAccess) {
                result.setResponseCode(Result.RESPONSE_UNAUTHORIZED);
                //                result.addHttpHeader(HtmlUtil.HTTP_WWW_AUTHENTICATE,"Basic realm=\"repository\"");
            } else {
                result.setResponseCode(Result.RESPONSE_INTERNALERROR);
                String userAgent = request.getHeaderArg("User-Agent");
                if (userAgent == null) {
                    userAgent = "Unknown";
                }
                getLogManager().logError("Error handling request:" + request
                                         + " ip:" + request.getIp(), inner);
            }
        }

        boolean okToAddCookie = false;


        if ((result != null) && (result.getInputStream() == null)
                && result.isHtml() && result.getShouldDecorate()
                && result.getNeedToWrite()) {
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
                request.setSessionId(getSessionManager().getSessionId());
            }
            String sessionId = request.getSessionId();
            //            result.addCookie("repositorysession", sessionId+"; path=" + getUrlBase() + "; expires=Fri, 31-Dec-2010 23:59:59 GMT;");
            result.addCookie(SessionManager.COOKIE_NAME,
                             sessionId + "; path=" + getUrlBase()
                             + "; expires=Fri, 31-Dec-2010 23:59:59 GMT;");
        }

        if (request.get("gc", false) && (request.getUser() != null)
                && request.getUser().getAdmin()) {
            clearAllCaches();
            Misc.gc();
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
    protected ApiMethod findApiMethod(Request request) throws Exception {
        String incoming = request.getRequestPath().trim();
        if (incoming.endsWith("/")) {
            incoming = incoming.substring(0, incoming.length() - 1);
        }
        String urlBase = getUrlBase();
        if (incoming.equals("/") || incoming.equals("")) {
            incoming = urlBase;
        }

        if ( !incoming.startsWith(urlBase)) {
            return null;
        }
        incoming = incoming.substring(urlBase.length());
        if (incoming.length() == 0) {
            return homeApi;
        }


        /*
        List<Group> topGroups =
            new ArrayList<Group>(getEntryManager().getTopGroups(request));
        topGroups.add(getEntryManager().getTopGroup());
        for (Group group : topGroups) {
            String name = "/" + getEntryManager().getPathFromEntry(group);
            if (incoming.startsWith(name + "/")) {
                incoming = incoming.substring(name.length());
                break;
            }
        }
        */

        ApiMethod apiMethod = (ApiMethod) requestMap.get(incoming);
        if (apiMethod == null) {
            for (ApiMethod tmp : wildCardApiMethods) {
                String path = tmp.getRequest();
                path = path.substring(0, path.length() - 2);
                if (incoming.startsWith(path)) {
                    apiMethod = tmp;
                    break;
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
     * @param path _more_
     *
     * @return _more_
     */
    public ApiMethod getApiMethod(String path) {
        return requestMap.get(path);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getInstallationComplete() {
        return getDbProperty(ARG_ADMIN_INSTALLCOMPLETE, false);
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

        ApiMethod apiMethod = findApiMethod(request);

        if (apiMethod == null) {
            return getHtdocsFile(request);
        }


        //        System.err.println("sslEnabled:" +sslEnabled + "  " + apiMethod.getNeedsSsl());
        Result sslRedirect = checkForSslRedirect(request, apiMethod);
        if (sslRedirect != null) {
            return sslRedirect;
        }


        //        System.out.println(absoluteUrl(request.getUrl()));

        request.setApiMethod(apiMethod);

        String userAgent = request.getHeaderArg("User-Agent");
        if (userAgent == null) {
            userAgent = "Unknown";
        }
        //        System.err.println(request + " user-agent:" + userAgent +" ip:" + request.getIp());

        if ( !getInstallationComplete()) {
            return getAdmin().doInitialization(request);
        }

        if ( !getUserManager().isRequestOk(request)
                || !apiMethod.isRequestOk(request, this)) {
            throw new AccessException(
                msg("You do not have permission to access this page"),
                request);
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


        getLogManager().logRequest(request);


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
     * @param apiMethod _more_
     *
     * @return _more_
     */
    private Result checkForSslRedirect(Request request, ApiMethod apiMethod) {
        boolean sslEnabled = isSSLEnabled(request);
        boolean allSsl     = false;
        if (sslEnabled) {
            allSsl = getProperty(PROP_ACCESS_ALLSSL, false);
            if (allSsl && !request.getSecure()) {
                return new Result(httpsUrl(request.getUrl()));
            }
        }


        if (sslEnabled) {
            if ( !request.get(ARG_NOREDIRECT, false)) {
                if (apiMethod.getNeedsSsl() && !request.getSecure()) {
                    return new Result(httpsUrl(request.getUrl()));
                } else if ( !allSsl && !apiMethod.getNeedsSsl()
                            && request.getSecure()) {
                    return new Result(absoluteUrl(request.getUrl()));
                }
            }
        }
        return null;
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
        //        System.err.println("path:" + path);
        if ( !path.startsWith(getUrlBase())) {
            getLogManager().log(request,
                                "Unknown request" + " \"" + path + "\"");
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
            try {
                InputStream is = getStorageManager().getInputStream(fullPath);
                if (path.endsWith(".js") || path.endsWith(".css")) {
                    String js = IOUtil.readInputStream(is);
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
        String userAgent = request.getHeaderArg(HtmlUtil.HTTP_USER_AGENT);
        if (userAgent == null) {
            userAgent = "Unknown";
        }

        if (path.startsWith("/alias/")) {
            String alias = path.substring("/alias/".length());
            if (alias.endsWith("/")) {
                alias = alias.substring(0, alias.length() - 1);
            }
            Entry entry = getEntryManager().getEntryFromAlias(request, alias);
            if (entry != null) {
                return new Result(request.url(URL_ENTRY_SHOW, ARG_ENTRYID,
                        entry.getId()));
            }
        }





        getLogManager().log(request,
                            "Unknown request:" + request.getUrl()
                            + " user-agent:" + userAgent + " ip:"
                            + request.getIp());
        Result result =
            new Result(msg("Error"),
                       new StringBuffer(msgLabel("Unknown request") + path));
        result.setResponseCode(Result.RESPONSE_NOTFOUND);
        return result;
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public HtmlTemplate getHtmlTemplate(Request request) {
        return null;
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

        String   template     = null;
        Metadata metadata     = null;
        String sessionMessage =
            getSessionManager().getSessionMessage(request);

        //        System.err.println(request +" DECORATE=" + request.get(ARG_DECORATE, true));

        if ( !request.get(ARG_DECORATE, true)) {
            template = getResource(
                "/ucar/unidata/repository/resources/templates/plain.html");
        }

        /*
        //            getMetadataManager().findMetadata((request.getCollectionEntry()
        //                != null)
        //                ? request.getCollectionEntry()
        //                : topGroup, AdminMetadataHandler.TYPE_TEMPLATE, true);
        */
        if (template == null) {
            if (metadata != null) {
                template = metadata.getAttr1();
                if (template.startsWith("file:")) {
                    template =
                        getStorageManager().localizePath(template.trim());
                    template = getStorageManager().readSystemResource(
                        template.substring("file:".length()));
                }
                if (template.indexOf("${content}") < 0) {
                    template = null;
                }
            }
        }

        if (template == null) {
            template = getTemplate(request).getTemplate();
            //            template = getResource(PROP_HTML_TEMPLATE);
        }


        String jsContent =
            HtmlUtil.div("", " id=\"tooltipdiv\" class=\"tooltip-outer\" ")
            + HtmlUtil.div("", " id=\"popupdiv\" class=\"tooltip-outer\" ")
            + HtmlUtil.div("", " id=\"output\"")
            + HtmlUtil.div("", " id=\"selectdiv\" class=\"selectdiv\" ")
            + HtmlUtil.div("", " id=\"floatdiv\" class=\"floatdiv\" ");

        List   links     = (List) result.getProperty(PROP_NAVLINKS);
        String linksHtml = HtmlUtil.space(1);
        if (links != null) {
            linksHtml = StringUtil.join(getTemplateProperty(request,
                    "ramadda.template.link.separator", ""), links);
        }
        String entryHeader = (String) result.getProperty(PROP_ENTRY_HEADER);
        if (entryHeader == null) {
            entryHeader = "";
        }
        String entryBreadcrumbs =
            (String) result.getProperty(PROP_ENTRY_BREADCRUMBS);
        if (entryBreadcrumbs == null) {
            entryBreadcrumbs = "";
        }
        List   sublinks     = (List) result.getProperty(PROP_NAVSUBLINKS);
        String sublinksHtml = "";
        if (sublinks != null) {
            String sublinksTemplate = getTemplateProperty(request,
                                          "ramadda.template.sublink.wrapper",
                                          "");
            sublinksHtml = StringUtil.join(getTemplateProperty(request,
                    "ramadda.template.sublink.separator", ""), sublinks);
            sublinksHtml = sublinksTemplate.replace("${sublinks}",
                    sublinksHtml);
        }


        String favoritesWrapper = getTemplateProperty(request,
                                      "ramadda.template.favorites.wrapper",
                                      "${link}");
        String favoritesTemplate =
            getTemplateProperty(
                request, "ramadda.template.favorites",
                "<span class=\"linkslabel\">Favorites:</span>${entries}");
        String favoritesSeparator =
            getTemplateProperty(request,
                                "ramadda.template.favrorites.separator", "");

        List<FavoriteEntry> favoritesList =
            getUserManager().getFavorites(request, request.getUser());
        StringBuffer favorites = new StringBuffer();
        if (favoritesList.size() > 0) {
            List favoriteLinks = new ArrayList();
            for (FavoriteEntry favorite : favoritesList) {
                Entry entry = favorite.getEntry();
                EntryLink entryLink = getEntryManager().getAjaxLink(request,
                                          entry, entry.getLabel(), null,
                                          false);
                String link = favoritesWrapper.replace("${link}",
                                  entryLink.toString());
                favoriteLinks.add("<nobr>" + link + "<nobr>");
            }
            favorites.append(favoritesTemplate.replace("${entries}",
                    StringUtil.join(favoritesSeparator, favoriteLinks)));
        }

        List<Entry> cartEntries = getUserManager().getCart(request);
        if (cartEntries.size() > 0) {
            String cartTemplate = getTemplateProperty(request,
                                      "ramadda.template.cart",
                                      "<b>Cart:<b><br>${entries}");
            List cartLinks = new ArrayList();
            for (Entry entry : cartEntries) {
                EntryLink entryLink = getEntryManager().getAjaxLink(request,
                                          entry, entry.getLabel(), null,
                                          false);
                String link = favoritesWrapper.replace("${link}",
                                  entryLink.toString());
                cartLinks.add("<nobr>" + link + "<nobr>");
            }
            favorites.append(HtmlUtil.br());
            favorites.append(cartTemplate.replace("${entries}",
                    StringUtil.join(favoritesSeparator, cartLinks)));
        }


        String content = new String(result.getContent());
        if (sessionMessage != null) {
            content = showDialogNote(sessionMessage) + content;
        }

        String head =
            "<script type=\"text/javascript\" src=\"${root}/shadowbox/adapter/shadowbox-base.js\"></script>\n<script type=\"text/javascript\" src=\"${root}/shadowbox/shadowbox.js\"></script>\n<script type=\"text/javascript\">\nShadowbox.loadSkin('classic', '${root}/shadowbox/skin'); \nShadowbox.loadLanguage('en', '${root}/shadowbox/lang');\nShadowbox.loadPlayer(['img', 'qt'], '${root}/shadowbox/player'); \nwindow.onload = Shadowbox.init;\n</script>";

        head = (String) result.getProperty(PROP_HTML_HEAD);
        if (head == null) {
            head = "";
        }
        String logoImage = getProperty(PROP_LOGO_IMAGE, "").trim();
        if (logoImage.length() == 0) {
            logoImage = "${root}/images/logo.png";
        }
        String   html   = template;
        String[] macros = new String[] {
            MACRO_LOGO_URL, getProperty(PROP_LOGO_URL, ""), MACRO_LOGO_IMAGE,
            logoImage, MACRO_HEADER_IMAGE, iconUrl(ICON_HEADER),
            MACRO_HEADER_TITLE,
            getProperty(PROP_REPOSITORY_NAME, "Repository"), MACRO_USERLINK,
            getUserManager().getUserLinks(request), MACRO_REPOSITORY_NAME,
            getProperty(PROP_REPOSITORY_NAME, "Repository"), MACRO_FOOTER,
            getProperty(PROP_HTML_FOOTER, BLANK), MACRO_TITLE,
            result.getTitle(), MACRO_BOTTOM, result.getBottomHtml(),
            MACRO_LINKS, linksHtml, MACRO_CONTENT, content + jsContent,
            MACRO_FAVORITES, favorites.toString(), MACRO_ENTRY_HEADER,
            entryHeader, MACRO_ENTRY_BREADCRUMBS, entryBreadcrumbs,
            MACRO_HEADFINAL, head, MACRO_ROOT, getUrlBase(),
        };





        for (int i = 0; i < macros.length; i += 2) {
            html = html.replace("${" + macros[i] + "}", macros[i + 1]);
        }

        if (sublinksHtml.length() > 0) {
            html = StringUtil.replace(html, "${sublinks}", sublinksHtml);
        } else {
            html = StringUtil.replace(html, "${sublinks}", BLANK);
        }
        html = translate(request, html);

        result.setContent(html.getBytes());

    }


    /**
     * _more_
     *
     * @param template _more_
     * @param ignoreErrors _more_
     *
     * @return _more_
     */
    public String processTemplate(String template, boolean ignoreErrors) {
        List<String> toks   = StringUtil.splitMacros(template);
        StringBuffer result = new StringBuffer();
        if (toks.size() > 0) {
            result.append(toks.get(0));
            for (int i = 1; i < toks.size(); i++) {
                if (2 * (i / 2) == i) {
                    result.append(toks.get(i));
                } else {
                    String prop = getRepository().getProperty(toks.get(i),
                                      (String) null);
                    if (prop == null) {
                        if (ignoreErrors) {
                            prop = "${" + toks.get(i) + "}";
                        } else {
                            throw new IllegalArgumentException(
                                "Could not find property:" + toks.get(i)
                                + ":");
                        }
                    }
                    if (prop.startsWith("bsf:")) {
                        prop = new String(
                            XmlUtil.decodeBase64(prop.substring(4)));
                    }
                    result.append(prop);
                }
            }
        }
        return result.toString();
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
        Properties tmpMap;
        Properties map = (Properties) languageMap.get("default");
        if (map == null) {
            map = new Properties();
        }
        tmpMap = (Properties) languageMap.get(getProperty(PROP_LANGUAGE,
                BLANK));
        if (tmpMap != null) {
            map.putAll(tmpMap);
        }
        tmpMap = (Properties) languageMap.get(language);

        if (tmpMap != null) {
            map.putAll(tmpMap);
        }

        if (phraseMap == null) {
            String phrases = getProperty(PROP_ADMIN_PHRASES, (String) null);
            if (phrases != null) {
                Object[] result = parsePhrases(phrases);
                phraseMap = (Properties) result[2];
            }
        }

        if (phraseMap != null) {
            map.putAll(phraseMap);
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
     * @return _more_
     */
    public boolean cacheResources() {
        String test = (String) properties.get("ramadda.cacheresources");
        if (test == null) {
            return true;
        }
        if (test != null) {
            return test.equals("true");
        }
        return true;
    }





    /**
     * _more_
     *
     * @return _more_
     */
    private List<HtmlTemplate> getTemplates() {
        List<HtmlTemplate> theTemplates = templates;
        if (theTemplates == null) {
            theTemplates = new ArrayList<HtmlTemplate>();
            for (String path : StringUtil.split(
                    getProperty(
                        PROP_HTML_TEMPLATES,
                        "%resourcedir%/template.html"), ";", true, true)) {
                path = getStorageManager().localizePath(path);
                try {
                    String resource =
                        getStorageManager().readSystemResource(path);
                    HtmlTemplate template = new HtmlTemplate(this, path,
                                                resource);
                    theTemplates.add(template);
                } catch (Exception exc) {
                    //noop
                }
            }
            if (cacheResources()) {
                templates = theTemplates;
            }
        }
        return theTemplates;
    }

    /** _more_ */
    private List<HtmlTemplate> templates;

    /**
     * _more_
     *
     * @return _more_
     */
    public List<TwoFacedObject> getTemplateSelectList() {
        List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
        for (HtmlTemplate template : getTemplates()) {
            tfos.add(new TwoFacedObject(template.getName(),
                                        template.getId()));
        }
        return tfos;

    }



    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public HtmlTemplate getTemplate(Request request) {
        List<HtmlTemplate> theTemplates = getTemplates();
        for (HtmlTemplate template : theTemplates) {
            if (request == null) {
                return template;
            }
            if (template.isTemplateFor(request)) {
                return template;
            }
        }
        return theTemplates.get(0);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public String getTemplateProperty(Request request, String name,
                                      String dflt) {
        return getTemplate(request).getTemplateProperty(name, dflt);
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public String getProperty(String name) {
        return getPropertyValue(name, true);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param checkDb _more_
     *
     * @return _more_
     */
    public String getPropertyValue(String name, boolean checkDb) {
        if (systemEnv == null) {
            systemEnv = System.getenv();
        }
        String prop = null;
        if ( !cacheResources()) {
            try {
                properties.load(
                    IOUtil.getInputStream(
                        "/ucar/unidata/repository/resources/repository.properties",
                        getClass()));
            } catch (Exception exc) {}
        }


        String override = "override." + name;
        //Check if there is an override 
        if (prop == null) {
            prop = (String) cmdLineProperties.get(override);
        }

        if (prop == null) {
            prop = (String) properties.get(override);
        }

        //Then look at the command line
        if (prop == null) {
            prop = (String) cmdLineProperties.get(name);
        }


        //Then the  database properties  first
        if (checkDb && (prop == null)) {
            prop = (String) dbProperties.get(name);
        }


        //then the  repository.properties first
        if (prop == null) {
            prop = (String) properties.get(name);
        }

        //Then look at system properties
        if (prop == null) {
            prop = System.getProperty(name);
        }

        //Then env vars
        if (prop == null) {
            prop = systemEnv.get(name);
        }

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
        return getPropertyValue(name, dflt, true);
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     * @param checkDb _more_
     *
     * @return _more_
     */
    public String getPropertyValue(String name, String dflt,
                                   boolean checkDb) {
        String prop = getPropertyValue(name, checkDb);
        if (prop != null) {
            return prop;
        }
        return dflt;
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
        return dflt;
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
        return dflt;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public long getProperty(String name, long dflt) {
        String prop = getProperty(name);
        if (prop != null) {
            return new Long(prop).longValue();
        }
        return dflt;
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public double getProperty(String name, double dflt) {
        String prop = getProperty(name);
        if (prop != null) {
            return new Double(prop).doubleValue();
        }
        return dflt;
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
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
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
        String sql = getStorageManager().readUncheckedSystemResource(
                         getProperty(PROP_DB_SCRIPT));
        sql = getDatabaseManager().convertSql(sql);

        getDatabaseManager().loadSql(sql, true, false);

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
                        "ucar.unidata.repository.type.GenericTypeHandler"));
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


        getDatabaseManager().initComplete();


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
     * @param request _more_
     * @param propName _more_
     *
     * @throws Exception _more_
     */
    protected void writeGlobal(Request request, String propName)
            throws Exception {
        writeGlobal(propName,
                    request.getString(propName, getProperty(propName, "")));
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
        phraseMap = null;
    }



    /**
     *  _more_
     *
     *  @param request _more_
     * @param state _more_
     *
     *  @return _more_
     *
     *  @throws Exception _more_
     */
    public List<Link> getOutputLinks(Request request,
                                     OutputHandler.State state)
            throws Exception {
        List<Link> links = new ArrayList<Link>();
        for (OutputHandler outputHandler : outputHandlers) {
            outputHandler.getEntryLinks(request, state, links);
        }
        List<Link> okLinks = new ArrayList<Link>();


        for (Link link : links) {
            OutputType outputType = link.getOutputType();
            if (isOutputTypeOK(outputType)) {
                okLinks.add(link);
            }
        }
        return okLinks;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Link> getLinksForHeader(Request request,
                                        OutputHandler.State state)
            throws Exception {
        List<Link> links   = getOutputLinks(request, state);

        List<Link> okLinks = new ArrayList<Link>();

        for (Link link : links) {
            if (link.isType(OutputType.TYPE_HTML)) {
                okLinks.add(link);
            }
        }
        return okLinks;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<Link> getLinksForToolbar(Request request,
                                         OutputHandler.State state)
            throws Exception {
        List<Link> links   = getOutputLinks(request, state);
        List<Link> okLinks = new ArrayList<Link>();
        for (Link link : links) {
            if (link.isType(OutputType.TYPE_ACTION)
                    || link.isType(OutputType.TYPE_NONHTML)) {
                okLinks.add(link);
            }
        }
        return okLinks;
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<OutputType> getOutputTypes() throws Exception {
        List<OutputType> allTypes = new ArrayList<OutputType>();
        for (OutputHandler outputHandler : outputHandlers) {
            allTypes.addAll(outputHandler.getTypes());
        }
        return allTypes;
    }


    /**
     * _more_
     *
     * @param outputType _more_
     *
     * @return _more_
     */
    public boolean isOutputTypeOK(OutputType outputType) {
        String prop = getProperty(outputType.getId() + ".ok");
        if ((prop == null) || prop.equals("true")) {
            return true;
        }
        return false;
    }

    /**
     * _more_
     *
     * @param outputType _more_
     * @param ok _more_
     *
     * @throws Exception _more_
     */
    public void setOutputTypeOK(OutputType outputType, boolean ok)
            throws Exception {
        String prop = outputType.getId() + ".ok";
        writeGlobal(prop, "" + ok);
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List<OutputHandler> getOutputHandlers() {
        return new ArrayList<OutputHandler>(outputHandlers);
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public HtmlOutputHandler getHtmlOutputHandler() throws Exception {
        return (HtmlOutputHandler) getOutputHandler(
            OutputHandler.OUTPUT_HTML);
    }


    /**
     * _more_
     *
     * @param outputType _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public OutputHandler getOutputHandler(OutputType outputType)
            throws Exception {
        if ( !isOutputTypeOK(outputType)) {
            return null;
        }
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
        OutputHandler handler = getOutputHandler(request.getOutput());
        if (handler != null) {
            return handler;
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
        if ((type == null) || (type.length() == 0)) {
            type = OutputHandler.OUTPUT_HTML.getId();
        }
        OutputType output = new OutputType("", type, OutputType.TYPE_HTML);
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

    /**
     * _more_
     *
     * @return _more_
     */
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
    public List<TypeHandler> getTypeHandlers() throws Exception {
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
    public TypeHandler getTypeHandler(Request request) throws Exception {
        if (request != null) {
            String type = request.getString(ARG_TYPE,
                                            TypeHandler.TYPE_ANY).trim();
            return getTypeHandler(type, false, true);
        } else {
            return getTypeHandler(TypeHandler.TYPE_FILE, false, true);
        }
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
    public TypeHandler getTypeHandler(String type) throws Exception {
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
    public TypeHandler getTypeHandler(String type,
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
    public Result processPing(Request request) throws Exception {
        if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
            Document resultDoc = XmlUtil.makeDocument();
            Element resultRoot = XmlUtil.create(resultDoc, TAG_RESPONSE,
                                     null, new String[] { ATTR_CODE,
                    "ok" });
            String xml = XmlUtil.toString(resultRoot);
            return new Result(xml, MIME_XML);
        }
        StringBuffer sb = new StringBuffer("OK");
        return new Result("", sb);
    }


    /** _more_ */
    int fileCnt = 0;

    /** _more_ */
    Object MUTEX = new Object();






    /** _more_ */
    byte[] buffer = new byte[1048748];

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processTestMemory(Request request) throws Exception {
        return new Result(
            BLANK, new BufferedInputStream(new ByteArrayInputStream(buffer)),
            "application/x-binary");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDescription() {
        return getProperty(PROP_REPOSITORY_DESCRIPTION, "");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ServerInfo getServerInfo() {
        int sslPort = getHttpsPort();
        return new ServerInfo(
            getHostname(), getPort(), sslPort, getUrlBase(),
            getProperty(PROP_REPOSITORY_NAME, "Repository"),
            getDescription(), getProperty(PROP_ADMIN_EMAIL, ""),
            getRegistryManager().isEnabledAsServer(), false);
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
    public Result processInfo(Request request) throws Exception {
        if (request.getString(ARG_RESPONSE, "").equals(RESPONSE_XML)) {
            Document doc  = XmlUtil.makeDocument();
            Element  info = getServerInfo().toXml(this, doc);
            info.setAttribute(ATTR_CODE, CODE_OK);
            String xml = XmlUtil.toString(info);
            return new Result(xml, MIME_XML);
        }
        StringBuffer sb = new StringBuffer("");

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
    public Result processHelp(Request request) throws Exception {
        String path = request.getRequestPath();
        path = path.substring((getUrlBase() + "/help").length());
        if (path.length() == 0) {
            path = "/index.html";
        }
        if (path.equals("/")) {
            path = "/index.html";
        }
        path = "/ucar/unidata/repository/docs/userguide/processed" + path;
        Result result = null;
        if (path.endsWith(".html")) {
            String helpText = getStorageManager().readSystemResource(path);
            //            Pattern pattern  = Pattern.compile(".*<body>(.*)</body>.*");

            //Pull out the body if we can
            Pattern pattern = Pattern.compile("(?s).*<body>(.*)</body>");
            Matcher matcher = pattern.matcher(helpText);
            if (matcher.find()) {
                helpText = matcher.group(1);
            }
            result = new Result(BLANK, new StringBuffer(helpText));
        } else {
            InputStream inputStream =
                getStorageManager().getInputStream(path);
            result = new Result(BLANK, inputStream,
                                IOUtil.getFileExtension(path));

        }
        result.setCacheOk(true);
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
    public Result processMessage(Request request) throws Exception {
        StringBuffer sb = new StringBuffer();
        request.appendMessage(sb);
        return new Result(BLANK, sb);
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result processSslRedirect(Request request) throws Exception {
        if (request.getCheckingAuthMethod()) {
            return new Result(AuthorizationMethod.AUTH_HTTP);
        }


        if (request.isAnonymous()) {
            throw new AccessException("Cannot access data", request);
        }
        String url = request.getString(ARG_REDIRECT, "");
        url = new String(XmlUtil.decodeBase64(url));
        return new Result(url);
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
    public List getListLinks(Request request, String what,
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
     * @param urls _more_
     *
     * @return _more_
     */
    public List getSubNavLinks(Request request, RequestUrl[] urls) {
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
    public List getSubNavLinks(Request request, RequestUrl[] urls,
                               String arg) {
        List   links = new ArrayList();
        String type  = request.getRequestPath();
        String onLinkTemplate = getTemplateProperty(request,
                                    "ramadda.template.sublink.on", "");
        String offLinkTemplate = getTemplateProperty(request,
                                     "ramadda.template.sublink.off", "");
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
    public List getNavLinks(Request request) {
        List    links   = new ArrayList();
        boolean isAdmin = false;
        if (request != null) {
            User user = request.getUser();
            isAdmin = user.getAdmin();
        }

        String template = getTemplateProperty(request,
                              "ramadda.template.link.wrapper", "");

        for (ApiMethod apiMethod : topLevelMethods) {
            if (apiMethod.getMustBeAdmin() && !isAdmin) {
                continue;
            }
            if ( !apiMethod.getIsTopLevel()) {
                continue;
            }
            String url;
            if (apiMethod == homeApi) {
                url = fileUrl(apiMethod.getRequest());
            } else {
                url = request.url(apiMethod.getUrl());
            }
            String html = template.replace("${url}", url);
            html = html.replace("${label}", apiMethod.getName());
            html = html.replace("${topgroup}",
                                getEntryManager().getTopGroup().getName());
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
            return request.get(ARG_SKIP, 0)
                   + request.get(ARG_MAX, DB_MAX_ROWS);
        }
        return request.get(ARG_MAX, DB_MAX_ROWS);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Request getTmpRequest() throws Exception {
        Request request = new Request(getRepository(), "", new Hashtable());
        request.setUser(getUserManager().getAnonymousUser());
        return request;
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
        if (msg == null) {
            return null;
        }
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
        if (msg == null) {
            return null;
        }
        return msg(msg) + ":" + HtmlUtil.space(1);
    }

    /**
     * _more_
     *
     * @param h _more_
     *
     * @return _more_
     */
    public static String msgHeader(String h) {
        return HtmlUtil.div(msg(h), HtmlUtil.cssClass("pageheading"));
    }






    /**
     * _more_
     *
     * @param sb _more_
     * @param date _more_
     * @param url _more_
     * @param dayLinks _more_
     */
    public void createMonthNav(StringBuffer sb, Date date, String url,
                               Hashtable dayLinks) {

        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        cal.setTime(date);
        int[] theDate  = CalendarOutputHandler.getDayMonthYear(cal);
        int   theDay   = cal.get(cal.DAY_OF_MONTH);
        int   theMonth = cal.get(cal.MONTH);
        int   theYear  = cal.get(cal.YEAR);
        while (cal.get(cal.DAY_OF_MONTH) > 1) {
            cal.add(cal.DAY_OF_MONTH, -1);
        }
        GregorianCalendar prev = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        prev.setTime(date);
        prev.add(cal.MONTH, -1);
        GregorianCalendar next = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        next.setTime(date);
        next.add(cal.MONTH, 1);

        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE,
                                HtmlUtil.attrs(HtmlUtil.ATTR_BORDER, "1",
                                    HtmlUtil.ATTR_CELLSPACING, "0",
                                    HtmlUtil.ATTR_CELLPADDING, "0")));
        String[] dayNames = {
            "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"
        };
        String prevUrl =
            HtmlUtil.space(1)
            + HtmlUtil.href(url + "&"
                            + CalendarOutputHandler.getUrlArgs(prev), "&lt;");
        String nextUrl =
            HtmlUtil.href(url + "&" + CalendarOutputHandler.getUrlArgs(next),
                          HtmlUtil.ENTITY_GT) + HtmlUtil.space(1);
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TR,
                                HtmlUtil.attr(HtmlUtil.ATTR_VALIGN,
                                    HtmlUtil.VALUE_TOP)));
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TD,
                                HtmlUtil.attrs(HtmlUtil.ATTR_COLSPAN, "7",
                                    HtmlUtil.ATTR_ALIGN,
                                    HtmlUtil.VALUE_CENTER,
                                    HtmlUtil.ATTR_CLASS,
                                    "calnavmonthheader")));


        sb.append(HtmlUtil.open(HtmlUtil.TAG_TABLE,
                                HtmlUtil.attrs(HtmlUtil.ATTR_CELLSPACING,
                                    "0", HtmlUtil.ATTR_CELLPADDING, "0",
                                    HtmlUtil.ATTR_WIDTH, "100%")));
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TR));
        sb.append(HtmlUtil.col(prevUrl,
                               HtmlUtil.attrs(HtmlUtil.ATTR_WIDTH, "1",
                                   HtmlUtil.ATTR_CLASS,
                                   "calnavmonthheader")));
        sb.append(
            HtmlUtil.col(
                DateUtil.MONTH_NAMES[cal.get(cal.MONTH)] + HtmlUtil.space(1)
                + theYear, HtmlUtil.attr(
                    HtmlUtil.ATTR_CLASS, "calnavmonthheader")));



        sb.append(HtmlUtil.col(nextUrl,
                               HtmlUtil.attrs(HtmlUtil.ATTR_WIDTH, "1",
                                   HtmlUtil.ATTR_CLASS,
                                   "calnavmonthheader")));
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TR));
        sb.append(HtmlUtil.open(HtmlUtil.TAG_TR));
        for (int colIdx = 0; colIdx < 7; colIdx++) {
            sb.append(HtmlUtil.col(dayNames[colIdx],
                                   HtmlUtil.attrs(HtmlUtil.ATTR_WIDTH, "14%",
                                       HtmlUtil.ATTR_CLASS,
                                       "calnavdayheader")));
        }
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TR));
        int startDow = cal.get(cal.DAY_OF_WEEK);
        while (startDow > 1) {
            cal.add(cal.DAY_OF_MONTH, -1);
            startDow--;
        }
        for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
            sb.append(HtmlUtil.open(HtmlUtil.TAG_TR,
                                    HtmlUtil.attrs(HtmlUtil.ATTR_VALIGN,
                                        HtmlUtil.VALUE_TOP)));
            for (int colIdx = 0; colIdx < 7; colIdx++) {
                int    thisDay   = cal.get(cal.DAY_OF_MONTH);
                int    thisMonth = cal.get(cal.MONTH);
                int    thisYear  = cal.get(cal.YEAR);
                String dayClass  = "calnavday";
                if (thisMonth != theMonth) {
                    dayClass = "calnavoffday";
                } else if ((theMonth == thisMonth) && (theYear == thisYear)
                           && (theDay == thisDay)) {
                    dayClass = "calnavtheday";
                }
                String content;
                if (dayLinks != null) {
                    String key = thisYear + "/" + thisMonth + "/" + thisDay;
                    if (dayLinks.get(key) != null) {
                        content = HtmlUtil.href(url + "&"
                                + CalendarOutputHandler.getUrlArgs(cal), ""
                                    + thisDay);
                        dayClass = "calnavtheday";
                    } else {
                        content  = "" + thisDay;
                        dayClass = "calnavday";
                    }
                } else {
                    content = HtmlUtil.href(
                        url + "&" + CalendarOutputHandler.getUrlArgs(cal),
                        "" + thisDay);
                }

                sb.append(HtmlUtil.col(content, HtmlUtil.cssClass(dayClass)));
                sb.append("\n");
                cal.add(cal.DAY_OF_MONTH, 1);
            }
            if (cal.get(cal.MONTH) > theMonth) {
                break;
            }
            if (cal.get(cal.YEAR) > theYear) {
                break;
            }
        }
        sb.append(HtmlUtil.close(HtmlUtil.TAG_TABLE));

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
    public Result processModelProducts(Request request) throws Exception {
        Date date = request.get(ARG_DATE, new Date());
        //        System.err.println(date);
        StringBuffer sb = new StringBuffer();
        request.remove(ARG_DATE);
        Hashtable dayLinks = new Hashtable();
        dayLinks.put("2008/10/2", "");
        dayLinks.put("2008/10/3", "");
        createMonthNav(sb, date, request.getUrl(), dayLinks);
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
        return getResource(id, false);
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param ignoreErrors _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getResource(String id, boolean ignoreErrors)
            throws Exception {
        String resource = (String) resources.get(id);
        if (resource != null) {
            return resource;
        }
        String fromProperties = getProperty(id);
        if (fromProperties != null) {
            List<String> paths = getResourcePaths(id);
            for (String path : paths) {
                try {
                    resource = getStorageManager().readSystemResource(path);
                } catch (Exception exc) {
                    //noop
                }
                if (resource != null) {
                    break;
                }
            }
        } else {
            try {
                resource = getStorageManager().readSystemResource(
                    getStorageManager().localizePath(id));
            } catch (Exception exc) {
                if ( !ignoreErrors) {
                    throw exc;
                }
            }
        }
        if (cacheResources() && (resource != null)) {
            resources.put(id, resource);
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
        return makeTypeSelect(request, includeAny, "", false, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param includeAny _more_
     * @param selected _more_
     * @param checkAddOk _more_
     * @param exclude _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String makeTypeSelect(Request request, boolean includeAny,
                                 String selected, boolean checkAddOk,
                                 HashSet<String> exclude)
            throws Exception {
        List<TypeHandler> typeHandlers = getTypeHandlers();
        List              tmp          = new ArrayList();
        for (TypeHandler typeHandler : typeHandlers) {
            if (typeHandler.isAnyHandler() && !includeAny) {
                continue;
            }
            if (exclude != null) {
                if (exclude.contains(typeHandler.getType())) {
                    continue;
                }
            }
            if (checkAddOk && !typeHandler.canBeCreatedBy(request)) {
                continue;
            }
            //            System.err.println("type: " + typeHandler.getType()+" label:" + typeHandler.getLabel());
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
    public List<TypeHandler> getTypeHandlers(Request request)
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
            typeHandler.select(request,
                               SqlUtil.distinct(Tables.ENTRIES.COL_TYPE),
                               where, "");
        String[] types =
            SqlUtil.readString(getDatabaseManager().getIterator(stmt), 1);
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
        String[] types =
            SqlUtil.readString(getDatabaseManager().getIterator(stmt), 1);
        List      tmp  = new ArrayList();
        Hashtable seen = new Hashtable();
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
    public Result listTypes(Request request) throws Exception {
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
     *       SqlUtil.readString(getDatabaseManager().getIterator(stmt =
     *           typeHandler.select(request,
     *                                     SqlUtil.distinct(Tables.TAGS.COL_NAME),
     *                                     where,
     *                                     " order by " + Tables.TAGS.COL_NAME)), 1);
     *   getDatabaseManager().closeAndReleaseStatement(stmt);
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
     *       getDatabaseManager().closeAndReleaseStatement(stmt2);
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
    public Result listAssociations(Request request) throws Exception {
        return getOutputHandler(request).listAssociations(request);
    }




    /**
     * _more_
     *
     * @param suffix _more_
     *
     * @return _more_
     */
    public String getMimeTypeFromSuffix(String suffix) {
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
        localFilePaths = (List<File>) Misc.toList(
            IOUtil.toFiles(
                (List<String>) StringUtil.split(
                    getProperty(PROP_LOCALFILEPATHS, ""), "\n", true, true)));
    }

    /**
     * _more_
     *
     * @return _more_
     */
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
    public String getQueryOrderAndLimit(Request request, boolean addOrderBy) {
        return getQueryOrderAndLimit(request, addOrderBy, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param addOrderBy _more_
     * @param forEntry _more_
     *
     * @return _more_
     */
    public String getQueryOrderAndLimit(Request request, boolean addOrderBy,
                                        Entry forEntry) {

        List<Metadata> metadataList = null;

        Metadata       sortMetadata = null;
        if ((forEntry != null) && !request.exists(ARG_ORDERBY)) {
            try {
                metadataList = getMetadataManager().findMetadata(forEntry,
                        ContentMetadataHandler.TYPE_SORT, true);
                if ((metadataList != null) && (metadataList.size() > 0)) {
                    sortMetadata = metadataList.get(0);
                }
            } catch (Exception ignore) {}
        }

        String  order     = " DESC ";
        boolean haveOrder = request.exists(ARG_ASCENDING);
        String  by        = null;
        int     max       = request.get(ARG_MAX, DB_MAX_ROWS);
        if (sortMetadata != null) {
            haveOrder = true;
            if (Misc.equals(sortMetadata.getAttr2(), "true")) {
                order = " ASC ";
            } else {
                order = " DESC ";
            }
            by = sortMetadata.getAttr1();
            /*            String tmp = sortMetadata.getAttr3();
            if(tmp!=null && tmp.length()>0) {
                max = Integer.parseInt(tmp.trim());
                }*/
        } else {
            by = request.getString(ARG_ORDERBY, (String) null);
            if (request.get(ARG_ASCENDING, false)) {
                order = " ASC ";
            }
        }

        String limitString = BLANK;





        limitString =
            getDatabaseManager().getLimitString(request.get(ARG_SKIP, 0),
                max);

        String orderBy = BLANK;
        if (addOrderBy) {
            orderBy = " ORDER BY " + Tables.ENTRIES.COL_FROMDATE + order;
        }
        if (by != null) {
            if (by.equals("fromdate")) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_FROMDATE + order;
            } else if (by.equals("todate")) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_TODATE + order;
            } else if (by.equals("createdate")) {
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_CREATEDATE
                          + order;
            } else if (by.equals("name")) {
                if ( !haveOrder) {
                    order = " ASC ";
                }
                orderBy = " ORDER BY " + Tables.ENTRIES.COL_NAME + order;
            }
        }

        //        System.err.println(orderBy);
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
    public String getFieldDescription(String fieldValue, String namesFile)
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
    public Properties getFieldProperties(String namesFile) throws Exception {
        if (namesFile == null) {
            return null;
        }
        Properties names = (Properties) namesHolder.get(namesFile);
        if (names == null) {
            try {
                names = new Properties();
                InputStream s = getStorageManager().getInputStream(namesFile);
                names.load(s);
                namesHolder.put(namesFile, names);
            } catch (Exception exc) {
                getLogManager().logError("err:" + exc, exc);
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
    public String getFieldDescription(String fieldValue, String namesFile,
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
     * @param request _more_
     * @param tag _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getTagLinks(Request request, String tag) throws Exception {
        return BLANK;

    }




    /**
     * _more_
     *
     * @param entries _more_
     */
    public void checkNewEntries(List<Entry> entries) {
        getMonitorManager().checkNewEntries(entries);
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
        String call = HtmlUtil.call(
                          "selectDate", HtmlUtil.comma(
                              HtmlUtil.squote(divName), "document.forms['"
                              + formName + "']."
                              + fieldName, HtmlUtil.squote(
                                  anchorName), HtmlUtil.squote(
                                  "yyyy-MM-dd"))) + "return false;";
        return HtmlUtil
            .href("#", HtmlUtil
                .img(iconUrl(ICON_CALENDAR), " Choose date", HtmlUtil
                    .attr(HtmlUtil.ATTR_BORDER, "0")), HtmlUtil
                        .onMouseClick(call) + HtmlUtil
                        .attrs(HtmlUtil.ATTR_NAME, anchorName, HtmlUtil
                            .ATTR_ID, anchorName)) + HtmlUtil
                                .div("", HtmlUtil
                                    .attrs(HtmlUtil.ATTR_ID, divName, HtmlUtil
                                        .ATTR_STYLE, "position:absolute;visibility:hidden;background-color:white;layer-background-color:white;"));
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
        return makeDateInput(request, name, formName, date, null);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param name _more_
     * @param formName _more_
     * @param date _more_
     * @param timezone _more_
     *
     * @return _more_
     */
    public String makeDateInput(Request request, String name,
                                String formName, Date date, String timezone) {
        String dateHelp = "e.g., yyyy-mm-dd,  now, -1 week, +3 days, etc.";
        String           timeHelp   = "hh:mm:ss Z, e.g. 20:15:00 MST";

        String           dateArg    = request.getString(name, "");
        String           timeArg    = request.getString(name + ".time", "");
        String           dateString = ((date == null)
                                       ? dateArg
                                       : dateSdf.format(date));
        SimpleDateFormat timeFormat = ((timezone == null)
                                       ? timeSdf
                                       : getSDF("HH:mm:ss z", timezone));
        String           timeString = ((date == null)
                                       ? timeArg
                                       : timeFormat.format(date));

        return HtmlUtil.input(
            name, dateString,
            HtmlUtil.SIZE_10
            + HtmlUtil.title(dateHelp)) + getCalendarSelector(formName, name)
                                        + " T:"
                                        + HtmlUtil.input(
                                            name + ".time", timeString,
                                            HtmlUtil.SIZE_15
                                            + HtmlUtil.attr(
                                                HtmlUtil.ATTR_TITLE,
                                                    timeHelp));
    }


    /** _more_ */
    private static final String MAP_JS_MICROSOFT =
        "http://dev.virtualearth.net/mapcontrol/mapcontrol.ashx?v=6.1";

    /** _more_ */
    private static final String MAP_ID_MICROSOFT = "microsoft";

    /** _more_ */
    private static final String MAP_JS_YAHOO =
        "http://api.maps.yahoo.com/ajaxymap?v=3.8&appid=idvunidata";

    /** _more_ */
    private static final String MAP_ID_YAHOO = "yahoo";


    /**
     * _more_
     *
     * @param request _more_
     * @param mapVarName _more_
     * @param sb _more_
     * @param width _more_
     * @param height _more_
     * @param normalControls _more_
     *
     * @return _more_
     */
    public String initMap(Request request, String mapVarName,
                          StringBuffer sb, int width, int height,
                          boolean normalControls) {
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
        String mapProvider = MAP_ID_MICROSOFT;
        String mapJS       = MAP_JS_MICROSOFT;
        //        googleMapsKey = "ABQIAAAA-JXA0-ozvUKU42oQp1aOZxT2yXp_ZAY8_ufC3CFXhHIE1NvwkxSig5NmAvzXxoX1Ly0QJZMRxtiLIg";        
        String googleKeys = getProperty(PROP_GOOGLEAPIKEYS, "");
        googleMapsKey = null;
        for (String line : (List<String>) StringUtil.split(googleKeys, "\n",
                true, true)) {
            if (line.length() == 0) {
                continue;
            }
            String[] toks = StringUtil.split(line, ":", 2);
            if (toks == null) {
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


        if (request.getExtraProperty("initmap") == null) {
            sb.append(HtmlUtil.importJS(mapJS));
            sb.append(HtmlUtil.importJS(fileUrl("/mapstraction.js")));
            sb.append(HtmlUtil.importJS(fileUrl("/mymap.js")));
            request.putExtraProperty("initmap", "");
        }


        sb.append(HtmlUtil.div("",
                               HtmlUtil.style("width:" + width
                                   + "px; height:" + height + "px") + " "
                                       + HtmlUtil.id(mapVarName)));
        sb.append(HtmlUtil.script(mapVarName + "="
                                  + HtmlUtil.call("MapInitialize",
                                      normalControls + ","
                                      + HtmlUtil.squote(mapProvider) + ","
                                      + HtmlUtil.squote(mapVarName)) + ";"));
        return "";
    }






    /**
     * _more_
     *
     * @param link _more_
     * @param menuContents _more_
     *
     * @return _more_
     */
    public String makePopupLink(String link, String menuContents) {
        return makePopupLink(link, menuContents, false, false);
    }


    /**
     * _more_
     *
     * @param link _more_
     * @param menuContents _more_
     * @param makeClose _more_
     * @param alignLeft _more_
     *
     * @return _more_
     */
    public String makePopupLink(String link, String menuContents,
                                boolean makeClose, boolean alignLeft) {
        String compId   = "menu_" + HtmlUtil.blockCnt++;
        String linkId   = "menulink_" + HtmlUtil.blockCnt++;
        String contents = makePopupDiv(menuContents, compId, makeClose);
        String onClick = HtmlUtil.onMouseClick(HtmlUtil.call("showPopup",
                             HtmlUtil.comma(new String[] { "event",
                HtmlUtil.squote(linkId), HtmlUtil.squote(compId), (alignLeft
                ? "1"
                : "0") })));
        String href = HtmlUtil.href("javascript:noop();", link,
                                    onClick + HtmlUtil.id(linkId));
        return href + contents;
    }




    /**
     * _more_
     *
     * @param contents _more_
     * @param compId _more_
     * @param makeClose _more_
     *
     * @return _more_
     */
    public String makePopupDiv(String contents, String compId,
                               boolean makeClose) {
        StringBuffer menu = new StringBuffer();
        if (makeClose) {
            String cLink =
                HtmlUtil.jsLink(HtmlUtil.onMouseClick("hidePopupObject();"),
                                HtmlUtil.img(iconUrl(ICON_CLOSE)), "");
            contents = cLink + HtmlUtil.br() + contents;
        }

        menu.append(HtmlUtil.div(contents,
                                 HtmlUtil.id(compId)
                                 + HtmlUtil.cssClass("popup")));
        return menu.toString();
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
    public static String makeOkCancelForm(Request request, RequestUrl url,
                                          String okArg, String extra) {
        StringBuffer fb = new StringBuffer();
        fb.append(request.form(url));
        fb.append(extra);
        String okButton     = HtmlUtil.submit("OK", okArg);
        String cancelButton = HtmlUtil.submit("Cancel", Constants.ARG_CANCEL);
        String buttons      = RepositoryUtil.buttons(okButton, cancelButton);
        fb.append(buttons);
        fb.append(HtmlUtil.formClose());
        return fb.toString();
    }




}

