
// $Id: Deprecation.java,v 1.8 2005/03/10 18:38:34 jeffmc Exp $

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

public class Deprecation extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/deprecation.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The deprecationId property
     */
    private int deprecationId;


    /**
     * The deprecationDate property
     */
    private String deprecationDate;


    /**
     * The changeId property
     */
    private double changeId;


    /**
     * The objectTableName property
     */
    private String objectTableName;


    /**
     * The objectCode property
     */
    private int objectCode;


    /**
     * The replacedBy property
     */
    private int replacedBy;


    /**
     * The deprecationReason property
     */
    private String deprecationReason;


    /**
     * The constructor
     *       * @param arg_deprecationId The deprecationId argument
     * @param arg_deprecationDate The deprecationDate argument
     * @param arg_changeId The changeId argument
     * @param arg_objectTableName The objectTableName argument
     * @param arg_objectCode The objectCode argument
     * @param arg_replacedBy The replacedBy argument
     * @param arg_deprecationReason The deprecationReason argument
     *
     */
    public Deprecation(int arg_deprecationId, String arg_deprecationDate,
                       double arg_changeId, String arg_objectTableName,
                       int arg_objectCode, int arg_replacedBy,
                       String arg_deprecationReason) {
        this.deprecationId     = arg_deprecationId;
        this.deprecationDate   = arg_deprecationDate;
        this.changeId          = arg_changeId;
        this.objectTableName   = arg_objectTableName;
        this.objectCode        = arg_objectCode;
        this.replacedBy        = arg_replacedBy;
        this.deprecationReason = arg_deprecationReason;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public Deprecation(List tuple) {
        this.deprecationId     = getInt((String) tuple.get(0));
        this.deprecationDate   = (String) tuple.get(1);
        this.changeId          = getDouble((String) tuple.get(2));
        this.objectTableName   = (String) tuple.get(3);
        this.objectCode        = getInt((String) tuple.get(4));
        this.replacedBy        = getInt((String) tuple.get(5));
        this.deprecationReason = (String) tuple.get(6);
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
                         Deprecation.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 7) {
                System.err.println("csv/deprecation.csv: line #" + i + " "
                                   + line);
                continue;
            }
            try {
                new Deprecation(line);
            } catch (Exception exc) {
                System.err.println("Error creating Deprecation " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the deprecationId property.
     *
     * @return The deprecationId property
     */
    public int getDeprecationId() {
        return deprecationId;
    }


    /**
     * Return the deprecationDate property.
     *
     * @return The deprecationDate property
     */
    public String getDeprecationDate() {
        return deprecationDate;
    }


    /**
     * Return the changeId property.
     *
     * @return The changeId property
     */
    public double getChangeId() {
        return changeId;
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
     * Return the replacedBy property.
     *
     * @return The replacedBy property
     */
    public int getReplacedBy() {
        return replacedBy;
    }


    /**
     * Return the deprecationReason property.
     *
     * @return The deprecationReason property
     */
    public String getDeprecationReason() {
        return deprecationReason;
    }


    /**
     * Find the Deprecation object with the deprecationId value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Deprecation findDeprecationId(int value) {
        for (int i = 0; i < members.size(); i++) {
            Deprecation obj = (Deprecation) members.get(i);
            if (obj.deprecationId == value) {
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
        if (varname.equals("deprecationId")) {
            return deprecationId;
        }
        if (varname.equals("objectCode")) {
            return objectCode;
        }
        if (varname.equals("replacedBy")) {
            return replacedBy;
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
        if (varname.equals("changeId")) {
            return changeId;
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
        if (varname.equals("deprecationDate")) {
            return deprecationDate;
        }
        if (varname.equals("objectTableName")) {
            return objectTableName;
        }
        if (varname.equals("deprecationReason")) {
            return deprecationReason;
        }
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   deprecationId=" + deprecationId + "\n"
               + "   deprecationDate=" + deprecationDate + "\n"
               + "   changeId=" + changeId + "\n" + "   objectTableName="
               + objectTableName + "\n" + "   objectCode=" + objectCode
               + "\n" + "   replacedBy=" + replacedBy + "\n"
               + "   deprecationReason=" + deprecationReason + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of Deprecation




