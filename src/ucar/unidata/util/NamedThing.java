/*
 * $Id: NamedThing.java,v 1.2 2007/06/28 14:42:03 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


package ucar.unidata.util;


/**
 * Holds a name and a description
 *
 *
 *
 * @author IDV development team
 * @version $Revision: 1.2 $Date: 2007/06/28 14:42:03 $
 */


public  class NamedThing implements NamedObject {

    /** _more_          */
    private String name;

    /** _more_          */
    private String description;

    /**
     * _more_
     */
    public NamedThing() {}

    /**
     * _more_
     *
     * @param name _more_
     */
    public NamedThing(String name) {
        this(name, name);
    }

    /**
     * _more_
     *
     * @param name _more_
     * @param description _more_
     */
    public NamedThing(String name, String description) {
        this.name        = name;
        this.description = description;
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
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if(description != null) 
            return description;
        return name;            
    }



    public int hashCode() {
        return Misc.hashcode(name) ^ Misc.hashcode(description);
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof NamedThing)) {
            return false;
        }
        NamedThing that = (NamedThing) o;
        return Misc.equals(this.name,that.name) && Misc.equals(this.description, that.description);
    }



}

