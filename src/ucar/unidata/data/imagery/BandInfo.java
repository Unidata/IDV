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

package ucar.unidata.data.imagery;


import ucar.unidata.util.Misc;


import java.util.List;


/**
 * Class for holding Band information
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.5 $
 */
public class BandInfo {

    /** the band number */
    private int bandNumber;

    /** the band description */
    private String bandDescription = null;

    /** the list of calibration units for this band */
    private List calibrationUnits;

    /** the satellite sensor number */
    private int sensorNumber;

    /** the preferred unit */
    private String preferredUnit;


    /**
     * Default constructor
     */
    public BandInfo() {}

    /**
     * Create a new BandInfo
     *
     * @param sensor  satellite sensor number
     * @param band   band number
     */
    public BandInfo(int sensor, int band) {
        this(sensor, band, "Band " + band, null);
    }

    /**
     * Create a new BandInfo
     *
     * @param sensor  satellite sensor number
     * @param band   band number
     * @param description  description of band
     */
    public BandInfo(int sensor, int band, String description) {
        this(sensor, band, description, null);
    }

    /**
     * Create a new BandInfo
     *
     * @param sensor  satellite sensor number
     * @param band   band number
     * @param description  description of band
     * @param units available calibration units
     */
    public BandInfo(int sensor, int band, String description, List units) {
        this.sensorNumber     = sensor;
        this.bandNumber       = band;
        this.bandDescription  = description;
        this.calibrationUnits = units;
    }

    /**
     * Copy constructor
     *
     * @param that other BandInfo
     */
    public BandInfo(BandInfo that) {
        this.sensorNumber     = that.sensorNumber;
        this.bandNumber       = that.bandNumber;
        this.bandDescription  = that.bandDescription;
        this.calibrationUnits = that.calibrationUnits;
    }

    /**
     * Set the band number for this BandInfo
     *
     * @param band band number
     */
    public void setBandNumber(int band) {
        this.bandNumber = band;
    }

    /**
     * Get the band number for this BandInfo
     *
     * @return band number
     */
    public int getBandNumber() {
        return bandNumber;
    }

    /**
     * Set the band description for this BandInfo
     *
     * @param description band description
     */
    public void setBandDescription(String description) {
        this.bandDescription = description;
    }

    /**
     * Get the description for the band
     *
     * @return band description
     */
    public String getBandDescription() {
        return bandDescription;
    }

    /**
     * Set the satellite sensor number for this BandInfo
     *
     * @param sensor sensor number
     */
    public void setSensor(int sensor) {
        this.sensorNumber = sensor;
    }

    /**
     * Get the satellite sensor number for this BandInfo
     *
     * @return the satellite sensor number for this BandInfo
     */
    public int getSensor() {
        return sensorNumber;
    }

    /**
     * Set the calibration units for this BandInfo
     *
     * @param units   list of calibration units
     */
    public void setCalibrationUnits(List units) {
        this.calibrationUnits = units;
    }

    /**
     * Cet the calibration units for this BandInfo
     *
     * @return the  calibration units, may be a list of Strings or
     */
    public List getCalibrationUnits() {
        return calibrationUnits;
    }

    /**
     * Set the preferred calibration unit for this BandInfo
     *
     * @param unit   unit id
     */
    public void setPreferredUnit(String unit) {
        this.preferredUnit = unit;
    }

    /**
     * Cet the preferred calibration units for this BandInfo
     *
     * @return the preferred calibration units
     */
    public String getPreferredUnit() {
        return preferredUnit;
    }

    /**
     * Get a String Representation of this object
     *
     * @return a String Representation of this object
     */
    public String toString() {
        return bandDescription;
    }

    /**
     * Check for equality
     *
     * @param o object to check
     * @return true if object is equal to this
     */
    public boolean equals(Object o) {
        if ( !(o instanceof BandInfo)) {
            return false;
        }
        BandInfo that = (BandInfo) o;
        if (this == that) {
            return true;
        }
        return (sensorNumber == that.sensorNumber)
               && (bandNumber == that.bandNumber)
               && bandDescription.equals(that.bandDescription)
               && Misc.equals(calibrationUnits, that.calibrationUnits);
    }

    /**
     * Get the Hashcode for this object.
     * @return hashcode
     */
    public int hashCode() {
        int hashCode = 0;
        hashCode ^= bandNumber;
        hashCode ^= sensorNumber;
        hashCode ^= bandDescription.hashCode();
        if (calibrationUnits != null) {
            hashCode ^= calibrationUnits.hashCode();
        }
        return hashCode;
    }

    /**
     * Find the  index in the list of the first band with the band number.
     *
     * @param band   band number
     * @param bandinfos  list of BandInfos
     * @return the index in the list or -1
     */
    public static int findIndexByNumber(int band, List<BandInfo> bandinfos) {
        int index = -1;
        for (int i = 0; i < bandinfos.size(); i++) {
            BandInfo bi = (BandInfo) bandinfos.get(i);
            if (bi.getBandNumber() == band) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * Find the  index in the list of the first band with the band description.
     *
     * @param desc   band name
     * @param bandinfos  list of BandInfos
     * @return the index in the list or -1
     */
    public static int findIndexByName(String desc, List<BandInfo> bandinfos) {
        int index = -1;
        for (int i = 0; i < bandinfos.size(); i++) {
            BandInfo bi = (BandInfo) bandinfos.get(i);
            if (bi.getBandDescription().equals(desc)) {
                index = i;
                break;
            }
        }
        return index;
    }

}
