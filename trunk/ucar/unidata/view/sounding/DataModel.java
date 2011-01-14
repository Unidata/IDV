/*
 * $Id: DataModel.java,v 1.10 2005/05/13 18:33:27 jeffmc Exp $
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



import java.beans.*;

import javax.swing.*;
import javax.swing.event.*;

import visad.*;


/**
 * Defines the API for a database of things.  The database comprises a
 * mutable list of things and selected things.
 *
 * @author Steven R. Emmerson
 * @version $Id: DataModel.java,v 1.10 2005/05/13 18:33:27 jeffmc Exp $
 */
interface DataModel {

    /**
     * The name of the index-of-the-selected-thing property.
     */
    static String SELECTED_INDEX = "selectedIndex";

    /**
     * Returns the index of the selected thing.
     * @return                  The index of the selected thing or -1 if nothing
     *                          is selected.
     */
    int getSelectedIndex();

    /**
     * Returns the number of things in the database.
     * @return number of things in the database
     */
    int getSize();

    /**
     * Indicates if the given thing is a member of the selected things.
     * @param index             The index of the given thing.
     * @return                  <code>true</code> if and only if the given
     *                          sounding is a member of the selected soundings.
     */
    boolean isSelectedIndex(int index);

    /**
     * Adds a PropertyChangeListener for a named property.
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be added.
     */
    void addPropertyChangeListener(String name,
                                   PropertyChangeListener listener);

    /**
     * Removes a PropertyChangeListener for a named property.
     * @param name              The name of the property.
     * @param listener          The PropertyChangeListener to be removed.
     */
    void removePropertyChangeListener(String name,
                                      PropertyChangeListener listener);

    /**
     * Adds a listener for changes to the selected things.
     * @param listener          The listener for changes to the selected
     *                          things.
     */
    void addListSelectionListener(ListSelectionListener listener);

    /**
     * Removes a listener for changes to the selected things.
     * @param listener          The listener for changes to the selected
     *                          things.
     */
    void removeListSelectionListener(ListSelectionListener listener);

    /**
     * Adds a listener for changes to the underlying list of things.
     * @param listener          The listener for changes to the underlying list
     *                          of things.
     */
    void addListDataListener(ListDataListener listener);

    /**
     * Removes a listener for changes to the underlying list of things.
     * @param listener          The listener for changes to the underlying list
     *                          of things.
     */
    void removeListDataListener(ListDataListener listener);
}







