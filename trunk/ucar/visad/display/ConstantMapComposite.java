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
 * Provides support for a composite of adapted VisAD ConstantMap-s.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $
 */
public class ConstantMapComposite implements ConstantMaps {

    /** set of maps */
    private final SortedSet mapSet = new TreeSet();

    /** listeners for changes */
    private PropertyChangeSupport propertyListeners;

    /**
     * Constructs.
     */
    public ConstantMapComposite() {}

    /**
     * Constructs.
     *
     * @param maps              The initial {@link ConstantMaps} for this
     *                          instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ConstantMapComposite(ConstantMaps maps)
            throws VisADException, RemoteException {

        try {
            add(maps);
        } catch (BadMappingException e) {}  // can't happen because this instance is empty
    }

    /**
     * Adds adapted {@link ConstantMaps} to this instance.
     *
     * @param maps              The adapted {@link ConstantMaps} to
     *                          be added.
     * @throws BadMappingException
     *                          Addition of the {@link ConstantMaps}
     *                          would result in multiple values for a {@link
     *                          visad.DisplayRealType}.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void add(ConstantMaps maps)
            throws BadMappingException, VisADException, RemoteException {

        maps.accept(new ConstantMaps.Visitor() {

            public void visit(ConstantMapComposite theMaps)
                    throws BadMappingException, VisADException,
                           RemoteException {
                theMaps.accept(this);
            }

            public void visit(ConstantMapAdapter mapAdapter)
                    throws BadMappingException {

                if (mapSet.contains(mapAdapter)) {
                    throw new BadMappingException(getClass().getName()
                            + ".visit(): "
                            + "ConstantMapAdapter already exists: "
                            + mapAdapter);
                }

                mapSet.add(mapAdapter);
            }
        });
    }

    /**
     * Removes adapted {@link ConstantMaps} from this instance.
     *
     * @param maps              The adapted {@link ConstantMaps}  to be
     *                          removed.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void remove(ConstantMaps maps)
            throws VisADException, RemoteException {

        maps.accept(new ConstantMaps.Visitor() {

            public void visit(ConstantMapComposite theMaps)
                    throws VisADException, RemoteException {
                theMaps.accept(this);
            }

            public void visit(ConstantMapAdapter mapAdapter) {
                mapSet.remove(mapAdapter);
            }
        });
    }

    /**
     * Accepts a {@link ConstantMaps.Visitor} to this instance.
     *
     * @param visitor           The {@link ConstantMaps.Visitor} to accept.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void accept(ConstantMaps.Visitor visitor)
            throws VisADException, RemoteException {

        for (Iterator iter = mapSet.iterator(); iter.hasNext(); ) {
            ((ConstantMapAdapter) iter.next()).accept(visitor);
        }
    }

    /**
     * Returns the {@link ConstantMap}(s) of this instance.
     *
     * @return                  The {@link ConstantMap}(s) of this instance.
     */
    public ConstantMap[] getConstantMaps() {
        return (ConstantMap[]) mapSet.toArray(new ConstantMapAdapter[0]);
    }

    /**
     * Adds the adapted {@link ConstantMap}(s) of this instance to a VisAD
     * display.  This method should only be invoked by a DisplayAdapter.
     *
     * @param display           The VisAD display.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void setDisplay(Display display)
            throws VisADException, RemoteException {

        for (Iterator iter = mapSet.iterator(); iter.hasNext(); ) {
            ((ConstantMapAdapter) iter.next()).setDisplay(display);
        }
    }

    /**
     * Adds a PropertyChangeListener to this instance.
     *
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {

        for (Iterator iter = mapSet.iterator(); iter.hasNext(); ) {
            ((ConstantMapAdapter) iter.next()).addPropertyChangeListener(
                listener);
        }
    }

    /**
     * Adds a PropertyChangeListener for a named property to this instance.
     *
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    public void addPropertyChangeListener(String name,
                                          PropertyChangeListener listener) {

        for (Iterator iter = mapSet.iterator(); iter.hasNext(); ) {
            ((ConstantMapAdapter) iter.next()).addPropertyChangeListener(
                name, listener);
        }
    }

    /**
     * Removes a PropertyChangeListener from this instance.
     *
     * @param listener          The PropertyChangeListener to be removed.
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {

        for (Iterator iter = mapSet.iterator(); iter.hasNext(); ) {
            ((ConstantMapAdapter) iter.next()).removePropertyChangeListener(
                listener);
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

        for (Iterator iter = mapSet.iterator(); iter.hasNext(); ) {
            ((ConstantMapAdapter) iter.next()).removePropertyChangeListener(
                name, listener);
        }
    }

    /**
     * Fires a PropertyChangeEvent.
     *
     * @param event             The PropertyChangeEvent.
     */
    protected void firePropertyChange(PropertyChangeEvent event) {

        if (propertyListeners != null) {
            propertyListeners.firePropertyChange(event);
        }
    }

    /**
     * Fires a PropertyChangeEvent.
     *
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
