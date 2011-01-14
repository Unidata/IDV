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


import ucar.unidata.data.grid.GridUtil;

import visad.Data;

import visad.DateTime;

import visad.Field;

import visad.FieldImpl;

import visad.Real;

import visad.RealTuple;

import visad.SampledSet;

import visad.SingletonSet;

import visad.VisADException;

import visad.georef.LatLonPoint;

import visad.util.DataUtility;



import java.rmi.RemoteException;


/**
 * A concrete {@link Grid3DSoundingDataNode} class for single-time model-output.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.4 $ $Date: 2006/12/01 20:16:37 $
 */
final class SingleTimeGrid3DSoundingDataNode extends Grid3DSoundingDataNode {

    /**
     * Constructs from an output listener.
     *
     * @param listener               The object that will receive output from
     *                               this instance.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    SingleTimeGrid3DSoundingDataNode(Listener listener)
            throws VisADException, RemoteException {
        super(listener);
    }

    /**
     * Implement superclass method to set the output time index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    void setOutputTimeIndex() throws VisADException, RemoteException {
        setOutputTimeIndex(0);
    }

    /**
     * Implement superclass method to set the output profiles.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    void setOutputProfiles() throws VisADException, RemoteException {

        Field tempPro = null;
        Field dewPro  = null;
        Field windPro = null;

        synchronized (this) {
            if ((inLoc != null) && (field != null)) {
                float[][] values = ((Field) GridUtil.getProfileAtLatLonPoint(
                                       (FieldImpl) field, inLoc,
                                       Data.NEAREST_NEIGHBOR).getSample(
                                           0)).getFloats();

                if (values != null) {
                    tempPro = makeTempProfile(values[0]);
                    dewPro  = makeDewProfile(values[1]);
                    if (values.length > 2) {
                        windPro = makeWindProfile(new float[][] {
                            values[2], values[3]
                        });
                    }
                }
            }
        }

        if (tempPro != null) {
            setOutputProfiles(new Field[] { tempPro },
                              new Field[] { dewPro },
                              new Field[] { windPro });
        }
    }
}
