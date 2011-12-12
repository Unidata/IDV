/*
 * $Id: SaturationMixingRatioContours.java,v 1.16 2005/05/13 18:33:36 jeffmc Exp $
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


import ucar.visad.Util;
import ucar.visad.VisADMath;
import ucar.visad.display.*;
import ucar.visad.quantities.*;

import visad.*;

import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for displaying contours of constant saturation mixing-ratio
 * on a thermodynamic diagram.
 *
 * @author Steven R. Emmerson
 * @version $Id: SaturationMixingRatioContours.java,v 1.16 2005/05/13 18:33:36 jeffmc Exp $
 */
public final class SaturationMixingRatioContours extends BackgroundContours {

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
            synchronized (SaturationMixingRatioContours.class) {
                if (pressureType == null) {
                    pressureType = Util.clone(
                        AirPressure.getRealType(),
                        "BackgroundContoursSaturationMixingRatioContoursPressureType");
                }
            }
        }

        return pressureType;
    }

    /**
     * Returns the type of the reange quantity.
     * @return                  The type of the reange quantity.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getRangeType() throws VisADException {

        if (rangeType == null) {
            synchronized (SaturationMixingRatioContours.class) {
                if (rangeType == null) {
                    rangeType = Util.clone(
                        SaturationMixingRatio.getRealType(),
                        "BackgroundContoursSaturationMixingRatioContoursRangeType");
                }
            }
        }

        return rangeType;
    }

    /**
     * Constructs from nothing.
     *
     * @throws VisADException if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public SaturationMixingRatioContours()
            throws VisADException, RemoteException {
        this(SkewTCoordinateSystem.instance());
    }

    /**
     * Constructs from a (p,T) <-> (x,y) coordinate system transformation.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public SaturationMixingRatioContours(
            AerologicalCoordinateSystem coordinateSystem)
            throws VisADException, RemoteException {

        super("SaturationMixingRatioContours",
              new IrregularContourLevels(
                  SaturationMixingRatio.getRealType().getDefaultUnit().toThis(
                      new float[] {
            .001f, .005f, .01f, .025f, .05f, .1f, .2f, .4f, .6f, .8f, 1, 1.5f,
            2, 2.5f, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 16, 18, 20, 24, 28, 32,
            36, 40, 44, 48, 52, 56, 60, 68, 76
        }, new ScaledUnit(.001)), Float.POSITIVE_INFINITY, true),  // will cause dashed lines
        getRangeType());

        coordinateSystemChange(coordinateSystem);
        setHSV(new float[] { 120f, .5f, .9f });  // unsaturated green
        setLabeling(true);
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected SaturationMixingRatioContours(
            SaturationMixingRatioContours that)
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
        return new SaturationMixingRatioContours(this);
    }

    /**
     * Handles a change to the (p,T) <-> (x,y) coordinate system transformation.
     * @param coordinateSystem  The new (p,T) <-> (x,y) transformation.
     * @throws TypeException    Somthing's type is incorrect.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void coordinateSystemChange(
            AerologicalCoordinateSystem coordinateSystem)
            throws TypeException, RemoteException, VisADException {

        try {
            FlatField ptField =
                (FlatField) VisADMath.fromReference(
                    new FunctionType(
                        RealTupleType.SpatialCartesian2DTuple,
                        new RealTupleType(
                            (RealType) coordinateSystem.getMinimumPressure().getType(),
                            (RealType) coordinateSystem.getMinimumTemperature().getType(),
                            coordinateSystem,
                            (visad.Set) null)), VisADMath.newFlatField(
                                new Linear2DSet(
                                    RealTupleType.SpatialCartesian2DTuple,
                                    coordinateSystem.getMinimumX().getValue(),
                                    coordinateSystem.getMaximumX().getValue(),
                                    40,
                                    coordinateSystem.getMinimumY().getValue(),
                                    coordinateSystem.getMaximumY().getValue(),
                                    40)));

            field = (FlatField) Util.ensureMathType(
                SaturationMixingRatio.create(
                    ptField.extract(0), ptField.extract(1)), getRangeType());

            setData(field);

            Unit tempUnit =
                SaturationMixingRatio.getRealType().getDefaultUnit();

            rangeMinimum = (float) ((Real) field.evaluate(
                new RealTuple(
                    RealTupleType.SpatialCartesian2DTuple,
                    new Real[] { coordinateSystem.getMinimumX(),
                                 coordinateSystem
                                 .getMaximumY() }, (CoordinateSystem) null)))
                                     .getValue(tempUnit);
            rangeMaximum = (float) ((Real) field.evaluate(
                new RealTuple(
                    RealTupleType.SpatialCartesian2DTuple,
                    new Real[] { coordinateSystem.getMaximumX(),
                                 coordinateSystem
                                 .getMaximumY() }, (CoordinateSystem) null)))
                                     .getValue(tempUnit);
        } catch (UnimplementedException e) {}  // ignore because the above is known to work
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

