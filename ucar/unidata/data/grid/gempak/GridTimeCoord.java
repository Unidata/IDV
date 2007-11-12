/*
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

// $Id:GridTimeCoord.java 63 2006-07-12 21:50:51Z edavis $

package ucar.unidata.data.grid.gempak;


//import ucar.grib.TableLookup;

import ucar.ma2.*;

import ucar.nc2.*;
import ucar.nc2.dataset.AxisType;
import ucar.nc2.dataset.conv._Coordinate;
import ucar.nc2.units.DateFormatter;
import ucar.nc2.units.DateUnit;

import java.util.*;


/**
 * A Time Coordinate for a Grid dataset.
 * @author caron
 * @version $Revision:63 $ $Date:2006-07-12 21:50:51Z $
 */
public class GridTimeCoord {

    /** _more_ */
    static private org.slf4j.Logger log =
        org.slf4j.LoggerFactory.getLogger(GridTimeCoord.class);

    // for parsing dates

    /** _more_ */
    private DateFormatter formatter = new DateFormatter();

    /** _more_ */
    private Calendar calendar;

    /** _more_ */
    private String name;

    /** _more_ */
    private GridTableLookup lookup;

    /** _more_ */
    private ArrayList times = new ArrayList();  //  Date
    //private double[] offsetHours;

    /** _more_ */
    private int seq = 0;

    /**
     * _more_
     */
    GridTimeCoord() {
        // need to have this non-static for thread safety
        calendar = Calendar.getInstance();
        calendar.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    }

