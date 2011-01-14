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



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of a molar gas constant.  A molar gas
 * constant is determined by the idea gas law -- variations of which include the
 * following:<pre>
 *
 *     pV = nRT
 *
 *     pv = RT
 *
 *     p = dRT/m
 *
 * where:
 *     p is pressure
 *     V is volume
 *     n is amount-of-substance
 *     R is molar gas constant
 *     T is temperature
 *     v is molar volume (volume per amount-of-substance)
 *     d is mass density
 *     m is molar mass (mass per amount-of-substance)</pre>
 *
 * The SI unit of a molar gas constant is J.mol-1.K-1.
 *
 * Note that some texts (including meteorolgical ones) use a "gas constant" that
 * is the true gas constant divided by the mean molar mass of the gas.  Such a
 * "gas constant" -- which can replace R/m in the third equation above -- has SI
 * units of J.kg-1.K-1.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2006/03/17 17:08:51 $
 */
public class GasConstant extends ScalarQuantity {

    /*
     * Fields:
     */

    /**
     * The SI unit of a gas constant.
     */
    public static final Unit JOULES_PER_MOLE_PER_DEGREE_KELVIN;

    /**
     * The default unit of a gas constant (same as the SI unit).
     */
    public static final Unit DEFAULT_UNIT;

    /**
     * The universal (or ideal) molar gas constant value.  See
     * <http://physics.nist.gov/cgi-bin/cuu/Constants/>.
     */
    public static final Real UNIVERSAL_GAS_CONSTANT;

    /**
     * The single instance of this class.
     */
    private static GasConstant instance;

    static {
        Unit unit;
        Real ugc;

        try {
            unit = SI.kilogram.multiply(
                SI.meter.divide(SI.second).pow(2)).divide(SI.mole).divide(
                SI.kelvin);
            instance = new GasConstant(unit);

            /*
             * See <http://physics.nist.gov/cgi-bin/cuu/Constants/>
             * for the numeric value of the following.
             */
            ugc = NewReal(8.314472, unit);
        } catch (Exception e) {
            unit = null;  // to fool compiler
            ugc  = null;  // to fool compiler

            System.err.println(
                "GasConstant.<clinit>(): Couldn't initialize class: " + e);
            System.exit(1);
        }

        JOULES_PER_MOLE_PER_DEGREE_KELVIN = unit;
        DEFAULT_UNIT                      = JOULES_PER_MOLE_PER_DEGREE_KELVIN;
        UNIVERSAL_GAS_CONSTANT            = ugc;
    }

    /*
     * Constructors:
     */

    /**
     * Constructs from a unit.  The name will be "Gas_Constant".
     *
     * @param unit              The unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #GasConstant(String name, Unit unit)
     */
    private GasConstant(Unit unit) throws VisADException {
        this("Gas_Constant", unit);
    }

    /**
     * Constructs from a name.  The default unit will be {@link #DEFAULT_UNIT}.
     *
     * @param name              The name of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #GasConstant(String name, Unit unit)
     */
    protected GasConstant(String name) throws VisADException {
        this(name, DEFAULT_UNIT);
    }

    /**
     * Constructs from a name and a unit.  The default representational set will
     * be <code>null</code>.
     *
     * @param name              The name of the quantity.
     * @param unit              The default unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #GasConstant(String name, Unit unit, Set set)
     */
    protected GasConstant(String name, Unit unit) throws VisADException {
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
    protected GasConstant(String name, Unit unit, Set set)
            throws VisADException {
        super(name, unit, set);
    }

    /*
     * Class methods:
     */

    /**
     * Returns an instance of this quantity.
     *
     * @return                  An instance of this quantity.  The class of the
     *                          object is this class.
     */
    public static ScalarQuantity instance() {
        return instance;
    }

    /**
     * Returns the single value of this quantity corresponding to a numeric
     * amount in the default unit.
     *
     * @param amount            The numeric value in the default unit.
     * @return                  The single value of this quantity corresponding
     *                          to the input.
     * @throws VisADException   VisAD failure.
     * @see #NewReal(double, Unit)
     */
    public static Real NewReal(double amount) throws VisADException {
        return NewReal(amount, (Unit) null);
    }

    /**
     * Returns the single value of this quantity corresponding to a numeric
     * amount and a unit.  The error estimate will be <code>null</code>.
     *
     * @param amount            The numeric value.
     * @param unit              The unit of the numeric value.  May be
     *                          <code>null</code>.
     * @return                  The single value of this quantity corresponding
     *                          to the input.
     * @throws VisADException   VisAD failure.
     * @see #NewReal(double, Unit, ErrorEstimate)
     */
    public static Real NewReal(double amount, Unit unit)
            throws VisADException {
        return NewReal(amount, unit, (ErrorEstimate) null);
    }

    /**
     * Returns the single value of this quantity corresponding to a numeric
     * amount, a unit, and an error estimate.
     *
     * @param amount            The numeric value.
     * @param unit              The unit of the numeric value.  May be
     *                          <code>null</code>.
     * @param error             The error estimate.  May be <code>null</code>.
     * @return                  The single value of this quantity corresponding
     *                          to the input.
     * @throws VisADException   VisAD failure.
     * @see Real#Real(RealType, double, Unit, ErrorEstimate)
     */
    public static Real NewReal(double amount, Unit unit, ErrorEstimate error)
            throws VisADException {
        return instance.newReal(amount, unit, error);
    }

    /**
     * Tests this class.
     *
     * @param args              Invocation arguments.  Ignored.
     */
    public static void main(String[] args) {

        try {
            System.out.println(instance.toString());
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /**
     * Vets a VisAD data object for compatibility.
     *
     * @param data              The VisAD data object to examine for
     *                          compatibility.
     * @throws TypeException    The VisAD data object is incompatible with this
     *                          quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static void Vet(visad.Data data)
            throws TypeException, VisADException, RemoteException {
        instance.vet(data);
    }

    /**
     * Vets a VisAD MathType for compatibility.
     *
     * @param type              The VisAD MathType to examine for compatibility.
     * @throws TypeException    The VisAD MathType is incompatible with this
     *                          quantity.
     * @throws VisADException   VisAD failure.
     */
    public static void Vet(MathType type)
            throws TypeException, VisADException {
        instance.vet(type);
    }
}
