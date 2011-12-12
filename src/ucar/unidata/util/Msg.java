/*
 * $Id: Msg.java,v 1.14 2007/03/15 19:16:45 jeffmc Exp $
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

import java.io.*;

import java.io.InputStream;





/**
 * A collection of utilities for doing logging and user messaging
 *
 * @author IDV development team
 */


import java.lang.reflect.InvocationTargetException;

import java.util.ArrayList;



import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;



/**
 * Class Msg
 *
 *
 * @author Unidata development team
 */
public class Msg {

    /** _more_          */
    private static Hashtable dontComponents = new Hashtable();

    /** _more_          */
    private static Hashtable dontRecurseComponents = new Hashtable();

    /** _more_ */
    private static boolean enabled = false;

    /** _more_ */
    private static Properties messages;

    /** _more_          */
    private static boolean showNoMsg = false;

    /** _more_ */
    private static boolean debug = false;

    /** _more_ */
    private static Hashtable seenMsgs;

    /** _more_ */
    private static OutputStream seenStream;

    /**
     * _more_
     *
     * @param b _more_
     */
    public static void setShowDebug(boolean b) {
        showNoMsg = b;
        if (b) {
            enabled = true;
        }
    }


    /**
     * _more_
     *
     * @param rc _more_
     */
    public static void init(ResourceCollection rc) {
        for (int i = rc.size() - 1; i >= 0; i--) {
            InputStream is = null;
            try {
                is = IOUtil.getInputStream((String) rc.get(i), Msg.class);
            } catch (Exception exc) {}
            if (is == null) {
                continue;
            }
            if (messages == null) {
                messages = new Properties();
            }
            try {
                messages.load(is);
                is.close();
            } catch (Exception exc) {
                System.err.println("Loading msgs:" + exc);
            }
        }
        if (messages == null) {
            enabled = false;
            return;
        }
        enabled = (seenStream != null) || showNoMsg || (messages.size() > 0);
        if (enabled) {
            initStatics();
        }
        //      System.err.println ("messages:" + messages);
    }


    /**
     * _more_
     *
     * @param file _more_
     */
    public static void recordMessages(File file) {
        if (messages == null) {
            messages = new Properties();
        }
        enabled = true;
        if (seenMsgs != null) {
            return;
        }
        try {

            Properties existing = null;
            if (file.exists()) {
                if (GuiUtils.showYesNoDialog(
                        null,
                        "The file: " + file
                        + " exists. Do you want to merge these already defined messages?", "File exists")) {
                    existing = Misc.readProperties(file.toString(), null,
                            Msg.class);
                }
            }
            seenMsgs   = new Hashtable();
            seenStream = new FileOutputStream(file);
            if (existing != null) {
                for (Enumeration keys = existing.keys();
                        keys.hasMoreElements(); ) {
                    String key   = (String) keys.nextElement();
                    String value = (String) existing.get(key);
                    seenMsgs.put(key, key);
                    seenStream.write((key + "=" + value + "\n").getBytes());
                    seenStream.flush();
                }
            }
        } catch (Exception exc) {
            System.err.println("Error in Msg.recordMessages:" + file);
            exc.printStackTrace();
        }
    }



    /**
     * _more_
     */
    public static void initStatics() {
        if (messages == null) {
            return;
        }
        //        GuiUtils.initLabels();
    }



    /**
     * _more_
     *
     * @param original _more_
     *
     * @return _more_
     */
    public static String msg(String original) {
        if ( !enabled) {
            return original;
        }
        return _msg(original, original);
    }

    /**
     * _more_
     *
     * @param original _more_
     * @param param1 _more_
     *
     * @return _more_
     */
    public static String msg(String original, String param1) {
        if (enabled) {
            original = msg(original);
        }
        return StringUtil.replace(original, "${param1}", param1);
    }

    /**
     * _more_
     *
     * @param original _more_
     * @param param1 _more_
     * @param param2 _more_
     *
     * @return _more_
     */
    public static String msg(String original, String param1, String param2) {
        if (enabled) {
            original = msg(original);
        }
        original = StringUtil.replace(original, "${param1}", param1);
        original = StringUtil.replace(original, "${param2}", param2);
        return original;
    }


    /**
     * _more_
     *
     * @param original _more_
     */
    private static void write(String original) {
        if (seenMsgs != null) {
            String trimmed = original.trim();
            trimmed = StringUtil.replace(trimmed, "=", "");
            String name = getKey(trimmed);
            if ((name.length() > 0) && (seenMsgs.get(name) == null)) {
                seenMsgs.put(name, name);
                try {
                    seenStream.write((name + "=" + trimmed
                                      + "\n").getBytes());
                    seenStream.flush();
                } catch (Exception exc) {
                    System.err.println("Error writing messages");
                    exc.printStackTrace();
                    seenMsgs   = null;
                    seenStream = null;
                }

            }
        }
    }

    /**
     * _more_
     *
     * @param name _more_
     *
     * @return _more_
     */
    private static String getKey(String name) {
        name = StringUtil.replace(name, " ", "_");
        name = StringUtil.replace(name, ":", "");
        name = StringUtil.replace(name, "...", "");
        //      name = name.toLowerCase();
        name = name.trim();
        return name;
    }


