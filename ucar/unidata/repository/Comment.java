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


import java.util.Date;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.30 $
 */
public class Comment {

    /** _more_ */
    private String id;

    /** _more_ */
    private String subject;

    /** _more_ */
    private String comment;

    /** _more_ */
    private Date date;

    /** _more_ */
    private User user;

    /** _more_ */
    private Entry entry;


    /**
     * _more_
     *
     *
     * @param id _more_
     * @param entry _more_
     * @param user _more_
     * @param date _more_
     * @param subject _more_
     * @param comment _more_
     */
    public Comment(String id, Entry entry, User user, Date date,
                   String subject, String comment) {
        this.id      = id;
        this.entry   = entry;
        this.user    = user;
        this.subject = subject;
        this.comment = comment;
        this.date    = date;
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
     * Set the Subject property.
     *
     * @param value The new value for Subject
     */
    public void setSubject(String value) {
        subject = value;
    }

    /**
     * Get the Subject property.
     *
     * @return The Subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Set the Comment property.
     *
     * @param value The new value for Comment
     */
    public void setComment(String value) {
        comment = value;
    }

    /**
     * Get the Comment property.
     *
     * @return The Comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * Set the User property.
     *
     * @param value The new value for User
     */
    public void setUser(User value) {
        user = value;
    }

    /**
     * Get the User property.
     *
     * @return The User
     */
    public User getUser() {
        return user;
    }

    /**
     * Set the Date property.
     *
     * @param value The new value for Date
     */
    public void setDate(Date value) {
        date = value;
    }

    /**
     * Get the Date property.
     *
     * @return The Date
     */
    public Date getDate() {
        return date;
    }






    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        return subject;
    }


}

