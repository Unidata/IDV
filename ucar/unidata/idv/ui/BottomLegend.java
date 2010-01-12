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

import ucar.unidata.util.GuiUtils;

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
 * This class provides the bottom legend of display controls shown in a ViewManager.
 *
 * @author IDV development team
 */

public class BottomLegend extends IdvLegend {

    /** The current background color of the ViewManager */
    private Color background;

    /** The current foreground color of the ViewManager */
    private Color foreground;


    /**
     * This is the panel that actually holds the legend
     * components from the display controls
     */
    private JPanel legendPanel;


    /**
     * This is the main GUI contents. It holds the scroller that
     * holds the legendPanel
     */
    private JPanel mainContents;



    /** The border around each display control legend component */
    private static Border legendBorder;

    static {
        Border outerSpace = BorderFactory.createEmptyBorder(0, 1, 0, 1);
        Border innerSpace = BorderFactory.createEmptyBorder(1, 2, 1, 2);
        legendBorder = BorderFactory.createCompoundBorder(
            outerSpace,
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.gray, 1), innerSpace));


    }



    /**
     * Create me with the given ViewManager
     *
     * @param viewManager The view manager I am part of
     *
     */
    public BottomLegend(ViewManager viewManager) {
        super(viewManager);
    }


    /**
     * Make the GUI contents.
     *
     * @return The GUI contents
     */
    protected JComponent doMakeContents() {
        legendPanel = new JPanel();
        JScrollPane scroller = GuiUtils.makeScrollPane(legendPanel, 500, 50);
        scroller.setBorder(BorderFactory.createEmptyBorder());
        scroller.setPreferredSize(new Dimension(500, 50));
        mainContents = GuiUtils.center(scroller);
        KeyListener keyListener = new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                viewManager.keyWasTyped(e);
            }
        };
        ObjectListener mouseListener = new ObjectListener(legendPanel) {
            public void mouseClicked(MouseEvent e) {
                ((Component) e.getSource()).requestFocus();
            }
        };
        JComponent[] comps = new JComponent[] { legendPanel, mainContents };
        for (int i = 0; i < comps.length; i++) {
            if (comps[i] == null) {
                continue;
            }
            comps[i].addKeyListener(keyListener);
            comps[i].addMouseListener(mouseListener);
            comps[i].setRequestFocusEnabled(true);
        }

        return mainContents;
    }


    /**
     * Set the colors of the legend.
     *
     * @param foreground The foreground color of the ViewManager
     * @param background The background color of the ViewManager
     */

    public void setColors(Color foreground, Color background) {
        this.foreground = foreground;
        this.background = background;
        mainContents.setBackground(background);
        legendPanel.setBackground(background);
        fillLegend();
    }




    /**
     * Override the case class method to refill the legend. This gets called
     * from within a synchronized block when the display controls we are showing
     * have changed in some way.
     */
    protected void fillLegendSafely() {
        legendPanel.removeAll();
        List controls = viewManager.getControls();
        List comps    = new ArrayList();
        int  cnt      = 0;
        for (int i = controls.size() - 1; i >= 0; i--) {
            DisplayControl control = (DisplayControl) controls.get(i);
            Container controlLegend =
                control.getLegendComponent(control.BOTTOM_LEGEND);
            control.setLegendBackground(background);
            control.setLegendForeground(foreground);
            String legendOrder = " #" + (++cnt) + "  ";
            JLabel ll          = new JLabel(legendOrder);
            ll.setBackground(background);
            ll.setForeground(foreground);

            JComponent legend = GuiUtils.hbox(ll, controlLegend);
            legend.setBackground(background);
            //            legend = GuiUtils.centerRight(legend, makeButtonPanel(control));
            legend.setBackground(background);
            legend.setBorder(legendBorder);
            comps.add(legend);
        }

        JPanel listPanel = GuiUtils.vbox(comps);
        legendPanel.setLayout(new BorderLayout());
        legendPanel.add(listPanel, BorderLayout.NORTH);
        legendPanel.repaint();
        legendPanel.revalidate();

    }



}
