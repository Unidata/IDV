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
import ucar.unidata.data.DataSelection;
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
public class DataSelectionWidget {

    /** Reference to the idv */
    private IntegratedDataViewer idv;

    /** The gui contents */
    JComponent contents;

    /** Holds the times list and its select all btn */
    private JComponent[] timesListInfo;

    /** JList that shows the times that can be selected */
    JList timesList;

    /** The times list component */
    JComponent timesComponent;

    /** subset tab. holds times list, geopspatial, etc. */
    JComponent selectionContainer;

    /** Contains the selection tabbed pane */
    JComponent selectionTabContainer;


    /** include the settings tab */
    private boolean doSettings = true;

    /** The display settings tree */
    private SettingsTree settingsTree;


    /** The selection tabbed pane */
    private JTabbedPane selectionTab;

    /** Holds the stride */
    private JPanel strideTab;

    private JCheckBox strideCbx;

    private JCheckBox areaCbx;


    private   JComponent strideComponent;

    private JComponent areaComponent;



    /** Holds the area subset */
    private JPanel areaTab;

    /** The chekcbox for selecting "All times" */
    private JCheckBox allTimesButton;

    /** List of all the possible dttms */
    private List allDateTimes;

    /** geo selection */
    private GeoSelectionPanel geoSelectionPanel;

    /** last level selected */
    private Object lastLevel;

    /** Current list of levels */
    private List levels;

    /** Shows the levels */
    private JList levelsList;

    /** Scrolls the levels list */
    private JScrollPane levelsScroller;


    /** Holds the levels list */
    private JComponent levelsTab;

    /** Last data source we were displaying for */
    private DataSource lastDataSource;

    /** Keeps track of the tab label so we can reselect that tab when we update */
    private String currentLbl;

    /**
     * Constructor  for when we are a part of the {@link DataSelector}
     *
     * @param idv Reference to the IDV
     *
     */
    public DataSelectionWidget(IntegratedDataViewer idv) {
        this(idv, true);
    }

    /**
     * Constructor  for when we are a part of the {@link DataSelector}
     *
     * @param idv Reference to the IDV
     * @param doSettings include the display settings in the tab
     */
    public DataSelectionWidget(IntegratedDataViewer idv, boolean doSettings) {
        this.doSettings = doSettings;
        this.idv        = idv;
        getContents();
    }

