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

package ucar.unidata.idv.ui;


import org.w3c.dom.Document;

import org.w3c.dom.Element;


import ucar.unidata.idv.*;


import ucar.unidata.ui.DndTree;



import ucar.unidata.util.FileManager;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.tree.*;


/**
 * Class BundleTree Gives a tree gui for editing bundles
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.34 $
 */
public class BundleTree extends DndTree {

    /** action command */
    public static final String CMD_EXPORT_TO_PLUGIN = "Export to Plugin";

    /** The window */
    private JFrame frame;


    /** What is the type of the bundles we are showing */
    private int bundleType;

    /** The root of the tree */
    private DefaultMutableTreeNode treeRoot;

    /** The tree model */
    private DefaultTreeModel treeModel;

    /** A mapping from tree node to, either, category or SavedBundle */
    private Hashtable nodeToData;

    /** The ui manager */
    private IdvUIManager uiManager;

    /** Icon to use for categories */
    private ImageIcon categoryIcon;

    /** Icon to use for bundles */
    private ImageIcon bundleIcon;




    /**
     * Create the tree with the given bundle type
     *
     *
     * @param uiManager The UI manager
     * @param bundleType The type of the bundles we are showing
     */
    public BundleTree(IdvUIManager uiManager, int bundleType) {

        categoryIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/folder.png",
                                             getClass());
        bundleIcon = GuiUtils.getImageIcon("/auxdata/ui/icons/page.png",
                                           getClass());

        this.uiManager = uiManager;

        setToolTipText(
            "<html>Right click to show popup menu.<br>Drag to move bundles or categories</html>");

        this.bundleType = bundleType;
        treeRoot = new DefaultMutableTreeNode(
            getPersistenceManager().getBundleTitle(getBundleType()));

