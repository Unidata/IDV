/*
 * $Id: BAMutil.java,v 1.16 2007/07/06 20:45:29 jeffmc Exp $
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



import java.awt.*;

import javax.swing.*;


/**
 * Button, Action and Menu utilities:
 * static helper methods for building ucar.unidata.UI's.
 *
 * @author John Caron
 * @version $Id: BAMutil.java,v 1.16 2007/07/06 20:45:29 jeffmc Exp $
 */
public class BAMutil {

    /** Action Property specifies Rollover icon name */
    public static final String ROLLOVER_ICON = "RolloverIcon";

    /** Action Property specifies is its a toggle */
    public static final String TOGGLE = "isToggle";

    /** Action Property specifies menu mneumonic */
    public static final String MNEMONIC = "mnemonic";

    /** Action Property specifies menu accelerator */
    public static final String ACCEL = "accelerator";

    /** the state of "toggle" actions = Boolean */
    public static final String STATE = "state";

    /** _more_ */
    static private String defaultResourcePath = "/auxdata/ui/icons/";  //BARF

    /** _more_ */
    static final private int META_KEY = java.awt.Event.CTRL_MASK;  // ??

    /** _more_ */
    static Class cl = (new BAMutil()).getClass();

    /** _more_ */
    static private boolean
        debug       = false,
        debugToggle = false;

    /**
     * Get the named Icon from the default resource (jar file).
     *  @param name name of the Icon ( will look for <name>.gif)
     *  @param errMsg true= print error message if not found
     *  @return the Icon or null if not found
     */
    public static ImageIcon getIcon(String name, boolean errMsg) {
        return ucar.unidata.util.GuiUtils.getScaledImageIcon(defaultResourcePath + name
                + ".gif", null, true);
    }

    /**
     * Get the named Image from the default resource (jar file).
     *  @param name name of the Image ( will look for <name>.gif)
     *  @return the Image or null if not found
     */
    public static Image getImage(String name) {
        return ucar.unidata.util.Resource.getImage(defaultResourcePath + name
                + ".gif");
    }

    /**
     * Make a cursor from the named Image in the default resource (jar file)
     *  @param name name of the Image ( will look for <name>.gif)
     *  @return the Cursor or null if failure
     */
    public static Cursor makeCursor(String name) {
        return ucar.unidata.util.Resource.makeCursor(defaultResourcePath
                + name + ".gif");
    }

    /**
     * Make a "buttcon" = button with an Icon
     *  @param icon the normal Icon
     *  @param rollover the rollover Icon
     *  @param tooltip the tooltip
     *  @param is_toggle if true, make JToggleButton, else JButton
     *  @return the buttcon (JButton or JToggleButton)
     */
    public static AbstractButton makeButtcon(Icon icon, Icon rollover,
                                             String tooltip,
                                             boolean is_toggle) {
        AbstractButton butt;
        if (is_toggle) {
            butt = new JToggleButton();
        } else {
            butt = new JButton();
        }

        if (debug) {
            System.out.println("   makeButtcon" + icon + " " + rollover + " "
                               + tooltip + " " + is_toggle);
        }

        if (icon != null) {
            butt.setIcon(icon);
        }
        if (rollover != null) {
            butt.setRolloverIcon(rollover);
            butt.setRolloverSelectedIcon(rollover);
            butt.setPressedIcon(rollover);
            butt.setRolloverEnabled(true);
        }

        butt.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        if (icon != null) {
            butt.setPreferredSize(new Dimension(icon.getIconHeight() + 2,
                    icon.getIconWidth() + 2));
        } else {
            butt.setPreferredSize(new Dimension(28, 28));
        }

        butt.setMaximumSize(new Dimension(28, 28));  // kludge

        butt.setToolTipText(tooltip);
        butt.setFocusPainted(false);

        return butt;
    }

