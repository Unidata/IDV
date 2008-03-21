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
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Clause {

    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);


    public static final String EXPR_EQUALS = "=";
    public static final String EXPR_GE = ">=";
    public static final String EXPR_LE = "<=";
    public static final String EXPR_NOTEQUALS = "<>";
    public static final String EXPR_LIKE = "LIKE";
    public static final String EXPR_NOTLIKE = "LIKE";
    public static final String EXPR_ISNULL = "is null";    
    public static final String EXPR_OR = "OR";
    public static final String EXPR_AND = "AND";


    private String expr =null;
    private String column;
    private Object value;
    private Clause[]subClauses;


    public Clause() {
    }

    public Clause(String expr, Clause[]subClauses) {
        this.expr = expr;
        this.subClauses = subClauses;
    }


    public Clause(String column, String expr, Object value) {
        this.column = column;
        this.expr = expr;
        this.value = value;
    }

    public static Clause or(Clause clause1, Clause clause2) {
        return new Clause(EXPR_OR, new Clause[]{clause1, clause2});
    }

    public static Clause and(Clause clause1, Clause clause2) {
        return new Clause(EXPR_AND, new Clause[]{clause1, clause2});
    }

    public static Clause or(Clause[]clauses) {
        return new Clause(EXPR_OR, clauses);
    }

    public static Clause and(Clause[]clauses) {
        return new Clause(EXPR_AND, clauses);
    }

    public static Clause and(List<Clause> clauses) {
        return new Clause(EXPR_AND, toArray(clauses));
    }


    public static Clause or(List<Clause> clauses) {
        return new Clause(EXPR_OR, toArray(clauses));
    }


    public static Clause eq(String column, Object value) {
        return new Clause(column, EXPR_EQUALS, value);
    }

    public static Clause ge(String column, Object value) {
        return new Clause(column, EXPR_GE, value);
    }

    public static Clause le(String column, Object value) {
        return new Clause(column, EXPR_LE, value);
    }


    public static Clause isNull(String column) {
        return new Clause(column, EXPR_ISNULL, null);
    }

    public static Clause neq(String column, Object value) {
        return new Clause(column, EXPR_NOTEQUALS, value);
    }


    public static Clause like(String column, Object value) {
        return new Clause(column, EXPR_LIKE, value);
    }

    public static Clause notLike(String column, Object value) {
        return new Clause(column, EXPR_NOTLIKE, value);
    }

    public static Clause[] toArray(List<Clause> clauses) {
        Clause[]array = new Clause[clauses.size()];
        for(int i=0;i<clauses.size();i++) {
            array[i] = clauses.get(i);
        }
        return array;
    }

    public  boolean isColumnFromTable(String table) {
        return column.startsWith(table+".");
    }

    public  static boolean isColumnFromTable(List<Clause> clauses, String table) {
        for(Clause clause: clauses) {
            if(clause.isColumnFromTable(table)) return true;
        }
        return false;
    }

    public static Clause makeOrSplit(String column, String values) {
        List toks    = StringUtil.split(values, ",", true, true);
        List nots    = new ArrayList();
        List notNots = new ArrayList();
        for (int i = 0; i < toks.size(); i++) {
            String expr = ((String) toks.get(i)).trim();
            if (expr.startsWith("!")) {
                nots.add(expr.substring(1));
            } else {
                notNots.add(expr);
            }
        }


        List<Clause> clauses = new ArrayList<Clause>();
        List<Clause> notClauses = new ArrayList<Clause>();


        for (int i = 0; i < nots.size(); i++) {
            String value = nots.get(i).toString();
            notClauses.add(Clause.neq(column, value));
        }


        for (int i = 0; i < notNots.size(); i++) {
            String value = notNots.get(i).toString();
            clauses.add(Clause.eq(column, value));
        }

        if ((nots.size() > 0) && (notNots.size() > 0)) {
            return Clause.and(Clause.and(toArray(notClauses)), Clause.or(toArray(clauses)));
        } else if (nots.size() > 0) {
            return Clause.and(toArray(notClauses));
        } else if (notNots.size() > 0) {
            return Clause.or(toArray(clauses));
        }
        return new Clause();
    }



    public  boolean isColumn(String col) {
        return column.equals(col);
    }

    public  static boolean isColumn(List<Clause> clauses, String col) {
        for(Clause clause: clauses) {
            if(clause.isColumn(col)) return true;
        }
        return false;
    }



    public StringBuffer addClause(StringBuffer sb) {
        if(expr == null) return sb;
        if(subClauses!=null) {
            List toks  = new ArrayList();
            for(int i=0;i<subClauses.length;i++) {
                StringBuffer buff = new StringBuffer();
                subClauses[i].addClause(buff);
                toks.add(buff.toString());
            }
            sb.append(StringUtil.join(" " + expr +  " ", toks));
            return sb;
        }

        if(expr.equals(EXPR_ISNULL)) {
            sb.append(SqlUtil.group(column +" is null "));
        } else   if(expr.equals(EXPR_ISNULL)) {
            sb.append(SqlUtil.group("NOT " +column +" is like ?" ));
        } else {
            String theExpr = column +" " + expr + " ?"; 
            sb.append(SqlUtil.group(theExpr));
        }
        return sb;
    }



    public int setValue(PreparedStatement stmt, int col) throws Exception {
        if(expr == null) return col;
        if(subClauses!=null) {
            for(int i=0;i<subClauses.length;i++) {
                col = subClauses[i].setValue(stmt, col);
            }
            return col;
        }


        if(expr.equals(EXPR_ISNULL)) {
            return col;
        }
        if(value instanceof String) {
        if(SqlUtil.debug) {
            System.err.println ("Setting " + column +"=" + value);
        }
            stmt.setString(col, value.toString());
        } else         if(value instanceof Double) {
            stmt.setDouble(col, ((Double)value).doubleValue());
        } else         if(value instanceof Integer) {
            stmt.setInt(col, ((Integer)value).intValue());
        } else if(value instanceof Date) {
            Date dttm = (Date) value;
            stmt.setTimestamp(col, new java.sql.Timestamp(dttm.getTime()),
                               calendar);
        } else {
            throw new IllegalArgumentException("Unknown value:" + value);
        }
        return col+1;
    }



    /**
       Set the Column property.

       @param value The new value for Column
    **/
    public void setColumn (String value) {
	column = value;
    }

    /**
       Get the Column property.

       @return The Column
    **/
    public String getColumn () {
	return column;
    }

    /**
       Set the Expr property.

       @param value The new value for Expr
    **/
    public void setExpr (String value) {
	expr = value;
    }

    /**
       Get the Expr property.

       @return The Expr
    **/
    public String getExpr () {
	return expr;
    }

    /**
       Set the Value property.

       @param value The new value for Value
    **/
    public void setValue (Object value) {
	value = value;
    }

    /**
       Get the Value property.

       @return The Value
    **/
    public Object getValue () {
	return value;
    }



}

