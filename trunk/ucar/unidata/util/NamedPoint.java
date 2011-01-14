/*
 * $Id: NamedPoint.java,v 1.7 2006/05/05 19:19:36 jeffmc Exp $
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



package ucar.unidata.util;



import java.*;

import java.util.Hashtable;



/**
 * A class to hold and transfer named locations' information, for a
 * landmark, city, observation point or station, data point, etc.
 * S Wier
 */

public class NamedPoint {

    /** _more_ */
    private double latitude, longitude, elevation;

    /** _more_ */
    private String id;

    /** _more_ */
    private String name;

    /** _more_ */
    private Hashtable hashtable = new Hashtable();

    /**
     * Construct an object to hold and transfer settings for a
     * landmark, city, observation point, etc.
     * @param latitude in decimal geographic degrees
     * @param longitude in decimal geographic degrees
     * @param elevation in meters above the geoid ("sea level")
     * @param id typically a 3- or 4- letter or number code
     * @param name some kind of name like "buoy 32" or "Kansas City"
     */
    public NamedPoint(String id, String name, double latitude,
                      double longitude, double elevation) {

        this.latitude  = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.id        = id;
        this.name      = name;

    }

    /**
     * _more_
     *
     */
    public NamedPoint() {}

    /**
     * _more_
     * @return _more_
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * _more_
     *
     * @param v
     */
    public void setLatitude(double v) {
        latitude = v;
    }

    /**
     * _more_
     * @return _more_
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * _more_
     *
     * @param v
     */
    public void setLongitude(double v) {
        longitude = v;
    }

    /**
     * _more_
     * @return _more_
     */
    public double getElevation() {
        return elevation;
    }

    /**
     * _more_
     *
     * @param v
     */
    public void setElevation(double v) {
        elevation = v;
    }

    /**
     * _more_
     * @return _more_
     */
    public String getID() {
        return id;
    }

    /**
     * _more_
     *
     * @param v
     */
    public void setID(String v) {
        id = v;
    }

    /**
     * _more_
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     *
     * @param v
     */
    public void setName(String v) {
        name = v;
    }

    /**
     * _more_
     * @return _more_
     */
    public Hashtable getProperties() {
        return hashtable;
    }

    /**
     * _more_
     *
     * @param v
     */
    public void setProperties(Hashtable v) {
        hashtable = v;
    }

    /**
     * _more_
     *
     * @param key
     * @param value
     */
    public void addProperty(String key, String value) {
        hashtable.put(key, value);
    }

    /*
    public float[] asArray () {
        return  new float[] {interval, base, min, max};
    }
    */

    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return name;
    }

    /**
     * _more_
     *
     * @param o
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof NamedPoint)) {
            return false;
        }
        NamedPoint that = (NamedPoint) o;
        return (id.equals(that.id) && name.equals(that.name)
                && (latitude == that.latitude)
                && (longitude == that.longitude)
                && (elevation == that.elevation));
    }


}

