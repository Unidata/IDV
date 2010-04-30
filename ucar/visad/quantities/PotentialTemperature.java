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
 * Provides support for the quantity of potential temperature.</p>
 *
 * <p>An instance of this class is immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: PotentialTemperature.java,v 1.13 2005/05/13 18:35:41 jeffmc Exp $
 */
public class PotentialTemperature extends AirTemperature {

    /**
     * The single instance.
     */
    private static final PotentialTemperature INSTANCE;

    /**
     * The exponent in the potential temperature equation.  The value
     * is take from "An Introduction to Boundary Layer Meteorology" by
     * Roland B. Stull; chapter 13 (Boundary Layer Clouds).
     */
    private static final Real exponent;

    /**
     * The reference pressure.
     */
    private static final Real referencePressure;

    static {
        try {
            exponent = (Real) VisADMath.divide(DryAirGasConstant.newReal(),
                    SpecificHeatCapacityOfDryAirAtConstantPressure.newReal());
            referencePressure = new Real(AirPressure.getRealType(), 1000,
                                         CommonUnits.MILLIBAR);
            INSTANCE = new PotentialTemperature();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Constructs an instance.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private PotentialTemperature() throws VisADException {
        this("PotentialTemperature");
    }

    /**
     * Constructs an instance from a name.
     *
     * @param name              The name for the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected PotentialTemperature(String name) throws VisADException {
        super(name, SI.kelvin);
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

        return INSTANCE.realTupleType();
    }

    /**
     * Creates a potential temperature data object from data objects for
     * pressure and air temperature.
     *
     * @param pressure          The air pressure data object.
     * @param temperature       The temperature data object.
     * @return                  The potential temperature data object
     *                          corresponding to the input arguments.
     * @throws TypeException    An input argument has wrong type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(AirPressure.getRealType(), pressure);
        Util.vetType(AirTemperature.getRealType(), temperature);

        return Util.clone(VisADMath.multiply(temperature, factor(pressure)),
                          getRealType());
    }

    /**
     * Compute the factor for use in the potential temperature equation.
     *
     * @param pressure          The air pressure data object.
     * @return                  The factor for use in the potential temperature
     *                          equation corresponding to the input air
     *                          pressure.
     * @throws TypeException    Input argument has wrong type.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    protected static final Data factor(Data pressure)
            throws TypeException, UnimplementedException, VisADException,
                   RemoteException {

        return VisADMath.pow(VisADMath.divide(referencePressure, pressure),
                             exponent);
    }

    /**
     * Creates an air temperature data object from data objects for pressure and
     * potential temperature.
     *
     * @param pressure          The air pressure data object.
     * @param theta             The potential temperature data object.
     * @return                  The air temperature data object
     *                          corresponding to the input arguments.
     * @throws TypeException    An input argument has wrong type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createAirTemperature(Data pressure, Data theta)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(AirPressure.getRealType(), pressure);
        Util.vetType(getRealType(), theta);

        return Util.clone(VisADMath.divide(theta, factor(pressure)),
                          AirTemperature.getRealType());
    }
}
