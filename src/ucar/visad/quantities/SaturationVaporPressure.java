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

import visad.CommonUnit;
import visad.Data;
import visad.DerivedUnit;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.SI;
import visad.Set;
import visad.TypeException;
import visad.UnimplementedException;
import visad.Unit;
import visad.UnitException;
import visad.VisADException;

import java.rmi.RemoteException;


/**
 * Provides support for the quantity saturation water vapor pressure.
 *
 * Instances are immutable.
 *
 * @author Steven R. Emmerson
 */
public class SaturationVaporPressure extends Pressure {

    /**
     * The dimensionless quantity two.
     */
    protected static Real two;

    /**
     * The single instance.
     */
    private static SaturationVaporPressure INSTANCE;

    /**
     * The reference saturation vapor pressure.
     */
    private static Real eSat0;

    /**
     * The mutiplier of the temperature interval ratios.
     */
    private static Real temperatureMultiplier;

    /**
     * The first temperature constant.
     */
    private static Real temperatureConstant1;

    /**
     * The second temperature constant.
     */
    private static Real temperatureConstant2;

    /**
     * The difference of the above, two, temperature constants.
     */
    private static Real temperatureConstantDifference;

    /**
     * The product of two constants.
     */
    private static Real constantProduct;

    /**
     * The RealType of the derivative w.r.t. Temperature.
     */
    private static RealType temperatureDerivativeRealType;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private SaturationVaporPressure() throws VisADException {
        this("SaturationVaporPressure");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for this quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected SaturationVaporPressure(String name) throws VisADException {
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
            synchronized (SaturationVaporPressure.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SaturationVaporPressure();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Obtains the RealType of the derivative with respect to temperature.
     *
     * @return                  The RealType of the derivative w.r.t.
     *                          temperature.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealType getTemperatureDerivativeRealType()
            throws VisADException {

        if (temperatureDerivativeRealType == null) {
            synchronized (SaturationVaporPressure.class) {
                if (temperatureDerivativeRealType == null) {
                    temperatureDerivativeRealType = RealType.getRealType(
                        "TemperatureDerivativeOfSaturationVaporPressure",
                        getRealType().getDefaultUnit().divide(SI.kelvin),
                        (Set) null);
                }
            }
        }

        return temperatureDerivativeRealType;
    }

    /**
     * Get the constant product
     * @return  the constant product value
     *
     * @throws VisADException  couldn't create a VisAD object
     */
    private static Real getConstantProduct() throws VisADException {

        if (constantProduct == null) {
            synchronized (SaturationVaporPressure.class) {
                if (constantProduct == null) {
                    try {
                        constantProduct =
                            (Real) getTemperatureMultiplier().multiply(
                                getTemperatureConstant1());
                    } catch (RemoteException e) {}  // can't happen because above data is local
                }
            }
        }

        return constantProduct;
    }

    /**
     * Get the temperature constant
     * @return  the temperature constant
     *
     * @throws VisADException  couldn't create a VisAD object
     */
    private static Real getTemperatureConstant2() throws VisADException {

        if (temperatureConstant2 == null) {
            synchronized (SaturationVaporPressure.class) {
                if (temperatureConstant2 == null) {
                    temperatureConstant2 =
                        new Real(Temperature.getRealType(), 29.66, SI.kelvin);
                }
            }
        }

        return temperatureConstant2;
    }

    /**
     * Get the temperature multiplier
     * @return  the multiplier
     *
     * @throws VisADException  couldn't create a VisAD object
     */
    private static Real getTemperatureMultiplier() throws VisADException {

        if (temperatureMultiplier == null) {
            synchronized (SaturationVaporPressure.class) {
                if (temperatureMultiplier == null) {
                    temperatureMultiplier = new Real(
                        RealType.getRealType(
                            "SaturationVaporPressureTemperatureMultiplier",
                            CommonUnit.dimensionless, (Set) null), 17.67);
                }
            }
        }

        return temperatureMultiplier;
    }

    /**
     * Get the saturation vapor pressure
     * @return eSat
     *
     * @throws VisADException  couldn't create a VisAD object
     */
    private static Real getESat0() throws VisADException {

        if (eSat0 == null) {
            synchronized (SaturationVaporPressure.class) {
                if (eSat0 == null) {
                    eSat0 = new Real(getRealType(), 611.2,
                                     CommonUnits.PASCAL);
                }
            }
        }

        return eSat0;
    }

    /**
     * Get the temperature constant
     * @return  the temperature constant
     *
     * @throws VisADException  couldn't create a VisAD object
     */
    private static Real getTemperatureConstant1() throws VisADException {

        if (temperatureConstant1 == null) {
            synchronized (SaturationVaporPressure.class) {
                if (temperatureConstant1 == null) {
                    temperatureConstant1 =
                        new Real(Temperature.getRealType(), 273.16,
                                 SI.kelvin);
                }
            }
        }

        return temperatureConstant1;
    }

    /**
     * Get the temperature constant difference
     * @return  the temperature constant
     *
     * @throws VisADException  couldn't create a VisAD object
     */
    private static Real getTemperatureConstantDifference()
            throws VisADException {

        if (temperatureConstantDifference == null) {
            synchronized (SaturationVaporPressure.class) {
                try {
                    if (temperatureConstantDifference == null) {
                        temperatureConstantDifference =
                            (Real) VisADMath.subtract(
                                getTemperatureConstant1(),
                                getTemperatureConstant2());
                    }
                } catch (RemoteException e) {}  // can't happen because above are local
            }
        }

        return temperatureConstant1;
    }

    /**
     * Get a value of 2 with the saturation vapor pressure type
     * @return  the value
     *
     * @throws VisADException  couldn't create a VisAD object
     */
    private static Real getTwo() throws VisADException {

        if (two == null) {
            synchronized (SaturationVaporPressure.class) {
                if (two == null) {
                    two = new Real(
                        RealType.getRealType(
                            "SaturationVaporPressureTwo",
                            CommonUnit.dimensionless, (Set) null), 2.0);
                }
            }
        }

        return two;
    }

    /**
     * Creates a SaturationVaporPressure data object from a temperature
     * data object.  The algorithm is based on Bolton's 1980 variation of
     * Teten's 1930 formula for saturation vapor pressure (see "An Introduction
     * to Boundary Layer Meteorology" by Roland B. Stull (1988) equation
     * 7.5.2d).
     *
     * @param temperature       The temperature data object.
     * @return                  Saturation water vapor pressure data object
     *                          computed from the temperature.  The type of the
     *                          object will be that of the arguments after
     *                          standard promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    Argument has wrong type.
     * @throws UnitException    Inappropriate unit argument.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data temperature)
            throws TypeException, UnitException, UnimplementedException,
                   VisADException, RemoteException {

        Util.vetType(Temperature.getRealType(), temperature);

        return Util.clone(
            VisADMath.multiply(
                getESat0(),
                VisADMath.exp(
                    VisADMath.multiply(
                        getTemperatureMultiplier(),
                        VisADMath.divide(
                            VisADMath.subtract(
                                temperature,
                                getTemperatureConstant1()), VisADMath.subtract(
                                    temperature,
                                    getTemperatureConstant2()))))), getRealType());
    }

    /**
     * Returns the derivative of saturation vapor pressure with respect to
     * temperature.
     *
     * @param temperature      The temperatures at which to return the
     *                         derivatives.
     * @return                 The derivative w.r.t. temperature at the given
     *                         temperatures.
     * @throws TypeException   if a necessary RealType can't be created.
     * @throws UnitException   if unit-convertion failure occurs.
     * @throws UnimplementedException
     *                         if a necessary method is unimplemented.
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public static Data temperatureDerivative(Data temperature)
            throws TypeException, UnitException, UnimplementedException,
                   VisADException, RemoteException {

        Real tempDiff = (Real) VisADMath.subtract(temperature,
                            getTemperatureConstant2());

        return Util
            .clone(VisADMath
                .multiply(VisADMath
                    .multiply(getTemperatureMultiplier(),
                        create(temperature)), VisADMath
                            .divide(getTemperatureConstantDifference(),
                                VisADMath
                                    .multiply(tempDiff,
                                        tempDiff))), getTemperatureDerivativeRealType());
    }

    /**
     * Creates a temperature data object from a saturation water vapor pressure
     * data object.  The algorithm is based on Bolton's 1980 variation of
     * Teten's 1930 formula for saturation vapor pressure (see "An Introduction
     * to Boundary Layer Meteorology" by Roland B. Stull (1988) equation
     * 7.5.2d).
     *
     * @param eSat              Saturation water vapor pressure data object.
     * @param outputType        the RealType of the values
     * @return                  Temperature data object computed from saturation
     *                          water vapor pressures data object.  The type of
     *                          the object will be that of the arguments after
     *                          standard promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    Argument has wrong type.
     * @throws UnitException    Inappropriate unit argument.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createTemperature(Data eSat, RealType outputType)
            throws TypeException, UnitException, VisADException,
                   RemoteException {

        Util.vetType(getRealType(), eSat);
        if (outputType == null) {
            outputType = AirTemperature.getRealType();
        }

        Data log = VisADMath.log(VisADMath.divide(eSat, getESat0()));

        return Util.clone(
            VisADMath.divide(
                VisADMath.subtract(
                    VisADMath.multiply(getTemperatureConstant2(), log),
                    getConstantProduct()), VisADMath.subtract(
                        log, getTemperatureMultiplier())), outputType);
    }

    /**
     * Creates a temperature data object from a saturation water vapor pressure
     * data object.  The algorithm is based on Bolton's 1980 variation of
     * Teten's 1930 formula for saturation vapor pressure (see "An Introduction
     * to Boundary Layer Meteorology" by Roland B. Stull (1988) equation
     * 7.5.2d).
     *
     * @param eSat              Saturation water vapor pressure data object.
     * @return                  Temperature data object computed from saturation
     *                          water vapor pressures data object.  The type of
     *                          the object will be that of the arguments after
     *                          standard promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    Argument has wrong type.
     * @throws UnitException    Inappropriate unit argument.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createTemperature(Data eSat)
            throws TypeException, UnitException, VisADException,
                   RemoteException {
        return createTemperature(eSat, null);
    }

}
