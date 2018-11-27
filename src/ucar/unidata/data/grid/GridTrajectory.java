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

package ucar.unidata.data.grid;


import ucar.unidata.data.DataUtil;
import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.util.LogUtil;
import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.LatLonPoint;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: 2/13/13
 * Time: 12:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class GridTrajectory {

    /**
     * Combine three Fields into one.  If the grids are on different
     * time domains, the second is resampled to the domain of the first.
     *
     * @param grid1  first grid.  This will be used for the time/space domain
     * @param grid2  second grid.
     * @param grid3  third grid.
     * @param grid4 _more_
     *
     * @return combined grid.
     *
     *
     * @throws Exception _more_
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public static List<FieldImpl> combineGridsArray(FieldImpl grid1,
            FieldImpl grid2, FieldImpl grid3, FieldImpl grid4)
            throws VisADException, RemoteException, Exception {

        ExecutorService   executor = Executors.newFixedThreadPool(4);


        Callable          pt       = new VarNClone(grid1);
        Future<FieldImpl> future1  = executor.submit(pt);

        Callable          pt1      = new VarNClone(grid2);
        Future<FieldImpl> future2  = executor.submit(pt1);

        Callable          pt2      = new VarNClone(grid3);
        Future<FieldImpl> future3  = executor.submit(pt2);

        Callable          pt3      = new VarNClone(grid4);
        Future<FieldImpl> future4  = executor.submit(pt3);

        FieldImpl         u        = future1.get();
        FieldImpl         v        = future2.get();
        FieldImpl         pw       = future3.get();
        FieldImpl         s        = future4.get();


        List              flist    = new ArrayList();
        flist.add(u);
        flist.add(v);
        flist.add(pw);
        flist.add(s);
        return flist;

    }

    /**
     * _more_
     *
     * @param grid1 _more_
     * @param grid2 _more_
     * @param grid3 _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static List<FieldImpl> combineGridsArray(FieldImpl grid1,
            FieldImpl grid2, FieldImpl grid3)
            throws VisADException, RemoteException, Exception {

        ExecutorService   executor = Executors.newFixedThreadPool(4);


        Callable          pt       = new VarNClone(grid1);
        Future<FieldImpl> future1  = executor.submit(pt);

        Callable          pt1      = new VarNClone(grid2);
        Future<FieldImpl> future2  = executor.submit(pt1);

        Callable          pt2      = new VarNClone(grid3);
        Future<FieldImpl> future3  = executor.submit(pt2);


        FieldImpl         u        = future1.get();
        FieldImpl         v        = future2.get();
        FieldImpl         s        = future3.get();


        List              flist    = new ArrayList();
        flist.add(u);
        flist.add(v);
        flist.add(s);
        return flist;

    }

    /**
     * _more_
     *
     * @param grid1 _more_
     * @param grid2 _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static List<FieldImpl> combineGridsArray(FieldImpl grid1,
            FieldImpl grid2)
            throws VisADException, RemoteException, Exception {

        ExecutorService   executor = Executors.newFixedThreadPool(4);


        Callable          pt       = new VarNClone(grid1);
        Future<FieldImpl> future1  = executor.submit(pt);

        Callable          pt1      = new VarNClone(grid2);
        Future<FieldImpl> future2  = executor.submit(pt1);


        FieldImpl         u        = future1.get();
        FieldImpl         v        = future2.get();


        List              flist    = new ArrayList();
        flist.add(u);
        flist.add(v);

        return flist;

    }

    /**
     * Class description
     *
     *
     * @version
     * @author
     */
    static class Varbar implements Callable<Object> {

        /** _more_ */
        private FieldImpl v;


        /**
         * _more_
         *
         * @param v _more_
         */
        private Varbar(FieldImpl v) {
            this.v = v;
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws CloneNotSupportedException _more_
         */
        public FieldImpl call() throws CloneNotSupportedException {
            return (FieldImpl) v.clone();

        }
    }

    /**
     * Class description
     *
     *
     * @version
     * @author
     */
    static class VarNClone implements Callable<Object> {

        /** _more_ */
        private FieldImpl v;


        /**
         * _more_
         *
         * @param v _more_
         */
        private VarNClone(FieldImpl v) {
            this.v = v;
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws CloneNotSupportedException _more_
         */
        public FieldImpl call() throws CloneNotSupportedException {
            return (FieldImpl) v;

        }
    }

    /**
     * Based on point data trajectory control, create all individual tracks and then merge them
     *
     *
     *
     *
     * @param uFI0 _more_
     * @param vFI0 _more_
     * @param sFI0 _more_
     * @param s0FI _more_
     * @return _more_
     *
     * @throws Exception _more_
     * @throws java.rmi.RemoteException _more_
     * @throws visad.VisADException _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static FieldImpl createTrajectoryGrid(final FieldImpl uFI0,
            final FieldImpl vFI0, final FieldImpl sFI0, final FieldImpl s0FI)
            throws VisADException, RemoteException, Exception {



        if (GridUtil.isVolume(uFI0) || GridUtil.isVolume(vFI0)
                || GridUtil.isVolume(sFI0)) {
            throw new IllegalArgumentException(
                "Grids U, V W and S can not be 3D volume");
        }

        long              start    = System.currentTimeMillis();
        ExecutorService   executor = Executors.newFixedThreadPool(4);


        Callable          pt       = new Varbar(uFI0);
        Future<FieldImpl> future   = executor.submit(pt);


        Callable          pt1      = new Varbar(vFI0);
        Future<FieldImpl> future1  = executor.submit(pt1);


        Callable          pt3      = new Varbar(sFI0);
        Future<FieldImpl> future3  = executor.submit(pt3);

        FieldImpl         uFI      = future.get();
        FieldImpl         vFI      = future1.get();
        FieldImpl         sFI      = future3.get();

        try {
            if (GridUtil.is3D(sFI0)) {
                sFI = GridUtil.make2DGridFromSlice(sFI, false);
            }
            if (GridUtil.is3D(uFI0)) {
                uFI = GridUtil.make2DGridFromSlice(uFI, false);
            }
            if (GridUtil.is3D(vFI0)) {
                vFI = GridUtil.make2DGridFromSlice(vFI, false);
            }
        } catch (Exception ee) {}

        //System.out.println("Time used to read = "
        //        + (System.currentTimeMillis() - start) / 1000.0);
        start = System.currentTimeMillis();



        final Set timeSet  = sFI.getDomainSet();
        int       numTimes = timeSet.getLength();
        Unit      timeUnit = timeSet.getSetUnits()[0];
        final Unit paramUnit =
            ((FlatField) sFI.getSample(0)).getRangeUnits()[0][0];
        FunctionType rt =
            (FunctionType) ((FlatField) sFI.getSample(0)).getType();
        final String paramName =
            rt.getFlatRange().getRealComponents()[0].getName();

        double[]   timeVals  = timeSet.getDoubles()[0];

        SampledSet domain0   = GridUtil.getSpatialDomain(s0FI);


        int        numPoints = domain0.getLength();


        float[][] geoVals =
            GridUtil.getEarthLocationPoints((GriddedSet) domain0);



        double[] ttts         = timeSet.getDoubles()[0];


        boolean  isLatLon     = GridUtil.isLatLonOrder(domain0);
        int      latIndex     = isLatLon
                                ? 0
                                : 1;
        int      lonIndex     = isLatLon
                                ? 1
                                : 0;
        boolean  haveAlt      = geoVals.length > 2;


        boolean  normalizeLon = true;

        //first step  init  u,v, w, and s at all initial points
        List<TrajInfo> tj = calculateTrackPoints(uFI, vFI, null, sFI, ttts,
                                geoVals, numPoints, numTimes, latIndex,
                                lonIndex, haveAlt, normalizeLon, null, false);


        int numParcels = numPoints;  //10;
        final FunctionType ft = new FunctionType(
                                    RealType.Generic,
                                    new FunctionType(
                                        RealTupleType.SpatialEarth3DTuple,
                                        RealType.getRealType(paramName)));

        List tracks;
        /*   for (int k = 0; k < numParcels; k++) {
               FlatField fft = createSingleTrack(paramName, lats[k], lons[k],
                                   alts[k], parcels[k], timeSet, ft, paramUnit);
               tracks.add(fft);
           }  */

        tracks = createTracks(paramName, tj, timeSet, ft, paramUnit,
                              numParcels);
        FlatField mergedTracks = DerivedGridFactory.mergeTracks(tracks);

        FunctionType fiType = new FunctionType(RealType.Time,
                                  mergedTracks.getType());

        DateTime endTime = new DateTime(timeVals[numTimes - 1], timeUnit);

        FieldImpl fi =
            new FieldImpl(fiType,
                          new SingletonSet(new RealTuple(new Real[] {
                              endTime })));
        fi.setSample(0, mergedTracks, false);
        //System.out.println("Time used to compute = "
        //                   + (System.currentTimeMillis() - start) / 1000.0);
        return fi;



    }

    /**
     * Based on point data trajectory control, create all individual tracks and then merge them
     *
     * @param uFI0 _more_
     * @param vFI0 _more_
     * @param pwFI0 _more_
     * @param sFI0 _more_
     * @param s0FI _more_
     * @return _more_
     *
     * @throws Exception _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static FieldImpl createTrajectoryGrid(final FieldImpl uFI0,
            final FieldImpl vFI0, final FieldImpl pwFI0,
            final FieldImpl sFI0, final FieldImpl s0FI)
            throws VisADException, RemoteException, Exception {

        //System.out.println("Time Start...\n");
        long              start    = System.currentTimeMillis();
        ExecutorService   executor = Executors.newFixedThreadPool(4);


        Callable          pt       = new Varbar(uFI0);
        Future<FieldImpl> future   = executor.submit(pt);


        Callable          pt1      = new Varbar(vFI0);
        Future<FieldImpl> future1  = executor.submit(pt1);


        Callable          pt2      = new Varbar(pwFI0);
        Future<FieldImpl> future2  = executor.submit(pt2);


        Callable          pt3      = new Varbar(sFI0);
        Future<FieldImpl> future3  = executor.submit(pt3);

        FieldImpl         uFI      = future.get();
        FieldImpl         vFI      = future1.get();
        FieldImpl         pwFI     = future2.get();
        FieldImpl         sFI      = future3.get();

        //System.out.println("Time used to read = "
        //        + (System.currentTimeMillis() - start) / 1000.0);
        start = System.currentTimeMillis();
        /*
        FieldImpl         uFI       = (FieldImpl)uFI0.clone(); //future.get();
        FieldImpl         vFI       = (FieldImpl)vFI0.clone(); //future1.get();
        FieldImpl         pwFI      = (FieldImpl)pwFI0.clone(); //future2.get();
        FieldImpl         sFI       = (FieldImpl)sFI0.clone(); //future3.get();
         */


        final Set timeSet  = sFI.getDomainSet();
        int       numTimes = timeSet.getLength();
        Unit      timeUnit = timeSet.getSetUnits()[0];
        final Unit paramUnit =
            ((FlatField) sFI.getSample(0)).getRangeUnits()[0][0];
        FunctionType rt =
            (FunctionType) ((FlatField) sFI.getSample(0)).getType();
        final String paramName =
            rt.getFlatRange().getRealComponents()[0].getName();

        double[]   timeVals = timeSet.getDoubles()[0];

        SampledSet domain0  = GridUtil.getSpatialDomain(s0FI);
        final Unit rgUnit =
            ((FlatField) pwFI.getSample(0)).getRangeUnits()[0][0];
        FieldImpl wFI;
        if (Unit.canConvert(rgUnit, CommonUnits.METERS_PER_SECOND)) {
            wFI = pwFI;
        } else {
            FieldImpl pFI = DerivedGridFactory.createPressureGridFromDomain(
                                (FlatField) pwFI.getSample(0));
            FieldImpl hPI = DerivedGridFactory.convertPressureToHeight(pFI);
            wFI = DerivedGridFactory.convertPressureVelocityToHeightVelocity(
                pwFI, hPI, null);
        }

        int numPoints = domain0.getLength();


        float[][] geoVals =
            GridUtil.getEarthLocationPoints((GriddedSet) domain0);



        double[] ttts         = timeSet.getDoubles()[0];


        boolean  isLatLon     = GridUtil.isLatLonOrder(domain0);
        int      latIndex     = isLatLon
                                ? 0
                                : 1;
        int      lonIndex     = isLatLon
                                ? 1
                                : 0;
        boolean  haveAlt      = geoVals.length > 2;


        boolean  normalizeLon = true;
        //  start = System.currentTimeMillis();
        //first step  init  u,v, w, and s at all initial points
        List<TrajInfo> tj = calculateTrackPoints(uFI, vFI, wFI, sFI, ttts,
                                geoVals, numPoints, numTimes, latIndex,
                                lonIndex, haveAlt, normalizeLon, null, false);

        // System.out.println("Time used to compute = "
        //                   + (System.currentTimeMillis() - start) / 1000.0);
        // now cal every point at each following time step

        /*
        // create FI of traj based on SSEC  NearTraj control
        int          numParcels = numPoints;  //10;
        FunctionType ft         = new FunctionType(
                              RealType.Generic,
                              new FunctionType(
                                  RealTupleType.SpatialEarth3DTuple,
                                  RealType.getRealType("paramName")));
        FieldImpl trajField = new FieldImpl(ft, new Integer1DSet(numParcels));
        FieldImpl trajTimeField =
            new FieldImpl(new FunctionType(RealType.Time,
                                           trajField.getType()), timeSet);

        for (int t = 0; t < numTimes; t++) {

            trajField = new FieldImpl(ft, new Integer1DSet(numParcels));
            for (int k = 0; k < numParcels; k++) {
                float[]   latsK    = subArray(lats[k], 0, t + 1, 0);
                float[]   lonsK    = subArray(lons[k], 0, t + 1, 0);
                float[]   altsK    = subArray(alts[k], 0, t + 1, 0);
                double[]  parcelsK = subArray(parcels[k], 0, t + 1, 0);

                FlatField fld = createSingleTraj("paramName", latsK, lonsK,
                                    altsK, parcelsK);
                trajField.setSample(k, fld);
            }

            trajTimeField.setSample(t, trajField);
        }
         */
        // create FI

        int numParcels = numPoints;  //10;
        final FunctionType ft = new FunctionType(
                                    RealType.Generic,
                                    new FunctionType(
                                        RealTupleType.SpatialEarth3DTuple,
                                        RealType.getRealType(paramName)));

        List tracks;
        /*   for (int k = 0; k < numParcels; k++) {
               FlatField fft = createSingleTrack(paramName, lats[k], lons[k],
                                   alts[k], parcels[k], timeSet, ft, paramUnit);
               tracks.add(fft);
           }  */

        tracks = createTracks(paramName, tj, timeSet, ft, paramUnit,
                              numParcels);
        //  System.out.println("Time used to compute = "
        //                     + (System.currentTimeMillis() - start) / 1000.0);
        FlatField mergedTracks = DerivedGridFactory.mergeTracks(tracks);

        FunctionType fiType = new FunctionType(RealType.Time,
                                  mergedTracks.getType());

        DateTime endTime = new DateTime(timeVals[numTimes - 1], timeUnit);

        FieldImpl fi =
            new FieldImpl(fiType,
                          new SingletonSet(new RealTuple(new Real[] {
                              endTime })));
        fi.setSample(0, mergedTracks, false);
        //System.out.println("Time used to compute = "
        //                   + (System.currentTimeMillis() - start) / 1000.0);
        return fi;



    }
    /**
     * _more_
     *
     * @param paramName _more_
     * @param lats _more_
     * @param lons _more_
     * @param alts _more_
     * @param param _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FlatField createSingleTraj(String paramName, float[] lats,
                                             float[] lons, float[] alts,
                                             double[] param)
            throws Exception {
        float trajCoords[][] = {
            lons, lats, alts
        };
        Gridded3DSet domain =
            new Gridded3DSet(RealTupleType.SpatialEarth3DTuple, trajCoords,
                             trajCoords[0].length);
        FunctionType fncType =
            new FunctionType(RealTupleType.SpatialEarth3DTuple,
                             RealType.getRealType(paramName));
        FlatField traj = new FlatField(fncType, domain);
        traj.setSamples(new double[][] {
            param
        }, false);
        return traj;
    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Fri, Mar 2, '12
     * @author         Enter your name here...
     */
    public static class TrajInfo {

        /** _more_ */
        float[] lats;

        /** _more_ */
        float[] lons;

        /** _more_ */
        float[] alts;

        /** _more_ */
        double[] parcels;

        /**
         * _more_
         *
         * @param numTimes _more_
         */
        public TrajInfo(int numTimes) {
            lats    = new float[numTimes];
            lons    = new float[numTimes];
            alts    = new float[numTimes];
            parcels = new double[numTimes];
        }

    }



    /**
     * _more_
     *
     * @param uFI0 _more_
     * @param vFI0 _more_
     * @param wFI0 _more_
     * @param sFI0 _more_
     * @param ttts _more_
     * @param geoVals _more_
     * @param numPoints _more_
     * @param numTimes _more_
     * @param latIndex _more_
     * @param lonIndex _more_
     * @param haveAlt _more_
     * @param normalizeLon _more_
     * @param clevel _more_
     * @param backward _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<TrajInfo> calculateTrackPoints(final FieldImpl uFI0,
            final FieldImpl vFI0, final FieldImpl wFI0, final FieldImpl sFI0,
            final double[] ttts, final float[][] geoVals,
            final int numPoints, final int numTimes, final int latIndex,
            final int lonIndex, final boolean haveAlt,
            final boolean normalizeLon, final Real clevel, boolean backward)
            throws Exception {

        ExecutorService   executor = Executors.newFixedThreadPool(8);

        Callable          pt0      = new Varbar(uFI0);
        Future<FieldImpl> future0  = executor.submit(pt0);


        Callable          pt1      = new Varbar(vFI0);
        Future<FieldImpl> future1  = executor.submit(pt1);

        Callable          pt2;
        Future<FieldImpl> future2 = null;
        if (wFI0 != null) {
            pt2     = new Varbar(wFI0);
            future2 = executor.submit(pt2);
        }

        Callable          pt3     = new Varbar(sFI0);
        Future<FieldImpl> future3 = executor.submit(pt3);

        FieldImpl         uFI     = future0.get();
        FieldImpl         vFI     = future1.get();
        FieldImpl         wFI     = null;
        if (wFI0 != null) {
            wFI = future2.get();
        }
        FieldImpl sFI  = future3.get();

        FieldImpl uvFI = DerivedGridFactory.createTrueFlowVectors(uFI, vFI);
        uFI = DerivedGridFactory.getUComponent(uvFI);
        vFI = DerivedGridFactory.getVComponent(uvFI);
        if ((wFI0 == null) && (clevel != null) && (haveAlt)) {
            uFI = GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(uFI,
                    clevel));
            vFI = GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(vFI,
                    clevel));
            SampledSet domain0 = GridUtil.getSpatialDomain(sFI);
            if (domain0.getManifoldDimension() == 3) {  // in case the s field is already subset
                sFI = GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(sFI,
                        clevel));
            } else {
                sFI = GridUtil.make2DGridFromSlice(sFI, false);
            }
        } else if ( !haveAlt) {
            uFI = GridUtil.make2DGridFromSlice(uFI, false);
            vFI = GridUtil.make2DGridFromSlice(vFI, false);
            sFI = GridUtil.make2DGridFromSlice(sFI, false);
        }
        LogUtil.message("Calculating grid trajectories...");
        final List<TrajInfo> result   = new ArrayList<TrajInfo>();
        List<Future>         pthreads = new ArrayList<Future>();
        for (int i = 0; i < numPoints; i++) {
            float lat = geoVals[latIndex][i];
            float lon = geoVals[lonIndex][i];
            if (normalizeLon) {
                lon = (float) LatLonPointImpl.lonNormal(lon);
            }
            float alt = haveAlt
                        ? geoVals[2][i]
                        : 0;
            Callable pt = new pointsThredds(uFI, vFI, wFI, sFI, ttts, lat,
                                            lon, alt, numTimes, backward);
            Future<Object> future = executor.submit(pt);
            pthreads.add(future);
        }

        for (Future<Object> o : pthreads) {
            try {
                result.add((TrajInfo) o.get());
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ExecutionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return result;
    }

    /**
     * Class description
     *
     *
     * @version
     * @author
     */
    static class pointsThredds implements Callable<TrajInfo> {

        /** _more_ */
        FieldImpl uFI;

        /** _more_ */
        FieldImpl vFI;

        /** _more_ */
        FieldImpl wFI;

        /** _more_ */
        FieldImpl sFI;

        /** _more_ */
        double[] ttts;

        /** _more_ */
        float lat;

        /** _more_ */
        float lon;

        /** _more_ */
        float alt;

        /** _more_ */
        int numTimes;

        /** _more_ */
        int latIndex;

        /** _more_ */
        int lonIndex;

        /** _more_ */
        boolean haveAlt;

        /** _more_ */
        boolean normalizeLon;

        /** _more_ */
        boolean backward;

        /**
         * _more_
         *
         * @param uFI _more_
         * @param vFI _more_
         * @param wFI _more_
         * @param sFI _more_
         * @param ttts _more_
         * @param lat _more_
         * @param lon _more_
         * @param alt _more_
         * @param numTimes _more_
         * @param backward _more_
         *
         */
        private pointsThredds(FieldImpl uFI, FieldImpl vFI, FieldImpl wFI,
                              FieldImpl sFI, double[] ttts, float lat,
                              float lon, float alt, int numTimes,
                              boolean backward) {

            this.uFI      = uFI;
            this.vFI      = vFI;
            this.wFI      = wFI;
            this.sFI      = sFI;
            this.ttts     = ttts;
            this.lat      = lat;
            this.lon      = lon;
            this.alt      = alt;
            this.numTimes = numTimes;
            this.backward = backward;

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public TrajInfo call() {
            if ( !backward) {
                if (wFI != null) {
                    return calculateSingleTrackPoints(uFI, vFI, wFI, sFI,
                            ttts, lat, lon, alt, numTimes);
                } else {
                    return calculateSingleTrackPoints(uFI, vFI, sFI, ttts,
                            lat, lon, alt, numTimes);
                }
            } else {
                if (wFI != null) {
                    return calculateSingleTrackPointsB(uFI, vFI, wFI, sFI,
                            ttts, lat, lon, alt, numTimes);
                } else {
                    return calculateSingleTrackPointsB(uFI, vFI, sFI, ttts,
                            lat, lon, alt, numTimes);
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param uFI _more_
     * @param vFI _more_
     * @param sFI _more_
     * @param ttts _more_
     * @param lat0 _more_
     * @param lon0 _more_
     * @param alt0 _more_
     * @param numTimes _more_
     *
     *
     * @return _more_
     *
     */
    public static TrajInfo calculateSingleTrackPoints(final FieldImpl uFI,
            final FieldImpl vFI, final FieldImpl sFI, final double[] ttts,
            final float lat0, final float lon0, final float alt0,
            final int numTimes) {

        final float radius   = 6371000.0f;
        final float f        = 180.0f / 3.1415926f;
        TrajInfo    trajInfo = new TrajInfo(numTimes);


        try {

            float[] u = new float[numTimes];
            float[] v = new float[numTimes];


            for (int timeStepIdx = 0; timeStepIdx < numTimes; timeStepIdx++) {
                if (timeStepIdx == 0) {

                    trajInfo.lats[timeStepIdx] = lat0;

                    trajInfo.lons[timeStepIdx] = lon0;
                    trajInfo.alts[timeStepIdx] = alt0;

                    EarthLocation el = new EarthLocationLite(lat0, lon0,
                                           alt0);
                    LatLonPoint llp = el.getLatLonPoint();

                    FieldImpl ssample = GridUtil.sample(sFI, llp,
                                            Data.NEAREST_NEIGHBOR);
                    Data srt   = ssample.getSample(0);
                    Real sreal = (srt instanceof RealTuple)
                                 ? (Real) ((RealTuple) srt).getComponent(0)
                                 : (Real) srt;
                    trajInfo.parcels[timeStepIdx] = (float) sreal.getValue();


                    FieldImpl usample = GridUtil.sample(uFI, llp,
                                            Data.NEAREST_NEIGHBOR);
                    Data urt   = usample.getSample(0);
                    Real ureal = (urt instanceof RealTuple)
                                 ? (Real) ((RealTuple) urt).getComponent(0)
                                 : (Real) urt;
                    u[timeStepIdx] = (float) ureal.getValue();

                    FieldImpl vsample = GridUtil.sample(vFI, llp,
                                            Data.NEAREST_NEIGHBOR);
                    Data vrt   = vsample.getSample(0);
                    Real vreal = (vrt instanceof RealTuple)
                                 ? (Real) ((RealTuple) vrt).getComponent(0)
                                 : (Real) vrt;
                    v[timeStepIdx] = (float) vreal.getValue();



                } else {
                    double delt = ttts[timeStepIdx] - ttts[timeStepIdx - 1];

                    float  lat  = trajInfo.lats[timeStepIdx - 1];
                    float  lon  = trajInfo.lons[timeStepIdx - 1];
                    float  alt  = trajInfo.alts[timeStepIdx - 1];

                    float lat1 = (float) (lat
                                          + f * (v[timeStepIdx - 1] * delt)
                                            / (radius + alt));
                    float lon1 = (float) (lon
                                          + f * (u[timeStepIdx - 1] * delt)
                                            / (radius + alt));
                    float         alt1 = alt;

                    EarthLocation el = new EarthLocationLite(lat1, lon1,
                                           alt1);
                    LatLonPoint llp = el.getLatLonPoint();
                    //GridUtil.sampleAtPoint for u, v, w
                    FieldImpl usample = GridUtil.sample(uFI, llp,
                                            Data.NEAREST_NEIGHBOR);
                    FieldImpl vsample = GridUtil.sample(vFI, llp,
                                            Data.NEAREST_NEIGHBOR);

                    FieldImpl ssample = GridUtil.sample(sFI, llp,
                                            Data.NEAREST_NEIGHBOR);

                    if ((usample == null) && (vsample == null)
                            && (ssample == null)) {
                        //outside the domain
                        trajInfo.lats[timeStepIdx] =
                            trajInfo.lats[timeStepIdx - 1];
                        trajInfo.lons[timeStepIdx] =
                            trajInfo.lons[timeStepIdx - 1];
                        trajInfo.alts[timeStepIdx] =
                            trajInfo.alts[timeStepIdx - 1];
                        trajInfo.parcels[timeStepIdx] =
                            trajInfo.parcels[timeStepIdx - 1];
                    } else {
                        trajInfo.lats[timeStepIdx] = lat1;
                        trajInfo.lons[timeStepIdx] = lon1;
                        trajInfo.alts[timeStepIdx] = alt1;

                        Data urt   = usample.getSample(timeStepIdx);
                        Real ureal = (urt instanceof RealTuple)
                                     ? (Real) ((RealTuple) urt).getComponent(
                                         0)
                                     : (Real) urt;
                        u[timeStepIdx] = (float) ureal.getValue();


                        Data vrt   = vsample.getSample(timeStepIdx);
                        Real vreal = (vrt instanceof RealTuple)
                                     ? (Real) ((RealTuple) vrt).getComponent(
                                         0)
                                     : (Real) vrt;
                        v[timeStepIdx] = (float) vreal.getValue();


                        Data srt   = ssample.getSample(timeStepIdx);
                        Real sreal = (srt instanceof RealTuple)
                                     ? (Real) ((RealTuple) srt).getComponent(
                                         0)
                                     : (Real) srt;
                        trajInfo.parcels[timeStepIdx] =
                            (float) sreal.getValue();

                    }

                }

            }


        } catch (Exception ee) {}


        return trajInfo;
    }



    /**
     * _more_
     *
     * @param uFI _more_
     * @param vFI _more_
     * @param sFI _more_
     * @param ttts _more_
     * @param lat0 _more_
     * @param lon0 _more_
     * @param alt0 _more_
     * @param numTimes _more_
     *
     * @return _more_
     */
    public static TrajInfo calculateSingleTrackPointsB(final FieldImpl uFI,
            final FieldImpl vFI, final FieldImpl sFI, final double[] ttts,
            final float lat0, final float lon0, final float alt0,
            final int numTimes) {

        final float radius   = 6371000.0f;
        final float f        = 180.0f / 3.1415926f;
        TrajInfo    trajInfo = new TrajInfo(numTimes);


        try {

            float[] u = new float[numTimes];
            float[] v = new float[numTimes];


            for (int timeStepIdx = numTimes - 1; timeStepIdx > -1;
                    timeStepIdx--) {
                if (timeStepIdx == numTimes - 1) {

                    trajInfo.lats[timeStepIdx] = lat0;

                    trajInfo.lons[timeStepIdx] = lon0;
                    trajInfo.alts[timeStepIdx] = alt0;

                    EarthLocation el = new EarthLocationLite(lat0, lon0,
                                           alt0);
                    LatLonPoint llp = el.getLatLonPoint();

                    FieldImpl ssample = GridUtil.sample(sFI, llp,
                                            Data.NEAREST_NEIGHBOR);
                    Data srt   = ssample.getSample(timeStepIdx);
                    Real sreal = (srt instanceof RealTuple)
                                 ? (Real) ((RealTuple) srt).getComponent(0)
                                 : (Real) srt;
                    trajInfo.parcels[timeStepIdx] = (float) sreal.getValue();


                    FieldImpl usample = GridUtil.sample(uFI, llp,
                                            Data.NEAREST_NEIGHBOR);
                    Data urt   = usample.getSample(timeStepIdx);
                    Real ureal = (urt instanceof RealTuple)
                                 ? (Real) ((RealTuple) urt).getComponent(0)
                                 : (Real) urt;
                    u[timeStepIdx] = (float) ureal.getValue();

                    FieldImpl vsample = GridUtil.sample(vFI, llp,
                                            Data.NEAREST_NEIGHBOR);
                    Data vrt   = vsample.getSample(timeStepIdx);
                    Real vreal = (vrt instanceof RealTuple)
                                 ? (Real) ((RealTuple) vrt).getComponent(0)
                                 : (Real) vrt;
                    v[timeStepIdx] = (float) vreal.getValue();



                } else {
                    double delt = ttts[timeStepIdx] - ttts[timeStepIdx + 1];

                    float  lat  = trajInfo.lats[timeStepIdx + 1];
                    float  lon  = trajInfo.lons[timeStepIdx + 1];
                    float  alt  = trajInfo.alts[timeStepIdx + 1];


                    float lat1 = (float) (lat
                                          + f * (v[timeStepIdx + 1]
                                                  * delt) / (radius + alt));
                    float lon1 = (float) (lon
                                          + f * (u[timeStepIdx + 1]
                                                  * delt) / (radius + alt));
                    float         alt1 = alt;

                    EarthLocation el = new EarthLocationLite(lat1, lon1,
                                           alt1);
                    LatLonPoint llp = el.getLatLonPoint();
                    //GridUtil.sampleAtPoint for u, v, w
                    FieldImpl usample = GridUtil.sample(uFI, llp,
                                            Data.NEAREST_NEIGHBOR);
                    FieldImpl vsample = GridUtil.sample(vFI, llp,
                                            Data.NEAREST_NEIGHBOR);

                    FieldImpl ssample = GridUtil.sample(sFI, llp,
                                            Data.NEAREST_NEIGHBOR);

                    if ((usample == null) && (vsample == null)
                            && (ssample == null)) {
                        //outside the domain
                        trajInfo.lats[timeStepIdx] =
                            trajInfo.lats[timeStepIdx + 1];
                        trajInfo.lons[timeStepIdx] =
                            trajInfo.lons[timeStepIdx + 1];
                        trajInfo.alts[timeStepIdx] =
                            trajInfo.alts[timeStepIdx + 1];
                        trajInfo.parcels[timeStepIdx] =
                            trajInfo.parcels[timeStepIdx + 1];
                    } else {
                        trajInfo.lats[timeStepIdx] = lat1;
                        trajInfo.lons[timeStepIdx] = lon1;
                        trajInfo.alts[timeStepIdx] = alt1;

                        Data urt   = usample.getSample(timeStepIdx);
                        Real ureal = (urt instanceof RealTuple)
                                     ? (Real) ((RealTuple) urt).getComponent(
                                         0)
                                     : (Real) urt;
                        u[timeStepIdx] = (float) ureal.getValue();


                        Data vrt   = vsample.getSample(timeStepIdx);
                        Real vreal = (vrt instanceof RealTuple)
                                     ? (Real) ((RealTuple) vrt).getComponent(
                                         0)
                                     : (Real) vrt;
                        v[timeStepIdx] = (float) vreal.getValue();


                        Data srt   = ssample.getSample(timeStepIdx);
                        Real sreal = (srt instanceof RealTuple)
                                     ? (Real) ((RealTuple) srt).getComponent(
                                         0)
                                     : (Real) srt;
                        trajInfo.parcels[timeStepIdx] =
                            (float) sreal.getValue();

                    }

                }

            }


        } catch (Exception ee) {}


        return trajInfo;
    }

    /**
     * _more_
     *
     * @param uFI _more_
     * @param vFI _more_
     * @param wFI _more_
     * @param sFI _more_
     * @param ttts _more_
     * @param lat0 _more_
     * @param lon0 _more_
     * @param alt0 _more_
     * @param numTimes _more_
     *
     *
     * @return _more_
     *
     */
    public static TrajInfo calculateSingleTrackPoints(final FieldImpl uFI,
            final FieldImpl vFI, final FieldImpl wFI, final FieldImpl sFI,
            final double[] ttts, final float lat0, final float lon0,
            final float alt0, final int numTimes) {

        final float radius   = 6371000.0f;
        final float f        = 180.0f / 3.1415926f;
        TrajInfo    trajInfo = new TrajInfo(numTimes);

        try {

            float[] u = new float[numTimes];
            float[] v = new float[numTimes];
            float[] w = new float[numTimes];

            for (int timeStepIdx = 0; timeStepIdx < numTimes; timeStepIdx++) {
                if (timeStepIdx == 0) {

                    trajInfo.lats[timeStepIdx] = lat0;

                    trajInfo.lons[timeStepIdx] = lon0;
                    trajInfo.alts[timeStepIdx] = alt0;

                    EarthLocation el = new EarthLocationLite(lat0, lon0,
                                           alt0);
                    FieldImpl ssample = GridUtil.sample(sFI, el);
                    Data      srt     = ssample.getSample(0);
                    Real      sreal   = (srt instanceof RealTuple)
                                        ? (Real) ((RealTuple) srt)
                                            .getComponent(0)
                                        : (Real) srt;
                    trajInfo.parcels[timeStepIdx] = (float) sreal.getValue();


                    FieldImpl usample = GridUtil.sample(uFI, el);
                    Data      urt     = usample.getSample(0);
                    Real      ureal   = (urt instanceof RealTuple)
                                        ? (Real) ((RealTuple) urt)
                                            .getComponent(0)
                                        : (Real) urt;
                    u[timeStepIdx] = (float) ureal.getValue();

                    FieldImpl vsample = GridUtil.sample(vFI, el);
                    Data      vrt     = vsample.getSample(0);
                    Real      vreal   = (vrt instanceof RealTuple)
                                        ? (Real) ((RealTuple) vrt)
                                            .getComponent(0)
                                        : (Real) vrt;
                    v[timeStepIdx] = (float) vreal.getValue();

                    FieldImpl wsample = GridUtil.sample(wFI, el);
                    Data      wrt     = wsample.getSample(0);
                    Real      wreal   = (wrt instanceof RealTuple)
                                        ? (Real) ((RealTuple) wrt)
                                            .getComponent(0)
                                        : (Real) wrt;
                    if (wreal.isMissing()) {
                        w[timeStepIdx] = 0;
                    } else {
                        w[timeStepIdx] = (float) wreal.getValue();
                    }

                } else {
                    double delt = ttts[timeStepIdx] - ttts[timeStepIdx - 1];

                    float  lat  = trajInfo.lats[timeStepIdx - 1];
                    float  lon  = trajInfo.lons[timeStepIdx - 1];
                    float  alt  = trajInfo.alts[timeStepIdx - 1];

                    float lat1 = (float) (lat
                                          + f * (v[timeStepIdx - 1] * delt)
                                            / (radius + alt));
                    float lon1 = (float) (lon
                                          + f * (u[timeStepIdx - 1] * delt)
                                            / (radius + alt));
                    float alt1 = (float) (alt + (w[timeStepIdx - 1] * delt));
                    if (alt1 < 0) {
                        alt1 = 0;
                    }
                    EarthLocation el = new EarthLocationLite(lat1, lon1,
                                           alt1);

                    //GridUtil.sampleAtPoint for u, v, w
                    FieldImpl usample = GridUtil.sample(uFI, el);
                    FieldImpl vsample = GridUtil.sample(vFI, el);
                    FieldImpl wsample = GridUtil.sample(wFI, el);
                    FieldImpl ssample = GridUtil.sample(sFI, el);

                    if ((usample == null) && (vsample == null)
                            && (wsample == null) && (ssample == null)) {
                        //outside the domain
                        trajInfo.lats[timeStepIdx] =
                            trajInfo.lats[timeStepIdx - 1];
                        trajInfo.lons[timeStepIdx] =
                            trajInfo.lons[timeStepIdx - 1];
                        trajInfo.alts[timeStepIdx] =
                            trajInfo.alts[timeStepIdx - 1];
                        trajInfo.parcels[timeStepIdx] =
                            trajInfo.parcels[timeStepIdx - 1];
                    } else {
                        trajInfo.lats[timeStepIdx] = lat1;
                        trajInfo.lons[timeStepIdx] = lon1;
                        trajInfo.alts[timeStepIdx] = alt1;

                        Data urt   = usample.getSample(timeStepIdx);
                        Real ureal = (urt instanceof RealTuple)
                                     ? (Real) ((RealTuple) urt).getComponent(
                                         0)
                                     : (Real) urt;
                        u[timeStepIdx] = (float) ureal.getValue();


                        Data vrt   = vsample.getSample(timeStepIdx);
                        Real vreal = (vrt instanceof RealTuple)
                                     ? (Real) ((RealTuple) vrt).getComponent(
                                         0)
                                     : (Real) vrt;
                        v[timeStepIdx] = (float) vreal.getValue();


                        Data wrt   = wsample.getSample(timeStepIdx);
                        Real wreal = (wrt instanceof RealTuple)
                                     ? (Real) ((RealTuple) wrt).getComponent(
                                         0)
                                     : (Real) wrt;
                        if (wreal.isMissing()) {
                            w[timeStepIdx] = 0;
                        } else {
                            w[timeStepIdx] = (float) wreal.getValue();
                        }

                        Data srt   = ssample.getSample(timeStepIdx);
                        Real sreal = (srt instanceof RealTuple)
                                     ? (Real) ((RealTuple) srt).getComponent(
                                         0)
                                     : (Real) srt;
                        trajInfo.parcels[timeStepIdx] =
                            (float) sreal.getValue();

                    }

                }

            }


        } catch (Exception ee) {}


        return trajInfo;
    }


    /**
     * _more_
     *
     * @param uFI _more_
     * @param vFI _more_
     * @param wFI _more_
     * @param sFI _more_
     * @param ttts _more_
     * @param lat0 _more_
     * @param lon0 _more_
     * @param alt0 _more_
     * @param numTimes _more_
     *
     * @return _more_
     */
    public static TrajInfo calculateSingleTrackPointsB(final FieldImpl uFI,
            final FieldImpl vFI, final FieldImpl wFI, final FieldImpl sFI,
            final double[] ttts, final float lat0, final float lon0,
            final float alt0, final int numTimes) {

        final float radius   = 6371000.0f;
        final float f        = 180.0f / 3.1415926f;
        TrajInfo    trajInfo = new TrajInfo(numTimes);

        try {

            float[] u = new float[numTimes];
            float[] v = new float[numTimes];
            float[] w = new float[numTimes];

            for (int timeStepIdx = numTimes - 1; timeStepIdx > -1;
                    timeStepIdx--) {
                if (timeStepIdx == numTimes - 1) {

                    trajInfo.lats[timeStepIdx] = lat0;

                    trajInfo.lons[timeStepIdx] = lon0;
                    trajInfo.alts[timeStepIdx] = alt0;

                    EarthLocation el = new EarthLocationLite(lat0, lon0,
                                           alt0);
                    FieldImpl ssample = GridUtil.sample(sFI, el);
                    Data      srt     = ssample.getSample(timeStepIdx);
                    Real      sreal   = (srt instanceof RealTuple)
                                        ? (Real) ((RealTuple) srt)
                                            .getComponent(0)
                                        : (Real) srt;
                    trajInfo.parcels[timeStepIdx] = (float) sreal.getValue();


                    FieldImpl usample = GridUtil.sample(uFI, el);
                    Data      urt     = usample.getSample(timeStepIdx);
                    Real      ureal   = (urt instanceof RealTuple)
                                        ? (Real) ((RealTuple) urt)
                                            .getComponent(0)
                                        : (Real) urt;
                    u[timeStepIdx] = (float) ureal.getValue();

                    FieldImpl vsample = GridUtil.sample(vFI, el);
                    Data      vrt     = vsample.getSample(timeStepIdx);
                    Real      vreal   = (vrt instanceof RealTuple)
                                        ? (Real) ((RealTuple) vrt)
                                            .getComponent(0)
                                        : (Real) vrt;
                    v[timeStepIdx] = (float) vreal.getValue();

                    FieldImpl wsample = GridUtil.sample(wFI, el);
                    Data      wrt     = wsample.getSample(timeStepIdx);
                    Real      wreal   = (wrt instanceof RealTuple)
                                        ? (Real) ((RealTuple) wrt)
                                            .getComponent(0)
                                        : (Real) wrt;
                    if (wreal.isMissing()) {
                        w[timeStepIdx] = 0;
                    } else {
                        w[timeStepIdx] = (float) wreal.getValue();
                    }

                } else {
                    double delt = ttts[timeStepIdx] - ttts[timeStepIdx + 1];  //negative for backword

                    float lat = trajInfo.lats[timeStepIdx + 1];
                    float lon = trajInfo.lons[timeStepIdx + 1];
                    float alt = trajInfo.alts[timeStepIdx + 1];

                 /*   float lat1 = (float) (lat
                                          + f * ((v[timeStepIdx + 1]
                                              + v[timeStepIdx]) * 0.5
                                                  * delt) / (radius + alt));
                    float lon1 = (float) (lon
                                          + f * ((u[timeStepIdx + 1]
                                              + u[timeStepIdx]) * 0.5
                                                  * delt) / (radius + alt));
                    float alt1 = (float) (alt
                                          + ((w[timeStepIdx + 1]
                                              + w[timeStepIdx]) * 0.5
                                                  * delt));   */
                    float lat1 = (float) (lat
                            + f * (v[timeStepIdx + 1]
                            * delt) / (radius + alt));
                    float lon1 = (float) (lon
                            + f * (u[timeStepIdx + 1]
                            * delt) / (radius + alt));
                    float alt1 = (float) (alt
                            + (w[timeStepIdx + 1]
                            * delt));
                    if (alt1 < 0) {
                        alt1 = 0;
                    }
                    EarthLocation el = new EarthLocationLite(lat1, lon1,
                                           alt1);

                    //GridUtil.sampleAtPoint for u, v, w
                    FieldImpl usample = GridUtil.sample(uFI, el);
                    FieldImpl vsample = GridUtil.sample(vFI, el);
                    FieldImpl wsample = GridUtil.sample(wFI, el);
                    FieldImpl ssample = GridUtil.sample(sFI, el);

                    if ((usample == null) && (vsample == null)
                            && (wsample == null) && (ssample == null)) {
                        //outside the domain
                        trajInfo.lats[timeStepIdx] =
                            trajInfo.lats[timeStepIdx + 1];
                        trajInfo.lons[timeStepIdx] =
                            trajInfo.lons[timeStepIdx + 1];
                        trajInfo.alts[timeStepIdx] =
                            trajInfo.alts[timeStepIdx + 1];
                        trajInfo.parcels[timeStepIdx] =
                            trajInfo.parcels[timeStepIdx + 1];
                    } else {
                        trajInfo.lats[timeStepIdx] = lat1;
                        trajInfo.lons[timeStepIdx] = lon1;
                        trajInfo.alts[timeStepIdx] = alt1;

                        Data urt   = usample.getSample(timeStepIdx);
                        Real ureal = (urt instanceof RealTuple)
                                     ? (Real) ((RealTuple) urt).getComponent(
                                         0)
                                     : (Real) urt;
                        u[timeStepIdx] = (float) ureal.getValue();


                        Data vrt   = vsample.getSample(timeStepIdx);
                        Real vreal = (vrt instanceof RealTuple)
                                     ? (Real) ((RealTuple) vrt).getComponent(
                                         0)
                                     : (Real) vrt;
                        v[timeStepIdx] = (float) vreal.getValue();


                        Data wrt   = wsample.getSample(timeStepIdx);
                        Real wreal = (wrt instanceof RealTuple)
                                     ? (Real) ((RealTuple) wrt).getComponent(
                                         0)
                                     : (Real) wrt;
                        if (wreal.isMissing()) {
                            w[timeStepIdx] = 0;
                        } else {
                            w[timeStepIdx] = (float) wreal.getValue();
                        }

                        Data srt   = ssample.getSample(timeStepIdx);
                        Real sreal = (srt instanceof RealTuple)
                                     ? (Real) ((RealTuple) srt).getComponent(
                                         0)
                                     : (Real) srt;
                        trajInfo.parcels[timeStepIdx] =
                            (float) sreal.getValue();

                    }

                }

            }


        } catch (Exception ee) {}


        return trajInfo;
    }

    /**
     * Class description
     *
     *
     * @version        Enter version here..., Mon, Mar 5, '12
     * @author         Enter your name here...
     */
    static class trackThredds implements Callable<FlatField> {

        /** _more_ */
        String variableName;

        /** _more_ */
        List<TrajInfo> tj;

        /** _more_ */
        Set timeSet;

        /** _more_ */
        FunctionType ft;

        /** _more_ */
        Unit varUnit;

        /** _more_ */
        int i;

        /**
         * _more_
         *
         * @param i _more_
         * @param variableName _more_
         * @param tj _more_
         * @param timeSet _more_
         * @param ft _more_
         * @param varUnit _more_
         */
        private trackThredds(int i, String variableName, List<TrajInfo> tj,
                             Set timeSet, FunctionType ft, Unit varUnit) {
            this.i            = i;
            this.variableName = variableName;
            this.tj           = tj;
            this.timeSet      = timeSet;
            this.ft           = ft;
            this.varUnit      = varUnit;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public FlatField call() {
            FlatField ff = createSingleTrack(variableName, tj, timeSet, ft,
                                             varUnit, i);
            //System.out.println("Thredds = " + i);
            return ff;
        }
    }



    /**
     * _more_
     *
     * @param variableName _more_
     * @param tj _more_
     * @param timeSet _more_
     * @param ft _more_
     * @param varUnit _more_
     * @param num _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<FlatField> createTracks(final String variableName,
            final List<TrajInfo> tj, final Set timeSet,
            final FunctionType ft, final Unit varUnit, final int num)
            throws Exception {

        ExecutorService       executor = Executors.newFixedThreadPool(8);
        final List<FlatField> result   = new ArrayList<FlatField>();
        List<Future>          pthreads = new ArrayList<Future>();
        for (int i = 0; i < num; i++) {
            Callable pt = new trackThredds(i, variableName, tj, timeSet, ft,
                                           varUnit);
            Future<Object> future = executor.submit(pt);
            pthreads.add(future);
        }

        for (Future<Object> o : pthreads) {
            try {
                result.add((FlatField) o.get());
            } catch (InterruptedException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ExecutionException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return result;
    }

    /**
     * _more_
     *
     * @param variableName _more_
     * @param tj _more_
     * @param timeSet _more_
     * @param ft _more_
     * @param varUnit _more_
     * @param num _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<FlatField> createTracks1(final String variableName,
            final List<TrajInfo> tj, final Set timeSet,
            final FunctionType ft, final Unit varUnit, final int num)
            throws Exception {

        ExecutorService           executor = Executors.newFixedThreadPool(4);
        Callable<List<FlatField>> task     = new Callable<List<FlatField>>() {
            public List<FlatField> call() {
                List<FlatField> result = new ArrayList<FlatField>();
                for (int i = 0; i < num; i = i + 1) {
                    result.add(i, createSingleTrack(variableName, tj,
                            timeSet, ft, varUnit, i));
                }
                return result;
            }
        };
        Future<List<FlatField>> future = executor.submit(task);
        List<FlatField>         cList  = future.get();
        return cList;
    }

    /**
     * _more_
     *
     * @param variableName _more_
     * @param tj _more_
     * @param timeSet _more_
     * @param ft _more_
     * @param varUnit _more_
     * @param i _more_
     *
     * @return _more_
     */
    public static FlatField createSingleTrack(final String variableName,
            final List<TrajInfo> tj, final Set timeSet,
            final FunctionType ft, final Unit varUnit, final int i) {

        try {
            Unit       timeUnit     = timeSet.getSetUnits()[0];
            int        length       = tj.size();
            double[][] newRangeVals = new double[2][length];
            double[]   timeVals     = timeSet.getDoubles()[0];
            newRangeVals[0] = tj.get(i).parcels;
            RealType timeType =
                RealType.getRealType(DataUtil.cleanName(variableName + "_"
                    + timeUnit), timeUnit);
            RealType varType =
                RealType.getRealType(DataUtil.cleanName(variableName + "_"
                    + varUnit), varUnit);
            RealTupleType rangeType = new RealTupleType(varType, timeType);
            newRangeVals[1] = timeVals;

            Set[] rangeSets = new Set[2];
            rangeSets[0] =
                new DoubleSet(new SetType(rangeType.getComponent(0)));
            rangeSets[1] =
                new DoubleSet(new SetType(rangeType.getComponent(1)));

            float trajCoords[][] = {
                tj.get(i).lons, tj.get(i).lats, tj.get(i).alts
            };
            Gridded3DSet domain =
                new Gridded3DSet(RealTupleType.SpatialEarth3DTuple,
                                 trajCoords, trajCoords[0].length);
            GriddedSet llaSet = domain;
            FunctionType newType =
                new FunctionType(((SetType) llaSet.getType()).getDomain(),
                                 rangeType);
            FlatField timeTrack = new FlatField(newType, llaSet,
                                      (CoordinateSystem) null, rangeSets,
                                      new Unit[] { varUnit,
                    timeUnit });
            timeTrack.setSamples(newRangeVals, false);

            return timeTrack;
        } catch (Exception ee) {}

        return null;
    }

    /**
     * _more_
     *
     * @param variableName _more_
     * @param lats _more_
     * @param lons _more_
     * @param alts _more_
     * @param param _more_
     * @param timeSet _more_
     * @param ft _more_
     * @param varUnit _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static FlatField createSingleTrack1(String variableName,
            float[] lats, float[] lons, float[] alts, double[] param,
            Set timeSet, FunctionType ft, Unit varUnit)
            throws Exception {

        Unit       timeUnit     = timeSet.getSetUnits()[0];
        double[][] newRangeVals = new double[2][param.length];
        double[]   timeVals     = timeSet.getDoubles()[0];
        newRangeVals[0] = param;
        RealType timeType =
            RealType.getRealType(DataUtil.cleanName(variableName + "_"
                + timeUnit), timeUnit);
        RealType varType =
            RealType.getRealType(DataUtil.cleanName(variableName + "_"
                + varUnit), varUnit);
        RealTupleType rangeType = new RealTupleType(varType, timeType);
        newRangeVals[1] = timeVals;

        Set[] rangeSets = new Set[2];
        rangeSets[0] = new DoubleSet(new SetType(rangeType.getComponent(0)));
        rangeSets[1] = new DoubleSet(new SetType(rangeType.getComponent(1)));

        float trajCoords[][] = {
            lons, lats, alts
        };
        Gridded3DSet domain =
            new Gridded3DSet(RealTupleType.SpatialEarth3DTuple, trajCoords,
                             trajCoords[0].length);
        GriddedSet llaSet = domain;
        FunctionType newType =
            new FunctionType(((SetType) llaSet.getType()).getDomain(),
                             rangeType);
        FlatField timeTrack = new FlatField(newType, llaSet,
                                            (CoordinateSystem) null,
                                            rangeSets, new Unit[] { varUnit,
                timeUnit });
        timeTrack.setSamples(newRangeVals, false);

        return timeTrack;
    }


}
