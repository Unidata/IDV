/*
 * $Id: WmsHandler.java,v 1.53 2007/07/09 22:59:58 jeffmc Exp $
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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;


import ucar.unidata.data.DataSource;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.gis.WmsSelection;

import ucar.unidata.geoloc.*;




import ucar.unidata.idv.*;
import ucar.unidata.idv.control.WMSControl;



import ucar.unidata.ui.XmlTree;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import ucar.unidata.view.geoloc.NavigatedMapPanel;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;



/**
 * This handles capability xml document from web maps
 * servers (WMS). It is used by the {@link XmlChooser}
 *
 *
 * @author IDV development team
 * @version $Revision: 1.53 $Date: 2007/07/09 22:59:58 $
 */


public class WmsHandler extends XmlHandler {

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

    /** The map panel in the GUI */
    private NavigatedMapPanel mapPanel;


    /**
     * Major WMS specification version of the doc  we are looking
     * We will  handle both 1.0 and 1.1 (?)
     */
    int versionMajor = 1;

    /** Minor WMS specification version of the doc  we are looking */
    int versionMinor = 0;

    /** Should we merge the layers */
    JCheckBox mergeLayerCbx;

    /**
     * Create the handler
     *
     * @param chooser The chooser we are in
     * @param root The root of the xml tree
     * @param path The url path of the xml document
     *
     */
    public WmsHandler(XmlChooser chooser, Element root, String path) {
        super(chooser, root, path);
        String version = XmlUtil.getAttribute(root, ATTR_VERSION);
        List   tokens  = StringUtil.split(version, ".");
        if (tokens.size() > 0) {
            versionMajor = new Integer((String) tokens.get(0)).intValue();
        }
        if (tokens.size() > 1) {
            versionMinor = new Integer((String) tokens.get(1)).intValue();
        }
    }


    protected void updateStatus() {
        if(chooser.getHaveData()) {
            chooser.setStatus("Press \"" + chooser.CMD_LOAD + "\" to load the selected WMS layer", "buttons");
        } else {
            chooser.setStatus("Please select a WMS layer");
        }
    }


    /**
     * Called when the user clicks on a node in the XmlTre
     *
     * @param node The node the user clicked on
     */
    void doTreeClick(Element node) {
        boolean isLoadable = isLoadable(node);
        chooser.setHaveData(isLoadable);
        mapPanel.setDrawBounds(null, null);
        if (isLoadable) {
            Element bboxNode = findBbox(node);
            if (bboxNode != null) {
                //              System.err.println ("bbox:" + XmlUtil.toString(bboxNode));
                ProjectionRect rect =
                    new ProjectionRect(XmlUtil.getAttribute(bboxNode,
                        ATTR_MINX, -180.0), XmlUtil.getAttribute(bboxNode,
                            ATTR_MINY, -90.0), XmlUtil.getAttribute(bboxNode,
                                ATTR_MAXX,
                                180.0), XmlUtil.getAttribute(bboxNode,
                                    ATTR_MAXY, 90.0));

                mapPanel.setDrawBounds(getUpperLeft(rect),
                                       getLowerRight(rect));
            }
        }
    }

    /**
     * Get the upper left point
     *
     *
     * @param r The rect
     * @return Upper left
     */
    private LatLonPoint getUpperLeft(ProjectionRect r) {
        return new LatLonPointImpl(r.getMaxPoint().getY(),
                                   r.getMinPoint().getX());
    }

    /**
     * Get the lower right point
     *
     *
     * @param r The rect
     * @return Lower right
     */
    private LatLonPoint getLowerRight(ProjectionRect r) {
        return new LatLonPointImpl(r.getMinPoint().getY(),
                                   r.getMaxPoint().getX());
    }




