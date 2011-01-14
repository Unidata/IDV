/**
 * $Id: KmlPoints.java,v 1.18 2006/12/01 20:42:31 jeffmc Exp $
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
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;



import visad.*;

import visad.georef.LatLonPoint;

import visad.util.DataUtility;

import java.awt.*;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;


import java.util.List;



/**
 * Class KmlPoints holds point info from KML
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.18 $
 */
public class KmlPoints extends KmlInfo {

    /** xml tags */
    public static final String TAG_NAME = "name";

    /** xml tags */
    public static final String TAG_POINT = "Point";

    /** xml tags */
    public static final String TAG_COORDINATES = "coordinates";


    /** the point names */
    List names = new ArrayList();

    /** The descirptions of the placemarks */
    List descriptions = new ArrayList();

    /** the point locations */
    List coordinates = new ArrayList();

    /**
     * ctor
     */
    public KmlPoints() {}


    /**
     * ctor_
     *
     * @param nodes xml nodes
     * @param displayCategory category
     */
    public KmlPoints(List nodes, String displayCategory) {
        super(displayCategory, displayCategory, "locations");
        for (int i = 0; i < nodes.size(); i++) {
            Element node      = (Element) nodes.get(i);
            Element pointNode = XmlUtil.findChild(node, TAG_POINT);

            String name = XmlUtil.getChildText(XmlUtil.findChild(node,
                              TAG_NAME));
            if (name == null) {
                name = "";
            }
            if (pointNode == null) {
                pointNode = XmlUtil.findChild(node, TAG_LINESTRING);
            }

            if (pointNode == null) {
                pointNode = XmlUtil.findDescendantFromPath(node,
                        "Polygon.outerBoundaryIs.LinearRing");
            }
            if (pointNode == null) {
                pointNode = XmlUtil.findDescendant(node, TAG_POINT);
            }


            double[][] coords = null;
            if (pointNode == null) {
                Element addressNode = XmlUtil.findChild(node, TAG_ADDRESS);
                if (addressNode == null) {
                    System.err.println("Could not  find point node:"
                                       + XmlUtil.toString(node));
                    continue;
                }
                String address = XmlUtil.getChildText(addressNode);
                LogUtil.message("KML: Fetching address: " + address);
                LatLonPoint llp =
                    ucar.visad.GeoUtils.getLocationFromAddress(address, null);
                LogUtil.message("");
                if (llp == null) {
                    System.err.println("Could not  find address:" + address);
                    continue;
                }
                coords = new double[][] {
                    { llp.getLongitude().getValue() },
                    { llp.getLatitude().getValue() }, { 0.0 }
                };
            }


            if (coords == null) {
                Element coordNode = XmlUtil.findChild(pointNode,
                                        TAG_COORDINATES);
                if (coordNode == null) {
                    continue;
                }
                try {
                    coords = StringUtil.parseCoordinates(
                        XmlUtil.getChildText(coordNode));
                } catch (Exception exc) {
                    System.err.println("Error parsing coordinates:" + exc);
                    System.err.println(XmlUtil.toString(node));
                    continue;
                }
            }
            descriptions.add(XmlUtil.getChildText(XmlUtil.findChild(node,
                    TAG_DESCRIPTION)));



            names.add(name);
            coordinates.add(coords);

        }
    }


    /**
     * get the data
     *
     * @param dataSource data source
     * @param loadId for loading
     *
     * @return the point data as xml
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data getData(KmlDataSource dataSource, Object loadId)
            throws VisADException, RemoteException {
        Trace.call1("KmlPoints.getData", " #placemarks:" + names.size());
        StringBuffer sb = new StringBuffer(XmlUtil.XML_HEADER
                                           + "\n<stationtable name=\""
                                           + getName() + "\">\n");
        int size = names.size();
        for (int i = 0; i < size; i++) {
            double[][] coords = (double[][]) coordinates.get(i);
            String     desc   = (String) descriptions.get(i);
            sb.append("<station ");
            attr(sb, "name", (String) names.get(i));
            if (coords[0].length == 1) {
                attr(sb, "lon", "" + coords[0][0]);
                attr(sb, "lat", "" + coords[1][0]);
                if (coords.length > 2) {
                    attr(sb, "elev", "" + coords[2][0]);
                }
                sb.append(">");
            } else {
                sb.append(">");
                sb.append("<coordinates>");

                for (int coordIdx = 0; coordIdx < coords[0].length;
                        coordIdx++) {
                    sb.append(coords[0][coordIdx]);
                    sb.append(",");
                    sb.append(coords[1][coordIdx]);
                    if (coords.length > 2) {
                        sb.append(",");
                        sb.append(coords[2][coordIdx]);
                    }
                    sb.append(" ");

                }
                sb.append("</coordinates>");
            }
            if (desc != null) {
                sb.append("<![CDATA[");
                sb.append(desc);
                sb.append("]]>");
            }
            sb.append("</station>");
        }
        sb.append("</stationtable>");
        Trace.call2("KmlPoints.getData");
        return new visad.Text(sb.toString());
    }


    /**
     * Utility to add an xml attribute to the buffer
     *
     * @param sb buffer
     * @param name name
     * @param value value
     */
    private void attr(StringBuffer sb, String name, String value) {
        sb.append(name);
        sb.append("=");
        sb.append(XmlUtil.quote(XmlUtil.encodeString(value)));
        sb.append(" ");
    }

    /**
     * Set the Names property.
     *
     * @param value The new value for Names
     */
    public void setNames(List value) {
        names = value;
    }

    /**
     * Get the Names property.
     *
     * @return The Names
     */
    public List getNames() {
        return names;
    }

    /**
     * Set the Coordinates property.
     *
     * @param value The new value for Coordinates
     */
    public void setCoordinates(List value) {
        coordinates = value;
    }

    /**
     * Get the Coordinates property.
     *
     * @return The Coordinates
     */
    public List getCoordinates() {
        return coordinates;
    }




}

