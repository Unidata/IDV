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
 */

package ucar.visad.display;


import visad.*;


import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;


/**
 * Provides support for a set of ScalarMap-s.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.17 $
 */
public class ScalarMapSet {

    /** internal map */
    private final TreeMap maps = new TreeMap();

    /**
     * Constructs from nothing.
     */
    public ScalarMapSet() {}

    /**
     * Constructs from another instance.  The {@link visad.ScalarMap}s of the
     * other instance are <em>not</em> cloned.
     *
     * @param that              The other instance.
     */
    public ScalarMapSet(ScalarMapSet that) {

        for (Iterator iter =
                that.maps.values().iterator(); iter.hasNext(); ) {
            myAdd((ScalarMap) iter.next());
        }
    }

    /**
     * Constructs from a {@link java.util.Collection} of
     * {@link visad.ScalarMap}s.  The {@link visad.ScalarMap}s of the
     * {@link java.util.Collection} are <em>not</em> cloned.</p>
     *
     * @param col                 The collection of ScalarMap-s.
     * @throws ClassCastException if an element in the collection is not a
     *                            ScalarMap.
     */
    public ScalarMapSet(Collection col) {
        myAdd(col);
    }

    /**
     * Adds a {@link visad.ScalarMap} to this set.  The previous
     * {@link visad.ScalarMap} is returned if it exists; otherwise
     * <code>null</code> is returned.
     *
     * @param map               The {@link visad.ScalarMap} to be added to
     *                          this set.
     * @return                  The previous {@link visad.ScalarMap} or
     *                          <code>null</code>.
     */
    public ScalarMap add(ScalarMap map) {
        return myAdd(map);
    }

    /**
     * Adds a {@link visad.ScalarMap} to this set.  The previous
     * {@link visad.ScalarMap} is returned if it exists; otherwise
     * <code>null</code> is returned.
     *
     * @param map               The {@link visad.ScalarMap} to be added to
     *                          this set.
     * @return                  The previous {@link visad.ScalarMap} or
     *                          <code>null</code>.
     */
    private synchronized ScalarMap myAdd(ScalarMap map) {
        return (ScalarMap) maps.put(new Key(map), map);
    }

    /**
     * Adds the contents of another {@link ScalarMapSet} to this instance's
     * set of {@link visad.ScalarMap}s.
     *
     * @param that                  The other set of {@link visad.ScalarMap}s.
     * @throws NullPointerException if the other set is <code>null</code>.
     */
    public void add(ScalarMapSet that) {
        myAdd(that);
    }

    /**
     * Adds the contents of another {@link ScalarMapSet} to this instance's
     * set of {@link visad.ScalarMap}s.
     *
     * @param that                  The other set of {@link visad.ScalarMap}s.
     * @throws NullPointerException if the other set is <code>null</code>.
     */
    private synchronized void myAdd(ScalarMapSet that) {

        synchronized (that) {
            maps.putAll(that.maps);
        }
    }

    /**
     * Adds the contents of a {@link java.util.Collection} to this instance's
     * set of {@link visad.ScalarMap}s.
     *
     * @param col                   The Collection of ScalarMap-s to be added.
     * @throws NullPointerException if the Collection is <code>null</code>.
     * @throws ClassCastException   if an element of the Collection isn't a
     *                              ScalarMap.
     */
    public void add(Collection col) {
        myAdd(col);
    }

    /**
     * Adds the contents of a {@link java.util.Collection} to this instance's
     * set of {@link visad.ScalarMap}s.
     *
     * @param col                   The Collection of ScalarMap-s to be added.
     * @throws NullPointerException if the Collection is <code>null</code>.
     * @throws ClassCastException   if an element of the Collection isn't a
     *                              ScalarMap.
     */
    private synchronized void myAdd(Collection col) {

        for (Iterator iter = col.iterator(); iter.hasNext(); ) {
            myAdd((ScalarMap) iter.next());
        }
    }

    /**
     * Returns the {@link visad.ScalarMap} in this instance that matches a
     * template.  Returns <code>null</code> is no such {@link visad.ScalarMap}
     * exists.
     *
     * @param template          The template.
     * @return                  The ScalarMap in this instance that matches the
     *                          template or </code>null</null>.
     */
    public ScalarMap get(ScalarMap template) {
        return myGet(template);
    }

    /**
     * Returns the {@link visad.ScalarMap} in this instance that matches a
     * template. * Returns <code>null</code> is no such {@link visad.ScalarMap}
     * exists.
     *
     * @param template          The template.
     * @return                  The ScalarMap in this instance that matches the
     *                          template or </code>null</null>.
     */
    private synchronized ScalarMap myGet(ScalarMap template) {
        return (ScalarMap) maps.get(new Key(template));
    }

    /**
     * Removes a ScalarMap from this set.
     *
     * @param map               The ScalarMap to be removed.
     * @return                  <code>true</code> if and only if the ScalarMap
     *                          existed.
     */
    public synchronized boolean remove(ScalarMap map) {
        return maps.remove(new Key(map)) != null;
    }

    /**
     * <p>Removes the contents of another {@link ScalarMapSet} from this
     * instance's set of {@link visad.ScalarMap}s.</p>
     *
     * @param that                  The other set of {@link visad.ScalarMap}s.
     * @throws NullPointerException if the other set is <code>null</code>.
     */
    public synchronized void remove(ScalarMapSet that) {

        synchronized (that) {
            for (Iterator iter = that.iterator(); iter.hasNext(); ) {
                remove((ScalarMap) iter.next());
            }
        }
    }

