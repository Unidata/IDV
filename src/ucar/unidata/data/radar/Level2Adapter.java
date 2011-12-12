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


import ucar.unidata.data.*;

import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectArray;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlResourceCollection;

import ucar.visad.RadarMapProjection;

import ucar.visad.Util;
import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.bom.Radar2DCoordinateSystem;
import visad.bom.Radar3DCoordinateSystem;

import visad.data.units.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.io.*;

import java.rmi.RemoteException;

import java.util.Arrays;

import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


/**
 * Adapt an Archive Level II file to a VisAD data structure.
 *
 * @author IDV Development team
 * @version $Revision: 1.111 $
 */
public class Level2Adapter implements RadarAdapter {

    /** local copy of the data object */
    private Level2Data data = null;

    /** location of the radar */
    private EarthLocation radarLocation = null;

    /** data source (used for caching) */
    private DataSourceImpl dataSource;

    /** nominal (starting) time for this volume */
    private DateTime baseTime = null;

    /** tilts */
    private double[] tilts;

    /** Type for a 2D domain */
    private RealTupleType domainType2D;

    /** Type for a 3D domain */
    private RealTupleType domainType3D;

    /** RealType for reflectivity */
    private static RealType refType = RealType.getRealType(REFLECTIVITY_NAME);

    /** RealType for Radial_Velocity */
    private static RealType velType =
        RealType.getRealType(RADIAL_VELOCITY_NAME, CommonUnit.meterPerSecond);

    /** RealType for Spectrum Width */
    private static RealType swType =
        RealType.getRealType(SPECTRUM_WIDTH_NAME, CommonUnit.meterPerSecond);

    /** radius of the earth (km) */
    private double R = 6371.01;
    // from http://ssd.jpl.nasa.gov/phys_props_earth.html

    /** station table */
    private static NamedStationTable nexradStations;


    /**
     * Construct a new Level2Adapter
     *
     * @param source DataSource for caching
     * @param filename  path of file
     *
     * @throws VisADException  couldn't create VisAD object
     * @throws RemoteException  couldn't create remote object
     * @throws IOException  problem opening or reading file
     */
    public Level2Adapter(DataSourceImpl source, String filename)
            throws VisADException, RemoteException, IOException {
        this(source, filename, null);
    }


    /**
     * Construct a new Level2Adapter for the radar location given
     *
     * @param source DataSource for caching
     * @param filename  path of file
     * @param radarLocation location of the radar
     *
     * @throws VisADException  couldn't create VisAD object
     * @throws RemoteException  couldn't create remote object
     * @throws IOException  problem opening or reading file
     */
    public Level2Adapter(DataSourceImpl source, String filename,
                         EarthLocation radarLocation)
            throws VisADException, RemoteException, IOException {
        this.dataSource    = source;
        this.radarLocation = radarLocation;
        File file = new File(filename);
        if ( !file.exists()) {
            throw new IOException("File " + filename + " does not exist");
        }
        loadData(filename);
        makeDomainTypes();
    }


    /**
     *  Implement the interce.
     *  For now lets not do anything
     */
    public void clearCachedData() {}


    /**
     * Get the starting time of this Volume
     *
     * @return starting time
     */
    public DateTime getBaseTime() {
        return baseTime;
    }

    /**
     * Get the array of sweep tilts in this files volume scan.
     *
     * @return tilts
     */
    public double[] getAngles() {
        return tilts;
    }

    /**
     * Set the radar location
     *
     * @param el  location of the radar
     *
     * @throws VisADException  couldn't create VisAD object
     * @throws RemoteException  couldn't create remote object
     */
    public void setRadarLocation(EarthLocation el)
            throws VisADException, RemoteException {
        radarLocation = el;
        makeDomainTypes();
    }

    /**
     * Get the data for this DataChoice
     *
     * @param choice              choice describing the data
     * @param ds                  sub-selection criteria
     * @param requestProperties   extra request criteria (e.g, used to get
     *                            RHI azimuth)
     *
     * @return VisAD Data object corresponding to this DataChoice
     *
     * @throws VisADException  couldn't create VisAD object
     * @throws RemoteException  couldn't create remote object
     */
    public DataImpl getData(DataChoice choice, DataSelection ds,
                            Hashtable requestProperties)
            throws VisADException, RemoteException {

        Object      choiceId = choice.getId();

        ObjectArray choiceAttrs;
        //Check for old bundles
        if (choiceId instanceof ObjectArray) {
            choiceAttrs = (ObjectArray) choiceId;
        } else if (choiceId instanceof ObjectPair) {
            ObjectPair pair = (ObjectPair) choiceId;
            choiceAttrs = new ObjectArray(pair.getObject1(),
                                          pair.getObject2(),
                                          RadarConstants.VALUE_3D);

        } else {
            throw new IllegalStateException("Unknown choice data:"
                                            + choiceId.getClass().getName());
        }


        if (requestProperties == null) {
            requestProperties = (choice.getProperties() != null)
                                ? choice.getProperties()
                                : new Hashtable();
        }

        int moment = ((Integer) choiceAttrs.getObject2()).intValue();
        boolean volume =
            VALUE_VOLUME.equals(Misc.getProperty(requestProperties,
                PROP_VOLUMEORSWEEP, VALUE_VOLUME));
        boolean getAll3DData =
            VALUE_3D.equals((String) choiceAttrs.getObject3());
        boolean in2D = VALUE_2D.equals(Misc.getProperty(requestProperties,
                           PROP_2DOR3D, VALUE_2D));
        FieldImpl fi = null;
        if (volume && getAll3DData) {
            if (requestProperties.containsKey(PROP_AZIMUTH)) {
                float rhiAzimuth = ((Float) requestProperties.get(
                                       PROP_AZIMUTH)).floatValue();
                fi = getRHI(moment, rhiAzimuth);
            } else {
                if (requestProperties.containsKey(PROP_TIMEHEIGHT)
                        || requestProperties.containsKey(PROP_VCS)) {
                    fi = getVolume(moment);
                    fi = (FlatField) resampleToLatLonAltGrid((FlatField) fi,
                            moment);
                } else if (requestProperties.containsKey(PROP_CAPPI_LEVEL)) {
                    Real level = null;

                    if (ds != null) {
                        Object o = ds.getFromLevel();
                        if ((o != null) && (o instanceof Real)) {
                            level = (Real) o;
                        }
                    }
                    if (level == null) {
                        level =
                            (Real) requestProperties.get(PROP_CAPPI_LEVEL);
                    }
                    fi = getCAPPI(moment, level);
                } else {
                    fi = getVolume(moment);
                }
            }
        } else {
            //System.out.println(" call getvwp ");
            if (requestProperties.containsKey(PROP_VWP)) {
                fi = getVWP(15.0);
            } else {
                double value = Double.NaN;

                Double fromProperties;
                if (ds != null) {
                    Object o = ds.getFromLevel();
                    if (o != null) {
                        Object newO = o;
                        if (o instanceof TwoFacedObject) {
                            newO = ((TwoFacedObject) o).getId();
                        }
                        if (newO instanceof Real) {
                            value = ((Real) newO).getValue();
                        }
                    }
                }
                if (Double.isNaN(value)) {
                    fromProperties =
                        (Double) requestProperties.get(PROP_ANGLE);
                    if (fromProperties != null) {
                        value = ((Double) fromProperties).doubleValue();

                    } else if (choiceAttrs.getObject1() instanceof Double) {
                        value =
                            ((Double) choiceAttrs.getObject1()).doubleValue();
                    }
                }
                // check one more time.
                if (Double.isNaN(value)) {
                    throw new IllegalArgumentException("No angle specified");
                }
                fi = getCut(moment, value, !in2D);
            }
        }
        // run GC again; then note memory in use now
        //long mem2 = Misc.gc() ;
        // show memory difference due to making this data object
        //System.out.println("  radar data object memory = "+(mem2-mem1) );
        return fi;
    }


