/*
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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

import ucar.nc2.NCdumpW;

import ucar.nc2.ft.*;
import ucar.nc2.ft.TrajectoryFeatureCollection;

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


import visad.CommonUnit;
import visad.Data;
import visad.DateTime;
import visad.DerivedUnit;
import visad.FieldImpl;
import visad.FunctionType;
import visad.Integer1DSet;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SetType;
import visad.Text;
import visad.Tuple;
import visad.TupleType;
import visad.Unit;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.io.IOException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * The Class CDMTrajectoryFeatureTypeInfo.
 */
public abstract class CDMTrajectoryFeatureTypeInfo extends TrackInfo {

    /** Basic tree node in the Data Choosers */
    private static final String DATA_CHOOSER_TREE_NODE_BASIC = "Basic";

    /** The data set. */
    private FeatureDatasetPoint fdp;

    /** The obs list. */
    List<PointFeature> obsList1 = null;

    /** The times. */
    double[] times;

    /** The positive. */
    int positive = 1;

    /** The feature collection. */
    private DsgFeatureCollection fc;

    /** The category attributes. */
    private static String[] categoryAttributes = { "category", "group" };

    /** The units cache. */
    private Map<String, Unit> unitsCache = new ConcurrentHashMap<>();

    CDMTrajectoryFeatureTypeInfo.TrajectoryFeatureBean trajBean = null;

    private Map<String, TrajectoryFeatureBean> beanCache = new ConcurrentHashMap<>();
    /**
     * Instantiates a new CDM trajectory feature type info.
     *
     * @param adapter the adapter
     * @param dataset the dataset
     * @param fc the fc
     * @throws Exception the exception
     */
    public CDMTrajectoryFeatureTypeInfo(TrajectoryFeatureTypeAdapter adapter,
                                        FeatureDatasetPoint dataset,
                                        DsgFeatureCollection fc)
            throws Exception {
        super(adapter, fc.getName());
        this.fdp = fdp;
        this.fc  = fc;
    }

    protected Unit getTimeUnit() throws Exception {
        return DataUtil.parseUnit(fc.getTimeUnit().toString());
    }

    /**
     * Gets the trajectory collection beans.
     *
     * @param trajCollection the traj collection
     * @return the trajectory collection beans
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private List<TrajectoryFeatureBean> getTrajectoryCollectionBeans(
            TrajectoryFeatureCollection trajCollection)
            throws IOException {
        List<TrajectoryFeatureBean> beans =
            new ArrayList<TrajectoryFeatureBean>();

        PointFeatureCollectionIterator iter =
            trajCollection.getPointFeatureCollectionIterator();
        while (iter.hasNext()) {
            PointFeatureCollection pob = iter.next();
            TrajectoryFeatureBean trajBean =
                new TrajectoryFeatureBean((TrajectoryFeature) pob);
            if (trajBean.pf != null) {  // may have missing values
                beans.add(trajBean);
            }
        }

        return beans;
    }

    /**
     * Helps init method get observations
     *
     * @param fc the fc
     * @return the trajectory feature bean
     */
    protected TrajectoryFeatureBean initHelper(DsgFeatureCollection fc) throws IOException{
        TrajectoryFeatureBean trajBean =
            new TrajectoryFeatureBean((TrajectoryFeature) fc);
        beanCache.put(fc.getName(), trajBean);

        return trajBean;
    }

