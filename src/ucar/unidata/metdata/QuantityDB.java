/*
 * $Id: QuantityDB.java,v 1.7 2005/05/13 18:31:30 jeffmc Exp $
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



import ucar.units.Unit;


/**
 * Class for supporting a data base of {@link Quantity}s.
 *
 * @author Glenn Davis
 * @version $Revision: 1.7 $
 */
public class QuantityDB {

    /** map of quantities */
    private static java.util.Map map = new java.util.HashMap();

    /** default constructor */
    public QuantityDB() {}

    /**
     * Get a Quantity from the database based on the name
     *
     * @param name   quantity name
     *
     * @return  the Quantity in the table or a new Quantity with the name
     */
    static public Quantity get(String name) {
        Object oo = map.get(name);
        if (oo != null) {
            return (Quantity) oo;
        }
        // else
        oo = new QuantityImpl(name);
        map.put(name, oo);
        return (Quantity) oo;
    }

    /**
     * Quantity implementation
     */
    static /* package */ class QuantityImpl
            implements Quantity, java.io.Serializable {

        /** quantity name */
        String name_;

        /* package */

        /**
         * Create a new Quantity with the specified name
         *
         * @param name   name of quantity
         */
        QuantityImpl(String name) {
            name_ = name;
        }

        /**
         * Get the name of the Quantity
         *
         * @return  quantity name
         */
        public String getName() {
            return name_;
        }

        /**
         * See if a Unit is assignable.
         *
         * @param unit  Unit in question
         *
         * @return  true
         */
        public boolean isAssignable(Unit unit) {
            return true;
        }

        /**
         * See if a value is assignable to this Quantity
         *
         * @param value  Value to assign
         *
         * @return  true
         */
        public boolean isAssignable(Value value) {
            return true;
        }

        /**
         * Compare another Quantity to this.
         *
         * @param qq  Quantity in question
         *
         * @return  comparative value
         */
        public int compareTo(Quantity qq) {
            return getName().compareTo(qq.getName());
        }

        /**
         * Compare another Object to this.
         *
         * @param oo  Object in question
         *
         * @return  comparative value
         */
        public int compareTo(Object oo) {
            return compareTo((Quantity) oo);
        }

        /**
         * Get the hashcode for this Quantity
         *
         * @return  hash code
         */
        public int hashCode() {
            return name_.hashCode();
        }

        /**
         * Check for equality.
         *
         * @param oo  Object to check
         *
         * @return  true if <code>oo</code> is a Quantity and
         *          they have the same name
         */
        public boolean equals(Object oo) {
            if (oo instanceof Quantity) {
                return getName().equals(((Quantity) oo).getName());
            }
            return false;
        }

        /**
         * Format this Quantity.
         *
         * @param buf  input/output buffer
         * @return  <code>buf</code> with formatted info
         */
        public StringBuffer format(StringBuffer buf) {
            buf.append(getName());
            return buf;
        }

        /**
         * Get a String representation of this Quantity
         *
         * @return  string representation
         */
        public String toString() {
            return format(new StringBuffer()).toString();
        }
    }
}
