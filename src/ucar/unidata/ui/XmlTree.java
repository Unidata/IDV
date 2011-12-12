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

package ucar.unidata.ui;


import org.w3c.dom.Attr;



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlNodeList;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;



import java.io.*;

import java.net.URL;

import java.util.ArrayList;
import java.util.Enumeration;

import java.util.Hashtable;
import java.util.List;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;


/**
 *  A generic JTree that displays an xml document.
 *  Can be configured in a variety of ways to show or not show
 *  certain tags, etc.
 */

public class XmlTree extends JTree {


    /** xml attribute */
    public static final String ATTR_NAME = "name";

    /** xml attribute */
    public static final String ATTR_LABEL = "label";

    /** xml attribute */
    public static final String ATTR_XLINKHREF = "xlink:href";

    /** the null string */
    public static final String NULL_STRING = null;


    /** Icon used for the leafs of the jtree */
    static ImageIcon leafIcon;

    /** The root of the dom we are diplaying */
    private Element xmlRoot;

    /** The jtree root */
    private XmlTreeNode treeRoot;

    /** The tree model */
    private DefaultTreeModel treeModel;


    /** A map from dom element to tree node */
    private Hashtable elementToNode = new Hashtable();

    /**
     * Collection of xml tag names that are not to be added to the jtree but we should just descend their
     *   children and keep processing
     */
    private Hashtable tagsToNotProcessButRecurse = null;

    /** You can specify image icons for particular tag names. This holds the mapping. */
    private Hashtable tagToIcons = new Hashtable();

    /** If true then add all of the attributes of each node as jtree children nodes */
    private boolean includeAttributes = false;


    /** If set then only tags in this list are recursed */
    private Hashtable tagsToRecurse = null;

    /** If set then only tags in this list are processed */
    private Hashtable tagsToProcess = null;

    /** Don't process the children trees of tags in this list */
    private Hashtable tagsToNotRecurse = null;

    /** Don't process the tags in this list */
    private Hashtable tagsToNotProcess = null;

    /** Define the name of the xml attribute that should be used for the label for certain tags */
    private Hashtable tagNameToLabelAttr = null;

    /** You can specify that the label for a certain tag is gotten from a child of the tag */
    private Hashtable tagNameToLabelChild = null;

    /** Where do we get the tooltip for a tag */
    private Hashtable tagNameToTooltipChild = null;

    /** Defines the tag names that are xlinks to other xml docs */
    private Hashtable xlinkTags = null;

    /**
     *  The useTagNameAsLabel property.
     */
    private boolean useTagNameAsLabel;





    /** gui */
    private JScrollPane scroller;

    /** Should the first level of the jtree be opened */
    private boolean openFirstLevel = false;

    /** have done initialization */
    private boolean haveInitialized = false;

    /** where we came from */
    private String baseUrlPath;

    /**
     * ctor
     *
     * @param xmlRoot The root of the xml dom tree
     *
     */
    public XmlTree(Element xmlRoot) {
        this(xmlRoot, false);
    }


    /**
     * ctor
     *
     * @param xmlRoot The root of the xml dom tree
     * @param openFirstLevel Should the first level of the jtree be opened
     *
     */
    public XmlTree(Element xmlRoot, boolean openFirstLevel) {
        this(xmlRoot, openFirstLevel, null);
    }


