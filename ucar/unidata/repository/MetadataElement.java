/**
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



package ucar.unidata.repository;


import org.w3c.dom.*;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetadataElement implements Constants {

    /** _more_ */
    public static final String TYPE_SKIP = "skip";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_URL = "url";
    public static final String TYPE_EMAIL = "email";

    /** _more_ */
    public static final String TYPE_BOOLEAN = "boolean";

    /** _more_ */
    public static final String TYPE_ENUMERATION = "enumeration";

    /** _more_ */
    private String type = TYPE_STRING;

    /** _more_ */
    private String label = "";

    /** _more_ */
    private int rows = 1;

    /** _more_ */
    private int columns = 60;

    /** _more_ */
    private List values;

    /** _more_          */
    private String dflt = "";



    /**
     * _more_
     *
     * @param type _more_
     * @param label _more_
     * @param values _more_
     */
    public MetadataElement(String type, String label, List<Object> values) {
        this(type, label, 1, 60, values);
    }



    /**
     * _more_
     *
     * @param type _more_
     * @param label _more_
     */
    public MetadataElement(String type, String label) {
        this(type, label, 1, 60, null);
    }


    /**
     * _more_
     *
     * @param type _more_
     * @param label _more_
     * @param rows _more_
     * @param columns _more_
     * @param values _more_
     */
    public MetadataElement(String type, String label, int rows, int columns,
                           List<Object> values) {
        this.type    = type;
        this.label   = label;
        this.rows    = rows;
        this.columns = columns;
        this.values  = values;
    }


    private boolean isString(String type) {
        return type.equals(TYPE_STRING) || type.equals(TYPE_EMAIL)|| type.equals(TYPE_URL);
    }


    public void getHtml(MetadataHandler handler, StringBuffer sb, String value) {
        if(type.equals(TYPE_SKIP)) {
            return;
        }
        if(type.equals(TYPE_EMAIL)) {
            sb.append(HtmlUtil.href("mailto:"+value,value));
        } else if(type.equals(TYPE_URL)) {
            sb.append(HtmlUtil.href(value,value));
            } else {
            sb.append(value);
        }
        sb.append(HtmlUtil.br());
    }

    /**
     * _more_
     *
     * @param arg _more_
     * @param value _more_
     *
     * @return _more_
     */
    public String getForm(String arg, String value) {
        if(type.equals(TYPE_SKIP)) {
            return "";
        }

        value = ((value == null || value.length()==0)
                 ? dflt
                 : value);
        if (isString(type)) {
            if(rows>1) {
            return HtmlUtil.textArea(arg, value,
                                     rows,columns);
            } 

            return HtmlUtil.input(arg, value,
                                  HtmlUtil.attr(HtmlUtil.ATTR_SIZE,
                                      "" + columns));
        } else if (type.equals(TYPE_BOOLEAN)) {
            return HtmlUtil.checkbox(arg, "true", Misc.equals(value, "true"));
        } else if (type.equals(TYPE_ENUMERATION)) {
            return HtmlUtil.select(arg, values, value);
        } else {
            return null;
        }
    }


    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public String getType() {
        return type;
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
     *  Set the Rows property.
     *
     *  @param value The new value for Rows
     */
    public void setRows(int value) {
        rows = value;
    }

    /**
     *  Get the Rows property.
     *
     *  @return The Rows
     */
    public int getRows() {
        return rows;
    }

    /**
     *  Set the Columns property.
     *
     *  @param value The new value for Columns
     */
    public void setColumns(int value) {
        columns = value;
    }

    /**
     *  Get the Columns property.
     *
     *  @return The Columns
     */
    public int getColumns() {
        return columns;
    }


    /**
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(List value) {
        values = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List getValues() {
        return values;
    }


    /**
     * Set the Dflt property.
     *
     * @param value The new value for Dflt
     */
    public void setDefault(String value) {
        dflt = value;
    }

    /**
     * Get the Dflt property.
     *
     * @return The Dflt
     */
    public String getDefault() {
        return dflt;
    }


}

