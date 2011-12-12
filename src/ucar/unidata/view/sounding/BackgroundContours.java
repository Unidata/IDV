/*
 * $Id: BackgroundContours.java,v 1.18 2005/05/13 18:33:24 jeffmc Exp $
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

import java.util.*;

import ucar.unidata.beans.*;

import ucar.visad.display.*;

import visad.*;


/**
 * Supports background contours on a thermodynamic diagram.
 *
 * @author Steven R. Emmerson
 * @version $Id: BackgroundContours.java,v 1.18 2005/05/13 18:33:24 jeffmc Exp $
 */
public abstract class BackgroundContours extends ContourLines {

    /**
     * The field of values to be contoured.  This field must be set by the
     * subclass.
     */
    protected Field field;

    /**
     * The type of the contoured quantity.
     */
    private RealType rangeType;

    /**
     * Constructs from a name for the displayable, the contour levels, and the
     * type of the contoured quantity.
     * @param name              The name for the displayable.
     * @param initialContours   The initial contours.
     * @param rangeType         The type of the contoured quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected BackgroundContours(String name, ContourLevels initialContours, RealType rangeType)
            throws VisADException, RemoteException {

        super(name, rangeType);

        setContourLevels(initialContours);
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected BackgroundContours(BackgroundContours that)
            throws VisADException, RemoteException {

        super(that);

        field = (Field) that.getData();  // from superclass
    }

    /**
     * Sets the associated coordinate system transformation.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     */
    public final void setCoordinateSystem(AerologicalCoordinateSystem coordinateSystem)
            throws VisADException {

        try {
            coordinateSystemChange(coordinateSystem);
            setData(field);
        } catch (RemoteException e) {}  // can't happen because data is local
    }

    /**
     * Indicates if this instance is semantically identical to another object.
     * @param obj               The other object.
     * @return                  <code>true</code> if and only if this instance
     *                          is semantically equal to the other object.
     */
    public final boolean equals(Object obj) {

        boolean equals;

        if ( !(obj instanceof BackgroundContours)) {
            equals = false;
        } else {
            BackgroundContours that = (BackgroundContours) obj;

            equals =
                (this == that)
                || (getContourRealType().equals(that.getContourRealType())
                    && ((field == null)
                        ? that.field == null
                        : field.equals(that.field)));
        }

        return equals;
    }

    /**
     * Gets the hash code of this instance.
     *
     * @return              The hash code of this instance.
     */
    public final int hashCode() {

        return getContourRealType().hashCode() ^ ((field == null)
                                                  ? 0
                                                  : field.hashCode());
    }

    /**
     * Handle a change to the coordinate system transformation.
     * @param coordinateSystem  The new (p,T) <-> (x,y) transformation.
     * @throws TypeException    Something has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void coordinateSystemChange(AerologicalCoordinateSystem coordinateSystem)
     throws TypeException, RemoteException, VisADException;
}







