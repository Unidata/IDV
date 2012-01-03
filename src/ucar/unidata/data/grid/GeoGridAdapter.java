/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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


import ucar.ma2.Array;

import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;

import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataUtil;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.vertical.VerticalTransform;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;

import ucar.visad.ProjectionCoordinateSystem;
import ucar.visad.RadarGridCoordinateSystem;
import ucar.visad.data.GeoGridFlatField;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.GeopotentialAltitude;
import ucar.visad.quantities.Gravity;

import visad.CachingCoordinateSystem;
import visad.CartesianProductCoordinateSystem;
import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.DateTime;
import visad.EmpiricalCoordinateSystem;
import visad.ErrorEstimate;
import visad.FieldImpl;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.Gridded3DSet;
import visad.GriddedSet;
import visad.IdentityCoordinateSystem;
import visad.Integer1DSet;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Linear3DSet;
import visad.LinearLatLonSet;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.SetException;
import visad.SetType;
import visad.SingletonSet;
import visad.Unit;
import visad.VisADException;

import visad.data.CachedFlatField;
import visad.data.in.ArithProg;
import visad.data.in.LonArithProg;

import visad.util.ThreadManager;

import java.io.IOException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;



/**
 * Adapt a ucar.unidata.GeoGrid into VisAD Data object(s).
 *
 * GeoGridAdapter gets a Geogrid and has methods to create a
 * a VisAD 2D or 3D FlatField, or sequence of same, from the GeoGrid
 * for parameter names and time(s) desired.
 *
 * @author Don Murray
 * @version $Revision: 1.112 $ $Date: 2007/08/20 22:33:38 $
 */
