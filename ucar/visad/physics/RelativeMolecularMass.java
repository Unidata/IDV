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
 * Provides support for the quantity of relative molecular mass (ratio of
 * molecular mass to that of Carbon-12).
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2006/03/17 17:08:52 $
 */
public class RelativeMolecularMass extends ScalarQuantity {

    /*
     * Fields:
     */

    /**
     * The SI unit of relative molecular mass.
     */
    public static final Unit DIMENSIONLESS_UNIT;

    /**
     * The default unit of molar mass (same as the SI unit).
     */
    public static final Unit DEFAULT_UNIT;

    /**
     * The relative molecular mass of carbon-12.
     */
    public static final Real RELATIVE_ATOMIC_MASS_OF_CARBON_12;

    /**
     * The single instance of this class.
     */
    private static RelativeMolecularMass instance;

    static {
        Real ramoc12;
        Unit du;

        try {
            du       = CommonUnit.dimensionless;
            instance = new RelativeMolecularMass(du);
            ramoc12  = NewReal(12, du);
        } catch (Exception e) {
            du      = null;  // to fool compiler
            ramoc12 = null;  // to fool compiler

            System.err.println("RelativeMolecularMass.<clinit>: "
                               + "Couldn't initialize class: " + e);
            System.exit(1);
        }

        DIMENSIONLESS_UNIT                = du;
        DEFAULT_UNIT                      = DIMENSIONLESS_UNIT;
        RELATIVE_ATOMIC_MASS_OF_CARBON_12 = ramoc12;
    }

    /*
     * Constructors:
     */

    /**
     * Constructs from a unit.  The name will be "Relative_Molecular_Mass".
     *
     * @param unit              The default unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #RelativeMolecularMass(String, Unit)
     */
    private RelativeMolecularMass(Unit unit) throws VisADException {
        this("Relative_Molecular_Mass", unit);
    }

    /**
     * Constructs from a name.  The unit will be {@link #DEFAULT_UNIT} and the
     * default set will be <code>null</code>.
     *
     * @param name              The name of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #RelativeMolecularMass(String name, Unit unit, Set set)
     */
    protected RelativeMolecularMass(String name) throws VisADException {
        this(name, DEFAULT_UNIT, (Set) null);
    }

    /**
     * Constructs from a name and a unit.  The default representational set will
     * be <code>null</code>.
     *
     * @param name              The name of the quantity.
     * @param unit              The default unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #RelativeMolecularMass(String name, Unit unit, Set set)
     */
    protected RelativeMolecularMass(String name, Unit unit)
            throws VisADException {
        this(name, unit, (Set) null);
    }

    /**
     * Constructs from a name, a unit, and a representational set.  This is the
     * most general constructor.
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
    protected RelativeMolecularMass(String name, Unit unit, Set set)
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
     * amount.  The assumed unit is {@link #DEFAULT_UNIT}.
     *
     * @param amount            The numeric value.
     * @return                  The single value of this quantity corresponding
     *                          to the input.
     * @throws VisADException   VisAD failure.
     * @see #NewReal(double, Unit)
     */
    public static Real NewReal(double amount) throws VisADException {
        return NewReal(amount, DEFAULT_UNIT);
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
