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



package ucar.unidata.data;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.io.File;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;


import java.text.SimpleDateFormat;



import java.util.regex.*;


/**
 * Class SqlUtils _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class SqlUtils {


    /** _more_          */
    private static final SimpleDateFormat sdf =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String quote(Object s) {
        return "'" + s.toString() + "'";
    }


    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static String format(Date d) {
        return sdf.format(d);
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     *
     * @return _more_
     */
    public static String comma(Object s1, Object s2) {
        return s1.toString() + "," + s2.toString();
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     *
     * @return _more_
     */
    public static String comma(Object s1, Object s2, Object s3) {
        return s1.toString() + "," + s2.toString() + "," + s3.toString();
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     *
     * @return _more_
     */
    public static String comma(Object s1, Object s2, Object s3, Object s4) {
        return s1.toString() + "," + s2.toString() + "," + s3.toString()
               + "," + s4.toString();
    }

    /**
     * _more_
     *
     * @param sb _more_
     * @param table _more_
     * @param values _more_
     */
    public static void makeInsert(StringBuffer sb, String table,
                                  String values) {
        sb.append("INSERT INTO ");
        sb.append(table);
        sb.append(" VALUES (");
        sb.append(values);
        sb.append(")");
    }

    /**
     * _more_
     *
     * @param what _more_
     * @param where _more_
     *
     * @return _more_
     */
    public static String makeSelect(String what, String where) {
        return makeSelect(what, where, "");
    }

    public static String makeSelect(String what, String where, String extra) {
        return "SELECT " + what + " FROM " + where + " " + extra;
    }





    public static void loadSql(String sql, Statement statement) 
            throws Exception {
        List<String> toks = (List<String>)StringUtil.split(sql,"\n");
        StringBuffer sb = new StringBuffer();
        for(String line: toks) {
            String trimLine = line.trim();
            if(trimLine.startsWith("--")) continue;
            sb.append (line);
            sb.append ("\n");
            if(trimLine.endsWith(";")) {
                String lineSql = sb.toString().trim();
                //Strip off the ";"
                lineSql = lineSql.substring(0, lineSql.length()-1);
                //                System.err.println ("EVAL:" +lineSql);
                statement.execute(lineSql);
                sb = new StringBuffer();
            }
        }
    }


    /**
     * _more_
     *
     * @param stmt _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static double[] readTime(Statement stmt, int column)
            throws Exception {
        ResultSet results;
        double[]  current = new double[10000];
        int       cnt     = 0;
        Iterator  iter    = getIterator(stmt);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                Date   dttm  = results.getDate(column);
                double value = dttm.getTime();
                current[cnt++] = value;
                if (cnt >= current.length) {
                    double[] tmp = current;
                    current = new double[current.length * 2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }

        double[] actual = new double[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;


    }


    /**
     * _more_
     *
     * @param stmt _more_
     * @param column _more_
     * @param missing _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static float[] readFloat(Statement stmt, int column, float missing)
            throws Exception {
        float[]   current = new float[10000];
        int       cnt     = 0;
        ResultSet results;
        Iterator  iter = getIterator(stmt);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                float value = results.getFloat(column);
                if (value == missing) {
                    value = Float.NaN;
                }
                current[cnt++] = value;
                if (cnt >= current.length) {
                    float[] tmp = current;
                    current = new float[current.length * 2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        float[] actual = new float[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }


    /**
     * _more_
     *
     * @param stmt _more_
     * @param column _more_
     * @param missing _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static int[] readInt(Statement stmt, int column)
            throws Exception {
        int[]   current = new int[10000];
        int       cnt     = 0;
        ResultSet results;
        Iterator  iter = getIterator(stmt);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                int value = results.getInt(column);
                //                if (value == missing) {
                //                    value = Integer.NaN;
                //                }
                current[cnt++] = value;
                if (cnt >= current.length) {
                    int[] tmp = current;
                    current = new int[current.length * 2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        int[] actual = new int[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }

    /**
     * _more_
     *
     * @param stmt _more_
     * @param columnName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String[] readString(Statement stmt, String columnName)
            throws Exception {
        return readString(stmt, -1, columnName);
    }



    /**
     * _more_
     *
     * @param stmt _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String[] readString(Statement stmt, int column)
            throws Exception {
        return readString(stmt, column, null);
    }


    /**
     * _more_
     *
     * @param stmt _more_
     * @param column _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
     private static String[] readString(Statement stmt, int column,
                                       String name)
            throws Exception {
        String[]  current = new String[10000];
        int       cnt     = 0;
        ResultSet results;
        Iterator  iter = getIterator(stmt);
        while ((results = iter.next()) != null) {
            if (name != null) {
                column = results.findColumn(name);
                name   = null;
            }
            while (results.next()) {
                current[cnt++] = results.getString(column);
                if (cnt >= current.length) {
                    String[] tmp = current;
                    current = new String[current.length * 2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        String[] actual = new String[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }


 

    /**
     * _more_
     *
     * @param stmt _more_
     * @param column _more_
     * @param missing _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static double[] readDouble(Statement stmt, int column,
                                      double missing)
            throws Exception {
        double[]  current = new double[10000];
        int       cnt     = 0;
        ResultSet results;
        Iterator  iter = getIterator(stmt);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                double value = results.getDouble(column);
                if (value == missing) {
                    value = Double.NaN;
                }
                current[cnt++] = value;
                if (cnt >= current.length) {
                    double[] tmp = current;
                    current = new double[current.length * 2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        double[] actual = new double[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }

    /**
     * _more_
     *
     * @param stmt _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Date[] readDate(Statement stmt, int column)
            throws Exception {
        Date[]    current = new Date[10000];
        int       cnt     = 0;
        ResultSet results;
        Iterator  iter = getIterator(stmt);
        while ((results = iter.next()) != null) {
            while (results.next()) {
                Date value = results.getDate(column);
                current[cnt++] = value;
                if (cnt >= current.length) {
                    Date[] tmp = current;
                    current = new Date[current.length * 2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                }
            }
        }
        Date[] actual = new Date[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }


    /**
     * _more_
     *
     * @param stmt _more_
     *
     * @return _more_
     */
    public static Iterator getIterator(Statement stmt) {
        return new Iterator(stmt);
    }


    /**
     * Class Iterator _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class Iterator {

        /** _more_          */
        Statement stmt;

        /** _more_          */
        int cnt = 0;

        /**
         * _more_
         *
         * @param stmt _more_
         */
        public Iterator(Statement stmt) {
            this.stmt = stmt;
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws SQLException _more_
         */
        public ResultSet next() throws SQLException {
            if (cnt != 0) {
                stmt.getMoreResults();
            }
            cnt++;
            return stmt.getResultSet();
        }
    }



}

