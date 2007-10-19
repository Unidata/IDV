/*
 * $Id: CDMRadarAdapter.java,v 1.54 2007/07/30 22:54:16 yuanho Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


import ucar.atd.dorade.DoradePARM;
import ucar.atd.dorade.DoradeSweep;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.units.DateUnit;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataUtil;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.util.*;
import ucar.visad.data.CachedFlatField;
import ucar.visad.RadarMapProjection;
import ucar.visad.Util;
import visad.*;
import visad.bom.Radar2DCoordinateSystem;
import visad.bom.Radar3DCoordinateSystem;
import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.NamedLocation;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A data adapter for Common Data Model radial data
 * (Level II, Level III, DORADE)
 * @author IDV Development Team
 * @version $Revision: 1.54 $
 */
public class CDMRadarAdapter implements RadarAdapter {

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(CDMRadarAdapter.class.getName());

    /** sweep file name */
    private String swpFileName = null;

    /** station name */
    private String stationName = null;

    /** station ID */
    private String stationID = null;

    /** data format */
    private String dataFormatName = null;

    /** location of the radar */
    private EarthLocation radarLocation = null;

    /** nominal (starting) time for this volume */
    private DateTime baseTime = null;

    /** factor for calculating bin */
    private double D = 0.000058869;

    /** map of angles to sweeps */
    private HashMap anglesMap;

    /** the data source */
    private DataSourceImpl dataSource;

    /** flag for a volume */
    private boolean isVolume;

    /** map param names to CDM params */
    private HashMap paramMap;

    /** list of parameters types */
    private RealType[] paramTypes;

    /** 2d domain type */
    private RealTupleType radarDomain2d;

    /** 3d domain type */
    private RealTupleType radarDomain3d;

    /** the sweep dataset */
    private RadialDatasetSweep rds;

    /** the volume pattern */
    private String vcp;

    /** netcdf dataset */
    private ucar.nc2.dt.grid.GridDataset gcd;

    /** flag for a raster */
    private boolean isRaster;

    /** Radius of the Earth */
    private double Re = (6374.0 * 4.0 / 3.0);

    /** PI */
    private double M_PI = 3.14159265358979323846;

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public CDMRadarAdapter() {}

    /**
     * Construct a new DORADE adapter.
     *
     * @param source     DataSource (may be null)
     * @param fileName  name of the DORADE file to read
     *
     * @throws VisADException problem creating data
     */
    public CDMRadarAdapter(DataSourceImpl source,
                           String fileName) throws VisADException {
        swpFileName     = fileName;
        this.dataSource = source;
        if (dataSource != null) {
            Object o =
                dataSource.getProperty(RadarDataSource.STATION_LOCATION,
                                       (Object) null);
            if (o != null) {
                if (o instanceof EarthLocation) {
                    radarLocation = (EarthLocation) o;
                } else if (o instanceof NamedStationImpl) {
                    radarLocation =
                        (EarthLocation) ((NamedStationImpl) o)
                            .getNamedLocation();
                }
            }

        }

        try {
            init();
        } catch (VisADException ex) {
            throw ex;
        }
    }

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
        double a = Math.cos(elevation * Math.PI / 180.0);

        a = D * a * a;

        double b = Math.sin(elevation * Math.PI / 180.0);
        double c = b * b + 4 * a * (level / 1000.0);

