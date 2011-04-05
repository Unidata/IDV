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
 * 
 */

package ucar.unidata.util;


import org.w3c.dom.Element;

import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.WmsSelection;

import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 *
 * @author IDV development team
 * @version $Revision: 1.53 $Date: 2007/07/09 22:59:58 $
 */


public class WmsUtil {

    /** XML tag name for the &quot;Abstract&quot; tag */
    public static final String TAG_ABSTRACT = "Abstract";

    /** XML tag name for the &quot;Dimension&quot; tag */
    public static final String TAG_DIMENSION = "Dimension";

    /** XML tag name for the &quot;Layer&quot; tag */
    public static final String TAG_LAYER = "Layer";

    /** xml tag name */
    public static final String TAG_LATLONBOUNDINGBOX = "LatLonBoundingBox";

    /** xml tag name */
    public static final String TAG_BOUNDINGBOX = "BoundingBox";

    /** xml tag name */
    public static final String TAG_SRS = "SRS";

    /** _more_          */
    public static final String TAG_NAME = "Name";

    /** xml tag name */
    public static final String TAG_CRS = "CRS";

    /** XML tag name for the &quot;Title&quot; tag */
    public static final String TAG_TITLE = "Title";

    /** XML tag name for the &quot;Style&quot; tag */
    public static final String TAG_STYLE = "Style";

    /** XML tag name for the &quot;Capability&quot; tag */
    public static final String TAG_CAPABILITY = "Capability";

    /**
     * This is one of the root document xml tags that I have seen
     * for a WMS cababilities document
     */
    public static final String TAG_WMS1 = "WMT_MS_Capabilities";

    /**
     * This is the ther root document xml tags that I have seen
     * for a WMS cababilities document
     */
    public static final String TAG_WMS2 = "WMS_Capabilities";

    /** xml attribute name */
    public static final String ATTR_FIXEDWIDTH = "fixedWidth";

    /** xml attribute name */
    public static final String ATTR_OPAQUE = "opaque";

    /** xml attribute name */
    public static final String ATTR_VERSION = "version";

    /** xml attribute name */
    public static final String ATTR_FIXEDHEIGHT = "fixedHeight";

    /** xml attribute name */
    public static final String ATTR_NAME = "name";


    /** xml attribute name */
    public static final String ATTR_NOSUBSETS = "noSubsets";

    /** xml attribute name */
    public static final String ATTR_MINX = "minx";

    /** xml attribute name */
    public static final String ATTR_MAXX = "maxx";

    /** xml attribute name */
    public static final String ATTR_MINY = "miny";

    /** xml attribute name */
    public static final String ATTR_MAXY = "maxy";


    /** xml attribute value */
    public static final String VALUE_TIME = "time";

