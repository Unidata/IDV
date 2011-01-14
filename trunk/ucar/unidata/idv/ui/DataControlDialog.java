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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;


import ucar.unidata.idv.*;


import ucar.unidata.idv.control.DisplaySetting;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;

import visad.VisADException;


import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.BoxLayout;
import javax.swing.event.*;
import javax.swing.tree.*;




/**
 * This class provides a list of the display controls and the data selection dialog
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.98 $
 */
public class DataControlDialog implements ActionListener {


    /** Reference to the idv */
    private IntegratedDataViewer idv;

    /** The JDialog  window object */
    private JDialog dialog;

    /** The gui contents */
    JComponent contents;

    /** Should we layout the display/control lists horizontally */
    private boolean horizontalOrientation = false;

    /**
     *   We keep a list of the buttons that can be used for creation
     *   so we can enable/disable all them as needed.
     */
    List createBtns = new ArrayList();

    /** A mutex for when we enable/disable the create buttons */
    private Object ENABLE_MUTEX = new Object();

    /** The top label */
    JLabel label;

    /** The JTree that holds the {@link ucar.unidata.idv.ControlDescriptor} hierarchy */
    JTree controlTree;

    /** A mutex for accesing the control tree */
    private Object CONTROLTREE_MUTEX;

    /** The root of the {@link ucar.unidata.idv.ControlDescriptor} hierarchy */
    DefaultMutableTreeNode controlTreeRoot;

    /** THe tree model used for the control descriptors */
    DefaultTreeModel controlTreeModel;

    /** Mapping from ControlDescriptor to tree node */
    private Hashtable cdToNode = new Hashtable();

    /**
     *   The first control descriptor we show.
     *   Keep this around so we can always have one selected
     *   when a new tree is created.
     */
    private ControlDescriptor firstCd;


    /** The wrapper for the display list */
    JComponent displayScroller;

    /** The data selection widget */
    private DataSelectionWidget dataSelectionWidget;

    /** The data source */
    DataSource dataSource;

    /** The data choice */
    DataChoice dataChoice;

    /** This class can be in its own window or be part of an other gui */
    private boolean inOwnWindow = true;


    /**
     * Constructor  for when we are a part of the {@link DataSelector}
     *
     * @param idv Reference to the IDV
     * @param inOwnWindow Should this object be in its own window
     * @param horizontalOrientation Show display/times hor?
     *
     */
    public DataControlDialog(IntegratedDataViewer idv, boolean inOwnWindow,
                             boolean horizontalOrientation) {
        this.inOwnWindow           = inOwnWindow;
        this.horizontalOrientation = horizontalOrientation;
        init(idv, null, 50, 50);
    }


    /**
     * Constructor for configuring a   {@link ucar.unidata.data.DataChoice}
     *
     * @param idv Reference to the IDV
     * @param dataChoice The {@link ucar.unidata.data.DataChoice} we are configuring
     * @param x X position on the screen to show window
     * @param y Y position on the screen to show window
     *
     */
    public DataControlDialog(IntegratedDataViewer idv, DataChoice dataChoice,
                             int x, int y) {
        init(idv, dataChoice, x, y);
    }


    /**
     * Initialize the gui. Popup a new Window if required.
     *
     * @param idv The IDV
     * @param dataChoice The {@link DataChoice} we are configuring (or null)
     * @param windowX X position on the screen to show window
     * @param windowY Y position on the screen to show window
     */
    private void init(IntegratedDataViewer idv, DataChoice dataChoice,
                      int windowX, int windowY) {
        this.idv        = idv;
        this.dataChoice = dataChoice;

        contents        = doMakeDataChoiceDialog(dataChoice);
        if (inOwnWindow) {
            doMakeWindow(GuiUtils.centerBottom(contents,
                    GuiUtils.makeApplyOkCancelButtons(this)), windowX,
                        windowY, "Select Display");
        }
    }


    /**
     * We keep track of the "Create" buttons so we can enable/disable them
     *
     * @param b The create button  to add into the list
     */
    public void addCreateButton(JButton b) {
        createBtns.add(b);
        b.setEnabled(getSelectedControl() != null);
    }

    /**
     * Return the gui contents
     * @return The gui contents
     */
    public JComponent getContents() {
        return contents;
    }

    /**
     * Utility to set the cursor on the gui contents
     *
     * @param cursor The cursor to set
     */
    public void setCursor(Cursor cursor) {
        contents.setCursor(cursor);
    }

