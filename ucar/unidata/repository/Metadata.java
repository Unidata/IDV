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


import ucar.unidata.data.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringBufferCollection;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Metadata implements Constants, Tables {

    /** _more_ */
    public static final String TYPE_HTML = "html";

    /** _more_ */
    public static final String TYPE_URL = "html";

    /** _more_ */
    public static final String TYPE_LINK = "link";




    /** _more_ */
    private String id;

    /** _more_ */
    private String name;

    /** _more_ */
    private String type;

    /** _more_ */
    private String subType;

    /** _more_ */
    private String content;

    private boolean inherited = false;


    /**
     * _more_
     *
     * @param id _more_
     * @param type _more_
     * @param name _more_
     * @param content _more_
     */
    public Metadata(String id,  String type,
                    String name, String content) {
        this.id           = id;
        this.name         = name;
        this.type = type;
        this.content      = content;
    }


    /**
     * _more_
     *
     * @param type _more_
     * @param name _more_
     * @param content _more_
     */
    public Metadata(String type, String name, String content) {
        this.type = type;
        this.name         = name;
        this.content      = content;
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
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
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
     * Set the Content property.
     *
     * @param value The new value for Content
     */
    public void setContent(String value) {
        content = value;
    }

    /**
     * Get the Content property.
     *
     * @return The Content
     */
    public String getContent() {
        return content;
    }




/**
Set the Inherited property.

@param value The new value for Inherited
**/
public void setInherited (boolean value) {
	inherited = value;
}

/**
Get the Inherited property.

@return The Inherited
**/
public boolean getInherited () {
	return inherited;
}

/**
Set the SubType property.

@param value The new value for SubType
**/
public void setSubType (String value) {
	subType = value;
}

/**
Get the SubType property.

@return The SubType
**/
public String getSubType () {
	return subType;
}



}

