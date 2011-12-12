
// $Id: Stateplane.java,v 1.8 2005/03/10 18:38:36 jeffmc Exp $

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

public class Stateplane extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/stateplane.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The id property
     */
    private int id;


    /**
     * The state property
     */
    private String state;


    /**
     * The zone property
     */
    private String zone;


    /**
     * The projMethod property
     */
    private int projMethod;


    /**
     * The datum property
     */
    private String datum;


    /**
     * The usgsCode property
     */
    private int usgsCode;


    /**
     * The epsgPcsCode property
     */
    private int epsgPcsCode;


    /**
     * The constructor
     *       * @param arg_id The id argument
     * @param arg_state The state argument
     * @param arg_zone The zone argument
     * @param arg_projMethod The projMethod argument
     * @param arg_datum The datum argument
     * @param arg_usgsCode The usgsCode argument
     * @param arg_epsgPcsCode The epsgPcsCode argument
     *
     */
    public Stateplane(int arg_id, String arg_state, String arg_zone,
                      int arg_projMethod, String arg_datum, int arg_usgsCode,
                      int arg_epsgPcsCode) {
        this.id          = arg_id;
        this.state       = arg_state;
        this.zone        = arg_zone;
        this.projMethod  = arg_projMethod;
        this.datum       = arg_datum;
        this.usgsCode    = arg_usgsCode;
        this.epsgPcsCode = arg_epsgPcsCode;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public Stateplane(List tuple) {
        this.id          = getInt((String) tuple.get(0));
        this.state       = (String) tuple.get(1);
        this.zone        = (String) tuple.get(2);
        this.projMethod  = getInt((String) tuple.get(3));
        this.datum       = (String) tuple.get(4);
        this.usgsCode    = getInt((String) tuple.get(5));
        this.epsgPcsCode = getInt((String) tuple.get(6));
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
                         Stateplane.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 7) {
                System.err.println("csv/stateplane.csv: line #" + i + " "
                                   + line);
                continue;
            }
            try {
                new Stateplane(line);
            } catch (Exception exc) {
                System.err.println("Error creating Stateplane " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the id property.
     *
     * @return The id property
     */
    public int getId() {
        return id;
    }


    /**
     * Return the state property.
     *
     * @return The state property
     */
    public String getState() {
        return state;
    }


    /**
     * Return the zone property.
     *
     * @return The zone property
     */
    public String getZone() {
        return zone;
    }


    /**
     * Return the projMethod property.
     *
     * @return The projMethod property
     */
    public int getProjMethod() {
        return projMethod;
    }


    /**
     * Return the datum property.
     *
     * @return The datum property
     */
    public String getDatum() {
        return datum;
    }


    /**
     * Return the usgsCode property.
     *
     * @return The usgsCode property
     */
    public int getUsgsCode() {
        return usgsCode;
    }


    /**
     * Return the epsgPcsCode property.
     *
     * @return The epsgPcsCode property
     */
    public int getEpsgPcsCode() {
        return epsgPcsCode;
    }


    /**
     * Find the Stateplane object with the id value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Stateplane findId(int value) {
        for (int i = 0; i < members.size(); i++) {
            Stateplane obj = (Stateplane) members.get(i);
            if (obj.id == value) {
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
        if (varname.equals("id")) {
            return id;
        }
        if (varname.equals("projMethod")) {
            return projMethod;
        }
        if (varname.equals("usgsCode")) {
            return usgsCode;
        }
        if (varname.equals("epsgPcsCode")) {
            return epsgPcsCode;
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
        if (varname.equals("state")) {
            return state;
        }
        if (varname.equals("zone")) {
            return zone;
        }
        if (varname.equals("datum")) {
            return datum;
        }
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   id=" + id + "\n" + "   state=" + state + "\n"
               + "   zone=" + zone + "\n" + "   projMethod=" + projMethod
               + "\n" + "   datum=" + datum + "\n" + "   usgsCode="
               + usgsCode + "\n" + "   epsgPcsCode=" + epsgPcsCode + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of Stateplane




