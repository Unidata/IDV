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
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.util.Misc;
import ucar.visad.Util;


import visad.*;

import visad.georef.EarthLocation;

import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 5:00:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class StormTrack implements Comparable {


    /** _more_ */
    private List<StormParam> params = null;

    /** _more_ */
    private LatLonRect bbox;


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
    private List<StormTrackPoint> trackPoints;

    //private Date trackStartTime;

    private Hashtable temporaryProperties = new Hashtable();

    private static final int DIAMOND_MISSING_VALUE = 9999;

    private boolean isEdited = false;

    /**
     * _more_
     *
     * @param track _more_
     */
    public StormTrack(StormTrack track) {
        this.stormInfo   = track.stormInfo;
        this.way         = track.way;
        this.params      = track.params;
        this.trackId     = track.trackId;
        this.trackPoints = new ArrayList<StormTrackPoint>(track.trackPoints);
    }

    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param way _more_
     * @param pts _more_
     * @param params _more_
     */
    public StormTrack(StormInfo stormInfo, Way way,
                      List<StormTrackPoint> pts, StormParam[] params) {
        this.stormInfo = stormInfo;
        this.way       = way;
        if (params != null) {
            this.params = (List<StormParam>) Misc.toList(params);
        }
        this.trackPoints = new ArrayList<StormTrackPoint>(pts);
        StormTrackPoint firstPoint     = (StormTrackPoint) pts.get(0);
        DateTime        trackStartTime = firstPoint.getTime();
        this.trackId = stormInfo.toString() + "_" + way + "_"
                       + trackStartTime.getValue();
    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param way _more_
     * @param startTime _more_
     * @param params _more_
     */
    public StormTrack(StormInfo stormInfo, Way way, DateTime startTime,
                      StormParam[] params) {
        this.stormInfo = stormInfo;
        this.way       = way;
        if (params != null) {
            this.params = (List<StormParam>) Misc.toList(params);
        }
        this.trackPoints = new ArrayList();
        this.trackId = stormInfo.toString() + "_" + way + "_"
                       + startTime.getValue();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public LatLonRect getBoundingBox() {
        if (trackPoints.size() == 0) {
            return null;
        }
        if (bbox == null) {
            //  public LatLonRect(LatLonPoint left, LatLonPoint right) {
            double minLon = Double.POSITIVE_INFINITY;
            double maxLon = Double.NEGATIVE_INFINITY;
            double minLat = Double.POSITIVE_INFINITY;
            double maxLat = Double.NEGATIVE_INFINITY;
            for (StormTrackPoint stp : trackPoints) {
                EarthLocation el = stp.getLocation();
                minLat = Math.min(minLat, el.getLatitude().getValue());
                maxLat = Math.max(maxLat, el.getLatitude().getValue());
                minLon = Math.min(minLon, el.getLongitude().getValue());
                maxLon = Math.max(maxLon, el.getLongitude().getValue());
            }

            bbox = new LatLonRect(new LatLonPointImpl(maxLat, minLon),
                                  new LatLonPointImpl(minLat, maxLon));
        }
        return bbox;
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

            double     v1   = getStartTime().getValue();
            double     v2   = that.getStartTime().getValue();
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
    public boolean isEdited() {
        return isEdited;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public void setIsEdited(boolean isEdited) {
        this.isEdited = isEdited;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsEdited() {
        return this.isEdited;
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
    public void setId(String id) {
        this.trackId = id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return trackId;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public DateTime getStartTime() {
        StormTrackPoint firstPoint = trackPoints.get(0);
        return firstPoint.getTime();

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
            trackTimes.add(stp.getTime());
        }
        return trackTimes;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormParam> getParams() {
        if (params == null) {
            params = new ArrayList<StormParam>();
            Hashtable seenParam = new Hashtable();
            for (StormTrackPoint stp : trackPoints) {
                List<Real> reals = stp.getTrackAttributes();
                for (Real r : reals) {
                    RealType type = (RealType) r.getType();
                    if (seenParam.get(type) == null) {
                        seenParam.put(type, type);
                        params.add(new StormParam(type));
                    }
                }
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
            locs.add(stp.getLocation());
        }
        return locs;
    }


    /**
     * _more_
     *
     *
     *
     * @param param _more_
     * @return _more_
     *
     * @throws VisADException _more_
     */
    public Real[] getTrackAttributeValues(StormParam param)
            throws VisADException {
        if (param == null) {
            return null;
        }
        int    size            = trackPoints.size();
        Real[] trackAttributes = new Real[size];
        Real   missing         = null;
        for (int i = 0; i < size; i++) {
            Real value = trackPoints.get(i).getAttribute(param);
            if (value == null) {
                if (i == 0) {
                    return null;
                }
                trackAttributes[i] = null;
            } else {
                if (missing == null) {
                    missing = value.cloneButValue(Double.NaN);
                }
                trackAttributes[i] = value;
            }
        }
        for (int i = 0; i < size; i++) {
            if (trackAttributes[i] == null) {
                trackAttributes[i] = missing;
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
     * Return the index of the given track point. This kist finds the point with the same lat/lon
     *
     * @param stp The track point
     * @return The index or -1 if not found
     */
    public int indexOf(StormTrackPoint stp) {
        for (int i = 0; i < trackPoints.size(); i++) {
            if(trackPoints.get(i).getLocation().equals(stp.getLocation())) {
                return i;
            }
        }
        return -1;
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



    public void putTemporaryProperty(Object key, Object value) {
        temporaryProperties.put(key,value);
    }

    public Object getTemporaryProperty(Object key) {
        return temporaryProperties.get(key);
    }




    static public StringBuffer toDiamond7( List<StormTrack> sts, String id ) throws VisADException {
        StringBuffer sb = new StringBuffer();
        sb.append("diamond 7 " + id + "TropicalCycloneTrack"+ "\n");
        for(StormTrack st : sts) {
            st.toDiamond7(sb, id);
        }
        return sb;
    }



    public void toDiamond7(StringBuffer sb, String id) throws VisADException {
        Calendar cal = Calendar.getInstance();
        List<StormTrackPoint> tpoints = getTrackPoints();

        sb.append("Name " + id + " " + way+ " " + tpoints.size()+ "\n");
        for (StormTrackPoint stp : tpoints) {
            Date dttm = null;

            try {
                dttm = Util.makeDate(stp.getTime());
            }  catch (Exception excp) {

            }
            cal.setTime(dttm);
            String year = Integer.toString(cal.get(Calendar.YEAR));
            int mm = cal.get(Calendar.MONTH);
            String mon = Integer.toString(mm);
            if(mm < 10)
                mon = "0" + mon;
            int dd = cal.get(Calendar.DAY_OF_MONTH);
            String day = Integer.toString(dd);
            if(dd < 10)
                day = "0" + day;
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int fhour = stp.getForecastHour();
            EarthLocation el = stp.getLocation();
            List<Real> attrs    = stp.getTrackAttributes();

            sb.append(year.substring(2));
            sb.append(" ");
            sb.append(mon);
            sb.append(" ");
            sb.append(day);
            sb.append(" ");
            sb.append(hour);
            sb.append(" ");
            sb.append(fhour);
            sb.append(" ");
            sb.append(el.getLongitude().getValue(CommonUnit.degree));
            sb.append(" ");
            sb.append(el.getLatitude().getValue(CommonUnit.degree));
            sb.append(" ");

            //TODO: What to do with units?
            appendDiamondValue(sb,stp.getAttribute(STIStormDataSource.PARAM_MAXWINDSPEED));
            appendDiamondValue(sb, stp.getAttribute(STIStormDataSource.PARAM_MINPRESSURE));
            appendDiamondValue(sb, stp.getAttribute(STIStormDataSource.PARAM_RADIUSMODERATEGALE));
            appendDiamondValue(sb, stp.getAttribute(STIStormDataSource.PARAM_RADIUSWHOLEGALE));
            appendDiamondValue(sb,stp.getAttribute(STIStormDataSource.PARAM_MOVESPEED));
            appendDiamondValue(sb, stp.getAttribute(STIStormDataSource.PARAM_MOVEDIRECTION));
                  
            sb.append("\n");
        }
    }


    private void appendDiamondValue(StringBuffer sb,Real r) {
        if(r == null || Double.isNaN(r.getValue()))
            sb.append(DIAMOND_MISSING_VALUE);
        else
            sb.append(r.getValue());
        sb.append(" ");
    }


}

