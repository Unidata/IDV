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

import visad.data.units.DefaultUnitsDB;



import java.rmi.RemoteException;


/**
 * Provides support for the latent heat of evaporation.
 *
 * @author Steven R. Emmerson
 * @version $Id: PseudoAdiabaticLapseRate.java,v 1.12 2005/05/13 18:35:42 jeffmc Exp $
 */
public class PseudoAdiabaticLapseRate extends ScalarQuantity {

    /**
     * The VisAD RealType of this quantity.
     */
    private static RealType realType;

    /**
     * The dimensionless quantity one.
     */
    protected static Real one;

    /**
     * The latent heat of evaporation divided by the dry air gas constant.
     */
    protected static Real lOverRSubD;

    /**
     * (waterVaporMolecularWeight / dryAirMolecularWeight)
     * latentHeatOfEvaporation
     */
    protected static Real epsilonL;

    /**
     * ((waterVaporMolecularWeight / dryAirMolecularWeight)
     * latentHeatOfEvaporation.pow(2))
     * (dryAirSpecificHeatCapacity * dryAirGasConstant)
     */
    protected static Real epsilonL2OverCSubPRSubD;

    static {
        try {
            if (one == null) {
                synchronized (PseudoAdiabaticLapseRate.class) {
                    if (one == null) {
                        try {
                            one = new Real(
                                RealType.getRealType(
                                    "PseudoAdiabaticLapseRateOne",
                                    CommonUnit.dimensionless,
                                    (Set) null), 1.0);
                            lOverRSubD =
                                (Real) LatentHeatOfEvaporation.newReal()
                                    .divide(DryAirGasConstant.newReal());
                            epsilonL =
                                (Real) MolecularWeightOfWater.newReal()
                                    .divide(MolecularWeightOfDryAir.newReal())
                                    .multiply(LatentHeatOfEvaporation
                                        .newReal());
                            epsilonL2OverCSubPRSubD = (Real) MolecularWeightOfWater
                                .newReal()
                                .divide(MolecularWeightOfDryAir.newReal())
                                .multiply(lOverRSubD)
                                .multiply(LatentHeatOfEvaporation.newReal())
                                .divide(
                                    SpecificHeatCapacityOfDryAirAtConstantPressure
                                        .newReal());
                        } catch (RemoteException e) {}  // can't happen because above objects are local
                    }
                }
            }
        } catch (Exception e) {
            System.err.println(
                "PseudoAdiabaticLapseRate.<clinit>: Couldn't initialize class: "
                + e);
            System.exit(1);
        }
    }

    /**
     * Constructs from nothing.
     *
     * @throws UnitException
     * @throws VisADException
     */
    private PseudoAdiabaticLapseRate() throws UnitException, VisADException {
        super(getRealType());
    }

    /**
     * Obtains the RealType associated with this class.
     *
     * @return                  The RealType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealType getRealType() throws VisADException {

        if (realType == null) {
            synchronized (PseudoAdiabaticLapseRate.class) {
                if (realType == null) {
                    realType = RealType.getRealType(
                        "PseudoAdiabaticLapseRate", SI.kelvin.divide(
                            DefaultUnitsDB.instance().get(
                                "gpm")), (Set) null);
                }
            }
        }

        return realType;
    }

    /**
     * Creates a pseudo-adiabatic lapse-rate data object from data objects for
     * in-situ air pressure and temperature.  Reasonable approximations are
     * used.
     *
     * @param pressure          The in-situ air pressure data object.
     * @param temperature       The in-situ air temperature data object.
     * @return                  The lapse-rate data object corresponding to the
     *                          input arguments.
     * @throws TypeException    An input argument has wrong type.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature)
            throws TypeException, VisADException, RemoteException {

        Util.vetType(Pressure.getRealType(), pressure);
        Util.vetType(Temperature.getRealType(), temperature);

        Data wSubSOverT =
            VisADMath.divide(SaturationMixingRatio.create(pressure,
                temperature), temperature);

        /*
        Data    epsilonLOverP = VisADMath.divide(epsilonL, pressure);
        */
        return Util.clone(
        /*
         * Expression from "Introduction to Theoretical Meteorology" by
         * Seymour L. Hess, 1985; Robert E. Krieger Publishing Company;
         * ISBN 0-88275-857-8.
         */
        VisADMath.divide(VisADMath.divide(VisADMath.add(one,
                VisADMath.multiply(lOverRSubD,
                    wSubSOverT)), VisADMath.add(one,
                        VisADMath.multiply(epsilonL2OverCSubPRSubD,
                            VisADMath.divide(wSubSOverT,
                                temperature)))), SpecificHeatCapacityOfDryAirAtConstantPressure.newReal()),
        /*
        VisADMath.divide(
            VisADMath.add(
                one,
                VisADMath.multiply(
                    epsilonLOverP,
                    VisADMath.divide(
                        SaturationVaporPressure.create(temperature),
                        VisADMath.multiply(
                            DryAirGasConstant.newReal(),
                            temperature)))),
            VisADMath.add(
                SpecificHeatCapacityOfDryAirAtConstantPressure
                    .newReal(),
                VisADMath.multiply(
                    epsilonLOverP,
                    SaturationVaporPressure.temperatureDerivative(
                        temperature)))),
        */
        PseudoAdiabaticLapseRate.getRealType());
    }

    /**
     * Tests this class.
     * @param args              Arguments, Ignored.
     * @throws Exception        Something went wrong.
     */
    public static void main(String[] args) throws Exception {

        Real lapseRate = (Real) VisADMath.multiply(
                             Gravity.newReal(),
                             create(new Real(
                                 AirPressure.getRealType(), 1000,
                                 CommonUnits.HECTOPASCAL), new Real(
                                     AirTemperature.getRealType(), 24.5,
                                     CommonUnits.CELSIUS)));

        System.out.println(
            SI.kelvin.divide(SI.meter.scale(1000)).toThis(
                lapseRate.getValue(), lapseRate.getUnit()));
    }
}
