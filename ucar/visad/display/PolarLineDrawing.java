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


/**
 * Provides support for lines drawing in a polar coordinate system.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $
 */
public abstract class PolarLineDrawing extends LineDrawing {

    /**
     * The vector space.
     */
    private RealTupleType polarType;

    /**
     * The index of the range element.
     */
    private int rangeIndex;

    /**
     * The index of the bearing element.
     */
    private int bearingIndex;

    /**
     * Constructs from a name and a vector space.
     * @param name              The name for the instance.
     * @param polarType         The vector space.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected PolarLineDrawing(String name, RealTupleType polarType)
            throws RemoteException, VisADException {

        super(name);

        this.polarType = polarType;
        bearingIndex   = getBearingIndex(polarType);
        rangeIndex     = 1 - bearingIndex;
    }

    /**
     * Constructs from another instance.  The following attributes are set from
     * the other instance: the vector space.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected PolarLineDrawing(PolarLineDrawing that)
            throws RemoteException, VisADException {

        super(that);

        polarType    = that.polarType;  // immutable object
        rangeIndex   = that.rangeIndex;
        bearingIndex = that.bearingIndex;
    }

    /**
     * Gets the vector space of this instance.
     * @return                  The vector space of this instance.
     */
    protected final RealTupleType getPolarType() {
        return polarType;
    }

    /**
     * Gets the index of the range element.
     * @return                  The index of the range element.
     */
    protected final int getRangeIndex() {
        return rangeIndex;
    }

    /**
     * Gets the index of the bearing element.
     * @return                  The index of the bearing element.
     */
    protected final int getBearingIndex() {
        return bearingIndex;
    }

    /**
     * Gets the index of the bearing element of a vector space.
     * @param polarType         The vector space.
     * @return                  The index of the bearing element.
     * @throws VisADException   VisAD failure.
     */
    public static int getBearingIndex(RealTupleType polarType)
            throws VisADException {

        int bearingIndex = -1;

        for (int i = 0; i < 2; ++i) {
            if (Unit.canConvert(
                    CommonUnit.degree,
                    ((RealType) polarType.getComponent(
                        i)).getDefaultUnit())) {
                bearingIndex = i;
            }
        }

        return bearingIndex;
    }

    /**
     * Gets the RealType of the range element.
     * @return                  The RealType of the range element of this
     *                          instance.
     * @throws VisADException   VisAD failure.
     */
    protected final RealType getRangeType() throws VisADException {
        return (RealType) getPolarType().getComponent(getRangeIndex());
    }

    /**
     * Gets the RealType of the bearing element.
     * @return                  The RealType of the bearing element of this
     *                          instance.
     * @throws VisADException   VisAD failure.
     */
    protected final RealType getBearingType() throws VisADException {
        return (RealType) getPolarType().getComponent(getBearingIndex());
    }

    /**
     * Indicates if this instance is semantically identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semantically identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof PolarLineDrawing)) {
            equals = false;
        } else {
            PolarLineDrawing that = (PolarLineDrawing) obj;

            equals = (that == this)
                     || (polarType.equals(that.polarType)
                         && super.equals(that));
        }

        return equals;
    }

    /**
     * Gets the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return polarType.hashCode() ^ super.hashCode();
    }
}
