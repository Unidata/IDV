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


import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;

import visad.*;

import visad.CoordinateSystem;

import visad.Data;

import visad.Field;

import visad.FieldImpl;

import visad.FlatField;

import visad.FunctionType;

import visad.IrregularSet;

import visad.MathType;

import visad.RealTupleType;

import visad.RealType;

import visad.SampledSet;

import visad.Set;

import visad.SetType;

import visad.Unit;

import visad.VisADException;



import java.rmi.RemoteException;

import java.util.Arrays;


/**
 * The quantity of horizontal wind in vector coordinates that are relative to
 * an underlying grid.  Note that, in general, the X component of the wind won't
 * be in an eastward direction.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.14 $ $Date: 2006/12/05 19:04:33 $
 */
public final class GridRelativeHorizontalWind extends HorizontalWind {

    /** flag for whether there is new code */
    public static boolean doNewCode = true;

    /**
     * Constructs from the underlying grid.  The grid-relative X and Y
     * components are assumed to be in the direction of increasing first
     * and second dimensions, respectively. The {@link visad.MathType} of the
     * grid-relative X and Y components will be <code>xWindType(grid)</code> and
     * <code>yWindType(grid)</code>, respectively.
     *
     * @param grid                      The underlying grid.
     * @throws VisADException           if a VisAD failure occurs.
     * @throws IllegalArgumentException if <code>grid.getDimension() < 2</code>.
     *                                  or <code>grid.getManifoldDimension() <
     *                                  2</code>.
     * @throws NullPointerException     if the input grid is <code>null</code>.
     * @see #xWindType(SampledSet)
     * @see #yWindType(SampledSet)
     */
    public GridRelativeHorizontalWind(SampledSet grid) throws VisADException {
        super(new RealType[] { xWindType(grid), yWindType(grid) });
    }

    /**
     * Returns the {@link visad.RealType} for the <em>X</em> component of a
     * grid-relative wind.  The <em>X</em> component is in the direction of
     * increasing first dimension of the grid.  The name of the component will
     * be <em>X</em><code>_Wind_Component</code>, where <em>X</em> is the name
     * of the first dimension of the grid, and the default unit will be {@link
     * #DEFAULT_SPEED_UNIT}.
     *
     * @param grid                      The grid.
     * @return                          The {@link visad.RealType} of the X wind
     *                                  component.
     * @throws VisADException           if a VisAD failure occurs.
     */
    public static RealType xWindType(SampledSet grid) throws VisADException {
        return RealType.getRealType(xComponentName(grid), DEFAULT_SPEED_UNIT);
    }

    /**
     * Returns the {@link visad.RealType} for the <em>Y</em> component of a
     * grid-relative wind.  The <em>Y</em> component is in the direction of
     * increasing second dimension of the grid.  The name of the component will
     * be <em>Y</em><code>_Wind_Component</code>, where <em>Y</em> is the name
     * of the second dimension of the grid, and the default unit will be {@link
     * #DEFAULT_SPEED_UNIT}.
     *
     * @param grid                      The grid.
     * @return                          The {@link visad.RealType} of the Y wind
     *                                  component.
     * @throws VisADException           if a VisAD failure occurs.
     */
    public static RealType yWindType(SampledSet grid) throws VisADException {
        return RealType.getRealType(yComponentName(grid), DEFAULT_SPEED_UNIT);
    }

    /**
     * Returns the name for the <em>X</em> component of a grid-relative wind.
     * The <em>X</em> component is in the direction of increasing first
     * dimension of the grid.  The name of the component will be <code>
     * <em>X</em><code>_Wind_Component</code> where <em>X</em> is the name of
     * the first dimension of the grid.
     *
     * @param grid                      The grid.
     * @return
     * @throws VisADException           if a VisAD failure occurs.
     */
    private static String xComponentName(SampledSet grid)
            throws VisADException {
        return componentName(grid, 0);
    }

    /**
     * Returns the name for the <em>Y</em> component of a grid-relative wind.
     * The <em>Y</em> component is in the direction of increasing second
     * dimension of the grid.  The name of the component will be <code>
     * <em>Y</em><code>_Wind_Component</code> where <em>Y</em> is the name of
     * the second dimension of the grid.
     *
     * @param grid                      The grid.
     * @return
     * @throws IllegalArgumentException if <code>grid.getDimension() < 2</code>.
     * @throws VisADException           if a VisAD failure occurs.
     */
    private static String yComponentName(SampledSet grid)
            throws VisADException {
        return componentName(grid, 1);
    }

