
// $Id: CoordinateOperationParameter.java,v 1.8 2005/03/10 18:38:33 jeffmc Exp $

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

public class CoordinateOperationParameter extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName =
        "csv/coordinate_operation_parameter.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * Properties generated from the csv file
     */
    public static final int LATITUDE_OFFSET = 8601;

    /** _more_          */
    public static final int LONGITUDE_OFFSET = 8602;

    /** _more_          */
    public static final int VERTICAL_OFFSET = 8603;

    /** _more_          */
    public static final int GEOID_UNDULATION = 8604;

    /** _more_          */
    public static final int X_AXIS_TRANSLATION = 8605;

    /** _more_          */
    public static final int Y_AXIS_TRANSLATION = 8606;

    /** _more_          */
    public static final int Z_AXIS_TRANSLATION = 8607;

    /** _more_          */
    public static final int X_AXIS_ROTATION = 8608;

    /** _more_          */
    public static final int Y_AXIS_ROTATION = 8609;

    /** _more_          */
    public static final int Z_AXIS_ROTATION = 8610;

    /** _more_          */
    public static final int SCALE_DIFFERENCE = 8611;

    /** _more_          */
    public static final int SCALE_FACTOR_FOR_SOURCE_COORDINATE_REFERENCE_SYSTEM_FIRST_AXIS =
        8612;

    /** _more_          */
    public static final int SCALE_FACTOR_FOR_SOURCE_COORDINATE_REFERENCE_SYSTEM_SECOND_AXIS =
        8613;

    /** _more_          */
    public static final int ROTATION_ANGLE_OF_SOURCE_COORDINATE_REFERENCE_SYSTEM_AXES =
        8614;

    /** _more_          */
    public static final int ROTATION_ANGLE_OF_SOURCE_COORDINATE_REFERENCE_SYSTEM_FIRST_AXIS =
        8615;

    /** _more_          */
    public static final int ROTATION_ANGLE_OF_SOURCE_COORDINATE_REFERENCE_SYSTEM_SECOND_AXIS =
        8616;

    /** _more_          */
    public static final int ORDINATE_1_OF_EVALUATION_POINT = 8617;

    /** _more_          */
    public static final int ORDINATE_2_OF_EVALUATION_POINT = 8618;

    /** _more_          */
    public static final int ORDINATE_1_OF_EVALUATION_POINT_IN_SOURCE_CRS =
        8619;

    /** _more_          */
    public static final int ORDINATE_2_OF_EVALUATION_POINT_IN_SOURCE_CRS =
        8620;

    /** _more_          */
    public static final int ORDINATE_1_OF_EVALUATION_POINT_IN_TARGET_CRS =
        8621;

    /** _more_          */
    public static final int ORDINATE_2_OF_EVALUATION_POINT_IN_TARGET_CRS =
        8622;

    /** _more_          */
    public static final int A0 = 8623;

    /** _more_          */
    public static final int A1 = 8624;

    /** _more_          */
    public static final int A2 = 8625;

    /** _more_          */
    public static final int A3 = 8626;

    /** _more_          */
    public static final int A4 = 8627;

    /** _more_          */
    public static final int A5 = 8628;

    /** _more_          */
    public static final int A6 = 8629;

    /** _more_          */
    public static final int A7 = 8630;

    /** _more_          */
    public static final int A8 = 8631;

    /** _more_          */
    public static final int A9 = 8632;

    /** _more_          */
    public static final int A10 = 8633;

    /** _more_          */
    public static final int A11 = 8634;

    /** _more_          */
    public static final int A12 = 8635;

    /** _more_          */
    public static final int A13 = 8636;

    /** _more_          */
    public static final int A14 = 8637;

    /** _more_          */
    public static final int B00 = 8638;

    /** _more_          */
    public static final int B0 = 8639;

    /** _more_          */
    public static final int B1 = 8640;

    /** _more_          */
    public static final int B2 = 8641;

    /** _more_          */
    public static final int B3 = 8642;

    /** _more_          */
    public static final int B4 = 8643;

    /** _more_          */
    public static final int B5 = 8644;

    /** _more_          */
    public static final int B6 = 8645;

    /** _more_          */
    public static final int B7 = 8646;

    /** _more_          */
    public static final int B8 = 8647;

    /** _more_          */
    public static final int B9 = 8648;

    /** _more_          */
    public static final int B10 = 8649;

    /** _more_          */
    public static final int B11 = 8650;

    /** _more_          */
    public static final int B12 = 8651;

    /** _more_          */
    public static final int B13 = 8652;

    /** _more_          */
    public static final int B14 = 8653;

    /** _more_          */
    public static final int SEMI_MAJOR_AXIS_LENGTH_DIFFERENCE = 8654;

    /** _more_          */
    public static final int FLATTENING_DIFFERENCE = 8655;

    /** _more_          */
    public static final int LATITUDE_AND_LONGITUDE_DIFFERENCE_FILE = 8656;

    /** _more_          */
    public static final int LATITUDE_DIFFERENCE_FILE = 8657;

    /** _more_          */
    public static final int LONGITUDE_DIFFERENCE_FILE = 8658;

    /** _more_          */
    public static final int GEOD_TFM_CODE_FOR_NORTHERN_BOUNDARY = 8659;

    /** _more_          */
    public static final int GEOD_TFM_CODE_FOR_SOUTHERN_BOUNDARY = 8660;

    /** _more_          */
    public static final int GEOD_TFM_NAME_FOR_NORTHERN_BOUNDARY = 8661;

    /** _more_          */
    public static final int GEOD_TFM_NAME_FOR_SOUTHERN_BOUNDARY = 8662;

    /** _more_          */
    public static final int POINT_SCALE_FACTOR = 8663;

    /** _more_          */
    public static final int EASTING_AND_NORTHING_DIFFERENCE_FILE = 8664;

    /** _more_          */
    public static final int MARITIME_PROVINCE_RESIDUAL_FILE = 8665;

    /** _more_          */
    public static final int GEOID_MODEL_FILE = 8666;

    /** _more_          */
    public static final int ORDINATE_3_OF_EVALUATION_POINT = 8667;

    /** _more_          */
    public static final int LATITUDE_OF_NATURAL_ORIGIN = 8801;

    /** _more_          */
    public static final int LONGITUDE_OF_NATURAL_ORIGIN = 8802;

    /** _more_          */
    public static final int SCALE_FACTOR_AT_NATURAL_ORIGIN = 8805;

    /** _more_          */
    public static final int FALSE_EASTING = 8806;

    /** _more_          */
    public static final int FALSE_NORTHING = 8807;

    /** _more_          */
    public static final int LATITUDE_OF_PROJECTION_CENTRE = 8811;

    /** _more_          */
    public static final int LONGITUDE_OF_PROJECTION_CENTRE = 8812;

    /** _more_          */
    public static final int AZIMUTH_OF_INITIAL_LINE = 8813;

    /** _more_          */
    public static final int ANGLE_FROM_RECTIFIED_TO_SKEW_GRID = 8814;

    /** _more_          */
    public static final int SCALE_FACTOR_ON_INITIAL_LINE = 8815;

    /** _more_          */
    public static final int EASTING_AT_PROJECTION_CENTRE = 8816;

    /** _more_          */
    public static final int NORTHING_AT_PROJECTION_CENTRE = 8817;

    /** _more_          */
    public static final int LATITUDE_OF_PSEUDO_STANDARD_PARALLEL = 8818;

    /** _more_          */
    public static final int SCALE_FACTOR_ON_PSEUDO_STANDARD_PARALLEL = 8819;

    /** _more_          */
    public static final int LATITUDE_OF_FALSE_ORIGIN = 8821;

    /** _more_          */
    public static final int LONGITUDE_OF_FALSE_ORIGIN = 8822;

    /** _more_          */
    public static final int LATITUDE_OF_1ST_STANDARD_PARALLEL = 8823;

    /** _more_          */
    public static final int LATITUDE_OF_2ND_STANDARD_PARALLEL = 8824;

    /** _more_          */
    public static final int EASTING_AT_FALSE_ORIGIN = 8826;

    /** _more_          */
    public static final int NORTHING_AT_FALSE_ORIGIN = 8827;

    /** _more_          */
    public static final int SPHERICAL_LATITUDE_OF_ORIGIN = 8828;

    /** _more_          */
    public static final int SPHERICAL_LONGITUDE_OF_ORIGIN = 8829;

    /** _more_          */
    public static final int INITIAL_LONGITUDE = 8830;

    /** _more_          */
    public static final int ZONE_WIDTH = 8831;



    /**
     * The parameterCode property
     */
    private int parameterCode;


    /**
     * The parameterName property
     */
    private String parameterName;


    /**
     * The description property
     */
    private String description;


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
     *       * @param arg_parameterCode The parameterCode argument
     * @param arg_parameterName The parameterName argument
     * @param arg_description The description argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public CoordinateOperationParameter(int arg_parameterCode,
                                        String arg_parameterName,
                                        String arg_description,
                                        String arg_informationSource,
                                        String arg_dataSource,
                                        String arg_revisionDate,
                                        String arg_changeId,
                                        String arg_deprecated) {
        this.parameterCode     = arg_parameterCode;
        this.parameterName     = arg_parameterName;
        this.description       = arg_description;
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
    public CoordinateOperationParameter(List tuple) {
        this.parameterCode     = getInt((String) tuple.get(0));
        this.parameterName     = (String) tuple.get(1);
        this.description       = (String) tuple.get(2);
        this.informationSource = (String) tuple.get(3);
        this.dataSource        = (String) tuple.get(4);
        this.revisionDate      = (String) tuple.get(5);
        this.changeId          = (String) tuple.get(6);
        this.deprecated        = (String) tuple.get(7);
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
                         CoordinateOperationParameter.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 8) {
                System.err.println(
                    "csv/coordinate_operation_parameter.csv: line #" + i
                    + " " + line);
                continue;
            }
            try {
                new CoordinateOperationParameter(line);
            } catch (Exception exc) {
                System.err.println(
                    "Error creating CoordinateOperationParameter " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the parameterCode property.
     *
     * @return The parameterCode property
     */
    public int getParameterCode() {
        return parameterCode;
    }


    /**
     * Return the parameterName property.
     *
     * @return The parameterName property
     */
    public String getParameterName() {
        return parameterName;
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
     * Find the CoordinateOperationParameter object with the parameterCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateOperationParameter findParameterCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateOperationParameter obj =
                (CoordinateOperationParameter) members.get(i);
            if (obj.parameterCode == value) {
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
        if (varname.equals("parameterCode")) {
            return parameterCode;
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
        if (varname.equals("parameterName")) {
            return parameterName;
        }
        if (varname.equals("description")) {
            return description;
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
        return "" + "   parameterCode=" + parameterCode + "\n"
               + "   parameterName=" + parameterName + "\n"
               + "   description=" + description + "\n"
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



}  //End of CoordinateOperationParameter




