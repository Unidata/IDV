/*
 * $Id: GeoLocationInfo.java,v 1.17 2006/12/01 20:41:23 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Found2ation; either version 2.1 of the License, or (at
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

package ucar.unidata.data;


import ucar.unidata.geoloc.*;
import java.awt.geom.Rectangle2D;



/**
 * Holds geo-location information  - lat/lon bounding box, image size, etc.
 * This is used to pass information from a chooser into a datasource.
 */
public class GeoLocationInfo {


    /**
     *  The maxLat property.
     */
    private double maxLat;

    /**
     *  The minLon property.
     */
    private double minLon;


    /**
     *  The minLat property.
     */
    private double minLat;


    /**
     *  The maxLon property.
     */
    private double maxLon;


    /**
     *  Parameterless constructor for xml encoding.
     */
    public GeoLocationInfo() {}


    /**
     * copy ctor
     *
     * @param that copy from
     */
    public GeoLocationInfo(GeoLocationInfo that) {
        this.maxLat = that.maxLat;
        this.minLat = that.minLat;
        this.minLon = that.minLon;
        this.maxLon = that.maxLon;
    }



    public GeoLocationInfo(Rectangle2D.Float rect) { 
	this(rect.getY(), rect.getX(), rect.getY()-rect.getHeight(),
	     rect.getX()+rect.getWidth());
    }


    /**
     *  Create a new GeoLocation
     *
     *  @param maxLat       The latitude of the upper left
     *  @param minLon       The longitude of the upper left
     *  @param minLat       The latitude of the lower
     *  @param maxLon       The longitude of the lower right
     */
    public GeoLocationInfo(double maxLat, double minLon, double minLat,
                           double maxLon) {
        this.maxLat = Math.max(maxLat, minLat);
        this.minLat = Math.min(maxLat, minLat);
        this.minLon = Math.min(minLon, maxLon);
        this.maxLon = Math.max(minLon, maxLon);
    }

    /**
     * ctor
     *
     * @param llr The latlonrect to create me from
     */
    public GeoLocationInfo(LatLonRect llr) {
        LatLonPointImpl ul = llr.getUpperLeftPoint();
        LatLonPointImpl lr = llr.getLowerRightPoint();
        maxLat = ul.getLatitude();
        minLat = lr.getLatitude();
        maxLon = lr.getLongitude();
        minLon = ul.getLongitude();
    }


    /**
     * Union with the other geolocationinfo
     *
     * @param that object to union with
     *
     * @return The union.
     */
    public GeoLocationInfo union(GeoLocationInfo that) {
        return new GeoLocationInfo(Math.max(this.maxLat, that.maxLat),
                                   Math.min(this.minLon, that.minLon),
                                   Math.min(this.minLat, that.minLat),
                                   Math.max(this.maxLon, that.maxLon));

    }

    /**
     * Create a LatLonRect from me
     *
     * @return The latlonrect
     */
    public LatLonRect getLatLonRect() {
        return new LatLonRect(getUpperLeft(), getLowerRight());
    }


    /**
     * Get the upper left point
     *
     * @return Upper left point
     */
    public LatLonPoint getUpperLeft() {
        return new LatLonPointImpl(maxLat, minLon);
    }

    /**
     * Get lower right point
     *
     * @return lower right point
     */
    public LatLonPoint getLowerRight() {
        return new LatLonPointImpl(minLat, maxLon);
    }


    /**
     * set  the value in the range
     *
     * @param v value
     * @param min min
     * @param max max
     *
     * @return value
     */
    private double inRange(double v, double min, double max) {
        return Math.min(Math.max(v, min), max);
    }

    /**
     * Make sure min/max lat and lon are in the right order
     */
    private void ensureOrder() {
        if (minLat > maxLat) {
            double tmp = minLat;
            minLat = maxLat;
            maxLat = tmp;
        }

        if (minLon > maxLon) {
            double tmp = minLon;
            minLon = maxLon;
            maxLon = tmp;
        }
    }

