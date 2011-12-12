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



import java.rmi.RemoteException;
import java.rmi.RemoteException;


/**
 * Provides support for the quantity of 3-dimensional velocity that is
 * transformable with cartesian 3D velocity.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2006/03/17 17:08:53 $
 */
public abstract class Velocity3D extends TupleQuantity {

    /*
     * Constructors:
     */

    /**
     * Constructs from a data interface, a name and three speed components.  The
     * associated coordinate system transformation will be <code>null</code>.
     *
     * @param name              The name for the quantity.
     * @param xSpeed            The speed in the X direction.
     * @param ySpeed            The speed in the Y direction.
     * @param zSpeed            The speed in the Z direction.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #Velocity3D(String, ScalarQuantity, ScalarQuantity, ScalarQuantity,
     *                  CoordinateSystem)
     */
    protected Velocity3D(String name, XSpeed xSpeed, YSpeed ySpeed,
                         ZSpeed zSpeed)
            throws VisADException {
        this(name, xSpeed, ySpeed, zSpeed, (CoordinateSystem) null);
    }

    /**
     * Constructs from a name, three components, and a
     * coordinate system transformation.
     *
     * @param name              The name for the quantity.
     * @param comp1             The first component.
     * @param comp2             The second component.
     * @param comp3             The third component.
     * @param coordSys          The coordinate system transformation.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Velocity3D(String name, ScalarQuantity comp1,
                         ScalarQuantity comp2, ScalarQuantity comp3,
                         CoordinateSystem coordSys)
            throws VisADException {
        super(name, new ScalarQuantity[] { comp1, comp2, comp3 }, coordSys);
    }
}
