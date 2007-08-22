/*
 * $Id: RaobSoundingDataNode.java,v 1.4 2006/12/01 20:16:37 jeffmc Exp $
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

package ucar.unidata.idv.control;


import visad.Data;

import visad.DateTime;

import visad.Field;

import visad.Real;

import visad.RealTuple;

import visad.SingletonSet;

import visad.Tuple;

import visad.VisADException;

import visad.georef.EarthLocationTuple;

import visad.georef.LatLonTuple;



import java.rmi.RemoteException;


/**
 * A concrete {@link SoundingDataNode} for RAOB soundings.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.4 $ $Date: 2006/12/01 20:16:37 $
 */
final class RaobSoundingDataNode extends SoundingDataNode {

    /**
     * Constructs from an output listener.
     *
     * @param listener               The object that will receive output from
     *                               this instance.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    RaobSoundingDataNode(Listener listener)
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

        Tuple tuple = (Tuple) data;
        SingletonSet outTimes = new SingletonSet(new RealTuple(new Real[] {
                                    (DateTime) tuple.getComponent(0) }));
        EarthLocationTuple loc3 = (EarthLocationTuple) tuple.getComponent(1);
        LatLonTuple outLoc = new LatLonTuple(loc3.getLatitude(),
                                             loc3.getLongitude());
        SingletonSet outLocs  = new SingletonSet(outLoc);
        Field[]      tempPros = new Field[] { (Field) tuple.getComponent(2) };
        Field[]      dewPros  = new Field[] { (Field) tuple.getComponent(3) };
        Field[]      windPros = new Field[] { (Field) tuple.getComponent(4) };

        setOutputTimes(outTimes);
        setOutputLocation(outLoc);
        setOutputLocations(outLocs);
        setOutputProfiles(tempPros, dewPros, windPros);
    }
}

