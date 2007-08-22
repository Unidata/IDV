/*
 * $Id: DryTemperatureCalculatorFactory.java,v 1.12 2005/05/13 18:33:29 jeffmc Exp $
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
 * Provides support for obtaining calculators for the dry portion of a lifted
 * parcel's adiabatic trajectory.
 *
 * @author Steven R. Emmerson
 * @version $Id: DryTemperatureCalculatorFactory.java,v 1.12 2005/05/13 18:33:29 jeffmc Exp $
 */
public class DryTemperatureCalculatorFactory
        implements TemperatureCalculatorFactory {

    /**
     * The single instance of this class.
     */
    private static DryTemperatureCalculatorFactory instance;

    static {
        try {
            instance = new DryTemperatureCalculatorFactory();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs from nothing.
     */
    private DryTemperatureCalculatorFactory() {}

    /**
     * Returns an instance of this class.
     * @return                  An instance of this class.
     */
    public static TemperatureCalculatorFactory instance() {
        return instance;
    }

    /**
     * Returns a caculator for the dry portion of a parcel's pseudo-adiabatic
     * trajectory given the starting pressure and temperature.
     * @param startPressure     The starting pressure.
     * @param startTemperature  The starting temperature.
     * @return                  A temperature calculator.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public TemperatureCalculator newTemperatureCalculator(Real startPressure, Real startTemperature)
            throws VisADException, RemoteException {
        return new DryTemperatureCalculator(startPressure, startTemperature);
    }
}







