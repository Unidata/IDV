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

import ucar.visad.display.*;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.EarthLocationTuple;

import java.io.*;

import java.rmi.RemoteException;

import java.sql.*;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 4:58:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class STIStormDataSource extends StormDataSource {



    /* Use this for mysql:
       private static final String DEFAULT_URL =  "jdbc:mysql://localhost:3306/typhoon?zeroDateTimeBehavior=convertToNull&user=jeff&password=mypassword";
       private static final String COL_YEAR = "year";
       private static final String COL_HOUR = "hour";
    */

    //Use this for java derby:
    private static final String DEFAULT_URL = "jdbc:derby:test;create=true";
    private static final String COL_HOUR = "hh";
    private static final String COL_YEAR = "yyyy";


    /** _more_          */
    private static float MISSING = 9999.0f;

    /** _more_          */
    private static final String ZEROHOUR = "0";

    /** _more_ */
    private static final String TABLE_TRACK = "typhoon";

    /** _more_ */
    private static final String COL_STORMID = "nno";

    /** _more_ */
    private static final String COL_TIME = "time";

    /** _more_ */
    private static final String COL_LATITUDE = "lat";

    /** _more_ */
    private static final String COL_LONGITUDE = "lon";



    /** _more_ */
    private static final String COL_MONTH = "mon";

    /** _more_ */
    private static final String COL_DAY = "day";


    /** _more_ */
    private static final String COL_FHOUR = "fhour";

    /** _more_ */
    private static final String COL_WAY = "way";

    /** _more_ */
    private static final String COL_PRESSURE = "pressure";

    /** _more_ */
    private static final String COL_WINDSPEED = "wind";

    /** _more_ */
    private static final String COL_RADIUSMG = "xx1";

    /** _more_ */
    private static final String COL_RADIUSWG = "xx2";

    /** _more_ */
    private static final String COL_MOVEDIR = "xx3";

    /** _more_ */
    private static final String COL_MOVESPEED = "xx4";


    /** _more_          */
    private String dbUrl;


    /** the db connection */
    private Connection connection;


    /** _more_ */
    private String fromDate = "-1 year";

    /** _more_ */
    private String toDate = "now";

    /** the stormInfo and track */

    private List<StormInfo> stormInfos;





    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */


    public STIStormDataSource() throws Exception {}




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
        if ((url == null) || url.trim().equalsIgnoreCase("default")
                || (url.trim().length() == 0)) {
            url = DEFAULT_URL;
        }
        dbUrl = url;
    }



    /**
     * _more_
     */
    protected void initAfter() {
        try {
            File userDir =
                getDataContext().getIdv().getObjectStore().getUserDirectory();
            String derbyDir = IOUtil.joinDir(userDir, "derbydb");
            IOUtil.makeDirRecursive(new File(derbyDir));
            System.setProperty("derby.system.home", derbyDir);

            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            Class.forName("com.mysql.jdbc.Driver");
            if ( !initConnection()) {
                setInError(true, true,
                           "Unable to initialize database connection");
            } else {
                stormInfos = getAllStormInfos();
            }
        } catch (Exception exc) {
            logException("Error initializing STI database", exc);
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
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public StormTrackCollection getTrackCollection(StormInfo stormInfo)
            throws Exception {
        long                 t1              = System.currentTimeMillis();
        StormTrackCollection trackCollection = new StormTrackCollection();
        List<Way>            forecastWays    = getForecastWays(stormInfo);
        for (Way forecastWay : forecastWays) {
            //            if(!forecastWay.getId().equals("SHTM")) continue;
            List forecastTracks = getForecastTracks(stormInfo, forecastWay);
            if (forecastTracks.size() > 0) {
                trackCollection.addTrackList(forecastTracks);
            }
        }
        StormTrack obsTrack = getObservationTrack(stormInfo);
        //                                         (Way) forecastWays.get(0));
        long t2 = System.currentTimeMillis();
        System.err.println("time:" + (t2 - t1));
        trackCollection.addTrack(obsTrack);
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
            COL_YEAR, COL_MONTH, COL_DAY, COL_HOUR, COL_FHOUR, COL_LATITUDE,
            COL_LONGITUDE, COL_WINDSPEED, COL_PRESSURE, COL_RADIUSMG,
            COL_RADIUSWG, COL_MOVEDIR, COL_MOVESPEED
        });


        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_WAY,
                                 SqlUtil.quote(forecastWay.getId())));

        addDateSelection(sTime, whereList);

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement             statement = evaluate(query);
        SqlUtil.Iterator      iter      = SqlUtil.getIterator(statement);
        ResultSet             results;

        List<StormTrackPoint> pts = new ArrayList();
        List<Attribute>       attrs;
        Real                  altReal = new Real(RealType.Altitude, 0);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                //                System.err.println ("row " + cnt);
                attrs = new ArrayList();
                int    col      = 1;
                int    year     = results.getInt(col++);
                int    month    = results.getInt(col++);
                int    day      = results.getInt(col++);
                int    hour     = results.getInt(col++);
                int    fhour    = results.getInt(col++);

                double latitude = results.getDouble(col++);
                if ((latitude == 9999) || (latitude == 999)) {
                    latitude = Float.NaN;
                }
                double longitude = results.getDouble(col++);
                if ((longitude == 9999) || (longitude == 999)) {
                    longitude = Float.NaN;
                }
                double windSpd = results.getDouble(col++);
                attrs.add(new Attribute("MaxWindSpeed", windSpd));
                double pressure = results.getDouble(col++);
                attrs.add(new Attribute("MinPressure", pressure));
                double radiusMG = results.getDouble(col++);
                attrs.add(new Attribute("RadiusModerateGale", radiusMG));
                double radiusWG = results.getDouble(col++);
                attrs.add(new Attribute("RadiusWholeGale", radiusWG));
                double moveDir = results.getDouble(col++);
                attrs.add(new Attribute("MoveDirection", moveDir));
                double moveSpd = results.getDouble(col++);
                attrs.add(new Attribute("MoveSpeed", moveSpd));

                EarthLocation elt =
                    new EarthLocationLite(new Real(RealType.Latitude,
                        latitude), new Real(RealType.Longitude, longitude),
                                   altReal);
                DateTime dttm = getDateTime(year, month, day, hour + fhour);
                StormTrackPoint stp = new StormTrackPoint(stormInfo, elt,
                                          dttm, fhour, attrs);
                pts.add(stp);
            }
        }

        if (pts.size() == 0) {
            //We should never be here
            System.err.println("found no track data time=" + sTime
                               + " from query:" + SqlUtil.makeAnd(whereList));
        }
        if (pts.size() > 0) {
            return new StormTrack(stormInfo, forecastWay, pts);
        } else {
            return null;
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
        whereList.add(SqlUtil.eq(COL_YEAR, Integer.toString(yy)));
        whereList.add(SqlUtil.eq(COL_MONTH, Integer.toString(mm)));
        whereList.add(SqlUtil.eq(COL_DAY, Integer.toString(dd)));
        whereList.add(SqlUtil.eq(COL_HOUR, Integer.toString(hh)));
    }

    /**
     * _more_
     *
     * @param stormInfo _more_
     * @param sTime _more_
     * @param forecastWay _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private StormTrack getForecastTrackX(StormInfo stormInfo, DateTime sTime,
                                         Way forecastWay)
            throws Exception {

        String columns = SqlUtil.comma(new String[] {
            COL_YEAR, COL_MONTH, COL_DAY, COL_HOUR, COL_FHOUR, COL_LATITUDE,
            COL_LONGITUDE, COL_WINDSPEED, COL_PRESSURE, COL_RADIUSMG,
            COL_RADIUSWG, COL_MOVEDIR, COL_MOVESPEED
        });



        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_WAY,
                                 SqlUtil.quote(forecastWay.getId())));


        addDateSelection(sTime, whereList);

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement statement = evaluate(query);

        //        SqlUtil.debug = firstTime;
        List<float[]>         values = SqlUtil.readFloats(statement, MISSING);
        int                   col       = 0;
        float[]               years     = values.get(col++);
        float[]               months    = values.get(col++);
        float[]               days      = values.get(col++);
        float[]               hours     = values.get(col++);
        float[]               fhours    = values.get(col++);
        float[]               lats      = values.get(col++);
        float[]               lons      = values.get(col++);
        List<StormTrackPoint> pts       = new ArrayList();

        List<EarthLocation>   earthLocs = new ArrayList<EarthLocation>();
        Real                  altReal   = new Real(RealType.Altitude, 0);


        List<DateTime>        times     = new ArrayList<DateTime>();
        for (int row = 0; row < years.length; row++) {
            if (Math.abs(lats[row]) > 90) {
                System.err.println("bad lat:" + lats[row] + " way="
                                   + forecastWay + " storm=" + stormInfo);
            }
            EarthLocation elt =
                new EarthLocationLite(new Real(RealType.Latitude, lats[row]),
                                      new Real(RealType.Longitude,
                                          lons[row]), altReal);
            /*
              double windSpd  = results.getDouble(col++);
              double pressure = results.getDouble(col++);
              double radiusMG = results.getDouble(col++);
              double radiusWG = results.getDouble(col++);
              double moveDir = results.getDouble(col++);
              double moveSpd = results.getDouble(col++);
            */

            times.add(getDateTime((int) years[row], (int) months[row],
                                  (int) days[row],
                                  (int) (hours[row] + fhours[row])));
        }


        return new StormTrack(
            stormInfo, forecastWay,
            new NamedArray("latitude", CommonUnit.degree, lats),
            new NamedArray("longitude", CommonUnit.degree, lons), times);

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

        String columns = SqlUtil.comma(new String[] { COL_YEAR, COL_MONTH,
                COL_DAY, COL_HOUR });

        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_FHOUR, ZEROHOUR));
        whereList.add(SqlUtil.eq(COL_WAY, SqlUtil.quote(way.getId())));

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;
        List<DateTime>   startDates = new ArrayList<DateTime>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col   = 1;
                int year  = results.getInt(col++);
                int month = results.getInt(col++);
                int day   = results.getInt(col++);
                int hour  = results.getInt(col++);
                startDates.add(getDateTime(year, month, day, hour));
            }
        }
        return startDates;
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
    protected StormTrack getObservationTrack(StormInfo stormInfo)
            throws Exception {
        List obsPts  = null;
        Way  babjWay = new Way("babj");
        //first get the obs from BABJ
        List<StormTrackPoint> obsBABJ = getObservationTrack(stormInfo,
                                            babjWay);

        DateTime timeMin = obsBABJ.get(0).getTrackPointTime();
        DateTime timeMax = obsBABJ.get(obsBABJ.size()
                                       - 1).getTrackPointTime();

        // get list of ways
        List<Way> ways = getForecastWays(stormInfo);

        for (Way way : ways) {
            if ( !way.equals(babjWay)) {
                obsBABJ = getObservationTrack(stormInfo, way, timeMin,
                        timeMax, obsBABJ);
                timeMin = obsBABJ.get(0).getTrackPointTime();
                timeMax = obsBABJ.get(obsBABJ.size() - 1).getTrackPointTime();
            }
        }

        return new StormTrack(stormInfo, Way.OBSERVATION, obsBABJ);
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
    protected List<StormTrackPoint> getObservationTrack(StormInfo stormInfo,
            Way wy)
            throws Exception {
        String columns = SqlUtil.comma(new String[] {
            COL_YEAR, COL_MONTH, COL_DAY, COL_HOUR, COL_LATITUDE,
            COL_LONGITUDE, COL_WINDSPEED, COL_PRESSURE, COL_RADIUSMG,
            COL_RADIUSWG, COL_MOVEDIR, COL_MOVESPEED, COL_WAY
        });

        List whereList = new ArrayList();

        whereList.add(SqlUtil.eq(COL_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_FHOUR, ZEROHOUR));
        whereList.add(SqlUtil.eq(COL_WAY, SqlUtil.quote(wy.getId())));

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        query = query + " order by  " + SqlUtil.comma(new String[] { COL_YEAR,
                COL_MONTH, COL_DAY, COL_HOUR });
        //        System.err.println (query);
        Statement             statement = evaluate(query);
        SqlUtil.Iterator      iter      = SqlUtil.getIterator(statement);
        ResultSet             results;

        List<StormTrackPoint> obsPts = new ArrayList();
        //Hashtable seenDate = new Hashtable();
        Real altReal = new Real(RealType.Altitude, 0);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                List<Attribute> attrs     = new ArrayList();
                int             col       = 1;
                int             year      = results.getInt(col++);
                int             month     = results.getInt(col++);
                int             day       = results.getInt(col++);
                int             hour      = results.getInt(col++);
                double          latitude  = results.getDouble(col++);
                double          longitude = results.getDouble(col++);
                double          windSpd   = results.getDouble(col++);
                attrs.add(new Attribute("MaxWindSpeed", windSpd));
                double pressure = results.getDouble(col++);
                attrs.add(new Attribute("MinPressure", pressure));
                double radiusMG = results.getDouble(col++);
                attrs.add(new Attribute("RadiusModerateGale", radiusMG));
                double radiusWG = results.getDouble(col++);
                attrs.add(new Attribute("RadiusWholeGale", radiusWG));
                double moveDir = results.getDouble(col++);
                attrs.add(new Attribute("MoveDirection", moveDir));
                double moveSpd = results.getDouble(col++);
                attrs.add(new Attribute("MoveSpeed", moveSpd));

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
                StormTrackPoint stp = new StormTrackPoint(stormInfo, elt,
                                          date, 0, attrs);
                obsPts.add(stp);
            }
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
            COL_YEAR, COL_MONTH, COL_DAY, COL_HOUR, COL_LATITUDE,
            COL_LONGITUDE, COL_WINDSPEED, COL_PRESSURE, COL_RADIUSMG,
            COL_RADIUSWG, COL_MOVEDIR, COL_MOVESPEED, COL_WAY
        });

        List whereList = new ArrayList();

        whereList.add(SqlUtil.eq(COL_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(COL_FHOUR, ZEROHOUR));
        whereList.add(SqlUtil.eq(COL_WAY, SqlUtil.quote(wy.getId())));

        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        query = query + " order by  " + SqlUtil.comma(new String[] { COL_YEAR,
                COL_MONTH, COL_DAY, COL_HOUR });
        //        System.err.println (query);
        Statement             statement = evaluate(query);
        SqlUtil.Iterator      iter      = SqlUtil.getIterator(statement);
        ResultSet             results;

        List<StormTrackPoint> obsPts  = new ArrayList();
        List<StormTrackPoint> obsPts1 = new ArrayList();
        List<StormTrackPoint> obsPts2 = new ArrayList();
        Real                  altReal = new Real(RealType.Altitude, 0);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                List<Attribute> attrs     = new ArrayList();
                int             col       = 1;
                int             year      = results.getInt(col++);
                int             month     = results.getInt(col++);
                int             day       = results.getInt(col++);
                int             hour      = results.getInt(col++);
                double          latitude  = results.getDouble(col++);
                double          longitude = results.getDouble(col++);
                double          windSpd   = results.getDouble(col++);
                attrs.add(new Attribute("MaxWindSpeed", windSpd));
                double pressure = results.getDouble(col++);
                attrs.add(new Attribute("MinPressure", pressure));
                double radiusMG = results.getDouble(col++);
                attrs.add(new Attribute("RadiusModerateGale", radiusMG));
                double radiusWG = results.getDouble(col++);
                attrs.add(new Attribute("RadiusWholeGale", radiusWG));
                double moveDir = results.getDouble(col++);
                attrs.add(new Attribute("MoveDirection", moveDir));
                double moveSpd = results.getDouble(col++);
                attrs.add(new Attribute("MoveSpeed", moveSpd));

                EarthLocation elt =
                    new EarthLocationLite(new Real(RealType.Latitude,
                        latitude), new Real(RealType.Longitude, longitude),
                                   altReal);

                DateTime date = getDateTime(year, month, day, hour);

                if (date.getValue() < before.getValue()) {
                    StormTrackPoint stp = new StormTrackPoint(stormInfo, elt,
                                              date, 0, attrs);
                    obsPts1.add(stp);
                }

                if (date.getValue() > after.getValue()) {
                    StormTrackPoint stp = new StormTrackPoint(stormInfo, elt,
                                              date, 0, attrs);
                    obsPts2.add(stp);
                }


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
        String columns = SqlUtil.distinct(COL_STORMID);
        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK));
        //        System.err.println (query);
        System.err.println(query);
        SqlUtil.Iterator iter = SqlUtil.getIterator(evaluate(query));
        ResultSet        results;
        List<StormInfo>  stormInfos = new ArrayList<StormInfo>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                String   id        = results.getString(1);
                DateTime startTime = getStormStartTime(id);
                System.err.println(id + " " + startTime);
                StormInfo sinfo = new StormInfo(id, startTime);
                stormInfos.add(sinfo);
            }
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
        String columns = SqlUtil.comma(new String[] { COL_YEAR, COL_MONTH,
                COL_DAY, COL_HOUR });

        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_STORMID, SqlUtil.quote(id)));
        whereList.add(SqlUtil.eq(COL_FHOUR, ZEROHOUR));
        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        query = query + " order by  " + columns;
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col   = 1;
                int year  = results.getInt(col++);
                int month = results.getInt(col++);
                int day   = results.getInt(col++);
                int hour  = results.getInt(col++);
                statement.close();
                //Just get the first one since we sorted the results with the order by
                return getDateTime(year, month, day, hour);
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
    protected List<Way> getForecastWays(StormInfo stormInfo)
            throws Exception {

        String columns   = SqlUtil.distinct(COL_WAY);

        List   whereList = new ArrayList();
        whereList.add(SqlUtil.eq(COL_STORMID,
                                 SqlUtil.quote(stormInfo.getStormId())));
        String query = SqlUtil.makeSelect(columns, Misc.newList(TABLE_TRACK),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;

        List<Way>        forecastWays = new ArrayList<Way>();

        //TODO: How do we handle no data???
        while ((results = iter.next()) != null) {
            while (results.next()) {
                Way way = new Way(results.getString(1));
                forecastWays.add(way);
            }
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
                System.err.println(url);
                connection = DriverManager.getConnection(url);
                //                connection = DriverManager.getConnection(url, userName,
                //                        password);
                return connection;
            } catch (SQLException sqe) {
                if ((sqe.toString()
                        .indexOf("role \"" + userName
                                 + "\" does not exist") >= 0) || (sqe
                                     .toString()
                                     .indexOf("user name specified") >= 0)) {
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

            //Load in the test data
            try {
                stmt.execute("select count(*) from typhoon");
                System.err.println("OK");
            } catch (Exception exc) {
                System.err.println("exc;" + exc);
                System.err.println("Creating test database");
                String initSql = IOUtil.readContents(
                                     "/ucar/unidata/data/storm/testdb.sql",
                                     getClass());

                connection.setAutoCommit(false);
                SqlUtil.loadSql(initSql, stmt, false);
                connection.commit();
                connection.setAutoCommit(true);
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
        StormTrackCollection cls            = s.getTrackCollection(sInfo);
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

