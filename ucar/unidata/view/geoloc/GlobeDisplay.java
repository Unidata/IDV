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


import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.MapLines;
import ucar.visad.display.RubberBandBox;
import ucar.visad.display.ScalarMapSet;
import ucar.visad.quantities.GeopotentialAltitude;

import visad.ActionImpl;
import visad.CommonUnit;
import visad.ConstantMap;
import visad.CoordinateSystem;
import visad.DataRenderer;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRealType;
import visad.DisplayTupleType;
import visad.FlatField;
import visad.FunctionType;
import visad.KeyboardBehavior;
import visad.MouseBehavior;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.Unit;
import visad.UnitException;
import visad.VisADException;
import visad.VisADRay;

import visad.data.mcidas.BaseMapAdapter;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.MapProjection;

import visad.java3d.DefaultDisplayRendererJ3D;
import visad.java3d.DisplayImplJ3D;
import visad.java3d.DisplayRendererJ3D;
import visad.java3d.KeyboardBehaviorJ3D;
import visad.java3d.ProjectionControlJ3D;


import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.Iterator;

import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;


/**
 * Provides a navigated globe for displaying geolocated data.
 * Any displayable data must be able to map to RealType.Latitude,
 * RealType.Longitude and/or RealType.Altitude.
 *
 * @author Don Murray
 * @version $Revision: 1.49 $ $Date: 2007/07/31 15:11:25 $
 */
public class GlobeDisplay extends NavigatedDisplay {

    /** Bottom View name */
    public static String BOTTOM_VIEW_NAME = "Southern Hemisphere";

    /** North View name */
    public static String NORTH_VIEW_NAME = "Western Hemisphere";

    /** East View name */
    public static String EAST_VIEW_NAME = "Pacific Region";

    /** Top View name */
    public static String TOP_VIEW_NAME = "Northern Hemisphere";

    /** South View name */
    public static String SOUTH_VIEW_NAME = "Eastern Hemisphere";

    /** West View name */
    public static String WEST_VIEW_NAME = "Atlantic Region";

    /** latitude ScalarMap */
    private ScalarMap latitudeMap = null;

    /** longitude ScalarMap */
    private ScalarMap longitudeMap = null;

    /** altitude ScalarMap */
    private ScalarMap altitudeMap = null;

    /** minimum range for altitudeMap; about a 40x vertical exaggeration */
    private double altitudeMin = -159000;

    /** maximum range for altitudeMap; about a 40x vertical exaggeration */
    private double altitudeMax = 159000;

    /** flag for whether this has been initialized or not */
    private boolean init = false;

    /** display coordinate system */
    private CoordinateSystem coordinateSystem =
        Display.DisplaySphericalCoordSys;

    /** units for cs */
    private Unit[] csUnits = null;

    /** default vertical parameter */
    private RealType verticalParameter = RealType.Altitude;

    /** default surface value */
    private Real surface = new Real(RealType.Altitude, 0);

    /** default view */
    private int view = ProjectionControlJ3D.Z_PLUS;

    /** flag for stereo */
    private boolean canDoStereo = false;

    /** Set of vertical maps */
    private VerticalMapSet verticalMapSet = new VerticalMapSet();

    /** Earth Radius (m) */
    public static final double EARTH_RADIUS = 6371229.;

    /**
     * Constructs a new GlobeDisplay.
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public GlobeDisplay() throws VisADException, RemoteException {
        this(false, null, null);
    }

    /**
     * Constructs a new GlobeDisplay.
     *
     * @param offscreen  true for an offscreen display
     * @param dimension  size of the display
     * @param screen     screen device
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public GlobeDisplay(boolean offscreen, Dimension dimension,
                        GraphicsDevice screen)
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
        boolean useStereo = System.getProperty("idv.enableStereo",
                                "false").equals("true");
        GraphicsConfiguration config =
            ucar.visad.display.DisplayUtil.getPreferredConfig(screen, true,
                useStereo);
        DisplayRendererJ3D renderer = new DefaultDisplayRendererJ3D();
        renderer.setScaleRotation(true);
        renderer.setRotateAboutCenter(true);
        if (offscreen) {
            displayImpl = new DisplayImplJ3D("Globe Display", renderer,
                                             dimension.width,
                                             dimension.height);
        } else {
            if (config == null) {
                LogUtil.userErrorMessage(
                    "Could not create a graphics configuration.\nPlease contact Unidata user support or see the FAQ");
                System.exit(1);
            }
            displayImpl = new DisplayImplJ3D("Globe Display", renderer, api,
                                             config);
        }
        super.init(displayImpl);
        /*
        super((DisplayImpl) new DisplayImplJ3D("Globe Display",
                defaultConfig));
        */
        setBoxVisible(false);
        setSpatialScalarMaps();
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
        csUnits = coordinateSystem.getCoordinateSystemUnits();
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        canDoStereo = rend.getCanvas().getStereoAvailable();
        //System.err.println("GlobeDisplay:canDoStereo = " + canDoStereo);
        setPerspectiveView(canDoStereo);
        checkClipDistance();
        setEyePosition(0.004);

