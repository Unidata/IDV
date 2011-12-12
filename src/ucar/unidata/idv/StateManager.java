/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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


import org.w3c.dom.Element;

import ucar.unidata.data.*;
import ucar.unidata.idv.ui.*;

import ucar.unidata.ui.Help;

import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;


import ucar.unidata.xml.*;

import java.awt.Color;
import java.awt.Dimension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;




/**
 * This class manages the intialize properties that configure
 * the IDV and the persistent store for writing preferences, etc.
 * to the user's local .unidata/idv directory
 *
 * @author IDV development team
 */


public class StateManager extends IdvManager {

    /** Major version xml attribute */
    private static final String PROP_VERSION_MAJOR = "idv.version.major";

    /** Minor version xml attribute */
    private static final String PROP_VERSION_MINOR = "idv.version.minor";

    /** Minor version xml attribute */
    private static final String PROP_BUILD_DATE = "idv.build.date";

    /** Revision version xml attribute */
    private static final String PROP_VERSION_REVISION =
        "idv.version.revision";

    /** Revision version xml attribute */
    static final String PROP_JYTHON_VERSION = "idv.jython.version";

    /** Holds the initial properties */
    private Properties idvProperties;

    /** The properties for converting old names to new names. Read from old.properties */
    private static Properties oldToNewNames;


    /** The code version of the app from  VERSION file */
    private String version;

    /** When the build ran */
    private String buildDate;

    /** The major version */
    private String versionMajor;

    /** The minor version */
    private String versionMinor;

    /** The version  revision */
    private String versionRevision;

    /** The version description */
    private String versionAbout;

    /** The Jython version */
    private String jythonVersion;

    /** flag for running isl */
    private boolean runningIsl = false;


    /**
     * The persistent store. Used to write out preferences,
     * manage the user's local .unidata/idv directory, etc.
     */
    private IdvObjectStore store;


    /** Do we show the DisplayControl-s in the DataTree-s. */
    protected boolean showControlsInTree = false;

    /** The name of the user */
    protected String userName;


    /** The default view size to be used for view managers */
    private Dimension viewSize;

    /** disable mixing **/
    public static final String PREF_SunAwtDisableMixing = "SunAwtDisableMixing";
    /**
     * Create this manager
     *
     * @param idv The IDV
     *
     */
    public StateManager(IntegratedDataViewer idv) {
        super(idv);
    }


    /** flag for synchronous loading */
    private boolean alwaysLoadBundlesSynchronously = false;

    /**
     * Set the flag to load bundles synchronously
     *
     * @param value true to load bundles synchronously
     */
    public void setAlwaysLoadBundlesSynchronously(boolean value) {
        alwaysLoadBundlesSynchronously = value;

    }

    /**
     * Get the flag for loading bundles synchronously
     *
     * @return true if synchronous
     */
    public boolean getShouldLoadBundlesSynchronously() {
        if (alwaysLoadBundlesSynchronously) {
            return true;
        }
        if ( !getIdv().getArgsManager().getIsOffScreen()
                && getIdv().getHaveInitialized()) {
            return false;
        }
        return true;
    }

    /**
     * This runs through the given list of property files,
     * reads in the properties in each and adds them into the
     * given props Properties object.
     *
     * @param props The Properties object that the properties get added into
     * @param propertyFiles List of property files.
     * @return True if at least one property file read was successful. False otherwise.
     */
    private boolean processPropertyList(Properties props,
                                        List propertyFiles) {
        boolean didone = false;
        for (int i = 0; i < propertyFiles.size(); i++) {
            try {
                Properties newProps = new Properties();

                newProps = Misc.readProperties((String) propertyFiles.get(i),
                        newProps, getIdvClass());
                //                System.err.println ("process: " +(String) propertyFiles.get(i));
                Hashtable processed = processPropertyTable(newProps);
                props.putAll(processed);
                didone = true;
            } catch (IllegalArgumentException iae) {
                //Ignore this
            }
        }
        return didone;
    }


    /**
     * Change property keys from application. to idv.
     *
     * @param newTable The table to change
     *
     * @return The converted table
     */
    protected Hashtable processPropertyTable(Hashtable newTable) {
        Hashtable processed = new Hashtable();
        for (Enumeration keys = newTable.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            Object value = newTable.get(key);
            String old   = key;
            key = StateManager.fixIds(key);
            if (key.startsWith("idv.gui")) {
                key = StringUtil.replace(key, "idv.gui", "idv.ui");
            }
            if (key.startsWith("View..")) {
                key = StringUtil.replace(key, "View..", "View.");
            }
            if (key.startsWith("View.Map.")) {
                key = StringUtil.replace(key, "View.Map.", "View.");
            }
            processed.put(key, value);
        }
        return processed;

    }

