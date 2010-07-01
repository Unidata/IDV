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

package ucar.unidata.repository.type;


import org.w3c.dom.*;

import ucar.unidata.data.gis.KmlUtil;

import ucar.unidata.repository.*;

import ucar.unidata.repository.output.OutputType;

import ucar.unidata.sql.Clause;
import ucar.unidata.sql.SqlUtil;


import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.text.DecimalFormat;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;

import java.util.Hashtable;
import java.util.List;


/**
 */

public class Column implements Constants {


    /** _more_          */
    public static final String OUTPUT_HTML = "html";

    /** _more_          */
    public static final String OUTPUT_CSV = "csv";

    /** _more_          */
    private static SimpleDateFormat dateTimeFormat =
        new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /** _more_          */
    private static SimpleDateFormat dateFormat =
        new SimpleDateFormat("yyyy-MM-dd");

    /** _more_ */

    public static final String EXPR_EQUALS = "=";

    /** _more_ */
    public static final String EXPR_LE = "<=";

    /** _more_ */
    public static final String EXPR_GE = ">=";

    /** _more_ */
    public static final String EXPR_BETWEEN = "between";

    /** _more_ */
    public static final List EXPR_ITEMS =
        Misc.newList(new TwoFacedObject("=", EXPR_EQUALS),
                     new TwoFacedObject("<=", EXPR_LE),
                     new TwoFacedObject(">=", EXPR_GE),
                     new TwoFacedObject("between", EXPR_BETWEEN));

    /** _more_ */
    public static final String EXPR_PATTERN = EXPR_EQUALS + "|" + EXPR_LE
                                              + "|" + EXPR_GE + "|"
                                              + EXPR_BETWEEN;

    /** _more_ */
    public static final String TYPE_STRING = "string";

    /** _more_          */
    public static final String TYPE_ENTRY = "entry";

    /** _more_          */
    public static final String TYPE_FILE = "file";

    /** _more_ */
    public static final String TYPE_PASSWORD = "password";

    /** _more_ */
    public static final String TYPE_CLOB = "clob";

    /** _more_          */
    public static final String TYPE_EMAIL = "email";

    /** _more_          */
    public static final String TYPE_URL = "url";

    /** _more_ */
    public static final String TYPE_INT = "int";

    /** _more_ */
    public static final String TYPE_DOUBLE = "double";

    /** _more_          */
    public static final String TYPE_PERCENTAGE = "percentage";

    /** _more_ */
    public static final String TYPE_BOOLEAN = "boolean";

    /** _more_ */
    public static final String TYPE_ENUMERATION = "enumeration";

    /** _more_          */
    public static final String TYPE_ENUMERATIONPLUS = "enumerationplus";

    /** _more_ */
    public static final String TYPE_DATE = "date";

    /** _more_ */
    public static final String TYPE_DATETIME = "datetime";

    /** _more_ */
    public static final String TYPE_LATLONBBOX = "latlonbbox";


    /** _more_          */
    public static final String TYPE_LATLON = "latlon";

    /** _more_ */
    public static final String SEARCHTYPE_TEXT = "text";

    /** _more_ */
    public static final String SEARCHTYPE_SELECT = "select";


    /** _more_ */
    public static final String ATTR_NAME = "name";

    /** _more_          */
    public static final String ATTR_CHANGETYPE = "changetype";

    /** _more_          */
    public static final String ATTR_ADDTOFORM = "addtoform";

    /** _more_ */
    public static final String ATTR_GROUP = "group";

    /** _more_ */
    public static final String ATTR_OLDNAMES = "oldnames";

    /** _more_ */
    public static final String ATTR_SUFFIX = "suffix";

    /** _more_ */
    public static final String ATTR_PROPERTIES = "properties";

    /** _more_ */
    public static final String ATTR_LABEL = "label";

    /** _more_ */
    public static final String ATTR_DESCRIPTION = "description";

    /** _more_ */
    public static final String ATTR_TYPE = "type";

    /** _more_ */
    public static final String ATTR_ISINDEX = "isindex";

    /** _more_ */
    public static final String ATTR_CANSEARCH = "cansearch";

    /** _more_ */
    public static final String ATTR_CANLIST = "canlist";

    /** _more_ */
    public static final String ATTR_VALUES = "values";

    /** _more_ */
    public static final String ATTR_DEFAULT = "default";

    /** _more_ */
    public static final String ATTR_SIZE = "size";

    /** _more_ */
    public static final String ATTR_ROWS = "rows";

    /** _more_ */
    public static final String ATTR_COLUMNS = "columns";

    /** _more_ */
    public static final String ATTR_SEARCHTYPE = "searchtype";

    /** _more_ */
    public static final String ATTR_SHOWINHTML = "showinhtml";


    /** Lat/Lon format */
    private DecimalFormat latLonFormat = new DecimalFormat("##0.00");


    /** _more_ */
    private TypeHandler typeHandler;


    /** _more_ */
    private String name;

    /** _more_ */
    private String group;

    /** _more_ */
    private List oldNames;

    /** _more_ */
    private String label;

    /** _more_ */
    private String description;


    /** _more_ */
    private String type;

    /** _more_          */
    private boolean changeType = false;

    /** _more_ */
    private String suffix;

    /** _more_ */
    private String searchType = SEARCHTYPE_TEXT;

    /** _more_ */
    private boolean isIndex;

    /** _more_ */
    private boolean canSearch;

    /** _more_ */
    private boolean canList;

    /** _more_ */
    private List enumValues;



    /** _more_ */
    private String dflt;

    /** _more_ */
    private int size = 200;

    /** _more_ */
    private int rows = 1;

    /** _more_ */
    private int columns = 40;

    /** _more_ */
    private String propertiesFile;

    /** _more_ */
    private int offset;

    /** _more_ */
    private boolean canShow = true;


    /** _more_          */
    private boolean addToForm = true;

    /** _more_          */
    private Hashtable<String, String> properties = new Hashtable<String,
                                                       String>();

