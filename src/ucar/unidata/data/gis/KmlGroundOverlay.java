/*
 * $Id: KmlGroundOverlay.java,v 1.12 2006/12/01 20:42:31 jeffmc Exp $
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
 * Class KmlGroundOverlay represents a ground overlay in KML
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.12 $
 */
public class KmlGroundOverlay extends KmlImageElement {


    /** xml tags */
    public static final String TAG_VIEWBOUNDSCALE = "viewBoundScale";

    /** xml tags */
    public static final String TAG_LATLONBOX = "LatLonBox";

    /** xml tags */
    public static final String TAG_NORTH = "north";

    /** xml tags */
    public static final String TAG_SOUTH = "south";

    /** xml tags */
    public static final String TAG_EAST = "east";

    /** xml tags */
    public static final String TAG_WEST = "west";

    /** xml tags */
    public static final String TAG_ROTATION = "rotation";

    /** scale */
    private double viewBoundScale = 1.0;

    /** properties from kml */
    private double rotation;

    /** properties from kml */
    private double north;

    /** properties from kml */
    private double south;

    /** properties from kml */
    private double east;

    /** properties from kml */
    private double west;

    /**
     * ctor
     */
    public KmlGroundOverlay() {}



    /**
     * ctor
     *
     * @param node kml node
     * @param displayCategory display category
     * @param baseUrl url of the kml doc
     */
    public KmlGroundOverlay(Element node, String displayCategory,
                            String baseUrl) {
        super(node, displayCategory, baseUrl);

        Element iconNode = XmlUtil.findChild(node, TAG_ICON);
        Element tmpNode = XmlUtil.findChild(iconNode, TAG_VIEWBOUNDSCALE);
        if (tmpNode != null) {
            viewBoundScale =
                new Double(XmlUtil.getChildText(tmpNode)).doubleValue();
        }



        Element latLonNode = XmlUtil.findChild(node, TAG_LATLONBOX);
        north = new Double(XmlUtil.getChildText(XmlUtil.findChild(latLonNode,
                                                                      TAG_NORTH))).doubleValue();
            south = new Double(XmlUtil.getChildText(XmlUtil.findChild(latLonNode,
                                                                      TAG_SOUTH))).doubleValue();
            east = new Double(XmlUtil.getChildText(XmlUtil.findChild(latLonNode,
                                                                     TAG_EAST))).doubleValue();
            west = new Double(XmlUtil.getChildText(XmlUtil.findChild(latLonNode,
                                                                     TAG_WEST))).doubleValue();
    }




    /**
     * get the image data
     *
     * @param dataSource data source
     * @param loadId for loading
     *
     * @return image data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data getData(KmlDataSource dataSource, Object loadId)
            throws VisADException, RemoteException {
        Image image = getImage(dataSource);
        if (image == null) {
            return null;
        }
        Trace.call1("makeField");
        try {
            //            FieldImpl xyData = DataUtility.makeField(image);
            //Threshold alpha at 127
            FieldImpl xyData = ucar.visad.Util.makeField(image,127f);
            Trace.call2("makeField");
            Linear2DSet domain = (Linear2DSet) xyData.getDomainSet();
            Linear2DSet imageDomain =
                new Linear2DSet(RealTupleType.SpatialEarth2DTuple, getWest(),
                                getEast(), domain.getX().getLength(),
                                getNorth(), getSouth(),
                                domain.getY().getLength());



            FieldImpl field = GridUtil.setSpatialDomain(xyData, imageDomain,
                                  true);

            return field;
        } catch (Exception iexc) {
            LogUtil.logException("Error making image data", iexc);
        }
        return null;


    }


    /**
     * Set the ViewBoundScale property.
     *
     * @param value The new value for ViewBoundScale
     */
    public void setViewBoundScale(double value) {
        viewBoundScale = value;
    }

    /**
     * Get the ViewBoundScale property.
     *
     * @return The ViewBoundScale
     */
    public double getViewBoundScale() {
        return viewBoundScale;
    }

    /**
     * Set the Rotation property.
     *
     * @param value The new value for Rotation
     */
    public void setRotation(double value) {
        rotation = value;
    }

    /**
     * Get the Rotation property.
     *
     * @return The Rotation
     */
    public double getRotation() {
        return rotation;
    }

    /**
     * Set the North property.
     *
     * @param value The new value for North
     */
    public void setNorth(double value) {
        north = value;
    }

    /**
     * Get the North property.
     *
     * @return The North
     */
    public double getNorth() {
        return north;
    }

    /**
     * Set the South property.
     *
     * @param value The new value for South
     */
    public void setSouth(double value) {
        south = value;
    }

    /**
     * Get the South property.
     *
     * @return The South
     */
    public double getSouth() {
        return south;
    }

    /**
     * Set the East property.
     *
     * @param value The new value for East
     */
    public void setEast(double value) {
        east = value;
    }

    /**
     * Get the East property.
     *
     * @return The East
     */
    public double getEast() {
        return east;
    }

    /**
     * Set the West property.
     *
     * @param value The new value for West
     */
    public void setWest(double value) {
        west = value;
    }

    /**
     * Get the West property.
     *
     * @return The West
     */
    public double getWest() {
        return west;
    }



}

