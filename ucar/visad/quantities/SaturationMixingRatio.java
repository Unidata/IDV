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

import visad.*;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of saturation mixing-ratio.
 *
 * Instances are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Id: SaturationMixingRatio.java,v 1.15 2005/05/13 18:35:43 jeffmc Exp $
 */
public final class SaturationMixingRatio extends WaterVaporMixingRatio {

    /**
     * The single instance.
     */
    private static SaturationMixingRatio INSTANCE;

    /**
     * The single RealType.
     */
    private static RealType myRealType;

    /**
     * The single Function.
     */
    private static Function myFunction;

    static {
        try {
            myRealType = RealType.getRealType("SaturationMixingRatio",
                    CommonUnits.GRAMS_PER_KILOGRAM, (Set) null);
            myFunction = new MyFunction();
        } catch (Exception e) {
            System.err.println("SaturationMixingRatio.<clinit>: "
                               + "Couldn't initialize class: " + e);
            System.exit(1);
        }
    }

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private SaturationMixingRatio() throws VisADException {
        super("SaturationMixingRatio");
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
            synchronized (SaturationMixingRatio.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SaturationMixingRatio();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Creates an AirTemperature data object from data objects of pressure and
     * saturation mixing-ratio.
     *
     * @param pressure          The air pressure data object.
     * @param rSat              The saturation mixing-ratio data object.
     * @return                  The air temperature data object computed from
     *                          the pressure and temperature data objects.  The
     *                          type of the object will be that of the arguments
     *                          after standard promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has incorrect type.
     * @throws UnitException    Inappropriate unit argument.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data createTemperature(Data pressure, Data rSat)
            throws TypeException, UnitException, VisADException,
                   RemoteException {

        Util.vetType(Pressure.getRealType(), pressure);
        Util.vetType(SaturationMixingRatio.getRealType(), rSat);

        return SaturationVaporPressure.createTemperature(
            Util.clone(
                VisADMath.divide(
                    VisADMath.multiply(rSat, pressure),
                    VisADMath.add(
                        getGasConstantRatio(),
                        rSat)), SaturationVaporPressure.getRealType()));
    }

    /**
     * Creates a saturation water-vapor mixing-ratio data object from data
     * objects for pressure and in-situ temperature.
     * @param pressure          The pressure data object.
     * @param temperature       The in-situ temperature data object.
     * @return                  Water vapor mixing-ratio.  The type of the
     *                          object will be that of the arguments after
     *                          standard promotion.
     * @see ucar.visad.VisADMath
     * @throws TypeException    At least one argument has incorrect type.
     * @throws UnitException    Unit convertion failure.
     * @throws UnimplementedException
     *                          Necessary operation not yet implemented.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data create(Data pressure, Data temperature)
            throws TypeException, UnitException, UnimplementedException,
                   VisADException, RemoteException {

        /*
         * This is the same as creating a regular water-vapor
         * mixing-ratio object using the in-situ temperature.
         */
        Util.vetType(InSituAirTemperature.getRealType(), temperature);

        return Util.clone(WaterVaporMixingRatio.create(pressure,
                temperature), getRealType());
    }

    /**
     * Returns a SaturationMixingRatio(pressure, temperature) function.
     *
     * @return                    A SaturationMixingRatio(pressure, temperature)
     *                            function.
     */
    public static Function newFunction() {
        return myFunction;
    }

    /**
     * The SaturationMixingRatio(pressure, temperature) function.
     */
    protected static class MyFunction extends FunctionImpl {

        /** a gradient tuple */
        private static Tuple gradient;

        static {
            try {
                gradient = null;
            } catch (Exception e) {
                System.err.println(
                    "WaterVaporMixingRatio$MyFunction.<clint>: "
                    + "Couldn't initialize class: " + e);
                System.exit(1);
            }
        }

        /**
         * Constructs from nothing.
         *
         * @throws VisADException if a core VisAD failure occurs.
         */
        public MyFunction() throws VisADException {
            super(new FunctionType(
                new RealTupleType(
                    AirPressure.getRealType(),
                    AirTemperature.getRealType()), myRealType));
        }

        /**
         * Returns the partial derivatives of this function with respect to
         * pressure and temperature.
         *
         * @param errorMode  The error mode.  Ignored.
         * @return           The partial derivatives of this function.
         */
        public Data derivative(int errorMode) {
            return gradient;
        }

        /**
         * Returns null.
         *
         * @param partials    mathtypes of partials
         * @param errorMode   error mode for resampling
         * @return null.
         */
        public Data derivative(MathType[] partials, int errorMode) {
            return null;
        }

        /**
         * Returns null.
         *
         * @param location           location
         * @param inputPartials      partial types
         * @param outputPartials     output partial types
         * @param errorMode          error mode for resampling
         * @return null.
         */
        public Data derivative(RealTuple location, RealType[] inputPartials,
                               MathType[] outputPartials, int errorMode) {
            return null;
        }

        /**
         * Returns null.
         *
         * @param inputPartial   partial derivative RealType
         * @param errorMode      error mode
         * @return null.
         */
        public Function derivative(RealType inputPartial, int errorMode) {
            return null;
        }

        /**
         * Returns null.
         *
         * @param inputPartial   partial derivative RealType
         * @param ouputType      output MathType
         * @param errorMode      error mode
         * @return null.
         */
        public Function derivative(RealType inputPartial, MathType ouputType,
                                   int errorMode) {
            return null;
        }

        /**
         * Returns null.
         *
         * @param domain        sampling set
         * @param samplingMode  sampling mode
         * @param errorMode     error mode
         * @return null.
         */
        public Field resample(Set domain, int samplingMode, int errorMode) {
            return null;
        }

        /**
         * Returns null.
         *
         * @param type       The shadow type.
         * @param shadow     Another shadow?
         * @return           A shadow?
         */
        public DataShadow computeRanges(ShadowType type, DataShadow shadow) {
            return null;
        }

        /**
         * Returns this instance.
         *
         * @return           This.
         */
        public Object clone() {
            return this;
        }

        /**
         * Returns true.
         *
         * @return true.
         */
        public boolean isMissing() {
            return true;
        }
    }
}
