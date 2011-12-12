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

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.j3d.Transform3D;
import javax.media.j3d.View;
import javax.swing.JToolBar;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.RubberBandBox;
import ucar.visad.display.ScalarMapSet;
import ucar.visad.quantities.GeopotentialAltitude;
import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.DisplayEvent;
import visad.DisplayImpl;
import visad.DisplayListener;
import visad.DisplayRealType;
import visad.DisplayRenderer;
import visad.MouseBehavior;
import visad.Real;
import visad.RealTuple;
import visad.RealType;
import visad.ScalarMap;
import visad.Unit;
import visad.VisADException;
import visad.VisADRay;
import visad.georef.EarthLocation;
import visad.georef.MapProjection;
import visad.java3d.DisplayRendererJ3D;

/**
 * Provides support for a navigated VisAD DisplayImplJ3D for
 * meteorological data.  <P>
 * Any displayable data must be able to map to RealType.Latitude,
 * RealType.Longitude and/or RealType.Altitude.
 *
 * @author Don Murray
 * @version $Revision: 1.100 $ $Date: 2007/04/24 14:00:56 $
 */
public abstract class NavigatedDisplay extends DisplayMaster {

    /** Bottom View */
    public static final int BOTTOM_VIEW = 0;

    /** Bottom View name */
    public static String BOTTOM_VIEW_NAME = "Bottom";

    /** default back clip value */
    public static double CLIP_BACK_DEFAULT = 1000.0;

    /** default back clip perspective */
    public static double CLIP_BACK_PERSPECTIVE = 10.0;

    /** default front clip value */
    public static double CLIP_FRONT_DEFAULT = -1000.0;

    /** default front clip perspective */
    public static double CLIP_FRONT_PERSPECTIVE = 0.1;

    /**
     * The name of the altitude property.
     */
    public static final String CURSOR_ALTITUDE = "cursorAltitude";

    /**
     * The name of the latitude property.
     */
    public static final String CURSOR_LATITUDE = "cursorLatitude";

    /**
     * The name of the longitude property.
     */
    public static final String CURSOR_LONGITUDE = "cursorLongitude";

    /** East View */
    public static final int EAST_VIEW = 2;

    /** East View name */
    public static String EAST_VIEW_NAME = "East";

    /** Field for a 2D mode in Java2D */
    public static final int MODE_2D = 1;

    /** Field for a 2D mode in Java3D */
    public static final int MODE_2Din3D = 2;

    /** Field for a 3D mode */
    public static final int MODE_3D = 0;

    /** North View */
    public static final int NORTH_VIEW = 1;

    /** North View name */
    public static String NORTH_VIEW_NAME = "North";

    /** South View */
    public static final int SOUTH_VIEW = 4;

    /** South View name */
    public static String SOUTH_VIEW_NAME = "South";

    /** Top View */
    public static final int TOP_VIEW = 3;

    /** Top View name */
    public static String TOP_VIEW_NAME = "Top";

    /** West View */
    public static final int WEST_VIEW = 5;

    /** West View name */
    public static String WEST_VIEW_NAME = "West";

    /** So we don't have more than one viewpoint animation running */
    int animationTimeStamp = 0;

    /** the back clip distance */
    private double clipDistanceBack = CLIP_BACK_DEFAULT;

    /** the front clip distance */
    private double clipDistanceFront = CLIP_FRONT_DEFAULT;

    /** flag for auto-rotation */
    private boolean autoRotate = false;

    /** rotation delay */
    private long rotateDelay = 50;

    /** A virtual timestamp for when we rotate */
    private int rotateTimeStamp = 0;

    /** rotate x flag */
    private double rotateX = 0;

    /** rotate y flag */
    private double rotateY = -1;

    /** rotate z flag */
    private double rotateZ = 0;

    /** flag for perspective view */
    private boolean isPerspective = true;

    /** flag for whether the animation readout is visible */
    private boolean animationVisible = true;

    /** displayable for rubberbanding */
    private RubberBandBox rubberBandBox = null;

    /** Lat / Lon Scale info */
    private LatLonScaleInfo latLonScaleInfo = new LatLonScaleInfo();

    /** isAnimating */
    private boolean isAnimating = false;

    /** flag for clipping */
    private boolean clipping = false;    // clipping not enabled by default

    /** flag for the visibility of the VisAD box */
    private boolean box;

    /**
     * The cursor altitude.
     * @serial
     */
    private volatile Real cursorAltitude;

    /**
     * The cursor latitude.
     * @serial
     */
    private volatile Real cursorLatitude;

    /**
     * The cursor longitude.
     * @serial
     */
    private volatile Real cursorLongitude;

    /** type for this display */
    private int myMode;

    /**
     * NavigatedDisplayToolBar associated with this display
     * @serial
     */
    private NavigatedDisplayToolBar navToolBar;

    /**
     * Default Constructor
     */
    protected NavigatedDisplay() {}

    /**
     * Construct a NavigatedDisplay with the specified VisAD display
     *
     * @param display  VisAD display for this NavigatedDisplay
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    protected NavigatedDisplay(DisplayImpl display) throws VisADException, RemoteException {
        init(display);
    }

    /**
     * Construct a NavigatedDisplay with the specified VisAD display
     *
     * @param display  VisAD display for this NavigatedDisplay
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException  remote error
     */
    protected void init(DisplayImpl display) throws VisADException, RemoteException {
        super.init(display, 1);

        DisplayRenderer displayRenderer = display.getDisplayRenderer();

        myMode = (displayRenderer instanceof visad.java2d.DisplayRendererJ2D)
                 ? MODE_2D
                 : (displayRenderer instanceof visad.java3d.TwoDDisplayRendererJ3D)
                   ? MODE_2Din3D
                   : MODE_3D;
        displayRenderer.setCursorStringOn(false);
    }

