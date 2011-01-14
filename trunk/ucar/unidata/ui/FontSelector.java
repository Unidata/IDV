/*
 * $Id: FontSelector.java,v 1.5 2007/07/06 20:45:30 jeffmc Exp $
 *
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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
import java.awt.font.*;
import java.awt.geom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;


/**
 * A widget for selecting a font.
 */
public class FontSelector implements ItemListener, ListSelectionListener {

    /** Flag for combo box UI */
    public static final int COMBOBOX_UI = 0;

    /** Flag for a List UI */
    public static final int LIST_UI = 1;

    /** List of Styles */
    public static final String[] styles = new String[] { "Plain", "Bold",
            "Italic", "Bold & Italic" };

    /** Default name */
    public static final String DEFAULT_NAME = "Default";

    /** Default name */
    public static final Font DEFAULT_FONT = new Font(null, Font.PLAIN, 12);

    /** Sizing for the combobox */
    private static final String MAX_TEXT = "XXXXXXXXXXXXXXXXXXX";

    /** UI type for this instance */
    private int uiType = COMBOBOX_UI;

    /** flag for showing labels */
    private boolean showLabels;

    /** flag for showing the sample text */
    private boolean showSample;

    /** the component */
    private JComponent component;

    /** selected font */
    private String fontChoice;

    /** selected size */
    private int sizeChoice;

    /** selected style */
    private int styleChoice;

    /** sample text panel */
    private FontPanel sample;

    /** the component for selecting the font name */
    private JComponent fontSelector;

    /** the component for selecting the font style */
    private JComponent styleSelector;

    /** the component for selecting the font size */
    private JComponent sizeSelector;

    /**
     * Create a Font selector using the defaults.
     */
    public FontSelector() {
        this(COMBOBOX_UI, true, false);
    }

    /**
     * Create a font selector
     *
     * @param uiType  UI type
     * @param showLabels  true to show labels
     * @param showSample  true to show sample text in the selected font
     */
    public FontSelector(int uiType, boolean showLabels, boolean showSample) {

        this.uiType     = uiType;
        this.showLabels = showLabels;
        this.showSample = showSample;

        GraphicsEnvironment gEnv =
            GraphicsEnvironment.getLocalGraphicsEnvironment();
        String envfonts[] = gEnv.getAvailableFontFamilyNames();
        Vector fonts      = new Vector();
        for (int i = 1; i < envfonts.length; i++) {
            fonts.addElement(envfonts[i]);
        }
        if ( !fonts.contains(DEFAULT_NAME)) {
            fonts.insertElementAt(DEFAULT_NAME, 0);
        }
        JComboBox box;
        JList     list;

        if (uiType == COMBOBOX_UI) {
            box = new JComboBox(fonts);
            box.setMaximumRowCount(9);
            Dimension d     = box.getPreferredSize();
            int       width = 6 * d.height;
            GuiUtils.setPreferredWidth(box, Math.min(d.width, width));
            box.addItemListener(this);
            fontSelector = box;
        } else {
            list = new ChooserList();
            list.setListData(fonts);
            list.addListSelectionListener(this);
            fontSelector = list;
        }

        Vector sizes = GuiUtils.getFontSizeList();
        if (uiType == COMBOBOX_UI) {
            box = new JComboBox(sizes);
            box.setMaximumRowCount(9);
            box.addItemListener(this);
            sizeSelector = box;
        } else {
            list = new ChooserList();
            list.setListData(sizes);
            list.addListSelectionListener(this);
            sizeSelector = list;
        }

        if (uiType == COMBOBOX_UI) {
            box = new JComboBox(styles);
            box.setMaximumRowCount(9);
            box.addItemListener(this);
            styleSelector = box;
        } else {
            list = new ChooserList();
            list.setListData(styles);
            list.addListSelectionListener(this);
            styleSelector = list;
        }

        sample = new FontPanel();
        sample.setBackground(Color.white);

        List comps = new ArrayList();

        if (showLabels) {
            comps.add(new JLabel("Font:"));
            comps.add(new JLabel("Style:"));
            comps.add(new JLabel("Size:"));
        }
        if (uiType == COMBOBOX_UI) {
            comps.add(fontSelector);
            comps.add(styleSelector);
            comps.add(sizeSelector);
        } else {
            comps.add(((ChooserList) fontSelector).getScroller());
            comps.add(((ChooserList) styleSelector).getScroller());
            comps.add(((ChooserList) sizeSelector).getScroller());
        }
        component = GuiUtils.doLayout(comps, 3, GuiUtils.WT_YNN,
                                      GuiUtils.WT_N);
        if (showSample) {
            component = GuiUtils.topCenter(component, sample);
        }
    }

    /**
     * Set the font name
     *
     * @param fontName the name of the font family
     */
    public void setFontName(String fontName) {
        if (uiType == COMBOBOX_UI) {
            ((JComboBox) fontSelector).setSelectedItem(fontName);
        } else {
            ((JList) fontSelector).setSelectedValue(fontName, true);
        }
    }

