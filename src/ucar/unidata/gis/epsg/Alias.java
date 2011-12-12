
// $Id: Alias.java,v 1.8 2005/03/10 18:38:32 jeffmc Exp $

/*
 * Copyright 1997-2001 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

package ucar.unidata.gis.epsg;


import java.util.List;
import java.util.ArrayList;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.CsvDb;


/**
 *  This class has been generated from the different csv files from the libgeotiff package
 *
 * @author IDV development team
 */

public class Alias extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/alias.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The aliasCode property
     */
    private int aliasCode;


    /**
     * The objectTableName property
     */
    private String objectTableName;


    /**
     * The objectCode property
     */
    private int objectCode;


    /**
     * The namingSystemCode property
     */
    private int namingSystemCode;


    /**
     * The alias property
     */
    private String alias;


    /**
     * The remarks property
     */
    private String remarks;


    /**
     * The constructor
     *       * @param arg_aliasCode The aliasCode argument
     * @param arg_objectTableName The objectTableName argument
     * @param arg_objectCode The objectCode argument
     * @param arg_namingSystemCode The namingSystemCode argument
     * @param arg_alias The alias argument
     * @param arg_remarks The remarks argument
     *
     */
    public Alias(int arg_aliasCode, String arg_objectTableName,
                 int arg_objectCode, int arg_namingSystemCode,
                 String arg_alias, String arg_remarks) {
        this.aliasCode        = arg_aliasCode;
        this.objectTableName  = arg_objectTableName;
        this.objectCode       = arg_objectCode;
        this.namingSystemCode = arg_namingSystemCode;
        this.alias            = arg_alias;
        this.remarks          = arg_remarks;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public Alias(List tuple) {
        this.aliasCode        = getInt((String) tuple.get(0));
        this.objectTableName  = (String) tuple.get(1);
        this.objectCode       = getInt((String) tuple.get(2));
        this.namingSystemCode = getInt((String) tuple.get(3));
        this.alias            = (String) tuple.get(4);
        this.remarks          = (String) tuple.get(5);
        members.add(this);
    }

    static {
        doInit();
    }

    /**
     * The static initialization
     */
    private static void doInit() {
        String csv = ucar.unidata.util.IOUtil.readContents(csvFileName,
                         Alias.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 6) {
                System.err.println("csv/alias.csv: line #" + i + " " + line);
                continue;
            }
            try {
                new Alias(line);
            } catch (Exception exc) {
                System.err.println("Error creating Alias " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the aliasCode property.
     *
     * @return The aliasCode property
     */
    public int getAliasCode() {
        return aliasCode;
    }


    /**
     * Return the objectTableName property.
     *
     * @return The objectTableName property
     */
    public String getObjectTableName() {
        return objectTableName;
    }


    /**
     * Return the objectCode property.
     *
     * @return The objectCode property
     */
    public int getObjectCode() {
        return objectCode;
    }


    /**
     * Return the namingSystemCode property.
     *
     * @return The namingSystemCode property
     */
    public int getNamingSystemCode() {
        return namingSystemCode;
    }


    /**
     * Return the alias property.
     *
     * @return The alias property
     */
    public String getAlias() {
        return alias;
    }


    /**
     * Return the remarks property.
     *
     * @return The remarks property
     */
    public String getRemarks() {
        return remarks;
    }


    /**
     * Find the Alias object with the aliasCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Alias findAliasCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            Alias obj = (Alias) members.get(i);
            if (obj.aliasCode == value) {
                return obj;
            }
        }
        return null;
    }

    /**
     * Return the integer value by name
     *
     * @param varname The name
     * @return The integer value
     */
    public int findIntByName(String varname) {
        if (varname.equals("aliasCode")) {
            return aliasCode;
        }
        if (varname.equals("objectCode")) {
            return objectCode;
        }
        if (varname.equals("namingSystemCode")) {
            return namingSystemCode;
        }
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Return the String value by name
     *
     * @param varname The name
     * @return The String value
     */
    public String findStringByName(String varname) {
        if (varname.equals("objectTableName")) {
            return objectTableName;
        }
        if (varname.equals("alias")) {
            return alias;
        }
        if (varname.equals("remarks")) {
            return remarks;
        }
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   aliasCode=" + aliasCode + "\n"
               + "   objectTableName=" + objectTableName + "\n"
               + "   objectCode=" + objectCode + "\n"
               + "   namingSystemCode=" + namingSystemCode + "\n"
               + "   alias=" + alias + "\n" + "   remarks=" + remarks + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of Alias