    /**
     * Change id  from application. to idv.
     *
     * @param id id to change
     *
     * @return changed id
     */
    public static String fixIds(String id) {
        if (id.startsWith("application.")) {
            id = "idv" + id.substring(11);
        }
        if (oldToNewNames != null) {
            String newId = (String) oldToNewNames.get(id);
            if (newId != null) {
                //                System.err.println ("Change:" + id + " to:" + newId);
                return newId;
            }
        }
        return id;
    }






    /**
     *  Initialize the list of property files and load them into the idvProperties.
     *  The algorithm is as follows:
     *  first process the property files that are hard coded from the system
     *  (e.g., /ucar/unidata/idv/resources/idv.propertes, vgee.properties, etc.),
     *  then process any command line argument property files (which are added into the
     *  propertyFiles list)
     *  <p>
     *  Now, we look for an "idv.properties" property (which is a semi-colon delimited list of property
     *  file paths) in the currently processed property files. If there is one then we create the list
     *  of property files, expand any %SITEPATH%, %USERPATH%, etc.,  macros and process the properties.
     *  <p>
     *  Note: Expanding the SITEPATH/USERPATH macros ends up creating the IdvObjectStore (to find
     *  the value of SITEPATH/USERPATH/etc.). The tricky thing is that the creation of the object store
     *  requires some things that we get from the properties (like the store name). These have to come from
     *  the original set of system property files.
     *
     */
    protected void loadProperties() {
        //Create this first because the next call ends up calling getProperty
        oldToNewNames =
            Misc.readProperties("/ucar/unidata/idv/resources/old.properties",
                                null, getIdvClass());

        idvProperties = new Properties();

        boolean didone = processPropertyList(idvProperties,
                                             getArgsManager().propertyFiles);
        if ( !didone) {
            throw new IllegalArgumentException(
                "Failed to load in any property files");
        }


        //Now check if there is an idv.properties file property.
        String props = (String) idvProperties.get("idv.properties");
        if (props != null) {
            //Expand out any macros (which ends up creating the IdvObjectStore).
            List l =
                getResourceManager().getResourcePaths(StringUtil.split(props,
                    ";"));
            processPropertyList(idvProperties, l);
        }


        //Now do any command line argument -Dname=value properties
        for (int i = 0; i < getArgsManager().argPropertyNames.size(); i++) {

            idvProperties.put(getArgsManager().argPropertyNames.get(i),
                              getArgsManager().argPropertyValues.get(i));
        }

        showControlsInTree = getProperty("idv.ui.showcontrolsintree", false);
        String topDir = getHelpRoot();
        Help.setTopDir(topDir);
        ucar.unidata.ui.symbol.StationModelCanvas.setHelpTopDir(topDir);
        ucar.unidata.ui.colortable.ColorTableEditor.setHelpTopDir(topDir);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getHelpRoot() {
        return getProperty("idv.help.topDir", DEFAULT_DOCPATH);
    }



    /**
     * Initialize the store, properties and {@link IdvResourceManager}
     *
     * @param interactiveMode Is the idv in interactive mode (the default)
     */
    protected void initState(boolean interactiveMode) {


        //Seems like we have to have this way up front here as something we do below
        //triggers the mac to ignore these settings

        //For now don't do this:
        //System.setProperty("apple.laf.useScreenMenuBar", "true");

        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
                           "IDV");
        // we'll have to figure out a better way to do this
        System.setProperty("sun.awt.disableMixing", "true");


        LogUtil.setTestMode(getArgsManager().noGui
                            || getArgsManager().getIsOffScreen());


        //Trace.msg("initState-1");

        //Load in the properties  to get some initial information (e.g., splash screen, store name, etc.)

        loadProperties();


        //Set the sitepath property if we were given it on the command line
        if (getArgsManager().sitePathFromArgs != null) {
            getStore().put(PREF_SITEPATH, getArgsManager().sitePathFromArgs);
        }

        //Now, initialize the store
        XmlResourceCollection preferencesRC =
            new XmlResourceCollection("idv.resource.preferences",
                                      "The user preferences");
        List prefPaths = getPreferencePaths();
        for (int i = 0; i < prefPaths.size(); i++) {
            String path = (String) prefPaths.get(i);
            path = getResourceManager().getResourcePath(path);
            if (path == null) {
                continue;
            }
            path = path.trim();
            if (path.length() == 0) {
                continue;
            }
            preferencesRC.addResource(path);
        }
        boolean didAnyPrefrences = getStore().init(preferencesRC) > 0;

        //Trace.msg("initState-2");

        //Set the sitepath property if we were given it on the command line
        if (getArgsManager().sitePathFromArgs != null) {
            getStore().put(PREF_SITEPATH, getArgsManager().sitePathFromArgs);
        }


        //Clear the macros so the next time we access the macros we will pick up
        //the sitepath from the newly initialized preference store
        getResourceManager().clearResourceMacros();


        //Load in the properties again to pick up the sitepath defined properties
        getStateManager().loadProperties();

        //Trace.msg("initState-3");

        //The rbi files define where we find all of the different types of resources
        List propertyRbiFiles =
            StringUtil.split(getProperty(PROP_RESOURCEFILES, ""), ";", true,
                             true);
        propertyRbiFiles.addAll(0, getArgsManager().argRbiFiles);


        //If there is no trustStore property defined then always trust self-signed certificates
        if(System.getProperty("javax.net.ssl.trustStore")==null) {
            ucar.unidata.util.NaiveTrustProvider.setAlwaysTrust(true);
        } 

        //Have the resource manager load up the resources
        getResourceManager().init(propertyRbiFiles);
        //Trace.msg("initState-4");

        String ut = (String) getProperty(PREF_SunAwtDisableMixing);
        if(ut != null && ut.equalsIgnoreCase("no")) {
             System.setProperty("sun.awt.disableMixing", "false");
        }
        getStore().append(
            getResourceManager().getXmlResources(
                IdvResourceManager.RSC_PREFERENCES), true);

        ucar.unidata.util.Msg.init(
            getResourceManager().getResources(
                IdvResourceManager.RSC_MESSAGES));



        // set the look and feel
        getIdvUIManager().loadLookAndFeel();

        // clear out the initial version strings because they only have the
        // stuff from idv.properties
        versionAbout = null;
        version      = null;
        getIdvUIManager().initSplash();
        getIdvUIManager().splashMsg("Initializing Resources");
        try {
            if (interactiveMode && getIdvUIManager().isMac()) {
                new ucar.unidata.idv.mac.MacBridge(getIdv());
            }
        } catch (Throwable ignore) {
            LogUtil.consoleMessage("Failed to create MacBridge:" + ignore);
        }



        //Trace.msg("initState-5");
        //Now, hopefully the store has read in the sitepath.

        getColorTableManager().init(
            getResourceManager().getResources(
                IdvResourceManager.RSC_COLORTABLES));

        //Trace.msg("initState-6");
        getIdvProjectionManager().initProjections();
        //Trace.msg("initState-7");

        getStationModelManager()
            .init(getResourceManager()
                .getXmlResources(IdvResourceManager
                    .RSC_STATIONSYMBOLS), getResourceManager()
                        .getResources(IdvResourceManager.RSC_STATIONMODELS));

        //Trace.msg("initState-8");
        getIdv().getParamDefaultsEditor();
        //Trace.msg("initState-9");

        //For now don't use this:
        //DataCategory.init (getResourceManager().getXmlResources (IdvResourceManager.RSC_CATEGORIES));


        DataGroup.init(
            getResourceManager().getXmlResources(
                IdvResourceManager.RSC_PARAMGROUPS));

        //Trace.msg("initState-10");
        DataAlias.init(
            getResourceManager().getXmlResources(
                IdvResourceManager.RSC_ALIASES));

        //Trace.msg("initState-11");

        //        System.err.println ("clean up tmp dir");
        //Get rid of anything in the tmp dir from before
        //Make sure we do this before we initialize the data manager
        //so any tmp dirs that get created for data caching don't get clobbered
        getStore().cleanupTmpDirectory();


        getDataManager().initResources(getResourceManager());

        ControlDescriptor.load(
            getIdv(),
            getResourceManager().getXmlResources(
                IdvResourceManager.RSC_CONTROLS));


        //Fix the legacy controls to show preference


        Object oldControls = getStore().get(PROP_OLDCONTROLDESCRIPTORS);
        if (oldControls != null) {
            Hashtable oldControlMap = null;
            if (oldControls instanceof Hashtable) {
                oldControlMap = (Hashtable) oldControls;
            } else {
                List controls = StringUtil.split(oldControls.toString(), ",");
                oldControlMap = new Hashtable();
                for (int i = 0; i < controls.size(); i++) {
                    oldControlMap.put(controls.get(i), controls.get(i));
                }
            }

            Hashtable newControlMap = new Hashtable();
            List      cds           = getIdv().getControlDescriptors();
            for (int i = 0; i < cds.size(); i++) {
                ControlDescriptor cd = (ControlDescriptor) cds.get(i);
                if (oldControlMap.get(cd.getControlId()) != null) {
                    newControlMap.put(cd.getControlId(), new Boolean(true));
                } else {
                    newControlMap.put(cd.getControlId(), new Boolean(false));
                }
            }
            getStore().put(PROP_CONTROLDESCRIPTORS, newControlMap);
            getStore().remove(PROP_OLDCONTROLDESCRIPTORS);
        }



        //Trace.msg("initState-12");
        getJythonManager().initUserFormulas(getResourceManager());

        //Trace.msg("initState-13");

        getStore().saveIfNeeded();
        ucar.unidata.view.geoloc.NavigatedMapPanel.DEFAULT_MAPS =
            Misc.newList("/auxdata/maps/OUTLSUPW", "/auxdata/maps/OUTLSUPU");

        //Trace.msg("initState-14");
        getStore().setTmpDir(
            (String) getProperty(IdvObjectStore.PROP_TMPDIR));

        FileManager.setFileHidingEnabled(
             !new Boolean(
                 getPreferenceOrProperty(
                     PREF_SHOWHIDDENFILES, "false")).booleanValue());

    }



