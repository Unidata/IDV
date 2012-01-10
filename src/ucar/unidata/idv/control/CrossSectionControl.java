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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.gis.Transect;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.CrossSectionViewManager;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.TransectViewManager;
import ucar.unidata.idv.VerticalXSDisplay;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;

import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.util.Coord;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;

import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;



import ucar.visad.display.AnimationInfo;
import ucar.visad.display.CrossSectionSelector;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.GridDisplayable;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.XSDisplay;

import ucar.visad.quantities.*;  // for AirPressure CoordinateSystem


import visad.*;

import visad.data.units.Parser;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;

import visad.util.DataUtility;


import java.awt.*;


import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;


import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

import javax.vecmath.Point3d;



/**
 * TODO: We need to be able to persist/unpersist the ViewManager
 * so its state is saved.
 *
 * Class to make one vertical cross section display
 * and its contents and controls.
 * Also makes a JFrame with buttons used for control of
 * the vertical cross section of one parameter in another display.
 * Also includes contents of a CrossSectionViewManager, a local small
 * vertical cross section display in its own window in this control frame.
 *
 * @author IDV development team
 * @version $Revision: 1.173 $
 */

public abstract class CrossSectionControl extends GridDisplayControl implements DisplayableData
    .DragAdapter {


    /**
     * Identifier for sharing cross-section position
     */
    public static final String SHARE_XSLINE = SHARE_TRANSECT;

    /** Displayable for depicting cross section in the control window */
    protected DisplayableData vcsDisplay;

    /** Displayable for depicting cross section in the main window */
    protected DisplayableData xsDisplay;

    /** the cross section selector */
    protected CrossSectionSelector csSelector;

    /** initial starting point */
    private RealTuple initStartPoint;

    /** initial ending point */
    private RealTuple initEndPoint;

    /** _more_ */
    private double initLat1 = Double.NaN;

    /** _more_ */
    private double initLon1 = Double.NaN;

    /** _more_ */
    private double initLat2 = Double.NaN;

    /** _more_ */
    private double initLon2 = Double.NaN;

    /** _more_ */
    private double initAlt = 16000;

    /** _more_ */
    private boolean lineVisible = true;

    /** the control window's view manager */
    protected CrossSectionViewManager crossSectionView;

    /** The cross section view gui */
    private Container viewContents;

    /** Should we automatically scale the axis to the line range */
    private boolean autoScaleYAxis = true;

    /** should we even allow autoscaling */
    private boolean allowAutoScale = true;

    /** foreground color */
    private Color foreground;

    /** background color */
    private Color background;

    /** Keep around to reset zoom/pan */
    private double[] displayMatrix;

    /** animation info for the crossSectionView */
    private AnimationInfo animationInfo = new AnimationInfo();

    /** transform to altitude */
    protected CoordinateSystem coordTrans;

    /** Do we updat the cross section when we are in a transectview and the transect changes */
    private boolean autoUpdate = true;

    /** X and Y size */
    protected int sizeX, sizeY;

    /** flag for 3D display */
    protected boolean displayIs3D = false;

    /** flag for 3D data */
    protected boolean dataIs3D = false;

    /** starting coordinate for the cross section selector */
    protected Coord startCoord;

    /** ending coordinate for the cross section selector */
    protected Coord endCoord;

    /** Keep around for the label macros */
    private String positionText;

    /** starting location in earth coordinates */
    protected EarthLocation startLocation;

    /** ending location in earth coordinates */
    protected EarthLocation endLocation;

    /** working lat/lon point for calculations */
    private LatLonPointImpl workLLP = new LatLonPointImpl();

    /** working lat/lon point for calculations */
    private LatLonPointImpl startLLP = new LatLonPointImpl();

    /** working bearing for calculations */
    private Bearing workBearing = new Bearing();

    /** label for showing cross section location */
    private JLabel locationLabel;

    /** animation mode */
    private String ANIMATE_TOP_BOTTOM = "Top to Bottom";

    /** animation mode */
    private String ANIMATE_BOTTOM_TOP = "Bottom to Top";

    /** animation mode */
    private String ANIMATE_LEFT_RIGHT = "Left to Right";

    /** animation mode */
    private String ANIMATE_RIGHT_LEFT = "Right to Left";

    /** Last trasnect we sampled on */
    private Transect lastTransect;

    /** range for Y axis */
    private Range verticalAxisRange = null;

    /** start lat/lon widget */
    LatLonWidget startLLW;

    /** end lat/lon widget */
    LatLonWidget endLLW;

    /** range label */
    private JLabel rangeLabel;

    /** auto scale checkbox */
    private JCheckBox autoscaleCbx;

    /** list of levels */
    private List levelsList;


    /**
     * Default constructor.  Sets the appropriate attribute flags.
     */
    public CrossSectionControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_DATACONTROL | FLAG_DISPLAYUNIT);
    }


    /**
     * Create the <code>DisplayableData</code> that will be used
     * to depict the data in the main display.
     * @return  depictor for data in main display
     * @throws VisADException  unable to create depictor
     * @throws RemoteException  unable to create depictor (shouldn't happen)
     */
    protected abstract DisplayableData createXSDisplay()
     throws VisADException, RemoteException;

    /**
     * Create the <code>DisplayableData</code> that will be used
     * to depict the data in the control's display.
     * @return  depictor for data in main display
     * @throws VisADException  unable to create depictor
     * @throws RemoteException  unable to create depictor (shouldn't happen)
     */
    protected abstract DisplayableData createVCSDisplay()
     throws VisADException, RemoteException;

    /**
     * Get the <code>GridDisplayable</code> used for setting the
     * data.
     * @return data's <code>GridDisplayable</code>
     */
    public GridDisplayable getGridDisplayable() {
        return (GridDisplayable) xsDisplay;
    }

    /**
     * Get the <code>DisplayableData</code> used for depicting
     * data in the control's display.
     * @return control's display depictor
     */
    public DisplayableData getVerticalCSDisplay() {
        return vcsDisplay;
    }

    /**
     * Get the <code>DisplayableData</code> used for depicting
     * data in the main display.
     * @return main display depictor
     */
    public DisplayableData getXSDisplay() {
        return xsDisplay;
    }

    /**
     * Get the selector used to position the cross section.
     * @return this controls selector
     */
    public CrossSectionSelector getCrossSectionSelector() {
        return csSelector;
    }

    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     * @param preSelectedDataChoices set of preselected data choices
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties,
                                       List preSelectedDataChoices) {

        //Before 2.2 we had the zPosition set to -1 but did not use it
        //Now we use it for the cross section selector position so need to fix it
        if (version < 2.2) {
            if (getZPosition() == -1) {
                try {
                    setZPosition(.95);
                } catch (Exception exc) {
                    logException("Setting z position", exc);
                }
            }
        }
        super.initAfterUnPersistence(vc, properties, preSelectedDataChoices);
    }



    /**
     * Get the view manager for the control window.
     * @return  control window's view manager
     */
    protected CrossSectionViewManager getCrossSectionViewManager() {
        return crossSectionView;
    }



    /**
     * Get the view manager for capture
     *
     * @param what  the name
     *
     * @return  the ViewManager to use
     *
     * @throws Exception  problem getting the view manager
     */
    public ViewManager getViewManagerForCapture(String what)
            throws Exception {
        //        if(Misc.equals(what,"crosssection")) {
        setMainPanelDimensions();
        if ( !getIdv().getArgsManager().getIsOffScreen()) {
            GuiUtils.showComponentInTabs(getMainPanel());
        }
        return getCrossSectionViewManager();
        //        return super.getViewManagerForCapture(what);
    }




    /**
     * Initialize the control using the data choice
     *
     * @param dataChoice   choice specifying the data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        //Are we in 3d?
        displayIs3D = isDisplay3D();
        levelsList  = dataChoice.getAllLevels(null);
        xsDisplay   = createXSDisplay();
        vcsDisplay  = createVCSDisplay();

        //Now set the data (which uses the displayables  above).
        if ( !setData(dataChoice)) {
            return false;
        }


        vcsDisplay.setVisible(true);
        if (crossSectionView != null) {
            //If the ViewManager is non-null it means we have been unpersisted.
            //If so, we initialie the VM with the IDV
            crossSectionView.initAfterUnPersistence(getIdv());
        } else {
            //We are new (or are unpersisted from an old bundle)
            //Create the new ViewManager
            crossSectionView = new CrossSectionViewManager(getViewContext(),
                    new ViewDescriptor("CrossSectionView"),
                    "showControlLegend=false;showScales=true", animationInfo);
            crossSectionView.setIsShared(false);
            crossSectionView.setAniReadout(false);
            //This will only be non-null if we have been unpersisted from an old
            //(prior to the persistence of the ViewManager) bundle
            if (displayMatrix != null) {
                XSDisplay csvxsDisplay = crossSectionView.getXSDisplay();
                csvxsDisplay.setProjectionMatrix(displayMatrix);
            }
        }
        XSDisplay csvxsDisplay = crossSectionView.getXSDisplay();


        addViewManager(crossSectionView);
        setYAxisRange(csvxsDisplay, verticalAxisRange);
        csvxsDisplay.setXDisplayUnit(getDefaultDistanceUnit());
        //crossSectionView.getMaster ().addDisplayable (vcsDisplay);
        if (haveMultipleFields()) {
            addDisplayable(vcsDisplay, crossSectionView,
                           FLAG_COLORTABLE | FLAG_COLORUNIT);
        } else {
            addDisplayable(vcsDisplay, crossSectionView);
        }


        if (displayIs3D) {
            if (haveMultipleFields()) {
                //If we have multiple fields then we want both the 
                //color unit and the display unit
                addDisplayable(xsDisplay, FLAG_COLORTABLE | FLAG_COLORUNIT);
            } else {
                addDisplayable(xsDisplay);
            }
        }

        ViewManager vm = getViewManager();
        createCrossSectionSelector();
        //Now create the selector (which needs the state from the setData call)
        if (vm instanceof MapViewManager) {
            if (csSelector != null) {
                csSelector.setPointSize(getDisplayScale());
                csSelector.setAutoSize(true);
                csSelector.setVisible(lineVisible);
                addDisplayable(csSelector, getSelectorAttributeFlags());
            } else {
                System.err.println("NO CS SELECTOR " + getClass().getName());
            }
        } else if (vm instanceof TransectViewManager) {
            xsDisplay.setAdjustFlow(false);
            setUseFastRendering(true);
        }
        loadDataFromLine();
        return true;
    }





    /**
     * Add display settings for cross section controls
     *
     * @param dsd  the dialog to add to
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        dsd.addPropertyValue(getVerticalAxisRange(), "verticalAxisRange",
                             "Vertical Scale", SETTINGS_GROUP_DISPLAY);
        if (getAllowAutoScale()) {
            dsd.addPropertyValue(new Boolean(getAutoScaleYAxis()),
                                 "autoScaleYAxis", "Auto-Scale",
                                 SETTINGS_GROUP_DISPLAY);
        }

    }


    /**
     * Get the cursor readout info
     *
     * @param el  earth location
     * @param animationValue  animation value
     * @param animationStep  animation step
     * @param samples  the list of samples
     *
     * @return the list of items
     *
     * @throws Exception    problem reading the data
     */
    public List getCursorReadoutInner(EarthLocation el, Real animationValue,
                                      int animationStep,
                                      List<ReadoutInfo> samples)
            throws Exception {
        if ( !isInTransectView()) {
            return null;
        }
        //TODO: Sample on slice
        return null;
    }



    /**
     * Return the attribute flags to apply to the cross section selector.
     * This allows derived classes to set their own, e.g., use z position.
     *
     * @return Flags to use
     */
    protected int getSelectorAttributeFlags() {
        return FLAG_COLOR | FLAG_ZPOSITION;
    }

    /**
     * Get the projection of the data.
     * @return data projection or null
     */
    public MapProjection getDataProjection() {

        MapProjection mp = null;
        if (getGridDataInstance() != null) {
            FieldImpl data = getGridDataInstance().getGrid(false);
            if (data != null) {
                try {
                    mp = GridUtil.getNavigation(
                        getGridDataInstance().getGrid(false));
                } catch (Exception e) {
                    mp = null;
                }
            }
        }
        return mp;
    }

    /**
     * Called after all initialization is finished. This sets the end points
     * of the csSelector to the correct position and adds this as a property
     * change listener to the csSelector.
     */
    public void initDone() {
        super.initDone();
        try {
            RealTuple start = initStartPoint;
            RealTuple end   = initEndPoint;

            if ((start == null) && (initLat1 == initLat1)
                    && (initLon1 == initLon1)) {
                start = new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                      new double[] { initLon1,
                        initLat1, initAlt });
            }

            if ((end == null) && (initLat2 == initLat2)
                    && (initLon2 == initLon2)) {
                end = new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                    new double[] { initLon2,
                        initLat2, initAlt });
            }

            if (csSelector != null) {
                if (inGlobeDisplay()) {
                    csSelector.setInterpolateLinePoints(true);
                }

                //Do we have old end points from bundles?
                if ((startCoord != null) && (endCoord != null)) {
                    EarthLocation startLoc = boxToEarth(new double[] {
                                                 startCoord.getX(),
                            startCoord.getY(), startCoord.getZ() });
                    EarthLocation endLoc = boxToEarth(new double[] {
                                               endCoord.getX(),
                            endCoord.getY(), endCoord.getZ() });
                    setPosition(startLoc, endLoc);
                } else if (start == null) {
                    MapProjection mp       = getDataProjection();
                    Rectangle2D   rect     = mp.getDefaultMapArea();
                    LatLonPoint   startLLP = mp.getLatLon(new double[][] {
                        { rect.getX() }, { rect.getCenterY() }
                    });
                    LatLonPoint   endLLP   = mp.getLatLon(new double[][] {
                        { rect.getX() + rect.getWidth() },
                        { rect.getCenterY() }
                    });

                    if (startLLP.getLatitude().isMissing()
                            || startLLP.getLongitude().isMissing()
                            || endLLP.getLatitude().isMissing()
                            || endLLP.getLongitude().isMissing()) {
                        //Tried to check here whether end   points of Transect are missing or not


                        //It is assumed that at least 10% of the projected area is not missing.
                        startLLP = mp.getLatLon(new double[][] {
                            //And reset the end points of the Data Transect.
                            { rect.getCenterX() - rect.getWidth() / 10 },
                            { rect.getCenterY() - rect.getHeight() / 10 }
                        });
                        endLLP = mp.getLatLon(new double[][] {
                            { rect.getCenterX() + rect.getWidth() / 10 },
                            { rect.getCenterY() + rect.getHeight() / 10 }
                        });
                    }




                    EarthLocation startLoc =
                        new EarthLocationTuple(
                            startLLP.getLatitude().getValue(),
                            startLLP.getLongitude().getValue(), 0);
                    EarthLocation endLoc =
                        new EarthLocationTuple(
                            endLLP.getLatitude().getValue(),
                            endLLP.getLongitude().getValue(), 0);

                    setPosition(startLoc, endLoc);
                } else {
                    csSelector.setPosition(start, end);
                }

                csSelector.getStartSelectorPoint().setDragAdapter(this);
                csSelector.getEndSelectorPoint().setDragAdapter(this);
            }


            //Now load the data
            reScale();
            loadDataFromLine();
            updateViewParameters();
        } catch (Exception e) {
            logException("Initializing the csSelector", e);
        }
        // when user moves position of the Selector line, call crossSectionChanged
        csSelector.addPropertyChangeListener(this);
        updatePositionWidget();
    }


    /**
     * Implementation of the DisplayableData.DragAdapter
     *
     * @param ray    the view ray
     * @param first  if this is the first time
     * @param mouseModifiers  the mouse modifiers
     *
     * @return true
     */
    public boolean handleDragDirect(VisADRay ray, boolean first,
                                    int mouseModifiers) {
        return true;
    }

    /**
     * Handle adding a point
     *
     * @param x  the coords
     *
     * @return  true
     */
    public boolean handleAddPoint(float[] x) {
        return true;
    }

    /**
     * Called by ISL.
     * Write out some data defined by the what parameter to the given file.
     *
     * @param what What is to be written out
     * @param filename To what file
     *
     * @throws Exception _more_
     */
    public void doExport(String what, String filename) throws Exception {
        //TODO: Implement this to write out the cross section data
        throw new IllegalArgumentException("doExport not implemented");
    }


    /**
     * Transform VisAD box coordinates to and EarthLocation
     *
     * @param tuple  the tuple of VisAD coordinates
     *
     * @return  the corresponding EarthLocation
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    public EarthLocation boxToEarth(RealTuple tuple)
            throws RemoteException, VisADException {
        return boxToEarth(((Real) tuple.getComponent(0)).getValue(),
                          ((Real) tuple.getComponent(1)).getValue(),
                          (tuple.getDimension() > 2)
                          ? ((Real) tuple.getComponent(2)).getValue()
                          : 0);
    }


    /**
     * Constrain the drag point
     *
     * @param position  the position
     *
     * @return true
     */
    public boolean constrainDragPoint(float[] position) {
        try {
            double        altitude = getSelectorAltitude();
            EarthLocation pt       = boxToEarth(position[0], position[1],
                                          position[2], false);
            double[] xyz = earthToBox(
                               new EarthLocationTuple(
                                   pt.getLatitude().getValue(),
                                   pt.getLongitude().getValue(), altitude));
            if (inGlobeDisplay()) {
                position[0] = (float) xyz[0];
                position[1] = (float) xyz[1];
                position[2] = (float) xyz[2];
            } else {
                position[2] = (float) xyz[2];
            }
            return true;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }



    /**
     * Add any macro name/label pairs
     *
     * @param names List of macro names
     * @param labels List of macro labels
     */
    protected void getMacroNames(List names, List labels) {
        super.getMacroNames(names, labels);
        names.addAll(Misc.newList(MACRO_POSITION));
        labels.addAll(Misc.newList("Cross Section Position"));
    }

    /**
     * Add any macro name/value pairs.
     *
     *
     * @param template The template to use
     * @param patterns The macro names
     * @param values The macro values
     */
    protected void addLabelMacros(String template, List patterns,
                                  List values) {
        super.addLabelMacros(template, patterns, values);
        patterns.add(MACRO_POSITION);
        values.add(positionText);
    }



    /**
     * Handle property change
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                SelectorDisplayable.PROPERTY_POSITION)) {
            crossSectionChanged();
        } else {
            super.propertyChange(evt);
        }
    }


    /**
     * Remove the cross section view component from the gui to fix the funny lock up problem on linux
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void doRemove() throws RemoteException, VisADException {
        if (viewContents != null) {
            Container parent = viewContents.getParent();
            if (parent != null) {
                parent.remove(viewContents);
            }
            viewContents = null;
        }
        super.doRemove();
    }


    /**
     * Add tabs to the properties dialog.
     *
     * @param jtp  the JTabbedPane to add to
     */
    public void addPropertiesComponents(JTabbedPane jtp) {
        super.addPropertiesComponents(jtp);

        if (crossSectionView != null) {
            jtp.add(getCrossSectionViewLabel(),
                    crossSectionView.getPropertiesComponent());
        }
    }

    /**
     * Apply the properties
     *
     * @return true if successful
     */
    public boolean doApplyProperties() {
        if ( !super.doApplyProperties()) {
            return false;
        }
        if (crossSectionView != null) {
            return crossSectionView.applyProperties();
        }
        return true;
    }


    /**
     * Format a lat/lon
     *
     * @param latlon  the lat/lon
     *
     * @return  the formatted string
     */
    private String fmt(double latlon) {
        return getDisplayConventions().formatLatLon(latlon);
    }

    /**
     *  update the position widget
     */
    private void updatePositionWidget() {
        try {
            if (startLLW == null) {
                return;
            }
            EarthLocation[] coords = getLineCoords();
            startLLW.setLatLon(fmt(coords[0].getLatitude().getValue()),
                               fmt(coords[0].getLongitude().getValue()));
            endLLW.setLatLon(fmt(coords[1].getLatitude().getValue()),
                             fmt(coords[1].getLongitude().getValue()));

        } catch (Exception exc) {
            logException("Error setting position ", exc);
        }
    }

    /**
     * Set the position from a widget
     */
    private void setPositionFromWidget() {
        try {
            setPosition(
                new EarthLocationTuple(
                    new Real(RealType.Latitude, startLLW.getLat()), new Real(
                        RealType.Longitude, startLLW.getLon()), new Real(
                        RealType.Altitude, getSelectorAltitude())), new EarthLocationTuple(
                            new Real(
                                RealType.Latitude, endLLW.getLat()), new Real(
                                RealType.Longitude, endLLW.getLon()), new Real(
                                RealType.Altitude, getSelectorAltitude())));

        } catch (Exception exc) {
            logException("Error setting position ", exc);
        }
    }

    /**
     * Called by doMakeWindow in DisplayControlImpl, which then calls its
     * doMakeMainButtonPanel(), which makes more buttons.
     *
     * @return container of contents
     */
    public Container doMakeContents() {
        try {
            JTabbedPane tab = new MyTabbedPane();
            tab.add("Display", GuiUtils.inset(getDisplayTabComponent(), 5));
            tab.add("Settings",
                    GuiUtils.inset(GuiUtils.top(doMakeWidgetComponent()), 5));
            //Set this here so we don't get odd crud on the screen
            //When the MyTabbedPane goes to paint itself the first time it
            //will set the tab back to 0
            tab.setSelectedIndex(1);
            GuiUtils.handleHeavyWeightComponentsInTabs(tab);
            return tab;
        } catch (Exception exc) {
            logException("doMakeContents", exc);
        }
        return null;
    }


    /**
     * Get edit menu item
     *
     * @param items list of items to add to
     * @param forMenuBar for the menu bar
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        if (isInTransectView()) {
            items.add(GuiUtils.makeCheckboxMenuItem("Auto-Update", this,
                    "autoUpdate", null));
        }


        super.getEditMenuItems(items, forMenuBar);
    }



    /**
     * Create the component that goes into the 'Display' tab
     *
     * @return Display tab component
     */
    protected JComponent getDisplayTabComponent() {
        ActionListener llListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setPositionFromWidget();
            }
        };
        startLLW = new LatLonWidget("Lat: ", "Lon: ", llListener);
        endLLW   = new LatLonWidget("Lat: ", "Lon: ", llListener);

        //        locationLabel = new JLabel(
        //            "From:                     To:                        ");
        //        JComponent locationComp = GuiUtils.label("Location: ", locationLabel);
        JComponent locationComp = GuiUtils.hbox(new Component[] {
                                      GuiUtils.rLabel("Location:"),
                                      GuiUtils.filler(5, 5), startLLW,
                                      GuiUtils.cLabel("  To  "),
                                      endLLW }, 3);


        viewContents = crossSectionView.getContents();
        //If foreground is not null  then this implies we have been unpersisted
        //We do this here because the CrossSectionViewManager sets the default black on white
        //colors in its init method which might nor be called until we ask for its contents
        if (foreground != null) {
            crossSectionView.setColors(foreground, background);
        }


        crossSectionView.setContentsBorder(null);
        return GuiUtils.centerBottom(viewContents,
                                     GuiUtils.left(locationComp));
    }


    /**
     * Get the control widgets specific to this control
     *
     * @param controlWidgets   list of widgets to add to.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        /*        locationLabel = new JLabel("From:            To:      ");
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Location:"),
                                             GuiUtils.left(locationLabel)));
        */
        super.getControlWidgets(controlWidgets);
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Vertical Scale:"),
                GuiUtils.left(doMakeVerticalRangeWidget())));
        /* TODO: make this work
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Animate Line"),
                                             getXSectAniControlWidget()));
                                             */
    }

    /**
     * Make the Vertical Range component
     *
     * @return  the component
     */
    private Component doMakeVerticalRangeWidget() {
        Range r = getVerticalAxisRange();
        if (r == null) {
            try {
                r = getRange();
            } catch (Exception e) {}
        }
        rangeLabel = new JLabel("  Range: " + ((r != null)
                ? r.toString()
                : "     "));
        JButton rdButton = new JButton("Change");
        rdButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                RangeDialog rd = new RangeDialog(CrossSectionControl.this,
                                     getVerticalAxisRange(),
                                     "Change Vertical Axis Range",
                                     "setVerticalAxisRange");
                rd.showDialog();
                rangeLabel.setText("  Range: "
                                   + getVerticalAxisRange().toString());
            }
        });
        Component c = GuiUtils.hbox(rdButton, rangeLabel);
        if (getAllowAutoScale()) {
            autoscaleCbx = GuiUtils.makeCheckbox("Auto-scale?", this,
                    "autoScaleYAxis");
            c = GuiUtils.leftRight(c, autoscaleCbx);
        }
        return c;

    }

    /*
     * TODO: make this work
     * Create a widget for animating the cross sections.
     * private Component getXSectAniControlWidget() {
     *   ArrayList list = new ArrayList(4);
     *   list.add(ANIMATE_TOP_BOTTOM);
     *   list.add(ANIMATE_BOTTOM_TOP);
     *   list.add(ANIMATE_LEFT_RIGHT);
     *   list.add(ANIMATE_RIGHT_LEFT);
     *   JRadioButton[] buttons = GuiUtils.makeRadioButtons(list,
     *       list.getIndex(getAnimationDirection()), this,
     *       setAnimationDirection);
     *
     * }
     */

    /**
     * Called when the user asked for a new kind of parameter to be displayed
     * in a pre-existing display of this class, with other kind of data
     * already displayed there.
     * Reset new parameter choice's data into the displayables.
     * Do over everything necessary to load in a new kind of data.
     *
     * @param dataChoice     specification of the data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !super.setData(dataChoice)) {
            return false;
        }
        dataIs3D = getGridDataInstance().is3D();

        if (dataIs3D) {
            sizeX      = getGridDataInstance().getSizeX();
            sizeY      = getGridDataInstance().getSizeY();
            coordTrans = getGridDataInstance().getThreeDCoordTrans();
        } else {
            GriddedSet domainSet =
                (GriddedSet) getGridDataInstance().getSpatialDomain();
            sizeX      = domainSet.getLengths()[0];
            sizeY      = domainSet.getLengths()[1];
            coordTrans = domainSet.getCoordinateSystem();
        }

        if (xsDisplay == null) {
            xsDisplay  = createXSDisplay();
            vcsDisplay = createVCSDisplay();
        }
        getGridDisplayable().setColoredByAnother(haveMultipleFields());
        if (getVerticalCSDisplay() instanceof GridDisplayable) {
            ((GridDisplayable) getVerticalCSDisplay()).setColoredByAnother(
                haveMultipleFields());
        }

        if (isTopography(getRawDataUnit()) && !dataIs3D) {
            addTopographyMap();
        }

        if (getHaveInitialized()) {
            loadDataFromLine();
        }

        // change the displayed units if different from actual
        Unit newUnit = getDisplayUnit();
        if ((newUnit != null) && !newUnit.equals(getRawDataUnit())
                && Unit.canConvert(newUnit, getRawDataUnit())) {
            xsDisplay.setDisplayUnit(newUnit);
            vcsDisplay.setDisplayUnit(newUnit);
        }

        if (getHaveInitialized()) {
            updateViewParameters();
        }
        setXAxisTitle();

        return true;
    }

    /**
     * This method is used to update anything that needs to be updated
     * in the CrossSectionViewManager.  Subclasses should override this
     * if they need to do anything special.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void updateViewParameters()
            throws VisADException, RemoteException {
        CrossSectionViewManager vm = getCrossSectionViewManager();
        if (vm != null) {
            vm.setDisplayTitle(
                "of " + getGridDataInstance().getDataChoice().toString());
        }
    }


    /**
     * Set the starting coordinate of the cross section selector.
     * Used by XML persistence.
     * @param c  starting coordinate
     */
    public void setStartCoord(Coord c) {
        startCoord = c;
    }


    /**
     * Set the ending coordinate of the cross section selector.
     * Used by XML persistence.
     * @param c  ending coordinate
     */
    public void setEndCoord(Coord c) {
        endCoord = c;
    }


    /**
     * Make a Selector line which shows and controls where cross section is
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void createCrossSectionSelector()
            throws VisADException, RemoteException {

        csSelector = new CrossSectionSelector(
            new RealTuple(RealTupleType.SpatialEarth3DTuple, new double[] { 0,
                0, 0 }), new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                       new double[] { 0,
                0, 0 }));
    }

    /**
     * Create the cross section selector
     *
     * @param loc1  the starting location
     * @param loc2  the ending location
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD Exception
     */
    protected void createCrossSectionSelector(EarthLocation loc1,
            EarthLocation loc2)
            throws VisADException, RemoteException {
        csSelector = new CrossSectionSelector(
            new RealTuple(
                RealTupleType.SpatialEarth3DTuple,
                new double[] { loc1.getLongitude().getValue(),
                               loc2.getLatitude().getValue(),
                               0.0 }), new RealTuple(
                                   RealTupleType.SpatialEarth3DTuple,
                                   new double[] {
                                       loc1.getLongitude().getValue(),
                                       loc2.getLatitude().getValue(), 0 }));

    }





    /**
     * Convert three ints of grid index values to VisAD RealTuple of
     * VisAD internal coordinates.  If convert is true then the x/y/z
     * needs to be converted to display coordinates.
     *
     * @param x     grid index x value.
     * @param y     grid index y value.
     * @param z     grid index z value.
     * @param convert  flag (true) to convert to display coords
     * @return the XY position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private RealTuple getXYPosition(double x, double y, double z,
                                    boolean convert)
            throws VisADException, RemoteException {
        Coord     to      = (convert)
                            ? convertToDisplay(new Coord(x, y, z))
                            : new Coord(x, y, z);


        RealTuple xyTuple =
            new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                          new double[] { to.getX(),
                                         to.getY() });
        return xyTuple;
    }


    /**
     * Convert a Coord (x, y, z) in grid index values
     * to a Coord in VisAD internal values; -1.0 to 1.0 in VisAD box.
     * Goes via intermediate lat,long,altitude position.
     *
     * @param from a Coord (x, y, z) in grid index values.
     * @return  converted coordinates
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public Coord convertToDisplay(Coord from)
            throws VisADException, RemoteException {

        SampledSet domain =
            GridUtil.getSpatialDomain(getGridDataInstance().getGrid());
        boolean   latfirst     = GridUtil.isLatLonOrder(domain);
        float[][] domainCoords = domain.getSamples(false);


        int       lonindex     = (latfirst)
                                 ? 1
                                 : 0;
        int       latindex     = 1 - lonindex;

        /*
        // set proper indices for lon and lat
        int latindex, lonindex;

        if (latfirst) {
            latindex = 0;
            lonindex = 1;
        } else {
            lonindex = 0;
            latindex = 1;
        }
        */

        int elem = from.getIntX()
                   + (from.getIntY() + from.getIntZ() * sizeY) * sizeX;

        // Convert grid position to reference
        double[][] llarr = (dataIs3D)
                           ? new double[][] {
            { domainCoords[0][elem] }, { domainCoords[1][elem] },
            { domainCoords[2][elem] }
        }
                           : new double[][] {
            { domainCoords[0][elem] }, { domainCoords[1][elem] }
        };


        if (coordTrans != null) {
            llarr = coordTrans.toReference(llarr, domain.getSetUnits());
        } else {
            Unit[] toUnits = (dataIs3D)
                             ? new Unit[] { CommonUnit.degree,
                                            CommonUnit.degree,
                                            CommonUnit.meter }
                             : new Unit[] { CommonUnit.degree,
                                            CommonUnit.degree };
            llarr = Unit.convertTuple(llarr, domain.getSetUnits(), toUnits,
                                      false);
        }

        double lat = llarr[latindex][0];
        double lon = llarr[lonindex][0];

        double alt = (dataIs3D)
                     ? llarr[2][0]
                     : 0;

        //check to make sure that longitude is normalized to the range of the data.

        float low = domain.getLow()[lonindex];
        float hi  = domain.getHi()[lonindex];

        //TODO: Is this right??
        Unit[] units = domain.getSetUnits();
        if (coordTrans == null) {
            try {
                low = (float) units[lonindex].toThat(low, CommonUnit.degree);
                hi  = (float) units[lonindex].toThat(hi, CommonUnit.degree);
            } catch (Exception exc) {
                System.err.println("Caught error:" + exc);
            }

            while ((float) lon < low && (float) lon < hi) {
                lon += 360;
            }


            while ((float) lon > hi && (float) lon > low) {
                lon -= 360;
            }
        }


        // Convert to VisAD internal positions; -1.0 to 1.0 in VisAD box
        RealTuple visadTup = earthToBoxTuple(new EarthLocationTuple(lat, lon,
                                 alt));
        return new Coord(((Real) visadTup.getComponent(0)).getValue(),
                         ((Real) visadTup.getComponent(1)).getValue(),
                         ((Real) visadTup.getComponent(2)).getValue());
    }


    /**
     * Called when shared data is received.
     *
     * @param from      object sharing data
     * @param dataId    type of data being shared
     * @param data      the sharable data
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        //        System.out.println(this + "got share data");
        if (dataId.equals(SHARE_XSLINE)) {
            if (csSelector == null) {
                return;
            }
            try {
                //We don't need to loadData here because changing the
                //cs selector will fire a property change event
                csSelector.setPosition((RealTuple) data[0],
                                       (RealTuple) data[1]);
            } catch (Exception e) {
                logException("Error in receiveShareData: " + dataId, e);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }

    /**
     * Apply the Z position.  If the selector is in lat/lon/alt space,
     * we need to transform from XYZ
     *
     * @throws RemoteException   Java RMI Exception
     * @throws VisADException    VisADException
     */
    protected void applyZPosition() throws VisADException, RemoteException {
        super.applyZPosition();
        if (csSelector != null) {
            RealTuple start = csSelector.getStartPoint();
            if (Util.isEarthCoordinates(start)) {
                EarthLocation[] startEnd = getLineCoords();
                setPosition(startEnd[0], startEnd[1]);
            }
        }
    }



    /**
     * Set the position of the selector
     *
     * @param startLoc    Start location
     * @param endLoc      End location
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void setPosition(EarthLocation startLoc, EarthLocation endLoc)
            throws VisADException, RemoteException {
        RealTuple start =
            new RealTuple(RealTupleType.SpatialEarth3DTuple,
                          new double[] { startLoc.getLongitude().getValue(),
                                         startLoc.getLatitude().getValue(),
                                         getSelectorAltitude() });
        RealTuple end = new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                      new double[] {
                                          endLoc.getLongitude().getValue(),
                                          endLoc.getLatitude().getValue(),
                                          getSelectorAltitude() });


        csSelector.setPosition(start, end);
    }


    /**
     * Get the selector altitude from the Z position
     *
     * @return  the altitude
     */
    public double getSelectorAltitude() {
        double           z          = getZPosition();
        double           altitude   = 16000.0;
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        if (navDisplay != null) {
            double[] range = navDisplay.getVerticalRange();
            if ( !((range[0] == 0) && (range[1] == 0))) {
                // Z ranges from -1 to 1 (map view) or ~0 to 2)
                double pcntOfZRange = Math.abs((z - -1.0) / 2.0);
                // find percentage along a -1 to 1 range
                altitude = range[0] + pcntOfZRange * (range[1] - range[0]);
            }
        }
        return altitude;
    }


    /**
     * Load or reload data for a cross section.
     */
    public void crossSectionChanged() {
        try {
            loadDataFromLine();
            updateLegendLabel();
            updatePositionWidget();
            CrossSectionSelector cs = getCrossSectionSelector();
            doShare(SHARE_XSLINE, new Object[] { cs.getStartPoint(),
                    cs.getEndPoint() });
        } catch (Exception exc) {
            logException("Error in crossSectionChanged ", exc);
        }
    }

    /**
     * Respond to a change in the display's projection.  In this case
     * we resample at the new location.
     */
    public void projectionChanged() {
        super.projectionChanged();
        //      System.err.println ("projection changed");
        try {
            loadDataFromLine();
        } catch (Exception exc) {
            logException("projectionChanged", exc);
        }
    }

    /**
     * Noop for the ControlListener interface
     */
    public void viewpointChanged() {
        super.viewpointChanged();
        //      System.err.println ("viewpoint changed");
        if (autoUpdate && isInTransectView()) {
            loadDataFromTransect();
        }
    }

    /**
     * Add the  relevant view menu items into the list
     *
     * @param menus List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     *                   for a popup menu in the legend
     */
    protected void getViewMenuItems(List menus, boolean forMenuBar) {
        super.getViewMenuItems(menus, forMenuBar);
        menus.add(GuiUtils.MENU_SEPARATOR);

        if (forMenuBar) {
            JMenu csvMenu = crossSectionView.makeViewMenu();
            csvMenu.setText(getCrossSectionViewLabel());
            menus.add(csvMenu);
        }
    }

    /**
     * Get the label for the CrossSectionView
     * @return  return the name of the cross section view
     */
    protected String getCrossSectionViewLabel() {
        return "Cross Section";
    }

    /**
     * Sample along the transect line from the TransectViewManager we are in
     */
    private void loadDataFromTransect() {
        try {
            ViewManager vm    = getViewManager();
            Transect transect = ((TransectViewManager) vm).getAxisTransect();
            if (Misc.equals(transect, lastTransect)) {
                return;
            } else {
                loadDataFromLine();
            }
        } catch (Exception exc) {
            logException("Loading data from transect", exc);
        }


    }



    /**
     * A hook to allow derived classes to tell us to add this
     * as a control listener
     *
     * @return Add as control listener
     */

    protected boolean shouldAddControlListener() {
        return true;
    }



    /**
     * Method called when a transect  changes.
     */
    public void transectChanged() {
        super.transectChanged();
        //      System.err.println ("transect changed");
        try {
            loadDataFromLine();
        } catch (Exception exc) {
            logException("projectionChanged", exc);
        }
    }



    /**
     * Get the line coordinates as an array of EarthLocations
     *
     * @return the  locations
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException Problem creating EarthLocations
     */
    protected EarthLocation[] getLineCoords()
            throws VisADException, RemoteException {
        if (isInTransectView()) {
            ViewManager vm    = getViewManager();
            Transect transect = ((TransectViewManager) vm).getAxisTransect();
            lastTransect = transect;
            List            points = transect.getPoints();
            LatLonPointImpl llp0   = (LatLonPointImpl) points.get(0);
            LatLonPointImpl llp1   = (LatLonPointImpl) points.get(1);
            EarthLocation   el0    = makeEarthLocation(llp0.getLatitude(),
                                    llp0.getLongitude(), 0.0);
            EarthLocation el1 = makeEarthLocation(llp1.getLatitude(),
                                    llp1.getLongitude(), 0.0);
            return new EarthLocation[] { el0, el1 };
        }

        if (csSelector != null) {
            RealTuple start = csSelector.getStartPoint();
            RealTuple end   = csSelector.getEndPoint();
            double    x1    = ((Real) start.getComponent(0)).getValue();
            double    y1    = ((Real) start.getComponent(1)).getValue();
            double    x2    = ((Real) end.getComponent(0)).getValue();
            double    y2    = ((Real) end.getComponent(1)).getValue();
            double    z1    = (start.getDimension() < 3)
                              ? 0
                              : ((Real) start.getComponent(2)).getValue();
            double    z2    = (end.getDimension() < 3)
                              ? 0
                              : ((Real) end.getComponent(2)).getValue();
            if (Util.isEarthCoordinates(start)) {
                return new EarthLocation[] {
                    new EarthLocationTuple(y1, x1, 0),
                    new EarthLocationTuple(y2, x2, 0) };
            } else {
                return new EarthLocation[] { boxToEarth(new double[] { x1, y1,
                        0.0 }), boxToEarth(new double[] { x2, y2, 0.0 }) };
            }
        }
        return null;
    }



    /**
     * Create and loads a 2D FieldImpl from the existing getGridDataInstance()
     * at the position indicated by the controlling Selector line end points;
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void loadDataFromLine() throws VisADException, RemoteException {
        if ( !getHaveInitialized()) {
            return;
        }

        EarthLocation[] elArray = getLineCoords();
        if (elArray == null) {
            System.err.println(getClass().getName());
        }
        startLocation = elArray[0];
        endLocation   = elArray[1];
        LatLonPoint      latLon1 = startLocation.getLatLonPoint();
        LatLonPoint      latLon2 = endLocation.getLatLonPoint();

        GridDataInstance gdi     = getGridDataInstance();
        FieldImpl        slice   = gdi.sliceAlongLatLonLine(
                              latLon1, latLon2,
                              getSamplingModeValue(
                                  getObjectStore().get(
                                      PREF_SAMPLING_MODE,
                                      DEFAULT_SAMPLING_MODE)));
        // apply smoothing
        if (checkFlag(FLAG_SMOOTHING)
                && !getSmoothingType().equals(LABEL_NONE)) {
            slice = GridUtil.smooth(slice, getSmoothingType(),
                                    getSmoothingFactor());
        }
        loadData(slice);
    }


    /**
     * Load the external display and the local display
     * with this data of a vertical cross section.
     *
     * @param fieldImpl   the data for the depiction
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void loadData(FieldImpl fieldImpl)
            throws VisADException, RemoteException {
        //if (GridUtil.isAllMissing(fieldImpl)) return;
        FieldImpl twoDData = make2DData(fieldImpl);
        if (twoDData == null) {
            return;
        }
        getGridDisplayable().loadData(fieldImpl);
        load2DData(twoDData);

        // rescale display so data fits inside the display
        reScale();
        updateLocationLabel();
    }

    /**
     * Load the 2D data into the appropriate display(s)
     * @param twoDData  cross section slice converted to 2D
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void load2DData(FieldImpl twoDData)
            throws VisADException, RemoteException {
        ((GridDisplayable) vcsDisplay).loadData(twoDData);
    }

    /**
     *  Use the value of the smoothing type and weight to subset the data.
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    protected void applySmoothing() throws VisADException, RemoteException {
        if (checkFlag(FLAG_SMOOTHING)
                && !getSmoothingType().equals(LABEL_NONE)) {
            loadDataFromLine();
        }
    }

    /**
     * Set the title on the XAxis.
     */
    private void setXAxisTitle() {
        if (crossSectionView != null) {
            ((VerticalXSDisplay) crossSectionView.getXSDisplay())
                .setXAxisTitle();
        }
    }

    /**
     * Call to reScale the display.  Does the right thing depending
     * on the value of autoScaleYAxis.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException VisAD error
     */
    protected void reScale() throws VisADException, RemoteException {
        if (getAutoScaleYAxis()) {
            crossSectionView.getXSDisplay().autoScaleYAxis();
        } else {
            setYAxisRange(crossSectionView.getXSDisplay(),
                          getVerticalAxisRange());
        }
        crossSectionView.getXSDisplay().reScale();
    }

    /**
     * Set the range on the Y Axis of the cross section
     *
     * @param range     Range of values in units of Y Axis.  May be null
     */
    public void setVerticalAxisRange(Range range) {
        verticalAxisRange = range;
        if (crossSectionView != null) {
            try {
                setYAxisRange(crossSectionView.getXSDisplay(), range);
            } catch (Exception exc) {
                logException("Setting Y Axis Range: ", exc);
            }
        }
        if (rangeLabel != null) {
            rangeLabel.setText("  Range: "
                               + getVerticalAxisRange().toString());
        }
    }

    /**
     * Get the range on the vertical Axis of the cross section
     *
     * @return range of values in units of Y Axis.  May be null
     */
    public Range getVerticalAxisRange() {
        return verticalAxisRange;
    }

    /**
     * Methods to do the things that need to be done when the data range
     * changes.
     *
     * @param display   the display to modify
     * @param range     Range of values in units of Y Axis.  May be null
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void setYAxisRange(XSDisplay display, Range range)
            throws VisADException, RemoteException {

        if (range == null) {
            NavigatedDisplay mapDisplay = getNavigatedDisplay();
            if (mapDisplay == null) {
                return;
            }
            double[] vals = mapDisplay.getVerticalRange();
            range = new Range(vals[0], vals[1]);
            display.setYDisplayUnit(mapDisplay.getVerticalRangeUnit());
            verticalAxisRange = range;
        }
        display.setYRange(range.getMin(), range.getMax());
    }

    /**
     * Make a FieldImpl suitable for the plain 2D vert cross section display;
     * of form (time -> ((x) -> parm));
     * new x axis positions are in distance along cross section from one end.
     * override from superclass since we are dealing only with 2D data.
     *
     * @param xsectSequence    the time sequence of cross section data
     * @return  xsectSequence transformed to 2D
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected FieldImpl make2DData(FieldImpl xsectSequence)
            throws VisADException, RemoteException {

        FieldImpl grid2D = null;
        // from the input Field of fome (time->((a,b,c) -> parm)
        // get the (a,b,c) part
        GriddedSet domainSet;
        if (GridUtil.isConstantSpatialDomain(xsectSequence)) {
            domainSet = (GriddedSet) GridUtil.getSpatialDomain(xsectSequence);
            GriddedSet newDomain = make2DDomainSet(domainSet);
            if (newDomain == null) {
                return null;
            }
            grid2D = GridUtil.setSpatialDomain(xsectSequence, newDomain);
        } else {
            Set          timeSet    = GridUtil.getTimeSet(xsectSequence);
            int          numTimes   = timeSet.getLength();
            FieldImpl[]  newSamples = new FieldImpl[numTimes];
            FunctionType newType    = null;
            for (int i = 0; i < numTimes; i++) {
                FieldImpl sample = (FieldImpl) xsectSequence.getSample(i);
                if (sample.isMissing()) {
                    continue;
                }
                domainSet = (GriddedSet) GridUtil.getSpatialDomain(sample);
                GriddedSet newDomain = make2DDomainSet(domainSet);
                newSamples[i] = GridUtil.setSpatialDomain(sample, newDomain);
                if (newType == null) {
                    newType =
                        new FunctionType(DataUtility.getDomainType(timeSet),
                                         newSamples[i].getType());
                }
            }
            if (newType != null) {
                grid2D = new FieldImpl(newType, timeSet);
                grid2D.setSamples(newSamples, false, false);
            }
        }
        return grid2D;
    }

    /**
     * Make the domain for the 2D grid
     *
     * @param domainSet   the domain to be 2D'ized
     *
     * @return  the 2D ized grid
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected GriddedSet make2DDomainSet(GriddedSet domainSet)
            throws VisADException, RemoteException {

        int[] lengths = domainSet.getLengths();

        //        System.err.println("length:" + lengths.length);
        if ((lengths.length == 0) || (dataIs3D && (lengths.length < 2))) {
            return null;
        }
        int sizeX = lengths[0];
        int sizeZ = dataIs3D
                    ? lengths[1]
                    : 1;

        // get its coordinate tranform
        CoordinateSystem transform = domainSet.getCoordinateSystem();
        int              lonIndex  = GridUtil.isLatLonOrder(domainSet)
                                     ? 1
                                     : 0;
        int              latIndex  = 1 - lonIndex;

        // get the array of the a,b,c values
        float[][] transformed = domainSet.getSamples(true);

        // make a new parallel array of positions in units of ;
        // this in used only to get the height positions in .

        // need to do this to make sure we get data in degrees
        transformed = dataIs3D
                      ? CoordinateSystem.transformCoordinates((lonIndex == 0)
                ? RealTupleType.SpatialEarth3DTuple
                : RealTupleType
                    .LatitudeLongitudeAltitude, (CoordinateSystem) null,
                        new Unit[] { CommonUnit.degree,
                                     CommonUnit.degree,
                                     CommonUnit
                                         .meter }, (ErrorEstimate[]) null,
                                             ((SetType) domainSet.getType())
                                                 .getDomain(), transform,
                                                     domainSet.getSetUnits(),
                                                         (ErrorEstimate[]) null,
                                                             transformed,
                                                                 false)
                      : CoordinateSystem.transformCoordinates((lonIndex == 0)
                ? RealTupleType.SpatialEarth2DTuple
                : RealTupleType
                    .LatitudeLongitudeTuple, (CoordinateSystem) null,
                                             new Unit[] { CommonUnit.degree,
                CommonUnit.degree }, (ErrorEstimate[]) null,
                                     ((SetType) domainSet.getType())
                                         .getDomain(), transform,
                                             domainSet.getSetUnits(),
                                             (ErrorEstimate[]) null,
                                             transformed, false);

        float[] xVals = createXFromLatLon(new float[][] {
            transformed[0], transformed[1]
        }, sizeX, lonIndex);

        // declare an array to hold the (distance,height) positions
        // of points in the 2D display
        float[][] plane = new float[2][domainSet.getLength()];

        int       index = 0;
        for (int i = 0; i < sizeZ; i++) {
            for (int j = 0; j < sizeX; j++) {
                plane[0][index] = xVals[j];
                plane[1][index] = dataIs3D
                                  ? transformed[2][index]
                                  : 0;
                index++;
            }
        }

        RealType xType = null;

        if (crossSectionView != null) {
            XSDisplay xs = crossSectionView.getXSDisplay();
            xType = xs.getXAxisType();
        } else {
            xType = Length.getRealType();
        }

        RealTupleType xzRTT   = new RealTupleType(xType, RealType.Altitude);

        Gridded2DSet  vcsG2DS = (dataIs3D)
                                ? new Gridded2DSet(xzRTT, plane, sizeX, sizeZ,
                                    (CoordinateSystem) null,
                                    new Unit[] { CommonUnits.KILOMETER,
                CommonUnit.meter }, (ErrorEstimate[]) null, false, false)
                                : new Gridded2DSet(xzRTT, plane, sizeX,
                                    (CoordinateSystem) null,
                                    new Unit[] { CommonUnits.KILOMETER,
                CommonUnit.meter }, (ErrorEstimate[]) null, false);
        return vcsG2DS;

        //return GridUtil.setSpatialDomain(xsectSequence, vcsG2DS);
    }

    /**
     * Get the label for the Z position slider.
     * @return  label
     */
    protected String getZPositionSliderLabel() {
        return "Selector Position:";
    }


    /**
     * Set the AnimationInfo property.
     *
     * @param value The new value for AnimationInfo
     */
    public void setAnimationInfo(AnimationInfo value) {
        animationInfo = value;
    }

    /**
     * Get the AnimationInfo property.
     *
     * @return The AnimationInfo
     */
    public AnimationInfo getAnimationInfo() {
        if (crossSectionView != null) {
            return crossSectionView.getAnimationInfo();
        }
        return animationInfo;
    }

    /**
     * From an array of latitudes and longitudes, calculate an
     * array of distance (in km) that corresponds to the distance
     * from the first point to the numNeeded point.  NB: In this implementation,
     * the distance from the origin is calculated as the sum of the
     * distances between each point in between.
     *
     * @param latlon   array of lat lon values in degrees (order doesn't matter)
     * @param numNeeded  number of distances to calculate
     * @param lonIndex   which of the indices in latlon is longitude
     *
     * @return array of distances each lat/lon point is from the
     *         origin.
     */
    protected float[] createXFromLatLon(float[][] latlon, int numNeeded,
                                        int lonIndex) {

        int   latIndex = 1 - lonIndex;
        float startLon = latlon[lonIndex][0];
        float startLat = latlon[latIndex][0];
        startLLP.set(startLat, startLon);

        // test to see if units are geographic degrees

        float   initXVal = 0.0f,
                bigDelta = 0.0f;
        boolean hitJump  = false;

        // declare an array to hold the (distance,height) positions
        // of points in the 2D display
        float[] xVals   = new float[numNeeded];

        float   prevLon = startLon;
        for (int i = 1; i < numNeeded; i++) {

            float lon = latlon[lonIndex][i];
            float lat = latlon[latIndex][i];

            // All the following up, to plane[0][i] = (float)...,
            // handles jumps in xval from one grid edge to another, a seam
            // in the global displays
            // look for jump across seam just occured
            // -- a sudden increase in xval
            // not really sure this works
            bigDelta = (lon - prevLon);
            if (bigDelta > 180.0) {
                hitJump = true;
                lon     = lon - 360;
            }
            workLLP.set(lat, lon);

            xVals[i] = xVals[i - 1]
                       + (float) Bearing.calculateBearing(startLLP, workLLP,
                           workBearing).getDistance();

            //System.out.println("                        xVal "+xVals[i]);

            startLLP.set(workLLP);

            prevLon = lon;
        }
        return xVals;
    }

    /**
     * Apply preferences to this control.  Subclasses should override
     * if needed.  This is a noop in this class.
     */
    public void applyPreferences() {
        super.applyPreferences();
        if (crossSectionView != null) {
            ((VerticalXSDisplay) crossSectionView.getXSDisplay())
                .setXDisplayUnit(getIdv().getPreferenceManager()
                    .getDefaultDistanceUnit());
        }
    }

    /**
     * Wrapper around {@link #addTopographyMap(int)} to allow subclasses
     * to set their own index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void addTopographyMap() throws VisADException, RemoteException {
        addTopographyMap(0);
    }

    /**
     * Called when a change in position occurs
     */
    protected void updateLocationLabel() {
        StringBuffer buf = new StringBuffer();
        buf.append(getDisplayConventions().formatEarthLocation(startLocation,
                false, false));
        buf.append("  to  ");
        buf.append(getDisplayConventions().formatEarthLocation(endLocation,
                false, false));

        positionText = buf.toString();
        if (locationLabel == null) {
            return;
        }

        locationLabel.setText(positionText);
    }

    /**
     * Set the AutoScale property.
     *
     * @param value The new value for AutoScale
     */
    public void setAllowAutoScale(boolean value) {
        allowAutoScale = value;
        if ( !allowAutoScale) {
            autoScaleYAxis = false;
        }
    }

    /**
     * Get the AutoScale property.
     *
     * @return The AutoScale
     */
    public boolean getAllowAutoScale() {
        return allowAutoScale;
    }


    /**
     * Set the AutoScale property.
     *
     * @param value The new value for AutoScale
     */
    public void setAutoScaleYAxis(boolean value) {
        autoScaleYAxis = value;
        try {
            loadDataFromLine();
        } catch (Exception exc) {
            logException("Loading data from line", exc);
        }
        if (autoscaleCbx != null) {
            autoscaleCbx.setSelected(value);
        }
    }

    /**
     * Get the AutoScale property.
     *
     * @return The AutoScale
     */
    public boolean getAutoScaleYAxis() {
        return autoScaleYAxis;
    }



    /**
     * Check to see if the unit is convertible with meter or gpm
     *
     * @param u  Unit to check
     *
     * @return true if convertible with meter or gpm
     *
     * @throws VisADException Unit Exception
     */
    private boolean isTopography(Unit u) throws VisADException {
        if (u == null) {
            return false;
        }
        return Unit.canConvert(u, CommonUnit.meter)
               || Unit.canConvert(
                   u, GeopotentialAltitude.getGeopotentialMeter());
    }


    /**
     * Set the AutoUpdate property.
     *
     * @param value The new value for AutoUpdate
     */
    public void setAutoUpdate(boolean value) {
        autoUpdate = value;
    }

    /**
     * Get the AutoUpdate property.
     *
     * @return The AutoUpdate
     */
    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    /**
     * Class MyTabbedPane handles the visad component in a tab
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.173 $
     */
    private class MyTabbedPane extends JTabbedPane implements ChangeListener {

        /** Have we been painted */
        boolean painted = false;

        /**
         * ctor
         */
        public MyTabbedPane() {
            addChangeListener(this);
        }

        /**
         *
         * Handle when the tab has changed. When we move to tab 1 then hide the heavy
         * component. Show it on change to tab 0.
         *
         * @param e The event
         */
        public void stateChanged(ChangeEvent e) {
            if ( !getActive() || !getHaveInitialized()) {
                return;
            }
            if ((crossSectionView == null)
                    || (crossSectionView.getContents() == null)) {
                return;
            }
            if (getSelectedIndex() == 0) {
                crossSectionView.getContents().setVisible(true);
            } else {
                crossSectionView.getContents().setVisible(false);
            }
        }


        /**
         * The first time we paint toggle the selected index. This seems to get rid of
         * screen crud
         *
         * @param g graphics
         */
        public void paint(java.awt.Graphics g) {
            if ( !painted) {
                painted = true;
                setSelectedIndex(1);
                setSelectedIndex(0);
                repaint();
            }
            super.paint(g);
        }
    }



    /**
     *  Set the CrossSectionView property.
     *
     *  @param value The new value for CrossSectionView
     */
    public void setCrossSectionView(CrossSectionViewManager value) {
        crossSectionView = value;
    }

    /**
     *  Get the CrossSectionView property.
     *
     *  @return The CrossSectionView
     */
    public CrossSectionViewManager getCrossSectionView() {
        return crossSectionView;
    }



    /**
     * Set the foreground color
     *
     * @param color    new color
     * @deprecated Keep this around for old bundles
     */
    public void setForeground(Color color) {
        this.foreground = color;
    }


    /**
     * Set the background color
     *
     * @param color   new color
     * @deprecated Keep this around for old bundles
     */
    public void setBackground(Color color) {
        this.background = color;
    }



    /**
     *  Set the DisplayMatrix property.
     *  @param value The new value for DisplayMatrix
     * @deprecated Keep this around for old bundles
     */
    public void setDisplayMatrix(double[] value) {
        displayMatrix = value;
    }


    /**
     * Can this display control write out data.
     * @return true if it can
     */
    public boolean canExportData() {
        return true;
    }

    /**
     * Get the DisplayedData
     * @return the data or null
     *
     * @throws RemoteException problem reading remote data
     * @throws VisADException  problem gettting data
     */
    protected Data getDisplayedData() throws VisADException, RemoteException {
        if ((xsDisplay == null) || (xsDisplay.getData() == null)) {
            return null;
        }
        return xsDisplay.getData();
    }


    /**
     * Get the initial Z position
     *
     * @return the position in Z space
     */
    protected double getInitialZPosition() {
        return .95;
    }





    /**
     * Set the probe position property; used by XML persistence.
     *
     * @param p    probe position
     */
    public void setStartPoint(RealTuple p) {
        initStartPoint = p;
    }

    /**
     * Set the probe position property; used by XML persistence.
     *
     * @return  probe position - may be <code>null</code>.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public RealTuple getStartPoint() throws VisADException, RemoteException {
        return ((csSelector != null)
                ? csSelector.getStartPoint()
                : null);
    }

    /**
     * Set the probe position property; used by XML persistence.
     *
     * @param p    probe position
     */
    public void setEndPoint(RealTuple p) {
        initEndPoint = p;
    }

    /**
     * Set the probe position property; used by XML persistence.
     *
     * @return  probe position - may be <code>null</code>.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public RealTuple getEndPoint() throws VisADException, RemoteException {
        return ((csSelector != null)
                ? csSelector.getEndPoint()
                : null);
    }

    /**
     *  Set the LineVisible property.
     *
     *  @param value The new value for LineVisible
     */
    public void setLineVisible(boolean value) {
        lineVisible = value;
    }

    /**
     *  Get the LineVisible property.
     *
     *  @return The LineVisible
     */
    public boolean getLineVisible() {
        return lineVisible;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setInitAlt(double value) {
        initAlt = value;
    }


    /**
     *  Set the Lat2 property.
     *
     *  @param value The new value for Lat2
     */
    public void setInitLat2(double value) {
        initLat2 = value;
    }


    /**
     *  Set the Lon2 property.
     *
     *  @param value The new value for Lon2
     */
    public void setInitLon2(double value) {
        initLon2 = value;
    }


    /**
     *  Set the InitLat1 property.
     *
     *  @param value The new value for InitLat1
     */
    public void setInitLat1(double value) {
        initLat1 = value;
    }


    /**
     *  Set the Lon1 property.
     *
     *  @param value The new value for Lon1
     */
    public void setInitLon1(double value) {
        initLon1 = value;
    }



}
