/*
 * $Id: SaturationAdiabats.java,v 1.18 2005/05/13 18:33:36 jeffmc Exp $
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
 * Supports background saturation adiabats on a thermodynamic diagram.
 *
 * @author Steven R. Emmerson
 * @version $Id: SaturationAdiabats.java,v 1.18 2005/05/13 18:33:36 jeffmc Exp $
 */
public final class SaturationAdiabats extends BackgroundContours {

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
     * The (p,T) <-> (x,y) coordinate system transformation.
     */
    private CoordinateSystem coordSys;

    /**
     * Returns the type of the pressure quantity.
     * @return                  The type of the pressure quantity.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getPressureType() throws VisADException {

        if (pressureType == null) {
            synchronized (SaturationAdiabats.class) {
                if (pressureType == null) {
                    pressureType = Util.clone(
                        AirPressure.getRealType(),
                        "BackgroundContoursSaturationAdiabatsPressureType");
                }
            }
        }

        return pressureType;
    }

    /**
     * Returns the type of the range quantity.
     * @return                  The type of the range quantity.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getRangeType() throws VisADException {

        if (rangeType == null) {
            synchronized (SaturationAdiabats.class) {
                if (rangeType == null) {
                    rangeType = Util.clone(
                        SaturationEquivalentPotentialTemperature.getRealType(),
                        "BackgroundContoursSaturationAdiabatsRangeType");
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
    public SaturationAdiabats() throws VisADException, RemoteException {
        this(SkewTCoordinateSystem.instance());
    }

    /**
     * Constructs from a (p,T) <-> (x,y) coordinate system transformation.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public SaturationAdiabats(AerologicalCoordinateSystem coordinateSystem)
            throws VisADException, RemoteException {

        super("SaturationAdiabats", new RegularContourLevels(20),
              getRangeType());

        coordinateSystemChange(coordinateSystem);
        setHSV(new float[]{ 120f, .5f, .9f });  // unsaturated green
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected SaturationAdiabats(SaturationAdiabats that)
            throws RemoteException, VisADException {

        super(that);

        rangeMaximum = that.rangeMaximum;
        rangeMinimum = that.rangeMinimum;
        coordSys     = that.coordSys;  // immutable object
    }

    /**
     * Handles a change to the (p,T) <-> (x,y) coordinate system transformation.
     * @param coordinateSystem  The new (p,T) <-> (x,y) transformation.
     * @throws TypeException    Somthing's type is incorrect.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void coordinateSystemChange(AerologicalCoordinateSystem coordinateSystem)
            throws TypeException, RemoteException, VisADException {

        try {
            int count = 20;
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
                                                .getValue(), count, coordinateSystem
                                                .getMinimumY()
                                                .getValue(), coordinateSystem
                                                .getMaximumY()
                                                .getValue(), count)));
            FlatField pressureField    = (FlatField) ptField.extract(0);
            FlatField temperatureField = (FlatField) ptField.extract(1);

            field = (FlatField) Util.ensureMathType(
                SaturationEquivalentPotentialTemperature.create(
                    pressureField, temperatureField), getRangeType());

            setData(field);

            float[] pressures = pressureField.getFloats(false)[0];

            coordSys = new EmpiricalCoordinateSystem(
                new Gridded2DSet(
                    new RealTupleType(
                        getPressureType(), SaturationEquivalentPotentialTemperature
                            .getRealType()), new float[][] {
                pressures, field.getFloats(false)[0]
            }, count, count, (CoordinateSystem) null,
               new Unit[]{ pressureField.getDefaultRangeUnits()[0],
                           field.getDefaultRangeUnits()[0] }, (ErrorEstimate[]) null), new Gridded2DSet(
                               new RealTupleType(
                                   getPressureType(),
                                   AirTemperature.getRealType()), new float[][] {
                pressures, temperatureField.getFloats(false)[0]
            }, count, count, (CoordinateSystem) null,
               new Unit[]{ pressureField.getDefaultRangeUnits()[0],
                           temperatureField
                               .getDefaultRangeUnits()[0] }, (ErrorEstimate[]) null));

            Unit tempUnit =
                SaturationEquivalentPotentialTemperature.getRealType()
                    .getDefaultUnit();

            rangeMinimum = (float) ((Real) field.evaluate(
                new RealTuple(
                    RealTupleType.SpatialCartesian2DTuple,
                    new Real[]{ coordinateSystem.getMinimumX(),
                                coordinateSystem
                                    .getMinimumY() }, (CoordinateSystem) null)))
                                        .getValue(tempUnit);

            /*
             * The maximum saturation equivalent potential temperature should be
             * in either the lower right corner or the upper right corner.
            rangeMaximum = (float) Math.max(
                ((Real) field.evaluate(
                    new RealTuple(
                        RealTupleType.SpatialCartesian2DTuple,
                        new Real[]{ coordinateSystem.getMaximumX(),
                                    coordinateSystem.getMinimumY() },
                        (CoordinateSystem) null))).getValue(tempUnit),
                ((Real) field.evaluate(
                    new RealTuple(
                        RealTupleType.SpatialCartesian2DTuple,
                        new Real[]{coordinateSystem.getMaximumX(),
                                   coordinateSystem.getMaximumY() },
                        (CoordinateSystem) null))).getValue(tempUnit));
             */

            /*
             * The maximum saturation equivalent potential temperature should be
             * in the lower right corner
             */
            rangeMaximum = (float) ((Real) field.evaluate(
                new RealTuple(
                    RealTupleType.SpatialCartesian2DTuple,
                    new Real[]{ coordinateSystem.getMaximumX(),
                                coordinateSystem
                                    .getMinimumY() }, (CoordinateSystem) null)))
                                        .getValue(tempUnit);

            setRange(rangeMinimum, rangeMaximum);
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

    /**
     * Returns the air temperature given air pressure and saturation equivalent
     * potential temperature.
     *
     * @param airPressure       The air pressure.
     * @param satEquivPotTem    The saturation equivalent potential temperature.
     * @return                  Air temperature.
     * @throws VisADException   VisAD failure.
     */
    public Real getTemperature(Real airPressure, Real satEquivPotTem)
            throws VisADException {

        return new Real(AirTemperature.getRealType(),
                        coordSys.toReference(new double[][] {
            { airPressure.getValue(getPressureType().getDefaultUnit()) },
            { satEquivPotTem
                .getValue(SaturationEquivalentPotentialTemperature
                    .getRealType().getDefaultUnit()) }
        })[1][0]);
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
        return new SaturationAdiabats(this);
    }
}