    /**
     * _more_
     */
    protected void applyPreferences() {
        FileManager.setFileHidingEnabled(
             !new Boolean(
                 getPreferenceOrProperty(
                     PREF_SHOWHIDDENFILES, "false")).booleanValue());
    }

    /**
     * Get the preference paths
     *
     * @return  the paths for preferences
     */
    protected List getPreferencePaths() {
        String preferencesPaths = getProperty(PROP_PREFERENCES,
                                      "%USERPATH%/main.xml");
        return StringUtil.split(preferencesPaths, ";");
    }


    /**
     * Apply macros
     *
     * @param s  string
     *
     * @return String with macros expanded
     */
    public String applyMacros(String s) {
        for (Enumeration keys =
                idvProperties.keys(); keys.hasMoreElements(); ) {
            String key   = StateManager.fixIds((String) keys.nextElement());
            String macro = "${" + key + "}";
            if (s.indexOf(macro) >= 0) {
                String value = idvProperties.get(key).toString();
                s = StringUtil.replace(s, macro, value);
            }
        }
        return s;
    }


    /**
     * Get the hashtable of properties
     *
     * @return  the properties Hashtable
     */
    public Hashtable getProperties() {
        return idvProperties;
    }


    /**
     * Get a property
     *
     * @param name name of the property
     *
     * @return  the property or null
     */
    public Object getProperty(String name) {
        Object value = null;
        if (GuiUtils.isMac()) {
            value = idvProperties.get("mac." + name);
        }
        if (value == null) {
            value = idvProperties.get(name);
        }
        if (value == null) {
            String fixedName = StateManager.fixIds(name);
            if ( !name.equals(fixedName)) {
                return idvProperties.get(fixedName);
            }
        }
        return value;
    }


