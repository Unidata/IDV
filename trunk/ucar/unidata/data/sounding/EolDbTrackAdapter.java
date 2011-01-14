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

package ucar.unidata.data.sounding;


import ucar.ma2.Range;

import ucar.nc2.*;

import ucar.unidata.data.*;

import ucar.unidata.sql.SqlUtil;

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

import visad.georef.EarthLocationTuple;

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
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;



/**
 *
 * @author IDV Development Team
 * @version $Revision: 1.90 $ $Date: 2007/08/06 17:02:27 $
 */
public class EolDbTrackAdapter extends TrackAdapter {

    /** _more_          */
    public static final String TABLE_GLOBALS = "global_attributes";

    /** _more_          */
    public static final String TABLE_VARIABLE_LIST = "variable_list";

    /** _more_          */
    public static final String TABLE_CATEGORIES = "categories";

    /** _more_          */
    public static final String TABLE_DATA = "raf_lrt";


    /** _more_          */
    public static final String COL_VARIABLE = "variable";

    /** _more_          */
    public static final String COL_CATEGORY = "category";

    /** _more_          */
    public static final String COL_NAME = "name";

    /** _more_          */
    public static final String COL_LONG_NAME = "long_name";

    /** _more_          */
    public static final String COL_UNITS = "units";

    /** _more_          */
    public static final String COL_MISSING_VALUE = "missing_value";

    /** _more_          */
    public static final String GLOBAL_PROJECTNAME = "ProjectName";

    /** _more_          */
    public static final String GLOBAL_FLIGHTNUMBER = "FlightNumber";

    /** _more_          */
    public static final String GLOBAL_COORDINATES = "coordinates";

    /** _more_          */
    public static final String GLOBAL_STARTTIME = "StartTime";

    /** _more_          */
    public static final String GLOBAL_ENDTIME = "EndTime";

    /** _more_          */
    private Connection connection;



    /** _more_          */
    private String url;

    /** _more_          */
    private String name;

    /** _more_          */
    private String description;

    /** _more_          */
    private Hashtable globals;

    /** _more_          */
    private Hashtable missingMap;




    /**
     * _more_
     *
     * @param dataSource _more_
     * @param filename _more_
     * @param pointDataFilter _more_
     * @param stride _more_
     * @param lastNMinutes _more_
     *
     * @throws Exception _more_
     */
    public EolDbTrackAdapter(TrackDataSource dataSource, String filename,
                             Hashtable pointDataFilter, int stride,
                             int lastNMinutes)
            throws Exception {
        super(dataSource, filename, pointDataFilter, stride, lastNMinutes);
        Class.forName("org.postgresql.Driver");
        if ( !initConnection()) {
            dataSource.setInError(true, false, "");
        }
    }





    /**
     * _more_
     *
     * @param actions _more_
     */
    protected void addActions(List actions) {
        AbstractAction a = new AbstractAction("Show Sql Shell") {
            public void actionPerformed(ActionEvent ae) {
                dataSource.showSqlShell();
            }
        };
        actions.add(a);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDataSourceName() {
        return name;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getDataSourceDescription() {
        return description;
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
        String url = getFilename();
        if ((dataSource.getUserName() == null)
                || (dataSource.getUserName().trim().length() == 0)) {
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
                        dataSource.setUserName(value);
                    } else if (name.equals("password")) {
                        dataSource.setPassword(value);
                    }
                }
            }
        }

