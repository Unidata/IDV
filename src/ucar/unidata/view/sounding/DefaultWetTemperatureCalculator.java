/*
 * $Id: DefaultWetTemperatureCalculator.java,v 1.13 2005/05/13 18:33:28 jeffmc Exp $
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
 * Provides support for the default way of computing a saturation, pseudo-
 * adiabatic trajectory.
 *
 * @author Steven R. Emmerson
 * @version $Id: DefaultWetTemperatureCalculator.java,v 1.13 2005/05/13 18:33:28 jeffmc Exp $
 */
public class DefaultWetTemperatureCalculator
        implements TemperatureCalculator {

    /**
     * The pressure.
     */
    private Real pressure;

    /**
     * The temperature.
     */
    private Real temperature;

    /**
     * The pseudo-adiabatic lapse rate.
     */
    private Real lapseRate;

    /**
     * The dimensionless value two.
     */
    private static Real two;

    static {
        try {
            two = new Real(
                RealType.getRealType(
                    "DefaultWetTemperatureCalculator_CONSTANT",
                    CommonUnit.dimensionless), 2);
        } catch (Exception e) {
            System.err.print("Couldn't initialize class");
            e.printStackTrace();

            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Constructs from the saturation pressure and temperature.
     * @param saturationPressure        The saturation pressure.
     * @param saturationTemperature     The saturation temperature.
     * @throws TypeException    Something has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DefaultWetTemperatureCalculator(Real saturationPressure, Real saturationTemperature)
            throws TypeException, VisADException, RemoteException {

        pressure    = saturationPressure;
        temperature = saturationTemperature;
        lapseRate = (Real) PseudoAdiabaticLapseRate.create(saturationPressure,
                saturationTemperature);
    }

    /**
     * Returns the temperature associated with the next pressures.
     * @param nextPressure      The next, lower pressure.
     * @return                  The temperature associated with the next, lower
     *                          pressure.
     * @throws TypeException    Something has the wrong type.
     * @throws UnitException    Invalid pressure unit.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Real nextTemperature(Real nextPressure)
            throws TypeException, UnitException, VisADException,
                   RemoteException {

        Real nextTemperature = null;

        try {
            Real deltaPressure = (Real) pressure.subtract(nextPressure);
            Real density = (Real) AirDensity.create(
                               pressure,
                               VirtualTemperature.createFromMixingRatio(
                                   temperature,
                                   SaturationMixingRatio.create(
                                       pressure, temperature)));
            Real deltaTemperature =
                (Real) lapseRate.multiply(deltaPressure.divide(density));

            nextTemperature = (Real) temperature.subtract(deltaTemperature);

            Real nextLapseRate =
                (Real) PseudoAdiabaticLapseRate.create(nextPressure,
                                                       nextTemperature);
            Real nextDensity = (Real) AirDensity.create(
                                   nextPressure,
                                   VirtualTemperature.createFromMixingRatio(
                                       nextTemperature,
                                       SaturationMixingRatio.create(
                                           nextPressure, nextTemperature)));

            nextTemperature = (Real) temperature.subtract(
                deltaTemperature.add(
                    nextLapseRate.multiply(
                        deltaPressure.divide(nextDensity))).divide(two));
            pressure    = nextPressure;
            temperature = nextTemperature;
            lapseRate = (Real) PseudoAdiabaticLapseRate.create(pressure,
                    temperature);
        } catch (UnimplementedException e) {}  // can't happen because the above is known to work

        return nextTemperature;
    }
}







