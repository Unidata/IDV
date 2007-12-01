/**
 * $Id: Timeline.java,v 1.36 2007/08/16 14:09:56 jeffmc Exp $
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






package ucar.unidata.idv.ui;


import org.itc.idv.math.SunriseSunsetCollector;


import ucar.unidata.ui.Timeline;
import ucar.unidata.ui.LatLonWidget;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;


import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DateUtil;



import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DatedThing;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


import java.io.*;


import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;



import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * Widget for selecting dates and times
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.36 $
 */
public class IdvTimeline extends Timeline {

    /** _more_          */
    private LatLonPoint sunriseLocation;

    /** _more_          */
    private List sunriseLocations;


    /**
     * Default  ctor
     */
    public IdvTimeline() {
    }

    /**
     * ctor. start and end date is the min/max of the times
     *
     * @param times List of DatedThings
     * @param initDimension initial width
     */
    public IdvTimeline(List times, int initDimension) {
        super(times, initDimension);
    }


    /**
     * ctor. start and end date is the min/max of the times
     *
     * @param times List of DatedThings
     * @param width init width
     * @param height init height
     */
    public IdvTimeline(List times, int width, int height) {
        super(times, width, height);
    }


    /**
     * Create a Timeline with the initial date range
     *
     * @param start start
     * @param end end
     */
    public IdvTimeline(Date start, Date end) {
        super(start, end);
    }


    /**
     * ctor
     *
     * @param start start
     * @param end end
     * @param initDimension width
     */
    public IdvTimeline(Date start, Date end, int initDimension) {
        super(start, end, initDimension);
    }



