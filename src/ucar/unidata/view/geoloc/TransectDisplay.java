/*
 * $Id: TransectDisplay.java,v 1.41 2007/08/09 20:46:19 dmurray Exp $
 *
 * Copyright  1997-2006 Unidata Program Center/University Corporation for
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



package ucar.unidata.view.geoloc;


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.display.*;
import ucar.visad.quantities.*;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.GeopotentialAltitude;

import visad.*;

import visad.georef.*;

import visad.java3d.*;

import visad.util.DataUtility;


import java.awt.*;
import java.awt.event.*;

import java.awt.geom.Rectangle2D;

import java.beans.*;

import java.net.URL;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;


/**
 * Provides a navigated globe for displaying meteorological data.
 * Any displayable data must be able to map to RealType.Latitude,
 * RealType.Longitude and/or RealType.Altitude.
 *
 * @author Don Murray
 * @version $Revision: 1.41 $ $Date: 2007/08/09 20:46:19 $
 */
public class TransectDisplay extends NavigatedDisplay implements DisplayListener,
        ControlListener {


    /** threshold for caching data */
    private static final int CACHE_THRESHOLD = 100;

    /** cache size */
    private static final int CACHE_SIZE = 10;

    /** default z value */
    public static final float DEFAULT_Z = 0.f;

    /** latitude ScalarMap */
    private ScalarMap latitudeMap = null;

    /** longitude ScalarMap */
    private ScalarMap longitudeMap = null;

    /** altitude ScalarMap */
    private ScalarMap altitudeMap = null;

    /** ScalarMap to Display.XAxis */
    private ScalarMap xMap = null;

    /** ScalarMap to Display.YAxis */
    private ScalarMap yMap = null;

    /** ScalarMap to Display.ZAxis */
    private ScalarMap zMap = null;

    /** ScalarMap of Length (distance) to Display.XAxis */
    private ScalarMap distanceMap = null;

    /** ScalarMap of Height to Display.YAxis */
    private ScalarMap heightMap = null;

    /** ScalarMap of Pressure to Display.YAxis */
    private ScalarMap pressureMap = null;

    /** The display's Latitude DisplayRealType */
    private DisplayRealType displayLatitudeType;

    /** The display's Longitude DisplayRealType */
    private DisplayRealType displayLongitudeType;

    /** The display's Altitude DisplayRealType */
    private DisplayRealType displayAltitudeType;

    /** the display tuple type */
    private DisplayTupleType displayTupleType;

    /** vertical height axis scale */
    private AxisScale heightScale = null;

    /** vertical pressure axis scale */
    private AxisScale pressureScale = null;

    /** X axis scale */
    private AxisScale latlonScale = null;

    /** distance axis scale */
    private AxisScale distanceScale = null;

    /** minimum range for altitudeMap */
    private double altitudeMin = 0;

    /** maximum range for altitudeMap */
    private double altitudeMax = 16000;

    /** flag for whether this has been initialized or not */
    private boolean init = false;

    /** display coordinate system */
    private CoordinateSystem coordinateSystem = null;

    /** instance counter */
    private static int instance = 0;

    /** instance locking mutex */
    private static Object INSTANCE_MUTEX = new Object();

    /** units for cs */
    private Unit[] csUnits = new Unit[] { CommonUnit.degree,
                                          CommonUnit.degree,
                                          CommonUnit.meter };

    /** lat/lon rectangle defining endpoints */
    private Gridded2DSet transect = null;

    /** default vertical parameter */
    private RealType verticalParameter = RealType.Altitude;

    /** default surface value */
    private Real surface = new Real(RealType.Altitude, 0);

    /** default vertical range unit */
    private Unit verticalRangeUnit = CommonUnit.meter;

    /** default horizontal range unit */
    private Unit horizontalRangeUnit = CommonUnits.KILOMETER;

    /** default view */
    private int view = ProjectionControlJ3D.Z_PLUS;

    /** number format for axis labels */
    DecimalFormat labelFormat = new DecimalFormat("####0.0");

    /** Set of vertical maps */
    private VerticalMapSet verticalMapSet = new VerticalMapSet();

    /** flag for grid lines */
    private boolean gridLinesVisible = true;

    /** scale offset from side */
    private double SCALE_OFFSET = 1. / 9.;

    /** max distance away from transect */
    private Real maxDistance = null;

    /** default pressure labels */
    public static String[] DEFAULT_PRESSURE_LABELS = new String[] {
        "1000", "850", "700", "500", "400", "300", "250", "200", "150", "100"
    };

    /** pressure labels being used */
    private String[] pressureLabels = DEFAULT_PRESSURE_LABELS;

    /** the minimum pressure */
    private double pressureMin = 1013.25;

    /** the axis font */
    private Font axisFont;

    /** cache infos */
    List cacheInfos = new ArrayList();

    /**
     * Constructs a new TransectDisplay to display data as an XY plot
     * along a lat/lon transect.  The transect can have multiple segments.
     *
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public TransectDisplay() throws VisADException, RemoteException {
        this(makeDefaultLine());
    }

    /**
     * Constructs a new TransectDisplay to display data as an XY plot
     * along a lat/lon transect.  The transect can have multiple segments.
     *
     * @param line  the lat/lon transect line
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public TransectDisplay(Gridded2DSet line)
            throws VisADException, RemoteException {
        this(line, false, null);
    }



    /**
     * Constructs a new TransectDisplay to display data as an XY plot
     * along a lat/lon transect.  The transect can have multiple segments.
     *
     * @param line  the lat/lon transect line
     * @param offscreen true if offscreen
     * @param dimension Dimension of the screen
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public TransectDisplay(Gridded2DSet line, boolean offscreen,
                           Dimension dimension)
            throws VisADException, RemoteException {

        if (offscreen) {
            if (dimension == null) {
                dimension = new Dimension(600, 400);
            }
            setOffscreenDimension(dimension);
        }
        DisplayImpl displayImpl = null;
        int         api         = (offscreen
                                   ? DisplayImplJ3D.OFFSCREEN
                                   : DisplayImplJ3D.JPANEL);
        if (offscreen) {
            displayImpl = new DisplayImplJ3D("TransectDisplay",
                                             new TransectDisplayRenderer(),
                                             dimension.width,
                                             dimension.height);
        } else {
            displayImpl = new DisplayImplJ3D("TransectDisplay",
                                             new TransectDisplayRenderer());
        }



        super.init(displayImpl);


        if (line.getManifoldDimension() != 1) {
            throw new VisADException(
                "Set needs to define a line, not a grid");
        }
        maxDistance      = new Real(Length.getRealType());
        transect         = ensureLatLon(line);
        coordinateSystem = new TransectCoordinateSystem(line);
        setBoxVisible(false);
        setScalesVisible(true);
        setMapParameters();
        initializeClass();
    }

    /**
     * Initialize the class.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void initializeClass() throws VisADException, RemoteException {
        super.initializeClass();
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        KeyboardBehaviorJ3D kb = new KeyboardBehaviorJ3D(rend);
        /*        kb.mapKeyToFunction(kb.ROTATE_Z_POS, KeyEvent.VK_Z,
                            InputEvent.CTRL_MASK);
        kb.mapKeyToFunction(kb.ROTATE_Z_NEG, KeyEvent.VK_Z,
                            InputEvent.SHIFT_MASK);
        kb.mapKeyToFunction(KeyboardBehaviorJ3D.TRANSLATE_LEFT, KeyEvent.VK_LEFT,
                            KeyboardBehaviorJ3D.NO_MASK);
        kb.mapKeyToFunction(KeyboardBehaviorJ3D.TRANSLATE_RIGHT, KeyEvent.VK_RIGHT,
                            KeyboardBehaviorJ3D.NO_MASK);
        */
        rend.addKeyboardBehavior(kb);
        setKeyboardBehavior(kb);
        setPerspectiveView(false);
        // Create a RubberBandBox
        RubberBandBox rubberBandBox = new RubberBandBox(RealType.XAxis,
                                          RealType.YAxis,
                                          InputEvent.SHIFT_MASK);
        rubberBandBox.addAction(new ActionImpl("RBB Action") {
            public void doAction() throws VisADException, RemoteException {
                RubberBandBox box = getRubberBandBox();
                if ((box == null) || (box.getBounds() == null)) {
                    return;
                }
                setMapRegion(box.getBounds());
            }
        });
        setRubberBandBox(rubberBandBox);
        enableRubberBanding(true);
        // set the moust functions
        int[][][] functions = new int[][][] {
            // 0 = left mouse
            {
                // 0 = no shift       1=shift
                { MouseHelper.DIRECT, MouseHelper.DIRECT },
                // 0 = no ctrl        1=ctrl
                { MouseHelper.DIRECT, MouseHelper.DIRECT }
            },
            // 1 = centre mouse
            {
                { MouseHelper.NONE, MouseHelper.NONE },
                { MouseHelper.NONE, MouseHelper.NONE }
            },
            // 2 = right mouse
            {
                { MouseHelper.TRANSLATE, MouseHelper.ZOOM },
                { MouseHelper.TRANSLATE, MouseHelper.ZOOM }
            }
        };
        setMouseFunctions(functions);

        getDisplay().getGraphicsModeControl().setPolygonOffsetFactor(1);
        getDisplay().getProjectionControl().addControlListener(this);
        addDisplayListener(this);
    }


    /**
     * Get the scale end points
     *
     * @return the end points of the scale
     */
    public EarthLocation[] getScaleEndPoints() {
        Rectangle bounds = getScreenBounds();
        EarthLocation leftEl =
            getEarthLocation(getSpatialCoordinatesFromScreen(0
                + (int) (SCALE_OFFSET * bounds.width), bounds.height, 0));
        EarthLocation rightEl =
            getEarthLocation(getSpatialCoordinatesFromScreen(bounds.width
                - (int) (SCALE_OFFSET * bounds.width), bounds.height, 0));
        return new EarthLocation[] { leftEl, rightEl };
    }


    /**
     * Add a keyboard behavior for this display
     *
     * @param behavior  behavior to add
     */
    public void addKeyboardBehavior(KeyboardBehavior behavior) {
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        KeyboardBehaviorWrapper3D beh = new KeyboardBehaviorWrapper3D(rend,
                                            behavior);
        rend.addKeyboardBehavior(beh);
    }


    /**
     * Handle a DisplayEvent
     *
     * @param event  event to handle
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void displayChanged(DisplayEvent event)
            throws VisADException, RemoteException {
        int id = event.getId();
        if (id == DisplayEvent.MAPS_CLEARED) {
            getDisplay().getProjectionControl().addControlListener(this);
        }
    }

    /**
     * Handle a change to the control
     *
     * @param ce ControlEvent
     */
    public void controlChanged(ControlEvent ce) {
        if (isClippingEnabled()) {
            clipAtScales(true);
        }
    }


    /**
     * Class KeyboardBehaviorWrapper3D
     *
     * @author Unidata development team
     */
    static class KeyboardBehaviorWrapper3D extends KeyboardBehaviorJ3D {

        /** behavior */
        KeyboardBehavior behavior;

        /**
         * Create a wrapper for a KeyboardBehaviorJ3D.
         *
         * @param rend       display renderer
         * @param behavior   behavior to wrap
         *
         */
        public KeyboardBehaviorWrapper3D(DisplayRendererJ3D rend,
                                         KeyboardBehavior behavior) {
            super(rend);
            this.behavior = behavior;
        }

        /**
         * Wrapper for behavior mapKeyToFunction
         *
         * @param function   function to map
         * @param keycode    key for function
         * @param modifiers  key modifiers
         */
        public void mapKeyToFunction(int function, int keycode,
                                     int modifiers) {
            //This method does not work because it is called by the super class's ctor
            //before we have a chance to set the behavior
            if (behavior != null) {
                behavior.mapKeyToFunction(function, keycode, modifiers);
            }
        }

        /**
         * Wrapper around KeyboardBehavior.processKeyEvent
         *
         * @param event  event to process
         */
        public void processKeyEvent(java.awt.event.KeyEvent event) {
            behavior.processKeyEvent(event);
        }

        /**
         * Wrapper around KeyboardBehavior.execFuntion
         *
         * @param function  function to execute
         */
        public void execFunction(int function) {
            behavior.execFunction(function);
        }
    }

    /**
     * Create a default line for this class
     *
     * @return a default line
     *
     * @throws VisADException problem constructing the set
     */
    private static Gridded2DSet makeDefaultLine() throws VisADException {
        return new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                                new float[][] {
            { -90, 90 }, { -180, 180 }
        }, 2, null, null, null, false);
    }

    /**
     * Handles a change to the cursor position.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void cursorMoved() throws VisADException, RemoteException {
        updateLocation(
            getEarthLocation(getDisplay().getDisplayRenderer().getCursor()));
    }

    /**
     * Handles a change in the position of the mouse-pointer.
     *
     * @param x    x mouse position
     * @param y    y mouse position
     *
     * @throws RemoteException    Java RMI problem
     * @throws UnitException      Unit conversion problem
     * @throws VisADException     VisAD problem
     */
    protected void pointerMoved(int x, int y)
            throws UnitException, VisADException, RemoteException {

        /*
         * Convert from (pixel, line) Java Component coordinates to (latitude,
         * longitude)
         */
        /* TODO: figure out why this won't work
       updateLocation(getEarthLocation(getSpatialCoordinatesFromScreen(x,
               y)));
               */
        VisADRay ray = getRay(x, y);

        updateLocation(getEarthLocation(new double[] { ray.position[0],
                ray.position[1], ray.position[2] }));
    }

    /**
     * Define the set of spatial scalar maps that this display will
     * use.  Every time a new projection is set, a new set of DisplayTypes
     * is created with a coordinate system for transposing between
     * projection space and xyz space.  The mappings are:
     * <UL>
     * <LI>RealType.Latitude  -> getDisplayLatitudeType()
     * <LI>RealType.Longitude -> getDisplayLongitudeType()
     * <LI>RealType.Altitude  -> getDisplayAltitudeType()
     * </UL>
     * This is called on construction of the display or with every rebuild.
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    private void setSpatialScalarMaps()
            throws VisADException, RemoteException {

        setDisplayInactive();
        ScalarMapSet mapSet = new ScalarMapSet();

        if (latitudeMap != null) {
            removeScalarMap(latitudeMap);
        }
        latitudeMap = new ScalarMap(RealType.Latitude, displayLatitudeType);
        mapSet.add(latitudeMap);
        latitudeMap.setRangeByUnits();
        latitudeMap.setScaleEnable(false);

        if (longitudeMap != null) {
            removeScalarMap(longitudeMap);
        }
        longitudeMap = new ScalarMap(RealType.Longitude,
                                     displayLongitudeType);
        mapSet.add(longitudeMap);
        longitudeMap.setRangeByUnits();
        longitudeMap.setScaleEnable(false);

        //altitudeMap = new ScalarMap(RealType.Altitude, displayAltitudeType);
        //altitudeMap.setScaleEnable(true);
        //mapSet.add(altitudeMap);
        ScalarMapSet newVertMaps = new ScalarMapSet();
        if (verticalMapSet.size() > 0) {
            for (Iterator iter =
                    verticalMapSet.iterator(); iter.hasNext(); ) {
                ScalarType r      = ((ScalarMap) iter.next()).getScalar();
                ScalarMap  newMap = new ScalarMap(r, displayAltitudeType);
                newMap.setScaleEnable(false);
                if (r.equals(RealType.Altitude)) {
                    altitudeMap = newMap;
                }
                newVertMaps.add(newMap);
            }
        } else {  // add Altitude at least
            altitudeMap = new ScalarMap(RealType.Altitude,
                                        displayAltitudeType);
            altitudeMap.setScaleEnable(false);
            newVertMaps.add(altitudeMap);
        }
        removeScalarMaps(verticalMapSet);
        verticalMapSet.clear();
        verticalMapSet.add(newVertMaps);
        mapSet.add(verticalMapSet);



        if ( !init) {
            xMap = new ScalarMap(RealType.XAxis, Display.XAxis);
            xMap.setRange(-1.0, 1.0);
            mapSet.add(xMap);
            xMap.setScaleEnable(true);
            latlonScale = xMap.getAxisScale();
            latlonScale.setScreenBased(true);
            latlonScale.setSide(AxisScale.SECONDARY);
            latlonScale.setLabelSize(10);

            distanceMap = new ScalarMap(Length.getRealType(), Display.XAxis);
            mapSet.add(distanceMap);
            distanceMap.setScaleEnable(true);
            distanceScale = distanceMap.getAxisScale();
            distanceScale.setScreenBased(true);
            distanceScale.setLabelAllTicks(true);
            distanceScale.setSide(AxisScale.PRIMARY);
            //distanceScale.setLabelBothSides(true);
            distanceScale.setLabelSize(10);

            yMap = new ScalarMap(RealType.YAxis, Display.YAxis);
            yMap.setRange(-1.0, 1.0);
            mapSet.add(yMap);
            yMap.setScaleEnable(false);

            heightMap = new ScalarMap(Length.getRealType(), Display.YAxis);
            mapSet.add(heightMap);
            heightMap.setScaleEnable(true);
            heightScale = heightMap.getAxisScale();
            heightScale.setScreenBased(true);
            heightScale.setLabelAllTicks(true);
            //heightScale.setLabelBothSides(true);
            heightScale.setLabelSize(10);

            pressureMap =
                new ScalarMap(RealType.getRealType("PressureForScale"),
                              Display.YAxis);
            mapSet.add(pressureMap);
            pressureMap.setScaleEnable(true);
            pressureScale = pressureMap.getAxisScale();
            pressureScale.setScreenBased(true);
            pressureScale.setSide(AxisScale.SECONDARY);
            pressureScale.setTicksVisible(false);
            pressureScale.setLabelSize(10);
            setPressureLabels(DEFAULT_PRESSURE_LABELS);

            zMap = new ScalarMap(RealType.ZAxis, Display.ZAxis);
            zMap.setRange(-1.0, 1.0);
            mapSet.add(zMap);
            zMap.setScaleEnable(false);

            init = true;
        }
        setFontOnScales();
        setVerticalRange(altitudeMin, altitudeMax);
        setVerticalRangeUnit(verticalRangeUnit);
        makeVerticalScales();

        addScalarMaps(mapSet);
        setDisplayActive();
    }

    /**
     * Add a new mapping of this type to the vertical coordinate
     *
     * @param newVertType  RealType of map
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void addVerticalMap(RealType newVertType)
            throws VisADException, RemoteException {
        if (getDisplayMode() == MODE_3D) {

            Unit u = newVertType.getDefaultUnit();
            if ( !(Unit.canConvert(u, CommonUnit.meter)
                    || Unit.canConvert(
                        u, GeopotentialAltitude.getGeopotentialMeter()))) {
                throw new VisADException("Unable to handle units of "
                                         + newVertType);
            }
            ScalarMap newMap = new ScalarMap(newVertType,
                                             getDisplayAltitudeType());
            setVerticalMapUnit(newMap, verticalRangeUnit);
            newMap.setRange(altitudeMin, altitudeMax);
            verticalMapSet.add(newMap);
            addScalarMaps(verticalMapSet);
        }
    }

    /**
     * Remove a new mapping of this type to the vertical coordinate
     *
     * @param vertType  RealType of map
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void removeVerticalMap(RealType vertType)
            throws VisADException, RemoteException {
        if (getDisplayMode() == MODE_3D) {
            ScalarMapSet sms = new ScalarMapSet();
            for (Iterator iter =
                    verticalMapSet.iterator(); iter.hasNext(); ) {
                ScalarMap s = (ScalarMap) iter.next();
                if (((RealType) s.getScalar()).equals(vertType)) {
                    sms.add(s);
                }
            }
            if ( !(sms.size() == 0)) {
                verticalMapSet.remove(sms);
                removeScalarMaps(sms);
            }
        }
    }

    /**
     * Set the Unit of the vertical range
     *
     * @param  newUnit  unit of range
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void setVerticalRangeUnit(Unit newUnit)
            throws VisADException, RemoteException {

        super.setVerticalRangeUnit(newUnit);
        if ((newUnit != null) && Unit.canConvert(newUnit, CommonUnit.meter)) {
            verticalMapSet.setVerticalUnit(newUnit);
            verticalRangeUnit = newUnit;
        }
        heightMap.setOverrideUnit(newUnit);
        makeVerticalScales();
    }

    /**
     * Set the range of the vertical coordinate
     *
     * @param  min  minimum value for vertical axis
     * @param  max  maximum value for vertical axis
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void setVerticalRange(double min, double max)
            throws VisADException, RemoteException {
        super.setVerticalRange(min, max);
        System.err.println("min:" + min +" max:" + max);

        verticalMapSet.setVerticalRange(min, max);
        altitudeMin = min;
        altitudeMax = max;
        heightMap.setRange(min, max);
        pressureMap.setRange(min, max);
        makeVerticalScales();
    }


    /**
     * Get the range of the vertical coordinate (Altitude)
     *
     * @return array of {min, max} range.
     */
    public double[] getVerticalRange() {
        ScalarMap vertMap = getAltitudeMap();
        return (vertMap != null)
               ? vertMap.getRange()
               : new double[] { altitudeMin, altitudeMax };
    }

    /**
     * Set the Unit of the horizontal range
     *
     * @param  newUnit  unit of range
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void setHorizontalRangeUnit(Unit newUnit)
            throws VisADException, RemoteException {

        if ((newUnit != null) && Unit.canConvert(newUnit, CommonUnit.meter)) {
            horizontalRangeUnit = newUnit;
        }
        distanceMap.setOverrideUnit(newUnit);
        setTransectRange();
    }

    /**
     * Get the Unit of the horizontal range
     *
     * @return  unit of horizontal range
     */
    public Unit getHorizontalRangeUnit() {
        return horizontalRangeUnit;
    }

    /**
     * Set the vertical axis scale
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    private void makeVerticalScales() throws VisADException, RemoteException {
        setDisplayInactive();
        if (heightScale != null) {
            double[] zRange = heightMap.getRange();
            String title = "Altitude (" + verticalRangeUnit.getIdentifier()
                           + ")";
            heightScale.setSnapToBox(true);
            heightScale.setTitle(title);
            heightScale.setGridLinesVisible(gridLinesVisible);
            heightScale.setTickBase(zRange[0]);
        }
        if (pressureScale != null) {
            setPressureLabels(pressureLabels);
            String title = "Pressure (" + CommonUnits.MILLIBAR + ")";
            pressureScale.setSnapToBox(true);
            pressureScale.setTitle(title);
        }
        setDisplayActive();
    }

    /**
     * Method called to reset all the map parameters after a change.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void setMapParameters() throws VisADException, RemoteException {
        setDisplayInactive();
        if (displayAltitudeType == null) {
            setDisplayTypes();
        }
        //resetProjection();  // make it the right size
        //setAspect();
        makeLatLonScale();
        setTransectRange();
        //rebuild();
        reDisplayAll();
        setDisplayActive();
    }

    /**
     * Set the maximum distance away from the transect for
     * data to be displayed.
     *
     * @param r  value of distance
     *
     * @throws VisADException  incompatible unit
     */
    public void setMaxDataDistance(Real r) throws VisADException {
        if ( !Unit.canConvert(r.getUnit(), SI.meter)) {
            throw new VisADException("Value must be in units of length");
        }
        maxDistance = r;
        cacheInfos  = new ArrayList();
        reDisplayAll();
    }

    /**
     * Set the maximum distance away from the transect for
     * data to be displayed.
     * @param distance distance in horizontal range units
     *
     * @throws VisADException  unable to create the VisAD object or
     *                         incompatible units.
     */
    public void setMaxDataDistance(double distance) throws VisADException {
        setMaxDataDistance(new Real((RealType) maxDistance.getType(),
                                    distance, getHorizontalRangeUnit()));
    }

    /**
     * Set the maximum distance away from the transect for
     * data to be displayed.
     *
     * @return the maximum distance
     */
    public Real getMaxDataDistance() {
        return maxDistance;
    }

    /**
     * Set the visibility of the axis grid lines.
     *
     * @param  on  true if the grid lines should be visible
     *
     * @throws  VisADException    Couldn't create the necessary VisAD object
     * @throws  RemoteException   If there was a problem making this
     *                               change in a remote collaborative display.
     */
    public void setGridLinesVisible(boolean on)
            throws VisADException, RemoteException {
        gridLinesVisible = on;
        makeScales();
    }

    /**
     * Get the box visibility.
     *
     * @return true if box is visible, otherwise false.
     */
    public boolean getScalesVisible() {
        return getDisplay().getGraphicsModeControl().getScaleEnable();
    }

    /**
     * Make the scales.
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    private void makeScales() throws VisADException, RemoteException {
        makeLatLonScale();
        makeVerticalScales();
        makeDistanceScale();
    }

    /**
     * Set the lat/lon scales
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    private void makeLatLonScale() throws VisADException, RemoteException {
        if ((latlonScale == null) || (coordinateSystem == null)) {
            return;
        }
        setDisplayInactive();
        double[] xRange = xMap.getRange();
        latlonScale.setSnapToBox(true);
        latlonScale.setTitle("Location");
        Hashtable       labelTable = new Hashtable();
        float[][]       linePoints = transect.getSamples();
        int             numpoints  = linePoints[0].length;
        float[][] xyzPoints = coordinateSystem.toReference(new float[][] {
            linePoints[0], linePoints[1], new float[numpoints]
        });
        LatLonPointImpl workPoint  = new LatLonPointImpl();
        for (int i = 0; i < xyzPoints[0].length; i++) {
            workPoint.set(linePoints[0][i], linePoints[1][i]);
            labelTable.put(new Double(xyzPoints[0][i]), workPoint.toString());
        }
        latlonScale.setLabelTable(labelTable);
        latlonScale.setTickBase(xRange[0]);
        latlonScale.setMajorTickSpacing(Math.abs(xRange[1] - xRange[0]));
        latlonScale.setMinorTickSpacing(Math.abs(xRange[1] - xRange[0]));
        setDisplayActive();
    }

    /**
     * Set the horizontal distance scale
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    private void makeDistanceScale() throws VisADException, RemoteException {
        if ((distanceScale == null) || (coordinateSystem == null)) {
            return;
        }
        setDisplayInactive();
        double[] distRange = distanceMap.getRange();
        distanceScale.setSnapToBox(true);
        distanceScale.setTitle("Distance (" + horizontalRangeUnit + ")");
        distanceScale.setGridLinesVisible(gridLinesVisible);
        distanceScale.setTickBase(0);
        setDisplayActive();
    }

    /**
     * Set up the DisplayTupleType.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void setDisplayTypes() throws VisADException, RemoteException {
        if (coordinateSystem == null) {
            System.out.println("coordSys == null");
            displayLongitudeType = Display.XAxis;
            displayAltitudeType  = Display.YAxis;
            displayLatitudeType  = Display.ZAxis;
            displayTupleType     = Display.DisplaySpatialCartesianTuple;
        } else {
            int myInstance;
            synchronized (INSTANCE_MUTEX) {
                myInstance = instance++;
            }

            displayLatitudeType = new DisplayRealType("TransectProjectionLat"
                    + myInstance, true, -90.0, 90.0, 0.0, CommonUnit.degree);
            displayLongitudeType =
                new DisplayRealType("TransectProjectionLon" + myInstance,
                                    true, -180, 180, 0.0, CommonUnit.degree);
            displayAltitudeType = new DisplayRealType("TransectProjectionAlt"
                    + myInstance, true, -1.0, 1.0, -1.0, null);
            displayTupleType = new DisplayTupleType(new DisplayRealType[] {
                displayLatitudeType,
                displayLongitudeType,
                displayAltitudeType }, coordinateSystem);
        }
        setSpatialScalarMaps();
    }

    /**
     * Set the range on the distance map from the transect.
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  can't get VisAD Data
     */
    private void setTransectRange() throws VisADException, RemoteException {
        float[][] linePoints = transect.getSamples();
        int       numpoints  = linePoints[0].length;
        float[][] xyzPoints = coordinateSystem.toReference(new float[][] {
            linePoints[0], linePoints[1], new float[numpoints]
        });
        LatLonPointImpl startPoint = new LatLonPointImpl(linePoints[0][0],
                                         linePoints[1][0]);
        LatLonPointImpl endPoint =
            new LatLonPointImpl(linePoints[0][numpoints - 1],
                                linePoints[1][numpoints - 1]);
        Bearing workBearing = new Bearing();
        workBearing = Bearing.calculateBearing(startPoint, endPoint,
                workBearing);
        double distance =
            horizontalRangeUnit.toThis(workBearing.getDistance(),
                                       CommonUnits.KILOMETER);
        if (distanceMap != null) {
            distanceMap.setRange(0., distance);
        }
        makeDistanceScale();
    }

    /**
     * Set the map area to be displayed in the box.  Does nothing at
     * this point.
     *
     * @param mapArea  ProjectionRect describing the map area to be displayed
     * @throws  VisADException         invalid navigation or VisAD error
     * @throws  RemoteException        Couldn't create a remote object
     */
    public void setMapArea(ProjectionRect mapArea)
            throws VisADException, RemoteException {}

    /**
     * Set the map region to be displayed.   The MathType of the domain
     * of the set must be either RealTupleType.SpatialCartesian2DTuple,
     * RealTupleType.SpatialEarth2DTuple, or
     * RealTupleType.LatitudeLongitudeTuple.
     *
     * @param region  Gridded2DSet containing the range of for the axis.
     *
     * @throws  VisADException         invalid domain or null set
     * @throws  RemoteException        Couldn't create a remote object
     */
    public void setMapRegion(Gridded2DSet region)
            throws VisADException, RemoteException {

        if (region == null) {
            throw new VisADException("Region can't be null");
        }
        if (region.isMissing()) {
            return;
        }

        // Check the type.  We need to work in XYZ coordinates
        Gridded2DSet  xyRegion;
        RealTupleType regionType = ((SetType) region.getType()).getDomain();
        if (regionType.equals(RealTupleType.SpatialCartesian2DTuple)) {
            xyRegion = region;
        } else if (regionType.equals(RealTupleType.SpatialEarth2DTuple)
                   || regionType.equals(
                       RealTupleType.LatitudeLongitudeTuple)) {
            // transform to x/y
            int latIndex =
                regionType.equals(RealTupleType.LatitudeLongitudeTuple)
                ? 0
                : 1;
            int       lonIndex = (latIndex == 0)
                                 ? 1
                                 : 0;
            float[][] values   = region.getSamples(true);
            float     xy[][]   = new float[3][values[0].length];
            xy[0]     = values[latIndex];
            xy[1]     = values[lonIndex];
            xy        = coordinateSystem.toReference(xy);
            values[0] = xy[0];
            values[1] = xy[1];
            xyRegion =
                new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                 values, 2);
        } else {
            throw new VisADException("Invalid domain for region "
                                     + regionType);
        }

        //System.out.println(xyRegion);
        // Okay, now we have our region, let's get cracking

        // First, let's figure out our component size.
        Dimension d = getComponent().getSize();
        //System.out.println("Component size = " + d);
        int componentCenterX = d.width / 2;
        int componentCenterY = d.height / 2;
        /*
        */
        //        System.out.println("Component Center point = " + componentCenterX
        //                           + "," + componentCenterY);

        // Now let's get the MouseBehavior so we can get some display coords
        MouseBehavior behavior =
            getDisplay().getDisplayRenderer().getMouseBehavior();
        ProjectionControl proj   = getDisplay().getProjectionControl();
        double[]          aspect = getDisplayAspect();
        //Misc.printArray("aspect", aspect);

        // We have to figure the component coordinates of the region.
        // To do this, we calculate the number of display units per pixel
        // in the x and y.  This logic comes from visad.MouseHelper.
        // Basically, we find out the current matrix, how much we should
        // scale, translate and rotate, and then apply the new matrix.
        double[] center_ray = behavior.findRay(componentCenterX,
                                  componentCenterY).position;
        //Misc.printArray("center_ray", center_ray);
        double[] center_ray_x = behavior.findRay(componentCenterX + 1,
                                    componentCenterY).position;
        //Misc.printArray("center_ray_x", center_ray_x);
        double[] center_ray_y = behavior.findRay(componentCenterX,
                                    componentCenterY + 1).position;
        //Misc.printArray("center_ray_y", center_ray_y);
        /* TODO:  test more to see if this makes a difference.  The
           rubber band box is actually at the Z=-1 position
           double[] center_ray = getRayPositionAtZ(behavior.findRay(componentCenterX,
           componentCenterY), -1);
           Misc.printArray("center_ray @ -1", center_ray);
           double[] center_ray_x = getRayPositionAtZ(behavior.findRay(componentCenterX + 1,
           componentCenterY), -1);
           Misc.printArray("center_ray_x @ -1", center_ray_x);
           double[] center_ray_y = getRayPositionAtZ(behavior.findRay(componentCenterX,
           componentCenterY + 1),-1);
           Misc.printArray("center_ray_y @ -1", center_ray_y);
        */

        double[] tstart = proj.getMatrix();
        //printMatrix("tstart", tstart);
        double[] rot   = new double[3];
        double[] scale = new double[3];
        double[] trans = new double[3];
        behavior.instance_unmake_matrix(rot, scale, trans, tstart);
        double stx = scale[0];
        double sty = scale[1];
        // System.out.println("stx = " + stx);
        // System.out.println("sty = " + sty);
        double[] trot = behavior.make_matrix(rot[0], rot[1], rot[2],
                                             scale[0], scale[1], scale[2],
        //scale[0], 
        0.0, 0.0, 0.0);
        //printMatrix("trot", trot);

        // WLH 17 Aug 2000
        double[] xmat = behavior.make_translate(center_ray_x[0]
                            - center_ray[0], center_ray_x[1] - center_ray[1],
                                             center_ray_x[2] - center_ray[2]);
        //xmat = behavior.multiply_matrix(mult, xmat);
        double[] ymat = behavior.make_translate(center_ray_y[0]
                            - center_ray[0], center_ray_y[1] - center_ray[1],
                                             center_ray_y[2] - center_ray[2]);
        //ymat = behavior.multiply_matrix(mult, ymat);
        double[] xmatmul = behavior.multiply_matrix(trot, xmat);
        double[] ymatmul = behavior.multiply_matrix(trot, ymat);
        /*
          printMatrix("xmat", xmat);
          printMatrix("ymat", ymat);
          printMatrix("xmatmul", xmatmul);
          printMatrix("ymatmul", ymatmul);
        */
        behavior.instance_unmake_matrix(rot, scale, trans, xmatmul);
        double xmul = trans[0];
        behavior.instance_unmake_matrix(rot, scale, trans, ymatmul);
        double ymul = trans[1];
        //System.out.println("Multipliers = " + xmul + "," + ymul);

        // make sure that we don't divide by 0 (happens if display
        // component is not yet on screen
        if ((Math.abs(xmul) > 0) && (Math.abs(ymul) > 0)) {
            // Now we can get the box coordinates in component space
            float[] lows              = xyRegion.getLow();
            float[] highs             = xyRegion.getHi();
            float   boxCenterDisplayX = (highs[0] + lows[0]) / 2.0f;
            float   boxCenterDisplayY = (highs[1] + lows[1]) / 2.0f;
            /*
              System.out.println(
              "Box center point (XY) = " +
              boxCenterDisplayX+","+boxCenterDisplayY);
            */

            // Check to see if the box is big enough (at least 5x5 pixels)
            // *** might want to ammend this to be a percentage of
            // component size ****
            int boxWidth  = (int) Math.abs((highs[0] - lows[0]) / xmul * stx);
            int boxHeight = (int) Math.abs((highs[1] - lows[1]) / ymul * sty);
            /*
              System.out.println(
              "Box size = " + boxWidth +"," + boxHeight);
            */
            if ((boxWidth > 5) && (boxHeight > 5)) {
                int boxCenterX = componentCenterX
                                 + (int) ((boxCenterDisplayX - center_ray[0])
                                          / xmul);
                int boxCenterY = componentCenterY
                                 - (int) ((boxCenterDisplayY - center_ray[1])
                                          / ymul);
                /*
                  System.out.println(
                  "Box Center point = " + boxCenterX +","+boxCenterY);
                */
                double transx = (componentCenterX - boxCenterX) * xmul * stx;
                double transy = (componentCenterY - boxCenterY) * ymul * sty;
                /*
                  System.out.println("transx = " + transx +
                  " transy = " + transy);
                */

                // Now calculate zoom factor
                double zoom = (boxWidth / boxHeight >= d.width / d.height)
                              ? d.getWidth() / boxWidth
                              : d.getHeight() / boxHeight;
                // zoom out if this is a bigger region than the component
                //System.out.println("zoom factor = " + zoom);

                translate(transx, -transy);
                zoom(zoom);
            }
        }
    }

    /**
     * Define the map projection using a MapProjection type CoordinateSystem.
     * Implementation will be subclass dependent.
     *
     * @param  mapProjection   map projection coordinate system
     *
     * @throws  VisADException         Unable to set projection
     * @throws  RemoteException        Couldn't create a remote object
     */
    public void setMapProjection(MapProjection mapProjection)
            throws VisADException, RemoteException {
        Rectangle2D rect   = mapProjection.getDefaultMapArea();
        LatLonRect  region = null;
        if (mapProjection instanceof ProjectionCoordinateSystem) {
            ProjectionImpl proj =
                ((ProjectionCoordinateSystem) mapProjection).getProjection();
            region = proj.getDefaultMapAreaLL();
        } else if (mapProjection instanceof TrivialMapProjection) {
            Rectangle2D     r2d2   = mapProjection.getDefaultMapArea();
            double          x      = r2d2.getX();
            double          y      = r2d2.getY();
            double          width  = r2d2.getWidth();
            double          height = r2d2.getHeight();
            LatLonPointImpl start  = (mapProjection.isLatLonOrder())
                                     ? new LatLonPointImpl(x, y)
                                     : new LatLonPointImpl(y, x);
            region = (mapProjection.isLatLonOrder())
                     ? new LatLonRect(start, width, height)
                     : new LatLonRect(start, height, width);
        } else {
            throw new VisADException("unable to get transect for "
                                     + mapProjection);
        }
        transect = rectToLine(region);
        setMapParameters();
    }

    /**
     * Method to convert a LatLonRect to a Gridded2DSet
     *
     * @param llr  LatLonRect
     * @return rectangle transect as a line from the lower left to upper right
     *
     * @throws VisADException  problem creating a new set
     */
    private static Gridded2DSet rectToLine(LatLonRect llr)
            throws VisADException {
        LatLonPointImpl start = llr.getLowerLeftPoint();
        LatLonPointImpl end   = llr.getUpperRightPoint();
        Gridded2DSet line =
            new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                             new float[][] {
            { (float) start.getLatitude(), (float) end.getLatitude() },
            { (float) start.getLongitude(), (float) end.getLongitude() }
        }, 2,       // 1-D manifold
           (CoordinateSystem) null, (Unit[]) null, (ErrorEstimate[]) null,
           false);  // don't copy
        return line;
    }

    /**
     * Accessor method for the DisplayLatitudeType
     *
     * @return DisplayRealType for Latitude mapping
     */
    public DisplayRealType getDisplayLatitudeType() {
        return displayLatitudeType;
    }

    /**
     * Accessor method for the DisplayLongitudeType
     * @return DisplayRealType for Longitude mapping
     */
    public DisplayRealType getDisplayLongitudeType() {
        return displayLongitudeType;
    }

    /**
     * Accessor method for the DisplayAltitudeType
     * @return DisplayRealType for Altitude mapping
     */
    public DisplayRealType getDisplayAltitudeType() {
        return displayAltitudeType;
    }

    /**
     * Accessor method for the DisplayTupleType.
     * @return the tuple of DisplayRealTypes
     */
    public DisplayTupleType getDisplayTupleType() {
        return displayTupleType;
    }

    /**
     * Accessor method for the ScalarMap for Altitude
     * @return the altitude ScalarMap
     */
    protected ScalarMap getAltitudeMap() {
        return altitudeMap;
    }

    /**
     * Get the transect for this display
     *
     * @return  the transect as a list of points
     */
    protected Gridded2DSet getTransect() {
        return transect;
    }

    /**
     * Set the transect for this display
     *
     * @param  newLine the transect as a rectangle
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setTransect(Gridded2DSet newLine)
            throws VisADException, RemoteException {
        cacheInfos = new ArrayList();
        transect   = newLine;
        setMapParameters();
    }

    /**
     * Handles a change to the cursor position.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void cursorChange() throws VisADException, RemoteException {
        setCursorLatitude(getCursorValue(RealType.Latitude, 0));
        setCursorLongitude(getCursorValue(RealType.Longitude, 1));
        Real fakeAltitude = getCursorValue(RealType.Radius, 2);
        double realValue = fakeAltitude.getValue()
                           * (altitudeMax - altitudeMin) / 2 + altitudeMin;
        setCursorAltitude(new Real(RealType.Altitude, realValue));
    }

    /**
     * Set the view for 3D.  The views are based on the original display
     * as follows:
     * <pre>
     *                        NORTH
     *                      _________
     *                    W |       | E
     *                    E |  TOP  | A
     *                    S | MOTTOB| S
     *                    T |_______| T
     *                        SOUTH
     * </pre>
     * @param  view  one of the static view fields (NORTH_VIEW, SOUTH_VIEW, ..
     *               etc).  In this display, NORTH is the Western Hemisphere,
     *               SOUTH is the Eastern Hemisphere, EAST is the Pacific
     *               region and WEST is the Atlantic Region
     */
    public void setView(int view) {
        /*
        try {
            ProjectionControlJ3D projControl =
                (ProjectionControlJ3D) getDisplay().getProjectionControl();

            switch (view) {

              case BOTTOM_VIEW :  // Bottom
                  projControl.setOrthoView(ProjectionControlJ3D.Z_MINUS);
                  break;

              case NORTH_VIEW :   // North
                  projControl.setOrthoView(ProjectionControlJ3D.Y_PLUS);
                  break;

              case EAST_VIEW :    // East
                  projControl.setOrthoView(ProjectionControlJ3D.X_PLUS);
                  break;

              case TOP_VIEW :     // Top
                  projControl.setOrthoView(ProjectionControlJ3D.Z_PLUS);
                  break;

              case SOUTH_VIEW :   // South
                  projControl.setOrthoView(ProjectionControlJ3D.Y_MINUS);
                  break;

              case WEST_VIEW :    // West
                  projControl.setOrthoView(ProjectionControlJ3D.X_MINUS);
                  break;

              default :           // no-op - unknown projection
                  projControl.setOrthoView(15);
                  break;
            }
        } catch (VisADException e) {
            ;
        } catch (RemoteException re) {
            ;
        }
        */
        this.view = view;
    }

    /**
     * Enable clipping of data at the scale edges
     *
     * @param  clip  true to turn clipping on, otherwise off
     */
    public void enableClipping(boolean clip) {
        clipAtScales(clip);
        super.enableClipping(clip);
    }

    /**
     * Clip at scales
     *
     * @param clip   true to clip
     */
    private void clipAtScales(boolean clip) {
        DisplayRendererJ3D dr =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        Dimension d = getComponent().getSize();
        if ((d.width == 0) && (d.height == 0)) {
            return;
        }
        int      numXPix = (int) (d.width * SCALE_OFFSET);
        double[] xy1     =  // lower left
            getSpatialCoordinatesFromScreen(numXPix, d.height - numXPix);
        double[] xy2     =  // upper right
            getSpatialCoordinatesFromScreen(d.width - numXPix, numXPix);
        try {
            dr.setClip(0, clip, 1.0f, 0.0f, 0.0f, (float) -xy2[0]);
            dr.setClip(1, clip, -1.0f, 0.0f, 0.0f, (float) xy1[0]);
            dr.setClip(2, clip, 0.0f, 1.0f, 0.0f, (float) -xy2[1]);
            dr.setClip(3, clip, 0.0f, -1.0f, 0.0f, (float) xy1[1]);
        } catch (VisADException ve) {
            System.err.println("Couldn't set clipping " + ve);
        }
    }

    /**
     * Get the coordinates of the left and right ends of the the
     * horizontal axis.
     * @return axis points  [left/right][x/y]
     */
    public double[][] getXAxisEndPoints() {

        DisplayRendererJ3D dr =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        Dimension d = getComponent().getSize();
        if ((d.width == 0) && (d.height == 0)) {
            return null;
        }
        int      numXPix = d.width / 9;
        double[] xy1     =  // lower left
            getSpatialCoordinatesFromScreen(numXPix, d.height - numXPix);
        double[] xy2 =      // lower right
            getSpatialCoordinatesFromScreen(d.width - numXPix,
                                            d.height - numXPix);
        return new double[][] {
            xy1, xy2
        };
    }

    /**
     * Get the coordinates of the left and right ends of the the
     * horizontal axis.
     * @return axis points  [left/right][x/y]
     */
    public double[][] getYAxisEndPoints() {

        DisplayRendererJ3D dr =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        Dimension d = getComponent().getSize();
        if ((d.width == 0) && (d.height == 0)) {
            return null;
        }
        int      numXPix = d.width / 9;
        double[] xy1     =  // lower left
            getSpatialCoordinatesFromScreen(numXPix, d.height - numXPix);
        double[] xy2     =  // upper left
            getSpatialCoordinatesFromScreen(numXPix, numXPix);
        return new double[][] {
            xy1, xy2
        };
    }

    /**
     * Override super method
     *
     * @param aspect   the new aspect
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void setDisplayAspect(double[] aspect)
            throws VisADException, RemoteException {
        super.setDisplayAspect(aspect);
        //getDisplay().getProjectionControl().setAspectCartesian(aspect);
    }

    /**
     * Set the view to perspective or parallel if this is a 3D display.
     *
     * @param perspective  true for perspective view
     */
    public void setPerspectiveView(boolean perspective) {
        if (perspective == isPerspectiveView()) {
            return;
        }
        try {
            getDisplay().getGraphicsModeControl().setProjectionPolicy(
                (perspective == true)
                ? DisplayImplJ3D.PERSPECTIVE_PROJECTION
                : DisplayImplJ3D.PARALLEL_PROJECTION);

        } catch (Exception e) {
            ;
        }
        super.setPerspectiveView(perspective);
    }

    /**
     * Get the earth location from the VisAD xyz coodinates
     *
     * @param x x
     * @param y y
     * @param z z
     * @param  setZToZeroIfOverhead If in the overhead view then set Z to 0
     *
     * @return corresponding EarthLocation
     */
    public EarthLocation getEarthLocation(double x, double y, double z,
                                          boolean setZToZeroIfOverhead) {
        EarthLocationTuple value = null;
        try {
            float[][] numbers = coordinateSystem.fromReference(new float[][] {
                new float[] { (float) (x) }, new float[] { (float) (y) },
                new float[] { (float) (z) }
            });
            Real lat = new Real(RealType.Latitude,
                                getScaledValue(latitudeMap, numbers[0][0]),
                                csUnits[0]);
            Real lon = new Real(RealType.Longitude,
                                getScaledValue(longitudeMap, numbers[1][0]),
                                csUnits[1]);
            Real alt = new Real(RealType.Altitude,
                                getScaledValue(altitudeMap, numbers[2][0]));
            value = new EarthLocationTuple(lat, lon, alt);
        } catch (VisADException e) {
            e.printStackTrace();
        }  // can't happen
                catch (RemoteException e) {
            e.printStackTrace();
        }  // can't happen
        return value;
    }

    /**
     * Returns the spatial (XYZ) coordinates of the particular EarthLocation
     *
     * @param el    earth location to transform
     *
     * @return  RealTuple of display coordinates.
     */
    public RealTuple getSpatialCoordinates(EarthLocation el) {

        if (el == null) {
            throw new NullPointerException(
                "MapProjectionDisplay.getSpatialCoorindate():  "
                + "null input EarthLocation");
        }
        RealTuple spatialLoc = null;
        try {
            double[] xyz = getSpatialCoordinates(el, null);
            spatialLoc = new RealTuple(RealTupleType.SpatialCartesian3DTuple,
                                       xyz);

        } catch (VisADException e) {
            e.printStackTrace();
        }  // can't happen
                catch (RemoteException e) {
            e.printStackTrace();
        }  // can't happen
        return spatialLoc;
    }



    /**
     * Returns the spatial (XYZ) coordinates of the particular EarthLocation
     *
     * @param el    earth location to transform
     * @param xyz    The in value to set. May be null.
     * @param altitude _more_
     *
     * @return  xyz array
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public double[] getSpatialCoordinates(EarthLocation el, double[] xyz,
                                          double altitude)
            throws VisADException, RemoteException {
        float[][] temp = coordinateSystem.toReference(new float[][] {
            latitudeMap.scaleValues(new double[] {
                el.getLatitude().getValue(CommonUnit.degree) }),
            longitudeMap.scaleValues(new double[] {
                el.getLongitude().getValue(CommonUnit.degree) }),
            altitudeMap.scaleValues(new double[] { altitude })
        });
        if (xyz == null) {
            xyz = new double[3];
        }
        xyz[0] = temp[0][0];
        xyz[1] = temp[1][0];
        xyz[2] = temp[2][0];
        return xyz;
    }



    /**
     * Returns the value of the cursor as a particular Real.
     *
     * @param realType          The type to be returned.
     * @param index             The index of the cursor array to access.
     *
     * @return cursor value for the particular index.
     */
    private Real getCursorValue(RealType realType, int index) {
        double[] cursor = getDisplay().getDisplayRenderer().getCursor();
        Real     value  = null;
        try {
            value = new Real(realType,
                             coordinateSystem.fromReference(new double[][] {
                new double[] { cursor[0] }, new double[] { cursor[1] },
                new double[] { cursor[2] }
            })[index][0], csUnits[index]);
        } catch (VisADException e) {
            e.printStackTrace();
        }  // can't happen
        return value;
    }


    /**
     * Determine if this MapDisplay can do stereo.
     *
     * @return true if the graphics device can do stereo
     */
    public boolean getStereoAvailable() {
        return false;
    }

    /**
     * Get the latlon box of the displayed area
     *
     * @return lat lon box  or null if it can't be determined
     * public Rectangle2D.Double getLatLonBox() {
     *   return latLonBox;
     * }
     */

    /**
     * Get the display coordinate system that turns lat/lon/alt to
     * x/y/z
     *
     * @return  the coordinate system (may be null)
     */
    public CoordinateSystem getDisplayCoordinateSystem() {
        return coordinateSystem;
    }

    /**
     * Set the fonts for the axis scales
     * @param f  Font to use
     */
    public void setScaleFont(Font f) {
        axisFont = f;
        setFontOnScales();
    }

    /**
     * update the font scales
     */
    private void setFontOnScales() {
        if (heightScale != null) {
            heightScale.setFont(axisFont);
        }
        if (pressureScale != null) {
            pressureScale.setFont(axisFont);
        }
        if (latlonScale != null) {
            latlonScale.setFont(axisFont);
        }
        if (distanceScale != null) {
            distanceScale.setFont(axisFont);
        }
    }

    /**
     * Ensure that the line is lat/lon order
     *
     * @param line   the line of points
     *
     * @return the line in lat/lon order
     *
     * @throws VisADException problem creating new set
     */
    private Gridded2DSet ensureLatLon(Gridded2DSet line)
            throws VisADException {
        RealTupleType type = ((SetType) line.getType()).getDomain();
        if (type.equals(RealTupleType.LatitudeLongitudeTuple)) {
            return line;
        } else if (type.equals(RealTupleType.SpatialEarth2DTuple)) {
            return new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                                    line.getSamples(false), line.getLength(),
                                    line.getCoordinateSystem(),
                                    line.getSetUnits(), line.getSetErrors(),
                                    false);
        } else {
            throw new VisADException("Line must be lat/lon or lon/lat");
        }
    }


    /**
     * Extend (or condense) the displayed transect.
     *
     * @param amount (%) to extend or contract  > 1 to expand, < 1 to contract
     */
    public void extendTransect(double amount) {
        DisplayRendererJ3D dr =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        Dimension d = getComponent().getSize();
        if ((d.width == 0) && (d.height == 0)) {
            return;
        }
        double[][]    points = getXAxisEndPoints();
        EarthLocation el     = getEarthLocation(points[0][0], points[0][1],
                                   0);
        zoom(amount, 1.0, 1.0);
        MouseBehavior     behavior = dr.getMouseBehavior();
        ProjectionControl proj     = getDisplay().getProjectionControl();
        double[]          tstart   = proj.getMatrix();
        double[]          rot      = new double[3];
        double[]          scale    = new double[3];
        double[]          trans    = new double[3];
        behavior.instance_unmake_matrix(rot, scale, trans, tstart);
        double     stx       = scale[0];
        double[][] newpoints = getXAxisEndPoints();
        double[]   elxyz     = getSpatialCoordinates(el).getValues();
        double     xtrans    = (newpoints[0][0] - elxyz[0]) * stx;
        //System.out.println("translating by " + xtrans);
        translate(xtrans, 0);
    }

    /**
     * Extend (or condense) the displayed transect.
     *
     * @param amount (%) to extend or contract  > 1 to expand, < 1 to contract
     */
    public void extendVerticalRange(double amount) {
        DisplayRendererJ3D dr =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        Dimension d = getComponent().getSize();
        if ((d.width == 0) && (d.height == 0)) {
            return;
        }
        double[][] points = getYAxisEndPoints();
        EarthLocation el = getEarthLocation(points[0][0], points[0][1],
                                            points[0][2]);
        zoom(1.0, amount, 1.0);
        MouseBehavior     behavior = dr.getMouseBehavior();
        ProjectionControl proj     = getDisplay().getProjectionControl();
        double[]          tstart   = proj.getMatrix();
        double[]          rot      = new double[3];
        double[]          scale    = new double[3];
        double[]          trans    = new double[3];
        behavior.instance_unmake_matrix(rot, scale, trans, tstart);
        double     sty       = scale[1];
        double[][] newpoints = getYAxisEndPoints();
        double[]   elxyz     = getSpatialCoordinates(el).getValues();
        double     ytrans    = (newpoints[0][1] - elxyz[1]) * sty;
        //System.out.println("translating by " + ytrans);
        translate(0, ytrans);
    }


    /**
     * Class CacheInfo
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.41 $
     */
    private static class CacheInfo {

        /** lat/lon/alt arrays */
        float[][] lla;

        /** xyzarrays */
        float[][] xyz;

        /** linepoints */
        float[][] linePoints;

        /**
         * Create a new CacheInfo
         *
         * @param lla lat/lon/alt values
         * @param linePoints  corresponding line points
         */
        public CacheInfo(float[][] lla, float[][] linePoints) {
            this.lla        = lla;
            this.linePoints = linePoints;
        }

        /**
         * Create a new CacheInfo
         *
         * @param lla lat/lon/alt values
         * @param xyz xyz values
         * @param linePoints  corresponding line points
         */
        public CacheInfo(float[][] lla, float[][] xyz, float[][] linePoints) {
            this.lla        = Misc.cloneArray(lla);
            this.xyz        = Misc.cloneArray(xyz);
            this.linePoints = Misc.cloneArray(linePoints);
        }

        /**
         * Return a String representation of this object
         *
         * @return  String representation of this object
         */
        public String toString() {
            return "CacheInfo:" + lla[0].length;
        }

        /**
         * Debug
         *
         * @param that a CacheInfo
         */
        public void debug(CacheInfo that) {
            System.err.println("debug:"
                               + Misc.arraysEquals(this.lla, that.lla) + " "
                               + Misc.arraysEquals(this.linePoints,
                                   that.linePoints));
        }

        /**
         * Check for equality
         *
         * @param o  object to check
         *
         * @return true if they are equal
         */
        public boolean equals(Object o) {
            if ( !(o instanceof CacheInfo)) {
                return false;
            }
            CacheInfo that = (CacheInfo) o;
            return Misc.arraysEquals(this.lla, that.lla)
                   && Misc.arraysEquals(this.linePoints, that.linePoints);
        }
    }

    /**
     * Set the labels for the pressure axis
     *
     * @param labels  array of labels
     *
     * @throws VisADException  unable to set the labels on the Axis
     */
    public void setPressureLabels(String[] labels) throws VisADException {
        int       numLabels  = labels.length;
        double    value      = Double.NaN;
        double    values[]   = new double[labels.length];
        Hashtable labelTable = new Hashtable();
        for (int i = 0; i < numLabels; i++) {
            try {
                value = Misc.parseNumber(labels[i]);
            } catch (NumberFormatException ne) {
                value = Double.NaN;
            }
            values[i] = value;
        }
        double[] heights =
            AirPressure.getStandardAtmosphereCS().toReference(new double[][] {
            values
        })[0];
        heights = getVerticalRangeUnit().toThis(heights, CommonUnit.meter,
                true);

        for (int i = 0; i < numLabels; i++) {
            labelTable.put(new Double(heights[i]), labels[i]);
        }
        if (pressureScale != null) {
            pressureScale.setLabelTable(labelTable);
        }
        // set the field here in case there was an error.
        pressureLabels = labels;
    }

    /**
     * Class for converting from lat/lon/alt to xyz
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.41 $
     */
    protected class TransectCoordinateSystem extends CoordinateSystem {

        /** transect */
        private Gridded2DSet transect = null;

        /** working point for toRef calcs */
        //private LatLonPointImpl toRefWorkPoint = new LatLonPointImpl();

        /** start working point for toRef calcs */
        //private LatLonPointImpl startWorkPoint = new LatLonPointImpl();

        /** end working point for toRef calcs */
        //private LatLonPointImpl endWorkPoint = new LatLonPointImpl();

        /** distances to each point in the segment */
        private double[] lenToPoint = null;

        /** distances for each bearing */
        private double[] distances = null;

        /** angles for each bearing */
        private double[] angles = null;

        /** total length of line */
        private double totalLength = 0;

        /** number of points on line */
        private int numLinePoints;

        /** number of segments on line */
        private int numSegments;

        /**
         * Create a new CS
         *
         * @param line  line of lat/lon points
         *
         * @throws VisADException  problem creating the CS
         */
        public TransectCoordinateSystem(Gridded2DSet line)
                throws VisADException {
            super(Display.DisplaySpatialCartesianTuple,
                  new Unit[] { CommonUnit.degree,
                               CommonUnit.degree, null });
            transect = ensureLatLon(line);
            precalculate();
        }


        /**
         * Transform latitude/longitude/altitude value to XYZ
         *
         * @param  latlonalt   array of latitude, longitude, altitude values
         *
         * @return array of display xyz values.
         *
         * @throws VisADException  can't create the necessary VisAD object
         */
        public float[][] toReference(float[][] latlonalt)
                throws VisADException {

            if ((latlonalt == null) || (latlonalt[0].length < 1)) {
                return latlonalt;
            }
            int numpoints = latlonalt[0].length;
            checkForNewLine();
            float[][] linePoints = transect.getSamples(false);
            if (numpoints > CACHE_THRESHOLD) {
                //See if we have this in the cache
                CacheInfo cacheInfo = new CacheInfo(latlonalt, linePoints);
                int       index     = cacheInfos.indexOf(cacheInfo);
                if (index >= 0) {
                    cacheInfo = (CacheInfo) cacheInfos.get(index);
                    return Misc.cloneArray(cacheInfo.xyz);
                }
            }
            Bearing   workBearing = new Bearing();
            float[][] xyz         = new float[3][numpoints];
            call1("toReference(f)", numpoints);
            float[] x               = xyz[0];
            float[] y               = xyz[1];
            float[] z               = xyz[2];
            float[] lat             = latlonalt[0];
            float[] lon             = latlonalt[1];
            float[] alt             = latlonalt[2];

            double  minDistance     = Double.MAX_VALUE;
            double  minAngle        = 0.0;
            double  lastDistance    = 0.0;
            double  lastAngle       = 0.0;
            double  segmentAngle    = angles[0];
            double  len             = lenToPoint[0];
            double  maxDataDistance = Double.MAX_VALUE;
            try {
                maxDataDistance =
                    getMaxDataDistance().getValue(CommonUnits.KILOMETER);
                if (Double.isNaN(maxDataDistance)) {
                    maxDataDistance = Double.MAX_VALUE;
                }
            } catch (Exception e) {
                maxDataDistance = Double.MAX_VALUE;
            }
            for (int i = 0; i < numpoints; i++) {
                if (numLinePoints == 2) {
                    workBearing = Bearing.calculateBearing(linePoints[0][0],
                            linePoints[1][0], lat[i], lon[i], workBearing);

                    minDistance = workBearing.getDistance();
                    minAngle    = workBearing.getAngle();
                } else {  // more than 2 points
                    int closestSegment = 0;
                    for (int j = 0; j < numLinePoints; j++) {
                        workBearing =
                            Bearing.calculateBearing(linePoints[0][j],
                                linePoints[1][j], lat[i], lon[i],
                                workBearing);

                        double dist  = workBearing.getDistance();
                        double angle = workBearing.getAngle();
                        if (dist < minDistance) {
                            closestSegment = Math.max(0, j - 1);
                            if (j == 0) {
                                minAngle    = angle;
                                minDistance = dist;
                            } else {
                                minAngle    = lastAngle;
                                minDistance = lastDistance;
                            }
                        }
                        lastDistance = dist;
                        lastAngle    = angle;
                    }
                    segmentAngle = angles[closestSegment];
                    len          = lenToPoint[closestSegment];
                }
                double distToSegment =
                    minDistance
                    * Math.sin(Math.toRadians(Math.abs(minAngle
                        - segmentAngle)));
                if (Math.abs(distToSegment) < maxDataDistance) {
                    double distAlongSegment =
                        minDistance
                        * Math.cos(Math.toRadians(Math.abs(minAngle
                            - segmentAngle)));
                    x[i] = (float) (-1.0
                                    + 2.0
                                      * ((len + distAlongSegment)
                                         / totalLength));
                    y[i] = alt[i];
                    z[i] = DEFAULT_Z;
                } else {
                    x[i] = Float.NaN;
                    y[i] = Float.NaN;
                    z[i] = Float.NaN;
                }

            }

            if (numpoints > CACHE_THRESHOLD) {
                while (cacheInfos.size() > CACHE_SIZE) {
                    cacheInfos.remove(0);
                }
                cacheInfos.add(new CacheInfo(latlonalt, xyz, linePoints));
            }
            call2("toReference(f)", numpoints);
            return xyz;
        }


        /**
         * Transform latitude/longitude/altitude value to XYZ
         *
         * @param  latlonalt   array of latitude, longitude, altitude values
         *
         * @return array of display xyz values.
         *
         * @throws VisADException  can't create the necessary VisAD object
         */
        public double[][] toReference(double[][] latlonalt)
                throws VisADException {

            if ((latlonalt == null) || (latlonalt[0].length < 1)) {
                return latlonalt;
            }
            int numpoints = latlonalt[0].length;
            checkForNewLine();
            float[][]  linePoints  = transect.getSamples(false);
            Bearing    workBearing = new Bearing();
            double[][] xyz         = new double[3][numpoints];
            call1("toReference(d)", numpoints);
            double[] x               = xyz[0];
            double[] y               = xyz[1];
            double[] z               = xyz[2];
            double[] lat             = latlonalt[0];
            double[] lon             = latlonalt[1];
            double[] alt             = latlonalt[2];

            double   minDistance     = Double.MAX_VALUE;
            double   minAngle        = 0.0;
            double   lastDistance    = 0.0;
            double   lastAngle       = 0.0;
            double   segmentAngle    = angles[0];
            double   len             = lenToPoint[0];
            double   maxDataDistance = Double.MAX_VALUE;
            try {
                maxDataDistance =
                    getMaxDataDistance().getValue(CommonUnits.KILOMETER);
                if (Double.isNaN(maxDataDistance)) {
                    maxDataDistance = Double.MAX_VALUE;
                }
            } catch (Exception e) {
                maxDataDistance = Double.MAX_VALUE;
            }
            for (int i = 0; i < numpoints; i++) {
                if (numLinePoints == 2) {
                    workBearing = Bearing.calculateBearing(linePoints[0][0],
                            linePoints[1][0], lat[i], lon[i], workBearing);

                    minDistance = workBearing.getDistance();
                    minAngle    = workBearing.getAngle();
                } else {  // more than 2 points
                    int closestSegment = 0;
                    for (int j = 0; j < numLinePoints; j++) {
                        workBearing =
                            Bearing.calculateBearing(linePoints[0][j],
                                linePoints[1][j], lat[i], lon[i],
                                workBearing);

                        double dist  = workBearing.getDistance();
                        double angle = workBearing.getAngle();
                        if (dist < minDistance) {
                            closestSegment = Math.max(0, j - 1);
                            if (j == 0) {
                                minAngle    = angle;
                                minDistance = dist;
                            } else {
                                minAngle    = lastAngle;
                                minDistance = lastDistance;
                            }
                        }
                        lastDistance = dist;
                        lastAngle    = angle;
                    }
                    segmentAngle = angles[closestSegment];
                    len          = lenToPoint[closestSegment];
                }
                double distToSegment =
                    minDistance
                    * Math.sin(Math.toRadians(Math.abs(minAngle
                        - segmentAngle)));
                if (Math.abs(distToSegment) < maxDataDistance) {
                    double distAlongSegment =
                        minDistance
                        * Math.cos(Math.toRadians(Math.abs(minAngle
                            - segmentAngle)));
                    x[i] = (-1.0
                            + 2.0 * ((len + distAlongSegment) / totalLength));
                    y[i] = alt[i];
                    z[i] = DEFAULT_Z;
                } else {
                    x[i] = Double.NaN;
                    y[i] = Double.NaN;
                    z[i] = Double.NaN;
                }
            }
            call2("toReference(d)", numpoints);
            return xyz;
        }

        /**
         * Transform display XYZ values to latitude/longitude/altitude
         *
         * @param  xyz  array of Display.DisplaySpatialCartesianTuple XYZ values
         * @return array of display lat/lon/alt values.
         *
         * @throws VisADException  can't create the necessary VisAD object
         */
        public double[][] fromReference(double[][] xyz)
                throws VisADException {
            checkForNewLine();
            int        numpoints  = xyz[0].length;
            double[][] latlonalt  = new double[3][numpoints];
            float[][]  linePoints = transect.getSamples(false);
            call1("fromReference(d)", numpoints);
            for (int i = 0; i < numpoints; i++) {
                // dist from origin to point
                double dist = ((xyz[0][i] + 1) / 2) * totalLength;
                // find which segment this is in
                int segment = 0;
                if (dist > totalLength) {
                    segment = numSegments - 1;  // last segment
                } else if (dist > 0) {
                    for (int j = 1; j < lenToPoint.length; j++) {
                        if (lenToPoint[j] > dist) {
                            segment = j - 1;
                            break;
                        }
                    }
                }
                double distOnSegment = (dist - lenToPoint[segment])
                                       / distances[segment];
                float startLat = linePoints[0][segment];
                float startLon = linePoints[1][segment];
                float deltaLat = linePoints[0][segment + 1] - startLat;
                float deltaLon = linePoints[1][segment + 1] - startLon;
                latlonalt[0][i] = startLat + (distOnSegment * deltaLat);
                latlonalt[1][i] = startLon + (distOnSegment * deltaLon);
                latlonalt[2][i] = xyz[1][i];
            }
            call2("fromReference(d)", numpoints);
            return latlonalt;
        }

        /**
         * Transform display XYZ values to latitude/longitude/altitude
         *
         * @param  xyz  array of Display.DisplaySpatialCartesianTuple XYZ values
         * @return array of display lat/lon/alt values.
         *
         * @throws VisADException  can't create the necessary VisAD object
         */
        public float[][] fromReference(float[][] xyz) throws VisADException {
            checkForNewLine();
            int       numpoints  = xyz[0].length;
            float[][] latlonalt  = new float[3][numpoints];
            float[][] linePoints = transect.getSamples(false);
            call1("fromReference(f)", numpoints);
            for (int i = 0; i < numpoints; i++) {
                // dist from origin to point
                double dist = ((xyz[0][i] + 1) / 2) * totalLength;
                // find which segment this is in
                int segment = 0;
                if (dist > totalLength) {
                    segment = numSegments - 1;  // last segment
                } else if (dist > 0) {
                    for (int j = 1; j < lenToPoint.length; j++) {
                        if (lenToPoint[j] > dist) {
                            segment = j - 1;
                            break;
                        }
                    }
                }
                float distOnSegment = (float) ((dist - lenToPoint[segment])
                                          / distances[segment]);
                float startLat = linePoints[0][segment];
                float startLon = linePoints[1][segment];
                float deltaLat = linePoints[0][segment + 1] - startLat;
                float deltaLon = linePoints[1][segment + 1] - startLon;
                latlonalt[0][i] = startLat + (distOnSegment * deltaLat);
                latlonalt[1][i] = startLon + (distOnSegment * deltaLon);
                latlonalt[2][i] = xyz[1][i];
            }
            call2("fromReference(f)", numpoints);
            return latlonalt;
        }

        /**
         * Check to see if we have new transect
         *
         * @throws VisADException problem creating a new set
         */
        private void checkForNewLine() throws VisADException {
            Gridded2DSet llr = ensureLatLon(getTransect());
            if ( !llr.equals(transect)) {
                transect = llr;
                precalculate();
            }
        }

        /**
         * debug
         *
         * @param msg debug
         * @param numpoints debug
         */
        void call1(String msg, int numpoints) {
            if (numpoints > 10000) {
                //              Misc.printStack(msg,5,null);
                Trace.call1("TransectCS." + msg, " numpoints = " + numpoints);
            }
        }

        /**
         * debug
         *
         * @param msg debug
         * @param numpoints debug
         */
        void call2(String msg, int numpoints) {
            if (numpoints > 10000) {
                Trace.call2("TransectCS." + msg);
            }
        }

        /**
         * See if this is equal to the object in question.
         *
         * @param o  object in question.
         *
         * @return      true if they are equal. The two objects are equal if
         *              their transect are equal.
         */
        public boolean equals(Object o) {
            if ( !(o instanceof TransectCoordinateSystem)) {
                return false;
            }
            TransectCoordinateSystem cs = (TransectCoordinateSystem) o;
            return cs.transect.equals(this.transect);
        }

        /**
         * Precalculate the variables for the computations
         *
         * @throws VisADException  problem getting the values from the set
         */
        private void precalculate() throws VisADException {
            float[][] linePoints = transect.getSamples(false);
            numLinePoints = linePoints[0].length;
            numSegments   = numLinePoints - 1;
            lenToPoint    = new double[numLinePoints];
            distances     = new double[numSegments];
            angles        = new double[numSegments];
            Bearing lineBearing = new Bearing();
            lenToPoint[0] = 0;
            LatLonPointImpl startWorkPoint = new LatLonPointImpl();
            LatLonPointImpl endWorkPoint   = new LatLonPointImpl();
            for (int i = 1; i < numLinePoints; i++) {
                startWorkPoint.set(linePoints[0][i - 1],
                                   linePoints[1][i - 1]);
                endWorkPoint.set(linePoints[0][i], linePoints[1][i]);
                lineBearing = Bearing.calculateBearing(startWorkPoint,
                        endWorkPoint, lineBearing);
                distances[i - 1] = lineBearing.getDistance();
                angles[i - 1]    = lineBearing.getAngle();
                lenToPoint[i]    = lenToPoint[i - 1] + distances[i - 1];
            }
            totalLength = lenToPoint[numLinePoints - 1];
        }
    }

    /**
     * test by running java ucar.unidata.view.geoloc.NavigatedDisplay
     *
     * @param args   not used
     *
     * @throws Exception  problem creating the display
     */
    public static void main(String[] args) throws Exception {

        JFrame frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        float[][] points = new float[][] {
            { 39, 38, 41 }, { -110, -107, -105 }
        };
        Gridded2DSet line =
            new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple, points,
                             points[0].length, null, null, null, false);

        TransectDisplay navDisplay = new TransectDisplay(line);
        //navDisplay.setBackground(Color.white);
        //navDisplay.setForeground(Color.black);
        navDisplay.setCursorStringOn(true);
        navDisplay.setScalesVisible(true);
        navDisplay.setGridLinesVisible(true);
        //navDisplay.enableClipping(true);
        navDisplay.setBoxVisible(false);
        navDisplay.setHorizontalRangeUnit(CommonUnits.NAUTICAL_MILE);
        navDisplay.setVerticalRangeUnit(CommonUnits.FOOT);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(navDisplay.getComponent(), BorderLayout.CENTER);
        panel.add(new NavigatedDisplayCursorReadout(navDisplay),
                  BorderLayout.SOUTH);
        navDisplay.draw();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        //navDisplay.getDisplay().getGraphicsModeControl().setScaleEnable(true);
        frame.pack();
        frame.setVisible(true);
        EarthLocation el = new EarthLocationTuple(44.254, -121.150, 938);
        System.out.println("Earth location " + el + " translates to:");
        Misc.printArray("xyz", navDisplay.getSpatialCoordinates(el, null));

    }

    /**
     * Specialized renderer for this display.
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.41 $
     */
    private static class TransectDisplayRenderer extends DefaultDisplayRendererJ3D {

        /**
         * Default ctor
         */
        public TransectDisplayRenderer() {
            super();
            //setScreenBasedScaleFactor(6.0);
        }

        /**
         * Override to make this a 2D display
         *
         * @return true
         */
        public boolean getMode2D() {
            return true;
        }
    }
}

