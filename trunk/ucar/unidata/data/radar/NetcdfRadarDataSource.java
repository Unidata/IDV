/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package ucar.unidata.data.radar;


import ucar.unidata.data.*;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import java.io.File;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A data source for DORADE radar data
 * @author IDV Development Team @ ATD
 * @version $Revision: 1.16 $
 */
public class NetcdfRadarDataSource extends RadarDataSource {

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(NetcdfRadarDataSource.class.getName());

    /** Identifier for Station location */
    private static final String DATA_DESCRIPTION = "Netcdf Sweep Data";

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public NetcdfRadarDataSource() {}

    /**
     * Construct a new DORADE data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the DORADE file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public NetcdfRadarDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
    }

    /**
     * Construct a new DORADE data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  sources   List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public NetcdfRadarDataSource(DataSourceDescriptor descriptor,
                                 List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, DATA_DESCRIPTION, properties);
    }

    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        //
        // just one category
        //
        List categories = Misc.newList(CATEGORY_SWEEP_2D);

        //
        // just the fixed angle for properties
        //
        NetcdfSweepfileAdapter nsa =
            (NetcdfSweepfileAdapter) getRadarAdapters().get(0);
        RealType[] paramTypes = (RealType[]) nsa.getParams();

        //
        // Add a DataChoice for each parameter we have available
        //
        for (int i = 0; i < paramTypes.length; i++) {
            String paramName = paramTypes[i].getName();
            addDataChoice(new DirectDataChoice(this, paramTypes[i],
                    paramName, paramName, categories,
                    DataChoice.NULL_PROPERTIES));
        }
    }

    /**
     * Make the RadarAdapter for this class
     *
     * @param source source of the data
     *
     * @return corresponding adapter
     *
     * @throws Exception problem opening the file or creating the data
     */
    protected RadarAdapter makeRadarAdapter(String source) throws Exception {
        NetcdfSweepfileAdapter adapter = new NetcdfSweepfileAdapter(this,
                                             source);
        //      System.err.println ("adapter:" + adapter.getScanMode() + " " + adapter.isPPI() + " " + adapter.isRHI() + " " + adapter.isSurvey());

        return adapter;
    }

    /**
     * Check to see if this <code>NetcdfRadarDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof NetcdfRadarDataSource)) {
            return false;
        }
        return (this == (NetcdfRadarDataSource) o);
    }

    /**
     * Get the hash code for this object.
     * @return hash code.
     */
    public int hashCode() {
        int hashCode = getName().hashCode();
        return hashCode;
    }

    /**
     * Test program
     *
     * @param args file name
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println(
                "Usage: NetcdfRadarDataSource <dorade_sweepfile>");
            System.exit(1);
        }

        try {
            DataSourceDescriptor dsDesc;
            dsDesc = new DataSourceDescriptor("NetCDF.SWEEP", "test label",
                    null, NetcdfRadarDataSource.class, "ncswp\\.*$", true,
                    false, null);
            NetcdfRadarDataSource dds = new NetcdfRadarDataSource(dsDesc,
                                            args[0], null);
            System.out.println("created " + dds.getName() + " ("
                               + dds.getDescription() + ")");

            // encode our NetcdfRadarDataSource to an XML string
            ucar.unidata.xml.XmlEncoder encoder =
                new ucar.unidata.xml.XmlEncoder();
            String xmlString = encoder.toXml(dds);
            System.out.println("saved to XML");

            // restore our data source from the XML string
            dds = (NetcdfRadarDataSource) encoder.toObject(xmlString);
            dds.initAfterUnpersistence();
            System.out.println("restored " + dds.getName() + " ("
                               + dds.getDescription() + ")");
            System.out.println("sweep time is "
                               + dds.doMakeDateTimes().get(0));
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }
}
