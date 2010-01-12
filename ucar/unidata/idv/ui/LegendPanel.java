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

package ucar.unidata.idv.ui;


import ucar.unidata.idv.*;



import ucar.unidata.idv.ui.*;


import ucar.unidata.ui.CheckboxCategoryPanel;
import ucar.unidata.ui.DropPanel;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;


import java.awt.*;
import java.awt.event.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * Class LegendPanel provides a generic panel that displays some GUI content
 * and can be closed/opened.
 *
 *
 * @author IDV development team
 */
public class LegendPanel {

    /** The overall contents */
    protected JPanel contents;

    /** For opening/closing */
    private JButton toggleBtn;

    /** The contents that is show/hidden when toggling */
    private JPanel innerPanel;

    /** The label displayed at the top */
    private JComponent topLabel;

    /** If non-null shown at the top to the left of the topLabel */
    private JComponent extraLeft;

    /** If non-null shown at the top to the right of the topLabel */
    protected JComponent extraRight;

    /** Tracks if the inner panel is visible */
    private boolean innerVisible = true;


    /** Should we make the top border */
    private boolean makeTopBorder = true;

    /**
     * Create me with no top components. These can get set later
     * with {@link #setComponents(JComponent, JComponent, JComponent)}
     */
    public LegendPanel() {
        this(null, null, null);
    }

    /**
     * Create me with the given top components
     *
     * @param topLabel Label shown at the top
     * @param extraLeft     If non-null shown at the top to the left of the topLabel
     * @param extraRight   If non-null shown at the top to the right of the topLabel
     *
     */
    public LegendPanel(JComponent topLabel, JComponent extraLeft,
                       JComponent extraRight) {
        this(topLabel, true, extraLeft, extraRight);
    }


    /**
     * Create me with the given top components
     *
     * @param topLabel Label shown at the top
     * @param makeBorder If true then we also create a border
     * @param extraLeft     If non-null shown at the top to the left of the topLabel
     * @param extraRight   If non-null shown at the top to the right of the topLabel
     */
    public LegendPanel(JComponent topLabel, boolean makeBorder,
                       JComponent extraLeft, JComponent extraRight) {
        setComponents(topLabel, extraLeft, extraRight);
        this.makeTopBorder = makeBorder;
        innerPanel         = new JPanel();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getExtraRight() {
        return extraRight;
    }


    /**
     * Set the top components
     *
     * @param topLabel Label shown at the top
     * @param extraLeft     If non-null shown at the top to the left of the topLabel
     * @param extraRight   If non-null shown at the top to the right of the topLabel
     */
    protected void setComponents(JComponent topLabel, JComponent extraLeft,
                                 JComponent extraRight) {
        this.topLabel   = topLabel;
        this.extraLeft  = extraLeft;
        this.extraRight = extraRight;
    }

    /**
     * Create, if needed, and return the overall GUI contents.
     *
     * @return the contents
     */
    public JComponent getContents() {
        if (contents == null) {
            doMakeContents();
        }
        return contents;
    }

    /**
     * Clear all components in the innerPanel
     */
    public void reInitialize() {
        if (innerPanel != null) {
            innerPanel.removeAll();
        }
    }


    /**
     * Clear the inner panel
     */
    public void clear() {
        if (innerPanel != null) {
            innerPanel.removeAll();
        }
    }


    /**
     * For debugging
     *
     * @param comp comp
     * @param tab tab _
     */
    public static void walkMe(JComponent comp, String tab) {
        List l =
            ucar.unidata.util.StringUtil.split(comp.getClass().getName(),
                ".");
        String name = (String) l.get(l.size() - 1);
        String extra = " " + comp.getPreferredSize().width + "/"
                       + comp.getPreferredSize().height;
        if (comp instanceof JLabel) {
            extra = extra + " \"" + ((JLabel) comp).getText() + "\"";
        }

        System.err.println(tab + name + extra);
        for (int i = 0; i < comp.getComponentCount(); i++) {
            JComponent child = (JComponent) comp.getComponent(i);
            walkMe(child, tab + " ");
        }

    }


    /**
     * Create the GUI. Make a topCenter panel where the top
     * is the toggle button,extraLeft,topLabel,extraRight
     * and the center is the innerPanel.
     */
    protected void doMakeContents() {
        MouseAdapter visListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent me) {
                //                walkMe(getContents(),"");
                toggleInnerVisible();
            }
        };

