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

package ucar.visad.physics;


import ucar.visad.*;

import visad.*;



import java.util.WeakHashMap;


/**
 * Provides support for polar coordinates.  The coordinate system tuple
 * is (magnitude, angle) (i.e. (rho, theta)).
 *
 * Instances are immutable.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2006/03/17 17:08:52 $
 */
public class PolarCoordinateSystem extends visad.CoordinateSystem {

    /*
     * Inner classes:
     */

    /**
     * Class Key
     *
     *
     * @author
     * @version %I%, %G%
     */
    private static class Key {

        /** _more_ */
        RealTupleType ref;

        /** _more_ */
        Unit magUnit;

        /** _more_ */
        Unit angUnit;

        /**
         * _more_
         *
         * @param ref
         * @param magUnit
         * @param angUnit
         *
         */
        public Key(RealTupleType ref, Unit magUnit, Unit angUnit) {

            this.ref     = ref;
            this.magUnit = magUnit;
            this.angUnit = angUnit;
        }

        /**
         * _more_
         *
         * @param obj
         * @return
         */
        public boolean equals(Object obj) {

            boolean equals;

            if ( !(obj instanceof Key)) {
                equals = false;
            } else {
                Key that = (Key) obj;

                equals = (this == that)
                         || (ref.equals(that.ref)
                             && magUnit.equals(that.magUnit)
                             && angUnit.equals(that.angUnit));
            }

            return equals;
        }

        /**
         * _more_
         * @return
         */
        public int hashCode() {
            return ref.hashCode() ^ magUnit.hashCode() ^ angUnit.hashCode();
        }
    }

    /*
     * Fields:
     */

    /** _more_ */
    private Unit refXUnit;

    /** _more_ */
    private Unit refYUnit;

    /** _more_ */
    private Unit magUnit;

    /** _more_ */
    private Unit angUnit;

    /** _more_ */
    private static WeakHashMap map = new WeakHashMap();

    /**
     * Constructs from a reference coordinate system and units for magnitude and
     * angle.  This constructor is protected to promote use of the {@link
     * #instance} method because immutable classes shouldn't have public
     * constructors, IMHO.
     *
     * @param ref               The reference coordinate system.  The assumed
     *                          order is (x,y).
     * @param magUnit           The magnitude unit for this coordinate system.
     * @param angUnit           The angular unit for this coordinate system.
     *                          It must be convertible with radians.
     * @throws UnitException    The angular unit is not convertible with
     *                          radians.
     * @throws VisADException   VisAD failure.
     * @see visad.CoordinateSystem#CoordinateSystem(RealTupleType, Unit[])
     */
    protected PolarCoordinateSystem(RealTupleType ref, Unit magUnit,
                                    Unit angUnit)
            throws UnitException, VisADException {

        super(ref, new Unit[] { magUnit, angUnit });

        Unit[] refUnits = ref.getDefaultUnits();

        refXUnit     = refUnits[0];
        refYUnit     = refUnits[1];
        this.magUnit = magUnit;
        this.angUnit = angUnit;

        if ( !Unit.canConvert(refXUnit, refYUnit)) {
            throw new CoordinateSystemException(getClass().getName()
                    + ".<init>(): Reference X-unit (" + refXUnit
                    + ") not convertible with reference Y-unit (" + refYUnit
                    + ")");
        }

        if ( !Unit.canConvert(magUnit, refXUnit)) {
            throw new CoordinateSystemException(getClass().getName()
                    + ".<init>(): Magnitude unit (" + magUnit
                    + ") not convertible with reference X-unit (" + refXUnit
                    + ")");
        }

        if ( !Unit.canConvert(angUnit, PlaneAngle.RADIAN)) {
            throw new CoordinateSystemException(getClass().getName()
                    + ".<init>(): " + "Plane-angle unit (" + angUnit
                    + ") not convertible with radians");
        }

        synchronized (map) {
            map.put(new Key(ref, magUnit, angUnit), this);
        }
    }

    /*
     * Class methods:
     */

    /**
     * Returns an instance of this class given a reference RealTupleType and
     * units.  This method uses a {@link WeakHashMap} to reduce construction
     * and memory costs.
     *
     * @param ref               The reference coordinate system.  The assumed
     *                          order is (x,y).
     * @param magUnit           The magnitude unit for this coordinate system.
     * @param angUnit           The angular unit for this coordinate system.
     *                          It must be convertible with radians.
     * @return                  An instance of this class.
     * @throws UnitException    The angular unit is not convertible with
     *                          radians.
     * @throws VisADException   VisAD failure.
     * @see #PolarCoordinateSystem(RealTupleType, Unit, Unit)
     */
    public static PolarCoordinateSystem instance(RealTupleType ref,
            Unit magUnit, Unit angUnit)
            throws UnitException, VisADException {

        PolarCoordinateSystem coordSys;

        synchronized (map) {
            Object key = new Key(ref, magUnit, angUnit);

            coordSys = (PolarCoordinateSystem) map.get(key);

            if (coordSys == null) {
                coordSys = new PolarCoordinateSystem(ref, magUnit, angUnit);
            }
        }

        return coordSys;
    }

    /*
     * Instance methods:
     */

