/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
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


import ucar.unidata.gis.maps.LatLonLabelData;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * A panel to hold the gui for one lat lon line
 */


public class LatLonLabelPanel extends JPanel {

    /** flag for ignoring events */
    private boolean ignoreEvents = false;


    /** This holds the data that describes the latlon lines */
    private LatLonLabelData latLonLabelData;

    /** The visibility cbx */
    JCheckBox onOffCbx;

    /** The spacing input box */
    JTextField spacingField;

    /** The base input box */
    JTextField baseField;

    /** The spacing input box */
    JTextField labelLinesField;

    /** Shows the color */
    //JButton colorButton;
    GuiUtils.ColorSwatch colorButton;

    /** The line style box */
    JCheckBox fastRenderCbx;

    /**
     * Create a LatLonLabelPanel
     *
     * @param lld Holds the lat lon data
     *
     */
    public LatLonLabelPanel(LatLonLabelData lld) {
        this.latLonLabelData = lld;
        onOffCbx        = new JCheckBox("", latLonLabelData.getVisible());
        onOffCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonLabelData.setVisible(onOffCbx.isSelected());
                }
            }
        });
        spacingField =
            new JTextField(String.valueOf(latLonLabelData.getIncrement()), 6);
        spacingField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonLabelData.setIncrement(
                    new Float(spacingField.getText()).floatValue());
            }
        });
        
        baseField = new JTextField(String.valueOf(latLonLabelData.getBaseValue()),
                                   6);
        baseField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonLabelData.setBaseValue(
                    new Float(baseField.getText()).floatValue());
            }
        });

        labelLinesField = new JTextField(formatLabelLines(latLonLabelData.getLabelLines()),
                                   6);
        labelLinesField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonLabelData.setLabelLines(parseLabelLineString(labelLinesField.getText()));
            }
        });

        colorButton = new GuiUtils.ColorSwatch(latLonLabelData.getColor(),
                "Set " + (latLonLabelData.getIsLatitude()
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
                    latLonLabelData.setColor(c);
                }
            }
        });
        fastRenderCbx = new JCheckBox("", latLonLabelData.getFastRendering());
        fastRenderCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonLabelData.setFastRendering(fastRenderCbx.isSelected());
                }
            }
        });
    }
    
    private String formatLabelLines(float[] vals) {
    	StringBuffer buf = new StringBuffer();
    	for (int i = 0; i < vals.length; i++) {
    		buf.append(vals[i]);
    		if (i < vals.length-1) {
    			buf.append(";");
    		}
    	}
    	return buf.toString();
    }
    
    private float[] parseLabelLineString(String llString) {
    	return Misc.parseFloats(llString, ";");
    }


    /**
     * Set the information that configures this.
     *
     * @param lld   the latlon data
     */
    public void setLatLonLabelData(LatLonLabelData lld) {
        this.latLonLabelData = lld;
        if (onOffCbx != null) {
            ignoreEvents = true;
            onOffCbx.setSelected(lld.getVisible());
            spacingField.setText("" + lld.getIncrement());
            baseField.setText("" + lld.getBaseValue());
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
    public static JPanel layoutPanels(LatLonLabelPanel latPanel,
                                      LatLonLabelPanel lonPanel) {
        Component[] comps = {
        	GuiUtils.filler(), GuiUtils.cLabel("<b>Labels</b>"),
            GuiUtils.cLabel("Increment"), GuiUtils.cLabel("Base"),
            GuiUtils.filler(),GuiUtils.filler(), GuiUtils.cLabel("Color"),
            latPanel.onOffCbx, GuiUtils.rLabel("Latitude:"), 
            latPanel.spacingField, latPanel.baseField,
            GuiUtils.rLabel("At Longitudes:"), latPanel.labelLinesField,
            latPanel.colorButton,
            lonPanel.onOffCbx, GuiUtils.rLabel("Longitude:"), 
            lonPanel.spacingField, lonPanel.baseField,
            GuiUtils.rLabel("At Latitudes:"), lonPanel.labelLinesField,
            lonPanel.colorButton
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
        latLonLabelData.setIncrement(
            new Float(spacingField.getText()).floatValue());
        //latLonData.setColor(colorButton.getSwatchColor());
    }


    /**
     * Get the latlondata object
     *
     * @return The latlondata object
     */
    public LatLonLabelData getLatLonLabelData() {
        return latLonLabelData;
    }


}
