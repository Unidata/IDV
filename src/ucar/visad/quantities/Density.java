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

import visad.SI;

import visad.Set;

import visad.Unit;

import visad.UnitException;

import visad.VisADException;


/**
 * Provides support for the quantity of density.
 *
 * @author Steven R. Emmerson
 * @version $Id: Density.java,v 1.9 2005/05/13 18:35:38 jeffmc Exp $
 */
public class Density extends ScalarQuantity {

    /**
     * The single instance.
     */
    private static Density INSTANCE;

    /**
     * Constructs from nothing.
     * @throws UnitException    Unit conversion failure.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private Density() throws UnitException, VisADException {
        this("Density");
    }

    /**
     * Constructs from a name.
     * @param name              The name of the quantity.
     * @throws UnitException    Unit conversion failure.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Density(String name) throws UnitException, VisADException {
        this(name, SI.kilogram.divide(SI.meter.pow(3)));
    }

    /**
     * Constructs from a name and a unit.  This is the most general constructor.
     * @param name              The name of the quantity.
     * @param unit              The default unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Density(String name, Unit unit) throws VisADException {
        super(name, unit, (Set) null);
    }

    /**
     * Obtains the RealType associated with this class.
     *
     * @return                  The RealType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealType getRealType() throws VisADException {
        return (RealType) getRealTupleType().getComponent(0);
    }

    /**
     * Obtains the RealTupleType associated with this class.
     *
     * @return                  The RealTupleType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static synchronized RealTupleType getRealTupleType()
            throws VisADException {

        if (INSTANCE == null) {
            INSTANCE = new Density();
        }

        return INSTANCE.realTupleType();
    }
}
