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
 * Provides support for a Displayable comprising a set of Radials
 *
 * @author Don Murray
 * @version $Revision: 1.6 $
 */
public class Radials extends PolarLineDrawing {

    /**
     * Constructs from a Displayable name and the type of the polar
     * coordinate system.
     *
     * @param name              The name for the Displayable.
     * @param polarType         The type of the (rho, theta) polar coordinate
     *                          system.
     * @param color             color for the radials
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Radials(String name, RealTupleType polarType, Color color)
            throws RemoteException, VisADException {
        super(name, polarType);
        setColor(color);
    }

    /**
     * Constructs from another instance.
     *
     * @param that              The other instance.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Radials(Radials that) throws RemoteException, VisADException {
        super(that);
    }

    /**
     * Sets the radials according to the maximum distance from the center
     * and the increment (in degrees).
     *
     * @param maximum           The maximum distance from the center.
     * @param inc               The increment between radials
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRadials(Real maximum, double inc)
            throws VisADException, RemoteException {
        setRadials(new Real((RealType) maximum.getType(), 0,
                            maximum.getUnit()), maximum, inc);
    }

    /**
     * Sets the radials according to the maximum distance from the center
     * and the increment (in degrees).
     *
     * @param minimum           The minimum distance from the center.
     * @param maximum           The maximum distance from the center.
     * @param inc               The increment between radials
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRadials(Real minimum, Real maximum, double inc)
            throws VisADException, RemoteException {

        if (inc <= 0) {
            return;
        }
        int          numRadials = (int) (360. / inc);
        SampledSet[] hairs      = new SampledSet[numRadials];
        float[][]    samples    = new float[2][];
        float max = (float) maximum.getValue(getRangeType().getDefaultUnit());
        float min = (float) minimum.getValue(getRangeType().getDefaultUnit());

        samples[getRangeIndex()] = new float[] { min, max };

        float[] bearings    = new float[2];
        Unit    bearingUnit = getBearingType().getDefaultUnit();

        samples[getBearingIndex()] = bearings;

        for (int i = 0; i < numRadials; i++) {
            Arrays.fill(bearings,
                        (float) bearingUnit.toThis(i * inc,
                            CommonUnit.degree));

            hairs[i] = new Gridded2DSet(getPolarType(), samples,
                                        bearings.length);
        }

        setData(new UnionSet(getPolarType(), hairs));
    }

    /**
     * Sets the radials according to another Radials.
     *
     * @param radials       The other radials
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setRadials(Radials radials)
            throws VisADException, RemoteException {
        setData(radials.getData());
    }

    /**
     * Indicates if this Radials is semantically identical to another object.
     *
     * @param obj               The other object.
     *
     * @return                  <code>true</code> if and only if this Radials
     *                          is semantically identical to <code>obj</code>.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof Radials)) {
            equals = false;
        } else {
            Radials that = (Radials) obj;

            equals = (that == this) || super.equals(that);
        }

        return equals;
    }

    /**
     * Add ScalarMaps for the display.  This is a no-op.
     *
     * @param display    display to add to
     */
    public void addScalarMaps(LocalDisplay display) {}  // no op

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     *
     * @return                  A semi-deep clone of this instance.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new Radials(this);
    }
}
