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



import java.lang.reflect.*;

import java.rmi.RemoteException;


/**
 * Provides support for quantities (ex: temperature, wind).  This class uses
 * the facade design pattern to provide a (hopefully) more convenient API to
 * the RealType, Real, RealTupleType, and RealTuple API-s.  It also defines the
 * root of a hierarchy of classes that encapsulate quantity-specific scientific
 * knowledge.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.13 $ $Date: 2005/05/13 18:34:03 $
 */
public abstract class Quantity {

    /**
     * Inner classes:
     */
    private class DependenceMode {}

    /*
     * Fields:
     */

    /**
     * Indicates that a secondary object will be the same object as the primary
     * object.
     */
    public final DependenceMode DEPENDENT = new DependenceMode();

    /**
     * Indicates that a secondary object will be a different object than
     * the primary object.
     */
    public final DependenceMode INDEPENDENT = new DependenceMode();

    /**
     * Indicates that it is irrelevant whether or not a secondary object will be
     * a different object than the primary object.
     */
    public final DependenceMode UNIMPORTANT = new DependenceMode();

    /** name */
    private final String name;

    /** MathType for the quantity */
    private final RealTupleType realTupleType;

    /** map for types */
    private static java.util.Map map = new java.util.TreeMap();

    /*
     * Constructors:
     */

    /**
     * Constructs from a name and a RealTupleType.  Package private to prevent
     * use.
     *
     * @param name              The name for the quantity.
     * @param realTupleType     The RealTupleType of the quantity.
     * @throws TypeException    A quantity with the given name already exists.
     * @throws VisADException   VisAD failure.
     */
    Quantity(String name, RealTupleType realTupleType)
            throws TypeException, VisADException {

        synchronized (map) {
            RealTupleType oldQuantity = (RealTupleType) map.get(name);

            if (oldQuantity == null) {
                map.put(name, realTupleType);
            } else {
                throw new TypeException(getClass().getName() + ".<init>: "
                                        + "Desired quantity <<" + name
                                        + ">> already exists as <<"
                                        + oldQuantity + ">>");
            }
        }

        this.name          = name;
        this.realTupleType = realTupleType;
    }

    /*
     * Class methods:
     */

    /*
     * Instance methods:
     */

    /**
     * Returns the name of this quantity.
     *
     * @return                  The name of this quantity.
     */
    public final String getName() {
        return name;
    }

    /**
     * Returns the VisAD {@link RealTupleType} of this quantity.
     *
     * @return                  The VisAD RealTupleType of this quantity.
     */
    public final RealTupleType getRealTupleType() {
        return realTupleType;
    }

    /**
     * Returns the natural {@link MathType} of this quantity.  This method
     * should be overridden in subclasses when appropriate.
     *
     * @return                  The natural {@link MathType} of this quantity.
     *                          The class of the object is either {@link
     *                          RealType} or {@link RealTupleType}.
     */
    public MathType getMathType() {
        return getRealTupleType();
    }

    /**
     * Returns a single tuple of this quantity.  The coordinate system
     * transformation specific to this instance will be <code>null</code>.
     *
     * @param amounts           The numeric values.
     * @param units             The units of the numeric values.  May be
     *                          <code>null</code>.
     * @return                  The single tuple corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #newRealTuple(double[], Unit[], CoordinateSystem)
     */
    public final RealTuple newRealTuple(double[] amounts, Unit[] units)
            throws VisADException, RemoteException {
        return newRealTuple(amounts, units, (CoordinateSystem) null);
    }

    /**
     * Returns a single tuple of this quantity.  The error estimate specific to
     * this instance will be <code>null</code>.
     *
     * @param amounts           The numeric values.
     * @param units             The units of the numeric values.  May be
     *                          <code>null</code>.
     * @param coordSys          The coordinate system transformation for this
     *                          particular tuple.  Must be compatible with the
     *                          default coordinate system transformation.  May
     *                          be <code>null</code>.
     * @return                  The single tuple corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #newRealTuple(double[], Unit[], ErrorEstimate[], CoordinateSystem)
     */
    public final RealTuple newRealTuple(double[] amounts, Unit[] units,
                                        CoordinateSystem coordSys)
            throws VisADException, RemoteException {
        return newRealTuple(amounts, units, (ErrorEstimate[]) null, coordSys);
    }

    /**
     * Returns a single tuple of this quantity.
     *
     * @param amounts           The numeric values.
     * @param units             The units of the numeric values.  May be
     *                          <code>null</code>.
     * @param errors            The error estimates.  May be <code>null</code>.
     * @param coordSys          The coordinate system transformation for this
     *                          particular tuple.  Must be compatible with the
     *                          default coordinate system transformation.  May
     *                          be <code>null</code>.
     * @return                  The single tuple corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #newRealTuple(Real[], CoordinateSystem)
     */
    public RealTuple newRealTuple(double[] amounts, Unit[] units,
                                  ErrorEstimate[] errors,
                                  CoordinateSystem coordSys)
            throws VisADException, RemoteException {

        Real[] values = new Real[amounts.length];

        for (int i = 0; i < amounts.length; ++i) {
            values[i] =
                new visad.Real((RealType) realTupleType.getComponent(i),
                               amounts[i], (units == null)
                                           ? null
                                           : units[i], (errors == null)
                    ? null
                    : errors[i]);
        }

        return newRealTuple(values, coordSys);
    }

