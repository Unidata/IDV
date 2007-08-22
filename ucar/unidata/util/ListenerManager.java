/*
 * $Id: ListenerManager.java,v 1.16 2006/05/05 19:19:35 jeffmc Exp $
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



import java.awt.event.*;

import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.ListIterator;


/**
 * Helper class for event listeners.
 * @author John Caron
 * @version $Id: ListenerManager.java,v 1.16 2006/05/05 19:19:35 jeffmc Exp $
 */
public class ListenerManager {

    /** _more_ */
    private ArrayList listeners = new ArrayList();

    /** _more_ */
    private java.lang.reflect.Method method = null;

    /** _more_ */
    private boolean hasListeners = false;

    /**
     * Constructor.
     * @param listener_class    the name of the EventListener class, eg "ucar.unidata.ui.UIChangeListener"
     * @param event_class       the name of the Event class, eg "ucar.unidata.ui.UIChangeEvent"
     * @param method_name       the name of the EventListener method, eg "processChange". <pre>
     *    This method must have the signature     public void method_name( event_class e) </pre>
     */
    public ListenerManager(String listener_class, String event_class,
                           String method_name) {

        try {
            Class   lc     = Class.forName(listener_class);
            Class   ec     = Class.forName(event_class);
            Class[] params = new Class[1];
            params[0] = ec;
            Method lm = lc.getMethod(method_name, params);
            this.method = lm;

        } catch (Exception ee) {
            System.err.println("ListenerManager failed on " + listener_class
                               + "." + method_name + "( " + event_class
                               + " )");
            System.err.println("   Exception = " + ee);
        }

    }

    /**
     * Add a listener.
     * @param l listener: must be of type "listener_class"
     */
    public synchronized void addListener(Object l) {
        if ( !listeners.contains(l)) {
            listeners.add(l);
            hasListeners = true;
        } else {
            System.out.println("ListenerManager already has Listener " + l);
        }
    }

    /**
     * Remove a listener.
     *
     * @param l
     */
    public synchronized void removeListener(Object l) {
        if (listeners.contains(l)) {
            listeners.remove(l);
            hasListeners = (listeners.size() > 0);
        } else {
            System.out.println("ListenerManager couldnt find Listener " + l);
        }
    }

    /**
     * _more_
     * @return _more_
     */
    public boolean hasListeners() {
        return hasListeners;
    }

    /**
     * Send an event to all registered listeners. If an exception is thrown, remove
     * the Listener from the list
     * @param event the event to be sent: public void method_name( event_class event)
     */
    public void sendEvent(java.util.EventObject event) {
        if ( !hasListeners) {
            return;
        }

        Object[] args = new Object[1];
        args[0] = event;

        // send event to all listeners
        ListIterator iter = listeners.listIterator();
        while (iter.hasNext()) {
            Object client = iter.next();
            try {
                method.invoke(client, args);
            } catch (IllegalAccessException e) {
                iter.remove();
                System.err.println(
                    "ListenerManager IllegalAccessException = " + e);
            } catch (IllegalArgumentException e) {
                iter.remove();
                System.err.println(
                    "ListenerManager IllegalArgumentException = " + e);
            } catch (InvocationTargetException e) {
                iter.remove();
                System.err.println(
                    "ListenerManager InvocationTargetException on " + method);
                System.err.println("   threw exception "
                                   + e.getTargetException());
                e.printStackTrace();
            }  /*catch (Exception e) {
               System.err.println("ListenerManager sendEvent failed "+ e);
               iter.remove();
             } */
        }
    }

    /**
     * Send an event to all registered listeners, except the named one.
     * @param event the event to be sent: public void method_name( event_class event)
     */
    public void sendEventExcludeSource(java.util.EventObject event) {
        if ( !hasListeners) {
            return;
        }

        Object   source = event.getSource();
        Object[] args   = new Object[1];
        args[0] = event;

        // send event to all listeners except the source
        ListIterator iter = listeners.listIterator();
        while (iter.hasNext()) {
            Object client = iter.next();
            if (client == source) {
                continue;
            }

            try {
                method.invoke(client, args);
            } catch (IllegalAccessException e) {
                iter.remove();
                System.err.println(
                    "ListenerManager IllegalAccessException = " + e);
            } catch (IllegalArgumentException e) {
                iter.remove();
                System.err.println(
                    "ListenerManager IllegalArgumentException = " + e);
            } catch (InvocationTargetException e) {
                iter.remove();
                System.err.println(
                    "ListenerManager InvocationTargetException on " + method);
                System.err.println("   threw exception "
                                   + e.getTargetException());
                e.printStackTrace();
            }
        }
    }

}

/*
 *  Change History:
 *  $Log: ListenerManager.java,v $
 *  Revision 1.16  2006/05/05 19:19:35  jeffmc
 *  Refactor some of the tabbedpane border methods.
 *  Also, since I ran jindent on everything to test may as well caheck it all in
 *
 *  Revision 1.15  2005/05/13 18:32:41  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.14  2004/08/23 17:27:26  dmurray
 *  silence some javadoc warnings
 *
 *  Revision 1.13  2004/08/19 21:34:45  jeffmc
 *  Scratch log4j
 *
 *  Revision 1.12  2004/02/27 21:18:51  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.11  2004/01/29 17:37:39  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.10  2000/08/18 04:16:11  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.9  2000/05/16 23:11:11  caron
 *  add sendEventExcludeSource()
 *
 *  Revision 1.8  2000/04/26 21:11:20  caron
 *  dont catch all Exceptions: Don will blame it on me :^}
 *
 *  Revision 1.7  2000/02/11 01:26:49  caron
 *  cleanup Debug
 *
 *  Revision 1.6  2000/02/07 18:00:08  caron
 *  add hasListenrs optimization
 *
 *  Revision 1.5  1999/12/16 22:58:29  caron
 *  gridded data viewer checkin
 *
 *  Revision 1.4  1999/06/08 23:24:03  caron
 *  more client/server changes
 *
 *  Revision 1.3  1999/06/03 01:44:20  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:21  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:51  caron
 *  startAgain
 *
 * # Revision 1.3  1999/03/26  19:58:44  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.2  1998/12/14  17:12:06  russ
 * # Add comment for accumulating change histories.
 * #
 */

