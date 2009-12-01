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

package ucar.unidata.idv.control.multi;


import ucar.unidata.idv.ControlContext;



import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.*;


import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;






/**
 * Holds a group of display components
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class DisplayGroup extends DisplayComponent {

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
    private JRadioButton gridbagLayoutBtn;


    /** properties widget */
    private JRadioButton rowBtn;


    /** properties widget */
    private JList displayList;

    /** for properties dialog */
    private boolean displayOrderChanged;


    /**
     * default ctor
     */
    public DisplayGroup() {}


    /**
     * ctor
     *
     * @param name name
     */
    public DisplayGroup(String name) {
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
     * do final initialization
     */
    public void initDone() {
        for (int i = 0; i < displayComponents.size(); i++) {
            ((DisplayComponent) displayComponents.get(i)).initDone();
        }
    }

    /**
     * Called by the {@link ucar.unidata.idv.IntegratedDataViewer} to
     * initialize after this control has been unpersisted
     *
     *
     * @param displayControl The control I am part of
     * @param vc The context in which this control exists
     * @param properties Properties that may hold things
     */
    public void initAfterUnPersistence(MultiDisplayHolder displayControl,
                                       ControlContext vc,
                                       Hashtable properties) {
        super.initAfterUnPersistence(displayControl, vc, properties);
        for (int i = 0; i < displayComponents.size(); i++) {
            ((DisplayComponent) displayComponents.get(
                i)).initAfterUnPersistence(displayControl, vc, properties);
        }
    }





    /**
     * Create and return the gui contents
     *
     * @return gui contents
     */
    public JComponent doMakeContents() {
        tabbedPane = new JTabbedPane();
        container  = new JPanel(new GridLayout(numRows, numColumns, 5, 5));
        JLabel label = getDisplayLabel();
        label.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ( !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                showPopup(getDisplayLabel(), e.getX(), e.getY());
            }
        });

        outerContainer = GuiUtils.topCenter(label, container);
        if (displayComponents.size() == 0) {
            //      outerContainer.setPreferredSize(new Dimension(100,50));
        }

        redoLayout();
        setName(getName());
        return outerContainer;
    }


    /**
     * Find the top most ancestor
     *
     * @return The top most ancestor
     */
    public DisplayGroup getAncestorGroup() {
        if (getDisplayGroup() == null) {
            return this;
        }
        return getDisplayGroup().getAncestorGroup();
    }



    /**
     * Recursively find all contained components of the given class
     *
     * @param compClass The class to look for
     *
     * @return List of components
     */
    public List findDisplayComponents(Class compClass) {
        List comps = new ArrayList();
        for (int i = 0; i < displayComponents.size(); i++) {
            DisplayComponent displayComponent =
                (DisplayComponent) displayComponents.get(i);
            if (compClass.isAssignableFrom(displayComponent.getClass())) {
                comps.add(displayComponent);
            }
            if (displayComponent instanceof DisplayGroup) {
                comps.addAll(
                    ((DisplayGroup) displayComponent).findDisplayComponents(
                        compClass));
            }
        }
        return comps;
    }



    /**
     * Layout components
     */
    private void redoLayout() {
        tabbedPane.removeAll();
        container.removeAll();
        if (layout == LAYOUT_GRID) {
            container.setLayout(new GridLayout(numRows, numColumns, 5, 5));
        } else {
            container.setLayout(new BorderLayout());
        }

        List comps = new ArrayList();
        for (int i = 0; i < displayComponents.size(); i++) {
            DisplayComponent displayComponent =
                (DisplayComponent) displayComponents.get(i);
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
        } else {
            container.add(BorderLayout.CENTER,
                          GuiUtils.doLayout(comps, gridColumns,
                                            GuiUtils.WT_Y, GuiUtils.WT_Y));
        }
        container.validate();
    }


    /**
     * Add the wrapper
     *
     * @param displayComponent new one
     */
    public void addDisplayComponent(DisplayComponent displayComponent) {
        addDisplayComponent(displayComponent, -1);
    }



    /**
     * What is the index of the child component
     *
     * @param displayComponent child component
     *
     * @return its index
     */
    public int indexOf(DisplayComponent displayComponent) {
        return displayComponents.indexOf(displayComponent);
    }


    /**
     * Add the wrapper
     *
     * @param displayComponent new one
     * @param index Where
     */
    public void addDisplayComponent(DisplayComponent displayComponent,
                                    int index) {
        displayComponent.setDisplayControl(getDisplayControl());
        if (index >= 0) {
            displayComponents.add(index, displayComponent);
        } else {
            displayComponents.add(displayComponent);
        }
        displayComponent.setDisplayGroup(this);
        redoLayout();
    }



    /**
     * Set the display control I'm in. Also set the DC on all of
     * my children components.
     *
     * @param displayControl The display control
     */
    public void setDisplayControl(MultiDisplayHolder displayControl) {
        super.setDisplayControl(displayControl);
        for (int i = 0; i < displayComponents.size(); i++) {
            ((DisplayComponent) displayComponents.get(i)).setDisplayControl(
                displayControl);
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
            if (displayComponents.size() != propertiesList.size()) {
                for (int i = 0; i < displayComponents.size(); i++) {
                    DisplayComponent displayComponent =
                        (DisplayComponent) displayComponents.get(i);
                    //TODO: Do we need to remove this 
                }
            }

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
            getDisplayControl().newName(DisplayGroup.this, oldName);
        }
        return result;

    }


    /**
     * Show dialog
     *
     * @param comps  List of components
     * @param tabIdx which tab
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {

        super.getPropertiesComponents(comps, tabIdx);
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
                    DisplayComponent comp =
                        (DisplayComponent) displayList.getSelectedValue();
                    if (comp != null) {
                        comp.showProperties();
                    }
                }
            }
        });

        displayList.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
		if (GuiUtils.isDeleteEvent(e)
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
        gridbagLayoutBtn = new JRadioButton("Columns ",
                                            layout == LAYOUT_GRIDBAG);
        ButtonGroup group = GuiUtils.buttonGroup(tabLayoutBtn, gridLayoutBtn);
        group.add(gridbagLayoutBtn);
        group.add(hsplitLayoutBtn);
        group.add(vsplitLayoutBtn);
        dimFld = new JTextField((fixedRows
                                 ? getNumRows()
                                 : getNumColumns()) + "", 5);

        colFld = new JTextField(getGridColumns() + "", 5);



        comps.add(GuiUtils.rLabel("Layout: "));
        comps.add(GuiUtils.left(GuiUtils.hbox(tabLayoutBtn, hsplitLayoutBtn,
                vsplitLayoutBtn)));

        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(GuiUtils.hbox(gridLayoutBtn,
                new JLabel("  Dimension: "), dimFld,
                GuiUtils.hbox(colBtn, rowBtn))));

        comps.add(GuiUtils.filler());
        comps.add(GuiUtils.left(GuiUtils.hbox(gridbagLayoutBtn,
                new JLabel("  # Columns: "), colFld)));

        comps.add(GuiUtils.top(GuiUtils.rLabel("Displays: ")));
        comps.add(displayPanel);
    }




    /**
     * Set animation time on components
     *
     * @param time time
     */
    public void animationTimeChanged(visad.Real time) {
        super.animationTimeChanged(time);
        for (int i = 0; i < displayComponents.size(); i++) {
            ((DisplayComponent) displayComponents.get(
                i)).animationTimeChanged(time);
        }
    }




    /**
     * remove the wrapper
     *
     * @param displayComponent the wrapper to remove
     */
    public void removeDisplayComponent(DisplayComponent displayComponent) {
        displayComponents.remove(displayComponent);
        displayComponent.setDisplayGroup(null);
        getContents();
        redoLayout();
        container.validate();
        getContents().repaint();
    }


    /**
     * do cleanup
     */
    public void doRemove() {
        super.doRemove();
        for (int i = 0; i < displayComponents.size(); i++) {
            ((DisplayComponent) displayComponents.get(i)).doRemove();
        }

    }


    /**
     * Tell components to load
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() throws RemoteException, VisADException {
        super.loadData();
        for (int i = 0; i < displayComponents.size(); i++) {
            ((DisplayComponent) displayComponents.get(i)).loadData();
        }
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

        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Save Image...", this,
                                        "doSaveImage"));
        items.add(GuiUtils.makeMenuItem("Save Movie...", this,
                                        "doSaveMovie"));


        //        items.add(GuiUtils.MENU_SEPARATOR);
        //        items.add(GuiUtils.makeMenuItem("Properties...", this,
        //                                        "showProperties"));
        items.add(GuiUtils.MENU_SEPARATOR);
        JMenu newMenu = new JMenu("New");
        items.add(newMenu);
        getDisplayControl().addGroupNewMenu(this, newMenu);
        return items;
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
        return "Group: " + getName();
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



}

