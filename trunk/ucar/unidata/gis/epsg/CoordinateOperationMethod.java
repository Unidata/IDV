
// $Id: CoordinateOperationMethod.java,v 1.8 2005/03/10 18:38:33 jeffmc Exp $

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

public class CoordinateOperationMethod extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/coordinate_operation_method.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The coordOpMethodCode property
     */
    private int coordOpMethodCode;


    /**
     * The coordOpMethodName property
     */
    private String coordOpMethodName;


    /**
     * The reverseOp property
     */
    private int reverseOp;


    /**
     * The formula property
     */
    private String formula;


    /**
     * The example property
     */
    private String example;


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
     *       * @param arg_coordOpMethodCode The coordOpMethodCode argument
     * @param arg_coordOpMethodName The coordOpMethodName argument
     * @param arg_reverseOp The reverseOp argument
     * @param arg_formula The formula argument
     * @param arg_example The example argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public CoordinateOperationMethod(int arg_coordOpMethodCode,
                                     String arg_coordOpMethodName,
                                     int arg_reverseOp, String arg_formula,
                                     String arg_example, String arg_remarks,
                                     String arg_informationSource,
                                     String arg_dataSource,
                                     String arg_revisionDate,
                                     String arg_changeId,
                                     String arg_deprecated) {
        this.coordOpMethodCode = arg_coordOpMethodCode;
        this.coordOpMethodName = arg_coordOpMethodName;
        this.reverseOp         = arg_reverseOp;
        this.formula           = arg_formula;
        this.example           = arg_example;
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
    public CoordinateOperationMethod(List tuple) {
        this.coordOpMethodCode = getInt((String) tuple.get(0));
        this.coordOpMethodName = (String) tuple.get(1);
        this.reverseOp         = getInt((String) tuple.get(2));
        this.formula           = (String) tuple.get(3);
        this.example           = (String) tuple.get(4);
        this.remarks           = (String) tuple.get(5);
        this.informationSource = (String) tuple.get(6);
        this.dataSource        = (String) tuple.get(7);
        this.revisionDate      = (String) tuple.get(8);
        this.changeId          = (String) tuple.get(9);
        this.deprecated        = (String) tuple.get(10);
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
                         CoordinateOperationMethod.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 11) {
                System.err.println(
                    "csv/coordinate_operation_method.csv: line #" + i + " "
                    + line);
                continue;
            }
            try {
                new CoordinateOperationMethod(line);
            } catch (Exception exc) {
                System.err.println("Error creating CoordinateOperationMethod "
                                   + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the coordOpMethodCode property.
     *
     * @return The coordOpMethodCode property
     */
    public int getCoordOpMethodCode() {
        return coordOpMethodCode;
    }


    /**
     * Return the coordOpMethodName property.
     *
     * @return The coordOpMethodName property
     */
    public String getCoordOpMethodName() {
        return coordOpMethodName;
    }


    /**
     * Return the reverseOp property.
     *
     * @return The reverseOp property
     */
    public int getReverseOp() {
        return reverseOp;
    }


    /**
     * Return the formula property.
     *
     * @return The formula property
     */
    public String getFormula() {
        return formula;
    }


    /**
     * Return the example property.
     *
     * @return The example property
     */
    public String getExample() {
        return example;
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
     * Find all of the CoordinateOperationMethod objects with
     * the coordOpMethodCode value == the given value
     *
     *
     * @param value _more_
     * @return The found objects
     */
    public static List findAllCoordOpMethodCode(int value) {
        List results = new ArrayList();
        for (int i = 0; i < members.size(); i++) {
            CoordinateOperationMethod obj =
                (CoordinateOperationMethod) members.get(i);
            if (obj.coordOpMethodCode == value) {
                results.add(obj);
            }
        }
        return results;
    }

    /**
     * Find the CoordinateOperationMethod object with the coordOpMethodCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateOperationMethod findCoordOpMethodCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateOperationMethod obj =
                (CoordinateOperationMethod) members.get(i);
            if (obj.coordOpMethodCode == value) {
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
        if (varname.equals("coordOpMethodCode")) {
            return coordOpMethodCode;
        }
        if (varname.equals("reverseOp")) {
            return reverseOp;
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
        if (varname.equals("coordOpMethodName")) {
            return coordOpMethodName;
        }
        if (varname.equals("formula")) {
            return formula;
        }
        if (varname.equals("example")) {
            return example;
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
        return "" + "   coordOpMethodCode=" + coordOpMethodCode + "\n"
               + "   coordOpMethodName=" + coordOpMethodName + "\n"
               + "   reverseOp=" + reverseOp + "\n" + "   formula=" + formula
               + "\n" + "   example=" + example + "\n" + "   remarks="
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



}  //End of CoordinateOperationMethod