    /**
     * _more_
     *
     * @param records _more_
     * @param lookup _more_
     */
    GridTimeCoord(List records, GridTableLookup lookup) {
        this();
        this.lookup = lookup;
        addTimes(records);
        Collections.sort(times);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param offsetHours _more_
     * @param lookup _more_
     */
    GridTimeCoord(String name, double[] offsetHours, GridTableLookup lookup) {
        this();
        this.name = name;
        //this.offsetHours = offsetHours;
        this.lookup = lookup;

        Date   baseTime = lookup.getFirstBaseTime();
        String refDate  = formatter.toDateTimeStringISO(baseTime);

        // the offset hours are reletive to whatever the base date is
        DateUnit convertUnit = null;
        try {
            convertUnit = new DateUnit("hours since " + refDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // now create a list of valid dates
        times = new ArrayList(offsetHours.length);
        for (int i = 0; i < offsetHours.length; i++) {
            double offsetHour = offsetHours[i];
            times.add(convertUnit.makeDate(offsetHour));
        }
    }

    /**
     * _more_
     *
     * @param records _more_
     */
    void addTimes(List records) {
        for (int i = 0; i < records.size(); i++) {
            GridRecord record    = (GridRecord) records.get(i);
            Date       validTime = getValidTime(record, lookup);
            if ( !times.contains(validTime)) {
                times.add(validTime);
            }
        }
    }

    /**
     * _more_
     *
     * @param records _more_
     *
     * @return _more_
     */
    boolean matchLevels(List records) {

        // first create a new list
        ArrayList timeList = new ArrayList(records.size());
        for (int i = 0; i < records.size(); i++) {
            GridRecord record    = (GridRecord) records.get(i);
            Date       validTime = getValidTime(record, lookup);
            if ( !timeList.contains(validTime)) {
                timeList.add(validTime);
            }
        }

        Collections.sort(timeList);
        return timeList.equals(times);
    }

    /**
     * _more_
     *
     * @param seq _more_
     */
    void setSequence(int seq) {
        this.seq = seq;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    String getName() {
        if (name != null) {
            return name;
        }
        return (seq == 0)
               ? "time"
               : "time" + seq;
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     * @param g _more_
     */
    void addDimensionsToNetcdfFile(NetcdfFile ncfile, Group g) {
        Collections.sort(times);
        ncfile.addDimension(g, new Dimension(getName(), getNTimes(), true));
    }

    /**
     * _more_
     *
     * @param ncfile _more_
     * @param g _more_
     */
    void addToNetcdfFile(NetcdfFile ncfile, Group g) {
        Variable v = new Variable(ncfile, g, null, getName());
        v.setDataType(DataType.INT);
        v.addAttribute(new Attribute("long_name", "forecast time"));
        //v.addAttribute( new Attribute("standard_name", "forecast_reference_time"));

        int      ntimes   = getNTimes();
        int[]    data     = new int[ntimes];

        Date     baseTime = lookup.getFirstBaseTime();
        String   timeUnit = lookup.getFirstTimeRangeUnitName();
        String   refDate  = formatter.toDateTimeStringISO(baseTime);
        DateUnit dateUnit = null;
        try {
            dateUnit = new DateUnit(timeUnit + " since " + refDate);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // convert the date into the time unit.
        for (int i = 0; i < times.size(); i++) {
            Date validTime = (Date) times.get(i);
            data[i] = (int) dateUnit.makeValue(validTime);
        }
        Array dataArray = Array.factory(DataType.INT.getClassType(),
                                        new int[] { ntimes }, data);

        v.setDimensions(v.getShortName());
        v.setCachedData(dataArray, false);

        Date d = lookup.getFirstBaseTime();

        v.addAttribute(new Attribute("units",
                                     timeUnit + " since " + refDate));
        //v.addAttribute( new Attribute("GRIB_orgReferenceTime", formatter.toDateTimeStringISO( d)));
        //v.addAttribute( new Attribute("GRIB2_significanceOfRTName", lookup.getFirstSignificanceOfRTName()));
        v.addAttribute(new Attribute(_Coordinate.AxisType,
                                     AxisType.Time.toString()));

        ncfile.addVariable(g, v);
    }

    /**
     * _more_
     *
     * @param record _more_
     *
     * @return _more_
     */
    int getIndex(GridRecord record) {
        Date validTime = getValidTime(record, lookup);
        return times.indexOf(validTime);
    }

    /**
     * _more_
     *
     * @param record _more_
     *
     * @return _more_
     */
    Date getValidTime(GridRecord record) {
        return getValidTime(record, lookup);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    int getNTimes() {
        return times.size();
    }

    /**
     * _more_
     *
     * @param record _more_
     * @param lookup _more_
     *
     * @return _more_
     */
    private Date getValidTime(GridRecord record, GridTableLookup lookup) {
        Date validTime = record.getValidTime();
        if (validTime != null) {
            return validTime;
        }

        try {
            validTime =
                formatter.getISODate(record.getReferenceTime().toString());
        } catch (Throwable e) {
            log.error("getValidTime(" + record.getReferenceTime() + ")", e);
            return null;
        }

        int    calandar_unit = Calendar.HOUR;
        int    factor        = 1;
        String timeUnit      = lookup.getFirstTimeRangeUnitName();

        if (timeUnit.equalsIgnoreCase("hour")
                || timeUnit.equalsIgnoreCase("hours")) {
            factor = 1;  // common case
        } else if (timeUnit.equalsIgnoreCase("minutes")
                   || timeUnit.equalsIgnoreCase("minute")) {
            calandar_unit = Calendar.MINUTE;
        } else if (timeUnit.equalsIgnoreCase("second")
                   || timeUnit.equalsIgnoreCase("secs")) {
            calandar_unit = Calendar.SECOND;
        } else if (timeUnit.equalsIgnoreCase("day")
                   || timeUnit.equalsIgnoreCase("days")) {
            factor = 24;
        } else if (timeUnit.equalsIgnoreCase("month")
                   || timeUnit.equalsIgnoreCase("months")) {
            factor = 24 * 30;  // ??
        } else if (timeUnit.equalsIgnoreCase("year")
                   || timeUnit.equalsIgnoreCase("years")
                   || timeUnit.equalsIgnoreCase("1year")) {
            factor = 24 * 365;        // ??
        } else if (timeUnit.equalsIgnoreCase("decade")) {
            factor = 24 * 365 * 10;   // ??
        } else if (timeUnit.equalsIgnoreCase("century")) {
            factor = 24 * 365 * 100;  // ??
        } else if (timeUnit.equalsIgnoreCase("3hours")) {
            factor = 3;
        } else if (timeUnit.equalsIgnoreCase("6hours")) {
            factor = 6;
        } else if (timeUnit.equalsIgnoreCase("12hours")) {
            // TODO: fix this in GRIB world
            factor = 12;
        }

        calendar.setTime(validTime);
        calendar.add(calandar_unit, factor * record.getValidTimeOffset());
        validTime = calendar.getTime();

        // TODO: should this just be done when the record is created?
        //record.setValidTime(validTime);
        return validTime;
    }

}

