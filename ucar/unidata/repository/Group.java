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
import java.util.List;




/**
 * Class RadarInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Group {

    /** _more_ */
    public static final String IDDELIMITER = "/";

    /** _more_ */
    private String id;

    /** _more_ */
    private String name;

    /** _more_ */
    private String description;

    /** _more_ */
    private String parentId;

    /** _more_ */
    private Group parent;

    /** _more_ */
    private List<Group> children = new ArrayList<Group>();


    /**
     *
     * @param parent _more_
     * @param id _more_
     * @param name _more_
     * @param description _more_
     */
    public Group(String id, Group parent, String name, String description) {
        this.id          = id;
        this.parent      = parent;
        this.name        = name;
        this.description = description;
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param parentId _more_
     * @param name _more_
     * @param description _more_
     */
    public Group(String id, String parentId, String name,
                 String description) {
        this.parentId    = parentId;
        this.id          = id;
        this.name        = name;
        this.description = description;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public String getFullName() {
        if (parent != null) {
            return parent.getFullName() + "/" + name;
        }
        return name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + " id:" + id;
    }

    /**
     *  Set the Parent property.
     *
     *  @param value The new value for Parent
     */
    public void setParent(Group value) {
        parent = value;
    }

    /**
     *  Get the Parent property.
     *
     *  @return The Parent
     */
    public Group getParent() {
        return parent;
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
     *  Set the Description property.
     *
     *  @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     *  Get the Description property.
     *
     *  @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the ParentId property.
     *
     * @param value The new value for ParentId
     */
    public void setParentId(String value) {
        parentId = value;
    }

    /**
     * Get the ParentId property.
     *
     * @return The ParentId
     */
    public String getParentId() {
        return parentId;
    }



}

