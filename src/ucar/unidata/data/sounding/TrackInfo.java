/*
 * $Id: TrackInfo.java,v 1.4 2007/08/06 17:02:27 jeffmc Exp $
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

package ucar.unidata.data.sounding;


import ucar.ma2.Range;

import ucar.unidata.data.BadDataException;
import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.VarInfo;
import ucar.unidata.data.point.*;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import ucar.visad.UtcDate;
import ucar.visad.Util;
import ucar.visad.quantities.*;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.georef.*;

import visad.util.DataUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Class TrackInfo Provides access to a track or trajectory
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.4 $
 */
public abstract class TrackInfo {

    /** _more_          */
    protected String varTime;

    /** _more_          */
    protected String varLatitude;

    /** _more_          */
    protected String varLongitude;

    /** _more_          */
    protected String varAltitude;


    /** RealType name for time */
    public static final String TIME_TYPE = "track_time";

    /** RealType name for latitude */
    public static final String LAT_TYPE = "track_lat";

    /** RealType name for longitude */
    public static final String LON_TYPE = "track_lon";

    /** RealType name for altitude */
    public static final String ALT_TYPE = "track_alt";

    /** cached time values */
    protected Hashtable cachedTimeVals = new Hashtable();

    /** The adapater */
    protected TrackAdapter adapter;


    /** lat/lon/altitude set */
    protected GriddedSet llaSet = null;

    /** The last range when we create the llaSet */
    protected Range lastSpatialSetRange = null;

    /** bearing class for bearing calculations */
    protected Bearing workBearing = new Bearing();

    /** base time for the sounding release */
    DateTime startTime;

    /** base time for the sounding release */
    DateTime endTime;

    /** All the variables */
    protected List variables = new ArrayList();

    /** Name of track */
    protected String trackName;


    /**
     * ctor
     *
     *
     * @param adapter The adapter
     * @param name name of track
     *
     * @throws Exception On badness
     */
    public TrackInfo(TrackAdapter adapter, String name) throws Exception {
        this.adapter   = adapter;
        this.trackName = name;
    }



