/*
 * $Id: PlanViewControl.java,v 1.185 2007/08/21 14:31:11 dmurray Exp $
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


package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.*;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;

import ucar.visad.display.Displayable;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.GridDisplayable;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.ZSelector;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Class to handle all kinds of PlanViews.  A plan view is a 
 * horizontal slice at a level.
 *
 * @author Unidata Development Team
 * @version $Revision: 1.185 $
 */
public abstract class PlanViewControl extends GridDisplayControl {

    /** Macro for the short parameter name for the label */
    public static final String MACRO_LEVEL = "%level%";

    /** property for sharing levels */
    public static final String SHARE_LEVEL = "PlanViewControl.SHARE_LEVEL";

    /** level selector */
    private ZSelector zSelector;

    /** level selection box */
    private JComboBox levelBox;

    /** level up button */
    private JButton levelUpBtn;

    /** level down button */
    private JButton levelDownBtn;

    /** level label */
    private JLabel levelLabel;

    /** cycle level checkbox */
    private JCheckBox cycleLevelsCbx;

    /** list of current levels */
    protected Real[] currentLevels;

    /** level readout label */
    protected JLabel levelReadout;

    /** level enabled */
    private boolean levelEnabled = false;

    /** level enabled */
    private boolean ignoreVerticalDimension = false;

    /** level animation flag */
    private boolean currentLevelAnimation = false;

    /** level index */
    private int currentLevelIdx = -1;

    /** the displayable for the plan data */
    private DisplayableData planDisplay;

    /** last Z value */
    private double lastZValue = 0;

    /** working grid */
    private FieldImpl workingGrid;

    /** current slice */
    protected FieldImpl currentSlice;

    /** current level */
    protected Real currentLevel;

    /**
     *  Have we loaded any data yet.
     */
    private boolean loadedAny = false;

    /** animation level */
    private int animationLevel = -1;

    /** flag for 3D display */
    private boolean displayIs3D = false;

    /** data choice for the data */
    protected DataChoice datachoice;

    /** data choice for the data */
    private boolean multipleIsTopography = false;

    /** vertical scalar map */
    private RealType topoType = null;

    /** Keep around the  range of the last level */
    private Range levelColorRange;

    //    private boolean levelAnimation = false;


    /**
     * Cstr; does nothing. See init() for creation actions.
     */
    public PlanViewControl() {
        setAttributeFlags(FLAG_DATACONTROL);
    }


    /**
     * Get the Data projection label
     *
     * @return  the label
     */
    protected String getDataProjectionLabel() {
        return "Use Grid Projection";
    }


