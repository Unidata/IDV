/*
 * $Id: DataTree.java,v 1.50 2007/08/21 12:15:45 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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




package ucar.unidata.ui;


import org.python.core.*;
import org.python.util.*;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.sql.SqlUtil;


import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;

import java.io.*;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;

import java.sql.*;



/**
 * This class provides  an interactive shell to query a sql database
 *
 * @author IDV development team
 * @version $Revision: 1.50 $Date: 2007/08/21 12:15:45 $
 */
public class SqlShell extends InteractiveShell {
    
    private Connection connection;


    /**
     * _more_
     *
     *
     * @param title _more_
     */
    public SqlShell(String title, Connection connection) {
        super(title);
        this.connection = connection;
        init();
        commandFld.setToolTipText("Enter '?' to list tables; '?tablename' to list columns of table;");
    }




    /**
     * _more_
     *
     * @param sql the sql to evaluate
     */
    public void eval(String sql) {
        super.eval(sql);
        showWaitCursor();
        try {
            StringBuffer sb    = new StringBuffer("");
            if(sql.trim().startsWith("?")) {
                String tablePattern = sql.substring(1).trim();
                if(tablePattern.length()==0) tablePattern = null;
                if(tablePattern== null) {
                    sb.append("Tables:<br>");
                }
                DatabaseMetaData dbmd = connection.getMetaData();
                ResultSet catalogs = dbmd.getCatalogs();
                while(catalogs.next()) {
                    String catalog = catalogs.getString(1);
                    sb.append("<h3>" + catalog +"</h3>");
                    ResultSet tables = dbmd.getTables(catalog, null,tablePattern,null);
                    while(tables.next()) {
                        String tableName = tables.getString("TABLE_NAME");
                        ResultSet 	columns = dbmd.getColumns(catalog, null, tableName,null);
                        String encoded = new String(XmlUtil.encodeBase64(("text:?"+tableName).getBytes()));
                        if(tablePattern!= null) {
                            sb.append ("Table:" + tableName + " " + getHref("select * from " +tableName+";", "(select *)")+"<ul>");
                        } else {
                            sb.append("<a href=\"" + encoded +"\">"+tableName+"</a>");
                        }
                        if(tablePattern!= null) {
                            while(columns.next()) {
                                String colName = columns.getString("COLUMN_NAME");
                                sb.append("<li>");
                                sb.append(getHref("select "+colName +" from " +tableName+";", colName));
                                sb.append(" ("+ columns.getString("TYPE_NAME")+")");
                            }
                            sb.append("</ul>");
                        } else {
                            sb.append ("<br>");
                        }
                    }
                }
                output(sb.toString());
                return;
            }

            if(!sql.endsWith(";")) sql  = sql +";";
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
            ResultSet results;
            int cnt = 0;
            SqlUtil.Iterator iter = SqlUtil.getIterator(stmt);
            ResultSetMetaData rsmd = null;
            while((results = iter.getNext())!=null) {
                if(rsmd==null) {
                    rsmd = results.getMetaData();
                }
                    int colcnt = 0;
                    while(colcnt<rsmd.getColumnCount()) {
                        String s =results.getString(++colcnt); 
                        if(colcnt>1) sb.append (", ");
                        sb.append(s);
                    }
                    sb.append("<br>");
                    cnt++;
                    if(cnt>100) {
                        sb.append("...");
                        break;
                    }
            }
            if(cnt == 0) {
                sb.append("No data retreived<br>");
            }
            output(sb.toString());
        } catch (Exception exc) {
            LogUtil.logException ("Error evaluating sql:" + sql, exc);
        } finally {
            showNormalCursor();
        }

        
    }




}

