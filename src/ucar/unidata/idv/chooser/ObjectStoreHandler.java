/*
 * Copyright 1997-2026 Unidata Program Center/University Corporation for
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
import org.w3c.dom.NodeList;import ucar.unidata.data.DataManager;
import ucar.unidata.io.s3.CdmS3Uri;
import ucar.unidata.util.GuiUtils;import ucar.unidata.xml.XmlUtil;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.*;

import java.awt.*;
import java.awt.event.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;




/**
 * This handles the Amazon S3 Bucket xml for the
 * {@link XmlChooser}.
 *
 * @author IDV development team
 * @version $Revision: 1.68 $Date: 2026/07/09 22:59:58 $
 */

public class ObjectStoreHandler extends XmlHandler {
    private static final int TYPE_PREFIX = 0;
    private static final int TYPE_OBJECT = 1;
    private static final int TYPE_MORE   = 2;
    private static final int CHILD_BATCH_SIZE = 50;
    private static final String schemeCdmS3 = "cdms3";

    static ucar.unidata.util.LogUtil.LogCategory log_ =
            ucar.unidata.util.LogUtil.getLogInstance(
                    ObjectStoreHandler.class.getName());

    private JComboBox dataSourcesCbx;
    private JCheckBox showThumbsCbx;

    private JTree objectTree;
    private DefaultTreeModel treeModel;

    private ImageIcon folderIcon;
    private ImageIcon fileIcon;
    private String bucketName;

    private PrefixNode makeDummyNode() {
        PrefixNode node = new PrefixNode("Loading...", null, false, false);
        node.setDummy(true);
        node.setAllowsChildren(false);
        return node;
    }

    /**
     * Create the handler
     *
     * @param chooser The chooser we are in
     * @param root The root of the xml tree
     * @param path The url path of the xml document
     *
     */
    public ObjectStoreHandler(XmlChooser chooser, Element root, String path) {
        super(chooser, root, path);
    }

    /**
     * Update the status
     */
    protected void updateStatus() {
        if (chooser.getHaveData()) {
            chooser.setStatus("Press \"" + chooser.CMD_LOAD
                    + "\" to load the selected object", "buttons");
        } else {
            chooser.setStatus("Please select an object from the store");
        }
    }

