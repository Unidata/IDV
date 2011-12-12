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

import visad.CommonUnit;

import visad.CoordinateSystem;

import visad.Data;

import visad.DerivedUnit;

import visad.Real;

import visad.RealTuple;

import visad.RealTupleType;

import visad.RealType;

import visad.SI;

import visad.ScaledUnit;

import visad.Set;

import visad.SingletonSet;

import visad.TypeException;

import visad.UnimplementedException;

import visad.Unit;

import visad.UnitException;

import visad.VisADException;



import java.rmi.RemoteException;


/**
 * Provides support for the quantity of geopotential altitude.
 *
 * @author Steven R. Emmerson
 * @version $Id: GeopotentialAltitude.java,v 1.15 2006/10/09 14:49:16 dmurray Exp $
 */
public final class GeopotentialAltitude extends ScalarQuantity {

    /**
     * The single instance.
     */
    private static GeopotentialAltitude INSTANCE;

    /**
     * The geopotential meter unit.
     */
    private static Unit geopotentialMeter;

    /**
     * Constructs from nothing.  The default unit will be the geopotential
     * meter and the default domain set will be a singleton set at mean
     * sea level.
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    private GeopotentialAltitude() throws VisADException {

        super(getRealType("GeopotentialAltitude", getGeopotentialMeter()),
              new GeopotentialCoordinateSystem(Gravity.newReal(),
                  Altitude.getRealType(), getGeopotentialMeter()));

        try {
            realTupleType().setDefaultSet(
                new SingletonSet(
                    new RealTuple(
                        realTupleType(),
                        new Real[] { new Real(realType(), 0.0) },  // msl
                    (CoordinateSystem) null)));
        } catch (RemoteException e) {}  // can't happen because above data is local
    }

    /**
     * Gets the geopotential meter unit.
     *
     * @return                  A geopotential meter unit.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    public static Unit getGeopotentialMeter() throws VisADException {

        if (geopotentialMeter == null) {
            synchronized (GeopotentialAltitude.class) {
                try {
                    if (geopotentialMeter == null) {
                        geopotentialMeter = getGeopotentialUnit(SI.meter,
                                Gravity.newReal()).clone("gpm");
                    }
                } catch (RemoteException e) {}  // can't happen because above data are local
            }
        }

        return geopotentialMeter;
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
            synchronized (GeopotentialAltitude.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GeopotentialAltitude();
                }
            }
        }

        return INSTANCE.realTupleType();
    }

    /**
     * Converts altitude to geopotential altitude.
     *
     * @param altitude          The altitude data object.
     * @return                  The equivalent geopotential altitude data
     *                          object.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public static Data fromAltitude(Data altitude)
            throws VisADException, RemoteException {
        return fromAltitude(altitude, Gravity.newReal());
    }

    /**
     * Converts altitude to geopotential altitude based on a value for gravity.
     *
     * @param altitude          The altitude data object.
     * @param gravity           The value for gravity.
     * @return                  The equivalent geopotential altitude data
     *                          object.
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public static Data fromAltitude(Data altitude, Real gravity)
            throws VisADException, RemoteException {
        return Util.clone(altitude.multiply(gravity), getRealType());
    }

    /**
     * Converts from geopotential altitude to altitude.
     *
     * @param geopotentialAltitude
     *                          Geopotential altitude.
     * @param gravity           Gravity.
     * @return                  Altitude.  The type of the object will be that
     *                          of the arguments after standard promotion.
     * @see ucar.visad.VisADMath
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data toAltitude(Data geopotentialAltitude, Real gravity)
            throws VisADException, RemoteException {
        return Util.clone(geopotentialAltitude.divide(gravity),
                          Altitude.getRealType());
    }

    /**
     * Converts from geopotential altitude to altitude.
     *
     * @param geopotentialAltitude
     *                          Geopotential altitude.
     * @return                  Altitude.  The type of the object will be that
     *                          of the arguments after standard promotion.
     * @see ucar.visad.VisADMath
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static Data toAltitude(Data geopotentialAltitude)
            throws VisADException, RemoteException {
        return toAltitude(geopotentialAltitude, Gravity.newReal());
    }

    /**
     * Converts from geopotential altitude to altitude.
     *
     * @param geoAlts           Geopotential altitudes in units of
     *                          <code>geoUnit</code>.
     * @param geoUnit           The unit of geopotential altitude.
     * @param gravity           Gravity.
     * @param altitudes         Altitudes in units of <code>altUnit</code>.
     *                          Its length shall be at least
     *                          <code>geoAlts.length</code>.  It may be the same
     *                          array as <code>geoAlts</code>.
     * @param altUnit           The unit of altitude.
     * @return                  <code>altitudes</code> (as a convenience).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static double[] toAltitude(double[] geoAlts, Unit geoUnit,
                                      Real gravity, double[] altitudes,
                                      Unit altUnit)
            throws VisADException, RemoteException {

        return toAltitude(geoAlts, geoUnit, gravity, altitudes, altUnit,
                          true);

    }

    /**
     * Converts from geopotential altitude to altitude.
     *
     * @param geoAlts           Geopotential altitudes in units of
     *                          <code>geoUnit</code>.
     * @param geoUnit           The unit of geopotential altitude.
     * @param gravity           Gravity.
     * @param altitudes         Altitudes in units of <code>altUnit</code>.
     *                          Its length shall be at least
     *                          <code>geoAlts.length</code>.  It may be the same
     *                          array as <code>geoAlts</code>.
     * @param altUnit           The unit of altitude.
     * @param copy              if true, a new copy of the array will be made
     * @return                  <code>altitudes</code> (as a convenience).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static double[] toAltitude(double[] geoAlts, Unit geoUnit,
                                      Real gravity, double[] altitudes,
                                      Unit altUnit, boolean copy)
            throws VisADException, RemoteException {

        altitudes = geoUnit.toThat(geoAlts,
                                   getGeopotentialUnit(altUnit, gravity),
                                   copy);
        double gVal = gravity.getValue();
        for (int i = 0; i < altitudes.length; i++) {
            altitudes[i] /= gVal;
        }
        return altitudes;
    }

    /**
     * Converts from geopotential altitude to altitude.
     *
     * @param geoAlts           Geopotential altitudes in units of
     *                          <code>geoUnit</code>.
     * @param geoUnit           The unit of geopotential altitude.
     * @param gravity           Gravity.
     * @param altitudes         Altitudes in units of <code>altUnit</code>.
     *                          Its length shall be at least
     *                          <code>geoAlts.length</code>.  It may be the same
     *                          array as <code>geoAlts</code>.
     * @param altUnit           The unit of altitude.
     * @return                  <code>altitudes</code> (as a convenience).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static float[] toAltitude(float[] geoAlts, Unit geoUnit,
                                     Real gravity, float[] altitudes,
                                     Unit altUnit)
            throws VisADException, RemoteException {

        return toAltitude(geoAlts, geoUnit, gravity, altitudes, altUnit,
                          true);
    }

    /**
     * Converts from geopotential altitude to altitude.
     *
     * @param geoAlts           Geopotential altitudes in units of
     *                          <code>geoUnit</code>.
     * @param geoUnit           The unit of geopotential altitude.
     * @param gravity           Gravity.
     * @param altitudes         Altitudes in units of <code>altUnit</code>.
     *                          Its length shall be at least
     *                          <code>geoAlts.length</code>.  It may be the same
     *                          array as <code>geoAlts</code>.
     * @param altUnit           The unit of altitude.
     * @param copy              if true, a new copy of the array will be made
     * @return                  <code>altitudes</code> (as a convenience).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static float[] toAltitude(float[] geoAlts, Unit geoUnit,
                                     Real gravity, float[] altitudes,
                                     Unit altUnit, boolean copy)
            throws VisADException, RemoteException {

        altitudes = geoUnit.toThat(geoAlts,
                                   getGeopotentialUnit(altUnit, gravity),
                                   copy);
        float gVal = (float) gravity.getValue();
        for (int i = 0; i < altitudes.length; i++) {
            altitudes[i] /= gVal;
        }
        return altitudes;
    }

    /**
     * Converts from altitude to geopotential altitude.
     *
     * @param altitudes         Altitudes in units of <code>altUnit</code>.
     * @param altUnit           The unit of altitude.
     * @param gravity           Gravity.
     * @param geoAlts           Geopotential altitudes in units of
     *                          <code>geoUnit</code>.
     *                          Its length shall be at least
     *                          <code>altitudes.length</code>.  It may be the
     *                          same array as <code>altitudes</code>.
     * @param geoUnit           The unit of geopotential altitude.
     * @return                  <code>geoAlts</code> (as a convenience).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static double[] toGeopotentialAltitude(double[] altitudes,
            Unit altUnit, Real gravity, double[] geoAlts, Unit geoUnit)
            throws VisADException, RemoteException {

        return toGeopotentialAltitude(altitudes, altUnit, gravity, geoAlts,
                                      geoUnit, true);
    }

    /**
     * Converts from altitude to geopotential altitude.
     *
     * @param altitudes         Altitudes in units of <code>altUnit</code>.
     * @param altUnit           The unit of altitude.
     * @param gravity           Gravity.
     * @param geoAlts           Geopotential altitudes in units of
     *                          <code>geoUnit</code>.
     *                          Its length shall be at least
     *                          <code>altitudes.length</code>.  It may be the
     *                          same array as <code>altitudes</code>.
     * @param geoUnit           The unit of geopotential altitude.
     * @param copy              if true, a new copy of the array will be made
     * @return                  <code>geoAlts</code> (as a convenience).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static double[] toGeopotentialAltitude(double[] altitudes,
            Unit altUnit, Real gravity, double[] geoAlts, Unit geoUnit,
            boolean copy)
            throws VisADException, RemoteException {

        System.arraycopy(geoUnit.toThis(altitudes,
                                        getGeopotentialUnit(altUnit,
                                            gravity), copy), 0, geoAlts, 0,
                                                altitudes.length);

        return geoAlts;
    }

    /**
     * Converts from altitude to geopotential altitude.
     *
     * @param altitudes         Altitudes in units of <code>altUnit</code>.
     * @param altUnit           The unit of altitude.
     * @param gravity           Gravity.
     * @param geoAlts           Geopotential altitudes in units of
     *                          <code>geoUnit</code>.
     *                          Its length shall be at least
     *                          <code>altitudes.length</code>.  It may be the
     *                          same array as <code>altitudes</code>.
     * @param geoUnit           The unit of geopotential altitude.
     * @return                  <code>geoAlts</code> (as a convenience).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static float[] toGeopotentialAltitude(float[] altitudes,
            Unit altUnit, Real gravity, float[] geoAlts, Unit geoUnit)
            throws VisADException, RemoteException {


        return toGeopotentialAltitude(altitudes, altUnit, gravity, geoAlts,
                                      geoUnit, true);
    }

    /**
     * Converts from altitude to geopotential altitude.
     *
     * @param altitudes         Altitudes in units of <code>altUnit</code>.
     * @param altUnit           The unit of altitude.
     * @param gravity           Gravity.
     * @param geoAlts           Geopotential altitudes in units of
     *                          <code>geoUnit</code>.
     *                          Its length shall be at least
     *                          <code>altitudes.length</code>.  It may be the
     *                          same array as <code>altitudes</code>.
     * @param geoUnit           The unit of geopotential altitude.
     * @param copy              if true, a new copy of the array will be made
     * @return                  <code>geoAlts</code> (as a convenience).
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @throws RemoteException  Java RMI failure.
     */
    public static float[] toGeopotentialAltitude(float[] altitudes,
            Unit altUnit, Real gravity, float[] geoAlts, Unit geoUnit,
            boolean copy)
            throws VisADException, RemoteException {

        System.arraycopy(geoUnit.toThis(altitudes,
                                        getGeopotentialUnit(altUnit,
                                            gravity)), 0, geoAlts, 0,
                                                altitudes.length);

        return geoAlts;
    }

