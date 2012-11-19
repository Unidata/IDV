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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.*;


import ucar.unidata.data.grid.DerivedGridFactory;
import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.control.drawing.*;
import ucar.unidata.ui.FineLineBorder;

import ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.util.*;

import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.Util;
import ucar.visad.display.*;


import ucar.visad.quantities.CommonUnits;
import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.LatLonPoint;
import visad.util.DataUtility;


import java.awt.*;
import java.awt.event.*;


import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;
import javax.swing.*;



/**
 * A MetApps Display Control for drawing lines on a navigated
 * display.
 *
 * @author MetApps development team
 * @version $Revision: 1.145 $
 */

public class GridTrajectoryControl extends TrackControl {


    /** xgrf xml attribute */
    public static final String ATTR_USETIMESINANIMATION =
            "usetimesinanimation";


    /** The cursor used for when in delete mode */
    static Cursor deleteCursor;

    /** property for sharing lines */
    public static final String SHARE_LINES = "DrawingControl.SHARE_LINES";

    /** Filled capable flag for the command object */
    public static final int FLAG_FILLED = 1 << 1;

    /** Full lat/lon capable flag for the command object */
    public static final int FLAG_FULLLATLON = 1 << 2;

    /** flag for straight */
    public static final int FLAG_STRAIGHT = 1 << 3;


    /** Controls the disabled state */
    protected JCheckBox enabledCbx;

    /** Is this control editable */
    private boolean editable = true;

    /** The title we get from the xml */
    private String editorTitle = null;


    /** The current coordinate type */
    private int coordType = DrawingGlyph.COORD_LATLON;

    /** Filled mode */
    protected JCheckBox straightCbx;

    /** List of all glyphs */
    protected List glyphs = new ArrayList();

    /** List of selected glyphs */
    protected List selectedGlyphs = new ArrayList();

    /** Top level displayable */
    CompositeDisplayable displayHolder0;


    /** Currently manipulated glyph */
    DrawingGlyph currentGlyph ;



    /** Current command */
    protected DrawingCommand currentCmd =
            GlyphCreatorCommand.CMD_SMOOTHPOLYGON;


    /** If we get our glyphs from a datachoice this is set to true */
    private boolean displayOnly = true;

    /** We can evaluate jython on a glyph */
    private String glyphJython;


    /** Do we skip the next time the mouse is released. */
    private boolean skipNextMouseReleased = false;

    /** the autoscroll counter */
    private int autoScrollCnt = 0;

    /** command */
    public static final DrawingCommand CMD_REMOVE =
            new DrawingCommand("Remove graphic", "remove all shape graphics",
                    "/auxdata/ui/icons/Reshape16.gif");

    DrawingControl tdc = null;

    DataChoice dataChoice;

    private StationModelDisplayable indicator = null;
    private float markerScale = 1.0f;
    private DisplayableData timesHolder = null;
    private EarthLocationLite lastIndicatorPosition;

    private JButton levelUpBtn;
    private JButton levelDownBtn;
    private JComboBox levelBox;
    private JLabel levelLabel;
    protected Object currentLevel;
    protected Object[] currentLevels;
    private boolean levelEnabled = false;
    private Unit zunit;
    private Unit newZunit = CommonUnit.meter;
    private Range lastRange;
    private boolean useTrackTimes = true;
    private static final Data DUMMY_DATA = new Real(0);
    private double timeDeclutterMinutes = 1;
    CoordinateSystem pressToHeightCS;

    /** streamlines button */
    private JRadioButton pointsBtn;

    /** vector/barb button */
    private JRadioButton rectangleBtn;

    /** flag for streamlines */
    boolean isPoints = true;

    private JButton createTrjBtn;

    JPanel controlPane;
    /**
     * Create a new Drawing Control; set attributes.
     */
    public GridTrajectoryControl() {
        setAttributeFlags(FLAG_DISPLAYUNIT);
    }


    /**
     * How long do we sleep until we act on the control changed event
     *
     * @return sleep time
     */
    protected long getControlChangeSleepTime() {
        return 250;
    }





    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     * @return true if everything is okay
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
       // super.init(dataChoice);
        this.dataChoice = dataChoice;
        List ds = new ArrayList();
        dataChoice.getDataSources(ds);
        GeoGridDataSource gds = (GeoGridDataSource)ds.get(0);
        List ldc = gds.getDataChoices();

        DataChoice sdc = (DataChoice)ldc.get(3);

