/*
 * $Id: SoundingAdapter.java,v 1.13 2006/12/01 20:42:44 jeffmc Exp $
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


import ucar.unidata.data.sounding.SoundingOb;
import ucar.unidata.data.sounding.SoundingStation;

import ucar.unidata.util.LogUtil;

import ucar.visad.quantities.GeopotentialAltitude;

import visad.DateTime;



import java.util.List;


/**
 * Interface for adapting and retrieving sounding datasets into SoundingObs
 */
public interface SoundingAdapter {

    /**
     *  Update internal data to reflect the current state
     */
    public void update();


    /**
     * Retrieves an array of the all stations in the dataset.
     *
     * @return   list of sounding stations or empty list if none found
     */
    public List getStations();

    /**
     * Retrieves an array of the stations in the dataset for a given time.
     *
     * @param    time       time of observation
     *
     * @return   list of sounding stations or null if none found
     */
    public List getStations(DateTime time);

    /**
     * Retrieve all the sounding observations in the dataset
     *
     * @return  first sounding observation for the given station
     */
    public SoundingOb[] getSoundingObs();


    /**
     *  Retrieve an array of the sounding times available in the dataset.
     *
     *  @return array of timestamps or null if no data
     */
    public DateTime[] getSoundingTimes();

    /**
     * Retrieve a list of the sounding times available for the station.
     *
     * @param    station   station of observation
     *
     * @return list of timestamps or empty list if no times
     */
    public List getSoundingTimes(SoundingStation station);

    /**
     * Make sure that the given SoundingOb has been fully initialized with data
     *
     * @param so   SoundingOb to initialize
     *
     * @return   the initialized ob
     */
    public SoundingOb initSoundingOb(SoundingOb so);


    /**
     * Get the source for this Adapter; used mostly by XML persistence.
     *
     * @return   the source (server or filename)
     */
    public String getSource();

    /**
     * Set the source for this Adapter; used mostly by XML persistence.
     *
     * @param s   new source
     */
    public void setSource(String s);

}

