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
import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


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
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;




/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class PointDatabaseTypeHandler extends GenericTypeHandler {

    /** _more_ */
    public static String TYPE_POINTDATABASE = "pointdatabase";


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
        return "pointdatabase_" + id;
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

        String       tableName = getTableName(entry);
        StringBuffer sql       = new StringBuffer();
        sql.append("CREATE TABLE ");
        sql.append(tableName);
        sql.append(
            " (latitude ramadda.double, \nlongitude ramadda.double,\naltitude ramadda.double,\ndate ramadda.datetime\n");
        FeatureDatasetPoint     fdp      = getDataset(entry);
        List<PointDataMetadata> metadata = new ArrayList<PointDataMetadata>();

        metadata.add(new PointDataMetadata(tableName, "latitude",
                                           metadata.size(),
                                           "degrees",PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, "longitude",
                                           metadata.size(),
                                           "degrees",  PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, "altitude",
                                           metadata.size(),
                                           "m", PointDataMetadata.TYPE_DOUBLE));
        metadata.add(new PointDataMetadata(tableName, "date",
                                           metadata.size(),
                                           "",
                                           PointDataMetadata.TYPE_STRING
                                           ));


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
                    || colName.equals("altitude") || colName.equals("date")) {
                continue;
            }
            if (colName.length() == 0) {
                continue;
            }
            //            System.err.println("name:" + colName+":" + var.getName());
            sql.append(",");
            sql.append(colName);
            sql.append(" ");
            boolean isString = type.equals(DataType.STRING)
                               || type.equals(DataType.CHAR);
            if (isString) {
                sql.append("varchar(1000)\n");
            } else {
                sql.append("ramadda.double\n");
            }
            metadata.add(new PointDataMetadata(tableName, colName,
                                            metadata.size(),varName,
                                               var.getName(), unit,
                                               (isString
                                                ? PointDataMetadata.TYPE_STRING
                                                : PointDataMetadata.TYPE_DOUBLE)));

        }

        sql.append(")");
        //        System.err.println(sql);
        getDatabaseManager().execute(
            getDatabaseManager().convertSql(sql.toString()), 1000, 10000);

        sql = new StringBuffer();
        sql.append("CREATE INDEX " + tableName + "_INDEX_DATE ON "
                   + tableName + " (DATE)");
        getDatabaseManager().execute(sql.toString(), 1000, 10000);


        Connection connection = getDatabaseManager().getNewConnection();

        try {
            String[] ARRAY = new String[metadata.size()];
            for (PointDataMetadata pdm : metadata) {
                getDatabaseManager().executeInsert(
                    Tables.POINTDATAMETADATA.INSERT, pdm.getValues());
                ARRAY[pdm.columnNumber] = pdm.columnName;
            }
            String insertString = SqlUtil.makeInsert(tableName,
                                      SqlUtil.commaNoDot(ARRAY),
                                      SqlUtil.getQuestionMarks(ARRAY.length));


            PreparedStatement insertStmt   = connection.prepareStatement(insertString);
            PointFeatureIterator pfi =
                DataOutputHandler.getPointIterator(fdp);
            Object[] values = new Object[metadata.size()];
            int      cnt    = 0;
            int batchCnt=0;
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
                StructureData structure = po.getData();
                for (PointDataMetadata pdm : metadata) {
                    Object value;
                    if (pdm.columnNumber == 0) {
                        value = new Double(lat);
                    } else if (pdm.columnNumber == 1) {
                        value = new Double(lon);
                    } else if (pdm.columnNumber == 2) {
                        value = new Double(alt);
                    } else if (pdm.columnNumber == 3) {
                        value = time;
                    } else {
                        StructureMembers.Member member =
                            structure.findMember((String) pdm.shortName);
                        if (pdm.isString()) {
                            value = structure.getScalarString(member);
                        } else {
                            value = new Double(
                                structure.convertScalarFloat(member));
                        }
                    }
                    values[pdm.columnNumber] = value;
                }

                getDatabaseManager().setValues(insertStmt, values);
                insertStmt.addBatch();
                batchCnt++;
                if(batchCnt>1000) {
                    insertStmt.executeBatch();
                    batchCnt=0;
                }
                if (((cnt++) % 100) == 0) {
                    System.err.println("added " + cnt +" observations");
                }
            }
            if(batchCnt>0) {
                insertStmt.executeBatch();
            }
        } catch (Exception exc) {
            try {
                getDatabaseManager().execute("drop table " + tableName, 1000,
                                             10000);
            } catch (Exception ignore) {}
            throw exc;
        } finally {
            try {
                connection.close();
            } catch(Exception ignore) {}
        }


    }

    public Result getHtmlDisplay(Request request, Entry entry) throws Exception {

        StringBuffer sb  = new StringBuffer();
        List<PointDataMetadata>metadata  =  getMetadata(getTableName(entry));
        sb.append("<ul>");
        for(PointDataMetadata pdm: metadata) {
            sb.append("<li>");
            sb.append(pdm.shortName);
            sb.append(" ");
            sb.append(pdm.longName);
            sb.append(" ");
            sb.append(pdm.unit);
        }
        sb.append("</ul>");



        return new Result("",sb);
    }





    private List<PointDataMetadata> getMetadata(String tableName) throws Exception {
        List<PointDataMetadata> metadata = new ArrayList<PointDataMetadata>();
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
            return varType.equals(TYPE_STRING);
        }

    }



}