    /**
     * Returns the standard geopotential altitude unit corresponding to an
     * altitude unit.
     *
     * @param altUnit           A unit of altitude (e.g. m)
     * @return                  The standard geopotential altitude unit
     *                          corresponding to <code>altUnit</code> (e.g.
     *                          gpm).
     * @throws VisADException   VisAD failure.
     */
    public static Unit getGeopotentialUnit(Unit altUnit)
            throws VisADException {

        Unit result = null;

        try {
            result = getGeopotentialUnit(altUnit, Gravity.newReal());
        } catch (UnitException e) {}  // can't happen
        catch (TypeException e) {}    // can't happen
        catch (RemoteException e) {}  // can't happen

        return result;
    }

    /**
     * Returns the geopotential altitude unit corresponding to an altitude
     * unit and gravity.
     *
     * @param altUnit           A unit of altitude (e.g. meter).
     * @param gravity           The value of gravity.
     * @return                  The geopotential altitude unit corresponding
     *                          to <code>altUnit</code> (e.g. geopotential
     *                          meter).
     * @throws UnitException    Altitude unit not a unit of length.
     * @throws TypeException    <code>gravity</code> isn't gravity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static Unit getGeopotentialUnit(Unit altUnit, Real gravity)
            throws UnitException, TypeException, RemoteException,
                   VisADException {

        Util.vetType(Gravity.getRealType(), gravity);

        if ( !Unit.canConvert(altUnit, SI.meter)) {
            throw new UnitException("Altitude unit (" + altUnit
                                    + ") not a unit of length");
        }

        return ScaledUnit.create(gravity.getValue(),
                                 altUnit.multiply(gravity.getUnit()));
    }

    /**
     * Provides support for coordinate system transformations between
     * geopotential altitude and altitude.
     */
    public static final class GeopotentialCoordinateSystem extends CoordinateSystem {

