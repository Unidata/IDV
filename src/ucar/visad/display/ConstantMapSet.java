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



import java.util.*;


/**
 * Provides support for a set of ConstantMap-s.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.15 $
 */
public class ConstantMapSet {

    /**
     * The DisplayRealType -> ConstantMap mapping.
     */
    private TreeMap maps;

    /**
     * Constructs from nothing.
     */
    public ConstantMapSet() {}

    /**
     * Adds a ConstantMap to this set.
     * @param map               The ConstantMap to be added to this set.
     * @return                  The previous ConstantMap if it exists;
     *                          otherwise, NULL.
     */
    public synchronized ConstantMap put(ConstantMap map) {

        if (maps == null) {
            maps = new TreeMap(new Comparator() {

                public int compare(Object o1, Object o2) {
                    return ((DisplayRealType) o1).getName().compareTo(
                        ((DisplayRealType) o2).getName());
                }

                public boolean equals(Object o1, Object o2) {
                    return compare(o1, o2) == 0;
                }
            });
        }

        return (ConstantMap) maps.put(map.getDisplayScalar(), map);
    }

    /**
     * Removes a {@link visad.ConstantMap} from this set.
     * @param type              The DisplayRealType associated with the
     *                          ConstantMap to be removed.
     * @return                  The ConstantMap associated with
     *                          <code>type</code> that was removed from this set
     *                          if it exists; otherwise, NULL.
     */
    public synchronized ConstantMap remove(DisplayRealType type) {

        return (maps == null)
               ? (ConstantMap) null
               : (ConstantMap) maps.remove(type);
    }

    /**
     * Removes all {@link visad.ConstantMap}s from this set.
     */
    public synchronized void removeAll() {

        maps.clear();
    }

    /**
     * Combines the ConstantMap-s of two sets.
     *
     * @param set1              The first set.
     * @param set2              The second set.
     *
     * @return                  The union of the two sets.  ConstantMap-s in
     *                          <code>set2</code> override ConstantMap-s for the
     *                          same DisplayRealType in <code>set1</code>.
     */
    public static ConstantMapSet combine(ConstantMapSet set1,
                                         ConstantMapSet set2) {

        ConstantMapSet set = new ConstantMapSet();

        synchronized (set1) {
            synchronized (set2) {
                if ((set1.size() > 0) || (set2.size() > 0)) {
                    set.maps = new TreeMap();
                }

                if (set1.size() > 0) {
                    set.maps.putAll(set1.maps);
                }

                if (set2.size() > 0) {
                    set.maps.putAll(set2.maps);
                }
            }
        }

        return set;
    }

    /**
     * Gets the number of elements in this set of ConstantMap-s.
     *
     * @return                  The number of ConstantMap-s in this set.
     */
    public int size() {

        return (maps == null)
               ? 0
               : maps.size();
    }

    /**
     * Gets clones of the ConstantMap-s of this set as an array.
     *
     * @return                  An array of clones of the ConstantMap-s of this
     *                          set.
     */
    public synchronized ConstantMap[] getConstantMaps() {

        ConstantMap[] mapArray = new ConstantMap[size()];

        if (mapArray.length > 0) {
            maps.values().toArray(mapArray);
        }

        for (int i = 0; i < mapArray.length; ++i) {
            mapArray[i] = (ConstantMap) mapArray[i].clone();
        }

        return mapArray;
    }

    /**
     * Returns a clone of this instance.
     *
     * @return                  A clone of this instance.
     *
     */
    public Object clone() {

        ConstantMapSet clone = new ConstantMapSet();

        return combine(clone, this);
    }

    /**
     * Returns a String representation of this ConstantMapSet.
     *
     * @return string representation of the set
     */
    public String toString() {
        return (maps == null)
               ? "No constant maps"
               : maps.toString();
    }
}
