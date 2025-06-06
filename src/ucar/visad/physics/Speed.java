/*
 * Copyright 1997-2025 Unidata Program Center/University Corporation for
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
 * Provides support for the quantity of speed.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $ $Date: 2006/03/17 17:08:52 $
 */
public class Speed extends ScalarQuantity {

    /*
     * Fields:
     */

    /**
     * The SI unit for this quantity.
     */
    public static final Unit METERS_PER_SECOND;

    /**
     * The default unit for this quantity (same as the SI unit).
     */
    public static final Unit DEFAULT_UNIT;

    /**
     * A common unit for this quantity.
     */
    public static final Unit MILES_PER_HOUR;

    /**
     * A common unit for this quantity.
     */
    public static final Unit FEET_PER_SECOND;

    /**
     * A common unit for this quantity.
     */
    public static final Unit KNOT;

    /**
     * The single instance of this class.
     */
    private static Speed instance;

    static {
        Unit mps;
        Unit mph;
        Unit fps;
        Unit kt;

        try {
            mps = Length.METER.divide(ucar.visad.physics.TimeInterval.SECOND);
            mph = Length.MILE.divide(ucar.visad.physics.TimeInterval.HOUR);
            fps = Length.FOOT.divide(
                ucar.visad.physics.TimeInterval.SECOND).clone("fps");
            kt = Length.NAUTICAL_MILE.divide(
                ucar.visad.physics.TimeInterval.HOUR).clone("kt");
            instance = new Speed(mps);
        } catch (Exception e) {
            mps = null;  // to fool compiler
            mph = null;  // to fool compiler
            fps = null;  // to fool compiler
            kt  = null;  // to fool compiler

            System.err.println(
                "Speed.<clinit>(): Couldn't initialize class: " + e);
            System.exit(1);
        }

        METERS_PER_SECOND = mps;
        MILES_PER_HOUR    = mph;
        DEFAULT_UNIT      = METERS_PER_SECOND;
        FEET_PER_SECOND   = fps;
        KNOT              = kt;
    }

    /*
     * Constructors:
     */

    /**
     * Constructs from a unit.  The name will be "Speed".
     *
     * @param unit              The unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #Speed(String name, Unit unit)
     */
    private Speed(Unit unit) throws VisADException {
        this("Speed", unit);
    }

    /**
     * Constructs from a name.  The default unit will be {@link #DEFAULT_UNIT}.
     *
     * @param name              The name of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #Speed(String name, Unit unit)
     */
    protected Speed(String name) throws VisADException {
        this(name, DEFAULT_UNIT);
    }

    /**
     * Constructs from a name and a unit.  The default representational set will
     * be <code>null</code>.
     *
     * @param name              The name of the quantity.
     * @param unit              The default unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #Speed(String name, Unit unit, Set set)
     */
    protected Speed(String name, Unit unit) throws VisADException {
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
     * @see ucar.visad.ScalarQuantity#ScalarQuantity(String name, Unit unit, Set set)
     */
    protected Speed(String name, Unit unit, Set set) throws VisADException {
        super(name, unit, set);
    }

    /*
     * Class methods:
     */

    /**
     * Returns an instance of this class.
     *
     * @return                  An instance of this class.  The class of the
     *                          object is this class.
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
