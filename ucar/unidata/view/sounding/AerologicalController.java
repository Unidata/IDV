/*
 * $Id: AerologicalController.java,v 1.10 2005/05/13 18:33:21 jeffmc Exp $
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

package ucar.unidata.view.sounding;



import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import ucar.unidata.data.sounding.*;

import visad.*;


/**
 * Provides support for mediating interactions between a (mutable) aerological
 * sounding database and views of the data in an aerological display.
 *
 * @author Steven R. Emmerson
 * @version $Id: AerologicalController.java,v 1.10 2005/05/13 18:33:21 jeffmc Exp $
 */
public class AerologicalController extends DataController {

    /** display to control */
    private AerologicalDisplay display;

    /** data model */
    private SoundingDataModel database;

    /**
     * Constructs from an aerological database and aerological display.
     * @param database          The aerological database.
     * @param display           The aerological display.
     */
    public AerologicalController(SoundingDataModel database,
                                 AerologicalDisplay display) {

        super(database);

        this.database = database;
        this.display  = display;
    }

    /**
     * Handles a change to the index of the selected sounding.
     * @param index             The index of the selected sounding.  A values of
     *                          -1 means that there is no selected sounding.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void selectedIndexChange(int index)
            throws VisADException, RemoteException {
        display.setActiveSounding(index);
    }

    /**
     * Adds a given sounding to the display.
     * @param index             The index of the sounding.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void addDataDisplayable(int index)
            throws VisADException, RemoteException {

        Field[] fields = database.getSounding(index);

        display.addProfile(index, fields[0], fields[1], null);
    }

    /**
     * Removes all displayed data from the view.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void removeAllDataDisplayables()
            throws VisADException, RemoteException {
        display.removeProfiles();
    }

    /**
     * Removes a given displayed datum from the view.
     * @param index             The index of the displayed datum.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void removeDataDisplayable(int index)
            throws IndexOutOfBoundsException, VisADException,
                   RemoteException {
        display.removeProfile(index);
    }

    /**
     * Sets the visibility of a given displayed datum.
     * @param index             The index of the displayed datum.
     * @param visible           Whether or not the datum should be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setVisibility(int index, boolean visible)
            throws VisADException, RemoteException {
        display.setProfileVisible(index, visible);
    }
}







