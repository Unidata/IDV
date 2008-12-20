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

import java.util.Date;
/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */

public  class WikiPageHistory {
    int version;
    User user;
    Date date;
    String description;
    String text;
    
    public WikiPageHistory(int version,User user, Date date, String description) {
        this(version, user, date, description,null);
    }

    public WikiPageHistory(int version,User user, Date date, String description,String text) {
        this.version = version;
        this.user = user;
        this.date = date;
        this.description = description;
        this.text = text;
    }

    /**
       Set the Version property.

       @param value The new value for Version
    **/
    public void setVersion (int value) {
	version = value;
    }

    /**
       Get the Version property.

       @return The Version
    **/
    public int getVersion () {
	return version;
    }

    /**
       Set the User property.

       @param value The new value for User
    **/
    public void setUser (User value) {
	user = value;
    }

    /**
       Get the User property.

       @return The User
    **/
    public User getUser () {
	return user;
    }

    /**
       Set the Date property.

       @param value The new value for Date
    **/
    public void setDate (Date value) {
	date = value;
    }

    /**
       Get the Date property.

       @return The Date
    **/
    public Date getDate () {
	return date;
    }

    /**
       Set the Description property.

       @param value The new value for Description
    **/
    public void setDescription (String value) {
	description = value;
    }

    /**
       Get the Description property.

       @return The Description
    **/
    public String getDescription () {
	return description;
    }


    /**
       Set the Text property.

       @param value The new value for Text
    **/
    public void setText (String value) {
	text = value;
    }

    /**
       Get the Text property.

       @return The Text
    **/
    public String getText () {
	return text;
    }



}

