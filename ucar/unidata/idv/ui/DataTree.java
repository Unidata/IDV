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


import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceFactory;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DerivedDataDescriptor;
import ucar.unidata.data.DescriptorDataSource;



import ucar.unidata.idv.*;


import ucar.unidata.ui.DndTree;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.awt.*;
import java.awt.event.*;



import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;


import javax.swing.tree.*;



/**
 * This class provides  a JTree interface for a set
 * of {@link ucar.unidata.data.DataChoice}-s and
 * {@link ucar.unidata.data.DataSource}-s.
 * It is used two ways:
 * <ul>
 * <li> To show the data choices in the {@link DataSelector}
 * <li> To show all of the data sources  and data choices when the user
 * is selecting  operands for formulas or when they are adding or changing
 * the data choices in a display control
 *
 * @author IDV development team
 * @version $Revision: 1.50 $Date: 2007/08/21 12:15:45 $
 */
public class DataTree extends DataSourceHolder {

    /**
     *  If non-null, contains a set of
     *  {@link ucar.unidata.data.DataCategory} objects. These
     *  are used to figure out what
     *  {@link ucar.unidata.data.DataCategory}-s to show in the JTree
     */
    List categories;

    /** Should show any icons */
    private boolean showIcons = true;

    /** The JTree */
    MyTree tree;

    /** The root of the JTree */
    DataTreeNode treeRoot;

    /** The tree model */
    DefaultTreeModel treeModel;

    /** Holds the start from the last tree search */
    private Object searchState = null;

    /**
     * A mapping from an object (either data choice or data source)
     *   to the tree node that represents it
     */
    Hashtable dataToTreeNode = new Hashtable();



    /** Should the data source be shown as a node in the tree */
    boolean showDataSourceNode = true;

    /** The scroll pane that holds this DataTree */
    JScrollPane scroller;

    /** The field name  of a data choice that should be selected */
    String initialSelectedFieldName;

    /** Should we sort the tree */
    boolean doSort = true;

    /**
     * Create a DataTree with the given idv reference.
     *
     *
     * @param idv The idv
     * @param showDataSourceNode Should this DataTree display the DataSource-s in the tree
     * @param treatFormulaDataSourceSpecial If true we put the Formula data source
     *  (which represents the end-user formulas) at the top.
     */
    public DataTree(IntegratedDataViewer idv, boolean showDataSourceNode,
                    boolean treatFormulaDataSourceSpecial) {
        this(idv, null);
        this.showDataSourceNode            = showDataSourceNode;
        this.treatFormulaDataSourceSpecial = treatFormulaDataSourceSpecial;
    }

    /**
     * Create a DataTree with the given idv reference and formula data source.
     *
     * @param idv The IDV
     * @param formulaDataSource The formula data source that holds the
     *  end-user formulas
     */
    public DataTree(IntegratedDataViewer idv, DataSource formulaDataSource) {
        this(idv, formulaDataSource, (Dimension) null);
    }


    /**
     * Create a DataTree with the given idv reference, formula data source
     * and window size.
     *
     * @param idv The IDV
     * @param formulaDataSource The formula data source that holds the
     * @param defaultSize Default size of the window (if non-null)
     */
    public DataTree(IntegratedDataViewer idv, DataSource formulaDataSource,
                    Dimension defaultSize) {
        super(idv, formulaDataSource, defaultSize);
        init(null);
        if (formulaDataSource != null) {
            addDataSource(formulaDataSource);
        }
    }


    /**
     * Create a DataTree with the given idv reference, list of
     * {@link ucar.unidata.data.DataSource}-s and
     * (potentially null) list of
     * {@link ucar.unidata.data.DataCategory}-s
     * If the categories list is non-null then this DataTree
     * will only show data choices that are applicable to the categories.
     *
     * @param idv The IDV
     * @param sources List of data  sources
     * @param categories List of data categories
     */
    public DataTree(IntegratedDataViewer idv, List sources, List categories) {
        this(idv, sources, categories, (Dimension) null);
    }

