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
    private String entryId;

    /** _more_          */
    private String type;


    /** _more_ */
    private String attr1;

    /** _more_ */
    private String attr2;

    /** _more_ */
    private String attr3;

    /** _more_ */
    private String attr4;



    /**
     * _more_
     *
     * @param id _more_
     * @param entryId _more_
     * @param type _more_
     * @param name _more_
     * @param content _more_
     * @param attr1 _more_
     * @param attr2 _more_
     * @param attr3 _more_
     * @param attr4 _more_
     */
    public Metadata(String id, String entryId, String type, String attr1,
                    String attr2, String attr3, String attr4) {
        this.id      = id;
        this.entryId = entryId;
        this.type    = type;
        this.attr1   = attr1;
        this.attr2   = attr2;
        this.attr3   = attr3;
        this.attr4   = attr4;
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
     * Set the EntryId property.
     *
     * @param value The new value for EntryId
     */
    public void setEntryId(String value) {
        entryId = value;
    }

    /**
     * Get the EntryId property.
     *
     * @return The EntryId
     */
    public String getEntryId() {
        return entryId;
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
     * Set the Attr1 property.
     *
     * @param value The new value for Attr1
     */
    public void setAttr1(String value) {
        attr1 = value;
    }

    /**
     * Get the Attr1 property.
     *
     * @return The Attr1
     */
    public String getAttr1() {
        return attr1;
    }

    /**
     * Set the Attr2 property.
     *
     * @param value The new value for Attr2
     */
    public void setAttr2(String value) {
        attr2 = value;
    }

    /**
     * Get the Attr2 property.
     *
     * @return The Attr2
     */
    public String getAttr2() {
        return attr2;
    }

    /**
     * Set the Attr3 property.
     *
     * @param value The new value for Attr3
     */
    public void setAttr3(String value) {
        attr3 = value;
    }

    /**
     * Get the Attr3 property.
     *
     * @return The Attr3
     */
    public String getAttr3() {
        return attr3;
    }

    /**
     * Set the Attr4 property.
     *
     * @param value The new value for Attr4
     */
    public void setAttr4(String value) {
        attr4 = value;
    }

    /**
     * Get the Attr4 property.
     *
     * @return The Attr4
     */
    public String getAttr4() {
        return attr4;
    }



}

