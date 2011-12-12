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


/**
 * Provides support for the quantity of 2-dimensional velocity that is
 * transformable with cartesian 2D velocity.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $ $Date: 2006/03/17 17:08:53 $
 */
public class Velocity2D extends TupleQuantity {

    /*
     * Constructors:
     */

    /**
     * Constructs from a name and two speed components.  The associated
     * coordinate system transformation will be <code>null</code>.
     *
     * @param name              The name for the quantity.
     * @param xSpeed            The speed in the X direction.
     * @param ySpeed            The speed in the Y direction.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Velocity2D(String name, XSpeed xSpeed, YSpeed ySpeed)
            throws VisADException {
        this(name, xSpeed, ySpeed, (CoordinateSystem) null);
    }

    /**
     * Constructs from a name, two components, and a coordinate system
     * transformation.
     *
     * @param name              The name for the quantity.
     * @param comp1             The first component.
     * @param comp2             The second component.
     * @param coordSys          The coordinate system transformation.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected Velocity2D(String name, ScalarQuantity comp1,
                         ScalarQuantity comp2, CoordinateSystem coordSys)
            throws VisADException {
        super(name, new ScalarQuantity[] { comp1, comp2 }, coordSys);
    }

    /*
     * Class methods:
     */

    /**
     * Returns the reference subclass.  This subclass defines the reference
     * RealTupleType for coordinate system transformations.
     *
     * @return              The reference subclass.
     * @see CartesianVelocity2D#instance()
     */
    public static Velocity2D getReference() {
        return (Velocity2D) CartesianVelocity2D.instance();
    }
}
