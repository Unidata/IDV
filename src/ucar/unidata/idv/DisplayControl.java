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


import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.idv.control.ReadoutInfo;

import ucar.visad.display.DisplayableData;

import visad.Data;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.MapProjection;


import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;



/**
 * A class to support controling the aspects of a display.
 *
 * @author IDV Development Team
 */
public interface DisplayControl extends Sharable {

    /** For now this allows us to programmatically turn off the time driver functionality */
    //public static final boolean DOTIMEDRIVER = true;

    /**
     * Preference prefix for preferences of this class.
     */
    public static final String PREF_PREFIX = "DisplayControl";

    /**
     * The property name of the String path or url of the icon this
     * display control should show in its legend.
     */
    public static final String PROP_LEGENDICON = "Prop.LegendIcon";

    /**
     * Preference for sampling mode
     */
    public static final String PREF_SAMPLING_MODE =
        "DisplayControlImpl.SamplingMode";

    /**
     * Preference for probe format
     */
    public static final String PREF_PROBEFORMAT =
        "DisplayControlImpl.ProbeFormat";

    /**
     * Default format for probe format
     */
    public static final String DEFAULT_PROBEFORMAT =
        "<b>%value%</b> [%unit%] ";

    /**
     * Preference for whether the control should be removed when
     *   the window is closed
     */
    public static String PREF_REMOVEONWINDOWCLOSE = PREF_PREFIX
                                                    + ".RemoveOnWindowClose";


    /** Should we remove stand alone display controls when their window is closed */
    public static final String PREF_STANDALONE_REMOVEONCLOSE =
        "idv.displaycontrol.standalone.removeonclose";

    /** Should we ask the user if they want to remove stand alone display controls */
    public static final String PREF_STANDALONE_REMOVEONCLOSE_ASK =
        "idv.displaycontrol.standalone.removeonclose.ask";


    /** Bottom type legend */
    public static final int BOTTOM_LEGEND = 0;

    /** Side type legend */
    public static final int SIDE_LEGEND = 1;

    /** Show raster flag */
    public static final int RASTERMODE_SHOWRASTER = 0;

    /** Show no raster flag */
    public static final int RASTERMODE_SHOWNONRASTER = 1;

    /** Show all flag */
    public static final int RASTERMODE_SHOWALL = 2;


    /**
     * Initialize the DisplayControl.
     *
     * @param displayId         the display id
     * @param dataCategories    the DisplayControl's data categories
     * @param choices           a list of {@link ucar.unidata.data.DataChoice}s
     * @param viewer            the control context in which this is viewed
     * @param properties         properties
     * @param dataSelection     specific data selection properties
     *
     * @deprecated Use init that that takes a properties Hashtable
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void init(String displayId, List dataCategories, List choices,
                     ControlContext viewer, String properties,
                     DataSelection dataSelection)
     throws VisADException, RemoteException;


    /**
     * Initialize the DisplayControl.
     *
     * @param displayId         the display id
     * @param dataCategories    the DisplayControl's data categories
     * @param choices           a list of {@link ucar.unidata.data.DataChoice}s
     * @param viewer            the control context in which this is viewed
     * @param properties        extra properties
     * @param dataSelection     specific data selection properties
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void init(String displayId, List dataCategories, List choices,
                     ControlContext viewer, Hashtable properties,
                     DataSelection dataSelection)
     throws VisADException, RemoteException;


    /**
     * Are we fully initialized
     *
     * @return is init done
     */
    public boolean isInitDone();


    /**
     * Get the control window for this DisplayControl.
     * @return The window that this display control is shown in
     */
    public JFrame getWindow();

    /**
     * Get whether the visibility of this display control is locked
     * (could be on or off)
     *
     * @return   true if locked.
     */
    public boolean getLockVisibilityToggle();

    /**
     * Should this display be shown in the display list
     *
     * @return Show in the display list
     */
    public boolean getShowInDisplayList();

    /**
     * Get the displayable for the display list for the particular view
     *
     *
     * @param view  The view manager
     * @return Displayable to show
     */
    public DisplayableData getDisplayListDisplayable(ViewManager view);

    /**
     * Get the data for the display list
     *
     * @return  the data
     */
    public Data getDataForDisplayList();

    /**
     * Set whether the visibility of this display control is locked
     * (could be on or off)
     *
     * @param value  true to lock to current state
     */
    public void setLockVisibilityToggle(boolean value);

