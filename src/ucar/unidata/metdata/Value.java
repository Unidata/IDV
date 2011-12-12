/*
 * $Id: Value.java,v 1.7 2005/05/13 18:31:31 jeffmc Exp $
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
 * Abstraction for the value a measured or calculated physical
 * quantity.
 *
 * @author $Author: jeffmc $
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:31:31 $
 */
public interface Value {

    /**
     * Get the {@link ucar.units.Unit} for this Value
     * @return  the unit
     */
    public Unit getUnit();

    /**
     * Get the value (as an integer) for this Value
     * @return  integer value
     */
    public int getInt();

    /**
     * Get the value in a different Unit
     *
     * @param outputUnit   new unit
     * @return  value in that unit
     *
     * @throws ConversionException   incompatible units
     */
    public int getInt(Unit outputUnit) throws ConversionException;

    /**
     * Get the float value
     *
     * @return  value as a float
     */
    public float getFloat();

    /**
     * Get the float value in a different Unit
     *
     * @param outputUnit   new Unit
     * @return  value in the new unit
     *
     * @throws ConversionException  problem with conversion
     */
    public float getFloat(Unit outputUnit) throws ConversionException;

    /**
     * Get the value as a double
     *
     * @return  double value
     */
    public double getDouble();

    /**
     * Get the double value in a different Unit
     *
     * @param outputUnit   new Unit
     * @return  value in the new unit
     *
     * @throws ConversionException  problem with conversion
     */
    public double getDouble(Unit outputUnit) throws ConversionException;

    /**
     * Format this Value
     *
     * @param buf  input/output buffer
     * @return <code>buf</code> with formatted data
     */
    public StringBuffer format(StringBuffer buf);
}
