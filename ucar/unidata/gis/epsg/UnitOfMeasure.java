
// $Id: UnitOfMeasure.java,v 1.8 2005/03/10 18:38:36 jeffmc Exp $

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

public class UnitOfMeasure extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/unit_of_measure.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The uomCode property
     */
    private int uomCode;


    /**
     * The unitOfMeasName property
     */
    private String unitOfMeasName;


    /**
     * The unitOfMeasType property
     */
    private String unitOfMeasType;


    /**
     * The targetUomCode property
     */
    private int targetUomCode;


    /**
     * The factorB property
     */
    private double factorB;


    /**
     * The factorC property
     */
    private double factorC;


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
     *       * @param arg_uomCode The uomCode argument
     * @param arg_unitOfMeasName The unitOfMeasName argument
     * @param arg_unitOfMeasType The unitOfMeasType argument
     * @param arg_targetUomCode The targetUomCode argument
     * @param arg_factorB The factorB argument
     * @param arg_factorC The factorC argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public UnitOfMeasure(int arg_uomCode, String arg_unitOfMeasName,
                         String arg_unitOfMeasType, int arg_targetUomCode,
                         double arg_factorB, double arg_factorC,
                         String arg_remarks, String arg_informationSource,
                         String arg_dataSource, String arg_revisionDate,
                         String arg_changeId, String arg_deprecated) {
        this.uomCode           = arg_uomCode;
        this.unitOfMeasName    = arg_unitOfMeasName;
        this.unitOfMeasType    = arg_unitOfMeasType;
        this.targetUomCode     = arg_targetUomCode;
        this.factorB           = arg_factorB;
        this.factorC           = arg_factorC;
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
    public UnitOfMeasure(List tuple) {
        this.uomCode           = getInt((String) tuple.get(0));
        this.unitOfMeasName    = (String) tuple.get(1);
        this.unitOfMeasType    = (String) tuple.get(2);
        this.targetUomCode     = getInt((String) tuple.get(3));
        this.factorB           = getDouble((String) tuple.get(4));
        this.factorC           = getDouble((String) tuple.get(5));
        this.remarks           = (String) tuple.get(6);
        this.informationSource = (String) tuple.get(7);
        this.dataSource        = (String) tuple.get(8);
        this.revisionDate      = (String) tuple.get(9);
        this.changeId          = (String) tuple.get(10);
        this.deprecated        = (String) tuple.get(11);
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
                         UnitOfMeasure.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 12) {
                System.err.println("csv/unit_of_measure.csv: line #" + i
                                   + " " + line);
                continue;
            }
            try {
                new UnitOfMeasure(line);
            } catch (Exception exc) {
                System.err.println("Error creating UnitOfMeasure " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the uomCode property.
     *
     * @return The uomCode property
     */
    public int getUomCode() {
        return uomCode;
    }


    /**
     * Return the unitOfMeasName property.
     *
     * @return The unitOfMeasName property
     */
    public String getUnitOfMeasName() {
        return unitOfMeasName;
    }


    /**
     * Return the unitOfMeasType property.
     *
     * @return The unitOfMeasType property
     */
    public String getUnitOfMeasType() {
        return unitOfMeasType;
    }


    /**
     * Return the targetUomCode property.
     *
     * @return The targetUomCode property
     */
    public int getTargetUomCode() {
        return targetUomCode;
    }


    /**
     * Return the factorB property.
     *
     * @return The factorB property
     */
    public double getFactorB() {
        return factorB;
    }


    /**
     * Return the factorC property.
     *
     * @return The factorC property
     */
    public double getFactorC() {
        return factorC;
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
     * Find all of the UnitOfMeasure objects with
     * the uomCode value == the given value
     *
     *
     * @param value _more_
     * @return The found objects
     */
    public static List findAllUomCode(int value) {
        List results = new ArrayList();
        for (int i = 0; i < members.size(); i++) {
            UnitOfMeasure obj = (UnitOfMeasure) members.get(i);
            if (obj.uomCode == value) {
                results.add(obj);
            }
        }
        return results;
    }

    /**
     * Find the UnitOfMeasure object with the uomCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static UnitOfMeasure findUomCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            UnitOfMeasure obj = (UnitOfMeasure) members.get(i);
            if (obj.uomCode == value) {
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
        if (varname.equals("uomCode")) {
            return uomCode;
        }
        if (varname.equals("targetUomCode")) {
            return targetUomCode;
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
        if (varname.equals("factorB")) {
            return factorB;
        }
        if (varname.equals("factorC")) {
            return factorC;
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
        if (varname.equals("unitOfMeasName")) {
            return unitOfMeasName;
        }
        if (varname.equals("unitOfMeasType")) {
            return unitOfMeasType;
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
        return "" + "   uomCode=" + uomCode + "\n" + "   unitOfMeasName="
               + unitOfMeasName + "\n" + "   unitOfMeasType="
               + unitOfMeasType + "\n" + "   targetUomCode=" + targetUomCode
               + "\n" + "   factorB=" + factorB + "\n" + "   factorC="
               + factorC + "\n" + "   remarks=" + remarks + "\n"
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



}  //End of UnitOfMeasure




