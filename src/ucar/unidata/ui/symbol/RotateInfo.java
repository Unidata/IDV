/*
 * $Id: RotateInfo.java,v 1.4 2007/05/22 20:00:22 jeffmc Exp $
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

package ucar.unidata.ui.symbol;


import ucar.unidata.util.Range;


import visad.Unit;



/**
 * Holds information on how to rotate around a particular axis.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.4 $
 */
public class RotateInfo implements Cloneable {

    /** which axis */
    public static final int TYPE_X = 0;

    /** which axis */
    public static final int TYPE_Y = 1;

    /** which axis */
    public static final int TYPE_Z = 2;

    /** which axis */
    public static final int[] TYPES = { TYPE_X, TYPE_Y, TYPE_Z };

    /** which axis */
    private int type;

    /** unit for rotate by parameter */
    private Unit unit = null;

    /** name of unit for  by parameter */
    private String unitName = null;


    /** parameter name to  by */
    private String param;

    /** default rotation range */
    Range range = new Range(0, 360);

    /** default range for data to */
    Range dataRange = new Range(0, 360);


    /**
     * default ctor
     */
    public RotateInfo() {}


    /**
     * ctor
     *
     * @param type Axis type
     */
    public RotateInfo(int type) {
        this.type = type;
    }


    public RotateInfo doClone() throws CloneNotSupportedException {
        return (RotateInfo) clone();
    }

    /**
     * Set the Param property.
     *
     * @param value The new value for Param
     */
    public void setParam(String value) {
        param = value;
    }

    /**
     * Get the Param property.
     *
     * @return The Param
     */
    public String getParam() {
        return param;
    }




    /**
     * Set the name of the  unit.  Used by subclasses which
     * have values that can be ed in different units.
     * @param name    name of unit
     */
    public void setUnitName(String name) {
        unitName = name;
        unit     = null;
    }

    /**
     * Get the name of the  unit.
     * @return  String representation of the unit name.
     *          May be <code>null</code>.
     */
    public String getUnitName() {
        return unitName;
    }



    /**
     * Get the the  unit.
     * @return  Unit used for ing values. May be <code>null</code>.
     */
    public Unit getUnit() {
        if ((unit == null) && (unitName != null)) {
            try {
                unit = ucar.visad.Util.parseUnit(unitName);
            } catch (Exception exc) {}
        }
        return unit;
    }



    /**
     *  Set the Range property.
     *
     *  @param value The new value for Range
     */
    public void setRange(Range value) {
        range = value;
    }

    /**
     *  Get the Range property.
     *
     *  @return The Range
     */
    public Range getRange() {
        return range;
    }


    /**
     *  Set the Range property.
     *
     *  @param value The new value for Range
     */
    public void setDataRange(Range value) {
        dataRange = value;
    }

    /**
     *  Get the DataRange property.
     *
     *  @return The DataRange
     */
    public Range getDataRange() {
        return dataRange;
    }


    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(int value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public int getType() {
        return type;
    }



}