    /**
     * <p>Removes all {@link visad.ScalarMap}s from this instance.</p>
     */
    public synchronized void removeAll() {

        maps.clear();
    }

    /**
     * Returns the number of {@link visad.ScalarMap}s in this set.
     *
     * @return                  The number of {@link visad.ScalarMap}s in this
     *                          set.
     */
    public int size() {
        return mySize();
    }

    /**
     * Returns the number of {@link visad.ScalarMap}s in this set.
     *
     * @return                  The number of {@link visad.ScalarMap}s in this
     *                          set.
     */
    private synchronized int mySize() {
        return maps.size();
    }

    /**
     * Clears the set of {@link visad.ScalarMap}s.
     */
    public synchronized void clear() {
        maps.clear();
    }

    /**
     * Returns an iterator for this instance.  Objects returned by the {@link
     * java.util.Iterator#next()} method have type {@link visad.ScalarMap}.
     *
     * @return                  An iterator for this instance.
     */
    public Iterator iterator() {
        return myIterator();
    }

    /**
     * Returns an iterator for this instance.  Objects returned by the {@link
     * java.util.Iterator#next()} method have type {@link visad.ScalarMap}.
     *
     * @return                  An iterator for this instance.
     */
    private Iterator myIterator() {
        return maps.values().iterator();
    }

    /**
     * Indicates if this instance equals an object.
     *
     * @param obj               The object.
     * @return                  True if and only if this instance equals the
     *                          object.
     */
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if ( !(obj instanceof ScalarMapSet)) {
            return false;
        }

        return maps.equals(((ScalarMapSet) obj).maps);
    }

    /**
     * Returns the hash code of this instance.
     *
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return maps.hashCode();
    }

    /**
     * <p>Returns a string representation of this instance.</p>
     *
     * @return                  A string representation of this instance.
     */
    public synchronized String toString() {

        StringBuffer buf   = new StringBuffer(getClass().getName() + "{");
        boolean      first = true;

        for (Iterator iter = myIterator(); iter.hasNext(); ) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }

            buf.append(((ScalarMap) iter.next()).toString());
        }

        buf.append("}");

        return buf.toString();
    }

    /**
     * <p>Combines the {@link visad.ScalarMap}s of two sets.
     * {@link visad.ScalarMap}s in the second set override
     * {@link visad.ScalarMap}s for the same {@link visad.DisplayRealType}
     * in the first set.  The input sets are not modified.</p>
     *
     * @param set1              The first set.
     * @param set2              The second set.
     * @return                  The union of the two sets.
     */
    public static ScalarMapSet combine(ScalarMapSet set1, ScalarMapSet set2) {

        ScalarMapSet set = new ScalarMapSet();

        set.myAdd(set1);
        set.myAdd(set2);

        return set;
    }

    /**
     * <p>Returns the difference between one set and another.  The result is
     * equal to the first set with no elements from the second set.  The input
     * sets are not modified.</p>
     *
     * @param set1                  The first set of maps.
     * @param set2                  The second set of maps.
     * @return                      Maps of the first set that are not in the
     *                              second set.
     */
    static ScalarMapSet subtract(ScalarMapSet set1, ScalarMapSet set2) {

        ScalarMapSet result = new ScalarMapSet();

        synchronized (set1) {
            synchronized (set2) {
                for (Iterator iter = set1.myIterator(); iter.hasNext(); ) {
                    ScalarMap map = (ScalarMap) iter.next();

                    if (set2.myGet(map) == null) {
                        result.myAdd(map);
                    }
                }
            }
        }

        return result;
    }

    /**
     * <p>Returns the intersection of one set and another.  The input sets are
     * not modified.</p>
     *
     * @param set1                  The first set of maps.
     * @param set2                  The second set of maps.
     * @return                      Maps of the first set that are also in the
     *                              second set.
     */
    static ScalarMapSet intersect(ScalarMapSet set1, ScalarMapSet set2) {

        ScalarMapSet result = new ScalarMapSet();

        synchronized (set1) {
            synchronized (set2) {
                if (set1.mySize() > set2.mySize()) {
                    ScalarMapSet tmp = set1;

                    set1 = set2;
                    set2 = tmp;
                }

                for (Iterator iter = set1.myIterator(); iter.hasNext(); ) {
                    ScalarMap map = (ScalarMap) iter.next();

                    if (set2.myGet(map) != null) {
                        result.myAdd(map);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Because the Scalar.alias(String) method of the Scalar of a ScalarMap
     * renders the Scalar mutable, neither the ScalarMap nor its Scalar can be
     * used as the key to a ScalarMap in a java.util.Map.  Therefore, the
     * following class is used.  Note that it uses only the original names
     * of the ScalarMap's Scalar and DisplayScalar.
     */
    private static class Key implements Comparable {

        /** scalar name */
        private final String scalarName;

        /** display name */
        private final String displayName;

        /**
         * Construct a key from the ScalarMap
         *
         * @param map  map to use
         */
        Key(ScalarMap map) {

            displayName = map.getDisplayScalar().getOriginalName();
            ScalarType scalar = map.getScalar();
            scalarName = (scalar == null)
                         ? displayName
                         : scalar.getOriginalName();
        }

        /**
         * Compare to another object
         *
         * @param obj  object for comparison
         * @return  comparison value
         */
        public int compareTo(Object obj) {

            Key that = (Key) obj;
            int cmp  = scalarName.compareTo(that.scalarName);

            if (cmp == 0) {
                cmp = displayName.compareTo(that.displayName);
            }

            return cmp;
        }
    }
}
