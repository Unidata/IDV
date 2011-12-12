/*
 * $Id: LatLonPanel.java,v 1.21 2007/07/06 20:45:31 jeffmc Exp $
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
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.Resource;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.io.*;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.*;

import java.util.ArrayList;

import javax.swing.*;

import javax.swing.event.*;


/**
 * A panel to hold the gui for one lat lon line
 */


public class LatLonPanel extends JPanel {

    /** flag for ignoring events */
    private boolean ignoreEvents = false;


    /** This holds the data that describes the latlon lines */
    private LatLonData latLonData;

    /** The visibility cbx */
    JCheckBox onOffCbx;

    /** The spacing input box */
    JTextField spacingField;

    /** Shows the color */
    //JButton colorButton;
    GuiUtils.ColorSwatch colorButton;

    /** The line width box */
    JComboBox widthBox;

    /** The line style box */
    JComboBox styleBox;

    /** The line style box */
    JCheckBox fastRenderCbx;

    /**
     * Create a LatLonPanel
     *
     * @param lld Holds the lat lon data
     *
     */
    public LatLonPanel(LatLonData lld) {
        this.latLonData = lld;
        onOffCbx        = new JCheckBox("", latLonData.getVisible());
        onOffCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonData.setVisible(onOffCbx.isSelected());
                }
            }
        });
        spacingField =
            new JTextField(String.valueOf(latLonData.getSpacing()), 6);
        spacingField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonData.setSpacing(
                    new Float(spacingField.getText()).floatValue());
            }
        });

        widthBox = new JComboBox(new String[] { "1.0", "1.5", "2.0", "2.5",
                "3.0" });
        widthBox.setMaximumSize(new Dimension(30, 16));
        widthBox.setEditable(true);
        widthBox.setSelectedItem(String.valueOf(latLonData.getLineWidth()));
        widthBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonData
                    .setLineWidth(Float
                        .parseFloat((String) ((JComboBox) e.getSource())
                            .getSelectedItem()));
            }
        });
        styleBox = new JComboBox(new String[] { "_____", "_ _ _", ".....",
                "_._._" });
        styleBox.setMaximumSize(new Dimension(30, 16));
        styleBox.setSelectedIndex(latLonData.getLineStyle());
        Font f = Font.decode("monospaced-BOLD");
        if (f != null) {
            styleBox.setFont(f);
        }
        styleBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonData.setLineStyle(
                    ((JComboBox) e.getSource()).getSelectedIndex());
            }
        });
        colorButton = new GuiUtils.ColorSwatch(latLonData.getColor(),
                "Set " + (latLonData.getIsLatitude()
                          ? "Latitude"
                          : "Longitude") + " Color");
        colorButton.setToolTipText("Set the line color");
        colorButton.addPropertyChangeListener("background",
                new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (ignoreEvents) {
                    return;
                }
                Color c = ((JPanel) evt.getSource()).getBackground();

                if (c != null) {
                    latLonData.setColor(c);
                }
            }
        });
        fastRenderCbx = new JCheckBox("", latLonData.getFastRendering());
        fastRenderCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonData.setFastRendering(fastRenderCbx.isSelected());
                }
            }
        });
    }


    /**
     * Set the information that configures this.
     *
     * @param lld   the latlon data
     */
    public void setLatLonData(LatLonData lld) {
        this.latLonData = lld;
        if (onOffCbx != null) {
            ignoreEvents = true;
            onOffCbx.setSelected(lld.getVisible());
            spacingField.setText("" + lld.getSpacing());
            widthBox.setSelectedItem("" + lld.getLineWidth());
            styleBox.setSelectedIndex(lld.getLineStyle());
            colorButton.setBackground(lld.getColor());
            fastRenderCbx.setSelected(lld.getFastRendering());
            ignoreEvents = false;
        }

    }


    /**
     * Layout the panels
     *
     * @param latPanel  the lat panel
     * @param lonPanel  the lon panel
     *
     * @return The layed out panels
     */
    public static JPanel layoutPanels(LatLonPanel latPanel,
                                      LatLonPanel lonPanel) {
        Component[] comps = {
            GuiUtils.cLabel("Lines"), GuiUtils.cLabel("Visible"),
            GuiUtils.cLabel("Spacing"), GuiUtils.cLabel("Width"),
            GuiUtils.cLabel("Style"), GuiUtils.cLabel("Color"),
            GuiUtils.cLabel("Fast Render"), GuiUtils.rLabel("Latitude:"),
            latPanel.onOffCbx, latPanel.spacingField, latPanel.widthBox,
            latPanel.styleBox, latPanel.colorButton, latPanel.fastRenderCbx,
            GuiUtils.rLabel("Longitude:"), lonPanel.onOffCbx,
            lonPanel.spacingField, lonPanel.widthBox, lonPanel.styleBox,
            lonPanel.colorButton, lonPanel.fastRenderCbx
        };
        GuiUtils.tmpInsets = new Insets(2, 4, 2, 4);
        return GuiUtils.doLayout(comps, 7, GuiUtils.WT_N, GuiUtils.WT_N);
    }




    /**
     * Apply any of the state in the gui (e.g., spacing) to the  latLonData
     */
    public void applyStateToData() {
        // need to get the value because people could type in a new value
        // without hitting return.  Other widgets should trigger a change
        latLonData.setSpacing(new Float(spacingField.getText()).floatValue());
        //latLonData.setColor(colorButton.getSwatchColor());
    }


    /**
     * Get the latlondata object
     *
     * @return The latlondata object
     */
    public LatLonData getLatLonData() {
        return latLonData;
    }


}

