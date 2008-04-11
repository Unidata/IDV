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


import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 5:00:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrackCollection {

    /** _more_          */
    //private HashMap forecastWayMapStartDates;

    /** _more_          */
    //private Track obsTrack;

    /** _more_          */
    private HashMap wayToTracksHashMap;

    /** _more_          */
    private List stormsTimeRanges;

    /**
     * _more_
     */
    public TrackCollection() {
        wayToTracksHashMap     = new HashMap();
        //forecastWayMapStartDates = new HashMap();
        //obsTrack           = null;
    }


    /**
     * _more_
     *
     * @param tracks _more_
     */
    public void addTrackList(List tracks) {
         Track track = (Track)tracks.get(0);
         wayToTracksHashMap.put(track.getWay(), tracks);
    }

   /**
     * _more_
     *
     * @param track _more_
     */
    public void addTrack(Track track) {
         wayToTracksHashMap.put(track.getWay(), track);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List getTrackList(Way way) {
        Object obj = wayToTracksHashMap.get(way);
        if(obj instanceof List)
            return (List)obj;
        else
            return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Track getTrack(Way way) {
        Object obj = wayToTracksHashMap.get(way);
        if(obj instanceof Track)
            return (Track)obj;
        else
            return null;
    }
    /**
     * _more_
     *
     * @return _more_
     */
    public HashMap getWayToTracksHashMap() {
        return wayToTracksHashMap;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public HashMap getWayToStartDatesHashMap() {
        HashMap wayToStartDatesHashMap = new HashMap();
        int size = wayToTracksHashMap.size();
        Set ways = wayToTracksHashMap.keySet();
        Iterator itr = ways.iterator();
        for(int i= 0; i< size; i++) {
            Way way = (Way)itr.next();
            List tracks = getTrackList(way);
            List startTimes = new ArrayList();
            if(tracks != null) {
                Iterator its = tracks.iterator();
                while(its.hasNext()) {
                    Track track = (Track)its.next();
                    Date st = track.getTrackStartTime();
                    startTimes.add(st);
                }
                if(startTimes.size()>0)
                    wayToStartDatesHashMap.put(way, startTimes);
            }

        }
        return wayToStartDatesHashMap;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public Track getObsTrack() {
        return (Track)wayToTracksHashMap.get("obsr");
    }

}

