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



import visad.CoordinateSystem;

import visad.DoubleSet;

import visad.FloatSet;

import visad.RealTupleType;

import visad.RealType;

import visad.Set;

import visad.TypeException;

import visad.Unit;

import visad.VisADException;


/**
 * Provides support for scalar quantities.
 *
 * @author Steven R. Emmerson
 * @version $Id: ScalarQuantity.java,v 1.11 2005/05/13 18:35:44 jeffmc Exp $
 */
public abstract class ScalarQuantity extends Quantity {

    /**
     * Constructs from a name, default unit, and default sampling set.
     *
     * @param name            The name.
     * @param unit            The default unit.
     * @param set             The default sampling set.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected ScalarQuantity(String name, Unit unit, Set set)
            throws VisADException {
        this(getRealType(name, unit, set));
    }

    /**
     * Returns a {@link RealType} that is compatible with this class given a
     * name and a compatible unit.
     *
     * @param name            The name for the RealType.
     * @param unit            A compatible unit for the RealType.
     * @return                A corresponding RealType.
     * @throws TypeException  if a necessary RealType can't be created.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static RealType getRealType(String name, Unit unit)
            throws TypeException, VisADException {
        return getRealType(name, unit, (Set) null);
    }

    /**
     * Returns a {@link RealType} that is compatible with this class given a
     * name, a compatible unit, and a compatible sampling set.
     *
     * @param name            The name for the RealType.
     * @param unit            A compatible unit for the RealType.
     * @param set             A compatible sampling set for the RealType.
     * @return                A corresponding RealType.
     * @throws TypeException  if a necessary RealType can't be created.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static RealType getRealType(String name, Unit unit, Set set)
            throws TypeException, VisADException {

        RealType realType = RealType.getRealTypeByName(name);

        if (realType == null) {
            realType = RealType.getRealType(name, unit, set);
        } else {
            Unit oldUnit = realType.getDefaultUnit();

            if ( !Unit.canConvert(oldUnit, unit)) {
                throw new TypeException(
                    "\"" + name
                    + "\" RealType already exists with incompatible unit: "
                    + oldUnit + ", " + unit + ")");
            }

            Set oldSet = realType.getDefaultSet();

            if ((set != null)
                    && (((set instanceof DoubleSet)
                         && !(oldSet instanceof DoubleSet)) || ((set
                              instanceof FloatSet) && !((oldSet
                                  instanceof DoubleSet) || (oldSet
                                      instanceof FloatSet))))) {
                throw new TypeException(
                    "\"" + name
                    + "\" RealType already exists with incompatible set: "
                    + oldSet + ", " + set + ")");
            }
        }

        return realType;
    }

    /**
     * Constructs from an existing {@link RealType}.
     *
     * @param realType        The existing RealType.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected ScalarQuantity(RealType realType) throws VisADException {
        this(realType, (CoordinateSystem) null);
    }

    /**
     * Constructs from an existing {@link RealType} and default {@link
     * CoordinateSystem}.
     *
     * @param realType        The existing RealType.
     * @param defaultCS       The default CoordinateSystem.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected ScalarQuantity(RealType realType, CoordinateSystem defaultCS)
            throws VisADException {
        this(realType, defaultCS, (Set) null);
    }

    /**
     * Constructs from an existing {@link RealType}, default {@link
     * CoordinateSystem}, and default sampling set.
     *
     * @param realType        The existing RealType.
     * @param defaultCS       The default CoordinateSystem.
     * @param defaultSet      The default sampling set.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected ScalarQuantity(RealType realType, CoordinateSystem defaultCS,
                             Set defaultSet)
            throws VisADException {
        this(new RealTupleType(realType, defaultCS, defaultSet));
    }

    /**
     * Constructs from an existing {@link RealTupleType}.
     *
     * @param realTupleType   The existing RealTupleType.
     */
    protected ScalarQuantity(RealTupleType realTupleType) {
        super(realTupleType);
    }

    /**
     * Returns the associated {@link RealType}.
     *
     * @return                The associated {@link RealType}.
     *
     * @throws VisADException
     */
    protected final RealType realType() throws VisADException {
        return (RealType) realTupleType().getComponent(0);
    }
}
