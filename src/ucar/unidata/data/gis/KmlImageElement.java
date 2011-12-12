/*
 * $Id: KmlImageElement.java,v 1.12 2006/12/01 20:42:31 jeffmc Exp $
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
 * Class KmlImageElement represents a ground overlay in KML
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.12 $
 */
public abstract class KmlImageElement extends KmlInfo {

    /** xml tags */
    public static final String TAG_ICON = "Icon";


    /** xml tags */
    public static final String TAG_NAME = "name";

    /** xml tags */
    public static final String TAG_HREF = "href";

    /** url of the kml doc */
    private String baseUrl;

    /** name */
    private String name;

    /** url */
    private String href;


    /**
     * ctor
     */
    public KmlImageElement() {}



    /**
     * ctor
     *
     * @param node kml node
     * @param displayCategory display category
     * @param baseUrl url of the kml doc
     */
    public KmlImageElement(Element node, String displayCategory,
                            String baseUrl) {
        super(node, displayCategory, "RGBIMAGE");
        this.baseUrl = baseUrl;
        //      System.out.println ("node:" + XmlUtil.toString(node));

        Element iconNode = XmlUtil.findChild(node, TAG_ICON);
        href = XmlUtil.getChildText(XmlUtil.findChild(iconNode, TAG_HREF));


    }



    /**
     * Find the href element
     *
     * @param node node
     *
     * @return href
     */
    public static String getHref(Element node) {
        Element iconNode = XmlUtil.findChild(node, TAG_ICON);
        return XmlUtil.getChildText(XmlUtil.findChild(iconNode, TAG_HREF));
    }

    /**
     * get the image data
     *
     * @param dataSource data source
     *
     * @return image data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Image  getImage(KmlDataSource dataSource) 
            throws VisADException, RemoteException {
        return  dataSource.readImage(getHref(), baseUrl);
    }



    /**
     * Set the Href property.
     *
     * @param value The new value for Href
     */
    public void setHref(String value) {
        href = value;
    }

    /**
     * Get the Href property.
     *
     * @return The Href
     */
    public String getHref() {
        return href;
    }


/**
Set the BaseUrl property.

@param value The new value for BaseUrl
**/
public void setBaseUrl (String value) {
	baseUrl = value;
}

/**
Get the BaseUrl property.

@return The BaseUrl
**/
public String getBaseUrl () {
	return baseUrl;
}



}

