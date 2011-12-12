/*
 * $Id: KmlId.java,v 1.5 2007/04/16 20:34:52 jeffmc Exp $
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

package ucar.unidata.data.gis;


import org.w3c.dom.*;

import ucar.unidata.data.*;


import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;

import ucar.unidata.xml.XmlUtil;


import visad.*;

import java.awt.*;

import java.io.*;


import java.net.URL;
import java.net.URLConnection;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Hashtable;
import java.util.List;
import java.util.zip.*;



/**
 * This is used to identify a node or collection of nodes in the kml.
 * It is used as the id for  the data choice
 *
 * @author IDV development team
 * @version $Revision: 1.5 $ $Date: 2007/04/16 20:34:52 $
 */
public class KmlId {

    /** Node type */
    public static final String NODE_GROUNDOVERLAY = "GroundOverlay";

    public static final String NODE_PHOTOOVERLAY = "PhotoOverlay";

    /** Node type */
    public static final String NODE_PLACEMARKS = "Placemarks";

    /** Node type */
    public static final String NODE_SHAPES = "Shapes";


    /** type of node */
    private String type;

    /** my name */
    private String name;

    /** displayCategories */
    private String displayCategories;

    /** Where is the kml doc I am from */
    private String docUrl;

    /** Humm, extra stuff */
    private Object extra;

    /**
     * ctor
     */
    public KmlId() {}

    /**
     * ctor
     *
     * @param type type
     * @param name name
     * @param displayCategories  displayCategories
     * @param docUrl kml url
     */
    public KmlId(String type, String name, String displayCategories,
                 String docUrl) {
        this(type, name, displayCategories, docUrl, null);
    }

    /**
     * ctor
     *
     * @param type type
     * @param name name
     * @param displayCategories  displayCategories
     * @param docUrl kml url
     * @param extra extra stuff
     */
    public KmlId(String type, String name, String displayCategories,
                 String docUrl, Object extra) {
        this.type              = type;
        this.name              = name;
        this.displayCategories = displayCategories;
        this.docUrl            = docUrl;
        this.extra             = extra;
    }



    /**
     * Am I a placemark list
     *
     * @return is placemarks
     */
    public boolean isPlacemarks() {
        return type.equals(NODE_PLACEMARKS);
    }


    /**
     * Am I a ground overlay
     *
     * @return is ground overlay
     */
    public boolean isGroundOverlay() {
        return type.equals(NODE_GROUNDOVERLAY);
    }


    /**
     * Am I a photo overlay
     *
     * @return is photo overlay
     */
    public boolean isPhotoOverlay() {
        return type.equals(NODE_PHOTOOVERLAY);
    }

    /**
     * Am I a shape collection
     *
     * @return is a shape collection
     */
    public boolean isShapes() {
        return type.equals(NODE_SHAPES);
    }



    /**
     * hash code
     *
     * @return hash code
     */
    public int hashCode() {
        return Misc.hashcode(type) ^ Misc.hashcode(name)
               ^ Misc.hashcode(displayCategories)  /*^ Misc.hashcode(docUrl)*/
               ^ Misc.hashcode(extra);
    }

    /**
     *  to string
     *
     * @return to string
     */
    public String toString() {
        return type + " " + name + " " + displayCategories;
    }

    /**
     * equals
     *
     * @param obj object
     *
     * @return is equals
     */
    public boolean equals(Object obj) {
        if ( !obj.getClass().equals(getClass())) {
            return false;
        }

        KmlId that = (KmlId) obj;
        return Misc.equals(this.type, that.type)
               && Misc.equals(this.name, that.name)
               && Misc.equals(this.displayCategories, that.displayCategories)
        //               && Misc.equals(this.docUrl, that.docUrl)
        && Misc.equals(this.extra, that.extra);
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
     * Set the DisplayCategories property.
     *
     * @param value The new value for DisplayCategories
     */
    public void setDisplayCategories(String value) {
        displayCategories = value;
    }

    /**
     * Get the DisplayCategories property.
     *
     * @return The DisplayCategories
     */
    public String getDisplayCategories() {
        return displayCategories;
    }

    /**
     * Set the DocUrl property.
     *
     * @param value The new value for DocUrl
     */
    public void setDocUrl(String value) {
        docUrl = value;
    }

    /**
     * Get the DocUrl property.
     *
     * @return The DocUrl
     */
    public String getDocUrl() {
        return docUrl;
    }

    /**
     * Set the Extra property.
     *
     * @param value The new value for Extra
     */
    public void setExtra(Object value) {
        extra = value;
    }

    /**
     * Get the Extra property.
     *
     * @return The Extra
     */
    public Object getExtra() {
        return extra;
    }


}

