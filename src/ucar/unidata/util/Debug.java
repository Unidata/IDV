/*
 * $Id: Debug.java,v 1.12 2006/05/05 19:19:34 jeffmc Exp $
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



import java.awt.*;

import java.beans.*;

import java.util.*;

import javax.swing.JCheckBoxMenuItem;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.*;


/**
 * This is a simple way to add runtime-settable debugging.
 *
 * Debug flags are just strings; the Debug class dynamically
 * adds them to the debug menu.
 *
 *
 *
 * Example:  putting a runtime flag in your code. A convention I use is to
 *  put the name of the debug flag in the message, so I know "where it comes from".
 *   <pre>
 *   if (Debug.isSet("Map.draw")) {
 *     System.out.println("Map.draw: makeShapes with "+displayProject);
 *   }
 *   </pre>
 *
 * Example:  adding the debug menu to a "System" menu
 *   <pre>
 *   private JMenu debugMenu;
 *
 *   JMenu sysMenu = new JMenu("System");
 *   debugMenu = (JMenu) sysMenu.add(new JMenu("Debug"));
 *   debugMenu.addMenuListener( new MenuListener() {
 *     public void menuSelected(MenuEvent e) { ucar.unidata.util.Debug.constructMenu(debugMenu); }
 *     public void menuDeselected(MenuEvent e) {}
 *     public void menuCanceled(MenuEvent e) {}
 *   });
 *   </pre>
 *
 *
 */

public class Debug {

    /** _more_ */
    static private final String STORE_NAME = "DebugFlags";

    /** _more_ */
    static private TreeMap map = new TreeMap();

    /** _more_ */
    static private boolean
        debug   = false,
        changed = true;

    /**
     * call this when you want to fetch the persistent data
     *
     * @param store
     */
    static public void fetchPersistentData(
            ucar.unidata.util.PersistentStore store) {
        Object o = store.get(STORE_NAME);
        if (o != null) {
            map     = (TreeMap) o;
            changed = true;
        }
    }

    /**
     * call this when you want to store the persistent data
     *
     * @param store
     */
    static public void storePersistentData(
            ucar.unidata.util.PersistentStore store) {
        store.put(STORE_NAME, map);
    }

    /**
     * _more_
     *
     * @param flagName
     * @return _more_
     */
    static public boolean isSet(String flagName) {
        Object val;
        if (null == (val = map.get(flagName))) {
            if (debug) {
                System.out.println("Debug.isSet new " + flagName);
            }
            map.put(flagName, new Boolean(false));
            changed = true;
            return false;
        }

        return ((Boolean) val).booleanValue();
    }

    /**
     * _more_
     *
     * @param flagName
     * @param value
     */
    static public void set(String flagName, boolean value) {
        Object val;
        if (null == (val = map.get(flagName))) {
            changed = true;
        }
        map.put(flagName, new Boolean(value));
    }

    /**
     * _more_
     */
    static public void clear() {
        map = new TreeMap();
    }

    /**
     * _more_
     *
     * @param topMenu
     */
    static public void constructMenu(JMenu topMenu) {
        if (debug) {
            System.out.println("Debug.constructMenu " + changed);
        }
        if ( !changed) {
            return;
        }
        changed = false;

        if (topMenu.getItemCount() > 0) {
            topMenu.removeAll();
        }

        Set keySet = map.keySet();
        if (null == keySet) {
            return;
        }

        // sort it
        ArrayList list = new ArrayList(keySet);
        Collections.sort(list);

        Iterator iter = list.iterator();
        while (iter.hasNext()) {
            Object  key  = iter.next();
            String  name = (String) key;
            Object  val  = map.get(key);
            boolean bval = ((Boolean) val).booleanValue();

            addDebugToMenu(topMenu, name, name, bval);
            if (debug) {
                System.out.println("Debug.constructMenu " + name);
            }
        }

        topMenu.revalidate();
    }


    // recursive menu adding

    /**
     * _more_
     *
     * @param menu
     * @param fullname
     * @param name
     * @param bval
     */
    static private void addDebugToMenu(JMenu menu, String fullname,
                                       String name, boolean bval) {
        int pos = name.indexOf('.');

        if (pos >= 0) {  // heirarchical menu: branchName.leafName
            String branchName = name.substring(0, pos);  // break the string in two
            String leafName = name.substring(pos + 1);

            JMenu  branch;
            if (null == (branch = findMenu(menu, branchName))) {
                branch = new JMenu(branchName);                 // new branch
                menu.add(branch);
            }
            addDebugToMenu(branch, fullname, leafName, bval);   // recurse
        } else {
            menu.add(new DebugMenuItem(fullname, name, bval));  // new leaf
        }
    }

    /**
     * _more_
     *
     * @param menu
     * @param name
     * @return _more_
     */
    static private JMenu findMenu(JMenu menu, String name) {
        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item instanceof JMenu) {
                JMenu m = (JMenu) item;
                if (name.equals(m.getText())) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * Class DebugMenuItem
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private static class DebugMenuItem extends JCheckBoxMenuItem {

        /** _more_ */
        private String fullname;

        /**
         * _more_
         *
         * @param foolName
         * @param menuName
         * @param val
         *
         */
        DebugMenuItem(String foolName, String menuName, boolean val) {
            super(menuName, val);
            fullname = foolName;

            addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent evt) {
                    //System.out.println("MyMI "+getText()+" "+getState());
                    map.put(fullname, new Boolean(getState()));
                }
            });
        }
    }

}

