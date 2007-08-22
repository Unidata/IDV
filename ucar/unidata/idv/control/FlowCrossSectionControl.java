/*
 * $Id: FlowCrossSectionControl.java,v 1.36 2006/12/01 20:16:33 jeffmc Exp $
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
public class FlowCrossSectionControl extends CrossSectionControl {


    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** scaling factor */
    protected final float scaleFactor = 0.02f;

    /** scale value */
    float flowScaleValue = 4.0f;

    /** flag for 3D flow */
    boolean isThreeComponents = false;

    /**
     * Create a new FlowCrossSectionControl; set attribute flags
     */
    public FlowCrossSectionControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_LINEWIDTH);
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
        if ( !getWindbarbs()) {
            setFlowRange(fieldImpl);
        }
        super.loadData(fieldImpl);
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
        super.getControlWidgets(controlWidgets);
        JComboBox barbSizeBox = GuiUtils.createValueBox(this, CMD_BARBSIZE,
                                    (int) flowScaleValue,
                                    Misc.createIntervalList(0, 10, 1), true);
        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Vector size:"),
                                             GuiUtils.left(barbSizeBox)));

    }


    /**
     * Public due to implementation of ActionListener
     *
     * @param e   the event to listen to
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            if (cmd.equals(CMD_BARBSIZE)) {
                setFlowScale(GuiUtils.getBoxValue((JComboBox) e.getSource()));
            } else {
                super.actionPerformed(e);
            }
        } catch (NumberFormatException nfe) {
            userErrorMessage("Incorrect number format");
        }

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
        if (getGridDisplay() != null) {
            getGridDisplay().setFlowScale(flowScaleValue * scaleFactor);
            ((FlowDisplayable) getVerticalCSDisplay()).setFlowScale(
                flowScaleValue * scaleFactor);
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
     * Set the range for the flow components
     *
     * @param field  the field to use for the flow range
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setFlowRange(FieldImpl field)
            throws RemoteException, VisADException {
        Range[] ranges = null;
        if (field != null) {
            Trace.call1("setFlowRange");
            ranges = GridUtil.getMinMax((FieldImpl) field);
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
            Trace.call2("setFlowRange", (min + " to " + max));
            // System.out.println("setFlowRange: " + min + " to " + max);
            getGridDisplay().setFlowRange(min, max);
            // TODO: for now, let VisAD auto scale this
            ((FlowDisplayable) getVerticalCSDisplay()).setFlowRange(min, max);
        }
    }

}