    /**
     * Load data from file into a "Level2Data" object; get tilts
     *
     * @param radarFile  file to use
     *
     * @throws IOException  problem opening or reading file
     * @throws RemoteException  couldn't create remote object
     * @throws VisADException  couldn't create VisAD object
     */
    private void loadData(String radarFile)
            throws VisADException, RemoteException, IOException {
        data = new Level2Data(radarFile, dataSource.getDataContext());
        data.read(0, true);
        tilts = VCP.getAngles(data.getVCP());
        baseTime = new DateTime((data.getJulianDate() - 1) * 24 * 3600
                                + data.getSecsSinceMidnight() / 1000);
        String       stationId = data.getStationId();
        NamedStation station   = null;
        if ((radarLocation == null) && (stationId != null)) {
            // HACK: station ids in the file are 4 char IACO.  In the
            // table, we use the old 3 letter id (for now). Try first
            // as a 3 letter id and then if that is null, try as
            // a 4 letter id.
            NamedStationTable stations = getStations();
            station = (NamedStation) stations.get(stationId.substring(1));
            if (station == null) {  // try 4 letter id
                station = (NamedStation) stations.get(stationId);
            }
            radarLocation = (station != null)
                            ? (EarthLocation) station.getNamedLocation()
                            : (EarthLocation) new EarthLocationTuple(0, 0, 0);
        }
        //System.out.println("el = " + radarLocation);
    }


    /**
     * Get a 2D sweep of a particular moment at the specified elevation
     * angle.
     *
     * @param moment     moment (REFLECTIVITY, VELOCITY, SPECTRUM_WIDTH) of data
     * @param elevation     elevation angle
     *
     * @return  a FlatField representation of the cut
     *
     * @throws RemoteException  couldn't create remote object
     * @throws VisADException  couldn't create VisAD object
     */
    public FlatField getCut(int moment, double elevation)
            throws VisADException, RemoteException {
        return getCut(moment, elevation, false);
    }


    /**
     * Get a 2D sweep of a particular moment at the specified elevation
     * angle.
     *
     * @param moment  moment (REFLECTIVITY, VELOCITY, SPECTRUM_WIDTH) of data
     * @param elevation     elevation angle
     * @param want3D        true if you want this as a 3-D field
     *
     * @return  a 2- or 3-D FlatField representation of the cut
     *
     * @throws RemoteException  couldn't create remote object
     * @throws VisADException  couldn't create VisAD object
     */
    public FlatField getCut(int moment, double elevation, boolean want3D)
            throws VisADException, RemoteException {

        Trace.call1(" getCut");
        ObjectPair cacheKey = new ObjectPair(
                                  new ObjectPair(
                                      new ObjectPair(
                                          radarLocation,
                                          baseTime), new ObjectPair(
                                              new Integer(moment),
                                              new Double(
                                                  elevation))), new Boolean(
                                                      want3D));
        FlatField retField = (dataSource == null)
                             ? null
                             : (FlatField) dataSource.getCache(cacheKey);
        if (retField != null) {
            return retField;
        }

        int cut = Arrays.binarySearch(tilts, elevation);
        if (cut == -1) {
            return null;
        }
        int local_cut = cut;
        if (moment == REFLECTIVITY) {
            if (cut == 1) {
                local_cut = 2;
            } else if (local_cut >= 2) {
                local_cut = local_cut + 2;
            }
        } else {
            if (local_cut == 0) {
                local_cut = 1;
            } else if (local_cut == 1) {
                local_cut = 3;
            } else if (local_cut >= 2) {
                local_cut = local_cut + 3;
            }
        }
        Level2Record record        = new Level2Record();
        int          record_number = data.getCutStart(local_cut);
        record.readHeader(data.getDataInput(), record_number);
        int bins = record.getBinNum(moment);
        if (bins == 0) {
            return null;
        }
        int num_radials = record.readCut(data.getDataInput(), record_number);
        double[] range  = new double[bins];
        double   range_step;
        double   range_to_first_gate;
        if (moment == REFLECTIVITY) {
            range_step          = record.surv_size / 1000.;
            range_to_first_gate = (double) (record.first_bin / 1000.0);
        } else {
            range_step          = record.dopl_size / 1000.;
            range_to_first_gate = (double) (record.doppler_range / 1000.0);
        }
        range[0] = (double) (range_to_first_gate + range_step / 2);
        for (int i = 1; i < bins; i++) {
            range[i] = (double) (range[i - 1] + range_step);
        }
        float   azimuth;
        float[] azimuths      = new float[num_radials];
        float[] values        = new float[num_radials * bins];
        int     vc            = 0,
                azimuth_num   = 0;
        int     value_counter = 0;
        for (int azi = 0; azi < num_radials; azi++) {
            azimuth = data.getAzimuth(record_number);
            while (azimuth < 0.0) {
                record_number++;
                azimuth = data.getAzimuth(record_number);
            }
            azimuths[azimuth_num++] = azimuth;
            for (int j = 0; j < bins; j++) {
                vc         = value_counter;
                values[vc] = record.getBinValue(moment, azi, j);


                /*  TEST ONLY, PART 1 of 2 parts
                //  demo of how straight the beams' plots are;
                //  i.e. how correct in azimuth angles on plot.
                //  Combine a plot of this data with the radar range rings and radial grid.
                //  Note that the ranges as plotted are not uniform either.
                float azt = (int)(azimuth + 0.5);
                // set all azimuths to integers
                azimuths[azimuth_num-1] = azt;
                // set flag color or value on selected beams , to stand out
                if ((int)(azimuth + 0.5)%30 == 0) {
                    values[vc]=100.0f;
                }  // END TEST */


                value_counter++;
            }
            record_number++;
        }
        int[]     sortToOld  = QuickSort.sort(azimuths);
        float[][] domainVals = new float[(want3D)
                                         ? 3
                                         : 2][value_counter + bins];
        float[][] signalVals = new float[1][value_counter + bins];
        int       i2         = 0;
        float     workingAz  = 0;
        for (int j = 0; j < azimuth_num; j++) {
            for (int i = 0; i < bins; i++) {
                domainVals[0][i2] = (float) range[i];
                domainVals[1][i2] = azimuths[j];
                if (want3D) {
                    domainVals[2][i2] = (float) elevation;
                }
                signalVals[0][i2] = values[i + sortToOld[j] * bins];

                /* TEST ONLY PART 2 of 2: print domain values to be sure
                   if (azimuths[j]%30==0 && i%30==0)
                   System.out.println   ( " bin "+i+"  azimuth "+
                   domainVals[1][i2]+  " value "+
                   signalVals[0][i2]  + "   range "+domainVals[0][i2]);
                // END TEST */

                i2++;
            }
        }
        for (int i = 0; i < bins; i++) {
            domainVals[0][i2] = (float) range[i];
            domainVals[1][i2] = domainVals[1][0];
            if (want3D) {
                domainVals[2][i2] = (float) elevation;
            }
            signalVals[0][i2] = values[i + sortToOld[0] * bins];
            i2++;
        }
        RealTupleType tt  = (want3D)
                            ? domainType3D
                            : domainType2D;
        GriddedSet    set = (want3D)
                            ? (GriddedSet) new Gridded3DSet(tt, domainVals,
                                bins, azimuth_num + 1,
                                (CoordinateSystem) null,
                                new Unit[] { CommonUnit.meter.scale(1000),
                                             CommonUnit.degree,
                                             CommonUnit
                                                 .degree }, (ErrorEstimate[]) null,
                                                     true)
                            : (GriddedSet) new Gridded2DSet(tt, domainVals,
                                bins, azimuth_num + 1,
                                tt.getCoordinateSystem(),
                                new Unit[] { CommonUnit.meter.scale(1000),
                                             CommonUnit
                                                 .degree }, (ErrorEstimate[]) null,
                                                     true, false);
        FunctionType sweepType = new FunctionType(tt, getMomentType(moment));
        retField = new FlatField(sweepType, set, (CoordinateSystem) null,
                                 (Set[]) null, (moment != REFLECTIVITY)
                ? new Unit[] { CommonUnit.meterPerSecond }
                : (Unit[]) null);
        retField.setSamples(signalVals, false);
        if (dataSource != null) {
            dataSource.putCache(cacheKey, retField);
        }
        Trace.call2(" getCut");
        return retField;
    }


