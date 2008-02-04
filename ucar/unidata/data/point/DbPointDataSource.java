/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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


import ucar.ma2.Range;

import ucar.nc2.*;

import ucar.unidata.data.*;

import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.ui.SqlShell;


import ucar.unidata.ui.TwoListPanel;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PollingInfo;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.WrapperException;


import visad.*;

import visad.georef.*;


import visad.util.DataUtility;

import java.awt.*;
import java.awt.event.*;

import java.io.File;

import java.net.MalformedURLException;

import java.net.URL;

import java.rmi.RemoteException;

import java.sql.*;

import java.text.SimpleDateFormat;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

/**
 *
 * @author IDV Development Team
 * @version $Revision: 1.90 $ $Date: 2007/08/06 17:02:27 $
 */
public class DbPointDataSource extends PointDataSource {

    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);


    //jdms.db.derby.home=%userhome%/.unidata/repository/derby
    //jdms.db.derby.driver=org.apache.derby.jdbc.EmbeddedDriver
    //jdms.db.derby.url=jdbc:derby:repository;create=true

    private String dbUrl = "jdbc:derby:pointdata;create=true";

    private Connection connection;




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
     * @throws VisADException  couldn't create the VisAD data
     */
    public DbPointDataSource(DataSourceDescriptor descriptor,
                               String source, Hashtable properties)
            throws Exception {
        super(descriptor, source, "Db Point Data", properties);
        Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
        if ( !initConnection()) {
            setInError(true, false, "");
        }       

    }


    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox)
            throws Exception {

        Statement   statement = select("*", "pointdata");
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        ResultSet        results;



        String [] units = {"C","m/s","degrees"};
        String [] numericNames = {"temperature","winddirection", "windspeed"};


        List numericTypes = new ArrayList();
        List numericUnits = new ArrayList();

        for(int i=0;i<numericNames.length;i++) {
            Unit unit = DataUtil.parseUnit(units[i]);
            numericTypes.add(DataUtil.makeRealType(numericNames[i], unit));
            numericUnits.add(unit);
        }

        Unit[] allUnits =
            (Unit[]) numericUnits.toArray(new Unit[numericUnits.size()]);


        List stringTypes = new ArrayList();
        stringTypes.add(TextType.getTextType("Station"));



        TupleType allTupleType =  DoubleStringTuple.makeTupleType(numericTypes, stringTypes);

        TupleType         finalTT = null;
        List obs = new ArrayList();

        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col=1;
                Date date = new Date(results.getTimestamp(col++, calendar).getTime());
                double latitude = results.getDouble(col++);
                double longitude = results.getDouble(col++);
                double altitude = results.getDouble(col++);

                EarthLocation elt =
                    new EarthLocationLite(new Real(RealType.Latitude, latitude),
                                          new Real(RealType.Longitude, longitude),
                                          new Real(RealType.Altitude, altitude));

                DateTime dttm = new DateTime(date);

                String station = results.getString(col++);
                double temperature = results.getDouble(col++);
                double windDirection = results.getDouble(col++);
                double windSpeed = results.getDouble(col++);


                double[] realArray   = new double[]{temperature, windDirection, windSpeed};
                String[] stringArray   = new String[]{station};
                Tuple tuple =  new DoubleStringTuple(allTupleType, realArray,
                                                     stringArray, allUnits);

                PointObTuple pot;
                if (finalTT == null) {
                    pot = new PointObTuple(elt, dttm, tuple);
                    finalTT = Tuple.buildTupleType(pot.getComponents());
                } else {
                    pot = new PointObTuple(elt, dttm, tuple, finalTT, false);
                    
                }
                obs.add(pot);
            }
        }
        statement.close();

        Integer1DSet indexSet =
            new Integer1DSet(RealType.getRealType("index"), obs.size());
        FieldImpl retField =
            new FieldImpl(
                new FunctionType(
                    ((SetType) indexSet.getType()).getDomain(),
                    ((PointObTuple)obs.get(0)).getType()), indexSet);
        Data[]obsArray = (Data[]) obs.toArray(new Data[obs.size()]);

        retField.setSamples(obsArray, false, false);




        return retField;
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
        String url = dbUrl;
        if ((getUserName() == null)
                || (getUserName().trim().length() == 0)) {
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
                    if ( !showPasswordDialog("Database Login",
                            label)) {
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
     * @param what _more_
     * @param where _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws SQLException _more_
     */
    private Statement select(String what, String where, String extra)
            throws SQLException {
        return evaluate(SqlUtil.makeSelect(what, Misc.newList(where), extra));
    }

    /**
     * _more_
     *
     * @param what _more_
     * @param where _more_
     *
     * @return _more_
     *
     * @throws SQLException _more_
     */
    private Statement select(String what, String where) throws SQLException {
        return evaluate(SqlUtil.makeSelect(what, Misc.newList(where)));
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
     *
     * @throws Exception _more_
     */
    private boolean initConnection() throws Exception {
        if (getConnection() == null) {
            return false;
        }

        try {
            //Create the dummy database
            Statement stmt = getConnection().createStatement();
            //Drop it - ignore any errors
            SqlUtil.loadSql("drop table pointdata;", stmt, true);
            System.err.println("Creating test database");
            String initSql = IOUtil.readContents("/ucar/unidata/data/point/testdb.sql",getClass());
            SqlUtil.loadSql(initSql, stmt, false);
            System.err.println ("OK");
        } catch(Exception exc) {
            exc.printStackTrace();
        }

        Statement         stmt;
        ResultSet         results;
        SqlUtil.Iterator iter;
        Hashtable         cats      = new Hashtable();
        try {
            /*            stmt = select("*", TABLE_CATEGORIES);
            iter = SqlUtil.getIterator(stmt);
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    cats.put(results.getString(COL_VARIABLE),
                             results.getString(COL_CATEGORY));
                }
                }*/
        } catch (Exception exc) {
            //                exc.printStackTrace();
        }

        return true;
    }


}

