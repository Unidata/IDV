/*
 * $Id: DefaultWetTemperatureCalculatorFactory.java,v 1.12 2005/05/13 18:33:28 jeffmc Exp $
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

import visad.*;


/**
 * Provides support for creating default calculators of saturation, pseudo-
 * adiabatic trajectories.
 *
 * @author Steven R. Emmerson
 * @version $Id: DefaultWetTemperatureCalculatorFactory.java,v 1.12 2005/05/13 18:33:28 jeffmc Exp $
 */
public class DefaultWetTemperatureCalculatorFactory
        implements TemperatureCalculatorFactory {

    /**
     * The single instance of this class.
     */
    private static DefaultWetTemperatureCalculatorFactory instance;

    static {
        try {
            instance = new DefaultWetTemperatureCalculatorFactory();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs from nothing.
     */
    private DefaultWetTemperatureCalculatorFactory() {}

    /**
     * Returns an instance.
     * @return                  An instance of this class.
     */
    public static TemperatureCalculatorFactory instance() {
        return instance;
    }

    /**
     * Returns an instance of a saturation, pseudo-adiabatic trajectory
     * calculator based on the saturation pressure and temperature.
     *
     * @param saturationPressure        The saturation pressure.
     * @param saturationTemperature     The saturation temperature.
     * @return                          A saturation, pseudo-adiabatic
     *                                  trajectory calculator.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public TemperatureCalculator newTemperatureCalculator(Real saturationPressure, Real saturationTemperature)
            throws VisADException, RemoteException {
        return new DefaultWetTemperatureCalculator(saturationPressure,
                                                   saturationTemperature);
    }
}







