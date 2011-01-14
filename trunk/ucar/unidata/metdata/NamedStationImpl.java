/*
 * $Id: NamedStationImpl.java,v 1.26 2007/06/08 21:28:20 dmurray Exp $
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


package ucar.unidata.metdata;



import ucar.unidata.metdata.NamedStation;

import visad.CommonUnit;
import visad.Real;
import visad.RealType;
import visad.Unit;

import visad.VisADException;



import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.NamedLocation;
import visad.georef.NamedLocationTuple;

import java.*;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;



/**
 * A class to hold named location information, for a
 * landmark, city, observation point or station, data point, etc.
 * Name should be unique.
 */

public class NamedStationImpl implements NamedStation {

    /** _more_          */
    private List coords;


    /** The Earth Location */
    private EarthLocation earthLocation;

    /** latitude, longitude values (degrees) */
    private double latitude, longitude;

    /** altitude */
    private Real altitude;

    /** altitude value */
    private double altValue;

    /** altitude unit */
    private Unit altUnit;

    /** station id */
    private String id;

    /** station name */
    private String name;

    /** hashtable */
    private Hashtable hashtable = new Hashtable();

    /** named location */
    private NamedLocation nLT;




    /**
     * Construct an object to hold and transfer settings for a
     * landmark, city, observation point, etc.
     *
     * @param id     typically a letter or number code
     * @param name   some kind of name such as "Kansas City" - should be UNIQUE.
     * @param latitude   latitude in decimal geographic degrees
     * @param longitude  longitude in decimal geographic degrees
     * @param alt        VisAD Real for height above the geoid ("sea level")
     * @param unit       VisAD Unit of the altitude
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public NamedStationImpl(String id, String name, double latitude,
                            double longitude, double alt,
                            Unit unit) throws VisADException,
                                RemoteException {
        this.id        = id;
        this.name      = name;
        this.latitude  = latitude;
        this.longitude = longitude;
        altValue       = alt;
        altUnit        = unit;
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     * @param coords _more_
     * @param unit _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public NamedStationImpl(String id, String name, List coords,
                            Unit unit) throws VisADException,
                                RemoteException {
        this.id     = id;
        this.name   = name;
        this.coords = coords;
        double[][] tmp = (double[][]) coords.get(0);
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double altSum = 0.0;
        for(int i=0;i<tmp[0].length;i++) {
            maxLat = Math.max(maxLat,tmp[1][i]);
            minLat = Math.min(minLat,tmp[1][i]);
            maxLon = Math.max(maxLon,tmp[0][i]);
            minLon = Math.min(minLon,tmp[0][i]);
            if (tmp.length > 2) {
                altSum+=tmp[2][i];
            }
        }

        this.latitude  = minLat+(maxLat-minLat)/2.0;
        this.longitude  = minLon+(maxLon-minLon)/2.0;
        if (tmp.length > 2) {
            this.altValue = altSum/tmp[0].length;
        }
        altUnit = unit;
    }



    /**
     * Create a NamedStationImpl from the name and NamedLocation
     *
     * @param name    station name
     * @param in_nLT  NamedLocation
     *
     */
    public NamedStationImpl(String name, NamedLocationTuple in_nLT) {
        this.id        = (in_nLT.getIdentifier()).getValue();
        this.name      = name;
        this.latitude  = (in_nLT.getLatitude()).getValue();
        this.longitude = (in_nLT.getLongitude()).getValue();
        this.altitude  = in_nLT.getAltitude();
        this.nLT       = in_nLT;
    }



    /**
     *  A no-op ctor for unpersiting this
     */
    public NamedStationImpl() {}