    /**
     * Construct and return the UI component.
     * We use an {@link ucar.unidata.ui.XmlTree} to show
     * the XML and a
     * @link ucar.unidata.view.geoloc.NavigatedMapPanel}
     * to show the map. We use the map display to show the
     * bounding box of the layer the user has clicked on.
     *
     *  @return The UI component
     */
    protected JComponent doMakeContents() {
        mergeLayerCbx = new JCheckBox("Merge Layers", false);
        mapPanel      = new NavigatedMapPanel();
        mapPanel.setPreferredSize(new Dimension(250, 250));



        tree = new XmlTree(root, true, path) {
            public void doDoubleClick(XmlTree theTree, Element node) {
                processNode(Misc.newList(node));
            }

            public void doClick(XmlTree theTree, Element node) {
                doTreeClick(node);
            }

            public void doRightClick(XmlTree theTree, Element node,
                                     MouseEvent event) {
                JPopupMenu popup = new JPopupMenu();
                makePopupMenu(theTree, node, popup);
                popup.show((Component) event.getSource(), event.getX(),
                           event.getY());
            }

            protected boolean shouldRecurse(Element xmlNode) {
                String tagName = xmlNode.getTagName();
                if ( !tagName.equals(TAG_LAYER)) {
                    return super.shouldRecurse(xmlNode);
                }
                List styleChildren = XmlUtil.findChildren(xmlNode, TAG_STYLE);
                return (styleChildren.size() == 0)
                       || (styleChildren.size() > 1);
            }
        };
        tree.setMultipleSelect(true);
        tree.addTagsToProcess(Misc.newList(TAG_LAYER, TAG_STYLE));
        tree.addTagsToNotProcessButRecurse(Misc.newList(TAG_CAPABILITY,
                TAG_WMS1, TAG_WMS2));
        tree.defineLabelChild(TAG_LAYER, TAG_TITLE);
        tree.defineLabelChild(TAG_STYLE, TAG_TITLE);
        tree.defineTooltipChild(TAG_LAYER, TAG_ABSTRACT);
        tree.defineTooltipChild(TAG_STYLE, TAG_ABSTRACT);


        JComponent left = GuiUtils.topCenter(mergeLayerCbx,
                                             tree.getScroller());
        JComponent right = GuiUtils.topCenter(
                               GuiUtils.inset(
                                   GuiUtils.lLabel("Image Bounds:"),
                                   5), mapPanel);
        if (true) {
            return GuiUtils.hsplit(left, right, 250);
        }



        return GuiUtils.doLayout(new Component[] {
            GuiUtils.topCenter(mergeLayerCbx, tree.getScroller()),
            GuiUtils.topCenter(
                GuiUtils.inset(GuiUtils.lLabel("Image Bounds:"), 5),
                mapPanel) }, 2, GuiUtils.WT_Y, GuiUtils.WT_Y);
        /*
        JPanel mapWrapper = GuiUtils.wrap (mapPanel);
        return GuiUtils.doLayout (new Component[]{tree.getScroller(), new JLabel (" "), mapWrapper},1,GuiUtils.WT_Y,GuiUtils.WT_YNY);
        */
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
    private boolean isLoadable(Element node) {
        if (node.getTagName().equals(TAG_STYLE)) {
            return true;
        }
        //TODO: For now just return true 
        return true;
    }


    /**
     * Create the popup menu for xml node that the
     * user has right clicked on.
     *
     * @param theTree The XmlTree form the GUI
     * @param node The node clicked on
     * @param popup The menu to put items in
     */
    private void makePopupMenu(final XmlTree theTree, final Element node,
                               JPopupMenu popup) {
        JMenuItem mi;
        mi = new JMenuItem("Load");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                processNode(Misc.newList(node));
            }
        });
        popup.add(mi);
    }


    /**
     * Find the bbox element
     *
     * @param node xml node
     *
     * @return bbox node
     */
    private Element findBbox(Element node) {
        Element bboxNode = XmlUtil.findChildRecurseUp(node,
                               TAG_LATLONBOUNDINGBOX);

        if (bboxNode == null) {
            bboxNode = XmlUtil.findChildRecurseUp(node, TAG_BOUNDINGBOX);


        }
        return bboxNode;

    }


    /**
     * Extract the layer.style information from the
     * list of given nodes. Construct the WMS url
     * and pass create a WMS data source.
     *
     * @param selectedNodes List of selected Element nodes
     * from the XmlTree.
     */
    private void processNode(List selectedNodes) {

        if (selectedNodes.size() == 0) {
            return;
        }

        boolean mergeLayers = mergeLayerCbx.isSelected();

        String  format      = null;



        Element getMapNode = XmlUtil.findDescendantFromPath(root,
                                 "Capability.Request.GetMap");
        if (getMapNode == null) {
            getMapNode = XmlUtil.findDescendantFromPath(root,
                    "Capability.Request.Map");
        }


        if (getMapNode == null) {
            chooser.userMessage("No 'GetMap' section found");
            return;
        }
        //        System.err.println ("found:" + XmlUtil.toString (getMapNode));
        Element httpNode = XmlUtil.findDescendantFromPath(getMapNode,
                               "DCPType.HTTP");
        if (httpNode == null) {
            chooser.userMessage("No 'HTTP' section found");
            return;
        }

        List formats = XmlUtil.findChildren(getMapNode, "Format");
        for (int i = 0; (i < formats.size()) && (format == null); i++) {
            Element formatNode = (Element) formats.get(i);
            String  content    = XmlUtil.getChildText(formatNode);
            content = content.toLowerCase();
            if ((content.indexOf("image/png") >= 0)
                    || (content.indexOf("image/jpeg") >= 0)
                    || (content.indexOf("image/gif") >= 0)
                    || (content.indexOf("image/tiff") >= 0)) {
                format = content;
            } else {
                if (XmlUtil.findChildren(formatNode, "PNG").size() > 0) {
                    format = "PNG";
                } else if (XmlUtil.findChildren(formatNode, "JPEG").size()
                           > 0) {
                    format = "JPEG";
                } else if (XmlUtil.findChildren(formatNode, "GIF").size()
                           > 0) {
                    format = "GIF";
                }
            }
        }
        if (format == null) {
            chooser.userMessage("No compatible image format found");
            return;
        }


        Element getNode = XmlUtil.findDescendantFromPath(httpNode,
                              "Get.OnlineResource");
        if (getNode == null) {
            getNode = XmlUtil.findDescendantFromPath(httpNode, "Get");
        }
        Element postNode = XmlUtil.findDescendantFromPath(httpNode,
                               "Post.OnlineResource");
        if ((getNode == null) && (postNode == null)) {
            chooser.userMessage("No 'Get' or 'Post'  section found");
            return;
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
            chooser.userMessage("No 'href'  attribute found");
            return;
        }

        double       minx         = -180,
                     maxx         = 180,
                     miny         = -90,
                     maxy         = 90;
        int          fixedWidth   = -1;
        int          fixedHeight  = -1;
        boolean      allowSubsets = true;

        String       srsString    = null;
        String       error        = null;
        String       iconPath     = null;
        List         timeList     = null;

        String       version      = XmlUtil.getAttribute(root, "version");
        List         infos        = new ArrayList();
        WmsSelection wmsSelection = null;
        for (int i = 0; i < selectedNodes.size(); i++) {
            Element selectedNode = (Element) selectedNodes.get(i);
            //      System.err.println ("selected:" + XmlUtil.toString(selectedNode));
            if ( !isLoadable(selectedNode)) {
                continue;
            }
            String title = tree.getLabel(selectedNode);
            //      System.err.println ("style:" + XmlUtil.toString(styleNode));
            Element layerNode = selectedNode;
            Element styleNode;
            if (layerNode.getTagName().equals(TAG_STYLE)) {
                styleNode = layerNode;
                layerNode = (Element) layerNode.getParentNode();
            } else {
                styleNode = XmlUtil.findChild(layerNode, TAG_STYLE);
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
                                        TAG_DIMENSION, ATTR_NAME, VALUE_TIME);
            if (timeDimension != null) {
                String timeText = XmlUtil.getChildText(timeDimension);
                timeList = StringUtil.split(timeText, "/");
            }


            Element styleNameNode = XmlUtil.findChild(styleNode, "Name");
            if (styleNameNode == null) {
                error = "No Name element found in Style element";
                break;
            }


            Element nameNode = XmlUtil.getElement(layerNode, "Name");
            if (nameNode == null) {
                nameNode = XmlUtil.getElement(layerNode, "Title");
                if (nameNode == null) {
                    error = "No name node found";
                    break;
                }
            }

            //TODO: use the exceptions
            String style  = XmlUtil.getChildText(styleNameNode);
            String layer  = XmlUtil.getChildText(nameNode);

            List srsNodes = XmlUtil.findChildrenRecurseUp(layerNode, TAG_SRS);
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
                                    TAG_CRS);
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
            minx       = XmlUtil.getAttribute(bboxNode, ATTR_MINX, minx);
            maxx       = XmlUtil.getAttribute(bboxNode, ATTR_MAXX, maxx);
            miny       = XmlUtil.getAttribute(bboxNode, ATTR_MINY, miny);
            maxy       = XmlUtil.getAttribute(bboxNode, ATTR_MAXY, maxy);

            fixedWidth = XmlUtil.getAttribute(layerNode, ATTR_FIXEDWIDTH, -1);
            fixedHeight = XmlUtil.getAttribute(layerNode, ATTR_FIXEDHEIGHT,
                    -1);

            allowSubsets = (XmlUtil.getAttribute(layerNode, ATTR_NOSUBSETS,
                    0) != 1);


            if ( !mergeLayers) {
                wmsSelection = null;
            }

            if (wmsSelection == null) {
                wmsSelection = new WmsSelection(url, layer, title, srsString,
                        format, version,
                        new GeoLocationInfo(miny, maxx, maxy, minx));

                Element abstractNode = XmlUtil.getElement(layerNode,
                                           TAG_ABSTRACT);
                if (abstractNode != null) {
                    String text = XmlUtil.getChildText(abstractNode);
                    wmsSelection.setDescription(text);
                }

                wmsSelection.setTimeList(timeList);
                wmsSelection.setLegendIcon(iconPath);
                wmsSelection.setAllowSubsets(allowSubsets);
                wmsSelection.setFixedWidth(fixedWidth);
                wmsSelection.setFixedHeight(fixedHeight);
                infos.add(wmsSelection);
            } else {
                wmsSelection.appendLayer(layer,
                                         new GeoLocationInfo(miny, maxx,
                                             maxy, minx));
            }
        }

        if (error != null) {
            chooser.userMessage(error);
            return;
        }
        if (infos.size() == 0) {
            chooser.userMessage("None of the selected items are loadable");
            return;
        }


        String title = path;
        Element titleNode = XmlUtil.findDescendantFromPath(root,
                                "Service.Title");
        if (titleNode != null) {
            title = XmlUtil.getChildText(titleNode);
        }
        Hashtable properties = new Hashtable();
        properties.put(DataSource.PROP_TITLE, title);
        //  System.out.println (XmlUtil.toString(root));
        chooser.makeDataSource(infos, "WMS", properties);

        /*
        WMSControl control = new WMSControl(infos, "");
        ControlDescriptor wmsDescriptor =
            chooser.getIdv().getControlDescriptor("wmscontrol");
        wmsDescriptor.initControl(control, new ArrayList(), chooser.getIdv(),
                                  "", null);
        */

        chooser.closeChooser();
    }

    /**
     *  The user  has pressed the 'Load' button. Check if a  node is selected
     * and if so create the WMS data source.
     */
    public void doLoad() {
        processNode(tree.getSelectedElements());
    }


    /**
     * Humm??
     *
     * @param url Humm
     *
     * @throws Exception On badness
     */
    public static void convertToKml(String url) throws Exception {
        Element root = XmlUtil.getRoot(url, WmsHandler.class);
        System.err.println("root:" + XmlUtil.toString(root));
    }

    /**
     * test
     *
     * @param args args
     *
     * @throws Exception On badness
     */
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String url = args[i];
            convertToKml(url);
        }
    }
}