        /**
         * Gravity.
         * @serial
         */
        private final Real gravity;

        /**
         * Constructs from a value for gravity, an altitude reference type, and
         * a geopotential altitude unit.
         *
         * @param gravity        A value for gravity.
         * @param reference      An altitude reference type.
         * @param unit           A geopotential altitude unit.
         * @throws VisADException if a core VisAD failure occurs.
         */
        public GeopotentialCoordinateSystem(Real gravity, RealType reference,
                                            Unit unit)
                throws VisADException {

            super(new RealTupleType(reference), new Unit[] { unit });

            this.gravity = gravity;
        }

        /**
         * Converts values from altitude to geopotential altitude.
         *
         * @param values      Altitude values.
         * @return            Corresponding geopotential altitude values.
         * @throws VisADException if a core VisAD failure occurs.
         */
        public double[][] fromReference(double[][] values)
                throws VisADException {

            try {
                GeopotentialAltitude.toGeopotentialAltitude(values[0],
                        getReferenceUnits()[0], gravity, values[0],
                        getCoordinateSystemUnits()[0]);
            } catch (RemoteException e) {
                throw new VisADException("Couldn't access remote data: " + e);
            }

            return values;
        }

        /**
         * Converts values from altitude to geopotential altitude.
         *
         * @param values      Altitude values.
         * @return            Corresponding geopotential altitude values.
         * @throws VisADException if a core VisAD failure occurs.
         */
        public float[][] fromReference(float[][] values)
                throws VisADException {

            try {
                GeopotentialAltitude.toGeopotentialAltitude(values[0],
                        getReferenceUnits()[0], gravity, values[0],
                        getCoordinateSystemUnits()[0]);
            } catch (RemoteException e) {
                throw new VisADException("Couldn't access remote data: " + e);
            }

            return values;
        }

