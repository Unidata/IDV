/*
 * $Id: DryTemperatureCalculator.java,v 1.11 2005/05/13 18:33:29 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.sounding;



import java.rmi.RemoteException;

import ucar.visad.*;
import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;


/**
 * Provides support for calculating the dry portion of the pseud-adiabatic
 * trajectory of a lifted parcel of air.
 *
 * @author Steven R. Emmerson
 * @version $Id: DryTemperatureCalculator.java,v 1.11 2005/05/13 18:33:29 jeffmc Exp $
 */
public class DryTemperatureCalculator implements TemperatureCalculator {

    /**
     * The potential temperature of the parcel.
     */
    private final Real potentialTemperature;

    /**
     * Constructs from a starting pressure and temperature.
     * @param startPressure     The starting pressure.
     * @param startTemperature  The starting temperature.
     * @throws TypeException    Something has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DryTemperatureCalculator(Real startPressure, Real startTemperature)
            throws TypeException, VisADException, RemoteException {

        potentialTemperature =
            (Real) PotentialTemperature.create(startPressure,
                                               startTemperature);
    }

    /**
     * Returns the next temperature associated with the next, lower pressure.
     * @param nextPressure      The next, lower pressure.
     * @return                  The next temperature.
     * @throws UnitException    Invalid argument unit.
     * @throws TypeException    Invalid argument type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Real nextTemperature(Real nextPressure)
            throws TypeException, UnitException, VisADException,
                   RemoteException {
        return (Real) PotentialTemperature.createAirTemperature(nextPressure,
                potentialTemperature);
    }
}







