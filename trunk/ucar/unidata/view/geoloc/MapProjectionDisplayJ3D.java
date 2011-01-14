/*
 * $Id: MapProjectionDisplayJ3D.java,v 1.36 2007/07/31 15:11:25 dmurray Exp $
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


import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.util.LogUtil;

import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;

import visad.KeyboardBehavior;

import visad.georef.MapProjection;

import visad.java2d.*;

import visad.java3d.*;

import java.awt.*;
import java.awt.event.InputEvent;

import java.rmi.RemoteException;

import javax.media.j3d.*;

import javax.vecmath.*;


/**
 * Provides a navigated VisAD DisplayImpl for displaying data.
 * The Projection or MapProjection provides the transformation from
 * lat/lon space to xy space.  There are two modes that can be used
 * with this display - MODE_3D (Java 3D) and MODE_2Din3D (2D in Java 3D).
 * Performance is better in Java 3D modes. In the 3D
 * mode, RealType.Altitude is mapped to the display Z axis.<P>
 * Any displayable data must be able to map to RealType.Latitude,
 * RealType.Longitude and/or RealType.Altitude.<P>
 * This Display also supports a RubberBandBox in Java 3D for panning
 * and zooming.
 * @see #enableRubberBanding
 *
 * @author Don Murray
 * @version $Revision: 1.36 $ $Date: 2007/07/31 15:11:25 $
 */
public class MapProjectionDisplayJ3D extends MapProjectionDisplay {

    /** instance locking object */
    private static Object INSTANCE_MUTEX = new Object();

    /** flag for perspective view */
    private boolean isPerspective = true;

    /** flag for stereo */
    private boolean canDoStereo = false;


