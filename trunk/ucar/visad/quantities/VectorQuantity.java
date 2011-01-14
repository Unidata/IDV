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
import visad.EarthVectorType;

import visad.RealTupleType;

import visad.RealType;

import visad.Set;

import visad.VisADException;


/**
 * Provides support for vector quantities.  A vector quantity has at least one
 * component (and usually at least two components) and may have associated with
 * it a default CoordinateSystem and default domain set.
 *
 * @author Steven R. Emmerson
 * @version $Id: VectorQuantity.java,v 1.12 2006/08/09 22:04:54 dmurray Exp $
 */
public abstract class VectorQuantity extends Quantity {

    /** EarthVectorType for a quantity */
    private EarthVectorType earthVectorType = null;


    /**
     * Constructs from an array of VisAD {@link RealType}s.
     *
     * @param realTypes         The array of {@link RealType}s.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected VectorQuantity(RealType[] realTypes) throws VisADException {
        this(realTypes, (CoordinateSystem) null);
    }

    /**
     * Constructs from an array of VisAD {@link RealType}s and a default
     * coordinate system transformation.
     *
     * @param realTypes         The array of {@link RealType}s.
     * @param coordinateSystem  The default CoordinateSystem of this quantity.
     *                          May be <code>null</code>.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected VectorQuantity(RealType[] realTypes,
                             CoordinateSystem coordinateSystem)
            throws VisADException {
        this(realTypes, coordinateSystem, (Set) null);
    }

    /**
     * Constructs from an array of VisAD {@link RealType}s, a default
     * coordinate system transformation, and a default sampling set.
     *
     * @param realTypes         The array of {@link RealType}s.
     * @param coordinateSystem  The default CoordinateSystem of this quantity.
     *                          May be <code>null</code>.
     * @param set               The default domain set of this quantity.  May be
     *                          <code>null</code>.
     * @throws VisADException   if a core VisAD failure occurs.
     */
    protected VectorQuantity(RealType[] realTypes,
                             CoordinateSystem coordinateSystem, Set set)
            throws VisADException {
        this(new RealTupleType(realTypes, coordinateSystem, set));
    }

    /**
     * Constructs from an existing VisAD {@link RealTupleType}.
     *
     * @param realTupleType     An existing {@link RealTupleType}.
     */
    protected VectorQuantity(RealTupleType realTupleType) {
        super(realTupleType);
    }

    /**
     * Returns the associated {@link RealTupleType}.
     *
     * @return                 The associated {@link RealTupleType}.
     *
     * @throws VisADException   if a core VisAD failure occurs.
     */
    protected final EarthVectorType earthVectorType() throws VisADException {
        if (earthVectorType == null) {
            earthVectorType =
                new EarthVectorType(realTupleType().getRealComponents(),
                                    realTupleType().getCoordinateSystem());
        }
        return earthVectorType;
    }

}
