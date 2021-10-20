/*
 * $Id: CdmTrackInfo.java,v 1.8 2007/08/16 22:44:32 jeffmc Exp $
 *
 * Copyright  1997-2022 Unidata Program Center/University Corporation for
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


import ucar.ma2.Array;

import ucar.ma2.Index;
import ucar.ma2.Index0D;
import ucar.ma2.Range;


import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;


import ucar.nc2.ft.*;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.DsgCollectionImpl;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;


import ucar.unidata.data.DataUtil;
import ucar.unidata.data.VarInfo;

import ucar.unidata.data.point.*;


import ucar.unidata.util.JobManager;

import ucar.unidata.util.Trace;


import ucar.visad.Util;
import ucar.visad.quantities.*;

import visad.*;
import visad.georef.*;


import java.util.*;

/**
 * Class TrackInfo Provides access to a track or trajectory
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.8 $
 */
public class CdmTrackInfo extends TrackInfo {

    /** Fixed var name for lat */
    public static final String VAR_LATITUDE = "Latitude";

    /** Fixed var name for lon */
    public static final String VAR_LONGITUDE = "Longitude";

    /** Fixed var name for alt */
    public static final String VAR_ALTITUDE = "Altitude";

    /** Fixed var name for time */
    public static final String VAR_TIME = "Time";

    List<PointFeature> obsList = new ArrayList<>();

    /** The times. */
    double[] times;

    /** The data set */
    private FeatureDatasetPoint tod;

    /** The data type */
    TrajectoryFeature todt;

    CDMTrajectoryFeatureTypeInfo.TrajectoryFeatureBean trajBean;
    /**
     * ctor
     *
     *
     * @param adapter The adapter
     * @param tod tod
     * @param todt todt
     *
     * @throws Exception On badness
     */
    public CdmTrackInfo(TrackAdapter adapter, FeatureDatasetPoint tod,
                        TrajectoryFeature todt)
            throws Exception {
        super(adapter, tod.getTitle());
        this.tod  = tod;
        this.todt = todt;
        init();

        //            ucar.unidata.util.Misc.run(new Runnable(){public void run(){testit();}});
    }

    /** _more_          */
    private static String[] categoryAttributes = { "category", "group" };


    /**
     * init
     *
     * @throws Exception On badness
     */
    private void init() throws Exception {
        varTime      = VAR_TIME;
        varLatitude  = VAR_LATITUDE;
        varLongitude = VAR_LONGITUDE;
        varAltitude  = VAR_ALTITUDE;



        trajBean = new CDMTrajectoryFeatureTypeInfo.TrajectoryFeatureBean(todt);

        List<PointFeature> pfs = trajBean.pfs;
        TrajectoryFeature tf = trajBean.pfc;

        int  psize = pfs.size();
        for (int i = 0; i < psize; i++) {
            obsList.add(pfs.get(i));
        }
        CalendarDateRange cdr = tf.getCalendarDateRange();
        Range rg = getDataRange();


        List allVariables = tod.getDataVariables();

        //TODO: Check size


        addVariable(new VarInfo(VAR_TIME, VAR_TIME, "Basic", getTimeUnit()));

        addVariable(new VarInfo(VAR_LATITUDE, VAR_LATITUDE, "Basic",
                                CommonUnits.DEGREE));

        addVariable(new VarInfo(VAR_LONGITUDE, VAR_LONGITUDE, "Basic",
                                CommonUnits.DEGREE));

        addVariable(new VarInfo(VAR_ALTITUDE, VAR_ALTITUDE, "Basic",
                                DataUtil.parseUnit("m")));


        for (int varIdx = 0; varIdx < allVariables.size(); varIdx++) {
            String                  ustr = null;
            Unit                    unit = null;
            VariableSimpleIF var =
                (VariableSimpleIF) allVariables.get(varIdx);
            //Skip vector variables
            if (var.getRank() != 0) {
                continue;
            }

            ustr = var.getUnitsString();
            if ((ustr != null) && !ustr.equalsIgnoreCase("none")) {
                    try {
                        unit = Util.parseUnit(ustr);
                    } catch (visad.VisADException e) {
                        unit = null;
                    }

            }
            VarInfo variable = new VarInfo(var.getShortName(),
                                           var.getDescription(),
                                           var.getUnitsString());


            Attribute attr = null;
            for (int i = 0; (i < categoryAttributes.length) && (attr == null);
                    i++) {
                attr = var.findAttributeIgnoreCase(categoryAttributes[i]);
            }
            if (attr != null) {
                variable.setCategory(attr.getStringValue());
            }

            if ((unit != null)) {
                if (unit.isConvertible(CommonUnit.secondsSinceTheEpoch)
                        && !(unit instanceof DerivedUnit) && (unit instanceof OffsetUnit)) {
                    varTime = variable.getName();
                } else if(Double.isNaN(trajBean.getAltitude()) && unit.isConvertible(CommonUnit.meter)) {
                    if (var.getShortName().equalsIgnoreCase("alt") ||
                            var.getShortName().equalsIgnoreCase("altitude") ||
                            var.getShortName().toLowerCase().startsWith("alt")) {
                        varAltitude = variable.getName();
                        System.out.println(variable.getName());
                    }
                }
            }

            addVariable(variable);
        }

        times     = getTime(rg);
        if(cdr != null) {
            startTime = new DateTime(cdr.getStart().toDate());
            endTime = new DateTime(cdr.getEnd().toDate());
        } else {
            startTime = getStartTime();
            endTime = getEndTime();
        }
    }



