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

package ucar.visad.quantities;



import visad.*;


/**
 * Provides support for the dry air gas constant.
 *
 * @author Steven R. Emmerson
 * @version $Id: SpecificHeatCapacityOfDryAir.java,v 1.9 2005/05/13 18:35:44 jeffmc Exp $
 */
public abstract class SpecificHeatCapacityOfDryAir extends ScalarQuantity {

    /** the unit of this quantity */
    private static Unit unit;

    /**
     * Constructs from a RealType.
     *
     * @param realType          The RealType for the instance.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected SpecificHeatCapacityOfDryAir(RealType realType)
            throws VisADException {
        super(realType);
    }

    /**
     * Returns the default, SI unit.
     *
     * @return                  The default, SI unit.
     */
    protected static Unit getDefaultSIUnit() {

        if (unit == null) {
            synchronized (SpecificHeatCapacityOfDryAir.class) {
                if (unit == null) {
                    try {
                        unit = SI.meter.divide(SI.second).pow(2).divide(
                            SI.kelvin);
                    } catch (UnitException e) {
                        unit = null;
                    }  // can't happen because above expression is valid
                }
            }
        }

        return unit;
    }

    /**
     * Returns the default sampling set.
     *
     * @return               The default sampling set.
     */
    protected static Set getDefaultSet() {
        return null;
    }
}
