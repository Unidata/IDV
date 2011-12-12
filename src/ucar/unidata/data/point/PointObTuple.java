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

package ucar.unidata.data.point;


import visad.Data;
import visad.DateTime;
import visad.Tuple;
import visad.TupleType;
import visad.VisADError;
import visad.VisADException;

import visad.georef.EarthLocation;

import java.rmi.RemoteException;


/**
 * Implementation of PointOb as a Tuple.  This is immutable.
 *
 * @author IDV Development Team
 */
public class PointObTuple extends Tuple implements PointOb {

    /** location of ob */
    private EarthLocation location;

    /** time of ob */
    private DateTime dateTime;

    /** ob data */
    private Data data;

    /** The components */
    private Data[] components;


    /**
     * Construct a new PointObTuple from the given location, date/time and data.
     *
     * @param location  location of the observation
     * @param dateTime  date/time of the observation
     * @param data      associated data.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public PointObTuple(EarthLocation location, DateTime dateTime, Data data)
            throws VisADException, RemoteException {
        super(new Data[] { location, dateTime, data }, false);
        this.location = location;
        this.dateTime = dateTime;
        this.data     = data;
    }


    /**
     * Get the i'th component
     *
     * @param i component index
     *
     * @return The component
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data getComponent(int i) throws VisADException, RemoteException {
        if (i == 0) {
            return location;
        }
        if (i == 1) {
            return dateTime;
        }
        return data;
    }

    /**
     * Is this missing?
     *
     * @return  true if location, time and obs are missing
     */
    public boolean isMissing() {
        return (location == null) || (dateTime == null) || (data == null);
    }



    /**
     * Create, if needed, and return the component array.
     *
     *
     * @param copy  true to copy
     * @return components
     */
    public Data[] getComponents(boolean copy) {
        //Create the array and populate it if needed
        if (components == null) {
            components    = new Data[3];
            components[0] = location;
            components[1] = dateTime;
            components[2] = data;
        }
        return components;
    }


    /**
     * Construct a new PointObTuple from the given location, date/time and data.
     *
     * @param location  location of the observation
     * @param dateTime  date/time of the observation
     * @param data      associated data.
     * @param tupleType The tuple type to use
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public PointObTuple(EarthLocation location, DateTime dateTime, Data data,
                        TupleType tupleType)
            throws VisADException, RemoteException {
        this(location, dateTime, data, tupleType, true);
    }

    /**
     * Construct a new PointObTuple from the given location, date/time and data.
     *
     * @param location  location of the observation
     * @param dateTime  date/time of the observation
     * @param data      associated data.
     * @param tupleType The tuple type to use
     * @param checkType If true then check that the tuple type matches the type of the data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */

    public PointObTuple(EarthLocation location, DateTime dateTime, Data data,
                        TupleType tupleType, boolean checkType)
            throws VisADException, RemoteException {
        super(tupleType);
        this.location = location;
        this.dateTime = dateTime;
        this.data     = data;
    }

    /**
     * Get the geolocated location of the observation.
     *
     * @return observation's geolocation
     */
    public EarthLocation getEarthLocation() {
        return location;
    }

    /**
     * Get the time associated with this observation.
     *
     * @return DateTime for this observation.
     */
    public DateTime getDateTime() {
        return dateTime;
    }

    /**
     * Get the data associated with this object.
     *
     * @return Data for this observation.
     */
    public Data getData() {
        return data;
    }

    /**
     * Check to see if this is equal to <code>o</code>
     * @param o object in question
     * @return true if they are equal.
     */
    public boolean equals(Object o) {
        if ( !(o instanceof PointObTuple)) {
            return false;
        }
        PointObTuple that = (PointObTuple) o;
        return location.equals(that.location)
               && dateTime.equals(that.dateTime) && data.equals(that.data);
    }

    /**
     * Returns the hash code of this object.
     * @return            The hash code of this object.
     */
    public int hashCode() {
        return location.hashCode() ^ dateTime.hashCode() ^ data.hashCode();
    }

    /**
     * Clones this instance.
     *
     * @return                    A clone of this instance.
     */
    public final Object clone() {
        PointObTuple clone;
        try {
            clone = (PointObTuple) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Assertion failure");
        }
        clone.location = location;
        clone.dateTime = dateTime;
        clone.data     = data;
        return clone;
    }

    /**
     * String representation of the point observation.
     *
     * @return this ob as a string.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Point Ob: ");
        buf.append("\tLocation: ");
        buf.append(location.toString());
        buf.append("\n\tDateTime: ");
        buf.append(dateTime.toString());
        buf.append("\n\tData: ");
        buf.append(data.toString());
        return buf.toString();
    }

}
