/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

package ucar.visad;


import ucar.unidata.util.StringUtil;

import visad.DateTime;
import visad.Gridded1DSet;
import visad.RealTupleType;
import visad.SetType;
import visad.VisADException;

import java.util.TimeZone;


/**
 * A set of utility functions for UTC DateTimes
 *
 * @author Unidata Development Team
 * @version $Revision: 1.6 $ $Date: 2007/05/22 13:52:35 $
 */
public final class UtcDate {

    /** timestamp macro identifier */
    public static final String MACRO_TIMESTAMP = "%timestamp%";

    /** timestamp macro identifier */
    public static final String TEMPLATE_TIMEFORMAT = "time:";

    /** default constructor */
    public UtcDate() {}

    /** Year-Month-Day format string */
    public static final String YMD_FORMAT = "yyyy-MM-dd";

    /** hour:minute:second format string (HH:mm:ss) */
    public static final String HMS_FORMAT = "HH:mm:ss";

    /** hour:minute format string (HH:mm) */
    public static final String HH_MM_FORMAT = "HH:mm";

    /** hour format string (HH) */
    public static final String HH_FORMAT = "HH";

    /** Year-Day format string */
    public static final String IYD_FORMAT = "yyyyDDD";

    /** Julian day format string */
    public static final String JDAY_FORMAT = "DDD";

    /** GMT Timezone */
    public static final TimeZone GMT = DateTime.DEFAULT_TIMEZONE;

    /** Default time format */
    public static final String DEFAULT_PATTERN = DateTime.DEFAULT_TIME_FORMAT;

    /**
     * Get the hour as a String.
     * @param dt  DateTime to use
     * @return hour as a String (HH)
     */
    public static String getHH(DateTime dt) {
        return getHH(dt, GMT);
    }

    /**
     * Get the hour as a String.
     * @param dt  DateTime to use
     * @param tz  the TimeZone to use
     * @return hour as a String (HH)
     */
    public static String getHH(DateTime dt, TimeZone tz) {
        return formatUtcDate(dt, HH_FORMAT, tz);
    }

    /**
     * See if the string contains a timestamp macro
     *
     * @param s   the  string
     *
     * @return true if it contains the macro
     */
    public static boolean containsTimeMacro(String s) {
        return containsTimeMacro(s, "%");
    }

    /**
     * Does this string contain a time macro
     *
     * @param s   the string
     * @param prefix  the prefix
     *
     * @return  true if it contains a time macro
     */
    public static boolean containsTimeMacro(String s, String prefix) {
        return (s.indexOf(MACRO_TIMESTAMP) >= 0)
               || (s.indexOf(prefix + TEMPLATE_TIMEFORMAT) >= 0);
    }

    /**
     * Apply the timestamp macro to the string
     *
     * @param template the timestamp template
     * @param dttm     the DateTime
     *
     * @return a formatted string of form template
     */
    public static String applyTimeMacro(String template, DateTime dttm) {
        return applyTimeMacro(template, dttm, GMT);
    }

    /**
     * Apply the timestamp macro to the string
     *
     * @param template the timestamp template
     * @param dttm     the DateTime
     * @param tz the TimeZone to use
     *
     * @return a formatted string of form template
     */
    public static String applyTimeMacro(String template, DateTime dttm,
                                        TimeZone tz) {
        return applyTimeMacro(template, dttm, "", "%", "%", tz);
    }

    /**
     * Apply the timestamp macro to the string
     *
     * @param template the timestamp template
     * @param dttm     the DateTime
     * @param noTimeLabel   the label if dttm is null;
     *
     * @return a formatted string of form template
     */
    public static String applyTimeMacro(String template, DateTime dttm,
                                        String noTimeLabel) {

        return applyTimeMacro(template, dttm, noTimeLabel, "%", "%");
    }

    /**
     * Apply the timestamp macro to the string
     *
     * @param template the timestamp template
     * @param dttm     the DateTime
     * @param noTimeLabel   the label if dttm is null;
     * @param prefix  macro prefix
     * @param suffix  macro suffix
     *
     * @return  the String with the macro applied
     */
    public static String applyTimeMacro(String template, DateTime dttm,
                                        String noTimeLabel, String prefix,
                                        String suffix) {
        return applyTimeMacro(template, dttm, noTimeLabel, prefix, suffix,
                              GMT);
    }

    /**
     * Apply the timestamp macro to the string
     *
     * @param template the timestamp template
     * @param dttm     the DateTime
     * @param noTimeLabel   the label if dttm is null;
     * @param prefix  macro prefix
     * @param suffix  macro suffix
     * @param tz      TimeZone
     *
     * @return  the String with the macro applied
     */
    public static String applyTimeMacro(String template, DateTime dttm,
                                        String noTimeLabel, String prefix,
                                        String suffix, TimeZone tz) {
        if (dttm != null) {
            template = StringUtil.replace(template, MACRO_TIMESTAMP,
                                          dttm.toString());
        } else {
            template = StringUtil.replace(template, MACRO_TIMESTAMP,
                                          noTimeLabel);
        }
        prefix = prefix + TEMPLATE_TIMEFORMAT;
        int prefixLength = prefix.length();
        int suffixLength = suffix.length();
        int idx1         = template.indexOf(prefix);
        if (idx1 >= 0) {
            int idx2 = template.indexOf(suffix, idx1 + suffixLength);
            if (idx2 > idx1) {
                String formatString = template.substring(idx1 + prefixLength,
                                          idx2);
                String tmp = ((dttm == null)
                              ? noTimeLabel
                              : formatUtcDate(dttm, formatString, tz));
                template = StringUtil.replace(template,
                        prefix + formatString + suffix, tmp);
            }

        }
        return template;
    }

