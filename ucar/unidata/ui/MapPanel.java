/*
 * $Id: MapPanel.java,v 1.21 2007/07/06 20:45:31 jeffmc Exp $
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


import ucar.unidata.gis.maps.*;

import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.io.*;

import java.net.URL;

import java.rmi.RemoteException;


import java.util.*;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;

import javax.swing.event.*;


/**
 * Panel to hold map gui items for one map
 */


public class MapPanel extends JPanel {

    /** This holds the data that describes the map */
    private MapData mapData;


    /** The visibility cbx */
    private JCheckBox shownCbx;


    /** The line width box */
    private JComboBox widthBox;

    /** The line style box */
    private JComboBox styleBox;

    /** Shows the color */
    private GuiUtils.ColorSwatch colorButton;

    /** The fast rendering  cbx */
    private JCheckBox fastRenderingCbx;

    /** Do we ignore the setVisibility */
    private boolean ignoreAction = false;

    /** Are we updating the UI */
    private boolean updatingUI = false;

    /**
     * Create the MapPanel with the given MapData
     *
     * @param data The MapData we represent
     *
     */
    public MapPanel(MapData data) {
        mapData = data;
        init();
    }

    /**
     * Set the visibility checkbox to the given value.
     * Ignore any actions fired from that
     *
     * @param visiblity The checkbox value
     */
    public void setVisibility(boolean visiblity) {
        ignoreAction = true;
        shownCbx.setSelected(visiblity);
        ignoreAction = false;
    }

    /**
     *  Create the GUI
     */
    private void init() {
        setLayout(new GridLayout(1, 2));
        setAlignmentY(this.CENTER_ALIGNMENT);
        String name = mapData.getDescription();
        if (name.length() > 30) {
            name = name.substring(0, 29);
        }
        name     = StringUtil.padRight(name, 30);

        shownCbx = new JCheckBox(name, mapData.getVisible());
        Font lblFont = shownCbx.getFont();
        Font monoFont = new Font("Monospaced", lblFont.getStyle(),
                                 lblFont.getSize());
        shownCbx.setFont(monoFont);


        shownCbx.setToolTipText("Toggle visibility");

        shownCbx.setHorizontalAlignment(JCheckBox.LEFT);
        add(shownCbx);
        shownCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (updatingUI) {
                    return;
                }
                if ( !ignoreAction) {
                    mapData.setVisible(shownCbx.isSelected());
                }
            }
        });

        JPanel p = new JPanel();
        p.setAlignmentX(p.RIGHT_ALIGNMENT);
        p.setAlignmentY(p.TOP_ALIGNMENT);
        widthBox = new JComboBox(new String[] { "1.0", "1.5", "2.0", "2.5",
                "3.0" });
        widthBox.setToolTipText("Set the line width");
        //        widthBox.setMaximumSize(new Dimension(30, 16));
        widthBox.setPreferredSize(new Dimension(60, 16));
        widthBox.setEditable(true);
        widthBox.setSelectedItem(String.valueOf(mapData.getLineWidth()));
        widthBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapData.setLineWidth(
                    Float.parseFloat((String) widthBox.getSelectedItem()));
            }
        });
        p.add(GuiUtils.wrap(widthBox));
        styleBox = new JComboBox(new String[] { "_____", "_ _ _", ".....",
                "_._._" });
        styleBox.setMaximumSize(new Dimension(30, 16));
        styleBox.setToolTipText("Set the line style");
        styleBox.setSelectedIndex(mapData.getLineStyle());
        Font f = Font.decode("monospaced-BOLD");
        if (f != null) {
            styleBox.setFont(f);
        }
        styleBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mapData.setLineStyle(styleBox.getSelectedIndex());
            }
        });
        p.add(styleBox);
        colorButton = new GuiUtils.ColorSwatch(mapData.getColor(),
                "Set Map Line Color");
        colorButton.setToolTipText("Set the line color");
        colorButton.addPropertyChangeListener("background",
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                Color c = colorButton.getBackground();
                if (c != null) {
                    mapData.setColor(c);
                }
            }
        });
        p.add(colorButton);

        fastRenderingCbx = new JCheckBox("Fast rendering",
                                         mapData.getFastRendering());
        fastRenderingCbx.setToolTipText("Toggle fast rendering");
        fastRenderingCbx.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                mapData.setFastRendering(fastRenderingCbx.isSelected());
            }
        });


        p.add(fastRenderingCbx);


        add(p);
    }







    /**
     * Apply the mapData state to the GUI widgets
     */
    public void updateUI() {
        if (updatingUI || (colorButton == null)) {
            return;
        }
        updatingUI = true;
        colorButton.setBackground(mapData.getColor());
        fastRenderingCbx.setSelected(mapData.getFastRendering());
        shownCbx.setSelected(mapData.getVisible());
        styleBox.setSelectedIndex(mapData.getLineStyle());
        widthBox.setSelectedItem(String.valueOf(mapData.getLineWidth()));
        updatingUI = false;
    }


    /**
     * Get the GUI components
     *
     * @return a list of the components
     */
    public java.util.List getGuiComponents() {
        java.util.List comps = new ArrayList();
        comps.add(shownCbx);
        comps.add(widthBox);
        comps.add(styleBox);
        comps.add(colorButton);
        comps.add(fastRenderingCbx);
        return comps;
    }


    /**
     * Get the MapData we represent
     *
     * @return The map data
     */
    public MapData getMapData() {
        return mapData;
    }
}

