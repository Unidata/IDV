/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import ucar.unidata.ui.FontSelector;
import ucar.unidata.ui.NumericTextField;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.view.geoloc.AxisScaleInfo.CoordSys;


/**
 * A widget to get lat/lon range info from the user
 *
 * @author   IDV Development Team
 */
public class LatLonScalePanel extends JPanel implements ActionListener {

    /** The coord format. */
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
     * Axis font selector
     */
    private FontSelector fontSelector;

    /**
     * Create a new dialog for setting the coordinate range of the display
     *
     * @param mpDisplay the mp display
     */
    public LatLonScalePanel(MapProjectionDisplay mpDisplay) {
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

        fontSelector       = new FontSelector(FontSelector.COMBOBOX_UI, false,
                                        false);

        JPanel latPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Label: "), latLabel = new JTextField(),
            GuiUtils.rLabel("Base (-90 90): "),
            latBaseLabel = new NumericTextField(),
            GuiUtils.rLabel("Major Increment: "),
            latIncrement = new NumericTextField(),
            GuiUtils.rLabel("Minor Division: "),
            latMinorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4,
                1)),
            GuiUtils.rLabel("Visible: "), yVisible = new JCheckBox("", true)
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        JPanel lonPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Label: "), lonLabel = new JTextField(),
            GuiUtils.rLabel("Base (-180 180): "),
            lonBaseLabel = new NumericTextField(),
            GuiUtils.rLabel("Major Increment: "),
            lonIncrement = new NumericTextField(),
            GuiUtils.rLabel("Minor Division: "),
            lonMinorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 4,
                1)),
            GuiUtils.rLabel("Visible: "), xVisible = new JCheckBox("", true),
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);

        latPanel.setBorder(BorderFactory.createTitledBorder("Latitude"));
        lonPanel.setBorder(BorderFactory.createTitledBorder("Longitude"));

        JComponent fontPanel =
            GuiUtils.doLayout(new Component[] {
                GuiUtils.left(fontSelector.getComponent()) }, 1,
                    GuiUtils.WT_N, GuiUtils.WT_N);
        fontPanel.setBorder(BorderFactory.createTitledBorder("Font"));

        JPanel formatPanel = GuiUtils.doLayout(new Component[] {
                                 GuiUtils.rLabel("Format: "),
                                 coordFormat = new JComboBox(
                                     AxisScaleInfo.CoordSys.values()) }, 2,
                                         GuiUtils.WT_NY, GuiUtils.WT_N);

        JPanel latLonPanel = GuiUtils.doLayout(new Component[] { latPanel,
                lonPanel }, 1, GuiUtils.WT_NY, GuiUtils.WT_N);


        JPanel pnl = GuiUtils.doLayout(new Component[] { latLonPanel,
                GuiUtils.filler() }, 2, GuiUtils.WT_YY, GuiUtils.WT_N);

        List<?> pnls = Arrays.asList(new Component[] { GuiUtils.left(pnl),
                GuiUtils.left(formatPanel), GuiUtils.left(fontPanel) });

        this.add("Center", GuiUtils.doLayout(pnls, 1, 5, 5));


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
            if ( !doApply()) {
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
        fontSelector.setFont(latScaleInfo.font);
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
        fontSelector.setFont(lonScaleInfo.font);
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
        newLatInfo.minorIncrement =
            Integer.valueOf(latMinorSpinner.getValue().toString());
        newLatInfo.visible     = yVisible.isSelected();
        newLatInfo.coordFormat = (CoordSys) coordFormat.getSelectedItem();
        newLatInfo.font        = fontSelector.getFont();

        if ( !newLatInfo.equals(latScaleInfo)) {
            latScaleInfo = newLatInfo;
        }

        AxisScaleInfo newLonInfo = new AxisScaleInfo();

        newLonInfo.label          = lonLabel.getText();
        newLonInfo.baseLabel      = lonBaseLabel.getText();
        newLonInfo.increment      = lonIncrement.getText();
        newLonInfo.minorIncrement =
            Integer.valueOf(lonMinorSpinner.getValue().toString());
        newLonInfo.visible     = xVisible.isSelected();
        newLonInfo.coordFormat = (CoordSys) coordFormat.getSelectedItem();
        newLonInfo.font        = fontSelector.getFont();

        if ( !newLonInfo.equals(lonScaleInfo)) {
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

    /**
     * Checks if is lon visible.
     *
     * @return true, if is lon visible
     */
    public boolean isLonVisible() {
        return xVisible.isSelected();
    }

    /**
     * Checks if is lat visible.
     *
     * @return true, if is lat visible
     */
    public boolean isLatVisible() {
        return yVisible.isSelected();
    }
}
