/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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


import ucar.ma2.*;
import ucar.ma2.StructureMembers.Member;
import ucar.nc2.Attribute;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.TrajectoryFeatureCollection;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.VarInfo;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObTuple;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.util.DateUtil;
import ucar.visad.Util;
import ucar.visad.functiontypes.InSituAirTemperatureProfile;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.quantities.*;
import visad.*;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.NamedLocationTuple;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho       
 * Date: Sep 23, 2009
 * Time: 12:46:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class CosmicTrajectoryFeatureTypeInfo extends TrackInfo {

    /** Fixed var name for lat */
    public static final String VAR_LATITUDE = "Latitude";

    /** Fixed var name for lon */
    public static final String VAR_LONGITUDE = "Longitude";

    /** Fixed var name for alt */
    public static final String VAR_ALTITUDE = "Altitude";

    /** Fixed var name for time */
    public static final String VAR_TIME = "Time";

    /** _more_ */
    private static final String temperature = "Temp";

    /** _more_ */
    private static final String vaporPressure = "Vp";

    /** _more_ */
    private static final String analyzedReflectivity = "Ref";

    /** _more_ */
    private static final String observedReflectivity = "Ref_obs";

    /** _more_ */
    private static final String height = "MSL_alt";

    /** _more_ */
    private static final String latitude = "Lat";

    /** _more_ */
    private static final String longitude = "Lon";

    /** _more_ */
    private static final String pressure = "Pres";

    /** _more_ */
    private static final String time = "time";

    /** The data set */
    private TrajectoryFeatureCollection tfc;

    /** The data type */
    FeatureDatasetPoint fd;

    /** _more_ */
    List<PointFeature> obsList;

    /**
     * ctor
     *
     *
     * @param adapter The adapter
     * @param fd _more_
     * @param tfc
     *
     * @throws Exception On badness
     */
    public CosmicTrajectoryFeatureTypeInfo(
            TrackAdapter adapter, FeatureDatasetPoint fd,
            TrajectoryFeatureCollection tfc) throws Exception {
        super(adapter, tfc.getName());
        this.tfc = tfc;
        this.fd  = fd;
        init();
        //            ucar.unidata.util.Misc.run(new Runnable(){public void run(){testit();}});
    }

    /** _more_ */
    private static String[] categoryAttributes = { "category", "group" };


    /**
     * init
     *
     * @throws Exception On badness
     */
    private void init() throws Exception {
        varTime      = time;
        varLatitude  = latitude;
        varLongitude = longitude;
        varAltitude  = height;
        Attribute stimeStr = fd.findGlobalAttributeIgnoreCase("start_time");
        Attribute etimeStr = fd.findGlobalAttributeIgnoreCase("stop_time");
        if(stimeStr == null){
            stimeStr = fd.findGlobalAttributeIgnoreCase("startTime");
            etimeStr = fd.findGlobalAttributeIgnoreCase("stopTime");
        }
        if(stimeStr == null){
            stimeStr = fd.findGlobalAttributeIgnoreCase("bottime");
            etimeStr = fd.findGlobalAttributeIgnoreCase("toptime");
            if(stimeStr != null) {
                if(stimeStr.getNumericValue().doubleValue() > etimeStr.getNumericValue().doubleValue()){
                    Attribute  t = etimeStr;
                    etimeStr = stimeStr;
                    stimeStr = t;
                }
                varLatitude  = "GEO_lat";
                varLongitude = "GEO_lon";

            }
        }

        startTime = new DateTime(stimeStr.getNumericValue().doubleValue(),
                                 getTimeUnit());
        endTime = new DateTime(etimeStr.getNumericValue().doubleValue(),
                               getTimeUnit());

        //  List allVariables = tfc.getDataVariables();
        tfc.resetIteration();

        obsList = new ArrayList<PointFeature>();
        while (tfc.hasNext()) {
            PointFeatureCollection pob = tfc.next();
            pob.resetIteration();
            try {
                while (pob.hasNext()) {
                    obsList.add(pob.next());
                }
            } finally {
                pob.finish();
            }
        }

        PointFeature  pf   = obsList.get(0);
        StructureData pfsd = pf.getData();
        List<Member> members = pfsd.getMembers();
        for(int i=0; i< members.size(); i++){
            Member mb = members.get(i);
            String ustr =  mb.getUnitsString();
            Unit unit = null;
            if(!ustr.equalsIgnoreCase("none")){
                try{
                    unit = Util.parseUnit(ustr);
                }  catch (visad.VisADException e) {
                    unit = null;
                }
            }

            if(unit != null && (unit.isConvertible(CommonUnits.DEGREE) ||unit.isConvertible(CommonUnits.KILOMETER) ||
                 unit.isConvertible(CommonUnit.secondsSinceTheEpoch)))
                addVariable(new VarInfo(mb.getName(), mb.getDescription(), "Basic", unit));
            else
               addVariable(new VarInfo(mb.getName(), mb.getDescription(), unit));

         }
       /*
        addVariable(new VarInfo(time, time, "Basic", getTimeUnit()));

        addVariable(new VarInfo(latitude, latitude, "Basic",
                                CommonUnits.DEGREE));
        addVariable(new VarInfo(longitude, longitude, "Basic",
                                CommonUnits.DEGREE));
        addVariable(new VarInfo(height, height, "Basic",
                                DataUtil.parseUnit("m")));
        addVariable(new VarInfo(temperature, temperature,
                                DataUtil.parseUnit("C")));
        addVariable(new VarInfo(vaporPressure, vaporPressure,
                                DataUtil.parseUnit("mb")));
        addVariable(new VarInfo(analyzedReflectivity, analyzedReflectivity,
                                DataUtil.parseUnit("dBZ")));
        addVariable(new VarInfo(observedReflectivity, observedReflectivity,
                                DataUtil.parseUnit("dBZ")));
        addVariable(new VarInfo(pressure, pressure, "Basic",
                                DataUtil.parseUnit("mb")));
       */
    }


    /**
     * Get number of points in track
     *
     * @return number of points
     */
    public int getNumberPoints() {
        return obsList.size();
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public NamedLocationTuple getLatLonPoint() throws Exception {
        float[] lats = getLatitude(getDataRange());
        float[] lons = getLongitude(getDataRange());
        float[] alts = getAltitude(getDataRange());
        String stName = "Lat:" + Misc.format(lats[0]) + " Lon:"
                        + Misc.format(lons[0]);
        NamedLocationTuple s = new NamedLocationTuple(stName, lats[0],
                                   lons[0], alts[0]);
        return s;
    }




    /**
     * Get the full range. Include the stride
     *
     * @return The range
     *
     * @throws Exception On badness
     */
    protected Range getDataRange() throws Exception {
        Range range = new Range(0, obsList.size() - 1);
        return range;
    }

    /**
     * A utility to get the time unit
     *
     * @return The time unit
     *
     * @throws Exception On badness
     */
    protected Unit getTimeUnit() throws Exception {
        return DataUtil.parseUnit("seconds since 1980-01-06 00:00:00");
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

        return getDoubleData(range, time);

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
    public float[] getFloatData(Range range, String var) throws Exception {
        float[] fdata  = new float[range.length()];

        int     first  = range.first();
        int     stride = range.stride();
        int     last   = range.last();
        int     scale  = 1;
        if (var.equals(height)) {
            scale = 1000;
        }
        int i = first;
        int j = 0;

        if(obsList.size() <= last){
            i = obsList.size() - 1;
        }
        while (i <= obsList.size() && j < range.length()) {
            PointFeature  pf   = obsList.get(i);
            StructureData pfsd = pf.getData();

            fdata[j++] = pfsd.getScalarFloat(var) * scale;

            i          = i + stride;
        }
        if(obsList.size() < last && j < range.length()) {
            for(int ii = i; ii < last; ii = ii+stride ){
                  fdata[ii] = fdata[i];
            }
        }
        return fdata;

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
    public double[] getDoubleData(Range range, String var) throws Exception {
        double[] fdata  = new double[range.length()];
        int      first  = range.first();
        int      stride = range.stride();
        int      last   = range.last();

        int      i      = first;
        int      j      = 0;

        if(obsList.size() <= last){
            i = obsList.size() - 1;
        }
        while (i <= obsList.size() && j < range.length()) {
            PointFeature  pf   = obsList.get(i);
            StructureData pfsd = pf.getData();

            fdata[j++] = pfsd.getScalarDouble(var);

            i          = i + stride;
        }
        if(obsList.size() < last && j < range.length()) {
            for(int ii = i; ii < last; ii = ii+stride ){
                  fdata[ii] = fdata[i];
            }
        }
        return fdata;
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
    public String[] getStringData(Range range, String var) throws Exception {
        String[] sdata  = new String[range.length()];
        int      first  = range.first();
        int      stride = range.stride();
        int      last   = range.last();

        int      i      = first;
        int      j      = 0;
        while (i < last) {
            PointFeature  pf   = obsList.get(i);
            StructureData pfsd = pf.getData();

            sdata[j++] = pfsd.getScalarString(var);
            i          = i + stride;
        }
        return sdata;
    }



    /**
     * Take a FlatField of data and turn it into a field of PointObs.
     *
     *
     * @param range The data range of the request
     * @return field of PointObs
     * @throws Exception On badness
     */
    public synchronized FieldImpl getPointObTrack(
            Range range) throws Exception {


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
        List    varsToUse  = getVarsToUse();
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
        //         doColumnOriented = false;
        if (doColumnOriented) {
            int realCnt   = 0;
            int stringCnt = 0;
            for (int varIdx = 0; varIdx < numVars; varIdx++) {
                if ( !JobManager.getManager().canContinue(loadId)) {
                    return null;
                }
                VarInfo var = (VarInfo) varsToUse.get(varIdx);
                if (var.getIsNumeric()) {
                    String  sn = var.getShortName();
                    float[] fvalues;

                    if (sn.equals("time")) {
                        double[] dvalues = getDoubleData(range,
                                               var.getShortName());
                        fvalues = new float[dvalues.length];
                        int i = 0;
                        for (double d : dvalues) {
                            fvalues[i++] = (float) d;
                        }

                    } else {
                        fvalues = getFloatData(range, var.getShortName());
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
                PointFeature  pf          = obsList.get(obIdx);
                StructureData structure   = pf.getData();

                List          members     = structure.getMembers();
                Data[]        tupleArray  = (Data[]) tuples.get(obIdx);
                double[]      realArray   = (double[]) reals.get(obIdx);
                String[]      stringArray = (String[]) strings.get(obIdx);
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
                        DataType dt = member.getDataType();
                        String dn =
                            dt.getPrimitiveClassType().getSimpleName();
                        if (dt.equals(DataType.DOUBLE)) {
                            double value = structure.getScalarDouble(member);
                            realArray[realCnt++] = value;
                            tupleArray[varCnt]   = (var.getUnit() == null)
                                    ? new Real(var.getRealType(), value)
                                    : new Real(var.getRealType(), value,
                                    var.getUnit());
                        } else if (dt.equals(DataType.FLOAT)) {
                            float value = structure.getScalarFloat(member);
                            realArray[realCnt++] = value;
                            tupleArray[varCnt]   = (var.getUnit() == null)
                                    ? new Real(var.getRealType(), value)
                                    : new Real(var.getRealType(), value,
                                    var.getUnit());
                        }
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



    public Data getAerologicalDiagramData() throws Exception {

        VarInfo pressureVar = null;
        VarInfo tempVar     = null;
        VarInfo vaprVar     = null;

        for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
            VarInfo var  = (VarInfo) variables.get(varIdx);
            String  name = var.getShortName();

            if (name.equals("Pres")) {
                pressureVar = var;
            } else if (name.equals("Temp")) {
                tempVar = var;
            } else if (name.equals("Vp")) {
                vaprVar = var;
            }
        }


        TupleType addType = new TupleType(new MathType[] {
                                AirPressure.getRealType(),
                                AirTemperature.getRealType(),
                                DewPoint.getRealType(),
                                RealTupleType.LatitudeLongitudeAltitude });

        Range        range        = getFullRange();

        FunctionType addFType     = new FunctionType(RealType.Time, addType);
        GriddedSet   llaSet       = getSpatialSet(getFullRange());
        Unit[]       llaUnits     = llaSet.getSetUnits();
        Unit         tempUnit     = tempVar.getUnit();
        Unit         dewpointUnit = tempVar.getUnit();  //dewpointVar.getUnit();
        Unit[]       addUnits     = new Unit[] {
            pressureVar.getUnit(),
            (Unit.canConvert(tempUnit, CommonUnits.CELSIUS)
             ? tempUnit
             : CommonUnits.CELSIUS), (Unit.canConvert(dewpointUnit,
                 CommonUnits.CELSIUS)
                                      ? dewpointUnit
                                      : CommonUnits.CELSIUS), llaUnits[0],
                                          llaUnits[1],
            llaUnits[2]
        };
     //   float[][] llaSamples = llaSet.getSamples();


        double[]  timeVals   = getTimeVals(range);
        Gridded1DDoubleSet timeSet = new Gridded1DDoubleSet(RealType.Time,
                                         new double[][] {
            timeVals
        }, timeVals.length, (CoordinateSystem) null,
           new Unit[] { getTimeUnit() }, (ErrorEstimate[]) null);



        FlatField addField = new FlatField(addFType, timeSet,
                                           (CoordinateSystem[]) null,
                                           (Set[]) null, addUnits);

  //      addField.setSamples(new float[][] {
  //          getFloatData(range, pressureVar), getFloatData(range, tempVar),
  //          getDewPointFloatData(range), llaSamples[0], llaSamples[1],
  //          llaSamples[2]
  //      });

        return addField;



    }

     

    /**
     * get the data
     *                                                  
     * @return the data
     *
     * @throws Exception On badness
     */
    public Data[] getAerologicalDiagramDataArray() throws Exception {

        VarInfo pressureVar = null;
        VarInfo tempVar     = null;
        VarInfo vaprVar     = null;

        for (int varIdx = 0; varIdx < variables.size(); varIdx++) {
            VarInfo var  = (VarInfo) variables.get(varIdx);
            String  name = var.getShortName();

            if (name.equals("Pres")) {
                pressureVar = var;
            } else if (name.equals("Temp")) {
                tempVar = var;
            } else if (name.equals("Vp")) {
                vaprVar = var;
            }
        }


        TupleType tempType = new TupleType(new MathType[] {
                                 AirPressure.getRealType(),
                                 AirTemperature.getRealType(),
                                 RealTupleType.LatitudeLongitudeAltitude });

        TupleType dewType = new TupleType(new MathType[] {
                                AirPressure.getRealType(),
                                DewPoint.getRealType(),
                                RealTupleType.LatitudeLongitudeAltitude });

        Range        range        = getFullRange();

        FunctionType tempFType    = new FunctionType(RealType.Time, tempType);
        FunctionType dewFType     = new FunctionType(RealType.Time, dewType)
        ;
        GriddedSet   llaSet       = getSpatialSet(getFullRange());
        Unit[]       llaUnits     = llaSet.getSetUnits();
        Unit         tempUnit     = tempVar.getUnit();
        Unit         dewpointUnit = tempVar.getUnit();  //dewpointVar.getUnit();
        Unit[] tempUnits = new Unit[] { pressureVar.getUnit(),
                                        (Unit.canConvert(tempUnit,
                                            CommonUnits.CELSIUS)
                                         ? tempUnit
                                         : CommonUnits.CELSIUS), llaUnits[0],
                                             llaUnits[1],
                                        llaUnits[2] };
        Unit[] dewUnits = new Unit[] { pressureVar.getUnit(),
                                       (Unit.canConvert(dewpointUnit,
                                           CommonUnits.CELSIUS)
                                        ? dewpointUnit
                                        : CommonUnits.CELSIUS), llaUnits[0],
                                            llaUnits[1],
                                       llaUnits[2] };

        float[][] llaSamples = llaSet.getSamples();

        double[]  timeVals   = getTimeVals(range);
        Gridded1DDoubleSet timeSet = new Gridded1DDoubleSet(RealType.Time,
                                         new double[][] {
            timeVals
        }, timeVals.length, (CoordinateSystem) null,
           new Unit[] { getTimeUnit() }, (ErrorEstimate[]) null);



        FlatField tempField = new FlatField(tempFType, timeSet,
                                            (CoordinateSystem[]) null,
                                            (Set[]) null, tempUnits);

        tempField.setSamples(new float[][] {
            getFloatData(range, pressureVar), getFloatData(range, tempVar),
            llaSamples[0], llaSamples[1], llaSamples[2]
        });

        float[] press   = getFloatData(range, pressureVar);
        float[] temps   = getFloatData(range, tempVar);
        int[]   indexes = Util.strictlySortedIndexes(press, false);
        press = Util.take(press, indexes);
        temps = Util.take(temps, indexes);

        Set presDomain = new Gridded1DSet(AirPressure.getRealTupleType(),
                                          new float[][] {
            press
        }, press.length, (CoordinateSystem) null,
           new Unit[] { pressureVar.getUnit() }, (ErrorEstimate[]) null);
        Field tempPro = new FlatField(InSituAirTemperatureProfile.instance(),
                                      presDomain);
        
        FlatField dewField = new FlatField(dewFType, timeSet,
                                           (CoordinateSystem[]) null,
                                           (Set[]) null, dewUnits);
        tempPro.setSamples(new float[][] {
            temps
        });
        if(vaprVar == null)
          return new Field[] { tempPro, null };

        dewField.setSamples(new float[][] {
            getFloatData(range, pressureVar), getDewPointFloatData(range),
            llaSamples[0], llaSamples[1], llaSamples[2]
        });

        //   return new Field[]{tempField, dewField};

        float[] dews    = getDewPointFloatData(range);

        dews  = Util.take(dews, indexes);


        Field dewPro = new FlatField(DewPointProfile.instance(), presDomain);



        dewPro.setSamples(new float[][] {
            dews
        });

        return new Field[] { tempPro, dewPro };
    }

    /**
     * _more_
     *
     * @param range _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected float[] getDewPointFloatData(Range range) throws Exception {
        float[] temp   = getFloatData(range, "Temp");
        float[] vapor  = getFloatData(range, "Vp");
        float[] dpoint = new float[temp.length];

        for (int i = 0; i < temp.length; i++) {
            dpoint[i] = getDewpoint(temp[i], vapor[i]);
        }
        return dpoint;

    }

    /**
     * _more_
     *
     * @param temp _more_
     * @param vp _more_
     *
     * @return _more_
     */
    public float getDewpoint(float temp, float vp) {
        float pa = 7.5f;
        float pb = 237.3f;
        float pc = 6.1078f;

        float p  = (float) Math.log10(vp / pc);

        return pb * p / (pa - p);

    }




}