    /**
     * @param grid                      The grid.
     * @param i                         The index of the dimension.
     * @return
     * @throws IllegalArgumentException if <code>i < 0 || i >=
     *                                  <code>grid.getDimension()</code>.
     * @throws VisADException           if a VisAD failure occurs.
     */
    private static String componentName(SampledSet grid, int i)
            throws VisADException {

        if ((i < 0) || (i >= grid.getDimension())) {
            throw new IllegalArgumentException(grid.toString());
        }

        return ((RealType) ((SetType) grid.getType()).getDomain()
            .getComponent(i)).getName() + "_Wind_Component";
    }

    /**
     * Converts grid-relative winds to true (or absolute) winds.  The
     * U and V components of true wind are {@link WesterlyWind}
     * and {@link SoutherlyWind}, respectively.  If the input
     * {@link visad.Field} is not a time-series, then it must be a {@link
     * visad.FlatField} and it must be compatible with the argument of {@link
     * #cartesianHorizontalWind(FlatField)}.  If, however, the the
     * input {@link visad.Field} is a time-series, then its domain must be
     * a temporal {@link visad.Gridded1DSet} or a {@link visad.SingletonSet} and
     * its range values must be compatible with the argument of {@link
     * #cartesianHorizontalWind(FlatField)}.
     *
     * @param rel                       The grid-relative winds.
     * @return                          The time-series of true wind
     *                                  corresponding to the input.
     * @throws NullPointerException     if <code>rel</code> is
     *                                  <code>null</code>.
     * @throws IllegalArgumentException if the input field is not a time-series
     *                                  and is incompatible with {@link
     *                                  #cartesianHorizontalWind(FlatField)},
     *                                  or if the input field is a
     *                                  time-series but its range values
     *                                  are incompatible with {@link
     *                                  #cartesianHorizontalWind(FlatField)}.
     * @throws VisADException           if a VisAD failure occurs.
     * @throws RemoteException          if a Java RMI failure occurs.
     */
    public static Field cartesianHorizontalWind(Field rel)
            throws VisADException, RemoteException {

        RealTupleType domType = ((FunctionType) rel.getType()).getDomain();
        Field         result  = null;
        if (RealType.Time.equalsExceptNameButUnits(domType)
                || RealType.TimeInterval.equalsExceptNameButUnits(domType)) {
            result = timeSeriesCartesianHorizontalWind(rel);
        } else {
            result = cartesianHorizontalWind((FlatField) rel);
        }
        return result;

    }

