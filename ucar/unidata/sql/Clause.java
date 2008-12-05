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

    /** _more_ */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);


    /** _more_ */
    public static final String EXPR_EQUALS = "=";

    /** _more_ */
    public static final String EXPR_GE = ">=";

    /** _more_ */
    public static final String EXPR_LE = "<=";

    /** _more_ */
    public static final String EXPR_NOTEQUALS = "<>";

    /** _more_ */
    public static final String EXPR_LIKE = "LIKE";

    /** _more_ */
    public static final String EXPR_NOTLIKE = "NOTLIKE";

    /** _more_ */
    public static final String EXPR_ISNULL = "is null";

    /** _more_ */
    public static final String EXPR_OR = "OR";

    /** _more_ */
    public static final String EXPR_AND = "AND";


    /** _more_          */
    public static final String EXPR_JOIN = "join";

    /** _more_ */
    private String expr = null;

    /** _more_ */
    private String column;

    /** _more_ */
    private Object value;

    /** _more_ */
    private Clause[] subClauses;


    /**
     * _more_
     */
    public Clause() {}

    /**
     * _more_
     *
     * @param expr _more_
     * @param subClauses _more_
     */
    public Clause(String expr, Clause[] subClauses) {
        this.expr       = expr;
        this.subClauses = subClauses;
    }


    /**
     * _more_
     *
     * @param column _more_
     * @param expr _more_
     * @param value _more_
     */
    public Clause(String column, String expr, Object value) {
        this.column = column;
        this.expr   = expr;
        this.value  = value;
    }

    /**
     * _more_
     *
     * @param clause1 _more_
     * @param clause2 _more_
     *
     * @return _more_
     */
    public static Clause or(Clause clause1, Clause clause2) {
        return new Clause(EXPR_OR, new Clause[] { clause1, clause2 });
    }

    /**
     * _more_
     *
     * @param clause1 _more_
     * @param clause2 _more_
     *
     * @return _more_
     */
    public static Clause and(Clause clause1, Clause clause2) {
        return new Clause(EXPR_AND, new Clause[] { clause1, clause2 });
    }

    /**
     * _more_
     *
     * @param clauses _more_
     *
     * @return _more_
     */
    public static Clause or(Clause[] clauses) {
        return new Clause(EXPR_OR, clauses);
    }

    /**
     * _more_
     *
     * @param clauses _more_
     *
     * @return _more_
     */
    public static Clause and(Clause[] clauses) {
        return new Clause(EXPR_AND, clauses);
    }

    /**
     * _more_
     *
     * @param clauses _more_
     *
     * @return _more_
     */
    public static Clause and(List<Clause> clauses) {
        return new Clause(EXPR_AND, toArray(clauses));
    }


    /**
     * _more_
     *
     * @param clauses _more_
     *
     * @return _more_
     */
    public static Clause or(List<Clause> clauses) {
        return new Clause(EXPR_OR, toArray(clauses));
    }


    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static Clause eq(String column, Object value) {
        return new Clause(column, EXPR_EQUALS, value);
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static Clause ge(String column, Object value) {
        return   new Clause(column, EXPR_GE, value);
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static Clause le(String column, Object value) {
        return  new Clause(column, EXPR_LE, value);
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static Clause le(String column, double value) {
        return le(column, new Double(value));
    }


    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static Clause ge(String column, double value) {
        return ge(column, new Double(value));
    }


    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public static Clause isNull(String column) {
        return new Clause(column, EXPR_ISNULL, null);
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param column2 _more_
     *
     * @return _more_
     */
    public static Clause join(String column, String column2) {
        return new Clause(column, EXPR_JOIN, column2);
    }



    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static Clause neq(String column, Object value) {
        return new Clause(column, EXPR_NOTEQUALS, value);
    }


    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static Clause like(String column, Object value) {
        return new Clause(column, EXPR_LIKE, value);
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param value _more_
     *
     * @return _more_
     */
    public static Clause notLike(String column, Object value) {
        return new Clause(column, EXPR_NOTLIKE, value);
    }

    /**
     * _more_
     *
     * @param clauses _more_
     *
     * @return _more_
     */
    public static Clause[] toArray(List<Clause> clauses) {
        Clause[] array = new Clause[clauses.size()];
        for (int i = 0; i < clauses.size(); i++) {
            array[i] = clauses.get(i);
        }
        return array;
    }

    /**
     * _more_
     *
     * @param table _more_
     *
     * @return _more_
     */
    public boolean isColumnFromTable(String table) {
        if (column == null) {
            if(subClauses!=null) {
                for (int i = 0; i < subClauses.length; i++) {
                    if(subClauses[i].isColumnFromTable(table)) {
                        return true;
                    }
                }
            }

            return false;
        }
        return column.startsWith(table + ".");
    }

    /**
     * _more_
     *
     * @param clauses _more_
     * @param table _more_
     *
     * @return _more_
     */
    public static boolean isColumnFromTable(List<Clause> clauses,
                                            String table) {
        for (Clause clause : clauses) {
            if (clause.isColumnFromTable(table)) {
                return true;
            }
        }
        return false;
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param values _more_
     *
     * @return _more_
     */
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


        List<Clause> clauses    = new ArrayList<Clause>();
        List<Clause> notClauses = new ArrayList<Clause>();


        for (int i = 0; i < nots.size(); i++) {
            String value = nots.get(i).toString();
            notClauses.add(Clause.neq(column, value));
        }


        for (int i = 0; i < notNots.size(); i++) {
            String value = notNots.get(i).toString();
            if(value.startsWith("%") || value.endsWith("%")) {
                clauses.add(Clause.like(column, value));
            } else {
                clauses.add(Clause.eq(column, value));
            }
        }

        if ((notClauses.size() > 0) && (clauses.size() > 0)) {
            return Clause.and(Clause.and(toArray(notClauses)),
                              Clause.or(toArray(clauses)));
        } else if (notClauses.size() > 0) {
            if (notClauses.size() == 1) {
                return notClauses.get(0);
            }
            return Clause.and(toArray(notClauses));
        } else if (clauses.size() > 0) {
            if (clauses.size() == 1) {
                return clauses.get(0);
            }
            return Clause.or(toArray(clauses));
        }
        return new Clause();
    }



    /**
     * _more_
     *
     * @param col _more_
     *
     * @return _more_
     */
    public boolean isColumn(String col) {
        if(column==null) return false;
        return column.equals(col);
    }

    /**
     * _more_
     *
     * @param clauses _more_
     * @param col _more_
     *
     * @return _more_
     */
    public static boolean isColumn(List<Clause> clauses, String col) {
        for (Clause clause : clauses) {
            if (clause.isColumn(col)) {
                return true;
            }
        }
        return false;
    }



    /**
     * _more_
     *
     * @param sb _more_
     *
     * @return _more_
     */
    public StringBuffer addClause(StringBuffer sb) {
        if (expr == null) {
            return sb;
        }
        if (subClauses != null) {
            List toks = new ArrayList();
            for (int i = 0; i < subClauses.length; i++) {
                StringBuffer buff = new StringBuffer();
                subClauses[i].addClause(buff);
                toks.add(buff.toString());
            }
            if(toks.size()>1) 
                sb.append("(");
            sb.append(StringUtil.join(" " + expr + " ", toks));
            if(toks.size()>1) 
                sb.append(")");
            return sb;
        }

        if (expr.equals(EXPR_ISNULL)) {
            sb.append(SqlUtil.group(column + " is null "));
        } else if (expr.equals(EXPR_JOIN)) {
            sb.append(SqlUtil.group(column + " =  " + value));
        } else if (expr.equals(EXPR_LIKE)) {
            sb.append(SqlUtil.group(column + "  like ?"));
        } else if (expr.equals(EXPR_NOTLIKE)) {
            sb.append(SqlUtil.group("NOT " + column + "  like ?"));
        } else {
            if(SqlUtil.debug)
                System.err.println (toString());
            String theExpr = column + " " + expr + " ?";
            sb.append(SqlUtil.group(theExpr));
        }
        return sb;
    }



    /**
     * _more_
     *
     * @param stmt _more_
     * @param col _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public int setValue(PreparedStatement stmt, int col) throws Exception {
        if (expr == null) {
            return col;
        }
        if (subClauses != null) {
            for (int i = 0; i < subClauses.length; i++) {
                col = subClauses[i].setValue(stmt, col);
            }
            return col;
        }

        if (expr.equals(EXPR_ISNULL) || expr.equals(EXPR_JOIN)) {
            return col;
        }
        if(SqlUtil.debug)
            System.err.println("   value:"  + value + " " + col);
        SqlUtil.setValue(stmt, value, col);
        return col + 1;
    }



    /**
     *  Set the Column property.
     *
     *  @param value The new value for Column
     */
    public void setColumn(String value) {
        column = value;
    }

    /**
     *  Get the Column property.
     *
     *  @return The Column
     */
    public String getColumn() {
        return column;
    }

    /**
     *  Set the Expr property.
     *
     *  @param value The new value for Expr
     */
    public void setExpr(String value) {
        expr = value;
    }

    /**
     *  Get the Expr property.
     *
     *  @return The Expr
     */
    public String getExpr() {
        return expr;
    }

    /**
     *  Set the Value property.
     *
     *  @param value The new value for Value
     */
    public void setValue(Object value) {
        value = value;
    }

    /**
     *  Get the Value property.
     *
     *  @return The Value
     */
    public Object getValue() {
        return value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (column != null) {
            return "column:" + column + " " + expr + " " + value;
        } else if (subClauses != null) {
            return expr + " " + Misc.toList(subClauses);
        }
        return "clause:null";
    }

}

