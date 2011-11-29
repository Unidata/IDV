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



package ucar.unidata.view.geoloc;

//~--- non-JDK imports --------------------------------------------------------

import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Trace;

import ucar.visad.GeoUtils;
import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.display.MapLines;
import ucar.visad.display.ScalarMapSet;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.GeopotentialAltitude;

import visad.AxisScale;
import visad.CachingCoordinateSystem;
import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRealType;
import visad.DisplayTupleType;
import visad.Gridded2DSet;
import visad.InverseLinearScaledCS;
import visad.KeyboardBehavior;
import visad.MouseBehavior;
import visad.ProjectionControl;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.ScalarType;
import visad.SetType;
import visad.Unit;
import visad.UnitException;
import visad.VisADException;
import visad.VisADRay;

import visad.data.mcidas.AREACoordinateSystem;
import visad.data.mcidas.BaseMapAdapter;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.net.URL;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

/**
 * Provides a navigated VisAD DisplayImpl for displaying data.
 * The Projection or MapProjection provides the transformation from
 * lat/lon space to xy space.  There are three modes that can be used
 * with this display - MODE_3D (Java 3D), MODE_2Din3D (2D in Java 3D),
 * MODE_2D (Java 2D).  Performance is better in Java 3D modes. In the 3D
 * mode, RealType.Altitude is mapped to the display Z axis.<P>
 * Any displayable data must be able to map to RealType.Latitude,
 * RealType.Longitude and/or RealType.Altitude.<P>
 *
 * @author Don Murray
 */
public abstract class MapProjectionDisplay extends NavigatedDisplay {

    /**
     * The name of the bearing from center property.
     */
    public static final String CURSOR_BEARING = "cursorBearing";

    /**
     * The name of the range from center property.
     */
    public static final String CURSOR_RANGE = "cursorRange";

    /** instance counter */
    private static int instance = 0;

    /** instance locking mutex */
    private static Object INSTANCE_MUTEX = new Object();

    /** logging category */
    private static LogUtil.LogCategory log_ = LogUtil.getLogInstance(MapProjectionDisplay.class.getName());

    /**
     * flag for forcing 2D
     */
    public static boolean force2D = false;

    /**
     * The range from center RealType.
     */
    public static RealType CURSOR_RANGE_TYPE = RealType.getRealType("Cursor_Range", CommonUnits.KILOMETER);

    /**
     * The bearing from center RealType.
     */
    public static RealType CURSOR_BEARING_TYPE = RealType.getRealType("Cursor_Bearing", CommonUnit.degree);

    /** ScalarMapf for altitude -> displayAltitudeType */
    private ScalarMap altitudeMap = null;

    /** coordinate system units */
    private Unit[] csUnits = null;

    /** y axis scale */
    private AxisScale latScale = null;

    /** ScalarMapf for latitude -> displayLatitudeType */
    private ScalarMap latitudeMap = null;

    /** x axis scale */
    private AxisScale lonScale = null;

    /** ScalarMapf for longitude -> displayLongitudeType */
    private ScalarMap longitudeMap = null;

    /** default maximum vertical range value */
    private double maxVerticalRange = 16000;

    /** default minimum vertical range value */
    private double minVerticalRange = 0;

    /** vertical axis scale */
    private AxisScale verticalScale = null;

    /** ScalarMap to Display.XAxis */
    private ScalarMap xMap = null;

    /** ScalarMap to Display.YAxis */
    private ScalarMap yMap = null;

    /** ScalarMap to Display.ZAxis */
    private ScalarMap zMap = null;

    /** bearing class for bearing calculations */
    private Bearing workBearing = new Bearing();

    /** default vertical range unit */
    private Unit verticalRangeUnit = CommonUnit.meter;

    /** Vertical type */
    private RealType verticalParameter = RealType.Altitude;

    /** Set of vertical maps */
    private VerticalMapSet verticalMapSet = new VerticalMapSet();

    /** use 0-360 for longitude range */
    private boolean use360 = true;

    /** number format for axis labels */
    DecimalFormat labelFormat = new DecimalFormat("####0.0");

    /** flag for whether we've been initialized */
    private boolean init = false;

    /** cursor location for bearing calculations */
    private LatLonPointImpl cursorLLP = new LatLonPointImpl();

    /** centerpoint for bearing calculations */
    private LatLonPointImpl centerLLP = new LatLonPointImpl();

    /** flag for adjusting lons or not */
    private boolean adjustLons = false;

    /** The coordinate system for the display */
    private CoordinateSystem coordinateSystem;

    /**
     * The cursor altitude.
     * @serial
     */
    private volatile Real cursorBearing;

    /**
     * The cursor altitude.
     * @serial
     */
    private volatile Real cursorRange;

    /** The display's Altitude DisplayRealType */
    private DisplayRealType displayAltitudeType;

    /** The display's Latitude DisplayRealType */
    private DisplayRealType displayLatitudeType;

    /** The display's Longitude DisplayRealType */
    private DisplayRealType displayLongitudeType;

    /** the display tuple type */
    private DisplayTupleType displayTupleType;

    /** The MapProjection */
    private MapProjection mapProjection;

    /**
     * Constructs an instance with the specified MapProjection
     */
    protected MapProjectionDisplay() {}

    /**
     * Constructs an instance with the specified MapProjection
     * CoordinateSystem and display.
     *
     * @param projection   map projection CS
     * @param display   display to use
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    protected MapProjectionDisplay(MapProjection projection, DisplayImpl display)
            throws VisADException, RemoteException {
        super(display);

        // mapProjection = projection;
        // coordinateSystem = makeCoordinateSystem(projection);
        setMapProjection(projection);
        initializeClass();
    }

    /**
     * Initializes an instance with the specified MapProjection
     * CoordinateSystem and display.
     *
     * @param projection   map projection CS
     * @param display   display to use
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    protected void init(MapProjection projection, DisplayImpl display) throws VisADException, RemoteException {
        super.init(display);
        setMapProjection(projection);
        initializeClass();
    }

    /**
     * Set up the display.  Any additional work should be done in
     * a subclass's intializeClass() method, which should call
     * super.initializeClass() first.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void initializeClass() throws VisADException, RemoteException {
        super.initializeClass();
        setDisplayTypes();
    }

    /**
     * Get an instance of a MapProjectionDisplay using the mode specified
     * and the default projection.
     *
     * @param mode  mode to use
     *
     * @return a MapProjectionDisplay
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    public static MapProjectionDisplay getInstance(int mode) throws VisADException, RemoteException {
        return getInstance(null, mode);
    }

    /**
     * Get an instance of a MapProjectionDisplay using the mode specified
     * and the MapProjection.
     *
     * @param mode  mode to use
     * @param p  initial MapProjection for display
     *
     * @return a MapProjection display of the correct mode and projection
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    public static MapProjectionDisplay getInstance(MapProjection p, int mode) throws VisADException, RemoteException {
        return getInstance(p, mode, false, null);
    }

    /**
     * Get an instance of a MapProjectionDisplay using the mode specified
     * and the MapProjection.
     *
     * @param p       map projection
     * @param mode    mode
     * @param offscreen true if offscreen
     * @param dimension dimension of display
     *
     * @return the instance
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   problem creating the display or some component
     */
    public static MapProjectionDisplay getInstance(MapProjection p, int mode, boolean offscreen, Dimension dimension)
            throws VisADException, RemoteException {
        return getInstance(p, mode, offscreen, dimension, null);
    }

