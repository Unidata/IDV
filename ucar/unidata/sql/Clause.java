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


    public static final String EXPR_EQUALS = "=";
    public static final String EXPR_NOTEQUALS = "<>";
    public static final String EXPR_LIKE = "LIKE";
    public static final String EXPR_OR = "OR";
    public static final String EXPR_AND = "AND";


    private String expr = EXPR_EQUALS;
    private String column;
    private Object value;
    private Clause[]subClauses;


    public Clause(String expr, Clause[]subClauses) {
        this.expr = expr;
        this.subClauses = subClauses;
    }


    public Clause(String column, String expr, Object value) {
        this.column = column;
        this.expr = expr;
        this.value = value;
    }

    public static Clause or(Clause[]clauses) {
        return new Clause(EXPR_OR, clauses);
    }

    public static Clause and(Clause[]clauses) {
        return new Clause(EXPR_AND, clauses);
    }


    public static Clause eq(String column, Object value) {
        return new Clause(column, EXPR_EQUALS, value);
    }

    public static Clause neq(String column, Object value) {
        return new Clause(column, EXPR_NOTEQUALS, value);
    }

    public static Clause like(String column, Object value) {
        return new Clause(column, EXPR_LIKE, value);
    }

    public void addClause(StringBuffer sb) {
        //TODO: Handle sub clauses
        String theExpr = column +" " + expr + " ?"; 
        sb.append(SqlUtil.group(theExpr));
    }



    public int setValue(PreparedStatement stmt, int col) throws Exception {
        if(subClauses!=null) {
            for(int i=0;i<subClauses.length;i++) {
                col = subClauses[i].setValue(stmt, col);
            }
            return col;
        }


        if(value instanceof String) {
            stmt.setString(col, value.toString());
        } else         if(value instanceof Double) {
            stmt.setDouble(col, ((Double)value).doubleValue());
        } else         if(value instanceof Integer) {
            stmt.setInt(col, ((Integer)value).intValue());
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

