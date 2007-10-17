/*
 * $Id: DataChoice.java,v 1.86 2007/06/21 14:44:58 jeffmc Exp $
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


import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlUtil;


import visad.*;

import visad.georef.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


/**
 * A data choice that simply holds a reference to a visad.Data object
 *
 * @author IDV development team
 * @version $Revision: 1.86 $
 */
public class DataDataChoice extends DataChoice {


    /** The data */
    private Data data;


    /**
     *  The bean constructor. We need this for xml decoding.
     */
    public DataDataChoice() {}


    /**
     * Create a new DataChoice, using the state of the given DataChoice to
     * initialize the new object.
     *
     * @param other      The other data choice.
     */
    public DataDataChoice(DataDataChoice other) {
        super(other);
        this.data = other.data;
    }


    /**
     *  Create a new DataChoice.
     *
     *  @param name The short name of this choice.
     * @param data The data
     */
    public DataDataChoice(String name, Data data) {
        super(name, name, null, null);
        this.data = data;
    }



    /**
     * Clone me
     *
     * @return my clone
     */
    public DataChoice cloneMe() {
        return new DataDataChoice(this);
    }


    /**
     * Return the {@link visad.Data} object that this DataChoice represents.
     *
     * @param category          The {@link DataCategory} used to subset this
     *                          call (usually not used but  placed in here
     *                          just in case it is needed.)
     * @param dataSelection     Allows one to subset the data request (e.g.,
     *                          asking for a smaller set of times, etc.)
     * @param requestProperties Extra selection properties
     *
     * @return The data.
     *
     * @throws DataCancelException   if the request to get data is canceled
     * @throws RemoteException       problem accessing remote data
     * @throws VisADException        problem creating the Data object
     */
    protected Data getData(DataCategory category,
                           DataSelection dataSelection,
                           Hashtable requestProperties)
            throws VisADException, RemoteException, DataCancelException {
        return data;
    }

    /**
     * add listener. This is a noop
     *
     * @param listener listener
     */
    public void addDataChangeListener(DataChangeListener listener) {}


    /**
     * Remove the {@link DataChangeListener}.
     *
     * @param listener The {@link DataChangeListener} to remove.
     */
    public void removeDataChangeListener(DataChangeListener listener) {}



}

