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

import ucar.unidata.data.point.*;

import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import ucar.visad.UtcDate;
import ucar.visad.Util;
import ucar.visad.quantities.*;

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

    /** Fixed var name for lat */
    public static final String VAR_LATITUDE = "Latitude";

    /** Fixed var name for lon */
    public static final String VAR_LONGITUDE = "Longitude";

    /** Fixed var name for alt */
    public static final String VAR_ALTITUDE = "Altitude";

    /** Fixed var name for time */
    public static final String VAR_TIME = "Time";

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

    /** List of parameter names */
    protected List parameterNames = new ArrayList();

    /** List of parameter descriptions */
    protected List parameterDescriptions = new ArrayList();

    /** List of parameter categories */
    protected List parameterCategories = new ArrayList();


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
    protected void addVariable(Variable variable) {
        variables.add(variable);
        parameterNames.add(variable.getName());
        parameterDescriptions.add(variable.getDescription());
        parameterCategories.add(variable.getCategory());
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
     */
    public abstract int getNumberPoints();





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
        return ucar.visad.Util.makeEarthDomainSet(
                                                  getLatitude(range),
                                                  getLongitude(range),
                                                  getAltitude(range));
    }


    /**
     *  Get the ParameterNames property.
     *
     *  @return The ParameterNames
     */
    public List getParameterNames() {
        return parameterNames;
    }



    /**
     *  Get the ParameterDescriptions property.
     *
     *  @return The ParameterDescriptions
     */
    public List getParameterDescriptions() {
        return parameterDescriptions;
    }

    /**
     *  Get the ParameterDescriptions property.
     *
     *  @return The ParameterDescriptions
     */
    public List getParameterCategories() {
        return parameterCategories;
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
        RealTupleType rangeType = new RealTupleType(getVarType(varType),timeType);
        if (!getTimeUnit().equals(timeType.getDefaultUnit())) {
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
    protected abstract Unit getTimeUnit() throws Exception;

    /**
     * Get the time for each ob
     *
     * @param range subset on range
     *
     * @return time values
     *
     * @throws Exception On badness
     */
    protected abstract double[] getTime(Range range) throws Exception;

    /**
     * Get latitude values
     *
     * @param range subset on range. may be null
     *
     * @return latitude values
     *
     * @throws Exception On badness
     */
    protected abstract float[] getLatitude(Range range) throws Exception;

    /**
     * get longitude values
     *
     * @param range subset on range. may be null
     *
     * @return longitude values
     *
     * @throws Exception On badness
     */
    protected abstract float[] getLongitude(Range range) throws Exception;


    /**
     * get altitude values
     *
     * @param range subset on range. May be null
     *
     * @return altitude values
     *
     * @throws Exception On badness
     */
    protected abstract float[] getAltitude(Range range) throws Exception;


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
    protected float[] getData(Range range, Variable var) throws Exception {
        return getData(range, var.getShortName());
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
    protected String[] getStrings(Range range, Variable var)
            throws Exception {
        return getStrings(range, var.getShortName());
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
    protected abstract float[] getData(Range range, String var)
     throws Exception;

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
    protected abstract String[] getStrings(Range range, String var)
     throws Exception;




    /**
     * Take a FlatField of data and turn it into a field of PointObs.
     *
     *
     * @param range The data range of the request
     * @return field of PointObs
     * @throws Exception On badness
     */
    public abstract FieldImpl getPointObTrack(Range range) throws Exception;


    /**
     * Utility to find the variable with the given name
     *
     * @param variableName The name
     *
     * @return The variable.
     */
    protected Variable getDataVariable(String variableName) {
        if (variableName.equals(VAR_LATITUDE)) {
            //                return tod.getLatitudeVariable();
        } else if (variableName.equals(VAR_LONGITUDE)) {
            //                return tod.getLongitudeVariable();
        } else if (variableName.equals(VAR_ALTITUDE)) {
            //                return tod.getAltitudeVariable();
        }

        //Jump through some hoops for legacy bundles
        for (int i = 0; i < 2; i++) {
            if (i == 1) {
                variableName = variableName.toLowerCase();
            }
            for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
                Variable theVar = (Variable) variables.get(varIdx);
                if (variableName.equals(theVar.getName())) {
                    return theVar;
                }
            }

            for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
                Variable theVar = (Variable) variables.get(varIdx);
                if (variableName.equals(theVar.getDescription())) {
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
            varType = RealType.getRealType(LAT_TYPE, CommonUnit.degree);
        } else if (varToCheck.equals(RealType.Longitude)) {
            varType = RealType.getRealType(LON_TYPE, CommonUnit.degree);
        } else if (varToCheck.equals(RealType.Time)) {
            try {
                Real value = new Real(RealType.Time, sampleValue, unit);
                double sinceEpoch =
                    value.getValue(CommonUnit.secondsSinceTheEpoch);
                int years = (int) (sinceEpoch / (365 * 24 * 3600));
                int year  = 1970 + (years);
                Unit timeUnit = Util.parseUnit("seconds since " + year
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
     * Get list of Variables to use
     *
     * @return List of vars
     */
    protected List getVarsToUse() {
        List varsToUse = new ArrayList();
        for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
            Variable var = (Variable) variables.get(varIdx);
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
            Variable var = (Variable) vars.get(varIdx);
            if (var.isNumeric) {
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
        //We will replace this when we can do a getLatitudeVariable, etc. 
        //call
        float[]  value  = null;
        double[] dvalue = null;
        if (variableName.equals(VAR_LATITUDE)) {
            unit  = DataUtil.parseUnit("degrees");
            value = getLatitude(range);
        } else if (variableName.equals(VAR_LONGITUDE)) {
            unit  = DataUtil.parseUnit("degrees");
            value = getLongitude(range);
        } else if (variableName.equals(VAR_ALTITUDE)) {
            unit  = DataUtil.parseUnit("m");
            value = getAltitude(range);
        } else if (variableName.equals(VAR_TIME)) {
            unit   = getTimeUnit();
            dvalue = getTime(range);
        } else {
            Variable var = getDataVariable(variableName);
            unit  = var.unit;
            value = getData(range, var.getShortName());
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
     * @param varName Variable name
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

        Variable pressureVar = null;
        Variable tempVar     = null;
        Variable dewpointVar = null;
        Variable wspdVar     = null;
        Variable wdirVar     = null;



        for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
            Variable var       = (Variable) variables.get(varIdx);
            String   name      = var.getShortName();
            String   canonical = DataAlias.aliasToCanonical(name);
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
        GriddedSet llaSet         = getSpatialSet(getFullRange());
        Unit[]       llaUnits     = llaSet.getSetUnits();
        Unit         tempUnit     = tempVar.unit;
        Unit         dewpointUnit = dewpointVar.unit;
        Unit[]       addUnits     = new Unit[] {
            pressureVar.unit, (Unit.canConvert(tempUnit, CommonUnits.CELSIUS)
                               ? tempUnit
                               : CommonUnits.CELSIUS), (Unit.canConvert(
                                   dewpointUnit, CommonUnits.CELSIUS)
                    ? dewpointUnit
                    : CommonUnits.CELSIUS), wspdVar.unit, wdirVar.unit,
            llaUnits[0], llaUnits[1], llaUnits[2]
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
            getData(range, pressureVar), getData(range, tempVar),
            getData(range, dewpointVar), getData(range, wspdVar),
            getData(range, wdirVar), llaSamples[0], llaSamples[1],
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
                samples[wdirIndex], Util.parseUnit("gpm"), samples[altIndex]);

        raob.setMandatoryPressureProfile(mpp);

        return raob;
    }




    /**
     * Class Variable Holds info about track variables
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.4 $
     */
    protected static class Variable {

        /** name */
        String name;

        /** desc */
        String description;

        /** unit */
        Unit unit;

        /** is this numeric */
        boolean isNumeric = true;

        /** The real type to use */
        RealType realType;

        String category;


        /**
         * ctor
         *
         * @param name name
         * @param desc desc
         * @param unit unit
         */
        public Variable(String name, String desc, Unit unit) {
            this.name        = name;
            this.description = desc;
            if ((this.description == null)
                    || (this.description.trim().length() == 0)) {
                this.description = name;
            }
            this.unit = unit;
            realType  = DataUtil.makeRealType(getShortName(), unit);
            if (realType == null) {
                System.out.println("can't create realtype for "
                                   + getShortName() + " with unit " + unit);
            }
        }


        /**
         * ctor
         *
         * @param name name
         * @param unit unit
         */
        public Variable(String name, Unit unit) {
            this(name, name, unit);
        }

        /**
         * ctor
         *
         * @param name name
         * @param units unit string
         */
        public Variable(String name, String units) {
            this(name, DataUtil.parseUnit(units));
        }

        /**
         * ctor
         *
         * @param name name
         * @param desc description
         * @param units unit string
         */
        public Variable(String name, String desc, String units) {
            this(name, desc, DataUtil.parseUnit(units));
        }

        /**
         * get the name
         *
         * @return name
         */
        public String getName() {
            return name;
        }


        /**
         * get the name
         *
         * @return name
         */
        public String getShortName() {
            return name;
        }

        /**
         * get desc
         *
         * @return desc
         */
        public String getDescription() {
            return description;
        }

/**
Set the Category property.

@param value The new value for Category
**/
public void setCategory (String value) {
        category = value;
}

/**
Get the Category property.

@return The Category
**/
public String getCategory () {
        return category;
}



        /**
         * to string
         *
         * @return to string
         */
        public String toString() {
            return name;
        }

    }
}

