/*
 * $Id: GlobeDisplay.java,v 1.49 2007/07/31 15:11:25 dmurray Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.quantities.GeopotentialAltitude;

import visad.*;

import visad.data.mcidas.BaseMapAdapter;

import visad.georef.*;

import visad.java3d.*;
import javax.media.j3d.Transform3D;
import javax.vecmath.*;

import java.awt.*;
import java.awt.event.*;

import java.awt.geom.Rectangle2D;

import java.beans.*;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.Iterator;

import javax.media.j3d.*;

import javax.swing.*;

import javax.vecmath.*;


/**
 * Provides a navigated globe for displaying meteorological data.
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

    /** minimum range for altitudeMap */
    private double altitudeMin = -16000;

    /** maximum range for altitudeMap */
    private double altitudeMax = 16000;

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
        getView().setFrontClipDistance(CLIP_FRONT_DEFAULT);
        //        getView().setBackClipDistance(CLIP_BACK_DEFAULT);
        setPerspectiveView(canDoStereo);
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
                /*
                System.out.println("bounds = " +
                                     samples[1][0] + "," + samples[0][0] + "," +
                           samples[1][1] + "," + samples[0][1]);
                ProjectionRect rect =
                   new ProjectionRect(samples[1][0], samples[0][0],
                           samples[1][1], samples[0][1]);
                System.out.println("rect = " + rect);
                setMapArea(rect);
                */
            }
        });
        setRubberBandBox(rubberBandBox);
        enableRubberBanding(true);
        getDisplay().getGraphicsModeControl().setPolygonOffsetFactor(1);
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
     * _more_
     *
     * @param x _more_
     * @param y _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
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
            throws VisADException, RemoteException {}

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


    public void resetScaleTranslate() throws VisADException, RemoteException  {
        double[]        myMatrix        = getProjectionMatrix();
        double[]        trans          = { 0.0, 0.0, 0.0 };
        double[]        rot            = { 0.0, 0.0, 0.0 };
        double[]        rot2            = { 0.0, 0.0, 0.0 };
        double[]        scale          = { 0.0, 0.0, 0.0 };
        MouseBehavior   mouseBehavior   = getMouseBehavior();
       

        double[] saved = getDisplay().getProjectionControl().getSavedProjectionMatrix();

        mouseBehavior.instance_unmake_matrix(rot, scale, trans, myMatrix);
        mouseBehavior.instance_unmake_matrix(rot2, scale, trans, saved);

        double[] t = mouseBehavior.make_matrix(rot[0], rot[1], rot[2],
                                          scale[0], scale[1], scale[2],
                                          trans[0],trans[1],trans[2]);
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
     * _more_
     *
     * @return _more_
     */
    public View getView() {
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

        if (perspectiveView) {
            getView().setFrontClipDistance(CLIP_FRONT_PERSPECTIVE);
        } else {
            getView().setFrontClipDistance(CLIP_FRONT_DEFAULT);
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



    public void centerAndZoom(final EarthLocation el, boolean animated,
                              double zoomFactor)
            throws VisADException, RemoteException {
        centerAndZoom(el, null,zoomFactor, animated);
    }


    public void centerAndZoomTo(final EarthLocation el, Real altitude,
                                boolean animated)
            throws VisADException, RemoteException {
        centerAndZoom(el, altitude, Double.NaN, animated);
    }



    public void centerAndZoom(final EarthLocation el, Real altitude, double zoomFactor,
                                boolean animated)
            throws VisADException, RemoteException {


        if (el.getLongitude().isMissing() || el.getLatitude().isMissing()) {
            return;
        }

        MouseBehavior      mouseBehavior = getMouseBehavior();
        double[]           currentMatrix = getProjectionMatrix();
        double[]           trans         = { 0.0, 0.0, 0.0 };
        double[]           scale         = { 0.0, 0.0, 0.0 };
        double[] rot1           = { 0.0, 0.0, 0.0 };
        double[] rot2           = { 0.0, 0.0, 0.0 };

        getMouseBehavior().instance_unmake_matrix(rot2, scale, trans,
                                                  currentMatrix);


        double[]           xy            = getSpatialCoordinates(el,
                                                                 null);

        Transform3D t = new Transform3D();
        double []xxx = getMouseBehavior().make_matrix(rot2[0], rot2[1],  rot2[2], 
                                                    1.0,0,0,0);

        Transform3D t2 =  new Transform3D(xxx);
        double[] vector =  new double[]{0,-1,0};
        Vector3d v3d = new Vector3d(vector[0],vector[1],vector[2]);
        //        t2.transform(v3d);
        //        System.err.println("v3d:" + v3d.x+"/"+v3d.y+"/"+v3d.z);
        t.lookAt(new Point3d(xy[0],xy[1],xy[2]), new Point3d(0,0,0), v3d);
        //        t.invert();
        double[]m = new double[16];
        t.get(m);

        getMouseBehavior().instance_unmake_matrix(rot1, scale, trans,
                                                  m);


        m = getMouseBehavior().make_matrix(rot1[0], rot1[1],  rot1[2], 
                                           (zoomFactor==zoomFactor?zoomFactor*scale[0]:scale[0]),
                                           (zoomFactor==zoomFactor?zoomFactor*scale[1]:scale[1]),
                                           (zoomFactor==zoomFactor?zoomFactor*scale[2]:scale[2]),
                                           trans[0],trans[1], trans[2]);

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
     * _more_
     *
     * @param x _more_
     * @param y _more_
     * @param zDepth _more_
     *
     * @return _more_
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
     * @param altitude _more_
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
     * Get the latlon box of the displayed area
     *
     * @return lat lon box  or null if it can't be determined
     */
    public Rectangle2D.Double xxxgetLatLonBox() {
        return new Rectangle2D.Double(-180, -90, 360, 180);
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
        //navDisplay.setBackground(Color.white);
        //navDisplay.setForeground(Color.black);
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
        panel.add((navDisplay.getDisplayMode() == navDisplay.MODE_3D)
                  ? (Component) ucar.unidata.util.GuiUtils.leftRight(
                      new NavigatedDisplayToolBar(navDisplay),
                      vpc.getToolBar())
                  : (Component) new NavigatedDisplayToolBar(
                      navDisplay), BorderLayout.NORTH);
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
        frame.getContentPane().add("South", rotate);
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
}

