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

import visad.Unit;

import visad.UnitException;

import visad.VisADException;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of atmospheric virtual temperature.
 *
 * @author Steven R. Emmerson
 * @version $Id: VirtualTemperature.java,v 1.13 2005/05/13 18:35:45 jeffmc Exp $
 */
public class VirtualTemperature extends AirTemperature {

    /**
     * The single instance.
     */
    private static VirtualTemperature INSTANCE;

    /**
     * First constant in the equation for virtual temperature.
     */
    private static Real constantA;

    /**
     * Second constant in the equation for virtual temperature.
     */
    private static Real constantB;

    /**
     * Constructs an instance.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private VirtualTemperature() throws VisADException {
        this("VirtualTemperature");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for this quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected VirtualTemperature(String name) throws VisADException {
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
            synchronized (VirtualTemperature.class) {
                if (INSTANCE == null) {
                    INSTANCE = new VirtualTemperature();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Creates a virtual temperature data object from data objects of
     * air pressure, in situ air temperature, and dew point.
     *
     * @param pressure          The air pressure data object.
     * @param temperature       The in situ air temperature data object.
     * @param dewPoint          The dew point data object.
     * @return                  The virtual air temperature corresponding to
     *                          the arguments.  The type of the returned object
     *                          will be that of the arguments after standard
     *                          promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has the wrong type.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createFromDewPoint(Data pressure, Data temperature,
                                          Data dewPoint)
            throws TypeException, UnimplementedException, VisADException,
                   RemoteException {

        return createFromMixingRatio(temperature,
                                     WaterVaporMixingRatio.create(pressure,
                                         dewPoint));
    }

    /**
     * Creates a virtual temperature data object from data objects of
     * in situ air temperature, and water vapor mixing-ratio.
     *
     * @param temperature       The in situ air temperature data object.
     * @param mixingRatio       The water vapor mixing-ratio data object.
     * @return                  The virtual air temperature corresponding to
     *                          the arguments.  The type of the returned object
     *                          will be that of the arguments after standard
     *                          promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has the wrong type.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createFromMixingRatio(Data temperature,
                                             Data mixingRatio)
            throws TypeException, UnimplementedException, VisADException,
                   RemoteException {

        Util.vetType(Temperature.getRealType(), temperature);
        Util.vetType(WaterVaporMixingRatio.getRealType(), mixingRatio);

        if (constantA == null) {
            synchronized (VirtualTemperature.class) {
                if (constantA == null) {
                    constantA = new Real(
                        RealType.getRealType(
                            "VirtualTemperatureConstantA",
                            CommonUnit.dimensionless, (Set) null), 1.0);
                    constantB = new Real(
                        RealType.getRealType(
                            "VirtualTemperatureConstantB",
                            CommonUnit.dimensionless, (Set) null), 0.61);
                }
            }
        }

        return Util.clone(VisADMath.multiply(temperature,
                                             VisADMath.add(constantA,  // 1.0
                VisADMath.multiply(constantB,  // 0.61
                                   mixingRatio))), getRealType());
    }
}
