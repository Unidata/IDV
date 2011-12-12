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


import ucar.visad.Util;

import ucar.visad.VisADMath;

import visad.Data;

import visad.RealTupleType;

import visad.RealType;

import visad.SI;

import visad.Set;

import visad.TypeException;

import visad.VisADException;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of massic volume (alias "specific
 * volume" or "volume per mass").
 *
 * @author Steven R. Emmerson
 * @version $Id: MassicVolume.java,v 1.4 2005/05/13 18:35:41 jeffmc Exp $
 */
public final class MassicVolume extends ScalarQuantity {

    /*
     * The single instance.
     */

    /** the single instance */
    private static MassicVolume INSTANCE;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private MassicVolume() throws VisADException {
        super("MassicVolume",
              SI.meter.pow(3).divide(SI.kilogram).clone("m3/kg"), (Set) null);
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
            INSTANCE = new MassicVolume();
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Creates an MassicVolume data object from a density data object.
     *
     * @param density           A Density data object.
     * @return                  The MassicVolume data object computed from the
     *                          density.  The type of the object will be that
     *                          of the arguments after standard promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has wrong type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createFromDensity(Data density)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(Density.getRealType(), density);

        return Util.clone(VisADMath.invert(density), getRealType());
    }
}
