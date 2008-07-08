/*
 * $Id: PointObFactory.java,v 1.53 2007/05/22 14:56:04 dmurray Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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




package ucar.unidata.data.point;


import au.gov.bom.aifs.osa.analysis.Barnes;


import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.Index0D;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.Station;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.StationObsDatatype;
import ucar.nc2.dt.point.*;

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.GeoLocationInfo;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.DataChoice;

import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;

import visad.*;

import visad.georef.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * Factory for dealing with point observations
 *
 * @author MetApps Development Team
 * @version $Revision: 1.53 $ $Date: 2007/05/22 14:56:04 $
 */
public class PointObFactory {

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(PointObFactory.class.getName());

    /** Constructor */
    public PointObFactory() {}

    /**
     * Returns a point observation as a FieldImpl of type.
     * The type is:
     * <pre>
     *  (Time -> ((lat, lon, alt) -> (data)))
     * </pre>
     *
     * @param point    point observation
     * @return  PointOb as a field
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl makePointObField(PointOb point)
            throws VisADException, RemoteException {

        return new PointObField(point.getEarthLocation(),
                                point.getDateTime(), point.getData());
    }

    /**
     * From a field of point observations, reorder them with time
     * as the outer dimension.
     *
     * @param pointObs    Field of point observations (index -> pointobs)
     * @return    time sequence of obs (time -> (index -> pointobs))
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl makeTimeSequenceOfPointObs(FieldImpl pointObs)
            throws VisADException, RemoteException {
        return makeTimeSequenceOfPointObs(pointObs, -1);
    }

    /**
     * From a field of point observations, reorder them with time
     * as the outer dimension.
     *
     * @param pointObs    Field of point observations (index -> pointobs)
     * @param lumpMinutes If greater then 0 is used to lump the times of the point obs
     * together
     * @return    time sequence of obs (time -> (index -> pointobs))
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl makeTimeSequenceOfPointObs(FieldImpl pointObs,
            int lumpMinutes)
            throws VisADException, RemoteException {
        int  numObs = pointObs.getDomainSet().getLength();
        List obs    = new ArrayList();
        for (int i = 0; i < numObs; i++) {
            obs.add(pointObs.getSample(i));
        }
        return makeTimeSequenceOfPointObs(obs, lumpMinutes, -1);
    }

    /**
     * From a field of point observations, reorder them with time
     * as the outer dimension. If componentIndex &gt; -1 then we extract that
     * real value from the observation tuple and use that in the range.
     * We also skip the intermediate index field and only use the first PointOb
     * for each time step
     *
     * @param pointObs    Field of point observations (index -> pointobs)
     * @param lumpMinutes If greater then 0 is used to lump the times of the point obs together
     * @param componentIndex If &gt;= 0 then make a T-&gt;componentvalue field
     * @return    time sequence of obs (time -> (index -> pointobs))
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl makeTimeSequenceOfPointObs(List pointObs,
            int lumpMinutes, int componentIndex)
            throws VisADException, RemoteException {

        Trace.call1("makeTimeSequence");


        List      uniqueTimes = new ArrayList();
        int       numObs      = pointObs.size();
        MathType  obType      = null;
        Hashtable timeToObs   = new Hashtable();
        // loop through and find all the unique times
        Trace.call1("makeTimeSequence-loop1",
                    " " + lumpMinutes + " num obs:" + numObs);
        Hashtable seenTime = new Hashtable();
        for (int i = 0; i < numObs; i++) {
            PointOb ob = (PointOb) pointObs.get(i);
            if (i == 0) {
                if (componentIndex < 0) {
                    obType = ob.getType();
                } else {
                    obType = ((Tuple) ob.getData()).getComponent(
                        componentIndex).getType();
                }
            }
            DateTime dttm = ob.getDateTime();
            if (lumpMinutes > 0) {
                double seconds = dttm.getValue();
                seconds = seconds - seconds % (lumpMinutes * 60);
                dttm    = new DateTime(seconds);
            }
            Double  dValue   = new Double(dttm.getValue());
            List    obs      = null;
            boolean contains = (seenTime.put(dValue, dValue) != null);
            if ( !contains) {
                uniqueTimes.add(dttm);
                obs = new ArrayList();
                timeToObs.put(dValue, obs);
            } else {
                obs = (List) timeToObs.get(dValue);
            }
            obs.add(ob);
        }
        Trace.call2("makeTimeSequence-loop1",
                    " #times:" + uniqueTimes.size());

        DateTime[] times = (DateTime[]) uniqueTimes.toArray(
                               new DateTime[uniqueTimes.size()]);
        Arrays.sort(times);
        RealType     index      = RealType.getRealType("index");

        FunctionType sampleType = new FunctionType(index, obType);
        FunctionType timeSequenceType = new FunctionType(RealType.Time,
                                            ((componentIndex < 0)
                                             ? (MathType) sampleType
                                             : obType));
        FieldImpl timeSequence = new FieldImpl(timeSequenceType,
                                     DateTime.makeTimeSet(times));

        List samples = new ArrayList();
        Trace.call1("makeTimeSequence-loop2");
        Data[] timeSamples = new Data[times.length];
        for (int i = 0; i < times.length; i++) {
            DateTime dttm   = times[i];
            Double   dValue = new Double(dttm.getValue());
            List     v      = (List) timeToObs.get(dValue);
            Data[]   obs;
            if (componentIndex < 0) {
                obs = (Data[]) v.toArray(new PointOb[v.size()]);
            } else {
                obs = new Data[v.size()];
                for (int obIdx = 0; obIdx < v.size(); obIdx++) {
                    obs[obIdx] = ((Tuple) ((PointOb) v.get(
                        obIdx)).getData()).getComponent(componentIndex);
                }
            }
            Integer1DSet set = new Integer1DSet(index, v.size());
            if (componentIndex < 0) {
                FieldImpl sample = new FieldImpl(sampleType, set);
                sample.setSamples(obs, false, false);
                timeSamples[i] = sample;
            } else {
                timeSamples[i] = obs[0];
            }

        }
        Trace.call2("makeTimeSequence-loop2");
        timeSequence.setSamples(timeSamples, false, false);
        Trace.call2("makeTimeSequence");
        return timeSequence;
    }

    /**
     * Returns a subset of the field of point observations that lie
     * within the boundaries of the LinearLatLonSet.
     *
     * @param pointObs    set of obs.
     * @param bounds      LinearLatLonSet bounding box
     * @return   subset within the bounds
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl subSet(FieldImpl pointObs, LinearLatLonSet bounds)
            throws VisADException, RemoteException {
        long      t1             = System.currentTimeMillis();
        boolean   isTimeSequence = GridUtil.isTimeSequence(pointObs);
        FieldImpl subSet         = null;
        if (isTimeSequence) {
            Trace.call1("subSet");
            Set timeSet  = pointObs.getDomainSet();
            int numTimes = timeSet.getLength();
            subSet = new FieldImpl((FunctionType) pointObs.getType(),
                                   timeSet);
            List samples = new Vector();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) pointObs.getSample(i);
                FieldImpl subTime = findIntersection(oneTime, bounds);
                samples.add(subTime);
            }
            subSet.setSamples(
                (Data[]) samples.toArray(new Data[samples.size()]), false,
                false);
            Trace.call2("subSet");
        } else {
            subSet = findIntersection(pointObs, bounds);
        }
        //        System.out.println("Subsetting took : " +
        //            (System.currentTimeMillis() - t1) + " ms");
        return subSet;
    }

    /**
     * Find the intersection of a field of PointObs and the lat/lon bounds
     *
     * @param pointObs     Field of point observations
     * @param bounds       lat/lon bounds
     * @return  Field of point obs in the bounds
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private static FieldImpl findIntersection(FieldImpl pointObs,
            LinearLatLonSet bounds)
            throws VisADException, RemoteException {
        if ((pointObs == null) || pointObs.isMissing()) {
            return pointObs;
        }
        FieldImpl retField  = null;
        Set       domainSet = pointObs.getDomainSet();
        int       numObs    = domainSet.getLength();
        Unit[]    units     = bounds.getSetUnits();
        int latIndex =
            (((RealType) ((SetType) bounds.getType()).getDomain()
                .getComponent(0)).equals(RealType.Latitude) == true)
            ? 0
            : 1;
        Vector    v      = new Vector();
        float[][] values = new float[2][1];
        //        Trace.call1("findIntersection-loop 1");
        for (int i = 0; i < numObs; i++) {
            PointOb       ob = (PointOb) pointObs.getSample(i);
            EarthLocation el = ob.getEarthLocation();
            values[0][0] = (float) el.getLatitude().getValue(units[latIndex]);
            values[1][0] =
                (float) el.getLongitude().getValue(units[1 - latIndex]);
            float[][] grids = bounds.valueToGrid(values);
            if ((grids[0][0] == grids[0][0])
                    && (grids[1][0] == grids[1][0])) {  //not NaN
                v.add(ob);  // is in the bounds
            }
        }
        //        Trace.call2("findIntersection-loop 1");
        //      Trace.call2("findIntersection");
        //System.out.println("found " + v.size() + " obs in region");
        if (v.size() == numObs) {
            retField = pointObs;  // all were in domain, just return input
        } else if ( !v.isEmpty()) {
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), v.size()));
            retField.setSamples((PointOb[]) v.toArray(new PointOb[v.size()]),
                                false, false);
        } else {
            PointOb point = (PointOb) pointObs.getSample(0);
            retField = new FieldImpl(
                (FunctionType) pointObs.getType(),
                new Integer1DSet(
                    ((SetType) domainSet.getType()).getDomain(), 1));
            retField.setSamples(
                new PointOb[] {
                    new PointObTuple(
                        new EarthLocationLite(
                            new Real(RealType.Latitude),
                            new Real(RealType.Longitude),
                            new Real(RealType.Altitude)), new DateTime(
                                Double.NaN), (point.getData()
                                instanceof RealTuple)
                                             ? new RealTuple(
                                             (RealTupleType) point.getData()
                                                 .getType())
                                             : new Tuple((TupleType) point
                                             .getData().getType())) }, false);
        }
        return retField;
    }

    /**
     * Take a field of data and turn it into a field of PointObs.  Right
     * now, this assumes a surface data from an ADDE server.
     *
     * @param input     FieldImpl of raw VisAD data
     *
     * @return field of PointObs
     *
     * @throws VisADException  couldn't create the VisAD data
     */

