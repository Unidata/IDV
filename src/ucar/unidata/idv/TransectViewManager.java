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

import ucar.unidata.collab.Sharable;
import ucar.unidata.data.gis.Transect;

import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.geoloc.Bearing;


import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.gis.maps.MapData;
import ucar.unidata.gis.maps.MapInfo;
import ucar.unidata.idv.control.TransectDrawingControl;

import ucar.unidata.idv.ui.*;
import ucar.unidata.ui.Command;


import ucar.unidata.ui.CommandManager;
import ucar.unidata.ui.XmlUi;


import ucar.unidata.util.BooleanProperty;
import ucar.unidata.util.FileManager;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.*;


import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.GeoUtils;
import ucar.visad.ProjectionCoordinateSystem;

import ucar.visad.Util;
import ucar.visad.display.*;





import ucar.visad.quantities.*;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;


import java.awt.*;
import java.awt.event.*;

import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;



import java.beans.PropertyChangeListener;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;




/**
 * A wrapper around a TransectDisplay display master.
 * Provides an interface for managing user interactions, gui creation, etc.
 *
 * @author IDV development team
 */

public class TransectViewManager extends NavigatedViewManager {



    /** Preference for  grid lines or 2d _ */
    public static final String PREF_GRIDLINES = "TransectView.Gridlines";

    /** max data distance */
    private Real maxDataDistance;


    /** The transect I'm on showing */
    private Transect transect;

    /** text field for properties */
    private JTextField maxDistanceFld;

    /** combo  box for distance unit properties */
    private JComboBox distanceUnitBox;

    /** KM unit */
    private static Unit KM = CommonUnits.KILOMETER;


    /**
     *  Default constructor
     */
    public TransectViewManager() {}


    /**
     * Construct a <code>TransectViewManager</code> from an IDV
     *
     * @param viewContext Really the IDV
     */
    public TransectViewManager(ViewContext viewContext) {
        super(viewContext);
    }


    /**
     * Construct a <code>TransectViewManager</code> with the specified params
     * @param viewContext   context in which this MVM exists
     * @param desc   <code>ViewDescriptor</code>
     * @param properties   semicolon separated list of properties (can be null)
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public TransectViewManager(ViewContext viewContext, ViewDescriptor desc,
                               String properties)
            throws VisADException, RemoteException {
        super(viewContext, desc, properties);
    }


    /**
     * Helper method
     *
     * @return The transect display_
     */
    public TransectDisplay getTransectDisplay() {
        return (TransectDisplay) getMaster();
    }


    /**
     * Factory method to create the display master
     *
     * @return The TransectDisplay
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected DisplayMaster doMakeDisplayMaster()
            throws VisADException, RemoteException {
        Dimension dimension = getIdv().getStateManager().getViewSize();
        if (dimension == null) {
            if (displayBounds != null) {
                dimension = new Dimension(displayBounds.width,
                                          displayBounds.height);
            }
        }
        if ((dimension == null) || (dimension.width == 0)
                || (dimension.height == 0)) {
            dimension = null;
        }

        TransectDisplay td = new TransectDisplay(getLine(getTransect(true)),
                                 getIdv().getArgsManager().getIsOffScreen(),
                                 dimension);
        td.setScalesVisible(getTransectLabelsVisible());
        Unit u = getIdv().getPreferenceManager().getDefaultDistanceUnit();
        if (u != null) {
            td.setHorizontalRangeUnit(u);
        }
        if (maxDataDistance == null) {
            try {
                if (u == null) {
                    u = KM;
                }
                maxDataDistance = new Real(Length.getRealType(),
                                           u.toThis(40000, KM), u);

            } catch (Exception exc) {
                logException("Making max distance unit", exc);
            }
        }
        td.setMaxDataDistance(maxDataDistance);

        setVerticalRangeUnitPreference(td);
        return td;
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
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public boolean addDisplayInfo(DisplayInfo displayInfo)
            throws RemoteException, VisADException {

        if (getIsDestroyed()) {
            return false;
        }
        return super.addDisplayInfo(displayInfo);
    }

    /**
     * Tell the displays my transect has changed
     */
    private void notfyDisplaysOfTransectChange() {
        List controls = getControls();
        for (int i = 0; i < controls.size(); i++) {
            DisplayControl control = (DisplayControl) controls.get(i);
            control.transectChanged();
        }
    }


