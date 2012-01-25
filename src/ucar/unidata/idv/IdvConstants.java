/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

/**
 * Class for IDV related constants.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.111 $ $Date: 2007/06/11 11:32:17 $
 */

package ucar.unidata.idv;


import ucar.unidata.data.DataSelection;
import ucar.unidata.idv.control.GridDisplayControl;
import ucar.unidata.util.PatternFileFilter;



/**
 * This holds a variety of constants used throughout the IDV
 *
 *
 * @author IDV development team
 */

public interface IdvConstants {

    /** Where to look for javahelp */
    public static final String DEFAULT_DOCPATH = "/auxdata/docs/userguide";

    /** File suffix for rbi files */
    public static final String SUFFIX_RBI = ".rbi";

    /** File suffix for jnlp files */
    public static final String SUFFIX_JNLP = ".jnlp";

    /** File suffix for shell files */
    public static final String SUFFIX_SH = ".sh";

    /** File suffix for windows bat files */
    public static final String SUFFIX_BAT = ".bat";

    /** File suffix for bundle files files */
    public static final String SUFFIX_XIDV = ".xidv";

    /** File suffix for bundle files files */
    public static final String SUFFIX_ZIDV = ".zidv";

    /** File suffix for xml files */
    public static final String SUFFIX_XML = ".xml";

    /** File suffix for event capture files files */
    public static final String SUFFIX_CPT = ".cpt";

    /** File suffix for image generationfiles */
    public static final String SUFFIX_ISL = ".isl";

    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_XIDV =
        new PatternFileFilter("(.+\\.xidv$)", "IDV Bundles (*.xidv)",
                              SUFFIX_XIDV);

    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_XIDVZIDV =
        new PatternFileFilter("(.+\\.xidv$|.+\\.zidv$)",
                              "IDV Bundles (*.xidv,*.zidv)", SUFFIX_XIDV);

    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_ZIDV =
        new PatternFileFilter("(.+\\.zidv$)", "Zipped IDV Bundles (*.zidv)",
                              SUFFIX_ZIDV);

    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_ISL =
        new PatternFileFilter("(.+\\.isl$)", "IDV Scripting Files (*.isl)",
                              SUFFIX_ISL);

    /** File filter used for bundle files */
    public static final PatternFileFilter FILTER_JNLP =
        new PatternFileFilter("(.+\\.jnlp$)", "JNLP Files (*.jnlp)",
                              SUFFIX_JNLP);


    /** File filter used for xml files */
    public static final PatternFileFilter FILTER_XML =
        new PatternFileFilter(".+\\.xml$", "XML Files (*.xml)", SUFFIX_XML);


    /** File filter used for event capture files */
    public static final PatternFileFilter FILTER_CPT =
        new PatternFileFilter(".+\\.cpt",
                              "Collaboration Capture Files (*.cpt)",
                              SUFFIX_CPT);


    /** Command line argument for overriding current time */
    public static final String ARG_CURRENTTIME = "-currenttime";



    /** Command line argument for recording the missing Msg translation messages */
    public static final String ARG_MSG_RECORD = "-recordmessages";

    /** Command line argument for debugging the missing Msg translation messages */
    public static final String ARG_MSG_DEBUG = "-debugmessages";

    /** is isl scripting interactive or batch */
    public static final String ARG_ISLINTERACTIVE = "-islinteractive";

    /** base64 isl encoding flag */
    public static final String ARG_B64ISL = "-b64isl";

    /** an isl file */
    public static final String ARG_ISLFILE = "-islfile";

    /** argument to list out the resource types */
    public static final String ARG_LISTRESOURCES = "-listresources";


    /** -setfiles argument */
    public static final String ARG_SETFILES = "-setfiles";


    /** Command line argument for turning on debug mode */
    public static final String ARG_DEBUG = "-debug";

    /** Command line argument for a plugin */
    public static final String ARG_PLUGIN = "-plugin";

    /** Command line argument for a plugin */
    public static final String ARG_INSTALLPLUGIN = "-installplugin";

    /** Command line argument for no plugins */
    public static final String ARG_NOPLUGINS = "-noplugins";

    /** Command line argument for  defining a display xml file */
    public static final String ARG_DXML = "-dxml";

    /** Command line argument for  a base64 encoded bundle */
    public static final String ARG_B64BUNDLE = "-b64bundle";

    /** Command line argument for  a bundle file or url */
    public static final String ARG_BUNDLE = "-bundle";

