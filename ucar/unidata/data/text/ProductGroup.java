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

    /** _more_          */
    private String name;

    /** _more_          */
    private List<Product> products = new ArrayList<Product>();

    public ProductGroup() {
    }

    /**
     * _more_
     *
     * @param name _more_
     */
    public ProductGroup(String name) {
        this.name = name;
    }

    public boolean equals(Object o) {
        if(!(o instanceof ProductGroup)) return false;
        return Misc.equals(name, ((ProductGroup)o).name);
    }

    /**
     * _more_
     *
     * @param file _more_
     *
     * @throws Exception _more_
     */
    public static List<ProductGroup> parse(String file) throws Exception {
        String contents = IOUtil.readContents(file, StringUtil.class);
        contents = contents.replace("{", "\n{\n");
        contents = contents.replace("}", "\n}\n");
        List<String> lines = (List<String>) StringUtil.split(contents, "\n",
                                 true, true);
        List<ProductGroup>   products     = new ArrayList<ProductGroup>();
        ProductGroup productGroup = null;
        boolean      inProduct    = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (productGroup != null) {
                if (line.equals("}")) {
                    productGroup = null;
                } else if (line.equals("{")) {
                    //NOOP
                } else {
                    String[] toks = StringUtil.split(line, "|", 2);
                    if ((toks == null) || (toks.length != 2)) {
                        throw new IllegalArgumentException("Bad line:"
                                + line);
                    }
                    if (toks[0].startsWith("(")) {
                        continue;
                    }
                    if (toks[0].startsWith("!")) {
                        continue;
                    }
                    productGroup.addProduct(new Product(toks[0].replace("_",
                            " "), toks[1]));
                }
            } else if (line.equals("{")) {
                productGroup = null;
            } else {
                productGroup = new ProductGroup(line.replace("_"," "));
                products.add(productGroup);
            }
        }
        return products;
    }

    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        parse(args[0]);
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
     * _more_
     *
     * @param product _more_
     */
    public void addProduct(Product product) {
        products.add(product);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public List<Product> getProducts() {
        return products;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name;
    }
}

