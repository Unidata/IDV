/*
 * $Id: SortedSetProxy.java,v 1.10 2006/05/05 19:19:37 jeffmc Exp $
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



import java.util.SortedSet;


/**
 * @author $Author: jeffmc $
 * @version $Revision: 1.10 $ $Date: 2006/05/05 19:19:37 $
 */
public class SortedSetProxy extends SetProxy implements SortedSet {

    /**
     * _more_
     *
     * @param adaptee
     *
     */
    public SortedSetProxy(SortedSet adaptee) {
        super(adaptee);
    }


    /**
     * _more_
     * @return _more_
     */
    public java.util.Comparator comparator() {
        return ((SortedSet) adaptee_).comparator();
    }

    /**
     * _more_
     * @return _more_
     */
    public java.lang.Object first() {
        return ((SortedSet) adaptee_).first();
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public java.util.SortedSet headSet(java.lang.Object arg) {
        return ((SortedSet) adaptee_).headSet(arg);
    }

    /**
     * _more_
     * @return _more_
     */
    public java.lang.Object last() {
        return ((SortedSet) adaptee_).last();
    }

    /**
     * _more_
     *
     * @param arg1
     * @param arg2
     * @return _more_
     */
    public java.util.SortedSet subSet(java.lang.Object arg1,
                                      java.lang.Object arg2) {
        return ((SortedSet) adaptee_).subSet(arg1, arg2);
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public java.util.SortedSet tailSet(java.lang.Object arg) {
        return ((SortedSet) adaptee_).tailSet(arg);
    }

    /*
     * Returns a new proxy backed by the clone of the adaptee.
    public Object
    clone()
    {
            return new SortedSetProxy((SortedSet)adaptee_.clone());
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
            SortedSetProxy px = new SortedSetProxy(new java.util.TreeSet());
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
    /* End Test */
}

