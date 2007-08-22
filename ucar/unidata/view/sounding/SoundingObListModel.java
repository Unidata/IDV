/*
 * $Id: SoundingObListModel.java,v 1.10 2005/05/13 18:33:38 jeffmc Exp $
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



import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import ucar.unidata.data.sounding.SoundingOb;


/**
 * Provides support for adapting a list of sounding observations to the
 * ListModel interface.
 *
 * @author Steven R. Emmerson
 * @version $Id: SoundingObListModel.java,v 1.10 2005/05/13 18:33:38 jeffmc Exp $
 */
public class SoundingObListModel extends AbstractListModel {

    /** list of sounding observations */
    private List soundingObs;

    /**
     * Constructs from nothing.
     */
    public SoundingObListModel() {
        soundingObs = new ArrayList();  // need random access by index
    }

    /**
     * Adds a sounding observation to the list.  Notifies ListDataListener-s.
     * @param sounding          The sounding observation to be added to the
     *                          list.
     */
    public synchronized void addSounding(SoundingOb sounding) {

        int index = soundingObs.size();

        soundingObs.add(sounding);
        fireIntervalAdded(this, index, index);
    }

    /**
     * Removes a sounding observation from the list.  Notifies
     * ListDataListener-s.
     * @param sounding          The sounding observation to be removed from the
     *                          list.
     */
    public synchronized void removeSounding(SoundingOb sounding) {

        int index = soundingObs.indexOf(sounding);

        if (index >= 0) {
            soundingObs.remove(index);
            fireIntervalRemoved(this, index, index);
        }
    }

    /**
     * Removes a sounding observation from the list.  Notifies
     * ListDataListener-s.
     * @param index             The index of the sounding observation to be
     *                          removed from the list.
     */
    public synchronized void removeSounding(int index) {

        if (index >= 0) {
            soundingObs.remove(index);
            fireIntervalRemoved(this, index, index);
        }
    }

    /**
     * Clears all sounding observations from the list.  Notifies
     * ListDataListener-s.
     */
    public synchronized void clearSoundings() {

        int index = soundingObs.size() - 1;

        if (index >= 0) {
            soundingObs.clear();
            fireIntervalRemoved(this, 0, index);
        }
    }

    /**
     * Returns the sounding at the given index.
     * @param index             The index of the sounding.
     * @return                  The sounding at the given index.  Will be
     *                          <code>null</code> if there's no such sounding.
     */
    public synchronized Object getElementAt(int index) {
        return soundingObs.get(index);
    }

    /**
     * Returns the number of soundings.
     * @return                  The number of soundings.
     */
    public int getSize() {
        return soundingObs.size();
    }
}







