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

import java.util.Hashtable;
import java.util.List;




import ucar.unidata.util.StringUtil;
import ucar.unidata.util.HtmlUtil;

import ucar.unidata.xml.XmlUtil;
import org.w3c.dom.*;


/**
 */

public class Column {
    public static final String TYPE_STRING = "string";
    public static final String TYPE_INT = "int";
    public static final String TYPE_DOUBLE = "double";
    public static final String TYPE_BOOLEAN = "boolean";
    public static final String TYPE_ENUMERATION    = "enumeration";
    public static final String TYPE_TIMESTAMP    = "timestamp";

    private static final  String ATTR_NAME="name"; 
    private static final  String ATTR_LABEL="label"; 
    private static final  String ATTR_DESCRIPTION="description"; 
    private static final  String ATTR_TYPE="type"; 
    private static final  String ATTR_ISINDEX="isindex"; 
    private static final  String ATTR_ISSEARCHABLE="issearchable"; 
    private static final  String ATTR_VALUES="values";     
    private static final  String ATTR_DEFAULT="default"; 
    private static final  String ATTR_SIZE="size"; 
    private static final  String ATTR_ROWS="rows";     
    private static final  String ATTR_COLUMNS="columns"; 



    private String table;
    private String name;
    private String label;
    private String description;
    private String type;
    private boolean isIndex;
    private boolean isSearchable;
    private List values;
    private String dflt;
    private int size=200;
    private int rows=1;
    private int columns=40;

    public Column(String table, Element element) {
        this.table = table;
        name = XmlUtil.getAttribute(element,ATTR_NAME);
        label = XmlUtil.getAttribute(element,ATTR_LABEL,name);
        description = XmlUtil.getAttribute(element,ATTR_DESCRIPTION,label);
        type = XmlUtil.getAttribute(element,ATTR_TYPE);
        dflt = XmlUtil.getAttribute(element,ATTR_DEFAULT,"");
        isIndex = XmlUtil.getAttribute(element,ATTR_ISINDEX,false);
        isSearchable = XmlUtil.getAttribute(element,ATTR_ISSEARCHABLE,false);
        size = XmlUtil.getAttribute(element,ATTR_SIZE,size);
        rows = XmlUtil.getAttribute(element,ATTR_ROWS, rows);
        columns = XmlUtil.getAttribute(element,ATTR_COLUMNS, columns);
        if(type.equals(TYPE_ENUMERATION)) {
            values= StringUtil.split(XmlUtil.getAttribute(element, ATTR_VALUES),",",true,true); 
        }
    }

    public String getSqlCreate() {
        String def = " " + name + " ";
        if(type.equals(TYPE_STRING)) 
            return  def+"varchar(" + size+") ";
        else if(type.equals(TYPE_ENUMERATION)) 
            return  def+"varchar(" + size+") ";
        else if(type.equals(TYPE_INT)) 
            return  def+"int ";
        else if(type.equals(TYPE_DOUBLE)) 
            return  def+"double ";
        else if(type.equals(TYPE_BOOLEAN)) 
            return  def+"int ";
        else 
            throw new IllegalArgumentException ("Unknwon column type:" + type + " for " + name);
    }

    public String getSqlIndex() {
        if(isIndex)
            return "CREATE INDEX " + table + "_INDEX_" + name +"  ON " + table + " ("+ name +");\n";
        else return  "";
    }

    public String getHtmlFormEntry() {
        if(rows>1) {
            return HtmlUtil.textArea(getFullName(), "",rows, columns);
        }
        return HtmlUtil.input(getFullName(), "", "size=\"" + columns +"\"");
    }


public String getFullName () {
    return table +"." + name;
}


/**
Set the Name property.

@param value The new value for Name
**/
public void setName (String value) {
	name = value;
}

/**
Get the Name property.

@return The Name
**/
public String getName () {
	return name;
}

/**
Set the Label property.

@param value The new value for Label
**/
public void setLabel (String value) {
	label = value;
}

/**
Get the Label property.

@return The Label
**/
public String getLabel () {
	return label;
}

/**
Set the Description property.

@param value The new value for Description
**/
public void setDescription (String value) {
	description = value;
}

/**
Get the Description property.

@return The Description
**/
public String getDescription () {
	return description;
}

/**
Set the Type property.

@param value The new value for Type
**/
public void setType (String value) {
	type = value;
}

/**
Get the Type property.

@return The Type
**/
public String getType () {
	return type;
}

/**
Set the IsIndex property.

@param value The new value for IsIndex
**/
public void setIsIndex (boolean value) {
	isIndex = value;
}

/**
Get the IsIndex property.

@return The IsIndex
**/
public boolean getIsIndex () {
	return isIndex;
}


/**
Set the IsSearchable property.

@param value The new value for IsSearchable
**/
public void setIsSearchable (boolean value) {
	isSearchable = value;
}

/**
Get the IsSearchable property.

@return The IsSearchable
**/
public boolean getIsSearchable () {
	return isSearchable;
}



/**
Set the Values property.

@param value The new value for Values
**/
public void setValues (List value) {
	values = value;
}

/**
Get the Values property.

@return The Values
**/
public List getValues () {
	return values;
}

/**
Set the Dflt property.

@param value The new value for Dflt
**/
public void setDflt (String value) {
	dflt = value;
}

/**
Get the Dflt property.

@return The Dflt
**/
public String getDflt () {
	return dflt;
}


}

