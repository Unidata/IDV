/*
 * $Id: Ensemble.java,v 1.7 2005/05/13 18:31:28 jeffmc Exp $
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

package ucar.unidata.metdata;



import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.io.Serializable;

import ucar.units.Unit;


/**
 * A class for holding an ensemble of {@link Quantity}s and
 * {@link Value}s.  Each Quantity can have only one associated
 * Value.
 *
 * @author Glenn Davis
 * @version $Revision: 1.7 $
 */
public class Ensemble implements Serializable {

    /** map holding ensemble */
    private final Map map_;

    /**
     * Create a new Ensemble
     */
    public Ensemble() {
        map_ = new TreeMap();
    }

    /**
     * Pust a {@link Quantity} into the Ensemble.
     *
     * @param qstr     quantity name
     * @param vv       quantity value
     * @param uStr     unit name
     * @return  the new Value or null if it is not already there
     */
    public Value put(String qstr, int vv, String uStr) {
        return put(QuantityDB.get(qstr), ValueFactory.newValue(vv, uStr));
    }

    /**
     * Put a {@link Quantity} in the Ensemble
     *
     * @param qq   Quantity
     * @param vv   Value
     * @return  vv or null if it is not already there
     */
    public Value put(Quantity qq, Value vv) {
        // next line prohibits null args as well
        if ( !qq.isAssignable(vv)) {
            throw new IllegalArgumentException("Can't assign");
        }
        return (Value) map_.put(qq, vv);
    }

    /**
     * Get the Value of a {@link Quantity} from the Ensemble
     *
     * @param qq    Quantity to look up
     * @return  associated value
     */
    public Value get(Quantity qq) {
        return (Value) map_.get(qq);
    }

    /**
     * Get the {@link Value} from the Ensemble by {@link Quantity} name
     *
     * @param qstr    quantity name
     * @return  associated value or null if it does not exist
     */
    public Value get(String qstr) {
        return (Value) map_.get(QuantityDB.get(qstr));
    }

    /**
     * See if this ensemble is empty
     *
     * @return   true if it has not entries
     */
    public boolean isEmpty() {
        return map_.isEmpty();
    }

    /**
     * Get the size of the ensemble.
     *
     * @return  number of entries
     */
    public int size() {
        return map_.size();
    }

    /**
     * Get the set of {@link Quantity}s in the Ensemble.
     *
     * @return  set of Quantities
     */
    public Set keySet() {
        return map_.keySet();
    }

    /**
     * Get the set of {@link Value}s in the Ensemble.
     *
     * @return  set of Values
     */
    public Set entrySet() {
        return map_.entrySet();
    }

    /**
     * Return the hash code for this object.
     *
     * @return  the hash code
     */
    public int hashCode() {
        return map_.hashCode();
    }

    /**
     * See if the Object in question is equal to this Ensemble.
     *
     * @param oo  Object in question
     *
     * @return  true if they are equal
     */
    public boolean equals(Object oo) {
        if (oo instanceof Ensemble) {
            return map_.equals(((Ensemble) oo).map_);
        }
        return false;
    }

    /**
     * Format the Entry.
     *
     * @param buf    StringBuffer for formatting
     * @param entry  Entry to format
     *
     * @return  formatted Entry
     */
    protected StringBuffer formatEntry(StringBuffer buf, Map.Entry entry) {
        ((Quantity) (entry.getKey())).format(buf);
        buf.append(": ");
        ((Value) (entry.getValue())).format(buf);
        return buf;
    }

    /**
     * Format the entire Ensemble.  Iterates through all entries
     * and returns the StringBuffer containing them.
     *
     * @param buf   StringBuffer to put formatted entries into
     *
     * @return  buf
     */
    public StringBuffer format(StringBuffer buf) {
        java.util.Iterator iter = entrySet().iterator();
        while (iter.hasNext()) {
            formatEntry(buf, (Map.Entry) iter.next());
            buf.append("\n");
        }
        return buf;
    }

    /**
     * Return a String representation of this Ensemble
     *
     * @return  string representation
     */
    public String toString() {
        return format(new StringBuffer()).toString();
    }
}
