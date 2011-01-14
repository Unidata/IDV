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


import visad.*;



import java.beans.*;

import java.rmi.RemoteException;

import java.util.*;


/**
 * Provides support for adapting VisAD ScalarMap-s and ConstantMap-s.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $
 */
public abstract class MapAdapter implements Propertied, Comparable {

    /** listeners for property changes */
    private PropertyChangeSupport propertyListeners;

    /** DisplayRealType for the ScalarMap */
    private final DisplayRealType displayRealType;

    /**
     * Constructs.
     * @param drt               The DisplayRealType to be associated with this
     *                          adapter.
     */
    protected MapAdapter(DisplayRealType drt) {
        displayRealType = drt;
    }

    /**
     * Returns the {@link visad.DisplayRealType} of this instance.
     *
     * @return                  This instance's DisplayRealType.
     */
    public final DisplayRealType getDisplayRealType() {
        return displayRealType;
    }

    /**
     * Adds a PropertyChangeListener to this instance.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        getPropertyListeners().addPropertyChangeListener(listener);
    }

    /**
     * Adds a PropertyChangeListener for a named property to this instance.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {
        getPropertyListeners().addPropertyChangeListener(name, listener);
    }

    /**
     * Removes a PropertyChangeListener from this instance.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {

        if (propertyListeners != null) {
            propertyListeners.removePropertyChangeListener(listener);
        }
    }

    /**
     * Removes a PropertyChangeListener for a named property from this instance.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            String name, PropertyChangeListener listener) {

        if (propertyListeners != null) {
            propertyListeners.removePropertyChangeListener(name, listener);
        }
    }

    /**
     * Returns the PropertyChangeListener-s of this instance.
     * @return                  The PropertyChangeListener-s.
     */
    private PropertyChangeSupport getPropertyListeners() {

        if (propertyListeners == null) {
            synchronized (this) {
                if (propertyListeners == null) {
                    propertyListeners = new PropertyChangeSupport(this);
                }
            }
        }

        return propertyListeners;
    }

    /**
     * Fires a PropertyChangeEvent.
     * @param event             The PropertyChangeEvent.
     */
    protected void firePropertyChange(PropertyChangeEvent event) {

        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(event);
        }
    }

    /**
     * Fires a PropertyChangeEvent.
     * @param propertyName      The name of the property.
     * @param oldValue          The old value of the property.
     * @param newValue          The new value of the property.
     */
    protected void firePropertyChange(String propertyName, Object oldValue,
                                      Object newValue) {

        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(propertyName, oldValue,
                    newValue);
        }
    }
}