    /**
     * Converts a time-series of grid-relative winds to a time-series of true
     * (or absolute) winds.
     * The U and V components of true wind are {@link WesterlyWind} and {@link
     * SoutherlyWind}, respectively.
     * The domain of the input {@link visad.Field} must be a temporal
     * {@link visad.Gridded1DSet} or a {@link visad.SingletonSet}.
     * The range values of the input {@link visad.Field} must be
     * {@link visad.FlatField}s. The domains of the range
     * {@link visad.FlatField}s must have a manifold dimension of two or
     * greater and they must have a
     * reference system which contains {@link visad.RealType#Latitude} and
     * {@link visad.RealType#Longitude}.
     * The number of components in the range of the {@link visad.FlatField}s
     * must be two.
     * Both components must have units convertible with {@link
     * #DEFAULT_SPEED_UNIT}.
     * The first and second components are assumed
     * to be the wind components in the direction of increasing first
     * and second manifold dimension indexes, respectively.
     * The domains of the {@link visad.FlatField}s must be equal.
     * The {@link visad.Field} returned by this method has the same domain as
     * the input {@link visad.Field}.
     * The range values of the returned {@link visad.Field} are
     * {@link visad.FlatField}s that have the same domain as the input
     * {@link visad.FlatField}s.
     * The {@link visad.MathType} of the range of the returned
     * {@link visad.FlatField}s will be
     * <code>CartesianHorizontalWind.getEarthVectorType()</code>.
     *
     * @param rel                       The time-series of grid-relative wind.
     * @return                          The time-series of true wind
     *                                  corresponding to the input.
     * @throws NullPointerException     if <code>rel</code> is
     *                                  <code>null</code>.
     * @throws IllegalArgumentException if the input field doesn't have a
     *                                  time-series domain,
     *                                  or if the range values aren't {@link
     *                                  visad.FlatField} with the same domain,
     *                                  or if the domain of the {@link
     *                                  visad.FlatField}s doesn't have a
     *                                  transformation
     *                                  to latitude and longitude,
     *                                  or if the domain is irregular or has
     *                                  too few points, or if the {@link
     *                                  visad.FlatField}s don't have two
     *                                  and only two components in their range,
     *                                  or if the default units of the {@link
     *                                  visad.FlatField}s range aren't equal.
     * @throws VisADException           if a VisAD failure occurs.
     * @throws RemoteException          if a Java RMI failure occurs.
     * @see CartesianHorizontalWind
     */
    public static Field timeSeriesCartesianHorizontalWind(Field rel)
            throws VisADException, RemoteException {

        FunctionType  outerFuncType = (FunctionType) rel.getType();
        RealTupleType outerDomType  = outerFuncType.getDomain();


        if ( !(RealType.Time.equalsExceptNameButUnits(outerDomType)
                || !RealType.TimeInterval.equalsExceptNameButUnits(
                    outerDomType))) {
            throw new IllegalArgumentException(outerDomType.toString());
        }

        MathType innerFuncType = outerFuncType.getRange();

        if ( !(innerFuncType instanceof FunctionType)) {
            throw new IllegalArgumentException(innerFuncType.toString());
        }

        Field innerField = (Field) rel.getSample(0);
        Set   innerDom   = innerField.getDomainSet();
        if (innerDom instanceof SingletonSet) {
            return rel;
        } else if (innerDom instanceof GriddedSet) {
            int[] lengths = ((GriddedSet) innerDom).getLengths();
            if ((lengths[0] == 1) && (lengths[1] == 1)) {
                return rel;
            }
        }

        // account for null units, assume m/sec
        Unit[] rangeUnits = innerField.getDefaultRangeUnits();
        if ((rangeUnits == null) || (rangeUnits[0] == null)
                || rangeUnits[0].isDimensionless()) {
            rangeUnits =
                CartesianHorizontalWind.getEarthVectorType()
                    .getDefaultUnits();
        }
        FunctionType innerType =
            new FunctionType(((SetType) innerDom.getType()).getDomain(),
                             CartesianHorizontalWind.getEarthVectorType());


        FlatField uvField = new FlatField(innerType, innerDom,
                                          (CoordinateSystem) null,
                                          (Set[]) null, rangeUnits);



        Field result = new FieldImpl(new FunctionType(outerDomType,
                           uvField.getType()), rel.getDomainSet());




        //System.out.println("making rHatField");
        Field rHatField = (doNewCode
                           ? hatFieldNew(innerDom, 0)
                           : hatFieldOld(innerDom, 0));
        //System.out.println("making sHatField");
        Field     sHatField = (doNewCode
                               ? hatFieldNew(innerDom, 1)
                               : hatFieldOld(innerDom, 1));

        float[][] rHats     = rHatField.getFloats(false);
        //ucar.unidata.util.Misc.printArray("rHats[0]", rHats[0]);
        //ucar.unidata.util.Misc.printArray("rHats[1]", rHats[1]);
        //System.out.println("\n");
        float[][] sHats = sHatField.getFloats(false);
        //ucar.unidata.util.Misc.printArray("sHats[0]", sHats[0]);
        //ucar.unidata.util.Misc.printArray("sHats[1]", sHats[1]);
        //System.out.println("\n");
        float[] us = new float[innerDom.getLength()];
        float[] vs = new float[us.length];



        for (int i = 0, n = rel.getLength(); i < n; i++) {
            if (i > 0) {
                innerField = (Field) rel.getSample(i);
                Set dom = innerField.getDomainSet();
                if ( !innerDom.equals(dom)) {
                    //System.out.println("new domain");
                    innerDom  = dom;
                    rHatField = (doNewCode
                                 ? hatFieldNew(innerDom, 0)
                                 : hatFieldOld(innerDom, 0));
                    sHatField = (doNewCode
                                 ? hatFieldNew(innerDom, 1)
                                 : hatFieldOld(innerDom, 1));

                    rHats     = rHatField.getFloats(false);
                    sHats     = sHatField.getFloats(false);
                    /*
                    throw new IllegalArgumentException("template="
                            + innerDom.toString() + "; domain="
                            + dom.toString());
                    */
                }
                uvField = new FlatField(innerType, innerDom,
                                        (CoordinateSystem) null,
                                        (Set[]) null, rangeUnits);
                us = new float[innerDom.getLength()];
                vs = new float[us.length];
            }

            float[][] rsWinds = innerField.getFloats(false);
            float[]   rWinds  = rsWinds[0];
            float[]   sWinds  = rsWinds[1];
            //ucar.unidata.util.Misc.printArray("rWinds", rWinds);
            //System.out.println("\n");
            //ucar.unidata.util.Misc.printArray("sWinds", sWinds);
            //System.out.println("\n");

            for (int j = 0; j < us.length; j++) {
                us[j] = rWinds[j] * rHats[0][j] + sWinds[j] * sHats[0][j];
                vs[j] = rWinds[j] * rHats[1][j] + sWinds[j] * sHats[1][j];
            }
            //ucar.unidata.util.Misc.printArray("us", us);
            //System.out.println("\n");
            //ucar.unidata.util.Misc.printArray("vs", vs);
            //System.out.println("\n");

            uvField.setSamples(new float[][] {
                us, vs
            }, false);
            result.setSample(i, uvField, false);
        }

        return result;
    }

