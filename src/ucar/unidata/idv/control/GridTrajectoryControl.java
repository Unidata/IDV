/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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


import ucar.nc2.units.SimpleUnit;

import ucar.unidata.collab.Sharable;

import ucar.unidata.data.*;


import ucar.unidata.data.gis.MapMaker;
import ucar.unidata.data.grid.DerivedGridFactory;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridTrajectory;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.ControlContext;
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
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.Action;



/**
 * A MetApps Display Control for drawing lines on a navigated
 * display.
 *
 * @author MetApps development team
 * @version $Revision: 1.145 $
 */

public class GridTrajectoryControl extends DrawingControl {


    /** Controls the disabled state */
    protected JCheckBox enabledCbx;

    /** The title we get from the xml */
    private String editorTitle = null;


    /** command */
    public static final DrawingCommand CMD_REMOVE =
        new DrawingCommand("Remove graphic", "remove all shape graphics",
                           "/auxdata/ui/icons/Reshape16.gif");

    /** _more_ */
    public static final String CMD_SETLEVELS = "cmd.setlevels";

    /** _more_ */
    public static final String CMD_createTrj = "cmd.createTrj";

    /** _more_ */
    DataChoice dataChoice;


    /** _more_ */
    private JButton levelUpBtn;

    /** _more_ */
    private JButton levelDownBtn;

    /** _more_ */
    private JComboBox levelBox;

    /** _more_ */
    private JLabel levelLabel;

    /** _more_ */
    protected Object currentLevel;

    /** _more_ */
    protected Object bundleLevel = null;

    /** _more_ */
    protected Object[] currentLevels;

    /** _more_ */
    private boolean levelEnabled = false;

    /** _more_ */
    private Unit zunit;

    /** _more_ */
    private Unit newZunit = CommonUnit.meter;


    /** _more_ */
    private static final Data DUMMY_DATA = new Real(0);

    /** _more_ */
    private Object MUTEX = new Object();

    /** _more_ */
    CoordinateSystem pressToHeightCS;

    /** streamlines button */
    private JRadioButton pointsBtn;

    /** streamlines button */
    private JRadioButton hiddenBtn;

    /** vector/barb button */
    private JRadioButton rectangleBtn;

    /** streamlines button */
    private JRadioButton closePolygonBtn;

    /** flag for streamlines */
    boolean isPoints = true;

    /** flag for streamlines */
    boolean isRectangle = false;

    /** _more_ */
    boolean isSelector = false;

    /** flag for streamlines */
    boolean isClosePlgn = true;

    /** _more_ */
    private JButton createTrjBtn;

    /** _more_ */
    boolean createTrjBtnClicked = false;

    /** _more_ */
    JPanel controlPane;

    /** _more_ */
    private MyTrackControl gridTrackControl;

    /** _more_ */
    FieldImpl u;

    /** _more_ */
    FieldImpl v;

    /** _more_ */
    FieldImpl pw;

    /** _more_ */
    FieldImpl s;


    /** _more_ */
    int trackLineWidth = 1;

    /** _more_ */
    private DataTimeRange trjDataTimeRange;

    /** _more_ */
    ColorTable trjColorTable;

    /** _more_ */
    Range trjColorRange;

    /** _more_ */
    Range bundleColorRange = null;

    /** _more_ */
    boolean is2DTraj = false;

    /** _more_ */
    boolean is2DDC = false;

    /** a component to change the skip */
    ValueSliderWidget skipFactorWidget;

    /** _more_ */
    int coordinateType = DrawingGlyph.COORD_LATLON;;

    /** _more_ */
    Unit newUnit = null;

    /** _more_ */
    boolean isBundle = false;

    /** _more_ */
    private JCheckBox backwardCbx;

    /** _more_ */
    private boolean backwardTrajectory = false;

    /**
     * Create a new Drawing Control; set attributes.
     */
    public GridTrajectoryControl() {
        //setCoordType(DrawingGlyph.COORD_LATLON);
        setLineWidth(2);
        reallySetAttributeFlags(FLAG_COLORTABLE | FLAG_DISPLAYUNIT
                                | FLAG_GRIDTRAJECTORY);
    }


    /**
     * Class MyRadarSweepControl _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class MyTrackControl extends TrackControl {

        /** _more_ */
        private float markerScale = 1.0f;

        /** _more_ */
        private Range lastRange;

        /** _more_ */
        int trackWidth;

        /** _more_ */
        GridTrajectoryControl gtc = null;

        /** _more_ */
        FieldImpl trackGrid;

