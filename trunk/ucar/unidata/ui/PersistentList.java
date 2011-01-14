/*
 * $Id: PersistentList.java,v 1.10 2007/07/06 20:45:32 jeffmc Exp $
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

package ucar.unidata.ui;


import ucar.unidata.util.PersistentStore;



import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.dnd.*;
import java.awt.event.*;

import java.io.*;

import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;


/**
 * Utility for managing a list of persistent objects.
 *  Uses a ListModel to manage a list of Objects.
 *  The id for an object is the String from "toString" method.
 *  Storing and fetching objects enforces id uniqueness.  <br>
 *  <br>
 *  An action event is sent when an object is selected. The event source
 *  is the selected object, or the listener can call getSelected(). <br>
 *  <br>
 *  (TBD) The first token of toString() must be a unique id (like a name).
 *  (TBD) Drag 'n Drop.
 *
 * @see ucar.unidata.util.PersistentStore
 * @author John Caron
 * @version $Id: PersistentList.java,v 1.10 2007/07/06 20:45:32 jeffmc Exp $
 */

public class PersistentList extends JPanel {

    /** _more_ */
    private DefaultListModel model = null;

    /** _more_ */
    private JList list;

    /** _more_ */
    private ucar.unidata.util.ListenerManager lm;

    /** _more_ */
    private PersistentStore store;

    /** _more_ */
    private String objectName;

    /** _more_ */
    private boolean debug = false;

    /** _more_ */
    private DragSource dragSource;

    /** _more_ */
    private DragGestureRecognizer recognizer;

    /** _more_ */
    private DragSourceListener dsl = new myDragSourceListener();

