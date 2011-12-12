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

import visad.RealType;

import visad.SI;

import visad.Unit;

import visad.VisADException;


/**
 * Provides support for the quantity of horizontal wind.
 *
 * @author Steven R. Emmerson
 * @version $Id: HorizontalWind.java,v 1.9 2005/05/13 18:35:40 jeffmc Exp $
 */
public abstract class HorizontalWind extends Wind {

    /**
     * The default speed unit.
     */
    public static final Unit DEFAULT_SPEED_UNIT;

    static {
        try {
            DEFAULT_SPEED_UNIT = SI.meter.divide(SI.second);
        } catch (Exception ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Constructs from an array of RealType-s.
     *
     * @param types             The array of RealType-s.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected HorizontalWind(RealType[] types) throws VisADException {
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
    protected HorizontalWind(RealType[] types, CoordinateSystem coordSys)
            throws VisADException {
        super(types, coordSys);
    }
}