    /**
     * Transforms points in this coordinate system to the reference coordinate
     * system.
     *
     * @param values            The points in this coordinate system.
     *                          <code>values[0][i]</code> and
     *                          <code>values[1][i]</code> are the magnitude
     *                          and angle of the <code>i</code>-th point,
     *                          respectively.  This array may be overwritten by
     *                          this method.
     * @return                  The input points transformed to the reference
     *                          coordinate system.  Element <code>[0][k]</code>
     *                          and <code>[1][k]</code> are the X and Y
     *                          components of the <code>i</code>-th point,
     *                          respectively.
     */
    public float[][] toReference(float[][] values) {

        try {
            float[] mags = values[0];
            float[] angs = PlaneAngle.RADIAN.toThis(values[1], angUnit);
            float[] xs   = mags;
            float[] ys   = angs;

            for (int i = 0; i < mags.length; ++i) {
                float mag = mags[i];
                float ang = angs[i];

                xs[i] = (float) (mag * Math.cos(ang));
                ys[i] = (float) (mag * Math.sin(ang));
            }

            values[0] = refXUnit.toThis(xs, magUnit);
            values[1] = refYUnit.toThis(ys, magUnit);
        } catch (UnitException e) {}  // can't happen because conversions checked during construction

        return values;
    }

    /**
     * Transforms points in this coordinate system to the reference coordinate
     * system.
     *
     * @param values            The points in this coordinate system.
     *                          <code>values[0][i]</code> and
     *                          <code>values[1][i]</code> are the magnitude
     *                          and angle of the <code>i</code>-th point,
     *                          respectively.  This array may be overwritten by
     *                          this method.
     * @return                  The input points transformed to the reference
     *                          coordinate system.  Element <code>[0][k]</code>
     *                          and <code>[1][k]</code> are the X and Y
     *                          components of the <code>i</code>-th point,
     *                          respectively.
     */
    public double[][] toReference(double[][] values) {

        try {
            double[] mags = values[0];
            double[] angs = PlaneAngle.RADIAN.toThis(values[1], angUnit);
            double[] xs   = mags;
            double[] ys   = angs;

            for (int i = 0; i < mags.length; ++i) {
                double mag = mags[i];
                double ang = angs[i];

                xs[i] = (double) (mag * Math.cos(ang));
                ys[i] = (double) (mag * Math.sin(ang));
            }

            values[0] = refXUnit.toThis(xs, magUnit);
            values[1] = refYUnit.toThis(ys, magUnit);
        } catch (UnitException e) {}  // can't happen because conversions checked during construction

        return values;
    }

    /**
     * Transforms points in the reference coordinate system to this coordinate
     * system.
     *
     * @param values            The points in the reference coordinate
     *                          system. <code>values[0][i]</code> and
     *                          <code>values[1][i]</code> are the X and Y
     *                          components of the <code>i</code>-th point,
     *                          respectively.  This array may be overwritten by
     *                          this method.
     * @return                  The input points transformed to this coordinate
     *                          system.  Element <code>[0][k]</code> and
     *                          <code>[1][k]</code> are the magnitude and angle
     *                          of the <code>i</code>-th point, respectively.
     */
    public float[][] fromReference(float[][] values) {

        try {
            float[] xs   = magUnit.toThis(values[0], refXUnit);
            float[] ys   = magUnit.toThis(values[1], refYUnit);
            float[] mags = xs;
            float[] angs = ys;

            for (int i = 0; i < xs.length; ++i) {
                float x = xs[i];
                float y = ys[i];

                angs[i] = (float) Math.atan2(y, x);
                mags[i] = (float) Math.sqrt(x * x + y * y);
            }

            values[0] = mags;
            values[1] = angUnit.toThis(angs, PlaneAngle.RADIAN);
        } catch (UnitException e) {}  // can't happen because conversions checked during construction

        return values;
    }

    /**
     * Transforms points in the reference coordinate system to this coordinate
     * system.
     *
     * @param values            The points in the reference coordinate
     *                          system. <code>values[0][i]</code> and
     *                          <code>values[1][i]</code> are the X and Y
     *                          components of the <code>i</code>-th point,
     *                          respectively.  This array may be overwritten by
     *                          this method.
     * @return                  The input points transformed to this coordinate
     *                          system.  Element <code>[0][k]</code> and
     *                          <code>[1][k]</code> are the magnitude and angle
     *                          of the <code>i</code>-th point, respectively.
     */
    public double[][] fromReference(double[][] values) {

        try {
            double[] xs   = magUnit.toThis(values[0], refXUnit);
            double[] ys   = magUnit.toThis(values[1], refYUnit);
            double[] mags = xs;
            double[] angs = ys;

            for (int i = 0; i < xs.length; ++i) {
                double x = xs[i];
                double y = ys[i];

                angs[i] = (double) Math.atan2(y, x);
                mags[i] = (double) Math.sqrt(x * x + y * y);
            }

            values[0] = mags;
            values[1] = angUnit.toThis(angs, PlaneAngle.RADIAN);
        } catch (UnitException e) {}  // can't happen because conversions checked during construction

        return values;
    }

    /**
     * Indicates if this instance is semantically identical to anonther object.
     *
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semantically identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof PolarCoordinateSystem)) {
            equals = false;
        } else {
            PolarCoordinateSystem that = (PolarCoordinateSystem) obj;

            equals = (this == that)
                     || (refXUnit.equals(that.refXUnit)
                         && refYUnit.equals(that.refYUnit)
                         && magUnit.equals(that.magUnit)
                         && angUnit.equals(that.angUnit));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.  If {@link #equals(Object obj)},
     * then <code>{@link #hashCode()} == obj.hashCode()</code>.
     *
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return refXUnit.hashCode() ^ refYUnit.hashCode() ^ magUnit.hashCode()
               ^ angUnit.hashCode();
    }
}
