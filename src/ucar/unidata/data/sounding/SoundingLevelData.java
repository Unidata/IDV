/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

/**
 * A class to hold sounding level data
 *
 * @author IDV Development Team
 * 
 */
class SoundingLevelData {

    /** the pressure */
    public float pressure;

    /** the height */
    public float height;

    /** the temperature */
    public float temperature;

    /** the dewpoint */
    public float dewpoint;

    /** the wind direction */
    public float direction;

    /** the wind speed */
    public float speed;

    /**
     * Ctor
     */
    public SoundingLevelData() {}

    /**
     * Get a String representation of this object
     *
     * @return the String representation of this object
     */
    public String toString() {
        StringBuffer b = new StringBuffer();
        b.append("p: ");
        b.append(pressure);
        b.append(" z: ");
        b.append(height);
        b.append(" t: ");
        b.append(temperature);
        b.append(" dp: ");
        b.append(dewpoint);
        b.append(" wind: ");
        b.append(direction);
        b.append("/");
        b.append(speed);
        return b.toString();
    }
}