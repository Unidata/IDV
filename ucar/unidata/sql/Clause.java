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
 * The Clause class provides a tree data structure for creating sql query clauses.
 * A leaf node clause has an operator and a set of operands - column names, values, etc.
 * A parent clause has one or more children clauses and an operator (e.g., EXPR_OR, EXPR_AND, etc).
 *
 * To use this do, e.g.:
 * <pre>
 * Clause andClause = Clause.and(Clause.eq("some db col", "some value"),
 *                              Clause.eq("some other db col", "some other value"));
 * StringBuffer sql = new StringBuffer("select * from table where ");
 * andClause.addClause(sql); //This creates the SQL expression for a PreparedStatement
 * PreparedStatement statement = ....
 * clause.setValue(statement, 1); //Set the values in the prepared statement. the "1" is the column count
 * </pre>
 * @author IDV & RAMADDA Development Team
 * @version $Revision: 1.3 $
 */
public class Clause {

    /** calendar */
    public static final GregorianCalendar calendar =
        new GregorianCalendar(DateUtil.TIMEZONE_GMT);


    /** expression */
    public static final String EXPR_EQUALS = "=";

    /** expression */
    public static final String EXPR_GE = ">=";

    /** expression */
    public static final String EXPR_LE = "<=";

    /** expression */
    public static final String EXPR_LT = "<";


    /** expression */
    public static final String EXPR_GT = ">";

    /** expression */
    public static final String EXPR_NOTEQUALS = "<>";

    /** expression */
    public static final String EXPR_LIKE = "LIKE";

    /** expression */
    public static final String EXPR_NOTLIKE = "NOTLIKE";

    /** expression */
    public static final String EXPR_ISNULL = "is null";

    /** expression */
    public static final String EXPR_ISNOTNULL = "is not null";


    /** expression */
    public static final String EXPR_IN = "IN";

    /** expression */
    public static final String EXPR_OR = "OR";

    /** expression */
    public static final String EXPR_AND = "AND";


    /** expression */
    public static final String EXPR_JOIN = "join";

    /** the expression */
    private String expr = null;

    /** database column name we are a clause for */
    private String column;

    /** the operand value */
    private Object value;

    /** Holds the extra select for the IN operand */
    private String extraSelectForInClause;

    /** for tree nodes */
    private Clause[] subClauses;


    /**
     * ctor
     */
    public Clause() {}

    /**
     * ctor for tree nodes
     *
     * @param expr the exprssion (e.g., EXPR_AND, EXPR_OR, ...)
     * @param subClauses children clauses
     */
    public Clause(String expr, Clause[] subClauses) {
        this.expr       = expr;
        this.subClauses = subClauses;
    }


    /**
     * ctor for regular column clause
     *
     * @param column the column name
     * @param expr the expression  (e.g., EXPR_GT, EXPR_EQ)
     * @param value the value for the expression
     */
    public Clause(String column, String expr, Object value) {
        this.column = column;
        this.expr   = expr;
        this.value  = value;
    }

    /**
     * utility to make a 2 element OR clause
     *
     * @param clause1 child clause
     * @param clause2 child clause
     *
     * @return a new OR clause
     */
    public static Clause or(Clause clause1, Clause clause2) {
        return new Clause(EXPR_OR, new Clause[] { clause1, clause2 });
    }



    /**
     * utility to make an AND clause
     *
     * @param clause1 child clause
     * @param clause2 child clause
     *
     * @return new AND clause
     */
    public static Clause and(Clause clause1, Clause clause2) {
        return new Clause(EXPR_AND, new Clause[] { clause1, clause2 });
    }

    /**
     * utility to make an N element OR clause
     *
     * @param clauses children clauses
     *
     * @return new OR clause
     */
    public static Clause or(Clause[] clauses) {
        if ((clauses == null) || (clauses.length == 0)) {
            return null;
        }
        return new Clause(EXPR_OR, clauses);
    }

    /**
     * utility to make an N element AND clause
     *
     * @param clauses children clauses
     *
     * @return new AND clause
     */
    public static Clause and(Clause[] clauses) {
        if ((clauses == null) || (clauses.length == 0)) {
            return null;
        }
        return new Clause(EXPR_AND, clauses);
    }

