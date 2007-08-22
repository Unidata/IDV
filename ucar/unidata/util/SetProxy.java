/*
 * $Id: SetProxy.java,v 1.10 2006/05/05 19:19:37 jeffmc Exp $
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



import java.util.Set;


/**
 * @author $Author: jeffmc $
 * @version $Revision: 1.10 $ $Date: 2006/05/05 19:19:37 $
 */
public class SetProxy extends CollectionProxy implements Set {

    /**
     * _more_
     *
     * @param adaptee
     *
     */
    public SetProxy(Set adaptee) {
        super(adaptee);
    }

    /**
     * _more_
     *
     * @param arg
     * @return _more_
     */
    public boolean equals(java.lang.Object arg) {
        return adaptee_.equals(arg);
    }

    /**
     * _more_
     * @return _more_
     */
    public int hashCode() {
        return adaptee_.hashCode();
    }

    /*
     * Returns a new proxy backed by the clone of the adaptee.
    public Object
    clone()
    {
            return new SetProxy((Set)adaptee_.clone());
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
            SetProxy px = new SetProxy(new java.util.TreeSet());
        } catch (Exception ee) {
            ee.printStackTrace(System.err);
            System.exit(1);
        }
        System.exit(0);
    }
    /* End Test */
}

