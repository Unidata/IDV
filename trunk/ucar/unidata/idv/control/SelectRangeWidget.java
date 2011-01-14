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


import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 * A widget for the control window for viewing data range properties
 *
 * @author  Unidata Development Team
 * @version $Revision: 1.7 $
 */
public class SelectRangeWidget extends ControlWidget {

    /** range */
    private Range range;

    /** The label for widget */
    private JLabel label;

    /** A button for brining up the editor */
    private JButton button;

    /** is it enabled */
    private JCheckBox enabledCbx;

    /** The right hand label that shows some of the contour information */
    private JLabel rhLabel;

    /** Change range dialog */
    private RangeDialog rangeDialog;

    /**
     * Construct a SelectRangeWidget
     *
     * @param control      the associate control
     * @param range The initial range
     */
    public SelectRangeWidget(DisplayControlImpl control, Range range) {
        this(control, range, "Change Range");
    }

    /**
     * Construct a SelectRangeWidget
     *
     * @param control      the associate control
     * @param range The initial range
     * @param dialogTitle Dialog title
     */
    public SelectRangeWidget(DisplayControlImpl control, Range range,
                             String dialogTitle) {
        super(control);
        label   = new JLabel("Visible Range:", SwingConstants.RIGHT);
        rhLabel = new JLabel(" ");
        setRange(range);
        button     = new JButton("Change");
        enabledCbx = GuiUtils.makeCheckbox("", this, "selectRangeEnabled");
        button.addActionListener(this);
        rhLabel.setEnabled(enabledCbx.isSelected());
        //        button.setEnabled(enabledCbx.isSelected());
    }

    /**
     * Method public due to ActionListener implementation
     *
     * @param ae    action event
     */
    public void actionPerformed(ActionEvent ae) {
        showChangeRangeDialog();
    }


    /**
     * Show the dialog
     */
    public void showChangeRangeDialog() {
        if (rangeDialog == null) {
            rangeDialog = new RangeDialog(getDisplayControl(), range,
                                          "Change Visible Range",
                                          "setSelectRange", button);
        }
        rangeDialog.showDialog();
    }

    /**
     * Set the range information for this widget.
     *
     * @param r  new Range
     */
    public void setRange(Range r) {
        this.range = r;
        if (r != null) {
            rhLabel.setText("From: "
                            + getDisplayConventions().format(r.getMin())
                            + " To: "
                            + getDisplayConventions().format(r.getMax()));
            if (rangeDialog != null) {
                rangeDialog.setRangeDialog(r);
            }
        }
    }

    /**
     * Get the label for this widget.
     *
     * @return   the label.
     */
    public JLabel getLabel() {
        return label;
    }

    /**
     * Fill a list of components
     *
     * @param l    list of widgets
     * @param columns  number of columns for layout
     */
    public void fillList(List l, int columns) {
        l.add(label);
        l.add(GuiUtils.doLayout(new Component[] { enabledCbx,
                GuiUtils.inset(button, new Insets(0, 8, 0, 0)),
                new Label(" "), rhLabel }, 4, GuiUtils.WT_NNNY,
                                           GuiUtils.WT_N));
    }

    /**
     * Are we enabled
     *
     * @return enabled
     */
    public boolean getSelectRangeEnabled() {
        return displayControl.getSelectRangeEnabled();
    }

    /**
     * Set enabled
     *
     * @param v is enabled
     */
    public void setSelectRangeEnabled(boolean v) {
        rhLabel.setEnabled(v);
        //        button.setEnabled(v);
        displayControl.setSelectRangeEnabled(v);
    }

    /**
     * Get the range from the color table
     *
     * @return range from the color table
     */
    public Range getRangeFromColorTable() {

        Range ctRange = null;
        ColorTable originalCT =
            getDisplayControl().getOldColorTableOrInitialColorTable();
        if (originalCT != null) {
            ctRange = originalCT.getRange();
        }
        return ctRange;
    }

    /**
     * Called to remove this from the display.
     */
    public void doRemove() {
        super.doRemove();
        if (rangeDialog != null) {
            rangeDialog.doRemove();
            rangeDialog = null;
        }
    }

}
