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


import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import java.util.ArrayList;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;


/**
 * Class Entry _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Association {

    /** _more_ */
    private String name;


    /** _more_ */
    private String fromId;

    /** _more_ */
    private String toId;


    /**
     * _more_
     *
     * @param name _more_
     * @param fromId _more_
     * @param toId _more_
     */
    public Association(String name, String fromId, String toId) {
        this.name   = name;
        this.fromId = fromId;
        this.toId   = toId;
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
     * Set the FromId property.
     *
     * @param value The new value for FromId
     */
    public void setFromId(String value) {
        fromId = value;
    }

    /**
     * Get the FromId property.
     *
     * @return The FromId
     */
    public String getFromId() {
        return fromId;
    }

    /**
     * Set the ToId property.
     *
     * @param value The new value for ToId
     */
    public void setToId(String value) {
        toId = value;
    }

    /**
     * Get the ToId property.
     *
     * @return The ToId
     */
    public String getToId() {
        return toId;
    }



}