    /**
     *  Utility method to retrieve a boolean property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name Property name
     * @param dflt The default value to return if name is not found
     * @return The property value converted into a boolean or dflt if not found
     */
    public boolean getProperty(String name, boolean dflt) {
        String v = (String) getProperty(name);
        if (v != null) {
            return new Boolean(v.trim()).booleanValue();
        }
        return dflt;
    }


    /**
     *  Utility method to retrieve an int property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name Property name
     * @param dflt The default value to return if name is not found
     * @return The property value converted into an int or dflt if not found
     */
    public int getProperty(String name, int dflt) {
        String v = (String) getProperty(name);
        if (v != null) {
            return new Integer(v.trim()).intValue();
        }
        return dflt;
    }

    /**
     *  Utility method to retrieve an int property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name Property name
     * @param dflt The default value to return if name is not found
     * @return The property value converted into an int or dflt if not found
     */
    public double getProperty(String name, double dflt) {
        String v = (String) getProperty(name);
        if (v != null) {
            return new Double(v.trim()).doubleValue();
        }
        return dflt;
    }


    /**
     *  Utility method to retrieve a String property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name Property name
     * @param dflt The default value to return if name is not found
     * @return The property value or dflt if not found
     */
    public String getProperty(String name, String dflt) {
        String v = (String) getProperty(name);
        if (v != null) {
            return v.trim();
        }
        return dflt;
    }



