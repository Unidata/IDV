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

package ucar.visad.display;



import visad.AxisScale;

import visad.Display;

import visad.DisplayImpl;

import visad.MouseBehavior;

import visad.ProjectionControl;

import visad.RealType;

import visad.ScalarMap;

import visad.Unit;

import visad.VisADException;

import visad.java2d.*;

import visad.java3d.*;

import java.awt.Dimension;

import java.rmi.RemoteException;


/**
 * A wrapper for a 2D display for XY plots of data
 *
 * @author Don Murray
 * @version $Revision: 1.25 $
 */
public class XYDisplay extends DisplayMaster {

    /** RealType for the Y Axis ScalarMap */
    private RealType yAxisType;

    /** RealType for the X Axis ScalarMap */
    private RealType xAxisType;

    /** YAxis ScalarMap */
    private ScalarMap yAxisMap;

    /** XAxis ScalarMap */
    private ScalarMap xAxisMap;

    /** ScalarMap for XAxis -> XAxis */
    private ScalarMap xMap;

    /** ScalarMap for YAxis -> YAxis */
    private ScalarMap yMap;

    /** flag for whether we have initialized or not */
    private boolean haveInitialized = false;

    /** Name of this display */
    private String name;

    /** clip property */
    private boolean clipOn = true;

    /** gridlines property */
    private boolean gridLinesVisible = false;

    /**
     * Constuctor for display with RealType.XAxis mapped to Display.XAxis
     * and RealType.YAxis mapped to Display.YAxis and a default name.
     *
     * @throws VisADException  some VisAD error
     * @throws RemoteException  a remote error
     */
    public XYDisplay() throws VisADException, RemoteException {
        this("XYDisplay");
    }

    /**
     * Constuctor for display with RealType.XAxis mapped to Display.XAxis
     * and RealType.YAxis mapped to Display.YAxis and the specified name.
     *
     * @param  name   name for the display
     *
     * @throws VisADException  some VisAD error
     * @throws RemoteException  a remote error
     */
    public XYDisplay(String name) throws VisADException, RemoteException {
        this(name, RealType.XAxis, RealType.YAxis);
    }

    /**
     * Constuctor for display with RealType.XAxis mapped to Display.XAxis
     * and RealType.YAxis mapped to Display.YAxis and the specified offscreen.
     *
     * @param  offscreen is this display offscreen
     * @param dimension size of display for off screen rendering. May be null.
     *
     * @throws VisADException  some VisAD error
     * @throws RemoteException  a remote error
     */
    public XYDisplay(boolean offscreen, Dimension dimension)
            throws VisADException, RemoteException {
        this("XYDisplay", RealType.XAxis, RealType.YAxis, offscreen,
             dimension);
    }

    /**
     * Constructor with RealTypes for the X and Y axes and the given name.
     *
     * @param  name        name for the display
     * @param  xAxisType   a RealType for ScalarMap(yAxisType, Display.XAxis)
     * @param  yAxisType   a RealType for ScalarMap(xAxisType, Display.YAxis)
     *
     * @throws VisADException  some VisAD error
     * @throws RemoteException  a remote error
     */
    public XYDisplay(String name, RealType xAxisType, RealType yAxisType)
            throws VisADException, RemoteException {

        this(name, xAxisType, yAxisType, false, null);
    }

    /**
     * Constructor with RealTypes for the X and Y axes and the given name.
     *
     * @param  name        name for the display
     * @param  xAxisType   a RealType for ScalarMap(yAxisType, Display.XAxis)
     * @param  yAxisType   a RealType for ScalarMap(xAxisType, Display.YAxis)
     * @param  offScreen is this display offscreen
     * @param dimension size of display for off screen rendering. May be null.
     *
     * @throws VisADException  some VisAD error
     * @throws RemoteException  a remote error
     */
    public XYDisplay(String name, RealType xAxisType, RealType yAxisType,
                     boolean offScreen, Dimension dimension)
            throws VisADException, RemoteException {
        //super(new DisplayImplJ2D(name);
        super(makeDisplayImpl(name, offScreen, dimension), 1, (offScreen
                ? ((dimension == null)
                   ? new Dimension(600, 300)
                   : dimension)
                : null));
        this.name      = name;
        this.yAxisType = yAxisType;
        this.xAxisType = xAxisType;
        initializeClass();
    }

