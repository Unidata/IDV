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
public class StormTrack {

    /** _more_ */
    private String trackId;

    /** _more_ */
    private StormInfo stormInfo;

    /** _more_ */
    private Way way;


    private NamedArray lats;
    private NamedArray lons;
    private List<DateTime> times;

    /** _more_ */
    private List<StormTrackPoint> trackPoints;

    //private Date trackStartTime;


    public StormTrack(StormInfo stormInfo, Way way, NamedArray lats, NamedArray lons, List<DateTime> times) {
        this.stormInfo   = stormInfo;
        this.way         = way;
        this.lats = lats;
        this.lons = lons;
        this.times = times;
    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param way _more_
     * @param pts _more_
     */
    public StormTrack(StormInfo stormInfo, Way way, List<StormTrackPoint> pts) {
        this.stormInfo   = stormInfo;
        this.way         = way;
        this.trackPoints = new ArrayList(pts);
        StormTrackPoint firstPoint = (StormTrackPoint)pts.get(0);
        DateTime trackStartTime = firstPoint.getTrackPointTime();
        this.trackId = stormInfo.toString() + "_" + way + "_"
                       + trackStartTime.getValue();
    }




    public StormTrack(StormInfo stormInfo, Way way, DateTime startTime) {
        this.stormInfo   = stormInfo;
        this.way         = way;
        this.trackPoints = new ArrayList();
        this.trackId = stormInfo.toString() + "_" + way + "_"
                       + startTime.getValue();
    }





    public void addPoint(StormTrackPoint point) {
        trackPoints.add(point);
    }

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
        for(StormTrackPoint stp: trackPoints){
            trackTimes.add(stp.getTrackPointTime());
        }
        return trackTimes;
    }


    public List<EarthLocation> getLocations() {
        List<EarthLocation> locs = new ArrayList();
        for(StormTrackPoint stp: trackPoints){
            locs.add(stp.getTrackPointLocation());
        }
        return locs;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public float [] getTrackAttributeValues(String attrName) {
        int size = trackPoints.size();
        float [] trackAttributes = new float[size];
        for(int i = 0; i< size; i++){
            String str = trackPoints.get(i).getAttribute(attrName);
            if(str.startsWith("9999"))
                 trackAttributes[i] = Float.NaN;
            else
                trackAttributes[i] = Float.valueOf(str);
        }
        for(int i = 0; i< size; i++){
            if( trackAttributes[i] == Float.NaN ) {
                 trackAttributes[i] =  findClosestAttr(trackAttributes, i) ;
            }
        }

        return trackAttributes;
    }

    public float findClosestAttr(float [] trackAttributes, int i) {
        int up = i;
        int down = i;
        int size = trackAttributes.length;
        float value = Float.NaN;
        while(Float.isNaN(value)) {
            up++;
            down--;
            if(up > 0 && up < size)
               value = trackAttributes[up];
            if(down > 0 && down < size)
               value = trackAttributes[down];
        }

        return value;

    }
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
Set the Lats property.

@param value The new value for Lats
**/
public void setLats (NamedArray value) {
	lats = value;
}

/**
Get the Lats property.

@return The Lats
**/
public NamedArray getLats () {
	return lats;
}

/**
Set the Lons property.

@param value The new value for Lons
**/
public void setLons (NamedArray value) {
	lons = value;
}

/**
Get the Lons property.

@return The Lons
**/
public NamedArray getLons () {
	return lons;
}

/**
Set the Times property.

@param value The new value for Times
**/
public void setTimes (List<DateTime> value) {
	times = value;
}

/**
Get the Times property.

@return The Times
**/
public List<DateTime> getTimes () {
	return times;
}



}

