/*
 * $Id: ShapeFileDataSource.java,v 1.28 2007/04/16 20:34:52 jeffmc Exp $
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

import ucar.visad.data.MapSet;
import ucar.unidata.gis.shapefile.DbaseFile;
import ucar.unidata.gis.shapefile.DbaseData;

import ucar.unidata.util.CacheManager;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.JobManager;
import ucar.unidata.util.Misc;

import ucar.visad.ShapefileAdapter;
import ucar.visad.MapFamily;



import visad.*;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * DataSource for Shapefiles.
 *
 * @author IDV development team
 * @version $Revision: 1.28 $ $Date: 2007/04/16 20:34:52 $
 */
public class ShapeFileDataSource extends FilesDataSource {

    private static MapFamily mapFamily = new MapFamily("IDV maps");

    /** Property id for the dbfile */
    public static final String PROP_DBFILE = "PROP_DBFILE";

    /** The db file */
    private DbaseFile dbFile;

    /** The data. We cache this here ourselves */
    private Data shapefileData;


    /**
     * Dummy constructor so this object can get unpersisted.
     */
    public ShapeFileDataSource() {}


    /**
     * Create a ShapeFileDataSource from the specification given.
     *
     * @param descriptor          descriptor for the data source
     * @param source of file      file name (or directory)
     * @param properties          extra properties
     *
     * @throws VisADException     some problem occurred creating data
     */
    public ShapeFileDataSource(DataSourceDescriptor descriptor,
                               String source, Hashtable properties)
            throws VisADException {
        super(descriptor, source, "Shapefile data source", properties);
        initShapeFileDataSource();
    }