    /**
     * Get the legend component for the type of legend
     *
     * @param  legendType   type of legend (BOTTOM, SIDE)
     * @return  component for that legend
     */
    public JComponent getLegendComponent(int legendType);


    /**
     * Get the legend component for the type of legend
     *
     * @param  legendType   type of legend (BOTTOM, SIDE)
     * @return  component for that legend
     */
    public JComponent getLegendButtons(int legendType);


    /**
     * Set the collapsed state of the legend.
     * @param collapse  true to collapse the legend.
     */
    public void setCollapseLegend(boolean collapse);


    /**
     * Get the collapsed state of the legend.
     * @return true if legend collapsed
     */
    public boolean getCollapseLegend();

    /**
     * Get the legend lable for the type of legend
     *
     * @param  legendType   type of legend (BOTTOM, SIDE)
     *
     * @return the legend label
     */
    public JComponent getLegendLabel(int legendType);

    /**
     * Called when the DisplayControl is removed from the display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void doRemove() throws VisADException, RemoteException;


    /**
     * Called when a ViewManager which holds the display is destoryed
     *
     *
     * @param viewManager The view manager that has been destroyed
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void viewManagerDestroyed(ViewManager viewManager)
     throws VisADException, RemoteException;

    /**
     * See if this DisplayControl can remove all controls.
     * @return  true if it can remove all
     */
    public boolean getCanDoRemoveAll();

    /**
     * Is this control active or has it been removed
     *
     * @return is active
     */
    public boolean getActive();

    /**
     * Get the {@link DataChoice} associated with this control.
     *
     * @return  the DataChoice (can be null)
     */
    public DataChoice getDataChoice();

    /**
     * Get the list of {@link DataChoice}s associated with this control.
     *
     * @return  the list of DataChoice-s
     */
    public List getDataChoices();

    /**
     * Get the display id for this control.
     *
     * @return the display id
     */
    public String getDisplayId();

    /**
     * Get the text that should show up in a menu listing this control.
     * @return  text for menu
     */
    public String getMenuLabel();

    /**
     * Get the menus for this control.
     * @param comp  component for placement of window (can be null)
     * @return  list of menus
     */
    public List getControlMenus(JComponent comp);

    /**
     * Get a label describing control
     * @return  a descriptive label
     */
    public String getLabel();



    /**
     * Move this control to a different {@link ViewManager}.
     *
     * @param viewManager  ViewManager to move to.
     */
    public void moveTo(ViewManager viewManager);

    /**
     * Get the view manager for this control
     *
     * @return the ViewManager
     */
    public ViewManager getViewManager();

    /**
     * Get the default view manager for this control
     *
     * @return the default view manager for this control
     */
    public ViewManager getDefaultViewManager();


    /**
     * Save an image of this control to a file.
     *
     * @param filename   filename to save to
     */
    public void saveImage(String filename);

    /**
     * Bring this control's component to the front
     */
    public void toFront();

    /**
     * Make a label like the one in the legend
     *
     * @return label
     */
    public JLabel makeLegendLabel();

    /**
     * Show this control's component
     */
    public void show();

    /**
     * Hide or show the main window
     */
    public void toggleWindow();

    /**
     * Set the display's visibility.
     *
     * @param b   true to set it visible
     */
    public void setDisplayVisibility(boolean b);

    /**
     * Get the display's visibility.
     *
     * @return  true if visible
     */
    public boolean getDisplayVisibility();


    /**
     * Toggle  the visibility for vector graphics rendering based on mode
     *
     * @param rasterMode  mode to use
     *
     * @throws Exception  problem toggling
     */
    public void toggleVisibilityForVectorGraphicsRendering(int rasterMode)
     throws Exception;



    /**
     * Make a visibility control as a JCheckbox.
     *
     * @param label   label for the visibility checkbox
     * @return  checkbox for toggling visibility
     */
    public JCheckBox doMakeVisibilityControl(String label);

    /**
     * Set the foreground color on the legend component
     *
     * @param fg   foreground color
     */
    public void setLegendForeground(java.awt.Color fg);

    /**
     * Set the background color on the legend component
     *
     * @param bg   background color
     */
    public void setLegendBackground(java.awt.Color bg);