    /**
     * make menu items
     *
     * @param items items
     */
    protected void getMenuItems(List items) {
        /*

        List      subItems;
        JMenuItem mi;

        items.add(mi = GuiUtils.makeMenuItem("Properties", this,
                                             "showProperties"));



        subItems = new ArrayList();
        long     now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance(getTimeZone());
        cal.setTimeInMillis(now);
        cal.clear(Calendar.MILLISECOND);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.HOUR, 0);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        now = cal.getTimeInMillis();

        subItems.add(GuiUtils.makeMenuItem("Reset", this, "resetDateRange"));

        subItems.add(
            GuiUtils.makeMenuItem(
                "Today", this, "setVisibleRange",
                new Date[] { new Date(now - DateUtil.daysToMillis(1)),
                             new Date(now) }));
        subItems.add(
            GuiUtils.makeMenuItem(
                "Past Week", this, "setVisibleRange",
                new Date[] { new Date(now - DateUtil.daysToMillis(7)),
                             new Date(now) }));
        subItems.add(
            GuiUtils.makeMenuItem(
                "Past Month", this, "setVisibleRange",
                new Date[] { new Date(now - DateUtil.daysToMillis(30)),
                             new Date(now) }));
        subItems.add(
            GuiUtils.makeMenuItem(
                "Past Year", this, "setVisibleRange",
                new Date[] { new Date(now - DateUtil.daysToMillis(365)),
                             new Date(now) }));

        if (dateSelectionActive()) {
            subItems.add(GuiUtils.makeMenuItem("Selection Range", this,
                    "setVisibleRange",
                    new Date[] { dateSelection.getStartFixedDate(),
                                 dateSelection.getEndFixedDate() }));
        }
        items.add(GuiUtils.makeMenu("Set Visible Range", subItems));


        if (dateSelectionActive()) {
            subItems = new ArrayList();
            subItems.add(GuiUtils.makeMenuItem("View", this,
                    "setVisibleRange",
                    new Date[] { dateSelection.getStartFixedDate(),
                                 dateSelection.getEndFixedDate() }));
            subItems.add(GuiUtils.makeMenuItem("Reset", this,
                    "resetDateSelection"));

            subItems.add(GuiUtils.makeMenuItem("Today", this,
                    "setDateSelection",
                    new Date[] { new Date(now - DateUtil.daysToMillis(1)),
                                 new Date(now) }));
            subItems.add(GuiUtils.makeMenuItem("Past Week", this,
                    "setDateSelection",
                    new Date[] { new Date(now - DateUtil.daysToMillis(7)),
                                 new Date(now) }));
            subItems.add(GuiUtils.makeMenuItem("Past Month", this,
                    "setDateSelection",
                    new Date[] { new Date(now - DateUtil.daysToMillis(30)),
                                 new Date(now) }));
            items.add(GuiUtils.makeMenu("Set Date Selection", subItems));
        }


        List sunriseLocations = getSunriseLocations();
        subItems = new ArrayList();
        subItems.add(GuiUtils.makeMenuItem("Clear Location", this,
                                           "clearSunriseLocation"));
        subItems.add(GuiUtils.makeMenuItem("Set Location", this,
                                           "setSunriseLocationFromUser"));
        if ((sunriseLocation != null) && getIsCapableOfSelection()) {
            subItems.add(GuiUtils.makeMenuItem("Select daytime", this,
                    "selectDaytime"));
        }

        if ((sunriseLocations != null) && (sunriseLocations.size() > 0)) {
            subItems.add(GuiUtils.MENU_SEPARATOR);
            for (int locIdx = 0; locIdx < sunriseLocations.size(); locIdx++) {
                LatLonPoint llp = (LatLonPoint) sunriseLocations.get(locIdx);
                subItems.add(
                    GuiUtils.makeMenuItem(
                        Misc.format(llp.getLatitude()) + "/"
                        + Misc.format(llp.getLongitude()), this,
                            "setSunriseLocation", llp));
            }
        }

        items.add(GuiUtils.makeMenu("Sunrise/Sunset", subItems));



        if (isCapableOfSelection) {
            items.add(GuiUtils.makeCheckboxMenuItem("Use Date Selection",
                    this, "useDateSelection", null));
        }
        if ( !dateSelectionActive()) {
            return;
        }

        items.add(GuiUtils.makeCheckboxMenuItem("Use Visible Range", this,
                "sticky", null));
        mi.setToolTipText("Make the selection range be the visible range");




        double[] intervals = {
            Double.NaN, 0, DateUtil.minutesToMillis(5),
            DateUtil.minutesToMillis(10), DateUtil.minutesToMillis(15),
            DateUtil.minutesToMillis(30), DateUtil.hoursToMillis(1),
            DateUtil.hoursToMillis(2), DateUtil.hoursToMillis(3),
            DateUtil.hoursToMillis(4), DateUtil.hoursToMillis(5),
            DateUtil.hoursToMillis(6), DateUtil.hoursToMillis(12),
            DateUtil.daysToMillis(1), DateUtil.daysToMillis(2),
            DateUtil.daysToMillis(7)
        };
        String[] intervalNames = {
            "Default", "0 minutes", "5 minutes", "10 minutes", "15 minutes",
            "30 minutes", "1 hour", "2 hours", "3 hours", "4 hours",
            "5 hours", "6 hours", "12 hours", "1 day", "2 days", "7 days",
        };

        subItems = new ArrayList();
        double currentInterval = dateSelection.getInterval();
        for (int i = 0; i < intervals.length; i++) {
            if (intervals[i] != intervals[i]) {
                continue;
            }
            String lbl = intervalNames[i];
            if (intervals[i] == 0) {
                lbl = "None";
            }
            subItems.add(GuiUtils.makeMenuItem(((intervals[i]
                    == currentInterval)
                    ? "-" + lbl + "-"
                    : " " + lbl + " "), this, "setInterval",
                                        new Double(intervals[i])));

        }

        items.add(GuiUtils.makeMenu("Interval", subItems));
        if (dateSelection.hasInterval()) {
            subItems = new ArrayList();
            double range;
            range = dateSelection.getPreRange();
            for (int i = 0; i < intervals.length; i++) {
                boolean isCurrent = intervals[i] == range;
                if ((range != range) && (intervals[i] != intervals[i])) {
                    isCurrent = true;
                }
                String lbl = intervalNames[i];
                subItems.add(GuiUtils.makeMenuItem((isCurrent
                        ? "-" + lbl + "-"
                        : " " + lbl + " "), this, "setPreRange",
                                            new Double(intervals[i])));

            }
            items.add(GuiUtils.makeMenu("Before Range", subItems));

            subItems = new ArrayList();
            range    = dateSelection.getPostRange();
            for (int i = 0; i < intervals.length; i++) {
                boolean isCurrent = intervals[i] == range;
                if ((range != range) && (intervals[i] != intervals[i])) {
                    isCurrent = true;
                }
                String lbl = intervalNames[i];
                subItems.add(GuiUtils.makeMenuItem((isCurrent
                        ? "-" + lbl + "-"
                        : " " + lbl + " "), this, "setPostRange",
                                            new Double(intervals[i])));

            }
            items.add(GuiUtils.makeMenu("After Range", subItems));


        }





        subItems = new ArrayList();
        int   currentSkip = dateSelection.getSkip();
        int[] skips       = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50, 75, 100
        };
        for (int i = 0; i < skips.length; i++) {
            subItems.add(GuiUtils.makeMenuItem(((skips[i] == currentSkip)
                    ? "-" + skips[i] + "-"
                    : " " + skips[i] + " "), this, "setSkipFactor",
                                             new Integer(skips[i])));
        }
        items.add(GuiUtils.makeMenu("Skip Factor", subItems));


        subItems = new ArrayList();
        int[] counts = {
            DateSelection.MAX_COUNT, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20,
            25, 30, 40, 50, 75, 100
        };
        int currentCount = dateSelection.getCount();
        for (int i = 0; i < counts.length; i++) {
            int    cnt = counts[i];
            String lbl;
            if (cnt == DateSelection.MAX_COUNT) {
                lbl = "All";
            } else {
                lbl = "" + cnt;
            }
            subItems.add(GuiUtils.makeMenuItem(((cnt == currentCount)
                    ? "-" + lbl + "-"
                    : " " + lbl + " "), this, "setCount", new Integer(cnt)));
        }
        items.add(GuiUtils.makeMenu("Count", subItems));
        */
    }



