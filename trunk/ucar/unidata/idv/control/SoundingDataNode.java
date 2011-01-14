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

import visad.MathType;

import visad.SampledSet;

import visad.VisADException;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;



import java.rmi.RemoteException;


/**
 * Adapts various input data for an aerological diagram (e.g. a Skew-T) to a
 * common output API.  The input data are: 1) a data object that contains
 * at least one profile of temperature and dew-point and wind; 2) a time;
 * and 3) a geographical location.  The output data are: 1) the set of
 * all times associated with the input data object; 2) the index of the
 * element in the set of all times that is the current time; 3) the set
 * of all locations associated with the input data object; 4) the current
 * location; and 5) the sets of temperaturek, dew-point  and wind profiles
 * -- the current profile is given by the output time index.
 *
 * @author Steven R. Emmerson
 * @author Don Murray
 * @version $Revision: 1.4 $ $Date: 2006/12/01 20:16:37 $
 */
abstract class SoundingDataNode {

    /** listener for changes */
    private final Listener listener;

    /** time for data */
    protected DateTime inTime;

    /** location for data */
    protected LatLonPoint inLoc;

    /**
     * Constructs from an output listener.  The output time index will be set to
     * zero.
     *
     * @param listener               The object that will receive output from
     *                               this instance.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    SoundingDataNode(Listener listener)
            throws VisADException, RemoteException {

        if (listener == null) {
            throw new NullPointerException();
        }

        this.listener = listener;

        /*
         * Set the output time-index.
         */
        setOutputTimeIndex(0);
    }

    /**
     * Returns an instance of this class given a {@link Listener} for the
     * output.
     *
     * @param listener              The object that will receive the instance's
     *                              output.
     *
     * @return and instance of this class
     *
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public static SoundingDataNode getInstance(Listener listener)
            throws VisADException, RemoteException {
        return new SoundingDataNodeWrapper(listener);
    }

    /**
     * <p>Sets the input {@link visad.Data} object.</p>
     *
     * @param data                   The input data object.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public abstract void setData(Data data)
     throws VisADException, RemoteException;

    /**
     * <p>Sets the horizontal location.</p>
     *
     * <p>This implementation does nothing.</p>
     *
     * @param loc                    The horizontal location.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public void setLocation(LatLonPoint loc)
            throws VisADException, RemoteException {}

    /**
     * <p>Sets the input time.</p>
     *
     * <p>This implementation does nothing.</p>
     *
     * @param time                  The input time.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     */
    public void setTime(DateTime time)
            throws VisADException, RemoteException {}

    /**
     * Sets the time-index of the current output profile.  The actual time of
     * the current profile will be given by the <code>index</code>-th element of
     * the set of times.
     *
     * @param index                  The time-index of the current profile.
     * @throws VisADException        if a VisAD failure occurs.
     * @throws RemoteException       if a Java RMI failure occurs.
     * @see #setTimes(SampledSet, SoundingDataNode)
     */
    final void setOutputTimeIndex(int index)
            throws VisADException, RemoteException {
        listener.setTimeIndex(index, this);
    }

    /**
     * Sets the set of times of all the output profiles.  The set will
     * contain one or more times as double values, in order, from earliest
     * to latest. The index of the current time will be set by {@link
     * #setTimeIndex(int, SoundingDataNode).
     *
     * @param times              The times of all the profiles or <code>
     *                           null</code>.
     * @throws VisADException    if a VisAD failure occurs.
     * @throws RemoteException   if a Java RMI failure occurs.
     */
    final void setOutputTimes(SampledSet times)
            throws VisADException, RemoteException {
        listener.setTimes(times, this);
    }

    /**
     * Sets the current output location.
     *
     * @param loc                The current location or <code>null</code>.
     * @throws VisADException    if a VisAD failure occurs.
     * @throws RemoteException   if a Java RMI failure occurs.
     */
    final void setOutputLocation(LatLonPoint loc)
            throws VisADException, RemoteException {
        listener.setLocation(loc, this);
    }

    /**
     * Sets the set of output locations of the output profiles.  The set will
     * contain one or more {@link visad.georef.EarthLocationTuple}-s.
     *
     * @param locs               The locations of the profiles or <code>
     *                           null</code>.
     * @throws VisADException    if a VisAD failure occurs.
     * @throws RemoteException   if a Java RMI failure occurs.
     */
    final void setOutputLocations(SampledSet locs)
            throws VisADException, RemoteException {
        listener.setLocations(locs, this);
    }

    /**
     * Sets the set of output profiles.  The index of the current profile will
     * be set by {@link #setTimeIndex(int, SoundingDataNode)}.
     *
     * @param tempPros           The temperature profiles.
     * @param dewPros            The dew-point profile.
     * @param windPros
     * @throws VisADException    if a VisAD failure occurs.
     * @throws RemoteException   if a Java RMI failure occurs.
     */
    final void setOutputProfiles(Field[] tempPros, Field[] dewPros,
                                 Field[] windPros)
            throws VisADException, RemoteException {
        listener.setProfiles(tempPros, dewPros, windPros, this);
    }

    /**
     * API for receiving the output of a {@link SoundingDataNode}.
     *
     * @author Steven R. Emmerson
     * @version $Revision: 1.4 $ $Date: 2006/12/01 20:16:37 $
     */
    public interface Listener {

        /**
         * Sets the time-index of the current profile.  The actual time of the
         * current profile will be given by the <code>index</code>-th element
         * of the set of times.
         *
         * @param index              The time-index of the current profile.
         * @param source             The source of the output data.
         * @see #setTimes(SampledSet, SoundingDataNode)
         * @throws VisADException    if a VisAD failure occurs.
         * @throws RemoteException   if a Java RMI failure occurs.
         */
        void setTimeIndex(int index, SoundingDataNode source)
         throws VisADException, RemoteException;

        /**
         * Sets the set of times of all the output profiles.  The set will
         * contain one or more times as double values, in order, from earliest
         * to latest. The index of the current time will be set by {@link
         * #setTimeIndex(int, SoundingDataNode)}.
         *
         * @param times                  The times of all the profiles or <code>
         *                               null</code>.
         * @param source                 The source of the output data.
         * @see #setTimeIndex(int, SoundingDataNode)
         * @throws VisADException        if a VisAD failure occurs.
         * @throws RemoteException       if a Java RMI failure occurs.
         */
        void setTimes(SampledSet times, SoundingDataNode source)
         throws VisADException, RemoteException;

        /**
         * Sets the current location.
         *
         * @param loc                    The current location or <code>null</code>.
         * @param source                 The source of the output data.
         * @throws VisADException        if a VisAD failure occurs.
         * @throws RemoteException       if a Java RMI failure occurs.
         */
        void setLocation(LatLonPoint loc, SoundingDataNode source)
         throws VisADException, RemoteException;

        /**
         * Sets the set of locations of the output profiles.  The set will
         * contain one or more {@link visad.georef.EarthLocationTuple}-s.
         *
         * @param locs                   The locations of the profiles or <code>
         *                               null</code>.
         * @param source                 The source of the output data.
         * @throws VisADException        if a VisAD failure occurs.
         * @throws RemoteException       if a Java RMI failure occurs.
         */
        void setLocations(SampledSet locs, SoundingDataNode source)
         throws VisADException, RemoteException;

        /**
         * Sets the set of temperature profiles.  The {@link visad.MathType} of
         * each profile will be (AirPressure -> AirTemperature).  The index
         * of the current profile will be set by {@link #setTimeIndex(int,
         * SoundingDataNode)}.
         *
         * @param tempPros               The temperature profiles.
         * @param dewPros                The dew-point profiles.
         * @param windPros               The wind profiles.
         * @param source                 The source of the output data.
         * @throws VisADException        if a VisAD failure occurs.
         * @throws RemoteException       if a Java RMI failure occurs.
         */
        void setProfiles(Field[] tempPros, Field[] dewPros, Field[] windPros,
                         SoundingDataNode source)
         throws VisADException, RemoteException;
    }
}