    /**
     * Method to call after unpersisting from XML.
     *
     * @param vc   control context for this DisplayControl.
     * @param properties A place to put properties
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties);

    /**
     * Initialize after unpersistance
     *
     * @param vc    the control context
     * @param properties  the properties
     * @param dataChoices the list of data choices
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties,
                                       List dataChoices);

    /**
     * Intialize the display control when it is created from a template
     */
    public void initAsTemplate();


    /**
     * Do basic initialization. This is for display controls that server
     * as prototype objects
     *
     * @param displayId id
     * @param categories display categories
     * @param properties properties
     */
    public void initBasic(String displayId, List categories,
                          Hashtable properties);

    /**
     * Method to call when the first frame has been rendered.
     */
    public void firstFrameDone();

    /**
     * This is called when we are generating a test archive of images from all components
     *
     * @param archivePath Where to write the images
     */
    public void writeTestArchive(String archivePath);

    /**
     * Get the {@link MapProjection} associated with the data in
     * this DisplayControl.
     *
     * @return  data projection
     */
    public MapProjection getDataProjection();

    /**
     * Method called when a map projection changes.
     */
    public void projectionChanged();

    /**
     * Method called when a transect  changes.
     */
    public void transectChanged();

    /**
     * Method called when a view manager  changes.
     * @param property  the property that changed
     */
    public void viewManagerChanged(String property);

    /**
     * Get the display category for this DisplayControl.
     *
     * @return  category for this control.
     */
    public String getDisplayCategory();


    /**
     * Set the display category
     *
     * @param category  the display category
     */
    public void setDisplayCategory(String category);



    /**
     * Get the list of {@link ucar.unidata.data.DataCategory}s
     *
     * @return List of data categories
     */
    public List getCategories();


    /**
     * Find a property on the display control. These properties are not saved.
     *
     * @param key The key
     *
     * @return The value
     */
    public Object getTransientProperty(Object key);

    /**
     * Put a property on the display control. These properties are not saved.
     *
     * @param key The key
     * @param value The value
     */
    public void putTransientProperty(Object key, Object value);

    /**
     * Apply preferences to this control.
     */
    public void applyPreferences();


    /**
     * Should the display control be shown in a legend
     *
     * @return Should this display control be shown in a legend
     */
    public boolean getShowInLegend();

    /**
     * Show help
     */
    public void showHelp();


    /**
     * Is this control expanded when it is shown in the main tabs
     *
     * @return expanded in tabs
     */
    public boolean getExpandedInTabs();

    /**
     * Set whether  this control expanded when it is shown in the main tabs
     *
     * @param value value
     */
    public void setExpandedInTabs(boolean value);


    /**
     * Should we show this control in the main tabs
     *
     * @return show in main tabs
     */
    public boolean getShowInTabs();

    /**
     * Should this be docked?
     *
     * @return  true if docked
     */
    public boolean shouldBeDocked();

    /**
     * Can this be docked
     *
     * @return true if can be docked
     */
    public boolean canBeDocked();

    /**
     * Set whether to show this control in the main tabs
     *
     * @param value value
     */
    public void setShowInTabs(boolean value);

    /**
     * get the time set of the data
     *
     * @return date times
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public visad.Set getTimeSet() throws RemoteException, VisADException;

    /**
     * Get the cursor readout
     *
     * @param el  position
     * @param animationValue animation value
     * @param animationStep  animation index
     * @param samples  the list of samples to add to
     *
     * @return List of values
     *
     * @throws Exception  problem getting the cursor readout
     */
    public List getCursorReadout(EarthLocation el, visad.Real animationValue,
                                 int animationStep, List<ReadoutInfo> samples)
     throws Exception;



    /**
     * Get the tmp property.
     *
     * @param key key
     *
     * @return property
     */
    public Object getTmpProperty(Object key);

    /**
     * put the tmp property. These are not persisted off
     *
     * @param key key
     * @param value value
     */
    public void putTmpProperty(Object key, Object value);

    /**
     * remove the tmp property
     *
     * @param key key
     *
     * @return the value or null if not found
     */
    public Object removeTmpProperty(Object key);


    /**
     * How long should this display be shown when in visibility animation mode
     * @return -1 if it is undefined. 0 if this one should not be used. else treat the value as seconds
     */
    public int getVisbilityAnimationPause();


    /**
     * is this display the one that drives time selection for other displays
     *
     * @return  true if this is the time driver
     */
    public boolean getIsTimeDriver();


}