    /** Command line argument for setting the fixed time */
    public static final String ARG_FIXEDTIME = "-fixedtime";

    /** Command line argument for  enabling one instance running */
    public static final String ARG_ONEINSTANCEPORT = "-oneinstanceport";

    /** Command line argument for  not doing the  one instance running */
    public static final String ARG_NOONEINSTANCE = "-nooneinstance";

    /** Command line argument for  showing the data chooser */
    public static final String ARG_CHOOSER = "-chooser";

    /** Command line argument for  defining a catalog */
    public static final String ARG_CATALOG = "-catalog";

    /** Command line argument for  specifying jython code to execute */
    public static final String ARG_CODE = "-code";

    /** Command line argument for  conecting to a collab server */
    public static final String ARG_CONNECT = "-connect";

    /** Command line argument for  specifying a data source */
    public static final String ARG_DATA = "-data";

    /** Command line argument for  specifying a different main class to instantiate */
    public static final String ARG_MAINCLASS = "-mainclass";

    /** Command line argument for  overriding the default bundle */
    public static final String ARG_DEFAULT = "-default";

    /** Command line argument for  clearing  the default bundle */
    public static final String ARG_CLEARDEFAULT = "-cleardefault";

    /** Command line argument for  specifying a display to create */
    public static final String ARG_DISPLAY = "-display";

    /** Command line argument for  specifying a display template to create */
    public static final String ARG_TEMPLATE = "-template";

    /** Command line argument for  showing usage */
    public static final String ARG_HELP = "-help";

    /** Command line argument for  creating an image */
    public static final String ARG_IMAGE = "-image";

    /** Command line argument for  running in image server mode */
    public static final String ARG_IMAGESERVER = "-imageserver";

    /** Command line argument for  creating a movie */
    public static final String ARG_MOVIE = "-movie";

    /** Command line argument for  turning off the loading of default bundles */
    public static final String ARG_NODEFAULT = "-nodefault";

    /** Command line argument for  not having a gui */
    public static final String ARG_NOGUI = "-nogui";

    /** Command line argument for  turning off loading the user preferences */
    public static final String ARG_NOPREF = "-nopref";

    /** Command line argument for  specifying the collab server port */
    public static final String ARG_PORT = "-port";

    /** Command line argument for   specifying a collb server hostname */
    public static final String ARG_SERVER = "-server";

    /** Command line argument for  including a properties file */
    public static final String ARG_PROPERTIES = "-properties";

    /** Command line argument for  spefiying a jython script to execute */
    public static final String ARG_SCRIPT = "-script";


    /** Command line argument for  running a test */
    public static final String ARG_TEST = "-test";

    /** Command line argument for evaluating all data choices when in test mode */
    public static final String ARG_TESTEVAL = "-testeval";

    /** Command line argument for not showing errors in gui */
    public static final String ARG_NOERRORSINGUI = "-noerrorsingui";

    /** Command line argument for  running a test */
    public static final String ARG_TRACE = "-trace";

    /** Command line argument for  running a test */
    public static final String ARG_TRACEONLY = "-traceonly";

    /** Command line argument for  overriding the site path */
    public static final String ARG_SITEPATH = "-sitepath";

    /** Command line argument for  overriding the user path */
    public static final String ARG_USERPATH = "-userpath";

    /** Command line argument for turning on printing of jnlp embedded bundles */
    public static final String ARG_PRINTJNLP = "-printjnlp";


    /** Helper so we don't have to do (String)null */
    public static final String NULL_STRING = null;

    /** Helper so we don't have to do (DataSelection)null */
    public static final DataSelection NULL_DATA_SELECTION = null;

    /**
     *  This is the id for the reference to the instance of the IntegratedDataViewer
     *  used in XmlEncoder encoding/decoding application state.
     */
    public static final String ID_IDV = "idv";

    /** The version the bundle was aved under */
    public static final String ID_VERSION = "version";


    /**
     *  When we persist off the application state the actual object
     *  that is persisted is a Hashtable that holds the list of DataSource-s,
     *  ViewManager-s, DisplayControl-s and a Hashtable of miscellaneous properties.
     *  Each of these objects is placed into the persisted Hashtable with the following
     *  ID_ string identifiers.
     */
    public static final String ID_DATASOURCES = "datasources";

    /** Id used in hashtable bundle to define the view managers */
    public static final String ID_VIEWMANAGERS = "viewmanagers";