    public static FieldImpl makePointObsFromField(FieldImpl input)
            throws VisADException {
        return makePointObsFromField(input, 0.0, 0.0);
    }


    /**
     *  Take a field of data and turn it into a field of PointObs.  Right
     *  now, this assumes a surface data from an ADDE server.
     *
     *  @param input     FieldImpl of raw VisAD data
     *  @param binRoundTo time bin  round to
     *  @param binWidth time bin size
     *
     *  @return The data
     *
     *  @throws VisADException On badness
     */
    public static FieldImpl makePointObsFromField(FieldImpl input,
            double binRoundTo, double binWidth)
            throws VisADException {

        FieldImpl retField = null;
        try {
            // first check to see if we can make a location ob
            // input has to be of form (index -> (parm1, parm2, parm3, ...., parmN))

            //System.out.print("Starting to make PointObs for ");
            TupleType    type     = null;
            Gridded1DSet indexSet = null;
            try {
                type = (TupleType) ((FunctionType) input.getType())
                    .getRange();
                indexSet = (Gridded1DSet) input.getDomainSet();
            } catch (ClassCastException ce) {
                throw new IllegalArgumentException(
                    "don't know how to convert input to a point ob");
            }
            //System.out.println(indexSet.getLength() + " obs");
            boolean allReals = (type instanceof RealTupleType);

            // check for time fields (DAY & TIME or DATE & HMS)
            int     dayIndex   = type.getIndex("DAY");
            int     timeIndex  = type.getIndex("TIME");
            int     dateIndex  = type.getIndex("DATE");
            int     hmsIndex   = type.getIndex("HMS");
            boolean hasDayTime = ((dayIndex != -1) && (timeIndex != -1));
            boolean hasDateHMS = ((dateIndex != -1) && (hmsIndex != -1));
            if ( !hasDayTime && !hasDateHMS) {
                throw new IllegalArgumentException(
                    "can't find DateTime components");
            }
            if (hasDateHMS) {
                dayIndex  = dateIndex;
                timeIndex = hmsIndex;
            }

            // Check for LAT/LON/ALT
            int latIndex = type.getIndex(RealType.Latitude);
            int lonIndex = type.getIndex(RealType.Longitude);
            int altIndex = type.getIndex(RealType.Altitude);
            if ((latIndex == -1) || (lonIndex == -1)) {
                throw new IllegalArgumentException("can't find lat/lon");
            }

            int[] indicies = new int[] { dayIndex, timeIndex, latIndex,
                                         lonIndex, altIndex };

            int numVars        = type.getDimension();
            int numNotRequired = numVars - ((altIndex != -1)
                                            ? 5
                                            : 4);
            //System.out.println("Of " + numVars + " vars, " + numNotRequired + 
            //                   " are not required");

            int[] notReqIndices = new int[numNotRequired];

            int   l             = 0;
            for (int i = 0; i < numVars; i++) {
                if ((i != dayIndex) && (i != timeIndex) && (i != latIndex)
                        && (i != lonIndex) && (i != altIndex)) {
                    notReqIndices[l++] = i;
                }
            }

            //      ucar.unidata.idv.test.TestManager.gc();
            //      Trace.msg("starting memory="+Misc.usedMemory());

            PointOb[] obs = new PointObTuple[indexSet.getLength()];
            //      Trace.call1("POB.makePOB"," Length:" + indexSet.getLength() + " num:" + numNotRequired);


            Trace.call1("loop-1", " Size:" + indexSet.getLength());
            int               length      = indexSet.getLength();
            List              times       = new ArrayList();
            boolean           hasTimeOnly = false;
            GregorianCalendar cal         = null;


            for (int i = 0; i < length; i++) {
                Tuple ob      = (Tuple) input.getSample(i);
                Real  realDay = (Real) ob.getComponent(dayIndex);
                // might only have time in seconds
                //Can this be done just once?
                hasTimeOnly =
                //realDay.getUnit().equals(CommonUnit.second);
                CommonUnit.second.equals(realDay.getUnit());
                int    day  = (int) realDay.getValue();
                double time = ((Real) ob.getComponent(timeIndex)).getValue();

                if ( !hasTimeOnly) {
                    if (hasDayTime) {
                        if (cal == null) {
                            cal = McIDASUtil.makeCalendarForDayTimeToSecs();
                        }
                        time = McIDASUtil.mcDayTimeToSecs(day, (int) time,
                                cal);
                    } else {
                        time = McIDASUtil.mcDateHmsToSecs(day, (int) time);
                    }
                }
                times.add(new DateTime(time));
            }
            Trace.call2("loop-1");
            times = binTimes(times, binRoundTo, binWidth);

            TupleType tupleType      = null;
            TupleType finalTupleType = null;
            Trace.call1("loop-2", " num vars: " + numNotRequired);
            for (int i = 0; i < length; i++) {
                DateTime dateTime = (DateTime) times.get(i);
                Tuple    ob       = (Tuple) input.getSample(i);
                // get location
                EarthLocation location =
                    new EarthLocationLite((Real) ob.getComponent(latIndex),
                                          (Real) ob.getComponent(lonIndex),
                                          (altIndex != -1)
                                          ? (Real) ob.getComponent(altIndex)
                                          : new Real(RealType.Altitude, 0));

                Data rest = null;
                // now make data
                Data[] others = (allReals == true)
                                ? new Real[numNotRequired]
                                : new Data[numNotRequired];
                if (allReals) {
                    for (int j = 0; j < numNotRequired; j++) {
                        others[j] = (Real) ob.getComponent(notReqIndices[j]);
                    }
                } else {
                    for (int j = 0; j < numNotRequired; j++) {
                        others[j] = (Data) ob.getComponent(notReqIndices[j]);
                    }
                }
                if (tupleType == null) {
                    tupleType = Tuple.buildTupleType(others);
                }

                rest = ((allReals == true)
                        ? new RealTuple((RealTupleType) tupleType,
                                        (Real[]) others, null, null, false)
                        : new Tuple(tupleType, others, false, false));

                if (finalTupleType == null) {
                    PointObTuple pot = new PointObTuple(location, dateTime,
                                           rest);
                    obs[i]         = pot;
                    finalTupleType =
                        Tuple.buildTupleType(pot.getComponents());
                } else {
                    obs[i] = new PointObTuple(location, dateTime, rest,
                            finalTupleType, false);
                }
            }
            Trace.call2("loop-2");


            retField = new FieldImpl(
                new FunctionType(
                    ((SetType) indexSet.getType()).getDomain(),
                    obs[0].getType()), indexSet);
            retField.setSamples(obs, false, false);
        } catch (RemoteException re) {
            throw new VisADException("got RemoteException " + re);
        }
        return retField;
    }