    /**
     * Constructor
     * @param objectName  name of list object in the PersistentStore
     * @param store       where objects are stored
     * @param header      field names placed at the top of the list
     */
    public PersistentList(String objectName, PersistentStore store,
                          String header) {
        super();

        this.objectName = objectName;
        this.store      = store;

        // manage Action Listeners
        lm = new ucar.unidata.util.ListenerManager(
            "java.awt.event.ActionListener", "java.awt.event.ActionEvent",
            "actionPerformed");

        // fetch persistent List of choices
        if (store != null) {
            model = (DefaultListModel) store.get(objectName);
        }
        if (model == null) {
            model = new DefaultListModel();
        } else if (debug) {
            System.out.println("SerialListManager read " + objectName);
            System.out.println("    " + model);
        }

        list = new JList(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        if (model.getSize() > 0) {
            list.setSelectedIndex(0);
        }

        setLayout(new BorderLayout());
        JScrollPane listScroller = new JScrollPane(list);

        add(new JLabel(header), BorderLayout.NORTH);
        add(listScroller, BorderLayout.CENTER);
        //setPreferredSize( new Dimension(500, 300));

        // set up the drag 'n drop
        dragSource = new DragSource();
        recognizer = dragSource.createDefaultDragGestureRecognizer(list,
                DnDConstants.ACTION_COPY, new myDragGestureListener());

        // catch selection events from the List
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if ( !e.getValueIsAdjusting()) {
                    if (null != list.getSelectedValue()) {
                        if (debug) {
                            System.out.println(" valueChanged event " + e);
                        }
                        lm.sendEvent(new ActionEvent(getSelected(),
                                ActionEvent.ACTION_PERFORMED, "use"));
                    }
                }
            }
        });

    }

    /**
     * add a "New Selection" listener
     *
     * @param l
     */
    public void addActionListener(ActionListener l) {
        lm.addListener(l);
    }

    /**
     * remove a "New Selection" listener
     *
     * @param l
     */
    public void removeActionListener(ActionListener l) {
        lm.removeListener(l);
    }

    /**
     * _more_
     *
     * @param o
     */
    public void appendElement(Object o) {
        model.addElement(o);
        list.setSelectedValue(o, true);
    }

    /**
     * check if this id already exists
     *
     * @param id
     * @return true if an Object with this id already exists in the list
     */
    public boolean contains(String id) {
        return (0 <= search(id));
    }

    /**
     * find the object in the list with the same id as editedObject
     * if it exists, replace it with editedObject.
     * if it doesnt exist, add it to the end of the list.
     * @param editedObject       the edited object
     */
    public void replace(Object editedObject) {
        int idx;
        if (0 <= (idx = search(editedObject.toString()))) {
            model.set(idx, editedObject);
            list.setSelectedIndex(idx);
        } else {
            model.addElement(editedObject);
            list.setSelectedIndex(model.getSize() - 1);
        }
    }

    /**
     * _more_
     *
     * @param id
     * @return _more_
     */
    private int search(String id) {
        for (int i = 0; i < model.getSize(); i++) {
            Object test = model.get(i);
            if (id.equals(test.toString())) {
                return i;
            }
        }
        return -1;
    }

    /** delete the currently selected object */
    public void deleteSelected() {
        Object selected = list.getSelectedValue();
        if (null == selected) {
            return;
        }

        int idx = list.getSelectedIndex();
        model.removeElement(selected);
        int len = model.getSize();
        if (len == 0) {
            list.clearSelection();
        } else if (idx > len - 1) {
            list.setSelectedIndex(idx - 1);
        }
        list.repaint();
    }

    /**
     * return the currently selected object
     * @return _more_
     */
    public int getNumElements() {
        return model.getSize();
    }

    /**
     * return the currently selected object
     * @return _more_
     */
    public Object getSelected() {
        return list.getSelectedValue();
    }

    /** save the list to the PersistentStore */
    public void storePersistentData() {
        store.put(objectName, model);
    }

    // DnD

    /**
     * Class myDragGestureListener
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private class myDragGestureListener implements DragGestureListener {

        /**
         * _more_
         *
         * @param dge
         */
        public void dragGestureRecognized(DragGestureEvent dge) {
            System.out.println("myDragGestureListener = " + dge);
            Object selected = list.getSelectedValue();
            dge.startDrag(DragSource.DefaultCopyDrop,
                          new StringSelection("test"), dsl);
        }
    }

    /**
     * Class myDragSourceListener
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private class myDragSourceListener implements DragSourceListener {

        /**
         * _more_
         *
         * @param e
         */
        public void dragDropEnd(DragSourceDropEvent e) {}

        /**
         * _more_
         *
         * @param e
         */
        public void dragEnter(DragSourceDragEvent e) {}

        /**
         * _more_
         *
         * @param e
         */
        public void dragExit(DragSourceEvent e) {}

        /**
         * _more_
         *
         * @param e
         */
        public void dragOver(DragSourceDragEvent e) {}

        /**
         * _more_
         *
         * @param e
         */
        public void dropActionChanged(DragSourceDragEvent e) {}
    }

}

/*
 *  Change History:
 *  $Log: PersistentList.java,v $
 *  Revision 1.10  2007/07/06 20:45:32  jeffmc
 *  A big J&J
 *
 *  Revision 1.9  2005/05/13 18:31:50  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.8  2004/09/07 18:36:25  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.7  2004/02/27 21:19:20  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.6  2004/01/29 17:37:12  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.5  2000/08/18 04:15:56  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.4  1999/12/16 22:58:15  caron
 *  gridded data viewer checkin
 *
 *  Revision 1.3  1999/06/03 01:44:13  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:07  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:48  caron
 *  startAgain
 *
 * # Revision 1.1  1999/03/26  19:58:32  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.6  1999/03/16  17:00:56  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.5  1999/02/24  21:10:40  caron
 * # corrections for Solaris
 * #
 * # Revision 1.4  1999/02/15  23:06:46  caron
 * # upgrade to java2D, new ProjectionManager
 * #
 * # Revision 1.3  1998/12/14  17:12:02  russ
 * # Add comment for accumulating change histories.
 * #
 */







