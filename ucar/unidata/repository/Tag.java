/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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



package ucar.unidata.repository;


import ucar.unidata.util.Misc;

import java.io.File;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class Tag {

    /** _more_ */
    private String name;

    /** _more_ */
    private int count = -1;

    /**
     * _more_
     *
     * @param name _more_
     */
    public Tag(String name) {
        this.name = name;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param count _more_
     */
    public Tag(String name, int count) {
        this.name  = name;
        this.count = count;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the Count property.
     *
     * @param value The new value for Count
     */
    public void setCount(int value) {
        count = value;
    }

    /**
     * Get the Count property.
     *
     * @return The Count
     */
    public int getCount() {
        return count;
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

