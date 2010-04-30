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


import ucar.visad.*;

import visad.*;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of saturation-point temperature.
 * Saturation-point temperature is the temperature at which a parcel of air
 * lifted dry-adiabatically will become saturated.</p>
 *
 * <p>An instance of this class is immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: SaturationPointTemperature.java,v 1.10 2005/05/13 18:35:43 jeffmc Exp $
 */
public final class SaturationPointTemperature extends AirTemperature {

    /**
     * The single instance.
     */
    private static SaturationPointTemperature INSTANCE;

    /**
     * The following values are taken from "An Introduction to Boundary Layer
     * Meteorology" by Roland B. Stull; chapter 13 (Boundary Layer Clouds).
     */
    private static Real numerator;

    /** Real for the log of T */
    private static Real logTCoeff;

    /** a real value for temperature */
    private static Real t0;

    /** a real value for pressure */
    private static Real p0;

    /** real constant for the denomenator */
    private static Real denomConst;

    /** a real constant for temperature */
    private static Real tempConst;

    /**
     * Constructs an instance.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private SaturationPointTemperature() throws VisADException {
        super("SaturationPointTemperature");
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
            synchronized (SaturationPointTemperature.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SaturationPointTemperature();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Returns the numerator associated with this quantity.
     *
     * @return                The numerator associated with this quantity.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static Real getNumerator() throws VisADException {

        if (numerator == null) {
            synchronized (SaturationPointTemperature.class) {
                if (numerator == null) {
                    numerator = new Real(
                        RealType.getRealType(
                            "SaturationPointTemperatureNumerator", SI.kelvin,
                            (Set) null), 2840);
                }
            }
        }

        return numerator;
    }

    /**
     * Returns the coefficient of the log(temperature) term for this quantity.
     *
     * @return                The coefficient.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static Real getLogTCoeff() throws VisADException {

        if (logTCoeff == null) {
            synchronized (SaturationPointTemperature.class) {
                if (logTCoeff == null) {
                    logTCoeff = new Real(
                        RealType.getRealType(
                            "SaturationPointTemperatureLogTCoef",
                            CommonUnit.dimensionless, (Set) null), 3.5);
                }
            }
        }

        return logTCoeff;
    }

    /**
     * Returns the reference pressure for this quantity.
     *
     * @return           The reference pressure.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static Real getP0() throws VisADException {

        if (p0 == null) {
            synchronized (SaturationPointTemperature.class) {
                if (p0 == null) {
                    p0 = new Real(
                        RealType.getRealType(
                            "SaturationPointTemperatureP0",
                            CommonUnits.PASCAL, (Set) null), 1000);
                }
            }
        }

        return p0;
    }

    /**
     * Returns the reference temperature for this quantity.
     *
     * @return           The reference temperature.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static Real getT0() throws VisADException {

        if (t0 == null) {
            synchronized (SaturationPointTemperature.class) {
                if (t0 == null) {
                    t0 = new Real(
                        RealType.getRealType(
                            "SaturationPointTemperatureT0", SI.kelvin,
                            (Set) null), 1);
                }
            }
        }

        return t0;
    }

    /**
     * Returns the constant in the denominator.
     *
     * @return           The constant in the denominator.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static Real getDenomConst() throws VisADException {

        if (denomConst == null) {
            synchronized (SaturationPointTemperature.class) {
                if (denomConst == null) {
                    denomConst = new Real(
                        RealType.getRealType(
                            "SaturationPointTemperatureDenomConst",
                            CommonUnit.dimensionless, (Set) null), 7.108);
                }
            }
        }

        return denomConst;
    }

    /**
     * Returns the temperature constant.
     *
     * @return           The temperature constant.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static Real getTempConst() throws VisADException {

        if (tempConst == null) {
            synchronized (SaturationPointTemperature.class) {
                if (tempConst == null) {
                    tempConst = new Real(
                        RealType.getRealType(
                            "SaturationPointTemperatureTempConst", SI.kelvin,
                            (Set) null), 55);
                }
            }
        }

        return tempConst;
    }

    /**
     * Creates a saturation-point temperature data object from data objects
     * for pressure, temperature, and water-vapor mixing-ratio.  The empirical
     * formula is taken from "An Introduction to Boundary Layer Meteorology" by
     * Roland B. Stull; chapter 13 (Boundary Layer Clouds).
     *
     * @param pressure          The air pressure data object.
     * @param temperature       The temperature data object.
     * @param mixingRatio       The water-vapor mixing-ratio data object.
     * @return                  The saturation-point temperature data object
     *                          corresponding to the input arguments.
     * @throws TypeException    An input argument has wrong type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature,
                              Data mixingRatio)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(AirPressure.getRealType(), pressure);
        Util.vetType(AirTemperature.getRealType(), temperature);
        Util.vetType(WaterVaporMixingRatio.getRealType(), mixingRatio);

        return Util.clone(VisADMath.add(VisADMath.divide(getNumerator(),
                VisADMath.subtract(VisADMath.subtract(VisADMath.multiply(getLogTCoeff(),
                    VisADMath.log(VisADMath.divide(temperature,
                        getT0()))), VisADMath.log(VisADMath.divide(VisADMath.multiply(mixingRatio,
                            VisADMath.divide(pressure,
                                getP0())), VisADMath.add(mixingRatio,
                                    WaterVaporMixingRatio.getGasConstantRatio())))), getDenomConst())), getTempConst()), getRealType());
    }
}