    /** Id used in hashtable bundle to define the jython */
    public static final String ID_JYTHON = "jython";

    /** Id used in hashtable bundle to include a message */
    public static final String ID_MESSAGE = "message";


    /** Id used in hashtable bundle to define the window list */
    public static final String ID_WINDOWS = "windows";

    /** Id used in hashtable bundle to define the display controls */
    public static final String ID_DISPLAYCONTROLS = "displaycontrols";

    /** Id used in hashtable bundle to define  misc stuff */
    public static final String ID_MISCHASHTABLE = "mischashtable";

    /** Id used in hashtable bundle to define commands to run (e.g., to show the color table editor) */
    public static final String ID_COMMANDSTORUN = "commandstorun";

    /** Id used for showing the legend icons */
    public static final String PREF_LEGEND_SHOWICONS = "idv.legend.showicons";

    /** Holds the preference for the default bounds for data source holder windows */
    public static final String PROP_DATAHOLDERBOUNDS = "dataholderbounds";

    /** Property used for showing the clock */
    public static final String PROP_SHOWCLOCK = "idv.monitor.showclock";

    /** Property for the minimum frame cycle time */
    public static final String PROP_MINIMUMFRAMECYCLETIME =
        "idv.minimumframecycletime";

    /** The fix file lockup property id */
    public static final String PROP_FIXFILELOCKUP = "idv.fixfilelockup";

    /** The display list group property id */
    public static final String PROP_DISPLAYLIST_GROUP =
        "idv.displaylist.group";

    /** Holds the preference for the one instance port */
    public static final String PROP_ONEINSTANCEPORT = "idv.oneinstanceport";

    /** The map globe level property id */
    public static final String PROP_MAP_GLOBE_LEVEL = "idv.map.globe.level";

    /** The map map level property id */
    public static final String PROP_MAP_MAP_LEVEL = "idv.map.map.level";

    /** The monitor port id */
    public static final String PROP_MONITORPORT = "idv.monitorport";

    /** The id for the maximum number of rendering threads preference */
    public static final String PREF_THREADS_RENDER = "idv.threads.render.max";

    /** The id for the maximum number of data reading threads preference */
    public static final String PREF_THREADS_DATA = "idv.threads.render.data";

    /** look and feel preference */
    public static final String PREF_EVENT_MOUSEMAP = "idv.event.mousemap";

    /** The show hidden files preference id */
    public static final String PREF_SHOWHIDDENFILES = "idv.showhiddenfiles";

    /** preference for scroll wheel */
    public static final String PREF_EVENT_WHEELMAP = "idv.event.wheelmap";

    /** preference for key mappings */
    public static final String PREF_EVENT_KEYBOARDMAP =
        "idv.event.keyboardmap";

    /** look and feel preference */
    public static final String PREF_LOOKANDFEEL = "idv.ui.lookandfeel";

    /** Embed a selector in the dashboard */
    public static final String PREF_EMBEDFIELDSELECTORINDASHBOARD =
        "idv.ui.embedfieldselectorindashboard";

    /** Embed a selector in the dashboard */
    public static final String PREF_EMBEDQUICKLINKSINDASHBOARD =
        "idv.ui.embedquicklinksindashboard";

    /** Embed a data chooserselector in the dashboard */
    public static final String PREF_EMBEDDATACHOOSERINDASHBOARD =
        "idv.ui.embeddatachooserindashboard";


    /** Embed the legends in the dashboard */
    public static final String PREF_EMBEDLEGENDINDASHBOARD =
        "idv.ui.embedlegendindashboard";

    /** Property name for showing in desktop mode */
    public static final String PROP_UI_DESKTOP = "idv.ui.desktop";


    /** Property for preferences */
    public static final String PROP_PREFERENCES = "idv.preferencepaths";


    /** Property name for showing in 3d or 2d */
    public static final String PROP_3DMODE = "idv.3d";

    /** Property name for  showing  in 3d or 2d */
    public static final String PROP_PROJ_NAME = "idv.projection.default";

    /** Holds the property for when  loading xml bundles */
    public static final String PROP_LOADINGXML = "idv.loadingxml";

    /** Property name for   the default station table list */
    public static final String PROP_DEFAULTLOCATIONS =
        "idv.locations.default";


    /** Property name for   showing the splash */
    public static final String PROP_SHOWSPLASH = "idv.splash.show";

