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
import org.w3c.dom.Node;

import ucar.unidata.data.DataManager;

import ucar.unidata.idv.chooser.IdvChooserManager;

import ucar.unidata.idv.collab.CollabManager;

import ucar.unidata.idv.publish.PublishManager;

import ucar.unidata.idv.test.TestManager;

import ucar.unidata.idv.ui.AliasEditor;



import ucar.unidata.idv.ui.AutoDisplayEditor;

import ucar.unidata.idv.ui.IdvUIManager;
import ucar.unidata.idv.ui.ImageGenerator;

import ucar.unidata.idv.ui.ParamDefaultsEditor;
import ucar.unidata.idv.ui.ParamGroupsEditor;
import ucar.unidata.idv.ui.ResourceViewer;
import ucar.unidata.ui.colortable.ColorTableEditor;
import ucar.unidata.ui.colortable.ColorTableManager;


import ucar.unidata.ui.symbol.StationModelManager;

import ucar.unidata.util.Misc;
import ucar.unidata.util.WrapperException;

import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlPersistable;

import java.awt.Color;

import java.lang.reflect.Constructor;


import java.util.ArrayList;
import java.util.List;


/**
 * This is the base class for the IntegratedDataViewer.
 * It holds a set of managers (e.g., IdvUIManager, DataManager)
 * and has methods to create them and get them. These factory
 * could be overwritten by derived classes to create their
 * own kind of manager or editor or so some different
 * initialization.
 *
 * @author IDV development team
 * @version $Revision: 1.40 $Date: 2007/06/12 22:52:02 $
 */

public abstract class IdvBase implements IdvConstants, XmlPersistable {


    /** List of classes that are dynamically loaded. We check these when we are creating managers */
    private static List pluginClasses = new ArrayList();


    /** The idv. This is really a pointer to this */
    protected IntegratedDataViewer idv;


    /** Command line  arguments */
    protected String[] args;

    /** The resource manager */
    protected IdvResourceManager resourceManager;

    /** The plugin manager */
    protected PluginManager pluginManager;

    /** The  manager of ViewManagers */
    protected VMManager vmManager;

    /** Handles doing publishing of content */
    protected PublishManager publishManager;

    /** Handles bundle writing */
    protected IdvPersistenceManager persistenceManager;

    /** Handles the different display conventions */
    protected DisplayConventions displayConventions;

    /** The  command line argument manager */
    protected ArgsManager argsManager;

    /** The color table editor. */
    protected ColorTableEditor colorTableEditor;

    /** The color table manager. */
    protected ColorTableManager colorTableManager;

    /** The  station model manager */
    protected StationModelManager stationModelManager;

    /** The data alias editor */
    protected AliasEditor aliasEditor;

    /** _more_          */
    protected ResourceViewer resourceViewer;


    /** The porojection manager */
    protected IdvProjectionManager projectionManager;


    /** The data alias editor */
    protected AutoDisplayEditor autoDisplayEditor;

    /** The  jython manager */
    protected JythonManager jythonManager;

    /** The  parameter defaults editor */
    protected ParamDefaultsEditor paramDefaultsEditor;

    /** The  parameter defaults editor */
    protected ParamGroupsEditor paramGroupsEditor;

    /** The   chooser manager */
    protected IdvChooserManager chooserManager;

    /** The data manager */
    protected DataManager dataManager;


    /** Handles collaboration and the event capture */
    protected CollabManager collabManager;

    /** Handles the end user preferences */
    protected IdvPreferenceManager preferenceManager;

    /** Handles running tests, creating test archives, etc. */
    protected TestManager testManager;

    /** Handles creating user interfaces, etc. */
    protected IdvUIManager uiManager;

    /** Handles generating images, movies, etc. */
    protected ImageGenerator imageGenerator;

    /** Handles properties, etc. */
    protected StateManager stateManager;

    /** Handles reinstalling the IDV */
    protected InstallManager installManager;


    /**
     * Create the IdvBase
     *
     * @param args Command line arguments
     */
    public IdvBase(String[] args) {
        this.args = args;
    }


    /**
     * Set the reference to the idv. This is really a reference
     * to this IdvBase object, but the idv is used to pass
     * in to the different managers and editors.
     *
     * @param idv The IDV
     */

    protected void setIdv(IntegratedDataViewer idv) {
        this.idv = idv;
    }

    /**
     * Get the IDV
     *
     * @return The idv
     */
    public IntegratedDataViewer getIdv() {
        return idv;
    }

