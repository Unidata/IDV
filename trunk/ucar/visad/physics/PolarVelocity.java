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
 * Provides support for the quantity of 2-dimensional velocity in polar
 * coordinates.  The associated coordinate system transformation has
 * cartesian speeds as its reference coordinate system.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2006/03/17 17:08:52 $
 */
public class PolarVelocity extends Velocity2D {

    /*
     * Fields:
     */

    /**
     * The single instance of this class.
     */
    private static PolarVelocity instance;

    static {
        CoordinateSystem coordSys;

        try {
            coordSys = new PolarCoordinateSystem(
                CartesianVelocity2D.instance().getRealTupleType(),
                Speed.DEFAULT_UNIT, PlaneAngle.DEFAULT_UNIT);
            instance = new PolarVelocity("Polar_Velocity",
                                         (Speed) Speed.instance(),
                                         (PlaneAngle) PlaneAngle.instance(),
                                         coordSys);
        } catch (Exception e) {
            System.err.println(
                "PolarVelocity.<clinit>(): Couldn't initialize class: " + e);
            System.exit(1);
        }
    }

    /*
     * Constructors:
     */

    /**
     * Constructs from a name, a speed component, a direction component, and a
     * coordinate system transformation.
     *
     * @param name              The name for the quantity.
     * @param speed             The speed component.
     * @param direction         The direction component.
     * @param coordSys          The coordinate system transformation.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected PolarVelocity(String name, Speed speed, PlaneAngle direction,
                            CoordinateSystem coordSys)
            throws VisADException {
        super(name, speed, direction, coordSys);
    }

    /*
     * Class methods:
     */

    /**
     * Returns an instance of this class.
     *
     * @return                  An instance of this class.
     */
    static PolarVelocity instance() {
        return instance;
    }

    /*
     * Instance methods:
     */

    /**
     * Returns a single value of this quantity.
     *
     * @param xSpeed            The speed in the X direction.
     * @param ySpeed            The speed in the Y direction.
     * @return                  The single value corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealTuple newRealTuple(Real xSpeed, Real ySpeed)
            throws VisADException, RemoteException {
        return (RealTuple) newRealTuple(new Real[] { xSpeed, ySpeed },
                                        (CoordinateSystem) null);
    }

    /**
     * Returns a single value of this quantity.
     *
     * @param speeds            The (x,y) components of the quantity, in order.
     * @param coordSys          The coordinate system transformation for this
     *                          particular value.  Must be <code>null</code>.
     * @return                  The single value corresponding to the input.
     * @throws CoordinateSystemException
     *                          The coordinate system transformation is non-
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public RealTuple newRealTuple(Real[] speeds, CoordinateSystem coordSys)
            throws CoordinateSystemException, VisADException,
                   RemoteException {

        if (coordSys != null) {
            throw new CoordinateSystemException(getClass().getName()
                    + ".newRealTuple(): "
                    + "Non-null coordinate system transformation argument");
        }

        return newRealTuple(speeds[0], speeds[1]);
    }
}