    /**
     * The timeline changed. repaint, etc.
     */
    public void timelineChanged() {
        super.timelineChanged();
        makeSunriseDates();
        repaint();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getSunriseLocations() {
        return sunriseLocations;
    }



    /**
     * _more_
     */
    public void setSunriseLocationFromUser() {
        LatLonWidget llw = ((sunriseLocation != null)
                            ? new LatLonWidget(sunriseLocation.getLatitude(),
                                sunriseLocation.getLongitude())
                            : new LatLonWidget(0, 0));
        if ( !GuiUtils.showOkCancelDialog(null, "Sunrise Location",
                                          GuiUtils.inset(llw, 5), null)) {
            return;
        }
        setSunriseLocation(new LatLonPointImpl(llw.getLat(), llw.getLon()));
    }



    /**
     * _more_
     *
     * @param locations _more_
     */
    public void setSunriseLocations(List locations) {
        this.sunriseLocations = locations;
    }

    /**
     * _more_
     */
    public void clearSunriseLocation() {
        setSunriseLocation(null);
    }

    /**
     * _more_
     *
     * @param llp _more_
     */
    public void setSunriseLocation(LatLonPoint llp) {
        sunriseLocation = llp;
        makeSunriseDates();
        repaint();
    }

    /**
     * _more_
     */
    public void makeSunriseDates() {
        sunriseDates = makeSunriseDates(sunriseLocation, getStartDate(), getEndDate());
    }


    /**
     * _more_
     */
    public static List  makeSunriseDates(LatLonPoint sunriseLocation, Date startDate, Date endDate) {
        List sunriseDates = new ArrayList();
        if (sunriseLocation == null) {
            return sunriseDates;
        }
        try {
            //Pad them out 24 hours
            GregorianCalendar gc1 = new GregorianCalendar();
            gc1.setTime(new Date(startDate.getTime()
                                 - DateUtil.hoursToMillis(48)));
            GregorianCalendar gc2 = new GregorianCalendar();
            gc2.setTime(new Date(endDate.getTime()
                                 + DateUtil.hoursToMillis(48)));
            List                   dates = Misc.newList(gc1, gc2);
            SunriseSunsetCollector ssc   = new SunriseSunsetCollector(dates);
            List cals = ssc.calculate(sunriseLocation.getLatitude(),
                                      sunriseLocation.getLongitude());
            for (int i = 0; i < cals.size(); i++) {
                GregorianCalendar cal = (GregorianCalendar) cals.get(i);
                sunriseDates.add(cal.getTime());
            }
            //            System.err.println("dates:" + sunriseDates);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return sunriseDates;

    }




}

