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

package ucar.unidata.data.radar;


import ucar.atd.dorade.DoradePARM;
import ucar.atd.dorade.DoradeSweep;

import ucar.ma2.InvalidRangeException;

import ucar.nc2.Attribute;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dods.DODSNetcdfFile;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.RadialDatasetSweep;
//import ucar.nc2.dt.TypedDatasetFactory;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.units.DateUnit;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.unidata.data.*;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.idv.control.ColorRadarVolumeControl;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.VolumeRenderControl;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.util.*;

import ucar.visad.RadarMapProjection;
import ucar.visad.Util;


import ucar.visad.data.*;

import ucar.visad.quantities.CommonUnits;
import visad.*;


import visad.Set;


import visad.bom.Radar2DCoordinateSystem;
import visad.bom.Radar3DCoordinateSystem;

import visad.data.*;

import visad.georef.*;

import java.io.IOException;

import java.rmi.RemoteException;

import java.util.*;

import static ucar.unidata.idv.IdvConstants.PREF_SAMPLINGMODE;


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

    /** data format */
    private Attribute format = null;

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

    /** flag for a RHI file */
    private boolean isRHI;


    /** sweep list */
    private CDMRadarSweepDB[] RSL_sweep_list = null;

    /** ray data */
    private float[][][] rayData = null;

    /** ray indices */
    private int[][] rayIndex = null;

    /** _more_ */
    private HashMap rhiData = null;

    /** _more_ */
    private float[] meanEle = null;

    /** _more_ */
    private HashMap cutmap = null;

    /** _more_ */
    private double range_step;

    /** _more_ */
    private double range_to_first_gate;

    /** _more_ */
    int number_of_bins;

     double  _searchResAz = 0.1;
     double  _searchResEl = 0.1;
     double  _searchAzOverlapDeg = 20.0;
     double  _searchAzOverlapHalf =  _searchAzOverlapDeg / 2.0;

     double DEG_TO_RAD = 0.017453292519943295;
     double RAD_TO_DEG = 57.29577951308232;
     double TINY_ANGLE = 1.e-4;
     double TINY_FLOAT = 1.e-10;
     double _earthRadiusKm = 6375.636;

    boolean nearest = true;

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
    public CDMRadarAdapter(DataSourceImpl source, String fileName)
            throws VisADException {
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
     * Get the radar location from the dataset.
     * @return EarthLocation.
     */
    public EarthLocation getRadarStationInFile() {
        Attribute latAttr =
            rds.findGlobalAttributeIgnoreCase("RadarLatitude");
        if (latAttr == null) {
            latAttr = rds.findGlobalAttributeIgnoreCase("StationLatitude");
        }
        Attribute lonAttr =
            rds.findGlobalAttributeIgnoreCase("RadarLongitude");
        if (lonAttr == null) {
            lonAttr = rds.findGlobalAttributeIgnoreCase("StationLongitude");
        }
        Attribute altAttr =
            rds.findGlobalAttributeIgnoreCase("RadarAltitude");
        if (altAttr == null) {
            altAttr =
                rds.findGlobalAttributeIgnoreCase("StationElevationInMeters");
        }

        if ((latAttr != null) && (lonAttr != null) && (altAttr != null)) {
            double latitude  = latAttr.getNumericValue().doubleValue();
            double longitude = lonAttr.getNumericValue().doubleValue();
            double altitude  = altAttr.getNumericValue().doubleValue();
            if ((latitude == 0.0) && (longitude == 0.0)) {
                return null;
            }
            EarthLocation elt = new EarthLocationLite(
                                    new Real(RealType.Latitude, latitude),
                                    new Real(RealType.Longitude, longitude),
                                    new Real(RealType.Altitude, altitude));
            return elt;

        } else {
            return null;
        }

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
            if(swpFileName.endsWith("entry.das"))
                rds = (RadialDatasetSweep) FeatureDatasetFactoryManager.open(
                    ucar.nc2.constants.FeatureType.RADIAL, DODSNetcdfFile.canonicalURL(swpFileName), null,
                        new Formatter());
            else
                rds = (RadialDatasetSweep) FeatureDatasetFactoryManager.open(
                        ucar.nc2.constants.FeatureType.RADIAL, swpFileName, null,
                        new Formatter());

            Trace.call2("CDMRadarAdapter:open dataset");
            if(rds.getRadarName() != "XXX") {
                stationID = rds.getRadarID();
                stationName = rds.getRadarName();
            } else {
                stationID = "XXX";
                stationName = "XXX";
            }
            isVolume       = rds.isVolume();
            dataFormatName = rds.getDataFormat();
            Attribute sweepMode =
                rds.findGlobalAttributeIgnoreCase("SweepMode");
            Attribute vcpAttr = rds.findGlobalAttributeIgnoreCase(
                                    "VolumeCoveragePatternName");
            EarthLocation elf = getRadarStationInFile();
            if (elf != null) {
                if (radarLocation instanceof NamedLocation) {
                    String sID =
                        ((NamedLocation) radarLocation).getIdentifier()
                            .getValue();
                    if (stationID.equalsIgnoreCase(sID)) {
                        radarLocation = elf;
                    }
                }

            }

            if (vcpAttr != null) {
                vcp = vcpAttr.getStringValue();
            }  // else {
            if (vcp == null) {
                vcp = "unknown";
            }
            //System.out.println("vcp = " + vcp);
            Attribute attr = rds.findGlobalAttributeIgnoreCase("isRadial");


            if (sweepMode != null) {
                Number nmode = sweepMode.getNumericValue();
                String smode = sweepMode.getStringValue();
                if (nmode != null) {
                    int mode = sweepMode.getNumericValue().intValue();
                    if (mode == 3) {
                        isRHI = true;
                    }
                } else if (smode != null) {
                    if (smode.equals("3")) {
                        isRHI = true;
                    }
                }
            } else if (vcp.equalsIgnoreCase("RHI")) {
                isRHI = true;
            }

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
                            //RealType.getRealType(gedVar.getName(), u);
                            DataUtil.makeRealType(gedVar.getName(), u);
                            //System.out.println("param = " + paramTypes[0].prettyString() + " unit: " + paramTypes[0].getDefaultUnit());
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
            format = rds.findGlobalAttributeIgnoreCase("format");

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


            baseTime = new DateTime(rds.getCalendarDateStart().toDate());

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

                    paramTypes[p] =
                    //RealType.getRealType(radVar.getName(), u);
                    DataUtil.makeRealType(radVar.getName(), u);
                    //System.out.println("param = " + paramTypes[p].prettyString() + " unit: " + paramTypes[p].getDefaultUnit());
                    angles = new double[nsweep];
                    if (isRHI) {
                        for (int i = 0; i < nsweep; i++) {
                            angles[i] = Misc.getAverage(
                                radVar.getSweep(i).getAzimuth());
                        }
                    } else {
                        for (int i = 0; i < nsweep; i++) {
                            angles[i] = radVar.getSweep(i).getMeanElevation();
                        }
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
    private RealTupleType makeDomainType2D(float cellSpacing,
                                           float centerOfFirstCell)
            throws VisADException {
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
    private RealTupleType makeDomainType3D(float cellSpacing,
                                           float centerOfFirstCell)
            throws VisADException {
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
    private void makeDomainTypes(float cellSpacing, float centerOfFirstCell)
            throws VisADException {
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
    private FieldImpl getCAPPIOld(int moment, String varName, Real level)
            throws VisADException, RemoteException, IOException {

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
            getRadialVariable(varName);
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
        Unit u = getUnit(sweepVar);
        FunctionType sweepType = new FunctionType(tt,
                                     getMomentType(varName, u));
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
    public FieldImpl getCAPPI(int moment, String varName, Real level)
            throws VisADException, RemoteException, IOException {

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
            getRadialVariable(varName);


        Object[] cut = getCutIdx(sweepVar);

        /* using sw0 information to construct the CAPPI*/

        int                      numSweep   = cut.length;
        int                      swIdx      = numSweep - 1;
        RadialDatasetSweep.Sweep sw0        = sweepVar.getSweep(swIdx);
        int                      numRay     = 360;
        int                      numBin     = getGateNumber(sweepVar);
        int                      numberOfRay = getRayNumber(sweepVar);
        float                    beamWidth  = sw0.getBeamWidth();
        float                    gateSize   = sw0.getGateSize();
        float                    range_step = sw0.getGateSize();
        float range_to_first_gate           = sw0.getRangeToFirstGate();

        // now get the hash map for each sweep contain azi as index and ray information.
        Trace.call1("   sweep list");
        if (RSL_sweep_list == null) {
            RSL_sweep_list = new CDMRadarSweepDB[numSweep];

            float[][] myAziArray  = new float[numSweep][];
            int[][]   aziArrayIdx = new int[numSweep][];
            for (int b = 0; b < numSweep; b++) {
                int sb = Integer.parseInt(cut[b].toString());
                RadialDatasetSweep.Sweep s1    = sweepVar.getSweep(sb);
                float[]                  tmpAz;
                if (format != null && format.toString().contains("AR2V"))
                    tmpAz =  getAzimuth(s1);
                else
                    tmpAz =  s1.getAzimuth();

                myAziArray[b]  = tmpAz.clone();
                aziArrayIdx[b] = QuickSort.sort(myAziArray[b]);
                if (s1.getRadialNumber() < numberOfRay)
                    numberOfRay = s1.getRadialNumber();
            }
            for (int b = 0; b < numSweep; b++) {
                //   RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
                RSL_sweep_list[b] = constructSweepHashTable(myAziArray[b],
                        aziArrayIdx[b], beamWidth);
            }
        }
        Trace.call2("   sweep list");

        // setting cappi azi value for each ray.
        float[] az = new float[numRay];
        for (int i = 0; i < numRay; i++) {
            az[i] = i;
        }

        int rayNum = getRayNumber(sweepVar);
        if (rayIndex == null) {
            rayIndex = getRayIndex(sweepVar, az, numRay);
        }

        if (rayData == null) {
            rayData = getRayData(sweepVar, numberOfRay, numBin);
            if (rayData == null) {
                return null;
            }
        }

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
            sweepI[b] = getClosestSweepIndex(slantrElev[b][1],
                                             beamWidth / 2.f);
        }


        // ranges for cappi
        ranges[0] = (range_to_first_gate + range_step / 2);
        for (int i = 1; i < numBin; i++) {
            ranges[i] = ranges[i - 1] + range_step;
        }

        Trace.call1("   get cappi value");


        //   if (cutmap == null) {
        cutmap = new HashMap();
        for (int b = 0; b < numSweep; b++) {
            int sb = Integer.parseInt(cut[b].toString());
            cutmap.put(sb, b);
        }
        //   }
        // get the cappi value for each ray and bin
        for (int a = 0; a < numRay; a++) {
            float azi = az[a];  //sw0.getAzimuth(a);
            cappiAz[a] = azi;

            for (int b = 0; b < numBin; b++) {
                int swIndex = sweepI[b];
                if ((swIndex == 999) || !cutmap.containsKey(swIndex)) {
                    cappiValue[b][a] = Float.NaN;
                } else {
                    int cutIdx =
                        Integer.parseInt(cutmap.get(swIndex).toString());
                    int rayIndx = rayIndex[cutIdx][a];
                    if ((rayIndx == 999) || (rayIndx >= numberOfRay)) {
                        cappiValue[b][a] = Float.NaN;
                    } else {
                        float[] rdata = rayData[cutIdx][rayIndx];
                        cappiValue[b][a] = getValueFromRay(rdata,
                                slantrElev[b][0], gateSize,
                                range_to_first_gate);
                    }
                }

            }
        }
        Trace.call2("   get cappi value");

        // int[]     sortedAzs  = QuickSort.sort(cappiAz);

        float[][] domainVals = new float[2][numBin * numRay];
        float[][] signalVals = new float[1][numBin * numRay];
        int       k          = 0;

        for (int azi = 0; azi < numRay; azi++) {
            for (int ri = 0; ri < numBin; ri++) {
                domainVals[0][k] = (float) ranges[ri];
                domainVals[1][k] = cappiAz[azi];
                signalVals[0][k] = cappiValue[ri][azi];
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
        Unit u = getUnit(sweepVar);
        FunctionType sweepType = new FunctionType(tt,
                                     getMomentType(varName, u));
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
     * @param numberOfRay number of radial, 360
     * @param numBin gate number
     *
     * @return 3d volume data
     *
     * @throws IOException  problem reading data
     */
    float[][][] getRayData(RadialDatasetSweep.RadialVariable sweepVar,
                           int numberOfRay, int numBin)
            throws IOException {

        Object[] cut            = getCutIdx(sweepVar);
        int      numberOfSweeps = cut.length;
        //float[][][] rData = new float[numberOfSweeps][numberOfRay][numBin];
        int[] rNum = new int[numberOfSweeps];

        // get data for the whole volume
        float[] allData = null;
        try {
            allData = sweepVar.readAllData();
        } catch (Exception np) {
            LogUtil.consoleMessage("Radar read volume data error in file:\n"
                                   + swpFileName);
            return null;
        }

        float[][][] data2 = new float[numberOfSweeps][numberOfRay][numBin];

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            float[][]                _data2 = data2[sweepIdx];
            int sb = Integer.parseInt(cut[sweepIdx].toString());
            RadialDatasetSweep.Sweep s1     = sweepVar.getSweep(sb);
            int rnumber = s1.getRadialNumber();
            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
                //int si = rayIndex[sweepIdx][rayIdx];
                float[] __data2 = _data2[rayIdx];
                //int     rnumber = rNum[sweepIdx];
                if (rayIdx < rnumber) {
                    for (int gateIdx = 0; gateIdx < numBin; gateIdx++) {
                        __data2[gateIdx] =
                            allData[numBin * numberOfRay * sb + numBin * rayIdx + gateIdx];
                    }
                } else {
                    _data2[rayIdx] = getFloatNaN(numBin);
                }
            }

        }

        return data2;

    }

    /**
     * setting the index for each ray and bin of cappi
     *
     * @param sweepVar sweep variable
     * @param az cappi azimuth array
     * @param numRay number of radial
     *
     * @return index
     */
    int[][] getRayIndex(RadialDatasetSweep.RadialVariable sweepVar,
                        float[] az, int numRay) {

        Object[] cut      = getCutIdx(sweepVar);
        int      numSweep = cut.length;
        int[][]  rIndex   = new int[numSweep][360];
        meanEle = new float[numSweep];

        //  calc the true value of index
        for (int sIndex = 0; sIndex < numSweep; sIndex++) {
            int swIndex = Integer.parseInt(cut[sIndex].toString());
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(swIndex);
            meanEle[sIndex] = s1.getMeanElevation();
            CDMRadarSweepDB sweepTable = RSL_sweep_list[sIndex];
            float beamWidth = s1.getBeamWidth();
            //

            for (int i = 0; i < 360; i++) {
                float azi = az[i];
                CDMRadarSweepDB.Ray r = hashBin(sweepTable, azi, az.length, beamWidth);
                int   ii  = r.rayIndex;
                rIndex[sIndex][i] = ii;
            }

        }

        return rIndex;
    }


    int[][] getRayIndexOld(RadialDatasetSweep.RadialVariable sweepVar,
                        float[] az, int numRay) {

        Object[] cut      = getCutIdx(sweepVar);
        int      numSweep = cut.length;
        int[][]  rIndex   = new int[numSweep][360];
        meanEle = new float[numSweep];

        //  calc the true value of index
        for (int sIndex = 0; sIndex < numSweep; sIndex++) {
            int swIndex = Integer.parseInt(cut[sIndex].toString());
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(swIndex);
            meanEle[sIndex] = s1.getMeanElevation();

            //
            float   r   = s1.getBeamWidth() / 2;
            float[] azs = null;
            try {
                if(format != null && format.toString().contains("AR2V"))
                    azs = getAzimuth(s1);
                else
                    azs = s1.getAzimuth(); //getAzimuth(s1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < 360; i++) {
                float azi = az[i];
                int   ii  = getClosestRayFromSweep(azi, r, sIndex, azs);
                rIndex[sIndex][i] = ii;
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
                                int rayNum, float beamWidth) {
        /* Internal Routine to calculate the hashing bin index
         * of a given angle.
         */
        int   hashIndex;
        float res;

        res       = 360.0f / rayNum;

        if (beamWidth != 0) {
            res = beamWidth;
        }
        hashIndex = (int) (angle + res / 2.0);  /*Centered about bin.*/

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
     * @param aziIdx       azi index
     * @param b
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
     * Find closest sweep to requested elevation angle.  Assume PPI sweep for
     * now. Meaning: sweep_angle represents elevation angle from
     * 0->90 degrees
     * @param sweep_angle   sweep elevation angle
     * @param limit         limit is half beamWidth
     * @return  the sweep index
     *
     */
    int getClosestSweepIndex(float sweep_angle, float limit) {
        RadialDatasetSweep.Sweep s;
        int                      i, ci;
        float                    delta_angle = 91;
        float                    check_angle;

        if (meanEle == null) {
            return -1;
        }

        //Object [] cut = getCutIdx(sweepVar);

        ci = 0;
        int numberOfSweeps = meanEle.length;
        for (i = 0; i < numberOfSweeps; i++) {
            // int sb =  Integer.parseInt(cut[i].toString());
            // s = sweepVar.getSweep(sb);
            // if (s == null) {
            //    continue;
            //}
            check_angle = (float) Math.abs((double) (meanEle[i]
                    - sweep_angle));

            if (check_angle <= delta_angle) {
                delta_angle = check_angle;
                ci          = i;
            }
        }
        float me = meanEle[ci];

        delta_angle = Math.abs(me - sweep_angle);

        if (delta_angle <= limit) {
            return ci;
        } else {
            return 999;
        }

    }

    /**
     * return closest Ray in Sweep within limit (angle) specified
     * in parameter list.  Assume PPI mode.
     * @param ray_angle  ray angle
     * @param limit      limit
     * @param tableIdx index
     * @param azimuths angle
     * @return  the ray index
     *
     */
    int getClosestRayFromSweep(float ray_angle, float limit, int tableIdx,
                               float[] azimuths) {

        CDMRadarSweepDB sweepTable;
        int             closestRay;
        double          close_diff;

        float           beamwidth = limit * 2.f;
        //int             i         = s.getSweepIndex();

        sweepTable = RSL_sweep_list[tableIdx];

        CDMRadarSweepDB.Ray r = hashBin(sweepTable, ray_angle, azimuths.length, beamwidth);
        //  s.getRadialNumber());

        /* Find hash entry with closest Ray */
        if (r == null) {
            return 999;
        }
        int rd = r.rayIndex;  //Integer.parseInt(r.index);  //.rayIndex;
        closestRay = theClosestHash(azimuths, ray_angle, rd, limit);

        /* Is closest ray within limit parameter ? If
         * so return ray, else return NULL.
         */
        if (closestRay == 999) {
            return 999;
        }

        close_diff = angleDiff(ray_angle, azimuths[closestRay]);

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
     * @param azimuths  azi for this sweep
     * @param ray_angle  ray angle
     * @param hindex     ray index
     * @param limit      limit
     * @return  the ray index
     *
     */
    int theClosestHash(float[] azimuths, float ray_angle, int hindex,
                       float limit) {
        /* Return the hash pointer with the minimum ray angle difference. */

        float clow, chigh, cclow;
        float high, low;
        //int     rNum      = s.getRadialNumber() - 1;
        //float[] _azimuths = null;
        /* Set low pointer to hash index with ray angle just below
             * requested angle and high pointer to just above requested
             * angle.
         */

        /* set low and high pointers to initial search locations*/
        /*
        try {
            _azimuths = getAzimuth(s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
        int rNum = azimuths.length - 1;

        if (hindex >= rNum) {
            hindex = (hindex - rNum);
        }
        low  = azimuths[hindex];
        high = azimuths[hindex + 1];


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
        while ((chigh >= clow) && (clow != 0)) {
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
                    low    = azimuths[hindex];
                    high   = azimuths[0];
                } else {
                    low  = azimuths[hindex];
                    high = azimuths[hindex + 1];
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
    public DataImpl getData(DataChoice dataChoice, DataSelection subset,
                            Hashtable requestProperties)
            throws VisADException, RemoteException {

        Object      choiceId = dataChoice.getId();
        String      rn;  // RealType name
        String      vn;
        String      vrhi;
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
            rn = choiceAttrs.getObject3().toString();
            vn = Util.cleanTypeName(rn);
            //System.out.println("looking for rt " + rn + " var " + vn);
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
        } else if (isRHI) {
            volume = VALUE_VOLUME.equals(Misc.getProperty(requestProperties,
                    PROP_AZIMUTH, VALUE_SWEEP));
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
                    if (o != null) {
                        Object newO = o;
                        if (o instanceof TwoFacedObject) {
                            newO = ((TwoFacedObject) o).getId();
                        }
                        if (newO instanceof Real) {
                            level = ((Real) newO);
                        }
                    }
                }
                if (level == null) {
                    level = (Real) requestProperties.get(PROP_CAPPI_LEVEL);
                }

                try {
                    fi = getCAPPI(moment, rn, level);
                } catch (IOException ex) {
                    LogUtil.logException("getCAPPI", ex);
                }
            } else if (requestProperties.containsKey(PROP_VCS)) {

                LatLonPoint latlonStart =
                    (LatLonPoint) requestProperties.get(PROP_VCS_START);
                LatLonPoint latlonEnd =
                    (LatLonPoint) requestProperties.get(PROP_VCS_END);
                //System.out.println("getting rhi at azimuth " + rhiAzimuth);

                try {
                    fi = getRadarCrossSection(moment, rn, latlonStart,
                            latlonEnd);
                } catch (IOException ex) {
                    LogUtil.logException("getRHI", ex);
                }

            } else if (requestProperties.containsKey(PROP_AZIMUTH)) {
                float rhiAzimuth = ((Float) requestProperties.get(
                        PROP_AZIMUTH)).floatValue();
                //System.out.println("getting rhi at azimuth " + rhiAzimuth);

                try {
                    fi = getRHI(moment, rn, rhiAzimuth);
                } catch (IOException ex) {
                    LogUtil.logException("getRHI", ex);
                }
            } else if (requestProperties.containsKey(ColorRadarVolumeControl.RADAR_VOLUME)) {
                try {
                    fi = getVolume(moment, rn);
                } catch (IOException ex) {
                    LogUtil.logException("getGridVolume", ex);
                }
            } else {
                try {
                     fi = getGridVolume(moment, rn);
                } catch (IOException ex) {
                    LogUtil.logException("getVolume", ex);
                }
            }
        } else {
            if (isRHI()) {
                try {
                    double value = Double.NaN;
                    //System.out.println("AZIMUTH angle is :" );
                    Double fromProperties;
                    fromProperties =
                        (Double) requestProperties.get(PROP_AZIMUTH);
                    if (fromProperties != null) {
                        value = (fromProperties).doubleValue();
                    }
                    if (Double.isNaN(value) && (subset != null)) {
                        Object o = subset.getFromLevel();
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
                            (Double) requestProperties.get(PROP_AZIMUTH);
                        if (choiceAttrs.getObject2() instanceof Double) {
                            value =
                                ((Double) choiceAttrs.getObject2())
                                    .doubleValue();
                        }

                        if (value == -1.0) {
                            value =
                                ((Double) choiceAttrs.getObject2())
                                    .doubleValue();
                        }
                    }
                    if (Double.isNaN(value)) {
                        List   levelsList1 = dataChoice.getAllLevels();
                        String azim        = levelsList1.get(0).toString();
                        value = Float.parseFloat(azim);
                    }
                    // check one more time.
                    if (Double.isNaN(value)) {
                        throw new IllegalArgumentException(
                            "No angle specified");
                    }
                    Object s      = dataChoice.getProperty(PROP_AZIMUTH);
                    int    sIndex = 0;
                    if (s == null) {
                        if (anglesMap.get(vn) == null) {
                            return null;
                        }
                        sIndex = getAngleIdx((double[]) anglesMap.get(vn),
                                             value);
                    } else {
                        sIndex = (int) new Double(s.toString()).doubleValue();
                    }
                    //System.out.println("AZIMUTH angle is :" + value);
                    fi = getRHISweep(moment, value, rn, sIndex, true);
                } catch (IOException ex) {
                    LogUtil.logException("getRhiSweep", ex);
                }
            } else if (isRaster()) {
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
                    Object s      = dataChoice.getProperty(PROP_ANGLE);
                    int    sIndex = 0;
                    if (s == null) {
                        if (anglesMap.get(vn) == null) {
                            return null;
                        }
                        sIndex = getAngleIdx((double[]) anglesMap.get(vn),
                                             value);
                    } else {
                        sIndex = (int) new Double(s.toString()).doubleValue();
                    }
                    fi = getSweep(moment, value, rn, sIndex, !in2D);
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
     * finding the index of an angle
     *
     * @param angles array of angles
     * @param angle input angle
     *
     * @return index of angle or 0
     */
    int getAngleIdx(double[] angles, double angle) {
        int size = angles.length;
        for (int i = 0; i < size; i++) {
            if (angle == angles[i]) {
                return i;
            }
        }
        return 0;
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
     * @param u unit
     * @return  the real type of request variable
     * private RealTupleType getMomentType(String vname) throws VisADException {
     *   //        return RealType.getRealType(vname);
     *   return getMomentType(vname, null);
     *   //return new RealTupleType(RealType.getRealType(vname));
     * }
     *
     * @throws VisADException
     */

    /**
     * Get the data for the given DataChoice and selection criteria.
     * @param vname
     * @return  the real type of request variable
     *
     *
     * @throws VisADException
     */
    private RealTupleType getMomentType(String vname, Unit u)
            throws VisADException {
        //        return RealType.getRealType(vname);
        return new RealTupleType(DataUtil.makeRealType(vname, u));
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
    public FieldImpl getRHIOld(int moment, String varName, double rhiAz)
            throws VisADException, RemoteException, IOException {

        Trace.call1("   getRHI");
        Trace.call1("   getRHI.setup");
        RadialDatasetSweep.RadialVariable sweepVar =
            getRadialVariable(varName);
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
            Unit          u               = getUnit(sweepVar);

            RealTupleType radarDomainType = makeDomainType3D();
            GriddedSet domainSet = new Gridded3DSet(radarDomainType,
                                       domainVals, bincounter, 2,
                                       radarDomainType.getCoordinateSystem(),
                                       new Unit[] {
                                           CommonUnit.meter.scale(1000),
                                           CommonUnit.degree,
                                           CommonUnit.degree }, null, false);
            FunctionType functionType = new FunctionType(radarDomainType,
                                            getMomentType(varName, u));

            retField = new FlatField(functionType, domainSet,
                                     (CoordinateSystem) null, (Set[]) null,
                                     new Unit[] { u });
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
     * @param p1 cross lint start point
     * @param p2 cross lint end point
     *
     * @return  a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws VisADException  Couldn't create VisAD Object
     */
    public FieldImpl getRadarCrossSection(int moment, String varName,
                                          LatLonPoint p1, LatLonPoint p2)
            throws VisADException, IOException {

        Trace.call1("   getRadarCrossSection");
        Trace.call1("   getRadarCrossSection.setup");
        RadialDatasetSweep.RadialVariable sweepVar =
            getRadialVariable(varName);

        Object[] cut           = getCutIdx(sweepVar);
        int      numberOfSweeps = cut.length;
        int      numberOfRay    = getRayNumber(sweepVar);
        int      numberOfBin    = getGateNumber(sweepVar);
        float    beamWidth      = sweepVar.getSweep(0).getBeamWidth();
        float[][] myAziArray  = new float[numberOfSweeps][];
        int[][]   aziArrayIdx = new int[numberOfSweeps][];

        for (int b = 0; b < numberOfSweeps; b++) {
            int sb = Integer.parseInt(cut[b].toString());
            RadialDatasetSweep.Sweep s1   = sweepVar.getSweep(sb);
            float[]                  tmpA;
            if (format != null && format.toString().contains("AR2V"))
                tmpA =  getAzimuth(s1);
            else
                tmpA =  s1.getAzimuth();
            myAziArray[b]  = tmpA.clone();
            aziArrayIdx[b] = QuickSort.sort(myAziArray[b]);
            if (s1.getRadialNumber() < numberOfRay)
                numberOfRay = s1.getRadialNumber();
        }

        if ((p1 == null) || (p2 == null)) {
            p1 = setCrossSectionLinePosition(180.0f);
            p2 = setCrossSectionLinePosition(0.0f);

        }

        if (RSL_sweep_list == null) {
            RSL_sweep_list = new CDMRadarSweepDB[numberOfSweeps];
            // now get the hash map for each sweep contain azi as index and ray information.
            for (int b = 0; b < numberOfSweeps; b++) {
                //   RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
                RSL_sweep_list[b] = constructSweepHashTable(myAziArray[b],
                        aziArrayIdx[b], beamWidth);
            }
        }
        //   rhiData = null;
        FlatField                retField;

        RadialDatasetSweep.Sweep s0 = sweepVar.getSweep(numberOfSweeps - 1);
        range_to_first_gate = s0.getRangeToFirstGate() / 1000.0;
        range_step          = s0.getGateSize() / 1000.0;
        beamWidth           = s0.getBeamWidth();
        double halfBeamWidth;

        halfBeamWidth = beamWidth / 2;

        // int     number_of_bins;
        float[] elevations  = new float[numberOfSweeps];
        int[]   tiltindices = new int[numberOfSweeps];
        float   lat3        = (float) p1.getLatitude().getValue();
        float   lon3        = (float) p1.getLongitude().getValue();
        float   lat4        = (float) p2.getLatitude().getValue();
        float   lon4        = (float) p2.getLongitude().getValue();
        Bearing b1          = getBearing(lat3, lon3);
        Bearing b2          = getBearing(lat4, lon4);

        double  azimuth1    = b1.getAngle();
        double  azimuth2    = b2.getAngle();
        double  deltaAzi    = Math.abs(azimuth1 - azimuth2);
        int     bincounter;
        boolean cs = false;
        if ((deltaAzi <= 181.0) && (deltaAzi >= 179.0)) {
            float dis = (float) (b1.getDistance() + b2.getDistance());
            bincounter = (int) Math.round(dis / range_step) + 1;
        } else if ((deltaAzi >= 0.0) && (deltaAzi <= 5.0)) {
            bincounter = (int) (Math.round(Math.abs(b1.getDistance()
                    - b2.getDistance())) / range_step);
        } else {
            bincounter = (int) Math.round(deltaAzi);
            if (bincounter > 180) {
                bincounter = 360 - bincounter;
                cs = true;
            }
            if(numberOfRay > 361) {
                bincounter = (int)(bincounter * (numberOfRay/360.f));
            }
        }

        Trace.call2("   getRadarCrossSection.setup");

        double[][] ranges       = new double[numberOfSweeps][bincounter];
        int        tiltcounter  = 0;

        int[]      ray0Idx      = new int[numberOfSweeps];
        int[]      ray1Idx      = new int[numberOfSweeps];

        float[]    gateSize     = new float[numberOfSweeps];

        meanEle = new float[numberOfSweeps];

        for (int b = 0; b < numberOfSweeps; b++) {
            int                      sb = Integer.parseInt(cut[b].toString());
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(sb);
            meanEle[b]      = s1.getMeanElevation();
            gateSize[b]     = s1.getGateSize() / 1000.f;
            //float[] azs     = s1.getAzimuth();
            //CDMRadarSweepDB sweepTable = RSL_sweep_list[b];
            // now get the beginning index of each sweep
            ray0Idx[b] = getClosestRayFromSweep((float) azimuth1,
                    (float) halfBeamWidth, b, myAziArray[b]);
            ray1Idx[b] = getClosestRayFromSweep((float) azimuth2,
                    (float) halfBeamWidth, b, myAziArray[b]);
         /*   CDMRadarSweepDB.Ray r1 = hashBin(sweepTable, (float) azimuth1, myAziArray[b].length, s1.getBeamWidth());
            CDMRadarSweepDB.Ray r2 = hashBin(sweepTable, (float) azimuth2, myAziArray[b].length, s1.getBeamWidth());
            ray0Idx[b] = r1.rayIndex;
            ray1Idx[b] = r2.rayIndex;
            */
            //System.out.println("Id 0: " + ray0Idx[b] + " Id 1: " + ray1Idx[b]);
        }


        Trace.call1("   getRadarCrossSection.getdata");


        if (rayData == null) {
            rayData = getRayData(sweepVar, numberOfRay, numberOfBin);
            if (rayData == null) {
                return null;
            }
        }

        float[][] rdata    = new float[numberOfSweeps][bincounter];
        float[][] rAzimuth = new float[numberOfSweeps][bincounter];

        for (int ti = 0; ti < numberOfSweeps; ti++) {

            float meanElevation = meanEle[ti];
            elevations[ti] = meanElevation;

            //       System.out.println("Azimuth " + deltaAzi);
            if ((deltaAzi <= 181.0) && (deltaAzi >= 179.0)) {
                if ((ray0Idx[ti] == 999) || (ray1Idx[ti] == 999)) {
                    continue;
                }
                int ray0 = aziArrayIdx[ti][ray0Idx[ti]];
                int ray1 = aziArrayIdx[ti][ray1Idx[ti]];
                int bincounter2 =
                    (int) (((b2.getDistance()
                        / Math.cos(Math.PI * meanElevation
                            / 180.f) - range_to_first_gate) / range_step) + 1);

                int gateIdx =
                    (int) ((b2.getDistance()
                            / Math.cos(
                                Math.PI * meanElevation
                                / 180.f) - range_to_first_gate) / range_step);
                //        System.out.println("radial " + ray1 + " " + azimuth1 + " " + azimuth2);
                for (int ri = 0; ri < bincounter2; ri++) {
                    rAzimuth[ti][ri] = (float) azimuth1;
                    ranges[ti][ri] = range_to_first_gate
                                     + gateIdx * range_step;
                    //      System.out.println("1-1 ti, ri, gi "+ ti + " " + ray1 + " " + gateIdx);
                    if ((gateIdx >= numberOfBin) || (gateIdx < 0)) {
                        rdata[ti][ri] = Float.NaN;
                    } else {
                        rdata[ti][ri] = rayData[ti][ray0][gateIdx];
                    }
                    gateIdx--;
                }
                gateIdx = 0;
                for (int ri = bincounter2; ri < bincounter; ri++) {
                    rAzimuth[ti][ri] = (float) azimuth2;
                    ranges[ti][ri] = range_to_first_gate
                                     + gateIdx * range_step;
                    //        System.out.println("1-2 ti, ri, gi"+ ti + " " + ray0 + " " + gateIdx);
                    if (gateIdx >= numberOfBin) {
                        rdata[ti][ri] = Float.NaN;
                    } else {
                        rdata[ti][ri] = rayData[ti][ray1][gateIdx];
                    }
                    gateIdx++;
                }

            } else if ((deltaAzi >= 0.0) && (deltaAzi <= 5.0)) {
                // average RHIs
                int     ray0    = aziArrayIdx[ti][ray0Idx[ti]];
                int     rn      = (int) deltaAzi;
                int     ray     = ray0;
                float   dist    = (float) b1.getDistance();
                float   azi     = (float) azimuth1;
                boolean incsign = true;
                if (b1.getDistance() > b2.getDistance()) {
                    incsign = false;
                }

                if (ray == 999) {
                    continue;
                }
                int gateIdx =
                    (int) ((dist / Math.cos(Math.PI * meanElevation / 180.f)
                            - range_to_first_gate) / range_step);
                //        System.out.println("radial " + ray + " " + azimuth1 + " " + azimuth2);
                for (int ri = 0; ri < bincounter; ri++) {
                    float rd  = 0;
                    int   cnt = 0;
                    for (int rj = 0; rj < rn; rj++) {
                        if ((gateIdx < numberOfBin) && (gateIdx > 0)) {
                            int rk = ray + rj;
                            if (rk >= 360) {
                                rk = rk - 360;
                            }
                            rd = rd + rayData[ti][rk][gateIdx];
                            cnt++;
                        }
                    }
                    rAzimuth[ti][ri] = (float) azi;
                    ranges[ti][ri] = range_to_first_gate
                                     + gateIdx * range_step;
                    //       System.out.println("2 ti, ri, gi "+ ti + " " + ray + " " + gateIdx);
                    if (Float.isNaN(rd)) {
                        rdata[ti][ri] = Float.NaN;
                    } else {
                        rdata[ti][ri] = rd / cnt;
                    }
                    if (incsign) {
                        gateIdx++;
                    } else {
                        gateIdx--;
                    }
                }

            } else {
                int     ray0;
                boolean incsign = true;
                ray0 = ray0Idx[ti];
                if (ray0 == 999) {
                    continue;
                }

                if (azimuth1 > azimuth2) {
                    incsign = false;
                }

                //    System.out.println(" Radial "+ bincounter + " " + azimuth1 + " " + azimuth2);


                for (int ri = 0; ri < bincounter; ri++) {
                    int rr;
                    if (cs && incsign) {
                        rr = ray0 - ri;
                        if (rr < 0) {
                            rr = numberOfRay + rr;
                        }
                    } else if (cs) {
                        rr = ray0 + ri;
                        if (rr >= numberOfRay) {
                            rr = rr - numberOfRay;
                        }
                    } else if ( !incsign) {
                        rr = ray0 - ri;
                        if (rr < 0) {
                            rr = numberOfRay + rr;
                        }
                    } else {
                        rr = ray0 + ri;
                        if (rr >= numberOfRay) {
                            rr = rr - numberOfRay;
                        }
                    }
                    float azi = myAziArray[ti][rr];
                    // System.out.println("azimuth  " + azi + "rr   " + rr + "ray0-ri  " + (ray0 - ri));
                    float[] inst = getIntersectionOfRayAndLine(radarLocation,
                                       azi, lat3, lon3, lat4, lon4);
                    Bearing b = getBearing(inst[0], inst[1]);
                    int gateIdx =
                        (int) ((b.getDistance()
                            / Math.cos(Math.PI * meanElevation
                                / 180.f) - range_to_first_gate) / range_step);
                    if (gateIdx < 0) {
                        gateIdx = 0;
                    }
                    rAzimuth[ti][ri] = azi;
                    ranges[ti][ri]   = b.getDistance();
                    //  System.out.println("4 "  + (rr) + "  " + gateIdx);

                    if (gateIdx >= numberOfBin) {
                        rdata[ti][ri] = Float.NaN;
                    } else {
                        if(aziArrayIdx[ti][rr] >= rayData[ti].length){
                            rdata[ti][ri] = Float.NaN;
                        } else {
                            float rd = rayData[ti][aziArrayIdx[ti][rr]][gateIdx];
                            rdata[ti][ri] = rd;
                        }
                    }
                }

            }

            tiltindices[tiltcounter] = ti;
            tiltcounter++;

        }
        Trace.call2("   getRadarCrossSection.getdata");


        Trace.call1("   getRadarCrossSection.makeField");


        // radial data
        float[][] domainVals = new float[3][bincounter * numberOfSweeps];
        float[][] signalVals = new float[1][bincounter * numberOfSweeps];
        int       l          = 0;
        for (int tc = 0; tc < numberOfSweeps; tc++) {
            int ti = tiltindices[tc];
            for (int bi = 0; bi < bincounter; bi++) {
                domainVals[0][l]   = (float) ranges[ti][bi];
                domainVals[1][l]   = rAzimuth[ti][bi];
                domainVals[2][l]   = elevations[ti];
                signalVals[0][l++] = rdata[ti][bi];
            }
        }

        Unit          u               = getUnit(sweepVar);

        RealTupleType radarDomainType = makeDomainType3D();
        GriddedSet domainSet = new Gridded3DSet(radarDomainType, domainVals,
                                   bincounter, numberOfSweeps,
                                   radarDomainType.getCoordinateSystem(),
                                   new Unit[] { CommonUnit.meter.scale(1000),
                CommonUnit.degree, CommonUnit.degree }, null, false);
        FunctionType functionType = new FunctionType(radarDomainType,
                                        getMomentType(varName, u));

        retField = new FlatField(functionType, domainSet,
                                 (CoordinateSystem) null, (Set[]) null,
                                 new Unit[] { u });
        retField.setSamples(signalVals, false);

        return retField;



    }

    /**
     * calculate the bearing of one location to the radar location
     *
     * @param lat input latitude
     * @param lon input longitude
     * @return bearing
     */
    public Bearing getBearing(double lat, double lon) {
        Bearing b1 =
            Bearing.calculateBearing(radarLocation.getLatitude().getValue(),
                                     radarLocation.getLongitude().getValue(),
                                     lat, lon, null);
        return b1;
    }

    /**
     * setting the init crosssection line position with azimuth angle
     *
     * @param azi input azimuth angle
     *
     * @return lat lon point
     *
     * @throws RemoteException
     * @throws VisADException
     */
    public LatLonPoint setCrossSectionLinePosition(float azi)
            throws VisADException, RemoteException {
        float stationLat = (float) radarLocation.getLatitude().getValue();
        float stationLon = (float) radarLocation.getLongitude().getValue();


        LatLonPointImpl lp1 = Bearing.findPoint(stationLat, stationLon, azi,
                                  150.0f, null);


        return new EarthLocationTuple(lp1.getLatitude(), lp1.getLongitude(),
                                      0.0);

    }

    /**
     * calculate the intersection of ray and a line
     *
     * @param radarCenter earthlocation of radar
     * @param azi ray azimuth
     * @param lat3 line latitude of one end
     * @param lon3 line longitude of one end
     * @param lat4 line latitude of another end
     * @param lon4 _more_
     *
     * @return lat lon points
     */
    public float[] getIntersectionOfRayAndLine(EarthLocation radarCenter,
            float azi, float lat3, float lon3, float lat4, float lon4) {
        float           lat1 = (float) radarCenter.getLatitude().getValue();
        float           lon1 = (float) radarCenter.getLongitude().getValue();

        LatLonPointImpl lp = Bearing.findPoint(lat1, lon1, azi, 1000.0f,
                                 null);

        float lat2 = (float) lp.getLatitude();
        float lon2 = (float) lp.getLongitude();

        return getIntersectionOfTwoLines(lat1, lon1, lat2, lon2, lat3, lon3,
                                         lat4, lon4);

    }

    /**
     * alculate the intersection of two lines
     *
     * @param lat1 latitude of line 1
     * @param lon1 longitude of line 1
     * @param lat2 latitude of line 1
     * @param lon2 longitude of line 1
     * @param lat3 latitude of line 2
     * @param lon3 longitude of line 2
     * @param lat4 latitude of line 2
     * @param lon4 longitude of line 2
     *
     * @return lat lon float array
     */
    public float[] getIntersectionOfTwoLines(float lat1, float lon1,
                                             float lat2, float lon2,
                                             float lat3, float lon3,
                                             float lat4, float lon4) {
        // lat1 = a * lon1 + b for line1
        // lat2 = c * lon2 + d for line2
        // first get the a, b, c, d from input
        float a = (lat2 - lat1) / (lon2 - lon1);
        float b = -1 * a * lon1 + lat1;
        float c = (lat4 - lat3) / (lon4 - lon3);
        float d = -1 * c * lon3 + lat3;

        //now cal the intersection
        float   lon0 = (d - b) / (a - c);
        float   lat0 = a * lon0 + b;
        float[] out  = new float[] { lat0, lon0 };

        return out;
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
    public FieldImpl getRHI(int moment, String varName, double rhiAz)
            throws VisADException, RemoteException, IOException {

        if (rhiAz > 359.5) {
            rhiAz = 0.0;
        }
        Trace.call1("   getRHI");
        Trace.call1("   getRHI.setup");
        RadialDatasetSweep.RadialVariable sweepVar =
            getRadialVariable(varName);

        Object[] cut            = getCutIdx(sweepVar);
        int      numberOfSweeps = cut.length;
        int      numberOfRay    = 360;  //getRayNumber(sweepVar);
        FlatField retField;
        int    value_counter = 0;
        double halfBeamWidth;

        halfBeamWidth = 0.95 / 2;
        // int     number_of_bins;
        float[] elevations = new float[numberOfSweeps];
        int[] tiltindices = new int[numberOfSweeps];
        int   bincounter  = 1200;
        int gateNumber0 = getGateNumber(sweepVar);
        Unit u0 = getUnit(sweepVar);
        if (moment == REFLECTIVITY || u0.isConvertible(DataUtil.parseUnit("dBZ"))) {
            if( gateNumber0 > 500)
                bincounter = gateNumber0;
            else
                bincounter = 500;
        }
        Trace.call2("   getRHI.setup");

        float[][]  values      = new float[numberOfSweeps][bincounter];
        double[][] ranges      = new double[numberOfSweeps][bincounter];
        int        tiltcounter = 0;

        if (RSL_sweep_list == null) {
            RSL_sweep_list = new CDMRadarSweepDB[numberOfSweeps];
            float[][] myAziArray  = new float[numberOfSweeps][];
            int[][]   aziArrayIdx = new int[numberOfSweeps][];

            for (int b = 0; b < numberOfSweeps; b++) {
                int sb = Integer.parseInt(cut[b].toString());
                RadialDatasetSweep.Sweep s1   = sweepVar.getSweep(sb);
                float[]                  tmpA;
                if(format!=null && format.toString().contains("AR2V")){
                    tmpA = getAzimuth(s1); // level 2 data
                } else {
                    tmpA = s1.getAzimuth();
                }

                myAziArray[b]  = tmpA.clone();
                aziArrayIdx[b] = QuickSort.sort(myAziArray[b]);

            }
            // now get the hash map for each sweep contain azi as index and ray information.
            for (int b = 0; b < numberOfSweeps; b++) {
                //   RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
                RSL_sweep_list[b] = constructSweepHashTable(myAziArray[b],
                        aziArrayIdx[b], 0.95f);
            }
        }


        Trace.call1("   getRHI.getdata");

        float[] az = new float[numberOfRay];
        for (int i = 0; i < numberOfRay; i++) {
            az[i] = i;
        }

        // from RSL_sweep_list table to index 0 -- 359
        if (rayIndex == null) {
            rayIndex = getRayIndex(sweepVar, az, numberOfRay);
        }

        // use rayIndex to construct data
        if (rhiData == null) {
            getRHIData(sweepVar);
            if (rhiData == null) {
                return null;
            }
        } else {
            if (rhiData.get(sweepVar.getName()) == null) {
                getRHIData(sweepVar);
            }
        }
        float beamWidth = 0.95f;
        float res       = 360.0f / numberOfRay;
        /* Check that this makes sense with beam width. */
        if ((res > 2 * beamWidth) && (beamWidth != 0)) {

            res = beamWidth;
        }
        int iazim = (int) (rhiAz + res / 2.0);

        if (iazim > 359) {
            iazim = 0;
        }
        float[][] rdata = (float[][]) rhiData.get(Integer.toString(iazim));


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


            ranges[ti][0]  = (range_to_first_gate + range_step / 2);

            if (number_of_bins < bincounter) {
                System.arraycopy(rdata[ti], 0, values[ti], 0, number_of_bins);
            } else {
                System.arraycopy(rdata[ti], 0, values[ti], 0, bincounter);
            }

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
            Unit          u               = getUnit(sweepVar);

            RealTupleType radarDomainType = makeDomainType3D();
            GriddedSet domainSet = new Gridded3DSet(radarDomainType,
                                       domainVals, bincounter, 2,
                                       radarDomainType.getCoordinateSystem(),
                                       new Unit[] {
                                           CommonUnit.meter.scale(1000),
                                           CommonUnit.degree,
                                           CommonUnit.degree }, null, false);
            FunctionType functionType = new FunctionType(radarDomainType,
                                            getMomentType(varName, u));

            retField = new FlatField(functionType, domainSet,
                                     (CoordinateSystem) null, (Set[]) null,
                                     new Unit[] { u });
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
     * get RHI data
     *
     * @param sweepVar radar variable
     *
     * @throws IOException
     */
    private void getRHIData(RadialDatasetSweep.RadialVariable sweepVar)
            throws IOException {

        Object[] cut            = getCutIdx(sweepVar);
        int      numberOfSweeps = cut.length;
        int      numberOfRay    = getRayNumber(sweepVar);
        float[]  azimuths       = new float[numberOfRay];
        int      numberOfBin    = getGateNumber(sweepVar);

        rhiData = new HashMap();

        rhiData.put(sweepVar.getName(), sweepVar);
        RadialDatasetSweep.Sweep[] sweep =
            new RadialDatasetSweep.Sweep[numberOfSweeps];
        float[][] aziAll = new float[numberOfSweeps][numberOfRay];
        int[]                    radialNumber = new int[numberOfSweeps];
        int[]                    gateNumber   = new int[numberOfSweeps];
        float[]                  beamWidth    = new float[numberOfSweeps];

        RadialDatasetSweep.Sweep s0 = sweepVar.getSweep(numberOfSweeps - 1);
        range_to_first_gate = s0.getRangeToFirstGate() / 1000.0;
        range_step          = s0.getGateSize() / 1000.0;
        number_of_bins      = getGateNumber(sweepVar);
        for (int i = 0; i < numberOfSweeps; i++) {
            int                      sb = Integer.parseInt(cut[i].toString());
            RadialDatasetSweep.Sweep sp = sweepVar.getSweep(sb);
            sweep[i]        = sp;
            aziAll[i]       = sp.getAzimuth(); //getAzimuth(sp);
            if(sp.getRadialNumber() < numberOfRay)
                numberOfRay = sp.getRadialNumber();
            radialNumber[i] = numberOfRay;  //sp.getRadialNumber();
            gateNumber[i]   = numberOfBin;  //sp.getGateNumber();
            beamWidth[i]    = sp.getBeamWidth();
        }

        // if(rayData == null)
        rayData = getRayData(sweepVar, numberOfRay, numberOfBin);
        if (rayData == null) {
            rhiData = null;
            return;
        }


        for (int r = 0; r < 360; r++) {
            float     rhiAz = r;
            float[][] gdata = new float[numberOfSweeps][];
            //float[][] gdata = null;
            for (int ti = 0; ti < numberOfSweeps; ti++) {
                int                      number_of_bins = gateNumber[ti];
                gdata[ti] = new float[number_of_bins];

                int j = rayIndex[ti][r];
                if (j == -1) {
                    continue;
                }

                if (j < numberOfRay) {
                    gdata[ti] = rayData[ti][j];    //sp.readData(j);
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

                if (num < numRay) {
                    numRay = num;
                }
            }
        }
        if (numRay == 720) {
            numRay = numRay / 2;
        }

        return numRay;
    }

    /**
     *  the number of ray maybe different among sweep;
     *
     *
     *  @param sweepVar
     *
     *  @return the maximum of ray number
     */
    private int getRayNumberMax(RadialDatasetSweep.RadialVariable sweepVar) {
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
     * get radar gate number
     *
     * @param sweepVar radar variable
     *
     * @return int
     */
    private int getGateNumber(RadialDatasetSweep.RadialVariable sweepVar) {
        int numSweep = sweepVar.getNumSweeps();
        int numGate  = sweepVar.getSweep(0).getGateNumber();


        for (int i = 0; i < numSweep; i++) {
            int num = sweepVar.getSweep(i).getGateNumber();

            if (num < numGate) {
                numGate = num;
            }
        }

        return numGate;
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
    public void setStationLocation(EarthLocation el)
            throws VisADException, RemoteException {
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
    public FlatField getRaster(int moment, String varName)
            throws VisADException, RemoteException, IOException,
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
        if (geod == null) {
            throw new VisADException("unable to find variable " + varName);
        }

        // LOOK! this assumes a product set
        CoordinateAxis xAxis =
            ((GridCoordSys) geod.getCoordinateSystem()).getXaxis();
        CoordinateAxis yAxis =
            ((GridCoordSys) geod.getCoordinateSystem()).getYaxis();
        int              sizeX = (int) xAxis.getSize();
        int              sizeY = (int) yAxis.getSize();

        GridCoordSystem  gcs   = geod.getCoordinateSystem();
        CoordinateAxis1D yaxis = (CoordinateAxis1D) gcs.getYHorizAxis();

        Unit             xUnit = DataUtil.parseUnit(xAxis.getUnitsString());
        Unit             yUnit = DataUtil.parseUnit(yAxis.getUnitsString());


        RealType         xType;
        RealType         yType;

        RealTupleType    domainTemplate;

        // get the Projection for this GeoGrid's GridCoordSys
        ProjectionImpl project = geod.getProjection();

        /*
        CoordinateSystem pCS = new CachingCoordinateSystem(
                                   new ProjectionCoordinateSystem(project));
        */

        int xRes = (int) (xAxis.getMaxValue() - xAxis.getMinValue())
                   / (sizeX - 1);

        CoordinateSystem pCS =
            new RadarMapProjection(
                radarLocation.getLatitude().getValue(CommonUnit.degree),
                radarLocation.getLongitude().getValue(CommonUnit.degree),
                sizeX, sizeY, (int) xRes);
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
                                        Unit u)
            throws VisADException {
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
     * @param azimus  azimuth angle
     * @param varName    variable name
     * @param idx index
     * @param want3D     true if should return a 3D field
     *
     * @return sweep as a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create VisAD Object
     */

    public FieldImpl getRHISweep(int moment, double azimus, String varName,
                                 int idx, boolean want3D)
            throws VisADException, RemoteException, IOException {

        Trace.call1(" getRHiSweep " + azimus);

        int sweepNum = idx;  //getSweepNumber(varName, elevation);
        if (sweepNum < 0) {
            // System.out.println("couldn't find sweep at " + elevation);
            Trace.call2(" getSweep " + azimus);
            return null;
        }
        RadialDatasetSweep.RadialVariable sweepVar =
            getRadialVariable(varName);
        RadialDatasetSweep.Sweep varSweep   = sweepVar.getSweep(sweepNum);
        int                      numRadials = varSweep.getRadialNumber();
        int                      numGates   = varSweep.getGateNumber();
        double                   range_step = varSweep.getGateSize();
        double range_to_first_gate = varSweep.getRangeToFirstGate()
                                     + 0.5f * range_step;
        float[] myElevations;  //new float[numRadials];
        // float[]   azimuths;                      //  = new float[numRadials];
        // int npix = (numRadials + 2) * numGates;  // add two additional rays
        float[][] values = new float[numRadials][numGates];

        //  for (int azi = 0; azi < numRadials; azi++) {
        // azimuths = varSweep.getAzimuth();  //.getAzimuth(azi);
        //  }
        //   float[] _azimuths = azimuths;

        //     for (int rayIdx = 0; rayIdx < numRadials; rayIdx++) {
        //        float azimuth = azimuths[rayIdx];
        //        if (Float.isNaN(azimuth)) {
        //            azimuth = 361.f;
        //         }
        //         _azimuths[rayIdx] = azimuth;
        //     }

        //calulate the total azi
        //     float azis   = 0;
        //      float preAzi = azimuths[0];
        /*      for (int rayIdx = 0; rayIdx < numRadials; rayIdx++) {
                  float dif = Math.abs(azimuths[rayIdx] - preAzi);
                  if (dif < 1.5) {
                      azis = azis + dif;
                  }

                  preAzi = azimuths[rayIdx];
              }
              //int[] sortedAzs = QuickSort.sort(azimuths);

              int[] sortedAzs;
              if (azis >= 300) {
                  sortedAzs = QuickSort.sort(azimuths);
              } else {
                  sortedAzs = new int[azimuths.length];
                  for (int i = 0; i < azimuths.length; i++) {
                      sortedAzs[i] = i;
                  }
              }    */
        // for (int eli = 0; eli < numRadials; eli++) {
        float[] tmpElevs = varSweep.getElevation();
        myElevations = tmpElevs.clone();  //.getElevation(eli);
        if (Float.isNaN(myElevations[0])) {
            myElevations[0] = 0.0f;
        }
        for (int rayIdx = 1; rayIdx < numRadials; rayIdx++) {
            if (Float.isNaN(myElevations[rayIdx])) {
                myElevations[rayIdx] = myElevations[rayIdx - 1] + 0.2f;
            }
        }
        int[] sortedEle = QuickSort.sort(myElevations);

        //  }
        for (int eli = 0; eli < numRadials; eli++) {
            values[eli] = varSweep.readData(eli);
        }

        //float[] rawValues = varSweep.readData();

        FlatField  retField;
        FieldImpl  fi            = null;
        int        bincounter    = numGates;
        int        tiltcounter   = numRadials - 1;
        float      rhiAz         = (float) azimus;
        int[]      tiltindices   = new int[numRadials];
        double     halfBeamWidth = 0.95 / 2;;
        double[][] ranges        = new double[numRadials][bincounter];

        for (int ti = 0; ti < numRadials; ti++) {

            ranges[ti][0] = (range_to_first_gate + range_step / 2);

            for (int bi = 1; bi < bincounter; bi++) {
                //values[ti][bi] = data[bi];
                ranges[ti][bi] = ranges[ti][bi - 1] + range_step;
            }
            tiltindices[ti] = ti;
        }

        for (int tc = 0; tc < tiltcounter; tc++) {
            int       ti         = sortedEle[tc];
            float[][] domainVals = new float[3][bincounter * 2];
            float[][] signalVals = new float[1][bincounter * 2];
            //  int       ti         = tiltindices[tc];
            float lowerElev = myElevations[tc] - (float) halfBeamWidth;
            //System.out.println("low " + lowerElev);
            for (int bi = 0; bi < bincounter; bi++) {
                domainVals[0][bi] = (float) ranges[ti][bi];
                domainVals[1][bi] = rhiAz;
                domainVals[2][bi] = lowerElev;
                signalVals[0][bi] = values[ti][bi];
            }

            float upperElev = myElevations[tc] + (float) halfBeamWidth;
            //System.out.println("up " + upperElev);
            for (int bi = 0; bi < bincounter; bi++) {
                domainVals[0][bi + bincounter] = (float) ranges[ti][bi];
                domainVals[1][bi + bincounter] = rhiAz;
                domainVals[2][bi + bincounter] = upperElev;
                signalVals[0][bi + bincounter] = values[ti][bi];
            }
            Unit          u               = getUnit(sweepVar);

            RealTupleType radarDomainType = makeDomainType3D();
            GriddedSet domainSet = new Gridded3DSet(radarDomainType,
                                       domainVals, bincounter, 2,
                                       radarDomainType.getCoordinateSystem(),
                                       new Unit[] { CommonUnit.meter,
                    CommonUnit.degree, CommonUnit.degree }, null, false);
            FunctionType functionType = new FunctionType(radarDomainType,
                                            getMomentType(varName, u));

            retField = new FlatField(functionType, domainSet,
                                     (CoordinateSystem) null, (Set[]) null,
                                     new Unit[] { u });
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

        return fi;

    }

    /**
     * Makes a field of all data from one common data model radar adapter;
     *
     * @param moment moment
     * @param elevation  elevation angle
     * @param varName    variable name
     * @param idx _more_
     * @param want3D     true if should return a 3D field
     *
     * @return sweep as a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create VisAD Object
     */
    public FlatField getSweep(int moment, double elevation, String varName,
                              int idx, boolean want3D)
            throws VisADException, RemoteException, IOException {

        Trace.call1(" getSweep " + elevation);

        ObjectPair cacheKey = new ObjectPair(
                                  new ObjectPair(
                                      new ObjectPair(
                                          radarLocation,
                                          baseTime), new ObjectPair(
                                              new Integer(moment),
                                              new ObjectPair(
                                                  new Double(elevation),
                                                  new Integer(
                                                      idx)))), new Boolean(
                                                          want3D));
        FlatField retField = (FlatField) getCache(cacheKey);

        if (retField != null) {
            Trace.call2(" getSweep " + elevation);
            return retField;
        }

        int sweepNum = idx;  //getSweepNumber(varName, elevation);
        if (sweepNum < 0) {
            // System.out.println("couldn't find sweep at " + elevation);
            Trace.call2(" getSweep " + elevation);
            return null;
        }
        RadialDatasetSweep.RadialVariable sweepVar =
            getRadialVariable(varName);
        if(sweepVar == null)
            return null;
        RadialDatasetSweep.Sweep varSweep   = sweepVar.getSweep(sweepNum);
        int                      numRadials = varSweep.getRadialNumber();
        int                      numGates   = varSweep.getGateNumber();
        double                   range_step = varSweep.getGateSize();
        double range_to_first_gate = varSweep.getRangeToFirstGate()
                                     + 0.5f * range_step;
        float[] elevations;  //new float[numRadials];
        float[] azimuths;    //  = new float[numRadials];


        //  for (int azi = 0; azi < numRadials; azi++) {
        azimuths = varSweep.getAzimuth();  //.getAzimuth(azi);
        //  }
        float[] myAzimuths = azimuths.clone();
        float[] _azimuths  = (float[]) myAzimuths;

        for (int rayIdx = 0; rayIdx < numRadials; rayIdx++) {
            float azimuth = myAzimuths[rayIdx];
            if (Float.isNaN(azimuth)) {
                azimuth = 361.f;
            }
            _azimuths[rayIdx] = azimuth;
        }

        //calulate the total azi
        float azis   = 0;
        float preAzi = myAzimuths[0];
        for (int rayIdx = 0; rayIdx < numRadials; rayIdx++) {
            float dif = Math.abs(myAzimuths[rayIdx] - preAzi);
            if (dif < 1.5) {
                azis = azis + dif;
            }

            preAzi = myAzimuths[rayIdx];
        }
        //int[] sortedAzs = QuickSort.sort(azimuths);
        boolean isSorted = false;
        int[] sortedAzs;
        if (azis >= 300) {
            sortedAzs = QuickSort.sort(myAzimuths);
            isSorted = true;
        } else {
            sortedAzs = new int[myAzimuths.length];
            for (int i = 0; i < myAzimuths.length; i++) {
                sortedAzs[i] = i;
            }
        }
        // for (int eli = 0; eli < numRadials; eli++) {
        elevations = varSweep.getElevation();  //.getElevation(eli);
        //  }

        float[] rawValues = varSweep.readData();

        int     l         = 0;
        // additional radial at the begining
        int   ray0 = 0;
        float az0  = myAzimuths[ray0];
        int   rayN = getRayNumber(myAzimuths);  //numRadials - 1;
        float azN  = myAzimuths[rayN - 1];


        float[][] domainVals3d = new float[3][(rayN) * numGates];
        float[][] domainVals2d = new float[2][];
        int       npix = (rayN) * numGates;  // add two additional rays
        float[][] values       = new float[1][npix];
        // add two additional radials at the begin and the end of each sweep
        if(isSorted) {
            domainVals3d = new float[3][(rayN + 2) * numGates];
            npix = (rayN + 2) * numGates;  // add two additional rays
            values       = new float[1][npix];
        }
        // extend to 0 if first radial is between 0 and 1 degree.
        if( isSorted ) {
            if ((az0 >= 0) && (az0 <= 1.0)) {
                for (int cell = 0; cell < numGates; cell++) {
                    int elem = sortedAzs[ray0] * numGates + cell;
                    domainVals3d[0][l] = cell;
                    domainVals3d[1][l] = 0.f;
                    domainVals3d[2][l] = elevations[sortedAzs[ray0]];
                    values[0][l++]     = rawValues[elem];
                }
            } else if (az0 > 1.0) {
                if ((azN >= 360.0) && (azN <= 361.0)) {
                    for (int cell = 0; cell < numGates; cell++) {
                        int elem = sortedAzs[rayN - 1] * numGates + cell;
                        domainVals3d[0][l] = cell;
                        domainVals3d[1][l] = ((azN - 360 - 0.5f < 0)
                                              ? 0
                                              : azN - 360 - 0.5f);
                        domainVals3d[2][l] = elevations[sortedAzs[rayN - 1]];
                        values[0][l++]     = rawValues[elem];
                    }

                } else {
                    for (int cell = 0; cell < numGates; cell++) {
                        int elem = sortedAzs[ray0] * numGates + cell;
                        domainVals3d[0][l] = cell;
                        domainVals3d[1][l] = az0 - 0.5f;
                        domainVals3d[2][l] = elevations[sortedAzs[ray0]];
                        values[0][l++]     = rawValues[elem];
                    }
                }
            }
        }
        // radial data
        for (int ray = 0; ray < rayN; ray++) {
            for (int cell = 0; cell < numGates; cell++) {
                int elem = sortedAzs[ray] * numGates + cell;

                domainVals3d[0][l] = cell;
                domainVals3d[1][l] = myAzimuths[ray];
                domainVals3d[2][l] = elevations[sortedAzs[ray]];
                values[0][l++]     = rawValues[elem];
            }
        }

        // additional radial at the end of the sweep
        if(isSorted) {
            if ((azN >= 359) && (azN <= 360)) {
                for (int cell = 0; cell < numGates; cell++) {
                    int elem = sortedAzs[rayN - 1] * numGates + cell;
                    domainVals3d[0][l] = cell;
                    domainVals3d[1][l] = 360;
                    domainVals3d[2][l] = elevations[sortedAzs[rayN - 1]];
                    values[0][l++]     = rawValues[elem];
                }

            } else if (azN < 359) {
                for (int cell = 0; cell < numGates; cell++) {
                    int elem = sortedAzs[rayN - 1] * numGates + cell;
                    domainVals3d[0][l] = cell;
                    domainVals3d[1][l] = azN + 0.5f;
                    domainVals3d[2][l] = elevations[sortedAzs[rayN - 1]];
                    values[0][l++]     = rawValues[elem];
                }
            } else if ((azN > 360) && (azN <= 361)) {
                for (int cell = 0; cell < numGates; cell++) {
                    int elem = sortedAzs[rayN - 1] * numGates + cell;
                    domainVals3d[0][l] = cell;
                    domainVals3d[1][l] = 360;
                    domainVals3d[2][l] = elevations[sortedAzs[rayN - 1]];
                    values[0][l++]     = rawValues[elem];
                }
            }
        }
        //check the value
        for (int samp = 0; samp < npix; samp++) {
            if (values[0][samp] == Float.MAX_VALUE) {
                values[0][samp] = Float.NaN;
            }
        }
        //
        // just ranges and azimuths for 2D
        //
        int rayN2 = rayN;
        if(isSorted)
            rayN2 = rayN + 2;
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
                                numGates, rayN2, (CoordinateSystem) null,
                                domUnits3d, (ErrorEstimate[]) null, false)
                            : (GriddedSet) new Gridded2DSet(tt, domainVals2d,
                                numGates, rayN2, tt.getCoordinateSystem(),
                                domUnits2d, (ErrorEstimate[]) null, false,
                                false);

        FunctionType sweepType = new FunctionType(tt,
                                     getMomentType(varName, u));

        //retField = new FlatField(sweepType, set, (CoordinateSystem) null,
        //                         (Set[]) null, new Unit[] { u });
        //retField.setSamples(values, null, false);
        retField = new CachedFlatField(sweepType, set,
                                       (CoordinateSystem) null, (Set[]) null,
                                       new Unit[] { u }, values);


        initCachedFlatField((CachedFlatField) retField);

        putCache(cacheKey, retField);

        Trace.call2(" getSweep " + elevation);

        return retField;

    }


    /**
     * _more_
     *
     * @param cff _more_
     */
    private void initCachedFlatField(CachedFlatField cff) {
        /*
        if (dataSource.getCacheDataToDisk()) {
            String filename = IOUtil.joinDir(dataSource.getDataCachePath(),
                                             Misc.getUniqueId() + ".dat");
            cff.setCacheFile(filename);
            cff.setShouldCache(true);
            cff.setCacheClearDelay(dataSource.getCacheClearDelay());
        } else {
            cff.setShouldCache(false);
        }
        */
    }





    /**
     * get number of ray
     *
     * @param azimuths array
     *
     * @return int
     */
    int getRayNumber(float[] azimuths) {
        int azn = azimuths.length;
        while (azimuths[azn - 1] >= 361) {
            azn--;
        }
        return azn;
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

        Unit u = DataUtil.parseUnit(unitName);

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

        if (unitName.equalsIgnoreCase("MetersPerSecond")) {
            unitName = "m/s";
        }
        Unit u = DataUtil.parseUnit(unitName);

        return u;
    }

    /** for testing */
    static boolean newWay = true;

    /**
     *  get radar sweep index
     *
     * @param sweepVar radar variable
     * @return sweep index array
     */
    public Object[] getCutIdx(RadialDatasetSweep.RadialVariable sweepVar) {

        int       spNum    = sweepVar.getNumSweeps();
        ArrayList eleArray = new ArrayList();
        float     eleLast  = 0.0f;
        if ( !isRHI) {
            for (int i = 0; i < spNum; i++) {
                RadialDatasetSweep.Sweep sp  = sweepVar.getSweep(i);
                float                    ele = sp.getMeanElevation();
                if ((ele != eleLast) && (ele - eleLast) > 0.2) {
                    eleArray.add(i);
                    eleLast = ele;
                }

            }
        } else {
            for (int i = 0; i < spNum; i++) {
                RadialDatasetSweep.Sweep sp  = sweepVar.getSweep(i);
                float                    azi = 0.f;
                try {
                    azi = (float) Misc.getAverage(sp.getAzimuth());
                } catch (java.io.IOException ex) {}
                if ((azi != eleLast) && (Math.abs(azi - eleLast) > 0.2)) {
                    eleArray.add(i);
                    eleLast = azi;
                }

            }
        }
        return eleArray.toArray();
    }

    /**
     * get azimuth array
     *
     * @param s1 sweep
     *
     * @return float array
     *
     * @throws IOException   e
     */
    float[] getAzimuth(RadialDatasetSweep.Sweep s1) throws IOException {
        float[] az    = s1.getAzimuth();
        int     aSize = az.length;
        float[] az1   = new float[aSize / 2];
        if (az.length <= 400) {
            return az;
        } else {
            for (int i = 0; i < aSize / 2; i++) {
                az1[i] = az[i * 2];
            }
            return az1;
        }

    }

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
    public FlatField getVolume(int moment, String varName)
            throws VisADException, RemoteException, IOException {

        //        Trace.call1("volume preamble");
        if (isRHI) {
            return getRHIVolume(moment, varName);
        }

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
            getRadialVariable(varName);
        int      numberOfSweeps;  //= sweepVar.getNumSweeps();
        int      numberOfRay = getRayNumber(sweepVar);
        int      gates       = getGateNumber(sweepVar);
        int      rayNumber   = 360;  // only show 360 rays in all volume displays
        Object[] cut         = getCutIdx(sweepVar);
        numberOfSweeps = cut.length;
        float beamWidth   = sweepVar.getSweep(0).getBeamWidth();

        if (moment == REFLECTIVITY) {
            // gates = 500;
        }

        float[][] myAziArray  = new float[numberOfSweeps][];
        int[][]   aziArrayIdx = new int[numberOfSweeps][];
        float[]   meanEle     = new float[numberOfSweeps];
        for (int b = 0; b < numberOfSweeps; b++) {
            int sb = Integer.parseInt(cut[b].toString());
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(sb);
            float[] tmpAzi;
            if(format!=null && format.toString().contains("AR2V")){
                tmpAzi = getAzimuth(s1);
            } else {
                tmpAzi = s1.getAzimuth();
            }
            myAziArray[b] = tmpAzi.clone();
            aziArrayIdx[b] = QuickSort.sort(myAziArray[b]);
            meanEle[b] = s1.getMeanElevation();
        }

        if (RSL_sweep_list == null) {
            RSL_sweep_list = new CDMRadarSweepDB[numberOfSweeps];
            // now get the hash map for each sweep contain azi as index and ray information.
            for (int b = 0; b < numberOfSweeps; b++) {
                //   RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
                RSL_sweep_list[b] = constructSweepHashTable(myAziArray[b],
                        aziArrayIdx[b], beamWidth);
            }
        }

        double[][] ranges = new double[numberOfSweeps][gates];
        double range_step = sweepVar.getSweep(numberOfSweeps
                                - 1).getGateSize();
        double range_to_first_gate = sweepVar.getSweep(numberOfSweeps
                                         - 1).getRangeToFirstGate();



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
            new float[3][gates * rayNumber * numberOfSweeps];
        float[][] signalVals =
            new float[1][gates * rayNumber * numberOfSweeps];
        Trace.call2("making arrays");


        float[] domainVals0 = domainVals[0];
        float[] domainVals1 = domainVals[1];
        float[] domainVals2 = domainVals[2];

        int[][] rayIndex    = new int[numberOfSweeps][numberOfRay];
        int[]   rNum        = new int[numberOfSweeps];

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            int sb = Integer.parseInt(cut[sweepIdx].toString());
            RadialDatasetSweep.Sweep sweep = sweepVar.getSweep(sb);
            float                    f     = sweep.getBeamWidth() / 2;
            rNum[sweepIdx] = sweep.getRadialNumber();
            CDMRadarSweepDB sweepTable = RSL_sweep_list[sweepIdx];;

            for (int rayIdx = 0; rayIdx < rayNumber; rayIdx++) {
                float rhiAz = rayIdx;
                CDMRadarSweepDB.Ray r = hashBin(sweepTable, rhiAz, rayNumber, sweep.getBeamWidth());
                rayIndex[sweepIdx][rayIdx] = r.rayIndex;
            }

        }

        //        Trace.call2("volume preamble");

        //        Trace.call1("data read");
        float[] allData;
        try {
            allData = sweepVar.readAllData();
        } catch (Exception np) {
            LogUtil.consoleMessage("Radar read volume data error in file:\n"
                                   + swpFileName);
            return null;
        }

        float[][][] data2 = new float[numberOfSweeps][rayNumber][gates];

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            float[][] _data2  = data2[sweepIdx];
            int       sb      = Integer.parseInt(cut[sweepIdx].toString());
            int       rnumber = rNum[sweepIdx];

            for (int rayIdx = 0; rayIdx < rayNumber; rayIdx++) {
                int     si      = rayIndex[sweepIdx][rayIdx];
                float[] __data2 = _data2[rayIdx];
                if (si < rnumber && si < numberOfRay) {
                    for (int gateIdx = 0; gateIdx < gates; gateIdx++) {
                        __data2[gateIdx] =
                            allData[gates * numberOfRay * sb + gates * si + gateIdx];
                    }
                } else {
                    _data2[rayIdx] = getFloatNaN(gates);
                }
            }

        }


        int k = 0;

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            //  float[]                  _azimuth2    = azimuth2[sweepIdx];
            float     _elevation2 = meanEle[sweepIdx];
            double[]  _ranges     = ranges[sweepIdx];
            float[][] _data2      = data2[sweepIdx];

            for (int rayIdx = 0; rayIdx < rayNumber; rayIdx++) {
                float   __azimuth = rayIdx;  //_azimuth2[rayIdx];
                float[] __data2   = _data2[rayIdx];

                for (int gateIdx = 0; gateIdx < gates; gateIdx++) {
                    domainVals2[k]   = _elevation2;
                    domainVals1[k]   = __azimuth;
                    domainVals0[k]   = (float) _ranges[gateIdx];
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
        //System.out.println("unit = " + u);

        // radarDomain3d  = makeDomainType3D((float)range_step, (float)range_to_first_gate);

        radarDomain3d = makeDomainType3D();


        RealTupleType tt = radarDomain3d;
        Unit[] domUnits3d = new Unit[] { CommonUnit.meter, CommonUnit.degree,
                                         CommonUnit.degree };
        GriddedSet set = new Gridded3DSet(tt, domainVals, gates, rayNumber,
                                          numberOfSweeps, null, domUnits3d,
                                          null, false, false);
        FunctionType sweepType = new FunctionType(tt,
                                     getMomentType(varName, u));

        retField = new CachedFlatField(sweepType, set,
                                       (CoordinateSystem) null, (Set[]) null,
                                       new Unit[] { u }, signalVals);

        initCachedFlatField((CachedFlatField) retField);
        // retField.setSamples(signalVals, false);

        //        Trace.call2("making gridded3d set");

        putCache(cacheKey, retField);
        return retField;
    }

    /**
     *  Makes a field of all data from one common data model radar adapter;
     *
     *
     * @param moment moment
     * @param varName variable name
     *
     * @return gridded volume as a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create VisAD Object
     */
    public FieldImpl getGridVolume(int moment, String varName)
            throws VisADException, IOException {

        ObjectPair cacheKey =
                new ObjectPair(new ObjectPair(radarLocation, baseTime),
                        new ObjectPair(new Integer(moment),
                                "grid vol"));
        FlatField retField = (FlatField) getCache(cacheKey);

        if (retField != null) {
            return retField;
        }

        FlatField rfi = getVolumeG(moment, varName);

        //FlatField fi = (FlatField) resampleToLatLonAltGrid(rfi,
        //        moment);
        //FlatField fii = (FlatField) GridUtil.smooth(fi, "SM5S", 6);
        //putCache(cacheKey, fii);
        return rfi;
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
        int xyDim = 100;
        int zDim  = 14;
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
                    domainVals[2][kk] = 1000.0f * zi + 500.0f;
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
     *  Makes a field of all data from one common data model radar adapter;
     *
     *
     * @param moment moment
     * @param varName variable name
     *
     * @return gridded volume as a FieldImpl
     *
     * @throws IOException     Problem reading data
     * @throws RemoteException Java RMI problem
     * @throws VisADException  Couldn't create VisAD Object
     */
    public FlatField getVolumeG(int moment, String varName)
            throws VisADException, IOException {

        int       numCols     = 100;
        int       numRows     = 100;
        int       numLevels     = 14;

        int xyDim = 100;
        int zDim  = 14;
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

        float [] lats = new float[xyDim];
        float [] lons = new float[xyDim];
        float [] levels = new float[zDim];
        for (int i = 0; i < xyDim; i++){
            lats[i] = latLonLocs[0][i*xyDim];
            lons[i] = latLonLocs[1][i];
        }
        for (int zi = 0; zi < zDim; zi++){
            levels[zi] =  1000.0f * zi + 500.0f;
        }

        GridLoc [][][] gridLocs = computeGridRow(numCols, numRows, numLevels, lats, lons, levels );

        RadialDatasetSweep.RadialVariable sweepVar =
                getRadialVariable(varName);

        Object[] cut           = getCutIdx(sweepVar);
        int      numberOfSweeps = cut.length;
        int      numberOfRay    = getRayNumberMax(sweepVar);

        RadialDatasetSweep.Sweep s0 = sweepVar.getSweep(numberOfSweeps - 1);
        range_to_first_gate = s0.getRangeToFirstGate() / 1000.0;
        range_step          = s0.getGateSize() / 1000.0;
        float beamWidth           = s0.getBeamWidth();
        double halfBeamWidth;

        halfBeamWidth = beamWidth / 2;
        double[][] myAziArray  = new double[numberOfSweeps][numberOfRay];
        int[][]   aziArrayIdx = new int[numberOfSweeps][numberOfRay];
        double[] elevs = new double[numberOfSweeps];
        double [][][] allData = new double[numLevels][numRows][numCols];

        Ray[][] allRays = getRays(sweepVar, numberOfRay, elevs, myAziArray);

        for (int b = 0; b < numberOfSweeps; b++) {
            aziArrayIdx[b] =sortFloatArrayWithNaNAndIndices(myAziArray[b]);
            myAziArray[b] = removeNaNfromSortedArray(myAziArray[b]);
        }

        for (int iz = 0; iz < numLevels; iz++) {
            for (int iy = 0; iy < numRows; iy++) {
                double[] rowData = interpGridRow(iz, iy, gridLocs, allRays, elevs, myAziArray, aziArrayIdx);
                allData[iz][iy] = rowData;
            } // iy
        } //

        FlatField                retField;

        float[][] domainVals = new float[3][xyDim * xyDim * zDim];
        float[][] signalVals = new float[1][xyDim * xyDim * zDim];
        int       kk         = 0;
        for (int zi = 0; zi < zDim; zi++) {
            int jj = 0;
            for (int yi = 0; yi < xyDim; yi++) {
                for (int xi = 0; xi < xyDim; xi++) {
                    domainVals[0][kk] = latLonLocs[0][jj];
                    domainVals[1][kk] = latLonLocs[1][jj];
                    domainVals[2][kk] = 1000.0f * zi + 500.0f;
                    jj++;
                    kk++;
                }
            }
        }

        int k = 0;

        for (int zi = 0; zi < zDim; zi++) {
            double[][] _data2      = allData[zi];
            for (int yi = 0; yi < xyDim; yi++) {
                double[] __data2   = _data2[yi];
                for (int xi = 0; xi < xyDim; xi++) {
                    signalVals[0][k] = (float)__data2[xi];
                    k++;
                }
            }
        }


        RealTupleType tt = new RealTupleType(RealType.Latitude,
                RealType.Longitude,
                RealType.Altitude);
        Gridded3DSet g3Dset = new Gridded3DSet(tt, domainVals, xyDim, xyDim,
                zDim);
        Unit          u               = getUnit(sweepVar);
        FunctionType functionType = new FunctionType(tt,
                getMomentType(varName, u));
        retField = new FlatField(functionType, g3Dset,
                (CoordinateSystem) null, (Set[]) null,
                new Unit[] { u });
        retField.setSamples(signalVals, false);


        return retField;

    }
    /**
     * the sorted array may have some NaN at the end of the array
     *
     * @param sortedArr _more_
     */
    public static double[] removeNaNfromSortedArray(double[] sortedArr) {
        int n = sortedArr.length - 1;

        while(Double.isNaN(sortedArr[n])){
           n--;
        }
        double [] noNaNArray = new double[n+1];
        System.arraycopy(sortedArr, 0, noNaNArray, 0, n+1);
        return noNaNArray;
    }

    /**
     * This is api by chatgpt to sort an array and put the NaN at its end
     *
     * @param arr _more_
     */
    public static int[] sortFloatArrayWithNaNAndIndices(double[] arr) {
        // Create an array of indices
        Integer[] indices = new Integer[arr.length];
        for (int i = 0; i < arr.length; i++) {
            indices[i] = i;
        }

        // Sort the indices array based on the corresponding float values in the input array
        Arrays.sort(indices, (a, b) -> {
            if (Double.isNaN(arr[a]) && Double.isNaN(arr[b])) {
                return 0; // Both are NaN values, considered equal
            } else if (Double.isNaN(arr[a])) {
                return 1; // NaN is considered greater than any other value
            } else if (Double.isNaN(arr[b])) {
                return -1; // NaN is considered greater than any other value
            } else {
                return Double.compare(arr[a], arr[b]); // Regular comparison for non-NaN values
            }
        });

        // Create the sorted float array with NaN values at the end
        double[] sortedArray = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            sortedArray[i] = arr[indices[i]];
        }

        // Create the array of original positions
        int[] originalPositions = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            originalPositions[i] = indices[i];
        }
        for (int i = 0; i < arr.length; i++) {
            arr[i] = sortedArray[i];
        }
        return originalPositions;
    }

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
    public FlatField getRHIVolume(int moment, String varName)
            throws VisADException, RemoteException, IOException {

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
            getRadialVariable(varName);
        int      numberOfSweeps;  //= sweepVar.getNumSweeps();
        int      numberOfRay = getRayNumber(sweepVar);
        int      gates       = getGateNumber(sweepVar);

        Object[] cut         = getCutIdx(sweepVar);
        numberOfSweeps = cut.length;


        //  float[][] aziArray    = new float[numberOfSweeps][];
        // int[][]   aziArrayIdx = new int[numberOfSweeps][];
        float[][] eleArray    = new float[numberOfSweeps][];
        int[][]   eleArrayIdx = new int[numberOfSweeps][];
        float[]   meanEle     = new float[numberOfSweeps];
        float[]   meanAzi     = new float[numberOfSweeps];
        int[]     cutIdx      = new int[numberOfSweeps];
        for (int b = 0; b < numberOfSweeps; b++) {
            int                      sb = Integer.parseInt(cut[b].toString());
            RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(sb);
            //    aziArray[b]    = getAzimuth(s1);
            //    aziArrayIdx[b] = QuickSort.sort(aziArray[b]);
            meanEle[b]     = s1.getMeanElevation();
            eleArray[b]    = s1.getElevation();
            eleArrayIdx[b] = QuickSort.sort(eleArray[b]);
            meanAzi[b]     = (float) Misc.getAverage(s1.getAzimuth());
        }

        cutIdx = QuickSort.sort(meanAzi);

        if (RSL_sweep_list == null) {
            RSL_sweep_list = new CDMRadarSweepDB[numberOfSweeps];
            // now get the hash map for each sweep contain azi as index and ray information.
            for (int b = 0; b < numberOfSweeps; b++) {
                //   RadialDatasetSweep.Sweep s1 = sweepVar.getSweep(b);
                int a = cutIdx[b];
                RSL_sweep_list[b] = constructSweepHashTable(eleArray[a],
                        eleArrayIdx[a], 0.95f);
            }
        }

        double[][] ranges = new double[numberOfSweeps][gates];
        double range_step = sweepVar.getSweep(numberOfSweeps
                                - 1).getGateSize();
        double range_to_first_gate = sweepVar.getSweep(numberOfSweeps
                                         - 1).getRangeToFirstGate();



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

        int[][] rayIndex    = new int[numberOfSweeps][numberOfRay];
        int[]   rNum        = new int[numberOfSweeps];

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            int                      sb    = cutIdx[sweepIdx];
            RadialDatasetSweep.Sweep sweep = sweepVar.getSweep(sb);
            float                    f     = sweep.getBeamWidth() / 2;
            rNum[sweepIdx] = sweep.getRadialNumber();
            // float[] azs = getAzimuth(sweep);
            float[] els = sweep.getElevation();
            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
                float rhiAz = rayIdx;
                rayIndex[sweepIdx][rayIdx] = getClosestRayFromSweep(rhiAz, f,
                        sweepIdx, els);

            }

        }

        //        Trace.call2("volume preamble");

        //        Trace.call1("data read");
        float[] allData;
        try {
            allData = sweepVar.readAllData();
        } catch (Exception np) {
            LogUtil.consoleMessage("Radar read volume data error in file:\n"
                                   + swpFileName);
            return null;
        }
        float[][][] data2 = new float[numberOfSweeps][numberOfRay][gates];

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            float[][] _data2  = data2[sweepIdx];
            int       sb      = cutIdx[sweepIdx];
            int       rnumber = rNum[sweepIdx];

            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
                int     si      = rayIndex[sweepIdx][rayIdx];
                float[] __data2 = _data2[rayIdx];
                if (si < rnumber) {
                    for (int gateIdx = 0; gateIdx < gates; gateIdx++) {
                        __data2[gateIdx] =
                            allData[gates * numberOfRay * sb + gates * si + gateIdx];
                    }
                } else {
                    _data2[rayIdx] = getFloatNaN(gates);
                }
            }

        }

        int k = 0;

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            //  float[]                  _azimuth2    = azimuth2[sweepIdx];
            int       sb          = cutIdx[sweepIdx];
            float[]   _elevation2 = eleArray[sb];
            float     _azimuth2   = meanAzi[sb];
            double[]  _ranges     = ranges[sb];
            float[][] _data2      = data2[sb];

            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
                float   __azimuth   = rayIdx;  //_azimuth2[rayIdx];
                float[] __data2     = _data2[rayIdx];
                float   __elevation = _elevation2[rayIdx];
                for (int gateIdx = 0; gateIdx < gates; gateIdx++) {
                    domainVals2[k]   = __elevation;
                    domainVals1[k]   = _azimuth2;
                    domainVals0[k]   = (float) _ranges[gateIdx];
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
        FunctionType sweepType = new FunctionType(tt,
                                     getMomentType(varName, u));

        retField = new CachedFlatField(sweepType, set,
                                       (CoordinateSystem) null, (Set[]) null,
                                       new Unit[] { u }, signalVals);

        initCachedFlatField((CachedFlatField) retField);
        // retField.setSamples(signalVals, false);

        //        Trace.call2("making gridded3d set");

        putCache(cacheKey, retField);
        return retField;
    }

    /**
     * get float array init with NaN
     *
     * @param n 1 D array length
     *
     * @return float array
     */
    public float[] getFloatNaN(int n) {
        float[] data = new float[n];
        for (int gIdx = 0; gIdx < n; gIdx++) {
            data[gIdx] = Float.NaN;
        }

        return data;
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
     * Get the parameters for this adapter
     *
     * @return parameters
     */
    public boolean isRHI() {
        return this.isRHI;
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

    /**
     * Get the radial variable from the name
     * @param varName either the RealType name or the variable name
     * @return the name or null
     */
    private RadialDatasetSweep.RadialVariable getRadialVariable(
            String varName) {
        return (RadialDatasetSweep.RadialVariable) rds.getDataVariable(
            Util.cleanTypeName(varName));
    }


    /**
     * Clean up whatever we need to when we are removed.
     */
    public void doRemove() {
        clearCachedData();
        try {
            if (rds != null) {
                rds.close();
            }
            if (gcd != null) {
                gcd.close();
            }
        } catch (IOException ioe) {}
    }

    /**
     * Ray is the class used to store all information associated with each ray.
     */
    class Ray {
        public int rayIndex;
        public int sweepIndex;
        public double el;
        public double az;
        public int nGates;
        public float [] data;

        public Ray(int sweepIndex, int rayIndex, int nGates, double el, double az, float [] data){
            this.sweepIndex = sweepIndex;
            this.rayIndex = rayIndex;
            this.nGates = nGates;
            this.el = el;
            this.az = az;
            this.data = data;
        }
    };

    /**
     * SearchPoint is the class used to store all information associated with each ray.
     */
    class SearchPoint {
        public int level;
        public int elDist;
        public int azDist;

        public   Ray ray;
        public double rayEl; // el in search matrix coords
        public double rayAz; // az in search matrix coords
        public double interpEl; // el used for interp
        public double interpAz; // az used for interp

        public SearchPoint(int level, int elDist, int azDist, double rayEl,
                           double rayAz, double interpAz, double interpEl, Ray ray){
            this.level = level;
            this.elDist = elDist;
            this.azDist = azDist;
            this.rayEl = rayEl;
            this.rayAz = rayAz;
            this.interpAz = interpAz;
            this.interpEl = interpEl;
            this.ray = ray;
        }

        public void clear() {
            level = 0;
            elDist = 0;
            azDist = 0;
            ray = null;
            rayEl = 0.0;
            rayAz = 0.0;
            interpEl = 0.0;
            interpAz = 0.0;
        }
    };
    /**
     * Neighbors is the class used to store values of 8 points near any grid point.
     */
    class Neighbors {
        public double ll_inner;
        public double ll_outer;
        public double ul_inner;
        public double ul_outer;
        public double lr_inner;
        public double lr_outer;
        public double ur_inner;
        public double ur_outer;

        public Neighbors(double ll_inner, double ll_outer, double ul_inner, double ul_outer,
                         double lr_inner, double lr_outer, double ur_inner, double ur_outer){
            this.ll_inner = ll_inner;
            this.ll_outer = ll_outer;
            this.ul_inner = ul_inner;
            this.ul_outer = ul_outer;
            this.lr_inner = lr_inner;
            this.lr_outer = lr_outer;
            this.ur_inner = ur_inner;
            this.ur_outer = ur_outer;
        }
    };
    /**
     * GridLoc is the class used to store both radial and grid coordinate information of
     * any grid point.
     */
    class GridLoc {
        public double elev;
        public double az;
        public double dist;
        public double lat;
        public double lon;
        public double alt;
        public GridLoc(double elev, double az, double dist, double lat, double lon, double alt) {
            this.elev = elev;
            this.az = az;
            this.dist = dist;
            this.lat = lat;
            this.lon = lon;
            this.alt = alt;
        }
    };

    /**
     * latLon2RTheta calculate r and theta of two latlon points
     *
     */
    public void latLon2RTheta(double lat1, double lon1,
                          double lat2, double lon2,
                          double[] r,  double[] theta)
    {

        double darc, colat1, colat2, delon, denom, therad;
        double [] cos_colat1= new double[1], sin_colat1 = new double[1];
        double [] cos_colat2= new double[1], sin_colat2= new double[1];
        double xx;
        double[] sin_darc=new double[1], cos_darc=new double[1];

        colat1 = (90.0 - lat1) * DEG_TO_RAD;
        colat2 = (90.0 - lat2) * DEG_TO_RAD;
        delon = (lon2 - lon1) * DEG_TO_RAD;

        if (delon < -M_PI) {
            delon += 2.0 * M_PI;
        }

        if (delon > M_PI) {
            delon -= 2.0 * M_PI;
        }

        ta_sincos(colat1, sin_colat1, cos_colat1);
        ta_sincos(colat2, sin_colat2, cos_colat2);

        xx = cos_colat1[0] * cos_colat2[0] + sin_colat1[0] * sin_colat2[0] * Math.cos(delon);
        if (xx < -1.0) xx = -1.0;
        if (xx > 1.0) xx = 1.0;
        darc = Math.acos(xx);
        ta_sincos(darc, sin_darc, cos_darc);

        r[0] = darc * _earthRadiusKm;

        denom = sin_colat1[0] * sin_darc[0];
        if ((Math.abs(colat1) <= TINY_ANGLE) || (Math.abs(denom) <= TINY_FLOAT)) {
            therad = 0.0;
        } else {
            xx = (cos_colat2[0] - cos_colat1[0] * cos_darc[0]) / denom;
            if (xx < -1.0) xx = -1.0;
            if (xx > 1.0) xx = 1.0;
            therad = Math.acos(xx);
        }

        if ((delon < 0.0) || (delon > M_PI))
            therad *= -1.0;

        theta[0] = therad * RAD_TO_DEG;

    }

    /**
     * ta_sincos calculate sin and cos of a radians
     *
     */
    void ta_sincos(double radians, double [] sinVal, double [] cosVal)

    {

        double cosv, sinv, interval;

        /* compute cosine */
        cosv = Math.cos(radians);
        cosVal[0] = cosv;

        /* compute sine magnitude */
        sinv = Math.sqrt(1.0 - cosv * cosv);

        /* set sine sign from location relative to PI */
        interval = Math.floor(radians / M_PI);
        if (Math.abs(fmod(interval, 2.0)) == 0) {
            sinVal[0] = sinv;
        } else {
            sinVal[0] = -1.0 * sinv;
        }

    }

    /**
     * fmod calculate modulus for float/double
     *
     */
    public static double fmod(double a, double b) {
        int result = (int) Math.floor(a / b);
        return a - result * b;
    }

    /**
     * computeGridRow calculate gridloc for each grid point
     *
     */
    public GridLoc [][][] computeGridRow(int numCols, int numRows, int numLevels, float[] latRows, float[] lonCols, float[] altLevels)

    {
        float _radarLat = (float) radarLocation.getLatitude().getValue();
        float _radarLon = (float) radarLocation.getLongitude().getValue();


        GridLoc [][][] gridLocs = new GridLoc[numLevels][numRows][numCols];

        for (int iz = 0; iz < numLevels; iz++) {
            double zz = altLevels[iz]/1000.0;
            for (int iy = 0; iy < numRows; iy++) {
                double gridLat = latRows[iy];

                for (int ix = 0; ix < numCols; ix++) {
                    // get the latlon of the (x,y) point in the output grid
                    double gridLon = lonCols[ix];
                    // get the azimuth and distance from the radar

                    double[] gndRange = new double[1];
                    double[] azimuth = new double[1];
                    latLon2RTheta(_radarLat, _radarLon,
                            gridLat, gridLon,
                            gndRange, azimuth);
                    if (azimuth[0] < 0) {
                        azimuth[0] += 360.0;
                    }
                    double distt = Math.sqrt(zz*zz + gndRange[0]*gndRange[0]);
                    // compute elevation
                    double elevDeg = computeElevationDeg(zz, gndRange[0]);
                    gridLocs[iz][iy][ix] = new GridLoc(elevDeg, azimuth[0], distt, gridLat, gridLon, zz);

                } // ix
            }
        }



        return gridLocs;

    }
    /**
     * computeElevationDeg calculate elevation
     *
     */
    double computeElevationDeg(double htKm, double gndRangeKm)
    {
        double _gndRangeKm = gndRangeKm;
        double _htKm;
        if (htKm == 0.0) {
            // prevent degenerative case
            _htKm = 0.00001;
        } else {
            _htKm = htKm;
        }
        // cannot solve directly, we use the secant gradient search
        // method

        double xx_n_2 = Math.atan2(_htKm, _gndRangeKm);
        return xx_n_2 * RAD_TO_DEG;
    }

    /**
     * angDist calculate sqrt of deltaEL and AZ
     *
     */
    double angDist(double deltaEl, double deltaAz) {
        double dist = Math.sqrt(deltaAz * deltaAz + deltaEl * deltaEl);
        if (dist == 0) {
            dist = 1.0e-6;
        }
        return dist;
    }

    /**
     * loadWtsFor2ValidRays calculate neighbour values
     *
     */
    Neighbors getWtsFor2ValidRays(GridLoc loc,
                               SearchPoint ll,
                               SearchPoint ul,
                               SearchPoint lr,
                               SearchPoint ur,
                               double wtInner,
                               double wtOuter)

    {
        double ll_inner;
        double ll_outer;
        double ul_inner;
        double ul_outer;
        double lr_inner;
        double lr_outer;
        double ur_inner;
        double ur_outer;


        double az = loc.az;

        // compute 'distance' in el/az space from ray to grid location
        // compute weights based on inverse of
        // distances from grid pt to surrounding rays multiplied
        // by weight for range

        if (ll != null) {
            double dist_ll = angDist(loc.elev - ll.rayEl, az - ll.rayAz);
            double wtDist = 1.0 / dist_ll;
            ll_inner = wtDist * wtInner;
            ll_outer = wtDist * wtOuter;
        } else {
            ll_inner = 0.0;
            ll_outer = 0.0;
        }

        if (ul != null) {
            double dist_ul = angDist(loc.elev - ul.rayEl, az - ul.rayAz);
            double wtDist = 1.0 / dist_ul;
            ul_inner = wtDist * wtInner;
            ul_outer = wtDist * wtOuter;
        } else {
            ul_inner = 0.0;
            ul_outer = 0.0;
        }

        if (lr != null) {
            double dist_lr = angDist(loc.elev - lr.rayEl, az - lr.rayAz);
            double wtDist = 1.0 / dist_lr;
            lr_inner = wtDist * wtInner;
            lr_outer = wtDist * wtOuter;
        } else {
            lr_inner = 0.0;
            lr_outer = 0.0;
        }

        if (ur != null) {
            double dist_ur = angDist(loc.elev - ur.rayEl, az - ur.rayAz);
            double wtDist = 1.0 / dist_ur;
            ur_inner = wtDist * wtInner;
            ur_outer = wtDist * wtOuter;
        } else {
            ur_inner = 0.0;
            ur_outer = 0.0;
        }

        return new Neighbors(ll_inner, ll_outer,ul_inner, ul_outer,
                lr_inner, lr_outer, ur_inner, ur_outer);

    }

    /**
     * getWtsFor3Or4ValidRays calculate neighbour values
     *
     */
    Neighbors getWtsFor3Or4ValidRays(GridLoc loc,
                                 SearchPoint ll,
                                 SearchPoint ul,
                                 SearchPoint lr,
                                 SearchPoint ur,
                                 double wtInner,
                                 double wtOuter)

    {
        double ll_inner;
        double ll_outer;
        double ul_inner;
        double ul_outer;
        double lr_inner;
        double lr_outer;
        double ur_inner;
        double ur_outer;


        double az = loc.az;

        // compute wts for interpolating based on azimuth lower

        double dazLower = lr.interpAz - ll.interpAz;
        double wtAzLr = 0.5;
        if (dazLower != 0.0) {
            wtAzLr = (az - ll.interpAz) / dazLower;
        }
        double wtAzLl = 1.0 - wtAzLr;
        double elLowerInterp = ll.interpEl * wtAzLl + lr.interpEl * wtAzLr;

        // compute wts for interpolating based on azimuth upper

        double dazUpper = ur.interpAz - ul.interpAz;
        double wtAzUr = 0.5;
        if (dazUpper != 0.0) {
            wtAzUr = (az - ul.interpAz) / dazUpper;
        }
        double wtAzUl = 1.0 - wtAzUr;
        double elUpperInterp = ul.interpEl * wtAzUl + ur.interpEl * wtAzUr;

        // compute wts for interpolating based on interpolated elevation

        double dEl = elUpperInterp - elLowerInterp;
        double wtElUpper = 0.5;
        if (dEl != 0) {
            wtElUpper = (loc.elev - elLowerInterp) / dEl;
        }
        double wtElLower = 1.0 - wtElUpper;

        // compute final wts as product of these weights

        if (ll != null) {
            double wtAng = wtAzLl * wtElLower;
            ll_inner = wtAng * wtInner;
            ll_outer = wtAng * wtOuter;
        } else {
            ll_inner = 0.0;
            ll_outer = 0.0;
        }

        if (ul != null) {
            double wtAng = wtAzUl * wtElUpper;
            ul_inner = wtAng * wtInner;
            ul_outer = wtAng * wtOuter;
        } else {
            ul_inner = 0.0;
            ul_outer = 0.0;
        }

        if (lr != null) {
            double wtAng = wtAzLr * wtElLower;
            lr_inner = wtAng * wtInner;
            lr_outer = wtAng * wtOuter;
        } else {
            lr_inner = 0.0;
            lr_outer = 0.0;
        }

        if (ur != null) {
            double wtAng = wtAzUr * wtElUpper;
            ur_inner = wtAng * wtInner;
            ur_outer = wtAng * wtOuter;
        } else {
            ur_inner = 0.0;
            ur_outer = 0.0;
        }

        return new Neighbors(ll_inner, ll_outer,ul_inner, ul_outer,
                lr_inner, lr_outer, ur_inner, ur_outer);
    }

    /**
     * loadNearestGridPt return the nearest radial point
     *
     */
    double loadNearestGridPt(int igateInner,
                                int igateOuter,
                                SearchPoint ll,
                                SearchPoint ul,
                                SearchPoint lr,
                                SearchPoint ur,
                                Neighbors wts)

    {

        // find value with highest weight - that will be closest

        double[] maxWt = {0.0};
        double[] closestVal = {0.0};
        int[] nContrib = {0};

        if (ll!= null) {
            calculateNearest(ll.ray,  igateInner, igateOuter,
                    wts.ll_inner, wts.ll_outer, closestVal, maxWt, nContrib);
        }

        if (ul!= null) {
            calculateNearest(ul.ray,  igateInner, igateOuter,
                    wts.ul_inner, wts.ul_outer, closestVal, maxWt, nContrib);
        }

        if (lr!= null) {
            calculateNearest(lr.ray,igateInner, igateOuter,
                    wts.lr_inner, wts.lr_outer, closestVal, maxWt, nContrib);
        }

        if (ur!= null) {
            calculateNearest(ur.ray,  igateInner, igateOuter,
                    wts.ur_inner, wts.ur_outer, closestVal, maxWt, nContrib);
        }

        // compute weighted mean

        if (nContrib[0] >= 1) {
            return closestVal[0];
        } else {
            return Float.NaN;
        }
       // return nContrib;

    }

    /**
     * calculateNearest return the nearest radial point
     *
     */
    void calculateNearest(Ray ray,
                       int igateInner,
                       int igateOuter,
                       double wtInner,
                       double wtOuter,
                       double [] closestVal,
                       double [] maxWt,
                       int [] nContrib)

    {
        if (ray.data != null) {

            int nGates = ray.nGates;
            float missing = Float.NaN;

            if (igateInner >= 0 && igateInner < nGates) {
                float val = ray.data[igateInner];
                if (val != missing) {
                    if (wtInner > maxWt[0]) {
                        closestVal[0] = val;
                        maxWt[0] = wtInner;
                    }
                    nContrib[0]++;
                }
            }

            if (igateOuter >= 0 && igateOuter < nGates) {
                float val = ray.data[igateOuter];
                if (val != missing) {
                    if (wtOuter > maxWt[0]) {
                        closestVal[0] = val;
                        maxWt[0] = wtOuter;
                    }
                    nContrib[0]++;
                }
            }
        } // if (ray->fldData)
    }

    /**
     * loadInterpGridPt calculate each grid point value with 8
     * nearby radial point and return the interporated value
     *
     */
    double loadInterpGridPt(
                            int igateInner,
                            int igateOuter,
                            SearchPoint ll,
                            SearchPoint ul,
                            SearchPoint lr,
                            SearchPoint ur,
                            Neighbors wts)

    {

        // sum up weighted vals
        double[] sumVals = {0.0};
        double[] sumWts = {0.0};
        int[] nContrib = {0};

        if (ll!= null) {
            calAccumInterp(ll.ray,  igateInner, igateOuter,
                    wts.ll_inner, wts.ll_outer, sumVals, sumWts, nContrib);
        }

        if (ul!= null) {
            calAccumInterp(ul.ray,  igateInner, igateOuter,
                    wts.ul_inner, wts.ul_outer, sumVals, sumWts, nContrib);
        }

        if (lr!= null) {
            calAccumInterp(lr.ray,   igateInner, igateOuter,
                    wts.lr_inner, wts.lr_outer, sumVals, sumWts, nContrib);
        }

        if (ur!= null) {
            calAccumInterp(ur.ray,  igateInner, igateOuter,
                    wts.ur_inner, wts.ur_outer, sumVals, sumWts, nContrib);
        }

        // compute weighted mean

        if (nContrib[0] >= 2) {
            double interpVal = Double.NaN;
            if (sumWts[0] > 0) {
                interpVal = sumVals[0] / sumWts[0];
            }
            return  interpVal;
        } else {
            return Double.NaN;
        }
    }

    /**
     * calAccumInterp return the interporate grid point value with
     * the nearby radial points on the same ray
     *
     */
    void  calAccumInterp(  Ray ray,
                        int igateInner,
                        int igateOuter,
                        double wtInner,
                        double wtOuter,
                        double [] sumVals,
                        double [] sumWts,
                        int [] nContrib)

    {
        if (ray.data != null) {
            int nGates = ray.nGates;
            float missing = Float.NaN;

            if (igateInner >= 0 && igateInner < nGates) {
                float val = ray.data[igateInner];
                if (val != missing) {
                    sumVals[0] += val * wtInner;
                    sumWts[0] += wtInner;
                    nContrib[0]++;
                }
            }

            if (igateOuter >= 0 && igateOuter < nGates) {
                float val = ray.data[igateOuter];
                if (val != missing) {
                    sumVals[0] += val * wtOuter;
                    sumWts[0] += wtOuter;
                    nContrib[0]++;
                }
            }
        } // if (ray->fldData)

    }

    /**
     * getRays return the whole volume data as 2d rays structure
     *
     */
    Ray[][] getRays(RadialDatasetSweep.RadialVariable sweepVar,
                           int numberOfRay, double[] elevs, double[][] azims)
            throws IOException {

        Object[] cut            = getCutIdx(sweepVar);
        int      numberOfSweeps = cut.length;

        Ray[][] rays = new Ray[numberOfSweeps][numberOfRay];

        for (int sweepIdx = 0; sweepIdx < numberOfSweeps; sweepIdx++) {
            //float[][]                _data2 = data2[sweepIdx];
            int sb = Integer.parseInt(cut[sweepIdx].toString());
            RadialDatasetSweep.Sweep s1     = sweepVar.getSweep(sb);
            int rnumber = s1.getRadialNumber();
            elevs[sweepIdx] = s1.getMeanElevation();
            float[] s1data = s1.readData();
            int gnum = s1.getGateNumber();
            for (int rayIdx = 0; rayIdx < numberOfRay; rayIdx++) {
                //int si = rayIndex[sweepIdx][rayIdx];
                float[] data2 = new float[gnum];
                if (rayIdx < rnumber) {
                    double az = s1.getAzimuth(rayIdx);
                    double el = s1.getElevation(rayIdx);
                    azims[sweepIdx][rayIdx] = az;
                    //int     rnumber = rNum[sweepIdx];
                    data2 = s1.readData(rayIdx);

                    rays[sweepIdx][rayIdx] = new Ray(sweepIdx, rayIdx, gnum, el, az, data2);
                } else {
                    //data2 = getFloatNaN(numBin);
                    azims[sweepIdx][rayIdx] = Float.NaN;
                    rays[sweepIdx][rayIdx] = null;
                }

                //rays[sweepIdx][rayIdx] = new Ray(sweepIdx, rayIdx, numBin, el, az, data2);
            }

        }

        return rays;

    }

    /**
     * interpRow return each row data
     * For each grid point use radial coordinate information in the gridloc to find the
     * neighbour rays, calculate the neighbour values, and finally calculate the value
     * of the grid point.
     *
     */
    double [] interpGridRow(int iz, int iy, GridLoc [][][]loc, Ray[][] rays, double[] elevs, double[][] azims, int[][] azidx)
    {
        int Nx = 100;
        double [] rowVals = new double[Nx];

        for (int ix = 0; ix < Nx; ix++) {
            GridLoc lloc = loc[iz][iy][ix];

            double elevation = lloc.elev;
            double azimuth = lloc.az;
            double alt = lloc.alt;

            int [][] nears = getNeighborRays(azimuth, elevation, elevs, azims, azidx);
            // SearchPoint(int level, int elDist, int azDist, double rayEl,
            //           double rayAz, double interpAz, double interpEl, Ray ray)
            SearchPoint ll ;
            SearchPoint ul ;
            SearchPoint lr ;
            SearchPoint ur ;
            int nAvail = 0;

            if(nears[0][0] == -999)
                ll = null;
            else {
                nAvail++;
                Ray ray = rays[nears[0][0]][nears[0][1]];
                ll = new SearchPoint(0, 0, 0, elevation, azimuth,ray.az, ray.el, ray);
            }

            if(nears[1][0] == -999)
                lr = null;
            else {
                nAvail++;
                Ray ray = rays[nears[1][0]][nears[1][1]];
                lr = new SearchPoint(0, 0, 0, elevation, azimuth,ray.az, ray.el, ray);
            }

            if(nears[2][0] == -999)
                ul = null;
            else {
                nAvail++;
                Ray ray = rays[nears[2][0]][nears[2][1]];
                ul = new SearchPoint(0, 0, 0, elevation, azimuth,ray.az, ray.el, ray);
            }

            if(nears[3][0] == -999)
                ur = null;
            else {
                nAvail++;
                Ray ray = rays[nears[3][0]][nears[3][1]];
                ur = new SearchPoint(0, 0, 0, elevation, azimuth,ray.az, ray.el, ray);
            }
            // get gate indices, compute weights based on range
            double rangeKm = lloc.dist;
            double dgate = (rangeKm - range_to_first_gate) / range_step;
            int igateInner = (int) Math.floor(dgate);
            int igateOuter = igateInner + 1;
            double wtOuter = dgate - igateInner;
            double wtInner = 1.0 - wtOuter;
            Neighbors wts = null;

            if(nAvail == 2){
                wts = getWtsFor2ValidRays(lloc, ll, ul, lr, ur, wtInner, wtOuter);
            } else {
                wts = getWtsFor3Or4ValidRays(lloc, ll, ul, lr, ur, wtInner, wtOuter);
            }

// normalize weights

            double sumWt = 0.0;
            sumWt += wts.ll_inner;
            sumWt += wts.ll_outer;
            sumWt += wts.ul_inner;
            sumWt += wts.ul_outer;
            sumWt += wts.lr_inner;
            sumWt += wts.lr_outer;
            sumWt += wts.ur_inner;
            sumWt += wts.ur_outer;
            if (sumWt == 0) {
                sumWt = 1.0;
            }

            wts.ll_inner /= sumWt;
            wts.ll_outer /= sumWt;
            wts.ul_inner /= sumWt;
            wts.ul_outer /= sumWt;
            wts.lr_inner /= sumWt;
            wts.lr_outer /= sumWt;
            wts.ur_inner /= sumWt;
            wts.ur_outer /= sumWt;


            // interpolate fields
            if(false){
                rowVals[ix] = loadNearestGridPt(igateInner, igateOuter, ll, ul, lr, ur, wts);
            } else {
                rowVals[ix] = loadInterpGridPt( igateInner, igateOuter, ll, ul, lr, ur, wts);
            }
        }
        return rowVals;
    }
    /**
     * this is API for finding the neighboring's ray of given az and elev
     * the returns of the ray index of ll = 0, lr = 1, ul = 2, ur = 3
     * @param azims is sorted azimuths with original idex in azidx
     *
     *
     */
    int [][] getNeighborRays(double az, double elev, double[] elevs, double[][] azims, int[][] azidx){
        int [][] neighbors = new int[4][2];

        int MM = elevs.length;
        int j = findIndexToBeInserted(elevs, elev, 0, MM-1);
        double deltaAz = azims[j][1] - azims[j][0];
        //int i = (int)Math.floor((az - azims[j][0])/deltaAz);
        //int i  = getAzIndex(az, azims[j], deltaAz);
        int i  =  findInsertIndex(azims[j], az);

        if (elev < elevs[0]) {
            int jel = 0;
            int jaz = i;
            int NN = azims[jel].length;
            neighbors[0][0] = -999; //ll
            neighbors[0][1] = -999; //ll
            neighbors[1][0] = -999;  //lr
            neighbors[1][1] = -999;  //lr
            if(az <= azims[jel][0]){
                neighbors[2][0] = 0; //ul elev idx
                neighbors[2][1] = azidx[j][NN-1]; //ul zimuths idx
                neighbors[3][0] = 0; //ur
                neighbors[3][1] = azidx[j][0]; //ur
                return neighbors;
            } else if(az >= azims[jel][NN-1]){
                neighbors[2][0] = 0; //ul
                neighbors[2][1] = azidx[j][NN-1]; //ul
                neighbors[3][0] = 0; //ur
                neighbors[3][1] = azidx[j][0]; //ur
                return neighbors;
            }
            for (int ii = 0; ii < 2; ii++) {
                if((azims[jel][jaz-1] <= az) && (azims[jel][jaz] >= az)){
                    neighbors[2][0] = 0; //ul elev idx
                    neighbors[2][1] = azidx[j][jaz-1]; //ul zimuths idx
                    neighbors[3][0] = 0; //ur
                    neighbors[3][1] = azidx[j][jaz]; //ur
                    break;
                } else if ((azims[jel][jaz-1] > az) && (jaz > 0)) {
                    jaz--;
                } else if ((azims[jel][jaz] < az) && (jaz < NN)) {
                    jaz++;
                } else {
                    System.out.println("Stop for check");
                }
            } // ii
            return neighbors;
        }

        if (elev > elevs[MM-1]) {
            int jel = MM-1;
            int jaz = i;
            int NN = azims[jel].length;
            neighbors[2][0] = -999; //ul
            neighbors[2][1] = -999; //ul
            neighbors[3][0] = -999;  //ur
            neighbors[3][1] = -999;  //ur
            if(az <= azims[jel][0]){
                neighbors[0][0] = jel; //ul elev idx
                neighbors[0][1] = azidx[j][NN-1]; //ul zimuths idx
                neighbors[1][0] = jel; //ur
                neighbors[1][1] = azidx[j][0]; //ur
                return neighbors;
            } else if(az >= azims[jel][NN-1]){
                neighbors[0][0] = 0; //ul
                neighbors[0][1] = azidx[j][NN-1]; //ul
                neighbors[1][0] = 0; //ur
                neighbors[1][1] = azidx[j][0]; //ur
                return neighbors;
            }
            for (int ii = 0; ii < 2; ii++) {
                if((azims[jel][jaz-1] <= az) && (azims[jel][jaz] >= az)) {
                    neighbors[0][0] = MM-1;//ll
                    neighbors[0][1] = azidx[j][jaz-1]; //ll
                    neighbors[1][0] = MM-1;//lr
                    neighbors[1][1] = azidx[j][jaz]; //lr
                    break;
                } else if ((azims[jel][jaz-1] > az) && (jaz > 0)) {
                    jaz--;
                } else if ((azims[jel][jaz] < az) && (jaz < NN)) {
                    jaz++;
                } else   {
                    System.out.println("Stop for check");
                }
            } // ii
            return neighbors;
        }

        int jel = j;
        int jaz = i;
        int NN = azims[jel].length;

        if(az <= azims[jel][0] || az >= azims[jel][NN-1]){
            neighbors[0][0] = jel; //ll elev idx
            neighbors[0][1] = azidx[j][NN-1]; //ll zimuths idx
            neighbors[1][0] = jel; //lr
            neighbors[1][1] = azidx[j][0]; //lr
        } else {
            for (int ii = 0; ii < 2; ii++) {
                if((azims[jel][jaz-1] <= az) && (azims[jel][jaz] >= az)) {
                    neighbors[0][0] = jel; //ll elev idx
                    neighbors[0][1] = azidx[jel][jaz-1]; //ll zimuths idx
                    neighbors[1][0] = jel; //lr
                    neighbors[1][1] = azidx[jel][jaz]; //lr
                    break;
                } else if ((azims[jel][jaz-1] > az) && (jaz > 0)) {
                    jaz--;
                } else if ((azims[jel][jaz] < az) && (jaz < NN)) {
                    jaz++;
                } else {
                    System.out.println("Stop for check");
                }
            } // ii
        }

        jel = j + 1;
        i  =  findInsertIndex(azims[jel], az);
        jaz = i;
        NN = azims[jel].length;
        if(az <= azims[jel][0] || az >= azims[jel][NN-1]){
            neighbors[2][0] = jel; //ul elev idx
            neighbors[2][1] = azidx[jel][NN-1]; //ul zimuths idx
            neighbors[3][0] = jel; //ur
            neighbors[3][1] = azidx[jel][0]; //ur
        } else {
            for (int ii = 0; ii < 2; ii++) {
                if((azims[jel][jaz-1] <= az) && (azims[jel][jaz] >= az)) {
                    neighbors[2][0] = jel; //ul
                    neighbors[2][1] = azidx[jel][jaz-1]; //ul
                    neighbors[3][0] = jel;  //ur
                    neighbors[3][1] = azidx[jel][jaz];  //ur
                    break;
                } else if ((azims[jel][jaz-1] > az) && (jaz > 0)) {
                    jaz--;
                } else if ((azims[jel][jaz] < az) && (jaz < NN)) {
                    jaz++;
                } else  {
                    System.out.println("Stop for check");
                }
            } // ii
        }
        return neighbors;
    }

    /**
     * findInsertIndex return the inserting index
     * this is used for the azimuth arrays
     */
    public static int findInsertIndex(double[] sortedArray, double value) {
        int insertIndex = Arrays.binarySearch(sortedArray, value);

        // If the value is found, binarySearch returns the index of the value.
        // If not found, it returns a negative value indicating the insert point.
        // We need to convert this negative value to the actual insert index.
        if (insertIndex < 0) {
            insertIndex = -(insertIndex + 1);
        }

        return insertIndex;
    }

    /**
     * findIndexToBeInserted return the inserting index
     * this is used for the elevation arrays
     *
     */
    static int findIndexToBeInserted(double[] arr, double k, int start, int end) {
        if (k == 0 || k <= arr[0])
            return 0;
        if(k >= arr[end])
            return end;

        int mid = (start + end) / 2;

        if (k > arr[mid] && k < arr[mid + 1])
            return mid;

        if (arr[mid] < k)
            return findIndexToBeInserted(arr, k, mid + 1, end);

        return findIndexToBeInserted(arr, k, start, mid - 1);
    }
}
