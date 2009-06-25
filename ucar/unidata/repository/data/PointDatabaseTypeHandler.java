/**
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


package ucar.unidata.repository.data;


import org.w3c.dom.*;

import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;


import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;


import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dt.GridDatatype;

//import ucar.nc2.dt.PointObsDataset;
//import ucar.nc2.dt.PointObsDatatype;

import ucar.nc2.dt.TrajectoryObsDataset;
import ucar.nc2.dt.TrajectoryObsDatatype;
import ucar.nc2.dt.TypedDatasetFactory;

import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.grid.NetcdfCFWriter;
import ucar.nc2.dt.trajectory.TrajectoryObsDatasetFactory;

import ucar.nc2.ft.FeatureCollection;

import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NestedPointFeatureCollection;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.ft.point.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.output.OutputHandler;
import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import javax.swing.*;
import java.awt.*;
import java.awt.Image;
import java.awt.image.*;
import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.GregorianCalendar;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.List;




import org.jfree.chart.*;
import org.jfree.chart.annotations.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.entity.*;
import org.jfree.chart.event.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.general.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;





/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class PointDatabaseTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static String TYPE_POINTDATABASE = "pointdatabase";

    public static final String RESULT_HTML = "html";
    public static final String RESULT_TIMESERIES = "timeseries";
    public static final String RESULT_TIMESERIES_IMAGE = "timeseries_image";
    public static final String RESULT_SCATTERPLOT = "scatterplot";
    public static final String RESULT_SCATTERPLOT_IMAGE = "scatterplot_image";
    public static final String RESULT_TIMELINE = "timeline";
    public static final String RESULT_CSV = "csv";
    public static final String RESULT_NETCDF = "netcdf";
    public static final String RESULT_MAP = "map";


    public static final double MISSING = -987654.98765;
 
    public static final String ARG_POINT_CHANGETYPE = "point_changetype";

    public static final String ARG_POINT_IMAGE_WIDTH = "point_image_width";
    public static final String ARG_POINT_IMAGE_HEIGHT = "point_image_height";

    public static final String ARG_POINT_SHOW_METADATA = "point_showmetadata";
    public static final String ARG_POINT_SHOW_DEFAULT = "point_showdefault";

    public static final String ARG_POINT_OUTPUT = "point_output";
    public static final String ARG_POINT_SEARCH = "point_search";
    public static final String ARG_POINT_FROMDATE = "point_fromdate";
    public static final String ARG_POINT_TODATE = "point_todate";
    public static final String ARG_POINT_BBOX = "point_bbox";
    public static final String ARG_POINT_HOUR   = "point_hour";
    public static final String ARG_POINT_MONTH  = "point_month";
    public static final String ARG_POINT_WHAT   = "point_what";
    public static final String ARG_POINT_WHAT_ALL   = "point_what_all";
    public static final String ARG_POINT_FIELD_VALUE = "point_field_value_";
    public static final String ARG_POINT_FIELD_EXACT = "point_field_exact_";
    public static final String ARG_POINT_FIELD_OP = "point_field_op_";


    public static final String OP_LT = "op_lt";
    public static final String OP_GT = "op_gt";
    public static final String OP_EQUALS = "op_equals";

    public static final String COL_LATITUDE = "ob_latitude";
    public static final String COL_LONGITUDE = "ob_longitude";
    public static final String COL_ALTITUDE = "ob_altitude";
    public static final String COL_DATE = "ob_date";
    public static final String COL_MONTH = "ob_month";
    public static final String COL_HOUR= "ob_hour";

    public static final int NUM_BASIC_COLUMNS = 6;

    private  List<TwoFacedObject> months;
    private  List hours;

    private Hashtable<String,List<PointDataMetadata>> metadataCache = new Hashtable<String,List<PointDataMetadata>>();

    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public PointDatabaseTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private String getTableName(String id) {
        id = id.replace("-", "_");
        return "pt_" + id;
    }

    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     */
    private String getTableName(Entry entry) {
        return getTableName(entry.getId());
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntry(Request request, Entry entry)
        throws Exception {
        Connection connection = getDatabaseManager().getNewConnection();
        connection.setAutoCommit(false);
        try {
            createDatabase(request, entry, connection);
            connection.commit();
            connection.setAutoCommit(true);
        } catch (Exception exc) {
            try {
                connection.close();
            } catch(Exception ignore) {}
            try {
                System.err.println("error:" + exc);
                exc.printStackTrace();
                deleteFromDatabase(getTableName(entry));
            } catch (Exception ignore) {}
            throw exc;
        } finally {
            try {
                connection.close();
            } catch(Exception ignore) {}
        }
    }


    private void createDatabase(Request request, Entry entry,  Connection connection) throws Exception {
        String       tableName = getTableName(entry);
        List<PointDataMetadata> metadata = new ArrayList<PointDataMetadata>();
        List<PointDataMetadata> stringMetadata = new ArrayList<PointDataMetadata>();
        List<PointDataMetadata> numericMetadata = new ArrayList<PointDataMetadata>();

        metadata.add(new PointDataMetadata(tableName, COL_LATITUDE,
                                           metadata.size(),
                                           "degrees",PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_LONGITUDE,
                                           metadata.size(),
                                           "degrees",  PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_ALTITUDE,
                                           metadata.size(),
                                           "m", PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_DATE,
                                           metadata.size(),
                                           "",
                                           PointDataMetadata.TYPE_DATE
                                           ));
        metadata.add(new PointDataMetadata(tableName, COL_MONTH,
                                           metadata.size(),
                                           "",
                                           PointDataMetadata.TYPE_INT
                                           ));
        metadata.add(new PointDataMetadata(tableName, COL_HOUR,
                                           metadata.size(),
                                           "",
                                           PointDataMetadata.TYPE_INT
                                           ));


        FeatureDatasetPoint     fdp      = getDataset(entry);


        List vars = fdp.getDataVariables();
        for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
            String unit = var.getUnitsString();
            if (unit == null) {
                unit = "";
            }
            DataType type    = var.getDataType();
            String   varName = var.getShortName();
            String   colName = SqlUtil.cleanName(varName).trim();
            if (colName.equals("latitude") || colName.equals("longitude")
                || colName.equals("altitude") || colName.equals("date") || colName.equals("time")) {
                continue;
            }
            if (colName.length() == 0) {
                continue;
            }
            colName = "ob_" + colName;
            boolean isString = var.getDataType().equals(DataType.STRING) ||
                var.getDataType().equals(DataType.CHAR);

            List<PointDataMetadata> listToAddTo = (isString?stringMetadata:numericMetadata);
            listToAddTo.add(new PointDataMetadata(tableName, colName,
                                               metadata.size(),varName,
                                               var.getName(), unit,
                                               (isString
                                                ? PointDataMetadata.TYPE_STRING
                                                : PointDataMetadata.TYPE_DOUBLE)));
        }
        for(PointDataMetadata pdm: stringMetadata) {
            pdm.columnNumber = metadata.size();
            metadata.add(pdm);
        }
        for(PointDataMetadata pdm: numericMetadata) {
            pdm.columnNumber = metadata.size();
            metadata.add(pdm);
        }




        StringBuffer sql       = new StringBuffer();
        List<String>indexSql = new ArrayList<String>();
        int indexCnt =0;

        sql.append("CREATE TABLE ");
        sql.append(tableName);
        sql.append(" (");
        boolean first = true;
        for(PointDataMetadata pdm: metadata) {
            if(!first) 
                sql.append(",");
            first = false;
            sql.append(pdm.columnName);
            sql.append(" ");
            sql.append(pdm.getDatabaseType());
            if(pdm.isBasic()) {
                indexSql.add("CREATE INDEX " + tableName + "_I" + (indexCnt++)+" ON "
                               + tableName + " (" + pdm.columnName+");");
            }
        }
        sql.append(")");
        //        System.err.println(sql);
        getDatabaseManager().execute(connection,
                                     getDatabaseManager().convertSql(sql.toString()), 1000, 10000);

        Statement statement = connection.createStatement();
        for(String index: indexSql) {
            SqlUtil.loadSql(index, statement, false, false);
        }
        statement.close();


        for (PointDataMetadata pdm : metadata) {
            getDatabaseManager().executeInsert(connection,
                                               Tables.POINTDATAMETADATA.INSERT, pdm.getValues());
        }
        insertData(entry, metadata, fdp, connection,true);
    }


    private double checkWriteValue(double v) {
        if(v!=v) return MISSING;
        return v;
    }


    private double checkReadValue(double v) {
        if(v== MISSING)return Double.NaN;
        return v;
    }



    private void insertData(Entry entry, List<PointDataMetadata> metadata, FeatureDatasetPoint  fdp, Connection connection, boolean newEntry) throws Exception {
        String       tableName = getTableName(entry);
        String[] ARRAY = new String[metadata.size()];
        for (PointDataMetadata pdm : metadata) {
            ARRAY[pdm.columnNumber] = pdm.columnName;
        }
        String insertString = SqlUtil.makeInsert(tableName,
                                                 SqlUtil.commaNoDot(ARRAY),
                                                 SqlUtil.getQuestionMarks(ARRAY.length));


        long minTime = (newEntry?Long.MAX_VALUE:entry.getStartDate());        
        long maxTime = (newEntry?Long.MIN_VALUE:entry.getEndDate());
        PreparedStatement insertStmt   = connection.prepareStatement(insertString);
        PointFeatureIterator pfi =
            DataOutputHandler.getPointIterator(fdp);
        Object[] values = new Object[metadata.size()];
        int      cnt    = 0;
        int batchCnt=0;
        GregorianCalendar calendar =
            new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        while (pfi.hasNext()) {
            PointFeature po = (PointFeature) pfi.next();
            ucar.unidata.geoloc.EarthLocation el = po.getLocation();
            if (el == null) {
                continue;
            }
            double        lat       = el.getLatitude();
            double        lon       = el.getLongitude();
            double        alt       = el.getAltitude();
            Date          time      = po.getNominalTimeAsDate();
            long tmpTime = time.getTime();
            if(tmpTime<minTime) minTime  = tmpTime;
            if(tmpTime>maxTime) maxTime  = tmpTime;
            calendar.setTime(time);
            StructureData structure = po.getData();
            

            boolean hadAnyNumericValues = false;
            boolean hadGoodNumericValue = false;


            for (PointDataMetadata pdm : metadata) {
                Object value;
                if (COL_LATITUDE.equals(pdm.columnName)) {
                    value = new Double(checkWriteValue(lat));
                } else if (COL_LONGITUDE.equals(pdm.columnName)) {
                    value = new Double(checkWriteValue(lon));
                } else if (COL_ALTITUDE.equals(pdm.columnName)) {
                    value = new Double(checkWriteValue(alt));
                } else if (COL_DATE.equals(pdm.columnName)) {
                    value = time;
                } else if (COL_HOUR.equals(pdm.columnName)) {
                    value = new Integer(calendar.get(GregorianCalendar.HOUR));
                } else if (COL_MONTH.equals(pdm.columnName)) {
                    value = new Integer(calendar.get(GregorianCalendar.MONTH));
                } else {
                    StructureMembers.Member member =
                        structure.findMember((String) pdm.shortName);
                    if (pdm.isString()) {
                        value = structure.getScalarString(member);
                    } else {
                        double d = structure.convertScalarFloat(member);
                        hadAnyNumericValues = true;
                        if(d==d) hadGoodNumericValue = true;
                        value = new Double(checkWriteValue(d));
                    }
                }
                values[pdm.columnNumber] = value;
            }
            if(hadAnyNumericValues && !hadGoodNumericValue) continue;
            getDatabaseManager().setValues(insertStmt, values);
            insertStmt.addBatch();
            batchCnt++;
            if(batchCnt>1000) {
                insertStmt.executeBatch();
                batchCnt=0;
            }
            if (((cnt++) % 1000) == 0) {
                System.err.println("added " + cnt +" observations");
            }
        }
        if(batchCnt>0) {
            insertStmt.executeBatch();
        }

        if(minTime!= Long.MAX_VALUE) {        
            entry.setStartDate(minTime);
            entry.setEndDate(maxTime);
        }

    }



    private Result doSearch(Request request, Entry entry) throws Exception {
        String result = request.getString(ARG_POINT_OUTPUT,RESULT_HTML);
        if(result.equals(RESULT_TIMESERIES)) {
            request.put(ARG_POINT_OUTPUT,RESULT_TIMESERIES_IMAGE);
            StringBuffer sb = new StringBuffer();
            sb.append(getHeader(request, entry));
            sb.append(HtmlUtil.img(request.getUrl()));
            return new Result("Search Results",sb);
        }

        if(result.equals(RESULT_SCATTERPLOT)) {
            request.put(ARG_POINT_OUTPUT,RESULT_SCATTERPLOT_IMAGE);
            StringBuffer sb = new StringBuffer();
            sb.append(getHeader(request, entry));
            sb.append(HtmlUtil.img(request.getUrl()));
            return new Result("Search Results",sb);
        }


        String       tableName = getTableName(entry);
        Date[] dateRange = request.getDateRange(ARG_POINT_FROMDATE, ARG_POINT_TODATE,
                                                new Date());
        List<Clause> clauses = new ArrayList<Clause>();

        if (dateRange[0] != null) {
            clauses.add(Clause.ge(COL_DATE, dateRange[0]));
        }

        if (dateRange[1] != null) {
            clauses.add(Clause.le(COL_DATE, dateRange[1]));
        }

        if(request.defined(ARG_POINT_BBOX+"_north")) {
            clauses.add(Clause.le(COL_LATITUDE, request.get(ARG_POINT_BBOX+"_north",90.0)));

        }


        if(request.defined(ARG_POINT_BBOX+"_south")) {
            clauses.add(Clause.ge(COL_LATITUDE, request.get(ARG_POINT_BBOX+"_south",90.0)));
        }


        if(request.defined(ARG_POINT_BBOX+"_west")) {
            clauses.add(Clause.ge(COL_LONGITUDE, request.get(ARG_POINT_BBOX+"_west",-180.0)));
        }

        if(request.defined(ARG_POINT_BBOX+"_east")) {
            clauses.add(Clause.le(COL_LONGITUDE, request.get(ARG_POINT_BBOX+"_east",180.0)));

        }

        if(request.defined(ARG_POINT_HOUR)) {
            clauses.add(Clause.eq(COL_HOUR, request.getString(ARG_POINT_HOUR)));
        }

        if(request.defined(ARG_POINT_MONTH)) {
            clauses.add(Clause.eq(COL_MONTH, request.getString(ARG_POINT_MONTH)));
        }

        List<PointDataMetadata>metadata  =  getMetadata(getTableName(entry));
        List<PointDataMetadata> columnsToUse = new ArrayList<PointDataMetadata>();
        if(request.get(ARG_POINT_WHAT_ALL,false)) {
            columnsToUse = metadata;
        } else {
            List<String> whatList = (List<String>)request.get(ARG_POINT_WHAT,new ArrayList());
            HashSet seen = new HashSet();
            for(String col: whatList) {
                seen.add(col);
            }
            for(PointDataMetadata pdm: metadata) {
                if(pdm.isBasic() || seen.contains(pdm.columnName)) {
                    columnsToUse.add(pdm);
                }
            }
        }


        for(PointDataMetadata pdm: metadata) {
            if(pdm.isBasic()) continue;
            if(!request.defined(ARG_POINT_FIELD_VALUE+pdm.columnName)) continue;
            if(pdm.isString()) {
                String value = request.getString(ARG_POINT_FIELD_VALUE+pdm.columnName,"");
                if(request.get(ARG_POINT_FIELD_EXACT+pdm.columnName,false)) {
                    clauses.add(Clause.eq(pdm.columnName, value));
                } else {
                    clauses.add(Clause.like(pdm.columnName, "%"+value+"%"));
                }
            } else {
                String op = request.getString(ARG_POINT_FIELD_OP+pdm.columnName,"");
                double value= request.get(ARG_POINT_FIELD_VALUE+pdm.columnName,0.0);
                if(op.equals(OP_LT)) 
                    clauses.add(Clause.le(pdm.columnName, value));
                else if(op.equals(OP_GT)) 
                   clauses.add(Clause.ge(pdm.columnName, value));
                else
                    clauses.add(Clause.eq(pdm.columnName, value));
            }
        }


        StringBuffer cols=null;

        for(PointDataMetadata pdm: columnsToUse) {
            if(cols == null) {
                cols = new StringBuffer();
            } else {
                cols.append(",");
            }
            cols.append(pdm.columnName);        
        }


        Statement stmt = getDatabaseManager().select(cols.toString(), Misc.newList(tableName), Clause.and(Clause.toArray(clauses)),
                                                     " ORDER BY " + COL_DATE+ " ASC ", request.get(ARG_MAX,1000));

        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        int cnt = 0;
        List<PointData> pointDataList = new ArrayList<PointData>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col =1;
                PointData pointData = new PointData(checkReadValue(results.getDouble(col++)),
                                                    checkReadValue(results.getDouble(col++)),
                                                    checkReadValue(results.getDouble(col++)),
                                                    getDatabaseManager().getDate(results, col++),
                                                    results.getInt(col++),
                                                    results.getInt(col++));
                List values = new ArrayList();
                while(col<=columnsToUse.size()) {
                    PointDataMetadata pdm = columnsToUse.get(col-1);
                    if(pdm.isString()) {
                        values.add(results.getString(col)); 
                    } else {
                        double d  = checkReadValue(results.getDouble(col));
                        values.add(new Double(d)); 
                    }
                    col++;
                }
                pointData.setValues(values);
                pointDataList.add(pointData);
            }
        }


        if(result.equals(RESULT_HTML) || result.equals(RESULT_TIMELINE)) {
            return makeSearchResultsHtml(request, entry, columnsToUse,pointDataList,result.equals(RESULT_TIMELINE));
        } else if(result.equals(RESULT_CSV)) {
            return makeSearchResultsCsv(request, entry, columnsToUse,pointDataList);
        } else if(result.equals(RESULT_TIMESERIES_IMAGE)) {
            return makeSearchResultsTimeSeries(request, entry, columnsToUse,pointDataList);

        } else if(result.equals(RESULT_SCATTERPLOT_IMAGE)) {
            return makeSearchResultsScatterPlot(request, entry, columnsToUse,pointDataList);
        } else if(result.equals(RESULT_MAP)) {
            return makeSearchResultsMap(request, entry, columnsToUse,pointDataList);
        }  else {
            return makeSearchResultsNetcdf(request, entry, columnsToUse,pointDataList);
        }
    }


    private static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
            "Point Data",  // title
            "Date",             // x-axis label
            "",   // y-axis label
            dataset,            // data
            true,               // create legend?
            true,               // generate tooltips?
            false               // generate URLs?
        );

        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        
        XYItemRenderer r = plot.getRenderer();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));
        
        return chart;

    }


    private Result makeSearchResultsHtml(Request request, Entry entry, List<PointDataMetadata> columnsToUse, List<PointData> list, boolean showTimeline) throws Exception {
        StringBuffer sb  = new StringBuffer();
        sb.append(getHeader(request, entry));
        sb.append(header(msg("Point Data Search Results")));
        if(list.size()==0) {
            sb.append(msg("No results found"));
            return new Result("Point Search Results",sb);
        }


        if(showTimeline) {
            String timelineAppletTemplate =
                getRepository().getResource(PROP_HTML_TIMELINEAPPLET);
            List times  = new ArrayList();
            List labels = new ArrayList();
            List ids    = new ArrayList();
            for(PointData pointData: list) {
                times.add(SqlUtil.format(pointData.date));
                labels.add(SqlUtil.format(pointData.date));
                ids.add("");
            }
            String tmp = StringUtil.replace(timelineAppletTemplate, "${times}",
                                            StringUtil.join(",", times));
            tmp = StringUtil.replace(tmp, "${root}",
                                     getRepository().getUrlBase());
            tmp = StringUtil.replace(tmp, "${labels}",
                                     StringUtil.join(",", labels));
            tmp = StringUtil.replace(tmp, "${ids}", StringUtil.join(",", ids));
            tmp = StringUtil.replace(
                                     tmp, "${loadurl}","");
            sb.append(tmp);
        }

        sb.append("<table>");
        StringBuffer header = new StringBuffer(HtmlUtil.cols(
                                                             HtmlUtil.b(msg("Date")),
                                                             HtmlUtil.b(msg("Latitude")),
                                                             HtmlUtil.b(msg("Longitude")),
                                                             HtmlUtil.b(msg("Altitude"))));
        for(PointDataMetadata pdm: columnsToUse) {
            if(pdm.isBasic()) continue;
            header.append(HtmlUtil.cols(HtmlUtil.b(pdm.formatName()+" " + pdm.formatUnit())));
        }
        sb.append(HtmlUtil.row(header.toString(),HtmlUtil.attr(HtmlUtil.ATTR_ALIGN, "center")));



        for(PointData pointData: list) {
            StringBuffer row = new StringBuffer();
            row.append(HtmlUtil.cols(
                                     pointData.date.toString(),
                                     ""+pointData.lat,
                                     ""+pointData.lon,
                                     ""+pointData.alt));
            List values = pointData.values;
            int cnt = -1;
            for(PointDataMetadata pdm: columnsToUse) {
                if(pdm.isBasic()) continue;
                cnt++;
                Object value = values.get(cnt);
                row.append(HtmlUtil.cols(""+value));
            }
            sb.append(HtmlUtil.row(row.toString(),HtmlUtil.attr(HtmlUtil.ATTR_ALIGN, "right")));
        }
        sb.append("</table>");
        return new Result("Point Search Results",sb);
    }




    private Result makeSearchResultsTimeSeries(Request request, Entry entry, List<PointDataMetadata> columnsToUse, List<PointData> list) throws Exception {
        StringBuffer sb  = new StringBuffer();
        sb.append(getHeader(request, entry));
        sb.append(header(msg("Point Data Search Results")));
        if(list.size()==0) {
            sb.append(msg("No results found"));
            return new Result("Point Search Results",sb);
        }

        TimeSeriesCollection dummy = new TimeSeriesCollection();
        JFreeChart chart = createChart(dummy);
        XYPlot xyPlot = (XYPlot) chart.getPlot();

        Hashtable<String,TimeSeries> seriesMap = new Hashtable<String,TimeSeries>();
        int paramCount = 0;
        int colorCount = 0;
        for(PointData pointData: list) {
            List values = pointData.values;
            int cnt = -1;
            for(PointDataMetadata pdm: columnsToUse) {
                if(pdm.isBasic()) continue;
                cnt++;
                Object value = values.get(cnt);
                if(pdm.isNumeric()) {
                    TimeSeries series = seriesMap.get(pdm.columnName);
                    if(series==null) {
                        paramCount++;
                        TimeSeriesCollection dataset = new TimeSeriesCollection();
                        series = new TimeSeries(pdm.shortName,
                                                Millisecond.class);
                        ValueAxis rangeAxis = new NumberAxis(pdm.shortName);
                        XYItemRenderer renderer = new XYAreaRenderer(XYAreaRenderer.LINES);
                        if(colorCount>=GuiUtils.COLORS.length) {
                            colorCount=0;
                        }
                        renderer.setSeriesPaint(0, GuiUtils.COLORS[colorCount]);
                        colorCount++;
                        xyPlot.setRenderer(paramCount, renderer);
                        xyPlot.setRangeAxis(paramCount, rangeAxis, false);
                        dataset.setDomainIsPointsInTime(true);
                        dataset.addSeries(series);
                        seriesMap.put(pdm.columnName, series);
                        xyPlot.setDataset(paramCount, dataset);
                        xyPlot.mapDatasetToRangeAxis(paramCount, paramCount);
                    }
                    series.addOrUpdate(new Millisecond(pointData.date), ((Double)value).doubleValue());
                }
            }
        }



        BufferedImage newImage = chart.createBufferedImage(
                                                           request.get(ARG_POINT_IMAGE_WIDTH,1000),
                                                           request.get(ARG_POINT_IMAGE_HEIGHT,400));
        File file = getStorageManager().getTmpFile(request, "point.png");
        ImageUtils.writeImageToFile(newImage, file);
        InputStream is = getStorageManager().getFileInputStream(file);
        Result result = new Result("",is,"image/png");
        return result;
    }




    private Result makeSearchResultsScatterPlot(Request request, Entry entry, List<PointDataMetadata> columnsToUse, List<PointData> list) throws Exception {
        StringBuffer sb  = new StringBuffer();
        /*
        sb.append(getHeader(request, entry));
        sb.append(header(msg("Point Data Search Results")));
        if(list.size()==0) {
            sb.append(msg("No results found"));
            return new Result("Point Search Results",sb);
        }

        XYSeries series = new XYSeries("Series 1");
        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createScatterPlot(
            "Scatter Plot Demo",
            "X", "Y", 
            dataset, 
            PlotOrientation.VERTICAL,
            true, 
            true, 
            false
        );


        XYPlot xyPlot = (XYPlot) chart.getPlot();
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesOutlinePaint(0, Color.black);
        renderer.setUseOutlinePaint(true);
        
        NumberAxis domainAxis = (NumberAxis) plot.getDomainAxis();
        domainAxis.setAutoRangeIncludesZero(false);


        double[][]data = new 
        series.add(newData);

        int paramCount = 0;
        int colorCount = 0;
        for(PointData pointData: list) {
            List values = pointData.values;
            int cnt = -1;
            for(PointDataMetadata pdm: columnsToUse) {
                if(pdm.isBasic()) continue;
                cnt++;
                Object value = values.get(cnt);
                if(pdm.isNumeric()) {
                }
            }
        }
        */

        /*

        BufferedImage newImage = chart.createBufferedImage(
                                                           request.get(ARG_POINT_IMAGE_WIDTH,800),
                                                           request.get(ARG_POINT_IMAGE_HEIGHT,800));
        File file = getStorageManager().getTmpFile(request, "point.png");
        ImageUtils.writeImageToFile(newImage, file);
        InputStream is = getStorageManager().getFileInputStream(file);
        Result result = new Result("",is,"image/png");
        return result;
        */
        return null;

    }




    private Result makeSearchResultsCsv(Request request, Entry entry, List<PointDataMetadata> columnsToUse, List<PointData> list) throws Exception {
        StringBuffer sb  = new StringBuffer();
        for(PointData pointData: list) {
            sb.append(pointData.lat+"/" + pointData.lon +"/" + pointData.alt + " " + pointData.date);
            sb.append(HtmlUtil.br());
        }
        return new Result("Point Search Results",sb);
    }


    private Result makeSearchResultsMap(Request request, Entry entry, List<PointDataMetadata> columnsToUse, List<PointData> list) throws Exception {
        StringBuffer sb  = new StringBuffer();
        sb.append(getHeader(request, entry));
        String   icon           = iconUrl("/icons/pointdata.gif");
        String              mapVarName = "mapstraction" + HtmlUtil.blockCnt++;
        StringBuffer        js         = new StringBuffer();
        js.append("var marker;\n");
        int      cnt            = 0;
        for(PointData pointData: list) {
            StringBuffer  info      = new StringBuffer("");
            cnt++;

            List values = pointData.values;
            int dcnt = -1;
            info.append("Date:"+pointData.date);
            info.append("<br>");
            for(PointDataMetadata pdm: columnsToUse) {
                if(pdm.isBasic()) continue;
                dcnt++;
                Object value = values.get(dcnt);
                info.append(pdm.shortName+":"+value);
                info.append("<br>");
            }

            js.append("marker = new Marker("
                      + DataOutputHandler.llp(pointData.lat, pointData.lon) + ");\n");

            js.append("marker.setIcon(" + HtmlUtil.quote(icon) + ");\n");

            js.append("marker.setInfoBubble(\"" + info.toString() + "\");\n");
            js.append("initMarker(marker," + HtmlUtil.quote("" + cnt) + ","
                      + mapVarName + ");\n");


        }
        js.append(mapVarName + ".autoCenterAndZoom();\n");
        //        js.append(mapVarName+".resizeTo(" + width + "," + height + ");\n");
        getRepository().initMap(request, mapVarName, sb,
                                request.get(ARG_WIDTH, 800),
                                request.get(ARG_HEIGHT, 500), true);
        sb.append(HtmlUtil.script(js.toString()));

        return new Result("Point Search Results",sb);
    }

    private Result makeSearchResultsNetcdf(Request request, Entry entry, List<PointDataMetadata> columnsToUse, List<PointData> list) throws Exception {
        StringBuffer sb  = new StringBuffer();
        for(PointData pointData: list) {
            sb.append(pointData.lat+"/" + pointData.lon +"/" + pointData.alt + " " + pointData.date);
            sb.append(HtmlUtil.br());
        }
        return new Result("Point Search Results",sb);
    }



    private String getHeader(Request request, Entry entry) throws Exception {
        List headerLinks = new ArrayList();
        boolean showMetadata = request.get(ARG_POINT_SHOW_METADATA,false);
        if(!showMetadata) {
            headerLinks.add(HtmlUtil.b(msg("Search form")));
            headerLinks.add(HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                                           entry,
                                                           ARG_POINT_SHOW_METADATA,"true"),msg("Metadata")));
        } else {
            headerLinks.add(HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                                           entry),msg("Search form")));

            headerLinks.add(HtmlUtil.b(msg("Metadata")));
        }

        headerLinks.add(HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                                       entry,
                                                       ARG_POINT_SHOW_DEFAULT,"true"),msg("Show default")));
        String header = StringUtil.join("&nbsp;|&nbsp;",
                                          headerLinks);
        return HtmlUtil.center(header);
    }

    public Result getHtmlDisplay(Request request, Entry entry) throws Exception {
        if(request.get(ARG_POINT_SHOW_DEFAULT,false)) return null;

        StringBuffer sb  = new StringBuffer();

        if(request.exists(ARG_POINT_SEARCH)) {
            return doSearch(request, entry);
        }


     
        boolean showMetadata = request.get(ARG_POINT_SHOW_METADATA,false);
        sb.append(getHeader(request, entry));
        if(showMetadata) {
            showMetadata(sb,request, entry);
        } else {
            createSearchForm(sb,request, entry);
        }
        return new Result("Point Data Search",sb);
    }





    private void initSelectors() {
        if(months ==null) {
            months  = Misc.toList(new Object[]{
            new TwoFacedObject("------",""),
            new TwoFacedObject("January",0),
            new TwoFacedObject("February",1),
            new TwoFacedObject("March",2),
            new TwoFacedObject("April",3),
            new TwoFacedObject("May",4),
            new TwoFacedObject("June",5),
            new TwoFacedObject("July",6),
            new TwoFacedObject("August",7),
            new TwoFacedObject("September",8),
            new TwoFacedObject("October",9),
            new TwoFacedObject("November",10),
            new TwoFacedObject("December",11)});
        }
        if(hours == null) {
            hours  = new ArrayList();
            hours.add(new TwoFacedObject("------",""));
            for(int i=0;i<24;i++) {
                hours.add(""+i);
            }
        }
    }



    private void showMetadata(StringBuffer sb, Request request, Entry entry) throws Exception {
        String tableName = getTableName(entry);
        boolean canEdit = getAccessManager().canDoAction(request, entry,
                                                         Permission.ACTION_EDIT);
                
        List<PointDataMetadata>metadata  =  getMetadata(getTableName(entry));
        if(canEdit && request.defined(ARG_POINT_CHANGETYPE)) {
            String column  = request.getString(ARG_POINT_CHANGETYPE,"");
            for(PointDataMetadata pdm: metadata) {
                if(pdm.isString() && pdm.columnName.equals(column)) {
                    if(pdm.varType.equals(pdm.TYPE_STRING)) {
                        pdm.varType = pdm.TYPE_ENUMERATION;
                    } else {
                        pdm.varType = pdm.TYPE_STRING;
                    }
                    getDatabaseManager().update(
                                   Tables.POINTDATAMETADATA.NAME,
                                   Clause.and(Clause.eq(Tables.POINTDATAMETADATA.COL_TABLENAME, tableName),
                                              Clause.eq(Tables.POINTDATAMETADATA.COL_COLUMNNAME, pdm.columnName)),
                                   new String[]{Tables.POINTDATAMETADATA.COL_VARTYPE},
                                   new String[]{pdm.varType});
                    break;
                }
            }
        }
        sb.append("<table>");
        sb.append(HtmlUtil.row(HtmlUtil.cols(
                                             HtmlUtil.b(msg("Short Name")),
                                             HtmlUtil.b(msg("Long Name")),
                                             HtmlUtil.b(msg("Unit Name")),
                                             HtmlUtil.b(msg("Type")))));
        for(PointDataMetadata pdm: metadata) {
            String type = pdm.varType;
            if(canEdit && pdm.isString()) {
                type  = HtmlUtil.href(request.entryUrl(getRepository().URL_ENTRY_SHOW,
                                                       entry,
                                                       ARG_POINT_SHOW_METADATA,"true",ARG_POINT_CHANGETYPE,pdm.columnName),type,HtmlUtil.title(msg("Change type")));

        }
        sb.append(HtmlUtil.row(HtmlUtil.cols(
                                             pdm.shortName,
                                             pdm.longName,
                                             pdm.unit,
                                             type)));
        }
        sb.append("</table>");
    }


    private void createSearchForm(StringBuffer sb, Request request, Entry entry) throws Exception {
        initSelectors();
        List<PointDataMetadata>metadata  =  getMetadata(getTableName(entry));
       String timezone = getEntryManager().getTimezone(entry);
       String tableName = getTableName(entry);




       Date fromDate = new Date(entry.getStartDate());
       Date toDate = new Date(entry.getEndDate());
        StringBuffer basicSB = new StringBuffer();

        basicSB.append(HtmlUtil.formTable());
        basicSB.append(HtmlUtil.hidden(ARG_OUTPUT,OutputHandler.OUTPUT_HTML));
        basicSB.append(HtmlUtil.hidden(ARG_ENTRYID,entry.getId()));

        basicSB.append(
                  HtmlUtil.formEntry(
                                     msgLabel("From Date"),
                                     getRepository().makeDateInput(
                                                                   request, ARG_POINT_FROMDATE, "pointsearch", fromDate,timezone)));

        basicSB.append(
                  HtmlUtil.formEntry(
                                     msgLabel("To Date"),
                                     getRepository().makeDateInput(
                                                                   request, ARG_POINT_TODATE, "pointsearch", toDate,timezone)));


        basicSB.append(
                  HtmlUtil.formEntry(
                                     msgLabel("Month"),
                                     HtmlUtil.select(ARG_POINT_MONTH, months, "")));

        basicSB.append(
                  HtmlUtil.formEntry(
                                     msgLabel("Hour"),
                                     HtmlUtil.select(ARG_POINT_HOUR, hours, "")));





        basicSB.append(HtmlUtil.formEntryTop(msgLabel("Location"),
                                             HtmlUtil.makeLatLonBox(ARG_POINT_BBOX, "","","","")));

        String max  = HtmlUtil.input(ARG_MAX,"1000",HtmlUtil.SIZE_5);
        basicSB.append(HtmlUtil.formEntry(msgLabel("Results"),HtmlUtil.select(ARG_POINT_OUTPUT,
                                                                              Misc.newList(RESULT_HTML,RESULT_TIMESERIES,RESULT_MAP,RESULT_CSV,RESULT_NETCDF))+HtmlUtil.space(2)+msgLabel("Max")+max));




        basicSB.append(HtmlUtil.formTableClose());


        StringBuffer extra = new StringBuffer();
        extra.append(HtmlUtil.formTable());
        List ops = Misc.newList(new TwoFacedObject("&lt;",OP_LT),new TwoFacedObject("&gt;",OP_GT),
                                new TwoFacedObject("=",OP_EQUALS));

        for(PointDataMetadata pdm: metadata) {
            if(pdm.isBasic()) continue;
            String suffix = HtmlUtil.space(1)+pdm.formatUnit();
            String label = pdm.formatName()+":";
            if(pdm.isEnumeration()) {
                List values = pdm.enumeratedValues;
                if(values == null) {
                    Statement stmt = getDatabaseManager().select(
                                                                 SqlUtil.distinct(pdm.columnName),
                                                                 tableName,(Clause)null);
                    values = Misc.toList(SqlUtil.readString(stmt, 1));
                    values.add(0,"");
                    getDatabaseManager().close(stmt);
                    pdm.enumeratedValues = values;
                }
                String field = HtmlUtil.select(ARG_POINT_FIELD_VALUE+pdm.columnName,values);
                extra.append(HtmlUtil.formEntry(label,field));
            } else  if(pdm.isString()) {
                String field = HtmlUtil.input(ARG_POINT_FIELD_VALUE+pdm.columnName,"",HtmlUtil.SIZE_20);
                String cbx = HtmlUtil.checkbox(ARG_POINT_FIELD_EXACT+pdm.columnName,"true",false) + " " + msg("Exact");
                extra.append(HtmlUtil.formEntry(label,field+" " + cbx));
            } else {
                String op = HtmlUtil.select(ARG_POINT_FIELD_OP+pdm.columnName,ops);
                String field = HtmlUtil.input(ARG_POINT_FIELD_VALUE+pdm.columnName,"",HtmlUtil.SIZE_10);
                extra.append(HtmlUtil.formEntry(label,op+" " +field+suffix));
            }
        }
        extra.append(HtmlUtil.formTableClose());


        StringBuffer what = new StringBuffer();
        what.append("<ul>");
        what.append(HtmlUtil.checkbox(ARG_POINT_WHAT_ALL,"true",false));
        what.append(HtmlUtil.space(1));
        what.append(msg("All"));
        what.append(HtmlUtil.br());
        for(PointDataMetadata pdm: metadata) {
            if(pdm.isBasic()) continue;
            what.append(HtmlUtil.checkbox(ARG_POINT_WHAT,pdm.columnName,pdm.isBasic()));
            what.append(HtmlUtil.space(1));
            what.append(pdm.formatName());
            what.append(HtmlUtil.br());
        }
        what.append("</ul>");


        //        sb.append(header(msg("Point Data Search")));
        sb.append(request.formPost(getRepository().URL_ENTRY_SHOW,
                               HtmlUtil.attr("name", "pointsearch")+HtmlUtil.id("pointsearch")));

        sb.append(HtmlUtil.submit(msg("Search"),ARG_POINT_SEARCH));
        sb.append(HtmlUtil.p());

        sb.append(HtmlUtil.makeShowHideBlock(msg("Basic"),
                                             basicSB.toString(),true));

        sb.append(HtmlUtil.makeShowHideBlock(msg("Advanced"),
                                             extra.toString(),false));

        sb.append(HtmlUtil.makeShowHideBlock(msg("What"),
                                             what.toString(),false));

        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.submit(msg("Search"),ARG_POINT_SEARCH));
        sb.append(HtmlUtil.formClose());




    }





    private List<PointDataMetadata> getMetadata(String tableName) throws Exception {
        List<PointDataMetadata> metadata = metadataCache.get(tableName);
        if(metadata == null) {
            if(metadataCache.size()>100) {
                metadataCache = new Hashtable<String,List<PointDataMetadata>>();
            }
            metadata = new ArrayList<PointDataMetadata>();
        Statement statement = getDatabaseManager().select(Tables.POINTDATAMETADATA.COLUMNS, Tables.POINTDATAMETADATA.NAME, 
                                                          Clause.eq(Tables.POINTDATAMETADATA.COL_TABLENAME,tableName),
                                                          " ORDER BY " + Tables.POINTDATAMETADATA.COL_COLUMNNUMBER + " ASC ");
        SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
        ResultSet        results;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col =1;
                metadata.add(new PointDataMetadata(results.getString(col++),
                                                   results.getString(col++),
                                                   results.getInt(col++),
                                                   results.getString(col++),
                                                   results.getString(col++),
                                                   results.getString(col++),
                                                   results.getString(col++)));
            }
        }
        metadataCache.put(tableName,metadata);
        }
        return metadata;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param statement _more_
     * @param id _more_
     *
     * @throws Exception _more_
     */
    public void deleteEntry(Request request, Statement statement, String id)
            throws Exception {
        super.deleteEntry(request, statement, id);
        String       tableName = getTableName(id);
        deleteFromDatabase(tableName);
    }

    private void deleteFromDatabase(String tableName) {
        StringBuffer sql       = new StringBuffer();
        try {
            sql.append("drop table " + tableName);
            getDatabaseManager().execute(sql.toString(), 1000, 10000);
        } catch(Exception ignore) {}
        try {
            getDatabaseManager().delete(
                                        Tables.POINTDATAMETADATA.NAME,
                                        Clause.eq(Tables.POINTDATAMETADATA.COL_TABLENAME, tableName));
        } catch(Exception ignore) {}
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, StringBuffer formBuffer,
                               Entry entry)
            throws Exception {
        super.addToEntryForm(request, formBuffer, entry);
    }


    /**
     * _more_
     *
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FeatureDatasetPoint getDataset(Entry entry) throws Exception {
        Formatter buf  = new Formatter();
        File      file = entry.getFile();
        getStorageManager().checkFile(file);
        FeatureDatasetPoint pods =
            (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                ucar.nc2.constants.FeatureType.POINT, file.toString(), null,
                buf);
        if (pods == null) {  // try as ANY_POINT
            pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(
                ucar.nc2.constants.FeatureType.ANY_POINT, file.toString(),
                null, buf);
        }
        return pods;
    }

    /**
     * Class PointDataMetadata _more_
     *
     *
     * @author IDV Development Team
     */
    public static class PointDataMetadata {

        /** _more_          */
        public static final String TYPE_STRING = "string";

        /** _more_          */
        public static final String TYPE_DOUBLE = "double";

        public static final String TYPE_INT = "int";

        /** _more_          */
        public static final String TYPE_DATE = "date";

        public static final String TYPE_ENUMERATION = "enumeration";


        /** _more_          */
        private String tableName;

        /** _more_          */
        private String columnName;

        /** _more_          */
        private String shortName;

        /** _more_          */
        private String longName;

        /** _more_          */
        private int columnNumber;

        /** _more_          */
        private String varType;

        /** _more_          */
        private String unit;

        private List enumeratedValues;

        /**
         * _more_
         *
         * @param tableName _more_
         * @param columnName _more_
         * @param type _more_
         * @param unit _more_
         * @param column _more_
         */
        public PointDataMetadata(String tableName, String columnName,
                                 int column,
                                 String unit,
                                 String type) {
            this(tableName, columnName, column, columnName, columnName, unit, type);
        }


        /**
         * _more_
         *
         * @param tableName _more_
         * @param columnName _more_
         * @param shortName _more_
         * @param longName _more_
         * @param type _more_
         * @param unit _more_
         * @param column _more_
         */
        public PointDataMetadata(String tableName, String columnName,
                                 int column,
                                 String shortName, String longName, 
                                 String unit,
                                 String type) {
            this.tableName    = tableName;
            this.columnName   = columnName;
            this.shortName    = shortName;
            this.longName     = longName;
            this.columnNumber = column;
            this.unit         = unit;
            this.varType      = type;

        }

        public String getDatabaseType() {
            if(varType.equals(TYPE_DOUBLE)) {
                return "ramadda.double";
            } else if(varType.equals(TYPE_INT)) {
                return "int";
            } else if(varType.equals(TYPE_DATE)) {
                return "ramadda.datetime";
            }
            return "varchar(1000)";
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public String toString() {
            return tableName + "." + columnName + " " + shortName + "  "
                   + varType;
        }

        public String formatName() {
            return shortName.replace("_"," ");
        }

        public String formatUnit() {
            if(unit == null || unit.length()==0) return "";
            return "["+unit +"]";

        }
        /**
         * _more_
         *
         * @return _more_
         */
        public Object[] getValues() {
            return new Object[] {
                tableName, columnName, new Integer(columnNumber), shortName,
                longName, unit, varType
            };
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isString() {
            return varType.equals(TYPE_STRING)|| varType.equals(TYPE_ENUMERATION);
        }

        public boolean isEnumeration() {
            return varType.equals(TYPE_ENUMERATION);
        }

        public boolean isNumeric() {
            return varType.equals(TYPE_DOUBLE)  || varType.equals(TYPE_INT);
        }

        public boolean isBasic() {
            return columnNumber<NUM_BASIC_COLUMNS;
        }

    }


    public static class PointData {
        double lat;
        double lon;
        double alt;
        Date date;
        int month;
        int hour;
        List values;
        public PointData(double lat,     double lon,   double alt,       Date date, int month, int hour) {
            this.lat = lat;        
            this.lon = lon;        
            this.alt = alt;
            this.date = date;
            this.month = month;
            this.hour = hour;
        }
        public void setValues(List values) {
            this.values = values;

        }
    }


}

