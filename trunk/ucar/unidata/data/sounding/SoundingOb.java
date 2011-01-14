/*
 * $Id: SoundingOb.java,v 1.18 2007/04/16 20:34:57 jeffmc Exp $
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


import ucar.unidata.data.sounding.RAOB;

import ucar.unidata.metdata.Station;

import ucar.unidata.util.Misc;



import visad.DateTime;


/**
 *  Provide support for an upper air observation with station information,
 *  a timestamp, and data.
 */
public class SoundingOb {

    /** the file for mandatory data */
    private String mandatoryFile;

    /** the file for significant data */
    private String sigFile;


    /** station for this ob */
    private SoundingStation station_;

    /** time for this ob */
    private DateTime timestamp_;

    /** data for this ob */
    private RAOB raob_;

    /**
     *  No-op constructor for XML persistence
     */
    public SoundingOb() {}


    /**
     * Create a sounding observation.
     *
     * @param   station     location information (lat, lon, id);
     * @param   timestamp   time of the observation
     */
    public SoundingOb(SoundingStation station, DateTime timestamp) {
        this(station, timestamp, null);
    }



    /**
     * Create a sounding observation.
     *
     * @param   station     location information (lat, lon, id);
     * @param   timestamp   time of the observation
     * @param   raob        the data
     */
    public SoundingOb(SoundingStation station, DateTime timestamp, RAOB raob) {
        station_   = station;
        timestamp_ = timestamp;
        raob_      = raob;
    }

    /**
     * Return the station information associated with this observation
     * @return   the station
     */
    public SoundingStation getStation() {
        return station_;
    }

    /**
     * Set the station
     *
     * @param s  the station
     */
    public void setStation(SoundingStation s) {
        station_ = s;
    }

    /**
     * Return the identifier of the station associated with this observation
     *
     * @return  the station ID
     */
    public String getStationIdentifier() {
        return station_.getIdentifier();
    }

    /**
     * Check that the raob has both a temperature profile and a
     * dewpoint profile; else return false.
     *
     * @return  check to see if this has a temp and dewpoint profile
     */
    public boolean hasData() {
        return !(getRAOB().getTemperatureProfile().isMissing()
                 && getRAOB().getDewPointProfile().isMissing()
                 && getRAOB().getWindProfile().isMissing());
    }


    /**
     * Return the RAOB associated with this observation
     *
     * @return  the RAOB
     */
    public RAOB getRAOB() {
        if (raob_ == null) {
            try {
                raob_ = new RAOB();
            } catch (Exception ve) {
                throw new IllegalStateException("Error creating RAOB:" + ve);
            }
        }
        return raob_;
    }

    /**
     * Set the RAOB member
     *
     * @param r  the RAOB
     */
    protected void setRAOB(RAOB r) {
        raob_ = r;
    }

    /**
     * Return the timestamp of this observation
     * @return  the time stamp
     */
    public DateTime getTimestamp() {
        return timestamp_;
    }

    /**
     * Set the date/time
     *
     * @param d  the timestamp
     */
    public void setTimestamp(DateTime d) {
        timestamp_ = d;
    }

    /**
     * Return a string representation of this SoundingOb
     *
     * @return  a string representation of this SoundingOb
     */
    public String toString() {
        return station_.toString() + " " + timestamp_.toString() + " "
               + ((raob_ != null)
                  ? raob_.toString()
                  : "");
    }

    /**
     * Print a pretty version of the object.
     * @return  a pretty version of this object
     */
    public String getLabel() {
        return station_.toString() + " " + timestamp_.toString();
    }

    /**
     * Check if another Object is equal to this one
     *
     * @param other  the other object
     * @return  true if they are equal (same station and time)
     */
    public boolean equals(Object other) {
        if ( !(other instanceof SoundingOb)) {
            return false;
        }
        SoundingOb that = (SoundingOb) other;
        return Misc.equals(station_, that.station_)
               && Misc.equals(timestamp_, that.timestamp_);
    }



    /**
     *  Set the MandatoryFile property.
     *
     *  @param value The new value for MandatoryFile
     */
    public void setMandatoryFile(String value) {
        mandatoryFile = value;
    }

    /**
     *  Get the MandatoryFile property.
     *
     *  @return The MandatoryFile
     */
    public String getMandatoryFile() {
        return mandatoryFile;
    }

    /**
     *  Set the SigFile property.
     *
     *  @param value The new value for SigFile
     */
    public void setSigFile(String value) {
        sigFile = value;
    }

    /**
     *  Get the SigFile property.
     *
     *  @return The SigFile
     */
    public String getSigFile() {
        return sigFile;
    }




}

