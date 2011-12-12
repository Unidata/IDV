/*
 * Copyright 1997-2011 Unidata Program Center/University Corporation for
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

    /** North attribute */
    public static final String ATTR_NORTH = "north";

    /** south attribute */
    public static final String ATTR_SOUTH = "south";

    /** east attribute */
    public static final String ATTR_EAST = "east";

    /** west attribute */
    public static final String ATTR_WEST = "west";

    /** x attribute */
    public static final String ATTR_X = "x";

    /** y attribute */
    public static final String ATTR_Y = "y";

    /** z attribute */
    public static final String ATTR_Z = "z";


    /** List of 2D categories for grids */
    private List twoDCategories;

    /** List of 3D categories for grids */
    private List threeDCategories;

    /** List of 2D categories for time series of grids */
    private List twoDTimeSeriesCategories;

    /** List of 2D ensemble categories for time series of grids */
    private List twoDEnsTimeSeriesCategories;

    /** List of 3D categories for time series of grids */
    private List threeDTimeSeriesCategories;

    /** List of 3D ensemble categories for time series of grids */
    private List threeDEnsTimeSeriesCategories;

    /** List of ens categories for grids */
    private DataCategory ensDCategory;

    /** grid ensemble members */
    public static final String PROP_ENSEMBLEMEMBERS = "prop.gridmembers";

    /**
     * Default constructor; initializes data categories
     */
    public GridDataSource() {
        initCategories();
    }



    /**
     * Create a GridDataSource from the descriptor
     *
     * @param descriptor  the descriptor
     */
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
        if (twoDTimeSeriesCategories == null) {
            twoDTimeSeriesCategories =
                DataCategory.parseCategories("2D grid;GRID-2D-TIME;");
            twoDEnsTimeSeriesCategories = DataCategory.parseCategories(
                "2D grid;GRID-2D-TIME;ENSEMBLE;");
            twoDCategories = DataCategory.parseCategories("2D grid;GRID-2D;");
            threeDTimeSeriesCategories =
                DataCategory.parseCategories("3D grid;GRID-3D-TIME;");
            threeDEnsTimeSeriesCategories = DataCategory.parseCategories(
                "3D grid;GRID-3D-TIME;ENSEMBLE;");
            threeDCategories =
                DataCategory.parseCategories("3D grid;GRID-3D;");
            ensDCategory = DataCategory.parseCategory("ENSEMBLE", true);
        }
    }

    /**
     * Get the ensemble data categories
     * @return   list of categories
     */
    public DataCategory getEnsDCategory() {
        return ensDCategory;
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
     * Get the list of 2D time series ensemble categories
     * @return   list of categories
     */
    public List getTwoDEnsTimeSeriesCategories() {
        return twoDEnsTimeSeriesCategories;
    }

    /**
     * Get the list of 3D time series categories
     * @return   list of categories
     */
    public List getThreeDTimeSeriesCategories() {
        return threeDTimeSeriesCategories;
    }

    /**
     * Get the list of 3D time series ensemble categories
     * @return   list of categories
     */
    public List getThreeDEnsTimeSeriesCategories() {
        return threeDEnsTimeSeriesCategories;
    }

    /**
     *  Set the ensemble selection
     *
     *  @param ensMembers  the ensemble memeber selection for this datasource
     */
    public void setEnsembleSelection(List<Integer> ensMembers) {
        if (ensMembers != null) {
            getProperties().put(PROP_ENSEMBLEMEMBERS, ensMembers);
        }
    }

    /**
     *  Get the ensemble selection
     *
     * @return the ensemble selection for this datasource or null
     */
    public List<Integer> getEnsembleSelection() {
        return (List<Integer>) getProperties().get(PROP_ENSEMBLEMEMBERS);
    }

}
