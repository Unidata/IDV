
// $Id: Gcs.java,v 1.8 2005/03/10 18:38:35 jeffmc Exp $

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

public class Gcs extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/gcs.csv";


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
     * The datumCode property
     */
    private int datumCode;


    /**
     * The datumName property
     */
    private String datumName;


    /**
     * The greenwichDatum property
     */
    private int greenwichDatum;


    /**
     * The uomCode property
     */
    private int uomCode;


    /**
     * The ellipsoidCode property
     */
    private int ellipsoidCode;


    /**
     * The primeMeridianCode property
     */
    private int primeMeridianCode;


    /**
     * The coordOpMethodCode property
     */
    private int coordOpMethodCode;


    /**
     * The dx property
     */
    private double dx;


    /**
     * The dy property
     */
    private double dy;


    /**
     * The dz property
     */
    private double dz;


    /**
     * The rx property
     */
    private double rx;


    /**
     * The ry property
     */
    private double ry;


    /**
     * The rz property
     */
    private double rz;


    /**
     * The ds property
     */
    private double ds;


    /**
     * The constructor
     *       * @param arg_coordRefSysCode The coordRefSysCode argument
     * @param arg_coordRefSysName The coordRefSysName argument
     * @param arg_datumCode The datumCode argument
     * @param arg_datumName The datumName argument
     * @param arg_greenwichDatum The greenwichDatum argument
     * @param arg_uomCode The uomCode argument
     * @param arg_ellipsoidCode The ellipsoidCode argument
     * @param arg_primeMeridianCode The primeMeridianCode argument
     * @param arg_coordOpMethodCode The coordOpMethodCode argument
     * @param arg_dx The dx argument
     * @param arg_dy The dy argument
     * @param arg_dz The dz argument
     * @param arg_rx The rx argument
     * @param arg_ry The ry argument
     * @param arg_rz The rz argument
     * @param arg_ds The ds argument
     *
     */
    public Gcs(int arg_coordRefSysCode, String arg_coordRefSysName,
               int arg_datumCode, String arg_datumName,
               int arg_greenwichDatum, int arg_uomCode,
               int arg_ellipsoidCode, int arg_primeMeridianCode,
               int arg_coordOpMethodCode, double arg_dx, double arg_dy,
               double arg_dz, double arg_rx, double arg_ry, double arg_rz,
               double arg_ds) {
        this.coordRefSysCode   = arg_coordRefSysCode;
        this.coordRefSysName   = arg_coordRefSysName;
        this.datumCode         = arg_datumCode;
        this.datumName         = arg_datumName;
        this.greenwichDatum    = arg_greenwichDatum;
        this.uomCode           = arg_uomCode;
        this.ellipsoidCode     = arg_ellipsoidCode;
        this.primeMeridianCode = arg_primeMeridianCode;
        this.coordOpMethodCode = arg_coordOpMethodCode;
        this.dx                = arg_dx;
        this.dy                = arg_dy;
        this.dz                = arg_dz;
        this.rx                = arg_rx;
        this.ry                = arg_ry;
        this.rz                = arg_rz;
        this.ds                = arg_ds;
    }


    /**
     * The list based constructor
     *
     * @param tuple The list
     */
    public Gcs(List tuple) {
        this.coordRefSysCode   = getInt((String) tuple.get(0));
        this.coordRefSysName   = (String) tuple.get(1);
        this.datumCode         = getInt((String) tuple.get(2));
        this.datumName         = (String) tuple.get(3);
        this.greenwichDatum    = getInt((String) tuple.get(4));
        this.uomCode           = getInt((String) tuple.get(5));
        this.ellipsoidCode     = getInt((String) tuple.get(6));
        this.primeMeridianCode = getInt((String) tuple.get(7));
        this.coordOpMethodCode = getInt((String) tuple.get(8));
        this.dx                = getDouble((String) tuple.get(9));
        this.dy                = getDouble((String) tuple.get(10));
        this.dz                = getDouble((String) tuple.get(11));
        this.rx                = getDouble((String) tuple.get(12));
        this.ry                = getDouble((String) tuple.get(13));
        this.rz                = getDouble((String) tuple.get(14));
        this.ds                = getDouble((String) tuple.get(15));
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
                         Gcs.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 16) {
                System.err.println("csv/gcs.csv: line #" + i + " " + line);
                continue;
            }
            try {
                new Gcs(line);
            } catch (Exception exc) {
                System.err.println("Error creating Gcs " + exc);
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
     * Return the greenwichDatum property.
     *
     * @return The greenwichDatum property
     */
    public int getGreenwichDatum() {
        return greenwichDatum;
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
     * Return the ellipsoidCode property.
     *
     * @return The ellipsoidCode property
     */
    public int getEllipsoidCode() {
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
     * Return the coordOpMethodCode property.
     *
     * @return The coordOpMethodCode property
     */
    public int getCoordOpMethodCode() {
        return coordOpMethodCode;
    }


    /**
     * Return the dx property.
     *
     * @return The dx property
     */
    public double getDx() {
        return dx;
    }


    /**
     * Return the dy property.
     *
     * @return The dy property
     */
    public double getDy() {
        return dy;
    }


    /**
     * Return the dz property.
     *
     * @return The dz property
     */
    public double getDz() {
        return dz;
    }


    /**
     * Return the rx property.
     *
     * @return The rx property
     */
    public double getRx() {
        return rx;
    }


    /**
     * Return the ry property.
     *
     * @return The ry property
     */
    public double getRy() {
        return ry;
    }


    /**
     * Return the rz property.
     *
     * @return The rz property
     */
    public double getRz() {
        return rz;
    }


    /**
     * Return the ds property.
     *
     * @return The ds property
     */
    public double getDs() {
        return ds;
    }


    /**
     * Find the Gcs object with the coordRefSysCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static Gcs findCoordRefSysCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            Gcs obj = (Gcs) members.get(i);
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
        if (varname.equals("datumCode")) {
            return datumCode;
        }
        if (varname.equals("greenwichDatum")) {
            return greenwichDatum;
        }
        if (varname.equals("uomCode")) {
            return uomCode;
        }
        if (varname.equals("ellipsoidCode")) {
            return ellipsoidCode;
        }
        if (varname.equals("primeMeridianCode")) {
            return primeMeridianCode;
        }
        if (varname.equals("coordOpMethodCode")) {
            return coordOpMethodCode;
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
        if (varname.equals("dx")) {
            return dx;
        }
        if (varname.equals("dy")) {
            return dy;
        }
        if (varname.equals("dz")) {
            return dz;
        }
        if (varname.equals("rx")) {
            return rx;
        }
        if (varname.equals("ry")) {
            return ry;
        }
        if (varname.equals("rz")) {
            return rz;
        }
        if (varname.equals("ds")) {
            return ds;
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
        if (varname.equals("datumName")) {
            return datumName;
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
               + "   datumCode=" + datumCode + "\n" + "   datumName="
               + datumName + "\n" + "   greenwichDatum=" + greenwichDatum
               + "\n" + "   uomCode=" + uomCode + "\n" + "   ellipsoidCode="
               + ellipsoidCode + "\n" + "   primeMeridianCode="
               + primeMeridianCode + "\n" + "   coordOpMethodCode="
               + coordOpMethodCode + "\n" + "   dx=" + dx + "\n" + "   dy="
               + dy + "\n" + "   dz=" + dz + "\n" + "   rx=" + rx + "\n"
               + "   ry=" + ry + "\n" + "   rz=" + rz + "\n" + "   ds=" + ds
               + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of Gcs




