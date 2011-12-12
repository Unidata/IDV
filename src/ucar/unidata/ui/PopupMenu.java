/*
 * $Id: PopupMenu.java,v 1.11 2007/07/06 20:45:32 jeffmc Exp $
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



import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;


/** convenience class for constructing popup menus */
public class PopupMenu extends JPopupMenu {

    /** _more_ */
    private JComponent parent;

    /**
     * Constructor.
     *
     * @param pop         MouseListener is added to this JComponent.
     * @param menuTitle   title of the popup menu.
     */
    public PopupMenu(JComponent pop, String menuTitle) {
        super(menuTitle);
        this.parent = pop;

        parent.addMouseListener(new PopupTriggerListener() {
            public void showPopup(java.awt.event.MouseEvent e) {
                show(parent, e.getX(), e.getY());
            }
        });
    }

    /**
     * Add an action to the popup menu.
     * Note that the menuName is made the NAME value of the action.
     * @param menuName name of the action on the menu.
     * @param act the Action.
     */
    public void addAction(String menuName, AbstractAction act) {
        act.putValue(Action.NAME, menuName);
        super.add(act);
    }

    /**
     * Add an action to the popup menu, using a JCheckBoxMenuItem.
     * Fetch the toggle state using:
     * <pre>Boolean state = (Boolean) act.getValue(BAMutil.STATE); </pre>
     * @param menuName name of the action on the menu.
     * @param act the Action.
     * @param state : initial state of the checkbox
     */
    public void addActionCheckBox(String menuName, AbstractAction act,
                                  boolean state) {
        JMenuItem mi = new JCheckBoxMenuItem(menuName, state);
        mi.addActionListener(new BAMutil.ActionToggle(act, mi));
        act.putValue(BAMutil.STATE, new Boolean(state));
        add(mi);
    }

    /**
     * Class PopupTriggerListener
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private abstract class PopupTriggerListener extends MouseAdapter {

        /**
         * _more_
         *
         * @param e
         */
        public void mousePressed(MouseEvent e) {
            //System.out.println( "PopupTriggerListener "+e);
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
}

