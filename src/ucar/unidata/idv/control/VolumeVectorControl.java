/*
 * Copyright 1997-2015 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DerivedDataChoice;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;
import ucar.unidata.util.*;
import ucar.unidata.view.geoloc.MapProjectionDisplay;

import ucar.visad.Util;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.WindBarbDisplayable;
import ucar.visad.display.ZSelector;

import visad.*;

import visad.georef.MapProjection;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * Created by yuanho on 4/5/15.
 */
public class VolumeVectorControl extends GridDisplayControl implements FlowDisplayControl {

    /**
     * the display for the volume renderer
     */
    FlowDisplayable myDisplay;

    /** data choice for the data */
    protected DataChoice datachoice;

    /** a component to change the barb size */
    ValueSliderWidget barbSizeWidget;

    /** vector/barb size component */
    JComponent sizeComponent;

    /** a component to change the traj size */
    ValueSliderWidget trajLengthWidget;

    /** vector/traj length component */
    JComponent trajLengthComponent;

    /** a component to change the cvector size */
    ValueSliderWidget cvectorLengthWidget;

    /** cvector length component */
    JComponent cvectorLengthComponent;

    /** a component to change the cvector arrow head size */
    ValueSliderWidget cvectorAHLengthWidget;

    /** a component to change the skip */
    ValueSliderWidget skipFactorWidget;

    /** _more_ */
    ValueSliderWidget skipFactorWidgetZ;

    /** a component to change the streamline density */
    JComponent densityComponent;

    /** streamline density slider */
    JSlider densitySlider;

    /** a label listing the range of the data */
    JLabel flowRangeLabel;

    /** the density label */
    private JLabel densityLabel;

    /** streamlines button */
    private JRadioButton streamlinesBtn;

    /** vector/barb button */
    private JRadioButton vectorBtn;

    /** trajectory button */
    private JRadioButton trajectoryBtn;

    /** flag for streamlines */
    boolean isStreamlines = false;

    /** _more_ */
    boolean isVectors = true;

    /** _more_ */
    boolean isTrajectories = false;

    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** flag for wind barbs */
    boolean isThreeComponents = true;

    /** autoscale */
    boolean autoSize = false;

    /** arrow */
    boolean arrowHead = false;

    /** a scale factor */
    protected final float scaleFactor = 0.02f;

    /** a scale value */
    float flowScaleValue = 4.0f;

    /** a traj offset value */
    float vectorLengthValue = 2.0f;

    /** a traj offset value */
    float arrowHeadSizeValue = 1.0f;

    /** a traj offset value */
    float trajOffsetValue = 4.0f;

