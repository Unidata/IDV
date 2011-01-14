/*
 * $Id: PersistentObject.java,v 1.7 2007/07/06 20:45:32 jeffmc Exp $
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

package ucar.unidata.ui;  // change to another package



/**
 * Abstraction for persistent objects: must be Cloneable and Serializable.
 *
 * @author John Caron
 * @version $Id: PersistentObject.java,v 1.7 2007/07/06 20:45:32 jeffmc Exp $
 */

public interface PersistentObject extends java.io.Serializable,
                                          java.lang.Cloneable {


    /**
     * ManagerBean
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    public interface ManagerBean {

        /**
         * Abstraction for managers of persistent objects. show() pops up a dialog that allows
         * the user to change/store/select. A PropertyChangeEvent is thrown when the user selects
         * a new one.
         *
         * TBD: add(Object) to storage?
         * TBD: generic types ?
         * @return _more_
         */


        /** get the name of this manager to put in a menu */
        public String getManagerName();

        /**
         * Get the Class type of the objects managed; must implement Serializable, Cloneable
         * @return _more_
         */
        public Class getPersistentObjectClass();

        /**
         * Add PropertyChangeEvent listener.
         *  PropertyChangeEvent is fired when the currently selected object is changed.
         *  It is not fired upon startup/ initialization (use getSelection() if needed at startup).
         *  The event passes the new selected object in e.getNewValue(), and optionally the old value in
         *  e.getOldValue(). e.getPropertyName() by convention returns the name of the class of the object.
         *  The listener must NOT CHANGE this object (but may clone it).
         *
         * @param l
         */
        public void addPropertyChangeListener(
                java.beans.PropertyChangeListener l);

        /**
         * Remove PropertyChangeEvent listener
         *
         * @param l
         */
        public void removePropertyChangeListener(
                java.beans.PropertyChangeListener l);

        /** Popup the Manager Dialog */
        public void show();

        /** Call this when you want to store the persistent data */
        public void storePersistentData();

        /**
         * Get the currently selected object, of type getPersistentObjectClass()
         * @return _more_
         */
        public PersistentObject getSelection();

        /**
         * Set the currently selected thing: must be of type getPersistentObjectClass().
         *  this does NOT fire a PropertyChangeEvent
         *
         * @param select
         */
        public void setSelection(PersistentObject select);

    }  // end inner interface managerBean

}

/*
 *  Change History:
 *  $Log: PersistentObject.java,v $
 *  Revision 1.7  2007/07/06 20:45:32  jeffmc
 *  A big J&J
 *
 *  Revision 1.6  2005/05/13 18:31:50  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.5  2004/09/07 18:36:25  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.4  2004/02/27 21:19:20  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.3  2004/01/29 17:37:12  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.2  2000/08/18 04:15:56  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.1  1999/12/16 22:58:16  caron
 *  gridded data viewer checkin
 *
 */






