
// $Id: CoordinateOperationParameterValue.java,v 1.8 2005/03/10 18:38:33 jeffmc Exp $

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

public class CoordinateOperationParameterValue
        extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName =
        "csv/coordinate_operation_parameter_value.csv";


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
     * The coordOpMethodCode property
     */
    private int coordOpMethodCode;


    /**
     * The parameterCode property
     */
    private int parameterCode;


    /**
     * The parameterValue property
     */
    private double parameterValue;


    /**
     * The paramValueFileRef property
     */
    private String paramValueFileRef;


    /**
     * The uomCode property
     */
    private int uomCode;


    /**
     * The constructor
     *       * @param arg_coordOpCode The coordOpCode argument
     * @param arg_coordOpMethodCode The coordOpMethodCode argument
     * @param arg_parameterCode The parameterCode argument
     * @param arg_parameterValue The parameterValue argument
     * @param arg_paramValueFileRef The paramValueFileRef argument
     * @param arg_uomCode The uomCode argument
     *
     */
    public CoordinateOperationParameterValue(int arg_coordOpCode,
                                             int arg_coordOpMethodCode,
                                             int arg_parameterCode,
                                             double arg_parameterValue,
                                             String arg_paramValueFileRef,
                                             int arg_uomCode) {
        this.coordOpCode       = arg_coordOpCode;
        this.coordOpMethodCode = arg_coordOpMethodCode;
        this.parameterCode     = arg_parameterCode;
        this.parameterValue    = arg_parameterValue;
        this.paramValueFileRef = arg_paramValueFileRef;
        this.uomCode           = arg_uomCode;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public CoordinateOperationParameterValue(List tuple) {
        this.coordOpCode       = getInt((String) tuple.get(0));
        this.coordOpMethodCode = getInt((String) tuple.get(1));
        this.parameterCode     = getInt((String) tuple.get(2));
        this.parameterValue    = getDouble((String) tuple.get(3));
        this.paramValueFileRef = (String) tuple.get(4);
        this.uomCode           = getInt((String) tuple.get(5));
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
                         CoordinateOperationParameterValue.class,
                         (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 6) {
                System.err.println(
                    "csv/coordinate_operation_parameter_value.csv: line #"
                    + i + " " + line);
                continue;
            }
            try {
                new CoordinateOperationParameterValue(line);
            } catch (Exception exc) {
                System.err.println(
                    "Error creating CoordinateOperationParameterValue "
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
     * Return the coordOpMethodCode property.
     *
     * @return The coordOpMethodCode property
     */
    public int getCoordOpMethodCode() {
        return coordOpMethodCode;
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
     * Return the parameterValue property.
     *
     * @return The parameterValue property
     */
    public double getParameterValue() {
        return parameterValue;
    }


    /**
     * Return the paramValueFileRef property.
     *
     * @return The paramValueFileRef property
     */
    public String getParamValueFileRef() {
        return paramValueFileRef;
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
     * Find the CoordinateOperationParameterValue object with the coordOpCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateOperationParameterValue findCoordOpCode(
            int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateOperationParameterValue obj =
                (CoordinateOperationParameterValue) members.get(i);
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
        if (varname.equals("coordOpMethodCode")) {
            return coordOpMethodCode;
        }
        if (varname.equals("parameterCode")) {
            return parameterCode;
        }
        if (varname.equals("uomCode")) {
            return uomCode;
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
        if (varname.equals("parameterValue")) {
            return parameterValue;
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
        if (varname.equals("paramValueFileRef")) {
            return paramValueFileRef;
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
               + "   coordOpMethodCode=" + coordOpMethodCode + "\n"
               + "   parameterCode=" + parameterCode + "\n"
               + "   parameterValue=" + parameterValue + "\n"
               + "   paramValueFileRef=" + paramValueFileRef + "\n"
               + "   uomCode=" + uomCode + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of CoordinateOperationParameterValue