        KeyboardBehaviorJ3D behavior = new KeyboardBehaviorJ3D(rend);
        rend.addKeyboardBehavior(behavior);
        setKeyboardBehavior(behavior);


        // Create a RubberBandBox
        RubberBandBox rubberBandBox = new RubberBandBox(RealType.Latitude,
                                          RealType.Longitude,
                                          InputEvent.SHIFT_MASK);
        rubberBandBox.addAction(new ActionImpl("RBB Action") {
            public void doAction() throws VisADException, RemoteException {
                RubberBandBox box = getRubberBandBox();
                if ((box == null) || (box.getBounds() == null)) {
                    return;
                }
                float[][] samples = box.getBounds().getSamples();
                if (samples == null) {
                    //              System.err.println ("Samples == null");
                    return;
                }
                ProjectionRect rect = new ProjectionRect(samples[1][0],
                                          samples[0][0], samples[1][1],
                                          samples[0][1]);
                setMapArea(rect);
            }
        });
        setRubberBandBox(rubberBandBox);
        enableRubberBanding(true);
        setPolygonOffsetFactor(1);
    }


    /**
     * Accessor method.
     * @return name for this view
     */
    public String getTopViewName() {
        return TOP_VIEW_NAME;
    }

    /**
     * Accessor method.
     * @return name for this view
     */
    public String getBottomViewName() {
        return BOTTOM_VIEW_NAME;
    }

    /**
     * Accessor method.
     * @return name for this view
     */
    public String getNorthViewName() {
        return NORTH_VIEW_NAME;
    }

    /**
     * Accessor method.
     * @return name for this view
     */
    public String getEastViewName() {
        return EAST_VIEW_NAME;
    }

    /**
     * Accessor method.
     * @return name for this view
     */
    public String getSouthViewName() {
        return SOUTH_VIEW_NAME;
    }

    /**
     * Accessor method.
     * @return name for this view
     */
    public String getWestViewName() {
        return WEST_VIEW_NAME;
    }


    //    private List keyboardBehaviors;

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
     * Get the earth coordinates from the screen coordinates
     *
     * @param x screen x position
     * @param y screen y position
     *
     * @return corresponding earth location
     *
     * @throws VisADException problem getting coordinates
     */
    public EarthLocation screenToEarthLocation(int x, int y)
            throws VisADException {

        double[]      xyz = getSpatialCoordinatesFromScreen(x, y, Double.NaN);
        EarthLocation el  = getEarthLocation(xyz);
        //        System.err.println("screenToEarth: x/y:" + x +"/" + y +" x/y/z:" + x1+"/" + x2 +"/" + x3 + "  el:" + el);
        return el;
    }






    /**
     * Handles a change in the position of the mouse-pointer.  For
     * this implementation, it will only list the
     *
     * @param x    x mouse location
     * @param y    y mouse location
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

        double[]      xyz  = getSpatialCoordinatesFromScreen(x, y,
                                 Double.NaN);
        EarthLocation el   = getEarthLocation(xyz);
        double[]      xyz2 = new double[3];
        xyz2 = getSpatialCoordinates(el, xyz2);

        //            xyz = navDisplay.getSpatialCoordinates(ob.getEarthLocation(),
        //                    xyz);
        //        System.err.println("x/y:" + x+"/" + y +" xyz:" + xyz[0]+"/"+xyz[1]);
        //        EarthLocation el = screenToEarthLocation(x,y);
        setCursorLatitude(el.getLatitude());
        setCursorLongitude(el.getLongitude());
        setCursorAltitude(surface);  // always use surface for this renderer
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
        if ( !init) {
            setDisplayInactive();
            ScalarMapSet mapSet = new ScalarMapSet();

            latitudeMap = new ScalarMap(RealType.Latitude, Display.Latitude);
            mapSet.add(latitudeMap);
            latitudeMap.setRange(-90, 90);
            latitudeMap.setScaleEnable(false);

            longitudeMap = new ScalarMap(RealType.Longitude,
                                         Display.Longitude);
            mapSet.add(longitudeMap);
            longitudeMap.setRange(-180, 180);
            longitudeMap.setScaleEnable(false);

            altitudeMap = new ScalarMap(RealType.Altitude, Display.Radius);
            setVerticalRange(altitudeMin, altitudeMax);
            mapSet.add(altitudeMap);
            altitudeMap.setScaleEnable(false);

            ScalarMap xMap = new ScalarMap(RealType.XAxis, Display.XAxis);
            xMap.setRange(-1.0, 1.0);
            xMap.setScaleEnable(false);
            mapSet.add(xMap);

            ScalarMap yMap = new ScalarMap(RealType.YAxis, Display.YAxis);
            yMap.setRange(-1.0, 1.0);
            yMap.setScaleEnable(false);
            mapSet.add(yMap);

            ScalarMap zMap = new ScalarMap(RealType.ZAxis, Display.ZAxis);
            zMap.setRange(-1.0, 1.0);
            zMap.setScaleEnable(false);
            mapSet.add(zMap);
            init = true;

            addScalarMaps(mapSet);
            setDisplayActive();
        }

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
            throws VisADException, RemoteException {

        double centerLat = mapArea.getY() + mapArea.getHeight() / 2;
        double centerLon = mapArea.getX() + mapArea.getWidth() / 2;
        centerAndZoom(new EarthLocationTuple(centerLat, centerLon, 0), true,
                      2.0);
    }

    /**
     * Define the map projection using a MapProjection type CoordinateSystem.
     * Implementation will be subclass dependent.
     *
     * @param  mapProjection   map projection coordinate system
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public void setMapProjection(MapProjection mapProjection)
            throws VisADException, RemoteException {}

    /**
     * Accessor method for the DisplayLatitudeType
     *
     * @return DisplayRealType for Latitude mapping
     */
    public DisplayRealType getDisplayLatitudeType() {
        return Display.Latitude;
    }

    /**
     * Accessor method for the DisplayLongitudeType
     * @return DisplayRealType for Longitude mapping
     */
    public DisplayRealType getDisplayLongitudeType() {
        return Display.Longitude;
    }

    /**
     * Accessor method for the DisplayAltitudeType
     * @return DisplayRealType for Altitude mapping
     */
    public DisplayRealType getDisplayAltitudeType() {
        return Display.Radius;
    }

    /**
     * Accessor method for the DisplayTupleType.
     * @return the tuple of DisplayRealTypes
     */
    public DisplayTupleType getDisplayTupleType() {
        return Display.DisplaySpatialSphericalTuple;
    }

    /**
     * Accessor method for the ScalarMap for Altitude
     * @return the altitude ScalarMap
     */
    protected ScalarMap getAltitudeMap() {
        return altitudeMap;
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
     * Reset the scale translate
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  matrix problem
     */
    public void resetScaleTranslate() throws VisADException, RemoteException {
        double[]      myMatrix      = getProjectionMatrix();
        double[]      trans         = { 0.0, 0.0, 0.0 };
        double[]      rot           = { 0.0, 0.0, 0.0 };
        double[]      rot2          = { 0.0, 0.0, 0.0 };
        double[]      scale         = { 0.0, 0.0, 0.0 };
        MouseBehavior mouseBehavior = getMouseBehavior();


        double[] saved =
            getDisplay().getProjectionControl().getSavedProjectionMatrix();

        mouseBehavior.instance_unmake_matrix(rot, scale, trans, myMatrix);
        mouseBehavior.instance_unmake_matrix(rot2, scale, trans, saved);

        double[] t = mouseBehavior.make_matrix(rot[0], rot[1], rot[2],
                         scale[0], scale[1], scale[2], trans[0], trans[1],
                         trans[2]);
        setProjectionMatrix(t);
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
            this.view = view;
        } catch (VisADException e) {
            ;
        } catch (RemoteException re) {
            ;
        }
    }

    /**
     * Enable clipping of data at the box edges
     *
     * @param  clip  true to turn clipping on, otherwise off
     */
    public void enableClipping(boolean clip) {
        DisplayRendererJ3D dr =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        try {
            dr.setClip(0, clip, 1.0f, 0.0f, 0.0f, -1.01f);
            dr.setClip(1, clip, -1.0f, 0.0f, 0.0f, -1.01f);
            dr.setClip(2, clip, 0.0f, 1.0f, 0.0f, -1.01f);
            dr.setClip(3, clip, 0.0f, -1.0f, 0.0f, -1.01f);
            dr.setClip(4, clip, 0.0f, 0.0f, 1.0f, -1.01f);
            dr.setClip(5, clip, 0.0f, 0.0f, -1.0f, -1.01f);
        } catch (VisADException ve) {
            System.err.println("Couldn't set clipping " + ve);
        }
        super.enableClipping(clip);
    }


    /**
     * Get the View
     *
     * @return the View
     */
    public View getView() {
        if (getDisplay() == null) {
            return null;
        }
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        return rend.getView();
    }


    /**
     * Set the view to perspective or parallel if this is a 3D display.
     *
     * @param perspectiveView  true for perspective view
     */
    public void setPerspectiveView(boolean perspectiveView) {

        if (perspectiveView == isPerspectiveView()) {
            return;
        }

        try {
            getDisplay().getGraphicsModeControl().setProjectionPolicy(
                (perspectiveView == true)
                ? DisplayImplJ3D.PERSPECTIVE_PROJECTION
                : DisplayImplJ3D.PARALLEL_PROJECTION);

        } catch (Exception e) {
            ;
        }
        super.setPerspectiveView(perspectiveView);

        checkClipDistance();

    }

    /**
     * Get the EarthLocation of a point in XYZ space
     *
     * @param  x  x coord.
     * @param  y  y coord.
     * @param  z  z coord.
     * @param  setZToZeroIfOverhead If in the overhead view then set Z to 0
     *
     * @return point in lat/lon/alt space.
     */
    public EarthLocation getEarthLocation(double x, double y, double z,
                                          boolean setZToZeroIfOverhead) {
        EarthLocationTuple value = null;
        try {
            float[][] numbers = visad.Set.doubleToFloat(
                                    coordinateSystem.fromReference(
                                        new double[][] {
                new double[] { x }, new double[] { y }, new double[] { z }
            }));
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
     * Center and zoom to a particular point
     *
     * @param el the earth location
     * @param altitude  the altitude
     * @param zoomFactor  zoom factor
     * @param animated  true to animate
     * @param northUp rotate so north is up
     *
     * @throws  RemoteException        Couldn't create a remote object
     * @throws  VisADException         Couldn't create necessary VisAD object
     */
    public void centerAndZoom(final EarthLocation el, Real altitude,
                              double zoomFactor, boolean animated,
                              boolean northUp)
            throws VisADException, RemoteException {


        if ((zoomFactor == 0) || (zoomFactor != zoomFactor)) {
            zoomFactor = 1.0;
        }
        if (el.getLongitude().isMissing() || el.getLatitude().isMissing()) {
            return;
        }

        MouseBehavior mouseBehavior = getMouseBehavior();
        double[]      currentMatrix = getProjectionMatrix();
        double[]      trans         = { 0.0, 0.0, 0.0 };
        double[]      scale         = { 0.0, 0.0, 0.0 };
        double[]      scaletmp      = { 0.0, 0.0, 0.0 };
        double[]      rot1          = { 0.0, 0.0, 0.0 };
        double[]      rot2          = { 0.0, 0.0, 0.0 };

        getMouseBehavior().instance_unmake_matrix(rot2, scale, trans,
                currentMatrix);

        double[]    xy = getSpatialCoordinates(el, null);

        Transform3D t  = new Transform3D();
        double[] xxx = getMouseBehavior().make_matrix(rot2[0], rot2[1],
                           rot2[2], 1.0, 0, 0, 0);

        Transform3D t2       = new Transform3D(xxx);
        Vector3d    upVector = new Vector3d(0, 0, (northUp
                ? 1
                : -1));

        //        t2.transform(v3d);
        //        System.err.println("v3d:" + v3d.x+"/"+v3d.y+"/"+v3d.z);
        t.lookAt(new Point3d(xy[0], xy[1], xy[2]), new Point3d(0, 0, 0),
                 upVector);
        double[] m = new double[16];
        t.get(m);
        getMouseBehavior().instance_unmake_matrix(rot1, scaletmp, trans, m);
        if (zoomFactor != zoomFactor) {
            zoomFactor = 1;
        }
        m = getMouseBehavior().make_matrix(rot1[0], rot1[1], rot1[2],
                                           zoomFactor * scale[0],
                                           zoomFactor * scale[1],
                                           zoomFactor * scale[2], trans[0],
                                           trans[1], trans[2]);

        if ( !animated) {
            setProjectionMatrix(m);
        } else {
            final double[] to = m;
            Misc.run(new Runnable() {
                public void run() {
                    animateMatrix(++animationTimeStamp,
                                  getProjectionMatrix(), to, null);
                }
            });
        }

    }





    /**
     * Get spatial coordinates from screen
     *
     * @param x screen X
     * @param y screen Y
     * @param zDepth  the z depth
     *
     * @return the coordinates
     */
    public double[] getSpatialCoordinatesFromScreen(int x, int y,
            double zDepth) {
        try {
            VisADRay ray =
                getDisplay().getDisplayRenderer().getMouseBehavior().findRay(
                    x, y);
            double           x1 = ray.position[0];
            double           x2 = ray.position[1];
            double           x3 = ray.position[2];
            java.util.Vector v  = getDisplay().getRenderers();
            if ( !v.isEmpty()) {
                DataRenderer rend      = (DataRenderer) v.get(0);
                double[]     origin    = ray.position;
                double[]     direction = ray.vector;
                float r = rend.findRayManifoldIntersection(true, origin,
                              direction,
                              Display.DisplaySpatialSphericalTuple, 2, 1);
                if (r != r) {
                    x1 = Double.NaN;
                    x2 = Double.NaN;
                    x3 = Double.NaN;
                } else {
                    float[][] xx = {
                        { (float) (origin[0] + r * direction[0]) },
                        { (float) (origin[1] + r * direction[1]) },
                        { (float) (origin[2] + r * direction[2]) }
                    };
                    x1 = xx[0][0];
                    x2 = xx[1][0];
                    x3 = xx[2][0];
                }
            }
            return new double[] { x1, x2, ((zDepth == zDepth)
                                           ? zDepth
                                           : x3) };
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * Returns the spatial (XYZ) coordinates of the particular EarthLocation
     *
     * @param el    earth location (lat/lon/alt) to translate
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
            float[][] temp = coordinateSystem.toReference(new float[][] {
                latitudeMap.scaleValues(new double[] {
                    el.getLatitude().getValue(CommonUnit.degree) }),
                longitudeMap.scaleValues(new double[] {
                    el.getLongitude().getValue(CommonUnit.degree) }),
                altitudeMap.scaleValues(new double[] {
                    el.getAltitude().getValue(CommonUnit.meter) })
            });
            double[] xyz = new double[3];
            xyz[0] = temp[0][0];
            xyz[1] = temp[1][0];
            xyz[2] = temp[2][0];
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
     * @param el    earth location (lat/lon/alt) to translate
     * @param xyz Where to put the value
     * @param altitude the altitude
     *
     * @return  The xyz array
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
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
     * Add a new mapping of this type to the vertical coordinate
     *
     * @param newVertType  RealType of map
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void addVerticalMap(RealType newVertType)
            throws VisADException, RemoteException {

        Unit u = newVertType.getDefaultUnit();
        if ( !(Unit.canConvert(u, CommonUnit.meter)
                || Unit.canConvert(
                    u, GeopotentialAltitude.getGeopotentialMeter()))) {
            throw new VisADException("Unable to handle units of "
                                     + newVertType);
        }
        ScalarMap newMap = new ScalarMap(newVertType,
                                         getDisplayAltitudeType());
        setVerticalMapUnit(newMap, getVerticalRangeUnit());
        double[] range = getVerticalRange();
        newMap.setRange(range[0], range[1]);
        verticalMapSet.add(newMap);
        addScalarMaps(verticalMapSet);
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
        ScalarMapSet sms = new ScalarMapSet();
        for (Iterator iter = verticalMapSet.iterator(); iter.hasNext(); ) {
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
        }
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
        verticalMapSet.setVerticalRange(min, max);
    }


    /**
     * Determine if this MapDisplay can do stereo.
     *
     * @return true if the graphics device can do stereo
     */
    public boolean getStereoAvailable() {
        return canDoStereo;
    }


    /** defaultConfiguration */
    private static GraphicsConfiguration defaultConfig = makeConfig();

    /**
     * Create the default configuration
     * @return the default graphic configuration
     */
    private static GraphicsConfiguration makeConfig() {
        GraphicsEnvironment e =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice           d        = e.getDefaultScreenDevice();
        GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();
        if (System.getProperty("idv.enableStereo", "false").equals("true")) {
            template.setStereo(GraphicsConfigTemplate3D.PREFERRED);
        }
        GraphicsConfiguration c = d.getBestConfiguration(template);

        return c;
    }

    /**
     * Method for setting the eye position for a 3D stereo view.
     *
     * @param position  x position of each eye (left negative, right positive).
     */
    public void setEyePosition(double position) {
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        // From Dan Bramer
        PhysicalBody myBody = rend.getView().getPhysicalBody();
        myBody.setLeftEyePosition(new Point3d(-position, 0.0, 0.0));
        // default is(-0.033, 0.0, 0.0)
        myBody.setRightEyePosition(new Point3d(+position, 0.0, 0.0));
    }

    /**
     * Get the latlon box of the displayed area. This checks to see if the globe is sort of fully zoomed out.
     * If it is then it returns an earth spanning bbox
     *
     * @return lat lon box  or null if it can't be determined
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Rectangle2D.Double getLatLonBox()
            throws VisADException, RemoteException {
        java.awt.Rectangle b   = getScreenBounds();
        EarthLocation      el1 = screenToEarthLocation(b.width / 2, 0);
        EarthLocation      el2 = screenToEarthLocation(b.width / 2, b.height);
        EarthLocation      el3 = screenToEarthLocation(0, b.height / 2);
        EarthLocation      el4 = screenToEarthLocation(b.width, b.height / 2);
        if ((el1 == null) || (el2 == null) || (el3 == null)
                || (el4 == null)) {
            return new Rectangle2D.Double(-180, -90, 360, 180);
        }

        double[] v = { el1.getLatitude().getValue(),
                       el2.getLatitude().getValue(),
                       el3.getLatitude().getValue(),
                       el4.getLatitude().getValue() };
        if (((v[0] == 0) || (v[0] != v[0]))
                && ((v[1] == 0) || (v[1] != v[1]))
                && ((v[2] == 0) || (v[2] != v[2]))
                && ((v[3] == 0) || (v[3] != v[3]))) {
            return new Rectangle2D.Double(-180, -90, 360, 180);
        }

        return super.getLatLonBox();

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
     * Get the viewpoint earth location
     *
     * @return  the location
     */
    public EarthLocation getViewPointEarthLocation() {
        //In VisAd viewpoint doesn't change - 
        //whole Universe is transformed around it
        double[] viewPointXYZCoords = new double[] { 0.0, 0.0,
                getZViewOffSet() };
        return whereIsTransformedXYZPoint(viewPointXYZCoords);
    }

    /**
     * Where is the transformed XYZ point
     *
     * @param xyzCoords  the point to check
     *
     * @return  the corresponding earth location
     */
    private EarthLocation whereIsTransformedXYZPoint(double[] xyzCoords) {
        //first undo transforms to get back to raw XYZ
        Transform3D currentTransform = new Transform3D(getProjectionMatrix());
        //want to be going back to VisAD XYZ coordinates
        currentTransform.invert();
        Point3d point3d = new Point3d(xyzCoords[0], xyzCoords[1],
                                      xyzCoords[2]);
        currentTransform.transform(point3d);
        return getEarthLocation(new double[] { point3d.x, point3d.y,
                point3d.z });
    }

    /**
     * Get the Z offset
     *
     * @return the z offset
     */
    private double getZViewOffSet() {
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        View           thisView  = rend.getView();
        Transform3D    t3d       = new Transform3D();
        TransformGroup viewTrans = rend.getViewTrans();
        double[]       vtMatrix  = new double[16];
        viewTrans.getTransform(t3d);
        t3d.get(vtMatrix);
        //zViewOffSet=vtMatrix[11];
        return vtMatrix[11];
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
        final GlobeDisplay navDisplay = new GlobeDisplay();
        if (args.length == 0) {
            MapLines mapLines  = new MapLines("maplines");
            URL      mapSource =
            //new URL("ftp://www.ssec.wisc.edu/pub/visad-2.0/OUTLSUPW");
            navDisplay.getClass().getResource("/auxdata/maps/OUTLSUPW");
            try {
                BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
                mapLines.setMapLines(mapAdapter.getData());
                mapLines.setColor(java.awt.Color.black);
                mapLines.addConstantMap(new ConstantMap(1.005,
                        Display.Radius));
                navDisplay.addDisplayable(mapLines);
            } catch (Exception excp) {
                System.out.println("Can't open map file " + mapSource);
                System.out.println(excp);
            }
            Grid2DDisplayable sphere = new Grid2DDisplayable("sphere", false);
            FlatField sff = FlatField.makeField1(
                                new FunctionType(
                                    RealTupleType.SpatialEarth2DTuple,
                                    RealType.getRealType("value")), 0, 360,
                                        360, -90, 90, 180);
            sphere.setData(sff);
            navDisplay.addDisplayable(sphere);
        } else {  // args.length != 0
            navDisplay.enableRubberBanding(false);
            navDisplay.setBoxVisible(true);
        }

        JPanel  panel  = new JPanel(new GridLayout(1, 0));
        JButton pushme = new JButton("Map Projection Manager");
        panel.add(pushme);
        frame.getContentPane().add(panel, BorderLayout.NORTH);

        ViewpointControl vpc = new ViewpointControl(navDisplay);
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(navDisplay.getComponent(), BorderLayout.CENTER);
        Component comp = (Component) GuiUtils.topCenterBottom(
                             vpc.getToolBar(JToolBar.VERTICAL),
                             new NavigatedDisplayToolBar(
                                 navDisplay,
                                 JToolBar.VERTICAL), GuiUtils.filler());
        panel.add(comp, BorderLayout.WEST);
        panel.add(new NavigatedDisplayCursorReadout(navDisplay),
                  BorderLayout.SOUTH);
        navDisplay.draw();
        frame.getContentPane().add(panel, BorderLayout.CENTER);
        if (navDisplay.getDisplayMode() == navDisplay.MODE_3D) {
            JMenuBar mb = new JMenuBar();
            mb.add(vpc.getMenu());
            frame.setJMenuBar(mb);
        }

        final ProjectionManager pm = new ProjectionManager();
        pm.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                if (e.getPropertyName().equals("ProjectionImpl")) {
                    try {
                        navDisplay.setMapProjection(
                            (ProjectionImpl) e.getNewValue());
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
        navDisplay.getDisplay().getGraphicsModeControl().setScaleEnable(true);
        // Rotate checkbox
        JCheckBox rotate = new JCheckBox("Rotate Display", false);
        rotate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                navDisplay.setAutoRotate(
                    ((JCheckBox) e.getSource()).isSelected());
            }
        });
        JButton vpWhere = new JButton("Where am I?");
        vpWhere.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("You are at "
                                   + navDisplay.getViewPointEarthLocation());
            }
        });
        JPanel p2 = new JPanel();
        p2.add(rotate);
        p2.add(vpWhere);
        frame.getContentPane().add("South", p2);
        frame.pack();
        frame.setVisible(true);

        /*
        JFrame frame = new JFrame();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e)
            {
                System.exit(0);
            }
        });
        GlobeDisplay globeDisplay = new GlobeDisplay();
        frame.getContentPane().add(
            globeDisplay.getComponent(),BorderLayout.CENTER);
        MapLines mapLines = new MapLines("maplines");
        URL mapSource =
            new URL("ftp://ftp.ssec.wisc.edu/pub/visad-2.0/OUTLSUPW");
        try
        {
            BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
            mapLines.setMapLines(mapAdapter.getData());
            //mapLines.setColor(java.awt.Color.red);
            globeDisplay.addDisplayable(mapLines);
            globeDisplay.draw();
        }
        catch (Exception excp)
        {
           System.out.println("Can't open map file " + mapSource);
           System.out.println(excp);
        }
        frame.pack();
        frame.setVisible(true);
        globeDisplay.setBoxVisible(false);
        */
    }



    /**
     * Set the front clip distance
     *
     * @param value clip distance
     */
    public void setClipDistanceFront(double value) {
        super.setClipDistanceFront(value);
        checkClipDistance();
    }

    /**
     * If we are in perspective view then set the clip distance shorter
     */
    public void checkClipDistance() {
        View view = getView();
        if (view == null) {
            return;
        }
        if (isPerspectiveView()) {
            view.setFrontClipDistance(CLIP_FRONT_PERSPECTIVE);
        } else {
            view.setFrontClipDistance(getClipDistanceFront());
        }
    }

}
