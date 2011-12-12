/*
 * $Id: Box.java,v 1.17 2005/05/13 18:33:24 jeffmc Exp $
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

import ucar.visad.display.*;

import visad.*;
import visad.java2d.DisplayRendererJ2D;


/**
 * Supports a non-standard box background on a thermodynamic diagram.
 *
 * @author Steven R. Emmerson
 * @version $Id: Box.java,v 1.17 2005/05/13 18:33:24 jeffmc Exp $
 */
public class Box extends LineDrawing {

    /**
     * Constructs from a coordinate system transformation.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Box(AerologicalCoordinateSystem coordinateSystem)
            throws VisADException, RemoteException {

        super("Box");

        setLineWidth(2);
        setCoordinateSystem(coordinateSystem);
    }

    /**
     * Sets the coordinate system property.
     * @param coordinateSystem  The new (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setCoordinateSystem(AerologicalCoordinateSystem coordinateSystem)
            throws RemoteException, VisADException {

        float minX = (float) coordinateSystem.getMinimumX().getValue();
        float maxX = (float) coordinateSystem.getMaximumX().getValue();
        float minY = (float) coordinateSystem.getMinimumY().getValue();
        float maxY = (float) coordinateSystem.getMaximumY().getValue();

        // 2D set on 1D manifold implies displayed as lines
        setData(new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                 new float[][] {
            { minX, maxX, maxX, minX, minX }, { minY, minY, maxY, maxY, minY }
        }, 5, (CoordinateSystem) null, (Unit[]) null,
           (ErrorEstimate[]) null));
    }

    /**
     * Constructs from another instance.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Box(Box that) throws RemoteException, VisADException {
        super(that);
    }

    /**
     * Returns a clone of this instance suitable for another VisAD display.
     * Underlying data objects are not cloned.
     * @return                  A clone of this instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Displayable cloneForDisplay()
            throws RemoteException, VisADException {
        return new Box(this);
    }
}