    /**
     * Constructs the default instance.  The default instance is based
     * on a LatLonProjection in 3D mode where Z is mapped to Altitude.
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public MapProjectionDisplayJ3D() throws VisADException, RemoteException {
        this(MODE_3D);
    }

    /**
     * Constructs an instance with a LatLonProjection with the specified
     * mode.
     *
     * @param  mode   mode for display (MODE_3D, MODE_2Din3D)
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public MapProjectionDisplayJ3D(int mode)
            throws VisADException, RemoteException {
        this((MapProjection) null, mode);
    }

    /**
     * Constructs an instance with the specified Projection and mode.
     *
     * @param  mode   mode for display (MODE_3D, MODE_2Din3D)
     * @param  projection   map projection
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public MapProjectionDisplayJ3D(ProjectionImpl projection, int mode)
            throws VisADException, RemoteException {
        this((projection == null)
             ? (MapProjection) null
             : new ProjectionCoordinateSystem(projection), mode);
    }

    /**
     * Constructs an instance with the specified MapProjection
     * CoordinateSystem and mode.
     *
     * @param  mode   mode for display (MODE_3D, MODE_2Din3D)
     * @param  projection   map projection CS
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public MapProjectionDisplayJ3D(MapProjection projection, int mode)
            throws VisADException, RemoteException {
        this(projection, mode, false, null);
    }


    /**
     * ctor
     *
     * @param projection The projection to use
     * @param mode 3D/2D mode
     * @param offscreen Are we in offscreen mode
     * @param dimension Size of offscreen image
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public MapProjectionDisplayJ3D(MapProjection projection, int mode,
                                   boolean offscreen, Dimension dimension)
            throws VisADException, RemoteException {
        this(projection, mode, offscreen, dimension, null);
    }

    /**
     * ctor
     *
     * @param projection The projection to use
     * @param mode 3D/2D mode
     * @param offscreen Are we in offscreen mode
     * @param dimension Size of offscreen image
     * @param screen  Graphics device to create on
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public MapProjectionDisplayJ3D(MapProjection projection, int mode,
                                   boolean offscreen, Dimension dimension,
                                   GraphicsDevice screen)
            throws VisADException, RemoteException {

        if ( !((mode == MODE_3D) || (mode == MODE_2Din3D))) {
            throw new VisADException("Illegal mode: " + mode);
        }
        if (projection == null) {
            projection = makeDefaultMapProjection();
        }

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
        boolean useStereo = ((mode == MODE_3D)
                             && System.getProperty("idv.enableStereo",
                                 "false").equals("true"));
        GraphicsConfiguration config = ucar.visad.display.DisplayUtil.getPreferredConfig(screen, true,
                                           useStereo);
        DisplayRendererJ3D renderer = (mode == MODE_3D)
                                      ? (DisplayRendererJ3D) new DefaultDisplayRendererJ3D()
                                      : (DisplayRendererJ3D) new TwoDDisplayRendererJ3D();
        if (offscreen) {
            displayImpl = new DisplayImplJ3D("Navigated Display", renderer,
                                             dimension.width,
                                             dimension.height);
        } else {
            if (config == null) {
                LogUtil.userErrorMessage(
                    "Could not create a graphics configuration.\nPlease contact Unidata user support or see the FAQ");
                System.exit(1);
            }
            displayImpl = new DisplayImplJ3D("Navigated Display", renderer,
                                             api, config);
        }
        super.init(projection, displayImpl);
    }

    /**
     * Set up the display called by constructor
     *
     * @throws RemoteException
     * @throws VisADException
     */
    protected void initializeClass() throws VisADException, RemoteException {
        super.initializeClass();
        if (getDisplayMode() == MODE_2Din3D) {
            setBoxVisible(false);
        }
        try {
            DisplayRendererJ3D rend =
                (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
            rend.setRotateAboutCenter(true);

            canDoStereo = (getDisplayMode() == MODE_3D)
                          && rend.getCanvas().getStereoAvailable();
            //System.err.println(
            //    "MapProjectionDisplayJ3D:canDoStereo = " + canDoStereo);
            setPerspectiveView(canDoStereo);
	    checkClipDistance();
            setEyePosition(0.004);
            KeyboardBehaviorJ3D behavior = new KeyboardBehaviorJ3D(rend);
            rend.addKeyboardBehavior(behavior);
            setKeyboardBehavior(behavior);

            /*
            //Handle zooming in with the NavigatedDisplay.zoom method
            rend.addKeyboardBehavior(new KeyboardBehaviorJ3D(rend){
                    public void execFunction(int function) {
                        if(function == ZOOM_IN) {
                            zoom(1.5);
                            return;
                        }
                        super.execFunction(function);
                    }
                    });*/
            // Create a RubberBandBox
            RubberBandBox rubberBandBox = new RubberBandBox(RealType.XAxis,
                                              RealType.YAxis,
                                              InputEvent.SHIFT_MASK);
            rubberBandBox.addAction(new ActionImpl("RBB Action") {
                public void doAction()
                        throws VisADException, RemoteException {
                    RubberBandBox box = getRubberBandBox();
                    if ((box == null) || (box.getBounds() == null)) {
                        return;
                    }
                    setMapRegion(box.getBounds());
                }
            });
            setRubberBandBox(rubberBandBox);
            enableRubberBanding(true);
            setPolygonOffsetFactor(1);
        } catch (RemoteException e) {}  // ignore

    }

    //    private List keyboardBehaviors;

    /**
     * Add a keyboard behavior to this display
     *
     * @param behavior  behavior to add
     */
    public void addKeyboardBehavior(KeyboardBehavior behavior) {
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        KeyboardBehaviorWrapper3D beh = new KeyboardBehaviorWrapper3D(rend,
                                            behavior);
        //      keyboardBehaviors.add (beh);
        rend.addKeyboardBehavior(beh);
    }


    /**
     * Class KeyboardBehaviorWrapper3D.  Wrapper around a KeyboardBehaviorJ3D.
     *
     * @author Unidata development team
     */
    static class KeyboardBehaviorWrapper3D extends KeyboardBehaviorJ3D {

        /** the behavior */
        KeyboardBehavior behavior;

        /**
         * Create a new wrapper.
         *
         * @param rend       display renderer
         * @param behavior   behavior to wrap
         */
        public KeyboardBehaviorWrapper3D(DisplayRendererJ3D rend,
                                         KeyboardBehavior behavior) {
            super(rend);
            this.behavior = behavior;
        }

        /**
         * Map a key to a function
         *
         * @param function   function to map
         * @param keycode    key to invoke function
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
         * @param event   event to process
         */
        public void processKeyEvent(java.awt.event.KeyEvent event) {
            behavior.processKeyEvent(event);
        }

        /**
         * Wrapper  around KeyboardBehavior.execFunction.
         *
         * @param function  function to execute
         */
        public void execFunction(int function) {
            behavior.execFunction(function);
        }
    }

    /**
     * Enable clipping of data at the box edges.
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


    public View getView() {
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();
        return rend.getView();
    }



    /**
     * Set the view to perspective or parallel if this is a 3D display..
     *
     * @param perspectiveView  true for perspective view
     */
    public void setPerspectiveView(boolean perspectiveView) {

        if ( !(getDisplayMode() == MODE_3D)) {
            return;
        }
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
     * Get the view to perspective or parallel..
     *
     * @return true if perpsective view (MODE_3D only)
     */
    public boolean isPerspectiveView() {
        return (getDisplayMode() == MODE_3D)
               ? super.isPerspectiveView()
               : false;
    }



    /**
     * Set the view for 3D.  The views are based on the original display
     * as follows:
     * <pre>
     *                        NORTH
     *                      _________
     *                    W |       | E
     *                    E |       | A
     *                    S |       | S
     *                    T |_______| T
     *                        SOUTH
     * </pre>
     * @param  view  one of the static view fields (NORTH_VIEW, SOUTH_VIEW, ..
     *               etc).
     */
    public void setView(int view) {
        super.setView(view);
        try {
            ProjectionControlJ3D projControl =
                (ProjectionControlJ3D) getDisplay().getProjectionControl();
            double[] matrix;

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
    }

    /**
     * Determine if this MapDisplay can do stereo..
     *
     * @return true if the graphics device can do stereo
     */
    public boolean getStereoAvailable() {
        return canDoStereo;
    }

    /** default configuration */
    private static GraphicsConfiguration defaultConfig = makeConfig();

    /**
     * Make the default configuration
     *
     * @return the configuration with stereo preferred if possible.
     */
    private static GraphicsConfiguration makeConfig() {
        try {
            GraphicsEnvironment e =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice           d        = e.getDefaultScreenDevice();
            GraphicsConfigTemplate3D template =
                new GraphicsConfigTemplate3D();
            if (System.getProperty("idv.enableStereo",
                                   "false").equals("true")) {
                template.setStereo(GraphicsConfigTemplate3D.PREFERRED);
            }
            GraphicsConfiguration c = d.getBestConfiguration(template);
            return c;
        } catch (HeadlessException he) {
            return null;
        }
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



    public void setClipDistanceFront (double value) {
        super.setClipDistanceFront(value);
	checkClipDistance();
    }

    public void setClipDistanceBack (double value) {
        super.setClipDistanceBack(value);
	checkClipDistance();
    }


    public void checkClipDistance() {
	View view = getView();
	if(view==null) return;
	if (isPerspectiveView()) {
            view.setFrontClipDistance(CLIP_FRONT_PERSPECTIVE);
            view.setBackClipDistance(CLIP_BACK_PERSPECTIVE);
        } else {
	    view.setFrontClipDistance(getClipDistanceFront());
	    view.setBackClipDistance(getClipDistanceBack());
        }
    }


}

