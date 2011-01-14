/*
 * $Id: IDV-Style.xjs,v 1.1 2006/05/03 21:43:47 dmurray Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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


package ucar.unidata.data.storm;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 9, 2008
 * Time: 4:57:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class Way implements Comparable {

    /** _more_ */
    public static final Way OBSERVATION = new Way("Observation");

    /** _more_ */
    private String id;

    /** _more_ */
    private String name;

    /**
     * _more_
     */
    public Way() {}

    /**
     * _more_
     *
     * @param id _more_
     */
    public Way(String id) {
        this.id = id;
    }

    /**
     * _more_
     *
     * @param id _more_
     * @param name _more_
     */
    public Way(String id, String name) {
        this.id   = id;
        this.name = name;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isObservation() {
        return this.equals(OBSERVATION);
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getId() {
        return id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String toString() {
        if (name != null) {
            return id + ": " + name;
        }
        return id;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if ( !(o instanceof Way)) {
            return false;
        }
        Way other = (Way) o;

        return (this.id.equals(other.id));
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
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public int compareTo(Object o) {
        if (o instanceof Way) {
            Way that = (Way) o;
            return this.id.toLowerCase().compareTo(that.id.toLowerCase());
        }
        return toString().compareTo(o.toString());
    }


}

