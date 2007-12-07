/*
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




package ucar.unidata.repository;


import org.w3c.dom.*;




import ucar.unidata.data.SqlUtil;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;


/**
 */

public class Column implements Tables, Constants {

    /** _more_          */
    public static final String TYPE_STRING = "string";

    /** _more_          */
    public static final String TYPE_INT = "int";

    /** _more_          */
    public static final String TYPE_DOUBLE = "double";

    /** _more_          */
    public static final String TYPE_BOOLEAN = "boolean";

    /** _more_          */
    public static final String TYPE_ENUMERATION = "enumeration";

    /** _more_          */
    public static final String TYPE_TIMESTAMP = "timestamp";

    /** _more_          */
    public static final String SEARCHTYPE_TEXT = "text";

    /** _more_          */
    public static final String SEARCHTYPE_SELECT = "select";


    /** _more_          */
    private static final String ATTR_NAME = "name";

    /** _more_          */
    private static final String ATTR_LABEL = "label";

    /** _more_          */
    private static final String ATTR_DESCRIPTION = "description";

    /** _more_          */
    private static final String ATTR_TYPE = "type";

    /** _more_          */
    private static final String ATTR_ISINDEX = "isindex";

    /** _more_          */
    private static final String ATTR_ISSEARCHABLE = "issearchable";

    /** _more_          */
    private static final String ATTR_VALUES = "values";

    /** _more_          */
    private static final String ATTR_DEFAULT = "default";

    /** _more_          */
    private static final String ATTR_SIZE = "size";

    /** _more_          */
    private static final String ATTR_ROWS = "rows";

    /** _more_          */
    private static final String ATTR_COLUMNS = "columns";

    /** _more_          */
    private static final String ATTR_SEARCHTYPE = "searchtype";



    /** _more_          */
    private String table;

    /** _more_          */
    private String name;

    /** _more_          */
    private String label;

    /** _more_          */
    private String description;

    /** _more_          */
    private String type;

    /** _more_          */
    private String searchType = SEARCHTYPE_TEXT;

    /** _more_          */
    private boolean isIndex;

    /** _more_          */
    private boolean isSearchable;

    /** _more_          */
    private List values;

    /** _more_          */
    private String dflt;

    /** _more_          */
    private int size = 200;

    /** _more_          */
    private int rows = 1;

    /** _more_          */
    private int columns = 40;

    /**
     * _more_
     *
     * @param table _more_
     * @param element _more_
     */
    public Column(String table, Element element) {
        this.table = table;
        name       = XmlUtil.getAttribute(element, ATTR_NAME);
        label      = XmlUtil.getAttribute(element, ATTR_LABEL, name);
        searchType = XmlUtil.getAttribute(element, ATTR_SEARCHTYPE,
                                          searchType);
        description  = XmlUtil.getAttribute(element, ATTR_DESCRIPTION, label);
        type         = XmlUtil.getAttribute(element, ATTR_TYPE);
        dflt         = XmlUtil.getAttribute(element, ATTR_DEFAULT, "");
        isIndex      = XmlUtil.getAttribute(element, ATTR_ISINDEX, false);
        isSearchable = XmlUtil.getAttribute(element, ATTR_ISSEARCHABLE,
                                            false);
        size    = XmlUtil.getAttribute(element, ATTR_SIZE, size);
        rows    = XmlUtil.getAttribute(element, ATTR_ROWS, rows);
        columns = XmlUtil.getAttribute(element, ATTR_COLUMNS, columns);
        if (type.equals(TYPE_ENUMERATION)) {
            values = StringUtil.split(XmlUtil.getAttribute(element,
                    ATTR_VALUES), ",", true, true);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getSqlCreate() {
        String def = " " + name + " ";
        if (type.equals(TYPE_STRING)) {
            return def + "varchar(" + size + ") ";
        } else if (type.equals(TYPE_ENUMERATION)) {
            return def + "varchar(" + size + ") ";
        } else if (type.equals(TYPE_INT)) {
            return def + "int ";
        } else if (type.equals(TYPE_DOUBLE)) {
            return def + "double ";
        } else if (type.equals(TYPE_BOOLEAN)) {
            return def + "int ";
        } else {
            throw new IllegalArgumentException("Unknwon column type:" + type
                    + " for " + name);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSqlIndex() {
        if (isIndex) {
            return "CREATE INDEX " + table + "_INDEX_" + name + "  ON "
                   + table + " (" + name + ");\n";
        } else {
            return "";
        }
    }

    /**
     * _more_
     *
     * @param typeHandler _more_
     * @param formBuffer _more_
     * @param headerBuffer _more_
     * @param request _more_
     * @param where _more_
     *
     * @throws Exception _more_
     */
    public void addToForm(GenericTypeHandler typeHandler,
                          StringBuffer formBuffer, StringBuffer headerBuffer,
                          Request request, List where)
            throws Exception {
        if ( !getIsSearchable()) {
            return;
        }

        List tmp = new ArrayList(where);
        if (searchType.equals(SEARCHTYPE_SELECT)) {
            String[] values = SqlUtil.readString(
                                                 typeHandler.executeSelect(request,
                                      SqlUtil.distinct(getFullName()),
                                                                           tmp), 1);
            List list = new ArrayList();
            for (int i = 0; i < values.length; i++) {
                list.add(
                    new TwoFacedObject(
                        typeHandler.getRepository().getLongName(values[i]),
                        values[i]));
            }
            list.add(0, "All");
            formBuffer.append(HtmlUtil.tableEntry(HtmlUtil.bold(getLabel()
                    + ":"), HtmlUtil.select(getFullName(), list)));
        } else {
            if (rows > 1) {
                formBuffer.append(
                    HtmlUtil.tableEntry(
                        HtmlUtil.bold(getLabel() + ":"),
                        HtmlUtil.textArea(getFullName(), "", rows, columns)));
            } else {
                formBuffer.append(
                    HtmlUtil.tableEntry(
                        HtmlUtil.bold(getLabel() + ":"),
                        HtmlUtil.input(
                            getFullName(), "", "size=\"" + columns + "\"")));
            }
        }
        formBuffer.append("\n");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullName() {
        return table + "." + name;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the Label property.
     *
     * @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     * Get the Label property.
     *
     * @return The Label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the IsIndex property.
     *
     * @param value The new value for IsIndex
     */
    public void setIsIndex(boolean value) {
        isIndex = value;
    }

    /**
     * Get the IsIndex property.
     *
     * @return The IsIndex
     */
    public boolean getIsIndex() {
        return isIndex;
    }


    /**
     * Set the IsSearchable property.
     *
     * @param value The new value for IsSearchable
     */
    public void setIsSearchable(boolean value) {
        isSearchable = value;
    }

    /**
     * Get the IsSearchable property.
     *
     * @return The IsSearchable
     */
    public boolean getIsSearchable() {
        return isSearchable;
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
    public void setDflt(String value) {
        dflt = value;
    }

    /**
     * Get the Dflt property.
     *
     * @return The Dflt
     */
    public String getDflt() {
        return dflt;
    }


}

