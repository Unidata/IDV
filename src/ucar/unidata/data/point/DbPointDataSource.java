/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.point;


import ucar.unidata.data.*;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import visad.*;

import visad.georef.*;



import java.rmi.RemoteException;

import java.sql.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;



/**
 *
 * @author IDV Development Team
 * @version $Revision: 1.90 $ $Date: 2007/08/06 17:02:27 $
 */
public class DbPointDataSource extends PointDataSource {

    /** calender */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);

    /** the db table */
    private String tableName = "pointdata";

    /** time column */
    private String timeColumn = "time";

    /** lat col */
    private String latitudeColumn = "latitude";

    /** long col */
    private String longitudeColumn = "longitude";

    /** alt col */
    private String altitudeColumn = "altitude";


    /** hard coded data base url for now */
    private String dbUrl = "jdbc:derby:pointdata;create=true";

    /** the db connection */
    private Connection connection;


    /** search criteria */
    private String fromDate = "-1 year";

    /** search criteria */
    private String toDate = "now";


    /**
     * Default contstructor.
     *
     * @throws VisADException
     */
    public DbPointDataSource() throws VisADException {
        init();
    }

    /**
     * Create a new <code>AddePointDataSource</code> from the parameters
     * supplied.
     *
     * @param descriptor  <code>DataSourceDescriptor</code> for this.
     * @param source      Source URL
     * @param properties  <code>Hashtable</code> of properties for the source.
     *
     *
     * @throws Exception On badness
     */
    public DbPointDataSource(DataSourceDescriptor descriptor, String source,
                             Hashtable properties)
            throws Exception {
        super(descriptor, source, "Db Point Data", properties);
        //Load in the java derby driver
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        if ( !initConnection()) {
            setInError(true, false, "");
        }

    }


    /**
     * get the jdbc connection
     *
     * @return the jdbc connection
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
     * intialize the jdbc connection
     *
     * @return success
     *
     * @throws Exception On badness
     */
    private boolean initConnection() throws Exception {
        if (getConnection() == null) {
            return false;
        }

        try {
            //Create the dummy database
            Statement stmt = getConnection().createStatement();
            //Drop the table  - ignore any errors
            SqlUtil.loadSql("drop table pointdata;", stmt, true);

            //Load in the test data
            System.err.println("Creating test database");
            String initSql =
                IOUtil.readContents("/ucar/unidata/data/point/testdb.sql",
                                    getClass());
            SqlUtil.loadSql(initSql, stmt, false);
            System.err.println("OK");
        } catch (Exception exc) {
            exc.printStackTrace();
            return false;
        }
        return true;
    }



    /**
     * make the obs
     *
     * @param dataChoice the data choice
     * @param subset the data selection
     * @param bbox the bbox
     *
     * @return the obs
     *
     * @throws Exception On badness
     */
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox)
            throws Exception {

        String columns = timeColumn + "," + latitudeColumn + ","
                         + longitudeColumn + "," + altitudeColumn + ","
                         + "station,temperature, winddir, windspeed";
        Date[] dateRange = DateUtil.getDateRange(fromDate, toDate,
                               new Date());
        List whereList = new ArrayList();
        if (dateRange[0] != null) {
            whereList.add(SqlUtil.ge(timeColumn, dateRange[0]));
        }
        if (dateRange[1] != null) {
            whereList.add(SqlUtil.le(timeColumn, dateRange[1]));
        }

        String query = SqlUtil.makeSelect(columns, Misc.newList(tableName),
                                          SqlUtil.makeAnd(whereList));
        //        System.err.println (query);
        Statement        statement = evaluate(query);
        SqlUtil.Iterator iter      = SqlUtil.getIterator(statement);
        ResultSet        results;

        String[]         units = { "C", "m/s", "degrees" };
        String[] numericNames = { "temperature", "winddirection",
                                  "windspeed" };

        List numericTypes = new ArrayList();
        List numericUnits = new ArrayList();

        for (int i = 0; i < numericNames.length; i++) {
            Unit unit = DataUtil.parseUnit(units[i]);
            numericTypes.add(DataUtil.makeRealType(numericNames[i], unit));
            numericUnits.add(unit);
        }

        Unit[] allUnits =
            (Unit[]) numericUnits.toArray(new Unit[numericUnits.size()]);


        List stringTypes = new ArrayList();
        stringTypes.add(TextType.getTextType("Station"));

        TupleType allTupleType =
            DoubleStringTuple.makeTupleType(numericTypes, stringTypes);

        TupleType finalTT = null;
        List      obs     = new ArrayList();

        //TODO: How do we handle no data???
        int cnt = 0;
        while ((results = iter.getNext()) != null) {
                //                System.err.println ("row " + cnt);
                cnt++;
                int col = 1;
                Date date = new Date(results.getTimestamp(col++,
                                calendar).getTime());
                double latitude  = results.getDouble(col++);
                double longitude = results.getDouble(col++);
                double altitude  = results.getDouble(col++);

                EarthLocation elt =
                    new EarthLocationLite(new Real(RealType.Latitude,
                        latitude), new Real(RealType.Longitude, longitude),
                                   new Real(RealType.Altitude, altitude));

                DateTime dttm          = new DateTime(date);

                String   station       = results.getString(col++);
                double   temperature   = results.getDouble(col++);
                double   windDirection = results.getDouble(col++);
                double   windSpeed     = results.getDouble(col++);

                double[] realArray = new double[] { temperature,
                        windDirection, windSpeed };
                String[] stringArray = new String[] { station };
                Tuple tuple = new DoubleStringTuple(allTupleType, realArray,
                                  stringArray, allUnits);

                PointObTuple pot;
                if (finalTT == null) {
                    pot     = new PointObTuple(elt, dttm, tuple);
                    finalTT = Tuple.buildTupleType(pot.getComponents());
                } else {
                    pot = new PointObTuple(elt, dttm, tuple, finalTT, false);

                }
                obs.add(pot);
        }


        Integer1DSet indexSet =
            new Integer1DSet(RealType.getRealType("index"), obs.size());
        FieldImpl retField =
            new FieldImpl(
                new FunctionType(
                    ((SetType) indexSet.getType()).getDomain(),
                    ((PointObTuple) obs.get(0)).getType()), indexSet);
        Data[] obsArray = (Data[]) obs.toArray(new Data[obs.size()]);

        retField.setSamples(obsArray, false, false);




        return retField;
    }


    /**
     * add to properties. The comps list contains pairs of label/widget.
     *
     *
     * @param comps comps
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        comps.add(GuiUtils.filler());
        comps.add(getPropertiesHeader("Database"));
        //Add the date range here
        //Look in PointDataSource to see how to add things to the comps list
    }


    /**
     * apply the properties
     *
     * @return success
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        //Set the state from the properties widgets here
        return true;
    }


    /**
     * execute the sql
     *
     * @param sql the sql
     *
     * @return the stmt
     *
     * @throws SQLException On badness
     */
    private Statement evaluate(String sql) throws SQLException {
        Statement stmt = getConnection().createStatement();
        stmt.execute(sql);
        return stmt;
    }



}