    /**
     *  Utility method to retrieve a String property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name Property name
     * @param dflt The default value to return if name is not found
     * @return The property value or dflt if not found
     */
    public Color getColorProperty(String name, Color dflt) {
        return GuiUtils.decodeColor((String) getProperty(name), dflt);
    }



    /**
     *  Sets the property name to the given value
     *
     * @param name Property name
     * @param value The value
     */
    public void putProperty(String name, String value) {
        idvProperties.put(name, value);
    }


    /**
     *  Converts the given value to a String and sets
     * the property name to the String value
     *
     * @param name Property name
     * @param value The value
     */
    public void putProperty(String name, boolean value) {
        idvProperties.put(name, "" + value);
    }




    /**
     *  Create the {@link IdvObjectStore}, if needed,  and return it.
     *
     * @return The object store
     */
    public IdvObjectStore getStore() {
        if (store == null) {
            store = doMakeObjectStore();
        }
        return store;
    }


    /**
     * Get the default base help url. This looks up the idv.help.defaulturl property.
     * If its not there then it returns  DEFAULT_DOCPATH
     *
     * @return The base help url
     */
    public String getDefaultHelpUrl() {
        return getProperty("idv.help.defaulturl", getHelpRoot());
    }



    /**
     * Get the default help target. This just returns an empty string but
     * could be overrode by a derived class.
     *
     * @return The default help target
     */
    public String getDefaultHelpTarget() {
        return "idv.index";
    }

    /**
     *  Returns whether we are currently unpersisting application state from xml.
     * @return Is the IDV currently loading an xml bundle
     */
    public boolean isLoadingXml() {
        return getProperty(PROP_LOADINGXML, false);
    }


    /**
     * Helper method to determine whether to show {@link DisplayControl}-s
     * in the data choosing guis.
     *
     * @return Should we show data choices in the gui
     */
    public boolean getShowControlsInTree() {
        return showControlsInTree;
    }

    /**
     * Return the user name
     *
     * @return The user name
     */
    public String getUserName() {
        if (userName == null) {
            userName = Misc.getSystemProperty("user.name", "");
        }
        return userName;
    }

    /**
     * Return the title property
     *
     * @return The title from the properties (&quot;idv.title&quot;)
     */
    public String getTitle() {
        return getProperty("idv.title", "Default title");
    }



    /**
     * Factory method to create the {@link IdvObjectStore}. This
     * will also call {@link #initObjectStore(IdvObjectStore)}
     *
     * @return A new object store.
     */
    protected IdvObjectStore doMakeObjectStore() {
        IdvObjectStore store = new IdvObjectStore(getIdv(),
                                   getStoreSystemName(), getStoreName(),
                                   getIdv().getEncoderForRead(),
                                   getArgsManager().defaultUserDirectory);
        initObjectStore(store);
        return store;
    }

    /**
     * Initialize the given object store. This mostly
     * initializes the user's local .unidata/idv directory
     * when it is first created.
     *
     * @param store The object store to initialize
     */
    protected void initObjectStore(IdvObjectStore store) {
        //Check if this is the first time the idv has run
        while ( !store.userDirectoryOk()) {
            LogUtil.userMessage(
                "<html>The IDV is unable to create or write to the local user's directory. <br>Please select a directory. </html>");
            File dir = FileManager.getDirectory(null,
                           "Please select a local IDV directory");
            if (dir != null) {
                store.setOverrideDirectory(dir);
            } else {
                System.exit(0);
            }
        }


        if (store.getMadeUserDirectory()) {
            initNewUserDirectory(store.getUserDirectory());
        }
        initUserDirectory(store.getUserDirectory());
    }