    /**
     * Make point obs from a PointObsDataset
     *
     * @param input pointobs dataset to load
     *
     * @return FieldImpl of form (index) -&gt; PointOb
     *
     * @throws Exception problem creating the ob or a cancel
     */
    public static FieldImpl makePointObs(PointObsDataset input)
            throws Exception {
        return makePointObs(input, 60, 15);
    }


    /**
     * Bin the times
     *
     * @param times List of times
     * @param binRoundTo round to factor
     * @param binWidth bin size
     *
     * @return Binned times
     *
     * @throws VisADException On badness
     */
    public static List binTimes(List times, double binRoundTo,
                                double binWidth)
            throws VisADException {
        binRoundTo = binRoundTo * 60;
        binWidth   = binWidth * 60;
        if (binWidth <= 0) {
            return times;
        }
        //        System.err.println ("binWidth:" + binWidth + " rt:" + binRoundTo);

        double   minTime = Double.MAX_VALUE;
        DateTime minDttm = null;
        for (int i = 0; i < times.size(); i++) {
            DateTime dttm = (DateTime) times.get(i);
            double   time = dttm.getValue(CommonUnit.secondsSinceTheEpoch);
            if (time < minTime) {
                minTime = time;
                minDttm = dttm;
            }
        }
        //        System.err.println ("min time:" + minDttm);

        List   newTimes = new ArrayList();
        double baseTime;
        int    roundToSeconds = (int) binRoundTo;
        //        System.err.println ("round to seconds:" + roundToSeconds);
        if (roundToSeconds == 0) {
            baseTime = minTime;
        } else {
            //            System.err.println ("minTime:" + new DateTime(minTime));
            baseTime = minTime - minTime % (60 * 60);
            //            System.err.println ("1st:" + new DateTime(baseTime));
            baseTime -= (60 * 60);
            //            System.err.println ("2nd:" + new DateTime(baseTime));
            baseTime = baseTime + roundToSeconds;
            //            System.err.println ("3rd:" + new DateTime(baseTime));
        }
        //        System.err.println ("round to:" + binRoundTo +" bin width:" + binWidth +" base time:"  + new DateTime(baseTime));
        Hashtable seen = new Hashtable();
        int       ucnt = 0;
        for (int i = 0; i < times.size(); i++) {
            DateTime dttm    = (DateTime) times.get(i);
            double   time    = dttm.getValue(CommonUnit.secondsSinceTheEpoch);
            double   rem     = time - baseTime;
            double   newTime = baseTime + binWidth * (int) (rem / binWidth);
            DateTime newDttm = new DateTime(newTime);
            //            if (seen.get(newDttm) == null) {
            //                ucnt++;
            //                if(ucnt<10)
            //                    System.err.println (dttm +" -- > " + newDttm);
            //                seen.put(newDttm, newDttm);
            //            }
            newTimes.add(newDttm);
        }
        return newTimes;
    }




