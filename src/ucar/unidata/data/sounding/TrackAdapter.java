/*
 * $Id: TrackAdapter.java,v 1.65 2007/04/16 20:34:57 jeffmc Exp $
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

package ucar.unidata.data.sounding;


import ucar.ma2.Range;

import ucar.unidata.data.BadDataException;



import ucar.unidata.data.DataAlias;


import ucar.unidata.data.DataUtil;

import ucar.unidata.data.point.*;
import ucar.unidata.geoloc.Bearing;

import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;


import ucar.visad.UtcDate;

import ucar.visad.Util;
import ucar.visad.quantities.*;

import visad.*;

import visad.data.netcdf.Plain;
import visad.data.units.ParseException;
import visad.data.units.Parser;

import visad.georef.*;

import visad.util.DataUtility;


import java.awt.Color;
import java.awt.event.*;




import java.net.MalformedURLException;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JFrame;


/**
 * Adapter for track type data.  This could be a balloon sounding
 * with spatial (lat/lon/alt) data for each time step, or an
 * aircraft track.
 *
 * @author IDV Development Team
 * @version $Revision: 1.65 $ $Date: 2007/04/16 20:34:57 $
 */
public abstract class TrackAdapter {


    /** start time for the sounding release */
    DateTime startTime;

    /** end time for the sounding release */
    DateTime endTime;

    /** sounding observation */
    SoundingOb soundingOb = null;

    /** _more_          */
    TrackDataSource dataSource;

    /** List of TrackInfo, one for each trajectory in the data */
    private List trackInfos = new ArrayList();

    /** Access the data */
    //    private 


    /** Holds the variable names that we only show. If empty then we show all. */
    private Hashtable pointDataFilter;

    /** The stride */
    int stride = 1;

    /** last n minuts */
    int lastNMinutes = -1;

    /** filename */
    private String filename;




    /**
     * Construct a new track from the filename
     *
     *
     * @param dataSource _more_
     * @param filename  location of file
     *
     * @throws Exception    On badness
     */
    public TrackAdapter(TrackDataSource dataSource, String filename)
            throws Exception {
        this(dataSource, filename, null, 1, -1);
    }


    /**
     * Construct a new track from the filename
     *
     *
     * @param dataSource _more_
     * @param filename  location of file
     * @param pointDataFilter Filters the variables to use
     * @param stride The stride
     * @param lastNMinutes use the last N minutes
     *
     * @throws Exception    On badness
     */
    public TrackAdapter(TrackDataSource dataSource, String filename,
                        Hashtable pointDataFilter, int stride,
                        int lastNMinutes)
            throws Exception {
        this.dataSource      = dataSource;
        this.filename        = filename;
        this.stride          = stride;
        this.lastNMinutes    = lastNMinutes;
        this.pointDataFilter = pointDataFilter;
    }





    /**
     * _more_
     *
     * @param actions _more_
     */
    protected void addActions(List actions) {}

    /**
     * Add the track info to the list
     *
     * @param trackInfo Describes a track
     *
     * @throws Exception On badness
     */
    protected void addTrackInfo(TrackInfo trackInfo) throws Exception {
        trackInfos.add(trackInfo);
        if ((startTime == null)
                || (trackInfo.getStartTime().getValue(
                    CommonUnit.secondsSinceTheEpoch) < startTime.getValue(
                    CommonUnit.secondsSinceTheEpoch))) {
            startTime = trackInfo.getStartTime();
        }
        if ((endTime == null)
                || (trackInfo.getEndTime().getValue(
                    CommonUnit.secondsSinceTheEpoch) > endTime.getValue(
                    CommonUnit.secondsSinceTheEpoch))) {
            endTime = trackInfo.getEndTime();
        }

    }

    /**
     * Get list of TrackInfo-s
     *
     * @return list of TrackInfo-s
     */
    public List getTrackInfos() {
        return trackInfos;
    }


    /**
     *
     * Find the track info with the given name
     *
     * @param name name of track info
     *
     * @return the track info or null if not found
     */
    public TrackInfo getTrackInfo(String name) {
        for (int trackIdx = 0; trackIdx < trackInfos.size(); trackIdx++) {
            TrackInfo trackInfo = (TrackInfo) trackInfos.get(trackIdx);
            if ((name == null) || name.equals(trackInfo.getTrackName()) ||
                    name.startsWith(trackInfo.getTrackName())) {
                return trackInfo;
            }
        }
        throw new IllegalArgumentException("Unknown track:" + name);
    }


    /**
     * Returns a track for the variable name specified. Returned track is
     * of type:
     * <pre>
     * ((Latitude, Longitude, Altitude) -> (variable, Time)
     * </pre>
     *
     *
     * @param trackId Which track
     * @param variable   variable to get
     * @param range The data range of the request
     * @return FlatField of the type above.
     * @throws Exception On badness
     */
    public FlatField getTrackWithTime(String trackId, String variable,
                                      Range range)
            throws Exception {
        return getTrackInfo(trackId).getTrackWithTime(variable, range);
    }



