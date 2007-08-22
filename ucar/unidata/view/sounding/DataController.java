/*
 * $Id: DataController.java,v 1.12 2005/05/13 18:33:27 jeffmc Exp $
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
 * Provides support for mediating interactions between a (mutable) sounding
 * database and views of the database.
 *
 * @author Steven R. Emmerson
 * @version $Id: DataController.java,v 1.12 2005/05/13 18:33:27 jeffmc Exp $
 */
public abstract class DataController {

    /** database of data */
    private DataModel database;

    /**
     * Constructs from a database.
     * @param database          The data model (i.e. database).
     */
    public DataController(DataModel database) {

        this.database = database;

        database.addPropertyChangeListener(database.SELECTED_INDEX,
                                           new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {

                try {
                    selectedIndexChange(
                        ((Integer) event.getNewValue()).intValue());
                } catch (Exception e) {
                    System.err.println(
                        getClass().getName() + ".propertyChange(): "
                        + "Couldn't handle change to selected sounding: "
                        + e);
                }
            }
        });
        database.addListDataListener(new ListDataListener() {

            /*
             * Keeps the views consistent with the database.
             */
            public void contentsChanged(ListDataEvent event) {

                try {
                    synchronized (DataController.this) {
                        removeAllDataDisplayables();

                        int last = DataController.this.database.getSize() - 1;

                        addDataDisplayables(0, last);

                        int index =
                            DataController.this.database.getSelectedIndex();

                        if (index < DataController.this.database.getSize()) {
                            selectedIndexChange(index);
                        }
                    }
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".contentsChanged(): "
                        + "Couldn't display change to sounding database: "
                        + e);
                }
            }

            public void intervalAdded(ListDataEvent event) {

                try {
                    synchronized (DataController.this) {
                        addDataDisplayables(event.getIndex0(),
                                            event.getIndex1());

                        int index =
                            DataController.this.database.getSelectedIndex();

                        if (index < DataController.this.database.getSize()) {
                            selectedIndexChange(index);
                        }
                    }
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".intervalAdded(): "
                        + "Couldn't add database interval to display: " + e);

                    // e.printStackTrace();
                }
            }

            public void intervalRemoved(ListDataEvent event) {

                try {
                    synchronized (DataController.this) {
                        removeDataDisplayables(event.getIndex0(),
                                               event.getIndex1());

                        int index =
                            DataController.this.database.getSelectedIndex();

                        if (index < DataController.this.database.getSize()) {
                            selectedIndexChange(index);
                        }
                    }
                } catch (Exception e) {
                    System.err.println(
                        this.getClass().getName() + ".intervalRemoved(): "
                        + "Couldn't remove database interval from display: "
                        + e);
                }
            }
        });
        database.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent event) {

                if ( !event.getValueIsAdjusting()) {
                    try {
                        setVisibility();
                    } catch (Exception e) {
                        System.err.println(
                            getClass().getName() + ".valueChanged(): "
                            + "Couldn't set visibility of displayables: "
                            + e);
                    }
                }
            }
        });
    }

    /**
     * Adds an interval of displayed data to the view.
     * @param index0            The first index of the interval, inclusive.
     * @param index1            The last index of the interval, inclusive.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void addDataDisplayables(int index0, int index1)
            throws VisADException, RemoteException {

        for (int i = index0; i <= index1; ++i) {
            addDataDisplayable(i);
            setVisibility(i, false);
        }
    }

    /**
     * Removes an interval of displayed data from the view.
     * @param index0            One index of the interval, inclusive.
     * @param index1            The other index of the interval, inclusive.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void removeDataDisplayables(int index0, int index1)
            throws VisADException, RemoteException {

        /**
         * The indices are traversed in descending order to obviate problems
         * due to compaction.
         */
        for (int i = Math.max(index0, index1); i >= Math.min(index0, index1);
                --i) {
            removeDataDisplayable(i);
        }
    }

    /**
     * Sets the visiblity of the displayed data.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected synchronized void setVisibility()
            throws VisADException, RemoteException {

        int count = database.getSize();

        for (int i = 0; i < count; ++i) {
            setVisibility(i, database.isSelectedIndex(i));
        }
    }

    /**
     * Handles a change to the index of the selected sounding.
     * @param index             The index of the selected sounding.  A value of
     *                          -1 means that there is no selected sounding.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void selectedIndexChange(int index)
     throws VisADException, RemoteException;

    /**
     * Adds a given datum to the view.
     * @param index             The index of the datum.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void addDataDisplayable(int index)
     throws VisADException, RemoteException;

    /**
     * Removes all displayed data from the view.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void removeAllDataDisplayables()
     throws VisADException, RemoteException;

    /**
     * Removes a given displayed datum from the view.
     * @param index             The index of the displayed datum.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void removeDataDisplayable(int index)
     throws VisADException, RemoteException;

    /**
     * Sets the visibility of a given displayed datum.
     * @param index             The index of the displayed datum.
     * @param visible           Whether or not the datum should be visible.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected abstract void setVisibility(int index, boolean visible)
     throws VisADException, RemoteException;
}







