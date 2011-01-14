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


import ucar.unidata.util.Misc;

import java.util.ArrayList;
import java.util.List;


/**
 * Holds a named product
 *
 * @author IDV development team
 * @version $Revision: 1.15 $
 */

public class ProductType {

    /** the name */
    private String name;

    /** the id */
    private String id;

    /** Should this product type be rendered as html   */
    private boolean renderAsHtml = false;


    /**
     * Create a new product type
     */
    public ProductType() {}

    /**
     * Create a new ProductType
     *
     * @param name  the name
     * @param id    the id
     */
    public ProductType(String name, String id) {
        this.name = name;
        this.id   = id;
    }

    /**
     * See if this equals o
     *
     * @param o  Object to check
     *
     * @return  true if they are equal
     */
    public boolean equals(Object o) {
        if ( !(o instanceof ProductType)) {
            return false;
        }
        return Misc.equals(id, ((ProductType) o).id);
    }


    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     *  Set the Id property.
     *
     *  @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     *  Get the Id property.
     *
     *  @return The Id
     */
    public String getId() {
        return id;
    }


    /**
     * Get a String representation
     *
     * @return a String representation
     */
    public String toString() {
        return name;
    }

    /**
     *  Set the RenderAsHtml property.
     *
     *  @param value The new value for RenderAsHtml
     */
    public void setRenderAsHtml(boolean value) {
        renderAsHtml = value;
    }

    /**
     *  Get the RenderAsHtml property.
     *
     *  @return The RenderAsHtml
     */
    public boolean getRenderAsHtml() {
        return renderAsHtml;
    }



}

