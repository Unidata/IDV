/*
 * $Id: ChooserList.java,v 1.10 2007/07/06 20:45:29 jeffmc Exp $
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



import ucar.unidata.util.GuiUtils;



import java.awt.*;
import java.awt.event.*;

import javax.swing.*;


/**
 * A Jlist in a scroller
 */
public class ChooserList extends JList {

    /** scroller */
    private JScrollPane myScroller;

    /**
     * Create a chooser list with the default mode
     */
    public ChooserList() {
        this(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }

    /**
     * Create a chooser with a specific selection mode
     * @param mode   selection mode
     *
     */
    public ChooserList(int mode) {
        setSelectionMode(mode);
    }


    /**
     * Get the scroller for this list
     * @return  the scroller
     */
    public JScrollPane getScroller() {
        if (myScroller == null) {
            myScroller =
                new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            myScroller.setPreferredSize(new Dimension(120, 100));
        }
        return myScroller;
    }

    /**
     *  Is there anything selected. If so if there is one thing selected and
     *  it is a String then return false. (We do this because the chooser's
     *  often put a String message into the list).
     * @return _more_
     */
    public boolean haveDataSelected() {
        if (getModel().getSize() == 0) {
            return false;
        }
        Object[] selected = getSelectedValues();
        if (selected.length == 0) {
            return false;
        }
        if ((selected.length == 1) && (selected[0] instanceof String)) {
            return false;
        }
        return true;
    }

}