    public DateTime getStartTime() {
        if (startTime == null) {
            try {
                startTime = new DateTime(times[0], getTimeUnit());
            } catch (Exception e) {}
        }
        return startTime;
    }

    /**
     * {@inheritDoc}
     */
    public DateTime getEndTime() {
        if (endTime == null) {
            try {
                endTime = new DateTime(times[times.length - 1],
                        getTimeUnit());
            } catch (Exception e) {}
        }
        return endTime;
    }

    /**
     * Get TrajectoryObsDatatype
     *
     * @return the TrajectoryObsDatatype
     */
    public TrajectoryFeature getTodt() {
        return todt;
    }


    /**
     * Get number of points in track
     *
     * @return number of points
     */
    public int getNumberPoints() {
        return getTodt().size();
    }


    /**
     * test
     */
    private void testit() {
        try {
            Trace.call1("TrackInfo.rowRead-new");
            test2();
            Trace.call2("TrackInfo.rowRead-new");
        } catch (Exception exc) {}
    }


    /**
     * test
     *
     * @throws Exception On badness
     */
    private void test1() throws Exception {
        Range range   = getFullRange();
        int   numVars = variables.size();
        int   varCnt  = 0;
        for (int varIdx = 0; varIdx < numVars; varIdx++) {
            VarInfo var = (VarInfo) variables.get(varIdx);
            getFloatData(range, var.getShortName());
            varCnt++;
        }
        Trace.msg("Column read #vars:" + varCnt);
    }


    /**
     * test
     *
     * @throws Exception On badness
     */
    private void test2() throws Exception {
        int   numObs      = getNumberPoints();
        Index scalarIndex = new Index0D(new int[0]);

            //TrajectoryFeature pf = itertor.next();
            StructureData structure  = todt.getFeatureData();
            List          members    = structure.getMembers();
            int           numMembers = members.size();

            Trace.msg("test2-numMembers:" + numMembers);

            for (int varIdx = 0; varIdx < numMembers; varIdx++) {
                StructureMembers.Member member =
                    (StructureMembers.Member) members.get(varIdx);
                Array a = structure.getArray(member);
            }

    }


    /**
     * Get the full range. Include the stride
     *
     * @return The range
     *
     * @throws Exception On badness
     */
    protected Range getDataRange() throws Exception {
        return new Range(0, (getNumberPoints()-1) , 1);
    }

    /**
     * A utility to get the time unit
     *
     * @return The time unit
     *
     * @throws Exception On badness
     */
    protected Unit getTimeUnit() throws Exception {
        return DataUtil.parseUnit(todt.getTimeUnit().toString());
    }

