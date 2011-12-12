/*
 * $Id: VertScaleDialog.java,v 1.24 2007/05/04 14:17:36 dmurray Exp $
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



package ucar.unidata.view.geoloc;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.visad.Util;
import visad.CommonUnit;
import visad.Unit;

/**
 * A widget to get vertical range or scaling info from the user
 *
 * @author   IDV Development Team
 * @version  $Revision: 1.24 $
 */
public class VertScaleDialog extends JPanel implements ActionListener {

    /** Lat lon scale info */
    private LatLonScaleInfo latLonInfo = new LatLonScaleInfo();

    /** Abscissa (x-axis) Label */
    private JTextField abscissaLabel;

    /** Ordinate (y-axis) Label */
    private JTextField ordinateLabel;

    /** The control */
    ViewpointControl control;

    /** The dialog whe in dialog mode */
    JDialog dialog;

    /** Major tick spinner */
    JSpinner majorTickSpinner;

    /** input fields for max/min values */
    private JTextField min, max;

    /** Minor tick spinner */
    JSpinner minorTickSpinner;

    /** flag for whether the user hit cancel or not */
    private boolean ok;

    /** The frame parent */
    JFrame parent;

    /** Holds the info */
    VertScaleInfo transfer;

    /** combo box for selecting units */
    private JComboBox unitCombo;

    /**
     * Create a new dialog for setting the vertical range of the display
     *
     * @param parent   parent for model dialog
     * @param control The control
     */
    public VertScaleDialog(JFrame parent, ViewpointControl control) {
        this(parent, control, null);
    }

    /**
     * Create a new dialog for setting the vertical range of the display
     *
     * @param parent   parent for model dialog
     * @param control The control
     * @param transfer The info to use
     */
    public VertScaleDialog(JFrame parent, ViewpointControl control, VertScaleInfo transfer) {
        this.control  = control;
        this.parent   = parent;
        this.transfer = transfer;
        doMakeContents();
    }

    /**
     * Make the widget contents (UI)
     */
    protected void doMakeContents() {
        setLayout(new BorderLayout());
        GuiUtils.tmpInsets = new Insets(5, 5, 0, 0);

        JPanel p1 = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Min value: "), min = new JTextField(""), GuiUtils.rLabel("Max value: "),
            max = new JTextField(""), GuiUtils.rLabel("Units: "),
            unitCombo = GuiUtils.getEditableBox(Misc.toList(new String[] { "meters", "km", "feet", "fathoms" }), null)
//            GuiUtils.rLabel("Abscissa (x-axis) Label: "), abscissaLabel = new JTextField(this.latLonInfo.abscissaLabel),
//            GuiUtils.rLabel("Ordinate (y-axis) Label: "), ordinateLabel = new JTextField(this.latLonInfo.ordinateLabel),
//            GuiUtils.rLabel("Major tick: "), majorTickSpinner = new JSpinner(new SpinnerNumberModel(4, 0, 10, 2)),
//            GuiUtils.rLabel("Minor tick: "), minorTickSpinner = new JSpinner(new SpinnerNumberModel(8, 0, 10, 2))
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);

        min.setActionCommand(GuiUtils.CMD_OK);
        min.addActionListener(this);
        max.setActionCommand(GuiUtils.CMD_OK);
        max.addActionListener(this);
        this.add("Center", GuiUtils.inset(p1, 5));

        if (transfer != null) {
            min.setText(Misc.format(transfer.minVertScale));
            max.setText(Misc.format(transfer.maxVertScale));

            if (transfer.unit != null) {
                unitCombo.setSelectedItem(transfer.unit.toString());
            }
        }
    }

    /**
     * Handle user click on OK or other(cancel) button.  Closes the
     * dialog.
     *
     * @param evt  event to handle
     */
    public void actionPerformed(ActionEvent evt) {
        String cmd = evt.getActionCommand();

        if (cmd.equals(GuiUtils.CMD_OK)) {
            if (!doApply()) {
                return;
            }

            setVisible(false);
        } else if (cmd.equals(GuiUtils.CMD_APPLY)) {
            doApply();
        } else if (cmd.equals(GuiUtils.CMD_CANCEL)) {
            setVisible(false);
        }
    }

    /**
     * Set visibility of dialog if in dialog mode
     *
     * @param v Is visible
     */
    public void setVisible(boolean v) {
        if (dialog != null) {
            dialog.setVisible(v);
        }
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
            dialog = new JDialog(parent, "Vertical Scale", false /* non-modal */);
            dialog.getContentPane().add("Center", this);
            dialog.setSize(240, 100);    // size of dialog box is X byY pixels
            dialog.pack();
            dialog.setLocation(100, 100);
        }

        this.transfer = transfer;
        min.setText(Misc.format(transfer.minVertScale));
        max.setText(Misc.format(transfer.maxVertScale));
        unitCombo.setSelectedItem(transfer.unit.toString());
        dialog.setVisible(true);
    }

    /**
     * Apply the dialog state
     *
     * @return Was it successful
     */
    public boolean doApply() {
        float minValue = Float.NaN;
        float maxValue = Float.NaN;

        try {
            minValue = (float) Misc.parseNumber(min.getText());
            maxValue = (float) Misc.parseNumber(max.getText());
        } catch (NumberFormatException nfe) {
            LogUtil.userMessage("Invalid max or min value");

            return false;
        }

        Unit newUnit = null;

        try {
            newUnit = Util.parseUnit((String) unitCombo.getSelectedItem());

            if (!Unit.canConvert(newUnit, CommonUnit.meter)) {
                throw new Exception();
            }
        } catch (Exception e) {
            LogUtil.userMessage("Unknown or incompatible unit " + unitCombo.getSelectedItem());

            return false;
        }

        VertScaleInfo newTransfer = new VertScaleInfo(minValue, maxValue, newUnit);

        if (!Misc.equals(newTransfer, transfer)) {
            transfer = newTransfer;

            try {
                control.applyVerticalScale(transfer);
            } catch (Exception exc) {
                LogUtil.userMessage("An error has occurred:" + exc);

                return false;
            }
        }

//        LatLonScaleInfo newLatLonInfo = new LatLonScaleInfo(abscissaLabel.getText(), ordinateLabel.getText(),
//                                            Integer.parseInt(majorTickSpinner.getModel().getValue() + ""),
//                                            Integer.parseInt(minorTickSpinner.getModel().getValue() + ""));
//
//        if (!newLatLonInfo.equals(latLonInfo)) {
//            latLonInfo = newLatLonInfo;
//
//            try {
//                control.applyLatLonScale(latLonInfo);
//            } catch (Exception e) {
//                LogUtil.userMessage("An error has occurred:" + e);
//
//                return false;
//            }
//        }

        return true;
    }
}
