/**
 * $Id: PropertyValue.java,v 1.4 2007/08/20 04:37:40 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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




package ucar.unidata.util;


import javax.swing.*;


/**
 * Class PropertyValue _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.4 $
 */
public class PropertyValue {

    /** _more_          */
    private String name;

    /** _more_          */
    private String label;

    /** _more_          */
    private Object value;

    /** _more_          */
    private String category;


    /**
     * _more_
     */
    public PropertyValue() {}

    /**
     * _more_
     *
     * @param name _more_
     * @param label _more_
     * @param obj _more_
     * @param category _more_
     */
    public PropertyValue(String name, String label, Object obj,
                         String category) {
        this.name     = name;
        this.label    = label;
        this.value    = obj;
        this.category = category;
    }


    public PropertyValue(PropertyValue that) {
        this(that.getName(), that.getLabel(), that.getValue(), that.getCategory());
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
     *  Set the Label property.
     *
     *  @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     *  Get the Label property.
     *
     *  @return The Label
     */
    public String getLabel() {
        return label;
    }

    /**
     *  Set the Value property.
     *
     *  @param value The new value for Value
     *
     * @param v _more_
     */
    public void setValue(Object v) {
        value = v;
    }

    /**
     *  Get the Value property.
     *
     *  @return The Value
     */
    public Object getValue() {
        return value;
    }

    /**
     *  Set the Category property.
     *
     *  @param value The new value for Category
     */
    public void setCategory(String value) {
        category = value;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public String getCategory() {
        return category;
    }


    public String toString() {
        return name + " " + label;
    }

}

