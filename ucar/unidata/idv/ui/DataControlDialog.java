/*
 * $Id: DataControlDialog.java,v 1.98 2007/08/20 20:54:30 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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
 * This class is a sortof polymorphic dialog/window that manages  selection
 * of times for a datasource, displays/times for a datachoice and (sometime)
 * a window showing a DataTree, list of displays and times.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.98 $
 */
public class DataControlDialog implements ActionListener {

    /**
     * A helper attribute so we can call static routines in LogUtil
     *   without typing the whole class name
     */
    public static final LogUtil LU = null;

    /** Use this member to log messages (through calls to LogUtil) */
    public static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(DataControlDialog.class.getName());


    /** Reference to the idv */
    private IntegratedDataViewer idv;

    /** The JDialog  window object */
    private JDialog dialog;
    //private JFrame dialog;


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

    /** Holds the times list and its select all btn */
    private JComponent[] timesListInfo;


    /** JlIst that shows the times that can be selected */
    JList timesList;

    /** The times list component */
    JComponent timesTab;

    /** subset tab. holds times list, geopspatial, etc. */
    JComponent selectionContainer;

    /** Contains the selection tabbed pane */
    JComponent selectionTabContainer;


    /** _more_ */
    private SettingsTree settingsTree;


    /** The selection tabbed pane */
    JTabbedPane selectionTab;

    /** Holds the stride */
    JPanel strideTab;

    /** Holds the area subset */
    JPanel areaTab;

    /** The chekcbox for selecting "All times" */
    JCheckBox allTimesButton;

    /** List of all the possible dttms */
    List allDateTimes;

    /** The data source */
    DataSource dataSource;

    /** The data choice */
    DataChoice dataChoice;

    /** This class can be in its own window or be part of an other gui */
    private boolean inOwnWindow = true;


    /** geo selection */
    private GeoSelectionPanel geoSelectionPanel;

    /** last level selected */
    private Object lastLevel;

    /** CUrrent list of levels */
    private List levels;

    /** Shows the levels */
    private JList levelsList;

    /** Scrolls the levels list */
    private JScrollPane levelsScroller;


