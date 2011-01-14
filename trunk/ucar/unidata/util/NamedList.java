/*
 * $Id: NamedList.java,v 1.9 2006/05/05 19:19:36 jeffmc Exp $
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



import java.util.Iterator;
import java.util.List;



/**
 */

public class NamedList implements NamedObject {

    /** _more_ */
    private String name;

    /** _more_ */
    private List list;

    /**
     * _more_
     *
     */
    public NamedList() {}


    /**
     *
     * @param name
     * @param list
     */
    public NamedList(String name, List list) {
        this.name = name;
        this.list = list;
    }


    /**
     * _more_
     *
     * @param value
     */
    public void setList(List value) {
        list = value;
    }

    /**
     * _more_
     * @return _more_
     */
    public List getList() {
        return list;
    }

    /**
     * _more_
     *
     * @param value
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * _more_
     * @return _more_
     */
    public String getName() {
        return name;
    }

    /**
     * _more_
     * @return _more_
     */
    public String getDescription() {
        return name;
    }


    /**
     * _more_
     * @return _more_
     */
    public Iterator iterator() {
        return list.iterator();
    }

    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return name;
    }

    /**
     * _more_
     *
     * @param o
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof NamedList)) {
            return false;
        }
        NamedList that = (NamedList) o;
        return (name.equals(that.name) && list.equals(that.list));
    }


}

