/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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


import ucar.unidata.util.BooleanProperty;
import ucar.unidata.util.GuiUtils;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.TimeHeightDisplay;

import visad.VisADException;


import java.awt.Container;

import java.rmi.RemoteException;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenu;


/**
 * A wrapper around a TimeHeight display master.
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class TimeHeightViewManager extends ViewManager {



    /** Preference for  grid lines or 2d _ */
    public static final String PREF_GRIDLINES = "TimeHeightView.Gridlines";

    /** preference for clipping */
    private boolean clipOn = false;

    /**
     *  Default constructor
     */
    public TimeHeightViewManager() {}


    /**
     * Construct a <code>TimeHeightViewManager</code> from an IDV
     *
     * @param viewContext Really the IDV
     */
    public TimeHeightViewManager(ViewContext viewContext) {
        super(viewContext);
    }



    /**
     * Construct a <code>TimeHeightViewManager</code> with the specified params.
     *
     * @param viewContext   context in which this MVM exists
     * @param desc   <code>ViewDescriptor</code>
     * @param properties   semicolon separated list of properties (can be null)
     * @throws VisADException the VisAD exception
     * @throws RemoteException the remote exception
     */
    public TimeHeightViewManager(ViewContext viewContext,
                                 ViewDescriptor desc, String properties)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties);
    }


    /**
     * Helper method
     *
     * @return The time-height display_
     */
    public TimeHeightDisplay getTimeHeightDisplay() {
        return (TimeHeightDisplay) getMaster();
    }


    /**
     * Factory method to create the display master
     *
     * @return The TimeHeightDisplay
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected DisplayMaster doMakeDisplayMaster()
            throws VisADException, RemoteException {
        return new TimeHeightDisplay();
    }


    /**
     * This is called by display controls and allows us to force fast rendering
     *
     * @param b The displays fast rendering flag
     *
     * @return true
     */
    public boolean getUseFastRendering(boolean b) {
        return true;
    }


    /**
     * Override base class method to force setting fast rendering to true.
     *
     * @param displayInfo The display info to add.
     * @return Was the addition successful
     * @throws RemoteException the remote exception
     * @throws VisADException the VisAD exception
     */
    public boolean addDisplayInfo(DisplayInfo displayInfo)
            throws RemoteException, VisADException {

        if (getIsDestroyed()) {
            return false;
        }
        return super.addDisplayInfo(displayInfo);
    }

    /**
     * The BooleanProperty identified by the given id has changed.
     * Apply the change to the display.
     *
     * @param id Id of the changed BooleanProperty
     * @param value Its new value
     *
     * @throws Exception problem handling the change
     */
    protected void handleBooleanPropertyChange(String id, boolean value)
            throws Exception {

        if (id.equals(PREF_GRIDLINES)) {
            if (hasDisplayMaster()) {
                getTimeHeightDisplay().setGridLinesVisible(value);
            }
        } else {
            super.handleBooleanPropertyChange(id, value);
        }
    }


    /**
     * Create and return the show menu.
     *
     * @return The Show menu
     */
    protected JMenu makeShowMenu() {
        JMenu showMenu = super.makeShowMenu();
        createCBMI(showMenu, PREF_GRIDLINES);
        return showMenu;
    }




    /**
     * Add to the intial Boolean properties
     *
     * @param props  list to add to
     */
    protected void getInitialBooleanProperties(List props) {
        super.getInitialBooleanProperties(props);
        props.add(new BooleanProperty(PREF_GRIDLINES, "Show Grid Lines", "",
                                      false));
    }


    /**
     * Make the GUI contents.
     *
     * @return The GUI contents
     */
    protected Container doMakeContents() {
        JComponent navComponent = getComponent();
        navComponent.setPreferredSize(getMySize());
        return navComponent;
    }

    /**
     * Set some properties
     */
    protected void initBooleanProperties() {
        super.initBooleanProperties();
        BooleanProperty bp = getBooleanProperty(PREF_SHOWSCALES);
        if (bp != null) {
            bp.setDefault(true);
        }
        bp = getBooleanProperty(PREF_GRIDLINES);
        if (bp != null) {
            bp.setDefault(false);
        }
        bp = getBooleanProperty(PREF_WIREFRAME);
        if (bp != null) {
            bp.setDefault(true);
        }
    }

    /**
     * Initialize the view menu
     *
     * @param viewMenu The view menu
     */
    public void initializeViewMenu(JMenu viewMenu) {
        showControlMenu = false;
        super.initializeViewMenu(viewMenu);
        viewMenu.add(makeColorMenu());
        //        viewMenu.add(makeSavedViewsMenu());
        viewMenu.addSeparator();
        createCBMI(viewMenu, PREF_SHAREVIEWS);
        viewMenu.add(GuiUtils.makeMenuItem("Set Share Group", this,
                                           "showSharableDialog"));
        //        viewMenu.addSeparator();
        //        viewMenu.add(GuiUtils.makeMenuItem("Full Screen", this,
        //                                           "setFullScreen"));


        viewMenu.addSeparator();
        viewMenu.add(GuiUtils.makeMenuItem("Properties", this,
                                           "showPropertiesDialog"));

    }


    /**
     * Create and return the list of menus for the menu bar.
     * Just the map and view menu.
     *
     * @return List of menus.
     * public ArrayList doMakeMenuList() {
     *   ArrayList menus    = super.doMakeMenuList();
     *   JMenu     viewMenu = makeViewMenu();
     *   menus.add(viewMenu);
     *   return menus;
     * }
     */

    /**
     * Get  the show cursor readout flag
     * @return The flag value
     */
    public boolean getShowGridLines() {
        return getBp(PREF_GRIDLINES);
    }

    /**
     * Set the clipping  flag
     *
     * @param value The value
     */
    public void setClipping(boolean value) {
        clipOn = value;
        if (getTimeHeightDisplay() != null) {
            getTimeHeightDisplay().enableClipping(clipOn);
        }
    }

    /**
     * Get the clipping  flag
     * @return The flag value
     */
    public boolean getClipping() {
        return clipOn;
    }

    /**
     * Do we support animation?
     *
     * @return false
     */
    public boolean animationOk() {
        return false;
    }

    /**
     * Don't show the side legend
     *
     * @return false
     */
    public boolean getShowSideLegend() {
        return false;
    }

}