    /**
     * Get an instance of a MapProjectionDisplay using the mode specified
     * and the MapProjection.
     *
     * @param p       map projection
     * @param mode    mode
     * @param offscreen true if offscreen
     * @param dimension dimension of display
     * @param screen    screen to display it on
     *
     * @return the instance
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   problem creating the display or some component
     */
    public static MapProjectionDisplay getInstance(MapProjection p, int mode, boolean offscreen, Dimension dimension,
            GraphicsDevice screen)
            throws VisADException, RemoteException {
        if (p == null) {
            Trace.call1("MapProjectionDisplay.getInstance:makeProjection");
            p = makeDefaultMapProjection();
            Trace.call2("MapProjectionDisplay.getInstance:makeProjection");
        }

        if (((mode == MODE_3D) || (mode == MODE_2Din3D)) &&!force2D) {
            Trace.call1("MapProjectionDisplay.getInstance:new MapProjectionDisplayJ3D");

            MapProjectionDisplay mpd = new MapProjectionDisplayJ3D(p, mode, offscreen, dimension, screen);

            Trace.call2("MapProjectionDisplay.getInstance:new MapProjectionDisplayJ3D");

            return mpd;
        } else {
            return new MapProjectionDisplayJ2D(p);
        }
    }

    /**
     * Destroy this class
     */
    public void destroy() {
        super.destroy();
    }

    // private List keyboardBehaviors;

    /**
     * Add a KeyboardBehavior to this class
     *
     * @param behavior    behavior to add
     */
    public abstract void addKeyboardBehavior(KeyboardBehavior behavior);

