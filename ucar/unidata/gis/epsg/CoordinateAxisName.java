
// $Id: CoordinateAxisName.java,v 1.8 2005/03/10 18:38:32 jeffmc Exp $

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

public class CoordinateAxisName extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/coordinate_axis_name.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The coordAxisNameCode property
     */
    private int coordAxisNameCode;


    /**
     * The coordAxisName property
     */
    private String coordAxisName;


    /**
     * The description property
     */
    private String description;


    /**
     * The remarks property
     */
    private String remarks;


    /**
     * The informationSource property
     */
    private String informationSource;


    /**
     * The dataSource property
     */
    private String dataSource;


    /**
     * The revisionDate property
     */
    private String revisionDate;


    /**
     * The changeId property
     */
    private String changeId;


    /**
     * The deprecated property
     */
    private String deprecated;


    /**
     * The constructor
     *       * @param arg_coordAxisNameCode The coordAxisNameCode argument
     * @param arg_coordAxisName The coordAxisName argument
     * @param arg_description The description argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public CoordinateAxisName(int arg_coordAxisNameCode,
                              String arg_coordAxisName,
                              String arg_description, String arg_remarks,
                              String arg_informationSource,
                              String arg_dataSource, String arg_revisionDate,
                              String arg_changeId, String arg_deprecated) {
        this.coordAxisNameCode = arg_coordAxisNameCode;
        this.coordAxisName     = arg_coordAxisName;
        this.description       = arg_description;
        this.remarks           = arg_remarks;
        this.informationSource = arg_informationSource;
        this.dataSource        = arg_dataSource;
        this.revisionDate      = arg_revisionDate;
        this.changeId          = arg_changeId;
        this.deprecated        = arg_deprecated;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public CoordinateAxisName(List tuple) {
        this.coordAxisNameCode = getInt((String) tuple.get(0));
        this.coordAxisName     = (String) tuple.get(1);
        this.description       = (String) tuple.get(2);
        this.remarks           = (String) tuple.get(3);
        this.informationSource = (String) tuple.get(4);
        this.dataSource        = (String) tuple.get(5);
        this.revisionDate      = (String) tuple.get(6);
        this.changeId          = (String) tuple.get(7);
        this.deprecated        = (String) tuple.get(8);
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
                         CoordinateAxisName.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 9) {
                System.err.println("csv/coordinate_axis_name.csv: line #" + i
                                   + " " + line);
                continue;
            }
            try {
                new CoordinateAxisName(line);
            } catch (Exception exc) {
                System.err.println("Error creating CoordinateAxisName "
                                   + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the coordAxisNameCode property.
     *
     * @return The coordAxisNameCode property
     */
    public int getCoordAxisNameCode() {
        return coordAxisNameCode;
    }


    /**
     * Return the coordAxisName property.
     *
     * @return The coordAxisName property
     */
    public String getCoordAxisName() {
        return coordAxisName;
    }


    /**
     * Return the description property.
     *
     * @return The description property
     */
    public String getDescription() {
        return description;
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
     * Return the informationSource property.
     *
     * @return The informationSource property
     */
    public String getInformationSource() {
        return informationSource;
    }


    /**
     * Return the dataSource property.
     *
     * @return The dataSource property
     */
    public String getDataSource() {
        return dataSource;
    }


    /**
     * Return the revisionDate property.
     *
     * @return The revisionDate property
     */
    public String getRevisionDate() {
        return revisionDate;
    }


    /**
     * Return the changeId property.
     *
     * @return The changeId property
     */
    public String getChangeId() {
        return changeId;
    }


    /**
     * Return the deprecated property.
     *
     * @return The deprecated property
     */
    public String getDeprecated() {
        return deprecated;
    }


    /**
     * Find the CoordinateAxisName object with the coordAxisNameCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateAxisName findCoordAxisNameCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateAxisName obj = (CoordinateAxisName) members.get(i);
            if (obj.coordAxisNameCode == value) {
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
        if (varname.equals("coordAxisNameCode")) {
            return coordAxisNameCode;
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
        if (varname.equals("coordAxisName")) {
            return coordAxisName;
        }
        if (varname.equals("description")) {
            return description;
        }
        if (varname.equals("remarks")) {
            return remarks;
        }
        if (varname.equals("informationSource")) {
            return informationSource;
        }
        if (varname.equals("dataSource")) {
            return dataSource;
        }
        if (varname.equals("revisionDate")) {
            return revisionDate;
        }
        if (varname.equals("changeId")) {
            return changeId;
        }
        if (varname.equals("deprecated")) {
            return deprecated;
        }
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   coordAxisNameCode=" + coordAxisNameCode + "\n"
               + "   coordAxisName=" + coordAxisName + "\n"
               + "   description=" + description + "\n" + "   remarks="
               + remarks + "\n" + "   informationSource=" + informationSource
               + "\n" + "   dataSource=" + dataSource + "\n"
               + "   revisionDate=" + revisionDate + "\n" + "   changeId="
               + changeId + "\n" + "   deprecated=" + deprecated + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of CoordinateAxisName




