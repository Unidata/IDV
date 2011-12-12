
// $Id: CoordinateSystem.java,v 1.8 2005/03/10 18:38:34 jeffmc Exp $

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

public class CoordinateSystem extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/coordinate_system.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The coordSysCode property
     */
    private int coordSysCode;


    /**
     * The coordSysName property
     */
    private String coordSysName;


    /**
     * The coordSysType property
     */
    private String coordSysType;


    /**
     * The dimension property
     */
    private String dimension;


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
     *       * @param arg_coordSysCode The coordSysCode argument
     * @param arg_coordSysName The coordSysName argument
     * @param arg_coordSysType The coordSysType argument
     * @param arg_dimension The dimension argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public CoordinateSystem(int arg_coordSysCode, String arg_coordSysName,
                            String arg_coordSysType, String arg_dimension,
                            String arg_remarks, String arg_informationSource,
                            String arg_dataSource, String arg_revisionDate,
                            String arg_changeId, String arg_deprecated) {
        this.coordSysCode      = arg_coordSysCode;
        this.coordSysName      = arg_coordSysName;
        this.coordSysType      = arg_coordSysType;
        this.dimension         = arg_dimension;
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
    public CoordinateSystem(List tuple) {
        this.coordSysCode      = getInt((String) tuple.get(0));
        this.coordSysName      = (String) tuple.get(1);
        this.coordSysType      = (String) tuple.get(2);
        this.dimension         = (String) tuple.get(3);
        this.remarks           = (String) tuple.get(4);
        this.informationSource = (String) tuple.get(5);
        this.dataSource        = (String) tuple.get(6);
        this.revisionDate      = (String) tuple.get(7);
        this.changeId          = (String) tuple.get(8);
        this.deprecated        = (String) tuple.get(9);
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
                         CoordinateSystem.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 10) {
                System.err.println("csv/coordinate_system.csv: line #" + i
                                   + " " + line);
                continue;
            }
            try {
                new CoordinateSystem(line);
            } catch (Exception exc) {
                System.err.println("Error creating CoordinateSystem " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the coordSysCode property.
     *
     * @return The coordSysCode property
     */
    public int getCoordSysCode() {
        return coordSysCode;
    }


    /**
     * Return the coordSysName property.
     *
     * @return The coordSysName property
     */
    public String getCoordSysName() {
        return coordSysName;
    }


    /**
     * Return the coordSysType property.
     *
     * @return The coordSysType property
     */
    public String getCoordSysType() {
        return coordSysType;
    }


    /**
     * Return the dimension property.
     *
     * @return The dimension property
     */
    public String getDimension() {
        return dimension;
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
     * Find the CoordinateSystem object with the coordSysCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateSystem findCoordSysCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateSystem obj = (CoordinateSystem) members.get(i);
            if (obj.coordSysCode == value) {
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
        if (varname.equals("coordSysCode")) {
            return coordSysCode;
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
        if (varname.equals("coordSysName")) {
            return coordSysName;
        }
        if (varname.equals("coordSysType")) {
            return coordSysType;
        }
        if (varname.equals("dimension")) {
            return dimension;
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
        return "" + "   coordSysCode=" + coordSysCode + "\n"
               + "   coordSysName=" + coordSysName + "\n"
               + "   coordSysType=" + coordSysType + "\n" + "   dimension="
               + dimension + "\n" + "   remarks=" + remarks + "\n"
               + "   informationSource=" + informationSource + "\n"
               + "   dataSource=" + dataSource + "\n" + "   revisionDate="
               + revisionDate + "\n" + "   changeId=" + changeId + "\n"
               + "   deprecated=" + deprecated + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of CoordinateSystem




