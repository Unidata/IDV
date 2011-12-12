/*
 * $Id: SoundingStation.java,v 1.10 2006/12/01 20:42:44 jeffmc Exp $
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

package ucar.unidata.data.sounding;


import ucar.unidata.metdata.NamedStationImpl;

import ucar.unidata.geoloc.Station;



import visad.VisADException;

import java.rmi.RemoteException;


/**
 * Temporary holder for sounding station info.    Wrapper around
 * ucar.unidata.metdata.NamedStationImpl
 *
 * @author IDV Development Team
 * @version $Revision: 1.10 $
 */
public class SoundingStation extends ucar.unidata.metdata.NamedStationImpl {


    /**
     *  No-op ctor for unpersisting
     */
    public SoundingStation() {}

    /**
     * Create a new SoundingStation
     *
     * @param id         station id
     * @param lat        station latitude (degrees)
     * @param lon        station longitude (degrees)
     * @param elev       station elevation (meters)
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     *
     */
    public SoundingStation(String id, double lat, double lon, double elev)
            throws VisADException, RemoteException {
        super(id, id, lat, lon, elev, visad.CommonUnit.meter);
    }

    /**
     * Make a SoundingStation from a Station
     * @param s  station 
     */
    public SoundingStation(Station s)
            throws VisADException, RemoteException {
        this(s.getName(), s.getLatitude(), s.getLongitude(), s.getAltitude());
    }

}

