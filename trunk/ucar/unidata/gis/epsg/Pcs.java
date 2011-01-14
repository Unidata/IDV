
// $Id: Pcs.java,v 1.8 2005/03/10 18:38:35 jeffmc Exp $

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

public class Pcs extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/pcs.csv";


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
     * The uomCode property
     */
    private int uomCode;


    /**
     * The sourceGeogcrsCode property
     */
    private int sourceGeogcrsCode;


    /**
     * The coordOpCode property
     */
    private int coordOpCode;


    /**
     * The coordOpMethodCode property
     */
    private int coordOpMethodCode;


    /**
     * The parameterCode1 property
     */
    private int parameterCode1;


    /**
     * The parameterValue1 property
     */
    private double parameterValue1;


    /**
     * The parameterUom1 property
     */
    private int parameterUom1;


    /**
     * The parameterCode2 property
     */
    private int parameterCode2;


    /**
     * The parameterValue2 property
     */
    private double parameterValue2;


    /**
     * The parameterUom2 property
     */
    private int parameterUom2;


    /**
     * The parameterCode3 property
     */
    private int parameterCode3;


    /**
     * The parameterValue3 property
     */
    private double parameterValue3;


    /**
     * The parameterUom3 property
     */
    private int parameterUom3;


    /**
     * The parameterCode4 property
     */
    private int parameterCode4;


    /**
     * The parameterValue4 property
     */
    private double parameterValue4;


    /**
     * The parameterUom4 property
     */
    private int parameterUom4;


    /**
     * The parameterCode5 property
     */
    private int parameterCode5;


    /**
     * The parameterValue5 property
     */
    private double parameterValue5;


    /**
     * The parameterUom5 property
     */
    private int parameterUom5;


    /**
     * The parameterCode6 property
     */
    private int parameterCode6;


    /**
     * The parameterValue6 property
     */
    private double parameterValue6;


    /**
     * The parameterUom6 property
     */
    private int parameterUom6;


    /**
     * The parameterCode7 property
     */
    private int parameterCode7;


    /**
     * The parameterValue7 property
     */
    private double parameterValue7;


    /**
     * The parameterUom7 property
     */
    private int parameterUom7;


    /**
     * The constructor
     *       * @param arg_coordRefSysCode The coordRefSysCode argument
     * @param arg_coordRefSysName The coordRefSysName argument
     * @param arg_uomCode The uomCode argument
     * @param arg_sourceGeogcrsCode The sourceGeogcrsCode argument
     * @param arg_coordOpCode The coordOpCode argument
     * @param arg_coordOpMethodCode The coordOpMethodCode argument
     * @param arg_parameterCode1 The parameterCode1 argument
     * @param arg_parameterValue1 The parameterValue1 argument
     * @param arg_parameterUom1 The parameterUom1 argument
     * @param arg_parameterCode2 The parameterCode2 argument
     * @param arg_parameterValue2 The parameterValue2 argument
     * @param arg_parameterUom2 The parameterUom2 argument
     * @param arg_parameterCode3 The parameterCode3 argument
     * @param arg_parameterValue3 The parameterValue3 argument
     * @param arg_parameterUom3 The parameterUom3 argument
     * @param arg_parameterCode4 The parameterCode4 argument
     * @param arg_parameterValue4 The parameterValue4 argument
     * @param arg_parameterUom4 The parameterUom4 argument
     * @param arg_parameterCode5 The parameterCode5 argument
     * @param arg_parameterValue5 The parameterValue5 argument
     * @param arg_parameterUom5 The parameterUom5 argument
     * @param arg_parameterCode6 The parameterCode6 argument
     * @param arg_parameterValue6 The parameterValue6 argument
     * @param arg_parameterUom6 The parameterUom6 argument
     * @param arg_parameterCode7 The parameterCode7 argument
     * @param arg_parameterValue7 The parameterValue7 argument
     * @param arg_parameterUom7 The parameterUom7 argument
     *
     */
    public Pcs(int arg_coordRefSysCode, String arg_coordRefSysName,
               int arg_uomCode, int arg_sourceGeogcrsCode,
               int arg_coordOpCode, int arg_coordOpMethodCode,
               int arg_parameterCode1, double arg_parameterValue1,
               int arg_parameterUom1, int arg_parameterCode2,
               double arg_parameterValue2, int arg_parameterUom2,
               int arg_parameterCode3, double arg_parameterValue3,
               int arg_parameterUom3, int arg_parameterCode4,
               double arg_parameterValue4, int arg_parameterUom4,
               int arg_parameterCode5, double arg_parameterValue5,
               int arg_parameterUom5, int arg_parameterCode6,
               double arg_parameterValue6, int arg_parameterUom6,
               int arg_parameterCode7, double arg_parameterValue7,
               int arg_parameterUom7) {
        this.coordRefSysCode   = arg_coordRefSysCode;
        this.coordRefSysName   = arg_coordRefSysName;
        this.uomCode           = arg_uomCode;
        this.sourceGeogcrsCode = arg_sourceGeogcrsCode;
        this.coordOpCode       = arg_coordOpCode;
        this.coordOpMethodCode = arg_coordOpMethodCode;
        this.parameterCode1    = arg_parameterCode1;
        this.parameterValue1   = arg_parameterValue1;
        this.parameterUom1     = arg_parameterUom1;
        this.parameterCode2    = arg_parameterCode2;
        this.parameterValue2   = arg_parameterValue2;
        this.parameterUom2     = arg_parameterUom2;
        this.parameterCode3    = arg_parameterCode3;
        this.parameterValue3   = arg_parameterValue3;
        this.parameterUom3     = arg_parameterUom3;
        this.parameterCode4    = arg_parameterCode4;
        this.parameterValue4   = arg_parameterValue4;
        this.parameterUom4     = arg_parameterUom4;
        this.parameterCode5    = arg_parameterCode5;
        this.parameterValue5   = arg_parameterValue5;
        this.parameterUom5     = arg_parameterUom5;
        this.parameterCode6    = arg_parameterCode6;
        this.parameterValue6   = arg_parameterValue6;
        this.parameterUom6     = arg_parameterUom6;
        this.parameterCode7    = arg_parameterCode7;
        this.parameterValue7   = arg_parameterValue7;
        this.parameterUom7     = arg_parameterUom7;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public Pcs(List tuple) {
        this.coordRefSysCode   = getInt((String) tuple.get(0));
        this.coordRefSysName   = (String) tuple.get(1);
        this.uomCode           = getInt((String) tuple.get(2));
        this.sourceGeogcrsCode = getInt((String) tuple.get(3));
        this.coordOpCode       = getInt((String) tuple.get(4));
        this.coordOpMethodCode = getInt((String) tuple.get(5));
        this.parameterCode1    = getInt((String) tuple.get(6));
        this.parameterValue1   = getDouble((String) tuple.get(7));
        this.parameterUom1     = getInt((String) tuple.get(8));
        this.parameterCode2    = getInt((String) tuple.get(9));
        this.parameterValue2   = getDouble((String) tuple.get(10));
        this.parameterUom2     = getInt((String) tuple.get(11));
        this.parameterCode3    = getInt((String) tuple.get(12));
        this.parameterValue3   = getDouble((String) tuple.get(13));
        this.parameterUom3     = getInt((String) tuple.get(14));
        this.parameterCode4    = getInt((String) tuple.get(15));
        this.parameterValue4   = getDouble((String) tuple.get(16));
        this.parameterUom4     = getInt((String) tuple.get(17));
        this.parameterCode5    = getInt((String) tuple.get(18));
        this.parameterValue5   = getDouble((String) tuple.get(19));
        this.parameterUom5     = getInt((String) tuple.get(20));
        this.parameterCode6    = getInt((String) tuple.get(21));
        this.parameterValue6   = getDouble((String) tuple.get(22));
        this.parameterUom6     = getInt((String) tuple.get(23));
        this.parameterCode7    = getInt((String) tuple.get(24));
        this.parameterValue7   = getDouble((String) tuple.get(25));
        this.parameterUom7     = getInt((String) tuple.get(26));
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
                         Pcs.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 27) {
                System.err.println("csv/pcs.csv: line #" + i + " " + line);
                continue;
            }
            try {
                new Pcs(line);
            } catch (Exception exc) {
                System.err.println("Error creating Pcs " + exc);
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
     * Return the uomCode property.
     *
     * @return The uomCode property
     */
    public int getUomCode() {
        return uomCode;
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
     * Return the parameterCode1 property.
     *
     * @return The parameterCode1 property
     */
    public int getParameterCode1() {
        return parameterCode1;
    }


    /**
     * Return the parameterValue1 property.
     *
     * @return The parameterValue1 property
     */
    public double getParameterValue1() {
        return parameterValue1;
    }


    /**
     * Return the parameterUom1 property.
     *
     * @return The parameterUom1 property
     */
    public int getParameterUom1() {
        return parameterUom1;
    }


    /**
     * Return the parameterCode2 property.
     *
     * @return The parameterCode2 property
     */
    public int getParameterCode2() {
        return parameterCode2;
    }


    /**
     * Return the parameterValue2 property.
     *
     * @return The parameterValue2 property
     */
    public double getParameterValue2() {
        return parameterValue2;
    }


    /**
     * Return the parameterUom2 property.
     *
     * @return The parameterUom2 property
     */
    public int getParameterUom2() {
        return parameterUom2;
    }


    /**
     * Return the parameterCode3 property.
     *
     * @return The parameterCode3 property
     */
    public int getParameterCode3() {
        return parameterCode3;
    }


    /**
     * Return the parameterValue3 property.
     *
     * @return The parameterValue3 property
     */
    public double getParameterValue3() {
        return parameterValue3;
    }


    /**
     * Return the parameterUom3 property.
     *
     * @return The parameterUom3 property
     */
    public int getParameterUom3() {
        return parameterUom3;
    }


    /**
     * Return the parameterCode4 property.
     *
     * @return The parameterCode4 property
     */
    public int getParameterCode4() {
        return parameterCode4;
    }


    /**
     * Return the parameterValue4 property.
     *
     * @return The parameterValue4 property
     */
    public double getParameterValue4() {
        return parameterValue4;
    }


    /**
     * Return the parameterUom4 property.
     *
     * @return The parameterUom4 property
     */
    public int getParameterUom4() {
        return parameterUom4;
    }


    /**
     * Return the parameterCode5 property.
     *
     * @return The parameterCode5 property
     */
    public int getParameterCode5() {
        return parameterCode5;
    }


    /**
     * Return the parameterValue5 property.
     *
     * @return The parameterValue5 property
     */
    public double getParameterValue5() {
        return parameterValue5;
    }


    /**
     * Return the parameterUom5 property.
     *
     * @return The parameterUom5 property
     */
    public int getParameterUom5() {
        return parameterUom5;
    }


    /**
     * Return the parameterCode6 property.
     *
     * @return The parameterCode6 property
     */
    public int getParameterCode6() {
        return parameterCode6;
    }


    /**
     * Return the parameterValue6 property.
     *
     * @return The parameterValue6 property
     */
    public double getParameterValue6() {
        return parameterValue6;
    }


    /**
     * Return the parameterUom6 property.
     *
     * @return The parameterUom6 property
     */
    public int getParameterUom6() {
        return parameterUom6;
    }


    /**
     * Return the parameterCode7 property.
     *
     * @return The parameterCode7 property
     */
    public int getParameterCode7() {
        return parameterCode7;
    }


    /**
     * Return the parameterValue7 property.
     *
     * @return The parameterValue7 property
     */
    public double getParameterValue7() {
        return parameterValue7;
    }


    /**
     * Return the parameterUom7 property.
     *
     * @return The parameterUom7 property
     */
    public int getParameterUom7() {
        return parameterUom7;
    }


    /**
     * Find all of the Pcs objects with
     * the coordRefSysCode value == the given value
     *
     *
     * @param value _more_
     * @return The found objects
     */
    public static List findAllCoordRefSysCode(int value) {
        List results = new ArrayList();
        for (int i = 0; i < members.size(); i++) {
            Pcs obj = (Pcs) members.get(i);
            if (obj.coordRefSysCode == value) {
                results.add(obj);
            }
        }
        return results;
    }

    /**
     * Find the Pcs object with the coordRefSysCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Pcs findCoordRefSysCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            Pcs obj = (Pcs) members.get(i);
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
        if (varname.equals("uomCode")) {
            return uomCode;
        }
        if (varname.equals("sourceGeogcrsCode")) {
            return sourceGeogcrsCode;
        }
        if (varname.equals("coordOpCode")) {
            return coordOpCode;
        }
        if (varname.equals("coordOpMethodCode")) {
            return coordOpMethodCode;
        }
        if (varname.equals("parameterCode1")) {
            return parameterCode1;
        }
        if (varname.equals("parameterUom1")) {
            return parameterUom1;
        }
        if (varname.equals("parameterCode2")) {
            return parameterCode2;
        }
        if (varname.equals("parameterUom2")) {
            return parameterUom2;
        }
        if (varname.equals("parameterCode3")) {
            return parameterCode3;
        }
        if (varname.equals("parameterUom3")) {
            return parameterUom3;
        }
        if (varname.equals("parameterCode4")) {
            return parameterCode4;
        }
        if (varname.equals("parameterUom4")) {
            return parameterUom4;
        }
        if (varname.equals("parameterCode5")) {
            return parameterCode5;
        }
        if (varname.equals("parameterUom5")) {
            return parameterUom5;
        }
        if (varname.equals("parameterCode6")) {
            return parameterCode6;
        }
        if (varname.equals("parameterUom6")) {
            return parameterUom6;
        }
        if (varname.equals("parameterCode7")) {
            return parameterCode7;
        }
        if (varname.equals("parameterUom7")) {
            return parameterUom7;
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
        if (varname.equals("parameterValue1")) {
            return parameterValue1;
        }
        if (varname.equals("parameterValue2")) {
            return parameterValue2;
        }
        if (varname.equals("parameterValue3")) {
            return parameterValue3;
        }
        if (varname.equals("parameterValue4")) {
            return parameterValue4;
        }
        if (varname.equals("parameterValue5")) {
            return parameterValue5;
        }
        if (varname.equals("parameterValue6")) {
            return parameterValue6;
        }
        if (varname.equals("parameterValue7")) {
            return parameterValue7;
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
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public UnitOfMeasure findUnitOfMeasure() {
        return UnitOfMeasure.findUomCode(getUomCode());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public CoordinateOperation findCoordinateOperation() {
        return CoordinateOperation.findCoordOpCode(getCoordOpCode());
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public CoordinateOperationMethod findCoordinateOperationMethod() {
        return CoordinateOperationMethod.findCoordOpMethodCode(
            getCoordOpMethodCode());
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   coordRefSysCode=" + coordRefSysCode + "\n"
               + "   coordRefSysName=" + coordRefSysName + "\n"
               + "   uomCode=" + uomCode + "\n" + "   sourceGeogcrsCode="
               + sourceGeogcrsCode + "\n" + "   coordOpCode=" + coordOpCode
               + "\n" + "   coordOpMethodCode=" + coordOpMethodCode + "\n"
               + "   parameterCode1=" + parameterCode1 + "\n"
               + "   parameterValue1=" + parameterValue1 + "\n"
               + "   parameterUom1=" + parameterUom1 + "\n"
               + "   parameterCode2=" + parameterCode2 + "\n"
               + "   parameterValue2=" + parameterValue2 + "\n"
               + "   parameterUom2=" + parameterUom2 + "\n"
               + "   parameterCode3=" + parameterCode3 + "\n"
               + "   parameterValue3=" + parameterValue3 + "\n"
               + "   parameterUom3=" + parameterUom3 + "\n"
               + "   parameterCode4=" + parameterCode4 + "\n"
               + "   parameterValue4=" + parameterValue4 + "\n"
               + "   parameterUom4=" + parameterUom4 + "\n"
               + "   parameterCode5=" + parameterCode5 + "\n"
               + "   parameterValue5=" + parameterValue5 + "\n"
               + "   parameterUom5=" + parameterUom5 + "\n"
               + "   parameterCode6=" + parameterCode6 + "\n"
               + "   parameterValue6=" + parameterValue6 + "\n"
               + "   parameterUom6=" + parameterUom6 + "\n"
               + "   parameterCode7=" + parameterCode7 + "\n"
               + "   parameterValue7=" + parameterValue7 + "\n"
               + "   parameterUom7=" + parameterUom7 + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of Pcs