    /**
     * Set the Font style (Font.PLAIN, Font.ITALIC, etc);
     *
     * @param fontStyle  style of the font.
     */
    public void setFontStyle(int fontStyle) {
        if (uiType == COMBOBOX_UI) {
            ((JComboBox) styleSelector).setSelectedIndex(fontStyle);
        } else {
            ((JList) styleSelector).setSelectedIndex(fontStyle);
        }
    }

    /**
     * Set the font size
     *
     * @param fontSize size of the font
     */
    public void setFontSize(int fontSize) {
        if (uiType == COMBOBOX_UI) {
            ((JComboBox) sizeSelector).setSelectedItem(new Integer(fontSize));
        } else {
            ((JList) sizeSelector).setSelectedValue(new Integer(fontSize),
                    true);
        }
    }

    /**
     * Get the font family name
     *
     * @return name of the font family
     */
    public String getFontName() {
        return fontChoice;
    }

    /**
     * Get the font style (Font.PLAIN, Font.BOLD, etc);
     *
     * @return font style
     */
    public int getFontStyle() {
        return styleChoice;
    }

    /**
     * Get the font size
     *
     * @return the font size
     */
    public int getFontSize() {
        return sizeChoice;
    }

    /**
     * Get the component
     *
     * @return the UI component
     */
    public JComponent getComponent() {
        return component;
    }

    /**
     * Get the font defined by the widget
     *
     * @return the font.
     */
    public Font getFont() {
        return new Font(fontChoice, styleChoice, sizeChoice);
    }

    /**
     * Set the font defined by the widget
     *
     * @param f  the font.
     */
    public void setFont(Font f) {
        if (f == null) {
            f = DEFAULT_FONT;
        }
        String name = f.getName();
        if ( !name.equals(DEFAULT_NAME)) {
            name = f.getFamily();
        }
        setFontName(name);
        setFontStyle(f.getStyle());
        setFontSize(f.getSize());
    }

    /**
     * Handle a change to a selection in any of the combo boxes.
     *
     * @param e  event
     */
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() != ItemEvent.SELECTED) {
            return;
        }

        Object list = e.getSource();

        if (list == fontSelector) {
            fontChoice =
                (String) ((JComboBox) fontSelector).getSelectedItem();
        } else if (list == styleSelector) {
            String style =
                (String) ((JComboBox) styleSelector).getSelectedItem();
            for (int i = 0; i < styles.length; i++) {
                if (style.equals(styles[i])) {
                    styleChoice = i;
                    break;
                }
            }
        } else {
            sizeChoice =
                ((Integer) ((JComboBox) sizeSelector).getSelectedItem())
                    .intValue();
        }
        sample.changeFont(fontChoice, styleChoice, sizeChoice);
    }

    /**
     * Handle a change to a selection in any of the lists.
     *
     * @param e  event
     */
    public void valueChanged(ListSelectionEvent e) {

        Object list = e.getSource();

        if (list == fontSelector) {
            fontChoice = (String) ((JList) fontSelector).getSelectedValue();
        } else if (list == styleSelector) {
            String style =
                (String) ((JList) styleSelector).getSelectedValue();
            for (int i = 0; i < styles.length; i++) {
                if (style.equals(styles[i])) {
                    styleChoice = i;
                    break;
                }
            }
        } else {
            sizeChoice =
                ((Integer) ((JList) sizeSelector).getSelectedValue())
                    .intValue();
        }
        sample.changeFont(fontChoice, styleChoice, sizeChoice);
    }

    /**
     * Test this class
     *
     * @param s  not used
     */
    public static void main(String s[]) {
        JFrame f = new JFrame("FontSelection");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        FontSelector fontSelection = new FontSelector();
        //FontSelector fontSelection = new FontSelector(FontSelector.LIST_UI,
        //                                 true, true);
        f.getContentPane().add(fontSelection.getComponent(),
                               BorderLayout.CENTER);
        fontSelection.setFont(fontSelection.DEFAULT_FONT);
        f.pack();
        f.setVisible(true);
    }


    /**
     * A class for displaying a sample font
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.5 $
     */
    class FontPanel extends JPanel {

        /** my font */
        Font thisFont = DEFAULT_FONT;

        /**
         * Create a new FontPanel
         */
        public FontPanel() {}


        /**
         * Resets thisFont to the currently selected fontname, size and style attributes.
         *
         * @param f  font name
         * @param st font style
         * @param si font size
         */
        public void changeFont(String f, int st, int si) {
            Integer newSize = new Integer(si);
            int     size    = newSize.intValue();
            thisFont = new Font(f, st, size);
            repaint();
        }

        /**
         * Paint me
         *
         * @param g  graphics component
         */
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int        w  = getWidth();
            int        h  = getHeight();

            g2.setColor(Color.darkGray);
            g2.setFont(thisFont);
            String      change  = "Sample Text";
            FontMetrics metrics = g2.getFontMetrics();
            int         width   = metrics.stringWidth(change);
            int         height  = metrics.getHeight();
            g2.drawString(change, w / 2 - width / 2, h / 2 - height / 2);
        }
    }
}

