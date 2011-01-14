/**
 * $Id: DateSelectionGui.java,v 1.7 2007/07/06 20:45:29 jeffmc Exp $
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


import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DateUtil;



import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;


import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;



import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * Widget for setting properties of a DateSelection object
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.7 $
 */
public class DateSelectionGui extends JPanel {

    /** gui widget */
    private TimeLengthField intervalField;

    /** gui widget */
    private TimeLengthField preRangeField;

    /** gui widget */
    private TimeLengthField postRangeField;

    /** gui widget */
    private TimeLengthField startOffsetField;

    /** gui widget */
    private TimeLengthField endOffsetField;

    /** gui widget */
    private TimeLengthField roundToField;

    /** gui widget */
    private JComboBox startModeBox;

    /** gui widget */
    private JComboBox endModeBox;

    /** gui widget */
    private DateTimePicker startTimePicker;

    /** gui widget */
    private DateTimePicker endTimePicker;

    /** gui widget */
    private DateSelection dateSelection;

    /** gui widget */
    private JTextField countFld;

    /** gui widget */
    private JTextField skipFld;

    /**
     * ctor
     *
     * @param dateSelection the date selection
     */
    public DateSelectionGui(DateSelection dateSelection) {
        this.dateSelection = dateSelection;
        init();
    }

    /**
     * Set the date selection
     *
     * @param dateSelection the date selection
     */
    public void setDateSelection(DateSelection dateSelection) {
        this.dateSelection = dateSelection;
        updateWidgets();
    }


    /**
     * Enable the widgets based on the date selection mode
     */
    public void enableWidgets() {
        startTimePicker.setEnabled(GuiUtils.getValueFromBox(startModeBox)
                                   == DateSelection.TIMEMODE_FIXED);
        endTimePicker.setEnabled(GuiUtils.getValueFromBox(endModeBox)
                                 == DateSelection.TIMEMODE_FIXED);
    }


    /**
     * Create the gui
     */
    private void init() {
        skipFld          = new JTextField("", 4);
        countFld         = new JTextField("", 4);
        startTimePicker  = new DateTimePicker();
        endTimePicker    = new DateTimePicker();
        intervalField    = new TimeLengthField("Interval");

        startOffsetField = new TimeLengthField("Start Offset");
        endOffsetField   = new TimeLengthField("Start Offset");

        preRangeField    = new TimeLengthField("Before Range");
        postRangeField   = new TimeLengthField("After Range");
        startModeBox = GuiUtils.makeComboBox(DateSelection.TIMEMODES,
                                             DateSelection.STARTMODELABELS,
                                             dateSelection.getStartMode());
        endModeBox = GuiUtils.makeComboBox(DateSelection.TIMEMODES,
                                           DateSelection.ENDMODELABELS,
                                           dateSelection.getEndMode());

        startOffsetField = new TimeLengthField("Start Offset");
        endOffsetField   = new TimeLengthField("End Offset");
        Component[] comps;


        JComponent  startExtra = GuiUtils.wrap(startTimePicker);
        comps = new Component[] { GuiUtils.left(startModeBox),
                                  GuiUtils.hbox(new JLabel("Offset:"),
                                  startOffsetField.getContents()),
                                  startExtra };
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent startPanel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_N,
                                    GuiUtils.WT_N);
        JComponent endExtra = GuiUtils.wrap(endTimePicker);

        comps = new Component[] { GuiUtils.left(endModeBox),
                                  GuiUtils.hbox(new JLabel("Offset:"),
                                  endOffsetField.getContents()),
                                  endExtra };
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent endPanel = GuiUtils.doLayout(comps, 2, GuiUtils.WT_N,
                                  GuiUtils.WT_N);


