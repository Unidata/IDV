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

package ucar.visad;


import visad.*;



import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for single-component quantities.
 *
 * This class supports both scalar quantities with and without an associated
 * coordinate system transformation.  This is done so that the knowledge
 * and semantics of superclass scalar quantities without associated coordinate
 * system transformations can be inherited by subclass scalar quantities
 * that have associated coordinate system transformations
 * (e.g.  ucar.visad.physics.PlaneAngle and  ucar.visad.geoscience.Azimuth or
 * ucar.visad.physics.Length and  ucar.visad.geoscience.Altitude)
 * -- thus, supporting a hierarchy of scientific scalar quantities without
 * regard to coordinate system relationships.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.18 $ $Date: 2005/05/13 18:34:03 $
 */
public abstract class ScalarQuantity extends Quantity {

    /*
     * Fields:
     */

    /** RealType for this scalar quantity */
    private final RealType realType;

    /*
     * Constructors:
     */

    /**
     * Constructs from a name and a default unit.  The default representational
     * set will be {@link FloatSet}.
     *
     * @param name              The name of the scalar quantity.
     * @param unit              The default unit of the scalar quantity.
     * @throws TypeException    The VisAD RealType already exists but has an
     *                          incompatible unit or representational set.
     * @throws VisADException   VisAD failure.
     * @see #ScalarQuantity(String name, Unit unit, Set set)
     */
    protected ScalarQuantity(String name, Unit unit)
            throws TypeException, VisADException {
        this(name, unit, (visad.Set) null);
    }

    /**
     * Constructs from a name, default unit, and default representational set.
     * The attribute mask will be zero.
     *
     * @param name              The name of the scalar quantity.
     * @param unit              The default unit of the scalar quantity.
     * @param set               The default representational set of the
     *                          quantity.  It shall be an instance of
     *                          <code>visad.DoubleSet</code>,
     *                          <code>visad.FloatSet</code>,
     *                          <code>visad.Integer1DSet</code>, or
     *                          <code>null</code>.  If <code>null</code>, then
     *                          the default is <code>visad.FloatSet</code>.
     * @throws VisADException   VisAD failure.
     * @see #ScalarQuantity(String name, Unit unit, Set set, int attrMask)
     */
    protected ScalarQuantity(String name, Unit unit, visad.Set set)
            throws VisADException {
        this(name, unit, set, 0);
    }

    /**
     * Constructs from a name, default unit, default representational set, and
     * an attribute mask.
     *
     * @param name              The name of the scalar quantity.
     * @param unit              The default unit of the scalar quantity.
     * @param set               The default representational set of the
     *                          quantity.  It shall be an instance of
     *                          <code>visad.DoubleSet</code>,
     *                          <code>visad.FloatSet</code>,
     *                          <code>visad.Integer1DSet</code>, or
     *                          <code>null</code>.  If <code>null</code>, then
     *                          the default is <code>visad.FloatSet</code>.
     * @param attrMask          The attribute mask: <code>0</code> or
     *                          <code>INTERVAL</code>.
     * @throws VisADException   VisAD failure.
     * @see #ScalarQuantity(RealType realType)
     */
    protected ScalarQuantity(String name, Unit unit, visad.Set set,
                             int attrMask)
            throws VisADException {
        this(RealType.getRealType(name, unit, set, attrMask));
    }

    /**
     * Constructs from an existing RealType.  The coordinate system
     * transformation will be <code>null</code>.
     *
     * @param realType          The existing RealType.
     * @throws TypeException    if an instance cannot be created.
     * @throws VisADException   if a core VisAD failure occurs.
     * @see #ScalarQuantity(RealType, CoordinateSystem)
     */
    protected ScalarQuantity(RealType realType)
            throws TypeException, VisADException {
        this(realType, (CoordinateSystem) null);
    }

    /**
     * Constructs from a name, an existing RealType, and a coordinate system
     * transformation.  The default domain set will be <code>null</code>.
     *
     * @param realType          The existing RealType.
     * @param coordSys          The coordinate system transformation.
     * @throws TypeException    if an instance cannot be created.
     * @throws VisADException   if a core VisAD failure occurs.
     * @see #ScalarQuantity(RealType, CoordinateSystem, visad.Set)
     */
    protected ScalarQuantity(RealType realType, CoordinateSystem coordSys)
            throws TypeException, VisADException {
        this(realType, coordSys, (visad.Set) null);
    }

