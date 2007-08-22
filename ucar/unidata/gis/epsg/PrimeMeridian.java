
// $Id: PrimeMeridian.java,v 1.8 2005/03/10 18:38:36 jeffmc Exp $

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

public class PrimeMeridian extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/prime_meridian.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The primeMeridianCode property
     */
    private int primeMeridianCode;


    /**
     * The primeMeridianName property
     */
    private String primeMeridianName;


    /**
     * The greenwichLongitude property
     */
    private double greenwichLongitude;


    /**
     * The uomCode property
     */
    private int uomCode;


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
    private double changeId;


    /**
     * The deprecated property
     */
    private int deprecated;


    /**
     * The constructor
     *       * @param arg_primeMeridianCode The primeMeridianCode argument
     * @param arg_primeMeridianName The primeMeridianName argument
     * @param arg_greenwichLongitude The greenwichLongitude argument
     * @param arg_uomCode The uomCode argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public PrimeMeridian(int arg_primeMeridianCode,
                         String arg_primeMeridianName,
                         double arg_greenwichLongitude, int arg_uomCode,
                         String arg_remarks, String arg_informationSource,
                         String arg_dataSource, String arg_revisionDate,
                         double arg_changeId, int arg_deprecated) {
        this.primeMeridianCode  = arg_primeMeridianCode;
        this.primeMeridianName  = arg_primeMeridianName;
        this.greenwichLongitude = arg_greenwichLongitude;
        this.uomCode            = arg_uomCode;
        this.remarks            = arg_remarks;
        this.informationSource  = arg_informationSource;
        this.dataSource         = arg_dataSource;
        this.revisionDate       = arg_revisionDate;
        this.changeId           = arg_changeId;
        this.deprecated         = arg_deprecated;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public PrimeMeridian(List tuple) {
        this.primeMeridianCode  = getInt((String) tuple.get(0));
        this.primeMeridianName  = (String) tuple.get(1);
        this.greenwichLongitude = getDouble((String) tuple.get(2));
        this.uomCode            = getInt((String) tuple.get(3));
        this.remarks            = (String) tuple.get(4);
        this.informationSource  = (String) tuple.get(5);
        this.dataSource         = (String) tuple.get(6);
        this.revisionDate       = (String) tuple.get(7);
        this.changeId           = getDouble((String) tuple.get(8));
        this.deprecated         = getInt((String) tuple.get(9));
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
                         PrimeMeridian.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 10) {
                System.err.println("csv/prime_meridian.csv: line #" + i + " "
                                   + line);
                continue;
            }
            try {
                new PrimeMeridian(line);
            } catch (Exception exc) {
                System.err.println("Error creating PrimeMeridian " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the primeMeridianCode property.
     *
     * @return The primeMeridianCode property
     */
    public int getPrimeMeridianCode() {
        return primeMeridianCode;
    }


    /**
     * Return the primeMeridianName property.
     *
     * @return The primeMeridianName property
     */
    public String getPrimeMeridianName() {
        return primeMeridianName;
    }


    /**
     * Return the greenwichLongitude property.
     *
     * @return The greenwichLongitude property
     */
    public double getGreenwichLongitude() {
        return greenwichLongitude;
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
    public double getChangeId() {
        return changeId;
    }


    /**
     * Return the deprecated property.
     *
     * @return The deprecated property
     */
    public int getDeprecated() {
        return deprecated;
    }


    /**
     * Find the PrimeMeridian object with the primeMeridianCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static PrimeMeridian findPrimeMeridianCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            PrimeMeridian obj = (PrimeMeridian) members.get(i);
            if (obj.primeMeridianCode == value) {
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
        if (varname.equals("primeMeridianCode")) {
            return primeMeridianCode;
        }
        if (varname.equals("uomCode")) {
            return uomCode;
        }
        if (varname.equals("deprecated")) {
            return deprecated;
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
        if (varname.equals("greenwichLongitude")) {
            return greenwichLongitude;
        }
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
        if (varname.equals("primeMeridianName")) {
            return primeMeridianName;
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
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   primeMeridianCode=" + primeMeridianCode + "\n"
               + "   primeMeridianName=" + primeMeridianName + "\n"
               + "   greenwichLongitude=" + greenwichLongitude + "\n"
               + "   uomCode=" + uomCode + "\n" + "   remarks=" + remarks
               + "\n" + "   informationSource=" + informationSource + "\n"
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



}  //End of PrimeMeridian




