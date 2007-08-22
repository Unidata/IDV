/*
 * $Id: Link.java,v 1.9 2006/12/01 20:42:43 jeffmc Exp $
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


/**
 * Provides support for conceptually linking two Link.End-s together.
 *
 * @author Steven R. Emmerson
 * @version $Id: Link.java,v 1.9 2006/12/01 20:42:43 jeffmc Exp $
 */
public class Link implements Comparable {

    /**
     * The first Link.End.
     * @serial
     */
    private final Link.End end1;

    /**
     * The second Link.End.
     * @serial
     */
    private final Link.End end2;

    /**
     * Constructs.
     *
     * @param end1              The first Link.End.
     * @param end2              The second Link.End.
     */
    public Link(Link.End end1, Link.End end2) {
        this.end1 = end1;
        this.end2 = end2;
    }

    /**
     * Gets the first Link.End.
     *
     * @return                  The first Link.End.
     */
    public Link.End getFirstEnd() {
        return end1;
    }

    /**
     * Gets the second Link.End.
     *
     * @return                  The second Link.End.
     */
    public Link.End getSecondEnd() {
        return end2;
    }

    /**
     * Compares one Link to another.  In comparing two Link-s, a major
     * comparison is made between the first ends and then, if necessary, a minor
     * comparison is made between the second ends.
     *
     * @param link              The other Link.
     * @return                  A number that is less that, equal to, or greater
     *                          than zero depending on whether this Link is
     *                          less than, equal to, or greater than the other
     *                          Link, respectively.
     */
    public int compareTo(Object link) {
        Link that = (Link) link;
        int  comp = getFirstEnd().compareTo(that.getFirstEnd());
        return (comp == 0)
               ? getSecondEnd().compareTo(that.getSecondEnd())
               : comp;
    }

    /**
     * Provides support for the end of a Link.
     */
    public static class End implements Comparable {

        /**
         * The name of the end of the link.
         * @serial
         */
        private final String name;

        /**
         * Constructs.
         *
         * @param name          The name of the end of the link.
         */
        public End(String name) {
            this.name = name;
        }

        /**
         * Gets the name of this end of the link.
         *
         * @return              The name of this end of the link.
         */
        public String getName() {
            return name;
        }

        /**
         * Compares this end to another end.
         *
         * @param that          The other end.
         * @return              A number that is less that, equal to, or greater
         *                      than zero depending on whether this end is
         *                      less than, equal to, or greater than the other
         *                      end, respectively.
         */
        public int compareTo(Object that) {
            return name.compareTo(((End) that).name);
        }
    }
}

