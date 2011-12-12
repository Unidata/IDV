/*
 * $Id: DataInstance.java,v 1.47 2006/12/01 20:41:21 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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


package ucar.unidata.data;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.ObjectArray;


import ucar.unidata.util.ThreeDSize;

import ucar.visad.quantities.CommonUnits;



import visad.*;

import java.rmi.RemoteException;

import java.util.Hashtable;


/**
 * A superclass for data instances.  It's a wrapper for a Data object
 * that allows quick access to metadata about it.
 * <p>This class is thread-compatible.</p>
 *
 * @author IDV development team
 * @version $Revision: 1.47 $
 */
public class DataInstance {

    /** logging category */
    protected static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            DataInstance.class.getName());

    /** next instance id */
    private static int nextId = 0;

    /** locking mutex */
    private static Object ID_MUTEX = new Object();

    /** flag for initialization */
    protected boolean haveBeenInitialized = false;

    /** flag for errors */
    protected boolean inError = false;


    //the objects needed to make one of these objects

    /** The {@link DataChoice} associated with the instance */
    protected DataChoice dataChoice;

    /** The data associated with the DataChoice */
    private Data data;

    /** The dataselection for the choice */
    protected DataSelection dataSelection;

    /** Extra request properties */
    protected Hashtable myRequestProperties;


    /**
     * Create a new DataInstance.
     *
     * @param dataChoice          choice for data
     * @param dataSelection       sub-selection criteria
     * @param requestProperties   extra request properties
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public DataInstance(DataChoice dataChoice, DataSelection dataSelection,
                        Hashtable requestProperties)
            throws VisADException, RemoteException {
        this(dataChoice, dataSelection, requestProperties, null);
    }



    /**
     * Create a new DataInstance.
     *
     * @param dataChoice          choice for data
     * @param dataSelection       sub-selection criteria
     * @param requestProperties   extra request properties
     * @param theData             Any initial data. Usually is null.
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public DataInstance(DataChoice dataChoice, DataSelection dataSelection,
                        Hashtable requestProperties, Data theData)
            throws VisADException, RemoteException {
        this.myRequestProperties = requestProperties;
        this.dataSelection       = dataSelection;
        this.dataChoice          = dataChoice;
        this.data                = theData;
    }

    /**
     * Initialize the instance
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected void init() throws VisADException, RemoteException {
        haveBeenInitialized = true;
    }

    /**
     * Does this data instance need to be initialized
     *
     * @return needs to be initialized
     */
    public boolean needsInitialization() {
        return !haveBeenInitialized;

    }

    /**
     * Check if this has been initialized.  If not, initialize.
     */
    protected void checkInit() {
        if ( !haveBeenInitialized) {
            try {
                init();
            } catch (Exception exc) {
                LogUtil.logException("Initialization of DataInstance", exc);
            }
        }
    }

    /**
     * Reinitilize this instance.  Set internal fields to null and
     * call init()
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public synchronized void reInitialize()
            throws VisADException, RemoteException {
        inError             = false;
        data                = null;
        haveBeenInitialized = false;
        init();
    }


    public DataSelection getDataSelection() {
        return dataSelection;
    }

    /**
     * Get the {@link DataChoice} associated with this instance.
     *
     * @return  associated DataChoice
     */
    public DataChoice getDataChoice() {
        return dataChoice;
    }

    /**
     * Have this be setTheData so it is not the same signature as getData
     * in case this gets persisted
     *
     * @param d    data to use
     */
    public void setTheData(Data d) {
        data = d;
    }


    /**
     * Calls getData, passing in the member DataSelection
     *
     * @return   the Data for this instance
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public Data getData() throws VisADException, RemoteException {
        return getData(dataSelection, myRequestProperties);
    }

    /**
     * Get the data using the specified sub selection.
     *
     * @param dataSelection    sub selection criteria
     * @return  the data
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public Data getData(DataSelection dataSelection)
            throws VisADException, RemoteException {
        return getData(dataSelection, myRequestProperties);
    }

    /**
     * Get the data using the specified sub selection and extra
     * request properties
     *
     * @param dataSelection       sub selection criteria
     * @param requestProperties   extra request properties
     * @return  the data specific to the request
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public Data getData(DataSelection dataSelection,
                        Hashtable requestProperties)
            throws VisADException, RemoteException {

        checkInit();
        //TODO: We cached the Data from a prior call 
        // but what if the dataSelection has changed.
        if (data == null) {
            data = dataChoice.getData(dataSelection, requestProperties);
            if (data == null) {
                inError = true;
            }
        }
        return data;
    }



    /**
     * _more_
     *
     * @param newData _more_
     */
    protected void setData(Data newData) {
        data = newData;
    }

    /**
     * Set the data selection
     *
     * @param dataSelection the data selection
     */
    public void setDataSelection(DataSelection dataSelection) {
        this.dataSelection = dataSelection;
    }

    /**
     * Clear any cached data held by this DataInstance
     *
     * public void clearCache () {
     *   data = null;
     * }
     * @return
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */

    /**
     * Is the data held by this DataInstance ok. That is,  is it non-null.
     *
     * @return  true if the data is okay
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public boolean dataOk() throws VisADException, RemoteException {
        if (inError) {
            return false;
        }
        return (getData() != null);
    }

    /**
     * Get the parameter name associated with this instance.
     * @return  name of the pararmeter
     */
    public String getParamName() {
        return dataChoice.getName();
    }

    /**
     * Get the next instance id.
     * @return  next instance id
     */
    protected static int getNextId() {
        synchronized (ID_MUTEX) {
            return nextId++;
        }
    }



}

