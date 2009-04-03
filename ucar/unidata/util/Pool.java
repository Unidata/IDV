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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;



/**
 * Provides a pool of keyed objects and keeps the total size below a given limit.
 * This maps a key to a list of values. The get method is consumptive, it removes
 * the returned value from the list. If the total number of list elements exceed the
 * given cache size this will remove the elements from the list on a key based last used
 *  basis
 *
 * @author IDV development group.
 *
 * @version $Revision: 1.271 $
 */
public class Pool<KeyType, ValueType> {

    /** The cache */
    private Hashtable<KeyType, List<ValueType>> cache =
        new Hashtable<KeyType, List<ValueType>>();

    /** Keep track of the keys */
    private List<KeyType> keys = new ArrayList<KeyType>();

    /** max cache size */
    private int maxSize = 100;

    /** current cache size */
    private int size = 0;

    /**
     * ctor
     *
     * @param size max size
     */
    public Pool(int size) {
        this.maxSize = size;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public synchronized boolean contains(KeyType key) {
        if (key == null) {
            return false;
        }
        List<ValueType> list = cache.get(key);
        if ((list != null) && (list.size() > 0)) {
            return true;
        }
        return false;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public  ValueType get(KeyType key) {
        synchronized(this) {
            if(key == null) return null;
            List<ValueType> list = cache.get(key);
            if ((list != null) && (list.size() > 0)) {
                size--;
                return getFromPool(list);
            }
        }
        return createValue(key);
    }


    protected  ValueType getFromPool(List<ValueType> list) {
        return list.remove(0);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public  boolean containsOrCreate(KeyType key) {
        synchronized(this) { 
            if(key == null) return false;
            if (contains(key)) {
                return true;
            }
        }
        ValueType value = createValue(key);
        if (value == null) {
            return false;
        }
        put(key, value);
        return true;
    }



    /**
     * _more_
     *
     * @param key _more_
     * @param value _more_
     */
    public synchronized void put(KeyType key, ValueType value) {
        if(key == null) return;
        keys.remove(key);
        keys.add(key);
        while (size >= maxSize - 1) {
            for (KeyType keyToCheck : keys) {
                List<ValueType> listToCheck = cache.get(keyToCheck);
                while (listToCheck.size() > 0) {
                    ValueType valueToRemove = listToCheck.remove(0);
                    removeValue(key, valueToRemove);
                    size--;
                    if (size < maxSize) {
                        break;
                    }
                }
            }
        }
        List<ValueType> list = cache.get(key);
        if ((list == null) || (list.size() == 0)) {
            list = new ArrayList<ValueType>();
            cache.put(key, list);
        }
        list.add(value);
        size++;
    }


    /**
     * Clear the cache
     */
    public synchronized void clear() {
        for(Enumeration<KeyType> keys= cache.keys();keys.hasMoreElements(); ) {
            KeyType key = keys.nextElement();
            for (ValueType value : cache.get(key)) {
                removeValue(key, value);
            }
        }
        size  = 0;
        cache = new Hashtable();
        keys  = new ArrayList();
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    protected ValueType createValue(KeyType key) {
        return null;
    }


    /**
     * _more_
     *
     * @param key _more_
     * @param object _more_
     */
    protected void removeValue(KeyType key, ValueType object) {}

}

