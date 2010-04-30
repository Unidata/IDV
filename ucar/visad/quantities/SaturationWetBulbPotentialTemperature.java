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

import visad.Real;

import visad.RealTupleType;

import visad.RealType;

import visad.SI;

import visad.Set;

import visad.TypeException;

import visad.UnimplementedException;

import visad.VisADException;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of saturation wet-bulb potential
 * temperature.  Contours of this quantity are "saturation adiabats" (alias
 * "moist adiabats" or "wet adiabats").</p>
 *
 * <p>An instance of this class is immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: SaturationWetBulbPotentialTemperature.java,v 1.10 2005/05/13 18:35:44 jeffmc Exp $
 */
public final class SaturationWetBulbPotentialTemperature extends PotentialTemperature {

    /**
     * The single instance.
     */
    private static SaturationWetBulbPotentialTemperature INSTANCE;

    /**
     * The dimensionless quantity one.
     */
    protected static Real one;

    /**
     * The coefficient in the equation.
     */
    protected static Real coefficient;

    /**
     * Constructs an instance.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private SaturationWetBulbPotentialTemperature() throws VisADException {
        this("SaturationWetBulbPotentialTemperature");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for this quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected SaturationWetBulbPotentialTemperature(String name)
            throws VisADException {
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
            synchronized (SaturationWetBulbPotentialTemperature.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SaturationWetBulbPotentialTemperature();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Creates a saturation wet-bulb potential temperature data object.
     *
     * @param pressure          The air pressure data object.
     * @param temperature       The temperature data object.
     * @return                  The saturation wet-bulb potential temperature
     *                          data object corresponding to the input
     *                          arguments.
     * @throws TypeException    An input argument has wrong type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature)
            throws TypeException, VisADException, RemoteException {

        if (one == null) {
            synchronized (SaturationWetBulbPotentialTemperature.class) {
                if (one == null) {
                    one = new Real(
                        RealType.getRealType(
                            "SaturationWetBulbPotentialTemperatureOne",
                            CommonUnit.dimensionless, (Set) null), 1.0);
                    coefficient = new Real(
                        RealType.getRealType(
                            "SaturationWetBulbPotentialTemperatureCoefficient",
                            SI.kelvin, (Set) null), 2500);
                }
            }
        }

        return Util.clone(
            VisADMath.multiply(
                PotentialTemperature.create(pressure, temperature),
                VisADMath.add(
                    one,
                    VisADMath.multiply(
                        coefficient,
                        VisADMath.divide(
                            SaturationMixingRatio.create(
                                pressure,
                                temperature), temperature)))), getRealType());
    }
}
