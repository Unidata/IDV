/*
 * $Id: LinkSet.java,v 1.9 2006/12/01 20:42:43 jeffmc Exp $
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

package ucar.unidata.data.sounding;



import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * Provides support for a set of Link-s.
 *
 * @author Steven R. Emmerson
 * @version $Id: LinkSet.java,v 1.9 2006/12/01 20:42:43 jeffmc Exp $
 */
public class LinkSet {

    /**
     * The set of sets for the first ends.
     * @serial
     */
    private final Map end1Map = new TreeMap();

    /**
     * The set of sets for the second ends.
     * @serial
     */
    private final Map end2Map = new TreeMap();

    /**
     * Create a LinkSet from a set of {@link Link}s.
     *
     * @param links  array of Links
     */
    public LinkSet(Link[] links) {
        for (int i = 0; i < links.length; ++i) {
            Link     link = links[i];
            Link.End end1 = link.getFirstEnd();
            Link.End end2 = link.getSecondEnd();
            add(end1Map, end1, end2);
            add(end2Map, end2, end1);
        }
    }

    /**
     * Add a link to another in the map
     *
     * @param map    map for links
     * @param key    link key
     * @param other  other link
     */
    protected static void add(Map map, Link.End key, Link.End other) {
        java.util.Set endSet = (java.util.Set) map.get(key);
        if (endSet == null) {
            endSet = new TreeSet();
        }
        endSet.add(other);
        map.put(key, endSet);
    }

    /**
     * Return an iterator for the first end
     *
     * @return  iterator
     */
    public Iterator FirstEndIterator() {
        return new EndIterator(end1Map);
    }

    /**
     * Return an iterator for the first end
     *
     * @return  iterator
     */
    public Iterator SecondEndIterator() {
        return new EndIterator(end2Map);
    }

    /**
     * A wrapper around the iterator of the keyset of a Map
     */
    protected static class EndIterator implements Iterator {

        /** interator for map */
        private final Iterator iter;

        /**
         * Create an EndIterator for the given Map.
         *
         * @param map   map for iterator
         */
        public EndIterator(Map map) {
            iter = map.entrySet().iterator();
        }

        /**
         * Returns true if the iteration has more elements. (In other words,
         * returns true if next would return an element rather than throwing
         * an exception.)
         *
         * @return  true if there are more items
         */
        public boolean hasNext() {
            return iter.hasNext();
        }

        /**
         * Returns the next element in the interation.
         *
         * @return the next element in the interation.
         */
        public Object next() {
            return (Map.Entry) iter.next();
        }

        /**
         * Removes from the underlying collection the last element returned
         * by the iterator (optional operation).   Not supported in this
         * implementation
         *
         * @throws UnsupportedOperationException   always (not supported)
         */
        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Can't remove");
        }
    }
}

