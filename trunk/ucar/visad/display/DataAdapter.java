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

import java.util.WeakHashMap;


/**
 * Provides support for adapting VisAD data objects into something that is
 * (hopefully) easier to use.  This class aggregates a VisAD Display, a
 * DataReference, a DataRenderer, and associated ConstantMap-s into a single
 * entity.
 *
 * <p>Instances of this class have the following, bound, JavaBean
 * properties:<br>
 * <table border align=center>
 *
 * <tr>
 * <th>Name</th>
 * <th>Type</th>
 * <th>Access</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 *
 * <tr align=center>
 * <td>constantMap</td>
 * <td>{@link visad.ConstantMap}</td>
 * <td></td>
 * <td>construction-dependent</td>
 * <td align=left>Fired whenever any {@link visad.ConstantMap} changes
 * </td>
 * </tr>
 *
 * </table>
 * {@link java.beans.PropertyChangeEvent}-s fired for the
 * {@link #CONSTANT_MAP} property have the originating instance of this
 * class as the source and the old and new values set to the appropriate
 * {@link visad.ConstantMap}s.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.9 $
 */
public class DataAdapter implements Propertied {

    /**
     * The name of the {@link visad.ConstantMap} "property".
     */
    public static final String CONSTANT_MAP = "constantMap";

    /** DataReference for data */
    private final DataReference dataReference;

    /** ConstantMaps for the ref */
    private final ConstantMaps constantMaps;

    /** Renderer for the data */
    private final DataRenderer dataRenderer;

    /** listener on the map adapter */
    private final PropertyChangeListener mapAdapterListener;

    /** the display */
    private final Display display;

    /** property change listeners */
    private PropertyChangeSupport propertyListeners;

    /** table of renderers */
    private static final WeakHashMap rendererTable = new WeakHashMap();

    /**
     * Constructs.
     *
     * @param displayAdapter    The adapted display.
     * @param dataReference     The reference to the datum.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DataAdapter(DisplayAdapter displayAdapter,
                       DataReference dataReference)
            throws VisADException, RemoteException {
        this(displayAdapter, dataReference, (ConstantMaps) null);
    }

    /**
     * Constructs.
     *
     * @param displayAdapter    The adapted display.
     * @param dataReference     The reference to the datum.
     * @param constantMaps      The constant maps for the datum.  May be
     *                          <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public DataAdapter(DisplayAdapter displayAdapter,
                       DataReference dataReference, ConstantMaps constantMaps)
            throws VisADException, RemoteException {
        this(displayAdapter, dataReference, constantMaps,
             (DataRenderer) null);
    }

    /**
     * Constructs.  This is the most general constructor.
     *
     * @param displayAdapter    The adapted display.
     * @param dataReference     The reference to the datum.
     * @param constantMaps      The constant maps for the datum.  May be
     *                          <code>null</code>.
     * @param renderer          The renderer for the data.  May be
     *                          <code>null</code>.  If non-<code>null</code>,
     *                          then must not have been used before.
     * @throws VisADException   The DataRenderer is already in-use for other
     *                          data.
     * @throws RemoteException  Java RMI failure.
     */
    public DataAdapter(DisplayAdapter displayAdapter,
                       DataReference dataReference,
                       ConstantMaps constantMaps, DataRenderer renderer)
            throws VisADException, RemoteException {

        display = displayAdapter.getDisplay();

        if (renderer != null) {
            if (rendererTable.get(renderer) != null) {
                throw new VisADException(
                    getClass().getName() + ".<init>: "
                    + "DataRenderer already in-use for other data");
            }

            rendererTable.put(renderer, this);
        }

        if (constantMaps == null) {
            mapAdapterListener = null;
        } else {
            mapAdapterListener = new PropertyChangeListener() {

                public void propertyChange(PropertyChangeEvent event) {

                    PropertyChangeEvent newEvent =
                        new PropertyChangeEvent(DataAdapter.this,
                            CONSTANT_MAP, event.getOldValue(),
                            event.getNewValue());

                    newEvent.setPropagationId(event.getPropagationId());
                    firePropertyChange(newEvent);
                }
            };

            constantMaps.addPropertyChangeListener(constantMaps.CONSTANT_MAP,
                    mapAdapterListener);
        }

        this.dataReference = dataReference;
        this.constantMaps  = constantMaps;
        dataRenderer       = renderer;

        displayAdapter.add(this);
    }

    /**
     * Links an {@link visad.Action} to the data component.
     *
     * @param action            The {@link visad.Action} to be linked.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void addAction(Action action)
            throws VisADException, RemoteException {
        action.addReference(dataReference);
    }

    /**
     * Unlinks an {@link visad.Action} from the data component.
     *
     * @param action            The {@link visad.Action} to be unlinked.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public synchronized void removeAction(Action action)
            throws VisADException, RemoteException {
        action.removeReference(dataReference);
    }

    /**
     * Adds a PropertyChangeListener to this instance.
     *
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
     *
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

    /**
     * Adds this instance to a VisAD display.  This method is package private
     * because it is expected that only the constructor's DisplayAdapter
     * argument will invoke this method.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    void addTo() throws VisADException, RemoteException {

        display.addReferences(dataRenderer, dataReference,
                              (constantMaps == null)
                              ? null
                              : constantMaps.getConstantMaps());
    }

    /**
     * Removes this instance from a VisAD display.  This method is package
     * private because it is expected that only the constructor's DisplayAdapter
     * argument will invoke this method.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    void removeFrom() throws VisADException, RemoteException {
        display.removeReference(dataReference);
    }

    /**
     * Returns the PropertyChangeListener-s of this instance.
     *
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
}
