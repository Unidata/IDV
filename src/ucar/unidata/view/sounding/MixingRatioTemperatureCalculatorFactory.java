/*
 * $Id: MixingRatioTemperatureCalculatorFactory.java,v 1.11 2005/05/13 18:33:34 jeffmc Exp $
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
 * Provides support for obtaining temperature calculators for a saturation
 * mixing-ratio.
 *
 * @author Steven R. Emmerson
 * @version $Id: MixingRatioTemperatureCalculatorFactory.java,v 1.11 2005/05/13 18:33:34 jeffmc Exp $
 */
public class MixingRatioTemperatureCalculatorFactory
        implements TemperatureCalculatorFactory {

    /**
     * The single instance of this class.
     */
    private static MixingRatioTemperatureCalculatorFactory instance;

    static {
        try {
            instance = new MixingRatioTemperatureCalculatorFactory();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs from nothing.
     */
    private MixingRatioTemperatureCalculatorFactory() {}

    /**
     * Returns an instance of this class.
     * @return                  An instance of this class.
     */
    public static TemperatureCalculatorFactory instance() {
        return instance;
    }

    /**
     * Returns a temperature calculator for the saturation mixing-ratio.
     *
     * @param saturationPressure        The saturation pressure.
     * @param saturationTemperature     The saturation temperature.
     * @return                          A temperature calculator.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public TemperatureCalculator newTemperatureCalculator(Real saturationPressure, Real saturationTemperature)
            throws VisADException, RemoteException {
        return new MixingRatioTemperatureCalculator(saturationPressure,
                                                    saturationTemperature);
    }
}