    /**
     * get the gui contents
     *
     * @return gui contents
     */
    public JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }


    /**
     * Called by the DataSelector to handle when the data source has changed
     *
     * @param dataSource The data source that changed
     */
    public void dataSourceChanged(DataSource dataSource) {
        if (dataSource == null) {
            setTimes(new ArrayList(), new ArrayList());
        } else {
            setTimes(dataSource.getAllDateTimes(),
                     dataSource.getDateTimeSelection());
        }
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




    /**
     * Update the display settings
     *
     * @param cd new control descriptor
     */
    protected void updateSettings(ControlDescriptor cd) {
        if (settingsTree == null) {
            settingsTree = new SettingsTree(idv);
        }
        if (settingsTree != null) {
            settingsTree.updateSettings(cd);
        }
    }


    /**
     * Update the tabbed pane
     *
     * @param dataChoice new data choice
     */
    protected void updateSelectionTab(DataChoice dataChoice) {
        if (dataChoice == null) {
            updateSelectionTab(null, dataChoice);
            return;
        }

        List sources = new ArrayList();
        dataChoice.getDataSources(sources);
        sources = Misc.makeUnique(sources);
        if (sources.size() == 1) {
            updateSelectionTab((DataSource) sources.get(0), dataChoice);
        } else {
            updateSelectionTab(null, dataChoice);
        }
    }




    /**
     * Update selection panel for data source
     *
     * @param dataSource  data source
     * @param dc  The data choice
     */
    protected void updateSelectionTab(DataSource dataSource, DataChoice dc) {

        //        System.err.println("update tab " + dataSource + " " + dc);
        if (lastDataSource != dataSource) {
            dataSourceChanged(dataSource);
        }
        lastDataSource = dataSource;

        if (selectionTab == null) {
            return;
        }
        int idx = selectionTab.getSelectedIndex();
        if (idx >= 0) {
            currentLbl = selectionTab.getTitleAt(idx);
        }

        selectionTab.removeAll();

        if (timesList.getModel().getSize() > 0) {
            selectionTab.add(timesComponent, "Times", 0);
        }

        if (dataSource == null) {
            if (dc != null) {
                addSettingsComponent();
            }
            checkSelectionTab();
            return;
        }

        if (dc == null) {
            checkSelectionTab();
            return;
        }


        levels = dc.getAllLevels(new DataSelection(GeoSelection.STRIDE_BASE));
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
            tmp.add(new TwoFacedObject("All Levels", null));
            for (int i = 0; i < levels.size(); i++) {
                Object o = levels.get(i);
                if (o instanceof visad.Real) {
                    visad.Real r = (visad.Real) levels.get(i);
                    tmp.add(Util.labeledReal(r, true));
                } else {
                    tmp.add(o);
                }
            }


            Object[] selectedLevels = levelsList.getSelectedValues();
            levelsList.setListData(tmp);
            ListSelectionModel lsm    = levelsList.getSelectionModel();
            boolean            didone = false;
            for (int i = 0; i < selectedLevels.length; i++) {
                int index = tmp.indexOf(selectedLevels[i]);
                if (index >= 0) {
                    lsm.addSelectionInterval(index, index);
                    didone = true;
                }
            }
            if ( !didone) {
                if(tmp.size()>1)
                    levelsList.setSelectedIndex(1);
                else
                    levelsList.setSelectedIndex(0);
            }

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
            GeoSelectionPanel oldPanel = geoSelectionPanel;
            geoSelectionPanel =
                ((DataSourceImpl) dataSource).doMakeGeoSelectionPanel(false);
            if(areaCbx == null) {
                areaCbx = new JCheckBox("Use Default",true);
                areaCbx.addActionListener(new ActionListener(){
                        public void actionPerformed(ActionEvent ae) {
                            if(areaComponent!=null) {
                                GuiUtils.enableTree(areaComponent, !areaCbx.isSelected());
                            }
                        }
                    });
            }

            if(strideCbx==null) {
                strideCbx = new JCheckBox("Use Default",true);
                strideCbx.addActionListener(new ActionListener(){
                        public void actionPerformed(ActionEvent ae) {
                            if(strideComponent!=null) {
                                GuiUtils.enableTree(strideComponent, !strideCbx.isSelected());
                            }
                        }
                    });
            }
            
            strideComponent =
                geoSelectionPanel.getStrideComponent();
            GuiUtils.enableTree(strideComponent, !strideCbx.isSelected());
            areaComponent = geoSelectionPanel.getAreaComponent();
            areaComponent.setPreferredSize(new Dimension(200, 150));
            GuiUtils.enableTree(areaComponent, !areaCbx.isSelected());

            if (oldPanel != null) {
                geoSelectionPanel.initWith(oldPanel);
            }

            if (areaComponent != null) {
                areaTab.add(GuiUtils.topCenter(GuiUtils.inset(GuiUtils.right(areaCbx),new Insets(0,0,5,0)),areaComponent));
                selectionTab.add("Region", areaTab);
            }
            if (strideComponent != null) {
                strideTab.add(GuiUtils.top(GuiUtils.topCenter(GuiUtils.inset(GuiUtils.right(strideCbx),new Insets(0,0,5,0)),strideComponent)));
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
     * Create the data selection from everything selected by the user
     *
     * @param addLevels include the levels
     *
     * @return new data selection
     */
    public DataSelection createDataSelection(boolean addLevels) {
        DataSelection dataSelection = null;
        if (getUseAllTimes()) {
            dataSelection = new DataSelection();

            /**
             *     !!!!! TODO !!!!!
             *     I commented this out to work on the "@time index" functionality in the formulas.
             *     What this says is that even though this data selection has no times list still
             *     use all of the times available in the end data source.
             *     However, with this in place what happens is the time selection of the child data choice
             *     that we create for a formula that has the @times is overwritten by this setTimesMode.
             *     One possible solution would be the DataSelection.merge in the DataChoice.getData
             *     could only merge times from the higher priority one when there really is a times list.
             *     !!!!! TODO !!!!!
             */
            //                dataSelection.setTimesMode (dataSelection.TIMESMODE_USETHIS);
        } else {
            dataSelection = new DataSelection(getSelectedDateTimes());
        }


        GeoSelection geoSelection = getGeoSelection();
        if(strideCbx.isSelected()) {
            geoSelection.clearStride();
        }

        if(areaCbx.isSelected()) {
            geoSelection.setBoundingBox(null);
        }

        dataSelection.setGeoSelection(geoSelection);
        Object[] levelRange = getSelectedLevelRange();
        if ((levelRange != null) && (levelRange.length > 0)) {
            if (addLevels || (levelRange.length == 2)) {
                if (levelRange.length == 1) {
                    dataSelection.setLevel(levelRange[0]);
                } else {
                    dataSelection.setLevelRange(levelRange[0], levelRange[1]);
                }
            }
        }
        return dataSelection;
    }



    /**
     * add/remove the tabbed pane from the gui
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
     * Get list of selected DisplaySettings
     *
     * @return list of selected DisplaySettings
     */
    protected List getSelectedSettings() {
        if (settingsTree == null) {
            return null;
        }
        return settingsTree.getSelectedSettings();
    }


    /**
     * Put the display settings  component into the tabbed pane
     */
    private void addSettingsComponent() {
        if ( !doSettings) {
            return;
        }

        List settings = idv.getResourceManager().getDisplaySettings();
        if ((settings == null) || (settings.size() == 0)) {
            return;
        }
        if (settingsTree == null) {
            settingsTree = new SettingsTree(idv);
        }
        selectionTab.add("Settings", settingsTree.getContents());
    }




    /**
     * Make the GUI for configuring a {@link ucar.unidata.data.DataChoice}
     *
     * @return The GUI
     */
    private JComponent doMakeContents() {
        timesComponent = getTimesList();
        selectionTab   = new JTabbedPane();
        selectionTab.setBorder(null);
        selectionTabContainer = new JPanel(new BorderLayout());
        selectionTabContainer.add(selectionTab);
        selectionContainer = new JPanel(new BorderLayout());
        selectionContainer.setPreferredSize(new Dimension(200, 150));
        Font font = selectionTab.getFont();
        font = font.deriveFont((float) font.getSize() - 2).deriveFont(
            Font.ITALIC).deriveFont(Font.BOLD);
        selectionTab.setFont(font);
        selectionTab.add("Times", timesComponent);
        return selectionContainer;
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
    public void setTimes(List all, List selected) {
        setTimes(timesList, allTimesButton, all, selected);
        if (all != null) {
            allDateTimes = new ArrayList(all);
        }
        // hack to deal with the selection of the Use All for a datasource
        allTimesButton.setSelected(allTimesButton.isSelected());

        //OLD:
        timesList.setEnabled( !allTimesButton.isSelected());
    }





    /**
     *  Create the GUI for the times list. (i.e., all times button and the
     *  times JList)
     *
     *  @return The GUI for times
     */
    public JComponent getTimesList() {
        if ( /*TODO dataSource != null*/false) {
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
     * Add the given times in the all/selected list into the
     * given JList. Configure the allTimeButton  appropriately
     *
     *
     * @param timesList The JList to put the times into.
     * @param allTimesButton The checkbox that allows the user to select all or some
     * @param all All the times
     * @param selected The selected times
     */
    private static void setTimes(JList timesList, JCheckBox allTimesButton,
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
     * Create the JList, an 'all times button', and a JPanel
     * to show times.
     *
     *
     * @param cbxLabel Label to use for the checkbox. (Use All or Use Default).
     * @return A triple: JList, all times button and the JPanel that wraps this.
     */
    private static JComponent[] makeTimesListAndPanel(String cbxLabel) {
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

