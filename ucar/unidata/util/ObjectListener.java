/*
 * $Id: ObjectListener.java,v 1.14 2007/05/29 13:36:44 jeffmc Exp $
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



import javax.swing.event.*;




/**
 * Implements ActionListener and MouseListener (and others) interfaces and holds an arbitrary object.
 * This can be used to pass state into anonymous listener objects without having the
 * defined as final in the creation scope of the listener.
 * This also servers like the MouseAdapter - i.e., it implements all of the methods
 * required in the various interfaces. A derived class does not need to implement all of
 *  the required methods -  just the ones they need.
 */

public class ObjectListener implements MouseListener, ActionListener,
                                       KeyListener, WindowListener,
                                       ItemListener, ChangeListener {

    /**
     *  This is the object that this  class is instantiated with.
     */
    protected Object theObject;

    /**
     *  Create the ObjectListener with the given object.
     *
     *  @param stateObject The state object
     */
    public ObjectListener(Object stateObject) {
        this.theObject = stateObject;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public Object getObject() {
        return theObject;
    }

    /**
     * _more_
     *
     * @param object _more_
     */
    public void setObject(Object object) {
        this.theObject = object;
    }

    /**
     *  A  helper constructor  that convert the array of objects into an ObjectArray object.
     *
     * @param objs
     */
    public ObjectListener(Object[] objs) {
        this(new ObjectArray(objs));
    }


    /**
     * _more_
     *
     * @param e
     */
    public void itemStateChanged(ItemEvent e) {}

    /**
     * _more_
     *
     * @param e
     */
    public void mouseClicked(MouseEvent e) {}

    /**
     * _more_
     *
     * @param e
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * _more_
     *
     * @param e
     */
    public void mouseExited(MouseEvent e) {}

    /**
     * _more_
     *
     * @param e
     */
    public void mousePressed(MouseEvent e) {}

    /**
     * _more_
     *
     * @param e
     */
    public void mouseReleased(MouseEvent e) {}

    /**
     * _more_
     *
     * @param ev
     */
    public void actionPerformed(ActionEvent ev) {}


    /**
     * _more_
     *
     * @param e the event
     */

    public void stateChanged(ChangeEvent e) {}

    /**
     * _more_
     *
     * @param e
     */
    public void keyPressed(KeyEvent e) {}

    /**
     * _more_
     *
     * @param e
     */
    public void keyReleased(KeyEvent e) {}

    /**
     * _more_
     *
     * @param e
     */
    public void keyTyped(KeyEvent e) {}






    /**
     * _more_
     *
     * @param event
     */
    public void windowActivated(WindowEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void windowClosed(WindowEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void windowClosing(WindowEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void windowDeactivated(WindowEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void windowDeiconified(WindowEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void windowIconified(WindowEvent event) {}

    /**
     * _more_
     *
     * @param event
     */
    public void windowOpened(WindowEvent event) {}


    /**
     *  This is a special method so we can pass a generic listener instance into
     *  another Class's method that can create some ui thing and route events to this guy.
     *
     * @param ev
     * @param data
     */
    public void actionPerformed(ActionEvent ev, Object data) {}


}

