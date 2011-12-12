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

package ucar.visad.display;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import visad.DateTime;
import visad.Set;

import visad.VisADException;

import java.awt.*;


import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.*;


/**
 * Holds state for constructing synthetic animation sets
 */

public class AnimationSetInfo {

    /** Mode for constructing set */
    public static final int TIMEMODE_DATA = 0;

    /** Mode for constructing set */
    public static final int TIMEMODE_CURRENT = 1;

    /** Mode for constructing set */
    public static final int TIMEMODE_FIXED = 2;

    /** Mode for constructing set */
    public static final int TIMEMODE_RELATIVE = 3;

    /** Mode for constructing set */
    public static int[] TIMEMODES = { TIMEMODE_DATA, TIMEMODE_CURRENT,
                                      TIMEMODE_FIXED, TIMEMODE_RELATIVE };

    /** Mode for constructing set */
    public static String[] STARTMODELABELS = { "Use Minimum Time from All Data",
            "Current Time (Now)", "Fixed", "Relative to End Time" };


    /** Mode for constructing set */
    public static String[] ENDMODELABELS = { "Use Maximum Time from All Data",
                                             "Current Time (Now)", "Fixed",
                                             "Relative to Start Time" };

    /** The start fixed time  in seconds */
    private long startFixedTime = Long.MAX_VALUE;

    /** The end fixed time  in seconds */
    private long endFixedTime = Long.MAX_VALUE;


    /** Start offset */
    private double startOffsetMinutes = 0.0;

    /** End offset */
    private double endOffsetMinutes = 0.0;

    /** Interval time */
    private double intervalMinutes = 15.0;


    /** Start mode */
    private int startMode = TIMEMODE_DATA;

    /** End mode */
    private int endMode = TIMEMODE_DATA;

    /** Do we create aa synthetic set or not */
    private boolean active = false;


    /** How often do we poll */
    private double pollMinutes = 5.0;

    /** Minutes to round to */
    private double roundTo = 1.0;

    /** _more_          */
    private Set baseTimes;


    /**
     * ctor
     */
    public AnimationSetInfo() {}


    /**
     * copy ctor
     *
     * @param that object to copy from
     */
    public AnimationSetInfo(AnimationSetInfo that) {
        this.active             = that.active;
        this.startMode          = that.startMode;
        this.endMode            = that.endMode;
        this.intervalMinutes    = that.intervalMinutes;
        this.pollMinutes        = that.pollMinutes;
        this.startOffsetMinutes = that.startOffsetMinutes;
        this.endOffsetMinutes   = that.endOffsetMinutes;
        this.roundTo            = that.roundTo;
        this.startFixedTime     = that.startFixedTime;
        this.endFixedTime       = that.endFixedTime;
        this.baseTimes          = that.baseTimes;
    }


    /**
     * Utility to round the given seconds
     *
     * @param seconds time to round
     *
     * @return Rounded value
     */
    private double round(double seconds) {
        return roundTo(roundTo, seconds);
    }


    /**
     * Utility to round the given seconds
     *
     *
     * @param roundTo _more_
     * @param seconds time to round
     *
     * @return Rounded value
     */
    public static double roundTo(double roundTo, double seconds) {
        int roundToSeconds = (int) (roundTo * 60);
        if (roundToSeconds == 0) {
            return seconds;
        }
        return seconds - ((int) seconds) % roundToSeconds;
    }


