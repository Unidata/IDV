/*
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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


import ucar.visad.*;

import visad.*;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of equivalent potential
 * temperature.</p>
 *
 * <p>An instance of this class is immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: EquivalentPotentialTemperature.java,v 1.10 2005/05/13 18:35:39 jeffmc Exp $
 */
public class EquivalentPotentialTemperature extends PotentialTemperature {

    /**
     * The single instance.
     */
    private static EquivalentPotentialTemperature INSTANCE;

    /**
     * Constructs an instance.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private EquivalentPotentialTemperature() throws VisADException {
        this("EquivalentPotentialTemperature");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for this quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected EquivalentPotentialTemperature(String name)
            throws VisADException {
        super(name);
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
            synchronized (EquivalentPotentialTemperature.class) {
                if (INSTANCE == null) {
                    INSTANCE = new EquivalentPotentialTemperature();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Creates an equivalent potential temperature data object.
     *
     * @param pressure          The air pressure data object.
     * @param temperature       The temperature data object.
     * @param mixingRatio       The water-vapor mixing-ratio data object.
     * @return                  The equivalent potential temperature
     *                          data object corresponding to the input
     *                          arguments.
     * @throws TypeException    An input argument has wrong type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature,
                              Data mixingRatio)
            throws TypeException, VisADException, RemoteException {

        return Util.clone(PotentialTemperature.create(pressure,
                AdiabaticEquivalentTemperature.create(temperature,
                    mixingRatio)), getRealType());
    }
}