        toggleBtn = GuiUtils.getImageButton(innerVisible
                                            ? CheckboxCategoryPanel
                                            .categoryOpenIcon
                                            : CheckboxCategoryPanel
                                            .categoryClosedIcon);
        toggleBtn.addMouseListener(visListener);
        JComponent togglePanel = GuiUtils.inset(toggleBtn,
                                     new Insets(0, 3, 0, 4));
        int labelHeight = topLabel.getPreferredSize().height;
        if (GuiUtils.checkHeight(labelHeight)) {
            topLabel.setPreferredSize(new Dimension(50, labelHeight));
        }
        JPanel mainPanel = GuiUtils.doLayout(null,
                                             new Component[] { togglePanel,
                extraLeft, topLabel, extraRight }, 4, GuiUtils.WT_NNYN,
                    GuiUtils.WT_N, null, null, new Insets(0, 0, 0, 0));
        if (makeTopBorder) {
            mainPanel.setBorder(new LegendBorder());
        }
        innerPanel.setVisible(innerVisible);
        if (makeDropPanel()) {
            DropPanel dropPanel = new DropPanel() {
                public void handleDrop(Object object) {
                    doDrop(object);
                }
                public boolean okToDrop(Object object) {
                    return dropOk(object);
                }
            };
            dropPanel.add(BorderLayout.CENTER, mainPanel);
            contents = GuiUtils.topCenter(dropPanel, innerPanel);
        } else {
            contents = GuiUtils.topCenter(mainPanel, innerPanel);
        }




    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean makeDropPanel() {
        return false;
    }

    /**
     * _more_
     *
     * @param object _more_
     *
     * @return _more_
     */
    public boolean dropOk(Object object) {
        return false;
    }

    /**
     * _more_
     *
     * @param object _more_
     */
    public void doDrop(Object object) {}

    /**
     * Add the component into the innerPanel.
     *
     * @param c The component to add
     */
    public void add(JComponent c) {
        add(c, true);
    }


    /**
     * Add the component into the innerPanel.
     *
     * @param c The component to add
     * @param doBorder Should  a border get created around the component.
     */
    public void add(JComponent c, boolean doBorder) {
        JPanel wrapper = GuiUtils.inset(c, new Insets(2, 4, 2, 0));
        if (doBorder) {
            if (innerPanel.getComponentCount() > 0) {
                wrapper.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                        Color.gray));
            } else {
                //                    wrapper.setBorder (BorderFactory.createMatteBorder(2,0,0,0,Color.gray));
                //                    wrapper.setBorder (new LegendBorder());
            }
        }
        innerPanel.add(wrapper);
        wrapper.setVisible(true);
    }

    /**
     * Show or hide the inner panel. Change the icon in the toggle button
     * accordingly.
     *
     * @param b Is it visible
     */
    protected void setInnerVisible(boolean b) {
        innerVisible = b;
        if ((toggleBtn == null) || (innerPanel == null)) {
            return;
        }
        if (innerVisible) {
            toggleBtn.setIcon(CheckboxCategoryPanel.categoryOpenIcon);
        } else {
            toggleBtn.setIcon(CheckboxCategoryPanel.categoryClosedIcon);
        }
        innerPanel.setVisible(innerVisible);
        innerPanel.invalidate();
        contents.validate();
    }



    /**
     * Get the InnerVisible property.
     *
     * @return The InnerVisible
     */
    public boolean getInnerVisible() {
        return innerVisible;
    }



    /**
     * Show or hide the inner panel. Change the icon in the toggle button
     * accordingly.
     */
    protected void toggleInnerVisible() {
        setInnerVisible( !innerVisible);
    }






    /**
     * Class LegendBorder. Provides a Border  for the legend.
     *
     *
     * @author IDV development team
     */
    private static class LegendBorder extends MatteBorder {

        /**
         * Create the border.
         */
        public LegendBorder() {
            super(2, 0, 2, 0, Color.black);
        }

        /**
         * Paint the border
         *
         * @param c The component
         * @param g The graphics
         * @param x x position of the component
         * @param y y position of the component
         * @param width Width of component
         * @param height Height of component
         */
        public void paintBorder(Component c, Graphics g, int x, int y,
                                int width, int height) {
            g.setColor(Color.black);
            g.drawLine(x, y, x + width, y);
            g.setColor(Color.white);
            g.drawLine(x, y + 1, x + width, y + 1);
            g.setColor(Color.black);
            g.drawLine(x, y + height - 2, x + width, y + height - 2);
            g.setColor(Color.white);
            g.drawLine(x, y + height - 1, x + width, y + height - 1);
        }

    }




}
