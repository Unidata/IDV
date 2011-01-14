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
 * Provides support for the quantity of air pressure.
 *
 * @author Steven R. Emmerson
 * @version $Id: AirPressure.java,v 1.16 2006/10/12 19:22:19 dmurray Exp $
 */
public class AirPressure extends Pressure {

    /**
     * The single instance.
     */
    private static AirPressure INSTANCE;

    /**
     * Air pressure coordinate system for pressure to height using
     *   the standard atmosphere
     */
    private static AirPressureCoordinateSystem standardAtmosphereCS;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private AirPressure() throws VisADException {
        this("AirPressure");
    }

    /**
     * Constructs from a name.  The resulting, associated RealTupleType has
     * a CoordinateSystem that is based on the Standard Atmosphere with a
     * reference RealTupleType of Altitude.  It also has a default domain set
     * that is a SingletonSet of one standard atmosphere (i.e. 1013.25 hPa).
     *
     * @param name              The name for the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected AirPressure(String name) throws VisADException {

        super(getRealType(name, CommonUnits.HECTOPASCAL),
              getStandardCoordinateSystem(name));

        try {
            realTupleType().setDefaultSet(
                new SingletonSet(
                    new RealTuple(
                        realTupleType(),
                        new Real[] { new Real(realType(), 1013.25,  // standard atmosphere
                    CommonUnits.HECTOPASCAL) }, (CoordinateSystem) null)));
        } catch (RemoteException e) {}  // can't happen because above data is local
    }

    /**
     * Returns a coordinate system transformation for the Standard Atmosphere.
     *
     * @return                A coordinate system transformation for the
     *                        Standard Atmosphere.
     * @throws VisADException if a core VisAD failure occurs.
     */
    public static AirPressureCoordinateSystem getStandardAtmosphereCS()
            throws VisADException {

        if (standardAtmosphereCS == null) {
            synchronized (AirPressure.class) {
                if (standardAtmosphereCS == null) {
                    standardAtmosphereCS =
                        (AirPressureCoordinateSystem) getStandardCoordinateSystem(
                            getRealType().getName());
                }
            }
        }

        return standardAtmosphereCS;
    }

