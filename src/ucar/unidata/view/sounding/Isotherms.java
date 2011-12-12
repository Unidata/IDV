/*
 * $Id: Isotherms.java,v 1.16 2005/05/13 18:33:31 jeffmc Exp $
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

import visad.*;

import ucar.visad.Util;
import ucar.visad.VisADMath;
import ucar.visad.display.*;
import ucar.visad.quantities.*;


/**
 * Supports background isotherms on a thermodynamic diagram.
 *
 * @author Steven R. Emmerson
 * @version $Id: Isotherms.java,v 1.16 2005/05/13 18:33:31 jeffmc Exp $
 */
public final class Isotherms extends BackgroundContours {

    /**
     * The maximum value of the range.
     */
    private float rangeMaximum;

    /**
     * The minimum value of the range.
     */
    private float rangeMinimum;

    /**
     * The type of the pressure quantity.
     */
    private static RealType pressureType;

    /**
     * The type of the range quantity.
     */
    private static RealType rangeType;

    /**
     * Returns the type of the pressure quantity.
     * @return                  The type of the pressure quantity.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getPressureType() throws VisADException {

        if (pressureType == null) {
            synchronized (Isotherms.class) {
                if (pressureType == null) {
                    pressureType =
                        Util.clone(AirPressure.getRealType(),
                                   "BackgroundContoursIsothermsPressureType");
                }
            }
        }

        return pressureType;
    }

    /**
     * Returns the type of the range.
     * @return                  The type of the range quantity.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getRangeType() throws VisADException {

        if (rangeType == null) {
            synchronized (Isotherms.class) {
                if (rangeType == null) {
                    rangeType =
                        Util.clone(AirTemperature.getRealType(),
                                   "BackgroundContoursIsothermsRangeType");
                }
            }
        }

        return rangeType;
    }

    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Isotherms() throws VisADException, RemoteException {
        this(SkewTCoordinateSystem.instance());
    }

    /**
     * Constructs from a (p,T) <-> (x,y) coordinate system transformation.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Isotherms(AerologicalCoordinateSystem coordinateSystem)
            throws VisADException, RemoteException {
        this(coordinateSystem, new RegularContourLevels(10));
    }

    /**
     * Constructs from a (p,T) <-> (x,y) coordinate system transformation and
     * a set of contour levels.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @param contours          The set of contour levels.
     * @throws TypeException    Invalid argument type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Isotherms(AerologicalCoordinateSystem coordinateSystem, ContourLevels contours)
            throws TypeException, VisADException, RemoteException {

        super("Isotherms", contours, getRangeType());

        setCoordinateSystem(coordinateSystem);
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Isotherms(Isotherms that)
            throws RemoteException, VisADException {

        super(that);

        rangeMaximum = that.rangeMaximum;
        rangeMinimum = that.rangeMinimum;
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
        return new Isotherms(this);
    }

    /**
     * Handles a change to the (p,T) <-> (x,y) coordinate system transformation.
     * @param coordinateSystem  The new (p,T) <-> (x,y) transformation.
     * @throws TypeException    if a necessary {@link visad.RealType} couldn't
     *                          be created.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void coordinateSystemChange(AerologicalCoordinateSystem coordinateSystem)
            throws TypeException, VisADException, RemoteException {

        try {
            FlatField ptField =
                (FlatField) VisADMath.fromReference(
                    new FunctionType(
                        RealTupleType
                            .SpatialCartesian2DTuple, new RealTupleType(
                                getPressureType(), getRangeType(), coordinateSystem, (visad
                                    .Set) null)), VisADMath.newFlatField(
                                        new Linear2DSet(
                                            RealTupleType
                                                .SpatialCartesian2DTuple, coordinateSystem
                                                .getMinimumX()
                                                .getValue(), coordinateSystem
                                                .getMaximumX()
                                                .getValue(), 20, coordinateSystem
                                                .getMinimumY()
                                                .getValue(), coordinateSystem
                                                .getMaximumY()
                                                .getValue(), 20)));

            field = (FlatField) Util.ensureMathType(ptField.extract(1),
                                                    getRangeType());
        } catch (RemoteException e) {}       // ignore because all data is local
        catch (UnimplementedException e) {}  // ignore because the above is known to work

        Unit tempUnit = coordinateSystem.getCoordinateSystemUnits()[1];

        rangeMinimum =
            (float) coordinateSystem.getMinimumTemperature().getValue(
                tempUnit);
        rangeMaximum =
            (float) coordinateSystem.getMaximumTemperature().getValue(
                tempUnit);

        setRange(rangeMinimum, rangeMaximum);
    }

    /**
     * Returns the minimum value of the range.
     * @return                  The minimum value of the range.
     */
    protected float getRangeMinimum() {
        return rangeMinimum;
    }

    /**
     * Returns the maximum value of the range.
     * @return                  The maximum value of the range.
     */
    protected float getRangeMaximum() {
        return rangeMaximum;
    }
}