    /**
     * Utility to create the gridded2dset from a transect
     *
     * @param transect The transect
     *
     * @return The set
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private Gridded2DSet getLine(Transect transect)
            throws VisADException, RemoteException {
        List      pointList = transect.getPoints();
        float[]   lats      = new float[pointList.size()];
        float[]   lons      = new float[pointList.size()];
        float[][] points    = new float[][] {
            lats, lons
        };
        for (int i = 0; i < pointList.size(); i++) {
            LatLonPointImpl llp = (LatLonPointImpl) pointList.get(i);
            lats[i] = (float) llp.getLatitude();
            lons[i] = (float) llp.getLongitude();
        }
        return new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple, points,
                                points[0].length, null, null, null, false);



    }


    /**
     * Initialize this object's state with the state from that.
     *
     * @param that The other obejct to get state from
     * @param ignoreWindow If true then don't set the window size and location
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void initWithInner(ViewManager that, boolean ignoreWindow)
            throws VisADException, RemoteException {
        if ( !(that instanceof TransectViewManager)) {
            return;
        }

        super.initWithInner(that, ignoreWindow);
        TransectViewManager tvm = (TransectViewManager) that;

        if ((this != that) && (tvm.transect != null)) {
            setTransect(tvm.transect);
        }
    }


    /**
     * Update the name jlabel. Override base class method to add the transect name
     */
    protected void updateNameLabel() {
        if (nameLabel == null) {
            return;
        }
        String name = ((getName() == null)
                       ? ""
                       : getName());
        if (transect != null) {
            if (name.trim().length() > 0) {
                name = name + ": ";
            }
            name = name + "" + transect.getName();
        }
        nameLabel.setText(name);
    }


    /**
     * The BooleanProperty identified byt he given id has changed.
     * Apply the change to the display.
     *
     * @param id Id of the changed BooleanProperty
     * @param value Its new value
     *
     * @throws Exception problem handeling the change
     */
    protected void handleBooleanPropertyChange(String id, boolean value)
            throws Exception {

        if (id.equals(PREF_GRIDLINES)) {
            if (hasDisplayMaster()) {
                getTransectDisplay().setGridLinesVisible(value);
            }
        } else if (id.equals(PREF_SHOWTRANSECTSCALES)) {
            if (hasDisplayMaster()) {
                getNavigatedDisplay().setScalesVisible(value);
            }
        } else {
            super.handleBooleanPropertyChange(id, value);
        }
    }

    /**
     * Apply properties specific to this ViewManager
     *
     * @return true if successfule
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        try {
            Real   oldMaxDataDistance = maxDataDistance;
            Unit   u                  = null;
            Object selected           = distanceUnitBox.getSelectedItem();
            String unitName           = TwoFacedObject.getIdString(selected);
            try {
                Unit newUnit = ucar.visad.Util.parseUnit(unitName);
                if ( !(selected instanceof TwoFacedObject)) {
                    selected = new TwoFacedObject(selected.toString(),
                            newUnit);
                }
                if ((newUnit == null) || !Unit.canConvert(newUnit, KM)) {
                    throw new Exception(
                        "Unit must be convertible with kilometers");
                }

                u = newUnit;
            } catch (Exception exc) {
                LogUtil.userMessage("Illegal unit :" + unitName + "\n" + exc);
            }
            maxDataDistance =
                new Real(Length.getRealType(),
                         Misc.parseNumber(maxDistanceFld.getText().trim()),
                         u);
            //ucar.visad.Util.toReal(maxDistanceFld.getText().trim());
            getTransectDisplay().setMaxDataDistance(maxDataDistance);
            if ( !Misc.equals(oldMaxDataDistance, maxDataDistance)) {
                showDisplayTransect();
            }
        } catch (Exception exc) {
            LogUtil.userErrorMessage("Bad value:"
                                     + maxDistanceFld.getText().trim());
            return false;
        }
        return true;

    }



    /**
     * Add a properties component
     *
     * @param tabbedPane  the tabbed pane
     */
    protected void addPropertiesComponents(JTabbedPane tabbedPane) {
        super.addPropertiesComponents(tabbedPane);
        Unit u = maxDataDistance.getUnit();
        maxDistanceFld =
            new JTextField(Misc.format(maxDataDistance.getValue()), 10);
        maxDistanceFld.setToolTipText(
            "Maximum distance shown. e.g.: value[unit]");
        distanceUnitBox = getDisplayConventions().makeUnitBox(u, null);
        tabbedPane.add(
            "Transect",
            GuiUtils.inset(
                GuiUtils.topLeft(
                    GuiUtils.doLayout(
                        new Component[] { GuiUtils.rLabel("Max distance: "),
                                          maxDistanceFld,
                                          GuiUtils.rLabel("Unit: "),
                                          distanceUnitBox }, 2,
                                              GuiUtils.WT_N,
                                                  GuiUtils.WT_N)), 5));

    }