    /**
     * Create the  UI
     *
     *  @return The UI component
     */
    protected JComponent doMakeContents() {
        showThumbsCbx = new JCheckBox("Show Thumbnail Images", !GuiUtils.isMac());

        dataSourcesCbx = chooser.getDataSourcesComponent(false,
                chooser.getDataManager());

        JComponent dsComp =
                GuiUtils.inset(GuiUtils.hbox(new JLabel("Data Source Type: "),
                        dataSourcesCbx, 5), 5);

        String currentPrefix = getDocumentPrefix(root);
        if (currentPrefix == null) {
            currentPrefix = "";
        }

        PrefixNode rootNode = new PrefixNode(currentPrefix, currentPrefix, true, true);
        bucketName = getBucketName(root);
        treeModel = new DefaultTreeModel(rootNode, true);
        objectTree = new JTree(treeModel);
        objectTree.setRootVisible(true);
        objectTree.setShowsRootHandles(true);
        objectTree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        objectTree.setCellRenderer(new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                          boolean sel, boolean expanded, boolean leaf, int row,
                                                          boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
                        row, hasFocus);

                if (folderIcon == null) {
                    folderIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/folderclosed.png");
                    fileIcon   = GuiUtils.getImageIcon("/auxdata/ui/icons/File.gif");
                }

                if (value instanceof PrefixNode) {
                    PrefixNode node = (PrefixNode) value;
                    setIcon(node.isPrefix() ? folderIcon : fileIcon);
                    setToolTipText(node.getToolTip());
                } else {
                    setToolTipText(null);
                }
                return this;
            }
        });

        objectTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            public void treeWillExpand(TreeExpansionEvent event)
                    throws ExpandVetoException {
                PrefixNode node = (PrefixNode) event.getPath().getLastPathComponent();
                if (!node.isPrefix() || node.isLoaded()) {
                    return;
                }
                loadChildren(node);
            }

            public void treeWillCollapse(TreeExpansionEvent event) {}
        });

        objectTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                TreePath tp = objectTree.getPathForLocation(e.getX(), e.getY());
                if (tp == null) {
                    chooser.setHaveData(false);
                    return;
                }
                PrefixNode node = (PrefixNode) tp.getLastPathComponent();
                chooser.setHaveData(!node.isPrefix());

                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    if (!node.isPrefix()) {
                        processNode(node);
                    }
                }

                if (SwingUtilities.isRightMouseButton(e)) {
                    objectTree.setSelectionPath(tp);
                    showPopup(node, e);
                }
            }
        });

        loadChildren(rootNode);

        JComponent treeScroller = new JScrollPane(objectTree);
        return GuiUtils.inset(GuiUtils.topCenter(dsComp, treeScroller), 5);
    }

    /**
     *  Process the Bucketname
     *
     *  @param root The xml node the user chose.
     */
    private String getBucketName(Element root) {
        Element nameNode = XmlUtil.findChild(root, "Name");
        return (nameNode != null) ? XmlUtil.getChildText(nameNode) : null;
    }

    /**
     *  Process the xml
     *
     *  @param parent The xml node the user chose.
     */
    private void loadChildren(final PrefixNode parent) {
        if (parent == null) {
            return;
        }

        if (!parent.isPrefix()) {
            return;
        }

        if (parent.isLoaded() && !parent.isMoreNode()) {
            return;
        }

        chooser.showWaitCursor();

        SwingWorker<List<PrefixNode>, Void> worker = new SwingWorker<List<PrefixNode>, Void>() {
            protected List<PrefixNode> doInBackground() throws Exception {
                if (parent.isMoreNode()) {
                    List<PrefixNode> deferred = parent.getDeferredChildren();
                    if (deferred == null) {
                        return new ArrayList<PrefixNode>();
                    }
                    return deferred;
                }

                return fetchChildren(parent.getFullPath());
            }

            protected void done() {
                try {
                    List<PrefixNode> kids = get();
                    if (kids == null) {
                        kids = new ArrayList<PrefixNode>();
                    }

                    if (parent.isMoreNode()) {
                        PrefixNode realParent = (PrefixNode) parent.getParent();
                        if (realParent == null) {
                            return;
                        }

                        int insertIndex = realParent.getIndex(parent);
                        if (insertIndex < 0) {
                            insertIndex = realParent.getChildCount();
                        }

                        treeModel.removeNodeFromParent(parent);

                        insertBatch(realParent, kids, insertIndex);
                        treeModel.nodeStructureChanged(realParent);
                        return;
                    }

                    parent.removeAllChildren();

                    insertBatch(parent, kids, 0);
                    parent.setLoaded(true);
                    treeModel.nodeStructureChanged(parent);

                } catch (Exception exc) {
                    chooser.logException("Loading object store children", exc);
                } finally {
                    chooser.showNormalCursor();
                }
            }
        };

        worker.execute();
    }

    /**
     *  Process the xml
     *
     *  @param prefix The xml tag.
     */
    private List<PrefixNode> fetchChildren(String prefix) throws Exception {
        List<PrefixNode> results = new ArrayList<PrefixNode>();
        String listingUrl = makePrefixUrl(path, prefix);
        Document doc = XmlUtil.getDocument(listingUrl, getClass());
        if (doc == null) {
            throw new IllegalStateException("Could not load: " + listingUrl);
        }
        Element docRoot = doc.getDocumentElement();
        String currentPrefix = getDocumentPrefix(docRoot);

        NodeList children = XmlUtil.getElements(docRoot);
        for (int i = 0; i < children.getLength(); i++) {
            Element child = (Element) children.item(i);
            String tag = XmlUtil.getLocalName(child);

            if ("CommonPrefixes".equals(tag)) {
                String childPrefix = XmlUtil.getChildText(XmlUtil.findChild(child, "Prefix"));
                String label = getRelativePrefixLabel(childPrefix, currentPrefix);
                PrefixNode n = new PrefixNode(label, childPrefix, true, true);
                results.add(n);
            } else if ("Contents".equals(tag)) {
                String key = XmlUtil.getChildText(XmlUtil.findChild(child, "Key"));
                if (key == null || key.equals(currentPrefix)) {
                    continue;
                }
                PrefixNode n = new PrefixNode(getLeafName(key), key, false, false);
                results.add(n);
            }
        }
        return results;
    }

    /**
     *  Process the xml
     *
     *  @param node The xml prefixnode.
     */
    private void processNode(PrefixNode node) {
        if (node == null || node.isPrefix()) {
            return;
        }

        List urls = new ArrayList();
        Hashtable properties = new Hashtable();
        if (node.getFullPath() == null) {
            return;
        }
        String objectUrl = makeObjectUrl(path, node.getFullPath());
        urls.add(objectUrl);

        String dataSourceId = chooser.getDataSourceId(dataSourcesCbx);
        if (dataSourceId != null) {
            properties.put(DataManager.DATATYPE_ID, dataSourceId);
        }
        properties.put("objectstore.key", node.getFullPath());
        properties.put("objectstore.listingurl", path);

        if (chooser.makeDataSource(urls, null, properties)) {
            chooser.closeChooser();
        }
    }

    /**
     *  load the dataset of selected
     *
     *  @param
     */
    public void doLoad() {
        TreePath[] paths = objectTree.getSelectionPaths();
        if (paths == null) {
            return;
        }

        List urls = new ArrayList();
        Hashtable properties = new Hashtable();

        String dataSourceId = chooser.getDataSourceId(dataSourcesCbx);
        if (dataSourceId != null) {
            properties.put(DataManager.DATATYPE_ID, dataSourceId);
        }
        properties.put("objectstore.listingurl", path);

        for (int i = 0; i < paths.length; i++) {
            PrefixNode node = (PrefixNode) paths[i].getLastPathComponent();
            if (node.isPrefix()) {
                continue;
            }
            String fullPath = node.getFullPath();

            urls.add(makeObjectUrl(path, node.getFullPath()));
        }

        if (urls.size() > 0 && chooser.makeDataSource(urls, null, properties)) {
            chooser.closeChooser();
        }
    }

    /**
     *  load the dataset of selected
     *
     *  @param node
     *  @param event
     */
    private void showPopup(final PrefixNode node, MouseEvent event) {
        JPopupMenu popup = new JPopupMenu();

        if (node.isPrefix()) {
            JMenuItem mi = new JMenuItem("Browse Prefix");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    TreePath tp = findTreePath(node);
                    if (tp != null) {
                        objectTree.expandPath(tp);
                    }
                }
            });
            popup.add(mi);
        } else {
            JMenuItem mi = new JMenuItem("Load Object");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    processNode(node);
                }
            });
            popup.add(mi);
        }

        popup.show(objectTree, event.getX(), event.getY());
    }

    private TreePath findTreePath(PrefixNode node) {
        return new TreePath(treeModel.getPathToRoot(node));
    }

    private String getDocumentPrefix(Element root) {
        Element prefixNode = XmlUtil.findChild(root, "Prefix");
        return (prefixNode != null) ? XmlUtil.getChildText(prefixNode) : "";
    }

    private String getRelativePrefixLabel(String fullPrefix, String currentPrefix) {
        if (fullPrefix == null) {
            return "Prefix";
        }
        if (currentPrefix != null && fullPrefix.startsWith(currentPrefix)) {
            return fullPrefix.substring(currentPrefix.length());
        }
        return fullPrefix;
    }

    private String makePrefixUrl(String listingUrl, String prefix) {
        String base = stripQuery(listingUrl);
        return base + "?prefix=" + encodePrefix(prefix) + "&delimiter=/";
    }

    private String makeObjectUrl(String listingUrl, String key) {
        String base = stripQuery1(listingUrl);
        CdmS3Uri cdmS3Uri = null;
        try {
            if (base != null) {
                cdmS3Uri = new CdmS3Uri(schemeCdmS3 + "://" + base + "@aws" + "/" + bucketName +  "?" + key);
            } else {
                cdmS3Uri = new CdmS3Uri(schemeCdmS3 + ":"  +  bucketName + "?" + key);
            }

        } catch (Exception e) {}

        return cdmS3Uri.toString();
    }

    private String stripQuery(String url) {
        int idx = url.indexOf('?');
        return (idx >= 0) ? url.substring(0, idx) : url;
    }

    private String stripQuery1(String url) {
        int idx = url.indexOf("/?");
        String surl = (idx >= 0) ? url.substring(0, idx) : url;
        idx = surl.indexOf("://");
        String url0 = (idx >= 0) ? surl.substring(idx+3) : surl;
        return url0;
    }

    private String encodePrefix(String prefix) {
        return URLEncoder.encode(prefix, StandardCharsets.UTF_8).replace("%2F", "/");
    }

    private String encodeKeyForUrl(String key) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%2F", "/");
    }

    private String getLeafName(String path) {
        if (path == null || path.length() == 0) {
            return "";
        }
        String tmp = path;
        if (tmp.endsWith("/")) {
            tmp = tmp.substring(0, tmp.length() - 1);
        }
        int idx = tmp.lastIndexOf('/');
        return (idx >= 0) ? tmp.substring(idx + 1) : tmp;
    }

    private void insertBatch(PrefixNode parent, List<PrefixNode> kids, int startIndex) {
        int visibleCount = Math.min(CHILD_BATCH_SIZE, kids.size());

        for (int i = 0; i < visibleCount; i++) {
            PrefixNode kid = kids.get(i);
            treeModel.insertNodeInto(kid, parent, startIndex + i);

            if (kid.isPrefix() && kid.mightHaveChildren()) {
                kid.add(makeDummyNode());
            }
        }

        if (kids.size() > CHILD_BATCH_SIZE) {
            List<PrefixNode> remaining = new ArrayList<PrefixNode>(
                    kids.subList(CHILD_BATCH_SIZE, kids.size())
            );

            PrefixNode moreNode = PrefixNode.makeMoreNode(
                    "Show next " + Math.min(CHILD_BATCH_SIZE, remaining.size()) + "...",
                    parent.getFullPath(),
                    remaining
            );

            treeModel.insertNodeInto(moreNode, parent, startIndex + visibleCount);
        }
    }

    private static class PrefixNode extends DefaultMutableTreeNode {
        private final String label;
        private final String fullPath;
        private final boolean prefix;
        private final boolean mayHaveChildren;

        private boolean loaded = false;
        private boolean moreNode = false;
        private boolean dummy = false;

        private List<PrefixNode> deferredChildren;

        PrefixNode(String label, String fullPath, boolean prefix, boolean mayHaveChildren) {
            super(label, prefix);
            this.label = label;
            this.fullPath = fullPath;
            this.prefix = prefix;
            this.mayHaveChildren = mayHaveChildren;
        }

        static PrefixNode makeMoreNode(String label, String fullPath, List<PrefixNode> deferredChildren) {
            PrefixNode node = new PrefixNode(label, fullPath, true, true);
            node.moreNode = true;
            node.loaded = false;
            node.deferredChildren = deferredChildren;
            return node;
        }

        public String getToolTip() {
            if (dummy) {
                return null;
            }
            if (moreNode) {
                return label;
            }
            return prefix
                    ? "Prefix: " + fullPath
                    : "Object: " + fullPath;
        }

        public boolean isPrefix() {
            return prefix;
        }

        public boolean mightHaveChildren() {
            return mayHaveChildren;
        }

        public boolean isLoaded() {
            return loaded;
        }

        public void setLoaded(boolean loaded) {
            this.loaded = loaded;
        }

        public boolean isMoreNode() {
            return moreNode;
        }

        public boolean isDummy() {
            return dummy;
        }

        public void setDummy(boolean dummy) {
            this.dummy = dummy;
        }

        public String getFullPath() {
            return fullPath;
        }

        public List<PrefixNode> getDeferredChildren() {
            return deferredChildren;
        }

        public void setDeferredChildren(List<PrefixNode> deferredChildren) {
            this.deferredChildren = deferredChildren;
        }

        public String toString() {
            return label;
        }
    }
}