    /**
     * Constructs from a name, an existing RealType, a coordinate system
     * transformation, and a default domain set.  The name of the quantity will
     * be that of the RealType.
     *
     * @param realType          The existing RealType.
     * @param coordSys          The coordinate system transformation.
     * @param domain            The default domain set.
     * @throws TypeException    if an instance cannot be created.
     * @throws VisADException   if a core VisAD failure occurs.
     */
    protected ScalarQuantity(RealType realType, CoordinateSystem coordSys,
                             visad.Set domain)
            throws TypeException, VisADException {

        super(realType.getName(),
              new RealTupleType(realType, coordSys, domain));

        this.realType = realType;
    }

    /*
     * Instance methods:
     */

    /**
     * Returns the VisAD RealType of this quantity.
     *
     * @return                  The VisAD RealType of this quantity.
     */
    public final RealType getRealType() {
        return realType;
    }

    /**
     * Returns the natural {@link MathType} of this quantity.
     *
     * @return                  The natural {@link MathType} of this quantity.
     *                          The class of the object is {@link RealType}.
     */
    public final MathType getMathType() {
        return getRealType();
    }

    /**
     * Returns the single value of this quantity corresponding to a numeric
     * amount in the default unit.  The error estimate will be
     * <code>null</code>.
     *
     * @param amount            The numeric value.
     * @return                  The single value of this quantity corresponding
     *                          to the input.
     * @throws VisADException   VisAD failure.
     * @see #newReal(double amount, Unit unit)
     */
    public final Real newReal(double amount) throws VisADException {
        return newReal(amount, realType.getDefaultUnit());
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
     * @see #newReal(double amount, Unit unit, ErrorEstimate error)
     */
    public final Real newReal(double amount, Unit unit)
            throws VisADException {
        return newReal(amount, unit, (ErrorEstimate) null);
    }

    /**
     * Returns the single value of this quantity corresponding to a numeric
     * amount, a unit, and an error estimate.  This is the most general factory
     * method for creating scalar values.
     *
     * @param amount            The numeric value.
     * @param unit              The unit of the numeric value.  May be
     *                          <code>null</code>.
     * @param error             The error estimate.  May be <code>null</code>.
     * @return                  The single value of this quantity corresponding
     *                          to the input.
     * @throws VisADException   VisAD failure.
     */
    public Real newReal(double amount, Unit unit, ErrorEstimate error)
            throws VisADException {
        return new Real(realType, amount, unit, error);
    }

    /**
     * Returns a single tuple of this quantity.
     *
     * @param amount            The numeric value.
     * @param unit              The unit of the numeric value.  May be
     *                          <code>null</code>.
     * @return                  The single tuple corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #newRealTuple(double[], Unit[])
     */
    public final RealTuple newRealTuple(double amount, Unit unit)
            throws VisADException, RemoteException {
        return newRealTuple(new double[] { amount }, new Unit[] { unit });
    }

    /**
     * Returns a single tuple of this quantity.
     *
     * @param amount            The numeric value.
     * @param unit              The unit of the numeric value.  May be
     *                          <code>null</code>.
     * @param coordSys          The coordinate system transformation for this
     *                          particular tuple.  Must be compatible with the
     *                          default coordinate system transformation.  May
     *                          be <code>null</code>.
     * @return                  The single tuple corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #newRealTuple(double[], Unit[], CoordinateSystem)
     */
    public final RealTuple newRealTuple(double amount, Unit unit,
                                        CoordinateSystem coordSys)
            throws VisADException, RemoteException {
        return newRealTuple(new double[] { amount }, new Unit[] { unit },
                            coordSys);
    }

    /**
     * Returns a single tuple of this quantity.
     *
     * @param amount            The numeric value.
     * @param unit              The unit of the numeric value.  May be
     *                          <code>null</code>.
     * @param error             The error estimate.  May be <code>null</code>.
     * @param coordSys          The coordinate system transformation for this
     *                          particular tuple.  Must be compatible with the
     *                          default coordinate system transformation.  May
     *                          be <code>null</code>.
     * @return                  The single tuple corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #newRealTuple(double[], Unit[] ErrorEstimate[], CoordinateSystem)
     */
    public RealTuple newRealTuple(double amount, Unit unit,
                                  ErrorEstimate error,
                                  CoordinateSystem coordSys)
            throws VisADException, RemoteException {
        return newRealTuple(new double[] { amount }, new Unit[] { unit },
                            new ErrorEstimate[] { error }, coordSys);
    }

    /**
     * Returns a single tuple of this quantity.
     *
     * @param value             The value.
     * @param coordSys          The coordinate system transformation.  May be
     *                          <code>null</code>, in which case the default
     *                          coordinate system transformation is used.
     * @return                  The single value corresponding to the input.
     *                          The class of the object is {@link RealTuple}.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #newRealTuple(Real[], CoordinateSystem)
     */
    public RealTuple newRealTuple(Real value, CoordinateSystem coordSys)
            throws VisADException, RemoteException {
        return newRealTuple(new Real[] { value }, coordSys);
    }

    /**
     * Returns the single value of this quantity corresponding to numeric
     * amounts, units, error estimates, and coordinate system.
     *
     * @param amounts           The numerical amounts.  Must have only a single
     *                          element.
     * @param units             The units of the amounts.  May be
     *                          <code>null</code>; otherwise, must have only
     *                          a single element, which is the unit for the
     *                          respective numerical amount (and may, itself, be
     *                          <code>null</code>).
     * @param errors            The uncertainties of the numerical amounts.  May
     *                          be <code>null</code>; otherwise, must have only
     *                          a single element, which is the uncertainty of
     *                          the numerical amount (and may, itself, be
     *                          <code>null</code>).
     * @param coordSys          The coordinate system transformation.  Must be
     *                          <code>null</code>.
     * @return                  The single value corresponding to the input.
     *                          The class of the object will be {@link Real}.
     * @throws VisADException   VisAD failure.
     */
    public DataImpl newValue(double[] amounts, Unit[] units,
                             ErrorEstimate[] errors,
                             CoordinateSystem coordSys)
            throws VisADException {

        if (amounts.length != 1) {
            throw new TypeException(getClass().getName()
                                    + ".newData: Bad amount-array length");
        }

        if ((units != null) && (units.length != 1)) {
            throw new TypeException(getClass().getName()
                                    + ".newData: Bad units-array length");
        }

        if ((errors != null) && (errors.length != 1)) {
            throw new TypeException(getClass().getName()
                                    + ".newData: Bad errors-array length");
        }

        if (coordSys != null) {
            throw new TypeException(getClass().getName()
                                    + ".newData: Non-null CoordinateSystem");
        }

        return newReal(amounts[0], (units == null)
                                   ? null
                                   : units[0], (errors == null)
                ? null
                : errors[0]);
    }

    /**
     * Indicates if a VisAD MathType is compatible with this instance.  A
     * RealType is compatible if its {@link RealType#equalsExceptNameButUnits}
     * method returns true when given the return value of {@link
     * #getRealType()} and if this quantity has no coordinate system
     * transformation.  A RealTupleType is compatible if its {@link
     * RealTupleType#equalsExceptNameButUnits} method returns true when given
     * the return value of {@link #getRealTupleType()} and if the coordinate
     * system transformations are compatible.  A SetType is compatible if
     * its RealTupleType is compatible.  A FunctionType is compatible if
     * the MathType of its range is compatible.  All other MathTypes are
     * incompatible.
     *
     * @param type              The VisAD MathType to examine for compatibility.
     * @return                  <code>true</code> if and only if the MathType is
     *                          compatible with this instance.
     * @throws VisADException   VisAD failure.
     */
    public boolean isCompatible(MathType type) throws VisADException {

        boolean isCompatible;

        if (type instanceof RealType) {
            isCompatible =
                ((RealType) type).equalsExceptNameButUnits(realType)
                && (getRealTupleType().getCoordinateSystem() == null);
        } else if (type instanceof RealTupleType) {
            RealTupleType thisTupleType = getRealTupleType();
            RealTupleType thatTupleType = (RealTupleType) type;

            if ( !thatTupleType.equalsExceptNameButUnits(thisTupleType)) {
                isCompatible = false;
            } else {
                CoordinateSystem thisCS = thisTupleType.getCoordinateSystem();
                CoordinateSystem thatCS = thatTupleType.getCoordinateSystem();

                isCompatible = ((thisCS == null)
                                ? thatCS == null
                                : thisCS.getReference()
                                    .equalsExceptNameButUnits(thatCS
                                        .getReference()));
            }
        } else if (type instanceof SetType) {
            isCompatible = isCompatible(((SetType) type).getDomain());
        } else if (type instanceof FunctionType) {
            isCompatible = isCompatible(((FunctionType) type).getRange());
        } else {
            isCompatible = false;
        }

        return isCompatible;
    }
}
