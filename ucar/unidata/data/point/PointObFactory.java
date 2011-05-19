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

package ucar.unidata.data.point;


import au.gov.bom.aifs.osa.analysis.Barnes;

import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;

import ucar.nc2.Attribute;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.PointObsDatatype;
import ucar.nc2.dt.StationObsDataset;
import ucar.nc2.dt.StationObsDatatype;
import ucar.nc2.ft.FeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.writer.CFPointObWriter;
import ucar.nc2.ft.point.writer.PointObVar;
import ucar.nc2.units.DateRange;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.DateSelection;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.GeoUtils;
import ucar.visad.Util;
import ucar.visad.quantities.GeopotentialAltitude;

import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.Data;
import visad.DateTime;
import visad.DoubleStringTuple;
import visad.DoubleTuple;
import visad.ErrorEstimate;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.GriddedSet;
import visad.Integer1DSet;
import visad.Linear1DSet;
import visad.LinearLatLonSet;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Scalar;
import visad.ScalarType;
import visad.Set;
import visad.SetType;
import visad.SingletonSet;
import visad.Text;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.Unit;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.LatLonPoint;

import visad.util.DataUtility;


import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;


/**
 * Factory for dealing with point observations
 *
 * @author IDV Development Team
 */
public class PointObFactory {

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(PointObFactory.class.getName());