    /**
     * Create a DataTree with the given idv reference, list of
     * {@link ucar.unidata.data.DataSource}-s,
     * (potentially null) list of
     * {@link ucar.unidata.data.DataCategory}-s and default window size.
     * If the categories list is non-null then this DataTree
     * will only show data choices that are applicable to the categories.
     *
     * @param idv The IDV
     * @param sources List of data  sources
     * @param categories List of data categories
     * @param defaultSize Windwo size (if non-null)
     */
    public DataTree(IntegratedDataViewer idv, List sources, List categories,
                    Dimension defaultSize) {
        this(idv, sources, categories, null, defaultSize);
    }

    /**
     * Create a DataTree with the given idv reference, list of
     * {@link ucar.unidata.data.DataSource}-s,
     * (potentially null) list of
     * {@link ucar.unidata.data.DataCategory}-s and default window size.
     * If the categories list is non-null then this DataTree
     * will only show data choices that are applicable to the categories.
     *
     * @param idv The IDV
     * @param sources List of data  sources
     * @param categories List of data categories
     * @param initialSelectedFieldName The name of the data choice we should select
     * @param defaultSize Window size (if non-null)
     */
    public DataTree(IntegratedDataViewer idv, List sources, List categories,
                    String initialSelectedFieldName, Dimension defaultSize) {
        super(idv, null, defaultSize);
        this.categories               = categories;
        this.initialSelectedFieldName = initialSelectedFieldName;
        init(sources);
    }





    /**
     * Turn on sorting
     */
    public void sort() {
        doSort = true;
    }




    /**
     * Overwrite the base class getName method.
     *
     * @return The name of this class to be used in the gui
     */
    protected String getName() {
        return "Data tree selector";
    }

    /**
     *  This does a removeAllDataSource/addDataSource
     *  resulting in a datatree that holds just the given datasource
     *
     * @param dataSource The data source to use
     */
    public void setDataSource(DataSource dataSource) {
        removeAllDataSources();
        addDataSource(dataSource);
    }

    /**
     * Get the main GUI contents
     *
     * @return The GUI
     */
    public JComponent getContents() {
        return getScroller();
    }

    /**
     * Get the JTree we use
     *
     * @return The JTree
     */
    public JTree getTree() {
        return tree;
    }


    /**
     * Handle right click
     *
     * @param e mouse event
     */
    private void handleTreeMouseEvent(MouseEvent e) {
        if ( !SwingUtilities.isRightMouseButton(e)) {
            return;
        }
        List<DataChoice> choices = getSelectedDataChoices();
        if ((choices == null) || (choices.size() == 0)) {
            return;
        }
        final DataChoice dataChoice = choices.get(0);
        JPopupMenu       popup      = new JPopupMenu();
        String           name       = dataChoice.getName();
        JMenu            menu = new JMenu("Add \"" + name
                                          + "\" as alias for");

        for (DataAlias alias :
                (List<DataAlias>) Misc.sort(DataAlias.getDataAliasList())) {
            String aliasName = alias.getName();
            if (alias.getLabel().length() > 0) {
                aliasName += " - " + alias.getLabel();
            }
            menu.add(GuiUtils.makeMenuItem(aliasName, this, "addAsAlias",
                                           new Object[] { name,
                    alias }));

        }
        GuiUtils.limitMenuSize(menu, "Canonical Name", 15);
        popup.add(menu);
        popup.show((Component) e.getSource(), e.getX(), e.getY());

    }

    /**
     * The pair contains a dataalias and a alias name
     * Call AliasEditor.addAsAlias
     *
     * @param pair Holds a DataAlias and a alias name
     */
    public void addAsAlias(Object[] pair) {
        String    name  = (String) pair[0];
        DataAlias alias = (DataAlias) pair[1];
        getIdv().getAliasEditor().addAsAlias(alias, name);
    }