    /**
     * Add variable
     *
     * @param variable the variable
     */
    protected void addVariable(VarInfo variable) {
        variables.add(variable);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<VarInfo> getVariables() {
        return variables;
    }

    /**
     * Get the starting time of this track.
     *
     * @return starting time
     */
    public DateTime getStartTime() {
        return startTime;
    }

    /**
     * Get the ending time of this track.
     *
     * @return ending time
     */
    public DateTime getEndTime() {
        return endTime;
    }



    /**
     * How many points in track
     *
     * @return num points in track
     *
     * @throws Exception _more_
     */
    public int getNumberPoints() throws Exception {
        float[] lats = getLatitude(null);
        if (lats == null) {
            return 0;
        }
        return lats.length;
    }





    /**
     * Make the earth spatial domain
     *
     *
     * @param range The data range of the request
     * @return The spatial domain
     *
     * @throws Exception On badness
     */
    protected GriddedSet makeEarthDomainSet(Range range) throws Exception {
        return ucar.visad.Util.makeEarthDomainSet(getLatitude(range),
                getLongitude(range), getAltitude(range));
    }



    /**
     * Returns a track for the variable name specified. Returned track is
     * of type:
     * <pre>
     * ((Latitude, Longitude, Altitude) -> (variable, Time)
     * </pre>
     *
     * @param variable   variable to get
     * @param range The data range of the request
     *
     * @return FlatField of the type above.
     *
     * @throws Exception On badness
     */
    public synchronized FlatField getTrackWithTime(String variable,
            Range range)
            throws Exception {
        Trace.call1("TrackAdapter.getTrackWithTime");
        if (range == null) {
            range = getFullRange();
        }
        FlatField trace = getTrack(variable, range);
        if (trace == null) {
            return null;
        }
        double[][] vals = trace.getValues(false);
        RealType varType =
            (RealType) DataUtility.getFlatRangeType(trace).getComponent(0);

        Unit       timeUnit     = getTimeUnit();
        double[][] newRangeVals = new double[2][vals[0].length];
        double[]   timeVals     = getTimeVals(range);
        newRangeVals[0] = vals[0];
        RealType timeType = getVarType(RealType.Time, timeUnit, timeVals[0]);
        RealTupleType rangeType = new RealTupleType(getVarType(varType),
                                      timeType);
        if ( !getTimeUnit().equals(timeType.getDefaultUnit())) {
            Unit tmpUnit = timeType.getDefaultUnit();
            timeVals = tmpUnit.toThis(timeVals, timeUnit);
            timeUnit = tmpUnit;
        }
        newRangeVals[1] = timeVals;

        Set[] rangeSets = new Set[2];
        rangeSets[0] = new DoubleSet(new SetType(rangeType.getComponent(0)));
        rangeSets[1] = new DoubleSet(new SetType(rangeType.getComponent(1)));

        GriddedSet llaSet = getSpatialSet(range);
        FunctionType newType =
            new FunctionType(((SetType) llaSet.getType()).getDomain(),
                             rangeType);
        FlatField timeTrack = new FlatField(newType, llaSet,
                                            (CoordinateSystem) null,
                                            rangeSets,
                                            new Unit[] {
                                                varType.getDefaultUnit(),
                timeUnit });
        timeTrack.setSamples(newRangeVals, false);
        Trace.call2("TrackAdapter.getTrackWithTime");
        return timeTrack;
    }


    /**
     * Get the time values for the range
     *
     * @param range  range to use
     *
     * @return time values in range
     *
     * @throws Exception   problem getting time values
     */
    public double[] getTimeVals(Range range) throws Exception {
        double[] timeVals = (double[]) cachedTimeVals.get(range);
        if (timeVals == null) {
            timeVals = getTime(range);
            cachedTimeVals.put(range, timeVals);
        }
        return timeVals;
    }

    /**
     * What is the time unit
     *
     * @return time unit
     *
     * @throws Exception On badness
     */
    protected Unit getTimeUnit() throws Exception {
        return CommonUnit.secondsSinceTheEpoch;
    }

    /**
     * Get the time for each ob
     *
     * @param range subset on range
     *
     * @return time values
     *
     * @throws Exception On badness
     */
    protected double[] getTime(Range range) throws Exception {
        return getDoubleData(range, varTime);
    }

    /**
     * Get latitude values
     *
     * @param range subset on range. may be null
     *
     * @return latitude values
     *
     * @throws Exception On badness
     */
    protected float[] getLatitude(Range range) throws Exception {
        return getFloatData(range, varLatitude);
    }

    /**
     * get longitude values
     *
     * @param range subset on range. may be null
     *
     * @return longitude values
     *
     * @throws Exception On badness
     */
    protected float[] getLongitude(Range range) throws Exception {
        return getFloatData(range, varLongitude);
    }


    /**
     * get altitude values
     *
     * @param range subset on range. May be null
     *
     * @return altitude values
     *
     * @throws Exception On badness
     */
    protected float[] getAltitude(Range range) throws Exception {
        return getFloatData(range, varAltitude);
    }


    /**
     * Utility for getting data
     *
     * @param range The range
     * @param var The variable
     *
     * @return The data
     *
     * @throws Exception On badness
     */
    protected float[] getFloatData(Range range, VarInfo var)
            throws Exception {
        return getFloatData(range, var.getShortName());
    }

    /**
     * Get string values for variable
     *
     * @param range the range. May be null
     * @param var The var
     *
     * @return String values
     *
     * @throws Exception On badness
     */
    protected String[] getStringData(Range range, VarInfo var)
            throws Exception {
        return getStringData(range, var.getShortName());
    }


    /**
     * Get the data values for range and var
     *
     * @param range The range. May be null.
     * @param var The variable
     *
     * @return Values
     *
     * @throws Exception On badness
     */
    protected abstract float[] getFloatData(Range range, String var)
     throws Exception;


    /**
     * _more_
     *
     * @param range _more_
     * @param var _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected double[] getDoubleData(Range range, String var)
            throws Exception {
        return Misc.arrayToDouble(getFloatData(range, var));
    }

    /**
     * Get string values
     *
     * @param range The range. May be null.
     * @param var The variable
     *
     * @return Values
     *
     *
     * @throws Exception On badness
     */
    protected abstract String[] getStringData(Range range, String var)
     throws Exception;





    /**
     * Take a FlatField of data and turn it into a field of PointObs.
     *
     *
     * @param range The data range of the request
     * @return field of PointObs
     * @throws Exception On badness
     */
    public synchronized FieldImpl getPointObTrack(Range range)
            throws Exception {

        //        Trace.startTrace();
        Trace.call1("TrackAdapter.getPointObTrack");
        Object loadId = JobManager.getManager().startLoad("TrackAdapter");
        if (range == null) {
            range = getFullRange();
        }

        float[]  lats     = getLatitude(range);
        float[]  lons     = getLongitude(range);


        float[]  alts     = getAltitude(range);
        double[] timeVals = getTimeVals(range);
        int      numObs   = lats.length;

        timeVals = CommonUnit.secondsSinceTheEpoch.toThis(timeVals,
                getTimeUnit());
        List<VarInfo>    varsToUse  = getVarsToUse();
        int     numReals   = countReals(varsToUse);
        int     numStrings = varsToUse.size() - numReals;
        boolean allReals   = numStrings == 0;
        int     numVars    = varsToUse.size();
        Unit[]  units      = new Unit[numVars];

        for (int varIdx = 0; varIdx < numVars; varIdx++) {
            VarInfo var =  varsToUse.get(varIdx);
            units[varIdx] = var.getUnit();
        }

        Trace.msg("TrackAdapter #obs: " + getNumberPoints() + " vars:"
                  + numVars);

        RealType      dirType        = Direction.getRealType();
        EarthLocation lastEL         = null;
        List          locations      = new ArrayList();
        List          times          = new ArrayList();
        List          tuples         = new ArrayList();
        List          reals          = new ArrayList();
        List          strings        = new ArrayList();
        List          bearings       = new ArrayList();
        List          stringTypeList = new ArrayList();



        for (int obIdx = 0; obIdx < numObs; obIdx++) {
            EarthLocation location =
                new EarthLocationTuple(new Real(RealType.Latitude,
                    lats[obIdx]), new Real(RealType.Longitude, lons[obIdx]),
                                  new Real(RealType.Altitude, alts[obIdx]));
            if (obIdx == 0) {
                lastEL = location;
            }
            locations.add(location);

            double dirVal = Util.calculateBearing(lastEL, location,
                                workBearing).getAngle();


            lastEL = location;
            Real bearing = new Real(dirType, dirVal);
            bearings.add(bearing);
            times.add(new DateTime(timeVals[obIdx]));
            //Do +1 for the bearing
            Data[] tupleArray = (allReals == true)
                                ? new Real[numVars + 1]
                                : new Data[numVars + 1];
            tuples.add(tupleArray);
            double[] realArray = new double[numReals + 1];
            reals.add(realArray);
            realArray[realArray.length - 1] = dirVal;
            strings.add(new String[numStrings]);
        }

        Trace.call1("TrackAdapter.reading data");


        boolean doColumnOriented = true;
        //            doColumnOriented = false;
        if (doColumnOriented) {
            int realCnt   = 0;
            int stringCnt = 0;
            for (int varIdx = 0; varIdx < numVars; varIdx++) {
                if ( !JobManager.getManager().canContinue(loadId)) {
                    return null;
                }
                VarInfo var =  varsToUse.get(varIdx);
                if (var.getIsNumeric()) {
                    float[] fvalues = getFloatData(range, var.getShortName());
                    if (var.getRealType() == null) {
                        //???
                    }

                    Data[] firstTuple = null;
                    for (int obIdx = 0; obIdx < numObs; obIdx++) {
                        Data[] tupleArray = (Data[]) tuples.get(obIdx);
                        ((double[]) reals.get(obIdx))[realCnt] =
                            fvalues[obIdx];
                        if (firstTuple != null) {
                            tupleArray[varIdx] =
                                ((Real) firstTuple[varIdx]).cloneButValue(
                                    fvalues[obIdx]);
                        } else {
                            firstTuple         = tupleArray;
                            tupleArray[varIdx] = (var.getUnit() == null)
                                    ? new Real(var.getRealType(),
                                    fvalues[obIdx])
                                    : new Real(var.getRealType(),
                                    fvalues[obIdx], var.getUnit());
                        }
                    }
                    realCnt++;
                } else {
                    String[] svalues = getStringData(range,
                                           var.getShortName());
                    for (int obIdx = 0; obIdx < numObs; obIdx++) {
                        Data[] tupleArray = (Data[]) tuples.get(obIdx);
                        tupleArray[varIdx] = new Text(svalues[obIdx]);
                        ((String[]) strings.get(obIdx))[stringCnt] =
                            svalues[obIdx];
                    }
                    stringCnt++;
                }
            }
        }

        Trace.call2("TrackAdapter.reading data");
        Trace.call1("TrackAdapter.processing data", " all reals?" + allReals);
        PointOb[]     obs     = new PointObTuple[locations.size()];
        TupleType     tt      = null;
        RealTupleType rtt     = null;
        TupleType     finalTT = null;
        for (int obIdx = 0; obIdx < obs.length; obIdx++) {
            Data[]   tupleArray  = (Data[]) tuples.get(obIdx);
            double[] realArray   = (double[]) reals.get(obIdx);
            String[] stringArray = (String[]) strings.get(obIdx);
            tupleArray[tupleArray.length - 1] = (Data) bearings.get(obIdx);
            if (tt == null) {
                tt = Tuple.buildTupleType(tupleArray);
                if (allReals) {
                    rtt = (RealTupleType) tt;
                }
            }
            Data rest = (allReals == true)
                        ? new RealTuple(rtt, (Real[]) tupleArray, null,
                                        units, false)
                        : new Tuple(tt, tupleArray, false, false);

            PointObTuple pot = null;
            if (finalTT == null) {
                pot = new PointObTuple((EarthLocation) locations.get(obIdx),
                                       (DateTime) times.get(obIdx), rest);
                finalTT = Tuple.buildTupleType(pot.getComponents());
            } else {
                pot = new PointObTuple((EarthLocation) locations.get(obIdx),
                                       (DateTime) times.get(obIdx), rest,
                                       finalTT, false);

            }
            obs[obIdx] = pot;
        }


        Trace.call2("TrackAdapter.processing data");
        //            Tuple.runCheck = !Tuple.runCheck;



        Integer1DSet indexSet =
            new Integer1DSet(RealType.getRealType("index"), numObs);
        FieldImpl retField =
            new FieldImpl(
                new FunctionType(
                    ((SetType) indexSet.getType()).getDomain(),
                    obs[0].getType()), indexSet);
        retField.setSamples(obs, false, false);

        Trace.call2("TrackAdapter.getPointObTrack");
        //      Trace.stopTrace();
        return retField;

    }




    /**
     * Utility to find the variable with the given name
     *
     * @param variableName The name
     *
     * @return The variable.
     */
    protected VarInfo getDataVariable(String variableName) {
        //Jump through some hoops for legacy bundles
        String[] vars = { variableName, variableName.toLowerCase() };
        for (int dummyIdx = 0; dummyIdx < vars.length; dummyIdx++) {
            for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
                VarInfo theVar = (VarInfo) variables.get(varIdx);
                if (vars[dummyIdx].equals(theVar.getName())) {
                    return theVar;
                }
            }

            for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
                VarInfo theVar = (VarInfo) variables.get(varIdx);
                if (vars[dummyIdx].equals(theVar.getDescription())) {
                    return theVar;
                }
            }
        }

