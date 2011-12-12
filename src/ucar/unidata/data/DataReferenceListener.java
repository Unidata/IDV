/*
 * $Id: DataReferenceListener.java,v 1.14 2006/12/01 20:41:21 jeffmc Exp $
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


import visad.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;


/**
 * A class for listening to changes in DataReferences
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.14 $
 */
public class DataReferenceListener extends DataReferenceImpl implements ThingChangedListener {


    /**
     * Create a new DataReferenceListener
     *
     * @param name   name of the reference
     *
     * @throws VisADException   problem creating this object
     */
    public DataReferenceListener(String name) throws VisADException {
        super(name);
    }

    /**
     * Called when the data in the reference changes
     *
     * @param e    change event
     * @return  true
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public boolean thingChanged(ThingChangedEvent e)
            throws VisADException, RemoteException {
        return true;
    }

    /**
     * Add a DataReference to this listener
     *
     * @param dataRef    DataReference to add
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    public void addDataReference(DataReference dataRef)
            throws VisADException, RemoteException {
        dataRef.addThingChangedListener(this, 0);
    }


}