        /**
         * _more_
         *
         * @param gtc _more_
         */
        public MyTrackControl(GridTrajectoryControl gtc) {
            setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                              | FLAG_GRIDTRAJECTORY | FLAG_TIMERANGE
                              | FLAG_SELECTRANGE);
            this.gtc = gtc;
            setUseTrackTimes(true);
            setTimeDeclutterEnabled(true);
        }

        /**
         * Construct a MyTrackControl
         */
        public MyTrackControl() {
            setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                              | FLAG_GRIDTRAJECTORY | FLAG_TIMERANGE
                              | FLAG_SELECTRANGE);
            setUseTrackTimes(true);
            setTimeDeclutterEnabled(true);
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getHaveInitialized() {
            return true;
        }



        /**
         * Get the track width property.  Used by persistence
         * @return  width
         */
        protected int getTrackWidth() {
            if (gtc.createTrjBtnClicked) {
                return gtc.getTrackLineWidth();  // trackWidth;
            } else {
                return trackWidth;
            }
        }


        /**
         * _more_
         *
         * @param width _more_
         */
        public void setLineWidth(int width) {
            super.setLineWidth(width);
            if (gtc != null) {
                gtc.setTrackLineWidth(width);
            }
        }



        /**
         * _more_
         *
         * @param range _more_
         */
        public void setDataTimeRange(DataTimeRange range) {
            ///if(range == null && gtc!= null)
            //    range = gtc.getTrjDataTimeRange();
            if (range != super.getDataTimeRange()) {
                super.setDataTimeRange(range);
            }
            if ((gtc != null) && (range != gtc.getTrjDataTimeRange())) {
                range.setStartOffsetMinutes(range.getStartOffsetMinutes());
                gtc.setTrjDataTimeRange(range);
            }

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public DataTimeRange getDataTimeRange() {
            if ((gtc != null) && (gtc.getTrjDataTimeRange() != null)) {
                return gtc.getTrjDataTimeRange();
            } else {
                return super.getDataTimeRange();
            }


        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getShowInLegend() {
            return false;
        }


        /**
         * _more_
         *
         * @param newColorTable _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void setColorTable(ColorTable newColorTable)
                throws RemoteException, VisADException {

            if (newColorTable != super.getColorTable()) {
                super.setColorTable(newColorTable);
            }
            if ((gtc != null) && (newColorTable != gtc.getTrjColorTable())) {

                gtc.setTrjColorTable(newColorTable);
            }
        }


        /**
         * Get the {@link ucar.unidata.util.ColorTable} property.
         *
         * @return The ColorTable
         */
        public ColorTable getColorTable() {


            if ((gtc != null) && (gtc.getTrjColorTable() != null)) {
                return gtc.getTrjColorTable();
            } else {
                return super.getColorTable();
            }
        }

        /**
         * _more_
         *
         * @param nRange _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void setRange(Range nRange)
                throws RemoteException, VisADException {
            super.setRange(nRange);
            if ((gtc != null) && (nRange != gtc.getTrjColorRange())) {
                gtc.setTrjColorRange(nRange);
                // gtc.bundleColorRange = null;
            }
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public Range getRange() throws RemoteException, VisADException {
            if ((gtc != null) && (gtc.getTrjColorRange() != null)) {
                return gtc.getTrjColorRange();
            } else {
                return super.getRange();
            }
        }

        /**
         * _more_
         *
         * @param fi _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        protected boolean setData(FieldImpl fi)
                throws VisADException, RemoteException {
            if (trackDisplay == null) {
                return true;
            }
            trackGrid = fi;
            Unit newUnit = getDisplayUnit();
            setColorUnit(newUnit);
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
            int len = fi.getLength();
            /*  for(int i = 0; i< len; i++) {
            FieldImpl fii = (FieldImpl)fi.getSample(i) ;

            trackDisplay.setTrack(fii);
        }    */
            updateTimeSelectRange();
            ff = (FlatField) fi.getSample(0, false);
            trackDisplay.setTrack(ff);


            setTrackTimes();
            applyTimeRange();

            return true;
        }


        /**
         * _more_
         */
        public void applyTimeRange() {
            try {
                DataTimeRange dataTimeRange = getDataTimeRange();
                if (gtc != null) {
                    gtc.setTrjDataTimeRange(dataTimeRange);
                }
                GridDataInstance gridDataInstance = getGridDataInstance();
                if ((gridDataInstance == null) || (dataTimeRange == null)) {
                    return;
                }
                Unit       dataTimeUnit;
                DateTime[] dts = gridDataInstance.getDateTimes();
                dataTimeUnit = dts[0].getUnit();
                int size = dts.length;
                // Range    r                = getRangeForTimeSelect();
                // RealType dataTimeRealType = Util.getRealType(dataTimeUnit);
                Real      startReal = dts[0].getReal();
                Real      endReal   = dts[size - 1].getReal();


                Animation anime     = getViewAnimation();
                Real      aniValue  = ((anime != null)
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
                dtr.setStartOffsetMinutes(
                    getDataTimeRange().getStartOffsetMinutes());
                if ((dtr != null) && (trackDisplay != null)
                        && getUseTrackTimes()) {
                    dtr.setEndMode(dtr.MODE_ANIMATION);
                    trackDisplay.setSelectedRange(startDate, aniDate);
                }


            } catch (Exception e) {
                logException("applyTimeRange", e);
            }
        }



        /**
         * _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        private boolean trackDataOk() throws VisADException, RemoteException {

            DataInstance dataInstance = getDataInstance();
            if ((dataInstance == null) || !dataInstance.dataOk()) {
                return false;
            }
            return true;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isInitDone() {
            return true;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected boolean haveMultipleFields() {
            return false;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected FieldImpl getTrjGridDataInstance() {
            return trackGrid;
        }

    }

    /**
     * _more_
     *
     * @param time _more_
     */
    protected void timeChanged(Real time) {

        gridTrackControl.timeChanged(time);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getTrackLineWidth() {
        return trackLineWidth;
    }

    /**
     * _more_
     *
     * @param width _more_
     */
    public void setTrackLineWidth(int width) {
        trackLineWidth = width;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DataTimeRange getTrjDataTimeRange() {
        return trjDataTimeRange;
    }

    /**
     * _more_
     *
     * @param range _more_
     */
    public void setTrjDataTimeRange(DataTimeRange range) {
        if (range != null) {
            trjDataTimeRange = range;
            if (gridTrackControl != null) {
                gridTrackControl.setDataTimeRange(range);
            }
            super.setDataTimeRange(range);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Range getTrjColorRange() {
        return trjColorRange;
    }

    /**
     * _more_
     *
     * @param crange _more_
     */
    public void setTrjColorRange(Range crange) {
        trjColorRange = crange;
        if (crange != null) {
            trjColorRange = crange;
            if (gridTrackControl != null) {
                try {
                    gridTrackControl.setRange(crange);
                } catch (Exception ee) {}
            }
            try {
                super.setRange(crange);
            } catch (Exception ee) {}
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public ColorTable getTrjColorTable() {
        return trjColorTable;
    }

    /**
     * _more_
     *
     * @param ctable _more_
     */
    public void setTrjColorTable(ColorTable ctable) {
        if (ctable != null) {
            trjColorTable = ctable;
            if (gridTrackControl != null) {
                try {
                    gridTrackControl.setColorTable(ctable);
                } catch (Exception ee) {}
            }
            try {
                super.setColorTable(ctable);
            } catch (Exception ee) {}

        }
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

        isBundle = getIdv().getStateManager().isLoadingXml();
        super.init((DataChoice) null);
        gridTrackControl = new MyTrackControl(this);
        // super.init(dataChoice);
        this.dataChoice = dataChoice;
        DerivedDataChoice ddc        = (DerivedDataChoice) dataChoice;
        List              choices0   = ddc.getChoices();
        DerivedDataChoice ddc0       = (DerivedDataChoice) choices0.get(0);
        Hashtable         choices    = ddc0.getUserSelectedChoices();
        DataInstance      di         = getDataInstance();

        int               numChoices = choices.size();
        if (numChoices == 2) {
            is2DTraj = true;
        }
        DirectDataChoice udc =
            (DirectDataChoice) choices.get(new String("D1"));
        DirectDataChoice vdc =
            (DirectDataChoice) choices.get(new String("D2"));
        DirectDataChoice wdc =
            (DirectDataChoice) choices.get(new String("D3"));
        if (choices0.size() == 1) {
            return false;
        }
        DataChoice sdc;

        sdc = (DataChoice) choices0.get(1);
        addDataChoice(udc);
        addDataChoice(vdc);
        if (wdc != null) {
            addDataChoice(wdc);
        }
        DataSelection dataSelection1 = getDataSelection();
        Object        fromLevel      = dataSelection1.getFromLevel();
        dataSelection1.setLevel(null);
        u = (FieldImpl) udc.getData(dataSelection1);
        v = (FieldImpl) vdc.getData(dataSelection1);
        if (wdc != null) {
            pw = (FieldImpl) wdc.getData(dataSelection1);
        }
        if (sdc == null) {
            return false;
        }
        sdc.setDataSelection(dataSelection1);
        s = (FieldImpl) sdc.getData(null);
        doMakeDataInstance(sdc);


        GridDataInstance gdi = new GridDataInstance(sdc, getDataSelection(),
                                   getRequestProperties());
        setDataInstance(gdi);
        gridTrackControl.controlContext = getControlContext();
        gridTrackControl.updateGridDataInstance(gdi);
        if (getDisplayUnit().equals(getDefaultDistanceUnit())) {
            setDisplayUnit(gdi.getRawUnit(0));
        }
        initDisplayUnit();

        // level widget init
        levelBox = gridTrackControl.doMakeLevelControl(null);
        levelBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                String cmd = event.getActionCommand();
                if (cmd.equals(CMD_SETLEVELS)) {
                    TwoFacedObject select =
                        (TwoFacedObject) ((JComboBox) event.getSource())
                            .getSelectedItem();
                    setLevel(select);
                }
            }
        });
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
            public void actionPerformed(ActionEvent event) {
                moveUpDown(1);
            }
        });

        //        levelLabel = GuiUtils.rLabel("<html><u>L</u>evels:");
        levelLabel = GuiUtils.rLabel(getLevelsLabel());
        levelLabel.setDisplayedMnemonic(GuiUtils.charToKeyCode("L"));
        levelLabel.setLabelFor(levelBox);

        DataSelection tmpSelection = new DataSelection(0);
        //tmpSelection.setFromLevel(null);
        //tmpSelection.setToLevel(null);

        List     levelsList = sdc.getAllLevels(tmpSelection);
        Object[] levels     = null;
        if ((levelsList != null) && (levelsList.size() > 0)) {
            levels =
                (Object[]) levelsList.toArray(new Object[levelsList.size()]);
            SampledSet ss = GridUtil.getSpatialDomain(gdi.getGrid());
            zunit = ss.getSetUnits()[2];
        }


        if (levels == null) {
            GridUtil.getSpatialDomain(gdi.getGrid());
            levels = ((GridDataInstance) getDataInstance()).getLevels();
            zunit  = ((GridDataInstance) getDataInstance()).getZUnit();
        }

        if (currentLevel == null) {
            currentLevel = fromLevel;  //getDataSelection().getFromLevel();
        }
        if ((levels != null) && (levels.length > 0)
                && (currentLevel == null)) {
            currentLevel = levels[0];
        }

        setLevels(levels, fromLevel);

        // the control for the track
        setDisplayActive();

        if ( !gridTrackControl.trackDataOk()) {
            List dlist = new ArrayList();
            dlist.add(sdc);
            gridTrackControl.appendDataChoices(dlist);
            if ( !gridTrackControl.trackDataOk()) {
                return false;
            }
        }
        gridTrackControl.trackDisplay = new TrackDisplayable("track"
                + dataChoice);
        setLineWidth(gridTrackControl.trackWidth);
        addDisplayable(gridTrackControl.trackDisplay, getAttributeFlags());
        gridTrackControl.selectRangeDisplay = new SelectRangeDisplayable();
        addDisplayable(gridTrackControl.selectRangeDisplay, FLAG_SELECTRANGE);
        getViewAnimation();
        gridTrackControl.indicator = new StationModelDisplayable("indicator");
        gridTrackControl.indicator.setScale(gridTrackControl.markerScale);
        gridTrackControl.indicator.setShouldUseAltitude(true);
        gridTrackControl.updateIndicator();
        addDisplayable(gridTrackControl.indicator);
        gridTrackControl.timesHolder = new LineDrawing("track_time"
                + dataChoice);
        gridTrackControl.timesHolder.setManipulable(false);
        gridTrackControl.timesHolder.setVisible(false);
        addDisplayable(gridTrackControl.timesHolder);
        gridTrackControl.addDisplayable(gridTrackControl.trackDisplay,
                                        getAttributeFlags());
        gridTrackControl.addDisplayable(gridTrackControl.selectRangeDisplay,
                                        FLAG_SELECTRANGE);
        gridTrackControl.addDisplayable(gridTrackControl.indicator);
        gridTrackControl.addDisplayable(gridTrackControl.timesHolder);
        // return setData(dataChoice);
        createTrjBtn = new JButton("Create Trajectory");
        createTrjBtn.addActionListener(this);
        createTrjBtn.setActionCommand(CMD_createTrj);
        createTrjBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                String cmd = actionEvent.getActionCommand();
                if (cmd.equals(CMD_createTrj)) {
                    try {
                        createTrjBtnClicked = true;
                        removeGC();
                        createTrajectory();
                        gridTrackControl.setLineWidth(trackLineWidth);
                    } catch (Exception exr) {
                        logException("Click create trajectory button", exr);
                    }
                }


            }
        });

        controlPane = new JPanel();
        controlPane.setPreferredSize(new Dimension(300, 180));

        return true;


    }

    /**
     * _more_
     *
     * @param oldUnit _more_
     * @param newUnit _more_
     */
    protected void displayUnitChanged(Unit oldUnit, Unit newUnit) {
        gridTrackControl.displayUnitChanged(oldUnit, newUnit);
        gridTrackControl.setNewDisplayUnit(newUnit, true);

        try {
            gridTrackControl.setSelectRange(
                gridTrackControl.getColorRangeFromData());
            gridTrackControl.selectRangeDisplay.setDisplayUnit(newUnit);
        } catch (Exception exc) {
            logException("change unit", exc);
        }
        this.setDisplayUnit(newUnit);
    }

    /**
     * _more_
     */
    public void createTrajectory() {
        Misc.run(new Runnable() {
            public void run() {

                try {
                    synchronized (MUTEX) {
                        showWaitCursor();
                        if ( !is2DTraj) {
                            createTrajectoryControl();
                        } else {
                            create2DTrajectoryControl();
                        }
                    }
                } catch (Exception exc) {
                    logException("Calculationing the grid trajectory", exc);
                } finally {
                    showNormalCursor();
                }

            }
        });
    }

    /**
     * _more_
     *
     * @param vc _more_
     * @param properties _more_
     * @param preSelectedDataChoices _more_
     */
    public void initAfterUnPersistence(ControlContext vc,
                                       Hashtable properties,
                                       List preSelectedDataChoices) {

        super.initAfterUnPersistence(vc, properties, preSelectedDataChoices);

        if (createTrjBtnClicked) {
            if ((getGlyphs() != null) && (glyphs.size() > 0)) {
                currentLevel = getCurrentLevel();
                if ((currentLevel != null) && (bundleLevel != null)
                        && !currentLevel.equals(bundleLevel)) {
                    setLevel(bundleLevel);
                    levelBox.setSelectedItem(bundleLevel);
                }
                newUnit = getDisplayUnit();
                createTrjBtn.doClick();
                gridTrackControl.setLineWidth(getTrackLineWidth());
                // gridTrackControl.setDataTimeRange(getTrjDataTimeRange());
                gridTrackControl.getDataTimeRange(true).getTimeModeLabel();

                try {
                    //gridTrackControl.setColorScaleInfo(getColorScaleInfo());
                    gridTrackControl.setColorTable(getTrjColorTable());
                    doMakeColorScales();
                    bundleColorRange = getTrjColorRange();
                } catch (Exception ee) {}

            }
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getHaveInitialized() {
        return true;
    }

    /**
     * current level
     *
     * @param levels _more_
     * @param initLevel _more_
     */

    public void setLevels(Object[] levels, Object initLevel) {
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
        if (initLevel != null) {
            if (initLevel instanceof Real) {
                TwoFacedObject clevel = Util.labeledReal((Real) initLevel);;
                levelBox.setSelectedItem(clevel);
            } else {
                levelBox.setSelectedItem(initLevel);
            }
        }

        setOkToFireEvents(true);
    }

    /**
     * _more_
     *
     * @param r _more_
     */
    public void setLevel(Object r) {
        // if ( !createTrjBtnClicked) {
        currentLevel = r;
        // }
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
        displayHolder  = null;
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
     * Remove the glyph from the drawing
     *
     * @param glyph The glyph to remove
     */
    public void removeGlyph(DrawingGlyph glyph) {
        glyph.setBeenRemoved(true);
        glyphs.remove(glyph);
        selectedGlyphs.remove(glyph);

        try {
            displayHolder.removeDisplayable(glyph.getDisplayable());
        } catch (Exception exc) {
            logException("Removing glyph", exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected String getColorParamName() {

        return paramName;
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    void createTrajectoryControl()
            throws VisADException, RemoteException, Exception {

        Unit dUnit = ((FlatField) s.getSample(0)).getRangeUnits()[0][0];
        gridTrackControl.setDisplayUnit(dUnit);
        final Unit rgUnit =
            ((FlatField) pw.getSample(0)).getRangeUnits()[0][0];
        FieldImpl w;
        if (Unit.canConvert(rgUnit, CommonUnits.METERS_PER_SECOND)) {
            w = pw;
        } else {
            FieldImpl pFI = DerivedGridFactory.createPressureGridFromDomain(
                                (FlatField) pw.getSample(0));
            FieldImpl hPI = DerivedGridFactory.convertPressureToHeight(pFI);
            w = DerivedGridFactory.convertPressureVelocityToHeightVelocity(
                pw, hPI, null);
        }

        final Set timeSet  = s.getDomainSet();
        int       numTimes = timeSet.getLength();
        Unit      timeUnit = timeSet.getSetUnits()[0];
        final Unit paramUnit =
            ((FlatField) s.getSample(0)).getRangeUnits()[0][0];
        FunctionType rt =
            (FunctionType) ((FlatField) s.getSample(0)).getType();
        final String paramName =
            rt.getFlatRange().getRealComponents()[0].getName();

        double[]   timeVals   = timeSet.getDoubles()[0];

        SampledSet domain0    = GridUtil.getSpatialDomain(s);
        SampledSet domain2D   = GridUtil.makeDomain2D((GriddedSet) domain0);
        int        skipFactor = (int) skipFactorWidget.getValue();
        if (skipFactor > 0) {
            SampledSet domain1 = GridUtil.subsetDomain((GriddedSet) domain0,
                                     skipFactor, skipFactor, 1);
            domain2D = GridUtil.makeDomain2D((GriddedSet) domain1);
        }
        double[] ttts         = timeSet.getDoubles()[0];
        boolean  normalizeLon = true;

        boolean  isLatLon     = GridUtil.isLatLonOrder(domain0);
        int      latIndex     = isLatLon
                                ? 0
                                : 1;
        int      lonIndex     = isLatLon
                                ? 1
                                : 0;

        Real     alt          = null;
        // if(zunit.getIdentifier().length() == 0) {
        alt = GridUtil.getAltitude(
            s, (Real) ((TwoFacedObject) currentLevel).getId());
        //}
        float[][] geoVals = getEarthLocationPoints(latIndex, lonIndex,
                                domain2D, alt);
        int numPoints = geoVals[0].length;
        //first step  init  u,v, w, and s at all initial points
        //LogUtil.consoleMessage("Calculation grid trajectory ");
        //LogUtil.message("Calculation grid trajectory...");
        List<GridTrajectory.TrajInfo> tj =
            GridTrajectory.calculateTrackPoints(u, v, w, s, ttts, geoVals,
                numPoints, numTimes, latIndex, lonIndex, true, normalizeLon,
                null, backwardTrajectory);

        int numParcels = numPoints;  //10;
        final FunctionType ft = new FunctionType(
                                    RealType.Generic,
                                    new FunctionType(
                                        RealTupleType.SpatialEarth3DTuple,
                                        RealType.getRealType(paramName)));

        List tracks;

        tracks = GridTrajectory.createTracks(paramName, tj, timeSet, ft,
                                             paramUnit, numParcels);
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

        // gridTrackControl.setLineWidth(gridTrackControl.getTrackWidth());
        // gridTrackControl.setDataTimeRange(gridTrackControl.getDataTimeRange());
        gridTrackControl.setData(fi);
        Range range = gridTrackControl.getGridDataInstance().getRange(
                          gridTrackControl.getColorRangeIndex());  //GridUtil.getMinMax(fi)[0];
        gridTrackControl.setRange(range);
        Set[]         rset = mergedTracks.getRangeSets();
        DoubleSet     ds   = (DoubleSet) rset[0];

        SetType       st   = (SetType) ds.getType();
        RealTupleType rtt  = st.getDomain();

        RealType      rt0  = (RealType) rtt.getRealComponents()[0];
        super.setDataInstance(getDataInstance());
        gridTrackControl.selectRangeDisplay.setSelectRealType(rt0);
        //super.initializationDone = true;
        super.paramName = paramName;
        controlPane.setVisible(true);
        controlPane.add(gridTrackControl.doMakeContents());

        Unit cUnit = getDisplayUnit();

        if (newUnit != null) {
            cUnit = newUnit;
        }

        gridTrackControl.displayUnitChanged(dUnit, cUnit);
        gridTrackControl.setNewDisplayUnit(cUnit, true);
        Range newRange;
        if (isBundle && (bundleColorRange != null)) {
            newRange = bundleColorRange;
        } else {
            newRange = gridTrackControl.getColorRangeFromData();
        }
        isBundle = false;
        gridTrackControl.setSelectRange(newRange);
        gridTrackControl.setRange(newRange);
        // gridTrackControl.getColorRangeFromData());
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    void create2DTrajectoryControl()
            throws VisADException, RemoteException, Exception {

        Unit dUnit = ((FlatField) s.getSample(0)).getRangeUnits()[0][0];
        gridTrackControl.setDisplayUnit(dUnit);


        final Set timeSet  = s.getDomainSet();
        int       numTimes = timeSet.getLength();
        Unit      timeUnit = timeSet.getSetUnits()[0];
        final Unit paramUnit =
            ((FlatField) s.getSample(0)).getRangeUnits()[0][0];
        FunctionType rt =
            (FunctionType) ((FlatField) s.getSample(0)).getType();
        final String paramName =
            rt.getFlatRange().getRealComponents()[0].getName();

        double[]   timeVals   = timeSet.getDoubles()[0];

        SampledSet domain0    = GridUtil.getSpatialDomain(s);
        SampledSet domain2D   = GridUtil.makeDomain2D((GriddedSet) domain0);
        int        skipFactor = (int) skipFactorWidget.getValue();
        if (skipFactor > 0) {
            SampledSet domain1 = GridUtil.subsetDomain((GriddedSet) domain0,
                                     skipFactor, skipFactor, 1);
            domain2D = GridUtil.makeDomain2D((GriddedSet) domain1);
        }
        SampledSet domain1      = GridUtil.getSpatialDomain(u);

        double[]   ttts         = timeSet.getDoubles()[0];
        boolean    normalizeLon = true;

        boolean    isLatLon     = GridUtil.isLatLonOrder(domain0);
        int        latIndex     = isLatLon
                                  ? 0
                                  : 1;
        int        lonIndex     = isLatLon
                                  ? 1
                                  : 0;

        boolean    haveAlt      = true;
        if ((domain0.getManifoldDimension() == 2)
                && (domain1.getManifoldDimension() == 2)) {
            //in case the s is already subset to a specific level in 3D derived 2D trajectory
            is2DDC  = true;
            haveAlt = false;
        }

        Real alt = null;
        // if(zunit.getIdentifier().length() == 0) {
        if ( !is2DDC) {
            alt = GridUtil.getAltitude(
                s, (Real) ((TwoFacedObject) currentLevel).getId());
        }
        float[][] geoVals = getEarthLocationPoints(latIndex, lonIndex,
                                domain2D, alt);
        int  numPoints = geoVals[0].length;
        Real clevel    = null;
        if (currentLevel instanceof Real) {
            clevel = ((Real) currentLevel);
        } else if (currentLevel instanceof TwoFacedObject) {
            clevel = (Real) ((TwoFacedObject) currentLevel).getId();

        }
        /* FieldImpl u1 = GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(u,
                            clevel));
         FieldImpl v1 = GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(v,
                            clevel));
         FieldImpl s1 = GridUtil.make2DGridFromSlice(GridUtil.sliceAtLevel(s,
                            clevel));    */
        //first step  init  u,v, w, and s at all initial points
        List<GridTrajectory.TrajInfo> tj =
            GridTrajectory.calculateTrackPoints(u, v, null, s, ttts, geoVals,
                numPoints, numTimes, latIndex, lonIndex, haveAlt,
                normalizeLon, clevel, backwardTrajectory);

        int numParcels = numPoints;  //10;
        final FunctionType ft = new FunctionType(
                                    RealType.Generic,
                                    new FunctionType(
                                        RealTupleType.SpatialEarth3DTuple,
                                        RealType.getRealType(paramName)));

        List tracks;

        tracks = GridTrajectory.createTracks(paramName, tj, timeSet, ft,
                                             paramUnit, numParcels);
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

        // gridTrackControl.setLineWidth(gridTrackControl.getTrackWidth());
        // gridTrackControl.setDataTimeRange(gridTrackControl.getDataTimeRange());
        gridTrackControl.setData(fi);
        Range range = gridTrackControl.getGridDataInstance().getRange(
                          gridTrackControl.getColorRangeIndex());  //GridUtil.getMinMax(fi)[0];
        gridTrackControl.setRange(range);
        Set[]         rset = mergedTracks.getRangeSets();
        DoubleSet     ds   = (DoubleSet) rset[0];

        SetType       st   = (SetType) ds.getType();
        RealTupleType rtt  = st.getDomain();

        RealType      rt0  = (RealType) rtt.getRealComponents()[0];
        super.setDataInstance(getDataInstance());
        gridTrackControl.selectRangeDisplay.setSelectRealType(rt0);
        //super.initializationDone = true;
        super.paramName = paramName;
        controlPane.setVisible(true);
        controlPane.add(gridTrackControl.doMakeContents());

        Unit cUnit = getDisplayUnit();

        if (newUnit != null) {
            cUnit = newUnit;
        }

        gridTrackControl.displayUnitChanged(dUnit, cUnit);
        gridTrackControl.setNewDisplayUnit(cUnit, true);
        Range newRange;
        if (isBundle && (bundleColorRange != null)) {
            newRange = bundleColorRange;
        } else {
            newRange = gridTrackControl.getColorRangeFromData();
        }
        isBundle = false;
        gridTrackControl.setSelectRange(newRange);
        gridTrackControl.setRange(newRange);

    }

    /**
     * _more_
     *
     *
     * @param latIndex _more_
     * @param lonIndex _more_
     * @param domain0 _more_
     * @param alt _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public float[][] getEarthLocationPoints(int latIndex, int lonIndex,
                                            SampledSet domain0, Real alt)
            throws Exception {

        double clevel = 0;
        if (currentLevel instanceof Real) {
            clevel = ((Real) currentLevel).getValue();
        } else if (currentLevel instanceof TwoFacedObject) {
            Object oj = ((TwoFacedObject) currentLevel).getId();
            clevel = ((Real) oj).getValue();
        }

        if (pressToHeightCS == null) {
            pressToHeightCS =
                DataUtil.getPressureToHeightCS(DataUtil.STD_ATMOSPHERE);
        }

        float z;  //= (float)alt.getValue();

        if (is2DDC) {
            z = (float) clevel;
        } else {
            z = (float) alt.getValue();
        }
        /*
       if ( !is2DDC) {
           double[][] hVals = pressToHeightCS.toReference(new double[][] {
               new double[] { clevel }
           }, new Unit[] { zunit });

           z = (float) hVals[0][0];
       } else {
           z = (float) clevel;
       }     */

        if (currentCmd.getLabel().equals(
                GlyphCreatorCommand.CMD_SYMBOL.getLabel()) || (glyphs.get(0)
                instanceof SymbolGlyph)) {
            int       pointNum = glyphs.size();

            float[][] points   = new float[3][pointNum];

            for (int i = 0; i < pointNum; i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                points[latIndex][i] = glyph.getLatLons()[0][0];
                points[lonIndex][i] = (float) LatLonPointImpl.lonNormal(
                    glyph.getLatLons()[1][0]);
                points[2][i] = z;
            }
            setCurrentCommand(CMD_SELECT);
            hiddenBtn.doClick();;

            return points;
        } else {

            if (glyphs.size() == 0) {
                return null;
            }
            //  Gridded3DSet domain =
            //      gridTrackControl.getGridDataInstance().getDomainSet3D();
            Unit[]   du       = domain0.getSetUnits();
            MapMaker mapMaker = new MapMaker();
            for (DrawingGlyph glyph : (List<DrawingGlyph>) glyphs) {
                float[][] lls = glyph.getLatLons();
                float[][] tmp = glyph.getLatLons();
                if (du[lonIndex].isConvertible(CommonUnit.radian)) {
                    lls[1] = ucar.visad.GeoUtils.normalizeLongitude(lls[1]);

                } else if (du[lonIndex].isConvertible(
                        CommonUnits.KILOMETER)) {
                    for (int i = 0; i < lls[1].length; i++) {
                        lls[1][i] =
                            (float) LatLonPointImpl.lonNormal(lls[1][i]);
                    }
                }
                mapMaker.addMap(lls);
            }

            float[][][] latlons =
                GridUtil.findContainedLatLons((GriddedSet) domain0,
                    mapMaker.getMaps());
            int num = 0;
            for (int i = 0; i < latlons.length; i++) {
                num = num + latlons[i][0].length;
            }

            //int       skipFactor = 0; //(int) skipFactorWidget.getValue();

            //int       onum       = num / (skipFactor + 1);

            float[][] points = new float[3][num];
            int       psize  = 0;
            for (int k = 0; k < latlons.length; k++) {
                int isize = latlons[k][0].length;
                for (int i = 0; i < isize; i++) {
                    points[latIndex][i + psize] = latlons[k][0][i];
                    points[lonIndex][i + psize] =
                        (float) LatLonPointImpl.lonNormal(latlons[k][1][i]);
                    points[2][i + psize] = z;
                }
                psize = psize + isize;
            }
            setCurrentCommand(CMD_SELECT);
            hiddenBtn.doClick();

            return points;
        }

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

        tabbedPane.add("Controls", doMakeControlsPanel());

        return GuiUtils.centerBottom(tabbedPane, new JLabel(""));
    }


    /**
     * Make the main tabbed pane
     *
     * @return Controls panel
     */
    protected JComponent doMakeControlsPanel() {

        List widgets = new ArrayList();
        addControlWidgets(widgets);
        GuiUtils.tmpInsets = new Insets(4, 4, 0, 4);
        JPanel comps = GuiUtils.doLayout(widgets, 2, GuiUtils.WT_NY,
                                         GuiUtils.WT_N);


        return GuiUtils.vbox(comps, controlPane);

        /* test */
    }

    /**
     * Add the widgets into the controls panel
     *
     * @param widgets List to add to. Add in pairs (label, widget)
     */
    protected void addControlWidgets(List widgets) {

        JPanel levelUpDown = GuiUtils.doLayout(new Component[] { levelUpBtn,
                levelDownBtn }, 1, GuiUtils.WT_N, GuiUtils.WT_N);
        JPanel levelSelector = GuiUtils.doLayout(new Component[] { levelBox,
                levelUpDown }, 2, GuiUtils.WT_N, GuiUtils.WT_N);

        JComponent widgets0 = GuiUtils.formLayout(new Component[] {
                                  levelLabel,
                                  GuiUtils.left(levelSelector) });
        JButton unloadBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Cut16.gif", this,
                                     "removeAllGlyphs");
        unloadBtn.setToolTipText("Remove existing glyphs and trajectories");

        msgLabel = new JLabel();
        if (createTrjBtnClicked) {
            isPoints = getIsPoints();
            if (isPoints) {
                setCurrentCommand(GlyphCreatorCommand.CMD_SYMBOL);
            } else if (isRectangle) {
                setCurrentCommand(GlyphCreatorCommand.CMD_RECTANGLE);
            } else if (isClosePlgn) {
                setCurrentCommand(GlyphCreatorCommand.CMD_CLOSEDPOLYGON);
            } else {
                setCurrentCommand(CMD_SELECT);
            }
        } else {
            setCurrentCommand(GlyphCreatorCommand.CMD_SYMBOL);
        }

        skipFactorWidget = new ValueSliderWidget(this, 0, 10, "skipValue",
                getSkipWidgetLabel());
        skipFactorWidget.setEnabled(false);
        addRemovable(skipFactorWidget);

        pointsBtn       = new JRadioButton("Points:", isPoints);
        rectangleBtn    = new JRadioButton("Rectangle:", isRectangle);
        closePolygonBtn = new JRadioButton("ClosePolygon:", isClosePlgn);
        hiddenBtn       = new JRadioButton("ClosePolygon:", isSelector);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JRadioButton source = (JRadioButton) e.getSource();
                coordinateType = DrawingGlyph.COORD_LATLON;
                if (source == pointsBtn) {
                    setCurrentCommand(GlyphCreatorCommand.CMD_SYMBOL);
                    isPoints    = true;
                    isClosePlgn = false;
                    isRectangle = false;
                    isSelector  = false;
                    skipFactorWidget.setEnabled(false);
                    removeAllGlyphs();
                } else if (source == rectangleBtn) {
                    setCurrentCommand(GlyphCreatorCommand.CMD_RECTANGLE);
                    isRectangle = true;
                    isPoints    = false;
                    isClosePlgn = false;
                    isSelector  = false;
                    skipFactorWidget.setEnabled(true);
                    removeAllGlyphs();
                } else if (source == closePolygonBtn) {
                    coordinateType = DrawingGlyph.COORD_XY;
                    setCurrentCommand(GlyphCreatorCommand.CMD_CLOSEDPOLYGON);
                    isRectangle = false;
                    isPoints    = false;
                    isSelector  = false;
                    isClosePlgn = true;
                    skipFactorWidget.setEnabled(true);
                    removeAllGlyphs();
                } else {
                    setCurrentCommand(CMD_SELECT);
                    isSelector  = true;
                    isRectangle = false;
                    isPoints    = false;
                    isClosePlgn = false;
                    skipFactorWidget.setEnabled(false);
                    //removeAllGlyphs();
                }
                setCoordType(coordinateType);
            }
        };
        pointsBtn.addActionListener(listener);
        rectangleBtn.addActionListener(listener);
        closePolygonBtn.addActionListener(listener);
        hiddenBtn.addActionListener(listener);
        GuiUtils.buttonGroup(pointsBtn, rectangleBtn, closePolygonBtn,
                             hiddenBtn);
        //


        JComponent rightComp = GuiUtils.vbox(GuiUtils.left(pointsBtn),
                                             GuiUtils.left(closePolygonBtn),
                                             GuiUtils.left(rectangleBtn));


        backwardCbx = GuiUtils.makeCheckbox("", this, "backwardTrajectory");
        backwardCbx.setToolTipText(
            "display the backward trajectory of air parcel");
        backwardCbx.setSelected(false);

        JLabel showLabel = GuiUtils.rLabel("Trajectory Initial Area:");
        JLabel removeLabel =
            GuiUtils.rLabel("Remove Trajectory Initial Area:");
        showLabel.setVerticalTextPosition(JLabel.TOP);

        widgets.add(GuiUtils
            .topCenterBottom(widgets0, GuiUtils
                .topCenterBottom(GuiUtils
                    .leftRight(GuiUtils
                        .top(GuiUtils
                            .inset(showLabel, new Insets(10, 0, 0, 0))), GuiUtils
                                .top(rightComp)), GuiUtils
                                    .top(GuiUtils
                                        .hbox(GuiUtils
                                            .rLabel("Initial Area Skip Factor:  "), skipFactorWidget
                                            .getContents(false))), GuiUtils
                                                .top(GuiUtils
                                                    .hbox(GuiUtils
                                                        .rLabel("Backward trajectory:"), GuiUtils
                                                            .left(backwardCbx)))), GuiUtils
                                                                .leftRight(GuiUtils
                                                                    .inset(GuiUtils
                                                                        .wrap(createTrjBtn), 2), GuiUtils
                                                                            .right(unloadBtn))));



    }

    /**
     * _more_
     *
     * @param back _more_
     *
     * @throws Exception _more_
     */
    public void setBackwardTrajectory(boolean back) throws Exception {
        backwardTrajectory = back;
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public boolean getBackwardTrajectory() throws Exception {
        return backwardTrajectory;
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
     * Apply the current color to all glyphs
     */
    public void applyColorToAll() {
        for (int i = 0; i < selectedGlyphs.size(); i++) {
            ((DrawingGlyph) selectedGlyphs.get(i)).setColor(getColor());
        }
    }

    /**
     * Set the  skip value
     *
     * @param value skip value
     */

    public void setSkipValue(int value) {
        super.setSkipValue(value);
        if (skipFactorWidget != null) {
            skipFactorWidget.setValue(value);
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
            while (controlPane.getComponentCount() > 0) {
                controlPane.remove(0);
                controlPane.setVisible(false);
                if (gridTrackControl.trackDisplay != null) {
                    gridTrackControl.trackDisplay.setData(DUMMY_DATA);
                    gridTrackControl.indicator.setVisible(false);
                    gridTrackControl.timesHolder.setData(DUMMY_DATA);
                }
                createTrjBtnClicked = false;
            }
        } catch (Exception exc) {
            logException("Removing drawings", exc);
        }

    }

    /**
     * _more_
     */
    public void removeGC() {
        try {

            while (controlPane.getComponentCount() > 0) {
                controlPane.remove(0);
                controlPane.setVisible(false);
                if (gridTrackControl.trackDisplay != null) {
                    gridTrackControl.trackDisplay.setData(DUMMY_DATA);
                    gridTrackControl.indicator.setVisible(false);
                    gridTrackControl.timesHolder.setData(DUMMY_DATA);
                }
                // createTrjBtnClicked = false;
            }
        } catch (Exception exc) {
            logException("Removing drawings", exc);
        }

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
     * _more_
     *
     * @return _more_
     */
    public int getCoordType() {
        return coordinateType;
    }


    /*
   public void setGridTrackControl(MyTrackControl mtc) {
       gridTrackControl = mtc;
   }

   public MyTrackControl getGridTrackControl() {
       return gridTrackControl;
   }     */

    /**
     * _more_
     *
     * @param lvl _more_
     */
    public void setCurrentLevel(Object lvl) {
        currentLevel = lvl;
        bundleLevel  = lvl;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Object getCurrentLevel() {
        return currentLevel;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public DrawingCommand getCurrentCmd() {
        return currentCmd;
    }

    /**
     * _more_
     *
     * @param command _more_
     */
    public void setCurrentCmd(String command) {
        if (command.contains(GlyphCreatorCommand.CMD_RECTANGLE.getLabel())) {
            currentCmd = GlyphCreatorCommand.CMD_RECTANGLE;
        } else if (command.contains(
                GlyphCreatorCommand.CMD_SYMBOL.getLabel())) {
            currentCmd = GlyphCreatorCommand.CMD_SYMBOL;
        } else if (command.contains(
                GlyphCreatorCommand.CMD_CLOSEDPOLYGON.getLabel())) {
            currentCmd = GlyphCreatorCommand.CMD_CLOSEDPOLYGON;
        } else {
            currentCmd = getCurrentCmd();
        }
    }

    /**
     * Get the range to use to apply to displayables
     *
     *
     * @param newInfo _more_
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setColorScaleInfo(ColorScaleInfo newInfo)
            throws VisADException, RemoteException {
        if (newInfo == null) {
            colorScaleInfo = null;
            return;
        }
        if (gridTrackControl != null) {
            gridTrackControl.colorScaleInfo = new ColorScaleInfo(newInfo);
            gridTrackControl.applyColorScaleInfo();
        }
        //applyColorScaleInfo();
        colorScaleInfo = new ColorScaleInfo(newInfo);
        applyColorScaleInfo();
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public Range getRangeToApply() throws RemoteException, VisADException {
        return gridTrackControl.getRange();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected ColorTable getColorTableToApply() {

        return gridTrackControl.getColorTableToApply();
    }


    /**
     * Get the contour information for any contours
     *
     * @return  the contour information
     */
    public ColorScaleInfo getColorScaleInfo() {
        //    this.colorScaleInfo =  gridTrackControl.getColorScaleInfo();

        return this.colorScaleInfo;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getCreateTrjBtnClicked() {
        return createTrjBtnClicked;
    }

    /**
     * _more_
     *
     * @param clicked _more_
     */
    public void setCreateTrjBtnClicked(boolean clicked) {
        createTrjBtnClicked = clicked;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsPoints() {
        return isPoints;
    }

    /**
     * _more_
     *
     * @param point _more_
     */
    public void setIsPoints(boolean point) {
        isPoints = point;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getCoordinateType() {
        return coordinateType;
    }

    /**
     * _more_
     *
     * @param type _more_
     */
    public void setCoordinateType(int type) {
        coordinateType = type;
    }
}
