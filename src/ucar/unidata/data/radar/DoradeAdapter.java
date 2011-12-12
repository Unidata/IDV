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

package ucar.unidata.data.radar;


import ucar.atd.dorade.*;
import ucar.atd.dorade.DoradeSweep.DoradeSweepException;
import ucar.atd.dorade.DoradeSweep.MovingSensorException;

import ucar.unidata.data.*;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.Trace;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.bom.Radar2DCoordinateSystem;
import visad.bom.Radar3DCoordinateSystem;

import visad.data.units.*;


import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A data adapter for DORADE radar data
 * @author IDV Development Team @ ATD
 * @version $Revision: 1.19 $
 */
public class DoradeAdapter implements RadarAdapter {

    /** sweep file name */
    private String swpFileName = null;

    /** the sweep for this instance */
    private DoradeSweep mySweep = null;

    /** the data source */
    private DataSourceImpl dataSource;

    /** sensor number */
    private int sensor = 0;  // fixed for now...

    /** number of rays */
    private int nRays;

    /** number of cells */
    private int nCells;

    /** sweep start time */
    private DateTime swpTime;

    /** map param names to DoradePARMs */
    private HashMap paramMap;

    /** list of parameters types */
    private RealType[] paramTypes;

    /** list of units */
    private Unit[] units;

    /** 2d domain type */
    private RealTupleType radarDomain2d;

    /** 3d domain type */
    private RealTupleType radarDomain3d;

    /** 2D product domain */
    private GriddedSet productDomain2d;

    /** 3D product domain */
    private GriddedSet productDomain3d;

    /** private cache */
    private Hashtable cache = new Hashtable();

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(DoradeAdapter.class.getName());


    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public DoradeAdapter() {}

    /**
     * Construct a new DORADE adapter.
     *
     * @param source     DataSource (may be null)
     * @param fileName  name of the DORADE file to read
     *
     * @throws VisADException problem creating data
     */
    public DoradeAdapter(DataSourceImpl source, String fileName)
            throws VisADException {
        swpFileName     = fileName;
        this.dataSource = source;
        try {
            init();
        } catch (VisADException ex) {
            throw ex;
        }
    }



    /**
     * Create our DoradeSweepfile from the named file.
     *
     * @throws VisADException problem creating data
     */
    private void init() throws VisADException {
        String sensorName = null;

        try {
            mySweep = new DoradeSweep(swpFileName);
            swpTime = new DateTime(mySweep.getTime());
            nRays   = mySweep.getNRays();
            nCells  = mySweep.getNCells(0);
            DoradePARM[] parms = mySweep.getParamList();
            if (mySweep.getNSensors() > 1) {
                throw new VisADException("cannot handle multiple-sensor "
                                         + "DORADE sweep files");
            }
            sensorName = mySweep.getSensorName(sensor);
            paramMap   = new HashMap();
            for (int p = 0; p < parms.length; p++) {
                paramMap.put(parms[p].getName(), parms[p]);
            }
            //            Trace.call1("DA.initVisad");
            initVisADMembers();
            //            Trace.call2("DA.initVisad");
        } catch (VisADException vex) {
            throw vex;
        } catch (DoradeSweepException ex) {
            LogUtil.logException("DORADE sweep error", ex);
        } catch (java.io.IOException ex) {
            LogUtil.logException("file not found", ex);
        }
        //
        // Set our description now.
        //
        /*
        setDescription(sensorName + " DORADE sweep "
                       + DoradeSweep.formatDate(mySweep.getTime()));
        */
    }

    /**
     *  Implement the interce.
     *  For now lets not do anything
     */
    public void clearCachedData() {}



