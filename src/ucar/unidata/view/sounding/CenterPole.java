/*
 * $Id: CenterPole.java,v 1.18 2005/05/13 18:33:25 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.sounding;



import java.rmi.RemoteException;

import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;


/**
 * Provides support for the center pole of a 3D wind hodograph.
 *
 * @author Steven R. Emmerson
 * @version $Id: CenterPole.java,v 1.18 2005/05/13 18:33:25 jeffmc Exp $
 */
public class CenterPole extends LineDrawing {

    /**
     * The set of points constituting the pole.
     */
    private Linear1DSet poleSet;

    /**
     * The type of the vertical dimension.
     */
    private RealType zType;

    /**
     * Constructs from a name for the displayable and the type of the vertical
     * dimension.
     * @param name              The name for the displayable.
     * @param zType             The type of the vertical dimension.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public CenterPole(String name, RealType zType)
            throws RemoteException, VisADException {

        super(name);

        this.zType = zType;
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected CenterPole(CenterPole that)
            throws RemoteException, VisADException {

        super(that);

        poleSet = that.poleSet;  // immutable object
        zType   = that.zType;    // immutable object
    }

    /**
     * Sets the vertical extent of the pole.
     * @param minimumZ          The minimum value.
     * @param maximumZ          The maximum value.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setExtent(Real minimumZ, Real maximumZ)
            throws VisADException, RemoteException {

        poleSet = new Linear1DSet(zType,
                                  minimumZ.getValue(zType.getDefaultUnit()),
                                  maximumZ.getValue(zType.getDefaultUnit()),
                                  50);

        setData(poleSet);
    }

    /**
     * Indicates if this instance is semantically identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semanticall identical to the other object.
     */
    public boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof CenterPole)) {
            equals = false;
        } else {
            CenterPole that = (CenterPole) obj;

            equals = (that == this)
                     || (zType.equals(that.zType)
                         && poleSet.equals(that.poleSet)
                         && super.equals(that));
        }

        return equals;
    }

    /**
     * Returns the hash code of this instance.
     * @return                  The hash code of this instance.
     */
    public int hashCode() {
        return zType.hashCode() ^ poleSet.hashCode() ^ super.hashCode();
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws VisADException, RemoteException {
        return new CenterPole(this);
    }
}







