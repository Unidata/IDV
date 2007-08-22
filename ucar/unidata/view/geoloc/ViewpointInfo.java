/*
 * $Id: ViewpointInfo.java,v 1.5 2005/05/13 18:33:15 jeffmc Exp $
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

package ucar.unidata.view.geoloc;


/**
 * A class to transfer angle of view settings from the
 * dialog box ViewpointDialog class. Azimuth is angle of line of view,
 * from north, clockwise, 0 to 360 degrees.
 * Tilt is angle down from overhead, 0 to 180 degrees; looking
 * straight down is 0, sideways is 90, straight up is 180.
 *
 * Member data .azimuth and .tilt is public for ease of access.
 *
 * @author Jeff McWhirter
 * @version $Id: ViewpointInfo.java,v 1.5 2005/05/13 18:33:15 jeffmc Exp $
 */
public class ViewpointInfo {

    /** Azimuth value */
    public double azimuth = 180.0;

    /** Tilt value */
    public double tilt = 45.0;

    /**
     * Default constructor
     *
     */
    public ViewpointInfo() {}

    /**
     * Construct class to hold angle of view settings from the
     * dialog box ViewpointDialog class.
     *
     * @param azimuth angle of line of view, from north, clockwise,
     *                0 to 360 degrees.
     * @param tilt    angle down from overhead, 0 to 180 degrees; looking
     *                 straight down is 0, sideways is 90, straight up is 180.
     *
     */
    public ViewpointInfo(double azimuth, double tilt) {
        this.azimuth = azimuth;
        this.tilt    = tilt;
    }


    /**
     *  Set the Azimuth property.
     *
     *  @param value The new value for Azimuth
     */
    public void setAzimuth(double value) {
        azimuth = value;
    }

    /**
     *  Get the Azimuth property.
     *
     *  @return The Azimuth
     */
    public double getAzimuth() {
        return azimuth;
    }


    /**
     *  Set the Tilt property.
     *
     *  @param value The new value for Tilt
     */
    public void setTilt(double value) {
        tilt = value;
    }

    /**
     *  Get the Tilt property.
     *
     *  @return The Tilt
     */
    public double getTilt() {
        return tilt;
    }

}
