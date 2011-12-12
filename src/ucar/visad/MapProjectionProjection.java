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

package ucar.visad;


import ucar.unidata.geoloc.*;

import visad.VisADException;

import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;


/**
 *   Projection wrapper for a VisAD MapProjection with reference of
 *   Lat,Lon or Lon,Lat
 *
 *   @see Projection
 *   @see ProjectionImpl
 *   @author Unidata Development Team
 */

public class MapProjectionProjection extends ProjectionImpl {

    /** x index */
    private int xIndex = 0;

    /** y index */
    private int yIndex = 1;

    /** the map projection */
    private MapProjection mapProjection;

    /**
     * copy constructor - avoid clone !!
     *
     * @return _more_
     */
    public ProjectionImpl constructCopy() {
        return new MapProjectionProjection(getMapProjection());
    }

    /**
     *  Constructor with default parameters
     */
    public MapProjectionProjection() {
        this(null);
    }


    /**
     * Construct a MapProjection Projection.
     * @param mapProjection   the VisAD MapProjection to wrap
     */
    public MapProjectionProjection(MapProjection mapProjection) {
        if (mapProjection == null) {
            try {
                mapProjection = new TrivialMapProjection();
            } catch (VisADException ve) {}
        }
        this.mapProjection = mapProjection;
        xIndex             = mapProjection.isXYOrder()
                             ? 0
                             : 1;
        yIndex             = mapProjection.isXYOrder()
                             ? 1
                             : 0;

        addParameter(ATTR_NAME, "visad_mapprojection");
        // TODO: What else should go here?
    }

    /**
     * Clone this projection.
     *
     * @return Clone of this
     */
    public Object clone() {
        MapProjectionProjection cl = (MapProjectionProjection) super.clone();
        return cl;
    }


    /**
     * Check for equality with the Object in question
     *
     * @param proj  object to check
     * @return true if they are equal
     */
    public boolean equals(Object proj) {
        if ( !(proj instanceof MapProjectionProjection)) {
            return false;
        }

        MapProjectionProjection oo = (MapProjectionProjection) proj;
        return true;
    }


    /**
     * Get the label to be used in the gui for this type of projection
     *
     * @return Type label
     */
    public String getProjectionTypeLabel() {
        return "MapProjectionProjection";
    }

    /**
     * Create a String of the parameters.
     * @return a String of the parameters
     */
    public String paramsToString() {
        return mapProjection.toString();
    }



    /**
     * Convert a LatLonPoint to projection coordinates
     *
     * @param latLon convert from these lat, lon coordinates
     * @param result the object to write to
     *
     * @return the given result
     */
    public ProjectionPoint latLonToProj(LatLonPoint latLon,
                                        ProjectionPointImpl result) {
        double     fromLat = latLon.getLatitude();
        double     fromLon = latLon.getLongitude();
        double[][] point   = new double[2][1];
        point[0][0] = fromLat;
        point[1][0] = fromLon;
        point       = latLonToProj(point, 0, 1);
        result.setLocation(point[xIndex][0], point[yIndex][0]);
        return result;
    }

    /**
     * Convert projection coordinates to a LatLonPoint
     *   Note: a new object is not created on each call for the return value.
     *
     * @param world convert from these projection coordinates
     * @param result the object to write to
     *
     * @return LatLonPoint convert to these lat/lon coordinates
     */

    public LatLonPoint projToLatLon(ProjectionPoint world,
                                    LatLonPointImpl result) {
        double     x     = world.getX();
        double     y     = world.getY();
        double[][] point = new double[2][1];
        point[xIndex][0] = x;
        point[yIndex][0] = y;
        point            = projToLatLon(point);
        result.setLatitude(point[INDEX_LAT][0]);
        result.setLongitude(point[INDEX_LON][0]);
        return result;
    }



    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates,
     *                 where to[0][i], to[1][i] is the (x,y) coordinate
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */

    public float[][] latLonToProj(float[][] from, float[][] to, int latIndex,
                                  int lonIndex) {
        float[][] temp = new float[2][];
        temp[mapProjection.getLatitudeIndex()]  = from[latIndex];
        temp[mapProjection.getLongitudeIndex()] = from[lonIndex];
        try {
            temp        = mapProjection.fromReference(temp);
            to[INDEX_X] = temp[xIndex];
            to[INDEX_Y] = temp[yIndex];
        } catch (VisADException ve) {}
        return to;
    }

    /**
     *  This returns true when the line between pt1 and pt2 crosses the seam.
     *  When the cone is flattened, the "seam" is lon0 +- 180.
     *
     * @param pt1   point 1
     * @param pt2   point 2
     * @return true when the line between pt1 and pt2 crosses the seam.
     */
    public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {

        // either point is infinite
        if (ProjectionPointImpl.isInfinite(pt1)
                || ProjectionPointImpl.isInfinite(pt2)) {
            return true;
        }
        return false;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */

    public float[][] projToLatLon(float[][] from, float[][] to) {
        float[][] temp = new float[2][];
        temp[xIndex] = from[INDEX_X];
        temp[yIndex] = from[INDEX_Y];
        try {
            temp          = mapProjection.toReference(temp);
            to[INDEX_LAT] = temp[mapProjection.getLatitudeIndex()];
            to[INDEX_LON] = temp[mapProjection.getLongitudeIndex()];
        } catch (VisADException ve) {}
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n],
     *                 where from[0][i], from[1][i] is the (lat,lon)
     *                 coordinate of the ith point
     * @param to       resulting array of projection coordinates,
     *                 where to[0][i], to[1][i] is the (x,y) coordinate
     *                 of the ith point
     * @param latIndex index of latitude in "from"
     * @param lonIndex index of longitude in "from"
     *
     * @return the "to" array.
     */

    public double[][] latLonToProj(double[][] from, double[][] to,
                                   int latIndex, int lonIndex) {
        double[][] temp = new double[2][];
        temp[mapProjection.getLatitudeIndex()]  = from[latIndex];
        temp[mapProjection.getLongitudeIndex()] = from[lonIndex];
        try {
            temp        = mapProjection.fromReference(temp);
            to[INDEX_X] = temp[xIndex];
            to[INDEX_Y] = temp[yIndex];
        } catch (VisADException ve) {}
        return to;
    }

    /**
     * Convert lat/lon coordinates to projection coordinates.
     *
     * @param from     array of lat/lon coordinates: from[2][n], where
     *                 (from[0][i], from[1][i]) is the (lat,lon) coordinate
     *                 of the ith point
     * @param to       resulting array of projection coordinates: to[2][n]
     *                 where (to[0][i], to[1][i]) is the (x,y) coordinate
     *                 of the ith point
     * @return the "to" array
     */

    public double[][] projToLatLon(double[][] from, double[][] to) {
        double[][] temp = new double[2][];
        temp[xIndex] = from[INDEX_X];
        temp[yIndex] = from[INDEX_Y];
        try {
            temp          = mapProjection.toReference(temp);
            to[INDEX_LAT] = temp[mapProjection.getLatitudeIndex()];
            to[INDEX_LON] = temp[mapProjection.getLongitudeIndex()];
        } catch (VisADException ve) {}
        return to;
    }

    /**
     * Get the underlying map projection
     * @return the MapProjection
     */
    public MapProjection getMapProjection() {
        return mapProjection;
    }

    /**
     * Test
     *
     * @param args not used
     */
    public static void main(String[] args) {
        MapProjectionProjection a = new MapProjectionProjection();
        ProjectionPointImpl     p = a.latLonToProj(89, -101);
        System.out.println("proj point = " + p);
        LatLonPoint ll = a.projToLatLon(p);
        System.out.println("ll = " + ll);
    }

}