    /**
     * Set up the display.  Any additional work should be done in
     * a subclass's intializeClass() method, which should call
     * super.initializeClass() first.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     Unable to create the display
     */
    protected void initializeClass() throws VisADException, RemoteException {
        DisplayImpl display = (DisplayImpl) getDisplay();

        display.enableEvent(DisplayEvent.KEY_PRESSED);
        display.enableEvent(DisplayEvent.KEY_RELEASED);
        display.enableEvent(DisplayEvent.MOUSE_MOVED);
        display.enableEvent(DisplayEvent.MOUSE_DRAGGED);
        display.enableEvent(DisplayEvent.MOUSE_PRESSED);
        display.enableEvent(DisplayEvent.WAIT_ON);
        display.enableEvent(DisplayEvent.WAIT_OFF);
        addDisplayListener(new DisplayListener() {
            public void displayChanged(DisplayEvent event) {
                int id = event.getId();

                try {
                    if (id == event.MOUSE_PRESSED_CENTER) {
                        cursorMoved();
                    } else if (id == event.MOUSE_MOVED) {
                        pointerMoved(event.getX(), event.getY());
                    } else if (id == event.MOUSE_DRAGGED) {
                        int mods    = event.getInputEvent().getModifiers();
                        int button2 = (InputEvent.BUTTON1_MASK | InputEvent.BUTTON3_MASK);
                        int button3 = (InputEvent.BUTTON1_MASK | InputEvent.BUTTON2_MASK);

                        if (((mods & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK)
                                || ((mods & button2) == button2)) {
                            cursorMoved();
                        } else    // if ( ((mods & InputEvent.BUTTON3_MASK) ==

                        // InputEvent.BUTTON3_MASK) ||
                        // ((mods & button3) == button3)) {
                        {
                            pointerMoved(event.getX(), event.getY());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Set the GraphicsModeControl.polygonOffsetFactor
     *
     * @param offset offset value
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public void setPolygonOffsetFactor(int offset) throws VisADException, RemoteException {

        // System.err.println(getClass().getName() + ".setPolygonOffsetFactor:"
        // + offset);
        getDisplay().getGraphicsModeControl().setPolygonOffsetFactor(offset);
    }

    /**
     * Set the GraphicsModeControl.polygonOffsetFactor
     *
     * @param offset offset value
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public void setPolygonOffset(int offset) throws VisADException, RemoteException {

        // System.err.println(getClass().getName() + ".setPolygonOffset:"
        // + offset);
        getDisplay().getGraphicsModeControl().setPolygonOffset(offset);
    }

    /**
     * Set the rotation multiplier matrix
     *
     * @param rotx  x rotation
     * @param roty  y rotation
     * @param rotz  z rotation
     */
    public void setRotationMultiplierMatrix(double rotx, double roty, double rotz) {
        rotateX = rotx;
        rotateY = roty;
        rotateZ = rotz;
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

    /**
     * Convert the screen coordinates to visad coordinates at the given
     * depth.
     *
     * @param screenX Screen x coordinate
     * @param screenY Screen y coordinate
     * @return visad coordinates.
     */
    public double[] getSpatialCoordinatesFromScreen(int screenX, int screenY) {
        return getSpatialCoordinatesFromScreen(screenX, screenY, Double.NaN);
    }

    /**
     * Convert the screen coordinates to visad coordinates.
     *
     * @param screenX Screen x coordinate
     * @param screenY Screen y coordinate
     * @param zDepth  depth in the zbox
     * @return visad coordinates.
     */
    public double[] getSpatialCoordinatesFromScreen(int screenX, int screenY, double zDepth) {
        return getRayPositionAtZ(getRay(screenX, screenY), zDepth);
    }

    /**
     * Convert the screen coordinates to the direction
     *
     * @param screenX Screen x coordinate
     * @param screenY Screen y coordinate
     * @return direction vector
     */
    public double[] getRayDirection(int screenX, int screenY) {
        return getRay(screenX, screenY).vector;
    }

    /**
     * Convert the screen coordinates to the ray
     *
     * @param screenX Screen x coordinate
     * @param screenY Screen y coordinate
     * @return the ray
     */
    public VisADRay getRay(int screenX, int screenY) {
        return getDisplay().getDisplayRenderer().getMouseBehavior().findRay(screenX, screenY);
    }

    /**
     * Get the screen coordinates for the xyz location
     *
     * @param position   xyz location
     *
     * @return screen (x,y) coordinates
     */
    public int[] getScreenCoordinates(double[] position) {
        return getDisplay().getDisplayRenderer().getMouseBehavior().getScreenCoords(position);
    }

    /**
     * See if this is a 2D or 3D display.
     *
     * @return display mode for this display (MODE_3D, MODE_2D, MODE_2Din3D)
     */
    public int getDisplayMode() {
        return myMode;
    }

    /**
     * Toggle the cursor display readout on/off.  By default, the
     * display is toggled off at construction.  Lat/Lon/Altitude values
     * are displayed with the NavigatedDisplayCursorReadout component..
     *
     * @see ucar.unidata.view.geoloc.NavigatedDisplayCursorReadout
     *
     * @param on   true will display cursor position on the VisAD display
     *             (might be useful for debugging).
     */
    public void setCursorStringOn(boolean on) {
        ((DisplayRenderer) getDisplay().getDisplayRenderer()).setCursorStringOn(on);
    }

    /**
     * Toggle the animation string visibility..
     *
     * @param visible  true to make it visible
     * @deprecated use #setAnimationStringVisible(boolean)
     */
    public void setAnimationStringOn(boolean visible) {
        setAnimationStringVisible(visible);
    }

    /**
     * Return whether the animation string is visible or not.
     *
     * @return  true if visible
     * @deprecated use #getAnimationStringVisible()
     */
    public boolean getAnimationStringOn() {
        return getAnimationStringVisible();
    }

    /**
     * Accessor method for the DisplayLatitudeType (i.e., what
     * RealType.Latitude is mapped to)..
     *
     * @return  the DisplayRealType that RealType.Latitude is mapped to
     */
    public abstract DisplayRealType getDisplayLatitudeType();

    /**
     * Accessor method for the DisplayLongitudeType (i.e., what
     * RealType.Longitude is mapped to)..
     *
     * @return  the DisplayRealType that RealType.Longitude is mapped to
     */
    public abstract DisplayRealType getDisplayLongitudeType();

    /**
     * Accessor method for the DisplayAltitudeType (i.e., what
     * RealType.Altitude is mapped to)..
     *
     * @return  the DisplayRealType that RealType.Altitude is mapped to
     */
    public abstract DisplayRealType getDisplayAltitudeType();

    /**
     * Enable clipping of data at the box edges.  Work is done
     * in subclasses, but these should call super.enableClipping(clip)
     * at the end..
     *
     * @param  clip  true to turn clipping on, otherwise off
     */
    public void enableClipping(boolean clip) {
        clipping = clip;
    }

    /**
     * Check to see if clipping is enabled..
     *
     * @return  true if clipping on, otherwise false
     */
    public boolean isClippingEnabled() {
        return clipping;
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
     * Define the map projection using a MapProjection type CoordinateSystem.
     * Implementation will be subclass dependent.
     *
     * @param  mapProjection   map projection coordinate system
     *
     * @throws  VisADException         Couldn't create necessary VisAD object
     * @throws  RemoteException        Couldn't create a remote object
     */
    public abstract void setMapProjection(MapProjection mapProjection) throws VisADException, RemoteException;

    /**
     * Set the map area to be displayed in the box.  Subclasses should
     * implement this if they want this functionality.  This implementation
     * does nothing.
     *
     * @param mapArea  ProjectionRect describing the map area to be displayed.
     *
     * @throws  VisADException         invalid navigation or VisAD error
     * @throws  RemoteException        Couldn't create a remote object
     */
    public void setMapArea(ProjectionRect mapArea) throws VisADException, RemoteException {}

    /**
     * Accessor method for the altitude ScalarMap (i.e.,
     * (RealType.Altitude -> getDisplayAltitudeType).
     *
     * @return  the ScalarMap that Altitude is mapped to
     */
    protected abstract ScalarMap getAltitudeMap();

    /**
     * Method to add a new ScalarMap to the vertical coordinate (i.e.,
     * getDisplayAltitudeType is mapped to)..
     * Subclasses should override if they want to implement this.  This
     * implementation does nothing.
     *
     * @param verticalType RealType of the new vertical map
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   VisAD problem
     */
    public void addVerticalMap(RealType verticalType) throws VisADException, RemoteException {}

    /**
     * Method to remove a new ScalarMap to the vertical coordinate (i.e.,
     * getDisplayAltitudeType is mapped to)..
     * Subclasses should override if they want to implement this.  This
     * implementation does nothing.
     *
     * @param verticalType RealType of the new vertical map
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   VisAD problem
     */
    public void removeVerticalMap(RealType verticalType) throws VisADException, RemoteException {}

    /**
     * Set the view for 3D.  The views are subject to each subclass-s
     * implementation.
     * @param  view  one of the static view fields (NORTH_VIEW, SOUTH_VIEW, ..
     *               etc).
     */
    public abstract void setView(int view);

    /**
     * Set the view from the name of a view
     *
     * @param view  the view name
     */
    public void setView(String view) {
        view = view.toLowerCase();

        if (view.equals("bottom")) {
            setView(BOTTOM_VIEW);
        } else if (view.equals("north")) {
            setView(NORTH_VIEW);
        } else if (view.equals("east")) {
            setView(EAST_VIEW);
        } else if (view.equals("top")) {
            setView(TOP_VIEW);
        } else if (view.equals("south")) {
            setView(SOUTH_VIEW);
        } else if (view.equals("west")) {
            setView(WEST_VIEW);
        }
    }

    /**
     * Set the view to perspective or parallel if this is a 3D display..
     *
     * @param perspective  true for perspective view
     */
    public void setPerspectiveView(boolean perspective) {
        isPerspective = perspective;
    }

    /**
     * Get the view to perspective or parallel..
     *
     * @return true if perpsective view
     */
    public boolean isPerspectiveView() {
        return isPerspective;
    }

    /**
     * Set the RubberBandBox being used in this component.
     * To be used by subclasses that support rubberbanding.
     *
     * @param  box RubberBandBox to use
     */
    protected void setRubberBandBox(RubberBandBox box) {
        rubberBandBox = box;
    }

    /**
     * Retrieve the RubberBandBox being used in this component.
     *
     * @return  RubberBandBox being used. Return null if display doesn't
     *          support rubberbanding.
     */
    public RubberBandBox getRubberBandBox() {
        return rubberBandBox;
    }

    /**
     * Toggle the use of RubberBandBoxing.  Use the direct manipulation
     * mouse button (usually MB3) plus the CTRL key to draw the rubber
     * band box.  This will automagically translate and zoom the
     * display to the region selected.
     *
     * @param  on   true to enable rubberbanding (on by default);
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException   unable to toggle the rubber banding function
     */
    public void enableRubberBanding(boolean on) throws VisADException, RemoteException {
        RubberBandBox box = getRubberBandBox();

        if (box == null) {
            return;
        }

        if (on) {
            if (indexOf(box) == -1) {
                addDisplayable(box);
            }
        } else {
            boolean b = removeDisplayable(box);
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
        ScalarMap vertMap = getAltitudeMap();

        if ((vertMap != null) && (newUnit != null)) {
            if (!Misc.equals(getVerticalRangeUnit(), newUnit)) {
                vertMap.setOverrideUnit(newUnit);
            }
        }
    }

    /**
     * Get the Unit of the vertical range
     *
     * @return  unit of range
     */
    public Unit getVerticalRangeUnit() {
        ScalarMap vertMap = getAltitudeMap();

        return (vertMap == null)
               ? null
               : (vertMap.getOverrideUnit() == null)
                 ? ((RealType) vertMap.getScalar()).getDefaultUnit()
                 : vertMap.getOverrideUnit();
    }

    /**
     * Set the range of the vertical coordinate
     *
     * @param  min  minimum value for vertical axis
     * @param  max  maximum value for vertical axis
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     Unable to create the display
     */
    public void setVerticalRange(double min, double max) throws VisADException, RemoteException {
        ScalarMap vertMap = getAltitudeMap();

        if (vertMap != null) {
            double[] vertRange = getVerticalRange();

            if (!((vertRange[0] == min) && (vertRange[1] == max))) {
                vertMap.setRange(min, max);
            }
        }
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
               : new double[] { 0, 0 };
    }

    /**
     * Local implementation to set rubber band box color also
     *
     * @param color  color for foreground
     */
    public void setForeground(Color color) {
        try {
            if (getRubberBandBox() != null) {
                getRubberBandBox().setColor(color);
            }

            super.setForeground(color);
        } catch (VisADException excp) {
            ;
        } catch (RemoteException excp) {
            ;
        }
    }

    /**
     * Set the visibility of a surrounding box.  At construction, the
     * box is set to be invisible if mode = 2D.
     *
     * @param  on  true if the box should be visible
     *
     * @throws  VisADException    Couldn't create the necessary VisAD object
     * @throws  RemoteException   If there was a problem making this
     *                               change in a remote collaborative display.
     */
    public void setBoxVisible(boolean on) throws VisADException, RemoteException {
        getDisplay().getDisplayRenderer().setBoxOn(on);
        box = on;
    }

    /**
     * Get the box visibility.
     *
     * @return true if box is visible, otherwise false.
     */
    public boolean getBoxVisible() {
        return box;
    }

    /**
     * Set the visibility of the axis scales. Subclasses should override
     * this if they don't support scales.
     *
     * @param  on  true if the box should be visible
     *
     * @throws  VisADException    Couldn't create the necessary VisAD object
     * @throws  RemoteException   If there was a problem making this
     *                               change in a remote collaborative display.
     */
    public void setScalesVisible(boolean on) throws VisADException, RemoteException {
        getDisplay().getGraphicsModeControl().setScaleEnable(on);
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
     * Get the center lat/lon/alt of the projection.
     *
     * @return center location
     */
    public EarthLocation getCenterPoint() {
        return getEarthLocation(new double[] { 0, 0, 0 });
    }

    /**
     * Get the EarthLocation of a point in XYZ space
     *
     * @param  xyz  RealTuple with MathType
     *              RealTupleType.SpatialCartesian3DTuple)
     *
     * @return point in lat/lon/alt space.
     */
    public EarthLocation getEarthLocation(RealTuple xyz) {
        EarthLocation el = null;

        try {
            el = getEarthLocation(new double[] { ((Real) xyz.getComponent(0)).getValue(),
                    ((Real) xyz.getComponent(1)).getValue(), ((Real) xyz.getComponent(2)).getValue() });
        } catch (VisADException e) {
            e.printStackTrace();
        }    // can't happen
                catch (RemoteException e) {
            e.printStackTrace();
        }    // can't happen

        return el;
    }

    /**
     * Get the EarthLocation of a point in XYZ space
     *
     * @param  xyz  double[3] of x,y,z coords.
     *
     * @return point in lat/lon/alt space.
     */
    public EarthLocation getEarthLocation(double[] xyz) {
        return getEarthLocation(xyz[0], xyz[1], xyz[2]);
    }

    /**
     * Get the EarthLocation of a point in XYZ space
     *
     * @param  x  x coord.
     * @param  y  y coord.
     * @param  z  z coord.
     *
     * @return point in lat/lon/alt space.
     */
    public EarthLocation getEarthLocation(double x, double y, double z) {
        return getEarthLocation(x, y, z, true);
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
    public abstract EarthLocation getEarthLocation(double x, double y, double z, boolean setZToZeroIfOverhead);

    /**
     * Returns the spatial (XYZ) coordinates of the particular EarthLocation
     *
     * @param el   earth location to translate
     *
     * @return  RealTuple of display coordinates.
     */
    public abstract RealTuple getSpatialCoordinates(EarthLocation el);

    /**
     * Returns the spatial (XYZ) coordinates of the particular EarthLocation
     *
     * @param el   earth location to translate
     * @param xyz  buffer to put value in
     *
     * @return  xyz
     *
     * @throws  RemoteException   If there was a problem making this
     *                            change in a remote collaborative display.
     * @throws  VisADException    Couldn't create the necessary VisAD object
     */
    public double[] getSpatialCoordinates(EarthLocation el, double[] xyz) throws VisADException, RemoteException {
        return getSpatialCoordinates(el, xyz, el.getAltitude().getValue(CommonUnit.meter));
    }

    /**
     * Returns the spatial (XYZ) coordinates of the particular EarthLocation
     *
     * @param el   earth location to translate
     * @param xyz  buffer to put value in
     * @param altitude the altitude
     *
     * @return  xyz
     *
     * @throws  RemoteException   If there was a problem making this
     *                            change in a remote collaborative display.
     * @throws  VisADException    Couldn't create the necessary VisAD object
     */
    public abstract double[] getSpatialCoordinates(EarthLocation el, double[] xyz, double altitude)
            throws VisADException, RemoteException;

    /**
     * Return the real altitude from a ZAxis (or displayAltitudeType) value
     *
     * @param map      map for scaling
     * @param value    value to scale
     *
     * @return scaled value
     */
    protected float getScaledValue(ScalarMap map, float value) {
        return (map != null)
               ? map.inverseScaleValues(new float[] { value }, false)[0]
               : 0.f;
    }

    /**
     * Sets the cursor latitude property.  Called by subclasses.
     *
     * @param latitude          The cursor latitude.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void setCursorLatitude(Real latitude) throws VisADException, RemoteException {
        Real oldLatitude = cursorLatitude;

        cursorLatitude = latitude;
        firePropertyChange(CURSOR_LATITUDE, oldLatitude, cursorLatitude);
    }

    /**
     * Gets the cursor latitude property.
     *
     * @return                  The currently-selected latitude.  May be
     *                          <code>null</code>.
     */
    public Real getCursorLatitude() {
        return cursorLatitude;
    }

    /**
     * Sets the cursor longitude property.  Called by subclasses.
     *
     * @param longitude          The cursor longitude.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void setCursorLongitude(Real longitude) throws VisADException, RemoteException {
        Real oldLongitude = cursorLongitude;

        cursorLongitude = longitude;
        firePropertyChange(CURSOR_LONGITUDE, oldLongitude, cursorLongitude);
    }

    /**
     * Gets the cursor longitude property.
     *
     * @return                  The currently-selected longitude.  May be
     *                          <code>null</code>.
     */
    public Real getCursorLongitude() {
        return cursorLongitude;
    }

    /**
     * Sets the cursor altitude property.  Called by subclasses.
     *
     * @param altitude          The cursor altitude.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void setCursorAltitude(Real altitude) throws VisADException, RemoteException {
        Real oldAltitude = cursorAltitude;

        cursorAltitude = altitude;
        firePropertyChange(CURSOR_ALTITUDE, oldAltitude, cursorAltitude);
    }

    /**
     * Gets the cursor altitude property.
     *
     * @return                  The currently-selected altitude.  May be
     *                          <code>null</code>.
     */
    public Real getCursorAltitude() {
        return cursorAltitude;
    }

    /**
     * See if this display is animating.
     *
     * @return true if animating
     */
    public boolean getIsAnimating() {
        return isAnimating;
    }

    /**
     * Animate the matrix
     *
     * @param to  matrix to animate to
     */
    public void animateMatrix(double[] to) {
        animateMatrix(++animationTimeStamp, getProjectionMatrix(), to, null);
    }

    /**
     * Animate matrix
     *
     * @param to  where to go
     * @param sleepTime  sleep time
     */
    public void animateMatrix(double[] to, long sleepTime) {
        animateMatrix(++animationTimeStamp, getProjectionMatrix(), to, null, sleepTime);
    }

    /**
     * Animate the matrix changes. Go through N steps, and set the projection matrix
     * to be step/N percent between the from and to values.
     *
     * @param myTimeStamp So we only have one running
     * @param from The original matrix
     * @param to The dest matrix
     * @param finalLocation   final location to animate to
     */
    public void animateMatrix(int myTimeStamp, double[] from, double[] to, EarthLocation finalLocation) {
        animateMatrix(myTimeStamp, from, to, finalLocation, 50);
    }

    /**
     * Apply the rotation
     *
     * @param p  the point to rotate to
     */
    public void applyRotation(Point3d p) {
        double[]      trans         = { 0.0, 0.0, 0.0 };
        double[]      rot           = { 0.0, 0.0, 0.0 };
        double[]      scale         = { 0.0, 0.0, 0.0 };
        MouseBehavior mouseBehavior = getMouseBehavior();

        mouseBehavior.instance_unmake_matrix(rot, scale, trans, getProjectionMatrix());

        double[]    t         = mouseBehavior.make_matrix(rot[0], rot[1], rot[2], 1, 1, 1, 0, 0, 0);
        Transform3D transform = new Transform3D(t);

        transform.invert();
        transform.transform(p);
    }

    /**
     * Animate the matrix
     *
     * @param myTimeStamp  my time
     * @param from  where we came from
     * @param to  where we are going to
     * @param finalLocation  the final location
     * @param sleepTime  sleepy time
     */
    public void animateMatrix(int myTimeStamp, double[] from, double[] to, EarthLocation finalLocation,
                              long sleepTime) {
        double[] lastGoodMatrix = from;

        try {
            setAutoRotate(false);

            double[] finalMatrix = to;
            double[] tmp         = new double[from.length];
            int      numSteps    = 20;

            isAnimating = true;

            double[]      lastMatrix    = null;
            double[]      trans1        = { 0.0, 0.0, 0.0 };
            double[]      rot1          = { 0.0, 0.0, 0.0 };
            double[]      scale1        = { 0.0, 0.0, 0.0 };
            double[]      trans2        = { 0.0, 0.0, 0.0 };
            double[]      rot2          = { 0.0, 0.0, 0.0 };
            double[]      scale2        = { 0.0, 0.0, 0.0 };
            double[]      trans         = { 0.0, 0.0, 0.0 };
            double[]      rot           = { 0.0, 0.0, 0.0 };
            double[]      scale         = { 0.0, 0.0, 0.0 };
            MouseBehavior mouseBehavior = getMouseBehavior();

            mouseBehavior.instance_unmake_matrix(rot1, scale1, trans1, from);
            mouseBehavior.instance_unmake_matrix(rot2, scale2, trans2, to);

            Transform3D        t1          = new Transform3D(from);
            Transform3D        t2          = new Transform3D(to);
            java.awt.Rectangle bounds      = getScreenBounds();
            int[]              ptsx        = { 0, bounds.width / 2, bounds.width };
            int[]              ptsy        = { 0, bounds.height / 2, bounds.height };
            double             maxDistance = 0;

            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    double[] screenLoc = getSpatialCoordinatesFromScreen(ptsx[i], ptsx[j]);
                    Point3d  p1        = new Point3d(screenLoc[0], screenLoc[1], screenLoc[2]);
                    Point3d  p2        = new Point3d(screenLoc[0], screenLoc[1], screenLoc[2]);

                    t1.transform(p1);
                    t2.transform(p2);

                    double distance = p1.distance(p2);

                    if (distance > maxDistance) {
                        maxDistance = distance;
                    }
                }
            }

            // maxDistance = maxDistance/2;
            // System.err.print("d:" + maxDistance);
            // maxDistance *= 2;
            maxDistance = Math.min(maxDistance, 10);

            if (maxDistance > 0) {
                numSteps = (int) (numSteps * maxDistance) + 1;
            }

            numSteps = Math.min(numSteps, 100);

            // System.err.println("     numSteps:" + numSteps);

            Quat4d q1 = new Quat4d();
            Quat4d q2 = new Quat4d();

            t1.get(q1);
            t2.get(q2);

            boolean ask = true;

            for (int step = 1; step <= numSteps; step++) {
                if (myTimeStamp != animationTimeStamp) {
                    isAnimating = false;

                    return;
                }

                if (step == numSteps) {
                    try {
                        setProjectionMatrix(finalMatrix);
                    } catch (Exception exc) {

                        // In case we have a bad affine transform
                        setProjectionMatrix(lastGoodMatrix);
                    }

                    isAnimating = false;

                    if (finalLocation != null) {

                        // xxxxx
                        center(finalLocation);
                    }

                    return;
                }

                double percent = ((double) step) / numSteps;

                isAnimating = true;

                Quat4d q3 = new Quat4d();

                q3.interpolate(q1, q2, percent);

                Transform3D t3 = new Transform3D();

                t3.set(q3);

                double[] tmpRot = new double[16];

                t3.get(tmpRot);
                getMouseBehavior().instance_unmake_matrix(rot, scale, trans, tmpRot);
                tmp = mouseBehavior.make_matrix(rot[0], rot[1], rot[2],

                // 0,0,0,
                interp(scale1, scale2, 0, percent), interp(scale1, scale2, 1, percent),
                        interp(scale1, scale2, 2, percent), interp(trans1, trans2, 0, percent),
                        interp(trans1, trans2, 1, percent), interp(trans1, trans2, 2, percent));

                double[] currentMatrix = getProjectionMatrix();

                if ((lastMatrix != null) &&!Misc.arraysEquals(lastMatrix, currentMatrix)) {
                    isAnimating = false;

                    return;
                }

                boolean ok = true;

                try {
                    Transform3D testt = new Transform3D(tmp);

                    testt.invert();
                } catch (Exception exc) {
                    setProjectionMatrix(lastGoodMatrix);

                    return;

                    // ok = false;
                    // System.err.println ("bad:" + exc);
                }

                /*
                 * double[] center=       getSpatialCoordinatesFromScreen(bounds.width/2, bounds.height/2);
                 * Transform3D centerTransform = new Transform3D(tmp);
                 * Point3d centerPoint = new Point3d(center[0],center[1],center[2]);
                 * centerTransform.transform(centerPoint);
                 * double dx = center[0]-centerPoint.x;
                 * double dy = center[1]-centerPoint.y;
                 * double []centerM = getMouseBehavior().make_translate(dx,dy);
                 * System.err.println ("   dx:" + dx + "  center:" + center[0] + "   centerPoint:" + centerPoint.x);
                 */
                if (ok) {
                    try {

                        // tmp =   getMouseBehavior().multiply_matrix(centerM, tmp);
                        setProjectionMatrix(tmp);
                        lastGoodMatrix = tmp;
                    } catch (Exception exc) {
                        System.err.println("EXC:" + exc);

                        // In case we have a bad affine transform
                        setProjectionMatrix(lastGoodMatrix);
                    }
                }

                lastMatrix = getProjectionMatrix();
                Misc.sleep(sleepTime);
            }
        } catch (Exception exp) {
            System.out.println("Error  animating matrix:" + exp);
        }
    }

    /**
     * Interpolate to a percentage between two values
     *
     * @param d1  first
     * @param d2  second
     * @param percent  the percentage between the two
     *
     * @return  the value
     */
    private double interp(double d1, double d2, double percent) {
        return d1 + percent * (d2 - d1);
    }

    /**
     * Interpolate between two vectors
     *
     * @param a1  vector 1
     * @param a2  vector 2
     * @param index  the index in the vector
     * @param percent  the percentage between
     *
     * @return the value
     */
    private double interp(Vector3d a1, Vector3d a2, int index, double percent) {
        if (index == 0) {
            return a1.x + percent * (a2.x - a1.x);
        }

        if (index == 1) {
            return a1.y + percent * (a2.y - a1.y);
        }

        return a1.z + percent * (a2.z - a1.z);
    }

    /**
     * Interpolate between one vector two points
     *
     * @param a1  point 1
     * @param a2  point 2
     * @param index  the index in the points
     * @param percent  the percent to interpolate
     *
     * @return the interpolated value
     */
    private double interp(double[] a1, double[] a2, int index, double percent) {
        return a1[index] + percent * (a2[index] - a1[index]);
    }

    /**
     * Get the x/y position of the center of the screen
     *
     * @return x/y of screen center
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double[] getScreenCenter() throws VisADException, RemoteException {
        java.awt.Rectangle screenBounds = getScreenBounds();

        return getSpatialCoordinatesFromScreen(screenBounds.width / 2, screenBounds.height / 2);
    }

    /**
     * Get the screen coordinates
     *
     * @return  screen coords
     *
     * @throws RemoteException Java RMI issue
     * @throws VisADException  VisAD issue
     */
    public List<TwoFacedObject> getScreenCoordinates() throws VisADException, RemoteException {
        List<TwoFacedObject> l = new ArrayList<TwoFacedObject>();

        l.add(new TwoFacedObject("Center", getScreenCenter()));
        l.add(new TwoFacedObject("Upper Left", getScreenUpperLeft()));
        l.add(new TwoFacedObject("Upper Right", getScreenUpperRight()));
        l.add(new TwoFacedObject("Lower Left", getScreenLowerLeft()));
        l.add(new TwoFacedObject("Lower Right", getScreenLowerRight()));

        return l;
    }

    /**
     * Get the x/y position of the left/center of the screen
     *
     * @return x/y of screen left
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double[] getScreenUpperLeft() throws VisADException, RemoteException {
        return getSpatialCoordinatesFromScreen(0, 0);
    }

    /**
     * Get the x/y position of the right/center of the screen
     *
     * @return x/y of screen right
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double[] getScreenUpperRight() throws VisADException, RemoteException {
        java.awt.Rectangle screenBounds = getScreenBounds();

        return getSpatialCoordinatesFromScreen(screenBounds.width, 0);
    }

    /**
     * Get the x/y position of the right/center of the screen
     *
     * @return x/y of screen right
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double[] getScreenLowerLeft() throws VisADException, RemoteException {
        java.awt.Rectangle screenBounds = getScreenBounds();

        return getSpatialCoordinatesFromScreen(0, screenBounds.height);
    }

    /**
     * Get the x/y position of the right/center of the screen
     *
     * @return x/y of screen right
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double[] getScreenLowerRight() throws VisADException, RemoteException {
        java.awt.Rectangle screenBounds = getScreenBounds();

        return getSpatialCoordinatesFromScreen(screenBounds.width, screenBounds.height);
    }

    /**
     * Get the latlon box of the displayed area
     *
     * @return lat lon box  or null if it can't be determined
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Rectangle2D.Double getLatLonBox() throws VisADException, RemoteException {
        return getLatLonBox(true, true);
    }

    /**
     * Get the latlon box of the displayed area
     *
     * @param padSamples  should we pad some
     * @param normalizeLon  true to normalize longitudes to -180 to 180
     *
     * @return lat lon box  or null if it can't be determined
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Rectangle2D.Double getLatLonBox(boolean padSamples, boolean normalizeLon)
            throws VisADException, RemoteException {
        java.awt.Rectangle b   = getScreenBounds();
        double             pad = (padSamples
                                  ? 0.1
                                  : 0.0);
        double[]           xs;
        double[]           ys;

        xs = new double[] {
            b.width * -pad, b.width * 0.0, b.width * 0.1, b.width * 0.2, b.width * 0.3, b.width * 0.4, b.width * 0.5,
            b.width * 0.6, b.width * 0.7, b.width * 0.8, b.width * 0.9, b.width * 1.0, b.width * (1 + pad)
        };
        ys = new double[] {
            0 - b.height * pad, 0, b.height * 0.2, b.height * 0.4, b.height * 0.6, b.height * 0.8, b.height * 1,
            b.height + b.height * pad
        };

        double[] rangeX = { Double.NaN, Double.NaN };
        double[] rangeY = { Double.NaN, Double.NaN };

        for (int yidx = 0; yidx < ys.length; yidx++) {
            for (int xidx = 0; xidx < xs.length; xidx++) {
                findMinMaxFromScreen((int) xs[xidx], (int) ys[yidx], rangeX, rangeY, normalizeLon);
            }
        }

        for (int xidx = 0; xidx < 100; xidx++) {
            double percent = xidx / 100.0;

            findMinMaxFromScreen((int) (b.width * percent), b.height / 2, rangeX, rangeY, normalizeLon);
        }

        double left   = rangeX[0];
        double right  = rangeX[1];
        double top    = rangeY[1];
        double bottom = rangeY[0];

        if (left > right) {
            double tmp = left;

            left  = right;
            right = tmp;
        }

        if (top < bottom) {
            double tmp = top;

            top    = bottom;
            bottom = top;
        }

        double width  = right - left;
        double height = top - bottom;

        // A little fudge factor
        left  = left - width * 0.02;
        right = right + width * 0.02;

        // System.err.println ("left:" + left);
        // System.err.println ("right:" + right);
        return new Rectangle2D.Double(left, bottom, right - left, top - bottom);
    }

    /**
     * Translate from screen to earth coordinates
     *
     * @param x  screen x
     * @param y  screen y
     *
     * @return  the corresponding earth location
     *
     * @throws VisADException problem accessing data
     */
    public EarthLocation screenToEarthLocation(int x, int y) throws VisADException {
        double[] pt = getSpatialCoordinatesFromScreen(x, y, -1);

        return getEarthLocation(pt);
    }

    /**
     * Find min max values from the screen
     *
     * @param x x position
     * @param y y position
     * @param rangeX X range
     * @param rangeY Y range
     * @param normalizeLon  normalize longitudes to -180 to 180
     *
     * @throws VisADException problem accessing screen
     */
    private void findMinMaxFromScreen(int x, int y, double[] rangeX, double[] rangeY, boolean normalizeLon)
            throws VisADException {
        EarthLocation el   = screenToEarthLocation(x, y);
        double        tmpx = el.getLongitude().getValue(CommonUnit.degree);

        if (tmpx != tmpx) {
            return;
        }

        double tmpy = el.getLatitude().getValue(CommonUnit.degree);

        if (tmpy != tmpy) {
            return;
        }

        if (normalizeLon) {
            tmpx = LatLonPointImpl.lonNormal(tmpx);
        }

        if ((rangeX[0] != rangeX[0]) || (tmpx < rangeX[0])) {
            rangeX[0] = tmpx;
        }

        if ((rangeX[1] != rangeX[1]) || (tmpx > rangeX[1])) {
            rangeX[1] = tmpx;
        }

        if ((rangeY[0] != rangeY[0]) || (tmpy < rangeY[0])) {
            rangeY[0] = tmpy;
        }

        if ((rangeY[1] != rangeY[1]) || (tmpy > rangeY[1])) {
            rangeY[1] = tmpy;
        }
    }

    /**
     * Get the latlon box of the displayed area
     *
     * @return lat lon box  or null if it can't be determined
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public LatLonRect getLatLonRect() throws VisADException, RemoteException {
        java.awt.Rectangle b      = getScreenBounds();
        double[]           xs     = {
            0 - b.width * 0.10, 0, b.width * 0.25, b.width * 0.5, b.width * 1, b.width + b.width * 0.10
        };
        double[]           ys     = {
            0 - b.height * 0.10, 0, b.height * 0.25, b.height * 0.5, b.height * 1, b.height + b.height * 0.10
        };
        double             left   = 180;
        double             right  = -180;
        double             top    = -90;
        double             bottom = 90;
        double[]           tpt    = getSpatialCoordinatesFromScreen(0, 0, -1);

        // System.err.println ("tpt:" + tpt[0] + " " + tpt[1]);

        for (int yidx = 0; yidx < ys.length; yidx++) {
            for (int xidx = 0; xidx < xs.length; xidx++) {
                double[] pt = getSpatialCoordinatesFromScreen((int) xs[xidx], (int) ys[yidx], -1);

                // System.err.println("pt["+xs[xidx]+","+ys[yidx]+"]: " + pt[0] + ", " + pt[1] + ", " + pt[2]);
                EarthLocation el   = getEarthLocation(pt);
                double        tmpx = el.getLongitude().getValue(CommonUnit.degree);

                if (Double.isNaN(tmpx)) {
                    continue;
                }

                double tmpy = el.getLatitude().getValue(CommonUnit.degree);

                if (Double.isNaN(tmpy)) {
                    continue;
                }

                if ((yidx == 0) || (tmpx < left)) {
                    left = tmpx;
                }

                if ((yidx == 0) || (tmpx > right)) {
                    right = tmpx;
                }

                if ((yidx == 0) || (tmpy < bottom)) {
                    bottom = tmpy;
                }

                if ((yidx == 0) || (tmpy > top)) {
                    top = tmpy;
                }
            }
        }

        if (left > right) {
            double tmp = left;

            left  = right;
            right = tmp;
        }

        if (top < bottom) {
            double tmp = top;

            top    = bottom;
            bottom = top;
        }

        LatLonPointImpl llp1 = new LatLonPointImpl(bottom, left);
        LatLonPointImpl llp2 = new LatLonPointImpl(top, right);

        return new LatLonRect(llp1, llp2);
    }

    /**
     * Get the visad box of the displayed area
     *
     * @return visad box
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Rectangle2D.Double getVisadBox() throws VisADException, RemoteException {
        java.awt.Rectangle b      = getScreenBounds();
        double[]           xs     = { 0, b.width * 0.25, b.width * 0.5, b.width * 1 };
        double[]           ys     = { 0, b.height * 0.25, b.height * 0.5, b.height * 1 };
        double             left   = 0;
        double             right  = 0;
        double             top    = 0;
        double             bottom = 0;

        for (int yidx = 0; yidx < ys.length; yidx++) {
            for (int xidx = 0; xidx < xs.length; xidx++) {
                double[] pt = getSpatialCoordinatesFromScreen((int) xs[xidx], (int) ys[yidx], -1);

                // if(xs[xidx]==0)
                // System.err.println("  x/y:" + xs[xidx] +"/" + ys[yidx] +"  pt:" + pt[0]+"/" + pt[1] +"/" + pt[2]);

                if (pt[0] == pt[0]) {
                    if ((yidx == 0) || (pt[0] < left)) {
                        left = pt[0];
                    }

                    if ((yidx == 0) || (pt[0] > right)) {
                        right = pt[0];
                    }
                }

                if (pt[1] == pt[1]) {
                    if ((yidx == 0) || (pt[1] < bottom)) {
                        bottom = pt[1];
                    }

                    if ((yidx == 0) || (pt[1] > top)) {
                        top = pt[1];
                    }
                }
            }
        }

        if (left > right) {
            double tmp = left;

            left  = right;
            right = tmp;
        }

        if (top < bottom) {
            double tmp = top;

            top    = bottom;
            bottom = top;
        }

        left   = Math.max(left, -180);
        right  = Math.min(right, 180);
        top    = Math.min(top, 90);
        bottom = Math.max(bottom, -90);

        Rectangle2D.Double box = new Rectangle2D.Double(left, bottom, right - left, top - bottom);

        return box;
    }

    /**
     * Center to x y
     *
     * @param  x   X
     * @param  y   Y
     */
    public void center(double x, double y) {
        center(x, y, false);
    }

    /**
     * Center to x y
     *
     * @param  x   X
     * @param  y   Y
     * @param animated Should animate the move
     */
    public void center(double x, double y, boolean animated) {
        java.awt.Rectangle screenBounds = getScreenBounds();

        // NANS?
        if ((x != x) || (y != y)) {
            return;
        }

        moveToScreen(x, y, (int) (screenBounds.getWidth() / 2), (int) (screenBounds.getHeight() / 2), animated);
    }

    /**
     * Move the x/y point to the x/y point of the the given screen coords
     *
     * @param x x
     * @param y y
     * @param sx screen x
     * @param sy screen y
     * @param times How many time should we iterate on the move
     * @deprecated dropped the times parameter.
     */
    public void moveToScreen(double x, double y, int sx, int sy, int times) {
        moveToScreen(x, y, sx, sy);
    }

    /**
     * Move the x/y point to the x/y point of the the given screen coords
     *
     * @param x x
     * @param y y
     * @param sx screen x
     * @param sy screen y
     */
    public void moveToScreen(double x, double y, int sx, int sy) {
        moveToScreen(x, y, sx, sy, false);
    }

    /**
     * Move the x/y point to the x/y point of the the given screen coords
     *
     * @param x x
     * @param y y
     * @param sx screen x
     * @param sy screen y
     * @param animated Animate the move
     */
    public void moveToScreen(double x, double y, int sx, int sy, boolean animated) {
        try {
            double[] currentMatrix = getProjectionMatrix();
            double[] trans         = { 0.0, 0.0, 0.0 };
            double[] rot           = { 0.0, 0.0, 0.0 };
            double[] scale         = { 0.0, 0.0, 0.0 };
            double[] centerXY      = getSpatialCoordinatesFromScreen(sx, sy);

            getMouseBehavior().instance_unmake_matrix(rot, scale, trans, currentMatrix);

            double[] translateMatrix = getMouseBehavior().make_translate(scale[0] * (centerXY[0] - x),
                                           scale[1] * (centerXY[1] - y));

            currentMatrix = getMouseBehavior().multiply_matrix(translateMatrix, currentMatrix);

            if (!animated) {
                setProjectionMatrix(currentMatrix);
            } else {
                final double[] to = currentMatrix;

                Misc.run(new Runnable() {
                    public void run() {
                        animateMatrix(++animationTimeStamp, getProjectionMatrix(), to, null);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the View
     *
     * @return the View
     */
    public View getView() {
        DisplayRendererJ3D rend = (DisplayRendererJ3D) getDisplay().getDisplayRenderer();

        return rend.getView();
    }

    /**
     * Move the center to the given earth location
     *
     * @param el el to center on
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(EarthLocation el) throws VisADException, RemoteException {
        center(el, false);
    }

    /**
     * Move the center to the given earth location
     *
     * @param el el to center on
     * @param animated animate the move
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void center(final EarthLocation el, boolean animated) throws VisADException, RemoteException {
        centerAndZoom(el, animated, 1.0);
    }

    /**
     * Move the center to the given earth location and zoom in
     *
     * @param el el to center on
     * @param animated animate the move
     * @param zoomFactor   factor to zoom
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void centerAndZoom(final EarthLocation el, boolean animated, double zoomFactor)
            throws VisADException, RemoteException {
        centerAndZoom(el, null, zoomFactor, animated, true);
    }

    /**
     * Move the center to the given earth location and zoom in
     *
     * @param el el to center on
     * @param altitude  the altitude of the point
     * @param animated animate the move
     * @param zoomFactor   factor to zoom
     * @param northUp  put north up
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void centerAndZoom(final EarthLocation el, Real altitude, double zoomFactor, boolean animated,
                              boolean northUp)
            throws VisADException, RemoteException {
        if ((zoomFactor == 0) || (zoomFactor != zoomFactor)) {
            zoomFactor = 1.0;
        }

        if (el.getLongitude().isMissing() || el.getLatitude().isMissing()) {
            return;
        }

        // Misc.printStack("centerAndZoom",10,null);
        try {
            java.awt.Rectangle screenBounds  = getScreenBounds();
            MouseBehavior      mouseBehavior = getMouseBehavior();
            double[]           currentMatrix = getProjectionMatrix();
            double[]           trans         = { 0.0, 0.0, 0.0 };
            double[]           rot           = { 0.0, 0.0, 0.0 };
            double[]           scale         = { 0.0 };
            double[]           xy            = getSpatialCoordinates(el, null);
            double[]           centerXY      = getSpatialCoordinatesFromScreen(screenBounds.width / 2,
                                                   screenBounds.height / 2);

            mouseBehavior.instance_unmake_matrix(rot, scale, trans, currentMatrix);

            double[] translateMatrix = mouseBehavior.make_translate(scale[0] * (centerXY[0] - xy[0]),
                                           scale[0] * (centerXY[1] - xy[1]));

            currentMatrix = mouseBehavior.multiply_matrix(translateMatrix, currentMatrix);

            if (!animated) {
                setProjectionMatrix(currentMatrix);
                zoom(zoomFactor);
            } else {
                double[] scaleMatrix = mouseBehavior.make_matrix(0.0, 0.0, 0.0, zoomFactor, 0.0, 0.0, 0.0);

                currentMatrix = mouseBehavior.multiply_matrix(scaleMatrix, currentMatrix);

                final double[] to = currentMatrix;

                Misc.run(new Runnable() {
                    public void run() {
                        animateMatrix(++animationTimeStamp, getProjectionMatrix(), to, el);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the rotation matrix
     *
     * @return rotation matrix
     */
    public double[] getRotation() {
        double[] currentMatrix = getProjectionMatrix();
        double[] trans         = { 0.0, 0.0, 0.0 };
        double[] rot           = { 0.0, 0.0, 0.0 };
        double[] scale         = { 0.0, 0.0, 0.0 };

        getMouseBehavior().instance_unmake_matrix(rot, scale, trans, currentMatrix);

        return rot;
    }

    /**
     * Change point of view of a 3D VisAD display, using input angles
     * (unit = degree): For example, a view from the
     * southwest has azimuth of 225 and decAngle say 20 to 70 or so.
     * Preserves initial scaling and aspect ratios.
     *
     * @param azimuth  azimuth from "north," clockwise, 0 to 360
     * @param decAngle tilt angle down from upward vertical. 0-180
     */
    public void rotateView(double azimuth, double decAngle) {
        rotateView(getProjectionMatrix(), azimuth, decAngle);
    }

    /**
     * Change point of view of a 3D display from the matrix supplied,
     * using input angles (unit = degree): For example, a view from the
     * southwest has azimuth of 225 and decAngle say 20 to 70 or so.
     *
     * @param matrix   matrix to rotate from
     * @param azimuth  azimuth from "north," clockwise, 0 to 360
     * @param decAngle tilt angle down from upward vertical. 0-180
     */
    public void rotateView(double[] matrix, double azimuth, double decAngle) {
        if (getDisplayMode() != MODE_3D) {
            return;
        }

        // trap input bad values - not necessary since trig
        // functions handle values outside of 0-360 properly.
        // rotation around z axis, made from user's "azimuth"
        double zAngle = 180.0 - azimuth;

        try {
            double[] origMatrix = (matrix == null)
                                  ? getProjectionMatrix()
                                  : matrix;

            // rotate in z
            double[] aziMatrix = getDisplay().make_matrix(0.0, 0.0, zAngle, 1.0, 0.0, 0.0, 0.0);
            double[] combo     = getDisplay().multiply_matrix(aziMatrix, origMatrix);

            // rotate in x
            double[] decMatrix = getDisplay().make_matrix(decAngle, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0);

            // total rotation matrix is computed and applied
            double[] combo2 = getDisplay().multiply_matrix(decMatrix, combo);

            setProjectionMatrix(combo2);
        } catch (Exception exp) {
            System.out.println("  rotate view got " + exp);
        }
    }

    /**
     * Return a toolbar that can be used to move around in the display
     * (zoom, pan, reset).
     *
     * @return  toolbar for navigating around the display
     */
    public NavigatedDisplayToolBar getNavigationToolBar() {
        return getNavigationToolBar(JToolBar.HORIZONTAL, false);
    }

    /**
     * Get the navigation toolbar for this NavigatedDisplay
     *
     * @param orientation  the orientation
     * @param floatable    true to allow floatation
     *
     * @return  the toolbar
     */
    public NavigatedDisplayToolBar getNavigationToolBar(int orientation, boolean floatable) {
        if (getDestroyed()) {
            return null;
        }

        if (navToolBar == null) {
            navToolBar = new NavigatedDisplayToolBar(this, orientation, floatable);
        }

        return navToolBar;
    }

    /**
     * Clean up
     */
    public void destroy() {
        if (navToolBar != null) {
            navToolBar.destroy();
            navToolBar = null;
        }

        super.destroy();
    }

    /**
     * Does nothing when the cursor changes. This method is called when
     * the mouse button controlling the cursor readout is pressed.  Override
     * where necessary.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void cursorMoved() throws VisADException, RemoteException {}

    /**
     * Does nothing when the pointer moves. This method is called when
     * the mouse moves over the display.  Override where necessary.
     * @param x  pointer's x location
     * @param y  pointer's y location
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void pointerMoved(int x, int y) throws VisADException, RemoteException {}

    /**
     * Update lat/lon/alt properties with the EarthLocation.
     *
     * @param el  EarthLocation to use.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void updateLocation(EarthLocation el) throws VisADException, RemoteException {
        if (el == null) {
            return;
        }

        setCursorLatitude(el.getLatitude());
        setCursorLongitude(el.getLongitude());

        if (getDisplayMode() == MODE_3D) {
            setCursorAltitude(el.getAltitude());
        }
    }

    /**
     * Get the position of the ray at a particular Z value.
     *
     * @param ray    ray to use
     * @param zValue Z value
     *
     * @return coordinates at Z value
     */
    public double[] getRayPositionAtZ(VisADRay ray, double zValue) {
        if (Double.isNaN(zValue) || (zValue == ray.position[2])) {
            return ray.position;
        }

        if (ray.vector[2] == 0) {
            return ray.position;
        }

        double r = (zValue - ray.position[2]) / ray.vector[2];

        return new double[] { ray.position[0] + r * ray.vector[0], ray.position[1] + r * ray.vector[1], zValue };
    }

    /**
     * Get the display coordinate system that turns lat/lon/alt to
     * x/y/z
     *
     * @return  the coordinate system (may be null)
     */
    public CoordinateSystem getDisplayCoordinateSystem() {
        return null;
    }

    /**
     * Set the vertical map unit
     *
     * @param vertMap   vertical map
     * @param u unit to set
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException problem setting unit
     */
    protected void setVerticalMapUnit(ScalarMap vertMap, Unit u) throws VisADException, RemoteException {
        if (Unit.canConvert(((RealType) vertMap.getScalar()).getDefaultUnit(),
                            GeopotentialAltitude.getGeopotentialMeter())) {
            vertMap.setOverrideUnit(GeopotentialAltitude.getGeopotentialUnit(u));
        } else {
            vertMap.setOverrideUnit(u);
        }
    }

    /**
     * Set the rotation delay
     *
     * @param millis   number of milliseconds between rotation events
     */
    public void setRotateDelay(long millis) {
        rotateDelay = millis;

        if (rotateDelay < 1) {
            rotateDelay = 1;
        }
    }

    /**
     * Rotate faster
     */
    public void rotateFaster() {
        setRotateDelay(rotateDelay / 2);
    }

    /**
     * rotate slower
     */
    public void rotateSlower() {
        setRotateDelay(rotateDelay * 2);
    }

    /**
     * Set the autorotation.
     *
     * @param rotate   true to auto-rotate
     */
    public void setAutoRotate(boolean rotate) {
        autoRotate = rotate;

        if (autoRotate) {
            Misc.run(new Runnable() {
                public void run() {
                    doRotate(++rotateTimeStamp);
                }
            });
        }
    }

    /**
     * Reset the scale and translation
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   VisAD problem
     */
    public void resetScaleTranslate() throws VisADException, RemoteException {
        getDisplay().getProjectionControl().resetProjection();
    }

    /**
     * Should be called from a thread. This will automatically rotate the display
     *
     * @param myTimeStamp The time stamp of when this thread got started
     */
    private void doRotate(int myTimeStamp) {
        double[] lastMatrix = null;

        while (autoRotate && (myTimeStamp == rotateTimeStamp)) {
            try {
                double[] currentMatrix = getProjectionMatrix();

                if ((lastMatrix != null) &&!Misc.arraysEquals(lastMatrix, currentMatrix)) {
                    setAutoRotate(false);

                    return;
                }

                rotate();
                lastMatrix = getProjectionMatrix();
                Thread.sleep(rotateDelay);
            } catch (Exception e) {
                return;
            }
        }
    }

    /**
     * Get the autorotation.
     *
     * @return true for auto-rotate
     */
    public boolean getAutoRotate() {
        return autoRotate;
    }

    /**
     * Rotate the display
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void rotate() throws VisADException, RemoteException {
        DisplayImpl     display         = (DisplayImpl) getDisplay();
        double[]        myMatrix        = getProjectionMatrix();
        double          scale           = getScale();
        double[]        transA          = { 0.0, 0.0, 0.0 };
        double[]        rotA            = { 0.0, 0.0, 0.0 };
        double[]        scaleA          = { 0.0, 0.0, 0.0 };
        MouseBehavior   mouseBehavior   = getMouseBehavior();
        DisplayRenderer displayRenderer = display.getDisplayRenderer();

        mouseBehavior.instance_unmake_matrix(rotA, scaleA, transA, myMatrix);

        double[] t2 = null;

        if (displayRenderer.getRotateAboutCenter()) {
            myMatrix = mouseBehavior.multiply_matrix(mouseBehavior.make_translate(-transA[0], -transA[1], -transA[2]),
                    myMatrix);
            t2 = mouseBehavior.make_translate(transA[0], transA[1], transA[2]);
        }

        double[] t1 = display.make_matrix(rotateX / scale, rotateY / scale, rotateZ / scale, 1.0, 0.0, 0.0, 0.0);

        t1 = mouseBehavior.multiply_matrix(t1, myMatrix);

        if (t2 != null) {
            t1 = mouseBehavior.multiply_matrix(t2, t1);
        }

        setProjectionMatrix(t1);
    }

    /**
     *  Set the ClipDistanceBack property.
     *
     *  @param value The new value for ClipDistanceBack
     */
    public void setClipDistanceBack(double value) {
        this.clipDistanceBack = value;
    }

    /**
     *  Get the ClipDistanceBack property.
     *
     *  @return The ClipDistanceBack
     */
    public double getClipDistanceBack() {
        return this.clipDistanceBack;
    }

    /**
     *  Set the ClipDistanceFront property.
     *
     *  @param value The new value for ClipDistanceFront
     */
    public void setClipDistanceFront(double value) {
        this.clipDistanceFront = value;
    }

    /**
     *  Get the ClipDistanceFront property.
     *
     *  @return The ClipDistanceFront
     */
    public double getClipDistanceFront() {
        return this.clipDistanceFront;
    }

    /**
     * Set the lat lon scale info
     *
     * @param latLonScaleInfo
     * @throws RemoteException
     * @throws VisADException
     */
    public void setLatLonScaleInfo(LatLonScaleInfo latLonScaleInfo) throws RemoteException, VisADException {
        this.latLonScaleInfo = latLonScaleInfo;
    }

    /**
     * Get the lat lon scale info
     *
     * @return
     */
    public LatLonScaleInfo getLatLonScaleInfo() {
        return latLonScaleInfo;
    }

    /**
     * A specialized ScalarMapSet for the set of Vertical maps
     *
     * @author IDV Development Team
     * @version $Revision: 1.100 $
     */
    protected class VerticalMapSet extends ScalarMapSet {

        /**
         * Set the vertical unit on all the scalar maps
         *
         * @param newUnit new unit for ScalarMap.  Must be meter or
         *
         * @throws RemoteException  Java RMI problem
         * @throws VisADException   Problem setting unit
         */
        public void setVerticalUnit(Unit newUnit) throws VisADException, RemoteException {
            for (Iterator iter = iterator(); iter.hasNext(); ) {
                ScalarMap vertMap = (ScalarMap) iter.next();

                if ((vertMap != null) && (newUnit != null)) {
                    setVerticalMapUnit(vertMap, newUnit);

                    if (Unit.canConvert(((RealType) vertMap.getScalar()).getDefaultUnit(),
                                        GeopotentialAltitude.getGeopotentialMeter())) {
                        vertMap.setOverrideUnit(GeopotentialAltitude.getGeopotentialUnit(newUnit));
                    } else {
                        vertMap.setOverrideUnit(newUnit);
                    }
                }
            }
        }

        /**
         * Set the vertical range on all the scalar maps
         *
         * @param min   min range value
         * @param max   max range value
         *
         *
         * @throws RemoteException  Java RMI problem
         * @throws VisADException   Problem setting unit
         */
        public void setVerticalRange(double min, double max) throws VisADException, RemoteException {
            for (Iterator iter = iterator(); iter.hasNext(); ) {
                ScalarMap vertMap = (ScalarMap) iter.next();

                vertMap.setRange(min, max);
            }
        }
    }
}
