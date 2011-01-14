/*
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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




package ucar.unidata.data.text;


import ucar.unidata.data.BadDataException;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.FilesDataSource;

import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.DateSelection;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import visad.*;

import java.io.File;
import java.io.FileInputStream;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;




/**
 * A class for handling text products (e.g. weather bulletins)
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public abstract class TextProductDataSource extends FilesDataSource {

    /**
     * Default bean constructor; does nothing.
     *
     */
    public TextProductDataSource() {}


    /**
     * Ctor
     *
     * @param descriptor The descriptor
     * @param name The name
     * @param description The long name
     * @param properties properties
     */
    public TextProductDataSource(DataSourceDescriptor descriptor,
                                 String name, String description,
                                 Hashtable properties) {
        super(descriptor, name, description, properties);
    }


    /**
     * Ctor
     *
     * @param descriptor The descriptor
     * @param newSources List of files or urls
     * @param description The long name
     * @param properties properties
     */
    public TextProductDataSource(DataSourceDescriptor descriptor,
                                 List newSources, String description,
                                 Hashtable properties) {
        super(descriptor, newSources, description, properties);
    }


    /**
     * Create a TrackDataSource from the specification given.
     *
     * @param descriptor    data source descriptor
     * @param newSources       List of sources of data (filename/URL)
     * @param name my name
     * @param description   description of the data
     * @param properties    extra properties for initialization
     */
    public TextProductDataSource(DataSourceDescriptor descriptor,
                                 List newSources, String name,
                                 String description, Hashtable properties) {
        super(descriptor, newSources, name, description, properties);
    }




    /**
     * Read the products for the product type and station
     *
     * @param productType the product type
     * @param stations the stations
     * @param dateSelection  the date selection
     *
     * @return the list of products
     */
    public abstract List<Product> readProducts(ProductType productType,
            List<NamedStationImpl> stations, DateSelection dateSelection);

    /**
     * Get the stations for a productType
     *
     * @param productType  the product type
     * @param dateSelection  the date selection
     *
     * @return  the list of stations
     *
     * @throws Exception problem getting the stations
     */
    public abstract NamedStationTable getStations(ProductType productType,
            DateSelection dateSelection)
     throws Exception;


    /**
     * Make a mapping of station name to integer count
     *
     * @param stations   list of stations
     *
     * @return  mapping of station name to integer count
     */
    protected static Hashtable<String, Integer> makeStationMap(
            List<NamedStationImpl> stations) {
        Hashtable<String, Integer> ids = new Hashtable<String, Integer>();
        if ((stations != null) && (stations.size() > 0)) {
            for (NamedStationImpl station : stations) {
                ids.put(station.getID(), new Integer(0));
            }
        }
        return ids;
    }


    /**
     * Get the list of product groups
     *
     * @return  the list of product groups
     */
    public abstract List<ProductGroup> getProductGroups();


    /**
     * Make the data choices
     */
    protected void doMakeDataChoices() {
        String category = "textproducts";
        String docName  = getName();
        addDataChoice(
            new DirectDataChoice(
                this, docName, docName, docName,
                DataCategory.parseCategories(category, false)));
    }



}

