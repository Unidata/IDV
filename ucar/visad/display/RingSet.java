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

package ucar.visad.display;


import ucar.visad.quantities.*;

import visad.*;

import java.awt.Color;

import java.rmi.RemoteException;



import java.util.Arrays;


/**
 * Provides support for a set of evenly-spaced, concentric rings (ie: a
 * "bullseye").
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.13 $
 */
public class RingSet extends PolarLineDrawing {

    /**
     * Constructs from a name and a vector space. Rings are white.
     * @param name              The name for this instance.
     * @param polarType         The vector space for this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RingSet(String name, RealTupleType polarType)
            throws RemoteException, VisADException {
        this(name, polarType, Color.white);
    }

    /**
     * Constructs from a name and a vector space and Color.
     * @param name              The name for this instance.
     * @param polarType         The vector space for this instance.
     * @param color             ring color
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RingSet(String name, RealTupleType polarType, Color color)
            throws RemoteException, VisADException {
        super(name, polarType);
        setColor(color);
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected RingSet(RingSet that) throws RemoteException, VisADException {
        super(that);
    }

    /**
     * Sets the distances of the rings from a set of evenly-spaced values.
     * @param ringValues        The distances for the rings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRingValues(Linear1DSet ringValues)
            throws VisADException, RemoteException {

        RealType realType =
            (RealType) ((SetType) ringValues.getType()).getDomain()
                .getComponent(0);
        Unit unit = ringValues.getSetUnits()[0];

        setRingValues(new Real(realType, ringValues.getStep(), unit),
                      new Real(realType, ringValues.getHiX(), unit));
    }

    /**
     * Sets the distances of the rings from an interval and a maximum distance.
     * @param rangeInc          The ring interval.
     * @param rangeMax          The maximum distance to be displayed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRingValues(Real rangeInc, Real rangeMax)
            throws VisADException, RemoteException {

        RealType rangeType = getRangeType();
        Unit     unit      = rangeType.getDefaultUnit();
        double   inc       = rangeInc.getValue(unit);
        double   max       = rangeMax.getValue(unit);

        setData(newUnionSet(getPolarType(),
                            new Linear1DSet(rangeType, inc, max,
                                            (int) Math.round(max / inc))));
    }

    /**
     * Creates a set of rings from a vector space and a set of distances.
     * @param polarType         The polar vector-space.
     * @param values            The set of distances.
     * @return                  The set of rings.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static SampledSet newUnionSet(RealTupleType polarType,
                                         Gridded1DSet values)
            throws VisADException, RemoteException {

        /*
         * 2000-01-06:
         *      This code is complicated by the fact that UnionSet requires
         *      the input sets to have the same type as the final set.
         *
         *      ProductSet is not used becuase it's displayed as a set of points
         *      rather than as a curve.
         */
        int          bearingIndex = getBearingIndex(polarType);
        int          rangeIndex   = 1 - bearingIndex;
        ;
        int          ringCount    = values.getLength(0);
        SampledSet[] rings        = new SampledSet[ringCount];
        float[][]    samples      = {
            new float[361], new float[361]
        };
        float[]      bearings     = samples[bearingIndex];

        for (int j = bearings.length; --j >= 0; ) {
            bearings[j] = j;
        }

        float[] ranges = samples[rangeIndex];
        Unit[]  units  = new Unit[2];

        units[bearingIndex] = CommonUnit.degree;
        units[rangeIndex]   = values.getSetUnits()[0];

        int[] indexes = new int[1];

        for (int i = 0; i < ringCount; ++i) {
            indexes[0] = i;

            Arrays.fill(ranges, values.indexToValue(indexes)[0][0]);

            rings[i] = new Gridded2DSet(polarType, samples, ranges.length,
                                        (CoordinateSystem) null, units,
                                        (ErrorEstimate[]) null);
        }

        return new UnionSet(polarType, rings);
    }

    /**
     * Sets the ring distances of this instance from another instance.
     *
     * @param that              The other instance.
     * @throws TypeException    if a necessary VisAD {@link visad.RealType}
     *                          couldn't be created.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRingValues(RingSet that)
            throws TypeException, VisADException, RemoteException {

        if ( !getPolarType().equals(that.getPolarType())) {
            throw new TypeException(
                this.getClass().getName() + ".setRingValues(RingSet): "
                + "Type of other RingSet doesn't match this one");
        }

        setData(that.getData());
    }

    /**
     * Adds this instance's {@link visad.ScalarMap}s to a given
     * {@link visad.LocalDisplay}.  This implementation does nothing.
     *
     * @param display           The {@link visad.LocalDisplay} to which to
     *                          add this instance's {@link visad.ScalarMap}s.
     */
    public void addScalarMaps(LocalDisplay display) {}  // no op

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A semi-deep clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new RingSet(this);
    }
}
