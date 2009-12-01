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


import org.w3c.dom.Element;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
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
    public static final String LAYOUT_GRIDBAG = "gridbag";

    /** type of layout */
    public static final String LAYOUT_GRID = "grid";

    /** type of layout */
    public static final String LAYOUT_TABS = "tabs";

    /** type of layout */
    public static final String LAYOUT_HSPLIT = "hsplit";

    /** type of layout */
    public static final String LAYOUT_VSPLIT = "vsplit";

    /** type of layout */
    public static final String LAYOUT_GRAPH = "graph";

    /** type of layout */
    public static final String LAYOUT_TREE = "tree";

    /** type of layout */
    public static final String LAYOUT_BORDER = "border";

    /** type of layout */
    public static final String LAYOUT_DESKTOP = "desktop";

    /** type of layout */
    public static final String LAYOUT_MENU = "menu";

    /** user readable names of layouts */
    public static final String[] LAYOUT_NAMES = {
        "Columns", "Grid", "Tabs", "Hor. Split", "Vert. Split", "Graph",
        "Tree", "Border", "Menu", "Desktop"
    };

    /** all of the layouts */
    public static final String[] LAYOUTS = {
        LAYOUT_GRIDBAG, LAYOUT_GRID, LAYOUT_TABS, LAYOUT_HSPLIT,
        LAYOUT_VSPLIT, LAYOUT_GRAPH, LAYOUT_TREE, LAYOUT_BORDER, LAYOUT_MENU,
        LAYOUT_DESKTOP
    };

    /** _more_          */
    public static final List LAYOUT_LIST = Misc.toList(LAYOUTS);

    /** type of layout */
    private String layout = LAYOUT_GRID;

    /** The gui */
    protected JComponent container;

    /** outermost gui component */
    protected JComponent outerContainer;

    /** List of display components */
    private List displayComponents = new ArrayList();

    /** Tabbed pane */
    protected JTabbedPane tabbedPane;


    /** _more_          */
    private JComboBox menuBox;

    /** _more_          */
    private GuiUtils.CardLayoutPanel menuPanel;

    /** _more_ */
    private JDesktopPane desktop;


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

    /** _more_ */
    private JComboBox layoutBox;

    /** _more_ */
    private GuiUtils.CardLayoutPanel extraPanel;

    /** _more_ */
    private JComponent extraPanelHolder;

    /** _more_ */
    Hashtable borderLayoutLocations;

    /** properties widget */
    private JRadioButton rowBtn;

    /** _more_ */
    private JRadioButton colBtn;


    /** properties widget */
    private JList displayList;

    /** for properties dialog */
    private boolean displayOrderChanged;


    /** _more_ */
    private MyDndTree propertiesTree;

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
     * _more_
     *
     * @param node _more_
     */
    public void initWith(Element node) {
        super.initWith(node);
        String layoutString = XmlUtil.getAttribute(node, "layout",
                                  (String) null);
        if (layoutString != null) {
            setLayout(layoutString);
            if (layout.equals(LAYOUT_GRIDBAG)) {
                gridColumns = XmlUtil.getAttribute(node, "layout_columns",
                        gridColumns);
            }
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     */
    public void setState(Element node) {
        super.setState(node);
        node.setAttribute("layout", getLayout());
        if (layout.equals(LAYOUT_GRIDBAG)) {
            node.setAttribute("layout_columns", gridColumns + "");
        }
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
        desktop    = new JDesktopPane();
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (isLayout(LAYOUT_TABS)) {
                    GuiUtils.checkHeavyWeightComponents(tabbedPane);
                }
            }
        });
        //        GuiUtils.handleHeavyWeightComponentsInTabs(tabbedPane);
        container      = new JPanel(new GridLayout(numRows, numColumns, 5,
                5));
        outerContainer = GuiUtils.center(container);
        redoLayout();
        return outerContainer;
    }


    /**
     * _more_
     *
     * @param contents _more_
     *
     * @return _more_
     */
    protected JComponent wrapContents(JComponent contents) {
        DropPanel dropPanel = new DropPanel() {
            public void handleDrop(Object object) {
                doDrop(object);
            }
            public boolean okToDrop(Object object) {
                return dropOk(object);
            }
        };
        dropPanel.add(BorderLayout.CENTER, contents);
        return dropPanel;
    }


    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     */
    public boolean dropOk(Object object) {
        return false;
    }

    /**
     * _more_
     *
     * @param obj _more_
     */
    protected void doDrop(Object obj) {}

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
        List subItems = new ArrayList();

        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder comp = (ComponentHolder) displayComponents.get(i);
            List            compItems = new ArrayList();
            comp.getPopupMenuItems(compItems);
            compItems.add(GuiUtils.MENU_SEPARATOR);
            compItems.add(GuiUtils.makeMenuItem("Properties...", comp,
                    "showProperties"));
            subItems.add(GuiUtils.makeMenu(comp.getName(), compItems));
        }


        if (subItems.size() > 0) {
            items.add(GuiUtils.MENU_SEPARATOR);
            items.add(GuiUtils.makeMenu("Components", subItems));
        }


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
    public String[] xxxxgetPropertyTabs() {
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
        if (propertiesTree == null) {
            propertiesTree = new MyDndTree();
        }
        propertiesTree.setModel(new DefaultTreeModel(makeTree(null)));




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

                if(GuiUtils.isDeleteEvent(e)
                        && (displayList.getSelectedIndex() >= 0)) {
                    displayOrderChanged = true;
                    propertiesList.remove(displayList.getSelectedIndex());
                    displayList.setListData(new Vector(propertiesList));
                }
            }
        });

        Vector layoutList = new Vector(TwoFacedObject.createList(LAYOUTS,
                                LAYOUT_NAMES));
        layoutBox = new JComboBox(layoutList);
        Object selectedLayout = TwoFacedObject.findId(layout, layoutList);
        if (selectedLayout == null) {
            selectedLayout = layoutList.get(0);
        }
        layoutBox.setSelectedItem(selectedLayout);
        layoutBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                checkExtraPanel(
                    TwoFacedObject.getIdString(layoutBox.getSelectedItem()));
            }
        });

        makeExtraPanel();
        comps.add(GuiUtils.rLabel("Layout: "));
        comps.add(GuiUtils.left(GuiUtils.leftCenter(layoutBox,
                GuiUtils.inset(extraPanelHolder, new Insets(0, 5, 0, 0)))));

        comps.add(GuiUtils.rLabel("Tree:"));
        propertiesTree.treeSP.setPreferredSize(new Dimension(200, 300));
        comps.add(propertiesTree.treeSP);


    }


    /**
     * _more_
     */
    private void makeExtraPanel() {
        if (rowBtn == null) {
            boolean fixedRows = getNumRows() != 0;
            rowBtn = new JRadioButton("Rows", fixedRows);
            colBtn = new JRadioButton("Columns", !fixedRows);
            GuiUtils.buttonGroup(rowBtn, colBtn);
            dimFld = new JTextField((fixedRows
                                     ? getNumRows()
                                     : getNumColumns()) + "", 5);

            colFld = new JTextField(getGridColumns() + "", 5);
        }

        borderLayoutLocations = new Hashtable();
        List borderComps = new ArrayList();
        Vector borderLayouts = new Vector(Misc.newList(BorderLayout.CENTER,
                                   BorderLayout.NORTH, BorderLayout.EAST,
                                   BorderLayout.SOUTH, BorderLayout.WEST));
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder comp = (ComponentHolder) displayComponents.get(i);
            JComboBox       box  = new JComboBox(borderLayouts);
            box.setSelectedItem(comp.getBorderLayoutLocation());
            borderComps.add(new JLabel(comp.getName() + ": "));
            borderComps.add(box);
            borderLayoutLocations.put(comp, box);
        }


        extraPanel = new GuiUtils.CardLayoutPanel();
        extraPanel.add("", new JLabel(" "));
        extraPanel.add(LAYOUT_BORDER + "",
                       GuiUtils.left(GuiUtils.hbox(borderComps)));
        extraPanel.add(
            LAYOUT_GRID + "",
            GuiUtils.left(
                GuiUtils.hbox(
                    new JLabel("  Dimension: "), GuiUtils.wrap(dimFld),
                    GuiUtils.hbox(colBtn, rowBtn))));

        extraPanel.add(
            LAYOUT_GRIDBAG + "",
            GuiUtils.left(
                GuiUtils.hbox(
                    new JLabel("  # Columns: "), GuiUtils.wrap(colFld))));
        extraPanel.add(
            LAYOUT_GRAPH + "",
            GuiUtils.left(
                GuiUtils.wrap(
                    GuiUtils.makeButton("Edit", this, "editLayout"))));
        if (extraPanelHolder == null) {
            extraPanelHolder = new JPanel(new BorderLayout());

        }
        extraPanelHolder.removeAll();
        extraPanelHolder.add(BorderLayout.CENTER, extraPanel);
        if (layoutBox != null) {
            checkExtraPanel(
                TwoFacedObject.getIdString(layoutBox.getSelectedItem()));
        }
    }

    /**
     * _more_
     *
     * @param theLayout _more_
     */
    private void checkExtraPanel(String theLayout) {
        if (extraPanel.containsKey(theLayout)) {
            extraPanel.show(theLayout);
        } else {
            extraPanel.show("");
        }
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
     * _more_
     *
     * @param l _more_
     *
     * @return _more_
     */
    private boolean isLayout(String l) {
        return layout.equals(l);
    }

    /**
     * Layout components
     */
    public void redoLayout() {

        if (tabbedPane == null) {
            return;
        }
        desktop.removeAll();

        if ( !isLayout(LAYOUT_TABS) && (tabbedPane.getTabCount() > 0)) {
            GuiUtils.resetHeavyWeightComponents(tabbedPane);
            tabbedPane.removeAll();
        }
        container.setVisible(false);
        container.removeAll();

        if (displayComponents.size() == 0) {
            container.setVisible(true);
            container.setPreferredSize(new Dimension(100, 100));
            return;
        }
        container.setPreferredSize(null);

        if (isLayout(LAYOUT_GRID)) {
            container.setLayout(new GridLayout(numRows, numColumns, 5, 5));
        } else {
            container.setLayout(new BorderLayout());
        }

        int lastMenuIdx = 0;
        if (isLayout(LAYOUT_MENU)) {
            if (menuBox != null) {
                lastMenuIdx = menuBox.getSelectedIndex();
            }
            menuPanel = new GuiUtils.CardLayoutPanel();
        }
        Vector menuItems = new Vector();

        List   comps     = new ArrayList();
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder displayComponent =
                (ComponentHolder) displayComponents.get(i);
            JComponent comp = displayComponent.getContents();
            if (comp.getParent() != null) {
                comp.getParent().remove(comp);
            }
            comp.setVisible(true);
            GuiUtils.toggleHeavyWeightComponents(comp, true);
            if (isLayout(LAYOUT_TABS)) {
                tabbedPane.addTab(displayComponent.getName(),
                                  displayComponent.getIcon(), comp);
            } else if (isLayout(LAYOUT_DESKTOP)) {
                boolean shouldIconify =
                    !displayComponent.getInternalFrameShown();
                JInternalFrame frame = displayComponent.getInternalFrame();
                frame.getContentPane().add(comp);
                frame.pack();
                frame.show();
                desktop.add(frame);
                if (shouldIconify) {
                    try {
                        //For now don't do this                        frame.setIcon(true);
                    } catch (Exception ignore) {}
                }
            } else if (isLayout(LAYOUT_MENU)) {
                menuPanel.addCard(comp);
                menuItems.add(displayComponent.getName());
            } else if (isLayout(LAYOUT_GRID)) {
                container.add(comp);
            } else if (isLayout(LAYOUT_BORDER)) {
                container.add(displayComponent.getBorderLayoutLocation(),
                              comp);
            } else {
                comps.add(comp);
            }
        }

        if (isLayout(LAYOUT_TABS)) {
            container.add(BorderLayout.CENTER, tabbedPane);
        } else if (isLayout(LAYOUT_MENU)) {
            menuBox = new JComboBox(menuItems);
            menuBox.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (menuBox.getSelectedIndex() >= 0) {
                        menuPanel.show(menuBox.getSelectedIndex());
                    }
                }
            });
            if ((lastMenuIdx >= 0) && (displayComponents.size() > 0)
                    && (lastMenuIdx < displayComponents.size())) {
                menuBox.setSelectedIndex(lastMenuIdx);
                menuPanel.show(lastMenuIdx);
            }
            container.add(BorderLayout.CENTER,
                          GuiUtils.topCenter(GuiUtils.left(menuBox),
                                             menuPanel));
        } else if (isLayout(LAYOUT_DESKTOP)) {
            container.add(BorderLayout.CENTER, desktop);

            for (int i = 0; i < displayComponents.size(); i++) {
                ComponentHolder displayComponent =
                    (ComponentHolder) displayComponents.get(i);
                GuiUtils.toggleHeavyWeightComponents(displayComponent.getInternalFrame(),
                                                     displayComponent.getInternalFrame().isSelected());
            }
        } else if (isLayout(LAYOUT_HSPLIT)) {
            if (comps.size() > 0) {
                container.add(BorderLayout.CENTER,
                              GuiUtils.doMultiSplitPane(comps, true));
            }
        } else if (isLayout(LAYOUT_VSPLIT)) {
            if (comps.size() > 0) {
                container.add(BorderLayout.CENTER,
                              GuiUtils.doMultiSplitPane(comps, false));
            }
        } else if (isLayout(LAYOUT_GRAPH)) {
            container.add(GraphPaperLayout.layout(getLocations()));
        } else if (isLayout(LAYOUT_TREE)) {
            TreePanel treePanel = new TreePanel();
            for (int i = 0; i < displayComponents.size(); i++) {
                ComponentHolder comp =
                    (ComponentHolder) displayComponents.get(i);
                String name = comp.getName();
                if (name == null) {
                    name = "Component";
                }
                treePanel.addComponent(comp.getContents(),
                                       comp.getCategory(), name,
                                       comp.getIcon());
            }
            container.add(treePanel);
        } else if (isLayout(LAYOUT_GRIDBAG)) {
            container.add(BorderLayout.CENTER,
                          GuiUtils.doLayout(comps, gridColumns,
                                            GuiUtils.WT_Y, GuiUtils.WT_Y));
        }
        container.setVisible(true);
        container.revalidate();

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
        if (wasMine) {
            displayComponents.remove(displayComponent);
        }
        if ((index >= 0) && (index < displayComponents.size())) {
            displayComponents.add(index, displayComponent);
        } else {
            displayComponents.add(displayComponent);
        }
        displayComponent.setParent(this);
        redoLayout();
        subtreeChanged();

    }


    /**
     * _more_
     */
    protected void subtreeChanged() {
        if (propertiesTree != null) {
            Hashtable paths =
                GuiUtils
                    .initializeExpandedPathsBeforeChange(propertiesTree,
                        (DefaultMutableTreeNode) propertiesTree.getModel()
                            .getRoot());
            propertiesTree.setModel(new DefaultTreeModel(makeTree(null)));
            GuiUtils.expandPathsAfterChange(
                propertiesTree, paths,
                (DefaultMutableTreeNode) propertiesTree.getModel().getRoot());
        }
        if (parent != null) {
            parent.subtreeChanged();
        }
        makeExtraPanel();
    }




    /**
     * Apply properties
     *
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        boolean result = super.applyProperties();
        if ( !result) {
            return false;
        }
        try {

            for (int i = 0; i < displayComponents.size(); i++) {
                ComponentHolder comp =
                    (ComponentHolder) displayComponents.get(i);
                JComboBox box = (JComboBox) borderLayoutLocations.get(comp);
                if (box != null) {
                    comp.setBorderLayoutLocation(
                        (String) box.getSelectedItem());
                }
            }


            layout = TwoFacedObject.getIdString(layoutBox.getSelectedItem());
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
        return "Group: " + getName() + " (" + getLayout() + ")";
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
     * Set the Layout property.
     *
     * @param value The new value for Layout
     */
    public void setLayout(String value) {
        if ((value != null) && !LAYOUT_LIST.contains(value)) {
            throw new IllegalArgumentException("Unknown layout value:"
                    + value);

        }
        layout = value;

    }

    /**
     * Get the Layout property.
     *
     * @return The Layout
     */
    public String getLayout() {
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


    /**
     * _more_
     *
     * @param parent _more_
     * @param descendant _more_
     *
     * @return _more_
     */
    public static boolean isAncestor(ComponentGroup parent,
                                     ComponentHolder descendant) {
        if (descendant == parent) {
            return true;
        }

        if (descendant.getParent() == null) {
            return false;
        }

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
        if (on && (dest instanceof ComponentGroup)) {
            if (dest == src) {
                return;
            }
            ((ComponentGroup) dest).addComponent(src);
            src.setParent((ComponentGroup) dest);

            return;
        }

        ComponentGroup newParent = dest.getParent();
        if (newParent == null) {
            return;
        }
        if (src.getParent() != null) {
            src.getParent().removeComponent(src);
        }
        //        System.err.println ("source:" + src + " dest:" + dest + " " + on + " index=" +newParent.indexOf(dest));
        newParent.addComponent(src, newParent.indexOf(dest) + 1);
    }

    /**
     * _more_
     *
     * @param tab _more_
     */
    public void print(String tab) {
        System.err.println(tab + this);
        for (int i = 0; i < displayComponents.size(); i++) {
            ComponentHolder displayComponent =
                (ComponentHolder) displayComponents.get(i);
            displayComponent.print(tab + "   ");
        }
    }


    /**
     * Class MyDndTree _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class MyDndTree extends DndTree {

        /** _more_ */
        public JScrollPane treeSP;

        /**
         * _more_
         */
        public MyDndTree() {
            treeSP = GuiUtils.makeScrollPane(this, 300, 400);
            setToolTipText(
                "Right click to show menu; Double click to show properties dialog");
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent event) {
                    TreePath path = getPathForLocation(event.getX(),
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
                        comp.showPopup(treeSP, event.getX(), event.getY());
                        return;
                    }
                    if (event.getClickCount() > 1) {
                        comp.showProperties(MyDndTree.this, 0, 0);
                    }
                }
            });

        }

        /**
         * _more_
         *
         * @param sourceNode _more_
         * @param destNode _more_
         * @param onNode _more_
         */
        protected void doDrop(DefaultMutableTreeNode sourceNode,
                              DefaultMutableTreeNode destNode,
                              boolean onNode) {
            changeParent((ComponentHolder) sourceNode.getUserObject(),
                         (ComponentHolder) destNode.getUserObject(), onNode);
        }

        /**
         * _more_
         *
         * @param sourceNode _more_
         * @param destNode _more_
         * @param onNode _more_
         *
         * @return _more_
         */
        protected boolean okToDrop(DefaultMutableTreeNode sourceNode,
                                   DefaultMutableTreeNode destNode,
                                   boolean onNode) {
            boolean result = okToDropx(sourceNode, destNode, onNode);
            return result;
        }

        /**
         * _more_
         *
         * @param sourceNode _more_
         * @param destNode _more_
         * @param onNode _more_
         *
         * @return _more_
         */
        protected boolean okToDropx(DefaultMutableTreeNode sourceNode,
                                    DefaultMutableTreeNode destNode,
                                    boolean onNode) {

            if ( !(sourceNode.getUserObject() instanceof ComponentGroup)) {
                return true;
            }
            ComponentGroup srcGroup =
                (ComponentGroup) sourceNode.getUserObject();
            return !isAncestor(srcGroup,
                               (ComponentHolder) destNode.getUserObject());
        }

    }

}

