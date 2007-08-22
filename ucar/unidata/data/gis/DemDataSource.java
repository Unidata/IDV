/*
 * $Id: DemDataSource.java,v 1.16 2007/04/16 20:34:52 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.gis;


import ucar.unidata.data.*;


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import ucar.visad.UTMCoordinateSystem;



import visad.*;

import visad.data.*;
import visad.data.gis.ArcAsciiGridForm;
import visad.data.gis.UsgsDemForm;

import java.awt.geom.Rectangle2D;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.MalformedURLException;



import java.net.URL;


import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * DataSource for Digital Elevation Model (DEM) files. Handles USGS
 * 7.5 minute DEMs, and Arc ASCIIGRID files.
 *
 * @author IDV development team
 * @version $Revision: 1.16 $
 */
public class DemDataSource extends FilesDataSource {

    /** the DEM reader */
    MyDemFamily dem;

    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public DemDataSource() {}


    /**
     * Create a DemDataSource from the specification given.
     *
     * @param descriptor              description of the source
     * @param source of file          filename
     * @param properties              extra properties
     *
     * @throws VisADException     VisAD problem
     */
    public DemDataSource(DataSourceDescriptor descriptor, String source,
                         Hashtable properties)
            throws VisADException {
        super(descriptor, Misc.newList(source), source, "DEM data source",
              properties);
        initDemDataSource();
    }


    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased();
    }

    /**
     * Initialize after we have been unpersisted
     */
    public void initAfterUnpersistence() {
        super.initAfterUnpersistence();
        initDemDataSource();
    }

    /**
     * Initialize the datasource
     */
    private void initDemDataSource() {
        dem = new MyDemFamily();
    }

    /** list of data categories */
    private List categories = DataCategory.parseCategories("DEM;GRID-2D;");


    /**
     * Make the {@link DataChoice}s associated with this source.
     */
    protected void doMakeDataChoices() {
        addDataChoice(new DirectDataChoice(this, sources.get(0), "Elevation",
                                           "Elevation", categories));
    }

    /**
     * Actually get the data identified by the given DataChoce. The default is
     * to call the getDataInner that does not take the requestProperties. This
     * allows other, non unidata.data DataSource-s (that follow the old API)
     * to work.
     *
     * @param dataChoice        The data choice that identifies the requested
     *                          data.
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return The visad.Data object
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        String filename = (String) dataChoice.getId();
        try {
            return makeDemData(filename);
        } catch (Exception exc) {
            logException("Reading DEM: " + filename, exc);
        }
        return null;
    }


    /**
     * Make the list of available times for this data source.
     *
     * @return  list of available data times
     */
    protected List doMakeDateTimes() {
        return new ArrayList();
    }



    /**
     * Get the data
     *
     * @param filename    filename
     * @return  the associated Data
     *
     * @throws Exception   problem accessing file or creating data
     */
    private FieldImpl makeDemData(String filename) throws Exception {
        FieldImpl fi = (FieldImpl) getCache(filename);
        if (fi == null) {
            String dataFile = filename;
            try {
                //See if its a URL
                LogUtil.message("Copying DEM file to local disk");
                URL url = new URL(filename);
                //If it is then get the bites and copy them to a temp file
                String tail = Misc.getUniqueId()
                              + IOUtil.getFileTail(filename);
                String tmpFile =
                    getDataContext().getIdv().getStore().getTmpFile(tail);
                FileOutputStream fos = new FileOutputStream(tmpFile);
                IOUtil.writeTo(IOUtil.getInputStream(filename, getClass()),
                               fos);
                fos.close();
                dataFile = tmpFile;
            } catch (MalformedURLException exc) {}
            LogUtil.message("Reading DEM file: "
                            + IOUtil.getFileTail(filename));
            fi = (FieldImpl) dem.open(dataFile);
            putCache(filename, fi);
        }
        return fi;
    }




    /**
     * A container for all the supported DEM types.  Currently, USGS
     * DEM and Arc ASCIIGRID formats are supported.
     * @author Don Murray
     * @version $Revision: 1.16 $ $Date: 2007/04/16 20:34:52 $
     */
    private static class MyDemFamily extends FunctionFormFamily {

        /**
         * Construct a family of the supported map datatype Forms
         */
        public MyDemFamily() {
            super("dem");
            forms.add(new UsgsDemForm());
            forms.add(
                new ArcAsciiGridForm(RealTupleType.SpatialEarth2DTuple));
        }


        /**
         * Determines if this is a DEM file from the name
         * @param  name  name of the file
         * @return  true if it matches the pattern for USGS DEM files
         */
        public boolean isThisType(String name) {
            return false;
        }

        /**
         * Determines if this is a USGS DEM file from the starting block
         * @param  block  block of data to check
         * @return  false  - there is no identifying block in a USGS DEM file
         */
        public boolean isThisType(byte[] block) {
            return false;
        }


        /**
         * Get a list of default suffixes for McIDAS map files
         * @return  valid list of suffixes
         */
        public String[] getDefaultSuffixes() {
            String[] suff = { ".dem", ".asc" };
            return suff;
        }


    }




}

