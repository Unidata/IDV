/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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
import ucar.visad.Util;
import visad.CommonUnit;
import visad.Unit;

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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * A widget to get vertical range or scaling info from the user.  NB: this should be
 * called VertRangeDialog, but history trumps practicality
 *
 * @author   IDV Development Team
 */

public class VertScaleDialog extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	/** The control */
    ViewpointControl control;

    /** The dialog when in dialog mode */
    JDialog dialog;

    /** input fields for max/min values */
    private JTextField min, max;
    
    /** input field for Label values */
    private JTextField jtfAxisLabel;

    /** The frame parent */
    JFrame parent;

    /** Holds the vertical scale info */
    VertScaleInfo vertScaleInfo;

    /** combo box for selecting units */
    private JComboBox unitCombo;

    /** Is the vertical scale visible */
    private JCheckBox jcbVisible;
    
    /** Vertical increment */
    private JTextField vertIncrement;
    
    /** Vertical scale minor increment */
    private JSpinner vertMinorSpinner;
    
    /**
     * Axis font selector
     */
    private FontSelector fontSelector;

    /**
     * Create a new dialog for setting the vertical range of the display
     *
     * @param parent   parent for model dialog
     * @param control The control
     * @param vertScaleInfo The info to use
     */
    
    public VertScaleDialog(JFrame parent, ViewpointControl control,
                           VertScaleInfo vertScaleInfo) {
        this.control       = control;
        this.parent        = parent;
        this.vertScaleInfo = vertScaleInfo;
        doMakeContents();
    }

    /**
     * Make the widget contents (UI)
     */
    
    protected void doMakeContents() {
    	
        setLayout(new BorderLayout());
        GuiUtils.tmpInsets = new Insets(5, 5, 0, 0);

        fontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false, false);
        
        jtfAxisLabel = new JTextField(20);
        
        JPanel vertPanel = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Min Value: "), min = new JTextField(""),
            GuiUtils.rLabel("Max Value: "), max = new JTextField(""),
            GuiUtils.rLabel("Axis Label: "), jtfAxisLabel,
            GuiUtils.rLabel("Units: "),
            unitCombo = GuiUtils.getEditableBox(Misc.toList(new String[] {
                "m", "km", "feet", "fathoms" }), null),
            GuiUtils.rLabel("Major Increment: "),
            vertIncrement = new JTextField(),
            GuiUtils.rLabel("Minor Division: "),
            vertMinorSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1)),
            GuiUtils.rLabel("Visible: "), jcbVisible = new JCheckBox("", true),
        }, 2, GuiUtils.WT_NY, GuiUtils.WT_N);
        
        vertPanel.setBorder(BorderFactory.createTitledBorder("Vertical Scale Parameters"));

        min.setActionCommand(GuiUtils.CMD_OK);
        min.addActionListener(this);
        max.setActionCommand(GuiUtils.CMD_OK);
        max.addActionListener(this);
        
        JComponent fontPanel =
                GuiUtils.doLayout(new Component[] {
                    GuiUtils.left(fontSelector.getComponent()) }, 1,
                        GuiUtils.WT_N, GuiUtils.WT_N);
            fontPanel.setBorder(BorderFactory.createTitledBorder("Font"));
            
        List<?> pnls = Arrays.asList(new Component[] { GuiUtils.left(vertPanel),
        		GuiUtils.left(fontPanel) });
            
        this.add("Center", GuiUtils.doLayout(pnls, 1, 5, 5));
        
        if (vertScaleInfo != null) {
            min.setText(Misc.format(vertScaleInfo.minVertRange));
            max.setText(Misc.format(vertScaleInfo.maxVertRange));
            vertIncrement.setText("" + vertScaleInfo.getMajorIncrement());
            vertMinorSpinner.setValue(vertScaleInfo.getMinorDivision());

            if (vertScaleInfo.unit != null) {
                unitCombo.setSelectedItem(vertScaleInfo.unit.toString());
            }

            // In the text field, don't show units
            String tmpStr = vertScaleInfo.getLabel();
            if ((tmpStr == null) || (tmpStr.isEmpty())) {
            	tmpStr = VertScaleInfo.DEFAULT_AXIS_LABEL;
            } else {
            	// Actual Axis label will have units in parentheses, 
            	// strip this off for UI text field
            	if (tmpStr.indexOf(" (") > 0) {
            		tmpStr = tmpStr.substring(0 , tmpStr.indexOf(" ("));
            	}
            }
            jtfAxisLabel.setText(tmpStr);
            jcbVisible.setSelected(vertScaleInfo.visible);
            fontSelector.setFont(vertScaleInfo.getFont());
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
            dialog = new JDialog(parent, "Vertical Scale", false);
            dialog.getContentPane().add("Center", this);
            dialog.pack();
            dialog.setLocation(100, 100);
        }

        this.vertScaleInfo = transfer;
        min.setText(Misc.format(transfer.minVertRange));
        max.setText(Misc.format(transfer.maxVertRange));
        unitCombo.setSelectedItem(transfer.unit.toString());
        
        // minor increment
        vertMinorSpinner.setValue(transfer.getMinorDivision());
        
        // font
        fontSelector.setFont(transfer.getFont());
        
        // In the text field, don't show units
        String tmpStr = transfer.getLabel();
        
        if ((tmpStr == null) || (tmpStr.isEmpty())) {
        	tmpStr = VertScaleInfo.DEFAULT_AXIS_LABEL;
        } else {
        	// Actual Axis label will have units in parentheses, 
        	// strip this off for UI text field
        	if (tmpStr.indexOf(" (") > 0) {
        		tmpStr = tmpStr.substring(0 , tmpStr.indexOf(" ("));
        	}
        }
        jtfAxisLabel.setText(tmpStr);
        vertIncrement.setText(
        		"" + transfer.getMajorIncrement()
        );
        jcbVisible.setSelected(transfer.isVisible());
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

            if ( !Unit.canConvert(newUnit, CommonUnit.meter)) {
                throw new Exception();
            }
        } catch (Exception e) {
            LogUtil.userMessage("Unknown or incompatible unit "
                                + unitCombo.getSelectedItem());
            return false;
        }

        VertScaleInfo newTransfer = new VertScaleInfo(minValue, maxValue, newUnit);

        newTransfer.setLabel(jtfAxisLabel.getText() + " (" + newUnit.getIdentifier() + ")");
        
        // TJJ 2014 - should not be accessing variable directly but that's what 
        // classes using the Transfer were doing with "visible" and others.
        // Should be addressed at some point.

        newTransfer.setVisible(jcbVisible.isSelected());
        newTransfer.visible = jcbVisible.isSelected();
        newTransfer.setMajorIncrement(Misc.parseNumber(vertIncrement.getText()));
        newTransfer.setMinorDivision(Integer.valueOf(vertMinorSpinner.getValue().toString()));
        newTransfer.setFont(fontSelector.getFont());

        // Force redraw each time rather than checking equality between
        // newTransfer and vertScaleInfo. This is necessary to ensure the
        // vertical axis visibility is correct, when the horizontal axes are
        // manipulated. If this is too brute force we can check to see if the
        // view point control has  a MapProjectionDisplay.

        //        if ( !Misc.equals(newTransfer, vertScaleInfo)) {
        vertScaleInfo = newTransfer;

        try {
            control.applyVerticalRange(vertScaleInfo);
        } catch (Exception exc) {
            LogUtil.userMessage("An error has occurred: " + exc);
            return false;
        }
        //        }

        return true;
    }

    /**
     * Checks if is axis visible.
     *
     * @return true, if is axis visible
     */
    
    public boolean isAxisVisible() {
        return jcbVisible.isSelected();
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