    /**
     * What is the store name. This is either the  value of the
     * idv.store.name property or the tail class name
     * of the IDV class (e.g., DefaultIdv).
     *
     * @return The name of the store. This ends up being the name of the
     * subdirectory of the ~/.unidata/idv base directory.
     */
    public String getStoreName() {
        String storeName = getProperty("idv.store.name", NULL_STRING);
        if ((storeName != null) && (storeName.trim().length() > 0)) {
            return storeName;
        }
        String className = getIdvClass().getName();
        int    idx       = className.lastIndexOf(".");
        if (idx >= 0) {
            className = className.substring(idx + 1);
        }
        return className;
    }

    /**
     * Get the name of the top level users directory.
     *  It is either the value of the
     * idv.store.system property or
     * &quot;unidata/idv&quot;
     *
     * @return The system name
     */
    public String getStoreSystemName() {
        return getProperty("idv.store.system", "unidata/idv");
    }



    /**
     *  This gets called when we first create the users local object store directory.
     *
     * @param dir The new directory
     */
    protected void initNewUserDirectory(File dir) {
        try {
            //Copy the README file over to the .unidata/idv dir
            String           readMeFile = IOUtil.joinDir(dir, "README");
            FileOutputStream fos        = new FileOutputStream(readMeFile);
            InputStream readmeInputStream =
                IOUtil.getInputStream(
                    "/ucar/unidata/idv/resources/README.store", getClass());
            IOUtil.writeTo(readmeInputStream, fos);
        } catch (Exception exc) {
            //noop
        }

    }


    /**
     *  This gets called when after we have created the IdvObjectStore object
     * For now this method just copies the default rbi file into the
     * directory. This makes it easier for a user to do further
     * rbi based configurations.
     *
     * @param dir The store directory
     */
    protected void initUserDirectory(File dir) {
        File rbiFile = new File(IOUtil.joinDir(dir, "idv.rbi"));
        if ( !rbiFile.exists()) {
            String defaultRbi =
                IOUtil.readContents(
                    "/ucar/unidata/idv/resources/usersExample.rbi",
                    (String) null);
            if (defaultRbi != null) {
                try {
                    IOUtil.writeFile(rbiFile, defaultRbi);
                } catch (Exception exc) {
                    logException("Writing default rbi", exc);
                }
            }
        }
    }




    /**
     *  Puts the given value in the object store and writes out the store.
     *
     * @param pref The name
     * @param value The value
     */
    public void writePreference(String pref, Object value) {
        getStore().put(pref, value);
        getStore().save();
    }

    /**
     *  Writes out the store.
     */
    public void writePreferences() {
        getStore().save();
    }





    /**
     *  Helper method that wraps getStore().put (pref value).
     * This does not write out the store.
     *
     * @param pref The name
     * @param value The value
     */
    public void putPreference(String pref, Object value) {
        getStore().put(pref, value);
    }





    /**
     *  Helper method that wraps getStore().get (pref)
     *
     * @param pref The name of the preference
     * @return The value of the preference
     */
    public Object getPreference(String pref) {
        return getStore().get(pref);
    }



    /**
     *  Helper method that wraps getStore().get (pref)
     *
     * @param pref The name of the preference
     * @param dflt The default value to use if pref is not found.
     * @return The value of the preference of the dflt if not found
     */
    public Object getPreference(String pref, Object dflt) {
        Object result = getPreference(pref);
        if (result == null) {
            return dflt;
        }
        return result;
    }



    /**
     * Find either the preference with the given name
     * or, if not found, return the property value of the given name
     *
     * @param pref The preference or property name
     * @return The value of either the preference or the property
     */
    public Object getPreferenceOrProperty(String pref) {
        Object o = getPreference(pref);
        if (o == null) {
            o = getProperty(pref, null);
        }
        return o;
    }



    /**
     * Find either the preference with the given name
     * or, if not found, return the property String value of the given name if found.
     * If not found return the dflt
     *
     * @param pref The preference or property name
     * @param dflt default
     * @return The value of either the preference or the property
     */

    public String getPreferenceOrProperty(String pref, String dflt) {
        Object o = getPreferenceOrProperty(pref);
        if (o != null) {
            return o.toString();
        }
        return dflt;
    }


    /**
     * Find either the preference with the given name
     * or, if not found, return the property String value of the given name if found.
     * If not found return the dflt
     *
     * @param pref The preference or property name
     * @param dflt default
     * @return The value of either the preference or the property
     */
    public double getPreferenceOrProperty(String pref, double dflt) {
        Object o = getPreferenceOrProperty(pref);
        if (o != null && !o.toString().isEmpty()) {
            return new Double(o.toString()).doubleValue();
        }
        return dflt;
    }


