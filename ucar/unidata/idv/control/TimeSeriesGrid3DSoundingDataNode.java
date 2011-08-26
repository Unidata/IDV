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

import visad.*;

import visad.georef.LatLonPoint;



import java.rmi.RemoteException;


/**
 * A concrete {@link Grid3DSoundingDataNode} class for multiple-time
 * model-output.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.4 $ $Date: 2006/12/01 20:16:38 $
 */
final class TimeSeriesGrid3DSoundingDataNode extends Grid3DSoundingDataNode {

    /** time domain */
    private SampledSet timeDomain;

    /** time unit */
    private Unit timeUnit;

    /** index into set */
    private int index = -1;

    /** number of times/profiles in each array */
    private int timeCount;

    /**
     * Constructs from an output listener.
     *
     * @param listener               The object that will receive output from
     *                               this instance.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    TimeSeriesGrid3DSoundingDataNode(Listener listener)
            throws VisADException, RemoteException {
        super(listener);
    }

    /**
     * <p>Sets the input {@link visad.Field} object.</p>
     *
     * @param field                  The input field object.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    void setField(Field field) throws VisADException, RemoteException {

        if (field == null) {
            throw new NullPointerException();
        }

        synchronized (this) {
            timeCount  = field.getLength();
            timeDomain = (SampledSet) field.getDomainSet();
            timeUnit   = timeDomain.getSetUnits()[0];
        }
    }

    /**
     * Set the output time index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    void setOutputTimeIndex() throws VisADException, RemoteException {

        int timeI = -1;

        synchronized (this) {
            if ((inTime != null) && (timeDomain != null)) {
                int i = timeDomain.doubleToIndex(new double[][] {
                    { inTime.getValue(timeUnit) }
                })[0];

                if (i != index) {
                    timeI = index = i;
                }
            }
        }

        if (timeI != -1) {
            setOutputTimeIndex(timeI);
        }
    }

    /**
     * Set the output forfiles
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    void setOutputProfiles() throws VisADException, RemoteException {

        Field[] tempPros = null;
        Field[] dewPros  = null;
        Field[] windPros = null;

        synchronized (this) {
            if ((inLoc != null) && (field != null)) {
                Field timeSer =
                    GridUtil.getProfileAtLatLonPoint((FieldImpl) field,
                        inLoc, Data.NEAREST_NEIGHBOR);
                Boolean ensble = GridUtil.hasEnsemble((FieldImpl)timeSer);

                if(ensble){
                    FieldImpl sample =
                        (FieldImpl) timeSer.getSample(0);
                    Set ensDomain = sample.getDomainSet();
                    int len = ensDomain.getLength();
                    tempPros = new FlatField[timeCount*len];
                    dewPros  = new FlatField[timeCount*len];
                    windPros = new FlatField[timeCount*len];
                } else {

                    tempPros = new FlatField[timeCount];
                    dewPros  = new FlatField[timeCount];
                    windPros = new FlatField[timeCount];
                }

                for (int i = 0; i < timeCount; i++) {

                    if(ensble){
                        FieldImpl sample = (FieldImpl) timeSer.getSample(i);
                        Set ensDomain = sample.getDomainSet();
                        int len = ensDomain.getLength();

                        for (int j = 0; j < len; j++) {
                            float[][] values = ((Field) sample.getSample(i)).getFloats();
                            tempPros[i] = makeTempProfile(values[0]);
                            dewPros[i]  = makeDewProfile(values[1]);
                            if (values.length > 2) {
                                windPros[i] = makeWindProfile(new float[][] {
                                    values[2], values[3]
                                });
                            }
                        }
                    } else {
                        float[][] values =
                            ((Field) timeSer.getSample(i)).getFloats();

                        tempPros[i] = makeTempProfile(values[0]);
                        dewPros[i]  = makeDewProfile(values[1]);
                        if (values.length > 2) {
                            windPros[i] = makeWindProfile(new float[][] {
                                values[2], values[3]
                            });
                        }
                    }
               }
            }
        }

        if (tempPros != null) {
            setOutputProfiles(tempPros, dewPros, windPros);
        }
    }
}