    /**
     * Initialize our VisAD members: parameter types, radar domain, etc.
     *
     * @throws VisADException problem creating the VisAD object
     */
    private void initVisADMembers() throws VisADException {

        boolean use3d = true;
        //
        // build an array of RealType-s for our parameters
        //
        int nParams = paramMap.size();
        paramTypes = new RealType[nParams];
        units      = new Unit[nParams];
        Iterator iter = paramMap.values().iterator();
        for (int p = 0; iter.hasNext(); p++) {
            DoradePARM parm = (DoradePARM) iter.next();

            //
            // try to build a Unit for this parameter
            //
            Unit u = null;
            try {
                u = Parser.parse(parm.getUnits());
            } catch (ParseException pex) {
                /* do nothing */
                ;
            }
            units[p] = u;

            //
            // make a RealType for the parameter
            //
            paramTypes[p] = RealType.getRealType(parm.getName(), u,
                    (Set) null);
        }



        //
        // Create the coordinate system to translate rng, az, el to
        // lat, lon, altitude
        //
        CoordinateSystem rcs3d = null;
        CoordinateSystem rcs2d = null;
        try {
            float cellSpacing = mySweep.getCellSpacing(sensor);
            float centerOfFirstCell = mySweep.getRangeToFirstCell(sensor)
                                      + 0.5f * cellSpacing;  // to center of cell
            rcs3d = new Radar3DCoordinateSystem(mySweep.getLatitude(sensor),
                    mySweep.getLongitude(sensor),
                    mySweep.getAltitude(sensor) / 1000.f,  // getAltitude returns km
                    centerOfFirstCell, cellSpacing, 0.0f, 1.0f,  // az bias/scale
                    0.0f, 1.0f);  // el bias/scale
            rcs2d = new Radar2DCoordinateSystem(mySweep.getLatitude(sensor),
                    mySweep.getLongitude(sensor), centerOfFirstCell,
                    cellSpacing, 0.0f, 1.0f);  // az bias/scale
        } catch (MovingSensorException ex) {
            LogUtil.logException("can't handle moving sensor", ex);
            return;
        }  /* catch (DoradeSweepException ex) {
              LogUtil.logException ("DORADE sweep error", ex);
              return;
          }*/
        //        Trace.msg("initVisad-1");
        Unit[] domUnits3d = new Unit[] { CommonUnit.meter, CommonUnit.degree,
                                         CommonUnit.degree };
        Unit[] domUnits2d = new Unit[] { CommonUnit.meter,
                                         CommonUnit.degree };

        //
        // build an array of domain values
        // 2d case: (range + az) x (nCells * nRays) array
        // 3d case: (range + az + el) x (nCells * nRays) array
        //
        float[][] domainVals3d = new float[3][nCells * nRays];
        float[][] domainVals2d = new float[2][];
        //Trace.msg("initVisad-2 cells="+ nCells+" rays=" + nRays + " total=" + (nCells*nRays));

        float[] azimuths   = mySweep.getAzimuths();
        float[] elevations = mySweep.getElevations();

        /* TODO: Comment out this for now, but revisit if problems
        //
        // determine (roughly) whether this sweep is clockwise or
        // counterclockwise
        //
        int cwCount = 0;
        for (int ray = 1; ray < nRays; ray++) {
            if (azimuths[ray] >= azimuths[ray - 1]) {
                cwCount++;
            }
        }
        Trace.msg("initVisad-3");

        int scanDir = (cwCount >= (nRays / 2))
                      ? 1
                      : -1;  // +1 is clockwise

        //
        // azimuths must be sorted, so normalize them as necessary
        // to make sure they remain increasing (clockwise
        // scan) or decreasing (counterclockwise scan)
        //
        for (int ray = 1; ray < nRays; ray++) {
            while (scanDir * (azimuths[ray] - azimuths[ray - 1]) < 0) {
                azimuths[ray] += scanDir * 360.0;
            }
        }
         */
        //        Trace.msg("initVisad-4");

        //
        // fill the domain value array with ranges, azimuths, and elevations
        //
        for (int ray = 0; ray < nRays; ray++) {
            for (int cell = 0; cell < nCells; cell++) {
                int elem = ray * nCells + cell;
                domainVals3d[0][elem] = cell;
                domainVals3d[1][elem] = azimuths[ray];
                domainVals3d[2][elem] = elevations[ray];
            }
        }
        //        Trace.msg("initVisad-5");

        //
        // just ranges and azimuths for 2D
        //
        domainVals2d[0] = domainVals3d[0];
        domainVals2d[1] = domainVals3d[1];

        //
        // radar domains, with coordinate systems
        //
        radarDomain3d = new RealTupleType(RANGE_TYPE, AZIMUTH_TYPE,
                                          ELEVATION_ANGLE_TYPE, rcs3d,
                                          (Set) null);
        radarDomain2d = new RealTupleType(RANGE_TYPE, AZIMUTH_TYPE, rcs2d,
                                          (Set) null);

        //        Trace.msg("init-7");

        //
        // now create the product domain
        //
        try {
            productDomain3d = (GriddedSet) new Gridded3DSet(radarDomain3d,
                    domainVals3d, nCells, nRays, (CoordinateSystem) null,
                    domUnits3d, (ErrorEstimate[]) null, false);

            productDomain2d = (GriddedSet) new Gridded2DSet(radarDomain2d,
                    domainVals2d, nCells, nRays, (CoordinateSystem) null,
                    domUnits2d, (ErrorEstimate[]) null, false, false);
        } catch (VisADException vae) {
            System.err.println("radarDomain2d: " + radarDomain2d + "\n"
                               + "radarDomain3d: " + radarDomain3d + "\n"
                               + "nCells: " + nCells + "\n" + "nRays: "
                               + nRays + "\n" + "domainVals.length "
                               + domainVals3d.length + "\n"
                               + "domainVals[0].length "
                               + domainVals3d[0].length + "\n");

            throw vae;
        }
    }

