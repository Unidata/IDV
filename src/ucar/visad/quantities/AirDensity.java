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

import visad.CoordinateSystem;

import visad.Data;

import visad.FlatField;

import visad.FunctionType;

import visad.RealTupleType;

import visad.RealType;

import visad.SI;

import visad.Set;

import visad.TypeException;

import visad.UnimplementedException;

import visad.VisADException;

import visad.util.DataUtility;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of atmospheric density.
 *
 * @author Steven R. Emmerson
 * @version $Id: AirDensity.java,v 1.11 2005/05/13 18:35:37 jeffmc Exp $
 */
public final class AirDensity extends Density {

    /**
     * The single instance.
     */
    private static AirDensity INSTANCE;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private AirDensity() throws VisADException {
        super("AirDensity",
              SI.kilogram.scale(.001).divide(SI.meter.pow(3)).clone("g/m3"));
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
            synchronized (AirDensity.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AirDensity();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Creates an AirDensity data object from data objects for Pressure,
     * InSituAirTemperature, and DewPoint.
     * @param pressure          A Pressure data object.
     * @param inSituAirTemperature
     *                          An InSituAirTemperature data object.
     * @param dewPoint          A DewPoint data object.
     * @return                  The AirDensity data object computed from the
     *                          arguments.  The type of the object will be that
     *                          of the arguments after standard promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has wrong type.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data inSituAirTemperature,
                              Data dewPoint)
            throws TypeException, UnimplementedException, VisADException,
                   RemoteException {

        /*
         * The arguments are unchecked because VirtualTemperature.create(...)
         * will do that.
         */
        return create(pressure,
                      VirtualTemperature.createFromDewPoint(pressure,
                          inSituAirTemperature, dewPoint));
    }

    /**
     * Creates an AirDensity data object from data objects for Pressure,
     * and temperature
     * @param pressure          A Pressure data object.
     * @param temperature       A temperature data object.  If the in-situ
     *                          temperature is used, then the returned density
     *                          will be that for dry air; if the virtual
     *                          temperature is used, then the returned density
     *                          will be the actual density.
     * @return                  The AirDensity data object computed from the
     *                          arguments.  The type of the object will be that
     *                          of the arguments after standard promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has wrong type.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature)
            throws TypeException, UnimplementedException, VisADException,
                   RemoteException {

        Util.vetType(Pressure.getRealType(), pressure);
        Util.vetType(Temperature.getRealType(), temperature);

        return Util.clone(
            VisADMath.divide(
                pressure,
                VisADMath.multiply(
                    DryAirGasConstant.newReal(),
                    temperature)), getRealType());
    }

    /**
     * Creates an AirPressure -> AirDensity field from an Pressure ->
     * (InSituAirTemperature, DewPoint) field.
     * @param inputField        An Pressure -> (InSituAirTemperature,
     *                          DewPoint) field.
     * @return                  The (AirPressure -> AirDensity) field
     *                          corresponding to the input field.
     * @throws TypeException    At least one argument has wrong type.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static FlatField create(FlatField inputField)
            throws TypeException, UnimplementedException, VisADException,
                   RemoteException {

        return (FlatField) create(inputField.getDomainSet(),
                                  DataUtility
                                      .ensureRange(inputField,
                                          InSituAirTemperature
                                              .getRealType()), DataUtility
                                                  .ensureRange(inputField,
                                                      DewPoint
                                                          .getRealType()));
    }
}
