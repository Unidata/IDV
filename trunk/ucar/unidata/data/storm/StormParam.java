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


import ucar.unidata.data.DataChoice;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

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

    /** _more_ */
    RealType type;

    /** _more_ */
    private boolean canDoDifference = true;

    /** _more_ */
    private boolean derived = false;

    /** _more_ */
    private boolean isChartParam = true;

    /**
     * _more_
     */
    public StormParam() {}

    /**
     * _more_
     *
     * @param type _more_
     */
    public StormParam(RealType type) {
        this.type = type;
        if (type != null) {
            DataChoice.addCurrentName(new TwoFacedObject("Storm Track>"
                    + type, Util.cleanTypeName(type)));
        }
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param derived _more_
     */
    public StormParam(RealType type, boolean derived) {
        this(type);
        this.derived = derived;
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param derived _more_
     * @param canDoDiff _more_
     */
    public StormParam(RealType type, boolean derived, boolean canDoDiff) {
        this(type);
        this.derived         = derived;
        this.canDoDifference = canDoDiff;
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param derived _more_
     * @param canDoDiff _more_
     * @param chartList _more_
     */
    public StormParam(RealType type, boolean derived, boolean canDoDiff,
                      boolean chartList) {
        this(type);
        this.derived         = derived;
        this.canDoDifference = canDoDiff;
        this.isChartParam    = chartList;
    }

    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public Real getReal(double value) throws VisADException {
        return new Real(type, value);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Unit getUnit() {
        return type.getDefaultUnit();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return type.hashCode();
    }

    /**
     * Set the CanDoDifference property.
     *
     * @param value The new value for CanDoDifference
     */
    public void setCanDoDifference(boolean value) {
        canDoDifference = value;
    }

    /**
     * Get the CanDoDifference property.
     *
     * @return The CanDoDifference
     */
    public boolean getCanDoDifference() {
        return canDoDifference;
    }


    /**
     * Set the CanDoDifference property.
     *
     * @param value The new value for CanDoDifference
     */
    public void setIsChartParam(boolean value) {
        isChartParam = value;
    }

    /**
     * Get the CanDoDifference property.
     *
     * @return The CanDoDifference
     */
    public boolean getIsChartParam() {
        return isChartParam;
    }



    /**
     * _more_
     *
     * @param attributes _more_
     *
     * @return _more_
     */
    public Real getAttribute(List<Real> attributes) {
        if (attributes == null) {
            return null;
        }
        for (Real attr : attributes) {
            if (attr.getType().equals(type)) {
                return attr;
            }
        }
        return null;
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !this.getClass().equals(o.getClass())) {
            return false;
        }
        StormParam that = (StormParam) o;
        return this.type.equals(that.type);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return Util.cleanTypeName(type.getName()).replace("_", " ");
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return Util.cleanTypeName(type.getName());
    }


    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(RealType value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public RealType getType() {
        return type;
    }


    /**
     * Set the Derived property.
     *
     * @param value The new value for Derived
     */
    public void setDerived(boolean value) {
        derived = value;
    }

    /**
     * Get the Derived property.
     *
     * @return The Derived
     */
    public boolean getDerived() {
        return derived;
    }



}