    /**
     * Create and return the show menu.
     *
     * @return The Show menu
     */
    protected JMenu makeShowMenu() {
        JMenu showMenu = super.makeShowMenu();
        createCBMI(showMenu, PREF_ANIREADOUT);
        createCBMI(showMenu, PREF_SHOWTRANSECTSCALES);
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
        props.add(new BooleanProperty(PREF_SHOWTRANSECTSCALES,
                                      "Show Display Scales",
                                      "Show Display Scales", true));
        props.add(new BooleanProperty(PREF_GRIDLINES, "Show Grid Lines", "",
                                      true));
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
     * When we have rendered the first frame tell any TransectDrawingControls
     * to show the display transect
     */
    protected void doneFirstFrame() {
        super.doneFirstFrame();
        showDisplayTransect();
    }



    /**
     * We've panned or zoomed. Show the display transect
     */
    protected void matrixChanged() {
        super.matrixChanged();
        showDisplayTransect();
    }

    /**
     * Show the display transect
     */
    protected void verticalRangeChanged() {
        super.matrixChanged();
        showDisplayTransect();
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
            bp.setDefault(true);
        }
        bp = getBooleanProperty(PREF_WIREFRAME);
        if (bp != null) {
            bp.setDefault(false);
        }
    }


    /**
     * Initialize the view menu
     *
     * @param viewMenu The view menu
     */
    public void initializeViewMenu(JMenu viewMenu) {
        super.initializeViewMenu(viewMenu);
        viewMenu.add(makeColorMenu());
        viewMenu.add(makeSavedViewsMenu());
        getViewpointControl().makeVerticalScaleMenuItem(viewMenu);


        viewMenu.addSeparator();
        createCBMI(viewMenu, PREF_SHAREVIEWS);
        viewMenu.add(GuiUtils.makeMenuItem("Set Share Group", this,
                                           "showSharableDialog"));
        viewMenu.addSeparator();
        viewMenu.add(GuiUtils.makeMenuItem("Full Screen", this,
                                           "setFullScreen"));


        viewMenu.addSeparator();
        viewMenu.add(GuiUtils.makeMenuItem("Properties", this,
                                           "showPropertiesDialog"));
    }

    /**
     * Create and return the list of menus for the menu bar.
     * Just the map and view menu.
     *
     * @return List of menus.
     */
    public ArrayList doMakeMenuList() {
        ArrayList menus = super.doMakeMenuList();
        menus.add(makeViewMenu());
        menus.add(GuiUtils.makeDynamicMenu("Transects", this,
                                           "initializeTransectMenu"));
        return menus;
    }


