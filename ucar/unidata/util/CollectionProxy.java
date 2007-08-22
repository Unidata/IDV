/*
 * $Id: CollectionProxy.java,v 1.11 2006/05/05 19:19:33 jeffmc Exp $
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



import java.util.Collection;


/**
 * @author $Author: jeffmc $
 * @version $Revision: 1.11 $ $Date: 2006/05/05 19:19:33 $
 */
public class CollectionProxy implements Collection {

    /** _more_ */
    protected final Collection adaptee_;

    /**
     * _more_
     *
     * @param adaptee
     *
     */
    public CollectionProxy(Collection adaptee) {
        adaptee_ = adaptee;
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public boolean add(java.lang.Object arg) {
        return adaptee_.add(arg);
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public boolean addAll(java.util.Collection arg) {
        return adaptee_.addAll(arg);
    }

    /**
     * _more_
     */
    public void clear() {
        adaptee_.clear();
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public boolean contains(java.lang.Object arg) {
        return adaptee_.contains(arg);
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public boolean containsAll(java.util.Collection arg) {
        return adaptee_.containsAll(arg);
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean isEmpty() {
        return adaptee_.isEmpty();
    }

    /**
     * _more_
     * @return _more_
     */
    public java.util.Iterator iterator() {
        return adaptee_.iterator();
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public boolean remove(java.lang.Object arg) {
        return adaptee_.remove(arg);
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public boolean removeAll(java.util.Collection arg) {
        return adaptee_.removeAll(arg);
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public boolean retainAll(java.util.Collection arg) {
        return adaptee_.retainAll(arg);
    }

    /**
     * _more_
     * @return _more_
     */
    public int size() {
        return adaptee_.size();
    }

    /**
     * _more_
     * @return _more_
     */
    public java.lang.Object[] toArray() {
        return adaptee_.toArray();
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public java.lang.Object[] toArray(java.lang.Object[] arg) {
        return adaptee_.toArray(arg);
    }

    /*
     * Returns a new proxy backed by the clone of the adaptee.
    public Object
    clone()
    {
            return new CollectionProxy((Collection)adaptee_.clone());
    }
     */

    /* Begin Test */

    /**
     * _more_
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            CollectionProxy px = new CollectionProxy(new java.util.TreeSet());
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
    /* End Test */
}

