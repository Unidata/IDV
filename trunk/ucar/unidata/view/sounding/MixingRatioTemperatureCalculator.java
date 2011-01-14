/*
 * $Id: MixingRatioTemperatureCalculator.java,v 1.10 2005/05/13 18:33:33 jeffmc Exp $
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
 * Provides support for calculating temperatures along a saturation
 * mixing-ratio.
 *
 * @author Steven R. Emmerson
 * @version $Id: MixingRatioTemperatureCalculator.java,v 1.10 2005/05/13 18:33:33 jeffmc Exp $
 */
public class MixingRatioTemperatureCalculator
        implements TemperatureCalculator {

    /**
     * The saturation mixing-ratio of the trajectory.
     */
    private final Real saturationMixingRatio;

    /**
     * Constructs from a saturation pressure and temperature.
     * @param saturationPressure        The saturation pressure.
     * @param saturationTemperature     The saturation temperature.
     * @throws TypeException    Something has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public MixingRatioTemperatureCalculator(Real saturationPressure, Real saturationTemperature)
            throws TypeException, VisADException, RemoteException {

        saturationMixingRatio =
            (Real) SaturationMixingRatio.create(saturationPressure,
                                                saturationTemperature);
    }

    /**
     * Returns the next temperature associated with the next pressure.
     * @param nextPressure      The next pressure.
     * @return                  The next temperature.
     * @throws UnitException    Invalid argument unit.
     * @throws TypeException    Invalid argument type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Real nextTemperature(Real nextPressure)
            throws TypeException, UnitException, VisADException,
                   RemoteException {
        return (Real) SaturationMixingRatio.createTemperature(nextPressure,
                saturationMixingRatio);
    }
}







