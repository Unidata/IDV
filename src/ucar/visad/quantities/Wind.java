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

package ucar.visad.quantities;



import visad.CoordinateSystem;

import visad.RealTupleType;

import visad.RealType;

import visad.Set;

import visad.VisADException;


/**
 * Provides support for the quantity of wind.
 *
 * @author Steven R. Emmerson
 * @version $Id: Wind.java,v 1.8 2005/05/13 18:35:46 jeffmc Exp $
 */
public abstract class Wind extends VectorQuantity {

    /**
     * Constructs from an array of RealType-s.
     *
     * @param types             The array of RealType-s.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Wind(RealType[] types) throws VisADException {
        this(types, (CoordinateSystem) null);
    }

    /**
     * Constructs from an array of RealType-s and a coordinate system
     * transformation.
     *
     * @param types             The array of RealType-s.
     * @param coordSys          The coordinate system transformation for this
     *                          quantity.  May be <code>null</code>.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Wind(RealType[] types, CoordinateSystem coordSys)
            throws VisADException {
        super(types, coordSys);
    }
}
