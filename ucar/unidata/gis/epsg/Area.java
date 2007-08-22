
// $Id: Area.java,v 1.8 2005/03/10 18:38:32 jeffmc Exp $

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

public class Area extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/area.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The areaCode property
     */
    private int areaCode;


    /**
     * The areaName property
     */
    private String areaName;


    /**
     * The areaOfUse property
     */
    private String areaOfUse;


    /**
     * The isoA2Code property
     */
    private String isoA2Code;


    /**
     * The isoA3Code property
     */
    private String isoA3Code;


    /**
     * The isoNCode property
     */
    private String isoNCode;


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
     *       * @param arg_areaCode The areaCode argument
     * @param arg_areaName The areaName argument
     * @param arg_areaOfUse The areaOfUse argument
     * @param arg_isoA2Code The isoA2Code argument
     * @param arg_isoA3Code The isoA3Code argument
     * @param arg_isoNCode The isoNCode argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public Area(int arg_areaCode, String arg_areaName, String arg_areaOfUse,
                String arg_isoA2Code, String arg_isoA3Code,
                String arg_isoNCode, String arg_remarks,
                String arg_informationSource, String arg_dataSource,
                String arg_revisionDate, String arg_changeId,
                String arg_deprecated) {
        this.areaCode          = arg_areaCode;
        this.areaName          = arg_areaName;
        this.areaOfUse         = arg_areaOfUse;
        this.isoA2Code         = arg_isoA2Code;
        this.isoA3Code         = arg_isoA3Code;
        this.isoNCode          = arg_isoNCode;
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
    public Area(List tuple) {
        this.areaCode          = getInt((String) tuple.get(0));
        this.areaName          = (String) tuple.get(1);
        this.areaOfUse         = (String) tuple.get(2);
        this.isoA2Code         = (String) tuple.get(3);
        this.isoA3Code         = (String) tuple.get(4);
        this.isoNCode          = (String) tuple.get(5);
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
                         Area.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 12) {
                System.err.println("csv/area.csv: line #" + i + " " + line);
                continue;
            }
            try {
                new Area(line);
            } catch (Exception exc) {
                System.err.println("Error creating Area " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the areaCode property.
     *
     * @return The areaCode property
     */
    public int getAreaCode() {
        return areaCode;
    }


    /**
     * Return the areaName property.
     *
     * @return The areaName property
     */
    public String getAreaName() {
        return areaName;
    }


    /**
     * Return the areaOfUse property.
     *
     * @return The areaOfUse property
     */
    public String getAreaOfUse() {
        return areaOfUse;
    }


    /**
     * Return the isoA2Code property.
     *
     * @return The isoA2Code property
     */
    public String getIsoA2Code() {
        return isoA2Code;
    }


    /**
     * Return the isoA3Code property.
     *
     * @return The isoA3Code property
     */
    public String getIsoA3Code() {
        return isoA3Code;
    }


    /**
     * Return the isoNCode property.
     *
     * @return The isoNCode property
     */
    public String getIsoNCode() {
        return isoNCode;
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
     * Find the Area object with the areaCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Area findAreaCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            Area obj = (Area) members.get(i);
            if (obj.areaCode == value) {
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
        if (varname.equals("areaCode")) {
            return areaCode;
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
        if (varname.equals("areaName")) {
            return areaName;
        }
        if (varname.equals("areaOfUse")) {
            return areaOfUse;
        }
        if (varname.equals("isoA2Code")) {
            return isoA2Code;
        }
        if (varname.equals("isoA3Code")) {
            return isoA3Code;
        }
        if (varname.equals("isoNCode")) {
            return isoNCode;
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
        return "" + "   areaCode=" + areaCode + "\n" + "   areaName="
               + areaName + "\n" + "   areaOfUse=" + areaOfUse + "\n"
               + "   isoA2Code=" + isoA2Code + "\n" + "   isoA3Code="
               + isoA3Code + "\n" + "   isoNCode=" + isoNCode + "\n"
               + "   remarks=" + remarks + "\n" + "   informationSource="
               + informationSource + "\n" + "   dataSource=" + dataSource
               + "\n" + "   revisionDate=" + revisionDate + "\n"
               + "   changeId=" + changeId + "\n" + "   deprecated="
               + deprecated + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of Area