    /**
     * The returned {@link visad.FlatField} will have NaN-s for those unit
     * vector components that could not be computed.
     *
     * @param grid                      The spatial grid.
     * @param index                     The index of the manifold dimension
     *                                  along which to compute the unit vector.
     * @return                          A field of components of the unit vector
     *                                  for the given manifold dimension.
     * @throws NullPointerException     if the grid is <code>null</code>.
     * @throws IllegalArgumentException if the manifold dimension of the grid
     *                                  is less than 2 or if the grid doesn't
     *                                  contain {@link
     *                                  visad.RealType#Latitude} and {@link
     *                                  visad.RealType#Longitude}.
     * @throws VisADException           if a VisAD failure occurs.
     * @throws RemoteException          if a Java RMI failure occurs.
     */
    private static FlatField hatFieldNew(Set grid, int index)
            throws VisADException, RemoteException {

        CoordinateSystem cs    = grid.getCoordinateSystem();
        boolean          hasCS = cs != null;

        RealTupleType    rtt   = (hasCS)
                                 ? cs.getReference()
                                 : ((SetType) grid.getType()).getDomain();

        int              latI  = rtt.getIndex(RealType.Latitude);

        if (latI == -1) {
            throw new IllegalArgumentException(rtt.toString());
        }

        int lonI = rtt.getIndex(RealType.Longitude);
        if (lonI == -1) {
            throw new IllegalArgumentException(rtt.toString());
        }

        if (grid.getManifoldDimension() < 2) {
            throw new IllegalArgumentException(grid.toString());
        }

        int[][]         neighbors     = grid.getNeighbors(index);
        LatLonPointImpl refPt         = new LatLonPointImpl();
        LatLonPointImpl neiPt         = new LatLonPointImpl();
        Bearing         bearing       = new Bearing();
        float[]         hat1          = new float[2];
        float[]         hat2          = new float[2];
        float[][]       hat           = new float[2][grid.getLength()];


        float[][]       refCoords     = null;
        float[][]       neiCoords     = null;
        float[][]       domainSamples = grid.getSamples(false);

        refCoords = (hasCS)
                    ? cs.toReference(Set.copyFloats(domainSamples))
                    : domainSamples;
        // If the grid is lat/lon or has an IdentityCoordinateSystem
        // don't do the rotation
        //TODO:  handle rotated lat/lon grids
        if ( !hasCS || (refCoords == domainSamples)
                || (Arrays.equals(refCoords[latI], domainSamples[latI])
                    && Arrays.equals(refCoords[lonI], domainSamples[lonI]))) {
            if (index == 0) {
                Arrays.fill(hat[0], 1);
                Arrays.fill(hat[1], 0);
            } else {
                Arrays.fill(hat[0], 0);
                Arrays.fill(hat[1], 1);
            }
        } else {

            float latBefore, lonBefore, latAfter, lonAfter;
            //int backOffset = (index==0) ? -180 : 0;
            //int foreOffset = (index==0) ? 0 : -180;
            int backOffset = -180;
            int foreOffset = 0;
            for (int i = 0; i < neighbors.length; i++) {
                refPt.set(refCoords[latI][i], refCoords[lonI][i]);
                if ((neighbors[i][0] < 0)
                        || (neighbors[i][0] >= neighbors.length)) {
                    latBefore = Float.NaN;
                    lonBefore = Float.NaN;
                } else {
                    latBefore = refCoords[latI][neighbors[i][0]];
                    lonBefore = refCoords[lonI][neighbors[i][0]];
                }
                if ((neighbors[i][1] < 0)
                        || (neighbors[i][1] >= neighbors.length)) {
                    latAfter = Float.NaN;
                    lonAfter = Float.NaN;
                } else {
                    latAfter = refCoords[latI][neighbors[i][1]];
                    lonAfter = refCoords[lonI][neighbors[i][1]];
                }

                compute(refPt, neiPt, latBefore, lonBefore, backOffset,
                        bearing, hat1);

                float d1 = (float) bearing.getDistance();

                compute(refPt, neiPt, latAfter, lonAfter, foreOffset,
                        bearing, hat2);

                float   d2   = (float) bearing.getDistance();
                boolean bad1 = Double.isNaN(d1);
                boolean bad2 = Double.isNaN(d2);

                if (bad1 && bad2) {
                    hat[0][i] = Float.NaN;
                    hat[1][i] = Float.NaN;
                } else {
                    if (bad1) {
                        hat[0][i] = hat2[0];
                        hat[1][i] = hat2[1];
                    } else if (bad2) {
                        hat[0][i] = hat1[0];
                        hat[1][i] = hat1[1];
                    } else {
                        float tot  = d1 + d2;
                        float c1   = d2 / tot;
                        float c2   = d1 / tot;
                        float xhat = c1 * hat1[0] + c2 * hat2[0];
                        float yhat = c1 * hat1[1] + c2 * hat2[1];
                        float mag = (float) Math.sqrt(xhat * xhat
                                        + yhat * yhat);

                        hat[0][i] = xhat / mag;
                        hat[1][i] = yhat / mag;
                    }
                }

            }
        }


        FlatField hatField =
            new FlatField(
                new FunctionType(
                    ((SetType) grid.getType()).getDomain(),
                    new RealTupleType(
                        RealType.getRealType(
                            "xHat",
                            CommonUnit.dimensionless), RealType.getRealType(
                                "yHat", CommonUnit.dimensionless))), grid);


        hatField.setSamples(hat, false);
        return hatField;
    }


