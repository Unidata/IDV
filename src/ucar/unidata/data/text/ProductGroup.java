/*
 * $Id: FrontDataSource.java,v 1.15 2007/04/17 22:22:52 jeffmc Exp $
 *
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


import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import java.util.ArrayList;
import java.util.List;


/**
 * Holds a named group of products
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */
public class ProductGroup {

    /** the name */
    private String name;

    /** a list of names */
    private List<ProductType> productTypes = new ArrayList<ProductType>();

    /**
     * The default ctor
     */
    public ProductGroup() {}

    /**
     * Create a new product group
     *
     * @param name the name of the group
     */
    public ProductGroup(String name) {
        this.name = name;
    }

    /**
     * See if this equals another
     *
     * @param o  the other
     *
     * @return  true if they are equal
     */
    public boolean equals(Object o) {
        if ( !(o instanceof ProductGroup)) {
            return false;
        }
        return Misc.equals(name, ((ProductGroup) o).name);
    }


    /**
     * Test this
     *
     * @param args  the file
     *
     * @throws Exception problem
     */
    public static void main(String[] args) throws Exception {
        //        parse(args[0]);
    }

    /*

Observed_Data
{
    Surface_Hourlies|SFC_HRLY
    Sounding_Data|SND_DATA
    Synoptic_Data|SYN_DATA
    Agriculture_Obs|AGRI_OBS
    TAFs_Decoded|TAFS_DEC
    RADAT|FZL_LVL
}*/




    /**
     * Add a product to the group
     *
     * @param product  the product to add
     */
    public void addProduct(ProductType product) {
        productTypes.add(product);
    }

    /**
     * Get the list of products in this group
     *
     * @return the list of products in this group
     */
    public List<ProductType> getProductTypes() {
        return productTypes;
    }

    /**
     * Get a String representation
     *
     * @return a String representation
     */
    public String toString() {
        return name;
    }
}

