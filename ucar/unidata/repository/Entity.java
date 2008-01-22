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
public class Entity {

    /** _more_          */
    static Hashtable ids = new Hashtable();

    /** _more_ */
    List<String> tags;

    List<Comment> comments;

    /** _more_ */
    List<Association> associations = new ArrayList<Association>();

    /** _more_ */
    List<Metadata> metadata = new ArrayList<Metadata>();

    /** _more_ */
    private String id;

    /** _more_ */
    private String name = "";

    /** _more_ */
    private String description = "";

    /** _more_ */
    private Group parentGroup;

    /** _more_ */
    private String parentGroupId;


    /** _more_ */
    private User user;

    /** _more_ */
    private long createDate;



    /**
     * _more_
     */
    public Entity() {}

    /**
     * _more_
     *
     *
     *
     * @param id _more_
     * @param typeHandler _more_
     * @param name _more_
     * @param description _more_
     * @param group _more_
     * @param parentGroup _more_
     * @param user _more_
     * @param resource _more_
     * @param date _more_
     * @param createDate _more_
     */
    public Entity(String id, String name, String description,
                  Group parentGroup, User user, long createDate) {
        //        if(ids.get(id)!=null) throw new IllegalArgumentException("");
        this.id          = id;
        this.name        = name;
        this.description = description;
        this.parentGroup = parentGroup;
        this.user        = user;
        this.createDate  = createDate;
    }


    /**
     * _more_
     *
     * @param name _more_
     * @param description _more_
     * @param parentGroup _more_
     * @param user _more_
     * @param createDate _more_
     */
    public void init(String name, String description, Group parentGroup,
                     User user, long createDate) {
        this.name        = name;
        this.description = description;
        this.parentGroup = parentGroup;
        this.user        = user;
        this.createDate  = createDate;
    }


    public boolean equals(Object o) {
        if ( !o.getClass().equals(getClass())) {
            return false;
        }
        Entity that = (Entity) o;
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
     * Set the Group property.
     *
     * @param value The new value for Group
     */
    public void setParentGroup(Group value) {
        parentGroup = value;
    }

    /**
     * Get the Group property.
     *
     * @return The Group
     */
    public Group getParentGroup() {
        return parentGroup;
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
        return ((parentGroup != null)
                ? parentGroup.getId()
                : parentGroupId);
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
     * Set the Metadata property.
     *
     * @param value The new value for Metadata
     */
    public void setMetadata(List<Metadata> value) {
        metadata = value;
    }

    /**
     * Get the Metadata property.
     *
     * @return The Metadata
     */
    public List<Metadata> getMetadata() {
        return metadata;
    }

    /**
     * _more_
     *
     * @param value _more_
     */
    public void addMetadata(Metadata value) {
        metadata.add(value);
    }


    /**
     * Set the Associations property.
     *
     * @param value The new value for Associations
     */
    public void setAssociations(List<Association> value) {
        associations = value;
    }

    /**
     * Get the Associations property.
     *
     * @return The Associations
     */
    public List<Association> getAssociations() {
        return associations;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void addAssociation(Association value) {
        if (associations == null) {
            associations = new ArrayList<Association>();
        }
        associations.add(value);

    }


    /**
     * Set the Comments property.
     *
     * @param value The new value for Comments
     */
    public void setComments(List<Comment> value) {
        comments = value;
    }

    /**
     * Get the Comments property.
     *
     * @return The Comments
     */
    public List<Comment> getComments() {
        return comments;
    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void addComment(Comment value) {
        if (comments == null) {
            comments = new ArrayList<Comment>();
        }
        comments.add(value);

    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return name + " id:" + id;
    }



}

