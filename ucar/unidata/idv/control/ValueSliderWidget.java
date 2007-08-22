/*
 * $Id: ValueSliderWidget.java,v 1.8 2007/08/21 14:30:37 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import java.awt.event.*;

import java.lang.reflect.*;

import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A class for holding a text field and a slider for setting the
 * some integer value
 *
 * @author IDV Development Team
 * @version $Revision: 1.8 $
 */
public class ValueSliderWidget {

    /** readout for value */
    DisplayControlImpl displayControl;

    /** readout for value */
    JTextField valueReadout;

    /** slider components */
    JComponent[] sliderComps;

    /** value slider */
    JSlider valueSlider;

    /** property that gets set by this widget */
    String propertyName = null;

    /** name for label of this method */
    String labelText = null;

    /** flag for ignoring UI events */
    private boolean ignoreUIEvents = false;

    /** set Method */
    private Method setMethod = null;

    /** set Method */
    private Method getMethod = null;

    /** minimum slider value */
    private int sliderMin;

    /** maximum slider value */
    private int sliderMax;

    /**
     * Create a ValueSliderWidget
     *
     * @param dc  the display control to use
     * @param min minimum slider value
     * @param max maximum slider value
     * @param property DisplayControl property to set
     * @param label  label for the widget
     */
    public ValueSliderWidget(DisplayControlImpl dc, int min, int max,
                             String property, String label) {
        displayControl = dc;
        propertyName   = property;
        labelText      = label;
        sliderMin      = min;
        sliderMax      = max;
        init();
    }

    /**
     * Initialize the class
     */
    private void init() {
        String methodBase = propertyName.substring(0, 1).toUpperCase()
                            + propertyName.substring(1);
        String setMethodName = "set" + methodBase;
        String getMethodName = "get" + methodBase;

        getMethod = Misc.findMethod(displayControl.getClass(), getMethodName,
                                    null);
        setMethod = Misc.findMethod(displayControl.getClass(), setMethodName,
                                    null);

        if ((getMethod == null) || (setMethod == null)) {
            displayControl.logException(
                "ValueSlider.init",
                new IllegalArgumentException(
                    "Unable to find set/get methods for property "
                    + propertyName));
        }

        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                try {
                    if ( !displayControl.getHaveInitialized()
                            || ignoreUIEvents) {
                        return;
                    }
                    ignoreUIEvents = true;
                    int value = valueSlider.getValue();
                    if (valueSlider.getValueIsAdjusting()) {
                        if (valueReadout != null) {
                            valueReadout.setText(String.valueOf(value));
                        }
                    } else {
                        handleValueChanged(value);
                    }
                    ignoreUIEvents = false;
                } catch (Exception exc) {
                    displayControl.logException("adjust line width ", exc);
                }
            }
        };

        int initialValue = getInitialValue();

        sliderComps = GuiUtils.makeSliderPopup(sliderMin, sliderMax,
                initialValue, listener);
        sliderComps[0].setToolTipText("Change " + labelText);
        valueSlider = (JSlider) sliderComps[1];
        int tickSpacing      = Math.max((sliderMax - sliderMin + 1) / 2, 1);
        int minorTickSpacing = Math.max((sliderMax - sliderMin + 1) / 10, 1);
        valueSlider.setMajorTickSpacing(tickSpacing);
        valueSlider.setMinorTickSpacing(minorTickSpacing);
        valueSlider.setSnapToTicks(true);
        valueSlider.setPaintTicks(true);
        valueSlider.setPaintLabels(true);

        valueReadout = new JTextField(String.valueOf(initialValue), 3);

        valueReadout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (ignoreUIEvents) {
                    return;
                }
                try {
                    int value =
                        (int) Misc.parseNumber(valueReadout.getText().trim());
                    boolean lastIgnore = ignoreUIEvents;
                    ignoreUIEvents = true;
                    if (valueSlider != null) {
                        valueSlider.setValue(value);
                    }
                    handleValueChanged(value);
                    ignoreUIEvents = lastIgnore;
                } catch (NumberFormatException nfe) {
                    displayControl.userMessage("Incorrect format: "
                            + valueReadout.getText());
                } catch (Exception exc) {
                    displayControl.logException("Setting line width", exc);
                }
            }
        });

    }

    /**
     * Get the GUI contents
     * @param withLabel  true if the component should include the label
     * @return the component
     */
    public JComponent getContents(boolean withLabel) {
        JLabel label = GuiUtils.rLabel(labelText + ": ");
        //label.setLabelFor(valueReadout);
        JPanel p = (withLabel)
                   ? GuiUtils.leftRight(label,
                                        GuiUtils.doLayout(new Component[] {
                                            valueReadout,
                                            GuiUtils.filler(),
                                            sliderComps[0] }, 3,
                                                GuiUtils.WT_N, GuiUtils.WT_N))
                   : GuiUtils.left(GuiUtils.doLayout(new Component[] {
                       valueReadout,
                       GuiUtils.filler(), sliderComps[0] }, 3, GuiUtils.WT_N,
                           GuiUtils.WT_N));
        return p;
    }

    /**
     * Get initial value.
     *
     * @return the initial value
     */
    private int getInitialValue() {
        int value = 0;
        if (getMethod != null) {
            try {
                value = ((Number) getMethod.invoke(displayControl,
                        (Object[]) null)).intValue();
            } catch (Exception exc2) {
                displayControl.logException("getInitialValue", exc2);
            }
        }
        return value;
    }

    /**
     * Handle a new value
     *
     * @param newValue  the new value to handle
     */
    private void handleValueChanged(int newValue) {
        if (setMethod != null) {
            try {
                setMethod.invoke(displayControl,
                                 new Object[] { new Integer(newValue) });
            } catch (Exception exc2) {
                displayControl.logException("propertyChange", exc2);
            }
        }
    }

    /**
     * Enable or disable the widget.
     * @param enable  true to enable
     */
    public void setEnabled(boolean enable) {
        valueReadout.setEnabled(enable);
        sliderComps[0].setEnabled(enable);
    }

    /**
     *  Remove the reference to the displayControl
     */
    public void doRemove() {
        displayControl = null;
    }
}