        setColor(Color.red);
        tdc = new DrawingControl();
        if (tdc.deleteCursor == null) {
            tdc.deleteCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                    GuiUtils.getImage("/auxdata/ui/icons/Cut16.gif"),
                    new Point(0, 0), "Custom Delete");
        }
        GridDataInstance gdi = new GridDataInstance(sdc,
                getDataSelection(), getRequestProperties());
        setDisplayUnit(gdi.getRawUnit(0));
        initDisplayUnit();
        tdc.setDisplayUnit(getDisplayUnit());
        tdc.setZPosition(getZPosition());
        tdc.displayHolder = new CompositeDisplayable();
        tdc.displayHolder.setUseTimesInAnimation(getUseTimesInAnimation());
        addDisplayable(tdc.displayHolder);

        tdc.controlContext = getControlContext();
        tdc.setDefaultView(getDefaultView());
        tdc.addViewManager(getViewManager());
        glyphs = new ArrayList();

        setDisplayActive();

        getViewAnimation();


        if (sdc != null) {
            Data data = sdc.getData(null);
            if (data != null) {
                editable    = true;
                displayOnly = false;

            }
        }
        // level widget init
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

        DataSelection tmpSelection = new DataSelection(getDataSelection());
        tmpSelection.setFromLevel(null);
        tmpSelection.setToLevel(null);

        List     levelsList = sdc.getAllLevels(tmpSelection);
        Object[] levels     = null;
        if ((levelsList != null) && (levelsList.size() > 0)) {
            levels =
                    (Object[]) levelsList.toArray(new Object[levelsList.size()]);
        }


        if (levels == null) {
            levels = ((GridDataInstance) getDataInstance()).getLevels();
            zunit = ((GridDataInstance) getDataInstance()).getZUnit();
        }

        if (currentLevel == null) {
            currentLevel = getDataSelection().getFromLevel();
        }
        if ((levels != null) && (levels.length > 0)
                && (currentLevel == null)) {
            currentLevel = levels[0];
        }

        setLevels(levels) ;

        // the control for the track
        trackDisplay = new TrackDisplayable("track" + dataChoice);
        setLineWidth(trackWidth);
        addDisplayable(trackDisplay, getAttributeFlags());
        selectRangeDisplay = new SelectRangeDisplayable();
        addDisplayable(selectRangeDisplay,
                FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);
        getViewAnimation();
        indicator = new StationModelDisplayable("indicator");
        indicator.setScale(markerScale);
        indicator.setShouldUseAltitude(true);
        updateIndicator();
        addDisplayable(indicator);
        timesHolder = new LineDrawing("track_time" + dataChoice);
        timesHolder.setManipulable(false);
        timesHolder.setVisible(false);
        addDisplayable(timesHolder);
       // return setData(dataChoice);

        return true;
    }


    /** current level */

    public void setLevels(Object[] levels) {
        setOkToFireEvents(false);
        currentLevels = levels;
        levelEnabled  = (levels != null);

        if (levelBox == null) {
            return;
        }
        levelBox.setEnabled(levelEnabled);
        levelUpBtn.setEnabled(levelEnabled);
        levelDownBtn.setEnabled(levelEnabled);
        levelLabel.setEnabled(levelEnabled);




        GuiUtils.setListData(levelBox, formatLevels(levels));
        if (currentLevel != null) {
            levelBox.setSelectedItem(currentLevel);
        }

        setOkToFireEvents(true);
    }

    public void setLevel(Object r) {
        currentLevel = r;
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
     * Get the label for the levels box.
     * @return the label
     */
    public String getLevelsLabel() {
        return "Levels:";
    }

    public void applyTimeRange() {
        try {
            DataTimeRange    dataTimeRange    = getDataTimeRange(true);
            GridDataInstance gridDataInstance = getGridDataInstance();
            Unit             dataTimeUnit;
            DateTime[] dts = gridDataInstance.getDateTimes();
            dataTimeUnit = dts[0].getUnit();
            int size = dts.length;
           // Range    r                = getRangeForTimeSelect();
           // RealType dataTimeRealType = Util.getRealType(dataTimeUnit);
            Real startReal = dts[0].getReal();
            Real endReal = dts[size-1].getReal();


            Animation anime    = getViewAnimation();
            Real      aniValue = ((anime != null)
                    ? anime.getAniValue()
                    : null);

            Real[] startEnd = getDataTimeRange().getTimeRange(startReal,
                    endReal, aniValue);


            double startDate = startEnd[0].getValue(dataTimeUnit);
            double endDate   = startEnd[1].getValue(dataTimeUnit);
            if ( !Misc.equals(lastRange, new Range(startDate, endDate))) {
                lastRange = new Range(startDate, endDate);
                if (trackDisplay != null) {
                    trackDisplay.setSelectedRange(startDate, endDate);
                }
            }
            // set the position of the marker at the animation time
            double aniDate = ((aniValue != null)
                    && (aniValue instanceof Real))
                    ? ((Real) aniValue).getValue(dataTimeUnit)
                    : endDate;
            DataTimeRange dtr = getDataTimeRange();
            if(dtr != null && trackDisplay != null && useTrackTimes){
                dtr.setEndMode(dtr.MODE_ANIMATION);
                trackDisplay.setSelectedRange(startDate, aniDate);
            }


        } catch (Exception e) {
            logException("applyTimeRange", e);
        }
    }


    private void updateIndicator() {
        if (indicator != null) {
            try {
                lastIndicatorPosition = null;
                indicator.setStationModel(getMarkerLayout());
                indicator.setVisible(getMarkerVisible());
                setScaleOnMarker();
                applyTimeRange();
            } catch (Exception exc) {
                logException("Updating indicator", exc);
            }
        }
    }
    private void setScaleOnMarker() throws RemoteException, VisADException {
        setScaleOnMarker(getDisplayScale() * markerScale);
    }

    /**
     *  A utility to set the scale on the marker dislayable
     *
     * @param f The new scale value
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    private void setScaleOnMarker(float f)
            throws RemoteException, VisADException {
        if (indicator != null) {
            indicator.setScale(f);
        }
    }
    /**
     * Initialize the display unit
     */
    protected void initDisplayUnit() {
        if (getDisplayUnit() == null) {
            setDisplayUnit(getDefaultDistanceUnit());
        }
    }

    /**
     * Signal base class to add this as a display listener
     *
     * @return Add as display listener
     */

    protected boolean shouldAddDisplayListener() {
        return true;
    }




    /**
     * Signal base class to add this as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }

    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {

        super.timeChanged(time);
    }



    /**
     * Toggle the visibility for vector graphics rendering
     *
     * @param rasterMode  the toggle mode
     *
     * @throws Exception  problem toggling
     */
    public void toggleVisibilityForVectorGraphicsRendering(int rasterMode)
            throws Exception {
        if (rasterMode == RASTERMODE_SHOWRASTER) {
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                glyph.oldVisibility = glyph.isVisible();
                if (glyph.oldVisibility) {
                    glyph.setVisible(glyph.getIsRaster());
                }
            }
        } else if (rasterMode == RASTERMODE_SHOWNONRASTER) {
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                if (glyph.oldVisibility) {
                    glyph.setVisible( !glyph.getIsRaster());
                }
            }

        } else {
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                glyph.setVisible(glyph.oldVisibility);
            }
        }
    }


    /**
     * Should this glyph be visible
     *
     * @param tg glyph
     *
     * @return should be visible
     */
    public boolean shouldBeVisible(DrawingGlyph tg) {
        return getDisplayVisibility();
    }


    /**
     *  TUrn on the visibility of this display
     *
     * @param on Visible?
     */
    public void setDisplayVisibility(boolean on) {
        super.setDisplayVisibility(on);
        for (int i = 0; i < glyphs.size(); i++) {
            DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
            glyph.checkVisibility();
        }


    }


    /**
     * Add the glyph into the list of glyphs
     *
     * @param glyph The glyph to add
     *
     * @return Was this successful
     */
    protected boolean addGlyph(DrawingGlyph glyph) {
        try {
            //Add the glyphs displayable first so the initFinal can access the
            //DisplayMaster if needed
            Displayable displayable = glyph.getDisplayable();
            displayable.setUseTimesInAnimation(getUseTimesInAnimation());
            tdc.displayHolder.addDisplayable(displayable);
            if ( !glyph.initFinal()) {
                return false;
            }
            glyphs.add(glyph);

        } catch (Exception e) {
            logException("Adding glyph", e);
            return false;
        }
        return true;
    }


    /**
     * Remove this DisplayControl from the system.  Nulls out any
     * objects for garbage collection
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void doRemove() throws VisADException, RemoteException {
        clearCursor();
        if (glyphs != null) {
            for (int i = 0; i < glyphs.size(); i++) {
                ((DrawingGlyph) glyphs.get(i)).setBeenRemoved(true);
            }
        }
        glyphs         = null;
        selectedGlyphs = null;
        tdc.displayHolder  = null;
        super.doRemove();
    }


    /**
     * Overwrite the legend labels method to use the editor title if there is one.
     *
     * @param labels List of labels
     * @param legendType Side or bottom
     */
    protected void getLegendLabels(List labels, int legendType) {
        if ((editorTitle != null) && (editorTitle.length() > 0)) {
            labels.add(editorTitle);
        } else {
            super.getLegendLabels(labels, legendType);
        }
    }


    /**
     * Create a glyph
     *
     * @param event The event
     * @param mousePress Is mouse pressed or released
     *
     * @return The glyph or null
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected DrawingGlyph createGlyph(DisplayEvent event, boolean mousePress)
            throws VisADException, RemoteException {

        DrawingGlyph glyph = null;

        if (currentCmd instanceof GlyphCreatorCommand) {
            GlyphCreatorCommand gcc = (GlyphCreatorCommand) currentCmd;
            if (gcc.getNeedsMouse() && !mousePress) {
                return null;
            }
            if ( !gcc.getNeedsMouse() && mousePress) {
                return null;
            }
            glyph = gcc.createGlyph(tdc, event);
        }

        if (glyph == null) {
            return null;
        }

        String name = glyph.getName();
        if ((name == null) || (name.length() == 0)) {
            name = getGlyphNameDefault(glyph) + " " + (glyphs.size() + 1);
            glyph.setName(name);
        }

        if ( !glyph.initFromUser(tdc, event)) {
            return null;
        }
        if ( !addGlyph(glyph)) {
            return null;
        }

        setCurrentGlyph(glyph, glyph.handleCreation(event));
        if (currentGlyph != null) {
            if (currentGlyph instanceof TextGlyph) {
                setCursor(Cursor.TEXT_CURSOR);
            } else {
                setCursor(Cursor.HAND_CURSOR);
            }
        }
        return glyph;
    }



    /**
     * Set the current glyph
     *
     *
     * @param glyphWeHad This is the one we had
     * @param glyph The glyph
     */
    private void setCurrentGlyph(DrawingGlyph glyphWeHad,
                                 DrawingGlyph glyph) {

        try {
            if ((glyphWeHad != null) && (glyphWeHad != glyph)
                    && glyphWeHad.getBeingCreated()) {
                try {
                    tdc.setSelection(glyphWeHad);
                } catch (Exception exc) {}
            }


            currentGlyph = glyph;
            if (currentGlyph == null) {
                setCursor();
            } else {
                if (Misc.equals(currentCmd, tdc.CMD_STRETCH)
                        && (currentGlyph instanceof ImageGlyph)) {
                    tdc.msgLabel.setText(
                            "Drag: reshape; Drag/Ctrl: unconstrained");

                }
            }
        } catch (Exception e) {
            logException("Setting current drawing glyph", e);
        }
    }



    /**
     * Respond to control changed events from the view manager
     */
    public void viewpointChanged() {
        super.viewpointChanged();
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        //        boolean wasActive = navDisplay.ensureInactive();
        try {
            long t1 = System.currentTimeMillis();
            if (glyphs != null) {
                for (int i = 0; i < glyphs.size(); i++) {
                    ((DrawingGlyph) glyphs.get(i)).viewpointChanged();
                }
            }
            //            navDisplay.setActive(wasActive);
            long t2 = System.currentTimeMillis();
            //            System.err.println ("time:" + (t2-t1)+ " #:" + glyphs.size());
        } catch (Exception e) {
            logException("Handling projection change event", e);
        }
    }

    /**
     * Respond to the projection changing event
     */
    public void projectionChanged() {
        super.projectionChanged();
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        //        boolean wasActive = navDisplay.ensureInactive();
        try {
            if (glyphs != null) {
                for (int i = 0; i < glyphs.size(); i++) {
                    ((DrawingGlyph) glyphs.get(i)).projectionChanged();
                }
            }
            //                navDisplay.setActive(wasActive);
        } catch (Exception e) {
            logException("Handling projection change event", e);
        }
    }



    /**
     * Should we handle display events
     *
     * @return Ok to handle events
     */
    protected boolean canHandleEvents() {
        if (displayOnly || !tdc.getEnabled() || !editable
                || !getHaveInitialized()
                || (getMakeWindow() && !getWindowVisible())) {
            return false;
        }
        return isGuiShown();
    }


    /**
     * Listen for DisplayEvents
     *
     * @param event The event
     */
    public void handleDisplayChanged(DisplayEvent event) {

        int id = event.getId();
        if ((id == DisplayEvent.MOUSE_MOVED) && (currentGlyph == null)) {
            return;
        }
        InputEvent inputEvent = event.getInputEvent();
        if ( !canHandleEvents()) {
            return;
        }

        try {
            //            System.err.println ("event:" + displayEventName(id));
            if (id == DisplayEvent.KEY_PRESSED) {
                if ((inputEvent instanceof KeyEvent)) {
                    KeyEvent keyEvent = (KeyEvent) inputEvent;

                }

                if (currentGlyph != null) {
                    setCurrentGlyph(currentGlyph,
                            currentGlyph.handleKeyPressed(event));
                }
            }



            //Don't respond to the event if the shift or control key
            //is pressed. These are used for zooming/panning/etc
            //            if ((event.getModifiers() & event.SHIFT_MASK) != 0
            //                    || (event.getModifiers() & event.CTRL_MASK) != 0) {


            setCursor();
            if (id == DisplayEvent.MOUSE_PRESSED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }

                DrawingGlyph closestGlyph = closestGlyph(event);
                if (closestGlyph != null) {
                    if (closestGlyph.mousePressed(event)) {
                        skipNextMouseReleased = true;
                        return;
                    }
                }



                if (currentGlyph == null) {
                    DrawingGlyph glyph = createGlyph(event, true);
                } else {
                    setCurrentGlyph(currentGlyph,
                            currentGlyph.handleMousePressed(event));
                }
            } else if (id == DisplayEvent.MOUSE_DRAGGED) {
                if ( !isLeftButtonDown(event)) {
                    return;
                }

                int       x      = (int) event.getX();
                int       y      = (int) event.getY();
                Rectangle bounds = getScreenBounds();
                int       pad    = 5;
                if ((x > bounds.width + pad) || (y > bounds.height + pad)
                        || (x < -pad) || (y < -pad)) {
                    if (autoScrollCnt++ > 10) {
                        EarthLocation el = screenToEarth(x, y);
                        getNavigatedDisplay().center(el, false);
                        autoScrollCnt = 0;
                    }
                }

                if (currentGlyph == null) {
                    return;
                }
                Real distance = currentGlyph.getDistance();


                setCurrentGlyph(currentGlyph,
                        currentGlyph.handleMouseDragged(event));
            } else if (id == DisplayEvent.MOUSE_RELEASED) {
                if (skipNextMouseReleased) {
                    skipNextMouseReleased = false;
                    return;
                }
                if ( !isLeftButtonDown(event)) {
                    return;
                }
                //DrawingGlyph closestGlyph = closestGlyph(event);
                if (currentGlyph == null) {
                    createGlyph(event, false);
                } else {
                    if ( !currentGlyph.isValid()) {
                        removeGlyph(currentGlyph);
                        setCurrentGlyph(currentGlyph, null);
                    } else {
                        DrawingGlyph glyphNow = currentGlyph;
                        tdc.setSelection(currentGlyph);
                        DrawingGlyph nextGlyph =
                                currentGlyph.handleMouseReleased(event);
                        setCurrentGlyph(currentGlyph, nextGlyph);
                        if (nextGlyph == null) {
                            //&& (currentCmd.equals(CMD_MOVE)
                            //|| currentCmd.equals(CMD_STRETCH))) {
                            tdc.doneMovingGlyph(glyphNow);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logException("Handling display event changed", e);
        }
    }



    /**
     * Find the glyph that is closest to the x/y point of the given event
     *
     * @param event The event
     *
     * @return The closest glyph (or null if none close).
     */
    public DrawingGlyph closestGlyph(DisplayEvent event) {
        return closestGlyph(event, false);
    }

    /**
     * Find the glyph that is closest to the x/y point of the given event
     *
     * @param event The event
     * @param forMove Are we looking for a glyph to move
     *
     * @return The closest glyph (or null if none close).
     */

    public DrawingGlyph closestGlyph(DisplayEvent event, boolean forMove) {
        try {
            DrawingGlyph closestGlyph = null;
            VisADRay ray = getNavigatedDisplay().getRay(event.getX(),
                    event.getY());
            double[] box1     = { 0, 0 };
            double[] box2     = { 100, 0 };

            double[] location = toBox(event);
            double[] vector =
                    getNavigatedDisplay().getRayDirection(event.getX(),
                            event.getY());
            Component comp =
                    tdc.displayHolder.getDisplayMaster().getDisplayComponent();
            Rectangle bounds = comp.getBounds();
            double[]  ul     = screenToBox(0, 0);
            double[] lr = screenToBox((int) bounds.getWidth(),
                    (int) bounds.getHeight());
            double diagonal    = DrawingGlyph.distanceBetween(ul, lr);
            double minDistance = diagonal * 0.05;

            //            System.err.println("Loc:" + location[0] + " " + location[1] + " " + location[2]);
            //            System.err.println("Vec:" + vector[0] + " " + vector[1] + " " + vector[2]);
            for (int i = 0; i < glyphs.size(); i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                if ( !glyph.isVisible() || !glyph.isSelectable()) {
                    continue;
                }
                if (forMove && !glyph.getEditable()) {
                    continue;
                }
                double tmpDistance = glyph.distance(location, vector);
                if (tmpDistance < minDistance) {
                    minDistance  = tmpDistance;
                    closestGlyph = glyph;
                }
            }
            return closestGlyph;
        } catch (Exception exc) {
            logException("Closest glyph", exc);
            return null;
        }
    }


    /**
     * Remove the glyph from the drawing
     *
     * @param glyph The glyph to remove
     */
    public void removeGlyph(DrawingGlyph glyph) {
        glyph.setBeenRemoved(true);
        glyphs.remove(glyph);
        selectedGlyphs.remove(glyph);

        try {
            tdc.displayHolder.removeDisplayable(glyph.getDisplayable());
        } catch (Exception exc) {
            logException("Removing glyph", exc);
        }
    }





    /**
     * Is the given glyph selected
     *
     * @param glyph The glyph
     *
     * @return Is selected
     */
    public boolean isSelected(DrawingGlyph glyph) {
        return selectedGlyphs.contains(glyph);
    }

    /**
     * Remove the glyphs in the selection set
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected void doCut() throws VisADException, RemoteException {
        setDisplayInactive();
        List tmp       = new ArrayList(selectedGlyphs);
        List cutGlyphs = new ArrayList();

        for (int i = 0; i < tmp.size(); i++) {
            DrawingGlyph g = (DrawingGlyph) tmp.get(i);
            if ( !g.isVisible()) {
                continue;
            }
            removeGlyph(g);
            cutGlyphs.add(g);
        }


        setDisplayActive();
    }




    /**
     * Show the properties dialog for the glyphs in the list
     *
     * @param glyphs Glyphs to set properties on
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    public void doProperties(final List glyphs)
            throws VisADException, RemoteException {
        if ((glyphs.size() > 0) && (glyphs.size() < 4)) {
            Misc.runInABit(1, new Runnable() {
                public void run() {
                    doPropertiesInner(glyphs);
                }
            });
        }
    }

    /**
     * Really set the properties on the glyphs
     *
     * @param glyphs The glyphs
     */
    private void doPropertiesInner(List glyphs) {
        try {
            for (int i = 0; i < glyphs.size(); i++) {
                ((DrawingGlyph) glyphs.get(i)).setProperties();
            }
        } catch (Exception exc) {
            logException("Do properties", exc);
        }
    }


    /**
     * Method called by other classes that share the the state.
     * @param from  other class.
     * @param dataId  type of sharing
     * @param data  Array of data being shared.  In this case, the first
     *              (and only?) object in the array is the level
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        super.receiveShareData(from, dataId, data);
    }


    /**
     * Set the current active command
     *
     * @param command The command
     */
    protected void setCurrentCommand(DrawingCommand command) {


        if (straightCbx != null) {
            straightCbx.setEnabled(command.isCapable(FLAG_STRAIGHT));
        }

        setCurrentGlyph(currentGlyph, null);
        currentCmd = command;
        setCursor();

    }

    /**
     * Set the cursor depending on the current command.
     */
    private void setCursor() {
        if ((enabledCbx == null) || (currentCmd == null)) {
            return;
        }
        if ( !enabledCbx.isSelected() || !canHandleEvents()) {
            clearCursor();
            return;
        }
       // if (currentCmd.equals(tdc.CMD_MOVE)) {
            //            setCursor(Cursor.MOVE_CURSOR);
       //     setCursor(Cursor.HAND_CURSOR);
       // } else

        if (currentCmd.equals(CMD_REMOVE)) {
            removeAllGlyphs();

        } else {
            clearCursor();
        }

    }
    protected String getColorParamName() {

        return paramName;
    }
    void createTrajectoryControl() throws VisADException, RemoteException, Exception {
      //  FieldImpl u = this.dataChoice;
     //   super.init(dataChoice0);
        DerivedDataChoice ddc = (DerivedDataChoice)dataChoice;
        Hashtable choices = ddc.getUserSelectedChoices();
        DirectDataChoice udc = (DirectDataChoice)choices.get(new String("D1"));
        DirectDataChoice vdc = (DirectDataChoice)choices.get(new String("D2"));
        DirectDataChoice wdc = (DirectDataChoice)choices.get(new String("D3"));
        DirectDataChoice sdc = (DirectDataChoice)choices.get(new String("scaler"));
        FieldImpl u = (FieldImpl)udc.getData(null);
        FieldImpl v = (FieldImpl)vdc.getData(null);
        FieldImpl pw = (FieldImpl)wdc.getData(null);
        FieldImpl s = (FieldImpl)sdc.getData(null);

        final Unit rgUnit =
                ((FlatField) pw.getSample(0)).getRangeUnits()[0][0];
        FieldImpl w;
        if (Unit.canConvert(rgUnit, CommonUnits.METERS_PER_SECOND)) {
            w = pw;
        } else {
            FieldImpl pFI =
                    DerivedGridFactory.createPressureGridFromDomain((FlatField) pw.getSample(0));
            FieldImpl hPI = DerivedGridFactory.convertPressureToHeight(pFI);
            w = DerivedGridFactory.convertPressureVelocityToHeightVelocity(pw, hPI, null);
        }
        float[][] geoVals = getEarthLocationPoints();
        int numPoints = glyphs.size();
        final Set timeSet  = s.getDomainSet();
        int       numTimes = timeSet.getLength();
        Unit      timeUnit = timeSet.getSetUnits()[0];
        final Unit paramUnit =
                ((FlatField) s.getSample(0)).getRangeUnits()[0][0];
        FunctionType rt =
                (FunctionType) ((FlatField) s.getSample(0)).getType();
        final String paramName =
                rt.getFlatRange().getRealComponents()[0].getName();

        double[]   timeVals = timeSet.getDoubles()[0];

        SampledSet domain0  = GridUtil.getSpatialDomain(s);

        double[] ttts         = timeSet.getDoubles()[0];
        boolean  normalizeLon = true;

        boolean  isLatLon     = GridUtil.isLatLonOrder(domain0);
        int      latIndex     = isLatLon
                ? 0
                : 1;
        int      lonIndex     = isLatLon
                ? 1
                : 0;
        //first step  init  u,v, w, and s at all initial points
        List<DerivedGridFactory.TrajInfo> tj =
                DerivedGridFactory.calculateTrackPoints(u, v, w, s, ttts,
                geoVals, numPoints, numTimes, latIndex,
                lonIndex, true, normalizeLon);

        int numParcels = numPoints;  //10;
        final FunctionType ft = new FunctionType(
                RealType.Generic,
                new FunctionType(
                        RealTupleType.SpatialEarth3DTuple,
                        RealType.getRealType(paramName)));

        List tracks;
        tracks = DerivedGridFactory.createTracks(paramName, tj, timeSet, ft, paramUnit,
                numParcels);
        FlatField mergedTracks = DerivedGridFactory.mergeTracks(tracks);

        FunctionType fiType = new FunctionType(RealType.Time,
                mergedTracks.getType());

        DateTime endTime = new DateTime(timeVals[numTimes - 1], timeUnit);

        FieldImpl fi =
                new FieldImpl(fiType,
                        new SingletonSet(new RealTuple(new Real[] {
                                endTime })));
        fi.setSample(0, mergedTracks, false);

        //super.init(fi)

        setData(fi);
        Set[] rset = mergedTracks.getRangeSets();
         FloatSet ds =  (FloatSet)rset[0];

        SetType st = (SetType)ds.getType();
         RealTupleType rtt = st.getDomain() ;

        RealType rt0 = (RealType)rtt.getRealComponents()[0] ;
        selectRangeDisplay.setSelectRealType( rt0);
        super.paramName = paramName;
        controlPane.setVisible(true);
        controlPane.add(super.doMakeContents());
    }

    protected boolean setData(FieldImpl fi)
            throws VisADException, RemoteException {
        if (trackDisplay == null) {
            return true;
        }

        Unit newUnit = getDisplayUnit();
        //TODO: use the right index
        if ((newUnit != null) && !newUnit.equals(getDisplayUnit())
                && Unit.canConvert(newUnit, getRawDataUnit())) {
            trackDisplay.setDisplayUnit(newUnit);
            selectRangeDisplay.setDisplayUnit(newUnit);


        }

        FlatField ff   = null;
        FieldImpl grid = null;

        if (trackDisplay != null) {
            trackDisplay.setData(DUMMY_DATA);
            indicator.setVisible(false);
            timesHolder.setData(DUMMY_DATA);
        }


        if (indicator != null) {
            indicator.setVisible(getMarkerVisible());
        }


        trackDisplay.setTrack(fi);

        setTrackTimes();
        applyTimeRange();

        return true;
    }


    private void setTrackTimes() throws VisADException, RemoteException {
        if (  (trackDisplay == null)) {
            return;
        }
        Data d = trackDisplay.getData();
        if (d.equals(DUMMY_DATA)) {
            return;
        }
        if ( !getUseTrackTimes()) {
            timesHolder.setData(DUMMY_DATA);
            return;
        }
        FlatField f ;
        try {
            f = (FlatField)((FieldImpl) d).getSample(0);
        } catch (ClassCastException e) {
            f = (FlatField)d;
        }

        //System.out.println(f.getType());
        double[][] samples  = f.getValues(false);
        int        numTimes = samples[1].length;
        if ( !getTimeDeclutterEnabled()) {
            if ( !getAskedUserToDeclutterTime() && (numTimes > 1000)) {
                int success =
                        GuiUtils
                                .showYesNoCancelDialog(getWindow(), "<html>There are "
                                        + numTimes
                                        + " time steps in the data.<br>Do you want to show them all?</html>", "Time Declutter", GuiUtils
                                        .CMD_NO);
                if (success == JOptionPane.CANCEL_OPTION) {
                    return;
                } else {
                    setAskedUserToDeclutterTime(true);
                    setTimeDeclutterEnabled(success == JOptionPane.NO_OPTION);
                }
            }
        }

        double[] times = samples[1];
        if ( !Util.isStrictlySorted(times)) {
            int[] indexes = Util.strictlySortedIndexes(times, true);
            times = Util.take(times, indexes);

        }
        if (getTimeDeclutterEnabled()) {
            LogUtil.message("Track display: subsetting times");
            Trace.call1("declutterTime");
            times = declutterTime(times);
            Trace.call2("declutterTime");
            LogUtil.message("");
        }
        Unit[] units = f.getDefaultRangeUnits();
        Gridded1DDoubleSet timeSet =
                new Gridded1DDoubleSet(RealTupleType.Time1DTuple, new double[][] {
                        times
                }, times.length, (CoordinateSystem) null, new Unit[] { units[1] },
                        (ErrorEstimate[]) null, false /*don't copy*/);
        if (timeSet != null) {
            timesHolder.setData(timeSet);
        }
    }

    private double[] declutterTime(double[] times)
            throws VisADException, RemoteException {
        int numTimes = times.length;
        int seconds  = (int) (timeDeclutterMinutes * 60);
        if (seconds == 0) {
            seconds = 1;
        }
        double[]  tmpTimes = new double[times.length];
        int       numFound = 0;
        Hashtable seenTime = new Hashtable();
        for (int timeIdx = 0; timeIdx < numTimes; timeIdx++) {
            Integer timeKey = new Integer((int) (times[timeIdx] / seconds));
            if ((timeIdx < numTimes - 1) && (seenTime.get(timeKey) != null)) {
                continue;
            }
            seenTime.put(timeKey, timeKey);
            tmpTimes[numFound++] = times[timeIdx];
        }
        double[] newTimes = new double[numFound];
        System.arraycopy(tmpTimes, 0, newTimes, 0, numFound);
        return newTimes;
    }


    public float[][] getEarthLocationPoints()
            throws Exception {

        int pointNum =glyphs.size();

        float[][]     points   = new float[3][pointNum];
        double clevel = 0;
        if(currentLevel instanceof Real)
            clevel = ((Real)currentLevel).getValue();
        else if(currentLevel instanceof TwoFacedObject) {
            Object oj = ((TwoFacedObject) currentLevel).getId();
            clevel = ((Real)oj).getValue();
        }

        if(pressToHeightCS == null)
            pressToHeightCS = DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE);
        double [][] hVals = pressToHeightCS.toReference(new double[][] {new double[] {clevel}}, new Unit[] {zunit}) ;

        float z = (float)hVals[0][0];
        for(int i = 0; i< pointNum; i++){
            DrawingGlyph glyph = (DrawingGlyph)glyphs.get(i);
            points[0][i] = glyph.getLatLons()[0][0];
            points[1][i] = (float)LatLonPointImpl.lonNormal(glyph.getLatLons()[1][0]);
            points[2][i] = z;
        }
        return points;

    }


    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */

    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.add("Controls", GuiUtils.top(doMakeControlsPanel()));

        return GuiUtils.centerBottom(tabbedPane, tdc.msgLabel);
    }


    /**
     * Make the main tabbed pane
     *
     * @return Controls panel
     */
    protected JComponent doMakeControlsPanel() {
        controlPane = new JPanel();
        controlPane.setPreferredSize(new Dimension(300, 180));
        JComponent controlHolder = GuiUtils.topCenter(new JLabel("Result:"),
                controlPane);


        List widgets = new ArrayList();
        addControlWidgets(widgets);
        GuiUtils.tmpInsets = new Insets(4, 4, 0, 4);
        JPanel comps = GuiUtils.doLayout(widgets, 2, GuiUtils.WT_NY,
                GuiUtils.WT_N);


        return GuiUtils.top(GuiUtils.topCenter(comps, controlPane));
    }

    /**
     * Add the widgets into the controls panel
     *
     * @param widgets List to add to. Add in pairs (label, widget)
     */
    protected void addControlWidgets(List widgets) {
        JPanel levelUpDown = GuiUtils.doLayout(new Component[] {
                levelUpBtn,
                levelDownBtn }, 1, GuiUtils.WT_N,
                GuiUtils.WT_N);
        JPanel levelSelector = GuiUtils.doLayout(new Component[] {
                levelBox,
                levelUpDown }, 2, GuiUtils.WT_N,
                GuiUtils.WT_N);

        JComponent widgets0 = GuiUtils.formLayout(new Component[] {
                levelLabel,
                GuiUtils.left(levelSelector)
        });
        JButton unloadBtn =
                GuiUtils.makeImageButton("/auxdata/ui/icons/Cut16.gif", this,
                        "removeAllGlyphs");
        unloadBtn.setToolTipText("Remove all glyphs");


        pointsBtn = new JRadioButton("Points:", isPoints);
        setCurrentCommand(GlyphCreatorCommand.CMD_SYMBOL);
        rectangleBtn  = new JRadioButton(
                "Rectangle:", !isPoints);
        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JRadioButton source = (JRadioButton) e.getSource();
                if (source == pointsBtn) {
                    setCurrentCommand(GlyphCreatorCommand.CMD_SYMBOL);
                } else {
                    setCurrentCommand(GlyphCreatorCommand.CMD_RECTANGLE);
                }
            }
        };
        pointsBtn.addActionListener(listener);
        rectangleBtn.addActionListener(listener);
        GuiUtils.buttonGroup(pointsBtn, rectangleBtn);
        createTrjBtn = new JButton("Create Trajectory");
        createTrjBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionEvent) {
                try{
                    createTrajectoryControl();
                } catch (VisADException ee ) {}
                  catch (RemoteException er) {}
                catch (Exception exr) {}
            }
        });

        JComponent rightComp =
                GuiUtils.vbox(
                        GuiUtils.left( pointsBtn  ),
                        GuiUtils.left( rectangleBtn ),
                        GuiUtils.left( createTrjBtn )
                             );
        JLabel showLabel = GuiUtils.rLabel("Trajectory Initial:");
        showLabel.setVerticalTextPosition(JLabel.TOP);

        widgets.add(GuiUtils.topBottom( widgets0,
                        GuiUtils.leftRight(
                            GuiUtils.top(
                                   GuiUtils.inset( showLabel, new Insets(10, 0, 0, 0))),
                            GuiUtils.centerRight(
                                   GuiUtils.top(rightComp), GuiUtils.right(unloadBtn)))  ) );

    }




    /**
     *  Enable/disable the z slider
     */
    protected void checkZSliderEnabled() {
        boolean posEnabled = ((coordType == DrawingGlyph.COORD_XY)
                || (coordType == DrawingGlyph.COORD_LATLON));
        //Leave this enabled
        //        GuiUtils.enableTree(zPositionPanel, posEnabled);
    }




    /**
     * Should we show the locatio  widgets
     *
     * @return  show the locatio  widgets
     */
    protected boolean showLocationWidgets() {
        return true;
    }




    /**
     * Get the coord type (e.g., LATLON, XYZ)
     *
     * @return The coord type
     */
    public int getCoordType() {
        return coordType;
    }


    /**
     * Get the coord type (e.g., LATLON, XYZ)
     *
     *
     * @param c The coord type to use
     */
    protected void setCoordType(int c) {
        coordType = c;
    }


    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        if ( !editable || displayOnly) {
            super.getEditMenuItems(items, forMenuBar);
            return;
        }

        items.add(GuiUtils.makeMenuItem("Remove All", this,
                "removeAllGlyphs"));


        super.getEditMenuItems(items, forMenuBar);

    }




    /**
     * Apply the current color to all glyphs
     */
    public void applyColorToAll() {
        for (int i = 0; i < selectedGlyphs.size(); i++) {
            ((DrawingGlyph) selectedGlyphs.get(i)).setColor(getColor());
        }
    }





    /**
     * Remove em all.
     */
    public void removeAllGlyphs() {
        try {
            while (glyphs.size() > 0) {
                removeGlyph((DrawingGlyph) glyphs.get(0));
            }
            while (controlPane.getComponentCount() >0 ){
                controlPane.remove(0);
                controlPane.setVisible(false);
                if (trackDisplay != null) {
                    trackDisplay.setData(DUMMY_DATA);
                    indicator.setVisible(false);
                    timesHolder.setData(DUMMY_DATA);
                }
            }
        } catch (Exception exc) {
            logException("Removing drawings", exc);
        }

    }



    /**
     * Set the DrawingGlyphs property.
     *
     * @param value The new value for DrawingGlyphs
     */
    public void setGlyphs(List value) {
        glyphs = value;
    }

    /**
     * Get the Glyphs property.
     *
     * @return The Glyphs
     */
    public List getGlyphs() {
        //If were are display only then don't save the glyphs
        if (displayOnly) {
            return new ArrayList();
        }
        return glyphs;
    }


    /**
     * Clear the cursor in the main display
     */
    private void clearCursor() {
        setCursor(null);
    }

    /**
     * Set the cursor in the main display
     *
     * @param c  The cursor id
     */
    private void setCursor(int c) {
        setCursor(Cursor.getPredefinedCursor(c));
    }

    /**
     * Set the cursor in the main display
     *
     * @param c The cursor
     */
    private void setCursor(Cursor c) {
        getViewManager().setCursorInDisplay(c);
    }



    /**
     * Set the GlyphJython property.
     *
     * @param value The new value for GlyphJython
     */
    public void setGlyphJython(String value) {
        glyphJython = value;
    }

    /**
     * Get the GlyphJython property.
     *
     * @return The GlyphJython
     */
    public String getGlyphJython() {
        return glyphJython;
    }


    /**
     * Get the default name to use
     *
     *
     * @param glyph the glyph
     * @return The name
     */
    protected String getGlyphNameDefault(DrawingGlyph glyph) {
        return "Glyph";
    }


}
