/*
 * $Id: ViewManager.java,v 1.401 2007/08/16 14:05:04 jeffmc Exp $
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
 * This library is distributed in the hope that it will be2 useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */





package ucar.unidata.idv;


import visad.*;

import visad.georef.*;

import java.util.Date;


/**
 *
 * @author IDV development team
 */

public class FlythroughPoint {

    /** _more_          */
    private EarthLocation earthLocation;

    /** _more_          */
    private Date dateTime;

    /** _more_          */
    private double tilt = Double.NaN;

    /** _more_          */
    private double zoom = Double.NaN;

    /**
     * _more_
     */
    public FlythroughPoint() {}

    /**
     * _more_
     *
     * @param earthLocation _more_
     */
    public FlythroughPoint(EarthLocation earthLocation) {
        this.earthLocation = earthLocation;
    }

    /**
     * Set the EarthLocation property.
     *
     * @param value The new value for EarthLocation
     */
    public void setEarthLocation(EarthLocation value) {
        this.earthLocation = value;
    }

    /**
     * Get the EarthLocation property.
     *
     * @return The EarthLocation
     */
    public EarthLocation getEarthLocation() {
        return this.earthLocation;
    }

    /**
     * Set the DateTime property.
     *
     * @param value The new value for DateTime
     */
    public void setDateTime(Date value) {
        this.dateTime = value;
    }

    /**
     * Get the DateTime property.
     *
     * @return The DateTime
     */
    public Date getDateTime() {
        return this.dateTime;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasTilt() {
        return tilt == tilt;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasZoom() {
        return zoom == zoom;
    }

    /**
     * Set the Tilt property.
     *
     * @param value The new value for Tilt
     */
    public void setTilt(double value) {
        this.tilt = value;
    }

    /**
     * Get the Tilt property.
     *
     * @return The Tilt
     */
    public double getTilt() {
        return this.tilt;
    }

    /**
     * Set the Zoom property.
     *
     * @param value The new value for Zoom
     */
    public void setZoom(double value) {
        this.zoom = value;
    }

    /**
     * Get the Zoom property.
     *
     * @return The Zoom
     */
    public double getZoom() {
        return this.zoom;
    }



}

