/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

// $Id: GridHorizCoordSys.java 70 2006-07-13 15:16:05Z caron $

package ucar.unidata.data.grid.gempak;


import ucar.ma2.Array;
import ucar.ma2.DataType;

import ucar.nc2.*;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;

import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.GaussianLatitudes;
import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;


/**
 * A horizontal coordinate system created from a GridDefRecord
 * <p/>
 * <p/>
 * <p> Note on "false_easting" and "fale_northing" projection parameters:
 * <ul><li>false_easting(northing) = The value added to all x (y) values in the rectangular coordinates for a map projection.
 * This value frequently is assigned to eliminate negative numbers.
 * Expressed in the unit of measure identified in Planar Coordinate Units.
 * <li>We dont currently use, assuming that the x and y are just fine as negetive numbers.
 *
 * @author caron
 * @version $Revision: 70 $ $Date: 2006-07-13 15:16:05Z $
 */
public class GridHorizCoordSys {

    /** _more_          */
    private GridTableLookup lookup;

    /** _more_          */
    private GridDefRecord gdsIndex;

    /** _more_          */
    private Group g;

    /** _more_          */
    private boolean debug = false;

    /** _more_          */
    private String grid_name, shape_name, id;

    /** _more_          */
    private boolean isLatLon   = true,
                    isGaussian = false;

    /** _more_          */
    HashMap varHash = new HashMap(200);  // GridVariables that have this GridHorizCoordSys

    /** _more_          */
    HashMap productHash = new HashMap(100);  // List of GridVariable, sorted by product desc

    /** _more_          */
    HashMap vcsHash = new HashMap(30);  // GridVertCoordSys

    /** _more_          */
    private double startx, starty;

    /** _more_          */
    private ProjectionImpl proj;

    /** _more_          */
    private ArrayList attributes = new ArrayList();

