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
public class Entry {


    Object []values;

    /** _more_          */
    List<String> tags;

    /** _more_ */
    private String id;

    /** _more_ */
    private String name;

    /** _more_ */
    private String description;


    /** _more_ */
    private Group group;

    /** _more_          */
    private User user;

    /** _more_ */
    private String file;

    /** _more_ */
    private long createDate;

    /** _more_ */
    private long startDate;

    /** _more_ */
    private long endDate;

    /** _more_ */
    private TypeHandler typeHandler;


    /**
     * _more_
     *
     *
     *
     * @param id _more_
     * @param typeHandler _more_
     * @param name _more_
     * @param description _more_
     * @param type _more_
     * @param group _more_
     * @param user _more_
     * @param file _more_
     * @param date _more_
     */
    public Entry(String id, TypeHandler typeHandler, String name,
                 String description, Group group, User user, String file,
                 long date) {
        this(id, typeHandler, name, description, group, user, file, date,
             date, date);
    }


    public Entry(String id, TypeHandler typeHandler, String name,
                 String description, Group group, User user, String file,
                 long date, Object[]values) {
        this(id, typeHandler, name, description, group, user, file, date,
             date, date,values);
    }


    /**
     * _more_
     *
     * @param id _more_
     * @param typeHandler _more_
     * @param name _more_
     * @param description _more_
     * @param type _more_
     * @param group _more_
     * @param user _more_
     * @param file _more_
     * @param station _more_
     * @param product _more_
     * @param date _more_
     * @param createDate _more_
     * @param startDate _more_
     * @param endDate _more_
     */
    public Entry(String id, TypeHandler typeHandler, String name,
                 String description, Group group, User user, String file,
                 long createDate, long startDate, long endDate) {
        this(id,typeHandler, name, description, group, user, file, createDate,
              startDate, endDate, null);
    }

    public Entry(String id, TypeHandler typeHandler, String name,
                 String description, Group group, User user, String file,
                 long createDate, long startDate, long endDate, Object[]values) {
        this.id          = id;
        this.typeHandler = typeHandler;
        this.name        = name;
        this.description = description;
        this.group       = group;
        this.user        = user;
        this.file        = file;
        this.createDate  = createDate;
        this.startDate   = startDate;
        this.endDate     = endDate;
        this.values = values;
    }


    public String getInsertSql() {
        return null;
    }

    /**
     * Set the File property.
     *
     * @param value The new value for File
     */
    public void setFile(String value) {
        file = value;
    }

    /**
     * Get the File property.
     *
     * @return The File
     */
    public String getFile() {
        return file;
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
     * Set the Group property.
     *
     * @param value The new value for Group
     */
    public void setGroup(Group value) {
        group = value;
    }

    /**
     * Get the Group property.
     *
     * @return The Group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getGroupId() {
        if (group != null) {
            return group.getId();
        }
        return "";
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setTypeHandler(TypeHandler value) {
        typeHandler = value;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getType() {
        return typeHandler.getType();
    }


    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public TypeHandler getTypeHandler() {
        return typeHandler;
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
    public void setUser(User value) {
        user = value;
    }

    /**
     *  Get the User property.
     *
     *  @return The User
     */
    public User getUser() {
        return user;
    }

    /**
     *  Set the Tags property.
     *
     *  @param value The new value for Tags
     */
    public void setTags(List<String> value) {
        tags = value;
    }

    /**
     *  Get the Tags property.
     *
     *  @return The Tags
     */
    public List<String> getTags() {
        return tags;
    }



    /**
     * _more_
     *
     * @param tag _more_
     */
    public void addTag(String tag) {
        if (tags == null) {
            tags = new ArrayList<String>();
        }
        tags.add(tag);

    }


/**
Set the Values property.

@param value The new value for Values
**/
public void setValues (Object[] value) {
	values = value;
}

/**
Get the Values property.

@return The Values
**/
public Object[] getValues () {
	return values;
}





}

