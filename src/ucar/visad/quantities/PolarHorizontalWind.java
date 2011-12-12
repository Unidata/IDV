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


import visad.*;

import java.rmi.RemoteException;


/**
 * Provides support for the quantity of horizontal wind in polar coordinates.
 * Instances of this class have CartesianHorizontalWind as their reference
 * CoordinateSystem.
 *
 * @author Steven R. Emmerson
 */
public final class PolarHorizontalWind extends HorizontalWind {

    /**
     * The single instance.
     */
    private static PolarHorizontalWind INSTANCE;

    /**
     * Constructs from nothing.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private PolarHorizontalWind() throws VisADException {

        super(new RealType[] { getSpeedRealType(), getDirectionRealType() },
              new PolarCoordinateSystem(
                  getSpeedRealType().getDefaultUnit(),
                  getDirectionRealType().getDefaultUnit()));
    }

    /**
     * Obtains the RealTupleType associated with this class.
     *
     * @return                  The RealTupleType associated with this class.
     * @throws VisADException   Couldn't perform necessary VisAD operation.
     */
    public static RealTupleType getRealTupleType() throws VisADException {

        if (INSTANCE == null) {
            synchronized (PolarHorizontalWind.class) {
                if (INSTANCE == null) {
                    INSTANCE = new PolarHorizontalWind();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Obtain the RealType of the speed component.
     *
     * @return                  The RealType of the speed compoment.
     * @throws VisADException   if a core VisAD failure occurs.
     */
    public static RealType getSpeedRealType() throws VisADException {
        return Speed.getRealType();
    }

    /**
     * Obtain the RealType of the direction component.
     *
     * @return                  The RealType of the direction compoment.
     * @throws VisADException   if a core VisAD failure occurs.
     */
    public static RealType getDirectionRealType() throws VisADException {
        return Direction.getRealType();
    }

    /**
     * Returns a (speed,direction) tuple constructed from a (u,v) tuple.
     * @param uv                The (westerly,southerly) wind.
     * @return                  The wind in polar coordinates.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static RealTuple newRealTuple(RealTuple uv)
            throws VisADException, RemoteException {

        RealType spdType   = getSpeedRealType();
        RealType dirType   = getDirectionRealType();
        Unit     spdUnit   = spdType.getDefaultUnit();
        double   u         = ((Real) uv.getComponent(0)).getValue(spdUnit);
        double   v         = ((Real) uv.getComponent(1)).getValue(spdUnit);
        double   speed     = Math.sqrt(u * u + v * v);
        double   direction = Math.atan2(-u, -v);

        if (direction < 0) {
            direction += 2 * Math.PI;
        }

        direction = dirType.getDefaultUnit().toThis(direction, SI.radian);

        return new RealTuple(getRealTupleType(),
                             new Real[] { new Real(spdType, speed),
                                          new Real(dirType,
                                          direction) }, (CoordinateSystem) null);
    }

    /**
     * Provides support for transforming wind coordinates between polar
     * and cartesian representations.
     */
    public static final class PolarCoordinateSystem extends CoordinateSystem {

        /**
         * Two PI.
         */
        private static final double twoPI = 2 * Math.PI;

        /**
         * Units of the internal computations.
         * @serial
         */
        private final Unit[] internalUnits;

        /**
         * Constructs from units for speed and direction.
         *
         * @param speedUnit             The unit for speed.
         * @param directionUnit         The unit for direction.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public PolarCoordinateSystem(Unit speedUnit, Unit directionUnit)
                throws VisADException {
            this(CartesianHorizontalWind.getRealTupleType(), speedUnit,
                 directionUnit);
        }

        /**
         * Constructs from units for speed and direction.
         *
         *
         * @param refType               The reference type
         * @param speedUnit             The unit for speed.
         * @param directionUnit         The unit for direction.
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public PolarCoordinateSystem(RealTupleType refType, Unit speedUnit,
                                     Unit directionUnit)
                throws VisADException {

            super(refType, new Unit[] { speedUnit, directionUnit });

            internalUnits =
                new Unit[] {
                    ((RealType) CartesianHorizontalWind.getRealTupleType()
                        .getComponent(0)).getDefaultUnit(),
                    SI.radian };
        }

        /**
         * Converts wind coordinates from polar to cartesian.
         *
         * @param values                Polar wind coordinates (speed,
         *                              direction).
         * @return                      Cartesian wind coordinates (u, v).
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public double[][] toReference(double[][] values)
                throws VisADException {

            values = Unit.convertTuple(values, getCoordinateSystemUnits(),
                                       internalUnits);

            double[] westerlies  = values[0];
            double[] southerlies = values[1];
            double[] speeds      = values[0];
            double[] directions  = values[1];

            for (int i = values[0].length; --i >= 0; ) {
                double speed     = -speeds[i];
                double direction = directions[i];

                westerlies[i]  = speed * Math.sin(direction);
                southerlies[i] = speed * Math.cos(direction);
            }

            return values;
        }

        /**
         * Converts wind coordinates from polar to cartesian.
         *
         * @param values                Polar wind coordinates (speed,
         *                              direction).
         * @return                      Cartesian wind coordinates (u, v).
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public float[][] toReference(float[][] values) throws VisADException {

            values = Unit.convertTuple(values, getCoordinateSystemUnits(),
                                       internalUnits);

            float[] westerlies  = values[0];
            float[] southerlies = values[1];
            float[] speeds      = values[0];
            float[] directions  = values[1];

            for (int i = values[0].length; --i >= 0; ) {
                float speed     = -speeds[i];
                float direction = directions[i];

                westerlies[i]  = speed * (float) Math.sin(direction);
                southerlies[i] = speed * (float) Math.cos(direction);
            }

            return values;
        }

        /**
         * Converts wind coordinates from cartesian to polar.
         *
         * @param values                Cartesian wind coordinates (u, v).
         * @return                      Polar wind coordinates (speed,
         *                              direction).
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public double[][] fromReference(double[][] values)
                throws VisADException {

            double[] westerlies  = values[0];
            double[] southerlies = values[1];
            double[] speeds      = values[0];
            double[] directions  = values[1];
            double   direction;

            for (int i = values[0].length; --i >= 0; ) {
                double u = westerlies[i];
                double v = southerlies[i];

                speeds[i] = Math.sqrt(u * u + v * v);
                direction = Math.atan2(-u, -v);

                if (direction < 0) {
                    direction += twoPI;
                }

                directions[i] = direction;
            }

            values = Unit.convertTuple(values, internalUnits,
                                       getCoordinateSystemUnits());

            return values;
        }

        /**
         * Converts wind coordinates from cartesian to polar.
         *
         * @param values                Cartesian wind coordinates (u, v).
         * @return                      Polar wind coordinates (speed,
         *                              direction).
         * @throws VisADException       Couldn't create necessary VisAD object.
         */
        public float[][] fromReference(float[][] values)
                throws VisADException {

            float[] westerlies  = values[0];
            float[] southerlies = values[1];
            float[] speeds      = values[0];
            float[] directions  = values[1];
            double  direction;

            for (int i = values[0].length; --i >= 0; ) {
                float u = westerlies[i];
                float v = southerlies[i];

                speeds[i] = (float) Math.sqrt(u * u + v * v);
                direction = Math.atan2(-u, -v);

                if (direction < 0) {
                    direction += twoPI;
                }

                directions[i] = (float) direction;
            }

            values = Unit.convertTuple(values, internalUnits,
                                       getCoordinateSystemUnits());

            return values;
        }

        /**
         * Tests for semantic equality with an object.
         *
         * @param object        The object.
         * @return              True if and only if this object is semantically
         *                      identical to the specified object.
         */
        public boolean equals(Object object) {

            if ( !(object instanceof PolarCoordinateSystem)) {
                return false;
            }

            PolarCoordinateSystem that = (PolarCoordinateSystem) object;

            return java.util.Arrays.equals(internalUnits, that.internalUnits);
        }
    }
}
