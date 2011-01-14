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

import ucar.unidata.util.WmsUtil;

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
        String version = XmlUtil.getAttribute(root, WmsUtil.ATTR_VERSION);
        List   tokens  = StringUtil.split(version, ".");
        if (tokens.size() > 0) {
            versionMajor = new Integer((String) tokens.get(0)).intValue();
        }
        if (tokens.size() > 1) {
            versionMinor = new Integer((String) tokens.get(1)).intValue();
        }
    }


    /**
     * _more_
     */
    protected void updateStatus() {
        if (chooser.getHaveData()) {
            chooser.setStatus(
                "Press \"" + chooser.CMD_LOAD
                + "\" to load the selected WMS layer", "buttons");
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
        boolean isLoadable = WmsUtil.isLoadable(node);
        chooser.setHaveData(isLoadable);
        mapPanel.setDrawBounds(null, null);
        if (isLoadable) {
            Element bboxNode = WmsUtil.findBbox(node);
            if (bboxNode != null) {
                //              System.err.println ("bbox:" + XmlUtil.toString(bboxNode));
                ProjectionRect rect =
                    new ProjectionRect(XmlUtil.getAttribute(bboxNode,
                        WmsUtil.ATTR_MINX,
                        -180.0), XmlUtil.getAttribute(bboxNode,
                            WmsUtil.ATTR_MINY,
                            -90.0), XmlUtil.getAttribute(bboxNode,
                                WmsUtil.ATTR_MAXX,
                                180.0), XmlUtil.getAttribute(bboxNode,
                                    WmsUtil.ATTR_MAXY, 90.0));

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
                processNodes((List<Element>) Misc.newList(node));
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
                if ( !tagName.equals(WmsUtil.TAG_LAYER)) {
                    return super.shouldRecurse(xmlNode);
                }
                List styleChildren = XmlUtil.findChildren(xmlNode,
                                         WmsUtil.TAG_STYLE);
                return (styleChildren.size() == 0)
                       || (styleChildren.size() > 1);
            }
        };
        tree.setMultipleSelect(true);
        tree.addTagsToProcess(Misc.newList(WmsUtil.TAG_LAYER,
                                           WmsUtil.TAG_STYLE));
        tree.addTagsToNotProcessButRecurse(
            Misc.newList(
                WmsUtil.TAG_CAPABILITY, WmsUtil.TAG_WMS1, WmsUtil.TAG_WMS2));
        tree.defineLabelChild(WmsUtil.TAG_LAYER, WmsUtil.TAG_TITLE);
        tree.defineLabelChild(WmsUtil.TAG_STYLE, WmsUtil.TAG_TITLE);
        tree.defineTooltipChild(WmsUtil.TAG_LAYER, WmsUtil.TAG_ABSTRACT);
        tree.defineTooltipChild(WmsUtil.TAG_STYLE, WmsUtil.TAG_ABSTRACT);


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
                processNodes((List<Element>) Misc.newList(node));
            }
        });
        popup.add(mi);
    }




    /**
     * Extract the layer.style information from the
     * list of given nodes. Construct the WMS url
     * and pass create a WMS data source.
     *
     * @param selectedNodes List of selected Element nodes
     * from the XmlTree.
     */
    private void processNodes(List<Element> selectedNodes) {

        if (selectedNodes.size() == 0) {
            return;
        }

        boolean  mergeLayers = mergeLayerCbx.isSelected();
        String[] message     = { null };

        List<WmsSelection> infos = WmsUtil.processNode(root, selectedNodes,
                                       message, mergeLayers);
        if (message[0] != null) {
            chooser.userMessage(message[0]);
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
        processNodes((List<Element>) tree.getSelectedElements());
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