    public void reloadData() {
        shapefileData = null;
        dbFile=null;
        super.reloadData();
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
     * Initialize if being unpersisted.
     */
    public void initAfterUnpersistence() {
        //From a legacy bundle
        if (sources == null) {
            sources = Misc.newList(getName());
        }
        super.initAfterUnpersistence();
        initShapeFileDataSource();
    }


    /**
     * Initialization method
     */
    private void initShapeFileDataSource() {}

    /** categories */
    private List categories = DataCategory.parseCategories("GIS-SHAPEFILE",
                                  false);

    /**
     * Create the data choices associated with this source.
     */
    protected void doMakeDataChoices() {
        File file = new File(getSource());
        if (file.isDirectory()) {
            CompositeDataChoice composite = new CompositeDataChoice(this, "",
                                                getSource(), getSource(),
                                                null);
            walkTree(file, composite);
            addDataChoice(composite);
        } else {
            String name = getProperty(PROP_TITLE, (String) null);
            if (name == null) {
                name = getProperty(PROP_NAME, (String) null);
            }
            if (name == null) {
                name = IOUtil.getFileTail(getSource());
            }
            Hashtable props = Misc.newHashtable(DataChoice.PROP_ICON,
                                  "/auxdata/ui/icons/Map16.gif");
            addDataChoice(new DirectDataChoice(this, getSource(), name, name,
                    categories, props));
        }
    }

    /**
     * Walk a directory tree and create children DataChoices
     *
     * @param directory      directory to walk
     * @param parent         parent DataChoice
     */
    private void walkTree(File directory, CompositeDataChoice parent) {
        File[] subfiles = directory.listFiles();
        for (int i = 0; i < subfiles.length; i++) {
            if (subfiles[i].isDirectory()) {
                String dirName = IOUtil.getFileTail(subfiles[i].toString());
                CompositeDataChoice composite = new CompositeDataChoice(this,
                                                    "", dirName, dirName,
                                                    null);
                parent.addDataChoice(composite);
                walkTree(subfiles[i], composite);
            } else if (subfiles[i].toString().toLowerCase().endsWith(
                    ".shp")) {
                String name = subfiles[i].toString();
                String shortName =
                    IOUtil.stripExtension(IOUtil.getFileTail(name));
                parent.addDataChoice(new DirectDataChoice(this, name,
                        shortName, shortName, categories));
            }
        }
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
        byte[] bytes    = null;
        try {
            if (shapefileData == null) {

                //If its not a shp or zip file then try it with the mapFamily
                if (!IOUtil.hasSuffix(filename,".shp") &&
                    !IOUtil.hasSuffix(filename,".zip")) {
                    try {
                        URL url = IOUtil.getURL(filename, getClass());
                        shapefileData = (url == null)
                            ? (SampledSet) mapFamily.open(filename)
                            : (SampledSet) mapFamily.open(url);
                        
                        return shapefileData;
                    } catch (Exception exc) {
                        exc.printStackTrace();
                    }
                }
                




                if (getProperty(PROP_CACHEABLE, false)) {
                    bytes = CacheManager.getCachedFile("ShapeFileDataSource",
                            filename);
                }

                if (bytes == null) {
                    Object loadId =
                        JobManager.getManager().startLoad("Map File");
                    bytes = IOUtil.readBytes(IOUtil.getInputStream(filename,
                            getClass()), loadId);
                    JobManager.getManager().stopLoad(loadId);
                }

                if (bytes == null) {
                    return null;
                }


                ShapefileAdapter sfa =
                    new ShapefileAdapter(new ByteArrayInputStream(bytes, 0,
                        bytes.length), filename);


                dbFile = sfa.getDbFile();
                //If this is a .shp file then try to read in the dbf file
                if ((dbFile == null) && IOUtil.hasSuffix(filename,".shp")) {
                    InputStream dbfInputStream = null;
                    try {
                        String dbFilename = IOUtil.stripExtension(filename)
                                            + ".dbf";
                        dbfInputStream = IOUtil.getInputStream(dbFilename,
                                getClass());
                    } catch (Exception exc) {}
                    if (dbfInputStream != null) {
                        dbFile = new DbaseFile(dbfInputStream);
                        dbFile.loadHeader();
                        dbFile.loadData();
                    }
                }
                shapefileData = sfa.getData();
                setProperties(shapefileData, dbFile);
            }

            if ((requestProperties != null) && (dbFile != null)) {
                requestProperties.put(PROP_DBFILE, dbFile);
            }
            return shapefileData;
        } catch (Exception exc) {
            logException("Reading shapefile: " + filename, exc, bytes);
        }
        return null;
    }


    private void setProperties(Data shapefileData, DbaseFile dbFile) {
        if(dbFile==null || shapefileData == null || !(shapefileData instanceof UnionSet)) {
            return;
        }
        SampledSet[] sets       =((UnionSet)shapefileData).getSets();
        if(sets.length ==0 ||!(sets[0] instanceof MapSet)) {
            return;
        }
        int       numFields  = dbFile.getNumFields();
        List propertyNames = new ArrayList();
        for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
            String fieldName = dbFile.getFieldName(fieldIdx);
            propertyNames.add(fieldName);
        }

        //Some of these might be union sets
        for(int i=0;i<sets.length;i++) {
            if(sets[i] instanceof MapSet) {
                MapSet mapSet = (MapSet) sets[i];
                mapSet.setPropertyNames(propertyNames);
            }
        }

        for (int fieldIdx = 0; fieldIdx < numFields; fieldIdx++) {
            String fieldName = dbFile.getFieldName(fieldIdx);
            DbaseData dbData    = dbFile.getField(fieldIdx);
            List      values = dbData.asList();
            if(values.size()!=sets.length) {
                throw new IllegalArgumentException ("DBfile size:" + values.size() + " != number of map lines:" + sets.length);
            }
            for(int i=0;i<sets.length;i++) {
                if(sets[i] instanceof MapSet) {
                    MapSet mapSet = (MapSet) sets[i];
                    mapSet.setProperty(fieldName, values.get(i));
                }
            }
        }


    }


    /**
     * See if this DataSource should cache or not
     *
     * @param data   Data to cache
     * @return  false
     */
    protected boolean shouldCache(Data data) {
        return false;
    }



    /**
     * Create a list of times for this data source.  Since shapefiles
     * don't have any times, return an empty List.
     *
     * @return  an empty List
     */
    protected List doMakeDateTimes() {
        return new ArrayList();
    }



}

