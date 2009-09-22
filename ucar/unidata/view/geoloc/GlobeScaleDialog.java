/*
 * Copyright  1997-2009 Unidata Program Center/University Corporation for
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


package ucar.unidata.view.geoloc;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.Util;

import visad.CommonUnit;
import visad.Unit;

import visad.VisADException;

import java.awt.*;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import javax.swing.event.*;



/**
 * A widget to get the vertical exaggeration
 *
 * @author   IDV Development Team
 */
public class GlobeScaleDialog extends VertScaleDialog {

    /** input fields for percent values */
    private JTextField valueFld;

    /** input fields for max/min values */
    private JSlider vexSlider;

    /** flag for ignoring UI events */
    private boolean ignoreUIEvents = false;

    /** Earth Radius (m) */
    private static final double EARTH_RADIUS = GlobeDisplay.EARTH_RADIUS;

    /**
     * Create a new dialog for setting the vertical range of the display
     *
     * @param parent   parent for model dialog
     * @param control The control
     */
    public GlobeScaleDialog(JFrame parent, ViewpointControl control) {
        this(parent, control, null);
    }


    /**
     * Create a new dialog for setting the vertical range of the display
     *
     * @param parent   parent for model dialog
     * @param control The control
     * @param transfer The info to use
     */
    public GlobeScaleDialog(JFrame parent, ViewpointControl control,
                            VertScaleInfo transfer) {
        super(parent, control, transfer);
    }

    /**
     * Make the contents for this widget
     */
    protected void doMakeContents() {

        setLayout(new BorderLayout());
        GuiUtils.tmpInsets = new Insets(5, 5, 0, 0);
        valueFld           = new JTextField("   ", 6);
        valueFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (ignoreUIEvents) {
                    return;
                }
                try {
                    double vex = Misc.parseNumber(valueFld.getText().trim());
                    vexSlider.setValue((int) vex);
                } catch (NumberFormatException nfe) {
                    LogUtil.userMessage("Incorrect format: "
                                        + valueFld.getText());
                } catch (Exception exc) {
                    //LogUtil.printException("Setting level", exc);
                }
            }
        });
        vexSlider = new JSlider(JSlider.HORIZONTAL, 1, 1000, 40);
        vexSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent ae) {
                if (ignoreUIEvents) {
                    return;
                }
                int vex = vexSlider.getValue();
                adjustSliderLabel(vex);
            }
        });


        JPanel p1 = GuiUtils.doLayout(new Component[] { vexSlider,
                        valueFld }, 2, GuiUtils.WT_YN, GuiUtils.WT_N);
        p1.setBorder(
            BorderFactory.createTitledBorder("Vertical Exaggeration"));

        this.add("Center", GuiUtils.inset(p1, 5));
        this.transfer = transfer;
        if (transfer != null) {
            double radius = Math.abs(transfer.minVertScale);
            if (transfer.unit != null) {
                try {
                    radius = CommonUnit.meter.toThis(radius, transfer.unit);
                } catch (VisADException ve) {
                    LogUtil.userMessage("Incompatible units for range "
                                        + transfer.unit);
                }
            }
            setWidgetValues(radiusToVex(radius));
        }
    }

    /**
     * Convert a radius to a vertical exaggeration
     * @param radius  the earth radius
     * @return the vertical exaggeration
     */
    private int radiusToVex(double radius) {
        if (radius == 0) {
            radius = EARTH_RADIUS;
        }
        if (radius < 0) {
            radius = Math.abs(radius);
        }
        return (int) (EARTH_RADIUS / radius);
    }

    /**
     * Convert a radius to a vertical exaggeration
     * @param radius  the earth radius
     *
     * @param vex the vertical exaggeration
     */
    private double vexToRadius(int vex) {
        if (vex <= 1) {
            vex = 1;
        }
        return EARTH_RADIUS / vex;
    }

    /**
     * Show the dialog box and wait for results and deal with them.
     *
     * @param transfer   default values for the dialog
     */
    public void showDialog(VertScaleInfo transfer) {
        if (dialog == null) {
            JPanel buttons = GuiUtils.makeApplyOkCancelButtons(this);
            this.add("South", buttons);
            dialog = new JDialog(parent, "Set Vertical Exaggeration",
                                 false /*non-modal*/);
            dialog.getContentPane().add("Center", this);
            dialog.setSize(240, 100);  // size of dialog box is X byY pixels
            dialog.pack();
            dialog.setLocation(100, 100);
        }
        this.transfer = transfer;
        setWidgetValues(radiusToVex(Math.abs(transfer.maxVertScale)));
        dialog.setVisible(true);
    }


    /**
     * Apply the dialog state
     *
     * @return Was it successful
     */
    public boolean doApply() {
        int exagg;
        try {
            exagg = (int) Misc.parseNumber(valueFld.getText());
        } catch (NumberFormatException nfe) {
            return false;
        }

        double maxValue = vexToRadius(exagg);
        double minValue = -maxValue;

        VertScaleInfo newTransfer = new VertScaleInfo(minValue, maxValue,
                                        CommonUnit.meter);
        if ( !Misc.equals(newTransfer, transfer)) {
            transfer = newTransfer;
            try {
                control.applyVerticalScale(transfer);
            } catch (Exception exc) {
                LogUtil.userMessage("An error has occurred:" + exc);
                return false;
            }
        }
        return true;
    }

    /**
     * Reset the label that shows the vertical exaggeration.
     *
     * @param vex   vertical exaggeration
     */
    private void adjustSliderLabel(int vex) {
        String  value      = Misc.format(vex);
        boolean lastIgnore = ignoreUIEvents;
        ignoreUIEvents = true;
        valueFld.setText(value);
        ignoreUIEvents = lastIgnore;
    }

    /**
     * Set the widget values.
     * @param vex   vertical exaggeration
     */
    private void setWidgetValues(int vex) {
        vexSlider.setValue(vex);
        adjustSliderLabel(vex);
    }
}

