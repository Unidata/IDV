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

package ucar.visad;



import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Provides support for a run-length encoded set of valid data segments.
 *
 * @author Steven R. Emmerson
 */
public class SegmentSet {

    /**
     * The set of run-length encoded segments.
     * @serial
     */
    private final SortedSet set = new TreeSet(new Comparator() {

        public int compare(Object obj1, Object obj2) {
            return ((Segment) obj1).getIndex() - ((Segment) obj2).getIndex();
        }

        public boolean equals(Object obj1, Object obj2) {
            return compare(obj1, obj2) == 0;
        }
    });

    /**
     * The total number of elements in all segments.
     * @serial
     */
    private int totalCount = 0;

    /**
     * Constructs from nothing.
     */
    public SegmentSet() {}

    /**
     * Adds a new segment.
     *
     * @param segment           The segment to be added.
     */
    protected void add(Segment segment) {

        set.add(segment);

        totalCount += segment.getCount();
    }

    /**
     * Gets the total number of elements in all segments.
     *
     * @return                 The total number of elements in all segments.
     */
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Gets the iterator over the segments.
     *
     * @return                  The iterator over the segments.
     */
    public Iterator iterator() {

        return new Iterator() {

            private final Iterator iter    = set.iterator();
            private Object         current = null;

            public boolean hasNext() {
                return iter.hasNext();
            }

            public Object next() {
                return current = iter.next();
            }

            public void remove() {

                if (current != null) {
                    totalCount -= ((Segment) current).getCount();
                }

                iter.remove();
            }
        };
    }

    /**
     * Extracts data based on segment information.
     *
     * @param values            The values to have segments extracted.
     * @return                  The extracted values from the segments.
     */
    public double[][] take(double[][] values) {

        double[][] newValues = new double[values.length][getTotalCount()];
        int        toIndex   = 0;

        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Segment segment   = (Segment) iter.next();
            int     fromIndex = segment.getIndex();
            int     count     = segment.getCount();

            for (int j = values.length; --j >= 0; ) {
                System.arraycopy(values[j], fromIndex, newValues[j], toIndex,
                                 count);
            }

            toIndex += count;
        }

        return newValues;
    }

    /**
     * Extracts data based on segment information.
     *
     * @param values            The values to have segments extracted.
     * @return                  The extracted values from the segments.
     */
    public float[][] take(float[][] values) {

        float[][] newValues = new float[values.length][getTotalCount()];
        int       toIndex   = 0;

        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Segment segment   = (Segment) iter.next();
            int     fromIndex = segment.getIndex();
            int     count     = segment.getCount();

            for (int j = values.length; --j >= 0; ) {
                System.arraycopy(values[j], fromIndex, newValues[j], toIndex,
                                 count);
            }

            toIndex += count;
        }

        return newValues;
    }
}
