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

package ucar.visad.display;


import visad.VisADException;

import java.beans.PropertyChangeEvent;


import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;


/**
 * SelectorDisplayable is an abstract class that manages a list
 * of PropertyChangeListeners. It is used by CrossSectionSelector
 * and others to fire events.
 *
 * @author Metapps development team
 * @version $Revision: 1.10 $
 */
public abstract class SelectorDisplayable extends CompositeDisplayable {

    /** position property */
    public static final String PROPERTY_POSITION = "SelectorDisplay.position";

    /** flag for whether we are fire events */
    private boolean firingEvent = false;

    /** flag for whether it's okay to fire events */
    private boolean okToFireEvent = true;

    /** point size */
    private float pointSize = 1.0f;

    /** flag for autosizing */
    private boolean autoSize = false;

    /**
     * Simple constructor
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public SelectorDisplayable() throws VisADException, RemoteException {}

    /**
     * Construct a SelectorDisplayable from another instance
     *
     * @param that  other instance
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public SelectorDisplayable(SelectorDisplayable that)
            throws VisADException, RemoteException {
        super(that);
    }

    /**
     * Set whether the marker should automatically resize as the
     * display is zoomed.
     * @param yesorno  true to automatically resize the marker.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setAutoSize(boolean yesorno)
            throws VisADException, RemoteException {
        autoSize = yesorno;
    }

    /**
     * Get whether we are autosizing the point as we zoom and pan
     * @return  true if autosize is set.
     */
    public boolean getAutoSize() {
        return autoSize;
    }


    /**
     * Set the point size of the selector
     *
     * @param size  size of the selector point
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setPointSize(float size)
            throws VisADException, RemoteException {
        super.setPointSize(size);
        pointSize = size;
    }

    /**
     * Get the point size
     * @return  the point size
     */
    public float getPointSize() {
        return pointSize;
    }

    /**
     * Get whether we are in the process of firing events
     * @return  true if we are in the process of firing events
     */
    public boolean getFiringEvent() {
        return firingEvent;
    }


    /**
     * Get whether events should be fired or not
     * @return true if it's okay to fire events
     */
    public boolean getOkToFireEvents() {
        return okToFireEvent;
    }

    /**
     * Set whether events should be fired or not
     *
     * @param v  true if it's okay to fire events
     */
    public void setOkToFireEvents(boolean v) {
        okToFireEvent = v;
    }

    /**
     * Adds a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     */
    public void addPropertyChangeListener(PropertyChangeListener action) {
        super.addPropertyChangeListener(PROPERTY_POSITION, action);
    }

    /**
     * Removes a listener for data changes.
     *
     * @param action              The listener for changes to the underlying
     *                            data.
     */
    public void removePropertyChangeListener(PropertyChangeListener action) {
        super.removePropertyChangeListener(PROPERTY_POSITION, action);
    }

    /**
     * Wrapper method that around the base class firePropertyChange
     */
    protected void notifyListenersOfMove() {
        if ( !okToFireEvent) {
            return;
        }

        firingEvent = true;
        firePropertyChange(PROPERTY_POSITION, null, null);
        firingEvent = false;
    }
}
