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

import visad.ScaledUnit;

import visad.Set;

import visad.TypeException;

import visad.UnimplementedException;

import visad.UnitException;

import visad.VisADException;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of saturation mixing-ratio.
 *
 * Instances are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: WaterVaporMixingRatio.java,v 1.15 2005/05/13 18:35:45 jeffmc Exp $
 */
public class WaterVaporMixingRatio extends ScalarQuantity {

    /**
     * The single instance.
     */
    private static WaterVaporMixingRatio INSTANCE;

    /**
     * The ratio of the water vapor and dry air gas constants.
     */
    private static Real gasConstantRatio;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private WaterVaporMixingRatio() throws VisADException {
        this("WaterVaporMixingRatio");
    }

    /**
     * Constructs from a name.
     *
     * @param name              The name for the instance.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected WaterVaporMixingRatio(String name) throws VisADException {
        super(name, CommonUnits.GRAMS_PER_KILOGRAM, (Set) null);
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
            synchronized (WaterVaporMixingRatio.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WaterVaporMixingRatio();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Returns the ratio of the water vapor and dry air gas constants.
     *
     * @return                  The water vapor to dry air gas constant ratio.
     * @throws VisADException if a core VisAD failure occurs.
     */
    public static Real getGasConstantRatio() throws VisADException {

        if (gasConstantRatio == null) {
            synchronized (WaterVaporMixingRatio.class) {
                if (gasConstantRatio == null) {
                    try {
                        gasConstantRatio = new Real(RealType
                            .getRealType("WaterVaporMixingRatioGasConstantRatio",
                                CommonUnit.dimensionless,
                                (Set) null), ((Real) VisADMath
                                    .divide(MolecularWeightOfWater.newReal(),
                                        MolecularWeightOfDryAir.newReal()))
                                            .getValue(CommonUnit
                                                .dimensionless));
                    } catch (RemoteException e) {}  // can't happen because above are local
                }
            }
        }

        return gasConstantRatio;
    }

    /**
     * Creates a water-vapor mixing-ratio data object from data objects for
     * pressure and temperature.
     *
     * @param pressure          The pressure data object.
     * @param temperature       The temperature data object.  If the in-situ
     *                          temperature is used, then the returned object is
     *                          the saturation mixing ratio.  If the dew-point
     *                          temperature is used, then the returned object is
     *                          the actual mixing-ratio.
     * @return                  Water vapor mixing-ratio or saturation
     *                          mixing-ratio depending on the type of
     *                          <code>temperature</code>.
     * @throws TypeException    At least one argument has incorrect type.
     * @throws UnitException    Unit conversion failure.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature)
            throws TypeException, VisADException, UnitException,
                   RemoteException {

        Data saturationVaporPressure =
            SaturationVaporPressure.create(temperature);

        return Util.clone(
            VisADMath.multiply(
                getGasConstantRatio(), VisADMath.divide(
                    saturationVaporPressure, VisADMath.subtract(
                        pressure, saturationVaporPressure))), getRealType());
    }

    /**
     * Creates a water-vapor mixing-ratio data object from specific humidity
     *
     * @param specificHumidity  The specific humidity data object.
     * @return                  Water vapor mixing-ratio or saturation
     *                          mixing-ratio depending on the type of
     *                          <code>temperature</code>.
     * @throws TypeException    At least one argument has incorrect type.
     * @throws UnitException    Unit conversion failure.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data specificHumidity)
            throws TypeException, VisADException, UnitException,
                   RemoteException {

        return Util.clone(
            VisADMath.divide(
                specificHumidity, VisADMath.subtract(
                    new Real(1), specificHumidity)), getRealType());
    }

}
