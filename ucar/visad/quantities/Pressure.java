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



import visad.CommonUnit;

import visad.CoordinateSystem;

import visad.RealTupleType;

import visad.RealType;

import visad.Set;

import visad.Unit;

import visad.UnitException;

import visad.VisADException;


/**
 * Provides support for the quantity of pressure.
 *
 * @author Steven R. Emmerson
 * @version $Id: Pressure.java,v 1.10 2005/05/13 18:35:42 jeffmc Exp $
 */
public class Pressure extends ScalarQuantity {

    /**
     * The single instance.
     */
    private static Pressure INSTANCE;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private Pressure() throws VisADException {
        this("Pressure");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for the instance.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Pressure(String name) throws VisADException {
        this(name, CommonUnits.PASCAL);
    }

    /**
     * Constructs from a name and a unit.
     * @param name              The name of the quantity.
     * @param unit              The default unit of the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Pressure(String name, Unit unit) throws VisADException {
        super(name, unit, (Set) null);
    }

    /**
     * Constructs from a RealType and a CoordinateSystem.
     * @param realType          The RealType of the quantity.
     * @param defaultCS         The default domain CoordinateSystem.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Pressure(RealType realType, CoordinateSystem defaultCS)
            throws VisADException {
        super(realType, defaultCS);
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
    public static RealTupleType getRealTupleType() throws VisADException {

        if (INSTANCE == null) {
            synchronized (Pressure.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Pressure();
                }
            }
        }

        return INSTANCE.realTupleType();
    }
}
