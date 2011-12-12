/*
 * $Id: AddeUtil.java,v 1.5 2007/05/26 13:31:06 dmurray Exp $
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

package ucar.unidata.data;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.Calendar;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;


/**
 * A class for holding some Adde related constants and static methods
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.5 $
 */
public final class AddeUtil {

    /**
     * The relative time indicator
     */
    public static final String RELATIVE_TIME = "%relative%";

    /**
     * The relative time range indicator
     */
    public static final String RELATIVE_TIME_RANGE = "%relative_range%";

    /**
     * The latlon bounding box time indicator
     */
    public static final String LATLON_BOX = "%latlonbox%";

    /**
     * The level indicator
     */
    public static final String LEVEL = "%level%";

    /** Property for the number of relative times */
    public static String NUM_RELATIVE_TIMES = "number of relative times";

    /** Property for the time increment */
    public static String RELATIVE_TIME_INCREMENT = "relative time increment";

    /** Property for miscellaneous keywords */
    public final static String MISC_KEYWORDS = "misckeywords";

    /**
     * Generate a list of URLs, expanding any time macros
     *
     * @param datasource   DataSource that holds relevant properties
     * @param url  original source URL
     *
     * @return List of expanded time urls
     */
    public static List generateTimeUrls(DataSourceImpl datasource,
                                        String url) {
        List urls = new ArrayList();
        if (url.indexOf(RELATIVE_TIME) >= 0) {
            Object tmp = datasource.getProperty(NUM_RELATIVE_TIMES,
                             new Integer(0));
            float timeInc =
                ((Number) datasource.getProperty(RELATIVE_TIME_INCREMENT,
                    new Float(1))).floatValue();

            int[] timeIndices;
            if (tmp instanceof Integer) {
                int numTimes = ((Integer) tmp).intValue();
                timeIndices = new int[numTimes];
                for (int i = 0; i < numTimes; i++) {
                    timeIndices[i] = i;
                }
            } else {
                timeIndices = (int[]) tmp;
            }
            String[] times = makeRelativeTimes(timeIndices, timeInc);
            for (int i = 0; i < times.length; i++) {
                String newUrl = url.replaceAll(RELATIVE_TIME, times[i]);
                //   System.err.println ("url:" + newUrl);
                urls.add(newUrl);
            }

        } else {
            urls.add(url);
        }
        return urls;
    }

    /**
     * Make the relative times
     *
     * @param timeIndices array of time indices
     * @param timeInc    time increment (hours)
     *
     * @return ADDE time select clause
     */
    public static String[] makeRelativeTimes(int[] timeIndices,
                                             float timeInc) {

        GregorianCalendar utcCalendar =
            new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        String currentDate = null;
        String times       = null;
        List   timesList   = new ArrayList();

        Date   now         = new Date();
        int    minInc      = (int) (60 * timeInc);
        int    minutes     = minInc % 60;
        int    hours       = minInc / 60;
        utcCalendar.setTime(now);
        int curHour = utcCalendar.get(utcCalendar.HOUR_OF_DAY);
        int curMin  = utcCalendar.get(utcCalendar.MINUTE);
        // normalize for time inc
        //        int diff = curHour- (curHour / timeInc) * timeInc;  
        /*
        int diff = curHour % hourInc;
        //        System.err.println("cur hour:" + curHour + " diff:" + diff);
        utcCalendar.add(utcCalendar.HOUR_OF_DAY, -diff);
        */
        int hdiff = (hours == 0)
                    ? 0
                    : curHour % hours;
        int mdiff = (minutes == 0)
                    ? curMin
                    : curMin % minutes;
        // System.err.println("cur hour:" + curHour + " diff:" + hdiff);
        // System.err.println("cur min:" + curMin + " diff:" + mdiff);
        utcCalendar.add(utcCalendar.HOUR_OF_DAY, -hdiff);
        utcCalendar.add(utcCalendar.MINUTE, -mdiff);
        now = utcCalendar.getTime();
        for (int i = 0; i < timeIndices.length; i++) {
            //Reset the date to now.
            utcCalendar.setTime(now);
            //Go back timeIndices*increment hours
            //utcCalendar.add(utcCalendar.HOUR_OF_DAY,
            //                -timeIndices[i] * hourInc);
            utcCalendar.add(utcCalendar.MINUTE, -timeIndices[i] * minInc);
            int    newMin   = utcCalendar.get(utcCalendar.MINUTE);
            int    newHour  = utcCalendar.get(utcCalendar.HOUR_OF_DAY);
            int    newDay   = utcCalendar.get(utcCalendar.DAY_OF_YEAR);
            int    newYear  = utcCalendar.get(utcCalendar.YEAR);
            String thisDate = "" + newYear + StringUtil.padZero(newDay, 3);
            //Do we have a new day
            if ( !Misc.equals(currentDate, thisDate)) {
                if (currentDate != null) {
                    //if not the first time through then add it to the list
                    timesList.add(makeDateUrl(currentDate, times.toString()));
                }
                currentDate = thisDate;
                times       = "";
            }

            //Append the time 
            if (times.length() > 0) {
                times = times + ",";
            }
            times = times + newHour + ":" + StringUtil.padZero(newMin, 2);
        }

        //add the last one to the list
        if (currentDate != null) {
            timesList.add(makeDateUrl(currentDate, times));
        }

        return (String[]) timesList.toArray(new String[timesList.size()]);

    }


    /**
     * Assemble the dttm part of the url
     *
     * @param day the day
     * @param times list of times
     *
     * @return the dttm part of the url
     */
    private static String makeDateUrl(String day, String times) {
        StringBuffer sb = new StringBuffer();
        sb.append("DAY ");
        sb.append(day);
        sb.append(" ");
        sb.append(day);
        sb.append(";TIME ");
        sb.append(times);
        //        System.err.println("time:" + sb);
        return sb.toString();
    }


    /**
     * Main method for testing
     *
     * @param args not used
     */
    public static void main(String[] args) {
        int[] times = {
            0, 5, 8, 12, 16, 20
        };
        times = new int[] { 0, 1, 2 };


        makeRelativeTimes(times, 12);
        for (int i = 1; i < 6; i++) {
            System.err.println("inc:" + i);
            makeRelativeTimes(times, i);
        }
    }

}

