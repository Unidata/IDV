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

package ucar.visad.physics;


import ucar.visad.*;



import visad.*;


/**
 * Provides support for the quantity of a gas constant that is specific to a
 * gas with a specific molar mass.  The specific gas constant is equal
 * to the regular gas constant divided by the gas's molar mass.  Such
 * a gas constant is appropriate to the the formula p = dR'T: where d is the
 * density and R' is the specific gas constant.  The SI unit for this quantity
 * is J.kg-1.K-1.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2006/03/17 17:08:52 $
 */
public abstract class SpecificGasConstant extends ScalarQuantity {

    /*
     * Fields:
     */

    /**
     * The SI unit of a specific gas constant.
     */
    public static final Unit JOULES_PER_KILOGRAM_PER_DEGREE_KELVIN;

    /**
     * The default unit of a specfic gas constant (same as the SI unit).
     */
    public static final Unit DEFAULT_UNIT;

    static {
        Unit unit;

        try {
            unit = SI.meter.divide(SI.second).pow(2).divide(SI.kelvin);
        } catch (Exception e) {
            unit = null;  // to fool compiler

            System.err.println(
                "SpecificGasConstant.<clinit>(): Couldn't initialize class: "
                + e);
            System.exit(1);
        }

        JOULES_PER_KILOGRAM_PER_DEGREE_KELVIN = unit;
        DEFAULT_UNIT = JOULES_PER_KILOGRAM_PER_DEGREE_KELVIN;
    }

    /*
     * Constructors:
     */

    /**
     * Constructs from a name.  The default unit will be {@link #DEFAULT_UNIT}.
     *
     * @param name              The name of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #SpecificGasConstant(String, Unit)
     */
    protected SpecificGasConstant(String name) throws VisADException {
        this(name, DEFAULT_UNIT);
    }

    /**
     * Constructs from a name and a unit.  The default representational set will
     * be <code>null</code>.
     *
     * @param name              The name of the quantity.
     * @param unit              The default unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #SpecificGasConstant(String name, Unit unit, Set set)
     */
    protected SpecificGasConstant(String name, Unit unit)
            throws VisADException {
        this(name, unit, (Set) null);
    }

    /**
     * Constructs from a name, a unit, and a default representational set.  This
     * is the most general constructor.
     *
     * @param name              The name of the quantity.
     * @param unit              The default unit of the quantity.
     * @param set               The default representational set of the
     *                          quantity.  It shall be an instance of
     *                          <code>visad.DoubleSet</code>,
     *                          <code>visad.FloatSet</code>,
     *                          <code>visad.Integer1DSet</code>, or
     *                          <code>null</code>.  If <code>null</code>, then
     *                          the default is <code>visad.FloatSet</code>.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see ScalarQuantity#ScalarQuantity(String name, Unit unit, Set set)
     */
    protected SpecificGasConstant(String name, Unit unit, Set set)
            throws VisADException {
        super(name, unit, set);
    }
}
