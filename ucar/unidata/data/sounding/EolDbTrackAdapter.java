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

import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;

import ucar.unidata.ui.TwoListPanel;

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

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;

import java.sql.*;



/**
 *
 * @author IDV Development Team
 * @version $Revision: 1.90 $ $Date: 2007/08/06 17:02:27 $
 */
public class EolDbTrackAdapter extends TrackAdapter {

    public static final String TABLE_GLOBALS = "global_attributes";
    public static final String TABLE_VARIABLE_LIST = "variable_list";
    public static final String TABLE_CATEGORIES = "categories";
    public static final String TABLE_DATA = "raf_lrt";


    public static final String COL_VARIABLE = "variable";   
    public static final String COL_CATEGORY = "category";
    public static final String COL_NAME = "name";
    public static final String COL_LONG_NAME = "long_name";
    public static final String COL_UNITS = "units";    
    public static final String COL_MISSING_VALUE = "missing_value";

    public static final String GLOBAL_PROJECTNAME = "ProjectName";
    public static final String GLOBAL_FLIGHTNUMBER = "FlightNumber";
    public static final String GLOBAL_COORDINATES="coordinates";

    private Connection connection;

    private String url;

    private String name;

    private String description;
 
    private  Hashtable         globals;

    private Hashtable          missingMap;

    public EolDbTrackAdapter(String filename, Hashtable pointDataFilter,
                             int stride, int lastNMinutes)
        throws Exception {
        super(filename, pointDataFilter, stride, lastNMinutes);
        Class.forName("org.postgresql.Driver");
        initConnection();
    }

    public String getDataSourceName() {
        return name;
    }


    public String getDataSourceDescription() {
        return description;
    }

    private Connection getConnection() {
        if(connection == null) {
            try {
                connection = DriverManager.getConnection(getFilename(), "ads", "");
            } catch(SQLException sqe) {
                throw new BadDataException ("Unable to make connection to database", sqe);
            }
        }
        return connection;
    }



    private ResultSet select(String what, String where) throws SQLException {
        return evaluate(SqlUtils.makeSelect(what,where));
    }

    private ResultSet evaluate(String sql) throws SQLException {
        System.err.println ("EVAL: " + sql);
        Statement stmt = getConnection().createStatement();
        return  stmt.executeQuery(sql);
    }

    private void initConnection() throws Exception {
        getConnection();
        EolDbTrackInfo trackInfo = new EolDbTrackInfo(this, "TRACK");
        Hashtable cats = new Hashtable();
        try {
            ResultSet catResults  = select("*", TABLE_CATEGORIES);
            while(catResults.next()) {
                cats.put(catResults.getString(COL_VARIABLE),
                         catResults.getString(COL_CATEGORY));
            }
        } catch(Exception exc) {
            //                exc.printStackTrace();
        }

        missingMap = new Hashtable();
        ResultSet globalResults  = select("*", TABLE_GLOBALS);
        globals = new Hashtable();
        boolean gotCoords = false;
        description = "<b>Globals</b><br>";
        while(globalResults.next()) {
            String globalName = globalResults.getString(1).trim();            
            String globalValue = globalResults.getString(2).trim();
            globals.put(globalName,globalValue);
            description = description +"<tr valign=\"top\"><td>"+globalName+"</td><td>" + globalValue +"</td></tr>";

            if(globalName.equals(GLOBAL_COORDINATES)) {
                List toks = StringUtil.split(globalValue, " ", true, true);
                if(toks.size()!=4) {
                    throw new BadDataException ("Incorrect coordinates value in database:" +  globalValue);
                }
                gotCoords = true;
                System.err.println("coords:" + toks);
                trackInfo.setCoordinateVars((String) toks.get(0),
                                            (String) toks.get(1),
                                            (String) toks.get(2),
                                            (String) toks.get(3));

                trackInfo.setCoordinateVars("GGLON","GGLAT","GGALT","datetime");
            }
        }
        description = description+"</table>";

        if(!gotCoords) {
            throw new BadDataException ("No coordinates found in database");
        }

        this.name = (String) globals.get(GLOBAL_PROJECTNAME);
        String flight = (String)globals.get(GLOBAL_FLIGHTNUMBER);
        if(this.name!=null  && flight!=null && flight.length()!=0) {
            this.name += " - " + flight;
        }

        ResultSet results  = select("*",TABLE_VARIABLE_LIST);
        while(results.next()) {
            String name = results.getString(COL_NAME).trim();
            String desc = results.getString(COL_LONG_NAME).trim();
            String unit  = results.getString(COL_UNITS).trim();            
            double missing  = results.getDouble(COL_MISSING_VALUE);
            missingMap.put(name, new Double(missing));
            String cat = (String) cats.get(name);
            TrackInfo.Variable variable = new TrackInfo.Variable(name, desc, unit);
            if(cat!=null) {
                variable.setCategory(cat);
            }
            trackInfo.addVariable(variable);
        }
        addTrackInfo(trackInfo);
    }

    protected double getMissingValue(String var) {
        Double d = (Double)missingMap.get(var);
        if(d!=null) return d.doubleValue();
        return Double.NaN;
    }            


    public  class EolDbTrackInfo extends TrackInfo {
        public EolDbTrackInfo(EolDbTrackAdapter adapter, String trackName) 
            throws Exception {
            super(adapter, trackName);
        }

        private int numberOfPoints = -1;
        public int getNumberPoints() 
            throws Exception {
            if(numberOfPoints<0) {
                ResultSet results  = select("COUNT(" + varTime +")",  TABLE_DATA);
                int cnt = 0;
                while(results.next()) {
                    cnt++;
                }
                numberOfPoints= cnt;
            } 
            return numberOfPoints;
        }

        protected  double[] getDoubleData(Range range, String var) throws Exception {
            return SqlUtils.readDouble(select(var,TABLE_DATA), 1, getMissingValue(var));
        }

        protected  double[] getTime(Range range) throws Exception {
            return SqlUtils.readTime(select(varTime,TABLE_DATA), 1);
        }

        protected  float[] getFloatData(Range range, String var)  throws Exception {
            float[]f= SqlUtils.readFloat(select(var,TABLE_DATA), 1, (float)getMissingValue(var));
            if(f.length==0) throw new BadDataException("No observations found in data base");
            return f;
        }

        protected  String[] getStringData(Range range, String var)  throws Exception {
            return SqlUtils.readString(select(var,TABLE_DATA), 1);
        }

    }

}

