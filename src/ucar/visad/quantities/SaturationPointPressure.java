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
 * Provides support for the quantity of saturation-point pressure.  Saturation-
 * point pressure is the pressure at which a parcel of air lifted dry-
 * adiabatically will become saturated.</p>
 *
 * <p>An instance of this class is immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: SaturationPointPressure.java,v 1.11 2005/05/13 18:35:43 jeffmc Exp $
 */
public final class SaturationPointPressure extends AirPressure {

    /**
     * The single instance.
     */
    private static SaturationPointPressure INSTANCE;

    /**
     * The following values are taken from "An Introduction to Boundary Layer
     * Meteorology" by Roland B. Stull; chapter 13 (Boundary Layer Clouds).
     */
    private static Real exponent;

    /**
     * Constructs an instance.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private SaturationPointPressure() throws VisADException {
        super("SaturationPointPressure");
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
            synchronized (SaturationPointPressure.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SaturationPointPressure();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Returns the exponent associated with this quantity.
     *
     * @return             The exponent associated with this quantity.
     * @throws VisADException if a core VisAD failure occurs.
     */
    protected static Real getExponent() throws VisADException {

        if (exponent == null) {
            synchronized (SaturationPointPressure.class) {
                if (exponent == null) {
                    exponent = new Real(
                        RealType.getRealType(
                            "SaturationPointTemperatureExponent",
                            CommonUnit.dimensionless, (Set) null), 3.5);
                }
            }
        }

        return exponent;
    }

    /**
     * Creates a saturation-point pressure data object from data objects for
     * pressure, temperature, and saturation-point temperature.  The empirical
     * formula is taken from "An Introduction to Boundary Layer Meteorology" by
     * Roland B. Stull; chapter 13 (Boundary Layer Clouds).
     *
     * @param pressure          The air pressure data object.
     * @param temperature       The temperature data object.
     * @param saturationPointTemperature
     *                          The saturation-point temperature data object.
     * @return                  The saturation-point pressure data object
     *                          corresponding to the input arguments.
     * @throws TypeException    An input argument has wrong type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature,
                              Data saturationPointTemperature)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(AirPressure.getRealType(), pressure);
        Util.vetType(AirTemperature.getRealType(), temperature);
        Util.vetType(SaturationPointTemperature.getRealType(),
                     saturationPointTemperature);

        return Util.clone(
            VisADMath.multiply(
                pressure,
                VisADMath.pow(
                    VisADMath.divide(
                        saturationPointTemperature,
                        temperature), getExponent())), getRealType());
    }
}
