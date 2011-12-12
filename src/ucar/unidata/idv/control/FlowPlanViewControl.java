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


import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import ucar.unidata.collab.Sharable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.visad.Util;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.WindBarbDisplayable;
import visad.Data;
import visad.FieldImpl;
import visad.Real;
import visad.RealTuple;
import visad.Unit;
import visad.VisADException;
import visad.georef.EarthLocation;



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

    /** flag for streamlines */
    boolean isStreamlines = false;

    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** flag for wind barbs */
    boolean isThreeComponents = false;

    /** autoscale */
    boolean autoSize = false;

    /** a scale factor */
    protected final float scaleFactor = 0.02f;

    /** a scale value */
    float flowScaleValue = 4.0f;

    /** streamline density value */
    float streamlineDensity = 1.0f;

    /** slider components */
    private JComponent[] widthSliderComps;

    /** Range for flow scale */
    private Range flowRange;

    /** the range dialog */
    RangeDialog rangeDialog;

    /**
     * Create a new FlowPlanViewControl; set attribute flags
     */
    public FlowPlanViewControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH | FLAG_SMOOTHING);
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
        }
        planDisplay.setStreamlinesEnabled(isStreamlines);
        planDisplay.setStreamlineDensity(streamlineDensity);
        planDisplay.setAutoScale(autoSize);
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
        getGridDisplay().setActive(false);
        boolean result = super.setData(dataChoice);
        if ( !result) {
            Trace.call2("FlowPlanView.setData");
            return false;
        }
        if ( !getWindbarbs()) {
            setFlowRange();
        }
        setFlowScale(flowScaleValue);
        //        setSkipValue(skipValue);
        getGridDisplay().setActive(true);
        Trace.call2("FlowPlanView.setData");
        return true;
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
        autoSizeCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                autoSize = ((JCheckBox) e.getSource()).isSelected();
                getGridDisplay().setAutoScale(autoSize);
            }
        });

        sizeComponent = GuiUtils.hbox(GuiUtils.rLabel("Size: "),
                                      barbSizeWidget.getContents(false),
                                      autoSizeCbx);
        if ( !getIsThreeComponents()) {
            streamlinesBtn = new JRadioButton("Streamlines:", isStreamlines);
            vectorBtn      = new JRadioButton((isWindBarbs
                    ? "Wind Barbs:"
                    : "Vectors:"), !isStreamlines);
            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    JRadioButton source = (JRadioButton) e.getSource();
                    if (source == streamlinesBtn) {
                        setStreamlines(source.isSelected());
                    } else {
                        setStreamlines( !source.isSelected());
                    }
                }
            };
            streamlinesBtn.addActionListener(listener);
            vectorBtn.addActionListener(listener);
            GuiUtils.buttonGroup(streamlinesBtn, vectorBtn);
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
                                                densityComponent), spacer))));
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
            GuiUtils.enableTree(sizeComponent, !isStreamlines);
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
     * @param v  true to use streamlines
     */
    public void setStreamlines(boolean v) {
        isStreamlines = v;
        if (getGridDisplay() != null) {
            getGridDisplay().setStreamlinesEnabled(isStreamlines);
            enableBarbSizeBox();
            enableDensityComponents();
        }
        if (streamlinesBtn != null) {
            streamlinesBtn.setSelected(v);
            vectorBtn.setSelected( !v);
        }
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
     * Set the skip value
     *
     * @param value the value
     */
    public void setSkipValue(int value) {
        super.setSkipValue(value);
        if (skipFactorWidget != null) {
            skipFactorWidget.setValue(value);
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
        dsd.addPropertyValue(new Integer(getSkipValue()), "skipValue",
                             "Skip Factor", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Double(getStreamlineDensity()),
                             "streamlineDensity", "Streamline Density",
                             SETTINGS_GROUP_DISPLAY);

        dsd.addPropertyValue(flowRange, "flowRange", "Flow Field Range",
                             SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(new Boolean(getStreamlines()), "streamlines",
                             "Show Streamlines", SETTINGS_GROUP_DISPLAY);
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
              getGridDisplay().setFlowScale(getDisplayScale()* scaleFactor * flowScaleValue);
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
     *  Return the range attribute of the colorTable  (if non-null)
     *  else return null;
     * @return The range from the color table attribute
     */
    public Range getColorRangeFromData() {
        Range r = super.getColorRangeFromData();
        return makeFlowRange(r);
    }

    /**
     * Get the range for the current slice.
     * @return range or null
     */
    protected Range getLevelColorRange() {
        return makeFlowRange(super.getLevelColorRange());
    }

    /**
     * Make a flow range from the given range (max of abs of min and max)
     *
     * @param r   range to normalize
     *
     * @return  flow type range
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
     * Return whether the Data held by this display control contains multiple
     * fields (e.g., for the isosurface colored by another parameter
     * @return  true if there are multiple fields
     */
    protected boolean haveMultipleFields() {
        if (getGridDataInstance() == null) {
            return false;
        }
        return (getGridDataInstance().getNumRealTypes()
                > ((getIsThreeComponents())
                   ? 3
                   : 2)) && !getMultipleIsTopography();
    }

    /**
     * Returns the index to use in the GridDataInstance array of ranges
     * for color ranges.
     *
     * @return  The index to be used for the color range.
     */
    protected int getColorRangeIndex() {
        if (getMultipleIsTopography()) {
            return 0;
        }
        return getIsThreeComponents()
               ? 3
               : 2;
    }

    /**
     * Set the range for the flow components
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setFlowRange() throws RemoteException, VisADException {
        if ((getGridDisplay() != null) && !getWindbarbs()) {
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
     * Show the color control widget in the widgets if FLAG_COLOR is set.
     * @return  false  subclasses should override
     */
    public boolean showColorControlWidget() {
        return !haveMultipleFields();
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

}
