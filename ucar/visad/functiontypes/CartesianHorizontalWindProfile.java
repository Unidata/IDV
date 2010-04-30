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

package ucar.visad.functiontypes;



import ucar.visad.quantities.CartesianHorizontalWind;

import visad.MathType;

import visad.VisADException;


/**
 * Provides support for profiles of the horizontal wind.
 *
 * @author Steven R. Emmerson
 * @version $Id: CartesianHorizontalWindProfile.java,v 1.9 2005/05/13 18:34:49 jeffmc Exp $
 */
public abstract class CartesianHorizontalWindProfile extends HorizontalWindProfile {

    /**
     * Constructs from MathType-s for the domain.
     *
     * @param domainType        The MathType of the domain.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected CartesianHorizontalWindProfile(MathType domainType)
            throws VisADException {
        super(domainType, CartesianHorizontalWind.getRealTupleType());
    }
}
