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
 * Provides support for the quantity of 2-dimensional velocity with no,
 * associated coordinate system transformation.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $ $Date: 2006/03/17 17:08:51 $
 */
public class CartesianVelocity2D extends Velocity2D {

    /*
     * Fields:
     */

    /**
     * The single instance of this class.
     */
    private static CartesianVelocity2D instance;

    /** _more_ */
    private RealType xSpeedType;

    /** _more_ */
    private RealType ySpeedType;

    static {
        try {
            instance = new CartesianVelocity2D();
        } catch (Exception e) {
            System.err.println(
                "CartesianVelocity2D.<clinit>: Couldn't initialize class: "
                + e);
            System.exit(1);
        }
    }

    /*
     * Constructors:
     */

    /**
     * Constructs from nothing.  The name will be "Cartesian_2D_Velocity".
     *
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #CartesianVelocity2D(Class dataIface, String name)
     */
    private CartesianVelocity2D() throws VisADException {
        this("Cartesian_2D_Velocity");
    }

    /**
     * Constructs from a name.  The components will be @{link XSpeed.instance()}
     * and @{link YSpeed.instance()}, in that order.
     *
     * @param name              The name for the quantity.
     * @throws VisADException   Couldn't create necessary VisAD object.
     * @see #CartesianVelocity2D(String, XSpeed, YSpeed)
     */
    protected CartesianVelocity2D(String name) throws VisADException {
        this(name, (XSpeed) XSpeed.instance(), (YSpeed) YSpeed.instance());
    }

    /**
     * Constructs from a name and two speed components.
     *
     * @param name              The name for the quantity.
     * @param xSpeed            The speed in the X direction.
     * @param ySpeed            The speed in the Y direction.
     * @throws VisADException   Couldn't create necessary VisAD object.
     */
    protected CartesianVelocity2D(String name, XSpeed xSpeed, YSpeed ySpeed)
            throws VisADException {

        super(name, xSpeed, ySpeed);

        xSpeedType = xSpeed.getRealType();
        ySpeedType = ySpeed.getRealType();
    }

    /*
     * Class methods:
     */

    /**
     * Returns an instance of this class.
     *
     * @return                  An instance of this class.  The class of the
     *                          object is this class.
     */
    public static TupleQuantity instance() {
        return instance;
    }

    /**
     * Vets a VisAD data object for compatibility.
     *
     * @param data              The VisAD data object to examine for
     *                          compatibility.
     * @throws TypeException    The VisAD data object is incompatible with this
     *                          quantity.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public static void Vet(visad.Data data)
            throws TypeException, VisADException, RemoteException {
        instance.vet(data);
    }

    /**
     * Returns a single value of this quantity.
     *
     * @param xSpeed            The speed in the X direction.  Must be
     *                          compatible with {@link Speed}.
     * @param ySpeed            The speed in the Y direction.  Must be
     *                          compatible with {@link Speed}.
     * @return                  The single value corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see #newRealTuple(Real, Real)
     */
    public static RealTuple NewRealTuple(Real xSpeed, Real ySpeed)
            throws VisADException, RemoteException {
        return instance.newRealTuple(xSpeed, ySpeed);
    }

    /*
     * Instance methods:
     */

    /**
     * Returns a single value of this quantity.
     *
     * @param xSpeed            The speed in the X direction.  Must be
     *                          compatible with {@link Speed}.
     * @param ySpeed            The speed in the Y direction.  Must be
     *                          compatible with {@link Speed}.
     * @return                  The single value corresponding to the input.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     * @see RealTuple#RealTuple(RealTupleType, Real[], CoordinateSystem)
     */
    public RealTuple newRealTuple(Real xSpeed, Real ySpeed)
            throws VisADException, RemoteException {

        Speed.Vet(xSpeed);
        Speed.Vet(ySpeed);

        if ( !xSpeedType.equals(xSpeed.getType())) {
            xSpeed = (Real) xSpeed.changeMathType(xSpeedType);
        }

        if ( !ySpeedType.equals(ySpeed.getType())) {
            ySpeed = (Real) ySpeed.changeMathType(ySpeedType);
        }

        return new RealTuple(getRealTupleType(), new Real[] { xSpeed,
                ySpeed }, (CoordinateSystem) null);
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

        if (speeds.length != 2) {
            throw new VisADException(
                getClass().getName()
                + ".newRealTuple: Bad speed-array length");
        }

        return newRealTuple((Real) speeds[0], (Real) speeds[1]);
    }
}
