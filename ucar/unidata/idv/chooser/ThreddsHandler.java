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
 */

package ucar.unidata.idv.chooser;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;




import ucar.unidata.idv.*;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.XmlTree;
import ucar.unidata.util.CatalogUtil;


import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlNodeList;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;

import java.awt.event.*;

import java.io.File;




import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;


/**
 * This handles the Thredds  catalog xml for the
 * {@link XmlChooser}.
 *
 * @author IDV development team
 * @version $Revision: 1.68 $Date: 2007/07/09 22:59:58 $
 */

public class ThreddsHandler extends XmlHandler {



    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ThreddsHandler.class.getName());

    /** More clear than  then doing (String)null */
    public static final String NULL_STRING = null;



    /** for gui */
    private JComboBox dataSourcesCbx;

    /** for gui */
    private JCheckBox loadIndividuallyCbx;

    /** show the thumbnail checkbox */
    private JCheckBox showThumbsCbx;

    /**
     * Create the handler
     *
     * @param chooser The chooser we are in
     * @param root The root of the xml tree
     * @param path The url path of the xml document
     *
     */
    public ThreddsHandler(XmlChooser chooser, Element root, String path) {
        super(chooser, root, path);
        //Add the catalog url attribute
        root.setAttribute(CatalogUtil.ATTR_CATALOGURL, path);
    }

    /**
     * Update the status
     */
    protected void updateStatus() {
        if (chooser.getHaveData()) {
            chooser.setStatus("Press \"" + chooser.CMD_LOAD
                              + "\" to load the selected data", "buttons");
        } else {
            chooser.setStatus("Please select a dataset from the catalog");
        }
    }


    /**
     * For the given documentation  node return the label that we use (eg:, Summary, rights, etc)
     *
     * @param node The documentation node
     *
     * @return The label to use
     */
    protected String getDocumentationLabel(Element node) {
        String type = XmlUtil.getAttribute(node, CatalogUtil.ATTR_TYPE, "");
        if (type.equals(CatalogUtil.VALUE_SUMMARY)) {
            String text = XmlUtil.getChildText(node);
            if ((text != null) && (text.length() < 50)) {
                return "Summary: " + text;
            }
            return "Summary";
        }
        if (type.equals(CatalogUtil.VALUE_RIGHTS)) {
            String text = XmlUtil.getChildText(node);
            if ((text != null) && (text.length() < 50)) {
                return "Rights: " + text;
            } else {
                return "Rights";
            }
        }
        String title = XmlUtil.getAttribute(node,
                                            CatalogUtil.ATTR_XLINK_TITLE,
                                            (String) null);
        if (title != null) {
            return title;
        }
        return "Documentation";
    }


    /**
     * Get the tooltip text to use for the given documentation node.
     *
     * @param node The xml doc node
     *
     * @return The tooltip - show the documentation if it is a summary or rights. Else return null.
     */
    protected String getDocumentationToolTip(Element node) {
        String text  = null;
        String title = null;
        String type  = XmlUtil.getAttribute(node, CatalogUtil.ATTR_TYPE, "");
        if (type.equals(CatalogUtil.VALUE_SUMMARY)) {
            text  = XmlUtil.getChildText(node);
            title = "Summary";
        } else if (type.equals(CatalogUtil.VALUE_RIGHTS)) {
            text  = XmlUtil.getChildText(node);
            title = "Rights";
        } else {
            return null;
        }

        return "<html><b>" + title + "</b><hr>"
               + StringUtil.breakText(text, "<br>", 50) + "</html>";
    }



    /**
     * Move any doc nodes to the end of the sibling list, under a docparent node
     * if there is more than one
     *
     * @param node The node in the tree
     * @param doc The document to create a new node with
     */
    private void shuffleDocNodes(Element node, Document doc) {
        if (XmlUtil.isTag(node, CatalogUtil.TAG_DOCUMENTATION)) {
            return;
        }
        List docNodes = null;
        List children = XmlUtil.findChildren(node, null);
        for (int i = 0; i < children.size(); i++) {
            Object tmp = children.get(i);
            if ( !(tmp instanceof Element)) {
                continue;
            }
            Element child = (Element) tmp;
            if (XmlUtil.isTag(child, CatalogUtil.TAG_DOCUMENTATION)) {
                node.removeChild(child);
                if (XmlUtil.getAttribute(child, "xlink:title",
                                         "").indexOf("Imported from") < 0) {
                    if (docNodes == null) {
                        docNodes = new ArrayList();
                    }
                    docNodes.add(child);
                }

            } else {
                shuffleDocNodes(child, doc);
            }
        }

        if ((docNodes != null) && (docNodes.size() > 0)) {
            if (docNodes.size() == 1) {
                node.appendChild((Element) docNodes.get(0));
            } else {
                Element newDocNode =
                    doc.createElement(CatalogUtil.TAG_DOCPARENT);
                node.appendChild(newDocNode);
                for (int i = 0; i < docNodes.size(); i++) {
                    newDocNode.appendChild((Element) docNodes.get(i));
                }
            }

        }

    }

    /** the thumbnails */
    private Hashtable<String, ImageIcon> thumbnails = new Hashtable();

    /** pending image urls */
    private HashSet pendingImageUrls = new HashSet();

    /** the remote icon */
    private ImageIcon remoteIcon;

    /** the dataset icon */
    private ImageIcon datasetIcon;

    /**
     * Create the  UI
     *
     *  @return The UI component
     */
    protected JComponent doMakeContents() {

        showThumbsCbx = new JCheckBox("Show Thumbnail Images",
                                      !GuiUtils.isMac());
        showThumbsCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                updateTree();
            }
        });
        double version = CatalogUtil.getVersion(root);
        shuffleDocNodes(root, chooser.getDocument());

        loadIndividuallyCbx = new JCheckBox("Load Multiples Separately",
                                            false);
        loadIndividuallyCbx.setToolTipText(
            "<html>You can select multiple data sets with a control-click.<br>When this checkbox is selected the IDV loads each one separately.<br>When unchecked the IDV tries to load them as a single data source<br>(e.g., as multiple times)</html>");

        dataSourcesCbx = chooser.getDataSourcesComponent(false,
                chooser.getDataManager());
        JComponent dsComp =
            GuiUtils.inset(GuiUtils.hbox(new JLabel("Data Source Type: "),
                                         dataSourcesCbx, 5), 5);
        //        JComponent dsComp = GuiUtils.inset(GuiUtils.hbox(new JLabel("Data Source Type: "), dataSourcesCbx,
        //                                                       loadIndividuallyCbx, 5),5);


        tree = new XmlTree(root, true, path) {

            public ImageIcon getIconForNode(Element node) {
                if (remoteIcon == null) {
                    datasetIcon = GuiUtils.getImageIcon(
                        "/auxdata/ui/icons/folderclosed.png");
                    remoteIcon = GuiUtils.getImageIcon(
                        "/auxdata/ui/icons/remotecatalog.png");
                }
                if (XmlUtil.isTag(node, CatalogUtil.TAG_CATALOGREF)) {
                    return remoteIcon;
                }

                List propertyNodes = XmlUtil.findChildren(node,
                                         CatalogUtil.TAG_PROPERTY);
                String[] ids;
                if (showThumbsCbx.isSelected()) {
                    ids = new String[] { "thumbnail", "icon" };
                } else {
                    ids = new String[] { "icon" };
                }
                for (String id : ids) {
                    for (Element propertyNode :
                            (List<Element>) propertyNodes) {
                        String name = XmlUtil.getAttribute(propertyNode,
                                          CatalogUtil.ATTR_NAME, "");
                        if (name.equals(id)) {
                            String imageUrl =
                                XmlUtil.getAttribute(propertyNode,
                                    CatalogUtil.ATTR_VALUE, "");
                            ImageIcon icon = thumbnails.get(imageUrl);
                            if (icon == null) {
                                //Only fetch it once
                                if ( !pendingImageUrls.contains(imageUrl)) {
                                    pendingImageUrls.add(imageUrl);
                                    Misc.run(ThreddsHandler.this,
                                             "fetchImages", new String[] { id,
                                            imageUrl });
                                }
                                return super.getIconForNode(node);
                            } else {
                                return icon;
                            }
                        }
                    }
                }


                NodeList elements = XmlUtil.getElements(node);
                for (int i = 0; i < elements.getLength(); i++) {
                    Element child = (Element) elements.item(i);
                    if (XmlUtil.isTag(child, CatalogUtil.TAG_DATASET)
                            || XmlUtil.isTag(child,
                                             CatalogUtil.TAG_CATALOGREF)) {
                        return datasetIcon;
                    }
                }


                return super.getIconForNode(node);
            }


            protected Document readXlinkXml(String href) throws Exception {
                //If it is a thredds catalog then keep it in place.
                Document doc = XmlUtil.getDocument(href, getClass());
                if (doc == null) {
                    LogUtil.userErrorMessage("Could not load catalog: "
                                             + href);
                    return null;
                }
                if (XmlUtil.isTag(doc.getDocumentElement(),
                                  CatalogUtil.TAG_CATALOG)) {
                    return doc;
                }
                //Else (e.g., wms) show a new xml xhooser ui
                chooser.makeUi(doc, doc.getDocumentElement(), href);
                return null;
            }


            /**
             * Import the children of the top level data set node
             */
            protected int getXlinkImportLevel() {
                return 2;
            }

            protected boolean initXlinkRoot(Element root, Document doc,
                                            String url) {
                shuffleDocNodes(root, doc);
                //Add the catalog url attribute
                root.setAttribute(CatalogUtil.ATTR_CATALOGURL, url);
                return true;
            }

            public String getToolTipText(Element n) {
                if (XmlUtil.isTag(n, CatalogUtil.TAG_DOCUMENTATION)) {
                    return getDocumentationToolTip(n);
                }
                if (XmlUtil.isTag(n, CatalogUtil.TAG_CATALOGREF)) {
                    String href = XmlUtil.getAttribute(n,
                                      XmlTree.ATTR_XLINKHREF, (String) null);
                    if (href == null) {
                        return "Remote catalog: " + "none defined";
                    }
                    return "Remote catalog: " + tree.expandRelativeUrl(href);
                }
                if (XmlUtil.isTag(n, CatalogUtil.TAG_DATASET)) {
                    List paths = new ArrayList();
                    if (collectUrlPaths(paths, n, root, false)) {
                        if (paths.size() > 0) {
                            XmlChooser.PropertiedAction pa =
                                (XmlChooser.PropertiedAction) paths.get(0);

                            String thumbnail = null;
                            List propertyNodes =
                                XmlUtil.findChildren(n,
                                    CatalogUtil.TAG_PROPERTY);
                            for (Element propertyNode :
                                    (List<Element>) propertyNodes) {
                                String name =
                                    XmlUtil.getAttribute(propertyNode,
                                        CatalogUtil.ATTR_NAME, "");
                                if (name.equals("thumbnail")) {
                                    String value =
                                        XmlUtil.getAttribute(propertyNode,
                                            CatalogUtil.ATTR_VALUE, "");
                                    thumbnail = "<img src=\"" + value + "\">";
                                    break;
                                }
                            }

                            if (showThumbsCbx.isSelected()
                                    && (thumbnail != null)) {
                                return "<html>" + "Url: " + pa.action
                                       + "<br>" + thumbnail + "</html>";
                            }
                            return "Url: " + pa.action;
                        }
                    }
                }
                return super.getToolTipText(n);
            }

            public String getLabel(Element n) {
                if (XmlUtil.isTag(n, CatalogUtil.TAG_DOCUMENTATION)) {
                    return getDocumentationLabel(n);
                }
                if (XmlUtil.isTag(n, CatalogUtil.TAG_DOCPARENT)) {
                    return "Documentation";
                }
                String lbl = super.getLabel(n);
                lbl = lbl.replace("_", " ");
                return lbl;
            }

            public void doDoubleClick(XmlTree theTree,
                                      XmlTree.XmlTreeNode node,
                                      Element element) {
                if (node instanceof XmlTree.XlinkTreeNode) {
                    XmlTree.XlinkTreeNode xn = (XmlTree.XlinkTreeNode) node;
                    if (xn.getHaveLoaded()) {
                        String href = xn.getHref();
                        href = theTree.expandRelativeUrl(node, href);
                        chooser.makeUiFromPath(href);
                    }
                    return;
                }

                processNodeInThread(element);
            }

            public void doClick(XmlTree theTree, XmlTree.XmlTreeNode node,
                                Element element) {
                List    elements = tree.getSelectedElements();
                boolean haveData = false;
                for (int i = 0; (i < elements.size()) && !haveData; i++) {
                    haveData =
                        CatalogUtil.getUrlPath((Element) elements.get(i))
                        != null;
                }
                chooser.setHaveData(haveData);
            }

            public void doRightClick(XmlTree theTree,
                                     XmlTree.XmlTreeNode node,
                                     Element element, MouseEvent event) {
                JPopupMenu popup = new JPopupMenu();
                if (makePopupMenu(theTree, element, popup, node)) {
                    popup.show((Component) event.getSource(), event.getX(),
                               event.getY());
                }
            }

            protected void expandXlink(XlinkTreeNode node, String href) {
                chooser.showWaitCursor();
                super.expandXlink(node, href);
                chooser.showNormalCursor();
            }

            public NodeList getXlinkImportElements(Element root) {
                NodeList importElements = XmlUtil.getGrandChildren(root);
                for (int i = 0; i < importElements.getLength(); i++) {
                    Element child = (Element) importElements.item(i);
                    if (shouldProcess(child)) {
                        return importElements;
                    }
                }
                importElements = XmlUtil.getElements(root);
                for (int i = 0; i < importElements.getLength(); i++) {
                    Element child = (Element) importElements.item(i);
                    if (shouldProcess(child)) {
                        return importElements;
                    }
                }
                importElements = new XmlNodeList();
                ((XmlNodeList) importElements).add(root);
                return importElements;

            }

        };

        //Define that we look for the label of a catalogref node with the xlink:title attribute.
        tree.defineLabelAttr(CatalogUtil.TAG_CATALOGREF, "xlink:title");
        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addXlinkTag(CatalogUtil.TAG_CATALOGREF);
        if (version == CatalogUtil.THREDDS_VERSION_0_4) {
            tree.addTagsToProcess(Misc.newList(new Object[] {
                CatalogUtil.TAG_CATALOG,
                CatalogUtil.TAG_CATALOGREF, CatalogUtil.TAG_COLLECTION,
                CatalogUtil.TAG_DATASET, CatalogUtil.TAG_DOCUMENTATION }));
        } else {
            tree.addTagsToProcess(Misc.newList(new Object[] {
                CatalogUtil.TAG_CATALOGREF,
                CatalogUtil.TAG_COLLECTION, CatalogUtil.TAG_DATASET,
                CatalogUtil.TAG_DOCUMENTATION, CatalogUtil.TAG_DOCPARENT }));
            tree.addTagsToNotProcessButRecurse(
                Misc.newList(CatalogUtil.TAG_CATALOG));
        }
        tree.setIconForTag(
            GuiUtils.getImageIcon(
                "/auxdata/ui/icons/Information16.gif",
                getClass()), CatalogUtil.TAG_DOCUMENTATION);

        /**
         *        tree.setIconForTag(
         *   GuiUtils.getImageIcon(
         *       "/auxdata/ui/icons/Information16.gif",
         *       getClass()), CatalogUtil.TAG_DOCPARENT);
         */
        JComponent ui =
            GuiUtils.inset(GuiUtils.topCenterBottom(GuiUtils.left(dsComp),
                tree.getScroller(), GuiUtils.right(showThumbsCbx)), 5);
        return ui;

    }





    /**
     * update the tree in the swing thread
     */
    private void updateTree() {
        GuiUtils.invokeInSwingThread(new Runnable() {
            public void run() {
                try {
                    tree.updateUI();
                    tree.repaint();
                } catch (Exception exc) {
                    exc.printStackTrace();
                }
            }
        });
    }



    /**
     * Fetch the image url and update the tree.
     * The tuple contains a pair [id, imageUrl] where id is either icon or thumbnail
     *
     * @param tuple id and url
     */
    public void fetchImages(String[] tuple) {
        String id       = tuple[0];
        String imageUrl = tuple[1];
        Image  image    = ImageUtils.readImage(imageUrl);
        if (id.equals("thumbnail")) {
            image = ImageUtils.resize(image, 100, -1);
            ImageUtils.waitOnImage(image);
        }
        ImageIcon icon = new ImageIcon(image);
        thumbnails.put(imageUrl, icon);
        pendingImageUrls.remove(imageUrl);
        updateTree();
    }



    /**
     *  Create and popup a command menu for when the user has clicked on the given xml node.
     *
     *  @param theTree The XmlTree object displaying the current xml document.
     *  @param node The xml node the user clicked on.
     *  @param popup The popup menu to put the menu items in.
     * @param treeNode the tree node
     * @return Did we add any items into the menu
     */
    private boolean makePopupMenu(final XmlTree theTree, final Element node,
                                  JPopupMenu popup,
                                  final XmlTree.XmlTreeNode treeNode) {
        String    tagName = XmlUtil.getLocalName(node);


        boolean   didone  = false;
        JMenuItem mi;
        if (tagName.equals(CatalogUtil.TAG_DATASET)) {
            if (CatalogUtil.getUrlPath(node) != null) {
                mi = new JMenuItem("Load Dataset");
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        processNodeInThread(node);
                    }
                });
                popup.add(mi);
                didone = true;
            }
        }




        if (tagName.equals(CatalogUtil.TAG_DOCUMENTATION)) {
            mi = new JMenuItem("View Documentation");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    showDocumentation(node);
                }
            });
            popup.add(mi);
            didone = true;
        }


        if (tagName.equals(CatalogUtil.TAG_CATALOGREF)) {
            final String href = XmlUtil.getAttribute(node,
                                    XmlTree.ATTR_XLINKHREF, (String) null);
            if (href != null) {
                mi = new JMenuItem("Load Remote Catalog");
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        chooser.makeUiFromPath(
                            theTree.expandRelativeUrl(treeNode, href));
                    }
                });
                popup.add(mi);
                didone = true;
            }
        }


        mi = new JMenuItem("Write Catalog");
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveCatalog(theTree);
            }
        });
        popup.add(mi);


        didone = true;


        return didone;
    }



    /**
     *  Prompt for a file name and write the current xml into it.
     *
     *  @param tree The XmlTree displaying the current document.
     */
    private void saveCatalog(XmlTree tree) {
        try {
            String filename =
                FileManager.getWriteFile(FileManager.FILTER_XML,
                                         FileManager.SUFFIX_XML);
            if (filename == null) {
                return;
            }
            String xml = chooser.getXml();
            IOUtil.writeFile(filename, xml);
        } catch (Exception exc) {
            chooser.logException("Writing file", exc);
        }
    }



    /**
     *  The user  has pressed the 'Load' button. Check if a  node is selected
     */
    public void doLoad() {
        List elements = tree.getSelectedElements();
        processNodes(elements);
    }

    /**
     * Show the documentation defined by the given node in an html window
     *
     * @param node The documentation node
     */
    private void showDocumentation(Element node) {
        String doc   = null;
        String title = null;
        if (XmlUtil.getAttribute(node, CatalogUtil.ATTR_TYPE,
                                 "").equals(CatalogUtil.VALUE_SUMMARY)) {
            //            <documentation type="summary">
            doc   = XmlUtil.getChildText(node);
            title = "Summary";
        } else if (XmlUtil.getAttribute(
                node, CatalogUtil.ATTR_TYPE, "").equals(
                CatalogUtil.VALUE_RIGHTS)) {
            //            <documentation type="summary">
            doc   = XmlUtil.getChildText(node);
            title = "Rights";
        } else {
            //            <documentation xlink:href="http://cloud1.arc.nasa.gov/solve/" xlink:title="SOLVE home page"/>
            String xlink = XmlUtil.getAttribute(node,
                               CatalogUtil.ATTR_XLINK_HREF, (String) null);
            if (xlink != null) {
                doc = IOUtil.readContents(xlink, (String) null);
                title = XmlUtil.getAttribute(node,
                                             CatalogUtil.ATTR_XLINK_TITLE,
                                             "");
            }
        }

        if (doc == null) {
            LogUtil.userMessage("Could not find the documentation");
            //System.err.println(XmlUtil.toString(node));
            return;
        }

        if (title == null) {
            title = "Documentation";
        }
        if (doc.indexOf("<html") < 0) {
            doc = StringUtil.breakText(doc, "<br>", 50);
        }
        try {
            //Some html docs have a head tag which seems to screw up the html rendering
            doc = doc.replaceAll("(?s)<head>.*</head>", "");
            GuiUtils.showHtmlDialog(doc, title, null);
        } catch (Exception exc) {
            chooser.logException("Showing documentation", exc);
        }



    }




    /**
     * handle load
     *
     * @param node selected node
     */
    private void processNodeInThread(Element node) {
        Misc.run(this, "processNode", node);
    }


    /**
     * Process the given node
     *
     * @param node node
     */
    public void processNode(Element node) {
        if (node == null) {
            return;
        }
        processNodes(Misc.newList(node));
    }

    /**
     * The entry point that does the work of loading in the data source
     * specified by the given node
     *
     * @param nodes nodes to process
     */
    private void processNodes(List nodes) {
        //System.err.println("doLoad");

        double    version    = CatalogUtil.getVersion(root);
        List      urls       = new ArrayList();
        Hashtable properties = null;
        for (int i = 0; i < nodes.size(); i++) {
            Element node = (Element) nodes.get(i);
            if (XmlUtil.isTag(node, CatalogUtil.TAG_DOCUMENTATION)) {
                showDocumentation(node);
                continue;
            }
            if ( !XmlUtil.isTag(node, CatalogUtil.TAG_DATASET)) {
                continue;
            }
            if (version == CatalogUtil.THREDDS_VERSION_0_4) {
                //Always handle v4 singly. We probably never see this anymore
                process04Dataset(node);
            } else if (version >= CatalogUtil.THREDDS_VERSION_0_6) {
                List urlPaths = new ArrayList();
                if ( !collectUrlPaths(urlPaths, node, root, true)) {
                    return;
                }
                boolean loadAsGroup = true;

                //              if(loadIndividuallyCbx.isSelected()) {
                if ( !loadAsGroup) {
                    chooser.handleActions(urlPaths);
                } else {
                    for (int actionIdx = 0; actionIdx < urlPaths.size();
                            actionIdx++) {
                        XmlChooser.PropertiedAction action =
                            (XmlChooser.PropertiedAction) urlPaths.get(
                                actionIdx);
                        if (properties == null) {
                            properties = new Hashtable();
                            if (action.properties != null) {
                                properties.putAll(action.properties);
                            }
                        }
                        properties.put(DataSource.PROP_SUBPROPERTIES
                                       + urls.size(), action.properties);
                        chooser.initSubProperties(action.properties);
                        urls.add(action.action);
                    }
                }
            } else {
                IdvChooser.errorMessage("Unknown thredds version:" + version);
                return;
            }
        }

        if (urls.size() > 0) {
            if ((properties != null)
                    && (properties.get(DataManager.DATATYPE_ID) == null)) {
                String dataSourceId = chooser.getDataSourceId(dataSourcesCbx);
                if (dataSourceId != null) {
                    properties.put(DataManager.DATATYPE_ID, dataSourceId);
                }
            }
            //System.err.println("p:" + properties);

            if (chooser.makeDataSource(urls, null, properties)) {
                chooser.closeChooser();
            }
        }

    }



    /**
     *  Process the Thredds dataset from the 0.4 version.
     *
     *  @param datasetNode The xml node the user chose.
     */
    private void process04Dataset(Element datasetNode) {
        String serverId = XmlUtil.getAttribute(datasetNode,
                              CatalogUtil.ATTR_SERVERID, NULL_STRING);
        if (serverId == null) {
            IdvChooser.errorMessage("No server id found");
            return;
        }
        String urlPath = XmlUtil.getAttribute(datasetNode,
                             CatalogUtil.ATTR_URLPATH, NULL_STRING);
        if (urlPath == null) {
            IdvChooser.errorMessage("No urlPath found");
            return;
        }
        Element serverNode = XmlUtil.findElement(root,
                                 CatalogUtil.TAG_SERVER, CatalogUtil.ATTR_ID,
                                 serverId);

        if (serverNode == null) {
            IdvChooser.errorMessage("No server with id:" + serverId
                                    + " found");
            return;
        }
        String base = XmlUtil.getAttribute(serverNode, CatalogUtil.ATTR_BASE,
                                           NULL_STRING);
        if (base == null) {
            IdvChooser.errorMessage("No base found for server:" + serverId);
            return;
        }
        String    url        = base + urlPath;
        Hashtable properties = new Hashtable();
        chooser.handleAction(url, getProperties(datasetNode, serverNode));
    }

    /**
     *  Insert the common properties used for thredds datasets into a new properties Hashtable.
     *
     *  @param datasetNode The node the user chose.
     *  @param serviceNode The service node for the chosen dataset.
     *  @return A new Hashtable containing any properties for the dataset.
     */
    private Hashtable getProperties(Element datasetNode,
                                    Element serviceNode) {
        return getProperties(datasetNode, serviceNode, new Hashtable());
    }


    /**
     *  This finds the last 1 or 2 name attrbiutes, concatenates
     *  them and places them in the return Hashtable.
     *
     *  @param datasetNode The node the user chose.
     *  @param serviceNode The service node for the chosen dataset.
     *  @param properties The table to put the properties in.
     *  @return The properties argument with the properties added into it.
     */
    private Hashtable getProperties(Element datasetNode, Element serviceNode,
                                    Hashtable properties) {
        if (properties == null) {
            properties = new Hashtable();
        }

        List docLinks = getDocLinks(datasetNode, null);
        if ((docLinks != null) && (docLinks.size() > 0)) {
            //Take this out for now. Do we really want the
            //doc links to show up as data choices?
            properties.put(DataSource.PROP_DOCUMENTLINKS, docLinks);
        }


        String dataType = CatalogUtil.findDataTypeForDataset(datasetNode,
                              root, CatalogUtil.getVersion(root), true);

        if ((dataType != null)
                && (dataType.equals("unknown")
                    || (dataType.trim().length() == 0))) {
            dataType = null;
        }
        String serviceType  = CatalogUtil.getServiceType(serviceNode);

        String dataSourceId = chooser.getDataSourceId(dataSourcesCbx);



        if (dataSourceId != null) {
            properties.put(DataManager.DATATYPE_ID, dataSourceId);
        } else {
            if ((dataType != null) && (serviceType != null)
                    && ( !serviceType.equals(CatalogUtil.SERVICE_RESOLVER))) {
                properties.put(DataManager.DATATYPE_ID,
                               serviceType + "." + dataType);
            } else if (serviceType != null) {
                //            properties.put(DataManager.DATATYPE_ID, serviceType);
            } else if (dataType != null) {
                //                properties.put(DataManager.DATATYPE_ID, dataType);
            }
        }
        //        System.out.println("xml:" + XmlUtil.toString(XmlUtil.findRoot(datasetNode)));

        String title = CatalogUtil.getTitleFromDataset(datasetNode);
        if (title != null) {
            properties.put(DataSource.PROP_TITLE, title);
        }


        return properties;
    }



    /**
     * Get the docs link from lower in the tree
     *
     * @param docNode documentation node
     * @param list list of nodes
     *
     * @return List of documentation nodes
     */
    private List getDocLinksDown(Element docNode, List list) {
        String link = XmlUtil.getAttribute(docNode,
                                           CatalogUtil.ATTR_XLINK_HREF,
                                           (String) null);
        if (XmlUtil.isTag(docNode, CatalogUtil.TAG_DOCUMENTATION)) {
            if (link != null) {
                if (list == null) {
                    list = new ArrayList();
                }
                list.add(link);
            } else {
                String type = XmlUtil.getAttribute(docNode,
                                  CatalogUtil.ATTR_TYPE, (String) null);
                if ((type != null)
                        && type.equals(CatalogUtil.VALUE_SUMMARY)) {
                    String text = XmlUtil.getChildText(docNode);
                    if ((text != null) && (text.trim().length() > 0)) {
                        if (list == null) {
                            list = new ArrayList();
                        }
                        list.add("<b>Summary:</b><hr>" + text);
                    }
                }
            }
        }
        NodeList elements = XmlUtil.getElements(docNode, (String) null);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            list = getDocLinksDown(child, list);
        }
        return list;
    }

    /**
     *  For the given node find the "documentation" child node. If found
     *  look for the xlink:href attribute, if found place the attribute
     *  value into the given list. Then recurse upwards.
     *
     *  @param node The node to find documentation links for.
     *  @param list The list to put the doc links into.
     *  @return A list of linksp
     *  The given list argument (if non-null) or a new list object
     *  containing any links found or null if no links found.
     */
    int cnt = 0;

    /**
     * Get the documentation links
     *
     * @param node node
     * @param list list of docs to add to
     *
     * @return list of docs for that node
     */
    private List getDocLinks(Node node, List list) {
        if (node instanceof Element) {
            NodeList elements = XmlUtil.getElements((Element) node,
                                    (String) null);
            for (int i = 0; i < elements.getLength(); i++) {
                Element child = (Element) elements.item(i);
                if (XmlUtil.isTag(child, CatalogUtil.TAG_DOCUMENTATION)
                        || XmlUtil.isTag(child, CatalogUtil.TAG_DOCPARENT)) {
                    list = getDocLinksDown(child, list);
                }
            }
        }


        Node parent = node.getParentNode();
        if (parent != null) {
            list = getDocLinks(parent, list);
        }
        return list;
    }




    /**
     *  Look for the named attribute contained by a child of the given element
     *  with the given tag name.
     *
     *  @param parent The xml node to look within.
     *  @param nameValueLookingFor The value we are looking for
     *  @param dflt The default value returned.
     *  @return The value of the given attribute or dflt if not found.
     */
    public String getPropertyAttributeFromChild(Element parent,
            String nameValueLookingFor, String dflt) {
        if (parent == null) {
            return dflt;
        }
        NodeList elements = XmlUtil.getElements(parent,
                                CatalogUtil.TAG_PROPERTY);
        for (int i = 0; i < elements.getLength(); i++) {
            Node child = (Node) elements.item(i);
            String nameValue = XmlUtil.getAttribute(child,
                                   CatalogUtil.ATTR_NAME, (String) null);
            if (nameValue == null) {
                continue;
            }
            if ( !nameValue.equals(nameValueLookingFor)) {
                continue;
            }
            String valueValue = XmlUtil.getAttribute(child,
                                    CatalogUtil.ATTR_VALUE, (String) null);
            if (valueValue != null) {
                return valueValue;
            }
        }
        Node nextParent = parent.getParentNode();
        if (nextParent instanceof Element) {
            return getPropertyAttributeFromChild((Element) nextParent,
                    nameValueLookingFor, dflt);
        }
        return dflt;
    }




    /**
     *  For the given dataset node find the urlPaths that define it. We use a list of paths
     *  because  sometime we may handle composite datasets. For now, if we just insert
     *  one path into the list. We actually add a "PropertiedAction" which holds a String (the
     *  url action) and a properties table.
     *
     *  @param urlPaths The List to add the paths to.
     *  @param datasetNode The dataset node we are looking at.
     *  @param root
     *  @param flagMissing If true then tell the user
     *  @return Were any paths found.
     */
    private boolean collectUrlPaths(List urlPaths, Element datasetNode,
                                    Element root, boolean flagMissing) {


        String urlPath = CatalogUtil.getUrlPath(datasetNode);

        if (urlPath != null) {
            //A hack to allow for absolute paths in urls
            String  url;
            Element serviceNode = null;

            if ((urlPath.indexOf("://") >= 0) || new File(urlPath).exists()) {
                url = urlPath;
            } else {
                serviceNode =
                    CatalogUtil.findServiceNodeForDataset(datasetNode,
                        flagMissing, null);
                if (serviceNode == null) {
                    return false;
                }

                url = CatalogUtil.getAbsoluteUrl(serviceNode, urlPath);
                if (url == null) {
                    if (flagMissing) {
                        IdvChooser.errorMessage(
                            "Could not read any dataset urls");
                    }
                    return false;
                }
            }

            Hashtable properties = new Hashtable();
            properties.put(CatalogUtil.PROP_CATALOGURL, path);
            String groupId = getPropertyAttributeFromChild(datasetNode,
                                 "group", null);
            if (groupId != null) {
                properties.put(CatalogUtil.PROP_DATASETGROUP, groupId);
            }

            String datasetId = XmlUtil.getAttribute(datasetNode,
                                   CatalogUtil.ATTR_DATASETID, (String) null);
            if (datasetId != null) {
                properties.put(CatalogUtil.PROP_DATASETID, datasetId);
            }


            List propertyNodes = XmlUtil.findChildren(datasetNode,
                                     CatalogUtil.TAG_PROPERTY);
            for (Element propertyNode : (List<Element>) propertyNodes) {
                properties
                    .put(XmlUtil
                        .getAttribute(propertyNode,
                                      CatalogUtil.ATTR_NAME), XmlUtil
                                          .getAttribute(propertyNode,
                                              CatalogUtil.ATTR_VALUE));
            }

            CatalogUtil.addServiceProperties(datasetNode, properties,
                                             urlPath);



            if (serviceNode != null) {
                //If this is a "Resolver" service type then the url points to a catalog 
                //that holds the real url.
                String serviceType = CatalogUtil.getServiceType(serviceNode);
                getProperties(datasetNode, serviceNode, properties);
                if (CatalogUtil.SERVICE_RESOLVER.equals(serviceType)) {
                    String resolverUrl = url;
                    Object[] result =
                        CatalogUtil.getResolverData(resolverUrl, properties);
                    if (result == null) {
                        return false;
                    }
                    datasetNode = (Element) result[1];
                    serviceNode = (Element) result[2];
                    url         = (String) result[3];
                    properties.put(DataSource.PROP_RESOLVERURL, resolverUrl);
                    String title = CatalogUtil.getTitleFromDataset(
                                       (Element) datasetNode);
                    if ((title != null) && (properties != null)) {
                        properties.put(DataSource.PROP_TITLE, title);
                    }
                }
            }
            //System.err.println("Absolute url:" + url);
            urlPaths.add(new XmlChooser.PropertiedAction(url, properties));
        } else {
            //For now don't deal with container dataset nodes
            if (true) {
                if (flagMissing) {
                    IdvChooser.errorMessage("No url path found for dataset.");
                }
                return false;
            }
            //This is for dataset nodes that contain other dataset nodes.
            List children = XmlUtil.findChildren(datasetNode,
                                CatalogUtil.TAG_DATASET);
            for (int i = 0; i < children.size(); i++) {
                Element childDatasetNode = (Element) children.get(i);
                urlPath = XmlUtil.getAttribute(childDatasetNode,
                        CatalogUtil.ATTR_URLPATH, NULL_STRING);
                if (urlPath != null) {
                    String base =
                        CatalogUtil.findBaseForDataset(childDatasetNode,
                            root);
                    if (base == null) {
                        return false;
                    }
                    urlPaths.add(base + urlPath);
                } else {
                    if ( !collectUrlPaths(urlPaths, childDatasetNode, root,
                                          flagMissing)) {
                        return false;
                    }
                }
            }
        }
        return true;

    }



    /**
     * Test the html generation
     *
     * @param args Command line args
     *
     * @throws Exception  problem occurred
     */
    public static void main(String[] args) throws Exception {


        String doc = IOUtil.readContents("test.html", ThreddsHandler.class);
        doc = "hello<head>\nxxx</head>";
        doc = doc.replaceAll("(?s)<head>.+</head>", "");
        System.err.println("doc:" + doc);


        if (true) {
            return;
        }


        LogUtil.configure();
        LogUtil.setTestMode(true);
        String catalog =
            "http://motherlode.ucar.edu:8080/thredds/idv/rt-models.xml";
        String xml = IOUtil.readContents(catalog, (String) null);
        if (xml == null) {
            System.err.println("Unable to read: " + catalog);
            System.exit(1);
        }
        String bundleTemplate = IOUtil.readContents("template.xidv", "");
        String jnlpTemplate   = IOUtil.readContents("template.jnlp", "");

        try {
            //      System.out.println (xml);
            Element root = XmlUtil.getRoot(xml);
            String  name = XmlUtil.getAttribute(root, "name");
            System.out.println(name + "\n<ul>");
            List children = XmlUtil.findChildren(root,
                                CatalogUtil.TAG_DATASET);
            for (int i = 0; i < children.size(); i++) {
                Element child = (Element) children.get(i);
                CatalogUtil.generateHtml(root, child, 0, bundleTemplate,
                                         jnlpTemplate);
            }

            System.out.println("</ul>");
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
        }
        System.exit(0);

    }





}