    /** Holds the levels list */
    private JComponent levelsTab;




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
        init(idv, null, null, 50, 50);
    }



    /**
     * Constructor for configuring a  {@link ucar.unidata.data.DataSource}
     *
     * @param idv Reference to the IDV
     * @param dataSource The {@link ucar.unidata.data.DataSource} we are configuring
     *
     */
    public DataControlDialog(IntegratedDataViewer idv,
                             DataSource dataSource) {
        this(idv, dataSource, true);
    }


    /**
     * Constructor for configuring a  {@link ucar.unidata.data.DataSource}
     *
     * @param idv Reference to the IDV
     * @param dataSource The {@link ucar.unidata.data.DataSource} we are configuring
     * @param inOwnWindow Show in own window
     *
     */

    public DataControlDialog(IntegratedDataViewer idv, DataSource dataSource,
                             boolean inOwnWindow) {
        this.inOwnWindow = inOwnWindow;
        init(idv, dataSource, null, 50, 50);
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
        init(idv, null, dataChoice, x, y);
    }

    /**
     * Initialize the gui. Popup a new Window if required.
     *
     * @param idv The IDV
     * @param dataSource The {@link DataSource} we are configuring (or null)
     * @param dataChoice The {@link DataChoice} we are configuring (or null)
     * @param windowX X position on the screen to show window
     * @param windowY Y position on the screen to show window
     */
    private void init(IntegratedDataViewer idv, DataSource dataSource,
                      DataChoice dataChoice, int windowX, int windowY) {
        this.idv        = idv;
        this.dataSource = dataSource;
        this.dataChoice = dataChoice;

        if (dataSource != null) {
            contents = doMakeDataSourceDialog(dataSource);
        } else {
            contents = doMakeDataChoiceDialog(dataChoice);
        }
        if (inOwnWindow) {
            doMakeWindow(GuiUtils.centerBottom(contents,
                    GuiUtils.makeApplyOkCancelButtons(this)), windowX,
                        windowY, ((dataSource != null)
                                  ? "Select Times"
                                  : "Select Display"));
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
        setTimes(dataSource.getAllDateTimes(),
                 dataSource.getDateTimeSelection());
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
     * Any geo selection
     *
     * @return the geoselection or null if none
     */
    public GeoSelection getGeoSelection() {
        if (geoSelectionPanel == null) {
            return null;
        }
        if ( !geoSelectionPanel.getEnabled()) {
            return null;
        }
        return geoSelectionPanel.getGeoSelection();
    }


    /**
     * Get the min/max level range
     *
     * @return min max levels
     */
    protected Object[] getSelectedLevelRange() {
        Object[] NO_LEVELS = new Object[] {};
        if ((levelsTab == null) || (levelsTab.getParent() == null)) {
            return NO_LEVELS;
        }

        int[] selected = levelsList.getSelectedIndices();
        //None selected or the 'All Levels'
        if ((selected.length == 0)
                || ((selected.length == 1) && (selected[0] == 0))) {
            return NO_LEVELS;
        }
        if (selected.length == 1) {
            lastLevel = levels.get(selected[0] - 1);
            //            idv.getStore().put("idv.dataselector.level", lastLevel);
            return new Object[] { lastLevel };
        }
        int first = -1;
        int last  = -1;
        for (int i = 0; i < selected.length; i++) {
            if (selected[i] == 0) {
                continue;
            }
            if ((i == 0) || (selected[i] < first)) {
                first = selected[i];
            }
            if ((i == 0) || (selected[i] > last)) {
                last = selected[i];
            }
        }
        //The 'All Levels'
        if ((first <= 0) || (last <= 0)) {
            return NO_LEVELS;
        }

        return new Object[] { levels.get(first - 1), levels.get(last - 1) };
    }


    /** _more_ */
    private String currentLbl;

    /**
     * Update selection panel for data source
     *
     * @param dataSource  data source
     * @param dc  The data choice
     */
    private void updateSelectionTab(DataSource dataSource, DataChoice dc) {
        //        System.err.println("update tab " + dataSource + " " + dc);
        if (selectionTab == null) {
            return;
        }
        int idx = selectionTab.getSelectedIndex();
        if (idx >= 0) {
            currentLbl = selectionTab.getTitleAt(idx);
        }

        selectionTab.removeAll();

        if (timesList.getModel().getSize() > 0) {
            selectionTab.add(timesTab, "Times", 0);
        }

        if (dataSource == null) {
            if (dc != null) {
                addSettingsComponent();
            }
            checkSelectionTab();
            return;
        }

        levels = dc.getAllLevels();
        if ((levels != null) && (levels.size() > 1)) {
            if (levelsList == null) {
                levelsList = new JList();
                //                lastLevel  = idv.getStore().get("idv.dataselector.level");
                levelsList.setSelectionMode(
                    ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                levelsList.setToolTipText("Shift-Click to select a range");
                levelsScroller = GuiUtils.makeScrollPane(levelsList, 300,
                        100);
                levelsTab = levelsScroller;
            }
            Vector tmp = new Vector();
            tmp.add(new TwoFacedObject("Default", null));
            for (int i = 0; i < levels.size(); i++) {
                Object o = levels.get(i);
                if (o instanceof visad.Real) {
                    visad.Real r = (visad.Real) levels.get(i);
                    tmp.add(Util.labeledReal(r, true));
                } else {
                    tmp.add(o);
                }
            }
            levelsList.setListData(tmp);

            /*  TODO:  figure out a better  way to have level be sticky
            if ((lastLevel != null) && levels.contains(lastLevel)) {
                int idx = levels.indexOf(lastLevel) + 1;  // account for extra
                levelsList.setSelectedIndex(idx);
                levelsList.ensureIndexIsVisible(idx);
            } else {
                levelsList.setSelectedIndex(0);
            }
            */
            levelsList.setSelectedIndex(0);
            selectionTab.add(levelsTab, "Level");
        }


        if (dataSource.canDoGeoSelection()) {
            if (strideTab == null) {
                strideTab = new JPanel(new BorderLayout());
            }
            if (areaTab == null) {
                areaTab = new JPanel(new BorderLayout());
            }
            strideTab.removeAll();
            areaTab.removeAll();
            geoSelectionPanel =
                ((DataSourceImpl) dataSource).doMakeGeoSelectionPanel(false);
            JComponent strideComponent =
                geoSelectionPanel.getStrideComponent();
            JComponent areaComponent = geoSelectionPanel.getAreaComponent();
            if (areaComponent != null) {
                areaTab.add(areaComponent);
                selectionTab.add("Region", areaTab);
            }
            if (strideComponent != null) {
                strideTab.add(strideComponent);
                selectionTab.add("Stride", strideTab);
            }
        }

        addSettingsComponent();
        checkSelectionTab();

        if (currentLbl != null) {
            idx = selectionTab.indexOfTab(currentLbl);
            if (idx >= 0) {
                selectionTab.setSelectedIndex(idx);
            }
        }

    }

    /**
     * _more_
     */
    private void checkSelectionTab() {
        boolean changed = false;
        if ((selectionTab.getTabCount() > 0)
                && (selectionTabContainer.getParent() == null)) {
            selectionContainer.add(selectionTabContainer);
            changed = true;
        } else if ((selectionTab.getTabCount() == 0)
                   && (selectionTabContainer.getParent() != null)) {
            selectionContainer.removeAll();
            changed = true;
        }
        if (changed) {
            selectionContainer.validate();
            selectionContainer.repaint();
        }
    }





    /**
     * _more_
     *
     * @return _more_
     */
    protected List getSelectedSettings() {
        if (settingsTree == null) {
            return null;
        }
        return settingsTree.getSelectedSettings();
    }


    /**
     * _more_
     */
    private void addSettingsComponent() {
        List settings = idv.getResourceManager().getDisplaySettings();
        if ((settings == null) || (settings.size() == 0)) {
            return;
        }
        if (settingsTree == null) {
            settingsTree = new SettingsTree(this, idv);
        }
        selectionTab.add("Settings", settingsTree.getContents());
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
            setTimes(new ArrayList(), new ArrayList());
            updateSelectionTab(null, null);
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

        if (inOwnWindow) {
            label.setText("Choose a display for data: " + dataChoice);
        }
        List sources = new ArrayList();
        dataChoice.getDataSources(sources);
        sources = Misc.makeUnique(sources);

        List selectedTimes = dataChoice.getSelectedDateTimes();
        //A hack -  data choices that have no times at all have a non-null but empty list of selected times.
        if (false && (selectedTimes != null)) {
            List allTimes = dataChoice.getAllDateTimes();
            //Now, convert the (possible) indices to actual datetimes
            selectedTimes = DataSourceImpl.getDateTimes(selectedTimes,
                    allTimes);
            setTimes(selectedTimes, selectedTimes);
        } else {
            setTimes(dataChoice.getAllDateTimes(), selectedTimes);
        }



        if (sources.size() == 1) {
            DataSource dataSource = (DataSource) sources.get(0);
            updateSelectionTab(dataSource, dc);
        } else {
            updateSelectionTab(null, dc);
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
     * Make the GUI for configuring a {@link ucar.unidata.data.DataSource}
     *
     * @param dataSource The DataSource
     * @return The GUI
     */
    public JComponent doMakeDataSourceDialog(DataSource dataSource) {
        JComponent timesList = getTimesList();
        setTimes(dataSource.getAllDateTimes(),
                 dataSource.getDateTimeSelection());


        return timesList;
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
                Object[] cd = getSelectedControls();
                Misc.run(new Runnable() {
                    public void run() {
                        Object[] cd = getSelectedControls();
                        synchronized (ENABLE_MUTEX) {
                            for (int i = 0; i < createBtns.size(); i++) {
                                ((JButton) createBtns.get(i)).setEnabled(
                                    cd.length > 0);
                            }
                        }
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
        if (horizontalOrientation) {
            displayScroller.setPreferredSize(new Dimension(150, 35));
        } else {
            displayScroller.setPreferredSize(new Dimension(200, 150));
        }
        if (inOwnWindow) {
            contents =
                GuiUtils.topCenter(label = new JLabel("                 "),
                                   displayScroller);
        } else {
            contents = displayScroller;
        }
        allDateTimes = ((dataChoice != null)
                        ? dataChoice.getAllDateTimes()
                        : null);
        timesTab     = getTimesList();


        selectionTab = new JTabbedPane();
        selectionTab.setBorder(null);
        selectionTabContainer = new JPanel(new BorderLayout());
        selectionTabContainer.add(selectionTab);
        selectionContainer = new JPanel(new BorderLayout());
        //        selectionContainer.setBackground(Color.white);
        Font font = selectionTab.getFont();
        font = font.deriveFont((float) font.getSize() - 2).deriveFont(
            Font.ITALIC).deriveFont(Font.BOLD);
        selectionTab.setFont(font);
        selectionTab.add("Times", timesTab);
        JComponent selectionPanel = selectionContainer;

        if ((dataChoice == null) || (allDateTimes != null)) {
            JSplitPane splitPane = (horizontalOrientation
                                    ? GuiUtils.hsplit(selectionPanel,
                                        contents)
                                    : GuiUtils.vsplit(contents,
                                        selectionPanel, 150, 0.5));
            contents = splitPane;
        }
        setDataChoice(dataChoice);
        return contents;
    }


    /**
     * Get the list of all dttms
     *
     * @return List of times
     */
    public List getAllDateTimes() {
        return allDateTimes;
    }

    /**
     *  Return a list of Integer indices of the selected times.
     *
     *  @return List of indices.
     */
    public List getSelectedDateTimes() {
        if (allTimesButton == null) {
            return null;
        }
        if (getUseAllTimes()) {
            return null;
        }
        if (timesList == null) {
            return new ArrayList();
        }
        List selected = Misc.toList(timesList.getSelectedValues());
        return Misc.getIndexList(selected, allDateTimes);
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
        idv          = null;
        timesList    = null;
        allDateTimes = null;
        dataSource   = null;
        dataChoice   = null;
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


    /**
     * Did user choose "Use all times"
     *
     * @return Is the allTimes checkbox selected or true if checkbox not created
     */
    public boolean getUseAllTimes() {
        //NEW:
        /*
        if (timesList.getModel().getSize()
                == timesList.getSelectedIndices().length) {
            return true;
        } else {
            return false;
            }*/
        if (allTimesButton == null) {
            return true;
        }
        return allTimesButton.isSelected();
    }


    /**
     * Select the times in the times list
     *
     * @param all All times
     * @param selected The selected times
     */
    private void setTimes(List all, List selected) {
        setTimes(timesList, allTimesButton, all, selected);
        if (all != null) {
            allDateTimes = new ArrayList(all);
        }
        // hack to deal with the selection of the Use All for a datasource
        allTimesButton.setSelected(allTimesButton.isSelected()
                                   || (dataChoice != null));
        //OLD:
        timesList.setEnabled( !allTimesButton.isSelected());
    }




    /**
     * Add the given times in the all/selected list into the
     * given JList. Configure the allTimeButton  appropriately
     *
     *
     * @param timesList The JList to put the times into.
     * @param allTimesButton The checkbox that allows the user to select all or some
     * @param all All the times
     * @param selected The selected times
     */
    public static void setTimes(JList timesList, JCheckBox allTimesButton,
                                List all, List selected) {

        selected = DataSourceImpl.getDateTimes(selected, all);

        if (DataSourceImpl.holdsIndices(selected)) {
            selected = Misc.getValuesFromIndices(selected, all);
        }

        if (all == null) {
            return;
        }
        List sortedAllDateTimes = Misc.sort(new HashSet(all));
        timesList.setListData(new Vector(sortedAllDateTimes));
        //      allTimesButton.setVisible (allDateTimes.size()>0);
        allTimesButton.setEnabled(sortedAllDateTimes.size() > 0);
        boolean allSelected = false;



        if ((selected != null) && (selected.size() > 0)) {
            allSelected = (sortedAllDateTimes.size() == selected.size());
            for (int i = 0; i < selected.size(); i++) {
                int idx = sortedAllDateTimes.indexOf(selected.get(i));
                if (idx >= 0) {
                    timesList.getSelectionModel().addSelectionInterval(idx,
                            idx);
                }
            }
        } else {
            allSelected = true;
            //      timesList.setSelectionInterval (0, sortedAllDateTimes.size()-1);
        }

        if (allSelected) {
            timesList.getSelectionModel().addSelectionInterval(0,
                    timesList.getModel().getSize() - 1);

        }


        //If there are no time selected then turn on the all times checkbox
        if ((selected == null) || (selected.size() == 0)) {
            allTimesButton.setSelected(true);
        }


        //Don't automatically toggle the checkbox
        //OLD
        timesList.setEnabled( !allTimesButton.isSelected());
        allTimesButton.setSelected(allSelected);
    }




    /**
     *  Create the GUI for the times list. (i.e., all times button and the
     *  times JList)
     *
     *  @return The GUI for times
     */
    public JComponent getTimesList() {
        if (dataSource != null) {
            return getTimesList("Use All ");
        } else {
            return getTimesList("Use Default ");
        }
    }




    /**
     * Create the GUI for the times list. (i.e., all times button and the
     * times JList)
     *
     * @param cbxLabel Label for times checkbox
     * @return The GUI for times
     */
    public JComponent getTimesList(String cbxLabel) {
        if (timesListInfo == null) {
            timesListInfo  = makeTimesListAndPanel(cbxLabel);
            timesList      = (JList) timesListInfo[0];
            allTimesButton = (JCheckBox) timesListInfo[1];
        }
        return timesListInfo[2];
    }




    /**
     * Create the JList, an 'all times button', and a JPanel
     * to show times.
     *
     * @return A triple: JList, all times button and the JPanel that wraps this.
     */
    public static JComponent[] makeTimesListAndPanel() {
        return makeTimesListAndPanel("Use All ");
    }


    /**
     * Create the JList, an 'all times button', and a JPanel
     * to show times.
     *
     *
     * @param cbxLabel Label to use for the checkbox. (Use All or Use Default).
     * @return A triple: JList, all times button and the JPanel that wraps this.
     */
    public static JComponent[] makeTimesListAndPanel(String cbxLabel) {
        final JList timesList = new JList();
        timesList.setBorder(null);
        GuiUtils.configureStepSelection(timesList);
        timesList.setToolTipText("Right click to show selection menu");
        timesList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        final JCheckBox allTimesButton = new JCheckBox(cbxLabel, true);
        allTimesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                timesList.setEnabled( !allTimesButton.isSelected());
            }
        });

        //        JComponent top = GuiUtils.leftRight(new JLabel("Times"),
        //                                            allTimesButton);
        JComponent top = GuiUtils.right(allTimesButton);

        //NEW
        //        JComponent top      = GuiUtils.left(new JLabel("Times"));
        JComponent scroller = GuiUtils.makeScrollPane(timesList, 300, 100);
        //      scroller.setBorder(BorderFactory.createMatteBorder(1,2,0,0,Color.gray));
        return new JComponent[] { timesList, allTimesButton,
                                  GuiUtils.topCenter(top, scroller) };
    }



}