    /**
     * _more_
     *
     * @param times _more_
     */
    protected void setBaseTimes(Set times) {
        this.baseTimes = times;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected Set getBaseTimes() {
        return this.baseTimes;
    }



    /**
     * Create the time set
     *
     * @param displayMaster The display master we're in
     *
     * @return The time set
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Set makeTimeSet(DisplayMaster displayMaster)
            throws VisADException, RemoteException {

        List       dateTimes    = new ArrayList();

        long       now          = (long) (System.currentTimeMillis()
                                          / 1000.0);
        double     startSeconds = 0.0;
        double     endSeconds   = 0.0;
        double[][] dataTimeSet  = null;

        //        System.err.println ("makeTimeSet " + baseTimes + " " + displayMaster);
        if (((startMode == TIMEMODE_DATA) || (endMode == TIMEMODE_DATA))) {
            Set timeSet = ((baseTimes != null)
                           ? baseTimes
                           : ((displayMaster != null)
                              ? displayMaster
                                  .getAnimationSetFromDisplayables()
                              : null));
            if (timeSet == null) {
                return null;
            }
            dataTimeSet = timeSet.getDoubles();
            if ((dataTimeSet == null) || (dataTimeSet.length == 0)
                    || (dataTimeSet[0].length == 0)) {
                //                System.err.println ("\tdata is null");
                return null;
            }
        }
        double interval = 60 * getIntervalMinutes();
        if (interval == 0) {
            return null;
        }

        if (startMode == TIMEMODE_DATA) {
            if (dataTimeSet != null) {
                double minValue = dataTimeSet[0][0];
                for (int i = 1; i < dataTimeSet[0].length; i++) {
                    minValue = Math.min(minValue, dataTimeSet[0][i]);
                }
                startSeconds = minValue;
            } else {
                startSeconds = now;
            }
        } else if (startMode == TIMEMODE_CURRENT) {
            startSeconds = now;
        } else if (startMode == TIMEMODE_FIXED) {
            startSeconds = startFixedTime / 1000;
        }

        if (endMode == TIMEMODE_DATA) {
            if (dataTimeSet != null) {
                double maxValue = dataTimeSet[0][0];
                for (int i = 1; i < dataTimeSet[0].length; i++) {
                    maxValue = Math.max(maxValue, dataTimeSet[0][i]);
                }
                endSeconds = maxValue;
            } else {
                endSeconds = now;
            }
        } else if (endMode == TIMEMODE_CURRENT) {
            endSeconds = now;
        } else if (endMode == TIMEMODE_FIXED) {
            endSeconds = endFixedTime / 1000;
        }


        if (startMode != TIMEMODE_RELATIVE) {
            startSeconds += startOffsetMinutes * 60;
            startSeconds = round(startSeconds);
        }
        if (endMode != TIMEMODE_RELATIVE) {
            endSeconds += endOffsetMinutes * 60;
            //      double foo = endSeconds;
            endSeconds = round(endSeconds);
            //      System.err.println("before:" + ((int)foo) +" after:" + ((int)endSeconds));
        }
        if (startMode == TIMEMODE_RELATIVE) {
            startSeconds = endSeconds + startOffsetMinutes * 60;
            startSeconds = round(startSeconds);
        }

        if (endMode == TIMEMODE_RELATIVE) {
            endSeconds = startSeconds + endOffsetMinutes * 60;
            endSeconds = round(endSeconds);
        }

        //      System.err.println("start:" + startSeconds +" end:" + endSeconds);
        //        System.err.println("");


        double cnt = (int) ((double) (endSeconds - startSeconds)) / interval;
        if (cnt > 10000) {
            throw new IllegalStateException(
                "Too many times in animation set:" + cnt);
        }
        while (startSeconds <= endSeconds) {
            //      System.err.print (" " + startSeconds);
            dateTimes.add(0, new DateTime(startSeconds));
            startSeconds += interval;
        }
        //      System.err.println ("");
        if (dateTimes.size() == 0) {
            return null;
        }
        return makeTimeSet(dateTimes);


    }



    /**
     * Utility to make a time set from a list of datetimes
     *
     * @param dateTimes List of datetimes
     *
     * @return The time set
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private Set makeTimeSet(List dateTimes)
            throws VisADException, RemoteException {
        DateTime[] dateTimeArray = new DateTime[dateTimes.size()];
        for (int i = 0; i < dateTimes.size(); i++) {
            dateTimeArray[i] = (DateTime) dateTimes.get(i);
        }
        return DateTime.makeTimeSet(dateTimeArray);
    }

    /**
     * Do we use the now time for either our start or end time
     *
     * @return Use now time
     */
    public boolean usingCurrentTime() {
        return active
               && ((startMode == TIMEMODE_CURRENT)
                   || (endMode == TIMEMODE_CURRENT));
    }

    /**
     * Set the Active property.
     *
     * @param value The new value for Active
     */
    public void setActive(boolean value) {
        active = value;
    }

    /**
     * Get the Active property.
     *
     * @return The Active
     */
    public boolean getActive() {
        return active;
    }

    /**
     * Set the StartMode property.
     *
     * @param value The new value for StartMode
     */
    public void setStartMode(int value) {
        startMode = value;
    }

    /**
     * Get the StartMode property.
     *
     * @return The StartMode
     */
    public int getStartMode() {
        return startMode;
    }

    /**
     * Set the EndMode property.
     *
     * @param value The new value for EndMode
     */
    public void setEndMode(int value) {
        endMode = value;
    }

    /**
     * Get the EndMode property.
     *
     * @return The EndMode
     */
    public int getEndMode() {
        return endMode;
    }





    /**
     * Set the IntervalMinutes property.
     *
     * @param value The new value for IntervalMinutes
     */
    public void setIntervalMinutes(double value) {
        intervalMinutes = value;
    }

    /**
     * Get the IntervalMinutes property.
     *
     * @return The IntervalMinutes
     */
    public double getIntervalMinutes() {
        return intervalMinutes;
    }



    /**
     * Set the PollMinutes property.
     *
     * @param value The new value for PollMinutes
     */
    public void setPollMinutes(double value) {
        pollMinutes = value;
        if (pollMinutes < 0.1) {
            pollMinutes = 0.1;
        }
    }

    /**
     * Get the PollMinutes property.
     *
     * @return The PollMinutes
     */
    public double getPollMinutes() {
        return pollMinutes;
    }

    /**
     * Set the StartOffsetMinutes property.
     *
     * @param value The new value for StartOffsetMinutes
     */
    public void setStartOffsetMinutes(double value) {
        startOffsetMinutes = value;
    }

    /**
     * Get the StartOffsetMinutes property.
     *
     * @return The StartOffsetMinutes
     */
    public double getStartOffsetMinutes() {
        return startOffsetMinutes;
    }

    /**
     * Set the EndOffsetMinutes property.
     *
     * @param value The new value for EndOffsetMinutes
     */
    public void setEndOffsetMinutes(double value) {
        endOffsetMinutes = value;
    }

    /**
     * Get the EndOffsetMinutes property.
     *
     * @return The EndOffsetMinutes
     */
    public double getEndOffsetMinutes() {
        return endOffsetMinutes;
    }

    /**
     * Set the RoundTo property.
     *
     * @param value The new value for RoundTo
     */
    public void setRoundTo(double value) {
        roundTo = value;
    }

    /**
     * Get the RoundTo property.
     *
     * @return The RoundTo
     */
    public double getRoundTo() {
        return roundTo;
    }

    /**
     *  Set the StartFixedTime property.
     *
     *  @param value The new value for StartFixedTime
     */
    public void setStartFixedTime(long value) {
        startFixedTime = value;
    }


    /**
     * set property
     *
     * @param d property
     */
    public void setStartFixedTime(Date d) {
        startFixedTime = d.getTime();
    }


    /**
     * set property
     *
     * @param d property
     */
    public void setEndFixedTime(Date d) {
        endFixedTime = d.getTime();
    }

    /**
     * get the property
     *
     * @return property
     */
    public Date getStartFixedDate() {
        return new Date(getStartFixedTime());
    }


    /**
     * get the property
     *
     * @return property
     */
    public Date getEndFixedDate() {
        return new Date(getEndFixedTime());
    }

    /**
     *  Get the StartFixedTime property.
     *
     *  @return The StartFixedTime
     */
    public long getStartFixedTime() {
        if (startFixedTime == Long.MAX_VALUE) {
            startFixedTime = System.currentTimeMillis();
        }
        return startFixedTime;
    }

    /**
     *  Set the EndFixedTime property.
     *
     *  @param value The new value for EndFixedTime
     */
    public void setEndFixedTime(long value) {
        endFixedTime = value;
    }

    /**
     *  Get the EndFixedTime property.
     *
     *  @return The EndFixedTime
     */
    public long getEndFixedTime() {
        if (endFixedTime == Long.MAX_VALUE) {
            endFixedTime = System.currentTimeMillis();
        }
        return endFixedTime;
    }



}
