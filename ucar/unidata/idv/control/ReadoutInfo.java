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



package ucar.unidata.idv.control;

import ucar.unidata.idv.DisplayControl;
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
    private DisplayControl fromDisplay;
    private EarthLocation location;
    private Data data;
    private Real animationValue;


    public ReadoutInfo(DisplayControl from, Data data) {
        this.fromDisplay = from;
        this.data = data;
    }


    public ReadoutInfo(DisplayControl from, Data data, EarthLocation location, Real time) {
        this.fromDisplay = from;
        this.data = data;
        this.location = location;
        this.animationValue = time;
    }

/**
Set the FromDisplay property.

@param value The new value for FromDisplay
**/
public void setFromDisplay (DisplayControl value) {
	this.fromDisplay = value;
}

/**
Get the FromDisplay property.

@return The FromDisplay
**/
public DisplayControl getFromDisplay () {
	return this.fromDisplay;
}

/**
Set the Location property.

@param value The new value for Location
**/
public void setLocation (EarthLocation value) {
	this.location = value;
}

/**
Get the Location property.

@return The Location
**/
public EarthLocation getLocation () {
	return this.location;
}

/**
Set the Data property.

@param value The new value for Data
**/
public void setData (Data value) {
	this.data = value;
}

/**
Get the Data property.

@return The Data
**/
public Data getData () {
	return this.data;
}

public Real getReal () {
    if(data instanceof Real)
        return (Real) data;
    return null;
}


}

