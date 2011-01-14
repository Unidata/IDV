/*
 * $Id: KmlPolygons.java,v 1.8 2006/12/01 20:42:31 jeffmc Exp $
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


import ucar.unidata.metdata.NamedStationTable;


import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
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
import java.util.Hashtable;


import java.util.List;



/**
 * Class KmlPolygons represents polygons from KML
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.8 $
 */
public class KmlPolygons extends KmlInfo {

    /** xml tag */
    public static final String TAG_POLYGON = "Polygon";

    /** xml tag */
    public static final String TAG_OUTERBOUNDARYIS = "outerBoundaryIs";

    /** xml tag */
    public static final String TAG_LINEARRING = "LinearRing";


    /**
     * ctor
     */
    public KmlPolygons() {}


    /**
     * ctor
     *
     * @param node node
     * @param displayCategory the category of display
     */
    public KmlPolygons(Element node, String displayCategory) {
        super(node, displayCategory, "xgrf");
    }


    /**
     * Create the data
     *
     * @param dataSource from
     * @param loadId For loading
     *
     * @return The data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public Data getData(KmlDataSource dataSource, Object loadId)
            throws VisADException, RemoteException {
        StringBuffer sb = new StringBuffer("<shapes>\n");
        Element multiGeometryNode = (Element) XmlUtil.findChild(node,
                                        KmlDataSource.TAG_MULTIGEOMETRY);
        Element linestringNode = 
            (Element) XmlUtil.findChild(node, KmlDataSource.TAG_LINESTRING);
        if (linestringNode != null) {
            processPolygonNode(linestringNode, sb);
        } else   if (multiGeometryNode != null) {
            NodeList children = XmlUtil.getElements(multiGeometryNode);
            for (int childIdx = 0; childIdx < children.getLength();
                    childIdx++) {
                Element child = (Element) children.item(childIdx);
                if (child.getTagName().equals(TAG_POLYGON)) {
                    processPolygonNode(child, sb);
                }
            }
        } else {
            Element polygonNode = (Element) XmlUtil.findChild(node,
                                      TAG_POLYGON);
            if (polygonNode != null) {
                processPolygonNode(polygonNode, sb);
            } else {
                processPolygonNode(node, sb);
            }
        }


        sb.append("</shapes>");
        return new visad.Text(sb.toString());
    }


    /**
     * Process the xml
     *
     * @param node xml node
     * @param sb for writing
     */
    private void processPolygonNode(Element node, StringBuffer sb) {
        String attrs = XmlUtil.attrs("smooth", "false", "filled", "false",
                                     "color", "255,0,0", "coordtype",
                                     "LATLONALT");
        sb.append("<polygon " + attrs);

        sb.append("points=\"");
        Element coordNode = XmlUtil.findDescendantFromPath(node,
                                TAG_OUTERBOUNDARYIS + "." + TAG_LINEARRING
                                + "." + TAG_COORDINATES);
        if (coordNode == null) {
            coordNode = XmlUtil.findDescendantFromPath(node,
                    TAG_LINESTRING + "." + TAG_COORDINATES);
        }
        if (coordNode == null) {
            coordNode = XmlUtil.findDescendantFromPath(node,
                                                       TAG_COORDINATES);
        }
        if (coordNode == null) {
            System.err.println("Could not find coord node: "
                               + XmlUtil.toString(node));
        } else {
            String     coordText = XmlUtil.getChildText(coordNode);
            double[][] coords    = KmlUtil.parseCoordinates(coordText);
            for (int coordIdx = 0; coordIdx < coords[0].length; coordIdx++) {
                if (coordIdx != 0) {
                    sb.append(",");
                }
                if(coords.length==3)
                    sb.append(coords[1][coordIdx] + ","
                              + coords[0][coordIdx] + ","
                              + 0.3048 * coords[2][coordIdx]);
                else
                    sb.append(coords[1][coordIdx] + ","
                              + coords[0][coordIdx]);
            }
        }
        sb.append("\" ");
        sb.append("/>");
    }



}

