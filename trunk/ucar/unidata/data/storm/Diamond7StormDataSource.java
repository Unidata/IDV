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


import org.apache.commons.net.ftp.FTP;

import org.apache.commons.net.ftp.FTPClient;

import ucar.nc2.Attribute;

import ucar.unidata.data.*;



import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.visad.Util;

import visad.*;
import visad.Set;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.EarthLocationTuple;

import java.io.*;

import java.net.URL;

import java.rmi.RemoteException;

import java.sql.*;

import java.text.SimpleDateFormat;

import java.util.*;


import java.util.Date;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: May 8, 2009
 * Time: 10:02:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class Diamond7StormDataSource extends StormDataSource {

    /**
     * _more_ 
     *
     * @return _more_
     */


    public String getId() {
        return "stiDiamond";
    }


    /** _more_ */
    public static StormParam PARAM_MAXWINDSPEED;

    /** _more_ */
    public static StormParam PARAM_RADIUSMODERATEGALE;

    /** _more_ */
    public static StormParam PARAM_RADIUSWHOLEGALE;


    /** _more_ */
    public static StormParam PARAM_DISTANCE_ERROR;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY100RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITYRADIUS;

    /** _more_ */
    public static StormParam PARAM_MOVEDIRECTION;

    /** _more_ */
    public static StormParam PARAM_MOVESPEED;



    /** _more_ */
    private static float MISSING = 9999.0f;


    /** _more_ */
    private String fileName;


    /** the stormInfo and track */
    private List<StormInfo> stormInfos;

    /** the stormInfo and track */
    private List<StormTrack> stormTracks;

    private HashMap<String, Way> stormWays;

    /**
     * constructor of sti storm data source
     *
     *
     *
     * @param descriptor _more_
     * @param fileName _more_
     * @param properties _more_
     * @throws Exception _more_
     */


    public Diamond7StormDataSource(DataSourceDescriptor descriptor,
                                   String fileName,
                                   Hashtable properties) throws Exception {
        super(descriptor, fileName, "Diamond7 Storm Data", properties);
        if ((fileName == null) || (fileName.trim().length() == 0)
                || fileName.trim().equalsIgnoreCase("default")) {
            System.err.println("No input file");;
        }

        this.fileName = fileName;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEditable() {
        return true;
    }



    static {
        try {
            //TODO: Make sure these are the right units
            PARAM_MINPRESSURE = new StormParam(makeRealType("minpressure",
                    "Min_Pressure", DataUtil.parseUnit("mb")));
            PARAM_MAXWINDSPEED = new StormParam(makeRealType("maxwindspeed",
                    "Max_Windspeed", Util.parseUnit("m/s")));
            PARAM_RADIUSMODERATEGALE =
                new StormParam(makeRealType("radiusmoderategale",
                                            "Radius_of_Beaufort_Scale7",
                                            DataUtil.parseUnit("km")));
            PARAM_RADIUSWHOLEGALE =
                new StormParam(makeRealType("radiuswholegale",
                                            "Radius_of_Beaufort_Scale10",
                                            DataUtil.parseUnit("km")));
            PARAM_MOVEDIRECTION =
                new StormParam(makeRealType("movedirection",
                                            "Storm_Direction",
                                            CommonUnit.degree));
            PARAM_MOVESPEED = new StormParam(makeRealType("movespeed",
                    "Storm_Speed", Util.parseUnit("m/s")));


        } catch (Exception exc) {
            System.err.println("Error creating storm params:" + exc);
            exc.printStackTrace();

        }
    }


    /**
     * _more_
     *
     * @throws VisADException _more_
     */
    protected void initParams() throws VisADException {
        super.initParams();

        obsParams = new StormParam[] {
            PARAM_MAXWINDSPEED, PARAM_MINPRESSURE, PARAM_RADIUSMODERATEGALE,
            PARAM_RADIUSWHOLEGALE, PARAM_MOVESPEED, PARAM_MOVEDIRECTION
        };

        forecastParams = new StormParam[] {
            PARAM_MAXWINDSPEED, PARAM_MINPRESSURE, PARAM_RADIUSMODERATEGALE,
            PARAM_RADIUSWHOLEGALE, PARAM_MOVESPEED, PARAM_MOVEDIRECTION
        };
    }





    /**
     * _more_
     *
     *
     * @throws Exception _more_
     */
    public Diamond7StormDataSource() throws Exception {}


    /**
     * _more_
     */
    protected void initializeStormData() {

        try {
            stormInfos  = new ArrayList<StormInfo>();
            stormTracks = new ArrayList<StormTrack>();
            stormWays = new HashMap<String, Way>();
            String s = IOUtil.readContents(fileName);
            /*

  diamond 7 0807 Tropical Cyclone Track
  Name 0807 japn 15
  08 06 15 8 0 123.7 18.1 18.0 996.0 NaN NaN NaN NaN
  08 06 15 14 6 124.4 18.8 19.0 995.0 NaN NaN NaN NaN
  year mon day time forecasttime lon lat speed pressure wind-circle-radii radii2 movspd movdir
                  */
            int                   lcn       = 0;
            String                sid       = null;
            String                sway      = null;
            boolean               nextTrack = false;
            Way                   trackWay  = null;
            StormInfo             sInfo     = null;
            List<StormTrackPoint> pts       = null;
            List<StormTrackPoint> obsPts       = new ArrayList();
            StormTrack            sTrack    = null;
            List<String> lines = StringUtil.split(s, "\n", true, true);
            int currentIndex = 0;
            String headerLine1 = lines.get(currentIndex++);
            double minTime= Double.MAX_VALUE;
            DateTime minDate=null;

            while(currentIndex < lines.size()) {

                String headerLine2 = lines.get(currentIndex++);
                List<String> toks = StringUtil.split(headerLine2, " ", true, true);
                sid = toks.get(1);
                sway     =  toks.get(2);
                int numberPts = Integer.parseInt(toks.get(3));
                trackWay = new Way(sway);
                stormWays.put(sway, trackWay);
                if(trackWay.isObservation()) hasObservation = true;
                if(sInfo==null) {
                    sInfo = new StormInfo(sid, new DateTime(new Date()));
                    stormInfos.add(sInfo);
                }

                pts      = new ArrayList();
                /*  */
                int endPtsIndex = currentIndex + numberPts;

                //System.out.println("endPtsIndex "+ endPtsIndex);
                while( currentIndex < endPtsIndex ) {

                    //System.out.println("currentIndex "+ currentIndex);
                    String line = lines.get(currentIndex++);
                    toks        = StringUtil.split(line, " ", true, true);
                    String year        =   toks.get(0);
                    String mon         =   toks.get(1);
                    String day         =   toks.get(2);
                    String hr          =   toks.get(3);
                    String fhr         =   toks.get(4);
                    String lon         =   toks.get(5);
                    String lat         =   toks.get(6);
                    String maxwindsp   =   toks.get(7);
                    String minpress    =   toks.get(8);
                    String radiusmgale =   toks.get(9);
                    String radiuswgale =   toks.get(10);
                    String mspd        =   toks.get(11);
                    String mdir        =   toks.get(12);
                    int yy = Integer.parseInt(year);
                    if(yy < 20) yy = 2000 + yy;
                    else if(yy > 50 && yy < 99 ) yy = 1900 + yy;
                    DateTime dtt = getDateTime(yy,
                                       Integer.parseInt(mon),
                                       Integer.parseInt(day),
                                       Integer.parseInt(hr));
                    double latitude  = Double.parseDouble(lat);
                    double longitude = Double.parseDouble(lon);
                    Real   altReal   = new Real(RealType.Altitude, 0);
                    int fhour = Integer.parseInt(fhr);

                    EarthLocation elt =
                        new EarthLocationLite(new Real(RealType.Latitude,
                            latitude), new Real(RealType.Longitude,
                                longitude), altReal);
                    List<Real> attributes = new ArrayList<Real>();

                    double     windspeed  = getDouble(maxwindsp);
                    double     pressure   = getDouble(minpress);
                    attributes.add(PARAM_MINPRESSURE.getReal(pressure));
                    attributes.add(PARAM_MAXWINDSPEED.getReal(windspeed));
                    attributes.add(
                        PARAM_RADIUSMODERATEGALE.getReal(
                            getDouble(radiusmgale)));
                    attributes.add(
                        PARAM_RADIUSWHOLEGALE.getReal(
                            getDouble(radiuswgale)));
                    attributes.add(
                        PARAM_MOVEDIRECTION.getReal(getDouble(mdir)));
                    attributes.add(PARAM_MOVESPEED.getReal(getDouble(mspd)));

                    StormTrackPoint stp = new StormTrackPoint(elt, dtt,
                                              fhour,
                                              attributes);

                   // System.out.println("fhour "+ fhour);
                    if(fhour == 0 && !trackWay.isObservation()) {
                        obsPts.add(stp);
                    }
                    if(fhour == 0 && pts.size() > 0 && !trackWay.isObservation()){
                        //System.out.println("fhours "+ pts.size());
                        DateTime trackStartTime = pts.get(0).getTime();
                        if(trackStartTime.getValue() < minTime) {
                                minTime = trackStartTime.getValue();
                                minDate = trackStartTime;
                        }
                        sTrack = new StormTrack(sInfo, trackWay, pts,
                                forecastParams);
                        stormTracks.add(sTrack);
                        pts      = new ArrayList();
                    }
                    pts.add(stp);
                }
                if(trackWay.isObservation()) {
                    DateTime trackStartTime = pts.get(0).getTime();
                    if(trackStartTime.getValue() < minTime) {
                            minTime = trackStartTime.getValue();
                            minDate = trackStartTime;
                    }
                    sTrack = new StormTrack(sInfo, trackWay, pts,
                            obsParams);
                    stormTracks.add(sTrack);
                    pts      = new ArrayList();
                }
                if (pts.size() > 0 && !trackWay.isObservation()) {
                    DateTime trackStartTime = pts.get(0).getTime();
                    if(trackStartTime.getValue() < minTime) {
                            minTime = trackStartTime.getValue();
                            minDate = trackStartTime;
                    }
                    sTrack = new StormTrack(sInfo, trackWay, pts,
                            forecastParams);
                    stormTracks.add(sTrack);
                    pts      = new ArrayList();
                }

            }
            /* last track */
            if (sInfo != null && minDate!=null) {
                    sInfo.setStartTime(minDate);
            }
            /* obs */
            if(!hasObservation && obsPts.size() > 0) {
                sTrack = new StormTrack(sInfo, DEFAULT_OBSERVATION_WAY, obsPts,
                            obsParams);
                stormTracks.add(sTrack);
                stormWays.put("Observation", DEFAULT_OBSERVATION_WAY);
            }

        } catch (Exception exc) {
            logException("Error initializing ATCF data", exc);
        } finally {
            decrOutstandingGetDataCalls();
        }

    }

    /**
     * _more_
     *
     * @param dstring _more_
     *
     * @return _more_
     */
    public double getDouble(String dstring) {
        if (dstring.equalsIgnoreCase("NaN") || dstring.equalsIgnoreCase("9999")) {
            return Double.NaN;
        } else {
            return Double.parseDouble(dstring);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormInfo> getStormInfos() {
        List<StormInfo> sInfos = new ArrayList();
        sInfos.addAll(stormInfos);
        return sInfos;
    }

    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param waysToUse _more_
     * @param observationWay _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */

    /** _more_ */
    private static final Way DEFAULT_OBSERVATION_WAY = new Way("Observation");
    private boolean hasObservation = false;
    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param waysToUse _more_
     * @param observationWay _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StormTrackCollection getTrackCollectionInner(StormInfo stormInfo,
            Hashtable<String, Boolean> waysToUse,
            Way observationWay) throws Exception {

        if (observationWay == null) {
            observationWay = DEFAULT_OBSERVATION_WAY;
        }


        // long                 t1              = System.currentTimeMillis();
        StormTrackCollection trackCollection = new StormTrackCollection();
        //        initializeStormData();
        List<Way> forecastWays = getForecastWays(stormInfo);


        for (Way forecastWay : forecastWays) {
            if ((waysToUse != null) && (waysToUse.size() > 0)
                    && (waysToUse.get(forecastWay.getId()) == null)) {
                continue;
            }
            List forecastTracks = getForecastTracks(stormInfo, forecastWay);
            if (forecastTracks.size() > 0) {
                trackCollection.addTrackList(forecastTracks);
            }
        }
        StormTrack obsTrack = getObservationTrack(stormInfo, observationWay);
        //                                         (Way) forecastWays.get(0));
        if (obsTrack != null) {
            List<StormTrack> tracks = trackCollection.getTracks();
            //  for (StormTrack stk : tracks) {
            //      addDistanceError(obsTrack, stk);
            //  }
            //long t2 = System.currentTimeMillis();
            //        System.err.println("time:" + (t2 - t1));
            trackCollection.addTrack(obsTrack);
        }
        return trackCollection;
    }


    /**
     * _more_
     *
     *
     *
     * @param stormInfo _more_
     * @param forecastWay _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    private List<StormTrack> getForecastTracks(StormInfo stormInfo,
            Way forecastWay) throws Exception {

        List<StormTrack> tracks = new ArrayList<StormTrack>();
        List<DateTime> startDates = getForecastTrackStartDates(stormInfo,
                                        forecastWay);

        int nstarts = startDates.size();
        for (int i = 0; i < nstarts; i++) {
            DateTime   dt = (DateTime) startDates.get(i);
            StormTrack tk = getForecastTrack(stormInfo, dt, forecastWay);
            if (tk != null) {
                int pn = tk.getTrackPoints().size();
                //Why > 1???
                if (pn > 1) {
                    tracks.add(tk);
                }
            }
        }
        return tracks;

    }


    /**
     * If d is a missing value return  NaN. Else return d
     * @param d is checked if not missing return same value
     * @param name _more_
     *
     * @return _more_
     */

    public double getValue(double d, String name) {
        if ((d == 9999) || (d == 999)) {
            return Double.NaN;
        }

        if (name.equalsIgnoreCase(PARAM_MAXWINDSPEED.getName())) {
            if ((d < 0) || (d > 60)) {
                return Double.NaN;
            }
        } else if (name.equalsIgnoreCase(PARAM_MINPRESSURE.getName())) {
            if ((d < 800) || (d > 1050)) {
                return Double.NaN;
            }
        } else if (name.equalsIgnoreCase(
                PARAM_RADIUSMODERATEGALE.getName())) {
            if ((d < 0) || (d > 900)) {
                return Double.NaN;
            }
        } else if (name.equalsIgnoreCase(PARAM_RADIUSWHOLEGALE.getName())) {
            if ((d < 0) || (d > 500)) {
                return Double.NaN;
            }
        } else if (name.equalsIgnoreCase(PARAM_MOVESPEED.getName())) {
            if ((d < 0) || (d > 55)) {
                return Double.NaN;
            }
        } else if (name.equalsIgnoreCase(PARAM_MOVEDIRECTION.getName())) {
            if ((d < 0) || (d > 360)) {
                return Double.NaN;
            }
        }

        return d;
    }


    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public double getLatLonValue(double d) {
        if ((d == 9999) || (d == 999)) {
            return Double.NaN;
        }
        return d;
    }


    /**
     * _more_
     *
     *
     *
     * @param stormInfo _more_
     * @param sTime _more_
     * @param forecastWay _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    private StormTrack getForecastTrack(StormInfo stormInfo, DateTime sTime,
                                        Way forecastWay) throws Exception {

        StormTrack track = null;

        Iterator   iter  = stormTracks.iterator();
        String     sid   = stormInfo.getStormId();
        String     sway  = forecastWay.getId();
        while (iter.hasNext()) {
            track = (StormTrack) iter.next();
            String   away = track.getWay().getId();
            String   id   = track.getStormInfo().getStormId();
            DateTime dt   = track.getStartTime();
            if (id.equalsIgnoreCase(sid) && (dt == sTime)
                    && sway.equalsIgnoreCase(away)) {
                return track;
            }

        }
        return null;
    }



    /**
     * _more_
     *
     * @param year _more_
     * @param month _more_
     * @param day _more_
     * @param hour _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private DateTime getDateTime(int year, int month, int day,
                                 int hour) throws Exception {
        GregorianCalendar convertCal =
            new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        convertCal.clear();
        convertCal.set(Calendar.YEAR, year);
        //The MONTH is 0 based. The incoming month is 1 based
        convertCal.set(Calendar.MONTH, month - 1);
        convertCal.set(Calendar.DAY_OF_MONTH, day);
        convertCal.set(Calendar.HOUR_OF_DAY, hour);
        return new DateTime(convertCal.getTime());
    }



    /**
     * _more_
     *
     *
     *
     * @param stormInfo _more_
     * @param way _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected List<DateTime> getForecastTrackStartDates(StormInfo stormInfo,
            Way way) throws Exception {

        Iterator       iter       = stormTracks.iterator();
        List<DateTime> startDates = new ArrayList<DateTime>();
        while (iter.hasNext()) {
            StormTrack track = (StormTrack) iter.next();
            if(!track.getWay().isObservation()) {
                DateTime   dt    = track.getStartTime();
                startDates.add(dt);
            }
        }
        return startDates;
    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param observationWay _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected StormTrack getObservationTrack(
            StormInfo stormInfo, Way observationWay) throws Exception {
        addWay(observationWay);
        //first get the obs from one specific way
        List<StormTrackPoint> obsTrackPoints =
            getObservationTrackPoints(stormInfo, observationWay);

        if ((obsTrackPoints == null) || (obsTrackPoints.size() == 0)) {
            return null;
        }


        return new StormTrack(stormInfo, addWay(Way.OBSERVATION),
                              obsTrackPoints, obsParams);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getIsObservationWayChangeable() {
        return true;
    }



    /**
     * _more_
     *
     *
     *
     * @param stormInfo _more_
     * @param wy _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected List<StormTrackPoint> getObservationTrackPoints(
            StormInfo stormInfo, Way wy) throws Exception {

        Iterator iter = stormTracks.iterator();
        String   sway = wy.getId();
        String   sid  = stormInfo.getStormId();

        while (iter.hasNext()) {
            StormTrack strack = (StormTrack) iter.next();
            String     away   = strack.getWay().getId();
            String     aid    = strack.getStormInfo().getStormId();
            if (away.equalsIgnoreCase(sway) && aid.equalsIgnoreCase(sid)) {
                return strack.getTrackPoints();
            }


        }

        return null;
    }

    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param wy _more_
     * @param before _more_
     * @param after _more_
     * @param pts _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List<StormTrackPoint> getObservationTrack(StormInfo stormInfo,
            Way wy, DateTime before, DateTime after,
            List pts) throws Exception {

        return null;
    }

    /**
     * _more_
     *
     * @param times _more_
     *
     * @return _more_
     */
    protected DateTime getStartTime(List times) {
        int      size  = times.size();
        DateTime dt    = (DateTime) times.get(0);
        int      idx   = 0;
        double   value = dt.getValue();
        for (int i = 1; i < size; i++) {
            dt = (DateTime) times.get(i);
            double dtValue = dt.getValue();
            if (dtValue < value) {
                value = dtValue;
                idx   = i;
            }
        }
        return (DateTime) times.get(idx);
    }



    /**
     * _more_
     *
     * @param sid _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected DateTime getStormStartTime(String sid) throws Exception {


        Iterator iter = stormTracks.iterator();

        while (iter.hasNext()) {
            StormTrack strack = (StormTrack) iter.next();
            String     aid    = strack.getStormInfo().getStormId();
            if (aid.equalsIgnoreCase(sid)) {
                return strack.getStartTime();
            }
        }
        return null;
    }

    /**
     * _more_
     *
     *
     *
     * @param stormInfo _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected List<Way> getForecastWays(
            StormInfo stormInfo) throws Exception {

        List<Way> ways = new ArrayList();

        Collection wc = stormWays.values();
        Iterator  iter = wc.iterator();

        while (iter.hasNext()) {
            Way way = (Way)iter.next();
            if(!way.isObservation())
                ways.add(way);
        }

        //        System.err.println ("ways:" + forecastWays);
        return ways;

    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String             sid = "0623";
        STIStormDataSource s   = null;
        try {
            s = new STIStormDataSource();
        } catch (Exception exc) {
            System.err.println("err:" + exc);
            exc.printStackTrace();
        }
        s.initAfter();
        List      sInfoList = s.getStormInfos();
        StormInfo sInfo     = (StormInfo) sInfoList.get(0);
        sInfo = s.getStormInfo(sid);
        String               sd             = sInfo.getStormId();
        StormTrackCollection cls = s.getTrackCollection(sInfo, null, null);
        StormTrack           obsTrack       = cls.getObsTrack();
        List                 trackPointList = obsTrack.getTrackPoints();
        List                 trackPointTime = obsTrack.getTrackTimes();
        List                 ways           = cls.getWayList();
        Map                  mp             = cls.getWayToStartDatesHashMap();
        Map                  mp1            = cls.getWayToTracksHashMap();


        System.err.println("test:");

    }





}

