/*
 *
 * Copyright  1997-2012 Unidata Program Center/University Corporation for
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

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.Util;

import visad.CommonUnit;
import visad.Unit;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A widget to get vertical range or scaling info from the user
 *
 * @author   IDV Development Team
 * @version  $Revision: 1.24 $
 */
public class VertScaleDialog extends JPanel implements ActionListener {

    /** The control */
    ViewpointControl control;

    /** The dialog when in dialog mode */
    JDialog dialog;

    /** input fields for max/min values */
    private JTextField min, max;

    /** flag for whether the user hit cancel or not */
    private boolean ok;

    /** The frame parent */
    JFrame parent;

    /** Holds the vertical scale info */
    VertScaleInfo vertScaleInfo;

    /** combo box for selecting units */
    private JComboBox unitCombo;

    /** Is the vertical scale visible */
    private JCheckBox visible;

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
     * @param vertScaleInfo The info to use
     */
    public VertScaleDialog(JFrame parent, ViewpointControl control, VertScaleInfo vertScaleInfo) {
        this.control  = control;
        this.parent   = parent;
        this.vertScaleInfo = vertScaleInfo;
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
            unitCombo = GuiUtils.getEditableBox(Misc.toList(new String[] { "meters", "km", "feet", "fathoms" }), null),
            GuiUtils.rLabel("Visible: "), visible = new JCheckBox("", true),
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);

        min.setActionCommand(GuiUtils.CMD_OK);
        min.addActionListener(this);
        max.setActionCommand(GuiUtils.CMD_OK);
        max.addActionListener(this);
        this.add("Center", GuiUtils.inset(p1, 5));

        if (vertScaleInfo != null) {
            min.setText(Misc.format(vertScaleInfo.minVertScale));
            max.setText(Misc.format(vertScaleInfo.maxVertScale));

            if (vertScaleInfo.unit != null) {
                unitCombo.setSelectedItem(vertScaleInfo.unit.toString());
            }

            visible.setSelected(vertScaleInfo.visible);
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

        this.vertScaleInfo = transfer;
        min.setText(Misc.format(transfer.minVertScale));
        max.setText(Misc.format(transfer.maxVertScale));
        unitCombo.setSelectedItem(transfer.unit.toString());
        visible.setSelected(transfer.visible);
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

        newTransfer.visible = visible.isSelected();

        if (!Misc.equals(newTransfer, vertScaleInfo)) {
            vertScaleInfo = newTransfer;

            try {
                control.applyVerticalScale(vertScaleInfo);
            } catch (Exception exc) {
                LogUtil.userMessage("An error has occurred:" + exc);

                return false;
            }
        }

        return true;
    }

    /**
     * Checks if is axis visible.
     *
     * @return true, if is axis visible
     */
    public boolean isAxisVisible() {
    	return visible.isSelected();
    }

	/**
	 * Gets the vert scale info.
	 *
	 * @return the vert scale info
	 */
	public VertScaleInfo getVertScaleInfo() {
		return this.vertScaleInfo;
	}
}
