/*
 * $Id: ObjectPair.java,v 1.7 2006/05/05 19:19:36 jeffmc Exp $
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



import java.*;



/**
 * A class to hold and transfer contour level settings, as to and from the
 * dialog box ContLevelDialog. All members public for ease of access.
 *
 */

public class ObjectPair {

    /** _more_ */
    private Object o1;

    /** _more_ */
    private Object o2;

    /**
     * _more_
     *
     */
    public ObjectPair() {}

    /**
     * _more_
     *
     * @param o1
     * @param o2
     *
     */
    public ObjectPair(Object o1, Object o2) {
        this.o1 = o1;
        this.o2 = o2;
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getObject1() {
        return o1;
    }

    /**
     * _more_
     *
     * @param o1
     */
    public void setObject1(Object o1) {
        this.o1 = o1;
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getObject2() {
        return o2;
    }

    /**
     * _more_
     *
     * @param o2
     */
    public void setObject2(Object o2) {
        this.o2 = o2;
    }

    /**
     * _more_
     * @return _more_
     */
    public int hashCode() {
        if ((o1 != null) && (o2 != null)) {
            return o1.hashCode() ^ o2.hashCode();
        }
        if (o1 != null) {
            return o1.hashCode();
        }
        if (o2 != null) {
            return o2.hashCode();
        }
        return super.hashCode();
    }

    /**
     * _more_
     *
     * @param o
     * @return _more_
     */
    public boolean equals(Object o) {
        if ( !(o instanceof ObjectPair)) {
            return false;
        }
        ObjectPair other = (ObjectPair) o;
        return (Misc.equals(o1, other.o1) && Misc.equals(o2, other.o2));
    }

    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return o1.toString() + " " + o2.toString();
    }


}

