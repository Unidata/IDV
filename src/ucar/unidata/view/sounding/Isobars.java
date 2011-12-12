/*
 * $Id: Isobars.java,v 1.15 2005/05/13 18:33:31 jeffmc Exp $
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
import ucar.visad.display.*;
import ucar.visad.quantities.*;


/**
 * Supports background isobars on a thermodynamic diagram.
 *
 * @author Steven R. Emmerson
 * @version $Id: Isobars.java,v 1.15 2005/05/13 18:33:31 jeffmc Exp $
 */
public final class Isobars extends BackgroundContours {

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
     * Returns the type of the pressure quantity.
     * @return                  The type of the pressure quantity.
     * @throws VisADException   VisAD failure.
     */
    private static RealType getPressureType() throws VisADException {

        if (pressureType == null) {
            synchronized (Isobars.class) {
                if (pressureType == null) {
                    pressureType =
                        Util.clone(AirPressure.getRealType(),
                                   "BackgroundContoursIsobarsPressureType");
                }
            }
        }

        return pressureType;
    }

    /**
     * Constructs from nothing.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Isobars() throws VisADException, RemoteException {
        this(SkewTCoordinateSystem.instance());
    }

    /**
     * Constructs from a (p,T) <-> (x,y) coordinate system transformation.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Isobars(AerologicalCoordinateSystem coordinateSystem)
            throws VisADException, RemoteException {
        this(coordinateSystem, new RegularContourLevels(100));
    }

    /**
     * Constructs from a (p,T) <-> (x,y) coordinate system transformation and
     * a set of contour levels.
     * @param coordinateSystem  The (p,T) <-> (x,y) transformation.
     * @param contours          The set of contour levels.
     * @throws TypeException    An argument has the wrong type.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public Isobars(AerologicalCoordinateSystem coordinateSystem, ContourLevels contours)
            throws TypeException, VisADException, RemoteException {

        super("Isobars", contours, getPressureType());

        setCoordinateSystem(coordinateSystem);
    }

    /**
     * Constructs from another instance.
     * @param that                      The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected Isobars(Isobars that) throws RemoteException, VisADException {

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
        return new Isobars(this);
    }

    /**
     * Handles a change to the coordinate system transformation.
     *
     * @param coordinateSystem  The new (p,T) <-> (x,y) transformation.
     * @throws TypeException    if a necessary RealType can't be created.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void coordinateSystemChange(AerologicalCoordinateSystem coordinateSystem)
            throws TypeException, VisADException, RemoteException {

        rangeMinimum =
            (float) coordinateSystem.getMinimumPressure().getValue();
        rangeMaximum =
            (float) coordinateSystem.getMaximumPressure().getValue();

        float[] isobarValues = getContourLevels().getLevels(rangeMinimum,
                                   rangeMaximum);
        float      minX = (float) coordinateSystem.getMinimumX().getValue();
        float      maxX = (float) coordinateSystem.getMaximumX().getValue();
        float[][]  domainSamples = new float[2][2 * isobarValues.length];
        double[][] ptCoordinates = new double[2][1];
        int        j             = 0;

        for (int i = 0; i < isobarValues.length; ++i) {
            ptCoordinates[0][0] = isobarValues[i];
            ptCoordinates[1][0] = 0;

            float y =
                (float) coordinateSystem.toReference(ptCoordinates)[1][0];

            domainSamples[0][j]   = minX;
            domainSamples[1][j++] = y;
            domainSamples[0][j]   = maxX;
            domainSamples[1][j++] = y;
        }

        field = new FlatField(
            new FunctionType(
                RealTupleType
                    .SpatialCartesian2DTuple, getPressureType()), new Gridded2DSet(
                        RealTupleType
                            .SpatialCartesian2DTuple, domainSamples, 2, isobarValues
                            .length));

        float[] rangeSamples = new float[2 * isobarValues.length];

        j = 0;

        for (int i = 0; i < isobarValues.length; ++i) {
            rangeSamples[j++] = isobarValues[i];
            rangeSamples[j++] = isobarValues[i];
        }

        try {
            field.setSamples(new float[][] {
                rangeSamples
            });
        } catch (RemoteException e) {}  // ignore because data is local

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







