/*
 * Copyright 1997-2024 Unidata Program Center/University Corporation for
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

package ucar.visad.data;


import ucar.nc2.time.*;

import ucar.nc2.time.Calendar;
import ucar.visad.Util;
import visad.CommonUnit;
import visad.CoordinateSystem;
import visad.DateTime;
import visad.ErrorEstimate;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.Unit;
import visad.VisADException;


import java.util.*;


/**
 * A wrapper around a ucar.nc2.time.CalendarDate to handle non-standard calendars
 */
public class CalendarDateTime extends visad.DateTime {

    /** the local CalendarDate */
    private CalendarDate calendarDate;

    /**
     * Construct a CalendarDateTime
     *
     * @param cd  the CalendarDate
     *
     * @throws VisADException problem creating the VisAD DateTime
     */
    public CalendarDateTime(CalendarDate cd) throws VisADException {
        super(cd.getMillis() / 1000.0);
        this.calendarDate = cd;
    }

    /**
     * Construct a CalendarDateTime
     *
     * @param date the Date
     *
     * @throws VisADException problem creating the VisAD DateTime
     */
    public CalendarDateTime(Date date) throws VisADException {
        this(date.getTime() / 1000.0, null);
    }

    /**
     * Construct a CalendarDateTime
     *
     * @param dt  the dateTime
     *
     * @throws VisADException  problem creating Data object
     */
    public CalendarDateTime(DateTime dt) throws VisADException {
        this(dt.getValue(CommonUnit.secondsSinceTheEpoch), null);
        if (dt instanceof CalendarDateTime) {
            this.calendarDate = ((CalendarDateTime) dt).calendarDate;
        }
    }

    /**
     * Construct a CalendarDateTime
     *
     * @param secs seconds since the epoch
     * @param cal  the associated Calendar
     *
     * @throws VisADException problem creating the VisAD DateTime
     */
    public CalendarDateTime(double secs, Calendar cal) throws VisADException {
        super(secs);
        this.calendarDate = makeCalendarDate(cal, (long) secs * 1000l);
    }

    /**
     * Construct a CalendarDateTime
     *
     * @throws VisADException problem creating Data object
     */
    public CalendarDateTime() throws VisADException {
        super();
        this.calendarDate = makeCalendarDate(null, (long) getValue() * 1000l);
    }

    /**
     * Make a CalendarDate from the calendar and the instance values
     *
     * @param cal    the Calendar
     * @param msecs  the millis
     *
     * @return the associated CalendarDate
     */
    private static CalendarDate makeCalendarDate(Calendar cal, long msecs) {
        return CalendarDate.of(cal, msecs);
    }

    /**
     * Return a string representation of this DateTime from a user
     * specified format.  The pattern uses the time format syntax
     * of java.text.SimpleDateFormat and the time zone is any of the
     * valid java.util.TimeZone values.
     * @see java.text.SimpleDateFormat
     * @see java.util.TimeZone
     *
     * @param   pattern         time format string
     * @param   timeZone        time zone to use
     * @return  String representing the date/time in the form specified
     *          by the pattern.
     */
    public String formattedString(String pattern, TimeZone timeZone) {
        CalendarTimeZone      ctz = new CalendarTimeZone(timeZone);
        CalendarDateFormatter cdf = new CalendarDateFormatter(pattern, ctz);
        return cdf.toString(calendarDate);
    }

    /**
     * Create a DateTime object from a string specification using the
     * supplied pattern and timezone.
     * @param  dateString     date string specification
     * @param  format             format string
     * @param  timezone       TimeZone to use
     * @param cal  the Calendar
     *
     * @return a DateTime
     * @throws  VisADException  formatting problem
     */
    public static CalendarDateTime createDateTime(String dateString,
            String format, TimeZone timezone, Calendar cal)
            throws VisADException {
        // TODO: make this work
        //return new CalendarDateTime(
        //    CalendarDateFormatter.isoStringToCalendarDate(
        //        Calendar.getDefault(), dateString));
        CalendarDateFormatter cdf = new CalendarDateFormatter(format,
                                        new CalendarTimeZone(timezone), cal);
        return new CalendarDateTime(cdf.parse(dateString));
    }

    /**
     * Create a Gridded1DDoubleSet from an array of DateTimes
     *
     * @param  times  array of DateTimes.  Array cannot be null or only
     *                have one entry.
     *
     * @return Gridded1DDouble set representing the array
     * @throws VisADException  couldn't create the GriddedDoubleSet
     */
    public static CalendarDateTimeSet makeTimeSet(DateTime[] times)
            throws VisADException {
        if ((times == null) || (times.length == 0)) {
            return null;
        }
        Calendar cal = null;
        if (times[0] instanceof CalendarDateTime) {
            cal = ((CalendarDateTime) times[0]).getCalendar();
        }
        if(cal != null && cal.equals(Calendar.julian)){
            DateTime[] timeArray = new DateTime[times.length];
            for(int i=0; i < times.length; i++){
                DateTime dt = times[i];
                TimeZone zoneInfo = dt.getFormatTimeZone();
                CalendarDate cdate = ((CalendarDateTime) dt).getCalendarDate();
                int day = cdate.getDayOfMonth();
                int mon= cdate.getFieldValue(CalendarPeriod.Field.Month);
                int year = cdate.getFieldValue(CalendarPeriod.Field.Day.Year);
                int hour = cdate.getFieldValue(CalendarPeriod.Field.Hour);
                int min = cdate.getFieldValue(CalendarPeriod.Field.Minute);
                int sec = cdate.getFieldValue(CalendarPeriod.Field.Second);
                DateTime  dt0 = Util.getGregorianDateTime(zoneInfo, year, mon, day, hour, min, sec);
                timeArray[i]  = new DateTime(dt0);
            }
            cal = Calendar.gregorian;
            times = timeArray;
        }
        Arrays.sort(times);
        double[][] timeValues = new double[1][times.length];
        for (int i = 0; i < times.length; i++) {
            timeValues[0][i] =
                times[i].getValue(CommonUnit.secondsSinceTheEpoch);
        }
        return new CalendarDateTimeSet(
            RealType.Time, timeValues, times.length, (CoordinateSystem) null,
            new Unit[] { CommonUnit.secondsSinceTheEpoch },
            (ErrorEstimate[]) null, false, cal);
    }

