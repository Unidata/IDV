
// $Id: CoordinateReferenceSystem.java,v 1.8 2005/03/10 18:38:34 jeffmc Exp $

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

public class CoordinateReferenceSystem extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/coordinate_reference_system.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The coordRefSysCode property
     */
    private int coordRefSysCode;


    /**
     * The coordRefSysName property
     */
    private String coordRefSysName;


    /**
     * The areaOfUseCode property
     */
    private int areaOfUseCode;


    /**
     * The coordRefSysKind property
     */
    private String coordRefSysKind;


    /**
     * The coordSysCode property
     */
    private int coordSysCode;


    /**
     * The datumCode property
     */
    private int datumCode;


    /**
     * The sourceGeogcrsCode property
     */
    private int sourceGeogcrsCode;


    /**
     * The projectionConvCode property
     */
    private int projectionConvCode;


    /**
     * The cmpdHorizcrsCode property
     */
    private int cmpdHorizcrsCode;


    /**
     * The cmpdVertcrsCode property
     */
    private int cmpdVertcrsCode;


    /**
     * The crsScope property
     */
    private String crsScope;


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
     * The showCrs property
     */
    private String showCrs;


    /**
     * The deprecated property
     */
    private String deprecated;


    /**
     * The constructor
     *       * @param arg_coordRefSysCode The coordRefSysCode argument
     * @param arg_coordRefSysName The coordRefSysName argument
     * @param arg_areaOfUseCode The areaOfUseCode argument
     * @param arg_coordRefSysKind The coordRefSysKind argument
     * @param arg_coordSysCode The coordSysCode argument
     * @param arg_datumCode The datumCode argument
     * @param arg_sourceGeogcrsCode The sourceGeogcrsCode argument
     * @param arg_projectionConvCode The projectionConvCode argument
     * @param arg_cmpdHorizcrsCode The cmpdHorizcrsCode argument
     * @param arg_cmpdVertcrsCode The cmpdVertcrsCode argument
     * @param arg_crsScope The crsScope argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_showCrs The showCrs argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public CoordinateReferenceSystem(int arg_coordRefSysCode,
                                     String arg_coordRefSysName,
                                     int arg_areaOfUseCode,
                                     String arg_coordRefSysKind,
                                     int arg_coordSysCode, int arg_datumCode,
                                     int arg_sourceGeogcrsCode,
                                     int arg_projectionConvCode,
                                     int arg_cmpdHorizcrsCode,
                                     int arg_cmpdVertcrsCode,
                                     String arg_crsScope, String arg_remarks,
                                     String arg_informationSource,
                                     String arg_dataSource,
                                     String arg_revisionDate,
                                     String arg_changeId, String arg_showCrs,
                                     String arg_deprecated) {
        this.coordRefSysCode    = arg_coordRefSysCode;
        this.coordRefSysName    = arg_coordRefSysName;
        this.areaOfUseCode      = arg_areaOfUseCode;
        this.coordRefSysKind    = arg_coordRefSysKind;
        this.coordSysCode       = arg_coordSysCode;
        this.datumCode          = arg_datumCode;
        this.sourceGeogcrsCode  = arg_sourceGeogcrsCode;
        this.projectionConvCode = arg_projectionConvCode;
        this.cmpdHorizcrsCode   = arg_cmpdHorizcrsCode;
        this.cmpdVertcrsCode    = arg_cmpdVertcrsCode;
        this.crsScope           = arg_crsScope;
        this.remarks            = arg_remarks;
        this.informationSource  = arg_informationSource;
        this.dataSource         = arg_dataSource;
        this.revisionDate       = arg_revisionDate;
        this.changeId           = arg_changeId;
        this.showCrs            = arg_showCrs;
        this.deprecated         = arg_deprecated;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public CoordinateReferenceSystem(List tuple) {
        this.coordRefSysCode    = getInt((String) tuple.get(0));
        this.coordRefSysName    = (String) tuple.get(1);
        this.areaOfUseCode      = getInt((String) tuple.get(2));
        this.coordRefSysKind    = (String) tuple.get(3);
        this.coordSysCode       = getInt((String) tuple.get(4));
        this.datumCode          = getInt((String) tuple.get(5));
        this.sourceGeogcrsCode  = getInt((String) tuple.get(6));
        this.projectionConvCode = getInt((String) tuple.get(7));
        this.cmpdHorizcrsCode   = getInt((String) tuple.get(8));
        this.cmpdVertcrsCode    = getInt((String) tuple.get(9));
        this.crsScope           = (String) tuple.get(10);
        this.remarks            = (String) tuple.get(11);
        this.informationSource  = (String) tuple.get(12);
        this.dataSource         = (String) tuple.get(13);
        this.revisionDate       = (String) tuple.get(14);
        this.changeId           = (String) tuple.get(15);
        this.showCrs            = (String) tuple.get(16);
        this.deprecated         = (String) tuple.get(17);
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
                         CoordinateReferenceSystem.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 18) {
                System.err.println(
                    "csv/coordinate_reference_system.csv: line #" + i + " "
                    + line);
                continue;
            }
            try {
                new CoordinateReferenceSystem(line);
            } catch (Exception exc) {
                System.err.println("Error creating CoordinateReferenceSystem "
                                   + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the coordRefSysCode property.
     *
     * @return The coordRefSysCode property
     */
    public int getCoordRefSysCode() {
        return coordRefSysCode;
    }


    /**
     * Return the coordRefSysName property.
     *
     * @return The coordRefSysName property
     */
    public String getCoordRefSysName() {
        return coordRefSysName;
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
     * Return the coordRefSysKind property.
     *
     * @return The coordRefSysKind property
     */
    public String getCoordRefSysKind() {
        return coordRefSysKind;
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
     * Return the datumCode property.
     *
     * @return The datumCode property
     */
    public int getDatumCode() {
        return datumCode;
    }


    /**
     * Return the sourceGeogcrsCode property.
     *
     * @return The sourceGeogcrsCode property
     */
    public int getSourceGeogcrsCode() {
        return sourceGeogcrsCode;
    }


    /**
     * Return the projectionConvCode property.
     *
     * @return The projectionConvCode property
     */
    public int getProjectionConvCode() {
        return projectionConvCode;
    }


    /**
     * Return the cmpdHorizcrsCode property.
     *
     * @return The cmpdHorizcrsCode property
     */
    public int getCmpdHorizcrsCode() {
        return cmpdHorizcrsCode;
    }


    /**
     * Return the cmpdVertcrsCode property.
     *
     * @return The cmpdVertcrsCode property
     */
    public int getCmpdVertcrsCode() {
        return cmpdVertcrsCode;
    }


    /**
     * Return the crsScope property.
     *
     * @return The crsScope property
     */
    public String getCrsScope() {
        return crsScope;
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
     * Return the showCrs property.
     *
     * @return The showCrs property
     */
    public String getShowCrs() {
        return showCrs;
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
     * Find the CoordinateReferenceSystem object with the coordRefSysCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateReferenceSystem findCoordRefSysCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateReferenceSystem obj =
                (CoordinateReferenceSystem) members.get(i);
            if (obj.coordRefSysCode == value) {
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
        if (varname.equals("coordRefSysCode")) {
            return coordRefSysCode;
        }
        if (varname.equals("areaOfUseCode")) {
            return areaOfUseCode;
        }
        if (varname.equals("coordSysCode")) {
            return coordSysCode;
        }
        if (varname.equals("datumCode")) {
            return datumCode;
        }
        if (varname.equals("sourceGeogcrsCode")) {
            return sourceGeogcrsCode;
        }
        if (varname.equals("projectionConvCode")) {
            return projectionConvCode;
        }
        if (varname.equals("cmpdHorizcrsCode")) {
            return cmpdHorizcrsCode;
        }
        if (varname.equals("cmpdVertcrsCode")) {
            return cmpdVertcrsCode;
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
        if (varname.equals("coordRefSysName")) {
            return coordRefSysName;
        }
        if (varname.equals("coordRefSysKind")) {
            return coordRefSysKind;
        }
        if (varname.equals("crsScope")) {
            return crsScope;
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
        if (varname.equals("showCrs")) {
            return showCrs;
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
        return "" + "   coordRefSysCode=" + coordRefSysCode + "\n"
               + "   coordRefSysName=" + coordRefSysName + "\n"
               + "   areaOfUseCode=" + areaOfUseCode + "\n"
               + "   coordRefSysKind=" + coordRefSysKind + "\n"
               + "   coordSysCode=" + coordSysCode + "\n" + "   datumCode="
               + datumCode + "\n" + "   sourceGeogcrsCode="
               + sourceGeogcrsCode + "\n" + "   projectionConvCode="
               + projectionConvCode + "\n" + "   cmpdHorizcrsCode="
               + cmpdHorizcrsCode + "\n" + "   cmpdVertcrsCode="
               + cmpdVertcrsCode + "\n" + "   crsScope=" + crsScope + "\n"
               + "   remarks=" + remarks + "\n" + "   informationSource="
               + informationSource + "\n" + "   dataSource=" + dataSource
               + "\n" + "   revisionDate=" + revisionDate + "\n"
               + "   changeId=" + changeId + "\n" + "   showCrs=" + showCrs
               + "\n" + "   deprecated=" + deprecated + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of CoordinateReferenceSystem