    /**
     * Make a data object to display in the IDV as a VAD Wind Profile.
     * for one time.
     *
     * @param altitudeLimit typically 10 to 15 km; highest wind used.
     *
     * @return  VWP for the given altitude limit
     *
     * @throws RemoteException  couldn't create remote object
     * @throws VisADException  couldn't create VisAD object
     */
    public FlatField getVWP(double altitudeLimit)
            throws VisADException, RemoteException {

        ObjectPair cacheKey =
            new ObjectPair(new ObjectPair(radarLocation, baseTime),
                           new ObjectPair(new String("VAD"),
                                          new String("VAD Wind Profile")));
        FlatField retField = (dataSource == null)
                             ? null
                             : (FlatField) dataSource.getCache(cacheKey);
        if (retField != null) {
            return retField;
        }
        int          moment        = VELOCITY;
        int          numbTilts     = VCP.getNumCuts(data.getVCP());
        int          bins          = 0,
                     dec           = 1,
                     record_number = 0;
        double       elevation     = 19.5;
        Level2Record record        = null;
        while (bins == 0) {
            int ii = numbTilts - dec;
            if (ii < 0) {
                return null;
            }
            elevation = tilts[ii];
            int cut = Arrays.binarySearch(tilts, elevation);
            if (cut == -1) {
                return null;
            }
            int local_cut = cut;
            if (moment == REFLECTIVITY) {
                if (cut == 1) {
                    local_cut = 2;
                } else if (local_cut >= 2) {
                    local_cut = local_cut + 2;
                }
            } else {
                if (local_cut == 0) {
                    local_cut = 1;
                } else if (local_cut == 1) {
                    local_cut = 3;
                } else if (local_cut >= 2) {
                    local_cut = local_cut + 3;
                }
            }
            record        = new Level2Record();
            record_number = data.getCutStart(local_cut);
            record.readHeader(data.getDataInput(), record_number);
            bins = record.getBinNum(moment);
            dec++;
        }
        int      numbVel           = bins / 4;
        int num_radials = record.readCut(data.getDataInput(), record_number);
        double[] range             = new double[bins];
        double[] altitude          = new double[bins];
        double   range_step        = record.dopl_size / 1000.0;  // 0.25 km
        double range_to_first_gate = (double) (record.doppler_range / 1000.0);
        range[0] = (double) (range_to_first_gate + range_step / 2);
        boolean gotlimit   = false;
        int     limitIndex = bins;
        double  radians    = elevation * Math.PI / 180.0;
        double stationAltitude =
            radarLocation.getAltitude().getValue(CommonUnit.meter) / 1000.0;
        for (int bi = 1; bi < bins; bi++) {
            range[bi] = (double) (range[bi - 1] + range_step);
            double dx  = range[bi] * Math.cos(radians);
            double dip = ((dx * dx) / 12742.0);
            // use this dip for 4 sig fig accuracy:
            //double dip = (-2*R + Math.sqrt(4*R*R + 4*dx*dx))/2;
            altitude[bi] = (dx * Math.sin(radians)) + stationAltitude + dip;
            if ((altitude[bi] > altitudeLimit) && (gotlimit == false)) {
                limitIndex = bi;
                gotlimit   = true;
            }
        }
        float   azimuth;
        float[] azimuths      = new float[num_radials];
        float[] windspeed     = new float[num_radials * bins];
        int     azimuth_num   = 0,
                value_counter = 0;
        for (int ac = 0; ac < num_radials; ac++) {
            azimuth = data.getAzimuth(record_number);
            while (azimuth < 0.0) {
                record_number++;
                azimuth = data.getAzimuth(record_number);
            }
            azimuths[azimuth_num++] = azimuth;
            for (int bin = 0; bin < limitIndex; bin++) {
                windspeed[value_counter++] = record.getBinValue(moment, ac,
                        bin);
            }
            record_number++;
        }
        int[] shiftedIndices = QuickSort.sort(azimuths);
        // wind speed and direction with height by preliminary simple method.
        // For better see William G. Collins, NCEP, 
        // "Quality Control of Velocity Azimuth Display (VAD) Winds..."
        float[] wndspd = new float[limitIndex];
        float[] wnddir = new float[limitIndex];
        float[] wndalt = new float[limitIndex];
        int     i2sum  = 0;
        for (int bi = 0; bi < limitIndex; bi++) {
            int   i2       = 0;
            float sum3Vels = 0.0f,
                  maxVel   = 0.0f,
                  minVel   = 0.0f,
                  minAZ    = 0,
                  maxAZ    = 0.0f;
            float v1, v2, v3, v4, v5, avspd;
            wndalt[bi] = (float) altitude[bi];
            for (int j = 2; j < azimuth_num - 2; j++) {
                v1 = windspeed[bi + shiftedIndices[j - 2] * limitIndex]
                     * 0.25f;
                v2 = windspeed[bi + shiftedIndices[j - 1] * limitIndex]
                     * 0.625f;
                v3 = windspeed[bi + shiftedIndices[j] * limitIndex];
                v4 = windspeed[bi + shiftedIndices[j + 1] * limitIndex]
                     * 0.625f;
                v5 = windspeed[bi + shiftedIndices[j + 2] * limitIndex]
                     * 0.25f;
                if ( !Float.isNaN(v1) && !Float.isNaN(v2) && !Float.isNaN(v3)
                        && !Float.isNaN(v4) && !Float.isNaN(v5)) {
                    avspd = (v1 + v2 + v3 + v4 + v5) / 2.75f;
                    if (avspd > maxVel) {
                        maxVel = avspd;
                        maxAZ  = azimuths[j];
                    }
                    if (avspd < minVel) {
                        minVel = avspd;
                        minAZ  = azimuths[j];
                    }
                    i2++;
                }
            }
            if ((i2 > 225) && (maxVel > 0.0) && (minVel < 0.0)) {
                wndspd[bi] = (maxVel - minVel) / 2;
                if (wndspd[bi] < 1.0) {
                    wndspd[bi] = 0.0f;  // see Collins's paper
                }
                maxAZ += 180f;
                if (maxAZ > 360.0) {
                    maxAZ -= 360.0f;
                }
                wnddir[bi] = (maxAZ + minAZ) / 2;
                i2sum      += i2;
                // print out to see the wind values at each level
                /*
                System.out.println("  bin " + bi + "  alt "
                                   + (float) altitude[bi] + "  i2 " + i2
                                   + "  speed " + wndspd[bi] + "  dir "
                                   + wnddir[bi]);
                                   */
            }
        }
        if (i2sum < 225) {
            System.out.println(
                "  VAD wind profile is unable to find significant winds"
                + " at any height with data from this station and time.");
            return null;
        }
        float[][] zsetfloats = new float[1][wndalt.length];
        for (int j = 0; j < wndalt.length; j++) {
            zsetfloats[0][j] = 1000.0f * (float) altitude[j];
        }
        Data[] ds = new RealTuple[wnddir.length];
        for (int j = 0; j < wnddir.length; j++) {
            ds[j] = new RealTuple(new Real[] {
                new Real(Display.Flow1Azimuth, wnddir[j]),
                new Real(Display.Flow1Radial, wndspd[j]) });
        }
        FunctionType onetimeFT = new FunctionType(RealType.Altitude,
                                     ds[0].getType());
        Gridded1DSet zset = new Gridded1DSet(RealType.Altitude, zsetfloats,
                                             zsetfloats[0].length);
        FlatField onetimeFF = new FlatField(onetimeFT, zset);
        onetimeFF.setSamples(ds, false);
        //if (dataSource != null) dataSource.putCache(cacheKey, onetimeFF);
        return onetimeFF;
    }

