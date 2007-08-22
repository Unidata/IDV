
// $Id: Datum.java,v 1.8 2005/03/10 18:38:34 jeffmc Exp $

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

public class Datum extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/datum.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The datumCode property
     */
    private int datumCode;


    /**
     * The datumName property
     */
    private String datumName;


    /**
     * The datumType property
     */
    private String datumType;


    /**
     * The originDescription property
     */
    private String originDescription;


    /**
     * The realizationEpoch property
     */
    private String realizationEpoch;


    /**
     * The ellipsoidCode property
     */
    private String ellipsoidCode;


    /**
     * The primeMeridianCode property
     */
    private int primeMeridianCode;


    /**
     * The areaOfUseCode property
     */
    private int areaOfUseCode;


    /**
     * The datumScope property
     */
    private String datumScope;


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
     *       * @param arg_datumCode The datumCode argument
     * @param arg_datumName The datumName argument
     * @param arg_datumType The datumType argument
     * @param arg_originDescription The originDescription argument
     * @param arg_realizationEpoch The realizationEpoch argument
     * @param arg_ellipsoidCode The ellipsoidCode argument
     * @param arg_primeMeridianCode The primeMeridianCode argument
     * @param arg_areaOfUseCode The areaOfUseCode argument
     * @param arg_datumScope The datumScope argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public Datum(int arg_datumCode, String arg_datumName,
                 String arg_datumType, String arg_originDescription,
                 String arg_realizationEpoch, String arg_ellipsoidCode,
                 int arg_primeMeridianCode, int arg_areaOfUseCode,
                 String arg_datumScope, String arg_remarks,
                 String arg_informationSource, String arg_dataSource,
                 String arg_revisionDate, String arg_changeId,
                 String arg_deprecated) {
        this.datumCode         = arg_datumCode;
        this.datumName         = arg_datumName;
        this.datumType         = arg_datumType;
        this.originDescription = arg_originDescription;
        this.realizationEpoch  = arg_realizationEpoch;
        this.ellipsoidCode     = arg_ellipsoidCode;
        this.primeMeridianCode = arg_primeMeridianCode;
        this.areaOfUseCode     = arg_areaOfUseCode;
        this.datumScope        = arg_datumScope;
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
    public Datum(List tuple) {
        this.datumCode         = getInt((String) tuple.get(0));
        this.datumName         = (String) tuple.get(1);
        this.datumType         = (String) tuple.get(2);
        this.originDescription = (String) tuple.get(3);
        this.realizationEpoch  = (String) tuple.get(4);
        this.ellipsoidCode     = (String) tuple.get(5);
        this.primeMeridianCode = getInt((String) tuple.get(6));
        this.areaOfUseCode     = getInt((String) tuple.get(7));
        this.datumScope        = (String) tuple.get(8);
        this.remarks           = (String) tuple.get(9);
        this.informationSource = (String) tuple.get(10);
        this.dataSource        = (String) tuple.get(11);
        this.revisionDate      = (String) tuple.get(12);
        this.changeId          = (String) tuple.get(13);
        this.deprecated        = (String) tuple.get(14);
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
                         Datum.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 15) {
                System.err.println("csv/datum.csv: line #" + i + " " + line);
                continue;
            }
            try {
                new Datum(line);
            } catch (Exception exc) {
                System.err.println("Error creating Datum " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the datumCode property.
     *
     * @return The datumCode property
     */
    public int getDatumCode() {
        return datumCode;
    }


    /**
     * Return the datumName property.
     *
     * @return The datumName property
     */
    public String getDatumName() {
        return datumName;
    }


    /**
     * Return the datumType property.
     *
     * @return The datumType property
     */
    public String getDatumType() {
        return datumType;
    }


    /**
     * Return the originDescription property.
     *
     * @return The originDescription property
     */
    public String getOriginDescription() {
        return originDescription;
    }


    /**
     * Return the realizationEpoch property.
     *
     * @return The realizationEpoch property
     */
    public String getRealizationEpoch() {
        return realizationEpoch;
    }


    /**
     * Return the ellipsoidCode property.
     *
     * @return The ellipsoidCode property
     */
    public String getEllipsoidCode() {
        return ellipsoidCode;
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
     * Return the areaOfUseCode property.
     *
     * @return The areaOfUseCode property
     */
    public int getAreaOfUseCode() {
        return areaOfUseCode;
    }


    /**
     * Return the datumScope property.
     *
     * @return The datumScope property
     */
    public String getDatumScope() {
        return datumScope;
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
     * Find the Datum object with the datumCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Datum findDatumCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            Datum obj = (Datum) members.get(i);
            if (obj.datumCode == value) {
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
        if (varname.equals("datumCode")) {
            return datumCode;
        }
        if (varname.equals("primeMeridianCode")) {
            return primeMeridianCode;
        }
        if (varname.equals("areaOfUseCode")) {
            return areaOfUseCode;
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
        if (varname.equals("datumName")) {
            return datumName;
        }
        if (varname.equals("datumType")) {
            return datumType;
        }
        if (varname.equals("originDescription")) {
            return originDescription;
        }
        if (varname.equals("realizationEpoch")) {
            return realizationEpoch;
        }
        if (varname.equals("ellipsoidCode")) {
            return ellipsoidCode;
        }
        if (varname.equals("datumScope")) {
            return datumScope;
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
        return "" + "   datumCode=" + datumCode + "\n" + "   datumName="
               + datumName + "\n" + "   datumType=" + datumType + "\n"
               + "   originDescription=" + originDescription + "\n"
               + "   realizationEpoch=" + realizationEpoch + "\n"
               + "   ellipsoidCode=" + ellipsoidCode + "\n"
               + "   primeMeridianCode=" + primeMeridianCode + "\n"
               + "   areaOfUseCode=" + areaOfUseCode + "\n"
               + "   datumScope=" + datumScope + "\n" + "   remarks="
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



}  //End of Datum




