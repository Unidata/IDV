/*
 * $Id: PseudoAdiabaticTrajectory.java,v 1.18 2005/05/13 18:33:35 jeffmc Exp $
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
 * Provides support for the pseudo-adiabatic trajectory of a lifted parcel of
 * air.
 *
 * Instances are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: PseudoAdiabaticTrajectory.java,v 1.18 2005/05/13 18:33:35 jeffmc Exp $
 */
public class PseudoAdiabaticTrajectory {

    /**
     * The trajectory of the lifted parcel.
     */
    private Gridded2DSet trajectory;

    /**
     * The ratio between pressures.
     */
    private static Real pressureRatio;

    /**
     * The logarithm of the pressure ratio.
     */
    private static Real logPressureRatio;

    static {
        try {
            pressureRatio = new Real(
                RealType.getRealType(
                    "PseudoAdiabaticTrajectoryPressureRatio",
                    CommonUnit.dimensionless, (Set) null), 0.95);
            logPressureRatio = (Real) pressureRatio.log();
        } catch (Exception e) {
            System.err.print("Couldn't initialize class: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Constructs from a starting pressure, temperature, and dew-point, and an
     * ending pressure.
     * @param startingPressure          The starting pressure.
     * @param startingTemperature       The starting temperature.
     * @param startingDewPoint          The starting dew-point.
     * @param endingPressure            The ending pressure.
     * @return                          An instance of this class.
     * @throws TypeException    An argument has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static PseudoAdiabaticTrajectory instance(Real startingPressure, Real startingTemperature, Real startingDewPoint, Real endingPressure)
            throws TypeException, VisADException, RemoteException {

        return instance(startingPressure, startingTemperature,
                        startingDewPoint, endingPressure,
                        DefaultWetTemperatureCalculatorFactory.instance());
    }

    /**
     * Returns an instance of this class based on a starting pressure,
     * temperature, and dew-point, an ending pressure, and temperature
     * calculator factory.
     * @param startingPressure          The starting pressure.
     * @param startingTemperature       The starting temperature.
     * @param startingDewPoint          The starting dew-point.
     * @param endingPressure            The ending pressure.
     * @param factory                   The factory for creating a temperature
     *                                  calculator.
     * @return                          An instance of this class.
     * @throws TypeException    An argument has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static PseudoAdiabaticTrajectory instance(Real startingPressure, Real startingTemperature, Real startingDewPoint, Real endingPressure, TemperatureCalculatorFactory factory)
            throws TypeException, VisADException, RemoteException {

        return new PseudoAdiabaticTrajectory(startingPressure,
                                             startingTemperature,
                                             startingDewPoint,
                                             endingPressure, factory);
    }

    /**
     * Returns the type of the pressure quantity.
     * @return                  The type of the pressure quantity.
     * @throws VisADException   VisAD failure.
     */
    public RealType getPressureRealType() throws VisADException {
        return (RealType) ((SetType) trajectory.getType()).getDomain()
            .getComponent(0);
    }

    /**
     * Returns the type of the temperature quantity.
     * @return                  The type of the temperature quantity.
     * @throws VisADException   VisAD failure.
     */
    public RealType getTemperatureRealType() throws VisADException {
        return (RealType) ((SetType) trajectory.getType()).getDomain()
            .getComponent(1);
    }

    /**
     * Returns the domain of the parcel's trajectory.
     * @return          The domain of the parcel's trajectory.
     */
    public SampledSet getSampledSet() {
        return trajectory;
    }

    /**
     * Returns the string version of this instance.
     * @return          The string version of this instance.
     */
    public String toString() {

        String string;

        try {
            string = trajectory.longString();
        } catch (VisADException e) {
            string = trajectory.toString();
        }

        return string;
    }

    /**
     * Constructs from a starting pressure, temperature, and dew-point, an
     * ending pressure, and temperature calculator factory.
     * @param startingPressure          The starting pressure.
     * @param startingTemperature       The starting temperature.
     * @param startingDewPoint          The starting dew-point.
     * @param endingPressure            The ending pressure.
     * @param factory                   The factory for creating a temperature
     *                                  calculator.
     * @throws TypeException    An argument has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected PseudoAdiabaticTrajectory(Real startingPressure, Real startingTemperature, Real startingDewPoint, Real endingPressure, TemperatureCalculatorFactory factory)
            throws TypeException, VisADException, RemoteException {

        RealType pressureType    = Pressure.getRealType();
        RealType temperatureType = Temperature.getRealType();

        Util.vetType(pressureType, startingPressure);
        Util.vetType(temperatureType, startingTemperature);
        Util.vetType(temperatureType, startingDewPoint);
        Util.vetType(pressureType, endingPressure);

        Real saturationPointTemperature =
            (Real) SaturationPointTemperature.create(startingPressure,
                startingTemperature,
                WaterVaporMixingRatio.create(startingPressure,
                                             startingDewPoint));
        Real saturationPointPressure =
            (Real) SaturationPointPressure.create(startingPressure,
                                                  startingTemperature,
                                                  saturationPointTemperature);
        Unit pressureUnit = AirPressure.getRealType().getDefaultUnit();

        if (saturationPointPressure.getValue(pressureUnit)
                > startingPressure.getValue(pressureUnit)) {
            startingPressure    = saturationPointPressure;
            startingTemperature = saturationPointTemperature;
        }

        double startPressure = startingPressure.getValue(pressureUnit);
        int dryCount = 1 + (int) ((Real) saturationPointPressure.divide(
                           startingPressure).log().divide(
                           logPressureRatio)).getValue(
                               CommonUnit.dimensionless);
        int wetCount = 1 + (int) ((Real) endingPressure.divide(
                           saturationPointPressure).log().divide(
                           logPressureRatio)).getValue(
                               CommonUnit.dimensionless);
        float[][] samples = new float[2][dryCount + wetCount];
        Real potentialTemperature =
            (Real) PotentialTemperature.create(startingPressure,
                                               startingTemperature);
        Real pressure        = startingPressure;
        Real temperature     = startingTemperature;
        Unit temperatureUnit = AirTemperature.getRealType().getDefaultUnit();

        for (int i = 0; i < dryCount; ++i) {
            samples[0][i] = (float) pressure.getValue(pressureUnit);
            samples[1][i] = (float) temperature.getValue(temperatureUnit);
            pressure      = (Real) pressure.multiply(pressureRatio);
            temperature =
                (Real) PotentialTemperature.createAirTemperature(pressure,
                    potentialTemperature);
        }

        Real saturationEquivalentPotentialTemperature =
            (Real) SaturationEquivalentPotentialTemperature.create(
                saturationPointPressure, saturationPointTemperature);
        TemperatureCalculator calculator =
            factory.newTemperatureCalculator(saturationPointPressure,
                                             saturationPointTemperature);

        pressure    = saturationPointPressure;
        temperature = saturationPointTemperature;

        for (int i = dryCount; i < dryCount + wetCount; ++i) {
            samples[0][i] = (float) pressure.getValue(pressureUnit);
            samples[1][i] = (float) temperature.getValue(temperatureUnit);

            Real nextPressure    = (Real) pressure.multiply(pressureRatio);
            Real nextTemperature = calculator.nextTemperature(nextPressure);

            pressure    = nextPressure;
            temperature = nextTemperature;
        }

        trajectory = new Gridded2DSet(new RealTupleType(AirPressure
            .getRealType(), AirTemperature.getRealType()), samples, samples[0]
                .length, (CoordinateSystem) null, new Unit[]{ pressureUnit,
                                                              temperatureUnit }, (ErrorEstimate[]) null);
    }
}







