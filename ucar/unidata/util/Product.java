/*
 * 
 * Copyright 1997-2009 Unidata Program Center/University Corporation for
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

package ucar.unidata.util;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: May 3, 2007
 * Time: 11:40:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class Product {

    /** _more_          */
    private String id;

    /** _more_          */
    private String name;

    /**
     * _more_
     */
    public Product() {}

    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     */
    public Product(String id, String name) {
        this.id   = id;
        this.name = name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getID() {
        return this.id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return this.name;
    }

    /**
     * _more_
     *
     * @param id _more_
     */
    public void setID(String id) {
        this.id = id;
    }

    /**
     * _more_
     *
     * @param name _more_
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * _more_
     *
     * @param oo _more_
     *
     * @return _more_
     */
    public boolean equals(Object oo) {

        if ( !(oo instanceof Product)) {
            return false;
        }
        Product that = (Product) oo;

        return this.id.equals(that.id);
    }

}