public class GeoGridAdapter {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            GeoGridAdapter.class.getName());

    /** cache file */
    public String cacheFile;

    /** the associated data source (for caching) */
    private GeoGridDataSource dataSource;

    /** extra key for caching */
    private Object extraCacheKey;

    /** Do we read the data right away or do we use a GeoGridFlatField */
    private boolean lazyEvaluation = true;

    /** the geogrid to adapt */
    private GeoGrid geoGrid = null;

    /** fixed height above ground level */
    private double fhgLevel = -999.9;

    /** the count for this instance */
    private static int count = 0;

    /** the associated netCDF file */
    private NetcdfFile ncFile;

    /** the parameter name */
    private String paramName;

    /** the parameter type */
    RealType paramType;

    /** Used to synchronize read access on the data set */
    private Object readLock;

    /** Default vertical transform */
    private String vertcs = DataUtil.STD_ATMOSPHERE;

    /**
     * Construct a GeoGridAdapter that will take a GeoGrid and return
     * VisAD Data objects from it.
     *
     * @param dataSource  source of this GeoGrid
     * @param geoGrid     the GeoGrid in question
     *
     * @throws VisADException  unable to create one of the VisAD objects
     */
    public GeoGridAdapter(GeoGridDataSource dataSource, GeoGrid geoGrid)
            throws VisADException {
        this(dataSource, geoGrid, geoGrid.getName());
    }

    /**
     * Construct a GeoGridAdapter that will take a GeoGrid and return
     * VisAD Data objects from it.
     *
     * @param dataSource  source of this GeoGrid
     * @param geoGrid     the GeoGrid in question
     * @param paramName   name of the parameter
     *
     * @throws VisADException  unable to create one of the VisAD objects
     */
    public GeoGridAdapter(GeoGridDataSource dataSource, GeoGrid geoGrid,
                          String paramName)
            throws VisADException {
        this(dataSource, geoGrid, paramName, (NetcdfFile) null);
    }



    /**
     * Construct a GeoGridAdapter that will take a GeoGrid and return
     * VisAD Data objects from it.
     *
     * @param dataSource  source of this GeoGrid
     * @param geoGrid     the GeoGrid in question
     * @param ncFile      file that the GeoGrid data is coming from
     *
     * @throws VisADException  unable to create one of the VisAD objects
     */
    public GeoGridAdapter(GeoGridDataSource dataSource, GeoGrid geoGrid,
                          NetcdfFile ncFile)
            throws VisADException {
        this(dataSource, geoGrid, geoGrid.getName(), ncFile);
    }


    /**
     * Construct a GeoGridAdapter that will take a GeoGrid and return
     * VisAD Data objects from it.
     *
     * @param dataSource  source of this GeoGrid
     * @param geoGrid     the GeoGrid in question
     * @param paramName   name of the parameter
     * @param ncFile      file that the GeoGrid data is coming from
     *
     * @throws VisADException  unable to create one of the VisAD objects
     */
    public GeoGridAdapter(GeoGridDataSource dataSource, GeoGrid geoGrid,
                          String paramName, NetcdfFile ncFile)
            throws VisADException {

        this(dataSource, geoGrid, paramName, ncFile, null);
    }

    /**
     * Construct a GeoGridAdapter that will take a GeoGrid and return
     * VisAD Data objects from it.
     *
     * @param dataSource  source of this GeoGrid
     * @param geoGrid     the GeoGrid in question
     * @param paramName   name of the parameter
     * @param ncFile      file that the GeoGrid data is coming from
     * @param extraCacheKey Extra key to use when caching
     *
     * @throws VisADException  unable to create one of the VisAD objects
     */
    public GeoGridAdapter(GeoGridDataSource dataSource, GeoGrid geoGrid,
                          String paramName, NetcdfFile ncFile,
                          Object extraCacheKey)
            throws VisADException {
        this.dataSource = dataSource;
        if (dataSource != null) {
            DataContext dataContext = dataSource.getDataContext();
            if (dataContext != null) {
                String vcs = (String) dataContext.getPreference(
                                 GeoGridDataSource.PREF_VERTICALCS);
                if (vcs != null) {
                    vertcs = vcs;
                }
            }
        }
        this.readLock = dataSource.readLock;
        if (geoGrid == null) {
            throw new IllegalArgumentException(
                "GeoGridAdapter: geogrid cannot be null");
        }
        this.ncFile    = ncFile;
        this.paramName = paramName;
        this.geoGrid   = geoGrid;

        paramType = makeRealType(paramName,
                                 getUnit(geoGrid.getUnitsString()));
        this.extraCacheKey = extraCacheKey;
    }


    /**
     * Create the list of levels for this grid
     *
     * @return List of levels as Real-s
     *
     * @throws VisADException On badness
     */
    protected List getLevels() throws VisADException {
        List              levels = new ArrayList();
        GridCoordSystem   gcs    = geoGrid.getCoordinateSystem();
        VerticalTransform vt     = gcs.getVerticalTransform();
        CoordinateAxis1D  zAxis  = gcs.getVerticalAxis();
        if (zAxis == null) {
            return levels;
        }
        int sizeZ = (int) zAxis.getSize();
        if (sizeZ == 0) {
            return levels;
        }
        boolean  isLinear = checkLinearity(zAxis, false);
        Unit     zUnit    = getUnit(zAxis.getUnitsString());
        boolean  isLatLon = gcs.isLatLon();
        RealType zType    = null;
        boolean  is2D     = is2D(sizeZ, zUnit, vt);
        if (is2D) {
            return levels;
        }
        //if (isLatLon) {
        if (Unit.canConvert(zUnit, CommonUnit.meter) && (vt == null)) {
            if ( !gcs.isZPositive()) {
                // negate units if depth
                zUnit = zUnit.scale(-1);
            }
            zType = RealType.Altitude;
        } else {  //not plain old lat/lon/alt
            zType = makeRealType(zAxis.getName(), zUnit);
        }
        /*
    } else {
        zType = makeRealType(zAxis.getName(), zUnit);
        if (vt != null) {}
        else if (Unit.canConvert(zUnit, CommonUnit.meter)) {
            zType = makeRealType("alti", zUnit);
        } else if (Unit.canConvert(zUnit, CommonUnits.MILLIBAR)) {
            //??
        } else {
            zType = RealType.Altitude;
        }
    }
    */

        // System.err.println("realtype:" + zType + " " + zUnit);
        for (int k = 0; k < sizeZ; k++) {
            float kValue = (float) zAxis.getCoordValue(k);
            levels.add(new Real(zType, kValue, zUnit));
        }
        return levels;

    }





    /**
     * Wrapper around a synchronized call to getSpatialDomainSet
     *
     * @param geogrid  object that contains the metadata used to create
     *                 the set.
     * @param timeIndex  time index for time dependent grids
     *
     * @return GriddedSet that represents the geolocation of the data.
     *         Set is cached so all GeoGrids with the same transforms
     *         use the identical, immutable set.
     *
     * @throws VisADException  a problem creating the domain set
     */
    private GriddedSet getSpatialDomainSet(GeoGrid geogrid, int timeIndex)
            throws VisADException {
        synchronized (dataSource.DOMAIN_SET_MUTEX) {
            return getSpatialDomainSetInner(geogrid, timeIndex);
        }
    }


    /**
     * ugly, long method to create VisAD Set from a geogrid. The
     * Set can take on a variety of forms (2D or 3D, linear or gridded)
     * and has a geolocation feature either through a lat/lon
     * SetType or through a CoordinateSystem transform.
     *
     * @param geogrid  object that contains the metadata used to create
     *                 the set.
     * @param timeIndex  time index for time dependent grids
     *
     * @return GriddedSet that represents the geolocation of the data.
     *         Set is cached so all GeoGrids with the same transforms
     *         use the identical, immutable set.
     *
     * @throws VisADException  a problem creating the domain set
     */
    private GriddedSet getSpatialDomainSetInner(GeoGrid geogrid,
            int timeIndex)
            throws VisADException {

        GridCoordSystem   gcs   = geogrid.getCoordinateSystem();
        VerticalTransform vt    = gcs.getVerticalTransform();
        boolean isTimeDependent = ((vt != null) && vt.isTimeDependent());
        Trace.msg("GeoGridAdapter isTimeDependent = " + isTimeDependent);

        // check to see if we have this cached for this time step
        Object timeStepKey = !isTimeDependent
                             ? (Object) gcs
                             : (Object) new ObjectPair(gcs,
                                 new Integer(timeIndex));
        timeStepKey = Misc.newList(timeStepKey, extraCacheKey);
        GriddedSet domainSet = (GriddedSet) dataSource.getCache(timeStepKey,
                                   true);
        if (domainSet != null) {
            return domainSet;
        }


        // check to see if we have the initial spatial domain cached
        Object    domainSetKey = Misc.newList(gcs, extraCacheKey);


        float[][] refVals      = null;
        Object[] cachedPair = (Object[]) dataSource.getCache(domainSetKey,
                                  true);
        if (cachedPair != null) {
            domainSet = (GriddedSet) cachedPair[0];
            refVals   = (float[][]) cachedPair[1];
        } else {
            Trace.call1("GeoGridAdapter.getInitialSpatialDomain");
            domainSet = getInitialSpatialDomain(geogrid);
            Trace.call2("GeoGridAdapter.getInitialSpatialDomain");

            CoordinateSystem cs = domainSet.getCoordinateSystem();
            if ((vt != null) && (cs != null)) {
                Trace.call1("GeoGridAdapter.transformCoordinates");
                RealTupleType setType =
                    ((SetType) domainSet.getType()).getDomain();
                RealTupleType   refType   = cs.getReference();
                ErrorEstimate[] oldErrors = domainSet.getSetErrors();
                ErrorEstimate[] newErrors =
                    new ErrorEstimate[oldErrors.length];
                float[][] setVals = domainSet.getSamples();
                // transform to the reference
                refVals = CoordinateSystem.transformCoordinates(refType,
                        refType.getCoordinateSystem(),
                        refType.getDefaultUnits(), newErrors, setType, cs,
                        domainSet.getSetUnits(), oldErrors, setVals, false);

                Trace.call2("GeoGridAdapter.transformCoordinates");

            }
            dataSource.putCache(domainSetKey, new Object[] { domainSet,
                    refVals }, true);
        }

        CoordinateSystem cs = domainSet.getCoordinateSystem();
        if ((vt != null) && (cs != null)) {
            Trace.call1(
                "GeoGridAdapter.spatial makeDomainWithVerticalTransform");
            domainSet = makeDomainWithVerticalTransform(vt, domainSet,
                    refVals, timeIndex);
            Trace.call2(
                "GeoGridAdapter.spatial makeDomainWithVerticalTransform");
        }


        dataSource.putCache(timeStepKey, domainSet, true);

        log_.debug("DomainSet = " + domainSet);
        return domainSet;
    }



    /**
     * Create the initial spatial domain
     *
     * @param geogrid The geogrid
     *
     * @return The initial spatial domain
     *
     * @throws VisADException   problem creating domain
     */
    private GriddedSet getInitialSpatialDomain(GeoGrid geogrid)
            throws VisADException {

        GriddedSet        domainSet = null;
        GridCoordSystem   gcs       = geogrid.getCoordinateSystem();
        VerticalTransform vt        = gcs.getVerticalTransform();

        // LOOK! this assumes a product set
        CoordinateAxis   xAxis = gcs.getXHorizAxis();
        CoordinateAxis   yAxis = gcs.getYHorizAxis();
        CoordinateAxis1D zAxis = gcs.getVerticalAxis();

        // Check to see if they are linear or not
        boolean xIsLinear = false;
        boolean yIsLinear = checkLinearity(yAxis, false);
        boolean isLinear  = yIsLinear;

        int     sizeZ     = (zAxis == null)
                            ? 0
                            : (int) zAxis.getSize();

        // check ZAxis (check if x is linear and sizeZ > 1)
        // if sizeZ == 1, we need to have a Gridded3DSet.  Linear3DSet does
        // not display correctly because it displays as points not a texture.
        isLinear = isLinear && (sizeZ != 1);
        if (isLinear && (sizeZ > 1)) {
            isLinear = checkLinearity(zAxis, false);
        }
        //  The possibilities that are handled are:
        //  xAxis = x[km], yAxis = y[km], zAxis = altitude[unit of length]
        //  xAxis = x[km], yAxis = y[km], zAxis = level[pressure]
        //  xAxis = lon[deg], yAxis = lat[deg], zAxis = altitude[unit of length]
        //  xAxis = lon[deg], yAxis = lat[deg], zAxis = level[pressure]
        //  NB:  if ZAxis is null or there is only one level, a 2D domain is made
        Unit xUnit = getUnit(xAxis.getUnitsString());
        Unit yUnit = getUnit(yAxis.getUnitsString());
        Unit zUnit = null;
        if (zAxis != null) {
            zUnit = getUnit(zAxis.getUnitsString());
        }

        log_.debug("    x axis has units " + xAxis.getUnitsString() + " or "
                   + xUnit);
        log_.debug("    y axis has units " + yAxis.getUnitsString() + " or "
                   + yUnit);
        if (zAxis != null) {
            log_.debug("    z axis has units " + zAxis.getUnitsString()
                       + " or " + zUnit);
        }
        log_.debug("  grid size: x = " + xAxis.getSize() + " y = "
                   + yAxis.getSize() + " z = " + sizeZ);

        //boolean isLatLon = (Unit.canConvert(xUnit, CommonUnits.DEGREE)
        //                    && Unit.canConvert(yUnit, CommonUnits.DEGREE));
        boolean          isLatLon      = gcs.isLatLon();


        CoordinateSystem domainCS      = null;
        RealType         xType         = null;
        RealType         yType         = null;
        RealType         zType         = null;

        boolean          needToWrapLon = false;
        //if (yIsLinear) {
        if (isLatLon) {
            xIsLinear = checkLinearity(xAxis, true);
            if (xIsLinear) {
                needToWrapLon = checkNeedToWrapLon(xAxis);
            }
        } else {
            xIsLinear = checkLinearity(xAxis, false);
        }
        //}
        isLinear = isLinear && xIsLinear;

        RealTupleType domainTemplate = null;
        boolean       is2D           = is2D(sizeZ, zUnit, vt);  // 2D Domain


        if (isLatLon) {
            log_.debug("is lat/lon");
            if (is2D) {  // 2D Domain
                xType          = RealType.Longitude;
                yType          = RealType.Latitude;
                domainTemplate = RealTupleType.SpatialEarth2DTuple;
                // hack for bad units
                if (xAxis.getUnitsString().equals("")) {
                    xUnit = CommonUnit.degree;
                }
                if (yAxis.getUnitsString().equals("")) {
                    yUnit = CommonUnit.degree;
                }
            } else {     // 3D domain

                if (Unit.canConvert(zUnit, CommonUnit.meter)) {
                    // check to see if positive is up
                    if ( !gcs.isZPositive()) {
                        // negate units if depth
                        zUnit = zUnit.scale(-1);
                    }
                    xType          = RealType.Longitude;
                    yType          = RealType.Latitude;
                    zType          = RealType.Altitude;
                    domainTemplate = RealTupleType.SpatialEarth3DTuple;
                } else {  //not plain old lat/lon/alt
                    xType = makeRealType((xAxis.getName().equals("Longitude"))
                                         ? "longi"
                                         : xAxis.getName(), xUnit);
                    yType = makeRealType((yAxis.getName().equals("Latitude"))
                                         ? "lati"
                                         : yAxis.getName(), yUnit);
                    zType = makeRealType(zAxis.getName(), zUnit);
                    CoordinateSystem compCS = null;

                    if (vt != null) {  // vertical transform handled later
                        compCS = new CartesianProductCoordinateSystem(
                            new IdentityCoordinateSystem(
                                RealTupleType
                                    .SpatialEarth2DTuple), new IdentityCoordinateSystem(
                                        new RealTupleType(zType)));

                        // if have z as "level" with pressure mb units
                    } else if (Unit.canConvert(zUnit, CommonUnits.MILLIBAR)) {
                        compCS = new CartesianProductCoordinateSystem(
                            new IdentityCoordinateSystem(
                                RealTupleType
                                    .SpatialEarth2DTuple), new CachingCoordinateSystem(
                                        DataUtil
                                            .getPressureToHeightCS(vertcs)));
                    } else if
                    // elevation angle
                    (Unit.canConvert(zUnit, CommonUnit.degree) &&
                    // make sure it's not dimensionless
                    !zUnit.getIdentifier().equals("")) {
                        zType  = makeRealType("elev_angle", zUnit);
                        compCS = makeElevationCS();

                    } else {  // unimplemented coversion
                        //throw new VisADException(
                        //    "Unable to handle Z axis with Unit " + zUnit);
                        LogUtil.userMessage(
                            log_,
                            "Unknown vertical coordinate with Unit " + zUnit
                            + ", using linear scale to Altitude", true);
                        compCS = new CartesianProductCoordinateSystem(
                            new IdentityCoordinateSystem(
                                RealTupleType
                                    .SpatialEarth2DTuple), new IdentityCoordinateSystem(
                                        new RealTupleType(
                                            RealType
                                                .Altitude), new Unit[] { zUnit }));

                    }
                    domainTemplate = new RealTupleType(xType, yType, zType,
                            compCS, null);
                }
            }

        } else {  // not lat/lon
            log_.debug("not lat/lon");


            /* get the Projection for this GeoGrid's GridCoordSys  */
            ProjectionImpl project = gcs.getProjection();
            CoordinateSystem pCS = new CachingCoordinateSystem(
                                       new ProjectionCoordinateSystem(
                                           project, new Unit[] { xUnit,
                    yUnit }));
            // make the proper RealTypes
            xType = makeRealType(xAxis.getName(), xUnit);
            yType = makeRealType(yAxis.getName(), yUnit);
            //if (sizeZ < 1) {           // 2D Domain
            if (is2D) {  // 2D Domain
                domainTemplate = new RealTupleType(xType, yType, pCS, null);
            } else {
                CoordinateSystem compCS = null;
                zType = makeRealType(zAxis.getName(), zUnit);
                log_.debug("non-lat/lon 3D grid zType=" + zType + ","
                           + zUnit);
                if (vt != null) {  // vertical transform handled later
                    compCS = new CartesianProductCoordinateSystem(
                        pCS,
                        new IdentityCoordinateSystem(
                            new RealTupleType(zType)));

                    // only if z units convertible with meters
                } else if (Unit.canConvert(zUnit, CommonUnit.meter)) {
                    zType = makeRealType("alti", zUnit);
                    compCS = new CartesianProductCoordinateSystem(
                        pCS,
                        new IdentityCoordinateSystem(
                            new RealTupleType(RealType.Altitude)));
                    // if z units is pressure units
                } else if (Unit.canConvert(zUnit, CommonUnits.MILLIBAR)) {
                    compCS = new CartesianProductCoordinateSystem(
                        pCS,
                        new CachingCoordinateSystem(
                            DataUtil.getPressureToHeightCS(vertcs)));
                } else {
                    //throw new VisADException(
                    //    "Unable to handle Z axis with Unit " + zUnit);
                    LogUtil.userMessage(
                        log_,
                        "Unknown vertical coordinate with Unit " + zUnit
                        + ", using linear scale to Altitude", true);
                    compCS = new CartesianProductCoordinateSystem(
                        pCS,
                        new CachingCoordinateSystem(
                            new IdentityCoordinateSystem(
                                new RealTupleType(RealType.Altitude),
                                new Unit[] { zUnit })));
                }
                domainTemplate = new RealTupleType(xType, yType, zType,
                        compCS, null);
            }
        }  // end non-lat/lon domain


        Object domainTemplateKey = Misc.newList(gcs, "DomainType",
                                       extraCacheKey);
        RealTupleType cachedDomainTemplate =
            (RealTupleType) dataSource.getCache(domainTemplateKey, true);

        if (cachedDomainTemplate != null) {
            Trace.msg("GeoGridAdapter:using cached domain template:"
                      + cachedDomainTemplate);
            domainTemplate = cachedDomainTemplate;
        } else {
            Trace.msg("GeoGridAdapter:using new domain template:"
                      + domainTemplate);
            dataSource.putCache(domainTemplateKey, domainTemplate, true);
        }


        if (isLinear) {
            Linear1DSet xSet = makeLinear1DSet((CoordinateAxis1D) xAxis,
                                   xType, xUnit, needToWrapLon);
            Linear1DSet ySet = makeLinear1DSet((CoordinateAxis1D) yAxis,
                                   yType, yUnit);

            //if (sizeZ < 1) {                     // 2D Domain
            if (is2D) {  // 2D Domain
                if (isLatLon) {
                    try {
                        domainSet = new LinearLatLonSet(domainTemplate,
                                new Linear1DSet[] { xSet,
                                ySet }, (CoordinateSystem) null,
                                        new Unit[] { xUnit,
                                yUnit }, (ErrorEstimate[]) null, true);
                    } catch (SetException se) {  // bad values for lon
                        domainSet = new Linear2DSet(domainTemplate,
                                new Linear1DSet[] { xSet,
                                ySet }, (CoordinateSystem) null,
                                        new Unit[] { xUnit,
                                yUnit }, (ErrorEstimate[]) null, true);
                    }
                } else {
                    domainSet = new Linear2DSet(domainTemplate,
                            new Linear1DSet[] { xSet,
                            ySet }, (CoordinateSystem) null,
                                    new Unit[] { xUnit,
                            yUnit }, (ErrorEstimate[]) null, true);
                }
            } else {
                Linear1DSet zSet = makeLinear1DSet(zAxis, zType, zUnit);
                domainSet = new Linear3DSet(domainTemplate,
                                            new Linear1DSet[] { xSet,
                        ySet, zSet }, (CoordinateSystem) null,
                                      new Unit[] { xUnit,
                        yUnit, zUnit }, (ErrorEstimate[]) null, true);
            }
        } else {  // not linear or only one level
            log_.debug("not linear set");
            // make the gridded 3D set of coordinates
            float[][] coordData;
            int       sizeX = (int) xAxis.getSize();
            log_.debug("x has " + sizeX + " elements");
            int sizeY = (int) yAxis.getSize();
            log_.debug("y has " + sizeY + " elements");
            boolean is1D = xAxis.getRank() == 1;
            log_.debug("x has rank " + xAxis.getRank());
            int[]  lengths;
            Unit[] units;
            // if (sizeZ < 1) {                     // 2D Domain
            if (is2D) {  // 2D Domain
                if (needToWrapLon) {
                    sizeX = sizeX + 1;
                }
                log_.debug("sizeZ <= 1 (i.e., 2D)");
                coordData = new float[2][(is1D)
                                         ? sizeX * sizeY
                                         : sizeX];
                int idx = 0;
                if (is1D) {
                    for (int j = 0; j < sizeY; j++) {
                        for (int i = 0; i < sizeX; i++) {

                            // set or load coordinate values
                            if (needToWrapLon && (i == sizeX - 1)) {
                                coordData[0][idx] =
                                    (float) (((CoordinateAxis1D) xAxis)
                                        .getCoordValue(i
                                            - 1) + ((CoordinateAxis1D) xAxis)
                                                .getIncrement());
                            } else {
                                coordData[0][idx] =
                                    (float) ((CoordinateAxis1D) xAxis)
                                        .getCoordValue(i);
                            }
                            coordData[1][idx] =
                                (float) ((CoordinateAxis1D) yAxis)
                                    .getCoordValue(j);
                            idx++;
                        }
                    }
                    lengths = new int[] { sizeX, sizeY };
                } else {  // CoordinateAxis2D
                    /*
                int[]      shape = xAxis.getShape();
                double[][] vals  = {
                    ((CoordinateAxis2D) xAxis).getCoordValues(),
                    ((CoordinateAxis2D) yAxis).getCoordValues()
                };
                coordData = Set.doubleToFloat(vals);
                */
                    int[]            shape   = xAxis.getShape();
                    int              iBounds = shape[0];
                    int              jBounds = shape[1];
                    CoordinateAxis2D xAxis2D = (CoordinateAxis2D) xAxis;
                    CoordinateAxis2D yAxis2D = (CoordinateAxis2D) yAxis;

                    Trace.call1("GeoGridAdapter.getCoordValues");
                    xAxis2D.getCoordValue(0, 0);
                    yAxis2D.getCoordValue(0, 0);
                    Trace.call2("GeoGridAdapter.getCoordValues");
                    float[] coordData0 = coordData[0];
                    float[] coordData1 = coordData[1];
                    for (int i = 0; i < iBounds; i++) {
                        for (int j = 0; j < jBounds; j++) {
                            coordData0[idx] =
                                (float) xAxis2D.getCoordValue(i, j);
                            coordData1[idx] =
                                (float) yAxis2D.getCoordValue(i, j);
                            idx++;
                        }
                    }
                    lengths = new int[] { shape[1], shape[0] };
                }
                units = new Unit[] { xUnit, yUnit };
            } else {  // 3D set
                if (is1D && needToWrapLon) {
                    sizeX = sizeX + 1;
                }
                Trace.call1("GeoGridAdapter:making coordData array",
                            " size:" + (sizeX * sizeY * sizeZ));
                coordData = (is1D)
                            ? new float[3][sizeX * sizeY * sizeZ]
                            : new float[3][sizeX * sizeZ];
                Trace.call2("GeoGridAdapter:making coordData array");

                int idx = 0;
                if (is1D) {
                    Trace.call1("GeoGridAdapter:getCoordValue");
                    float[] xValues = new float[sizeX];
                    float[] yValues = new float[sizeY];
                    for (int i = 0; i < xValues.length; i++) {
                        if (needToWrapLon && (i == sizeX - 1)) {
                            xValues[i] =
                                (float) (((CoordinateAxis1D) xAxis)
                                    .getCoordValue(i
                                        - 1) + ((CoordinateAxis1D) xAxis)
                                            .getIncrement());
                        } else {
                            xValues[i] =
                                (float) ((CoordinateAxis1D) xAxis)
                                    .getCoordValue(i);
                        }

                    }
                    for (int i = 0; i < yValues.length; i++) {
                        yValues[i] =
                            (float) ((CoordinateAxis1D) yAxis).getCoordValue(
                                i);
                    }
                    float[] coordData0 = coordData[0];
                    float[] coordData1 = coordData[1];
                    float[] coordData2 = coordData[2];
                    for (int k = 0; k < sizeZ; k++) {
                        float kValue = (float) zAxis.getCoordValue(k);
                        for (int j = 0; j < sizeY; j++) {
                            float yValue = yValues[j];
                            for (int i = 0; i < sizeX; i++) {
                                coordData0[idx] = xValues[i];
                                coordData1[idx] = yValue;
                                coordData2[idx] = kValue;
                                idx++;
                            }
                        }
                    }
                    Trace.call2("GeoGridAdapter:getCoordValue",
                                " cnt=" + idx);
                    lengths = (sizeZ > 1)
                              ? new int[] { sizeX, sizeY, sizeZ }
                              : new int[] { sizeX, sizeY };
                } else {

                    int[]            shape   = xAxis.getShape();
                    int              iBounds = shape[0];
                    int              jBounds = shape[1];
                    CoordinateAxis2D xAxis2D = (CoordinateAxis2D) xAxis;
                    CoordinateAxis2D yAxis2D = (CoordinateAxis2D) yAxis;

                    Trace.call1("GeoGridAdapter.getCoordValues");
                    xAxis2D.getCoordValue(0, 0);
                    yAxis2D.getCoordValue(0, 0);
                    zAxis.getCoordValue(0);
                    Trace.call2("GeoGridAdapter.getCoordValues");
                    float[] coordData0 = coordData[0];
                    float[] coordData1 = coordData[1];
                    float[] coordData2 = coordData[2];
                    for (int k = 0; k < sizeZ; k++) {
                        float kValue = (float) zAxis.getCoordValue(k);
                        for (int i = 0; i < iBounds; i++) {
                            for (int j = 0; j < jBounds; j++) {
                                coordData0[idx] =
                                    (float) xAxis2D.getCoordValue(i, j);
                                coordData1[idx] =
                                    (float) yAxis2D.getCoordValue(i, j);
                                coordData2[idx] = kValue;
                                idx++;
                            }
                        }
                    }
                    lengths = (sizeZ > 1)
                              ? new int[] { shape[1], shape[0], sizeZ }
                              : new int[] { shape[1], shape[0] };
                }
                units = new Unit[] { xUnit, yUnit, zUnit };
            }  // end 3D gridded set

            Trace.call1("GeoGridAdapter.spatial GriddedSet.create");
            try {
                domainSet = GriddedSet.create(domainTemplate, coordData,
                        lengths, (CoordinateSystem) null, units,
                        (ErrorEstimate[]) null, false, true);
            } catch (SetException se) {
                // if a SetException is thrown, then it's possible that the 
                // samples are missing or inconsistent.  Try again with 
                // test = false
                String msg = se.getMessage();
                if ((msg.indexOf("form a valid grid") >= 0)
                        || (msg.indexOf("may not be missing") >= 0)) {
                    domainSet = GriddedSet.create(domainTemplate, coordData,
                            lengths, (CoordinateSystem) null, units,
                            (ErrorEstimate[]) null, false, false);
                } else {
                    throw new VisADException(se);
                }
            }
            Trace.call2("GeoGridAdapter.spatial GriddedSet.create");
        }  // end non-linear
        log_.debug("Domain set = " + domainSet);

        return domainSet;
    }

    /**
     * Check if we need to wrap the longitudes.
     *
     * @param axis Axis to check
     *
     * @return true if need to wrap
     */
    private boolean checkNeedToWrapLon(CoordinateAxis axis) {
        if (axis.getRank() > 1) {
            return false;
        }
        CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;
        if (axis1D.isRegular()) {
            double first = axis1D.getCoordValue(0);
            double last  = axis1D.getCoordValue((int) axis1D.getSize() - 1);
            if (first + 360. != last) {
                double newLast = last + axis1D.getIncrement();
                // Sometimes the increment has a roundoff error
                if (GridUtil.isLonCyclic(first, newLast)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Make a domain set that supports a vertical transform.
     *
     * @param vt    VerticalTransform
     * @param domainSet 3D domain set from data
     * @param refVals  reference vals
     * @param timeIndex time index
     *
     * @return Gridded3DSet with EmpiricalCoordianteSystem to transform
     *         vertical coordinate to Altitude
     *
     * @throws VisADException Problem creating the CoordinateSystem
     */
    private GriddedSet makeDomainWithVerticalTransform(VerticalTransform vt,
            GriddedSet domainSet, float[][] refVals, int timeIndex)
            throws VisADException {

        Gridded3DSet newDSet = (Gridded3DSet) domainSet;
        try {
            CoordinateSystem cs = domainSet.getCoordinateSystem();
            if (cs == null) {
                return newDSet;
            }
            RealTupleType setType =
                ((SetType) domainSet.getType()).getDomain();
            RealTupleType   refType   = cs.getReference();
            ErrorEstimate[] oldErrors = domainSet.getSetErrors();
            ErrorEstimate[] newErrors = new ErrorEstimate[oldErrors.length];

            // now create a new domain type based on the vertical transform
            Unit vtu = getUnit(vt.getUnitString());
            log_.debug("vtu = " + vtu);
            RealType[] types                  = refType.getRealComponents();
            boolean    isPressure             = false;
            boolean    isGeopotentialAltitude = false;


            if ( !Unit.canConvert(vtu, CommonUnit.meter)) {  // other than height
                if (Unit.canConvert(vtu, CommonUnits.MILLIBAR)) {
                    isPressure = true;
                } else if (Unit.canConvert(
                        vtu, GeopotentialAltitude.getGeopotentialMeter())) {
                    isGeopotentialAltitude = true;
                } else {
                    throw new VisADException("unknown vertical coordinate");
                }
            }
            RealTupleType newDomainType = new RealTupleType(types[0],
                                              types[1], RealType.Altitude);

            Trace.call1("GeoGridAdapter.getCoordinateArray",
                        " vt:" + vt.getClass().getName());
            // now, let's substitute the altitude values in
            Array array = vt.getCoordinateArray(timeIndex);
            Trace.call2("GeoGridAdapter.getCoordinateArray");

            Trace.call1("GeoGridAdapter.get1DValues");
            float[] vals = DataUtil.toFloatArray(array);
            if (vals.length != domainSet.getLength()) {
                int[]   dl    = domainSet.getLengths();
                float[] valsT = new float[domainSet.getLength()];
                int     ii    = 0;
                int     jj    = 0;
                int     xl    = dl[0] - 1;
                if (dl.length == 2) {
                    for (int j = 0; j < dl[1]; j++) {
                        for (int i = 0; i < xl; i++) {
                            ii        = i + j * xl;
                            jj        = i + j * (xl + 1);
                            valsT[jj] = vals[ii];
                        }
                        valsT[jj + 1] = valsT[j * xl];
                    }
                } else if (dl.length == 3) {
                    int yl = dl[1];
                    for (int k = 0; k < dl[2]; k++) {
                        for (int j = 0; j < dl[1]; j++) {
                            for (int i = 0; i < xl; i++) {
                                ii        = i + j * xl + k * xl * yl;
                                jj        = i + j * (xl + 1) + k * xl * yl;
                                valsT[jj] = vals[ii];
                            }
                            valsT[jj + 1] = valsT[j * xl + k * xl * yl];
                        }
                    }
                }
                refVals[2] = valsT;
            } else {
                refVals[2] = vals;
            }
            Trace.call2("GeoGridAdapter.get1DValues");

            if (isPressure) {  // convert to altitude using standard atmos
                CoordinateSystem vcs = DataUtil.getPressureToHeightCS(vertcs);
                refVals[2] = vcs.toReference(new float[][] {
                    refVals[2]
                }, new Unit[] { vtu })[0];
                vtu        = vcs.getReferenceUnits()[0];
            } else if (isGeopotentialAltitude) {
                refVals[2] = GeopotentialAltitude.toAltitude(refVals[2], vtu,
                        Gravity.newReal(), refVals[2], CommonUnit.meter,
                        false);
                vtu = CommonUnit.meter;
            }
            /*
            for (int i = 0; i < 10; i++) {
                System.out.println("vals["+i+"] = " + refVals[2][i]);
            }
            */


            int[]  lengths        = domainSet.getLengths();
            Unit[] newDomainUnits = newDomainType.getDefaultUnits();
            newDomainUnits[2] = vtu;


            Trace.call1("GeoGridAdapter.new GriddedSet");
            Gridded3DSet newDomain =
                (Gridded3DSet) GriddedSet.create(newDomainType, refVals,
                    lengths, null, newDomainUnits, newErrors, false, false);

            // it's a slice - no need to use Empirical CS
            if (domainSet.getManifoldDimension() == 2) {
                return newDomain;
            }
            Trace.call2("GeoGridAdapter.new GriddedSet");

            Trace.call1("GeoGridAdapter.new EmpiricalCoordinateSystem");
            EmpiricalCoordinateSystem ecs =
                new EmpiricalCoordinateSystem(domainSet, newDomain, false,
                    false);

            Trace.call2("GeoGridAdapter.new EmpiricalCoordinateSystem");

            //CoordinateSystem gcs = new CachingCoordinateSystem(ecs);
            CoordinateSystem gcs = ecs;


            RealTupleType newSetType =
                new RealTupleType(setType.getRealComponents(), gcs, null);

            Trace.call1("GeoGridAdapter final GriddedSet");
            newDSet = (Gridded3DSet) GriddedSet.create(newSetType,
                    domainSet.getSamples(false), lengths, null,
                    domainSet.getSetUnits(), oldErrors, false, false);
            Trace.call2("GeoGridAdapter final GriddedSet");

        } catch (VisADException ve) {
            throw ve;
        } catch (Exception re) {
            re.printStackTrace();
            throw new VisADException(re.getMessage());
        }
        return newDSet;
    }

    /**
     * Get a time ordered sequence of 2D flat fields
     *
     * @return data for all the times for a particular 2D field
     */
    public FieldImpl getSequence() {
        return makeSequence(null);
    }

    /**
     * Get a time ordered sequence of 2D flat fields
     *
     * @param timeIndices  indices of times in the data
     *
     * @return data for all the times for a particular 2D field
     */
    public FieldImpl getSequence(int[] timeIndices) {
        return makeSequence(timeIndices);
    }


    /**
     * Get a time ordered sequence of 2D flat fields
     *
     * @param timeIndices  indices of times in the data
     * @param loadId  loadId
     *
     * @return data for all the times for a particular 2D field
     */
    public FieldImpl getSequence(int[] timeIndices, Object loadId) {
        return makeSequence(timeIndices, null, loadId);
    }

    /**
     * Get a time ordered sequence of 2D flat fields
     *
     * @param timeIndices  indices of times in the data
     * @param memberIndices the member indices
     * @param loadId  loadId
     *
     * @return data for all the times for a particular 2D field
     */
    public FieldImpl getSequence(int[] timeIndices, int[] memberIndices,
                                 Object loadId) {
        return makeSequence(timeIndices, memberIndices, loadId);
    }

    /**
     * Get all the data from this GeoGrid.
     *
     * @return  VisAD representation of this GeoGrid
     *
     * @throws VisADException  problem creating the grid
     */
    public FieldImpl getData() throws VisADException {
        return ((CoordinateAxis1D) geoGrid.getCoordinateSystem()
            .getTimeAxis() != null)
               ? getSequence()
               : (getBaseTime() == null)
                 ? getFlatField(0, "")
                 : makeSequence(null);
    }

    /** Flag for testing */
    static boolean makeGeoGridFlatField = true;


    /**
     * Create a FlatField for the particular time index.  Retrieve from
     * cache if possible.
     *
     * @param timeIndex  index into set of times
     * @param readLabel  label to write to
     * @return  the data at that time
     *
     * @throws VisADException  problem creating the FlatField
     */
    private CachedFlatField getFlatField(int timeIndex, String readLabel)
            throws VisADException {
        return getFlatField(timeIndex, -1, readLabel);
    }

    /**
     * Create a FlatField for the particular time index.  Retrieve from
     * cache if possible.
     *
     * @param timeIndex  index into set of times
     * @param ensIndex the ensemble index
     * @param readLabel  label to write to
     * @return  the data at that time
     *
     * @throws VisADException  problem creating the FlatField
     */
    private CachedFlatField getFlatField(int timeIndex, int ensIndex,
                                         String readLabel)
            throws VisADException {

        String baseCacheKey = "t_" + timeIndex;
        List   cacheKey     = Misc.newList(baseCacheKey);
        if (ensIndex >= 0) {
            String ensCacheKey = "e_" + ensIndex;
            cacheKey.add(ensCacheKey);
        } else {
            ensIndex = 0;
        }
        if (extraCacheKey != null) {
            cacheKey.add(extraCacheKey);
        }

        CachedFlatField retField = null;
        if (retField != null) {
            //            System.err.println("in cache");
            return retField;
        }
        Trace.call1("GeoGridAdapter.getFlatField:" + paramName + ":time="
                    + timeIndex);

        GridCoordSystem gcs = geoGrid.getCoordinateSystem();

        /* get the multiarray from the GeoGrid, and make an Array
         * with the data from the appropriate time */

        Trace.call1("GeoGridAdapter.getSpatialDomainSet");
        GriddedSet domainSet = getSpatialDomainSet(geoGrid, timeIndex);

        Trace.call2("GeoGridAdapter.getSpatialDomainSet");

        Array arr;
        FunctionType ffType =
            new FunctionType(((SetType) domainSet.getType()).getDomain(),
                             paramType);
        if ( !makeGeoGridFlatField) {
            System.err.println("making flat field");
            try {
                LogUtil.message(readLabel);
                Trace.call1("GeoGridAdapter.geogrid.readVolumeData");
                //                synchronized(geoGrid) {
                System.err.println(System.currentTimeMillis() + " time:"
                                   + timeIndex + " start read");
                //arr = geoGrid.readVolumeData(timeIndex);
                arr = geoGrid.readDataSlice(0, ensIndex, timeIndex, -1, -1,
                                            -1);
                System.err.println(System.currentTimeMillis() + " time:"
                                   + timeIndex + " end read");
                //                }
                Trace.call2("GeoGridAdapter.geogrid.readVolumeData");
                // 3D grid with one level - slice to 2D grid
                if ((arr.getRank() > 2) && (domainSet.getDimension() == 2)) {
                    int[] lengths    = domainSet.getLengths();
                    int   sizeX      = lengths[0];
                    int   sizeY      = lengths[1];
                    int   levelIndex = 0;  // get the first by default
                    int[] shape      = arr.getShape();
                    for (int i = 0; i <= arr.getRank(); i++) {
                        // find the index whose dimension is not x or y
                        if ((shape[i] != sizeX) && (shape[i] != sizeY)) {
                            // extract the correct "z" level data:
                            arr = arr.slice(i, levelIndex);
                            break;
                        }
                    }
                }
                //arr = ma.read();
            } catch (RemoteException e) {
                LogUtil.printException(
                    log_, "getFlatField read got RemoteException", e);
                return null;
            } catch (IOException e) {
                LogUtil.printException(log_,
                                       "getFlatField read got IOException",
                                       e);
                return null;
            }


            /* Simple Java arrays are used to make FlatFields:
             *   to hold coordinates:
             *   there are x,y,z values for every point in the 3 D array;
             *   they are ALL stored here (allows for non regular grid, possibly)
             */

            //        Trace.call1("toFloatArray", " array:" + arr.getClass().getName());
            final float[][] fieldArray = new float[1][];
            //fieldArray[0] = DataUtil.toFloatArray(arr);
            float[] values = DataUtil.toFloatArray(arr);
            if (values.length < domainSet.getLength()) {  // need to extend array
                float[] newValues = new float[domainSet.getLength()];
                int[]   lengths   = domainSet.getLengths();
                int     l         = 0;
                int     sizeX     = lengths[0];
                int     sizeY     = lengths[1];
                if (lengths.length == 2) {
                    for (int j = 0; j < sizeY; j++) {
                        for (int i = 0; i < sizeX; i++) {
                            int xpos = (i < sizeX - 1)
                                       ? i
                                       : 0;
                            newValues[l++] = values[j * (sizeX - 1) + xpos];
                        }
                    }
                } else {
                    for (int k = 0; k < lengths[2]; k++) {
                        for (int j = 0; j < sizeY; j++) {
                            for (int i = 0; i < sizeX; i++) {
                                int xpos = (i < sizeX - 1)
                                           ? i
                                           : 0;
                                newValues[l++] =
                                    values[k * sizeY * (sizeX - 1) + j * (sizeX - 1) + xpos];
                            }
                        }
                    }
                }
                fieldArray[0] = newValues;

            } else {
                fieldArray[0] = values;
            }
            //        Trace.call2("toFloatArray", " length:" + fieldArray[0].length);
            retField = new CachedFlatField(ffType, domainSet, fieldArray);
        } else {
            Object readLockToUse = (dataSource.isLocalFile()
                                    ? readLock
                                    : new Object());

            GeoGridFlatField ggff = new GeoGridFlatField(geoGrid,
                                        readLockToUse, timeIndex, ensIndex,
                                        domainSet, ffType);


            ggff.setReadLabel(readLabel);
            retField = ggff;
            //Not sure why we had this here since this forces a data read
            //            ggff.unpackFloats(false);
        }

        dataSource.putCache(cacheKey, retField);
        Trace.call2("GeoGridAdapter.getFlatField:" + paramName + ":time="
                    + timeIndex);

        return retField;

    }

    /**
     * Get a time ordered sequence of FlatFields
     *
     * @param timeIndices  indices of requested times
     *
     * @return all grid data for all the times requested, in proper increasing
     * time order, for the particular parameter loaded in this GeoGridAdapter.
     */
    private FieldImpl makeSequence(int[] timeIndices) {
        return makeSequence(timeIndices, null, null);
    }

    /**
     * Get a time ordered sequence of FlatFields
     *
     * @param timeIndices  indices of requested times
     * @param memberIndices  the member indices
     * @param loadId  loadId
     *
     * @return all grid data for all the times requested, in proper increasing
     * time order, for the particular parameter loaded in this GeoGridAdapter.
     */
    private FieldImpl makeSequence(int[] timeIndices, int[] memberIndices,
                                   Object loadId) {

        FieldImpl data = null;



        Trace.call1("GeoGridAdapter.makeSequence");
        try {
            final TreeMap        gridMap  = new TreeMap();
            GridCoordSystem      geoSys   = geoGrid.getCoordinateSystem();
            CoordinateAxis1DTime timeAxis = geoSys.getTimeAxis1D();
            int[]                times;

            java.util.Date[]     dates = null;
            if (timeAxis != null) {
                dates = timeAxis.getTimeDates();
            }

            if (timeAxis == null) {
                times = new int[] { 0 };
            } else if (timeIndices == null) {
                int numTimes = (int) timeAxis.getSize();
                times = new int[numTimes];
                for (int i = 0; i < numTimes; i++) {
                    times[i] = i;
                }
            } else {
                times = timeIndices;
            }
            final Range[][] sampleRanges   = {
                null
            };
            StringBuffer    testModeBuffer = null;

            ThreadManager threadManager =
                new ThreadManager("GeoGrid data reading");
            //            threadManager.debug = true;
            for (int i = 0; i < times.length; i++) {
                if ( !JobManager.getManager().canContinue(loadId)) {
                    return null;
                }
                DateTime time;
                if (times[i] >= 0) {
                    if (timeAxis != null) {
                        time = new DateTime(dates[times[i]]);
                    } else {
                        time = getBaseTime();  // will be null if not found
                        if (time == null) {
                            if (timeAxis == null) {
                                return getFlatField(0, "");
                            } else {
                                // return current time.
                                // probably not good, but what the hey.
                                time = new DateTime();
                            }
                        }
                    }

                    log_.debug("  ...grid " + i);
                    log_.debug("    data time " + time);


                    final String readLabel = "Time: " + (i + 1) + "/"
                                             + times.length + " " + paramName
                                             + " From: "
                                             + dataSource.toString();


                    final int      theTimeIndex     = times[i];
                    final DateTime theTime          = time;
                    final int[]    theMemberIndices = memberIndices;
                    threadManager.addRunnable(new ThreadManager.MyRunnable() {
                        public void run() throws Exception {
                            readTimeStep(theTimeIndex, theTime, readLabel,
                                         gridMap, sampleRanges,
                                         lazyEvaluation, theMemberIndices);
                        }
                    });
                    try {
                        //                        readTimeStep(times[i],time, readLabel, 
                        //                                     gridMap, sampleRanges,lazyEvaluation );
                    } catch (Exception excp) {
                        throw new WrapperException(excp);
                    }
                }

            }
            if ((dataSource.getIdv() == null) || dataSource.isLocalFile()) {
                threadManager.runSequentially();
            } else {
                threadManager
                    .runInParallel(dataSource.getDataContext().getIdv()
                        .getMaxDataThreadCount());
            }

            //            System.err.println ("GeoGridAdapter DONE");
            log_.debug("    found " + gridMap.size() + " times");

            java.util.Set keySet = gridMap.keySet();
            if (gridMap.size() > 0) {
                SampledSet domain = (gridMap.size() == 1)
                                    ? (SampledSet) new SingletonSet(
                                        new RealTuple(
                                            new Real[] {
                                                (DateTime) gridMap
                                                    .firstKey() }))
                                    : (SampledSet) DateTime.makeTimeSet(
                                        (DateTime[]) keySet.toArray(
                                            new DateTime[keySet.size()]));
                int i = 0;

                for (Iterator iter = keySet.iterator(); iter.hasNext(); ) {
                    FieldImpl field = (FieldImpl) gridMap.get(iter.next());

                    if (i == 0) {
                        FunctionType fType = new FunctionType(RealType.Time,
                                                 field.getType());

                        data = new FieldImpl(fType, domain);
                    }

                    data.setSample(i, field, false);

                    i++;
                }
            } else {
                if (testModeBuffer != null) {
                    System.err.println(testModeBuffer.toString());
                }
            }
        } catch (Exception e) {
            LogUtil.logException("Couldn't get data ", e);
        }
        Trace.call2("GeoGridAdapter.makeSequence");
        return data;
    }


    /**
     * Read the given time step from the GeoGrid
     *
     * @param timeIndex the time index
     * @param time The time
     * @param readLabel What to show in the status bar
     * @param gridMap where to store the grid
     * @param sampleRanges sample ranges
     * @param lazyEvaluation Are we using the CachedFlatField
     * @param memberIndices  the member indices
     *
     * @throws Exception On badness
     */
    private void readTimeStep(int timeIndex, DateTime time, String readLabel,
                              TreeMap gridMap, Range[][] sampleRanges,
                              boolean lazyEvaluation, int[] memberIndices)
            throws Exception {
        Dimension    ensDim = geoGrid.getEnsembleDimension();
        Gridded1DSet ensSet = null;
        FieldImpl    sample = null;
        int          numEns = 1;
        if ((memberIndices != null)) {      //&& (memberIndices.length > 1)) {
            numEns = memberIndices.length;  //ensDim.getLength();
            //ensSet = new Integer1DSet(GridUtil.ENSEMBLE_TYPE, numEns);
            float[][] setVals = new float[1][numEns];
            for (int i = 0; i < numEns; i++) {
                setVals[0][i] = memberIndices[i];
            }
            ensSet = new Gridded1DSet(GridUtil.ENSEMBLE_TYPE, setVals,
                                      numEns);
        } else if ((ensDim != null) && (ensDim.getLength() > 1)) {
            numEns = ensDim.getLength();
            ensSet = new Integer1DSet(GridUtil.ENSEMBLE_TYPE, numEns);
        }
        for (int i = 0; i < numEns; i++) {
            int ii = i;
            if (memberIndices != null) {
                ii = memberIndices[i];
            }
            CachedFlatField oneTime = getFlatField(timeIndex, ii, readLabel);
            synchronized (sampleRanges) {
                if (sampleRanges[0] == null) {
                    sampleRanges[0] =
                        GridUtil.makeRanges(oneTime.getRanges(true));
                    //Check to see if the sample is valid
                    if ((sampleRanges[0] != null)
                            && (sampleRanges[0].length > 0)) {
                        for (int rangeIdx = 0;
                                rangeIdx < sampleRanges[0].length;
                                rangeIdx++) {
                            Range r = sampleRanges[0][rangeIdx];
                            if ((r.getMin() == r.getMax())
                                    || Double.isInfinite(r.getMin())
                                    || Double.isInfinite(r.getMax())) {
                                sampleRanges[0] = null;
                                //  System.err.println("bad sample range");
                                break;
                            }
                        }
                    }
                }
            }

            oneTime.setSampleRanges(GridUtil.makeDataRanges(sampleRanges[0]));
            if (numEns == 1) {
                sample = oneTime;
            } else {
                if ((sample == null) && (oneTime != null)) {
                    sample = new FieldImpl(
                        new FunctionType(
                            ((SetType) ensSet.getType()).getDomain(),
                            oneTime.getType()), ensSet);
                }
                if (oneTime != null) {
                    sample.setSample(i, oneTime, false);
                }
            }
        }


        if ((sample != null) && !sample.isMissing()) {
            if (lazyEvaluation) {
                //If we are running under lazy evaluation then
                //we don't want to do the fieldMinMax because it
                //will force a read of the data
                synchronized (gridMap) {
                    gridMap.put(time, sample);
                }
            } else {
                Range range = GridUtil.getMinMax(sample)[0];
                // For now, min and max are flipped if all values were NaN
                if ( !(Double.isInfinite(range.getMin())
                        && Double.isInfinite(range.getMax()))) {
                    //When we are testing break after we've read one time.
                    synchronized (gridMap) {
                        gridMap.put(time, sample);
                    }
                }
            }
        }
    }



    /**
     * make proper RealType from name and unit name.
     *
     * @param name    name for the RealType
     * @param unit    RealType Unit
     * @return  RealType or null
     *
     * @throws VisADException  problem creating the RealType
     */
    private RealType makeRealType(String name, Unit unit)
            throws VisADException {

        return DataUtil.makeRealType(name, unit);
    }

    /**
     * Get the unit from the string
     *
     * @param uString    unit specification
     * @return  corresponding Unit
     */
    private Unit getUnit(String uString) {
        Unit r = null;
        try {
            r = DataUtil.parseUnit(uString);
        } catch (Exception excp) {
            System.err.println("Unknown unit " + uString);
            r = null;
        }
        return r;
    }

    /**
     * Make a Linear1DSet from a 1D axis.  Use the RealType for the
     * MathType of the set.  Must ensure that axis is linear
     * (use {@link #checkLinearity(CoordinateAxis, boolean}) before
     * calling this.
     *
     * @param  axis  1D CoordinateAxis
     * @param  type  MathType for LinearSet.
     *
     * @return  Linear1DSet representing the axis
     *
     * @throws VisADException  problem making set
     */
    private Linear1DSet makeLinear1DSet(CoordinateAxis1D axis, RealType type)
            throws VisADException {
        return makeLinear1DSet(axis, type, null);
    }

    /**
     * Make a Linear1DSet from a 1D axis.  Use the RealType for the
     * MathType of the set.  Must ensure that axis is linear
     * (use {@link #checkLinearity(CoordinateAxis, boolean}) before
     * calling this.
     *
     * @param  axis  1D CoordinateAxis
     * @param  type  MathType for LinearSet.
     * @param  u     unit for data
     *
     * @return  Linear1DSet representing the axis
     *
     * @throws VisADException  problem making set
     */
    private Linear1DSet makeLinear1DSet(CoordinateAxis1D axis, RealType type,
                                        Unit u)
            throws VisADException {
        return makeLinear1DSet(axis, type, u, false);
    }

    /**
     * Make a Linear1DSet from a 1D axis.  Use the RealType for the
     * MathType of the set.  Must ensure that axis is linear
     * (use {@link #checkLinearity(CoordinateAxis, boolean}) before
     * calling this.
     *
     * @param  axis  1D CoordinateAxis
     * @param  type  MathType for LinearSet.
     * @param  u     unit for data
     * @param  extend  extend by one point
     *
     * @return  Linear1DSet representing the axis
     *
     * @throws VisADException  problem making set
     */
    private Linear1DSet makeLinear1DSet(CoordinateAxis1D axis, RealType type,
                                        Unit u, boolean extend)
            throws VisADException {
        Trace.call1("GeoGridAdapter.makeLinear1DSet");
        double start = axis.getCoordValue(0);
        double end   = axis.getCoordValue((int) axis.getSize() - 1);
        if (extend) {
            end = end + axis.getIncrement();
        }
        int numPoints = (int) axis.getSize();
        if (extend) {
            numPoints++;
        }

        Linear1DSet result = new Linear1DSet(type, start, end, numPoints,
                                             (CoordinateSystem) null,
                                             new Unit[] { u },
                                             (ErrorEstimate[]) null, true);  // cache the results
        Trace.call2("GeoGridAdapter.makeLinear1DSet");
        return result;
    }

    /**
     * Check to see if the axis is a linear progression or not.  If
     * it's longitude, use a different algorithm.
     *
     * @param  axis  CoordinateAxis to check.
     * @param isLon  true if this is a set of longitudes
     *
     * @return  true if a linear progression, false if not or 2D axis
     *
     * @throws VisADException  unable to check
     */
    private boolean checkLinearity(CoordinateAxis axis, boolean isLon)
            throws VisADException {

        if (axis.getRank() > 1) {
            return false;
        }
        if (axis.getSize() <= 1) {
            return false;
        }
        if ( !isLon && ((CoordinateAxis1D) axis).isRegular()) {
            return true;
        }
        ArithProg progChecker = (isLon == true)
                                ? new LonArithProg()
                                : new ArithProg();
        int       i           = 0;
        boolean   linear      = true;
        while ((i < axis.getSize()) && linear) {
            linear = progChecker.accumulate(
                ((CoordinateAxis1D) axis).getCoordValue(i));
            i++;
        }
        return linear;
    }

    /**
     * HACK to make a RadarCoordinateSystem for a radar dataset.
     * @return  RadarCoordinateSystem
     *
     * @throws VisADException  problem making CS
     */
    private CoordinateSystem makeElevationCS() throws VisADException {
        if (ncFile == null) {
            throw new VisADException("Unable to determine center point ");
        }
        Variable lat = ncFile.findVariable("sensor_latitude");
        Variable lon = ncFile.findVariable("sensor_longitude");
        Variable alt = ncFile.findVariable("sensor_altitude");
        if ((lat == null) || (lon == null) || (alt == null)) {
            throw new VisADException(
                "Unable to find center point variables ");
        }
        Real latitude  = makeReal(lat, RealType.Latitude);
        Real longitude = makeReal(lon, RealType.Longitude);
        Real altitude  = makeReal(alt, RealType.Altitude);
        return new RadarGridCoordinateSystem(
            latitude.getValue(CommonUnit.degree),
            longitude.getValue(CommonUnit.degree),
            altitude.getValue(CommonUnit.meter));
    }

    /**
     * Make a Real from a <code>Variable</code> and a <code>RealType</code>
     *
     * @param v  variable
     * @param rt  RealType
     * @return   the Real
     *
     * @throws VisADException
     */
    private Real makeReal(Variable v, RealType rt) throws VisADException {
        Unit      unit;
        double    value;
        Attribute a = v.findAttribute("units");
        try {
            unit = (a != null)
                   ? DataUtil.parseUnit(a.getStringValue())
                   : rt.getDefaultUnit();
            Array array = v.read();
            value = array.getDouble(array.getIndex());

        } catch (IOException ioe) {
            throw new VisADException("couldn't read varaible " + v);
        } catch (Exception pe) {
            throw new VisADException("couldn't parse unit "
                                     + a.getStringValue());
        }
        return new Real(rt, value, unit);
    }

    // Attempt to get time from the netCDF file.  If not, return current
    // time

    /**
     * Attempt to get time from the netCDF file.  If not, return current
     * time
     *
     * @return  time
     *
     * @throws VisADException  problem creating VisAD object
     */
    private DateTime getBaseTime() throws VisADException {
        DateTime time = null;
        if (ncFile != null) {
            Variable timeVar = ncFile.findVariable("base_time");
            if (timeVar != null) {  // found it
                try {
                    time = new DateTime(makeReal(timeVar, RealType.Time));
                } catch (VisADException ve) {}
            }
        }
        return time;
    }

    /**
     * Utility to check if we should ignore the given z axis
     *
     * @param zaxis given z axis
     *
     * @return Is ok
     */
    public static boolean isZAxisOk(CoordinateAxis1D zaxis) {

        /*
        if ((zaxis != null) && (zaxis.getDescription() != null)
                && (zaxis.getDescription().startsWith("Hybrid")
                    || zaxis.getDescription().startsWith("PotTemp")
                    || zaxis.getDescription().startsWith("Boundary"))) {
            return false;
        }
        */
        return true;
    }

    /**
     * Utility to check if we can handle the vertical transform
     * @param zUnit  zUnit
     * @param vt     vertical transform
     * @return true if we can
     */
    public static boolean isZUnitOk(Unit zUnit, VerticalTransform vt) {
        return (vt != null)
               || ((zUnit != null)
                   && (Unit.canConvert(zUnit, CommonUnits.MILLIBAR)
                       || Unit.canConvert(zUnit, CommonUnit.meter)));
    }

    /**
     * Check to see if we shouuld handle this as a 2D grid
     * @param sizeZ  size of the z axis
     * @param zUnit  zUnit
     * @param vt     vertical transform
     * @return true if we can
     */
    private boolean is2D(int sizeZ, Unit zUnit, VerticalTransform vt) {
        return (sizeZ < 1) || ((sizeZ == 1) && !isZUnitOk(zUnit, vt));
    }

}