    /**
     * Returns a single tuple of this quantity.
     *
     * @param values            The values.
     * @param coordSys          The coordinate system transformation.  May be
     *                          <code>null</code>, in which case the default
     *                          coordinate system transformation is used.
     * @return                  The single value corresponding to the input.
     *                          The class of the object is {@link
     *                          visad.RealTuple}.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealTuple newRealTuple(Real[] values, CoordinateSystem coordSys)
            throws VisADException, RemoteException {
        return new RealTuple(realTupleType, values, coordSys);
    }

    /**
     * Returns a single value of this quantity.  This method should be
     * overridden when appropriate.
     *
     * @param amounts           The numerical amounts.
     * @param units             The units of the amounts.  May be
     *                          <code>null</code> -- in which case the defualt
     *                          units are used; otherwise, element
     *                          <code>i</code> is the unit for the respective
     *                          numerical amount (and may, itself, be
     *                          <code>null</code> -- in which case the default
     *                          unit is used).
     * @param errors            The uncertainties of the numerical amounts.
     *                          May be <code>null</code>, in which case the
     *                          undertainties are zero; otherwise, element
     *                          <code>i</code> is the uncertainty for the
     *                          respectve numerical amount (and may, itself, be
     *                          <code>null</code> -- in which case the
     *                          uncertainty is zero).
     * @param coordSys          The coordinate system transformation.  May be
     *                          <code>null</code>, in which case the default
     *                          coordinate system transformation is used.
     * @return                  The single value corresponding to the input.
     *                          Unless overridden, the class of the object is
     *                          {@link RealTuple}.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DataImpl newValue(double[] amounts, Unit[] units,
                             ErrorEstimate[] errors,
                             CoordinateSystem coordSys)
            throws VisADException, RemoteException {
        return newRealTuple(amounts, units, errors, coordSys);
    }

    /**
     * Indicates if a VisAD data object is compatible with this
     * instance.  A VisAD data object is compatible if its VisAD {@link
     * MathType} is compatible.
     *
     * @param data              The VisAD data object to examine for
     *                          compatibility.
     * @return                  <code>true</code> if and only if the MathType of
     *                          the VisAD data object is compatible with this
     *                          instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #isCompatible(MathType)
     */
    public boolean isCompatible(Data data)
            throws VisADException, RemoteException {
        return isCompatible(data.getType());
    }

    /**
     * Indicates if a VisAD MathType is compatible with this
     * instance.  A RealTupleType is compatible if its {@link
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

        if (type instanceof RealTupleType) {
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
    public final void vet(visad.Data data)
            throws TypeException, VisADException, RemoteException {
        vet(data.getType());
    }

    /**
     * Vets a VisAD MathType for compatibility.
     *
     * @param type              The VisAD MathType to examine for compatibility.
     * @throws VisADException   if a core VisAD failure occurs.
     */
    public final void vet(MathType type) throws VisADException {

        if ( !isCompatible(type)) {
            throw new TypeException(getClass().getName()
                                    + ".vet(MathType): Incompatible type: "
                                    + type);
        }
    }

    /**
     * Ensures that a VisAD {@link RealTuple} is of this quantity.
     *
     * @param realTuple         The VisAD {@link RealTuple} to be of this
     *                          quantity.
     * @param mode              The dependence relationship between the original
     *                          object and the returned object.  This argument
     *                          determines whether changes to the returned
     *                          object might or will be reflected in the
     *                          original object.
     * @throws TypeException    The argument can't be seen as data of this
     *                          quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     *
     * public RealTuple ensureEquality(RealTuple original, DependenceMode mode)
     *   throws TypeException, VisADException, RemoteException
     * {
     *   RealTuple       result;
     *   RealTupleType   originalType = (RealTupleType)original.getType();
     *   if (realTupleType.equals(originalType))
     *   {
     *       result =
     *           mode == INDEPENDENT
     *               ? (RealTuple)original.dataClone()
     *               : original;
     *   }
     *   else if (isCompatible(originalType))
     *   {
     *       if (mode == DEPENDENT)
     *           throw new VisADException(
     *               getClass().getName() + ".ensureEquality: " +
     *               "Can't modify a RealTuple");
     *       result = original.changeMathType(realTupleType);
     *   }
     *   else
     *   {
     *       if (mode == DEPENDENT)
     *           throw new VisADException(
     *               getClass().getName() + ".ensureEquality: " +
     *               "Can't modify a RealTuple");
     *       int         i;
     *       int         rank = realTupleType.getDimension();
     *       int         indexes = new int[rank];
     *       for (i = 0; i < rank; ++i)
     *       {
     *           RealType        realType = realTupleType.getComponent(i);
     *           int             index = originalType.getIndex(realType);
     *           if (index < 0)
     *               break;
     *           indexes[i] = index;
     *       }
     *       if (i < rank)
     *       {
     *           int     originalRank = originalType.getDimension();
     *           for (i = 0; i < rank; ++i)
     *           {
     *               RealType    realType = realTupleType.getComponent(i);
     *               for (int j = 0; j < originalRank; ++j)
     *               {
     *                   RealType        originalRealType =
     *                       originalType.getComponent(j);
     *                   if (!realType.equalsExceptNameButUnits(
     *                       originalRealType))
     *                   {
     *                       indexes[i] = index;
     *                       break;
     *                   }
     *               }
     *               if (j
     *           }
     *       }
     *   }
     *   return result;
     * }
     */
}
