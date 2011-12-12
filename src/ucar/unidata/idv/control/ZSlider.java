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


import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.Misc;



import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A widget for the control window for viewing contour properties and
 * launching the editor.
 *
 * @author  IDV Development Team
 * @version $Revision: 1.27 $
 */
public class ZSlider {

    /** _more_ */
    private JSlider slider;

    /** _more_ */
    private double zValue;

    /** _more_ */
    private JComponent contents;

    /** _more_ */
    private boolean ignoreSliderEvents = false;

    /** _more_ */
    private JTextField valueFld;

    /** _more_ */
    DecimalFormat format = new DecimalFormat("0.###");


    /**
     * _more_
     *
     * @param zValue _more_
     */
    public ZSlider(double zValue) {
        this.zValue = zValue;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private JComponent doMakeContents() {
        valueFld = new JTextField(format.format(zValue), 5);
        valueFld.setToolTipText(
            "Enter an exact value between -1 and 1 and hit return");
        valueFld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (ignoreSliderEvents) {
                    return;
                }
                setValue(Misc.toDouble(valueFld.getText().trim()));
                valueHasBeenSet();
            }
        });
        int min       = -100;
        int max       = 100;
        int sliderPos = (int) (zValue * 100);
        sliderPos = Math.min(Math.max(sliderPos, min), max);
        slider    = new JSlider(min, max, sliderPos);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (ignoreSliderEvents) {
                    return;
                }
                zValue = slider.getValue() / 100.;
                valueFld.setText(format.format(zValue));
                if ( !slider.getValueIsAdjusting()) {
                    valueHasBeenSet();
                }
            }
        });

        JPanel labelPanel = GuiUtils.leftCenterRight(new JLabel("Bottom"),
                                GuiUtils.cLabel("Middle"),
                                GuiUtils.rLabel("Top"));

        JPanel sliderPanel = GuiUtils.doLayout(new Component[] { slider,
                valueFld, labelPanel, GuiUtils.filler() }, 2, GuiUtils.WT_YN,
                    GuiUtils.WT_NN);
        return sliderPanel;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public double getValue() {
        return zValue;
    }

    /**
     * _more_
     *
     * @param v _more_
     */
    public void setValue(double v) {
        ignoreSliderEvents = true;
        zValue             = v;
        slider.setValue((int) (100 * v));
        valueFld.setText(format.format(zValue));
        ignoreSliderEvents = false;
    }

    /**
     * _more_
     */
    public void valueHasBeenSet() {}

}
