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
 * Provides support for the quantity of massic energy (alias "specific
 * energy" or "energy per mass").
 *
 * @author Steven R. Emmerson
 * @version $Id: MassicEnergy.java,v 1.4 2005/05/13 18:35:41 jeffmc Exp $
 */
public final class MassicEnergy extends ScalarQuantity {

    /*
     * The single instance.
     */

    /** the single instance */
    private static MassicEnergy INSTANCE;

    static {
        try {
            INSTANCE = new MassicEnergy();
        } catch (Exception ex) {
            System.err.println("Couldn't initialize class");
            ex.printStackTrace();
            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private MassicEnergy() throws VisADException {
        super("MassicEnergy",
              SI.meter.divide(SI.second).pow(2).clone("J/kg"), (Set) null);
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

        return INSTANCE.realTupleType();
    }
}