    /**
     * Initialize this DataTree with the given list of
     * {@link ucar.unidata.data.DataSource}-s
     *
     * @param sources The data sources to use
     */
    private void init(List sources) {
        //      this.tree = this;
        showIcons = idv.getProperty("idv.ui.datatree.showicons", true);
        this.tree = new MyTree(this);
        this.tree.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                handleTreeMouseEvent(e);
            }
        });



        ToolTipManager.sharedInstance().registerComponent(getTree());
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree theTree,
                    Object value, boolean sel, boolean expanded,
                    boolean leaf, int row, boolean hasFocus) {

                super.getTreeCellRendererComponent(theTree, value, sel,
                        expanded, leaf, row, hasFocus);
                setToolTipText(null);
                if (value instanceof DataTreeNode) {
                    DataTreeNode dtn = (DataTreeNode) value;
                    if (dtn.getIsDerived()) {
                        if (showIcons) {
                            setIcon(getDerivedIcon());
                        }
                    }
                    Object o = dtn.getObject();
                    if (o instanceof DerivedDataChoice) {
                        DerivedDataChoice ddc = (DerivedDataChoice) o;
                        if (ddc.getFormula() != null) {
                            setToolTipText("<html> " + ddc.getName()
                                           + "<br> " + ddc.getFormula()
                                           + "</html>");
                        }
                    } else if (o instanceof DataChoice) {
                        DataChoice dc = (DataChoice) o;
                        if (showIcons) {
                            String iconPath =
                                dc.getProperty(DataChoice.PROP_ICON,
                                    (String) null);
                            if (iconPath != null) {
                                ImageIcon icon =
                                    GuiUtils.getImageIcon(iconPath, true);
                                if (icon != null) {
                                    setIcon(icon);
                                }
                            }
                        }
                        setToolTipText("<html> " + dc.getName() + "<br> "
                                       + dc.getDescription() + "</html>");
                    } else if (o instanceof DataSource) {
                        setToolTipText(((DataSource) o).getDescription());
                    }
                }
                return this;
            }
        };
        tree.setCellRenderer(renderer);
        tree.setRowHeight(18);
        ImageIcon icon =
            GuiUtils.getImageIcon("/ucar/unidata/idv/ui/Bullet.gif",
                                  getClass());
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        if (showIcons) {
            renderer.setLeafIcon(icon);
        } else {
            renderer.setLeafIcon(null);
        }
        Font f = renderer.getFont();
        //        renderer.setFont (f.deriveFont (14.0f));


        treeRoot  = new DataTreeNode(this, "");
        treeModel = new DefaultTreeModel(treeRoot);
        tree.setModel(treeModel);
        tree.setRootVisible(false);
        setMultipleSelect(true);
        tree.setShowsRootHandles(true);
        if (sources != null) {
            for (int i = 0; i < sources.size(); i++) {
                addDataSource((DataSource) sources.get(i));
            }
        }
    }





    /**
     * CLear the search state
     */
    public void clearSearchState() {
        searchState = null;
    }

    /**
     * Search the tree
     *
     *
     * @param searchString search for
     * @param near component to show dialog near
     *
     * @return Success
     */
    public boolean doSearch(String searchString, JComponent near) {
        GuiUtils.TreeSearchResults results = GuiUtils.doTreeSearch(tree,
                                                 searchState, "field", near,
                                                 searchString);
        searchState = results.lastState;
        return results.success;
    }


    /**
     * We use this mostly so we can clear the toggled paths
     * (which is a protected method in JTree)
     *
     * @author IDV development team
     * @version %I%, %G%
     */
    private static class MyTree extends DndTree {

        /** The data tree I am part of */
        private DataTree dataTree;

        /**
         * The ctor
         *
         *
         * @param dataTree The data tree
         */
        public MyTree(DataTree dataTree) {
            this.dataTree = dataTree;
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
                    && (path[path.length - 1] instanceof DataTreeNode)) {
                DataTreeNode node = (DataTreeNode) path[path.length - 1];
                if (node.getObject() instanceof CompositeDataChoice) {
                    node.checkExpansion(dataTree);
                }
            }
            super.fireTreeWillExpand(treePath);
        }



        /**
         * Clear links, etc.
         */
        public void dispose() {
            dataTree = null;
        }

        /**
         *  A hook to call the  protected base class method
         */
        public void clearPaths() {
            //SKIP THIS            super.clearToggledPaths();
        }


        /**
         * Tell the DndTree if it is ok to drag the given node
         *
         * @param sourceNode THe node
         *
         * @return Ok to drag
         */
        protected boolean okToDrag(DefaultMutableTreeNode sourceNode) {
            if ( !(sourceNode instanceof DataTreeNode)) {
                return false;
            }
            DataTreeNode dtn = (DataTreeNode) sourceNode;
            if ( !(dtn.getObject() instanceof DerivedDataChoice)) {
                return false;
            }
            DerivedDataChoice ddc = (DerivedDataChoice) dtn.getObject();
            return ddc.isEndUserFormula();
        }

        /**
         * Is it ok to drop
         *
         * @param sourceNode The dragged node
         * @param destNode Where to drop
         *
         * @return  ok to drop
         */
        protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                                   DefaultMutableTreeNode destNode) {

            if (sourceNode.getParent() == destNode) {
                return false;
            }
            if ( !(sourceNode instanceof DataTreeNode)) {
                return false;
            }
            if ( !(destNode instanceof DataTreeNode)) {
                return false;
            }
            Object destData = ((DataTreeNode) destNode).getObject();
            if (destData instanceof String) {
                return true;
            }
            return false;
        }

        /**
         * Handle the DND drop
         *
         * @param sourceNode The dragged node
         * @param destNode Where to drop
         */
        protected void doDrop(DefaultMutableTreeNode sourceNode,
                              DefaultMutableTreeNode destNode) {

            DerivedDataChoice ddc =
                (DerivedDataChoice) ((DataTreeNode) sourceNode).getObject();
            DerivedDataDescriptor ddd      = ddc.getDataDescriptor();
            String                category = "";
            category = ((DataTreeNode) destNode).getObject().toString();

            DataCategory dataCategory = DataCategory.parseCategory(category,
                                            true);
            dataCategory.setForDisplay(true);

            ddd.setDataCategories(Misc.newList(dataCategory));

            dataTree.getIdv().getJythonManager().descriptorChanged(ddd);


        }

    }




    /**
     * Have the JTree select the paths that lead to
     * the {@link ucar.unidata.data.DataChoice}-s in the given list.
     *
     * @param choices List of data choices
     */
    public void selectChoices(List choices) {
        selectChoices(choices, false);
    }

    /**
     * Have the JTree select the paths that lead to
     * the {@link ucar.unidata.data.DataChoice}-s in the given list.
     *
     * @param choices List of data choices
     * @param shouldSet Should we set the selection or add to the selection
     */
    public void selectChoices(List choices, boolean shouldSet) {
        for (int i = 0; i < choices.size(); i++) {
            DataTreeNode node = getTreeNode(choices.get(i));
            if (node == null) {
                //              System.err.println ("Couldn't find node for:" + choices.get (i));
                continue;
            }
            TreeNode[] path = treeModel.getPathToRoot(node);
            if (shouldSet) {
                tree.setSelectionPath(new TreePath(path));
            } else {
                tree.addSelectionPath(new TreePath(path));
            }
        }
    }



    /**
     * Set the selection mode  of the JTree
     *
     * @param v JTree is multiple or single select
     */
    public void setMultipleSelect(boolean v) {
        tree.getSelectionModel().setSelectionMode(v
                ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
                : TreeSelectionModel.SINGLE_TREE_SELECTION);
    }




    /**
     * Create (if needed) and return the JScrollPane
     * around the JTree
     *
     * @return The tree scroller
     */
    public JScrollPane getScroller() {
        if (scroller == null) {
            if (defaultDimension == null) {
                defaultDimension = new Dimension(300, 400);
            }
            scroller = GuiUtils.makeScrollPane(getTree(),
                    (int) defaultDimension.getWidth(),
                    (int) defaultDimension.getHeight());
            scroller.setPreferredSize(defaultDimension);
        }
        return scroller;
    }


    /**
     * We have this here so we can display the
     * {@link ucar.unidata.idv.DisplayControl}-s in the DataTree.
     * We display them under the tree node
     * that represents the given
     * {@link ucar.unidata.data.DataChoice}
     *
     * @param control The display control to represent
     * @param choice The data choice to show the display control under
     */
    public void addDisplayControl(DisplayControl control, DataChoice choice) {
        DataTreeNode parent = getTreeNode(choice);
        if (parent == null) {
            return;
        }
        createTreeNode(parent, control.toString(), control);
    }


    /**
     * Remove the tree node that represents the given display control
     *
     * @param control The control to remove from the tree
     */
    public void removeDisplayControl(DisplayControl control) {
        removeTreeNode(getTreeNode(control));
    }


    /**
     * Is the given object (either a data source or a data choice)
     * represented in the JTree
     *
     * @param data The object to look for a tree node for
     * @return Is object represented in tree
     */
    public boolean isValidData(Object data) {
        return (getTreeNode(data) != null);
    }

    /**
     * Return the tree node that represents the given
     * data object (e.g., data source, data choice)
     *
     * @param data The object to look for a tree node
     * @return The tree node that represents the given object
     */
    public DataTreeNode getTreeNode(Object data) {
        return (DataTreeNode) dataToTreeNode.get(data);
    }

    /**
     * Create a {@link DataTreeNode} with the given label
     * that holds the given data object
     *
     * @param label The label
     * @param data The data
     * @return The new DataTreeNode
     */
    public DataTreeNode createTreeNode(String label, Object data) {
        DataTreeNode node = new DataTreeNode(this, label, data);
        dataToTreeNode.put(data, node);
        return node;
    }

    /**
     * Create a {@link DataTreeNode} with the given label
     * that holds the given data object. Add it as a child if the
     * given parent node.
     *
     * @param parent The parent node
     * @param label The label
     * @param data The data
     * @return The new DataTreeNode
     */
    public DataTreeNode createTreeNode(DataTreeNode parent, String label,
                                       Object data) {
        DataTreeNode child = createTreeNode(label, data);
        parent.add(child);
        treeStructureChanged(child);
        return child;
    }


    /**
     * Remove the given {@link DataTreeNode} from the
     * tree. Remove it from the dataToNode mapping.
     * Jump through some hoops to fire the tree
     * structure changed event and maintain the current
     * expanded tree paths.
     *
     * @param node The node to remove
     */
    public void removeTreeNode(DataTreeNode node) {
        if (node == null) {
            return;
        }
        if (node.getObject() != null) {
            dataToTreeNode.remove(node.getObject());
        }
        DataTreeNode parent = (DataTreeNode) node.getParent();
        node.removeFromParent();


        Hashtable paths = getExpandedPaths();
        if (parent != null) {
            treeModel.nodeStructureChanged(parent);
            TreePath path = new TreePath(parent.getPath());
            tree.expandPath(path);
        }
        GuiUtils.expandPathsAfterChange(tree, paths, treeRoot);
    }

    /**
     * Find the {@link DataTree} node that represents the
     * given data object and remove it from the tree
     *
     * @param dataObject The object whose tree node is to be removed
     */
    public void removeObject(Object dataObject) {
        removeTreeNode(getTreeNode(dataObject));
    }


    /**
     * Get the TreePath to the given treeNode
     *
     * @param treeNode The node to find the path to
     * @return The path
     */
    public TreePath getPath(DefaultMutableTreeNode treeNode) {
        return new TreePath(treeNode.getPath());
    }

    /**
     * Get all expanded tree paths
     *
     * @return All expanded paths
     */
    public Hashtable getExpandedPaths() {
        return GuiUtils.initializeExpandedPathsBeforeChange(tree, treeRoot);
    }


    /**
     *  Remove all references to anything we may have. We do this because (stupid) Swing
     *  seems to keep around lots of different references to thei component and/or it's
     *  frame. So when we do a window.dispose () this DataTree  does not get gc'ed.
     */
    public void dispose() {
        super.dispose();
        tree.clearPaths();
        tree.setModel(new DefaultTreeModel(new DataTreeNode(this, "")));
        tree.dispose();
        categories     = null;
        treeRoot       = null;
        treeModel      = null;
        dataToTreeNode = null;
        scroller       = null;
    }



    /**
     * Update the tree structure. Scroll to the given node
     *
     * @param nodeToScrollTo Scroll the tree to this node
     */
    private void treeStructureChanged(DataTreeNode nodeToScrollTo) {
        treeStructureChanged(nodeToScrollTo, null /*getExpandedPaths()*/);
    }



    /**
     *  This method tells the  tree that some new node (or nodes)
     *  have been added. It gets the set of currently expanded paths,
     *  tells the treeModel the node structure has changed (which for some
     *  reason causes the collapse of the expanded paths) and
     *  then  runs through the set of previously expanded paths
     *  and re-expands them. If nodeToScrollTo is non-null
     *  it scrolls to that node. If paths is non-null this is a collection
     *  of tree paths to re-expand.
     *
     * @param nodeToScrollTo Scroll the tree to this node
     * @param paths The  paths to expand after we restructure the tree
     */
    private void treeStructureChanged(DataTreeNode nodeToScrollTo,
                                      Hashtable paths) {
        tree.clearPaths();
        treeModel.nodeStructureChanged(treeRoot);
        if (nodeToScrollTo != null) {
            //            System.err.println("Calling exandPath from treeStructureChanged " + nodeToScrollTo);
            //            tree.expandPath(getPath(nodeToScrollTo));
        }

        if (paths != null) {
            //            System.err.println("Calling from treeStructureChanged");
            //            expandPaths(paths);
        }

        //treeModel.nodeStructureChanged (treeRoot); 
        if (nodeToScrollTo != null) {
            tree.scrollPathToVisible(getPath(nodeToScrollTo));
            tree.setSelectionPath(getPath(nodeToScrollTo));
        }
    }


    /**
     *  Remove all data sources from this tree
     */
    public synchronized void removeAllDataSources() {
        super.removeAllDataSources();
        treeStructureChanged(null);
        rebuildMaps();
    }

    /**
     *  This data tree keeps a mapping between data object and tree node.
     *  This method creates a new map.
     */
    private void rebuildMaps() {
        dataToTreeNode = new Hashtable();
        rebuildMaps(treeRoot);
    }

    /**
     * This walks the tree and rebuilds the data to tree node mappings
     *
     * @param node  The node we are recursing down on
     */
    private void rebuildMaps(TreeNode node) {
        if (node == null) {
            return;
        }
        if (node instanceof DataTreeNode) {
            DataTreeNode treeNode = (DataTreeNode) node;
            if (treeNode.getObject() != null) {
                dataToTreeNode.put(treeNode.getObject(), node);
            }
        }
        for (Enumeration nodes = node.children(); nodes.hasMoreElements(); ) {
            rebuildMaps((TreeNode) nodes.nextElement());
        }
    }

    /**
     *  Remove the specified data source only if it is not the formulaDataSource.
     *
     * @param dataSource The data source to remove
     */
    public void removeDataSource(DataSource dataSource) {
        super.removeDataSource(dataSource);
        treeStructureChanged(null);
        rebuildMaps();
    }


    /**
     *  Remove the specified data source only if it is not the formulaDataSource.
     *
     * @param dataSource The data source to remove
     * @return Was this removal successful
     */
    protected boolean removeDataSourceInner(DataSource dataSource) {
        if ( !super.removeDataSourceInner(dataSource)) {
            return false;
        }
        if (showDataSourceNode) {
            removeObject(dataSource);
        } else {
            treeRoot.removeAllChildren();
        }
        return true;
    }


    /**
     * Something changed about the given data source. Simply re-add it.
     *
     * @param source The data source that changed.
     */
    public void dataSourceChanged(DataSource source) {
        addDataSource(source);
    }


    /**
     *  Add the given {@link ucar.unidata.data.DataSource} and
     * its {@link ucar.unidata.data.DataChoice}-s into the jtree.
     *
     * @param dataSource The data source to add
     */
    public void addDataSource(DataSource dataSource) {

        //Get the list of expanded tree paths now.
        Hashtable    paths          = getExpandedPaths();

        DataTreeNode dataSourceNode = getTreeNode(dataSource);
        boolean      newDataSource  = true;
        DataTreeNode parentNode     = null;

        if ( !showDataSourceNode) {
            treeRoot.removeAllChildren();
        }
        if (dataSourceNode != null) {
            //Already have this. Empty the children
            dataSourceNode.removeAllChildren();
            newDataSource = false;
        } else {
            if (treatFormulaDataSourceSpecial
                    && DataManager.isFormulaDataSource(dataSource)
                    && (formulaDataSource == null)) {
                formulaDataSource = dataSource;
                dataSourceNode    = getTreeNode(dataSource);
                //Create the Formula tree node if needed
                if (dataSourceNode == null) {
                    if (showDataSourceNode) {
                        treeRoot.insert(dataSourceNode =
                            createTreeNode(dataSource.toString(),
                                           dataSource), 0);
                    }
                }
            } else {
                if (showDataSourceNode) {
                    treeRoot.add(dataSourceNode =
                        createTreeNode(dataSource.toString(), dataSource));
                }
            }
            super.addDataSource(dataSource);
        }

        if (showDataSourceNode && (dataSourceNode != null)) {
            parentNode = dataSourceNode;
        } else {
            parentNode = treeRoot;
        }

        List         choices                = dataSource.getDataChoices();
        List         nodesToExpand          = new ArrayList();
        List         initialSelectedChoices = null;

        int          cnt                    = 0;
        DataTreeNode lastNode               = null;
        Hashtable    catToNode              = new Hashtable();
        if (DataManager.isFormulaDataSource(dataSource)) {
            List pairs = new ArrayList();
            for (int i = 0; i < choices.size(); i++) {
                DataChoice choice = (DataChoice) choices.get(i);
                String     desc   = choice.getDescription();
                if (desc.trim().length() == 0) {
                    desc = "no name";
                }
                pairs.add(new Object[] { desc, choice });
            }
            pairs   = Misc.sortTuples(pairs, true);
            choices = new ArrayList();
            for (int i = 0; i < pairs.size(); i++) {
                choices.add(((Object[]) pairs.get(i))[1]);
            }
        }


        for (int i = 0; i < choices.size(); i++) {
            DataChoice choice = (DataChoice) choices.get(i);
            //We can have some DataChoices that are not intended for the user
            if ( !choice.getForUser()) {
                continue;
            }

            //If we have a list of DataCategory-ies then check if we are applicable
            //to the categories of the current DataChoice. If not then don't show it.
            if (categories != null) {
                /*
                  System.err.println (choice + " - " +DataCategory.applicableTo(categories,
                  choice.getCategories()) +" choice:" + choice.getCategories());*/
                if(!(choice instanceof CompositeDataChoice)){
                    if ( !DataCategory.applicableTo(categories,
                            choice.getCategories())) {
                        continue;
                    }
                }
            }
            cnt++;

            if (initialSelectedFieldName != null) {
                if (choice.toString().equals(initialSelectedFieldName)
                        || choice.getName().equals(
                            initialSelectedFieldName)) {
                    initialSelectedChoices   = Misc.newList(choice);
                    initialSelectedFieldName = null;
                }
            }


            //Create and initialize the treeNode for this DataChoice
            //            DataTreeNode fieldNode = createTreeNode (choice.toString (), choice);
            String desc = choice.getDescription();
            if (desc.trim().length() == 0) {
                desc = "no name";
            }
            DataTreeNode fieldNode = createTreeNode(desc, choice);
            lastNode = fieldNode;

            fieldNode.setIsDerived((choice instanceof DerivedDataChoice));

            //Now run through the DataChoices display categories
            //to create the tree
            DataCategory topCategory = choice.getDisplayCategory();

            DataTreeNode parent      = parentNode;
            String       catPath     = null;
            while (topCategory != null) {
                String catName = topCategory.getName();
                if ( !catName.equals("skip")) {
                    if (catPath == null) {
                        catPath = catName;
                    } else {
                        catPath = catPath + DataCategory.DIVIDER + catName;
                    }
                    DataTreeNode nextNode =
                        (DataTreeNode) catToNode.get(catPath);
                    if (nextNode == null) {
                        nextNode = createTreeNode(parent, catName, catPath);
                        catToNode.put(catPath, nextNode);
                    }
                    parent = nextNode;
                }
                topCategory = topCategory.getChild();
            }
            parent.add(fieldNode);
            if (choice instanceof CompositeDataChoice) {
                fieldNode.add(new DefaultMutableTreeNode(""));
                //                createSubtree((CompositeDataChoice) choice, fieldNode);
            }
        }


        if (showDataSourceNode && (dataSourceNode != null) && (cnt == 0)) {
            DataTreeNode parent = (DataTreeNode) dataSourceNode.getParent();
            parent.remove(dataSourceNode);
        }

        GuiUtils.moveSubtreesToTop(parentNode);

        //For now rebuild the data object to tree node mapping
        //This is overkill
        rebuildMaps();
        if ((cnt != 0) || true) {
            treeStructureChanged(parentNode);
        }

        for (int i = 0; i < nodesToExpand.size(); i++) {
            DataTreeNode nodeToExpand = (DataTreeNode) nodesToExpand.get(i);
            TreePath     path         = getPath(nodeToExpand);
            tree.expandPath(path);
        }


        if (cnt == 1) {
            if (lastNode.getParent() != parentNode) {
                for (Enumeration nodes = parentNode.children();
                        nodes.hasMoreElements(); ) {
                    parentNode.remove(
                        (DefaultMutableTreeNode) nodes.nextElement());
                }
                parentNode.add(lastNode);
            }
            treeStructureChanged(lastNode);
        }

        //Now select any initial nodes we  might
        if (initialSelectedChoices != null) {
            selectChoices(initialSelectedChoices, true);
        }

        GuiUtils.expandPathsAfterChange(tree, paths, treeRoot);

    }


    /**
     * Recurse down the {@link ucar.unidata.data.CompositeDataChoice}
     * hierarhcy of DataChoice-s, creating the JTree tree.
     *
     * @param choice The data choice to recurse down on
     * @param treeNode The tree node which represents the given
     * composite data choice
     */
    protected void createSubtree(CompositeDataChoice choice,
                                 DataTreeNode treeNode) {
        List children = choice.getDataChoices();
        for (int i = 0; i < children.size(); i++) {
            DataChoice   child     = (DataChoice) children.get(i);
            DataTreeNode childNode = createTreeNode(child.toString(), child);
            treeNode.add(childNode);
            if (child instanceof CompositeDataChoice) {
                createSubtree((CompositeDataChoice) child, childNode);
            }
        }
    }


    /**
     *  If the tree has a single path to a DataChoice then open up the tree
     * to that data choice and select it.
     */
    public void openUp() {
        openUp(treeRoot, null);
    }


    /**
     * Recurse down the tree, looking for the first DataChoice node. If any
     * nodes we encounter have more than one child then give up.
     *
     * @param node The tree node we're looking at.
     * @param parent Its parent
     */
    private void openUp(DataTreeNode node, DataTreeNode parent) {
        Object object = node.getObject();
        if (object instanceof DataChoice) {
            tree.expandPath(getPath(parent));
            TreeNode[] path = treeModel.getPathToRoot(node);
            tree.addSelectionPath(new TreePath(path));
            return;
        }

        if ((node.getChildCount() > 1) || (node.getChildCount() == 0)) {
            return;
        }
        openUp((DataTreeNode) node.getChildAt(0), node);
    }

    /**
     * Find and return the first DataChoice that is selected.
     *
     * @return The first selected data choice
     */
    public DataChoice getSelectedDataChoice() {
        List choices = getSelectedDataChoices();
        if (choices.size() == 0) {
            return null;
        }
        return (DataChoice) choices.get(0);
    }

    /**
     * Find the first selected data choice that the given
     * {@link ucar.unidata.idv.ControlDescriptor} is applicable
     * to.
     *
     * @param descriptor The descriptor to check for data choice applicability
     * @return Selected DataChoice or null if none found
     */
    public DataChoice getSelectedDataChoice(ControlDescriptor descriptor) {
        List choices = getSelectedDataChoices();
        for (int i = 0; i < choices.size(); i++) {
            DataChoice dataChoice = (DataChoice) choices.get(i);
            if (descriptor.applicableTo(dataChoice)) {
                return dataChoice;
            }
        }
        return null;
    }


    /**
     * Get the list of selected data choices
     *
     * @return List of selected data choices
     */
    public List<DataChoice> getSelectedDataChoicesRecursive() {
        return null;
    }



    /**
     * Get the list of selected data choices
     *
     * @return List of selected data choices
     */
    public List<DataChoice> getSelectedDataChoices() {
        TreePath[]       paths = tree.getSelectionModel().getSelectionPaths();
        List<DataChoice> choices = new ArrayList<DataChoice>();
        if (paths == null) {
            return choices;
        }
        for (int i = 0; i < paths.length; i++) {
            Object last = paths[i].getLastPathComponent();
            if (last == null) {
                continue;
            }
            Object object = ((DataTreeNode) last).getObject();
            if ( !(object instanceof DataChoice)) {
                continue;
            }
            DataChoice choice = (DataChoice) object;
            choices.add(choice);
        }
        return choices;
    }



    /**
     * Find the data object that is contained by the
     * tree node nearest to the given x/y position.
     *
     * @param x x position
     * @param y y position
     * @return Nearest data object
     */
    public Object getObjectAt(int x, int y) {
        TreePath path = tree.getPathForLocation(x, y);
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last == null) {
            return null;
        }
        Object object = ((DataTreeNode) last).getObject();
        if (object == null) {
            return null;
        }
        //If user clicks on a data choice then select it
        if (object instanceof DataChoice) {
            tree.setSelectionPath(path);
        }
        return object;
    }



}
