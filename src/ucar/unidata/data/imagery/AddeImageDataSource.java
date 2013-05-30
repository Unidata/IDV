/*
 * Copyright 1997-2013 Unidata Program Center/University Corporation for
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

package ucar.unidata.data.imagery;


import ucar.unidata.data.*;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;

import visad.Data;
import visad.DataReference;
import visad.VisADException;

import visad.data.mcidas.AreaAdapter;

import visad.meteorology.SingleBandedImage;

import java.io.RandomAccessFile;


import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A data source for ADDE images. This is a thin wrapper (derived class) around the ImageDataSource
 * which does all of the work.
 *
 * @author Don Murray
 * @version $Revision: 1.56 $ $Date: 2007/07/05 18:46:09 $
 */

public class AddeImageDataSource extends ImageDataSource {


    /**
     *  The parameterless ctor unpersisting.
     */
    public AddeImageDataSource() {}

    /**
     *  Create a new AddeImageDataSource with an a single image ADDE url.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  image ADDE Url
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
    public AddeImageDataSource(DataSourceDescriptor descriptor, String image,
                               Hashtable properties)
            throws VisADException {
        super(descriptor, new String[] { image }, properties);
    }

    /**
     *  Create a new AddeImageDataSource with an array (String) image ADDE urls.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images Array of  ADDE urls.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */

    public AddeImageDataSource(DataSourceDescriptor descriptor,
                               String[] images, Hashtable properties)
            throws VisADException {
        super(descriptor, images, properties);
    }

    /**
     *  Create a new AddeImageDataSource with an array (String) image ADDE urls.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images Array of  ADDE urls.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */

    public AddeImageDataSource(DataSourceDescriptor descriptor, List images,
                               Hashtable properties)
            throws VisADException {
        super(descriptor, images, properties);
    }


    /**
     *  Create a new AddeImageDataSource with the given dataset.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  ids The dataset.
     *  @param properties The properties for this data source.
     *
     * @throws VisADException
     */
    public AddeImageDataSource(DataSourceDescriptor descriptor,
                               ImageDataset ids, Hashtable properties)
            throws VisADException {
        super(descriptor, ids, properties);
    }

    /**
     *  Overwrite base class  method to return the name of this class.
     *
     *  @return The name.
     */
    public String getImageDataSourceName() {
        return "Adde Image Data Source";
    }

    /**
     * Get the name for this data.  Override base class for more info.
     *
     * @return  name for the main data object
     */
    public String getDataName() {
        String dataName =
            (String) getProperty(
                ucar.unidata.idv.chooser.adde.AddeChooser.DATA_NAME_KEY,
                (String) null);
        if (dataName == null) {
            dataName = (String) getProperty(
                ucar.unidata.idv.chooser.adde.AddeChooser.PROP_DATANAME,
                (String) null);
        }

        if ((dataName == null) || dataName.trim().equals("")) {
            dataName = super.getDataName();
        }
        return dataName;

    }

    /**
     * Save files to local disk
     *
     * @param prefix destination dir and file prefix
     * @param loadId For JobManager
     * @param changeLinks Change internal file references
     *
     * @return Files copied
     *
     * @throws Exception On badness
     */
    protected List saveDataToLocalDisk(String prefix, Object loadId,
                                       boolean changeLinks)
            throws Exception {
        List newFiles = super.saveDataToLocalDisk(prefix, loadId,
                            changeLinks);
        if (newFiles == null) {
            return newFiles;
        }
        // write 0 as the first word
        for (int i = 0; i < newFiles.size(); i++) {
            try {
                RandomAccessFile to =
                    new RandomAccessFile((String) newFiles.get(i), "rw");
                to.seek(0);
                to.writeInt(0);
                to.close();
            } catch (Exception e) {
                System.out.println("unable to set first word to 0");
            }
        }
        return newFiles;
    }
    
    /**
     * For cases where each data value has an English meaning,
     * (e.g., quality flags).  Useful for cursor readouts.
     * 
     * For AddeImageDataSource, used to return a description corresponding
     * to values of the NEXRAD L3 Hydrometeor Classification product.
     * 
     * @param val  the data value to translae
     * @param dataChoiceName to determine what translation table to use
     * @return the string translation of the data point 
     */
    public String getStringForDataValue(int val, String dataChoiceName) {
        if (dataChoiceName.startsWith("7_Band16")) {
            // this is the Hydrometeor Classification product
            // list of codes found here:
            // (51.2.2) http://www.roc.noaa.gov/wsr88d/PublicDocs/ICDs/2620003R.pdf
            String str;
            switch (val) {
                case 0:  str = "SNR&lt;Threshold";  // black
                         break;
                case 10:  str = "Biological";  // medium gray
                         break;
                case 20:  str = "AP/Ground Clutter";  // dark gray
                         break;
                case 30:  str = "Ice Crystals";  // light pink
                         break;
                case 40:  str = "Dry Snow";  // light blue
                         break;
                case 50:  str = "Wet Snow";  // medium blue
                         break;
                case 60:  str = "Light-Moderate Rain";  // light green
                         break;
                case 70:  str = "Heavy Rain";  // medium green
                         break;
                case 80:  str = "Big Drops Rain";  // dark yellow
                         break;
                case 90:  str = "Graupel";  // medium pink
                         break;
                case 100: str = "Hail, Possibly With Rain";  // red
                         break;
                // classification algorithm reports "unknown type" here.
                // How to distinguish this from "McV doesnt understand the code"?
                case 140: str = "Unknown Type";  // purple
                         break;
                case 150: str = "RF";  // dark purple
                         break;
                default: str = "No translation available";
                         break;
            }
            return str;
        } else {
            return null;
        }
    }

}
