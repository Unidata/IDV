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

package ucar.unidata.ui;


import ucar.unidata.util.GuiUtils;


import java.awt.BorderLayout;

import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner.DateEditor;
import javax.swing.SpinnerDateModel;



/**
 * Widget for selecting dates and times
 *
 *
 * @author IDV Development Team
 */
public class DateTimePicker extends JPanel {

    /** the default time zone */
    private static TimeZone defaultTimeZone;

    /** the SpinnerDateModel */
    SpinnerDateModel datetimeModel;

    /**
     * Default  ctor
     */
    public DateTimePicker() {
        this(null, true);
    }

    /**
     * Create a DateTimePicker with the initial date.
     * @param date initial date
     */
    public DateTimePicker(Date date) {
        this(date, true);
    }

    /**
     * Create a DateTimePicker with the initial date.
     * @param date initial date
     * @param includeHours   true to have an hour picker
     */
    public DateTimePicker(Date date, boolean includeHours) {
        Calendar calendar = getCalendar(null);
        setLayout(new BorderLayout());

        datetimeModel = new SpinnerDateModel(calendar.getTime(), null, null,
                                         Calendar.HOUR_OF_DAY);
        javax.swing.JSpinner spinner = new javax.swing.JSpinner(datetimeModel);
        DateEditor           editor;
        if (includeHours) {
            editor = new DateEditor(spinner, "yyyy-MM-dd HH:mm");
        } else {
            editor = new DateEditor(spinner, "yyyy-MM-dd");
        }

        spinner.setEditor(editor);
        SimpleDateFormat format =
            ((DateEditor) spinner.getEditor()).getFormat();
        format.setTimeZone(
            TimeZone.getTimeZone(TimeZone.getTimeZone("GMT").getID()));

        JPanel timeComp = GuiUtils.hbox(spinner, new JLabel(" "
                              + getDefaultTimeZone().getID()), 5);

        add(BorderLayout.CENTER, timeComp);
        if (date != null) {
            setDate(date);
        }
    }

    /**
     * Set the default time zone for all instances
     *
     * @param timeZone  the time zone
     */
    public static void setDefaultTimeZone(TimeZone timeZone) {
        defaultTimeZone = timeZone;
    }

    /**
     * Get the default time zone
     *
     * @return  the default time zone
     */
    public static TimeZone getDefaultTimeZone() {
        if (defaultTimeZone == null) {
            defaultTimeZone = TimeZone.getTimeZone("GMT");
        }
        return defaultTimeZone;
    }


    /**
     * Get the Date that has been set
     *
     * @return  the date
     */
    public Date getDate() {
        Date     d  = datetimeModel.getDate();
        Calendar c0 = getCalendar(d);
        Calendar c1 = new GregorianCalendar(c0.get(Calendar.YEAR),
                                            c0.get(Calendar.MONTH),
                                            c0.get(Calendar.DAY_OF_MONTH));

        if (datetimeModel != null) {
            Date     time    = datetimeModel.getDate();
            Calendar timeCal = getCalendar(time);
            c1.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY));
            c1.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE));
            c1.set(Calendar.SECOND, timeCal.get(Calendar.SECOND));
        }
        return c1.getTime();
    }

    /**
     * Get the calendar for this object
     *
     * @param d  the date
     *
     * @return  the associated calendar
     */
    private Calendar getCalendar(Date d) {
        Calendar calendar = new GregorianCalendar();
        if (d != null) {
            calendar.setTime(d);
        }
        return calendar;
    }


    /**
     * Set the Date.
     *
     * @param d  the new Date
     */
    public void setDate(Date d) {
        datetimeModel.setValue(d);
    }
}
