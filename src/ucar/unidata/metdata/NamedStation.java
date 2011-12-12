/*
 * $Id: NamedStation.java,v 1.5 2005/05/13 18:31:30 jeffmc Exp $
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



import visad.Real;
import visad.georef.NamedLocation;

import java.*;



/**
 * Interface for a landmark with name, id, lat, lon, altitude
 * (height above msl); and a hash table of "properties".
 * ID is usually a letter or number code like "KICT,"
 * and is the "id" part of the NamedLocation.
 * Name is a name or description like "Wichita."
 */

public interface NamedStation extends ucar.unidata.metdata.Station {

    /**
     * Get the VisAD NamedLocation that can be used to represent
     * this NamedStation.
     * @return  <code>NamedLocation</code> object.
     */
    public NamedLocation getNamedLocation();

    /**
     * Set the VisAD NamedLocation that can be used to represent
     * this NamedStation.
     * @param nl  <code>NamedLocation</code> to use.
     */
    public void setNamedLocation(NamedLocation nl);

    /**
     * Get the latitude of this station in degrees.
     * @return  latitude in degrees north.
     */
    public double getLatitude();

    /**
     * Set the latitude of this station in degrees.
     * @param  lat  latitude in degrees north.
     */
    public void setLatitude(double lat);

    /**
     * Get the longitude of this station in degrees.
     * @return  longitude in degrees east.
     */
    public double getLongitude();

    /**
     * Set the longitude of this station in degrees.
     * @param lon   longitude in degrees east.
     */
    public void setLongitude(double lon);

    /**
     * Get the altitude as a Real. Units are not necessarily meters.
     * @return altitude as a <code>Real</code>
     */
    public Real getAltitude();

    /**
     * Set the altitude as a Real. Units are not necessarily meters.
     * @param alt  altitude as a <code>Real</code>
     */
    public void setAltitude(Real alt);

    /**
     * Get the id of this station.  Something like "KICT" or "72518".
     * @return  name of the station
     */
    public String getID();

    /**
     * Set the id of this station.  Something like "KICT" or "72518".
     * @param id  name of the station
     */
    public void setID(String id);

    /**
     * Get the name of this station.  Usually a human readable string.
     * @return  name of the station
     */
    public String getName();

    /**
     * Set the name of this station.  Usually a human readable string.
     * @param name   name of the station
     */
    public void setName(String name);

    /**
     * See if this object is equal to the object in question.
     * @param  o  <code>Object</code> in question
     * @return true if they are "equal"
     */
    public boolean equals(Object o);
}