    /**
     * Make the display
     *
     * @param name name
     * @param offScreen is display offscreen
     * @param dimension If offscreen this is the size of the display. May be null. If so defaults to 600x300
     *
     * @return The display
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private static DisplayImplJ3D makeDisplayImpl(String name,
            boolean offScreen, Dimension dimension)
            throws VisADException, RemoteException {
        TwoDDisplayRendererJ3D renderer = new TwoDDisplayRendererJ3D();
        if (dimension == null) {
            dimension = new Dimension(600, 300);
        }
        if (offScreen) {
            return new DisplayImplJ3D(name, renderer, dimension.width,
                                      dimension.height);
        }
        return new DisplayImplJ3D(name, renderer);
    }


    /**
     * Set up the display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void initializeClass() throws VisADException, RemoteException {

        /*  Maybe refactor this in the future
        DisplayRendererJ2D rend =
            (DisplayRendererJ2D) getDisplay().getDisplayRenderer();

        rend.addKeyboardBehavior(new KeyboardBehaviorJ2D(rend));
        */
        DisplayRendererJ3D rend =
            (DisplayRendererJ3D) getDisplay().getDisplayRenderer();

        rend.addKeyboardBehavior(new KeyboardBehaviorJ3D(rend));
        setSpatialScalarMaps();
    }

    /**
     * Create and add spatial ScalarMap-s;
     * Create and assign values to AxisScales.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setSpatialScalarMaps()
            throws VisADException, RemoteException {


        setDisplayInactive();
        ScalarMapSet mapSet = new ScalarMapSet();
        if ( !haveInitialized) {
            xMap = new ScalarMap(RealType.XAxis, Display.XAxis);
            xMap.setRange(-1.0, 1.0);
            mapSet.add(xMap);
            yMap = new ScalarMap(RealType.YAxis, Display.YAxis);
            yMap.setRange(-1.0, 1.0);
            mapSet.add(yMap);
            haveInitialized = true;
        }

        if ((xAxisMap == null) || !xAxisMap.getScalar().equals(xAxisType)) {
            if (xAxisMap != null) {
                removeScalarMap(xAxisMap);
            }

            xAxisMap = new ScalarMap(xAxisType, Display.XAxis);

            mapSet.add(xAxisMap);
            xMap.setScaleEnable(false);
        }

        if ((yAxisMap == null) || !yAxisMap.getScalar().equals(yAxisType)) {
            if (yAxisMap != null) {
                removeScalarMap(yAxisMap);
            }

            yAxisMap = new ScalarMap(yAxisType, Display.YAxis);

            mapSet.add(yAxisMap);
            yMap.setScaleEnable(false);
        }

        addScalarMaps(mapSet);
        setDisplayActive();
    }

    /**
     * Change the mapping for the XAxis.
     *
     * @param xType  new RealType for ScalarMap
     *
     * @throws VisADException  some VisAD error
     * @throws RemoteException  a remote error
     */
    public void setXAxisType(RealType xType)
            throws VisADException, RemoteException {

        xAxisType = xType;

        setSpatialScalarMaps();
    }

    /**
     * Change the mapping for the YAxis.
     *
     * @param yType  new RealType for ScalarMap
     *
     * @throws VisADException  some VisAD error
     * @throws RemoteException  a remote error
     */
    public void setYAxisType(RealType yType)
            throws VisADException, RemoteException {

        yAxisType = yType;

        setSpatialScalarMaps();
    }

    /**
     * Get the mapping for the XAxis.
     *
     * @return RealType for X axis ScalarMap
     */
    public RealType getXAxisType() {
        return xAxisType;
    }

    /**
     * Get the mapping for the YAxis.
     *
     * @return RealType for Y axis ScalarMap
     */
    public RealType getYAxisType() {
        return yAxisType;
    }

    /**
     * Change the mapping for both axes
     *
     * @param xType  new RealType for X axis ScalarMap
     * @param yType  new RealType for Y axis ScalarMap
     *
     * @throws VisADException  some VisAD error
     * @throws RemoteException  a remote error
     */
    public void setAxisTypes(RealType xType, RealType yType)
            throws VisADException, RemoteException {

        xAxisType = xType;
        yAxisType = yType;

        setSpatialScalarMaps();
    }

    /**
     * Get the AxisScale associated with the X axis.
     *
     * @return  X AxisScale
     */
    public AxisScale getXAxisScale() {

        return (xAxisMap == null)
               ? null
               : xAxisMap.getAxisScale();
    }

    /**
     * Get the AxisScale associated with the Y axis.
     *
     * @return  X AxisScale
     */
    public AxisScale getYAxisScale() {

        return (yAxisMap == null)
               ? null
               : yAxisMap.getAxisScale();
    }

    /**
     * Show the scale on both axes.
     *
     * @param  show  true to show
     */
    public void showAxisScales(boolean show) {

        try {
            getDisplay().getGraphicsModeControl().setScaleEnable(show);
        } catch (VisADException ve) {
            ;
        } catch (RemoteException re) {
            ;
        }
    }

    /**
     * Set the range of displayed values on the X axis
     *
     * @param  min  minimum value for axis
     * @param  max  maximum value for axis
     */
    public void setXRange(double min, double max) {

        try {
            if (xAxisMap != null) {
                xAxisMap.setRange(min, max);
            }
        } catch (VisADException ve) {
            ;
        } catch (RemoteException re) {
            ;
        }
    }

    /**
     * Set the range of displayed values on the Y axis
     *
     * @param  min  minimum value for axis
     * @param  max  maximum value for axis
     */
    public void setYRange(double min, double max) {

        try {
            if (yAxisMap != null) {
                yAxisMap.setRange(min, max);
            }
        } catch (VisADException ve) {
            ;
        } catch (RemoteException re) {
            ;
        }
    }

    /**
     * Let the XAxis autoscale.
     */
    public void autoScaleXAxis() {
        if (xAxisMap != null) {
            xAxisMap.resetAutoScale();
        }
    }

    /**
     * Let the YAxis autoscale.
     */
    public void autoScaleYAxis() {
        if (yAxisMap != null) {
            yAxisMap.resetAutoScale();
        }
    }

    /**
     * Set the units of displayed values on the X axis
     *
     * @param  newUnit  units to use
     */
    public void setXDisplayUnit(Unit newUnit) {

        try {
            if (xAxisMap != null) {
                xAxisMap.setOverrideUnit(newUnit);
            }
        } catch (VisADException ve) {
            ;
        }
    }

    /**
     * Set the units of displayed values on the Y axis
     *
     * @param  newUnit  units to use
     */
    public void setYDisplayUnit(Unit newUnit) {

        try {
            if (yAxisMap != null) {
                yAxisMap.setOverrideUnit(newUnit);
            }
        } catch (VisADException ve) {
            ;
        }
    }

    /**
     * Get the units of displayed values on the X axis
     *
     * @return  axis units
     */
    public Unit getXDisplayUnit() {

        Unit u = null;
        if (xAxisMap != null) {
            u = xAxisMap.getOverrideUnit();
            if (u == null) {
                u = ((RealType) xAxisMap.getScalar()).getDefaultUnit();
            }
        }
        return u;
    }

    /**
     * Get the units of displayed values on the Y axis
     *
     * @return  axis units
     */
    public Unit getYDisplayUnit() {

        Unit u = null;
        if (yAxisMap != null) {
            u = yAxisMap.getOverrideUnit();
            if (u == null) {
                u = ((RealType) yAxisMap.getScalar()).getDefaultUnit();
            }
        }
        return u;
    }

    /**
     * Set the aspect ratio of the axes
     *
     * @param  x  X axis ratio
     * @param  y  Y axis ratio
     */
    public void setAspect(double x, double y) {

        try {

            ProjectionControl pc = getDisplay().getProjectionControl();

            pc.setAspectCartesian(new double[] { x, y });
            //saveProjection();
        } catch (VisADException ve) {
            ;
        } catch (RemoteException re) {
            ;
        }
    }

    /**
     * Get the name given to this display.
     *
     * @return name of display.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this display.
     *
     * @param newName new name of display.
     */
    public void setName(String newName) {
        name = newName;
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
            clipOn = clip;
        } catch (VisADException ve) {
            System.err.println("Couldn't set clipping " + ve);
        }
    }


    /**
     * Set the grid lines visible
     *
     * @param yesorno  true to be visible
     */
    public void setGridLinesVisible(boolean yesorno) {
        AxisScale scale = getXAxisScale();
        if (scale != null) {
            scale.setGridLinesVisible(yesorno);
        }
        scale = getYAxisScale();
        if (scale != null) {
            scale.setGridLinesVisible(yesorno);
        }
        gridLinesVisible = yesorno;
    }

    /**
     * Get whether the grid lines are visible
     *
     * @return true if visible
     */
    public boolean getGridLinesVisible() {
        return gridLinesVisible;
    }





}
