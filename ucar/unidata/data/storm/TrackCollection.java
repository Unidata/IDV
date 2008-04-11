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


import java.util.HashMap;
import java.util.List;
import java.util.Iterator;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 5:00:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrackCollection {

    /** _more_          */
    private HashMap forecastWayMapStartDates;

    /** _more_          */
    private Track obsTrack;

    /** _more_          */
    private HashMap forecastWayMapTracks;

    /** _more_          */
    private List stormsTimeRanges;

    /**
     * _more_
     */
    public TrackCollection() {
        forecastWayMapTracks     = new HashMap();
        forecastWayMapStartDates = new HashMap();
        obsTrack           = null;
    }


    /**
     * _more_
     *
     * @param tracks _more_
     */
    public void addForecastWayMapTracks(List tracks) {
         Track track = (Track)tracks.get(0);
         forecastWayMapTracks.put(track.getWay(), tracks);        
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public HashMap getForecastWayMapTracks() {
        return forecastWayMapTracks;
    }

    /**
     * _more_
     *
     * @param dTimes _more_
     */
    public void addForecastWayMapStartDates(Way way, List dTimes) {
        forecastWayMapStartDates.put(way, dTimes);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public HashMap getForecastWayMapStartDates() {
        return forecastWayMapStartDates;
    }

    /**
     * _more_
     *
     * @param track _more_
     */
    public void addObsTrack(Track track) {
        obsTrack = track;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Track getObsTrack() {
        return obsTrack;
    }

}

