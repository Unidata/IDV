/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for Atmospheric Research
 * Copyright 2010- Jeff McWhirter
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
 * 
 */

package ucar.unidata.sql;


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;



import java.io.File;

import java.sql.*;



import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;



import java.util.regex.*;


/**
 * Class SqlUtil provides a set of utility functions for sql databases
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class SqlUtil {

    /** _more_ */
    public static boolean debug = false;

    /** A calendar to use */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);

    /** The formatter to use */
    private static SimpleDateFormat sdf;



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
     * @param s _more_
     *
     * @return _more_
     */
    public static String group(String s) {
        return "(" + s + ")";
    }

    /**
     * _more_
     *
     * @param columns _more_
     *
     * @return _more_
     */
    public static String groupBy(String columns) {
        return " GROUP BY " + columns;
    }


    /**
     * _more_
     *
     * @param toks _more_
     *
     * @return _more_
     */
    public static String makeAnd(List toks) {
        return StringUtil.join(" AND ", toks);
    }

    /**
     * _more_
     *
     * @param clause1 _more_
     * @param clause2 _more_
     *
     * @return _more_
     */
    public static String makeAnd(String clause1, String clause2) {
        return clause1 + " AND " + clause2;
    }

    /**
     * _more_
     *
     * @param clause1 _more_
     * @param clause2 _more_
     * @param clause3 _more_
     *
     * @return _more_
     */
    public static String makeAnd(String clause1, String clause2,
                                 String clause3) {
        return makeAnd(clause1, clause2) + " AND " + clause3;
    }

    /**
     * _more_
     *
     * @param toks _more_
     *
     * @return _more_
     */
    public static String makeOr(List toks) {
        return StringUtil.join(" OR ", toks);
    }


    /**
     * _more_
     *
     * @param toks _more_
     *
     * @return _more_
     */
    public static String makeWhere(List toks) {
        if (toks.size() > 0) {
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
        if (sdf == null) {
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(DateUtil.TIMEZONE_GMT);
        }
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
    public static String getDateString(String dttm)
            throws java.text.ParseException {
        Date date = DateUtil.parse(dttm);
        return SqlUtil.format(date);

    }


    /**
     * _more_
     *
     * @param cnt _more_
     *
     * @return _more_
     */
    public static String getQuestionMarks(int cnt) {
        String s = "";
        for (int i = 0; i < cnt; i++) {
            if (i > 0) {
                s = s + ",";
            }
            s = s + "?";
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



    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String comma(String[] s) {
        return StringUtil.join(",", s);
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String commaNoDot(String[] s) {
        List l = new ArrayList();
        for (int i = 0; i < s.length; i++) {
            l.add(unDot(s[i]));
        }
        return StringUtil.join(",", l);
    }


    /**
     * _more_
     *
     * @param col _more_
     *
     * @return _more_
     */
    public static String unDot(String col) {
        int idx = col.indexOf(".");
        if (idx >= 0) {
            col = col.substring(idx + 1);
        }
        return col;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String comma(List s) {
        return StringUtil.join(",", s);
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
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     * @param s5 _more_
     *
     * @return _more_
     */
    public static String comma(Object s1, Object s2, Object s3, Object s4,
                               Object s5) {
        return s1.toString() + "," + s2.toString() + "," + s3.toString()
               + "," + s4.toString() + "," + s5.toString();
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     * @param s5 _more_
     * @param s6 _more_
     *
     * @return _more_
     */
    public static String comma(Object s1, Object s2, Object s3, Object s4,
                               Object s5, Object s6) {
        return comma(s1, s2, s3, s4, s5) + "," + s6;
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     * @param s5 _more_
     * @param s6 _more_
     * @param s7 _more_
     *
     * @return _more_
     */
    public static String comma(Object s1, Object s2, Object s3, Object s4,
                               Object s5, Object s6, Object s7) {
        return comma(s1, s2, s3, s4, s5, s6) + "," + s7;
    }

    /**
     * _more_
     *
     * @param s1 _more_
     * @param s2 _more_
     * @param s3 _more_
     * @param s4 _more_
     * @param s5 _more_
     * @param s6 _more_
     * @param s7 _more_
     * @param s8 _more_
     *
     * @return _more_
     */
    public static String comma(Object s1, Object s2, Object s3, Object s4,
                               Object s5, Object s6, Object s7, Object s8) {
        return comma(s1, s2, s3, s4, s5, s6, s7) + "," + s8;
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param values _more_
     * @param quoteThem _more_
     *
     * @return _more_
     */
    public static String makeOrSplit(String column, String values,
                                     boolean quoteThem) {
        List toks    = StringUtil.split(values, ",", true, true);

        List nots    = new ArrayList();
        List notNots = new ArrayList();
        for (int i = 0; i < toks.size(); i++) {
            String expr = ((String) toks.get(i)).trim();
            if (expr.startsWith("!")) {
                nots.add(expr);
            } else {
                notNots.add(expr);
            }
        }


        StringBuffer sb    = new StringBuffer();
        StringBuffer notSb = new StringBuffer();

        for (int i = 0; i < nots.size(); i++) {
            if (i > 0) {
                notSb.append(" AND ");
            }
            String value = nots.get(i).toString();
            notSb.append(expr(column, value, quoteThem));
        }



        for (int i = 0; i < notNots.size(); i++) {
            if (i > 0) {
                sb.append(" OR ");
            }
            String value = notNots.get(i).toString();
            sb.append(expr(column, value, quoteThem));
        }

        if ((nots.size() > 0) && (notNots.size() > 0)) {
            return group(notSb.toString()) + " AND " + group(sb.toString());
        } else if (nots.size() > 0) {
            return group(notSb.toString());
        } else if (notNots.size() > 0) {
            return group(sb.toString());
        }
        return "";
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String validName(String name) {
        //TODO: Check if the given name is a valid table column name
        return name;
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String count(String name) {
        return " count(" + validName(name) + ")";
    }


    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String distinct(String name) {
        return " distinct " + validName(name);
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String max(String name) {
        return " max(" + validName(name) + ")";
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    public static String min(String name) {
        return " min(" + validName(name) + ")";
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
     * @param table _more_
     * @param names _more_
     * @param values _more_
     *
     * @return _more_
     */
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


    /**
     * _more_
     *
     * @param table _more_
     * @param names _more_
     *
     * @return _more_
     */
    public static String makeInsert(String table, String[] names) {
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO ");
        sb.append(table);
        sb.append(" (");
        sb.append(comma(names));
        sb.append(" ) values ( ");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(" ? ");
        }
        sb.append(" )");
        return sb.toString();
    }





    /**
     * _more_
     *
     * @param table _more_
     * @param where _more_
     *
     * @return _more_
     */
    public static String makeDelete(String table, String where) {
        StringBuffer sb = new StringBuffer();
        sb.append("DELETE FROM   ");
        sb.append(table);
        sb.append(" WHERE ");
        sb.append(where);
        return sb.toString();
    }



    /**
     * _more_
     *
     * @param table _more_
     * @param colId _more_
     * @param id _more_
     *
     * @return _more_
     */
    public static String makeDelete(String table, String colId, String id) {
        StringBuffer sb = new StringBuffer();
        sb.append("DELETE FROM   ");
        sb.append(table);
        sb.append(" WHERE ");
        sb.append(colId + "=" + id);
        return sb.toString();
    }

    /**
     * _more_
     *
     *
     * @param connection _more_
     * @param table _more_
     * @param colId _more_
     * @param id _more_
     * @param names _more_
     * @param values _more_
     *
     *
     * @throws Exception _more_
     */
    public static void update(Connection connection, String table,
                              String colId, String id, String[] names,
                              Object[] values)
            throws Exception {
        String            query = makeUpdate(table, colId, names);
        PreparedStatement stmt  = connection.prepareStatement(query);
        for (int i = 0; i < values.length; i++) {
            SqlUtil.setValue(stmt, values[i], i + 1);
        }
        stmt.setString(values.length + 1, id);
        stmt.execute();
        close(stmt);
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param table _more_
     * @param clause _more_
     * @param names _more_
     * @param values _more_
     *
     * @throws Exception _more_
     */
    public static void update(Connection connection, String table,
                              Clause clause, String[] names, Object[] values)
            throws Exception {
        String            query  = makeUpdate(table, clause, names);
        PreparedStatement stmt   = connection.prepareStatement(query);
        int               colCnt = 1;
        for (int i = 0; i < values.length; i++) {
            SqlUtil.setValue(stmt, values[i], colCnt);
            colCnt++;
        }
        clause.setValue(stmt, colCnt);

        stmt.execute();
        close(stmt);
    }



    /**
     * _more_
     *
     * @param stmt _more_
     * @param value _more_
     * @param col _more_
     *
     * @throws Exception _more_
     */
    public static void setValue(PreparedStatement stmt, Object value, int col)
            throws Exception {
        if (value instanceof String) {
            stmt.setString(col, value.toString());
        } else if (value instanceof Double) {
            stmt.setDouble(col, ((Double) value).doubleValue());
        } else if (value instanceof Integer) {
            stmt.setInt(col, ((Integer) value).intValue());
        } else if (value instanceof Date) {
            Date dttm = (Date) value;
            stmt.setTimestamp(col, new java.sql.Timestamp(dttm.getTime()),
                              calendar);
        } else {
            throw new IllegalArgumentException("Unknown value:" + value);
        }

    }


    /**
     * _more_
     *
     * @param table _more_
     * @param colId _more_
     * @param names _more_
     *
     * @return _more_
     */
    public static String makeUpdate(String table, String colId,
                                    String[] names) {
        StringBuffer sb = new StringBuffer();
        sb.append("UPDATE  ");
        sb.append(table);
        sb.append(" SET ");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(" " + unDot(names[i]) + "=?" + " ");
        }
        sb.append(" WHERE ");
        sb.append(colId + " = ?");
        return sb.toString();
    }



    /**
     * _more_
     *
     * @param table _more_
     * @param clause _more_
     * @param names _more_
     *
     * @return _more_
     */
    public static String makeUpdate(String table, Clause clause,
                                    String[] names) {
        StringBuffer sb = new StringBuffer();
        sb.append("UPDATE  ");
        sb.append(table);
        sb.append(" SET ");
        for (int i = 0; i < names.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(" " + unDot(names[i]) + "=?" + " ");
        }
        sb.append(" WHERE ");
        clause.addClause(sb);
        return sb.toString();
    }






    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String eq(String name, String value) {
        return " " + validName(name) + "=" + value + " ";
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String ge(String name, double value) {
        return " " + validName(name) + ">=" + value + " ";
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String ge(String name, int value) {
        return " " + validName(name) + ">=" + value + " ";
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String ge(String name, String value) {
        return " " + validName(name) + ">=" + value + " ";
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String le(String name, double value) {
        return " " + validName(name) + "<=" + value + " ";
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String le(String name, int value) {
        return " " + validName(name) + "<=" + value + " ";
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String le(String name, Date value) {
        return " " + validName(name) + "<=" + quote(format(value)) + " ";
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String ge(String name, Date value) {
        return " " + validName(name) + ">=" + quote(format(value)) + " ";
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String eq(String name, Date value) {
        return " " + validName(name) + "=" + quote(format(value)) + " ";
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static String neq(String name, Date value) {
        return " " + validName(name) + "<>" + quote(format(value)) + " ";
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param table _more_
     * @param where _more_
     *
     * @return _more_
     */
    public static String makeSelect(String what, String table, String where) {
        return makeSelect(what, Misc.newList(table), where);
    }

    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     *
     * @return _more_
     */
    public static String makeSelect(String what, List<String> tables) {
        return makeSelect(what, tables, "");
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param where _more_
     *
     * @return _more_
     */
    public static String makeSelect(String what, List<String> tables,
                                    String where) {
        return makeSelect(what, tables, where, "");
    }



    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param where _more_
     * @param suffixSql _more_
     *
     * @return _more_
     */
    public static String makeSelect(String what, List tables, String where,
                                    String suffixSql) {
        return makeSelect(what, tables, where, null, suffixSql);
    }

    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param where _more_
     * @param sqlBetweenFromAndWhere _more_
     * @param suffixSql _more_
     *
     * @return _more_
     */
    public static String makeSelect(String what, List tables, String where,
                                    String sqlBetweenFromAndWhere,
                                    String suffixSql) {




        if (sqlBetweenFromAndWhere == null) {
            sqlBetweenFromAndWhere = "";
        } else {
            sqlBetweenFromAndWhere = " " + sqlBetweenFromAndWhere;
        }

        if (suffixSql == null) {
            suffixSql = "";
        } else {
            suffixSql = " " + suffixSql;
        }

        String    tableClause = "";
        Hashtable seen        = new Hashtable();
        for (int i = 0; i < tables.size(); i++) {
            String table = (String) tables.get(i);
            if (seen.get(table) != null) {
                continue;
            }
            seen.put(table, table);
            if (tableClause.length() > 0) {
                tableClause += ",";
            }
            tableClause += table;
        }
        String sql = "SELECT " + what + " FROM " + tableClause
                     + sqlBetweenFromAndWhere + ((where.trim().length() > 0)
                ? " \nWHERE " + where
                : "") + suffixSql;

        return sql;
    }




    /**
     * _more_
     *
     * @param col _more_
     * @param value _more_
     * @param quoteThem _more_
     *
     * @return _more_
     */
    public static String expr(String col, String value, boolean quoteThem) {
        boolean doNot = false;
        if (value.startsWith("!")) {
            value = value.substring(1);
            doNot = true;
        }
        if (value.startsWith("%") || value.endsWith("%")) {
            return " " + col + (doNot
                                ? " NOT "
                                : "") + " LIKE " + quote(value) + " ";
        }
        return " " + col + (doNot
                            ? " <> "
                            : "=") + (quoteThem
                                      ? quote(value)
                                      : value) + " ";
    }

    /**
     * _more_
     *
     * @param sql _more_
     * @param statement _more_
     * @param ignoreErrors _more_
     *
     * @throws Exception _more_
     */
    public static void loadSql(String sql, Statement statement,
                               boolean ignoreErrors)
            throws Exception {
        loadSql(sql, statement, ignoreErrors, false);
    }

    /**
     * _more_
     *
     * @param sql _more_
     * @param statement _more_
     * @param ignoreErrors _more_
     * @param printStatus _more_
     *
     * @throws Exception _more_
     */
    public static void loadSql(String sql, Statement statement,
                               boolean ignoreErrors, boolean printStatus)
            throws Exception {

        loadSql(sql, statement, ignoreErrors, printStatus,
                new ArrayList<SqlError>());
    }


    /** _more_ */
    public static boolean showLoadingSql = false;

    /**
     * _more_
     *
     * @param sql _more_
     * @param statement _more_
     * @param ignoreErrors _more_
     * @param printStatus _more_
     * @param errors _more_
     *
     * @throws Exception _more_
     */
    public static void loadSql(String sql, Statement statement,
                               boolean ignoreErrors, boolean printStatus,
                               List<SqlError> errors)
            throws Exception {

        int cnt = 0;
        for (String command : parseSql(sql)) {
            if (printStatus) {
                cnt++;
                if (cnt % 100 == 0) {
                    System.err.print(".");
                }
                if (cnt % 1000 == 0) {
                    System.err.println("\n" + cnt);
                }
            }
            try {
                command = command.trim();
                if (command.length() > 0) {
                    statement.execute(command);
                    if (showLoadingSql) {
                        System.err.println("SqlUtil.loadSql:"
                                           + command.replace("\n", " "));
                    }
                }
            } catch (Exception exc) {
                errors.add(new SqlError(command, exc));
                String msg = exc.toString().toLowerCase();
                if ((msg.indexOf("duplicate") < 0)
                        && (msg.indexOf("already exists") < 0)
                        && (msg.indexOf("can't drop") < 0)
                        && (msg.indexOf("doesn't exist") < 0)
                        && (msg.indexOf("is not a column in table") < 0)) {
                    System.err.println(
                        "\n*********************************************");
                    System.err.println("Bad sql:" + command);
                    System.err.println(exc);
                    System.err.println(
                        "********************************************\n");
                }
                if ( !ignoreErrors) {
                    throw exc;
                }
            }
        }
    }

    /**
     * _more_
     *
     * @param sql _more_
     *
     * @return _more_
     */
    public static List<String> parseSql(String sql) {
        List<String> result = new ArrayList<String>();
        List<String> toks   = (List<String>) StringUtil.split(sql, "\n");
        StringBuffer sb     = new StringBuffer();
        for (String line : toks) {
            String trimLine = line.trim();
            if (trimLine.startsWith("--")) {
                continue;
            }
            sb.append(line);
            sb.append("\n");
            if (trimLine.endsWith(";")) {
                String lineSql = sb.toString().trim();
                //Strip off the ";"
                lineSql = lineSql.substring(0, lineSql.length() - 1);
                result.add(lineSql);
                sb = new StringBuffer();
            }
        }
        if (sb.toString().length() > 0) {
            result.add(sb.toString());
        }
        return result;
    }




    /**
     * _more_
     *
     *
     * @param iter _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static double[] readTime(Iterator iter, int column)
            throws Exception {

        double[]  current = new double[10000];
        int       cnt     = 0;
        ResultSet results;
        while ((results = iter.getNext()) != null) {
            Date   dttm  = results.getDate(column, calendar);
            double value = dttm.getTime();
            current[cnt++] = value;
            if (cnt >= current.length) {
                double[] tmp = current;
                current = new double[current.length * 2];
                System.arraycopy(tmp, 0, current, 0, tmp.length);
            }
        }

        double[] actual = new double[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;


    }


    /**
     * _more_
     *
     *
     * @param iter _more_
     * @param column _more_
     * @param missing _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static float[] readFloat(Iterator iter, int column, float missing)
            throws Exception {
        float[]   current = new float[10000];
        int       cnt     = 0;
        ResultSet results;
        while ((results = iter.getNext()) != null) {
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

        float[] actual = new float[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }



    /**
     * _more_
     *
     * @param iter _more_
     * @param missing _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static List<float[]> readFloats(Iterator iter, float missing)
            throws Exception {
        List<float[]> arrays  = new ArrayList<float[]>();
        int           numCols = -1;
        int           cnt     = 0;
        ResultSet     results;
        while ((results = iter.getNext()) != null) {
            if (numCols == -1) {
                ResultSetMetaData rsmd = results.getMetaData();
                numCols = rsmd.getColumnCount();
                for (int column = 0; column < numCols; column++) {
                    arrays.add(new float[1000]);
                }
            }
            for (int column = 0; column < numCols; column++) {
                float[] current = arrays.get(column);
                float   value   = results.getFloat(column + 1);
                if (value == missing) {
                    value = Float.NaN;
                }
                current[cnt] = value;
                if (cnt + 1 >= current.length) {
                    float[] tmp = current;
                    current = new float[current.length * 2];
                    System.arraycopy(tmp, 0, current, 0, tmp.length);
                    arrays.set(column, current);
                }
            }
            cnt++;
        }


        if (debug) {
            //            System.err.println ("arrays: " + arrays.size() + " cnt=" + cnt);
        }
        for (int column = 0; column < numCols; column++) {
            float[] current = arrays.get(column);
            float[] actual  = new float[cnt];
            System.arraycopy(current, 0, actual, 0, cnt);
            arrays.set(column, actual);
        }
        return arrays;
    }




    /**
     * _more_
     *
     *
     * @param iter _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static int[] readInt(Iterator iter, int column) throws Exception {
        int[]     current = new int[10000];
        int       cnt     = 0;
        ResultSet results;
        while ((results = iter.getNext()) != null) {
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

        int[] actual = new int[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }


    /**
     * _more_
     *
     *
     * @param iter _more_
     * @param columnName _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String[] readString(Iterator iter, String columnName)
            throws Exception {
        return readString(iter, -1, columnName);
    }



    /**
     * _more_
     *
     *
     * @param iter _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String[] readString(Iterator iter) throws Exception {
        return readString(iter, 1);
    }

    /**
     * _more_
     *
     *
     * @param iter _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String[] readString(Iterator iter, int column)
            throws Exception {
        return readString(iter, column, null);
    }


    /**
     * _more_
     *
     *
     * @param iter _more_
     * @param column _more_
     * @param name _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private static String[] readString(Iterator iter, int column, String name)
            throws Exception {
        String[]  current = new String[10000];
        int       cnt     = 0;
        ResultSet results;
        while ((results = iter.getNext()) != null) {
            if (name != null) {
                column = results.findColumn(name);
                name   = null;
            }
            String value = results.getString(column);
            if (value == null) {
                continue;
            }
            current[cnt++] = value;
            if (cnt >= current.length) {
                String[] tmp = current;
                current = new String[current.length * 2];
                System.arraycopy(tmp, 0, current, 0, tmp.length);
            }
        }
        String[] actual = new String[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }



    /**
     * _more_
     *
     *
     *
     *
     * @param iter _more_
     * @param cnt _more_
     *
     * @return _more_
     * @throws Exception _more_
     */
    public static List<String[]> readStringTuples(Iterator iter, int cnt)
            throws Exception {
        List<String[]> values = new ArrayList<String[]>();
        ResultSet      results;
        while ((results = iter.getNext()) != null) {
            String[] tuple = new String[cnt];
            for (int col = 1; col <= cnt; col++) {
                tuple[col - 1] = results.getString(col);
            }
            values.add(tuple);
        }
        return values;
    }




    /**
     * _more_
     *
     *
     * @param iter _more_
     * @param column _more_
     * @param missing _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static double[] readDouble(Iterator iter, int column,
                                      double missing)
            throws Exception {
        double[]  current = new double[10000];
        int       cnt     = 0;
        ResultSet results;
        while ((results = iter.getNext()) != null) {
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

        double[] actual = new double[cnt];
        System.arraycopy(current, 0, actual, 0, cnt);
        return actual;
    }

    /**
     * _more_
     *
     *
     * @param iter _more_
     * @param column _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Date[] readDate(Iterator iter, int column)
            throws Exception {
        Date[]    current = new Date[10000];
        int       cnt     = 0;
        ResultSet results;
        while ((results = iter.getNext()) != null) {
            Date value = results.getDate(column, calendar);
            current[cnt++] = value;
            if (cnt >= current.length) {
                Date[] tmp = current;
                current = new Date[current.length * 2];
                System.arraycopy(tmp, 0, current, 0, tmp.length);
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
     * _more_
     *
     * @param stmt _more_
     * @param offset _more_
     * @param limit _more_
     *
     * @return _more_
     */
    public static Iterator getIterator(Statement stmt, int offset,
                                       int limit) {
        return new Iterator(stmt, offset, limit);
    }


    /**
     * Class Iterator _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public static class Iterator {

        /** _more_ */
        private Statement stmt;

        /** _more_ */
        private int resultSetCnt = 0;

        /** _more_ */
        private ResultSet lastResultSet;

        /** _more_ */
        private boolean shouldCloseStatement = true;

        /** _more_ */
        private int offset = 0;

        /** _more_ */
        private int limit = -1;

        /** _more_ */
        private int cnt = 0;

        /**
         *
         * @param stmt _more_
         */
        public Iterator(Statement stmt) {
            this(stmt, 0, -1);
        }

        /**
         * _more_
         *
         * @param stmt _more_
         * @param offset _more_
         * @param limit _more_
         */
        public Iterator(Statement stmt, int offset, int limit) {
            this.stmt   = stmt;
            this.offset = offset;
            this.limit  = limit;
        }

        /**
         *  Set the ShouldCloseStatement property.
         *
         *  @param value The new value for ShouldCloseStatement
         */
        public void setShouldCloseStatement(boolean value) {
            shouldCloseStatement = value;
        }

        /**
         *  Get the ShouldCloseStatement property.
         *
         *  @return The ShouldCloseStatement
         */
        public boolean getShouldCloseStatement() {
            return shouldCloseStatement;
        }


        /**
         * _more_
         *
         * @return _more_
         *
         * @throws SQLException _more_
         */
        public ResultSet xxxnext() throws SQLException {
            if (stmt == null) {
                return null;
            }
            if (resultSetCnt != 0) {
                stmt.getMoreResults();
            }
            if (lastResultSet != null) {
                lastResultSet.close();
            }
            resultSetCnt++;
            lastResultSet = stmt.getResultSet();
            if (lastResultSet == null) {
                if (shouldCloseStatement) {
                    close(stmt);
                }
                stmt = null;
            }
            return lastResultSet;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public ResultSet getResults() {
            return lastResultSet;
        }


        /**
         * _more_
         *
         * @return _more_
         *
         * @throws SQLException _more_
         */
        public ResultSet getNext() throws SQLException {
            try {
                if (stmt == null) {
                    return null;
                }
                if (lastResultSet == null) {
                    lastResultSet = stmt.getResultSet();
                    if (lastResultSet == null) {
                        return null;
                    }
                    if ( !lastResultSet.next()) {
                        checkClose();
                        return null;
                    }
                    //Run through the offset
                    while (offset-- > 0) {
                        if ( !lastResultSet.next()) {
                            checkClose();
                            return null;
                        }
                    }
                    cnt++;
                    return lastResultSet;
                }

                if (lastResultSet.next()) {
                    cnt++;
                    return lastResultSet;
                }

                if (lastResultSet != null) {
                    lastResultSet.close();
                }
                lastResultSet = stmt.getResultSet();
                if (lastResultSet.next()) {
                    cnt++;
                    return lastResultSet;
                }

            } catch (SQLException exc) {}
            checkClose();
            return null;
        }


        /**
         * _more_
         *
         * @return _more_
         */
        public int getCount() {
            return cnt;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean countOK() {
            return cnt < limit;
        }

        /**
         * _more_
         *
         * @throws SQLException _more_
         */
        private void checkClose() throws SQLException {
            if (shouldCloseStatement) {
                close(stmt);
                lastResultSet = null;
                stmt          = null;
            }
        }

        /**
         * _more_
         *
         * @throws SQLException _more_
         */
        public void close() throws SQLException {
            if (stmt != null) {
                if (shouldCloseStatement) {
                    close(stmt);
                }
                stmt = null;
            }
        }

        /**
         * _more_
         *
         * @param stmt _more_
         *
         * @throws SQLException _more_
         */
        protected void close(Statement stmt) throws SQLException {
            SqlUtil.close(stmt);
        }
    }


    /** _more_ */
    private static ConnectionManager connectionManager;

    /**
     * _more_
     *
     * @param mgr _more_
     */
    public static void setConnectionManager(ConnectionManager mgr) {
        if ((connectionManager != null) && (connectionManager != mgr)) {
            /*            throw new IllegalArgumentException(
                "Already have a connection manager:"
                + connectionManager.getClass().getName());*/
        }
        connectionManager = mgr;
    }

    /**
     * _more_
     *
     * @param stmt _more_
     *
     * @throws SQLException _more_
     */
    public static void close(Statement stmt) throws SQLException {
        if (connectionManager != null) {
            connectionManager.closeStatement(stmt);
        } else {
            stmt.close();
        }
    }


    /**
     * Interface description
     *
     *
     * @author         Enter your name here...
     */
    public static interface ConnectionManager {

        /**
         * _more_
         *
         * @param stmt _more_
         */
        public void closeStatement(Statement stmt);

        /**
         * _more_
         *
         * @param stmt _more_
         */
        public void initSelectStatement(Statement stmt);

    }



    /**
     * _more_
     *
     * @param formArgs _more_
     *
     * @return _more_
     */
    public static Hashtable cleanUpArguments(Hashtable formArgs) {

        Hashtable cleanArgs = new Hashtable();
        for (Enumeration keys = formArgs.keys(); keys.hasMoreElements(); ) {
            String key   = (String) keys.nextElement();
            String value = (String) formArgs.get(key);
            value = cleanUp(value);
            cleanArgs.put(key, value);
        }
        return cleanArgs;
    }



    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public static String cleanUp(String value) {
        //TODO: Atually implement this!!!!
        value = value.replace("'", "");
        return value;
    }


    /**
     * _more_
     *
     * @param value _more_
     *
     * @return _more_
     */
    public static String cleanName(String value) {
        value = value.replaceAll(" ", "_");
        value = value.replaceAll("\\.", "_");
        return value;
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param suffixSql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static PreparedStatement getSelectStatement(Connection connection,
            String what, List tables, Clause clause, String suffixSql)
            throws Exception {
        return getSelectStatement(connection, what, tables, clause, null,
                                  suffixSql);
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param sqlBetweenFromAndWhere _more_
     * @param suffixSql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static PreparedStatement getSelectStatement(Connection connection,
            String what, List tables, Clause clause,
            String sqlBetweenFromAndWhere, String suffixSql)
            throws Exception {
        PreparedStatement statement =
            connection.prepareStatement(getSelectStatement(what, tables,
                clause, sqlBetweenFromAndWhere, suffixSql));
        if (connectionManager != null) {
            connectionManager.initSelectStatement(statement);
        }
        return statement;
    }


    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param suffixSql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getSelectStatement(String what, List tables,
                                            Clause clause, String suffixSql)
            throws Exception {
        return getSelectStatement(what, tables, clause, null, suffixSql);
    }

    /**
     * _more_
     *
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param sqlBetweenFromAndWhere _more_
     * @param suffixSql _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static String getSelectStatement(String what, List tables,
                                            Clause clause,
                                            String sqlBetweenFromAndWhere,
                                            String suffixSql)
            throws Exception {

        StringBuffer sb = new StringBuffer();
        if (clause != null) {
            clause.addClause(sb);
        }

        String sql = makeSelect(what, tables, sb.toString(),
                                sqlBetweenFromAndWhere, suffixSql);
        if (debug) {
            System.err.println("sql: " + sql);
        }
        return sql;
    }



    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Statement select(Connection connection, String what,
                                   List tables, Clause clause, String extra)
            throws Exception {
        return select(connection, what, tables, clause, extra, -1);
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param extra _more_
     * @param max _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Statement select(Connection connection, String what,
                                   List tables, Clause clause, String extra,
                                   int max)
            throws Exception {

        return select(connection, what, tables, clause, extra, max, 0);
    }

    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param suffixSql _more_
     * @param max _more_
     * @param timeout _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Statement select(Connection connection, String what,
                                   List tables, Clause clause,
                                   String suffixSql, int max, int timeout)
            throws Exception {
        return select(connection, what, tables, clause, null, suffixSql, max,
                      timeout);
    }

    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     * @param sqlBetweenFromAndWhere _more_
     * @param suffixSql _more_
     * @param max _more_
     * @param timeout _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Statement select(Connection connection, String what,
                                   List tables, Clause clause,
                                   String sqlBetweenFromAndWhere,
                                   String suffixSql, int max, int timeout)
            throws Exception {
        PreparedStatement stmt = getSelectStatement(connection, what, tables,
                                     clause, sqlBetweenFromAndWhere,
                                     suffixSql);
        if (max > 0) {
            stmt.setMaxRows(max);
        }
        if (clause != null) {

            clause.setValue(stmt, 1);
        }

        if (timeout > 0) {
            stmt.setQueryTimeout(timeout);
        }
        stmt.execute();
        return stmt;
    }



    /**
     * _more_
     *
     * @param connection _more_
     * @param table _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static PreparedStatement getDeleteStatement(Connection connection,
            String table, Clause clause)
            throws Exception {
        StringBuffer sb = new StringBuffer();
        clause.addClause(sb);
        String query = makeDelete(table, sb.toString());
        return connection.prepareStatement(query);
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param table _more_
     * @param clause _more_
     *
     * @throws Exception _more_
     */
    public static void delete(Connection connection, String table,
                              Clause clause)
            throws Exception {
        PreparedStatement stmt = getDeleteStatement(connection, table,
                                     clause);
        clause.setValue(stmt, 1);
        stmt.execute();
        close(stmt);
    }

    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clause _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Statement select(Connection connection, String what,
                                   List tables, Clause clause)
            throws Exception {
        return select(connection, what, tables, clause, "");
    }


    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clauses _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Statement select(Connection connection, String what,
                                   List tables, Clause[] clauses)
            throws Exception {
        return select(connection, what, tables, ((clauses == null)
                ? null
                : Clause.and(clauses)));
    }

    /**
     * _more_
     *
     * @param connection _more_
     * @param what _more_
     * @param tables _more_
     * @param clauses _more_
     * @param extra _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public static Statement select(Connection connection, String what,
                                   List tables, Clause[] clauses,
                                   String extra)
            throws Exception {
        return select(connection, what, tables, ((clauses == null)
                ? null
                : Clause.and(clauses)), extra);
    }



    /**
     * _more_
     *
     * @param connection _more_
     * @param table _more_
     * @param clause _more_
     *
     * @throws Exception _more_
     */
    public static void update(Connection connection, String table,
                              Clause clause)
            throws Exception {
        /*
        StringBuffer sb = new StringBuffer();
       clause.addClause(sb);
        String query = makeDelete(table, sb.toString());
        return connection.prepareStatement(query);*/


    }



    /**
     * Class description
     *
     *
     * @version        $version$, Mon, Aug 2, '10
     * @author         Enter your name here...
     */
    public static final class SqlError {

        /** _more_ */
        private String sql;

        /** _more_ */
        private Exception exception;


        /**
         * _more_
         *
         * @param sql _more_
         * @param exc _more_
         */
        public SqlError(String sql, Exception exc) {
            this.sql       = sql;
            this.exception = exc;
        }

        /**
         *  Get the Sql property.
         *
         *  @return The Sql
         */
        public String getSql() {
            return this.sql;
        }


        /**
         *  Get the Exception property.
         *
         *  @return The Exception
         */
        public Exception getException() {
            return this.exception;
        }


    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String wildCardBefore(String s) {
        return "%" + s;
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String wildCardAfter(String s) {
        return s + "%";
    }

    /**
     * _more_
     *
     * @param s _more_
     *
     * @return _more_
     */
    public static String wildCardBoth(String s) {
        return "%" + s + "%";
    }


}
