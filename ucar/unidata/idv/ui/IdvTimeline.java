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


import org.itc.idv.math.SunriseSunsetCollector;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.ui.LatLonWidget;


import ucar.unidata.ui.Timeline;


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

    /** _more_ */
    private LatLonPoint sunriseLocation;

    /** _more_ */
    private List sunriseLocations;


    /**
     * Default  ctor
     */
    public IdvTimeline() {}

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
        super.getMenuItems(items);


        List sunriseLocations = getSunriseLocations();
        List subItems         = new ArrayList();
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
        sunriseDates = makeSunriseDates(sunriseLocation, getStartDate(),
                                        getEndDate());
    }


    /**
     * _more_
     *
     * @param sunriseLocation _more_
     * @param startDate _more_
     * @param endDate _more_
     *
     * @return _more_
     */
    public static List makeSunriseDates(LatLonPoint sunriseLocation,
                                        Date startDate, Date endDate) {
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
            for (int i = 0; i < cals.size(); i += 2) {
                GregorianCalendar cal1 = (GregorianCalendar) cals.get(i);
                GregorianCalendar cal2 = (GregorianCalendar) cals.get(i + 1);
                Date              d1   = cal1.getTime();
                Date              d2   = cal2.getTime();
                //Check the order 
                if (d1.getTime() > d2.getTime()) {
                    //                    System.err.println("Flipping the dates for location:" + sunriseLocation + " date1:" + d1  + " date2:" + d2);
                    Date tmp = d1;
                    d1 = d2;
                    d2 = tmp;

                }
                sunriseDates.add(d1);
                sunriseDates.add(d2);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return sunriseDates;

    }




}
