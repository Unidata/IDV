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

package ucar.unidata.data.imagery;


import edu.wisc.ssec.mcidas.AreaDirectory;
import edu.wisc.ssec.mcidas.AreaDirectoryList;

import ucar.unidata.data.*;

import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import visad.Data;

import visad.DataReference;
import visad.DateTime;
import visad.VisADException;

import visad.data.mcidas.AreaAdapter;

import visad.meteorology.SingleBandedImage;



import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;



/**
 * A data source for ADDE images AREA files. This is a thin wrapper (derived class) around the ImageDataSource
 * which does all of the work.
 *
 * @author Don Murray
 * @version $Revision: 1.26 $ $Date: 2006/12/01 20:42:05 $
 */
public class McIDASImageDataSource extends ImageDataSource {

    /**
     *  The parameterless ctor unpersisting.
     */
    public McIDASImageDataSource() {}


    /**
     *  Create a new McIDASImageDataSource with  a single AREA file.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  image AREA file
     *  @param properties The properties for this data source.
     */

    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 String image, Hashtable properties) {
        super(descriptor, new String[] { image }, properties);
    }


    /**
     *  Create a new McIDASImageDataSource with list of  AREA files.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images List of AREA files
     *  @param properties The properties for this data source.
     */
    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 ArrayList images, Hashtable properties) {
        super(descriptor, StringUtil.listToStringArray(images), properties);
    }


    /**
     *  Create a new McIDASImageDataSource with array of  AREA files.
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  images Array of AREA files
     *  @param properties The properties for this data source.
     */
    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 String[] images, Hashtable properties) {
        super(descriptor, images, properties);
    }

    /**
     *  Create a new McIDASImageDataSource with a {@link ImageDataset}
     *
     *  @param descriptor The descriptor for this data source.
     *  @param  ids The dataset.
     *  @param properties The properties for this data source.
     */
    public McIDASImageDataSource(DataSourceDescriptor descriptor,
                                 ImageDataset ids, Hashtable properties) {
        super(descriptor, ids, properties);
    }

    /**
     *  Overwrite base class  method to return the name of this class.
     *
     *  @return The name.
     */
    public String getImageDataSourceName() {
        return "McIDAS Image dataset";
    }


    /**
     * Get the name for the main data object
     *
     * @return name of main data object
     */
    public String getDataName() {
        /*  TODO: Flesh this out
        List images = getImages();
        if (images != null) {
            Object o = images.get(0);
            if (o instanceof AddeImageDescriptor) {
                AreaDirectory ad = ((AddeImageDescriptor) o).getDirectory();
                return "Band " + ad.getBands()[0];
            }
        }
        */
        return "All Images";
    }

}