    // to make data for a pseudo-RHI display, 
    // extract beam data from from several 
    // aximuthal sweeps, the one sweep beam at each tilt 
    // nearest one azimuth only.
    //
    // use this with class = RadarRhiControl in the controls.xml, 
    // with the item with id=rhi.
    //
    // An RHI is a  
    // vertical cross section with one end fixed on the radar station,
    // at azimuth "rhiAz"
    // 
    // moment is flag for reflectivity, velocity etc.

    /**
     * Get an RHI for the given azimuth
     *
     * @param moment  moment (REFLECTIVITY, VELOCITY, SPECTRUM_WIDTH) of data
     * @param rhiAz   azimuth for the RHI
     *
     * @return  RHI as a Field of FlatFields, each for a particular elevation
     *          angle
     *
     * @throws RemoteException  couldn't create remote object
     * @throws VisADException  couldn't create VisAD object
     */
    public FieldImpl getRHI(int moment, double rhiAz)
            throws VisADException, RemoteException {

        Trace.call1("   getRHI");
        FlatField retField            = null;
        double    range_step          = 1.0;
        double    range_to_first_gate = 0;
        int       value_counter       = 0;
        double    halfBeamWidth;
        // from Kevin Manross, NWS: WSR-88D beam width in vertical direction is 0.95 deg
        halfBeamWidth = 0.95 / 2;
        int     record_number;
        boolean found;
        int     numbTilts      = VCP.getNumCuts(data.getVCP());
        int     number_of_bins = 0;
        float   azimuth;
        float[] elevations  = new float[numbTilts];
        int[]   bincount    = new int[numbTilts];
        int[]   tiltindices = new int[numbTilts];
        int     bincounter  = 1000;
        if (moment == REFLECTIVITY) {
            bincounter = 500;
        }
        float[][]    values   = new float[numbTilts][bincounter];
        double[][]   ranges   = new double[numbTilts][bincounter];
        DateTime     beamtime = new DateTime();
        Level2Record record;
        int          tiltcounter = 0;
        for (int ti = 0; ti < numbTilts; ti++) {
            found = false;
            int cut = Arrays.binarySearch(tilts, tilts[ti]);
            if (cut == -1) {
                continue;  // didn't find this tilt
            }
            int local_cut = cut;
            if (moment == REFLECTIVITY) {
                if (cut == 1) {
                    local_cut = 2;
                } else if (local_cut >= 2) {
                    local_cut = local_cut + 2;
                }
            } else {
                if (local_cut == 0) {
                    local_cut = 1;
                } else if (local_cut == 1) {
                    local_cut = 3;
                } else if (local_cut >= 2) {
                    local_cut = local_cut + 3;
                }
            }
            record_number = data.getCutStart(local_cut);
            int nextcut = data.getCutStart(local_cut + 1);
            for (int j = record_number; j < nextcut; j++) {
                float azi = data.getAzimuth(j);
                if (Math.abs(azi - rhiAz) <= halfBeamWidth) {
                    found  = true;
                    record = new Level2Record();
                    record.readRecord(data.getDataInput(), j);
                    range_to_first_gate = (float) (record.first_bin / 1000.0);
                    if (moment == REFLECTIVITY) {
                        range_step = 1.0;
                    } else {
                        range_step = 0.25;
                    }
                    number_of_bins = record.getBinNum(moment);
                    bincount[ti]   = number_of_bins;
                    if (number_of_bins == 0) {
                        continue;
                    }
                    float el = record.getElevation();
                    if (el == 0.0f) {
                        break;
                    }
                    elevations[ti] = el;
                    for (int z = 0; z < ti; z++) {
                        if (elevations[z] == el) {
                            break;
                        }
                    }
                    ranges[ti][0] = (double) (range_to_first_gate
                            + range_step / 2);
                    values[ti][0] = record.getBinValue(moment, 0, 0);
                    value_counter++;
                    for (int bi = 1; bi < number_of_bins; bi++) {
                        values[ti][bi] = record.getBinValue(moment, 0, bi);
                        ranges[ti][bi] = (double) (ranges[ti][bi - 1]
                                + range_step);
                        value_counter++;
                    }
                    if (number_of_bins < bincounter) {
                        for (int bi = number_of_bins; bi < bincounter; bi++) {
                            values[ti][bi] = Float.NaN;
                            ranges[ti][bi] = (double) (ranges[ti][bi - 1]
                                    + range_step);
                            value_counter++;
                        }
                    }
                    tiltindices[tiltcounter] = ti;
                    tiltcounter++;
                    break;
                }
            }
        }
        if (value_counter < 1) {
            return null;
        }
        FieldImpl fi = null;
        for (int tc = 0; tc < tiltcounter; tc++) {
            float[][] domainVals = new float[3][bincounter * 2];
            float[][] signalVals = new float[1][bincounter * 2];
            int       ti         = tiltindices[tc];
            float     lowerElev  = elevations[ti] - (float) halfBeamWidth;
            for (int bi = 0; bi < bincounter; bi++) {
                domainVals[0][bi] = (float) ranges[ti][bi];
                domainVals[1][bi] = (float) rhiAz;
                domainVals[2][bi] = lowerElev;
                signalVals[0][bi] = values[ti][bi];
            }
            float upperElev = elevations[ti] + (float) halfBeamWidth;
            for (int bi = 0; bi < bincounter; bi++) {
                domainVals[0][bi + bincounter] = (float) ranges[ti][bi];
                domainVals[1][bi + bincounter] = (float) rhiAz;
                domainVals[2][bi + bincounter] = upperElev;
                signalVals[0][bi + bincounter] = values[ti][bi];
            }
            RealTupleType radarDomainType = domainType3D;
            GriddedSet domainSet =
                (GriddedSet) new Gridded3DSet(radarDomainType, domainVals,
                    bincounter, 2, radarDomainType.getCoordinateSystem(),
                    new Unit[] { CommonUnit.meter.scale(1000),
                                 CommonUnit.degree,
                                 CommonUnit.degree }, (ErrorEstimate[]) null,
                                     true);
            FunctionType functionType = new FunctionType(radarDomainType,
                                            getMomentType(moment));
            retField = new FlatField(functionType, domainSet,
                                     (CoordinateSystem) null, (Set[]) null,
                                     (moment != REFLECTIVITY)
                                     ? new Unit[] {
                                         CommonUnit.meterPerSecond }
                                     : (Unit[]) null);
            retField.setSamples(signalVals, true);
            if (tc == 0) {
                RealType indexType = RealType.getRealType("integer_index");
                FunctionType fiFunction = new FunctionType(indexType,
                                              retField.getType());
                Integer1DSet intSet = new Integer1DSet(tiltcounter);
                fi = new FieldImpl(fiFunction, intSet);
                fi.setSample(0, retField, false);
            } else {
                fi.setSample(tc, retField, false);
            }
        }
        //if (dataSource != null) dataSource.putCache(cacheKey, fi);
        return fi;
    }  // end get RHI 



