/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.DerivedGridFactory;
import ucar.unidata.data.grid.GridMath;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.Range;

import ucar.visad.data.GeoGridFlatField;
import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.data.units.UnitParser;


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

    /** _more_ */
    private float trajOffset = 4.0f;

    /** _more_ */
    private int smoothFactor = 20;

    /** _more_ */
    private float trajWidth = 0.01f;

    /** _more_ */
    private float ribbonWidth = 1f;

    /** _more_ */
    private int zskip = 0;

    /** _more_ */
    private float vectorLength = 2.0f;

    /** _more_ */
    private boolean arrowHead = false;

    /** _more_ */
    private float arrowHeadSize = 1.0f;

    /** streamline density factor */
    private float streamlineDensity = 1.f;

    /** Color of barbs/vectors */
    private Color myColor;

    /** flag for streamlines */
    private boolean isStreamlines = false;

    /** _more_ */
    private boolean isTrajectories = false;

    /** _more_ */
    private boolean isRefresh = false;

    /** _more_ */
    private boolean streamline = false;

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

    /** speed index */
    protected int spdIndex = 1;

    /** _more_ */
    private int trajFormType = 0;

    /** _more_ */
    private int trajStartLevel = 0;

    /** _more_ */
    private boolean forward = true;

    /** _more_ */
    RealTupleType trajStartPointType = null;

    /** _more_ */
    float[][] trajStartPoints = null;


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
     * sets trajectory parms if the enable is true.
     * @param enable boolean whether trajectories are enabled.
     * @param mSize _more_
     * @param refresh _more_
     */
    public void setTrojectoriesEnabled(boolean enable, float mSize,
                                       boolean refresh) {
        setTrojectoriesEnabled(enable, false, mSize, refresh);
    }

    /**
     * sets trajectory parms if the enable is true.
     * @param enable boolean is true if it is traj or cvector.
     * @param markerOn _more_
     * @param mSize _more_
     * @param refresh _more_
     */
    public void setTrojectoriesEnabled(boolean enable, boolean markerOn,
                                       float mSize, boolean refresh) {

        if ((flowControl != null)) {
            try {
                if ( !enable) {
                    flowControl.enableTrajectory(false);
                } else {
                    Set timeSet =
                        GridUtil.getTimeSet((FieldImpl) (getData()));
                    int        numTimes = timeSet.getLength();
                    double[][] td       = timeSet.getDoubles();
                    double     tlen;
                    double     rlen;

                    if (refresh) {
                        rlen = (td[0][1] - td[0][0]) * vectorLength;
                        tlen = (td[0][1] - td[0][0]) * vectorLength;
                    } else {
                        rlen = (td[0][1] - td[0][0]) * numTimes;
                        tlen = (td[0][1] - td[0][0]) * trajOffset;
                        if(streamline)
                            rlen = (td[0][1] - td[0][0]);
                    }
                    arrowHead     = markerOn;
                    arrowHeadSize = mSize;
                    TrajectoryParams tparm =
                        flowControl.getTrajectoryParams();
                    tparm.setTrajectoryForm(trajFormType);
                    tparm.setMarkerSize(mSize);
                    tparm.setTrajRefreshInterval(rlen);
                    tparm.setTrajVisibilityTimeWindow(tlen);
                    tparm.setMarkerEnabled(markerOn);
                    tparm.setCylinderWidth(trajWidth);
                    tparm.setRibbonWidthFactor(ribbonWidth);
                    tparm.setZStartIndex(trajStartLevel);
                    tparm.setStartPoints(trajStartPointType, trajStartPoints);
                    tparm.setZStartSkip(zskip);
                    tparm.setDirectionFlag(forward);
                    if (isTrajectories) {
                        tparm.setCachingEnabled(false);
                        tparm.setMethod(TrajectoryParams.Method.HySplit);
                        tparm.setTimeStepScaleFactor(1.0);
                        tparm.setInterpolationMethod(TrajectoryParams.InterpolationMethod.Cubic);
                    }
                    if(streamline) {
                        tparm.setTrajVisibilityTimeWindow(rlen);
                        tparm.setManualIntrpPts(true);
                        tparm.setMethod(TrajectoryParams.Method.Euler);
                        tparm.setInterpolationMethod(TrajectoryParams.InterpolationMethod.None);
                        tparm.setTimeStepScaleFactor(trajOffset);
                        tparm.setNumIntrpPts(smoothFactor);
                    }
                    flowControl.enableTrajectory(enable, tparm);
                }
            } catch (VisADException ve) {
                ve.printStackTrace();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }

        isTrajectories = enable;
        isRefresh      = refresh;
    }

    /**
     * _more_
     *
     * @param isTrajectories _more_
     */
    public void setIsTrajectories(boolean isTrajectories) {
        this.isTrajectories = isTrajectories;
    }

    /**
     * _more_
     *
     * @param streamline _more_
     */
    public void setStreamline(boolean streamline) {
        this.streamline = streamline;
    }
    /**
     * resets trajectory parms
     *
     */

    public void resetTrojectories() {

        if ((flowControl != null)) {
            try {
                Set timeSet = GridUtil.getTimeSet((FieldImpl) (getData()));
                int        numTimes = timeSet.getLength();
                double[][] td       = timeSet.getDoubles();
                double     tlen;
                double     rlen;

                if (isRefresh) {
                    rlen = (td[0][1] - td[0][0]) * vectorLength;
                    tlen = (td[0][1] - td[0][0]) * vectorLength;
                } else {
                    rlen = (td[0][1] - td[0][0]) * numTimes;
                    tlen = (td[0][1] - td[0][0]) * trajOffset;
                if(streamline)
                    rlen = (td[0][1] - td[0][0]);
                }

                TrajectoryParams tparm = flowControl.getTrajectoryParams();
                int              t     = tparm.getStartSkip();
                tparm.setTrajectoryForm(trajFormType);
                tparm.setMarkerSize(arrowHeadSize);
                tparm.setTrajRefreshInterval(rlen);
                tparm.setTrajVisibilityTimeWindow(tlen);
                tparm.setMarkerEnabled(arrowHead);
                tparm.setCylinderWidth(trajWidth);
                tparm.setRibbonWidthFactor(ribbonWidth);
                tparm.setStartSkip(t);
                tparm.setNumIntrpPts(smoothFactor);
                tparm.setZStartIndex(trajStartLevel);
                tparm.setZStartSkip(zskip);
                tparm.setStartPoints(trajStartPointType, trajStartPoints);
                tparm.setDirectionFlag(forward);
                //if(isTrajectories)
                tparm.setCachingEnabled(false);
                //flowControl.setTrajectoryParams(tparm);
                if(streamline) {
                    tparm.setTrajVisibilityTimeWindow(rlen);
                    tparm.setManualIntrpPts(true);
                    tparm.setMethod(TrajectoryParams.Method.Euler);
                    tparm.setInterpolationMethod(TrajectoryParams.InterpolationMethod.None);
                    tparm.setTimeStepScaleFactor(trajOffset);
                    //tparm.setNumIntrpPts(smoothFactor);
                    flowControl.enableTrajectory(true, tparm);
                } else {
                    tparm.setMethod(TrajectoryParams.Method.HySplit);
                    tparm.setTimeStepScaleFactor(1.0);
                    tparm.setInterpolationMethod(TrajectoryParams.InterpolationMethod.Cubic);
                    flowControl.enableTrajectory(isTrajectories, tparm);
                }

            } catch (VisADException ve) {
                ve.printStackTrace();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }
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
     * _more_
     *
     * @param onoff _more_
     */
    public void setArrowHead(boolean onoff) {
        arrowHead = onoff;
    }

    /**
     * _more_
     *
     * @param size _more_
     */
    public void setArrowHeadSize(float size) {
        arrowHeadSize = size;
        if (flowControl != null) {
            try {
                flowControl.setArrowScale(arrowHeadSize);
            } catch (Exception e) {}
        }
    }


    /**
     * _more_
     *
     * @param len _more_
     */
    public void setVectorLength(float len) {
        vectorLength = len;

    }

    /**
     * _more_
     *
     * @param offset _more_
     */
    public void setTrajOffset(float offset) {
        trajOffset = offset;
    }

    /**
     * _more_
     *
     * @param factor _more_
     */
    public void setSmoothFactor(int factor) {
        smoothFactor = factor;
    }

    /**
     * _more_
     *
     * @param formType _more_
     */
    public void setTrajFormType(int formType) {
        trajFormType = formType;
    }

    /**
     * _more_
     *
     * @param startLevel _more_
     */
    public void setTrajStartLevel(int startLevel) {
        trajStartLevel = startLevel;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getTrajStartLevel() {
        return trajStartLevel;
    }

    /**
     * _more_
     *
     * @param width _more_
     */
    public void setTrajWidth(float width) {
        trajWidth = width;
    }

    /**
     * _more_
     *
     *
     * @return _more_
     */
    public float getTrajWidth() {
        return trajWidth;
    }

    /**
     * _more_
     *
     * @param width _more_
     */
    public void setRibbonWidth(float width) {
        ribbonWidth = width;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float getRibbonWidth() {
        return ribbonWidth;
    }

    /**
     * _more_
     *
     * @param skip _more_
     */
    public void setZskip(int skip) {
        zskip = skip;
    }

    /**
     * _more_
     *
     * @param
     *
     * @return _more_
     */
    public int getZskip() {
        return zskip;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getForward() {
        return forward;
    }

    /**
     * _more_
     *
     * @param forw _more_
     */
    public void setForward(boolean forw) {
        forward = forw;
    }

    /**
     * _more_
     *
     * @param type _more_
     */
    public void setTrajStartPointType(RealTupleType type) {
        trajStartPointType = type;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public RealTupleType getTrajStartPointType() {
        return trajStartPointType;
    }

    /**
     * _more_
     *
     * @param pts _more_
     */
    public void setTrajStartPoints(float[][] pts) {
        trajStartPoints = pts;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float[][] getTrajStartPoints() {
        return trajStartPoints;
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
        if ( !isCartesianWind()
                && (flowYMap != null)
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

        spdIndex = 1;

        // if data is u,v (so far as you can tell by units)
        if ((Unit.canConvert(units[0],
                             CommonUnit.meterPerSecond) && Unit.canConvert(
                                 units[1],
                                 CommonUnit.meterPerSecond))
                || ((units[0] == null) && (units[1] == null))
                || (CommonUnit.dimensionless.equals(units[0])
                    && CommonUnit.dimensionless.equals(units[1]))) {
            flowXMap = new ScalarMap(realTypes[0], (useFlow1
                    ? Display.Flow1X
                    : Display.Flow2X));
            flowYMap = new ScalarMap(realTypes[1], (useFlow1
                    ? Display.Flow1Y
                    : Display.Flow2Y));
            spdIndex = 0;  // color speed by u component
            if (coloredByAnother || useSpeedForColor) {
                spdIndex = 2;
            }
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

                                          public void controlChanged(
                                          ScalarMapControlEvent event)
                                                  throws RemoteException,
                                                  VisADException {

                                              int id = event.getId();

                                              if ((id == event.CONTROL_ADDED)
                                                      || (id == event
                                                      .CONTROL_REPLACED)) {
                                                  flowControl =
                                                  (FlowControl) flowYMap.getControl();
                                                  flowControl.enableStreamlines(
                                                  isStreamlines);
                                                  flowControl.enableTrajectory(
                                                  isTrajectories);
                                                  flowControl.setAutoScale(
                                                  autoScale);
                                                  flowControl.setStreamlineDensity(
                                                  streamlineDensity);
                                                  flowControl.setAdjustFlowToEarth(
                                                  adjustFlow);
                                                  flowControl.setBarbOrientation(
                                                  barborientation);
                                                  if (isTrajectories || streamline) {
                                                      resetTrojectories();
                                                  }
                                                  adjustScale(flowscale);
                                              }
                                          }

                                          public void mapChanged(
                                          ScalarMapEvent event) {}  // ignore
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
        RealType      rt0       = (RealType) rtt.getComponents()[0];
        RealType      rt1       = (RealType) rtt.getComponents()[1];
        int           threeDDim = (coloredByAnother || ignoreExtraParameters)
                                  ? 3
                                  : 2;
        if (is3D && (rtt.getDimension() == 3)) {
            RealType rt = (RealType) rtt.getComponents()[2];
            if (rt.getDefaultUnit().isConvertible(
                    CommonUnit.meterPerSecond)) {
                set3DFlow(true);
            } else {
                set3DFlow(false);
            }
        } else {
            set3DFlow(rtt.getDimension() > threeDDim);
        }
        // uncomment to determine ad-hoc rather than programatically
        //coloredByAnother = coloredByAnother && (rtt.getDimension() > 2);
        if (rt0.getDefaultUnit().isConvertible(CommonUnit.meterPerSecond)
                && rt1.getDefaultUnit().isConvertible(CommonUnit.degree)) {
            FieldImpl spdFimpl   = DerivedGridFactory.getUComponent(field);
            FieldImpl dirFimpl   = DerivedGridFactory.getVComponent(field);
            FieldImpl otherFimpl = null;

            if (coloredByAnother && !useSpeedForColor) {
                otherFimpl = DerivedGridFactory.getVComponent(field);;
                dirFimpl = DerivedGridFactory.getComponent(spdFimpl, 1, true);
                spdFimpl = DerivedGridFactory.getComponent(spdFimpl, 0, true);
            }
            String oname = spdFimpl.getType().prettyString();
            FieldImpl uFimpl =
                GridUtil.setParamType(GridMath.multiply(spdFimpl,
                                                        (FieldImpl) dirFimpl.sin()), oname
                                                            + "Uspd");
            FieldImpl vFimpl =
                GridUtil.setParamType(GridMath.multiply(spdFimpl,
                                                        (FieldImpl) dirFimpl.cos()), oname
                                                            + "Vspd");;
            field = DerivedGridFactory.createFlowVectors(uFimpl, vFimpl);

            if (useSpeedForColor) {
                field = DerivedGridFactory.combineGrids(field, spdFimpl);
            } else if (coloredByAnother) {
                field = DerivedGridFactory.combineGrids(field, otherFimpl);
            }

            tt  = GridUtil.getParamType(field);
            rtt = new RealTupleType(tt.getRealComponents());
        } else if (rt0.getDefaultUnit().isConvertible(CommonUnit.degree)
                   && rt1.getDefaultUnit().isConvertible(
                       CommonUnit.meterPerSecond)) {
            FieldImpl spdFimpl   = DerivedGridFactory.getVComponent(field);
            FieldImpl dirFimpl   = DerivedGridFactory.getUComponent(field);
            FieldImpl otherFimpl = null;

            if (coloredByAnother && !useSpeedForColor) {
                otherFimpl = DerivedGridFactory.getUComponent(field);
                dirFimpl = DerivedGridFactory.getComponent(spdFimpl, 0, true);
                spdFimpl = DerivedGridFactory.getComponent(spdFimpl, 1, true);
            }
            String oname = spdFimpl.getType().prettyString();
            FieldImpl uFimpl =
                GridUtil.setParamType(GridMath.multiply(spdFimpl,
                                                        (FieldImpl) dirFimpl.sin()), oname
                                                            + "Uspd");
            FieldImpl vFimpl =
                GridUtil.setParamType(GridMath.multiply(spdFimpl,
                                                        (FieldImpl) dirFimpl.cos()), oname
                                                            + "Vcomp");
            field = DerivedGridFactory.createFlowVectors(uFimpl, vFimpl);

            if (useSpeedForColor) {
                field = DerivedGridFactory.combineGrids(field, spdFimpl);
            } else if (coloredByAnother) {
                field = DerivedGridFactory.combineGrids(field, otherFimpl);
            }

            tt  = GridUtil.getParamType(field);
            rtt = new RealTupleType(tt.getRealComponents());

        } else if (coloredByAnother || useSpeedForColor) {
            // get the RealType of the range data

            //if vector is formed by u and v, we need to create the speed field
            // for the colorby
            if (rt0.getDefaultUnit().isConvertible(CommonUnit.meterPerSecond)
                    && rt1.getDefaultUnit().isConvertible(
                        CommonUnit.meterPerSecond)
                    && useSpeedForColor) {
                FieldImpl uFimpl = DerivedGridFactory.getUComponent(field);
                FieldImpl vFimpl = DerivedGridFactory.getVComponent(field);

                if(!GridUtil.getParamUnits(vFimpl)[0].isConvertible(CommonUnit.meterPerSecond)){
                    if(GridUtil.getParamUnits(uFimpl).length == 2 &&
                            GridUtil.getParamUnits(uFimpl)[0].isConvertible(CommonUnit.meterPerSecond) &&
                            GridUtil.getParamUnits(uFimpl)[1].isConvertible(CommonUnit.meterPerSecond)){
                        vFimpl = DerivedGridFactory.getVComponent(uFimpl);
                        uFimpl = DerivedGridFactory.getUComponent(uFimpl);
                    }
                }
                FieldImpl speedImpl =
                    DerivedGridFactory.createWindSpeed(uFimpl, vFimpl);
                field = DerivedGridFactory.combineGrids(field, speedImpl);
                tt    = GridUtil.getParamType(field);
                rtt   = new RealTupleType(tt.getRealComponents());
            }
        }

        if (useSpeedForColor || coloredByAnother) {
            RealType rgbType = (RealType) rtt.getComponent(rtt.getDimension()
                                   - 1);
            setRGBRealType(rgbType);
        }

        //setType(rtt);

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

        setType(rtt);

    }

    /**
     * _more_
     *
     * @param topo _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void loadTopoData(FieldImpl topo)
            throws VisADException, RemoteException {
        if (flowControl != null) {
            TrajectoryParams tparm = flowControl.getTrajectoryParams();
            tparm.setTerrain(((GeoGridFlatField) topo.getSample(0)));
        }
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
     * _more_
     *
     * @param scale _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void adjustTrajOffsetLength(float scale)
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
        // set RealType to color by speed
        if (useSpeedForColor && !isCartesian) {
            //if ( !coloredByAnother) {
            try {
                setRGBRealType(
                    (RealType) flowRealTupleType.getComponent(spdIndex));
            } catch (Exception e) {}
        }
    }

    /**
     * Set whether this GridDisplayable should ignore extra parameters
     *
     * @param yesno true if extra params should be ignored
     */
    public void setIgnoreExtraParameters(boolean yesno) {
        ignoreExtraParameters = yesno;
    }

    /**
     * _more_
     *
     *
     * @param types _more_
     * @param stp _more_
     */
    public void setStartPoints(RealTupleType types, float[][] stp) {
        trajStartPoints    = stp;
        trajStartPointType = types;
        if (flowControl != null) {
            TrajectoryParams tParm = flowControl.getTrajectoryParams();
            tParm.setStartPoints(types, stp);
        }
    }

    /**
     * _more_
     *
     * @param startLevel _more_
     */
    public void setStartLevel(int startLevel) {
        if (flowControl == null) {
            return;
        }
        TrajectoryParams tParm = flowControl.getTrajectoryParams();
        tParm.setZStartIndex(startLevel);
        tParm.setZStartSkip(10);

    }

}
