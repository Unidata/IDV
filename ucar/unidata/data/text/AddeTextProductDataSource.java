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


import edu.wisc.ssec.mcidas.adde.AddeTextReader;
import edu.wisc.ssec.mcidas.adde.WxTextProduct;

import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import visad.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A class for handling text ADDE test
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class AddeTextProductDataSource extends NwxTextProductDataSource {

    /**
     * Default bean constructor; does nothing.
     *
     */
    public AddeTextProductDataSource() {}

    /**
     *
     * @param descriptor    descriptor for this DataSource
     * @param filename      name of the file (or URL)
     * @param properties    extra data source properties
     */
    public AddeTextProductDataSource(DataSourceDescriptor descriptor,
                                     String filename, Hashtable properties) {
        this(descriptor, Misc.newList(filename), properties);
    }


    /**
     *
     * @param descriptor    Descriptor for this DataSource
     * @param files         List of files or urls
     * @param properties    Extra data source properties
     */
    public AddeTextProductDataSource(DataSourceDescriptor descriptor,
                                     List files, Hashtable properties) {
        super(descriptor, files, "ADDE Text Products", "ADDE Text Products",
              properties);
    }

    protected List<Product> readProducts(TableInfo tableInfo,
                                       NamedStationImpl station) {
        List<Product> products = new ArrayList<Product>();
        if (station == null) {
            return products;
        }
        String url = "adde://adde.ucar.edu/wxtext?num=100&"
                     + getSearch(tableInfo, station);
        try {
            AddeTextReader      atr   = new AddeTextReader(url);
            List<WxTextProduct> prods = atr.getWxTextProducts();
            for (Iterator itera = prods.iterator(); itera.hasNext(); ) {
                WxTextProduct wtp = (WxTextProduct) itera.next();
                products.add(new Product(wtp.getWstn(), wtp.getText(),
                                         wtp.getDate()));
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return products;
    }

    /**
     * Get the search string
     *
     * @param pt   product type
     * @param station  station
     *
     * @return the search string
     */
    private String getSearch(TableInfo ti, NamedStationImpl station) {
        if (station == null) {
            return "";
        }
        StringBuilder buf = new StringBuilder("WMO=");
        buf.append(station.getProperty("BULLETIN", ""));
        buf.append("&WSTN=");
        buf.append(station.getID());
        return buf.toString();
    }

    /**
     * Get the table path
     * @return the base path of the data.
     */
    protected String getTablePath() {
        return "http://www.unidata.ucar.edu/software/idv/resources";
    }

    /**
     * Set the additional resources needed for this to work.
     * @return  true if resources set okay
     */
    protected boolean setAdditionalResources() {
        return true;
    }

    /**
     * Get the error message if additional resources aren't available
     * @return  error messaage
     */
    public String getAdditionalResourcesError() {
        return "";
    }

}

