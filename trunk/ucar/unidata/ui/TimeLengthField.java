/*
 * $Id: TimeLengthField.java,v 1.13 2007/07/06 20:45:33 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.ui;


import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.awt.*;
import java.awt.event.*;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;


import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Class TimeField is a widget that provides for showing a time length label  borken down as
 * days/hours/minutes and a dialog that allows the user to set the time length.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class TimeLengthField {

    /**
     * The PropertyChangeListener-s.
     */
    private PropertyChangeSupport changeListeners;


    /** Only show positive numbers */
    boolean isPositiveOnly = false;


    /** The time */
    double time = 0.0;

    /** gui widget */
    JTextField yearsFld;

    /** gui widget */
    JTextField daysFld;

    /** gui widget */
    JTextField hoursFld;

    /** gui widget */
    JTextField minutesFld;

    /** gui widget */
    JLabel label;

    /** gui widget */
    JComboBox signCbx;

    /** gui widget */
    ImageIcon editIcon = null;

    /** The edit button */
    JButton editButton;

    /** Name to use in dialog */
    String name = "Interval";

    /** Description of what we are converting. Used in showing error messages to user */
    private String intervalText;

    /**
     * ctor
     *
     * @param name The name we use
     */
    public TimeLengthField(String name) {
        this(name, false);
    }


    /**
     * ctor
     *
     * @param name The name we use
     * @param isPositiveOnly Only show positive numbers
     */
    public TimeLengthField(String name, boolean isPositiveOnly) {
        this(name, isPositiveOnly, true);
    }


    /**
     * ctor
     *
     * @param name The name we use
     * @param minutes minutes
     */
    public TimeLengthField(String name, double minutes) {
        this(name, minutes, false, true);
    }



    /**
     * ctor
     *
     * @param name The name
     * @param isPositiveOnly only have positive values
     * @param showYearsFld Also show the years field
     */
    public TimeLengthField(String name, boolean isPositiveOnly,
                           boolean showYearsFld) {

        this(name, 0.0, isPositiveOnly, showYearsFld);
    }

    /**
     * ctor
     *
     * @param name The name
     * @param minutes Number of initial minutes
     * @param isPositiveOnly only have positive values
     * @param showYearsFld Also show the years field
     */
    public TimeLengthField(String name, double minutes,
                           boolean isPositiveOnly, boolean showYearsFld) {
        changeListeners     = new PropertyChangeSupport(this);
        this.isPositiveOnly = isPositiveOnly;
        this.name           = name;
        signCbx             = new JComboBox();
        GuiUtils.setListData(signCbx, Misc.newList("+", "-"));
        if (showYearsFld) {
            yearsFld = new JTextField("", 4);
        }
        daysFld    = new JTextField("", 3);
        hoursFld   = new JTextField("", 3);
        minutesFld = new JTextField("", 3);
        Font monoFont = Font.decode("monospaced");
        label = new JLabel(" ");
        label.setFont(monoFont);
        setTime(minutes);
    }


    /**
     * Utility to convert text to a double
     *
     * @param t text
     *
     * @return double value_
     */
    private double getNum(String t) {
        intervalText = t;
        if (t.length() == 0) {
            return 0.0;
        }
        return new Double(t).doubleValue();
    }





    /**
     * Show the dialog
     *
     * @return User pressed ok
     */
    public boolean showDialog() {
        return showDialog(null);
    }


    /**
     * Get the gui component
     *
     * @param allFields ignored
     *
     * @return the gui
     */
    public JComponent getFieldsComponent(boolean allFields) {
        List comps = new ArrayList();
        if ( !isPositiveOnly) {
            comps.add(signCbx);
            comps.add(new JLabel("  "));
        }

        if (yearsFld != null) {
            comps.add(yearsFld);
            comps.add(new JLabel(" Years   "));
        }

        comps.add(daysFld);
        comps.add(new JLabel(" Days   "));
        comps.add(hoursFld);
        comps.add(new JLabel(" Hours   "));
        comps.add(minutesFld);
        comps.add(new JLabel(" Minutes   "));
        return GuiUtils.left(GuiUtils.hbox(comps));
    }



    /**
     * Show the dialog
     *
     * @param topMsg If non-null show this in the dialog as a label
     * @return User pressed ok
     */
    public boolean showDialog(String topMsg) {
        double oldTime = time;
        setTime(time);
        JComponent contents = getFieldsComponent(true);
        if (topMsg != null) {
            contents = GuiUtils.topCenter(GuiUtils.inset(new JLabel(topMsg),
                    5), contents);
        }
        while (true) {
            if ( !GuiUtils.showOkCancelDialog(null, "Set " + name,
                    GuiUtils.inset(contents, 5), editButton)) {
                return false;
            }
            if (applyFields(true)) {
                break;
            }
        }
        changeListeners.firePropertyChange("time", new Double(oldTime),
                                           new Double(time));
        return true;
    }


    /**
     * Apply the fields to the internal state
     *
     * @param allFields  ignored
     *
     * @return everything ok
     */
    public boolean applyFields(boolean allFields) {
        try {
            String minutesText = minutesFld.getText().trim();
            String hoursText   = hoursFld.getText().trim();
            String daysText    = daysFld.getText().trim();
            String yearsText   = "";
            if (yearsFld != null) {
                yearsText = yearsFld.getText().trim();
            }

            //Check if the fields are empty. If so then we are undefined and
            //set time with NaN
            if ((minutesText.length() == 0) && (hoursText.length() == 0)
                    && (daysText.length() == 0)
                    && (yearsText.length() == 0)) {
                setTime(Double.NaN);
                return true;
            }

            double newTime = getNum(minutesText);
            newTime += getNum(hoursText) * 60;
            newTime += getNum(daysText) * 60 * 24;
            if (yearsFld != null) {
                newTime += getNum(yearsText) * 365 * 60 * 24;
            }
            if (isPositiveOnly) {
                newTime = Math.abs(newTime);
            } else {
                if (signCbx.getSelectedIndex() == 1) {
                    newTime = -newTime;
                }
            }
            time = newTime;
            setTime(time);
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad value:" + intervalText);
            return false;
        }
        return true;
    }


    /**
     * Adds a PropertyChangeListener.
     *
     * @param listener          The PropertyChangeListener to add.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeListeners.addPropertyChangeListener(listener);
    }


    /**
     * Get the gui contents
     *
     * @return contents
     */
    public JComponent getContents() {
        if (editIcon == null) {
            editIcon = GuiUtils.getImageIcon(
                "/auxdata/ui/icons/calendar_edit.png", getClass());
        }
        editButton = GuiUtils.getImageButton(editIcon);
        editButton.setToolTipText("Edit " + name);
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showDialog(null);
            }
        });
        return GuiUtils.wrap(GuiUtils.hbox(label, editButton));
    }

    /**
     * Current time interval as minutes
     *
     * @return time length as minutes
     */
    public double getTime() {
        return (isPositiveOnly
                ? Math.abs(time)
                : time);
    }

    /**
     * set the time as minutes
     *
     * @param minutes minutes
     */
    public void setTime(double minutes) {
        if (minutes != minutes) {
            time = minutes;
            label.setText(StringUtil.padLeft("Undefined", 12));
            if (yearsFld != null) {
                yearsFld.setText("");
            }
            daysFld.setText("");
            hoursFld.setText("");
            minutesFld.setText("");
            return;
        }


        if (isPositiveOnly) {
            minutes = Math.abs(minutes);
        }
        boolean negative = (minutes < 0);
        time    = minutes;
        minutes = Math.abs(minutes);
        if (signCbx == null) {
            return;
        }
        if (negative) {
            signCbx.setSelectedIndex(1);
        } else {
            signCbx.setSelectedIndex(0);
        }


        double years    = (double) (minutes / (365 * 60 * 24));
        int    intYears = (int) years;
        if (years > 1000) {
            minutes -= (years * 365 * 60 * 24);
        } else {
            minutes -= (intYears * 365 * 60 * 24);
        }


        int days = (int) (minutes / (60 * 24));
        if (yearsFld != null) {
            yearsFld.setText("" + intYears);
        } else {
            days += 365 * intYears;
        }

        daysFld.setText("" + days);
        minutes -= (days * 60 * 24);

        int hours = (int) (minutes / 60);
        minutes -= (hours * 60);
        hoursFld.setText("" + hours);

        double tmp  = minutes;
        String text = "";
        if (minutes == 0) {
            text = "0";
        } else if (minutes == (int) minutes) {
            text = text + ((int) minutes);
        } else {
            text = text + minutes;
        }
        minutesFld.setText(text);
        text = (negative
                ? "- "
                : "");
        if (intYears != 0) {
            text = text + intYears + "y ";
        }
        if (days != 0) {
            text = text + days + "d ";
        }
        if (hours != 0) {
            text = text + hours + "h ";
        }
        if (minutes != 0) {
            if (minutes == (int) minutes) {
                text = text + ((int) minutes) + "m";
            } else {
                text = text + (minutes) + "m";
            }
        } else if (text.length() == 0) {
            text = text + "0" + "m";
        }
        label.setText(StringUtil.padLeft(text, 12));
        timeChanged();
    }

    /**
     * Signal that the time changed
     */
    public void timeChanged() {}

    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return label.getText();
    }

}