    /**
     * I have no idea what this does.
     *
     * @param grid   sampling grid
     * @param index  some sort of index
     * @return a new flat field with something different
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private static FlatField hatFieldOld(Set grid, int index)
            throws VisADException, RemoteException {

        CoordinateSystem cs    = grid.getCoordinateSystem();
        boolean          hasCS = (cs != null);

        RealTupleType    rtt   = (hasCS)
                                 ? cs.getReference()
                                 : ((SetType) grid.getType()).getDomain();

        int              latI  = rtt.getIndex(RealType.Latitude);

        if (latI == -1) {
            throw new IllegalArgumentException(grid.toString());
        }

        int lonI = rtt.getIndex(RealType.Longitude);

        if (lonI == -1) {
            throw new IllegalArgumentException(grid.toString());
        }

        if (grid.getManifoldDimension() < 2) {
            throw new IllegalArgumentException(grid.toString());
        }

        int[][]         neighbors = grid.getNeighbors(index);
        LatLonPointImpl refPt     = new LatLonPointImpl();
        LatLonPointImpl neiPt     = new LatLonPointImpl();
        Bearing         bearing   = new Bearing();
        float[]         hat1      = new float[2];
        float[]         hat2      = new float[2];
        float[][]       hat       = new float[2][grid.getLength()];

        for (int i = 0; i < neighbors.length; i++) {
            float[][] refCoords = grid.indexToValue(new int[] { i });
            if (hasCS) {
                refCoords = cs.toReference(refCoords);
            }

            float[][] neiCoords = grid.indexToValue(neighbors[i]);
            if (hasCS) {
                neiCoords = cs.toReference(neiCoords);
            }

            refPt.set(refCoords[latI][0], refCoords[lonI][0]);
            compute(refPt, neiPt, neiCoords[latI][0], neiCoords[lonI][0],
                    -180, bearing, hat1);

            float d1 = (float) bearing.getDistance();

            compute(refPt, neiPt, neiCoords[latI][1], neiCoords[lonI][1], 0,
                    bearing, hat2);

            float   d2   = (float) bearing.getDistance();
            boolean bad1 = Double.isNaN(d1);
            boolean bad2 = Double.isNaN(d2);

            if (bad1 && bad2) {
                hat[0][i] = Float.NaN;
                hat[1][i] = Float.NaN;
            } else {
                if (bad1) {
                    hat[0][i] = hat2[0];
                    hat[1][i] = hat2[1];
                } else if (bad2) {
                    hat[0][i] = hat1[0];
                    hat[1][i] = hat1[1];
                } else {
                    float tot  = d1 + d2;
                    float c1   = d2 / tot;
                    float c2   = d1 / tot;
                    float xhat = c1 * hat1[0] + c2 * hat2[0];
                    float yhat = c1 * hat1[1] + c2 * hat2[1];
                    float mag  = (float) Math.sqrt(xhat * xhat + yhat * yhat);

                    hat[0][i] = xhat / mag;
                    hat[1][i] = yhat / mag;
                }
            }
        }

        FlatField hatField =
            new FlatField(
                new FunctionType(
                    ((SetType) grid.getType()).getDomain(),
                    new RealTupleType(
                        RealType.getRealType(
                            "xHat",
                            CommonUnit.dimensionless), RealType.getRealType(
                                "yHat", CommonUnit.dimensionless))), grid);

        hatField.setSamples(hat, false);

        return hatField;
    }




    /**
     * @param refPt             The geodetic position of the reference point.
     * @param neiPt             The holder for the geodetic position of the
     *                          neighboring grid point.
     * @param lat               The latitude of the neighboring grid point.
     * @param lon               The longitude of the neighboring grid point.
     * @param azTerm            An amount in degrees to be added to the computed
     *                          azimuth.
     * @param bearing           The holder for the computed range and bearing
     *                          from the reference point to the neighboring
     *                          grid point.
     * @param hat               2-element output array for computed (x,y)
     *                          components of unit vector.
     */
    private static void compute(LatLonPointImpl refPt, LatLonPointImpl neiPt,
                                float lat, float lon, float azTerm,
                                Bearing bearing, float[] hat) {

        neiPt.set(lat, lon);
        Bearing.calculateBearing(refPt, neiPt, bearing);

        float az = (float) Math.toRadians(bearing.getAngle() + azTerm);
        //System.out.println("bearing.Angle = " + bearing.getAngle() + "; azTerm = " + azTerm+ "; result= " + Math.toDegrees(az));

        hat[0] = (float) Math.sin(az);
        hat[1] = (float) Math.cos(az);
    }