    /**
     * _more_
     *
     * @param root _more_
     * @param selectedNodes _more_
     * @param message _more_
     * @param mergeLayers _more_
     *
     * @return _more_
     */
    public static List<WmsSelection> processNode(Element root,
            List selectedNodes, String[] message, boolean mergeLayers) {

        String format = null;
        Element getMapNode = XmlUtil.findDescendantFromPath(root,
                                 "Capability.Request.GetMap");
        if (getMapNode == null) {
            getMapNode = XmlUtil.findDescendantFromPath(root,
                    "Capability.Request.Map");
        }


        if (getMapNode == null) {
            message[0] = "No 'GetMap' section found";
            return null;
        }
        //        System.err.println ("found:" + XmlUtil.toString (getMapNode));
        Element httpNode = XmlUtil.findDescendantFromPath(getMapNode,
                               "DCPType.HTTP");
        if (httpNode == null) {
            message[0] = "No 'HTTP' section found";
            return null;
        }

        List formatNodes = XmlUtil.findChildren(getMapNode, "Format");
        Hashtable<String, String> formatMap = new Hashtable<String, String>();
        List                      formats   = new ArrayList();
        for (int i = 0; (i < formatNodes.size()) && (format == null); i++) {
            Element formatNode = (Element) formatNodes.get(i);
            String  content = XmlUtil.getChildText(formatNode).toLowerCase();
            formats.add(content);
            formatMap.put(content, content);
        }


        if (format == null) {
            format = formatMap.get("image/png; mode=24bit");
        }
        if (format == null) {
            format = formatMap.get("image/png");
        }
        if (format == null) {
            format = formatMap.get("image/jpeg");
        }
        if (format == null) {
            format = formatMap.get("image/gif");
        }
        //        if(format ==null)
        //            format = formatMap.get("image/tiff");
        //        System.err.println("format:" + format);
        if (format == null) {
            for (int i = 0; (i < formatNodes.size()) && (format == null);
                    i++) {
                Element formatNode = (Element) formatNodes.get(i);
                if (XmlUtil.findChildren(formatNode, "PNG").size() > 0) {
                    format = "PNG";
                    break;
                } else if (XmlUtil.findChildren(formatNode, "JPEG").size()
                           > 0) {
                    format = "JPEG";
                    break;
                } else if (XmlUtil.findChildren(formatNode, "GIF").size()
                           > 0) {
                    format = "GIF";
                    break;
                }
            }
        }

        if (format == null) {
            message[0] = "No compatible image format found";
            return null;
        }


        Element getNode = XmlUtil.findDescendantFromPath(httpNode,
                              "Get.OnlineResource");
        if (getNode == null) {
            getNode = XmlUtil.findDescendantFromPath(httpNode, "Get");
        }
        Element postNode = XmlUtil.findDescendantFromPath(httpNode,
                               "Post.OnlineResource");
        if ((getNode == null) && (postNode == null)) {
            message[0] = "No 'Get' or 'Post'  section found";
            return null;
        }
        boolean doGet = (getNode != null);

        //TODO: If we are using POST then we need to pass that info on to the 
        //data source
        if (getNode == null) {
            getNode = postNode;
        }
        String url = XmlUtil.getAttribute(getNode, "xlink:href",
                                          (String) null);
        if (url == null) {
            url = XmlUtil.getAttribute(getNode, "onlineResource",
                                       (String) null);
        }
        if (url == null) {
            message[0] = "No 'href'  attribute found";
            return null;
        }

        double             minx         = -180,
                           maxx         = 180,
                           miny         = -90,
                           maxy         = 90;
        int                opaque   = 0;
        int                fixedWidth   = -1;
        int                fixedHeight  = -1;
        boolean            allowSubsets = true;

        String             srsString    = null;
        String             error        = null;
        String             iconPath     = null;
        List               timeList     = null;

        String             version = XmlUtil.getAttribute(root, "version");
        List<WmsSelection> infos        = new ArrayList<WmsSelection>();
        WmsSelection       wmsSelection = null;
        for (int i = 0; i < selectedNodes.size(); i++) {
            Element selectedNode = (Element) selectedNodes.get(i);
            //      System.err.println ("selected:" + XmlUtil.toString(selectedNode));
            if ( !isLoadable(selectedNode)) {
                continue;
            }
            String title = getLabel(selectedNode);
            //      System.err.println ("style:" + XmlUtil.toString(styleNode));
            Element layerNode = selectedNode;
            Element styleNode;
            if (layerNode.getTagName().equals(WmsUtil.TAG_STYLE)) {
                styleNode = layerNode;
                layerNode = (Element) layerNode.getParentNode();
            } else {
                styleNode = XmlUtil.findChild(layerNode, WmsUtil.TAG_STYLE);
            }
            if (styleNode == null) {
                styleNode = layerNode;
            }

            Element iconElement = XmlUtil.findDescendantFromPath(styleNode,
                                      "LegendURL.OnlineResource");
            if (iconElement != null) {
                iconPath = XmlUtil.getAttribute(iconElement, "xlink:href",
                        (String) null);
            }


            //<Dimension name="time" units="ISO8601" default="2003-06-22T00:56Z">2003-06-20T02:44Z/2003-06-22T00:56Z/PT1H39M</Dimension>
            Element timeDimension = XmlUtil.findElement(styleNode,
                                        WmsUtil.TAG_DIMENSION,
                                        WmsUtil.ATTR_NAME,
                                        WmsUtil.VALUE_TIME);
            if (timeDimension != null) {
                String timeText = XmlUtil.getChildText(timeDimension);
                timeList = StringUtil.split(timeText, "/");
            }


            Element styleNameNode = XmlUtil.findChild(styleNode, TAG_NAME);
            if (styleNameNode == null) {
                error = "No Name element found in Style element";
                System.err.println(XmlUtil.toString(styleNode).substring(0,
                        300));
                break;
            }


            Element nameNode = XmlUtil.getElement(layerNode, TAG_NAME);
            if (nameNode == null) {
                nameNode = XmlUtil.getElement(layerNode, "Title");
                if (nameNode == null) {
                    error = "No name node found";
                    break;
                }
            }

            //TODO: use the exceptions
            String style = XmlUtil.getChildText(styleNameNode);
            String layer = XmlUtil.getChildText(nameNode);

            List srsNodes = XmlUtil.findChildrenRecurseUp(layerNode,
                                WmsUtil.TAG_SRS);
            for (int srsIdx = 0;
                    (srsIdx < srsNodes.size()) && (srsString == null);
                    srsIdx++) {
                String content =
                    XmlUtil.getChildText((Element) srsNodes.get(srsIdx));
                if (content == null) {
                    continue;
                }
                List srsTokens = StringUtil.split(content, " ", true, true);
                if (srsTokens.size() == 0) {
                    srsString = "EPSG:4326";
                }
                for (int srsTokenIdx = 0; srsTokenIdx < srsTokens.size();
                        srsTokenIdx++) {
                    String token = (String) srsTokens.get(srsTokenIdx);
                    //For now just look for epsg:4326
                    if (token.equalsIgnoreCase("EPSG:4326")) {
                        srsString = token;
                        break;
                    }
                }
            }

            if (srsString == null) {
                List crsNodes = XmlUtil.findChildrenRecurseUp(layerNode,
                                    WmsUtil.TAG_CRS);
                for (int crsIdx = 0;
                        (crsIdx < crsNodes.size()) && (srsString == null);
                        crsIdx++) {
                    String content =
                        XmlUtil.getChildText((Element) crsNodes.get(crsIdx));
                    if (content == null) {
                        continue;
                    }
                    List crsTokens = StringUtil.split(content, " ", true,
                                         true);
                    for (int crsTokenIdx = 0; crsTokenIdx < crsTokens.size();
                            crsTokenIdx++) {
                        String token = (String) crsTokens.get(crsTokenIdx);
                        //For now just look for crs:84
                        if (token.equalsIgnoreCase("CRS:84")) {
                            srsString = token;
                            break;
                        }
                    }
                }
            }



            if (srsString == null) {
                error = "No compatible SRS found";
                break;
            }
            Element bboxNode = findBbox(layerNode);
            if (bboxNode == null) {
                error = "No bbox node found";
                break;
            }
            minx = XmlUtil.getAttribute(bboxNode, WmsUtil.ATTR_MINX, minx);
            maxx = XmlUtil.getAttribute(bboxNode, WmsUtil.ATTR_MAXX, maxx);
            miny = XmlUtil.getAttribute(bboxNode, WmsUtil.ATTR_MINY, miny);
            maxy = XmlUtil.getAttribute(bboxNode, WmsUtil.ATTR_MAXY, maxy);

            opaque = XmlUtil.getAttribute(layerNode,
                    WmsUtil.ATTR_OPAQUE, 0);
            fixedWidth = XmlUtil.getAttribute(layerNode,
                    WmsUtil.ATTR_FIXEDWIDTH, -1);
            fixedHeight = XmlUtil.getAttribute(layerNode,
                    WmsUtil.ATTR_FIXEDHEIGHT, -1);

            allowSubsets = (XmlUtil.getAttribute(layerNode,
                    WmsUtil.ATTR_NOSUBSETS, 0) != 1);


            if ( !mergeLayers) {
                wmsSelection = null;
            }

            if (wmsSelection == null) {
                wmsSelection = new WmsSelection(url, layer, title, srsString,
                        format, version,
                        new GeoLocationInfo(miny, maxx, maxy, minx));

                Element abstractNode = XmlUtil.getElement(layerNode,
                                           WmsUtil.TAG_ABSTRACT);
                if (abstractNode != null) {
                    String text = XmlUtil.getChildText(abstractNode);
                    wmsSelection.setDescription(text);
                }

                wmsSelection.setTimeList(timeList);
                wmsSelection.setLegendIcon(iconPath);
                wmsSelection.setAllowSubsets(allowSubsets);
                wmsSelection.setFixedWidth(fixedWidth);
                wmsSelection.setFixedHeight(fixedHeight);
                wmsSelection.setOpaque(opaque);
                infos.add(wmsSelection);
            } else {
                wmsSelection.appendLayer(layer,
                                         new GeoLocationInfo(miny, maxx,
                                             maxy, minx));
            }
        }


        if (error != null) {
            message[0] = error;
            return null;
        }
        if (infos.size() == 0) {
            message[0] = "None of the selected items are loadable";
            return null;
        }
        return infos;



    }

    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     */
    public static String getLabel(Element node) {
        String tag   = node.getTagName();
        String label = null;
        if (tag.equals(WmsUtil.TAG_LAYER) || tag.equals(WmsUtil.TAG_STYLE)) {
            label = XmlUtil.getGrandChildText(node, WmsUtil.TAG_TITLE);
            return label;
        }
        return XmlUtil.getLocalName(node);
    }

    /**
     * The given node is an Xml node that the user has clicked
     * on. This method determines if the node is loadable, i.e.,
     * if it is a layer or a style node that also has a
     * loadable=1 attribute.
     *
     * @param node The node to check
     * @return Is the node loadable
     */
    public static boolean isLoadable(Element node) {
        if (node.getTagName().equals(WmsUtil.TAG_STYLE)) {
            return true;
        }
        //TODO: For now just return true 
        return true;
    }

    /**
     * Find the bbox element
     *
     * @param node xml node
     *
     * @return bbox node
     */
    public static Element findBbox(Element node) {
        Element bboxNode = XmlUtil.findChildRecurseUp(node,
                               WmsUtil.TAG_LATLONBOUNDINGBOX);

        if (bboxNode == null) {
            bboxNode = XmlUtil.findChildRecurseUp(node,
                    WmsUtil.TAG_BOUNDINGBOX);


        }
        return bboxNode;

    }


}