    // To make the data object for a CAPPI display, at one time,
    // extract beam data from from all
    // aximuthal sweeps, using bin values only at ranges that hit the level.
    //
    // A CAPPI is a Constant Altitude PPI constructed from several sweeps,
    // at a *constant altitude above the earth.*
    // Note this is on a spherical surface "parallel" to Earth's surface,
    // not on a flat plane at zero tilt at some altitude above the station.
    // In IDV plots where the earth's surface is projected onto a flat plane,
    // the CAPPI is plotted as a flat plane as well.
    // 
    // This code gives the same numerical data values as the IRAS CAPPI plots.
    // See TEST code in getCut to show demo of plots being distorted 
    // (slightly shifted azimuths and ranges) even when data values are correct.
    // 
    // @param  moment is flag for reflectivity or velocity etc.
    // @param level is level of CAPPI in METERS

    /**
     * Get a CAPPI for the given moment at the given level
     *
     * @param moment  moment (REFLECTIVITY, VELOCITY, SPECTRUM_WIDTH) of data
     * @param level   level (Altitude)
     *
     * @return CAPPI as a Field
     *
     * @throws RemoteException  couldn't create remote object
     * @throws VisADException  couldn't create VisAD object
     */
    public FieldImpl getCAPPI(int moment, Real level)
            throws VisADException, RemoteException {

        //Trace.call1("   getCAPPI");
        //long t1 = System.currentTimeMillis ();
        ObjectPair cacheKey =
            new ObjectPair(new ObjectPair(new ObjectPair(radarLocation,
                baseTime), new ObjectPair(new Integer(moment),
                                          level)), new String("CAPPI"));
        FieldImpl retField = (dataSource == null)
                             ? null
                             : (FieldImpl) dataSource.getCache(cacheKey);
        if (retField != null) {
            return retField;
        }
        double range_step          = 1.0;
        double range_to_first_gate = 0;
        int    dataVCP             = data.getVCP();
        int    numbTilts           = VCP.getNumCuts(dataVCP);
        int    totalbins           = 0;
        int    binCount            = 1000;
        int    record_number;
        double levelInMeters = level.getValue(CommonUnit.meter);
        if (moment == REFLECTIVITY) {
            binCount      = 500;
            record_number = data.getCutStart(0);
        } else {
            record_number = data.getCutStart(1);
        }
        Level2Record record = new Level2Record();
        record.readHeader(data.getDataInput(), record_number);
        int low_bins = record.getBinNum(moment);
        range_to_first_gate = (float) (record.first_bin / 1000.0);
        binCount            = low_bins;
        if (moment == REFLECTIVITY) {
            range_step = (float) 1.0;
        } else {
            range_step = (float) 0.25;
        }
        double[] ranges = new double[binCount];
        ranges[0] = (double) (range_to_first_gate + range_step / 2);
        for (int i = 1; i < binCount; i++) {
            ranges[i] = (double) (ranges[i - 1] + range_step);
        }
        float     azimuth;
        int       numAz        = 370;
        double[]  cappiRadius  = new double[binCount];
        float[][] cappiAz      = new float[binCount][numAz];
        float[][] cappiValue   = new float[binCount][numAz];
        int       bins         = 0;
        int       numberOfBins = 0;
        int       ringcounter  = 0;
        int[][]   shiftedIndex = new int[binCount][numAz];
        double    hiradians, loradians;
        double    dx;
        double stationAltitude =
            radarLocation.getAltitude().getValue(CommonUnit.meter) / 1000.0;
        int[] bin_LUT = new int[20];
        bin_LUT[0] = 0;
        int   vcpbin;
        float vcpelev;
        for (int ti = 1; ti < numbTilts; ti++) {
            int cut = Arrays.binarySearch(tilts, tilts[ti]);
            if (cut == -1) {
                continue;
            }
            int local_cut = cut;
            if (moment == REFLECTIVITY) {
                if (cut == 0) {
                    local_cut = 0;
                } else if (cut == 1) {
                    local_cut = 2;
                } else {
                    local_cut = cut + 2;
                }
            } else {
                if (cut == 0) {
                    local_cut = 1;
                } else if (cut == 1) {
                    local_cut = 3;
                } else {
                    local_cut = cut + 2;
                }
            }

            record_number = data.getCutStart(local_cut);
            record.readHeader(data.getDataInput(), record_number);
            /* keep for future demands for times of each sweep
            int seconds = (int) (record.milliseconds/1000);
            int hour   = (int) (seconds/3600);
            int minute = (int) ((seconds - hour*3600)/60);
            int second = (int) (seconds - hour*3600 - minute*60);
            System.out.println(
                "   tilt "+ti+"  first radial time "+hour+":"+minute+
                ":"+second+" UT   - Height "+levelInMeters+" meters");  */
            numberOfBins = record.getBinNum(moment);
            //System.out.println("Number of bins = " +numberOfBins);
            //if (numberOfBins == 0) {
            //    continue;
            //}
            vcpelev = (float) ((VCP.getCutAngle(dataVCP,
                    VCP.getNumCuts(dataVCP) - ti) + VCP.getCutAngle(dataVCP,
                        VCP.getNumCuts(dataVCP) - ti - 1)) / 2);
            vcpbin = calcRangeBin(vcpelev, levelInMeters, range_step);
            if (vcpbin > low_bins) {
                bin_LUT[ti] = low_bins;
            } else {
                bin_LUT[ti] = vcpbin;
            }
        }

        bin_LUT[numbTilts] = calcRangeBin(0.0, levelInMeters, range_step);

        if (bin_LUT[numbTilts] > low_bins) {
            bin_LUT[numbTilts] = low_bins;
        }

        for (int ti = 0; ti < numbTilts; ti++) {
            int cut = Arrays.binarySearch(tilts, tilts[ti]);
            if (cut == -1) {
                continue;
            }
            int local_cut = cut;
            if (moment == REFLECTIVITY) {
                if (cut == 0) {
                    local_cut = 0;
                } else if (cut == 1) {
                    local_cut = 2;
                } else {
                    local_cut = local_cut + 2;
                }
            } else {
                if (cut == 0) {
                    local_cut = 1;
                } else if (cut == 1) {
                    local_cut = 3;
                } else {
                    local_cut = local_cut + 2;
                }
            }
            record_number = data.getCutStart(local_cut);
            record.readHeader(data.getDataInput(), record_number);
            numberOfBins = record.getBinNum(moment);

            if (numberOfBins == 0) {
                continue;
            }

            int bininner = bin_LUT[numbTilts - ti - 1];
            int binouter = bin_LUT[numbTilts - ti];

            //System.out.println("binouter = " + binouter);
            //System.out.println("bininner = " + bininner);
            //System.out.println("ringcounter = " + ringcounter);

            int num_radials = record.readCut(data.getDataInput(),
                                             record_number);
            int bc = 0;
            for (int ac = 0; ac < numAz; ac++) {
                // IRAS uses integer azimuths here:
                azimuth = data.getAzimuth(record_number);  //exact azimuth for beam
                while (azimuth < 0.0) {
                    record_number++;
                    azimuth = data.getAzimuth(record_number);
                }
                bc = 0;
                for (int binIndex = binouter - 1; binIndex >= bininner;
                        binIndex--) {
                    int rc = ringcounter + bc;
                    if (ac == 0) {
                        cappiRadius[rc] = ranges[binIndex];
                    }
                    if (ac < num_radials) {
                        cappiAz[rc][ac] = azimuth;
                        cappiValue[rc][ac] = record.getBinValue(moment, ac,
                                binIndex);
                    } else {
                        cappiAz[rc][ac]    = 360.0f;
                        cappiValue[rc][ac] = Float.NaN;
                    }
                    bc++;
                }
                record_number++;
            }
            for (int bd = 0; bd < bc; bd++) {
                shiftedIndex[ringcounter + bd] =
                    QuickSort.sort(cappiAz[ringcounter + bd]);
            }
            ringcounter += bc;
        }

        float[][] domainVals = new float[2][ringcounter * numAz];
        float[][] signalVals = new float[1][ringcounter * numAz];
        int       k          = 0;
        for (int azi = 0; azi < numAz; azi++) {
            for (int ri = ringcounter - 1; ri >= 0; ri--) {
                domainVals[0][k] = (float) cappiRadius[ri];
                domainVals[1][k] = cappiAz[ri][azi];
                signalVals[0][k] =
                    (float) cappiValue[ri][shiftedIndex[ri][azi]];
                k++;
            }
        }
        RealTupleType tt = domainType2D;
        GriddedSet set = (GriddedSet) new Gridded2DSet(tt, domainVals,
                             ringcounter, numAz, tt.getCoordinateSystem(),
                             new Unit[] { CommonUnit.meter.scale(1000),
                                          CommonUnit
                                              .degree }, (ErrorEstimate[]) null,
                                                  true, false);

        FunctionType sweepType = new FunctionType(tt, getMomentType(moment));

        FlatField ff = new FlatField(sweepType, set, (CoordinateSystem) null,
                                     (Set[]) null, (moment != REFLECTIVITY)
                ? new Unit[] { CommonUnit.meterPerSecond }
                : (Unit[]) null);
        ff.setSamples(signalVals, false);

        FunctionType fiFunction = new FunctionType(RealType.Altitude,
                                      ff.getType());
        RealTuple    altRT  = new RealTuple(new Real[] { level });

        SingletonSet altSet = new SingletonSet(altRT);
        retField = new FieldImpl(fiFunction, altSet);
        retField.setSample(0, ff, false);
        //long t2 = System.currentTimeMillis ();
        //System.out.println("getCAPPI used "+((t2-t1)/1000.0f)+" secs");
        if (dataSource != null) {
            dataSource.putCache(cacheKey, retField);
        }
        return retField;  //retField;
    }

