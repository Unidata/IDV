/*
 * $Id: PopupTriggerListener.java,v 1.7 2005/05/13 18:32:16 jeffmc Exp $
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

package ucar.unidata.ui.event;



import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


/**
 * standard way to attach popup menu
 * subclass and provide your own showPopup()
 * attach as MouseListener to the Component
 */
public abstract class PopupTriggerListener extends MouseAdapter {

    /**
     * _more_
     *
     * @param e
     */
    public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    /**
     * _more_
     *
     * @param e
     */
    public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    /**
     * _more_
     *
     * @param e
     */
    public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
            showPopup(e);
        }
    }

    /**
     * _more_
     *
     * @param e
     */
    public abstract void showPopup(MouseEvent e);
}





