/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.grid;

import ucar.nc2.time.Calendar;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObTuple;
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.util.Range;
import ucar.visad.Util;
import ucar.visad.data.CalendarDateTime;
import ucar.visad.data.CalendarDateTimeSet;
import visad.*;
import visad.data.CachedFlatField;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class WrfUtil extends  ucar.unidata.data.grid.GridUtil {
    /**
     * Find min and max of range data in any VisAD FieldImpl
     *
     * @param grid       a VisAD FlatField.  Cannot be null
     * @param min       double value
     * @param max       double value
     * @return  the range of the data as point obs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl getGridMinMaxAsPointObs(FieldImpl grid, double min, double max)
            throws VisADException {
        if (grid == null) {
            return null;
        }
        RealType  index    = RealType.getRealType("index");
        FieldImpl retField = null;
        try {
            if (isTimeSequence(grid)) {
                SampledSet   timeSet      = (SampledSet) getTimeSet(grid);
                FunctionType retFieldType = null;
                double[][]   times        = timeSet.getDoubles(false);
                Unit         timeUnit     = timeSet.getSetUnits()[0];
                if ( !timeUnit.equals(CommonUnit.secondsSinceTheEpoch)) {
                    Unit.convertTuple(
                            times, timeSet.getSetUnits(),
                            new Unit[] { CommonUnit.secondsSinceTheEpoch }, true);
                }
                Calendar cal = null;
                if (timeSet instanceof CalendarDateTimeSet) {
                    cal = ((CalendarDateTimeSet) timeSet).getCalendar();
                }
                for (int i = 0; i < timeSet.getLength(); i++) {
                    CalendarDateTime dt = new CalendarDateTime(times[0][i],
                            cal);
                    FieldImpl ff =
                            makePointObs((FlatField) grid.getSample(i), dt, min, max);
                    if (ff == null) {
                        continue;
                    }
                    if (retFieldType == null) {
                        retFieldType = new FunctionType(
                                ((SetType) timeSet.getType()).getDomain(),
                                ff.getType());
                        retField = new FieldImpl(retFieldType, timeSet);
                    }
                    retField.setSample(i, ff, false);
                }
            } else {
                retField = makePointObs((FlatField) grid,
                        new DateTime(Double.NaN), min,max);
            }
        } catch (RemoteException re) {}
        return retField;
    }

    /**
     * Find min and max of range data in any VisAD FieldImpl
     *
     * @param grid0       a VisAD FlatField.  Cannot be null
     * @param level      level value
     * @param value      float
     * @return  the range of the data as point obs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static FieldImpl getSpeedAreaNRadiusAsPointObs(FieldImpl grid0, int level, float value)
            throws VisADException {
        if (grid0 == null) {
            return null;
        }
        FieldImpl grid = null;
        if(canSliceAtLevel(grid0, new Real(level))){
            grid = sliceAtLevel(grid0, new Real(level));
            grid = make2DGridFromSlice(grid, false);
        } else {
            grid = make2DGridFromSlice(grid0, false);;
        }
        //RealType  index    = RealType.getRealType("index");
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        FieldImpl retField = null;
        //Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
        //        4);
        TupleType rangeType0 = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                RealType.getRealType("distance", kmUnit) });

        List<PointOb> pointObsList = new ArrayList<>();
        try {
            if (isTimeSequence(grid)) {
                SampledSet   timeSet      = (SampledSet) getTimeSet(grid);
                FunctionType retFieldType = null;
                double[][]   times        = timeSet.getDoubles(false);
                Unit         timeUnit     = timeSet.getSetUnits()[0];
                if ( !timeUnit.equals(CommonUnit.secondsSinceTheEpoch)) {
                    Unit.convertTuple(
                            times, timeSet.getSetUnits(),
                            new Unit[] { CommonUnit.secondsSinceTheEpoch }, true);
                }
                Calendar cal = null;
                if (timeSet instanceof CalendarDateTimeSet) {
                    cal = ((CalendarDateTimeSet) timeSet).getCalendar();
                }
                for (int i = 0; i < timeSet.getLength(); i++) {
                    CalendarDateTime dt = new CalendarDateTime(times[0][i],
                            cal);
                    PointOb[] pointObs =
                            makePointObsForAreaRadius((FlatField) grid.getSample(i), dt,value, FUNC_MAX);
                    if(pointObs != null && pointObs.length > 1) {
                        for(PointOb pob: pointObs)
                            pointObsList.add(pob);
                    }
                }
                int ssize = pointObsList.size();
                PointOb[] finalPointOb =  new PointOb[ssize];
                for(int i = 0; i < ssize; i++)
                    finalPointOb[i] = pointObsList.get(i);

                Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                        ssize);
                FieldImpl ff = new FieldImpl(
                        new FunctionType(
                                ((SetType) points.getType()).getDomain(),
                                rangeType0), points);
                ff.setSamples(finalPointOb, false, false);
                return ff;
            } else {
                retField = makePointObs((FlatField) grid,
                        new DateTime(Double.NaN));
            }
        } catch (RemoteException re) {}
        return retField;
    }

    public static FieldImpl getSpeedAreaNRadiusAsPointObs(FieldImpl grid1, FieldImpl grid0, int level, float value)
            throws VisADException {
        if (grid0 == null) {
            return null;
        }
        FieldImpl grid = null;
        if(canSliceAtLevel(grid0, new Real(level))){
            grid = sliceAtLevel(grid0, new Real(level));
            grid = make2DGridFromSlice(grid, false);
        } else {
            grid = make2DGridFromSlice(grid0, false);;
        }

        FieldImpl gridh = null;
        if(canSliceAtLevel(grid1, new Real(level))){
            gridh = sliceAtLevel(grid1, new Real(level));
            gridh = make2DGridFromSlice(gridh, false);
        } else {
            gridh = make2DGridFromSlice(grid1, false);;
        }
        //RealType  index    = RealType.getRealType("index");
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        FieldImpl retField = null;
        //Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
        //        4);
        TupleType rangeType0 = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                RealType.getRealType("distance", kmUnit) });

        List<PointOb> pointObsList = new ArrayList<>();
        try {
            if (isTimeSequence(grid)) {
                SampledSet   timeSet      = (SampledSet) getTimeSet(grid);
                FunctionType retFieldType = null;
                double[][]   times        = timeSet.getDoubles(false);
                Unit         timeUnit     = timeSet.getSetUnits()[0];
                if ( !timeUnit.equals(CommonUnit.secondsSinceTheEpoch)) {
                    Unit.convertTuple(
                            times, timeSet.getSetUnits(),
                            new Unit[] { CommonUnit.secondsSinceTheEpoch }, true);
                }
                Calendar cal = null;
                if (timeSet instanceof CalendarDateTimeSet) {
                    cal = ((CalendarDateTimeSet) timeSet).getCalendar();
                }
                for (int i = 0; i < timeSet.getLength(); i++) {
                    CalendarDateTime dt = new CalendarDateTime(times[0][i],
                            cal);
                    PointOb[] pointObs =
                            makePointObsForAreaRadius((FlatField) gridh.getSample(i), (FlatField) grid.getSample(i), dt,value, FUNC_MAX);
                    if(pointObs != null && pointObs.length > 1) {
                        for(PointOb pob: pointObs)
                            pointObsList.add(pob);
                    }
                }
                int ssize = pointObsList.size();
                PointOb[] finalPointOb =  new PointOb[ssize];
                for(int i = 0; i < ssize; i++)
                    finalPointOb[i] = pointObsList.get(i);

                Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                        ssize);
                FieldImpl ff = new FieldImpl(
                        new FunctionType(
                                ((SetType) points.getType()).getDomain(),
                                rangeType0), points);
                ff.setSamples(finalPointOb, false, false);
                return ff;
            } else {
                retField = makePointObs((FlatField) grid,
                        new DateTime(Double.NaN));
            }
        } catch (RemoteException re) {}
        return retField;
    }

    public static PointOb[] makePointObsForAreaRadius(FlatField timeSteph, FlatField timeStep, DateTime dt, float val, String function)
            throws VisADException, RemoteException {
        final boolean doMax = function.equals(FUNC_MAX);
        final boolean doMin = function.equals(FUNC_MIN);
        if (timeStep == null) {
            return null;
        }
        SampledSet domain    = getSpatialDomain(timeStep);
        int        numPoints = domain.getLength();
        PointOb[] obs = new PointOb[5];
        TupleType tt = getParamType(timeStep);
        TupleType rangeType = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                tt });
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        TupleType rangeType0 = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                RealType.getRealType("distance", kmUnit) });
        RealTupleType rtt =
                new RealTupleType(DataUtil.makeRealType("distance",
                        RealType.getRealType("distance", kmUnit).getDefaultUnit()));

        float[][] geoVals  = getEarthLocationPoints((GriddedSet) domain);
        boolean   isLatLon = isLatLonOrder(domain);
        int       latIndex = isLatLon
                ? 0
                : 1;
        int       lonIndex = isLatLon
                ? 1
                : 0;
        boolean   haveAlt  = geoVals.length > 2;
        float   pMin   = Float.POSITIVE_INFINITY;
        float   pMax   = Float.NEGATIVE_INFINITY;
        int index = 0;
        // first find the center of max or min
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            if ((lat == lat) && (lon == lon)) {
                if((float)timeSteph.getValues(i)[0] < pMin){
                    pMin = (float)timeSteph.getValues(i)[0];
                    index = i;
                }
            }
        }
        float lat0 = geoVals[latIndex][index];
        float lon0 = geoVals[lonIndex][index];
        float alt0  = haveAlt
                ? geoVals[2][index]
                : 0;
        EarthLocation el0 = new EarthLocationLite(lat0, lon0, alt0);
        // TODO:  make this  more efficient
        //PointObTuple pot = new PointObTuple(el0, dt,
        //        timeStep.getSample(index), rangeType);
        //ff.setSample(0, pot, false, false);

        // now look for the surrounding points
        float deltaLatLon = 0.01f;
        float preVal = (float)timeStep.getValues(index)[0];
        double areaTotal = 0.0;
        float [] lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   0.0f);
        Bearing result1 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   15.0f);
        Bearing result3 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   75.0f);
        Bearing result4 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   90.0f);
        Bearing result5 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   45.0f);
        Bearing result2 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        float dis1 = (float)(result1.getDistance() + result2.getDistance() + result3.getDistance() +
                result4.getDistance() + result5.getDistance())/5.0f;
        obs[1] = getPointObValTuple(lln, dis1, alt0, rtt, rangeType0, dt);


        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   105.0f);
        Bearing result7 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   165.0f);
        Bearing result8 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   180.0f);
        Bearing result9 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   135.0f);
        Bearing result6 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        float dis2 = (float)(result5.getDistance() + result6.getDistance() + result7.getDistance() +
                result8.getDistance() + result9.getDistance())/5.0f;
        obs[2] = getPointObValTuple(lln, dis2, alt0, rtt, rangeType0, dt);


        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   195.0f);
        Bearing result11 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   255.0f);
        Bearing result12 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   270.0f);
        Bearing result13 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   225.0f);
        Bearing result10 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        float dis3 = (float)(result9.getDistance() + result10.getDistance() + result11.getDistance() +
                result12.getDistance() + result13.getDistance())/5.0f;
        obs[3] = getPointObValTuple(lln, dis3, alt0, rtt, rangeType0, dt);


        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   285.0f);
        Bearing result15 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   345.0f);
        Bearing result16 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   315.0f);
        Bearing result14 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        float dis4 = (float)(result13.getDistance() + result14.getDistance() + result15.getDistance() +
                result16.getDistance() + result1.getDistance())/5.0f;
        obs[4] = getPointObValTuple(lln, dis4, alt0, rtt, rangeType0, dt);

        areaTotal = 0.25*3.14*(dis1*dis1 + dis2*dis2 + dis3*dis3 + dis4*dis4);
        double[] distance = new double[]{areaTotal};
        Data sample = new RealTuple(rtt, distance);
        PointObTuple pot = new PointObTuple(el0, dt, sample, rangeType0);
        obs[0] = pot;

        return obs;
    }

    /**
     * Make point obs from a single timestep of a grid
     *
     * @param timeStep     the grid
     * @param dt           the timestep for the grid
     *
     * @return   a Field of PointObs
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    private static FieldImpl makePointObs(FlatField timeStep, DateTime dt)
            throws VisADException, RemoteException {
        if (timeStep == null) {
            return null;
        }
        SampledSet domain    = getSpatialDomain(timeStep);
        int        numPoints = domain.getLength();
        Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                numPoints);
        TupleType tt = getParamType(timeStep);
        TupleType rangeType = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                tt });
        FieldImpl ff = new FieldImpl(
                new FunctionType(
                        ((SetType) points.getType()).getDomain(),
                        rangeType), points);
        float[][] samples  = timeStep.getFloats(false);
        float[][] geoVals  = getEarthLocationPoints((GriddedSet) domain);
        boolean   isLatLon = isLatLonOrder(domain);
        int       latIndex = isLatLon
                ? 0
                : 1;
        int       lonIndex = isLatLon
                ? 1
                : 0;
        boolean   haveAlt  = geoVals.length > 2;
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            float alt = haveAlt
                    ? geoVals[2][i]
                    : 0;
            if ((lat == lat) && (lon == lon)) {
                if ( !(alt == alt)) {
                    alt = 0;
                }
                EarthLocation el = new EarthLocationLite(lat, lon, alt);
                // TODO:  make this  more efficient
                PointObTuple pot = new PointObTuple(el, dt,
                        timeStep.getSample(i), rangeType);
                ff.setSample(i, pot, false, false);
            }
        }
        return ff;
    }

    /**
     * Make point obs from a single timestep of a grid
     *
     * @param timeStep     the grid
     * @param dt           the timestep for the grid
     *
     * @return   a Field of PointObs
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    private static FieldImpl makePointObs(FlatField timeStep, DateTime dt, String function)
            throws VisADException, RemoteException {
        final boolean doMax = function.equals(FUNC_MAX);
        final boolean doMin = function.equals(FUNC_MIN);
        if (timeStep == null) {
            return null;
        }
        SampledSet domain    = getSpatialDomain(timeStep);
        int        numPoints = domain.getLength();
        Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                numPoints);
        TupleType tt = getParamType(timeStep);
        TupleType rangeType = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                tt });
        FieldImpl ff = new FieldImpl(
                new FunctionType(
                        ((SetType) points.getType()).getDomain(),
                        rangeType), points);
        float[][] samples  = timeStep.getFloats(false);
        float[][] geoVals  = getEarthLocationPoints((GriddedSet) domain);
        boolean   isLatLon = isLatLonOrder(domain);
        int       latIndex = isLatLon
                ? 0
                : 1;
        int       lonIndex = isLatLon
                ? 1
                : 0;
        boolean   haveAlt  = geoVals.length > 2;
        float   pMin   = Float.POSITIVE_INFINITY;
        float   pMax   = Float.NEGATIVE_INFINITY;
        int index = 0;
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            float alt = haveAlt
                    ? geoVals[2][i]
                    : 0;
            if ((lat == lat) && (lon == lon)) {
                if ( !(alt == alt)) {
                    alt = 0;
                }
                if(doMax && (float)timeStep.getValues(i)[0] >= pMax){
                    pMax = (float)timeStep.getValues(i)[0];
                    index = i;
                } else if(doMin && (float)timeStep.getValues(i)[0] < pMin){
                    pMin = (float)timeStep.getValues(i)[0];
                    index = i;
                }
            }
        }
        float alt0  = haveAlt
                ? geoVals[2][index]
                : 0;
        EarthLocation el0 = new EarthLocationLite(geoVals[latIndex][index], geoVals[lonIndex][index], alt0);
        // TODO:  make this  more efficient
        PointObTuple pot = new PointObTuple(el0, dt,
                timeStep.getSample(index), rangeType);
        ff.setSample(0, pot, false, false);
        return ff;
    }

    /**
     * Make point obs from a single timestep of a grid
     *
     * @param timeStep     the grid
     * @param dt           the timestep for the grid
     * @param val   float value
     * @param function name string
     * @return   a Field of PointObs
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    public static PointOb[] makePointObsForAreaRadius(FlatField timeStep, DateTime dt, float val, String function)
            throws VisADException, RemoteException {
        final boolean doMax = function.equals(FUNC_MAX);
        final boolean doMin = function.equals(FUNC_MIN);
        if (timeStep == null) {
            return null;
        }
        SampledSet domain    = getSpatialDomain(timeStep);
        int        numPoints = domain.getLength();
        PointOb[] obs = new PointOb[5];
        TupleType tt = getParamType(timeStep);
        TupleType rangeType = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                tt });
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        TupleType rangeType0 = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                RealType.getRealType("distance", kmUnit) });
        RealTupleType rtt =
                new RealTupleType(DataUtil.makeRealType("distance",
                        RealType.getRealType("distance", kmUnit).getDefaultUnit()));

        float[][] geoVals  = getEarthLocationPoints((GriddedSet) domain);
        boolean   isLatLon = isLatLonOrder(domain);
        int       latIndex = isLatLon
                ? 0
                : 1;
        int       lonIndex = isLatLon
                ? 1
                : 0;
        boolean   haveAlt  = geoVals.length > 2;
        float   pMin   = Float.POSITIVE_INFINITY;
        float   pMax   = Float.NEGATIVE_INFINITY;
        int index = 0;
        // first find the center of max or min
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            float alt = haveAlt
                    ? geoVals[2][i]
                    : 0;
            if ((lat == lat) && (lon == lon)) {
                if ( !(alt == alt)) {
                    alt = 0;
                }
                if(doMax && (float)timeStep.getValues(i)[0] >= pMax){
                    pMax = (float)timeStep.getValues(i)[0];
                    index = i;
                } else if(doMin && (float)timeStep.getValues(i)[0] < pMin){
                    pMin = (float)timeStep.getValues(i)[0];
                    index = i;
                }
            }
        }
        float lat0 = geoVals[latIndex][index];
        float lon0 = geoVals[lonIndex][index];
        float alt0  = haveAlt
                ? geoVals[2][index]
                : 0;
        EarthLocation el0 = new EarthLocationLite(lat0, lon0, alt0);
        // TODO:  make this  more efficient
        //PointObTuple pot = new PointObTuple(el0, dt,
        //        timeStep.getSample(index), rangeType);
        //ff.setSample(0, pot, false, false);

        // now look for the surrounding points
        float deltaLatLon = 0.01f;
        float preVal = (float)timeStep.getValues(index)[0];
        double areaTotal = 0.0;
        float [] lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   0.0f);
        Bearing result1 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   15.0f);
        Bearing result3 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   75.0f);
        Bearing result4 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   90.0f);
        Bearing result5 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   45.0f);
        Bearing result2 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        float dis1 = (float)(result1.getDistance() + result2.getDistance() + result3.getDistance() +
                result4.getDistance() + result5.getDistance())/5.0f;
        obs[1] = getPointObValTuple(lln, dis1, alt0, rtt, rangeType0, dt);


        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   105.0f);
        Bearing result7 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   165.0f);
        Bearing result8 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   180.0f);
        Bearing result9 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   135.0f);
        Bearing result6 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        float dis2 = (float)(result5.getDistance() + result6.getDistance() + result7.getDistance() +
                result8.getDistance() + result9.getDistance())/5.0f;
        obs[2] = getPointObValTuple(lln, dis2, alt0, rtt, rangeType0, dt);


        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   195.0f);
        Bearing result11 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   255.0f);
        Bearing result12 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   270.0f);
        Bearing result13 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   225.0f);
        Bearing result10 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        float dis3 = (float)(result9.getDistance() + result10.getDistance() + result11.getDistance() +
                result12.getDistance() + result13.getDistance())/5.0f;
        obs[3] = getPointObValTuple(lln, dis3, alt0, rtt, rangeType0, dt);


        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   285.0f);
        Bearing result15 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   345.0f);
        Bearing result16 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaLatLon,   315.0f);
        Bearing result14 = Bearing.calculateBearing(new LatLonPointImpl(lln[0],
                lln[1]), new LatLonPointImpl(lat0, lon0));
        float dis4 = (float)(result13.getDistance() + result14.getDistance() + result15.getDistance() +
                result16.getDistance() + result1.getDistance())/5.0f;
        obs[4] = getPointObValTuple(lln, dis4, alt0, rtt, rangeType0, dt);

        areaTotal = 0.25*3.14*(dis1*dis1 + dis2*dis2 + dis3*dis3 + dis4*dis4);
        double[] distance = new double[]{areaTotal};
        Data sample = new RealTuple(rtt, distance);
        PointObTuple pot = new PointObTuple(el0, dt, sample, rangeType0);
        obs[0] = pot;

        return obs;
    }

    public static PointObTuple getPointObValTuple(float lln[], float value, float alt0,
                                                  RealTupleType rtt, TupleType rangeType0, DateTime dt)
            throws VisADException, RemoteException {
        EarthLocation el1 = new EarthLocationLite(lln[0], lln[1], alt0);

        double[] distance1 = new double[]{value};
        Data sample1 = new RealTuple(rtt, distance1);
        PointObTuple pot11 = new PointObTuple(el1, dt, sample1, rangeType0);

        return pot11;
    }

    public static float[] findPointDisFromMaxCenter(FieldImpl timeStep, float val, float preVal,
                                                    float lat0, float lon0, float alt0,
                                                    float deltaLatLon, float angle) throws VisADException, RemoteException{
        float [] point = new float[2];
        float    degreesToRadians = (float) (Math.PI / 180.0);
        for(int i = 1; i< 1000; i++){
            float lat = lat0 + deltaLatLon * i * (float)Math.sin(angle * degreesToRadians);
            float lon = lon0 + deltaLatLon * i * (float)Math.cos(angle * degreesToRadians);;
            EarthLocation el = new EarthLocationLite(lat, lon, alt0);
            FieldImpl f1  = sample(timeStep, el.getLatLonPoint(),
                    DEFAULT_SAMPLING_MODE, DEFAULT_ERROR_MODE);
            if(f1.getFloats()[0][0] <= val && preVal >= val) {
                //double[] distance = new double[]{result.getDistance()};
                point[0] = lat;
                point[1] = lon;
                //ff.setSample(3, pot1, false, false);
                break;
            } else {
                preVal = f1.getFloats()[0][0];
            }
        }
        return point;
    }

    /**
     *   Find min and max of range data in any VisAD FlatField
     *
     * @param field       a VisAD FlatField.  Cannot be null
     * @param function   function name
     * @return  the range of the data.  Dimension is the number of parameters
     *          in the range of the flat field
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static Range[] fieldMinMax(visad.FlatField field, String function)
            throws VisADException, RemoteException {
        if (field instanceof CachedFlatField) {
            return makeRanges(((CachedFlatField) field).getRanges());
        }


        float   allValues[][] = field.getFloats(false);
        Range[] result        = new Range[allValues.length];
        for (int rangeIdx = 0; rangeIdx < allValues.length; rangeIdx++) {
            float   pMin   = Float.POSITIVE_INFINITY;
            float   pMax   = Float.NEGATIVE_INFINITY;
            float[] values = allValues[rangeIdx];
            int     length = values.length;
            for (int i = 0; i < length; i++) {
                float value = values[i];
                //Note: we don't check for Float.isNaN (value) because if value is a
                //NaN then each test below is false;
                if (pMax < value) {
                    pMax = value;
                }
                if (pMin > value) {
                    pMin = value;
                }
            }
            result[rangeIdx] = new Range(pMin, pMax);
        }
        return result;
    }

    /**
     * Make point obs from a single timestep of a grid
     *
     * @param timeStep     the grid
     * @param dt           the timestep for the grid
     *
     * @return   a Field of PointObs
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    private static FieldImpl makePointObs(FlatField timeStep, DateTime dt, double min, double max)
            throws VisADException, RemoteException {
        if (timeStep == null) {
            return null;
        }
        SampledSet domain    = getSpatialDomain(timeStep);
        int        numPoints = domain.getLength();
        Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                numPoints);
        TupleType tt = getParamType(timeStep);
        TupleType rangeType = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                tt });
        FieldImpl ff = new FieldImpl(
                new FunctionType(
                        ((SetType) points.getType()).getDomain(),
                        rangeType), points);
        float[][] samples  = timeStep.getFloats(false);
        float[][] geoVals  = getEarthLocationPoints((GriddedSet) domain);
        boolean   isLatLon = isLatLonOrder(domain);
        int       latIndex = isLatLon
                ? 0
                : 1;
        int       lonIndex = isLatLon
                ? 1
                : 0;
        boolean   haveAlt  = geoVals.length > 2;
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            float alt = haveAlt
                    ? geoVals[2][i]
                    : 0;
            if ((lat == lat) && (lon == lon)) {
                if ( !(alt == alt)) {
                    alt = 0;
                }
                if(timeStep.getValues(i)[0] >= min && timeStep.getValues(i)[0] < max) {
                    EarthLocation el = new EarthLocationLite(lat, lon, alt);
                    // TODO:  make this  more efficient
                    PointObTuple pot = new PointObTuple(el, dt,
                            timeStep.getSample(i), rangeType);
                    ff.setSample(i, pot, false, false);
                }
            }
        }
        return ff;
    }

    public static FieldImpl getIsoSurfaceTopBottomAsPointObs(FieldImpl grid,  float value)
            throws VisADException {
        if (grid == null) {
            return null;
        }
        //FieldImpl grid = null;

        //RealType  index    = RealType.getRealType("index");
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        FieldImpl retField = null;
        //Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
        //        4);
        TupleType rangeType0 = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                RealType.getRealType("distance", kmUnit) });

        List<PointOb> pointObsList = new ArrayList<>();
        try {
            if (isTimeSequence(grid)) {
                SampledSet   timeSet      = (SampledSet) getTimeSet(grid);
                double[][]   times        = timeSet.getDoubles(false);
                Unit         timeUnit     = timeSet.getSetUnits()[0];
                if ( !timeUnit.equals(CommonUnit.secondsSinceTheEpoch)) {
                    Unit.convertTuple(
                            times, timeSet.getSetUnits(),
                            new Unit[] { CommonUnit.secondsSinceTheEpoch }, true);
                }
                Calendar cal = null;
                if (timeSet instanceof CalendarDateTimeSet) {
                    cal = ((CalendarDateTimeSet) timeSet).getCalendar();
                }
                for (int i = 0; i < timeSet.getLength(); i++) {
                    CalendarDateTime dt = new CalendarDateTime(times[0][i],
                            cal);
                    PointOb[] pointObs =
                            makePointObsForTopBottom((FlatField) grid.getSample(i), dt,value, FUNC_MAX);
                    if(pointObs != null && pointObs.length > 1) {
                        for(PointOb pob: pointObs)
                            pointObsList.add(pob);
                    }
                }
                int ssize = pointObsList.size();
                PointOb[] finalPointOb =  new PointOb[ssize];
                for(int i = 0; i < ssize; i++)
                    finalPointOb[i] = pointObsList.get(i);

                Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                        ssize);
                FieldImpl ff = new FieldImpl(
                        new FunctionType(
                                ((SetType) points.getType()).getDomain(),
                                rangeType0), points);
                ff.setSamples(finalPointOb, false, false);
                return ff;
            } else {
                retField = makePointObs((FlatField) grid,
                        new DateTime(Double.NaN));
            }
        } catch (RemoteException re) {}
        return retField;
    }

    /**
     * Make point obs from a single timestep of a grid
     *
     * @param timeStep     the grid
     * @param dt           the timestep for the grid
     * @param val  the value
     * @param function name of function
     * @return   a Field of PointObs
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    public static PointOb[] makePointObsForTopBottom(FlatField timeStep, DateTime dt, float val, String function)
            throws VisADException, RemoteException {
        final boolean doMax = function.equals(FUNC_MAX);
        final boolean doMin = function.equals(FUNC_MIN);
        if (timeStep == null) {
            return null;
        }
        SampledSet domain    = getSpatialDomain(timeStep);
        int        numPoints = domain.getLength();
        PointOb[] obs = new PointOb[3];
        TupleType tt = getParamType(timeStep);
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        TupleType rangeType0 = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                RealType.getRealType("distance", kmUnit) });
        RealTupleType rtt =
                new RealTupleType(DataUtil.makeRealType("distance",
                        RealType.getRealType("distance", kmUnit).getDefaultUnit()));

        float[][] geoVals  = getEarthLocationPoints((GriddedSet) domain);
        boolean   isLatLon = isLatLonOrder(domain);
        int       latIndex = isLatLon
                ? 0
                : 1;
        int       lonIndex = isLatLon
                ? 1
                : 0;
        boolean   haveAlt  = geoVals.length > 2;
        float   pMin   = Float.POSITIVE_INFINITY;
        float   pMax   = Float.NEGATIVE_INFINITY;
        int index = 0;
        // first find the center of max or min
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            float alt = haveAlt
                    ? geoVals[2][i]
                    : 0;
            if ((lat == lat) && (lon == lon)) {
                if ( !(alt == alt)) {
                    alt = 0;
                }
                if(doMax && (float)timeStep.getValues(i)[0] >= pMax){
                    pMax = (float)timeStep.getValues(i)[0];
                    index = i;
                } else if(doMin && (float)timeStep.getValues(i)[0] < pMin){
                    pMin = (float)timeStep.getValues(i)[0];
                    index = i;
                }
            }
        }
        float lat0 = geoVals[latIndex][index];
        float lon0 = geoVals[lonIndex][index];
        float alt0  = haveAlt
                ? geoVals[2][index]
                : 0;
        EarthLocation el0 = new EarthLocationLite(lat0, lon0, alt0);
        // TODO:  make this  more efficient
        //PointObTuple pot = new PointObTuple(el0, dt,
        //        timeStep.getSample(index), rangeType);
        //ff.setSample(0, pot, false, false);

        // now look for the surrounding points
        float deltaAlt = 100.0f; // meter
        float preVal = (float)timeStep.getValues(index)[0];

        float [] lln = findPointDisFromMaxCenter(timeStep, val, preVal,
                lat0,   lon0,   alt0, deltaAlt);
        EarthLocation el1 = new EarthLocationLite(lat0, lon0, lln[0]);
        double[] distance1 = new double[]{lln[0]};
        Data sample1 = new RealTuple(rtt, distance1);
        obs[1] = new PointObTuple(el1, dt, sample1, rangeType0);

        EarthLocation el2 = new EarthLocationLite(lat0, lon0, lln[1]);
        double[] distance2 = new double[]{lln[1]};
        Data sample2 = new RealTuple(rtt, distance2);
        obs[2] = new PointObTuple(el2, dt, sample2, rangeType0);

        double[] distance = new double[]{pMax};
        Data sample = new RealTuple(rtt, distance);
        PointObTuple pot = new PointObTuple(el0, dt, sample, rangeType0);
        obs[0] = pot;

        return obs;
    }

    public static float[] findPointDisFromMaxCenter(FieldImpl timeStep, float val, float preVal,
                                                    float lat0, float lon0, float alt0,
                                                    float deltaAlt) throws VisADException, RemoteException{
        float [] point = new float[2];

        for(int i = 1; i< 1000; i++){
            float alt = alt0 - deltaAlt * i;
            EarthLocation el = new EarthLocationLite(lat0, lon0, alt);
            FieldImpl f1  = sample(timeStep, el,
                    DEFAULT_SAMPLING_MODE, DEFAULT_ERROR_MODE);
            if(f1.getFloats()[0][0] <= val && preVal >= val) {
                //double[] distance = new double[]{result.getDistance()};
                point[0] = alt;
                //ff.setSample(3, pot1, false, false);
                break;
            } else {
                preVal = f1.getFloats()[0][0];
            }
        }
        for(int i = 1; i< 1000; i++){
            float alt = alt0 + deltaAlt * i;
            EarthLocation el = new EarthLocationLite(lat0, lon0, alt);
            FieldImpl f1  = sample(timeStep, el,
                    DEFAULT_SAMPLING_MODE, DEFAULT_ERROR_MODE);
            if(f1.getFloats()[0][0] <= val && preVal >= val) {
                //double[] distance = new double[]{result.getDistance()};
                point[1] = alt;
                //ff.setSample(3, pot1, false, false);
                break;
            } else {
                preVal = f1.getFloats()[0][0];
            }
        }
        return point;
    }

    public double getArea(List points, int IDX_LAT, int IDX_LON) throws Exception {
        double    area   = 0.0;
        float[][] pts    = getLatLons(points, IDX_LAT, IDX_LON);
        double    minLat = Double.POSITIVE_INFINITY;
        double    minLon = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < pts[0].length; i++) {
            if (i == 0) {
                minLat = pts[IDX_LAT][i];
                minLon = pts[IDX_LON][i];
            } else {
                minLat = Math.min(minLat, pts[IDX_LAT][i]);
                minLon = Math.min(minLon, pts[IDX_LON][i]);
            }
        }
        int len = pts[0].length;
        for (int i = 0; i < len; i++) {
            double x1 = distance(minLat, minLon, minLat, pts[IDX_LON][i]);
            double y1 = distance(minLat, minLon, pts[IDX_LAT][i], minLon);
            double x2 = distance(minLat, minLon, minLat, ((i < len - 1)
                    ? pts[IDX_LON][i + 1]
                    : pts[IDX_LON][0]));
            double y2 = distance(minLat, minLon, ((i < len - 1)
                    ? pts[IDX_LAT][i + 1]
                    : pts[IDX_LAT][0]), minLon);
            area += x1 * y2 - x2 * y1;
        }
        area = 0.5 * area;
        return Math.abs(area);
    }

    public double distance(double lat1, double lon1, double lat2,
                           double lon2)
            throws Exception {
        Bearing result = Bearing.calculateBearing(new LatLonPointImpl(lat1,
                lon1), new LatLonPointImpl(lat2, lon2));
        double distance = result.getDistance();
        Unit kmUnit   = ucar.visad.Util.parseUnit("km");
        Unit   feetUnit = ucar.visad.Util.parseUnit("feet");
        Real kmDistance = new Real(RealType.getRealType("distance", kmUnit),
                distance, kmUnit);

        return kmDistance.getValue(feetUnit);
    }

    /**
     * Convert points list of array of xyz or lat/lon/alt
     *
     *
     * @param points List of points. Either double array or EarthLocation
     * @param IDX_LAT idx
     * @param IDX_LON idx
     * @return lat/lons
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected float[][] getLatLons(List points, int IDX_LAT, int IDX_LON)
            throws VisADException, RemoteException {
        float[][] pts = new float[2][points.size()];

        for (int i = 0; i < points.size(); i++) {
            EarthLocation el = (EarthLocation) points.get(i);
            pts[IDX_LAT][i] =
                    (float) el.getLatLonPoint().getLatitude().getValue();
            pts[IDX_LON][i] =
                    (float) el.getLatLonPoint().getLongitude().getValue();
        }

        return pts;
    }

    public static FieldImpl getIsoSurfaceTopBottomAsPointObs(FieldImpl gridh, FieldImpl grid,  int level, float value)
            throws VisADException {
        if (grid == null) {
            return null;
        }
        FieldImpl grid0 = null;
        if(canSliceAtLevel(gridh, new Real(level))){
            grid0 = sliceAtLevel(gridh, new Real(level));
            grid0 = make2DGridFromSlice(grid0, false);
        } else {
            grid0 = make2DGridFromSlice(gridh, false);;
        }
        //RealType  index    = RealType.getRealType("index");
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        FieldImpl retField = null;
        //Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
        //        4);
        TupleType rangeType0 = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                RealType.getRealType("distance", kmUnit) });

        List<PointOb> pointObsList = new ArrayList<>();
        try {
            if (isTimeSequence(grid)) {
                SampledSet   timeSet      = (SampledSet) getTimeSet(grid);
                double[][]   times        = timeSet.getDoubles(false);
                Unit         timeUnit     = timeSet.getSetUnits()[0];
                if ( !timeUnit.equals(CommonUnit.secondsSinceTheEpoch)) {
                    Unit.convertTuple(
                            times, timeSet.getSetUnits(),
                            new Unit[] { CommonUnit.secondsSinceTheEpoch }, true);
                }
                Calendar cal = null;
                if (timeSet instanceof CalendarDateTimeSet) {
                    cal = ((CalendarDateTimeSet) timeSet).getCalendar();
                }
                for (int i = 0; i < timeSet.getLength(); i++) {
                    CalendarDateTime dt = new CalendarDateTime(times[0][i],
                            cal);
                    PointOb[] pointObs =
                            makePointObsForTopBottom((FlatField) grid0.getSample(i), (FlatField) grid.getSample(i), dt,value, FUNC_MIN);
                    if(pointObs != null && pointObs.length > 1) {
                        for(PointOb pob: pointObs)
                            pointObsList.add(pob);
                    }
                }
                int ssize = pointObsList.size();
                PointOb[] finalPointOb =  new PointOb[ssize];
                for(int i = 0; i < ssize; i++)
                    finalPointOb[i] = pointObsList.get(i);

                Integer1DSet points = new Integer1DSet(RealType.getRealType("index"),
                        ssize);
                FieldImpl ff = new FieldImpl(
                        new FunctionType(
                                ((SetType) points.getType()).getDomain(),
                                rangeType0), points);
                ff.setSamples(finalPointOb, false, false);
                return ff;
            } else {
                retField = makePointObs((FlatField) grid,
                        new DateTime(Double.NaN));
            }
        } catch (RemoteException re) {}
        return retField;
    }


    public static PointOb[] makePointObsForTopBottom(FlatField timehStep, FlatField timeStep, DateTime dt, float val, String function)
            throws VisADException, RemoteException {
        final boolean doMax = function.equals(FUNC_MAX);
        final boolean doMin = function.equals(FUNC_MIN);
        if (timeStep == null) {
            return null;
        }
        Unit   kmUnit   = ucar.visad.Util.parseUnit("km");
        SampledSet domain    = getSpatialDomain(timeStep);
        //int        numPoints = domain.getLength();
        SampledSet domain0    = getSpatialDomain(timehStep);
        int        numPoints0 = domain0.getLength();
        PointOb[] obs = new PointOb[3];
        TupleType tt = getParamType(timeStep);

        TupleType rangeType0 = new TupleType(new MathType[] {
                RealTupleType.LatitudeLongitudeAltitude,
                RealType.Time,
                RealType.getRealType("distance", kmUnit) });
        RealTupleType rtt =
                new RealTupleType(DataUtil.makeRealType("distance",
                        RealType.getRealType("distance", kmUnit).getDefaultUnit()));

        float[][] geoVals  = getEarthLocationPoints((GriddedSet) domain0);
        boolean   isLatLon = isLatLonOrder(domain);
        int       latIndex = isLatLon
                ? 0
                : 1;
        int       lonIndex = isLatLon
                ? 1
                : 0;
        boolean   haveAlt  = geoVals.length > 2;
        float   pMin   = Float.POSITIVE_INFINITY;
        float   pMax   = Float.NEGATIVE_INFINITY;
        int index = 0;
        // first find the center of max or min
        for (int i = 0; i < numPoints0; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];

            if ((lat == lat) && (lon == lon)) {
                if(doMax && (float)timehStep.getValues(i)[0] >= pMax){
                    pMax = (float)timehStep.getValues(i)[0];
                    index = i;
                } else if(doMin && (float)timehStep.getValues(i)[0] < pMin){
                    pMin = (float)timehStep.getValues(i)[0];
                    index = i;
                }
            }
        }
        float lat0 = geoVals[latIndex][index];
        float lon0 = geoVals[lonIndex][index];
        float alt0  = 0;
        //EarthLocation el0 = new EarthLocationLite(lat0, lon0, alt0);
        // TODO:  make this  more efficient
        //PointObTuple pot = new PointObTuple(el0, dt,
        //        timeStep.getSample(index), rangeType);
        //ff.setSample(0, pot, false, false);

        // now look for the surrounding points
        float deltaAlt = 100.0f; // meter
        float preVal = (float)timeStep.getValues(index)[0];

        float [] lln = findPointDisFromSFC(timeStep, val, preVal,
                lat0,   lon0, deltaAlt);
        EarthLocation el1 = new EarthLocationLite(lat0, lon0, lln[2]);
        double[] distance1 = new double[]{lln[2]};
        Data sample1 = new RealTuple(rtt, distance1);
        obs[1] = new PointObTuple(el1, dt, sample1, rangeType0);

        EarthLocation el2 = new EarthLocationLite(lat0, lon0, lln[3]);
        double[] distance2 = new double[]{lln[3]};
        Data sample2 = new RealTuple(rtt, distance2);
        obs[2] = new PointObTuple(el2, dt, sample2, rangeType0);

        double[] distance = new double[]{lln[0]};
        EarthLocation el0 = new EarthLocationLite(lat0, lon0, lln[1]);
        Data sample = new RealTuple(rtt, distance);
        PointObTuple pot = new PointObTuple(el0, dt, sample, rangeType0);
        obs[0] = pot;

        return obs;
    }

    public static float[] findPointDisFromSFC(FieldImpl timeStep, float val, float preVal,
                                                    float lat0, float lon0,
                                                    float deltaAlt) throws VisADException, RemoteException{
        float [] point = new float[4];

        // find max first
        float   pMax   = Float.NEGATIVE_INFINITY;
        float   alt0 = 0.0f;
        for(int i = 1; i< 1000; i++){
            float alt = alt0 + deltaAlt * i;
            EarthLocation el = new EarthLocationLite(lat0, lon0, alt);
            FieldImpl f1  = sample(timeStep, el,
                    DEFAULT_SAMPLING_MODE, DEFAULT_ERROR_MODE);
            if(f1.getFloats()[0][0] >= pMax && alt < 16000.0f) {
                pMax = f1.getFloats()[0][0];
                alt0 = alt;
            }
        }
        point[0] = pMax;
        point[1] = alt0;
        // bottom
        for(int i = 1; i< 1000; i++){
            float alt = alt0 - deltaAlt * i;
            EarthLocation el = new EarthLocationLite(lat0, lon0, alt);
            FieldImpl f1  = sample(timeStep, el,
                    DEFAULT_SAMPLING_MODE, DEFAULT_ERROR_MODE);
            if(f1.getFloats()[0][0] <= val && preVal >= val) {
                //double[] distance = new double[]{result.getDistance()};
                point[2] = alt;
                //ff.setSample(3, pot1, false, false);
                break;
            } else {
                preVal = f1.getFloats()[0][0];
            }
        }
        //top
        for(int i = 1; i< 1000; i++){
            float alt = alt0 + deltaAlt * i;
            EarthLocation el = new EarthLocationLite(lat0, lon0, alt);
            FieldImpl f1  = sample(timeStep, el,
                    DEFAULT_SAMPLING_MODE, DEFAULT_ERROR_MODE);
            if(f1.getFloats()[0][0] <= val && preVal >= val) {
                //double[] distance = new double[]{result.getDistance()};
                point[3] = alt;
                //ff.setSample(3, pot1, false, false);
                break;
            } else {
                preVal = f1.getFloats()[0][0];
            }
        }
        return point;
    }

}