    /**
     * Converts grid-relative wind to true (or absolute) wind.  The U
     * and V components of true wind are {@link WesterlyWind} and {@link
     * SoutherlyWind}, respectively.  The domain of the input
     * {@link visad.FlatField} must have a manifold dimension of two or
     * greater and it must have a
     * reference system which contains {@link visad.RealType#Latitude} and
     * {@link visad.RealType#Longitude}.  The number
     * of components in the range of the input {@link visad.FlatField} must
     * be two.  Both components must have units convertible with {@link
     * #DEFAULT_SPEED_UNIT}.  The first and second components are assumed
     * to be the wind components in the direction of increasing first
     * and second manifold dimension indexes, respectively.  The {@link
     * visad.MathType} of the range of the returned {@link visad.FlatField}
     * will be <code>CartesianHorizontalWind.getEarthVectorType()</code> and
     * the domain will be the same as the input domain.
     *
     * @param rel                       The field of grid-relative wind.
     * @return                          The field of true wind corresponding
     *                                  to the input field.
     * @throws NullPointerException     if <code>rel</code> is
     *                                  <code>null</code>.
     * @throws IllegalArgumentException if the input field doesn't have two
     *                                  and only two components in its range,
     *                                  or if the default units of the input
     *                                  range aren't equal, or if the domain of
     *                                  the input field doesn't have a
     *                                  transformation
     *                                  to latitude and longitude, or the grid
     *                                  is irregular or has too few points.
     * @throws VisADException           if a VisAD failure occurs.
     * @throws RemoteException          if a Java RMI failure occurs.
     * @see CartesianHorizontalWind
     */
    public static FlatField cartesianHorizontalWind(FlatField rel)
            throws VisADException, RemoteException {

        FunctionType funcType  = (FunctionType) rel.getType();
        MathType     rangeType = funcType.getRange();

        if (rel.getRangeDimension() != 2) {
            throw new IllegalArgumentException(rangeType.toString());
        }

        Unit[] units = rel.getDefaultRangeUnits();

        if ( !units[0].equals(units[1])) {
            throw new IllegalArgumentException(units.toString());
        }

        SampledSet grid = (SampledSet) rel.getDomainSet();
        // check for single point grid
        if (grid instanceof SingletonSet) {
            return rel;
        } else if (grid instanceof GriddedSet) {
            int[] lengths = ((GriddedSet) grid).getLengths();
            if ((lengths[0] == 1) && (lengths[1] == 1)) {
                return rel;
            }
        }
        FlatField abs =
            new FlatField(
                new FunctionType(
                    funcType.getDomain(),
                    CartesianHorizontalWind.getEarthVectorType()), grid,
                        (CoordinateSystem[]) null, rel.getRangeSets(), units);

        abs.setSamples(trueWind(rel.getFloats(), grid), false);

        return abs;
    }