    /**
     * _more_
     *
     * @param original _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private static String _msg(String original, String dflt) {
        if (showNoMsg) {
            if (dflt != null) {
                if ((dflt.trim().length() > 0) && !dflt.startsWith("NO-")) {
                    dflt = "NO-" + dflt;
                }
            } else if (original != null) {
                if ((original.trim().length() > 0)
                        && !original.startsWith("NO-")) {
                    dflt = "NO-" + original;
                }
            }
        }

        if ( !enabled || (original == null) || (messages == null)) {
            return dflt;
        }
        if (debug) {
            System.err.println("msg: original: <" + original + ">");
        }
        String name = getKey(original);
        String tmp  = (String) messages.get(name);
        if (tmp != null) {
            if (debug) {
                System.err.println("msg: got it:" + tmp);
            }
            return tmp;
        }
        if (original.startsWith("<html>")) {
            return dflt;
        }
        String trimmed = original.trim();
        if (trimmed.endsWith(":")) {
            if (debug) {
                System.err.println("msg-1:" + "got ':'");
            }
            String substring = trimmed.substring(0, trimmed.length() - 1);
            if (tmp == null) {
                write(substring);
                return dflt;
            }

            int index = 0;
            tmp = tmp + ":";
            while (original.startsWith(" ", index)) {
                tmp = " " + tmp;
                index++;
            }
            while (original.endsWith(" ")) {
                tmp      = tmp + " ";
                original = original.substring(0, original.length() - 2);
            }
            return tmp + ":";
        }
        if (original.endsWith("...")) {
            String substring = original.substring(0, original.length() - 3);
            if (tmp == null) {
                write(substring);
                return dflt;
            }
            return tmp + "...";
        }
        write(original);
        return dflt;
    }


    /**
     * _more_
     *
     * @param comp _more_
     */
    public static void addDontComponent(Component comp) {
        dontComponents.put(comp, comp);
    }

    /**
     * _more_
     *
     * @param comp _more_
     */
    public static void addDontRecurseComponent(Component comp) {
        dontRecurseComponents.put(comp, comp);
    }


    /**
     * _more_
     *
     * @param comp _more_
     */
    public static void translateTree(Component comp) {
        if ( !enabled || (messages == null)) {
            return;
        }
        translateTree(comp, true, false);
    }

    /**
     * _more_
     *
     * @param comp _more_
     * @param doTabTitles _more_
     */
    public static void translateTree(Component comp, boolean doTabTitles) {
        if ( !enabled || (messages == null)) {
            return;
        }
        translateTree(comp, doTabTitles, false);
    }


    /** _more_          */
    static List stack = new ArrayList();

    /**
     * _more_
     *
     * @param comp _more_
     * @param doTabTitles _more_
     * @param debug _more_
     */
    public static void translateTree(Component comp, boolean doTabTitles,
                                     boolean debug) {
        if ((comp == null) || !enabled || (messages == null)) {
            return;
        }
        if (dontComponents.get(comp) != null) {
            return;
        }

        //This enables client code to do the translateTree but to skip
        //parts of the gui
        if (comp instanceof SkipPanel) {
            return;
        }

        if (debug) {
            System.err.println("comp:" + comp.getClass().getName());
        }
        if (comp instanceof JComponent) {
            String tt = ((JComponent) comp).getToolTipText();
            if (tt != null) {
                tt = _msg(tt, null);
                if (tt != null) {
                    ((JComponent) comp).setToolTipText(tt);
                }
            }
        }


        if ((comp instanceof JTabbedPane) && doTabTitles) {
            JTabbedPane tab = (JTabbedPane) comp;
            for (int i = 0; i < tab.getTabCount(); i++) {
                String title = _msg(tab.getTitleAt(i), null);
                if (title != null) {
                    tab.setTitleAt(i, title);
                }
            }
        }

        if (comp instanceof JMenuItem) {
            JMenuItem mi  = (JMenuItem) comp;
            String    msg = _msg(mi.getText(), null);
            if (msg != null) {
                mi.setText(msg);
                String mnemonic = _msg("mnemonic_" + msg, null);
                if (mnemonic != null) {
                    int keyCode = GuiUtils.charToKeyCode(mnemonic);
                    if (keyCode != -1) {
                        //For now, don't do this because this triggers 
                        //a menu selected event  which can result in an infinite loop
                        //                        mi.setMnemonic(keyCode);
                    }
                }
            }
            if ((comp instanceof JMenu)
                    && (dontRecurseComponents.get(comp) == null)) {
                JMenu menu = (JMenu) comp;
                for (int i = 0; i < menu.getItemCount(); i++) {
                    translateTree(menu.getItem(i), doTabTitles, debug);
                }
            }
            return;
        }

        if (comp instanceof JLabel) {
            JLabel label   = (JLabel) comp;
            String lblText = label.getText();
            if (lblText != null) {
                String msg = _msg(lblText, null);
                if (msg != null) {
                    ((JLabel) comp).setText(msg);
                }
            }
            return;
        }

        if (comp instanceof AbstractButton) {
            AbstractButton btn = (AbstractButton) comp;
            String         msg = _msg(btn.getText(), null);
            if (msg != null) {
                ((AbstractButton) comp).setText(msg);
            }
            return;
        }
        if ( !(comp instanceof Container) || (comp instanceof JTree)
                || (comp instanceof JTable) || (comp instanceof JList)) {
            return;
        }

        if (dontRecurseComponents.get(comp) != null) {
            return;
        }

        Container c = (Container) comp;
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponent(i);
            translateTree(child, doTabTitles, debug);
        }
    }

    /**
     * Class SkipPanel _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.14 $
     */
    public static class SkipPanel extends JPanel {

        /**
         * _more_
         *
         * @param layout _more_
         */
        public SkipPanel(LayoutManager layout) {
            super(layout);
        }

        /**
         * _more_
         *
         * @param comp _more_
         */
        public SkipPanel(Component comp) {
            setLayout(new BorderLayout());
            this.add(BorderLayout.CENTER, comp);
        }

    }


}

