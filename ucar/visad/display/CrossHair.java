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

import java.rmi.RemoteException;



import java.util.Arrays;


/**
 * Provides support for a Displayable comprising a cross-hair.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.11 $
 */
public class CrossHair extends PolarLineDrawing {

    /**
     * Constructs from a Displayable name and the type of the polar
     * coordinate system.
     * @param name              The name for the Displayable.
     * @param polarType         The type of the (rho, theta) polar coordinate
     *                          system.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public CrossHair(String name, RealTupleType polarType)
            throws RemoteException, VisADException {
        super(name, polarType);
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected CrossHair(CrossHair that)
            throws RemoteException, VisADException {
        super(that);
    }

    /**
     * Sets the cross-hairs according to the maximum distance from the center.
     * @param maximum           The maximum distance from the center.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setHairs(Real maximum)
            throws VisADException, RemoteException {

        /*
         * 2000-01-06:
         *      ProductSet isn't used because it's displayed as points rather
         *      than as a line.
         */
        SampledSet[] hairs   = new SampledSet[2];
        float[][]    samples = new float[2][];
        float max = (float) maximum.getValue(getRangeType().getDefaultUnit());

        samples[getRangeIndex()] = new float[] { -max, max };

        float[] bearings    = new float[2];
        Unit    bearingUnit = getBearingType().getDefaultUnit();

        samples[getBearingIndex()] = bearings;

        Arrays.fill(bearings,
                    (float) bearingUnit.toThis(0, CommonUnit.degree));

        hairs[0] = new Gridded2DSet(getPolarType(), samples, bearings.length);

        Arrays.fill(bearings,
                    (float) bearingUnit.toThis(90, CommonUnit.degree));

        hairs[1] = new Gridded2DSet(getPolarType(), samples, bearings.length);

        setData(new UnionSet(getPolarType(), hairs));
    }

    /**
     * Sets the cross-hairs according to another CrossHair.
     * @param crossHair         The other cross-hair.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setHairs(CrossHair crossHair)
            throws VisADException, RemoteException {
        setData(crossHair.getData());
    }

    /**
     * Indicates if this CrossHair is semantically identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this CrossHair
     *                          is semantically identical to <code>obj</code>.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof CrossHair)) {
            equals = false;
        } else {
            CrossHair that = (CrossHair) obj;

            equals = (that == this) || super.equals(that);
        }

        return equals;
    }

    /**
     * Adds this instance's {@link visad.ScalarMap}s to a display.  This
     * implementation does nothing.
     *
     * @param display           The display to which to add the ScalarMap-s.
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
        return new CrossHair(this);
    }
}
