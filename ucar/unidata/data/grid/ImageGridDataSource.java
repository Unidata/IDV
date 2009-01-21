/**
 * $Id: GeoGridDataSource.java,v 1.179 2007/06/18 22:28:35 dmurray Exp $
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




package ucar.unidata.data.grid;


import ucar.unidata.data.*;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;

import java.util.List;




/**
 * Handles gridded files
 *
 * @author IDV Development Team
 * @version $Revision: 1.179 $
 */

public class ImageGridDataSource extends GeoGridDataSource {

    /** List of 2D categories for grids */
    private List twoDCategories;


    /** List of 2D categories for time series of grids */
    private List twoDTimeSeriesCategories;


    /**
     * Default constructor
     */
    public ImageGridDataSource() {}

    /**
     * Create a GeoGridDataSource from a File.
     *
     * @param descriptor   Describes this data source, has a label etc.
     * @param file         This is the file that points to the actual
     *                     data source.
     * @param properties   General properties used in the base class
     *
     * @throws IOException  problem opening file
     */
    public ImageGridDataSource(DataSourceDescriptor descriptor, File file,
                               Hashtable properties)
            throws IOException {
        super(descriptor, file, properties);
    }



    /**
     * Create a GeoGridDataSource from the filename.
     * @param descriptor   Describes this data source, has a label etc.
     * @param filename     This is the filename (or url) that points
     *                     to the actual data source.
     * @param properties   General properties used in the base class
     *
     * @throws IOException
     */
    public ImageGridDataSource(DataSourceDescriptor descriptor,
                               String filename, Hashtable properties)
            throws IOException {
        super(descriptor, filename, properties);
    }


    /**
     * Create a GeoGridDataSource from the filename.
     * @param descriptor   Describes this data source, has a label etc.
     * @param files List of files or urls
     * @param properties   General properties used in the base class
     *
     * @throws IOException
     */
    public ImageGridDataSource(DataSourceDescriptor descriptor, List files,
                               Hashtable properties)
            throws IOException {
        super(descriptor, files, properties);
    }


    /**
     * make the category list if needed
     */
    private void makeImageGridCategories() {
        if (twoDCategories == null) {
            twoDTimeSeriesCategories =
                DataCategory.parseCategories("IMAGE-2D-TIME;", false);
            twoDCategories = DataCategory.parseCategories("IMAGE-2D;", false);
        }
    }


    /**
     * Get the 2D data categories
     * @return   list of categories
     */
    public List getTwoDCategories() {
        makeImageGridCategories();
        return twoDCategories;
    }



    /**
     * Get the list of 2D time series categories
     * @return   list of categories
     */
    public List getTwoDTimeSeriesCategories() {
        makeImageGridCategories();
        return twoDTimeSeriesCategories;
    }


}

