/*
 * Copyright 1997-2026 Unidata Program Center/University Corporation for
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


import ucar.unidata.ui.FontSelector;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


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


/**
 * A widget to get lat/lon range info from the user
 *
 * @author   IDV Development Team
 */
@SuppressWarnings("serial")
public class LatLonScalePanel extends JPanel implements ActionListener {

    /** The coord format. */
    private JComboBox coordFormat;

    /** Latitude base label */
    private JTextField latBaseLabel;

    /** Latitude increment */
    private JTextField latIncrement;

    /** Lat (y-axis) Label */
    private JTextField latLabel;

    /** Latitude minor increment */
    private JSpinner latMinorSpinner;

    /** Lat scale info */
    private LatLonAxisScaleInfo latScaleInfo;

    /** Longitude base label */
    private JTextField lonBaseLabel;

    /** Longitude increment */
    private JTextField lonIncrement;

    /** Lon (x-axis) Label */
    private JTextField lonLabel;

    /** Longitude minor increment */
    private JSpinner lonMinorSpinner;

    /** Lon scale info */
    private LatLonAxisScaleInfo lonScaleInfo;

    /** Map projection display */
    private MapProjectionDisplay mpDisplay;

    /** The frame parent */
    JFrame parent;

    /** x axis visible */
    private JCheckBox xVisible;

    /** y axis visible */
    private JCheckBox yVisible;

    /** Use 360 */
    private JCheckBox use360;

    /**
     * Axis font selector
     */
    private FontSelector fontSelector;

    /** Is the label angled away from the axis or not */
    private JCheckBox labelRelief;

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
            latBaseLabel = new JTextField(),
            GuiUtils.rLabel("Major Increment: "),
            latIncrement = new JTextField(),
            GuiUtils.rLabel("Minor Division: "),
            latMinorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10,
                1)),
            GuiUtils.rLabel("Visible: "), yVisible = new JCheckBox("", true)
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        JPanel lonPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Label: "), lonLabel = new JTextField(),
            GuiUtils.rLabel("Base (-180 180): "),
            lonBaseLabel = new JTextField(),
            GuiUtils.rLabel("Major Increment: "),
            lonIncrement = new JTextField(),
            GuiUtils.rLabel("Minor Division: "),
            lonMinorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10,
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
                                     LatLonAxisScaleInfo.COORD_FORMATS),
                                 GuiUtils.rLabel(" 0-360 Longitude"),
                                 use360 = new JCheckBox("", false) }, 4,
                                     GuiUtils.WT_NY, GuiUtils.WT_N);

        JPanel reliefPanel = GuiUtils.doLayout(new Component[] {
                                 GuiUtils.rLabel("Axis label relief:"),
                                 labelRelief = new JCheckBox("", true) }, 4,
                                     GuiUtils.WT_NY, GuiUtils.WT_N);

        coordFormat.setEditable(true);
        coordFormat.setEnabled(true);

        JPanel latLonPanel = GuiUtils.doLayout(new Component[] { latPanel,
                lonPanel }, 1, GuiUtils.WT_NY, GuiUtils.WT_N);


        JPanel pnl = GuiUtils.doLayout(new Component[] { latLonPanel,
                GuiUtils.filler() }, 2, GuiUtils.WT_YY, GuiUtils.WT_N);

        List<?> pnls = Arrays.asList(new Component[] { GuiUtils.left(pnl),
                GuiUtils.left(reliefPanel), GuiUtils.left(formatPanel),
                GuiUtils.left(fontPanel) });

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
        latLabel.setText(latScaleInfo.getLabel());
        latBaseLabel.setText(Misc.format(latScaleInfo.getBaseLabel()));
        latIncrement.setText(Misc.format(latScaleInfo.getIncrement()));
        latMinorSpinner.setValue(latScaleInfo.getMinorDivision());
        yVisible.setSelected(latScaleInfo.isVisible());
        coordFormat.setSelectedItem(latScaleInfo.getCoordFormat());
        use360.setSelected(latScaleInfo.isUse360());
        labelRelief.setSelected(latScaleInfo.isLabelRelief());
        fontSelector.setFont(latScaleInfo.getFont());
    }

    /**
     * Populate lon scale info.
     */
    private void populateLonScaleInfo() {
        lonLabel.setText(lonScaleInfo.getLabel());
        lonBaseLabel.setText(Misc.format(lonScaleInfo.getBaseLabel()));
        lonIncrement.setText(Misc.format(lonScaleInfo.getIncrement()));
        lonMinorSpinner.setValue(lonScaleInfo.getMinorDivision());
        xVisible.setSelected(lonScaleInfo.isVisible());
        coordFormat.setSelectedItem(lonScaleInfo.getCoordFormat());
        use360.setSelected(lonScaleInfo.isUse360());
        labelRelief.setSelected(lonScaleInfo.isLabelRelief());
        fontSelector.setFont(lonScaleInfo.getFont());
    }

    /**
     * Apply the dialog state
     *
     * @return Was it successful
     */
    public boolean doApply() {
        LatLonAxisScaleInfo newLatInfo = new LatLonAxisScaleInfo();

        newLatInfo.setLabel(latLabel.getText());
        newLatInfo.setBaseLabel(Misc.parseNumber(latBaseLabel.getText()));
        newLatInfo.setIncrement(Misc.parseNumber(latIncrement.getText()));
        newLatInfo.setMinorDivision(
            Integer.valueOf(latMinorSpinner.getValue().toString()));
        newLatInfo.setVisible(yVisible.isSelected());
        newLatInfo.setCoordFormat(coordFormat.getSelectedItem() + "");
        newLatInfo.setFont(fontSelector.getFont());
        newLatInfo.setUse360(use360.isSelected());
        newLatInfo.setLabelRelief(labelRelief.isSelected());

        if ( !newLatInfo.equals(latScaleInfo)) {
            latScaleInfo = newLatInfo;
        }

        LatLonAxisScaleInfo newLonInfo = new LatLonAxisScaleInfo();

        newLonInfo.setLabel(lonLabel.getText());
        newLonInfo.setBaseLabel(Misc.parseNumber(lonBaseLabel.getText()));
        newLonInfo.setIncrement(Misc.parseNumber(lonIncrement.getText()));
        newLonInfo.setMinorDivision(
            Integer.valueOf(lonMinorSpinner.getValue().toString()));
        newLonInfo.setVisible(xVisible.isSelected());
        newLonInfo.setCoordFormat(coordFormat.getSelectedItem() + "");
        newLonInfo.setFont(fontSelector.getFont());
        newLonInfo.setUse360(use360.isSelected());
        newLonInfo.setLabelRelief(labelRelief.isSelected());

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
    public void setLatScaleInfo(LatLonAxisScaleInfo latScaleInfo) {
        this.latScaleInfo = latScaleInfo;
    }

    /**
     * Sets the lon scale info.
     *
     * @param lonScaleInfo the new lon scale info
     */
    public void setLonScaleInfo(LatLonAxisScaleInfo lonScaleInfo) {
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
