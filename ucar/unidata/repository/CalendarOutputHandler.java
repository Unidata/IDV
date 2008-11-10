/**
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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

package ucar.unidata.repository;


import org.w3c.dom.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;


import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CalendarOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_GRID = new OutputType("Date Grid","calendar.grid");

    /** _more_ */
    public static final OutputType OUTPUT_CALENDAR = new OutputType("Calendar","calendar.calendar");


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public CalendarOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_GRID);
        addType(OUTPUT_CALENDAR);
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param state _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void addOutputTypes(Request request, State state,
                                  List<OutputType> types)
            throws Exception {
        if (state.entry != null) {
            return;
        }
        types.add(OUTPUT_CALENDAR);
        types.add(OUTPUT_GRID);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        OutputType       output = request.getOutput();
        StringBuffer sb     = new StringBuffer();
        showNext(request, subGroups, entries, sb);
        entries.addAll(subGroups);
        Result result;
        sb.append(getRepository().makeEntryHeader(request, group));
        if (output.equals(OUTPUT_GRID)) {
            result = outputDateGrid(request, group, entries, sb);
        } else {
            result = outputCalendar(request, group, entries, sb);
        }
        addLinks(request, result, new State(group, subGroups, entries));
        return result;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputDateGrid(Request request, Group group,
                                  List<Entry> entries, StringBuffer sb)
            throws Exception {
        String           title    = group.getFullName();
        List             types    = new ArrayList();
        List             days     = new ArrayList();
        Hashtable        dayMap   = new Hashtable();
        Hashtable        typeMap  = new Hashtable();
        Hashtable        contents = new Hashtable();

        SimpleDateFormat sdf      = new SimpleDateFormat();
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern("yyyy/MM/dd");
        SimpleDateFormat timeSdf = new SimpleDateFormat();
        timeSdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        timeSdf.applyPattern("HH:mm");
        SimpleDateFormat monthSdf = new SimpleDateFormat();
        monthSdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        monthSdf.applyPattern("MM");
        StringBuffer header = new StringBuffer();
        header.append(HtmlUtil.cols(HtmlUtil.bold(msg("Date"))));
        for (Entry entry : entries) {
            String type = entry.getTypeHandler().getType();
            String day  = sdf.format(new Date(entry.getStartDate()));
            if (typeMap.get(type) == null) {
                types.add(entry.getTypeHandler());
                typeMap.put(type, type);
                header.append(
                    "<td>" + HtmlUtil.bold(entry.getTypeHandler().getLabel())
                    + "</td>");
            }
            if (dayMap.get(day) == null) {
                days.add(new Date(entry.getStartDate()));
                dayMap.put(day, day);
            }
            String       time =
                timeSdf.format(new Date(entry.getStartDate()));
            String       key   = type + "_" + day;
            StringBuffer colSB = (StringBuffer) contents.get(key);
            if (colSB == null) {
                colSB = new StringBuffer();
                contents.put(key, colSB);
            }
            colSB.append(getRepository().getAjaxLink(request, entry, time, true));
            colSB.append(HtmlUtil.br());
        }

        sb.append(
            "<table border=\"1\" cellspacing=\"0\" cellpadding=\"3\" width=\"100%\">");
        days = Misc.sort(days);
        String currentMonth = "";
        for (int dayIdx = 0; dayIdx < days.size(); dayIdx++) {
            Date   date  = (Date) days.get(dayIdx);
            String month = monthSdf.format(date);
            //Put the header in every month
            if ( !currentMonth.equals(month)) {
                currentMonth = month;
                sb.append("<tr class=\"calheader\">" + header + "</tr>");
            }

            String day = sdf.format(date);
            sb.append("<tr valign=\"top\">");
            sb.append("<td width=\"5%\">" + day + "</td>");
            for (int i = 0; i < types.size(); i++) {
                TypeHandler  typeHandler = (TypeHandler) types.get(i);
                String       type        = typeHandler.getType();
                String       key         = type + "_" + day;
                StringBuffer cb          = (StringBuffer) contents.get(key);
                if (cb == null) {
                    sb.append("<td>" + HtmlUtil.space(1) + "</td>");
                } else {
                    sb.append(
                        "<td><div style=\"max-height: 150px; overflow-y: auto;\">"
                        + cb + "</div></td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append("</table>");

        Result result = new Result(title, sb);
        return result;

    }

    /**
     * _more_
     *
     * @param day _more_
     *
     * @return _more_
     */
    public static  GregorianCalendar getCalendar(int[] day) {
        return getCalendar(day[IDX_DAY], day[IDX_MONTH], day[IDX_YEAR]);
    }

    /**
     * _more_
     *
     * @param day _more_
     * @param month _more_
     * @param year _more_
     *
     * @return _more_
     */
    public static  GregorianCalendar getCalendar(int day, int month, int year) {
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        cal.set(cal.DAY_OF_MONTH, day);
        cal.set(cal.MONTH, month);
        cal.set(cal.YEAR, year);
        return cal;
    }

    /**
     * _more_
     *
     * @param cal _more_
     *
     * @return _more_
     */
    public static  int[] getDayMonthYear(GregorianCalendar cal) {
        return new int[] { cal.get(cal.DAY_OF_MONTH), cal.get(cal.MONTH),
                           cal.get(cal.YEAR) };
    }

    /**
     * _more_
     *
     * @param cal _more_
     * @param what _more_
     * @param delta _more_
     *
     * @return _more_
     */
    private GregorianCalendar add(GregorianCalendar cal, int what,
                                  int delta) {
        cal.add(what, delta);
        return cal;
    }


    /** _more_ */
    private static final int IDX_DAY = 0;

    /** _more_ */
    private static final int IDX_MONTH = 1;

    /** _more_ */
    private static final int IDX_YEAR = 2;


    public static String getUrlArgs(GregorianCalendar cal) {
        return getUrlArgs(getDayMonthYear(cal));
    }


    public static String getUrlArgs(int[]dayMonthYear) {
        return ARG_YEAR +"=" + dayMonthYear[IDX_YEAR] +"&" +
            ARG_MONTH +"=" + dayMonthYear[IDX_MONTH] +"&" +
            ARG_DAY +"=" + dayMonthYear[IDX_DAY];
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param entries _more_
     * @param sb _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result outputCalendar(Request request, Group group,
                                  List<Entry> entries, StringBuffer sb)
            throws Exception {

        if (entries.size() == 0) {
            sb.append(getRepository().note(msg("No entries found")));
        }
        boolean hadDateArgs = request.defined(ARG_YEAR)
                              || request.defined(ARG_MONTH)
                              || request.defined(ARG_DAY);

        int[] today =
            getDayMonthYear(new GregorianCalendar(DateUtil.TIMEZONE_GMT));

        int[] selected = new int[] { request.get(ARG_DAY, today[IDX_DAY]),
                                     request.get(ARG_MONTH, today[IDX_MONTH]),
                                     request.get(ARG_YEAR, today[IDX_YEAR]) };

        boolean doDay = request.defined(ARG_DAY);

        int[]   prev  = (doDay
                         ? getDayMonthYear(add(getCalendar(selected),
                             Calendar.DAY_OF_MONTH, -1))
                         : getDayMonthYear(add(getCalendar(selected),
                             Calendar.MONTH, -1)));
        int[] next = (doDay
                      ? getDayMonthYear(add(getCalendar(selected),
                                            Calendar.DAY_OF_MONTH, 1))
                      : getDayMonthYear(add(getCalendar(selected),
                                            Calendar.MONTH, 1)));

        int[] prevprev = (doDay
                          ? getDayMonthYear(add(getCalendar(selected),
                              Calendar.MONTH, -1))
                          : getDayMonthYear(add(getCalendar(selected),
                              Calendar.YEAR, -1)));
        int[] nextnext = (doDay
                          ? getDayMonthYear(add(getCalendar(selected),
                              Calendar.MONTH, 1))
                          : getDayMonthYear(add(getCalendar(selected),
                              Calendar.YEAR, 1)));

        int[]                   someDate = null;


        List                    dayItems = null;
        Hashtable dates      = new Hashtable();
        Hashtable<String, List> map      = new Hashtable<String, List>();
        GregorianCalendar mapCal =
            new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        boolean didone = false;
        for (int tries = 0; tries < 2; tries++) {
            dayItems = new ArrayList();
            for (Entry entry : entries) {
                Date entryDate = new Date(entry.getStartDate());
                mapCal.setTime(entryDate);
                int[] entryDay = getDayMonthYear(mapCal);
                String key = entryDay[IDX_YEAR] + "/" + entryDay[IDX_MONTH]
                             + "/" + entryDay[IDX_DAY];
                if(tries==0) {
                    dates.put(key,key);
                }
                if (someDate == null) {
                    someDate = entryDay;
                }
                if (doDay) {
                    if ((entryDay[IDX_DAY] != selected[IDX_DAY])
                            || (entryDay[IDX_MONTH] != selected[IDX_MONTH])
                            || (entryDay[IDX_YEAR] != selected[IDX_YEAR])) {
                        continue;
                    }
                } else {
                    if ( !((entryDay[IDX_YEAR] == selected[IDX_YEAR])
                            && (entryDay[IDX_MONTH]
                                == selected[IDX_MONTH]))) {
                        continue;
                    }
                }

                List dayList = map.get(key);
                if (dayList == null) {
                    map.put(key, dayList = new ArrayList());
                }
                String label = entry.getLabel();
                if (doDay) {
                    dayItems.add(entry);
                } else {
                    if (label.length() > 20) {
                        label = label.substring(0, 19) + "...";
                    }
                    dayList.add(HtmlUtil.nobr(getRepository().getAjaxLink(request, entry,
                            label, true)));
                }
                didone = true;
            }


            if (didone || hadDateArgs) {
                break;
            }
            if (someDate != null) {
                selected = someDate;
                prev     = (doDay
                            ? getDayMonthYear(add(getCalendar(selected),
                            Calendar.DAY_OF_MONTH, -1))
                            : getDayMonthYear(add(getCalendar(selected),
                            Calendar.MONTH, -1)));
                next = (doDay
                        ? getDayMonthYear(add(getCalendar(selected),
                        Calendar.DAY_OF_MONTH, 1))
                        : getDayMonthYear(add(getCalendar(selected),
                        Calendar.MONTH, 1)));
                prevprev = (doDay
                            ? getDayMonthYear(add(getCalendar(selected),
                            Calendar.MONTH, -1))
                            : getDayMonthYear(add(getCalendar(selected),
                            Calendar.YEAR, -1)));
                nextnext = (doDay
                            ? getDayMonthYear(add(getCalendar(selected),
                            Calendar.MONTH, 1))
                            : getDayMonthYear(add(getCalendar(selected),
                            Calendar.YEAR, 1)));
            }
        }




        String[] navIcons = { "/icons/prevprev.gif", "/icons/prev.gif",
                              "/icons/today.gif", "/icons/next.gif",
                              "/icons/nextnext.gif" };


        String[]          navLabels;
        SimpleDateFormat  headerSdf;
        GregorianCalendar cal;
        List<String>      navUrls = new ArrayList<String>();


        if (doDay) {
            headerSdf = new SimpleDateFormat("MMMMM, dd yyyy");
            navLabels = new String[] { "Previous Month", "Previous Day",
                                       "Today", "Next Day", "Next Month" };
            cal = getCalendar(selected);
        } else {
            headerSdf = new SimpleDateFormat("MMMMM yyyy");
            navLabels = new String[] { "Last Year", "Last Month",
                                       "Current Month", "Next Month",
                                       "Next Year" };
            cal = getCalendar(1, selected[IDX_MONTH], selected[IDX_YEAR]);
        }


        request.put(ARG_YEAR, "" + (prevprev[IDX_YEAR]));
        request.put(ARG_MONTH, "" + (prevprev[IDX_MONTH]));
        if (doDay) {
            request.put(ARG_DAY, "" + (prevprev[IDX_DAY]));
        }
        navUrls.add(request.getUrl());


        request.put(ARG_YEAR, "" + (prev[IDX_YEAR]));
        request.put(ARG_MONTH, "" + (prev[IDX_MONTH]));
        if (doDay) {
            request.put(ARG_DAY, "" + (prev[IDX_DAY]));
        }
        navUrls.add(request.getUrl());


        request.put(ARG_YEAR, "" + (today[IDX_YEAR]));
        request.put(ARG_MONTH, "" + (today[IDX_MONTH]));
        if (doDay) {
            request.put(ARG_DAY, "" + (today[IDX_DAY]));
        }
        navUrls.add(request.getUrl());


        request.put(ARG_YEAR, "" + next[IDX_YEAR]);
        request.put(ARG_MONTH, "" + next[IDX_MONTH]);
        if (doDay) {
            request.put(ARG_DAY, "" + (next[IDX_DAY]));
        }
        navUrls.add(request.getUrl());

        request.put(ARG_YEAR, "" + (nextnext[IDX_YEAR]));
        request.put(ARG_MONTH, "" + (nextnext[IDX_MONTH]));
        if (doDay) {
            request.put(ARG_DAY, "" + (nextnext[IDX_DAY]));
        }
        navUrls.add(request.getUrl());


        request.remove(ARG_DAY);
        request.remove(ARG_MONTH);
        request.remove(ARG_YEAR);


        List navList = new ArrayList();

        for (int i = 0; i < navLabels.length; i++) {
            navList.add(
                HtmlUtil.href(
                    navUrls.get(i),
                    HtmlUtil.img(
                        getRepository().fileUrl(navIcons[i]), navLabels[i],
                        " border=\"0\"")));
        }

        if (doDay) {
            StringBuffer tmp = new StringBuffer();
            String link = getEntryHtml(tmp, dayItems, request, true, false,
                                       false);

            request.remove(ARG_MONTH);
            request.remove(ARG_YEAR);
            request.remove(ARG_DAY);
            sb.append(HtmlUtil.p());
            sb.append("<table cellpadding=10><tr valign=top><td>");
            getRepository().createMonthNav(sb, cal.getTime(),request.getUrl(),dates);
            sb.append("</td><td>");
            request.put(ARG_MONTH, "" + selected[IDX_MONTH]);
            request.put(ARG_YEAR, "" + selected[IDX_YEAR]);
            String monthUrl = request.getUrl();
            request.put(ARG_DAY,selected[IDX_DAY]);
            //            sb.append(HtmlUtil.b(StringUtil.join(HtmlUtil.space(1),
            //                    navList)));
            sb.append(
                HtmlUtil.href(
                    monthUrl, HtmlUtil.b(headerSdf.format(cal.getTime()))));
            if (dayItems.size() == 0) {
                sb.append("<p>No Entries");
            } else {
                sb.append(link);
                sb.append(tmp);
            }
            sb.append("</table>");
        } else {
            sb.append(
                HtmlUtil.center(
                    HtmlUtil.b(StringUtil.join(HtmlUtil.space(1), navList))));
            sb.append(
                HtmlUtil.center(HtmlUtil.b(headerSdf.format(cal.getTime()))));
            sb.append(
                "<table border=\"1\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%\">");
            String[] dayNames = {
                "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
            };
            sb.append("<tr>");
            for (int colIdx = 0; colIdx < 7; colIdx++) {
                sb.append("<td width=\"14%\" class=\"calheader\">"
                          + dayNames[colIdx] + "</td>");
            }
            sb.append("</tr>");
            int startDow = cal.get(cal.DAY_OF_WEEK);
            while (startDow > 1) {
                cal.add(cal.DAY_OF_MONTH, -1);
                startDow--;
            }
            for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
                sb.append("<tr valign=top>");
                for (int colIdx = 0; colIdx < 7; colIdx++) {
                    String content   = HtmlUtil.space(1);
                    String bg        = "";
                    int    thisDay   = cal.get(cal.DAY_OF_MONTH);
                    int    thisMonth = cal.get(cal.MONTH);
                    int    thisYear  = cal.get(cal.YEAR);
                    String key = thisYear + "/" + thisMonth + "/" + thisDay;
                    List   inner     = map.get(key);
                    request.put(ARG_MONTH, "" + thisMonth);
                    request.put(ARG_YEAR, "" + thisYear);
                    request.put(ARG_DAY, "" + thisDay);
                    String dayUrl = request.getUrl();
                    if (thisMonth != selected[IDX_MONTH]) {
                        bg = " style=\"background-color:lightgray;\"";
                    } else if ((today[IDX_DAY] == thisDay)
                               && (today[IDX_MONTH] == thisMonth)
                               && (today[IDX_YEAR] == thisYear)) {
                        bg = " style=\"background-color:lightblue;\"";
                    }
                    String dayContents = "&nbsp;";
                    if (inner != null) {
                        dayContents = "<div class=\"calcontents\">"
                                      + StringUtil.join("<br>", inner)
                                      + "</div>";

                    }
                    content =
                        "<table border=0 cellspacing=\"0\" cellpadding=\"2\" width=100%><tr valign=top><td>"
                        + dayContents + "</td><td align=right class=calday>"
                        + HtmlUtil.href(dayUrl, "" + thisDay)
                        + "<br>&nbsp;</td></tr></table>";
                    sb.append("<td class=\"calentry\" " + bg + " >" + content
                              + "</td>");
                    cal.add(cal.DAY_OF_MONTH, 1);
                }
                if ((cal.get(cal.YEAR) >= selected[IDX_YEAR])
                        && (cal.get(cal.MONTH) > selected[IDX_MONTH])) {
                    break;
                }
            }

            sb.append("</table>");
        }

        request.remove(ARG_DAY);
        request.remove(ARG_MONTH);
        request.remove(ARG_YEAR);

        Result result = new Result(group.getFullName(), sb);
        return result;

    }





}