    /**
     * Make point obs
     *
     * @param input the data set
     * @param binRoundTo bin round to
     * @param binWidth time bin size
     *
     * @return The field
     *
     * @throws Exception On badness
     */
    public static FieldImpl makePointObs(PointObsDataset input,
                                         double binRoundTo, double binWidth)
            throws Exception {
        return makePointObs(input, binRoundTo, binWidth, null);
    }

    /*
    public static List PointOb findClosestObs(EarthLocation el, FieldImpl theField)
        throws VisADException, RemoteException {
        return null;
    }

    private static  PointOb findClosestOb(EarthLocation el, FieldImpl theField)
        throws VisADException, RemoteException {
        if ((el == null) || (theField == null)) {
            return null;
        }
        Set         domainSet   = theField.getDomainSet();
        int         numObs      = domainSet.getLength();
        PointOb     closestOb   = null;
        LatLonPoint llp         = el.getLatLonPoint();
        Bearing     bearing     = null;
        for (int i = 0; i < numObs; i++) {
            Object tmp = theField.getSample(i);
            if ( !(tmp instanceof PointOb)) {
                continue;
            }
            PointOb       ob       = (PointOb) tmp;
            EarthLocation obEl     = ob.getEarthLocation();
            xxx
            if (distance < minDistance) {
                closestOb   = ob;
                minDistance = distance;
            }
        }
        return closestOb;
    }
    */







