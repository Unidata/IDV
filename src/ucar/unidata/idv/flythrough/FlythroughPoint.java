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








package ucar.unidata.idv.flythrough;


import ucar.unidata.idv.*;


import visad.*;

import visad.georef.*;

import java.util.Date;


/**
 *
 * @author IDV development team
 */

public class FlythroughPoint {

    /** _more_ */
    private EarthLocation earthLocation;

    /** _more_ */
    private DateTime dateTime;

    /** _more_ */
    private double tiltX = Double.NaN;

    /** _more_ */
    private double tiltY = Double.NaN;

    /** _more_ */
    private double tiltZ = Double.NaN;

    /** _more_ */
    private double zoom = Double.NaN;


    /** _more_ */
    private double[] matrix;


    /** _more_ */
    private String description;

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
     * _more_
     *
     * @param earthLocation _more_
     * @param dttm _more_
     */
    public FlythroughPoint(EarthLocation earthLocation, DateTime dttm) {
        this(earthLocation, dttm, Double.NaN, Double.NaN, Double.NaN);
    }


    /**
     * _more_
     *
     * @param earthLocation _more_
     * @param dttm _more_
     * @param tiltX _more_
     * @param tiltY _more_
     * @param tiltZ _more_
     */

    public FlythroughPoint(EarthLocation earthLocation, DateTime dttm,
                           double tiltX, double tiltY, double tiltZ) {
        this.earthLocation = earthLocation;
        this.dateTime      = dttm;
        this.tiltX         = tiltX;
        this.tiltY         = tiltY;
        this.tiltZ         = tiltZ;
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
    public void setDateTime(DateTime value) {
        this.dateTime = value;
    }

    /**
     * Get the DateTime property.
     *
     * @return The DateTime
     */
    public DateTime getDateTime() {
        return this.dateTime;
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

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasTiltX() {
        return tiltX == tiltX;
    }

    /**
     * Set the Tilt propertx.
     *
     * @param value The new value for Tilt
     */
    public void setTiltX(double value) {
        this.tiltX = value;
    }

    /**
     * Get the Tilt property.
     *
     * @return The Tilt
     */
    public double getTiltX() {
        return this.tiltX;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasTiltY() {
        return tiltY == tiltY;
    }

    /**
     * Set the Tilt property.
     *
     * @param value The new value for Tilt
     */
    public void setTiltY(double value) {
        this.tiltY = value;
    }

    /**
     * Get the Tilt property.
     *
     * @return The Tilt
     */
    public double getTiltY() {
        return this.tiltY;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasTiltZ() {
        return tiltZ == tiltZ;
    }

    /**
     * Set the Tilt property.
     *
     * @param value The new value for Tilt
     */
    public void setTiltZ(double value) {
        this.tiltZ = value;
    }


    /**
     * Get the Tilt property.
     *
     * @return The Tilt
     */
    public double getTiltZ() {
        return this.tiltZ;
    }


    /**
     *  Set the Matrix property.
     *
     *  @param value The new value for Matrix
     */
    public void setMatrix(double[] value) {
        this.matrix = value;
    }

    /**
     *  Get the Matrix property.
     *
     *  @return The Matrix
     */
    public double[] getMatrix() {
        return this.matrix;
    }

    /**
     *  Set the Description property.
     *
     *  @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     *  Get the Description property.
     *
     *  @return The Description
     */
    public String getDescription() {
        return description;
    }



}