        int cnt = 0;
        while (true) {
            String userName = dataSource.getUserName();
            String password = dataSource.getPassword();
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
                    if ( !dataSource.showPasswordDialog("Database Login",
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
        System.err.println("EVAL: " + sql);
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
        //jdbc:postgresql://eol-rt-data.guest.ucar.edu/real-time
        //        evaluate("CREATE RULE update AS ON UPDATE TO global_attributes DO NOTIFY current;");

        Statement         stmt;
        ResultSet         results;
        SqlUtil.Iterator iter;
        EolDbTrackInfo    trackInfo = new EolDbTrackInfo(this, "TRACK");
        Hashtable         cats      = new Hashtable();
        try {
            stmt = select("*", TABLE_CATEGORIES);
            iter = SqlUtil.getIterator(stmt);
            while ((results = iter.getNext()) != null) {
                    cats.put(results.getString(COL_VARIABLE),
                             results.getString(COL_CATEGORY));
            }
        } catch (Exception exc) {
            //                exc.printStackTrace();
        }

        missingMap = new Hashtable();
        stmt       = select("*", TABLE_GLOBALS);
        globals    = new Hashtable();
        boolean gotCoords = false;
        description = "<b>Globals</b><br>";
        iter        = SqlUtil.getIterator(stmt);
        while ((results = iter.getNext()) != null) {
                String globalName  = results.getString(1).trim();
                String globalValue = results.getString(2).trim();
                globals.put(globalName, globalValue);
                description = description + "<tr valign=\"top\"><td>"
                              + globalName + "</td><td>" + globalValue
                              + "</td></tr>";

                //            System.err.println(globalName +"=" + globalValue);

                if (globalName.equals(GLOBAL_STARTTIME)) {
                    startTime = new DateTime(DateUtil.parse(globalValue));
                } else if (globalName.equals(GLOBAL_ENDTIME)) {
                    endTime = new DateTime(DateUtil.parse(globalValue));
                } else if (globalName.equals(GLOBAL_COORDINATES)) {
                    List toks = StringUtil.split(globalValue, " ", true,
                                    true);
                    if (toks.size() != 4) {
                        throw new BadDataException(
                            "Incorrect coordinates value in database:"
                            + globalValue);
                    }
                    gotCoords = true;
                    System.err.println("coords:" + toks);
                    trackInfo.setCoordinateVars((String) toks.get(0),
                            (String) toks.get(1), (String) toks.get(2),
                            (String) toks.get(3));

                    trackInfo.setCoordinateVars("GGLON", "GGLAT", "GGALT",
                            "datetime");
                }
        }
        description = description + "</table>";

        if ( !gotCoords) {
            throw new BadDataException("No coordinates found in database");
        }

        this.name = (String) globals.get(GLOBAL_PROJECTNAME);
        String flight = (String) globals.get(GLOBAL_FLIGHTNUMBER);
        if ((this.name != null) && (flight != null)
                && (flight.length() != 0)) {
            this.name += " - " + flight;
        }

        stmt = select("*", TABLE_VARIABLE_LIST);
        iter = SqlUtil.getIterator(stmt);
        while ((results = iter.getNext()) != null) {
                String name    = results.getString(COL_NAME).trim();
                String desc    = results.getString(COL_LONG_NAME).trim();
                Unit   unit    = DataUtil.parseUnit(results.getString(COL_UNITS).trim());
                String  cat      = (String) cats.get(name);
                double missing = results.getDouble(COL_MISSING_VALUE);
                VarInfo variable = new VarInfo(name, desc, cat, unit, missing);
                trackInfo.addVariable(variable);
        }
        addTrackInfo(trackInfo);
        return true;
    }

    /**
     * _more_
     *
     * @param var _more_
     *
     * @return _more_
     */
    protected double getMissingValue(String var) {
        Double d = (Double) missingMap.get(var);
        if (d != null) {
            return d.doubleValue();
        }
        return Double.NaN;
    }


    /**
     * Class EolDbTrackInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class EolDbTrackInfo extends TrackInfo {

        /**
         * _more_
         *
         * @param adapter _more_
         * @param trackName _more_
         *
         * @throws Exception _more_
         */
        public EolDbTrackInfo(EolDbTrackAdapter adapter, String trackName)
                throws Exception {
            super(adapter, trackName);
        }

        /** _more_          */
        private int numberOfPoints = -1;

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        public int getNumberPoints() throws Exception {
            if (numberOfPoints < 0) {
                Statement stmt = select("COUNT(" + varTime + ")", TABLE_DATA);
                ResultSet         results;
                int               cnt  = 0;
                SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
                while ((results = iter.getNext()) != null) {
                    cnt++;
                }
                numberOfPoints = cnt;
            }
            return numberOfPoints;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public DateTime getStartTime() {
            return EolDbTrackAdapter.this.getStartTime();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public DateTime getEndTime() {
            return EolDbTrackAdapter.this.getEndTime();
        }

        /**
         * _more_
         *
         * @param range _more_
         * @param var _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        protected double[] getDoubleData(Range range, String var)
                throws Exception {
            double[] d = SqlUtil.readDouble(SqlUtil.getIterator(select(var, TABLE_DATA,
                             "order by " + varTime)), 1, getMissingValue(var));
            if (d.length == 0) {
                throw new BadDataException(
                    "No observations found in data base");
            }
            return d;
        }

        /**
         * _more_
         *
         * @param range _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        protected double[] getTime(Range range) throws Exception {
            String[] s = getStringData(range, varTime);
            if (s.length == 0) {
                throw new BadDataException("No times found in data base");
            }
            return DateUtil.toSeconds(s);
        }

        /**
         * _more_
         *
         * @param range _more_
         * @param var _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        protected float[] getFloatData(Range range, String var)
                throws Exception {
            long t1 = System.currentTimeMillis();
            float[] f = SqlUtil.readFloat(SqlUtil.getIterator(select("(" + var + ")",
                            TABLE_DATA, "order by " + varTime)), 1,
                                (float) getMissingValue(var));
            long t2 = System.currentTimeMillis();
            //            System.err.println("length:" + f.length + " time:" + (t2-t1));
            if (f.length == 0) {
                throw new BadDataException(
                    "No observations found in data base");
            }
            return f;
        }

        /**
         * _more_
         *
         * @param range _more_
         * @param var _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        protected String[] getStringData(Range range, String var)
                throws Exception {
            return SqlUtil.readString(SqlUtil.getIterator(select(var, TABLE_DATA,
                    "order by " + varTime)), 1);
        }

    }

}