    /**
     * Make point obs
     *
     * @param input the data set
     * @param binRoundTo bin round to
     * @param binWidth time bin size
     * @param llr bounding box
     *
     * @return The field
     *
     * @throws Exception On badness
     */
    public static FieldImpl makePointObs(PointObsDataset input,
                                         double binRoundTo, double binWidth,
                                         LatLonRect llr)
            throws Exception {



        Object  loadId = JobManager.getManager().startLoad("PointObFactory");

        List    actualVariables    = input.getDataVariables();
        int     numVars            = actualVariables.size();

        boolean needToAddStationId = false;
        String  stationFieldName   = null;

        //Is this station data
        if (input instanceof StationObsDataset) {
            //TODO: Get the variable name used for the station id:
            //stationFieldName = ((StationObsDataset)input).getStationVarName();

            //If it is we need to see if there is already a station id in the data itself
            needToAddStationId = true;

            /**
             * For now don't do this...
             * for (Iterator iter = actualVariables.iterator(); needToAddStationId&&iter.hasNext(); ) {
             *   VariableSimpleIF var = (VariableSimpleIF) iter.next();
             *   String name = var.getShortName();
             *   if (Misc.equals(name,PointOb.PARAM_ID) || Misc.equals(name,PointOb.PARAM_IDN)) {
             *       needToAddStationId = false;
             *   }
             *   String canonical = DataAlias.aliasToCanonical(name);
             *   if (Misc.equals(canonical,PointOb.PARAM_ID) || Misc.equals(canonical,PointOb.PARAM_IDN)) {
             *       System.err.println ("Don't need to add id. Already have it in the data:" + name);
             *       needToAddStationId = false;
             *   }
             * }
             */
        }



        int varIdxBase = 0;
        if (needToAddStationId) {
            numVars++;
            varIdxBase = 1;
        }

        List shortNamesList = new ArrayList();

        log_.debug("number of data variables = " + numVars);
        boolean[]    isVarNumeric = new boolean[numVars];
        boolean      allReals     = true;
        ScalarType[] types        = new ScalarType[numVars];
        Unit[]       varUnits     = new Unit[numVars];



        List         numericTypes = new ArrayList();
        List         numericUnits = new ArrayList();
        List         stringTypes  = new ArrayList();

        //If we really have a StationObsDataset then we need to add in the station id
        //into the data fields 
        if (needToAddStationId) {
            isVarNumeric[0] = false;
            if (stationFieldName == null) {
                stationFieldName = PointOb.PARAM_ID;
            }
            shortNamesList.add(stationFieldName);
            types[0] = TextType.getTextType(stationFieldName);
            stringTypes.add(types[0]);
            allReals = false;
        }


        //        Trace.call1("loop-1");
        int varIdx = varIdxBase;
        for (Iterator iter = actualVariables.iterator(); iter.hasNext(); ) {
            VariableSimpleIF var = (VariableSimpleIF) iter.next();
            shortNamesList.add(var.getShortName());
            isVarNumeric[varIdx] = !((var.getDataType() == DataType.STRING)
                                     || (var.getDataType() == DataType.CHAR));
            if ( !isVarNumeric[varIdx]) {
                allReals = false;
            }

            DataChoice.addCurrentName(new TwoFacedObject("Point Data"+">" + var.getShortName() ,var.getShortName())); 

            // now make types
            if (isVarNumeric[varIdx]) {  // RealType
                Unit unit = DataUtil.parseUnit(var.getUnitsString());
                types[varIdx] = DataUtil.makeRealType(var.getShortName(),
                        unit);
                varUnits[varIdx] = unit;
                numericTypes.add(types[varIdx]);
                numericUnits.add(unit);
            } else {
                types[varIdx]    = TextType.getTextType(var.getShortName());
                varUnits[varIdx] = null;
                stringTypes.add(types[varIdx]);
            }
            varIdx++;
        }
        //        Trace.call2("loop-1");


        String[] shortNames = (String[]) shortNamesList.toArray(
                                  new String[shortNamesList.size()]);


        int    numReals   = numericTypes.size();
        int    numStrings = stringTypes.size();
        Data[] firstTuple = null;
        int    total      = input.getDataCount();
        //        System.err.println("#obs:" + total +" #vars:" +  numVars);
        int       obIdx        = 0;
        Iterator  dataIterator = input.getDataIterator(16384);
        int       NUM          = 500;
        TupleType allTupleType = (allReals
                                  ? new RealTupleType(
                                      (RealType[]) numericTypes.toArray(
                                          new RealType[numericTypes.size()]))
                                  : DoubleStringTuple.makeTupleType(
                                      numericTypes, stringTypes));
        Unit[] allUnits =
            (Unit[]) numericUnits.toArray(new Unit[numericUnits.size()]);


        Real              lat      = new Real(RealType.Latitude, 40),
                          lon      = new Real(RealType.Longitude, -100),
                          alt      = new Real(RealType.Altitude, 0);
        DateTime          dateTime = null;
        EarthLocationLite elt;
        TupleType         finalTT = null;
        PointObTuple      pot     = null;
        List              pos     = new ArrayList(100000);
        List              times   = new ArrayList(1000000);

        //First do spatial subset and collect times
        //        Trace.call1("loop-2");
        while (dataIterator.hasNext()) {
            PointObsDatatype po = (PointObsDatatype) dataIterator.next();
            ucar.nc2.dt.EarthLocation el = po.getLocation();
            if (el == null) {
                continue;
            }
            if ((llr != null)
                    && !llr.contains(el.getLatitude(), el.getLongitude())) {
                continue;
            }
            pos.add(po);
            times.add(new DateTime(po.getNominalTimeAsDate()));
        }
        //        Trace.call2("loop-2");


        //Bin times
        times = binTimes(times, binRoundTo, binWidth);

        StructureMembers.Member member;
        PointOb[]               obs = new PointOb[pos.size()];
        //Make the obs
        //        Trace.call1("loop-3");
        int size = pos.size();

        for (int i = 0; i < size; i++) {
            PointObsDatatype          po = (PointObsDatatype) pos.get(i);
            ucar.nc2.dt.EarthLocation el = po.getLocation();
            elt = new EarthLocationLite(lat.cloneButValue(el.getLatitude()),
                                        lon.cloneButValue(el.getLongitude()),
                                        alt.cloneButValue(el.getAltitude()));
            double[] realArray   = new double[numReals];
            String[] stringArray = ((numStrings == 0)
                                    ? null
                                    : new String[numStrings]);

            // make the VisAD data object
            StructureData structure = po.getData();
            int           stringCnt = 0;
            int           realCnt   = 0;
            if (needToAddStationId) {
                StationObsDatatype sod = (StationObsDatatype) po;
                stringArray[stringCnt++] = sod.getStation().getName();
            }
            for (varIdx = varIdxBase; varIdx < numVars; varIdx++) {
                member = structure.findMember((String) shortNames[varIdx]);
                if ( !isVarNumeric[varIdx]) {
                    stringArray[stringCnt++] =
                        structure.getScalarString(member);
                } else {
                    realArray[realCnt++] =
                        structure.convertScalarFloat(member);
                }
            }


            Tuple tuple = (allReals
                           ? (Tuple) new DoubleTuple(
                               (RealTupleType) allTupleType, realArray,
                               allUnits)
                           : new DoubleStringTuple(allTupleType, realArray,
                               stringArray, allUnits));


            if (finalTT == null) {
                pot = new PointObTuple(elt, (DateTime) times.get(i), tuple);
                finalTT = Tuple.buildTupleType(pot.getComponents());
            } else {
                pot = new PointObTuple(elt, (DateTime) times.get(i), tuple,
                                       finalTT, false);

            }
            obs[obIdx++] = pot;
            if (obIdx % NUM == 0) {
                if ( !JobManager.getManager().canContinue(loadId)) {
                    LogUtil.message("");
                    return null;
                }
                if (llr == null) {
                    LogUtil.message("Read " + obIdx + "/" + total
                                    + " observations");
                } else {
                    LogUtil.message("Read " + obIdx + " observations");
                }
            }
        }

        //        Trace.call2("loop-3");


        LogUtil.message("Read " + obIdx + " observations");


        LogUtil.message("Done processing point data");

        Integer1DSet indexSet =
            new Integer1DSet(RealType.getRealType("index"), obs.length);
        FieldImpl retField =
            new FieldImpl(
                new FunctionType(
                    ((SetType) indexSet.getType()).getDomain(),
                    obs[0].getType()), indexSet);
        retField.setSamples(obs, false, false);
        return retField;
    }



