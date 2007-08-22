/*
 * $Id: ValueFactory.java,v 1.7 2005/05/13 18:31:31 jeffmc Exp $
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
import ucar.units.ConversionException;


/**
 * A factory for creating {@link Value}s.
 *
 * @author Glenn Davis
 * @version $Revision: 1.7 $
 */
public class ValueFactory {

    /** Default constructor */
    public ValueFactory() {}

    /**
     * Create a new Value
     *
     * @param vv         data value
     * @param unitName   Unit name
     * @return  Value with unit and value
     */
    static public Value newValue(int vv, String unitName) {
        return new iValue(vv, unitName);
    }

    /**
     * Implementation of an integer value
     */
    static /* package */ class iValue implements Value, java.io.Serializable {

        /** the value */
        private final int vv_;
        /* private final Unit unit_; */

        /** the unit */
        private final String unitName_;

        /* package */

        /**
         * Create a new Value
         *
         * @param vv          data value
         * @param unitName    Unit name
         *
         */
        iValue(int vv, String unitName) {
            vv_       = vv;
            unitName_ = unitName;
        }

        /**
         * Get the Unit.  Not supported in this implementation
         * @return UnsupportedOperationException
         */
        public Unit getUnit() {
            throw new UnsupportedOperationException("Not yet implemented");
        }

        /**
         * Get the integer value
         * @return  the integer value
         */
        public int getInt() {
            return vv_;
        }

        /**
         * Get the integer value in a new Unit
         *
         * @param outputUnit   new Unit
         * @return  value in new Unit
         *
         * @throws ConversionException    problem convertin
         */
        public int getInt(Unit outputUnit) throws ConversionException {
            return (int) getUnit().convertTo((float) vv_, outputUnit);
        }

        /**
         * Get the float value.
         *
         * @return  value as a float
         */
        public float getFloat() {
            return (float) vv_;
        }

        /**
         * Get the float value in a new Unit
         *
         * @param outputUnit   new Unit
         * @return  value in new Unit
         *
         * @throws ConversionException    problem convertin
         */
        public float getFloat(Unit outputUnit) throws ConversionException {
            return getUnit().convertTo((float) vv_, outputUnit);
        }

        /**
         * Get the double value.
         *
         * @return  value as a double
         */
        public double getDouble() {
            return (double) vv_;
        }

        /**
         * Get the double value in a new Unit
         *
         * @param outputUnit   new Unit
         * @return  value in new Unit
         *
         * @throws ConversionException    problem convertin
         */
        public double getDouble(Unit outputUnit) throws ConversionException {
            return getUnit().convertTo((double) vv_, outputUnit);
        }

        /**
         * Format this.
         *
         * @param buf   input/output buffer
         * @return  <code>buf</code> with formatted info
         */
        public StringBuffer format(StringBuffer buf) {
            buf.append(vv_);
            buf.append(" ");
            buf.append(unitName_);
            return buf;
        }

        /**
         * Get a String representation of this Value
         * @return a String representation of this Value
         */
        public String toString() {
            return format(new StringBuffer()).toString();
        }
    }
}
