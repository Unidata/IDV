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

package ucar.unidata.idv.ui;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.Removable;

import java.awt.*;
import java.awt.event.*;

import java.lang.reflect.*;

import java.util.Hashtable;

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
public class ValueSliderComponent implements Removable {

    /** readout for value */
    protected Object callingObject;

    /** readout for value */
    private JTextField valueReadout;

    /** slider components */
    private JComponent[] sliderComps;

    /** value slider */
    private JSlider valueSlider;

    /** property that gets set by this widget */
    private String propertyName = null;

    /** name for label of this method */
    private String labelText = null;

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

    /** maximum slider value */
    private float scaleFactor = 1;

    /** flag for calling set method */
    private boolean useSetMethod = true;

    /** flag for calling set method */
    private String toolTipText;

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ValueSliderComponent.class.getName());

    /**
     * Create a ValueSliderComponent
     *
     * @param co  calling object
     * @param min minimum slider value
     * @param max maximum slider value
     * @param property Object's property to set
     * @param label  label for the widget
     */
    public ValueSliderComponent(Object co, int min, int max, String property,
                                String label) {
        this(co, min, max, property, label, 1, true);
    }

    /**
     * Create a ValueSliderComponent
     *
     * @param co  calling object
     * @param min minimum slider value
     * @param max maximum slider value
     * @param property Object's property to set
     * @param label  label for the widget
     * @param scale  scale factor for the values
     * @param andSet  set the property on the calling object if true
     */
    public ValueSliderComponent(Object co, int min, int max, String property,
                                String label, float scale, boolean andSet) {
        this(co, min, max, property, label, scale, andSet, null);
    }

    /**
     * Create a ValueSliderComponent
     *
     * @param co  calling object
     * @param min minimum slider value
     * @param max maximum slider value
     * @param property calling object property to set
     * @param label  label for the widget
     * @param scale  scale factor for the values
     * @param andSet  set the property on the calling object if true
     * @param tip  tool tip text for the widget
     */
    public ValueSliderComponent(Object co, int min, int max, String property,
                                String label, float scale, boolean andSet,
                                String tip) {
        callingObject = co;
        propertyName  = property;
        labelText     = label;
        sliderMin     = min;
        sliderMax     = max;
        scaleFactor   = scale;
        useSetMethod  = andSet;
        toolTipText   = tip;
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

        getMethod = Misc.findMethod(callingObject.getClass(), getMethodName,
                                    null);
        setMethod = Misc.findMethod(callingObject.getClass(), setMethodName,
                                    null);

        if (((getMethod == null) || (setMethod == null)) && useSetMethod) {
            logException(
                "ValueSliderComponent.init",
                new IllegalArgumentException(
                    "Unable to find set or get methods for property "
                    + propertyName));
        }

        ChangeListener listener = new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                try {
                    if ( !getObjectHasInitialized() || ignoreUIEvents) {
                        return;
                    }
                    ignoreUIEvents = true;
                    int value = valueSlider.getValue();
                    if (valueSlider.getValueIsAdjusting()) {
                        if (valueReadout != null) {
                            valueReadout.setText(
                                getDisplayConventions().format(
                                    value / scaleFactor));
                        }
                    } else {
                        handleValueChanged(value);
                    }
                    ignoreUIEvents = false;
                } catch (Exception exc) {
                    logException("adjust line width ", exc);
                }
            }
        };

        int initialValue = getInitialValue();

        //        System.err.println("slider:" + sliderMin + " " + sliderMax + " " + initialValue);
        if (sliderMax < initialValue) {
            sliderMax = initialValue;
        }
        if (sliderMin > initialValue) {
            sliderMin = initialValue;
        }
        //        initialValue = Math.min(Math.max(initialValue, sliderMin), sliderMax);
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
        if (scaleFactor != 1) {
            valueSlider.setLabelTable(makeLabelTable());
        }
        valueReadout =
            new JTextField(getDisplayConventions().format(initialValue
                / scaleFactor), 3);
        if (toolTipText != null) {
            valueReadout.setToolTipText(toolTipText);
        }

        valueReadout.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (ignoreUIEvents) {
                    return;
                }
                try {
                    float value = (float) Misc.parseNumber(
                                      valueReadout.getText().trim());
                    int     intValue   = (int) (value * scaleFactor);
                    boolean lastIgnore = ignoreUIEvents;
                    ignoreUIEvents = true;
                    if (valueSlider != null) {
                        valueSlider.setValue(intValue);
                    }
                    handleValueChanged(intValue);
                    ignoreUIEvents = lastIgnore;
                } catch (NumberFormatException nfe) {
                    LogUtil.userMessage("Incorrect format: "
                                        + valueReadout.getText());
                } catch (Exception exc) {
                    logException("Setting line width", exc);
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
                value = (int) (((Number) getMethod.invoke(callingObject,
                        (Object[]) null)).floatValue() * scaleFactor);
            } catch (Exception exc2) {
                logException("getInitialValue", exc2);
            }
        }
        return value;
    }

    /**
     * Handle a new value
     *
     * @param newValue  the value in slider range
     */
    private void handleValueChanged(int newValue) {
        if ((setMethod != null) && useSetMethod) {
            try {
                String floatString = getDisplayConventions().format(newValue
                                         / scaleFactor);
                Misc.setProperty(callingObject, setMethod, floatString,
                                 false);
            } catch (Exception exc2) {
                logException("propertyChange", exc2);
            }
        }
    }

    /**
     * Get the display conventions
     *
     * @return the display conventions.
     */
    private DisplayConventions getDisplayConventions() {
        return DisplayConventions.getDisplayConventions();
    }

    /**
     * Set the value on the widget.
     * @param value  new value
     */
    public void setValue(int value) {
        setValue((float) value);
    }

    /**
     * Set the value on the widget.
     * @param value  new value
     */
    public void setValue(float value) {
        valueReadout.setText("" + value);
    }

    /**
     * Get the value of the widget
     * @return new value
     */
    public float getValue() {
        return (float) GuiUtils.getValue(valueReadout);
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
     * Make the label table for the slider
     *
     * @return a label table
     */
    private Hashtable<Integer, JLabel> makeLabelTable() {
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer,
                                                    JLabel>();
        float min       = sliderMin / scaleFactor;
        float max       = sliderMax / scaleFactor;
        float increment = valueSlider.getMajorTickSpacing() / scaleFactor;
        if ((min > max) || (increment > (max - min))) {
            return labelTable;
        }
        float[] values = Misc.computeTicks(max, min, 0, increment);
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                labelTable.put(
                    new Integer((int) (values[i] * scaleFactor)),
                    new JLabel(getDisplayConventions().format(values[i])));
            }
        }
        return labelTable;

    }


    /**
     * Set the property on the slider
     * @param value  true to snap slider to ticks
     */
    public void setSnapToTicks(boolean value) {
        valueSlider.setSnapToTicks(value);
    }

    /**
     * Set the property on the slider
     * @param value  true to paint slider ticks
     */
    public void setPaintTicks(boolean value) {
        valueSlider.setPaintTicks(true);
    }

    /**
     * Set the property on the slider
     * @param value  true to paint slider labels
     */
    public void setPaintLabels(boolean value) {
        valueSlider.setPaintLabels(true);
    }

    /**
     * Has the calling object been initialized?  Subclasses should
     * override if necessary.
     * @return true
     */
    public boolean getObjectHasInitialized() {
        return true;
    }

    /**
     * Shortcut to logging facility for subclasses to use
     *
     * @param msg   error message
     * @param exc   error Exception
     */
    public void logException(String msg, Exception exc) {
        LogUtil.printException(log_, msg, exc);
    }

    /**
     *  Remove the reference to the displayControl
     */
    public void doRemove() {
        callingObject = null;
    }

}