        comps = new Component[] {
            GuiUtils.rLabel("Start Time:"), GuiUtils.left(startPanel),
            GuiUtils.rLabel("End Time:"), GuiUtils.left(endPanel),
            GuiUtils.rLabel("Interval:"),
            GuiUtils.left(GuiUtils.rowGrid(new Component[] {
                GuiUtils.cLabel("Before"), GuiUtils.cLabel("Center"),
                GuiUtils.cLabel("After"), preRangeField.getContents(),
                intervalField.getContents(), postRangeField.getContents()
            }, 2)),
            /*
            GuiUtils.left(intervalField.getContents()),
            GuiUtils.rLabel("Range:"),
            GuiUtils.left(preRangeField.getContents()),
            GuiUtils.rLabel("After Range:"),
            GuiUtils.left(postRangeField.getContents()),
            */
            GuiUtils.rLabel("Max Count:"),
            GuiUtils.left(GuiUtils.hbox(countFld, GuiUtils.filler(10, 5),
                                        GuiUtils.lLabel("Skip: "),
                                        GuiUtils.left(skipFld))),
        };

        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);
        setLayout(new BorderLayout());
        this.add(BorderLayout.CENTER, contents);

        updateWidgets();
    }


    /**
     * Apply state of date selection to the widgets
     */
    private void updateWidgets() {


        skipFld.setText(dateSelection.getSkip() + "");
        if (dateSelection.hasCount()) {
            countFld.setText(dateSelection.getCount() + "");
        } else {
            countFld.setText("");
        }
        intervalField.setTime(
            DateUtil.millisToMinutes(dateSelection.getInterval()));

        //        System.err.println ("before:" + dateSelection);
        preRangeField.setTime(
            DateUtil.millisToMinutes(dateSelection.getPreRange()));
        postRangeField.setTime(
            DateUtil.millisToMinutes(dateSelection.getPostRange()));
        GuiUtils.setValueOfBox(startModeBox, dateSelection.getStartMode(),
                               DateSelection.TIMEMODES,
                               DateSelection.STARTMODELABELS);
        GuiUtils.setValueOfBox(endModeBox, dateSelection.getEndMode(),
                               DateSelection.TIMEMODES,
                               DateSelection.ENDMODELABELS);
        startOffsetField.setTime(
            DateUtil.millisToMinutes(dateSelection.getStartOffset()));
        endOffsetField.setTime(
            DateUtil.millisToMinutes(dateSelection.getEndOffset()));
        startTimePicker.setDate(dateSelection.getStartFixedDate());
        endTimePicker.setDate(dateSelection.getEndFixedDate());

        enableWidgets();
    }


    /**
     * Apply widgets to date selection
     *
     * @return everything ok
     */
    public boolean applyProperties() {
        if ((GuiUtils.getValueFromBox(
                startModeBox) == GuiUtils.getValueFromBox(
                endModeBox)) && (GuiUtils.getValueFromBox(
                startModeBox) == DateSelection.TIMEMODE_RELATIVE)) {
            LogUtil.userErrorMessage(
                "The start and end modes cannot both be relative");
            return false;
        }
        double interval = intervalField.getTime();

        if (countFld.getText().trim().length() == 0) {
            dateSelection.setCount(DateSelection.MAX_COUNT);
        } else {
            dateSelection.setCount(
                new Integer(countFld.getText().trim()).intValue());
        }
        dateSelection.setSkip(
            new Integer(skipFld.getText().trim()).intValue());


        if (interval != interval) {
            dateSelection.setInterval(0);
        } else {
            dateSelection.setInterval(
                (long) DateUtil.minutesToMillis(interval));
        }
        double preRange = preRangeField.getTime();
        if (preRange != preRange) {
            dateSelection.setPreRange(Double.NaN);
        } else {
            dateSelection.setPreRange(
                (long) DateUtil.minutesToMillis(preRange));
        }
        double postRange = postRangeField.getTime();
        if (postRange != postRange) {
            dateSelection.setPostRange(Double.NaN);
        } else {
            dateSelection.setPostRange(
                (long) DateUtil.minutesToMillis(postRange));
        }

        dateSelection.setStartFixedTime(startTimePicker.getDate());
        dateSelection.setEndFixedTime(endTimePicker.getDate());
        dateSelection.setStartOffset(
            DateUtil.minutesToMillis(startOffsetField.getTime()));
        dateSelection.setEndOffset(
            DateUtil.minutesToMillis(endOffsetField.getTime()));
        dateSelection.setStartMode(GuiUtils.getValueFromBox(startModeBox));
        dateSelection.setEndMode(GuiUtils.getValueFromBox(endModeBox));
        //        System.err.println ("after:" + dateSelection);
        return true;
    }




}

