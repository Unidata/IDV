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

import ucar.unidata.data.DataChoice;

import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.WindBarbDisplayable;


import visad.*;

import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A cross section control for depicting flow (vectors or wind barbs)
 *
 * @author Unidata Development Team
 * @version $Revision: 1.36 $
 */
public class FlowCrossSectionControl extends CrossSectionControl implements FlowDisplayControl {


    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** scaling factor */
    protected final float scaleFactor = 0.02f;

    /** scale value */
    float flowScaleValue = 4.0f;

    /** flag for 3D flow */
    boolean isThreeComponents = false;

    /** a label listing the range of the data */
    JLabel flowRangeLabel;

    /** Range for flow scale */
    private Range flowRange;

    /** Widget to set barb size */
    ValueSliderWidget barbSizeWidget;

    /**
     * Create a new FlowCrossSectionControl; set attribute flags
     */
    public FlowCrossSectionControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH | FLAG_SMOOTHING);
    }

    /**
     * Actions to perform after init().
     * @see #init
     */
    public void initDone() {
        super.initDone();
        setFlowScale(flowScaleValue);
        ((FlowDisplayable) getXSDisplay()).setAdjustFlow(
            getIsThreeComponents());
    }

    /**
     * Load the external display and the local display
     * with this data of a vertical cross section.
     *
     * @param fieldImpl   the data for the depiction
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void loadData(FieldImpl fieldImpl)
            throws VisADException, RemoteException {
        if ((getFlowRange() == null) && !getWindbarbs()) {
            setFlowRange(fieldImpl);
        }
        // hack for setting the barbOrientation
        if ((startLocation != null) && getWindbarbs()) {
            if (startLocation.getLatitude().getValue() >= 0) {
                ((FlowDisplayable) getVerticalCSDisplay()).setBarbOrientation(
                    FlowDisplayable.NH_ORIENTATION);
            } else {
                ((FlowDisplayable) getVerticalCSDisplay()).setBarbOrientation(
                    FlowDisplayable.SH_ORIENTATION);
            }
        }
        super.loadData(fieldImpl);
    }


    /**
     * Add this displaycontrol's display settings to the dialog
     *
     * @param dsd the dialog
     */
    protected void addDisplaySettings(DisplaySettingsDialog dsd) {
        super.addDisplaySettings(dsd);
        dsd.addPropertyValue(new Double(flowScaleValue), "flowScale",
                             "Scale", SETTINGS_GROUP_DISPLAY);
        dsd.addPropertyValue(flowRange, "flowRange", "Flow Field Range",
                             SETTINGS_GROUP_DISPLAY);
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
        Trace.call1("FlowCrossSection.setData");
        boolean result = super.setData(dataChoice);
        if ( !result) {
            Trace.call2("FlowCrossSection.setData");
            return false;
        }
        //if ( !getWindbarbs()) {
        if ((getFlowRange() == null) && !getWindbarbs()) {
            setFlowRange((FieldImpl) null);
        }
        setFlowScale(flowScaleValue);
        //        setSkipValue(skipValue);
        Trace.call2("FlowCrossSection.setData");
        return true;
    }

    /**
     * Get the label for the color widget
     *
     * @return  label text
     */
    public String getColorWidgetLabel() {
        return "Color";
    }

    /**
     * Get the grid display for the depiction.
     *
     * @return  the displayable for flow depictions.
     */
    FlowDisplayable getGridDisplay() {
        return (FlowDisplayable) getXSDisplay();
    }

    /**
     * Create the depiction for the data in the main window
     * @return  the Displayable
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected DisplayableData createXSDisplay()
            throws VisADException, RemoteException {
        DisplayableData displayable;
        if (isWindBarbs) {
            displayable = new WindBarbDisplayable("wb_cs_color_" + paramName,
                    null);
        } else {
            displayable = new FlowDisplayable("wv_cs_color_" + paramName,
                    null);
        }
        addAttributedDisplayable(displayable);
        return displayable;
    }

    /**
     * Create the depiction for the data in the control window
     * @return  the Displayable
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected DisplayableData createVCSDisplay()
            throws VisADException, RemoteException {
        DisplayableData displayable;
        if (isWindBarbs) {
            displayable = new WindBarbDisplayable("wb_xs_color_" + paramName,
                    null);
        } else {
            displayable = new FlowDisplayable("wv_xs_color_" + paramName,
                    null);
        }
        addAttributedDisplayable(displayable);
        return displayable;
    }

    /**
     * Get any extra control widgets
     *
     * @param controlWidgets  control widgets special to this control
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        barbSizeWidget = new ValueSliderWidget(this, 1, 21, "flowScale",
                "Scale: ");
        addRemovable(barbSizeWidget);
        JPanel extra = GuiUtils.hbox(GuiUtils.rLabel("Scale:  "),
                                     barbSizeWidget.getContents(false));
        if ( !getWindbarbs()) {
            extra = GuiUtils.hbox(extra,
                                  GuiUtils.hbox(GuiUtils.filler(),
                                      doMakeFlowRangeComponent()));
        }
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel(getSizeLabel()),
                                             GuiUtils.left(extra)));
        super.getControlWidgets(controlWidgets);

    }

    /**
     * Get the appropriate size label for this instance.
     * @return the label
     */
    private String getSizeLabel() {
        return (getWindbarbs())
               ? "Barb Size: "
               : "Vector Size: ";
    }

    /** _more_ */
    RangeDialog rangeDialog;


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
                    rangeDialog = new RangeDialog(
                        FlowCrossSectionControl.this, flowRange,
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
     * Get the flow scale factor.  used by XML persistence
     * @return the flow scale
     */
    public float getFlowScale() {
        return flowScaleValue;
    }

    /**
     * Get the flow scale factor.  used by XML persistence
     *
     * @param f  new flow scale
     */
    public void setFlowScale(float f) {
        flowScaleValue = f;
        if (getHaveInitialized()) {
            if (getGridDisplay() != null) {
                getGridDisplay().setFlowScale(flowScaleValue * scaleFactor);
                ((FlowDisplayable) getVerticalCSDisplay()).setFlowScale(
                    flowScaleValue * scaleFactor);
            }
            doShare(SHARE_FLOWRANGE, flowRange);
        }
        if (barbSizeWidget != null) {
            barbSizeWidget.setValue(f);
        }
    }


    /**
     * Set whether this is depicting wind barbs or not.  Use by XML persistence.
     *
     * @param v   true for wind barbs
     */
    public void setWindbarbs(boolean v) {
        isWindBarbs = v;
    }

    /**
     * Get whether this is depicting wind barbs or not.  Use by XML persistence.
     * @return  true if wind barbs
     */
    public boolean getWindbarbs() {
        return isWindBarbs;
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
        return getGridDataInstance().getNumRealTypes()
               > (getIsThreeComponents()
                  ? 3
                  : 2);
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
     * Returns the index to use in the GridDataInstance array of ranges
     * for color ranges.
     *
     * @return  The index to be used for the color range.
     */
    protected int getColorRangeIndex() {
        return getIsThreeComponents()
               ? 3
               : 2;
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
        if (getHaveInitialized()) {
            if ((getGridDisplay() != null) && (flowRange != null)
                    && !getWindbarbs()) {
                try {
                    getGridDisplay().setFlowRange(flowRange);
                    ((FlowDisplayable) getVerticalCSDisplay()).setFlowRange(
                        flowRange);
                } catch (Exception excp) {
                    logException("setFlowRange: ", excp);
                }
            }
            setFlowRangeLabel();
            doShare(SHARE_FLOWRANGE, flowRange);
        }
    }

    /**
     * Set the range for the flow components
     *
     * @param data  data to use for range (may be null)
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setFlowRange(FieldImpl data)
            throws RemoteException, VisADException {
        if ((getGridDisplay() != null) && !getWindbarbs()) {
            if (getFlowRange() == null) {
                Range[] ranges = null;
                if (data == null) {
                    data = (FieldImpl) getGridDisplay().getData();
                }
                if (data != null) {
                    ranges = GridUtil.getMinMax((FieldImpl) data);
                    double max      = Double.NEGATIVE_INFINITY;
                    double min      = Double.POSITIVE_INFINITY;
                    int    numComps = getIsThreeComponents()
                                      ? 3
                                      : 2;
                    for (int i = 0; i < numComps; i++) {
                        Range compRange = ranges[i];
                        max = Math.max(compRange.getMax(), max);
                        min = Math.min(compRange.getMin(), min);
                    }
                    if ( !Double.isInfinite(max) && !Double.isInfinite(min)) {
                        max = Math.max(max, -min);
                        min = -max;
                    }
                    // System.out.println("setFlowRange: " + min + " to " + max);
                    //getGridDisplay().setFlowRange(min,max);
                    setFlowRange(new Range(min, max));
                } else {  // gotta set it to something
                    setFlowRange(new Range(-40, 40));
                }
            } else {
                getGridDisplay().setFlowRange(flowRange.getMin(),
                        flowRange.getMax());
            }
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
            flowRangeLabel = new JLabel("Range: ", SwingConstants.RIGHT);

        }
        Range r = getFlowRange();
        if (r != null) {
            flowRangeLabel.setText("Range: " + r.formatMin() + " to "
                                   + r.formatMax());
        } else {
            flowRangeLabel.setText("Range: Undefined");
        }
    }

    /**
     * Show the color control widget in the widgets if FLAG_COLOR is set.
     * @return  false  subclasses should override
     */
    public boolean showColorControlWidget() {
        return true;
    }


}
