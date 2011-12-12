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

package ucar.visad.display;


import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.Range;

import visad.BadMappingException;
import visad.CommonUnit;
import visad.ConstantMap;
import visad.Display;
import visad.EarthVectorType;
import visad.FieldImpl;
import visad.FlatField;
import visad.FlowControl;
import visad.MathType;
import visad.RealTupleType;
import visad.RealType;
import visad.RealVectorType;
import visad.ScalarMap;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.ScalarMapListener;
import visad.TupleType;
import visad.Unit;
import visad.VisADException;


import java.awt.Color;

import java.rmi.RemoteException;


/**
 * Provides support for a Displayable displays wind data (u,v) or
 * (spd, dir) as wind vectors.
 *
 * @author IDV Development Team
 */
public class FlowDisplayable extends RGBDisplayable  /*DisplayableData*/
    implements GridDisplayable {

    /** use Flow1 ScalarMaps */
    private static boolean useFlow1 = false;

    /**
     * The name of the "real-type" property.
     */
    public static final String FLOW_TYPE = "flowRealTupleType";

    /**
     * The name of the color property.
     */
    public static String COLOR = "color";

    /** ScalarMap for X component of flow */
    volatile ScalarMap flowXMap;

    /** ScalarMap for Y component of flow */
    volatile ScalarMap flowYMap;

    /** ScalarMap for Z component of flow */
    volatile ScalarMap flowZMap;

    /** FlowControl for ScalarMap */
    private volatile FlowControl flowControl;

    /** RealTupleType of flow parameters */
    private volatile RealTupleType flowRealTupleType;

    /** scale factor for size of vectors/barbs */
    private float flowscale = 0.02f;

    /** streamline density factor */
    private float streamlineDensity = 1.f;

    /** Color of barbs/vectors */
    private Color myColor;

    /** flag for streamlines */
    private boolean isStreamlines = false;

    /** flag for whether wind is cartesian (u,v) or polar (spd,dir) */
    private boolean isCartesian = true;

    /** flag for coloring one param by another */
    private boolean coloredByAnother = false;

    /** flag for coloring by wind speed */
    private boolean useSpeedForColor = false;

    /** flag for autoscale */
    private boolean autoScale = false;

    /** flag for orientation of barbs (north or south) */
    private int barborientation;

    /** flag for adjustToFlowEarth */
    private boolean adjustFlow = true;

    /** flag for 3D vectors */
    private boolean is3D = false;

    /** flag for ignoring extra parameters */
    private boolean ignoreExtraParameters = false;

    /** Value for Northern Hemisphere orientation */
    public static final int NH_ORIENTATION = FlowControl.NH_ORIENTATION;

    /** Value for Southern Hemisphere orientation */
    public static final int SH_ORIENTATION = FlowControl.SH_ORIENTATION;

    /** min flow range */
    private double flowMinValue = Double.NEGATIVE_INFINITY;

    /** max flow range */
    private double flowMaxValue = Double.POSITIVE_INFINITY;

    /** max flow range */
    protected Unit speedUnit = null;

    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter, and the desired size of "scale"
     *
     * @param name           The name for the displayable.
     * @param flowscale      A float size for the "flow scale".
     * @param rTT        The VisAD RealTupleType of the parameter.  May be
     *                          <code>null</code>.
     * @param useSpeedForColor  set to true if you want to color by speed
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public FlowDisplayable(String name, RealTupleType rTT, float flowscale,
                           boolean useSpeedForColor)
            throws VisADException, RemoteException {

        super(name, null, null, true);

        this.flowRealTupleType = rTT;  // immutable object
        this.flowscale         = flowscale;
        this.useSpeedForColor  = useSpeedForColor;

        if (flowRealTupleType != null) {
            setFlowMaps();
        }
    }

    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter, and the desired size of "scale"
     *
     * @param name           The name for the displayable.
     * @param flowscale      A float size for the "flow scale".
     * @param rTT        The VisAD RealTupleType of the parameter.  May be
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public FlowDisplayable(String name, RealTupleType rTT, float flowscale)
            throws VisADException, RemoteException {

        this(name, rTT, flowscale, false);
    }


    /**
     * Constructs from a name for the Displayable and the type of the
     * parameter.
     *
     * @param name           The name for the displayable.
     * @param rTT        The VisAD RealTupleType of the parameter.  May be
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public FlowDisplayable(String name, RealTupleType rTT)
            throws VisADException, RemoteException {

        this(name, rTT, 0.02f);
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected FlowDisplayable(FlowDisplayable that)
            throws VisADException, RemoteException {

        super(that);

        flowRealTupleType = that.flowRealTupleType;  // immutable object
        flowscale         = that.flowscale;

        if (flowRealTupleType != null) {
            setFlowMaps();
        }
    }

    /**
     * Returns the RealTupleType of the parameter.
     * @return                  The RealTupleType of the parameter.  May
     *                          be <code>null</code>.
     */
    public RealTupleType getFlowTuple() {
        return flowRealTupleType;
    }


    /**
     * Returns boolean whether streamlines are enabled.
     *
     * @return boolean whether streamlines are enabled.
     */
    public boolean getStreamlinesEnabled() {

        return (flowControl == null)
               ? false
               : flowControl.streamlinesEnabled();
    }

    /**
     * Sets whether streamlines are enabled.
     * @param enable boolean whether streamlines are enabled.
     */
    public void setStreamlinesEnabled(boolean enable) {

        if ((flowControl != null) && (enable != isStreamlines)) {
            try {
                flowControl.enableStreamlines(enable);
            } catch (VisADException ve) {
                ve.printStackTrace();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }

        isStreamlines = enable;
    }

    /**
     * Returns indicator whether wind barb style is that used in
     * the northern hemisphere or the southern hemisphere.
     *
     * @return NH_ORIENTATION or SH_ORIENTATION
     */
    public int getBarbOrientation() {
        return barborientation;
    }


    /**
     * Set indicator whether wind barb style is that used in
     * the northern hemisphere or the southern hemisphere.
     *
     * @param style either NH_ORIENTATION or SH_ORIENTATION.
     */
    public void setBarbOrientation(int style) {

        if (flowControl != null) {
            try {
                if ((style == NH_ORIENTATION) || (style == SH_ORIENTATION)) {
                    flowControl.setBarbOrientation(style);
                    barborientation = style;
                }
            } catch (VisADException ve) {
                ve.printStackTrace();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }

    }

    /**
     * Returns indicator whether the flow should be adjusted to the earth
     *
     * @return true to adjust
     */
    public boolean getAdjustFlow() {
        return adjustFlow;
    }


    /**
     * Set indicator whether winds should be adusted or not
     *
     * @param adjust  true to adjust
     */
    public void setAdjustFlow(boolean adjust) {

        if (flowControl != null) {
            try {
                flowControl.setAdjustFlowToEarth(adjust);
            } catch (VisADException ve) {
                ve.printStackTrace();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }
        adjustFlow = adjust;
    }

    /**
     * Set the density of the streamlines
     *
     * @param density  typically roughly .1 to 2.
     */
    public void setStreamlineDensity(float density) {

        if ((flowControl != null) && (streamlineDensity != density)) {
            try {
                flowControl.setStreamlineDensity(density);
            } catch (VisADException e) {
                ;
            } catch (RemoteException e) {
                ;
            }
        }

        streamlineDensity = density;
    }


    /**
     * Set the length of shaft on vector or on wind barb.
     *
     * @param scale  typically roughly 0.05 to 0.15.
     */
    public void setFlowScale(float scale) {

        if ((flowControl != null) && (scale != flowscale)) {
            try {
                adjustScale(scale);
            } catch (VisADException e) {
                ;
            } catch (RemoteException e) {
                ;
            }
        }

        flowscale = scale;
    }

    /**
     * Set the autoscale property
     *
     * @param auto  the autoscale property
     */
    public void setAutoScale(boolean auto) {

        if ((flowControl != null) && (auto != autoScale)) {
            try {
                flowControl.setAutoScale(auto);
            } catch (VisADException e) {}
        }

        autoScale = auto;
    }

    /**
     * Sets the set of ScalarMap-s of this instance.  The ScalarMap-s of
     * this instance will be added to the set before the SCALAR_MAP_SET
     * property is set.  This method fires a PropertyChangeEvent for
     * SCALAR_MAP_SET with <code>null</code> for the old value and the new
     * set of ScalarMap-s for the new Value.  Intermediate subclasses that
     * have their own ScalarMap-s should override this method and invoke
     * <code>super.setScalarMaps(ScalarMapSet)</code>.
     *
     * @param maps              The set of ScalarMap-s to be added.
     *
     * @throws BadMappingException      The types of the maps have
     *                          not been set or its ScalarMap is already in
     *                          the set.
     */
    protected void setScalarMaps(ScalarMapSet maps)
            throws BadMappingException {

        if (flowXMap == null) {
            throw new BadMappingException(getClass().getName()
                                          + ".setScalarMaps(ScalarMapSet): "
                                          + "flowXMap not yet set");
        }

        maps.add(flowXMap);

        if (flowYMap == null) {
            throw new BadMappingException(getClass().getName()
                                          + ".setScalarMaps(ScalarMapSet): "
                                          + "flowYMap not yet set");
        }

        maps.add(flowYMap);

        if (get3DFlow()) {
            if (flowZMap == null) {
                throw new BadMappingException(getClass().getName()
                        + ".setScalarMaps(ScalarMapSet): "
                        + "flowZMap not yet set");
            }
            maps.add(flowZMap);
        }

        super.setScalarMapSet(maps);
    }

    /**
     * Get the index of the speed type if this is not a cartesian wind
     * @return the speed index or -1 if cartesian or not set
     */
    public int getSpeedTypeIndex() {
        int index = -1;
        if ( !isCartesianWind() && (flowYMap != null)
                && (flowRealTupleType != null)) {
            RealType   speedType = (RealType) flowYMap.getScalar();
            RealType[] realTypes = flowRealTupleType.getRealComponents();
            for (int i = 0; i < realTypes.length; i++) {
                if (speedType.equals(realTypes[i])) {
                    index = i;
                }
            }
        }

        return index;
    }

    /**
     * Make ScalarMaps for flow, for 1st and 2nd components of wind
     * vector input data.
     * If data is of form u,v (both components have units convertible with m/s)
     * make map for 1st comp to Display.Flow1X and 2nd comp to Display.Flow1Y.
     * If data is dir,spd or spd, dir,  make map for dir comp to
     * Display.Flow1Azimuth and spd comp to Display.Flow1Radial.
     *
     * Note - makes bad plot if data is v,u form
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setFlowMaps() throws RemoteException, VisADException {

        useFlow1 = !useFlow1;

        RealType[] realTypes = flowRealTupleType.getRealComponents();
        Unit[]     units     = flowRealTupleType.getDefaultUnits();

        int        spdIndex  = 1;

        // if data is u,v (so far as you can tell by units)
        if ((Unit.canConvert(units[0], CommonUnit.meterPerSecond)
                && Unit.canConvert(units[1],
                    CommonUnit.meterPerSecond)) || ((units[0] == null)
                        && (units[1]
                            == null)) || (CommonUnit.dimensionless.equals(units[0])
                                && CommonUnit.dimensionless.equals(units[1]))) {
            flowXMap = new ScalarMap(realTypes[0], (useFlow1
                    ? Display.Flow1X
                    : Display.Flow2X));
            flowYMap = new ScalarMap(realTypes[1], (useFlow1
                    ? Display.Flow1Y
                    : Display.Flow2Y));
            spdIndex = 0;  // color speed by u component
        } else if (Unit.canConvert(units[0], CommonUnit.degree)
                   && Unit.canConvert(units[1], CommonUnit.meterPerSecond)) {
            isCartesian = false;
            flowXMap    = new ScalarMap(realTypes[0], (useFlow1
                    ? Display.Flow1Azimuth
                    : Display.Flow2Azimuth));
            flowXMap.setRange(0, 360);
            flowYMap = new ScalarMap(realTypes[1], (useFlow1
                    ? Display.Flow1Radial
                    : Display.Flow2Radial));
            spdIndex = 1;
        } else if (Unit.canConvert(units[0], CommonUnit.meterPerSecond)
                   && Unit.canConvert(units[1], CommonUnit.degree)) {
            isCartesian = false;
            flowXMap    = new ScalarMap(realTypes[1], (useFlow1
                    ? Display.Flow1Azimuth
                    : Display.Flow2Azimuth));
            flowXMap.setRange(0, 360);
            flowYMap = new ScalarMap(realTypes[0], (useFlow1
                    ? Display.Flow1Radial
                    : Display.Flow2Radial));
            spdIndex = 0;
        } else {
            //throw new VisADException("Unknown units for flow types");
            flowXMap = new ScalarMap(realTypes[0], (useFlow1
                    ? Display.Flow1X
                    : Display.Flow2X));
            flowYMap = new ScalarMap(realTypes[1], (useFlow1
                    ? Display.Flow1Y
                    : Display.Flow2Y));
            spdIndex = 0;  // color speed by u component
        }
        speedUnit = units[spdIndex];

        if (get3DFlow()) {
            if (Unit.canConvert(units[2], CommonUnit.meterPerSecond)) {
                isCartesian = true;
                flowZMap    = new ScalarMap(realTypes[2], (useFlow1
                        ? Display.Flow1Z
                        : Display.Flow2Z));
            } else {
                isCartesian = false;
                flowZMap    = new ScalarMap(realTypes[2], (useFlow1
                        ? Display.Flow1Elevation
                        : Display.Flow2Elevation));
            }
        }

        // set RealType to color by speed
        if (useSpeedForColor && !isCartesian) {
            //if ( !coloredByAnother) {
            setRGBRealType(
                (RealType) flowRealTupleType.getComponent(spdIndex));
        }

        flowYMap.addScalarMapListener(new ScalarMapListener() {

            public void controlChanged(ScalarMapControlEvent event)
                    throws RemoteException, VisADException {

                int id = event.getId();

                if ((id == event.CONTROL_ADDED)
                        || (id == event.CONTROL_REPLACED)) {
                    flowControl = (FlowControl) flowYMap.getControl();
                    flowControl.enableStreamlines(isStreamlines);
                    flowControl.setAutoScale(autoScale);
                    flowControl.setStreamlineDensity(streamlineDensity);
                    flowControl.setAdjustFlowToEarth(adjustFlow);
                    flowControl.setBarbOrientation(barborientation);
                    adjustScale(flowscale);
                }
            }

            public void mapChanged(ScalarMapEvent event) {}  // ignore
        });
        //System.out.println("FlowX = " + flowXMap);
        //System.out.println("FlowY = " + flowYMap);
        //System.out.println("isCartesian = " + isCartesian);

        ScalarMapSet maps = getScalarMapSet();  //new ScalarMapSet();
        maps.add(flowXMap);
        maps.add(flowYMap);
        if (get3DFlow()) {
            maps.add(flowZMap);
        }
        setFlowRange(flowMinValue, flowMaxValue);
        setScalarMapSet(maps);
    }

    /**
     * Set the range of the flow maps
     *
     * @param flowRange range for flow maps
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setFlowRange(Range flowRange)
            throws VisADException, RemoteException {
        if (flowRange == null) {
            return;
        }
        setFlowRange(flowRange.getMin(), flowRange.getMax());
    }

    /**
     * Set the range of the flow maps
     *
     * @param min min value
     * @param max max value
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setFlowRange(double min, double max)
            throws VisADException, RemoteException {
        flowMinValue = min;
        flowMaxValue = max;
        if ( !Double.isInfinite(flowMinValue)
                && !Double.isInfinite(flowMaxValue)) {

            if (isCartesianWind() && (flowXMap != null)) {
                flowXMap.setRange(flowMinValue, flowMaxValue);
                flowYMap.setRange(flowMinValue, flowMaxValue);
                if (get3DFlow()) {
                    flowZMap.setRange(flowMinValue, flowMaxValue);
                }
            } else if ( !isCartesianWind() && (flowYMap != null)) {
                flowYMap.setRange(0, flowMaxValue);
            }
        }
    }

    /**
     * Set the 3d grid (a FlatField) data into the Displayable
     *
     * @param field a VisAD FlatField with a 3D nature
     *
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public void setGrid3D(FieldImpl field)
            throws VisADException, RemoteException {
        loadData(field);
    }

    /**
     * Set the data into the Displayable
     *
     * @param field a VisAD FlatField with a 2D nature
     *
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public void loadData(FieldImpl field)
            throws VisADException, RemoteException {

        FlatField ffld;

        TupleType tt = GridUtil.getParamType(field);

        // get the RealTupleType of the range data in the  FlatField
        RealTupleType rtt       = new RealTupleType(tt.getRealComponents());

        int           threeDDim = (coloredByAnother || ignoreExtraParameters)
                                  ? 3
                                  : 2;
        set3DFlow(rtt.getDimension() > threeDDim);
        // uncomment to determine ad-hoc rather than programatically
        //coloredByAnother = coloredByAnother && (rtt.getDimension() > 2);
        if (coloredByAnother) {
            // get the RealType of the range data
            RealType rgbType = (RealType) rtt.getComponent(rtt.getDimension()
                                   - 1);
            setRGBRealType(rgbType);
        }
        setType(rtt);

        TupleType newParamType = null;
        // if u/v, then set as EarthVectorType (determined in course of setType)
        if (isCartesianWind() && !((tt instanceof RealVectorType  /* complete tuple */
                ) || (tt.getComponent(0)
                      instanceof RealVectorType)) /*colored by another */) {
            int numBase = get3DFlow()
                          ? 3
                          : 2;
            try {
                if ( !coloredByAnother || (rtt.getDimension() == numBase)) {
                    newParamType =
                        new EarthVectorType(rtt.getRealComponents());
                } else {
                    RealType[] reals  = rtt.getRealComponents();
                    RealType[] extras = new RealType[reals.length - numBase];
                    System.arraycopy(reals, numBase, extras, 0,
                                     extras.length);
                    newParamType = new TupleType(new MathType[] {
                        (get3DFlow())
                        ? new EarthVectorType(reals[0], reals[1], reals[2])
                        : new EarthVectorType(reals[0], reals[1]),
                        new RealTupleType(extras) });
                }
            } catch (VisADException ve) {}
        }
        setData((newParamType == null)
                ? field
                : GridUtil.setParamType(field, newParamType, false));
    }

    /**
     * Sets the color of the vectors.  Only overrides super.setColor
     * to fire the property change event for backward compatibility.
     *
     * @param   color     color for the vectors.
     *
     * @throws VisADException     VisAD failure.
     * @throws RemoteException    Java RMI failure.
     */
    public void setColor(Color color) throws VisADException, RemoteException {

        Color oldValue;

        synchronized (this) {
            oldValue = myColor;
            addConstantMaps(new ConstantMap[] {
                new ConstantMap(color.getRed() / 255., Display.Red),
                new ConstantMap(color.getGreen() / 255., Display.Green),
                new ConstantMap(color.getBlue() / 255., Display.Blue) });
            //super.setColor(color);
            myColor = color;
        }

        firePropertyChange(COLOR, oldValue, myColor);
    }

    /**
     * Sets the RealType of the parameter.
     * @param rTT          The new RealTupleType of the parameter.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setType(RealTupleType rTT)
            throws RemoteException, VisADException {

        if ( !rTT.equals(flowRealTupleType)) {
            RealTupleType oldValue = flowRealTupleType;

            flowRealTupleType = rTT;

            setFlowMaps();
            firePropertyChange(FLOW_TYPE, oldValue, flowRealTupleType);
        }
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return this;
    }

    /**
     * Use this method in case we want to also adjust the
     * streamline properties as well.
     *
     * @param scale
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void adjustScale(float scale)
            throws VisADException, RemoteException {
        flowControl.setFlowScale(scale);
    }

    /**
     * Check to see if the wind data is cartesian (u,v) or
     * if it's polar (spd, dir).
     * @return  true if the wind is cartesian
     */
    public boolean isCartesianWind() {
        return isCartesian;
    }

    /**
     * Check to see if this is 3D flow
     * @return  true if the flow is 3D
     */
    public boolean get3DFlow() {
        return is3D;
    }

    /**
     * Set to use 3D flow
     * @param threeD  true if the flow is 3D
     */
    public void set3DFlow(boolean threeD) {
        is3D = threeD;
    }

    /**
     * Does this object use the displayUnit (or the colorUnit) for
     * its display unit.  If we have the case where this wind field
     * is colored by another field then this returns false.
     * @return  true if the display unit should be used for color
     */
    protected boolean useDisplayUnitForColor() {
        return !coloredByAnother;
    }

    /**
     * Set whether this GridDisplayable should have the data colored
     * by another parameter.
     *
     * @param yesno true if colored by another
     */
    public void setColoredByAnother(boolean yesno) {
        coloredByAnother = yesno;
    }

    /**
     * Set whether this GridDisplayable should have the data colored
     * by speed.
     *
     * @param yesno true if colored by speed
     */
    public void setUseSpeedForColor(boolean yesno) {
        useSpeedForColor = yesno;
    }

    /**
     * Set whether this GridDisplayable should ignore extra parameters
     *
     * @param yesno true if extra params should be ignored
     */
    public void setIgnoreExtraParameters(boolean yesno) {
        ignoreExtraParameters = yesno;
    }
}
