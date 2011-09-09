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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.GridDisplayable;
import ucar.visad.display.ScalarMapSet;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.ZSelector;

import visad.Data;
import visad.DisplayRealType;
import visad.FieldImpl;
import visad.Real;
import visad.RealType;
import visad.ScalarMap;
import visad.Unit;
import visad.VisADException;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;


import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;


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
    protected Object[] currentLevels;

    /** If we have a 3d volume of data then this is the levels we actually have from the data */
    private Object[] levelsFromData;

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
    protected Object currentLevel;

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

    /** multiple is topography flag */
    private boolean multipleIsTopography = false;

    /** parameter is topography flag */
    private boolean parameterIsTopography = false;

    /** ScalarMap for parameter topography */
    ScalarMap parameterTopoMap = null;

    /** vertical scalar map */
    private VerticalRangeWidget verticalRangeWidget = null;

    /** vertical scalar map */
    private Range verticalRange;

    /** Keep around the  range of the last level */
    private Range levelColorRange;

    //    private boolean levelAnimation = false;

    /** polygon mode */
    int polygonMode = Grid2DDisplayable.POLYGON_FILL;

    /** old smoothing type */
    private String OldSmoothingType = LABEL_NONE;

    /** old smoothing factor */
    private int OldSmoothingFactor = 0;

    /** flag for ensembles */
    protected boolean haveEnsemble = false;

    /**
     * Cstr; does nothing. See init() for creation actions.
     */
    public PlanViewControl() {
        setAttributeFlags(FLAG_DATACONTROL);
    }


    /**
     * Get the cursor readout data
     *
     * @return the data
     *
     * @throws Exception problem getting data
     */
    protected Data getCursorReadoutData() throws Exception {
        return currentSlice;
    }

    /**
     * Set the current slice
     *
     * @param slice  the slice
     *
     * @throws Exception  problem setting the slice
     */
    protected void setCurrentSlice(FieldImpl slice) throws Exception {
        currentSlice = slice;
    }

    /**
     * Get the current slice
     *
     * @return the current data for the plan view
     *
     * @throws Exception  problem getting the data
     */
    protected FieldImpl getCurrentSlice() throws Exception {
        return currentSlice;
    }

    /**
     * Get the cursor data
     *
     * @param el  earth location
     * @param animationValue   the animation value
     * @param animationStep  the animation step
     * @param samples the list of samples
     *
     * @return  the list of readout data
     *
     * @throws Exception  problem getting the data
     */
    protected List getCursorReadoutInner(EarthLocation el,
                                         Real animationValue,
                                         int animationStep,
                                         List<ReadoutInfo> samples)
            throws Exception {
        if (currentSlice == null) {
            return null;
        }
        List result = new ArrayList();
        Real r = GridUtil.sampleToReal(
                     currentSlice, el, animationValue,
                     getSamplingModeValue(
                         getObjectStore().get(
                             PREF_SAMPLING_MODE, DEFAULT_SAMPLING_MODE)));
        if (r != null) {
            ReadoutInfo readoutInfo = new ReadoutInfo(this, r, el,
                                          animationValue);
            readoutInfo.setUnit(getDisplayUnit());
            readoutInfo.setRange(getRange());
            samples.add(readoutInfo);
        }

        if ((r != null) && !r.isMissing()) {

            result.add("<tr><td>" + getMenuLabel()
                       + ":</td><td  align=\"right\">"
                       + formatForCursorReadout(r) + ((currentLevel != null)
                    ? ("@" + currentLevel)
                    : "") + "</td></tr>");
        }
        return result;
    }


    /**
     * Get the Data projection label
     *
     * @return  the label
     */
    protected String getDataProjectionLabel() {
        return "Use Grid Projection";
    }


    /**
     * Add DisplaySettings appropriate for this display
     *
     * @param dsd  the dialog to add to
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        if (currentLevel != null) {
            dsd.addPropertyValue(currentLevel, "settingsLevel", "Level",
                                 SETTINGS_GROUP_DISPLAY);
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
        try {
            if ((workingGrid != null) && GridUtil.isVolume(workingGrid)) {
                final Range r = getLevelColorRange();
                if (r == null) {
                    return;
                }
                JMenuItem mi = new JMenuItem("From Displayed Data");
                items.add(mi);
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        rw.setRangeDialog(convertColorRange(r));
                    }
                });
            }
        } catch (Exception exc) {
            logException("addToRangeMenu", exc);
        }
    }

    /**
     * Get the range for the current slice.
     * @return range or null
     */
    protected Range getLevelColorRange() {
        if (currentSlice == null) {
            return null;
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
            return null;
        }
        return levelColorRange;
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

        //        debug("PV-1");
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
        if ( !result) {
            return false;
        }

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
        /*
        if (getParameterIsTopography()) {
                addParameterTopographyMap();
        }
        */

        Trace.call2("PlanView.init");
        return result;

    }

    /**
     * What to do when you are done.
     */
    public void initDone() {
        // N.B.  This is done here instead of in init because the projection changed 
        // event wasn't getting passed through and the wrong ScalarMap was being used.
        if (getParameterIsTopography()) {
            try {
                addParameterTopographyMap();
            } catch (Exception e) {}
        }
    }

    /**
     * Create a jcombobox for setting the polygon mode.
     *
     * @return polygon mode combo box
     */
    protected JComboBox getPolyModeComboBox() {
        JComboBox polyModeCombo = new JComboBox();
        TwoFacedObject[] polyModes = { new TwoFacedObject(
                                         "Solid",
                                         new Integer(
                                             Grid2DDisplayable.POLYGON_FILL)),
                                       new TwoFacedObject("Mesh",
                                           new Integer(Grid2DDisplayable
                                               .POLYGON_LINE)),
                                       new TwoFacedObject("Points",
                                           new Integer(Grid2DDisplayable
                                               .POLYGON_POINT)) };
        GuiUtils.setListData(polyModeCombo, polyModes);
        polyModeCombo.setSelectedIndex((getPolygonMode()
                                        == Grid2DDisplayable.POLYGON_POINT)
                                       ? 2
                                       : (getPolygonMode()
                                          == Grid2DDisplayable.POLYGON_LINE)
                                         ? 1
                                         : 0);
        polyModeCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    setPolygonMode(((Integer) ((TwoFacedObject) ((JComboBox) e
                        .getSource()).getSelectedItem()).getId()).intValue());

                    if ((planDisplay != null)
                            && (planDisplay instanceof Grid2DDisplayable)) {
                        ((Grid2DDisplayable) planDisplay).setPolygonMode(
                            getPolygonMode());
                    }
                } catch (Exception ve) {
                    logException("setPolygonMode", ve);
                }
            }
        });
        return polyModeCombo;

    }

    /**
     *
     *  Handle property change
     *
     *  @param evt The event
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
     * Remove this control
     *
     * @throws RemoteException  Java RMI problem
     * @throws VisADException   VisAD data problem
     */
    public void doRemove() throws RemoteException, VisADException {
        super.doRemove();
        datachoice  = null;
        workingGrid = null;
        parameterTopoMap = null;
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

        Trace.call1("PlanView.setData");
        boolean result = super.setData(dataChoice);
        if ( !result) {
            Trace.call2("PlanView.setData");
            return false;
        }
        loadedAny = false;

        // TODO:  We might want to move haveEnsembles up to GridDisplayControl in the future
        if (getGridDataInstance() != null) {
            haveEnsemble = getGridDataInstance().getNumEnsembles() > 1;
        }

        getGridDisplayable().setColoredByAnother(haveMultipleFields());
        if (getMultipleIsTopography()) {
            addTopographyMap();
        }
        Trace.call1("PlanView.getLevels");
        //Now get the list of levels. We don't want to pass in the level range here since then we won't see
        //any other levels
        DataSelection tmpSelection = new DataSelection(getDataSelection());
        tmpSelection.setFromLevel(null);
        tmpSelection.setToLevel(null);

        List     levelsList = dataChoice.getAllLevels(tmpSelection);
        Object[] levels     = null;
        if ((levelsList != null) && (levelsList.size() > 0)) {
            levels =
                (Object[]) levelsList.toArray(new Object[levelsList.size()]);
        }


        if (levels == null) {
            levels = getGridDataInstance().getLevels();
        }

        if (currentLevel == null) {
            currentLevel = getDataSelection().getFromLevel();
        }
        if ((levels != null) && (levels.length > 0)
                && (currentLevel == null)) {
            currentLevel = levels[0];
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
     * Add a topography map for the parameter
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException Unable to set the ScalarMap
     */
    protected void addParameterTopographyMap()
            throws VisADException, RemoteException {
        NavigatedDisplay nd = getNavigatedDisplay();
        if (nd == null) {
            return;
        }
        DisplayRealType vertType = getDisplayAltitudeType();
        ScalarMapSet    mapSet   = getPlanDisplay().getScalarMapSet();
        if (parameterTopoMap != null) {
            mapSet.remove(parameterTopoMap);
        }
        RealType paramTopoType = getGridDataInstance().getRealType(0);
        parameterTopoMap = new ScalarMap(paramTopoType, vertType);
        parameterTopoMap.setOverrideUnit(getDisplayUnit());
        if (verticalRange != null) {
            setVerticalRange(verticalRange);
        }
        mapSet.add(parameterTopoMap);
        getPlanDisplay().setScalarMapSet(mapSet);
    }

    /**
     * Set the range on the parameter topography ScalarMap
     *
     * @param vertRange the vertical range
     */
    public void setVerticalRange(Range vertRange) {
        verticalRange = vertRange;
        if (vertRange != null) {
            try {
                if (parameterTopoMap != null) {
                    parameterTopoMap.setRange(vertRange.getMin(),
                            vertRange.getMax());
                }
            } catch (Exception exc) {
                logException("Unable to set the vertical range ", exc);
            }
            if (verticalRangeWidget != null) {
                verticalRangeWidget.setRange(vertRange);
            }
        }
    }

    /**
     * Get the vertical range
     * @return the vertical range
     */
    public Range getVerticalRange() {
        return verticalRange;
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
    public void setLevels(Object[] levels) {
        setOkToFireEvents(false);
        currentLevels = levels;
        levelEnabled  = (levels != null);
        if ( !levelEnabled) {
            currentLevelAnimation = false;
            cycleLevelsCbx.setSelected(false);
        }

        if (levelBox == null) {
            return;
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
            levelBox.setSelectedItem(currentLevel);
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
            final TwoFacedObject level = getLabeledReal(currentLevels[i]);
            JMenuItem            mi    = new JMenuItem("" + level);
            levelMenu.add(mi);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        setLevelFromUser(level.getId());
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
     * Set the point size
     *
     * @param value  the size
     */
    public void setPointSize(float value) {
        super.setPointSize(value);
        if (planDisplay != null) {
            try {
                planDisplay.setPointSize(getPointSize());
            } catch (Exception e) {
                logException("Setting point size", e);
            }
        }
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
            if (getParameterIsTopography()) {
                addParameterTopographyMap();
            }
        } catch (Exception e) {}
    }


    /**
     * Return active level value.
     *
     * @return  active level
     */
    public Object getLevel() {
        return currentLevel;
    }


    /**
     * Set the active level.
     *
     * @param pl  present level.
     */
    public void setLevel(Object pl) {
        setLevel(pl, false);
    }


    /**
     * Set the active level.
     *
     * @param level present level.
     */
    public void setSettingsLevel(Object level) {
        setDataSelectionLevel(level);
        setLevel(level, false);
    }


    /**
     * Set the level in the data selection
     *
     * @param level The level
     */
    public void setDataSelectionLevel(Object level) {
        if (level instanceof String) {
            try {
                Real level1 = Util.toReal((String) level);
                getDataSelection().setLevel(level1);
            } catch (Exception e) {
                System.err.println("error parsing level: " + level + " " + e);
                getDataSelection().setLevel(level);
            }
        } else {
            getDataSelection().setLevel(level);
        }
    }


    /**
     * Set the level from the user
     *
     * @param pl  level
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void setLevelFromUser(Object pl)
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
    private void setLevelFromUser(final Object pl, final boolean fromSelector)
            throws VisADException, RemoteException {
        //We only set the level if there is a level in the data selection
        //This implies that we were created with an initial level
        if ((getDataSelection().getFromLevel() != null)
                && Misc.equals(getDataSelection().getFromLevel(),
                               getDataSelection().getToLevel())) {
            Real realLevel = getLevelReal(pl);
            getDataSelection().setLevel(realLevel);
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
    private void setLevel(final Object pl, boolean fromSelector) {
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
    public void loadDataAtLevel(Object level)
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
    private void loadDataAtLevel(Object level, boolean fromSelector)
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
                workingGrid = (FieldImpl) getGrid(getGridDataInstance());
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
        int  samplingMode = getSamplingModeValue(getDefaultSamplingMode());
        Real realLevel    = getLevelReal(level);
        // NB: someday, someone needs to clean this block up without
        // breaking anything.
        if (GridUtil.isVolume(workingGrid)) {  // need to slice
            if (((level != null) && (currentLevels != null))
                    && hasLevel(level)) {
                samplingMode = Data.NEAREST_NEIGHBOR;
            }
            // more than one level
            if ((level != null)
                    && ((currentLevels != null)
                        && (currentLevels.length > 1))) {
                if (realLevel == null) {
                    return;
                }
                // regular volume slice
                if (displayIs3D && !getMultipleIsTopography()) {
                    currentSlice = GridUtil.sliceAtLevel(workingGrid,
                            realLevel, samplingMode);
                } else {  // slice for 2D display or topography
                    currentSlice = GridUtil.make2DGridFromSlice(
                        GridUtil.sliceAtLevel(
                            workingGrid, realLevel, samplingMode));
                }
                if (levelsFromData == null) {
                    levelsFromData = getGridDataInstance().getLevels();
                    setLevels(levelsFromData);
                }
            } else {
                // only one level?  - can we get here?
                //                System.out.println("PlanViewControl: only one level?");
                //                Trace.msg("got here");
                currentSlice = workingGrid;
            }
        } else {  // 2D grid or requested slice
            currentSlice = workingGrid;
            if (GridUtil.is3D(currentSlice)
                    && ( !displayIs3D || getMultipleIsTopography()
                         || getParameterIsTopography())) {
                currentSlice = GridUtil.make2DGridFromSlice(currentSlice);
            }
        }

        getGridDisplayable().loadData(getSliceForDisplay(currentSlice));
        //Trace.call2 ("PlanView.gridDisplayable.loadData");
        if ((level == null) || (realLevel == null) || !displayIs3D) {
            return;
        }
        if (levelBox != null) {
            levelBox.setSelectedItem(getLabeledReal(level));
        }

        Real altitude = null;
        // we do the try/catch around this for 2D data instead of just
        // setting the level to null.
        try {
            altitude = GridUtil.getAltitude(currentSlice, realLevel);
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
                level = GridUtil.getLevel(currentSlice, realLevel);
            }
        }

        setLevelReadoutLabel("Current level: " + formatLevel(level));
        updateLegendAndList();
        Trace.call2("PlanView.loadData");

    }




    /**
     * Does the list of levels have this level
     *
     * @param level  the level in question
     *
     * @return  true if it is in the list
     */
    private boolean hasLevel(Object level) {
        if ((currentLevels == null) || (level == null)) {
            return false;
        }
        Object firstLevel = currentLevels[0];
        if (level.getClass().equals(firstLevel.getClass())) {
            return (Arrays.binarySearch(currentLevels, level) >= 0);
        }
        if ((level instanceof Real)
                && (firstLevel instanceof TwoFacedObject)) {
            return TwoFacedObject.findId(level, Misc.toList(currentLevels))
                   != null;
        }
        return false;
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
        FieldImpl retField = slice;
        if (slice != null) {
            // apply skip factor
            if (getSkipValue() > 0) {
                retField = GridUtil.subset(retField, getSkipValue() + 1);
            }
            // apply smoothing
            if (checkFlag(FLAG_SMOOTHING)
                    && !getSmoothingType().equals(LABEL_NONE)) {
                retField = GridUtil.smooth(retField, getSmoothingType(),
                                           getSmoothingFactor());
            }
        }
        //System.out.println("slice for " + paramName + " = " + retField);
        return retField;
    }

    /**
     *  Use the value of the smoothing type and weight to subset the data.
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  VisAD problem
     */
    protected void applySmoothing() throws VisADException, RemoteException {
        if (checkFlag(FLAG_SMOOTHING)) {
            if ((getGridDisplayable() != null) && (currentSlice != null)) {
                if ( !getSmoothingType().equalsIgnoreCase(LABEL_NONE)
                        || !OldSmoothingType.equalsIgnoreCase(LABEL_NONE)) {
                    if ( !getSmoothingType().equals(OldSmoothingType)
                            || (getSmoothingFactor() != OldSmoothingFactor)) {
                        OldSmoothingType   = getSmoothingType();
                        OldSmoothingFactor = getSmoothingFactor();
                        try {
                            getGridDisplayable().loadData(
                                getSliceForDisplay(currentSlice));
                        } catch (Exception ve) {
                            logException("applySmoothing", ve);
                        }
                    }
                }
            }
        }
    }

    /**
     * Test if the given flag is set in the attrbiuteFlags
     *
     * @param f The flag to check
     * @return Is the given flag set
     */
    protected boolean checkFlag(int f) {
        if (f == FLAG_SMOOTHING) {
            return super.checkFlag(f) && !getMultipleIsTopography();
        }
        return super.checkFlag(f);
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
    protected String formatLevel(Object level) {
        if (level == null) {
            return "                                       ";
        }

        Real         myLevel = getLevelReal(level);
        StringBuffer buf     = new StringBuffer();
        buf.append(getDisplayConventions().format(myLevel.getValue()));
        buf.append(" ");
        buf.append(myLevel.getUnit());
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
                setLevelFromUser(data[0]);
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
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {

        //if (currentLevel == null) {
        //    currentLevel = (Real) getDataSelection().getFromLevel();
        //    if (currentLevel == null) {
        //        List levelsList = dataChoice.getAllLevels(getDataSelection());
        //        if ((levelsList != null) && (levelsList.size() > 0)) {
        //            currentLevel = (Real) levelsList.get(0);
        //        }
        //    }
        //}

        //Don't set this now
        //        getDataSelection().setLevel(currentLevel);
        return new GridDataInstance(dataChoice, getDataSelection(),
                                    getRequestProperties());

    }
     */


    /**
     * Add in any special control widgets to the current list of widgets.
     *
     * @param controlWidgets  list of control widgets
     *
     * @throws VisADException   VisAD error
     * @throws RemoteException   RMI error
     */
    public void getControlWidgets(List<ControlWidget> controlWidgets)
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
        if (getParameterIsTopography()) {
            if (verticalRange == null) {
                verticalRange = getColorRangeFromData();
            }
            verticalRangeWidget = new VerticalRangeWidget(this,
                    verticalRange);
            addRemovable(verticalRangeWidget);
            controlWidgets.add(verticalRangeWidget);
        }
    }

    /**
     * A hook that is called when the display unit is changed. Allows
     * derived classes to act accordingly.
     *
     * @param oldUnit The old color unit
     * @param newUnit The new color unit
     */
    protected void displayUnitChanged(Unit oldUnit, Unit newUnit) {
        if (parameterTopoMap != null) {
            try {
                parameterTopoMap.setOverrideUnit(newUnit);
                if (verticalRangeWidget != null) {
                    Range newRange =
                        Util.convertRange(verticalRangeWidget.getRange(),
                                          oldUnit, newUnit);
                    setVerticalRange(newRange);
                }
            } catch (Exception excp) {
                logException("Unable to set the topo override unit", excp);
            }
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
        boolean b = !getMultipleIsTopography() && haveLevels()
                    && useZPosition() && !getParameterIsTopography();
        try {
            b = b && ((workingGrid != null)
                      && GridUtil.isVolume(workingGrid));
        } catch (Exception e) {
            b = false;
        }
        return b;
    }


    /**
     * Determine if the display and gui should have a
     * z position. This is only used when we are also
     * not showing the level widget
     *
     * @return Should use z position
     */
    protected boolean shouldUseZPosition() {
        return !haveLevels() && !getMultipleIsTopography()
               && !getParameterIsTopography();
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
     * Set the parameter is topography property.
     *
     * @param v true if second parameter is topography
     */
    public void setParameterIsTopography(boolean v) {
        parameterIsTopography = v;
    }

    /**
     * Get the parameter is topography property.
     *
     * @return true if multiple grid is topography
     */
    public boolean getParameterIsTopography() {
        return parameterIsTopography;
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

        // if (checkFlag(FLAG_SKIPFACTOR)) {
        if ((getGridDisplayable() != null) && (currentSlice != null)) {
            try {
                getGridDisplayable().loadData(
                    getSliceForDisplay(currentSlice));
            } catch (Exception ve) {
                logException("applySkipFactor", ve);
            }
        }
        // }
    }



    /**
     * Set the type of depiction (solid, line, mesh) for this display
     *
     * @param v polygon mode.  Used by XML persistence.
     */
    public void setPolygonMode(int v) {
        polygonMode = v;
    }

    /**
     * Return the type of depiction for this display
     *
     * @return true if shading is smoothed.
     */
    public int getPolygonMode() {
        return polygonMode;
    }

    /**
     * A widget for the control window for setting the vertical range properties
     *
     * @author  Unidata Development Team
     */
    public class VerticalRangeWidget extends ControlWidget {

        /** range */
        private Range range;

        /** The label for widget */
        private JLabel label;

        /** A button for brining up the editor */
        private JButton button;

        /** The right hand label that shows some of the contour information */
        private JLabel rhLabel;

        /** Change range dialog */
        private RangeDialog rangeDialog;

        /**
         * Construct a VerticalRangeWidget
         *
         * @param control      the associate control
         * @param range The initial range
         */
        public VerticalRangeWidget(PlanViewControl control, Range range) {
            this(control, range, "Change Vertical Range");
        }

        /**
         * Construct a VerticalRangeWidget
         *
         * @param control      the associate control
         * @param range The initial range
         * @param dialogTitle Dialog title
         */
        public VerticalRangeWidget(PlanViewControl control, Range range,
                                   String dialogTitle) {
            super(control);
            label   = new JLabel("Vertical Range:", SwingConstants.RIGHT);
            rhLabel = new JLabel(" ");
            setRange(range);
            button = new JButton("Change");
            button.addActionListener(this);
        }

        /**
         * Method public due to ActionListener implementation
         *
         * @param ae    action event
         */
        public void actionPerformed(ActionEvent ae) {
            showChangeRangeDialog();
        }


        /**
         * Show the dialog
         */
        public void showChangeRangeDialog() {
            if (rangeDialog == null) {
                rangeDialog = new RangeDialog(getDisplayControl(), range,
                        "Change Vertical Range", "setVerticalRange", button);
            }
            rangeDialog.showDialog();
        }

        /**
         * Get the range information for this widget.
         *
         * @return the Range
         */
        public Range getRange() {
            return range;
        }

        /**
         * Set the range information for this widget.
         *
         * @param r  new Range
         */
        public void setRange(Range r) {
            this.range = r;
            if (r != null) {
                updateLabel();
                if (rangeDialog != null) {
                    rangeDialog.setRangeDialog(r);
                }
            }
        }

        /**
         * Update the label
         */
        private void updateLabel() {
            if (rhLabel == null) {
                return;
            }
            StringBuilder buf = new StringBuilder();
            buf.append("From: ");
            buf.append(getDisplayConventions().format(range.getMin()));
            buf.append(" To: ");
            buf.append(getDisplayConventions().format(range.getMax()));
            if (displayControl.getDisplayUnit() != null) {
                ;
            }
            {
                buf.append(" ");
                buf.append(displayControl.getDisplayUnit());
            }
            rhLabel.setText(buf.toString());
        }

        /**
         * Get the label for this widget.
         *
         * @return   the label.
         */
        public JLabel getLabel() {
            return label;
        }

        /**
         * Fill a list of components
         *
         * @param l    list of widgets
         * @param columns  number of columns for layout
         */
        public void fillList(List l, int columns) {
            l.add(label);
            l.add(GuiUtils.doLayout(new Component[] {
                GuiUtils.inset(button, new Insets(0, 8, 0, 0)),
                new Label(" "), rhLabel, GuiUtils.filler() }, 4,
                    GuiUtils.WT_NNNY, GuiUtils.WT_N));
        }

        /**
         * Get the range from the color table
         *
         * @return range from the color table
         */
        public Range getRangeFromColorTable() {

            Range ctRange = null;
            ColorTable originalCT =
                getDisplayControl().getOldColorTableOrInitialColorTable();
            if (originalCT != null) {
                ctRange = originalCT.getRange();
            }
            return ctRange;
        }

        /**
         * Called to remove this from the display.
         */
        public void doRemove() {
            super.doRemove();
            if (rangeDialog != null) {
                rangeDialog.doRemove();
                rangeDialog = null;
            }
        }

    }


}
