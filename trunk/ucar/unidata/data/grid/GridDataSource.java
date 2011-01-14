/*
 * $Id: GridDataSource.java,v 1.18 2007/04/20 13:54:08 dmurray Exp $
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

/**
 * DataSource for Grid files.
 *
 * @author Metapps development team
 * @version $Revision: 1.18 $ $Date: 2007/04/20 13:54:08 $
 */

package ucar.unidata.data.grid;


import ucar.unidata.data.*;

import ucar.unidata.util.Misc;


import ucar.unidata.xml.XmlEncoder;

import visad.VisADException;

import java.rmi.RemoteException;



import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.List;


/**
 *  An abstract  class that provides a list of 2d and 3d DataCategory objects
 *  for   grid data sources.
 */
public abstract class GridDataSource extends FilesDataSource {

    public static final String ATTR_NORTH = "north";
    public static final String ATTR_SOUTH = "south";
    public static final String ATTR_EAST = "east";
    public static final String ATTR_WEST = "west";

    public static final String ATTR_X = "x";
    public static final String ATTR_Y = "y";
    public static final String ATTR_Z = "z";


    /** List of 2D categories for grids */
    private List twoDCategories;

    /** List of 3D categories for grids */
    private List threeDCategories;

    /** List of 2D categories for time series of grids */
    private List twoDTimeSeriesCategories;

    /** List of 3D categories for time series of grids */
    private List threeDTimeSeriesCategories;


    /**
     * Default constructor; initializes data categories
     */
    public GridDataSource() {
        initCategories();
    }



    public GridDataSource(DataSourceDescriptor descriptor) {
        super(descriptor);
        initCategories();
    }

    /**
     * Create a GridDataSource from the specification given.
     *
     * @param descriptor       data source descriptor
     * @param source of file   filename or URL
     * @param name             name of this data source
     * @param properties       extra initialization properties
     */
    public GridDataSource(DataSourceDescriptor descriptor, String source,
                          String name, Hashtable properties) {
        super(descriptor, Misc.newList(source), source, name, properties);
        initCategories();
    }


    /**
     * Create a GridDataSource from the specification given.
     *
     * @param descriptor       data source descriptor
     * @param sources          List of files or URLS
     * @param name             name of this data source
     * @param properties       extra initialization properties
     */
    public GridDataSource(DataSourceDescriptor descriptor, List sources,
                          String name, Hashtable properties) {
        super(descriptor, sources, name, properties);
        initCategories();
    }



    /**
     * Initialize the data categories
     */
    public void initCategories() {
        if(twoDTimeSeriesCategories==null) {
            twoDTimeSeriesCategories =
                DataCategory.parseCategories("2D grid;GRID-2D-TIME;");
            twoDCategories = DataCategory.parseCategories("2D grid;GRID-2D;");
            threeDTimeSeriesCategories =
                DataCategory.parseCategories("3D grid;GRID-3D-TIME;");
            threeDCategories = DataCategory.parseCategories("3D grid;GRID-3D;");
        }
    }




    /**
     * Get the 2D data categories
     * @return   list of categories
     */
    public List getTwoDCategories() {
        return twoDCategories;
    }

    /**
     * Get the 3D data categories
     * @return   list of categories
     */
    public List getThreeDCategories() {
        return threeDCategories;
    }


    /**
     * Get the list of 2D time series categories
     * @return   list of categories
     */
    public List getTwoDTimeSeriesCategories() {
        return twoDTimeSeriesCategories;
    }

    /**
     * Get the list of 3D time series categories
     * @return   list of categories
     */
    public List getThreeDTimeSeriesCategories() {
        return threeDTimeSeriesCategories;
    }



}

