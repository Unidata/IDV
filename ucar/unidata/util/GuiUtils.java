/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.awt.image.*;


import java.io.*;

import java.lang.reflect.*;

import java.net.URI;

import java.net.URL;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import javax.swing.text.*;
import javax.swing.tree.*;



/**
 * This is a vast catchall class to old various
 * utilities for doing GUI things.
 *
 *
 * @author IDV development team
 */
public class GuiUtils extends LayoutUtil {

    /** _more_ */
    private static String applicationTitle = "";

    /** missing image path */
    public static String MISSING_IMAGE = "/ucar/unidata/util/scream.gif";

    /** xml attribute name */
    public static final String ATTR_ACTION = "action";

    /** xml attribute name */
    public static final String ATTR_TOOLTIP = "tooltip";

    /** xml attribute name */
    public static final String ATTR_ICON = "icon";

    /** xml attribute name */
    public static final String ATTR_ID = "id";


    /** The cursor to use when waiting */
    public static final Cursor waitCursor =
        Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);

    /** The normal cursor_ */
    public static final Cursor normalCursor =
        Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);


    /** This is used to log errors to LogUtil */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(GuiUtils.class.getName());


    /** font sizes */
    public static final int[] FONT_SIZES = {
        8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 24, 26, 28, 32, 36, 40, 48,
        56, 64, 72
    };

    /** Used to map named colors to color */
    public static final String[] COLORNAMES = {
        "blue", "black", "red", "gray", "light gray", "white", "green",
        "orange", "cyan", "magenta", "pink", "yellow"
    };


    /** Used to map named colors to color */
    public static final Color[] COLORS = {
        Color.blue, Color.black, Color.red, Color.gray, Color.lightGray,
        Color.white, Color.green, Color.orange, Color.cyan, Color.magenta,
        Color.pink, Color.yellow
    };



    /** Action command used for the Apply button */
    public static String CMD_APPLY = "Apply";

    /** Action command used for the Cancel button */
    public static String CMD_CANCEL = "Cancel";

    /** Action command used for the Close button */
    public static String CMD_CLOSE = "Close";

    /** Action command used for the Import button */
    public static String CMD_IMPORT = "Import";

    /** Action command used for the Export button */
    public static String CMD_EXPORT = "Export";

    /** Action command used for the Submit button */
    public static String CMD_SUBMIT = "Submit";

    /** Action command used for the Rename button */
    public static String CMD_RENAME = "Rename";

    /** Action command used for the Remove button */
    public static String CMD_REMOVE = "Remove";

    /** Action command used for the New button */
    public static String CMD_NEW = "New";

    /** Action command used for the Yes button */
    public static String CMD_YES = "Yes";

    /** Action command used for the No button */
    public static String CMD_NO = "No";

    /** Action command used for the OK button */
    public static String CMD_OK = "OK";

    /** Action command used for the Open button */
    public static String CMD_OPEN = "Open";

    /** Action command used for the Reset button */
    public static String CMD_RESET = "Reset";

    /** Action command used for the Help button */
    public static String CMD_HELP = "Help";

    /** Action command used for the Save button */
    public static String CMD_SAVE = "Save";

    /** Action command used for the Saveas button */
    public static String CMD_SAVEAS = "Save as";

    /** Action command used for the Update button */
    public static String CMD_UPDATE = "Update";

    /** Action command used for the Start button */
    public static String CMD_START = "Start";

    /** Action command used for the Stop button */
    public static String CMD_STOP = "Stop";


    /** Used by apps for having a common font for buttons */
    public static Font buttonFont = new Font("Dialog", Font.BOLD, 10);


    /** Holds a mapping from image filename to Image object */
    private static Hashtable imageCache = new Hashtable();


    /** Do we put the icons in the menus */
    private static boolean setIconsInMenus = true;


    /** default icon size */
    private static int dfltIconSize = -1;

    /** The default timezone to use for formatting */
    private static TimeZone defaultTimeZone;


    /** Default date format */
    private static SimpleDateFormat defaultDateFormat =
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");


    /**
     * Dummy ctor for doclint
     */
    private GuiUtils() {}



    /**
     * Init the fixed labels
     */
    public static void initLabels() {
        GuiUtils.CMD_APPLY  = Msg.msg(GuiUtils.CMD_APPLY);
        GuiUtils.CMD_CANCEL = Msg.msg(GuiUtils.CMD_CANCEL);
        GuiUtils.CMD_CLOSE  = Msg.msg(GuiUtils.CMD_CLOSE);
        GuiUtils.CMD_IMPORT = Msg.msg(GuiUtils.CMD_IMPORT);
        GuiUtils.CMD_EXPORT = Msg.msg(GuiUtils.CMD_EXPORT);
        GuiUtils.CMD_RENAME = Msg.msg(GuiUtils.CMD_RENAME);
        GuiUtils.CMD_REMOVE = Msg.msg(GuiUtils.CMD_REMOVE);
        GuiUtils.CMD_NEW    = Msg.msg(GuiUtils.CMD_NEW);
        GuiUtils.CMD_YES    = Msg.msg(GuiUtils.CMD_YES);
        GuiUtils.CMD_NO     = Msg.msg(GuiUtils.CMD_NO);
        GuiUtils.CMD_OK     = Msg.msg(GuiUtils.CMD_OK);
        GuiUtils.CMD_OPEN   = Msg.msg(GuiUtils.CMD_OPEN);
        GuiUtils.CMD_RESET  = Msg.msg(GuiUtils.CMD_RESET);
        GuiUtils.CMD_HELP   = Msg.msg(GuiUtils.CMD_HELP);
        GuiUtils.CMD_SAVE   = Msg.msg(GuiUtils.CMD_SAVE);
        GuiUtils.CMD_SAVEAS = Msg.msg(GuiUtils.CMD_SAVEAS);
        GuiUtils.CMD_UPDATE = Msg.msg(GuiUtils.CMD_UPDATE);
        GuiUtils.CMD_START  = Msg.msg(GuiUtils.CMD_START);
        GuiUtils.CMD_STOP   = Msg.msg(GuiUtils.CMD_STOP);
    }







    /**
     * Set the default timezone used for formatting
     *
     * @param tz timezone
     */
    public static void setTimeZone(TimeZone tz) {
        defaultTimeZone = tz;
    }

    public static final TimeZone TIMEZONE_UTC = TimeZone.getTimeZone("UTC");


    /**
     * Get the default timezone used for formatting
     *
     * @return timezone
     */
    public static TimeZone getTimeZone() {
        if (defaultTimeZone == null) {
            return TIMEZONE_UTC;
        }
        return defaultTimeZone;
    }

    /**
     * Set the default date format
     *
     * @param fmt date format string
     */
    public static void setDefaultDateFormat(String fmt) {
        defaultDateFormat = new SimpleDateFormat(fmt);
        defaultDateFormat.setTimeZone(defaultTimeZone);
    }

    /**
     * Format the date with the default date format and timezone
     *
     * @param dttm date
     *
     * @return formatted date
     */
    public static String formatDate(Date dttm) {
        return defaultDateFormat.format(dttm);
    }


    /**
     * Set the default icon size
     *
     * @param size  new size
     */
    public static void setDefaultIconSize(int size) {
        dfltIconSize = size;
    }

    /**
     *  Get the default icon size
     *
     * @return  the default icon size
     */
    public static int getDefaultIconSize() {
        return dfltIconSize;
    }

    /** default font */
    private static Font dfltFont;

    /**
     * Set the default font
     *
     * @param font the default font
     */
    public static void setDefaultFont(Font font) {
        dfltFont = font;
        if (dfltFont != null) {
            buttonFont = dfltFont;
        }
    }

    /**
     * Get the default font
     *
     * @return the default font
     */
    public static Font getDefaultFont() {
        if (dfltFont != null) {
            return dfltFont;
        }
        return null;
    }


    /**
     * _more_
     *
     * @param comp _more_
     */
    public static void applyDefaultFont(Component comp) {
        if (dfltFont != null) {
            comp.setFont(dfltFont);
        }
    }


    /**
     * Create a JLabel with a fixed width font
     *
     * @param s Initial label string
     *
     * @return label
     */
    public static JLabel getFixedWidthLabel(String s) {
        JLabel lbl     = new JLabel(s);
        Font   lblFont = lbl.getFont();
        Font monoFont = new Font("Monospaced", lblFont.getStyle(),
                                 lblFont.getSize());

        lbl.setFont(monoFont);
        return lbl;

    }



    /**
     * Set the font on the component to be monospaced
     *
     * @param comp The component
     */
    public static void setFixedWidthFont(Component comp) {
        Font lblFont = comp.getFont();
        Font monoFont = new Font("Monospaced", lblFont.getStyle(),
                                 lblFont.getSize());

        comp.setFont(monoFont);

    }








    /**
     * This finds the Window the given component c is in and,
     * if it is found, sets the cursor of the Window to the given cursor.
     *
     * @param component The component  to look for the window from
     * @param cursor The cursor
     */
    public static void setCursor(Component component, Cursor cursor) {
        Window f = getWindow(component);
        if (f != null) {
            f.setCursor(cursor);
        }
    }

    /**
     * This finds and returns the JFrame, or null if not found, that
     * holds the given component.
     *
     * @param component The component to look for the frame
     * @return The JFrame
     */
    public static JFrame getFrame(Component component) {
        if (component == null) {
            return null;
        }
        Component parent = component.getParent();
        while (parent != null) {
            if (JFrame.class.isAssignableFrom(parent.getClass())) {
                return (JFrame) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }


    /**
     * Show the component in any contained tabs
     *
     * @param comp The component  to look for the window from
     */
    public static void showComponentInTabs(Component comp) {
        showComponentInTabs(comp, true);
    }

    /**
     * Show the component in any containing tabs
     *
     * @param comp Component to show
     * @param andShowWindow If we get to a window do we also show it
     */
    public static void showComponentInTabs(Component comp,
                                           boolean andShowWindow) {
        if (comp == null) {
            return;
        }
        Component parent = comp.getParent();
        if (parent == null) {
            return;
        }
        if ((parent instanceof Window) && andShowWindow) {
            if ((parent instanceof JFrame)) {
                ((JFrame) parent).setState(Frame.NORMAL);
            }
            ((Window) parent).setVisible(true);
            toFront(((Window) parent));
            toFrontModalDialogs();
        }
        if (parent instanceof JTabbedPane) {
            ((JTabbedPane) parent).setSelectedComponent(comp);
        }
        //        if(parent instanceof ButtonTabbedPane.ComponentPanel) {
        //        }
        if (parent instanceof CardLayoutPanel) {
            CardLayoutPanel cardLayoutPanel = (CardLayoutPanel) parent;
            cardLayoutPanel.show(comp);
        }


        showComponentInTabs(parent, andShowWindow);
    }


    /**
     * Move the window to the front
     *
     * @param window the window
     */
    public static void toFront(Window window) {
        if (window == null) {
            return;
        }
        window.setVisible(true);
        window.toFront();
        if (window instanceof Frame) {
            Frame f = (Frame) window;
            //            f.setExtendedState(Frame.ICONIFIED);
            //            f.setExtendedState(Frame.NORMAL);
        }


    }


    /**
     * This finds and returns the Window, or null if not found, that contains
     * the given component.
     *
     * @param component The component  to look for the window from
     * @return The window the component is on or null if not found
     */
    public static Window getWindow(Component component) {
        if (component == null) {
            return null;
        }
        Component parent = component.getParent();
        while (parent != null) {
            if (parent instanceof Window) {
                return (Window) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }


    /**
     *  If non-null then removes all children and removes from parent.
     *
     * @param c The component to empty
     */
    public static void empty(Container c) {
        empty(c, false);
    }

    /**
     *  If non-null then removes all children and removes from parent.
     *
     * @param c The component to empty
     * @param doItInSwingThread If true then do the emptying in the Swing thread
     */
    public static void empty(final Container c, boolean doItInSwingThread) {
        if (c == null) {
            return;
        }
        if (doItInSwingThread) {
            invokeInSwingThread(new Runnable() {
                public void run() {
                    empty(c, false);
                }
            });
            return;
        }
        if (c != null) {
            c.removeAll();
            Container parent = c.getParent();
            if (parent != null) {
                parent.remove(c);
            }
        }
    }


    /**
     * This takes the  given String and tries to convert it to a color.
     * The string may be a space or comma separated triple of RGB integer
     * values. It may be an integer or it may be a color name defined in
     * the COLORNAMES array
     *
     * @param value String value
     * @param dflt This is returned if the value cannot be converted
     * @return Color defined by the String value or the dflt
     */
    public static Color decodeColor(String value, Color dflt) {
        if (value == null) {
            return dflt;
        }
        value = value.trim();
        if (value.equals("null")) {
            return null;
        }
        String s       = value;
        String lookFor = ",";
        int    i1      = s.indexOf(lookFor);
        if (i1 < 0) {
            lookFor = " ";
            i1      = s.indexOf(lookFor);
        }
        if (i1 > 0) {
            String red = s.substring(0, i1);
            s = s.substring(i1 + 1).trim();
            int i2 = s.indexOf(lookFor);
            if (i2 > 0) {
                String green = s.substring(0, i2);
                String blue  = s.substring(i2 + 1);
                try {
                    return new Color(Integer.decode(red).intValue(),
                                     Integer.decode(green).intValue(),
                                     Integer.decode(blue).intValue());
                } catch (Exception exc) {
                    System.err.println("Bad color:" + value);
                }
            }
        }

        try {
            return new Color(Integer.decode(s).intValue());
        } catch (Exception e) {
            s = s.toLowerCase();
            for (int i = 0; i < COLORNAMES.length; i++) {
                if (s.equals(COLORNAMES[i])) {
                    return COLORS[i];
                }
            }
        }
        return dflt;
    }


    /**
     * Return the name of the given color.
     *
     * @param color The color
     * @return name of the color
     */
    public static String getColorName(Color color) {
        for (int i = 0; i < COLORNAMES.length; i++) {
            if (color.equals(COLORS[i])) {
                return COLORNAMES[i];
            }
        }
        return "blue";
    }


    /**
     * Check the height against a value
     *
     * @param height  the value to check
     *
     * @return return height > 100;
     */
    public static boolean checkHeight(int height) {
        if (height > 100) {
            //            LogUtil.printMessage("Got large height when setting preferred size:" + height);
            //            LogUtil.printMessage(LogUtil.getStackTrace());
            return false;
        }
        return true;
    }


    /**
     * Set the preferred width on a component
     *
     * @param comp  component
     * @param width width
     */
    public static void setPreferredWidth(JComponent comp, int width) {
        int height = comp.getPreferredSize().height;
        if ( !checkHeight(height)) {
            //For now just set the height to some reasonable amount and use that
            height = 24;
        }
        comp.setPreferredSize(new Dimension(width, height));
    }

    /**
     * A color swatch panel
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.317 $
     */
    public static class ColorSwatch extends JPanel {

        /** flag for alpha */
        boolean doAlpha = false;

        /** color of the swatch */
        Color color;

        /** clear button */
        JButton clearBtn;

        /** set button */
        JButton setBtn;

        /** label */
        String label;

        /**
         * Create a new ColorSwatch for the specified color
         *
         * @param c  Color
         * @param dialogLabel  label for the dialog
         */
        public ColorSwatch(Color c, String dialogLabel) {
            this(c, dialogLabel, false);
        }

        /**
         * Create a new color swatch
         *
         * @param c   the color
         * @param dialogLabel label for the dialog
         * @param alphaOk  use alpha?
         */
        public ColorSwatch(Color c, String dialogLabel, boolean alphaOk) {
            this.doAlpha = alphaOk;
            this.color   = c;
            this.label   = dialogLabel;
            setMinimumSize(new Dimension(40, 10));
            setPreferredSize(new Dimension(40, 10));
            setToolTipText("Click to change color");
            setBackground(color);
            setBorder(BorderFactory.createLoweredBevelBorder());

            clearBtn = new JButton("Clear");
            clearBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    ColorSwatch.this.setBackground(null);
                }
            });

            setBtn = new JButton("Set");
            setBtn.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setColorFromChooser();
                }
            });


            this.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    Misc.run(new Runnable() {
                        public void run() {
                            showColorChooser();
                        }
                    });
                }
            });
        }


        /**
         * Show the color chooser
         */
        private void showColorChooser() {
            Color         oldColor    = this.getBackground();
            int           alpha       = oldColor.getAlpha();
            JColorChooser chooser     = new JColorChooser(oldColor);
            JSlider       alphaSlider = new JSlider(0, 255, alpha);
            JComponent    contents;
            if (doAlpha) {
                contents =
                    centerBottom(chooser,
                                 inset(hbox(new JLabel("Transparency:"),
                                            alphaSlider), new Insets(5, 5, 5,
                                                5)));
            } else {
                contents = chooser;
            }
            if ( !showOkCancelDialog(null, label, contents, null)) {
                return;
            }
            alpha = alphaSlider.getValue();
            //                    Color newColor = JColorChooser.showDialog(null, label,
            //                                                              oldColor);
            Color newColor = chooser.getColor();
            if (newColor != null) {
                newColor = new Color(newColor.getRed(), newColor.getGreen(),
                                     newColor.getBlue(), alpha);
                ColorSwatch.this.userSelectedNewColor(newColor);
            }
        }


        /**
         * Set color from chooser
         */
        private void setColorFromChooser() {
            Color newColor = JColorChooser.showDialog(null, label,
                                 this.getBackground());
            if (newColor != null) {
                ColorSwatch.this.userSelectedNewColor(newColor);
            }
        }

        /**
         * Get the set button
         *
         * @return the set button
         */
        public JButton getSetButton() {
            return setBtn;
        }

        /**
         * Get the clear button
         *
         * @return the clear button
         */
        public JButton getClearButton() {
            return clearBtn;
        }

        /**
         * Get the Color of the swatch
         *
         * @return the swatch color
         */
        public Color getSwatchColor() {
            return color;
        }

        /**
         * the user chose a new color. Set the background. THis can be overwritted by client code to act on the color change
         *
         * @param c color
         */
        public void userSelectedNewColor(Color c) {
            setBackground(c);
        }

        /**
         * Set the background to the color
         *
         * @param c  Color for background
         */
        public void setBackground(Color c) {
            color = c;
            super.setBackground(c);
        }

        /**
         * Paint this swatch
         *
         * @param g  graphics
         */
        public void paint(Graphics g) {
            Rectangle b = getBounds();
            if (color != null) {
                g.setColor(Color.black);
                for (int x = 0; x < b.width; x += 4) {
                    g.fillRect(x, 0, 2, b.height);
                }
            }

            super.paint(g);
            if (color == null) {
                g.setColor(Color.black);
                g.drawLine(0, 0, b.width, b.height);
                g.drawLine(b.width, 0, 0, b.height);
            }
        }

        /**
         * Get the panel
         *
         * @return the panel
         */
        public JComponent getPanel() {
            return GuiUtils.hbox(this, clearBtn, 4);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Color getColor() {
            return color;
        }


        /**
         * Get the panel that shows the swatch and the Set button.
         *
         * @return the panel
         */
        public JComponent getSetPanel() {
            List    comps    = Misc.newList(this);
            JButton popupBtn = new JButton("Change");
            popupBtn.addActionListener(makeActionListener(ColorSwatch.this,
                    "popupNameMenu", popupBtn));
            comps.add(popupBtn);
            return GuiUtils.hbox(comps, 4);
        }


        /**
         * Popup the named list menu
         *
         * @param popupBtn Popup near this button
         */
        public void popupNameMenu(JButton popupBtn) {
            List items = new ArrayList();
            for (int i = 0; i < COLORNAMES.length; i++) {
                items.add(makeMenuItem(COLORNAMES[i], this, "setColorName",
                                       COLORNAMES[i]));
            }
            items.add(GuiUtils.MENU_SEPARATOR);
            items.add(makeMenuItem("Custom", this, "setColorName", "custom"));
            showPopupMenu(items, popupBtn);
        }

        /**
         * Set the color based on name
         *
         * @param name color name
         */
        public void setColorName(String name) {
            if (name.equals("custom")) {
                setColorFromChooser();
                return;
            }
            Color newColor = decodeColor(name, getBackground());
            if (newColor != null) {
                ColorSwatch.this.setBackground(newColor);
            }
        }

    }

    /**
     * This makes a color swatch and a 'Set' button that is used to set the
     * color. It returns a 2 element array holding the swatch and the button.
     *
     * @param color The initial color
     * @param label The label to use when popping up the color dialog
     *
     * @return 3 element array that holds the swatch and the set button and the clear button
     */
    public static JComponent[] makeColorSwatchWidget(Color color,
            String label) {
        ColorSwatch swatch = new ColorSwatch(color, label);
        return new JComponent[] { swatch, swatch.setBtn, swatch.clearBtn };
    }


    /**
     * A helper to make a color selector combo box
     *
     * @param dflt The default color value
     * @return The color selector combo box
     */
    public static JComboBox makeColorNameComboBox(Color dflt) {
        Vector    names = new Vector(Misc.toList(COLORNAMES));
        JComboBox jcb   = new JComboBox(names);
        if (dflt != null) {
            String colorName = getColorName(dflt);
            if (colorName != null) {
                jcb.setSelectedItem(colorName);
            }
        }
        return jcb;
    }


    /**
     * Create an  ImageIcon from the given image file name.
     * The filename may be a file, a java resource or a url.
     *
     * @param file The image file
     * @return The ImageIcon or null if it could not be created
     */
    public static ImageIcon getImageIcon(String file) {
        return getImageIcon(file, true);
    }

    /**
     * Create an  ImageIcon from the given image file name.
     * The filename may be a file, a java resource or a url.
     *
     * @param file The image file
     * @param cache Should the local in memory cache be checked
     * @return The ImageIcon or null if it could not be created
     */
    public static ImageIcon getImageIcon(String file, boolean cache) {
        return getImageIcon(file, GuiUtils.class, cache);
    }

    /**
     * Create an  ImageIcon from the given image file name.
     * The filename may be a file, a java resource or a url.
     *
     * @param file The image file
     * @param c The class to use when looking up the image as a resource
     * @return The ImageIcon or null if it could not be created
     */
    public static ImageIcon getImageIcon(String file, Class c) {
        return getImageIcon(file, c, true);
    }


    /**
     * Create an  ImageIcon from the given image file name.
     * The filename may be a file, a java resource or a url.
     *
     * @param file The image file
     * @param c The class to use when looking up the image as a resource
     * @param cache Should the local in memory cache be checked
     * @return The ImageIcon or null if it could not be created
     */

    public static ImageIcon getImageIcon(String file, Class c,
                                         boolean cache) {
        if (c == null) {
            c = GuiUtils.class;
        }
        Image image = getImage(file, c, cache, false);
        if (image == null) {
            return null;
        }
        return new ImageIcon(image);
    }


    /**
     * Get a scaled image icon
     *
     * @param file  location of the image
     * @param c     relative class
     * @param cache  true to cache result
     *
     * @return  the icon or null
     */
    public static ImageIcon getScaledImageIcon(String file, Class c,
            boolean cache) {
        ImageIcon icon = getImageIcon(file, c, cache);
        return scaleImageIcon(icon);
    }


    /**
     * This scales the image icon up to the minimum icon size if it is defined
     *
     * @param icon The icon
     *
     * @return The scaled icon
     */
    public static ImageIcon scaleImageIcon(ImageIcon icon) {
        if (icon == null) {
            return null;
        }
        int w = icon.getIconWidth();
        if ((w > 0) && (w < dfltIconSize)) {
            Image image = icon.getImage();
            int   h     = image.getHeight(null);
            h     = (int) (h * (dfltIconSize / (double) w));
            image = image.getScaledInstance(dfltIconSize, h, 0);
            icon.setImage(image);
        }
        return icon;

    }






    /**
     * Create an  Image from the given image file name.
     * The filename may be a file, a java resource or a url.
     *
     * @param file The image file
     * @return The Image or null if it could not be created
     */
    public static Image getImage(String file) {
        return getImage(file, GuiUtils.class);
    }

    /**
     * Create an  Image from the given image file name.
     * The filename may be a file, a java resource or a url.
     *
     * @param file The image file
     * @param c Used to lookup the image as a java resource
     * @return The Image or null if it could not be created
     */
    public static Image getImage(String file, Class c) {
        return getImage(file, c, true);
    }



    /**
     * Create an  Image from the given image file name.
     * The filename may be a file, a java resource or a url.
     *
     * @param file The image file
     * @param c Used to lookup the image as a java resource
     * @param cache Should the local cache of Images be checked
     * @return The Image or null if it could not be created
     */
    public static Image getImage(String file, Class c, boolean cache) {
        return getImage(file, c, cache, false);
    }

    /**
     * Get an image
     *
     * @param file  location of the image
     * @param c     relative class
     * @param cache true to cache result
     * @param returnNullIfNotFound  true to return null;
     *
     * @return  image or null (if returnNullIfNotFound is true);
     */
    public static Image getImage(String file, Class c, boolean cache,
                                 boolean returnNullIfNotFound) {
        if ( !CacheManager.getDoCache()) {
            cache = false;
        }

        if (file == null) {
            return null;
        }
        if (c == null) {
            c = GuiUtils.class;
        }

        String key   = file + "-" + c.getName();
        Image  image = null;
        if (cache) {
            image = (Image) imageCache.get(key);
            if (image != null) {
                return image;
            }
        }

        try {
            InputStream is = IOUtil.getInputStream(file, c);
            if (is != null) {
                byte[] bytes = IOUtil.readBytes(is);
                image = Toolkit.getDefaultToolkit().createImage(bytes);
                if (cache) {
                    imageCache.put(key, image);
                }
                return image;
            }
            if (returnNullIfNotFound) {
                return null;
            }
        } catch (Exception exc) {
            if (returnNullIfNotFound) {
                return null;
            }
            System.err.println(exc + " getting image ");
        }

        System.err.println("Unable to find image:" + file);
        URL url = Misc.getURL(MISSING_IMAGE, GuiUtils.class);
        if (url == null) {
            System.err.println("Whoah, could not load missing image:"
                               + MISSING_IMAGE);
            return null;
        }
        return Toolkit.getDefaultToolkit().createImage(url);
    }





    /**
     * This will set the location of the theWindow component (might be
     * A JDialog or a JFrame) at the screen location of the given src component.
     * It will then show the theWindow.
     *
     * @param src  Where we locate
     * @param theWindow What we locate
     */
    public static void showDialogNearSrc(Component src, Component theWindow) {
        try {
            boolean iconified = ((src instanceof Frame)
                                 && ((Frame) src).getState()
                                    == Frame.ICONIFIED);

            if ((src != null) && !iconified) {
                Point loc = src.getLocationOnScreen();
                loc.y += src.getSize().height + 10;

                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
                //Offset a bit for the icon bar
                screenSize.height -= 50;
                Dimension windowSize = theWindow.getSize();

                if (loc.y + windowSize.height > screenSize.height) {
                    loc.y = screenSize.height - windowSize.height;
                }
                if (loc.x + windowSize.width > screenSize.width) {
                    loc.x = screenSize.width - windowSize.width;
                }
                theWindow.setLocation(loc);
            } else {
                Point center = getLocation(null);
                theWindow.setLocation(center.x - theWindow.getWidth() / 2,
                                      center.y - theWindow.getHeight() / 2);

            }
        } catch (Exception exc) {
            Point center = getLocation(null);
            theWindow.setLocation(center.x - theWindow.getWidth() / 2,
                                  center.y - theWindow.getHeight() / 2);

        }
        showWidget(theWindow);
    }


    /**
     * This will show and/or deiconify the given component.
     * The component needs to be a Window or a Frame
     *
     * @param c The thing to show.
     */
    public static void showWidget(Component c) {
        if (c == null) {
            return;
        }
        c.setVisible(true);
        if (c instanceof Frame) {
            if (((Frame) c).getState() == Frame.ICONIFIED) {
                ((Frame) c).setState(Frame.NORMAL);
            }
            toFront((Frame) c);
        }
    }


    /**
     * Utility to make a JButton, adding the given listener as an ActionListener
     *
     * @param label The button label
     * @param listener The ActionListener
     * @return The newly created button
     */
    public static JButton makeJButton(String label, ActionListener listener) {
        JButton b = new JButton(label);
        b.addActionListener(listener);
        return b;
    }


    /**
     *  Utility method for creating and setting various properties of a JButton
     *  args array holds a set of key value pairs:
     *  -tooltip &lt;The tooltip text&gt;
     *  -bg      &lt;Background color&gt;
     *  -listener &lt;Action listener&gt;
     *  -command &lt;Action command&gt;
     *
     * @param label The button label
     * @param args The argname/value array
     * @return The newly created button
     */
    public static JButton makeJButton(String label, Object[] args) {
        JButton b = new JButton(label);
        if ((args.length / 2 * 2) != args.length) {
            throw new IllegalArgumentException(
                "makeJButton: must have an even number of arguments");
        }
        for (int i = 0; i < args.length; i += 2) {
            String attr = (String) args[i];
            if (attr.equals("-tooltip")) {
                b.setToolTipText((String) args[i + 1]);
            } else if (attr.equals("-bg")) {
                b.setBackground((Color) args[i + 1]);
            } else if (attr.equals("-font")) {
                b.setFont((Font) args[i + 1]);
            } else if (attr.equals("-listener")) {
                b.addActionListener((ActionListener) args[i + 1]);
            } else if (attr.equals("-command")) {
                b.setActionCommand((String) args[i + 1]);
            } else {
                throw new IllegalArgumentException(
                    "makeJButton: unknown argument: " + attr);
            }
        }
        return b;
    }


    /**
     * Return the slider value as a percentage between its min and max
     *
     * @param s The slider
     * @return The percent value
     */
    public static double getSliderPercent(JSlider s) {
        BoundedRangeModel r = s.getModel();
        return ((double) (s.getValue() - r.getMinimum())
                / (r.getMaximum() - r.getMinimum()));
    }

    /**
     * Set the slider value as a percentage between its min and max
     *
     * @param s The slider
     * @param percent The percent value
     */
    public static void setSliderPercent(JSlider s, double percent) {
        BoundedRangeModel r = s.getModel();
        s.setValue(r.getMinimum()
                   + (int) (percent * (r.getMaximum() - r.getMinimum())));
    }

    /**
     * Recurse the Component hierarchy, setting the background color
     * of each component.
     *
     * @param c The component
     * @param bgColor The color
     */
    public static void setBackgroundOnTree(Container c, Color bgColor) {
        c.setBackground(bgColor);
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponent(i);
            if (child instanceof Container) {
                setBackgroundOnTree((Container) child, bgColor);
            } else {
                child.setBackground(bgColor);
            }
        }
    }

    /**
     * Recurse the Component hierarchy, setting the tooltip
     * of each component.
     *
     * @param c The component
     * @param tooltip The tooltip
     */
    public static void setToolTipOnTree(Container c, String tooltip) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponent(i);
            if (child instanceof Container) {
                setToolTipOnTree((Container) child, tooltip);
            }
            if (child instanceof JComponent) {
                ((JComponent) child).setToolTipText(tooltip);
            }
        }
    }




    /**
     * Recurse the Component hierarchy, setting the font.
     *
     * @param c The component
     * @param f The font
     */
    public static void setFontOnTree(JComponent c, Font f) {
        c.setFont(f);
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponent(i);
            if (child instanceof JComponent) {
                setFontOnTree((JComponent) child, f);
            }
        }
    }




    /**
     * Recurse the Component hierarchy, setting the foreground color
     * of each component.
     *
     * @param comp The component
     * @param fgColor The color
     */
    public static void setForegroundOnTree(Component comp, Color fgColor) {
        setForegroundOnTree(comp, fgColor, null);
    }

    /**
     * Recurse the Component hierarchy, setting the foreground color
     * of each component.
     *
     * @param comp The component
     * @param fgColor The color
     * @param ifEquals If non null then only set the foreground color
     * on the component its current color  equals the given fgColor
     */
    public static void setForegroundOnTree(Component comp, Color fgColor,
                                           Color ifEquals) {
        synchronized (comp.getTreeLock()) {
            if ((ifEquals == null) || ifEquals.equals(comp.getForeground())) {
                comp.setForeground(fgColor);
            }
            if (comp instanceof Container) {
                Container c = (Container) comp;
                for (int i = 0; i < c.getComponentCount(); i++) {
                    Component child = c.getComponent(i);
                    setForegroundOnTree(child, fgColor);
                }
            }
        }
    }


    /**
     *  Enable or disable a whole tree's worth of components
     *
     * @param comp The component
     * @param enable The enable flag
     */
    public static void enableTree(Component comp, boolean enable) {
        comp.setEnabled(enable);
        if (comp instanceof Container) {
            Container container = (Container) comp;
            for (int i = 0; i < container.getComponentCount(); i++) {
                enableTree(container.getComponent(i), enable);
            }
        }
    }


    /**
     * Enable or disable the list of Components
     *
     * @param comps List of components
     * @param enable Enable or disable
     */
    public static void enableComponents(List comps, boolean enable) {
        for (int i = 0; i < comps.size(); i++) {
            enableTree((Component) comps.get(i), enable);
        }
    }


    /**
     *  Make a scroll pane for the input box which may be used to hold
     * selection buttons later; size is given; text for a title in box.
     *
     * @param c The component to put in the scroll pane
     * @param xdim The x dimension
     * @param ydim The y dimension
     * @return The new scroll pane
     */
    public static JScrollPane makeScrollPane(Component c, int xdim,
                                             int ydim) {
        JScrollPane sp =
            new JScrollPane(
                c, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JViewport vp = sp.getViewport();
        vp.setViewSize(new Dimension(xdim, ydim));
        return sp;
    }


    /**
     * Set the size and preferred size of the component and return it
     *
     * @param comp component
     * @param w width
     * @param h height_
     *
     * @return The component
     */
    public static JComponent setSize(JComponent comp, int w, int h) {
        comp.setSize(w, h);
        comp.setPreferredSize(new Dimension(w, h));
        return comp;
    }

    /**
     * Create a button group and add the two buttons to it.
     *
     * @param b1 Button 1
     * @param b2 Button 2
     * @return The created button group.
     */
    public static ButtonGroup buttonGroup(JToggleButton b1,
                                          JToggleButton b2) {
        ButtonGroup bg = new ButtonGroup();
        bg.add(b1);
        bg.add(b2);
        return bg;
    }

    /**
     * Create a button group and add the three buttons to it.
     *
     * @param b1 Button 1
     * @param b2 Button 2
     * @param b3 Button 3
     * @return The created button group.
     */
    public static ButtonGroup buttonGroup(JToggleButton b1, JToggleButton b2,
                                          JToggleButton b3) {
        ButtonGroup bg = buttonGroup(b1, b2);
        bg.add(b3);
        return bg;
    }



    /**
     * This makes a component that contains a jlabel.  The jlabel is inset with some padding at the top and the outer component is aligned to the top. It is intended to be used when doing a form layout and the
     * component on the right it a tall one
     *
     * @param s The string to create the label with
     *
     * @return the component
     */
    public static JComponent valignLabel(String s) {
        return GuiUtils.top(inset(GuiUtils.rLabel(s),
                                  new Insets(7, 0, 0, 0)));
    }

    /**
     * Layout as a 2 column form
     *
     * @param objects  objects to lay out
     *
     * @return  the form
     */
    public static JComponent formLayout(List objects) {
        return formLayout(objects, INSETS_5);
    }


    /**
     * Do a 2 column layout of the objects with the given insets for spacing
     *
     * @param objects May be components or strings. If strings this method will create jlabels
     * @param insets spacing
     *
     * @return component
     */
    public static JComponent formLayout(List objects, Insets insets) {
        Component[] comps = new Component[objects.size()];
        for (int i = 0; i < objects.size(); i++) {
            Component comp   = null;
            Object    object = objects.get(i);
            if ( !(object instanceof Component)) {
                comp = rLabel(object.toString());
            } else {
                comp = (Component) object;
            }
            comps[i] = comp;
        }

        LayoutUtil.tmpInsets = insets;
        return doLayout(comps, 2, WT_NY, WT_N);


    }


    /**
     * This does a doLayout with 2 columns. If any of the objects are not a Component
     * then it creates a rLabel(object.toString)
     *
     * @param objects array of components to layout
     * @param insets The spacing
     * @return component
     */
    public static JComponent formLayout(Object[] objects, Insets insets) {
        return formLayout(Misc.toList(objects), insets);
    }




    /**
     * This does a doLayout with 2 columns. If any of the objects are not a Component
     * then it creates a rLabel(object.toString)
     *
     * @param objects array of components to layout
     * @return component
     */
    public static JComponent formLayout(Object[] objects) {
        return formLayout(Misc.toList(objects));
    }



    /**
     * Create a panel and do a a right align flow layout of the components
     *
     * @param comps The components to add
     * @return The new panel
     */
    public static JPanel flowRight(Component[] comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] != null) {
                p.add(comps[i]);
            }
        }
        return p;
    }



    /**
     * Create a panel and do  flow layout of the components
     *
     * @param comps The components to add
     * @return The new panel
     */
    public static JPanel flow(Component[] comps) {
        JPanel p = new JPanel();
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] != null) {
                p.add(comps[i]);
            }
        }
        return p;
    }





    /**
     * Make a set of JButtons, each with a label and action command from the given cmds
     * array. Add the buttons into a new JPanel  and return it.
     *
     * @param l Listener
     * @param cmds Button labels.action commands
     * @return JPanel that contains the buttons
     */
    public static JPanel makeButtons(ActionListener l, String[] cmds) {
        return makeButtons(l, null, cmds, null);
    }


    /**
     * Make a set of JButtons, each with a label and action command from the given cmds
     * array. Add the buttons into a new JPanel  and return it.
     *
     * @param l Listener
     * @param cmds Button labels.action commands
     * @param buttonMap If non-null will hold a mapping from (String) command to JButton
     * @return JPanel that contains the buttons
     */
    public static JPanel makeButtons(ActionListener l, String[] cmds,
                                     Hashtable buttonMap) {
        return makeButtons(l, null, cmds, buttonMap);
    }

    /**
     * Make a set of JButtons, each with a label and action command from the given cmds and labels
     * arrays. Add the buttons into a new JPanel  and return it.
     *
     * @param l Listener
     * @param labels  Button labels
     * @param cmds Button action commands
     * @return JPanel that contains the buttons
     */

    public static JPanel makeButtons(ActionListener l, String[] labels,
                                     String[] cmds) {
        return makeButtons(l, labels, cmds, (Hashtable) null);
    }

    /**
     * Make a set of JButtons, each with a label and action command from the given cmds and labels
     * arrays. Add the buttons into a new JPanel  and return it.
     *
     * @param l Listener
     * @param labels  Button labels
     * @param cmds Button action commands
     * @param buttonMap If non-null will hold a mapping from (String) command to JButton
     * @return JPanel that contains the buttons
     */

    public static JPanel makeButtons(ActionListener l, String[] labels,
                                     String[] cmds, Hashtable buttonMap) {

        return makeButtons(l, labels, cmds, null, buttonMap);
    }

    /**
     * Make a set of JButtons, each with a label and action command from the given cmds and labels
     * arrays. Add the buttons into a new JPanel  and return it.
     *
     * @param l Listener
     * @param labels  Button labels. If a label starts with icon: then we make an image button, the image path
     * is the rest of the label
     * @param cmds Button action commands
     * @param tooltips If non-null then set te tooltip on the button
     * @param buttonMap If non-null will hold a mapping from (String) command to JButton
     * @return JPanel that contains the buttons
     */
    public static JPanel makeButtons(ActionListener l, String[] labels,
                                     String[] cmds, String[] tooltips,
                                     Hashtable buttonMap) {


        JPanel p       = new JPanel();
        List   buttons = new ArrayList();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (int i = 0; i < cmds.length; i++) {
            String cmd   = ((cmds[i] != null)
                            ? cmds[i]
                            : ((labels != null)
                               ? labels[i]
                               : ""));
            String label = ((labels != null)
                            ? labels[i]
                            : cmd);
            if (label == null) {
                label = cmd;
            }
            JButton button;
            if (label.startsWith("icon:")) {
                button = getImageButton(label.substring(5), GuiUtils.class);
                button.addActionListener(l);
                button.setActionCommand(cmd);
            } else {
                button = makeJButton(label, new Object[] { "-listener", l,
                        "-command", cmd });
            }
            /*
              int keyCode = charToKeyCode(label);
              if(keyCode!=-1)
              button.setMnemonic(keyCode);*/
            if (cmd.equals(CMD_OK)) {
                button.setDefaultCapable(true);
            }
            if ((tooltips != null) && (i < tooltips.length)) {
                button.setToolTipText(tooltips[i]);
            }
            if (buttonMap != null) {
                buttonMap.put(cmd, button);
            }
            buttons.add(button);
        }
        return doLayout(p, getComponentArray(buttons), buttons.size(), WT_N,
                        WT_N, null, null, new Insets(5, 5, 5, 5));
    }


    /**
     * Utility to make apply/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return The button panel
     */
    public static JPanel makeApplyCancelButtons(ActionListener l) {
        return makeButtons(l, new String[] { "Apply", "Cancel" },
                           new String[] { CMD_APPLY,
                                          CMD_CANCEL });
    }


    /**
     * Utility to make apply button panel
     *
     * @param l The listener to add to the buttons
     * @return The button panel
     */
    public static JButton makeApplyButton(ActionListener l) {
        JButton b = new JButton("Apply");
        b.setActionCommand(CMD_APPLY);
        b.addActionListener(l);
        return b;
    }

    /**
     * Utility to make ok/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return The button panel
     */
    public static JPanel makeOkCancelButtons(ActionListener l) {
        return makeOkCancelButtons(l, "OK", "Cancel");
    }

    /**
     * Utility to make new/ok/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return The button panel
     */

    public static JPanel makeNewOkCancelButtons(ActionListener l) {
        return makeNewOkCancelButtons(l, CMD_NEW, CMD_OK, CMD_CANCEL);
    }

    /**
     * Utility to make new/ok/cancel/help button panel
     *
     * @param l The listener to add to the buttons
     * @return The button panel
     */

    public static JPanel makeNewOkCancelHelpButtons(ActionListener l) {
        return makeButtons(l, new String[] { CMD_NEW, CMD_OK, CMD_CANCEL,
                                             CMD_HELP });
    }


    /**
     * Utility to make new/ok/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @param newLabel Label to use for the new button
     * @param okLabel Label to use for the ok button
     * @param cancelLabel Label to use for the cancel button
     * @return Button panel
     */
    public static JPanel makeNewOkCancelButtons(ActionListener l,
            String newLabel, String okLabel, String cancelLabel) {
        return makeButtons(l, new String[] { newLabel, okLabel,
                                             cancelLabel }, new String[] {
                                                 CMD_NEW,
                CMD_OK, CMD_CANCEL });
    }


    /**
     * Utility to make ok/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @param okLabel Label to use for the ok button
     * @param cancelLabel Label to use for the cancel button
     * @return Button panel
     */
    public static JPanel makeOkCancelButtons(ActionListener l,
                                             String okLabel,
                                             String cancelLabel) {
        return makeButtons(l, new String[] { okLabel, cancelLabel },
                           new String[] { CMD_OK,
                                          CMD_CANCEL });
    }


    /**
     * Utility to make apply/ok/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return Button panel
     */
    public static JPanel makeApplyOkCancelButtons(ActionListener l) {
        return makeButtons(l, new String[] { "Apply", "OK", "Cancel" },
                           new String[] { CMD_APPLY,
                                          CMD_OK, CMD_CANCEL });
    }

    /**
     * Utility to make ok/help/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return Button panel
     */

    public static JPanel makeOkHelpCancelButtons(ActionListener l) {
        return makeButtons(l, new String[] { "OK", "Help", "Cancel" },
                           new String[] { CMD_OK,
                                          CMD_HELP, CMD_CANCEL });
    }

    /**
     * Utility to make apply/ok/help/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return Button panel
     */
    public static JPanel makeApplyOkHelpCancelButtons(ActionListener l) {
        return makeButtons(l, new String[] { "Apply", "OK", "Help",
                                             "Cancel" }, new String[] {
                                             CMD_APPLY,
                                             CMD_OK, CMD_HELP, CMD_CANCEL });
    }


    /**
     * Utility to make apply/ok/reset/cancel button panel
     *
     * @param l The listener to add to the buttons
     * @return Button panel
     */
    public static JPanel makeApplyOkResetCancelButtons(ActionListener l) {
        return makeButtons(l, new String[] { "Apply", "OK", "Reset",
                                             "Cancel" }, new String[] {
                                             CMD_APPLY,
                                             CMD_OK, CMD_RESET, CMD_CANCEL });
    }



    /**
     * Show a modeful dialog, attached to the given frame, with the given message.
     * Ask the user Yes or No.
     *
     * @param frame Frame to attach to.
     * @param message Message to show
     * @param title Window title
     * @return True if user selects Yes, false if No
     */
    public static boolean showYesNoDialog(Window frame, String message,
                                          String title) {
        return showYesNoDialog(frame, message, title, "Yes", "No");
    }

    /**
     * Show a modeful dialog, attached to the given frame, with the given message.
     * Ask the user Yes or No.
     *
     * @param frame Frame to attach to.
     * @param message Message to show
     * @param title Window title
     * @param yes The Yes text
     * @param no The No text
     * @return True if user selects Yes, false if No
     */
    public static boolean showYesNoDialog(Window frame, String message,
                                          String title, String yes,
                                          String no) {
        Object[]  options   = { yes, no };
        Component component = frame;
        if (component == null) {
            component = LogUtil.getCurrentWindow();
        }
        boolean result = (JOptionPane.YES_OPTION
                          == JOptionPane.showOptionDialog(component, message,
                              title, JOptionPane.YES_NO_OPTION,
                              JOptionPane.QUESTION_MESSAGE, null, options,
                              yes));

        return result;
    }


    /**
     * Insert text into the component
     *
     * @param comp   component
     * @param s      text to insert
     */
    public static void insertText(JTextComponent comp, String s) {
        int    pos = comp.getCaretPosition();
        String t   = comp.getText();
        t = t.substring(0, pos) + s + t.substring(pos);
        comp.setText(t);
        comp.setCaretPosition(pos + s.length());
    }


    /**
     * Show a modeful dialog, attached to the given frame, with the given message.
     * Ask the user Yes or No or Cancel
     *
     * @param frame Frame to attach to.
     * @param message Message to show
     * @param title Window title
     * @return 0 if Yes, 1 if No, 2 if Cancel
     */
    public static int showYesNoCancelDialog(Window frame, String message,
                                            String title) {
        return showYesNoCancelDialog(frame, message, title, CMD_YES);
    }

    /**
     * Show a modeful dialog, attached to the given frame, with the given message.
     * Ask the user Yes or No or Cancel
     *
     * @param frame Frame to attach to.
     * @param message Message to show
     * @param title Window title
     * @param defaultCmd   default for the dialog (CMD_YES, CMD_NO, CMD_CANCEL)
     * @return 0 if Yes, 1 if No, 2 if Cancel
     */
    public static int showYesNoCancelDialog(Window frame, String message,
                                            String title, String defaultCmd) {
        Object[]  options   = { CMD_YES, CMD_NO, CMD_CANCEL };
        Component component = frame;
        if (component == null) {
            component = LogUtil.getCurrentWindow();
        }
        int result = JOptionPane.showOptionDialog(component, message, title,
                         JOptionPane.YES_NO_CANCEL_OPTION,
                         JOptionPane.QUESTION_MESSAGE, null, options,
                         defaultCmd);

        return result;
    }


    /**
     * Show a modeful dialog, attached to the given frame, with the given message.
     * Ask the user Yes or No.
     *
     * @param frame Frame to attach to.
     * @param contents GUI contents
     * @param title Window title
     * @param src Where to show window
     * @return True if user selects Yes, false if No
     */
    public static boolean showYesNoDialog(Window frame, String title,
                                          Component contents, Component src) {
        JDialog              dialog   = createDialog(frame, title, true);
        final ObjectListener listener = getCloseDialogListener(dialog);
        JPanel buttons = makeButtons(listener, new String[] { CMD_YES,
                CMD_NO });
        packDialog(dialog, centerBottom(contents, buttons));
        dialog.setLocation(getLocation(src));
        dialog.setVisible(true);
        return listener.theObject.equals(GuiUtils.CMD_YES);
    }




    /**
     * Show a modeful  Ok/Cancel dialog.
     *
     * @param f The frame to attach to
     * @param title The window title
     * @param contents The gui contents to show
     * @param src Where should the window popup
     * @return True if Ok was pressed, false otherwise
     */
    public static boolean showOkCancelDialog(Window f, String title,
                                             Component contents,
                                             Component src) {
        return showOkCancelDialog(f, title, contents, src, null);
    }

    /**
     * Show a modeful  Ok/Cancel dialog.
     *
     * @param f The frame to attach to
     * @param title The window title
     * @param contents The gui contents to show
     * @param src Where should the window popup
     * @param actionComponents If non-null then these are components
     * in the contents (e.g., JTextField) that an action listener is added to to do the Ok
     * on an action event
     * @return True if Ok was pressed, false otherwise
     */

    public static boolean showOkCancelDialog(Window f, String title,
                                             Component contents,
                                             Component src,
                                             List actionComponents) {
        return showOkCancelDialog(f, title, contents, src, actionComponents,
                                  CMD_OK);
    }

    /**
     * Find the JButton at or under the given Container whose action command is CMD_OK
     * and set it as the default button for the given root pane.
     *
     * @param c The component
     * @param root The root pane
     */
    private static void setDefaultButton(Container c, JRootPane root) {
        if ((c instanceof JButton)
                && Misc.equals(((JButton) c).getActionCommand(), CMD_OK)) {
            root.setDefaultButton((JButton) c);
            return;
        }
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponent(i);
            if (child instanceof Container) {
                setDefaultButton((Container) child, root);
            }
        }


    }


    /**
     * A utility to create a frame. This also registers the frame with the LogUtil
     * facility that tracks the recently active windows.
     *
     * @param title Frame title
     *
     * @return The frame
     */
    public static JFrame createFrame(String title) {
        JFrame frame = new JFrame(title);
        LogUtil.registerWindow(frame);
        return frame;
    }


    /**
     * A utility to create a dialog. If modal then this method uses the
     * LogUtil.getCurrentWindow as the parent window of the dialog.
     *
     * @param title Dialog title
     * @param modal Is modal
     *
     * @return THe dialog
     */
    public static JDialog createDialog(String title, boolean modal) {
        return createDialog(null, title, modal);
    }



    /**
     * Popup an html widget at the given x/y that shows the text (or, if the text
     * is a URI will read the URI).
     *
     * @param text text or uri
     * @param x x
     * @param y y
     * @param modal modal
     *
     * @return 2 element array holding the html component and the dialog
     */
    public static Component[] popup(String text, int x, int y,
                                    boolean modal) {
        try {
            String contents = IOUtil.readContents(text, (String) null);
            if (contents != null) {
                text = contents;
            }
        } catch (Exception exc) {}
        Component[]   comps    = getHtmlComponent(text, null, 200, 200);
        Component     scroller = comps[1];
        final JDialog dialog   = GuiUtils.createDialog(null, "Popup", modal);
        JButton       btn      = new JButton("Close");
        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                dialog.dispose();
            }
        });
        dialog.getContentPane().add(centerBottom(scroller,
                GuiUtils.wrap(btn)));
        dialog.pack();
        dialog.setLocation(new Point(x, y));
        dialog.setVisible(true);
        return new Component[] { comps[0], dialog };
    }



    /**
     * Utility to create a JDialog with a parent component. If not modal
     * this method also registers the JDialog with the LogUtil last active window
     * facility.
     *
     * @param parent The parent window. May be null, a Dialog or a Frame
     * @param title Dialog title
     * @param modal Is modal
     * @return The dialog
     */
    public static JDialog createDialog(Window parent, String title,
                                       boolean modal) {

        JDialog dialog;
        if (modal && (parent == null)) {
            parent = LogUtil.getCurrentWindow();
        }
        //      System.err.println ("createDialog:" + title + " " + (parent!=null));
        if ((parent == null) || (parent instanceof Frame)) {
            dialog = new JDialog((Frame) parent, title, modal);
        } else if (parent instanceof Dialog) {
            dialog = new JDialog((Dialog) parent, title, modal);
        } else {
            dialog = new JDialog((Dialog) null, title, modal);
        }
        if ( !modal) {
            LogUtil.registerWindow(dialog);
        }
        return dialog;
    }


    /**
     * Show a modeful  Ok/Cancel dialog.
     *
     * @param f                The frame to attach to
     * @param title            The window title
     * @param contents         The gui contents to show
     * @param src              Where should the window popup
     * @param actionComponents If non-null then these are components
     *                         in the contents (e.g., JTextField) that an
     *                         action listener is added to to do the Ok
     *                         on an action event
     * @param okLabel          text for the OK button
     * @return True if Ok was pressed, false otherwise
     */
    public static boolean showOkCancelDialog(Window f, String title,
                                             Component contents,
                                             Component src,
                                             List actionComponents,
                                             String okLabel) {
        if ( !LogUtil.getInteractiveMode()) {
            throw new IllegalStateException(
                "Cannot show dialog in non-interactive mode");
        }
        JDialog              dialog   = createDialog(f, title, true);
        final ObjectListener listener = getCloseDialogListener(dialog);
        JPanel buttons = makeOkCancelButtons(listener, okLabel, CMD_CANCEL);
        if (actionComponents != null) {
            ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    listener.actionPerformed(new ActionEvent(this, 0,
                            GuiUtils.CMD_OK));
                }
            };
            for (int i = 0; i < actionComponents.size(); i++) {
                Object o = actionComponents.get(i);
                if (o instanceof JTextField) {
                    ((JTextField) o).addActionListener(actionListener);
                } else if (o instanceof JComboBox) {
                    ((JComboBox) o).getEditor().addActionListener(
                        actionListener);
                }

            }
        }
        packDialog(dialog, centerBottom(contents, buttons));
        setDefaultButton(buttons, dialog.getRootPane());
        if (src != null) {
            dialog.setLocation(getLocation(src));
            dialog.setVisible(true);
        } else {
            showInCenter(dialog);
        }

        return listener.theObject.equals(GuiUtils.CMD_OK);
    }



    /**
     * Show OK dialog
     *
     * @param f     relative window
     * @param title title for the dialog
     * @param contents  dialog contents
     * @param src       src component
     */
    public static void showOkDialog(Window f, String title,
                                    Component contents, Component src) {
        if ( !LogUtil.getInteractiveMode()) {
            throw new IllegalStateException(
                "Cannot show dialog in non-interactive mode");
        }
        JDialog              dialog   = createDialog(f, title, true);
        final ObjectListener listener = getCloseDialogListener(dialog);
        JPanel buttons = makeButtons(listener, new String[] { CMD_OK },
                                     new String[] { CMD_OK });
        packDialog(dialog, centerBottom(contents, buttons));
        setDefaultButton(buttons, dialog.getRootPane());
        if (src != null) {
            dialog.setLocation(getLocation(src));
            dialog.setVisible(true);
        } else {
            showInCenter(dialog);
        }
    }





    /**
     * Make and show a modeful dialog with the given collection of buttons.
     * Return the index of the button that was pushed
     *
     * @param f Frame to attach to
     * @param title Window title
     * @param contents GUI contents
     * @param src Where to show window
     * @param buttonLabels Buttons
     * @return Which button was pushed
     */
    public static int makeDialog(Window f, String title, Component contents,
                                 Component src, String[] buttonLabels) {
        JDialog              dialog   = createDialog(f, title, true);
        final ObjectListener listener = getCloseDialogListener(dialog);
        JPanel buttons = makeButtons(listener, buttonLabels, buttonLabels);
        packDialog(dialog, centerBottom(contents, inset(buttons,5)));
        if (src != null) {
            dialog.setLocation(getLocation(src));
        } else {
            dialog.setLocation(new Point(200, 100));
        }
        setDefaultButton(buttons, dialog.getRootPane());
        dialog.setVisible(true);
        for (int i = 0; i < buttonLabels.length; i++) {
            if (listener.theObject.equals(buttonLabels[i])) {
                return i;
            }
        }
        return -1;
    }



    /**
     * Add a listener to the JList that pops up a menu on a right
     * click that allos for the selection of different strides.
     *
     * @param list list to popup on
     */
    public static void configureStepSelection(final JList list) {
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupConfigureStepSelection(e, list);
                }
            }
        });
        if (list.getToolTipText() == null) {
            list.setToolTipText(
                "Right mouse to show range select popup menu");
        }

    }

    /**
     * popup a menu to select strides
     *
     * @param e mouse click
     * @param list JList
     */
    private static void popupConfigureStepSelection(MouseEvent e,
            final JList list) {
        if ( !list.isEnabled()) {
            return;
        }
        List items = new ArrayList();
        getConfigureStepSelectionItems(list, items);
        JPopupMenu popup = makePopupMenu(items);
        popup.show(list, e.getX(), e.getY());
    }


    /** widget for interval dailog */
    private static JTextField intervalStepFld = new JTextField("1", 4);

    /** widget for interval dailog */
    private static JTextField intervalStartFld = new JTextField("1", 4);

    /**
     * Show the list interval selection dialog
     *
     * @param list list
     */
    public static void showIntervalSelectionDialog(JList list) {
        tmpInsets = INSETS_5;
        JComponent contents = doLayout(new Component[] {
                                  rLabel("Start Index:"),
                                  intervalStartFld, rLabel("Interval:"),
                                  intervalStepFld }, 2, WT_NN, WT_N);
        contents = vbox(new JLabel("Choose Selection Interval"), contents);
        contents = inset(contents, 5);
        final int size = list.getModel().getSize();

        while (true) {
            if ( !showOkCancelDialog(null, "Selection Interval", contents,
                                     list)) {
                return;
            }
            try {
                int start =
                    new Integer(intervalStartFld.getText().trim()).intValue()
                    - 1;
                int step =
                    new Integer(intervalStepFld.getText().trim()).intValue();
                list.clearSelection();
                for (int idx = start; idx < size; idx += step) {
                    list.addSelectionInterval(idx, idx);
                }
                return;
            } catch (NumberFormatException nfe) {
                LogUtil.userErrorMessage("Bad input value");
            }
        }
    }




    /**
     * popup a menu to select strides
     *
     * @param list JList
     * @param items menu items
     */
    public static void getConfigureStepSelectionItems(final JList list,
            List items) {
        if ( !list.isEnabled()) {
            return;
        }
        final int size   = list.getModel().getSize();
        int[]     steps  = {
            1, 2, 3, 4, 5, 10, 20
        };
        String[]  labels = {
            "all", "every other one", "every third one", "every fourth one",
            "every fifth one", "every tenth one", "every twentieth one"
        };
        JMenu rangeMenu = new JMenu("Select Range");
        items.add(rangeMenu);
        for (int i = 0; (i < 20) && (i < list.getModel().getSize()); i++) {
            JMenuItem item = new JMenuItem("First  " + (i + 1));
            rangeMenu.add(item);
            final int cnt = i + 1;
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    list.clearSelection();
                    for (int idx = 0; idx < cnt; idx++) {
                        list.addSelectionInterval(idx, idx);
                    }
                }
            });
        }


        JMenuItem selectStrideMenuItem = new JMenuItem("Choose Interval");
        selectStrideMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                final JList theList = list;
                Misc.run(new Runnable() {
                    public void run() {
                        showIntervalSelectionDialog(theList);
                    }
                });
            }
        });
        items.add(MENU_SEPARATOR);
        items.add(selectStrideMenuItem);
        items.add(MENU_SEPARATOR);
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] > size) {
                break;
            }
            final int step = steps[i];
            JMenuItem item = new JMenuItem("Select " + labels[i]);
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    list.clearSelection();
                    for (int idx = 0; idx < size; idx += step) {
                        list.addSelectionInterval(idx, idx);
                    }
                }
            });
            items.add(item);
        }
    }



    /**
     * Do a 2 column layout. Right align the first column. Make the width of the
     * first column non-stretcy, second column stretchy.
     *
     * @param comps Components to layout
     * @param rightAlignFirstColumn If true then right align the first column
     *
     * @return Panel containing form
     */
    public static JComponent formLayout(List comps,
                                        boolean rightAlignFirstColumn) {
        return formLayout(comps, WT_NY, WT_N, rightAlignFirstColumn);
    }

    /**
     * Do a 2 column layout. Right align the first column. Make the width of the
     * first column non-stretcy, second column stretchy.
     *
     * @param comps Components to layout
     *
     * @return Panel containing form
     */
    public static JComponent formLayout(Component[] comps) {
        return formLayout(Misc.toList(comps));
    }

    /**
     * Do a 2 column layout. Right align the first column. Use the given stretchy flags.
     *
     * @param comps Components to layout
     * @param widths WT_
     * @param heights WT_
     *
     * @return Panel containing form
     */
    public static JComponent formLayout(Component[] comps, double[] widths,
                                        double[] heights) {
        return formLayout(Misc.toList(comps), widths, heights, true);
    }

    /**
     * Do a 2 column layout. Right align the first column. Use the given stretchy flags.
     *
     * @param comps Components to layout
     * @param widths WT_
     * @param heights WT_
     *
     * @return Panel containing form
     */
    public static JComponent formLayout(List comps, double[] widths,
                                        double[] heights) {
        return formLayout(comps, widths, heights, true);
    }

    /**
     * Do a 2 column layout. Use the given stretchy flags.
     *
     * @param comps Components to layout
     * @param rightAlignFirstColumn If true then right align the first column.
     * @param widths WT_
     * @param heights WT_
     *
     * @return Panel containing form
     */
    public static JComponent formLayout(List comps, double[] widths,
                                        double[] heights,
                                        boolean rightAlignFirstColumn) {

        if (rightAlignFirstColumn) {
            for (int i = 0; i < comps.size(); i += 2) {
                JComponent comp = (JComponent) comps.get(i);
                comp = GuiUtils.right(comp);
                comps.set(i, comp);
            }
        }
        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        return GuiUtils.doLayout(comps, 2, widths, heights);
    }


    /**
     * Create a JPanel that holds a JLabel/the given widget
     *
     * @param label The label text to use
     * @param widget The widget to label
     * @return The panel
     */
    public static JPanel label(String label, Component widget) {
        return doLayout(new Component[] { new JLabel(label), widget }, 2,
                        WT_NY, WT_N);
    }

    /**
     * Create a JPanel that holds a the given widget/JLabel
     *
     * @param widget The widget to label
     * @param label The label text to use
     * @return The panel
     */
    public static JPanel label(Component widget, String label) {
        return doLayout(new Component[] { widget, new JLabel(label) }, 2,
                        WT_N, WT_N);
    }


    /**
     * Add the given  contents into the given frame
     *
     * @param f The frame
     * @param contents The contents
     * @return The given frame
     */
    public static JFrame packWindow(JFrame f, Component contents) {
        return packWindow(f, contents, false);
    }


    /**
     * Add the given  contents into the given frame
     *
     * @param f The frame
     * @param contents The contents
     * @param andShow If true then show the window.
     * @return The given frame
     */
    public static JFrame packWindow(JFrame f, Component contents,
                                    boolean andShow) {
        if (f == null) {
            f = new JFrame();
        }
        Container cpane = f.getContentPane();
        cpane.add(contents);
        f.pack();
        Msg.translateTree(f);
        if (andShow) {
            f.setVisible(true);
        }
        return f;
    }


    /**
     * Add the given  contents into the given dialog
     *
     * @param f The dialog
     * @param contents The contents
     * @return The dialog
     */
    public static JDialog packDialog(JDialog f, Component contents) {
        Container cpane = f.getContentPane();
        cpane.add(contents);
        f.pack();
        Msg.translateTree(f);
        return f;
    }



    /**
     * Create a new JFrame, add the contents to it, set its location
     * and return it.
     *
     * @param title The window title
     * @param contents The contents
     * @param x x location
     * @param y y location
     * @return The new JFrame
     */
    public static JFrame makeWindow(String title, Component contents, int x,
                                    int y) {
        JFrame    f     = new JFrame(title);
        Container cpane = f.getContentPane();
        cpane.add(contents);
        f.pack();
        Msg.translateTree(f);
        f.setLocation(x, y);
        return f;
    }

    /**
     *  Create a JMenuBar and add the menus contained with the menus list
     *  If no menus then return null.
     *
     * @param menus List if JMenu-s
     * @return The JMenuBar
     */
    public static JMenuBar makeMenuBar(List menus) {
        if ((menus == null) || (menus.size() == 0)) {
            return null;
        }
        JMenuBar menuBar = new JMenuBar();
        for (int i = 0; i < menus.size(); i++) {
            menuBar.add((JMenu) menus.get(i));
        }
        return menuBar;
    }





    /**
     * Create a listener that will close the given dialog.
     *
     * @param dialog The dialog to close
     * @return The listener that closes the dialog on an action event
     */
    public static ObjectListener getCloseDialogListener(
            final JDialog dialog) {
        return new ObjectListener(dialog) {
            public void actionPerformed(ActionEvent ae) {
                theObject = ae.getActionCommand();
                dialog.setVisible(false);
            }
        };
    }



    /**
     * Create a JLabel that displays an image icon create from the
     * given icon path (may be a file, resource or url).
     *
     * @param icon The image file
     * @param origin Used to lookup java resources
     * @return New JLabel showing image
     */
    public static JLabel getImageLabel(String icon, Class origin) {
        return new JLabel(getImageIcon(icon, origin));
    }

    /**
     * Create a JLabel that displays an image icon create from the
     * given icon path (may be a file, resource or url).
     *
     * @param icon The image file
     * @return New JLabel showing image
     */
    public static JLabel getImageLabel(String icon) {
        return new JLabel(getImageIcon(icon, GuiUtils.class));
    }



    /**
     * Create a JButton  that displays an image icon create from the
     * given icon path (may be a file, resource or url).
     *
     * @param icon The image file
     * @param origin Used to lookup java resources
     * @return New JButton showing image
     */
    public static JButton getImageButton(String icon, Class origin) {
        return getImageButton(icon, origin, 0, 0);
    }

    /**
     * Create a JButton  that displays an image icon create from the
     * given icon path (may be a file, resource or url).
     *
     * @param icon The image file
     * @param origin Used to lookup java resources
     * @param hInset Horizontal inset
     * @param vInset Vertical inset
     * @return New JButton showing image
     */
    public static JButton getImageButton(String icon, Class origin,
                                         int hInset, int vInset) {
        return getImageButton(getImageIcon(icon, origin), hInset, vInset);
    }



    /**
     * Get a scaled image button
     *
     * @param icon   path to icon for the button
     * @param origin relative class
     * @param hInset horizontal inset
     * @param vInset vertical inset
     *
     * @return the button
     */
    public static JButton getScaledImageButton(String icon, Class origin,
            int hInset, int vInset) {
        return getImageButton(getScaledImageIcon(icon, origin, true), hInset,
                              vInset);
    }

    /**
     * Create a JButton  that displays the given  image icon
     *
     * @param icon The image icon
     * @return New JButton showing image
     */
    public static JButton getImageButton(ImageIcon icon) {
        return getImageButton(icon, 0);
    }

    /**
     * Create a JButton  that displays the given  image icon
     *
     * @param icon The image icon
     * @param offset The spacing around the image in the JButton
     * @return New JButton showing image
     */
    public static JButton getImageButton(ImageIcon icon, int offset) {
        return getImageButton(icon, offset, offset);
    }


    /**
     * Create a JButton  that displays the given  image icon
     *
     * @param icon The image icon
     * @param hinset The hor. spacing around the image in the JButton
     * @param vinset The vert. spacing around the image in the JButton
     * @return New JButton showing image
     */
    public static JButton getImageButton(ImageIcon icon, int hinset,
                                         int vinset) {
        JButton b = new JButton(icon);
        b.setBackground(null);
        b.setContentAreaFilled(false);
        b.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        b.setPreferredSize(new Dimension(icon.getIconWidth() + hinset,
                                         icon.getIconHeight() + vinset));
        return b;
    }


    /**
     * Create a JToggleButton with the given image and, if non-null,
     * the given  selected image.
     *
     * @param icon Image for button
     * @param selectedIcon Image to use when selected
     * @param hinset Hor. inset
     * @param vinset Vert. inset
     * @param addMouseOverBorder add a mouseover border
     * @return New button
     */
    public static JToggleButton getToggleImageButton(String icon,
            String selectedIcon, int hinset, int vinset,
            boolean addMouseOverBorder) {
        return getToggleImageButton(getImageIcon(icon),
                                    getImageIcon(selectedIcon), hinset,
                                    vinset, addMouseOverBorder);
    }


    /**
     * Create a JToggleButton with the given image and, if non-null,
     * the given  selected image.
     *
     * @param icon Image for button
     * @param selectedIcon Image to use when selected
     * @param hinset Hor. inset
     * @param vinset Vert. inset
     * @return New button
     */
    public static JToggleButton getToggleImageButton(ImageIcon icon,
            ImageIcon selectedIcon, int hinset, int vinset) {
        return getToggleImageButton(icon, selectedIcon, hinset, vinset,
                                    false);
    }



    /**
     * Create a JToggleButton with the given image and, if non-null,
     * the given  selected image.
     *
     * @param icon Image for button
     * @param selectedIcon Image to use when selected
     * @param hinset Hor. inset
     * @param vinset Vert. inset
     * @param addMouseOverBorder add a mouseover border
     * @return New button
     */
    public static JToggleButton getToggleImageButton(ImageIcon icon,
            ImageIcon selectedIcon, int hinset, int vinset,
            boolean addMouseOverBorder) {
        final JToggleButton b = new JToggleButton(icon);
        if (icon != selectedIcon) {
            b.setSelectedIcon(selectedIcon);
        }
        if (addMouseOverBorder) {
            makeMouseOverBorder(b);
        } else {
            b.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        }
        b.setPreferredSize(new Dimension(icon.getIconWidth() + hinset,
                                         icon.getIconHeight() + vinset));

        return b;
    }


    /**
     * Make a mouse over border
     *
     * @param b   the component
     */
    public static void makeMouseOverBorder(final JComponent b) {
        b.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBorder(BorderFactory.createLineBorder(Color.gray));
                }
            }
            public void mouseExited(MouseEvent e) {
                b.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            }
        });

    }

    /**
     * Create a toggle button with the given image.
     *
     * @param iconPath path to image
     * @param hinset hor. inset
     * @param vinset vert inset
     *
     * @return button
     */
    public static JToggleButton getToggleButton(String iconPath, int hinset,
            int vinset) {
        return getToggleButton(getImageIcon(iconPath, GuiUtils.class),
                               hinset, vinset);
    }


    /**
     * Create a toggle button with the given image.
     *
     *
     * @param icon The icon to use
     * @param hinset hor.inset
     * @param vinset vert. inset
     * @return The new button
     */
    public static JToggleButton getToggleButton(ImageIcon icon, int hinset,
            int vinset) {
        JToggleButton b = new JToggleButton(icon);
        //Don't do this now because in Windows L&F the selected button doesn't show up well
        //      b.setContentAreaFilled(false);
        //b.setBorder (BorderFactory.createEmptyBorder (0,2,0,2));
        b.setPreferredSize(new Dimension(icon.getIconWidth() + hinset,
                                         icon.getIconHeight() + vinset));
        return b;
    }


    /**
     * Find the location of the given componet on the screen. If any
     * errors then return the screen center - 100
     *
     * @param src Source component
     * @return Location on screen
     */
    public static Point getLocation(Component src) {
        if (src != null) {
            try {
                return src.getLocationOnScreen();
            } catch (Exception exc) {}
        }
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        return new Point(ss.width / 2 - 100, ss.height / 2 - 100);
    }



    /**
     * pack and show the dialog in the center of the screen
     *
     * @param dialog The dialog
     */
    public static void showInCenter(JDialog dialog) {
        packInCenter(dialog);
        dialog.setVisible(true);
    }

    /**
     * pack  the dialog in the center of the screen
     *
     * @param dialog The dialog
     */
    public static void packInCenter(JDialog dialog) {
        dialog.pack();
        Point center = getLocation(null);
        int   x      = center.x - dialog.getWidth() / 2;
        int   y      = center.y - dialog.getHeight() / 2;
        if (x < 20) {
            x = 20;
        }
        if (y < 20) {
            y = 20;
        }
        dialog.setLocation(x, y);

    }




    /**
     * Create a JSplitPane in vertical mode.
     *
     * @param top Top component
     * @param bottom Bottom  component
     * @param topSpace How much space to give the top
     * @param resizeWeight How much weight to give the top when resizing
     * @return The split pane
     */
    public static JSplitPane vsplit(Component top, Component bottom,
                                    int topSpace, double resizeWeight) {
        JSplitPane split = vsplit(top, bottom, topSpace);
        split.setResizeWeight(resizeWeight);
        return split;
    }


    /**
     * Create a JSplitPane in vertical mode.
     *
     * @param top Top component
     * @param bottom Bottom  component
     * @param topSpace How much space to give the top
     * @return The split pane
     */
    public static JSplitPane vsplit(Component top, Component bottom,
                                    int topSpace) {
        JSplitPane split = vsplit(top, bottom);
        split.setDividerLocation(topSpace);
        return split;
    }

    /**
     * Create a JSplitPane in vertical mode.
     *
     * @param top Top component
     * @param bottom Bottom  component
     * @param resizeWeight How much weight to give the top when resizing
     * @return The split pane
     */

    public static JSplitPane vsplit(Component top, Component bottom,
                                    double resizeWeight) {
        JSplitPane split = vsplit(top, bottom);
        split.setResizeWeight(resizeWeight);
        return split;
    }

    /**
     * Create a JSplitPane in vertical mode.
     *
     * @param top Top component
     * @param bottom Bottom  component
     * @return The split pane
     */
    public static JSplitPane vsplit(Component top, Component bottom) {
        return new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
    }


    /**
     *  Create a horizontally aligned JSplitPane. Give the left component the specified
     *  space.
     *
     * @param left The left component
     * @param right The right component
     * @param leftSpace Where to put the divider
     * @param resizeWeight The resize weight
     * @return The split pane
     *
     */
    public static JSplitPane hsplit(Component left, Component right,
                                    int leftSpace, double resizeWeight) {
        JSplitPane split = hsplit(left, right, leftSpace);
        split.setResizeWeight(resizeWeight);
        return split;
    }


    /**
     *  Create a  horizontally aligned JSplitPane
     *
     * @param left The left component
     * @param right The right component
     * @param leftSpace Where to put the divider
     * @return The split pane
     */
    public static JSplitPane hsplit(Component left, Component right,
                                    int leftSpace) {
        JSplitPane split = hsplit(left, right);
        if (leftSpace >= 0) {
            split.setDividerLocation(leftSpace);
        }
        return split;
    }

    /**
     *  Create a  horizontally aligned JSplitPane
     *
     * @param left The left component
     * @param right The right component
     * @param resizeWeight How much resize weight  to use
     * @return The split pane
     */
    public static JSplitPane hsplit(Component left, Component right,
                                    double resizeWeight) {
        JSplitPane split = hsplit(left, right);
        split.setResizeWeight(resizeWeight);
        return split;
    }

    /**
     *  Create a basic horizontally aligned JSplitPane
     *
     * @param left The left component
     * @param right The right component
     * @return The split pane
     */
    public static JSplitPane hsplit(Component left, Component right) {
        return new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
    }


    /**
     *  Create a right aligned JLabel
     *
     * @param s The String to use for the label
     * @return The label
     */

    public static JLabel rLabel(String s) {
        return new JLabel(s, SwingConstants.RIGHT);
    }



    /**
     *  Create a center aligned JLabel
     *
     * @param s The String to use for the label
     * @return The label
     */

    public static JLabel cLabel(String s) {
        return new JLabel(s, SwingConstants.CENTER);
    }

    /**
     *  Create a left aligned JLabel
     *
     * @param s The String to use for the label
     * @return The label
     */

    public static JLabel lLabel(String s) {
        return new JLabel(s);
    }


    /**
     * Create a a minimally sized spacer
     *
     * @param w The min width
     * @param h The min height
     * @return The spacer
     */
    public static JComponent hspace(int w, int h) {
        JLabel p = new JLabel(" ");
        p.setMinimumSize(new Dimension(w, h));
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }




    /**
     * Ask the user the question. Return their response or null.
     *
     * @param question The question.
     * @param label Extra label.
     * @param initValue Initial value of answer
     * @return The user's response
     */
    public static String getInput(String question, String label,
                                  String initValue) {

        return getInput(question, label, initValue, "");
    }




    /**
     * Ask the user the question. Return their response or null.
     *
     * @param question The question.
     * @param label Extra label.
     * @param initValue Initial value of answer
     * @param trailingLabel Label after the text field.
     * @return The user's response
     */
    public static String getInput(String question, String label,
                                  String initValue, String trailingLabel) {

        return getInput(question, label, initValue, trailingLabel, null);
    }

    /**
     * Ask the user the question. Return their response or null.
     *
     * @param question The question.
     * @param label Extra label.
     * @param initValue Initial value of answer
     * @param trailingLabel Label after the text field.
     * @param underLabel Label under the text field.
     * @return The user's response
     */
    public static String getInput(String question, String label,
                                  String initValue, String trailingLabel,
                                  Object underLabel) {
        return getInput(question, label, initValue, trailingLabel,
                        underLabel, "");
    }


    /**
     * Ask the user the question. Return their response or null.
     *
     * @param question The question.
     * @param label Extra label.
     * @param initValue Initial value of answer
     * @param trailingLabel Label after the text field.
     * @param underLabel Label under the text field.
     * @param title for the dialog box.
     * @return The user's response
     */
    public static String getInput(String question, String label,
                                  String initValue, String trailingLabel,
                                  Object underLabel, String title) {
        return getInput(question, label, initValue, trailingLabel,
                        underLabel, title, 20);
    }


    /**
     * Ask the user the question. Return their response or null.
     *
     * @param question The question.
     * @param label Extra label.
     * @param initValue Initial value of answer
     * @param trailingLabel Label after the text field.
     * @param underLabel Label under the text field.
     * @param title for the dialog box.
     * @param fieldWidth Field width
     * @return The user's response
     */

    public static String getInput(String question, String label,
                                  String initValue, String trailingLabel,
                                  Object underLabel, String title,
                                  int fieldWidth) {


        return getInput(question, label, initValue, trailingLabel,
                        underLabel, title, fieldWidth, null);
    }

    /**
     * Ask the user the question. Return their response or null.
     *
     * @param question The question.
     * @param label Extra label.
     * @param initValue Initial value of answer
     * @param trailingLabel Label after the text field.
     * @param underLabel Label under the text field.
     * @param title for the dialog box.
     * @param fieldWidth Field width
     * @param nearComponent If non-null then show the dialog near this component
     * @return The user's response
     */

    public static String getInput(String question, String label,
                                  String initValue, String trailingLabel,
                                  Object underLabel, String title,
                                  int fieldWidth, JComponent nearComponent) {



        final JDialog    dialog   = createDialog(title, true);
        final JTextField field    = new JTextField(((initValue == null)
                ? ""
                : initValue), fieldWidth);
        ObjectListener   listener = new ObjectListener(new Boolean(false)) {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if ((ae.getSource() == field) || cmd.equals(CMD_OK)) {
                    theObject = new Boolean(true);
                } else {
                    theObject = new Boolean(false);
                }
                dialog.setVisible(false);
            }
        };
        field.addActionListener(listener);
        List comps = new ArrayList();
        if (question != null) {
            comps.add(left(inset(new JLabel(question), 4)));
        }
        if (trailingLabel != null) {
            comps.add(left(GuiUtils.centerRight(label(label, field),
                    new JLabel(trailingLabel))));
        } else {
            comps.add(left(label(label, field)));
        }

        if (underLabel != null) {
            if (underLabel instanceof String) {
                comps.add(left(new JLabel(underLabel.toString())));
            } else if (underLabel instanceof Component) {
                comps.add(left((Component) underLabel));
            }
        }

        JComponent contents = inset(centerBottom(vbox(comps),
                                  makeOkCancelButtons(listener)), 4);

        packDialog(dialog, contents);
        Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
        if (nearComponent != null) {
            showDialogNearSrc(nearComponent, dialog);
        } else {
            Point ctr = new Point(ss.width / 2 - 100, ss.height / 2 - 100);
            dialog.setLocation(ctr);
            dialog.setVisible(true);
        }
        if ( !((Boolean) listener.theObject).booleanValue()) {
            return null;
        }
        return field.getText();
    }


    /**
     * Ask the user the question. Return the answer.
     *
     * @param question The  question
     * @return The answer
     */
    public static String getInput(String question) {
        return JOptionPane.showInputDialog(LogUtil.getCurrentWindow(),
                                           question);
    }


    /**
     * Ask the user the question. Return yes or no.
     *
     * @param title The window title.
     * @param question The question.
     * @return True if they say yes
     */
    public static boolean askYesNo(String title, Object question) {
        int response =
            JOptionPane.showConfirmDialog(LogUtil.getCurrentWindow(),
                                          question, title,
                                          JOptionPane.YES_NO_OPTION);
        return (response == 0);
    }

    /**
     * Ask the user the question. Return ok or cancel.
     *
     * @param title The window title.
     * @param question The question.
     * @return True if they say ok, false otherwise
     */

    public static boolean askOkCancel(String title, Object question) {
        int response =
            JOptionPane.showConfirmDialog(LogUtil.getCurrentWindow(),
                                          question, title,
                                          JOptionPane.OK_CANCEL_OPTION);
        return (response == 0);
    }


    /**
     * Show the given component in a dialog.
     *
     * @param title The title
     * @param comp The component to show
     */
    public static void showDialog(String title, Component comp) {
        showDialog(title, comp, null);
    }



    /** Keep a list of the modal dialogs that are currently being shown */
    private static List modalDialogComponents = new ArrayList();

    /**
     * Add the model dialog to the list
     *
     * @param comp The component in the model dialog
     */
    public static void addModalDialogComponent(Component comp) {
        modalDialogComponents.add(comp);
    }

    /**
     * Remove the model dialog from the list
     *
     * @param comp The component in the model dialog
     */
    public static void removeModalDialogComponent(Component comp) {
        modalDialogComponents.remove(comp);
    }

    /**
     * Show the given component in a dialog.
     *
     * @param title The title
     * @param comp The component to show
     * @param parentComponent The parent component of the dialog.
     */
    public static void showDialog(String title, Component comp,
                                  Component parentComponent) {
        if (parentComponent == null) {
            parentComponent = LogUtil.getCurrentWindow();
        }
        modalDialogComponents.add(comp);
        JOptionPane.showMessageDialog(parentComponent, comp, title,
                                      JOptionPane.INFORMATION_MESSAGE);
        modalDialogComponents.remove(comp);
    }

    /**
     * Move to the front any modal dialogs
     */
    public static void toFrontModalDialogs() {
        for (int i = 0; i < modalDialogComponents.size(); i++) {
            Component comp   = (Component) modalDialogComponents.get(i);
            Window    window = getWindow(comp);
            if (window != null) {
                toFront(window);
            }
        }
    }


    /**
     * Procedure to set the list of items in a ComboBox
     *
     * @param box Combobox to fill
     * @param items Items to add
     */
    public static void setListData(JComboBox box, Object[] items) {
        box.removeAllItems();
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                box.addItem(items[i]);
            }
        }
    }

    /**
     * Check if there are any selected items in the combobox. We have
     * this as a method because if the box as no items a getSelectedItem
     * causes an error.
     *
     * @param box The box to check
     *
     * @return Any selected items.
     */
    public static boolean anySelected(JComboBox box) {
        if (box == null) {
            return false;
        }
        if (box.getModel().getSize() > 0) {
            return box.getSelectedItem() != null;
        }
        return false;
    }



    /**
     * Procedure to set the list of items in a ComboBox
     *
     * @param box Combobox to fill
     * @param items Items to add
     */
    public static void setListData(JComboBox box, List items) {
        box.removeAllItems();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                box.addItem(items.get(i));
            }
        }
    }

    /**
     * Create an editable combobox with the given items. Preselect
     * the selected item if non-null.
     *
     * @param items Items in the box
     * @param selected Selected item
     * @return New JComboBox
     */
    public static JComboBox getEditableBox(List items, Object selected) {
        JComboBox fld = new JComboBox();
        fld.setEditable(true);
        if (selected != null) {
            if ( !items.contains(selected)) {
                items.add(selected);
            }
        }
        GuiUtils.setListData(fld, items);
        if (selected != null) {
            fld.setSelectedItem(selected);
        }
        return fld;
    }



    /**
     * Return the selected item in the given box as an integer.
     * Do a new Double(selected.toString()):
     *
     * @param box The box
     * @return The int value of the selected item.
     */
    public static int getBoxValue(JComboBox box) {
        Object o = box.getSelectedItem();
        return (int) Misc.parseValue(o.toString());
    }


    /**
     * Create a combobx boxc that holds a set of integer values.
     *
     * @param listener The action listener
     * @param command Action command for box
     * @param initValue Initial value
     * @param values List of Integers
     * @param editable Is box editable
     * @return The combobox
     */
    public static JComboBox createValueBox(ActionListener listener,
                                           String command, int initValue,
                                           List values, boolean editable) {
        JComboBox box = new JComboBox(new Vector(values));
        if (editable) {
            box.setEditable(true);
        }
        box.setActionCommand(command);
        box.setSelectedItem(new Integer(initValue));
        //Add the listener at the end so we don't get the event 
        //fired from the prior setSelectedItem
        box.addActionListener(listener);
        return box;
    }





    /**
     * Create the JMenuItem defined by the given xml
     *
     * @param node Xml menu item node
     * @param listener Action listener to add to the menu item
     * @param menuItems Mapping from id (from xml) to JMenuItem
     * @return The menu item
     *
     * @throws Exception
     */
    public static JMenuItem processXmlMenuItem(Node node,
            ActionListener listener, Hashtable menuItems)
            throws Exception {

        return processXmlMenuItem(node, listener, menuItems, null);
    }


    /**
     * Create the JMenuItem defined by the given xml
     *
     * @param node Xml menu item node
     * @param listener Action listener to add to the menu item
     * @param menuItems Mapping from id (from xml) to JMenuItem
     * @param actionIcons mapping of string action to imageicon to use in the menu item
     * @return The menu item
     *
     * @throws Exception
     */
    public static JMenuItem processXmlMenuItem(Node node,
            ActionListener listener, Hashtable menuItems,
            Hashtable<String, ImageIcon> actionIcons)
            throws Exception {

        NamedNodeMap attrs     = node.getAttributes();
        String       label     = Msg.msg(getAttribute(attrs, "label"));
        String       action = getAttribute(attrs, ATTR_ACTION, (String) null);
        ImageIcon    imageIcon = (((action != null) && (actionIcons != null))
                                  ? actionIcons.get(action)
                                  : null);
        //        if(label.indexOf("Color")>=0) {
        //            Misc.printStack("",20,null);
        //            System.err.println (label +" actions :" + (actionIcons!=null) + " action:" + action +" icon:" + imageIcon);
        //        }


        if ( !getIconsInMenus()) {
            imageIcon = null;
        }


        JMenuItem menuItem = ((imageIcon != null)
                              ? new JMenuItem(label,
                                  scaleImageIcon(imageIcon))
                              : new JMenuItem(label));
        String mnemonic = getAttribute(node, "mnemonic", (String) null);
        if (mnemonic != null) {
            int keyCode =
                charToKeyCode(mnemonic.trim().toUpperCase().charAt(0));
            if (keyCode != -1) {
                menuItem.setMnemonic(keyCode);
            }
        }

        String tooltip = getAttribute(attrs, ATTR_TOOLTIP, (String) null);
        if (tooltip != null) {
            menuItem.setToolTipText(tooltip);
        }



        String id   = getAttribute(attrs, ATTR_ID, (String) null);
        String icon = getAttribute(attrs, ATTR_ICON, (String) null);
        menuItem.addActionListener(listener);
        if (action != null) {
            menuItem.setActionCommand(action);
        }

        if (icon != null) {
            setIcon(menuItem, icon);
        }
        if (id != null) {
            menuItems.put(id, menuItem);
        }

        return menuItem;
    }

    /**
     * Take the first character of the string and return the numeric key code
     *
     * @param s The string
     *
     * @return The key code of the first char of the string
     */
    public static int charToKeyCode(String s) {
        return charToKeyCode(s.trim().toUpperCase().charAt(0));
    }

    /**
     * Return the numeric key code of the given character.
     *
     * @param ch The character
     *
     * @return Its key code
     */
    public static int charToKeyCode(char ch) {
        if ((ch == 'A') || (ch == 'a')) {
            return KeyEvent.VK_A;
        }
        if ((ch == 'B') || (ch == 'b')) {
            return KeyEvent.VK_B;
        }
        if ((ch == 'C') || (ch == 'c')) {
            return KeyEvent.VK_C;
        }
        if ((ch == 'D') || (ch == 'd')) {
            return KeyEvent.VK_D;
        }
        if ((ch == 'E') || (ch == 'e')) {
            return KeyEvent.VK_E;
        }
        if ((ch == 'F') || (ch == 'f')) {
            return KeyEvent.VK_F;
        }
        if ((ch == 'G') || (ch == 'g')) {
            return KeyEvent.VK_G;
        }
        if ((ch == 'H') || (ch == 'h')) {
            return KeyEvent.VK_H;
        }
        if ((ch == 'I') || (ch == 'i')) {
            return KeyEvent.VK_I;
        }
        if ((ch == 'J') || (ch == 'j')) {
            return KeyEvent.VK_J;
        }
        if ((ch == 'K') || (ch == 'k')) {
            return KeyEvent.VK_K;
        }
        if ((ch == 'L') || (ch == 'l')) {
            return KeyEvent.VK_L;
        }
        if ((ch == 'M') || (ch == 'm')) {
            return KeyEvent.VK_M;
        }
        if ((ch == 'N') || (ch == 'n')) {
            return KeyEvent.VK_N;
        }
        if ((ch == 'O') || (ch == 'o')) {
            return KeyEvent.VK_O;
        }
        if ((ch == 'P') || (ch == 'p')) {
            return KeyEvent.VK_P;
        }
        if ((ch == 'Q') || (ch == 'q')) {
            return KeyEvent.VK_Q;
        }
        if ((ch == 'R') || (ch == 'r')) {
            return KeyEvent.VK_R;
        }
        if ((ch == 'S') || (ch == 's')) {
            return KeyEvent.VK_S;
        }
        if ((ch == 'T') || (ch == 't')) {
            return KeyEvent.VK_T;
        }
        if ((ch == 'U') || (ch == 'u')) {
            return KeyEvent.VK_U;
        }
        if ((ch == 'V') || (ch == 'v')) {
            return KeyEvent.VK_V;
        }
        if ((ch == 'W') || (ch == 'w')) {
            return KeyEvent.VK_W;
        }
        if ((ch == 'X') || (ch == 'x')) {
            return KeyEvent.VK_X;
        }
        if ((ch == 'Y') || (ch == 'y')) {
            return KeyEvent.VK_Y;
        }
        if ((ch == 'Z') || (ch == 'z')) {
            return KeyEvent.VK_Z;
        }
        return -1;
    }


    /**
     *     Create the JMenu from the given xml.
     *
     *     @param menuNode The menu xml node
     *     @param listener The action listener
     *     @param menuItems Mapping from id to menu items
     *     @return New JMenu
     *
     *     @throws Exception
     */
    public static JMenu processXmlMenu(Node menuNode,
                                       ActionListener listener,
                                       Hashtable menuItems)
            throws Exception {
        return processXmlMenu(menuNode, listener, menuItems, null);
    }

    /**
     *     Create the JMenu from the given xml.
     *
     *     @param menuNode The menu xml node
     *     @param listener The action listener
     *     @param menuItems Mapping from id to menu items
     * @param actionIcons mapping of string action to imageicon to use in the menu item
     *     @return New JMenu
     *
     *     @throws Exception
     */
    public static JMenu processXmlMenu(Node menuNode,
                                       ActionListener listener,
                                       Hashtable menuItems,
                                       Hashtable<String,
                                           ImageIcon> actionIcons)
            throws Exception {
        NamedNodeMap attrs    = menuNode.getAttributes();
        String       id       = getAttribute(attrs, "id", (String) null);
        NodeList     children = menuNode.getChildNodes();
        String       label    = Msg.msg(getAttribute(attrs, "label"));
        JMenu        menu     = new JMenu(label);



        String mnemonic = getAttribute(menuNode.getAttributes(), "mnemonic",
                                       (String) null);
        String icon = getAttribute(attrs, "icon", (String) null);
        if (icon != null) {
            setIcon(menu, icon);
        }

        if (mnemonic != null) {
            int keyCode =
                charToKeyCode(mnemonic.trim().toUpperCase().charAt(0));
            if (keyCode != -1) {
                menu.setMnemonic(keyCode);
            }
        }

        JMenu menuToReturn = menu;

        if (id != null) {
            JMenu existing = (JMenu) menuItems.get(id);
            if (existing == null) {
                menuItems.put(id, menu);
            } else {
                menuToReturn = null;
                menu         = existing;
                if (getAttribute(menuNode.getAttributes(), "replace",
                                 false)) {
                    menu.removeAll();
                }
            }
        }
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().equals("menuitem")) {
                menu.add(processXmlMenuItem(child, listener, menuItems,
                                            actionIcons));
            } else if (child.getNodeName().equals("menu")) {
                JMenu childMenu = processXmlMenu(child, listener, menuItems,
                                      actionIcons);
                if (childMenu != null) {
                    menu.add(childMenu);
                }
            } else if (child.getNodeName().equals(MENU_SEPARATOR)) {
                menu.addSeparator();
            }
        }
        return menuToReturn;
    }


    /**
     * Create, if null, and ddd to the JMenuBar from the xml
     *
     * @param root Xml root
     * @param menuBar  The menu bar
     * @param listener The action listener
     * @param menuItems Mapping from id to menu items
     * @return The JMenuBar
     */
    public static JMenuBar processXmlMenuBar(Element root, JMenuBar menuBar,
                                             ActionListener listener,
                                             Hashtable menuItems) {
        return processXmlMenuBar(root, menuBar, listener, menuItems, null);
    }


    /**
     * Create, if null, and ddd to the JMenuBar from the xml
     *
     * @param root Xml root
     * @param menuBar  The menu bar
     * @param listener The action listener
     * @param menuItems Mapping from id to menu items
     * @param actionIcons mapping of string action to imageicon to use in the menu item
     * @return The JMenuBar
     */

    public static JMenuBar processXmlMenuBar(Element root, JMenuBar menuBar,
                                             ActionListener listener,
                                             Hashtable menuItems,
                                             Hashtable<String,
                                                 ImageIcon> actionIcons) {



        if (root == null) {
            return menuBar;
        }
        if (menuBar == null) {
            menuBar = new JMenuBar();
        }
        try {
            List menus = findChildren(root, "menu");
            for (int i = 0; i < menus.size(); i++) {
                JMenu menu = processXmlMenu((Node) menus.get(i), listener,
                                            menuItems, actionIcons);
                if (menu != null) {
                    menuBar.add(menu);
                }
            }
        } catch (Exception excp) {
            LogUtil.printException(log_, "Processing menu xml", excp);
        }
        return menuBar;
    }




    /**
     *  return the list of items held by the combo box.
     *
     * @param box The box
     * @return List of items
     */
    public static List getItems(JComboBox box) {
        List l = new ArrayList();
        for (int i = 0; i < box.getItemCount(); i++) {
            l.add(box.getItemAt(i));
        }
        return l;
    }


    /**
     * Is the frame showing
     *
     * @param f The frame
     * @return Is it showing
     */
    public static boolean isShowing(JFrame f) {
        if (f == null) {
            return false;
        }
        return f.isShowing() && (f.getState() != Frame.ICONIFIED);
    }


    /**
     * Is the dialog showing
     *
     * @param f The dialog
     * @return Is it showing
     */
    public static boolean isShowing(JDialog f) {
        if (f == null) {
            return false;
        }
        return f.isShowing();
    }



    /**
     * Get the screen image from the component
     *
     * @param component The component.
     * @return Its image
     *
     * @throws Exception
     */
    public static Image getImage(Component component) throws Exception {
        RepaintManager manager = RepaintManager.currentManager(component);
        //        Image image = manager.getOffscreenBuffer(component, component.getWidth (), component.getHeight ());
        double w = component.getWidth();
        double h = component.getHeight();
        BufferedImage image = new BufferedImage((int) w, (int) h,
                                  BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        component.paint(g);
        return image;
    }



    /**
     * Show the given html  in a window. If linkListener is non-null then
     * add itinto the html viewer to listen for  hyper link clicks.
     *
     * @param html The html
     * @param linkListener The listener
     *
     * @return A 2-tuple. First element is the JDialog. Second is the JEditorPane
     */
    public static Component[] showHtmlDialog(String html,
                                             HyperlinkListener linkListener) {
        return showHtmlDialog(html, "", linkListener);
    }


    /**
     * Show the given html in a window with the given title.
     * If linkListener is non-null then add itinto the html viewer to
     * listen for  hyper link clicks.
     *
     * @param html The html
     * @param title The title of the window
     * @param linkListener The listener
     *
     * @return A 2-tuple. First element is the JFrame. Second is the JEditorPane
     */
    public static Component[] showHtmlDialog(String html, String title,
                                             HyperlinkListener linkListener) {
        return showHtmlDialog(html, title, null, linkListener, false);
    }


    /**
     *  Make a JTextEditor component and scroller for the given html
     *
     * @param html html
     * @param linkListener Listener
     * @param width height
     * @param height width
     *
     * @return 2 component array. First is the editor. Second is the scroller its in
     */
    public static Component[] getHtmlComponent(String html,
            HyperlinkListener linkListener, int width, int height) {

        JEditorPane editor = new JEditorPane();
        //        editor.setMinimumSize(new Dimension(width, height));
        editor.setPreferredSize(new Dimension(width, height));
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.setText(html);
        if (linkListener != null) {
            editor.addHyperlinkListener(linkListener);
        }
        JScrollPane scroller = GuiUtils.makeScrollPane(editor, width, height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(width, height));
        //        scroller.setMinimumSize(new Dimension(width, height));
        return new Component[] { editor, scroller };
    }

    /**
     *  Show a dialog window that contains an html editor
     *
     * @param html html
     * @param title  window title
     * @param label label
     * @param linkListener Listener
     * @param modal is window modal
     *
     * @return 2 component array. First is the editor. Second is the scroller its in
     */
    public static Component[] showHtmlDialog(String html, String title,
                                             String label,
                                             HyperlinkListener linkListener,
                                             boolean modal) {
        Component[] comps    = getHtmlComponent(html, linkListener, 400, 600);
        JEditorPane editor   = (JEditorPane) comps[0];
        JScrollPane scroller = (JScrollPane) comps[1];


        if (title == null) {
            title = "";
        }
        final JDialog  window       = createDialog(title, modal);
        ActionListener windowCloser = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                window.dispose();
            }
        };
        JPanel buttons = makeButtons(windowCloser,
                                     new String[] { CMD_CLOSE });
        JPanel contents = centerBottom(scroller, buttons);
        if (label != null) {
            label = "<html><h4>" + label + "</h4></html>";
            contents = GuiUtils.topCenter(GuiUtils.inset(new JLabel(label),
                    1), contents);
        }
        window.getContentPane().add(contents);
        window.pack();
        try {
            editor.setCaretPosition(0);
            editor.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
        } catch (Exception noop) {}
        Msg.translateTree(window);
        window.setVisible(true);
        return new Component[] { window, editor };
    }



    /**
     * Call this before a JTree changes. It stores into the returned hashtable
     * state that it uses later to re-expand the paths of the jtree after its
     * structure changes.
     *
     * @param tree The jtree
     * @param root Its tree root
     *
     * @return Holds the state for later expansion
     */
    public static Hashtable initializeExpandedPathsBeforeChange(JTree tree,
            DefaultMutableTreeNode root) {
        Enumeration paths =
            tree.getExpandedDescendants(new TreePath(root.getPath()));
        Hashtable expandedPaths = new Hashtable();
        if (paths != null) {
            while (paths.hasMoreElements()) {
                TreePath treePath = (TreePath) paths.nextElement();
                expandedPaths.put(treePath.toString(), Boolean.TRUE);
            }
        }
        return expandedPaths;
    }


    /**
     * Call this after the structure of a JTree changes to re-expand the paths.
     *
     * @param tree The tree
     * @param state The state. From initializeExpandedPathsBeforeChange
     * @param root The tree root
     */
    public static void expandPathsAfterChange(JTree tree, Hashtable state,
            DefaultMutableTreeNode root) {
        TreePath path = new TreePath(root.getPath());
        if (state.get(path.toString()) != null) {
            tree.expandPath(path);
        }
        for (int i = 0; i < root.getChildCount(); i++) {
            expandPathsAfterChange(
                tree, state, (DefaultMutableTreeNode) root.getChildAt(i));
        }
    }


    /**
     * Class TreeSearchResults Holds state from a tree search
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.317 $
     */
    public static class TreeSearchResults {

        /** success */
        public boolean success;

        /** Last position */
        public Object lastState;

        /**
         * ctor
         *
         * @param success success
         * @param lastState lastState
         */
        public TreeSearchResults(boolean success, Object lastState) {
            this.success   = success;
            this.lastState = lastState;
        }
    }


    /**
     * Popup a dialog and search the given tree for a tree node that matches
     * the user provided string.
     *
     * @param tree Tree to search
     * @param lastState For successive searches, this keeps state around
     * @param nodeDesc A descriptive term for the node for when we tell something to the user
     * @param near If non-null then show the input dialog near this component
     *
     * @return Some state for successive searches
     */
    public static TreeSearchResults doTreeSearch(JTree tree,
            Object lastState, String nodeDesc, JComponent near) {

        return doTreeSearch(tree, lastState, nodeDesc, near, null);
    }

    /**
     * This takes the list of JMenuItems and, for each JMenu, will ensure
     * that there are no more than size number of items in any sub menu.
     * It uses name (e.g., Group) to make the sub-menus, e.g., Group 1, Group 2, ...
     *
     * @param items List of JMenuItems
     * @param name The name suffix to use
     * @param size Max size of a menu
     */
    public static void limitMenuSize(List items, String name, int size) {
        for (int i = 0; i < items.size(); i++) {
            Object o = items.get(i);
            if ( !(o instanceof JMenu)) {
                continue;
            }
            JMenu menu = (JMenu) o;
            limitMenuSize(menu, name, size);
        }
    }

    /**
     * This ensures that there are no more than size number of items in any sub menu.
     * It uses name (e.g., Group) to make the sub-menus, e.g., Group 1, Group 2, ...
     *
     *
     * @param menu The menu
     * @param name The name suffix to use
     * @param size Max size of a menu
     */
    public static void limitMenuSize(JMenu menu, String name, int size) {
        limitMenuSize(menu, name, size, true);
    }


    /**
     * This ensures that there are no more than size number of items in any sub menu.
     * It uses name (e.g., Group) to make the sub-menus, e.g., Group 1, Group 2, ...
     *
     *
     * @param menu The menu
     * @param name The name suffix to use
     * @param size Max size of a menu
     * @param recurse If true then limit the size of all sub menus
     */
    public static void limitMenuSize(JMenu menu, String name, int size,
                                     boolean recurse) {
        int  cnt      = menu.getItemCount();
        List subMenus = new ArrayList();
        List subItems = new ArrayList();
        for (int i = 0; i < cnt; i++) {
            JMenuItem mi = menu.getItem(i);
            subItems.add(mi);
            if (mi instanceof JMenu) {
                subMenus.add(mi);
            }
        }
        if (recurse) {
            limitMenuSize(subMenus, name, size);
        }
        if (cnt >= size) {
            menu.removeAll();
            JMenu currentMenu = new JMenu(name + " 1");
            menu.add(currentMenu);
            for (int i = 0; i < subItems.size(); i++) {
                JMenuItem mi = (JMenuItem) subItems.get(i);
                if (currentMenu.getItemCount() >= size) {
                    currentMenu = new JMenu(name + " "
                                            + (menu.getItemCount() + 1));
                    menu.add(currentMenu);
                }
                if (mi == null) {
                    continue;
                }
                currentMenu.add(mi);
            }
        }
    }

    /**
     * Search the given tree
     *
     * @param tree the tree
     * @param lastState   last state
     * @param nodeDesc    the node description
     * @param near        the near component
     * @param originalPhrase   the search phrase
     *
     * @return the results
     */
    public static TreeSearchResults doTreeSearch(JTree tree,
            Object lastState, String nodeDesc, JComponent near,
            String originalPhrase) {
        String startAt      = null;
        String searchString = "";
        String label        = "Search for a " + nodeDesc;
        if (lastState != null) {
            searchString = (String) ((Object[]) lastState)[0];
            startAt      = (String) ((Object[]) lastState)[1];
        }
        if (originalPhrase != null) {
            searchString = originalPhrase;
        }
        DefaultTreeModel treeModel = (DefaultTreeModel) tree.getModel();
        Object           node      = null;
        boolean          success   = false;
        while (true) {
            if (originalPhrase == null) {
                String s = getInput(label, "Search For: ", searchString,
                                    null, null, "", 20, near);
                if (s == null) {
                    break;
                }
                searchString = s;
            }
            Object treeRoot = tree.getModel().getRoot();
            node = treeSearch(treeModel, treeRoot, searchString,
                              new Object[] { startAt });
            if (node != null) {
                success = true;
                TreeNode[] pathArray =
                    treeModel.getPathToRoot((TreeNode) node);
                TreePath path = new TreePath(pathArray);
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                break;
            } else {
                tree.clearSelection();
            }
            startAt = null;
            label   = "Could not find " + nodeDesc;
            if (originalPhrase != null) {
                break;
            }
        }
        return new TreeSearchResults(success,
                                     new Object[] { searchString,
                                         ((node != null)
                                          ? node.toString()
                                          : (String) null) });
    }


    /**
     * Recurse down the tree searching for a node that matches the given string
     *
     * @param treeModel tree model
     * @param node Current node we're looking at
     * @param s Search string
     * @param startAt If holds a non-null string then don't start searching until we hit a node
     * that matches the string
     *
     * @return The found node or null
     */
    private static Object treeSearch(DefaultTreeModel treeModel, Object node,
                                     String s, Object[] startAt) {
        if (startAt[0] == null) {
            if (StringUtil.stringMatch(node.toString(), s, true, false)) {
                return node;
            }
        }
        if ((startAt[0] != null)
                && Misc.equals(startAt[0].toString(), node.toString())) {
            startAt[0] = null;
        }
        int cnt = treeModel.getChildCount(node);
        for (int i = 0; i < cnt; i++) {
            Object child = treeModel.getChild(node, i);
            child = treeSearch(treeModel, child, s, startAt);
            if (child != null) {
                return child;
            }
        }
        return null;
    }


    /**
     * Convert the given table model to comma separated string
     *
     * @param model The table model to write
     *
     * @return CSV representation of the given table model
     */
    public static String toCsv(TableModel model) {
        return toCsv(model, false);
    }

    /**
     * Convert the given table model to comma separated string
     *
     * @param model The table model to write
     * @param includeColumnNames  true to include the column names
     *
     * @return CSV representation of the given table model
     */
    public static String toCsv(TableModel model, boolean includeColumnNames) {
        StringBuffer sb   = new StringBuffer();
        int          rows = model.getRowCount();
        int          cols = model.getColumnCount();
        if (includeColumnNames) {
            for (int col = 0; col < cols; col++) {
                if (col > 0) {
                    sb.append(",");
                }
                sb.append(model.getColumnName(col));
            }
            sb.append("\n");
        }
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (col > 0) {
                    sb.append(",");
                }
                sb.append(model.getValueAt(row, col));
            }
            sb.append("\n");
        }
        return sb.toString();
    }




    /**
     * Write out the given table model as a csv file
     *
     * @param model The table model to write
     */
    public static void exportAsCsv(TableModel model) {
        exportAsCsv("", model, false);
    }

    /**
     * Write out the given table model as a comma separated value (CSV) file
     * prepending the header.
     *
     * @param header  a header to prepend to the table
     * @param model The table model to write
     */
    public static void exportAsCsv(String header, TableModel model) {
        exportAsCsv(header, model, false);
    }

    /**
     * Write out the given table model as a comma separated value (CSV) file
     * prepending the header.
     *
     * @param header  a header to prepend to the table
     * @param model The table model to write
     * @param includeColumnNames  true to include the column names
     */
    public static void exportAsCsv(String header, TableModel model,
                                   boolean includeColumnNames) {
        String filename =
            FileManager.getWriteFile(Misc.newList(FileManager.FILTER_CSV,
                FileManager.FILTER_XLS), FileManager.SUFFIX_CSV);
        if (filename == null) {
            return;
        }
        exportAsCsv(header, model, filename, includeColumnNames);
    }

    /**
     * Export a table as csv
     *
     * @param header   header for the output
     * @param model    table model
     * @param filename  file name to export to
     */
    public static void exportAsCsv(String header, TableModel model,
                                   String filename) {
        exportAsCsv(header, model, filename, false);
    }

    /**
     * Export a table as csv
     *
     * @param header   header for the output
     * @param model    table model
     * @param filename  file name to export to
     * @param includeColumnNames  true to include the column names
     */
    public static void exportAsCsv(String header, TableModel model,
                                   String filename,
                                   boolean includeColumnNames) {
        if (filename.toLowerCase().endsWith(".xls")) {
            //A hack
            try {
                Class c = Misc.findClass("ucar.unidata.data.DataUtil");
                Method method = Misc.findMethod(c, "writeXls",
                                    new Class[] { String.class,
                        List.class });
                List rows    = new ArrayList();
                int  numRows = model.getRowCount();
                int  numCols = model.getColumnCount();
                if (includeColumnNames) {
                    List colNames = new ArrayList();
                    for (int i = 0; i < model.getColumnCount(); i++) {
                        colNames.add(model.getColumnName(i));
                    }
                    rows.add(colNames);
                }
                for (int row = 0; row < numRows; row++) {
                    List cols = new ArrayList();
                    rows.add(cols);
                    for (int col = 0; col < numCols; col++) {
                        cols.add(model.getValueAt(row, col));
                    }
                }
                method.invoke(null, new Object[] { filename, rows });
            } catch (Exception exc) {
                LogUtil.logException("Exporting data to xsl", exc);
            }
            return;
        }
        String csv = GuiUtils.toCsv(model, includeColumnNames);
        if (csv == null) {
            return;
        }
        if (header == null) {
            header = "";
        }
        try {
            String output = ((header == null) || header.equals(""))
                            ? csv
                            : header + "\n" + csv;
            IOUtil.writeFile(filename, output);
        } catch (Exception exc) {
            LogUtil.logException("Exporting data", exc);
        }
    }



    /**
     * Set up a directory chooser
     *
     * @param btn          button for choosing directory
     * @param directoryFld directory field
     */
    public static void setupDirectoryChooser(JButton btn,
                                             final JTextField directoryFld) {

        setupFileChooser(btn, directoryFld, true);
    }

    /**
     * Set up a directory chooser
     *
     * @param btn          button for choosing directory
     * @param directoryFld directory field
     * @param justDirectories  flag for just looking at directories
     */
    public static void setupFileChooser(JButton btn,
                                        final JTextField directoryFld,
                                        final boolean justDirectories) {

        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser =
                    new javax.swing.JFileChooser(
                        new File(directoryFld.getText().trim()));
                if (justDirectories) {
                    chooser.setFileSelectionMode(
                        JFileChooser.DIRECTORIES_ONLY);
                }
                File file      = chooser.getSelectedFile();
                int  returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    directoryFld.setText(
                        chooser.getSelectedFile().toString());
                }
            }
        });
    }



    /**
     * Make a checkbox. Automatically call the set'property' method on the object
     *
     * @param label Label
     * @param object  Object to call
     * @param property Name of property to get/set value
     *
     * @return The checkbox
     */
    public static JCheckBox makeCheckbox(String label, final Object object,
                                         final String property) {

        return makeCheckbox(label, object, property, null);
    }


    /**
     * Make a checkbox. Automatically call the set'property' method on the object
     *
     * @param label Label
     * @param object  Object to call
     * @param property Name of property to get/set value
     * @param arg Optional arg to pass to method
     *
     * @return The checkbox
     */
    public static JCheckBox makeCheckbox(String label, final Object object,
                                         final String property,
                                         final Object arg) {

        boolean value = true;
        try {
            String methodName = "get"
                                + property.substring(0, 1).toUpperCase()
                                + property.substring(1);
            Method theMethod = Misc.findMethod(object.getClass(), methodName,
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


        final JCheckBox cbx      = new JCheckBox(label, value);
        ActionListener  listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    String methodName =
                        "set" + property.substring(0, 1).toUpperCase()
                        + property.substring(1);
                    Method theMethod = Misc.findMethod(object.getClass(),
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
        cbx.addActionListener(listener);
        return cbx;
    }




    /**
     * Make a set of radio buttons
     *
     * @param labels labels
     * @param selectedIndex which one is on
     * @param object  Object to call
     * @param methodName The method
     *
     * @return The radio buttons
     */
    public static JRadioButton[] makeRadioButtons(List labels,
            int selectedIndex, final Object object, String methodName) {

        try {
            final Method theMethod = Misc.findMethod(object.getClass(),
                                         methodName,
                                         new Class[] { Integer.TYPE });
            JRadioButton[] rbs = new JRadioButton[labels.size()];
            ButtonGroup    bg  = new ButtonGroup();
            for (int i = 0; i < labels.size(); i++) {
                final int theIndex = i;
                JRadioButton rb = new JRadioButton(labels.get(i).toString(),
                                      i == selectedIndex);
                bg.add(rb);
                ActionListener listener = new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            theMethod.invoke(object,
                                             new Object[] {
                                                 new Integer(theIndex) });
                        } catch (Exception exc) {
                            System.err.println("Error in makeCheckbox:"
                                    + exc);
                            exc.printStackTrace();
                        }
                    }
                };
                rb.addActionListener(listener);
                rbs[i] = rb;
            }
            return rbs;
        } catch (Exception exc) {
            throw new IllegalArgumentException("Error in makeCeckbox:" + exc);
        }
    }





    /**
     * Make a JButton. Call methodName on object when button pressed.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     *
     * @return The button
     */
    public static JButton makeButton(String label, final Object object,
                                     final String methodName) {
        return makeButton(label, object, methodName, null);
    }

    /**
     * Make a JButton. Call methodName on object when button pressed.
     * Pass in given arg if non-null.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     * @param arg Pass this to method name if non-null.
     *
     * @return The button
     */
    public static JButton makeButton(final String label, final Object object,
                                     final String methodName,
                                     final Object arg) {
        return makeButton(label, object, methodName, arg, null);
    }

    /**
     * Make a JButton. Call methodName on object when button pressed.
     * Pass in given arg if non-null.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     * @param arg Pass this to method name if non-null.
     * @param tooltip if non-null then set the tooltip on the button
     *
     * @return The button
     */
    public static JButton makeButton(final String label, final Object object,
                                     final String methodName,
                                     final Object arg, String tooltip) {
        final JButton btn = new JButton(label);
        btn.addActionListener(makeActionListener(object, methodName, arg));
        if (tooltip != null) {
            btn.setToolTipText(tooltip);
        }
        return btn;
    }



    /**
     * Make an ActionListener. Call methodName on object when button pressed.
     * Pass in given arg if non-null.
     *
     * @param object Object to call
     * @param methodName Method name to call
     * @param arg Pass this to method name if non-null.
     *
     * @return The action listener
     */
    public static ActionListener makeActionListener(final Object object,
            final String methodName, final Object arg) {
        final Method   theMethod = findMethod(object, methodName, arg);
        ActionListener listener  = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    if (arg == null) {
                        theMethod.invoke(object, new Object[] {});
                    } else {
                        theMethod.invoke(object, new Object[] { arg });
                    }
                } catch (Exception exc) {
                    LogUtil.logException(
                        "Error in makeActionListener calling:" + methodName,
                        exc);
                }
            }
        };
        return listener;
    }


    /**
     * RUn the given runnable in the swing event dispatch thread
     *
     * @param runnable runnable to run
     */
    public static void invokeInSwingThread(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception exc) {
                throw new WrapperException("Error invoking in swing thread",
                                           exc);
            }
        }
    }


    /**
     * Make a JButton. Call methodName on object when button pressed.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     *
     * @return The button
     */
    public static JButton makeImageButton(String label, final Object object,
                                          final String methodName) {
        return makeImageButton(label, object, methodName, null);
    }

    /**
     * Make a JButton. Call methodName on object when button pressed.
     * Pass in given arg if non-null.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     * @param arg Pass this to method name if non-null.
     *
     * @return The button
     */
    public static JButton makeImageButton(String label, final Object object,
                                          final String methodName,
                                          final Object arg) {

        return makeImageButton(label, object, methodName, arg, false);
    }



    /**
     * Make a JButton. Call methodName on object when button pressed.
     * Pass in given arg if non-null.
     *
     * @param label Label
     * @param object Object to call
     * @param methodName Method name to call
     * @param arg Pass this to method name if non-null.
     * @param addMouseOverBorder add a mouse-over border
     *
     * @return The button
     */
    public static JButton makeImageButton(String label, final Object object,
                                          final String methodName,
                                          final Object arg,
                                          boolean addMouseOverBorder) {
        final JButton btn = getImageButton(label, GuiUtils.class);
        btn.setBackground(null);
        if (addMouseOverBorder) {
            makeMouseOverBorder(btn);
        }
        return (JButton) addActionListener(btn, object, methodName, arg);
    }



    /**
     * Adds an action listener to the button.
     * Call methodName on object when button pressed. Pass in given arg
     * if non-null.
     *
     * @param comp The component
     * @param object Object to call
     * @param methodName Method name to call
     * @param arg Pass this to method name if non-null.
     *
     * @return The button
     */
    public static JComponent addActionListener(JComponent comp,
            final Object object, final String methodName, final Object arg) {
        final Method   theMethod = findMethod(object, methodName, arg);
        ActionListener listener  = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    if (arg == null) {
                        theMethod.invoke(object, new Object[] {});
                    } else {
                        theMethod.invoke(object, new Object[] { arg });
                    }
                } catch (Exception exc) {
                    LogUtil.logException("Error in makeButton calling:"
                                         + methodName, exc);
                }
            }
        };
        if (comp instanceof AbstractButton) {
            ((AbstractButton) comp).addActionListener(listener);
        } else if (comp instanceof JTextField) {
            ((JTextField) comp).addActionListener(listener);
        } else {
            throw new IllegalArgumentException("Cannot add action listener");
        }
        return comp;
    }









    /**
     * Create a menu and add a listener to it that removes all items
     * and calls the given method on te given object with the menu as an argument.
     *
     * @param name Menu name
     * @param object Object to call
     * @param methodName method to invoke
     * @return The menu
     */
    public static JMenu makeDynamicMenu(final String name,
                                        final Object object,
                                        String methodName) {
        return makeDynamicMenu(name, object, methodName, true);
    }


    /**
     * Create a menu and add a listener to it that removes all items
     * and calls the given method on te given object with the menu as an argument.
     *
     * @param name Menu name
     * @param object Object to call
     * @param methodName method to invoke
     * @param doRemoveAll   true to remove all first
     * @return The menu
     */
    public static JMenu makeDynamicMenu(final String name,
                                        final Object object,
                                        String methodName,
                                        final boolean doRemoveAll) {
        final JMenu  menu      = new JMenu(name);
        final Method theMethod = findMethod(object, methodName, menu);
        menu.addMenuListener(new MenuListener() {
            public void menuCanceled(MenuEvent e) {}

            public void menuDeselected(MenuEvent e) {}

            public void menuSelected(MenuEvent e) {
                try {
                    if (doRemoveAll) {
                        menu.removeAll();
                    }
                    theMethod.invoke(object, new Object[] { menu });
                } catch (Exception exc) {
                    LogUtil.logException("Error in makeDynamicMenu:" + name,
                                         exc);
                }
            }
        });
        return menu;
    }





    /**
     * Make a JComboBox
     *
     * @param items    items for the box
     * @param selected the selected item
     * @param editable flag for whether this is editable or not
     * @param listener Listener for changes
     * @param methodName method to call when item changes
     *
     * @return the combo box
     */
    public static JComboBox makeComboBox(List items, Object selected,
                                         boolean editable,
                                         final Object listener,
                                         String methodName) {
        return makeComboBox(items, selected, editable, listener, methodName,
                            false);
    }

    /**
     * Make a JComboBox
     *
     * @param items    items for the box
     * @param selected the selected item
     * @param editable flag for whether this is editable or not
     * @param listener Listener for changes
     * @param methodName method to call when item changes
     * @param inAThread Call the method in a thread
     *
     * @return the combo box
     */
    public static JComboBox makeComboBox(List items, Object selected,
                                         boolean editable,
                                         final Object listener,
                                         String methodName,
                                         final boolean inAThread) {


        final Method theMethod = findMethod(listener, methodName,
                                            ((selected == null)
                                             ? new Object()
                                             : selected));
        final JComboBox box = new JComboBox(new Vector(items));
        if (selected != null) {
            box.setSelectedItem(selected);
        }
        //This is a little odd but when the box is editable its preferred size is 
        //increased. So we get the preferred size before, then set editable then reset the 
        //preferred size.
        if (editable) {
            Dimension preferred = box.getPreferredSize();
            box.setEditable(true);
            if (checkHeight(preferred.height)) {
                box.setPreferredSize(preferred);
            }
        }

        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    theMethod.invoke(listener,
                                     new Object[] { box.getSelectedItem() });
                } catch (Exception exc) {
                    System.err.println("Error invoking method" + exc);
                    exc.printStackTrace();
                }
            }
        });
        return box;
    }



    /**
     * Make a combo box from a set of values and labels
     *
     * @param values   integer values
     * @param labels   labels for values
     * @param current  current one to select
     *
     * @return the JComboBox
     */
    public static JComboBox makeComboBox(int[] values, String[] labels,
                                         int current) {
        List      l        = TwoFacedObject.createList(values, labels);
        JComboBox box      = new JComboBox(new Vector(l));
        Object    selected = TwoFacedObject.findId(new Integer(current), l);
        if (selected != null) {
            box.setSelectedItem(selected);
        }
        return box;
    }

    /**
     * Set the value of the box
     *
     * @param box  the box to set
     * @param value  the default value
     * @param values the values
     * @param labels labels for the values
     */
    public static void setValueOfBox(JComboBox box, int value, int[] values,
                                     String[] labels) {
        for (int i = 0; i < values.length; i++) {
            if (value == values[i]) {
                box.setSelectedItem(new TwoFacedObject(labels[i], value));
                return;
            }
        }
        box.setSelectedItem(new TwoFacedObject("" + value, value));
    }

    /**
     * Get the integer value from a JComboBox of integer items
     *
     * @param box   the box to use
     *
     * @return  the integer value
     */
    public static int getValueFromBox(JComboBox box) {
        TwoFacedObject tfo = (TwoFacedObject) box.getSelectedItem();
        return ((Integer) tfo.getId()).intValue();
    }


    /**
     * Make a JSlider
     *
     * @param min    minimum value
     * @param max    maximum value
     * @param value  initial value
     * @param listener   listener for changes
     * @param methodName method to call when change occurs
     *
     * @return JSlider
     */
    public static JSlider makeSlider(int min, int max, int value,
                                     final Object listener,
                                     String methodName) {
        return makeSlider(min, max, value, listener, methodName, false);
    }



    /**
     * Make a JSlider
     *
     * @param min    minimum value
     * @param max    maximum value
     * @param value  initial value
     * @param listener   listener for changes
     * @param methodName method to call when change occurs
     * @param updateAsMove If true we call the listener as the slider moves.
     *
     * @return JSlider
     */
    public static JSlider makeSlider(int min, int max, int value,
                                     final Object listener,
                                     String methodName,
                                     final boolean updateAsMove) {
        final Method theMethod = findMethodWithClass(listener, methodName,
                                     Integer.TYPE);

        JSlider slider = new JSlider(min, max, value);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slider = (JSlider) e.getSource();
                if (updateAsMove || !slider.getValueIsAdjusting()) {
                    int value = slider.getValue();
                    try {
                        theMethod.invoke(listener,
                                         new Object[] { new Integer(value) });
                    } catch (Exception exc) {
                        System.err.println("Error invoking method" + exc);
                        exc.printStackTrace();
                    }
                }
            }
        });
        return slider;
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
            theMethod = Misc.findMethod(object.getClass(), methodName,
                                        new Class[] {});
        } else {
            theMethod = Misc.findMethod(object.getClass(), methodName,
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
     * Find a method with a class argument
     *
     * @param object     Object with the method in it
     * @param methodName method name
     * @param c          the class of the method's argument
     *
     * @return  the method
     */
    private static Method findMethodWithClass(Object object,
            String methodName, Class c) {
        Method theMethod = null;
        theMethod = Misc.findMethod(object.getClass(), methodName,
                                    new Class[] { c });
        if (theMethod == null) {
            throw new IllegalArgumentException("Unknown method:"
                    + object.getClass() + "." + methodName);
        }
        return theMethod;

    }

    /**
     * Get a list of font sizes
     *
     * @return list of font sizes
     */
    public static Vector getFontSizeList() {
        int[]  fontSizes    = GuiUtils.FONT_SIZES;
        Vector fontSizeList = new Vector();
        for (int i = 0; i < FONT_SIZES.length; i++) {
            fontSizeList.add(new Integer(FONT_SIZES[i]));
        }
        return fontSizeList;
    }

    /**
     * Get a list of fonts
     *
     * @return vector of fonts
     */
    public static Vector getFontList() {
        Font[] fonts =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        Vector         fontList = new Vector();
        TwoFacedObject selected = null;
        for (int i = 0; i < fonts.length; i++) {
            fontList.add(makeTwoFacedFont(fonts[i]));
        }
        return fontList;
    }

    /**
     * Make a TwoFacedObject from a font for displaying in a combobox.
     * @param f  Font to use
     * @return corresponding TwoFacedObject
     */
    public static TwoFacedObject makeTwoFacedFont(Font f) {
        return new TwoFacedObject(StringUtil.shorten(f.getName(), 24), f);
    }

    /**
     * Make the given row in the table visible
     *
     * @param table The table
     * @param row The row
     */
    public static void makeRowVisible(JTable table, int row) {
        Rectangle cellRect = table.getCellRect(row, 0, true);
        if (cellRect != null) {
            table.scrollRectToVisible(cellRect);
        }
    }

    /**
     * Scroll the given component to the top
     *
     * @param editor editor to scroll
     */
    public static void scrollToTop(final JEditorPane editor) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    editor.scrollRectToVisible(new Rectangle(1, 1, 1, 1));
                } catch (Exception exc) {}
            }
        });
    }



    /**
     * Add a mouse listener to all components of a container
     *
     * @param listener  the mouse listener
     * @param c  the container
     */
    public static void addMouseListenerRecurse(MouseListener listener,
            Container c) {
        c.addMouseListener(listener);
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponent(i);
            if (child instanceof Container) {
                addMouseListenerRecurse(listener, (Container) child);
            }
        }

    }


    /**
     * Add a key listener to all components of a container
     *
     * @param listener  the key listener
     * @param c  the container
     */
    public static void addKeyListenerRecurse(KeyListener listener,
                                             Container c) {
        c.addKeyListener(listener);
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component child = c.getComponent(i);
            if (child instanceof Container) {
                addKeyListenerRecurse(listener, (Container) child);
            }
        }

    }




    /**
     * Calculate distance between 2 points.
     *
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     *
     * @return distance
     */
    public static double distance(double x1, double y1, double x2,
                                  double y2) {
        return (Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)));
    }

    /**
     * Get distance between
     *
     * @param p1 From
     * @param p2 To
     *
     * @return Distance
     */
    public static double distance(double[] p1, double[] p2) {
        return distance(p1[0], p1[1], p2[0], p2[1]);
    }



    /**
     * Get distance between
     *
     * @param p1 From
     * @param p2 To
     *
     * @return Distance
     */
    public static double distance(int[] p1, int[] p2) {
        return distance(p1[0], p1[1], p2[0], p2[1]);
    }




    /**
     * Calculate distance between point and rectangle.
     * This will give the min distance from the 4 corners
     * and the sides.
     *
     * @param x x
     * @param y y
     * @param r rect
     *
     * @return distance_
     */
    public static double distance(double x, double y, Rectangle2D r) {
        double minDistance = distance(x, y, r.getX(), r.getY());
        minDistance = Math.min(minDistance,
                               distance(x, y, r.getX(),
                                        r.getY() + r.getHeight()));
        minDistance = Math.min(minDistance,
                               distance(x, y, r.getX() + r.getWidth(),
                                        r.getY()));
        minDistance = Math.min(minDistance,
                               distance(x, y, r.getX() + r.getWidth(),
                                        r.getY() + r.getHeight()));

        if ((y >= r.getY()) && (y <= r.getY() + r.getHeight())) {
            minDistance = Math.min(minDistance,
                                   Math.min(Math.abs(x - r.getX()),
                                            Math.abs(x
                                                - (r.getX()
                                                    + r.getWidth()))));
        }

        if ((x >= r.getX()) && (x <= r.getX() + r.getWidth())) {
            minDistance = Math.min(minDistance,
                                   Math.min(Math.abs(y - r.getY()),
                                            Math.abs(y
                                                - (r.getY()
                                                    + r.getHeight()))));
        }
        //        System.err.println (x +"/"+y + "r=" + r +" dis= " + minDistance);
        return minDistance;

    }



    /**
     * Make a vertical label
     *
     * @param text text
     *
     * @return vertical label
     */
    public static JLabel makeVerticalLabel(String text) {
        JLabel l = new JLabel(text);
        l.setUI(new VerticalLabelUI(false));
        return l;
    }

    /**
     * Class for a vertical label
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.317 $
     */
    public static class VerticalLabelUI extends BasicLabelUI {

        /** rotation */
        protected boolean clockwise;

        /**
         * Create a rotated vertical label
         *
         * @param clockwise  true if rotation is clockwise
         */
        VerticalLabelUI(boolean clockwise) {
            super();
            this.clockwise = clockwise;
        }

        /**
         * Get the preferred size
         *
         * @param c  the component
         *
         * @return  the dimension
         */
        public Dimension getPreferredSize(JComponent c) {
            Dimension dim = super.getPreferredSize(c);
            return new Dimension(dim.height, dim.width);
        }

        /** paint icon rectangle */
        private static Rectangle paintIconR = new Rectangle();

        /** paint text rectangle */
        private static Rectangle paintTextR = new Rectangle();

        /** paint view rectangle */
        private static Rectangle paintViewR = new Rectangle();

        /** paint view insets */
        private static Insets paintViewInsets = new Insets(0, 0, 0, 0);

        /**
         * Paint
         *
         * @param g graphics context
         * @param c the component
         */
        public void paint(Graphics g, JComponent c) {
            JLabel label = (JLabel) c;
            String text  = label.getText();
            Icon   icon  = (label.isEnabled())
                           ? label.getIcon()
                           : label.getDisabledIcon();

            if ((icon == null) && (text == null)) {
                return;
            }

            FontMetrics fm = g.getFontMetrics();
            paintViewInsets = c.getInsets(paintViewInsets);

            paintViewR.x    = paintViewInsets.left;
            paintViewR.y    = paintViewInsets.top;

            // Use inverted height & width
            paintViewR.height = c.getWidth()
                                - (paintViewInsets.left
                                   + paintViewInsets.right);
            paintViewR.width = c.getHeight()
                               - (paintViewInsets.top
                                  + paintViewInsets.bottom);

            paintIconR.x = paintIconR.y = paintIconR.width =
                paintIconR.height = 0;
            paintTextR.x = paintTextR.y = paintTextR.width =
                paintTextR.height = 0;

            String clippedText = layoutCL(label, fm, text, icon, paintViewR,
                                          paintIconR, paintTextR);

            Graphics2D      g2 = (Graphics2D) g;
            AffineTransform tr = g2.getTransform();
            if (clockwise) {
                g2.rotate(Math.PI / 2);
                g2.translate(0, -c.getWidth());
            } else {
                g2.rotate(-Math.PI / 2);
                g2.translate(-c.getHeight(), 0);
            }

            if (icon != null) {
                icon.paintIcon(c, g, paintIconR.x, paintIconR.y);
            }

            if (text != null) {
                int textX = paintTextR.x;
                int textY = paintTextR.y + fm.getAscent();

                if (label.isEnabled()) {
                    paintEnabledText(label, g, clippedText, textX, textY);
                } else {
                    paintDisabledText(label, g, clippedText, textX, textY);
                }
            }

            g2.setTransform(tr);
        }
    }


    /**
     * Make an evenly balanced group of split panes for the given components
     *
     * @param comps List of components
     * @param hsplit horizontal or vertical
     *
     * @return split panes
     */
    public static JComponent doMultiSplitPane(List comps, boolean hsplit) {
        if (comps.size() == 0) {
            return null;
        }
        if (comps.size() == 1) {
            return (JComponent) comps.get(0);
        }
        JSplitPane split;
        if (comps.size() == 2) {
            if (hsplit) {
                split = hsplit((Component) comps.get(0),
                               (Component) comps.get(1), 0.5);
            } else {
                split = vsplit((Component) comps.get(0),
                               (Component) comps.get(1), 0.5);
            }
        } else {
            Component comp = (Component) comps.get(0);
            comps.remove(0);
            int size = comps.size();
            if (hsplit) {
                split = hsplit(comp, doMultiSplitPane(comps, hsplit));
            } else {
                split = vsplit(comp, doMultiSplitPane(comps, hsplit));
            }
            size++;
            split.setResizeWeight(1.0 / size);
        }
        split.setOneTouchExpandable(true);
        return split;
    }

    /**
     * Make a JTabbedPane without some of its border
     *
     * @return Tabbed pane
     */
    public static JTabbedPane getNestedTabbedPane() {
        return getNestedTabbedPane(JTabbedPane.TOP);
    }

    /**
     * Make a JTabbedPane without some of its border
     *
     * @param orient tab orientation
     *
     * @return Tabbed pane
     */
    public static JTabbedPane getNestedTabbedPane(int orient) {
        if (orient == JTabbedPane.TOP) {
            return getNestedTabbedPane(orient, 1, 0, 0, 0);
        }
        if (orient == JTabbedPane.BOTTOM) {
            return getNestedTabbedPane(orient, 0, 0, 1, 0);
        }
        if (orient == JTabbedPane.LEFT) {
            return getNestedTabbedPane(orient, 1, 1, 0, 0);
        }
        return getNestedTabbedPane(orient, 1, 0, 0, 1);
    }



    /**
     * Make a JTabbedPane without some of its border
     *
     * @param orient tab orientation
     * @param top top border
     * @param left left border
     * @param bottom bottom border
     * @param right right  border
     *
     * @return Tabbed pane
     */
    public static JTabbedPane getNestedTabbedPane(int orient, int top,
            int left, int bottom, int right) {
        Insets oldInsets =
            UIManager.getInsets("TabbedPane.contentBorderInsets");
        Insets insets = new Insets(top, left, bottom, right);
        UIManager.put("TabbedPane.contentBorderInsets", insets);
        JTabbedPane tabPane = new JTabbedPane(orient);
        UIManager.put("TabbedPane.contentBorderInsets", oldInsets);
        return tabPane;
    }



    /**
     * This creates a JButton and a JSlider. The intent is that the button can be placed in
     * some GUI. On click a small, decorationless modeful dialog that contains the slider is popped
     * up. Focus is set on the slider and a return or escape or press of the close button closes the dialog.
     * slider events are routed to the change listener.
     *
     * @param min slider min value
     * @param max slider max value
     * @param value slider value
     * @param listener slider change listener
     * @return a 2 element array containing the button and the slider
     */
    public static JComponent[] makeSliderPopup(final int min, final int max,
            final int value, final ChangeListener listener) {
        final JButton btn = getImageButton("/auxdata/ui/icons/Slider16.gif",
                                           GuiUtils.class);
        makeMouseOverBorder(btn);
        btn.setToolTipText("Change the Value");
        final JSlider   slider      = new JSlider(min, max, value);
        final JDialog[] dialogArray = { null };
        slider.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {}
            public void focusLost(FocusEvent e) {
                if (dialogArray[0] != null) {
                    dialogArray[0].dispose();
                }
                dialogArray[0] = null;
            }
        });
        KeyListener keyListener = new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if ((ke.getKeyCode() == KeyEvent.VK_ENTER)
                        || (ke.getKeyCode() == KeyEvent.VK_ESCAPE)) {
                    if (dialogArray[0] != null) {
                        dialogArray[0].dispose();
                    }
                    dialogArray[0] = null;
                }
            }
        };
        slider.addKeyListener(keyListener);
        slider.addChangeListener(listener);

        btn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                dialogArray[0] = createDialog(getWindow(btn), "", false);
                JDialog d = dialogArray[0];
                dialogArray[0].setUndecorated(true);
                JButton closeBtn =
                    getImageButton("/auxdata/ui/icons/cancel.gif",
                                   GuiUtils.class);
                makeMouseOverBorder(closeBtn);
                closeBtn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        if (dialogArray[0] != null) {
                            dialogArray[0].dispose();
                        }
                        dialogArray[0] = null;
                    }
                });
                JComponent panel = leftCenter(GuiUtils.top(closeBtn), slider);
                panel = inset(panel, 1);
                panel.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
                        Color.black));
                d.getContentPane().add(panel);
                d.pack();
                Msg.translateTree(d);
                Point loc = btn.getLocationOnScreen();
                loc.y += btn.getSize().height;
                d.setLocation(loc);
                slider.requestFocus();
                d.setVisible(true);
            }
        });

        return new JComponent[] { btn, slider };
    }



    /**
     * Position and fit a window to the screen
     *
     * @param window  window to fit
     * @param bounds  new bounds
     */
    public static void positionAndFitToScreen(Window window,
            Rectangle bounds) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int       diff;
        diff = (bounds.x + bounds.width) - screenSize.width;
        if (diff > 0) {
            bounds.x -= Math.min(diff, bounds.x);
            diff     = screenSize.width - (bounds.x + bounds.width);
            if (diff > 0) {
                bounds.width -= diff;
            }
        }
        diff = (bounds.y + bounds.height) - screenSize.height;
        if (diff > 0) {
            bounds.y -= Math.min(diff, bounds.y);
            diff     = screenSize.height - (bounds.y + bounds.height);
            if (diff > 0) {
                bounds.height -= diff;
            }
        }
        window.setBounds(bounds);
    }



    /**
     * Walk the tree and set any heavyweight components visibility.
     * Note: We don't turn off any component that is under the javax.swing package
     * If we encounter a JTabbedPane then only show the components
     * that are in the selected tab
     *
     * @param comp Component
     * @param visible On/off
     */
    public static void toggleHeavyWeightComponents(Component comp,
            boolean visible) {
        if ( !(comp instanceof JComponent)) {
            if ( !comp.getClass().getName().startsWith("javax.swing")) {
                comp.setVisible(visible);
            }
        }
        if ( !(comp instanceof Container)) {
            return;
        }
        Container cont = (Container) comp;
        if (visible && (cont instanceof JTabbedPane)) {
            checkHeavyWeightComponents((JTabbedPane) cont);
            return;
        }

        Component[] comps = cont.getComponents();
        for (int i = 0; i < comps.length; i++) {
            Component child = comps[i];
            toggleHeavyWeightComponents(child, visible);
        }
    }

    /**
     * Walk the components of the tab and toggle the heavyweight components visiblity
     *
     * @param tab tab
     */
    public static void checkHeavyWeightComponents(JTabbedPane tab) {
        Component[] comps       = tab.getComponents();
        int         selectedIdx = tab.getSelectedIndex();
        for (int i = 0; i < comps.length; i++) {
            toggleHeavyWeightComponents(comps[i], i == selectedIdx);
        }
    }


    /**
     * Walk the components of the tab and toggle the heavyweight components visiblity
     *
     * @param tab tab
     */
    public static void resetHeavyWeightComponents(JTabbedPane tab) {
        Component[] comps       = tab.getComponents();
        int         selectedIdx = tab.getSelectedIndex();
        for (int i = 0; i < comps.length; i++) {
            toggleHeavyWeightComponents(comps[i], true);
        }
    }


    /**
     * Add a change listener to the tab that toggles on any heavy weight components
     * in the selected tab and turns off any in the non-selected tabs.
     *
     * @param tab tab
     */
    public static void handleHeavyWeightComponentsInTabs(
            final JTabbedPane tab) {
        tab.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                checkHeavyWeightComponents(tab);
            }
        });
    }





    /**
     * test
     *
     * @param args args
     *
     * @throws Exception on badness
     */
    public static void main(String[] args) throws Exception {
        JTextField     fld   = new JTextField("", 20);
        JTextComponent tarea = new JTextArea("", 10, 10);
        addKeyBindings(fld);
        addKeyBindings(tarea);
        Action[] actions = tarea.getActions();
        for (int i = 0; i < actions.length; i++) {
            //            System.err.println(actions[i]);
        }
        showOkCancelDialog(null, "", vbox(fld, tarea), null);
        if (true) {
            return;
        }


        /*        Locale list[] = java.text.DateFormat.getAvailableLocales();
                  for (int i=0;i<list.length;i++) {
                  System.out.println(list[i].toString());
                  }*/

        Locale.setDefault(Locale.CHINESE);
        String[] fontnames =
            GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames(Locale.CHINESE);
        Font[] allfonts =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        int    fontcount     = 0;
        String chinesesample = "\u4e00";
        for (int j = 0; j < fontnames.length; j++) {
            //            System.err.println ("font:" + fontnames[j]);
            //      if (allfonts[j].canDisplayUpTo(chinesesample) == -1) { 
            //          chinesefonts.add(allfonts[j].getFontName());
            //      }
            //              fontcount++;
        }


        //        System.err.println("chinese fonts:" + chinesefonts);


        JLabel lbl =
            new JLabel(
                "Here are the characters: \u7834\u70C2\u7269\u7A0B\u5E8F\u5458");
        lbl.setFont(lbl.getFont().deriveFont(24.0f));
        System.err.println(lbl.getFont().getFamily(Locale.CHINESE));

        showOkCancelDialog(null, "", inset(lbl, 20), null);
        if (true) {
            return;
        }



        //        JSlider sb = new JSlider(JSlider.HORIZONTAL);
        //        sb.setPreferredSize(new Dimension(200, 10));
        List        l  = new ArrayList();
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < 5; i++) {
            JToggleButton btn = new JToggleButton("Button " + i);
            l.add(btn);
            bg.add(btn);
        }
        JComponent contents = GuiUtils.vbox(l);

        JFrame     f        = new JFrame();
        f.getContentPane().add(contents);
        f.pack();
        f.setLocation(100, 100);
        f.setVisible(true);




    }

    /**
     * Make a button that pops up a file browser and sets the text of the given field with the selected file
     *
     * @param fld Field to set
     *
     * @return The button
     */
    public static JButton makeFileBrowseButton(final JTextComponent fld) {
        return makeFileBrowseButton(fld, null);
    }

    /**
     * Make a button that pops up a file browser and sets the text of the given field with the selected file
     *
     * @param fld Field to set
     * @param filters File filters. May be null.
     *
     * @return The button
     */
    public static JButton makeFileBrowseButton(final JTextComponent fld,
            final List filters) {
        return makeFileBrowseButton(fld, false, filters);
    }

    /**
     * Make a button that pops up a file browser and sets the text of the given field with the selected file
     *
     * @param fld Field to set
     * @param chooseDirectory Select a directory
     * @param filters File filters. May be null.
     *
     * @return The button
     */
    public static JButton makeFileBrowseButton(final JTextComponent fld,
            final boolean chooseDirectory, final List filters) {
        JButton browseButton = new JButton((chooseDirectory
                                            ? "Select Directory..."
                                            : "Select File..."));
        browseButton.setToolTipText((chooseDirectory
                                     ? "Choose a directory"
                                     : "Choose a file from disk"));
        browseButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                File f = new File(fld.getText());
                f = f.getParentFile();
                JFileChooser chooser = (((f != null) && f.exists())
                                        ? new JFileChooser(f)
                                        : new JFileChooser());

                if (chooseDirectory) {
                    chooser.setFileFilter(
                        new javax.swing.filechooser.FileFilter() {
                        public boolean accept(File f) {
                            return f.isDirectory();
                        }

                        public String getDescription() {
                            return "Directories";
                        }
                    });

                    chooser.setAcceptAllFileFilterUsed(false);
                    chooser.setFileSelectionMode(
                        JFileChooser.DIRECTORIES_ONLY);
                } else {
                    if (filters != null) {
                        for (int i = 0; i < filters.size(); i++) {
                            chooser.addChoosableFileFilter((javax.swing
                                .filechooser.FileFilter) filters.get(i));
                        }
                        if (filters.size() > 0) {
                            chooser.setFileFilter((javax.swing.filechooser
                                .FileFilter) filters.get(0));
                        }
                    }
                }
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    fld.setText(chooser.getSelectedFile().toString());
                }
            }
        });
        return browseButton;

    }



    /**
     * Make a font selector box
     *
     * @param f Font to select in the box
     *
     * @return The box
     */
    public static JComboBox doMakeFontBox(Font f) {
        Font[] fonts =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
        List   fontList = new ArrayList();
        String theName  = null;
        if (f != null) {
            theName = f.getName().toLowerCase();
        }
        TwoFacedObject selected = null;
        for (int i = 0; i < fonts.length; i++) {
            String name = fonts[i].getName();
            if (name.length() > 24) {
                name = name.substring(0, 23) + "...";
            }
            TwoFacedObject tfo      = new TwoFacedObject(name, fonts[i]);
            String         fontName = fonts[i].getName().toLowerCase();
            if ((selected == null) && (theName != null)
                    && (theName.equalsIgnoreCase(fonts[i].getName())
                        || fontName.startsWith(theName + "."))) {
                selected = tfo;
            }
            fontList.add(tfo);
        }
        JComboBox fontBox = new JComboBox();
        GuiUtils.setListData(fontBox, fontList);
        if (selected != null) {
            fontBox.setSelectedItem(selected);
        }
        return fontBox;
    }


    /**
     * Make a box for setting the font size
     *
     * @param size Selected size
     *
     * @return The box
     */
    public static JComboBox doMakeFontSizeBox(int size) {
        JComboBox fontSizeBox = new JComboBox(GuiUtils.getFontSizeList());
        fontSizeBox.setSelectedItem(new Integer(size));
        return fontSizeBox;
    }



    /**
     * Italicize the font on the given component
     *
     * @param comp The component
     *
     * @return the component
     */
    public static Component italicizeFont(Component comp) {
        if (comp != null) {
            Font f = comp.getFont();
            if (f != null) {
                comp.setFont(f.deriveFont(Font.ITALIC));
            }
        }
        return comp;
    }


    /**
     * Class CardLayoutPanel is a utility that does a card layou of its components.
     * It keeps the CardLayout object around and allows for easy calls to show without
     * keeping track of the String layout  key.
     * It is also used in the showComponentInTabs calls to show components in the card layout.
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.317 $
     */
    public static class CardLayoutPanel extends JPanel {

        /** counter for unique keys */
        int counter = 0;

        /** The layout */
        CardLayout cardLayout;

        /** maps component to key */
        Hashtable map = new Hashtable();

        /** key map */
        Hashtable keyMap = new Hashtable();

        /**
         * ctor
         */
        public CardLayoutPanel() {
            this(new CardLayout());
        }

        /**
         * ctor
         *
         * @param cardLayout the layout to use
         */
        public CardLayoutPanel(CardLayout cardLayout) {
            super(cardLayout);
            this.cardLayout = cardLayout;
        }

        /**
         * Get the layout
         *
         * @return The layout
         */
        public CardLayout getCardLayout() {
            return cardLayout;
        }

        /**
         * Get the component that is visible
         *
         * @return visible component
         */
        public Component getVisibleComponent() {
            for (int i = 0; i < getComponentCount(); i++) {
                Component child = getComponent(i);
                if (child.isVisible()) {
                    return (Component) child;
                }
            }
            return null;
        }


        /**
         * Add the component. Generate and return a unique layout key
         *
         * @param comp the component
         *
         * @return Where its added in the card layout
         */
        public String addCard(Component comp) {
            String layoutKey = "component_" + (counter++);
            add(layoutKey, comp);
            return layoutKey;
        }

        /**
         * add the component. This overrides the base class method to store the key to component mapping
         *
         * @param layoutKey layout key
         * @param comp component
         *
         * @return The component
         */
        public Component add(String layoutKey, Component comp) {
            keyMap.put(layoutKey, layoutKey);
            map.put(comp, layoutKey);
            return super.add(layoutKey, comp);
        }

        /**
         * Show the component  identified by the key
         *
         * @param key the key
         */
        public void show(String key) {
            cardLayout.show(this, key);
        }

        /**
         * Show the card
         *
         * @param i  card index
         */
        public void show(int i) {
            show(getComponent(i));
        }

        /**
         * Get the visible index
         *
         * @return  the index
         */
        public int getVisibleIndex() {
            int cnt = getComponentCount();
            for (int i = 0; i < cnt; i++) {
                Component child = getComponent(i);
                if (child.isVisible()) {
                    return i;
                }
            }
            return -1;
        }


        /**
         *  flip to the next component
         */
        public void flip() {
            int cnt = getComponentCount();
            for (int i = 0; i < cnt; i++) {
                Component child = getComponent(i);
                if (child.isVisible()) {
                    if (i < cnt - 1) {
                        show(getComponent(i + 1));
                    } else {
                        show(getComponent(0));
                    }
                    break;
                }
            }
        }

        /**
         * Does this contain the component
         *
         * @param comp  the component
         *
         * @return  true if it's there
         */
        public boolean contains(Component comp) {
            return map.get(comp) != null;
        }

        /**
         * Look for a key
         *
         * @param object key object
         *
         * @return  true if found
         */
        public boolean containsKey(Object object) {
            return keyMap.get(object) != null;
        }

        /**
         * Show the component
         *
         * @param comp the component
         */
        public void show(Component comp) {
            cardLayout.show(this, (String) map.get(comp));
        }

        /**
         * Overwrite base class method to clear the component to key map
         */
        public void removeAll() {
            super.removeAll();
            map = new Hashtable();
        }

        /**
         * Overwrite base class method to clear the component to key map
         *
         * @param comp the component
         */
        public void remove(Component comp) {
            super.remove(comp);
            Object key = map.get(comp);
            if (key != null) {
                keyMap.remove(key);
            }
            map.remove(comp);
        }
    }

    /**
     * This pops up a menu near the given comp and allows the user to
     * select a unit name which gets put into the given fld
     *
     * @param fld The fld to set
     * @param comp THe component to popup the menu near
     */
    public static void popupUnitMenu(final JTextField fld, JComponent comp) {
        //unitGroups is <category1, semi-colon delimited list of units, category2, list of units, etc. >
        //J-
        String[] unitGroups = {
            "Date/Time",
            "yyyy-MM-dd HH:mm:ss;MM/dd/yy HH:mm z;dd.MM.yy HH:mm z;yyyy-MM-dd;EEE, MMM dd yyyy HH:mm z;HH:mm:ss;HH:mm;yyyy-MM-dd'T'HH:mm:ss'Z';yyyy-MM-dd'T'HH:mm:ssz;yyyy-MM-dd'T'HH:mm:ssZ",
            "Geo-spatial",
            "degree;degree_west",
            "Distance",
            "foot;kilometer;meter;mile",
            "Temperature",
            "Celsius;Kelvin;Fahrenheit",
            "Speed",
            "m/sec;mi/hr;km/hr;furlong/fortnight",
            "Misc",
            "Text"
        };
        //J+
        List menus = new ArrayList();
        for (int groupIdx = 0; groupIdx < unitGroups.length; groupIdx += 2) {
            String name  = unitGroups[groupIdx];
            List   units = StringUtil.split(unitGroups[groupIdx + 1], ";");
            List   items = new ArrayList();
            for (int i = 0; i < units.size(); i++) {
                items.add(GuiUtils.makeMenuItem(units.get(i).toString(), fld,
                        "setText", units.get(i).toString()));
            }
            menus.add(GuiUtils.makeMenu(name, items));
        }
        GuiUtils.showPopupMenu(menus, comp);
    }




    /**
     * Do we show icons in the menus
     *
     * @return show icons in menus
     */
    public static boolean getIconsInMenus() {
        return setIconsInMenus;
    }

    /**
     * Do we show icons in the menus
     *
     * @param doIcons show icons
     */
    public static void setIconsInMenus(boolean doIcons) {
        setIconsInMenus = doIcons;
    }


    /**
     * Set the icon on the button. This button is usually a JMenu or JMenuItem
     * If the setIconsInMenus flag is false then don't do this
     *
     * @param button The button
     * @param iconPath The icon path
     *
     * @return Just return the button so you can do something like menu.add(GuiUtils.setIcon(menuItem,"/icon path"));
     */
    public static AbstractButton setIcon(AbstractButton button,
                                         String iconPath) {
        if (setIconsInMenus) {


            button.setIcon(getScaledImageIcon(iconPath, GuiUtils.class,
                    true));
            button.setIconTextGap(2);
        }
        return button;
    }



    /**
     * _more_
     *
     * @param list _more_
     * @param selected _more_
     */
    public static void setSelectedItems(JList list, List selected) {
        List  items   = getItems(list);
        int   cnt     = 0;
        int[] indices = new int[items.size()];
        for (int i = 0; i < selected.size(); i++) {
            int idx = items.indexOf(selected.get(i));
            if (idx < 0) {
                continue;
            }
            indices[cnt++] = idx;
        }
        if (cnt > 0) {
            int[] realIndices = new int[cnt];
            for (int i = 0; i < cnt; i++) {
                realIndices[i] = indices[i];
            }
            list.setSelectedIndices(realIndices);
        }
    }


    /**
     * _more_
     *
     * @param list _more_
     *
     * @return _more_
     */
    public static List getItems(JList list) {
        List      items = new ArrayList();
        ListModel model = list.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            items.add(model.getElementAt(i));
        }
        return items;
    }



    /**
     * _more_
     *
     * @param n _more_
     * @param local _more_
     *
     * @return _more_
     */
    public static String getLocalName(String n, boolean local) {
        return getLocalName(n, local, true);
    }

    /**
     * _more_
     *
     * @param n _more_
     * @param local _more_
     * @param addHtml _more_
     *
     * @return _more_
     */
    public static String getLocalName(String n, boolean local,
                                      boolean addHtml) {
        return (addHtml
                ? "<html>"
                : "") + n + (local
                             ? " &lt;<span style=\"color:blue\">local</span>&gt;"
                             : "") + (addHtml
                                      ? "</html>"
                                      : "");
    }


    /**
     * _more_
     *
     * @param fld _more_
     * @param s _more_
     * @param delimiter _more_
     */
    public static void appendText(JTextComponent fld, String s,
                                  String delimiter) {
        String t = fld.getText();
        while (t.endsWith(" ")) {
            t = t.substring(0, t.length() - 1);
        }
        if ((t.length() > 0) && !t.endsWith(delimiter)) {
            t = t + delimiter;
        }
        t = t + s;
        fld.setText(t);
    }



    /**
     * _more_
     *
     * @param comp _more_
     */
    public static void addKeyBindings(final JTextComponent comp) {
        //TODO Make this into a KeyMap
        comp.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if ( !e.isControlDown()) {
                    return;
                }
                int pos = comp.getCaretPosition();

                if (e.getKeyCode() == e.VK_B) {
                    if (comp.getCaretPosition() > 0) {
                        comp.setCaretPosition(pos - 1);
                    }
                    return;
                }
                if (e.getKeyCode() == e.VK_F) {
                    if (comp.getCaretPosition() < comp.getText().length()) {
                        comp.setCaretPosition(pos + 1);
                    }
                    return;
                }
                if (e.getKeyCode() == e.VK_E) {
                    String t      = comp.getText();
                    int    endPos = t.indexOf("\n", pos);
                    if (endPos >= 0) {
                        comp.setCaretPosition(endPos);
                    } else {
                        comp.setCaretPosition(t.length());
                    }
                }

                if ((e.getKeyCode() == e.VK_O)
                        && (comp instanceof JTextArea)) {
                    String t = comp.getText();
                    t = t.substring(0, pos) + "\n" + t.substring(pos);
                    comp.setText(t);
                    comp.setCaretPosition(pos);
                }

                if (false && (comp instanceof JTextArea)
                        && (e.getKeyCode() == e.VK_P)) {
                    String t = comp.getText();
                    char   c;
                    int    cnt = 0;
                    while ((--pos >= 0) && (c = t.charAt(pos)) != '\n') {
                        cnt++;
                    }

                    comp.setCaretPosition(pos - cnt);
                }

                if (e.getKeyCode() == e.VK_K) {
                    String t = comp.getText();
                    if (pos >= t.length()) {
                        return;
                    }
                    int endPos = t.indexOf("\n", pos);
                    if (endPos == pos) {
                        t = t.substring(0, pos) + t.substring(endPos + 1);
                    } else if (endPos > pos) {
                        t = t.substring(0, pos) + "\n"
                            + t.substring(endPos + 1);
                    } else {
                        t = t.substring(0, pos);
                    }
                    comp.setText(t);
                    comp.setCaretPosition(pos);
                }

                if (e.getKeyCode() == e.VK_D) {
                    String t = comp.getText();
                    if (pos >= t.length()) {
                        return;
                    }
                    if (pos > 0) {
                        t = t.substring(0, pos) + t.substring(pos + 1);
                    } else {
                        t = t.substring(pos + 1);
                    }
                    comp.setText(t);
                    if ((pos >= 0) && (pos < t.length())) {
                        comp.setCaretPosition(pos);
                    }
                }
            }
        });
    }

    /**
     * _more_
     *
     * @param parent _more_
     */
    public static void moveSubtreesToTop(DefaultMutableTreeNode parent) {
        boolean gotAny   = false;
        List    children = Misc.toList(parent.children());
        for (int i = 0; (i < children.size()) && !gotAny; i++) {
            DefaultMutableTreeNode child =
                (DefaultMutableTreeNode) children.get(i);
            gotAny = child.getChildCount() > 0;
        }
        if ( !gotAny) {
            return;
        }
        //        for(int i=0;i<children.size();i++) {
        for (int i = children.size() - 1; i >= 0; i--) {
            DefaultMutableTreeNode child =
                (DefaultMutableTreeNode) children.get(i);
            if (child.getChildCount() > 0) {
                moveSubtreesToTop(child);
                parent.remove(child);
                parent.insert(child, 0);
            }
        }
    }

    /**
     * Utility to create a header that is a label and a line.
     *
     * @param label Header label
     *
     * @return Header
     */
    public static JComponent makeHeader(String label) {
        JComponent header = GuiUtils.lLabel(label);
        header = GuiUtils.left(GuiUtils.inset(header,
                new Insets(10, 5, 0, 0)));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                Color.gray.darker()));
        header = GuiUtils.inset(header, new Insets(0, 0, 0, 10));
        return header;
    }



    //Cut-and-pasted from XmlUtil to break the dependency

    /**
     *  Get the given name-d attribute from the given element. If not found
     *  return the dflt argument.
     *
     *  @param element The xml element to look within.
     *  @param name The attribute name.
     *  @param dflt The default value.
     *  @return The attribute value or the dflt if not found.
     */
    public static String getAttribute(Node element, String name,
                                      String dflt) {
        if (element == null) {
            return dflt;
        }
        return getAttribute(element.getAttributes(), name, dflt);
    }

    /**
     * _more_
     *
     * @param attrs _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static String getAttribute(NamedNodeMap attrs, String name,
                                      String dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        return ((n == null)
                ? dflt
                : n.getNodeValue());
    }

    /**
     * _more_
     *
     * @param attrs _more_
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    public static boolean getAttribute(NamedNodeMap attrs, String name,
                                       boolean dflt) {
        if (attrs == null) {
            return dflt;
        }
        Node n = attrs.getNamedItem(name);
        return ((n == null)
                ? dflt
                : new Boolean(n.getNodeValue()).booleanValue());
    }

    /**
     * _more_
     *
     * @param attrs _more_
     * @param name _more_
     *
     * @return _more_
     */
    public static String getAttribute(NamedNodeMap attrs, String name) {
        String value = getAttribute(attrs, name, (String) null);
        if (value == null) {
            throw new IllegalArgumentException(
                "Could not find xml attribute:" + name);
        }
        return value;
    }

    /**
     * _more_
     *
     * @param parent _more_
     * @param tag _more_
     *
     * @return _more_
     */
    public static List findChildren(Node parent, String tag) {
        ArrayList found    = new ArrayList();
        NodeList  children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ((tag == null) || tag.equals("*")
                    || child.getNodeName().equals(tag)) {
                found.add(child);
            }
        }
        return found;
    }

    /**
     * Class ProgressDialog _more_
     *
     *
     * @author IDV Development Team
     */
    public static class ProgressDialog extends JDialog {

        /** _more_ */
        private JLabel statusLbl;

        /** _more_ */
        private boolean cancelPressed = false;

        /**
         * _more_
         *
         * @param title _more_
         */
        public ProgressDialog(String title) {
            this(title, false);
        }

        /**
         * _more_
         *
         * @param title _more_
         * @param doCancel _more_
         */
        public ProgressDialog(String title, boolean doCancel) {
            super((JFrame) null, title, false);
            statusLbl = new JLabel(" ");
            statusLbl.setMinimumSize(new Dimension(350, 20));
            statusLbl.setPreferredSize(new Dimension(350, 20));
            JLabel waitLbl =
                new JLabel(getImageIcon("/ucar/unidata/util/wait.gif"));
            JComponent contents =
                GuiUtils.inset(GuiUtils.centerRight(statusLbl, waitLbl), 5);
            if (doCancel) {
                JButton btn = new JButton("Cancel");
                btn.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        cancelPressed = true;
                        setText("Cancelling");
                    }
                });
                contents = GuiUtils.centerBottom(contents,
                        GuiUtils.inset(GuiUtils.wrap(btn), 5));
            }
            this.getContentPane().add(contents);
            this.pack();
            this.setLocation(200, 200);
            this.show();
        }

        /**
         * _more_
         *
         * @param lbl _more_
         */
        public void setText(String lbl) {
            statusLbl.setText(lbl);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isCancelled() {
            return cancelPressed;
        }
    }


    /**
     * Get an integer value from the text field
     * @param fld text field
     * @return integer value shown
     */
    public static int getInt(JTextField fld) {
        return (int) getValue(fld);
    }


    /**
     * Get an double value from the text field
     * @param fld text field
     * @return double value shown
     */
    public static double getValue(JTextField fld) {
        return Misc.parseValue(fld.getText().trim());
    }


    /*
     * Set the global application title
     *
     * @param title The title
     */

    /**
     * _more_
     *
     * @param title _more_
     */
    public static void setApplicationTitle(String title) {
        applicationTitle = title;
    }


    /*
     * Get the global application title
     *
     * @return The title
     */

    /**
     * _more_
     *
     * @return _more_
     */
    public static String getApplicationTitle() {
        return applicationTitle;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public static boolean doMacMenubar() {
        if (true) {
            return false;
        }
        return isMac();
    }


    /**
     * Is the given event a control (or meta for the mac) key
     *
     * @param event _more_
     *
     * @return _more_
     */
    public static boolean isControlKey(InputEvent event) {
        if (!isMac()) {
            return event.isControlDown();
        }
        //Don't do this for now
        return event.isControlDown();
        //        return event.isMetaDown();
    }


    public static boolean isControlKey(KeyEvent event, int keyCode) {
        if(!isControlKey(event)) return false;
        return event.getKeyCode() == keyCode;
    }


    /**
     * Is this running on a Mac?
     *
     * @return true if running on Mac
     */
    public static boolean isMac() {
        String os = System.getProperty("os.name");
        if ((os != null) && (os.toLowerCase().indexOf("mac") >= 0)) {
            return true;
        }
        return false;
    }

    /**
     * _more_
     *
     * @param e _more_
     *
     * @return _more_
     */
    public static boolean isDeleteEvent(KeyEvent e) {
        return ((e.getKeyCode() == KeyEvent.VK_DELETE)
                || (GuiUtils.isMac() && (e.getKeyCode() == 8))
                || ((e.getKeyCode() == KeyEvent.VK_D) && e.isControlDown()));
    }


    /**
     * _more_
     *
     * @param frame _more_
     * @param menuBar _more_
     */
    public static void decorateFrame(JFrame frame, JMenuBar menuBar) {
        if ((frame != null & menuBar != null) && doMacMenubar()) {
            frame.setJMenuBar(menuBar);
        }
    }


    /**
     * _more_
     *
     * @param s _more_
     *
     * @throws Exception _more_
     */
    public static void showUrl(String s) throws Exception {
        URI url = new URI(s);
        if ( !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            System.err.println("Browse not supported");
            return;
        }
        Desktop.getDesktop().browse(url);
    }

    /**
     * _more_
     *
     * @param editor _more_
     */
    public static void addLinkListener(JEditorPane editor) {
        editor.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    String url = e.getDescription();
                    try {
                        showUrl(url);
                    } catch (Exception exc) {
                        LogUtil.logException("error showing url:" + url, exc);

                    }
                }
            }
        });

    }

}
