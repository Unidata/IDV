/*
 * $Id: BeanNetwork.java,v 1.7 2005/05/13 18:33:24 jeffmc Exp $
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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Set;
import java.util.SortedSet;

import visad.TypeException;

import visad.VisADException;


/**
 * <p>Controls the timing of computations of a network of {@link
 * ClockedBean}s.</p>
 *
 * <p>This implementation is not thread-safe.</p>
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2005/05/13 18:33:24 $
 */
public final class BeanNetwork {

    /** set of beans */
    private final Set beans;

    /** sorted set of beans */
    private final SortedSet sortedBeans;

    /** dirty flag */
    private boolean isDirty;

    /**
     * Constructs from nothing.
     */
    public BeanNetwork() {

        beans       = new HashSet(1);
        sortedBeans = new TreeSet(new Comparator() {

            public int compare(Object o1, Object o2) {

                int         cmp;
                ClockedBean b1 = (ClockedBean) o1;
                ClockedBean b2 = (ClockedBean) o2;

                if (b1.dependsOn(b2)) {
                    cmp = 1;
                } else if (b2.dependsOn(b1)) {
                    cmp = -1;
                } else {
                    long del = (long) System.identityHashCode(b1)
                               - (long) System.identityHashCode(b2);

                    cmp = (del < 0)
                          ? -1
                          : (del == 0)
                            ? 0
                            : 1;
                }

                return cmp;
            }
        });
        isDirty = true;
    }

    /**
     * Handles the addition of a {@link java.beans.PropertyChangeListener} to
     * a component bean.
     *
     * @param bean                      The component bean.
     * @param listener                  The listener that was added to the bean.
     * @throws NullPointerException     if an argument is <code>null</code>.
     */
    void listenerAdded(ClockedBean bean, PropertyChangeListener listener) {

        if (bean == null) {
            throw new NullPointerException();
        }

        if (listener == null) {
            throw new NullPointerException();
        }

        if (beans.contains(bean)) {
            isDirty = true;
        }
    }

    /**
     * Handles the removal of a {@link java.beans.PropertyChangeListener}
     * from a component bean.
     *
     * @param bean                      The component bean.
     * @param listener                  The listener that was removed from the
     *                                  bean.
     * @throws NullPointerException     if an argument is <code>null</code>.
     */
    void listenerRemoved(ClockedBean bean, PropertyChangeListener listener) {

        if (bean == null) {
            throw new NullPointerException();
        }

        if (listener == null) {
            throw new NullPointerException();
        }

        if (beans.contains(bean)) {
            isDirty = true;
        }
    }

    /**
     * Adds a clocked JavaBean.
     *
     * @param bean              The clocked JavaBean.
     */
    void add(ClockedBean bean) {

        if (beans.add(bean)) {
            isDirty = true;
        }
    }

    /**
     * Removes a clocked JavaBean.
     *
     * @param bean              The clocked JavaBean.
     */
    void remove(ClockedBean bean) {

        if (beans.remove(bean)) {
            isDirty = true;
        }
    }

    /**
     * Configures the clocked JavaBean network.  The network is analyzed for
     * dependencies and the sequence of {@link ClockedBean#clock()} invocations
     * is determined.
     *
     * @throws ClockedBeanCycleException if the graph of dependencies of the
     *                                   clocked JavaBean-s contains a cycle.
     */
    private void analyze() {

        sortedBeans.clear();
        sortedBeans.addAll(beans);

        isDirty = false;
    }

    /**
     * Clocks the network.
     *
     * @throws TypeException        if a value in the computation has the wrong
     *                              VisAD type.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    public void clock()
            throws TypeException, VisADException, RemoteException {

        if (isDirty) {
            analyze();
        }

        for (Iterator iter = sortedBeans.iterator(); iter.hasNext(); ) {
            ((ClockedBean) iter.next()).clock();
        }
    }
}