    /**
     * Set the data source
     *
     * @param ds The new data source
     */
    public void setDataSource(DataSource ds) {
        dataSource = ds;
    }

    /**
     * Called by the DataSelector to handle when the data source has changed
     *
     * @param dataSource The data source that changed
     */
    public void dataSourceChanged(DataSource dataSource) {
        if (this.dataSource == null) {
            setDataChoice(dataChoice);
            return;
        }
        if (this.dataSource != dataSource) {
            return;
        }
        dataSelectionWidget.dataSourceChanged(dataSource);
    }


    /**
     * Return the {@link ucar.unidata.idv.ControlDescriptor} that is currently selected
     *
     * @return Selected ControlDescriptor
     */
    protected ControlDescriptor getSelectedControl() {
        if (controlTree == null) {
            return null;
        }

        List l = getSelectedControlsFromTree();
        if (l.size() > 0) {
            return (ControlDescriptor) l.get(0);
        } else {
            return null;
        }
    }

    /**
     * Find and return the list of {@link ucar.unidata.idv.ControlDescriptor}-s
     * that have been selected in the JTree
     *
     * @return List of control descriptors
     */
    private List getSelectedControlsFromTree() {

        TreePath[] paths;
        synchronized (CONTROLTREE_MUTEX) {
            paths = controlTree.getSelectionModel().getSelectionPaths();
        }
        List descriptors = new ArrayList();
        if (paths == null) {
            return descriptors;
        }
        for (int i = 0; i < paths.length; i++) {
            Object last = paths[i].getLastPathComponent();
            if (last == null) {
                continue;
            }
            Object object = ((DefaultMutableTreeNode) last).getUserObject();
            if ( !(object instanceof ControlDescriptor)) {
                continue;
            }
            descriptors.add(object);
        }
        return descriptors;
    }



    /**
     * Find and return the array of selected {@link ucar.unidata.idv.ControlDescriptor}-s
     *
     * @return Select control descriptors
     */
    public Object[] getSelectedControls() {
        return getSelectedControlsFromTree().toArray();
    }


    /**
     * Add the given list of {@link ucar.unidata.idv.ControlDescriptor}-s
     * int othe Tree.
     *
     * @param v List of new {@link ucar.unidata.idv.ControlDescriptor}-s
     */
    private void setControlList(Vector v) {
        synchronized (CONTROLTREE_MUTEX) {
            List openCds = new ArrayList();
            Enumeration paths = controlTree.getExpandedDescendants(
                                    new TreePath(controlTreeRoot.getPath()));
            if (paths != null) {
                while (paths.hasMoreElements()) {
                    TreePath path = (TreePath) paths.nextElement();
                    DefaultMutableTreeNode last =
                        (DefaultMutableTreeNode) path.getLastPathComponent();
                    Enumeration nodeChildren = last.children();
                    while (nodeChildren.hasMoreElements()) {
                        DefaultMutableTreeNode child =
                            (DefaultMutableTreeNode) nodeChildren
                                .nextElement();
                        if (child.getUserObject()
                                instanceof ControlDescriptor) {
                            openCds.add(child.getUserObject());
                        }
                    }
                }
            }

            cdToNode = new Hashtable();
            firstCd  = null;
            controlTreeRoot.removeAllChildren();


            List      leafNodes = new ArrayList();

            Hashtable nodes     = new Hashtable();
            for (int i = 0; i < v.size(); i++) {
                ControlDescriptor      cd = (ControlDescriptor) v.get(i);
                String displayCategory            = cd.getDisplayCategory();
                DefaultMutableTreeNode parentNode = controlTreeRoot;
                String                 catSoFar   = "";
                List cats = StringUtil.split(displayCategory, "/", true,
                                             true);
                for (int catIdx = 0; catIdx < cats.size(); catIdx++) {
                    String subCat = (String) cats.get(catIdx);
                    catSoFar = catSoFar + "/" + subCat;
                    DefaultMutableTreeNode node =
                        (DefaultMutableTreeNode) nodes.get(catSoFar);
                    if (node == null) {
                        node = new DefaultMutableTreeNode(subCat);
                        parentNode.add(node);
                        nodes.put(catSoFar, node);
                    }
                    parentNode = node;
                }
                if (firstCd == null) {
                    firstCd = cd;
                }
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(cd);
                leafNodes.add(node);
                cdToNode.put(cd, node);
                parentNode.add(node);
            }

            //Trim out single child category nodes.
            for (int i = 0; i < leafNodes.size(); i++) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) leafNodes.get(i);
                DefaultMutableTreeNode parent =
                    (DefaultMutableTreeNode) node.getParent();
                if (parent == controlTreeRoot) {
                    continue;
                }
                if (parent.getChildCount() == 1) {
                    DefaultMutableTreeNode grandParent =
                        (DefaultMutableTreeNode) parent.getParent();
                    if ((grandParent == controlTreeRoot)
                            || (grandParent.getChildCount() == 1)) {
                        node.removeFromParent();
                        grandParent.add(node);
                        parent.removeFromParent();
                    }
                }
            }


