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


import ucar.visad.Util;

import visad.*;

import visad.georef.LatLonTuple;
import visad.georef.NamedLocationTuple;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


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
    public void setData1(Data data) throws VisADException, RemoteException {
        Tuple tuple = (Tuple) data;
        setData(tuple);
    }

    /**
     * _more_
     *
     * @param data _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setData(Tuple data) throws VisADException, RemoteException {

        Tuple tuple = data;

        SingletonSet outTimes = new SingletonSet(new RealTuple(new Real[] {
                                    (DateTime) tuple.getComponent(0) }));
        NamedLocationTuple station =
            (NamedLocationTuple) tuple.getComponent(1);
        //EarthLocationTuple loc3 = (EarthLocationTuple) tuple.getComponent(1);
        LatLonTuple outLoc = new LatLonTuple(station.getLatitude(),
                                             station.getLongitude());
        SingletonSet outLocs  = new SingletonSet(outLoc);
        Field[]      tempPros = new Field[] { (Field) tuple.getComponent(2) };
        Field[]      dewPros  = new Field[] { (Field) tuple.getComponent(3) };
        Field[]      windPros = new Field[] { (Field) tuple.getComponent(4) };

        setOutputTimes(outTimes);
        setOutputLocation(outLoc);
        setOutputLocations(outLocs);
        setOutputProfiles(tempPros, dewPros, windPros);
    }

    /** _more_ */
    private DateTime[] dateTimes;

    /** _more_ */
    private List stations;

    /** _more_ */
    private Hashtable<String, List> stationsTuples;

    /** _more_ */
    private String[] stationIds;

    /**
     * _more_
     *
     * @param data _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setData(Data data) throws VisADException, RemoteException {
        Tuple   tuple    = (Tuple) data;
        Field[] tempPros = new Field[tuple.getDimension()];
        Field[] dewPros  = new Field[tempPros.length];
        Field[] windPros = new Field[tempPros.length];
        dateTimes      = new DateTime[tempPros.length];
        stationIds     = new String[tempPros.length];
        stations       = new ArrayList();
        stationsTuples = new Hashtable<String, List>();
        LatLonTuple[] outLoc = new LatLonTuple[tempPros.length];
        //  SingletonSet outLocs = new SingletonSet[tempPros.length];

        for (int i = 0; i < tempPros.length; i++) {

            Tuple ob = (Tuple) tuple.getComponent(i);

            dateTimes[i] = ((DateTime) ob.getComponent(0));

            NamedLocationTuple station =
                (NamedLocationTuple) ob.getComponent(1);
            stationIds[i] = station.getIdentifier().toString() + " "
                            + ((DateTime) ob.getComponent(0)).toString();

            if ( !stations.contains(station.getIdentifier().toString())) {
                stations.add(station.getIdentifier().toString());
            }
            List tupleList = stationsTuples.get(stationIds[i]);
            if (tupleList == null) {
                tupleList = new ArrayList();
                stationsTuples.put(station.getIdentifier().toString(),
                                   tupleList);
            }
            tupleList.add(ob);

            //EarthLocationTuple loc3 = (EarthLocationTuple) tuple.getComponent(1);
            outLoc[i] = new LatLonTuple(station.getLatitude(),
                                        station.getLongitude());
            //     outLocs.add( new SingletonSet(outLoc[i]));
            tempPros[i] = (Field) ob.getComponent(2);
            dewPros[i]  = (Field) ob.getComponent(3);
            windPros[i] = (Field) ob.getComponent(4);
        }
        setOutputTimes((SampledSet) getDataTimeSet(dateTimes));
        //   setOutputLocation(outLoc);
        setOutputLocations(tempPros[0]);
        setOutputProfiles(tempPros, dewPros, windPros);
    }

    /**
     * _more_
     *
     * @param time _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void setTime(DateTime time)
            throws VisADException, RemoteException {

        if (time == null) {
            throw new NullPointerException();
        }

        boolean notify = false;

        synchronized (this) {
            if ( !time.equals(inTime)) {
                inTime = time;
                notify = true;
            }
        }
        int idx = getTimeIndex(time);
        if (notify) {
            setOutputTimeIndex(idx);
        }
    }

    /**
     * _more_
     *
     * @param time _more_
     *
     * @return _more_
     */
    protected int getTimeIndex(DateTime time) {
        int i = 0;
        for (DateTime dt : dateTimes) {
            if (dt.getValue() == time.getValue()) {
                return i;
            }
            i++;
        }
        return 0;
    }

    /**
     * _more_
     *
     * @param data _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected Set getDataTimeSet(DateTime[] data)
            throws RemoteException, VisADException {
        Set  aniSet = null;
        List times  = null;

        times = new ArrayList();
        for (DateTime t : data) {
            // ((DateTime));
            if ( !times.contains(t)) {
                times.add(t);
            }
        }
        aniSet = Util.makeTimeSet(times);

        return aniSet;
    }

    /**
     * _more_
     *
     * @param data _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    final void setOutputLocations(Field data)
            throws VisADException, RemoteException {

        SampledSet locs = null;

        synchronized (this) {
            if (data != null) {
                locs = (SampledSet) data.getDomainSet();
            }
        }

        if (locs != null) {
            setOutputLocations(locs);
        }
    }
}
