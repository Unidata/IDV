/*
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ucar.ma2.*;
import ucar.nc2.NCdumpW;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureCollectionIterator;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.ProfileFeatureCollection;
import ucar.nc2.ft.point.StationFeature;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.VarInfo;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObTuple;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.Trace;
import ucar.visad.Util;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.Direction;
import visad.*;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Sep 17, 2010
 * Time: 1:37:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class CDMProfileFeatureTypeInfo extends TrackInfo {


    /** The data set */
    private FeatureDatasetPoint fdp;

    /** The data type */
    ProfileFeatureCollection pfc;

    /** The data type */
    ProfileFeature  pf;

    /** _more_ */
    List<PointFeature> obsList;

    /** _more_ */
    double[] times;

    /** _more_          */
    int positive = 1;

    /** _more_          */
    private int maxCount = Integer.MAX_VALUE;

    /** _more_          */
    private int nptsTotal = 0;

    private Map<String, ProfileFeatureBean> beanCache = new ConcurrentHashMap<>();

    Unit [] units = new Unit[3];
    /**
     * ctor
     *
     *
     * @param adapter The adapter
     * @param fdp
     * @param pfc
     *
     * @throws Exception On badness
     */
    public CDMProfileFeatureTypeInfo(TrackAdapter adapter,
                                     FeatureDatasetPoint fdp,
                                     ProfileFeatureCollection pfc)
            throws Exception {
        super(adapter, pfc.getName());
        this.fdp = fdp;
        this.pfc = pfc;
        init();
        //            ucar.unidata.util.Misc.run(new Runnable(){public void run(){testit();}});
    }


    public CDMProfileFeatureTypeInfo(TrackAdapter adapter,
                                     FeatureDatasetPoint fdp,
                                     ProfileFeature pf)
            throws Exception {
        super(adapter, pf.getName());
        this.fdp = fdp;
        this.pfc = null;
        this.pf = pf;
        initPF();
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


        PointFeatureCollectionIterator iter =
            pfc.getPointFeatureCollectionIterator();
        obsList = new ArrayList<PointFeature>();
        ProfileFeatureBean profBean = null;
        int                iii      = 0;
        int                count    = 0;
        while (iter.hasNext()) {
            PointFeatureCollection pob = iter.next();
            profBean = new ProfileFeatureBean((ProfileFeature) pob);

            // obsList.add(profBean);
            nptsTotal = nptsTotal + pob.size();

            PointFeatureIterator iter1 = pob.getPointFeatureIterator();
            try {
                while (iter1.hasNext() && (count < maxCount)) {
                    PointFeature pf = iter1.next();
                    obsList.add(pf);
                    count++;
                    //System.out.println(" here " + count + " and " + i );
                }
            } finally {
                iter1.close();
            }
            obsList.add(null);
            nptsTotal = nptsTotal + 1;
        }

        // TrajectoryFeatureBean         pf      = obsList.get(0);
        StructureData                 pfsd    = obsList.get(0).getFeatureData();
        List<StructureMembers.Member> members = pfsd.getMembers();
        for (int i = 0; i < members.size(); i++) {
            StructureMembers.Member mb   = members.get(i);
            String                  ustr = mb.getUnitsString();
            Unit                    unit = null;
            if ((ustr != null) && !ustr.equalsIgnoreCase("none")) {
                try {
                    unit = Util.parseUnit(ustr);
                } catch (visad.VisADException e) {
                    unit = null;
                }
            }

            if ((unit != null)
                    && unit.isConvertible(CommonUnit.secondsSinceTheEpoch)) {
                addVariable(new VarInfo(mb.getName(), mb.getDescription(),
                                        "Basic", unit));
                varTime = mb.getName();
            } else if (mb.getName().equalsIgnoreCase("wv_level")) {
                //   && unit.isConvertible(CommonUnits.KILOMETER)) {
                if (mb.getName().equalsIgnoreCase("ALTITUDE")
                        || mb.getName().equalsIgnoreCase("ALT")) {

                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), "Basic",
                                            unit));
                    varAltitude = mb.getName();
                    units[2] = unit;
                } else if (mb.getName().equalsIgnoreCase("DEPTH")) {
                    positive = -1;
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), "Basic",
                                            unit));
                    varAltitude = mb.getName();
                } else if (mb.getName().equalsIgnoreCase("wv_level")) {
                    positive = 1;
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), "Basic",
                                            CommonUnits.KILOMETER));
                    varAltitude = mb.getName();
                } else {
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), unit));
                }
            } else if ((unit != null)
                       && unit.isConvertible(CommonUnits.DEGREE)) {


                if (mb.getName().equalsIgnoreCase("LATITUDE")
                        || mb.getName().equalsIgnoreCase("LAT")) {
                    varLatitude = mb.getName();
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), "Basic",
                                            unit));
                    units[0] = unit;
                } else if (mb.getName().equalsIgnoreCase("LONGITUDE")
                           || mb.getName().equalsIgnoreCase("LON")) {
                    varLongitude = mb.getName();
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), "Basic",
                                            unit));
                    units[1] = unit;
                } else {
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), unit));
                }

            } else {
                addVariable(new VarInfo(mb.getName(), mb.getDescription(),
                                        unit));
            }

        }
        Range rg = getDataRange();
        times = getTime(rg);

        startTime = getStartTime();  //new DateTime(df.parse(stimeStr.toString()));
        endTime = getEndTime();  //new DateTime(df.parse(etimeStr.toString()));

    }

    /**
     * initPF
     *
     * @throws Exception On badness
     */
    private void initPF() throws Exception {
        obsList = new ArrayList<PointFeature>();
        int                count    = 0;
        ProfileFeatureBean profBean = new ProfileFeatureBean((ProfileFeature) pf);
        beanCache.put(pf.getName(), profBean);
        nptsTotal = nptsTotal + pf.size();

        Iterator<PointFeature> iter1 = pf.iterator();

        while (iter1.hasNext() && (count < maxCount)) {
            PointFeature pf = iter1.next();
            obsList.add(pf);
            count++;
            //System.out.println(" here " + count + " and " + i );
        }

        obsList.add(null);
        nptsTotal = nptsTotal + 1;


        // TrajectoryFeatureBean         pf      = obsList.get(0);
        StructureData                 pfsd    = obsList.get(0).getDataAll();   //.getFeatureData();
        List<StructureMembers.Member> members = pfsd.getMembers();
        for (int i = 0; i < members.size(); i++) {
            StructureMembers.Member mb   = members.get(i);
            String                  ustr = mb.getUnitsString();
            Unit                    unit = null;
            if ((ustr != null) && !ustr.equalsIgnoreCase("none")) {
                try {
                    unit = Util.parseUnit(ustr);
                } catch (visad.VisADException e) {
                    unit = null;
                }
            }

            if ((unit != null)
                    && unit.isConvertible(CommonUnit.secondsSinceTheEpoch)) {
                addVariable(new VarInfo(mb.getName(), mb.getDescription(),
                        "Basic", unit));
                varTime = mb.getName();
            } else if (mb.getName().equalsIgnoreCase("ALTITUDE")
                    || mb.getName().equalsIgnoreCase("ALT")) {
                //   && unit.isConvertible(CommonUnits.KILOMETER)) {
                if (mb.getName().equalsIgnoreCase("ALTITUDE")
                        || mb.getName().equalsIgnoreCase("ALT")) {

                    addVariable(new VarInfo(mb.getName(),
                            mb.getDescription(), "Basic",
                            unit));
                    varAltitude = mb.getName();
                    units[2] = unit;
                } else if (mb.getName().equalsIgnoreCase("DEPTH")) {
                    positive = -1;
                    addVariable(new VarInfo(mb.getName(),
                            mb.getDescription(), "Basic",
                            unit));
                    varAltitude = mb.getName();
                } else if (mb.getName().equalsIgnoreCase("wv_level")) {
                    positive = 1;
                    addVariable(new VarInfo(mb.getName(),
                            mb.getDescription(), "Basic",
                            CommonUnits.KILOMETER));
                    varAltitude = mb.getName();
                } else {
                    addVariable(new VarInfo(mb.getName(),
                            mb.getDescription(), unit));
                }
            } else if ((unit != null)
                    && unit.isConvertible(CommonUnits.DEGREE)) {


                if (mb.getName().equalsIgnoreCase("LATITUDE")
                        || mb.getName().equalsIgnoreCase("LAT")) {
                    varLatitude = mb.getName();
                    addVariable(new VarInfo(mb.getName(),
                            mb.getDescription(), "Basic",
                            unit));
                    units[0] = unit;
                } else if (mb.getName().equalsIgnoreCase("LONGITUDE")
                        || mb.getName().equalsIgnoreCase("LON")) {
                    varLongitude = mb.getName();
                    addVariable(new VarInfo(mb.getName(),
                            mb.getDescription(), "Basic",
                            unit));
                    units[1] = unit;
                } else {
                    addVariable(new VarInfo(mb.getName(),
                            mb.getDescription(), unit));
                }

            } else {
                addVariable(new VarInfo(mb.getName(), mb.getDescription(),
                        unit));
            }

        }
        Range rg = getDataRange();
        times = getTime(rg);

        startTime = getStartTime();  //new DateTime(df.parse(stimeStr.toString()));
        endTime = getEndTime();  //new DateTime(df.parse(etimeStr.toString()));

    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected Unit getTimeUnit() throws Exception {
        return DataUtil.parseUnit("days since 1950-01-01T00:00:00Z");
    }

    /**
     * Get TrajectoryObsDatatype
     *
     * @return the TrajectoryObsDatatype
     */
    public ProfileFeatureCollection getFt() {
        return pfc;
    }

    /**
     * Get the full range. Include the stride
     *
     * @return The range
     *
     * @throws Exception On badness
     */
    protected Range getDataRange() throws Exception {
        // TrajectoryFeatureBean tfb   = obsList.get(0);
        // List                  ls    = tfb.pfs;
        ProfileFeatureBean pfb = beanCache.get(pf.getName());
        nptsTotal = pfb.pfc.size();
        Range range = new Range(0, nptsTotal - 1);
        return range;
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
                getLongitude(range), getAltitude(range), getUnits());
    }

    public Unit[] getUnits() {
        return units;
    }
    /**
     * Get number of points in track
     *
     * @return number of points
     */
    public int getNumberPoints() {
        return pfc.size();
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
        if (times == null) {
            float[] t = getFloatData(range, varTime);
            times = new double[t.length];
            for (int i = 0; i < t.length; i++) {
                times[i] = (double) t[i];
            }

        }
        return times;

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
    public double[] getTimeVals(Range range) throws Exception {
        /*double[] timeVals = (double[]) cachedTimeVals.get(range);
        if (timeVals == null) {
            timeVals = getTime(range);
            cachedTimeVals.put(range, timeVals);
        }    */

        return getTime(range);  //timeVals;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getStartTime() {
        if (startTime == null) {
            try {
                startTime = new DateTime(times[0], getTimeUnit());
            } catch (Exception e) {}
        }
        return startTime;
    }

    /**
     * _more_
     *
     * @return _more_
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
     * _more_
     *
     * @param range _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected float[] getAltitude(Range range) throws Exception {
        float[] fdata = new float[range.length()];

        //  TrajectoryFeatureBean tfb = obsList.get(0);
        //   fdata = tfb.getAltitudes(range);
        int size = obsList.size();
           int      first  = range.first();
           int      stride = range.stride();
           int      last   = range.last();

           int      i      = first;
           int      j      = 0;
           while (i <= last) {
               PointFeature  pf   = obsList.get(i);

               fdata[j++] = (float)pf.getLocation().getAltitude() * positive;
               i          = i + stride;
           }
       // float[] t = getFloatData(range, varAltitude);
        return fdata;
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
    protected float[] getLatitude(Range range) throws Exception {
        float[] fdata = new float[range.length()];

        //TrajectoryFeatureBean tfb = obsList.get(0);
        //fdata = tfb.getLatitudes(range);

        int          size   = obsList.size();
        int          first  = range.first();
        int          stride = range.stride();
        int          last   = range.last();
        PointFeature pf_Old = null;
        int          i      = first;
        int          j      = 0;
        while (i <= last) {
            PointFeature pf = obsList.get(i);
            if (pf == null) {
                pf = pf_Old;
            }
            fdata[j++] = (float) pf.getLocation().getLatitude();

            pf_Old     = pf;
            i          = i + stride;
        }

        return fdata;
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
    protected float[] getLongitude(Range range) throws Exception {
        float[] fdata = new float[range.length()];

        //TrajectoryFeatureBean tfb = obsList.get(0);
        //fdata = tfb.getLongitudes(range);

        int          size   = obsList.size();
        int          first  = range.first();
        int          stride = range.stride();
        int          last   = range.last();
        PointFeature pf_Old = null;
        int          i      = first;
        int          j      = 0;
        while (i <= last) {
            PointFeature pf = obsList.get(i);

            if (pf == null) {
                pf = pf_Old;
            }

            fdata[j++] = (float) pf.getLocation().getLongitude();
            pf_Old     = pf;

            i          = i + stride;
        }

        return fdata;

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

        float[] fdata = new float[range.length()];
        //TrajectoryFeatureBean tfb   = obsList.get(0);
        //fdata = tfb.getFloatData(range, var);
        int          size   = obsList.size();
        int          first  = range.first();
        int          stride = range.stride();
        int          last   = range.last();
        PointFeature pf_Old = null;
        int          i      = first;
        int          j      = 0;
        while (i <= last) {
            PointFeature pf = obsList.get(i);
            if (pf == null) {
                pf = pf_Old;
            }

            StructureData pfsd = pf.getDataAll(); //.getFeatureData();
            Array a   = pfsd.getArray(var);
            fdata[j++] = a.getFloat(0);;
            pf_Old     = pf;
            i          = i + stride;
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
        double[] fdata = new double[range.length()];
        //TrajectoryFeatureBean tfb   = obsList.get(0);
        //fdata = tfb.getDoubleData(range, var);
        int first  = range.first();
        int stride = range.stride();
        int last   = range.last();

        int i      = first;
        int j      = 0;
        while (i <= last) {
            PointFeature pf = obsList.get(i);

            if (pf != null) {
                StructureData pfsd = pf.getDataAll();  //.getFeatureData();
                fdata[j++] = pfsd.getScalarDouble(var);
            } else {
                fdata[j++] = Float.NaN;
            }
            i = i + stride;
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
        while (i <= last) {
            PointFeature pf = obsList.get(i);


            if (pf != null) {
                StructureData pfsd = pf.getFeatureData();
                sdata[j++] = pfsd.getScalarString(var);
            } else {
                sdata[j++] = null;
            }
            i = i + stride;
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
        List<VarInfo> varsToUse  = getVarsToUse();

        int           numReals   = countReals(varsToUse);
        int           numStrings = varsToUse.size() - numReals;
        boolean       allReals   = numStrings == 0;
        int           numVars    = varsToUse.size();
        Unit[]        units      = new Unit[numVars];
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
                VarInfo var = varsToUse.get(varIdx);
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
                StructureData structure   = null;  //todt.getData(obIdx);
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

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Wed, Dec 22, '10
     * @author         Enter your name here...
     */
    public static class FeatureBean {

        StructureData sdata;
        String fields;

        /**
         *
         */
        public FeatureBean() {}

        /**
         *
         */
        FeatureBean(StructureData sdata) throws IOException {
            this.sdata = sdata;
            fields = NCdumpW.toString(sdata);
        }

        public String getFields() {
            return fields;
        }

        public String showFields() {
            StringWriter sw = new StringWriter(10000);
            NCdumpW.printStructureData(new PrintWriter(sw), sdata);
            return sw.toString();
        }
    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Wed, Dec 22, '10
     * @author         Enter your name here...
     */
    public static class StationBean extends FeatureBean implements ucar.unidata.geoloc.Station {

        /** _more_ */
        private StationFeature s;

        /** _more_ */
        private int npts = -1;

        /**
         * _more_
         */
        public StationBean() {}


        public StationBean(StructureData sdata) throws IOException {
            super(sdata);
        }
        /**
         * _more_
         *
         * @param s _more_
         */
        public StationBean(Station s) throws IOException {
            super(((StationFeature) s).getFeatureData());
            this.s = (StationFeature) s;
            this.npts = s.getNobs();
        }

        // for BeanTable

        /**
         * _more_
         *
         * @return _more_
         */
        static public String hiddenProperties() {
            return "latLon";
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getNobs() {
            return npts;
        }

        /**
         * _more_
         *
         * @param npts _more_
         */
        public void setNobs(int npts) {
            this.npts = npts;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getWmoId() {
            return s.getWmoId();
        }

        // all the station dependent methods need to be overridden

        /**
         * _more_
         *
         * @return _more_
         */
        public String getName() {
            return s.getName();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getDescription() {
            return s.getDescription();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getLatitude() {
            return s.getLatitude();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getLongitude() {
            return s.getLongitude();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getAltitude() {
            return s.getAltitude();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public LatLonPoint getLatLon() {
            return s.getLatLon();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isMissing() {
            return s.isMissing();
        }

        /**
         * _more_
         *
         * @param so _more_
         *
         * @return _more_
         */
        public int compareTo(Station so) {
            return getName().compareTo(so.getName());
        }
    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Wed, Dec 22, '10
     * @author         Enter your name here...
     */


    public static class ProfileFeatureBean extends StationBean {

        /** _more_          */
        int npts;

        /** _more_          */
        public ProfileFeature pfc;

        /** _more_          */
        public PointFeature pf;

        /**
         * _more_
         *
         * @param pfc _more_
         */
          public ProfileFeatureBean(ProfileFeature pfc) throws IOException {
            super(pfc.getFeatureData());
            this.pfc = pfc;

            for (PointFeature pf2 : pfc) {
                pf = pf2; // a random point
            }
            npts = pfc.size();
        }

        /**
         * _more_
         *
         * @param npts _more_
         */
        public void setNobs(int npts) {
            this.npts = npts;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getNobs() {
            return npts;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getName() {
            return pfc.getName();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getDescription() {
            return null;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getWmoId() {
            return null;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getLatitude() {
            return pf.getLocation().getLatitude();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getLongitude() {
            return pf.getLocation().getLongitude();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public double getAltitude() {
            return pf.getLocation().getAltitude();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public LatLonPoint getLatLon() {
            return pf.getLocation().getLatLon();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isMissing() {
            return Double.isNaN(getLatitude());
        }

        /**
         * _more_
         *
         * @param so _more_
         *
         * @return _more_
         */
        public int compareTo(Station so) {
            return getName().compareTo(so.getName());
        }
    }

}
