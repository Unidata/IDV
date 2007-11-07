/*
 * $Id: ThreddsHandler.java,v 1.68 2007/07/09 22:59:58 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

import ucar.unidata.ui.XmlTree;


import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;




import java.util.ArrayList;
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


    /** Property name for the url of the catalog */
    public static final String PROP_CATALOGURL = "Thredds.CatalogUrl";

    /** Property name for the   data set id */
    public static final String PROP_DATASETID = "Thredds.DataSetId";

    /** Property name for the  data set group */
    public static final String PROP_DATASETGROUP = "Thredds.DataGroup";

    /** Property name for the  annotations server url */
    public static final String PROP_ANNOTATIONSERVER =
        "Thredds.AnnotationServer";

    /** Xml attribute name for the url where the doc came from */
    public static final String ATTR_CATALOGURL = "catalogurl";


    /** Xml attribute name for the data set group */
    public static final String ATTR_DATASETGROUP = "group";

    /** Xml attribute name for the data set id */
    public static final String ATTR_DATASETID = "id";

    /** Xml attribute name for the data set id */
    public static final String VALUE_ANNOTATIONSERVER = "annotationServer";



    /** Xml attribute value for the summary documentation */
    public static final String VALUE_SUMMARY = "summary";

    /** Xml attribute value for the rights documentation */
    public static final String VALUE_RIGHTS = "rights";


    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ThreddsHandler.class.getName());

    /** More clear than  then doing (String)null */
    public static final String NULL_STRING = null;

    /**
     * Service name of the special resolver service.
     * If a data set has a resolver service then the url
     * of the data set actually points to a resolver
     * service which will give back a catalog that contains
     * the actual data set
     */
    public static final String SERVICE_RESOLVER = "Resolver";

    /** Service type value for the compound service */
    public static final String SERVICE_COMPOUND = "Compound";

    /** Service type value for the compound service */
    public static final String SERVICE_FILE = "FILE";

    /** Service type value for the wcs service */
    public static final String SERVICE_HTTP = "HTTPServer";

    /** Service type value for the dods service */
    public static final String SERVICE_DODS = "DODS";

    /** Service type value for the adde service */
    public static final String SERVICE_ADDE = "ADDE";

    /** Service type value for the OPeNDAP service */
    public static final String SERVICE_OPENDAP = "OPENDAP";

    /** Value for the thredds catalog v0.4 */
    public static final double THREDDS_VERSION_0_4 = 0.4;

    /** Value for the thredds catalog v0.5 */
    public static final double THREDDS_VERSION_0_5 = 0.5;

    /** Value for the thredds catalog v0.6 */
    public static final double THREDDS_VERSION_0_6 = 0.6;

    /** Value for the thredds catalog v1.0 */
    public static final double THREDDS_VERSION_1_0 = 1.0;



    /** Xml tag name for the property tag for a dataset */
    public static final String TAG_PROPERTY = "property";


    /** Tag name for the xml node &quot;access&quot; */
    public static final String TAG_ACCESS = "access";

    /** Tag name for the xml node &quot;documentation&quot; */
    public static final String TAG_DOCUMENTATION = "documentation";

    /** Tag name for the xml node &quot;docparent&quot; */
    public static final String TAG_DOCPARENT = "docparent";

    /** Tag name for the xml node &quot;catalog&quot; */
    public static final String TAG_CATALOG = "catalog";

    /** Tag name for the xml node &quot;catalogRef&quot; */
    public static final String TAG_CATALOGREF = "catalogRef";

    /** Tag name for the xml node &quot;collection&quot; */
    public static final String TAG_COLLECTION = "collection";

    /** Tag name for the xml node &quot;dataset&quot; */
    public static final String TAG_DATASET = "dataset";

    /** Tag name for the xml node &quot;dataType&quot; */
    public static final String TAG_DATATYPE = "dataType";

    /** Tag name for the xml node &quot;metadata&quot; */
    public static final String TAG_METADATA = "metadata";

    /** Tag name for the xml node &quot;queryCapability&quot; */
    public static final String TAG_QUERYCAPABILITY = "queryCapability";

    /** Tag name for the xml node &quot;server &quot; */
    public static final String TAG_SERVER = "server";

    /** Tag name for the xml node &quot;service &quot; */
    public static final String TAG_SERVICE = "service";

    /** Tag name for the xml node &quot;serviceName&quot; */
    public static final String TAG_SERVICENAME = "serviceName";

    /** Attribute name for the xml attribute &quot;action &quot; */
    public static final String ATTR_ACTION = "action";

    /** Attribute name for the xml attribute &quot;dataType &quot; */
    public static final String ATTR_DATATYPE = "dataType";

    /** Attribute name for the xml attribute &quot;base &quot; */
    public static final String ATTR_BASE = "base";

    /** Attribute name for the xml attribute &quot;ID &quot; */
    public static final String ATTR_ID = "ID";

    /** Attribute name for the xml attribute &quot;inherited &quot; */
    public static final String ATTR_INHERITED = "inherited";

    /** Attribute name for the xml attribute &quot;name &quot; */
    public static final String ATTR_NAME = "name";

    /** Attribute name for the xml attribute &quot;value &quot; */
    public static final String ATTR_VALUE = "value";

    /** Attribute name for the xml attribute &quot;serverID &quot; */
    public static final String ATTR_SERVERID = "serverID";

    /** Attribute name for the xml attribute &quot;serviceName &quot; */
    public static final String ATTR_SERVICENAME = "serviceName";

    /** Attribute name for the xml attribute &quot;serviceType &quot; */
    public static final String ATTR_SERVICETYPE = "serviceType";

    /** Attribute name for the xml attribute &quot;suffix &quot; */
    public static final String ATTR_SUFFIX = "suffix";

    /** Attribute name for the xml attribute &quot;type &quot; */
    public static final String ATTR_TYPE = "type";

    /** Attribute name for the xml attribute &quot;url &quot; */
    public static final String ATTR_URL = "url";

    /** Attribute name for the xml attribute &quot;urlPath &quot; */
    public static final String ATTR_URLPATH = "urlPath";

    /** Attribute name for the xml attribute &quot;version &quot; */
    public static final String ATTR_VERSION = "version";

    /** Attribute name for the xml attribute &quot;xlink:href &quot; */
    public static final String ATTR_XLINK_HREF = "xlink:href";

    /** Attribute name for the xml attribute &quot;xlink:title &quot; */
    public static final String ATTR_XLINK_TITLE = "xlink:title";



    /** for gui */
    private JComboBox dataSourcesCbx;

    /** for gui */
    private JCheckBox loadIndividuallyCbx;

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
        root.setAttribute(ATTR_CATALOGURL, path);
    }

    protected void updateStatus() {
        if(chooser.getHaveData()) {
            chooser.setStatus("Press \"" + chooser.CMD_LOAD + "\" to load the selected data", "buttons");
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
        String type = XmlUtil.getAttribute(node, ATTR_TYPE, "");
        if (type.equals(VALUE_SUMMARY)) {
            String text = XmlUtil.getChildText(node);
            if ((text != null) && (text.length() < 50)) {
                return "Summary: " + text;
            }
            return "Summary";
        }
        if (type.equals(VALUE_RIGHTS)) {
            String text = XmlUtil.getChildText(node);
            if ((text != null) && (text.length() < 50)) {
                return "Rights: " + text;
            } else {
                return "Rights";
            }
        }
        String title = XmlUtil.getAttribute(node, ATTR_XLINK_TITLE,
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
        String type  = XmlUtil.getAttribute(node, ATTR_TYPE, "");
        if (type.equals(VALUE_SUMMARY)) {
            text  = XmlUtil.getChildText(node);
            title = "Summary";
        } else if (type.equals(VALUE_RIGHTS)) {
            text  = XmlUtil.getChildText(node);
            title = "Rights";
        } else {
            return null;
        }

        return "<html><b>" + title + "</b><hr>"
               + StringUtil.breakText(text, "<br>", 50) + "</html>";
    }

    /**
     * A utiliry to get the version from the catalog root.
     *
     *
     * @param node The xml node
     *
     * @return The version
     */
    private static double getVersion(Element node) {
        if ( !XmlUtil.hasAttribute(node, ATTR_VERSION)) {
            Node parent = node.getParentNode();
            if ((parent == null) || !(parent instanceof Element)) {
                return THREDDS_VERSION_1_0;
            }
            return getVersion((Element) parent);
        }

        String version = XmlUtil.getAttribute(node, ATTR_VERSION,
                             String.valueOf(THREDDS_VERSION_1_0));
        while (version.indexOf(".") != version.lastIndexOf(".")) {
            version = version.substring(0, version.lastIndexOf("."));
        }
        return new Double(version).doubleValue();
    }




    /**
     * Move any doc nodes to the end of the sibling list, under a docparent node
     * if there is more than one
     *
     * @param node The node in the tree
     * @param doc The document to create a new node with
     */
    private void shuffleDocNodes(Element node, Document doc) {
        if (node.getTagName().equals(TAG_DOCUMENTATION)) {
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
            if (child.getTagName().equals(TAG_DOCUMENTATION)) {
                if (docNodes == null) {
                    docNodes = new ArrayList();
                }
                docNodes.add(child);
                node.removeChild(child);
            } else {
                shuffleDocNodes(child, doc);
            }
        }

        if (docNodes != null) {
            if (docNodes.size() == 1) {
                node.appendChild((Element) docNodes.get(0));
            } else {
                Element newDocNode = doc.createElement(TAG_DOCPARENT);
                node.appendChild(newDocNode);
                for (int i = 0; i < docNodes.size(); i++) {
                    newDocNode.appendChild((Element) docNodes.get(i));
                }
            }

        }

    }


    /**
     * Create the  UI
     *
     *  @return The UI component
     */
    protected JComponent doMakeContents() {

        double version = getVersion(root);
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

            protected Document readXlinkXml(String href) throws Exception {
                //If it is a thredds catalog then keep it in place.
                Document doc = XmlUtil.getDocument(href, getClass());
                if (doc == null) {
                    LogUtil.userErrorMessage("Could not load catalog: "
                                             + href);
                    return null;
                }
                if (doc.getDocumentElement().getTagName().equals(
                        ThreddsHandler.TAG_CATALOG)) {
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
                root.setAttribute(ATTR_CATALOGURL, url);
                return true;
            }

            public String getToolTipText(Element n) {
                if (n.getTagName().equals(TAG_DOCUMENTATION)) {
                    return getDocumentationToolTip(n);
                }
                if (n.getTagName().equals(TAG_CATALOGREF)) {
                    String href = XmlUtil.getAttribute(n,
                                      XmlTree.ATTR_XLINKHREF, (String) null);
                    if (href == null) {
                        return "Remote catalog: " + "none defined";
                    }
                    return "Remote catalog: " + tree.expandRelativeUrl(href);
                }
                if (n.getTagName().equals(TAG_DATASET)) {
                    List paths = new ArrayList();
                    if (collectUrlPaths(paths, n, root, false)) {
                        if (paths.size() > 0) {
                            XmlChooser.PropertiedAction pa =
                                (XmlChooser.PropertiedAction) paths.get(0);
                            return "Url: " + pa.action;
                        }
                    }
                }

                return super.getToolTipText(n);
            }

            public String getLabel(Element n) {
                if (n.getTagName().equals(TAG_DOCUMENTATION)) {
                    return getDocumentationLabel(n);
                }
                if (n.getTagName().equals(TAG_DOCPARENT)) {
                    return "Documentation";
                }
                return super.getLabel(n);
            }

            public void doDoubleClick(XmlTree theTree,
                                      XmlTree.XmlTreeNode node,
                                      Element element) {
                if (node instanceof XmlTree.XlinkTreeNode) {
                    XmlTree.XlinkTreeNode xn = (XmlTree.XlinkTreeNode) node;
                    if (xn.getHaveLoaded()) {
                        String href = xn.getHref();
                        href = theTree.expandRelativeUrl(href);
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
                    haveData = getUrlPath((Element) elements.get(i)) != null;
                }
                chooser.setHaveData(haveData);
            }

            public void doRightClick(XmlTree theTree,
                                     XmlTree.XmlTreeNode node,
                                     Element element, MouseEvent event) {
                JPopupMenu popup = new JPopupMenu();
                if (makePopupMenu(theTree, element, popup)) {
                    popup.show((Component) event.getSource(), event.getX(),
                               event.getY());
                }
            }

            protected void expandXlink(XlinkTreeNode node, String href) {
                chooser.showWaitCursor();
                super.expandXlink(node, href);
                chooser.showNormalCursor();
            }


        };
        //Define that we look for the label of a catalogref node with the xlink:title attribute.
        tree.defineLabelAttr(TAG_CATALOGREF, "xlink:title");
        tree.getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        tree.addXlinkTag(TAG_CATALOGREF);
        if (version == THREDDS_VERSION_0_4) {
            tree.addTagsToProcess(Misc.newList(TAG_CATALOG, TAG_CATALOGREF,
                    TAG_COLLECTION, TAG_DATASET, TAG_DOCUMENTATION));
        } else {
            tree.addTagsToProcess(Misc.newList(TAG_CATALOGREF,
                    TAG_COLLECTION, TAG_DATASET, TAG_DOCUMENTATION,
                    TAG_DOCPARENT));
            tree.addTagsToNotProcessButRecurse(Misc.newList(TAG_CATALOG));
        }
        tree.setIconForTag(
            GuiUtils.getImageIcon(
                "/auxdata/ui/icons/Information16.gif",
                getClass()), TAG_DOCUMENTATION);

        /**
         *        tree.setIconForTag(
         *   GuiUtils.getImageIcon(
         *       "/auxdata/ui/icons/Information16.gif",
         *       getClass()), TAG_DOCPARENT);
         */
        JComponent ui =
            GuiUtils.inset(GuiUtils.topCenter(GuiUtils.left(dsComp),
                tree.getScroller()), 5);
        return ui;
    }







    /**
     *  Create and popup a command menu for when the user has clicked on the given xml node.
     *
     *  @param theTree The XmlTree object displaying the current xml document.
     *  @param node The xml node the user clicked on.
     *  @param popup The popup menu to put the menu items in.
     * @return Did we add any items into the menu
     */
    private boolean makePopupMenu(final XmlTree theTree, final Element node,
                                  JPopupMenu popup) {
        String    tagName = node.getTagName();


        boolean   didone  = false;
        JMenuItem mi;
        if (tagName.equals(TAG_DATASET)) {
            if (getUrlPath(node) != null) {
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




        if (tagName.equals(TAG_DOCUMENTATION)) {
            mi = new JMenuItem("View Documentation");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    showDocumentation(node);
                }
            });
            popup.add(mi);
            didone = true;
        }


        if (tagName.equals(TAG_CATALOGREF)) {
            final String href = XmlUtil.getAttribute(node,
                                    XmlTree.ATTR_XLINKHREF, (String) null);
            if (href != null) {
                mi = new JMenuItem("Load Remote Catalog");
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        chooser.makeUiFromPath(
                            theTree.expandRelativeUrl(href));
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
        if (XmlUtil.getAttribute(node, ATTR_TYPE, "").equals(VALUE_SUMMARY)) {
            //            <documentation type="summary">
            doc   = XmlUtil.getChildText(node);
            title = "Summary";
        } else if (XmlUtil.getAttribute(node, ATTR_TYPE,
                                        "").equals(VALUE_RIGHTS)) {
            //            <documentation type="summary">
            doc   = XmlUtil.getChildText(node);
            title = "Rights";
        } else {
            //            <documentation xlink:href="http://cloud1.arc.nasa.gov/solve/" xlink:title="SOLVE home page"/>
            String xlink = XmlUtil.getAttribute(node, ATTR_XLINK_HREF,
                               (String) null);
            //            System.err.println("xlink:" + xlink);
            if (xlink != null) {
                doc   = IOUtil.readContents(xlink, (String) null);
                title = XmlUtil.getAttribute(node, ATTR_XLINK_TITLE, "");
            }
        }

        if (doc == null) {
            LogUtil.userMessage("Could not find the documentation");
            System.err.println(XmlUtil.toString(node));
            return;
        }

        if (title == null) {
            title = "Documentation";
        }
        if (doc.indexOf("<html") < 0) {
            doc = StringUtil.breakText(doc, "<br>", 50);
        }
        try {
            System.err.println("doc:" + doc);
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
        double    version    = getVersion(root);
        List      urls       = new ArrayList();
        Hashtable properties = null;
        for (int i = 0; i < nodes.size(); i++) {
            Element node = (Element) nodes.get(i);
            if (node.getTagName().equals(TAG_DOCUMENTATION)) {
                showDocumentation(node);
                continue;
            }
            if ( !node.getTagName().equals(TAG_DATASET)) {
                continue;
            }
            if (version == THREDDS_VERSION_0_4) {
                //Always handle v4 singly. We probably never see this anymore
                process04Dataset(node);
            } else if (version >= THREDDS_VERSION_0_6) {
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
                        urls.add(action.action);
                    }
                }
            } else {
                IdvChooser.errorMessage("Unknown thredds version:" + version);
                return;
            }
        }

        if (urls.size() > 0) {
            //            System.out.println(urls);
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
        String serverId = XmlUtil.getAttribute(datasetNode, ATTR_SERVERID,
                              NULL_STRING);
        if (serverId == null) {
            IdvChooser.errorMessage("No server id found");
            return;
        }
        String urlPath = XmlUtil.getAttribute(datasetNode, ATTR_URLPATH,
                             NULL_STRING);
        if (urlPath == null) {
            IdvChooser.errorMessage("No urlPath found");
            return;
        }
        Element serverNode = XmlUtil.findElement(root, TAG_SERVER, ATTR_ID,
                                 serverId);

        if (serverNode == null) {
            IdvChooser.errorMessage("No server with id:" + serverId
                                    + " found");
            return;
        }
        String base = XmlUtil.getAttribute(serverNode, ATTR_BASE,
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


        String dataType = findDataTypeForDataset(datasetNode, root,
                              getVersion(root), true);

        String serviceType  = getServiceType(serviceNode);

        String dataSourceId = chooser.getDataSourceId(dataSourcesCbx);
        if (dataSourceId != null) {
            properties.put(DataManager.DATATYPE_ID, dataSourceId);
        } else {
            if ((dataType != null) && (serviceType != null)
                    && ( !serviceType.equals(SERVICE_RESOLVER))) {
                properties.put(DataManager.DATATYPE_ID,
                               serviceType + "." + dataType);
            } else if (serviceType != null) {
                //            properties.put(DataManager.DATATYPE_ID, serviceType);
            }
        }
        //        System.out.println("xml:" + XmlUtil.toString(XmlUtil.findRoot(datasetNode)));

        String title = getTitleFromDataset(datasetNode);
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
        String link = XmlUtil.getAttribute(docNode, ATTR_XLINK_HREF,
                                           (String) null);
        if (docNode.getTagName().equals(TAG_DOCUMENTATION)) {
            if (link != null) {
                if (list == null) {
                    list = new ArrayList();
                }
                list.add(link);
            } else {
                String type = XmlUtil.getAttribute(docNode, ATTR_TYPE,
                                  (String) null);
                if ((type != null) && type.equals(VALUE_SUMMARY)) {
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
                if (child.getTagName().equals(TAG_DOCUMENTATION)
                        || child.getTagName().equals(TAG_DOCPARENT)) {
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
     *  Find the service type attribute for the given service node. This is thredds version
     *  specific - looking for either "servicetype" attribute or "type" attr.
     *
     *  @param serviceNode The service node to look for the service type.
     *  @return The service type attribute or null if not found.
     */
    private static String getServiceType(Element serviceNode) {
        String serviceType = XmlUtil.getAttribute(serviceNode,
                                 ATTR_SERVICETYPE, NULL_STRING);
        if (serviceType == null) {
            //Maybe  version 0.4
            serviceType = XmlUtil.getAttribute(serviceNode, ATTR_TYPE,
                    NULL_STRING);
        }
        return serviceType;
    }

    /**
     * Find the data type attribute for the given service node.
     *
     * @param datasetNode The dataset node to look for the data type.
     * @return The dataType attribute or null if not found.
     */
    private static String getDataType(Element datasetNode) {
        String dataType = XmlUtil.getAttribute(datasetNode, ATTR_DATATYPE,
                              NULL_STRING);
        if (dataType == null) {}
        return dataType;
    }

    /**
     *  Assemble the String title for the given dataset. We look for the first two
     *  "name" attributes in the xml tree and concatenate them (If found).
     *
     *  @param datasetNode The dataset node we are looking at.
     *  @return The title for this dataset node. (may be null).
     */


    public static String getTitleFromDataset(Element datasetNode) {
        Hashtable tags = Misc.newHashtable(TAG_DATASET, TAG_DATASET,
                                           TAG_COLLECTION, TAG_COLLECTION);
        List titleAttrs = XmlUtil.getAttributesFromTree(datasetNode,
                              ATTR_NAME, tags);
        String title = null;
        if ((titleAttrs != null) && (titleAttrs.size() >= 1)) {
            if (titleAttrs.size() >= 2) {
                String t1 = titleAttrs.get(titleAttrs.size() - 2).toString();
                String t2 = titleAttrs.get(titleAttrs.size() - 1).toString();
                //If the first 8 characters are the same then just use the name
                if ((t1.length() > 8) && (t2.length() > 8)
                        && t1.substring(0, 8).equals(t2.substring(0, 8))) {
                    title = t2;
                } else {
                    title = t1 + " " + t2;
                }
            } else {
                title = titleAttrs.get(titleAttrs.size() - 1).toString();
            }
        }
        return title;
    }




    /**
     *  Search up the tree of dataset nodes, looking for a child service node.
     *  If not found then return null.
     *
     *
     * @param nodes nodes to process
     * @param datasetNode The element to look at.
     * @param serviceName The name of the service
     */
    private static void findChildServiceNode(List nodes, Element datasetNode,
                                             String serviceName) {
        if (datasetNode == null) {
            return;
        }

        List childrenServiceNodes = XmlUtil.findChildren(datasetNode,
                                        TAG_SERVICE);
        for (int i = 0; i < childrenServiceNodes.size(); i++) {
            Element serviceNode = (Element) childrenServiceNodes.get(i);
            if ( !Misc.equals(XmlUtil.getAttribute(serviceNode, ATTR_NAME,
                    NULL_STRING), serviceName)) {
                continue;
            }

            String serviceType = XmlUtil.getAttribute(serviceNode,
                                     ATTR_SERVICETYPE, NULL_STRING);
            if (Misc.equals(serviceType, SERVICE_COMPOUND)) {
                List children = XmlUtil.findChildren(serviceNode,
                                    TAG_SERVICE);
                for (int childIdx = 0; childIdx < children.size();
                        childIdx++) {
                    nodes.add(children.get(childIdx));
                }
            } else {
                nodes.add(serviceNode);
            }
        }
        Node node = datasetNode.getParentNode();
        //Are we at the top?
        if (node instanceof Element) {
            findChildServiceNode(nodes, (Element) node, serviceName);
        }
    }


    /**
     *  Find the service xml element for the given dataset node. First, we look for any  service nodes
     *  contained by the dataset node. If not found then we find the service name from the dataset node.
     *  If no service name attrbiute is found then  print an error and return null.  Now, we search the
     *  xml tree under the root node to find a service node with the given name. If not found then
     *  print an error and return null.
     *
     * @param datasetNode The dataset node to look for a service node for.
     * @param showErrors Do we tell the user if there was an error
     * @param type service type
     * @return Return the service node or null if not found.
     */
    private static Element findServiceNodeForDataset(Element datasetNode,
            boolean showErrors, String type) {
        double version = getVersion(datasetNode);
        String serviceName = findServiceNameForDataset(datasetNode, version,
                                 true);
        if (serviceName == null) {
            if (showErrors) {
                IdvChooser.errorMessage("Could not find service name");
            }
            return null;
        }

        List serviceNodes = new ArrayList();
        findChildServiceNode(serviceNodes, datasetNode, serviceName);
        if (serviceNodes.size() == 0) {
            if (showErrors) {
                IdvChooser.errorMessage("No service found with id = "
                                        + serviceName);
            }
            //            System.err.println (XmlUtil.toString (root));
            return null;
        }

        boolean typeWasNull = (type == null);
        if (type == null) {
            type = SERVICE_DODS + "|" + SERVICE_OPENDAP + "|" + SERVICE_ADDE
                   + "|" + SERVICE_RESOLVER + "|" + SERVICE_FILE;
        }
        type = type.toLowerCase();

        for (int i = 0; i < serviceNodes.size(); i++) {
            Element serviceNode = (Element) serviceNodes.get(i);

            //Now, we see if we have a compound service. If we do then we find a service node
            //with DODS as the service. This is a hack but for now it works.
            String serviceType = XmlUtil.getAttribute(serviceNode,
                                     ATTR_SERVICETYPE, NULL_STRING);

            if (serviceType == null) {
                continue;
            }
            serviceType = serviceType.toLowerCase();
            if (StringUtil.stringMatch(serviceType, type)) {
                return serviceNode;
            }
        }


        if (typeWasNull && (serviceNodes.size() > 0)) {
            return (Element) serviceNodes.get(0);
        }

        if (showErrors) {
            IdvChooser.errorMessage("No service found with id = "
                                    + serviceName);
        }
        //            System.err.println (XmlUtil.toString (root));
        return null;
    }


    /**
     *  Find the base url attribute from the service that the given datasetNode is
     *  associated with.
     *
     *  @param datasetNode The dataset node we are looking for a base url for.
     * @param root
     *  @return The base url for the given dataset node.
     */
    private static String findBaseForDataset(Element datasetNode,
                                             Element root) {

        //Find the service node
        Element serviceNode = findServiceNodeForDataset(datasetNode,  /*root,*/
            true, null);

        //If we couldn't find it then return null - the error message has been shown
        if (serviceNode == null) {
            return null;
        }

        //Pull out the base attribute
        String base = XmlUtil.getAttribute(serviceNode, ATTR_BASE,
                                           NULL_STRING);
        if (base == null) {
            IdvChooser.errorMessage("No base found for dataset.");
            return null;
        }

        return base;
    }

    /**
     * Recurse up the dom tree to find the node that has a catalogurl attribute
     *
     * @param node The node to look at
     *
     * @return  The foudn catalogurl attribute or null
     */
    private static String findCatalogSource(Element node) {
        String source = XmlUtil.getAttribute(node, ATTR_CATALOGURL,
                                             (String) null);
        if (source != null) {
            return source;
        }
        Node parent = node.getParentNode();
        if ((parent == null) || !(parent instanceof Element)) {
            return null;
        }
        return findCatalogSource((Element) parent);
    }

    /**
     *  Recurse up the DOM tree, looking for a dataset that contains a serviceName attribute.
     *  We also look at "access" nodes contained by the dataset node.
     *
     *  @param datasetNode The dataset node we are looking at.
     *  @param version The catalog version
     *  @param firstCall Is this the leaf node
     *  @return The name of the service that provides this dataset.
     */
    private static String findServiceNameForDataset(Element datasetNode,
            double version, boolean firstCall) {
        //First look for a service name attribute.
        String serviceName = XmlUtil.getAttribute(datasetNode,
                                 ATTR_SERVICENAME, NULL_STRING);
        if (serviceName != null) {
            return serviceName;
        }

        //Look for a contained access node
        Element accessNode = XmlUtil.findChild(datasetNode, TAG_ACCESS);
        if (accessNode != null) {
            serviceName = XmlUtil.getAttribute(accessNode, ATTR_SERVICENAME);
            if (serviceName != null) {
                return serviceName;
            }
        }

        serviceName = findServiceNameTagValue(datasetNode);
        if (serviceName != null) {
            return serviceName;
        }



        if (version >= 1.0) {
            return findServiceNameFromMetaData(datasetNode, version, true);
        } else {
            Element parent = (Element) datasetNode.getParentNode();
            //Only look at parent dataset nodes.
            if ((parent == null)
                    || !parent.getTagName().equals(TAG_DATASET)) {
                return null;
            }
            return findServiceNameForDataset(parent, version, false);
        }
    }

    /**
     * Recurse up the DOM tree, looking for a dataset that contains a
     * dataType attribute.
     *  We also look at "access" nodes contained by the dataset node.
     *
     * @param datasetNode The dataset node we are looking at.
     * @param root The root of the xml tree
     * @param version The catalog version
     * @param firstCall Is this the leaf node
     * @return The name of the service that provides this dataset.
     */
    private static String findDataTypeForDataset(Element datasetNode,
            Element root, double version, boolean firstCall) {
        //First look for a data type attribute.
        String dataType = XmlUtil.getAttributeFromTree(datasetNode,
                              ATTR_DATATYPE, null);

        if (dataType != null) {
            return dataType;
        }

        dataType = findTagValue(datasetNode, TAG_DATATYPE);
        if (dataType != null) {
            return dataType;
        }

        if (version >= 1.0) {
            return findTagValueFromMetaData(datasetNode, version, true,
                                            TAG_DATATYPE);
        } else {
            Element parent = (Element) datasetNode.getParentNode();
            //Only look at parent dataset nodes.
            if ((parent == null)
                    || !parent.getTagName().equals(TAG_DATASET)) {
                return null;
            }
            return findDataTypeForDataset(parent, root, version, false);
        }
    }

    /**
     *  Find the value of the serviceName tag which is a child of the
     * given datasetNode. If none found then return null.
     *
     * @param datasetNode The node to look under
     *
     * @return The service name or null if none found
     */
    private static String findServiceNameTagValue(Element datasetNode) {
        return findTagValue(datasetNode, TAG_SERVICENAME);
    }

    /**
     * Find the value of the which is a child of the given datasetNode.
     * If none found then return null.
     *
     * @param datasetNode The node to look under
     * @param tagName name of the tag to look for
     *
     * @return The tag or null if none found
     */
    private static String findTagValue(Element datasetNode, String tagName) {
        Element tagNode = XmlUtil.findChild(datasetNode, tagName);
        if (tagNode != null) {
            String value = XmlUtil.getChildText(tagNode);
            if (value != null) {
                if (value.trim().length() > 0) {
                    return value.trim();
                }
            }
        }
        return null;
    }

    /**
     * Find the value contained by the serviceName node contained by
     * a metadata node contained by the given datasetNode.
     * If this is not the first recursive call then only look at metadata
     * nodes that have inherited=true.
     *
     * @param datasetNode The data set node
     * @param version The catalog version
     * @param first Is this the first recursive call
     *
     * @return The service name or null if none found
     */
    private static String findServiceNameFromMetaData(Element datasetNode,
            double version, boolean first) {
        return findTagValueFromMetaData(datasetNode, version, first,
                                        TAG_SERVICENAME);
    }

    /**
     * Find the value of the tag from the metadata node
     *
     * @param datasetNode The data set node
     * @param version The catalog version
     * @param first Is this the first recursive call
     * @param tagName  name of the tag to search for
     *
     * @return the tag value or null if none found
     */
    private static String findTagValueFromMetaData(Element datasetNode,
            double version, boolean first, String tagName) {
        List children = XmlUtil.findChildren(datasetNode, TAG_METADATA);
        for (int i = 0; i < children.size(); i++) {
            Element metaDataNode = (Element) children.get(i);
            if ( !first
                    && !XmlUtil.getAttribute(metaDataNode, ATTR_INHERITED,
                                             false)) {
                continue;
            }
            String value = findTagValue(metaDataNode, tagName);
            if (value != null) {
                return value;
            }
        }


        Element parent = (Element) datasetNode.getParentNode();
        if ((parent == null) || !parent.getTagName().equals(TAG_DATASET)) {
            return null;
        }
        return findTagValueFromMetaData(parent, version, false, tagName);

    }

    /**
     * Find the base url for the given service node. If not found print an
     * error and return null. If found then look for the "suffix" attribute
     * of the service node. If found append it to the urlPath. Return the
     * base concatenated with the urlPath.
     *
     * @param serviceNode The  service node for the given urlPath.
     * @param urlPath The  tail end of the absolute url.
     * @return The full url path.
     */
    private static String getAbsoluteUrl(Element serviceNode,
                                         String urlPath) {
        String base = XmlUtil.getAttribute(serviceNode, ATTR_BASE,
                                           NULL_STRING);
        if (base == null) {
            IdvChooser.errorMessage("No base found for dataset.");
            return null;
        }
        String suffix = XmlUtil.getAttribute(serviceNode, ATTR_SUFFIX,
                                             NULL_STRING);
        if (suffix != null) {
            urlPath = urlPath + suffix;
        }


        base = base + urlPath;
        String catalogSource = findCatalogSource(serviceNode);
        if (catalogSource != null) {
            base = XmlTree.expandRelativeUrl(base, catalogSource);
        }



        return base;
    }


    /**
     *  Lookup and return the urlPath defined for the given datasetNode.
     *
     *  @param datasetNode The dataset node we are looking at.
     *  @return The url path for the dataset node.
     */
    public static String getUrlPath(Element datasetNode) {
        String urlPath = XmlUtil.getAttribute(datasetNode, ATTR_URLPATH,
                             NULL_STRING);

        //   <dataset name="Model data" dataType="Grid">
        //      <service serviceType="DODS" name="mlode" base="http://motherlode.ucar.edu/cgi-bin/dods/nph-nc/"/>
        //      <dataset name="NCEP AVN-Q model data">
        //         <dataset name="NCEP AVN-Q 2002-12-20 18:00:00 GMT" serviceName="mlode" urlPath="dods/model/2002122018_avn-q.nc"/>


        //If no urlPath attribute look for a contained access node which holds a urlPath
        if (urlPath == null) {
            Element accessNode = XmlUtil.findChild(datasetNode, TAG_ACCESS);
            if (accessNode != null) {
                urlPath = XmlUtil.getAttribute(accessNode, ATTR_URLPATH);
            }
        }
        return urlPath;
    }


    /**
     * This reads the xml pointed to by the given resolverUrl. It flags an
     * error if the url is bad, the xml is bad, the xml contains 0 dataset
     * nodes, the xml contains more than one dataset node. It returns an
     * array of object which contain:
     *  <pre>
     *        Object[] {newXmlRoot, datasetNode, serviceNode, url}
     *  </pre>
     *
     *  @param resolverUrl The url pointing to the resolver catalog.
     * @param properties The properties
     *  @return Array of root element, dataset node, service node and the
     *          absolute url of the data.
     */
    private static Object[] getResolverData(String resolverUrl,
                                            Hashtable properties) {
        Element newRoot = null;
        try {
            String contents = IOUtil.readContents(resolverUrl);
            if (contents == null) {
                IdvChooser.errorMessage("Failed to read the catalog:"
                                        + resolverUrl);
                return null;
            }
            newRoot = XmlUtil.getRoot(contents);
            newRoot.setAttribute(ATTR_CATALOGURL, resolverUrl);
        } catch (Exception exc) {
            IdvChooser.errorMessage("Error reading catalog:" + resolverUrl
                                    + "\n" + exc);
            return null;
        }
        if (newRoot == null) {
            IdvChooser.errorMessage("Failed to retrieve the catalog:"
                                    + resolverUrl);
            return null;
        }

        List datasetNodes = XmlUtil.findDescendants(newRoot, TAG_DATASET);
        if (datasetNodes.size() == 0) {
            IdvChooser.errorMessage("No dataset nodes found in the  catalog:"
                                    + resolverUrl);
            return null;
        }
        if (datasetNodes.size() > 1) {
            IdvChooser.errorMessage(
                "Too many dataset nodes found in the  catalog:"
                + resolverUrl);
            return null;
        }
        Element datasetNode = (Element) datasetNodes.get(0);
        Element serviceNode = findServiceNodeForDataset(datasetNode, false,
                                  null);

        if (serviceNode == null) {
            IdvChooser.errorMessage("Could not find service node");
            return null;
        }
        String urlPath = getUrlPath(datasetNode);
        if (properties != null) {
            addServiceProperties(datasetNode, properties, urlPath);
        }
        return new Object[] { newRoot, datasetNode, serviceNode,
                              getAbsoluteUrl(serviceNode, urlPath) };
    }

    /**
     *  The given resolverUrl should return a catalog that holds one dataset. This method returns
     *  the absolute url that that catalog holds. If the given properties is no null then
     *  this will also try to extract the title from the xml and will put the PROP_TITLE into the
     *  properties.
     *
     *  @param resolverUrl The url pointing to the resolved catalog.
     *  @param properties To put the title into.
     *  @return The absolute url that the resolverUrl resolves to (may be null).
     */
    public static String resolveUrl(String resolverUrl,
                                    Hashtable properties) {

        Object[] result = getResolverData(resolverUrl, properties);
        if (result == null) {
            return null;
        }
        if (properties != null) {
            String title = getTitleFromDataset((Element) result[1]);
            if (title != null) {
                properties.put(DataSource.PROP_TITLE, title);
            }
        }
        return (String) result[3];
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
        NodeList elements = XmlUtil.getElements(parent, TAG_PROPERTY);
        for (int i = 0; i < elements.getLength(); i++) {
            Node child = (Node) elements.item(i);
            String nameValue = XmlUtil.getAttribute(child, ATTR_NAME,
                                   (String) null);
            if (nameValue == null) {
                continue;
            }
            if ( !nameValue.equals(nameValueLookingFor)) {
                continue;
            }
            String valueValue = XmlUtil.getAttribute(child, ATTR_VALUE,
                                    (String) null);
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

        String urlPath = getUrlPath(datasetNode);

        if (urlPath != null) {
            Element serviceNode = findServiceNodeForDataset(datasetNode,
                                      flagMissing, null);
            if (serviceNode == null) {
                return false;
            }

            String url = getAbsoluteUrl(serviceNode, urlPath);
            if (url == null) {
                if (flagMissing) {
                    IdvChooser.errorMessage(
                        "Could not read any dataset urls");
                }
                return false;
            }

            //If this is a "Resolver" service type then the url points to a catalog 
            //that holds the real url.
            String    serviceType = getServiceType(serviceNode);
            Hashtable properties  = new Hashtable();
            properties.put(PROP_CATALOGURL, path);
            String groupId = getPropertyAttributeFromChild(datasetNode,
                                 "group", null);
            if (groupId != null) {
                properties.put(PROP_DATASETGROUP, groupId);
            }

            String datasetId = XmlUtil.getAttribute(datasetNode,
                                   ATTR_DATASETID, (String) null);
            if (datasetId != null) {
                properties.put(PROP_DATASETID, datasetId);
            }


            String annotationServer =
                getPropertyAttributeFromChild(datasetNode,
                    VALUE_ANNOTATIONSERVER, null);
            if (annotationServer != null) {
                properties.put(PROP_ANNOTATIONSERVER, annotationServer);
            }

            addServiceProperties(datasetNode, properties, urlPath);


            getProperties(datasetNode, serviceNode, properties);
            if (SERVICE_RESOLVER.equals(serviceType)) {
                String   resolverUrl = url;
                Object[] result = getResolverData(resolverUrl, properties);
                if (result == null) {
                    return false;
                }
                datasetNode = (Element) result[1];
                serviceNode = (Element) result[2];
                url         = (String) result[3];
                properties.put(DataSource.PROP_RESOLVERURL, resolverUrl);
                String title = getTitleFromDataset((Element) datasetNode);
                if ((title != null) && (properties != null)) {
                    properties.put(DataSource.PROP_TITLE, title);
                }
            }
            //      System.err.println ("Absolute url:" + url);
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
            List children = XmlUtil.findChildren(datasetNode, TAG_DATASET);
            for (int i = 0; i < children.size(); i++) {
                Element childDatasetNode = (Element) children.get(i);
                urlPath = XmlUtil.getAttribute(childDatasetNode,
                        ATTR_URLPATH, NULL_STRING);
                if (urlPath != null) {
                    String base = findBaseForDataset(childDatasetNode, root);
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
     * Add any service urls to the properties
     *
     * @param datasetNode data set node
     * @param properties properties
     * @param urlPath base url
     */
    private static void addServiceProperties(Element datasetNode,
                                             Hashtable properties,
                                             String urlPath) {


        Element dataServiceNode;

        dataServiceNode = findServiceNodeForDataset(datasetNode,  /*root,*/
                false, SERVICE_HTTP);
        if (dataServiceNode != null) {
            String serviceUrl = getAbsoluteUrl(dataServiceNode, urlPath);
            if (serviceUrl != null) {
                properties.put(DataSource.PROP_SERVICE_HTTP, serviceUrl);
            }
        }
    }


    /**
     * Generate an html representation of the catalog
     *
     * @param root Root of the catalog
     * @param datasetNode The data set node we are looking at
     * @param cnt The current count of the data set nodes we have processed
     * @param bundleTemplate The bundle template we generate the bundle from
     * @param jnlpTemplate The jnlp template
     *
     * @return The current count
     */
    public static int generateHtml(Element root, Element datasetNode,
                                   int cnt, String bundleTemplate,
                                   String jnlpTemplate) {
        cnt++;
        String  name        = XmlUtil.getAttribute(datasetNode, "name");
        Element serviceNode = findServiceNodeForDataset(datasetNode,  /*root,*/
            false, null);
        if (serviceNode == null) {
            System.out.println("<li> " + name + "\n");
        } else {
            String    serviceType = getServiceType(serviceNode);
            Hashtable properties  = new Hashtable();
            boolean   isResolver  = (SERVICE_RESOLVER.equals(serviceType));
            if ( !isResolver) {
                String urlPath    = getUrlPath(datasetNode);
                String dataUrl    = getAbsoluteUrl(serviceNode, urlPath);
                String jnlpFile   = "generated" + cnt + ".jnlp";
                String bundleFile = "generated" + cnt + ".xidv";
                System.out.println("<li> <a href=\"" + jnlpFile + "\">"
                                   + name + "</a>\n");
                try {
                    String bundle = StringUtil.replace(bundleTemplate,
                                        "%datasource%", dataUrl);
                    bundle = StringUtil.replace(bundle, "%title%", name);
                    IOUtil.writeFile(bundleFile, bundle);
                    String jnlp = StringUtil.replace(jnlpTemplate, "%title%",
                                      "Generated bundle for:" + name);
                    jnlp = StringUtil.replace(jnlp, "%jnlpfile%", jnlpFile);
                    jnlp = StringUtil.replace(
                        jnlp, "%bundle%",
                        "http://www.unidata.ucar.edu/projects/metapps/testgen/"
                        + bundleFile);
                    IOUtil.writeFile(jnlpFile, jnlp);
                } catch (Exception exc) {
                    System.err.println("error:" + exc);
                    System.exit(1);
                }
            }
        }

        List children = XmlUtil.findChildren(datasetNode, TAG_DATASET);
        for (int i = 0; i < children.size(); i++) {
            if (i == 0) {
                System.out.println("<ul>");
            }
            Element child = (Element) children.get(i);
            cnt = generateHtml(root, child, cnt, bundleTemplate,
                               jnlpTemplate);
        }
        if (children.size() > 0) {
            System.out.println("</ul>");
        }
        return cnt;
    }

    /**
     * Test the html generation
     *
     * @param args Command line args
     */
    public static void main(String[] args) {
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
            List children = XmlUtil.findChildren(root, TAG_DATASET);
            for (int i = 0; i < children.size(); i++) {
                Element child = (Element) children.get(i);
                generateHtml(root, child, 0, bundleTemplate, jnlpTemplate);
            }

            System.out.println("</ul>");
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
        }
        System.exit(0);

    }





}

