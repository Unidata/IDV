/*
 * Copyright 1997-2017 Unidata Program Center/University Corporation for
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


import ucar.nc2.dataset.*;
import ucar.nc2.dt.GridCoordSystem;

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

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.unidata.xml.XmlUtil;


import ucar.visad.Util;
import ucar.visad.display.*;


import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.CoordinateSystem;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.LatLonPoint;

import visad.georef.MapProjection;

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

public class GridTrajectoryControlNew extends DrawingControl {


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
    //private Unit zunit;

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
    boolean isPoints = false;

    /** flag for streamlines */
    boolean isRectangle = false;

    /** _more_ */
    boolean isSelector = true;

    /** flag for streamlines */
    boolean isClosePlgn = false;

    /** _more_ */
    private JButton createTrjBtn;

    /** _more_ */
    boolean createTrjBtnClicked = false;

    /** _more_ */
    JPanel controlPane;

    /** _more_ */
    private MyTrajectoryControl gridTrackControl;

    /** _more_ */
    int trackLineWidth = 1;

    /** _more_ */
    boolean trackArrowHead = false;

    /** _more_ */
    Integer trackFormType = new Integer(0);

    /** _more_ */
    float trackOffsetValue = 4.0f;

    /** _more_ */
    Color trackColor = Color.red;

    /** _more_ */
    private DataTimeRange trjDataTimeRange;

    /** _more_ */
    ColorTable trjColorTable;

    /** _more_ */
    Range trjColorRange;

    /** _more_ */
    Range bundleColorRange = null;

    /** _more_ */
    boolean is2DDC = false;

    /** a component to change the skip */
    ValueSliderWidget skipFactorWidget;

    /** _more_ */
    int coordinateType = DrawingGlyph.COORD_LATLONALT;;

    /** _more_ */
    Unit newUnit = null;

    /** _more_ */
    boolean isBundle = false;

    /** _more_ */
    private JCheckBox backwardCbx;

    /** _more_ */
    private boolean backwardTrajectory = false;

    /** _more_ */
    protected DataSelection dataSelection1;

    /** _more_ */
    protected GridDataInstance gridDataInstance;

    /** _more_ */
    private boolean useSpeedForColor = false;

    /** _more_ */
    private boolean coloredByAnother = false;

    /** _more_ */
    private boolean withTopo = false;

    /**
     * Create a new Drawing Control; set attributes.
     */
    public GridTrajectoryControlNew() {
        //setCoordType(DrawingGlyph.COORD_LATLON);
        setLineWidth(2);
        reallySetAttributeFlags(FLAG_DISPLAYUNIT | FLAG_GRIDTRAJECTORY);
    }

    /**
     * _more_
     *
     * @param yesno _more_
     */
    public void setColoredByAnother(boolean yesno) {
        coloredByAnother = yesno;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getColoredByAnother() {
        return coloredByAnother;
    }

    /**
     * _more_
     *
     * @param yesno _more_
     */
    public void setUseSpeedForColor(boolean yesno) {
        useSpeedForColor = yesno;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getUseSpeedForColor() {
        return useSpeedForColor;
    }

    /**
     * _more_
     *
     * @param yesno _more_
     */
    public void setWithTopo(boolean yesno) {
        withTopo = yesno;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getWithTopo() {
        return withTopo;
    }

    /**
     * Class MyTrackControl _more_
     *
     *
     * @author yuanho
     * @version $Revision: 5.3 $
     */
    public static class MyTrajectoryControl extends GridDisplayControl implements FlowDisplayControl {

        /**
         * the display for the volume renderer
         */
        FlowDisplayable myDisplay;

        /** data choice for the data */
        protected DataChoice datachoice;

        /** a component to change the traj size */
        ValueSliderWidget trajLengthWidget;

        /** vector/traj length component */
        JComponent trajLengthComponent;

        /** _more_ */
        JComponent trajSkipComponent;

        /** a component to change the skip */
        int skipFactor = 0;

        /** flag for wind barbs */
        boolean isThreeComponents = true;

        /** arrow */
        boolean arrowHead = false;

        /** a scale factor */
        protected final float scaleFactor = 0.02f;

        /** a scale value */
        float flowScaleValue = 4.0f;

        /** a traj offset value */
        float arrowHeadSizeValue = 1.0f;

        /** a traj offset value */
        float trajOffsetValue = 4.0f;

        /** slider components */
        private JComponent[] widthSliderComps;

        /** Range for flow scale */
        private Range flowRange;

        /** the range dialog */
        RangeDialog rangeDialog;

        /** _more_ */
        private boolean useSpeedForColor = false;

        /** _more_ */
        private boolean coloredByAnother = false;

        /** _more_ */
        private int colorIndex = -1;

        /** labels for trajectory form */
        private final static String[] trajFormLabels = new String[] { "Line",
                                                                      "Ribbon",
                                                                      "Cylinder",
                                                                      "Deform Ribbon",
                                                                      "Point" };

        /** types of smoothing functions */
        private final static int[] trajForm = new int[] { 0, 1, 2, 3, 4 };

        /** vector/traj length component */
        JComponent trajFormComponent;

        /** default type */
        private Integer trajFormType = new Integer(0);

        /** start level */
        private int trajStartLevel = 0;

        /** start level */
        private int trajSkipLevels = 0;

        /** _more_ */
        private Range flowColorRange;

        /** _more_ */
        GridTrajectoryControlNew gtc;

        /** _more_ */
        boolean is2D = false;

        /** _more_ */
        Data topoData = null;

        /**
         * Default constructor; does nothing.
         *
         * @param gtc _more_
         */
        public MyTrajectoryControl(GridTrajectoryControlNew gtc) {
            //setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH | FLAG_SMOOTHING);
            //setAttributeFlags(FLAG_LINEWIDTH | FLAG_COLOR);
            this.gtc         = gtc;
            useSpeedForColor = gtc.getUseSpeedForColor();
            coloredByAnother = gtc.getColoredByAnother();
            if (useSpeedForColor || coloredByAnother) {
                setAttributeFlags(FLAG_LINEWIDTH | FLAG_COLORTABLE);
            } else {
                setAttributeFlags(FLAG_LINEWIDTH | FLAG_COLOR);
            }
        }

        /**
         * Construct a MyTrajectoryControl
         */
        public MyTrajectoryControl() {
            setAttributeFlags(FLAG_LINEWIDTH | FLAG_COLOR);

        }

        /**
         * Call to help make this kind of Display Control; also calls code to
         * made the Displayable (empty of data thus far).
         * This method is called from inside DisplayControlImpl.init(several args).
         *
         * @param dataChoice the DataChoice of the moment.
         *
         * @return true if successful
         *
         * @throws java.rmi.RemoteException  Java RMI error
         * @throws visad.VisADException   VisAD Error
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public boolean init(DataChoice dataChoice)
                throws VisADException, RemoteException {
            datachoice = dataChoice;


            // checeking grid size matching between u and w
            if (dataChoice instanceof DerivedDataChoice) {
                DerivedDataChoice ddc      = (DerivedDataChoice) dataChoice;
                List              choices0 = ddc.getChoices();
                if (choices0.size() == 1) {  // colored by another param
                    ddc      = (DerivedDataChoice) choices0.get(0);
                    choices0 = ddc.getChoices();
                }
                if (choices0.size() == 3) {
                    DirectDataChoice udc = (DirectDataChoice) choices0.get(0);
                    DirectDataChoice vdc = (DirectDataChoice) choices0.get(1);
                    DirectDataChoice wdc = (DirectDataChoice) choices0.get(2);
                    List             usTime = udc.getAllDateTimes();
                    List             wsTime = wdc.getAllDateTimes();
                    List selectedTimes      = getDataSelection().getTimes();
                    if (selectedTimes != null) {
                        int len = selectedTimes.size();
                        if ((usTime.get((int) selectedTimes.get(0))
                                != wsTime.get((int) selectedTimes.get(0)))
                                || (usTime.get(
                                    (int) selectedTimes.get(
                                        len - 1)) != wsTime.get(
                                            (int) selectedTimes.get(
                                                len - 1)))) {
                            userErrorMessage(
                                "w grid selected times are different from u grid ");
                            return false;
                        }
                    } else if (wdc.getSelectedDateTimes() != null) {
                        selectedTimes = wdc.getSelectedDateTimes();
                        int len = selectedTimes.size();
                        if ((usTime.get(0) != wsTime.get(0))
                                || (usTime.get(len - 1)
                                    != wsTime.get(len - 1))) {
                            userErrorMessage(
                                "w grid selected times are different from u grid ");
                            return false;
                        }
                    }
                } else if (choices0.size() == 2) {
                    is2D = true;
                }
            }

            levelBox = doMakeLevelControl(null);
            levelBox.addActionListener(new ActionListener() {
                                           public void actionPerformed(
                                           ActionEvent e) {
                                               if (getOkToFireEvents()) {
                                                   TwoFacedObject select =
                                                       (TwoFacedObject) ((JComboBox) e.getSource()).getSelectedItem();
                                                   int selectIdx =
                                                       ((JComboBox) e.getSource()).getSelectedIndex();

                                                   if ((select != null)) {
                                                       int ct =
                                                           ((JComboBox) e.getSource()).getItemCount();
                                                       if (select.toString()
                                                       .equals("All Levels")) {
                                                           setTrajStartLevel(
                                                           select, selectIdx);
                                                       } else {
                                                           setTrajStartLevel(
                                                           select, selectIdx);
                                                       }

                                                   }
                                               }
                                           }
                                       });
            //myDisplay = (FlowDisplayable) createPlanDisplay();

            //myDisplay.setPointSize(4);
            //addDisplayable(myDisplay, getAttributeFlags());
            //myDisplay.setForward(!gtc.backwardTrajectory);
            //DataSelection ds = getDataSelection();
            DataSelection tmpSelection =
                new DataSelection(gtc.getDataSelection());
            tmpSelection.setFromLevel(null);
            tmpSelection.setToLevel(null);
            dataChoice.setDataSelection(tmpSelection);

            //updateDataSelection(ds);
            //doMakeDataInstance(dataChoice);
            //Now, set the data. Return false if it fails.
            if ( !setData(dataChoice)) {
                return false;
            }

            addDisplayable(myDisplay, getAttributeFlags());

            //Now set up the flags and add the displayable
            return true;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getLineWidthWidgetLabel() {
            return "Line Width/Point Size";
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        protected DisplayableData createPlanDisplay()
                throws VisADException, RemoteException {
            FlowDisplayable planDisplay;

            planDisplay = new FlowDisplayable("FlowPlanViewControl_vectors_"
                    + ((datachoice != null)
                       ? datachoice.toString()
                       : ""), null);

            planDisplay.set3DFlow(true);

            planDisplay.setUseSpeedForColor(useSpeedForColor);
            planDisplay.setTrojectoriesEnabled(true, arrowHeadSizeValue,
                    false);

            if (useSpeedForColor || coloredByAnother) {
                if (coloredByAnother) {
                    if (is2D) {
                        colorIndex = 2;
                    } else {
                        colorIndex = 3;
                    }
                }
                addAttributedDisplayable(planDisplay, FLAG_COLORTABLE);
                addAttributedDisplayable(planDisplay, FLAG_LINEWIDTH);
            } else {
                addAttributedDisplayable(planDisplay);
            }
            return planDisplay;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        FlowDisplayable getGridDisplay() {
            return myDisplay;
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

            //super.getControlWidgets(controlWidgets);

            JCheckBox arrowCbx = new JCheckBox("Arrow", arrowHead);

            arrowCbx.addActionListener(new ActionListener() {
                                           public void actionPerformed(
                                           ActionEvent e) {
                                               arrowHead =
                                               ((JCheckBox) e.getSource()).isSelected();
                                               if (arrowHead) {
                                                   getGridDisplay().setArrowHead(
                                                   arrowHead);
                                               } else {
                                                   getGridDisplay().setArrowHead(
                                                   arrowHead);
                                               }
                                               if (gtc != null) {
                                                   gtc.setTrackArrowHead(
                                                   arrowHead);
                                               }
                                               getGridDisplay().resetTrojectories();
                                           }
                                       });


            if (getIsThreeComponents()) {

                trajLengthWidget = new ValueSliderWidget(this, 1, 21,
                        "trajOffset", "LengthOffset");

                List<TwoFacedObject> trajFormList =
                    TwoFacedObject.createList(trajForm, trajFormLabels);
                JComboBox trajFormBox = new JComboBox();
                GuiUtils.setListData(trajFormBox, trajFormList);
                trajFormBox.setSelectedItem(
                    TwoFacedObject.findId(getTrajFormType(),
                                          trajFormList));
                trajFormBox.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                TwoFacedObject select =
                                    (TwoFacedObject) ((JComboBox) e.getSource()).getSelectedItem();
                                setTrajFormType(select.getId().hashCode());
                            }
                        });
                trajFormComponent =
                    GuiUtils.hbox(GuiUtils.rLabel("Trajectory Form: "),
                                  GuiUtils.filler(), trajFormBox,
                                  GuiUtils.filler());
                controlWidgets.add(new WrapperWidget(this,
                        GuiUtils.rLabel("Trajectory Form: "),
                        GuiUtils.left(trajFormBox)));

                trajLengthComponent =
                    GuiUtils.hbox(GuiUtils.rLabel("Length Offset: "),
                                  trajLengthWidget.getContents(false),
                                  arrowCbx);
                Insets spacer = new Insets(0, 30, 0, 0);
                controlWidgets.add(new WrapperWidget(this,
                        GuiUtils.rLabel("Length Offset: "),
                        GuiUtils.left(
                            GuiUtils.hbox(trajLengthWidget.getContents(false),
                                          arrowCbx))));

            }



            enableTrajLengthBox();


            List timeL = getDataSelection().getTimes();
            if ((timeL == null) && getHadDataChoices()) {
                List dchoices = getMyDataChoices();
                timeL = ((DataChoice) dchoices.get(0)).getSelectedDateTimes();
                if ((timeL != null) && (timeL.size() == 0)) {
                    timeL = ((DataChoice) dchoices.get(0)).getAllDateTimes();
                }
            }
            super.getControlWidgets(controlWidgets);


        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getIsThreeComponents() {
            return isThreeComponents;
        }

        /**
         * _more_
         */
        public void setStreamlines1() {
            //isStreamlines = v;
            if (getGridDisplay() != null) {
                int    ct = levelBox.getItemCount();
                String tt = levelBox.getSelectedItem().toString();
                if (tt.equals("All Levels")) {
                    getGridDisplay().setZskip(1);
                } else {
                    getGridDisplay().setZskip(ct - 1);
                }

                getGridDisplay().setTrajStartLevel(trajStartLevel);
                getGridDisplay().setIsTrajectories(true);
                getGridDisplay().setTrojectoriesEnabled(true,
                        arrowHeadSizeValue, false);
                enableTrajLengthBox();

            }


        }

        /**
         * enable the barb size box
         */
        private void enableTrajLengthBox() {
            if (trajLengthComponent != null) {
                GuiUtils.enableTree(trajLengthComponent, true);
                GuiUtils.enableTree(trajFormComponent, true);
                //GuiUtils.enableTree(trajSkipComponent, true);
                GuiUtils.enableTree(levelBox, true);
            }
        }


        /**
         * _more_
         *
         * @param value _more_
         */
        public void setPointSize(float value) {
            super.setPointSize(value);
            if (myDisplay != null) {
                try {
                    myDisplay.setPointSize(getPointSize());
                } catch (Exception e) {
                    logException("Setting point size", e);
                }
            }
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getArrowHead() {
            return arrowHead;
        }

        /**
         * _more_
         *
         * @param ah _more_
         */
        public void setArrowHead(boolean ah) {
            arrowHead = ah;
            if (gtc != null) {
                gtc.setTrackArrowHead(ah);
            }
        }


        /**
         * Set the data in this control.
         *
         * @param choice  data description
         *
         * @return true if successful
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         */
        protected boolean setData(DataChoice choice)
                throws VisADException, RemoteException {
            if ( !super.setData(choice) || (getNavigatedDisplay() == null)) {
                return false;
            }

            DataSelection tmpSelection =
                new DataSelection(getDataSelection());
            tmpSelection.setFromLevel(null);
            tmpSelection.setToLevel(null);
            DataChoice wchoice = null;

            if (coloredByAnother) {
                DerivedDataChoice derivedDataChoice =
                    ((DerivedDataChoice) ((DerivedDataChoice) choice).getChoices()
                    .get(0));
                if (is2D) {
                    wchoice =
                        ((DataChoice) (derivedDataChoice).getChoices().get(
                            0));
                } else {
                    wchoice =
                        ((DataChoice) (derivedDataChoice).getChoices().get(
                            2));
                }
            } else if (is2D) {
                wchoice =
                    ((DataChoice) ((DerivedDataChoice) choice).getChoices()
                    .get(0));
            } else {
                wchoice =
                    ((DataChoice) ((DerivedDataChoice) choice).getChoices()
                    .get(2));
            }
            List     levelsList = wchoice.getAllLevels(tmpSelection);
            Object[] levels     = null;
            if ((levelsList != null) && (levelsList.size() > 0)) {
                levels = (Object[]) levelsList.toArray(
                    new Object[levelsList.size()]);
            }

            if (levels == null) {
                levels = getGridDataInstance().getLevels();
            }

            if (levels != null) {
                setLevels(levels);
            } else {
                is2D = true;
            }
            myDisplay = (FlowDisplayable) createPlanDisplay();
            myDisplay.setActive(false);
            myDisplay.setUseSpeedForColor(useSpeedForColor);
            myDisplay.setColoredByAnother(coloredByAnother);
            if (useSpeedForColor) {
                colorIndex = myDisplay.getSpeedTypeIndex();
            }
            if (coloredByAnother) {
                colorIndex = 3;
            }

            //loadVolumeData();

            if (gtc.withTopo) {
                // topoData = (FieldImpl) getTOPOdata(wchoice);
                DataChoice topoChoice =
                    ((DataChoice) ((DerivedDataChoice) choice).getChoices()
                    .get(3));
                topoData = topoChoice.getData(wchoice.getDataSelection());
            }

            if (useSpeedForColor || coloredByAnother) {
                setFlowColorRange();
            }

            myDisplay.setActive(true);
            return true;
        }


        /** level selection box */
        private JComboBox levelBox;

        /** current level */
        protected Object currentLevel;

        /**
         * _more_
         *
         * @param levels _more_
         */
        public void setLevels(Object[] levels) {

            if (levels == null) {
                levels = getGridDataInstance().getLevels();
            }

            setOkToFireEvents(false);

            if (levelBox == null) {
                return;
            }
            levelBox.setEnabled(false);
            Object[] all = formatLevels(levels);

            GuiUtils.setListData(levelBox, formatLevels(levels));

            int len = levels.length;

            currentLevel = all[len - 1];
            if (((Real) ((TwoFacedObject) all[0]).getId()).getValue()
                    < ((Real) ((TwoFacedObject) all[len - 1]).getId()).getValue()) {
                trajStartLevel = len - 1;
            } else {
                trajStartLevel = 0;
            }
            setTrajStartLevel(currentLevel, trajStartLevel);

            levelBox.setSelectedItem(getLabeledReal(currentLevel));

            setOkToFireEvents(true);
            levelBox.setEnabled(true);
        }

        /**
         * _more_
         *
         * @param levels _more_
         *
         * @return _more_
         */
        protected Object[] formatLevels(Object[] levels) {
            if (levels == null) {
                return null;
            }

            int      len     = levels.length;
            Object[] tfoList = new Object[len];
            for (int i = 0; i < len; i++) {
                tfoList[i] = getLabeledReal(levels[i]);
            }

            return tfoList;
        }

        /**
         * _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        private void setFlowColorRange()
                throws RemoteException, VisADException {
            if ((getGridDisplay() != null)) {
                if (getFlowRange() == null) {
                    Range[] ranges = null;
                    Data    data   = getGridDisplay().getData();
                    if (data != null) {
                        ranges = GridUtil.getMinMax((FieldImpl) data);
                        double max       = Double.NEGATIVE_INFINITY;
                        double min       = Double.POSITIVE_INFINITY;
                        int    startComp = 0;
                        int    numComps  = getIsThreeComponents()
                                           ? 3
                                           : 2;
                        boolean isCartesian =
                            getGridDisplay().isCartesianWind();
                        //System.out.println("control thinks cartesian is " + isCartesian);
                        if ( !isCartesian) {
                            int speedIndex =
                                getGridDisplay().getSpeedTypeIndex();
                            if (speedIndex != -1) {
                                startComp = speedIndex;
                                numComps  = startComp + 1;
                            }
                        }
                        if ((useSpeedForColor || coloredByAnother)
                                && (ranges.length > numComps)) {
                            Range compRange = ranges[ranges.length - 1];
                            max = Math.max(compRange.getMax(), max);
                            min = Math.min(compRange.getMin(), min);
                        } else {
                            for (int i = startComp; i < numComps; i++) {
                                Range compRange = ranges[i];
                                max = Math.max(compRange.getMax(), max);
                                //min = Math.min(compRange.getMin(), min);
                                min = Math.min(compRange.getMin(), min);
                            }
                        }

                        if ( !useSpeedForColor
                                && !coloredByAnother
                                && !Double.isInfinite(max)
                                && !Double.isInfinite(min)) {
                            max = Math.max(max, -min);
                            min = isCartesian
                                  ? -max
                                  : 0;
                        }

                        flowColorRange = new Range(min, max);
                    } else {  // gotta set it to something
                        flowColorRange = new Range(-40, 40);
                    }
                }
            }
        }

        /**
         * _more_
         *
         * @param colorRange _more_
         */
        public void setFlowColorRange(Range colorRange) {
            flowColorRange = colorRange;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Range getFlowColorRange() {
            return flowColorRange;
        }

        /**
         * Get the flow range.
         * Used by XML persistence
         *
         * @return  the flow range for this control
         */
        public Range getFlowRange() {
            return flowRange;
        }




        /**
         * Set the flow range.
         * Used by XML persistence
         *
         * @param f   new flow range
         */
        public void setFlowRange(Range f) {
            flowRange = f;
            if ((getGridDisplay() != null) && (flowRange != null)) {
                try {
                    getGridDisplay().setFlowRange(flowRange);
                } catch (Exception excp) {
                    logException("setFlowRange: ", excp);
                }
            }

            if (getHaveInitialized()) {
                doShare(SHARE_FLOWRANGE, flowRange);
            }
        }

        /**
         * Make the gui. Align it left
         *
         * @return The gui
         *
         * @throws RemoteException on badness
         * @throws VisADException on badness
         */
        protected Container doMakeContents()
                throws VisADException, RemoteException {

            return GuiUtils.left(doMakeWidgetComponent());
        }


        /**
         * Load the volume data to the display
         *
         * @throws RemoteException   problem loading remote data
         * @throws VisADException    problem loading the data
         */
        private void loadVolumeData() throws VisADException, RemoteException {
            Trace.call1("VRC.loadVolumeData");
            FieldImpl grid    = getGridDataInstance().getGrid();
            FieldImpl newGrid = grid;
            //    myDisplay.setTrojectoriesEnabled(true,
            //            arrowHeadSizeValue, false);
            //if(!isTrajectories) {
            if (gtc.getSkipValue() > 0) {
                grid    = GridUtil.subset(grid, gtc.getSkipValue());
                newGrid = grid;
            }

            Trace.call1("VRC.loadVolumeData.loadData");
            myDisplay.setColoredByAnother(coloredByAnother);
            myDisplay.loadData(newGrid);
            Trace.call2("VRC.loadVolumeData.loadData");
            Trace.call2("loadVolumeData");
        }



        /**
         * Method to call if projection changes.  Subclasses that
         * are worried about such events should implement this.
         */
        public void projectionChanged() {
            //System.out.println("projection changed");
            try {
                loadVolumeData();
            } catch (Exception exc) {
                logException("loading volume data", exc);
            }
            super.projectionChanged();
        }



        /**
         * Is this a raster display
         *
         * @return true
         */
        public boolean getIsRaster() {
            return false;
        }


        /**
         * Get the flow scale.
         * Used by XML persistence
         *
         * @return  the flow scale for this control
         */
        public float getFlowScale() {
            return flowScaleValue;
        }

        /**
         * Set the flow scale.
         * Used by XML persistence
         *
         * @param f   new flow scale
         */
        public void setFlowScale(float f) {
            flowScaleValue = f;
            if (getGridDisplay() != null) {
                try {
                    getGridDisplay().setFlowScale(getDisplayScale()
                            * scaleFactor * flowScaleValue);
                } catch (Exception ex) {
                    logException("setFlowScale: ", ex);
                }

            }

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public float getTrajOffset() {
            return trajOffsetValue;
        }

        /**
         * _more_
         *
         *
         * @param f _more_
         * @return _more_
         */

        public void setTrajOffset(float f) {
            trajOffsetValue = f;
            if (getGridDisplay() != null) {
                try {
                    if (gtc != null) {
                        gtc.setTrackOffsetValue(f);
                    }
                    getGridDisplay().setTrajOffset(trajOffsetValue);
                    getGridDisplay().resetTrojectories();
                } catch (Exception ex) {
                    logException("setFlowScale: ", ex);
                }

            }

            if (trajLengthWidget != null) {
                trajLengthWidget.setValue(f);
            }
        }



        /**
         * _more_
         *
         * @return _more_
         */
        public Integer getTrajFormType() {
            return trajFormType;
        }

        /**
         * _more_
         *
         * @param trajForm _more_
         */
        public void setTrajFormType(Integer trajForm) {
            trajFormType = trajForm;

            if (gtc != null) {
                gtc.setTrackFormType(trajFormType);
            }
            if (getGridDisplay() != null) {
                try {
                    getGridDisplay().setTrajFormType(trajForm.intValue());
                    //getGridDisplay().resetTrojectories();
                    getGridDisplay().setArrowHead(arrowHead);
                    getGridDisplay().resetTrojectories();
                    //setLineWidth(super.getLineWidth());
                } catch (Exception ex) {
                    logException("setFlowScale: ", ex);
                }

            }
        }

        /**
         * _more_
         *
         * @param startLevel _more_
         * @param idx _more_
         */
        public void setTrajStartLevel(Object startLevel, int idx) {
            trajStartLevel = idx;
            currentLevel   = startLevel;
            int ct = levelBox.getItemCount();
            if (ct == 1) {
                ct = 2;
            }
            //if(skipFactorWidgetZ != null)
            //    skipFactorWidgetZ.setValue(ct-1);
            if (getGridDisplay() != null) {
                try {
                    getGridDisplay().setTrajStartLevel(idx);
                    getGridDisplay().setZskip(ct);
                    getGridDisplay().resetTrojectories();
                } catch (Exception ex) {
                    logException("setFlowScale: ", ex);
                }

            }

        }


        /**
         * _more_
         *
         * @param yesno _more_
         */
        public void setColoredByAnother(boolean yesno) {
            coloredByAnother = yesno;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getColoredByAnother() {
            return coloredByAnother;
        }

        /**
         * _more_
         *
         * @param yesno _more_
         */
        public void setUseSpeedForColor(boolean yesno) {
            useSpeedForColor = yesno;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getUseSpeedForColor() {
            return useSpeedForColor;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected int getColorRangeIndex() {
            if (colorIndex >= 0) {
                return colorIndex;
            }

            return getIsThreeComponents()
                   ? 3
                   : 2;
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public Range getRangeForColorTable()
                throws RemoteException, VisADException {
            if (getFlowColorRange() == null) {
                setFlowColorRange();
            }
            return getFlowColorRange();
        }

        /**
         * _more_
         *
         * @param r _more_
         *
         * @return _more_
         */
        private Range makeFlowRange(Range r) {
            if (haveMultipleFields()) {
                return r;
            }
            if (r == null) {
                return r;
            }
            double max = Math.max(Math.abs(r.getMax()), Math.abs(r.getMin()));
            return new Range(-max, max);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected boolean haveMultipleFields() {
            if (getGridDataInstance() == null) {
                return false;
            }
            return ((getGridDataInstance().getNumRealTypes()
                     > ((getIsThreeComponents())
                        ? 3
                        : 2))) || useSpeedForColor || coloredByAnother;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean showColorControlWidget() {
            return !useSpeedForColor && !coloredByAnother;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getColorWidgetLabel() {
            return "Color";
        }

        /**
         * _more_
         *
         * @return _more_
         */
        protected String getColorParamName() {
            if (useSpeedForColor) {
                return "windSpeed";
            } else if (coloredByAnother) {
                if (getGridDataInstance() == null) {
                    return "ColoredByAnother";
                }
                String pname =
                    getGridDataInstance().getRealTypeName(colorIndex);
                int eidx = pname.indexOf("[unit");
                return pname.substring(0, eidx);
            } else {
                return super.getColorParamName();
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
        protected Range getInitialRange()
                throws RemoteException, VisADException {
            if (useSpeedForColor) {
                return flowRange;
            } else if (coloredByAnother) {
                if (getGridDataInstance() == null) {
                    return null;
                }
                return getGridDataInstance().getRanges()[colorIndex];
            } else {
                return super.getInitialRange();
            }

        }


        /**
         * _more_
         *
         *
         * @param width _more_
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void setLineWidth(int width)
                throws RemoteException, VisADException {
            if (getGridDisplay() != null) {
                if (trajFormType == 4) {
                    setPointSize(width * 1.0f);
                } else if (trajFormType == 2) {
                    getGridDisplay().setTrajWidth(width * 0.01f);
                    getGridDisplay().resetTrojectories();
                } else if ((trajFormType == 1) || (trajFormType == 3)) {
                    getGridDisplay().setRibbonWidth(width);
                    getGridDisplay().resetTrojectories();
                }
                //getGridDisplay().resetTrojectories();
            }
            if (gtc != null) {
                gtc.setTrackLineWidth(width);
            }
            super.setLineWidth(width);

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getLineWidth() {
            return super.getLineWidth();
        }


        /**
         * _more_
         *
         * @param vc _more_
         * @param properties _more_
         */
        @Override
        public void initAfterUnPersistence(ControlContext vc,
                                           Hashtable properties) {
            //super.initAfterUnPersistence(vc, properties);

            setTrajFormType(gtc.getTrackFormType());
            int width = super.getLineWidth();
            if (getGridDisplay() != null) {
                setTrajFormType(gtc.getTrackFormType());
                try {
                    setRange(gtc.getRange());
                } catch (Exception ee) {}
                if (trajFormType == 2) {
                    getGridDisplay().setTrajWidth(width * 0.01f);
                } else if ((trajFormType == 1) || (trajFormType == 3)) {
                    getGridDisplay().setRibbonWidth(width);
                }
                setArrowHead(gtc.getTrackArrowHead());
                getGridDisplay().setArrowHead(gtc.getTrackArrowHead());

            }

            getGridDisplay().resetTrojectories();
        }

        /**
         * _more_
         *
         * @param c _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public void setColor(Color c) throws RemoteException, VisADException {
            super.setColor(c);
            if (gtc != null) {
                gtc.setTrackColor(c);
            }
        }

        /**
         * _more_
         *
         *
         * @return _more_
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public Range getRange() throws RemoteException, VisADException {
            return super.getRange();
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
            setFlowColorRange(nRange);
            if (gtc != null) {
                gtc.setRange(nRange);
            }
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
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        private boolean trackDataOk() throws VisADException, RemoteException {

            DataInstance dataInstance = gtc.getDataInstance();
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
        public Object[] getLevels() {
            DataSelection tmpSelection =
                new DataSelection(getDataSelection());
            tmpSelection.setFromLevel(null);
            tmpSelection.setToLevel(null);

            DataChoice wchoice = null;

            if (coloredByAnother) {
                DerivedDataChoice derivedDataChoice =
                    ((DerivedDataChoice) ((DerivedDataChoice) datachoice).getChoices()
                    .get(0));
                if (is2D) {
                    wchoice =
                        ((DataChoice) (derivedDataChoice).getChoices().get(
                            0));
                } else {
                    wchoice =
                        ((DataChoice) (derivedDataChoice).getChoices().get(
                            2));
                }
            } else if (is2D) {
                wchoice =
                    ((DataChoice) ((DerivedDataChoice) datachoice).getChoices()
                    .get(0));
            } else {
                wchoice =
                    ((DataChoice) ((DerivedDataChoice) datachoice).getChoices()
                    .get(2));
            }
            List     levelsList = wchoice.getAllLevels(tmpSelection);
            Object[] levels     = null;
            if ((levelsList != null) && (levelsList.size() > 0)) {
                levels = (Object[]) levelsList.toArray(
                    new Object[levelsList.size()]);
            }

            if (levels == null) {
                levels = getGridDataInstance().getLevels();
            }

            return levels;
        }

        /**
         * _more_
         *
         * @param dataChoice _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        protected DataInstance doMakeDataInstance(DataChoice dataChoice)
                throws RemoteException, VisADException {
            DataSelection ds = getDataSelection();
            Object t =
                ds.getProperty(DataSelection.PROP_PROGRESSIVERESOLUTION);
            if (t != null) {
                isProgressiveResolution = ((Boolean) t).booleanValue();
            }

            return gridDataInstance = new GridDataInstance(dataChoice,
                    ds,
                    getRequestProperties());
        }

        /**
         * _more_
         *
         * @param
         *
         * @return _more_
         */
        public GridDataInstance getGridDataInstance() {
            if (this.gridDataInstance == null) {
                try {
                    doMakeDataInstance(this.datachoice);
                } catch (Exception exce) {}
            }
            return gridDataInstance;

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean getHaveInitialized() {
            return true;
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
        gridTrackControl = new MyTrajectoryControl(this);
        //super.init(dataChoice);
        dataSelection1 = getDataSelection();
        dataChoice.setDataSelection(dataSelection1);
        this.dataChoice                 = dataChoice;
        gridTrackControl.controlContext = getControlContext();

        gridTrackControl.setUsesTimeDriver(this.getUsesTimeDriver());
        gridTrackControl.updateDataSelection(dataSelection1);
        gridTrackControl.init(dataChoice);

        //updateDataSelection(dataSelection1);

        Object fromLevel = dataSelection1.getFromLevel();
        dataSelection1.setLevel(null);


        //GridDataInstance gdi = new GridDataInstance(sdc, dataSelection1,
        //                          getRequestProperties());

        gridTrackControl.controlContext = getControlContext();
        //gridTrackControl.updateGridDataInstance(gdi);

        initDisplayUnit();

        // level widget init
        levelBox = gridTrackControl.doMakeLevelControl(null);
        levelBox.addActionListener(new ActionListener() {
                                       public void actionPerformed(
                                       ActionEvent event) {
                                           String cmd =
                                               event.getActionCommand();
                                           if (cmd.equals(CMD_SETLEVELS)) {
                                               TwoFacedObject select =
                                                   (TwoFacedObject) ((JComboBox) event.getSource()).getSelectedItem();
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
                                         public void actionPerformed(
                                         ActionEvent ae) {
                                             moveUpDown(-1);
                                         }
                                     });

        ImageIcon downIcon =
            GuiUtils.getImageIcon(
                "/ucar/unidata/idv/control/images/LevelDown.gif");
        levelDownBtn = new JButton(downIcon);
        levelDownBtn.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        levelDownBtn.addActionListener(new ActionListener() {
                                           public void actionPerformed(
                                           ActionEvent event) {
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


        Object[] levels = gridTrackControl.getLevels();


        if (currentLevel == null) {
            currentLevel = fromLevel;  //getDataSelection().getFromLevel();
        }
        if ((levels != null)
                && (levels.length > 0)
                && (currentLevel == null)) {
            currentLevel = levels[0];
        }
        if ((levels != null) && (fromLevel == null)) {
            int len = levels.length;
            if (((Real) levels[0]).getValue()
                    < ((Real) levels[len - 1]).getValue()) {
                fromLevel = levels[len - 1];
            } else {
                fromLevel = levels[0];
            }
        }
        if (levels != null) {
            setLevels(levels, fromLevel);
        }

        // the control for the track
        setDisplayActive();
        if (gridTrackControl.myDisplay == null) {
            gridTrackControl.myDisplay =
                (FlowDisplayable) gridTrackControl.createPlanDisplay();
        }
        setLineWidth(gridTrackControl.getLineWidth());
        addDisplayable(gridTrackControl.myDisplay, getAttributeFlags());


        getViewAnimation();
        gridTrackControl.addDisplayable(gridTrackControl.myDisplay,
                                        getAttributeFlags());
        // return setData(dataChoice);
        createTrjBtn = new JButton("Create Trajectory");
        createTrjBtn.addActionListener(this);
        createTrjBtn.setActionCommand(CMD_createTrj);
        createTrjBtn.addActionListener(new ActionListener() {
                                           public void actionPerformed(
                                           ActionEvent actionEvent) {
                                               String cmd =
                                                   actionEvent.getActionCommand();
                                               if (
                                               cmd.equals(CMD_createTrj)) {
                                                   try {
                                                       createTrjBtnClicked =
                                                       true;
                                                       removeGC();
                                                       // gridTrackControl.setLineWidth(trackLineWidth);
                                                       createTrajectory();

                                                   } catch (Exception exr) {
                                                       logException(
                                                       "Click create trajectory button",
                                                           exr);
                                                   }
                                               }


                                           }
                                       });

        controlPane = new JPanel();
        controlPane.setPreferredSize(new Dimension(300, 280));

        return true;


    }


    @Override
    public synchronized void dataChanged() {
        try {
            //removeAllGlyphs();
            //initData();
        } catch (Exception e) {}

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
            //gridTrackControl.selectRangeDisplay.setDisplayUnit(newUnit);
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
                                 createTrajectoryControl();
                             }
                         } catch (Exception exc) {
                             logException(
                                 "Calculationing the grid trajectory", exc);
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
            //if ((getGlyphs() != null) && (glyphs.size() > 0)) {
            currentLevel = getCurrentLevel();
            if ((currentLevel != null)
                    && (bundleLevel != null)
                    && !currentLevel.equals(bundleLevel)) {
                setLevel(bundleLevel);
                levelBox.setSelectedItem(bundleLevel);
            }
            if (backwardTrajectory) {
                backwardCbx.doClick();
            }
            newUnit = getDisplayUnit();
            gridTrackControl.initAfterUnPersistence(vc, properties);
            createTrjBtn.doClick();

            // gridTrackControl.setDataTimeRange(getTrjDataTimeRange());
            gridTrackControl.getDataTimeRange(true).getTimeModeLabel();

            try {
                gridTrackControl.setLineWidth(getTrackLineWidth());
                gridTrackControl.setTrajOffset(getTrackOffsetValue());
                gridTrackControl.setColor(getTrackColor());
                //gridTrackControl.setColorScaleInfo(getColorScaleInfo());
                gridTrackControl.setColorTable(getTrjColorTable());
                doMakeColorScales();
                bundleColorRange = getTrjColorRange();
            } catch (Exception ee) {}

            // }
        }

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

        if (selected >= 0) {
            int    ct       = levelBox.getItemCount();
            Object levelObj = levelBox.getSelectedItem();
            if (levelObj.toString().equals("All Levels")) {
                gridTrackControl.setTrajStartLevel(levelObj, selected);
            } else {
                gridTrackControl.setTrajStartLevel(levelObj, selected);
            }
        }

    }

    /**
     * Get the label for the levels box.
     * @return the label
     */
    public String getLevelsLabel() {
        return "Trajectory Start Level:";
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

        Object slevel = levelBox.getSelectedItem();
        int    idx    = levelBox.getSelectedIndex();
        gridTrackControl.loadVolumeData();
        gridTrackControl.setTrajStartLevel(slevel, idx);
        //
        if ( !hiddenBtn.isSelected() || (glyphs.size() > 0)) {
            GriddedSet domainSet =
                (GriddedSet) gridTrackControl.getGridDataInstance()
                .getSpatialDomain();
            SampledSet domain2D =
                GridUtil.makeDomain2D((GriddedSet) domainSet);
            CoordinateSystem cs =
                getNavigatedDisplay().getDisplayCoordinateSystem();
            MapProjectionDisplay mpd =
                (MapProjectionDisplay) getNavigatedDisplay();
            boolean isLatLon = GridUtil.isLatLonOrder(domainSet);
            int     latIndex = isLatLon
                               ? 0
                               : 1;
            int     lonIndex = isLatLon
                               ? 1
                               : 0;

            Real    alt      = null;
            // if(zunit.getIdentifier().length() == 0) {
            if (currentLevel != null) {
                alt = GridUtil.getAltitude(
                    domainSet,
                    (Real) ((TwoFacedObject) currentLevel).getId());
            } else {
                alt = new Real(0.0);
            }
            float[][] geoVals = getEarthLocationPoints(latIndex, lonIndex,
                                    domain2D, alt, getSkipValue());
            float[][] setLocs = cs.toReference(geoVals);
            setLocs[2] = mpd.scaleVerticalValues(setLocs[2]);

            RealTupleType types = cs.getReference();
            gridTrackControl.myDisplay.setStartPoints(types, setLocs);

        } else {
            gridTrackControl.myDisplay.setStartPoints(null, null);
        }
        if (withTopo) {
            gridTrackControl.myDisplay.loadTopoData(
                (FieldImpl) gridTrackControl.topoData);
        }
        //gridTrackControl.loadVolumeData();
        gridTrackControl.myDisplay.setForward( !backwardCbx.isSelected());
        gridTrackControl.myDisplay.setArrowHead(
            gridTrackControl.getArrowHead());
        Range range = gridTrackControl.getGridDataInstance().getRange(
                          gridTrackControl.getColorRangeIndex());
        gridTrackControl.myDisplay.resetTrojectories();
        //gridTrackControl.setRange(range);

        controlPane.setVisible(true);
        controlPane.add(gridTrackControl.doMakeContents());

        //Unit cUnit = getDisplayUnit();

        //if (newUnit != null) {
        //    cUnit = newUnit;
        //}

        // gridTrackControl.getColorRangeFromData());
    }



    /**
     * _more_
     *
     *
     * @param latIndex _more_
     * @param lonIndex _more_
     * @param domain0 _more_
     * @param alt _more_
     * @param skipFactor _more_
     * @return _more_
     *
     * @throws Exception _more_
     */
    public float[][] getEarthLocationPoints(int latIndex, int lonIndex,
                                            SampledSet domain0, Real alt,
                                            int skipFactor)
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
                GlyphCreatorCommand.CMD_SYMBOL.getLabel())
                || ((glyphs != null) && (glyphs.size() > 0)
                    && (glyphs.get(0) instanceof SymbolGlyph))) {
            int       pointNum = glyphs.size();

            float[][] points   = new float[3][pointNum];

            for (int i = 0; i < pointNum; i++) {
                DrawingGlyph glyph = (DrawingGlyph) glyphs.get(i);
                points[0][i] = glyph.getLatLons()[0][0];
                points[1][i] = (float) LatLonPointImpl.lonNormal(
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

            int       onum   = num / (skipFactor + 1) + 1;

            float[][] points = new float[3][num];
            int       psize  = 0;
            for (int k = 0; k < latlons.length; k++) {
                int isize = latlons[k][0].length;
                for (int i = 0; i < isize; i++) {
                    points[0][i + psize] = latlons[k][0][i];
                    points[1][i + psize] =
                        (float) LatLonPointImpl.lonNormal(latlons[k][1][i]);
                    points[2][i + psize] = z;
                }
                psize = psize + isize;
            }
            // now skipFactor
            if (skipFactor > 0) {
                float[][] points0 = new float[3][onum];
                for (int i = 0, j = 0; (i < onum)
                                       && (j < num);
                        i++, j = i * (skipFactor + 1)) {
                    points0[0][i] = points[0][j];
                    points0[1][i] = points[1][j];
                    points0[2][i] = points[2][j];
                }
                setCurrentCommand(CMD_SELECT);
                hiddenBtn.doClick();

                return points0;
            } else {
                return points;
            }
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
        //super.setCurrentCommand(CMD_SELECT);
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
                                                                 levelDownBtn }, 1,
                                                                     GuiUtils.WT_N,
                                                                     GuiUtils.WT_N);
        JPanel levelSelector = GuiUtils.doLayout(new Component[] { levelBox,
                                                                   levelUpDown }, 2,
                                                                       GuiUtils.WT_N,
                                                                       GuiUtils.WT_N);

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
            setCurrentCommand(CMD_SELECT);
        }

        skipFactorWidget = new ValueSliderWidget(this, 0, 10, "skipValue",
                getSkipWidgetLabel());
        skipFactorWidget.setEnabled(true);
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
                    skipFactorWidget.setValue(0.0f);
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
                    skipFactorWidget.setEnabled(true);
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

        widgets.add(GuiUtils.topLeft(GuiUtils.topCenterBottom(widgets0,
                GuiUtils.topCenterBottom(
                    GuiUtils.leftRight(GuiUtils.top(GuiUtils.inset(showLabel,
                            new Insets(10, 0, 0, 0))),
                                       GuiUtils.top(rightComp)),
                    GuiUtils.top(
                        GuiUtils.hbox(
                            GuiUtils.rLabel("Initial Area Skip Factor:  "),
                            skipFactorWidget.getContents(false))),
                    GuiUtils.top(GuiUtils.hbox(GuiUtils.rLabel("Backward trajectory:"),
                            GuiUtils.left(backwardCbx)))),
                GuiUtils.leftRight(
                    GuiUtils.inset(GuiUtils.wrap(createTrjBtn),
                                   2),
                    GuiUtils.right(unloadBtn)))));



    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected double getInitialZPosition() {
        Real alt = null;
        GriddedSet domainSet =
            (GriddedSet) gridTrackControl.getGridDataInstance()
            .getSpatialDomain();
        // if(zunit.getIdentifier().length() == 0) {
        try {
            alt = GridUtil.getAltitude(
                domainSet, (Real) ((TwoFacedObject) currentLevel).getId());
        } catch (Exception e) {}
        if (alt == null) {
            return 0.0;
        }
        return alt.getValue();
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public double getZPosition() {
        NavigatedDisplay navDisplay = getNavigatedDisplay();
        double[]         rg         = navDisplay.getVerticalRange();
        double           z          = getInitialZPosition();
        double           zz         = -1.0 + (z / (rg[1] - rg[0])) * 2.0;
        return zz;
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
        if (gridTrackControl != null) {
            gridTrackControl.skipFactor = value;
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
                if (gridTrackControl.myDisplay != null) {
                    gridTrackControl.myDisplay.setData(DUMMY_DATA);

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
                if (gridTrackControl.myDisplay != null) {
                    gridTrackControl.myDisplay.setData(DUMMY_DATA);
                    // gridTrackControl.indicator.setVisible(false);
                    //gridTrackControl.timesHolder.setData(DUMMY_DATA);
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
     *
     * @param ah _more_
     * @return _more_
     */
    public void setTrackArrowHead(boolean ah) {
        trackArrowHead = ah;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getTrackArrowHead() {
        return trackArrowHead;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Integer getTrackFormType() {
        return trackFormType;
    }

    /**
     * _more_
     *
     *
     * @param ah _more_
     * @return _more_
     */
    public void setTrackFormType(Integer ah) {
        trackFormType = ah;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float getTrackOffsetValue() {
        return trackOffsetValue;
    }

    /**
     * _more_
     *
     *
     * @param ah _more_
     * @return _more_
     */
    public void setTrackOffsetValue(float ah) {
        trackOffsetValue = ah;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Color getTrackColor() {
        return trackColor;
    }

    /**
     * _more_
     *
     *
     * @param ah _more_
     * @return _more_
     */
    public void setTrackColor(Color ah) {
        trackColor = ah;
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


    /**
     * _more_
     *
     * @param dataChoice _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected DataInstance doMakeDataInstance(DataChoice dataChoice)
            throws RemoteException, VisADException {
        gridDataInstance = new GridDataInstance(dataChoice,
                getDataSelection(), getRequestProperties(), null);
        return gridDataInstance;

    }

    /**
     * _more_
     *
     * @param
     *
     * @return _more_
     */
    public GridDataInstance getGridDataInstance() {
        if (this.gridDataInstance == null) {
            try {
                doMakeDataInstance(this.dataChoice);
            } catch (Exception exce) {}
        }
        return gridDataInstance;

    }

}