    /**
     * Get the data range for the very last obs
     *
     * @param trackId which track
     *
     * @return The range
     *
     * @throws Exception On badness
     */
    public Range getLastPointRange(String trackId) throws Exception {
        int cnt = getNumObservations(trackId);
        return new Range(cnt - 1, cnt - 1);
    }

    /**
     * get total number of obs_
     *
     * @param trackId Which track
     *
     * @return num of obs
     *
     * @throws Exception On badness
     */
    public int getNumObservations(String trackId) throws Exception {
        return getTrackInfo(trackId).getNumberPoints();
    }



    /**
     * Take a FlatField of data and turn it into a field of PointObs.
     *
     *
     * @param trackId Which track
     * @param range The data range of the request
     * @return field of PointObs
     * @throws Exception On badness
     */
    public FieldImpl getPointObTrack(String trackId, Range range)
            throws Exception {
        return getTrackInfo(trackId).getPointObTrack(range);
    }




    /**
     * Returns a track for the variable name specified. Returned track is
     * of type:
     * <pre>
     * ((Latitude, Longitude, Altitude) -> (variable)
     * </pre>
     *
     *
     * @param trackId Which track
     * @param variable    variable of data
     * @param range The data range of the request
     * @return FlatField of the type above.
     * @throws Exception On badness
     */
    public FlatField getTrack(String trackId, String variable, Range range)
            throws Exception {
        return getTrackInfo(trackId).getTrack(variable, range);
    }



    /**
     * Get the track data parameters necessary to plot an aerological
     * diagram. Returned data is of type:
     * <pre>
     *     (Time -> (AirPressure,
     *                AirTemperature,
     *                Dewpoint,
     *                (Speed, Direction),
     *                (Latitude, Longitude, Altitude)))
     * </pre>
     *
     *
     * @param trackId id of the track
     * @return Data object in the format above
     *
     *
     * @throws Exception On badness
     */
    public Data getAerologicalDiagramData(String trackId) throws Exception {
        return getTrackInfo(trackId).getAerologicalDiagramData();
    }




    /**
     * Make a RAOB from the raw data
     *
     *
     * @param trackId id of the track
     * @return  RAOB
     *
     * @throws Exception On badness
     */
    private RAOB makeRAOB(String trackId) throws Exception {
        return getTrackInfo(trackId).makeRAOB();
    }



    /**
     * Get a SoundingOb corresponding to this track.
     *
     * @return  track as a sounding observation
     */
    private SoundingOb getSoundingOb() {
        return null;

        /**
         * if (soundingOb == null) {
         *   try {
         *       RealTuple launchPoint = DataUtility.getSample(llaSet, 0);
         *       SoundingStation station =
         *           new SoundingStation(
         *               "Sounding @"
         *               + baseTime
         *                   .toString(), ((Real) launchPoint.getComponent(0))
         *                   .getValue(
         *                       CommonUnit.degree), ((Real) launchPoint
         *                           .getComponent(1)).getValue(
         *                               CommonUnit
         *                                   .degree), ((Real) launchPoint
         *                                       .getComponent(2)).getValue(
         *                                           CommonUnit.meter));
         *       soundingOb = new SoundingOb(station, baseTime, xmakeRAOB());
         *   } catch (Exception e) {
         *       System.err.println("couldn't make soundingOb " + e);
         *   }
         * }
         * return soundingOb;
         */
    }



    /**
     * Get the base (starting) time of this track.
     *
     * @return starting time
     * @deprecated use #getStartTime()
     */
    public DateTime getBaseTime() {
        return getStartTime();
    }

    /**
     * Get the starting time of this track.
     *
     * @return starting time
     */
    public DateTime getStartTime() {
        return startTime;
    }

    /**
     * Get the ending time of this track.
     *
     * @return ending time
     */
    public DateTime getEndTime() {
        return endTime;
    }



    /**
     * Set the Stride property.
     *
     * @param value The new value for Stride
     */
    public void setStride(int value) {
        stride = value;
    }

    /**
     * Get the Stride property.
     *
     * @return The Stride
     */
    public int getStride() {
        return stride;
    }

    /**
     * Set the LastNMinutes property.
     *
     * @param value The new value for LastNMinutes
     */
    public void setLastNMinutes(int value) {
        lastNMinutes = value;
    }

    /**
     * Get the LastNMinutes property.
     *
     * @return The LastNMinutes
     */
    public int getLastNMinutes() {
        return lastNMinutes;
    }



    /**
     * Should we include the given var in the point data
     *
     * @param varName Variable name
     *
     * @return Include in point data
     */
    public boolean includeInPointData(String varName) {
        if ((pointDataFilter == null) || (pointDataFilter.size() == 0)) {
            return true;
        }
        return pointDataFilter.get(varName) != null;
    }


    /**
     * Get the Filename property.
     *
     * @return The Filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDataSourceName() {
        return null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getDataSourceDescription() {
        return null;
    }

    /*
     * String representation of this adapter
     *
     * @return String representation of this adapter
     */

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return filename;
    }





}

