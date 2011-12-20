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

package ucar.unidata.idv;


import ucar.unidata.data.CacheDataSource;
import ucar.unidata.data.DataCancelException;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceResults;
import ucar.unidata.data.DerivedDataDescriptor;
import ucar.unidata.data.gis.WmsDataSource;
import ucar.unidata.data.gis.WmsSelection;
import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.DisplaySettingsDialog;
import ucar.unidata.idv.ui.DataSelector;
import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.IdvWindow;
import ucar.unidata.idv.ui.QuicklinkPanel;
import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.util.CacheManager;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PropertyValue;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.VisADPersistence;

import visad.Data;
import visad.VisADException;



import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import java.io.File;
import java.io.FileInputStream;

import java.lang.reflect.Method;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;


/**
 * This is the central class for  IDV applications. It is abstract
 * and needs a concrete derived class to overwrite the doMakeContents
 * method. (e.g., {@link DefaultIdv}). This class serves as the nexus
 * of a collection of editors  and managers (e.g., IdvUIManager, VMManager, etc.)
 * Its base class, {@link IdvBase}, holds these editors and managers and
 * uses a set of factory methods (e.g., doMakeStationModelManager)
 * for creating them.  One can override, in a derived class,
 * one of these factory methods to create special purpose
 * components or handlers.
 * <p>
 * To run the IDV you:
 * <ul>
 *   <li> Create the  instance of the IDV (e.g., {@link DefaultIdv#main(String[])})
 *   <li> The IDV, in its constructor, initializes the property files and command line arguments:
 *   <ul>
 *     <li> Constucts the {@link ArgsManager}.
 *     <li> Adds the default property file to the ArgsManager list of propertyFiles
 *     <li> Calls {@link #initPropertyFiles(List)} to add in any application specific
 *          property file. Properties in files at the end of the list will overwrite
 *          properties from files that are in the beginning of the list.
 *     <li> Call {@link ArgsManager#parseArgs()}. This parses the command line arguments.
 *          If your IDV app needs to have its own command line arguments then override the
 *          <code>doMakeArgsManager (String[]args)</code> to create your own ArgsManager.
 *     <li> Call {@link StateManager#loadProperties()} to load in the property files.
 *   </ul>
 *   <li> The derived IDV class calls init, either from its constructor or form the main.
 *        We have the derived class call init (as opposed to this class) so that its state is fully initialized
 *        when init is called.
 * </ul>
 * @author IDV development team
 */