    /**
     * ctor
     *
     * @param xmlRoot The root of the xml dom tree
     * @param openFirstLevel Should the first level of the jtree be opened
     * @param basePath Where the xml came from
     *
     */
    public XmlTree(Element xmlRoot, boolean openFirstLevel, String basePath) {
        setToolTipText(" ");
        baseUrlPath         = basePath;
        this.xmlRoot        = xmlRoot;
        this.openFirstLevel = openFirstLevel;
        setMultipleSelect(false);
        setShowsRootHandles(true);
        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                treeClick(event);
            }
        });
        setCellRenderer(new MyRenderer(this));
    }

    /**
     * Class MyRenderer is used to return the correct image icon for certain jtree nodes
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    public class MyRenderer extends DefaultTreeCellRenderer {

        /** the tree */
        XmlTree xmlTree;

        /**
         * ctor
         *
         * @param xmlTree The tree
         *
         */
        public MyRenderer(XmlTree xmlTree) {
            this.xmlTree = xmlTree;
        }

        /**
         * Get the tree cell renderer
         *
         * @param tree tree
         * @param value value
         * @param sel sel
         * @param expanded expanded
         * @param leaf leaf
         * @param row row
         * @param hasFocus hasFocus
         * @return The renderer component
         */
        public Component getTreeCellRendererComponent(JTree tree,
                Object value, boolean sel, boolean expanded, boolean leaf,
                int row, boolean hasFocus) {

            super.getTreeCellRendererComponent(tree, value, sel, expanded,
                    leaf, row, hasFocus);
            setIcon(null);
            if (value instanceof XmlTreeNode) {
                XmlTreeNode treeNode = (XmlTreeNode) value;
                if (treeNode.getXmlNode() != null) {
                    ImageIcon icon = getIconForNode(treeNode.getXmlNode());
                    if (icon == null) {
                        icon = xmlTree.getIcon(treeNode.getXmlNode(), leaf);
                    }
                    if (icon != null) {
                        setIcon(icon);
                    }
                }
            }
            return this;
        }
    }

    /**
     * Get the icon for a node
     *
     * @param node  the node
     *
     * @return null
     */
    public ImageIcon getIconForNode(Element node) {
        return null;
    }


    /**
     * Get the tooltip at the mouse
     *
     * @param event Where the mouse is
     * @return The tooltip text
     */
    public String getToolTipText(MouseEvent event) {
        Element n = getXmlNodeAt(event.getX(), event.getY());
        if (n == null) {
            return null;
        }


        if (tagNameToTooltipChild != null) {
            String childTag =
                (String) tagNameToTooltipChild.get(XmlUtil.getLocalName(n));
            if (childTag != null) {
                Element child = XmlUtil.getElement(n, childTag);
                if (child != null) {
                    String text = XmlUtil.getChildText(child);
                    if ((text != null) && (text.length() > 50)) {
                        text = "<html>"
                               + StringUtil.breakText(text, "<br>", 50)
                               + "</html>";
                    }
                    return text;
                }
            }
        }

        return getToolTipText(n);
    }


    /**
     * A hook to allow subclasses to get tooltip text for a particular xml element
     *
     * @param n The xml element
     *
     * @return The tooltip text
     */
    public String getToolTipText(Element n) {
        return null;
    }



    /**
     * init me
     */
    private void init() {
        haveInitialized = true;
        treeRoot        = new XmlTreeNode(null, "", baseUrlPath);
        treeModel       = new DefaultTreeModel(treeRoot);
        setModel(treeModel);
        setRootVisible(false);
        loadTree();
    }


    /**
     * Reload the xml into the jtree
     */
    public void loadTree() {
        if ( !haveInitialized) {
            init();
        }

        if (xmlRoot != null) {
            treeRoot.removeAllChildren();
            process(treeRoot, xmlRoot);
            treeModel.nodeStructureChanged(treeRoot);
            if (openFirstLevel) {
                for (Enumeration children = treeRoot.children();
                        children.hasMoreElements(); ) {
                    DefaultMutableTreeNode child =
                        (DefaultMutableTreeNode) children.nextElement();
                    expandPath(new TreePath(new Object[] { treeRoot,
                            child }));
                }
            }
        }
    }



    /**
     *  Set the UseTagNameAsLabel property.
     *
     *  @param value The new value for UseTagNameAsLabel
     */
    public void setUseTagNameAsLabel(boolean value) {
        useTagNameAsLabel = value;
    }

    /**
     *  Get the UseTagNameAsLabel property.
     *
     *  @return The UseTagNameAsLabel
     */
    public boolean getUseTagNameAsLabel() {
        return useTagNameAsLabel;
    }



    /**
     *  Set the IncludeAttributes property.
     *
     *  @param value The new value for IncludeAttributes
     */
    public void setIncludeAttributes(boolean value) {
        includeAttributes = value;
    }

    /**
     *  Get the IncludeAttributes property.
     *
     *  @return The IncludeAttributes
     */
    public boolean getIncludeAttributes() {
        return includeAttributes;
    }



    /**
     *  Return a list of the xml Element nodes that have been selected.
     *
     *  @return List of selected nodes.
     */
    public List getSelectedElements() {
        List       l     = new ArrayList();
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if (paths != null) {
            for (int i = 0; i < paths.length; i++) {
                l.add(getXmlNodeAtPath(paths[i]));
            }
        }
        return l;
    }


    /**
     * Select in the jtree the node that corresponds to the given xml element
     *
     * @param element The xml element to select
     */
    public void selectElement(Element element) {
        TreeNode node = (TreeNode) elementToNode.get(element);
        if (node == null) {
            return;
        }
        TreePath treePath = new TreePath(treeModel.getPathToRoot(node));
        addSelectionPath(treePath);
        scrollPathToVisible(treePath);
    }


    /**
     *  Find the xml element that corresponds to the selected jtree node
     *
     * @return Selected xml element or null  if none selected
     */
    public Element getSelectedElement() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return null;
        }
        return getXmlNodeAtPath(paths[0]);
    }

    /**
     *  Return the root element  of the xml dom
     *
     * @return root element  of the xml dom
     */
    public Element getXmlRoot() {
        return xmlRoot;
    }


    /**
     * Set the root and reinitialize
     *
     * @param newRoot The new xml root
     */
    public void setXmlRoot(Element newRoot) {
        xmlRoot = newRoot;
        //Reinitialize
        init();
    }


    /**
     *  Gets called when the tree is clicked.
     *
     * @param event Mouse event
     */
    protected void treeClick(MouseEvent event) {
        XmlTreeNode node = getXmlTreeNodeAt(event.getX(), event.getY());
        if (node == null) {
            return;
        }
        Element element = node.getXmlNode();
        if (element == null) {
            return;
        }
        if (SwingUtilities.isRightMouseButton(event)) {
            doRightClick(this, node, element, event);
        } else if (event.getClickCount() > 1) {
            doDoubleClick(this, node, element);
        } else {
            doClick(this, node, element);
        }
    }


    /**
     *  Gets called when an Xml Element has been double clicked.
     *
     * @param tree The tree (this).
     * @param node The node that was clicked on
     * @param element The corresponding xml element
     */
    public void doDoubleClick(XmlTree tree, XmlTreeNode node,
                              Element element) {
        doDoubleClick(tree, element);
    }

    /**
     *  Gets called when an Xml Element has been double clicked.
     *
     * @param tree The tree (this)
     * @param element The xml element
     */
    public void doDoubleClick(XmlTree tree, Element element) {}


    /**
     * tree node was clicked
     *
     * @param tree The tree (this)
     * @param node Tree node that was clicked
     * @param element Corresponding xml node
     */
    public void doClick(XmlTree tree, XmlTreeNode node, Element element) {
        doClick(tree, element);
    }

    /**
     * tree node was clicked
     *
     * @param tree The tree (this)
     * @param element Corresponding xml node
     */
    public void doClick(XmlTree tree, Element element) {}


    /**
     * Handle right click
     *
     * @param tree The tree (this)
     * @param node Tree node that was clicked
     * @param element Corresponding xml node
     * @param event The mouse event
     */
    public void doRightClick(XmlTree tree, XmlTreeNode node, Element element,
                             MouseEvent event) {
        doRightClick(tree, element, event);
    }

    /**
     * Handle right click
     *
     * @param tree The tree (this)
     * @param element Corresponding xml node
     * @param event The mouse event
     */
    public void doRightClick(XmlTree tree, Element element,
                             MouseEvent event) {}



    /**
     *  Return the gui component. This has to be called because
     *  we create the JTree here. (We don't at construction time because of the
     *  tagsToRecurse/Process setting).
     *
     * @return The contents
     */
    public Component getContents() {
        if ( !haveInitialized) {
            init();
        }
        return this;
    }

    /**
     * Get the scrollpane the xmltree is in
     *
     * @return The scroller
     */
    public JScrollPane getScroller() {
        if (scroller == null) {
            scroller = GuiUtils.makeScrollPane(getContents(), 200, 300);
        }
        return scroller;
    }


    /**
     *  A utility method that adds the objects (Usually tag names)
     *  in the given tags list into the given hashtable. If the hashtable
     *  is null it creates a new one.
     *
     * @param ht The hashtable
     * @param tags List of tags to add
     * @return the ht argument (or the newly constructed ht of the ht arg is null)
     */
    private Hashtable addToTable(Hashtable ht, List tags) {
        if (ht == null) {
            ht = new Hashtable();
        }
        for (int i = 0; i < tags.size(); i++) {
            ht.put(tags.get(i), tags.get(i));
        }
        return ht;
    }


    /**
     *  Define the name of the attribute to use for a label for elements
     *  with the given tag name
     *
     * @param tagName The tag name
     * @param attrName Attribute that defines the label
     */
    public void defineLabelAttr(String tagName, String attrName) {
        if (tagNameToLabelAttr == null) {
            tagNameToLabelAttr = new Hashtable();
        }
        tagNameToLabelAttr.put(tagName, attrName);
    }

    /**
     * Where do we get the label for the tag
     *
     * @param tagName The tag name
     * @param childTag Tag name of child node to look for label
     */
    public void defineLabelChild(String tagName, String childTag) {
        if (tagNameToLabelChild == null) {
            tagNameToLabelChild = new Hashtable();
        }
        tagNameToLabelChild.put(tagName, childTag);
    }


    /**
     * Where do we get the tooltip text
     *
     * @param tagName The tag name
     * @param childTag Tag of the child where we get tooltip text
     */
    public void defineTooltipChild(String tagName, String childTag) {
        if (tagNameToTooltipChild == null) {
            tagNameToTooltipChild = new Hashtable();
        }
        tagNameToTooltipChild.put(tagName, childTag);
    }


    /**
     * Define a tag name that holds xlink references to other xml files
     *
     * @param tagName The tag name
     */
    public void addXlinkTag(String tagName) {
        if (xlinkTags == null) {
            xlinkTags = new Hashtable();
        }
        xlinkTags.put(tagName, tagName);
    }

    /**
     * Is the tag an xlink holder
     *
     * @param tag The tag
     * @return Is tag an xlink holder
     */
    private boolean isXlinkTag(String tag) {
        if (xlinkTags == null) {
            return false;
        }
        return (xlinkTags.get(tag) != null);
    }

    /**
     *  Define the set of tags whose child elements we should process
     *
     * @param tags List of tag names
     */
    public void addTagsToRecurse(List tags) {
        tagsToRecurse = addToTable(tagsToRecurse, tags);
    }


    /**
     * Associate the icon with the tag
     *
     * @param icon The icon
     * @param tagName The tag name
     */
    public void setIconForTag(ImageIcon icon, String tagName) {
        tagToIcons.put(tagName, icon);
    }

    /**
     *  Define a tag whose child elements we should process
     *
     * @param tag The tag name
     */
    public void addTagToRecurse(String tag) {
        addTagsToRecurse(Misc.newList(tag));
    }

    /**
     *  Define the set of tags who we should process
     *
     * @param tags List of tag names
     */
    public void addTagsToProcess(List tags) {
        tagsToProcess = addToTable(tagsToProcess, tags);
    }

    /**
     *  Define a tag who we should process
     *
     * @param tag the tag name
     */
    public void addTagToProcess(String tag) {
        addTagsToProcess(Misc.newList(tag));
    }

    /**
     *  Define the set of tags that we don't want to add to the jtree but do want to recurse
     *
     * @param tags List of tag names
     */
    public void addTagsToNotProcessButRecurse(List tags) {
        tagsToNotProcessButRecurse = addToTable(tagsToNotProcessButRecurse,
                tags);
    }


    /**
     *  Define the set of tags whose children we should NOT  process
     *
     * @param tags List of tag names
     */
    public void addTagsToNotRecurse(List tags) {
        tagsToNotRecurse = addToTable(tagsToNotRecurse, tags);
    }

    /**
     *  Define a tag whose children we should NOT  process
     *
     * @param tag The tag name
     */
    public void addTagToNotRecurse(String tag) {
        addTagsToNotRecurse(Misc.newList(tag));
    }

    /**
     *  Define the set of tags  we should NOT  process
     *
     * @param tags List of tag names
     */
    public void addTagsToNotProcess(List tags) {
        tagsToNotProcess = addToTable(tagsToNotProcess, tags);
    }

    /**
     *  Define a tag we should NOT  process
     *
     * @param tag The tag name
     */
    public void addTagToNotProcess(String tag) {
        addTagsToNotProcess(Misc.newList(tag));
    }




    /**
     * Fire the event
     *
     * @param treePath  Expanding path
     *
     * @throws ExpandVetoException on badness
     */
    public void fireTreeWillExpand(TreePath treePath)
            throws ExpandVetoException {
        Object[] path = treePath.getPath();
        if ((path.length > 0)
                && (path[path.length - 1] instanceof XlinkTreeNode)) {
            ((XlinkTreeNode) path[path.length - 1]).checkExpansion();
        }
        super.fireTreeWillExpand(treePath);
    }

    /**
     * Class XmlTreeNode
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    public static class XmlTreeNode extends DefaultMutableTreeNode {

        /** Corresponding xml node */
        Element xmlNode;

        /** base path */
        String baseLocation;

        /**
         * ctor
         *
         * @param node The xml node
         * @param name The label to use
         *
         */
        public XmlTreeNode(Element node, String name) {
            this(node, name, null);
        }

        /**
         * ctor
         *
         * @param node The xml node
         * @param name The label to use
         * @param baseLocation Where the xml came from. May be null.
         */
        public XmlTreeNode(Element node, String name, String baseLocation) {
            super(name);
            this.xmlNode      = node;
            this.baseLocation = baseLocation;
        }

        /**
         * Get the node
         *
         * @return The xml node
         */
        public Element getXmlNode() {
            return xmlNode;
        }

        /**
         * Find the xml file location. Recurse up the tree if needed.
         *
         * @return Where the xml came from where I came from
         */
        public String getBaseLocation() {
            String      parentBaseLocation = null;
            XmlTreeNode parent             = (XmlTreeNode) getParent();
            if (parent != null) {
                parentBaseLocation = parent.getBaseLocation();
            }
            return expandRelativeUrl(baseLocation, parentBaseLocation);
        }
    }


    /**
     * Class XlinkTreeNode. Represents xlink nodes
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    public static class XlinkTreeNode extends XmlTreeNode {

        /** Have I loaded */
        private boolean haveLoaded = false;

        /** The tree I am in */
        XmlTree tree;

        /** Where I point to */
        String href;


        /**
         * ctor
         *
         * @param node the xml node
         * @param tree The tree I am in
         * @param name My name
         * @param href Where I put to
         *
         */
        public XlinkTreeNode(Element node, XmlTree tree, String name,
                             String href) {
            super(node, name, href);
            this.href = href;
            this.tree = tree;
        }


        /**
         * Expand if needed
         */
        public void checkExpansion() {
            if (haveLoaded) {
                return;
            }
            haveLoaded = true;
            //Run it in a thread
            Misc.run(this, "checkExpansionInner");
        }


        /**
         * expand if needed. This gets called in a thread from the above method.
         */
        public void checkExpansionInner() {
            tree.expandXlink(this, getXlinkHref());
        }

        /**
         * Have I loaded my doc
         *
         * @return loaded my doc
         */
        public boolean getHaveLoaded() {
            return haveLoaded;
        }

        /**
         * Get the xlink:href url
         *
         * @return  the url
         */
        public String getXlinkHref() {
            String      baseLocation = null;
            XmlTreeNode parent       = (XmlTreeNode) getParent();
            if (parent != null) {
                baseLocation = parent.getBaseLocation();
            }
            String xmlUrl = expandRelativeUrl(href, baseLocation);
            //            System.err.println("base:" + baseLocation +" href:" + href);
            //            System.err.println("href url:" + xmlUrl);
            return xmlUrl;
        }


        /**
         * Where I point to
         *
         * @return Where I point to
         */
        public String getHref() {
            return getXlinkHref();
        }

    }

    /**
     * Expand the relative url
     *
     * @param node  the node
     * @param href  the base href
     *
     * @return  the expanded URL
     */
    public String expandRelativeUrl(XmlTreeNode node, String href) {
        String base = node.getBaseLocation();
        if (base != null) {
            return expandRelativeUrl(href, base);
        }
        return expandRelativeUrl(href);
    }



    /**
     * Utility to expand a relative url wrt to a base url
     *
     * @param href The (potentially) relative url
     *
     * @return Fully qualified url
     */
    public String expandRelativeUrl(String href) {
        return expandRelativeUrl(href, baseUrlPath);
    }

    /**
     * Utility to expand a relative url wrt to a base url
     *
     * @param href The (potentially) relative url
     * @param baseUrlPath The base path
     *
     * @return Fully qualified url
     */
    public static String expandRelativeUrl(String href, String baseUrlPath) {
        if (href == null) {
            return baseUrlPath;
        }
        if (baseUrlPath == null) {
            return href;
        }
        try {
            //See if is a well formed url
            URL url = new URL(href);
            return href;
        } catch (Exception badHrefException) {
            //Its not a well formed url
            //is it an absolute path
            if (href.startsWith("/")) {
                try {
                    //See if the baseUrlPath is well formed
                    URL url = new URL(baseUrlPath);
                    //don't add the port if there isn't one
                    int    port       = url.getPort();
                    String portString = "";
                    if (port != -1) {
                        portString = ":" + port;
                    }
                    //If it is then construct the full
                    return url.getProtocol() + "://" + url.getHost() +
                    //":" + url.getPort() + href
                    portString + href;
                } catch (Exception exc) {
                    //Humm, here the baseurl is not well formed so we'll just return the href
                    return href;
                }
            }
            //Here the href does not begin with "/" so we strip off the end
            //of the the baseurl and append the href
            int idx = baseUrlPath.lastIndexOf("/");
            if (idx >= 0) {
                String base = baseUrlPath.substring(0, idx);
                //Add a trailing "/" if needed
                if (base.lastIndexOf("/") != base.length() - 1) {
                    base = base + "/";
                }
                href = base + href;
            }
        }
        return href;
    }


    /**
     * Process the xlink href
     *
     * @param href Points to the xml file
     *
     * @return The new document
     *
     * @throws Exception On badness
     */
    protected Document readXlinkXml(String href) throws Exception {
        String xml = IOUtil.readContents(href, getClass());
        if (xml == null) {
            return null;
        }
        return XmlUtil.getDocument(xml);
    }

    /**
     * When we load in an xlinked document how far down do we go before
     * we start displaying the nodes
     *
     * @return import level
     */
    protected int getXlinkImportLevel() {
        return 0;
    }

    /**
     * expand the xlink node
     *
     * @param node The node
     * @param href The href to the xml doc
     */
    protected void expandXlink(final XlinkTreeNode node, String href) {
        try {
            href = expandRelativeUrl(href);
            Document document = readXlinkXml(href);
            node.removeAllChildren();
            //if the document was null then just return
            if (document == null) {
                treeModel.nodeStructureChanged(node);
                return;
            }
            Element newRoot = document.getDocumentElement();
            if ( !initXlinkRoot(newRoot, document, href)) {
                return;
            }
            NodeList importElements = getXlinkImportElements(newRoot);
            for (int i = 0; i < importElements.getLength(); i++) {
                Element child = (Element) importElements.item(i);
                /*
                child =
                    (Element) node.getXmlNode().getOwnerDocument().importNode(
                        child, true);
                        node.getXmlNode().appendChild(child);*/
                process(node, child);
            }
            final TreePath path = new TreePath(treeModel.getPathToRoot(node));
            GuiUtils.invokeInSwingThread(new Runnable() {
                public void run() {
                    expandPath(path);
                    treeModel.nodeStructureChanged(node);
                    repaint();
                }
            });
        } catch (Throwable exc) {
            LogUtil.logException("Expanding xlink node:" + href, exc);
        }
    }


    /**
     * Find the xml elements to use when we have an xlink to an xml doc
     *
     * @param root  get the xlink elements
     * @return element to use
     */
    public NodeList getXlinkImportElements(Element root) {
        NodeList importElements = null;
        int      importLevel    = getXlinkImportLevel();
        if (importLevel == 1) {
            importElements = XmlUtil.getElements(root);
        } else if (importLevel == 2) {
            importElements = XmlUtil.getGrandChildren(root);
            if (importElements.getLength() == 0) {
                importElements = XmlUtil.getElements(root);
            }
            if (importElements.getLength() == 0) {
                ((XmlNodeList) importElements).add(root);
            }
        } else {
            importElements = new XmlNodeList();
            ((XmlNodeList) importElements).add(root);
        }

        return importElements;
    }



    /**
     * Allows derived classes to initialize the xlink loaded xml
     *
     * @param root The root of the xlink loaded xml
     * @param doc The document the xml was created with
     * @param url The url
     * @return true if the xlink tree should be added
     */
    protected boolean initXlinkRoot(Element root, Document doc, String url) {
        return true;
    }


    /**
     *  Should we show the given xml Element
     *
     * @param xmlNode
     * @return Should we look at this node and turn it into a jtree node
     */
    protected boolean shouldProcess(Element xmlNode) {
        String tagName = XmlUtil.getLocalName(xmlNode);
        if (tagsToProcess != null) {
            if (tagsToProcess.get(tagName) == null) {
                return false;
            }
        }

        if (tagsToNotProcess != null) {
            return (tagsToNotProcess.get(XmlUtil.getLocalName(xmlNode))
                    == null);
        }
        return true;
    }



    /**
     * Get the icon used for the node
     *
     * @param xmlNode The node
     * @param isLeaf Is it a leaf node
     * @return The icon or null
     */
    protected ImageIcon getIcon(Element xmlNode, boolean isLeaf) {
        String    tagName = XmlUtil.getLocalName(xmlNode);
        ImageIcon icon    = (ImageIcon) tagToIcons.get(tagName);
        if (icon != null) {
            return icon;
        }

        if (leafIcon == null) {
            leafIcon =
                GuiUtils.getImageIcon("/ucar/unidata/ui/images/bullet.gif",
                                      getClass());
        }
        return (isLeaf
                ? leafIcon
                : null);
    }


    /**
     *  Walk the xml tree at the given xmlNode and create the JTree
     *
     * @param parentTreeNode The parent jtree node
     * @param xmlNode The xml node to process
     */
    protected void process(XmlTreeNode parentTreeNode, Element xmlNode) {
        XmlTreeNode childTreeNode = null;
        if (shouldProcess(xmlNode)) {
            String xlinkHref = XmlUtil.getAttribute(xmlNode, ATTR_XLINKHREF,
                                   NULL_STRING);
            String label = getLabel(xmlNode);
            if (isXlinkTag(XmlUtil.getLocalName(xmlNode))
                    && (xlinkHref != null)) {
                childTreeNode = new XlinkTreeNode(xmlNode, this, label,
                        xlinkHref);
                childTreeNode.add(new DefaultMutableTreeNode("Please wait"));
            } else {
                childTreeNode = new XmlTreeNode(xmlNode, label);
                elementToNode.put(xmlNode, childTreeNode);
            }
            parentTreeNode.add(childTreeNode);
            if (includeAttributes) {
                NamedNodeMap           attrs     = xmlNode.getAttributes();
                DefaultMutableTreeNode attrsNode = null;
                for (int i = 0; i < attrs.getLength(); i++) {
                    Attr attr = (Attr) attrs.item(i);
                    if (i == 0) {
                        attrsNode = new DefaultMutableTreeNode("Attributes");
                        childTreeNode.add(attrsNode);
                    }
                    attrsNode.add(
                        new DefaultMutableTreeNode(
                            attr.getNodeName() + "=" + attr.getNodeValue()));
                }
            }
        }


        if (shouldRecurse(xmlNode)) {
            //If this is null it implies that we don't process the current node but we do recurse
            NodeList children = XmlUtil.getElements(xmlNode);
            if (childTreeNode == null) {
                childTreeNode = parentTreeNode;
            }
            for (int i = 0; i < children.getLength(); i++) {
                Element childXmlNode = (Element) children.item(i);
                process(childTreeNode, childXmlNode);
            }
        }
    }

    /**
     *  Should we recursiely descend the children of the given xml Element
     *
     * @param xmlNode The xml node
     * @return    Should we recurse down
     */
    protected boolean shouldRecurse(Element xmlNode) {
        String tagName = XmlUtil.getLocalName(xmlNode);

        if (tagsToNotProcessButRecurse != null) {
            if (tagsToNotProcessButRecurse.get(tagName) != null) {
                return true;
            }
        }

        if (tagsToRecurse != null) {
            if (tagsToRecurse.get(tagName) == null) {
                return false;
            }
        }

        if (tagsToNotRecurse != null) {
            return (tagsToNotRecurse.get(XmlUtil.getLocalName(xmlNode))
                    == null);
        }
        return true;

    }

    /**
     *  Return the String used for the JTree node.
     *  This first looks in the tagNameToLabelAttr hashtable
     *  for an attribute name to fetch the label. If not found
     *  we try the attributes "label" and "name".
     *
     * @param n The node
     * @return Its label
     */
    public String getLabel(Element n) {
        String label = null;

        if (useTagNameAsLabel) {
            return XmlUtil.getLocalName(n);
        }

        if (tagNameToLabelAttr != null) {
            String attrName =
                (String) tagNameToLabelAttr.get(XmlUtil.getLocalName(n));
            if (attrName != null) {
                label = XmlUtil.getAttribute(n, attrName, NULL_STRING);
            }
        }

        if (tagNameToLabelChild != null) {
            String childTag =
                (String) tagNameToLabelChild.get(XmlUtil.getLocalName(n));
            if (childTag != null) {
                Element child = XmlUtil.getElement(n, childTag);
                if (child != null) {
                    label = XmlUtil.getChildText(child);
                }
            }
        }

        if (label == null) {
            label = XmlUtil.getAttribute(n, ATTR_LABEL, NULL_STRING);
        }

        if (label == null) {
            label = XmlUtil.getAttribute(n, ATTR_NAME, NULL_STRING);
        }

        if (label == null) {
            label = XmlUtil.getLocalName(n);
        }


        return label;
    }


    /**
     *  Return the xml Element located at the given position
     *
     * @param x x
     * @param y y
     * @return The node
     */
    public Element getXmlNodeAt(int x, int y) {
        return getXmlNodeAtPath(getPathForLocation(x, y));
    }

    /**
     *  Return the xml Element located at the given position
     *
     * @param path The path
     * @return The node or null
     */
    protected Element getXmlNodeAtPath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        if ( !(last instanceof XmlTreeNode)) {
            return null;
        }
        return ((XmlTreeNode) last).getXmlNode();
    }




    /**
     *  Return the xml tree node located at the given position
     *
     * @param x x
     * @param y y
     * @return The node or null
     */
    public XmlTreeNode getXmlTreeNodeAt(int x, int y) {
        return getXmlTreeNodeAtPath(getPathForLocation(x, y));
    }

    /**
     *  Return the xml tree node located at the given position
     *
     * @param path The tree path
     * @return The node or null
     */
    protected XmlTreeNode getXmlTreeNodeAtPath(TreePath path) {
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        if ( !(last instanceof XmlTreeNode)) {
            return null;
        }
        return (XmlTreeNode) last;
    }





    /**
     *  Return the xml Element that corresponds to the given tree node
     *
     * @param treeNode The tree node
     * @return The corresponding xml node
     */
    public Element getXmlElement(TreeNode treeNode) {
        if ( !(treeNode instanceof XmlTreeNode)) {
            return null;
        }
        return ((XmlTreeNode) treeNode).getXmlNode();
    }




    /**
     * Set tree select mode
     *
     * @param v Do multiples?
     */
    public void setMultipleSelect(boolean v) {
        getSelectionModel().setSelectionMode(v
                                             ? TreeSelectionModel
                                             .DISCONTIGUOUS_TREE_SELECTION
                                             : TreeSelectionModel
                                             .SINGLE_TREE_SELECTION);
    }


    /**
     * Test
     *
     * @param args cmd line args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Provide a  xml file");
            System.exit(0);
        }
        try {
            final XmlTree t = new XmlTree(
                                  XmlUtil.getRoot(
                                      IOUtil.readContents(
                                          new File(args[0]))));

            t.addTagToProcess("catalog");
            t.addTagToProcess("collection");
            t.addTagToProcess("dataset");

            JFrame f = new JFrame();
            f.getContentPane().add(GuiUtils.makeScrollPane(t.getContents(),
                    200, 300));
            f.pack();
            f.show();
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
        }
    }


}
