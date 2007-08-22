
// $Id: CoordinateOperationPath.java,v 1.8 2005/03/10 18:38:34 jeffmc Exp $

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

public class CoordinateOperationPath extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/coordinate_operation_path.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The concatOperationCode property
     */
    private int concatOperationCode;


    /**
     * The singleOperationCode property
     */
    private int singleOperationCode;


    /**
     * The opPathStep property
     */
    private int opPathStep;


    /**
     * The constructor
     *       * @param arg_concatOperationCode The concatOperationCode argument
     * @param arg_singleOperationCode The singleOperationCode argument
     * @param arg_opPathStep The opPathStep argument
     *
     */
    public CoordinateOperationPath(int arg_concatOperationCode,
                                   int arg_singleOperationCode,
                                   int arg_opPathStep) {
        this.concatOperationCode = arg_concatOperationCode;
        this.singleOperationCode = arg_singleOperationCode;
        this.opPathStep          = arg_opPathStep;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public CoordinateOperationPath(List tuple) {
        this.concatOperationCode = getInt((String) tuple.get(0));
        this.singleOperationCode = getInt((String) tuple.get(1));
        this.opPathStep          = getInt((String) tuple.get(2));
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
                         CoordinateOperationPath.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 3) {
                System.err.println("csv/coordinate_operation_path.csv: line #"
                                   + i + " " + line);
                continue;
            }
            try {
                new CoordinateOperationPath(line);
            } catch (Exception exc) {
                System.err.println("Error creating CoordinateOperationPath "
                                   + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the concatOperationCode property.
     *
     * @return The concatOperationCode property
     */
    public int getConcatOperationCode() {
        return concatOperationCode;
    }


    /**
     * Return the singleOperationCode property.
     *
     * @return The singleOperationCode property
     */
    public int getSingleOperationCode() {
        return singleOperationCode;
    }


    /**
     * Return the opPathStep property.
     *
     * @return The opPathStep property
     */
    public int getOpPathStep() {
        return opPathStep;
    }


    /**
     * Find the CoordinateOperationPath object with the concatOperationCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static CoordinateOperationPath findConcatOperationCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            CoordinateOperationPath obj =
                (CoordinateOperationPath) members.get(i);
            if (obj.concatOperationCode == value) {
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
        if (varname.equals("concatOperationCode")) {
            return concatOperationCode;
        }
        if (varname.equals("singleOperationCode")) {
            return singleOperationCode;
        }
        if (varname.equals("opPathStep")) {
            return opPathStep;
        }
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   concatOperationCode=" + concatOperationCode + "\n"
               + "   singleOperationCode=" + singleOperationCode + "\n"
               + "   opPathStep=" + opPathStep + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of CoordinateOperationPath