    /**
     * Returns a coordinateSystem based on the Standard Atmosphere.
     *
     * @param name
     * @return
     *
     * @throws VisADException
     */
    private static CoordinateSystem getStandardCoordinateSystem(String name)
            throws VisADException {

        CoordinateSystem cs = null;

        try {
            cs = new StandardAtmosphereCoordinateSystem();
        } catch (RemoteException e) {}  // can't happen because above data is local

        return cs;
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
            synchronized (AirPressure.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AirPressure();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Converts pressure to altitude.
     *
     * @param pressure        A pressure data object.
     * @return                The corresponding altitude data object.
     * @throws VisADException if a core VisAD failure occurs.
     */
    public static Data toAltitude(Data pressure) throws VisADException {

        Data altitude;

        if (pressure instanceof Real) {
            altitude = new Real(
                Altitude.getRealType(),
                getStandardAtmosphereCS().toReference(new double[][] {
                { ((Real) pressure).getValue(
                    getStandardAtmosphereCS().getCoordinateSystemUnits()[0]) }
            })[0][0], getStandardAtmosphereCS().getReferenceUnits()[0]);
        } else {
            altitude = null;
        }

        return altitude;
    }

    /**
     * Converts altitude to pressure.
     *
     * @param altitude        A altitude data object.
     * @return                The corresponding pressure data object.
     * @throws VisADException if a core VisAD failure occurs.
     */
    public static Data fromAltitude(Data altitude) throws VisADException {

        Data pressure;

        if (altitude instanceof Real) {
            pressure = new Real(
                getRealType(),
                getStandardAtmosphereCS().fromReference(new double[][] {
                { ((Real) altitude).getValue(
                    getStandardAtmosphereCS().getReferenceUnits()[0]) }
            })[0][0], getStandardAtmosphereCS()
                .getCoordinateSystemUnits()[0]);
        } else {
            pressure = null;
        }

        return pressure;
    }

    /**
     * Provides support for a Pressure <-> Altitude coordinate system
     * transformation.
     *
     * @author Steven R. Emmerson
     */
    public static class AirPressureCoordinateSystem extends CoordinateSystem {

        /**
         * The reference pressure.
         * @serial
         */
        private final double p0Float;

        /**
         * The reference pressure.
         * @serial
         */
        private final double p0Double;

        /**
         * The log-P empirical coordinate system transformation.
         * @serial
         */
        private final EmpiricalCoordinateSystem logPCoordSys;

        /**
         * Constructs from a set of Pressure-s and a set of Altitude-s.  The
         * reference RealTupleType will be that of the altitude set.  The unit
         * of the pressure coordinate will be the actual unit of pressure in
         * the pressure set.
         *
         * @param pressure              A set of Pressure-s.
         * @param altitude              A set of Altitude-s.
         * @throws VisADException       VisAD failure.
         * @throws RemoteException      Java RMI failure.
         */
        public AirPressureCoordinateSystem(Gridded1DSet pressure,
                                           Gridded1DSet altitude)
                throws VisADException, RemoteException {

            super(((SetType) altitude.getType()).getDomain(),
                  pressure.getSetUnits());

            /*
             * Because EmpiricalCoordinateSystem uses linear interpolation,
             * the pressure values are converted to log(p/p0) values so that
             * interpolation will occur in a more linear environment.
             */
            p0Float  = pressure.getHi()[0];
            p0Double = p0Float;
            logPCoordSys =
                new EmpiricalCoordinateSystem(
                    (GriddedSet) VisADMath
                        .log(VisADMath
                            .divide(
                                pressure, new Real(
                                    (RealType) ((SetType) pressure.getType())
                                        .getDomain()
                                        .getComponent(0), p0Double, pressure
                                        .getSetUnits()[0]))), altitude);
        }

        /**
         * Converts from pressures to altitudes.
         *
         * @param pressures     The pressures to be converted in units of
         *                      getCoordinateSystemUnits()[0].  Only
         *                      pressures[0] is used.  It may be overwritten.
         * @return              Altitudes in units of getReferenceUnits()[0]
         *                      corresponding to the input pressures.
         * @throws VisADException       VisAD failure.
         */
        public double[][] toReference(double[][] pressures)
                throws VisADException {

            double[] vals = pressures[0];

            for (int i = vals.length; --i >= 0; ) {
                vals[i] = Math.log(vals[i] / p0Double);
            }

            return logPCoordSys.toReference(pressures);
        }

        /**
         * Converts to pressures from altitudes.
         *
         * @param altitudes     The altitudes to be converted in units of
         *                      getReferenceUnits()[0].  Only altitudes[0] is
         *                      used.  It may be overwritten.
         * @return              Pressures in units of
         *                      getCoordinateSystemUnits()[0] corresponding to
         *                      the input altitudes.
         * @throws VisADException       VisAD failure.
         */
        public double[][] fromReference(double[][] altitudes)
                throws VisADException {

            altitudes = logPCoordSys.fromReference(altitudes);

            double[] vals = altitudes[0];

            for (int i = vals.length; --i >= 0; ) {
                vals[i] = p0Double * Math.exp(vals[i]);
            }

            return altitudes;
        }

        /**
         * Converts from pressures to altitudes.
         *
         * @param pressures     The pressures to be converted in units of
         *                      getCoordinateSystemUnits()[0].  Only
         *                      pressures[0] is used.  It may be overwritten.
         * @return              Altitudes in units of getReferenceUnits()[0]
         *                      corresponding to the input pressures.
         * @throws VisADException       VisAD failure.
         */
        public float[][] toReference(float[][] pressures)
                throws VisADException {

            float[] vals = pressures[0];

            for (int i = vals.length; --i >= 0; ) {
                vals[i] = (float) Math.log(vals[i] / p0Double);
            }

            return logPCoordSys.toReference(pressures);
        }

        /**
         * Converts to pressures from altitudes.
         *
         * @param altitudes     The altitudes to be converted in units of
         *                      getReferenceUnits()[0].  Only altitudes[0] is
         *                      used.  It may be overwritten.
         * @return              Pressures in units of
         *                      getCoordinateSystemUnits()[0] corresponding to
         *                      the input altitudes.
         * @throws VisADException       VisAD failure.
         */
        public float[][] fromReference(float[][] altitudes)
                throws VisADException {

            altitudes = logPCoordSys.fromReference(altitudes);

            float[] vals = altitudes[0];

            for (int i = vals.length; --i >= 0; ) {
                vals[i] = (float) (p0Double * Math.exp(vals[i]));
            }

            return altitudes;
        }

        /**
         * Indicates if this instance is semantically idential to another
         * object.
         *
         * @param obj           The other object.
         * @return              True if an only if this instance is sematically
         *                      equall to <code>obj</code>.
         */
        public boolean equals(Object obj) {

            boolean equals;

            if ( !(obj instanceof AirPressureCoordinateSystem)) {
                equals = false;
            } else {
                AirPressureCoordinateSystem that =
                    (AirPressureCoordinateSystem) obj;

                equals = (this == that)
                         || ((p0Double == that.p0Double)
                             && logPCoordSys.equals(that.logPCoordSys));
            }

            return equals;
        }
    }

    /**
     * The class for the standard atmosphere converion from altitude to pressure
     *
     * @author IDV Development Team
     * @version $Revision: 1.16 $
     */
    public static class StandardAtmosphereCoordinateSystem extends AirPressureCoordinateSystem {

        /** list of pressures */
        private static float[] pressures = new float[] {
            1013.25f, 954.61f, 898.76f, 845.59f, 795.01f, 746.91f, 701.21f,
            657.80f, 616.6f, 577.52f, 540.48f, 505.39f, 472.17f, 440.75f,
            411.05f, 382.99f, 356.51f, 331.54f, 303.00f, 285.85f, 264.99f,
            226.99f, 193.99f, 165.79f, 141.70f, 121.11f, 103.52f, 88.497f,
            75.652f, 64.674f, 55.293f, 25.492f, 11.970f, 5.746f, 2.871f,
            1.491f, 0.798f, 0.220f, 0.052f, 0.010f,
        };

        /** list of corresponding altitudes */
        private static float[] alts = new float[] {
            0, 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000,
            5500, 6000, 6500, 7000, 7500, 8000, 8500, 9000, 9500, 10000,
            11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000,
            20000, 25000, 30000, 35000, 40000, 45000, 50000, 60000, 70000,
            80000,
        };

        /**
         * Create a new StandardAtmosphereCoordinateSystem.
         *
         * @throws RemoteException  can't happen, but we've got to throw it
         * @throws VisADException   Problem creating the CoordinateSystem
         */
        public StandardAtmosphereCoordinateSystem()
                throws VisADException, RemoteException {
            super(new Gridded1DSet(getRealType("AirPressure",
                    CommonUnits.HECTOPASCAL), new float[][] {
                pressures
            }, 40, (CoordinateSystem) null,
               new Unit[] { CommonUnits.HECTOPASCAL },
               (ErrorEstimate[]) null), new Gridded1DSet(
                   Altitude.getRealType(), new float[][] {
                alts
            }, 40, (CoordinateSystem) null, new Unit[] { SI.meter },
               (ErrorEstimate[]) null));
        }
    }
}
