/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
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



package ucar.unidata.data.storm;


import ucar.unidata.data.*;


import visad.*;

import visad.georef.EarthLocation;

import java.util.ArrayList;

import java.util.List;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 5:00:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormTrack implements Comparable {

    /** _more_ */
    private String trackId;

    /** _more_ */
    private StormInfo stormInfo;

    /** _more_ */
    private Way way;


    /** _more_ */
    private NamedArray lats;

    /** _more_ */
    private NamedArray lons;

    /** _more_ */
    private List<DateTime> times;

    /** _more_ */
    private List<StormTrackPoint> trackPoints;

    //private Date trackStartTime;


    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param way _more_
     * @param lats _more_
     * @param lons _more_
     * @param times _more_
     */
    public StormTrack(StormInfo stormInfo, Way way, NamedArray lats,
                      NamedArray lons, List<DateTime> times) {
        this.stormInfo = stormInfo;
        this.way       = way;
        this.lats      = lats;
        this.lons      = lons;
        this.times     = times;
    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param way _more_
     * @param pts _more_
     */
    public StormTrack(StormInfo stormInfo, Way way,
                      List<StormTrackPoint> pts) {
        this.stormInfo   = stormInfo;
        this.way         = way;
        this.trackPoints = new ArrayList<StormTrackPoint>(pts);
        StormTrackPoint firstPoint     = (StormTrackPoint) pts.get(0);
        DateTime        trackStartTime = firstPoint.getTrackPointTime();
        this.trackId = stormInfo.toString() + "_" + way + "_"
                       + trackStartTime.getValue();
    }




    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param way _more_
     * @param startTime _more_
     */
    public StormTrack(StormInfo stormInfo, Way way, DateTime startTime) {
        this.stormInfo   = stormInfo;
        this.way         = way;
        this.trackPoints = new ArrayList();
        this.trackId = stormInfo.toString() + "_" + way + "_"
                       + startTime.getValue();
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public int compareTo(Object o) {
        if (o instanceof StormTrack) {
            StormTrack that = (StormTrack) o;

            double     v1   = getTrackStartTime().getValue();
            double     v2   = that.getTrackStartTime().getValue();
            if (v1 < v2) {
                return -1;
            }
            if (v1 > v2) {
                return 1;
            }
            return 0;
        }
        return toString().compareTo(o.toString());
    }


    /**
     * _more_
     *
     * @param hour _more_
     *
     * @return _more_
     */
    public StormTrackPoint findPointWithForecastHour(int hour) {
        for (StormTrackPoint stp : trackPoints) {
            if (stp.getForecastHour() == hour) {
                return stp;
            }
        }
        return null;
    }

    /**
     * _more_
     *
     * @param point _more_
     */
    public void addPoint(StormTrackPoint point) {
        trackPoints.add(point);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isObservation() {
        return way.isObservation();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return trackId.hashCode();
    }

    /**
     * _more_
     *
     * @param id _more_
     */
    public void setTrackId(String id) {
        this.trackId = id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getTrackId() {
        return trackId;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getTrackStartTime() {
        StormTrackPoint firstPoint = trackPoints.get(0);
        return firstPoint.getTrackPointTime();

    }

    /**
     * _more_
     *
     * @param stormInfo _more_
     */
    public void setStormInfo(StormInfo stormInfo) {
        this.stormInfo = stormInfo;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public StormInfo getStormInfo() {
        return stormInfo;
    }

    /**
     * _more_
     *
     * @param way _more_
     */
    public void setWay(Way way) {
        this.way = way;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Way getWay() {
        return way;
    }

    /**
     * _more_
     *
     * @param pts _more_
     */
    public void setTrackPoints(List<StormTrackPoint> pts) {
        this.trackPoints = new ArrayList<StormTrackPoint>(pts);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormTrackPoint> getTrackPoints() {
        return trackPoints;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<DateTime> getTrackTimes() {
        List<DateTime> trackTimes = new ArrayList();
        for (StormTrackPoint stp : trackPoints) {
            trackTimes.add(stp.getTrackPointTime());
        }
        return trackTimes;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormParam> getParams() {
        List<StormParam> params = new ArrayList<StormParam>();
        if (trackPoints.size() > 0) {
            List<Real> reals = trackPoints.get(0).getTrackAttributes();
            for (Real r : reals) {
                params.add(new StormParam((RealType) r.getType()));
            }

        }
        return params;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<EarthLocation> getLocations() {
        List<EarthLocation> locs = new ArrayList();
        for (StormTrackPoint stp : trackPoints) {
            locs.add(stp.getTrackPointLocation());
        }
        return locs;
    }


    /**
     * _more_
     *
     *
     * @param type _more_
     *
     * @param param _more_
     * @return _more_
     */
    public Real[] getTrackAttributeValues(StormParam param) {
        if (param == null) {
            return null;
        }
        int    size            = trackPoints.size();
        Real[] trackAttributes = new Real[size];
        for (int i = 0; i < size; i++) {
            Real value = trackPoints.get(i).getAttribute(param);
            if (value == null) {
                if (i == 0) {
                    return null;
                }
                trackAttributes[i] = null;
            } else {
                trackAttributes[i] = value;
            }
        }
        return trackAttributes;
    }

    /**
     * _more_
     *
     * @param trackAttributes _more_
     * @param i _more_
     *
     * @return _more_
     */
    public float findClosestAttr(float[] trackAttributes, int i) {
        int   up    = i;
        int   down  = i;
        int   size  = trackAttributes.length;
        float value = Float.NaN;
        while (Float.isNaN(value)) {
            up++;
            down--;
            if ((up > 0) && (up < size)) {
                value = trackAttributes[up];
            }
            if ((down > 0) && (down < size)) {
                value = trackAttributes[down];
            }
        }

        return value;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return trackId;
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if ( !(o instanceof StormTrack)) {
            return false;
        }
        StormTrack other = (StormTrack) o;
        return ((trackId.equals(other.trackId)));
    }



    /**
     * Set the Lats property.
     *
     * @param value The new value for Lats
     */
    public void setLats(NamedArray value) {
        lats = value;
    }

    /**
     * Get the Lats property.
     *
     * @return The Lats
     */
    public NamedArray getLats() {
        return lats;
    }

    /**
     * Set the Lons property.
     *
     * @param value The new value for Lons
     */
    public void setLons(NamedArray value) {
        lons = value;
    }

    /**
     * Get the Lons property.
     *
     * @return The Lons
     */
    public NamedArray getLons() {
        return lons;
    }

    /**
     * Set the Times property.
     *
     * @param value The new value for Times
     */
    public void setTimes(List<DateTime> value) {
        times = value;
    }

    /**
     * Get the Times property.
     *
     * @return The Times
     */
    public List<DateTime> getTimes() {
        return times;
    }



}

