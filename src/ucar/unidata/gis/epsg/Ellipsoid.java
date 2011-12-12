
// $Id: Ellipsoid.java,v 1.8 2005/03/10 18:38:34 jeffmc Exp $

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

public class Ellipsoid extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/ellipsoid.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The ellipsoidCode property
     */
    private int ellipsoidCode;


    /**
     * The ellipsoidName property
     */
    private String ellipsoidName;


    /**
     * The semiMajorAxis property
     */
    private double semiMajorAxis;


    /**
     * The uomCode property
     */
    private int uomCode;


    /**
     * The invFlattening property
     */
    private double invFlattening;


    /**
     * The semiMinorAxis property
     */
    private double semiMinorAxis;


    /**
     * The ellipsoidShape property
     */
    private int ellipsoidShape;


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
     *       * @param arg_ellipsoidCode The ellipsoidCode argument
     * @param arg_ellipsoidName The ellipsoidName argument
     * @param arg_semiMajorAxis The semiMajorAxis argument
     * @param arg_uomCode The uomCode argument
     * @param arg_invFlattening The invFlattening argument
     * @param arg_semiMinorAxis The semiMinorAxis argument
     * @param arg_ellipsoidShape The ellipsoidShape argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public Ellipsoid(int arg_ellipsoidCode, String arg_ellipsoidName,
                     double arg_semiMajorAxis, int arg_uomCode,
                     double arg_invFlattening, double arg_semiMinorAxis,
                     int arg_ellipsoidShape, String arg_remarks,
                     String arg_informationSource, String arg_dataSource,
                     String arg_revisionDate, String arg_changeId,
                     String arg_deprecated) {
        this.ellipsoidCode     = arg_ellipsoidCode;
        this.ellipsoidName     = arg_ellipsoidName;
        this.semiMajorAxis     = arg_semiMajorAxis;
        this.uomCode           = arg_uomCode;
        this.invFlattening     = arg_invFlattening;
        this.semiMinorAxis     = arg_semiMinorAxis;
        this.ellipsoidShape    = arg_ellipsoidShape;
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
    public Ellipsoid(List tuple) {
        this.ellipsoidCode     = getInt((String) tuple.get(0));
        this.ellipsoidName     = (String) tuple.get(1);
        this.semiMajorAxis     = getDouble((String) tuple.get(2));
        this.uomCode           = getInt((String) tuple.get(3));
        this.invFlattening     = getDouble((String) tuple.get(4));
        this.semiMinorAxis     = getDouble((String) tuple.get(5));
        this.ellipsoidShape    = getInt((String) tuple.get(6));
        this.remarks           = (String) tuple.get(7);
        this.informationSource = (String) tuple.get(8);
        this.dataSource        = (String) tuple.get(9);
        this.revisionDate      = (String) tuple.get(10);
        this.changeId          = (String) tuple.get(11);
        this.deprecated        = (String) tuple.get(12);
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
                         Ellipsoid.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 13) {
                System.err.println("csv/ellipsoid.csv: line #" + i + " "
                                   + line);
                continue;
            }
            try {
                new Ellipsoid(line);
            } catch (Exception exc) {
                System.err.println("Error creating Ellipsoid " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the ellipsoidCode property.
     *
     * @return The ellipsoidCode property
     */
    public int getEllipsoidCode() {
        return ellipsoidCode;
    }


    /**
     * Return the ellipsoidName property.
     *
     * @return The ellipsoidName property
     */
    public String getEllipsoidName() {
        return ellipsoidName;
    }


    /**
     * Return the semiMajorAxis property.
     *
     * @return The semiMajorAxis property
     */
    public double getSemiMajorAxis() {
        return semiMajorAxis;
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
     * Return the invFlattening property.
     *
     * @return The invFlattening property
     */
    public double getInvFlattening() {
        return invFlattening;
    }


    /**
     * Return the semiMinorAxis property.
     *
     * @return The semiMinorAxis property
     */
    public double getSemiMinorAxis() {
        return semiMinorAxis;
    }


    /**
     * Return the ellipsoidShape property.
     *
     * @return The ellipsoidShape property
     */
    public int getEllipsoidShape() {
        return ellipsoidShape;
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
     * Find the Ellipsoid object with the ellipsoidCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Ellipsoid findEllipsoidCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            Ellipsoid obj = (Ellipsoid) members.get(i);
            if (obj.ellipsoidCode == value) {
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
        if (varname.equals("ellipsoidCode")) {
            return ellipsoidCode;
        }
        if (varname.equals("uomCode")) {
            return uomCode;
        }
        if (varname.equals("ellipsoidShape")) {
            return ellipsoidShape;
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
        if (varname.equals("semiMajorAxis")) {
            return semiMajorAxis;
        }
        if (varname.equals("invFlattening")) {
            return invFlattening;
        }
        if (varname.equals("semiMinorAxis")) {
            return semiMinorAxis;
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
        if (varname.equals("ellipsoidName")) {
            return ellipsoidName;
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
        return "" + "   ellipsoidCode=" + ellipsoidCode + "\n"
               + "   ellipsoidName=" + ellipsoidName + "\n"
               + "   semiMajorAxis=" + semiMajorAxis + "\n" + "   uomCode="
               + uomCode + "\n" + "   invFlattening=" + invFlattening + "\n"
               + "   semiMinorAxis=" + semiMinorAxis + "\n"
               + "   ellipsoidShape=" + ellipsoidShape + "\n" + "   remarks="
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



}  //End of Ellipsoid