    /**
     * Add items to trasnect menu
     *
     * @param transectMenu menu
     */
    public void initializeTransectMenu(JMenu transectMenu) {
        List transects = new ArrayList(getResourceManager().getTransects());
        transectMenu.removeAll();
        transectMenu.add(GuiUtils.makeMenuItem("Edit", this,
                "editTransects"));
        if ((transect != null) && !transects.contains(transect)) {
            transects.add(0, transect);
        }

        if (transects.size() > 0) {
            transectMenu.addSeparator();
        }

        List  vms      = getIdv().getVMManager().getViewManagers();
        JMenu viewMenu = null;
        int   cnt      = 0;
        for (int i = 0; i < vms.size(); i++) {
            if ( !(vms.get(i) instanceof MapViewManager)) {
                continue;
            }
            MapViewManager mvm = (MapViewManager) vms.get(i);
            if (viewMenu == null) {
                viewMenu = new JMenu("From Views");
                transectMenu.add(viewMenu);
            }
            cnt++;
            String name    = mvm.getName();
            JMenu  theMenu = new JMenu(((name == null)
                                        ? ("View " + cnt)
                                        : name));
            viewMenu.add(theMenu);
            NavigatedDisplay   nd = mvm.getMapDisplay();
            java.awt.Rectangle b  = nd.getScreenBounds();
            double[]           pt1, pt2;
            pt1 = nd.getSpatialCoordinatesFromScreen(0, b.height / 2);
            pt2 = nd.getSpatialCoordinatesFromScreen(b.width, b.height / 2);
            theMenu.add(GuiUtils.makeMenuItem("Horizontal", this,
                    "setTransect",
                    new Transect("", Util.toLLP(nd.getEarthLocation(pt1)),
                                 Util.toLLP(nd.getEarthLocation(pt2)))));
            pt1 = nd.getSpatialCoordinatesFromScreen(b.width / 2, 0);
            pt2 = nd.getSpatialCoordinatesFromScreen(b.width / 2, b.height);
            theMenu.add(GuiUtils.makeMenuItem("Vertical", this,
                    "setTransect",
                    new Transect("", Util.toLLP(nd.getEarthLocation(pt1)),
                                 Util.toLLP(nd.getEarthLocation(pt2)))));
        }


        for (int i = 0; i < transects.size(); i++) {
            Transect menuTransect = (Transect) transects.get(i);
            String   prefix       = "";
            if (Misc.equals(transect, menuTransect)) {
                prefix = "> ";
            }
            transectMenu.add(GuiUtils.makeMenuItem(prefix
                    + menuTransect.toString(), this, "setTransect",
                        menuTransect));
        }
    }



    /**
     * Tell any TransectDrawing Controls to show the display transect
     *
     */
    private void showDisplayTransect() {
        try {
            if (hasDisplayMaster()) {
                List controls = getVMManager().findTransectDrawingControls();
                for (int i = 0; i < controls.size(); i++) {
                    TransectDrawingControl tdc =
                        (TransectDrawingControl) controls.get(i);
                    tdc.setDisplayedTransect(this);
                }
            }
        } catch (Exception exc) {
            logException("Showing the display transect", exc);
        }
    }


    /**
     * Set last active
     *
     * @param b  true to show last active
     */
    public void setLastActive(boolean b) {
        super.setLastActive(b);
        if (b) {
            //            showDisplayTransect();
        }
    }




    /**
     * Create, if needed, and show a TransectDrawingControl
     */
    public void editTransects() {
        List controls = getVMManager().findTransectDrawingControls();
        if (controls.size() == 0) {
            getIdv().doMakeControl("transectdrawingcontrol");
            return;
        }
        for (int i = 0; i < controls.size(); i++) {
            TransectDrawingControl tdc =
                (TransectDrawingControl) controls.get(i);
            tdc.show();
            ViewManager vm = tdc.getDefaultViewManager();
            if (vm != null) {
                vm.showWindow();
            }
        }

    }


    /**
     * Set the Transect property.
     *
     * @param value The new value for Transect
     */
    public void setTransect(Transect value) {
        setTransect(value, false);
    }

    public void setTransect(Transect value, boolean force) {
        if (!force && Misc.equals(transect, value)) {
            return;
        }
        transect = value;
        updateNameLabel();
        if ( !hasDisplayMaster()) {
            return;
        }
        TransectDisplay transectDisplay = getTransectDisplay();
        try {
            boolean         wasRunning   = false;
            int             currentIndex = 0;
            AnimationWidget widget       = getAnimationWidget();
            Animation       animation    = getAnimation();
            if (animation != null) {
                wasRunning   = animation.isAnimating();
                currentIndex = animation.getCurrent();
            }

            transectDisplay.setTransect(getLine(transect));
            //For now explictly reset the projection control
            //in ViewManager because we just created a new one
            //and we need to re-add outselves as  a listener
            resetProjectionControl();
            showDisplayTransect();
            if ((widget != null) && wasRunning && !widget.isRunning()) {
                animation.setCurrent(currentIndex);
                widget.setRunning(true);
            }
        } catch (Exception exc) {
            logException("Setting transect", exc);
        }
        notfyDisplaysOfTransectChange();
    }