    /** streamline density value */
    float streamlineDensity = 1.0f;

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
            "Ribbon", "Cylinder", "Deform Ribbon" };

    /** types of smoothing functions */
    private final static int[] trajForm = new int[] { 0, 1, 2, 3 };

    /** vector/traj length component */
    JComponent trajFormComponent;

    /** default type */
    private Integer trajFormType = new Integer(0);

    /** _more_ */
    private Range flowColorRange;

    /** _more_ */
    private int skipValueZ = 0;

    /**
     * Default constructor; does nothing.
     */
    public VolumeVectorControl() {
        //setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH | FLAG_SMOOTHING);
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
        if ( !isDisplay3D()) {
            LogUtil.userMessage(log_, "Can't render volume in 2D display");
            return false;
        }

        // checeking grid size matching between u and w
        if (dataChoice instanceof DerivedDataChoice) {
            DerivedDataChoice ddc      = (DerivedDataChoice) dataChoice;
            List              choices0 = ddc.getChoices();
            if (choices0.size() == 3) {
                DirectDataChoice udc = (DirectDataChoice) choices0.get(0);
                DirectDataChoice vdc = (DirectDataChoice) choices0.get(1);
                DirectDataChoice wdc = (DirectDataChoice) choices0.get(2);
                ThreeDSize us = (ThreeDSize) udc.getProperty("prop.gridsize");
                ThreeDSize ws = (ThreeDSize) wdc.getProperty("prop.gridsize");
                if (us.getSizeZ() != ws.getSizeZ()) {
                    userErrorMessage("w grid size is different: " + ws
                                     + "\n from " + us);
                    return false;
                }
            }
        }
        myDisplay = (FlowDisplayable) createPlanDisplay();


        myDisplay.setPointSize(getPointSize());
        addDisplayable(myDisplay, getAttributeFlags());

        //Now, set the data. Return false if it fails.
        if ( !setData(dataChoice)) {
            return false;
        }


        //Now set up the flags and add the displayable
        return true;
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
        if (isWindBarbs) {
            planDisplay =
                new WindBarbDisplayable("FlowPlanViewControl_windbarbs_"
                                        + ((datachoice != null)
                                           ? datachoice.toString()
                                           : ""), null);
        } else {
            planDisplay = new FlowDisplayable("FlowPlanViewControl_vectors_"
                    + ((datachoice != null)
                       ? datachoice.toString()
                       : ""), null);

            planDisplay.set3DFlow(true);
        }
        planDisplay.setStreamlinesEnabled(isStreamlines);
        planDisplay.setStreamlineDensity(streamlineDensity);
        planDisplay.setAutoScale(autoSize);
        planDisplay.setUseSpeedForColor(useSpeedForColor);
        planDisplay.setTrojectoriesEnabled(isTrajectories,
                                           arrowHeadSizeValue, false);

        if (useSpeedForColor || coloredByAnother) {
            addAttributedDisplayable(planDisplay, FLAG_COLORTABLE);
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

        skipFactorWidget = new ValueSliderWidget(this, 0, 10, "skipValue",
                getSkipWidgetLabel());

        skipFactorWidgetZ = new ValueSliderWidget(this, 0, 10, "skipValueZ",
                getSkipWidgetLabel());

        addRemovable(skipFactorWidget);
        addRemovable(skipFactorWidgetZ);

        barbSizeWidget = new ValueSliderWidget(this, 1, 21, "flowScale",
                "Size");
        addRemovable(barbSizeWidget);

        JCheckBox autoSizeCbx = new JCheckBox("Autosize", autoSize);
        JCheckBox arrowCbx    = new JCheckBox("Arrow", arrowHead);
        autoSizeCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoSize = ((JCheckBox) e.getSource()).isSelected();
                getGridDisplay().setAutoScale(autoSize);
            }
        });

        arrowCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                arrowHead = ((JCheckBox) e.getSource()).isSelected();
                if (arrowHead) {
                    getGridDisplay().setArrowHead(arrowHead);
                } else {
                    getGridDisplay().setArrowHead(arrowHead);
                }
                getGridDisplay().resetTrojectories();
            }
        });

        sizeComponent = GuiUtils.hbox(GuiUtils.rLabel("Size: "),
                                      barbSizeWidget.getContents(false),
                                      autoSizeCbx);
        if (getIsThreeComponents()) {

            vectorBtn = new JRadioButton((isWindBarbs
                                          ? "Wind Barbs:"
                                          : "Vectors:"), isVectors);
            trajLengthWidget = new ValueSliderWidget(this, 1, 21,
                    "trajOffset", "LengthOffset");

            List<TwoFacedObject> trajFormList =
                TwoFacedObject.createList(trajForm, trajFormLabels);
            JComboBox trajFormBox = new JComboBox();
            GuiUtils.setListData(trajFormBox, trajFormList);
            trajFormBox.setSelectedItem(
                TwoFacedObject.findId(getTrajFormType(), trajFormList));
            trajFormBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    TwoFacedObject select =
                        (TwoFacedObject) ((JComboBox) e.getSource())
                            .getSelectedItem();
                    setTrajFormType(select.getId().hashCode());
                }
            });
            trajFormComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Trajecotry Form: "),
                              GuiUtils.filler(), trajFormBox,
                              GuiUtils.filler());

            trajLengthComponent = GuiUtils.hbox(trajFormComponent,
                    GuiUtils.rLabel("Length Offset: "),
                    trajLengthWidget.getContents(false), arrowCbx);
            // trajLengthComponent =
            //         GuiUtils.hbox(GuiUtils.rLabel("Length Offset: "),
            //                 trajLengthWidget.getContents(false), arrowCbx);

            trajectoryBtn = new JRadioButton("Trajectories:", isTrajectories);

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JRadioButton source = (JRadioButton) e.getSource();
                    if (source == trajectoryBtn) {
                        isTrajectories = true;
                        isVectors      = false;
                    } else {
                        isVectors      = true;
                        isTrajectories = false;
                    }
                    setStreamlines();
                }
            };

            vectorBtn.addActionListener(listener);
            trajectoryBtn.addActionListener(listener);
            GuiUtils.buttonGroup(vectorBtn, trajectoryBtn);
            densityLabel = GuiUtils.rLabel("Density: ");

            enableDensityComponents();
            Insets spacer = new Insets(0, 30, 0, 0);
            JComponent rightComp =
                GuiUtils.vbox(
                    GuiUtils.left(
                        GuiUtils.vbox(
                            vectorBtn,
                            GuiUtils.inset(
                                sizeComponent, spacer))), GuiUtils.left(
                                    GuiUtils.vbox(
                                        trajectoryBtn,
                                        GuiUtils.inset(
                                            trajLengthComponent, spacer))));
            JLabel showLabel = GuiUtils.rLabel("Show:");
            showLabel.setVerticalTextPosition(JLabel.TOP);
            controlWidgets.add(
                new WrapperWidget(
                    this,
                    GuiUtils.top(
                        GuiUtils.inset(
                            showLabel,
                            new Insets(10, 0, 0, 0))), GuiUtils.left(
                                GuiUtils.top(rightComp))));


        }

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Skip:"),
                GuiUtils.left(
                    GuiUtils.hbox(
                        GuiUtils.rLabel("XY:  "),
                        skipFactorWidget.getContents(false)))));
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Skip:"),
                GuiUtils.left(
                    GuiUtils.hbox(
                        GuiUtils.rLabel("Z:  "),
                        skipFactorWidgetZ.getContents(false)))));

        enableTrajLengthBox();

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
    public void setStreamlines() {
        //isStreamlines = v;
        if (getGridDisplay() != null) {
            getGridDisplay().setStreamlinesEnabled(isStreamlines);
            getGridDisplay().setIsTrajectories(isTrajectories);
            getGridDisplay().setTrojectoriesEnabled(isTrajectories,
                    arrowHeadSizeValue, false);

            enableDensityComponents();
            enableTrajLengthBox();
        }
        if (streamlinesBtn != null) {
            streamlinesBtn.setSelected(isStreamlines);
            vectorBtn.setSelected(isVectors);
            trajectoryBtn.setSelected(isTrajectories);
        }

    }

    /**
     * enable the barb size box
     */
    private void enableTrajLengthBox() {
        if (trajLengthComponent != null) {
            GuiUtils.enableTree(trajLengthComponent, isTrajectories);
        }
    }

    /**
     * enable the density slider components
     */
    private void enableDensityComponents() {
        if (densityComponent != null) {
            GuiUtils.enableTree(densityComponent, isStreamlines);
        }
        if (densityLabel != null) {
            GuiUtils.enableTree(densityLabel, isStreamlines);
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
     *  Use the value of the skip factor to subset the data.
     */
    protected void applySkipFactor() {
        try {
            showWaitCursor();
            loadVolumeData();
        } catch (Exception exc) {
            logException("loading volume data", exc);
        } finally {
            showNormalCursor();
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

        myDisplay.setActive(false);
        myDisplay.setUseSpeedForColor(useSpeedForColor);
        myDisplay.setColoredByAnother(coloredByAnother);
        if (useSpeedForColor) {
            colorIndex = myDisplay.getSpeedTypeIndex();
        }
        if (coloredByAnother) {
            colorIndex = 3;
        }

        loadVolumeData();

        if (useSpeedForColor || coloredByAnother) {
            setFlowColorRange();
        }

        myDisplay.setActive(true);
        return true;
    }


    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void setFlowColorRange() throws RemoteException, VisADException {
        if ((getGridDisplay() != null)) {
            if (getFlowRange() == null) {
                Range[] ranges = null;
                Data    data   = getGridDisplay().getData();
                if (data != null) {
                    ranges = GridUtil.getMinMax((FieldImpl) data);
                    double  max         = Double.NEGATIVE_INFINITY;
                    double  min         = Double.POSITIVE_INFINITY;
                    int     startComp   = 0;
                    int     numComps    = getIsThreeComponents()
                                          ? 3
                                          : 2;
                    boolean isCartesian = getGridDisplay().isCartesianWind();
                    //System.out.println("control thinks cartesian is " + isCartesian);
                    if ( !isCartesian) {
                        int speedIndex = getGridDisplay().getSpeedTypeIndex();
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

                    if ( !useSpeedForColor && !coloredByAnother
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

        if ((getSkipValue() > 0) && (getSkipValueZ() > 0)) {
            grid = GridUtil.subset(grid, getSkipValue() + 1,
                                   getSkipValue() + 1, getSkipValueZ() + 1);
            newGrid = grid;
        } else if (getSkipValue() > 0) {
            grid    = GridUtil.subset(grid, getSkipValue() + 1);
            newGrid = grid;
        } else if (getSkipValueZ() > 0) {
            grid    = GridUtil.subset(grid, 1, 1, getSkipValueZ() + 1);
            newGrid = grid;
        }


        Trace.call1("VRC.loadVolumeData.loadData");
        myDisplay.loadData(newGrid);
        Trace.call2("VRC.loadVolumeData.loadData");
        Trace.call2("loadVolumeData");
    }

    /**
     * Make a grid with a Linear3DSet for the volume rendering
     *
     * @param grid grid to transform
     * @param cs   coordinate system to transform to XYZ
     *
     * @return transformed grid
     *
     * @throws RemoteException  Java RMI Exception
     * @throws VisADException   problem creating grid
     */
    private FieldImpl makeLinearGrid(FieldImpl grid, CoordinateSystem cs)
            throws VisADException, RemoteException {

        Trace.call1("VRC.makeLinearGrid");
        GriddedSet domainSet   = (GriddedSet) GridUtil.getSpatialDomain(grid);
        SampledSet ss          = null;
        boolean    latLonOrder = GridUtil.isLatLonOrder(domainSet);
        //System.out.println("grid is latLonOrder " + latLonOrder);
        Trace.call1("VRC.convertDomain");
        if (latLonOrder) {
            ss = Util.convertDomain(domainSet,
                                    RealTupleType.LatitudeLongitudeAltitude,
                                    null);
        } else {
            ss = Util.convertDomain(domainSet,
                                    RealTupleType.SpatialEarth3DTuple, null);
        }
        Trace.call2("VRC.convertDomain");
        float[][] refVals = ss.getSamples(true);
        MapProjectionDisplay mpd =
            (MapProjectionDisplay) getNavigatedDisplay();
        MapProjection mp             = mpd.getMapProjection();
        boolean       mapLatLonOrder = mp.isLatLonOrder();
        //System.out.println("map is latLonOrder " + mapLatLonOrder);
        float[][] newVals = (latLonOrder)
                            ? refVals
                            : new float[][] {
            refVals[1], refVals[0], refVals[2]
        };
        Trace.call1("VRC.toRef");
        newVals = cs.toReference(newVals);
        Trace.call2("VRC.toRef");
        Trace.call1("VRC.scaleVerticalValues");
        newVals[2] = mpd.scaleVerticalValues(newVals[2]);
        Trace.call2("VRC.scaleVerticalValues");
        int[] lengths = domainSet.getLengths();
        //Misc.printArray("lengths",lengths);
        GriddedSet xyzSet =
            GriddedSet.create(RealTupleType.SpatialCartesian3DTuple, newVals,
                              domainSet.getLengths(),
                              (CoordinateSystem) null, (Unit[]) null,
                              (ErrorEstimate[]) null, false, true);
        Trace.call1("VRC.setSpatialDomain");
        FieldImpl newGrid = GridUtil.setSpatialDomain(grid, xyzSet);  //, true);
        Trace.call2("VRC.setSpatialDomain");
        float[] lows  = xyzSet.getLow();
        float[] highs = xyzSet.getHi();
        //Misc.printArray("lows",lows);
        //Misc.printArray("highs",highs);
        Linear3DSet volumeXYZ =
            new Linear3DSet(RealTupleType.SpatialCartesian3DTuple, lows[0],
                            highs[0], lengths[0], lows[1], highs[1],
                            lengths[1], lows[2], highs[2], lengths[2]);
        // System.out.println(volumeXYZ);
        Trace.call1("VRC.resampleGrid");
        newGrid = GridUtil.resampleGrid(newGrid, volumeXYZ);
        Trace.call2("VRC.resampleGrid");
        Trace.call2("VRC.makeLinearGrid");
        return newGrid;
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
                getGridDisplay().setFlowScale(getDisplayScale() * scaleFactor
                        * flowScaleValue);
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
                getGridDisplay().setTrajOffset(trajOffsetValue);
                getGridDisplay().resetTrojectories();
                if (arrowHead) {
                    getGridDisplay().setArrowHead(arrowHead);
                } else {
                    getGridDisplay().setArrowHead(arrowHead);
                }
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
     * @param v _more_
     */
    public void setTrajectories(boolean v) {
        isTrajectories = v;
        isVectors      = !v;
        setStreamlines();
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setVectors(boolean v) {
        isTrajectories = !v;
        isVectors      = v;
        setStreamlines();
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

        if (isTrajectories) {
            if (getGridDisplay() != null) {
                try {
                    getGridDisplay().setTrajFormType(trajForm.intValue());
                    //getGridDisplay().resetTrojectories();
                    getGridDisplay().setArrowHead(arrowHead);
                    //getGridDisplay().resetTrojectories();
                    setLineWidth(super.getLineWidth());
                } catch (Exception ex) {
                    logException("setFlowScale: ", ex);
                }

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
            return getGridDataInstance().getRealTypeName(colorIndex);
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
    protected Range getInitialRange() throws RemoteException, VisADException {
        if (useSpeedForColor) {
            return flowRange;
        } else if (coloredByAnother) {
            return getGridDataInstance().getRanges()[colorIndex];
        } else {
            return super.getInitialRange();
        }

    }


    /**
     * Set the skip value
     *
     * @param value the value
     */
    public void setSkipValueZ(int value) {
        skipValueZ = value;
        if (skipFactorWidgetZ != null) {
            skipFactorWidgetZ.setValue(value);
            //
        }

        if (getHaveInitialized()) {
            applySkipFactor();
            doShare(SHARE_SKIPVALUE + "Z", new Integer(skipValueZ));
        }
        /*  FlowDisplayable fd = getGridDisplay();
          if (fd != null) {
              fd.setUseSpeedForColor(useSpeedForColor);
              if (useSpeedForColor) {
                  colorIndex = fd.getSpeedTypeIndex();
              }
          }*/
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getSkipValueZ() {
        return (int) ((skipFactorWidgetZ == null)
                      ? skipValueZ
                      : skipFactorWidgetZ.getValue());

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
        if (isTrajectories && (getGridDisplay() != null)) {
            if (trajFormType == 2) {
                getGridDisplay().setTrajWidth(width * 0.01f);
            } else if ((trajFormType == 1) || (trajFormType == 3)) {
                getGridDisplay().setRibbonWidth(width);
            }

            getGridDisplay().resetTrojectories();
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
     * @return _more_
     */
    public boolean getIsTrajectories() {
        return isTrajectories;
    }

    /**
     * _more_
     *
     * @param isTrajectories _more_
     */
    public void setIsTrajectories(boolean isTrajectories) {
        this.isTrajectories = isTrajectories;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean setIsStreamlines() {
        return isStreamlines;
    }

    /**
     * _more_
     *
     * @param isStreamlines _more_
     */
    public void setIsStreamlines(boolean isStreamlines) {
        this.isStreamlines = isStreamlines;
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
        super.initAfterUnPersistence(vc, properties);
        if (isTrajectories) {
            trajectoryBtn.doClick();
            setTrajFormType(getTrajFormType());
            int width = super.getLineWidth();
            if (isTrajectories && (getGridDisplay() != null)) {
                if (trajFormType == 2) {
                    getGridDisplay().setTrajWidth(width * 0.01f);
                } else if ((trajFormType == 1) || (trajFormType == 3)) {
                    getGridDisplay().setRibbonWidth(width);
                }
            }

            getGridDisplay().resetTrojectories();
        } else {
            vectorBtn.doClick();
        }

        skipFactorWidgetZ.setValue(getSkipValueZ());
    }
}
