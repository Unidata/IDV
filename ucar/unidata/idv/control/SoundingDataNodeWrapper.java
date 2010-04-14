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


import visad.Data;

import visad.DateTime;

import visad.Field;

import visad.FunctionType;

import visad.MathType;

import visad.SampledSet;

import visad.TupleType;

import visad.VisADException;

import visad.georef.LatLonPoint;



import java.rmi.RemoteException;

import java.util.Arrays;


/**
 * A {@link SoundingDataNode} that wraps a {@link SoundingDataNode} that is specific
 * to the type of input {@link visad.Data} object.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.4 $ $Date: 2006/12/01 20:16:37 $
 */
final class SoundingDataNodeWrapper extends SoundingDataNode {

    /** node to wrap */
    private volatile SoundingDataNode node;

    /** listener */
    private final Listener innerEar;

    /** output index */
    private int outIndex = -1;

    /** time for data */
    private SampledSet outTimes;

    /** location for data */
    private LatLonPoint outLoc;

    /** set of locations */
    private SampledSet outLocs;

    /** array of temperature profiles */
    private Field[] outTempPros;

    /** array of dewpoint profiles */
    private Field[] outDewPros;

    /** array of wind profiles */
    private Field[] outWindPros;

    /**
     * Constructs from an output listener.
     *
     * @param listener               The object that will receive output from
     *                               this instance.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    SoundingDataNodeWrapper(Listener listener)
            throws VisADException, RemoteException {

        super(listener);

        innerEar = new Listener() {

            /*
             * Because the wrapped node is unavailable outside this
             * instance, this instance should not be called back by old
             * nodes that have been replaced; consequently, we don't check
             * to ensure that the calling node is the currently wrapped node
             * in the following methods.
             */
            public void setTimeIndex(int index, SoundingDataNode source)
                    throws VisADException, RemoteException {

                synchronized (this) {
                    if (index == outIndex) {
                        index = -1;
                    } else {
                        outIndex = index;
                    }
                }

                if (index != -1) {
                    setOutputTimeIndex(index);
                }
            }

            public void setTimes(SampledSet times, SoundingDataNode source)
                    throws VisADException, RemoteException {

                synchronized (this) {
                    if (times.equals(outTimes)) {
                        times = null;
                    } else {
                        outTimes = times;
                    }
                }

                if (times != null) {
                    setOutputTimes(times);
                }
            }

            public void setLocation(LatLonPoint loc, SoundingDataNode source)
                    throws VisADException, RemoteException {

                synchronized (this) {
                    if (loc.equals(outLoc)) {
                        loc = null;
                    } else {
                        outLoc = loc;
                    }
                }

                if (loc != null) {
                    setOutputLocation(loc);
                }
            }

            public void setLocations(SampledSet locs, SoundingDataNode source)
                    throws VisADException, RemoteException {

                synchronized (this) {
                    if (locs.equals(outLocs)) {
                        locs = null;
                    } else {
                        outLocs = locs;
                    }
                }

                if (locs != null) {
                    setOutputLocations(locs);
                }
            }

            public void setProfiles(Field[] tempPros, Field[] dewPros,
                                    Field[] windPros, SoundingDataNode source)
                    throws VisADException, RemoteException {

                synchronized (this) {
                    if (Arrays.equals(tempPros, outTempPros)
                            && Arrays.equals(dewPros, outDewPros)
                            && Arrays.equals(windPros, outWindPros)) {
                        tempPros = null;
                    } else {
                        outTempPros = tempPros;
                        outDewPros  = dewPros;
                        outWindPros = windPros;
                    }
                }

                if (tempPros != null) {
                    setOutputProfiles(tempPros, dewPros, windPros);
                }
            }
        };
    }

    /**
     * Sets the input VisAD {@link visad.Data} object.  The input
     * {@link visad.Data} object is examined to determine exactly what type
     * of data-dependent {@link SoundingDataNode} to create and wrap with
     * this instance.
     *
     * @param data                  The input VisAD data object.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void setData(Data data) throws VisADException, RemoteException {

        MathType type = data.getType();

        if (type instanceof TupleType) {
            changeNodeAndData(new RaobSoundingDataNode(innerEar), data);
        } else if (((FunctionType) type).getRange() instanceof TupleType) {
            changeNodeAndData(new TrackSoundingDataNode(innerEar), data);
        } else if (((Field) data).getLength() == 1) {
            changeNodeAndData(new SingleTimeGrid3DSoundingDataNode(innerEar),
                              data);
        } else {
            changeNodeAndData(new TimeSeriesGrid3DSoundingDataNode(innerEar),
                              data);
        }
    }

    /**
     * Change the node and the data
     *
     * @param newNode         new node
     * @param data            the data for that node
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void changeNodeAndData(SoundingDataNode newNode, Data data)
            throws VisADException, RemoteException {

        synchronized (this) {
            node = newNode;
        }

        newNode.setData(data);
    }

    /**
     * <p>Sets the horizontal location.</p>
     *
     * <p>This implementation merely passes the invocation to the wrapped
     * {@link SoundingDataNode}.</p>
     *
     * @param loc                    The horizontal location.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public void setLocation(LatLonPoint loc)
            throws VisADException, RemoteException {
        node.setLocation(loc);
    }

    /**
     * <p>Sets the input time.</p>
     *
     * <p>This implementation merely passes the invocation to the wrapped
     * {@link SoundingDataNode}.</p>
     *
     * @param time                   The input time.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public void setTime(DateTime time)
            throws VisADException, RemoteException {
        node.setTime(time);
    }
}