    /**
     * Get the hour:minute as a String.
     * @param dt  DateTime to use
     * @return hour:minute as a String (HH:mm);
     */
    public static String getHHMM(DateTime dt) {
        return getHHMM(dt, GMT);
    }


    /**
     * Get the hour:minute as a String.
     * @param dt  DateTime to use
     * @param tz  the TimeZone to use
     * @return hour:minute as a String (HH:mm);
     */
    public static String getHHMM(DateTime dt, TimeZone tz) {
        return formatUtcDate(dt, HH_MM_FORMAT, tz);
    }

    /**
     * Get the year-month-day as a String
     * @param dt  DateTime to use
     * @return year-month-day as a String (yyyy-MM-dd);
     */
    public static String getYMD(DateTime dt) {
        return getYMD(dt, GMT);
    }

    /**
     * Get the year-month-day as a String
     * @param dt  DateTime to use
     * @param tz  the TimeZone to use
     * @return year-month-day as a String (yyyy-MM-dd);
     */
    public static String getYMD(DateTime dt, TimeZone tz) {
        return formatUtcDate(dt, YMD_FORMAT, tz);
    }

    /**
     * Get the hour/minute/second as a String
     * @param dt  DateTime to use
     * @return the hour/minute/second as a String (HH:mm:ss)
     */
    public static String getHMS(DateTime dt) {
        return getHMS(dt, GMT);
    }

    /**
     * Get the hour/minute/second as a String
     * @param dt  DateTime to use
     * @param tz  the TimeZone to use
     * @return the hour/minute/second as a String (HH:mm:ss)
     */
    public static String getHMS(DateTime dt, TimeZone tz) {
        return formatUtcDate(dt, HMS_FORMAT);
    }

    /**
     * Get the full datetime using the DEFAULT_PATTERN
     * @param dt  DateTime to use
     * @return the formatted UTC date
     */
    public static String getUtcDate(DateTime dt) {
        return formatUtcDate(dt, DEFAULT_PATTERN, GMT);
    }

    /**
     * Get the year.jday (IYD_FORMAT) as a String
     * @param dt DateTime to use
     * @return year.jday as a String (yyyyDDD);
     */
    public static String getIYD(DateTime dt) {
        return getIYD(dt, GMT);
    }

    /**
     * Get the year.jday (IYD_FORMAT) as a String
     * @param dt DateTime to use
     * @param tz  the TimeZone to use
     * @return year.jday as a String (yyyyDDD);
     */
    public static String getIYD(DateTime dt, TimeZone tz) {
        return formatUtcDate(dt, IYD_FORMAT, tz);
    }

    /**
     * Return a formated date in UTC time. Uses DateTime.formatString()
     * with the UTC (GMT) time zone.
     * @param dt DateTime object
     * @param pattern format pattern
     * @return formatted date.
     */
    public static String formatUtcDate(DateTime dt, String pattern) {
        return formatUtcDate(dt, pattern, GMT);
    }

    /**
     * Return a formated date in UTC time. Uses DateTime.formatString()
     * with the UTC (GMT) time zone.
     * @param dt DateTime object
     * @param pattern format pattern
     * @param tz  the TimeZone to use
     * @return formatted date.
     */
    public static String formatUtcDate(DateTime dt, String pattern,
                                       TimeZone tz) {
        if (dt == null) {
            return "null";
        }
        if (pattern == null) {
            return dt.toString();
        }
        try {
            return dt.formattedString(pattern, tz);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Convert an array of DateTime objects to an array of the Julian day of each date
     * @param dates  array of dates
     * @return the corresponding Julian Days
     */
    public static int[] convertDateTimeToJulianDay(DateTime[] dates) {
        int[] jdays = new int[dates.length];
        for (int i = 0; i < dates.length; i++) {
            jdays[i] = Integer.parseInt(formatUtcDate(dates[i], JDAY_FORMAT,
                    GMT));
        }
        return jdays;
    }

    /**
     * Convert the time set to an array of the Julian days of each date in the set
     *
     * @param timeSet set of dates
     * @return the corresponding Julian Days
     *
     * @throws VisADException set must have type of RealType.Time
     */
    public static int[] convertDateTimeToJulianDay(Gridded1DSet timeSet)
            throws VisADException {
        if ( !((SetType) timeSet.getType()).getDomain().equals(
                RealTupleType.Time1DTuple)) {
            throw new VisADException("Set must have type of RealType.Time");
        }
        DateTime[] dates = DateTime.timeSetToArray(timeSet);
        return convertDateTimeToJulianDay(dates);
    }

}
