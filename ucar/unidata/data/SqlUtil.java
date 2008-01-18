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
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.List;
import java.util.Hashtable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;


import java.text.SimpleDateFormat;



import java.util.regex.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class SqlUtil {


    public static final  GregorianCalendar calendar = new GregorianCalendar(DateUtil.TIMEZONE_GMT);

    /** _more_          */
    private static final SimpleDateFormat sdf =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");



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

    public  static String group(String s) {
        return  "("+s +")";
    }

    public  static String makeAnd(List toks) {
        return StringUtil.join(" AND ", toks);
    }

    public  static String makeOr(List toks) {
        return StringUtil.join(" OR ", toks);
    }


    public static String makeWhere(List toks) {
        if(toks.size()>0) {
            return " WHERE " + StringUtil.join(" AND ", toks);
        }
        return " ";
    }


    /**
     * _more_
     *
     * @param d _more_
     *
     * @return _more_
     */
    public static String format(Date d) {
        sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        return sdf.format(d);
    }


    /**
     * _more_
     *
     * @param dttm _more_
     *
     * @return _more_
     *
     * @throws java.text.ParseException _more_
     */
    public static  String getDateString(String dttm)
            throws java.text.ParseException {
        Date date = DateUtil.parse(dttm);
        return SqlUtil.format(date);

    }


    public static String getQuestionMarks(int cnt) {
        String s = "";
        for(int i=0;i<cnt;i++) {
            if(i>0) s = s+",";
            s = s+"?";
        }
        return s;
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



    public static String comma(String[]s) {
        return StringUtil.join(",",s);
    }

    public static String comma(List s) {
        return StringUtil.join(",",s);
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

    public static String comma(Object s1, Object s2, Object s3, Object s4,Object s5) {
        return s1.toString() + "," + s2.toString() + "," + s3.toString()
               + "," + s4.toString()+","+s5.toString();
    }

    public static String comma(Object s1, Object s2, Object s3, Object s4,Object s5, Object s6 ) {
        return comma(s1,s2,s3,s4,s5)+","+s6;
    }

    public static String comma(Object s1, Object s2, Object s3, Object s4,Object s5, Object s6, Object s7 ) {
        return comma(s1,s2,s3,s4,s5,s6)+","+s7;
    }

    public static String comma(Object s1, Object s2, Object s3, Object s4,Object s5, Object s6, Object s7 , Object s8) {
        return comma(s1,s2,s3,s4,s5,s6,s7)+","+s8;
    }

    public static String makeOrSplit(String column, String values, boolean quoteThem) {
        List toks = StringUtil.split(values,",",true,true);
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<toks.size();i++) {
            if(i>0) sb.append (" OR ");
            String value = toks.get(i).toString();
            sb.append(expr(column,value,quoteThem));
        }
        return sb.toString();
    }


    public static String validName(String name) {
        //TODO: Check if the given name is a valid table column name
        return name;
    }

    public static String count(String name) {
        return " count(" + validName(name)+")";
    }


    public static String distinct(String name) {
        return " distinct " + validName(name);
    }
    public static String max(String name) {
        return " max(" + validName(name)+")";
    }
    public static String min(String name) {
        return " min(" + validName(name)+")";
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




    public static String makeInsert(String table, String names,
                                    String values) {
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO ");
        sb.append(table);
        sb.append(" (");
        sb.append(names);
        sb.append(" )");
        sb.append(" VALUES (");
        sb.append(values);
        sb.append(")");
        return sb.toString();
    }






    public static String makeDelete(String table, String where) {
        StringBuffer sb = new StringBuffer();
        sb.append("DELETE FROM   ");
        sb.append(table);
        sb.append (" WHERE ");
        sb.append(where);
        return sb.toString();
    }



    public static String makeDelete(String table, String colId, String id) {
        StringBuffer sb = new StringBuffer();
        sb.append("DELETE FROM   ");
        sb.append(table);
        sb.append (" WHERE ");
        sb.append(colId +"=" + id);
        return sb.toString();
    }

    public static String makeUpdate(String table, String colId, String id, String []names,
                                    String[]values) {
        StringBuffer sb = new StringBuffer();
        sb.append("UPDATE  ");
        sb.append(table);
        sb.append(" SET ");
        for(int i=0;i<names.length;i++) {
            if(i>0)
                sb.append(",");
            sb.append(" " + names[i] +"=" + values[i] +" " );
        }
        sb.append(" WHERE ");
        sb.append(colId +" = " + id);
        return sb.toString();
    }


    public static String makeUpdate(String table, String colId,  String []names) {
        StringBuffer sb = new StringBuffer();
        sb.append("UPDATE  ");
        sb.append(table);
        sb.append(" SET ");
        for(int i=0;i<names.length;i++) {
            if(i>0)
                sb.append(",");
            sb.append(" " + names[i] +"=?" +" " );
        }
        sb.append(" WHERE ");
        sb.append(colId +" = ?");
        return sb.toString();
    }

    public static String makeUpdate(String table, String colId,  List<String> names) {
        StringBuffer sb = new StringBuffer();
        sb.append("UPDATE  ");
        sb.append(table);
        sb.append(" SET ");
        for(int i=0;i<names.size();i++) {
            if(i>0)
                sb.append(",");
            sb.append(" " + names.get(i) +"=?" +" " );
        }
        sb.append(" WHERE ");
        sb.append(colId +" = ?");
        return sb.toString();
    }




    public static String like(String name, String value) {
        return " " +validName(name) +" LIKE " + quote(value)+" ";
    }
    public static String notLike(String name, String value) {
        return " NOT " +validName(name) +" LIKE " + quote(value)+" ";
    }

    public static String neq(String name, String value) {
        return " " +validName(name) +"<>" + value +" ";
    }

    public static String eq(String name, double value) {
        return " " +validName(name) +"=" + value +" ";
    }

    public static String eq(String name, int value) {
        return " " +validName(name) +"=" + value +" ";
    }

    public static String eq(String name, String value) {
        return " " +validName(name) +"=" + value +" ";
    }

    public static String ge(String name, double value) {
        return " " +validName(name) +">=" + value +" ";
    }


    public static String ge(String name, int value) {
        return " " +validName(name) +">=" + value +" ";
    }


    public static String ge(String name, String value) {
        return " " +validName(name) +">=" + value +" ";
    }


    public static String le(String name, double value) {
        return " " +validName(name) +"<=" + value +" ";
    }

    public static String le(String name, int value) {
        return " " +validName(name) +"<=" + value +" ";
    }


    public static String le(String name, Date value) {
        return " " + validName(name) + "<=" + quote(format(value)) +" ";
    }

    public static String ge(String name, Date value) {
        return " " + validName(name) + ">=" + quote(format(value)) +" ";
    }

    public static String eq(String name, Date value) {
        return " " + validName(name) + "=" + quote(format(value)) +" ";
    }

    public static String neq(String name, Date value) {
        return " " + validName(name) + "<>" + quote(format(value)) +" ";
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param where _more_
     *
     * @return _more_
     */
    public static String makeSelect(String what, List<String> tables) {
        return makeSelect(what, tables, "");
    }


    public static String makeSelect(String what, List<String> tables, String where) {
        return makeSelect(what, tables, where, "");
    }



    public static String makeSelect(String what, List tables, String where, String extra) {
        String tableClause = "";
        Hashtable seen = new Hashtable();
        for(int i=0;i<tables.size();i++) {
            String table = (String)tables.get(i);
            if(seen.get(table)!=null) continue;
            seen.put(table,table);
            if(tableClause.length()>0)
                tableClause += ",";
            tableClause += table;
        }
        return "SELECT " + what + " FROM " + tableClause + (where.trim().length()>0?" WHERE "  + where:"") +" " +extra;
    }




    public static String expr(String col, String value, boolean quoteThem) {
        boolean doNot = false;
        if(value.startsWith("!")) {
            value = value.substring(1);
            doNot = true;
        }
        if(value.startsWith("%") || value.endsWith("%")) {
            return " " + col +(doNot?" NOT ":"") + " LIKE " + quote(value) + " " ;
        }
        return " " + col +(doNot?" <> ":"=")  + (quoteThem?quote(value):value) + " " ;
    }

    public static void loadSql(String sql, Statement statement, boolean ignoreErrors) 
            throws Exception {
        for(String command: parseSql(sql)) {
            try {
                statement.execute(command);
                //                System.err.println ("OK:" + command);
            } catch(Exception exc) {
                //                System.err.println ("bad sql:" + command+ " " + exc);
                if(!ignoreErrors) {
                    System.err.println ("bad query:" + command);
                    throw exc;
                }
            }
        }
    }

    public static List<String> parseSql(String sql) {
        List<String> result = new ArrayList<String>();
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
                result.add(lineSql);
                sb = new StringBuffer();
            }
        }
        return result;
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
                Date   dttm  = results.getDate(column,calendar);
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
                Date value = results.getDate(column,calendar);
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

    public static Hashtable cleanUpArguments(Hashtable formArgs) {

        Hashtable cleanArgs = new Hashtable();
        for(Enumeration keys = formArgs.keys();keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            String value = (String)formArgs.get(key);
            value = cleanUp(value);
            cleanArgs.put(key,value);
        }
        return cleanArgs;
    }



    public static String cleanUp(String value) {
        //TODO: Atually implement this!!!!
        value = value.replace("'","");
        return value;
    }


}

