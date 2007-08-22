/*
 * $Id: WindController.java,v 1.11 2005/05/13 18:33:41 jeffmc Exp $
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
 * Provides support for mediating interactions between a (mutable) wind profile
 * database and views of the wind-data in a wind-profile display.
 *
 * @author Steven R. Emmerson
 * @version $Id: WindController.java,v 1.11 2005/05/13 18:33:41 jeffmc Exp $
 */
public class WindController extends DataController {

    /** wind display */
    private WindProfileDisplay display;

    /** wind data */
    private WindDataModel database;

    /**
     * Constructs from an wind-profile database and wind-profile display.
     * @param database          The wind-profile database.
     * @param display           The wind-profile display.
     */
    public WindController(WindDataModel database,
                          WindProfileDisplay display) {

        super(database);

        this.database = database;
        this.display  = display;
    }

    /**
     * Handles a change to the index of the selected wind-profile.
     * @param index             The index of the selected wind-profile.  A
     *                          values of -1 means that there is no selected
     *                          wind-profile.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void selectedIndexChange(int index)
            throws VisADException, RemoteException {
        display.setActiveWindProfile(index);
        display.setActiveMeanWind(index);
    }

    /**
     * Adds a given displayed datum to the view.
     * @param index             The index of the displayed datum.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void addDataDisplayable(int index)
            throws VisADException, RemoteException {
        display.addProfile(index, database.getWindProfile(index));
        display.setMeanWind(index, database.getMeanWindRef(index));
    }

    /**
     * Removes all displayed data from the view.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void removeAllDataDisplayables()
            throws VisADException, RemoteException {
        display.clear();
    }

    /**
     * Removes a given displayed datum from the view.
     * @param index             The index of the displayed datum.
     * @throws IndexOutOfBoundsException
     *                          The index is out of range.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void removeDataDisplayable(int index)
            throws IndexOutOfBoundsException, VisADException,
                   RemoteException {
        display.removeProfile(index);
        display.removeMeanWind(index);
    }

    /**
     * Sets the visibility of a given displayed datum.
     * @param index             The index of the displayed datum.
     * @param visible           Whether or not the datum should be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void setVisibility(int index, boolean visible)
            throws VisADException, RemoteException {
        display.setProfileVisible(index, visible);
        display.setMeanWindVisible(index, visible);
    }
}







