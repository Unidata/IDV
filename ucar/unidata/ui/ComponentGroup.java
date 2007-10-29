/*
 * $Id: DisplayGroup.java,v 1.13 2007/04/16 21:32:37 jeffmc Exp $
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


package ucar.unidata.ui;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;

import java.awt.*;
import java.awt.event.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import javax.swing.tree.*;






/**
 * Holds a group of display components
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class ComponentGroup extends ComponentHolder {

    /** type of layout */
    public static final int LAYOUT_GRIDBAG = 0;

    /** type of layout */
    public static final int LAYOUT_GRID = 1;

    /** type of layout */
    public static final int LAYOUT_TABS = 2;

    /** type of layout */
    public static final int LAYOUT_HSPLIT = 3;

    /** type of layout */
    public static final int LAYOUT_VSPLIT = 4;

    /** _more_          */
    public static final int LAYOUT_GRAPH = 5;

    /** _more_          */
    public static final int LAYOUT_TREE = 6;

    public static final String[]LAYOUT_NAMES = {"Columns", "Grid","Tabs","Hor. Split","Vert. Split", "Graph", "Tree"};

    /** type of layout */
    private int layout = LAYOUT_GRID;

    /** The gui */
    protected JComponent container;

    /** outermost gui component */
    protected JComponent outerContainer;

    /** List of display components */
    private List displayComponents = new ArrayList();

    /** Tabbed pane */
    private JTabbedPane tabbedPane;

    /** for layout */
    private int gridColumns = 1;

    /** for layout */
    private int numRows = 0;

    /** for layout */
    private int numColumns = 1;


    /** Last position of the internal frame */
    private Rectangle lastPosition;

    /** internal frame state */
    private boolean iconified = false;

    /** for properties dialog */
    private Vector propertiesList;

    /** properties widget */
    private JTextField dimFld;

    /** properties widget */
    private JTextField colFld;

    /** properties widget */
    private JRadioButton tabLayoutBtn;

    /** properties widget */
    private JRadioButton hsplitLayoutBtn;

    /** properties widget */
    private JRadioButton vsplitLayoutBtn;

    /** properties widget */
    private JRadioButton gridLayoutBtn;

    /** properties widget */
    private JRadioButton graphLayoutBtn;

    /** properties widget */
    private JRadioButton treeLayoutBtn;

    /** properties widget */
    private JRadioButton gridbagLayoutBtn;


    /** properties widget */
    private JRadioButton rowBtn;


    /** properties widget */
    private JList displayList;

    /** for properties dialog */
    private boolean displayOrderChanged;


    /** _more_          */
    private JTree tree;

    /** _more_          */
    private JScrollPane treeSP;


    /**
     * default ctor
     */
    public ComponentGroup() {}


    /**
     * ctor
     *
     * @param name name
     */
    public ComponentGroup(String name) {
        super(name);
    }


    /**
     * What type of thing is this
     *
     * @return type name
     */
    public String getTypeName() {
        return "Group";
    }


    /**
     * Create and return the gui contents
     *
     * @return gui contents
     */
    public JComponent doMakeContents() {
        tabbedPane     = new JTabbedPane();
        container      = new JPanel(new GridLayout(numRows, numColumns, 5,
                5));
        outerContainer = GuiUtils.center(container);
        redoLayout();
        return outerContainer;
    }



    /**
     * Make the edit menu items
     *
     *
     * @param items Holds the menu items
     *
     * @return The list of items
     */
    protected List getPopupMenuItems(List items) {
        super.getPopupMenuItems(items);
        //        items.add(GuiUtils.makeMenuItem("Remove Group", this,
        //                                        "removeDisplayComponent"));
        //        items.add(GuiUtils.MENU_SEPARATOR);
        return items;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getPropertyTabs() {
        return new String[] { "Properties", "Tree View" };
    }



    /**
     * Show dialog
     *
     * @param comps  List of components
     * @param tabIdx which tab
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {

        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx == 1) {
            if (tree == null) {
                tree = new MyDndTree();
                treeSP = GuiUtils.makeScrollPane(tree, 300, 400);
                tree.setToolTipText(
                    "Right click to show menu; Double click to show properties dialog");
                tree.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent event) {
                        TreePath path = tree.getPathForLocation(event.getX(),
                                            event.getY());
                        if (path == null) {
                            return;
                        }
                        Object last = path.getLastPathComponent();
                        if (last == null) {
                            return;
                        }
                        DefaultMutableTreeNode node =
                            (DefaultMutableTreeNode) last;
                        ComponentHolder comp =
                            (ComponentHolder) node.getUserObject();

                        if (SwingUtilities.isRightMouseButton(event)) {
                            comp.showPopup(treeSP, event.getX(),
                                           event.getY());
                            return;
                        }
                        if (event.getClickCount() > 1) {
                            comp.showProperties(tree, 0, 0);
                        }
                    }
                });

            }
            tree.setModel(new DefaultTreeModel(makeTree(null)));
            comps.add(new JLabel(""));
            comps.add(treeSP);
            return;
        }
        ButtonGroup bg        = new ButtonGroup();
        boolean     fixedRows = getNumRows() != 0;
        displayOrderChanged = false;
        displayList =
            new JList(propertiesList = new Vector(displayComponents));
        displayList.setToolTipText(
            "Press 'delete' to remove selected display");
        JComponent displayPanel = GuiUtils.makeScrollPane(displayList, 300,
                                      100);


        displayList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() > 1) {
                    ComponentHolder comp =
                        (ComponentHolder) displayList.getSelectedValue();
                    if (comp != null) {
                        comp.showProperties(displayList, 0, 0);
                    }
                }
            }
        });

        displayList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ((e.getKeyCode() == KeyEvent.VK_DELETE)
                        && (displayList.getSelectedIndex() >= 0)) {
                    displayOrderChanged = true;
                    propertiesList.remove(displayList.getSelectedIndex());
                    displayList.setListData(new Vector(propertiesList));
                }
            }
        });
        JButton upButton =
            GuiUtils.getImageButton("/auxdata/ui/icons/Up16.gif", getClass());
        upButton.setActionCommand("up");
        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int selectedIndex = displayList.getSelectedIndex();
                if (selectedIndex > 0) {
                    displayOrderChanged = true;
                    Object o = propertiesList.remove(selectedIndex);
                    propertiesList.add(selectedIndex - 1, o);
                    displayList.setListData(new Vector(propertiesList));
                    displayList.setSelectedIndex(propertiesList.indexOf(o));
                }
            }
        });
        JButton downButton =
            GuiUtils.getImageButton("/auxdata/ui/icons/Down16.gif",
                                    getClass());
        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                int selectedIndex = displayList.getSelectedIndex();
                if (selectedIndex < propertiesList.size() - 1) {
                    displayOrderChanged = true;
                    Object o = propertiesList.remove(selectedIndex);
                    propertiesList.add(selectedIndex + 1, o);
                    displayList.setListData(new Vector(propertiesList));
                    displayList.setSelectedIndex(propertiesList.indexOf(o));
                }
            }
        });
        displayPanel =
            GuiUtils.centerRight(displayPanel,
                                 GuiUtils.top(GuiUtils.vbox(upButton,
                                     downButton)));

        rowBtn = new JRadioButton("Rows", fixedRows);
        JRadioButton colBtn = new JRadioButton("Columns", !fixedRows);
        bg.add(rowBtn);
        bg.add(colBtn);

        hsplitLayoutBtn = new JRadioButton("Horizontal Split Pane ",
                                           layout == LAYOUT_HSPLIT);
        vsplitLayoutBtn = new JRadioButton("Vertical Split Pane ",
                                           layout == LAYOUT_VSPLIT);
        tabLayoutBtn  = new JRadioButton("Tabs ", layout == LAYOUT_TABS);
        gridLayoutBtn = new JRadioButton("Grid ", layout == LAYOUT_GRID);
        graphLayoutBtn = new JRadioButton("Graph Paper",
                                          layout == LAYOUT_GRAPH);
        treeLayoutBtn = new JRadioButton("Tree Panel", layout == LAYOUT_TREE);
        gridbagLayoutBtn = new JRadioButton("Columns ",
                                            layout == LAYOUT_GRIDBAG);
        ButtonGroup group = GuiUtils.buttonGroup(tabLayoutBtn, gridLayoutBtn);
        group.add(graphLayoutBtn);
        group.add(treeLayoutBtn);
        group.add(gridbagLayoutBtn);
        group.add(hsplitLayoutBtn);
        group.add(vsplitLayoutBtn);
        dimFld = new JTextField((fixedRows
                                 ? getNumRows()
                                 : getNumColumns()) + "", 5);

        colFld = new JTextField(getGridColumns() + "", 5);



        comps.add(GuiUtils.rLabel("Layout: "));
        comps.add(GuiUtils.left(GuiUtils.hbox(tabLayoutBtn, hsplitLayoutBtn,
                vsplitLayoutBtn, treeLayoutBtn)));

        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(GuiUtils.hbox(gridLayoutBtn,
                new JLabel("  Dimension: "), dimFld,
                GuiUtils.hbox(colBtn, rowBtn))));

        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(GuiUtils.hbox(gridbagLayoutBtn,
                new JLabel("  # Columns: "), colFld)));



        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(GuiUtils.hbox(graphLayoutBtn,
                GuiUtils.makeButton("Edit", this, "editLayout"))));



        comps.add(GuiUtils.top(GuiUtils.rLabel("Displays: ")));
        comps.add(displayPanel);
    }






    /**
     * _more_
     *
     * @param parent _more_
     *
     * @return _more_
     */
    public DefaultMutableTreeNode makeTree(DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode node = super.makeTree(parent);
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder comp = (ComponentHolder) displayComponents.get(i);
            comp.makeTree(node);
        }
        return node;
    }



    /**
     * Recursively find all contained components of the given class
     *
     * @param compClass The class to look for
     *
     * @return List of components
     */
    public List findComponentsWithType(Class compClass) {
        List comps = new ArrayList();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder displayComponent =
                (ComponentHolder) displayComponents.get(i);
            if (compClass.isAssignableFrom(displayComponent.getClass())) {
                comps.add(displayComponent);
            }
            if (displayComponent instanceof ComponentGroup) {
                comps.addAll(((ComponentGroup) displayComponent)
                    .findComponentsWithType(compClass));
            }
        }
        return comps;
    }


    /**
     * Layout components
     */
    private void redoLayout() {
        if (tabbedPane == null) {
            return;
        }
        tabbedPane.removeAll();
        container.setVisible(false);
        container.removeAll();
        if (layout == LAYOUT_GRID) {
            container.setLayout(new GridLayout(numRows, numColumns, 5, 5));
        } else {
            container.setLayout(new BorderLayout());
        }

        List comps = new ArrayList();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder displayComponent =
                (ComponentHolder) displayComponents.get(i);
            JComponent comp = displayComponent.getContents();
            if (layout == LAYOUT_TABS) {
                tabbedPane.add(displayComponent.getName(), comp);
            } else if (layout == LAYOUT_GRID) {
                container.add(comp);
            } else {
                comps.add(comp);
            }
        }



        if (layout == LAYOUT_TABS) {
            container.add(BorderLayout.CENTER, tabbedPane);
        } else if (layout == LAYOUT_GRID) {
            //noop
        } else if (layout == LAYOUT_HSPLIT) {
            if (comps.size() > 0) {
                container.add(BorderLayout.CENTER,
                              GuiUtils.doMultiSplitPane(comps, true));
            }
        } else if (layout == LAYOUT_VSPLIT) {
            if (comps.size() > 0) {
                container.add(BorderLayout.CENTER,
                              GuiUtils.doMultiSplitPane(comps, false));
            }
        } else if (layout == LAYOUT_GRAPH) {
            container.add(GraphPaperLayout.layout(getLocations()));
        } else if (layout == LAYOUT_TREE) {
            TreePanel treePanel = new TreePanel();
            for (int i = 0; i < displayComponents.size(); i++) {
                ComponentHolder comp =
                    (ComponentHolder) displayComponents.get(i);
                if (comp.getName() == null) {
                    System.err.println("name is null "
                                       + comp.getClass().getName());
                }
                treePanel.addComponent(comp.getContents(),
                                       comp.getCategory(), comp.getName(),
                                       null);
            }
            container.add(treePanel);
        } else {
            container.add(BorderLayout.CENTER,
                          GuiUtils.doLayout(comps, gridColumns,
                                            GuiUtils.WT_Y, GuiUtils.WT_Y));
        }
        container.setVisible(true);
        container.validate();
    }


    /**
     * Get the GraphPaperLayout locations
     *
     * @return locations for layout
     */
    private List getLocations() {
        List locations = new ArrayList();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder displayComponent =
                (ComponentHolder) displayComponents.get(i);
            if ( !displayComponent.getBeingShown()) {
                continue;
            }
            locations.add(
                new GraphPaperLayout.Location(
                    displayComponent.getContents(), displayComponent,
                    displayComponent.getName(),
                    displayComponent.getLayoutRect()));
        }
        return locations;
    }

    /**
     * apply graph paper layout locations
     *
     * @param locations locations
     */
    private void applyLocations(List locations) {
        for (int i = 0; i < locations.size(); i++) {
            GraphPaperLayout.Location loc =
                (GraphPaperLayout.Location) locations.get(i);
            ComponentHolder componentHolder =
                (ComponentHolder) loc.getObject();
            componentHolder.setLayoutRect(loc.getRect());
        }
    }



    /**
     * _more_
     */
    public void editLayout() {
        graphLayoutBtn.setSelected(true);
        List locations = getLocations();
        GraphPaperLayout.showDialog(locations, "Edit Group Layout");
        applyLocations(locations);
        redoLayout();
    }



    /**
     * Add the wrapper
     *
     * @param displayComponent new one
     */
    public void addComponent(ComponentHolder displayComponent) {
        addComponent(displayComponent, -1);
    }



    /**
     * What is the index of the child component
     *
     * @param displayComponent child component
     *
     * @return its index
     */
    public int indexOf(ComponentHolder displayComponent) {
        return displayComponents.indexOf(displayComponent);
    }


    /**
     * Add the wrapper
     *
     * @param displayComponent new one
     * @param index Where
     */
    public void addComponent(ComponentHolder displayComponent, int index) {
        boolean wasMine = displayComponents.contains(displayComponent);
        if(wasMine) {
            displayComponents.remove(displayComponent);
        }
        if (index >= 0 && index< displayComponents.size()) {
            displayComponents.add(index, displayComponent);
        } else {
            displayComponents.add(displayComponent);
        }
        if(!wasMine) {
            displayComponent.setParent(this);
        }
        redoLayout();
        subtreeChanged();

    }


    /**
     * _more_
     */
    protected void subtreeChanged() {
        if (tree != null) {
            Hashtable paths = GuiUtils.initializeExpandedPathsBeforeChange(tree,(DefaultMutableTreeNode)tree.getModel().getRoot());
            tree.setModel(new DefaultTreeModel(makeTree(null)));
            GuiUtils.expandPathsAfterChange(tree, paths, (DefaultMutableTreeNode)tree.getModel().getRoot());
        }
        if (parent != null) {
            parent.subtreeChanged();
        }
    }




    /**
     * Apply properties
     *
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        String  oldName = getName();
        boolean result  = super.applyProperties();
        if ( !result) {
            return false;
        }
        try {
            if (displayOrderChanged) {
                displayComponents = new ArrayList(propertiesList);
            }

            if (hsplitLayoutBtn.isSelected()) {
                layout = LAYOUT_HSPLIT;
            }
            if (vsplitLayoutBtn.isSelected()) {
                layout = LAYOUT_VSPLIT;
            }

            if (tabLayoutBtn.isSelected()) {
                layout = LAYOUT_TABS;
            }
            if (gridLayoutBtn.isSelected()) {
                layout = LAYOUT_GRID;
            }
            if (graphLayoutBtn.isSelected()) {
                layout = LAYOUT_GRAPH;
            }
            if (treeLayoutBtn.isSelected()) {
                layout = LAYOUT_TREE;
            }
            if (gridbagLayoutBtn.isSelected()) {
                layout = LAYOUT_GRIDBAG;
            }

            if (rowBtn.isSelected()) {
                setRowsColumns(
                    new Integer(dimFld.getText().trim()).intValue(), 0);
            } else {
                setRowsColumns(
                    0, new Integer(dimFld.getText().trim()).intValue());
            }
            setGridColumns(new Integer(colFld.getText().trim()).intValue());
            redoLayout();
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad number format " + nfe);
            return false;
        }



        String newName = getName();
        if ( !newName.equals(oldName)) {
            // getDisplayControl().newName(DisplayGroup.this, oldName);

        }
        return result;

    }



    /**
     * remove the wrapper
     *
     * @param displayComponent the wrapper to remove
     */
    public void removeComponent(ComponentHolder displayComponent) {
        displayComponents.remove(displayComponent);
        displayComponent.setParent(null);
        //        getContents();
        redoLayout();
        subtreeChanged();
    }


    /**
     * do cleanup
     */
    public void doRemove() {
        if (isRemoved) {
            return;
        }
        List tmp = new ArrayList(displayComponents);
        for (int i = 0; i < tmp.size(); i++) {
            ((ComponentHolder) tmp.get(i)).doRemove();
        }
        displayComponents = null;
        super.doRemove();
    }



    /**
     * Make the edit menu items
     *
     *
     * @param items Holds the menu items
     *
     * @param value _more_
     *
     * @return The list of items
     */
    //    protected List getPopupMenuItems(List items) {
    //    }


    /**
     * Set the DisplayComponents property.
     *
     * @param value The new value for DisplayComponents
     */
    public void setDisplayComponents(List value) {
        displayComponents = value;
    }

    /**
     * Get the DisplayComponents property.
     *
     * @return The DisplayComponents
     */
    public List getDisplayComponents() {
        return displayComponents;
    }


    /**
     * Set layout
     *
     * @param rows rows
     * @param cols cols
     */
    public void setRowsColumns(int rows, int cols) {
        numRows    = rows;
        numColumns = cols;
    }

    /**
     * Set the NumRows property.
     *
     * @param value The new value for NumRows
     */
    public void setNumRows(int value) {
        numRows = value;
    }

    /**
     * Get the NumRows property.
     *
     * @return The NumRows
     */
    public int getNumRows() {
        return numRows;
    }

    /**
     * Set the NumColumns property.
     *
     * @param value The new value for NumColumns
     */
    public void setNumColumns(int value) {
        numColumns = value;
    }

    /**
     * Get the NumColumns property.
     *
     * @return The NumColumns
     */
    public int getNumColumns() {
        return numColumns;
    }


    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return "Group: " + getName() + " (" +LAYOUT_NAMES[layout]+")";
    }



    /**
     * Set the LastPosition property.
     *
     * @param value The new value for LastPosition
     */
    public void setLastPosition(Rectangle value) {
        lastPosition = value;
    }

    /**
     * Get the LastPosition property.
     *
     * @return The LastPosition
     */
    public Rectangle getLastPosition() {
        return lastPosition;
    }

    /**
     * Set the Iconified property.
     *
     * @param value The new value for Iconified
     */
    public void setIconified(boolean value) {
        iconified = value;
    }

    /**
     * Get the Iconified property.
     *
     * @return The Iconified
     */
    public boolean getIconified() {
        return iconified;
    }

    /**
     * _more_
     *
     * @param l _more_
     */
    public void setLayout(String l) {
        if (l.equals("tabs")) {
            setLayout(LAYOUT_TABS);
        } else if (l.equals("graph")) {
            setLayout(LAYOUT_GRAPH);
        } else if (l.equals("gridbag")) {
            setLayout(LAYOUT_GRIDBAG);
        } else if (l.equals("hsplit")) {
            setLayout(LAYOUT_HSPLIT);
        } else if (l.equals("vsplit")) {
            setLayout(LAYOUT_VSPLIT);
        } else if (l.equals("tree")) {
            setLayout(LAYOUT_TREE);
        }
    }

    /**
     * Set the Layout property.
     *
     * @param value The new value for Layout
     */
    public void setLayout(int value) {
        layout = value;
    }

    /**
     * Get the Layout property.
     *
     * @return The Layout
     */
    public int getLayout() {
        return layout;
    }

    /**
     * Set the GridColumns property.
     *
     * @param value The new value for GridColumns
     */
    public void setGridColumns(int value) {
        gridColumns = value;
    }

    /**
     * Get the GridColumns property.
     *
     * @return The GridColumns
     */
    public int getGridColumns() {
        return gridColumns;
    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        ComponentGroup  g1 = new ComponentGroup("Group 1");
        ComponentGroup  g2 = new ComponentGroup("Group 2");

        JComponent      lbl;
        ComponentHolder comp;
        g1.addComponent(comp = new ComponentHolder("label1",
                lbl = GuiUtils.wrap(new JLabel("label1"))));
        lbl.setBackground(Color.red);
        comp.setCategory("Foo");
        g1.addComponent(comp = new ComponentHolder("label2",
                lbl = GuiUtils.wrap(new JLabel("label2"))));
        comp.setCategory("Foo");
        lbl.setBackground(Color.blue);
        g1.addComponent(comp = new ComponentHolder("label3",
                lbl = GuiUtils.wrap(new JLabel("label3"))));
        comp.setCategory("Bar");

        lbl.setBackground(Color.green);
        g1.layout = LAYOUT_TABS;
        g1.redoLayout();


        g2.layout = LAYOUT_HSPLIT;
        g2.addComponent(g1);
        g2.addComponent(comp = new ComponentHolder("label4",
                lbl = GuiUtils.wrap(new JLabel("label4"))));
        lbl.setBackground(Color.cyan);
        g2.addComponent(comp = new ComponentHolder("label5",
                lbl = GuiUtils.wrap(new JLabel("label5"))));
        lbl.setBackground(Color.orange);

        JFrame f = new JFrame();
        f.setLocation(300, 300);
        f.getContentPane().add(g2.getContents());
        f.pack();
        f.show();
    }


    public static boolean isAncestor(ComponentGroup parent, ComponentHolder descendant) {
        if(descendant.getParent()==null) return false;
        if(descendant==parent) return true;
        return isAncestor(parent, descendant.getParent());
    }


    /**
     * _more_
     *
     * @param src _more_
     * @param dest _more_
     * @param on _more_
     */
    private void changeParent(ComponentHolder src, ComponentHolder dest,
                              boolean on) {
        if (on && dest instanceof ComponentGroup) {
            if (dest == src) {
                return;
            }
            src.setParent((ComponentGroup)dest);
            return;
        }

        ComponentGroup newParent =  dest.getParent();
        if(newParent == null) return;
        if(src.getParent()!=null) {
            src.getParent().removeComponent(src);
        }
        //        System.err.println ("source:" + src + " dest:" + dest + " " + on + " index=" +newParent.indexOf(dest));
        newParent.addComponent(src, newParent.indexOf(dest)+1);
    }



    private class MyDndTree extends DndTree {
        public MyDndTree() {}
        protected void doDrop(DefaultMutableTreeNode sourceNode,
                              DefaultMutableTreeNode destNode,
                              boolean onNode) {
            changeParent(
                         (ComponentHolder) sourceNode.getUserObject(),
                         (ComponentHolder) destNode.getUserObject(),
                         onNode);
        }
        protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                                   DefaultMutableTreeNode destNode,
                                   boolean onNode) {
                boolean result = okToDropx(sourceNode, destNode, onNode);
                //                System.err.println ("oktodrop:" + sourceNode + " " + destNode + " " + result);
                return result;
            }

        protected boolean okToDropx(DefaultMutableTreeNode sourceNode,
                                   DefaultMutableTreeNode destNode,
                                   boolean onNode) {
           
            if(!(sourceNode.getUserObject() instanceof ComponentGroup))return true;
            ComponentGroup srcGroup = (ComponentGroup) sourceNode.getUserObject();
            return !isAncestor(srcGroup,(ComponentHolder) destNode.getUserObject());
        }

    }

}

