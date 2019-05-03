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

package ucar.unidata.data;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.visad.UtcDate;

import ucar.visad.data.CalendarDateTime;
import visad.DateTime;
import visad.VisADException;


import java.util.*;


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
     * The time driver time indicator
     */
    public static final String TIME_DRIVER_TIMES = "%timedriver%";

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

    /** Property for the absolute times */
    public static String ABSOLUTE_TIMES = "absolute_times";

    /** Property for miscellaneous keywords */
    public final static String MISC_KEYWORDS = "misckeywords";

    /**
     * Generate a list of URLs, expanding any time macros
     *
     * @param datasource   DataSource that holds relevant properties
     * @param url  original source URL
     * @param subset  the data selection with time subset info
     *
     * @return List of expanded time urls
     */
    public static List<String> generateTimeUrls(DataSourceImpl datasource,
            String url, DataSelection subset) {
        List<String>   urls        = new ArrayList<String>();
        List<DateTime> driverTimes = null;
        List           subTimes    = null;
        List<DateTime> absTimes    = null;

        // figure out the time indices
        int[] timeIndices;
        Object tmp = datasource.getProperty(NUM_RELATIVE_TIMES,
                                            new Integer(0));
        if (tmp instanceof Integer) {
            int numTimes = ((Integer) tmp).intValue();
            timeIndices = new int[numTimes];
            for (int i = 0; i < numTimes; i++) {
                timeIndices[i] = i;
            }
        } else {
            timeIndices = (int[]) tmp;
        }
        // get the driver times or list of subset times
        if (subset != null) {
            driverTimes = subset.getTimeDriverTimes();
            subTimes    = subset.getTimes();
        }
        if (driverTimes == null) {                            // have abs or relative times
            List allTimes = datasource.getAllDateTimes();
            if ((allTimes != null) && !allTimes.isEmpty()) {  // have times
                Object timeObj = allTimes.get(0);
                if ((subTimes != null) && !subTimes.isEmpty()) {
                    timeIndices = new int[subTimes.size()];
                    for (int i = 0; i < subTimes.size(); i++) {
                        timeIndices[i] =
                            ((Integer) subTimes.get(i)).intValue();
                    }
                    if (timeObj instanceof DateTime) {
                        absTimes = new ArrayList<DateTime>();
                        for (int i = 0; i < timeIndices.length; i++) {
                            absTimes.add(
                                (DateTime) allTimes.get(timeIndices[i]));
                        }
                    }
                } else {
                    if (timeObj instanceof DateTime) {
                        absTimes = allTimes;
                    }
                }
            }
        }
        java.util.Hashtable  htable = datasource.getProperties();
        String checkglm = "";
        if(htable != null)
            checkglm = (String) datasource.getProperties()
                        .get(ucar.unidata.idv.chooser.adde.AddeChooser
                                .DATA_NAME_KEY);
        if (driverTimes != null) {
            if(checkglm.contains("GLM Lightning Data")) {
                urls.addAll(makeGLMDriverTimesUrls(url, datasource,
                        (List) driverTimes));
            } else {
                urls.addAll(makeDriverTimesUrls(url, datasource,
                        (List) driverTimes));
            }
        } else if (absTimes != null) {
            if(checkglm.contains("GLM Lightning Data")) {
                float timeInc =
                        ((Number) datasource.getProperty(RELATIVE_TIME_INCREMENT,
                                new Float(1))).floatValue();
                urls.addAll(makeAbsoluteTimesUrlsGLM(url, datasource, absTimes, timeInc));
            } else {
                urls.addAll(makeAbsoluteTimesUrls(url, datasource, absTimes));
            }
        } else if (url.indexOf(RELATIVE_TIME) >= 0 ) {
           if(checkglm.contains("GLM Lightning Data")){
               float timeInc =
                       ((Number) datasource.getProperty(RELATIVE_TIME_INCREMENT,
                               new Float(1))).floatValue();
               url = url.replaceAll("MAX=99999", "MAX=999999");
               url = url.replaceAll("POS=1", "POS=ALL");
               String[] times = makeRelativeTimesGLM(timeIndices, timeInc);
               for (int i = 0; i < times.length; i++) {
                   String newUrl = url.replaceAll(RELATIVE_TIME, times[i]);
                   //   System.err.println ("url:" + newUrl);
                   urls.add(newUrl);
               }
           } else {
               float timeInc =
                       ((Number) datasource.getProperty(RELATIVE_TIME_INCREMENT,
                               new Float(1))).floatValue();

               String[] times = makeRelativeTimes(timeIndices, timeInc);
               for (int i = 0; i < times.length; i++) {
                   String newUrl = url.replaceAll(RELATIVE_TIME, times[i]);
                   //   System.err.println ("url:" + newUrl);
                   urls.add(newUrl);
               }
           }

        } else {
            urls.add(url);
        }
        return urls;
    }

    /**
     * Make URLs that correspond to the absolute list of (timedriver) times
     *
     * @param url   the ADDE URL
     * @param datasource the datasource
     * @param absTimes    the list of absolute times
     *
     * @return the list of URLs
     */
    private static List<String> makeAbsoluteTimesUrls(String url,
            DataSourceImpl datasource, List<DateTime> absTimes) {
        List<String> urls = new ArrayList<String>();
        if ((absTimes == null) || absTimes.isEmpty()) {
            urls.add(url);
        }
        Collections.sort(absTimes);
        String        day   = null;
        String        time  = null;
        StringBuilder hours = new StringBuilder();
        for (DateTime dt : absTimes) {
            String date = UtcDate.getIYD(dt);
            if (day == null) {
                day = date;
            } else if ( !Misc.equals(day, date)) {
                String dayTime = makeDateUrl(day, hours.toString());
                urls.add(replaceDateTime(url, dayTime));
                day   = date;
                hours = new StringBuilder();
            }
            String newTime = UtcDate.getHMS(dt);
            if ( !Misc.equals(newTime, time)) {
                hours.append(UtcDate.getHMS(dt));
                hours.append(",");
                time = newTime;
            }
        }
        String dayTime = makeDateUrl(day, hours.toString());
        urls.add(replaceDateTime(url, dayTime));
        return urls;
    }

    /**
     * Make URLs that correspond to the absolute list of (timedriver) times
     *
     * @param url   the ADDE URL
     * @param datasource the datasource
     * @param absTimes    the list of absolute times
     * @param timInc    in hour
     *
     * @return the list of URLs
     */
    private static List<String> makeAbsoluteTimesUrlsGLM(String url,
                                  DataSourceImpl datasource, List<DateTime> absTimes, float timInc) {
        List<String> urls = new ArrayList<String>();
        if ((absTimes == null) || absTimes.isEmpty()) {
            urls.add(url);
        }
        timInc = timInc * 60.0f;
        Collections.sort(absTimes);

        GregorianCalendar utcCalendar =
                new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        url = url.replaceAll("MAX=99999", "MAX=999999");
        url = url.replaceAll("POS=1", "POS=ALL");
        for (DateTime dt : absTimes) {
            try {
                CalendarDateTime cdt = new CalendarDateTime(dt);
                utcCalendar.setTime(cdt.getCalendarDate().toDate());
            } catch (Exception e){}

            int    newMin   = utcCalendar.get(utcCalendar.MINUTE);
            int    newHour  = utcCalendar.get(utcCalendar.HOUR_OF_DAY);
            int    newDay   = utcCalendar.get(utcCalendar.DAY_OF_YEAR);
            int    newYear  = utcCalendar.get(utcCalendar.YEAR);
            String thisDate = "" + newYear + StringUtil.padZero(newDay, 3);

            utcCalendar.add(utcCalendar.MINUTE,  -(int)timInc);
            int    newMin0   = utcCalendar.get(utcCalendar.MINUTE);
            int    newHour0  = utcCalendar.get(utcCalendar.HOUR_OF_DAY);
            int    newDay0   = utcCalendar.get(utcCalendar.DAY_OF_YEAR);
            int    newYear0  = utcCalendar.get(utcCalendar.YEAR);
            String thisDate0 = "" + newYear0 + StringUtil.padZero(newDay0, 3);
            //Do we have a new day

            String times       = "";

            times = times + newHour0 + StringUtil.padZero(newMin0, 2) + "00" +
                    " " + newHour + StringUtil.padZero(newMin, 2) + "00";
            LogUtil.consoleMessage(times);
            //System.out.println(times);
            String daytime = makeDateUrl(thisDate0, thisDate, times);
            urls.add(replaceDateTime(url, daytime));

        }

        return urls;
    }
    /**
     * Replace the date/time in the url with the new one
     * @param url  the url to munge
     * @param newTime  the new time string
     *
     * @return the url with the date replaced (or not)
     */
    private static String replaceDateTime(String url, String newTime) {
        if (url.indexOf(RELATIVE_TIME) >= 0) {
            url = url.replaceAll(RELATIVE_TIME, newTime);
        } else {
            url = url.replaceAll("time [^;]*;", newTime + ";");
        }
        //System.err.println("url:" + url);
        return url;
    }

    /**
     * Make URLs that correspond to the absolute list of (timedriver) times
     *
     * @param url   the ADDE URL
     * @param datasource the datasource
     * @param driverTimes the list of times
     *
     * @return the list of URLs
     */
    private static List<String> makeDriverTimesUrls(String url,
            DataSourceImpl datasource, List<DateTime> driverTimes) {
        List<String> urls = new ArrayList<String>();
        if ((driverTimes == null) || driverTimes.isEmpty()) {
            urls.add(url);
        }
        Collections.sort(driverTimes);
        String day  = null;
        String time = null;
        // Loop through all the days and create
        // a list of potential times based on the interval.  Then pass
        // that to DataUtil.selectTimesFromList for consistency
        // Get a list of unique dates
        SortedSet<String> uniqueDays =
            Collections.synchronizedSortedSet(new TreeSet<String>());
        for (DateTime dt : driverTimes) {
            String theDay = UtcDate.getYMD(dt);
            uniqueDays.add(theDay);
        }
        // now build a list of possible times
        float timeInc =
            ((Number) datasource.getProperty(RELATIVE_TIME_INCREMENT,
                                             new Float(1))).floatValue();
        int            numTimes = (int) (24 / timeInc);
        List<DateTime> alltimes = new ArrayList<DateTime>();
        for (String today : uniqueDays) {
            for (int i = 0; i < numTimes; i++) {
                float hours   = i * timeInc;
                int   hour    = (int) hours;
                int   minutes = (int) ((hours - hour) * 60);
                String dateString = today + " " + StringUtil.padZero(hour, 2)
                                    + ":" + StringUtil.padZero(minutes, 2)
                                    + ":00";
                try {
                    DateTime dt = UtcDate.createDateTime(dateString,
                                      "yyyy-MM-dd HH:mm:ss");
                    alltimes.add(dt);
                } catch (VisADException ve) {
                    System.err.println("Unable to parse date string: "
                                       + dateString);
                }
            }
        }
        try {
            List<DateTime> matches = DataUtil.selectTimesFromList(alltimes,
                                         driverTimes);
            urls.addAll(makeAbsoluteTimesUrls(url, datasource, matches));
        } catch (Exception e) {}
        return urls;
    }

    /**
     * Make URLs that correspond to the absolute list of (timedriver) times
     *
     * @param url   the ADDE URL
     * @param datasource the datasource
     * @param driverTimes the list of times
     *
     * @return the list of URLs
     */
    private static List<String> makeGLMDriverTimesUrls(String url,
                    DataSourceImpl datasource, List<DateTime> driverTimes) {
        List<String> urls = new ArrayList<String>();
        if ((driverTimes == null) || driverTimes.isEmpty()) {
            urls.add(url);
        }
        Collections.sort(driverTimes);
        String day  = null;
        String time = null;
        // Loop through all the days and create
        // a list of potential times based on the interval.  Then pass
        // that to DataUtil.selectTimesFromList for consistency
        // Get a list of unique dates
        SortedSet<String> uniqueDays =
                Collections.synchronizedSortedSet(new TreeSet<String>());
        for (DateTime dt : driverTimes) {
            String theDay = UtcDate.getYMD(dt);
            uniqueDays.add(theDay);
        }
        // now build a list of possible times
        float timeInc =
                ((Number) datasource.getProperty(RELATIVE_TIME_INCREMENT,
                        new Float(1))).floatValue();
        int            numTimes = (int) (24 / timeInc);
        List<DateTime> alltimes = new ArrayList<DateTime>();
        for (String today : uniqueDays) {
            for (int i = 0; i < numTimes; i++) {
                float hours   = i * timeInc;
                int   hour    = (int) hours;
                int   minutes = (int) ((hours - hour) * 60);
                String dateString = today + " " + StringUtil.padZero(hour, 2)
                        + ":" + StringUtil.padZero(minutes, 2)
                        + ":00";
                try {
                    DateTime dt = UtcDate.createDateTime(dateString,
                            "yyyy-MM-dd HH:mm:ss");
                    alltimes.add(dt);
                } catch (VisADException ve) {
                    System.err.println("Unable to parse date string: "
                            + dateString);
                }
            }
        }
        try {
            List<DateTime> matches = DataUtil.selectTimesFromList(alltimes,
                    driverTimes);
           // System.out.println( driverTimes.toString());
           // System.out.println( matches.toString());
            urls.addAll(makeAbsoluteTimesUrlsGLM(url, datasource, matches, timeInc));
        } catch (Exception e) {}
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
     * Make the relative times
     *
     * @param timeIndices array of time indices
     * @param timeInc    time increment (hours)
     *
     * @return ADDE time select clause
     */
    public static String[] makeRelativeTimesGLM(int[] timeIndices,
                                             float timeInc) {

        GregorianCalendar utcCalendar =
                new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        String currentDate = null;
        String times       = null;
        List   timesList   = new ArrayList();

        Date   now         = new Date();
        int    minInc      = (int) ((timeInc) * 60);
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
       // utcCalendar.add(utcCalendar.MINUTE,  - minInc);
        for (int i = 0; i < timeIndices.length; i++)
        {
            //Go back timeIndices*increment hours
            //utcCalendar.add(utcCalendar.HOUR_OF_DAY,
            //                -timeIndices[i] * hourInc);
            utcCalendar.add(utcCalendar.MINUTE, -timeIndices[i] * minInc);
            int    newMin   = utcCalendar.get(utcCalendar.MINUTE);
            int    newHour  = utcCalendar.get(utcCalendar.HOUR_OF_DAY);
            int    newDay   = utcCalendar.get(utcCalendar.DAY_OF_YEAR);
            int    newYear  = utcCalendar.get(utcCalendar.YEAR);
            String thisDate = "" + newYear + StringUtil.padZero(newDay, 3);

            utcCalendar.add(utcCalendar.MINUTE,  - minInc);
            int    newMin0   = utcCalendar.get(utcCalendar.MINUTE);
            int    newHour0  = utcCalendar.get(utcCalendar.HOUR_OF_DAY);
            int    newDay0   = utcCalendar.get(utcCalendar.DAY_OF_YEAR);
            int    newYear0  = utcCalendar.get(utcCalendar.YEAR);
            String thisDate0 = "" + newYear0 + StringUtil.padZero(newDay0, 3);
            //Do we have a new day

            times       = "";

            //Append the time
            if (times.length() > 0) {
                times = times + ",";
            }
            times = times + newHour0 + StringUtil.padZero(newMin0, 2) + "00" +
                    " " + newHour + StringUtil.padZero(newMin, 2) + "00";
            System.out.println(times);
            LogUtil.consoleMessage(times);
            timesList.add(makeDateUrl(thisDate0, thisDate, times));
            utcCalendar.add(utcCalendar.MINUTE,  minInc + timeIndices[i] * minInc);
        }

        //add the last one to the list
       // if (currentDate != null) {
        //    timesList.add(makeDateUrl(currentDate, times));
       // }

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
     * Assemble the dttm part of the url
     *
     * @param day the day
     * @param times list of times
     *
     * @return the dttm part of the url
     */
    private static String makeDateUrl(String day, String day1, String times) {
        StringBuffer sb = new StringBuffer();
        sb.append("DAY ");
        sb.append(day);
        sb.append(" ");
        sb.append(day1);
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