    /**
     * Find either the preference with the given name
     * or, if not found, return the property String value of the given name if found.
     * If not found return the dflt
     *
     * @param pref The preference or property name
     * @param dflt default
     * @return The value of either the preference or the property
     */
    public boolean getPreferenceOrProperty(String pref, boolean dflt) {
        Object o = getPreferenceOrProperty(pref);
        if (o != null && !o.toString().isEmpty()) {
            return new Boolean(o.toString()).booleanValue();
        }
        return dflt;
    }


    /**
     *  Lookup in the object store whether we should popup a DataSelector in a window
     *  on start up.
     *
     * @return Should we show the  data selector on start up
     */
    public boolean getShowDashboardOnStart() {
        return (getStore().get(PREF_SHOWDASHBOARD, true)
                && getProperty(PROP_SHOWDASHBOARD, true));
    }


    /**
     * Get the jython version
     *
     * @return The jython version
     */
    public String getJythonVersion() {
        getVersion();
        return jythonVersion;
    }


    /**
     * Get the major version
     *
     * @return The major version
     */
    public String getVersionMajor() {
        getVersion();
        return versionMajor;
    }

    /**
     * Get the minor version
     *
     * @return The minor version
     */
    public String getVersionMinor() {
        getVersion();
        return versionMinor;
    }





    /**
     * Get the version revision
     *
     * @return The  version revision
     */
    public String getVersionRevision() {
        getVersion();
        return versionRevision;
    }

    /**
     * Get the version about
     *
     * @return The  version about
     */
    public String getVersionAbout() {
        getVersion();
        return versionAbout;
    }


    /**
     *  Read in  and return the current version from the resources/VERSION file.
     *
     * @return The IDV version
     */
    public String getVersion() {
        if (version == null) {
            try {
                Properties props =
                    Misc.readProperties(getProperty(PROP_VERSIONFILE,
                        "/ucar/unidata/idv/resources/build.properties"), null, getClass());
                buildDate = Misc.getProperty(props, PROP_BUILD_DATE, "");
                versionMajor = Misc.getProperty(props, PROP_VERSION_MAJOR,
                        "no_major");
                versionMinor = Misc.getProperty(props, PROP_VERSION_MINOR,
                        "no_minor");
                versionRevision = Misc.getProperty(props,
                        PROP_VERSION_REVISION, "no_revision");
                jythonVersion = Misc.getProperty(props, PROP_JYTHON_VERSION,
                        "");
                versionAbout =
                    IOUtil.readContents(getProperty(PROP_ABOUTTEXT,
                        "/ucar/unidata/idv/resources/about.html"), "");
                version = versionMajor + "." + versionMinor + versionRevision;
                versionAbout = StringUtil.replace(versionAbout,
                        "%idv.version%", version);
                versionAbout = StringUtil.replace(versionAbout,
                        "%idv.build.date%", buildDate);
            } catch (Exception exc) {
                logException("Reading version file", exc);
                version         = "error";
                versionMajor    = "";
                versionMinor    = "";
                versionRevision = "";
                jythonVersion   = "";
                versionAbout    = "";
            }
        }
        return version;
    }





    /**
     * Get the build date
     *
     * @return build date
     */
    public String getBuildDate() {
        //Make sure we read it
        getVersion();
        return buildDate;
    }

    /**
     * Get major-minor as a number
     *
     * @return number version
     */
    public double getNumberVersion() {
        return new Double(getVersionMajor()).doubleValue()
               + new Double(getVersionMinor()).doubleValue() * 0.1;
    }

    /**
     * Set the ViewSize property.
     *
     * @param value The new value for ViewSize
     */
    public void setViewSize(Dimension value) {
        viewSize = value;
    }

    /**
     * Get the ViewSize property.
     *
     * @return The ViewSize
     */
    public Dimension getViewSize() {
        return viewSize;
    }




    /**
     *  Set the RunningIsl property.
     *
     *  @param value The new value for RunningIsl
     */
    public void setRunningIsl(boolean value) {
        this.runningIsl = value;
    }

    /**
     *  Get the RunningIsl property.
     *
     *  @return The RunningIsl
     */
    public boolean getRunningIsl() {
        return this.runningIsl;
    }



}
