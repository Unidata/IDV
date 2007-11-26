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

import java.util.Date;
import java.util.List;




/**
 * Class RadarInfo _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class Collection {

    private String id;
    private String name;
    private String description;
    private Collection parent;

    /**
     */
    public Collection(Collection parent, String id, String name, String description) {
        this.parent = parent;
        this.id = id;
        this.name = name;
        this.description = description;
    }


    public String getFullName() {
        if(parent!=null) return parent.getFullName()+"/"+ name;
        return name;
    }

    public String toString() {
        return name + " id:" + id;
    }

    /**
       Set the Parent property.

       @param value The new value for Parent
    **/
    public void setParent (Collection value) {
	parent = value;
    }

    /**
       Get the Parent property.

       @return The Parent
    **/
    public Collection getParent () {
	return parent;
    }

    /**
       Set the Id property.

       @param value The new value for Id
    **/
    public void setId (String value) {
	id = value;
    }

    /**
       Get the Id property.

       @return The Id
    **/
    public String getId () {
	return id;
    }

    /**
       Set the Name property.

       @param value The new value for Name
    **/
    public void setName (String value) {
	name = value;
    }

    /**
       Get the Name property.

       @return The Name
    **/
    public String getName () {
	return name;
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

}