    /**
     * _more_
     *
     * @param icon
     * @param rollover
     * @param menu_cmd
     * @param is_toggle
     * @param mnemonic
     * @param accel
     * @return _more_
     */
    private static JMenuItem makeMenuItem(Icon icon, Icon rollover,
                                          String menu_cmd, boolean is_toggle,
                                          int mnemonic, int accel) {
        JMenuItem mi;
        if (is_toggle) {
            mi = new JCheckBoxMenuItem(menu_cmd);
        } else {
            mi = new JMenuItem(menu_cmd);
        }

        if (icon != null) {
            mi.setIcon(icon);
        }
        if (rollover != null) {
            mi.setRolloverIcon(rollover);
            mi.setRolloverSelectedIcon(rollover);
            mi.setPressedIcon(rollover);
            mi.setRolloverEnabled(true);
        }

        mi.setHorizontalTextPosition(SwingConstants.LEFT);
        if (mnemonic != 0) {
            mi.setMnemonic(mnemonic);
        }
        if (accel != 0) {
            mi.setAccelerator(KeyStroke.getKeyStroke(accel, META_KEY));
        }

        return mi;
    }


    // NB: doesnt add action to MenuItem

    /**
     * _more_
     *
     * @param act
     * @return _more_
     */
    private static JMenuItem makeMenuItemFromAction(Action act) {
        // this prevents null pointer exception if user didnt call setProperties()
        Boolean tog       = (Boolean) act.getValue(BAMutil.TOGGLE);
        boolean is_toggle = (tog == null)
                            ? false
                            : tog.booleanValue();
        Integer mnu       = (Integer) act.getValue(BAMutil.MNEMONIC);
        int     mnemonic  = (tog == null)
                            ? -1
                            : mnu.intValue();
        Integer acc       = (Integer) act.getValue(BAMutil.ACCEL);
        int     accel     = (acc == null)
                            ? 0
                            : acc.intValue();

        return makeMenuItem((Icon) act.getValue(Action.SMALL_ICON),
                            (Icon) act.getValue(BAMutil.ROLLOVER_ICON),
                            (String) act.getValue(Action.SHORT_DESCRIPTION),
                            is_toggle, mnemonic, (accel < 0)
                ? 0
                : accel);
    }


    /**
     * creates a MenuItem using the given Action and adds it to the given Menu.
     * Uses Properties that have been set on the Action (see setActionProperties()). All
     * are optional except for Action.SHORT_DESCRIPTION:  <pre>
     *     Action.SHORT_DESCRIPTION   String     MenuItem text (required)
     *     Action.SMALL_ICON          Icon       the Icon to Use
     *     BAMutil.ROLLOVER_ICON      Icon       the rollover Icon
     *     BAMutil.TOGGLE             Boolean    true if its a toggle
     *     BAMutil.MNEMONIC           Integer    menu item shortcut
     *     BAMutil.ACCEL              Integer    menu item global keyboard accelerator
     * </pre><br>
     * The Action is triggered when the MenuItem is selected. Enabling and disabling the Action
     * does the same for the MenuItem. For toggles, state is maintained in the Action,
     * and MenuItem state changes when the Action state changes. <br><br>
     * The point of all this is that once you set it up, you work exclusively with the action object,
     * and all changes are automatically reflected in the UI.
     *
     * @param menu  add to this menu
     * @param act the Action to make it out of
     * @param menuPos
     * @return the MenuItem created
     */
    public static JMenuItem addActionToMenu(JMenu menu, Action act,
                                            int menuPos) {
        JMenuItem mi = makeMenuItemFromAction(act);
        if (menuPos >= 0) {
            menu.add(mi, menuPos);
        } else {
            menu.add(mi);
        }

        Boolean tog       = (Boolean) act.getValue(BAMutil.TOGGLE);
        boolean is_toggle = (tog == null)
                            ? false
                            : tog.booleanValue();
        Action  myAct     = is_toggle
                            ? new toggleAction(act)
                            : act;
        mi.addActionListener(myAct);
        act.addPropertyChangeListener(new myActionChangedListener(mi));
        return mi;
    }

    /**
     * _more_
     *
     * @param menu
     * @param act
     * @return _more_
     */
    public static JMenuItem addActionToMenu(JMenu menu, Action act) {
        return addActionToMenu(menu, act, -1);
    }

    // NB: doesnt add action to button

