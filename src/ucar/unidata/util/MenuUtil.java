/*
 * $Id: GuiUtils.java,v 1.317 2007/08/10 14:26:33 jeffmc Exp $
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
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.lang.reflect.*;



import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * This is a vast catchall class to old various
 * utilities for doing GUI things.
 *
 *
 * @author IDV development team
 */
public class MenuUtil {

    /** Separator flag */
    public static final String MENU_SEPARATOR = "separator";




    /**
     * Returns true if the Classes defined in the actual parameter
     * are equal or a sub-class of the corresponding classes defined in the
     * formal argument.
     *
     * @param formals     formal classes (types)
     * @param actuals     actual classes
     * @return   true  if they match
     */
    public static boolean typesMatch(Class[] formals, Class[] actuals) {
        if (formals.length != actuals.length) {
            return false;
        }
        for (int j = 0; j < formals.length; j++) {
            if (actuals[j] == null) {
                continue;
            }
            if ( !formals[j].isAssignableFrom(actuals[j])) {
                return false;
            }
        }
        return true;
    }




    /**
     * Find all methods with the given name.
     * Of these methods find one whose parameter types
     * are assignable from the given parameter types.
     *
     * @param c            class to check
     * @param methodName   name of method
     * @param paramTypes   parameter types
     * @return  class method or <code>null</code> if one doesn't exist
     */
    public static Method findMethod(Class c, String methodName,
                                    Class[] paramTypes) {
        ArrayList all     = new ArrayList();
        Method[]  methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            if ( !methodName.equals(methods[i].getName())) {
                continue;
            }
            if (paramTypes == null) {
                return methods[i];
            }
            if (typesMatch(methods[i].getParameterTypes(), paramTypes)) {
                all.add(methods[i]);
            }
        }
        if (all.size() > 1) {
            String msg = "More than one method: " + methodName
                         + " found for class:" + c.getName();
            for (int i = 0; i < paramTypes.length; i++) {
                if (paramTypes[i] != null) {
                    msg += " " + paramTypes[i].getName();
                }
            }
            throw new IllegalArgumentException(msg);
        }
        if (all.size() == 1) {
            return (Method) all.get(0);
        }
        return null;
    }



    /**
     * Find a method
     *
     * @param object     Object with the method in it
     * @param methodName method name
     * @param arg        method argument
     *
     * @return  the method
     */
    private static Method findMethod(Object object, String methodName,
                                     Object arg) {

        Method theMethod = null;
        if (arg == null) {
            theMethod = findMethod(object.getClass(), methodName,
                                        new Class[] {});
        } else {
            theMethod = findMethod(object.getClass(), methodName,
                                        new Class[] { arg.getClass() });
        }
        if (theMethod == null) {
            System.err.println("arg = " + arg);
            throw new IllegalArgumentException("Unknown method:"
                    + object.getClass() + "." + methodName + "("
                    + ((arg == null)
                       ? ""
                       : arg.getClass().getName()) + ")");
        }
        return theMethod;

    }




    /**
     * Make a jmenuItem. Call methodName on object when menuItem pressed. Pass in given arg
     * if non-null.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     * @param arg Pass this to method name if non-null.
     *
     * @return The menuItem
     */
    public static JMenuItem makeMenuItem(String label, final Object object,
                                         final String methodName,
                                         final Object arg) {
        return makeMenuItem(label, object, methodName, arg, false);
    }




    /**
     * Make a jmenuItem. Call methodName on object when menuItem pressed.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     *
     * @return The menuItem
     */
    public static JMenuItem makeMenuItem(String label, final Object object,
                                         final String methodName) {
        return makeMenuItem(label, object, methodName, null,false);
    }



    /**
     * Make a jmenuItem. Call methodName on object when menuItem pressed. Pass in given arg
     * if non-null.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     * @param arg Pass this to method name if non-null.
     * @param inThread If true then call the method in a thread
     *
     * @return The menuItem
     */
    public static JMenuItem makeMenuItem(String label, final Object object,
                                         final String methodName,
                                         final Object arg,
                                         final boolean inThread) {
        final Method    theMethod = findMethod(object, methodName, arg);
        final JMenuItem mi        = new JMenuItem(label);
        ActionListener  listener  = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (inThread) {
                    Thread t = new Thread() {
                            public void run() {
                                invokeMethod();
                            }
                        };
                    t.start();
                } else {
                    invokeMethod();
                }
            }

            public void invokeMethod() {
                try {
                    if (arg == null) {
                        theMethod.invoke(object, new Object[] {});
                    } else {
                        theMethod.invoke(object, new Object[] { arg });
                    }
                } catch (Exception exc) {
                    //                    LogUtil.logException("Error in makeMenuItem", exc);
                }
            }
        };
        mi.addActionListener(listener);
        return mi;
    }





    /**
     * Make a checkbox menu item. Automatically call the set'property' method on the object
     *
     * @param label Label
     * @param object  Object to call
     * @param property Name of property to get/set value
     * @param arg Optional arg to pass to method
     *
     * @return The checkbox
     */
    public static JCheckBoxMenuItem makeCheckboxMenuItem(String label,
            final Object object, final String property, final Object arg) {

        boolean value = true;
        try {
            String methodName = "get"
                                + property.substring(0, 1).toUpperCase()
                                + property.substring(1);
            Method theMethod = findMethod(object.getClass(), methodName,
                                   ((arg == null)
                                    ? new Class[] {}
                                    : new Class[] { arg.getClass() }));
            if (theMethod != null) {
                Boolean v = (Boolean) theMethod.invoke(object, ((arg == null)
                        ? new Object[] {}
                        : new Object[] { arg }));
                value = v.booleanValue();
            }
        } catch (Exception exc) {
            System.err.println("Error in makeCeckbox:" + exc);
            exc.printStackTrace();
        }
        return makeCheckboxMenuItem(label, object, property, value, arg);
    }



    /**
     * Make a checkbox menu item. Automatically call the set'property' method on the object
     *
     * @param label Label
     * @param object  Object to call
     * @param property Name of property to get/set value
     * @param value The value
     * @param arg Optional arg to pass to method
     *
     * @return The checkbox
     */
    public static JCheckBoxMenuItem makeCheckboxMenuItem(String label,
            final Object object, final String property, boolean value,
            final Object arg) {

        final JCheckBoxMenuItem cbx      = new JCheckBoxMenuItem(label,
                                               value);
        ItemListener            listener = new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                try {
                    String methodName =
                        "set" + property.substring(0, 1).toUpperCase()
                        + property.substring(1);
                    Method theMethod = findMethod(object.getClass(),
                                           methodName, ((arg == null)
                            ? new Class[] { Boolean.TYPE }
                            : new Class[] { Boolean.TYPE, arg.getClass() }));
                    if (theMethod == null) {
                        System.err.println("Unknown method:"
                                           + object.getClass() + "."
                                           + methodName);
                    } else {
                        theMethod.invoke(object, ((arg == null)
                                ? new Object[] {
                                    new Boolean(cbx.isSelected()) }
                                : new Object[] {
                                    new Boolean(cbx.isSelected()),
                                    arg }));
                    }
                } catch (Exception exc) {
                    System.err.println("Error in makeCheckbox:" + exc);
                    exc.printStackTrace();
                }
            }
        };
        cbx.addItemListener(listener);
        return cbx;
    }



    /**
     *  Create a JMenu and add the menus contained with the menus list
     *  If no menus then return null.
     *
     * @param name The menu name
     * @param menuItems List of either, JMenu, JMenuItem or MENU_SEPARATOR
     * @return The new menu
     */
    public static JMenu makeMenu(String name, List menuItems) {
        return makeMenu(new JMenu(name), menuItems);
    }

    /**
     *  Create a JMenu and add the menus contained with the menus list
     *  If no menus then return null.
     *
     * @param menu The menu to add to
     * @param menuItems List of either, JMenu, JMenuItem or MENU_SEPARATOR
     * @return The given menu
     */
    public static JMenu makeMenu(JMenu menu, List menuItems) {
        if (menuItems == null) {
            return menu;
        }
        for (int i = 0; i < menuItems.size(); i++) {
            Object o = menuItems.get(i);
            if (o.toString().equals(MENU_SEPARATOR)) {
                menu.addSeparator();
            } else if (o instanceof JMenuItem) {
                menu.add((JMenuItem) o);
            }
        }
        return menu;
    }


    /**
     * Utility to make a list of menu items.
     *
     * @param object The object to call the method on
     * @param items An array. Each sub array has at least two elements:<pre>
     * {Menu name, method name}
     * </pre>
     * If it has 3 elements then the 3rd element is an argument that is also passed
     * to the method. If it has 4 elements then the 4th element is a tooltip.
     * If there are 4 elements and the 3rd element is null then we don't try to find a method
     * that tags an extra argument.
     *
     * @return List of menu items
     */
    public static List makeMenuItems(Object object, Object[][] items) {
        List list = new ArrayList();
        for (int i = 0; i < items.length; i++) {
            JMenuItem menuItem = makeMenuItem(items[i][0].toString(), object,
                                     items[i][1].toString(),
                                     ((items[i].length >= 3)
                                      ? items[i][2]
                                      : null));

            if (items[i].length == 4) {
                menuItem.setToolTipText((String) items[i][3]);
            }
            list.add(menuItem);
        }
        return list;
    }

    /**
     * Create a popup menu and  show it near the given component
     *
     * @param menuItems List of menu items
     * @param comp Component to show the menu at
     */
    public static void showPopupMenu(List menuItems, Component comp) {
        JPopupMenu popup = makePopupMenu(menuItems);
        popup.show(comp, 0, (int) comp.getBounds().getHeight());
    }


    /**
     *  Create a JPopupMenu and add the menus contained with the menus list
     *  If no menus then return null.
     *
     * @param menu The menu
     * @param menuItems List of either, JMenu, JMenuItem or MENU_SEPARATOR
     * @return The given menu
     */
    public static JPopupMenu makePopupMenu(JPopupMenu menu, List menuItems) {
        if ((menuItems == null) || (menuItems.size() == 0)) {
            return null;
        }
        for (int i = 0; i < menuItems.size(); i++) {
            Object o = menuItems.get(i);
            if (o.toString().equals(MENU_SEPARATOR)) {
                menu.addSeparator();
            } else if (o instanceof JMenuItem) {
                menu.add((JMenuItem) o);
            } else if (o instanceof JMenu) {
                menu.add((JMenu) o);
            }
        }
        return menu;
    }

    /**
     *  Create a JPopupMenu and add the menus contained with the menus list
     *  If no menus then return null.
     *
     * @param menuItems List of either, JMenu, JMenuItem or MENU_SEPARATOR
     * @return The new popup menu
     */
    public static JPopupMenu makePopupMenu(List menuItems) {
        return makePopupMenu(new JPopupMenu(), menuItems);
    }






}



















