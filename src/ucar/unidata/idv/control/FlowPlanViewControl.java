/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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
import ucar.unidata.data.grid.GridTrajectory;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.*;

import ucar.visad.Util;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.WindBarbDisplayable;

import visad.*;

import visad.georef.EarthLocation;


import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 * A plan view control for flow data (vector or wind barbs)
 *
 * @author IDV Development Team
 */
public class FlowPlanViewControl extends PlanViewControl implements FlowDisplayControl {

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

    /** a component to change the vector arrow head size */
    ValueSliderWidget vectorAHSizeWidget;

    /** a component to change the skip */
    ValueSliderWidget skipFactorWidget;

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

    /** cvector button */
    private JRadioButton cvectorBtn;

    /** flag for streamlines */
    boolean isStreamlines = false;

    /** _more_ */
    boolean isVectors = true;

    /** _more_ */
    boolean isTrajectories = false;

    /** _more_ */
    boolean isCVectors = false;

    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** flag for wind barbs */
    boolean isThreeComponents = false;

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

    /** _more_ */
    JCheckBox arrowCbx;

    /** _more_ */
    JComboBox trajFormBox;

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

    /**
     * Create a new FlowPlanViewControl; set attribute flags
     */
    public FlowPlanViewControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH);  //| FLAG_SMOOTHING);
    }

    /**
     * Method to call if projection changes. Handle flowscale.
     */
    public void projectionChanged() {
        super.projectionChanged();
        setFlowScale(flowScaleValue);
    }

    /**
     * Get the color table widget label text.
     * @return  the label text.
     */
    public String getColorWidgetLabel() {
        return "Color";
    }

    /**
     * Get the displayable for depicting the data
     * @return  the FlowDisplayable
     */
    FlowDisplayable getGridDisplay() {
        return (FlowDisplayable) getPlanDisplay();
    }

    /**
     * Create the {@link ucar.visad.display.Displayable} for the data depiction.
     * @return the Displayable.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
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
            if (getMultipleIsTopography()) {
                planDisplay.setIgnoreExtraParameters(true);
            }
            planDisplay.set3DFlow(true);
        }
        planDisplay.setUseSpeedForColor(useSpeedForColor);
        planDisplay.setColoredByAnother(coloredByAnother);
        planDisplay.setStreamlinesEnabled(isStreamlines);
        planDisplay.setStreamlineDensity(streamlineDensity);
        planDisplay.setAutoScale(autoSize);
        if (isCVectors) {
            planDisplay.setTrojectoriesEnabled(isCVectors,
                    arrowHeadSizeValue, true);
        } else {
            planDisplay.setTrojectoriesEnabled(isTrajectories,
                    arrowHeadSizeValue, false);
        }
        //addAttributedDisplayable(planDisplay, FLAG_SKIPFACTOR);
        addAttributedDisplayable(planDisplay);
        return planDisplay;
    }

    /**
     * Called to initialize this control from the given dataChoice;
     * override super class instance to set skip factor before displaying data.
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
        Trace.call1("FlowPlanView.setData");
        // checking the grid size matching
        //if (dataChoice.getDescription().contains("3D Flow Vectors")) {
        if(dataChoice instanceof DerivedDataChoice) {
            DerivedDataChoice ddc      = (DerivedDataChoice) dataChoice;
            List              choices0 = ddc.getChoices();

            if (choices0.size() == 3 && choices0.get(0) instanceof DirectDataChoice) {
                DirectDataChoice udc = (DirectDataChoice) choices0.get(0);
                DirectDataChoice vdc = (DirectDataChoice) choices0.get(1);
                DirectDataChoice wdc = (DirectDataChoice) choices0.get(2);
                ThreeDSize us = (ThreeDSize) udc.getProperty("prop.gridsize");
                ThreeDSize ws = (ThreeDSize) wdc.getProperty("prop.gridsize");
                if (us.getSizeZ() != ws.getSizeZ()) {
                    userErrorMessage("Grid sizes are different: " + ws
                                     + "\n from " + us);
                    return false;
                }
            }
        }


        FlowDisplayable fd = getGridDisplay();
        fd.setActive(false);
        boolean result = super.setData(dataChoice);

        if (this.getDisplayName().contains("Colored by Another")
                && coloredByAnother) {
            colorIndex = 2;
        }

        if ( !result) {
            Trace.call2("FlowPlanView.setData");
            return false;
        }
        if ( !getWindbarbs()) {
            setFlowRange();
        }
        // If not u/v, always color by speed.
        if ( !useSpeedForColor) {
            useSpeedForColor = !fd.isCartesianWind();
        }
        //fd.setUseSpeedForColor(useSpeedForColor);
        if (useSpeedForColor) {
            colorIndex = 2;  //fd.getSpeedTypeIndex();
        }

        if (isTrajectories) {
            fd.setTrajFormType(getTrajFormType());
        }
        // end color by speed.
        setFlowScale(flowScaleValue);
        if (getGridDisplay() != null) {
            getGridDisplay().setArrowHead(arrowHead);
            getGridDisplay().setTrajOffset(trajOffsetValue);
            getGridDisplay().setVectorLength(vectorLengthValue);
            getGridDisplay().setArrowHeadSize(arrowHeadSizeValue);
        }
        //        setSkipValue(skipValue);
        fd.setActive(true);
        Trace.call2("FlowPlanView.setData");
        return true;
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
     * @param
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
     * Get control widgets special to this control.
     *
     * @param controlWidgets   list of control widget from the superclass
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {

        skipFactorWidget = new ValueSliderWidget(this, 0, 10, "skipValue",
                getSkipWidgetLabel());
        addRemovable(skipFactorWidget);

        barbSizeWidget = new ValueSliderWidget(this, 1, 21, "flowScale",
                "Size");
        addRemovable(barbSizeWidget);

        JCheckBox autoSizeCbx = new JCheckBox("Autosize", autoSize);
        arrowCbx = new JCheckBox("Arrow", arrowHead);
        autoSizeCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoSize = ((JCheckBox) e.getSource()).isSelected();
                getGridDisplay().setAutoScale(autoSize);
            }
        });

        arrowCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                arrowHead = ((JCheckBox) e.getSource()).isSelected();
                getGridDisplay().setArrowHead(arrowHead);
                getGridDisplay().resetTrojectories();
            }
        });

        sizeComponent = GuiUtils.hbox(GuiUtils.rLabel("Size: "),
                                      barbSizeWidget.getContents(false),
                                      autoSizeCbx);
        if ( !getIsThreeComponents()) {
            streamlinesBtn = new JRadioButton("Streamlines:", isStreamlines);
            vectorBtn      = new JRadioButton((isWindBarbs
                    ? "Wind Barbs:"
                    : "Vectors:"), isVectors);
            trajLengthWidget = new ValueSliderWidget(this, 1, 21,
                    "trajOffset", "LengthOffset");
            List<TwoFacedObject> trajFormList =
                TwoFacedObject.createList(trajForm, trajFormLabels);
            trajFormBox = new JComboBox();
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
            cvectorLengthWidget = new ValueSliderWidget(this, 1, 21,
                    "VectorLength", "Curly Vector Length");
            cvectorLengthComponent =
                GuiUtils.hbox(GuiUtils.rLabel("Vector Length: "),
                              cvectorLengthWidget.getContents(false));

            trajectoryBtn = new JRadioButton("Trajectories:", isTrajectories);
            cvectorBtn    = new JRadioButton("Curly Vectors:", isCVectors);
            trajectoryBtn.setToolTipText(
                "Require mininmum four time steps for this display");
            cvectorBtn.setToolTipText(
                "Require mininmum four time steps for this display");
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JRadioButton source = (JRadioButton) e.getSource();
                    if (source == streamlinesBtn) {
                        isStreamlines  = true;
                        isVectors      = false;
                        isTrajectories = false;
                        isCVectors     = false;
                    } else if (source == trajectoryBtn) {
                        isTrajectories = true;
                        isStreamlines  = false;
                        isVectors      = false;
                        isCVectors     = false;
                    } else if (source == cvectorBtn) {
                        isCVectors     = true;
                        isTrajectories = false;
                        isStreamlines  = false;
                        isVectors      = false;
                    } else {
                        isVectors      = true;
                        isStreamlines  = false;
                        isTrajectories = false;
                        isCVectors     = false;
                    }
                    setStreamlines();
                }
            };
            streamlinesBtn.addActionListener(listener);
            vectorBtn.addActionListener(listener);
            trajectoryBtn.addActionListener(listener);
            cvectorBtn.addActionListener(listener);
            GuiUtils.buttonGroup(streamlinesBtn, vectorBtn, trajectoryBtn,
                                 cvectorBtn);
            densityLabel     = GuiUtils.rLabel("Density: ");
            densityComponent = doMakeDensityComponent();
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
                                        streamlinesBtn,
                                        GuiUtils.inset(
                                            GuiUtils.hbox(
                                                densityLabel,
                                                densityComponent), spacer))), GuiUtils.left(
                                                    GuiUtils.vbox(
                                                        trajectoryBtn,
                                                        GuiUtils.inset(
                                                            trajLengthComponent,
                                                            spacer))), GuiUtils.left(
                                                                GuiUtils.vbox(
                                                                    cvectorBtn,
                                                                    GuiUtils.inset(
                                                                        cvectorLengthComponent,
                                                                        spacer))));
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

            /*
            JCheckBox toggle = new JCheckBox("Show", isStreamlines);
            toggle.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    isStreamlines = ((JCheckBox) e.getSource()).isSelected();
                    getGridDisplay().setStreamlinesEnabled(isStreamlines);
                    enableBarbSizeBox();
                    enableDensityComponents();
                }
            });
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Streamlines:"),
                    GuiUtils.hbox(toggle, densityComponent)));

            controlWidgets.add(new WrapperWidget(this, densityLabel,
            densityComponent));*/
        } else {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel(getSizeLabel()),
                    GuiUtils.left(sizeComponent)));
        }

        vectorAHSizeWidget = new ValueSliderWidget(this, 0, 40,
                "ArrowHeadSize", "Arrow Head Size", 10.0f);

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Arrow Scale: "),
                    vectorAHSizeWidget.getContents(false)));

        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Skip:"),
                GuiUtils.left(
                    GuiUtils.hbox(
                        GuiUtils.rLabel("XY:  "),
                        skipFactorWidget.getContents(false)))));

        if ( !getWindbarbs()) {
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Range:"), doMakeFlowRangeComponent()));
        }

        enableBarbSizeBox();
        enableTrajLengthBox();
        enableCVectorLengthBox();
        List timeL = getDataSelection().getTimes();
        if ((timeL == null) && getHadDataChoices()) {
            List dchoices = getMyDataChoices();
            timeL = ((DataChoice) dchoices.get(0)).getSelectedDateTimes();
            if ((timeL != null) && (timeL.size() == 0)) {
                timeL = ((DataChoice) dchoices.get(0)).getAllDateTimes();
            }
        }
        if ((timeL != null) && (timeL.size() < 4)) {
            GuiUtils.enableTree(cvectorBtn, false);
            GuiUtils.enableTree(trajectoryBtn, false);
        }
        super.getControlWidgets(controlWidgets);
    }

    /**
     * Create the streamline density slider
     *
     * @return The panel that shows the streamline density slider
     */
    protected JComponent doMakeDensityComponent() {
        int sliderPos   = (int) (getStreamlineDensity() * 100);
        int DENSITY_MIN = 10;
        int DENSITY_MAX = 500;
        sliderPos = Math.min(Math.max(sliderPos, DENSITY_MIN), DENSITY_MAX);
        densitySlider = GuiUtils.makeSlider(DENSITY_MIN, DENSITY_MAX,
                                            sliderPos, this,
                                            "densitySliderChanged");
        densitySlider.setToolTipText(
            "Control the density of the streamlines");

        return GuiUtils.doLayout(new Component[] { GuiUtils.rLabel("Low "),
                densitySlider, GuiUtils.lLabel(" High"),
                GuiUtils.filler() }, 4, GuiUtils.WT_NYNY, GuiUtils.WT_N);
    }




    /**
     * Create the streamline density slider
     *
     * @return The panel that shows the streamline density slider
     */
    protected JComponent doMakeFlowRangeComponent() {
        setFlowRangeLabel();
        JButton editButton =
            GuiUtils.getImageButton("/ucar/unidata/idv/images/edit.gif",
                                    getClass());
        editButton.setToolTipText("Range used for scaling the vector size");
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                if (rangeDialog == null) {
                    rangeDialog = new RangeDialog(FlowPlanViewControl.this,
                            flowRange,
                            "Set the range of data for sizing vectors",
                            "setFlowRange");
                    addRemovable(rangeDialog);
                }
                rangeDialog.showDialog();
                setFlowRangeLabel();
            }
        });
        return GuiUtils.hbox(flowRangeLabel, editButton);
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
        try {
            if (dataId.equals(SHARE_FLOWRANGE)) {
                setFlowRange((Range) data[0]);
            } else if (dataId.equals(SHARE_FLOWSCALE)) {
                setFlowScale(((Float) data[0]).floatValue());
            } else {
                super.receiveShareData(from, dataId, data);
            }
        } catch (Exception exc) {
            logException("Error processing shared state: " + dataId, exc);
        }
    }

    /**
     * Set the range label
     */
    private void setFlowRangeLabel() {
        if (flowRangeLabel == null) {
            flowRangeLabel = new JLabel(" ", SwingConstants.RIGHT);

        }
        Range r = getFlowRange();
        if (r != null) {
            flowRangeLabel.setText(" " + r.formatMin() + " to "
                                   + r.formatMax());
        } else {
            flowRangeLabel.setText(" Undefined");
        }
    }

    /**
     * The streamline density slider changed
     *
     * @param value slider value
     */
    public void densitySliderChanged(int value) {
        try {
            setStreamlineDensity((float) (value / 100.), true);
        } catch (Exception exc) {
            logException("Setting streamline density ", exc);
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
     * enable the barb size box
     */
    private void enableBarbSizeBox() {
        if (sizeComponent != null) {
            GuiUtils.enableTree(sizeComponent, isVectors);
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
     * _more_
     */
    private void enableCVectorLengthBox() {
        if (cvectorLengthComponent != null) {
            GuiUtils.enableTree(cvectorLengthComponent, isCVectors);
        }
    }

    /**
     * Get the appropriate size label for this instance.
     * @return the label
     */
    private String getSizeLabel() {
        return (isWindBarbs)
               ? "Barb Size: "
               : "Vector Size: ";
    }

    /**
     * Wrapper around {@link #addTopographyMap(int)} to allow subclasses
     * to set their own index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void addTopographyMap() throws VisADException, RemoteException {
        addTopographyMap(isThreeComponents
                         ? 3
                         : 2);
    }

    /**
     * Set the streamline property.
     * Used by XML persistence
     *
     */
    public void setStreamlines() {
        //isStreamlines = v;
        if (getGridDisplay() != null) {
            getGridDisplay().setStreamlinesEnabled(isStreamlines);
            if (isCVectors) {
                trajFormType = 0;
                trajFormBox.setSelectedIndex(0);
                getGridDisplay().setTrajFormType(0);
                getGridDisplay().setIsTrajectories(true);
                getGridDisplay().setTrojectoriesEnabled(isCVectors, true,
                        arrowHeadSizeValue, true);
                arrowHead = true;
            } else if (isTrajectories) {
                arrowHead = arrowCbx.isSelected();
                getGridDisplay().setArrowHead(arrowHead);
                getGridDisplay().setTrajFormType(trajFormType);
                getGridDisplay().setIsTrajectories(true);
                getGridDisplay().setTrojectoriesEnabled(isTrajectories,
                        false, arrowHeadSizeValue, false);
            } else {
                // vector or streamline
                getGridDisplay().setTrajFormType(0);
                getGridDisplay().setIsTrajectories(false);
                getGridDisplay().setTrojectoriesEnabled(false, false,
                        arrowHeadSizeValue, false);
            }

            //getGridDisplay().setArrowHead(arrowHead);
            //getGridDisplay().setTrajFormType(trajFormType);
            //getGridDisplay().resetTrojectories();
            enableBarbSizeBox();
            enableDensityComponents();
            enableTrajLengthBox();
            enableCVectorLengthBox();
        }
        if (streamlinesBtn != null) {
            streamlinesBtn.setSelected(isStreamlines);
            vectorBtn.setSelected(isVectors);
            trajectoryBtn.setSelected(isTrajectories);
            cvectorBtn.setSelected(isCVectors);
        }

    }


    /**
     * _more_
     *
     * @param v _more_
     */
    public void setStreamlines(boolean v) {
        isStreamlines = v;
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setTrajectories(boolean v) {
        isTrajectories = v;
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setVectors(boolean v) {
        isVectors = v;
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setCVectors(boolean v) {
        isCVectors = v;

    }

    /**
     * Get the streamline property.
     * Used by XML persistence
     *
     * @return  the current streamline property (true if streamlines)
     */
    public boolean getStreamlines() {
        return isStreamlines;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getTrajectories() {
        return isTrajectories;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getCVectors() {
        return isCVectors;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getVectors() {
        return isVectors;
    }

    /**
     * Set the skip value
     *
     * @param value the value
     */
    public void setSkipValue(int value) {
        super.setSkipValue(value);
        FlowDisplayable fd = getGridDisplay();
        if (skipFactorWidget != null) {
            skipFactorWidget.setValue(value);
        }
        if (fd != null) {
            fd.setUseSpeedForColor(useSpeedForColor);
            fd.setColoredByAnother(coloredByAnother);
            if (useSpeedForColor || coloredByAnother) {
                colorIndex = 2;  //fd.getSpeedTypeIndex();
            }
        }
    }


    /**
     * Set the wind barb property.
     * Used by XML persistence
     *
     * @param v   true if you want to use wind barbs
     */
    public void setWindbarbs(boolean v) {
        isWindBarbs = v;
    }

    /**
     * Get the wind barb property.
     * Used by XML persistence
     *
     * @return  true if wind barbs are being depicted
     */
    public boolean getWindbarbs() {
        return isWindBarbs;
    }

    /**
     * Set the use 3 components property.
     * Used by XML persistence
     *
     * @param v   true if using u, v and w
     */
    public void setIsThreeComponents(boolean v) {
        isThreeComponents = v;
    }

    /**
     * Get the use 3 components property.
     * Used by XML persistence
     *
     * @return  true if using 3 components for wind
     */
    public boolean getIsThreeComponents() {
        return isThreeComponents;
    }

    /**
     * Set the scale factor.
     * Used by XML persistence
     *
     * @param s   the new factor
     */
    public void setScaleFactor(float s) {
        //No op
    }

    /**
     * Add DisplaySettings appropriate for this display
     *
     * @param dsd  the dialog to add to
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        dsd.addPropertyValue(new Double(flowScaleValue), "flowScale",
                             "Scale", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Double(trajOffsetValue), "trajOffset",
                             "Offset Length", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Integer(trajFormType), "trajFormType",
                             "Traj Form", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Integer(getSkipValue()), "skipValue",
                             "Skip Factor", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Double(getStreamlineDensity()),
                             "streamlineDensity", "Streamline Density",
                             SETTINGS_GROUP_DISPLAY);

        dsd.addPropertyValue(flowRange, "flowRange", "Flow Field Range",
                             SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Boolean(getStreamlines()), "streamlines",
                             "Show Streamlines", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Boolean(getTrajectories()), "trajectory",
                             "Show Trajectory", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Boolean(getCVectors()), "curlyvector",
                             "Show Curly Vector", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Boolean(getAutoSize()), "autoSize",
                             "Autosize", SETTINGS_GROUP_DISPLAY);



    }

    /**
     * Get the autosize property
     * Used by XML persistence
     *
     * @return  the autosize for this control
     */
    public boolean getAutoSize() {
        return autoSize;
    }

    /**
     * Set the autosize property
     * Used by XML persistence
     *
     * @param auto   new autosize value
     */
    public void setAutoSize(boolean auto) {
        autoSize = auto;
        if (getGridDisplay() != null) {
            getGridDisplay().setAutoScale(autoSize);
        }
    }

    /**
     * Get the arrowHead property
     * Used by XML persistence
     *
     * @return  the autosize for this control
     */
    public boolean getArrowHead() {
        return arrowHead;
    }

    /**
     * Set the autosize property
     * Used by XML persistence
     *
     * @param  arrow value
     */
    public void setArrowHead(boolean arrow) {
        arrowHead = arrow;
        if (getGridDisplay() != null) {
            getGridDisplay().setArrowHead(arrowHead);
        }
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
        if (getHaveInitialized()) {
            doShare(SHARE_FLOWRANGE, flowRange);
        }
        if (barbSizeWidget != null) {
            barbSizeWidget.setValue(f);
        }
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public void setTrajOffset(float f) {
        trajOffsetValue = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setTrajOffset(trajOffsetValue);
                getGridDisplay().resetTrojectories();
                getGridDisplay().setArrowHead(arrowHead);
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
     * @param f _more_
     */
    public void setVectorLength(float f) {
        vectorLengthValue = f;
        if (getGridDisplay() != null) {
            try {
                arrowHead = true;
                getGridDisplay().setArrowHead(arrowHead);
                getGridDisplay().setVectorLength(vectorLengthValue);
                getGridDisplay().setArrowHeadSize(arrowHeadSizeValue);
                getGridDisplay().resetTrojectories();
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

        if (cvectorLengthWidget != null) {
            cvectorLengthWidget.setValue(f);
        }
    }

    /**
     * _more_
     *
     * @param f _more_
     */
    public void setArrowHeadSize(float f) {
        arrowHeadSizeValue = f;
        if (getGridDisplay() != null) {
            try {
                getGridDisplay().setTrajOffset(vectorLengthValue);
                getGridDisplay().setArrowHeadSize(arrowHeadSizeValue);
                if (isTrajectories || isCVectors) {
                    getGridDisplay().resetTrojectories();
                }
            } catch (Exception ex) {
                logException("setFlowScale: ", ex);
            }

        }

        if (vectorAHSizeWidget != null) {
            vectorAHSizeWidget.setValue(f);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float getVectorLength() {
        return vectorLengthValue;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public float getArrowHeadSize() {
        return arrowHeadSizeValue;
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
        if ((getGridDisplay() != null) && (flowRange != null)
                && !getWindbarbs()) {
            try {
                getGridDisplay().setFlowRange(flowRange);
            } catch (Exception excp) {
                logException("setFlowRange: ", excp);
            }
        }
        setFlowRangeLabel();
        if (getHaveInitialized()) {
            doShare(SHARE_FLOWRANGE, flowRange);
        }
    }

    /**
     * Get the streamline density.
     * Used by XML persistence
     *
     * @return  the streamline density for this control
     */
    public float getStreamlineDensity() {
        return streamlineDensity;
    }

    /**
     * Set the streamline density.
     * Used by XML persistence
     *
     * @param f   new flow scale
     */
    public void setStreamlineDensity(float f) {
        setStreamlineDensity(f, false);
    }


    /**
     * Set the streamline density
     *
     * @param f value
     * @param fromSlider true if from slider
     */
    public void setStreamlineDensity(float f, boolean fromSlider) {
        streamlineDensity = f;
        if (getGridDisplay() != null) {
            getGridDisplay().setStreamlineDensity(f);
        }

        if ( !fromSlider && (densitySlider != null)) {
            densitySlider.setValue((int) (f * 100));
        }
    }


    /**
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        if (getGridDataInstance() == null) {
            return false;
        }
        return ((getGridDataInstance().getNumRealTypes()
                 > ((getIsThreeComponents())
                    ? 3
                    : 2)) && !getMultipleIsTopography()) || useSpeedForColor;
    }

    /**
     * Returns the index to use in the GridDataInstance array of ranges
     * for color ranges.
     *
     * @return  The index to be used for the color range.
     */
    protected int getColorRangeIndex() {
        if (colorIndex >= 0) {
            return colorIndex;
        }
        if (getMultipleIsTopography()) {
            return 0;
        }
        return getIsThreeComponents()
               ? 3
               : 2;
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
     * Set the range for the flow components
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setFlowRange() throws RemoteException, VisADException {
        if ((getGridDisplay() != null)) {  //&& !getWindbarbs()) {
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

                    for (int i = startComp; i < numComps; i++) {
                        Range compRange = ranges[i];
                        max = Math.max(compRange.getMax(), max);
                        //min = Math.min(compRange.getMin(), min);
                        min = Math.min(compRange.getMin(), min);
                    }

                    if (useSpeedForColor || coloredByAnother) {
                        Range compRange = ranges[ranges.length - 1];
                        flowColorRange = compRange;
                    }

                    if ( !Double.isInfinite(max) && !Double.isInfinite(min)) {
                        max = Math.max(max, -min);
                        min = isCartesian
                              ? -max
                              : 0;
                    }
                    //System.out.println("setFlowRange: " + min + " to " + max);
                    //getGridDisplay().setFlowRange(min,max);
                    setFlowRange(new Range(min, max));
                } else {  // gotta set it to something
                    setFlowRange(new Range(-40, 40));
                }
            } else {
                getGridDisplay().setFlowRange(flowRange);
            }
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
    public Range getRangeForColorTable()
            throws RemoteException, VisADException {
        if (getFlowColorRange() == null) {
            setFlowRange();
        }
        return flowColorRange;
    }


    /**
     * Show the color control widget in the widgets if FLAG_COLOR is set.
     * @return  false  subclasses should override
     */
    public boolean showColorControlWidget() {
        return !haveMultipleFields() && !useSpeedForColor
               && !coloredByAnother;
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
     * _more_
     *
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
    }
    /**
     * Get the cursor data
     *
     * @param el  earth location
     * @param animationValue   the animation value
     * @param animationStep  the animation step
     * @param samples the list of sample readouts
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
        RealTuple r = GridUtil.sampleToRealTuple(
                          currentSlice, el, animationValue,
                          getSamplingModeValue(
                              getObjectStore().get(
                                  PREF_SAMPLING_MODE,
                                  DEFAULT_SAMPLING_MODE)));
        if (r != null) {
            ReadoutInfo readoutInfo = new ReadoutInfo(this, r, el,
                                          animationValue);
            readoutInfo.setUnit(getDisplayUnit());
            readoutInfo.setRange(getRange());
            samples.add(readoutInfo);
        }

        if ((r != null) && !Util.allMissing(r)) {

            result.add("<tr><td>" + getMenuLabel()
                       + ":</td><td  align=\"right\">"
                       + formatForCursorReadout(r) + ((currentLevel != null)
                    ? ("@" + currentLevel)
                    : "") + "</td></tr>");
        }
        return result;
    }

    /**
     * Format a real for the cursor readout
     *
     * @param rt the realtuple
     *
     * @return  the formatted string
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException  VisAD error
     */
    protected String formatForCursorReadout(RealTuple rt)
            throws VisADException, RemoteException {
        Unit   displayUnit = getDisplayUnit();
        double value;
        Unit   unit = null;
        String result;
        if (Util.allMissing(rt)) {
            result = "missing";
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < rt.getDimension(); i++) {
                Real r = (Real) rt.getComponent(i);
                if (displayUnit != null) {
                    value = r.getValue(displayUnit);
                    unit  = displayUnit;
                } else {
                    value = r.getValue();
                    unit  = r.getUnit();
                }
                builder.append(Misc.format(value));
                if (i != rt.getDimension() - 1) {
                    builder.append("/");
                }
            }
            builder.append("[");
            builder.append(unit);
            builder.append("]");
            result = builder.toString();
            int length = result.length();
            result = StringUtil.padLeft(result, 8 * (20 - length), "&nbsp;");
        }

        return result;
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
                    int width = getLineWidth();
                    getGridDisplay().setTrajFormType(trajForm.intValue());
                    getGridDisplay().setArrowHead(arrowHead);
                    getGridDisplay().setRibbonWidth(width);
                    getGridDisplay().setTrajWidth(width * 0.01f);
                    getGridDisplay().resetTrojectories();
                } catch (Exception ex) {
                    logException("setTrajFormType: ", ex);
                }

            }
        }
    }

    /**
     * _more_
     *
     * @param width _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setLineWidth(int width)
            throws RemoteException, VisADException {
        width = (width < 1)
                ? 1
                : width;
        if (isTrajectories) {
            if (trajFormType == 2) {
                getGridDisplay().setTrajWidth(width * 0.01f);
            } else if ((trajFormType == 1) || (trajFormType == 3)) {
                getGridDisplay().setRibbonWidth(width);
            }

            getGridDisplay().resetTrojectories();
        }

        super.setLineWidth(width);
    }

}