    /**
     *
     * @param gridWinds
     * @param grid
     * @return
     * @throws IllegalArgumentException if the grid is irregular or has too few
     *                                  points or has no {@link
     *                                  visad.CoordinateSystem}, or if the
     *                                  reference of the grid's {@link
     *                                  visad.CoordinateSystem} doesn't contain
     *                                  {@link visad.RealType.Latitude} and
     *                                  {@link visad.RealType.Longitude}.
     *
     * @throws VisADException
     */
    private static float[][] trueWind(float[][] gridWinds, SampledSet grid)
            throws VisADException {

        if (grid instanceof IrregularSet) {
            throw new IllegalArgumentException(grid.toString());
        }

        CoordinateSystem cs    = grid.getCoordinateSystem();
        boolean          hasCS = (cs != null);

        RealTupleType    rtt   = (hasCS)
                                 ? cs.getReference()
                                 : ((SetType) grid.getType()).getDomain();

        int              latI  = rtt.getIndex(RealType.Latitude);

        if (latI == -1) {
            throw new IllegalArgumentException(rtt.toString());
        }

        int lonI = rtt.getIndex(RealType.Longitude);

        if (lonI == -1) {
            throw new IllegalArgumentException(rtt.toString());
        }

        if (grid.getManifoldDimension() < 2) {
            throw new IllegalArgumentException(grid.toString());
        }

        float[] us = new float[grid.getLength()];
        float[] vs = new float[grid.getLength()];

        Arrays.fill(us, 0);
        Arrays.fill(vs, 0);
        addComponent(grid, gridWinds, cs, 0, latI, lonI, us, vs);
        addComponent(grid, gridWinds, cs, 1, latI, lonI, us, vs);

        return new float[][] {
            us, vs
        };
    }