    /**
     * Snap corners to grid
     */
    public void snapToGrid() {
        double diffx   = getDegreesX();
        double diffy   = getDegreesY();
        double factorX = 0.0;
        double factorY = 0.0;
        if (diffx > 10.0) {
            factorX = 1.0;
        } else if (diffx > 1.0) {
            factorX = 10.0;
        } else if (diffx > 0.1) {
            factorX = 100.0;
        } else if (diffx > 0.01) {
            factorX = 1000.0;
        }

        if (diffy > 10.0) {
            factorY = 1.0;
        } else if (diffy > 1.0) {
            factorY = 10.0;
        } else if (diffy > 0.1) {
            factorY = 100.0;
        } else if (diffy > 0.01) {
            factorY = 1000.0;
        }
        if (factorX != 0.0) {
            minLon = Math.floor(factorX * minLon) / factorX;
            maxLon = Math.ceil(factorX * maxLon) / factorX;
        }
        if (factorY != 0.0) {
            minLat = Math.floor(factorY * minLat) / factorY;
            maxLat = Math.ceil(factorY * maxLat) / factorY;
        }
    }


    /**
     * Make sure wew are within the that with the given resolution.
     *
     * @param that bounds to be within
     * @param minResolution min size
     */
    public void rectify(GeoLocationInfo that, double minResolution) {
        minLon = inRange(minLon, that.minLon, that.maxLon);
        maxLon = inRange(maxLon, that.minLon, that.maxLon);
        minLat = inRange(minLat, that.minLat, that.maxLat);
        maxLat = inRange(maxLat, that.minLat, that.maxLat);
        ensureOrder();
        if ((maxLat - minLat) < minResolution) {
            maxLat = minLat + minResolution;
            maxLon = minLon + minResolution;
        }
        if ((maxLon - minLon) < minResolution) {
            maxLon = minLon + minResolution;
            maxLat = minLat + minResolution;
        }
    }



    /**
     * Get the number of degrees from one side to the other in the X direction
     *
     * @return  number of degrees
     */
    public double getDegreesX() {
        return maxLon - minLon;
    }

    /**
     * Get the number of degrees from one side to the other in the Y direction
     *
     * @return   number of degrees
     */
    public double getDegreesY() {
        return maxLat - minLat;
    }


    /**
     * Set the MaxLat property.
     *
     * @param value The new value for MaxLat
     */
    public void setMaxLat(double value) {
        maxLat = value;
    }

    /**
     * Get the MaxLat property.
     *
     * @return The MaxLat
     */
    public double getMaxLat() {
        return maxLat;
    }

    /**
     * Set the MinLon property.
     *
     * @param value The new value for MinLon
     */
    public void setMinLon(double value) {
        minLon = value;
    }

    /**
     * Get the MinLon property.
     *
     * @return The MinLon
     */
    public double getMinLon() {
        return minLon;
    }

    /**
     * Set the MinLat property.
     *
     * @param value The new value for MinLat
     */
    public void setMinLat(double value) {
        minLat = value;
    }

    /**
     * Get the MinLat property.
     *
     * @return The MinLat
     */
    public double getMinLat() {
        return minLat;
    }

    /**
     * Set the MaxLon property.
     *
     * @param value The new value for MaxLon
     */
    public void setMaxLon(double value) {
        maxLon = value;
    }

    /**
     * Get the MaxLon property.
     *
     * @return The MaxLon
     */
    public double getMaxLon() {
        return maxLon;
    }



    /**
     * Return a String representation of this
     *
     * @return    string representation
     */
    public String toString() {
        return " lat: (" + maxLat + " - " + minLat + ") lon: (" + minLon
               + " - " + maxLon + ")";
    }



    /**
     * equals
     *
     * @param obj obj
     *
     * @return is equals
     */
    public boolean equals(Object obj) {
        if ( !(obj instanceof GeoLocationInfo)) {
            return false;
        }
        GeoLocationInfo that = (GeoLocationInfo) obj;
        return (maxLat == that.maxLat) && (minLat == that.minLat)
               && (maxLon == that.maxLon) && (minLon == that.minLon);
    }



}

