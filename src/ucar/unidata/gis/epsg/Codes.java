
// $Id: Codes.java,v 1.8 2005/03/10 18:38:32 jeffmc Exp $

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

public class Codes extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/codes.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The recordId property
     */
    private double recordId;


    /**
     * The tableName property
     */
    private String tableName;


    /**
     * The tableContents property
     */
    private String tableContents;


    /**
     * The codeMinimum property
     */
    private int codeMinimum;


    /**
     * The codeMaximum property
     */
    private int codeMaximum;


    /**
     * The allocatedLow property
     */
    private int allocatedLow;


    /**
     * The allocatedHigh property
     */
    private int allocatedHigh;


    /**
     * The remarks property
     */
    private String remarks;


    /**
     * The constructor
     *       * @param arg_recordId The recordId argument
     * @param arg_tableName The tableName argument
     * @param arg_tableContents The tableContents argument
     * @param arg_codeMinimum The codeMinimum argument
     * @param arg_codeMaximum The codeMaximum argument
     * @param arg_allocatedLow The allocatedLow argument
     * @param arg_allocatedHigh The allocatedHigh argument
     * @param arg_remarks The remarks argument
     *
     */
    public Codes(double arg_recordId, String arg_tableName,
                 String arg_tableContents, int arg_codeMinimum,
                 int arg_codeMaximum, int arg_allocatedLow,
                 int arg_allocatedHigh, String arg_remarks) {
        this.recordId      = arg_recordId;
        this.tableName     = arg_tableName;
        this.tableContents = arg_tableContents;
        this.codeMinimum   = arg_codeMinimum;
        this.codeMaximum   = arg_codeMaximum;
        this.allocatedLow  = arg_allocatedLow;
        this.allocatedHigh = arg_allocatedHigh;
        this.remarks       = arg_remarks;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public Codes(List tuple) {
        this.recordId      = getDouble((String) tuple.get(0));
        this.tableName     = (String) tuple.get(1);
        this.tableContents = (String) tuple.get(2);
        this.codeMinimum   = getInt((String) tuple.get(3));
        this.codeMaximum   = getInt((String) tuple.get(4));
        this.allocatedLow  = getInt((String) tuple.get(5));
        this.allocatedHigh = getInt((String) tuple.get(6));
        this.remarks       = (String) tuple.get(7);
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
                         Codes.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 8) {
                System.err.println("csv/codes.csv: line #" + i + " " + line);
                continue;
            }
            try {
                new Codes(line);
            } catch (Exception exc) {
                System.err.println("Error creating Codes " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the recordId property.
     *
     * @return The recordId property
     */
    public double getRecordId() {
        return recordId;
    }


    /**
     * Return the tableName property.
     *
     * @return The tableName property
     */
    public String getTableName() {
        return tableName;
    }


    /**
     * Return the tableContents property.
     *
     * @return The tableContents property
     */
    public String getTableContents() {
        return tableContents;
    }


    /**
     * Return the codeMinimum property.
     *
     * @return The codeMinimum property
     */
    public int getCodeMinimum() {
        return codeMinimum;
    }


    /**
     * Return the codeMaximum property.
     *
     * @return The codeMaximum property
     */
    public int getCodeMaximum() {
        return codeMaximum;
    }


    /**
     * Return the allocatedLow property.
     *
     * @return The allocatedLow property
     */
    public int getAllocatedLow() {
        return allocatedLow;
    }


    /**
     * Return the allocatedHigh property.
     *
     * @return The allocatedHigh property
     */
    public int getAllocatedHigh() {
        return allocatedHigh;
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
     * Find the Codes object with the recordId value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Codes findRecordId(double value) {
        for (int i = 0; i < members.size(); i++) {
            Codes obj = (Codes) members.get(i);
            if (obj.recordId == value) {
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
        if (varname.equals("codeMinimum")) {
            return codeMinimum;
        }
        if (varname.equals("codeMaximum")) {
            return codeMaximum;
        }
        if (varname.equals("allocatedLow")) {
            return allocatedLow;
        }
        if (varname.equals("allocatedHigh")) {
            return allocatedHigh;
        }
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Return the double value by name
     *
     * @param varname The name
     * @return The double value
     */
    public double findDoubleByName(String varname) {
        if (varname.equals("recordId")) {
            return recordId;
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
        if (varname.equals("tableName")) {
            return tableName;
        }
        if (varname.equals("tableContents")) {
            return tableContents;
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
        return "" + "   recordId=" + recordId + "\n" + "   tableName="
               + tableName + "\n" + "   tableContents=" + tableContents
               + "\n" + "   codeMinimum=" + codeMinimum + "\n"
               + "   codeMaximum=" + codeMaximum + "\n" + "   allocatedLow="
               + allocatedLow + "\n" + "   allocatedHigh=" + allocatedHigh
               + "\n" + "   remarks=" + remarks + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of Codes




