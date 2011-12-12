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

package ucar.unidata.idv.control;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;

import ucar.visad.Util;

import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.Altitude;

import visad.CoordinateSystem;

import visad.Data;

import visad.DateTime;

import visad.ErrorEstimate;

import visad.Field;

import visad.FlatField;

import visad.FunctionType;

import visad.Gridded1DSet;

import visad.GriddedSet;

import visad.Real;

import visad.RealTuple;

import visad.RealTupleType;

import visad.RealType;

import visad.SingletonSet;

import visad.Tuple;

import visad.TupleType;

import visad.TypeException;

import visad.Unit;

import visad.VisADException;

import visad.georef.EarthLocationTuple;

import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import visad.util.DataUtility;



import java.rmi.RemoteException;


/**
 * A concrete {@link SoundingDataNode} for track (e.g. aircraft) soundings.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.4 $ $Date: 2006/12/01 20:16:39 $
 */
final class TrackSoundingDataNode extends SoundingDataNode {

    /** the data field */
    private Field field;

    /**
     * Constructs from an output listener.
     *
     * @param listener               The object that will receive output from
     *                               this instance.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    TrackSoundingDataNode(Listener listener)
            throws VisADException, RemoteException {
        super(listener);
    }

    /**
     * <p>Sets the input {@link visad.Data} object.</p>
     *
     * @param data                   The input data object.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public void setData(Data data) throws VisADException, RemoteException {

        field = (Field) data;

        if (field != null) {
            float[][] values       = field.getFloats();  // NB: default units
            int[]     indexes = Util.strictlySortedIndexes(values[0], false);
            int       primaryIndex = indexes[0];

            /*
             * Set the set of output times.  Use the time of the greatest valid
             * pressure.
             */
            setOutputTimes(
                new SingletonSet(
                    new RealTuple(
                        new Real[] {
                            new DateTime(
                                (Real) DataUtility.getSample(
                                    field.getDomainSet(),
                                    primaryIndex).getComponent(0)) })));

            /*
             * Set the output location.  Use the location of the greatest valid
             * pressure.
             */
            RealTuple loc3 = (RealTuple) ((Tuple) field.getSample(
                                 primaryIndex)).getComponent(4);
            LatLonPoint loc = new LatLonTuple((Real) loc3.getComponent(0),
                                  (Real) loc3.getComponent(1));

            setOutputLocation(loc);

            /*
             * Extract the valid data.
             */
            float[] press = Util.take(values[0], indexes);
            float[] temps = Util.take(values[1], indexes);
            float[] dews  = Util.take(values[2], indexes);
            float[] spds  = Util.take(values[3], indexes);
            float[] dirs  = Util.take(values[4], indexes);
            float[] lats  = Util.take(values[5], indexes);
            float[] lons  = Util.take(values[6], indexes);
            float[] alts  = Util.take(values[7], indexes);

            /*
             * Set the set of output locations.
             */
            setOutputLocations(
                GriddedSet.create(
                    RealTupleType.LatitudeLongitudeAltitude, new float[][] {
                lats, lons, alts
            }, new int[] { lats.length }));

            /*
             * Create the pressure domain.
             */
            TupleType rangeType =
                (TupleType) ((FunctionType) field.getType()).getRange();
            RealType presType = (RealType) rangeType.getComponent(0);
            RealType altType =
                (RealType) ((RealTupleType) rangeType.getComponent(
                    4)).getComponent(2);
            Gridded1DSet presDomain =
                Gridded1DSet.create(AirPressure.getRealTupleType(), press,
                                    (CoordinateSystem) null,
                                    presType.getDefaultUnit(),
                                    (ErrorEstimate) null);

            /*
             * The following constructor is invoked with a null
             * CoordinateSystem in order to use the default CoordinateSystem
             * of the pressure domain.  The alternative (constructing an
             * EmpiricalCoordinateSystem based on the pressure and altitude
             * values of the data) would result in a domain whose pressure
             * values might not be respected in a binary operation with another
             * doamin due to the behavior of
             * CoordinateSystem.transformCoordinatesFreeUnits().
             */
            presDomain = Gridded1DSet.create(presDomain.getType(),
                                             presDomain.getSamples(false)[0],
                                             (CoordinateSystem) null,
                                             presDomain.getSetUnits()[0],
                                             (ErrorEstimate) null);

            /*
             * Set the output profiles.
             */
            RealType tempType =
                (RealType) ((TupleType) ((FunctionType) field.getType())
                    .getRange()).getComponent(1);
            RealType dewType =
                (RealType) ((TupleType) ((FunctionType) field.getType())
                    .getRange()).getComponent(2);
            RealTupleType windType =
                (RealTupleType) ((TupleType) ((FunctionType) field.getType())
                    .getRange()).getComponent(3);
            FlatField tempPro = new FlatField(
                                    new FunctionType(
                                        AirPressure.getRealTupleType(),
                                        tempType), presDomain);
            FlatField dewPro = new FlatField(
                                   new FunctionType(
                                       AirPressure.getRealTupleType(),
                                       dewType), presDomain);
            FlatField windPro = new FlatField(
                                    new FunctionType(
                                        AirPressure.getRealTupleType(),
                                        windType), presDomain);

            tempPro.setSamples(new float[][] {
                temps
            }, false);
            dewPro.setSamples(new float[][] {
                dews
            }, false);
            dewPro.setSamples(new float[][] {
                spds, dirs
            }, false);
            setOutputProfiles(new Field[] { tempPro },
                              new Field[] { dewPro },
                              new Field[] { windPro });
        }
    }
}