    /**
     * _more_
     *
     * @param act
     * @return _more_
     */
    private static AbstractButton makeButtconFromAction(Action act) {
        // this prevents null pointer exception if user didnt call setProperties()
        Boolean tog       = (Boolean) act.getValue(BAMutil.TOGGLE);
        boolean is_toggle = (tog == null)
                            ? false
                            : tog.booleanValue();

        return makeButtcon((Icon) act.getValue(Action.SMALL_ICON),
                           (Icon) act.getValue(BAMutil.ROLLOVER_ICON),
                           (String) act.getValue(Action.SHORT_DESCRIPTION),
                           is_toggle);
    }

    /**
     * creates an AbstractButton using the given Action and adds it to the given Container at the position..
     * Uses Properties that have been set on the Action (see setActionProperties()). All
     * are optional except for Action.SMALL_ICON:  <pre>
     *     Action.SMALL_ICON          Icon       the Icon to Use (required)
     *     BAMutil.ROLLOVER_ICON      Icon       the rollover Icon
     *     Action.SHORT_DESCRIPTION   String     tooltip
     *     BAMutil.TOGGLE             Boolean    true if its a toggle
     * </pre><br>
     * The Action is triggered when the Button is selected. Enabling and disabling the Action
     * does the same for the Button. For toggles, state is maintained in the Action,
     * and the Button state changes when the Action state changes. <br><br>
     * The point of all this is that once you set it up, you work exclusively with the action object,
     * and all changes are automatically reflected in the UI.
     *
     * @param c   add to this Container
     * @param act the Action to make it out of
     * @param pos add to the container at this position (if pos < 0, add at the end)
     * @return the AbstractButton created  (JButton or JToggleButton)
     */

    public static AbstractButton addActionToContainerPos(Container c,
            Action act, int pos) {
        AbstractButton butt = makeButtconFromAction(act);
        if (pos < 0) {
            c.add(butt);
        } else {
            c.add(butt, pos);
        }

        if (debug) {
            System.out.println(" addActionToContainerPos " + act + " " + butt
                               + " " + pos);
        }

        Boolean tog       = (Boolean) act.getValue(BAMutil.TOGGLE);
        boolean is_toggle = (tog == null)
                            ? false
                            : tog.booleanValue();
        Action  myAct     = is_toggle
                            ? new toggleAction(act)
                            : act;
        butt.addActionListener(myAct);
        act.addPropertyChangeListener(new myActionChangedListener(butt));
        return butt;
    }

    /**
     * Same as addActionToContainerPos, but add to end of Container
     *
     * @param c
     * @param act
     * @return _more_
     */
    public static AbstractButton addActionToContainer(Container c,
            Action act) {
        return addActionToContainerPos(c, act, -1);
    }

    /**
     * Standard way to set Properties for Actions.
     * This also looks for an Icon "<icon_name>Sel" and sets ROLLOVER_ICON if it exists.
     *
     *  If is_toggle, a toggle button is created (in addActionToContainer()), default state false
     *  To get or set the state of the toggle button:
     *    Boolean state = (Boolean) action.getValue(BAMutil.STATE);
     *    action.putValue(BAMutil.STATE, new Boolean(true/false));
     *
     * @param act          add properties to this action
     * @param icon_name    name of icon (or null).
     * @param action_name  menu name / tooltip
     * @param is_toggle    true if its a toggle
     * @param mnemonic     menu item shortcut
     * @param accel        menu item global keyboard accelerator
     */
    public static void setActionProperties(AbstractAction act,
                                           String icon_name,
                                           String action_name,
                                           boolean is_toggle, int mnemonic,
                                           int accel) {
        if (icon_name != null) {
            act.putValue(Action.SMALL_ICON, getIcon(icon_name, true));
            //            act.putValue(BAMutil.ROLLOVER_ICON,
            //                         getIcon(icon_name + "Sel", false));
        }
        act.putValue(Action.SHORT_DESCRIPTION, action_name);
        act.putValue(Action.LONG_DESCRIPTION, action_name);
        act.putValue(BAMutil.TOGGLE, new Boolean(is_toggle));
        act.putValue(BAMutil.MNEMONIC, new Integer(mnemonic));
        act.putValue(BAMutil.ACCEL, new Integer(accel));
    }

