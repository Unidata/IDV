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

package ucar.visad.functiontypes;



import ucar.visad.quantities.AirPressure;

import visad.FunctionType;

import visad.MathType;

import visad.VisADException;


/**
 * Provides support for atmospheric horizontal wind profiles in cartesian
 * coordinates on a pressure domain.
 *
 * @author Don Murray
 * @version $Id: CartesianHorizontalWindOfPressure.java,v 1.11 2005/05/13 18:34:49 jeffmc Exp $
 */
public final class CartesianHorizontalWindOfPressure extends CartesianHorizontalWindProfile {

    /**
     * The singleton instance.
     */
    private static CartesianHorizontalWindOfPressure INSTANCE;

    /**
     * Constructs from a MathType for the domain.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected CartesianHorizontalWindOfPressure() throws VisADException {
        super(AirPressure.getRealTupleType());
    }

    /**
     * Obtains an instance of this class.
     *
     * @return                  An instance of this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static FunctionType instance() throws VisADException {

        if (INSTANCE == null) {
            synchronized (CartesianHorizontalWindOfPressure.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CartesianHorizontalWindOfPressure();
                }
            }
        }

        return INSTANCE;
    }
}
