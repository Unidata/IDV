/*
 * $Id: Quantity.java,v 1.8 2005/05/13 18:31:30 jeffmc Exp $
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
 * Abstraction for physical quantity.
 *
 * @author Glenn Davis
 * @version $Revision: 1.8 $ $Date: 2005/05/13 18:31:30 $
 */
public interface Quantity extends Comparable {
    /*
    * What are the base quantities and exponents?
    getDimensions()
    */

    /**
     * Get the name of this Quantity.
     *
     * @return  quantity name
     */
    public String getName();

    /**
     * See if the {@link ucar.units.Unit} is assignable to this quantity.
     * i.e, is it a compatible unit
     *
     * @param unit   Unit in question
     *
     * @return  true if compatible
     */
    public boolean isAssignable(Unit unit);

    /**
     * See if a {@link Value} is assignable to this Quantity
     *
     * @param value   Value to check.
     *
     * @return   true if it is assignable.
     */
    public boolean isAssignable(Value value);

    /**
     * Format this Quantity
     *
     * @param buf    input/output buffer
     * @return  <code>buf</code> with formatted info
     */
    public StringBuffer format(StringBuffer buf);
}
