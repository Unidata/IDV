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


import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.visad.display.DisplayableData;


import ucar.visad.display.FlowDisplayable;
import ucar.visad.display.WindBarbDisplayable;

import visad.*;

import visad.georef.*;

import java.awt.Component;
import java.awt.Container;

import java.awt.event.*;

import java.rmi.RemoteException;


import java.util.List;

import javax.swing.*;
import javax.swing.event.*;



/**
 * A plan view control for flow data (vector or wind barbs)
 *
 * @author Stu Wier\
 * @version $Revision: 1.6 $
 */
public class FlowTimeHeightControl extends TimeHeightControl implements FlowDisplayControl {


    /** skip value */
    private int skipValue = 0;

    /** a component to change the barb size */
    JComponent barbSizeBox;

    /** flag for streamlines */
    boolean isStreamlines = false;

    /** flag for wind barbs */
    boolean isWindBarbs = false;

    /** flag for wind barbs */
    boolean isThreeComponents = false;

    /** a scale factor */
    protected final float scaleFactor = 0.02f;

    /** a scale value */
    float flowScaleValue = 2.0f;

    /**
     * Create a new FlowTimeHeightControl; set attribute flags
     */
    public FlowTimeHeightControl() {
        setAttributeFlags(FLAG_COLOR);
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
        return (FlowDisplayable) getDataDisplay();
    }

    /**
     * Create the {@link ucar.visad.display.Displayable} for the data depiction.
     * @return the Displayable.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected DisplayableData createDataDisplay()
            throws VisADException, RemoteException {
        FlowDisplayable dataDisplay;
        if (isWindBarbs) {
            dataDisplay = new WindBarbDisplayable("wb_color_" + paramName,
                    null);
        } else {
            dataDisplay = new FlowDisplayable("wv_color_" + paramName, null);
            dataDisplay.setStreamlinesEnabled(isStreamlines);
        }
        dataDisplay.setAdjustFlow(false);
        addAttributedDisplayable(dataDisplay);
        return dataDisplay;
    }

    /**
     * Method to call after init().
     * @see #init
     */
    public void initDone() {
        super.initDone();
        setFlowScale(flowScaleValue);
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
        barbSizeBox = GuiUtils.createValueBox(this, CMD_BARBSIZE,
                (int) flowScaleValue, Misc.createIntervalList(0, 10, 1),
                true);
        /*
        JComponent intervalBox = GuiUtils.createValueBox(this, CMD_INTERVAL,
                                     skipValue,
                                     Misc.createIntervalList(0, 10, 1), true);
        JPanel extra = GuiUtils.hbox(barbSizeBox,
                                     new JLabel("  Skip interval: "),
                                     intervalBox);
        */
        JPanel extra = GuiUtils.hbox(barbSizeBox, GuiUtils.filler());

        controlWidgets.add(
            new WrapperWidget(this, GuiUtils.rLabel("Vector size: "), extra));
        enableBarbSizeBox();
        super.getControlWidgets(controlWidgets);
    }


    /**
     * Get the attribute flags for the data display
     * @return the flags
     */
    protected int getDataDisplayFlags() {
        // TODO Auto-generated method stub
        return FLAG_DISPLAYUNIT;
    }

    /**
     * enable the barb size box
     */
    private void enableBarbSizeBox() {
        barbSizeBox.setEnabled( !isStreamlines);
    }

    /**
     * Handle actions as an ActionListener
     *
     * @param e  action event to process
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            if (cmd.equals(CMD_BARBSIZE)) {
                setFlowScale(GuiUtils.getBoxValue((JComboBox) e.getSource()));

                /*
                } else if (cmd.equals(CMD_INTERVAL)) {
                    setSkipValue(
                        (int) GuiUtils.getBoxValue((JComboBox) e.getSource()));
                */
            } else {
                super.actionPerformed(e);
            }
        } catch (NumberFormatException nfe) {
            userErrorMessage("Incorrect number format");
        }

    }

    /**
     * Set the streamline property.
     * Used by XML persistence
     *
     * @param v  true to use streamlines
     */
    public void setStreamlines(boolean v) {
        isStreamlines = v;
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
            getGridDisplay().setFlowScale(flowScaleValue * scaleFactor);
        }
    }

    /**
     * Given the location of the profile SelectorPoint,
     * create a data set for a profile at that location,
     * and load it in display. Show lat-lon location on the control frame.
     *
     * @param position the location
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void loadProfile(RealTuple position)
            throws VisADException, RemoteException {

        if ( !getHaveInitialized()) {
            return;
        }
        if (position == null) {
            return;
        }
        LatLonPoint llp = getPositionLL(position);
        int orient      = (llp.getLatitude().getValue(CommonUnit.degree) > 0)
                          ? FlowControl.NH_ORIENTATION
                          : FlowControl.SH_ORIENTATION;
        ((FlowDisplayable) getDataDisplay()).setBarbOrientation(orient);
        super.loadProfile((LatLonTuple) llp);
    }

    /**
     * Make a 2D display of the range values against domain coordinate # NN.
     *
     * @param fi a VisAD FlatField or seqence of FlatFields with 3 or more
     *           domain coordinates, manifold dimension 1.
     * @param NN an integer, the index number of the coordinate to use
     *               as profile or y axis of plot (0,1,2,...)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void displayTHForCoord(FieldImpl fi, int NN)
            throws VisADException, RemoteException {
        ((FlowDisplayable) getDataDisplay()).loadData(fi);
    }

}