        /**
         * Converts values from geopotential altitude to altitude.
         *
         * @param values      Geopotential altitude values.
         * @return            Corresponding altitude values.
         * @throws VisADException if a core VisAD failure occurs.
         */
        public double[][] toReference(double[][] values)
                throws VisADException {

            try {
                GeopotentialAltitude.toAltitude(values[0],
                        getCoordinateSystemUnits()[0], gravity, values[0],
                        getReferenceUnits()[0]);
            } catch (RemoteException e) {
                throw new VisADException("Couldn't access remote data: " + e);
            }

            return values;
        }

        /**
         * Converts values from geopotential altitude to altitude.
         *
         * @param values      Geopotential altitude values.
         * @return            Corresponding altitude values.
         * @throws VisADException if a core VisAD failure occurs.
         */
        public float[][] toReference(float[][] values) throws VisADException {

            try {
                GeopotentialAltitude.toAltitude(values[0],
                        getCoordinateSystemUnits()[0], gravity, values[0],
                        getReferenceUnits()[0]);
            } catch (RemoteException e) {
                throw new VisADException("Couldn't access remote data: " + e);
            }

            return values;
        }

        /**
         * Indicates if this instance equals an object.
         *
         * @param object        The object.
         *
         * @return              True if and only if this instance equals the
         *                      object.
         */
        public boolean equals(Object object) {

            boolean equals;

            if ( !(object instanceof GeopotentialCoordinateSystem)) {
                equals = false;
            } else {
                GeopotentialCoordinateSystem that =
                    (GeopotentialCoordinateSystem) object;

                equals = gravity.equals(that.gravity);
            }

            return equals;
        }
    }
}
