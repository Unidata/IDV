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
import ucar.unidata.util.NumericTextField;
import ucar.unidata.view.geoloc.AxisScaleInfo.CoordSys;

//~--- JDK imports ------------------------------------------------------------

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * A widget to get lat/lon range info from the user
 *
 * @author   IDV Development Team
 */
public class LatLonScaleDialog extends JPanel implements ActionListener {
    private JComboBox coordFormat;

    /** Latitude base label */
    private NumericTextField latBaseLabel;

    /** Latitude increment */
    private NumericTextField latIncrement;

    /** Lat (y-axis) Label */
    private JTextField latLabel;

    /** Latitude minor increment */
    private JSpinner latMinorSpinner;

    /** Lat scale info */
    private AxisScaleInfo latScaleInfo;

    /** Longitude base label */
    private NumericTextField lonBaseLabel;

    /** Longitude increment */
    private NumericTextField lonIncrement;

    /** Lon (x-axis) Label */
    private JTextField lonLabel;

    /** Longitude minor increment */
    private JSpinner lonMinorSpinner;

    /** Lon scale info */
    private AxisScaleInfo lonScaleInfo;

    /** Map projection display */
    private MapProjectionDisplay mpDisplay;

    /** flag for whether the user hit cancel or not */
    private boolean ok;

    /** The frame parent */
    JFrame parent;

    /** x axis visible */
    private JCheckBox xVisible;

    /** y axis visible */
    private JCheckBox yVisible;

    /**
     * Create a new dialog for setting the coordinate range of the display
     *
     */
    public LatLonScaleDialog(MapProjectionDisplay mpDisplay) {
        this.mpDisplay    = mpDisplay;
        this.parent       = GuiUtils.getFrame(mpDisplay.getComponent());
        this.latScaleInfo = mpDisplay.getLatScaleInfo();
        this.lonScaleInfo = mpDisplay.getLonScaleInfo();
        doMakeContents();
    }

    /**
     * Make the widget contents (UI)
     */
    protected void doMakeContents() {
        setLayout(new BorderLayout());
        GuiUtils.tmpInsets = new Insets(5, 5, 0, 0);

        JPanel p1 = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Longitude Label: "), lonLabel = new JTextField(),
            GuiUtils.rLabel("Latitude Label: "), latLabel = new JTextField(),
            GuiUtils.rLabel("Latitude Base (-90 90): "), latBaseLabel = new NumericTextField(), GuiUtils.rLabel("Longitude Base (-180 180): "),
            lonBaseLabel = new NumericTextField(), GuiUtils.rLabel("Latitude Increment: "),
            latIncrement = new NumericTextField(), GuiUtils.rLabel("Latitude Minor Increment: "),
            latMinorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1)),
            GuiUtils.rLabel("Longitude Increment: "), lonIncrement = new NumericTextField(),
            GuiUtils.rLabel("Longitude Minor Increment: "),
            lonMinorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4, 1)),
            GuiUtils.rLabel("Longitude Visible: "), xVisible = new JCheckBox("", true),
            GuiUtils.rLabel("Latitude Visible: "), yVisible = new JCheckBox("", true),
            GuiUtils.rLabel("Coordinate: "), coordFormat = new JComboBox(AxisScaleInfo.CoordSys.values())
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);

        this.add("Center", GuiUtils.inset(p1, 5));

        if (latScaleInfo != null) {
            populateLatScaleInfo();
        }

        if (lonScaleInfo != null) {
            populateLonScaleInfo();
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
     * Populate lat scale info.
     */
    private void populateLatScaleInfo() {
        latLabel.setText(latScaleInfo.label);
        latBaseLabel.setText(latScaleInfo.baseLabel);
        latIncrement.setText(latScaleInfo.increment);
        latMinorSpinner.setValue(latScaleInfo.minorIncrement);
        yVisible.setSelected(latScaleInfo.visible);
        coordFormat.setSelectedItem(latScaleInfo.coordFormat);
    }

    /**
     * Populate lon scale info.
     */
    private void populateLonScaleInfo() {
        lonLabel.setText(lonScaleInfo.label);
        lonBaseLabel.setText(lonScaleInfo.baseLabel);
        lonIncrement.setText(lonScaleInfo.increment);
        lonMinorSpinner.setValue(lonScaleInfo.minorIncrement);
        xVisible.setSelected(lonScaleInfo.visible);
        coordFormat.setSelectedItem(lonScaleInfo.coordFormat);
    }

    /**
     * Apply the dialog state
     *
     * @return Was it successful
     */
    public boolean doApply() {
    	
        AxisScaleInfo newLatInfo = new AxisScaleInfo();
        newLatInfo.label          = latLabel.getText();
        newLatInfo.baseLabel      = latBaseLabel.getText();
        newLatInfo.increment      = latIncrement.getText();
        newLatInfo.minorIncrement = Integer.valueOf(latMinorSpinner.getValue().toString());
        newLatInfo.visible        = yVisible.isSelected();
        newLatInfo.coordFormat    = (CoordSys) coordFormat.getSelectedItem();

        if (!newLatInfo.equals(latScaleInfo)) {
            latScaleInfo = newLatInfo;
        }

        AxisScaleInfo newLonInfo = new AxisScaleInfo();

        newLonInfo.label          = lonLabel.getText();
        newLonInfo.baseLabel      = lonBaseLabel.getText();
        newLonInfo.increment      = lonIncrement.getText();
        newLonInfo.minorIncrement = Integer.valueOf(lonMinorSpinner.getValue().toString());
        newLonInfo.visible        = xVisible.isSelected();
        newLonInfo.coordFormat    = (CoordSys) coordFormat.getSelectedItem();

        if (!newLonInfo.equals(lonScaleInfo)) {
            lonScaleInfo = newLonInfo;
        }

        mpDisplay.setDisplayInactive();

        try {
            mpDisplay.setLatScaleInfo(latScaleInfo);
            mpDisplay.setLonScaleInfo(lonScaleInfo);
            mpDisplay.setDisplayActive();
        } catch (Exception e) {
            LogUtil.userMessage("An error has occurred:" + e);
            return false;
        }

        return true;
    }

    /**
     * Sets the lat scale info.
     *
     * @param latScaleInfo the new lat scale info
     */
    public void setLatScaleInfo(AxisScaleInfo latScaleInfo) {
        this.latScaleInfo = latScaleInfo;
    }

    /**
     * Sets the lon scale info.
     *
     * @param lonScaleInfo the new lon scale info
     */
    public void setLonScaleInfo(AxisScaleInfo lonScaleInfo) {
        this.lonScaleInfo = lonScaleInfo;
    }
}
