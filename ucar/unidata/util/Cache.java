/*
 * $Id: Misc.java,v 1.271 2007/08/20 20:22:46 dmurray Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;







/**
 * Provides a hashtable cache of key value pairs and keeps the size below a given limit
 *
 * @author IDV development group.
 *
 * @version $Revision: 1.271 $
 */
public class Cache {

    /** _more_          */
    private Hashtable cache = new Hashtable();

    /** _more_          */
    private List keys = new ArrayList();

    /** _more_          */
    private int cacheSize = 100;

    /**
     * _more_
     *
     * @param size _more_
     */
    public Cache(int size) {
        this.cacheSize = size;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public synchronized Object get(Object key) {
        return cache.get(key);
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public synchronized void put(Object key, Object value) {
        //TESTING:
        if(true) return;
        keys.remove(key);
        keys.add(key);
        while (keys.size() > cacheSize) {
            Object keyToRemove   = keys.get(0);
            Object valueToRemove = cache.get(keyToRemove);
            removeValue(keyToRemove, valueToRemove);
            keys.remove(0);
            cache.remove(keyToRemove);
        }
        cache.put(key, value);
    }


    /**
     * _more_
     */
    public synchronized void clear() {
        cache = new Hashtable();
        keys  = new ArrayList();
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param object _more_
     */
    protected void removeValue(Object key, Object object) {}

}