    /**
     * Merge a List of FieldImpls of point obs into one.
     * @param datas   List of FieldImpls of point obs
     * @return merged FieldImpl
     *
     * @throws VisADException problem getting the data
     */
    public static FieldImpl mergeData(List datas) throws VisADException {

        if (datas.isEmpty()) {
            return null;
        }
        if (datas.size() == 1) {
            return (FieldImpl) datas.get(0);
        }
        FieldImpl retField = null;
        try {
            int numObs = 0;
            for (int i = 0; i < datas.size(); i++) {
                numObs +=
                    ((FieldImpl) datas.get(i)).getDomainSet().getLength();
            }
            int curPos = 0;
            for (int i = 0; i < datas.size(); i++) {
                FieldImpl data = (FieldImpl) datas.get(i);
                if (i == 0) {
                    FunctionType retType = (FunctionType) data.getType();
                    retField =
                        new FieldImpl(retType,
                                      new Integer1DSet(retType.getDomain(),
                                          numObs));
                }

                int length = data.getDomainSet().getLength();
                for (int j = 0; j < length; j++) {
                    retField.setSample(curPos, data.getSample(j), false);
                    curPos++;
                }
            }
        } catch (RemoteException re) {
            throw new VisADException("got RemoteException " + re);
        }
        return retField;
    }

