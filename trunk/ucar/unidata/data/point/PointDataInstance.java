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

package ucar.unidata.data.point;


import edu.wisc.ssec.mcidas.McIDASUtil;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;

import ucar.unidata.util.Trace;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;


import java.rmi.RemoteException;

import java.util.Hashtable;



/**
 * Utility class to wrap a Point Data Choice
 */
public class PointDataInstance extends DataInstance {

    /** point observations */
    public FieldImpl pointObs = null;

    /** point observations timeSequence */
    private FieldImpl pointObsSequence = null;

    /**
     * Create a new PointDataInstance from the parameters
     *
     * @param choice              data choice describing the data
     * @param selection           selection criteria
     * @param requestProperties   extra initialization properties
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public PointDataInstance(DataChoice choice, DataSelection selection,
                             Hashtable requestProperties)
            throws VisADException, RemoteException {
        super(choice, selection, requestProperties);
        init();
    }

    /**
     * Initialize the class and populate the fields
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void init() throws VisADException, RemoteException {
        if (haveBeenInitialized) {
            return;
        }
        super.init();
        if (dataOk()) {
            pointObs = (FieldImpl) getData();
        }
        pointObsSequence = null;
    }

    /**
     * Reinitializae this GridDataInstance
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void reInitialize() throws VisADException, RemoteException {
        pointObs         = null;
        pointObsSequence = null;
        super.reInitialize();
    }

    /**
     * Get the data as a time sequence
     * @return   time sequence of PointObs
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public FieldImpl getTimeSequence()
            throws VisADException, RemoteException {
        checkInit();
        FieldImpl pointObs = getPointObs();
        //        ucar.unidata.util.Trace.call1("makeTimeSequence");
        if (pointObsSequence == null) {
            pointObsSequence =
                PointObFactory.makeTimeSequenceOfPointObs(pointObs);
        }
        //        ucar.unidata.util.Trace.call2("makeTimeSequence");
        return pointObsSequence;
    }

    /**
     * Get the data as a time sequence.  Only use points inside the
     * bounding box
     *
     * @param bounds   lat/lon boundds
     * @return   time sequence of PointObs in the bounds
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public FieldImpl getTimeSequence(LinearLatLonSet bounds)
            throws VisADException, RemoteException {
        if (bounds == null) {
            return getTimeSequence();
        }
        checkInit();
        return PointObFactory.subSet(getTimeSequence(), bounds);
    }

    /**
     * Get the data
     * @return   the data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public FieldImpl getPointObs() throws VisADException, RemoteException {
        checkInit();
        if (pointObs == null) {
            pointObs = (FieldImpl) getData();
        }
        return pointObs;
    }

    /**
     * Get the data, using only points inside the bounding box
     *
     * @param bounds   lat/lon boundds
     * @return   PointObs in the bounds
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public FieldImpl getPointObs(LinearLatLonSet bounds)
            throws VisADException, RemoteException {
        Trace.msg("getPointObs-1");
        if (bounds == null) {
            return getPointObs();
        }
        Trace.msg("getPointObs-2");
        checkInit();
        Trace.msg("getPointObs-3");
        FieldImpl result = PointObFactory.subSet(getPointObs(), bounds);
        Trace.msg("getPointObs-1");
        return result;
    }

}
