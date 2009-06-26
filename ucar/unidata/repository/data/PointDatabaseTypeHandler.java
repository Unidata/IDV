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

    /** _more_ */
    public static String TYPE_POINTDATABASE = "pointdatabase";

    /** _more_          */
    public static final String FORMAT_HTML = "html";

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
    public static final double MISSING = -987654.98765;

    /** _more_          */
    public static final String ARG_POINT_CHANGETYPE = "point_changetype";

    /** _more_          */
    public static final String ARG_POINT_UPLOAD_FORM = "point_upload_form";

    /** _more_          */
    public static final String ARG_POINT_UPLOAD_FILE = "point_upload_file";


    /** _more_          */
    public static final String ARG_POINT_IMAGE_WIDTH = "point_image_width";

    /** _more_          */
    public static final String ARG_POINT_IMAGE_HEIGHT = "point_image_height";

    /** _more_          */
    public static final String ARG_POINT_SHOW_METADATA = "point_showmetadata";

    /** _more_          */
    public static final String ARG_POINT_SHOW_DEFAULT = "point_showdefault";

    /** _more_          */
    public static final String ARG_POINT_OUTPUT = "point_output";

    /** _more_          */
    public static final String ARG_POINT_SEARCH = "point_search";

    /** _more_          */
    public static final String ARG_POINT_FROMDATE = "point_fromdate";

    /** _more_          */
    public static final String ARG_POINT_TODATE = "point_todate";

    /** _more_          */
    public static final String ARG_POINT_BBOX = "point_bbox";

    /** _more_          */
    public static final String ARG_POINT_HOUR = "point_hour";

    /** _more_          */
    public static final String ARG_POINT_MONTH = "point_month";

    /** _more_          */
    public static final String ARG_POINT_WHAT = "point_what";

    /** _more_          */
    public static final String ARG_POINT_WHAT_ALL = "point_what_all";

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

    /** _more_          */
    public static final String COL_DATE = "obtime";

    /** _more_          */
    public static final String COL_LATITUDE = "latitude";

    /** _more_          */
    public static final String COL_LONGITUDE = "longitude";

    /** _more_          */
    public static final String COL_ALTITUDE = "altitude";

    /** _more_          */
    public static final String COL_MONTH = "month";

    /** _more_          */
    public static final String COL_HOUR = "hour";

    /** _more_          */
    public static final int NUM_BASIC_COLUMNS = 6;

    /** _more_          */
    private List<TwoFacedObject> months;

    /** _more_          */
    private List hours;

    /** _more_          */
    private Hashtable<String, List<PointDataMetadata>> metadataCache =
        new Hashtable<String, List<PointDataMetadata>>();

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
    private void createDatabase(Request request, Entry entry,
                                Connection connection)
            throws Exception {

        String                  tableName = getTableName(entry);
        List<PointDataMetadata> metadata = new ArrayList<PointDataMetadata>();
        List<PointDataMetadata> stringMetadata =
            new ArrayList<PointDataMetadata>();
        List<PointDataMetadata> numericMetadata =
            new ArrayList<PointDataMetadata>();


        metadata.add(new PointDataMetadata(tableName, COL_DATE,
                                           metadata.size(), "",
                                           PointDataMetadata.TYPE_DATE));
        metadata.add(new PointDataMetadata(tableName, COL_LATITUDE,
                                           metadata.size(), "degrees",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_LONGITUDE,
                                           metadata.size(), "degrees",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_ALTITUDE,
                                           metadata.size(), "m",
                                           PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, COL_MONTH,
                                           metadata.size(), "",
                                           PointDataMetadata.TYPE_INT));
        metadata.add(new PointDataMetadata(tableName, COL_HOUR,
                                           metadata.size(), "",
                                           PointDataMetadata.TYPE_INT));


        FeatureDatasetPoint fdp  = getDataset(entry, entry.getFile());


        List                vars = fdp.getDataVariables();
        for (VariableSimpleIF var : (List<VariableSimpleIF>) vars) {
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
                    value =
                        new Integer(calendar.get(GregorianCalendar.MONTH));
                } else {
                    StructureMembers.Member member =
                        structure.findMember((String) pdm.shortName);
                    if (pdm.isString()) {
                        value = structure.getScalarString(member);
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
            getDatabaseManager().setValues(insertStmt, values);
            insertStmt.addBatch();
            batchCnt++;
            if (batchCnt > 1000) {
                insertStmt.executeBatch();
                batchCnt = 0;
            }
            if (((cnt++) % 1000) == 0) {
                System.err.println("added " + cnt + " observations");
            }
        }
        if (batchCnt > 0) {
            insertStmt.executeBatch();
        }

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
    private Result doUpload(Request request, Entry entry) throws Exception {
        StringBuffer sb = new StringBuffer();

        sb.append(getHeader(request, entry));
        if (request.exists(ARG_POINT_UPLOAD_FILE)) {
            File file =
                new File(request.getUploadedFile(ARG_POINT_UPLOAD_FILE));
            FeatureDatasetPoint fdp = getDataset(entry, file);
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
            sb.append(HtmlUtil.hidden(ARG_POINT_UPLOAD_FORM, "true"));

            sb.append(HtmlUtil.fileInput(ARG_POINT_UPLOAD_FILE,
                                         HtmlUtil.SIZE_50));
            sb.append(HtmlUtil.br());
            sb.append(HtmlUtil.submit("Add new data"));
            sb.append(request.uploadForm(getRepository().URL_ENTRY_SHOW));
            sb.append(HtmlUtil.formClose());
        }

        return new Result("Upload", sb);
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

        String result = request.getString(ARG_POINT_OUTPUT, FORMAT_HTML);
        String baseName =
            IOUtil.stripExtension(getStorageManager().getFileTail(entry));
        if (result.equals(FORMAT_TIMESERIES)) {
            request.put(ARG_POINT_OUTPUT, FORMAT_TIMESERIES_IMAGE);
            StringBuffer sb = new StringBuffer();
            sb.append(getHeader(request, entry));
            String redirect = request.getRequestPath() + "/" + baseName
                              + ".png" + "?"
                              + request.getUrlArgs(null,
                                  Misc.newHashtable(OP_LT, OP_LT));
            sb.append(HtmlUtil.img(redirect));
            return new Result("Search Results", sb);
        }

        if (result.equals(FORMAT_SCATTERPLOT)) {}


        String tableName = getTableName(entry);
        Date[] dateRange = request.getDateRange(ARG_POINT_FROMDATE,
                               ARG_POINT_TODATE, new Date());
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
        if (request.get(ARG_POINT_WHAT_ALL, false)) {
            tmp = metadata;
        } else {
            List<String> whatList =
                (List<String>) request.get(ARG_POINT_WHAT, new ArrayList());
            HashSet seen = new HashSet();
            for (String col : whatList) {
                seen.add(col);
            }
            for (PointDataMetadata pdm : metadata) {
                if (pdm.isBasic() || seen.contains(pdm.columnName)) {
                    tmp.add(pdm);
                }
            }
        }


        //Strip out the month/hour
        List<PointDataMetadata> columnsToUse =
            new ArrayList<PointDataMetadata>();
        for (PointDataMetadata pdm : tmp) {
            if (pdm.columnName.equals(COL_MONTH)
                    || pdm.columnName.equals(COL_HOUR)) {
                continue;
            }
            columnsToUse.add(pdm);
        }





        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic()) {
                continue;
            }
            if ( !request.defined(ARG_POINT_FIELD_VALUE + pdm.columnName)) {
                continue;
            }
            if (pdm.isString()) {
                String value = request.getString(ARG_POINT_FIELD_VALUE
                                   + pdm.columnName, "");
                if (request.get(ARG_POINT_FIELD_EXACT + pdm.columnName,
                                false)) {
                    clauses.add(Clause.eq(pdm.columnName, value));
                } else {
                    clauses.add(Clause.like(pdm.columnName,
                                            "%" + value + "%"));
                }
            } else {
                String op = request.getString(ARG_POINT_FIELD_OP
                                + pdm.columnName, OP_LT);
                double value = request.get(ARG_POINT_FIELD_VALUE
                                           + pdm.columnName, 0.0);
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

        for (PointDataMetadata pdm : columnsToUse) {
            if (cols == null) {
                cols = new StringBuffer();
            } else {
                cols.append(",");
            }
            cols.append(pdm.columnName);
        }


        Statement stmt = getDatabaseManager().select(cols.toString(),
                             Misc.newList(tableName),
                             Clause.and(Clause.toArray(clauses)),
                             " ORDER BY " + COL_DATE + " ASC ",
                             request.get(ARG_MAX, 1000));

        SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
        ResultSet        results;
        int              cnt           = 0;
        List<PointData>  pointDataList = new ArrayList<PointData>();
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int col = 1;
                PointData pointData =
                    new PointData(getDatabaseManager().getDate(results,
                        col++), checkReadValue(results.getDouble(col++)),
                                checkReadValue(results.getDouble(col++)),
                                checkReadValue(results.getDouble(col++)), 0,
                                0);
                List values = new ArrayList();
                while (col <= columnsToUse.size()) {
                    PointDataMetadata pdm = columnsToUse.get(col - 1);
                    if (pdm.isString()) {
                        values.add(results.getString(col));
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


        if (result.equals(FORMAT_HTML) || result.equals(FORMAT_TIMELINE)) {
            return makeSearchResultsHtml(request, entry, columnsToUse,
                                         pointDataList,
                                         result.equals(FORMAT_TIMELINE));
        } else if (result.equals(FORMAT_CSV) || result.equals(FORMAT_CSVIDV)
                   || result.equals(FORMAT_CSVHEADER)
                   || result.equals(FORMAT_XLS)) {
            return makeSearchResultsCsv(request, entry, columnsToUse,
                                        pointDataList, result);
        } else if (result.equals(FORMAT_TIMESERIES_IMAGE)) {
            return makeSearchResultsTimeSeries(request, entry, columnsToUse,
                    pointDataList);

        } else if (result.equals(FORMAT_SCATTERPLOT_IMAGE)) {
            return makeSearchResultsScatterPlot(request, entry, columnsToUse,
                    pointDataList);
        } else if (result.equals(FORMAT_MAP)) {
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
    private static JFreeChart createChart(XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart("Point Data",  // title
            "Date",   // x-axis label
            "",       // y-axis label
            dataset,  // data
            true,     // create legend?
            true,     // generate tooltips?
            false     // generate URLs?
                );

        chart.setBackgroundPaint(Color.white);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.white);
        plot.setRangeGridlinePaint(Color.white);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        XYItemRenderer r    = plot.getRenderer();
        DateAxis       axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MMM-yyyy"));

        return chart;

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
        sb.append(getHeader(request, entry));
        sb.append(header(msg("Point Data Search Results")));
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
        StringBuffer header =
            new StringBuffer(HtmlUtil.cols(HtmlUtil.b(msg("Date")),
                                           HtmlUtil.b(msg("Latitude")),
                                           HtmlUtil.b(msg("Longitude")),
                                           HtmlUtil.b(msg("Altitude"))));
        for (PointDataMetadata pdm : columnsToUse) {
            if (pdm.isBasic()) {
                continue;
            }
            header.append(HtmlUtil.cols(HtmlUtil.b(pdm.formatName() + " "
                    + pdm.formatUnit())));
        }
        sb.append(HtmlUtil.row(header.toString(),
                               HtmlUtil.attr(HtmlUtil.ATTR_ALIGN, "center")));



        for (PointData pointData : list) {
            StringBuffer row = new StringBuffer();
            row.append(HtmlUtil.cols(pointData.date.toString(),
                                     "" + pointData.lat, "" + pointData.lon,
                                     "" + pointData.alt));
            List values = pointData.values;
            int  cnt    = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                if (pdm.isBasic()) {
                    continue;
                }
                cnt++;
                Object value = values.get(cnt);
                row.append(HtmlUtil.cols("" + value));
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
        JFreeChart           chart  = createChart(dummy);
        XYPlot               xyPlot = (XYPlot) chart.getPlot();

        Hashtable<String, TimeSeries> seriesMap = new Hashtable<String,
                                                      TimeSeries>();
        int paramCount = 0;
        int colorCount = 0;
        for (PointData pointData : list) {
            List values = pointData.values;
            int  cnt    = -1;
            for (PointDataMetadata pdm : columnsToUse) {
                if (pdm.isBasic()) {
                    continue;
                }
                cnt++;
                Object value = values.get(cnt);
                if (pdm.isNumeric()) {
                    TimeSeries series = seriesMap.get(pdm.columnName);
                    if (series == null) {
                        paramCount++;
                        TimeSeriesCollection dataset =
                            new TimeSeriesCollection();
                        series = new TimeSeries(pdm.shortName,
                                Millisecond.class);
                        ValueAxis rangeAxis = new NumberAxis(pdm.shortName);
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
                        dataset.setDomainIsPointsInTime(true);
                        dataset.addSeries(series);
                        seriesMap.put(pdm.columnName, series);
                        xyPlot.setDataset(paramCount, dataset);
                        xyPlot.mapDatasetToRangeAxis(paramCount, paramCount);
                    }
                    series.addOrUpdate(new Millisecond(pointData.date),
                                       ((Double) value).doubleValue());
                }
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
                if (cnt != 0) {
                    sb.append(",");
                }
                sb.append(pdm.shortName);
                sb.append(pdm.formatUnit());
                cnt++;
            }
        }

        if (addMetadata) {
            StringBuffer h1 = new StringBuffer();
            StringBuffer h2 = new StringBuffer();
            h1.append("(index) -> (");
            int cnt = 0;
            for (PointDataMetadata pdm : columnsToUse) {
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
                if (pdm.isBasic()) {
                    continue;
                }
                dcnt++;
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
                if (pdm.isBasic()) {
                    continue;
                }
                dcnt++;
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
        return new Result("", getStorageManager().getFileInputStream(file),
                          "application/x-netcdf");
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
        boolean showMetadata = request.get(ARG_POINT_SHOW_METADATA, false);
        boolean showUpload   = request.get(ARG_POINT_UPLOAD_FORM, false);
        boolean doSearch     = request.exists(ARG_POINT_SEARCH);
        boolean showSearch   = !showMetadata && !showUpload && !doSearch;

        if (showSearch) {
            headerLinks.add(HtmlUtil.b(msg("Search form")));
        } else {
            headerLinks.add(
                HtmlUtil.href(
                    request.entryUrl(getRepository().URL_ENTRY_SHOW, entry),
                    msg("Search form")));

        }

        if (showMetadata) {
            headerLinks.add(HtmlUtil.b(msg("Metadata")));
        } else {
            headerLinks.add(
                HtmlUtil.href(
                    request.entryUrl(
                        getRepository().URL_ENTRY_SHOW, entry,
                        ARG_POINT_SHOW_METADATA, "true"), msg("Metadata")));
        }

        if (canEdit) {
            if ( !showUpload) {
                headerLinks.add(
                    HtmlUtil.href(
                        request.entryUrl(
                            getRepository().URL_ENTRY_SHOW, entry,
                            ARG_POINT_UPLOAD_FORM, "true"), msg(
                                "Upload more data")));
            } else {
                headerLinks.add(HtmlUtil.b(msg("Upload more data")));
            }
        }

        headerLinks.add(
            HtmlUtil.href(
                request.entryUrl(
                    getRepository().URL_ENTRY_SHOW, entry,
                    ARG_POINT_SHOW_DEFAULT, "true"), msg("Show default")));
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

        if (request.get(ARG_POINT_SHOW_DEFAULT, false)) {
            return null;
        }



        if (request.exists(ARG_POINT_SEARCH)) {
            return doSearch(request, entry);
        }

        if (request.get(ARG_POINT_UPLOAD_FORM, false)) {
            return doUpload(request, entry);
        }

        StringBuffer sb      = new StringBuffer();


        boolean showMetadata = request.get(ARG_POINT_SHOW_METADATA, false);
        sb.append(getHeader(request, entry));
        if (showMetadata) {
            showMetadata(sb, request, entry);
        } else {
            createSearchForm(sb, request, entry);
        }
        return new Result("Point Data Search", sb);
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
                        ARG_POINT_SHOW_METADATA, "true",
                        ARG_POINT_CHANGETYPE, pdm.columnName), type,
                            HtmlUtil.title(msg("Change type")));

            }
            sb.append(HtmlUtil.row(HtmlUtil.cols(pdm.shortName, pdm.longName,
                    pdm.unit, type)));
        }
        sb.append("</table>");
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




        Date                    fromDate  = new Date(entry.getStartDate());
        Date                    toDate    = new Date(entry.getEndDate());
        StringBuffer            basicSB   = new StringBuffer();

        basicSB.append(HtmlUtil.formTable());
        basicSB.append(HtmlUtil.hidden(ARG_OUTPUT,
                                       OutputHandler.OUTPUT_HTML));
        basicSB.append(HtmlUtil.hidden(ARG_ENTRYID, entry.getId()));

        basicSB.append(
            HtmlUtil.formEntry(
                msgLabel("From Date"),
                getRepository().makeDateInput(
                    request, ARG_POINT_FROMDATE, "pointsearch", fromDate,
                    timezone)));

        basicSB.append(
            HtmlUtil.formEntry(
                msgLabel("To Date"),
                getRepository().makeDateInput(
                    request, ARG_POINT_TODATE, "pointsearch", toDate,
                    timezone)));


        basicSB.append(HtmlUtil.formEntry(msgLabel("Month"),
                                          HtmlUtil.select(ARG_POINT_MONTH,
                                              months, "")));

        basicSB.append(HtmlUtil.formEntry(msgLabel("Hour"),
                                          HtmlUtil.select(ARG_POINT_HOUR,
                                              hours, "")));





        basicSB.append(
            HtmlUtil.formEntryTop(
                msgLabel("Location"),
                HtmlUtil.makeLatLonBox(ARG_POINT_BBOX, "", "", "", "")));

        String max     = HtmlUtil.input(ARG_MAX, "1000", HtmlUtil.SIZE_5);
        List   formats = Misc.toList(new Object[] {
            new TwoFacedObject("Html", FORMAT_HTML),
            new TwoFacedObject("Time Series", FORMAT_TIMESERIES),
            new TwoFacedObject("Map", FORMAT_MAP),
            new TwoFacedObject("CSV-Plain", FORMAT_CSV),
            new TwoFacedObject("CSV-Header", FORMAT_CSVHEADER),
            new TwoFacedObject("CSV-IDV", FORMAT_CSVIDV),
            new TwoFacedObject("NetCDF", FORMAT_NETCDF)
        });

        basicSB.append(HtmlUtil.formEntry(msgLabel("Format"),
                                          HtmlUtil.select(ARG_POINT_OUTPUT,
                                              formats) + HtmlUtil.space(2)
                                                  + msgLabel("Max") + max));




        basicSB.append(HtmlUtil.formTableClose());


        StringBuffer extra = new StringBuffer();
        extra.append(HtmlUtil.formTable());
        List ops = Misc.newList(new TwoFacedObject("&lt;", OP_LT),
                                new TwoFacedObject("&gt;", OP_GT),
                                new TwoFacedObject("=", OP_EQUALS));

        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic()) {
                continue;
            }
            String suffix = HtmlUtil.space(1) + pdm.formatUnit();
            String label  = pdm.formatName() + ":";
            if (pdm.isEnumeration()) {
                List values = pdm.enumeratedValues;
                if (values == null) {
                    Statement stmt = getDatabaseManager().select(
                                         SqlUtil.distinct(pdm.columnName),
                                         tableName, (Clause) null);
                    values = Misc.toList(SqlUtil.readString(stmt, 1));
                    values.add(0, "");
                    getDatabaseManager().close(stmt);
                    pdm.enumeratedValues = values;
                }
                String field = HtmlUtil.select(ARG_POINT_FIELD_VALUE
                                   + pdm.columnName, values);
                extra.append(HtmlUtil.formEntry(label, field));
            } else if (pdm.isString()) {
                String field = HtmlUtil.input(ARG_POINT_FIELD_VALUE
                                   + pdm.columnName, "", HtmlUtil.SIZE_20);
                String cbx = HtmlUtil.checkbox(ARG_POINT_FIELD_EXACT
                                 + pdm.columnName, "true", false) + " "
                                     + msg("Exact");
                extra.append(HtmlUtil.formEntry(label, field + " " + cbx));
            } else {
                String op = HtmlUtil.select(ARG_POINT_FIELD_OP
                                            + pdm.columnName, ops);
                String field = HtmlUtil.input(ARG_POINT_FIELD_VALUE
                                   + pdm.columnName, "", HtmlUtil.SIZE_10);
                extra.append(HtmlUtil.formEntry(label,
                        op + " " + field + suffix));
            }
        }
        extra.append(HtmlUtil.formTableClose());


        StringBuffer what = new StringBuffer();
        what.append("<ul>");
        what.append(HtmlUtil.checkbox(ARG_POINT_WHAT_ALL, "true", false));
        what.append(HtmlUtil.space(1));
        what.append(msg("All"));
        what.append(HtmlUtil.br());
        for (PointDataMetadata pdm : metadata) {
            if (pdm.isBasic()) {
                continue;
            }
            what.append(HtmlUtil.checkbox(ARG_POINT_WHAT, pdm.columnName,
                                          pdm.isBasic()));
            what.append(HtmlUtil.space(1));
            what.append(pdm.formatName());
            what.append(HtmlUtil.br());
        }
        what.append("</ul>");


        //        sb.append(header(msg("Point Data Search")));
        sb.append(
            HtmlUtil.formPost(
                getRepository().URL_ENTRY_SHOW + "/"
                + IOUtil.stripExtension(
                    getStorageManager().getFileTail(entry)), HtmlUtil.attr(
                    "name", "pointsearch") + HtmlUtil.id("pointsearch")));

        sb.append(HtmlUtil.submit(msg("Search"), ARG_POINT_SEARCH));
        sb.append(HtmlUtil.p());

        sb.append(HtmlUtil.makeShowHideBlock(msg("Basic"),
                                             basicSB.toString(), true));

        sb.append(HtmlUtil.makeShowHideBlock(msg("Advanced Search"),
                                             extra.toString(), false));

        sb.append(HtmlUtil.makeShowHideBlock(msg("Parameters"),
                                             what.toString(), false));

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
     * @param file _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FeatureDatasetPoint getDataset(Entry entry, File file)
            throws Exception {
        Formatter buf = new Formatter();

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
        public PointData(Date date, double lat, double lon, double alt,
                         int month, int hour) {
            this.lat   = lat;
            this.lon   = lon;
            this.alt   = alt;
            this.date  = date;
            this.month = month;
            this.hour  = hour;
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