        return (int) (((-b + Math.sqrt(c)) / (2 * a)) / rangeStep);
    }

    /**
     * Check to see if this <code>CDMDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof CDMRadarAdapter)) {
            return false;
        }

        CDMRadarAdapter da = (CDMRadarAdapter) o;

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
     *  init from the named data file
     *
     * @throws VisADException problem creating data
     */
    private void init() throws VisADException {

        Trace.call1("CDMRadarAdapter:init");
        paramMap  = new HashMap();
        anglesMap = new HashMap();

        double[] angles;
        double[] vcpAngles;
        try {
            Trace.call1("CDMRadarAdapter:open dataset");
            rds = (RadialDatasetSweep) TypedDatasetFactory.open(
                thredds.catalog.DataType.RADIAL, swpFileName, null,
                new StringBuffer());
            Trace.call2("CDMRadarAdapter:open dataset");
            stationID      = rds.getRadarID();
            stationName    = rds.getRadarName();
            isVolume       = rds.isVolume();
            dataFormatName = rds.getDataFormat();
            vcp = rds.findGlobalAttributeIgnoreCase(
                "VolumeCoveragePatternName").getStringValue();
            Attribute attr = rds.findGlobalAttributeIgnoreCase("isRadial");
            if (attr != null) {
                int isR = attr.getNumericValue().intValue();
                if (isR == 3) {
                    isRaster = true;
                    gcd      = ucar.nc2.dt.grid.GridDataset.open(swpFileName);
                    NetcdfDataset nds = gcd.getNetcdfDataset();
                    attr = nds.findGlobalAttributeIgnoreCase(
                        "ProductStation");
                    stationID = attr.getStringValue();
                    attr = gcd.getNetcdfDataset()
                        .findGlobalAttributeIgnoreCase("ProductStationName");
                    stationName = attr.getStringValue();

                    ucar.nc2.Attribute attLat =
                        nds.findGlobalAttribute("RadarLatitude");
                    ucar.nc2.Attribute attLon =
                        nds.findGlobalAttribute("RadarLongitude");
                    ucar.nc2.Attribute attElev =
                        nds.findGlobalAttribute("RadarAltitude");
                    radarLocation = new EarthLocationTuple(
                        attLat.getNumericValue().doubleValue(),
                        attLon.getNumericValue().doubleValue(),
                        attElev.getNumericValue().intValue());
                    List glist = gcd.getGrids();

                    String attTime = nds.findAttValueIgnoreCase(null,
                                         "time_coverage_start", null);
                    baseTime =
                        new DateTime(DateUnit.getStandardOrISO(attTime));
                    Iterator iter = glist.iterator();
                    paramTypes = new RealType[1];

                    while (iter.hasNext()) {
                        GeoGrid gedVar = (GeoGrid) iter.next();
                        if ( !gedVar.getName().endsWith("RAW")) {
                            paramMap.put(gedVar.getName(), gedVar);
                            Unit u = getUnit(
                                         gedVar.findAttributeIgnoreCase(
                                             "units"));

                            paramTypes[0] =
                                RealType.getRealType(gedVar.getName(), u,
                                    null);
                            angles    = new double[1];
                            angles[0] = 0.0;
                            anglesMap.put(gedVar.getName(), angles);
                        }
                    }
                    return;
                }
                if (isR == 0) {
                    throw new IOException(
                        "Unable to handle this radar product!\n");
                }

            }
            short id    = getVCPid(vcp);
            List  rvars = rds.getDataVariables();

            if (radarLocation == null) {
                if (rds.isStationary()) {
                    radarLocation = new EarthLocationTuple(
                        rds.getCommonOrigin().getLatitude(),
                        rds.getCommonOrigin().getLongitude(),
                        rds.getCommonOrigin().getAltitude());
                } else {
                    radarLocation = new EarthLocationTuple(0, 0, 0);
                }
            } else if (radarLocation instanceof NamedLocation) {
                stationID =
                    ((NamedLocation) radarLocation).getIdentifier()
                        .getValue();
            }


            baseTime = new DateTime(rds.getStartDate());

            Iterator iter = rvars.iterator();
            int      p    = 0;

            paramTypes = new RealType[rvars.size()];

            Trace.call1("CDMRadarAdapter:var iterator");
            while (iter.hasNext()) {
                RadialDatasetSweep.RadialVariable radVar =
                    (RadialDatasetSweep.RadialVariable) iter.next();
                if ( !radVar.getName().endsWith("RAW")) {
                    // get sweep
                    int nsweep = radVar.getNumSweeps();

                    // mySweep = new RadialDatasetSweep.Sweep[nsweep];
                    paramMap.put(radVar.getName(), radVar);

                    Unit u = getUnit(radVar);

                    paramTypes[p] = RealType.getRealType(radVar.getName(), u,
                            null);
                    angles = new double[nsweep];

                    for (int i = 0; i < nsweep; i++) {
                        angles[i] = radVar.getSweep(i).getMeanElevation();
                    }

                    if (id != 0) {
                        vcpAngles = getVCPAngles(id, angles);
                        anglesMap.put(radVar.getName(), vcpAngles);
                    } else {
                        anglesMap.put(radVar.getName(), angles);
                    }
                    p++;
                } else {
                    paramTypes = new RealType[rvars.size() - 1];
                }
            }
            Trace.call2("CDMRadarAdapter:var iterator");
        } catch (java.io.IOException ex) {
            //LogUtil.logException("Error",ex);
            throw new VisADException(ex.getMessage());
        }
        Trace.call2("CDMRadarAdapter:init");


    }





    /**
     *  Create a type for the 2-D domain (range, azimuth).  If the station
     *  location has been set, the type will include a CoordinateSystem to
     *  transform to lat/lon.
     *
     *  @return 2-D domain type
     *
     *  @throws VisADException unable to create VisAD object
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
     *  Create a type for the 2-D domain (range, azimuth).  If the station
     *  location has been set, the type will include a CoordinateSystem to
     *  transform to lat/lon.
     *
     *
     * @param cellSpacing   cell spacing
     * @param centerOfFirstCell   center of first cell
     *  @return 2-D domain type
     *
     *  @throws VisADException unable to create VisAD object
     */
    private RealTupleType makeDomainType2D(
            float cellSpacing,
            float centerOfFirstCell) throws VisADException {
        CoordinateSystem cs = (radarLocation == null)
                              ? null
                              : new Radar2DCoordinateSystem((float) radarLocation
                                  .getLatitude()
                                  .getValue(CommonUnit
                                      .degree), (float) radarLocation
                                          .getLongitude()
                                          .getValue(CommonUnit
                                              .degree), centerOfFirstCell,
                                                  cellSpacing, 0.0f, 1.0f);

        return new RealTupleType(RANGE_TYPE, AZIMUTH_TYPE, cs, null);
    }

    /**
     *  Create a type for the 3-D domain (range, azimuth, elevation_angle).
     *  If the station * location has been set, the type will include a
     *  CoordinateSystem to transform to lat/lon/alt.
     *
     *  @return 3-D domain type
     *
     *  @throws VisADException unable to create VisAD object
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
     *  Create a type for the 3-D domain (range, azimuth, elevation_angle).
     *  If the station * location has been set, the type will include a
     *  CoordinateSystem to transform to lat/lon/alt.
     *
     *
     * @param cellSpacing         cell spacing
     * @param centerOfFirstCell   center of first cell
     * @return 3-D domain type
     *
     * @throws VisADException unable to create VisAD object
     */
    private RealTupleType makeDomainType3D(
            float cellSpacing,
            float centerOfFirstCell) throws VisADException {
        CoordinateSystem cs = (radarLocation == null)
                              ? null
                              : new Radar3DCoordinateSystem(
                                  (float) radarLocation.getLatitude().getValue(
                                      CommonUnit.degree), (float) radarLocation.getLongitude().getValue(
                                      CommonUnit.degree), (float) radarLocation.getAltitude().getValue(
                                      CommonUnit.meter), centerOfFirstCell, cellSpacing, 0.0f, 1.0f, 0.0f, 1.0f);

        return new RealTupleType(RANGE_TYPE, AZIMUTH_TYPE,
                                 ELEVATION_ANGLE_TYPE, cs, null);
    }

    /**
     *  Init radar domain for both 2D and 3D
     *
     * @param cellSpacing     cell spacing
     * @param centerOfFirstCell  distance to center of first cell
     *
     * @throws VisADException unable to create VisAD object
     */
    private void makeDomainTypes(
            float cellSpacing,
            float centerOfFirstCell) throws VisADException {
        radarDomain2d = makeDomainType2D(cellSpacing, centerOfFirstCell);
        radarDomain3d = makeDomainType3D(cellSpacing, centerOfFirstCell);
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
     * Get the angles for this parameter
     *
     * @param vname   variable name
     * @return  angles for that variable
     */
    protected double[] getAngles(String vname) {
        return (double[]) anglesMap.get(vname);
    }

    /**
     * Get the base time for this sweep
     *
     * @return time of sweep
     */
    public DateTime getBaseTime() {
        return baseTime;
    }

    /**
     * Get the parameters for this adapter
     *
     *
     * @param vname   variable name
     * @return  id for that variable
     */
    protected short getVCPid(String vname) {
        if (vname == null) {
            return 0;
        }
        if (vname.startsWith("9 elevation")) {
            return 121;
        } else if (vname.startsWith("7 elevation")) {
            return 32;
        } else if (vname.startsWith("8 elevation")) {
            return 31;
        } else if (vname.startsWith("11 elevation")) {
            return 21;
        } else if (vname.startsWith("14 elevation")) {
            return 12;
        } else if (vname.startsWith("16 elevation")) {
            return 11;
        } else {
            return 0;
        }
    }

    /**
     * Get the parameters for this adapter
     *
     *
     * @param id   vcp id number
     * @param origAngles The original angles
     * @return  dd vcp angles
     */
    protected double[] getVCPAngles(short id, double[] origAngles) {
        int      len = origAngles.length;
        double[] dd  = new double[len];
        double   ang;
        for (int i = 0; i < len; i++) {
            ang   = origAngles[i];
            dd[i] = getVCPAngle(ang, id);
        }
        return dd;
    }

    /**
     * Find the closest angle to the given VCP angles
     * @param angle to find
     * @param id of VCP
     * @return ang for angle;
     */
    protected double getVCPAngle(double angle, short id) {
        double   minD      = Double.MAX_VALUE;
        double[] vcpAngles = VCP.getAngles(id);
        double   ang       = 0;
        for (int i = 0; i < vcpAngles.length; i++) {
            double diff = Math.abs(angle - vcpAngles[i]);
            if (diff < minD) {
                ang  = vcpAngles[i];
                minD = diff;
            }
        }
        return ang;
    }

    /**
     * Makes a field of all data from one common data model radar adapter;
     *
     * @param moment  the moment
     * @param varName variable name
     * @param level  the level
     *
     * @return capi as a FieldImpl
     *
     * @throws IOException  problem reading the file
     * @throws RemoteException   problem with Java RMI
     * @throws VisADException    problem creating VisAD object
     */
    private FieldImpl getCAPPIOld(int moment, String varName,
                                  Real level) throws VisADException,
                                      RemoteException, IOException {

        Trace.call1("   getCAPPI", level.longString());
        ObjectPair cacheKey =
            new ObjectPair(new ObjectPair(new ObjectPair(radarLocation,
                baseTime), new ObjectPair(new Integer(moment),
                                          level)), "CAPPI");
        FieldImpl retField = (FieldImpl) getCache(cacheKey);

        if (retField != null) {
            return retField;
        }

        RadialDatasetSweep.RadialVariable sweepVar =
            (RadialDatasetSweep.RadialVariable) rds.getDataVariable(varName);
        int    numberOfSweeps = sweepVar.getNumSweeps();
        int    numberOfRay    = getRayNumber(sweepVar);
        double levelInMeters  = level.getValue(CommonUnit.meter);
        int    vcpbin;
        float  vcpelev;
        double range_step          = 1.0;
        double range_to_first_gate = 0;
        int    low_bins            = 0;
        int    low_rays            = numberOfRay;
        int[]  bin_LUT             = new int[20];

        if (numberOfRay > 360) {
            low_rays = 360;
        }

        for (int t = 1; t < numberOfSweeps; t++) {
            int bins = sweepVar.getSweep(t).getGateNumber();

            if (bins > low_bins) {
                low_bins = bins;
            }

            vcpelev =
                (sweepVar.getSweep(numberOfSweeps - t).getMeanElevation()
                 + sweepVar.getSweep(numberOfSweeps - t
                                     - 1).getMeanElevation()) / 2.f;
            vcpbin = calcRangeBin(vcpelev, levelInMeters, range_step);

            if (vcpbin > low_bins) {
                bin_LUT[t] = low_bins;
            } else {
                bin_LUT[t] = vcpbin;
            }
        }

        bin_LUT[numberOfSweeps] = calcRangeBin(0.0, levelInMeters,
                range_step);

        if (bin_LUT[numberOfSweeps] > low_bins) {
            bin_LUT[numberOfSweeps] = low_bins;
        }

        int       low_binss = 1000;
        int       numberOfBins;
        int       ringcounter  = 0;
        int[][]   shiftedIndex = new int[low_binss][low_rays];
        float     azimuth;
        double[]  cappiRadius = new double[low_binss];
        float[][] cappiAz     = new float[low_binss][low_rays];
        float[][] cappiValue  = new float[low_binss][low_rays];
        double[]  ranges      = new double[low_binss];

        ranges[0] = (range_to_first_gate + range_step / 2);

        for (int i = 1; i < low_binss; i++) {
            ranges[i] = ranges[i - 1] + range_step;
        }

        for (int ti = 0; ti < numberOfSweeps; ti++) {
            RadialDatasetSweep.Sweep rds = sweepVar.getSweep(ti);
            numberOfBins = rds.getGateNumber();

            if (numberOfBins == 0) {
                continue;
            }

            int   bininner    = bin_LUT[numberOfSweeps - ti - 1];
            int   binouter    = bin_LUT[numberOfSweeps - ti];
            int   num_radials = rds.getRadialNumber();
            int   bc          = 0;
            float lastAzi     = 0.0f;

            for (int ac = 0; ac < low_rays; ac++) {
                float[] rayData = null;
                // IRAS uses integer azimuths here:
                if (ac < num_radials) {
                    azimuth = rds.getAzimuth(ac);  // exact azimuth for beam
                    rayData = rds.readData(ac);
                    lastAzi = azimuth;
                } else {
                    //if(lastAzi <= 359.0 ) {
                    azimuth = lastAzi + .01f;  //360.0f;
                    lastAzi = azimuth;
                    // }
                    // else {
                    //     azimuth = lastAzi - 360 + 1.f;
                    //     lastAzi = azimuth;
                    // }
                }

                bc = 0;


                for (int binIndex = binouter - 1; binIndex >= bininner;
                        binIndex--) {
                    int rc = ringcounter + bc;

                    if (ac == 0) {
                        cappiRadius[rc] = ranges[binIndex];
                    }
                    cappiAz[rc][ac] = azimuth;
                    if (ac < num_radials) {
                        //   cappiAz[rc][ac]    = azimuth;
                        cappiValue[rc][ac] = rayData[binIndex];
                    } else {
                        //    cappiAz[rc][ac]    = 360.0f;
                        cappiValue[rc][ac] = Float.NaN;
                    }

                    bc++;
                }
            }

            for (int bd = 0; bd < bc; bd++) {
                shiftedIndex[ringcounter + bd] =
                    QuickSort.sort(cappiAz[ringcounter + bd]);
            }

            ringcounter += bc;
        }

        float[][] domainVals = new float[2][ringcounter * low_rays];
        float[][] signalVals = new float[1][ringcounter * low_rays];
        int       k          = 0;

        for (int azi = 0; azi < low_rays; azi++) {
            for (int ri = ringcounter - 1; ri >= 0; ri--) {
                domainVals[0][k] = (float) cappiRadius[ri];
                domainVals[1][k] = cappiAz[ri][azi];
                signalVals[0][k] = cappiValue[ri][shiftedIndex[ri][azi]];
                k++;
            }
        }

        radarDomain2d = makeDomainType2D();

        RealTupleType tt = radarDomain2d;
        GriddedSet set = new Gridded2DSet(tt, domainVals, ringcounter,
                                          low_rays, tt.getCoordinateSystem(),
                                          new Unit[] {
                                              CommonUnit.meter.scale(1000),
                CommonUnit.degree }, null, false, true);
        FunctionType sweepType = new FunctionType(tt, getMomentType(varName));
        Unit         u         = getUnit(sweepVar);
        FlatField ff = new FlatField(sweepType, set, (CoordinateSystem) null,
                                     (Set[]) null, new Unit[] { u });

        ff.setSamples(signalVals, false);

        FunctionType fiFunction = new FunctionType(RealType.Altitude,
                                      ff.getType());
        RealTuple    altRT  = new RealTuple(new Real[] { level });
        SingletonSet altSet = new SingletonSet(altRT);

        retField = new FieldImpl(fiFunction, altSet);
        retField.setSample(0, ff, false);

        putCache(cacheKey, retField);
        Trace.call2("   getCAPPI");

        return retField;  // retField;

    }

    /**
     * This api is based on the rsl c library from TRMM Office Radar Software Library.
     *
     * @param moment  the moment
     * @param varName variable name
     * @param level  the level
     *
     * @return capi as a FieldImpl
     *
     * @throws IOException  problem reading the file
     * @throws RemoteException   problem with Java RMI
     * @throws VisADException    problem creating VisAD object
     */
    public FieldImpl getCAPPI(int moment, String varName,
                              Real level) throws VisADException,
                                  RemoteException, IOException {

        Trace.call1("   rsl_getCAPPI", level.longString());
        ObjectPair cacheKey =
            new ObjectPair(new ObjectPair(new ObjectPair(radarLocation,
                baseTime), new ObjectPair(new Integer(moment),
                                          level)), "CAPPI");
        FieldImpl retField = (FieldImpl) getCache(cacheKey);

        if (retField != null) {
            return retField;
        }

        RadialDatasetSweep.RadialVariable sweepVar =
            (RadialDatasetSweep.RadialVariable) rds.getDataVariable(varName);

        //float range_step;
        //float range_to_first_gate = 0;


        /* using sw0 information to construct the CAPPI*/

        int                      swIdx      = 0;
        RadialDatasetSweep.Sweep sw0        = sweepVar.getSweep(swIdx);
        int                      numRay     = 361;
        int                      numBin     = sw0.getGateNumber();
        int                      numSweep   = sweepVar.getNumSweeps();
        float                    beamWidth  = sw0.getBeamWidth();
        float                    gateSize   = sw0.getGateSize();
        float                    range_step = sw0.getGateSize();
        float range_to_first_gate           = sw0.getRangeToFirstGate();

        if (rayData == null) {
            rayData = new float[numSweep][][];
            //  rayIndex = new int[numSweep][];
        }

        //    if (numRay > 360) {
        //       numRay = 360;
        //    }

        /* Calculate elevation angle verse range array */
        float[][] slantrElev = new float[numBin][2];
        float     ht         = (float) level.getValue() / 1000.0f;
        for (int a = 0; a < numBin; a++) {
            float grange = (range_to_first_gate + (a * range_step)) / 1000.f;

            slantrElev[a] = getSlantrAndElev(grange, ht);
        }

        float[]   cappiAz    = new float[numRay];
        float[][] cappiValue = new float[numBin][numRay];


        double[]  ranges     = new double[numBin];

        // get the closest sweep index for each bin
        int[] sweepI = new int[numBin];
        for (int b = 0; b < numBin; b++) {
            sweepI[b] = getClosestSweepIndex(sweepVar, slantrElev[b][1],
                                             beamWidth / 2.f);
        }
        // read all the azimuth and sorted
        float[][] aziArray    = new float[numSweep][];
        int[][]   aziArrayIdx = new int[numSweep][];

        for (int b = 0; b < numSweep; b++) {
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
            aziArray[b]    = s1.getAzimuth();
            aziArrayIdx[b] = QuickSort.sort(aziArray[b]);
        }

        // now get the hash map for each sweep contain azi as index and ray information.
        Trace.call1("   sweep list");
        if (RSL_sweep_list == null) {
            RSL_sweep_list = new CDMRadarSweepDB[sweepVar.getNumSweeps()];

            for (int b = 0; b < numSweep; b++) {
                //   RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
                RSL_sweep_list[b] = constructSweepHashTable(aziArray[b],
                        aziArrayIdx[b], beamWidth);
            }
        }
        Trace.call2("   sweep list");

        // ranges for cappi
        ranges[0] = (range_to_first_gate + range_step / 2);
        for (int i = 1; i < numBin; i++) {
            ranges[i] = ranges[i - 1] + range_step;
        }

        Trace.call1("   get cappi value");
        // setting cappi azi value for each ray.
        float[] az = new float[numRay];
        for (int i = 0; i < numRay; i++) {
            az[i] = i;
        }


        if (rayIndex == null) {
            rayIndex = getRayIndex(sweepVar, az, numRay, numSweep);
            rayData  = getRayData(sweepVar, numRay, numSweep, numBin);
        }

        // get the cappi value for each ray and bin
        for (int a = 0; a < numRay; a++) {
            float azi = az[a];  //sw0.getAzimuth(a);
            cappiAz[a] = azi;

            for (int b = 0; b < numBin; b++) {
                int swIndex = sweepI[b];
                if (swIndex == 999) {
                    cappiValue[b][a] = Float.NaN;
                } else {
                    int rayIndx = rayIndex[swIndex][a];
                    if ((rayIndx == 999) || (rayIndx >= 360)) {
                        cappiValue[b][a] = Float.NaN;
                    } else {
                        float[] rdata = rayData[swIndex][rayIndx];
                        cappiValue[b][a] = getValueFromRay(rdata,
                                slantrElev[b][0], gateSize,
                                range_to_first_gate);
                    }
                }

            }
        }
        Trace.call2("   get cappi value");

        int[]     sortedAzs  = QuickSort.sort(cappiAz);

        float[][] domainVals = new float[2][numBin * numRay];
        float[][] signalVals = new float[1][numBin * numRay];
        int       k          = 0;

        for (int azi = 0; azi < numRay; azi++) {
            for (int ri = 0; ri < numBin; ri++) {
                domainVals[0][k] = (float) ranges[ri];
                domainVals[1][k] = cappiAz[azi];
                signalVals[0][k] = cappiValue[ri][sortedAzs[azi]];
                k++;
            }
        }


        Trace.call1("   make field");
        radarDomain2d = makeDomainType2D();

        RealTupleType tt = radarDomain2d;
        GriddedSet set = new Gridded2DSet(tt, domainVals, numBin, numRay,
                                          tt.getCoordinateSystem(),
                                          new Unit[] {
                                              CommonUnit.meter.scale(1),
                CommonUnit.degree }, null, false, true);
        FunctionType sweepType = new FunctionType(tt, getMomentType(varName));
        Unit         u         = getUnit(sweepVar);
        FlatField ff = new FlatField(sweepType, set, (CoordinateSystem) null,
                                     (Set[]) null, new Unit[] { u });

        ff.setSamples(signalVals, false);

        FunctionType fiFunction = new FunctionType(RealType.Altitude,
                                      ff.getType());
        double stationElev =
            radarLocation.getAltitude().getValue(level.getUnit());

        Real levelAboveStation = level.cloneButValue(stationElev
                                     + level.getValue());
        RealTuple    altRT  = new RealTuple(new Real[] { levelAboveStation });
        SingletonSet altSet = new SingletonSet(altRT);

        retField = new FieldImpl(fiFunction, altSet);
        retField.setSample(0, ff, false);

        putCache(cacheKey, retField);
        Trace.call2("   make field");
        Trace.call2("   rsl_getCAPPI");

        return retField;  // retField;

    }

    /**
     * re arrange the volume data
     *
     * @param sweepVar
     * @param numRay number of radial, 360
     * @param numSweep sweep number
     * @param numBin gate number
     *
     * @return 3d volume data
     *
     * @throws IOException _more_
     */
    float[][][] getRayData(RadialDatasetSweep.RadialVariable sweepVar,
                           int numRay, int numSweep,
                           int numBin) throws IOException {


        float[][][] rData = new float[numSweep][numRay][numBin];


        // get data for the whole volume
        for (int swIndex = 0; swIndex < numSweep; swIndex++) {

            RadialDatasetSweep.Sweep s1   = sweepVar.getSweep(swIndex);
            int                      rNum = s1.getRadialNumber();
            if (rNum > 360) {
                rNum = 360;
            }

            float[] _swData = s1.readData();

            for (int r = 0; r < rNum; r++) {

                float[] _rayData = new float[numBin];

                for (int b = 0; b < numBin; b++) {
                    _rayData[b] = _swData[r * numBin + b];
                }

                rData[swIndex][r] = _rayData;
            }


            // fill the value of missing radials
            if (rNum < 360) {
                for (int r = rNum; r < 360; r++) {
                    float[] _rayData = new float[numBin];
                    for (int b = 0; b < numBin; b++) {
                        _rayData[b] = Float.NaN;
                    }
                    rData[swIndex][r] = _rayData;
                }
            }

        }

        return rData;
    }

    /**
     * setting the index for each ray and bin of cappi
     *
     * @param sweepVar sweep variable
     * @param az cappi azimuth array
     * @param numRay number of radial
     * @param numSweep number of sweep
     *
     * @return index
     */
    int[][] getRayIndex(RadialDatasetSweep.RadialVariable sweepVar,
                        float[] az, int numRay, int numSweep) {
        int[][] rIndex = new int[numSweep][numRay];


        //  calc the true value of index
        for (int swIndex = 0; swIndex < numSweep; swIndex++) {
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(swIndex);
            //
            float r = s1.getBeamWidth() / 2;

            for (int i = 0; i < 360; i++) {
                float azi = az[i];

                rIndex[swIndex][i] = getClosestRayFromSweep(s1, azi, r);
            }

        }

        return rIndex;
    }

    /**
     * This equation lifted from Dennis Fannigan's rsph.c code.
     * return data in sweep variable within limit (angle) specified
     * in parameter list.  Assume PPI mode.
     * @param gr       - Ground range in km.
     * @param h        - Height of data point above earth, in km.
     * @return  slant_r - slant range, along the beam, in km.
     *          elev    - elevation angle, in degrees.
     *
     */
    float[] getSlantrAndElev(float gr, float h) {
        double  slant_r_2;  /* Slant range squared. */
        double  elev;
        double  slantr;
        float[] sne = new float[2];

        if (gr == 0) {
            sne[0] = h;
            sne[1] = 90.0f;
            return sne;
        }

        h += Re;

        slant_r_2 = Math.pow(Re, 2.0) + Math.pow(h, 2.0)
                    - (2 * Re * h * Math.cos(gr / Re));
        slantr = Math.sqrt(slant_r_2);

        elev = Math.acos((Math.pow(Re, 2.0) + slant_r_2 - Math.pow(h, 2.0))
                         / (2 * Re * (slantr)));
        elev   *= 180.0 / M_PI;
        elev   -= 90.0;

        sne[0] = (float) slantr;
        sne[1] = (float) elev;

        return sne;

    }

    /** sweep list */
    private CDMRadarSweepDB[] RSL_sweep_list = null;

    /** ray data */
    private float[][][] rayData = null;

    /** ray indices */
    private int[][] rayIndex = null;

    /** _more_          */
    private HashMap rhiData = null;

    /**
     * return data in sweep variable within limit (angle) specified
     * in parameter list.  Assume PPI mode.
     * @param table        sweep table
     * @param angle        azi angle
     * @param  rayNum      number of ray
     * @return  the ray object
     *
     */
    CDMRadarSweepDB.Ray hashBin(CDMRadarSweepDB table, float angle,
                                int rayNum) {
        /* Internal Routine to calculate the hashing bin index
         * of a given angle.
         */
        int   hashIndex;
        float res;

        res       = 360.0f / rayNum;
        hashIndex = (int) (angle / res + res / 2.0);  /*Centered about bin.*/

        if (hashIndex >= rayNum) {
            hashIndex = hashIndex - rayNum;
        }

        /* Could test see which direction is closer, but
         * why bother?
         */
        CDMRadarSweepDB.Ray r = table.get(hashIndex);
        while (r == null) {
            hashIndex++;
            if (hashIndex >= rayNum) {
                hashIndex = 0;
            }
            r = table.get(hashIndex);
        }

        return r;
    }

    /**
     * construct the sweep table
     * in parameter list.  Assume PPI mode.
     * @param azi         sweep object
     * @param aziIdx _more_
     * @param b _more_
     * @return  the hash map table for sweep
     *
     */
    CDMRadarSweepDB constructSweepHashTable(float[] azi, int[] aziIdx,
                                            float b) {
        CDMRadarSweepDB sd = null;
        if (azi == null) {
            return null;
        }
        try {
            sd = new CDMRadarSweepDB(azi, aziIdx, b);
        } catch (IOException e) {
            e.printStackTrace();

        }
        return sd;
    }


    /**
     * _more_
     *
     * @param sVar _more_
     * @param elev _more_
     * @param azimuth _more_
     * @param range _more_
     * @param rdata _more_
     *
     * @return _more_
     *
     * @throws IOException _more_
     */
    float getValue(RadialDatasetSweep.RadialVariable sVar, float elev,
                   float azimuth, float range,
                   float[] rdata) throws IOException {

        /*
         * 1. Locate sweep using 'elev'.
         * 2. Call RSL_get_value_from_sweep
         */
        float data = 0;

        // float[]   _swData = s.readData();

        /*    if (rayData[swIndex] == null) {
                rayData[swIndex] = new float[rNum][bNum];
                float[]   _swData = s.readData();

                for (int r = 0; r < rNum; r++) {
                    float[]   _rayData = new float[bNum];
                    for(int b = 0; b < bNum; b++) {
                         _rayData[b] = _swData[r*bNum + b];
                    }
                    rayData[swIndex][r] = _rayData;

                   // rayData[swIndex][r] = s.readData(r);

                }

            }
            float r = s.getBeamWidth() / 2;

            // int   rayIdex = getClosestRayFromSweep(s, azimuth, r);
            float [] _azimuths = s.getAzimuth();

            if (rayIndex[swIndex] == null) {
                rayIndex[swIndex] = new int[rNum];
                for (int i = 0; i < rNum; i++) {
                    float azi = _azimuths[i];
                   // azi = _azimuths[i];  //s.getAzimuth(i);
                    rayIndex[swIndex][i] = getClosestRayFromSweep(s, azi, r);
                }

            }

            int rayIdex = rayIndex[swIndex][rIndex];


                data = getValueFromRay(rdata, range,
                                       gateSize, rangeToFirstGate, rayIdex);
            */

        return data;
    }


    /**
     * Find closest sweep to requested elevation angle.  Assume PPI sweep for
     * now. Meaning: sweep_angle represents elevation angle from
     * 0->90 degrees
     * @param sweepVar      sweep variable object
     * @param sweep_angle   sweep elevation angle
     * @param limit         limit is half beamWidth
     * @return  the sweep index
     *
     */
    int getClosestSweepIndex(RadialDatasetSweep.RadialVariable sweepVar,
                             float sweep_angle, float limit) {
        RadialDatasetSweep.Sweep s;
        int                      i, ci;
        float                    delta_angle = 91;
        float                    check_angle;

        if (sweepVar == null) {
            return -1;
        }

        ci = 0;
        int numberOfSweeps = sweepVar.getNumSweeps();
        for (i = 0; i < numberOfSweeps; i++) {
            s = sweepVar.getSweep(i);
            if (s == null) {
                continue;
            }
            check_angle = (float) Math.abs((double) (s.getMeanElevation()
                    - sweep_angle));

            if (check_angle <= delta_angle) {
                delta_angle = check_angle;
                ci          = i;
            }
        }
        s           = sweepVar.getSweep(ci);

        delta_angle = Math.abs(s.getMeanElevation() - sweep_angle);

        if (delta_angle <= limit) {
            return ci;
        } else {
            return 999;
        }

    }

    /**
     * return closest Ray in Sweep within limit (angle) specified
     * in parameter list.  Assume PPI mode.
     * @param s          sweep
     * @param ray_angle  ray angle
     * @param limit      limit
     * @return  the ray index
     *
     */
    int getClosestRayFromSweep(RadialDatasetSweep.Sweep s, float ray_angle,
                               float limit) {

        CDMRadarSweepDB sweepTable;
        int             closestRay;
        double          close_diff;

        float           beamwidth = limit * 2.f;
        int             i         = s.getSweepIndex();

        sweepTable = RSL_sweep_list[i];

        CDMRadarSweepDB.Ray r = hashBin(sweepTable, ray_angle,
                                        s.getRadialNumber());

        /* Find hash entry with closest Ray */
        if(r == null) return 999;
        int rd = r.rayIndex;
        closestRay = theClosestHash(s, ray_angle, rd, limit);

        /* Is closest ray within limit parameter ? If
         * so return ray, else return NULL.
         */
        if (closestRay == 999) {
            return 999;
        }
        try {
            close_diff = angleDiff(ray_angle, s.getAzimuth(closestRay));
        } catch (IOException e) {
            e.printStackTrace();
            return 999;
        }
        if (close_diff <= beamwidth) {
            return closestRay;
        }

        return 999;
    }

    /**
     * calculate the angle diff  within +/-180
     * @param x         first angle
     * @param y         second angle
     * @return  the diff
     *
     */
    double angleDiff(float x, float y) {
        double d;
        d = Math.abs((double) (x - y));
        if (d > 180) {
            d = 360 - d;
        }
        return d;
    }

    /**
     * Get the data for the ray.
     * @param s          sweep
     * @param ray_angle  ray angle
     * @param hindex     ray index
     * @param limit      limit
     * @return  the ray index
     *
     */
    int theClosestHash(RadialDatasetSweep.Sweep s, float ray_angle,
                       int hindex, float limit) {
        /* Return the hash pointer with the minimum ray angle difference. */

        float   clow, chigh, cclow;
        float   high, low;
        int     rNum      = s.getRadialNumber() - 1;
        float[] _azimuths = null;
        /* Set low pointer to hash index with ray angle just below
             * requested angle and high pointer to just above requested
             * angle.
         */

        /* set low and high pointers to initial search locations*/
        try {
            _azimuths = s.getAzimuth();
        } catch (IOException e) {
            e.printStackTrace();
        }


        if (hindex >= rNum) {
            hindex = (hindex - rNum);
        }
        low  = _azimuths[hindex];
        high = _azimuths[hindex + 1];


        /* Search until clockwise angle to high is less then clockwise
             * angle to low.
             */

        clow  = cwiseAngleDiff(ray_angle, low);
        chigh = cwiseAngleDiff(ray_angle, high);
        cclow = ccwiseAngleDiff(ray_angle, low);

        if (clow == 0) {
            return hindex;
        }
        if (Math.abs(clow) < limit) {
            return hindex;
        }
        if (Math.abs(chigh) < limit) {
            return hindex + 1;
        }


        int t = 999;
        while ((chigh > clow) && (clow != 0)) {
            t = hindex;
            if (clow < cclow) {
                hindex = hindex - 1;
            } else {
                hindex = hindex + 1;
            }


            if (hindex >= rNum) {
                hindex = (hindex - rNum);
            }
            //if(hindex < 0) hindex = rNum;
            //if(hindex >= 360) return 999;
            try {
                if (hindex < 0) {
                    hindex = rNum;
                    low    = _azimuths[hindex];
                    high   = _azimuths[0];
                } else {
                    low  = _azimuths[hindex];
                    high = _azimuths[hindex + 1];
                }
            } catch (ArrayIndexOutOfBoundsException ee) {
                int a = hindex;
                ee.printStackTrace();
            }

            clow  = cwiseAngleDiff(ray_angle, low);
            chigh = cwiseAngleDiff(ray_angle, high);
            cclow = ccwiseAngleDiff(ray_angle, low);
        }

        if (clow == 0) {
            return hindex;
        }

        if (chigh <= cclow) {
            if (t == 0) {
                return 0;
            }
            return hindex + 1;
        } else {
            return hindex;
        }

    }

    /**
     * calculate the clock wise angle diff
     * Returns the counterclockwise angle differnce of x to y.
     * If x = 345 and y = 355 return 10.
     *  If x = 345 and y = 335 return 350
     * @param x         first angle
     * @param y         second angle
     * @return  the diff
     *
     */
    float cwiseAngleDiff(float x, float y) {
        float d;

        d = (y - x);
        if (d < 0) {
            d += 360;
        }
        return d;
    }

    /**
     * calculate the anti clock wise angle diff
     * Returns the counterclockwise angle differnce of x to y.
     * If x = 345 and y = 355 return 350.
     *  If x = 345 and y = 335 return 10
     * @param x         first angle
     * @param y         second angle
     * @return  the diff
     *
     */
    float ccwiseAngleDiff(float x, float y) {
        float d;
        d = (x - y);
        if (d < 0) {
            d += 360;
        }
        return d;
    }

    /**
     * Get the data for the ray.
     * @param rData     sweep
     * @param r         slant range in km
     * @param gate_size gate size
     * @param rangeToFirstGate range to first gate
     * @return  the Data object for the request
     *
     */
    float getValueFromRay(float[] rData, float r, float gate_size,
                          float rangeToFirstGate) {
        int   bin_index;
        float rm;

        rm = r * 1000;
        int nbins = rData.length;  //s.getGateNumber();
        //if (s == null) return Float.NaN;

        //float gate_size = s.getGateSize();

        /* range_bin1 is range to center of first bin */
        bin_index = (int) (((rm - rangeToFirstGate) / gate_size) + 0.5);
        float[] data = new float[nbins];
        /* Bin indexes go from 0 to nbins - 1 */
        if ((bin_index >= nbins) || (bin_index < 0)) {
            return Float.NaN;
        }

        /*    try {
                data = s.readData(ray);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }   */
        return rData[bin_index];
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
    public DataImpl getData(
            DataChoice dataChoice, DataSelection subset,
            Hashtable requestProperties) throws VisADException,
                RemoteException {

        Object      choiceId = dataChoice.getId();
        String      vn;
        ObjectArray choiceAttrs;
        Trace.call1("CDMRA:getData");


        if (choiceId instanceof ObjectArray) {
            choiceAttrs = (ObjectArray) choiceId;
            /*  keep this around in case Yuan remembers why
                new ObjectArray(((ObjectArray) choiceId).getObject1(),
                                ((ObjectArray) choiceId).getObject2(),
                                ((ObjectArray) choiceId).getObject3(),
                                RadarConstants.VALUE_3D);
            */
            vn = choiceAttrs.getObject3().toString();
        } else {
            throw new IllegalStateException("Unknown choice data:"
                                            + choiceId.getClass().getName());
        }

        if (requestProperties == null) {
            requestProperties = (dataChoice.getProperties() != null)
                                ? dataChoice.getProperties()
                                : new Hashtable();
        }

        Object  momentObj = choiceAttrs.getObject1();
        int     moment    = Integer.parseInt(momentObj.toString());
        boolean volume;
        if (isRaster()) {
            volume = false;
        } else {
            volume = VALUE_VOLUME.equals(Misc.getProperty(requestProperties,
                    PROP_VOLUMEORSWEEP, VALUE_VOLUME));
        }
        boolean getAll3DData =
            VALUE_3D.equals((String) choiceAttrs.getObject4());
        boolean in2D = VALUE_2D.equals(Misc.getProperty(requestProperties,
                           PROP_2DOR3D, VALUE_2D));
        FieldImpl fi = null;

        if (volume && getAll3DData) {
            if (requestProperties.containsKey(PROP_CAPPI_LEVEL)) {
                Real level = null;

                if (subset != null) {
                    Object o = subset.getFromLevel();
                    if ((o != null) && (o instanceof Real)) {
                        level = (Real) o;
                    }
                }
                if (level == null) {
                    level = (Real) requestProperties.get(PROP_CAPPI_LEVEL);
                }

                try {
                    fi = getCAPPI(moment, vn, level);
                } catch (IOException ex) {
                    LogUtil.logException("getCAPPI", ex);
                }
            } else if (requestProperties.containsKey(PROP_AZIMUTH)) {
                float rhiAzimuth = ((Float) requestProperties.get(
                                       PROP_AZIMUTH)).floatValue();
                //System.out.println("getting rhi at azimuth " + rhiAzimuth);

                try {
                    fi = getRHI(moment, vn, rhiAzimuth);
                } catch (IOException ex) {
                    LogUtil.logException("getRHI", ex);
                }
            } else {
                try {
                    fi = getVolume(moment, vn);
                } catch (IOException ex) {
                    LogUtil.logException("getVolume", ex);
                }
            }
        } else {
            if (isRaster()) {
                try {
                    fi = getRaster(moment, vn);
                } catch (IOException ex) {
                    LogUtil.logException("getRaster", ex);
                } catch (InvalidRangeException ed) {
                    LogUtil.logException("getRaster", ed);
                }
            } else {
                try {
                    double value = Double.NaN;

                    Double fromProperties;
                    if (subset != null) {
                        Object o = subset.getFromLevel();
                        if ((o != null) && (o instanceof Real)) {
                            value = ((Real) o).getValue();
                        }
                    }
                    if (Double.isNaN(value)) {
                        fromProperties =
                            (Double) requestProperties.get(PROP_ANGLE);
                        if (fromProperties != null) {
                            value = ((Double) fromProperties).doubleValue();

                        } else if (choiceAttrs.getObject2()
                                   instanceof Double) {
                            value =
                                ((Double) choiceAttrs.getObject2())
                                    .doubleValue();
                        }
                    }
                    // check one more time.
                    if (Double.isNaN(value)) {
                        throw new IllegalArgumentException(
                            "No angle specified");
                    }
                    fi = getSweep(moment, value, vn, !in2D);
                } catch (IOException ex) {
                    LogUtil.logException("getSweep", ex);
                }
            }
        }
        Trace.call2("CDMRA:getData");
        //We can clear the cached data the rds holds by doing the following.
        //However, we do it only when we need to clear the global cache
        //        rds.clearDatasetMemory();
        return fi;

    }


    /**
     * Clear the rds data
     */
    public void clearCachedData() {
        if (rds != null) {
            rds.clearDatasetMemory();
        }
    }


    /**
     * Get the data for the given DataChoice and selection criteria.
     * @param vname
     * @return  the real type of request variable
     *
     */
    private RealType getMomentType(String vname) {
        return RealType.getRealType(vname);
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
     * Get the parameters for this adapter
     *
     * @return parameters
     */
    protected RealType[] getParams() {
        return paramTypes;
    }

    /**
     * Makes a field of all data from one common data model radar adapter;
     *
     * @param moment  moment
     * @param varName variable name
     * @param rhiAz   azimuth for RHI
     *
     * @return rhi as a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create VisAD Object
     */
    public FieldImpl getRHIOld(int moment, String varName,
                               double rhiAz) throws VisADException,
                                   RemoteException, IOException {

        Trace.call1("   getRHI");
        Trace.call1("   getRHI.setup");
        RadialDatasetSweep.RadialVariable sweepVar =
            (RadialDatasetSweep.RadialVariable) rds.getDataVariable(varName);
        int       numberOfSweeps = sweepVar.getNumSweeps();
        int       numberOfRay    = getRayNumber(sweepVar);

        FlatField retField;
        double    range_step;
        double    range_to_first_gate;
        int       value_counter = 0;
        double    halfBeamWidth;

        halfBeamWidth = 0.95 / 2;

        int     number_of_bins;
        float[] elevations = new float[numberOfSweeps];
        //int[]   bincount    = new int[numberOfSweeps];
        int[] tiltindices = new int[numberOfSweeps];
        int   bincounter  = 1000;

        if (moment == REFLECTIVITY) {
            bincounter = 500;
        }
        Trace.call2("   getRHI.setup");

        float[][]  values      = new float[numberOfSweeps][bincounter];
        double[][] ranges      = new double[numberOfSweeps][bincounter];
        int        tiltcounter = 0;

        Trace.call1("   getRHI.getdata");
        float[] azimuths = new float[numberOfRay];
        for (int ti = 0; ti < numberOfSweeps; ti++) {
            RadialDatasetSweep.Sweep sp          = sweepVar.getSweep(ti);
            int                      num_radials = sp.getRadialNumber();
            float                    lastAzi     = 0.00f;

            Trace.call1("getting array of azimuths");
            // get this array from the RadialCoordSys
            float[] _azimuths     = sp.getAzimuth();
            float[] _elevations   = sp.getElevation();
            float   meanElevation = sp.getMeanElevation();
            for (int j = 0; j < numberOfRay; j++) {
                if (j < num_radials) {
                    azimuths[j] = _azimuths[j];    //sp.getAzimuth(j);
                    lastAzi     = azimuths[j];
                } else {
                    azimuths[j] = lastAzi + .01f;  //360.0f;
                    lastAzi     = azimuths[j];
                }

            }
            Trace.call2("getting array of azimuths");
            Trace.call1("findClosestRay");
            int j = findClosestRay((float) rhiAz, azimuths);
            Trace.call2("findClosestRay");
            if (j == -1) {
                continue;
            }

            range_to_first_gate = sp.getRangeToFirstGate() / 1000.0;
            range_step          = sp.getGateSize() / 1000.0;
            number_of_bins      = sp.getGateNumber();
            //bincount[ti]        = number_of_bins;

            if (number_of_bins == 0) {
                continue;
            }

            float el;

            if (j < num_radials) {
                el = _elevations[j];               //sp.getElevation(j);
            } else {
                el = meanElevation;                //sp.getMeanElevation();
            }

            if (el == 0.0f) {
                break;
            }

            elevations[ti] = el;

            for (int z = 0; z < ti; z++) {
                if (elevations[z] == el) {
                    break;
                }
            }

            ranges[ti][0] = (range_to_first_gate + range_step / 2);
            float[] data = new float[number_of_bins];

            if (j < num_radials) {
                data = sp.readData(j);
            } else {
                for (int i = 1; i < number_of_bins; i++) {
                    data[i] = Float.NaN;           //360.0f;
                }
            }

            System.arraycopy(data, 0, values[ti], 0, number_of_bins);
            //values[ti][0] = data[0];
            value_counter++;

            for (int bi = 1; bi < number_of_bins; bi++) {
                //values[ti][bi] = data[bi];
                ranges[ti][bi] = ranges[ti][bi - 1] + range_step;
                value_counter++;
            }

            if (number_of_bins < bincounter) {
                for (int bi = number_of_bins; bi < bincounter; bi++) {
                    values[ti][bi] = Float.NaN;
                    ranges[ti][bi] = ranges[ti][bi - 1] + range_step;
                    value_counter++;
                }
            }

            tiltindices[tiltcounter] = ti;
            tiltcounter++;

        }
        Trace.call2("   getRHI.getdata");

        if (value_counter < 1) {
            return null;
        }
        Trace.call1("   getRHI.makeField");

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

            RealTupleType radarDomainType = makeDomainType3D();
            GriddedSet domainSet = new Gridded3DSet(radarDomainType,
                                       domainVals, bincounter, 2,
                                       radarDomainType.getCoordinateSystem(),
                                       new Unit[] {
                                           CommonUnit.meter.scale(1000),
                                           CommonUnit.degree,
                                           CommonUnit.degree }, null, false);
            FunctionType functionType = new FunctionType(radarDomainType,
                                            getMomentType(varName));

            retField = new FlatField(functionType, domainSet,
                                     (CoordinateSystem) null, (Set[]) null,
                                     (moment != REFLECTIVITY)
                                     ? new Unit[] {
                                         CommonUnit.meterPerSecond }
                                     : (Unit[]) null);
            retField.setSamples(signalVals, false);

            if (tc == 0) {
                RealType indexType = RealType.getRealType("integer_index");
                FunctionType fiFunction = new FunctionType(indexType,
                                              retField.getType());
                Integer1DSet intSet = new Integer1DSet(tiltcounter);

                fi = new FieldImpl(fiFunction, intSet);
                fi.setSample(0, retField, false, true);
            } else {
                fi.setSample(tc, retField, false, false);
            }
        }
        Trace.call2("   getRHI.makeField");
        Trace.call2("   getRHI");

        return fi;

    }

    /**
     * Makes a field of all data from one common data model radar adapter;
     *
     * @param moment  moment
     * @param varName variable name
     * @param rhiAz   azimuth for RHI
     *
     * @return rhi as a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create VisAD Object
     */
    public FieldImpl getRHI(int moment, String varName,
                            double rhiAz) throws VisADException,
                                RemoteException, IOException {

        if (rhiAz > 359.5) {
            rhiAz = 0.0;
        }
        Trace.call1("   getRHI");
        Trace.call1("   getRHI.setup");
        RadialDatasetSweep.RadialVariable sweepVar =
            (RadialDatasetSweep.RadialVariable) rds.getDataVariable(varName);
        int       numberOfSweeps = sweepVar.getNumSweeps();
        int       numberOfRay    = 360;  //getRayNumber(sweepVar);
        rhiData = null;
        FlatField retField;
        double    range_step;
        double    range_to_first_gate;
        int       value_counter = 0;
        double    halfBeamWidth;

        halfBeamWidth = 0.95 / 2;

        int     number_of_bins;
        float[] elevations = new float[numberOfSweeps];
        //int[]   bincount    = new int[numberOfSweeps];
        int[] tiltindices = new int[numberOfSweeps];
        int   bincounter  = 1000;

        if (moment == REFLECTIVITY) {
            bincounter = 500;
        }
        Trace.call2("   getRHI.setup");

        float[][]  values      = new float[numberOfSweeps][bincounter];
        double[][] ranges      = new double[numberOfSweeps][bincounter];
        int        tiltcounter = 0;


        float[][]  aziArray    = new float[numberOfSweeps][];
        int[][]    aziArrayIdx = new int[numberOfSweeps][];
        float[]    meanEle     = new float[numberOfSweeps];
        for (int b = 0; b < numberOfSweeps; b++) {
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
            aziArray[b]    = s1.getAzimuth();
            aziArrayIdx[b] = QuickSort.sort(aziArray[b]);
            meanEle[b]     = s1.getMeanElevation();
        }

        if (RSL_sweep_list == null) {
            RSL_sweep_list = new CDMRadarSweepDB[numberOfSweeps];
            // now get the hash map for each sweep contain azi as index and ray information.
            for (int b = 0; b < numberOfSweeps; b++) {
                //   RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
                RSL_sweep_list[b] = constructSweepHashTable(aziArray[b],
                        aziArrayIdx[b], 0.95f);
            }
        }


        Trace.call1("   getRHI.getdata");
        float[] azimuths = new float[numberOfRay];

        if (rhiData == null) {
            getRHIData(sweepVar);
        }
        float beamWidth = 0.95f;
        float res       = 360.0f / numberOfRay;
        /* Check that this makes sense with beam width. */
        if ((res > 2 * beamWidth) && (beamWidth != 0)) {

            res = beamWidth;
        }
        int                      iazim = (int) (rhiAz / res + res / 2.0);

        float[][] rdata = (float[][]) rhiData.get(Integer.toString(iazim));

        RadialDatasetSweep.Sweep s0    = sweepVar.getSweep(0);
        range_to_first_gate = s0.getRangeToFirstGate() / 1000.0;
        range_step          = s0.getGateSize() / 1000.0;
        number_of_bins      = s0.getGateNumber();

        for (int ti = 0; ti < numberOfSweeps; ti++) {

            float meanElevation = meanEle[ti];

            if (number_of_bins == 0) {
                continue;
            }

            float el = meanElevation;

            if (el == 0.0f) {
                break;
            }

            elevations[ti] = el;


            ranges[ti][0] = (range_to_first_gate + range_step / 2);

            System.arraycopy(rdata[ti], 0, values[ti], 0, number_of_bins);

            value_counter++;

            for (int bi = 1; bi < number_of_bins; bi++) {
                //values[ti][bi] = data[bi];
                ranges[ti][bi] = ranges[ti][bi - 1] + range_step;
                value_counter++;
            }

            if (number_of_bins < bincounter) {
                for (int bi = number_of_bins; bi < bincounter; bi++) {
                    values[ti][bi] = Float.NaN;
                    ranges[ti][bi] = ranges[ti][bi - 1] + range_step;
                    value_counter++;
                }
            }

            tiltindices[tiltcounter] = ti;
            tiltcounter++;

        }
        Trace.call2("   getRHI.getdata");

        if (value_counter < 1) {
            return null;
        }
        Trace.call1("   getRHI.makeField");

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

            RealTupleType radarDomainType = makeDomainType3D();
            GriddedSet domainSet = new Gridded3DSet(radarDomainType,
                                       domainVals, bincounter, 2,
                                       radarDomainType.getCoordinateSystem(),
                                       new Unit[] {
                                           CommonUnit.meter.scale(1000),
                                           CommonUnit.degree,
                                           CommonUnit.degree }, null, false);
            FunctionType functionType = new FunctionType(radarDomainType,
                                            getMomentType(varName));

            retField = new FlatField(functionType, domainSet,
                                     (CoordinateSystem) null, (Set[]) null,
                                     (moment != REFLECTIVITY)
                                     ? new Unit[] {
                                         CommonUnit.meterPerSecond }
                                     : (Unit[]) null);
            retField.setSamples(signalVals, false);

            if (tc == 0) {
                RealType indexType = RealType.getRealType("integer_index");
                FunctionType fiFunction = new FunctionType(indexType,
                                              retField.getType());
                Integer1DSet intSet = new Integer1DSet(tiltcounter);

                fi = new FieldImpl(fiFunction, intSet);
                fi.setSample(0, retField, false, true);
            } else {
                fi.setSample(tc, retField, false, false);
            }
        }
        Trace.call2("   getRHI.makeField");
        Trace.call2("   getRHI");

        return fi;


    }

    /**
     * _more_
     *
     * @param sweepVar _more_
     *
     * @throws IOException _more_
     */
    private void getRHIData(
            RadialDatasetSweep.RadialVariable sweepVar) throws IOException {
        int       numberOfSweeps = sweepVar.getNumSweeps();
        int       numberOfRay    = getRayNumber(sweepVar);
        float[]   azimuths       = new float[numberOfRay];

        float[][][] rdata;
        rhiData = new HashMap();

        RadialDatasetSweep.Sweep [] sweep = new RadialDatasetSweep.Sweep[numberOfSweeps];
        float [][] aziAll = new float[numberOfSweeps][numberOfRay];
        int [] radialNumber = new int[numberOfSweeps];
        int [] gateNumber = new int[numberOfSweeps];
        float [] beamWidth = new float[numberOfSweeps];
        rdata = new float[numberOfSweeps][][];
        
        for(int i = 0; i< numberOfSweeps; i++ ) {
            RadialDatasetSweep.Sweep sp = sweepVar.getSweep(i);
            sweep[i] = sp;
            aziAll[i] = sp.getAzimuth();
            int rNumber = sp.getRadialNumber();
            radialNumber[i] = rNumber;
            int gNumber = sp.getGateNumber();
            gateNumber[i] = gNumber;
            beamWidth[i] = sp.getBeamWidth();
            rdata[i] = new float[rNumber][gNumber];
            float[] vdata        = sp.readData();
           // for(int j = 0; j<rNumber; j++) {
           //     rdata[i][j] = sp.  .readData(j);
            //}
            float [][]               _data2       = rdata[i];
            for (int rayIdx = 0; rayIdx < rNumber; rayIdx++) {
                float []               __data2       = _data2[rayIdx];

                for (int gateIdx = 0; gateIdx < gNumber; gateIdx++) {
                        __data2[gateIdx] = vdata[gNumber * rayIdx + gateIdx];
                }

            }
        }

        for (int r = 0; r < 360; r++) {
            float rhiAz = r;
            float [][] gdata = new float[numberOfSweeps][];
           //float[][] gdata = null;
            for (int ti = 0; ti < numberOfSweeps; ti++) {
                RadialDatasetSweep.Sweep sp = sweep[ti];
                int                      num_radials    = radialNumber[ti];
                int                      number_of_bins = gateNumber[ti];
                float                    lastAzi        = 0.00f;


                // get this array from the RadialCoordSys
                float[] _azimuths = aziAll[ti]; //sp.getAzimuth();
                float   f         = beamWidth[ti] / 2;
                gdata[ti] = new float[number_of_bins];

                for (int j = 0; j < numberOfRay; j++) {
                    if (j < num_radials) {
                        azimuths[j] = _azimuths[j];
                        lastAzi     = azimuths[j];
                    } else {
                        azimuths[j] = lastAzi + .01f;
                        lastAzi     = azimuths[j];
                    }

                }


                int j = getClosestRayFromSweep(sp, rhiAz, f);

                if (j == -1) {
                    continue;
                }


                if (j < num_radials) {
                    gdata[ti] = rdata[ti][j]; //sp.readData(j);
                } else {
                    for (int i = 1; i < number_of_bins; i++) {
                        gdata[ti][i] = Float.NaN;  //360.0f;
                    }
                }


            }

            rhiData.put(Integer.toString(r), gdata);
        }

        // return rhiData;
    }

    /**
     * Find the closest index to the given azimuth
     * @param azi azimuth to find
     * @param azimuths  array to search
     * @return index of closest azimuth or -1;
     */
    private int findClosestRay(float azi, float[] azimuths) {
        float minDist = Float.MAX_VALUE;
        int   ray     = -1;
        for (int i = 0; i < azimuths.length; i++) {
            float dist = Math.abs(azi - azimuths[i]);
            if (azi == 0) {
                dist = Math.min(dist, Math.abs(360.f - azimuths[i]));
            }
            if (dist < minDist) {
                minDist = dist;
                ray     = i;
            }
        }
        return ray;
    }

    /**
     *  the number of ray maybe different among sweep;
     *
     *
     *  @param sweepVar
     *
     *  @return the maximum of ray number
     */
    private int getRayNumber(RadialDatasetSweep.RadialVariable sweepVar) {
        int numSweep = sweepVar.getNumSweeps();
        int numRay   = sweepVar.getSweep(0).getRadialNumber();

        if (numSweep == 1) {
            return numRay;
        } else {
            for (int i = 0; i < numSweep; i++) {
                int num = sweepVar.getSweep(i).getRadialNumber();

                if (num > numRay) {
                    numRay = num;
                }
            }
        }

        return numRay;
    }



    /**
     * Get the station ID
     *
     * @return  the station ID
     */
    public String getStationID() {
        return stationID;
    }

    /**
     * Get the parameters for this adapter
     *
     * @return parameters
     */
    public EarthLocation getStationLocation() {
        return radarLocation;
    }

    /**
     * Set the station location
     *
     * @param el  location of the radar
     *
     * @throws VisADException  couldn't create VisAD object
     * @throws RemoteException  couldn't create remote object
     */
    public void setStationLocation(EarthLocation el) throws VisADException,
            RemoteException {
        radarLocation = el;
        if (el instanceof NamedLocation) {
            stationID =
                ((NamedLocation) radarLocation).getIdentifier().getValue();
        }
    }

    /**
     * Get the parameters for this adapter
     *
     * @return parameters
     */
    public String getStationName() {
        return stationName;
    }

    /**
     *  Get the data format name (e.g. Level II, DORADE, etc)
     *
     *  @return  the format name
     */
    public String getDataFormatName() {
        return dataFormatName;
    }

    /**
     * Create the initial spatial domain
     *
     * @param moment
     * @param varName variable name
     *
     * @return The initial spatial domain
     *
     *
     * @throws IOException Problem reading data
     * @throws InvalidRangeException Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException   problem creating domain
     */
    public FlatField getRaster(int moment,
                               String varName) throws VisADException,
                                   RemoteException, IOException,
                                   InvalidRangeException {

        //List cacheKey = Misc.newList(varName, baseTime, stationName);
        ObjectPair cacheKey =
            new ObjectPair(new ObjectPair(radarLocation, baseTime),
                           new ObjectPair(new Integer(moment), varName));

        FlatField retField = (FlatField) getCache(cacheKey);

        if (retField != null) {
            return retField;
        }

        GriddedSet domainSet;
        GeoGrid    geod = gcd.findGridByName(varName);
        //NetcdfDataset nds = gcd.getNetcdfDataset();

        // LOOK! this assumes a product set
        CoordinateAxis xAxis =
            ((GridCoordSys) geod.getCoordinateSystem()).getXaxis();
        CoordinateAxis yAxis =
            ((GridCoordSys) geod.getCoordinateSystem()).getYaxis();
        int              sizeX = (int) xAxis.getSize();
        int              sizeY = (int) yAxis.getSize();

        GridCoordSystem  gcs   = geod.getCoordinateSystem();
        CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();

        Unit             xUnit = Util.parseUnit(xAxis.getUnitsString());
        Unit             yUnit = Util.parseUnit(yAxis.getUnitsString());


        RealType         xType;
        RealType         yType;

        RealTupleType    domainTemplate;

        // get the Projection for this GeoGrid's GridCoordSys
        ProjectionImpl project = geod.getProjection();

        /*
        CoordinateSystem pCS = new CachingCoordinateSystem(
                                   new ProjectionCoordinateSystem(project));
        */
        CoordinateSystem pCS =
            new RadarMapProjection(
                radarLocation.getLatitude().getValue(CommonUnit.degree),
                radarLocation.getLongitude().getValue(CommonUnit.degree),
                sizeX, sizeY);
        // make the proper RealTypes
        /*  if we use a  real projection, we need the x and y units
        xType = DataUtil.makeRealType(xAxis.getName(), xUnit);
        yType = DataUtil.makeRealType(yAxis.getName(), yUnit);
        */
        xType          = DataUtil.makeRealType(xAxis.getName(), null);
        yType          = DataUtil.makeRealType(yAxis.getName(), null);
        domainTemplate = new RealTupleType(xType, yType, pCS, null);


        Object domainTemplateKey = new ObjectPair(geod, "DomainType");
        RealTupleType cachedDomainTemplate =
            (RealTupleType) dataSource.getCache(domainTemplateKey);

        if (cachedDomainTemplate != null) {
            Trace.msg("GeoGridAdapter:using cached domain template:"
                      + cachedDomainTemplate);
            domainTemplate = cachedDomainTemplate;
        } else {
            Trace.msg("GeoGridAdapter:using new domain template:"
                      + domainTemplate);
            dataSource.putCache(domainTemplateKey, domainTemplate);
        }

        ucar.ma2.Array arr = geod.readYXData(0, 0);
        if (yaxis.getCoordValue(0) < yaxis.getCoordValue(1)) {
            arr = arr.flip(0);
        }

        float[][] fieldarray = new float[1][];
        fieldarray[0] = DataUtil.toFloatArray(arr);

        Unit u = getUnit(geod.getVariable().findAttributeIgnoreCase("units"));
        RealType paramType = DataUtil.makeRealType(varName, u);


        /* if we have a real projection, go back to these
        Linear1DSet xSet = makeLinear1DSet((CoordinateAxis1D) xAxis, xType,
                                           xUnit);
        Linear1DSet ySet = makeLinear1DSet((CoordinateAxis1D) yAxis, yType,
                                           yUnit);
        */

        Linear1DSet xSet = new Linear1DSet(xType, 0, sizeX - 1, sizeX);
        Linear1DSet ySet = new Linear1DSet(yType, 0, sizeY - 1, sizeY);
        domainSet = new Linear2DSet(domainTemplate, new Linear1DSet[] { xSet,
                ySet }, (CoordinateSystem) null, (Unit[]) null,
                        (ErrorEstimate[]) null, true);

        FunctionType ffType =
            new FunctionType(((SetType) domainSet.getType()).getDomain(),
                             paramType);
        retField = new FlatField(ffType, domainSet);
        try {
            retField.setSamples(fieldarray, false);
        } catch (RemoteException re) {
            ;
        }  // can't happen here

        putCache(cacheKey, retField);

        return retField;

    }


    /**
     * Make the set
     *
     * @param axis coordinate axis
     * @param type real type
     * @param u unit
     *
     * @return linear1DSet object
     *
     * @throws VisADException Couldn't create VisAD Object
     */
    private Linear1DSet makeLinear1DSet(CoordinateAxis1D axis, RealType type,
                                        Unit u) throws VisADException {
        Trace.call1("GeoGridAdapter.makeLinear1DSet");

        Linear1DSet result =
            new Linear1DSet(type, axis.getCoordValue(0),
                            axis.getCoordValue((int) axis.getSize() - 1),
                            (int) axis.getSize(), (CoordinateSystem) null,
                            new Unit[] { u }, (ErrorEstimate[]) null, true);  // cache the results
        Trace.call2("GeoGridAdapter.makeLinear1DSet");
        return result;
    }

    /**
     * Makes a field of all data from one common data model radar adapter;
     *
     * @param moment moment
     * @param elevation  elevation angle
     * @param varName    variable name
     * @param want3D     true if should return a 3D field
     *
     * @return sweep as a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create VisAD Object
     */
    public FlatField getSweep(int moment, double elevation, String varName,
                              boolean want3D) throws VisADException,
                                  RemoteException, IOException {

        Trace.call1(" getSweep " + elevation);

        ObjectPair cacheKey = new ObjectPair(
                                  new ObjectPair(
                                      new ObjectPair(
                                          radarLocation,
                                          baseTime), new ObjectPair(
                                              new Integer(moment),
                                              new Double(
                                                  elevation))), new Boolean(
                                                      want3D));
        FlatField retField = (FlatField) getCache(cacheKey);

        if (retField != null) {
            Trace.call2(" getSweep " + elevation);
            return retField;
        }

        int sweepNum = getSweepNumber(varName, elevation);
        if (sweepNum < 0) {
            // System.out.println("couldn't find sweep at " + elevation);
            Trace.call2(" getSweep " + elevation);
            return null;
        }
        RadialDatasetSweep.RadialVariable sweepVar =
            (RadialDatasetSweep.RadialVariable) rds.getDataVariable(varName);
        RadialDatasetSweep.Sweep varSweep   = sweepVar.getSweep(sweepNum);
        int                      numRadials = varSweep.getRadialNumber();
        int                      numGates   = varSweep.getGateNumber();
        double                   range_step = varSweep.getGateSize();
        double range_to_first_gate = varSweep.getRangeToFirstGate()
                                     + 0.5f * range_step;
        float[]   elevations;  //new float[numRadials];
        float[]   azimuths;    //  = new float[numRadials];
        int       npix   = (numRadials+2) * numGates;    // add two additional rays
        float[][] values = new float[1][npix];

        //  for (int azi = 0; azi < numRadials; azi++) {
        azimuths = varSweep.getAzimuth();  //.getAzimuth(azi);
        //  }
        float[]                  _azimuths  = azimuths;

        for (int rayIdx = 0; rayIdx < numRadials; rayIdx++) {
            float azimuth = azimuths[rayIdx];
            if (Float.isNaN(azimuth)) {
                   azimuth = 361.f;
            }
            _azimuths[rayIdx] = azimuth;
        }

        int[] sortedAzs = QuickSort.sort(azimuths);

        // for (int eli = 0; eli < numRadials; eli++) {
        elevations = varSweep.getElevation();  //.getElevation(eli);
        //  }


        float[] rawValues = varSweep.readData();

        for (int samp = 0; samp < npix; samp++) {
            if (values[0][samp] == Float.MAX_VALUE) {
                values[0][samp] = Float.NaN;
            }
        }
        // add two additional radials at the begin and the end of each sweep
        float[][] domainVals3d = new float[3][(numRadials+2 ) * numGates];
        float[][] domainVals2d = new float[2][];

        int       l            = 0;
        // additional radial at the begining
        int ray0 = 0;
        float az0 = azimuths[ray0] - 0.5f;
        if ( az0 < 0) az0 = 0f;
        for (int cell = 0; cell < numGates; cell++) {
            int elem = sortedAzs[ray0] * numGates + cell;
            domainVals3d[0][l] = cell;
            domainVals3d[1][l] = az0;
            domainVals3d[2][l] = elevations[sortedAzs[ray0]];
            values[0][l++]     = rawValues[elem];
        }

        for (int ray = 0; ray < numRadials; ray++) {
            for (int cell = 0; cell < numGates; cell++) {
                int elem = sortedAzs[ray] * numGates + cell;

                domainVals3d[0][l] = cell;
                domainVals3d[1][l] = azimuths[ray];
                domainVals3d[2][l] = elevations[sortedAzs[ray]];
                values[0][l++]     = rawValues[elem];
            }
        }

        // additional radial at the end of the sweep
        int rayN = numRadials - 1;
        float azN = azimuths[rayN] + 0.5f;
        if ( azN > 360) azN = 360f;
        for (int cell = 0; cell < numGates; cell++) {
            int elem = sortedAzs[rayN] * numGates + cell;
            domainVals3d[0][l] = cell;
            domainVals3d[1][l] = azN;
            domainVals3d[2][l] = elevations[sortedAzs[rayN]];
            values[0][l++]     = rawValues[elem];
        }
        //
        // just ranges and azimuths for 2D
        //
        domainVals2d[0] = domainVals3d[0];
        domainVals2d[1] = domainVals3d[1];

        Unit[] domUnits3d = new Unit[] { CommonUnit.meter, CommonUnit.degree,
                                         CommonUnit.degree };
        Unit[] domUnits2d = new Unit[] { CommonUnit.meter,
                                         CommonUnit.degree };
        Unit u = getUnit(sweepVar);

        makeDomainTypes((float) range_step, (float) range_to_first_gate);

        RealTupleType tt  = (want3D)
                            ? radarDomain3d
                            : radarDomain2d;
        GriddedSet    set = (want3D)
                            ? (GriddedSet) new Gridded3DSet(tt, domainVals3d,
                                numGates, numRadials+2,
                                (CoordinateSystem) null, domUnits3d,
                                (ErrorEstimate[]) null, false)
                            : (GriddedSet) new Gridded2DSet(tt, domainVals2d,
                                numGates, numRadials+2,
                                tt.getCoordinateSystem(), domUnits2d,
                                (ErrorEstimate[]) null, false, false);
        FunctionType sweepType = new FunctionType(tt, getMomentType(varName));

        retField = new FlatField(sweepType, set, (CoordinateSystem) null,
                                 (Set[]) null, new Unit[] { u });
        retField.setSamples(values, null, false);

        putCache(cacheKey, retField);

        Trace.call2(" getSweep " + elevation);

        return retField;

    }

    /**
     * Get the sweep number for the particular variable and elevation angle
     *
     * @param vname   variable name
     * @param angle   elevation angle
     *
     * @return the index of the sweep in the dataset
     */
    public int getSweepNumber(String vname, double angle) {
        double[] angles = (double[]) anglesMap.get(vname);
        // Misc.printArray("angles" , angles);
        for (int i = 0; i < angles.length; i++) {
            // TODO: figure a better way to do this
            // Since angles vary from file to file, find closest
            // if (angles[i] == angle) {
            if (Math.abs(angles[i] - angle) < .05) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get the unit for a particular variable
     *
     * @param v   variable
     *
     * @return corresponding unit
     */
    private Unit getUnit(RadialDatasetSweep.RadialVariable v) {
        Attribute a        = v.findAttributeIgnoreCase("units");
        String    unitName = "";

        if (a != null) {
            unitName = a.getStringValue();
        } else {
            System.out.println("no unit for variable " + v);
        }

        if (unitName.equalsIgnoreCase("dbZ")) {
            return null;
        }

        Unit u;

        try {
            u = Util.parseUnit(unitName);
        } catch (Exception pe) {
            u = null;
            pe.printStackTrace();
        }

        return u;
    }

    /**
     * Get the unit for a particular attribute
     *
     * @param a attribute
     *
     * @return corresponding unit
     */
    private Unit getUnit(Attribute a) {

        String unitName = "";

        if (a != null) {
            unitName = a.getStringValue();
        } else {
            System.out.println("no unit for variable ");
        }

        if (unitName.equalsIgnoreCase("dbZ")) {
            return null;
        }
        if (unitName.equalsIgnoreCase("MetersPerSecond")) {
            unitName = "m/s";
        }
        Unit u;

        try {
            u = Util.parseUnit(unitName);
        } catch (Exception pe) {
            u = null;
            pe.printStackTrace();
        }

        return u;
    }

    /** for testing */
    static boolean newWay = true;

    /**
     *  Makes a field of all data from one common data model radar adapter;
     *
     *
     * @param moment moment
     * @param varName variable name
     *
     * @return volume as a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create VisAD Object
     */
    public FlatField getVolume(int moment,
                               String varName) throws VisADException,
                                   RemoteException, IOException {

        //        Trace.call1("volume preamble");
        ObjectPair cacheKey =
            new ObjectPair(new ObjectPair(radarLocation, baseTime),
                           new ObjectPair(new Integer(moment),
                                          "range-az vol"));
        FlatField retField = (FlatField) getCache(cacheKey);

        if (retField != null) {
            return retField;
        }



        // String varName = pType.getName();
        RadialDatasetSweep.RadialVariable sweepVar =
            (RadialDatasetSweep.RadialVariable) rds.getDataVariable(varName);
        int       numberOfSweeps = sweepVar.getNumSweeps();
        int       numberOfRay    = getRayNumber(sweepVar);
        int       gates          = 1000;

        if (moment == REFLECTIVITY) {
            gates = 500;
        }

        float[][]  aziArray    = new float[numberOfSweeps][];
        int[][]    aziArrayIdx = new int[numberOfSweeps][];
        float[]    meanEle     = new float[numberOfSweeps];
        for (int b = 0; b < numberOfSweeps; b++) {
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
            aziArray[b]    = s1.getAzimuth();
            aziArrayIdx[b] = QuickSort.sort(aziArray[b]);
            meanEle[b]     = s1.getMeanElevation();
        }

        if (RSL_sweep_list == null) {
            RSL_sweep_list = new CDMRadarSweepDB[sweepVar.getNumSweeps()];
            // now get the hash map for each sweep contain azi as index and ray information.
            for (int b = 0; b < numberOfSweeps; b++) {
                //   RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
                RSL_sweep_list[b] = constructSweepHashTable(aziArray[b],
                        aziArrayIdx[b], 0.95f);
            }
        }

        double[][] ranges     = new double[numberOfSweeps][gates];
        double     range_step = sweepVar.getSweep(0).getGateSize();
        double range_to_first_gate =
            sweepVar.getSweep(0).getRangeToFirstGate();



        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            double[] _ranges = ranges[sweepIdx];
            _ranges[0] = (range_to_first_gate + range_step / 2);
            for (int gateIdx = 1; gateIdx < gates; gateIdx++) {
                _ranges[gateIdx] = (_ranges[gateIdx - 1] + range_step);
            }
        }


        Trace.call1("making arrays",
                    " size=" + (gates * numberOfRay * numberOfSweeps));




        float[][] domainVals =
            new float[3][gates * numberOfRay * numberOfSweeps];
        float[][] signalVals =
            new float[1][gates * numberOfRay * numberOfSweeps];
        Trace.call2("making arrays");


        float[] domainVals0 = domainVals[0];
        float[] domainVals1 = domainVals[1];
        float[] domainVals2 = domainVals[2];

        int[][] rayIndex  = new int[numberOfSweeps][numberOfRay];


        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            RadialDatasetSweep.Sweep sweep      = sweepVar.getSweep(sweepIdx);
            float                     f         = sweep.getBeamWidth() / 2;

            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
                float rhiAz = rayIdx;
                rayIndex[sweepIdx][rayIdx] = getClosestRayFromSweep(sweep, rhiAz, f);

            }

        }



        //        Trace.call2("volume preamble");

        //        Trace.call1("data read");

      //  float [][] azimuth2 = new float[numberOfSweeps][numberOfRay];
        float [][][] data2 = new float[numberOfSweeps][numberOfRay][gates];

        for (int sweepIdx = 0; sweepIdx <numberOfSweeps; sweepIdx++) {
           // float[]                  _azimuths    = azimuths[sweepIdx];
            float [][]               _data2       = data2[sweepIdx];
            RadialDatasetSweep.Sweep sweep = sweepVar.getSweep(sweepIdx);
            int                      gateNum      = sweep.getGateNumber();
            float[]                  vdata        = sweep.readData();
            int                      rayNum     = sweep.getRadialNumber();

            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
             //   azimuth2[sweepIdx][rayIdx] =  rayIdx;
                float []               __data2       = _data2[rayIdx];
                for (int gateIdx = 0; gateIdx < gates; gateIdx++) {
                       __data2[gateIdx] = Float.NaN;
                }
            }

            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
                float []               __data2       = _data2[rayIdx];
                int si = rayIndex[sweepIdx][rayIdx];

                if ( si < rayNum) {
                    for (int gateIdx = 0; gateIdx < gateNum; gateIdx++) {
                            __data2[gateIdx] = vdata[gateNum * si + gateIdx];
                    }
                }
            }
        }

        int     k           = 0;

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
          //  float[]                  _azimuth2    = azimuth2[sweepIdx];
            float                    _elevation2  = meanEle[sweepIdx];
            double[]                 _ranges      = ranges[sweepIdx];
            float [][]               _data2       = data2[sweepIdx];

            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
                 float                  __azimuth    = rayIdx;  //_azimuth2[rayIdx];
                 float []               __data2       = _data2[rayIdx];

                    for (int gateIdx = 0; gateIdx < gates; gateIdx++) {
                        domainVals2[k] = _elevation2;
                        domainVals1[k] = __azimuth;
                        domainVals0[k] = (float) _ranges[gateIdx];
                        signalVals[0][k] = __data2[gateIdx];
                        k++;
                    }
            }
        }


        //        Trace.call2("data read");


        //      Trace.msg("size:" + gates + " " + numberOfRay +" " + numberOfSweeps);

        // sorting all data as azimuths should start from small to large
        //        Trace.call1("making gridded3d set");
        Unit u = getUnit(sweepVar);

        // radarDomain3d  = makeDomainType3D((float)range_step, (float)range_to_first_gate);

        radarDomain3d = makeDomainType3D();


        RealTupleType tt = radarDomain3d;
        Unit[] domUnits3d = new Unit[] { CommonUnit.meter, CommonUnit.degree,
                                         CommonUnit.degree };
        GriddedSet set = new Gridded3DSet(tt, domainVals, gates, numberOfRay,
                                          numberOfSweeps, null, domUnits3d,
                                          null, false, false);
        FunctionType sweepType = new FunctionType(tt, getMomentType(varName));

        retField = new CachedFlatField(sweepType, set,
                                       (CoordinateSystem) null, (Set[]) null,
                                       new Unit[] { u }, signalVals);

        // retField.setSamples(signalVals, false);

        //        Trace.call2("making gridded3d set");

        putCache(cacheKey, retField);
        return retField;
    }


    /**
     * Get the parameters for this adapter
     *
     * @return parameters
     */
    public boolean isRaster() {
        return this.isRaster;
    }


    /**
     * Get the parameters for this adapter
     *
     * @return parameters
     */
    public boolean isVolume() {
        return this.isVolume;
    }


    /**
     * utility caching method
     *
     * @param key key
     * @param object value
     */
    private void putCache(Object key, Object object) {
        if (dataSource != null) {
            dataSource.putCache(key, object);
        }
    }



    /**
     * utility caching method
     *
     * @param key key
     *
     * @return value
     */
    private Object getCache(Object key) {
        return (dataSource == null)
               ? null
               : dataSource.getCache(key);
    }


    /**
     * Test main
     *
     * @param args cmd line args
     *
     * @throws Exception when bad things happen
     */
    public static void mainDorade(String[] args) throws Exception {
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
     * main
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            //            newWay = false;
            long total = 0;
            for (int j = 0; j < 20; j++) {
                long            t1  = System.currentTimeMillis();
                CDMRadarAdapter cra = new CDMRadarAdapter(null, args[i]);
                cra.getVolume(0, "Reflectivity");
                long t2 = System.currentTimeMillis();
                if (j != 0) {
                    total += (t2 - t1);
                    System.err.println("avg:" + (total / j));
                }
            }
        }

    }







}

