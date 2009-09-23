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

import java.io.File;

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
public class ClientEntry {

    /** _more_ */
    public static final double NONGEO = -999999;


    /** _more_ */
    private String id;

    /** _more_ */
    private String name = "";

    /** _more_ */
    private String description = "";

    /** _more_ */
    private String parentGroupId;

    /** _more_ */
    private String user;

    /** _more_ */
    private long createDate;

    /** _more_ */
    private long startDate;

    /** _more_ */
    private long endDate;


    /** _more_ */
    private Resource resource = new Resource();

    /** _more_ */
    private String dataType;


    /** _more_ */
    private double south = NONGEO;

    /** _more_ */
    private double north = NONGEO;

    /** _more_ */
    private double east = NONGEO;

    /** _more_ */
    private double west = NONGEO;



    /**
     * _more_
     */
    public ClientEntry(String id) {
        this.id = id;
    }



    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        ClientEntry that = (ClientEntry) o;
        return Misc.equals(this.id, that.id);
    }



    /**
     * Set the CreateDate property.
     *
     * @param value The new value for CreateDate
     */
    public void setCreateDate(long value) {
        createDate = value;
    }

    /**
     * Get the CreateDate property.
     *
     * @return The CreateDate
     */
    public long getCreateDate() {
        return createDate;
    }


    /**
     * Set the ParentId property.
     *
     * @param value The new value for ParentId
     */
    public void setParentGroupId(String value) {
        parentGroupId = value;
    }

    /**
     * Get the ParentId property.
     *
     * @return The ParentId
     */
    public String getParentGroupId() {
        return  parentGroupId;
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
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the Id property.
     *
     * @param value The new value for Id
     */
    public void setId(String value) {
        id = value;
    }

    /**
     * Get the Id property.
     *
     * @return The Id
     */
    public String getId() {
        return id;
    }

    /**
     *  Set the User property.
     *
     *  @param value The new value for User
     */
    public void setUser(String value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public String getUser() {
        return user;
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
     * Set the StartDate property.
     *
     * @param value The new value for StartDate
     */
    public void setStartDate(long value) {
        startDate = value;
    }

    /**
     * Get the StartDate property.
     *
     * @return The StartDate
     */
    public long getStartDate() {
        return startDate;
    }

    /**
     * Set the EndDate property.
     *
     * @param value The new value for EndDate
     */
    public void setEndDate(long value) {
        endDate = value;
    }

    /**
     * Get the EndDate property.
     *
     * @return The EndDate
     */
    public long getEndDate() {
        return endDate;
    }




    /**
     * Set the South property.
     *
     * @param value The new value for South
     */
    public void setSouth(double value) {
        south = value;
    }

    /**
     * Get the South property.
     *
     * @return The South
     */
    public double getSouth() {
        return ((south == south)
                ? south
                : NONGEO);
    }

    /**
     * Set the North property.
     *
     * @param value The new value for North
     */
    public void setNorth(double value) {
        north = value;
    }

    /**
     * Get the North property.
     *
     * @return The North
     */
    public double getNorth() {
        return ((north == north)
                ? north
                : NONGEO);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasNorth() {
        return (north == north) && (north != NONGEO);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasSouth() {
        return (south == south) && (south != NONGEO);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasEast() {
        return (east == east) && (east != NONGEO);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasWest() {
        return (west == west) && (west != NONGEO);
    }


    /**
     * Set the East property.
     *
     * @param value The new value for East
     */
    public void setEast(double value) {
        east = value;
    }

    /**
     * Get the East property.
     *
     * @return The East
     */
    public double getEast() {
        return ((east == east)
                ? east
                : NONGEO);
    }

    /**
     * Set the West property.
     *
     * @param value The new value for West
     */
    public void setWest(double value) {
        west = value;
    }

    /**
     * Get the West property.
     *
     * @return The West
     */
    public double getWest() {
        return ((west == west)
                ? west
                : NONGEO);
    }




    /**
     * Set the resource property.
     *
     * @param value The new value for resource
     */
    public void setResource(Resource value) {
        resource = value;
    }

    /**
     * Get the resource property.
     *
     * @return The resource
     */
    public Resource getResource() {
        return resource;
    }

}

