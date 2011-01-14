
// $Id: NamingSystem.java,v 1.8 2005/03/10 18:38:35 jeffmc Exp $

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

public class NamingSystem extends ucar.unidata.util.CsvDb {

    /**
     * My csv file
     */
    private static String csvFileName = "csv/naming_system.csv";


    /**
     * Have we initialized
     */
    private static boolean haveInitialized = false;


    /**
     * The members list
     */
    private static List members = new ArrayList();


    /**
     * The namingSystemCode property
     */
    private int namingSystemCode;


    /**
     * The namingSystemName property
     */
    private String namingSystemName;


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
    private int deprecated;


    /**
     * The constructor
     *       * @param arg_namingSystemCode The namingSystemCode argument
     * @param arg_namingSystemName The namingSystemName argument
     * @param arg_remarks The remarks argument
     * @param arg_informationSource The informationSource argument
     * @param arg_dataSource The dataSource argument
     * @param arg_revisionDate The revisionDate argument
     * @param arg_changeId The changeId argument
     * @param arg_deprecated The deprecated argument
     *
     */
    public NamingSystem(int arg_namingSystemCode,
                        String arg_namingSystemName, String arg_remarks,
                        String arg_informationSource, String arg_dataSource,
                        String arg_revisionDate, String arg_changeId,
                        int arg_deprecated) {
        this.namingSystemCode  = arg_namingSystemCode;
        this.namingSystemName  = arg_namingSystemName;
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
    public NamingSystem(List tuple) {
        this.namingSystemCode  = getInt((String) tuple.get(0));
        this.namingSystemName  = (String) tuple.get(1);
        this.remarks           = (String) tuple.get(2);
        this.informationSource = (String) tuple.get(3);
        this.dataSource        = (String) tuple.get(4);
        this.revisionDate      = (String) tuple.get(5);
        this.changeId          = (String) tuple.get(6);
        this.deprecated        = getInt((String) tuple.get(7));
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
                         NamingSystem.class, (String) null);
        if (csv == null) {
            System.err.println("Failed to read:" + csvFileName);
            return;
        }
        List lines = StringUtil.parseCsv(csv, true);
        for (int i = 0; i < lines.size(); i++) {
            List line = (List) lines.get(i);
            if (line.size() < 8) {
                System.err.println("csv/naming_system.csv: line #" + i + " "
                                   + line);
                continue;
            }
            try {
                new NamingSystem(line);
            } catch (Exception exc) {
                System.err.println("Error creating NamingSystem " + exc);
                exc.printStackTrace();
                return;
            }
        }
    }


    /**
     * Return the namingSystemCode property.
     *
     * @return The namingSystemCode property
     */
    public int getNamingSystemCode() {
        return namingSystemCode;
    }


    /**
     * Return the namingSystemName property.
     *
     * @return The namingSystemName property
     */
    public String getNamingSystemName() {
        return namingSystemName;
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
    public int getDeprecated() {
        return deprecated;
    }


    /**
     * Find the NamingSystem object with the namingSystemCode value == the given value
     *
     *
     * @param value _more_
     * @return The object
     */
    public static NamingSystem findNamingSystemCode(int value) {
        for (int i = 0; i < members.size(); i++) {
            NamingSystem obj = (NamingSystem) members.get(i);
            if (obj.namingSystemCode == value) {
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
        if (varname.equals("namingSystemCode")) {
            return namingSystemCode;
        }
        if (varname.equals("deprecated")) {
            return deprecated;
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
        if (varname.equals("namingSystemName")) {
            return namingSystemName;
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
        throw new IllegalArgumentException("Unknown name:" + varname);
    }

    /**
     * Override toString
     *
     * @return String
     */
    public String toString() {
        return "" + "   namingSystemCode=" + namingSystemCode + "\n"
               + "   namingSystemName=" + namingSystemName + "\n"
               + "   remarks=" + remarks + "\n" + "   informationSource="
               + informationSource + "\n" + "   dataSource=" + dataSource
               + "\n" + "   revisionDate=" + revisionDate + "\n"
               + "   changeId=" + changeId + "\n" + "   deprecated="
               + deprecated + "\n";
    }

    /**
     * Implement main
     *
     * @param args The args
     */
    public static void main(String[] args) {}



}  //End of NamingSystem




