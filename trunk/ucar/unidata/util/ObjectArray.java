/*
 * $Id: ObjectArray.java,v 1.8 2006/05/05 19:19:36 jeffmc Exp $
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
 * A generic Object wrapper that holds a set of objects
 * @author Metapps development team
 * @version $Revision: 1.8 $ $Date: 2006/05/05 19:19:36 $
 */



public class ObjectArray {

    /** _more_ */
    Object[] objects;

    /**
     * _more_
     *
     */
    public ObjectArray() {}

    /**
     * _more_
     *
     * @param objects
     *
     */
    public ObjectArray(Object[] objects) {
        this.objects = objects;
    }

    /**
     * _more_
     *
     * @param o1
     *
     */
    public ObjectArray(Object o1) {
        objects = new Object[] { o1 };
    }

    /**
     * _more_
     *
     * @param o1
     * @param o2
     *
     */
    public ObjectArray(Object o1, Object o2) {
        objects = new Object[] { o1, o2 };
    }

    /**
     * _more_
     *
     * @param o1
     * @param o2
     * @param o3
     *
     */
    public ObjectArray(Object o1, Object o2, Object o3) {
        objects = new Object[] { o1, o2, o3 };
    }

    /**
     * _more_
     *
     * @param o1
     * @param o2
     * @param o3
     * @param o4
     *
     */
    public ObjectArray(Object o1, Object o2, Object o3, Object o4) {
        objects = new Object[] { o1, o2, o3, o4 };
    }

    /**
     * _more_
     * @return _more_
     */
    public Object[] getObjects() {
        return objects;
    }

    /**
     * _more_
     *
     * @param o
     */
    public void setObjects(Object[] o) {
        objects = o;
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getObject1() {
        return objects[0];
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getObject2() {
        return objects[1];
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getObject3() {
        return objects[2];
    }

    /**
     * _more_
     * @return _more_
     */
    public Object getObject4() {
        return objects[3];
    }

}

