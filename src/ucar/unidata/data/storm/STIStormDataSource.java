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

import java.rmi.RemoteException;

import java.sql.*;

import java.util.*;


import java.util.Date;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 4:58:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class STIStormDataSource extends StormDataSource {

    /** _more_ */
    private static final Way DEFAULT_OBSERVATION_WAY = new Way("babj");

    /* Use this for mysql:     */

    /** _more_ */
    private static final String DEFAULT_URL =
        "jdbc:mysql://localhost:3306/typhoon?zeroDateTimeBehavior=convertToNull&user=yuanho&password=password";


    //    private static final String DEFAULT_URL =
    //        "jdbc:mysql://localhost:3306/typhoon?zeroDateTimeBehavior=convertToNull&user=yuanho&password=password";



    /** _more_ */
    private static final String DEFAULT_DERBY_URL =
        "jdbc:derby:test;create=true";

    /** _more_ */
    private static final String COL_DERBY_HOUR = "hh";

    /** _more_ */
    private static final String COL_DERBY_YEAR = "yyyy";





    /**
     * _more_
     *
     * @return _more_
     */
    private boolean useDerby() {
        if ((dbUrl != null) && (dbUrl.indexOf("derby") >= 0)) {
            return true;
        }
        return false;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return "sti";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getColHour() {
        if (useDerby()) {
            return COL_DERBY_HOUR;
        }
        return COL_TYPHOON_HOUR;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private String getColYear() {
        if (useDerby()) {
            return COL_DERBY_YEAR;
        }
        return COL_TYPHOON_YEAR;
    }


    /** _more_ */
    public static StormParam PARAM_MAXWINDSPEED;

    /** _more_ */
    public static StormParam PARAM_RADIUSMODERATEGALE;

    /** _more_ */
    public static StormParam PARAM_RADIUSWHOLEGALE;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY10RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY20RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY30RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY40RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY50RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY60RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY70RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY80RADIUS;

    /** _more_ */
    public static StormParam PARAM_PROBABILITY90RADIUS;

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
    private static final String ZEROHOUR = "0";

    /** _more_ */
    private static final String TABLE_TRACK = "typhoon";



    /** _more_ */
    private static final String COL_TYPHOON_YEAR = "year";

    /** _more_ */
    private static final String COL_TYPHOON_HOUR = "hour";
    /**/


    /** _more_ */
    private static final String COL_TYPHOON_STORMID = "nno";

    /** _more_ */
    private static final String COL_TYPHOON_TIME = "time";

    /** _more_ */
    private static final String COL_TYPHOON_LATITUDE = "lat";

    /** _more_ */
    private static final String COL_TYPHOON_LONGITUDE = "lon";


    /** _more_ */
    private static final String COL_TYPHOON_MONTH = "mon";

    /** _more_ */
    private static final String COL_TYPHOON_DAY = "day";


    /** _more_ */
    private static final String COL_TYPHOON_FHOUR = "fhour";

    /** _more_ */
    private static final String COL_TYPHOON_WAY = "way";

    /** _more_ */
    private static final String COL_TYPHOON_PRESSURE = "pressure";

    /** _more_ */
    private static final String COL_TYPHOON_WINDSPEED = "wind";

    /** _more_ */
    private static final String COL_TYPHOON_RADIUSMG = "xx1";

    /** _more_ */
    private static final String COL_TYPHOON_RADIUSWG = "xx2";

    /** _more_ */
    private static final String COL_TYPHOON_MOVEDIR = "xx3";

    /** _more_ */
    private static final String COL_TYPHOON_MOVESPEED = "xx4";



    /** _more_ */
    private static final String TABLE_PROBILITY = "probility";



    /** _more_ */
    private static final String COL_PROBILITY_WAYNAME = "wayname";

    /** _more_ */
    private static final String COL_PROBILITY_FHOUR = "fhour";

    /** _more_ */
    private static final String COL_PROBILITY_P10 = "p10";

    /** _more_ */
    private static final String COL_PROBILITY_P20 = "p20";

    /** _more_ */
    private static final String COL_PROBILITY_P30 = "p30";

    /** _more_ */
    private static final String COL_PROBILITY_P40 = "p40";

    /** _more_ */
    private static final String COL_PROBILITY_P50 = "p50";

    /** _more_ */
    private static final String COL_PROBILITY_P60 = "p60";

    /** _more_ */
    private static final String COL_PROBILITY_P70 = "p70";

    /** _more_ */
    private static final String COL_PROBILITY_P80 = "p80";

    /** _more_ */
    private static final String COL_PROBILITY_P90 = "p90";

    /** _more_ */
    private static final String COL_PROBILITY_P100 = "p100";

    /** _more_ */
    private static final String COL_DISTANCE_ERROR = "error";

    /** _more_ */
    private static final String COL_PROBILITY_REMARK = "remark";


    /** _more_ */
    private String dbUrl;


    /** the db connection */
    private Connection connection;


    /** _more_ */
    private String fromDate = "-1 year";

    /** _more_ */
    private String toDate = "now";


    /** the stormInfo and track */
    private List<StormInfo> stormInfos;

    /** _more_ */
    private HashMap<String, float[]> wayfhourToRadius;



    /**
     * constructor of sti storm data source
     *
     *
     * @throws Exception _more_
     */


    public STIStormDataSource() throws Exception {}







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

            PARAM_PROBABILITY10RADIUS =
                new StormParam(makeRealType("probabilityradius10",
                                            "Probability_10%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY20RADIUS =
                new StormParam(makeRealType("probabilityradius20",
                                            "Probability_20%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY30RADIUS =
                new StormParam(makeRealType("probabilityradius30",
                                            "Probability_30%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY40RADIUS =
                new StormParam(makeRealType("probabilityradius40",
                                            "Probability_40%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY50RADIUS =
                new StormParam(makeRealType("probabilityradius50",
                                            "Probability_50%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY60RADIUS =
                new StormParam(makeRealType("probabilityradius60",
                                            "Probability_60%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY70RADIUS =
                new StormParam(makeRealType("probabilityradius70",
                                            "Probability_70%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY80RADIUS =
                new StormParam(makeRealType("probabilityradius80",
                                            "Probability_80%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY90RADIUS =
                new StormParam(makeRealType("probabilityradius90",
                                            "Probability_90%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_PROBABILITY100RADIUS =
                new StormParam(makeRealType("probabilityradius100",
                                            "Probability_100%_Radius",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
            PARAM_DISTANCE_ERROR =
                new StormParam(makeRealType("meanDistanceError",
                                            "Mean_Distance_Error",
                                            DataUtil.parseUnit("km")), true,
                                                false, false);
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
            PARAM_RADIUSWHOLEGALE, PARAM_MOVEDIRECTION, PARAM_MOVESPEED
        };

        forecastParams = new StormParam[] {
            PARAM_MAXWINDSPEED, PARAM_MINPRESSURE, PARAM_RADIUSMODERATEGALE,
            PARAM_RADIUSWHOLEGALE, PARAM_MOVEDIRECTION, PARAM_MOVESPEED,  //PARAM_DISTANCEERROR,
            PARAM_PROBABILITY10RADIUS, PARAM_PROBABILITY20RADIUS,
            PARAM_PROBABILITY30RADIUS, PARAM_PROBABILITY40RADIUS,
            PARAM_PROBABILITY50RADIUS, PARAM_PROBABILITY60RADIUS,
            PARAM_PROBABILITY70RADIUS, PARAM_PROBABILITY80RADIUS,
            PARAM_PROBABILITY90RADIUS, PARAM_PROBABILITY100RADIUS,
            PARAM_DISTANCE_ERROR
        };
    }





    /**
     * _more_
     *
     * @param descriptor _more_
     * @param url _more_
     * @param properties _more_
     *
     * @throws Exception _more_
     */
    public STIStormDataSource(DataSourceDescriptor descriptor, String url,
                              Hashtable properties)
            throws Exception {
        super(descriptor, "STI Storm Data", "STI Storm Data", properties);
        if ((url != null) && url.trim().equals("test")) {
            url = DEFAULT_DERBY_URL;
        }
        if ((url == null) || url.trim().equalsIgnoreCase("default")
                || (url.trim().length() == 0)) {
            url = (useDerby()
                   ? DEFAULT_DERBY_URL
                   : DEFAULT_URL);
        }
        dbUrl = url;
    }



    /**
     * _more_
     */
    protected void initializeStormData() {
        try {
            initParams();
            File userDir =
                getDataContext().getIdv().getObjectStore().getUserDirectory();
            String derbyDir = IOUtil.joinDir(userDir, "derbydb");
            IOUtil.makeDirRecursive(new File(derbyDir));
            System.setProperty("derby.system.home", derbyDir);

            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            Class.forName("com.mysql.jdbc.Driver");
            if ( !initConnection()) {
                setInError(true, true,
                           "Unable to initialize database connection:"
                           + dbUrl);
            } else {
                stormInfos = getAllStormInfos();
            }
        } catch (Exception exc) {
            logException("Error initializing STI database: " + dbUrl, exc);
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


    public StormTrackCollection getTrackCollectionInner(StormInfo stormInfo,
            Hashtable<String, Boolean> waysToUse, Way observationWay)
            throws Exception {
        if (observationWay == null) {
            observationWay = DEFAULT_OBSERVATION_WAY;
        }

        long                 t1              = System.currentTimeMillis();
        StormTrackCollection trackCollection = new StormTrackCollection();
        List<Way>            forecastWays    = getForecastWays(stormInfo);

        getWayProbabilityRadius();
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
            long t2 = System.currentTimeMillis();
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
            Way forecastWay)
            throws Exception {

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
                                        Way forecastWay)
            throws Exception {

        //        if(true) return getForecastTrackX(stormInfo, sTime, forecastWay);
        String columns = SqlUtil.comma(new String[] {
            getColYear(), COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour(),
            COL_TYPHOON_FHOUR, COL_TYPHOON_LATITUDE, COL_TYPHOON_LONGITUDE,
            COL_TYPHOON_WINDSPEED, COL_TYPHOON_PRESSURE, COL_TYPHOON_RADIUSMG,
            COL_TYPHOON_RADIUSWG, COL_TYPHOON_MOVEDIR, COL_TYPHOON_MOVESPEED
        });


        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_TYPHOON_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_TYPHOON_WAY,
                                 SqlUtil.quote(forecastWay.getId())));

        addDateSelection(sTime, whereList);

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        query = query + " order by  "
                + SqlUtil.comma(new String[] { getColYear(),
                COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour(),
                COL_TYPHOON_FHOUR });
        //        System.err.println (query);
        Statement             statement = evaluate(query);
        SqlUtil.Iterator      iter      = SqlUtil.getIterator(statement);
        ResultSet             results;
        double                radius  = 0;
        List<StormTrackPoint> pts     = new ArrayList();

        Real                  altReal = new Real(RealType.Altitude, 0);
        while ((results = iter.getNext()) != null) {
            //                System.err.println ("row " + cnt);
            List<Real> attrs    = new ArrayList<Real>();
            int        col      = 1;
            int        year     = results.getInt(col++);
            int        month    = results.getInt(col++);
            int        day      = results.getInt(col++);
            int        hour     = results.getInt(col++);
            int        fhour    = results.getInt(col++);

            double     latitude =
                getLatLonValue(results.getDouble(col++));
            if ((latitude > 90) || (latitude < -90)) {
                continue;
            }
            double longitude = getLatLonValue(results.getDouble(col++));
            if ((longitude > 360) || (longitude < -180)) {
                continue;
            }
            attrs.add(
                      PARAM_MAXWINDSPEED.getReal(
                                                 getValue(
                                                          results.getDouble(col++),
                                                          PARAM_MAXWINDSPEED.getName())));
            attrs.add(
                      PARAM_MINPRESSURE.getReal(
                                                getValue(
                                                         results.getDouble(col++),
                                                         PARAM_MINPRESSURE.getName())));
            attrs.add(
                      PARAM_RADIUSMODERATEGALE.getReal(
                                                       getValue(
                                                                results.getDouble(col++),
                                                                PARAM_RADIUSMODERATEGALE.getName())));
            attrs.add(
                      PARAM_RADIUSWHOLEGALE.getReal(
                                                    getValue(
                                                             results.getDouble(col++),
                                                             PARAM_RADIUSWHOLEGALE.getName())));
            attrs.add(
                      PARAM_MOVEDIRECTION.getReal(
                                                  getValue(
                                                           results.getDouble(col++),
                                                           PARAM_MOVEDIRECTION.getName())));
            attrs.add(
                      PARAM_MOVESPEED.getReal(
                                              getValue(
                                                       results.getDouble(col++),
                                                       PARAM_MOVESPEED.getName())));
            float[] radiuses = getProbabilityRadius(forecastWay, fhour);
            DateTime dttm = getDateTime(year, month, day, hour + fhour);
            EarthLocation elt =
                new EarthLocationLite(new Real(RealType.Latitude,
                                               latitude), new Real(RealType.Longitude, longitude),
                                      altReal);
            if (true) {  //radiuses != null) {
                //radius = fhour * 50.0f / 24.0f;
                addProbabilityRadiusAttrs(attrs, radiuses);
            }
            StormTrackPoint stp = new StormTrackPoint(elt, dttm, fhour,
                                                      attrs);
            if ( !elt.isMissing()) {
                pts.add(stp);
            }
        }

        if (pts.size() == 0) {
            //We should never be here
            System.err.println("found no track data time=" + sTime
                               + " from query:" + SqlUtil.makeAnd(whereList));
        }
        if (pts.size() > 0) {
            return new StormTrack(stormInfo, forecastWay, pts,
                                  forecastParams);
        } else {
            return null;
        }


    }

    /**
     * _more_
     *
     * @param way _more_
     * @param forecastHour _more_
     *
     * @return _more_
     */
    private float[] getProbabilityRadius(Way way, int forecastHour) {
        String key = way.getId().toUpperCase() + forecastHour;
        //        System.out.println("get:" + key + " " +(wayfhourToRadius.get(key)!=null));
        return wayfhourToRadius.get(key);
    }

    /**
     * _more_
     *
     * @param way _more_
     * @param forecastHour _more_
     * @param radiuses _more_
     */
    private void putProbabilityRadius(Way way, int forecastHour,
                                      float[] radiuses) {
        String key = way.getId().toUpperCase() + forecastHour;
        //        System.out.println("put:" + key);
        wayfhourToRadius.put(key, radiuses);
    }


    /**
     * _more_
     *
     * @param attrs _more_
     * @param radiuses _more_
     *
     * @throws Exception _more_
     */
    private void addProbabilityRadiusAttrs(List<Real> attrs, float[] radiuses)
            throws Exception {
        if (radiuses != null) {
            attrs.add(PARAM_PROBABILITY10RADIUS.getReal(radiuses[0]));
            attrs.add(PARAM_PROBABILITY20RADIUS.getReal(radiuses[1]));
            attrs.add(PARAM_PROBABILITY30RADIUS.getReal(radiuses[2]));
            attrs.add(PARAM_PROBABILITY40RADIUS.getReal(radiuses[3]));
            attrs.add(PARAM_PROBABILITY50RADIUS.getReal(radiuses[4]));
            attrs.add(PARAM_PROBABILITY60RADIUS.getReal(radiuses[5]));
            attrs.add(PARAM_PROBABILITY70RADIUS.getReal(radiuses[6]));
            attrs.add(PARAM_PROBABILITY80RADIUS.getReal(radiuses[7]));
            attrs.add(PARAM_PROBABILITY90RADIUS.getReal(radiuses[8]));
            attrs.add(PARAM_PROBABILITY100RADIUS.getReal(radiuses[9]));
            attrs.add(
                PARAM_DISTANCE_ERROR.getReal(getLatLonValue(radiuses[10])));
        } else {
            attrs.add(PARAM_PROBABILITY10RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY20RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY30RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY40RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY50RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY60RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY70RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY80RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY90RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_PROBABILITY100RADIUS.getReal(Float.NaN));
            attrs.add(PARAM_DISTANCE_ERROR.getReal(Float.NaN));

        }
    }

    /**
     * _more_
     *
     * @param sTime _more_
     * @param whereList _more_
     *
     * @throws VisADException _more_
     */
    private void addDateSelection(DateTime sTime, List whereList)
            throws VisADException {
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        cal.setTime(ucar.visad.Util.makeDate(sTime));
        int yy = cal.get(Calendar.YEAR);
        //The MONTH is 0 based. The db month is 1 based
        int mm = cal.get(Calendar.MONTH) + 1;
        int dd = cal.get(Calendar.DAY_OF_MONTH);
        int hh = cal.get(Calendar.HOUR_OF_DAY);
        whereList.add(SqlUtil.eq(getColYear(), Integer.toString(yy)));
        whereList.add(SqlUtil.eq(COL_TYPHOON_MONTH, Integer.toString(mm)));
        whereList.add(SqlUtil.eq(COL_TYPHOON_DAY, Integer.toString(dd)));
        whereList.add(SqlUtil.eq(getColHour(), Integer.toString(hh)));
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
    private DateTime getDateTime(int year, int month, int day, int hour)
            throws Exception {
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
            Way way)
            throws Exception {

        String columns = SqlUtil.comma(new String[] { getColYear(),
                COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour() });

        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_TYPHOON_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_TYPHOON_FHOUR, ZEROHOUR));
        whereList.add(SqlUtil.eq(COL_TYPHOON_WAY,
                                 SqlUtil.quote(way.getId())));

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        query = query + " order by  "
                + SqlUtil.comma(new String[] { getColYear(),
                COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour() });
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;
        List<DateTime>   startDates = new ArrayList<DateTime>();
        while ((results = iter.getNext()) != null) {
            int col   = 1;
            int year  = results.getInt(col++);
            int month = results.getInt(col++);
            int day   = results.getInt(col++);
            int hour  = results.getInt(col++);
            startDates.add(getDateTime(year, month, day, hour));
        }
        return startDates;
    }

    /**
     * _more_
     *
     *
     * @throws Exception _more_
     */
    protected void getWayProbabilityRadius() throws Exception {

        String columns = SqlUtil.comma(new String[] {
            COL_PROBILITY_WAYNAME, COL_PROBILITY_FHOUR, COL_PROBILITY_P10,
            COL_PROBILITY_P20, COL_PROBILITY_P30, COL_PROBILITY_P40,
            COL_PROBILITY_P50, COL_PROBILITY_P60, COL_PROBILITY_P70,
            COL_PROBILITY_P80, COL_PROBILITY_P90, COL_PROBILITY_P100,
            COL_DISTANCE_ERROR
        });

        List whereList = new ArrayList();

        String query = SqlUtil.makeSelect(columns,
                                          Misc.newList(TABLE_PROBILITY),
                                          SqlUtil.makeAnd(whereList));
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;
        wayfhourToRadius = new HashMap();
        while ((results = iter.getNext()) != null) {
            float[] wp      = new float[11];
            int     col     = 1;
            String  wayName = results.getString(col++);
            int     fhour   = results.getInt(col++);
            wp[0]  = results.getFloat(col++);
            wp[1]  = results.getFloat(col++);
            wp[2]  = results.getFloat(col++);
            wp[3]  = results.getFloat(col++);
            wp[4]  = results.getFloat(col++);
            wp[5]  = results.getFloat(col++);
            wp[6]  = results.getFloat(col++);
            wp[7]  = results.getFloat(col++);
            wp[8]  = results.getFloat(col++);
            wp[9]  = results.getFloat(col++);
            wp[10] = results.getFloat(col++);
            putProbabilityRadius(new Way(wayName), fhour, wp);
        }
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
    protected StormTrack getObservationTrack(StormInfo stormInfo,
                                             Way observationWay)
            throws Exception {
        addWay(observationWay);
        //first get the obs from one specific way
        List<StormTrackPoint> obsTrackPoints =
            getObservationTrackPoints(stormInfo, observationWay);

        if (obsTrackPoints.size() == 0) {
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
     * @return _more_
     */
    public Way getDefaultObservationWay() {
        return DEFAULT_OBSERVATION_WAY;
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
            StormInfo stormInfo, Way wy)
            throws Exception {
        String columns = SqlUtil.comma(new String[] {
            getColYear(), COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour(),
            COL_TYPHOON_LATITUDE, COL_TYPHOON_LONGITUDE,
            COL_TYPHOON_WINDSPEED, COL_TYPHOON_PRESSURE, COL_TYPHOON_RADIUSMG,
            COL_TYPHOON_RADIUSWG, COL_TYPHOON_MOVEDIR, COL_TYPHOON_MOVESPEED,
            COL_TYPHOON_WAY
        });

        List whereList = new ArrayList();

        whereList.add(SqlUtil.eq(COL_TYPHOON_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_TYPHOON_FHOUR, ZEROHOUR));
        whereList.add(SqlUtil.eq(COL_TYPHOON_WAY, SqlUtil.quote(wy.getId())));

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        query = query + " order by  "
                + SqlUtil.comma(new String[] { getColYear(),
                COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour() });
        //        System.err.println (query);
        Statement             statement = evaluate(query);
        SqlUtil.Iterator      iter      = SqlUtil.getIterator(statement);
        ResultSet             results;

        List<StormTrackPoint> obsPts = new ArrayList();
        //Hashtable seenDate = new Hashtable();
        Real altReal = new Real(RealType.Altitude, 0);

        while ((results = iter.getNext()) != null) {
            List<Real> attrs    = new ArrayList();
            int        col      = 1;
            int        year     = results.getInt(col++);
            int        month    = results.getInt(col++);
            int        day      = results.getInt(col++);
            int        hour     = results.getInt(col++);
            double     latitude =
                getLatLonValue(results.getDouble(col++));
            if ((latitude > 90) || (latitude < -90)) {
                continue;
            }
            double longitude = getLatLonValue(results.getDouble(col++));
            if ((longitude > 360) || (longitude < -180)) {
                continue;
            }
            attrs.add(
                      PARAM_MAXWINDSPEED.getReal(
                                                 getValue(
                                                          results.getDouble(col++),
                                                          PARAM_MAXWINDSPEED.getName())));
            attrs.add(
                      PARAM_MINPRESSURE.getReal(
                                                getValue(
                                                         results.getDouble(col++),
                                                         PARAM_MINPRESSURE.getName())));
            attrs.add(
                      PARAM_RADIUSMODERATEGALE.getReal(
                                                       getValue(
                                                                results.getDouble(col++),
                                                                PARAM_RADIUSMODERATEGALE.getName())));
            attrs.add(
                      PARAM_RADIUSWHOLEGALE.getReal(
                                                    getValue(
                                                             results.getDouble(col++),
                                                             PARAM_RADIUSWHOLEGALE.getName())));
            attrs.add(
                      PARAM_MOVEDIRECTION.getReal(
                                                  getValue(
                                                           results.getDouble(col++),
                                                           PARAM_MOVEDIRECTION.getName())));
            attrs.add(
                      PARAM_MOVESPEED.getReal(
                                              getValue(
                                                       results.getDouble(col++),
                                                       PARAM_MOVESPEED.getName())));

            EarthLocation elt =
                new EarthLocationLite(new Real(RealType.Latitude,
                                               latitude), new Real(RealType.Longitude, longitude),
                                      altReal);

            DateTime date = getDateTime(year, month, day, hour);
            String   key  = "" + latitude + " " + longitude;
            // if(seenDate.get(date)!=null) {
            //     if(!seenDate.get(date).equals(key)) {
            //                        System.err.println ("seen: " + date + " " + seenDate.get(date) + " != " + key);
            //    }
            //    continue;
            // }
            //                                seenDate.put(date,date);
            // seenDate.put(date,key);
            StormTrackPoint stp = new StormTrackPoint(elt, date, 0,
                                                      attrs);
            obsPts.add(stp);
        }

        return obsPts;
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
            Way wy, DateTime before, DateTime after, List pts)
            throws Exception {

        String columns = SqlUtil.comma(new String[] {
            getColYear(), COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour(),
            COL_TYPHOON_LATITUDE, COL_TYPHOON_LONGITUDE,
            COL_TYPHOON_WINDSPEED, COL_TYPHOON_PRESSURE, COL_TYPHOON_RADIUSMG,
            COL_TYPHOON_RADIUSWG, COL_TYPHOON_MOVEDIR, COL_TYPHOON_MOVESPEED,
            COL_TYPHOON_WAY
        });

        List whereList = new ArrayList();

        whereList.add(SqlUtil.eq(COL_TYPHOON_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_TYPHOON_FHOUR, ZEROHOUR));
        whereList.add(SqlUtil.eq(COL_TYPHOON_WAY, SqlUtil.quote(wy.getId())));

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        query = query + " order by  "
                + SqlUtil.comma(new String[] { getColYear(),
                COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour() });
        //        System.err.println (query);
        Statement             statement = evaluate(query);
        SqlUtil.Iterator      iter      = SqlUtil.getIterator(statement);
        ResultSet             results;

        List<StormTrackPoint> obsPts  = new ArrayList();
        List<StormTrackPoint> obsPts1 = new ArrayList();
        List<StormTrackPoint> obsPts2 = new ArrayList();
        Real                  altReal = new Real(RealType.Altitude, 0);

        while ((results = iter.getNext()) != null) {
                List<Real> attrs    = new ArrayList();
                int        col      = 1;
                int        year     = results.getInt(col++);
                int        month    = results.getInt(col++);
                int        day      = results.getInt(col++);
                int        hour     = results.getInt(col++);
                double     latitude =
                    getLatLonValue(results.getDouble(col++));
                if ((latitude > 90) || (latitude < -90)) {
                    continue;
                }
                double longitude = getLatLonValue(results.getDouble(col++));
                if ((longitude > 360) || (longitude < -180)) {
                    continue;
                }

                attrs.add(
                    PARAM_MAXWINDSPEED.getReal(
                        getValue(
                            results.getDouble(col++),
                            PARAM_MAXWINDSPEED.getName())));
                attrs.add(
                    PARAM_MINPRESSURE.getReal(
                        getValue(
                            results.getDouble(col++),
                            PARAM_MINPRESSURE.getName())));
                attrs.add(
                    PARAM_RADIUSMODERATEGALE.getReal(
                        getValue(
                            results.getDouble(col++),
                            PARAM_RADIUSMODERATEGALE.getName())));
                attrs.add(
                    PARAM_RADIUSWHOLEGALE.getReal(
                        getValue(
                            results.getDouble(col++),
                            PARAM_RADIUSWHOLEGALE.getName())));
                attrs.add(
                    PARAM_MOVEDIRECTION.getReal(
                        getValue(
                            results.getDouble(col++),
                            PARAM_MOVEDIRECTION.getName())));
                attrs.add(
                    PARAM_MOVESPEED.getReal(
                        getValue(
                            results.getDouble(col++),
                            PARAM_MOVESPEED.getName())));

                EarthLocation elt =
                    new EarthLocationLite(new Real(RealType.Latitude,
                        latitude), new Real(RealType.Longitude, longitude),
                                   altReal);

                DateTime date = getDateTime(year, month, day, hour);

                if (date.getValue() < before.getValue()) {
                    StormTrackPoint stp = new StormTrackPoint(elt, date, 0,
                                              attrs);
                    obsPts1.add(stp);
                }

                if (date.getValue() > after.getValue()) {
                    StormTrackPoint stp = new StormTrackPoint(elt, date, 0,
                                              attrs);
                    obsPts2.add(stp);
                }

        }

        if (obsPts1.size() > 0) {
            obsPts.addAll(obsPts1);
        }

        obsPts.addAll(pts);

        if (obsPts2.size() > 0) {
            obsPts.addAll(obsPts2);
        }

        return obsPts;

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
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    private List<StormInfo> getAllStormInfos() throws Exception {
        String columns = SqlUtil.distinct(COL_TYPHOON_STORMID);
        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK));
        //        System.err.println (query);
        //        System.err.println(query);
        SqlUtil.Iterator iter = SqlUtil.getIterator(evaluate(query));
        ResultSet        results;
        List<StormInfo>  stormInfos = new ArrayList<StormInfo>();
        while ((results = iter.getNext()) != null) {
            String   id        = results.getString(1);
            DateTime startTime = getStormStartTime(id);
            //                System.err.println(id + " " + startTime);
            StormInfo sinfo = new StormInfo(id, startTime);
            stormInfos.add(sinfo);
        }
        return stormInfos;
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected DateTime getStormStartTime(String id) throws Exception {
        String columns = SqlUtil.comma(new String[] { getColYear(),
                COL_TYPHOON_MONTH, COL_TYPHOON_DAY, getColHour() });

        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_TYPHOON_STORMID, SqlUtil.quote(id)));
        whereList.add(SqlUtil.eq(COL_TYPHOON_FHOUR, ZEROHOUR));
        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        query = query + " order by  " + columns;
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;
        while ((results = iter.getNext()) != null) {
            int col   = 1;
            int year  = results.getInt(col++);
            int month = results.getInt(col++);
            int day   = results.getInt(col++);
            int hour  = results.getInt(col++);
            statement.close();
            //Just get the first one since we sorted the results with the order by
            return getDateTime(year, month, day, hour);
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
    protected List<Way> getForecastWays(StormInfo stormInfo)
            throws Exception {

        String columns   = SqlUtil.distinct(COL_TYPHOON_WAY);

        List   whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_TYPHOON_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;

        List<Way>        forecastWays = new ArrayList<Way>();

        //TODO: How do we handle no data???
        while ((results = iter.getNext()) != null) {
            Way way = new Way(results.getString(1));
            addWay(way);
            forecastWays.add(way);
        }

        //        System.err.println ("ways:" + forecastWays);
        return forecastWays;

    }

    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     *
     * @throws SQLException _more_
     */
    private Statement evaluate(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        stmt.execute(sql);
        return stmt;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Connection getConnection() {
        if (connection != null) {
            return connection;
        }
        //        String url = getFilePath();
        //Just hard code the jdbc url
        String url = dbUrl;
        //We don't need to do this for derby.
        /*
        if ((getUserName() == null) || (getUserName().trim().length() == 0)) {
            if (url.indexOf("?") >= 0) {
                int idx = url.indexOf("?");
                List<String> args =
                    (List<String>) StringUtil.split(url.substring(idx + 1),
                        "&", true, true);
                url = url.substring(0, idx);
                for (String tok : args) {
                    List<String> subtoks =
                        (List<String>) StringUtil.split(tok, "=", true, true);
                    if (subtoks.size() != 2) {
                        continue;
                    }
                    String name  = subtoks.get(0);
                    String value = subtoks.get(1);
                    if (name.equals("user")) {
                        setUserName(value);
                    } else if (name.equals("password")) {
                        setPassword(value);
                    }
                }
            }
            }*/

        int cnt = 0;
        while (true) {
            String userName = getUserName();
            String password = getPassword();
            if (userName == null) {
                userName = "";
            }
            if (password == null) {
                password = "";
            }
            //            userName = "jeff";
            //            password = "mypassword";
            try {
                //System.err.println(url);
                if (useDerby()) {
                    connection = DriverManager.getConnection(url);
                } else {
                    if ((url.indexOf("user") > 0)
                            && (url.indexOf("password") > 0)) {
                        connection = DriverManager.getConnection(url);
                    } else {
                        connection = DriverManager.getConnection(url,
                                userName, password);
                    }
                }

                return connection;
            } catch (Exception sqe) {
                //  System.out.println(sqe);
                String msg = sqe.toString();
                if ((msg.indexOf("Access denied") >= 0) || (msg.indexOf(
                        "role \"" + userName
                        + "\" does not exist") >= 0) || (msg.indexOf(
                            "user name specified") >= 0)) {
                    String label;
                    if (cnt == 0) {
                        label =
                            "<html>The database requires a login.<br>Please enter a user name and password:</html>";
                    } else {
                        label =
                            "<html>Incorrect username/password. Please try again.</html>";
                    }
                    if ( !showPasswordDialog("Database Login", label)) {
                        return null;
                    }
                    cnt++;
                    continue;
                }
                throw new BadDataException("Unable to connect to database",
                                           sqe);
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean initConnection() throws Exception {
        if (getConnection() == null) {
            return false;
        }

        try {
            //Create the dummy database
            Connection connection = getConnection();
            Statement  stmt       = connection.createStatement();
            //Drop the table  - ignore any errors
            //          SqlUtil.loadSql("drop table " + TABLE_TRACK, stmt, false);

            if (useDerby()) {
                //Load in the test data
                try {
                    stmt.execute("select count(*) from typhoon");
                    System.err.println("Derby DB OK");
                } catch (Exception exc) {
                    System.err.println("exc;" + exc);
                    System.err.println("Creating test database");
                    String initSql =
                        IOUtil.readContents(
                            "/ucar/unidata/data/storm/testdb.sql",
                            getClass());

                    connection.setAutoCommit(false);
                    SqlUtil.loadSql(initSql, stmt, false);
                    connection.commit();
                    connection.setAutoCommit(true);
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            return false;
        }
        return true;
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


    /**
     * Set the DbUrl property.
     *
     * @param value The new value for DbUrl
     */
    public void setDbUrl(String value) {
        dbUrl = value;
    }

    /**
     * Get the DbUrl property.
     *
     * @return The DbUrl
     */
    public String getDbUrl() {
        return dbUrl;
    }




}

