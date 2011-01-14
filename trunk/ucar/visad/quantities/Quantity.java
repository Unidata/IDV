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



import visad.RealTupleType;

import visad.RealType;


/**
 * Provides support for physical quantities.
 *
 * @author Steven R. Emmerson
 * @version $Id: Quantity.java,v 1.11 2005/05/13 18:35:42 jeffmc Exp $
 */
public abstract class Quantity {

    /** RealTupleType for a quantity */
    private RealTupleType realTupleType;

    /**
     * Constructs from nothing.
     */
    protected Quantity() {}

    /**
     * Constructs from an existing {@link RealTupleType}.
     *
     * @param realTupleType    The existing {@link RealTupleType}.
     */
    protected Quantity(RealTupleType realTupleType) {
        this.realTupleType = realTupleType;
    }

    /**
     * Returns the associated {@link RealTupleType}.
     *
     * @return                 The associated {@link RealTupleType}.
     */
    protected final RealTupleType realTupleType() {
        return realTupleType;
    }

    /**
     * Should only be used by subclasses immediately after super() invocation.
     *
     * @param realTupleType    Sets the associated {@link RealTupleType}.
     */
    protected final void setRealTupleType(RealTupleType realTupleType) {
        this.realTupleType = realTupleType;
    }
}