    /**
     * Get the time for each ob. May be subset by range.
     *
     * @param range Subset on range. May be null
     *
     * @return time values
     *
     * @throws Exception On badness
     */
    protected double[] getTime(Range range) throws Exception {
        List<PointFeature> pfs = trajBean.pfs;
        //VariableDS tvar =  ((PointDatasetStandardFactory.PointDatasetStandard) this.tod).netcdfDataset.findCoordinateAxis(AxisType.Time).orgVar;
        double [] time = null;
        time = trajBean.getTimes(range);

        if(time == null || (time.length > 1 &&time[0]==time[time.length-1])) {
            if (varTime != null)
                time = trajBean.getDoubleData(range, varTime);
            else {
                time = trajBean.getDoubleData(range, "Time");
                if (time == null) {
                    time = trajBean.getDoubleData(range, "time");
                    if (time == null)
                        time = trajBean.getDoubleData(range, "TIME");
                    if (time == null)
                        return null;
                }
            }
        }
        return time;
    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    public static float[] qcLatLon(float[] v) {
        if ((v == null) || (v.length == 0)) {
            return v;
        }
        float lastValue = v[0];
        for (int i = 0; i < v.length; i++) {
            if (v[i] != v[i]) {
                continue;
            }
            if (v[i] == 0) {}
            else {
                lastValue = v[i];
                break;
            }
        }

        for (int i = 0; i < v.length; i++) {
            if (v[i] != v[i]) {
                continue;
            }
            if (Math.abs(v[i] - lastValue) > 10) {
                v[i] = lastValue;
            }
            lastValue = v[i];
        }
        return v;
    }


    /**
     * Get the data values for the range.
     *
     * @param range subset. May be null
     * @param var The variable
     *
     * @return values
     *
     * @throws Exception On badness
     */
    protected float[] getFloatData(Range range, String var) throws Exception {
        if (var.equals(VAR_TIME)) {
            return trajBean.getFloatData(range, varTime); //todt..getTime(range));
        }
        if (var.equals(VAR_LATITUDE)) {
            return trajBean.getLatitudes(range);
        }
        if (var.equals(VAR_LONGITUDE)) {
            return trajBean.getLongitudes(range);
        }
        if (var.equals(VAR_ALTITUDE)) {
            return trajBean.getAltitudes(range);
        }
        return trajBean.getFloatData(range, var);
    }


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
        if (var.equals(VAR_TIME)) {
            return trajBean.getDoubleData(range, varTime);
        }
        if (var.equals(VAR_LATITUDE)) {
            return trajBean.getDoubleData(range, VAR_LATITUDE);
        }
        if (var.equals(VAR_LONGITUDE)) {
            return trajBean.getDoubleData(range, VAR_LONGITUDE);
        }
        if (var.equals(VAR_ALTITUDE)) {
            return trajBean.getDoubleData(range, VAR_ALTITUDE);
        }
        return trajBean.getDoubleData(range, var);
    }



    /**
     * Get the string values for the var
     *
     * @param range subset. May be null.
     * @param var The var
     *
     * @return string values
     *
     * @throws Exception On badness
     */
    protected String[] getStringData(Range range, String var)
            throws Exception {
        return DataUtil.toStringArray(null); //todt.getData(range, var));
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
        if(Double.isNaN(trajBean.getAltitude()) && varAltitude != null)
            return getFloatData(range, varAltitude);
        else
            return getFloatData(range, VAR_ALTITUDE);
    }

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
            VarInfo var = (VarInfo) varsToUse.get(varIdx);
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

            double dirVal = Util.calculateBearing(lastEL, location).getAngle();


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
        } else {
            //TODO: use the range here...
            Index scalarIndex = new Index0D(new int[0]);
            for (int obIdx = 0; obIdx < numObs; obIdx++) {
                if ( !JobManager.getManager().canContinue(loadId)) {
                    return null;
                }
                StructureData structure   = todt.getFeatureData();
                List          members     = structure.getMembers();
                Data[]        tupleArray  = (Data[]) tuples.get(0); //obIdx);
                double[]      realArray   = (double[]) reals.get(0); //
                String[]      stringArray = (String[]) strings.get(0);
                int           numMembers  = members.size();
                int           varCnt      = 0;
                int           realCnt     = 0;
                int           stringCnt   = 0;
                //When row oriented we read the whole row.
                //However, some we ignore because they are non-scalar
                //Some we ignore because we are filtering them
                for (int varIdx = 0; varIdx < numVars; varIdx++) {
                    //TODO: don't do repeated look ups here
                    VarInfo var = (VarInfo) varsToUse.get(varIdx);
                    StructureMembers.Member member =
                        (StructureMembers.Member) structure.findMember(
                            var.getShortName());
                    if ( !var.getIsNumeric()) {
                        String value = new String(
                                           DataUtil.toCharArray(
                                               structure.getArray(member)));
                        stringArray[stringCnt++] = value;
                        tupleArray[varCnt]       = new Text(value);
                    } else {
                        //TODO: do the cloneButValue call here
                        float value = structure.convertScalarFloat(member);
                        realArray[realCnt++] = value;
                        tupleArray[varCnt]   = (var.getUnit() == null)
                                ? new Real(var.getRealType(), value)
                                : new Real(var.getRealType(), value,
                                           var.getUnit());
                    }
                    varCnt++;
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





}

