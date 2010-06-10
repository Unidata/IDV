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

package ucar.unidata.repository;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Service {
    public static final String TYPE_KML = "kml";
    public static final String TYPE_WMS = "wms";
    public static final String TYPE_GRID = "grid";

    private String type;
    private String name;
    private String url;
    private String icon;



    public Service(String type, String name, String url) {
        this(type,name,url,null);
    }

    public Service(String type, String name, String url, String icon) {
        this.type = type;
        this.name = name;
        this.url = url;
        this.icon = icon;
    }


    public boolean isType(String type) {
        return this.type.equals(type);
    }

    /**
       Set the Type property.

       @param value The new value for Type
    **/
    public void setType (String value) {
	this.type = value;
    }

    /**
       Get the Type property.

       @return The Type
    **/
    public String getType () {
	return this.type;
    }

    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	this.name = value;
    }

    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
	return this.name;
    }

    /**
       Set the Url property.

       @param value The new value for Url
    **/
    public void setUrl (String value) {
	this.url = value;
    }

    /**
       Get the Url property.

       @return The Url
    **/
    public String getUrl () {
	return this.url;
    }


    /**
       Set the Icon property.

       @param value The new value for Icon
    **/
    public void setIcon (String value) {
	this.icon = value;
    }

    /**
       Get the Icon property.

       @return The Icon
    **/
    public String getIcon () {
	return this.icon;
    }



}
