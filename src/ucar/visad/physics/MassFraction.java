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
 * Provides support for the quantity of mass fraction (i.e.  kg/kg).
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2006/03/17 17:08:51 $
 */
public class MassFraction extends ScalarQuantity {

    /*
     * Fields:
     */

    /**
     * The SI unit for this quantity.
     */
    public static final Unit KILOGRAMS_PER_KILOGRAM;

    /**
     * The default unit for this quantity (same as the SI unit).
     */
    public static final Unit DEFAULT_UNIT;

    /**
     * The single instance of this class.
     */
    private static MassFraction instance;

    static {
        Unit unit;

        try {
            unit     = SI.kilogram.divide(SI.kilogram);
            instance = new MassFraction(unit);
        } catch (Exception e) {
            unit = null;  // to fool compiler

            System.err.println(
                "MassFraction.<clinit>(): Couldn't initialize class: " + e);
            System.exit(1);
        }

        KILOGRAMS_PER_KILOGRAM = unit;
        DEFAULT_UNIT           = KILOGRAMS_PER_KILOGRAM;
    }

    /*
     * Constructors:
     */

    /**
     * Constructs from a unit.  The name will be "Mass_Fraction".
     *
     * @param unit              The unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #MassFraction(String name, Unit unit)
     */
    private MassFraction(Unit unit) throws VisADException {
        this("Mass_Fraction", unit);
    }

    /**
     * Constructs from a name.  The default unit will be {@link #DEFAULT_UNIT}.
     *
     * @param name              The name of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #MassFraction(String name, Unit unit)
     */
    protected MassFraction(String name) throws VisADException {
        this(name, DEFAULT_UNIT);
    }

    /**
     * Constructs from a name and a unit.  The default representational set will
     * be <code>null</code>.
     *
     * @param name              The name of the quantity.
     * @param unit              The default unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #MassFraction(String name, Unit unit, Set set)
     */
    protected MassFraction(String name, Unit unit) throws VisADException {
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
     * @see #MassFraction(RealType type)
     */
    protected MassFraction(String name, Unit unit, Set set)
            throws VisADException {
        this(RealType.getRealType(name, unit, set, 0));
    }

    /**
     * Constructs from a VisAD RealType.
     *
     * @param type              The VisAD RealType.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see ScalarQuantity#ScalarQuantity(RealType)
     */
    protected MassFraction(RealType type) throws VisADException {
        super(type);
    }

    /*
     * Class Methods:
     */

    /**
     * Returns an instance of this quantity.
     *
     * @return                  An instance of this quantity.
     */
    public static ScalarQuantity instance() {
        return instance;
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
     */
    public static Real NewReal(double amount, Unit unit, ErrorEstimate error)
            throws VisADException {
        return instance.newReal(amount, unit, error);
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