    /*
    public static void addActionToMenuAndToolbar( JMenu menu, JToolBar toolbar, Action act,
        String name, String icon_name, boolean is_toggle, int mnemonic, int accel) {

      AbstractButton butt = addAction( toolbar, act, name, icon_name, is_toggle);
      JMenuItem mi = addActionToMenu( menu, act, name, icon_name, is_toggle, mnemonic, accel);
    }


    public static void addActionToMenuAndToolbar2( JMenu menu, JComponent toolbar,
        int pos, Action act) {
      String name = (String) act.getValue( Action.SHORT_DESCRIPTION);
      Icon icon = (Icon) act.getValue( Action.SMALL_ICON);

      JMenuItem mi = new JMenuItem(name);
      mi.setIcon(icon);
      mi.setHorizontalTextPosition( SwingConstants.LEFT );
      mi.addActionListener( act);
      menu.add(mi);

      JButton butt = new JButton();
      butt.setIcon(icon);
      butt.setMaximumSize(new Dimension(24,24));       // kludge
      butt.setPreferredSize(new Dimension(24,24));
      butt.setToolTipText(name);
      butt.setFocusPainted(false);
      butt.addActionListener( act);
      toolbar.add(butt, pos);
    }   */


    /**
     * This wraps a regular action and makes it into a "toggle action",
     *  and associates it with an AbstractButton.
     *  Fetch/set the toggle state on the <U>original</u> action using:
     *  <pre>Boolean state = (Boolean) act.getValue(BAMutil.STATE);
     *  act.putValue(BAMutil.STATE, new Boolean(state)); </pre>
     *  It will automatically change the button state if the action state changes.
     */
    public static class ActionToggle extends AbstractAction {

        /** _more_ */
        private Action orgAct;

        /** _more_ */
        private AbstractButton button;

        /**
         * _more_
         *
         * @param oa
         * @param b
         *
         */
        public ActionToggle(Action oa, AbstractButton b) {
            this.orgAct = oa;
            this.button = b;
            orgAct.putValue(STATE, new Boolean(true));  // state is kept with original action

            orgAct.addPropertyChangeListener(
                new java.beans.PropertyChangeListener() {
                public void propertyChange(java.beans.PropertyChangeEvent e) {
                    String propertyName = e.getPropertyName();
                    if (debugToggle) {
                        System.out.println("propertyChange " + propertyName
                                           + " "
                                           + ((Boolean) e.getNewValue()));
                    }
                    if (propertyName.equals(Action.NAME)) {
                        String text = (String) e.getNewValue();
                        button.setText(text);
                        button.repaint();
                    } else if (propertyName.equals("enabled")) {
                        Boolean enabledState = (Boolean) e.getNewValue();
                        button.setEnabled(enabledState.booleanValue());
                        button.repaint();
                    } else if (propertyName.equals(STATE)) {
                        Boolean state = (Boolean) e.getNewValue();
                        button.setSelected(state.booleanValue());
                        button.repaint();
                    }
                }
            });
        }

        /**
         * _more_
         *
         * @param e
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            Boolean state = (Boolean) orgAct.getValue(BAMutil.STATE);
            orgAct.putValue(STATE, new Boolean( !state.booleanValue()));
            orgAct.actionPerformed(e);
        }
    }


    /**
     * Class toggleAction
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private static class toggleAction extends AbstractAction {

        /** _more_ */
        private Action orgAct;

        /**
         * _more_
         *
         * @param orgAct
         *
         */
        toggleAction(Action orgAct) {
            this.orgAct = orgAct;
            orgAct.putValue(STATE, new Boolean(false));  // state is kept with original action
        }

        /**
         * _more_
         *
         * @param e
         */
        public void actionPerformed(java.awt.event.ActionEvent e) {
            Boolean state = (Boolean) orgAct.getValue(STATE);
            orgAct.putValue(STATE, new Boolean( !state.booleanValue()));
            if (debugToggle) {
                System.out.println("toggleAction " + state);
            }
            orgAct.actionPerformed(e);
        }
    }

