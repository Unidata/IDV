/*
 * $Id: DryAdiabats.java,v 1.15 2005/05/13 18:33:29 jeffmc Exp $
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
 * Supports background dry adiabats on a thermodynamic diagram.
 *
 * @author Steven R. Emmerson
 * @version $Id: DryAdiabats.java,v 1.15 2005/05/13 18:33:29 jeffmc Exp $
 */
public final class DryAdiabats extends BackgroundContours {

    /**
     * The maximum range value.
     */
    private float rangeMaximum;

    /**
     * The minimum range value.
     */
    private float rangeMinimum;

    /**
     * The type of the pressure.
     */
    private static RealType pressureType;

    /**
     * The type of the range.
     */
    private static RealType rangeType;

    static {
        RealType pt = null;
        RealType rt = null;

        try {
            pt = Util.clone(AirPressure.getRealType(),
                            "BackgroundContoursDryAdiabatsPressureType");
            rt = Util.clone(PotentialTemperature.getRealType(),
                            "BackgroundContoursDryAdiabatsRangeType");
        } catch (Exception e) {
            System.err.println("Couldn't initialize class: " + e);
            e.printStackTrace();

            throw new RuntimeException();
        }

        pressureType = pt;
        rangeType    = rt;
    }

    /**
     * Returns the type of the pressure quantity.
     * @return                  The type of the pressure quantity.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getPressureType() throws VisADException {
        return pressureType;
    }

    /**
     * Returns the type of the range quantity.
     * @return                  The type of the range quantity.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getRangeType() throws VisADException {
        return rangeType;
    }

    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DryAdiabats() throws VisADException, RemoteException {
        this(SkewTCoordinateSystem.instance());
    }

    /**
     * Constructs from a coordinate system transformation.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DryAdiabats(AerologicalCoordinateSystem coordinateSystem)
            throws VisADException, RemoteException {

        super("DryAdiabats", new RegularContourLevels(20), getRangeType());

        coordinateSystemChange(coordinateSystem);
        setHSV(new float[]{ 40f, .5f, 1f });  // brown
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected DryAdiabats(DryAdiabats that)
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
        return new DryAdiabats(this);
    }

    /**
     * Handle a change to the coordinate system transformation.
     * @param coordinateSystem  The new (p,T) <-> (x,y) transformation.
     * @throws TypeException    Something has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void coordinateSystemChange(AerologicalCoordinateSystem coordinateSystem)
            throws TypeException, RemoteException, VisADException {

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

            field = (FlatField) Util.ensureMathType(
                PotentialTemperature.create(
                    ptField.extract(0), ptField.extract(1)), getRangeType());

            setData(field);

            Unit tempUnit =
                PotentialTemperature.getRealType().getDefaultUnit();

            rangeMinimum = (float) ((Real) field.evaluate(
                new RealTuple(
                    RealTupleType.SpatialCartesian2DTuple,
                    new Real[]{ coordinateSystem.getMinimumX(),
                                coordinateSystem
                                    .getMinimumY() }, (CoordinateSystem) null)))
                                        .getValue(tempUnit);
            rangeMaximum = (float) ((Real) field.evaluate(
                new RealTuple(
                    RealTupleType.SpatialCartesian2DTuple,
                    new Real[]{ coordinateSystem.getMaximumX(),
                                coordinateSystem
                                    .getMaximumY() }, (CoordinateSystem) null)))
                                        .getValue(tempUnit);

            setRange(rangeMinimum, rangeMaximum);
        } catch (UnimplementedException e) {}  // can't happen because the above is known to work
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







