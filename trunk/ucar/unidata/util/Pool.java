/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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
 * 
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
 *
 * @param <KeyType>
 * @param <ValueType>
 */
public class Pool<KeyType, ValueType> {

    /** _more_          */
    private Object MUTEX = new Object();

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
     * @return _more_
     */
    public Object getMutex() {
        return MUTEX;
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean contains(KeyType key) {
        if (key == null) {
            return false;
        }
        synchronized (getMutex()) {
            List<ValueType> list = cache.get(key);
            if ((list != null) && (list.size() > 0)) {
                return true;
            }
            return false;
        }
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public ValueType get(KeyType key) {
        if (key == null) {
            return null;
        }
        synchronized (getMutex()) {
            List<ValueType> list = cache.get(key);
            if ((list != null) && (list.size() > 0)) {
                size--;
                ValueType value = getFromPool(list);
                if (list.size() == 0) {
                    cache.remove(key);
                    keys.remove(key);
                }
                return value;
            }
        }
        return createValue(key);
    }


    /**
     * _more_
     *
     * @param list _more_
     *
     * @return _more_
     */
    protected ValueType getFromPool(List<ValueType> list) {
        return list.remove(0);
    }


    /**
     * _more_
     *
     * @param key _more_
     *
     * @return _more_
     */
    public boolean containsOrCreate(KeyType key) {
        if (key == null) {
            return false;
        }
        synchronized (getMutex()) {
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
    public void put(KeyType key, ValueType value) {
        if (key == null) {
            return;
        }
        List<KeyType> keysToRemove = null;
        synchronized (getMutex()) {
            while (size >= maxSize - 1) {
                for (KeyType keyToCheck : keys) {
                    List<ValueType> listToCheck = cache.get(keyToCheck);
                    while ((listToCheck != null)
                            && (listToCheck.size() > 0)) {
                        ValueType valueToRemove = listToCheck.remove(0);
                        removeValue(key, valueToRemove);
                        size--;
                        if (size < maxSize) {
                            break;
                        }
                    }
                    if (listToCheck.size() == 0) {
                        if (keysToRemove == null) {
                            keysToRemove = new ArrayList<KeyType>();
                        }
                        keysToRemove.add(keyToCheck);
                    }
                }
            }

            if (keysToRemove != null) {
                for (KeyType keyToRemove : keysToRemove) {
                    cache.remove(keyToRemove);
                    keys.remove(keyToRemove);
                }
            }


            keys.remove(key);
            keys.add(key);
            List<ValueType> list = cache.get(key);
            if ((list == null) || (list.size() == 0)) {
                list = new ArrayList<ValueType>();
                cache.put(key, list);
            }
            list.add(value);
            size++;
        }
    }


    /**
     * _more_
     *
     * @param sb _more_
     */
    public void getStats(StringBuffer sb) {
        synchronized (getMutex()) {
            sb.append("Cache size:" + cache.size());
            sb.append("\n");
            for (Enumeration<KeyType> keys = cache.keys();
                    keys.hasMoreElements(); ) {
                KeyType key = keys.nextElement();
                sb.append(key + " #:" + cache.get(key).size());
                sb.append("\n");
            }
        }
    }


    /**
     * Clear the cache
     */
    public void clear() {
        synchronized (getMutex()) {
            for (Enumeration<KeyType> keys = cache.keys();
                    keys.hasMoreElements(); ) {
                KeyType key = keys.nextElement();
                for (ValueType value : cache.get(key)) {
                    removeValue(key, value);
                }
            }
            size  = 0;
            cache = new Hashtable();
            keys  = new ArrayList();
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public int getSize() {
        return size;
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
