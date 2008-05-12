/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.storm;

import ucar.visad.Util;




import visad.*;

import visad.georef.EarthLocation;

import java.util.ArrayList;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 18, 2008
 * Time: 1:45:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormParam {

    RealType type;

    private boolean canDoDifference = true;

    public StormParam(RealType type) {
        this.type = type;
    }

    public StormParam(RealType type, boolean canDoDifference) {
        this(type);
        this.canDoDifference = canDoDifference;
    }


    public Real getReal(double  value) throws VisADException {
        return new Real(type, value);
    }


    public Unit getUnit() {
        return type.getDefaultUnit();
    }

    public int hashCode() {
        return type.hashCode();
    }

/**
Set the CanDoDifference property.

@param value The new value for CanDoDifference
**/
public void setCanDoDifference (boolean value) {
	canDoDifference = value;
}

/**
Get the CanDoDifference property.

@return The CanDoDifference
**/
public boolean getCanDoDifference () {
	return canDoDifference;
}





    public Real getAttribute(List<Real> attributes) {
        if(attributes==null) return null;
        for (Real attr : attributes) {
            if (attr.getType().equals(type)) {
                return attr;
            }
        }
        return null;
    }

    public boolean equals(Object o) {
        if(!this.getClass().equals(o.getClass())) return false;
        StormParam that = (StormParam) o;
        return this.type.equals(that.type);
    }


    public String toString() {
        return Util.cleanTypeName(type.getName()).replace("_", " ");
    }


/**
Set the Type property.

@param value The new value for Type
**/
public void setType (RealType value) {
	type = value;
}

/**
Get the Type property.

@return The Type
**/
public RealType getType () {
	return type;
}




}