    /** factor for calculating bin */
    private double D = 0.000058869;

    /**
     * Calculate the range bin from the given parameters
     *
     * @param elevation  elevation angle
     * @param level      CAPPI level in meters
     * @param rangeStep  range step (km)
     * @return corresponding range bin
     */
    private int calcRangeBin(double elevation, double level,
                             double rangeStep) {
        double a = Math.cos((double) elevation * Math.PI / 180.0);
        a = D * a * a;
        double b = Math.sin((double) elevation * Math.PI / 180.0);
        double c = b * b + 4 * a * (level / 1000.0);
        return (int) (((-b + Math.sqrt(c)) / (2 * a)) / rangeStep);
    }

    /**
     * Get the RealType for the specified moment
     *
     * @param moment  moment (REFLECTIVITY, VELOCITY, SPECTRUM_WIDTH) of data
     *
     * @return RealType corresponding to the moment
     */
    private RealType getMomentType(int moment) {
        return (moment == REFLECTIVITY)
               ? refType
               : (moment == VELOCITY)
                 ? velType
                 : swType;
    }

    /**
     * Create the types for the 2- and 3-D domains
     *
     * @throws VisADException unable to create VisAD object
     */
    private void makeDomainTypes() throws VisADException {
        domainType2D = makeDomainType2D();
        domainType3D = makeDomainType3D();
    }

