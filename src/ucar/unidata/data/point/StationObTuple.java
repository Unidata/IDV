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
import visad.Text;
import visad.Tuple;
import visad.VisADError;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.NamedLocation;

import java.rmi.RemoteException;


/**
 * Implementation of StationOb as a Tuple.  This is immutable.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.10 $ $Date: 2006/12/01 20:42:34 $
 */
public class StationObTuple extends Tuple implements StationOb {

    /** station data */
    private NamedLocation station;

    /** ob time */
    private DateTime dateTime;

    /** ob data */
    private Data data;

    /**
     * Construct a new StationObTuple from the given station, date/time
     * and data.
     *
     * @param station  station of the observation
     * @param dateTime  date/time of the observation
     * @param data      associated data.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public StationObTuple(NamedLocation station, DateTime dateTime, Data data)
            throws VisADException, RemoteException {
        super(new Data[] { station, dateTime, data }, false);
        this.station  = (NamedLocation) getComponent(0);
        this.dateTime = (DateTime) getComponent(1);
        this.data     = (Data) getComponent(2);
    }

    /**
     * Get the station of the observation.
     *
     * @return observation's station
     */
    public NamedLocation getStation() {
        return station;
    }

    /**
     * Get the geolocated location of the observation.
     *
     * @return observation's geolocation
     */
    public EarthLocation getEarthLocation() {
        return station.getEarthLocation();
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
     * Get the station's identifier
     *
     * @return station identifier
     */
    public Text getStationId() {
        return station.getIdentifier();
    }

    /**
     * Clones this instance.
     *
     * @return                    A clone of this instance.
     */
    public final Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException("Assertion failure");
        }
    }

    /**
     * String representation of the station observation.
     *
     * @return this ob as a string.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Station Ob: ");
        buf.append("\tStation: ");
        buf.append(station.toString());
        buf.append("\n\tDateTime: ");
        buf.append(dateTime.toString());
        buf.append("\n\tData: ");
        buf.append(data.toString());
        return buf.toString();
    }

}
