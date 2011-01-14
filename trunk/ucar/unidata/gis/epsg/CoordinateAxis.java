
// $Id: CoordinateAxis.java,v 1.8 2005/03/10 18:38:32 jeffmc Exp $

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

public class CoordinateAxis extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/coordinate_axis.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The coordSysCode property
     */
    private int coordSysCode;


    /**
     * The coordAxisNameCode property
     */
    private int coordAxisNameCode;


    /**
     * The coordAxisOrientation property
     */
    private String coordAxisOrientation;


    /**
     * The coordAxisAbbreviation property
     */
    private String coordAxisAbbreviation;


    /**
     * The uomCode property
     */
    private int uomCode;


    /**
     * The order property
     */
    private int order;


    /**
     * The constructor
     *       * @param arg_coordSysCode The coordSysCode argument
     * @param arg_coordAxisNameCode The coordAxisNameCode argument
     * @param arg_coordAxisOrientation The coordAxisOrientation argument
     * @param arg_coordAxisAbbreviation The coordAxisAbbreviation argument
     * @param arg_uomCode The uomCode argument
     * @param arg_order The order argument
     *
     */
    public CoordinateAxis(int arg_coordSysCode, int arg_coordAxisNameCode,
                          String arg_coordAxisOrientation,
                          String arg_coordAxisAbbreviation, int arg_uomCode,
                          int arg_order) {
        this.coordSysCode          = arg_coordSysCode;
        this.coordAxisNameCode     = arg_coordAxisNameCode;
        this.coordAxisOrientation  = arg_coordAxisOrientation;
        this.coordAxisAbbreviation = arg_coordAxisAbbreviation;
        this.uomCode               = arg_uomCode;
        this.order                 = arg_order;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public CoordinateAxis(List tuple) {
        this.coordSysCode          = getInt((String) tuple.get(0));
        this.coordAxisNameCode     = getInt((String) tuple.get(1));
        this.coordAxisOrientation  = (String) tuple.get(2);
        this.coordAxisAbbreviation = (String) tuple.get(3);
        this.uomCode               = getInt((String) tuple.get(4));
        this.order                 = getInt((String) tuple.get(5));
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
                         CoordinateAxis.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 6) {
                System.err.println("csv/coordinate_axis.csv: line #" + i
                                   + " " + line);
                continue;
            }
            try {
                new CoordinateAxis(line);
            } catch (Exception exc) {
                System.err.println("Error creating CoordinateAxis " + exc);
                exc.printStackTrace();
                return;
            }
        }
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
     * Return the coordAxisNameCode property.
     *
     * @return The coordAxisNameCode property
     */
    public int getCoordAxisNameCode() {
        return coordAxisNameCode;
    }


    /**
     * Return the coordAxisOrientation property.
     *
     * @return The coordAxisOrientation property
     */
    public String getCoordAxisOrientation() {
        return coordAxisOrientation;
    }


    /**
     * Return the coordAxisAbbreviation property.
     *
     * @return The coordAxisAbbreviation property
     */
    public String getCoordAxisAbbreviation() {
        return coordAxisAbbreviation;
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
     * Return the order property.
     *
     * @return The order property
     */
    public int getOrder() {
        return order;
    }


    /**
     * Find the CoordinateAxis object with the coordSysCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateAxis findCoordSysCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateAxis obj = (CoordinateAxis) members.get(i);
            if (obj.coordSysCode == value) {
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
        if (varname.equals("coordSysCode")) {
            return coordSysCode;
        }
        if (varname.equals("coordAxisNameCode")) {
            return coordAxisNameCode;
        }
        if (varname.equals("uomCode")) {
            return uomCode;
        }
        if (varname.equals("order")) {
            return order;
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
        if (varname.equals("coordAxisOrientation")) {
            return coordAxisOrientation;
        }
        if (varname.equals("coordAxisAbbreviation")) {
            return coordAxisAbbreviation;
        }
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   coordSysCode=" + coordSysCode + "\n"
               + "   coordAxisNameCode=" + coordAxisNameCode + "\n"
               + "   coordAxisOrientation=" + coordAxisOrientation + "\n"
               + "   coordAxisAbbreviation=" + coordAxisAbbreviation + "\n"
               + "   uomCode=" + uomCode + "\n" + "   order=" + order + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of CoordinateAxis