            controlTreeModel.nodeStructureChanged(controlTreeRoot);
            for (int i = 0; i < openCds.size(); i++) {
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) cdToNode.get(openCds.get(i));
                if (node != null) {
                    TreePath path =
                        new TreePath(controlTreeModel.getPathToRoot(node));
                    //Here we do an addSelectionPath as well as the the expand path
                    //because the expandPath does not seem to work.
                    controlTree.addSelectionPath(path);
                    controlTree.expandPath(path);
                }
            }
            //Now, clear the selection, hopefully leaving the expanded paths expanded.
            controlTree.clearSelection();
            checkSettings();
        }
    }


    /**
     * Expand the JTree out to the node that represents the
     * given {@link ucar.unidata.idv.ControlDescriptor}
     *
     * @param cd The selected  {@link ucar.unidata.idv.ControlDescriptor}
     */
    private void setSelectedControl(ControlDescriptor cd) {
        synchronized (CONTROLTREE_MUTEX) {
            if (cd == null) {
                cd = firstCd;
            }
            if (cd == null) {
                return;
            }
            DefaultMutableTreeNode node =
                (DefaultMutableTreeNode) cdToNode.get(cd);
            if (node == null) {
                return;
            }
            TreePath path =
                new TreePath(controlTreeModel.getPathToRoot(node));
            controlTree.addSelectionPath(path);
            controlTree.expandPath(path);
        }
    }


    /**
     * Show help for the DisplayControl represented by the selected control descriptor
     */
    private void showControlHelp() {
        Object selected = getSelectedControl();
        if (selected == null) {
            idv.getIdvUIManager().showHelp("idv.data.fieldselector");
        } else {
            ((ControlDescriptor) selected).showHelp();
        }
    }


    /**
     * Be notified of a change to the display templates
     */
    public void displayTemplatesChanged() {
        setDataChoice(dataChoice);
    }


    /**
     * Set the {@link ucar.unidata.data.DataChoice} we are representing
     *
     * @param dc The current {@link ucar.unidata.data.DataChoice}
     */
    public synchronized void setDataChoice(DataChoice dc) {
        dataChoice = dc;
        if (dataChoice == null) {
            setControlList(new Vector());
            dataSelectionWidget.setTimes(new ArrayList(), new ArrayList());
            dataSelectionWidget.updateSelectionTab(null, null);
            return;
        }
        ControlDescriptor selected = getSelectedControl();
        Vector newList =
            new Vector(
                ControlDescriptor.getApplicableControlDescriptors(
                    dataChoice.getCategories(),
                    idv.getControlDescriptors(true)));
        setControlList(newList);
        if ( !newList.contains(selected)) {
            selected = null;
        }
        setSelectedControl(selected);

        if (label != null) {
            label.setText("Choose a display for data: " + dataChoice);
        }
        List sources = new ArrayList();
        dataChoice.getDataSources(sources);
        sources = Misc.makeUnique(sources);



        List selectedTimes = dataChoice.getSelectedDateTimes();
        //A hack -  data choices that have no times at all have a non-null but empty list of selected times.
        List times = dataChoice.getAllDateTimes();
        if (false && (selectedTimes != null)) {
            List allTimes = dataChoice.getAllDateTimes();
            //Now, convert the (possible) indices to actual datetimes
            selectedTimes = DataSourceImpl.getDateTimes(selectedTimes,
                    allTimes);
            dataSelectionWidget.setTimes(selectedTimes, selectedTimes);
        } else {
            dataSelectionWidget.setTimes(times, selectedTimes);
        }


        if (sources.size() == 1) {
            DataSource dataSource = (DataSource) sources.get(0);
            //If the widget thinks this is a new data source then we need to reset the times list
            if (dataSelectionWidget.updateSelectionTab(dataSource, dc)) {
                dataSelectionWidget.setTimes(times, selectedTimes);
            }
        } else {
            dataSelectionWidget.updateSelectionTab(null, dc);
        }




    }


    /**
     * Return the {@link ucar.unidata.data.DataSource} we are configuring
     *
     * @return The {@link ucar.unidata.data.DataSource}
     */
    public DataSource getDataSource() {
        return dataSource;
    }

    /**
     * Get the {@link ucar.unidata.data.DataChoice} we are representing
     *
     * @return  The current {@link ucar.unidata.data.DataChoice}
     */
    public DataChoice getDataChoice() {
        return dataChoice;
    }




    /**
     * Class ControlTree shows the display controls.
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.98 $
     */
    private class ControlTree extends JTree {

        /** tooltip to use */
        private String defaultToolTip = "Click here and press F1 for help";

        /**
         * ctor
         */
        public ControlTree() {
            setRootVisible(false);
            setShowsRootHandles(true);
            setToggleClickCount(1);
            setToolTipText(defaultToolTip);
            DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
                public Component getTreeCellRendererComponent(JTree theTree,
                        Object value, boolean sel, boolean expanded,
                        boolean leaf, int row, boolean hasFocus) {

                    super.getTreeCellRendererComponent(theTree, value, sel,
                            expanded, leaf, row, hasFocus);
                    if ( !(value instanceof DefaultMutableTreeNode)) {
                        return this;
                    }
                    Object object =
                        ((DefaultMutableTreeNode) value).getUserObject();
                    if ( !(object instanceof ControlDescriptor)) {
                        return this;
                    }
                    ControlDescriptor cd = (ControlDescriptor) object;
                    if (cd.getIcon() != null) {
                        ImageIcon icon = GuiUtils.getImageIcon(cd.getIcon(),
                                             false);
                        // setIcon(icon);
                    }
                    return this;
                }
            };
            setCellRenderer(renderer);

        }


        /**
         * get the tooltip
         *
         * @param event mouse event
         *
         * @return Tooltip
         */
        public String getToolTipText(MouseEvent event) {
            if (getSelectedControl() == null) {
                return defaultToolTip;
            }
            return defaultToolTip + " on this display type";
        }

        /**
         * Find the tooltip
         *
         * @param event mouse event
         *
         * @return tooltip
         */
        private String findToolTipText(MouseEvent event) {
            TreePath path = getPathForLocation(event.getX(), event.getY());
            if (path == null) {
                return defaultToolTip;
            }
            Object last = path.getLastPathComponent();
            if (last == null) {
                return defaultToolTip;
            }
            if ( !(last instanceof DefaultMutableTreeNode)) {
                return defaultToolTip;
            }
            Object object = ((DefaultMutableTreeNode) last).getUserObject();
            if ( !(object instanceof ControlDescriptor)) {
                return defaultToolTip;
            }
            ControlDescriptor cd = (ControlDescriptor) object;
            return cd.getToolTipText();
        }

    }


    /**
     * _more_
     */
    private void controlTreeChanged() {
        Object[] cd = getSelectedControls();
        synchronized (ENABLE_MUTEX) {
            for (int i = 0; i < createBtns.size(); i++) {
                ((JButton) createBtns.get(i)).setEnabled(cd.length > 0);
            }
        }

        if (dataSelectionWidget != null) {
            List levels = null;
            if (cd.length == 1) {
                levels = ((ControlDescriptor) cd[0]).getLevels();
            }
            dataSelectionWidget.setLevelsFromDisplay(levels);
        }

    }


    /**
     * Make the GUI for configuring a {@link ucar.unidata.data.DataChoice}
     *
     * @param dataChoice The DataChoice
     * @return The GUI
     */
    public JComponent doMakeDataChoiceDialog(DataChoice dataChoice) {

        controlTree       = new ControlTree();
        CONTROLTREE_MUTEX = controlTree.getTreeLock();
        controlTreeRoot   = new DefaultMutableTreeNode("Displays");
        controlTreeModel  = new DefaultTreeModel(controlTreeRoot);
        controlTree.setModel(controlTreeModel);
        DefaultTreeCellRenderer renderer =
            (DefaultTreeCellRenderer) controlTree.getCellRenderer();
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        renderer.setLeafIcon(null);
        Font f = renderer.getFont();
        controlTree.setRowHeight(18);
        //        renderer.setFont (f.deriveFont (14.0f));

        controlTree.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == e.VK_F1) {
                    showControlHelp();
                }
            }
        });

        controlTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                checkSettings();
                Misc.run(new Runnable() {
                    public void run() {
                        controlTreeChanged();
                    }
                });
            }
        });
        controlTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doOk();
                }
                //For now let's not publicize this facility
                //                if (true) {return;}
                if (SwingUtilities.isRightMouseButton(e)) {
                    //TBD
                    if (true) {
                        return;
                    }
                    final ControlDescriptor cd = getSelectedControl();
                    if (cd == null) {
                        return;
                    }
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem mi =
                        new JMenuItem(
                            "Create this display whenever the data is loaded");
                    mi.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            idv.getAutoDisplayEditor()
                                .addDisplayForDataSource(getDataChoice(), cd);
                        }
                    });

                    List dataSources = new ArrayList();
                    getDataChoice().getDataSources(dataSources);
                    if (dataSources.size() > 0) {
                        menu.add(mi);
                    }
                    mi = new JMenuItem("Edit automatic display creation");
                    mi.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent ae) {
                            idv.showAutoDisplayEditor();
                        }
                    });
                    menu.add(mi);
                    menu.show(controlTree, e.getX(), e.getY());
                }
            }
        });

        displayScroller = GuiUtils.makeScrollPane(controlTree, 200, 150);
        displayScroller.setBorder(null);

        dataSelectionWidget = new DataSelectionWidget(idv);

        if (horizontalOrientation) {
            displayScroller.setPreferredSize(new Dimension(150, 35));
            contents = GuiUtils.hsplit(dataSelectionWidget.getContents(),
                                       displayScroller);
        } else {
            displayScroller.setPreferredSize(new Dimension(200, 150));
            dataSelectionWidget.getContents().setPreferredSize(
                new Dimension(200, 200));
            contents = GuiUtils.vsplit(displayScroller,
                                       dataSelectionWidget.getContents(),
                                       150, 0.5);
        }

        if (inOwnWindow) {
            contents =
                GuiUtils.topCenter(GuiUtils.inset(label =
                    new JLabel("                 "), 5), contents);
        }
        setDataChoice(dataChoice);
        return contents;

    }


    /**
     * Check settings
     */
    private void checkSettings() {
        Object[] cd = getSelectedControls();
        dataSelectionWidget.updateSettings((ControlDescriptor) ((cd.length
                > 0)
                ? cd[0]
                : null));
    }

    /**
     * Get the data selection widget
     *
     * @return data selection widget
     */
    public DataSelectionWidget getDataSelectionWidget() {
        return dataSelectionWidget;
    }

    /**
     * Make a new window with the given GUI contents and position
     * it at the given screen location
     *
     * @param contents Gui contents
     * @param x Screen x
     * @param y Screen y
     */
    protected void doMakeWindow(JComponent contents, int x, int y) {
        doMakeWindow(contents, x, y, "Select Display");
    }

    /**
     * Make a new window with the given GUI contents with the given
     * window title and position it at the given screen location
     *
     * @param contents Gui contents
     * @param x Screen x
     * @param y Screen y
     * @param title Window title
     */
    private void doMakeWindow(JComponent contents, int x, int y,
                              String title) {

        dialog = GuiUtils.createDialog(title, true);
        Container cpane = dialog.getContentPane();
        cpane.setLayout(new BorderLayout());
        cpane.add("Center", GuiUtils.inset(contents, 5));
        dialog.setLocation(x, y);
        dialog.pack();
        dialog.show();
    }

    /**
     * Dispose of  the dialog window (if it is created)
     * and remove this object from the IDV's list of
     * DataControlDialog-s
     */
    public void dispose() {
        if (dialog != null) {
            dialog.dispose();
        }
        idv.getIdvUIManager().removeDCD(this);
    }

    /**
     *  Calls dispose, nulls out references (for leaks), etc.
     */
    public void doClose() {
        if ( !inOwnWindow) {
            return;
        }
        if (dialog != null) {
            dialog.setVisible(false);
        }
        dispose();
        idv        = null;
        dataSource = null;
        dataChoice = null;
    }

    /**
     * Call doApply and close the window
     */
    public void doOk() {
        doApply();
        if (inOwnWindow) {
            doClose();
        }
    }

    /**
     * Call the IDV's processDialog method. The IDV figures out what to do.
     */
    private void doApply() {
        idv.getIdvUIManager().processDialog(this);
    }

    /**
     * Handle OK, APPLY and CANCEL events
     *
     * @param event The event
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(GuiUtils.CMD_OK)) {
            doOk();
        } else if (event.getActionCommand().equals(GuiUtils.CMD_APPLY)) {
            doApply();
        } else if (event.getActionCommand().equals(GuiUtils.CMD_CANCEL)) {
            doClose();
        }
    }




}