    /**
     * Get the calendar associated with this object
     *
     * @return  the calendar
     */
    public Calendar getCalendar() {
        return calendarDate.getCalendar();
    }

    /**
     * Get the CalendarDate associated with this object
     *
     * @return  the calendar
     */
    public CalendarDate getCalendarDate() {
        return calendarDate;
    }

    /**
     * Create a Gridded1DDoubleSet from an array of doubles of seconds
     * since the epoch.
     *
     * @param  times  array of times in seconds since the epoch. Array
     *                cannot be null or only have one entry.
     *
     * @return set representing the array as a Gridded1DDoubleSet
     * @throws VisADException  couldn't create the GriddedDoubleSet
     */
    public static CalendarDateTimeSet makeTimeSet(double[] times)
            throws VisADException {
        Arrays.sort(times);
        double[][] alltimes = new double[1][times.length];
        for (int i = 0; i < times.length; i++) {
            alltimes[0][i] = times[i];
        }
        return new CalendarDateTimeSet(
            RealType.Time, alltimes, times.length, (CoordinateSystem) null,
            new Unit[] { CommonUnit.secondsSinceTheEpoch },
            (ErrorEstimate[]) null, false, null);
    }

    /**
     * Get the list of DateTime objects from the domain of the given grid
     *
     * @param timeSet  time set to check
     *
     * @return list of times or null if no times.
     *
     * @throws VisADException   problem determining this
     */
    public static List<DateTime> getDateTimeList(Set timeSet)
            throws VisADException {
        if ( !(timeSet instanceof SampledSet)) {
            throw new VisADException("timeSet is not a SampledSet");
        }
        List<DateTime> timeList = new ArrayList<DateTime>();
        Calendar       cal      = null;
        if (timeSet instanceof CalendarDateTimeSet) {
            cal = ((CalendarDateTimeSet) timeSet).getCalendar();
        }
        Unit unit = timeSet.getSetUnits()[0];
        if ( !Unit.canConvert(unit, CommonUnit.secondsSinceTheEpoch)) {
            throw new VisADException("Invalid Units for timeSet");
        }
        double[][] values;
        if ( !unit.equals(CommonUnit.secondsSinceTheEpoch)) {
            values = Unit.convertTuple(
                timeSet.getDoubles(), new Unit[] { unit },
                new Unit[] { CommonUnit.secondsSinceTheEpoch }, false);
        } else {
            values = timeSet.getDoubles(false);
        }

        for (int i = 0; i < timeSet.getLength(); i++) {
            timeList.add(new CalendarDateTime(values[0][i], cal));
        }
        return timeList;
    }

    /**
     * Create an array of DateTimes from a Gridded1DSet of times.
     *
     * @param  timeSet   Gridded1DSet of times
     *
     * @return an array of CalendarDateTime's
     *
     * @throws VisADException  invalid time set or couldn't create DateTimes
     */
    public static CalendarDateTime[] timeSetToArray(Set timeSet)
            throws VisADException {
        List<DateTime> timeList = getDateTimeList(timeSet);
        if (timeList.isEmpty()) {
            return new CalendarDateTime[] {};
        }
        CalendarDateTime[] times = new CalendarDateTime[timeList.size()];

        for (int i = 0; i < timeList.size(); i++) {
            times[i] = (CalendarDateTime) timeList.get(i);
        }
        return times;
    }

    /**
     *  Implement Comparable interface
     *
     * @param   oo      Object for comparison - should be DateTime
     *
     * @return  the comparison
     */
    public int compareTo(Object oo) {
        if (oo instanceof CalendarDateTime) {
            return calendarDate.compareTo(
                ((CalendarDateTime) oo).calendarDate);
        }
        return super.compareTo(oo);
    }

    /**
     * Check if this equals the other object
     *
     * @param oo  the other object
     *
     * @return  true if equal
     */
    public boolean equals(Object oo) {
        if (oo instanceof CalendarDateTime) {
            return calendarDate.equals(((CalendarDateTime) oo).calendarDate);
        }
        return super.equals(oo);
    }

    /**
     * Get the hashcode for this object
     *
     * @return  the hashcode
     */
    public int hashCode() {
        return calendarDate.hashCode();
    }

}