    /**
     * Create a type for the 2-D domain (range, azimuth).  If the station
     * location has been set, the type will include a CoordinateSystem to
     * transform to lat/lon.
     *
     * @return 2-D domain type
     *
     * @throws VisADException unable to create VisAD object
     */
    private RealTupleType makeDomainType2D() throws VisADException {
        CoordinateSystem cs = (radarLocation == null)
                              ? null
                              : new Radar2DCoordinateSystem(
                                  (float) radarLocation.getLatitude()
                                      .getValue(
                                          CommonUnit
                                              .degree), (float) radarLocation
                                                  .getLongitude()
                                                  .getValue(
                                                      CommonUnit.degree));
        return new RealTupleType(RANGE_TYPE, AZIMUTH_TYPE, cs, null);
    }

    /**
     * Create a type for the 3-D domain (range, azimuth, elevation_angle).
     * If the station * location has been set, the type will include a
     * CoordinateSystem to transform to lat/lon/alt.
     *
     * @return 3-D domain type
     *
     * @throws VisADException unable to create VisAD object
     */
    private RealTupleType makeDomainType3D() throws VisADException {
        CoordinateSystem cs = (radarLocation == null)
                              ? null
                              : new Radar3DCoordinateSystem((float) radarLocation
                                  .getLatitude()
                                  .getValue(CommonUnit
                                      .degree), (float) radarLocation
                                          .getLongitude()
                                          .getValue(CommonUnit
                                              .degree), (float) radarLocation
                                                  .getAltitude()
                                                  .getValue(CommonUnit
                                                      .meter));

        return new RealTupleType(RANGE_TYPE, AZIMUTH_TYPE,
                                 ELEVATION_ANGLE_TYPE, cs, null);
    }

    /**
     * String representation of this adapter.
     *
     * @return String representation of this object
     */
    public String toString() {
        return "Adapter for " + radarLocation + " at " + baseTime;
    }

    /**
     * Return the name of the file
     *
     * @return name
     */
    public String getName() {
        if (data != null) {
            return data.getFilename();
        }
        return "";
    }


    /**
     *  Makes a field of all data from one level II file (one volume scan);
     *  which is composed of severla azimuthal sweeps at different
     *  angles (titls, elevations) above the horizontal.
     *  Character of data field when plotted with ColorRadarVolumeControl is
     *  colored data point values (pixels) along beam lines,
     *  forming semi-transparent nested cones, not filled surfaces.
     *
     * @param moment  moment (REFLECTIVITY, VELOCITY, SPECTRUM_WIDTH) of data
     *
     * @return volume as a FieldImpl
     *
     * @throws RemoteException  couldn't create remote object
     * @throws VisADException  couldn't create VisAD object
     */
    public FlatField getVolume(int moment)
            throws VisADException, RemoteException {

        //long t1 = System.currentTimeMillis ();
        ObjectPair cacheKey =
            new ObjectPair(new ObjectPair(radarLocation, baseTime),
                           new ObjectPair(new Integer(moment),
                                          new String("range-az vol")));
        FlatField retField = (dataSource == null)
                             ? null
                             : (FlatField) dataSource.getCache(cacheKey);
        if (retField != null) {
            return retField;
        }
        int       numberOfSweeps = tilts.length;
        float     azimuth;
        float[][] azimuths   = new float[numberOfSweeps][370];
        float[][] elevations = new float[numberOfSweeps][370];
        int       bincounter = 1000;
        if (moment == REFLECTIVITY) {
            bincounter = 500;
        }
        float[][][] values = new float[numberOfSweeps][370][bincounter];
        for (int a = 0; a < numberOfSweeps; a++) {
            for (int b = 0; b < 370; b++) {
                for (int c = 0; c < bincounter; c++) {
                    values[a][b][c] = Float.NaN;
                }
            }
        }
        double[][]   ranges = new double[numberOfSweeps][bincounter];
        double       range_step;
        double       range_to_first_gate;
        int          value_counter = 0;
        Level2Record record        = new Level2Record();
        int          tiltcounter   = 0;
        for (int ti = 0; ti < numberOfSweeps; ti++) {
            double tilt = tilts[ti];
            int    cut  = Arrays.binarySearch(tilts, tilt);
            if (cut == -1) {
                continue;
            }
            int local_cut = cut;
            if (moment == REFLECTIVITY) {
                if (cut == 1) {
                    local_cut = 2;
                } else if (local_cut >= 2) {
                    local_cut = local_cut + 2;
                }
            } else {
                if (local_cut == 0) {
                    local_cut = 1;
                } else if (local_cut == 1) {
                    local_cut = 3;
                } else if (local_cut >= 2) {
                    local_cut = local_cut + 3;
                }
            }
            int record_number = data.getCutStart(local_cut);
            record.readHeader(data.getDataInput(), record_number);
            int numberOfBins = record.getBinNum(moment);
            if (numberOfBins <= 0) {
                continue;
            }
            if (moment == REFLECTIVITY) {
                range_step          = record.surv_size / 1000.;
                range_to_first_gate = (double) (record.first_bin / 1000.0);
            } else {
                range_step          = record.dopl_size / 1000.;
                range_to_first_gate = (double) (record.doppler_range
                        / 1000.0);
            }
            ranges[ti][0] = (double) (range_to_first_gate + range_step / 2);
            for (int i = 1; i < bincounter; i++) {
                ranges[ti][i] = (double) (ranges[ti][i - 1] + range_step);
            }
            int num_radials = record.readCut(data.getDataInput(),
                                             record_number);
            int nbi = 370;
            if (num_radials < 370) {
                nbi = num_radials;
            }
            for (int bi = 0; bi < nbi; bi++) {
                azimuth = data.getAzimuth(record_number);
                while (azimuth < 0.0) {
                    record_number++;
                    azimuth = data.getAzimuth(record_number);
                }
                int si = (int) (azimuth + 0.5);
                azimuths[ti][si]   = azimuth;
                elevations[ti][si] = record.getElevation();
                for (int binj = 0; binj < numberOfBins; binj++) {
                    values[ti][si][binj] = record.getBinValue(moment, bi,
                            binj);
                    value_counter++;
                }
                record_number++;
            }
            tiltcounter++;
        }
        float[][] domainVals = new float[3][bincounter * 370 * tiltcounter];
        float[][] signalVals = new float[1][bincounter * 370 * tiltcounter];
        int       k          = 0;
        for (int ti = 0; ti < tiltcounter; ti++) {
            for (int bi = 0; bi < 370; bi++) {
                for (int ri = 0; ri < bincounter; ri++) {
                    domainVals[0][k] = (float) ranges[ti][ri];
                    domainVals[1][k] = azimuths[ti][bi];
                    domainVals[2][k] = elevations[ti][bi];
                    signalVals[0][k] = values[ti][bi][ri];
                    k++;
                }
            }
        }
        RealTupleType tt = domainType3D;
        GriddedSet set = (GriddedSet) new Gridded3DSet(tt, domainVals,
                             bincounter, 370, tiltcounter,
                             (CoordinateSystem) null,
                             new Unit[] { CommonUnit.meter.scale(1000),
                                          CommonUnit.degree,
                                          CommonUnit
                                              .degree }, (ErrorEstimate[]) null,
                                                  true, false);
        FunctionType sweepType = new FunctionType(tt, getMomentType(moment));
        retField = new FlatField(sweepType, set, (CoordinateSystem) null,
                                 (Set[]) null, (moment != REFLECTIVITY)
                ? new Unit[] { CommonUnit.meterPerSecond }
                : (Unit[]) null);
        retField.setSamples(signalVals, false);
        //long t2 = System.currentTimeMillis ();
        //System.out.println("  end getVolume (range-az-elev); "
        //                 + ((t2-t1)/1000.0f)+" secs");
        if (dataSource != null) {
            dataSource.putCache(cacheKey, retField);
        }
        return retField;
    }