    /**
     * Set the lat/lon scales
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    private void makeLatLonScales() throws VisADException, RemoteException {

        // TODO: implement something for the xy axes
        setDisplayInactive();

        double[] xRange = xMap.getRange();
        double[] yRange = yMap.getRange();
        double[] zRange = null;

        if (zMap != null) {
            zRange = zMap.getRange();
        } else {
            zRange = new double[] { 0, 0 };
        }

        if (latScale != null) {
            latScale.setVisible(false);

            EarthLocation ll = getEarthLocation(new double[] { xRange[0], yRange[0], zRange[0] });
            EarthLocation ur = getEarthLocation(new double[] { xRange[0], yRange[1], zRange[0] });

            updateLatLonScale(latScale, getLatLonScaleInfo().ordinateLabel, zRange, ll, ur, true);
        }

        if (lonScale != null) {
            lonScale.setVisible(false);

            EarthLocation ll = getEarthLocation(new double[] { xRange[0], yRange[0], zRange[0] });
            EarthLocation lr = getEarthLocation(new double[] { xRange[1], yRange[0], zRange[0] });

            updateLatLonScale(lonScale, getLatLonScaleInfo().abscissaLabel, xRange, ll, lr, false);
        }

        setDisplayActive();
    }

    /**
     * Method to update lat lon scale
     *
     * @param scale    AxisScale to update
     * @param title    Title
     * @param maxmin   max/min limits of axis
     * @param bottom   value for lower limit
     * @param top      value for upper limit
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    private void updateLatLonScale(AxisScale scale, String title, double[] maxmin, EarthLocation left,
                                   EarthLocation right, boolean absicca)
            throws VisADException, RemoteException {
        double    bottom     = absicca
                               ? left.getLatitude().getValue()
                               : left.getLongitude().getValue();
        double    top        = absicca
                               ? right.getLatitude().getValue()
                               : right.getLongitude().getValue();
        Hashtable labelTable = new Hashtable();

        // Labeling extremities
        labelTable.put(new Double(maxmin[0]), labelFormat.format(bottom));
        labelTable.put(new Double(maxmin[1]), labelFormat.format(top));

//      for (double i = bottom; i < top; i += 10) {
//              RealTuple spatialCoordinates = getSpatialCoordinates(absicca ? new EarthLocationTuple(
//                              i, left.getLongitude().getValue(), 0) : new EarthLocationTuple(right.getLatitude().getValue(), i, 0));
//              double[] values = spatialCoordinates.getValues();
//      labelTable.put(new Double(values[absicca ? 1 : 0]), labelFormat.format(i));
//      } 
        double minorTickSpacing = Math.abs(maxmin[1] - maxmin[0]) / getLatLonScaleInfo().minorTickSpacing;
        double majorTickSpacing = Math.abs(maxmin[1] - maxmin[0]) / getLatLonScaleInfo().majorTickSpacing;

        for (double i = -1; i < 1; i += majorTickSpacing) {
            EarthLocation el = absicca
                               ? getEarthLocation(-1, i, -1)
                               : getEarthLocation(i, -1, -1);

            labelTable.put(i, labelFormat.format(absicca
                    ? el.getLatitude().getValue()
                    : el.getLongitude().getValue()));

//          System.out.println(i + " " +  (absicca ? el.getLatitude().getValue() : el.getLongitude().getValue()));
        }

        scale.setSnapToBox(true);
        scale.setTitle(title);
        scale.setLabelTable(labelTable);
        scale.setTicksVisible(true);
        scale.setMajorTickSpacing(minorTickSpacing);
        scale.setMinorTickSpacing(majorTickSpacing);
    }

    /**
     * Method to update the properties of an AxisScale
     *
     * @param scale    AxisScale to update
     * @param title    Title
     * @param maxmin   max/min limits of axis
     * @param bottom   value for lower limit
     * @param top      value for upper limit
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    private void updateVertScale(AxisScale scale, String title, double[] maxmin, double bottom, double top)
            throws VisADException, RemoteException {
        scale.setSnapToBox(true);
        scale.setTitle(title);

        Hashtable labelTable = new Hashtable();

        labelTable.put(new Double(maxmin[0]), labelFormat.format(bottom));
        labelTable.put(new Double(maxmin[1]), labelFormat.format(top));
        scale.setLabelTable(labelTable);
        scale.setTickBase(maxmin[0]);
        scale.setMajorTickSpacing(Math.abs(maxmin[1] - maxmin[0]));
        scale.setMinorTickSpacing(Math.abs(maxmin[1] - maxmin[0]));
    }

    /**
     * Set the vertical axis scale
     *
     * @throws VisADException   problem creating some VisAD object
     * @throws RemoteException   problem creating remote object
     */
    private void makeVerticalScale() throws VisADException, RemoteException {
        if (verticalScale == null) {
            return;
        }

        setDisplayInactive();

        double[] zRange = zMap.getRange();
        String   title  = verticalParameter.getName() + "(" + verticalRangeUnit.getIdentifier() + ")";

        updateVertScale(verticalScale, title, zRange, minVerticalRange, maxVerticalRange);

        /*
         * verticalScale.setSnapToBox(true);
         * verticalScale.setTitle(
         * Hashtable labelTable = new Hashtable();
         * labelTable.put(new Double(zRange[0]),
         *              labelFormat.format(minVerticalRange));
         * labelTable.put(new Double(zRange[1]),
         *              labelFormat.format(maxVerticalRange));
         * verticalScale.setLabelTable(labelTable);
         * verticalScale.setTickBase(zRange[0]);
         * verticalScale.setMajorTickSpacing(Math.abs(zRange[1]-zRange[0]));
         * verticalScale.setMinorTickSpacing(Math.abs(zRange[1]-zRange[0]));
         */
        setDisplayActive();
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
    private void setSpatialScalarMaps() throws VisADException, RemoteException {
        setDisplayInactive();

        ScalarMapSet mapSet = new ScalarMapSet();

        if (latitudeMap != null) {
            removeScalarMap(latitudeMap);
        }

        latitudeMap = new ScalarMap(RealType.Latitude, displayLatitudeType);
        mapSet.add(latitudeMap);
        latitudeMap.setRangeByUnits();
        latitudeMap.setScaleEnable(true);

        if (longitudeMap != null) {
            removeScalarMap(longitudeMap);
        }

        longitudeMap = new ScalarMap(RealType.Longitude, displayLongitudeType);
        mapSet.add(longitudeMap);
        longitudeMap.setRangeByUnits();
        longitudeMap.setScaleEnable(true);

        if (getDisplayMode() == MODE_3D) {
            ScalarMapSet newVertMaps = new ScalarMapSet();

            if (verticalMapSet.size() > 0) {
                for (Iterator iter = verticalMapSet.iterator(); iter.hasNext(); ) {
                    ScalarType r      = ((ScalarMap) iter.next()).getScalar();
                    ScalarMap  newMap = new ScalarMap(r, displayAltitudeType);

                    newMap.setScaleEnable(true);

                    if (r.equals(RealType.Altitude)) {
                        altitudeMap = newMap;
                    }

                    newVertMaps.add(newMap);
                }
            } else {    // add Altitude at least
                altitudeMap = new ScalarMap(RealType.Altitude, displayAltitudeType);
                altitudeMap.setScaleEnable(true);
                newVertMaps.add(altitudeMap);
            }

            removeScalarMaps(verticalMapSet);
            verticalMapSet.clear();
            verticalMapSet.add(newVertMaps);
            setVerticalRange(minVerticalRange, maxVerticalRange);
            setVerticalRangeUnit(verticalRangeUnit);
            mapSet.add(verticalMapSet);
        }

        if (!init) {
            xMap = new ScalarMap(RealType.XAxis, Display.XAxis);
            xMap.setRange(-1.0, 1.0);
            mapSet.add(xMap);
            xMap.setScaleEnable(true);
            lonScale = xMap.getAxisScale();
            yMap     = new ScalarMap(RealType.YAxis, Display.YAxis);
            yMap.setRange(-1.0, 1.0);
            mapSet.add(yMap);
            yMap.setScaleEnable(true);
            latScale = yMap.getAxisScale();

            if (getDisplayMode() == MODE_3D) {
                zMap = new ScalarMap(RealType.ZAxis, Display.ZAxis);
                zMap.setRange(-1.0, 1.0);
                mapSet.add(zMap);
                zMap.setScaleEnable(true);
                verticalScale = zMap.getAxisScale();
            }

            init = true;
        }

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
    public void addVerticalMap(RealType newVertType) throws VisADException, RemoteException {
        if (getDisplayMode() == MODE_3D) {
            Unit u = newVertType.getDefaultUnit();

            if (!(Unit.canConvert(u, CommonUnit.meter)
                    || Unit.canConvert(u, GeopotentialAltitude.getGeopotentialMeter()))) {
                throw new VisADException("Unable to handle units of " + newVertType);
            }

            ScalarMap newMap = new ScalarMap(newVertType, getDisplayAltitudeType());

            setVerticalMapUnit(newMap, verticalRangeUnit);
            newMap.setRange(minVerticalRange, maxVerticalRange);
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
    public void removeVerticalMap(RealType vertType) throws VisADException, RemoteException {
        if (getDisplayMode() == MODE_3D) {
            ScalarMapSet sms = new ScalarMapSet();

            for (Iterator iter = verticalMapSet.iterator(); iter.hasNext(); ) {
                ScalarMap s = (ScalarMap) iter.next();

                if (((RealType) s.getScalar()).equals(vertType)) {
                    sms.add(s);
                }
            }

            if (!(sms.size() == 0)) {
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
    public void setVerticalRangeUnit(Unit newUnit) throws VisADException, RemoteException {
        super.setVerticalRangeUnit(newUnit);

        if ((newUnit != null) && Unit.canConvert(newUnit, CommonUnit.meter)) {
            verticalMapSet.setVerticalUnit(newUnit);
            verticalRangeUnit = newUnit;
        }

        makeVerticalScale();
    }

    /**
     * {@inheritDoc}
     */
    public void setLatLonScaleInfo(LatLonScaleInfo latLonScaleInfo) throws RemoteException, VisADException {
        super.setLatLonScaleInfo(latLonScaleInfo);
        makeLatLonScales();
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
    public void setVerticalRange(double min, double max) throws VisADException, RemoteException {
        super.setVerticalRange(min, max);
        verticalMapSet.setVerticalRange(min, max);
        minVerticalRange = min;
        maxVerticalRange = max;
        makeVerticalScale();
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
               : new double[] { minVerticalRange, maxVerticalRange };
    }

    /**
     * Sets the cursor range from center property.
     *
     * @param range          The cursor range from center.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void setCursorRange(Real range) throws VisADException, RemoteException {
        Real oldRange = cursorRange;

        cursorRange = range;
        firePropertyChange(CURSOR_RANGE, oldRange, cursorRange);
    }

    /**
     * Gets the cursor range from center property.
     *
     * @return                  The currently-selected range.  May be
     *                          <code>null</code>.
     */
    public Real getCursorRange() {
        return cursorRange;
    }

    /**
     * Sets the cursor bearing (degrees) from center property.
     * This implementation uses a great circle distance.
     *
     * @param bearing          The cursor bearing from center.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void setCursorBearing(Real bearing) throws VisADException, RemoteException {
        Real oldBearing = cursorBearing;

        cursorBearing = bearing;
        firePropertyChange(CURSOR_BEARING, oldBearing, cursorBearing);
    }

    /**
     * Gets the cursor bearing from center property.
     *
     * @return                  The currently-selected bearing.  May be
     *                          <code>null</code>.
     */
    public Real getCursorBearing() {
        return cursorBearing;
    }

    /**
     * Set the view for 3D.  The views are based on the original display
     * as follows:
     * <pre>
     *                        NORTH
     *                      _________
     *                    W |       | E
     *                    E |  TOP  | A
     *                    S |       | S
     *                    T |_______| T
     *                        SOUTH
     * </pre>
     * @param  view  one of the static view fields (NORTH_VIEW, SOUTH_VIEW, ..
     *               etc).
     */
    public void setView(int view) {
        if (getDisplayMode() != MODE_3D) {
            return;
        }
    }

    /**
     * Accessor method for the DisplayLatitudeType (i.e., what
     * RealType.Latitude is mapped to).
     *
     * @return  the DisplayRealType that RealType.Latitude is mapped to
     */
    public DisplayRealType getDisplayLatitudeType() {
        return displayLatitudeType;
    }

    /**
     * Accessor method for the DisplayLongitudeType (i.e., what
     * RealType.Longitude is mapped to).
     *
     * @return  the DisplayRealType that RealType.Longitude is mapped to
     */
    public DisplayRealType getDisplayLongitudeType() {
        return displayLongitudeType;
    }

    /**
     * Accessor method for the DisplayAltitudeType (i.e., what
     * RealType.Altitude is mapped to).
     *
     * @return  the DisplayRealType that RealType.Altitude is mapped to
     */
    public DisplayRealType getDisplayAltitudeType() {
        return displayAltitudeType;
    }

    /**
     * Accessor method for the vertical coordinate ScalarMap (i.e., what
     * getDisplayAltitudeType is mapped from).
     * @return  the ScalarMap that the vertical coordinate is mapped to
     */
    protected ScalarMap getAltitudeMap() {
        return altitudeMap;
    }

    /**
     * Define the map projection using a Projection interface
     *
     * @param  projection   Projection to use
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public void setMapProjection(ProjectionImpl projection) throws VisADException, RemoteException {
        setMapProjection(new ProjectionCoordinateSystem(projection));
    }

    /**
     * Define the map projection using a MapProjection type CoordinateSystem
     *
     * @param  mapProjection   map projection coordinate system
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public void setMapProjection(MapProjection mapProjection) throws VisADException, RemoteException {
        if (mapProjection.equals(this.mapProjection)) {
            return;
        }

        this.mapProjection = mapProjection;
        coordinateSystem   = makeCoordinateSystem(mapProjection);
        resetMapParameters();

        EarthLocation el = getEarthLocation(new double[] { 0, 0, 0 });

        centerLLP.set(el.getLatitude().getValue(CommonUnit.degree), el.getLongitude().getValue(CommonUnit.degree));
    }

    /**
     * Get the MapProjection that defines the xy mapping of this
     * MapProjectionDisplay.
     *
     * @return  MapProjection being used.
     */
    public MapProjection getMapProjection() {
        return mapProjection;
    }

    /**
     * Set the map area from the projection rectangle
     *
     * @param mapArea   map area as lat/lon lines
     *
     * @throws RemoteException  problem setting remote data
     * @throws VisADException   problem creating VisAD data object
     */
    public void setMapArea(ProjectionRect mapArea) throws VisADException, RemoteException {
        if (coordinateSystem == null) {
            throw new VisADException("Navigation hasn't been set yet");
        }

        // System.out.println("Map Area = " + mapArea);
        MapProjection project = ((MapProjection3DAdapter) coordinateSystem).getMapProjection();

        // get the corners in latlon coords
        ProjectionPoint ppMax = mapArea.getMaxPoint();
        ProjectionPoint ppMin = mapArea.getMinPoint();

        // System.out.println("ppMax:" + ppMax);
        // System.out.println("ppMin:" + ppMin);
        float[][] values = new float[2][2];

        values[0][0] = (float) ppMax.getY();
        values[0][1] = (float) ppMin.getY();
        values[1][0] = (float) ppMax.getX();
        values[1][1] = (float) ppMin.getX();

        // values       = project.toReference(values);
        Gridded2DSet region = new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple, values, 2);

        setMapRegion(region);
    }

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
    public void setMapRegion(Gridded2DSet region) throws VisADException, RemoteException {
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
                   || regionType.equals(RealTupleType.LatitudeLongitudeTuple)) {

            // transform to x/y
            int       latIndex = regionType.equals(RealTupleType.LatitudeLongitudeTuple)
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
            xyRegion  = new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple, values, 2);
        } else {
            throw new VisADException("Invalid domain for region " + regionType);
        }

        // System.out.println(xyRegion);
        // Okay, now we have our region, let's get cracking
        // First, let's figure out our component size.
        Dimension d = getComponent().getSize();

        // System.out.println("Component size = " + d);
        int componentCenterX = d.width / 2;
        int componentCenterY = d.height / 2;

        /*
         * System.out.println(
         * "Component Center point = " +
         * componentCenterX +","+componentCenterY);
         */

        // Now let's get the MouseBehavior so we can get some display coords
        MouseBehavior     behavior = getDisplay().getDisplayRenderer().getMouseBehavior();
        ProjectionControl proj     = getDisplay().getProjectionControl();
        double[]          aspect   = getDisplayAspect();

        // Misc.printArray("aspect", aspect);

        // We have to figure the component coordinates of the region.
        // To do this, we calculate the number of display units per pixel
        // in the x and y.  This logic comes from visad.MouseHelper.
        // Basically, we find out the current matrix, how much we should
        // scale, translate and rotate, and then apply the new matrix.
        double[] center_ray = behavior.findRay(componentCenterX, componentCenterY).position;

        // Misc.printArray("center_ray", center_ray);
        double[] center_ray_x = behavior.findRay(componentCenterX + 1, componentCenterY).position;

        // Misc.printArray("center_ray_x", center_ray_x);
        double[] center_ray_y = behavior.findRay(componentCenterX, componentCenterY + 1).position;

        // Misc.printArray("center_ray_y", center_ray_y);

        /*
         *  TODO:  test more to see if this makes a difference.  The
         *  rubber band box is actually at the Z=-1 position
         *  double[] center_ray = getRayPositionAtZ(behavior.findRay(componentCenterX,
         *  componentCenterY), -1);
         *  Misc.printArray("center_ray @ -1", center_ray);
         *  double[] center_ray_x = getRayPositionAtZ(behavior.findRay(componentCenterX + 1,
         *  componentCenterY), -1);
         *  Misc.printArray("center_ray_x @ -1", center_ray_x);
         *  double[] center_ray_y = getRayPositionAtZ(behavior.findRay(componentCenterX,
         *  componentCenterY + 1),-1);
         *  Misc.printArray("center_ray_y @ -1", center_ray_y);
         */
        double[] tstart = proj.getMatrix();

        // printMatrix("tstart", tstart);
        double[] rot   = new double[3];
        double[] scale = new double[3];
        double[] trans = new double[3];

        behavior.instance_unmake_matrix(rot, scale, trans, tstart);

        double stx = scale[0];
        double sty = scale[1];

        // System.out.println("stx = " + stx);
        // System.out.println("sty = " + sty);
        double[] trot = behavior.make_matrix(rot[0], rot[1], rot[2], scale[0], scale[1], scale[2],

        // scale[0],
        0.0, 0.0, 0.0);

        // printMatrix("trot", trot);

        // WLH 17 Aug 2000
        double[] xmat = behavior.make_translate(center_ray_x[0] - center_ray[0], center_ray_x[1] - center_ray[1],
                            center_ray_x[2] - center_ray[2]);

        // xmat = behavior.multiply_matrix(mult, xmat);
        double[] ymat = behavior.make_translate(center_ray_y[0] - center_ray[0], center_ray_y[1] - center_ray[1],
                            center_ray_y[2] - center_ray[2]);

        // ymat = behavior.multiply_matrix(mult, ymat);
        double[] xmatmul = behavior.multiply_matrix(trot, xmat);
        double[] ymatmul = behavior.multiply_matrix(trot, ymat);

        /*
         * printMatrix("xmat", xmat);
         * printMatrix("ymat", ymat);
         * printMatrix("xmatmul", xmatmul);
         * printMatrix("ymatmul", ymatmul);
         */
        behavior.instance_unmake_matrix(rot, scale, trans, xmatmul);

        double xmul = trans[0];

        behavior.instance_unmake_matrix(rot, scale, trans, ymatmul);

        double ymul = trans[1];

        // System.out.println("Multipliers = " + xmul + "," + ymul);

        // make sure that we don't divide by 0 (happens if display
        // component is not yet on screen
        if ((Math.abs(xmul) > 0) && (Math.abs(ymul) > 0)) {

            // Now we can get the box coordinates in component space
            float[] lows              = xyRegion.getLow();
            float[] highs             = xyRegion.getHi();
            float   boxCenterDisplayX = (highs[0] + lows[0]) / 2.0f;
            float   boxCenterDisplayY = (highs[1] + lows[1]) / 2.0f;

            /*
             * System.out.println(
             * "Box center point (XY) = " +
             * boxCenterDisplayX+","+boxCenterDisplayY);
             */

            // Check to see if the box is big enough (at least 5x5 pixels)
            // *** might want to ammend this to be a percentage of
            // component size ****
            int boxWidth  = (int) Math.abs((highs[0] - lows[0]) / xmul * stx);
            int boxHeight = (int) Math.abs((highs[1] - lows[1]) / ymul * sty);

            /*
             * System.out.println(
             * "Box size = " + boxWidth +"," + boxHeight);
             */
            if ((boxWidth > 5) && (boxHeight > 5)) {
                int boxCenterX = componentCenterX + (int) ((boxCenterDisplayX - center_ray[0]) / xmul);
                int boxCenterY = componentCenterY - (int) ((boxCenterDisplayY - center_ray[1]) / ymul);

                /*
                 * System.out.println(
                 * "Box Center point = " + boxCenterX +","+boxCenterY);
                 */
                double transx = (componentCenterX - boxCenterX) * xmul * stx;
                double transy = (componentCenterY - boxCenterY) * ymul * sty;

                /*
                 * System.out.println("transx = " + transx +
                 * " transy = " + transy);
                 */

                // Now calculate zoom factor
                double zoom = (boxWidth / boxHeight >= d.width / d.height)
                              ? d.getWidth() / boxWidth
                              : d.getHeight() / boxHeight;

                // zoom out if this is a bigger region than the component
                // System.out.println("zoom factor = " + zoom);

                translate(transx, -transy);
                zoom(zoom);
            }
        }
    }

    /**
     * Scale vertical values using the range of the vertical
     * scalar map.
     *
     * @param altValues  altitude map values
     *
     * @return z values  (may transform in place);
     */
    public float[] scaleVerticalValues(float[] altValues) {
        if (getAltitudeMap() == null) {
            return altValues;
        }

        return getAltitudeMap().scaleValues(altValues, false);
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
            displayLatitudeType  = Display.YAxis;
            displayLongitudeType = Display.XAxis;
            displayAltitudeType  = Display.ZAxis;
            displayTupleType     = Display.DisplaySpatialCartesianTuple;
        } else {
            int myInstance;

            synchronized (INSTANCE_MUTEX) {
                myInstance = instance++;
            }

            // We need to set the range on the longitude axis
            // to be equal to the range of the projection if the
            // X coordinate is approximately equal to Longitude.
            // For now, this is only LatLonProjections and TrivalMP's
            double        minLon    = -360;
            double        maxLon    = 360.;
            double        centerLon = 0;
            MapProjection mp        = ((MapProjection3DAdapter) coordinateSystem).getMapProjection();
            boolean       isLatLon  = false;

            adjustLons = true;

            // HACK, HACK, HACK, HACK
            if (mp instanceof ProjectionCoordinateSystem) {
                ProjectionImpl proj = ((ProjectionCoordinateSystem) mp).getProjection();

                if (proj instanceof LatLonProjection) {
                    Rectangle2D r2d2 = mp.getDefaultMapArea();

                    minLon    = r2d2.getX();
                    maxLon    = minLon + r2d2.getWidth();
                    centerLon = minLon + r2d2.getWidth() / 2;
                    isLatLon  = true;
                }
            } else if (mp instanceof TrivialMapProjection) {
                Rectangle2D r2d2 = mp.getDefaultMapArea();

                minLon    = r2d2.getX();
                maxLon    = minLon + r2d2.getWidth();
                centerLon = minLon + r2d2.getWidth() / 2;

                // isLatLon  = true;
                // TODO:  figure out this a little more.
            } else if (mp instanceof AREACoordinateSystem) {

                // minLon    = -180;
                // maxLon    = 180.;
                adjustLons = false;
            }

            // TODO:  figure out what we should be doing here.
            use360 = !((minLon >= -185) && (maxLon <= 185));

            if ((isLatLon &&!use360)                                             // lat/lon projections in +/-180 rang
                    ||!mp.isXYOrder()                                            // Vis5D
                    || ((minLon > -360) && (minLon < 0) && (maxLon > 180))) {    // AVN grids
                adjustLons = false;
            }

            /*
             * System.out.println("DisplayProjectionLon" + myInstance
             *                  + " has range of " + minLon + " to " + maxLon
             *                  + " with center at " + centerLon
             *                  + "; use360 = " + use360 + "; adjust lons = "
             *                  + adjustLons);
             */

            displayLatitudeType = new DisplayRealType("ProjectionLat" + myInstance, true, -90.0, 90.0, 0.0,
                    CommonUnit.degree);
            displayLongitudeType = new DisplayRealType("ProjectionLon" + myInstance, true, minLon, maxLon, centerLon,
                    CommonUnit.degree);

            double defaultZ = (getDisplayMode() != MODE_3D)
                              ? 0.0
                              : -1.0;

            displayAltitudeType = new DisplayRealType("ProjectionAlt" + myInstance, true, -1.0, 1.0, defaultZ, null);
            displayTupleType    = new DisplayTupleType(new DisplayRealType[] { displayLatitudeType,
                    displayLongitudeType, displayAltitudeType }, coordinateSystem);
        }

        setSpatialScalarMaps();
    }

    /**
     * Create the adapter coordinate system using a MapProjection
     *
     * @param mapProjection
     *
     * @return the adapted coordinate system
     *
     * @throws VisADException   null mapProjection or other VisAD problem
     */
    private CoordinateSystem makeCoordinateSystem(MapProjection mapProjection) throws VisADException {
        if (mapProjection == null) {
            throw new VisADException("MapProjection can't be null");
        }

        CoordinateSystem cs = new MapProjection3DAdapter(mapProjection);

        csUnits = cs.getCoordinateSystemUnits();

        return cs;
    }

    /**
     * Handles a change to the cursor position.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void cursorMoved() throws VisADException, RemoteException {
        double[] c = getDisplay().getDisplayRenderer().getCursor();

        updateLocation(getEarthLocation(getDisplay().getDisplayRenderer().getCursor()));
    }

    /**
     * Update lat/lon/alt properties with the EarthLocation.
     *
     * @param el  EarthLocation to use.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void updateLocation(EarthLocation el) throws VisADException, RemoteException {
        super.updateLocation(el);
        cursorLLP.set(el.getLatitude().getValue(CommonUnit.degree), el.getLongitude().getValue(CommonUnit.degree));
        Bearing.calculateBearing(centerLLP, cursorLLP, workBearing);
        setCursorRange(new Real(CURSOR_RANGE_TYPE, workBearing.getDistance()));
        setCursorBearing(new Real(CURSOR_BEARING_TYPE, workBearing.getAngle()));
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
    protected void pointerMoved(int x, int y) throws UnitException, VisADException, RemoteException {

        /*
         * Convert from (pixel, line) Java Component coordinates to (latitude,
         * longitude)
         */

        /*
         *  TODO: figure out why this won't work
         * updateLocation(getEarthLocation(getSpatialCoordinatesFromScreen(x,
         *      y)));
         */

        // TODO: java2d
        // if(true) return;
        VisADRay      ray = getRay(x, y);
        EarthLocation el  = getEarthLocation(new double[] { ray.position[0], ray.position[1], ray.position[2] });

        updateLocation(el);
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
    public EarthLocation getEarthLocation(double x, double y, double z, boolean setZToZeroIfOverhead) {
        EarthLocationTuple value = null;

        try {
            float[][] numbers = coordinateSystem.fromReference(new float[][] {
                new float[] { (float) (x) }, new float[] { (float) (y) }, new float[] { (float) (z) }
            });
            Real      lat     = new Real(RealType.Latitude, getScaledValue(latitudeMap, numbers[0][0]), csUnits[0]);
            Real      lon     = new Real(RealType.Longitude, getScaledValue(longitudeMap, numbers[1][0]), csUnits[1]);
            Real      alt     = null;

            if (getDisplayMode() == MODE_3D) {
                if (setZToZeroIfOverhead && Arrays.equals(getProjectionMatrix(), getSavedProjectionMatrix())

                /*
                 * && (alt
                 *           .getValue(
                 *               getVerticalRangeUnit()) != altitudeMap
                 *                   .getRange()[0])
                 */
                ) {
                    alt = new Real(RealType.Altitude, altitudeMap.getRange()[0], getVerticalRangeUnit());
                } else {
                    alt = new Real(RealType.Altitude, getScaledValue(altitudeMap, numbers[2][0]));
                }
            } else {
                alt = new Real(RealType.Altitude, 0);
            }

            value = new EarthLocationTuple(lat, lon, alt);
        } catch (VisADException e) {
            e.printStackTrace();
        }    // can't happen
                catch (RemoteException e) {
            e.printStackTrace();
        }    // can't happen

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
            throw new NullPointerException("MapProjectionDisplay.getSpatialCoorindate():  "
                                           + "null input EarthLocation");
        }

        RealTuple spatialLoc = null;

        try {
            double[] xyz = getSpatialCoordinates(el, null);

            spatialLoc = new RealTuple(RealTupleType.SpatialCartesian3DTuple, xyz);
        } catch (VisADException e) {
            e.printStackTrace();
        }    // can't happen
                catch (RemoteException e) {
            e.printStackTrace();
        }    // can't happen

        return spatialLoc;
    }

    /**
     * Returns the spatial (XYZ) coordinates of the particular EarthLocation
     *
     * @param el    earth location to transform
     * @param xyz    The in value to set. May be null.
     * @param altitude altitude value
     *
     * @return  xyz array
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public double[] getSpatialCoordinates(EarthLocation el, double[] xyz, double altitude)
            throws VisADException, RemoteException {
        float[] altValues;

        if ((altitudeMap != null) && (el.getAltitude() != null) &&!(Double.isNaN(altitude))) {
            altValues = altitudeMap.scaleValues(new double[] { altitude });
        } else {
            altValues = new float[] { 0f };
        }

        float[][] temp = coordinateSystem.toReference(new float[][] {
            latitudeMap.scaleValues(new double[] { el.getLatitude().getValue(CommonUnit.degree) }),
            longitudeMap.scaleValues(new double[] { el.getLongitude().getValue(CommonUnit.degree) }), altValues
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
     * Method called to reset all the map parameters after a change.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void resetMapParameters() throws VisADException, RemoteException {
        setDisplayInactive();
        setDisplayTypes();
        resetProjection();    // make it the right size
        setAspect();
        makeLatLonScales();
        setDisplayActive();
    }

    /**
     * Set the aspect for the display.
     */
    private void setAspect() {
        Rectangle2D mapArea  = mapProjection.getDefaultMapArea();
        double      ratio    = mapArea.getWidth() / mapArea.getHeight();
        double[]    myaspect = getDisplayAspect();

        try {
            if (ratio == 1.0) {    // height == width
                setDisplayAspect((getDisplayMode() != MODE_2D)
                                 ? new double[] { 1.0, 1.0, myaspect[2] }
                                 : new double[] { 1.0, 1.0 });

                /*
                 *   guess this doesn't matter, just use the other
                 * } else if (ratio < 1) {  // height > width
                 *  setDisplayAspect(
                 *      (getDisplayMode() != MODE_2D)
                 *         ? new double[] { 1.0, ratio, myaspect[2] }
                 *         : new double[] { 1.0, ratio });
                 */
            } else {               // width > height
                setDisplayAspect((getDisplayMode() != MODE_2D)
                                 ? new double[] { ratio, 1.0, myaspect[2] }
                                 : new double[] { ratio, 1.0 });
            }

            // Misc.printArray("aspect", getDisplayAspect());
        } catch (Exception excp) {
            System.out.println("MapProjectionDisplay.setDisplayAspect() got exception: " + excp);
        }
    }

    /**
     * Make the default projection.
     *
     * @return Default projectcion
     *
     * @throws VisADException  couldn't create MapProjection
     */
    protected static MapProjection makeDefaultMapProjection() throws VisADException {
        return new ProjectionCoordinateSystem(new LatLonProjection("Default Projection",

        // Use this to make the aspect ratio correct
        new ProjectionRect(-180., -180., 180., 180.)));
    }

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
     * test by running java ucar.unidata.view.geoloc.MapProjectionDisplay
     *
     * @param args  include an argument for a 3D display
     *
     * @throws Exception  problem  creating the display
     */
    public static void main(String[] args) throws Exception {
        JFrame frame = new JFrame();

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        final MapProjectionDisplay navDisplay = ((args.length > 0) && visad.util.Util.canDoJava3D())
                ? MapProjectionDisplay.getInstance(NavigatedDisplay.MODE_3D)
                : (visad.util.Util.canDoJava3D() == true)
                  ? MapProjectionDisplay.getInstance(NavigatedDisplay.MODE_2Din3D)
                  : MapProjectionDisplay.getInstance(NavigatedDisplay.MODE_2D);

        /*
         * double[]aspect = { 1.0, 1.0, 0.4 };
         * navDisplay.setDisplayAspect((navDisplay.getDisplayMode() == NavigatedDisplay.MODE_2D)
         *                                       ? new double[]{ 1.0, 1.0 }
         *                                       : aspect);
         */
        DisplayImpl display = (DisplayImpl) navDisplay.getDisplay();

        navDisplay.setBackground(Color.white);
        navDisplay.setForeground(Color.black);

        // navDisplay.setCursorStringOn(true);
        MapLines mapLines  = new MapLines("maplines");
        URL      mapSource =

        // new URL("ftp://www.ssec.wisc.edu/pub/visad-2.0/OUTLSUPW");
        navDisplay.getClass().getResource("/auxdata/maps/OUTLSUPW");

        try {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);

            mapLines.setMapLines(mapAdapter.getData());
            mapLines.setColor(java.awt.Color.black);
            navDisplay.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println("Can't open map file " + mapSource);
            System.out.println(excp);
        }

        JPanel  panel  = new JPanel(new GridLayout(1, 0));
        JButton pushme = new JButton("Map Projection Manager");

        panel.add(pushme);
        frame.getContentPane().add(panel, BorderLayout.NORTH);

        ViewpointControl vpc = new ViewpointControl(navDisplay);

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(navDisplay.getComponent(), BorderLayout.CENTER);
        panel.add((navDisplay.getDisplayMode() == navDisplay.MODE_3D)
                  ? (Component) ucar.unidata.util.GuiUtils.topCenterBottom(vpc.getToolBar(JToolBar.VERTICAL),
                  new NavigatedDisplayToolBar(navDisplay, JToolBar.VERTICAL), GuiUtils.filler())
                  : (Component) new NavigatedDisplayToolBar(navDisplay, JToolBar.VERTICAL), BorderLayout.WEST);

        JPanel readout = new JPanel();

        readout.add(new NavigatedDisplayCursorReadout(navDisplay));
        readout.add(new RangeAndBearingReadout(navDisplay));
        panel.add(readout, BorderLayout.SOUTH);
        navDisplay.draw();
        frame.getContentPane().add(panel, BorderLayout.CENTER);

        if (navDisplay.getDisplayMode() == navDisplay.MODE_3D) {
            JMenuBar mb = new JMenuBar();

            mb.add(vpc.getMenu());
            frame.setJMenuBar(mb);
        }

        System.out.println("Using rectilinear projection");

        final ProjectionManager pm = new ProjectionManager();

        pm.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("ProjectionImpl")) {
                    try {
                        navDisplay.setMapProjection((ProjectionImpl) e.getNewValue());
                    } catch (Exception exp) {
                        System.out.println(exp);
                    }
                }
            }
        });
        pushme.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                pm.show();
            }
        });

        /*
         * EarthLocationTuple elt = new EarthLocationTuple(40,-105, 8000);
         * SelectorPoint sp = new SelectorPoint("foo", elt);
         * sp.setFixed(false,false,true);
         * sp.setColor(Color.black);
         * navDisplay.addDisplayable(sp);
         */
        navDisplay.getDisplay().getGraphicsModeControl().setScaleEnable(true);
        frame.pack();
        frame.setVisible(true);

        /*
         * if (navDisplay.getDisplayMode() != navDisplay.MODE_2D) {
         *   final CurveDrawer cd =
         *       //new CurveDrawer(RealType.XAxis, RealType.YAxis,
         *       new CurveDrawer(RealType.Latitude, RealType.Longitude,
         *                       InputEvent.SHIFT_MASK);
         *   cd.addAction(new ActionImpl("Curve Drawer") {
         *     int numSets = 0;
         *     public void doAction ()
         *       throws VisADException, RemoteException {
         *         UnionSet curves = (UnionSet) cd.getData();
         *         if (curves != null) {
         *           int num = curves.getSets().length;
         *           if (num != numSets) {
         *               numSets = num;
         *               System.out.println("Data has type " +
         *                                  curves.getType() +
         *                                  " length = " + numSets);
         *           }
         *         }
         *     }
         *   });
         *   cd.setColor(Color.red);
         *   cd.setLineWidth(2.0f);
         *   navDisplay.addDisplayable(cd);
         * }
         */

        /*
         * System.out.println("Setting projection GOES-E satellite");
         * Thread.sleep(5000);
         * AreaAdapter aa =
         *   new AreaAdapter(
         *       "adde://adde.ucar.edu/imagedata?group=rtimages&descr=edfloater-i&compress=true");
         *   navDisplay.setMapProjection((MapProjection) aa.getCoordinateSystem());
         * EarthLocation el =
         *   navDisplay.getEarthLocation(new double[] {0.0, 0.0, 1.0});
         * System.out.println("location = " + el);
         * System.out.println(navDisplay.getSpatialCoordinates(el));
         */
    }

    /**
     * An adapter for visad.georef.MapProjection coordinate systems (ie:
     * ones with * a reference of Lat/Lon).  Allows for the conversion from
     * lat/lon to Display.DisplaySpatialCartesianTuple (XYZ).
     * Altitude (z) values are held constant.
     */
    protected class MapProjection3DAdapter extends CoordinateSystem implements InverseLinearScaledCS {

        /** index of the latitude coordinate */
        private final int latIndex;

        /** index of the longitude coordinate */
        private final int lonIndex;

        /** map projection for xy -> lat/lon transformations */
        private final MapProjection mapProjection;

        /** X offset */
        private final double offsetX;

        /** Y offset */
        private final double offsetY;

        /** X scaling factor */
        private final double scaleX;

        /** Y scaling factor */
        private final double scaleY;

        /** the coordinate system */
        private CoordinateSystem theCoordinateSystem;

        /** index of the x coordinate */
        private final int xIndex;

        /** index of the y coordinate */
        private final int yIndex;

        /**
         * Construct a new CoordinateSystem which uses a MapProjection for
         * the transformations between x,y and lat/lon.
         *
         * @param  mapProjection  CoordinateSystem that transforms from xy
         *                        in the data space to lat/lon.
         * @exception  VisADException  can't create the necessary VisAD object
         */
        public MapProjection3DAdapter(MapProjection mapProjection) throws VisADException {
            super(Display.DisplaySpatialCartesianTuple, new Unit[] { CommonUnit.degree, CommonUnit.degree, null });
            this.mapProjection       = mapProjection;
            this.theCoordinateSystem = new CachingCoordinateSystem(this.mapProjection);
            latIndex                 = mapProjection.getLatitudeIndex();
            lonIndex                 = mapProjection.getLongitudeIndex();

            if (mapProjection.isXYOrder()) {
                xIndex = 0;
                yIndex = 1;
            } else {
                xIndex = 1;
                yIndex = 0;
            }

            /*
             * System.out.println("latIndex = " + latIndex +
             *                  " lonIndex = " + lonIndex +
             *                  " xIndex = " + xIndex +
             *                  " yIndex = " + yIndex);
             */

            java.awt.geom.Rectangle2D bounds = mapProjection.getDefaultMapArea();

            /*
             * System.out.println("X = " + bounds.getX() +
             *                  " Y = "+ bounds.getY() +
             *                  " width = "+ bounds.getWidth() +
             *                  " height = "+ bounds.getHeight());
             */
            scaleX  = bounds.getWidth() / 2.0;
            scaleY  = bounds.getHeight() / 2.0;
            offsetX = bounds.getX() + scaleX;
            offsetY = bounds.getY() + scaleY;

            /*
             * System.out.println("scaleX = " + scaleX +
             *                  " scaleY = "+ scaleY +
             *                  " offsetX = "+ offsetX +
             *                  " offsetY = "+ offsetY);
             */
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
        public double[][] toReference(double[][] latlonalt) throws VisADException {
            if ((latlonalt == null) || (latlonalt[0].length < 1)) {
                return latlonalt;
            }

            int numpoints = latlonalt[0].length;

            call1("toReference(d)", numpoints);

            double[][] t2 = new double[2][];

            t2[latIndex] = latlonalt[0];
            t2[lonIndex] = latlonalt[1];

            /*
             */
            if (adjustLons) {
                t2[lonIndex] = (use360)
                               ? GeoUtils.normalizeLongitude360(latlonalt[1])

                // ? latlonalt[1]
                               : GeoUtils.normalizeLongitude(latlonalt[1]);
            }

            t2 = theCoordinateSystem.fromReference(t2);

            if (t2 == null) {
                throw new VisADException("MapProjection.toReference: " + "Can't do (lat,lon) to (x,y) transformation");
            }

            double   x, y;
            double[] t2x = t2[xIndex];
            double[] t2y = t2[yIndex];

            for (int i = 0; i < numpoints; i++) {
                if (Double.isNaN(t2x[i]) || Double.isNaN(t2y[i])) {
                    x = Double.NaN;
                    y = Double.NaN;
                } else {
                    x = (t2x[i] - offsetX) / scaleX;
                    y = (t2y[i] - offsetY) / scaleY;
                }

                latlonalt[0][i] = x;
                latlonalt[1][i] = y;
            }

            call2("toReference(d)", numpoints);

            return latlonalt;
        }

        /**
         * debug
         *
         * @param msg debug
         * @param numpoints debug
         */
        void call1(String msg, int numpoints) {
            if (numpoints > 10000) {

                // Misc.printStack(msg,5,null);
                Trace.call1("MapProjectionDisplay." + msg, " numpoints = " + numpoints);
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
                Trace.call2("MapProjectionDisplay." + msg);
            }
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
        public float[][] toReference(float[][] latlonalt) throws VisADException {
            if ((latlonalt == null) || (latlonalt[0].length < 1)) {
                return latlonalt;
            }

            int numpoints = latlonalt[0].length;

            call1("toReference(f)", numpoints);

            float[][] t2 = new float[2][];

            t2[latIndex] = latlonalt[0];
            t2[lonIndex] = latlonalt[1];

            /*
             */
            if (adjustLons) {
                t2[lonIndex] = (use360)
                               ? GeoUtils.normalizeLongitude360(latlonalt[1])

                // ? latlonalt[1]
                               : GeoUtils.normalizeLongitude(latlonalt[1]);
            }

            // call1("mapProjection.fromReference", numpoints);
            // Trace.msg("MapProjectionDisplay. class=" + mapProjection.getClass().getName());
            t2 = theCoordinateSystem.fromReference(t2);

            // call2("mapProjection.fromReference", numpoints);

            if (t2 == null) {
                throw new VisADException("MapProjection.toReference: " + "Can't do (lat,lon) to (x,y) transformation");
            }

            float   x, y;
            float[] t2ax = t2[xIndex];
            float[] t2ay = t2[yIndex];

            for (int i = 0; i < numpoints; i++) {
                float t2x = t2ax[i];
                float t2y = t2ay[i];

                if ((t2x != t2x) || (t2y != t2y)) {
                    x = Float.NaN;
                    y = Float.NaN;
                } else {
                    x = (float) ((t2x - offsetX) / scaleX);
                    y = (float) ((t2y - offsetY) / scaleY);
                }

                latlonalt[0][i] = x;
                latlonalt[1][i] = y;
            }

            call2("toReference(f)", numpoints);

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
        public double[][] fromReference(double[][] xyz) throws VisADException {
            if ((xyz == null) || (xyz[0].length < 1)) {
                return xyz;
            }

            int numpoints = xyz[0].length;

            call1("fromReference(d)", numpoints);

            for (int i = 0; i < numpoints; i++) {
                if (Double.isNaN(xyz[0][i]) || Double.isNaN(xyz[0][i])) {
                    continue;
                }

                xyz[0][i] = (xyz[0][i] * scaleX + offsetX);
                xyz[1][i] = (xyz[1][i] * scaleY + offsetY);
            }

            double[][] t2 = new double[][] {
                xyz[xIndex], xyz[yIndex]
            };

            t2 = theCoordinateSystem.toReference(t2);

            if (t2 == null) {
                throw new VisADException("MapProjection.toReference: " + "Can't do (x,y) to (lat,lon) transformation");
            }

            xyz[0] = t2[latIndex];
            xyz[1] = t2[lonIndex];

            /*
             */
            if (adjustLons) {
                xyz[1] = (use360)
                         ? GeoUtils.normalizeLongitude360(t2[lonIndex])

                // ? t2[lonIndex]
                         : GeoUtils.normalizeLongitude(t2[lonIndex]);
            }

            call2("fromReference(d)", numpoints);

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
        public float[][] fromReference(float[][] xyz) throws VisADException {
            if ((xyz == null) || (xyz[0].length < 1)) {
                return xyz;
            }

            int numpoints = xyz[0].length;

            call1("fromReference(f)", numpoints);

            for (int i = 0; i < numpoints; i++) {
                if (Float.isNaN(xyz[0][i]) || Float.isNaN(xyz[0][i])) {
                    continue;
                }

                xyz[0][i] = (float) (xyz[0][i] * scaleX + offsetX);
                xyz[1][i] = (float) (xyz[1][i] * scaleY + offsetY);
            }

            float[][] t2 = new float[][] {
                xyz[xIndex], xyz[yIndex]
            };

            t2 = theCoordinateSystem.toReference(t2);

            if (t2 == null) {
                throw new VisADException("MapProjection.toReference: " + "Can't do (x,y) to (lat,lon) transformation");
            }

            xyz[0] = t2[latIndex];
            xyz[1] = t2[lonIndex];

            /*
             */
            if (adjustLons) {
                xyz[1] = (use360)
                         ? GeoUtils.normalizeLongitude360(t2[lonIndex])

                // ? t2[lonIndex]
                         : GeoUtils.normalizeLongitude(t2[lonIndex]);
            }

            call2("fromReference(f)", numpoints);

            return xyz;
        }

        /**
         * See if this is equal to the object in question.
         *
         * @param  obj  object in question.
         *
         * @return      true if they are equal. The two objects are equal if
         *              their MapProjections are equal.
         */
        public boolean equals(Object obj) {
            if (!(obj instanceof MapProjection3DAdapter)) {
                return false;
            }

            MapProjection3DAdapter that = (MapProjection3DAdapter) obj;

            return (that.mapProjection).equals(mapProjection);
        }

        /**
         * Return the MapProjection being used by the CoordinateSystem.
         *
         * @return  the MapProjection used in this instance
         */
        public MapProjection getMapProjection() {
            return mapProjection;
        }

        /**
         * Get the scale
         *
         * @return  the scale (x,y)
         */
        public double[] getScale() {
            return new double[] { scaleX, scaleY };
        }

        /**
         * Get the offset
         *
         * @return  the offset (x_off, y_off)
         */
        public double[] getOffset() {
            return new double[] { offsetX, offsetY };
        }

        /**
         * Get the inverted coordinate system
         *
         * @return the inverted coordinate system
         */
        public CoordinateSystem getInvertedCoordinateSystem() {
            return this.mapProjection;
        }
    }
}