    /** OA Grid Default value */
    public static final float OA_GRID_DEFAULT = 0;

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
     * Make a point cloud structure from point obs
     *
     * @param pointObs the point obs
     * @param param  the parameter to extract - null if want all
     *
     * @return  a PointCloud (time->((index)->((Latitude, Longitude, Altitude),param)))
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   VisAD problem
     */
    public static FieldImpl makePointCloud(FieldImpl pointObs, String param)
            throws VisADException, RemoteException {
        /* TODO: subset parameter if we want - use extractParameter
        int paramIndex = -1;
        if (param != null) {
            PointObTuple ob = (PointObTuple) pointObs.getSample(0, false);
            TupleType    obType =
                (TupleType) ((Tuple) ob.getData()).getType();
            RealType     paramType = RealType.getRealType(param);
            int          index     = obType.getIndex(paramType);
            if (index != 0) {
                paramIndex = index;
            }
        }
        */
        Trace.call1("PointObFactory: makingTimeSequence");
        FieldImpl timeObs = makeTimeSequenceOfPointObs(pointObs);
        Trace.call2("PointObFactory: makingTimeSequence");
        FieldImpl    cloudData     = null;
        Set          timeSet       = timeObs.getDomainSet();
        FunctionType cloudType     = null;
        TupleType    cloudDataType = null;
        FunctionType timeCloudType = null;
        Trace.call1("PointObFactory: makingCloudFI");
        float[][] timeStepVals  = null;
        Unit[]    dataUnits     = null;
        Unit[]    rangeUnits    = null;
        boolean   needToConvert = false;
        for (int i = 0; i < timeSet.getLength(); i++) {
            FieldImpl    obs      = (FieldImpl) timeObs.getSample(i, false);
            Integer1DSet indexSet = (Integer1DSet) obs.getDomainSet();
            FlatField    timeStep = null;
            //Trace.call1("PointObFactory: makingCloudFF", "numObs for time " + i + " is " + indexSet.getLength());
            for (int j = 0; j < indexSet.getLength(); j++) {
                PointOb ob = (PointOb) obs.getSample(j, false);
                if (cloudType == null) {
                    cloudDataType = new TupleType(new MathType[] {
                        ob.getEarthLocation().getType(),
                        ob.getData().getType() });
                    cloudType =
                        new FunctionType(DataUtility.getDomainType(indexSet),
                                         cloudDataType);
                }
                double[] elVals =
                    ((RealTuple) ob.getEarthLocation()).getValues();
                double[] dataVals = ((RealTuple) ob.getData()).getValues();
                if (timeStep == null) {  // first time through
                    timeStep = new FlatField(cloudType, indexSet);
                    timeStepVals =
                        new float[elVals.length + dataVals.length][timeStep.getLength()];
                    Unit[] elUnits =
                        ((RealTuple) ob.getEarthLocation()).getTupleUnits();
                    if (elUnits == null) {
                        elUnits = new Unit[] { CommonUnit.degree,
                                CommonUnit.degree, CommonUnit.meter };
                    }
                    Unit[] valUnits =
                        ((RealTuple) ob.getData()).getTupleUnits();
                    dataUnits = new Unit[elUnits.length + valUnits.length];
                    for (int k = 0; k < elUnits.length; k++) {
                        dataUnits[k] = elUnits[k];
                    }
                    for (int k = 0; k < valUnits.length; k++) {
                        dataUnits[k + elUnits.length] = valUnits[k];
                    }
                    rangeUnits = Util.getDefaultRangeUnits(timeStep);
                    needToConvert = !java.util.Arrays.equals(dataUnits,
                            rangeUnits);
                }
                for (int k = 0; k < elVals.length; k++) {
                    timeStepVals[k][j] = (float) elVals[k];
                }
                for (int k = 0; k < dataVals.length; k++) {
                    timeStepVals[k + elVals.length][j] = (float) dataVals[k];
                }


                /*  Setting the samples on a FlatField using Data objects is really expensive
                timeStep.setSample(j, new Tuple(cloudDataType, new Data[] {
                    ob.getEarthLocation(),
                    ob.getData() }, false, false), false);
                    */
            }
            // TODO:  this assumes that the values are the same units as the default units.
            if (needToConvert) {
                timeStepVals = Unit.convertTuple(timeStepVals, dataUnits,
                        rangeUnits);
            }
            timeStep.setSamples(timeStepVals, false);
            if (timeCloudType == null) {
                timeCloudType =
                    new FunctionType(DataUtility.getDomainType(timeSet),
                                     timeStep.getType());
                cloudData = new FieldImpl(timeCloudType, timeSet);
            }
            //Trace.call2("PointObFactory: makingCloudFF");
            cloudData.setSample(i, timeStep, false, false);
        }
        Trace.call2("PointObFactory: makingCloudFI");
        return cloudData;
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
        List obs    = new ArrayList(numObs);
        Trace.call1("makeTimeSequence: get list of obs");
        for (int i = 0; i < numObs; i++) {
            obs.add(pointObs.getSample(i, false));
        }
        Trace.call2("makeTimeSequence: get list of obs");
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
                    " " + lumpMinutes + " component " + componentIndex
                    + ", num obs:" + numObs);
        Hashtable seenTime = new Hashtable();
        for (int i = 0; i < numObs; i++) {
            PointObTuple ob = (PointObTuple) pointObs.get(i);
            if (i == 0) {
                if (componentIndex < 0) {
                    obType = ob.getType();
                } else {
                    obType = ((Tuple) ob.getData()).getComponent(
                        componentIndex).getType();
                }
            }
            DateTime dttm = ob.getDateTime();

            if (dttm.isMissing()) {
                continue;
            }

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
        SampledSet timeSet = (times.length > 1)
                             ? DateTime.makeTimeSet(times)
                             : (SampledSet) new SingletonSet(
                                 new RealTuple(new Real[] { times[0] }));


        FieldImpl timeSequence = new FieldImpl(timeSequenceType, timeSet);

        List      samples      = new ArrayList();
        Trace.call1("makeTimeSequence-loop2");
        Data[] timeSamples = new Data[times.length];
        for (int i = 0; i < times.length; i++) {
            DateTime dttm   = times[i];

            Double   dValue = new Double(dttm.getValue());
            List     v      = (List) timeToObs.get(dValue);
            Data[]   obs    = null;
            if (componentIndex < 0) {
                //obs = (Data[]) v.toArray(new PointOb[v.size()]);
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
                //sample.setSamples(obs, false, false);
                for (int j = 0; j < v.size(); j++) {
                    sample.setSample(j, (Data) v.get(j), false, false);
                }
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
    public static FieldImpl subSet(FieldImpl pointObs, LatLonRect bounds)
            throws VisADException, RemoteException {
        return subSet(pointObs, GeoUtils.latLonRectToSet(bounds));
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
     * Get the list of PointOb objects from the given field
     *
     * @param field the field that contains the PointObs
     *
     * @return List of PointObs
     *
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public static List<PointOb> getPointObs(FieldImpl field)
            throws VisADException, RemoteException {
        List<PointOb> obs            = new ArrayList<PointOb>();
        boolean       isTimeSequence = GridUtil.isTimeSequence(field);
        if (isTimeSequence) {
            Set timeSet  = field.getDomainSet();
            int numTimes = timeSet.getLength();
            for (int timeIdx = 0; timeIdx < numTimes; timeIdx++) {
                FieldImpl oneTime = (FieldImpl) field.getSample(timeIdx);
                int       numObs  = oneTime.getDomainSet().getLength();
                for (int obIdx = 0; obIdx < numObs; obIdx++) {
                    obs.add((PointOb) oneTime.getSample(obIdx));
                }
            }
        } else {
            FieldImpl oneTime = field;
            int       numObs  = oneTime.getDomainSet().getLength();
            for (int obIdx = 0; obIdx < numObs; obIdx++) {
                obs.add((PointOb) oneTime.getSample(obIdx));
            }

        }
        return obs;
    }


    /**
     * Write the PointObs contained in the given field as a netcdf file
     *
     * @param file file to write to
     * @param field The field
     *
     * @throws IOException on badness
     * @throws RemoteException on badness
     * @throws VisADException on badness
     */
    public static void writeToNetcdf(File file, FieldImpl field)
            throws VisADException, RemoteException, IOException {

        List<PointOb> obs = getPointObs(field);
        if (obs.size() == 0) {
            throw new IllegalArgumentException(
                "No point observations to write");
        }
        List<Attribute>  attrs    = new ArrayList<Attribute>();
        List<PointObVar> dataVars = new ArrayList<PointObVar>();
        DataOutputStream dos =
            new DataOutputStream(new FileOutputStream(file));
        CFPointObWriter writer    = null;
        int             numFloat  = 0;
        int             numString = 0;
        int[]           lengths   = null;
        boolean[]       isText    = null;
        for (PointOb ob : obs) {
            EarthLocation el        = ob.getEarthLocation();
            Real          alt       = el.getAltitude();
            LatLonPoint   llp       = el.getLatLonPoint();
            Tuple         tuple     = (Tuple) ob.getData();
            TupleType     type      = (TupleType) tuple.getType();
            MathType[]    types     = type.getComponents();
            int           numFields = types.length;
            if (writer == null) {
                lengths = new int[numFields];
                isText  = new boolean[numFields];
                boolean haveText = false;
                for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
                    lengths[fieldIdx] = 0;
                    if (types[fieldIdx] instanceof TextType) {
                        haveText         = true;
                        isText[fieldIdx] = true;
                        continue;
                    }
                    isText[fieldIdx] = false;
                    PointObVar pointObVar = new PointObVar();
                    pointObVar.setName(Util.cleanTypeName(types[fieldIdx]));


                    Unit unit = ((RealType) types[fieldIdx]).getDefaultUnit();
                    if (unit != null) {
                        String unitName = unit.getIdentifier();
                        //                        System.err.println("unitName:" + unitName + " unit:" + unit);
                        if ((unitName == null) || (unitName.length() == 0)) {
                            unitName = unit.toString();
                        }
                        pointObVar.setUnits(unitName);
                        //                        System.err.println("Var:" + pointObVar.getName()
                        //                                           + " unit: "
                        //                                           + pointObVar.getUnits());
                    }

                    pointObVar.setDataType(DataType.DOUBLE);
                    dataVars.add(pointObVar);
                    numFloat++;
                }

                if (haveText) {
                    for (PointOb ob2 : obs) {
                        Tuple  tuple2 = (Tuple) ob2.getData();
                        Data[] data   = tuple2.getComponents();
                        for (int fieldIdx = 0; fieldIdx < lengths.length;
                                fieldIdx++) {
                            if ( !isText[fieldIdx]) {
                                continue;
                            }
                            String s   = ((Text) data[fieldIdx]).getValue();
                            int    len = s.length();
                            if (len > lengths[fieldIdx]) {
                                lengths[fieldIdx] = len;
                            }
                        }
                    }
                }



                for (int fieldIdx = 0; fieldIdx < lengths.length;
                        fieldIdx++) {
                    if ( !isText[fieldIdx]) {
                        continue;
                    }
                    PointObVar pointObVar = new PointObVar();
                    lengths[fieldIdx] = Math.max(lengths[fieldIdx], 2);
                    //                    System.err.println("idx:" + fieldIdx + "   name:"
                    //                                       + Util.cleanTypeName(types[fieldIdx])
                    //                                       + " length:" + lengths[fieldIdx]);
                    pointObVar.setName(Util.cleanTypeName(types[fieldIdx]));
                    pointObVar.setDataType(DataType.STRING);
                    pointObVar.setLen(lengths[fieldIdx]);
                    dataVars.add(pointObVar);
                    numString++;
                }
                writer = new CFPointObWriter(dos, attrs, ((alt != null)
                        ? alt.getUnit().toString()
                        : null), dataVars, obs.size());
            }

            double[] dvals = new double[numFloat];
            String[] svals = new String[numString];
            int      dcnt  = 0;
            int      scnt  = 0;
            Data[]   data  = tuple.getComponents();
            for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
                if (isText[fieldIdx]) {
                    String s = ((Text) data[fieldIdx]).getValue();
                    s             = StringUtil.padLeft(s, lengths[fieldIdx]);
                    svals[scnt++] = s;
                    //                    System.err.println(fieldIdx + ":" + svals[scnt - 1]);
                } else {
                    dvals[dcnt++] = ((Real) data[fieldIdx]).getValue();
                }
            }


            try {
                writer.addPoint(
                    llp.getLatitude().getValue(CommonUnit.degree),
                    llp.getLongitude().getValue(CommonUnit.degree),
                    ((alt != null)
                     ? alt.getValue(CommonUnit.meter)
                     : 0.0), ucar.visad.Util.makeDate(ob.getDateTime()),
                             dvals, svals);
            } catch (Exception exc) {
                int xxx = 1;
                for (PointObVar pov : dataVars) {
                    System.out.println("var #" + xxx + " " + pov.getName()
                                       + " " + pov.getDataType() + " unit:"
                                       + pov.getUnits() + " length:"
                                       + pov.getLen());
                    xxx++;
                }
                System.out.println("#dvals:" + dvals.length + " #svals:"
                                   + svals.length);

                for (String s : svals) {
                    System.err.println("sval=" + s + ":");
                }
                throw new RuntimeException(exc);
            }
        }


        writer.finish();
        dos.close();


    }


    /**
     * Make a CFPointObWriter
     *
     * @param dos  the output stream
     * @param type the tupe
     * @param skipIndices which indices to skip
     * @param defaultStringLength  the default string length
     * @param altUnit  the altitude unit
     * @param cnt      the number
     * @param slengths string lengths
     *
     * @return  the writer
     *
     * @throws Exception  problem creating something
     */
    public static CFPointObWriter makeWriter(DataOutputStream dos,
                                             TupleType type,
                                             int[] skipIndices,
                                             int defaultStringLength,
                                             String altUnit, int cnt,
                                             int[] slengths)
            throws Exception {
        MathType[]       types     = type.getComponents();
        int              numFields = types.length;
        int              numFloat  = 0;
        int              numString = 0;
        int[]            lengths   = new int[numFields];
        boolean[]        isText    = new boolean[numFields];
        boolean          haveText  = false;
        List<Attribute>  attrs     = new ArrayList<Attribute>();
        List<PointObVar> dataVars  = new ArrayList<PointObVar>();
        HashSet          skip      = new HashSet();
        for (int i = 0; i < skipIndices.length; i++) {
            skip.add(new Integer(skipIndices[i]));
        }


        for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
            if (skip.contains(new Integer(fieldIdx))) {
                continue;
            }

            if (types[fieldIdx] instanceof TextType) {
                lengths[fieldIdx] = ((slengths == null)
                                     ? defaultStringLength
                                     : slengths[fieldIdx]);
                haveText          = true;
                isText[fieldIdx]  = true;
                continue;
            }
            lengths[fieldIdx] = 0;
            isText[fieldIdx]  = false;
            PointObVar pointObVar = new PointObVar();
            pointObVar.setName(Util.cleanTypeName(types[fieldIdx]));
            Unit unit = ((RealType) types[fieldIdx]).getDefaultUnit();
            if (unit != null) {
                String unitName = unit.getIdentifier();
                if ((unitName == null) || (unitName.length() == 0)) {
                    unitName = unit.toString();
                }
                pointObVar.setUnits(unitName);
            }

            pointObVar.setDataType(DataType.DOUBLE);
            dataVars.add(pointObVar);
            numFloat++;
        }


        for (int fieldIdx = 0; fieldIdx < lengths.length; fieldIdx++) {
            if (skip.contains(new Integer(fieldIdx))) {
                continue;
            }
            if ( !isText[fieldIdx]) {
                continue;
            }
            PointObVar pointObVar = new PointObVar();
            lengths[fieldIdx] = Math.max(lengths[fieldIdx], 2);
            pointObVar.setName(Util.cleanTypeName(types[fieldIdx]));
            pointObVar.setDataType(DataType.STRING);
            pointObVar.setLen(lengths[fieldIdx]);
            dataVars.add(pointObVar);
            numString++;
        }
        //        return  new CFPointObWriter(dos, attrs, altUnit, dataVars, 2000000);
        return new CFPointObWriter(dos, attrs, altUnit, dataVars, cnt);
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

            // Check for METAR cloud cover groups
            int cc1Index = type.getIndex("CC1");
            int cc2Index = type.getIndex("CC2");
            int cigIndex = type.getIndex("CIGC");
            int caIndex  = type.getIndex("CA");
            boolean mergeClouds = ((cc1Index >= 0) && (cc2Index >= 0)
                                   && (cigIndex >= 0) && (caIndex < 0));

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
            int            cnt       = 0;
            List<RealType> realTypes = new ArrayList<RealType>();
            List<TextType> textTypes = new ArrayList<TextType>();
            for (int j = 0; j < numNotRequired; j++) {
                ScalarType stype =
                    (ScalarType) type.getComponent(notReqIndices[j]);
                if (stype instanceof TextType) {
                    textTypes.add((TextType) stype);
                } else {
                    realTypes.add((RealType) stype);
                }
            }
            RealType caType = null;
            if (mergeClouds) {
                caType = DataUtil.makeRealType("CA", DataUtil.parseUnit(""));
                realTypes.add(caType);
            }
            if (allReals) {
                tupleType = new RealTupleType(
                    (RealType[]) realTypes.toArray(
                        new RealType[realTypes.size()]));
            } else {
                tupleType = DoubleStringTuple.makeTupleType(realTypes,
                        textTypes);
            }
            int    numDouble = realTypes.size();
            int    numString = textTypes.size();
            Real[] protos    = null;
            Unit[] realUnits = null;
            Real   caProto   = null;
            double caValue   = 0;
            if (mergeClouds) {
                caProto = new Real(caType, 0);
            }

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

                Tuple rest = null;
                // McIDAS Metar Cloud covers are:
                // 0 - clear       = WMO 0
                // 1 - scattered   = WMO 2
                // 2 - broken      = WMO 6
                // 3 - overcast    = WMO 8
                // 5 - few         = WMO 1
                int wmo = 0;
                if (mergeClouds) {
                    double cc1 =
                        ((Real) ob.getComponent(cc1Index)).getValue();
                    double cc2 =
                        ((Real) ob.getComponent(cc2Index)).getValue();
                    double cig =
                        ((Real) ob.getComponent(cigIndex)).getValue();
                    if (Double.isNaN(cc1)) {
                        cc1 = 0;
                    }
                    if (Double.isNaN(cc2)) {
                        cc2 = 0;
                    }
                    if (Double.isNaN(cig)) {
                        cig = 0;
                    }
                    boolean haveFew = ((cc1 == 5) || (cc2 == 5));
                    int     largest = 0;
                    if ( !haveFew) {
                        largest = (int) Math.max(Math.max(cc1, cc2), cig);
                    } else {
                        double val1 = (cc1 == 5)
                                      ? 0
                                      : cc1;
                        double val2 = (cc2 == 5)
                                      ? 0
                                      : cc2;
                        largest = (int) Math.max(Math.max(val1, val2), cig);
                    }
                    if (largest < 2) {          // CLR or SCT
                        wmo = 2 * largest;
                    } else if (largest == 2) {  // BKN
                        wmo = 6;
                    } else {                    // OVC
                        wmo = 8;
                    }
                    if ((wmo == 0) && haveFew) {
                        wmo++;                  // FEW
                    }
                }

                // now make data
                if (allReals) {
                    double[] obValues   = ((RealTuple) ob).getValues();
                    double[] realValues = new double[numDouble];
                    for (int j = 0; j < numNotRequired; j++) {
                        realValues[j] = obValues[notReqIndices[j]];
                    }
                    if (mergeClouds) {
                        realValues[numDouble - 1] = wmo;
                    }

                    if (protos == null) {
                        protos    = new Real[numDouble];
                        realUnits = new Unit[numDouble];
                        for (int j = 0; j < numNotRequired; j++) {
                            protos[j] =
                                (Real) ob.getComponent(notReqIndices[j]);
                            realUnits[j] = protos[j].getUnit();
                        }
                        if (mergeClouds) {
                            protos[numDouble - 1]    = caProto;
                            realUnits[numDouble - 1] = caProto.getUnit();
                        }
                    }

                    rest = new DoubleTuple((RealTupleType) tupleType, protos,
                                           realValues, realUnits);
                } else {
                    String[] strings = (numString > 0)
                                       ? new String[numString]
                                       : null;
                    double[] values  = (numDouble > 0)
                                       ? new double[numDouble]
                                       : null;
                    if (i == 0) {
                        protos    = new Real[numDouble];
                        realUnits = new Unit[numDouble];
                        if (mergeClouds) {
                            protos[numDouble - 1]    = caProto;
                            realUnits[numDouble - 1] = caProto.getUnit();
                        }
                    }
                    int stringIdx = 0;
                    int valIdx    = 0;
                    for (int j = 0; j < numNotRequired; j++) {
                        Scalar scalar =
                            (Scalar) ob.getComponent(notReqIndices[j]);
                        if (scalar instanceof Text) {
                            strings[stringIdx++] = ((Text) scalar).getValue();
                        } else {
                            Real real = (Real) scalar;
                            values[valIdx] = real.getValue();
                            if (i == 0) {
                                realUnits[valIdx] = real.getUnit();
                                protos[valIdx]    = real;
                            }
                            valIdx++;
                        }

                    }
                    if (mergeClouds) {
                        values[numDouble - 1] = wmo;
                    }
                    rest = new DoubleStringTuple(tupleType, protos, values,
                            strings, realUnits);
                }

                if (finalTupleType == null) {
                    PointObTuple pot = new PointObTuple(location, dateTime,
                                           rest);
                    obs[i]         = pot;
                    finalTupleType =
                        Tuple.buildTupleType(pot.getComponents());

                    Data[] comps = rest.getComponents();
                    for (int compIdx = 0; compIdx < comps.length; compIdx++) {
                        String name = ucar.visad.Util.cleanTypeName(
                                          comps[compIdx].getType());
                        DataChoice.addCurrentName(
                            new TwoFacedObject(
                                "Point Data" + ">" + name, name));
                    }

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

        return makePointObs(input, binRoundTo, binWidth, llr, false);
    }


    /**
     * Make point obs
     *
     * @param input the data set
     * @param binRoundTo bin round to
     * @param binWidth time bin size
     * @param llr bounding box
     * @param sample If true then just sample the data, i.e., read the first ob
     *
     * @return The field
     *
     * @throws Exception On badness
     */
    public static FieldImpl makePointObs(PointObsDataset input,
                                         double binRoundTo, double binWidth,
                                         LatLonRect llr, boolean sample)
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
            types[0] = DataUtil.makeTextType(stationFieldName);
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

            DataChoice.addCurrentName(new TwoFacedObject("Point Data" + ">"
                    + var.getShortName(), var.getShortName()));

            // now make types
            if (isVarNumeric[varIdx]) {  // RealType
                Unit unit = DataUtil.parseUnit(var.getUnitsString());
                types[varIdx] = DataUtil.makeRealType(var.getShortName(),
                        unit);
                varUnits[varIdx] = unit;
                numericTypes.add(types[varIdx]);
                numericUnits.add(unit);
            } else {
                types[varIdx]    = DataUtil.makeTextType(var.getShortName());
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
            ucar.unidata.geoloc.EarthLocation el = po.getLocation();
            if (el == null) {
                continue;
            }
            if ((llr != null)
                    && !llr.contains(el.getLatitude(), el.getLongitude())) {
                continue;
            }
            pos.add(po);
            times.add(new DateTime(po.getNominalTimeAsDate()));
            if (sample) {
                break;
            }
        }
        //        Trace.call2("loop-2");


        //Bin times
        times = binTimes(times, binRoundTo, binWidth);

        StructureMembers.Member member;
        PointOb[]               obs = new PointOb[pos.size()];
        //Make the obs
        //        Trace.call1("loop-3");
        int    size      = pos.size();

        Data[] prototype = null;
        for (int i = 0; i < size; i++) {
            PointObsDatatype po = (PointObsDatatype) pos.get(i);
            ucar.unidata.geoloc.EarthLocation el = po.getLocation();
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
                               (RealTupleType) allTupleType, prototype,
                               realArray, allUnits)
                           : new DoubleStringTuple(allTupleType, prototype,
                               realArray, stringArray, allUnits));

            if (prototype == null) {
                prototype = tuple.getComponents();
            }

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
     * Make point obs
     *
     * @param input the data set
     * @param binRoundTo bin round to
     * @param binWidth time bin size
     * @param llr bounding box
     * @param dateSelection  the date selection
     * @param sample If true then just sample the data, i.e., read the first ob
     *
     * @return The field
     *
     * @throws Exception On badness
     */
    public static FieldImpl makePointObs(FeatureDatasetPoint input,
                                         double binRoundTo, double binWidth,
                                         LatLonRect llr,
                                         DateSelection dateSelection,
                                         boolean sample)
            throws Exception {



        Object  loadId = JobManager.getManager().startLoad("PointObFactory");

        List    actualVariables    = input.getDataVariables();
        int     numVars            = actualVariables.size();
        String  _isMissing         = "_isMissing";

        boolean needToAddStationId = false;
        String  stationFieldName   = null;

        // make sure we can read this kind of data
        List<FeatureCollection> collectionList =
            input.getPointFeatureCollectionList();
        if (collectionList.size() > 1) {
            throw new IllegalArgumentException(
                "Can't handle point data with multiple collections");
        }
        FeatureCollection      fc         = collectionList.get(0);
        PointFeatureCollection collection = null;
        // System.out.println("llr = " + llr);
        DateRange dateRange = null;
        if (dateSelection != null) {
            if (dateSelection.getTimes() != null) {
                List<Date> range = dateSelection.getTimes();
                Collections.sort(range);
                dateRange = new DateRange(range.get(0),
                                          range.get(range.size() - 1));
            } else if (dateSelection.hasInterval()) {
                double interval = dateSelection.getInterval();
                int    count    = dateSelection.getCount();
                long   timespan = (long) interval * count;
                Date   now      = new Date();
                dateRange = new DateRange(new Date(now.getTime() - timespan),
                                          now);
            }
        }
        //if (dateRange == null) {
        //    dateRange = new DateRange(null, new DateType(true, null), new TimeDuration("1 hour"), null);
        //}
        if (fc instanceof PointFeatureCollection) {
            collection = (PointFeatureCollection) fc;
            if ((llr != null) || (dateRange != null)) {
                collection = collection.subset(llr, dateRange);
            }
        } else if (fc instanceof NestedPointFeatureCollection) {
            NestedPointFeatureCollection npfc =
                (NestedPointFeatureCollection) fc;
            //if (llr != null) {
            //    npfc = npfc.subset(llr);
            //}
            collection = npfc.flatten(llr, dateRange);
        } else {
            throw new IllegalArgumentException(
                "Can't handle collection of type " + fc.getClass().getName());
        }
        //System.out.println("collection = " + collection.getClass().getName());
        //Trace.call1("FeatureDatasetPoint: calculating bounds");
        //collection.calcBounds();
        //Trace.call2("FeatureDatasetPoint: calculating bounds");
        //System.out.println("bounds = " + collection.getBoundingBox());
        //System.out.println("date range = " + collection.getDateRange());

        //Is this station data
        if (input instanceof StationObsDataset) {
            //TODO: Get the variable name used for the station id:
        }


        int varIdxBase = 0;
        if (needToAddStationId) {
            numVars++;
            varIdxBase = 1;
        }

        List<String> shortNamesList = new ArrayList<String>();

        log_.debug("number of data variables = " + numVars);
        boolean[]        isVarNumeric = new boolean[numVars];
        boolean          allReals     = true;
        ScalarType[]     types        = new ScalarType[numVars];
        Unit[]           varUnits     = new Unit[numVars];

        List<ScalarType> numericTypes = new ArrayList<ScalarType>();
        List<Unit>       numericUnits = new ArrayList<Unit>();
        List<ScalarType> stringTypes  = new ArrayList<ScalarType>();

        //If we really have a StationObsDataset then we need to add in the station id
        //into the data fields 
        if (needToAddStationId) {
            isVarNumeric[0] = false;
            if (stationFieldName == null) {
                stationFieldName = PointOb.PARAM_ID;
            }
            shortNamesList.add(stationFieldName);
            types[0] = DataUtil.makeTextType(stationFieldName);
            stringTypes.add(types[0]);
            allReals = false;
        }


        Trace.call1("FeatureDatasetPoint: getting variable info");
        int varIdx = varIdxBase;
        for (Iterator iter = actualVariables.iterator(); iter.hasNext(); ) {
            VariableSimpleIF var       = (VariableSimpleIF) iter.next();
            String           shortName = var.getShortName();
            if (shortName.equals(_isMissing)) {
                numVars--;
                continue;
            }
            // make sure data is either numeric or string
            if ( !((var.getDataType() == DataType.BYTE)
                    || (var.getDataType() == DataType.SHORT)
                    || (var.getDataType() == DataType.INT)
                    || (var.getDataType() == DataType.LONG)
                    || (var.getDataType() == DataType.FLOAT)
                    || (var.getDataType() == DataType.DOUBLE)
                    || (var.getDataType() == DataType.STRING)
                    || (var.getDataType() == DataType.CHAR))) {
                numVars--;
                continue;
            }
            shortNamesList.add(shortName);

            isVarNumeric[varIdx] = !((var.getDataType() == DataType.STRING)
                                     || (var.getDataType() == DataType.CHAR));
            if ( !isVarNumeric[varIdx]) {
                allReals = false;
            }

            DataChoice.addCurrentName(new TwoFacedObject("Point Data" + ">"
                    + var.getShortName(), var.getShortName()));

            // System.err.println("param "  + var.getShortName());

            // now make types
            if (isVarNumeric[varIdx]) {  // RealType
                Unit unit = DataUtil.parseUnit(var.getUnitsString());
                types[varIdx] = DataUtil.makeRealType(var.getShortName(),
                        unit);
                varUnits[varIdx] = unit;
                numericTypes.add(types[varIdx]);
                numericUnits.add(unit);
            } else {
                types[varIdx] =
                    DataUtil.makeTextType(Util.cleanName(var.getShortName()));
                varUnits[varIdx] = null;
                stringTypes.add(types[varIdx]);
            }
            varIdx++;
        }
        Trace.call2("FeatureDatasetPoint: getting variable info");


        String[] shortNames = (String[]) shortNamesList.toArray(
                                  new String[shortNamesList.size()]);


        int       numReals     = numericTypes.size();
        int       numStrings   = stringTypes.size();
        int       obIdx        = 0;
        int       NUM          = 500;
        TupleType allTupleType = (allReals
                                  ? new RealTupleType(
                                      (RealType[]) numericTypes.toArray(
                                          new RealType[numericTypes.size()]))
                                  : DoubleStringTuple.makeTupleType(
                                      numericTypes, stringTypes));
        Unit[] allUnits =
            (Unit[]) numericUnits.toArray(new Unit[numericUnits.size()]);


        int               listSize = (sample)
                                     ? 1
                                     : 100000;
        Real              lat      = new Real(RealType.Latitude, 40);
        Real              lon      = new Real(RealType.Longitude, -100);
        Real              alt      = new Real(RealType.Altitude, 0);
        DateTime          dateTime = null;
        EarthLocationLite elt;
        TupleType         finalTT = null;
        PointObTuple      pot     = null;
        List<Tuple>       tuples  = new ArrayList<Tuple>(listSize);
        List<DateTime>    times   = new ArrayList<DateTime>(listSize);
        List<EarthLocationLite> elts =
            new ArrayList<EarthLocationLite>(listSize);

        StructureMembers.Member member;
        Trace.call1("FeatureDatasetPoint: iterating on PointFeatures",
                    "sample = " + sample);
        int     missing    = 0;
        int     ismissing  = 0;
        boolean iammissing = false;
        String  svalue     = "missing";
        float   value      = Float.NaN;
        // if we are only getting a sample there's no need to use the iterator
        if (sample) {
            obIdx++;
            elt = new EarthLocationLite(lat, lon, alt);
            double[] realArray   = new double[numReals];
            String[] stringArray = ((numStrings == 0)
                                    ? null
                                    : new String[numStrings]);

            // make the VisAD data object
            int stringCnt = 0;
            int realCnt   = 0;
            for (varIdx = varIdxBase; varIdx < numVars; varIdx++) {
                if ( !isVarNumeric[varIdx]) {
                    stringArray[stringCnt++] = svalue;
                } else {
                    realArray[realCnt++] = value;
                }
            }

            Tuple tuple = (allReals
                           ? (Tuple) new DoubleTuple(
                               (RealTupleType) allTupleType, realArray,
                               allUnits)
                           : new DoubleStringTuple(allTupleType, realArray,
                               stringArray, allUnits));

            tuples.add(tuple);
            times.add(new DateTime(0));
            elts.add(elt);
        } else {
            PointFeatureIterator dataIterator =
            //collection.getPointFeatureIterator(-1);
            collection.getPointFeatureIterator(16384);
            while (dataIterator.hasNext()) {
                PointFeature po = (PointFeature) dataIterator.next();
                iammissing = false;
                obIdx++;
                ucar.unidata.geoloc.EarthLocation el = po.getLocation();
                elt = new EarthLocationLite(
                    lat.cloneButValue(el.getLatitude()),
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
                boolean allMissing = true;

                // check for a missing flag
                member = structure.findMember(_isMissing);
                if (member != null) {
                    value = structure.convertScalarInt(member);
                    if (value == 1) {
                        iammissing = true;
                        ismissing++;
                        missing++;
                        continue;
                    }
                }

                for (varIdx = varIdxBase; varIdx < numVars; varIdx++) {
                    member =
                        structure.findMember((String) shortNames[varIdx]);
                    if (member == null) {
                        continue;
                    }
                    if ( !isVarNumeric[varIdx]) {
                        svalue = structure.getScalarString(member);
                        if (svalue.length() != 0) {
                            allMissing = false;
                        }
                        stringArray[stringCnt++] = svalue;
                    } else {
                        value = structure.convertScalarFloat(member);
                        if (value == value) {
                            allMissing = false;
                        }
                        realArray[realCnt++] = value;
                    }
                }
                /*
                if (allMissing  && !iammissing) {
                    System.out.println("has all missing, but not iammissing: " + el);
                }
                */
                if (allMissing) {
                    missing++;
                    continue;
                }

                Tuple tuple = (allReals
                               ? (Tuple) new DoubleTuple(
                                   (RealTupleType) allTupleType, realArray,
                                   allUnits)
                               : new DoubleStringTuple(allTupleType,
                                   realArray, stringArray, allUnits));

                tuples.add(tuple);
                times.add(new DateTime(po.getNominalTimeAsDate()));
                elts.add(elt);

                if (obIdx % NUM == 0) {
                    if ( !JobManager.getManager().canContinue(loadId)) {
                        LogUtil.message("");
                        return null;
                    }
                    LogUtil.message("Read " + obIdx + " observations");
                }
            }
            Trace.call2("FeatureDatasetPoint: iterating on PointFeatures",
                        "found " + ismissing + "/" + missing
                        + " missing out of " + obIdx);
            dataIterator.finish();
        }
        if (tuples.isEmpty()) {
            return null;
        }

        //Bin times
        Trace.call1("FeatureDatasetPoint: binTimes");
        times = binTimes(times, binRoundTo, binWidth);
        Trace.call2("FeatureDatasetPoint: binTimes");


        Trace.call1("FeatureDatasetPoint: making PointObTuples");
        PointOb[] obs  = new PointOb[tuples.size()];
        int       size = tuples.size();
        for (int i = 0; i < size; i++) {

            if (finalTT == null) {
                pot = new PointObTuple(elts.get(i), (DateTime) times.get(i),
                                       tuples.get(i));
                finalTT = Tuple.buildTupleType(pot.getComponents());
            } else {
                pot = new PointObTuple(elts.get(i), (DateTime) times.get(i),
                                       tuples.get(i), finalTT, false);

            }
            obs[i] = pot;
        }
        Trace.call2("FeatureDatasetPoint: making PointObTuples");


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
                    retField.setSample(curPos, data.getSample(j), false,
                                       false);
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
        return barnes(pointObs, type, xSpacing, ySpacing, numPasses, 10f,
                      1.0f, null, null);
    }

    /**
     *  Perform an object analysis on a set of point obs
     *
     * @param pointObs Observations to analyze
     * @param type  RealTypes of parameter
     * @param xSpacing  x spacing (degrees)
     * @param ySpacing  y spacing (degrees)
     * @param numPasses number of passes
     * @param gain      grid convergence/pass
     * @param scaleLength  search radius
     * @param params       analysis parameters - used to pass back computed vals
     * @param firstGuessField The data to use for a first guess. May be null.
     *
     * @return  Grid of objectively analyzed data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FieldImpl barnes(FieldImpl pointObs, RealType type,
                                   float xSpacing, float ySpacing,
                                   int numPasses, float gain,
                                   float scaleLength,
                                   Barnes.AnalysisParameters params,
                                   FieldImpl firstGuessField)
            throws VisADException, RemoteException {
        FieldImpl retFI = null;
        // System.err.println("xspacing: " + xSpacing+" ySpacing:" + ySpacing);
        boolean haveGuess = firstGuessField != null;
        boolean guessIsTime = (firstGuessField != null)
                              && GridUtil.isTimeSequence(firstGuessField);
        if (haveGuess && guessIsTime && GridUtil.isTimeSequence(pointObs)) {
            firstGuessField =
                (FieldImpl) firstGuessField.resample(pointObs.getDomainSet(),
                    Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);
        }
        FlatField guessField = null;

        if (GridUtil.isTimeSequence(pointObs)) {
            Set timeSet    = GridUtil.getTimeSet(pointObs);
            int errorCount = 0;
            for (int i = 0; i < timeSet.getLength(); i++) {
                if (haveGuess) {
                    if (guessIsTime) {
                        guessField = (FlatField) firstGuessField.getSample(i,
                                false);
                    } else {
                        guessField = (FlatField) firstGuessField;
                    }
                    if (guessField.isMissing()) {
                        if (errorCount == 0) {
                            LogUtil.userMessage(
                                log_,
                                "Unable to find matching time for first guess");
                        }
                        guessField = null;
                        errorCount++;
                    }
                }
                FieldImpl oneTime =
                    barnesOneTime((FieldImpl) pointObs.getSample(i), type,
                                  xSpacing, ySpacing, numPasses, gain,
                                  scaleLength, params, guessField);
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
            if (haveGuess) {
                if (guessIsTime) {
                    guessField = (FlatField) firstGuessField.getSample(0,
                            false);
                } else {
                    guessField = (FlatField) firstGuessField;
                }
            }
            retFI = barnesOneTime(pointObs, type, xSpacing, ySpacing,
                                  numPasses, gain, scaleLength, params,
                                  guessField);
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
        return barnesOneTime(pointObs, type, xSpacing, ySpacing, numPasses,
                             10f, 1.0f, null);
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
     * @param gain      grid convergence/pass
     * @param scaleLength  search radius
     * @param params       analysis parameters - used to pass back computed vals
     *
     * @return  Grid of objectively analyzed data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FlatField barnesOneTime(FieldImpl pointObs, RealType type,
                                          float xSpacing, float ySpacing,
                                          int numPasses, float gain,
                                          float scaleLength,
                                          Barnes.AnalysisParameters params)
            throws VisADException, RemoteException {
        return barnesOneTime(pointObs, type, xSpacing, ySpacing, numPasses,
                             gain, scaleLength, params, null);
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
     * @param gain      grid convergence/pass
     * @param scaleLength  search radius
     * @param params       analysis parameters - used to pass back computed vals
     * @param firstGuess   analysis parameters - used to pass back computed vals
     *
     * @return  Grid of objectively analyzed data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException problem getting the data
     */
    public static FlatField barnesOneTime(FieldImpl pointObs, RealType type,
                                          float xSpacing, float ySpacing,
                                          int numPasses, float gain,
                                          float scaleLength,
                                          Barnes.AnalysisParameters params,
                                          FlatField firstGuess)
            throws VisADException, RemoteException {

        int numObs = pointObs.getLength();
        if (numObs < 4) {
            return null;
        }
        float[][] obVals  = new float[3][numObs];
        PointOb   firstOb = (PointOb) pointObs.getSample(0);
        // TODO: May not be a tuple
        Tuple     data      = (Tuple) firstOb.getData();
        TupleType ttype     = (TupleType) data.getType();
        int       typeIndex = ttype.getIndex(type);
        if (typeIndex == -1) {
            return null;
        }
        float latMin      = 90;
        float lonMin      = 180;
        float latMax      = -90;
        float lonMax      = -180;
        int   cnt         = 0;
        int   numMissing  = 0;
        Unit  outputUnits = type.getDefaultUnit();
        for (int i = 0; i < numObs; i++) {
            PointOb po     = (PointOb) pointObs.getSample(i);
            Tuple   obData = (Tuple) po.getData();
            Real    val    = (Real) obData.getComponent(typeIndex);
            //if (i == 0) System.out.println("val["+i+"] ="+ val.toValueString() + ">"+val.getUnit()+"<");
            double obVal = val.getValue(type.getDefaultUnit());
            if (Double.isNaN(obVal)) {
                numMissing++;
                continue;
            }

            EarthLocation el = po.getEarthLocation();
            float lat = (float) el.getLatitude().getValue(CommonUnit.degree);
            // sanity check
            if (Float.isNaN(lat) || (lat < -90) || (lat > 90)) {
                numMissing++;
                continue;
            }
            if (lat < latMin) {
                latMin = lat;
            }
            if (lat > latMax) {
                latMax = lat;
            }
            float lon = (float) el.getLongitude().getValue(CommonUnit.degree);
            // sanity check
            if (Float.isNaN(lon) || (lon < -360) || (lon > 360)) {
                numMissing++;
                continue;
            }
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

            obVals[0][cnt] = lon;
            obVals[1][cnt] = lat;
            obVals[2][cnt] = (float) obVal;
            cnt++;
        }
        // need at least 3 obs for analysis
        if (cnt <= 3) {
            return null;
        }
        //  System.out.println("cnt = " + cnt + " num obs = " + numObs + " missing = " + numMissing);
        if (cnt != numObs) {
            for (int i = 0; i < 3; i++) {
                float[] temp = new float[cnt];
                System.arraycopy(obVals[i], 0, temp, 0, cnt);
                obVals[i] = temp;
            }
        }


        log_.debug("lat range = " + latMin + " " + latMax + ", lon range = "
                   + lonMin + " " + lonMax);
        float[] faGridX = null;
        float[] faGridY = null;
        if (firstGuess == null) {
            if ((xSpacing == OA_GRID_DEFAULT)
                    || (ySpacing == OA_GRID_DEFAULT)) {
                Barnes.AnalysisParameters ap =
                    Barnes.getRecommendedParameters(lonMin, latMin, lonMax,
                        latMax, new float[][] {
                    obVals[0], obVals[1]
                });
                faGridX     = ap.getGridXArray();
                faGridY     = ap.getGridYArray();
                scaleLength = (float) ap.getScaleLengthGU();
                log_.debug("random data spacing = "
                           + ap.getRandomDataSpacing());
            } else {
                faGridX = Barnes.getRecommendedGridX(lonMin, lonMax,
                        xSpacing);
                faGridY = Barnes.getRecommendedGridY(latMin, latMax,
                        ySpacing);
            }
        } else {  // figure out XY, convert units
            GriddedSet domainSet =
                (GriddedSet) GridUtil.getSpatialDomain(firstGuess);
            CoordinateSystem refCS = domainSet.getCoordinateSystem();
            float[]          his   = domainSet.getHi();
            float[]          lows  = domainSet.getLow();
            int[]            sizes = domainSet.getLengths();
            faGridX = Barnes.getRecommendedGridX(lows[0], his[0],
                    (his[0] - lows[0]) / (sizes[0] - 1));
            faGridY = Barnes.getRecommendedGridX(lows[1], his[1],
                    (his[1] - lows[1]) / (sizes[1] - 1));
            if (refCS != null) {
                float[][] transformedXY =
                    new float[domainSet.getDimension()][];
                if (GridUtil.isLatLonOrder(firstGuess)) {
                    transformedXY[0] = obVals[1];
                    transformedXY[1] = obVals[0];
                } else {
                    transformedXY[0] = obVals[0];
                    transformedXY[1] = obVals[1];
                }
                if (transformedXY.length == 3) {
                    transformedXY[2] = new float[obVals[0].length];
                }
                transformedXY = refCS.fromReference(transformedXY);
                obVals[0]     = transformedXY[0];
                obVals[1]     = transformedXY[1];
            } else {
                // check for 0-360 in domain
                float low, hi;
                if (GridUtil.isLatLonOrder(firstGuess)) {
                    low = lows[1];
                    hi  = his[1];
                } else {
                    low = lows[0];
                    hi  = his[0];
                }
                if ((hi > 180) || (low < -180)) {
                    //System.out.println("normalizing longitudes to 0-360");
                    obVals[0] = GeoUtils.normalizeLongitude360(obVals[0]);
                }
            }
            // HACK for grids in gpm!
            Unit guessUnits = firstGuess.getDefaultRangeUnits()[0];
            if (guessUnits
                    .equals(GeopotentialAltitude
                        .getGeopotentialMeter()) && Unit
                            .canConvert(type.getDefaultUnit(), CommonUnit
                                .meter)) {
                guessUnits = CommonUnit.meter;
            }
            //System.out.println("guess units = " + guessUnits + ", obUnits = " + type.getDefaultUnit());
            obVals[2] = guessUnits.toThis(obVals[2], type.getDefaultUnit(),
                                          false);
            outputUnits = guessUnits;
        }
        if (params != null) {
            params.setGridXArray(faGridX);
            params.setGridYArray(faGridY);
            params.setScaleLengthGU(scaleLength);
        }
        log_.debug("num X pts = " + faGridX.length + "  num Y pts = "
                   + faGridY.length + " scaleLength = " + scaleLength
                   + " gain = " + gain);


        float[][] griddedData = null;

        if (firstGuess != null) {
            float[][] gridVals =
                GridUtil.makeGrid2D(firstGuess).getvalues()[0];
            griddedData = Barnes.point2grid(faGridX, faGridY, obVals,
                                            gridVals, scaleLength, gain,
                                            numPasses);
        } else {
            griddedData = Barnes.point2grid(faGridX, faGridY, obVals,
                                            scaleLength, gain, numPasses);
        }


        float[][] faaGridValues3 =
            new float[1][faGridX.length * faGridY.length];

        int m = 0;
        for (int j = 0; j < faGridY.length; j++) {
            for (int i = 0; i < faGridX.length; i++) {
                faaGridValues3[0][m] = (float) griddedData[i][j];
                m++;
            }
        }

        GriddedSet    gdsSet  = null;
        RealTupleType gdsType = null;
        if (firstGuess == null) {
            Linear1DSet xSet = new Linear1DSet(RealType.Longitude,
                                   faGridX[0], faGridX[faGridX.length - 1],
                                   faGridX.length);
            Linear1DSet ySet = new Linear1DSet(RealType.Latitude, faGridY[0],
                                   faGridY[faGridY.length - 1],
                                   faGridY.length);
            gdsSet = new LinearLatLonSet(RealTupleType.SpatialEarth2DTuple,
                                         new Linear1DSet[] { xSet,
                    ySet }, (CoordinateSystem) null, (Unit[]) null,
                            (ErrorEstimate[]) null, true);
        } else {
            gdsSet = (GriddedSet) GridUtil.getSpatialDomain(firstGuess);
        }
        FunctionType ftLatLon2Param =
            new FunctionType(((SetType) gdsSet.getType()).getDomain(),
                             new RealTupleType(type));
        FlatField retData = new FlatField(ftLatLon2Param, gdsSet,
                                          (CoordinateSystem) null,
                                          (Set[]) null,
                                          new Unit[] { outputUnits });
        retData.setSamples(faaGridValues3, false);
        return retData;

    }

    /**
     * Extract the parameter from some point obs
     * @param obs  Field of point obs
     * @param paramName name of the parameter to extract
     * @return new FieldImpl where the data for each Point ob is just the param
     *
     * @throws RemoteException Java RMI exception
     * @throws VisADException  problem extracting parameter
     */
    public static FieldImpl extractParameter(FieldImpl obs, String paramName)
            throws VisADException, RemoteException {
        return extractParameter(obs, RealType.getRealType(paramName));
    }

    /**
     * Extract the parameter from some point obs
     * @param obs  Field of point obs
     * @param parameter  parameter to extract
     * @return new FieldImpl where the data for each Point ob is just the param
     *
     * @throws RemoteException Java RMI exception
     * @throws VisADException  problem extracting parameter
     */
    public static FieldImpl extractParameter(FieldImpl obs,
                                             RealType parameter)
            throws VisADException, RemoteException {

        boolean   isTimeSequence = GridUtil.isTimeSequence(obs);
        FieldImpl subset         = null;
        if (isTimeSequence) {
            FieldImpl timeSubset = null;
            Set       timeSet    = obs.getDomainSet();
            int       numTimes   = timeSet.getLength();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl timeStep = (FieldImpl) obs.getSample(i);
                if ((timeStep == null) || timeStep.isMissing()) {
                    continue;
                }
                FieldImpl newSample = extractParameter(timeStep, parameter);
                if (newSample == null) {
                    continue;
                }
                if (timeSubset == null) {
                    FunctionType newFieldType =
                        new FunctionType(
                            ((SetType) timeSet.getType()).getDomain(),
                            newSample.getType());
                    timeSubset = new FieldImpl(newFieldType, timeSet);
                }
                timeSubset.setSample(i, newSample, false);
            }
            subset = timeSubset;
        } else {
            Set     indexSet = obs.getDomainSet();
            PointOb po       = null;
            try {
                po = (PointOb) obs.getSample(0);
            } catch (ClassCastException cce) {
                throw new VisADException(
                    "not a field of pointObs: "
                    + obs.getSample(0).getClass().getName());
            }
            Tuple ob         = (Tuple) po.getData();
            int   paramIndex = ((TupleType) ob.getType()).getIndex(parameter);
            if (paramIndex == -1) {
                throw new VisADException("Parameter does not exist in obs");
            }
            for (int i = 0; i < indexSet.getLength(); i++) {
                PointOb sample = (PointOb) obs.getSample(i);
                Tuple   data   = (Tuple) sample.getData();
                Real    parm   = (Real) data.getComponent(paramIndex);
                PointObTuple newPO =
                    new PointObTuple(sample.getEarthLocation(),
                                     sample.getDateTime(),
                                     new RealTuple(new Real[] { parm }));
                if (subset == null) {
                    FunctionType subsetType =
                        new FunctionType(
                            ((SetType) indexSet.getType()).getDomain(),
                            newPO.getType());
                    subset = new FieldImpl(subsetType, indexSet);
                }
                subset.setSample(i, newPO, false);
            }
        }
        return subset;
    }


    /**
     * Get the bounding box of the given obs
     *
     * @param pointObs the obs
     *
     * @return bbox of the given time field-  { minY, minX, maxY, maxX };
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static double[] getBoundingBox(FieldImpl pointObs)
            throws VisADException, RemoteException {
        boolean isTimeSequence = GridUtil.isTimeSequence(pointObs);
        if (isTimeSequence) {
            double[] bbox     = null;
            Set      timeSet  = pointObs.getDomainSet();
            int      numTimes = timeSet.getLength();
            for (int i = 0; i < numTimes; i++) {
                FieldImpl oneTime = (FieldImpl) pointObs.getSample(i);
                //{ minY, minX, maxY, maxX };
                double[] tmp = PointObFactory.getBoundingBoxOneTime(oneTime);
                if (bbox == null) {
                    bbox = tmp;
                } else {
                    bbox[0] = Math.min(bbox[0], tmp[0]);
                    bbox[1] = Math.min(bbox[1], tmp[1]);
                    bbox[2] = Math.max(bbox[0], tmp[2]);
                    bbox[3] = Math.max(bbox[1], tmp[3]);
                }
            }
            return bbox;
        } else {
            return PointObFactory.getBoundingBoxOneTime(pointObs);
        }
    }


    /**
     * Get the bounding box of the given obs
     *
     * @param pointObs the obs
     *
     * @return bbox of the given time field-  { minY, minX, maxY, maxX };
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public static double[] getBoundingBoxOneTime(FieldImpl pointObs)
            throws VisADException, RemoteException {

        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;


        if ( !pointObs.isMissing()) {
            Set domainSet = pointObs.getDomainSet();
            int numObs    = domainSet.getLength();
            for (int i = 0; i < numObs; i++) {
                PointOb     ob  = (PointOb) pointObs.getSample(i);
                LatLonPoint llp = ob.getEarthLocation().getLatLonPoint();
                double lat = llp.getLatitude().getValue(CommonUnit.degree);
                double lon = llp.getLongitude().getValue(CommonUnit.degree);
                if ((lat == lat) && (lon == lon)) {
                    if (Math.abs(lat) <= 90) {
                        minY = Math.min(minY, lat);
                        maxY = Math.max(maxY, lat);
                    }
                    if (Math.abs(lon) <= 180) {
                        minX = Math.min(minX, lon);
                        maxX = Math.max(maxX, lon);
                    }
                }

            }
        }
        double[] bbox = { minY, minX, maxY, maxX };
        return bbox;
    }





}
