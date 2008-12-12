/*
 * $Id: DataAlias.java,v 1.39 2006/12/01 20:41:20 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.data;


import visad.*;



/**
 *
 * @author IDV development team
 * @version $Revision: 1.39 $
 */

public class NamedArray {

    /** the name */
    private String name;

    /** the unit */
    private Unit unit;

    /** the values */
    private float[] values;

    /**
     * ctor
     *
     * @param name name
     * @param unit unit
     * @param values values
     */
    public NamedArray(String name, Unit unit, float[] values) {
        this.name   = name;
        this.unit   = unit;
        this.values = values;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     *  Set the Unit property.
     *
     *  @param value The new value for Unit
     */
    public void setUnit(Unit value) {
        unit = value;
    }

    /**
     *  Get the Unit property.
     *
     *  @return The Unit
     */
    public Unit getUnit() {
        return unit;
    }

    /**
     *  Set the Values property.
     *
     *  @param value The new value for Values
     */
    public void setValues(float[] value) {
        values = value;
    }

    /**
     *  Get the Values property.
     *
     *  @return The Values
     */
    public float[] getValues() {
        return values;
    }



}

