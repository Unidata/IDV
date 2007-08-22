/*
 * $Id: AsynchronousPropertyChangeWrapper.java,v 1.7 2006/05/05 19:19:32 jeffmc Exp $
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


package ucar.unidata.util;



import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * Provides support for the asynchronous handling of property-change events.
 * Property-change events are placed in a one-element event-queue for later,
 * asynchronous processing by a wrapped {@link PropertyChangeListener}.  If
 * more than one event is received while the asynchronous, wrapped {@link
 * PropertyChangeListener} is processing an event, then all events but the last
 * are discarded.  This implementation is good for {@link PropertyChangeEvent}s
 * that are self-contained and completely independent of one another.
 *
 * @author Steven R. Emmerson
 * @version $Revision: 1.7 $ $Date: 2006/05/05 19:19:32 $
 */
public final class AsynchronousPropertyChangeWrapper implements Runnable,
        PropertyChangeListener {

    /** _more_ */
    private boolean active = true;

    /** _more_ */
    private PropertyChangeEvent event = null;

    /** _more_ */
    private boolean ready = false;

    /** _more_ */
    private PropertyChangeListener listener;

    /**
     * Constructs from nothing.  The {@link PropertyChangeEvent} thread will be
     * started.
     *
     * @param listener              The listener to be wrapped.
     * @throws NullPointerException if the listener is <code>null</code>.
     */
    public AsynchronousPropertyChangeWrapper(
            PropertyChangeListener listener) {

        this.listener = listener;

        new Thread(this).start();
    }

    /**
     * _more_
     */
    public synchronized void doRemove() {
        active = false;
        notify();
    }


    /**
     * Executes the asynchronous, {@link PropertyChangeEvent}-handling thread.
     */
    public final void run() {

        for (;;) {
            PropertyChangeEvent localEvent;

            synchronized (this) {
                if ( !ready) {
                    try {
                        wait();
                    } catch (InterruptedException e) {

                        /*
                         * It's not possible for the wait() to be interrupted
                         * because the asynchonous, event-handling thread object
                         * is confined to this class and this class never
                         * invokes that thread object's interrupt() method.
                         */
                        continue;  // just in case :-)
                    }
                }

                if ( !active) {
                    return;
                }
                localEvent = event;
                ready      = false;
            }

            /*
             * Concurrent reception of subsequent events may now occur.
             */
            listener.propertyChange(localEvent);
        }
    }

    /**
     * Receives a {@link PropertyChangeEvent} for asynchronous handling.  The
     * event is placed on the event-queue, replacing any previous event.
     *
     * @param event             The event to receive.  May be <code>null</code>.
     */
    public synchronized final void propertyChange(PropertyChangeEvent event) {

        this.event = event;
        ready      = true;

        notify();
    }
}

