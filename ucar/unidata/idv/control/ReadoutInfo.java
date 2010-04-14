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

package ucar.unidata.idv.control;


import ucar.unidata.idv.DisplayControl;

import ucar.unidata.util.Range;
import ucar.unidata.view.*;
import ucar.unidata.view.geoloc.*;



import visad.*;

import visad.georef.*;

import visad.java3d.*;



/**
 *
 * @author IDV development team
 */

public class ReadoutInfo {

    /** _more_ */
    private DisplayControl fromDisplay;

    /** _more_ */
    private EarthLocation location;

    /** _more_ */
    private Data data;

    /** _more_ */
    private Real animationValue;

    /** _more_ */
    private Unit unit;

    /** _more_ */
    private Range range;


    /** _more_ */
    private String imageUrl;

    /** _more_ */
    private String imageName;

    /**
     * _more_
     *
     * @param from _more_
     * @param data _more_
     */
    public ReadoutInfo(DisplayControl from, Data data) {
        this.fromDisplay = from;
        this.data        = data;
    }


    /**
     * _more_
     *
     * @param from _more_
     * @param data _more_
     * @param location _more_
     * @param time _more_
     */
    public ReadoutInfo(DisplayControl from, Data data,
                       EarthLocation location, Real time) {
        this.fromDisplay    = from;
        this.data           = data;
        this.location       = location;
        this.animationValue = time;
    }

    /**
     * Set the FromDisplay property.
     *
     * @param value The new value for FromDisplay
     */
    public void setFromDisplay(DisplayControl value) {
        this.fromDisplay = value;
    }

    /**
     * Get the FromDisplay property.
     *
     * @return The FromDisplay
     */
    public DisplayControl getFromDisplay() {
        return this.fromDisplay;
    }

    /**
     * Set the Location property.
     *
     * @param value The new value for Location
     */
    public void setLocation(EarthLocation value) {
        this.location = value;
    }

    /**
     * Get the Location property.
     *
     * @return The Location
     */
    public EarthLocation getLocation() {
        return this.location;
    }

    /**
     * Set the Data property.
     *
     * @param value The new value for Data
     */
    public void setData(Data value) {
        this.data = value;
    }

    /**
     * Get the Data property.
     *
     * @return The Data
     */
    public Data getData() {
        return this.data;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Real getReal() {
        if (data instanceof Real) {
            return (Real) data;
        }
        return null;
    }

    /**
     *  Set the Unit property.
     *
     *  @param value The new value for Unit
     */
    public void setUnit(Unit value) {
        this.unit = value;
    }

    /**
     *  Get the Unit property.
     *
     *  @return The Unit
     */
    public Unit getUnit() {
        return this.unit;
    }

    /**
     *  Set the Range property.
     *
     *  @param value The new value for Range
     */
    public void setRange(Range value) {
        this.range = value;
    }

    /**
     *  Get the Range property.
     *
     *  @return The Range
     */
    public Range getRange() {
        return this.range;
    }


    /**
     *  Set the ImageUrl property.
     *
     *  @param value The new value for ImageUrl
     */
    public void setImageUrl(String value) {
        this.imageUrl = value;
    }

    /**
     *  Get the ImageUrl property.
     *
     *  @return The ImageUrl
     */
    public String getImageUrl() {
        return this.imageUrl;
    }



    /**
     *  Set the ImageName property.
     *
     *  @param value The new value for ImageName
     */
    public void setImageName(String value) {
        this.imageName = value;
    }

    /**
     *  Get the ImageName property.
     *
     *  @return The ImageName
     */
    public String getImageName() {
        return this.imageName;
    }


}
