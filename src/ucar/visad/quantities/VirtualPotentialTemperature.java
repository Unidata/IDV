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

import visad.*;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of atmospheric virtual potential
 * temperature.  The equation for virtual potential temperature uses potential
 * temperature in place of in-situ temperature in the equation for virtual
 * temperature.
 *
 * @author Steven R. Emmerson
 * @version $Id: VirtualPotentialTemperature.java,v 1.10 2005/05/13 18:35:45 jeffmc Exp $
 */
public final class VirtualPotentialTemperature extends VirtualTemperature {

    /**
     * The single instance.
     */
    private static VirtualPotentialTemperature INSTANCE;

    /**
     * Constructs an instance.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private VirtualPotentialTemperature() throws VisADException {
        super("VirtualPotentialTemperature");
    }

    /**
     * Creates a virtual potential temperature data object from data objects of
     * air pressure, in situ air temperature, and dew point.
     *
     * @param pressure          The air pressure data object.
     * @param temperature       The in situ air temperature data object.
     * @param dewPoint          The dew point data object.
     * @return                  The virtual air temperature corresponding to
     *                          the arguments.  The type of the returned object
     *                          will be that of the arguments after standard
     *                          promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has the wrong type.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createFromDewPoint(Data pressure, Data temperature,
                                          Data dewPoint)
            throws TypeException, UnimplementedException, VisADException,
                   RemoteException {
        return VirtualTemperature.createFromDewPoint(pressure,
                PotentialTemperature.create(pressure, temperature), dewPoint);
    }

    /**
     * Creates a virtual potential temperature data object from data objects of
     * in situ air temperature, and water vapor mixing-ratio.
     *
     * @param pressure          The air pressure data object.
     * @param temperature       The in situ air temperature data object.
     * @param mixingRatio       The water vapor mixing-ratio data object.
     * @return                  The virtual air temperature corresponding to
     *                          the arguments.  The type of the returned object
     *                          will be that of the arguments after standard
     *                          promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has the wrong type.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createFromMixingRatio(Data pressure, Data temperature,
                                             Data mixingRatio)
            throws TypeException, UnimplementedException, VisADException,
                   RemoteException {

        return VirtualTemperature.createFromMixingRatio(
            PotentialTemperature.create(pressure, temperature), mixingRatio);
    }
}
