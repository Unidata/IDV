
// $Id: CoordinateOperation.java,v 1.8 2005/03/10 18:38:33 jeffmc Exp $

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

public class CoordinateOperation extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/coordinate_operation.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The coordOpCode property
     */
    private int coordOpCode;


    /**
     * The coordOpName property
     */
    private String coordOpName;


    /**
     * The coordOpType property
     */
    private String coordOpType;


    /**
     * The sourceCrsCode property
     */
    private int sourceCrsCode;


    /**
     * The targetCrsCode property
     */
    private int targetCrsCode;


    /**
     * The coordTfmVersion property
     */
    private String coordTfmVersion;


    /**
     * The coordOpVariant property
     */
    private int coordOpVariant;


    /**
     * The areaOfUseCode property
     */
    private int areaOfUseCode;


    /**
     * The coordOpScope property
     */
    private String coordOpScope;


    /**
     * The coordOpMethodCode property
     */
    private String coordOpMethodCode;


    /**
     * The uomCodeSourceOffsets property
     */
    private String uomCodeSourceOffsets;


    /**
     * The uomCodeTargetOffsets property
     */
    private String uomCodeTargetOffsets;


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
     * The showOperation property
     */
    private String showOperation;


    /**
     * The deprecated property
     */
    private String deprecated;


    /**
     * The constructor
     *       * @param arg_coordOpCode The coordOpCode argument
     * @param arg_coordOpName The coordOpName argument
     * @param arg_coordOpType The coordOpType argument
     * @param arg_sourceCrsCode The sourceCrsCode argument
     * @param arg_targetCrsCode The targetCrsCode argument
     * @param arg_coordTfmVersion The coordTfmVersion argument
     * @param arg_coordOpVariant The coordOpVariant argument
     * @param arg_areaOfUseCode The areaOfUseCode argument
     * @param arg_coordOpScope The coordOpScope argument
     * @param arg_coordOpMethodCode The coordOpMethodCode argument
     * @param arg_uomCodeSourceOffsets The uomCodeSourceOffsets argument
     * @param arg_uomCodeTargetOffsets The uomCodeTargetOffsets argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_showOperation The showOperation argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public CoordinateOperation(int arg_coordOpCode, String arg_coordOpName,
                               String arg_coordOpType, int arg_sourceCrsCode,
                               int arg_targetCrsCode,
                               String arg_coordTfmVersion,
                               int arg_coordOpVariant, int arg_areaOfUseCode,
                               String arg_coordOpScope,
                               String arg_coordOpMethodCode,
                               String arg_uomCodeSourceOffsets,
                               String arg_uomCodeTargetOffsets,
                               String arg_remarks,
                               String arg_informationSource,
                               String arg_dataSource,
                               String arg_revisionDate, String arg_changeId,
                               String arg_showOperation,
                               String arg_deprecated) {
        this.coordOpCode          = arg_coordOpCode;
        this.coordOpName          = arg_coordOpName;
        this.coordOpType          = arg_coordOpType;
        this.sourceCrsCode        = arg_sourceCrsCode;
        this.targetCrsCode        = arg_targetCrsCode;
        this.coordTfmVersion      = arg_coordTfmVersion;
        this.coordOpVariant       = arg_coordOpVariant;
        this.areaOfUseCode        = arg_areaOfUseCode;
        this.coordOpScope         = arg_coordOpScope;
        this.coordOpMethodCode    = arg_coordOpMethodCode;
        this.uomCodeSourceOffsets = arg_uomCodeSourceOffsets;
        this.uomCodeTargetOffsets = arg_uomCodeTargetOffsets;
        this.remarks              = arg_remarks;
        this.informationSource    = arg_informationSource;
        this.dataSource           = arg_dataSource;
        this.revisionDate         = arg_revisionDate;
        this.changeId             = arg_changeId;
        this.showOperation        = arg_showOperation;
        this.deprecated           = arg_deprecated;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public CoordinateOperation(List tuple) {
        this.coordOpCode          = getInt((String) tuple.get(0));
        this.coordOpName          = (String) tuple.get(1);
        this.coordOpType          = (String) tuple.get(2);
        this.sourceCrsCode        = getInt((String) tuple.get(3));
        this.targetCrsCode        = getInt((String) tuple.get(4));
        this.coordTfmVersion      = (String) tuple.get(5);
        this.coordOpVariant       = getInt((String) tuple.get(6));
        this.areaOfUseCode        = getInt((String) tuple.get(7));
        this.coordOpScope         = (String) tuple.get(8);
        this.coordOpMethodCode    = (String) tuple.get(9);
        this.uomCodeSourceOffsets = (String) tuple.get(10);
        this.uomCodeTargetOffsets = (String) tuple.get(11);
        this.remarks              = (String) tuple.get(12);
        this.informationSource    = (String) tuple.get(13);
        this.dataSource           = (String) tuple.get(14);
        this.revisionDate         = (String) tuple.get(15);
        this.changeId             = (String) tuple.get(16);
        this.showOperation        = (String) tuple.get(17);
        this.deprecated           = (String) tuple.get(18);
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
                         CoordinateOperation.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 19) {
                System.err.println("csv/coordinate_operation.csv: line #" + i
                                   + " " + line);
                continue;
            }
            try {
                new CoordinateOperation(line);
            } catch (Exception exc) {
                System.err.println("Error creating CoordinateOperation "
                                   + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the coordOpCode property.
     *
     * @return The coordOpCode property
     */
    public int getCoordOpCode() {
        return coordOpCode;
    }


    /**
     * Return the coordOpName property.
     *
     * @return The coordOpName property
     */
    public String getCoordOpName() {
        return coordOpName;
    }


    /**
     * Return the coordOpType property.
     *
     * @return The coordOpType property
     */
    public String getCoordOpType() {
        return coordOpType;
    }


    /**
     * Return the sourceCrsCode property.
     *
     * @return The sourceCrsCode property
     */
    public int getSourceCrsCode() {
        return sourceCrsCode;
    }


    /**
     * Return the targetCrsCode property.
     *
     * @return The targetCrsCode property
     */
    public int getTargetCrsCode() {
        return targetCrsCode;
    }


    /**
     * Return the coordTfmVersion property.
     *
     * @return The coordTfmVersion property
     */
    public String getCoordTfmVersion() {
        return coordTfmVersion;
    }


    /**
     * Return the coordOpVariant property.
     *
     * @return The coordOpVariant property
     */
    public int getCoordOpVariant() {
        return coordOpVariant;
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
     * Return the coordOpScope property.
     *
     * @return The coordOpScope property
     */
    public String getCoordOpScope() {
        return coordOpScope;
    }


    /**
     * Return the coordOpMethodCode property.
     *
     * @return The coordOpMethodCode property
     */
    public String getCoordOpMethodCode() {
        return coordOpMethodCode;
    }


    /**
     * Return the uomCodeSourceOffsets property.
     *
     * @return The uomCodeSourceOffsets property
     */
    public String getUomCodeSourceOffsets() {
        return uomCodeSourceOffsets;
    }


    /**
     * Return the uomCodeTargetOffsets property.
     *
     * @return The uomCodeTargetOffsets property
     */
    public String getUomCodeTargetOffsets() {
        return uomCodeTargetOffsets;
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
     * Return the showOperation property.
     *
     * @return The showOperation property
     */
    public String getShowOperation() {
        return showOperation;
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
     * Find the CoordinateOperation object with the coordOpCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateOperation findCoordOpCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateOperation obj = (CoordinateOperation) members.get(i);
            if (obj.coordOpCode == value) {
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
        if (varname.equals("coordOpCode")) {
            return coordOpCode;
        }
        if (varname.equals("sourceCrsCode")) {
            return sourceCrsCode;
        }
        if (varname.equals("targetCrsCode")) {
            return targetCrsCode;
        }
        if (varname.equals("coordOpVariant")) {
            return coordOpVariant;
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
        if (varname.equals("coordOpName")) {
            return coordOpName;
        }
        if (varname.equals("coordOpType")) {
            return coordOpType;
        }
        if (varname.equals("coordTfmVersion")) {
            return coordTfmVersion;
        }
        if (varname.equals("coordOpScope")) {
            return coordOpScope;
        }
        if (varname.equals("coordOpMethodCode")) {
            return coordOpMethodCode;
        }
        if (varname.equals("uomCodeSourceOffsets")) {
            return uomCodeSourceOffsets;
        }
        if (varname.equals("uomCodeTargetOffsets")) {
            return uomCodeTargetOffsets;
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
        if (varname.equals("showOperation")) {
            return showOperation;
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
        return "" + "   coordOpCode=" + coordOpCode + "\n"
               + "   coordOpName=" + coordOpName + "\n" + "   coordOpType="
               + coordOpType + "\n" + "   sourceCrsCode=" + sourceCrsCode
               + "\n" + "   targetCrsCode=" + targetCrsCode + "\n"
               + "   coordTfmVersion=" + coordTfmVersion + "\n"
               + "   coordOpVariant=" + coordOpVariant + "\n"
               + "   areaOfUseCode=" + areaOfUseCode + "\n"
               + "   coordOpScope=" + coordOpScope + "\n"
               + "   coordOpMethodCode=" + coordOpMethodCode + "\n"
               + "   uomCodeSourceOffsets=" + uomCodeSourceOffsets + "\n"
               + "   uomCodeTargetOffsets=" + uomCodeTargetOffsets + "\n"
               + "   remarks=" + remarks + "\n" + "   informationSource="
               + informationSource + "\n" + "   dataSource=" + dataSource
               + "\n" + "   revisionDate=" + revisionDate + "\n"
               + "   changeId=" + changeId + "\n" + "   showOperation="
               + showOperation + "\n" + "   deprecated=" + deprecated + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of CoordinateOperation