    /**
     * Add a class that was loaded in via the plugin mechanism.
     * We check these classes when we are creating a manager
     *
     * @param c The class
     */
    public static void addPluginClass(Class c) {
        pluginClasses.add(c);
    }


    /**
     * Utility to make a manager class. This first sees if there is
     * a sub-class of the given managerClass in th list of plugin classes
     * If there is it will use that class.
     * If args is null this method jsut does a newInstance
     * If args is non-null we'll look for the right constructor
     *
     * @param managerClass Class to instantiate
     * @param args Args to pass in. May be null.
     *
     * @return Instantiated object
     */
    protected Object makeManager(Class managerClass, Object[] args) {
        try {
            for (int i = 0; i < pluginClasses.size(); i++) {
                Class c = (Class) pluginClasses.get(i);
                if (managerClass.isAssignableFrom(c)) {
                    managerClass = c;
                    break;
                }
            }
            if (args == null) {
                return managerClass.newInstance();
            }
            Class[] types = new Class[args.length];
            for (int i = 0; i < types.length; i++) {
                types[i] = args[i].getClass();
            }
            Constructor ctor = Misc.findConstructor(managerClass, types);
            if (ctor == null) {
                throw new IllegalArgumentException(
                    "Could not find constructor:" + managerClass.getName());
            }
            return ctor.newInstance(args);
        } catch (Exception exc) {
            Throwable thr = ucar.unidata.util.LogUtil.getInnerException(exc);
            System.err.println("Error:" + thr);
            thr.printStackTrace();
            throw new WrapperException(thr);
        }
    }



