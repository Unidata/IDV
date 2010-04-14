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


import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.GuiUtils;

import visad.Unit;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;


/**
 * A widget for the control window for viewing contour properties and
 * launching the editor.
 *
 * @author  IDV Development Team
 * @version $Revision: 1.27 $
 */
public class ContourWidget extends ControlWidget {

    /** contour information */
    private ContourInfo contourInfo;

    /** The label for widget */
    private JLabel label;

    /** The right hand label that shows some of the contour information */
    private JLabel rhLabel;

    /** A button for brining up the editor */
    private JButton button;

    /**
     * Construct a ContourWidget
     *
     * @param control      the associate control
     * @param contourInfo  the contour information
     */
    public ContourWidget(DisplayControlImpl control,
                         ContourInfo contourInfo) {
        super(control);
        label   = new JLabel("Contour:", SwingConstants.RIGHT);
        rhLabel = new JLabel(" ");
        setContourInfo(contourInfo);
        button = new JButton("Change");
        button.addActionListener(this);
    }

    /**
     * Method public due to ActionListener implementation
     *
     * @param ae    action event
     */
    public void actionPerformed(ActionEvent ae) {
        showContourPropertiesDialog();
    }

    /**
     * Popup the contour properties dialog
     */
    public void showContourPropertiesDialog() {
        Unit unit = getDisplayControl().getDisplayUnit();
        ContLevelDialog contDialog = new ContLevelDialog(displayControl,
                                         "Contour Properties Editor", unit);
        contDialog.showDialog(new ContourInfo(contourInfo));
    }

    /**
     * Set the contour information for this widget and update the
     * labels.
     *
     * @param ci    new contour information
     */
    public void setContourInfo(ContourInfo ci) {
        this.contourInfo = ci;
        if (ci != null) {
            Unit   unit       = getDisplayControl().getDisplayUnit();
            String unitString = "";
            if (unit != null) {
                unitString = " " + unit.toString() + " ";
            }

            rhLabel.setText("Interval: " + (ci.getIntervalDefined()
                                            ? getDisplayControl()
                                            .getDisplayConventions()
                                            .format(ci
                                                .getInterval()) + unitString
                                            : "Irregular"));
        }
    }

    /**
     * Get the button for this widget.
     *
     * @return   the button
     */
    public JButton getButton() {
        return button;
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
        l.add(GuiUtils.doLayout(new Component[] { button, new Label(" "),
                rhLabel }, 3, GuiUtils.WT_NNY, GuiUtils.WT_N));
    }

}