    /**
     * Make a PointOb from an EarthLocation.  The time and data
     * are bogus.
     * @param el  EarthLocation to use
     * @return PointOb
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static PointOb makePointOb(EarthLocation el)
            throws VisADException, RemoteException {
        return makePointOb(el, null);
    }

    /**
     * Make a PointOb from an EarthLocation.  The time and data
     * are bogus.
     * @param el  EarthLocation to use
     * @param dt  DateTime to use
     * @return PointOb
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static PointOb makePointOb(EarthLocation el, DateTime dt)
            throws VisADException, RemoteException {
        return makePointOb(el, dt, new RealTuple(new Real[] { new Real(0) }));
    }


    /**
     * Make a point ob
     *
     * @param el earth location
     * @param dt date time
     * @param tuple  data
     *
     * @return the point ob
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static PointOb makePointOb(EarthLocation el, DateTime dt,
                                      Tuple tuple)
            throws VisADException, RemoteException {
        if (dt == null) {
            dt = new DateTime(Double.NaN);
        }
        return new PointObTuple(el, dt, tuple);
    }

    /**
     * Make a FieldImpl of PointOb-s from an EarthLocation.  The time and data
     * are bogus.
     * @param el  EarthLocation to use
     * @return FieldImpl of index-&gt;po
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FieldImpl makePointObs(EarthLocation el)
            throws VisADException, RemoteException {
        return makePointObs(el, null);
    }

    /**
     * Make a FieldImpl of PointOb-s from an EarthLocation.  The time and data
     * are bogus.
     * @param el  EarthLocation to use
     * @param dt  DateTime to use
     * @return FieldImpl of index-&gt;po
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FieldImpl makePointObs(EarthLocation el, DateTime dt)
            throws VisADException, RemoteException {
        PointOb ob = makePointOb(el, dt);
        return makePointObs(ob);
    }

    /**
     * Make a FieldImpl from a PointOb.
     *
     * @param po The ob
     * @return FieldImpl of index-&gt;po
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FieldImpl makePointObs(PointOb po)
            throws VisADException, RemoteException {

        RealType index = RealType.getRealType("index");
        FieldImpl stationField = new FieldImpl(new FunctionType(index,
                                     po.getType()), new Integer1DSet(index,
                                         1));
        stationField.setSample(0, po, false);
        return stationField;
    }

    /**
     * Remove the time dimension from a field of point obs, returning
     * just and indexed list of the obs
     *
     * @param pointObs  time field of obs
     *
     * @return indexed list of obs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FieldImpl removeTimeDimension(FieldImpl pointObs)
            throws VisADException, RemoteException {
        if ( !GridUtil.isTimeSequence(pointObs)) {
            return pointObs;
        }
        Set          timeSet = pointObs.getDomainSet();
        List         l       = new ArrayList();
        FunctionType ft      = null;
        for (int i = 0; i < timeSet.getLength(); i++) {
            FieldImpl indexField = (FieldImpl) pointObs.getSample(i, false);
            if (ft == null) {
                ft = (FunctionType) indexField.getType();
            }
            for (int j = 0; j < indexField.getLength(); j++) {
                Data d = indexField.getSample(j, false);
                if ((d == null) || d.isMissing()) {
                    continue;
                }
                l.add(d);
            }
        }
        RealTupleType rt       = ft.getDomain();
        Integer1DSet  domain   = new Integer1DSet(rt, l.size());
        FieldImpl     retField = new FieldImpl(ft, domain);
        for (int i = 0; i < l.size(); i++) {
            retField.setSample(i, (Data) l.get(i), false, false);
        }
        return retField;
    }

    /**
     *  Perform an object analysis on a set of point obs
     *
     * @param pointObs Observations to analyze
     * @param type  RealTypes of parameter
     * @param xSpacing  x spacing (degrees)
     * @param ySpacing  y spacing (degrees)
     * @param numPasses number of passes
     *
     * @return  Grid of objectively analyzed data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FieldImpl barnes(FieldImpl pointObs, RealType type,
                                   float xSpacing, float ySpacing,
                                   int numPasses)
            throws VisADException, RemoteException {
        FieldImpl retFI = null;
        if (GridUtil.isTimeSequence(pointObs)) {
            Set timeSet = GridUtil.getTimeSet(pointObs);
            for (int i = 0; i < timeSet.getLength(); i++) {
                FieldImpl oneTime =
                    barnesOneTime((FieldImpl) pointObs.getSample(i), type,
                                  xSpacing, ySpacing, numPasses);
                if ((retFI == null) && (oneTime != null)) {
                    FunctionType ft =
                        new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            oneTime.getType());
                    retFI = new FieldImpl(ft, timeSet);
                }
                if (oneTime != null) {
                    retFI.setSample(i, oneTime, false);
                }
            }
        } else {
            retFI = barnesOneTime(pointObs, type, xSpacing, ySpacing,
                                  numPasses);
        }
        return retFI;
    }

    /**
     * Do the analysis on the single time.  Should be of the structure:
     *  (index -> PointOb)
     *
     * @param pointObs Observations to analyze
     * @param type  RealTypes of parameter
     * @param xSpacing  x spacing (degrees)
     * @param ySpacing  y spacing (degrees)
     * @param numPasses number of passes
     *
     * @return  Grid of objectively analyzed data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FlatField barnesOneTime(FieldImpl pointObs, RealType type,
                                          float xSpacing, float ySpacing,
                                          int numPasses)
            throws VisADException, RemoteException {
        int       numObs    = pointObs.getLength();
        float[][] domainPts = new float[2][numObs];
        float[][] paramVals = new float[1][numObs];
        PointOb   firstOb   = (PointOb) pointObs.getSample(0);
        // TODO: May not be a tuple
        Tuple     data  = (Tuple) firstOb.getData();
        TupleType ttype = (TupleType) data.getType();
        //System.out.println("type = " + ttype);
        int typeIndex = ttype.getIndex(type);
        if (typeIndex == -1) {
            return null;
        }
        float latMin = 90;
        float lonMin = 180;
        float latMax = -90;
        float lonMax = -180;
        for (int i = 0; i < numObs; i++) {
            PointOb       po = (PointOb) pointObs.getSample(i);
            EarthLocation el = po.getEarthLocation();
            float lat = (float) el.getLatitude().getValue(CommonUnit.degree);
            if (lat < latMin) {
                latMin = lat;
            }
            if (lat > latMax) {
                latMax = lat;
            }
            domainPts[1][i] = lat;
            float lon = (float) el.getLongitude().getValue(CommonUnit.degree);
            if (lon < -180) {
                lon += 360;
            }
            if (lon > 180) {
                lon -= 360;
            }
            if (lon < lonMin) {
                lonMin = lon;
            }
            if (lon > lonMax) {
                lonMax = lon;
            }
            domainPts[0][i] = lon;
            Tuple obData = (Tuple) po.getData();
            Real  val    = (Real) obData.getComponent(typeIndex);
            //System.out.println("val["+i+"] ="+ val);
            paramVals[0][i] = (float) val.getValue(type.getDefaultUnit());
        }
        //System.out.println("lat = " + latMin + "-"+latMax+", lon = " +lonMin+"-"+lonMax);
        float[] faGridX     = null;
        float[] faGridY     = null;
        float   scaleLength = 1.0f;
        float   gain        = 1.0f;
        if ((xSpacing == 0) || (ySpacing == 0)) {
            Barnes.AnalysisParameters ap =
                Barnes.getRecommendedParameters(lonMin, latMin, lonMax,
                    latMax, domainPts);
            faGridX     = ap.getGridXArray();
            faGridY     = ap.getGridYArray();
            scaleLength = (float) ap.getScaleLengthGU();
        } else {
            faGridX = Barnes.getRecommendedGridX(lonMin, lonMax, xSpacing);
            faGridY = Barnes.getRecommendedGridY(latMin, latMax, ySpacing);
        }
        double[][] griddedData = Barnes.point2grid(faGridX, faGridY,
                                     new float[][] {
            domainPts[0], domainPts[1], paramVals[0]
        }, scaleLength, gain, numPasses);

        float[][] faaDomainSet =
            new float[2][faGridX.length * faGridY.length];
        float[][] faaGridValues3 =
            new float[1][faGridX.length * faGridY.length];

        int m = 0;
        for (int j = 0; j < faGridY.length; j++) {
            for (int i = 0; i < faGridX.length; i++) {
                faaDomainSet[0][m]   = faGridY[j];
                faaDomainSet[1][m]   = faGridX[i];
                faaGridValues3[0][m] = (float) griddedData[i][j];
                m++;
            }
        }

        Gridded2DSet g2ddsSet =
            new Gridded2DSet(RealTupleType.LatitudeLongitudeTuple,
                             faaDomainSet, faGridX.length, faGridY.length,
                             (CoordinateSystem) null, (Unit[]) null,
                             (ErrorEstimate[]) null, false);
        FunctionType ftLatLon2Param =
            new FunctionType(RealTupleType.LatitudeLongitudeTuple,
                             new RealTupleType(type));
        FlatField retData = new FlatField(ftLatLon2Param, g2ddsSet);
        retData.setSamples(faaGridValues3, false);
        return retData;
    }

    /**
     * main
     *
     * @param args args
     *
     * @throws Exception on badness
     */
    public static void main(String[] args) throws Exception {
        Real           lat  = new Real(RealType.Latitude, 40.0);
        Real           lon  = new Real(RealType.Longitude, -100.0);
        Real           alt  = new Real(RealType.Altitude, 0);
        java.util.Date dttm = new java.util.Date(1000);
        for (int j = 0; j < 10; j++) {
            List l  = new ArrayList(100000);
            long t1 = System.currentTimeMillis();
            //            DateTime[]da= new DateTime[100000];
            GregorianCalendar utcCalendar;
            Hashtable         ht = new Hashtable();
            for (int i = 0; i < 100000; i++) {
                new DateTime(dttm);
            }
            long t2 = System.currentTimeMillis();
            System.err.println("time:" + (t2 - t1));
        }
    }



}

