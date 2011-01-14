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
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.profiler.*;
import ucar.unidata.idv.ControlContext;

import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;



import visad.*;

import java.awt.*;

import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


/**
 * An abstract class that provides common services
 * for Profiler display related controls.
 *
 * @author MetApps/Unidata
 * @version $Revision: 1.26 $
 */
public abstract class ProfilerControl extends DisplayControlImpl {

    /** scale factor for the wind barbs */
    protected final float scaleFactor = 0.02f;

    /** flow scale */
    protected float flowScaleValue = 2.0f;

    /** vertical spacing interval */
    protected float verticalIntervalValue = 250.0f;

    /**
     *  Default constructor; does nothing. See init() for creation actions.
     */
    public ProfilerControl() {}


    /**
     * Make any extra components for the UI.
     * @return  extra components wrapped in a Component
     */
    protected JComponent doMakeExtraComponent() {
        return null;
    }


    /**
     * Make a combo box to select vertical separation of wind barbs, in m
     * @return  component for vertical interval selection
     */
    protected JComponent doMakeVerticalIntervalComponent() {
        JComboBox intervalBox = GuiUtils.createValueBox(this, CMD_INTERVAL,
                                    (int) verticalIntervalValue,
                                    Misc.createIntervalList(250, 1000, 250),
                                    true);
        return GuiUtils.label("  Vertical interval (m): ",
                              GuiUtils.wrap(intervalBox));
    }


    /**
     * Make the control widgets
     * @param controlWidgets list of control widgets
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);

        JComponent barbSizeBox = GuiUtils.wrap(GuiUtils.createValueBox(this,
                                     CMD_BARBSIZE, (int) flowScaleValue,
                                     Misc.createIntervalList(1, 10, 1),
                                     true));

        // make possible another component(s) to put on same line w barbSizeBox
        JComponent extra      = doMakeExtraComponent();

        JComponent rightPanel = GuiUtils.leftCenter(((extra != null)
                ? (JComponent) GuiUtils.hflow(Misc.newList(barbSizeBox,
                    extra))
                : (JComponent) barbSizeBox), GuiUtils.filler());
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Windbarb size: "), rightPanel));

    }


    /**
     *  Override the base class method to catch any events.
     *
     *  @param e The action event.
     */
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            if (cmd.equals(CMD_BARBSIZE)) {
                setFlowScale(GuiUtils.getBoxValue((JComboBox) e.getSource()));
            } else if (cmd.equals(CMD_INTERVAL)) {
                setVerticalInterval(
                    GuiUtils.getBoxValue((JComboBox) e.getSource()));
            } else {
                super.actionPerformed(e);
            }
        } catch (NumberFormatException nfe) {
            userErrorMessage("Incorrect number format");
        }

    }


    /**
     * Method called after initialization.
     */
    public void initDone() {
        super.initDone();
        setFlowScale(flowScaleValue);
        setVerticalInterval(verticalIntervalValue);
    }


    /**
     * Use the value of the factor in the vert interval box
     * to set a different vertical interval.
     *
     * @param value   new vertical interval value (meters)
     */
    public void setVerticalInterval(float value) {
        verticalIntervalValue = value;
    }


    /**
     * Get the vertical interval value
     * @return  vertical interval in meters
     */
    public float getVerticalInterval() {
        return verticalIntervalValue;
    }



    /**
     * Set the length of the wind barb
     *
     * @param  f  the length of the wind barb
     */
    public void setFlowScale(float f) {
        flowScaleValue = f;
    }

    /**
     *  Return the value of the barb size.
     *
     *  @return The value of the barb size.
     */
    public float getFlowScale() {
        return flowScaleValue;
    }


}