    /** Property name for  splash icon */
    public static final String PROP_SPLASHICON = "idv.splash.icon";


    /**
     *  Do we show the data tree when we start
     */
    public static final String PROP_SHOWDATATREE = "idv.ui.showdatatree";

    /**
     *  Do we show the data selector when we start
     */
    public static final String PROP_SHOWDASHBOARD = "idv.ui.showdashboard";

    /** Holds the preference for */
    public static final String PROP_SHOWFORMULAS = "idv.ui.showformulas";

    /** Preference used for showing the toolbar */
    public static final String PREF_WINDOW_SHOWTOOLBAR = "idv.window.showtoolbar";
    
    /**
     *  Should we size the window wrt the screen size
     */
    public static final String PROP_WINDOW_USESCREENSIZE =
        "idv.ui.window.usescreensize";

    /**
     *  If so, how much smaller that the screen
     */
    public static final String PROP_WINDOW_SCREENSIZEOFFSET =
        "idv.ui.window.screensizeoffset";

    /** Property name for window width */
    public static final String PROP_WINDOW_SIZEWIDTH = "idv.ui.window.width";

    /** Property name for window height */
    public static final String PROP_WINDOW_SIZEHEIGHT =
        "idv.ui.window.height";


    /**
     *  Property id that defines what jnlp template file to use.
     */
    public static final String PROP_JNLPTEMPLATE = "idv.jnlp.template";

    /**
     *  Property id that defines the codebase for saving jnlp files.
     *  This property should be a url that points to a directory that holds jar files, etc.
     */
    public static final String PROP_JNLPCODEBASE = "idv.jnlp.codebase";

    /**
     *  Property id of the title used in generated jnlp files.
     */
    public static final String PROP_JNLPTITLE = "idv.jnlp.title";


    /** Property name for version file */
    public static final String PROP_VERSIONFILE = "idv.version.file";

    /**
     *  Property for the about.html
     */
    public static final String PROP_ABOUTTEXT = "idv.about.text";


    /**
     *  Property id that holds the application resource path. This is the value that
     *  gets substituted for the %APPPATH% macro in resource lists.
     */
    public static final String PROP_APPRESOURCEPATH = "idv.resourcepath";

    /**
     *  Where we find the resource bundle files
     */

    public static final String PROP_RESOURCEFILES = "idv.resourcefiles";

    /** The preference id for turning on/off caching */
    public static final String PREF_DOCACHE = "idv.docache";

    /** The preference id for the default distance unit */
    public static final String PREF_DISTANCEUNIT = "idv.distanceunit";

    /** The preference id for the default distance unit */
    public static final String PREF_VERTICALUNIT = "idv.verticalunit";

    /** The preference id for the size of the file cache */
    public static final String PREF_CACHESIZE = "idv.cachesize";


    /** The preference id for the size of the field cache */
    public static final String PREF_FIELD_CACHETHRESHOLD =
        "idv.field.cachethreshold";


    /** The preference id for the max image size */
    public static final String PREF_MAXIMAGESIZE = "idv.data.image.maxsize";


    /** The preference id for the size of the file cache */
    public static final String PREF_AUTOSELECTDATA = "idv.autoselectdata";


    /** The preference id for creating auto displays */
    public static final String PREF_AUTODISPLAYS_ENABLE =
        "idv.autodisplays.enable";

    /** The preference id for showing the autodisplay create dialog */
    public static final String PREF_AUTODISPLAYS_SHOWGUI =
        "idv.autodisplays.showgui";

    /** The preference id for whether we should confirm the quit */
    public static final String PREF_SHOWQUITCONFIRM =
        "idv.ui.showquitconfirm";

    /** The preference id for adjusting seam */
    public static final String PREF_FAST_RENDER = "idv.fastrender";

    /** The preference id for the last version the user ran with */
    public static final String PREF_LASTVERSION = "LastVersion";

    /** The id of the preference for asking the user to remove data/displays when they do an open */
    public static final String PREF_OPEN_ASK = "idv.open.ask";

    /** The id of the preference for whether to remove data/displays when they do an open */
    public static final String PREF_OPEN_REMOVE = "idv.open.remove";

    /** Property for saving zip data files to tmp */
    public static final String PREF_ZIDV_SAVETOTMP = "idv.zidv.savetotmp";

    /** Property for asking where to save zip data files */
    public static final String PREF_ZIDV_ASK = "idv.zidv.ask";

