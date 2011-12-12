/*
 * $Id: KmlInfo.java,v 1.11 2006/12/01 20:42:31 jeffmc Exp $
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

import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;

import ucar.unidata.data.grid.GridUtil;


import ucar.unidata.data.imagery.ImageXmlDataSource;


import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;



import visad.*;

import visad.util.DataUtility;

import java.awt.*;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;



/**
 * Class KmlInfo holds information about a kml node or group of nodes
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.11 $
 */
public abstract class KmlInfo {

    /** xml tag */
    public static final String TAG_LINESTRING = "LineString";

    /** Address tag */
    public static final String TAG_ADDRESS = "address";


    /** xml tags */
    public static final String TAG_DESCRIPTION = "description";

    /** kml tag */
    public static final String TAG_COORDINATES = "coordinates";

    /** kml tag */
    public static final String TAG_NAME = "name";

    /** categories */
    List categories;

    /** My name */
    private String name;

    /** The node I reference */
    Element node;

    /** My display category */
    String displayCategory;

    /**
     * ctor
     */
    public KmlInfo() {}

    /**
     * ctor
     *
     * @param node kml node
     * @param displayCategory category
     * @param dataCategory data category
     */
    public KmlInfo(Element node, String displayCategory,
                   String dataCategory) {
        this(XmlUtil.getChildText(XmlUtil.findChild(node, TAG_NAME)),
             displayCategory, dataCategory);
        this.node = node;
    }


    /**
     * name
     *
     * @param name name
     * @param displayCategory category
     * @param dataCategory data category
     */
    public KmlInfo(String name, String displayCategory, String dataCategory) {
        this.name = name;
        if (name == null) {
            name = displayCategory;
        }
        this.displayCategory = displayCategory;
        if (displayCategory.length() > 0) {
            categories = DataCategory.parseCategories(displayCategory + ";"
                    + dataCategory, true);
        } else {
            categories = DataCategory.parseCategories(dataCategory, false);
        }

    }



    /**
     * get the categories
     *
     * @return categories
     */
    public List getCategories() {
        return categories;
    }


    /**
     * Abstract method that returns the visad data
     *
     * @param dataSource the source
     * @param loadId to stop load
     *
     * @return visad data this kml info represents
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public abstract Data getData(KmlDataSource dataSource, Object loadId)
     throws VisADException, RemoteException;

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

}