        throw new IllegalArgumentException("Unknown variable: "
                                           + variableName);
    }





    /**
     * Get the appropriate RealType for the particular variable.  Used
     * to get alternate names for lat/lon/alt
     *
     * @param varToCheck   variable to check
     *
     * @return new variable or the original
     */
    protected RealType getVarType(RealType varToCheck) {
        return getVarType(varToCheck, null, Double.NaN);
    }


    /**
     * Get the name of the track
     *
     * @return Track name
     */
    public String getTrackName() {
        return trackName;
    }

    /**
     * _more_
     *
     * @param lon _more_
     * @param lat _more_
     * @param alt _more_
     * @param time _more_
     */
    public void setCoordinateVars(String lon, String lat, String alt,
                                  String time) {
        varLongitude = lon;
        varLatitude  = lat;
        varAltitude  = alt;
        varTime      = time;
    }

    /**
     * Get the appropriate RealType for the particular variable.  Used
     * to get alternate names for lat/lon/alt
     *
     * @param varToCheck   variable to check
     * @param unit   unit for return RealType
     * @param sampleValue sample value for comparing RealTypes
     *
     * @return new variable or the original
     */
    protected RealType getVarType(RealType varToCheck, Unit unit,
                                  double sampleValue) {
        RealType varType = null;
        if (varToCheck.equals(RealType.Altitude)) {
            varType = RealType.getRealType(ALT_TYPE, CommonUnit.meter);
        } else if (varToCheck.equals(RealType.Latitude)) {
            varType = RealType.getRealType(LAT_TYPE, CommonUnits.DEGREE);
        } else if (varToCheck.equals(RealType.Longitude)) {
            varType = RealType.getRealType(LON_TYPE, CommonUnits.DEGREE);
        } else if (varToCheck.equals(RealType.Time)) {
            try {
                Real value = new Real(RealType.Time, sampleValue, unit);
                double sinceEpoch =
                    value.getValue(CommonUnit.secondsSinceTheEpoch);
                int years = (int) (sinceEpoch / (365 * 24 * 3600));
                int year  = 1970 + (years);
                Unit timeUnit = DataUtil.parseUnit("seconds since " + year
                                    + "-1-1 0:00:00 0:00");
                varType = RealType.getRealType(DataUtil.cleanName(TIME_TYPE
                        + "_" + timeUnit), timeUnit);
            } catch (Exception excp) {
                varType = RealType.getRealType(TIME_TYPE,
                        CommonUnit.secondsSinceTheEpoch);
            }
        } else {
            varType = varToCheck;
        }
        return varType;
    }



    /**
     * Get the default range of data
     *
     * @return range
     *
     * @throws Exception On badness
     */
    protected Range getDataRange() throws Exception {
        //TODO: check this make sure the indices are correct
        return new Range(0, getNumberPoints() - 1, 1);
    }


    /**
     * Get the full range but clipped to the adapters lastNMinutes
     *
     * @return range
     *
     * @throws Exception On badness
     */
    protected Range getFullRange() throws Exception {
        Range range = getDataRange();
        if (adapter.getLastNMinutes() > 0) {
            long endTime = (long) getStartTime().getValue(
                               CommonUnit.secondsSinceTheEpoch);
            long     startTime = endTime - adapter.getLastNMinutes() * 60;
            double[] timeVals  = getTimeVals(range);
            timeVals = CommonUnit.secondsSinceTheEpoch.toThis(timeVals,
                    getTimeUnit());

            int beginIndex = timeVals.length - 1;
            while (beginIndex >= 0) {
                if (startTime > timeVals[beginIndex]) {
                    break;
                }
                beginIndex--;
            }
            range = new Range(beginIndex, range.last(), range.stride());
        }
        if (adapter.getStride() > 1) {
            range = new Range(range.first(), range.last(),
                              adapter.getStride());
        }
        return range;
    }

    /**
     * Get list of VarInfos to use
     *
     * @return List of vars
     */
    protected List<VarInfo> getVarsToUse() {
        List<VarInfo> varsToUse = new ArrayList<VarInfo>();
        for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
            VarInfo var = (VarInfo) variables.get(varIdx);
            if (includeInPointData(var.getShortName())) {
                varsToUse.add(var);
            }
        }
        return varsToUse;
    }

    /**
     * How many of the given vars are numeric
     *
     * @param vars List of vars
     *
     * @return how many numeric
     */
    protected int countReals(List vars) {
        int numReals = 0;
        for (int varIdx = 0; varIdx < vars.size(); varIdx++) {
            VarInfo var = (VarInfo) vars.get(varIdx);
            if (var.getIsNumeric()) {
                numReals++;
            }
        }

        return numReals;
    }




    /**
     * Returns a track for the variable name specified. Returned track is
     * of type:
     * <pre>
     * ((Latitude, Longitude, Altitude) -> (variable)
     * </pre>
     *
     * @param variableName    variable of data
     * @param range The data range of the request
     *
     * @return FlatField of the type above.
     *
     * @throws Exception On badness
     */
    public synchronized FlatField getTrack(String variableName, Range range)
            throws Exception {
        if (range == null) {
            range = getFullRange();
        }
        Unit unit = null;
        //Look for the special variables.
        //We will replace this when we can do a getLatitudeVarInfo, etc.
        //call
        float[]  value  = null;
        double[] dvalue = null;
        if (Misc.equals(variableName, varTime)) {
            unit   = getTimeUnit();
            dvalue = getTime(range);
        } else {
            VarInfo var = VarInfo.getVarInfo(variableName, variables);
            unit  = var.getUnit();
            value = getFloatData(range, var.getShortName());
        }


        float[][]  values    = new float[1][];
        double[][] valuesD   = new double[1][];
        Set[]      rangeSets = null;
        //If the fixed VAR_ names are just Latitude, Longitude, Altitude
        //we will get an error in the display from this real type.
        RealType type = DataUtil.makeRealType(variableName, unit);
        if (type == null) {
            throw new VisADException(variableName + " not available");
        }

        if (dvalue == null) {
            values[0] = value;
            rangeSets = null;
        } else {
            valuesD[0]   = dvalue;
            rangeSets    = new Set[1];
            rangeSets[0] = new DoubleSet(new SetType(type));
        }

        FunctionType ftype =
            new FunctionType(RealTupleType.LatitudeLongitudeAltitude,
                             getVarType(type));
        FlatField field = new FlatField(ftype, getSpatialSet(range),
                                        (CoordinateSystem) null, rangeSets,
                                        new Unit[] { unit });
        if (dvalue == null) {
            field.setSamples(values, false);
        } else {
            field.setSamples(valuesD, false);
        }
        return field;
    }


    /**
     * Returns the lat/lon/alt values as a GriddedSet with manifold
     * dimension 1.
     *
     *
     * @param range The data range of the request
     * @return set of lat/lon/alt points.
     *
     * @throws Exception On badness
     */
    protected GriddedSet getSpatialSet(Range range) throws Exception {
        if ((llaSet == null) || (range == null)
                || (lastSpatialSetRange == null)
                || !Misc.equals(lastSpatialSetRange, range)) {
            llaSet = makeEarthDomainSet(range);
        }
        lastSpatialSetRange = range;
        return llaSet;
    }

    /**
     * Should we include the given var in the point data
     *
     * @param varName VarInfo name
     *
     * @return Include in point data
     */
    public boolean includeInPointData(String varName) {
        return adapter.includeInPointData(varName);
    }

    /**
     * get the data
     *
     * @return the data
     *
     * @throws Exception On badness
     */
    public Data getAerologicalDiagramData() throws Exception {

        VarInfo pressureVar = null;
        VarInfo tempVar     = null;
        VarInfo dewpointVar = null;
        VarInfo wspdVar     = null;
        VarInfo wdirVar     = null;



        for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
            VarInfo var       = (VarInfo) variables.get(varIdx);
            String  name      = var.getShortName();
            String  canonical = DataAlias.aliasToCanonical(name);
            if (canonical == null) {
                continue;
            }
            if (canonical.equals("PRESSURE")) {
                pressureVar = var;
            } else if (canonical.equals("TEMP")) {
                tempVar = var;
            } else if (canonical.equals("DEWPOINT")) {
                dewpointVar = var;
            } else if (canonical.equals("SPEED")) {
                wspdVar = var;
            } else if (canonical.equals("DIR")) {
                wdirVar = var;
            }
        }



        String missing = "";
        if (pressureVar == null) {
            missing = missing + "\nPressure";
        }
        if (dewpointVar == null) {
            missing = missing + "\nDewpoint";
        }
        if (wspdVar == null) {
            missing = missing + "\nWind speed";
        }
        if (wdirVar == null) {
            missing = missing + "\nWind direction";
        }
        if (missing.length() > 0) {
            throw new VisADException(
                "Couldn't find all needed variables for aerological diagram:"
                + missing);
        }


        TupleType addType = new TupleType(new MathType[] {
                                AirPressure.getRealType(),
                                AirTemperature.getRealType(),
                                DewPoint.getRealType(),
                                PolarHorizontalWind.getRealTupleType(),
                                RealTupleType.LatitudeLongitudeAltitude });

        Range        range        = getFullRange();

        FunctionType addFType     = new FunctionType(RealType.Time, addType);
        GriddedSet   llaSet       = getSpatialSet(getFullRange());
        Unit[]       llaUnits     = llaSet.getSetUnits();
        Unit         tempUnit     = tempVar.getUnit();
        Unit         dewpointUnit = dewpointVar.getUnit();
        Unit[]       addUnits     = new Unit[] {
            pressureVar.getUnit(),
            (Unit.canConvert(tempUnit, CommonUnits.CELSIUS)
             ? tempUnit
             : CommonUnits.CELSIUS), (Unit.canConvert(dewpointUnit,
                 CommonUnits.CELSIUS)
                                      ? dewpointUnit
                                      : CommonUnits.CELSIUS), wspdVar
                                          .getUnit(),
            wdirVar.getUnit(), llaUnits[0], llaUnits[1], llaUnits[2]
        };
        float[][] llaSamples = llaSet.getSamples();


        double[]  timeVals   = getTimeVals(range);
        Gridded1DDoubleSet timeSet = new Gridded1DDoubleSet(RealType.Time,
                                         new double[][] {
            timeVals
        }, timeVals.length, (CoordinateSystem) null,
           new Unit[] { getTimeUnit() }, (ErrorEstimate[]) null);
        FlatField addField = new FlatField(addFType, timeSet,
                                           (CoordinateSystem[]) null,
                                           (Set[]) null, addUnits);

        addField.setSamples(new float[][] {
            getFloatData(range, pressureVar), getFloatData(range, tempVar),
            getFloatData(range, dewpointVar), getFloatData(range, wspdVar),
            getFloatData(range, wdirVar), llaSamples[0], llaSamples[1],
            llaSamples[2]
        });
        return addField;
    }


    /**
     * Make the RAOB
     *
     * @return The RAOB_
     *
     * @throws Exception On badness
     */
    protected RAOB makeRAOB() throws Exception {
        RAOB      raob          = new RAOB();
        FlatField ff = (FlatField) getAerologicalDiagramData();
        float[][] samples       = ff.getFloats(true);
        int       presIndex     = 0;
        int       tempIndex     = 1;
        int       dewpointIndex = 2;
        int       wspdIndex     = 3;
        int       wdirIndex     = 4;
        int       altIndex      = 7;

        RAOB.MandatoryPressureProfile mpp =
            RAOB.newMandatoryPressureProfile(
                AirPressure.getRealType().getDefaultUnit(),
                samples[presIndex],
                AirTemperature.getRealType().getDefaultUnit(),
                samples[tempIndex], DewPoint.getRealType().getDefaultUnit(),
                samples[dewpointIndex],
                PolarHorizontalWind.getSpeedRealType().getDefaultUnit(),
                samples[wspdIndex],
                PolarHorizontalWind.getDirectionRealType().getDefaultUnit(),
                samples[wdirIndex], DataUtil.parseUnit("gpm"), samples[altIndex]);

        raob.setMandatoryPressureProfile(mpp);

        return raob;
    }




}