    // resample the 3D range-az-elev volume 
    // to a new 3D lat, lon, altitude in meters, grid
    // spans 460 km square box in 100 steps;
    // takes very roughly 15 seconds for a typical Level II file to
    // be resampled.
    // Too slow for real use; but kept here for future reference or use

    /**
     * Resample the sweep to a lat/lon/alt grid
     *
     * @param retField field to resample
     * @param moment  moment (REFLECTIVITY, VELOCITY, SPECTRUM_WIDTH) of data
     *
     * @return  resampled grid
     *
     * @throws RemoteException  couldn't create remote object
     * @throws VisADException  couldn't create VisAD object
     */
    private Field resampleToLatLonAltGrid(FlatField retField, int moment)
            throws VisADException, RemoteException {
        ObjectPair cacheKey =
            new ObjectPair(new ObjectPair(radarLocation, baseTime),
                           new ObjectPair(new Integer(moment),
                                          new String("latlonalt grid")));
        Field cacheField = (dataSource == null)
                           ? null
                           : (Field) dataSource.getCache(cacheKey);
        if (cacheField != null) {
            return cacheField;
        }
        int xyDim = 100,
            zDim  = 21;
        //long t1 = System.currentTimeMillis ();
        RadarMapProjection radarCS =
            new RadarMapProjection(radarLocation.getLatLonPoint(), xyDim,
                                   xyDim);
        Linear2DSet l2dset = new Linear2DSet(-180.0, 280.0, xyDim, -180.0,
                                             280.0, xyDim);
        float[][] gridlocs = l2dset.getSamples();
        float[][] latLonLocs = radarCS.toReference(gridlocs,
                                   new Unit[] { CommonUnits.KILOMETER,
                CommonUnits.KILOMETER });
        float[][] domainVals = new float[3][xyDim * xyDim * zDim];
        int       kk         = 0;
        for (int zi = 0; zi < zDim; zi++) {
            int jj = 0;
            for (int yi = 0; yi < xyDim; yi++) {
                for (int xi = 0; xi < xyDim; xi++) {
                    domainVals[0][kk] = latLonLocs[0][jj];
                    domainVals[1][kk] = latLonLocs[1][jj];
                    domainVals[2][kk] = 1000.0f * zi;
                    jj++;
                    kk++;
                }
            }
        }
        RealTupleType tt = new RealTupleType(RealType.Latitude,
                                             RealType.Longitude,
                                             RealType.Altitude);
        Gridded3DSet g3Dset = new Gridded3DSet(tt, domainVals, xyDim, xyDim,
                                  zDim);
        Field rsfield = retField.resample(g3Dset, Data.NEAREST_NEIGHBOR,
                                          Data.NO_ERRORS);
        //long t2 = System.currentTimeMillis ();
        if (dataSource != null) {
            dataSource.putCache(cacheKey, rsfield);
        }
        return (FlatField) rsfield;
    }

    /**
     * Read in the nexrad stations from the
     * idv/resources/nexradstns.xml resource
     *
     * @return List of of {@link ucar.unidata.metdata.NamedStation}-s
     */
    public static NamedStationTable getStations() {
        if (nexradStations == null) {
            List resources =
                Misc.newList("/ucar/unidata/idv/resources/nexradstns.xml");
            XmlResourceCollection stationResources =
                new XmlResourceCollection("", resources);
            List listOfTables =
                NamedStationTable.createStationTables(stationResources);
            if (listOfTables.size() > 0) {
                nexradStations = (NamedStationTable) listOfTables.get(0);
            } else {
                //What to do if there are no stations
                nexradStations = new NamedStationTable();
            }
        }
        return nexradStations;
    }

    /**
     * Print out the name of the moment.
     *
     * @param moment  radar moment (ex: REFLECTIVITY)
     *
     * @return a String name of the moment (ex: "Reflectivity");
     */
    public static String getMomentName(int moment) {
        switch (moment) {

          case REFLECTIVITY :
              return "Reflectivity";

          case VELOCITY :
              return "Radial Velocity";

          case SPECTRUM_WIDTH :
              return "Spectrum Width";

          default :
              return "Unknown moment";
        }
    }

    /**
     * Clean up whatever we need to when we are removed.
     */
    public void doRemove() {
        nexradStations = null;
        clearCachedData();
    }
}