    /*
     * _more_
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        if(currentLevel!=null) {
            dsd.addPropertyValue(currentLevel, "settingsLevel",
                         "Level", SETTINGS_GROUP_DISPLAY);
        }
    }




    /**
     * Add an entry into the range menu
     *
     * @param rw The widget that manages the range dialog
     * @param items List of menu items
     */
    public void addToRangeMenu(final RangeWidget rw, List items) {
        super.addToRangeMenu(rw, items);
        if (currentSlice == null) {
            return;
        }
        try {
            //Find the range of data
            Range[] range = GridUtil.getMinMax(currentSlice);
            int     index = getColorRangeIndex();
            // getColorRangeIndex returns 1 by default
            if (index >= range.length) {
                index = 0;
            }
            levelColorRange = range[index];
        } catch (Exception exc) {}
        if (levelColorRange == null) {
            return;
        }
        JMenuItem mi = new JMenuItem("From Displayed Data");
        items.add(mi);
        mi.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                rw.setRangeDialog(convertColorRange(levelColorRange));
            }
        });
    }



    /**
     * move up/down levels by the delta
     *
     * @param delta   delta between levels
     */
    private void moveUpDown(int delta) {
        int selected = levelBox.getSelectedIndex();
        if (selected >= 0) {
            selected += delta;
            int max = levelBox.getItemCount();
            if (selected >= max) {
                selected = max - 1;
            }
        }
        if (selected < 0) {
            selected = 0;
        }
        levelBox.setSelectedIndex(selected);
    }

    /**
     * Called to make this kind of Display Control; also calls code to
     * made the Displayable.
     * This method is called from inside DisplayControlImpl init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        datachoice = dataChoice;

        Trace.call1("PlanView.init");

        Trace.call1("PlanView.initMisc");
        //Create some of the gui components here
        levelReadout = new JLabel(" ");
        setLevelReadoutLabel(formatLevel(null));
        // in GridDisplayControl:

        levelBox = doMakeLevelControl(null);
        ImageIcon upIcon =
            GuiUtils.getImageIcon(
                "/ucar/unidata/idv/control/images/LevelUp.gif");
        levelUpBtn = new JButton(upIcon);
        levelUpBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        levelUpBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                moveUpDown(-1);
            }
        });

        ImageIcon downIcon =
            GuiUtils.getImageIcon(
                "/ucar/unidata/idv/control/images/LevelDown.gif");
        levelDownBtn = new JButton(downIcon);
        levelDownBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        levelDownBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                moveUpDown(1);
            }
        });

        //        levelLabel = GuiUtils.rLabel("<html><u>L</u>evels:");
        levelLabel = GuiUtils.rLabel(getLevelsLabel());
        levelLabel.setDisplayedMnemonic(GuiUtils.charToKeyCode("L"));
        levelLabel.setLabelFor(levelBox);

        // to toggle animation by level:
        cycleLevelsCbx = new JCheckBox("Cycle", false);
        int keyCode = GuiUtils.charToKeyCode("Y");
        if (keyCode != -1) {
            cycleLevelsCbx.setMnemonic(keyCode);
        }
        cycleLevelsCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                setLevelAnimation(cycleLevelsCbx.isSelected());
            }
        });



        // create the actual displayable; see code in sub class
        planDisplay = createPlanDisplay();
        // make visible and add it to display
        planDisplay.setVisible(true);

        //addDisplayable (planDisplay);
        displayIs3D = isDisplay3D();

        // Create a zSelector, a small selector point in the VisAD display that
        // the user can drag up and down.
        // Always create the zSelector. We still want to do this even if there are
        // no levels because the user can change the DataChoice to one that has levels.
        zSelector = new ZSelector(-1, -1, -1);
        zSelector.addPropertyChangeListener(this);
        Trace.call2("PlanView.initMisc");

        boolean result = setData(dataChoice);

        if (shouldShowZSelector()) {
            addDisplayable(zSelector, FLAG_COLOR);
        }

        if (haveMultipleFields()) {
            //If we have multiple fields then we want both the 
            //color unit and the display unit
            addDisplayable(planDisplay, FLAG_COLORTABLE | FLAG_COLORUNIT);
        } else {
            if (shouldShowLevelWidget()) {
                addDisplayable(planDisplay);
            } else if (shouldUseZPosition()) {
                addDisplayable(planDisplay, FLAG_ZPOSITION);
            } else {
                addDisplayable(planDisplay);
            }
        }
        Trace.call2("PlanView.init");
        return result;
    }


    /**
     * Handle property change
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                SelectorDisplayable.PROPERTY_POSITION)) {
            try {
                if ( !getHaveInitialized()) {
                    return;
                }
                loadDataAtZ(zSelector.getPosition().getValue());
            } catch (Exception exc) {
                logException("", exc);
            }
        } else {
            super.propertyChange(evt);
        }
    }



    /**
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        return super.haveMultipleFields() && !getMultipleIsTopography();
    }

    /**
     * Returns the index to use in the GridDataInstance array of ranges
     * for color ranges. If we are being draped over topography then
     * return 0. Else return the default value from the parent  class.
     *
     * @return  The index to be used for the color range.
     */
    protected int getColorRangeIndex() {
        if (getMultipleIsTopography()) {
            return 0;
        }
        return super.getColorRangeIndex();
    }



    /**
     * Called to initialize this control from the given dataChoice;
     * sets levels controls to match data; make data slice at first level;
     * set display's color table and display units.
     *
     * @param dataChoice  choice that describes the data to be loaded.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        //Now get the list of levels
        List   levelsList = dataChoice.getAllLevels();
        Real[] levels     = null;
        if ((levelsList != null) && (levelsList.size() > 0)) {
            levels = (Real[]) levelsList.toArray(new Real[levelsList.size()]);
        }

        Trace.call1("PlanView.setData");
        boolean result = super.setData(dataChoice);
        if ( !result) {
            Trace.call2("PlanView.setData");
            return false;
        }
        loadedAny = false;

        getGridDisplayable().setColoredByAnother(haveMultipleFields());
        if (getMultipleIsTopography()) {
            addTopographyMap();
        }


        Trace.call1("PlanView.getLevels");
        if (levels == null) {
            levels = getGridDataInstance().getLevels();
            if ((levels != null) && (currentLevel == null)) {
                currentLevel = levels[0];
            }
        }

        // If we have already made the plan view gui for some previous 
        // selected parameter, now then set up the levels controls as needed
        // for the new set of levels.
        setLevels(levels);
        Trace.call2("PlanView.getLevels");
        //We reassign the local levels variable here because some
        //derived classes might override setLevels to use some other levels
        //(e.g., CappiControl)
        levels = currentLevels;
        // if there are no levels do not shown the z selector
        setDisplayableVisibility(zSelector,
                                 ((levels != null) && (levels.length > 0)));
        loadDataAtLevel(currentLevel);
        Unit newUnit = getDisplayUnit();
        if ((newUnit != null)
                && ( !newUnit.equals(getRawDataUnit())
                     && Unit.canConvert(newUnit, getRawDataUnit()))) {
            planDisplay.setDisplayUnit(newUnit);
        }

        processRequestProperties();

        Trace.call2("PlanView.setData");
        return true;
    }


    /**
     * Wrapper around {@link #addTopographyMap(int)} to allow subclasses
     * to set their own index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void addTopographyMap() throws VisADException, RemoteException {
        addTopographyMap(1);
    }

    /**
     * Turn on or off animation by level according to input arg. true = on.
     *
     * @param on  true to animate
     */
    private void setLevelAnimation(boolean on) {
        if ( !levelEnabled) {
            return;
        }
        if (currentLevelAnimation == on) {
            return;
        }
        currentLevelAnimation = on;
        if (currentLevelAnimation) {
            Thread t = new Thread() {
                public void run() {
                    animateLevel();
                }
            };
            t.start();
        }
    }


    /**
     * Animate by levels by a loop incrementing the level index and
     * resetting level; wait 1000 miliseconds in level animation steps
     * (1 second  for grepping) between steps so it doesn't run away.
     * Must first call setLevelAnimation (true)
     */
    private void animateLevel() {
        while (getActive() && levelEnabled && currentLevelAnimation
                && (currentLevels != null)) {
            currentLevelIdx++;
            if (currentLevelIdx >= currentLevels.length) {
                currentLevelIdx = 0;
            }
            if ( !getActive()) {
                return;
            }
            try {
                setLevelFromUser(currentLevels[currentLevelIdx]);
            } catch (Exception ie) {
                logException("Setting level", ie);
                return;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                return;
            }

        }
    }


    /**
     * Determine whether the data in this <code>PlanViewControl</code>
     * has levels or not.
     *
     * @return  true if multi level data
     */
    public boolean haveLevels() {
        return levelEnabled;
    }

    /**
     * If there are, or are not, some levels in the data, set the controls
     * accordingly. If no levels exist, you do not enable the menu to
     * set levels for example.
     *
     * @param levels  array of levels
     */
    public void setLevels(Real[] levels) {
        setOkToFireEvents(false);
        currentLevels = levels;
        levelEnabled  = (levels != null);
        if ( !levelEnabled) {
            currentLevelAnimation = false;
            cycleLevelsCbx.setSelected(false);
        }

        levelBox.setEnabled(levelEnabled);
        levelUpBtn.setEnabled(levelEnabled);
        levelDownBtn.setEnabled(levelEnabled);
        levelLabel.setEnabled(levelEnabled);
        cycleLevelsCbx.setEnabled(levelEnabled);

        if (levels == null) {
            setLevelReadoutLabel(formatLevel(null));
        }

        GuiUtils.setListData(levelBox, formatLevels(levels));
        if (currentLevel != null) {
            levelBox.setSelectedItem(Util.labeledReal(currentLevel));
        }

        setOkToFireEvents(true);
    }


    /**
     * Overwrite the base class method to add a Levels menu to the edit menu
     *
     * @param items Menu items to add to
     * @param forMenuBar Is this edit menu for the main menu bar
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        if (currentLevels != null) {
            final JMenu levelsMenu = new JMenu("Levels");
            levelsMenu.addMenuListener(new MenuListener() {
                public void menuCanceled(MenuEvent e) {}

                public void menuDeselected(MenuEvent e) {}

                public void menuSelected(MenuEvent e) {
                    handleLevelMenuSelected(levelsMenu);
                }
            });
            items.add(levelsMenu);
        }
        super.getEditMenuItems(items, forMenuBar);
    }

    /**
     * Fill in the levelMenu with the current levels
     *
     * @param levelMenu The level menu
     */
    private void handleLevelMenuSelected(JMenu levelMenu) {
        levelMenu.removeAll();
        if ((currentLevels == null) || (currentLevels.length == 0)) {
            levelMenu.add(new JLabel(" No levels "));
            return;
        }
        for (int i = 0; i < currentLevels.length; i++) {
            final TwoFacedObject level = Util.labeledReal(currentLevels[i]);
            JMenuItem            mi    = new JMenuItem("" + level);
            levelMenu.add(mi);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        setLevelFromUser((Real) level.getId());
                    } catch (Exception exc) {
                        logException("setLevel", exc);
                    }
                }
            });
        }


    }


    /**
     * Return the displayable of the Plan View as a DisplayableData.
     *
     * @return  <code>DisplayableData</code> that is being used for the
     *          main depiction of this <code>PlanDisplay</code>
     */
    public DisplayableData getPlanDisplay() {
        return planDisplay;
    }


    /**
     * Return the <code>Displayable</code> of the Plan View as a
     * <code>GridDisplayable</code>.
     *
     * @return <code>Displayable</code> cast to a <code>GridDisplay</code>
     */
    public GridDisplayable getGridDisplayable() {
        return (GridDisplayable) planDisplay;
    }

    /**
     * Method for creating the <code>DisplayableData</code> object
     * that is the main depiction for the data controlled by this
     * <code>PlanViewControl</code>; implemented by each subclass.
     *
     * @return <code>DisplayableData</code> for the data depiction.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected abstract DisplayableData createPlanDisplay()
     throws VisADException, RemoteException;

    /**
     * Method to call if projection changes.  Handle topography
     * changes.
     */
    public void projectionChanged() {
        super.projectionChanged();
        try {
            if (getMultipleIsTopography()) {
                addTopographyMap();
            }
        } catch (Exception e) {}
    }


    /**
     * Return active level value.
     *
     * @return  active level
     */
    public Real getLevel() {
        return currentLevel;
    }


    /**
     * Set the active level.
     *
     * @param pl  present level.
     */
    public void setLevel(Real pl) {
        setLevel(pl, false);
    }


    /**
     * Set the active level.
     *
     * @param level present level.
     */
    public void setSettingsLevel(Real level) {
        setDataSelectionLevel(level);
        setLevel(level, false);
    }


    /**
     * Set the level in the data selection
     *
     * @param level The level
     */
    public void setDataSelectionLevel(Real level) {
        getDataSelection().setLevel(level);
    }


    /**
     * Set the level from the user
     *
     * @param pl  level
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void setLevelFromUser(Real pl)
            throws VisADException, RemoteException {
        setLevelFromUser(pl, false);
    }



    /**
     * Set the level from the user
     *
     * @param pl  level
     * @param fromSelector true if from selector
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setLevelFromUser(final Real pl, final boolean fromSelector)
            throws VisADException, RemoteException {
        //We only set the level if there is a level in the data selection
        //This implies that we were created with an initial level
        if ((getDataSelection().getFromLevel() != null)
                && Misc.equals(getDataSelection().getFromLevel(),
                               getDataSelection().getToLevel())) {
            getDataSelection().setLevel(pl);
            getDataInstance().setDataSelection(getDataSelection());
        }
        if ((workingGrid == null)
                || ( !GridUtil.isVolume(workingGrid)
                     && !Misc.equals(pl, currentLevel))) {
            showWaitCursor();
            try {
                getDataInstance().reInitialize();
            } finally {
                showNormalCursor();
            }
        }
        setLevel(pl, fromSelector);
    }


    /**
     * Move the Plan View to this level. Reset current level value.
     *
     * @param  pl   level to select
     * @param  fromSelector   true if being done by the selector.
     */
    private void setLevel(final Real pl, boolean fromSelector) {
        try {
            if ( !getHaveInitialized()) {
                currentLevel = pl;
            } else {
                deactivateDisplays();
                loadDataAtLevel(pl, fromSelector);
                doShare(SHARE_LEVEL, pl);
                activateDisplays();
            }
        } catch (Exception exc) {
            logException("setLevel", exc);
        }
    }


    /**
     * Load data at the level specified.  Uses the working grid.
     *
     * @param level  level to load at
     *
     * @throws  VisADException  illegal level or other VisAD error
     * @throws  RemoteException  RMI error
     */
    public void loadDataAtLevel(Real level)
            throws VisADException, RemoteException {
        loadDataAtLevel(level, false);
    }

    /**
     * Make calls to other classes to slice the data grid at the input level
     * and to load that data in the displayable.
     * Reset zselector to that level.
     *
     * @param level  level to load at
     * @param fromSelector  true if the selector caused this
     *
     * @throws  VisADException  illegal level or other VisAD error
     * @throws  RemoteException  RMI error
     */
    private void loadDataAtLevel(Real level, boolean fromSelector)
            throws VisADException, RemoteException {
        Trace.call1("PlanView.loadData");
        if (loadedAny && (level != null) && level.equals(currentLevel)) {
            return;
        }

        //If we have no data or if its a slice then reset the data selection
        //to the new level and refetch data
        if ((workingGrid == null) || !GridUtil.isVolume(workingGrid)) {
            try {
                showWaitCursor();
                //Just to make sure the DI has it
                //                getDataSelection().setLevel(level);
                //                getDataInstance().setDataSelection(getDataSelection());
                if (loadedAny) {
                    //                    getDataInstance().reInitialize();
                }
                workingGrid = (FieldImpl) getGridDataInstance().getGrid();
                if (workingGrid == null) {
                    return;
                }
            } finally {
                showNormalCursor();
            }
        }
        loadedAny    = true;
        currentLevel = level;
        //Trace.call1 ("PlanView.slice");
        currentSlice = null;
        int samplingMode = getSamplingModeValue(getDefaultSamplingMode());
        // NB: someday, someone needs to clean this block up without
        // breaking anything.
        if (GridUtil.isVolume(workingGrid)) {  // need to slice
            if (((level != null) && (currentLevels != null))
                    && (Arrays.binarySearch(currentLevels, level) >= 0)) {
                samplingMode = Data.NEAREST_NEIGHBOR;
            }
            // more than one level
            if ((level != null)
                    && ((currentLevels != null)
                        && (currentLevels.length > 1))) {
                // regular volume slice
                if (displayIs3D && !getMultipleIsTopography()) {
                    currentSlice = GridUtil.sliceAtLevel(workingGrid, level,
                            samplingMode);
                } else {  // slice for 2D display or topography
                    currentSlice = GridUtil.make2DGridFromSlice(
                        GridUtil.sliceAtLevel(
                            workingGrid, level, samplingMode));
                }
            } else {
                // only one level?  - can we get here?
                System.out.println("PlanViewControl: only one level?");
                currentSlice = workingGrid;
            }
        } else {  // 2D grid or requested slice
            currentSlice = workingGrid;
            if (GridUtil.is3D(currentSlice) && !displayIs3D) {
                currentSlice = GridUtil.make2DGridFromSlice(currentSlice);
            }
        }

        getGridDisplayable().loadData(getSliceForDisplay(currentSlice));
        //Trace.call2 ("PlanView.gridDisplayable.loadData");
        if ((level == null) || !displayIs3D) {
            return;
        }
        if (levelBox != null) {
            levelBox.setSelectedItem(Util.labeledReal(level));
        }

        Real altitude = null;
        // we do the try/catch around this for 2D data instead of just
        // setting the level to null.
        try {
            altitude = GridUtil.getAltitude(currentSlice, level);
        } catch (Exception ve) {
            altitude = null;
        }

        if ((altitude != null) && !altitude.isMissing()) {
            EarthLocationTuple elt = new EarthLocationTuple(0, 0,
                                         altitude.getValue());
            if ( !fromSelector) {
                lastZValue = earthToBox(elt)[2];
                zSelector.setZValue(lastZValue);
            }
            if (fromSelector) {
                level = GridUtil.getLevel(currentSlice, level);
            }
        }

        setLevelReadoutLabel("Current level: " + formatLevel(level));
        updateLegendAndList();
        Trace.call2("PlanView.loadData");
    }


    /**
     * Get the slice for the display
     *
     * @param slice  slice to use
     *
     * @return slice with skip value applied
     *
     * @throws VisADException  problem subsetting the slice
     */
    protected FieldImpl getSliceForDisplay(FieldImpl slice)
            throws VisADException {
        if (getSkipValue() <= 0) {
            return slice;
        }
        return GridUtil.subset(slice, getSkipValue() + 1);
    }

    /**
     * Add any macro name/label pairs
     *
     * @param names List of macro names
     * @param labels List of macro labels
     */
    protected void getMacroNames(List names, List labels) {
        super.getMacroNames(names, labels);
        names.addAll(Misc.newList(MACRO_LEVEL));
        labels.addAll(Misc.newList("Level"));
    }

    /**
     * Add any macro name/value pairs.
     *
     *
     * @param template template
     * @param patterns The macro names
     * @param values The macro values
     */
    protected void addLabelMacros(String template, List patterns,
                                  List values) {
        super.addLabelMacros(template, patterns, values);
        patterns.add(MACRO_LEVEL);
        if (currentLevel == null) {
            values.add("");
        } else {
            values.add("" + formatLevel(currentLevel));
        }
    }


    /**
     * Append any label information to the list of labels.
     *
     * @param labels   in/out list of labels
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        if (currentLevel != null) {
            labels.add("Level: " + formatLevel(currentLevel));
        }
    }

    /**
     * Format the level for labelling.  If subclasses want to have
     * different formatting, they can override this method.
     *
     * @param level  level to format
     *
     * @return formatted string for level
     */
    protected String formatLevel(Real level) {
        if (level == null) {
            return "                                       ";
        }
        StringBuffer buf = new StringBuffer();
        buf.append(getDisplayConventions().format(currentLevel.getValue()));
        buf.append(" ");
        buf.append(level.getUnit());
        return buf.toString();
    }

    /**
     * Move the Plan View to this level, in VisAD scale. Reset current
     * level value.  Called when user has moved z selector, or if z
     * selector has been moved in any other way.
     *
     * @param newZValue is always in VisAd scale of -1 to 1; unitless
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void loadDataAtZ(double newZValue)
            throws VisADException, RemoteException {
        if (lastZValue == newZValue) {
            return;
        }
        lastZValue = newZValue;
        Real altitude = boxToEarth(new double[] { 0.0, 0.0,
                            newZValue }).getAltitude();
        //TODO: make sure the unit of altitude is the same as the data. e.g., convert from m to Pa
        setLevelFromUser(altitude, true);
    }


    /**
     * Method called by other classes that share the selector.
     *
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if ( !getHaveInitialized()) {
            return;
        }
        if (dataId.equals(SHARE_LEVEL)) {
            try {
                //loadDataAtLevel((Real) data[0]);
                setLevelFromUser((Real) data[0]);
            } catch (Exception exc) {
                logException("receiveShareData.level", exc);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }


    /**
     * Make some Plan view controls for the UI.
     *
     * @return create the contents for the UI.
     */
    public Container doMakeContents() {
        return GuiUtils.top(
            GuiUtils.vbox(Misc.newList(doMakeWidgetComponent())));
    }

    /**
     * Make a DataInstance
     *
     * @param dataChoice the data choice
     *
     * @return  the DataInstance
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {

        if (currentLevel == null) {
            currentLevel = (Real) getDataSelection().getFromLevel();
            if (currentLevel == null) {
                List levelsList = dataChoice.getAllLevels();
                if ((levelsList != null) && (levelsList.size() > 0)) {
                    currentLevel = (Real) levelsList.get(0);
                }
            }
        }
        //Don't set this now
        //        getDataSelection().setLevel(currentLevel);
        return new GridDataInstance(dataChoice, getDataSelection(),
                                    getRequestProperties());

    }



    /**
     * Add in any special control widgets to the current list of widgets.
     *
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        //Allow derived classes to turn off the display of the widget
        if (shouldShowLevelWidget()) {
            JPanel levelUpDown = GuiUtils.doLayout(new Component[] {
                                     levelUpBtn,
                                     levelDownBtn }, 1, GuiUtils.WT_N,
                                         GuiUtils.WT_N);
            JPanel levelSelector = GuiUtils.doLayout(new Component[] {
                                       levelBox,
                                       levelUpDown }, 2, GuiUtils.WT_N,
                                           GuiUtils.WT_N);
            controlWidgets.add(new WrapperWidget(this, levelLabel,
                    GuiUtils.left(levelSelector),
                    GuiUtils.centerRight(levelReadout, cycleLevelsCbx)));
        }
    }




    /**
     * This allows for derived classes to turn this off.  Subclasses
     * should override if not true
     *
     * @return  true
     */
    protected boolean shouldShowLevelWidget() {
        return haveLevels();
    }

    /**
     * This allows for derived classes to turn this off.  Subclasses
     * should override if not true
     *
     * @return  true
     */
    protected boolean shouldShowZSelector() {
        return !getMultipleIsTopography() && haveLevels() && useZPosition();
    }


    /**
     * Determine if the display and gui should have a
     * z position. This is only used when we are also
     * not showing the level widget
     *
     * @return Should use z position
     */
    protected boolean shouldUseZPosition() {
        return !haveLevels() && !getMultipleIsTopography();
    }


    /**
     * Set the text for the level readout in the control window.
     *
     * @param text text for the level readout
     */
    public void setLevelReadoutLabel(String text) {
        if (levelReadout != null) {
            levelReadout.setText(text);
        }
    }

    /**
     * Set the other is topography property.
     *
     * @param v true if second parameter is topography
     */
    public void setMultipleIsTopography(boolean v) {
        multipleIsTopography = v;
    }

    /**
     * Get the multiple is topography property.
     *
     * @return true if multiple grid is topography
     */
    public boolean getMultipleIsTopography() {
        return multipleIsTopography;
    }

    /**
     * Set the ignore Vertical Dimension property
     *
     * @param v true if vertical dimension should be ignored
     */
    public void setIgnoreVerticalDimension(boolean v) {
        ignoreVerticalDimension = v;
    }

    /**
     * get the Ignore Vertical Dimension property
     *
     * @return true if vertical dimension should be ignored
     */
    public boolean getIgnoreVerticalDimension() {
        return ignoreVerticalDimension;
    }

    /**
     * Get the label for the levels box.
     * @return the label
     */
    public String getLevelsLabel() {
        return "Levels:";
    }

    /**
     * Can this display control write out data.
     * @return true if it can
     */
    public boolean canExportData() {
        return true;
    }

    /**
     * Get the DisplayedData
     * @return the data or null
     *
     * @throws RemoteException problem reading remote data
     * @throws VisADException  problem gettting data
     */
    protected Data getDisplayedData() throws VisADException, RemoteException {
        if ((planDisplay == null) || (planDisplay.getData() == null)) {
            return null;
        }
        return planDisplay.getData();
    }

    /**
     *  Use the value of the skip factor to subset the data.
     */
    protected void applySkipFactor() {

        if ((getGridDisplayable() != null) && (currentSlice != null)) {
            try {
                getGridDisplayable().loadData(
                    getSliceForDisplay(currentSlice));
            } catch (Exception ve) {
                logException("applySkipFactor", ve);
            }
        }
    }
}