    /**
     * Have this method here for backward compatibility with bundles.
     *
     * @deprecated
     *
     * @param v   new NamedLocationTuple
     */
    public void setNamedLocationTuple(NamedLocationTuple v) {
        nLT = v;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List getCoords() {
        return coords;
    }


    /**
     * Create, if needed, and return the location of this station
     *
     * @return location
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public EarthLocation getEarthLocation() throws VisADException,
            RemoteException {
        if (earthLocation == null) {
            earthLocation =
                new EarthLocationTuple(new Real(RealType.Latitude, latitude),
                                       new Real(RealType.Longitude,
                                           longitude), getAltitude());
        }
        return earthLocation;
    }


    // next 13 methods needed to implement ucar.unidata.metdata.NamedStation

    /**
     * Get the {@link NamedLocation} for this.  Used mostly by XML encoder.
     *
     * @return  NamedLocation
     */
    public NamedLocation getNamedLocation() {
        //If we don't have one then just create and return one.
        //We do this so the NamedLocationTuple does not get saved in memory
        if (nLT == null) {
            try {
                return new NamedLocationTuple(id, latitude, longitude,
                        getAltitude().getValue(CommonUnit.meter));
            } catch (Exception exc) {}
        }
        return nLT;
    }

    /**
     * Set the {@link NamedLocation} for this.  Used mostly by XML encoder.
     *
     * @param v  the NamedLocation
     */
    public void setNamedLocation(NamedLocation v) {
        nLT = v;
    }


    /**
     * Get the latitude.  Used mostly by XML encoding.
     *
     * @return  latitude (degrees)
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Set the latitude.  Used mostly by XML encoding.
     *
     * @param v  latitude (degrees)
     */
    public void setLatitude(double v) {
        latitude = v;
    }

    /**
     * Get the longitude.  Used mostly by XML encoding.
     *
     * @return  longitude (degrees)
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Set the longitude.  Used mostly by XML encoding.
     *
     * @param v   longitude (degrees)
     */
    public void setLongitude(double v) {
        longitude = v;
    }

    /**
     * Get the altitude as a real (value and unit)
     *
     * @return  altitude
     */
    public Real getAltitude() {
        if (altitude == null) {
            try {
                altitude = new Real(RealType.Altitude, altValue, altUnit);
            } catch (Exception exc) {}
        }
        return altitude;
    }

    /**
     * Set the alitude.  Used mostly by XML encoder.
     *
     * @param v   altitude
     */
    public void setAltitude(Real v) {
        altitude = v;
    }

    /**
     * Get the station ID.  Used mostly by XML encoder.
     *
     * @return  the station ID
     */
    public String getID() {
        return id;
    }

    /**
     * Set the station ID.  Used mostly by XML encoder.
     *
     * @param v   the station ID
     */
    public void setID(String v) {
        id = v;
    }

    /**
     * Get the station name.  Used mostly by XML encoder.
     *
     * @return  the station name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the station name.  Used mostly by XML encoder.
     *
     * @param v  the station name
     */
    public void setName(String v) {
        name = v;
    }

    /**
     * See if the Object in question is equal to this.
     *
     * @param o   Object in question
     *
     * @return  true if they are equal
     */
    public boolean equals(Object o) {
        if ( !(o instanceof NamedStation)) {
            return false;
        }
        NamedStation that = (NamedStation) o;
        return (id.equals(that.getID()) //&& name.equals(that.getName())
                && (latitude == that.getLatitude())
                && (longitude == that.getLongitude())
                && (getAltitude().equals(that.getAltitude())));
    }

    // next 1 method is needed to implement Station

    /**
     * Get the identifier for this NamedLocation.  It can either be
     * the station ID, or the name if ID is null
     *
     * @return  identifier for this station
     */
    public String getIdentifier() {
        if ((id == null) || (id.length() == 0)) {
            return name;
        }
        return id;
    }


    /**
     * Return altitude in units of getAltitudeUnit().
     *
     * @return  altitude value
     */
    public double getAltitudeAsDouble() {
        return getAltitude().getValue();
    }

    /**
     * Return {@link visad.Unit} of altitude
     *
     * @return  altitude Unit
     */
    public Unit getAltitudeUnit() {
        return getAltitude().getUnit();
    }

    /**
     * Get the hashtable of extra properties
     *
     * @return  extra properties
     */
    public Hashtable getProperties() {
        return hashtable;
    }

    /**
     * Set the table of extra properties
     *
     * @param v  extra properties
     */
    public void setProperties(Hashtable v) {
        hashtable = v;
    }

    /**
     * Add an extra property
     *
     * @param key     property name
     * @param value   property value
     */
    public void addProperty(String key, Object value) {
        if (hashtable == null) {
            hashtable = new Hashtable();
        }
        hashtable.put(key, value);
    }

    /**
     * Get an extra property
     *
     * @param key     property name
     * @param deflt   default value if property doesn't exist
     */
    public Object getProperty(String key, Object deflt) {
        if (hashtable == null) {
            return deflt;
        }
        Object retVal = hashtable.get(key);
        return retVal == null ? deflt : retVal;
    }

    /**
     * Return a String representation of this NamedLocation
     *
     * @return   a String representation
     */
    public String toString() {
        return name;
    }


    /**
     * Compare this NamedStationImpl to another NamedStationImpl.
     * Comparison is on the unique identifying name.
     *
     * @param  stn  other NamedStationImpl to compare
     *
     * @return  comparative value
     * @see Comparable
     */
    public int compareTo(NamedStationImpl stn) {
        return name.compareTo(stn.getName());
    }

    /**
     * Compare this NamedStationImpl to another object
     *
     * @param  oo  other object to compare
     *
     * @return  comparative value
     */
    public int compareTo(java.lang.Object oo) {
        return compareTo((NamedStationImpl) oo);
    }

}

