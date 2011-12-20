/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.data.GeoSelectionPanel;
import ucar.unidata.idv.ControlDescriptor;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.IntegratedDataViewer;
import ucar.unidata.idv.chooser.TimesChooser;
import ucar.unidata.ui.Timeline;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;




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

    /** JList that shows the members that can be selected */
    JList membersList;

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

    /** Stride Checkbox */
    private JCheckBox strideCbx;

    /** Area Checkbox */
    private JCheckBox areaCbx;

    /** Stride Component */
    private JComponent strideComponent;

    /** Area Component */
    private JComponent areaComponent;

    /** Holds the area subset */
    private JPanel areaTab;

    /** The chekcbox for selecting "All times" */
    private JCheckBox allTimesButton;

    /** time options label box */
    private JComboBox timeOptionLabelBox;

    /** List of all the possible dttms */
    private List allDateTimes;

    /** geo selection */
    private GeoSelectionPanel geoSelectionPanel;

    /** last level selected */
    private Object lastLevel;

    /** Current list of levels */
    private List levels;

    /** Current list of ensemble members */
    private List<TwoFacedObject> members;

    /** levels from display */
    private List levelsFromDisplay;

    /** default level to first */
    private boolean defaultLevelToFirst = true;

    /** default member to all */
    private boolean defaultMemberToAll = true;

    /** Shows the levels */
    private JList levelsList;

    /** Scrolls the levels list */
    private JScrollPane levelsScroller;

    /** Scrolls the ensemble members list */
    private JScrollPane membersScroller;

    /** Holds the levels list */
    private JComponent levelsTab;

    /** Holds the ensemble members list */
    private JComponent membersTab;

    /** Last data source we were displaying for */
    private DataSource lastDataSource;

    /** last choice requires volume */
    private boolean lastChoiceRequiresVolume = false;

    /** last DataChoice */
    private DataChoice lastDataChoice;

    /** Keeps track of the tab label so we can reselect that tab when we update */
    private String currentLbl;


    /** list of data selection components */
    private List<DataSelectionComponent> dataSelectionComponents;

    /** flag for using the display */
    private boolean doUseDisplay = true;

    /** flag for chooser using time matching */
    private boolean chooserDoTimeMatching = false;

    /** use default times identifier */
    public final static String USE_DEFAULTTIMES = "Use Default";

    /** use selected times identifier */
    public final static String USE_SELECTEDTIMES = "Use Selected";

    /** use time driver times */
    public final static String USE_DRIVERTIMES = "Use Time Driver";

    /** options for time selection type */
    private final static String[] timeSubsetOptionLabels =
        new String[] { USE_DEFAULTTIMES,
                       USE_SELECTEDTIMES, USE_DRIVERTIMES };

    /** timeline */
    private Timeline timeline;

    /** the time selection type */
    private String timeOption = USE_DEFAULTTIMES;



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
     * Constructor  for when we are a part of the {@link DataSelector}
     *
     * @param idv Reference to the IDV
     * @param doSettings include the display settings in the tab
     * @param doUseDisplay true to use display times
     */
    public DataSelectionWidget(IntegratedDataViewer idv, boolean doSettings,
                               boolean doUseDisplay) {
        this.doSettings   = doSettings;
        this.idv          = idv;
        this.doUseDisplay = doUseDisplay;
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
                || ((selected.length == 1) && (selected[0] == 0)
                    && (levelsFromDisplay == null))) {
            return NO_LEVELS;
        }
        int indexOffset = ((levelsFromDisplay == null)
                           ? 1
                           : 0);
        if (selected.length == 1) {
            lastLevel = levels.get(selected[0] - indexOffset);
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

        return new Object[] { levels.get(first - indexOffset),
                              levels.get(last - indexOffset) };
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
     *
     * @return true if successful
     */
    protected boolean updateSelectionTab(DataSource dataSource,
                                         DataChoice dc) {

        //        System.err.println("update tab " + dataSource + " " + dc);
        lastDataChoice = dc;
        boolean newDataSource = false;
        if (lastDataSource != dataSource) {
            dataSourceChanged(dataSource);
            newDataSource = true;
        }
        lastDataSource = dataSource;
        if (dc != null) {
            lastDataChoice.setProperty(DataSelection.PROP_USESTIMEDRIVER,
                                       false);
            if (dataSource != null) {
                Object cu = dataSource.getProperty(
                                DataSelection.PROP_CHOOSERTIMEMATCHING);
                if (cu != null) {
                    chooserDoTimeMatching = ((Boolean) cu).booleanValue();
                }
            }
        }
        if (selectionTab == null) {
            return newDataSource;
        }
        int idx = selectionTab.getSelectedIndex();
        if (idx >= 0) {
            currentLbl = selectionTab.getTitleAt(idx);
        }

        selectionTab.removeAll();

        if ((dc != null) && (dataSource != null)) {
            dataSelectionComponents =
                dataSource.getDataSelectionComponents(dc);
        } else {
            dataSelectionComponents = null;
        }


        if (timesList.getModel().getSize() > 0) {
            selectionTab.add(timesComponent, "Times", 0);
        }

        if (dataSource == null) {
            if (dc != null) {
                addSettingsComponent();
            }
            checkSelectionTab();
            return newDataSource;
        }

        if (dc == null) {
            checkSelectionTab();
            return newDataSource;
        }


        members = (List) dc.getProperty("prop.gridmembers");
        if ((members != null) && (members.size() > 1)) {
            if (membersList == null) {
                membersList = new JList();
                //                lastLevel  = idv.getStore().get("idv.dataselector.level");
                membersList.setSelectionMode(
                    ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                membersScroller = GuiUtils.makeScrollPane(membersList, 300,
                        100);
                membersTab = membersScroller;
            }
            Vector membersForGui = new Vector();
            if (levelsFromDisplay == null) {
                membersForGui.add(new TwoFacedObject("All Members", null));
            }
            for (int i = 0; i < members.size(); i++) {
                Object o = members.get(i);
                membersForGui.add(o);
            }

            Object[] selectedMembers = membersList.getSelectedValues();
            if ((selectedMembers == null) || (selectedMembers.length == 0)) {
                List dcMembers = (List) dc.getProperty("prop.gridmembers");
                if ((dcMembers != null) && !dcMembers.isEmpty()) {
                    selectedMembers = dcMembers.toArray();
                }
            }



            membersList.setListData(membersForGui);

            if (membersForGui.size() > 1) {
                if (defaultMemberToAll) {
                    membersList.setSelectedIndex(0);
                } else {
                    membersList.setSelectedIndex(1);
                }
            } else {
                membersList.setSelectedIndex(0);
            }

            selectionTab.add(membersTab, "Ensemble", 0);
        }

        if (dc instanceof DerivedDataChoice) {
            DerivedDataChoice ddc = (DerivedDataChoice) dc;
            List              ll  = ddc.getChoices();
            DataChoice        cdc = (DataChoice) ll.get(0);
            if (cdc instanceof DerivedDataChoice) {
                DerivedDataChoice ddc0 = (DerivedDataChoice) cdc;
                List              ll0  = ddc0.getChoices();
                cdc = (DataChoice) ll0.get(0);
            }
            members = (List) cdc.getProperty("prop.gridmembers");
            if ((members != null) && (members.size() > 1)) {
                if (membersList == null) {
                    membersList = new JList();
                    //                lastLevel  = idv.getStore().get("idv.dataselector.level");
                    membersList.setSelectionMode(
                        ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    membersScroller = GuiUtils.makeScrollPane(membersList,
                            300, 100);
                    membersTab = membersScroller;
                }
                Vector membersForGui = new Vector();
                if (levelsFromDisplay == null) {
                    membersForGui.add(new TwoFacedObject("All Members",
                            null));
                }
                for (int i = 0; i < members.size(); i++) {
                    Object o = members.get(i);
                    membersForGui.add(o);
                }

                Object[] selectedMembers = membersList.getSelectedValues();
                if ((selectedMembers == null)
                        || (selectedMembers.length == 0)) {
                    List dcMembers =
                        (List) dc.getProperty("prop.gridmembers");
                    if ((dcMembers != null) && !dcMembers.isEmpty()) {
                        selectedMembers = dcMembers.toArray();
                    }
                }



                membersList.setListData(membersForGui);

                if (membersForGui.size() > 1) {
                    if (defaultMemberToAll) {
                        membersList.setSelectedIndex(0);
                    } else {
                        membersList.setSelectedIndex(1);
                    }
                } else {
                    membersList.setSelectedIndex(0);
                }

                selectionTab.add(membersTab, "Ensemble", 0);
            }
        }
        levels = ((levelsFromDisplay != null)
                  ? levelsFromDisplay
                  : dc.getAllLevels(
                      new DataSelection(GeoSelection.STRIDE_BASE)));
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
            Vector levelsForGui = new Vector();
            if (levelsFromDisplay == null) {
                levelsForGui.add(new TwoFacedObject("All Levels", null));
            }
            for (int i = 0; i < levels.size(); i++) {
                Object o = levels.get(i);
                if (o instanceof visad.Real) {
                    visad.Real r = (visad.Real) levels.get(i);
                    levelsForGui.add(Util.labeledReal(r, true));
                } else {
                    levelsForGui.add(o);
                }
            }

            Object[] selectedLevels = levelsList.getSelectedValues();
            if ((selectedLevels == null) || (selectedLevels.length == 0)) {
                List dcLevels =
                    (List) dc.getProperty(DataSelection.PROP_DEFAULT_LEVELS);
                if ((dcLevels != null) && !dcLevels.isEmpty()) {
                    selectedLevels = dcLevels.toArray();
                }
            }
            boolean    thisChoiceRequiresVolume = false;
            DataChoice theDataChoice            = dc;
            if (theDataChoice == null) {
                theDataChoice = lastDataChoice;
            }
            if (theDataChoice != null) {
                thisChoiceRequiresVolume =
                    Misc.equals(theDataChoice.getProperty("requiresvolume"),
                                "true");
            }

            levelsList.setListData(levelsForGui);
            ListSelectionModel lsm = levelsList.getSelectionModel();
            int                previouslySelectedLevels = 0;
            for (int i = 0; i < selectedLevels.length; i++) {
                int index = levelsForGui.indexOf(selectedLevels[i]);
                if (index >= 0) {
                    lsm.addSelectionInterval(index, index);
                    previouslySelectedLevels++;
                }
            }

            if (thisChoiceRequiresVolume) {
                if ((thisChoiceRequiresVolume != lastChoiceRequiresVolume)
                        && (previouslySelectedLevels == 1)) {
                    previouslySelectedLevels = 0;
                }
            }

            if (previouslySelectedLevels == 0) {
                if (levelsForGui.size() > 1) {
                    if (defaultLevelToFirst && !thisChoiceRequiresVolume) {
                        levelsList.setSelectedIndex(1);
                    } else {
                        levelsList.setSelectedIndex(0);
                    }
                } else {
                    levelsList.setSelectedIndex(0);
                }
            }

            selectionTab.add(levelsTab, "Level");
            lastChoiceRequiresVolume = thisChoiceRequiresVolume;
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
            strideComponent = geoSelectionPanel.getStrideComponent();
            areaComponent   = geoSelectionPanel.getAreaComponent();
            if (areaComponent != null) {
                areaComponent.setPreferredSize(new Dimension(200, 150));
                GuiUtils.enableTree(areaComponent, !areaCbx.isSelected());
            }
            if (oldPanel != null) {
                geoSelectionPanel.initWith(oldPanel);
            }

            if (areaComponent != null) {
                areaComponent.setPreferredSize(new Dimension(200, 150));
                GuiUtils.enableTree(areaComponent, !areaCbx.isSelected());
                areaTab.add(
                    GuiUtils.topCenter(
                        GuiUtils.inset(GuiUtils.right(areaCbx), 0),
                        areaComponent));
                selectionTab.add("Region", areaTab);
            }
            if (strideComponent != null) {
                GuiUtils.enableTree(strideComponent, !strideCbx.isSelected());
                strideTab.add(
                    GuiUtils.top(
                        GuiUtils.topCenter(
                            GuiUtils.inset(
                                GuiUtils.right(strideCbx),
                                new Insets(0, 0, 5, 0)), strideComponent)));
                strideTab.add(
                    GuiUtils.top(
                        GuiUtils.topCenter(
                            GuiUtils.inset(
                                GuiUtils.right(strideCbx),
                                new Insets(0, 0, 5, 0)), strideComponent)));
                selectionTab.add("Stride", strideTab);
            }
        }

        if (dataSelectionComponents != null) {
            for (DataSelectionComponent comp : dataSelectionComponents) {
                selectionTab.addTab(comp.getName(), comp.getContents());
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

        return newDataSource;
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

        if (chooserDoTimeMatching) {
            dataSelection.putProperty(DataSelection.PROP_USESTIMEDRIVER,
                                      true);
        } else {
            dataSelection.putProperty(DataSelection.PROP_USESTIMEDRIVER,
                                      timeOption.equals(USE_DRIVERTIMES));
        }
        GeoSelection geoSelection = getGeoSelection();
        if (geoSelection != null) {
            if (strideCbx.isSelected()) {
                geoSelection.clearStride();
            }
            if (areaCbx.isSelected()) {
                geoSelection.setBoundingBox(null);
                geoSelection.setUseFullBounds(false);
            }
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

        if (dataSelectionComponents != null) {
            for (DataSelectionComponent comp : dataSelectionComponents) {
                comp.applyToDataSelection(dataSelection);
            }
        }

        List selectedMembers = getSelectedMembers();
        if ((selectedMembers != null) && (selectedMembers.size() > 0)) {
            Hashtable props = new Hashtable();
            props.put("prop.gridmembers", selectedMembers);
            dataSelection.setProperties(props);
        }

        return dataSelection;
    }




    /**
     * Check if everything is OK so we can create a display.
     * This just checks if the current data choice requires a volume and that the user has selected
     * Either "All Levels" or a range of levels
     *
     * @param addLevels Does the display need levels
     *
     * @return Is it ok to create the display
     */
    public boolean okToCreateTheDisplay(boolean addLevels) {
        if (lastChoiceRequiresVolume) {
            boolean inError = false;
            if ((levelsTab == null) || (levelsTab.getParent() == null)) {
                return true;
            }
            int[] selected = levelsList.getSelectedIndices();
            if (selected.length == 0) {
                //None selected
                inError = true;
            } else if ((selected.length == 1) && (selected[0] != 0)) {
                //One selected but not "All Levels"
                inError = true;
            }
            if (inError) {
                LogUtil.userErrorMessage(
                    new JLabel(
                        "<html>The selected field requires a 3D volume of data.<br>Please select \"All Levels\" or a range of 2 or more levels</html>"));
                return false;
            }
        }
        return true;
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
        areaCbx = new JCheckBox("Use Default", true);
        areaCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (areaComponent != null) {
                    GuiUtils.enableTree(areaComponent, !areaCbx.isSelected());
                }
            }
        });

        strideCbx = new JCheckBox("Use Default", true);
        strideCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (strideComponent != null) {
                    GuiUtils.enableTree(strideComponent,
                                        !strideCbx.isSelected());
                }
            }
        });

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
        if (idv.getUseTimeDriver()) {
            if (timeOptionLabelBox == null) {
                return null;
            }
        } else {
            if (allTimesButton == null) {
                return null;
            }
        }

        if (getUseAllTimes()) {
            return null;
        }
        return getSelectedDateTimesInList();
    }

    /**
     *  Return a list of Integer indices of the selected members.
     *
     *  @return List of indices.
     */
    public List getSelectedMembers() {
        if (membersList == null) {
            return null;
        }
        List selected = Misc.toList(membersList.getSelectedValues());

        int  ssize    = selected.size();
        for (int i = 0; i < ssize; i++) {
            TwoFacedObject to = (TwoFacedObject) selected.get(i);
            if (to.getLabel() == "All Members") {
                return Misc.getIndexList(members, members);
            }
        }
        return Misc.getIndexList(selected, members);
    }

    /**
     * Get selected times in the list
     *
     * @return  list of times
     */
    private List getSelectedDateTimesInList() {
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
        if (idv.getUseTimeDriver()) {
            if (timeOptionLabelBox == null) {
                return true;
            }
            return timeOptionLabelBox.getSelectedIndex() == 0;
        } else {
            if (allTimesButton == null) {
                return true;
            }
            return allTimesButton.isSelected();
        }
    }


    /**
     * Select the times in the times list
     *
     * @param all All times
     * @param selected The selected times
     */
    public void setTimes(List all, List selected) {
        //if we are not using defaults and the new list is the same as the old list
        //then keep around the currently selected times
        if ( !getUseAllTimes() && Misc.equals(allDateTimes, all)) {
            selected = getSelectedDateTimesInList();
        }

        if (idv.getUseTimeDriver()) {
            setTimes(timesList, timeOptionLabelBox, all, selected);
            if (all != null) {
                allDateTimes = new ArrayList(all);
            }
            int idx = timeOptionLabelBox.getSelectedIndex();

            if (idx == 1) {
                timesList.setEnabled(true);
            } else {
                timesList.setEnabled(false);
            }

        } else {
            setTimes(timesList, allTimesButton, all, selected);
            if (all != null) {
                allDateTimes = new ArrayList(all);
            }
            // hack to deal with the selection of the Use All for a datasource
            allTimesButton.setSelected(allTimesButton.isSelected());

            //OLD:
            timesList.setEnabled( !allTimesButton.isSelected());
        }
    }


    /**
     * Set the use all times flag
     *
     * @param useAllTimes  true to use all times
     */
    public void setUseAllTimes(boolean useAllTimes) {
        if (idv.getUseTimeDriver()) {
            if (timeOptionLabelBox != null) {
                if (useAllTimes) {
                    timeOptionLabelBox.setSelectedIndex(0);
                    timesList.setEnabled(false);
                } else {
                    timeOptionLabelBox.setSelectedIndex(1);
                    timesList.setEnabled(true);
                }
            } else if (allTimesButton != null) {
                allTimesButton.setSelected(useAllTimes);
                timesList.setEnabled( !allTimesButton.isSelected());
            }
        }
    }


    /**
     *  Create the GUI for the times list. (i.e., all times button and the
     *  times JList)
     *
     *  @return The GUI for times
     */
    public JComponent getTimesList() {
        if ( /*TODO dataSource != null*/false) {
            return getTimesList("Use All");
        } else {
            return getTimesList("Use Default");
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
            timesListInfo = makeTimesListAndPanel(cbxLabel, null);
            timesList     = (JList) timesListInfo[0];
            if (idv.getUseTimeDriver()) {
                timeOptionLabelBox = (JComboBox) timesListInfo[1];
            } else {
                allTimesButton = (JCheckBox) timesListInfo[1];
            }
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
            //            allTimesButton.setSelected(true);
        }


        //Don't automatically toggle the checkbox
        //OLD
        timesList.setEnabled( !allTimesButton.isSelected());
        //        allTimesButton.setSelected(allSelected);
    }

    /**
     * Add the given times in the all/selected list into the
     * given JList. Configure the allTimeButton  appropriately
     *
     *
     * @param timesList The JList to put the times into.
     * @param timeOptionLabelBox The checkbox that allows the user to select all or some
     * @param all All the times
     * @param selected The selected times
     */
    private static void setTimes(JList timesList,
                                 JComboBox timeOptionLabelBox, List all,
                                 List selected) {

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
        timeOptionLabelBox.setEnabled(sortedAllDateTimes.size() > 0);
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
            //            allTimesButton.setSelected(true);
        }


        //Don't automatically toggle the checkbox
        //OLD
        int idx = timeOptionLabelBox.getSelectedIndex();
        timesList.setEnabled(idx == 0);
        //        allTimesButton.setSelected(allSelected);
    }


    /**
     * Create the JList, an 'all times button', and a JPanel
     * to show times.
     *
     *
     * @param cbxLabel Label to use for the checkbox. (Use All or Use Default).
     * @param extra  extra component
     * @return A triple: JList, all times button and the JPanel that wraps this.
     */
    private JComponent[] makeTimesListAndPanel(String cbxLabel,
            JComponent extra) {
        final JComboBox timeOptionLabelBox = new JComboBox();
        final JList     timesList          = new JList();
        timesList.setBorder(null);
        //        timeline = new Timeline();
        TimesChooser.addTimeSelectionListener(timesList, timeline);
        timesList.setToolTipText("Right click to show selection menu");
        timesList.setSelectionMode(
            ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        final JCheckBox allTimesButton = new JCheckBox(cbxLabel, true);
        allTimesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                timesList.setEnabled( !allTimesButton.isSelected());
            }
        });
        //added
        timeOptionLabelBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object selectedObj = timeOptionLabelBox.getSelectedItem();
                setTimeOptions(selectedObj);
            }

        });

        //timeDeclutterFld = new JTextField("" + getTimeDeclutterMinutes(), 5);
        GuiUtils.enableTree(timeOptionLabelBox, true);

        List timeOptionNames = Misc.toList(timeSubsetOptionLabels);

        if (idv.getUseTimeDriver() && !doUseDisplay) {
            timeOptionNames.remove(2);
        }
        GuiUtils.setListData(timeOptionLabelBox, timeOptionNames);
        //        JComponent top = GuiUtils.leftRight(new JLabel("Times"),
        //                                            allTimesButton);
        JComponent top;

        if (idv.getUseTimeDriver()) {
            if (extra != null) {
                top = GuiUtils.leftRight(extra, timeOptionLabelBox);
            } else {
                top = GuiUtils.right(timeOptionLabelBox);
            }
        } else {
            if (extra != null) {
                top = GuiUtils.leftRight(extra, allTimesButton);
            } else {
                top = GuiUtils.right(allTimesButton);
            }

        }
        //NEW
        //        JComponent top      = GuiUtils.left(new JLabel("Times"));
        JComponent scroller = GuiUtils.makeScrollPane(timesList, 300, 100);
        //      scroller.setBorder(BorderFactory.createMatteBorder(1,2,0,0,Color.gray));
        if (idv.getUseTimeDriver()) {
            return new JComponent[] { timesList, timeOptionLabelBox,
                                      GuiUtils.topCenter(top, scroller) };
        } else {
            return new JComponent[] { timesList, allTimesButton,
                                      GuiUtils.topCenter(top, scroller) };
        }
    }

    /**
     * Set the time option from the selected object
     *
     * @param selectedObject  the selected time mode
     */
    public void setTimeOptions(Object selectedObject) {
        if (timesList == null) {
            return;
        }
        timeOption = selectedObject.toString();
        if (selectedObject.equals(USE_DEFAULTTIMES)) {
            //selectIdx = 0;
            timesList.setVisible(true);
            timesList.setEnabled(false);
        } else if (selectedObject.equals(USE_SELECTEDTIMES)) {
            //selectIdx = 1;
            timesList.setVisible(true);
            timesList.setEnabled(true);
            chooserDoTimeMatching = false;
        } else if (selectedObject.equals(USE_DRIVERTIMES)) {
            //selectIdx = 2;
            timesList.setVisible(false);
            timesList.setEnabled(false);
            if (lastDataChoice != null) {
                lastDataChoice.setProperty(DataSelection.PROP_USESTIMEDRIVER,
                                           true);
            }
        }
    }

    /**
     * Get the time option type
     * @return time s
     */
    public String getTimeOption() {
        return timeOption;
    }


    /**
     * Set levels from the display
     *
     * @param levels  the list of levels
     */
    public void setLevelsFromDisplay(List levels) {
        if ( !Misc.equals(levelsFromDisplay, levels)) {
            levelsFromDisplay = levels;
            updateSelectionTab(lastDataSource, lastDataChoice);
        }
    }

    /**
     * Set the DefaultLevelToFirst property.
     *
     * @param value The new value for DefaultLevelToFirst
     */
    public void setDefaultLevelToFirst(boolean value) {
        defaultLevelToFirst = value;
    }

    /**
     * Get the DefaultLevelToFirst property.
     *
     * @return The DefaultLevelToFirst
     */
    public boolean getDefaultLevelToFirst() {
        return defaultLevelToFirst;
    }

    /**
     * Get the DefaultMemberToAll property.
     *
     * The DefaultMemberToAll
     *
     * @param value  tru to set the default member to all
     */
    public void setDefaultMemberToAll(boolean value) {
        defaultMemberToAll = value;
    }

    /**
     * Get the DefaultMemberToAll property.
     *
     * @return The DefaultMemberToAll
     */
    public boolean getDefaultMemberToAll() {
        return defaultMemberToAll;
    }
}