    /**
     * Init method.
     *
     *
     * @param trajBean trajectory bean
     *
     * @throws Exception the exception
     */
    protected void init(TrajectoryFeatureBean trajBean) throws Exception {
        StructureData                 pfsd    = trajBean.pf.getFeatureData();

        this.trajBean = trajBean;
        List<StructureMembers.Member> members = pfsd.getMembers();

        for (int i = 0; i < members.size(); i++) {

            StructureMembers.Member mb   = members.get(i);
            String                  ustr = mb.getUnitsString();
            Unit                    unit = null;

            /**
             * Introducing a units cache here makes this method spend from 70%
             * to 50% of the total time in this 'if' block.
             */
            if ((ustr != null) && !ustr.equalsIgnoreCase("none")) {
                if (unitsCache.get(ustr) != null) {
                    unit = unitsCache.get(ustr);
                } else {
                    try {
                        unit = Util.parseUnit(ustr);
                    } catch (visad.VisADException e) {
                        unit = null;
                    }
                    if(unit != null)
                     unitsCache.put(ustr, unit);
                }
            }

            if ((unit != null)
                    && unit.isConvertible(CommonUnit.secondsSinceTheEpoch)
                    && !(unit instanceof DerivedUnit)) {
                addVariable(new VarInfo(mb.getName(), mb.getDescription(),
                                        DATA_CHOOSER_TREE_NODE_BASIC, unit));
                varTime = mb.getName();
            } else if ((unit != null)
                       && unit.isConvertible(CommonUnits.KILOMETER)) {
                if (mb.getName().equalsIgnoreCase("ALTITUDE")
                        || mb.getName().equalsIgnoreCase("ALT")) {

                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(),
                                            DATA_CHOOSER_TREE_NODE_BASIC,
                                            unit));
                    varAltitude = mb.getName();
                } else if (mb.getName().equalsIgnoreCase("DEPTH")) {
                    positive = -1;
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(),
                                            DATA_CHOOSER_TREE_NODE_BASIC,
                                            unit));
                    varAltitude = mb.getName();
                } else {
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), unit));
                    addVariableData(mb.getName(), mb.getDataArray().copy());
                }
            } else if ((unit != null)
                       && unit.isConvertible(CommonUnits.DEGREE)) {


                if (mb.getName().equalsIgnoreCase("LATITUDE")
                        || mb.getName().equalsIgnoreCase("LAT")) {
                    varLatitude = mb.getName();
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(),
                                            DATA_CHOOSER_TREE_NODE_BASIC,
                                            unit));
                } else if (mb.getName().equalsIgnoreCase("LONGITUDE")
                           || mb.getName().equalsIgnoreCase("LON")) {
                    varLongitude = mb.getName();
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(),
                                            DATA_CHOOSER_TREE_NODE_BASIC,
                                            unit));
                } else {
                    addVariable(new VarInfo(mb.getName(),
                                            mb.getDescription(), unit));
                    addVariableData(mb.getName(), mb.getDataArray().copy());
                }

            } else {
                addVariable(new VarInfo(mb.getName(), mb.getDescription(),
                                        unit));
                addVariableData(mb.getName(), mb.getDataArray().copy());
            }
        }

        Range rg = getDataRange();

        times     = getTime(rg);


        startTime = getStartTime();

        endTime   = getEndTime();
    }

    /**
     * Get the full range. Include the stride
     *
     * @return The range
     *
     * @throws Exception On badness
     */
    protected Range getDataRange() throws Exception {
        Range range = new Range(0, trajBean.pfs.size() - 1);
        return range;
    }

    /**
     * Get number of points in track.
     *
     * @return number of points
     */
    public int getNumberPoints() {
        return trajBean.pfs.size();
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
            times = getDoubleData(range, varTime);
        }
        return times;

    }

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * test.
     */
    private void testit() {
        try {
            Trace.call1("TrackInfo.rowRead-new");
            //test2();
            Trace.call2("TrackInfo.rowRead-new");
        } catch (Exception exc) {}
    }


    /**
     * test.
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
     * {@inheritDoc}
     */
    protected float[] getAltitude(Range range) throws Exception {
        float[] fdata = new float[range.length()];
        return trajBean.getAltitudes(range);
    }

    /**
     * {@inheritDoc}
     */
    protected float[] getLatitude(Range range) throws Exception {
        float[] fdata = new float[range.length()];
        return trajBean.getLatitudes(range);
    }

    /**
     * {@inheritDoc}
     */
    protected float[] getLongitude(Range range) throws Exception {
        float[] fdata = new float[range.length()];
        return trajBean.getLongitudes(range);
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

    public float[] getFloatData(Range range, String var)
            throws Exception {
        trajBean = beanCache.get(this.fc.getName());
        trajBean.setPfs();
        Range range1 = new Range(0, trajBean.npts - 1);
        return trajBean.getFloatData(range1, var);
    }
    /**
     * {@inheritDoc}
     */
    public double[] getDoubleData(Range range, String var) throws Exception {
        trajBean = beanCache.get(this.fc.getName());
        trajBean.setPfs();
        return trajBean.getDoubleData(range, var);
    }


    /**
     * Get the string values for the var.
     *
     * @param range subset. May be null.
     * @param var The var
     * @return string values
     * @throws Exception On badness
     */
    public String[] getStringData(Range range, String var) throws Exception {
        return trajBean.getStringData(range, var);
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
                    double[] dvalues = getDoubleData(range,
                                           var.getShortName());
                    if (var.getRealType() == null) {
                        //???
                    }

                    Data[] firstTuple = null;
                    for (int obIdx = 0; obIdx < numObs; obIdx++) {
                        Data[] tupleArray = (Data[]) tuples.get(obIdx);
                        ((double[]) reals.get(obIdx))[realCnt] =
                            dvalues[obIdx];
                        if (firstTuple != null) {
                            tupleArray[varIdx] =
                                ((Real) firstTuple[varIdx]).cloneButValue(
                                    dvalues[obIdx]);
                        } else {
                            firstTuple         = tupleArray;
                            tupleArray[varIdx] = (var.getUnit() == null)
                                    ? new Real(var.getRealType(),
                                    dvalues[obIdx])
                                    : new Real(var.getRealType(),
                                    dvalues[obIdx], var.getUnit());
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
     * The Class StationBean.
     */
    public static class StationBean extends FeatureBean implements Station {

        private StationFeature stnFeat;
        private int npts = -1;

        /**
         *
         */
        public StationBean() {}

        /**
         *
         */
        public StationBean(StructureData sdata) throws IOException {
            super(sdata);
        }

        /**
         *
         */
        public StationBean(Station s) throws IOException {
            super(((StationFeature) s).getFeatureData());
            this.stnFeat = (StationFeature) s;
            this.npts = s.getNobs();
        }

        // for BeanTable
        public static String hiddenProperties() {
            return "latLon";
        }

        /**
         * Gets the nobs.
         *
         * @return the nobs
         */
        public int getNobs() {
            return npts;
        }

        /**
         * Sets the nobs.
         *
         * @param npts the new nobs
         */
        public void setNobs(int npts) {
            this.npts = npts;
        }

        /**
         * {@inheritDoc}
         */
        public String getWmoId() {
            return stnFeat.getWmoId();
        }

        // all the station dependent methods need to be overridden
        public String getName() {
            return stnFeat.getName();
        }

        public String getDescription() {
            return stnFeat.getDescription();
        }

        public double getLatitude() {
            return stnFeat.getLatitude();
        }

        public double getLongitude() {
            return stnFeat.getLongitude();
        }

        public double getAltitude() {
            return stnFeat.getAltitude();
        }

        public LatLonPoint getLatLon() {
            return stnFeat.getLatLon();
        }

        public boolean isMissing() {
            return stnFeat.isMissing();
        }

        public int compareTo(Station so) {
            return getName().compareTo(so.getName());
        }
    }

    /**
     * The Class TrajectoryFeatureBean.
     */
    public static class TrajectoryFeatureBean extends StationBean {

        /** The npts. */
        int npts;

        /** The pfc. */
        TrajectoryFeature pfc;

        /** The pf. */
        PointFeature pf;

        /** The pfs. */
        List<PointFeature> pfs;

        StructureData sdata;
        /**
         * Instantiates a new trajectory feature bean.
         *
         * @param pfc the pfc
         */
        public TrajectoryFeatureBean(TrajectoryFeature pfc) throws IOException {
            this.pfc = pfc;
            this.sdata = pfc.getFeatureData();
            this.pfs = new ArrayList<PointFeature>();

            try {
                while (pfc.hasNext()) {
                    pfs.add(pfc.next());
                }
                pf = pfs.get(0);
            } catch (IOException ioe) {
            }

            npts = pfc.size();
        }

        /**
         * when there is multiple trajectories, pfs needs
         * to be reset
         */
        public void setPfs(){
            try {
                pfc.resetIteration();
                pfs = new ArrayList<PointFeature>();
                while (pfc.hasNext()) {
                    pfs.add(pfc.next());
                }
            } catch (IOException ioe) { }

        }
        // for BeanTable

        /**
         * Hidden properties.
         *
         * @return the string
         */
        static public String hiddenProperties() {
            return "latLon";
        }

        /**
         * {@inheritDoc}
         */
        public void setNobs(int npts) {
            this.npts = npts;
        }

        /**
         * {@inheritDoc}
         */
        public int getNobs() {
            return npts;
        }

        /**
         * {@inheritDoc}
         */
        public String getName() {
            return pfc.getName();
        }

        /**
         * {@inheritDoc}
         */
        public String getDescription() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public String getWmoId() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        public double getLatitude() {
            return pf.getLocation().getLatitude();
        }

        /**
         * {@inheritDoc}
         */
        public double getLongitude() {
            return pf.getLocation().getLongitude();
        }

        /**
         * {@inheritDoc}
         */
        public double getAltitude() {
            return pf.getLocation().getAltitude();
        }

        /**
         * {@inheritDoc}
         */
        public ucar.unidata.geoloc.LatLonPoint getLatLon() {
            return pf.getLocation().getLatLon();
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo(Station so) {
            return getName().compareTo(so.getName());
        }

        /**
         * {@inheritDoc}
         */
        public boolean isMissing() {
            return Double.isNaN(getLatitude());
        }

        /**
         * {@inheritDoc}
         */
        public double[] getTimes(Range range) {
            double[] fdata  = new double[range.length()];
            int     first  = range.first();
            int     stride = range.stride();
            int     last   = range.last();

            int     i      = first;
            int     j      = 0;

            while (i <= last) {
                PointFeature  pf0 = pfs.get(i);
                fdata[j++] = pf0.getObservationTime();
                i          = i + stride;
            }
            return fdata;
        }
        /**
         * Gets the latitudes.
         *
         * @param range the range
         * @return the latitudes
         */
        public float[] getLatitudes(Range range) {
            float[] fdata  = new float[range.length()];
            int     first  = range.first();
            int     stride = range.stride();
            int     last   = range.last();

            int     i      = first;
            int     j      = 0;

            while (i <= last) {
                PointFeature  pf0 = pfs.get(i);
                fdata[j++] = (float)pf0.getLocation().getLatitude();
                i          = i + stride;
            }

            return fdata;
        }

        /**
         * Gets the longitudes.
         *
         * @param range the range
         * @return the longitudes
         */
        public float[] getLongitudes(Range range) {
            float[] fdata  = new float[range.length()];
            int     first  = range.first();
            int     stride = range.stride();
            int     last   = range.last();

            int     i      = first;
            int     j      = 0;

            while (i <= last)  {
                PointFeature  pf0 = pfs.get(i);
                fdata[j++] = (float)pf0.getLocation().getLongitude();
                i          = i + stride;
            }

            return fdata;
        }

        /**
         * Gets the altitudes.
         *
         * @param range the range
         * @return the altitudes
         */
        public float[] getAltitudes(Range range) {
            float[] fdata  = new float[range.length()];
            int     first  = range.first();
            int     stride = range.stride();
            int     last   = range.last();

            int     i      = first;
            int     j      = 0;

            while (i <= last)  {
                PointFeature  pf0 = pfs.get(i);
                if(Double.isNaN(pf0.getLocation().getAltitude()))
                    fdata[j++] = 0.0f;
                else
                    fdata[j++] = (float)pf0.getLocation().getAltitude();
                i          = i + stride;
            }

            return fdata;
        }

        /**
         * Gets the float data.
         *
         * @param range the range
         * @param varStr the var str
         * @return the float data
         * @throws Exception the exception
         */
        public float[] getFloatData(Range range, String varStr)
                throws Exception {
            float[] fdata  = new float[range.length()];
            int     first  = range.first();
            int     stride = range.stride();
            int     last   = range.last();

            int     i      = first;
            int     j      = 0;

            while (i <= last)  {
                PointFeature  pf0 = pfs.get(i);
                StructureData std = pf0.getFeatureData();
                Array         a   = std.getArray(varStr);
                fdata[j++] = a.getFloat(0);
                i          = i + stride;
            }

            return fdata;
        }

        /**
         * Gets the double data.
         *
         * @param range the range
         * @param varStr the var str
         * @return the double data
         * @throws Exception the exception
         */
        public double[] getDoubleData(Range range, String varStr)
                throws Exception {
            double[] fdata  = new double[range.length()];
            int      first  = range.first();
            int      stride = range.stride();
            int      last   = range.last();

            int      i      = first;
            int      j      = 0;


            while (i <= last)   {
                PointFeature  pf0 = pfs.get(i);
                StructureData std = pf0.getFeatureData();
                Array         a   = std.getArray(varStr);
                fdata[j++] =  a.getDouble(0);
                i          = i + stride;
            }
            return fdata;
        }


        /**
         * Gets the String data.
         *
         * @param range the range
         * @param varStr the var str
         * @return the double data
         * @throws Exception the exception
         */
        public String[] getStringData(Range range, String varStr)
                throws Exception {
            String[] fdata  = new String[range.length()];

            return fdata;
        }
    }

    /**
     * The Class PointFeatureTypeInfo.
     */
    public static class PointFeatureTypeInfo extends CDMTrajectoryFeatureTypeInfo {

        /** The pfc. */
        private PointFeatureCollection pfc;

        /**
         * Instantiates a new point feature type info.
         *
         * @param adapter the adapter
         * @param dataset the dataset
         * @param pfc the pfc
         * @throws Exception the exception
         */
        public PointFeatureTypeInfo(TrajectoryFeatureTypeAdapter adapter,
                                    FeatureDatasetPoint dataset,
                                    PointFeatureCollection pfc)
                throws Exception {
            super(adapter, dataset, pfc);
            this.pfc = pfc;
            init(initHelper(pfc));
        }


        /**
         * {@inheritDoc}
         */
        protected Unit getTimeUnit() throws Exception {
            //There has got to be a better way to do this.

            String  timeUnitString = super.getTimeUnit().toString();

            return DataUtil.parseUnit(timeUnitString);
        }
    }

    /**
     * The Class TrajectoryFeatureTypeInfo.
     */
    public static class TrajectoryFeatureTypeInfo extends CDMTrajectoryFeatureTypeInfo {

        /** The tfc. */
        private TrajectoryFeatureCollection tfc;

        /**
         * Instantiates a new trajectory feature type info.
         *
         * @param adapter the adapter
         * @param dataset the dataset
         * @param tfc the tfc
         * @throws Exception the exception
         */
        public TrajectoryFeatureTypeInfo(
                TrajectoryFeatureTypeAdapter adapter,
                FeatureDatasetPoint dataset, TrajectoryFeatureCollection tfc)
                throws Exception {
            super(adapter, dataset, tfc);
            this.tfc = tfc;

            TrajectoryFeatureBean trajBean = null;
            PointFeatureCollectionIterator iter =
                tfc.getPointFeatureCollectionIterator();
            while (iter.hasNext()) {
                trajBean = initHelper(iter.next());
            }
            init(trajBean);
        }

        /**
         * {@inheritDoc}
         */
        protected Unit getTimeUnit() throws Exception {
            return DataUtil.parseUnit("days since 1950-01-01T00:00:00Z");
        }
    }
}