    /** Where to save zip data files */
    public static final String PREF_ZIDV_DIRECTORY = "idv.zidv.directory";

    /** geometry by reference */
    public static final String PREF_GEOMETRY_BY_REF =
        "visad.java3d.geometryByRef";

    /** image by reference */
    public static final String PREF_IMAGE_BY_REF = "visad.java3d.imageByRef";

    /** non-power of two textures */
    public static final String PREF_NPOT_IMAGE = "visad.java3d.textureNpot";

    /** Some preference */
    public static final String PREF_OPEN_MERGE = "idv.open.merge";


    /** Will be the id of the preference for whether to show the load bundle dialog */
    public static final String PREF_SHOWDECODEDIALOG = "idv.showdecodedialog";

    /**
     *  The preference id for the sitepath (which can be defined with the -sitepath command
     *  line argument).
     */
    public static final String PREF_SITEPATH = "SitePath";

    /** History file name */
    public static final String PREF_HISTORY = "history.xml";

    /** Used in the FileManager persistence */
    public static final String PREF_FILEWRITEDIR = "Idv.FileWriteDirectory";

    /** Used in the FileManager persistence */
    public static final String PREF_FILEREADDIR = "Idv.FileReadDirectory";


    /** Keep around the last archive name */
    public static final String PREF_ARCHIVENAME = "test.archivename";

    /** Keep around the last archive dir */
    public static final String PREF_ARCHIVEDIR = "test.archivedir";

    /** IDV memory usage */
    public static final String PREF_MEMORY = "idv.memory";

    /** IDV max perm gen memory usage */
    public static final String PREF_MAX_PERMGENSIZE = "idv.maxpermgensize";

    /** default max perm gen memory usage  (megabytes) */
    public static final int DEFAULT_MAX_PERMGENSIZE = 256;

    /** IDV use time driver preference*/
    public static final String PROP_USE_TIMEDRIVER = "idv.usetimedriver";
    
    /** Preference to store what choosers to show */
    public static final String PROP_CHOOSERS = "idv.choosers";

    /** Preference to store whether to show all  choosers */
    public static final String PROP_CHOOSERS_ALL = "idv.choosers.all";

    /** Preference to store what control descriptors to show */
    public static final String PROP_OLDCONTROLDESCRIPTORS =
        "idv.showcontrols";

    /** Preference to store what control descriptors to show */
    public static final String PROP_CONTROLDESCRIPTORS = "idv.controlstoshow";

    /** Preference to store whether to show all control descriptors */
    public static final String PROP_CONTROLDESCRIPTORS_ALL =
        "idv.showcontrols.all";

    /** Prefix for data things */
    public static final String PREF_DATAPREFIX = "Data.";


    /** Preference for showing the data selector */
    public static final String PREF_SHOWDASHBOARD = PROP_SHOWDASHBOARD;

    /** Preference for the lat/lon format */
    public static final String PREF_LATLON_FORMAT = PREF_DATAPREFIX
                                                    + "LatLonFormat";

    /** Preference for the TimeZone format */
    public static final String PREF_TIMEZONE = PREF_DATAPREFIX + "TimeZone";

    /** Preference for the date format */
    public static final String PREF_DATE_FORMAT = PREF_DATAPREFIX
                                                  + "DateFormat";

    /** default date format */
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss z";

    /** default time zone */
    public static final String DEFAULT_TIMEZONE = "GMT";

    /** Preference for the locale */
    public static final String PREF_LOCALE = PREF_DATAPREFIX + "Locale";

    /** Preference for showing the display control windows */
    public static final String PREF_SHOWCONTROLWINDOW =
        "Display.showControlWindow";

    /** Show controls in a tabbed window */
    public static final String PREF_CONTROLSINTABS = "Display.ShowInTabs";


    /** Holds the projection list */
    public static final String PREF_PROJ_LIST = "View.Map.ProjectionList";

    /** The sampling mode preference */
    public static final String PREF_SAMPLINGMODE =
        DisplayControl.PREF_SAMPLING_MODE;

    /** The sampling mode preference */
    public static final String PREF_VERTICALCS = PREF_DATAPREFIX
                                                 + "VerticalCS";


    /** Inital probe location  preference */
    public static final String INITIAL_PROBE_EARTHLOCATION =
        GridDisplayControl.INITIAL_PROBE_EARTHLOCATION;

}