    /**
     * Get the data for the given DataChoice and selection criteria.
     * @param dataChoice         DataChoice for selection
     * @param subset             subsetting criteria
     * @param requestProperties  extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException couldn't create a remote data object
     * @throws VisADException  couldn't create the data
     */
    public DataImpl getData(DataChoice dataChoice, DataSelection subset,
                            Hashtable requestProperties)
            throws VisADException, RemoteException {

        if (requestProperties != null) {
            Hashtable timeLabels =
                (Hashtable) requestProperties.get(DataSource.PROP_TIMELABELS);
            if (timeLabels == null) {
                timeLabels = new Hashtable();
                requestProperties.put(DataSource.PROP_TIMELABELS, timeLabels);
            }

            if (isRHI()) {
                timeLabels.put(getBaseTime(),
                               "RHI: " + mySweep.getFixedAngle());
            } else {
                timeLabels.put(getBaseTime(),
                               "Elevation: "
                               + Misc.format(mySweep.getFixedAngle()));
            }
        }

        //
        // determine if we need 2D or 3D data, defaulting to 2D
        //
        if (requestProperties == null) {
            requestProperties = (dataChoice.getProperties() != null)
                                ? dataChoice.getProperties()
                                : new Hashtable();
        }

        String prop2dOr3d =
            (String) requestProperties.get(RadarConstants.PROP_2DOR3D);
        boolean use3d = (prop2dOr3d != null)
                        && prop2dOr3d.equals(RadarConstants.VALUE_3D);
        ObjectPair cacheKey = new ObjectPair(swpFileName,
                                             new ObjectPair(dataChoice,
                                                 new Boolean(use3d)));
        FieldImpl singleSweep = null;
        //TODO: Gotta figure out a better way to cache so that when the CacheManager flushes
        //its cache when memory is getting blown it also flushes  this cache.

        /**
         *   if (dataSource != null) {
         *   singleSweep = (FieldImpl) dataSource.getCache(cacheKey);
         *   }
         */
        singleSweep = (FieldImpl) cache.get(cacheKey);



        if (singleSweep != null) {
            return singleSweep;
        }

        RealTupleType radarDomain   = use3d
                                      ? radarDomain3d
                                      : radarDomain2d;
        GriddedSet    productDomain = use3d
                                      ? productDomain3d
                                      : productDomain2d;

        //
        // extract the parameter from the DataChoice
        //
        RealType pType     = (RealType) dataChoice.getId();
        int      unitIndex = -1;
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].equals(pType)) {
                unitIndex = i;
                break;
            }
        }
        Unit       u         = units[unitIndex];
        String     paramName = pType.getName();
        DoradePARM parm      = (DoradePARM) paramMap.get(paramName);

        //
        // create the FlatField
        //

        RealTupleType rangeType = new RealTupleType(pType);

        FunctionType  ftype     = new FunctionType(radarDomain, pType);
        singleSweep = new FlatField(ftype, productDomain,
                                    (CoordinateSystem[]) null, (Set[]) null,
                                    new Unit[] { u });

        //
        // now get the values
        //
        //        Trace.msg("DA.get-1");
        float[][] allValues = new float[1][nCells * nRays];
        //        Trace.msg("DA.get-2");
        long    start     = System.currentTimeMillis();
        float[] rayValues = null;
        for (int r = 0; r < nRays; r++) {
            try {
                //Pass in the rayValues as  a working set so we don't keep allocating.
                rayValues = mySweep.getRayData(parm, r, rayValues);
            } catch (DoradeSweepException ex) {
                LogUtil.logException("getting ray data", ex);
            }
            System.arraycopy(rayValues, 0, allValues[0], r * nCells, nCells);
        }
        //System.out.println("(DoradeDataSource) "
        //                   + (System.currentTimeMillis() - start)
        //                   + " ms getting " + paramName);

        //
        // replace BAD_VALUE-s with NaN, as used by VisAD
        //
        for (int samp = 0; samp < nRays * nCells; samp++) {
            if (allValues[0][samp] == DoradeSweep.BAD_VALUE) {
                allValues[0][samp] = Float.NaN;
            }
        }

        try {
            //jeffmc: Pass in false so we don't do a copy of allValues
            ((FlatField) singleSweep).setSamples(allValues, null, false);
        } catch (Exception exc) {
            LogUtil.logException("putting sample data", exc);
        }

        /**
         * if (dataSource != null) {
         * dataSource.putCache(cacheKey, singleSweep);
         * }
         */
        cache.put(cacheKey, singleSweep);


        return singleSweep;
    }

    /**
     * Check to see if this <code>DoradeDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DoradeAdapter)) {
            return false;
        }
        DoradeAdapter da = (DoradeAdapter) o;
        return this.swpFileName.equals(da.swpFileName);
    }

    /**
     * Get the hash code for this object.
     * @return hash code.
     */
    public int hashCode() {
        int hashCode = swpFileName.hashCode();
        return hashCode;
    }

    /**
     * Get the base time for this sweep
     *
     * @return time of sweep
     */
    public DateTime getBaseTime() {
        return swpTime;
    }

    /**
     * Get the parameters for this adapter
     *
     * @return parameters
     */
    protected RealType[] getParams() {
        return paramTypes;
    }


    /**
     * Is this sweep in ppi mode
     *
     * @return Is ppi mode
     */
    public boolean isPPI() {
        return mySweep.getScanMode() == ScanMode.MODE_PPI;
    }

    /**
     * Is this sweep in rhi mode
     *
     * @return Is RHI mode
     */
    public boolean isRHI() {
        return mySweep.getScanMode() == ScanMode.MODE_RHI;
    }

    /**
     * Is this sweep in survey mode
     *
     * @return Is survey mode
     */
    public boolean isSurvey() {
        return mySweep.getScanMode() == ScanMode.MODE_SUR;
    }


    /**
     * Get the scan mode for this sweep
     *
     * @return The scan mode
     */
    public ScanMode getScanMode() {
        return mySweep.getScanMode();
    }

    /**
     * Return the name of the file
     *
     * @return name
     */
    public String getName() {
        return toString();
    }


    /**
     * to String.
     *
     * @return to string
     */
    public String toString() {
        return swpFileName;
    }


    /**
     * Test main
     *
     * @param args cmd line args
     *
     * @throws Exception when bad things happen
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            DoradeSweep sweep      = new DoradeSweep(args[i]);
            int         nRays      = sweep.getNRays();
            int         nCells     = sweep.getNCells(0);
            DoradePARM  param      = sweep.lookupParamIgnoreCase("VR");
            float[]     azimuths   = sweep.getAzimuths();
            float[]     elevations = sweep.getElevations();
            for (int rayIdx = 0; rayIdx < nRays; rayIdx++) {
                System.out.println("ray:" + rayIdx + " " + elevations[rayIdx]
                                   + " " + azimuths[rayIdx]);
                float[] rayValues = sweep.getRayData(param, rayIdx);
                for (int cellIdx = 0; cellIdx < nRays; cellIdx++) {
                    if (cellIdx > 0) {
                        System.out.print(",");
                    }
                    System.out.print("" + rayValues[cellIdx]);
                }
                System.out.println("");
            }
        }
    }

    /**
     * Clean up whatever we need to when we are removed.
     */
    public void doRemove() {}

}