    /**
     * Class myActionChangedListener
     *
     *
     * @author Unidata development team
     * @version %I%, %G%
     */
    private static class myActionChangedListener implements java.beans
        .PropertyChangeListener {

        /** _more_ */
        private AbstractButton button;

        /**
         * _more_
         *
         * @param b
         *
         */
        myActionChangedListener(AbstractButton b) {
            button = b;
        }

        /**
         * _more_
         *
         * @param e
         */
        public void propertyChange(java.beans.PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (debugToggle) {
                System.out.println("propertyChange " + propertyName + " "
                                   + ((Boolean) e.getNewValue()));
            }
            if (propertyName.equals(Action.NAME)) {
                String text = (String) e.getNewValue();
                button.setText(text);
                button.repaint();
            } else if (propertyName.equals("enabled")) {
                Boolean enabledState = (Boolean) e.getNewValue();
                button.setEnabled(enabledState.booleanValue());
                button.repaint();
            } else if (propertyName.equals(STATE)) {
                Boolean state = (Boolean) e.getNewValue();
                button.setSelected(state.booleanValue());
                button.repaint();
            }
        }
    }

}

/*
 *  Change History:
 *  $Log: BAMutil.java,v $
 *  Revision 1.16  2007/07/06 20:45:29  jeffmc
 *  A big J&J
 *
 *  Revision 1.15  2005/05/13 18:31:42  jeffmc
 *  Clean up the odd copyright symbols
 *
 *  Revision 1.14  2004/09/07 18:36:20  jeffmc
 *  Jindent and javadocs
 *
 *  Revision 1.13  2004/08/23 17:27:48  dmurray
 *  silence some javadoc warnings now that we are at 1.4
 *
 *  Revision 1.12  2004/02/27 21:19:16  jeffmc
 *  Lots of javadoc warning fixes
 *
 *  Revision 1.11  2004/01/29 17:37:07  jeffmc
 *  A big sweeping checkin after a big sweeping reformatting
 *  using the new jindent.
 *
 *  jindent adds in javadoc templates and reformats existing javadocs. In the new javadoc
 *  templates there is a '_more_' to remind us to fill these in.
 *
 *  Revision 1.10  2002/11/26 16:48:25  jeffmc
 *  Have the buttcons be smaller - no border and set their preferred size to the icon size
 *
 *  Revision 1.9  2001/02/06 23:03:38  caron
 *  add addActionToMenu( JMenu menu, Action act, int menuPos)
 *
 *  Revision 1.8  2000/09/27 19:44:37  caron
 *  move to auxdata
 *
 *  Revision 1.7  2000/08/28 22:39:18  caron
 *  new icons
 *
 *  Revision 1.6  2000/08/18 04:15:52  russ
 *  Licensed under GNU LGPL.
 *
 *  Revision 1.5  2000/05/09 20:39:04  caron
 *  add toggleAction
 *
 *  Revision 1.4  2000/04/26 21:14:01  caron
 *  latest version of GDV
 *
 *  Revision 1.3  1999/06/03 01:44:09  caron
 *  remove the damn controlMs
 *
 *  Revision 1.2  1999/06/03 01:27:04  caron
 *  another reorg
 *
 *  Revision 1.1.1.1  1999/05/21 17:33:48  caron
 *  startAgain
 *
 * # Revision 1.6  1999/03/26  19:58:12  caron
 * # add SpatialSet; update javadocs
 * #
 * # Revision 1.5  1999/03/18  18:21:17  caron
 * # bug fixes
 * #
 * # Revision 1.4  1999/03/16  17:00:18  caron
 * # fix StationModel editing; add TopLevel
 * #
 * # Revision 1.3  1999/03/08  19:45:36  caron
 * # world coord now Point2D
 * #
 * # Revision 1.2  1999/03/03  19:59:14  caron
 * # more java2D changes
 * #
 * # Revision 1.1  1999/02/15  23:06:33  caron
 * # upgrade to java2D, new ProjectionManager
 * #
 * # Revision 1.3  1998/12/14  17:11:56  russ
 * # Add comment for accumulating change histories.
 * #
 */