    /**
     * _more_
     *
     * @param gdsIndex _more_
     * @param lookup _more_
     * @param g _more_
     */
    GridHorizCoordSys(GridDefRecord gdsIndex, GridTableLookup lookup,
                      Group g) {
        this.gdsIndex = gdsIndex;
        this.lookup   = lookup;
        this.g        = g;

        this.grid_name = NetcdfFile.createValidNetcdfObjectName(
            lookup.getGridName(gdsIndex));
        this.shape_name = lookup.getShapeName(gdsIndex);
        this.g          = g;
        isLatLon        = lookup.isLatLon(gdsIndex);
        grid_name       = StringUtil.replace(grid_name, ' ', "_");
        id              = (g == null)
                          ? grid_name
                          : g.getName();

        if (isLatLon
                && (lookup.getProjectionType(gdsIndex)
                    == GridTableLookup.GaussianLatLon)) {
            isGaussian = true;
            double np = 90.0;
            String nps = getParamString("Np");  // # lats between pole and equator  (octet 26/27)
            if (null != nps) {
                np = Double.parseDouble(nps);
            }
            gdsIndex.addParam(gdsIndex.DX, String.valueOf(np));  // fake - need to get actual gaussian calculation here

            // hack-a-whack : who is this for ???
            // gdsIndex.dy = 2 * gdsIndex.La1 / gdsIndex.ny;
            //gdsIndex.nx = 800;
            //gdsIndex.dx = 360.0/gdsIndex.nx;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    String getID() {
        return id;
    }  // unique within the file

    /**
     * _more_
     *
     * @return _more_
     */
    String getGridName() {
        return grid_name;
    }  // used in CF-1 attributes

    /**
     * _more_
     *
     * @return _more_
     */
    Group getGroup() {
        return g;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    boolean isLatLon() {
        return isLatLon;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    int getNx() {
        return (int) getParamValue(gdsIndex.NX);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    int getNy() {
        return (int) getParamValue(gdsIndex.NY);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private double getDxInKm() {
        return getParamValue(gdsIndex.DX) * .001;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private double getDyInKm() {
        return getParamValue(gdsIndex.DY) * .001;
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     */
    void addDimensionsToNetcdfFile(NetcdfFile ncfile) {

        if (isLatLon) {
            ncfile.addDimension(g, new Dimension("lat",
                    (int) getParamValue(gdsIndex.NY), true));
            ncfile.addDimension(g, new Dimension("lon",
                    (int) getParamValue(gdsIndex.NX), true));
        } else {
            ncfile.addDimension(g, new Dimension("y",
                    (int) getParamValue(gdsIndex.NY), true));
            ncfile.addDimension(g, new Dimension("x",
                    (int) getParamValue(gdsIndex.NX), true));
        }
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     */
    void addToNetcdfFile(NetcdfFile ncfile) {

        if (isLatLon) {
            double dy = (getParamValue("La2") < getParamValue(gdsIndex.LA1))
                        ? -getParamValue(gdsIndex.DY)
                        : getParamValue(gdsIndex.DY);
            if (isGaussian) {
                addGaussianLatAxis(ncfile, "lat", "degrees_north",
                                   "latitude coordinate", "latitude",
                                   AxisType.Lat);
            } else {
                addCoordAxis(ncfile, "lat", (int) getParamValue(gdsIndex.NY),
                             getParamValue(gdsIndex.LA1), dy,
                             "degrees_north", "latitude coordinate",
                             "latitude", AxisType.Lat);
            }

            addCoordAxis(ncfile, "lon", (int) getParamValue(gdsIndex.NX),
                         getParamValue(gdsIndex.LO1),
                         getParamValue(gdsIndex.DX), "degrees_east",
                         "longitude coordinate", "longitude", AxisType.Lon);
            addCoordSystemVariable(ncfile, "latLonCoordSys", "time lat lon");

        } else {
            makeProjection(ncfile);
            double[] yData = addCoordAxis(ncfile, "y",
                                          (int) getParamValue(gdsIndex.NY),
                                          starty, getDyInKm(), "km",
                                          "y coordinate of projection",
                                          "projection_y_coordinate",
                                          AxisType.GeoY);
            double[] xData = addCoordAxis(ncfile, "x",
                                          (int) getParamValue(gdsIndex.NX),
                                          startx, getDxInKm(), "km",
                                          "x coordinate of projection",
                                          "projection_x_coordinate",
                                          AxisType.GeoX);
            // TODO: ?
            //if (GribServiceProvider.addLatLon) addLatLon2D(ncfile, xData, yData);
            //add2DCoordSystem(ncfile, "projectionCoordSys", "time y x"); // LOOK is this needed?
        }
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     * @param name _more_
     * @param n _more_
     * @param start _more_
     * @param incr _more_
     * @param units _more_
     * @param desc _more_
     * @param standard_name _more_
     * @param axis _more_
     *
     * @return _more_
     */
    private double[] addCoordAxis(NetcdfFile ncfile, String name, int n,
                                  double start, double incr, String units,
                                  String desc, String standard_name,
                                  AxisType axis) {

        // ncfile.addDimension(g, new Dimension(name, n, true));

        Variable v = new Variable(ncfile, g, null, name);
        v.setDataType(DataType.DOUBLE);
        v.setDimensions(name);

        // create the data
        double[] data = new double[n];
        for (int i = 0; i < n; i++) {
            data[i] = start + incr * i;
        }
        Array dataArray = Array.factory(DataType.DOUBLE.getClassType(),
                                        new int[] { n }, data);
        v.setCachedData(dataArray, false);

        v.addAttribute(new Attribute("units", units));
        v.addAttribute(new Attribute("long_name", desc));
        v.addAttribute(new Attribute("standard_name", standard_name));
        v.addAttribute(new Attribute("grid_spacing", incr + " " + units));
        v.addAttribute(new Attribute(_Coordinate.AxisType, axis.toString()));

        ncfile.addVariable(g, v);
        return data;
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     * @param name _more_
     * @param units _more_
     * @param desc _more_
     * @param standard_name _more_
     * @param axis _more_
     *
     * @return _more_
     */
    private double[] addGaussianLatAxis(NetcdfFile ncfile, String name,
                                        String units, String desc,
                                        String standard_name, AxisType axis) {

        double np = getParamValue("NumberParallels");
        if (Double.isNaN(np)) {
            np = getParamValue("Np");
        }
        if (Double.isNaN(np)) {
            throw new IllegalArgumentException(
                "Gaussian LAt/Lon grid must have NumberParallels parameter");
        }
        double            startLat       = getParamValue(gdsIndex.LA1);
        double            endLat         = getParamValue("La2");

        int               nlats          = (int) (2 * np);
        GaussianLatitudes gaussLats      = new GaussianLatitudes(nlats);

        int               bestStartIndex = 0,
                          bestEndIndex   = 0;
        double            bestStartDiff  = Double.MAX_VALUE;
        double            bestEndDiff    = Double.MAX_VALUE;
        for (int i = 0; i < nlats; i++) {
            double diff = Math.abs(gaussLats.latd[i] - startLat);
            if (diff < bestStartDiff) {
                bestStartDiff  = diff;
                bestStartIndex = i;
            }
            diff = Math.abs(gaussLats.latd[i] - endLat);
            if (diff < bestEndDiff) {
                bestEndDiff  = diff;
                bestEndIndex = i;
            }
        }
        assert Math.abs(bestEndIndex - bestStartIndex + 1)
               == getParamValue(gdsIndex.NY);
        boolean  goesUp = bestEndIndex > bestStartIndex;

        Variable v      = new Variable(ncfile, g, null, name);
        v.setDataType(DataType.DOUBLE);
        v.setDimensions(name);

        // create the data
        int      n        = (int) getParamValue(gdsIndex.NY);
        int      useIndex = bestStartIndex;
        double[] data     = new double[n];
        double[] gaussw   = new double[n];
        for (int i = 0; i < n; i++) {
            data[i]   = gaussLats.latd[useIndex];
            gaussw[i] = gaussLats.gaussw[useIndex];
            if (goesUp) {
                useIndex++;
            } else {
                useIndex--;
            }
        }
        Array dataArray = Array.factory(DataType.DOUBLE.getClassType(),
                                        new int[] { n }, data);
        v.setCachedData(dataArray, false);

        v.addAttribute(new Attribute("units", units));
        v.addAttribute(new Attribute("long_name", desc));
        v.addAttribute(new Attribute("standard_name", standard_name));
        v.addAttribute(new Attribute("weights", "gaussw"));
        v.addAttribute(new Attribute(_Coordinate.AxisType, axis.toString()));
        ncfile.addVariable(g, v);

        v = new Variable(ncfile, g, null, "gaussw");
        v.setDataType(DataType.DOUBLE);
        v.setDimensions(name);
        v.addAttribute(new Attribute("long_name",
                                     "gaussian weights (unnormalized)"));
        dataArray = Array.factory(DataType.DOUBLE.getClassType(),
                                  new int[] { n }, gaussw);
        v.setCachedData(dataArray, false);
        ncfile.addVariable(g, v);

        return data;
    }


    /**
     * _more_
     *
     * @param ncfile _more_
     * @param xData _more_
     * @param yData _more_
     */
    private void addLatLon2D(NetcdfFile ncfile, double[] xData,
                             double[] yData) {

        Variable latVar = new Variable(ncfile, g, null, "lat");
        latVar.setDataType(DataType.DOUBLE);
        latVar.setDimensions("y x");
        latVar.addAttribute(new Attribute("units", "degrees_north"));
        latVar.addAttribute(new Attribute("long_name",
                                          "latitude coordinate"));
        latVar.addAttribute(new Attribute("standard_name", "latitude"));
        latVar.addAttribute(new Attribute(_Coordinate.AxisType,
                                          AxisType.Lat.toString()));

        Variable lonVar = new Variable(ncfile, g, null, "lon");
        lonVar.setDataType(DataType.DOUBLE);
        lonVar.setDimensions("y x");
        lonVar.addAttribute(new Attribute("units", "degrees_east"));
        lonVar.addAttribute(new Attribute("long_name",
                                          "longitude coordinate"));
        lonVar.addAttribute(new Attribute("standard_name", "longitude"));
        lonVar.addAttribute(new Attribute(_Coordinate.AxisType,
                                          AxisType.Lon.toString()));

        int nx = xData.length;
        int ny = yData.length;

        // create the data
        ProjectionPointImpl projPoint   = new ProjectionPointImpl();
        LatLonPointImpl     latlonPoint = new LatLonPointImpl();
        double[]            latData     = new double[nx * ny];
        double[]            lonData     = new double[nx * ny];
        for (int i = 0; i < ny; i++) {
            for (int j = 0; j < nx; j++) {
                projPoint.setLocation(xData[j], yData[i]);
                proj.projToLatLon(projPoint, latlonPoint);
                latData[i * nx + j] = latlonPoint.getLatitude();
                lonData[i * nx + j] = latlonPoint.getLongitude();
            }
        }
        Array latDataArray = Array.factory(DataType.DOUBLE.getClassType(),
                                           new int[] { ny,
                nx }, latData);
        latVar.setCachedData(latDataArray, false);

        Array lonDataArray = Array.factory(DataType.DOUBLE.getClassType(),
                                           new int[] { ny,
                nx }, lonData);
        lonVar.setCachedData(lonDataArray, false);

        ncfile.addVariable(g, latVar);
        ncfile.addVariable(g, lonVar);
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     */
    private void makeProjection(NetcdfFile ncfile) {
        switch (lookup.getProjectionType(gdsIndex)) {

          case GridTableLookup.PolarStereographic :
              makePS();
              break;

          case GridTableLookup.LambertConformal :
              makeLC();
              break;

          case GridTableLookup.Mercator :
              makeMercator();
              break;

          case GridTableLookup.Orthographic :
              makeSpaceViewOrOthographic();
              break;

          default :
              throw new UnsupportedOperationException("unknown projection = "
                      + getParamValue(gdsIndex.GRID_TYPE));
        }

        Variable v = new Variable(ncfile, g, null, grid_name);
        v.setDataType(DataType.CHAR);
        v.setDimensions(new ArrayList());  // scalar
        char[] data = new char[] { 'd' };
        Array dataArray = Array.factory(DataType.CHAR.getClassType(),
                                        new int[0], data);
        v.setCachedData(dataArray, false);

        for (int i = 0; i < attributes.size(); i++) {
            Attribute att = (Attribute) attributes.get(i);
            v.addAttribute(att);
        }

        v.addAttribute(new Attribute("earth_shape", shape_name));
        if (getParamValue(gdsIndex.GRID_SHAPE_CODE) == 1) {
            v.addAttribute(
                new Attribute(
                    "spherical_earth_radius_meters",
                    new Double(
                        getParamValue(gdsIndex.RADIUS_SPHERICAL_EARTH))));
        }

        addGDSparams(v);
        ncfile.addVariable(g, v);
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    private void addGDSparams(Variable v) {
        // add all the gds parameters
        java.util.Set keys    = gdsIndex.getKeys();
        ArrayList     keyList = new ArrayList(keys);
        Collections.sort(keyList);
        for (int i = 0; i < keyList.size(); i++) {
            String key = (String) keyList.get(i);
            String name =
                NetcdfFile.createValidNetcdfObjectName("GRIB_param_" + key);

            String vals = getParamString(key);
            try {
                int vali = Integer.parseInt(vals);
                v.addAttribute(new Attribute(name, new Integer(vali)));
            } catch (Exception e) {
                try {
                    double vald = Double.parseDouble(vals);
                    v.addAttribute(new Attribute(name, new Double(vald)));
                } catch (Exception e2) {
                    v.addAttribute(new Attribute(name, vals));
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     * @param name _more_
     * @param dims _more_
     */
    private void addCoordSystemVariable(NetcdfFile ncfile, String name,
                                        String dims) {
        Variable v = new Variable(ncfile, g, null, name);
        v.setDataType(DataType.CHAR);
        v.setDimensions(new ArrayList());  // scalar
        Array dataArray = Array.factory(DataType.CHAR.getClassType(),
                                        new int[0], new char[] { '0' });
        v.setCachedData(dataArray, false);
        v.addAttribute(new Attribute(_Coordinate.Axes, dims));
        if ( !isLatLon()) {
            v.addAttribute(new Attribute(_Coordinate.Transforms,
                                         getGridName()));
        }

        addGDSparams(v);
        ncfile.addVariable(g, v);
    }

    // lambert conformal

    /**
     * _more_
     */
    private void makeLC() {
        // we have to project in order to find the origin
        proj = new LambertConformal(getParamValue(gdsIndex.LATIN1),
                                    getParamValue(gdsIndex.LOV),
                                    getParamValue(gdsIndex.LATIN1),
                                    getParamValue(gdsIndex.LATIN2));
        LatLonPointImpl startLL =
            new LatLonPointImpl(getParamValue(gdsIndex.LA1),
                                getParamValue(gdsIndex.LO1));
        ProjectionPointImpl start =
            (ProjectionPointImpl) proj.latLonToProj(startLL);
        startx = start.getX();
        starty = start.getY();

        if (debug) {
            System.out.println("GridHorizCoordSys.makeLC start at latlon "
                               + startLL);

            double          Lo2   = gdsIndex.readDouble("Lo2");
            double          La2   = gdsIndex.readDouble("La2");
            LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
            System.out.println("GridHorizCoordSys.makeLC end at latlon "
                               + endLL);

            ProjectionPointImpl endPP =
                (ProjectionPointImpl) proj.latLonToProj(endLL);
            System.out.println("   end at proj coord " + endPP);

            double endx = startx + getNx() * getDxInKm();
            double endy = starty + getNy() * getDyInKm();
            System.out.println("   should be x=" + endx + " y=" + endy);
        }

        attributes.add(new Attribute("grid_mapping_name",
                                     "lambert_conformal_conic"));
        if (getParamValue(gdsIndex.LATIN1)
                == getParamValue(gdsIndex.LATIN2)) {
            attributes.add(
                new Attribute(
                    "standard_parallel",
                    new Double(getParamValue(gdsIndex.LATIN1))));
        } else {
            double[] data = new double[] { getParamValue(gdsIndex.LATIN1),
                                           getParamValue(gdsIndex.LATIN2) };
            attributes.add(
                new Attribute(
                    "standard_parallel",
                    Array.factory(
                        DataType.DOUBLE.getClassType(), new int[] { 2 },
                        data)));
        }
        attributes.add(
            new Attribute(
                "longitude_of_central_meridian",
                new Double(getParamValue(gdsIndex.LOV))));
        attributes.add(
            new Attribute(
                "latitude_of_projection_origin",
                new Double(getParamValue(gdsIndex.LATIN1))));
        //attributes.add( new Attribute("false_easting", new Double(startx)));
        //attributes.add( new Attribute("false_northing", new Double(starty)));
    }

    // polar stereographic

    /**
     * _more_
     */
    private void makePS() {
        double scale     = .933;

        double latOrigin = 90.0;
        String s         = getParamString("NpProj");
        if ((s != null) && !s.equalsIgnoreCase("true")) {
            latOrigin = -90.0;
        }

        // Why the scale factor?. accordining to GRIB docs:
        // "Grid lengths are in units of meters, at the 60 degree latitude circle nearest to the pole"
        // since the scale factor at 60 degrees = k = 2*k0/(1+sin(60))  [Snyder,Working Manual p157]
        // then to make scale = 1 at 60 degrees, k0 = (1+sin(60))/2 = .933
        proj = new Stereographic(latOrigin, getParamValue(gdsIndex.LOV),
                                 scale);

        // we have to project in order to find the origin
        ProjectionPointImpl start = (ProjectionPointImpl) proj.latLonToProj(
                                        new LatLonPointImpl(
                                            getParamValue(gdsIndex.LA1),
                                            getParamValue(gdsIndex.LO1)));
        startx = start.getX();
        starty = start.getY();

        if (debug) {
            System.out.println("start at proj coord " + start);
            LatLonPoint llpt = proj.projToLatLon(start);
            System.out.println("   end at lat/lon coord " + llpt);
            System.out.println("   should be lat="
                               + getParamValue(gdsIndex.LA1) + " lon="
                               + getParamValue(gdsIndex.LO1));
        }

        attributes.add(new Attribute("grid_mapping_name",
                                     "polar_stereographic"));
        attributes.add(
            new Attribute(
                "longitude_of_projection_origin",
                new Double(getParamValue(gdsIndex.LOV))));
        attributes.add(new Attribute("scale_factor_at_projection_origin",
                                     new Double(scale)));
        attributes.add(new Attribute("latitude_of_projection_origin",
                                     new Double(latOrigin)));
    }

    // Mercator

    /**
     * _more_
     */
    private void makeMercator() {

        /**
         * Construct a Mercator Projection.
         * @param lat0 latitude of origin (degrees)
         * @param lon0 longitude of origin (degrees)
         * @param par standard parallel (degrees). cylinder cuts earth at this latitude.
         */
        double Latin = gdsIndex.readDouble("Latin") * .001;
        double La1   = getParamValue(gdsIndex.LA1);
        double Lo1   = getParamValue(gdsIndex.LO1);

        // put projection origin at La1, Lo1
        proj   = new Mercator(La1, Lo1, Latin);
        startx = 0;
        starty = 0;

        attributes.add(new Attribute("grid_mapping_name", "mercator"));
        attributes.add(new Attribute("standard_parallel", new Double(Latin)));
        attributes.add(new Attribute("longitude_of_projection_origin",
                                     new Double(Lo1)));
        attributes.add(new Attribute("latitude_of_projection_origin",
                                     new Double(La1)));

        if (debug) {
            double          Lo2   = getParamValue("Lo2") + 360.0;
            double          La2   = getParamValue("La2");
            LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
            System.out.println(
                "GridHorizCoordSys.makeMercator end at latlon " + endLL);

            ProjectionPointImpl endPP =
                (ProjectionPointImpl) proj.latLonToProj(endLL);
            System.out.println("   end at proj coord " + endPP);

            double endx = startx + getNx() * getDxInKm();
            double endy = starty + getNy() * getDyInKm();
            System.out.println("   should be x=" + endx + " y=" + endy);
        }
    }

    /**
     * _more_
     */
    private void makeSpaceViewOrOthographic() {
        double Lat0 = getParamValue("Lap");  // sub-satellite point lat
        double Lon0 = getParamValue("Lop");  // sub-satellite point lon

        double xp = getParamValue("Xp");  // sub-satellite point in grid lengths
        double yp   = getParamValue("Yp");

        double dx = getParamValue("Dx");  // apparent diameter in units of grid lengths
        double dy         = getParamValue("Dy");

        double major_axis = getParamValue("major_axis_earth");  // km
        double minor_axis = getParamValue("minor_axis_earth");  // km

        // Nr = altitude of camera from center, in units of radius
        double nr = getParamValue("Nr") * 1e-6;
        double apparentDiameter = 2 * Math.sqrt((nr - 1) / (nr + 1));  // apparent diameter, units of radius (see Snyder p 173)

        // app diameter kmeters / app diameter grid lengths = m per grid length
        double gridLengthX = major_axis * apparentDiameter / dx;
        double gridLengthY = minor_axis * apparentDiameter / dy;

        gdsIndex.addParam(gdsIndex.DX, String.valueOf(1000 * gridLengthX));  // meters
        gdsIndex.addParam(gdsIndex.DY, String.valueOf(1000 * gridLengthY));  // meters

        startx = -gridLengthX * xp;  // km
        starty = -gridLengthY * yp;

        double radius = Earth.getRadius() / 1000.0;  // km

        if (nr == 1111111111.0) {  // LOOK: not sure how all ones will appear as a double, need example
            proj = new Orthographic(Lat0, Lon0, radius);

            attributes.add(new Attribute("grid_mapping_name",
                                         "orthographic"));
            attributes.add(new Attribute("longitude_of_projection_origin",
                                         new Double(Lon0)));
            attributes.add(new Attribute("latitude_of_projection_origin",
                                         new Double(Lat0)));

        } else {  // "space view perspective"

            double height = (nr - 1.0) * radius;  // height = the height of the observing camera in km
            proj = new VerticalPerspectiveView(Lat0, Lon0, radius, height);

            attributes.add(new Attribute("grid_mapping_name",
                                         "vertical_perspective"));
            attributes.add(new Attribute("longitude_of_projection_origin",
                                         new Double(Lon0)));
            attributes.add(new Attribute("latitude_of_projection_origin",
                                         new Double(Lat0)));
            attributes.add(new Attribute("height_above_earth",
                                         new Double(height)));
        }

        if (debug) {

            double          Lo2   = getParamValue("Lo2") + 360.0;
            double          La2   = getParamValue("La2");
            LatLonPointImpl endLL = new LatLonPointImpl(La2, Lo2);
            System.out.println(
                "GridHorizCoordSys.makeOrthographic end at latlon " + endLL);

            ProjectionPointImpl endPP =
                (ProjectionPointImpl) proj.latLonToProj(endLL);
            System.out.println("   end at proj coord " + endPP);

            double endx = startx + getNx() * getDxInKm();
            double endy = starty + getNy() * getDyInKm();
            System.out.println("   should be x=" + endx + " y=" + endy);
        }
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private double getParamValue(String id) {
        return gdsIndex.readDouble(id);
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private String getParamString(String id) {
        return gdsIndex.getParam(id);
    }

}