    /**
     * Factory method to create the
     * {@link ucar.unidata.ui.symbol.StationModelManager}
     *
     * @return The  station model manager
     */
    protected StationModelManager doMakeStationModelManager() {
        return (StationModelManager) makeManager(StationModelManager.class,
                                                 new Object[]{getEncoder()});
    }


    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.ui.symbol.StationModelManager}
     *
     * @return The  station model manager
     */
    public StationModelManager getStationModelManager() {
        if (stationModelManager == null) {
            //The smm depends on the ctm
            getDisplayConventions();
            getColorTableManager();
            stationModelManager = doMakeStationModelManager();
        }
        return stationModelManager;
    }

    /**
     * Show the station model editor
     */
    public void showStationModelEditor() {
        getStationModelManager().show();
    }





    /**
     * Factory method to create the
     * {@link ArgsManager}
     *
     * @param args The command lint arguments
     * @return The  command line argument manager
     */
    protected ArgsManager doMakeArgsManager(String[] args) {
        return (ArgsManager) makeManager(ArgsManager.class,
                                         new Object[] { idv,
                args });
    }


    /**
     * Get the persistence manager
     *
     *
     * @return The persistence manager
     */
    public IdvPersistenceManager getPersistenceManager() {
        if (persistenceManager == null) {
            persistenceManager = doMakePersistenceManager();
        }
        return persistenceManager;
    }




    /**
     * Factory method to create the
     * {@link IdvPersistenceManager}
     *
     * @return The  IdvPersistenceManager
     */
    protected IdvPersistenceManager doMakePersistenceManager() {
        return (IdvPersistenceManager) makeManager(
            IdvPersistenceManager.class, new Object[] { idv });
    }



    /**
     * Create, if needed, and return the {@link ArgsManager}
     *
     * @return The   command line argument manager
     */
    public ArgsManager getArgsManager() {
        if (argsManager == null) {
            argsManager = doMakeArgsManager(args);
        }
        return argsManager;
    }


    /**
     * Factory method to create the
     * {@link ucar.unidata.data.DataManager}
     *
     * @return The  data manager
     */
    protected DataManager doMakeDataManager() {
        return (DataManager) makeManager(DataManager.class,
                                         new Object[] { idv });
    }



    /**
     * Create, if needed, and return  the
     * {@link ucar.unidata.data.DataManager}. This
     * manages the creation and manipulation of the
     * data sources.
     *
     * @return The  data manager
     */
    public DataManager getDataManager() {
        if (dataManager == null) {
            dataManager = doMakeDataManager();
        }
        return dataManager;
    }




    /**
     * Factory method to create the
     * {@link DisplayConventions}
     *
     * @return The  display conventions
     */
    protected DisplayConventions doMakeDisplayConventions() {
        return (DisplayConventions) makeManager(DisplayConventions.class,
                new Object[] { idv });
    }



    /**
     * Create, if needed, and return  the
     * {@link DisplayConventions}. This
     * manages the creation and manipulation of the
     * data sources.
     *
     * @return The  data manager
     */
    public DisplayConventions getDisplayConventions() {
        if (displayConventions == null) {
            displayConventions = doMakeDisplayConventions();
        }
        return displayConventions;
    }


    /**
     * Factory method to create the
     * {@link ucar.unidata.ui.colortable.ColorTableManager}
     *
     * @return The color table manager
     */
    protected ColorTableManager doMakeColorTableManager() {
        return (ColorTableManager) makeManager(ColorTableManager.class, null);

    }

    /**
     * Create, if needed, and return  the
     * {@link ucar.unidata.ui.colortable.ColorTableManager}
     *
     * @return The color table manager
     */
    public ColorTableManager getColorTableManager() {
        if (colorTableManager == null) {
            colorTableManager = doMakeColorTableManager();
            ColorTableManager.setManager(colorTableManager);
        }
        return colorTableManager;
    }





    /**
     * Factory method to create the
     * {@link TestManager}.
     *
     * @return The test manager
     */
    protected TestManager doMakeTestManager() {
        return (TestManager) makeManager(TestManager.class,
                                         new Object[] { idv });
    }


    /**
     * Create, if needed, and return the
     * {@link TestManager}.  This class runs the
     * idv in test mode and also creates the test archives.
     *
     * @return The test manager
     */
    public TestManager getTestManager() {
        if (testManager == null) {
            testManager = doMakeTestManager();
        }
        return testManager;
    }


    /**
     * Factory method to create the
     * {@link VMManager}. This manages the set of
     * {@link ViewManager}s. It really should be called
     * the ViewManagerManager
     *
     * @return The  ViewManager manager
     */
    protected VMManager doMakeVMManager() {
        return (VMManager) makeManager(VMManager.class, new Object[] { idv });
    }


    /**
     * Create, if needed, and return the
     * {@link VMManager}.
     *
     * @return The  ViewManager manager
     */
    public VMManager getVMManager() {
        if (vmManager == null) {
            vmManager = doMakeVMManager();
        }
        return vmManager;
    }


    /**
     * Factory method to create the
     * {@link ucar.unidata.idv.publish.PublishManager}
     * This manages publishing content to weblogs, etc.
     *
     * @return The Publish manager
     */
    protected PublishManager doMakePublishManager() {
        return (PublishManager) makeManager(PublishManager.class,
                                            new Object[] { idv });
    }


    /**
     * Create, if needed, and return
     * {@link ucar.unidata.idv.publish.PublishManager}
     *
     * @return The publish manager
     */
    public PublishManager getPublishManager() {
        if (publishManager == null) {
            publishManager = doMakePublishManager();
        }
        return publishManager;
    }



    /**
     * Factory method to create the
     * {@link IdvUIManager}
     *
     * @return The UI manager
     */
    protected IdvUIManager doMakeIdvUIManager() {
        return (IdvUIManager) makeManager(IdvUIManager.class,
                                          new Object[] { idv });
    }



    /**
     * Create, if needed, and return the
     * {@link IdvUIManager}
     *
     * @return The UI manager
     */
    public IdvUIManager getIdvUIManager() {
        if (uiManager == null) {
            uiManager = doMakeIdvUIManager();
        }
        return uiManager;
    }



    /**
     * Factory method to create the
     * {@link ImageGenerator}
     *
     * @return The image generator
     */
    protected ImageGenerator doMakeImageGenerator() {
        return (ImageGenerator) makeManager(ImageGenerator.class,
                                            new Object[] { idv });
    }


    /**
     * Create, if needed, and return the
     * {@link ImageGenerator}
     *
     * @return The ImageGenerator
     */
    public ImageGenerator getImageGenerator() {
        if (imageGenerator == null) {
            imageGenerator = doMakeImageGenerator();
        }
        return imageGenerator;
    }




    /**
     * Factory method to create the
     * {@link StateManager}.
     *
     * @return The state manager
     */
    protected StateManager doMakeStateManager() {
        return (StateManager) makeManager(StateManager.class,
                                          new Object[] { idv });
    }



    /**
     * Create, if needed, and return the
     * {@link StateManager}
     *
     * @return The state manager
     */
    public StateManager getStateManager() {
        if (stateManager == null) {
            stateManager = doMakeStateManager();
        }
        return stateManager;
    }





    /**
     * Factory method to create the
     * {@link InstallManager}.
     *
     * @return The install manager
     */
    protected InstallManager doMakeInstallManager() {
        return (InstallManager) makeManager(InstallManager.class,
                                          new Object[] { idv });
    }



    /**
     * Create, if needed, and return the
     * {@link InstallManager}
     *
     * @return The install manager
     */
    public InstallManager getInstallManager() {
        if (installManager == null) {
            installManager = doMakeInstallManager();
        }
        return installManager;
    }



    /**
     * Factory method to create the
     * {@link JythonManager}
     *
     * @return The  jython manager
     */
    protected JythonManager doMakeJythonManager() {
        return (JythonManager) makeManager(JythonManager.class,
                                           new Object[] { idv });
    }

    /**
     * Create, if needed, and return the
     * {@link JythonManager}
     *
     * @return The jython manager
     */
    public JythonManager getJythonManager() {
        if (jythonManager == null) {
            jythonManager = doMakeJythonManager();
        }
        return jythonManager;
    }



    /**
     * Factory method to create the
     * {@link ucar.unidata.idv.chooser.IdvChooserManager}
     *
     * @return The   Chooser manager
     */
    protected IdvChooserManager doMakeIdvChooserManager() {
        //We need to set this here because the init method
        //ends up triggering a call to getIdvChooserManager
        //If we don't set this here we infinite loop
        chooserManager =
            (IdvChooserManager) makeManager(IdvChooserManager.class,
                                            new Object[] { idv });
        chooserManager.init();
        return chooserManager;
    }


    /**
     *  Create, if needed,  and return the
     * {@link ucar.unidata.idv.chooser.IdvChooserManager}
     *
     * @return The Chooser manager
     */
    public IdvChooserManager getIdvChooserManager() {
        if (chooserManager == null) {
            chooserManager = doMakeIdvChooserManager();
        }
        return chooserManager;
    }


    /**
     *  Call show on the
     * {@link ucar.unidata.idv.chooser.IdvChooserManager}
     */
    public void showChooser() {
        getIdvChooserManager().show();
    }


    /**
     *  Call show on the
     * {@link ucar.unidata.idv.chooser.IdvChooserManager}
     */
    public void showChooserModal() {
        getIdvChooserManager().showModal();
    }



    /**
     * Factory method to create the
     * {@link ucar.unidata.idv.collab.CollabManager}
     *
     * @return The collaboration manager
     */
    protected CollabManager doMakeCollabManager() {
        return (CollabManager) makeManager(CollabManager.class,
                                           new Object[] { idv });
    }



    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.idv.collab.CollabManager}
     *
     * @return The collaboration manager
     */
    public CollabManager getCollabManager() {
        if (collabManager == null) {
            collabManager = doMakeCollabManager();
        }
        return collabManager;
    }

    /**
     * Do we have a non-null collab manager
     *
     * @return Have a collab  manager
     */
    public boolean haveCollabManager() {
        return collabManager != null;
    }



    /**
     * Factory  method to create the
     * {@link ucar.unidata.idv.ui.AliasEditor}
     *
     * @return The alias editor
     */
    protected AliasEditor doMakeAliasEditor() {
        return (AliasEditor) makeManager(AliasEditor.class,
                                         new Object[] { idv });
    }


    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.idv.ui.AliasEditor}
     *
     * @return The alias editor
     */
    public AliasEditor getAliasEditor() {
        if (aliasEditor == null) {
            aliasEditor = doMakeAliasEditor();
        }
        return aliasEditor;
    }


    /**
     *  Show the alias editor
     */
    public void showAliasEditor() {
        getAliasEditor().show();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected ResourceViewer doMakeResourceViewer() {
        return (ResourceViewer) makeManager(ResourceViewer.class,
                                            new Object[] { idv });
    }


    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.idv.ui.AliasEditor}
     *
     * @return The alias editor
     */
    public ResourceViewer getResourceViewer() {
        if (resourceViewer == null) {
            resourceViewer = doMakeResourceViewer();
        }
        return resourceViewer;
    }



    /**
     *  Show the alias editor
     */
    public void showResourceViewer() {
        getResourceViewer().show();
    }



    /**
     * Factory  method to create the
     * {@link ucar.unidata.view.geoloc.ProjectionManager}
     *
     * @return The alias editor
     */
    protected IdvProjectionManager doMakeIdvProjectionManager() {
        return (IdvProjectionManager) makeManager(IdvProjectionManager.class,
                new Object[] { idv });
    }


    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.view.geoloc.ProjectionManager}
     *
     * @return The projection manager
     */
    public IdvProjectionManager getIdvProjectionManager() {
        if (projectionManager == null) {
            projectionManager = doMakeIdvProjectionManager();
        }
        return projectionManager;
    }


    /**
     *  Show the alias editor
     */
    public void showIdvProjectionManager() {
        getIdvProjectionManager().show();
    }




    /**
     * Factory  method to create the
     * {@link ucar.unidata.idv.ui.AutoDisplayEditor}
     *
     * @return The auto display editor
     */
    protected AutoDisplayEditor doMakeAutoDisplayEditor() {
        return (AutoDisplayEditor) makeManager(AutoDisplayEditor.class,
                new Object[] { idv });
    }


    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.idv.ui.AutoDisplayEditor}
     *
     * @return The auto display editor
     */
    public AutoDisplayEditor getAutoDisplayEditor() {
        if (autoDisplayEditor == null) {
            autoDisplayEditor = doMakeAutoDisplayEditor();
        }
        return autoDisplayEditor;
    }



    /**
     *  Show the alias editor
     */
    public void showAutoDisplayEditor() {
        getAutoDisplayEditor().show();
    }


    /**
     * Factory method to create the
     * {@link ucar.unidata.idv.ui.ParamDefaultsEditor}
     *
     * @return The parameter defaults editor
     */
    protected ParamDefaultsEditor doMakeParamDefaultsEditor() {
        return (ParamDefaultsEditor) makeManager(ParamDefaultsEditor.class,
                new Object[] { idv });
    }



    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.idv.ui.ParamDefaultsEditor}
     *
     * @return The parameter defaults editor
     */
    public ParamDefaultsEditor getParamDefaultsEditor() {
        if (paramDefaultsEditor == null) {
            paramDefaultsEditor = doMakeParamDefaultsEditor();
        }
        return paramDefaultsEditor;
    }




    /**
     * Show the param defaults editor
     */
    public void showDefaultsEditor() {
        getParamDefaultsEditor().show();
    }



    /**
     * Factory method to create the
     * {@link ucar.unidata.idv.ui.ParamGroupsEditor}
     *
     * @return The parameter Groups editor
     */
    protected ParamGroupsEditor doMakeParamGroupsEditor() {
        return (ParamGroupsEditor) makeManager(ParamGroupsEditor.class,
                new Object[] { idv });
    }



    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.idv.ui.ParamGroupsEditor}
     *
     * @return The parameter Groups editor
     */
    public ParamGroupsEditor getParamGroupsEditor() {
        if (paramGroupsEditor == null) {
            paramGroupsEditor = doMakeParamGroupsEditor();
        }
        return paramGroupsEditor;
    }




    /**
     * Show the param defaults editor
     */
    public void showParamGroupsEditor() {
        getParamGroupsEditor().show();
    }





    /**
     * Factory method to create the
     * {@link ucar.unidata.ui.colortable.ColorTableEditor}
     *
     * @return The color table editor
     */
    protected ColorTableEditor doMakeColorTableEditor() {
        return (ColorTableEditor) makeManager(ColorTableEditor.class,
                new Object[] { getColorTableManager() });
    }



    /**
     * Create, if needed, and return the
     * {@link ucar.unidata.ui.colortable.ColorTableEditor}
     *
     * @return The color table editor
     */
    public ColorTableEditor getColorTableEditor() {
        if (colorTableEditor == null) {
            colorTableEditor = doMakeColorTableEditor();
        }
        return colorTableEditor;
    }


    /**
     * Show the color table editor
     */
    public void showColorTableEditor() {
        showColorTableEditor(null);
    }


    /**
     * Show the color table editor with the color table
     * with the given name (if non-null)
     *
     * @param colorTableName The name of the color table to show
     */
    public void showColorTableEditor(String colorTableName) {
        if (colorTableName != null) {
            getColorTableEditor().setColorTable(colorTableName);
        }
        getColorTableEditor().show();
    }


    /**
     * Factory method to create the
     * {@link  IdvPreferenceManager}
     *
     * @return The preference manager
     */
    protected IdvPreferenceManager doMakePreferenceManager() {
        return (IdvPreferenceManager) makeManager(IdvPreferenceManager.class,
                new Object[] { idv });
    }

    /**
     *  Create, if needed, and return the
     * {@link  IdvPreferenceManager}
     *
     * @return The preference manager
     */
    public IdvPreferenceManager getPreferenceManager() {
        if (preferenceManager == null) {
            preferenceManager = doMakePreferenceManager();
        }
        return preferenceManager;
    }



    /**
     *  Create (if null) and popup the user preference dialog window.
     */
    public void showPreferenceManager() {
        getPreferenceManager().show();
        getPreferenceManager().toFront();
    }



    /**
     * Factory method to create the
     * {@link IdvResourceManager}
     *
     * @return The resource manager
     */
    protected IdvResourceManager doMakeResourceManager() {
        return (IdvResourceManager) makeManager(IdvResourceManager.class,
                new Object[] { idv });
    }


    /**
     * Create, if needed, and return the
     * {@link IdvResourceManager}
     *
     * @return The resource manager
     */
    public IdvResourceManager getResourceManager() {
        if (resourceManager == null) {
            resourceManager = doMakeResourceManager();
        }
        return resourceManager;
    }



    /**
     * Factory method to create the
     * {@link PluginManager}
     *
     * @return The plugin manager
     */
    protected PluginManager doMakePluginManager() {
        return (PluginManager) makeManager(PluginManager.class,
                                           new Object[] { idv });
    }

    /**
     * Create, if needed, and return the
     * {@link IdvResourceManager}
     *
     * @return The resource manager
     */
    public PluginManager getPluginManager() {
        if (pluginManager == null) {
            pluginManager = doMakePluginManager();
        }
        return pluginManager;
    }



    /**
     *  This simply returns the call to getStore. We have this here
     *  because getStore returns an IdvObjectStore, not an XmlObjectStore
     *  which is called for  in the inteface.
     *
     * @return The {@link ucar.unidata.xml.XmlObjectStore}
     */
    public XmlObjectStore getObjectStore() {
        return getStateManager().getStore();
    }

    /**
     *  Create the IdvObjectStore (if null) and return it.
     *
     * @return The {@link ucar.unidata.xml.XmlObjectStore}
     */
    public IdvObjectStore getStore() {
        return getStateManager().getStore();
    }


    /**
     *  Utility method to retrieve a boolean property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name The name of the property
     * @param dflt The default value if the property is not found
     * @return The given property or the dflt value
     */
    public boolean getProperty(String name, boolean dflt) {
        return getStateManager().getProperty(name, dflt);
    }


    /**
     *  Utility method to retrieve an int property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name The name of the property
     * @param dflt The default value if the property is not found
     * @return The given property or the dflt value
     */
    public int getProperty(String name, int dflt) {
        return getStateManager().getProperty(name, dflt);
    }


    /**
     *  Utility method to retrieve an int property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name The name of the property
     * @param dflt The default value if the property is not found
     * @return The given property or the dflt value
     */

    public double getProperty(String name, double dflt) {
        return getStateManager().getProperty(name, dflt);
    }


    /**
     *  Utility method to retrieve a String property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name The name of the property
     * @param dflt The default value if the property is not found
     * @return The given property or the dflt value
     */
    public String getProperty(String name, String dflt) {
        return getStateManager().getProperty(name, dflt);
    }

    /**
     *  Utility method to retrieve a String property from the idv properties.
     *  If the property does not exists return the given default value.
     *
     * @param name The name of the property
     * @param dflt The default value if the property is not found
     * @return The given property or the dflt value
     */
    public Color getColorProperty(String name, Color dflt) {
        return getStateManager().getColorProperty(name, dflt);
    }



    /**
     *  Helper method that wraps getStore().get (pref)
     *
     * @param pref The name of the preference
     * @return The value of the preference, or null if not found
     */
    public Object getPreference(String pref) {
        return getStore().get(pref);
    }


    /**
     *  Helper method that wraps getStore().get (pref)
     *
     * @param pref The name of the preference
     * @param dflt The default value to return if the preference is not found
     * @return The preference value or the dflt
     */
    public Object getPreference(String pref, Object dflt) {
        Object result = getStore().get(pref);
        if (result == null) {
            return dflt;
        }
        return result;
    }



    /**
     * Implement the XmlPersistable createElement method,
     * just return null since we don't really want to be persisted.
     *
     * @param encoder The encoder doing the encoding
     * @return null, because we don't want this object to actually be encoded
     */

    public Element createElement(XmlEncoder encoder) {
        return null;
    }

    protected XmlEncoder getEncoder() {
        return new XmlEncoder();
    }

    /**
     *  Just needed so we can implement XmlPersistable
     *
     * @param encoder The encoder doing the encoding
     * @param element The xml element that defines the object
     * @return Was this intialization successful
     */
    public boolean initFromXml(XmlEncoder encoder, Element element) {
        return true;
    }

}