    /**
     * utility to make an N element AND clause
     *
     * @param clauses children clauses
     *
     * @return new AND clause
     */
    public static Clause and(List<Clause> clauses) {
        return new Clause(EXPR_AND, toArray(clauses));
    }


    /**
     * utility to make an N element OR clause
     *
     * @param clauses children clauses
     *
     * @return new OR clause
     */
    public static Clause or(List<Clause> clauses) {
        return new Clause(EXPR_OR, toArray(clauses));
    }


    /**
     * This makes a number of  EQUALS clauses first converting the given String values  into ints
     *
     * @param colName column
     * @param values values to convert to int
     *
     * @return List of EQUALS clauses
     */
    public static List<Clause> makeIntClauses(String colName,
            List<String> values) {
        List<Clause> clauses = new ArrayList<Clause>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            value = value.trim();
            clauses.add(Clause.eq(colName, new Integer(value).intValue()));
        }
        return clauses;
    }

    /**
     * This makes a number of EQUALS clauses with the given String values
     *
     * @param colName The column
     * @param values values
     *
     * @return List of EQUALS clauses
     */
    public static List<Clause> makeStringClauses(String colName,
            List<String> values) {
        List<Clause> clauses = new ArrayList<Clause>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            value = value.trim();
            clauses.add(Clause.eq(colName, value));
        }
        return clauses;
    }


    /**
     * make an EQUALS or a NOT EQUALS clause with the given column and value
     *
     * @param column Column name
     * @param value value
     * @param not If true  then make a NOT EQUALS clause
     *
     * @return Clause
     */
    public static Clause eq(String column, Object value, boolean not) {
        if (not) {
            return neq(column, value);
        }
        return eq(column, value);
    }


    /**
     * Make an EQUALS clause
     *
     * @param column Column name
     * @param value value
     *
     * @return Clause
     */
    public static Clause eq(String column, Object value) {
        return new Clause(column, EXPR_EQUALS, value);
    }

    /**
     * Make an NOT EQUALS clause
     *
     * @param column Column name
     * @param value value
     *
     * @return Clause
     */
    public static Clause neq(String column, Object value) {
        return new Clause(column, EXPR_NOTEQUALS, value);
    }


    /**
     * makes a new GE clause
     *
     * @param column Column name
     * @param value The value
     *
     * @return The new Clause
     */
    public static Clause ge(String column, Object value) {
        return new Clause(column, EXPR_GE, value);
    }

    /**
     * makes a new GT clause
     *
     * @param column The column
     * @param value The value
     *
     * @return the new clause
     */
    public static Clause gt(String column, Object value) {
        return new Clause(column, EXPR_GT, value);
    }

    /**
     * makes a new LT clause
     *
     * @param column The column
     * @param value The value
     *
     * @return the new clause
     */
    public static Clause lt(String column, Object value) {
        return new Clause(column, EXPR_LT, value);
    }

    /**
     * makes a new LE clause
     *
     * @param column Column name
     * @param value The value
     *
     * @return The new Clause
     */
    public static Clause le(String column, Object value) {
        return new Clause(column, EXPR_LE, value);
    }

    /**
     * makes a new LE clause
     *
     * @param column Column name
     * @param value The value
     *
     * @return The new Clause
     */
    public static Clause le(String column, double value) {
        return le(column, new Double(value));
    }


    /**
     * makes a new GE clause
     *
     * @param column Column name
     * @param value The value
     *
     * @return The new Clause
     */
    public static Clause ge(String column, double value) {
        return ge(column, new Double(value));
    }


    /**
     * makes a IS NULL clause
     *
     * @param column Column name
     *
     * @return The new Clause
     */
    public static Clause isNull(String column) {
        return new Clause(column, EXPR_ISNULL, null);
    }

    /**
     * makes a IS NOT NULL clause
     *
     * @param column The column
     *
     * @return the new clause
     */
    public static Clause isNotNull(String column) {
        return new Clause(column, EXPR_ISNOTNULL, null);
    }




    /**
     * Makes a COLUMN LIKE 'value' or a NOT LIKE
     *
     * @param column Column name
     * @param value The value
     * @param not NOT or not
     *
     * @return The new Clause
     */
    public static Clause like(String column, Object value, boolean not) {
        if (not) {
            return notLike(column, value);
        }
        return like(column, value);
    }



    /**
     * Make a LIKE clause
     *
     * @param column Column name
     * @param value The value
     *
     * @return The new Clause
     */
    public static Clause like(String column, Object value) {
        return new Clause(column, EXPR_LIKE, value);
    }

    /**
     * Make a NOT LIKE clause
     *
     * @param column Column name
     * @param value The value
     *
     * @return The new Clause
     */
    public static Clause notLike(String column, Object value) {
        return new Clause(column, EXPR_NOTLIKE, value);
    }




    /**
     * Makes a simple COLUMN IN (a,b,c,...) clause
     *
     * @param column Column name
     * @param inner The inner set
     *
     * @return The new Clause
     */
    public static Clause in(String column, String inner) {
        return new Clause(column, EXPR_IN, inner);
    }

    /**
     * Makes a COLUMN IN (SELECT what FROM from where <inner clause>) caluse
     *
     * @param column Column name
     * @param what What to select in the inner select
     * @param from from where
     * @param inner with clause
     *
     * @return The new Clause
     */
    public static Clause in(String column, String what, String from,
                            Clause inner) {
        Clause clause = new Clause(EXPR_IN, new Clause[] { inner });
        clause.column = column;
        clause.extraSelectForInClause = " select " + what + " from " + from
                                        + " ";
        return clause;
    }


    /**
     * makes a join clause
     *
     * @param column Column name
     * @param column2 Other column
     *
     * @return The new Clause
     */
    public static Clause join(String column, String column2) {
        return new Clause(column, EXPR_JOIN, column2);
    }



    /**
     * Converts list to array
     *
     * @param clauses array
     *
     * @return list
     */
    public static Clause[] toArray(List<Clause> clauses) {
        Clause[] array = new Clause[clauses.size()];
        for (int i = 0; i < clauses.size(); i++) {
            array[i] = clauses.get(i);
        }
        return array;
    }




    /**
     * Utility to check if any of the clauses come from the given DB table
     *
     * @param clauses clauses to check
     * @param table DB table name
     *
     * @return any clauses on given table
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
     * This tokenizes the given values by splitting the string on ","
     * If a value has a wildcard "%" in it then this creates a LIKE clause
     * If a value begins with "!" then this creates a NEQ clause
     * Else it creates an EQUALS clause
     *
     * If its just regular clauses we get an OR of the clauses.
     * If its just the NOT clauses we AND them
     * Else if its a mix this  does:
     * (OR of the EQUALS clauses) AND (AND of the NOT EQUALS clauses)
     *
     * @param column Column name
     * @param values the values
     *
     * @return new Clause
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
            if (value.startsWith("%") || value.endsWith("%")) {
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
     * Is this Clauses (or its children) column from the DB table. Typically the column name
     * is the fully qualified table_name.column_name
     *
     * @param table
     *
     * @return is from table
     */
    public boolean isColumnFromTable(String table) {
        if (column == null) {
            if (subClauses != null) {
                for (int i = 0; i < subClauses.length; i++) {
                    if ((subClauses[i] != null)
                            && subClauses[i].isColumnFromTable(table)) {
                        return true;
                    }
                }
            }

            return false;
        }
        return column.startsWith(table + ".");
    }


    /**
     * Get the table names used in the Clause tree
     *
     * @return List of unique table names
     */
    public List<String> getTableNames() {
        return getTableNames(new ArrayList<String>());
    }


    /**
     * Get the table names used in the Clause tree
     *
     * @param names list to add to
     *
     * @return List of unique table names
     */
    public List<String> getTableNames(List<String> names) {
        doGetTableNames(names);
        return names;
    }

    /**
     * Get the table names used in the Clause tree
     *
     * @param names list to add to
     */
    private void doGetTableNames(List<String> names) {
        if (expr.equals(EXPR_IN)) {
            return;
        }
        if (subClauses != null) {


            for (Clause clause : subClauses) {
                if (clause == null) {
                    continue;
                }
                clause.doGetTableNames(names);
            }
            return;
        }

        if (expr.equals(EXPR_JOIN)) {
            int idx = value.toString().indexOf(".");
            if (idx >= 0) {
                String name = value.toString().substring(0, idx);
                if ( !names.contains(name)) {
                    names.add(name);
                }
            }
        }

        if (column != null) {
            int idx = column.indexOf(".");
            if (idx >= 0) {
                String name = column.substring(0, idx);
                if ( !names.contains(name)) {
                    names.add(name);
                }
            }
        }
    }





    /**
     * If this Clause has a column is it the given one
     *
     * @param col column name to check
     *
     * @return my column
     */
    public boolean isColumn(String col) {
        if (column == null) {
            return false;
        }
        return column.equals(col);
    }

    /**
     * Any of the clauses applied to the given DB column
     *
     * @param clauses clauses to check
     * @param col column name to check
     *
     * @return my column
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
     * This makes the prepared statement SQL string. All of the values will have "?" in them.
     *
     * @param sb Buffer to append to
     *
     * @return the given sb buffer
     */
    public StringBuffer addClause(StringBuffer sb) {
        if (expr == null) {
            return sb;
        }
        if (subClauses != null) {
            List toks = new ArrayList();
            for (int i = 0; i < subClauses.length; i++) {
                StringBuffer buff = new StringBuffer();
                if (subClauses[i] != null) {
                    subClauses[i].addClause(buff);
                    toks.add(buff.toString());
                }
            }

            if (expr.equals(EXPR_IN)) {
                if (toks.size() == 0) {
                    sb.append(SqlUtil.group(column + "  IN ( "
                                            + extraSelectForInClause + ")"));
                } else {
                    sb.append(SqlUtil.group(column + "  IN ( "
                                            + extraSelectForInClause
                                            + " WHERE " + toks.get(0) + ")"));
                }
            } else {
                if (toks.size() > 1) {
                    sb.append("(");
                }
                sb.append(StringUtil.join(" " + expr + " ", toks));
                if (toks.size() > 1) {
                    sb.append(")");
                }
            }
            return sb;
        }

        if (expr.equals(EXPR_ISNULL)) {
            sb.append(SqlUtil.group(column + " is null "));
        } else if (expr.equals(EXPR_ISNOTNULL)) {
            sb.append(SqlUtil.group(column + " is not null "));
        } else if (expr.equals(EXPR_JOIN)) {
            sb.append(SqlUtil.group(column + " =  " + value));
        } else if (expr.equals(EXPR_LIKE)) {
            sb.append(SqlUtil.group(column + "  like ?"));
        } else if (expr.equals(EXPR_IN)) {
            sb.append(SqlUtil.group(column + "  IN (" + value + ")"));
        } else if (expr.equals(EXPR_NOTLIKE)) {
            sb.append(SqlUtil.group("NOT " + column + "  like ?"));
        } else {
            if (SqlUtil.debug) {
                //                System.err.println(toString());
            }
            String theExpr = column + " " + expr + " ?";
            sb.append(SqlUtil.group(theExpr));
        }
        return sb;
    }



    /**
     * This sets the values in the prepared statement
     *
     * @param stmt statement
     * @param col Which value index in the SQL (e.g., which "?") are we  at
     *
     * @return Next value index
     *
     * @throws Exception On badness
     */
    public int setValue(PreparedStatement stmt, int col) throws Exception {
        if (expr == null) {
            return col;
        }
        if (subClauses != null) {
            for (int i = 0; i < subClauses.length; i++) {
                if (subClauses[i] != null) {
                    col = subClauses[i].setValue(stmt, col);
                }
            }
            return col;
        }

        if (expr.equals(EXPR_ISNULL) || expr.equals(EXPR_ISNOTNULL)
                || expr.equals(EXPR_JOIN)) {
            return col;
        }
        if (SqlUtil.debug) {
            System.err.println("   value:" + value + " " + col);
        }
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
     * Convert this to a string representation of the expression
     *
     * @return to string
     */
    public String toString() {
        if (column != null) {
            if (expr.equals(EXPR_IN)) {
                if ((subClauses != null) && (subClauses.length > 0)) {
                    return column + " " + expr + " ("
                           + extraSelectForInClause + " WHERE "
                           + subClauses[0] + ")";
                } else {
                    return column + " " + expr + " ("
                           + extraSelectForInClause + ")";
                }
            }
            String svalue;
            if (value instanceof String) {
                svalue = "'" + value + "'";
            } else {
                svalue = "" + value;
            }
            return column + " " + expr + " " + svalue;
        } else if (subClauses != null) {
            if (subClauses.length == 1) {
                return subClauses[0].toString();
            }
            return "(" + Misc.join(" " + expr + " ", subClauses) + ")";
        }
        return "clause:null";
    }

}
