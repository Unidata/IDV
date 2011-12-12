/*
 * $Id: ClockedBean.java,v 1.8 2005/05/13 18:33:26 jeffmc Exp $
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



import java.awt.event.ActionEvent;

import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import visad.TypeException;

import visad.VisADException;


/**
 * A skeletal Java Bean that computes the value of its output properties only
 * when "clocked" by an external source.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.8 $ $Date: 2005/05/13 18:33:26 $
 */
public abstract class ClockedBean {

    /** bean network */
    private final BeanNetwork network;

    /** change support */
    private final PropertyChangeSupport changeSupport;

    /*
     * TODO: replace "listeners" with
     * PropertyChangeSupport.getPropertyChangeListeners() when J2SE 1.4 is
     * supported.
     */

    /** set of listeners */
    private Set listeners;

    /** visited beans */
    private final Set visitedBeans;

    /**
     * Constructs from the network in which this bean will be a component.
     *
     * @param network               The associated, clocked, JavaBean network.
     * @throws NullPointerException if the argument is <code>null</code>.
     */
    ClockedBean(BeanNetwork network) {

        if (network == null) {
            throw new NullPointerException();
        }

        this.network  = network;
        changeSupport = new PropertyChangeSupport(this);
        listeners     = new HashSet(1);
        visitedBeans  = new HashSet(1);

        network.add(this);
    }

    /**
     * Adds a listener for changes in the output properties.  The listener is
     * registered for all properties.
     *
     * @param listener          The listener to be added.
     */
    public final synchronized void addPropertyChangeListener(
            PropertyChangeListener listener) {

        changeSupport.addPropertyChangeListener(listener);
        listeners.add(listener);
        network.listenerAdded(this, listener);
    }

    /**
     * Adds a listener for changes in a named output property.
     *
     * @param name              The name of the property.
     * @param listener          The listener to be added.
     */
    public final synchronized void addPropertyChangeListener(String name,
            PropertyChangeListener listener) {

        changeSupport.addPropertyChangeListener(name, listener);
        listeners.add(listener);
        network.listenerAdded(this, listener);
    }

    /**
     * Removes a listener for changes in the output properties.  The listener is
     * unregistered for all properties.
     *
     * @param listener          The listener to be removed.
     */
    public final synchronized void removePropertyChangeListener(
            PropertyChangeListener listener) {

        changeSupport.removePropertyChangeListener(listener);
        listeners.remove(listener);
        network.listenerRemoved(this, listener);
    }

    /**
     * Removes a listener for changes in a named output property.
     *
     * @param name              The name of the property.
     * @param listener          The listener to be removed.
     */
    public final synchronized void removePropertyChangeListener(String name,
            PropertyChangeListener listener) {

        changeSupport.removePropertyChangeListener(name, listener);
        listeners.remove(listener);
        network.listenerRemoved(this, listener);
    }

    /**
     * Computes the output properties.  When done, an unsynchronized thread
     * should invoke {@link #firePropertyChange} with old and new values.
     *
     * @throws TypeException        if a value in the computation has the wrong
     *                              VisAD type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    abstract void clock()
     throws TypeException, VisADException, RemoteException;

    /**
     * Fires a {@link java.beans.PropertyChangeEvent} for a named output
     * property.  The event is not fired if the old and new values are
     * equal.  This method should be invoked by an unsynchronized thread.
     *
     * @param name                  The name of the property.
     * @param oldValue              The previous value of the property.
     * @param newValue              The current value of the property.
     */
    synchronized final void firePropertyChange(String name, Object oldValue,
                                               Object newValue) {
        changeSupport.firePropertyChange(name, oldValue, newValue);
    }

    /**
     * Indicates if this instance depends on another instance.
     * Instance A depends on instance B if and only if A is a {@link
     * java.beans.PropertyChangeListener} of B or of an instance that
     * depends on B.
     *
     * @param that                  The instance to be examined.
     * @return                      True if and only if this instance depends on
     *                              the given instance.
     * @throws ClockedBeanCycleException if a cycle is detected in the directed
     *                                   graph of clocked bean listeners.
     */
    final synchronized boolean dependsOn(ClockedBean that) {

        boolean dependsOn;

        try {
            dependsOn = depends(that);
        } finally {
            visitedBeans.clear();
        }

        return dependsOn;
    }

    /**
     * Check for dependency between this and that
     * @param that  bean to check
     * @return true if there is dependency
     * @throws ClockedBeanCycleException if a cycle is detected in the directed
     *                                   graph of clocked bean listeners.
     */
    private boolean depends(ClockedBean that) {

        if (visitedBeans.contains(that)) {
            throw new ClockedBeanCycleException(that);
        }

        visitedBeans.add(that);

        boolean dependsOn = false;

        synchronized (that) {
            if (that.listeners.contains(this)) {
                dependsOn = true;
            } else {
                for (Iterator iter = that.listeners.iterator();
                        iter.hasNext(); ) {
                    Object obj = iter.next();

                    if (obj instanceof ClockedBean) {
                        if (depends((ClockedBean) obj)) {
                            dependsOn = true;

                            break;
                        }
                    }
                }
            }
        }

        return dependsOn;
    }
}







