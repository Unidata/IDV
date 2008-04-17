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

import java.rmi.RemoteException;

import java.sql.*;

import java.text.SimpleDateFormat;

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

    // params for the table

    /** _more_ */
    private String tableName = "TYPHOON";

    /** _more_ */
    private String sIdColumn = "nno";

    /** _more_ */
    private String timeColumn = "time";

    /** _more_ */
    private String latitudeColumn = "lat";

    /** _more_ */
    private String longitudeColumn = "lon";

    /** _more_ */
    private String altitudeColumn = "altitude";

    /** _more_ */
    private String yearColumn = "yyyy";

    /** _more_ */
    private String monthColumn = "mm";

    /** _more_ */
    private String dayColumn = "day";

    /** _more_ */
    private String hourColumn = "hh";

    /** _more_ */
    private String fhourColumn = "fhour";

    /** _more_ */
    private String wayColumn = "way";

    /** hard coded data base url for now */
    private String dbUrl = "jdbc:derby:test;create=true";

    /** the db connection */
    private Connection connection;


    /** _more_ */
    private String fromDate = "-1 year";

    /** _more_ */
    private String toDate = "now";

    /** the stormInfo and track */

    private List stormInfos;





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
    }



    protected void initAfter() {
        try {
            Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
            if ( !initConnection()) {}
            initStormInfo();
        } catch(Exception exc) {
            logException("Error initializing STI database",exc);
        }
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public void initStormInfo() throws Exception {
        if (stormInfos == null) {
            stormInfos = getAllStormInfos();
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<StormInfo> getStormInfos() {
        int             size   = stormInfos.size();
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
        StormTrackCollection trackCollection =   new StormTrackCollection();
        List     forecastWays = getForecastWays(stormInfo);

        Iterator itr          = forecastWays.iterator();
        while (itr.hasNext()) {
            Way  forecastWay    = (Way) itr.next();
            List forecastTracks = getForecastTracks(stormInfo, forecastWay);
            if (forecastTracks.size() > 0) {
                trackCollection.addTrackList(forecastTracks);
            }

        }

        StormTrack obsTrack = getObservationTrack(stormInfo,
                                             (Way) forecastWays.get(0));
        trackCollection.addTrack(obsTrack);

        return trackCollection;
    }



    /**
     * _more_
     *
     * @param fTrack _more_
     */
    public void setForecastTracks(Map fTrack) {}

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
    protected List getForecastTracks(StormInfo stormInfo, Way forecastWay)
            throws Exception {

        List tracks     = new ArrayList();
        List startDates = getForecastTrackStartDates(stormInfo, forecastWay);
        int  nstarts    = startDates.size();
        for (int i = 0; i < nstarts; i++) {
            Date  dt = (Date) startDates.get(i);
            StormTrack tk = getForecastTracks(stormInfo, dt, forecastWay);
            if (tk != null) {
                int pn = tk.getTrackPoints().size();
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
    protected StormTrack getForecastTracks(StormInfo stormInfo, Date sTime,
                                      Way forecastWay)
            throws Exception {

        String columns = sIdColumn + "," + yearColumn + "," + monthColumn
                         + "," + dayColumn + "," + hourColumn + ","
                         + fhourColumn + "," + latitudeColumn + ","
                         + longitudeColumn + "," + wayColumn;


        //SimpleDateFormat sdf = new SimpleDateFormat();
        //sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        //sdf.applyPattern("yyyy-MM-dd HH:mm:ss");

        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(sIdColumn,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(wayColumn,
                                 SqlUtil.quote(forecastWay.getId())));

        //Date s = sdf.parse(sTime.toString());
        Calendar cal = Calendar.getInstance();
        cal.setTime(sTime);
        int yy = cal.get(Calendar.YEAR);
        int mm = cal.get(Calendar.MONTH) + 1;
        int dd = cal.get(Calendar.DAY_OF_MONTH);
        int hh = cal.get(Calendar.HOUR_OF_DAY);

        whereList.add(SqlUtil.eq(yearColumn, Integer.toString(yy)));
        whereList.add(SqlUtil.eq(monthColumn, Integer.toString(mm)));
        whereList.add(SqlUtil.eq(dayColumn, Integer.toString(dd)));
        whereList.add(SqlUtil.eq(hourColumn, Integer.toString(hh)));
        String query = SqlUtil.makeSelect(columns, Misc.newList(tableName),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;

        //

        List             times = new ArrayList();
        List             pts   = new ArrayList();
        SimpleDateFormat sdf   = new SimpleDateFormat();
        //sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern("yyyy/MM/dd HH");
        while ((results = iter.next()) != null) {
            while (results.next()) {
                //                System.err.println ("row " + cnt);

                int col = 1;
                col++;  //sIdColumn
                int    year      = results.getInt(col++);
                int    month     = results.getInt(col++);
                int    day       = results.getInt(col++);
                int    hour      = results.getInt(col++);
                int    fhour     = results.getInt(col++);


                double latitude  = results.getDouble(col++);
                double longitude = results.getDouble(col++);
                double altitude  = 0.0;

                EarthLocation elt =
                    new EarthLocationLite(new Real(RealType.Latitude,
                        latitude), new Real(RealType.Longitude, longitude),
                                   new Real(RealType.Altitude, altitude));
                int h = hour + fhour;
                Date fdate = sdf.parse(year + "/" + month + "/" + day + " "
                                       + h);

                times.add(fdate);
                pts.add(elt);

            }

        }
        if (pts.size() > 0) {
            return new StormTrack(stormInfo, forecastWay, pts, times, null);
        } else {
            return null;
        }

    }


    /**
     * _more_
     */
    protected void doMakeDataChoices() {
        List cats = DataCategory.parseCategories("stormtrack", false);
        DataChoice choice = new DirectDataChoice(this, "stormtrack",
                                "Storm Track", "Storm Track", cats,
                                (Hashtable) null);
        addDataChoice(choice);

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
    protected List getForecastTrackStartDates(StormInfo stormInfo, Way way)
            throws Exception {

        String columns = sIdColumn + "," + yearColumn + "," + monthColumn
                         + "," + dayColumn + "," + hourColumn + ","
                         + fhourColumn + "," + wayColumn;

        List whereList = new ArrayList();
        whereList.add(SqlUtil.eq(sIdColumn,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(fhourColumn, Integer.toString(0)));
        whereList.add(SqlUtil.eq(wayColumn, SqlUtil.quote(way.getId())));

        String query = SqlUtil.makeSelect(columns, Misc.newList(tableName),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;
        List             startDates = new ArrayList();

        //
        int              cnt = 0;
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern("yyyy/MM/dd HH");

        while ((results = iter.next()) != null) {
            while (results.next()) {
                //                System.err.println ("row " + cnt);
                cnt++;
                int col = 1;
                col++;
                int year  = results.getInt(col++);
                int month = results.getInt(col++);
                int day   = results.getInt(col++);
                int hour  = results.getInt(col++);

                Date date = sdf.parse(year + "/" + month + "/" + day + " "
                                      + hour);

                //DateTime dttm          = new DateTime(date);

                startDates.add(date);
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
     * @param wy _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected StormTrack getObservationTrack(StormInfo stormInfo, Way wy)
            throws Exception {

        String columns = sIdColumn + "," + yearColumn + "," + monthColumn
                         + "," + dayColumn + "," + hourColumn + ","
                         + fhourColumn + "," + latitudeColumn + ","
                         + longitudeColumn + "," + wayColumn;

        List whereList = new ArrayList();

        whereList.add(SqlUtil.eq(sIdColumn,
                                 SqlUtil.quote(stormInfo.getStormId())));
        whereList.add(SqlUtil.eq(fhourColumn, "0"));
        whereList.add(SqlUtil.eq(wayColumn, SqlUtil.quote(wy.getId())));

        String query = SqlUtil.makeSelect(columns, Misc.newList(tableName),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;

        //
        int              cnt = 0;
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern("yyyy/MM/dd HH");
        List obsPts = new ArrayList();
        List obsDts = new ArrayList();

        while ((results = iter.next()) != null) {
            while (results.next()) {
                //                System.err.println ("row " + cnt);
                cnt++;
                int col = 1;
                col++;
                int year  = results.getInt(col++);
                int month = results.getInt(col++);
                int day   = results.getInt(col++);
                int hour  = results.getInt(col++);
                col++;  //fhour

                double latitude  = results.getDouble(col++);
                double longitude = results.getDouble(col++);
                double altitude  = 0.0;

                EarthLocation elt =
                    new EarthLocationLite(new Real(RealType.Latitude,
                        latitude), new Real(RealType.Longitude, longitude),
                                   new Real(RealType.Altitude, altitude));


                Date date = sdf.parse(year + "/" + month + "/" + day + " "
                                      + hour);

                //DateTime dttm          = new DateTime(date);
                obsDts.add(date);
                obsPts.add(elt);
            }
        }
        //Date dts = getStartTime(obsDts);
        Way obsWay = new Way("obsr");
        return new StormTrack(stormInfo, obsWay, obsPts, obsDts, null);

    }


    /**
     * _more_
     *
     * @param times _more_
     *
     * @return _more_
     */
    protected Date getStartTime(List times) {
        int  size  = times.size();
        Date dt    = (Date) times.get(0);
        int  idx   = 0;
        long value = dt.getTime();
        for (int i = 1; i < size; i++) {
            dt = (Date) times.get(i);
            long dtValue = dt.getTime();
            if (dtValue < value) {
                value = dtValue;
                idx   = i;
            }
        }
        return (Date) times.get(idx);
    }

    /**
     * _more_
     *
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    protected List getAllStormInfos() throws Exception {

        String columns   = "DISTINCT" + " " + sIdColumn;

        List   whereList = new ArrayList();

        String query = SqlUtil.makeSelect(columns, Misc.newList(tableName),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;

        List             stormInfos = new ArrayList();

        //TODO: How do we handle no data???
        int cnt = 0;

        while ((results = iter.next()) != null) {
            while (results.next()) {
                //                System.err.println ("row " + cnt);
                cnt++;
                int       col       = 1;
                String    id        = results.getString(col++);

                Date      startTime = getStormStartTime(id);

                StormInfo sinfo     = new StormInfo(id, startTime);
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
    protected Date getStormStartTime(String id) throws Exception {

        String columns = sIdColumn + "," + yearColumn + "," + monthColumn
                         + "," + dayColumn + "," + hourColumn + ","
                         + fhourColumn;

        List whereList = new ArrayList();

        whereList.add(SqlUtil.eq(sIdColumn, SqlUtil.quote(id)));
        whereList.add(SqlUtil.eq(fhourColumn, Integer.toString(0)));


        String query = SqlUtil.makeSelect(columns, Misc.newList(tableName),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;

        //
        int              cnt = 0;
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        sdf.applyPattern("yyyy/MM/dd HH");

        List obsDts = new ArrayList();

        while ((results = iter.next()) != null) {
            while (results.next()) {
                //                System.err.println ("row " + cnt);
                cnt++;
                int col = 1;
                col++;
                int year  = results.getInt(col++);
                int month = results.getInt(col++);
                int day   = results.getInt(col++);
                int hour  = results.getInt(col++);
                col++;  //fhour


                Date date = sdf.parse(year + "/" + month + "/" + day + " "
                                      + hour);

                obsDts.add(date);

            }
        }

        Date startTime = getStartTime(obsDts);

        return startTime;

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
    protected List getForecastWays(StormInfo stormInfo) throws Exception {

        String columns   = "DISTINCT" + " " + sIdColumn + "," + wayColumn;

        List   whereList = new ArrayList();
        whereList.add(SqlUtil.eq(sIdColumn,
                                 SqlUtil.quote(stormInfo.getStormId())));
        String query = SqlUtil.makeSelect(columns, Misc.newList(tableName),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;

        List             forecastWays = new ArrayList();

        //TODO: How do we handle no data???
        int cnt = 0;

        while ((results = iter.next()) != null) {
            while (results.next()) {
                //                System.err.println ("row " + cnt);
                cnt++;
                int col = 1;
                col++;
                String id  = results.getString(col++);
                Way    way = new Way(id);
                forecastWays.add(way);

            }
        }

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
        }

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
            try {
                connection = DriverManager.getConnection(url, userName,
                        password);
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
            /*

            Connection connection = getConnection();
            Statement  stmt       = connection.createStatement();
            //Drop the table  - ignore any errors
            SqlUtil.loadSql("drop table " + tableName, stmt, false);

            //Load in the test data
            System.err.println("Creating test database");
            String initSql = IOUtil.readContents(
                                 "/ucar/unidata/data/sounding/typhoon.sql",
                                 getClass());


            connection.setAutoCommit(false);
            SqlUtil.loadSql(initSql, stmt, false);
            connection.commit();
            connection.setAutoCommit(true);
            System.err.println("OK");
            */
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
        String             sid = "0622";
        STIStormDataSource s   = null;
        try {
            s = new STIStormDataSource();
        } catch (Exception exc) {
            System.err.println("err:" + exc);
            exc.printStackTrace();
        }

        List            sInfoList = s.getStormInfos();
        StormInfo       sInfo     = (StormInfo) sInfoList.get(0);

        String          sd        = sInfo.getStormId();
        StormTrackCollection cls       = s.getTrackCollection(sInfo);
        Map             mp        = cls.getWayToStartDatesHashMap();
        Map             mp1       = cls.getWayToTracksHashMap();
        StormTrack           obsTrack  = cls.getObsTrack();

        System.err.println("test:");

    }
}