    /**
     * Get the Transect property.
     *
     * @return The Transect
     */
    public Transect getTransect() {
        return transect;
    }


    /**
     * Get the Transect that is implicitly defined by the x axis
     *
     * @return The Transect
     */
    public Transect getAxisTransect() {
        EarthLocation[] els     = getTransectDisplay().getScaleEndPoints();
        EarthLocation   leftEl  = els[0];
        EarthLocation   rightEl = els[1];

        return new Transect("Transect",
                            Misc
                            .newList(new LatLonPointImpl(leftEl
                                .getLatLonPoint().getLatitude()
                                .getValue(), leftEl.getLatLonPoint()
                                .getLongitude()
                                .getValue()), new LatLonPointImpl(rightEl
                                .getLatLonPoint().getLatitude()
                                .getValue(), rightEl.getLatLonPoint()
                                .getLongitude().getValue())));

    }


    /**
     * Return  the transect
     *
     * @param force If true then create one
     *
     * @return The transect
     */
    public Transect getTransect(boolean force) {
        if ((transect == null) && force) {
            List transects = getResourceManager().getTransects();
            if (transects.size() > 0) {
                transect = (Transect) transects.get(0);
            } else {
                if (transects.size() == 0) {
                    transect =
                        new Transect("Transect",
                                     Misc.newList(new LatLonPointImpl(40.0,
                                         -120), new LatLonPointImpl(40.0,
                                             -100)));
                }
            }
        }
        return transect;
    }


    /**
     * Get  the show cursor readout flag
     * @return The flag value
     */
    public boolean getShowGridLines() {
        return getBp(PREF_GRIDLINES);
    }


    /**
     * Process the key event
     *
     * @param keyEvent The key event
     */
    public void keyWasTyped(KeyEvent keyEvent) {
        char c    = keyEvent.getKeyChar();
        int  code = keyEvent.getKeyCode();

        if (keyEvent.getID() == KeyEvent.KEY_PRESSED) {
            if (GuiUtils.isControlKey(keyEvent)) {
                if (keyEvent.isShiftDown()) {
                    if (code == KeyEvent.VK_LEFT) {
                        transect.shiftPercent(0,-0.25,true,true);
                    } else if (code == KeyEvent.VK_RIGHT) {
                        transect.shiftPercent(0,0.25,true,true);
                    } else if (code == KeyEvent.VK_UP) {
                        transect.shiftPercent(0.25,0,true,true);
                    } else if (code == KeyEvent.VK_DOWN) {
                        transect.shiftPercent(-0.25,0,true,true);
                    } else {
                        return;
                    }
                    setTransect(transect, true);
                    return;
                } 

                if (code == KeyEvent.VK_LEFT) {
                    getTransectDisplay().extendTransect(1.25);
                } else if (code == KeyEvent.VK_RIGHT) {
                    getTransectDisplay().extendTransect(.75);
                } else if (code == KeyEvent.VK_UP) {
                    getTransectDisplay().extendVerticalRange(.75);
                } else if (code == KeyEvent.VK_DOWN) {
                    getTransectDisplay().extendVerticalRange(1.25);
                }
            } else {
                if (code == KeyEvent.VK_LEFT) {
                    getTransectDisplay().translate(-0.25,0);
                } else if (code == KeyEvent.VK_RIGHT) {
                    getTransectDisplay().translate(0.25,0);
                } else if (code == KeyEvent.VK_UP) {
                    getTransectDisplay().translate(0,0.25);
                } else if (code == KeyEvent.VK_DOWN) {
                    getTransectDisplay().translate(0,-0.25);
                }

            }
        }
        super.keyWasTyped(keyEvent);
    }



    /**
     * Set the MaxDataDistance property.
     *
     * @param value The new value for MaxDataDistance
     */
    public void setMaxDataDistance(Real value) {
        maxDataDistance = value;
    }

    /**
     * Get the MaxDataDistance property.
     *
     * @return The MaxDataDistance
     */
    public Real getMaxDataDistance() {
        return maxDataDistance;
    }

    /**
     * What type of view is this
     *
     * @return The type of view
     */
    public String getTypeName() {
        return "Transect";
    }



}
