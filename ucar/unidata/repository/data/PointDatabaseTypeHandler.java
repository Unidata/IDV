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

import ucar.unidata.data.point.PointObFactory;


import ucar.unidata.data.point.TextPointDataSource;

import ucar.unidata.repository.*;
import ucar.unidata.repository.metadata.*;
import ucar.unidata.repository.output.OutputHandler;
import ucar.unidata.sql.Clause;


import org.w3c.dom.*;
import org.w3c.dom.Element;

import ucar.unidata.data.gis.KmlUtil;

import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;
import ucar.unidata.xml.XmlEncoder;

import visad.FieldImpl;

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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Date;
import java.util.Enumeration;
import java.util.Formatter;

import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;


import javax.swing.*;





/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class PointDatabaseTypeHandler extends GenericTypeHandler {

    public  static final String PROP_ID = "point.id";

    public  static final String PROP_CNT = "point.cnt";

    /** _more_ */
    public static String TYPE_POINTDATABASE = "pointdatabase";

    /** _more_          */
    public static final String FORMAT_HTML = "html";

    public static final String FORMAT_KML = "kml";

    /** _more_          */
    public static final String FORMAT_TIMESERIES = "timeseries";

    /** _more_          */
    public static final String FORMAT_TIMESERIES_IMAGE = "timeseries_image";

    /** _more_          */
    public static final String FORMAT_SCATTERPLOT = "scatterplot";

    /** _more_          */
    public static final String FORMAT_SCATTERPLOT_IMAGE = "scatterplot_image";

    /** _more_          */
    public static final String FORMAT_TIMELINE = "timeline";

    /** _more_          */
    public static final String FORMAT_CSV = "csv";

    public static final String FORMAT_CHART = "chart";

    /** _more_          */
    public static final String FORMAT_CSVHEADER = "csvheader";

    /** _more_          */
    public static final String FORMAT_CSVIDV = "csvidv";

    /** _more_          */
    public static final String FORMAT_XLS = "xls";

    /** _more_          */
    public static final String FORMAT_NETCDF = "netcdf";

    /** _more_          */
    public static final String FORMAT_MAP = "map";


    /** _more_          */
    public static final double MISSING = -987654.987654;

    /** _more_          */
    public static final String ARG_POINT_STRIDE = "stride";

    public static final String ARG_POINT_CHART_USETIMEFORNAME = "usetimeforname";

    public static final String ARG_POINT_REDIRECT = "redirect";
    public static final String ARG_POINT_CHANGETYPE = "changetype";

    public static final String ARG_POINT_ASCENDING = "ascending";

    public static final String ARG_POINT_SORTBY = "sortby";

    /** _more_          */
    public static final String ARG_POINT_UPLOAD_FILE = "upload_file";


    /** _more_          */
    public static final String ARG_POINT_IMAGE_WIDTH = "image_width";

    /** _more_          */
    public static final String ARG_POINT_IMAGE_HEIGHT = "image_height";

    public static final String ARG_POINT_VIEW = "pointview";

    public static final String VIEW_UPLOAD = "upload";
    public static final String VIEW_SEARCHFORM = "searchform";
    public static final String VIEW_METADATA = "metadata";
    public static final String VIEW_DEFAULT = "default";


    /** _more_          */
    public static final String ARG_POINT_FORMAT = "format";

    /** _more_          */
    public static final String ARG_POINT_SEARCH = "search";

    /** _more_          */
    public static final String ARG_POINT_FROMDATE = "fromdate";

    /** _more_          */
    public static final String ARG_POINT_TODATE = "todate";

    /** _more_          */
    public static final String ARG_POINT_BBOX = "bbox";

    /** _more_          */
    public static final String ARG_POINT_HOUR = "hour";

    /** _more_          */
    public static final String ARG_POINT_MONTH = "month";

    /** _more_          */
    public static final String ARG_POINT_PARAM = "what";

    /** _more_          */
    public static final String ARG_POINT_PARAM_ALL = "what_all";

    /** _more_          */
    public static final String ARG_POINT_FIELD_VALUE = "value_";

    /** _more_          */
    public static final String ARG_POINT_FIELD_EXACT = "exact_";

    /** _more_          */
    public static final String ARG_POINT_FIELD_OP = "op_";


    /** _more_          */
    public static final String OP_LT = "op_lt";

    /** _more_          */
    public static final String OP_GT = "op_gt";

    /** _more_          */
    public static final String OP_EQUALS = "op_equals";

    public static final String COL_ID = "obid";

    /** _more_          */
    public static final String COL_DATE = "obtime";

    /** _more_          */
    public static final String COL_LATITUDE = "latitude";

    /** _more_          */
    public static final String COL_LONGITUDE = "longitude";

    /** _more_          */
    public static final String COL_ALTITUDE = "altitude";

    /** _more_          */
    public static final String COL_MONTH = "obmonth";

    /** _more_          */
    public static final String COL_HOUR = "obhour";

    /** _more_          */
    public static final int NUM_BASIC_COLUMNS = 7;

    /** _more_          */
    private List<TwoFacedObject> months;

    /** _more_          */
    private List hours;

    /** _more_          */
    private Hashtable<String, List<PointDataMetadata>> metadataCache =
        new Hashtable<String, List<PointDataMetadata>>();


    private Hashtable<String, Hashtable> propertiesCache =
        new Hashtable<String, Hashtable>();

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


    private Hashtable  getProperties(Entry entry) throws Exception {
        Hashtable properties = propertiesCache.get(entry.getId());
        if(properties == null) {
            Object[] values       = entry.getValues();
            XmlEncoder   xmlEncoder = new XmlEncoder();
            if(values != null  && values.length>0 && values[0]!=null) {
                properties = (Hashtable) xmlEncoder.decodeXml((String)values[0]);
            } else {
                properties = new Hashtable();
            }
            propertiesCache.put(entry.getId(),properties);
        }
        return properties;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void initializeEntry(Request request, Entry entry, Group parent, boolean newEntry)
            throws Exception {
        if(!newEntry) return;
        Hashtable properties = getProperties(entry);

        Connection connection = getDatabaseManager().getNewConnection();
        connection.setAutoCommit(false);
        try {
            createDatabase(request, entry, parent, connection);
            connection.commit();
            connection.setAutoCommit(true);
            getEntryManager().addAttachment(
                entry, new File(entry.getResource().getPath()), false);
            entry.setResource(new Resource());
        } catch (Exception exc) {
            try {
                connection.close();
            } catch (Exception ignore) {}
            try {
                System.err.println("error:" + exc);
                exc.printStackTrace();
                deleteFromDatabase(getTableName(entry));
            } catch (Exception ignore) {}
            throw exc;
        } finally {
            try {
                connection.close();
            } catch (Exception ignore) {}
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param connection _more_
     *
     * @throws Exception _more_
     */
    private void createDatabase(Request request, Entry entry, Group parent,
                                Connection connection)
            throws Exception {

        String                  tableName = getTableName(entry);
        List<PointDataMetadata> metadata = new ArrayList<PointDataMetadata>();
        List<PointDataMetadata> stringMetadata =
            new ArrayList<PointDataMetadata>();
        List<PointDataMetadata> numericMetadata =
            new ArrayList<PointDataMetadata>();

        metadata.add(new PointDataMetadata(tableName, COL_ID,
                                           metadata.size(), "ID", "ID","",
                                           PointDataMetadata.TYPE_INT));
        metadata.add(new PointDataMetadata(tableName, COL_DATE,
                                           metadata.size(), "Observation Time", "Observation Time","",
                                           PointDataMetadata.TYPE_DATE));
        metadata.add(new PointDataMetadata(tableName, COL_LATITUDE,
                                           metadata.size(), "Latitude", "Latitude", "degrees",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_LONGITUDE,
                                           metadata.size(), "Longitude","Longitude","degrees",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_ALTITUDE,
                                           metadata.size(), "Altitude","Altitude","m",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_MONTH,
                                           metadata.size(), "",
                                           PointDataMetadata.TYPE_INT));
        metadata.add(new PointDataMetadata(tableName, COL_HOUR,
                                           metadata.size(), "",
                                           PointDataMetadata.TYPE_INT));


        FeatureDatasetPoint fdp  = getDataset(entry, parent, entry.getFile());
        if(fdp == null) {
            throw new IllegalArgumentException("Could not open file as point observation data");
        }

        List                vars = fdp.getDataVariables();

        for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
            //            System.err.println("   var:"+ var.getShortName());
            String unit = var.getUnitsString();
            if (unit == null) {
                unit = "";
            }
            DataType type    = var.getDataType();
            String   varName = var.getShortName();
            String   colName = SqlUtil.cleanName(varName).trim();
            if (colName.equals("latitude") || colName.equals("longitude")
                    || colName.equals("altitude") || colName.equals("date")
                    || colName.equals("time")) {
                continue;
            }
            if (colName.length() == 0) {
                continue;
            }
            colName = "ob_" + colName;
            boolean isString = var.getDataType().equals(DataType.STRING)
                               || var.getDataType().equals(DataType.CHAR);

            List<PointDataMetadata> listToAddTo = (isString
                    ? stringMetadata
                    : numericMetadata);
            listToAddTo.add(new PointDataMetadata(tableName, colName,
                    metadata.size(), varName, var.getName(), unit, (isString
                    ? PointDataMetadata.TYPE_STRING
                    : PointDataMetadata.TYPE_DOUBLE)));
        }
        for (PointDataMetadata pdm : stringMetadata) {
            pdm.columnNumber = metadata.size();
            metadata.add(pdm);
        }
        for (PointDataMetadata pdm : numericMetadata) {
            pdm.columnNumber = metadata.size();
            metadata.add(pdm);
        }




        StringBuffer sql      = new StringBuffer();
        List<String> indexSql = new ArrayList<String>();
        int          indexCnt = 0;

        sql.append("CREATE TABLE ");
        sql.append(tableName);
        sql.append(" (");
        boolean first = true;
        for (PointDataMetadata pdm : metadata) {
            if ( !first) {
                sql.append(",");
            }
            first = false;
            sql.append(pdm.columnName);
            sql.append(" ");
            sql.append(pdm.getDatabaseType());
            if (pdm.isBasic()) {
                indexSql.add("CREATE INDEX " + tableName + "_I"
                             + (indexCnt++) + " ON " + tableName + " ("
                             + pdm.columnName + ");");
            }
        }
        sql.append(")");
        //        System.err.println(sql);
        getDatabaseManager().execute(
            connection, getDatabaseManager().convertSql(sql.toString()),
            1000, 10000);

        Statement statement = connection.createStatement();
        for (String index : indexSql) {
            SqlUtil.loadSql(index, statement, false, false);
        }
        statement.close();


        for (PointDataMetadata pdm : metadata) {
            getDatabaseManager().executeInsert(connection,
                    Tables.POINTDATAMETADATA.INSERT, pdm.getValues());
        }
        insertData(entry, metadata, fdp, connection, true);
    }



    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    private double checkWriteValue(double v) {
        if (v != v) {
            return MISSING;
        }
        if(v == Double.POSITIVE_INFINITY) return Double.MAX_VALUE;
        if(v == Double.NEGATIVE_INFINITY) return -Double.MAX_VALUE;
        return v;
    }


    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    private double checkReadValue(double v) {
        if (v == MISSING) {
            return Double.NaN;
        }
        return v;
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param metadata _more_
     * @param fdp _more_
     * @param connection _more_
     * @param newEntry _more_
     *
     * @throws Exception _more_
     */
    private void insertData(Entry entry, List<PointDataMetadata> metadata,
                            FeatureDatasetPoint fdp, Connection connection,
                            boolean newEntry)
            throws Exception {

        String   tableName = getTableName(entry);
        String[] ARRAY     = new String[metadata.size()];
        for (PointDataMetadata pdm : metadata) {
            ARRAY[pdm.columnNumber] = pdm.columnName;
        }
        String insertString = SqlUtil.makeInsert(tableName,
                                  SqlUtil.commaNoDot(ARRAY),
                                  SqlUtil.getQuestionMarks(ARRAY.length));


        double north   = 0,
               south   = 0,
               east    = 0,
               west    = 0;

        long   minTime = (newEntry
                          ? Long.MAX_VALUE
                          : entry.getStartDate());
        long   maxTime = (newEntry
                          ? Long.MIN_VALUE
                          : entry.getEndDate());
        PreparedStatement insertStmt =
            connection.prepareStatement(insertString);
        PointFeatureIterator pfi = DataOutputHandler.getPointIterator(fdp);
        Object[]             values   = new Object[metadata.size()];
        int                  cnt      = 0;
        int                  batchCnt = 0;
        GregorianCalendar calendar =
            new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        boolean didone = false;

        Hashtable properties = getProperties(entry);
        int baseId = Misc.getProperty(properties,PROP_ID,0);
        int totalCnt = Misc.getProperty(properties,PROP_CNT,0);
        long t1 = System.currentTimeMillis();
        while (pfi.hasNext()) {
            PointFeature                      po = (PointFeature) pfi.next();
            ucar.unidata.geoloc.EarthLocation el = po.getLocation();
            if (el == null) {
                continue;
            }

            double lat     = el.getLatitude();
            double lon     = el.getLongitude();
            double alt     = el.getAltitude();
            Date   time    = po.getNominalTimeAsDate();

            //            if(totalCnt<5)
            //                System.err.println("altitiude:" + alt);
            long   tmpTime = time.getTime();
            if (tmpTime < minTime) {
                minTime = tmpTime;
            }
            if (tmpTime > maxTime) {
                maxTime = tmpTime;
            }

            if (didone) {
                north = Math.max(north, lat);
                south = Math.min(south, lat);
                west  = Math.min(west, lon);
                east  = Math.max(east, lon);
            } else {
                north = (newEntry
                         ? lat
                         : entry.hasNorth()
                           ? entry.getNorth()
                           : lat);
                south = (newEntry
                         ? lat
                         : entry.hasSouth()
                           ? entry.getSouth()
                           : lat);
                east  = (newEntry
                         ? lon
                         : entry.hasEast()
                           ? entry.getEast()
                           : lon);
                west  = (newEntry
                         ? lon
                         : entry.hasWest()
                           ? entry.getWest()
                           : lon);
            }
            didone = true;

            calendar.setTime(time);
            StructureData structure           = po.getData();
            boolean       hadAnyNumericValues = false;
            boolean       hadGoodNumericValue = false;
            /*
            if(totalCnt<5) {
                StructureMembers.Member member =
                    structure.findMember("altitude");
                if(member!=null) {
                    double d = structure.convertScalarFloat(member);
                } else {
                    System.err.println("no member");

                }
            }
            */


            for (PointDataMetadata pdm : metadata) {
                Object value;
                if (COL_ID.equals(pdm.columnName)) {
                    value = new Integer(baseId);
                    baseId++;
                } else if (COL_LATITUDE.equals(pdm.columnName)) {
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
                    value =
                        new Integer(calendar.get(GregorianCalendar.MONTH));
                } else {
                    StructureMembers.Member member =
                        structure.findMember((String) pdm.shortName);
                    if (pdm.isString()) {
                        value = structure.getScalarString(member);
                        if(value== null) value = "";
                        value = value.toString().trim();
                    } else {
                        double d = structure.convertScalarFloat(member);
                        hadAnyNumericValues = true;
                        if (d == d) {
                            hadGoodNumericValue = true;
                        }
                        value = new Double(checkWriteValue(d));
                    }
                }
                values[pdm.columnNumber] = value;
            }
            if (hadAnyNumericValues && !hadGoodNumericValue) {
                continue;
            }
            totalCnt++;
            getDatabaseManager().setValues(insertStmt, values);
            insertStmt.addBatch();
            batchCnt++;
            if (batchCnt > 1000) {
                insertStmt.executeBatch();
                batchCnt = 0;
            }
            if (((cnt++) % 5000) == 0) {
                System.err.println("added " + cnt + " observations");
            }
        }


        if (batchCnt > 0) {
            insertStmt.executeBatch();
        }
        insertStmt.close();

        long t2 = System.currentTimeMillis();
        System.err.println("inserted " + cnt +" observations in " + (t2-t1) +"ms");

        properties.put(PROP_CNT,totalCnt+"");
        properties.put(PROP_ID,baseId+"");
        XmlEncoder   xmlEncoder = new XmlEncoder();
        entry.setValues(new Object[]{xmlEncoder.encodeObject(properties)});

        if (didone) {
            entry.setWest(west);
            entry.setEast(east);
            entry.setNorth(north);
            entry.setSouth(south);
        }

        if (minTime != Long.MAX_VALUE) {
            entry.setStartDate(minTime);
            entry.setEndDate(maxTime);
        }


    }



    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private  void doUpload(StringBuffer sb, Request request, Entry entry) throws Exception {
        if (request.exists(ARG_POINT_UPLOAD_FILE)) {
            File file =
                new File(request.getUploadedFile(ARG_POINT_UPLOAD_FILE));
            FeatureDatasetPoint fdp = getDataset(entry, entry.getParentGroup(), file);
            List<PointDataMetadata> metadata =
                getMetadata(getTableName(entry));
            Connection connection = getDatabaseManager().getNewConnection();
            try {
                connection.setAutoCommit(false);
                insertData(entry, metadata, fdp, connection, true);
                connection.commit();
                getEntryManager().addAttachment(entry, file, true);
                for (PointDataMetadata pdm : metadata) {
                    pdm.enumeratedValues = null;
                }
            } finally {
                getDatabaseManager().closeConnection(connection);
            }
            sb.append(
                getRepository().showDialogNote("New data has been loaded"));
        } else {
            sb.append(request.uploadForm(getRepository().URL_ENTRY_SHOW));
            sb.append(msgLabel("New data file"));
            sb.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));
            sb.append(HtmlUtil.hidden(ARG_POINT_VIEW, VIEW_UPLOAD));

            sb.append(HtmlUtil.fileInput(ARG_POINT_UPLOAD_FILE,
                                         HtmlUtil.SIZE_50));
            sb.append(HtmlUtil.br());
            sb.append(HtmlUtil.submit("Add new data"));
            sb.append(request.uploadForm(getRepository().URL_ENTRY_SHOW));
            sb.append(HtmlUtil.formClose());
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result doSearch(Request request, Entry entry) throws Exception {

        String format = request.getString(ARG_POINT_FORMAT, FORMAT_HTML);
        String baseName =  IOUtil.stripExtension(entry.getName());
        boolean redirect = request.get(ARG_POINT_REDIRECT,false);
        request.remove(ARG_POINT_REDIRECT);
        request.remove(ARG_POINT_SEARCH);
        if (format.equals(FORMAT_TIMESERIES)) {
            StringBuffer sb = new StringBuffer();
            getHtmlHeader(request,  sb, entry, null);
            request.put(ARG_POINT_FORMAT, FORMAT_TIMESERIES_IMAGE);
            String redirectUrl = request.getRequestPath() + "/" + baseName
                              + ".png" + "?"
                              + request.getUrlArgs(null,
                                  Misc.newHashtable(OP_LT, OP_LT));
            sb.append(HtmlUtil.img(redirectUrl));
            return new Result("Search Results", sb);
        }

        if(redirect) {
            String urlSuffix= ".html";
            if (format.equals(FORMAT_CSV) || format.equals(FORMAT_CSVIDV)
                || format.equals(FORMAT_CSVHEADER)) {
                urlSuffix = ".csv";
            } else if(format.equals(FORMAT_XLS)) {
                urlSuffix = ".xls";
            } else if(format.equals(FORMAT_KML)) {
                urlSuffix = ".kml";
            } else if(format.equals(FORMAT_NETCDF)) {
                urlSuffix = ".nc";
            }

            String redirectUrl = request.getRequestPath() + "/" + HtmlUtil.urlEncode(baseName)
                              + urlSuffix + "?"
                              + request.getUrlArgs(null,
                                  Misc.newHashtable(OP_LT, OP_LT));
            return new Result(redirectUrl);
        }


        if (format.equals(FORMAT_SCATTERPLOT)) {}


        String tableName = getTableName(entry);
        Date[] dateRange = request.getDateRange(ARG_POINT_FROMDATE,
                               ARG_POINT_TODATE, null);
        List<Clause> clauses = new ArrayList<Clause>();

        if (dateRange[0] != null) {
            clauses.add(Clause.ge(COL_DATE, dateRange[0]));
        }

        if (dateRange[1] != null) {
            clauses.add(Clause.le(COL_DATE, dateRange[1]));
        }

        if (request.defined(ARG_POINT_BBOX + "_north")) {
            clauses.add(Clause.le(COL_LATITUDE,
                                  request.get(ARG_POINT_BBOX + "_north",
                                      90.0)));

        }


        if (request.defined(ARG_POINT_BBOX + "_south")) {
            clauses.add(Clause.ge(COL_LATITUDE,
                                  request.get(ARG_POINT_BBOX + "_south",
                                      90.0)));
        }


        if (request.defined(ARG_POINT_BBOX + "_west")) {
            clauses.add(Clause.ge(COL_LONGITUDE,
                                  request.get(ARG_POINT_BBOX + "_west",
                                      -180.0)));
        }

        if (request.defined(ARG_POINT_BBOX + "_east")) {
            clauses.add(Clause.le(COL_LONGITUDE,
                                  request.get(ARG_POINT_BBOX + "_east",
                                      180.0)));

        }

        if (request.defined(ARG_POINT_HOUR)) {
            clauses.add(Clause.eq(COL_HOUR,
                                  request.getString(ARG_POINT_HOUR)));
        }

        if (request.defined(ARG_POINT_MONTH)) {
            clauses.add(Clause.eq(COL_MONTH,
                                  request.getString(ARG_POINT_MONTH)));
        }

        List<PointDataMetadata> metadata = getMetadata(getTableName(entry));
        List<PointDataMetadata> tmp      = new ArrayList<PointDataMetadata>();
        if (request.get(ARG_POINT_PARAM_ALL, false)) {
            tmp = metadata;
        } else {
            List<String> whatList =
                (List<String>) request.get(ARG_POINT_PARAM, new ArrayList());
            HashSet seen = new HashSet();
            for (String col : whatList) {
                seen.add(col);
            }
            for (PointDataMetadata pdm : metadata) {
                if (seen.contains(""+pdm.columnNumber)) {
                    tmp.add(pdm);
                }
            }
        }


        //Strip out the month/hour
        List<PointDataMetadata> columnsToUse =
            new ArrayList<PointDataMetadata>();
        for (PointDataMetadata pdm : tmp) {
            //Skip the db month and hour
            if (pdm.columnName.equals(COL_MONTH)
                || pdm.columnName.equals(COL_HOUR)) {
                continue;
            }
            columnsToUse.add(pdm);
        }


        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic()&& !pdm.columnName.equals(COL_ALTITUDE)) {
                continue;
            }
            String suffix = ""+pdm.columnNumber;
            if ( !request.defined(ARG_POINT_FIELD_VALUE + suffix)) {
                continue;
            }
            if (pdm.isString()) {
                String value = request.getString(ARG_POINT_FIELD_VALUE
                                   + suffix, "");
                if (request.get(ARG_POINT_FIELD_EXACT + suffix,
                                false)) {
                    clauses.add(Clause.eq(pdm.columnName, value));
                } else {
                    clauses.add(Clause.like(pdm.columnName,
                                            "%" + value + "%"));
                }
            } else {
                String op = request.getString(ARG_POINT_FIELD_OP
                                              + suffix, OP_LT);
                double value = request.get(ARG_POINT_FIELD_VALUE
                                           + suffix, 0.0);
                if (op.equals(OP_LT)) {
                    clauses.add(Clause.le(pdm.columnName, value));
                } else if (op.equals(OP_GT)) {
                    clauses.add(Clause.ge(pdm.columnName, value));
                } else {
                    clauses.add(Clause.eq(pdm.columnName, value));
                }
            }
        }


        StringBuffer cols = null;
        List<PointDataMetadata> queryColumns = new ArrayList<PointDataMetadata>();

        //Always add the basic columns
        for (PointDataMetadata pdm : metadata) {
            if(pdm.isBasic()) {
                queryColumns.add(pdm);                
           }
        }

        for (PointDataMetadata pdm : columnsToUse) {
            if(!pdm.isBasic()) {
                queryColumns.add(pdm);
            }
        }


        for (PointDataMetadata pdm : queryColumns) {
            if (cols == null) {
                cols = new StringBuffer();
            } else {
                cols.append(",");
            }
            cols.append(pdm.columnName);
        }

        if (cols == null) {
            cols = new StringBuffer();
        }


        String sortByCol = COL_DATE;
        String orderDir = (request.get(ARG_POINT_ASCENDING,false)?" ASC ":" DESC ");

        String sortByArg = request.getString(ARG_POINT_SORTBY,"");
        for (PointDataMetadata pdm : metadata) {
            if(pdm.columnName.equals(sortByArg)) {
                sortByCol = sortByArg;
                break;
            }
        }

        int max = request.get(ARG_MAX,1000);
        int stride = request.get(ARG_POINT_STRIDE,1);
        if(stride>1) {
            max = max*stride;
        }
        Statement stmt = getDatabaseManager().select(cols.toString(),
                             Misc.newList(tableName),
                             Clause.and(Clause.toArray(clauses)),
                             " ORDER BY " + sortByCol + orderDir+
                                                     getDatabaseManager().getLimitString(request.get(ARG_SKIP,0), max),
                                                     max);

        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        int              cnt           = 0;
        List<PointData>  pointDataList = new ArrayList<PointData>();
        

        int skipHowMany = 0;
        while ((results = iter.next()) != null) {
            while (results.next()) {
                if(skipHowMany>0) {
                    skipHowMany--;
                    continue;
                }
                if(stride>1) {
                    skipHowMany = stride-1;
                }
                int col = 1;
                PointData pointData =
                    new PointData(results.getInt(col++),
                                  getDatabaseManager().getDate(results, col++), 
                                  checkReadValue(results.getDouble(col++)),
                                  checkReadValue(results.getDouble(col++)),
                                  checkReadValue(results.getDouble(col++)), 0,
                                  0);
                List values = new ArrayList();
                //Add in the selected basic values
                for(PointDataMetadata pdm: columnsToUse) {
                    if(pdm.isBasic()) {
                        values.add(pointData.getValue(pdm.columnName));
                    }
                }

                while (col <= queryColumns.size()) {
                    PointDataMetadata pdm = queryColumns.get(col - 1);
                    if (pdm.isString()) {
                        values.add(results.getString(col).trim());
                    } else {
                        double d = checkReadValue(results.getDouble(col));
                        values.add(new Double(d));
                    }
                    col++;
                }
                pointData.setValues(values);
                pointDataList.add(pointData);
            }
        }

        if (format.equals(FORMAT_HTML) || format.equals(FORMAT_TIMELINE)) {
            return makeSearchResultsHtml(request, entry, columnsToUse,
                                         pointDataList,
                                         format.equals(FORMAT_TIMELINE));
        }  if (format.equals(FORMAT_KML)) {
            return makeSearchResultsKml(request, entry, columnsToUse,
                                         pointDataList,
                                         format.equals(FORMAT_TIMELINE));
        } else if (format.equals(FORMAT_CSV) || format.equals(FORMAT_CSVIDV)
                   || format.equals(FORMAT_CSVHEADER)
                   || format.equals(FORMAT_XLS)) {
            return makeSearchResultsCsv(request, entry, columnsToUse,
                                        pointDataList, format);

        } else if (format.equals(FORMAT_CHART)) {
            return makeSearchResultsChart(request, entry, columnsToUse,
                                          pointDataList);
        } else if (format.equals(FORMAT_TIMESERIES_IMAGE)) {
            return makeSearchResultsTimeSeries(request, entry, columnsToUse,
                    pointDataList);

        } else if (format.equals(FORMAT_SCATTERPLOT_IMAGE)) {
            return makeSearchResultsScatterPlot(request, entry, columnsToUse,
                    pointDataList);
        } else if (format.equals(FORMAT_MAP)) {
            return makeSearchResultsMap(request, entry, columnsToUse,
                                        pointDataList);
        } else {
            return makeSearchResultsNetcdf(request, entry, columnsToUse,
                                           pointDataList);
        }

    }


    /**
     * _more_
     *
     * @param dataset _more_
     *
     * @return _more_
     */
    private static JFreeChart createChart(Request request, Entry entry,XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(entry.getName(),  // title
            "Date",   // x-axis label
            "",       // y-axis label
            dataset,  // data
            true,     // create legend?
            true,     // generate tooltips?
            false     // generate URLs?
                );

        chart.setBackgroundPaint(Color.white);
        ValueAxis rangeAxis = new NumberAxis("");
        rangeAxis.setVisible(false);
        XYPlot plot = (XYPlot) chart.getPlot();
        if(request.get("gray",false)) {
            plot.setBackgroundPaint(Color.lightGray);
            plot.setDomainGridlinePaint(Color.white);
            plot.setRangeGridlinePaint(Color.white);
        } else {
            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinePaint(Color.lightGray);
            plot.setRangeGridlinePaint(Color.lightGray);
        }
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.setRangeAxis(0, rangeAxis, false);


        XYItemRenderer r    = plot.getRenderer();
        DateAxis       axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

        return chart;

    }

    private void getHtmlHeader(Request request, StringBuffer sb, Entry entry, List<PointData> list) throws Exception 
    {
        StringBuffer searchForm = new StringBuffer();
        searchForm.append("<ul><hr>");
        createSearchForm(searchForm, request, entry);
        searchForm.append("<hr></ul>");

        StringBuffer cntSB =  new StringBuffer();

        int max = request.get(ARG_MAX, 1000);
        int numItems = (list==null?max:list.size());
        if ((numItems > 0) && ((numItems == max) || request.defined(ARG_SKIP))) {
            int skip = Math.max(0, request.get(ARG_SKIP, 0));
            cntSB.append(msgLabel("Showing") + (skip + 1) + "-" + (skip + numItems));
            List<String> toks = new ArrayList<String>();
            String url;
            if (skip > 0) {
                request.put(ARG_SKIP,(skip - max)+"");
                url = request.getRequestPath() +
                    "?" 
                    + request.getUrlArgs(null,
                                         Misc.newHashtable(OP_LT, OP_LT));
                request.put(ARG_SKIP,skip +"");
                toks.add(HtmlUtil.href(url, msg("Previous...")));
            }

            if (numItems >= max) {
                request.put(ARG_SKIP,(skip + max)+"");
                url = request.getRequestPath() +
                    "?" 
                    + request.getUrlArgs(null,
                                         Misc.newHashtable(OP_LT, OP_LT));
                request.put(ARG_SKIP,skip +"");
                toks.add(HtmlUtil.href(url, msg("Next...")));
            }


            if (numItems >= max) {
                request.put(ARG_MAX, "" + (max + 100));
                url = request.getRequestPath() +
                    "?" 
                    + request.getUrlArgs(null,
                                         Misc.newHashtable(OP_LT, OP_LT));
                toks.add(HtmlUtil.href(url, msg("View More")));
                request.put(ARG_MAX, "" + (max / 2));
                url = request.getRequestPath() +
                    "?" 
                    + request.getUrlArgs(null,
                                         Misc.newHashtable(OP_LT, OP_LT));
                toks.add(HtmlUtil.href(url, msg("View Less")));
            }

            request.put(ARG_SKIP, ""+skip);
            request.put(ARG_MAX, ""+max);
            if (toks.size() > 0) {
                cntSB.append(HtmlUtil.space(2));
                cntSB.append(StringUtil.join(HtmlUtil.span("&nbsp;|&nbsp;",
                        HtmlUtil.cssClass("separator")), toks));
            }
        }



        sb.append(getHeader(request, entry));


        sb.append(header(msg("Point Data Search Results")));
        sb.append(cntSB);
        sb.append(HtmlUtil.makeShowHideBlock(msg("Search Again"),
                                             searchForm.toString(), false));


        
    }

    



    private Result makeSearchResultsChart(
            Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        getHtmlHeader(request,  sb, entry, list);

        if (list.size() == 0) {
            sb.append(msg("No results found"));
            return new Result("Point Search Results", sb);
        }
        sb.append("<script type=\"text/javascript\" src=\"http://www.google.com/jsapi\"></script>\n");
        sb.append("<script type=\"text/javascript\">\ngoogle.load('visualization', '1', {'packages':['motionchart']});\ngoogle.setOnLoadCallback(drawChart);\nfunction drawChart() {\n        var data = new google.visualization.DataTable();\n");
        sb.append("data.addRows(" + list.size()+");\n");

        sb.append("data.addColumn('string', 'Location');\n");
        sb.append("data.addColumn('date', 'Date');\n");

        int baseCnt = 2;

        int entityIdx=-1;
        int idx =-1;
        boolean useTimeForName = request.get(ARG_POINT_CHART_USETIMEFORNAME,false);
        String           dateFormat  = "yyyy/MM/dd HH:mm:ss";
        SimpleDateFormat sdf         = new SimpleDateFormat(dateFormat);
        for (PointDataMetadata pdm : columnsToUse) {
            idx++;
            if (pdm.isBasic()) {
                continue;
            }
            if(entityIdx<0 && pdm.shortName.toLowerCase().indexOf("station")>=0) 
                entityIdx  = idx;
            if(pdm.shortName.toLowerCase().indexOf("name")>=0) 
                entityIdx  = idx;
            String varName=   pdm.formatName();
            varName = varName.replace("'","\\'");
            if(pdm.isString()) {
                sb.append("data.addColumn('string', '" + varName+"');\n");
            } else  if(pdm.isDate()) {
                sb.append("data.addColumn('string', '" + varName+"');\n");
            } else {
                sb.append("data.addColumn('number', '" + varName+"');\n");
            }
        }


        GregorianCalendar cal =
            new GregorianCalendar(DateUtil.TIMEZONE_GMT);

        int row=-1;

        sb.append("var theDate;\n");
        for (PointData pointData : list) {
            row++;
            cal.setTime(pointData.date);
            List values = pointData.values;
            String entityName;
            if(useTimeForName) {
                entityName = sdf.format(pointData.date);
            } else  if(entityIdx>=0)  {
                entityName = values.get(entityIdx).toString().trim();
            } else {
                entityName = "latlon_" + pointData.lat+"/"+pointData.lon;
            }

            entityName = entityName.replace("'","\\'");
            sb.append("data.setValue(" +row+", 0, '" +entityName + "');\n");

            sb.append("theDate = new Date(" + cal.get(cal.YEAR) +"," + cal.get(cal.MONTH) +","+ cal.get(cal.DAY_OF_MONTH)+ ");\n");

            sb.append("theDate.setHours("+cal.get(cal.HOUR)+
                      ","+ cal.get(cal.MINUTE)+
                      ","+ cal.get(cal.SECOND)+
                      ","+ cal.get(cal.MILLISECOND)+
                      ");\n");

            //            if(row<10)        sb.append("alert(theDate);\n");
            sb.append("data.setValue(" +row+", 1, theDate);\n");

            /*            sb.append("data.setValue(" +row+", 2," +pointData.lon+");\n");
                          sb.append("data.setValue(" +row+", 3,"+ pointData.lat+");\n");
                          sb.append("data.setValue(" +row+", 4,"+ pointData.alt+");\n");
                          sb.append("data.setValue(" +row+", 5,"+ pointData.month+");\n");
            */


            int  cnt    = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                cnt++;
                //Already did the entity
                if(cnt == entityIdx) continue;
                Object value = values.get(cnt);
                if(pdm.isString()) {
                    String tmp = value.toString().trim();
                    tmp = tmp.replace("'","\\'");
                    sb.append("data.setValue(" +row+", " + (cnt+baseCnt) +", '" +tmp +"');\n");
                } else  if(pdm.isDate()) {
                    sb.append("data.setValue(" +row+", " + (cnt+baseCnt) +", " +value +");\n");
                } else {
                    sb.append("data.setValue(" +row+", " + (cnt+baseCnt) +", " +value +");\n");
                }
            }
        }

        sb.append("var chart = new google.visualization.MotionChart(document.getElementById('chart_div'));\n");
        sb.append("chart.draw(data, {width: 800, height:500});\n");
        sb.append("}\n");
        sb.append("</script>\n");
        sb.append("<div id=\"chart_div\" style=\"width: 800px; height: 500px;\"></div>\n");
        return new Result("Point Search Results", sb);
    }


    private Result makeSearchResultsKml(
            Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list,
            boolean showTimeline)
            throws Exception {
        Element             root         = KmlUtil.kml(entry.getName());
        Element             docNode = KmlUtil.document(root, entry.getName());
        
        for (PointData pointData : list) {
            int lblCnt = 0;
            StringBuffer  info      = new StringBuffer("");
            StringBuffer  label      = new StringBuffer("");
            info.append("<b>Date:</b> " + pointData.date + "<br>");
            List values = pointData.values;
            int  cnt    = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                cnt++;
                if (pdm.isBasic()) {
                    continue;
                }
                Object value = values.get(cnt);
                if(lblCnt<4) {
                    if(lblCnt>0)
                        label.append("/");
                    lblCnt++;
                    if(value instanceof Double) {
                        value = Misc.format(((Double)value).doubleValue());
                        label.append(value+ pdm.formatUnit()+"");
                    } else {
                        label.append(""+value);
                    }
                }
                info.append(HtmlUtil.b(pdm.formatName())+":" +value+" " +pdm.formatUnit()+"<br>");
            }
            
            Element placemark  = KmlUtil.placemark(docNode, label.toString(),
                                                   info.toString(), pointData.lat, pointData.lon, pointData.alt, null);
            KmlUtil.timestamp(placemark, pointData.date);
        }
        
        StringBuffer sb = new StringBuffer(XmlUtil.toString(root));

        return new Result(msg("Point Data"), sb,
                          getRepository().getMimeTypeFromSuffix(".kml"));
        
    }





    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     * @param showTimeline _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsHtml(
            Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list,
            boolean showTimeline)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        getHtmlHeader(request,  sb, entry, list);

        if (list.size() == 0) {
            sb.append(msg("No results found"));
            return new Result("Point Search Results", sb);
        }


        if (showTimeline) {
            String timelineAppletTemplate =
                getRepository().getResource(PROP_HTML_TIMELINEAPPLET);
            List times  = new ArrayList();
            List labels = new ArrayList();
            List ids    = new ArrayList();
            for (PointData pointData : list) {
                times.add(SqlUtil.format(pointData.date));
                labels.add(SqlUtil.format(pointData.date));
                ids.add("");
            }
            String tmp = StringUtil.replace(timelineAppletTemplate,
                                            "${times}",
                                            StringUtil.join(",", times));
            tmp = StringUtil.replace(tmp, "${root}",
                                     getRepository().getUrlBase());
            tmp = StringUtil.replace(tmp, "${labels}",
                                     StringUtil.join(",", labels));
            tmp = StringUtil.replace(tmp, "${ids}",
                                     StringUtil.join(",", ids));
            tmp = StringUtil.replace(tmp, "${loadurl}", "");
            sb.append(tmp);
        }

        sb.append("<table>");
        StringBuffer header = new StringBuffer();
        for (PointDataMetadata pdm : columnsToUse) {
            header.append(HtmlUtil.cols(HtmlUtil.b(pdm.formatName() + " "
                    + pdm.formatUnit())));
        }
        sb.append(HtmlUtil.row(header.toString(),
                               HtmlUtil.attr(HtmlUtil.ATTR_ALIGN, "center")));



        for (PointData pointData : list) {
            StringBuffer row = new StringBuffer();
            List values = pointData.values;
            int  cnt    = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                cnt++;
                Object value = values.get(cnt);
                if(value instanceof Double) {
                    double d = ((Double)value).doubleValue();
                    row.append(HtmlUtil.cols("" + Misc.format(d)));
                } else {
                    row.append(HtmlUtil.cols("" + value));
                }
            }
            sb.append(HtmlUtil.row(row.toString(),
                                   HtmlUtil.attr(HtmlUtil.ATTR_ALIGN,
                                       "right")));
        }
        sb.append("</table>");
        return new Result("Point Search Results", sb);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsTimeSeries(Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(getHeader(request, entry));
        sb.append(header(msg("Point Data Search Results")));
        if (list.size() == 0) {
            sb.append(msg("No results found"));
            return new Result("Point Search Results", sb);
        }

        TimeSeriesCollection dummy  = new TimeSeriesCollection();
        JFreeChart           chart  = createChart(request,entry,dummy);
        XYPlot               xyPlot = (XYPlot) chart.getPlot();

        Hashtable<String, TimeSeries> seriesMap = new Hashtable<String,
                                                      TimeSeries>();
        int paramCount = 0;
        int colorCount = 0;
        boolean axisLeft = true;
        Hashtable<String,List<ValueAxis>> axisMap = new Hashtable<String,List<ValueAxis>>();
        Hashtable<String,double[]> rangeMap = new Hashtable<String,double[]>();
        List<String> units = new ArrayList<String>();

        for (PointData pointData : list) {
            List values = pointData.values;
            int  cnt    = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                cnt++;
                if (pdm.isBasic()) {
                    continue;
                }
                if (!pdm.isNumeric()) {continue;}
                double value = ((Double)values.get(cnt)).doubleValue();
                if(value!=value) continue;
                List<ValueAxis> axises = null;
                double[]range = null;
                if(pdm.hasUnit()) {
                    axises = axisMap.get(pdm.unit);
                    range = rangeMap.get(pdm.unit);
                    if(axises==null) {
                        axises= new ArrayList<ValueAxis>();
                        range = new double[]{value,value};
                        rangeMap.put(pdm.unit, range);
                        axisMap.put(pdm.unit, axises);
                        units.add(pdm.unit);
                    }
                    range[0] = Math.min(range[0],value);
                    range[1] = Math.max(range[1],value);
                }
                TimeSeries series = seriesMap.get(pdm.columnName);
                if (series == null) {
                    paramCount++;
                    TimeSeriesCollection dataset =
                        new TimeSeriesCollection();
                    series = new TimeSeries(pdm.formatName(),
                                            Millisecond.class);
                    ValueAxis rangeAxis = new NumberAxis(pdm.formatName()+" " +pdm.formatUnit());
                    if(axises!=null) axises.add(rangeAxis);
                    XYItemRenderer renderer =
                        new XYAreaRenderer(XYAreaRenderer.LINES);
                    if (colorCount >= GuiUtils.COLORS.length) {
                        colorCount = 0;
                    }
                    renderer.setSeriesPaint(0,
                                            GuiUtils.COLORS[colorCount]);
                    colorCount++;
                    xyPlot.setRenderer(paramCount, renderer);
                    xyPlot.setRangeAxis(paramCount, rangeAxis, false);
                    AxisLocation side        = (axisLeft?AxisLocation.TOP_OR_LEFT:AxisLocation.BOTTOM_OR_RIGHT);
                    axisLeft = !axisLeft;
                    xyPlot.setRangeAxisLocation(paramCount, side);

                    dataset.setDomainIsPointsInTime(true);
                    dataset.addSeries(series);
                    seriesMap.put(pdm.columnName, series);
                    xyPlot.setDataset(paramCount, dataset);
                    xyPlot.mapDatasetToRangeAxis(paramCount, paramCount);
                }
                series.addOrUpdate(new Millisecond(pointData.date),value);
            }
        }


        for(String unit: units) {
            List<ValueAxis> axises = axisMap.get(unit);
            double[]range = rangeMap.get(unit);
            for(ValueAxis rangeAxis: axises) {
                rangeAxis.setRange(new org.jfree.data.Range(range[0],range[1]));
            }
        }






        BufferedImage newImage =
            chart.createBufferedImage(request.get(ARG_POINT_IMAGE_WIDTH,
                1000), request.get(ARG_POINT_IMAGE_HEIGHT, 400));
        File file = getStorageManager().getTmpFile(request, "point.png");
        ImageUtils.writeImageToFile(newImage, file);
        InputStream is     = getStorageManager().getFileInputStream(file);
        Result      result = new Result("", is, "image/png");
        return result;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsScatterPlot(Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list)
            throws Exception {
        StringBuffer sb = new StringBuffer();
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
                cnt++;
                if(pdm.isBasic()) continue;
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




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     * @param format _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsCsv(Request request, Entry entry,
                                        List<PointDataMetadata> columnsToUse,
                                        List<PointData> list, String format)
            throws Exception {
        StringBuffer sb = getCsv(columnsToUse, list, format);
        return new Result("", sb, "text/csv");
    }


    /**
     * _more_
     *
     * @param columnsToUse _more_
     * @param list _more_
     * @param format _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private StringBuffer getCsv(List<PointDataMetadata> columnsToUse,
                                List<PointData> list, String format)
            throws Exception {
        boolean          addMetadata = format.equals(FORMAT_CSVIDV);
        boolean          addHeader   = format.equals(FORMAT_CSVHEADER);
        boolean          xls         = format.equals(FORMAT_XLS);

        String           dateFormat  = "yyyy/MM/dd HH:mm:ss Z";
        SimpleDateFormat sdf         = new SimpleDateFormat(dateFormat);
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        StringBuffer sb    = new StringBuffer();
        String       comma = ", ";
        List         rows  = new ArrayList();

        if (addHeader) {
            int cnt = 0;
            for (PointDataMetadata pdm : columnsToUse) {
                if(pdm.isObId()) continue;
                if (cnt != 0) {
                    sb.append(",");
                }
                sb.append(pdm.shortName);
                sb.append(pdm.formatUnit());
                cnt++;
            }
            sb.append("\n");
        }

        if (addMetadata) {
            StringBuffer h1 = new StringBuffer();
            StringBuffer h2 = new StringBuffer();
            h1.append("(index) -> (");
            int cnt = 0;
            for (PointDataMetadata pdm : columnsToUse) {
                if(pdm.isObId()) continue;
                if (cnt != 0) {
                    h1.append(",");
                    h2.append(",");
                }
                h1.append(pdm.shortName);
                h2.append(pdm.shortName);
                if (pdm.isString()) {
                    h1.append("(Text)");
                    h2.append("(Text)");
                } else {
                    h2.append("[");
                    if ((pdm.unit != null) && (pdm.unit.length() > 0)) {
                        h2.append(" unit=\"" + pdm.unit + "\" ");
                    }
                    if (pdm.varType.equals(pdm.TYPE_DATE)) {
                        h2.append(" fmt=\"" + dateFormat + "\" ");
                    }
                    h2.append("]");
                }
                cnt++;
            }
            h1.append(")");
            sb.append(h1);
            sb.append("\n");
            sb.append(h2);
            sb.append("\n");
        }


        for (PointData pointData : list) {
            sb.append(sdf.format(pointData.date));
            sb.append(comma);
            sb.append(pointData.lat);
            sb.append(comma);
            sb.append(pointData.lon);
            sb.append(comma);
            sb.append(pointData.alt);
            int  dcnt   = -1;
            List values = pointData.values;
            for (PointDataMetadata pdm : columnsToUse) {
                dcnt++;
                if (pdm.isBasic()) {
                    continue;
                }
                Object value = values.get(dcnt);
                sb.append(comma);
                sb.append(value);
            }
            sb.append("\n");
        }
        return sb;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsMap(Request request, Entry entry,
                                        List<PointDataMetadata> columnsToUse,
                                        List<PointData> list)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(getHeader(request, entry));
        String       icon       = iconUrl("/icons/pointdata.gif");
        String       mapVarName = "mapstraction" + HtmlUtil.blockCnt++;
        StringBuffer js         = new StringBuffer();
        js.append("var marker;\n");
        int cnt = 0;
        for (PointData pointData : list) {
            StringBuffer info = new StringBuffer("");
            cnt++;

            List values = pointData.values;
            info.append("Date:" + pointData.date);
            info.append("<br>");
            int dcnt = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                dcnt++;
                if (pdm.isBasic()) {
                    continue;
                }
                Object value = values.get(dcnt);
                info.append(pdm.shortName + ":" + value);
                info.append("<br>");
            }

            js.append("marker = new Marker("
                      + DataOutputHandler.llp(pointData.lat, pointData.lon)
                      + ");\n");

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

        return new Result("Point Search Results", sb);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param columnsToUse _more_
     * @param list _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Result makeSearchResultsNetcdf(
            Request request, Entry entry,
            List<PointDataMetadata> columnsToUse, List<PointData> list)
            throws Exception {

        TextPointDataSource dataSource = new TextPointDataSource("dummy.csv");
        StringBuffer        sb = getCsv(columnsToUse, list, FORMAT_CSVIDV);
        FieldImpl field = dataSource.makeObs(sb.toString(), ",", null, null,
                                             null, false, false);

        File file = getStorageManager().getTmpFile(request, "test.nc");
        PointObFactory.writeToNetcdf(file, field);
        Result result = new Result("", getStorageManager().getFileInputStream(file),
                          "application/x-netcdf");
        result.addHttpHeader(HtmlUtil.HTTP_CONTENT_LENGTH, "" + file.length());
        return result;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private String getHeader(Request request, Entry entry) throws Exception {
        boolean canEdit = getAccessManager().canDoAction(request, entry,
                              Permission.ACTION_EDIT);

        List    headerLinks  = new ArrayList();
        String view =  request.getString(ARG_POINT_VIEW, VIEW_SEARCHFORM);


        boolean doSearch     = request.exists(ARG_POINT_SEARCH) || request.exists(ARG_POINT_FORMAT);

        if (!doSearch && view.equals(VIEW_SEARCHFORM)) {
            headerLinks.add(HtmlUtil.b(msg("Search form")));
        } else {
            Object tmp1 = request.remove(ARG_POINT_SEARCH);
            Object tmp2  = request.remove(ARG_POINT_FORMAT);
            headerLinks.add(
                HtmlUtil.href(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                    msg("Search form")));

            if(tmp1!=null) request.put(ARG_POINT_SEARCH,tmp1);
            if(tmp2!=null) request.put(ARG_POINT_FORMAT,tmp2);
        }

        if (view.equals(VIEW_METADATA)) {
            headerLinks.add(HtmlUtil.b(msg("Metadata")));
        } else {
            headerLinks.add(
                HtmlUtil.href(
                    request.entryUrl(
                        getRepository().URL_ENTRY_SHOW, entry,
                        ARG_POINT_VIEW, VIEW_METADATA), msg("Metadata")));
        }

        if (canEdit) {
            if(view.equals(VIEW_UPLOAD)) {
                headerLinks.add(HtmlUtil.b(msg("Upload more data")));
            } else {
                headerLinks.add(
                    HtmlUtil.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW, entry,
                            ARG_POINT_VIEW, VIEW_UPLOAD), msg(
                                "Upload more data")));
            }
        }

        headerLinks.add(
            HtmlUtil.href(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry,
                    ARG_POINT_VIEW, VIEW_DEFAULT), msg("Show default")));
        String header = StringUtil.join("&nbsp;|&nbsp;", headerLinks);
        return HtmlUtil.center(header);
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result getHtmlDisplay(Request request, Entry entry)
            throws Exception {
        boolean canEdit = getAccessManager().canDoAction(request, entry,
                              Permission.ACTION_EDIT);

        String view = request.getString(ARG_POINT_VIEW,VIEW_SEARCHFORM);
        if (view.equals(VIEW_DEFAULT)) {
            return null;
        }

        boolean doSearch     = request.exists(ARG_POINT_SEARCH) || request.exists(ARG_POINT_FORMAT);
        if (doSearch) {
            return doSearch(request, entry);
        }

        StringBuffer sb      = new StringBuffer();
        if (view.equals(VIEW_METADATA)) {
            if(request.getString(ARG_RESPONSE,"").equals("xml")) {
                return showMetadataXml(request, entry);
            }
            sb.append(getHeader(request, entry));
            showMetadata(sb, request, entry);
        }  else if (view.equals(VIEW_UPLOAD)) {
            sb.append(getHeader(request, entry));
            doUpload(sb, request, entry);
        } else {
            sb.append(getHeader(request, entry));
            createSearchForm(sb, request, entry);
        }
        return new Result("Point Data", sb);
    }





    /**
     * _more_
     */
    private void initSelectors() {
        if (months == null) {
            months = Misc.toList(new Object[] {
                new TwoFacedObject("------", ""),
                new TwoFacedObject("January", 0),
                new TwoFacedObject("February", 1),
                new TwoFacedObject("March", 2),
                new TwoFacedObject("April", 3), new TwoFacedObject("May", 4),
                new TwoFacedObject("June", 5), new TwoFacedObject("July", 6),
                new TwoFacedObject("August", 7),
                new TwoFacedObject("September", 8),
                new TwoFacedObject("October", 9),
                new TwoFacedObject("November", 10),
                new TwoFacedObject("December", 11)
            });
        }
        if (hours == null) {
            hours = new ArrayList();
            hours.add(new TwoFacedObject("------", ""));
            for (int i = 0; i < 24; i++) {
                hours.add("" + i);
            }
        }
    }



    /**
     * _more_
     *
     * @param sb _more_
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void showMetadata(StringBuffer sb, Request request, Entry entry)
            throws Exception {


        String tableName = getTableName(entry);
        boolean canEdit = getAccessManager().canDoAction(request, entry,
                              Permission.ACTION_EDIT);

        List<PointDataMetadata> metadata = getMetadata(getTableName(entry));
        if (canEdit && request.defined(ARG_POINT_CHANGETYPE)) {
            String column = request.getString(ARG_POINT_CHANGETYPE, "");
            for (PointDataMetadata pdm : metadata) {
                if (pdm.isString() && pdm.columnName.equals(column)) {
                    if (pdm.varType.equals(pdm.TYPE_STRING)) {
                        pdm.varType = pdm.TYPE_ENUMERATION;
                    } else {
                        pdm.varType = pdm.TYPE_STRING;
                    }
                    getDatabaseManager()
                        .update(
                            Tables.POINTDATAMETADATA.NAME,
                            Clause.and(
                                Clause.eq(
                                    Tables.POINTDATAMETADATA.COL_TABLENAME,
                                    tableName), Clause
                                        .eq(
                                        Tables.POINTDATAMETADATA
                                            .COL_COLUMNNAME, pdm
                                            .columnName)), new String[] {
                                                Tables.POINTDATAMETADATA
                                                    .COL_VARTYPE }, new String[] {
                                                        pdm.varType });
                    break;
                }
            }
        }
        sb.append("<table>");
        sb.append(HtmlUtil.row(HtmlUtil.cols(HtmlUtil.b(msg("Short Name")),
                                             HtmlUtil.b(msg("Long Name")),
                                             HtmlUtil.b(msg("Unit Name")),
                                             HtmlUtil.b(msg("Type")))));
        for (PointDataMetadata pdm : metadata) {
            String type = pdm.varType;
            if (canEdit && pdm.isString()) {
                type = HtmlUtil.href(
                    request.entryUrl(
                        getRepository().URL_ENTRY_SHOW, entry,
                        ARG_POINT_VIEW, VIEW_METADATA,
                        ARG_POINT_CHANGETYPE, pdm.columnName), type,
                            HtmlUtil.title(msg("Change type")));

            }
            sb.append(HtmlUtil.row(HtmlUtil.cols(pdm.shortName, pdm.longName,
                    pdm.unit, type)));
        }
        sb.append("</table>");
    }



    private Result showMetadataXml(Request request, Entry entry)
            throws Exception {
        String tableName = getTableName(entry);
        List<PointDataMetadata> metadata = getMetadata(getTableName(entry));
        Document doc = XmlUtil.makeDocument();
        Element root = doc.createElement("pointmetadata");
        StringBuffer xml = new StringBuffer();
        for (PointDataMetadata pdm : metadata) {
            String type = pdm.varType;
            Element colNode = XmlUtil.create("column", root);
            XmlUtil.create(doc, "id", colNode,""+ pdm.columnNumber,null);
            XmlUtil.create(doc, "isbasic", colNode, ""+pdm.isBasic(),null);
            XmlUtil.create(doc, "shortname", colNode, pdm.shortName,null);
            XmlUtil.create(doc, "longname", colNode, pdm.longName,null);
            XmlUtil.create(doc, "unit", colNode, pdm.unit,null);
            XmlUtil.create(doc, "type", colNode, pdm.varType,null);
        }
        return new Result("",new StringBuffer(XmlUtil.toString(root)),"text/xml");
    }


    /**
     * _more_
     *
     * @param sb _more_
     * @param request _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    private void createSearchForm(StringBuffer sb, Request request,
                                  Entry entry)
            throws Exception {

        initSelectors();
        List<PointDataMetadata> metadata  = getMetadata(getTableName(entry));
        String timezone = getEntryManager().getTimezone(entry);
        String                  tableName = getTableName(entry);



        Date[] dateRange = request.getDateRange(ARG_POINT_FROMDATE,
                                                ARG_POINT_TODATE, null);

        if(dateRange[0] == null) dateRange[0]  = new Date(entry.getStartDate());
        if(dateRange[1] == null) dateRange[1]  = new Date(entry.getEndDate());


        StringBuffer            basicSB   = new StringBuffer();

        basicSB.append(HtmlUtil.formTable());
        basicSB.append(HtmlUtil.hidden(ARG_OUTPUT,
                                       OutputHandler.OUTPUT_HTML));
        basicSB.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));

        basicSB.append(
            HtmlUtil.formEntry(
                msgLabel("From Date"),
                getRepository().makeDateInput(
                    request, ARG_POINT_FROMDATE, "pointsearch", dateRange[0],
                    timezone)));

        basicSB.append(
            HtmlUtil.formEntry(
                msgLabel("To Date"),
                getRepository().makeDateInput(
                    request, ARG_POINT_TODATE, "pointsearch", dateRange[1],
                    timezone)));


        basicSB.append(HtmlUtil.formEntry(msgLabel("Month"),
                                          HtmlUtil.select(ARG_POINT_MONTH,
                                                          months, request.getString(ARG_POINT_MONTH,"")) +" " +
                                          msgLabel("Hour")+
                                          HtmlUtil.select(ARG_POINT_HOUR,
                                                          hours, request.getString(ARG_POINT_HOUR,""))));





        basicSB.append(
            HtmlUtil.formEntryTop(
                msgLabel("Location"),
                HtmlUtil.makeLatLonBox(ARG_POINT_BBOX, 
                                       request.getString(ARG_POINT_BBOX + "_south",""),
                                       request.getString(ARG_POINT_BBOX + "_north",""),
                                       request.getString(ARG_POINT_BBOX + "_east",""),
                                       request.getString(ARG_POINT_BBOX + "_west",""))));

        basicSB.append(HtmlUtil.hidden(ARG_POINT_REDIRECT,"true"));

        basicSB.append(HtmlUtil.formTableClose());



        String max     = HtmlUtil.input(ARG_MAX, request.getString(ARG_MAX,"1000"), HtmlUtil.SIZE_5);
        List   formats = Misc.toList(new Object[] {
            new TwoFacedObject("Html", FORMAT_HTML),
            new TwoFacedObject("Interactive Chart", FORMAT_CHART),
            new TwoFacedObject("Time Series Image", FORMAT_TIMESERIES),
            new TwoFacedObject("Map", FORMAT_MAP),
            new TwoFacedObject("Google Earth KML", FORMAT_KML),
            //            new TwoFacedObject("CSV-Plain", FORMAT_CSV),
            new TwoFacedObject("CSV", FORMAT_CSVHEADER),
            new TwoFacedObject("CSV-IDV", FORMAT_CSVIDV),
            new TwoFacedObject("NetCDF", FORMAT_NETCDF)
        });

        String format = request.getString(ARG_POINT_FORMAT, FORMAT_HTML);


        List sortByList = new ArrayList();
        sortByList.add(new TwoFacedObject("Date",COL_DATE));
        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic()) {
                continue;
            }
            sortByList.add(new TwoFacedObject(pdm.formatName(),pdm.columnName));
        }
        
        int cnt = Misc.getProperty(getProperties(entry),PROP_CNT,0);

        StringBuffer outputSB = new StringBuffer();
        outputSB.append(HtmlUtil.formTable());
        String sortBy = HtmlUtil.select(ARG_POINT_SORTBY, sortByList,request.getString(ARG_POINT_SORTBY,""));
        outputSB.append(HtmlUtil.formEntry(msgLabel("Format"),
                                          HtmlUtil.select(ARG_POINT_FORMAT, formats,format)));
        outputSB.append(HtmlUtil.formEntry(msgLabel("Max"), max+ HtmlUtil.space(1)+"(" + cnt +" " + msg("total") +")"));
        List skip = Misc.toList(new Object[]{new TwoFacedObject("None",1),
                                 new TwoFacedObject("Every other one",2),
                                 new TwoFacedObject("Every third",3),
                                 new TwoFacedObject("Every fourth",4),
                                 new TwoFacedObject("Every fifth",5),
                                 new TwoFacedObject("Every tenth",10),
                                 new TwoFacedObject("Every twentieth",20),
                                 new TwoFacedObject("Every fiftieth",50),
                                             new TwoFacedObject("Every hundredth",100)});

        outputSB.append(HtmlUtil.formEntry(msgLabel("Skip"),HtmlUtil.select(ARG_POINT_STRIDE,skip,request.getString(ARG_POINT_STRIDE,"1"))));
        outputSB.append(HtmlUtil.formEntry(msgLabel("Sort by"), 
                                           sortBy+" " +   HtmlUtil.checkbox(ARG_POINT_ASCENDING,"true", request.get(ARG_POINT_ASCENDING,true))+" " + msg("ascending")));

        StringBuffer advOutputSB = new StringBuffer();
        advOutputSB.append(msgLabel("Interactive Chart"));
        advOutputSB.append(HtmlUtil.checkbox(ARG_POINT_CHART_USETIMEFORNAME,"true",request.get(ARG_POINT_CHART_USETIMEFORNAME, false)));
        advOutputSB.append(HtmlUtil.space(1));
        advOutputSB.append(msg("Use time as name"));
        outputSB.append(HtmlUtil.formEntry(msgLabel("Settings"),
                                           HtmlUtil.makeShowHideBlock(msg("..."),
                                                                      advOutputSB.toString(), false)));
        outputSB.append(HtmlUtil.formTableClose());

        StringBuffer extra = new StringBuffer();
        extra.append(HtmlUtil.formTable());
        List ops = Misc.newList(new TwoFacedObject("&lt;", OP_LT),
                                new TwoFacedObject("&gt;", OP_GT),
                                new TwoFacedObject("=", OP_EQUALS));

        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic()&& !pdm.columnName.equals(COL_ALTITUDE)) {
                continue;
            }
            String suffix = HtmlUtil.space(1) + pdm.formatUnit();
            String argSuffix = ""+pdm.columnNumber;
            String label  = pdm.formatName() + ":";
            if (pdm.isEnumeration()) {
                List values = pdm.enumeratedValues;
                if (values == null) {
                    Statement stmt = getDatabaseManager().select(
                                         SqlUtil.distinct(pdm.columnName),
                                         tableName, (Clause) null);
                    values = Misc.toList(SqlUtil.readString(stmt, 1));
                    values =  new ArrayList(Misc.sort(values));
                    values.add(0, "");
                    getDatabaseManager().close(stmt);
                    pdm.enumeratedValues = values;
                }
                String field = HtmlUtil.select(ARG_POINT_FIELD_VALUE
                                   + argSuffix, values,request.getString(ARG_POINT_FIELD_VALUE+argSuffix,""));
                extra.append(HtmlUtil.formEntry(label, field));
            } else if (pdm.isString()) {
                String field = HtmlUtil.input(ARG_POINT_FIELD_VALUE
                                   + argSuffix, request.getString(ARG_POINT_FIELD_VALUE
                                   + argSuffix,""), HtmlUtil.SIZE_20);
                String cbx = HtmlUtil.checkbox(ARG_POINT_FIELD_EXACT
                                 + argSuffix, "true", request.get(ARG_POINT_FIELD_EXACT
                                 + argSuffix,false)) + " "
                                     + msg("Exact");
                extra.append(HtmlUtil.formEntry(label, field + " " + cbx));
            } else {
                String op = HtmlUtil.select(ARG_POINT_FIELD_OP
                                            + argSuffix, ops,request.getString(ARG_POINT_FIELD_OP
                                            + argSuffix,""));
                String field = HtmlUtil.input(ARG_POINT_FIELD_VALUE
                                   + argSuffix, request.getString(ARG_POINT_FIELD_VALUE
                                   + argSuffix,""), HtmlUtil.SIZE_10);
                extra.append(HtmlUtil.formEntry(label,
                        op + " " + field + suffix));
            }
        }
        extra.append(HtmlUtil.formTableClose());


        StringBuffer params = new StringBuffer();
        params.append("<ul>");
        params.append(HtmlUtil.checkbox(ARG_POINT_PARAM_ALL, "true", request.get(ARG_POINT_PARAM_ALL,false)));
        params.append(HtmlUtil.space(1));
        params.append(msg("All"));
        params.append(HtmlUtil.br());
        List list = request.get(ARG_POINT_PARAM, new ArrayList());
        int cbxCnt = 0;

        for (PointDataMetadata pdm : metadata) {
            if(pdm.isObMonth() || pdm.isObHour()) {
                continue;
            }
            if (pdm.isBasic()) {
                //                continue;
            }
            String value = ""+pdm.columnNumber;

            boolean checked = (list.size()==0?pdm.isBasic():list.contains(value));
            String cbxId = "cbx" + (cbxCnt++);
            String cbxExtra = HtmlUtil.id(cbxId) +
                HtmlUtil.attr(HtmlUtil.ATTR_ONCLICK,
                              HtmlUtil.call(
                                            "checkboxClicked",
                                            HtmlUtil.comma(
                                                           "event",
                                                           HtmlUtil.squote(ARG_POINT_PARAM),
                                                           HtmlUtil.squote(cbxId))));
            params.append(HtmlUtil.checkbox(ARG_POINT_PARAM, value,
                                          checked,cbxExtra));



            params.append(HtmlUtil.space(1));
            params.append(pdm.formatName());
            params.append(HtmlUtil.br());
        }
        params.append("</ul>");

        //        sb.append(header(msg("Point Data Search")));
        sb.append(
            HtmlUtil.formPost(getRepository().URL_ENTRY_SHOW.toString(), HtmlUtil.attr(
                    "name", "pointsearch") + HtmlUtil.id("pointsearch")));

        sb.append(HtmlUtil.submit(msg("Search"), ARG_POINT_SEARCH));
        sb.append(HtmlUtil.p());

        sb.append(HtmlUtil.makeShowHideBlock(msg("Basic"),
                                             basicSB.toString(), true));

        sb.append(HtmlUtil.makeShowHideBlock(msg("Output"),
                                             outputSB.toString(), false));

        sb.append(HtmlUtil.makeShowHideBlock(msg("Advanced Search"),
                                             extra.toString(), false));

        sb.append(HtmlUtil.makeShowHideBlock(msg("Parameters"),
                                             params.toString(), false));

        sb.append(HtmlUtil.p());
        sb.append(HtmlUtil.submit(msg("Search"), ARG_POINT_SEARCH));
        sb.append(HtmlUtil.formClose());





    }





    /**
     * _more_
     *
     * @param tableName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private List<PointDataMetadata> getMetadata(String tableName)
            throws Exception {
        List<PointDataMetadata> metadata = metadataCache.get(tableName);
        if (metadata == null) {
            if (metadataCache.size() > 100) {
                metadataCache = new Hashtable<String,
                        List<PointDataMetadata>>();
            }
            metadata = new ArrayList<PointDataMetadata>();
            Statement statement =
                getDatabaseManager()
                    .select(Tables.POINTDATAMETADATA.COLUMNS, Tables
                        .POINTDATAMETADATA.NAME, Clause
                        .eq(Tables.POINTDATAMETADATA
                            .COL_TABLENAME, tableName), " ORDER BY "
                                + Tables.POINTDATAMETADATA.COL_COLUMNNUMBER
                            + " ASC ");
            

            


            SqlUtil.Iterator iter = SqlUtil.getIterator(statement);
            ResultSet        results;
            while ((results = iter.next()) != null) {
                while (results.next()) {
                    int col = 1;
                    metadata.add(
                        new PointDataMetadata(
                            results.getString(col++),
                            results.getString(col++), results.getInt(col++),
                            results.getString(col++),
                            results.getString(col++),
                            results.getString(col++),
                            results.getString(col++)));
                }
            }
            metadataCache.put(tableName, metadata);
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
        String tableName = getTableName(id);
        deleteFromDatabase(tableName);
    }

    /**
     * _more_
     *
     * @param tableName _more_
     */
    private void deleteFromDatabase(String tableName) {
        StringBuffer sql = new StringBuffer();
        try {
            sql.append("drop table " + tableName);
            getDatabaseManager().execute(sql.toString(), 1000, 10000);
        } catch (Exception ignore) {}
        try {
            getDatabaseManager().delete(
                Tables.POINTDATAMETADATA.NAME,
                Clause.eq(Tables.POINTDATAMETADATA.COL_TABLENAME, tableName));
        } catch (Exception ignore) {}
    }




    public void addColumnsToEntryForm(Request request, StringBuffer formBuffer,
                                      Entry entry) {
        //noop
    }



    /**
     * _more_
     *
     * @param entry _more_
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FeatureDatasetPoint getDataset(Entry entry, Group parent, File file)
            throws Exception {
        Formatter buf = new Formatter();

        getStorageManager().checkFile(file);
        if(file.toString().toLowerCase().endsWith(".csv")) {
            TextPointDataSource dataSource = new TextPointDataSource("dummy.csv");
            String contents = getStorageManager().readSystemResource(file);
            FieldImpl field = dataSource.makeObs(contents, ",", null, null,
                                                 null, false, false);
            file = getStorageManager().getTmpFile(null, "test.nc");
            PointObFactory.writeToNetcdf(file, field);
        }


        List<Metadata> metadataList =
            getMetadataManager().findMetadata(entry,
                ContentMetadataHandler.TYPE_ATTACHMENT, true);
        if (metadataList == null) {
            metadataList =
                getMetadataManager().findMetadata(parent,
                                                  ContentMetadataHandler.TYPE_ATTACHMENT, true,false);
        }

        if (metadataList != null) {
            for (Metadata metadata : metadataList) {
                if (metadata.getAttr1().endsWith(".ncml")) {
                    File templateNcmlFile =
                        new File(
                                 IOUtil.joinDir(
                                                getRepository().getStorageManager().getEntryDir(
                                                                                                metadata.getEntryId(),
                                                                                                false), metadata.getAttr1()));
                    String ncml = getStorageManager().readSystemResource(templateNcmlFile);
                    String filePath  = file.toString();
                    filePath = filePath.replace("\\","/");
                    if(filePath.indexOf(":")>=0) {
                        filePath  = IOUtil.getURL(filePath,getClass()).toString();
                    }
                    ncml = ncml.replace("${location}", filePath);
                    //                    System.err.println ("exists:" + file.exists());
                    //                    System.err.println ("exists:" + (new File(filePath)).exists());
                    //                    System.err.println ("ncml:" + ncml);
                    File ncmlFile =
                        getStorageManager().getScratchFile(entry.getId() + "_"
                                                           + metadata.getId() + ".ncml");
                    IOUtil.writeBytes(ncmlFile, ncml.getBytes());
                    file = new File(ncmlFile.toString());
                    break;
                }
            }
       } 


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

        /** _more_ */
        public static final String TYPE_STRING = "string";

        /** _more_ */
        public static final String TYPE_DOUBLE = "double";

        /** _more_          */
        public static final String TYPE_INT = "int";

        /** _more_ */
        public static final String TYPE_DATE = "date";

        /** _more_          */
        public static final String TYPE_ENUMERATION = "enumeration";


        /** _more_ */
        private String tableName;

        /** _more_ */
        private String columnName;

        /** _more_ */
        private String shortName;

        /** _more_ */
        private String longName;

        /** _more_ */
        private int columnNumber;

        /** _more_ */
        private String varType;

        /** _more_ */
        private String unit;

        /** _more_          */
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
                                 int column, String unit, String type) {
            this(tableName, columnName, column, columnName, columnName, unit,
                 type);
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
                                 int column, String shortName,
                                 String longName, String unit, String type) {
            this.tableName    = tableName;
            this.columnName   = columnName;
            this.shortName    = shortName;
            this.longName     = longName;
            this.columnNumber = column;
            this.unit         = unit;
            this.varType      = type;

        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String getDatabaseType() {
            if (varType.equals(TYPE_DOUBLE)) {
                return "ramadda.double";
            } else if (varType.equals(TYPE_INT)) {
                return "int";
            } else if (varType.equals(TYPE_DATE)) {
                return "ramadda.datetime";
            }
            return "varchar(1000)";
        }


        public boolean isObId() {
            return columnName.equals(COL_ID);
        }

        public boolean isObMonth() {
            return columnName.equals(COL_MONTH);
        }

        public boolean isObHour() {
            return columnName.equals(COL_HOUR);
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

        /**
         * _more_
         *
         * @return _more_
         */
        public String formatName() {
            return shortName.replace("_", " ");
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public String formatUnit() {
            if ((unit == null) || (unit.length() == 0)) {
                return "";
            }
            return "[" + unit + "]";

        }

        public boolean hasUnit() {
            if ((unit == null) || (unit.length() == 0)) {
                return false;
            }
            return true;
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
            return varType.equals(TYPE_STRING)
                   || varType.equals(TYPE_ENUMERATION);
        }

        public boolean isDate() {
            return varType.equals(TYPE_DATE);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isEnumeration() {
            return varType.equals(TYPE_ENUMERATION);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isNumeric() {
            return varType.equals(TYPE_DOUBLE) || varType.equals(TYPE_INT);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isBasic() {
            return columnNumber < NUM_BASIC_COLUMNS;
        }

    }


    /**
     * Class PointData _more_
     *
     *
     * @author IDV Development Team
     */
    public static class PointData {

        int id;

        /** _more_          */
        double lat;

        /** _more_          */
        double lon;

        /** _more_          */
        double alt;

        /** _more_          */
        Date date;

        /** _more_          */
        int month;

        /** _more_          */
        int hour;

        /** _more_          */
        List values;

        /**
         * _more_
         *
         * @param date _more_
         * @param lat _more_
         * @param lon _more_
         * @param alt _more_
         * @param month _more_
         * @param hour _more_
         */
        public PointData(int id,
                         Date date, double lat, double lon, double alt,
                         int month, int hour) {
            this.id = id;
            this.lat   = lat;
            this.lon   = lon;
            this.alt   = alt;
            this.date  = date;
            this.month = month;
            this.hour  = hour;
        }

        public Object getValue(String col) {
            if(col.equals(COL_ID)) return new Double(id);
            if(col.equals(COL_LATITUDE)) return new Double(lat);
            if(col.equals(COL_LONGITUDE)) return new Double(lon);
            if(col.equals(COL_ALTITUDE)) return new Double(alt);
            if(col.equals(COL_DATE)) return date;
            if(col.equals(COL_MONTH)) return new Double(month);
            if(col.equals(COL_HOUR)) return new Double(hour);
            return "n/a";
        }

        /**
         * _more_
         *
         * @param values _more_
         */
        public void setValues(List values) {
            this.values = values;
        }
    }


}