public class IntegratedDataViewer extends IdvBase implements ControlContext,
        ViewContext, DataContext, ActionListener, HyperlinkListener,
        LogUtil.DialogManager {

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            IntegratedDataViewer.class.getName());


    /** Is set to true after the idv has been fully initialized */
    private boolean haveInitialized = false;


    /**
     *  Should we go about removing a display contorl from the menus, etc.
     *  This is set to false when the IDV does a remove all displays call.
     *  Normally this is true;
     */
    private boolean ignoreRemoveDisplayControl = false;


    /**
     *  A list of the files that the user has opened in the past.
     */
    private List historyList = null;

    /** mutex for accessing the history list */
    private Object MUTEX_HISTORY = new Object();



    /** List of all of the {@link DisplayControl}s currently active */
    protected List displayControls = new ArrayList();


    /** List of the {@link ControlDescriptor}s defined in the controls.xml file */
    protected List controlDescriptors = new ArrayList();


    /** Mapping from control descriptor id to {@link ControlDescriptor} */
    protected Hashtable controlDescriptorMap = new Hashtable();


    /** Serves up images */
    private ImageServer imageServer;


    /** This is instantiated to allow other idvs to connect to only have one process running */
    private OneInstanceServer oneInstanceServer;

    /** The http based monitor to dump stack traces and shutdown the IDV */
    private IdvMonitor idvMonitor;


    /** The list of background image wmsselections */
    private List backgroundImages;

    /** The version of Java 3D */
    private static final String JAVA3D_VERSION = "1.2";

    /** Accessory in file save dialog */
    private JCheckBox overwriteDataCbx;

    /** Are we interactive */
    private boolean interactiveMode = true;




    /**
     * Parameterless constructor. Not sure if this is needed.
     * Perhaps it is needed for the XmlEncoder encoding?
     *
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public IntegratedDataViewer() throws VisADException, RemoteException {
        this(new String[] {});
    }


    /**
     * The main constructor. After the dervied class is created
     * then the {@link #init()} method should be called.
     * We have the derived class call init so that the object
     * has been fully instantiated when init is called.
     *
     * @param args The command line arguments
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     *
     */
    public IntegratedDataViewer(String[] args)
            throws VisADException, RemoteException {
        this(args, true);
    }

    /**
     * Exit if interactive
     */
    private void bailOut() {
        if (interactiveMode) {
            System.exit(1);
        }
    }


    /**
     * Ctor for when some other code is calling us. eg: creating an image
     *
     * @param interactiveMode Is interactive
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public IntegratedDataViewer(boolean interactiveMode)
            throws VisADException, RemoteException {
        this(new String[] {}, interactiveMode);
    }


    /**
     * ctor
     *
     * @param args cmd line args
     * @param interactiveMode Are we interactive mode. Normally we are, including when running an isl.
     * However, we use this flag when code from some jvm calls us directly. eg: when generating an image.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public IntegratedDataViewer(String[] args, boolean interactiveMode)
            throws VisADException, RemoteException {
        super(args);
        this.interactiveMode = interactiveMode;
        LogUtil.setDialogManager(this);
        checkSystem();
        HTTPClient.CookieModule.setCookiePolicyHandler(null);

        /*
        VirtualUniverse.addRenderingErrorListener(new RenderingErrorListener() {
                public void errorOccurred(RenderingError err) {
                    LogUtil.userErrorMessage("<html>A Java3D rendering error occurred:<br>"+ err.getErrorMessage() +"<br>" +(err.getDetailMessage()!=null?err.getDetailMessage():""));
                }
            });*/
        super.setIdv(this);

        if ( !interactiveMode) {
            getArgsManager().setIsOffScreen(true);
        }


        //Put the default property file in the list before we parse args 
        getArgsManager().propertyFiles.add(
            "/ucar/unidata/idv/resources/idv.properties");
        initPropertyFiles(getArgsManager().propertyFiles);

        //Now parse the command line arguments
        try {
            getArgsManager().parseArgs();
        } catch (Throwable exc) {
            logException("Processing arguments", exc);
            bailOut();
            return;
        }

        if ((argsManager.oneInstancePort > 0) && !argsManager.noOneInstance) {
            checkOneInstance(argsManager.oneInstancePort);
        }




        try {
            Trace.call1("initState");
            getStateManager().initState(interactiveMode);
            Trace.call2("initState");
        } catch (Throwable exc) {
            //If there was any error here then exit
            logException("Fatal error initializing resources", exc);
            bailOut();
            return;
        }

        //Now, if we didnt have a command line oneinstanceport argument check if we have
        //a property
        if ((argsManager.oneInstancePort <= 0) && !argsManager.noOneInstance
                && (getProperty(PROP_ONEINSTANCEPORT, -1) > 0)) {
            checkOneInstance(getProperty(PROP_ONEINSTANCEPORT, -1));
        }



        //Set the default directory property
        FileManager.setStore(getStore(), PREF_FILEWRITEDIR, PREF_FILEREADDIR);
        FileManager.setFixFileLockup(getProperty(PROP_FIXFILELOCKUP, false));
        long minFrameCycleTime =
            (long) getProperty(PROP_MINIMUMFRAMECYCLETIME, 0);
        if (minFrameCycleTime != 0) {
            System.err.println("Setting minFrameCycleTime to:"
                               + minFrameCycleTime);
            visad.java3d.UniverseBuilderJ3D.setMinimumFrameCycleTime(
                minFrameCycleTime);
        }




        startMonitor();
        getJythonManager();
        getDataManager();
        getPublishManager().initPublisher();
    }

    /**
     * See if we are in server mode or not
     *
     * @return true if in server mode
     */
    public boolean getServerMode() {
        return !interactiveMode;
    }


    /**
     * Are we interactive
     *
     * @return is interactive
     */
    public boolean getInteractiveMode() {
        //        if (true) {
        //            return true;
        //        }
        return interactiveMode && !getArgsManager().isScriptingMode();
    }


    /**
     * Check whether the system has the necessary components (ex: Java 3D).
     * Subclasses can override.
     */
    protected void checkSystem() {
        if ( !visad.util.Util.canDoJava3D(JAVA3D_VERSION)) {
            if (interactiveMode) {
                LogUtil.userMessage(
                    "<html>This application needs Java 3D " + JAVA3D_VERSION
                    + " or higher to run.<br>Please see the User's Guide for more information.</html>");
                bailOut();
            } else {
                throw new IllegalArgumentException(
                    "This application needs Java 3D " + JAVA3D_VERSION
                    + " or higher to run.<br>Please see the User's Guide for more information.");
            }
        }
    }


    /** flag for trying one instance */
    private boolean tryingOneInstance = false;

    /** flag for one instance interupted */
    private boolean oneInstanceInterrupted = false;


    /**
     * Check for one instance
     *
     * @param port the port
     */
    private synchronized void checkOneInstance(final int port) {
        tryingOneInstance = true;
        Misc.run(new Runnable() {
            public void run() {
                checkOneInstanceInner(port);
                boolean tmp = tryingOneInstance;
                tryingOneInstance = false;
                try {
                    if (tmp) {
                        IntegratedDataViewer.this.notify();
                    }
                } catch (Exception exc) {}
            }
        });
        try {
            if (tryingOneInstance) {
                //Wait 10 seconds 
                wait(10000);
                //If we are still trying then there was some problem
                if (tryingOneInstance) {
                    tryingOneInstance = false;
                    LogUtil.userErrorMessage(
                        "The IDV was unable to connect to the one instance port: "
                        + port);
                }
                tryingOneInstance = false;
            }
        } catch (Exception exc) {
            System.err.println("err:" + exc);
            exc.printStackTrace();
        }
    }




    /**
     * Wait until the displays have been rendered
     */
    public void waitUntilDisplaysAreDone() {
        IdvManager.waitUntilDisplaysAreDone(getIdvUIManager());
    }

    /**
     * Try to connect to another idv and, if connected, pass it the command line args
     * and exit.
     *
     *
     * @param port the port
     */
    private synchronized void checkOneInstanceInner(final int port) {
        try {
            String url = OneInstanceServer.assembleUrl(port,
                             argsManager.originalArgs);
            String result = IOUtil.readContents(url, (String) null);
            if ( !tryingOneInstance) {
                return;
            }
            tryingOneInstance = false;
            if ((result != null) && result.trim().equals("ok")) {
                System.exit(0);
            }
            Misc.run(new Runnable() {
                public void run() {
                    startOneInstanceServer(port);
                }
            });
        } catch (Exception exc) {}
    }

    /**
     * Start up the one instance server
     *
     * @param port the port
     */
    private void startOneInstanceServer(int port) {
        try {
            oneInstanceServer = new OneInstanceServer(this, port);
            oneInstanceServer.init();
        } catch (Exception exc) {
            logException("Trying to create a one-instance server on port:"
                         + port, exc);
        }
    }



    /**
     *  This is a wrapper that calls initInner within a thread.
     *  That  way the  gui can get built and displayed, etc.
     */
    protected final void init() {
        GuiUtils.setApplicationTitle("Unidata IDV - ");

        /*
        final PrintStream oldErr = System.err;
        System.setErr(new PrintStream(System.out){
                public void     println(String x) {
                    oldErr.println("ERR:" + x);
                    //                    if(x.indexOf("size:")>=0) {
                        Misc.printStack("got it");
                        //                    }
                }
            });
        */

        /*
        final PrintStream oldOut = System.out;
        System.setOut(new PrintStream(System.err){
                public void     println(String x) {
                    oldOut.println("ERR:" + x);
                    if(x.indexOf("size:")>=0) {
                        throw new IllegalArgumentException("got it");
                        //                        Misc.printStack("got it");
                    }
                }
            });
        */



        //First create the gui.
        try {}
        catch (Throwable exc) {
            logException("Initializing the GUI", exc);
            //TODO: do we exit here?
        }

        //Now call initInner within a thread so the gui can be painted.
        Misc.run(new Runnable() {
            public void run() {
                try {
                    initInner();
                } catch (Throwable exc) {
                    logException("Initializing the IDV", exc);
                    //TODO: do we exit here?
                }
            }
        });
    }



    /**
     * Load in a datasource for each file/url in the given files list.
     *
     * @param files String file or urls
     */
    protected void loadDataFiles(List files) {
        //Now run through any command line data files
        String            period  = ".";
        DataSourceResults results = new DataSourceResults();
        for (int i = 0; i < files.size(); i++) {
            if (i == 0) {
                getIdvUIManager().splashMsg("Loading initial data" + period);
            }
            results.merge(
                getDataManager().createDataSource(files.get(i).toString()));
            period += ".";
            getIdvUIManager().splashMsg("Loading Initial Data" + period);
        }
        getIdvUIManager().showResults(results);
    }


    /**
     *  Build the gui, process bundles, etc.
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void initInner() throws VisADException, RemoteException {

        /*      Misc.runInABit(1000, new Runnable() {
                public void run () {
                    LogUtil.userErrorMessage("TEST");
                }
            });
        */


        getIdvUIManager().doBasicInitialization();

        if (argsManager.traceMode) {
            getTestManager().startTrace();
        }



        //If we had a command line argument of a script file then evaluate the script.
        //The LogUtil.setDoGui (false) turns off any jdialogs from popping up
        if (argsManager.jythonCode != null) {
            try {
                LogUtil.setTestMode(true);
                getJythonManager().evaluateTrusted(argsManager.jythonCode);
                LogUtil.setTestMode(false);
            } catch (Throwable exc) {
                Misc.fatal(exc);
            }
        }

        if (argsManager.testMode) {
            haveInitialized = true;
            getTestManager().doTest();
            return;
        }


        List fileMappingIds   = getArgsManager().fileMappingIds;
        List fileMappingFiles = getArgsManager().fileMappingFiles;
        for (int i = 0; i < fileMappingIds.size(); i++) {
            getPersistenceManager().addFileMapping(
                fileMappingIds.get(i).toString(),
                (List) fileMappingFiles.get(i));
        }

        //        LogUtil.logException ("test1",new IllegalArgumentException ("xxx"));
        //        LogUtil.logException ("test2",new IllegalArgumentException ("xxx"));


        getIdvUIManager().init();

        loadDataFiles(argsManager.initDataFiles);
        getArgsManager().processInitialBundles();
        initCacheManager();

        haveInitialized = true;
        //The args manager needs to go first
        getIdvUIManager().initDone();
        getArgsManager().initDone();




        if ( !getArgsManager().getIsOffScreen()) {
            //checkVersion();
        }


        for (int i = 0; i < argsManager.displayTemplates.size(); i += 3) {
            String datasourcePath =
                (String) argsManager.displayTemplates.get(i);
            String param = (String) argsManager.displayTemplates.get(i + 1);
            String bundle = (String) argsManager.displayTemplates.get(i + 2);
            DataSource dataSource = makeOneDataSource(datasourcePath, null,
                                        null);
            if (dataSource == null) {
                continue;
            }
            DataChoice choice = dataSource.findDataChoice(param);
            if (choice == null) {
                LogUtil.userMessage("No parameter found: " + param
                                    + " in data source:" + datasourcePath);
                continue;
            }

            DisplayControl control =
                getPersistenceManager().instantiateFromTemplate(bundle);
            if (control == null) {
                LogUtil.userMessage("Unable to create display from template:"
                                    + bundle);
                continue;
            }
            control.init("display", new ArrayList(), Misc.newList(choice),
                         this, new Hashtable(), null);
        }

        //Create the chooser so it is snappy when  it is first requested
        if ( !getArgsManager().isScriptingMode()) {
            getIdvChooserManager();
        }

        //Start up the image server
        if ((argsManager.imageServerPort > 0)
                || (argsManager.imageServerPropertyFile != null)) {
            runImageServer(argsManager.imageServerPort,
                           argsManager.imageServerPropertyFile);
        }


        if ( !getArgsManager().isScriptingMode()) {
            ucar.unidata.ui.HelpActionLabel.setActionListener(
                new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    handleAction(ae.getActionCommand(), null);
                }

            });
        }


        initDone();


        /*
        Misc.run(new Runnable() {
                public void run() {
        try {
            ucar.unidata.repository.MetaDataServer mds = new ucar.unidata.repository.MetaDataServer(new String[]{});
            mds.init();
        } catch(Exception exc) {
            logException ("Starting the metadata server", exc);
        }
        }});*/
    }


    /**
     *  Gets called when all initialization is finished.
     */
    public void initDone() {
        //Clear out the automatic display creation args
        argsManager.initParams   = new ArrayList();
        argsManager.initDisplays = new ArrayList();


        if ( !getArgsManager().getIsOffScreen()) {
            Misc.run(new Runnable() {
                public void run() {
                    try {
                        //Check for the bundles.xml
                        getInstallManager().automaticallyCheckForUpdates();
                        IOUtil.readContents(
                            "http://www.unidata.ucar.edu/software/idv/resources/bundles.xml",
                            getClass());
                    } catch (Exception exc) {}
                }
            });
        }
    }


    /**
     * Start up the IDV montior server. This is an http server on the port defined
     * by the property idv.monitorport (8788). It provides 2 urls only accessible from localhost:
     * http://localhost:8788/stack.html
     * http://localhost:8788/shutdown.html
     */
    protected void startMonitor() {
        if (idvMonitor != null) {
            return;
        }
        final String monitorPort = getProperty(PROP_MONITORPORT, "");
        if ((monitorPort != null) && (monitorPort.trim().length() > 0)
                && !monitorPort.trim().equals("none")) {
            Misc.run(new Runnable() {
                public void run() {
                    try {
                        idvMonitor = new IdvMonitor(
                            IntegratedDataViewer.this,
                            new Integer(monitorPort).intValue());
                        idvMonitor.init();
                    } catch (Exception exc) {
                        LogUtil.consoleMessage(
                            "Unable to start IDV monitor on port:"
                            + monitorPort);
                        LogUtil.consoleMessage("Error:" + exc);
                    }
                }
            });
        }
        /*
        final Object test1 = new Object();
        final Object test2 = new Object();
        Misc.run(new Runnable() {
                    public void run() {
                        synchronized(test1) {
                            Misc.sleep(1000);
                        synchronized(test2) {
                        }
                        }
                    }});


Misc.run(new Runnable() {
                    public void run() {
                        synchronized(test2) {
                            Misc.sleep(1000);
                        synchronized(test1) {
                        }
                        }
                    }});



        */
    }

    /**
     * Set the state in the cache manager
     */
    public void initCacheManager() {
        //Set the tmp dir
        CacheManager.setTmpDir(new File(getStore().getUserTmpDirectory()));
        CacheManager.setDoCache(getStore().get(PREF_DOCACHE, true));
        //We save off megabytes
        double size = getStore().get(PREF_CACHESIZE, 20.0);
        CacheManager.setMaxFileCacheSize((int) (size * 1000000));
        CacheManager.addCacheListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                clearCachedData();
            }
        });

    }

    /**
     * This is called when the CacheManager detects the need ot clear memory.
     */
    protected void clearCachedData() {
        List dataSources = getDataSources();
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            dataSource.clearCachedData();
        }
    }

    /**
     *  Has this IDV been fully  initialized
     *
     * @return Has this idv been initialized
     */
    public boolean getHaveInitialized() {
        return haveInitialized;
    }

    /**
     * Is it ok to show any windows. This returns false if we have not been initialized yet
     * or if we are in offscreen mode
     *
     * @return Ok to show windows
     */
    public boolean okToShowWindows() {
        return /*haveInitialized &&*/ !getArgsManager().getIsOffScreen();
    }

    /**
     *  A hook to allow derived classes to add in their own property files.
     *
     * @param files A list of file names or urls (String) that point to
     * the property files that are to be loaded in. Properties from later
     * files in the list overwrite properties from earlier files.
     */
    public void initPropertyFiles(List files) {}



    /** default string for no version */
    private static String NO_VERSION = "No version";

    /**
     * This checks to see if the user is running a new version of the idv.
     * If they are then it asks the user if they want to see the release
     * notes.  If yes, they are shown.
     */
    private void checkVersion() {
        String lastVersion    = getStore().get(PREF_LASTVERSION, NO_VERSION);
        String currentVersion = getStateManager().getVersion();
        if (lastVersion.equals(currentVersion)) {
            return;
        }
        getStore().put(PREF_LASTVERSION, currentVersion);
        getStore().saveIfNeeded();
        if (GuiUtils.showYesNoDialog(
                null,
                "<html><center>You are now running IDV version "
                + currentVersion
                + "<p>Would you like to see the Release Notes?</center></html>", "Show Release Notes?")) {
            getIdvUIManager().showHelp("idv.releasenotes");;
        }
    }

    /**
     * Register a help key for a component
     *
     * @param comp    component
     * @param helpId  the help id for that component
     */
    public void registerHelpKey(JComponent comp, final String helpId) {
        System.err.println("register");
        comp.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                System.err.println("key pressed");
                if (e.getKeyCode() == KeyEvent.VK_F1) {
                    getIdvUIManager().showHelp(helpId);
                }
            }
        });
    }

    /**
     * Make a help button for a particular help topic
     *
     * @param helpId  the id of the topic
     *
     * @return  the component
     */
    public JComponent makeHelpButton(String helpId) {
        return makeHelpButton(helpId, "Show Help");
    }

    /**
     * Make a help button for a particular help topic
     *
     * @param helpId  the topic id
     * @param toolTip  the tooltip
     *
     * @return  the button
     */
    public JComponent makeHelpButton(String helpId, String toolTip) {
        JButton btn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Help16.gif",
                                     getIdvUIManager(), "showHelp", helpId);
        if (toolTip != null) {
            btn.setToolTipText(toolTip);
        }
        return btn;
    }


    /**
     *  Sometime we will be able to dynamically load in a rbi file (resource bundle)
     *
     * @param filename The rbi file.
     */
    public void loadRbiFile(String filename) {}


    /**
     *  Create (if null) and return the list NamedStationTable-s defined by the
     *  locationResources XmlResourceCollection.
     *
     * @return The list of {@link ucar.unidata.metdata.NamedStationTable}s
     */
    public List getLocationList() {
        return getResourceManager().getLocationList();
    }




    /**
     *  Return the list of {@link ucar.unidata.geoloc.Projection}s
     *
     * @return The list of {@link ucar.unidata.geoloc.Projection}s
     */
    protected List getProjections() {
        return getIdvProjectionManager().getProjections();
    }


    /**
     * Add a new {@link ControlDescriptor} into the controlDescriptor
     * list  and controlDescriptorMap hashtable. Only add this in
     * if we do <em>not</em> already have one loaded with the same id
     *
     * @param cd The ControlDescriptor to add
     */
    protected void addControlDescriptor(ControlDescriptor cd) {
        if (controlDescriptorMap.get(cd.getControlId()) == null) {
            controlDescriptors.add(cd);
            controlDescriptorMap.put(cd.getControlId(), cd);
        }
    }


    /**
     *  Add into the {@link IdvPreferenceManager}  the different gui components/preference managers
     *
     * @param preferenceManager The preferenceManager  to initialize
     */
    protected void initPreferences(IdvPreferenceManager preferenceManager) {
        preferenceManager.initPreferences();

    }


    /**
     * Create a {@link ucar.unidata.idv.ui.DataSelector} window.
     *
     * @return the new DataSelector window.
     */
    public DataSelector createDataSelector() {
        return getIdvUIManager().createDataSelector();
    }


    /**
     *  Popup a dialog containing a {@link ucar.unidata.idv.ui.DataTree}
     *  showing the  {@link ucar.unidata.data.DataChoice}s that are applicable
     *  to the given {@link ControlDescriptor}. Return the select data choice.
     *
     * @param descriptor The {@link ControlDescriptor} to select a DataChoice for.
     * @return The selected {@link ucar.unidata.data.DataChoice} or null if none selected.
     */
    public DataChoice selectDataChoice(ControlDescriptor descriptor) {
        return getIdvUIManager().selectDataChoice(descriptor);
    }

    /**
     *  Popup a dialog containing a DataTree for each operand in the given fullParamNames list
     *  Return a List of DataChoice's the user selects or null if they canceled.
     *
     * @param fullParamNames List of String names representing the operands or parameters that
     *                       are to be chosen.
     * @return List of selected {@link ucar.unidata.data.DataChoice}s
     */
    public List selectDataChoices(List fullParamNames) {
        if (LogUtil.getTestMode()) {
            return null;
        }

        if (fullParamNames.size() == 0) {
            return new ArrayList();
        }
        return getIdvUIManager().selectDataChoices(fullParamNames);
    }



    /**
     * Popup a JTextField containing dialog that allows  the user
     * to enter text values, one for each name in the userChoices List.
     * This is a pass through to
     * {@link IdvUIManager#selectUserChoices (String,List)}
     *
     * @param msg The message to display in the GUI
     * @param userChoices List of Strings, one for each value
     * @return List of Strings the user entered or null if they cancelled
     */
    public List selectUserChoices(String msg, List userChoices) {
        if (LogUtil.getTestMode()) {
            return null;
        }
        return getIdvUIManager().selectUserChoices(msg, userChoices);
    }


    /**
     * Implementation of the DataContext method.
     * This method gets called when something changed in the data source.
     * It just acts as a pass through to the IdvUIManager that updates
     * any user interfaces.
     *
     * @param source The data source that changed
     */
    public void dataSourceChanged(DataSource source) {
        getIdvUIManager().dataSourceChanged(source);
    }


    /**
     * Evalute the data choice and then save it off in a CacheDataSource
     *
     * @param dataChoice The data chocie to evaluate
     */
    public void evaluateAndSave(DataChoice dataChoice) {
        DataChoice clonedDataChoice = dataChoice.createClone();
        showWaitCursor();
        try {
            Data d = clonedDataChoice.getData(new DataSelection());
            if (d == null) {
                return;
            }
            saveInCacheInner(clonedDataChoice, d, null,
                             clonedDataChoice.getName());
        } catch (DataCancelException exc) {}
        catch (Exception exc) {
            logException("Evaluating data choice", exc);
        }
        showNormalCursor();
    }




    /**
     * Clone the data choice and then save it off in a CacheDataSource
     *
     * @param dataChoice The data choice to save
     * @param data The data
     */
    public void saveInCache(DataChoice dataChoice, Data data) {
        saveInCache(dataChoice, data, null, dataChoice.getName());
    }


    /**
     * Save the given data in the CacheDataSource
     *
     * @param dataChoice data choice
     * @param data data
     * @param dataSelection data selection
     */
    public void saveInCache(DataChoice dataChoice, Data data,
                            DataSelection dataSelection) {
        saveInCache(dataChoice, data, dataSelection, dataChoice.getName());
    }


    /**
     * Clone the data choice and then save it off in a CacheDataSource
     *
     * @param dataChoice The data choice to save
     * @param data The data
     * @param name The name to use
     */
    public void saveInCache(DataChoice dataChoice, Data data, String name) {
        saveInCache(dataChoice, data, null, name);
    }


    /**
     * Clone the data choice and then save it off in a CacheDataSource
     *
     * @param dataChoice The data choice to save
     * @param data The data
     * @param dataSelection data selection
     * @param name The name to use
     */
    public void saveInCache(DataChoice dataChoice, Data data,
                            DataSelection dataSelection, String name) {
        DataChoice clonedDataChoice = dataChoice.createClone();
        saveInCacheInner(clonedDataChoice, data, dataSelection, name);
    }


    /**
     * Evalute the data choice and then save it off in a CacheDataSource
     *
     * @param clonedDataChoice The data choice to save
     * @param data The data
     * @param dataSelection data selection
     * @param name The name to use
     */
    private void saveInCacheInner(DataChoice clonedDataChoice, Data data,
                                  DataSelection dataSelection, String name) {
        try {
            name = GuiUtils.getInput("Please enter a name for this data:",
                                     "Name: ", name, null, null,
                                     "Save In Cache");
            if (name == null) {
                return;
            }
            CacheDataSource cds         = null;
            List            dataSources = getDataSources();
            for (int i = 0; i < dataSources.size(); i++) {
                DataSource dataSource = (DataSource) dataSources.get(i);
                if (dataSource instanceof CacheDataSource) {
                    cds = (CacheDataSource) dataSource;
                    break;
                }
            }
            if (cds == null) {
                cds = (CacheDataSource) makeOneDataSource("", "CACHED",
                        new Hashtable());
            }
            cds.addDataChoice(clonedDataChoice, name, data, dataSelection);
        } catch (Exception exc) {
            logException("Evaluating data choice", exc);
        }
        showNormalCursor();
    }





    /**
     * Get the list of derived data choices
     *
     * @param dataSource data source
     * @param dataChoices data choices
     *
     * @return derived data choices
     */
    public List getDerivedDataChoices(DataSource dataSource,
                                      List dataChoices) {
        List derivedList = DerivedDataDescriptor.getDerivedDataChoices(this,
                               dataChoices,
                               getJythonManager().getDescriptors());
        return derivedList;
    }





    /**
     * Make and return the menu of commands that can be applied
     * to the given {@link ucar.unidata.data.DataChoice}.
     * Just a pass through to the {@link IdvUIManager#doMakeDataChoiceMenu(DataChoice)}
     *
     * @param dataChoice The data choice to make the menu for
     * @return The menu
     */
    public JMenu doMakeDataChoiceMenu(DataChoice dataChoice) {
        return getIdvUIManager().doMakeDataChoiceMenu(dataChoice);
    }


    /**
     *  Return a String of semi-colon separated name=value pairs
     *  that define the default properties for ViewManagers.
     *
     * @return Propery string for when we create a {@link ViewManager}
     */
    public String getViewManagerProperties() {
        return getProperty("idv.viewmanager.properties", "");
    }


    /**
     * Create a new window containing a new {@link MapViewManager}
     */
    public void createNewWindow() {
        getIdvUIManager().createNewWindow();
    }


    /**
     * Create, if needed, and return the default 3d view manager
     * @return The view manager
     */
    public ViewManager getViewManager() {
        return getViewManager(ViewDescriptor.LASTACTIVE, true, null);
    }


    /**
     * Implementation of the ControlContext interface call.
     * Creates, if needed, and returns the {@link ViewManager}  that is specified
     * by the given {@link ViewDescriptor}
     *
     * @param viewDescriptor Defines the name and type of the ViewManager
     * that is to be found or created.
     *
     * @return Either the  existing ViewManager or a new one
     */
    public ViewManager getViewManager(ViewDescriptor viewDescriptor) {
        return getViewManager(viewDescriptor, true, null);
    }

    /**
     * Creates, if needed, and returns the {@link ViewManager}  that is specified
     * by the given {@link ViewDescriptor}
     *
     * @param viewDescriptor Defines the name and type of the ViewManager
     * that is to be found or created.
     *
     * @param newWindow If true we create a new window and add the ViewManager into it
     *
     * @param properties The properties to pass through to the
     * ViewManager if we are creating a new one.
     *
     * @return Either the  existing ViewManager or a new one
     */
    public ViewManager getViewManager(ViewDescriptor viewDescriptor,
                                      boolean newWindow, String properties) {
        //The false says to not force the creation of the VM
        ViewManager viewManager =
            getVMManager().findViewManager(viewDescriptor);
        //        System.err.println("getViewManager:" + viewDescriptor +" got it=" +(viewManager!=null));
        if (viewManager != null) {
            return viewManager;
        }
        if (getArgsManager().getIsOffScreen()
                || ( !getIdv().okToShowWindows() && !newWindow)) {
            ViewManager vm = getVMManager().createViewManager(viewDescriptor,
                                 properties);
            return vm;
        }
        IdvWindow window = getIdvUIManager().createNewWindow();
        if (window != null) {
            List viewManagers = window.getViewManagers();
            for (int i = 0; i < viewManagers.size(); i++) {
                ViewManager tmpViewManager =
                    (ViewManager) viewManagers.get(i);
                if (viewManager == null) {
                    if (tmpViewManager.isDefinedBy(viewDescriptor)) {
                        viewManager = tmpViewManager;
                    }
                }
            }
            if ((viewManager == null) && (viewManagers.size() > 0)) {
                viewManager = (ViewManager) viewManagers.get(0);
            }
            if (viewManager != null) {
                viewManager.addViewDescriptor(viewDescriptor);
            }
        }
        return viewManager;
    }


    /**
     * Return  all the  {@link ControlDescriptor}s
     *
     * @return List of ControlDescriptor-s
     */
    public List getAllControlDescriptors() {
        return controlDescriptors;
    }

    /**
     * This returns the set of {@link ControlDescriptor}s
     * that can be shown. That is, the control descriptors that the
     * user has chosen to show through the user preferences.
     *
     * @return List of shown control descriptors.
     */
    public List getControlDescriptors() {
        return getControlDescriptors(false);
    }



    /**
     * This returns the set of {@link ControlDescriptor}s
     * that can be shown. That is, the control descriptors that the
     * user has chosen to show through the user preferences.
     *
     * @param includeTemplates If true then include the display templates
     * @return List of shown control descriptors.
     */
    public List getControlDescriptors(boolean includeTemplates) {
        List l = new ArrayList();
        for (int i = 0; i < controlDescriptors.size(); i++) {
            ControlDescriptor controlDescriptor =
                (ControlDescriptor) controlDescriptors.get(i);
            if (getPreferenceManager().shouldShowControl(controlDescriptor)) {
                l.add(controlDescriptor);
            }
        }
        if (includeTemplates) {
            getPersistenceManager().getControlDescriptors(l);
        }
        return l;
    }



    /**
     * Return the {@link ControlDescriptor} with the given name, or null
     * if not found.
     *
     * @param name ControlDescriptor name to look up
     * @return The ControlDescriptor with the given name or null
     */
    public ControlDescriptor getControlDescriptor(String name) {
        return (ControlDescriptor) controlDescriptorMap.get(name);
    }

    /**
     * Return the list of {@link DisplayControl}s currently active
     *
     * @return List of display controls
     */
    public List getDisplayControls() {
        return new ArrayList(displayControls);
    }

    /**
     * Have all of the displays been initialixed
     *
     * @return all displays initialized
     */
    public boolean getAllDisplaysIntialized() {
        for (DisplayControl control :
                (List<DisplayControl>) getDisplayControls()) {
            if ( !control.isInitDone()) {
                return false;
            }
        }
        return true;
    }


    /**
     * Move the given display control to the front. We simply move the control to the end of
     * the list of display controls.
     *
     * @param control The display control
     */
    public void toFront(DisplayControl control) {
        displayControls.remove(control);
        displayControls.add(control);
    }

    /**
     * Called by the given {@link DisplayControl} when it has been fully
     * initialized.
     *
     * @param control The initialized DisplayControl
     */
    public void controlHasBeenInitialized(DisplayControl control) {
        if (collabManager != null) {
            collabManager.controlHasBeenInitialized(control);
        }
    }


    /**
     * Add the given {@link DisplayControl} into the list of
     * display controls. Update the user interfaces.
     *
     * @param control The new display control
     */
    public void addDisplayControl(DisplayControl control) {
        displayControls.add(control);
        getIdvUIManager().addDisplayControl(control);
    }


    /**
     * Remove the given {@link DisplayControl} from the list of
     * display controls. Update the user interfaces.
     *
     * @param control The  removed display control
     */
    public void removeDisplayControl(DisplayControl control) {
        if ( !ignoreRemoveDisplayControl) {
            displayControls.remove(control);
            getIdvUIManager().removeDisplayControl(control);
        }
        if (collabManager != null) {
            collabManager.writeRemoveDisplayControl(control);
        }
        //        Runtime.getRuntime().gc();
    }



    /**
     * Remove all of the displays.
     */
    public void removeAllDisplays() {
        removeAllDisplays(true);
    }

    /**
     * remove all displays
     *
     * @param payAttentionToCanDoRemoveAll Remove all
     */
    public void removeAllDisplays(boolean payAttentionToCanDoRemoveAll) {
        try {
            getVMManager().setDisplayMastersInactive();
            List tmp = getDisplayControls();
            displayControls = new ArrayList();
            //Tell each display control to clear. The false tells the display control
            //to not notify this ViewContext.
            ignoreRemoveDisplayControl = true;
            for (int i = 0; i < tmp.size(); i++) {
                DisplayControl dc = (DisplayControl) tmp.get(i);
                if ( !payAttentionToCanDoRemoveAll
                        || dc.getCanDoRemoveAll()) {
                    dc.doRemove();
                    getIdvUIManager().removeDisplayControl(dc);
                } else {
                    displayControls.add(dc);
                }
            }
            getVMManager().setDisplayMastersActive();
            ignoreRemoveDisplayControl = false;
            getIdvUIManager().displayControlsChanged();
            Runtime.getRuntime().gc();
        } catch (Throwable exc) {
            logException("removeAllDisplays", exc);
            exc.printStackTrace();
        }
    }




    /**
     * Implement the hyperlinklistener interface. This
     * gets called code that handles html. It gets the
     * URL from the event and calls {@link #handleAction(String,Hashtable)}.
     *
     * @param e The <code> HyperlinkEvent</code> event
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        hyperlinkUpdate(e, null);
    }


    /**
     * Handle the click
     *
     * @param e event
     * @param properties any properties
     */
    public void hyperlinkUpdate(HyperlinkEvent e, Hashtable properties) {
        if (e.getEventType() != HyperlinkEvent.EventType.ACTIVATED) {
            return;
        }
        String url;
        if (e.getURL() == null) {
            url = e.getDescription();
        } else {
            url = e.getURL().toString();
        }
        handleAction(url, properties);
    }







    /**
     *  This method checks if the given action is one of the following.
     * <p>
     *  <li> Jython code -- starts with jython:<br>
     *  <li> Help link -- starts with help:<br>
     *  <li> Resource bundle file -- ends with .rbi<br>
     *  <li> bundle file -- ends with .xidv<br>
     *  <li> jnlp file -- ends with .jnlp<br>
     *  It returns true if the action is one of these.
     *  False otherwise.
     *
     * @param action The string action
     * @param properties any properties
     * @return Was this action handled
     */
    protected boolean handleFileOrUrlAction(String action,
                                            Hashtable properties) {
        boolean ok     = false;
        boolean isFile = false;

        if (action.startsWith("idv:")) {
            action = StringUtil.replace(action, "&", "&amp;");
            action = action.substring("idv:".length());
            getJythonManager().evaluateUntrusted(action, properties);
            return true;
        }
        if (action.startsWith("jython:")) {
            getJythonManager().evaluateTrusted(action.substring(7),
                    properties);
            ok = true;
        } else if (action.startsWith("help:")) {
            getIdvUIManager().showHelp(action.substring(5));
            ok = true;
        } else if (getArgsManager().isRbiFile(action)) {
            loadRbiFile(action);
            ok     = true;
            isFile = true;
        } else if (getArgsManager().isXidvFile(action)
                   || getArgsManager().isZidvFile(action)) {
            //TODO: If this is asynch then when  do we add this to the history list
            getPersistenceManager().decodeXmlFile(action, true);
            //We handled it
            return true;
        } else if (getArgsManager().isIslFile(action)) {
            final String scriptFile = action;
            Misc.run(new Runnable() {
                public void run() {
                    getImageGenerator().processScriptFile(scriptFile);
                }
            });
            ok     = true;
            isFile = true;
        } else if (getArgsManager().isJnlpFile(action)) {
            getPersistenceManager().decodeJnlpFile(action);
            ok     = true;
            isFile = true;
        }
        if (isFile) {
            addToHistoryList(action);
        }
        if (ok) {
            //TODO            collabManager.write (collabManager.MSG_ACTION, action);
        }
        return ok;
    }



    /**
     * handle action
     *
     * @param action action
     *
     * @return handled action
     */
    public boolean handleAction(String action) {
        return handleAction(action, null);
    }


    /**
     *  This method  tries to handle the given action. The action may be
     *  a normal gui action (e.g., jython: or help:) or it may be a
     *  description of a datasource. The properties table is used
     *  to provide further information about this action.
     *
     * @param action The action (file, data source url, etc.)
     * @param properties Properties to pass to the data source creation
     * @return Was this action handled by this method.
     */
    public boolean handleAction(String action, Hashtable properties) {
        return handleAction(action, properties, true);
    }


    /**
     *  This method  tries to handle the given action. The action may be
     *  a normal gui action (e.g., jython: or help:), a file (e.g., test.xidv)
     *  or it may be a description of a datasource. The properties table is used
     *  to provide further information about this action.
     *
     * @param action The action (file, data source url, etc.)
     * @param properties Properties to pass to the data source creation
     * @param checkForAlias  Data sources can have aliases, (e.g., ETA means a dods url
     * pointing to the latest eta run on a remote server). This flag, if true,
     * has this routine check if the given action is indeed an alias.
     * @return Was this action handled by this method.
     */
    public boolean handleAction(String action, Hashtable properties,
                                boolean checkForAlias) {


        /**
         *  try {String tmp = null;tmp.length();} catch(Exception exc) {logException ("oops",exc);}
         */
        if (getIdvUIManager().isAction(action)) {
            String newAction = getIdvUIManager().getAction(action);
            if (newAction == null) {
                LogUtil.userMessage("Unknown action:" + action);
                return true;
            }
            action = newAction;
        }



        try {
            if (handleFileOrUrlAction(action, properties)) {
                return true;
            }
        } catch (IllegalStateException ise) {
            return false;
        }

        if (IOUtil.isHtmlFile(action)) {
            String execPath = getProperty("idv.browser.path", (String) null);
            if ((execPath != null) && (execPath.trim().length() > 0)) {
                try {
                    Process process = Runtime.getRuntime().exec(execPath
                                          + " " + action);
                    process.waitFor();
                } catch (Exception exc) {
                    logException("Executing the browser:" + execPath, exc);
                }

                return true;
            }
        }


        if (checkForAlias) {
            History history = History.findWithAlias(action.trim(),
                                  getHistory(), null);
            if (history != null) {
                return history.process(this);
            }
        }

        if (dataManager.validDatasourceId(action, properties)) {
            return makeDataSource(action, null, properties, true);
        }

        return false;
    }


    /**
     * Implementation of the <code>ActionListener</code> interface. This
     * passes the the action command from the given   <code>ActionEvent</code>
     * off to the handleAction method.
     *
     * @param event The   <code>ActionEvent</code>
     */
    public void actionPerformed(ActionEvent event) {
        handleAction(event.getActionCommand(), null);
    }






    /**
     *  Remove the data source from the DataManager and from any DataTree-s.
     *  Update the data menu in all menu bars.
     *
     * @param dataSource The data source to remove
     */
    public void removeDataSource(DataSource dataSource) {
        try {
            dataManager.removeDataSource(dataSource);
            if (collabManager != null) {
                collabManager.writeRemoveDataSource(dataSource);
            }
            getIdvUIManager().removeDataSource(dataSource);
        } catch (Exception exc) {
            logException("Removing data source:" + dataSource, exc);
        }
    }


    /**
     * Remove all current data sources. Update the GUI.
     */
    public void removeAllDataSources() {
        dataManager.removeAllDataSources();
        getIdvUIManager().removeAllDataSources();
    }

    /**
     * reload all data sources
     */
    public void reloadAllDataSources() {
        Misc.run(dataManager, "reloadAllDataSources");
    }


    /**
     * Wrapper method around
     * {@link DataManager#getDataSources()}.
     * Returns the list of {@link ucar.unidata.data.DataSource}-s
     * currently being used in the application.
     *
     * @return List of current data sources.
     */
    public List getDataSources() {
        return getDataManager().getDataSources();
    }


    /**
     * Gets all of the data sources. Normally, the method getDataSources,
     * only returns the data sources held by the DataManager. This method
     * also adds the Formula data source into the list.
     *
     * @return List of all current data sources.
     */

    public List getAllDataSources() {
        List dataSources = new ArrayList(getDataSources());
        if (getJythonManager().getDescriptorDataSource() != null) {
            dataSources.add(
                0, getIdv().getJythonManager().getDescriptorDataSource());
        }
        return dataSources;
    }



    /**
     * A helper method that will create a data source from the
     * given defining object (e.g., url, filename, collection
     * of images, etc.) and dataType, will show any
     * errors, and, if a data source was created will return it.
     *
     * @param definingObject Defines the data source. e.g., filename, url, list of images
     * @param dataType Defines the data source type id. From datasources.xml. If null
     *  then the data source type is found by checking for a pattern match on the
     * defining object from the patterns in datasources.xml
     * @param properties  Optional properties to pass to the data source. May also
     * contain  the data source type id.
     * @return The newly create data source or null if there was a failure.
     */
    public DataSource makeOneDataSource(Object definingObject,
                                        String dataType,
                                        Hashtable properties) {
        DataSourceResults results = createDataSource(definingObject,
                                        dataType, properties, true);
        getIdvUIManager().showResults(results);
        if (results.anyOk()) {
            return (DataSource) results.getDataSources().get(0);
        }
        return null;
    }


    /**
     *  Create the datasource, identified by the given dataType if non-null,
     *  with the given definingObject and properties.
     *
     * @param definingObject Defines the data source. e.g., filename, url, list of images
     * @param dataType Defines the data source type id. From datasources.xml. If null
     *  then the data source type is found by checking for a pattern match on the
     * defining object from the patterns in datasources.xml
     * @param properties  Optional properties to pass to the data source. May also
     * contain  the data source type id.
     *  @return Was the creation successfull.
     */
    public boolean makeDataSource(Object definingObject, String dataType,
                                  Hashtable properties) {
        return makeDataSource(definingObject, dataType, properties, true);
    }

    /**
     * Make the data source. This is a data source that does not need urls or files
     *
     * @param descriptor descriptor
     */
    public void makeDataSource(DataSourceDescriptor descriptor) {
        makeDataSource("", descriptor.getId(), new Hashtable());
    }

    /**
     *  Create the datasource, identified by the given dataType if non-null,
     *  with the given definingObject and properties.
     *
     * @param definingObject Defines the data source. e.g., filename, url, list of images
     * @param dataType Defines the data source type id. From datasources.xml. If null
     *  then the data source type is found by checking for a pattern match on the
     * defining object from the patterns in datasources.xml
     * @param properties  Optional properties to pass to the data source. May also
     * contain  the data source type id.
     * @param checkAlias If true then see if the given definineObject is a data source alias name.
     * @return Was the creation successfull.
     */
    public boolean makeDataSource(Object definingObject, String dataType,
                                  Hashtable properties, boolean checkAlias) {
        return makeDataSource(definingObject, dataType, properties,
                              checkAlias, null);
    }


    /**
     * Is the given object a file name of a bundle
     *
     * @param obj object
     *
     * @return is it a bundle
     */
    private boolean isABundle(Object obj) {
        return ((obj instanceof String)
                && (getArgsManager().isXidvFile((String) obj)
                    || getArgsManager().isZidvFile((String) obj)));
    }

    /**
     *  Create the datasource, identified by the given dataType if non-null,
     *  with the given definingObject and properties.
     *
     * @param definingObject Defines the data source. e.g., filename, url, list of images
     * @param dataType Defines the data source type id. From datasources.xml. If null
     *  then the data source type is found by checking for a pattern match on the
     * defining object from the patterns in datasources.xml
     * @param properties  Optional properties to pass to the data source. May also
     * contain  the data source type id.
     * @param checkAlias If true then see if the given definineObject is a data source alias name.
     * @param displayType If non-null hten also create this display
     * @return Was the creation successfull.
     */
    public boolean makeDataSource(Object definingObject, String dataType,
                                  Hashtable properties, boolean checkAlias,
                                  String displayType) {


        //Check for any bundles. Either the definingObject is a string or a list that contains strings
        List listOfBundles = new ArrayList();
        if (isABundle(definingObject)) {
            listOfBundles  = Misc.newList(definingObject);
            definingObject = null;
        } else if (definingObject instanceof List) {
            List tmp = (List) definingObject;
            definingObject = new ArrayList();
            for (int i = 0; i < tmp.size(); i++) {
                Object obj = tmp.get(i);
                if (isABundle(obj)) {
                    listOfBundles.add(obj);
                } else {
                    ((List) definingObject).add(obj);
                }
            }
            if (((List) definingObject).size() == 0) {
                definingObject = null;
            }
        }

        for (int i = 0; i < listOfBundles.size(); i++) {
            doOpen((String) listOfBundles.get(i));
        }
        if (definingObject == null) {
            return true;
        }


        DataSourceResults results = createDataSource(definingObject,
                                        dataType, properties, checkAlias);
        getIdvUIManager().showResults(results);
        if (results.allOk()) {
            if ( !results.anyOk()) {
                return false;
            }
        }

        if ( !results.allOk()) {
            return false;
        }

        if (displayType != null) {
            DisplayControl dc = createDisplay(results, "", displayType, "",
                                    true);
            return (dc != null);
        }


        return true;
    }


    /**
     * Move a history to the front of the list
     *
     * @param history  the history item to move
     */
    public void moveHistoryToFront(History history) {
        synchronized (MUTEX_HISTORY) {
            historyList.remove(history);
            historyList.add(0, history);
            writeHistoryList();
        }
    }

    /**
     *  This creates a new data source from the xml encoded representation
     * of a persisted data source. It is used in the data source history
     * mechanism.
     *
     * @param dataSourceXml The xml encoded data source representation
     * @return The results that hold the new data source.
     */
    public DataSourceResults makeDataSourceFromXml(String dataSourceXml) {
        return getPersistenceManager().makeDataSourceFromXml(dataSourceXml);
    }


    /**
     * Create the data source (or data sources) defined by the
     * given definingObject.
     *
     * @param definingObject Defines the data source. e.g., filename, url, list of images
     * @param dataType Defines the data source type id. From datasources.xml. If null
     *  then the data source type is found by checking for a pattern match on the
     * defining object from the patterns in datasources.xml
     * @param properties  Optional properties to pass to the data source. May also
     * contain  the data source type id.
     * @param checkAlias If true then see if the given definineObject is a data source alias name.
     * @return List of {@link ucar.unidata.data.DataSourceResults} that
     * hold the results of this method.
     */
    public DataSourceResults createDataSource(Object definingObject,
            String dataType, Hashtable properties, boolean checkAlias) {
        //First check if we have a history entry with the given alias.
        showWaitCursor();
        DataSourceResults results                   = null;
        boolean           createDataSourceFromAlias = false;
        if (checkAlias && (definingObject instanceof String)) {
            DataSourceHistory history =
                (DataSourceHistory) History.findWithAlias(
                    definingObject.toString().trim(), getHistory(),
                    DataSourceHistory.class);
            if (history != null) {
                results = makeDataSourceFromXml(history.getDataSourceXml());

            }
        }

        //No alias, then we create it directly
        if (results == null) {
            results = dataManager.createDataSource(definingObject, dataType,
                    properties);
        } else {
            createDataSourceFromAlias = true;
        }

        List dataSources = results.getDataSources();
        List data        = results.getSuccessData();

        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource     = (DataSource) dataSources.get(i);
            Object     dataSourceData = data.get(i);
            String     dataSourceXml  = encodeObject(dataSource, true);
            if (collabManager != null) {
                collabManager.write(collabManager.MSG_DATASOURCE,
                                    dataSourceXml);
            }
            //Don't add them to the history list if  we did create this from an alias.
            if ( !createDataSourceFromAlias) {
                String identifier = dataSource.getClass().getName() + "_"
                                    + encodeObject(dataSourceData, false);
                identifier =
                    new String(XmlUtil.encodeBase64(identifier.getBytes()));
                addToHistoryList(new DataSourceHistory(dataSource.toString(),
                        dataSourceXml, identifier));
            }
        }

        showNormalCursor();
        return results;
    }



    /**
     * Ask the user to select a data type for the given defining object
     *
     * @param definingObject The defining object for the data source
     *
     * @return The data type name or null
     */
    public String selectDataType(Object definingObject) {
        return selectDataType(
            definingObject,
            "<html>Unable to figure out how to read the data:<br>&nbsp;<p>&nbsp;&nbsp;&nbsp;<i>"
            + definingObject + "</i>"
            + "<br>&nbsp;<p>Please specify a data source type<br>&nbsp;</html>");
    }


    /**
     * Popup a dialog to allows the user to select data  source type
     *
     * @param definingObject the object to create the data source with
     * @param message message to show the user
     *
     * @return Selected data source type or null
     */
    public String selectDataType(Object definingObject, String message) {
        JComboBox dataSourcesCbx = IdvChooser.getDataSourcesComponent(false,
                                       getDataManager(), false);
        JLabel label    = new JLabel(message);
        JPanel contents = GuiUtils.vbox(
                              GuiUtils.inset(label, 5),
                              GuiUtils.inset(
                                  GuiUtils.left(
                                      GuiUtils.label(
                                          "Data Source Type:  ",
                                          dataSourcesCbx)), 5));

        contents = GuiUtils.inset(contents, 5);
        if ( !GuiUtils.showOkCancelDialog(null, "Select data source type",
                                          contents, null, null)) {
            return null;
        }
        Object selected = dataSourcesCbx.getSelectedItem();

        if ((selected != null) && (selected instanceof TwoFacedObject)) {
            return (String) ((TwoFacedObject) selected).getId();
        }
        return (String) selected;
    }

    /**
     * Load in a {@link ucar.unidata.data.DataSource}. Called from the
     * {@link ucar.unidata.data.DataManager} when a new data source has
     * been created or unpersisted.
     *
     * @param dataSource  Data source to load in.
     * @return If true then signals that the given data source should be
     * kept around in the list of data sources. False says to not keep it around
     * in the list.
     */
    public boolean loadDataSource(DataSource dataSource) {
        boolean keepDataSource = true;
        try {
            if (dataSource == null) {
                return false;
            }
            String dataType = dataSource.getTypeName();
            LogUtil.message("Loading  data:" + dataSource.getName());
            //Check if any of the default display controls are applicable
            if ( !getProperty(PROP_LOADINGXML, false)) {
                if (getStore().get(PREF_AUTODISPLAYS_ENABLE, true)) {
                    JDialog notifyWindow = null;
                    List    pairs        =
                        getAutoDisplayEditor().getDisplaysForDataSource(
                            dataSource);
                    for (int i = 0; i < pairs.size(); i += 2) {
                        DataChoice        dc = (DataChoice) pairs.get(i);
                        ControlDescriptor cd =
                            (ControlDescriptor) pairs.get(i + 1);
                        doMakeControl(dc, cd, NULL_STRING);
                        if (false && (notifyWindow == null)
                                && getStore().get(PREF_AUTODISPLAYS_SHOWGUI,
                                    true)) {
                            notifyWindow = new JDialog((Frame) null,
                                    "Automatic display creation");
                            JLabel notifyLabel =
                                new JLabel("Creating automatic display");
                            notifyWindow.getContentPane().add(notifyLabel);
                            GuiUtils.showInCenter(notifyWindow);
                        }
                    }
                    if (notifyWindow != null) {
                        //                        notifyWindow.dispose();
                    }
                }
                for (int pIdx = 0; pIdx < argsManager.initParams.size();
                        pIdx++) {
                    DataChoice choice = dataSource.findDataChoice(
                                            argsManager.initParams.get(pIdx));
                    if (choice == null) {
                        continue;
                    }
                    ControlDescriptor cd = getControlDescriptor(
                                               argsManager.initDisplays.get(
                                                   pIdx).toString());
                    if (cd != null) {
                        doMakeControl(choice, cd, NULL_STRING);
                    } else {
                        log_.error("Unknown display specifier:"
                                   + argsManager.initDisplays.get(pIdx));
                    }
                }
                if (dataType != null) {
                    createDefaultDisplays(dataType, dataSource);
                }
            }

            if ((dataType == null)
                    || getDataManager().getProperty(dataType,
                        DataManager.PROP_SHOW_IN_TREE, true)) {
                getIdvUIManager().addDataSource(dataSource);
            } else {
                keepDataSource = false;
                //If we don't put it into the tree then remove it
                //getDataManager ().removeDataSource (dataSource);
            }
        } catch (Throwable exp) {
            logException("loadDataSource", exp);
        }

        LogUtil.clearMessage("Loading  data:" + dataSource.getName());
        return keepDataSource;
    }


    /**
     * Get the max number of threads to be used when rendering in visad
     *
     * @return max threads for rendering
     */
    public int getMaxRenderThreadCount() {
        return getStore().get(PREF_THREADS_RENDER,
                              Runtime.getRuntime().availableProcessors());
    }

    /**
     * Get the max number of threads to be used when reading data
     *
     * @return max threads for reading data
     */
    public int getMaxDataThreadCount() {
        return getStore().get(PREF_THREADS_DATA, 4);
    }

    /**
     * Get the max size of the perm gen space
     *
     * @return max size of perm gen space
     */
    public int getMaxPermGenSize() {
        return getStore().get(PREF_MAX_PERMGENSIZE, DEFAULT_MAX_PERMGENSIZE);
    }

    /**
     * Get whether we are using the time driver facility
     *
     * @return true if using time matching
     */
    public boolean getUseTimeDriver() {
        return getProperty(PROP_USE_TIMEDRIVER, false);
    }


    /**
     * Create any default displays defined for the datasource descriptor
     *
     * @param dataType The data type
     * @param dataSource The data source
     */
    private void createDefaultDisplays(String dataType,
                                       DataSource dataSource) {
        String defaultDisplay = (String) dataSource.getProperty(
                                    DataSource.PROP_AUTOCREATEDISPLAY);
        if (defaultDisplay == null) {
            defaultDisplay = dataManager.getProperty(dataType,
                    DataManager.PROP_DEFAULT_DISPLAY);
        }
        if (defaultDisplay != null) {
            dataSource.createAutoDisplay(defaultDisplay, this);
        }

    }



    /**
     * This method loads in the data,  creates the display from the
     * given paramName and writes out a screen image.
     *
     * @param imageName The file name of the image
     * @param dataSourceName The data source to load (e.g., a filename or url)
     * @param paramName The parameter to create the display with.
     * @param displayName The id of the display. From controls.xml
     */
    public void createImage(String imageName, String dataSourceName,
                            String paramName, String displayName) {
        DisplayControl displayControl = createDisplay(dataSourceName,
                                            paramName, displayName, null);
        if (displayControl == null) {
            LogUtil.userMessage("Unknown display type: " + displayName);
            return;
        }
        displayControl.saveImage(imageName);
    }

    /**
     * Create a display from the given datasource/paramname pair.
     *
     * @param dataSourceName The data source to load (e.g., a filename or url)
     * @param paramName The parameter to create the display with.
     * @param displayName The id of the display. From controls.xml
     * @param properties Semi-color delimited list of name=value properties to pass to the display.
     * @return The newly created {@link DisplayControl}
     */
    public DisplayControl createDisplay(String dataSourceName,
                                        String paramName, String displayName,
                                        String properties) {
        return createDisplay(dataSourceName, paramName, displayName,
                             properties, true);
    }

    /**
     * Create a display from the given datasource/paramname pair.
     *
     * @param dataSourceName The data source to load (e.g., a filename or url)
     * @param paramName The parameter to create the display with.
     * @param displayName The id of the display. From controls.xml
     * @param properties Semi-color delimited list of name=value properties to pass to the display.
     * @param initDisplayInThread If true then initialize the display in a separate thread
     * @return The newly created {@link DisplayControl}
     */
    public DisplayControl createDisplay(String dataSourceName,
                                        String paramName, String displayName,
                                        String properties,
                                        boolean initDisplayInThread) {
        try {
            DataSourceResults results =
                dataManager.createDataSource(dataSourceName);
            if (results.anyFailed()) {
                getIdvUIManager().showResults(results);
                return null;
            }

            return createDisplay(results, paramName, displayName, properties,
                                 initDisplayInThread);

        } catch (Throwable excp) {
            throw new IllegalArgumentException("Error creating display:"
                    + excp);
        }

    }



    /**
     * Create the display from the data source
     *
     * @param results Holds the data sources
     * @param paramName The parameter to create the display with.
     * @param displayName The id of the display. From controls.xml
     * @param properties Semi-color delimited list of name=value properties to pass to the display.
     * @param initDisplayInThread If true then initialize the display in a separate thread
     * @return The newly created {@link DisplayControl}
     */
    private DisplayControl createDisplay(DataSourceResults results,
                                         String paramName,
                                         String displayName,
                                         String properties,
                                         boolean initDisplayInThread) {

        try {
            List dataSources = results.getDataSources();
            if (dataSources.size() == 0) {
                throw new IllegalArgumentException(
                    "Unable to load data source:");
            }
            //For now just pull out the first DataSource in the 
            //list (Probably when this method is called there will only be 1 datasource created)
            DataSource dataSource = (DataSource) dataSources.get(0);

            DataChoice dataChoice = dataSource.findDataChoice(paramName);
            if (dataChoice == null) {
                throw new IllegalArgumentException(
                    "Unable to find parameter:" + paramName);
            }

            ControlDescriptor desc = getControlDescriptor(displayName);
            if (desc == null) {
                throw new IllegalArgumentException("Unknown display:"
                        + displayName);
            }
            return doMakeControl(Misc.newList(dataChoice), desc, properties,
                                 NULL_DATA_SELECTION, initDisplayInThread);
        } catch (Throwable excp) {
            throw new IllegalArgumentException("Error creating display:"
                    + excp);
        }
    }





    /**
     *  Implementation of the ControlContext method.
     *  If the idv has been initialized then this simply shows the
     *  window. If not yet fully initialized then we place this window
     *  in a list of windows to be displayed after initialization is done.
     *
     * @param control The {@link DisplayControl} whose window we should show.
     * @param window The window
     */
    public void showWindow(DisplayControl control, IdvWindow window) {
        getIdvUIManager().showWindow(control, window);
    }




    /**
     * Get the background images resource
     *
     * @return  the resource
     */
    public List getBackgroundImages() {
        if (backgroundImages == null) {
            backgroundImages = WmsSelection.parseWmsResources(
                getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_BACKGROUNDWMS));
        }
        return backgroundImages;
    }


    /**
     * Make the background wms image
     */
    public void doMakeBackgroundImage() {
        //If we failed to make it it might mean we already have one around

        DataSource dataSource  = null;
        List       dataSources = getAllDataSources();
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource tmp = (DataSource) dataSources.get(i);
            if ( !(tmp instanceof WmsDataSource)) {
                continue;
            }
            WmsDataSource wmsDataSource = (WmsDataSource) tmp;
            if ( !Misc.equals(getBackgroundImages(),
                              wmsDataSource.getWmsSelections())) {
                continue;
            }
            dataSource = tmp;
            break;
        }

        if (dataSource == null) {
            Hashtable properties = Misc.newHashtable(DataSource.PROP_TITLE,
                                       "Background images");
            dataSource = makeOneDataSource(getBackgroundImages(), "WMS",
                                           properties);
        } else {
            createDefaultDisplays("WMS", dataSource);
        }
    }


    /**
     * Create the {@link DisplayControl}, identified by the given
     * {@link ControlDescriptor} for the given {@link ucar.unidata.data.DataChoice}
     *
     * @param dataChoice The data choice.
     * @param descriptor Defines the control to create.
     * @param properties Semi-colon delimited list of name=value properties to pass to the control.
     * @return The newly create display control or null if it fails.
     */
    public DisplayControl doMakeControl(DataChoice dataChoice,
                                        ControlDescriptor descriptor,
                                        String properties) {
        if (dataChoice == null) {
            return null;
        }
        return doMakeControl(Misc.newList(dataChoice), descriptor,
                             properties, null, true);
    }

    /**
     * This method is called by the helptips, passing in the name
     * of the display control (from controls.xml). It creates
     * {@link DisplayControl} without any data.
     *
     * @param controlName Id of the display control
     * @return The newly created display control.
     */
    public DisplayControl doMakeControl(String controlName) {
        ControlDescriptor cd = getControlDescriptor(controlName);
        if (cd != null) {
            return doMakeControl(new ArrayList(), cd);
        }
        return null;
    }


    /**
     * make a control
     *
     * @param controlName name of control
     * @param dataChoice with data choice
     *
     * @return control
     */
    public DisplayControl doMakeControl(String controlName,
                                        DataChoice dataChoice) {

        return doMakeControl(controlName, Misc.newList(dataChoice));
    }

    /**
     * make a control
     *
     * @param controlName name of control
     * @param dataChoices list of data choices
     *
     * @return control
     */
    public DisplayControl doMakeControl(String controlName,
                                        List dataChoices) {
        ControlDescriptor cd = getControlDescriptor(controlName);
        if (cd != null) {
            return doMakeControl(dataChoices, cd);
        }
        return null;
    }



    /**
     * Create the {@link DisplayControl}, identified by the given
     * {@link ControlDescriptor} for the given list of
     * {@link ucar.unidata.data.DataChoice}s
     *
     * @param dataChoices The list of data choices.
     * @param descriptor Defines the control to create.
     * @return The newly create display control or null if it fails.
     */
    public DisplayControl doMakeControl(List dataChoices,
                                        ControlDescriptor descriptor) {
        return doMakeControl(dataChoices, descriptor, (String) null, null);
    }



    /**
     * Create the {@link DisplayControl}, identified by the given
     * {@link ControlDescriptor} for the given {@link ucar.unidata.data.DataChoice}
     *
     * @param dataChoice The data choice.
     * @param descriptor Defines the control to create.
     * @param properties Semi-colon delimited list of name=value properties to pass to the control.
     * @param dataSelection Holds any user specified subsetting of data (e.g., times list).
     * @return The newly create display control or null if it fails.
     */
    public DisplayControl doMakeControl(DataChoice dataChoice,
                                        ControlDescriptor descriptor,
                                        String properties,
                                        DataSelection dataSelection) {
        if ((dataChoice == null) || (descriptor == null)) {
            return null;
        }
        return doMakeControl(Misc.newList(dataChoice), descriptor,
                             properties, dataSelection, true);
    }



    /**
     * Create the {@link DisplayControl}, identified by the given
     * {@link ControlDescriptor} for the given list of
     * {@link ucar.unidata.data.DataChoice}s
     *
     * @param dataChoices List of data choices.
     * @param descriptor Defines the control to create.
     * @param propertiesString Semi-colon delimited list of
     *        name=value properties to pass to the control.
     * @param dataSelection Holds any user specified subsetting of data
     *        (e.g., times list).
     * @return The newly create display control or null if it fails.
     */
    public DisplayControl doMakeControl(List dataChoices,
                                        ControlDescriptor descriptor,
                                        String propertiesString,
                                        DataSelection dataSelection) {
        Hashtable properties = ((propertiesString == null)
                                ? null
                                : StringUtil.parsePropertiesString(
                                    propertiesString));
        return doMakeControl(dataChoices, descriptor, properties,
                             dataSelection);
    }


    /**
     * Create the {@link DisplayControl}, identified by the given
     * {@link ControlDescriptor} for the given list of
     * {@link ucar.unidata.data.DataChoice}s
     *
     * @param dataChoices List of data choices.
     * @param descriptor Defines the control to create.
     * @param properties Hashtable of properties
     * @param dataSelection Holds any user specified subsetting of data
     *        (e.g., times list).
     * @return The newly create display control or null if it fails.
     */
    public DisplayControl doMakeControl(List dataChoices,
                                        ControlDescriptor descriptor,
                                        Hashtable properties,
                                        DataSelection dataSelection) {
        return doMakeControl(dataChoices, descriptor, properties,
                             dataSelection, true);
    }


    /**
     * Finally, we really create the {@link DisplayControl}, identified by the given
     * {@link ControlDescriptor} using  the given list of
     * {@link ucar.unidata.data.DataChoice}s
     *
     * @param dataChoices List of data choices.
     * @param descriptor Defines the control to create.
     * @param propertiesString Semi-colon delimited list of
     *        name=value properties to pass to the control.
     * @param dataSelection Holds any user specified subsetting of data
     *        (e.g., times list).
     * @param initDisplayInThread If true then do the display control
     *        initialization in a thread.
     * @return The newly create display control or null if it fails.
     */
    public DisplayControl doMakeControl(final List dataChoices,
                                        final ControlDescriptor descriptor,
                                        final String propertiesString,
                                        final DataSelection dataSelection,
                                        boolean initDisplayInThread) {
        Hashtable properties = ((propertiesString == null)
                                ? null
                                : StringUtil.parsePropertiesString(
                                    propertiesString));
        return doMakeControl(dataChoices, descriptor, properties,
                             dataSelection, initDisplayInThread);
    }

    /**
     * Finally, we really create the {@link DisplayControl}, identified by
     * the given * {@link ControlDescriptor} using  the given list of
     * {@link ucar.unidata.data.DataChoice}s
     *
     * @param dataChoices List of data choices.
     * @param descriptor Defines the control to create.
     * @param properties Hashtable of properties
     * @param dataSelection Holds any user specified subsetting of data
     *        (e.g., times list).
     * @param initDisplayInThread If true then do the display control
     *        initialization in a thread.
     * @return The newly create display control or null if it fails.
     */
    public DisplayControl doMakeControl(final List dataChoices,
                                        final ControlDescriptor descriptor,
                                        final Hashtable properties,
                                        final DataSelection dataSelection,
                                        boolean initDisplayInThread) {

        if ((dataChoices == null) || (descriptor == null)) {
            return null;
        }
        LogUtil.message("Creating control: " + descriptor);
        DisplayControl control = null;
        try {
            control = descriptor.doMakeDisplay(dataChoices, this, properties,
                    dataSelection, initDisplayInThread);
        } catch (Throwable exp) {
            logException("doMakeControl", exp);
        }
        LogUtil.clearMessage("Creating control: " + descriptor);
        return control;
    }



    /**
     * Return the xml representation of the given object. If
     * prettyPrint is true then format the xml with tabs, etc.
     *
     * @param object The object to xmlize
     * @param prettyPrint Should the string xml be formatted
     * @return Xml representation of the given object
     */
    public String encodeObject(Object object, boolean prettyPrint) {
        return XmlUtil.toString(getEncoderForWrite().toElement(object),
                                prettyPrint);
    }

    /**
     * Create and return the Object defined by the given xml.
     *
     * @param xml The xml representation of an object
     * @return The Object
     * @throws Exception
     */
    public Object decodeObject(String xml) throws Exception {
        return (getEncoderForRead().toObject(xml));
    }



    /**
     * Empty the history list and write it out.
     */
    public void clearHistoryList() {
        synchronized (MUTEX_HISTORY) {
            historyList = new ArrayList();
            writeHistoryList();
        }
    }


    /**
     * Create, if needed, and return the history list. This is
     * the list of {@link History} objects that that define the
     * files and data sources the user has loaded in the past.
     *
     * @return History list
     */
    public List getHistory() {
        synchronized (MUTEX_HISTORY) {
            if (historyList == null) {
                try {
                    List tmp = (List) getStore().getEncodedFile(PREF_HISTORY);
                    historyList = new ArrayList();
                    if (tmp == null) {
                        tmp = new ArrayList();
                    }
                    //We had a case where the history list held a null value.
                    //Not sure how that happened but put this check in
                    for (Object o : tmp) {
                        if (o != null) {
                            historyList.add(o);
                        }
                    }
                } catch (Exception exc) {
                    logException("Creating history list", exc);
                }
            }
            return new ArrayList(historyList);
        }
    }


    /**
     * Add the given file to the history list.
     *
     * @param filename The file name to add
     */
    public void addToHistoryList(String filename) {
        addToHistoryList(new FileHistory(filename));
    }

    /**
     * Persist the history list into its own file.
     */
    public void writeHistoryList() {
        synchronized (MUTEX_HISTORY) {
            getStore().putEncodedFile(PREF_HISTORY, historyList);
        }
        QuicklinkPanel.updateHistoryLinks();
    }


    /**
     * Add the given {@link History} object into the history list.
     * This also writes out the history list.
     *
     * @param newHistory The History object to add.
     */
    public void addToHistoryList(History newHistory) {
        getHistory();
        synchronized (MUTEX_HISTORY) {
            while (historyList.contains(newHistory)) {
                historyList.remove(newHistory);
            }
            for (int i = 0; i < historyList.size(); i++) {
                Object obj = historyList.get(i);
                obj.equals(newHistory);
            }
            //Only keep the last 20 files in the list (But keep all that have an alias)
            List tmpList = new ArrayList(historyList);
            for (int i = tmpList.size() - 1; i >= 0; i--) {
                if (historyList.size() < 20) {
                    break;
                }
                History history = (History) tmpList.get(i);
                if ( !history.hasAlias()) {
                    historyList.remove(history);
                    i--;
                }
            }
            historyList.add(0, newHistory);
            writeHistoryList();
        }
    }


    /**
     * Have the user select an xidv filename and
     * write the current application state to it.
     * This also sets the current file name and
     * adds the file to the history list.
     */
    public void doSaveAs() {
        Misc.run(new Runnable() {
            public void run() {
                getPersistenceManager().doSaveAs();
            }
        });
    }


    /**
     * Save the current state off to the current xidv filename
     */
    public void doSave() {
        Misc.run(new Runnable() {
            public void run() {
                getPersistenceManager().doSave();
            }
        });
    }

    /**
     *  Called from the menu command to save the current state as the default bundle
     */
    public void doSaveAsDefault() {
        Misc.run(new Runnable() {
            public void run() {
                getPersistenceManager().doSaveAsDefault();
            }
        });
    }

    /**
     *  Called from the menu command to open the default bundle
     */
    public void doOpenDefault() {
        getPersistenceManager().doOpenDefault();
    }

    /**
     * Have the user select an xidv file. If andRemove is
     * true then we remove all data sources and displays.
     * Then we open the unpersist the bundle in the xidv  file
     *
     *
     * @param filename The filename to open
     * @param checkUserPreference Should we show, if needed,  the Open dialog
     * @param andRemove If true then first remove all data sources and displays
     */
    public void doOpen(final String filename,
                       final boolean checkUserPreference,
                       final boolean andRemove) {
        //For now don't do this  in a thread.
        //        Misc.run(new Runnable(){
        //                public void run() {
        doOpenInThread(filename, checkUserPreference, andRemove);
        //                }
        //            });
    }



    /**
     * Did the user select to change the  data paths when loading in a bundle
     *
     * @return change data in loaded bundles
     */
    public boolean getChangeDataPaths() {
        return getChangeDataPathCbx().isSelected();
    }

    /**
     * Get the checkbox to show to change data
     *
     * @return change data cbx
     */
    public JCheckBox getChangeDataPathCbx() {
        if (overwriteDataCbx == null) {
            overwriteDataCbx = new JCheckBox("Change data paths", false);
            overwriteDataCbx.setToolTipText(
                "Change the file paths that the data sources use");
        }
        return overwriteDataCbx;
    }

    /**
     * Have the user select an xidv file. If andRemove is
     * true then we remove all data sources and displays.
     * Then we open the unpersist the bundle in the xidv  file
     *
     *
     * @param filename The filename to open
     * @param checkUserPreference Should we show, if needed,  the Open dialog
     * @param andRemove If true then first remove all data sources and displays
     */
    private void doOpenInThread(String filename, boolean checkUserPreference,
                                boolean andRemove) {
        boolean overwriteData = false;
        if (filename == null) {
            filename = FileManager.getReadFile(
                "Open File",
                Misc.newList(
                    getArgsManager().getXidvZidvFileFilter(), FILTER_JNLP,
                    FILTER_ISL), GuiUtils.top(getChangeDataPathCbx()));
            if (filename == null) {
                return;
            }
            overwriteData = getChangeDataPathCbx().isSelected();
        }

        if (getArgsManager().isXidvFile(filename)) {
            getPersistenceManager().decodeXmlFile(filename,
                    checkUserPreference, overwriteData);
            return;
        }
        handleAction(filename, null);
    }


    /**
     * Have the user select an xidv bundle file, remove all data sources and displays,
     * and then unpersist the  bundle.
     */
    public void doOpen() {
        //filename, checkWithUser, doRemove
        doOpen(null, true, true);
    }

    /**
     * Load in the given bundle. Check to see if we should also remove the current state.
     *
     * @param bundleUri The filename or url of the bundle
     */
    public void doOpen(String bundleUri) {
        doOpen(bundleUri, true, true);
    }

    /**
     * Have the user select an xidv bundle file and then unpersist the  bundle.
     * This does not remove the existing data sources and displays.
     */
    public void doImport() {
        doOpen(null, false, false);
    }


    /**
     *  Called from the menu command to clear the default bundle
     */
    public void doClearDefaults() {
        if ( !GuiUtils.showYesNoDialog(
                null, "Are you sure you want to delete your default bundle?",
                "Delete confirmation")) {
            return;
        }
        resourceManager.clearDefaultBundles();
    }


    /**
     * Create a new {@link  ucar.unidata.xml.XmlEncoder} for doing
     * unpersisting.
     *
     * @return The encoder used for reading xml encoded objects
     */
    public XmlEncoder getEncoderForRead() {
        return getEncoder(true);
    }

    /**
     * Create a new {@link  ucar.unidata.xml.XmlEncoder} for doing
     * persisting.
     *
     * @return The encoder used for writing xml encoded objects
     */
    public XmlEncoder getEncoderForWrite() {
        return getEncoder(false);
    }


    /**
     * Get the XmlEncoder for this instance
     *
     * @return  the encoder
     */
    protected XmlEncoder getEncoder() {
        return getEncoder(true);
    }


    /**
     *  Create an {@link ucar.unidata.xml.XmlEncoder} and initialize it
     * with the VisADPersistence delegates.
     *
     * @param forRead If true then we initialize the encoder for reading the xml
     * @return The new encoder
     */
    protected XmlEncoder getEncoder(boolean forRead) {
        XmlEncoder encoder = new XmlEncoder();
        //We moved some classes around
        encoder.defineObjectId(this, ID_IDV);
        if (dataManager != null) {
            dataManager.initEncoder(encoder, forRead);
        }
        //For now put this here
        encoder.registerNewClassName(
            "ucar.unidata.repository.InteractiveRepositoryClient",
            "org.ramadda.repository.client.InteractiveRepositoryClient");

        encoder.registerNewClassName(
            "org.ramadda.repository.InteractiveRepositoryClient",
            "org.ramadda.repository.client.InteractiveRepositoryClient");

        encoder.registerNewClassName(
            "ucar.unidata.idv.FlythroughPoint",
            "ucar.unidata.idv.flythrough.FlythroughPoint");

        //A hack to support the new geon package structure
        //Sometime we need to have this be a property or some xml format
        encoder.addClassPatternReplacement("ucar.unidata.apps.geon",
                                           "org.unavco.idv.geon");
        encoder.addClassPatternReplacement("ucar.unidata.repository",
                                           "org.ramadda.repository");
        encoder.addClassPatternReplacement(
            "org.ramadda.repository.idv.RamaddaPublisher",
            "org.ramadda.geodata.publisher.RamaddaPublisher");


        VisADPersistence.init(encoder);
        initEncoder(encoder, forRead);
        return encoder;
    }

    /**
     * A hook so derived classes can add their own initialization to the
     * given encoder.
     *
     * @param encoder The encoder to initialize
     * @param forRead If true then we initialize the encoder for reading the xml
     */
    protected void initEncoder(XmlEncoder encoder, boolean forRead) {}



    /**
     * Called to end execution of this process.
     * This method must be evoked from the event-dispatching thread. (why?)
     *
     * @return Did the user really want to quit. Of course, if the user did
     * quit then we exit the app and never return true anyway.
     */
    public boolean quit() {
        if (getStore().get(PREF_SHOWQUITCONFIRM, true)) {
            JCheckBox  cbx  = new JCheckBox("Always ask", true);
            JComponent comp =
                GuiUtils
                    .vbox(new JLabel(
                        "<html><b>Do you really want to exit?</b></html>"), GuiUtils
                            .inset(cbx, new Insets(4, 15, 0, 10)));
            int result =
                JOptionPane.showConfirmDialog(LogUtil.getCurrentWindow(),
                    comp, "Exit Confirmation", JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                return false;
            }
            getStore().put(PREF_SHOWQUITCONFIRM, cbx.isSelected());
        }
        StationModelManager smm = getStationModelManager();
        if ( !smm.checkCloseWindow()) {
            return false;
        }

        if ( !getJythonManager().saveOnExit()) {
            return false;
        }


        getStore().saveIfNeeded();
        getStore().cleanupTmpFiles();
        getPluginManager().closeResources();
        getJythonManager().applicationClosing();
        if (interactiveMode) {
            exit(0);
        }
        return true;
    }



    /**
     * Exit the application. By default this calls System.exit. However, a custom
     * application could override this method.
     *
     * @param exitCode System exit code to use
     */
    protected void exit(int exitCode) {
        System.exit(exitCode);
    }


    /**
     *  Show the error message console. We have this as a method so it can be called via jython.
     */
    public void showConsole() {
        LogUtil.showConsole();
    }



    /**
     *  Helper method that calls LogUtil.printException
     *
     * @param msg The error message
     * @param exc The exception
     */
    public void logException(String msg, Throwable exc) {
        LogUtil.printException(log_, msg, exc);
    }


    /**
     *  Pass through to {@link ucar.unidata.idv.ui.IdvUIManager#showWaitCursor}
     */
    public void showWaitCursor() {
        getIdvUIManager().showWaitCursor();
    }



    /**
     *  Pass through to {@link ucar.unidata.idv.ui.IdvUIManager#showNormalCursor}
     */
    public void showNormalCursor() {
        getIdvUIManager().showNormalCursor();
    }


    /**
     *  Pass through to {@link ucar.unidata.idv.ui.IdvUIManager#clearWaitCursor}
     */
    public void clearWaitCursor() {
        getIdvUIManager().clearWaitCursor();
    }

    /**
     * close current window
     */
    public void closeCurrentWindow() {
        getIdvUIManager().closeCurrentWindow();
    }


    /**
     * The user clicked on the wait label
     */
    public void waitLabelClicked() {
        clearWaitCursor();
        JobManager.getManager().stopAllLoads();
    }


    /**
     *  Default main implementation. Just turn around and call DefaultIdv.main.
     *
     * @param args Command line arguments.
     *
     * @throws Exception When something bad happends
     */
    public static void main(String[] args) throws Exception {
        /*
        try {
            for(int i=0;i<5;i++) {
                System.err.println ("Test call " + i);
                processScript(args[0]);
            }
            System.exit(0);
        } catch(Throwable exc) {
            Throwable wrappedExc = LogUtil.getInnerException(exc);
            if(wrappedExc!=null) exc = wrappedExc;
            exc.printStackTrace();
        }
        */
        DefaultIdv.main(args);
    }


    /**
     * Get the image from the given isl script. This is called by other
     * code directly and runs the idv in non-interactive mode
     *
     * @param scriptFile The path to the isl script
     *
     * @throws Exception On badness
     */
    public static void processScript(String scriptFile) throws Exception {
        IntegratedDataViewer idv = new IntegratedDataViewer(false);
        try {
            idv.getImageGenerator().processScriptFile(scriptFile);
        } finally {
            idv.cleanup();
        }
    }


    /**
     * Remove all state, etc.
     */
    public void cleanup() {
        getStore().cleanupTmpFiles();
        removeAllDisplays();
        idv.removeAllDataSources();
        getVMManager().removeAllViewManagers();
        getIdvUIManager().disposeAllWindows();
        getIdvUIManager().clearWaitCursor();
        CacheManager.clearCache();
    }

    /**
     * Call CacheManager.printStats
     */
    public void printCache() {
        CacheManager.printStats();
    }



    /**
     * Get image
     *
     * @param bundle bundle
     *
     * @return image
     *
     * @throws Exception On badness
     */
    protected byte[] getStaticImage(String bundle) throws Exception {

        Trace.call1("getStaticImage-decode");
        getPersistenceManager().decodeXmlFile(bundle, false);
        Trace.call2("getStaticImage-decode");

        Trace.call1("getStaticImage-capture");
        getImageGenerator().captureImage("test.png");
        Trace.call2("getStaticImage-capture");

        removeAllDisplays();
        removeAllDataSources();
        getVMManager().removeAllViewManagers();
        CacheManager.clearCache();
        FileInputStream fis   = new FileInputStream("test.png");
        byte[]          bytes = IOUtil.readBytes(fis);
        fis.close();


        return bytes;
    }




    /**
     * Print cache statistics
     */
    public void printCacheStats() {
        CacheManager.printStats();
    }

    /**
     * Print the data cache stats
     */
    public void printDataCacheStats() {
        visad.data.DataCacheManager.getCacheManager().printStats();
    }

    /**
     * Flush the data cache
     */
    public void flushDataCache() {
        visad.data.DataCacheManager.getCacheManager().flushAllCachedData();
    }


    /**
     * Apply preferences.
     */
    public void applyPreferences() {
        getStateManager().applyPreferences();
        getVMManager().applyPreferences();
        getPluginManager().applyPreferences();
        List l = getDisplayControls();
        if ((l == null) || l.isEmpty()) {
            return;
        }
        for (Iterator iter = l.iterator(); iter.hasNext(); ) {
            ((DisplayControl) iter.next()).applyPreferences();
        }
    }





    /**
     * startup and run the image server
     *
     * @param port what port to listen on
     * @param propertyFile properties - may be null
     */
    protected void runImageServer(int port, String propertyFile) {
        LogUtil.setTestMode(true);
        if (propertyFile != null) {
            imageServer = new ImageServer(this, propertyFile);
        } else {
            imageServer = new ImageServer(this, port);
        }
        getArgsManager().setIsOffScreen(true);
        imageServer.init();
    }




    /**
     * Implement the LogUtil.DialogManager interface to add buttons to the dialog.
     * This adds the 'Support Form' button.
     *
     * @param dialog The dialog to add to.
     * @param buttonList The list of buttons to add my button into.
     * @param msg The error message
     * @param exc The exception that had been thrown.
     */
    public void addErrorButtons(final JDialog dialog, List buttonList,
                                final String msg, final Throwable exc) {
        JButton supportBtn = new JButton("Support Form");
        supportBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                getIdvUIManager().showSupportForm(msg,
                        LogUtil.getStackTrace(exc), dialog);
            }
        });
        buttonList.add(supportBtn);

    }

    /**
     * Utility to list the public api of the given object
     *
     * @param o object
     *
     * @return api
     */
    public String listApi(Object o) {
        StringBuffer sb = new StringBuffer();

        if (o instanceof DisplayControl) {
            DisplaySettingsDialog dsd = new DisplaySettingsDialog(this,
                                            (DisplayControlImpl) o, false);
            List props = dsd.getPropertyValues();
            for (int i = 0; i < props.size(); i++) {
                PropertyValue p    = (PropertyValue) props.get(i);
                Object        v    = p.getValue();
                String        type = "n/a";
                if (v != null) {
                    type = v.getClass().getName();
                }
                sb.append(Misc.getSetterMethod(p.getName()) + "(" + type
                          + ")  = " + DisplaySettingsDialog.getValueLabel(v)
                          + "<br>");
            }
            return sb.toString();
        }

        List methods = XmlEncoder.findPropertyMethods(o.getClass(), false);
        for (int i = 0; i < methods.size(); i++) {
            Method m = (Method) methods.get(i);
            sb.append(m.getName());
            sb.append("<br>");
        }
        return sb.toString();
    }



}