    /**
     * @param grid                       The grid.
     * @param gridWinds                  The grid-relative winds.
     * @param cs                         The coordinate system transformation of
     *                                   the grid.
     * @param index                      The index of the grid-relative wind
     *                                   component.
     * @param latI                       The index of latitude in the reference
     *                                   coordinate system.
     * @param lonI                       The index of longitude in the reference
     *                                   coordinate system.
     * @param us                         The array in which to add the computed
     *                                   U-component of the wind.
     * @param us                         The array in which to add the computed
     *                                   V-component of the wind.
     * @param vs
     * @throws IndexOutOfBoundsException if <code>gridWinds</code>, <code>us
     *                                   </code>, or <code>vs</code> is too
     *                                   small.
     * @throws VisADException            if a VisAD failure occurs.
     */
    private static void addComponent(SampledSet grid, float[][] gridWinds,
                                     CoordinateSystem cs, int index,
                                     int latI, int lonI, float[] us,
                                     float[] vs)
            throws VisADException {

        int[][]         neighbors     = grid.getNeighbors(index);
        LatLonPointImpl refPt         = new LatLonPointImpl();
        LatLonPointImpl neiPt         = new LatLonPointImpl();
        Bearing         bearing       = new Bearing();
        float[]         uv1           = new float[2];
        float[]         uv2           = new float[2];
        boolean         hasCS         = cs != null;
        float[][]       domainSamples = grid.getSamples(false);
        float[][]       crefCoords    = (hasCS)
                                        ? cs.toReference(
                                            Set.copyFloats(domainSamples))
                                        : domainSamples;
        // If the grid is lat/lon or has an IdentityCoordinateSystem
        // don't do the rotation
        //TODO:  handle rotated lat/lon grids
        if ( !hasCS || (crefCoords == domainSamples)
                || (Arrays.equals(crefCoords[latI], domainSamples[latI])
                    && Arrays.equals(crefCoords[lonI],
                                     domainSamples[lonI]))) {
            //us = gridWinds[0];
            //vs = gridWinds[1];
            System.arraycopy(gridWinds[0], 0, us, 0, us.length);
            System.arraycopy(gridWinds[1], 0, vs, 0, vs.length);
        } else {

            for (int i = 0; i < neighbors.length; i++) {

                float[][] refCoords = grid.indexToValue(new int[] { i });
                if (hasCS) {
                    refCoords = cs.toReference(refCoords);
                }

                float[][] neiCoords = grid.indexToValue(neighbors[i]);
                if (hasCS) {
                    neiCoords = cs.toReference(neiCoords);
                }

                refPt.set(refCoords[latI][0], refCoords[lonI][0]);

                compute(refPt, neiPt, neiCoords[latI][0], neiCoords[lonI][0],
                        -180, gridWinds[index][i], bearing, uv1);

                float d1 = (float) bearing.getDistance();

                compute(refPt, neiPt, neiCoords[latI][1], neiCoords[lonI][1],
                        0, gridWinds[index][i], bearing, uv2);

                float   d2   = (float) bearing.getDistance();
                boolean bad1 = Double.isNaN(d1);
                boolean bad2 = Double.isNaN(d2);

                if (bad1 && bad2) {
                    us[i] = Float.NaN;
                    vs[i] = Float.NaN;
                } else {
                    if (bad1) {
                        us[i] += uv2[0];
                        vs[i] += uv2[1];
                    } else if (bad2) {
                        us[i] += uv1[0];
                        vs[i] += uv1[1];
                    } else {
                        float tot = d1 + d2;
                        float c1  = d2 / tot;
                        float c2  = d1 / tot;

                        us[i] += c1 * uv1[0] + c2 * uv2[0];
                        vs[i] += c1 * uv1[1] + c2 * uv2[1];
                    }
                }
            }
        }
    }

    /**
     * @param refPt             The geodetic position of the reference point.
     * @param neiPt             The holder for the geodetic position of the
     *                          neighboring grid point.
     * @param lat               The latitude of the neighboring grid point.
     * @param lon               The longitude of the neighboring grid point.
     * @param azTerm            An amount in degrees to be added to the computed
     *                          azimuth.
     * @param wind              Magnitude of grid-relative wind component.
     * @param bearing           The holder for the computed range and bearing
     *                          from the reference point to the neighboring
     *                          grid point.
     * @param uv                2-element output array for computed (U,V) wind
     *                          components.
     */
    private static void compute(LatLonPointImpl refPt, LatLonPointImpl neiPt,
                                float lat, float lon, float azTerm,
                                float wind, Bearing bearing, float[] uv) {

        neiPt.set(lat, lon);
        Bearing.calculateBearing(refPt, neiPt, bearing);

        float az   = (float) Math.toRadians(bearing.getAngle() + azTerm);
        float xhat = (float) Math.sin(az);
        float yhat = (float) Math.cos(az);

        uv[0] = xhat * wind;
        uv[1] = yhat * wind;
    }
}