        //        setRootVisible(false);
        setShowsRootHandles(true);
        treeModel = new DefaultTreeModel(treeRoot);
        setModel(treeModel);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                if ((nodeToData == null) || (value == null)) {
                    return this;
                }
                Object data = nodeToData.get(value);
                if (data == null) {
                    setIcon(categoryIcon);
                    return this;
                }
                if (data instanceof SavedBundle) {
                    setToolTipText(
                        "<html>Right click to show bundle menu.<br>Drag to move bundle</html>");
                    setIcon(bundleIcon);
                } else {
                    setToolTipText(
                        "<html>Right click to show category menu.<br>Drag to move bundles or categories</html><");
                    setIcon(categoryIcon);
                }
                return this;
            }
        };
        setCellRenderer(renderer);




        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (GuiUtils.isDeleteEvent(e)) {
                    deleteSelected();
                } else if (e.getKeyCode() == e.VK_ENTER) {
                    SavedBundle bundle = findSelectedBundle();
                    if (bundle != null) {
                        doOpen(bundle);
                    }
                }
            }
        });

        getSelectionModel().setSelectionMode(
            TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        //            TreeSelectionModel.SINGLE_TREE_SELECTION);


        addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                TreePath path = getPathForLocation(event.getX(),
                                    event.getY());
                Object data = findDataAtPath(path);
                if ( !SwingUtilities.isRightMouseButton(event)) {
                    if (event.getClickCount() > 1) {
                        if ((data != null) && (data instanceof SavedBundle)) {
                            doOpen((SavedBundle) data);
                        }
                    }
                    return;
                }
                clearSelection();
                addSelectionPath(path);
                final DefaultMutableTreeNode parentNode =
                    (DefaultMutableTreeNode) path.getLastPathComponent();

                JPopupMenu popup = new JPopupMenu();
                if (data == null) {
                    popup.add(GuiUtils.makeMenuItem("Add Sub-Category",
                            BundleTree.this, "addCategory", parentNode));
                } else {
                    if (data instanceof SavedBundle) {
                        SavedBundle bundle = (SavedBundle) data;
                        popup.add(GuiUtils.makeMenuItem("Open",
                                BundleTree.this, "doOpen", bundle));
                        popup.add(GuiUtils.makeMenuItem("Rename",
                                BundleTree.this, "doRename", bundle));
                        popup.add(GuiUtils.makeMenuItem("Export",
                                BundleTree.this, "doExport", bundle));
                        popup.add(GuiUtils.makeMenuItem("Export to Plugin",
                                BundleTree.this, "doExportToPlugin", bundle));
                        popup.add(GuiUtils.makeMenuItem("Delete",
                                BundleTree.this, "deleteBundle", bundle));
                    } else {
                        popup.add(GuiUtils.makeMenuItem("Import Bundle",
                                BundleTree.this, "doImport", parentNode));
                        popup.add(GuiUtils.makeMenuItem("Delete Category",
                                BundleTree.this, "deleteCategory",
                                data.toString()));
                        popup.add(GuiUtils.makeMenuItem("Add Sub-Category",
                                BundleTree.this, "addCategory", parentNode));
                    }
                }
                popup.show((Component) event.getSource(), event.getX(),
                           event.getY());
            }
        });
        loadBundles();

        String title =
            "Local "
            + getPersistenceManager().getBundleTitle(getBundleType())
            + " Manager";
        Dimension defaultDimension = new Dimension(300, 400);
        JScrollPane sp = GuiUtils.makeScrollPane(this,
                             (int) defaultDimension.getWidth(),
                             (int) defaultDimension.getHeight());
        sp.setPreferredSize(defaultDimension);


        JMenuBar menuBar  = new JMenuBar();
        JMenu    fileMenu = new JMenu("File");
        JMenu    helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        //        menuBar.add(helpMenu);
        fileMenu.add(GuiUtils.makeMenuItem("Export to File", this,
                                           "doExport"));
        fileMenu.add(GuiUtils.makeMenuItem("Export to Plugin", this,
                                           "doExportToPlugin"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "doClose"));




        JComponent bottom = GuiUtils.wrap(GuiUtils.makeButton("Close", this,
                                "doClose"));

        JPanel contents = GuiUtils.topCenterBottom(menuBar, sp, bottom);
        frame = GuiUtils.createFrame(title);
        frame.getContentPane().add(contents);
        frame.pack();
        frame.setLocation(100, 100);
    }

    /**
     * Open the bundle
     * @param bundle the bundle
     */
    public void doOpen(SavedBundle bundle) {
        getPersistenceManager().open(bundle);
    }


    /**
     * Rename the bundle
     * @param bundle the bundle
     */
    public void doRename(SavedBundle bundle) {
        getPersistenceManager().rename(bundle, getBundleType());
    }

    /**
     * Export the bundle
     * @param bundle the bundle
     */
    public void doExport(SavedBundle bundle) {
        getPersistenceManager().export(bundle, getBundleType());

    }


    /**
     * Export the bundle
     * @param bundle the bundle
     */
    public void doExportToPlugin(SavedBundle bundle) {
        uiManager.getIdv().getPluginManager().addObject(bundle);

    }

    /**
     * close
     */
    public void doClose() {
        frame.dispose();
    }


    /**
     * Get the list of selected bundles
     *
     * @return The selected bundles
     */
    private List getSelectedBundles() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return new ArrayList();
        }
        List bundles = new ArrayList();
        for (int i = 0; i < paths.length; i++) {
            Object data = findDataAtPath(paths[i]);
            if (data == null) {
                continue;
            }
            if ( !(data instanceof SavedBundle)) {
                continue;
            }
            bundles.add(data);
        }
        return bundles;
    }



    /**
     * Export the selected bundles to the plugin creator
     */
    public void doExportToPlugin() {
        List bundles = getSelectedBundles();
        if (bundles.size() == 0) {
            LogUtil.userMessage("No bundles are selected");
            return;
        }
        uiManager.getIdv().getPluginManager().addObjects(bundles);
    }




    /**
     * Export the selected bundles
     */
    public void doExport() {
        try {
            List bundles = getSelectedBundles();
            if (bundles.size() == 0) {
                LogUtil.userMessage("No bundles are selected");
                return;
            }

            String filename =
                FileManager.getWriteFile(FileManager.FILTER_XML,
                                         FileManager.SUFFIX_XML);
            if (filename == null) {
                return;
            }

            File    dir      = new File(filename).getParentFile();

            boolean anyExist = false;
            for (int i = 0; i < bundles.size(); i++) {
                SavedBundle bundle = (SavedBundle) bundles.get(i);
                if (new File(
                        IOUtil.joinDir(
                            dir,
                            IOUtil.getFileTail(bundle.getUrl()))).exists()) {
                    anyExist = true;
                    break;
                }
            }

            if (anyExist) {
                if ( !GuiUtils.showOkCancelDialog(null,
                        "Overwrite Bundle Files",
                        new JLabel("<html>One or more bundle files already exist.<br>Do you want to overwrite them?</html>"),
                        null, null)) {
                    return;
                }
            }


            Document doc  = XmlUtil.makeDocument();
            Element  root = doc.createElement(SavedBundle.TAG_BUNDLES);
            for (int i = 0; i < bundles.size(); i++) {
                SavedBundle bundle = (SavedBundle) bundles.get(i);
                bundle.toXml(doc, root);
            }

            String xml = XmlUtil.toString(doc.getDocumentElement());
            IOUtil.writeFile(filename, xml);
            String msg =
                "<html>The selected bundles have been exported. The files are:<br>";

            for (int i = 0; i < bundles.size(); i++) {
                SavedBundle bundle = (SavedBundle) bundles.get(i);
                String      tail   = IOUtil.getFileTail(bundle.getUrl());
                msg += "&nbsp;&nbsp;<b>" + tail + "</b><br>";
                File toFile = new File(IOUtil.joinDir(dir, tail));
                IOUtil.copyFile(new File(bundle.getUrl()), toFile);
            }
            msg += "</html>";
            LogUtil.userMessage(msg);


        } catch (Exception exc) {
            LogUtil.logException("Exporting bundles", exc);
        }

    }

    /**
     * Return the bundle type
     *
     * @return The bundle type
     */
    private int getBundleType() {
        return bundleType;
    }

    /**
     * Return the persistence manager
     *
     * @return The persistence manager
     */
    private IdvPersistenceManager getPersistenceManager() {
        return uiManager.getPersistenceManager();
    }


    /**
     * Show the window
     */
    public void show() {
        frame.show();
    }


    /**
     * Ok to drag the node
     *
     * @param sourceNode The node to drag
     *
     * @return Ok to drag
     */
    protected boolean okToDrag(DefaultMutableTreeNode sourceNode) {
        return sourceNode.getParent() != null;
    }


    /**
     * Ok to drop the node
     *
     *
     * @param sourceNode The dragged node
     * @param destNode Where to drop
     *
     * @return Ok to drop
     */
    protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                               DefaultMutableTreeNode destNode) {


        //Don't drop a bundle onto the root. It must be in a catgegory
        if (sourceNode.getUserObject() instanceof SavedBundle) {
            if (destNode.getParent() == null) {
                return false;
            }
        }

        if (destNode.getUserObject() instanceof SavedBundle) {
            return false;
        }
        if (destNode == sourceNode.getParent()) {
            return false;
        }
        while (destNode != null) {
            if (destNode == sourceNode) {
                return false;
            }
            destNode = (DefaultMutableTreeNode) destNode.getParent();
        }
        return true;
    }



    /**
     * Handle the DND drop
     *
     *
     * @param sourceNode The dragged node
     * @param destNode Where to drop
     */
    protected void doDrop(DefaultMutableTreeNode sourceNode,
                          DefaultMutableTreeNode destNode) {
        if (sourceNode.getUserObject() instanceof SavedBundle) {
            uiManager.getPersistenceManager().moveBundle(
                (SavedBundle) sourceNode.getUserObject(),
                getCategoryList(destNode), bundleType);
        } else {
            uiManager.getPersistenceManager().moveCategory(
                getCategoryList(sourceNode), getCategoryList(destNode),
                bundleType);
        }

        loadBundles();
    }




    /**
     * Create the list of categories
     *
     * @param destNode From where
     *
     * @return List of String categories
     */
    private List getCategoryList(DefaultMutableTreeNode destNode) {
        List categories = new ArrayList();
        while (destNode.getParent() != null) {
            categories.add(0, destNode.getUserObject().toString());
            destNode = (DefaultMutableTreeNode) destNode.getParent();
        }
        return categories;
    }



    /**
     * Delete the selected item in the tree
     */
    public void deleteSelected() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return;
        }
        Object data = findDataAtPath(paths[0]);
        if (data == null) {
            return;
        }
        if (data instanceof SavedBundle) {
            deleteBundle((SavedBundle) data);
        } else {
            deleteCategory(data.toString());
        }
    }


    /**
     * Find and return the selected bundle. May return null if none selected
     *
     * @return Selected bundle
     */
    public SavedBundle findSelectedBundle() {
        TreePath[] paths = getSelectionModel().getSelectionPaths();
        if ((paths == null) || (paths.length == 0)) {
            return null;
        }
        Object data = findDataAtPath(paths[0]);
        if (data == null) {
            return null;
        }
        if (data instanceof SavedBundle) {
            return (SavedBundle) data;
        }
        return null;
    }


    /**
     * Load in the bundles into the tree
     */
    protected void loadBundles() {

        Enumeration paths =
            getExpandedDescendants(new TreePath(treeRoot.getPath()));
        Hashtable expandedState =
            GuiUtils.initializeExpandedPathsBeforeChange(this, treeRoot);

        List allCategories =
            uiManager.getPersistenceManager().getAllCategories(bundleType);
        nodeToData = new Hashtable();
        treeRoot.removeAllChildren();
        Hashtable catNodes    = new Hashtable();
        Hashtable fakeBundles = new Hashtable();
        List      bundles     = new ArrayList();

        //We use a set of fake bundles to we include all categories into the tree
        for (int i = 0; i < allCategories.size(); i++) {
            List categories =
                uiManager.getPersistenceManager().stringToCategories(
                    (String) allCategories.get(i));
            SavedBundle fakeBundle = new SavedBundle("", "", categories);
            fakeBundles.put(fakeBundle, fakeBundle);
            bundles.add(fakeBundle);
        }
        bundles.addAll(
            uiManager.getPersistenceManager().getWritableBundles(bundleType));
        for (int i = 0; i < bundles.size(); i++) {
            SavedBundle            bundle     = (SavedBundle) bundles.get(i);
            List                   categories = bundle.getCategories();
            DefaultMutableTreeNode catNode    = treeRoot;
            String                 fullCat    = "";
            for (int catIdx = 0; catIdx < categories.size(); catIdx++) {
                String cat = (String) categories.get(catIdx);
                if (fullCat.length() > 0) {
                    fullCat = fullCat
                              + IdvPersistenceManager.CATEGORY_SEPARATOR;
                }
                fullCat = fullCat + cat;
                DefaultMutableTreeNode tmpNode =
                    (DefaultMutableTreeNode) catNodes.get(fullCat);
                if (tmpNode == null) {
                    tmpNode = new DefaultMutableTreeNode(cat);
                    nodeToData.put(tmpNode, fullCat);
                    catNode.add(tmpNode);
                    catNodes.put(fullCat, tmpNode);
                }
                catNode = tmpNode;
            }
            //Skip over the fake ones
            if (fakeBundles.get(bundle) == null) {
                DefaultMutableTreeNode bundleNode =
                    new DefaultMutableTreeNode(bundle);
                nodeToData.put(bundleNode, bundle);
                catNode.add(bundleNode);
            }
        }
        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(this, expandedState, treeRoot);
    }



    /**
     * Delete the given bundle
     *
     * @param bundle The bundle to delete
     */
    public void deleteBundle(SavedBundle bundle) {
        if ( !GuiUtils.askYesNo(
                "Bundle delete confirmation",
                "Are you sure you want to delete the bundle \"" + bundle
                + "\"  ?")) {
            return;
        }
        uiManager.getPersistenceManager().deleteBundle(bundle.getUrl());
        loadBundles();
    }


    /**
     * Create a new category under the given node
     *
     * @param parentNode The parent tree node
     */
    public void addCategory(DefaultMutableTreeNode parentNode) {
        String cat =
            GuiUtils.getInput("Please enter the new sub-category name",
                              "Name: ", "");
        if (cat == null) {
            return;
        }
        String parentCat = (String) nodeToData.get(parentNode);
        String fullCat   = ((parentCat == null)
                            ? cat
                            : (parentCat
                               + IdvPersistenceManager.CATEGORY_SEPARATOR
                               + cat));
        if ( !uiManager.getPersistenceManager().addBundleCategory(bundleType,
                fullCat)) {
            LogUtil.userMessage(
                "A subcategory with the given name already exists");
            return;
        }
        DefaultMutableTreeNode newCatNode = new DefaultMutableTreeNode(cat);
        nodeToData.put(newCatNode, fullCat);
        parentNode.add(newCatNode);


        Hashtable expandedState =
            GuiUtils.initializeExpandedPathsBeforeChange(this, treeRoot);
        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(this, expandedState, treeRoot);
    }



    /**
     * Create a new category under the given node
     *
     * @param parentNode The parent tree node
     */
    public void doImport(DefaultMutableTreeNode parentNode) {
        String filename =
            FileManager
                .getReadFile("Import Bundle",
                             Misc
                             .newList(uiManager.getIdv().getArgsManager()
                                 .getXidvZidvFileFilter()));
        if (filename == null) {
            return;
        }

        String fullCat = (String) nodeToData.get(parentNode);
        uiManager.getPersistenceManager().doImport(bundleType, filename,
                fullCat);
        Hashtable expandedState =
            GuiUtils.initializeExpandedPathsBeforeChange(this, treeRoot);
        treeModel.nodeStructureChanged(treeRoot);
        GuiUtils.expandPathsAfterChange(this, expandedState, treeRoot);
    }



    /**
     * Delete the given bundle category
     *
     * @param category The category to delete
     */
    public void deleteCategory(String category) {
        if ( !GuiUtils.askYesNo(
                "Bundle Category Delete Confirmation",
                "<html>Are you sure you want to delete the category:<p> <center>\""
                + category
                + "\"</center> <br> and all bundles and categories under it?</html>")) {
            return;
        }
        uiManager.getPersistenceManager().deleteBundleCategory(bundleType,
                category);
        loadBundles();
    }



    /**
     * Find the data (either a SavedBundle or a category)
     * associated with the given  tree path
     *
     * @param path The path
     *
     * @return The data
     */

    private Object findDataAtPath(TreePath path) {
        if ((path == null) || (nodeToData == null)) {
            return null;
        }
        DefaultMutableTreeNode last =
            (DefaultMutableTreeNode) path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        return nodeToData.get(last);
    }

}