    /**
     * _more_
     *
     * @param typeHandler _more_
     * @param element _more_
     * @param offset _more_
     *
     * @throws Exception _more_
     */
    public Column(TypeHandler typeHandler, Element element, int offset)
            throws Exception {
        this.typeHandler = typeHandler;
        this.offset      = offset;
        name             = XmlUtil.getAttribute(element, ATTR_NAME);
        group = XmlUtil.getAttribute(element, ATTR_GROUP, (String) null);
        oldNames = StringUtil.split(XmlUtil.getAttribute(element,
                ATTR_OLDNAMES, ""), ",", true, true);
        suffix = XmlUtil.getAttribute(element, ATTR_SUFFIX, "");
        label  = XmlUtil.getAttribute(element, ATTR_LABEL, name);
        searchType = XmlUtil.getAttribute(element, ATTR_SEARCHTYPE,
                                          searchType);
        propertiesFile = XmlUtil.getAttribute(element, ATTR_PROPERTIES,
                (String) null);

        description = XmlUtil.getAttribute(element, ATTR_DESCRIPTION, label);
        type        = XmlUtil.getAttribute(element, ATTR_TYPE);
        changeType  = XmlUtil.getAttribute(element, ATTR_CHANGETYPE, false);
        dflt        = XmlUtil.getAttribute(element, ATTR_DEFAULT, "").trim();
        isIndex     = XmlUtil.getAttribute(element, ATTR_ISINDEX, false);
        canSearch   = XmlUtil.getAttribute(element, ATTR_CANSEARCH, false);
        addToForm   = XmlUtil.getAttribute(element, ATTR_ADDTOFORM,
                                           addToForm);
        canShow     = XmlUtil.getAttribute(element, ATTR_SHOWINHTML, canShow);
        canList     = XmlUtil.getAttribute(element, ATTR_CANLIST, false);
        size        = XmlUtil.getAttribute(element, ATTR_SIZE, size);
        rows        = XmlUtil.getAttribute(element, ATTR_ROWS, rows);
        columns     = XmlUtil.getAttribute(element, ATTR_COLUMNS, columns);

        List propNodes = XmlUtil.findChildren(element, "property");
        for (int i = 0; i < propNodes.size(); i++) {
            Element propNode = (Element) propNodes.get(i);
            properties.put(XmlUtil.getAttribute(propNode, "name"),
                           XmlUtil.getAttribute(propNode, "value"));
        }


        if (type.equals(TYPE_ENUMERATION)) {
            String valueString = XmlUtil.getAttribute(element, ATTR_VALUES,
                                     (String) null);
            if (valueString != null) {
                if (valueString.startsWith("file:")) {
                    valueString =
                        typeHandler.getStorageManager().readSystemResource(
                            valueString.substring("file:".length()));
                    enumValues = StringUtil.split(valueString, "\n", true,
                            true);
                } else {
                    enumValues = StringUtil.split(valueString, ",", true,
                            true);
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param t _more_
     *
     * @return _more_
     */
    public boolean isType(String t) {
        return type.equals(t);
    }

    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public String getProperty(String key) {
        return properties.get(key);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String msg(String s) {
        return typeHandler.msg(s);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public String msgLabel(String s) {
        return typeHandler.msgLabel(s);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Repository getRepository() {
        return typeHandler.getRepository();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isNumeric() {
        return isType(TYPE_INT) || isDouble();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnumeration() {
        return isType(TYPE_ENUMERATION) || isType(TYPE_ENUMERATIONPLUS);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDate() {
        return isType(TYPE_DATETIME) || isType(TYPE_DATE);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isDouble() {
        return isType(TYPE_DOUBLE) || isType(TYPE_PERCENTAGE);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isString() {
        return isType(TYPE_STRING) || isType(TYPE_ENUMERATION)
               || isType(TYPE_ENUMERATIONPLUS) || isType(TYPE_ENTRY)
               || isType(TYPE_EMAIL) || isType(TYPE_URL);
    }

    /**
     * _more_
     *
     * @param values _more_
     * @param idx _more_
     *
     * @return _more_
     */
    public String toString(Object[] values, int idx) {
        if (values == null) {
            return ((dflt != null)
                    ? dflt
                    : "");
        }
        if (values[idx] == null) {
            return ((dflt != null)
                    ? dflt
                    : "");
        }
        return values[idx].toString();
    }


    /**
     * _more_
     *
     * @param values _more_
     * @param idx _more_
     *
     * @return _more_
     */
    private String toLatLonString(Object[] values, int idx) {
        if (values == null) {
            return ((dflt != null)
                    ? dflt
                    : "NA");
        }
        if (values[idx] == null) {
            return ((dflt != null)
                    ? dflt
                    : "NA");
        }
        if ( !latLonOk(values[idx])) {
            return "NA";
        }
        double d = ((Double) values[idx]).doubleValue();
        return latLonFormat.format(d);
    }

    /**
     * _more_
     *
     * @param values _more_
     * @param idx _more_
     *
     * @return _more_
     */
    private boolean toBoolean(Object[] values, int idx) {
        if (values[idx] == null) {
            if (StringUtil.notEmpty(dflt)) {
                return new Boolean(dflt).booleanValue();
            }
            return true;
        }
        return ((Boolean) values[idx]).booleanValue();
    }


    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param sb _more_
     * @param output _more_
     * @param values _more_
     * @param valueIdx _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public void formatValue(Entry entry, StringBuffer sb, String output,
                            Object[] values)
            throws Exception {

        String delimiter = (Misc.equals(OUTPUT_CSV, output)
                            ? "|"
                            : ",");
        if (isType(TYPE_LATLON)) {
            sb.append(toLatLonString(values, offset));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 1));
        } else if (isType(TYPE_LATLONBBOX)) {
            sb.append(toLatLonString(values, offset));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 1));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 2));
            sb.append(delimiter);
            sb.append(toLatLonString(values, offset + 3));
        } else if (isType(TYPE_PERCENTAGE)) {
            if (Misc.equals(output, OUTPUT_CSV)) {
                sb.append(toString(values, offset));
            } else {
                //                System.err.println("offset:" + offset +" values:");
                //                Misc.printArray("", values);
                double percent = (Double) values[offset];
                sb.append((int) (percent * 100) + "");
            }
        } else if (isType(TYPE_DATETIME)) {
            sb.append(dateTimeFormat.format((Date) values[offset]));
        } else if (isType(TYPE_DATE)) {
            sb.append(dateFormat.format((Date) values[offset]));
        } else if (isType(TYPE_ENTRY)) {
            String entryId  = toString(values, offset);
            Entry  theEntry = null;
            if ((entryId != null) && (entryId.length() > 0)) {
                try {
                    theEntry =
                        getRepository().getEntryManager().getEntry(null,
                            entryId);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }
            if (Misc.equals(output, OUTPUT_CSV)) {
                sb.append(entryId);
            } else {
                if (theEntry != null) {
                    try {
                        String link =
                            getRepository().getEntryManager().getAjaxLink(
                                getRepository().getTmpRequest(), theEntry,
                                theEntry.getName()).toString();
                        sb.append(link);
                    } catch (Exception exc) {
                        throw new RuntimeException(exc);
                    }

                } else {
                    sb.append("---");
                }

            }
        } else if (isType(TYPE_EMAIL)) {
            String s = toString(values, offset);
            if (Misc.equals(output, OUTPUT_CSV)) {
                sb.append(s);
            } else {
                sb.append("<a href=\"mailto:" + s + "\">" + s + "</a>");
            }
        } else if (isType(TYPE_URL)) {
            String s = toString(values, offset);
            if (Misc.equals(output, OUTPUT_CSV)) {
                sb.append(s);
            } else {
                sb.append("<a href=\"" + s + "\">" + s + "</a>");
            }
        } else {
            String s = toString(values, offset);
            if (rows > 1) {
                s = typeHandler.getRepository().getHtmlOutputHandler()
                    .wikifyEntry(typeHandler.getRepository().getTmpRequest(),
                                 entry, s, false, null, null);
            }
            sb.append(s);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getOffset() {
        return offset;
    }

    /**
     * _more_
     *
     * @param statement _more_
     * @param values _more_
     * @param statementIdx _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected int setValues(PreparedStatement statement, Object[] values,
                            int statementIdx)
            throws Exception {
        if (isType(TYPE_INT)) {
            if (values[offset] != null) {
                statement.setInt(statementIdx,
                                 ((Integer) values[offset]).intValue());
            } else {
                statement.setInt(statementIdx, 0);
            }
            statementIdx++;
        } else if (isDouble()) {
            if (values[offset] != null) {
                statement.setDouble(statementIdx,
                                    ((Double) values[offset]).doubleValue());
            } else {
                statement.setDouble(statementIdx, 0.0);
            }
            statementIdx++;
        } else if (isType(TYPE_BOOLEAN)) {
            if (values[offset] != null) {
                boolean v = ((Boolean) values[offset]).booleanValue();
                statement.setInt(statementIdx, (v
                        ? 1
                        : 0));
            } else {
                statement.setInt(statementIdx, 0);
            }
            statementIdx++;
        } else if (isDate()) {
            Date dttm = (Date) values[offset];
            typeHandler.getRepository().getDatabaseManager().setDate(
                statement, statementIdx, dttm);
            statementIdx++;
        } else if (isType(TYPE_LATLON)) {
            if (values[offset] != null) {
                double lat = ((Double) values[offset]).doubleValue();
                statement.setDouble(statementIdx, lat);
                double lon = ((Double) values[offset + 1]).doubleValue();
                statement.setDouble(statementIdx + 1, lon);
            } else {
                statement.setDouble(statementIdx, Entry.NONGEO);
                statement.setDouble(statementIdx + 1, Entry.NONGEO);
            }
            statementIdx += 2;
        } else if (isType(TYPE_LATLONBBOX)) {
            for (int i = 0; i < 4; i++) {
                if (values[offset + i] != null) {
                    statement.setDouble(
                        statementIdx++,
                        ((Double) values[offset + i]).doubleValue());
                } else {
                    statement.setDouble(statementIdx++, Entry.NONGEO);
                }
            }
        } else if (isType(TYPE_PASSWORD)) {
            if (values[offset] != null) {
                String value =
                    new String(XmlUtil.encodeBase64(toString(values,
                        offset).getBytes()).getBytes());
                statement.setString(statementIdx, value);
            } else {
                statement.setString(statementIdx, null);
            }
            statementIdx++;
        } else {
            //            System.err.println("\tset statement:" + offset + " " + values[offset]);

            if (values[offset] != null) {
                statement.setString(statementIdx, toString(values, offset));
            } else {
                statement.setString(statementIdx, null);
            }
            statementIdx++;
        }
        return statementIdx;


    }


    /**
     * _more_
     *
     * @param results _more_
     * @param values _more_
     * @param valueIdx _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int readValues(ResultSet results, Object[] values, int valueIdx)
            throws Exception {
        if (isType(TYPE_INT)) {
            values[offset] = new Integer(results.getInt(valueIdx));
            valueIdx++;
        } else if (isType(TYPE_PERCENTAGE)) {
            values[offset] = new Double(results.getDouble(valueIdx));
            valueIdx++;
        } else if (isDouble()) {
            values[offset] = new Double(results.getDouble(valueIdx));
            valueIdx++;
        } else if (isType(TYPE_BOOLEAN)) {
            values[offset] = new Boolean(results.getInt(valueIdx) == 1);
            valueIdx++;
        } else if (isDate()) {
            values[offset] =
                typeHandler.getDatabaseManager().getTimestamp(results,
                    valueIdx);
            valueIdx++;
        } else if (isType(TYPE_LATLON)) {
            values[offset] = new Double(results.getDouble(valueIdx));
            valueIdx++;
            values[offset + 1] = new Double(results.getDouble(valueIdx));
            valueIdx++;

        } else if (isType(TYPE_LATLONBBOX)) {
            values[offset]     = new Double(results.getDouble(valueIdx++));
            values[offset + 1] = new Double(results.getDouble(valueIdx++));
            values[offset + 2] = new Double(results.getDouble(valueIdx++));
            values[offset + 3] = new Double(results.getDouble(valueIdx++));

        } else if (isType(TYPE_PASSWORD)) {
            String value = results.getString(valueIdx);
            if (value != null) {
                byte[] bytes = XmlUtil.decodeBase64(value);
                if (bytes != null) {
                    value = new String(bytes);
                }
            }
            values[offset] = value;
            valueIdx++;
        } else {
            values[offset] = results.getString(valueIdx);
            valueIdx++;
        }
        return valueIdx;
    }


    /**
     * _more_
     *
     * @param statement _more_
     * @param name _more_
     * @param type _more_
     *
     * @throws Exception _more_
     */
    private void defineColumn(Statement statement, String name, String type)
            throws Exception {
        String sql = "alter table " + getTableName() + " add column " + name
                     + " " + type;
        SqlUtil.loadSql(sql, statement, true);


        if (changeType) {
            if (typeHandler.getDatabaseManager().isDatabaseDerby()) {
                sql = "alter table " + getTableName() + "  alter column "
                      + name + "  set data type " + type + ";";
            } else {
                sql = "alter table " + getTableName() + " modify column "
                      + name + " " + type + ";";
            }
            System.err.println("altering table: " + sql);
            SqlUtil.loadSql(sql, statement, true);
        }
    }


    /**
     * _more_
     *
     *
     * @param statement _more_
     *
     * @throws Exception _more_
     */
    public void createTable(Statement statement) throws Exception {
        if (isType(TYPE_STRING) || isType(TYPE_PASSWORD)
                || isType(TYPE_EMAIL) || isType(TYPE_URL)
                || isType(TYPE_ENTRY)) {
            defineColumn(statement, name, "varchar(" + size + ") ");
        } else if (isType(TYPE_CLOB)) {
            String clobType =
                typeHandler.getRepository().getDatabaseManager().convertType(
                    "clob", size);
            defineColumn(statement, name, clobType);
        } else if (isType(TYPE_ENUMERATION) || isType(TYPE_ENUMERATIONPLUS)) {
            defineColumn(statement, name, "varchar(" + size + ") ");
        } else if (isType(TYPE_INT)) {
            defineColumn(statement, name, "int");
        } else if (isDouble()) {
            defineColumn(
                statement, name,
                typeHandler.getRepository().getDatabaseManager().convertType(
                    "double"));
        } else if (isType(TYPE_BOOLEAN)) {
            //use int as boolean for database compatibility
            defineColumn(statement, name, "int");

        } else if (isDate()) {
            defineColumn(
                statement, name,
                typeHandler.getDatabaseManager().convertSql(
                    "ramadda.datetime"));
        } else if (isType(TYPE_LATLON)) {
            defineColumn(
                statement, name + "_lat",
                typeHandler.getRepository().getDatabaseManager().convertType(
                    "double"));
            defineColumn(
                statement, name + "_lon",
                typeHandler.getRepository().getDatabaseManager().convertType(
                    "double"));
        } else if (isType(TYPE_LATLONBBOX)) {
            defineColumn(
                statement, name + "_north",
                typeHandler.getRepository().getDatabaseManager().convertType(
                    "double"));
            defineColumn(
                statement, name + "_west",
                typeHandler.getRepository().getDatabaseManager().convertType(
                    "double"));
            defineColumn(
                statement, name + "_south",
                typeHandler.getRepository().getDatabaseManager().convertType(
                    "double"));
            defineColumn(
                statement, name + "_east",
                typeHandler.getRepository().getDatabaseManager().convertType(
                    "double"));

        } else {
            throw new IllegalArgumentException("Unknown column type:" + type
                    + " for " + name);
        }


        for (int i = 0; i < oldNames.size(); i++) {
            String sql = "update " + getTableName() + " set " + name + " = "
                         + oldNames.get(i);
            SqlUtil.loadSql(sql, statement, true);
            sql = "alter table " + getTableName() + " drop "
                  + oldNames.get(i);
            SqlUtil.loadSql(sql, statement, true);
        }

        if (isIndex) {
            SqlUtil.loadSql("CREATE INDEX " + getTableName() + "_INDEX_"
                            + name + "  ON " + getTableName() + " (" + name
                            + ")", statement, true);
        }

    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public Object convert(String value) {
        if (isType(TYPE_INT)) {
            return new Integer(value);
        } else if (isDouble()) {
            return new Double(value);
        } else if (isType(TYPE_BOOLEAN)) {
            return new Boolean(value);
        } else if (isType(TYPE_DATETIME)) {
            //TODO
        } else if (isType(TYPE_DATE)) {
            //TODO
        } else if (isType(TYPE_LATLON)) {
            //TODO
        } else if (isType(TYPE_LATLONBBOX)) {
            //TODO
        }
        return value;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getTableName() {
        return typeHandler.getTableName();
    }



    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    private boolean latLonOk(Object o) {
        if (o == null) {
            return false;
        }
        Double d = (Double) o;
        return latLonOk(d.doubleValue());
    }

    /**
     * _more_
     *
     * @param v _more_
     *
     * @return _more_
     */
    private boolean latLonOk(double v) {
        return ((v == v) && (v != Entry.NONGEO));
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param where _more_
     * @param searchCriteria _more_
     *
     * @throws Exception _more_
     */
    public void assembleWhereClause(Request request, List<Clause> where,
                                    StringBuffer searchCriteria)
            throws Exception {

        String id = getFullName();
        if (isType(TYPE_LATLON)) {
            double north = request.get(id + "_north", Double.NaN);
            double south = request.get(id + "_south", Double.NaN);
            double east  = request.get(id + "_east", Double.NaN);
            double west  = request.get(id + "_west", Double.NaN);
            if (latLonOk(north)) {
                where.add(Clause.le(id + "_lat", north));
            }
            if (latLonOk(south)) {
                where.add(Clause.ge(id + "_lat", south));
            }
            if (latLonOk(west)) {
                where.add(Clause.ge(id + "_lon", west));
            }
            if (latLonOk(east)) {
                where.add(Clause.le(id + "_lon", east));
            }
        } else if (isType(TYPE_LATLONBBOX)) {
            double north = request.get(id + "_north", Double.NaN);
            double south = request.get(id + "_south", Double.NaN);
            double east  = request.get(id + "_east", Double.NaN);
            double west  = request.get(id + "_west", Double.NaN);

            if (latLonOk(north)) {
                where.add(Clause.le(id + "_north", north));
            }
            if (latLonOk(south)) {
                where.add(Clause.ge(id + "_south", south));
            }
            if (latLonOk(west)) {
                where.add(Clause.ge(id + "_west", west));
            }
            if (latLonOk(east)) {
                where.add(Clause.le(id + "_east", east));
            }
        } else if (isNumeric()) {
            String expr = request.getCheckedString(id + "_expr", EXPR_EQUALS,
                              EXPR_PATTERN);
            double from  = request.get(id + "_from", Double.NaN);
            double to    = request.get(id + "_to", Double.NaN);
            double value = request.get(id, Double.NaN);

            if (isType(TYPE_PERCENTAGE)) {
                from  = from / 100.0;
                to    = to / 100.0;
                value = value / 100.0;
            }
            if ((from == from) && (to != to)) {
                to = value;
            } else if ((from != from) && (to == to)) {
                from = value;
            } else if ((from != from) && (to != to)) {
                from = value;
                to   = value;
            }
            if (from == from) {
                if (expr.equals(EXPR_EQUALS)) {
                    where.add(Clause.eq(getFullName(), from));
                } else if (expr.equals(EXPR_LE)) {
                    where.add(Clause.le(getFullName(), from));
                } else if (expr.equals(EXPR_GE)) {
                    where.add(Clause.ge(getFullName(), from));
                } else if (expr.equals(EXPR_BETWEEN)) {
                    where.add(Clause.ge(getFullName(), from));
                    where.add(Clause.le(getFullName(), to));
                } else if (expr.length() > 0) {
                    throw new IllegalArgumentException("Unknown expression:"
                            + expr);
                }
            }
        } else if (isType(TYPE_BOOLEAN)) {
            if (request.defined(id)) {
                where.add(Clause.eq(getFullName(), (request.get(id, true)
                        ? 1
                        : 0)));
            }
        } else if (isDate()) {
            String relativeArg = id + "_relative";
            Date[] dateRange = request.getDateRange(id + "_fromdate",
                                   id + "_todate", relativeArg, new Date());
            if (dateRange[0] != null) {
                where.add(Clause.ge(getFullName(), dateRange[0]));
            }

            if (dateRange[1] != null) {
                where.add(Clause.le(getFullName(), dateRange[1]));
            }
        } else if (isType(TYPE_ENTRY)) {
            String value = request.getString(id + "_hidden", "");
            if (value.length() > 0) {
                where.add(Clause.eq(getFullName(), value));
            }
        } else {
            String value = request.getString(id, "");
            typeHandler.addOrClause(getFullName(),
                                    (String) request.getString(getFullName(),
                                        (String) null), where);

        }


    }




    /**
     * _more_
     *
     * @param arg _more_
     * @param value _more_
     * @param entry _more_
     * @param values _more_
     *
     * @return _more_
     */
    public int matchValue(String arg, Object value, Object[] values) {
        if (isType(TYPE_LATLON)) {
            //TODO:
        } else if (isType(TYPE_LATLONBBOX)) {
            //TODO:
        } else if (isType(TYPE_BOOLEAN)) {
            if (arg.equals(getFullName())) {
                if (values[offset].toString().equals(value)) {
                    return TypeHandler.MATCH_TRUE;
                }
                return TypeHandler.MATCH_FALSE;
            }
        } else if (isNumeric()) {
            //
        } else {
            if (arg.equals(getFullName())) {
                if (values[offset].equals(value)) {
                    return TypeHandler.MATCH_TRUE;
                }
                return TypeHandler.MATCH_FALSE;
            }
        }
        return TypeHandler.MATCH_UNKNOWN;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param entry _more_
     * @param values _more_
     * @param state _more_
     *
     * @throws Exception _more_
     */
    public void addToEntryForm(Request request, Entry entry,
                               StringBuffer formBuffer, Object[] values,
                               Hashtable state)
            throws Exception {
        if ( !addToForm) {
            return;
        }
        String widget = getFormWidget(request, entry, values);
        //        formBuffer.append(HtmlUtil.formEntry(getLabel() + ":",
        //                                             HtmlUtil.hbox(widget, suffix)));
        if ((group != null) && (state.get(group) == null)) {
            formBuffer.append(
                HtmlUtil.row(
                    HtmlUtil.colspan(
                        HtmlUtil.div(group, " class=\"formgroupheader\" "),
                        2)));
            state.put(group, group);
        }
        if (rows > 1) {
            formBuffer.append(HtmlUtil.formEntryTop(getLabel() + ":",
                    widget));
        } else {
            formBuffer.append(HtmlUtil.formEntry(getLabel() + ":", widget));
        }
        formBuffer.append("\n");
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param values _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getFormWidget(Request request, Entry entry, Object[] values)
            throws Exception {

        String widget = "";
        String id     = getFullName();
        if (isType(TYPE_LATLON)) {
            double lat = 0;
            double lon = 0;
            if (values != null) {
                lat = ((Double) values[offset]).doubleValue();
                lon = ((Double) values[offset + 1]).doubleValue();
            }
            widget =
                typeHandler.getRepository().getMapManager().makeMapSelector(
                    id, true, "", "", new String[] { latLonOk(lat)
                    ? lat + ""
                    : "", latLonOk(lon)
                          ? lon + ""
                          : "" });
        } else if (isType(TYPE_LATLONBBOX)) {
            if (values != null) {
                String[] snew = {
                    latLonOk(values[offset + 2]) ? values[offset + 2] + "" : "", 
                    latLonOk(values[offset + 0]) ? values[offset + 0] + "" : "", 
                    latLonOk(values[offset + 3]) ? values[offset + 3] + "" : "", 
                    latLonOk(values[offset + 1]) ? values[offset + 1] + "" : ""
                };
                widget =
                    typeHandler.getRepository().getMapManager()
                        .makeMapSelector(id, true, "", "", snew);
            } else {
                widget =
                    typeHandler.getRepository().getMapManager()
                        .makeMapSelector(request, id, true, "", "");
            }
        } else if (isType(TYPE_BOOLEAN)) {
            String value = "True";
            if (values != null) {
                if (toBoolean(values, offset)) {
                    value = "True";
                } else {
                    value = "False";
                }
            }
            widget = HtmlUtil.select(id, Misc.newList("True", "False"),
                                     value);
        } else if (isType(TYPE_DATETIME)) {
            Date date;
            if (values != null) {
                date = (Date) values[offset];
            } else {
                date = new Date();
            }
            widget = typeHandler.getRepository().makeDateInput(request, id,
                    "", date, null);
        } else if (isType(TYPE_DATE)) {
            Date date;
            if (values != null) {
                date = (Date) values[offset];
            } else {
                date = new Date();
            }
            widget = typeHandler.getRepository().makeDateInput(request, id,
                    "", date, null, false);
        } else if (isType(TYPE_ENUMERATION)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = (String) toString(values, offset);
            }
            widget = HtmlUtil.select(id, enumValues, value);
        } else if (isType(TYPE_ENUMERATIONPLUS)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = (String) toString(values, offset);
            }
            widget = HtmlUtil.select(id,
                                     typeHandler.getEnumValues(this, entry),
                                     value) + "  or:  "
                                            + HtmlUtil.input(id + "_plus",
                                                "", HtmlUtil.SIZE_10);
        } else if (isType(TYPE_INT)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HtmlUtil.input(id, value, HtmlUtil.SIZE_10);
        } else if (isType(TYPE_DOUBLE)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HtmlUtil.input(id, value, HtmlUtil.SIZE_10);
        } else if (isType(TYPE_PERCENTAGE)) {
            String value = ((dflt != null)
                            ? dflt
                            : "0");

            if (values != null) {
                value = "" + toString(values, offset);
            }
            if (value.trim().length() == 0) {
                value = "0";
            }
            double d          = new Double(value).doubleValue();
            int    percentage = (int) (d * 100);
            widget = HtmlUtil.input(id, percentage + "", HtmlUtil.SIZE_5)
                     + "%";
        } else if (isType(TYPE_PASSWORD)) {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = "" + toString(values, offset);
            }
            widget = HtmlUtil.password(id, value, HtmlUtil.SIZE_10);
        } else if (isType(TYPE_ENTRY)) {
            String value = "";
            if (values != null) {
                value = toString(values, offset);
            }

            Entry theEntry = null;
            if (value.length() > 0) {
                theEntry =
                    getRepository().getEntryManager().getEntry(request,
                        value);
            }
            StringBuffer sb = new StringBuffer();
            String select =
                getRepository().getHtmlOutputHandler().getSelect(request, id,
                    "Select", true, null, entry);
            sb.append(HtmlUtil.hidden(id + "_hidden", value,
                                      HtmlUtil.id(id + "_hidden")));
            sb.append(HtmlUtil.disabledInput(id, ((theEntry != null)
                    ? theEntry.getFullName()
                    : ""), HtmlUtil.id(id) + HtmlUtil.SIZE_60) + select);

            widget = sb.toString();
        } else {
            String value = ((dflt != null)
                            ? dflt
                            : "");
            if (values != null) {
                value = toString(values, offset);
            } else if (request.defined(id)) {
                value = request.getString(id);
            }
            if (searchType.equals(SEARCHTYPE_SELECT)) {
                Hashtable props =
                    typeHandler.getRepository().getFieldProperties(
                        propertiesFile);
                List<TwoFacedObject> tfos = new ArrayList<TwoFacedObject>();
                if (props != null) {
                    for (Enumeration keys = props.keys();
                            keys.hasMoreElements(); ) {
                        String xid = (String) keys.nextElement();
                        if (xid.endsWith(".label")) {
                            xid = xid.substring(0,
                                    xid.length() - ".label".length());
                            tfos.add(new TwoFacedObject(getLabel(xid), xid));
                        }
                    }
                }

                tfos = (List<TwoFacedObject>) Misc.sort(tfos);
                if (tfos.size() == 0) {
                    widget = HtmlUtil.input(id, value, " size=10 ");
                } else {

                    widget = HtmlUtil.select(id, tfos, value);
                }
            } else if (rows > 1) {
                widget = HtmlUtil.textArea(id, value, rows, columns);
            } else {
                widget = HtmlUtil.input(id, value,
                                        "size=\"" + columns + "\"");
            }
        }
        return HtmlUtil.hbox(widget, HtmlUtil.inset(suffix, 5));

    }



    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public double[] getLatLonBbox(Object[] values) {
        return new double[] { (Double) values[offset],
                              (Double) values[offset + 1],
                              (Double) values[offset + 2],
                              (Double) values[offset + 3] };
    }


    /**
     * _more_
     *
     * @param values _more_
     *
     * @return _more_
     */
    public double[] getLatLon(Object[] values) {
        return new double[] { (Double) values[offset],
                              (Double) values[offset + 1] };
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public void setValue(Request request, Entry entry, Object[] values)
            throws Exception {
        if ( !addToForm) {
            return;
        }

        String id = getFullName();

        if (isType(TYPE_LATLON)) {
            if (request.exists(id + "_lat")) {
                values[offset] = new Double(request.getString(id + "_lat",
                        "0").trim());
                values[offset + 1] = new Double(request.getString(id
                        + "_lon", "0").trim());
            }
        } else if (isType(TYPE_LATLONBBOX)) {
            values[offset] = new Double(request.get(id + "_north",
                    Entry.NONGEO));
            values[offset + 1] = new Double(request.get(id + "_west",
                    Entry.NONGEO));
            values[offset + 2] = new Double(request.get(id + "_south",
                    Entry.NONGEO));
            values[offset + 3] = new Double(request.get(id + "_east",
                    Entry.NONGEO));
        } else if (isDate()) {
            values[offset] = request.getDate(id, new Date());
        } else if (isType(TYPE_BOOLEAN)) {
            String value = request.getString(id, (StringUtil.notEmpty(dflt)
                    ? dflt
                    : "true")).toLowerCase();
            values[offset] = new Boolean(value);
        } else if (isType(TYPE_ENUMERATION)) {
            if (request.exists(id)) {
                values[offset] = request.getString(id, ((dflt != null)
                        ? dflt
                        : ""));
            } else {
                values[offset] = dflt;
            }
        } else if (isType(TYPE_ENUMERATIONPLUS)) {
            String theValue = "";
            if (request.defined(id + "_plus")) {
                theValue = request.getString(id + "_plus", ((dflt != null)
                        ? dflt
                        : ""));
            } else if (request.defined(id)) {
                theValue = request.getString(id, ((dflt != null)
                        ? dflt
                        : ""));

            } else {
                theValue = dflt;
            }
            values[offset] = theValue;
            typeHandler.addEnumValue(this, entry, theValue);
        } else if (isType(TYPE_INT)) {
            int dfltValue = (StringUtil.notEmpty(dflt)
                             ? new Integer(dflt).intValue()
                             : 0);
            if (request.exists(id)) {
                values[offset] = new Integer(request.get(id, dfltValue));
            } else {
                values[offset] = dfltValue;
            }
        } else if (isType(TYPE_PERCENTAGE)) {
            double dfltValue = (StringUtil.notEmpty(dflt)
                                ? new Double(dflt.trim()).doubleValue()
                                : 0);
            if (request.exists(id)) {
                values[offset] = new Double(request.get(id, dfltValue) / 100);
            } else {
                values[offset] = dfltValue;

            }
        } else if (isDouble()) {
            double dfltValue = (StringUtil.notEmpty(dflt)
                                ? new Double(dflt.trim()).doubleValue()
                                : 0);
            if (request.exists(id)) {
                values[offset] = new Double(request.get(id, dfltValue));
            } else {
                values[offset] = dfltValue;

            }
        } else if (isType(TYPE_ENTRY)) {
            values[offset] = request.getString(id + "_hidden", "");
        } else {
            if (request.exists(id)) {
                values[offset] = request.getString(id, ((dflt != null)
                        ? dflt
                        : ""));
            } else {
                values[offset] = dflt;
            }
        }
    }




    /**
     * _more_
     *
     * @param formBuffer _more_
     * @param request _more_
     * @param where _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer formBuffer,
                                List<Clause> where)
            throws Exception {
        addToSearchForm(request, formBuffer, where, null);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param formBuffer _more_
     * @param where _more_
     * @param entry _more_
     *
     * @throws Exception _more_
     */
    public void addToSearchForm(Request request, StringBuffer formBuffer,
                                List<Clause> where, Entry entry)
            throws Exception {

        if ( !getCanSearch()) {
            return;
        }

        String       id     = getFullName();

        List<Clause> tmp    = new ArrayList<Clause>(where);
        String       widget = "";
        if (isType(TYPE_LATLON)) {
            widget =
                typeHandler.getRepository().getMapManager().makeMapSelector(
                    request, id, true, "", "");
        } else if (isType(TYPE_LATLONBBOX)) {
            widget =
                typeHandler.getRepository().getMapManager().makeMapSelector(
                    request, id, true, "", "");
        } else if (isDate()) {
            List dateSelect = new ArrayList();
            dateSelect.add(new TwoFacedObject(msg("Relative Date"), "none"));
            dateSelect.add(new TwoFacedObject(msg("Last hour"), "-1 hour"));
            dateSelect.add(new TwoFacedObject(msg("Last 3 hours"),
                    "-3 hours"));
            dateSelect.add(new TwoFacedObject(msg("Last 6 hours"),
                    "-6 hours"));
            dateSelect.add(new TwoFacedObject(msg("Last 12 hours"),
                    "-12 hours"));
            dateSelect.add(new TwoFacedObject(msg("Last day"), "-1 day"));
            dateSelect.add(new TwoFacedObject(msg("Last 7 days"), "-7 days"));
            String dateSelectValue;
            String relativeArg = id + "_relative";
            if (request.exists(relativeArg)) {
                dateSelectValue = request.getString(relativeArg, "");
            } else {
                dateSelectValue = "none";
            }

            String dateSelectInput = HtmlUtil.select(id + "_relative",
                                         dateSelect, dateSelectValue);

            widget = getRepository().makeDateInput(
                request, id + "_fromdate", "searchform", null, null,
                isType(TYPE_DATETIME)) + HtmlUtil.space(1)
                    + HtmlUtil.img(getRepository().iconUrl(ICON_RANGE))
                    + HtmlUtil.space(1)
                    + getRepository().makeDateInput(
                        request, id + "_todate", "searchform", null, null,
                            isType(TYPE_DATETIME)) + HtmlUtil.space(4)
                                + msgLabel("Or") + dateSelectInput;


        } else if (isType(TYPE_BOOLEAN)) {
            widget = HtmlUtil.select(id,
                                     Misc.newList(TypeHandler.ALL_OBJECT,
                                         "True",
                                         "False"), request.getString(id, ""));
        } else if (isType(TYPE_ENUMERATION)) {
            List tmpValues = Misc.newList(TypeHandler.ALL_OBJECT);
            tmpValues.addAll(enumValues);
            widget = HtmlUtil.select(id, tmpValues, request.getString(id));
        } else if (isType(TYPE_ENUMERATIONPLUS)) {
            List tmpValues = Misc.newList(TypeHandler.ALL_OBJECT);
            tmpValues.addAll(typeHandler.getEnumValues(this, entry));
            widget = HtmlUtil.select(id, tmpValues, request.getString(id));
        } else if (isNumeric()) {
            String expr = HtmlUtil.select(id + "_expr", EXPR_ITEMS,
                                          request.getString(id + "_expr",
                                              ""));
            widget = expr
                     + HtmlUtil.input(id + "_from",
                                      request.getString(id + "_from", ""),
                                      "size=\"10\"") + HtmlUtil.input(id
                                      + "_to", request.getString(id + "_to",
                                          ""), "size=\"10\"");
        } else if (isType(TYPE_ENTRY)) {


            String entryId  = request.getString(id + "_hidden", "");
            Entry  theEntry = null;
            if ((entryId != null) && (entryId.length() > 0)) {
                try {
                    theEntry =
                        getRepository().getEntryManager().getEntry(null,
                            entryId);
                } catch (Exception exc) {
                    throw new RuntimeException(exc);
                }
            }

            String select =
                getRepository().getHtmlOutputHandler().getSelect(request, id,
                    "Select", true, null, entry);
            StringBuffer sb = new StringBuffer();
            sb.append(HtmlUtil.hidden(id + "_hidden", entryId,
                                      HtmlUtil.id(id + "_hidden")));
            sb.append(HtmlUtil.disabledInput(id, ((theEntry != null)
                    ? theEntry.getFullName()
                    : ""), HtmlUtil.id(id) + HtmlUtil.SIZE_60) + select);

            widget = sb.toString();
        } else {
            if (searchType.equals(SEARCHTYPE_SELECT)) {
                long t1 = System.currentTimeMillis();
                Statement statement = typeHandler.select(request,
                                          SqlUtil.distinct(id), tmp, "");
                long t2 = System.currentTimeMillis();
                String[] values =
                    SqlUtil.readString(
                        typeHandler.getDatabaseManager().getIterator(
                            statement), 1);
                long t3 = System.currentTimeMillis();
                //                System.err.println("TIME:" + (t2-t1) + " " + (t3-t2));
                List<TwoFacedObject> list = new ArrayList();
                for (int i = 0; i < values.length; i++) {
                    if (values[i] == null) {
                        continue;
                    }
                    list.add(new TwoFacedObject(getLabel(values[i]),
                            values[i]));
                }

                List sorted = Misc.sort(list);
                list = new ArrayList<TwoFacedObject>();
                list.addAll(sorted);
                if (list.size() == 1) {
                    widget =
                        HtmlUtil.hidden(id, (String) list.get(0).getId())
                        + " " + list.get(0).toString();
                } else {
                    list.add(0, TypeHandler.ALL_OBJECT);
                    widget = HtmlUtil.select(id, list);
                }
            } else if (rows > 1) {
                widget = HtmlUtil.textArea(id, request.getString(id, ""),
                                           rows, columns);
            } else {
                widget = HtmlUtil.input(id, request.getString(id, ""),
                                        "size=\"" + columns + "\"");
            }
        }
        formBuffer.append(
            HtmlUtil.formEntry(
                getLabel() + ":",
                "<table>" + HtmlUtil.row(HtmlUtil.cols(widget, suffix))
                + "</table>"));
        formBuffer.append("\n");
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected String getLabel(String value) throws Exception {
        String desc = typeHandler.getRepository().getFieldDescription(value
                          + ".label", propertiesFile);
        if (desc == null) {
            desc = value;
        } else {
            if (desc.indexOf("${value}") >= 0) {
                desc = desc.replace("${value}", value);
            }
        }
        return desc;

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullName() {
        return typeHandler.getTableName() + "." + name;
    }


    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<String> getColumnNames() {
        List<String> names = new ArrayList<String>();
        if (isType(TYPE_LATLON)) {
            names.add(name + "_lat");
            names.add(name + "_lon");
        } else if (isType(TYPE_LATLONBBOX)) {
            names.add(name + "_north");
            names.add(name + "_west");
            names.add(name + "_south");
            names.add(name + "_east");
        } else {
            names.add(name);
        }
        return names;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getSortByColumn() {
        if (isType(TYPE_LATLON)) {
            return name + "_lat";
        }
        if (isType(TYPE_LATLONBBOX)) {
            return name + "_north";
        }
        return name;
    }


    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getPropertiesFile() {
        return propertiesFile;
    }

    /**
     * Set the Label property.
     *
     * @param value The new value for Label
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     * Get the Label property.
     *
     * @return The Label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(String value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public String getType() {
        return type;
    }

    /**
     * Set the IsIndex property.
     *
     * @param value The new value for IsIndex
     */
    public void setIsIndex(boolean value) {
        isIndex = value;
    }

    /**
     * Get the IsIndex property.
     *
     * @return The IsIndex
     */
    public boolean getIsIndex() {
        return isIndex;
    }



    /**
     *  Set the CanShow property.
     *
     *  @param value The new value for CanShow
     */
    public void setCanShow(boolean value) {
        canShow = value;
    }

    /**
     *  Get the CanShow property.
     *
     *  @return The CanShow
     */
    public boolean getCanShow() {
        return canShow;
    }



    /**
     * Set the IsSearchable property.
     *
     * @param value The new value for IsSearchable
     */
    public void setCanSearch(boolean value) {
        canSearch = value;
    }

    /**
     * Get the IsSearchable property.
     *
     * @return The IsSearchable
     */
    public boolean getCanSearch() {
        return canSearch;
    }

    /**
     * Set the IsListable property.
     *
     * @param value The new value for IsListable
     */
    public void setCanList(boolean value) {
        canList = value;
    }

    /**
     * Get the IsListable property.
     *
     * @return The IsListable
     */
    public boolean getCanList() {
        return canList;
    }



    /**
     * Set the Values property.
     *
     * @param value The new value for Values
     */
    public void setValues(List value) {
        enumValues = value;
    }

    /**
     * Get the Values property.
     *
     * @return The Values
     */
    public List getValues() {
        return enumValues;
    }

    /**
     * Set the Dflt property.
     *
     * @param value The new value for Dflt
     */
    public void setDflt(String value) {
        dflt = value;
    }

    /**
     * Get the Dflt property.
     *
     * @return The Dflt
     */
    public String getDflt() {
        return dflt;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + ":" + offset;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getRows() {
        return rows;
    }

}
