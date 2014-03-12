/*
 * Copyright 1997-2014 Unidata Program Center/University Corporation for
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
import ucar.unidata.ui.drawing.Glyph;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

    /** the font selector */
    FontSelector fontSelector;

    /** the alignment selector */
    JComboBox alignSelector;

    /** the alignment selector */
    JComboBox formatSelector;

    /** the alignment point list */
    private List<TwoFacedObject> alignPoints;

    /** the use360 checkbox */
    private JCheckBox use360Cbx;

    /** The alignment point names */
    private static final String[] RECTPOINTNAMES = {
        "Top Left", "Top Center", "Top Right", "Left", "Center", "Right",
        "Bottom Left", "Bottom Center", "Bottom Right"
    };

    /** list of predefined formats */
    private static final String[] LABEL_FORMATS = {
        "DD", "DD.d", "DD:MM", "DD:MM:SS", "DDH", "DD.dH", "DD:MMH",
        "DD:MM:SSH"
    };

    /**
     * Create a LatLonLabelPanel
     *
     * @param lld Holds the lat lon data
     *
     */
    public LatLonLabelPanel(LatLonLabelData lld) {

        this.latLonLabelData = lld;
        ignoreEvents         = true;
        onOffCbx             = new JCheckBox("",
                                             latLonLabelData.getVisible());
        onOffCbx.setToolTipText("Turn on/off labels");
        onOffCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonLabelData.setVisible(onOffCbx.isSelected());
                }
            }
        });

        spacingField =
            new JTextField(String.valueOf(latLonLabelData.getInterval()), 6);
        spacingField.setToolTipText(
            "Set the interval (degrees) between labels");
        spacingField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonLabelData.setInterval(
                    new Float(spacingField.getText()).floatValue());
            }
        });

        baseField =
            new JTextField(String.valueOf(latLonLabelData.getBaseValue()), 6);
        baseField.setToolTipText("Set the base value for the interval");
        baseField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                latLonLabelData.setBaseValue(
                    new Float(baseField.getText()).floatValue());
            }
        });

        labelLinesField = new JTextField(
            LatLonLabelData.formatLabelLines(
                latLonLabelData.getLabelLines()), 6);
        labelLinesField.setToolTipText(
            "Set the lines to place labels separated by a semicolon (;)");
        labelLinesField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreEvents) {
                    return;
                }
                try {
                    latLonLabelData.setLabelsLineString(
                        labelLinesField.getText());
                } catch (NumberFormatException nfe) {
                    LogUtil.userErrorMessage("Bad format for label lines");
                }
            }
        });

        colorButton = new GuiUtils.ColorSwatch(latLonLabelData.getColor(),
                "Set " + (latLonLabelData.getIsLatitude()
                          ? "Latitude"
                          : "Longitude") + " Color");
        colorButton.setToolTipText("Set the label color");
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
        fastRenderCbx.setToolTipText("Set if labels don't render correctly");
        fastRenderCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonLabelData.setFastRendering(
                        fastRenderCbx.isSelected());
                }
            }
        });

        fontSelector = new FontSelector(FontSelector.COMBOBOX_UI, false,
                                        false);
        fontSelector.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if ( !ignoreEvents) {
                    latLonLabelData.setFont(fontSelector.getFont());
                }
            }
        });
        fontSelector.setFont((Font) latLonLabelData.getFont());

        alignPoints = TwoFacedObject.createList(Glyph.RECTPOINTS,
                Glyph.RECTPOINTNAMES);
        alignSelector = new JComboBox();
        alignSelector.setToolTipText(
            "Set the positioning of the label relative to the location");
        alignSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if ( !ignoreEvents) {
                    latLonLabelData.setAlignment(
                        TwoFacedObject.getIdString(
                            alignSelector.getSelectedItem()));
                }
            }
        });
        GuiUtils.setListData(alignSelector, alignPoints);
        alignSelector.setSelectedItem(
            getAlignSelectorItem(latLonLabelData.getAlignment()));

        formatSelector = new JComboBox(LABEL_FORMATS);
        formatSelector.setEditable(true);
        formatSelector.setToolTipText("Set the label format");
        formatSelector.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if ( !ignoreEvents) {
                    latLonLabelData.setLabelFormat(
                        formatSelector.getSelectedItem().toString());
                }
            }
        });
        //GuiUtils.setListData(formatSelector, LABEL_FORMATS);
        formatSelector.setSelectedItem(latLonLabelData.getLabelFormat());
        ignoreEvents = false;

        use360Cbx    = new JCheckBox("0-360", latLonLabelData.getUse360());
        use360Cbx.setToolTipText(
            "Use 0 to 360 vs. -180 to 180 convention for longitude labels");
        use360Cbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if ( !ignoreEvents) {
                    latLonLabelData.setUse360(use360Cbx.isSelected());
                }
            }
        });
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
            spacingField.setText("" + lld.getInterval());
            baseField.setText("" + lld.getBaseValue());
            labelLinesField.setText(
                "" + LatLonLabelData.formatLabelLines(lld.getLabelLines()));
            colorButton.setBackground(lld.getColor());
            fastRenderCbx.setSelected(lld.getFastRendering());
            alignSelector.setSelectedItem(
                getAlignSelectorItem(lld.getAlignment()));
            if (lld.getFont() != null) {
                fontSelector.setFont((Font) lld.getFont());
            }
            formatSelector.setSelectedItem(lld.getLabelFormat());
            use360Cbx.setSelected(lld.getUse360());
            ignoreEvents = false;
        }

    }


    /**
     * Layout the panels
     *
     * @param latPanel  the lat panel
     * @param lonPanel  the lon panel
     *
     * @return The laid back panels
     */
    public static JPanel layoutPanels(LatLonLabelPanel latPanel,
                                      LatLonLabelPanel lonPanel) {
        Component[] comps = {
            GuiUtils.lLabel("<html><b>Labels</b></html>"), GuiUtils.filler(),
            GuiUtils.cLabel("Interval"), GuiUtils.cLabel("Relative to"),
            GuiUtils.filler(), GuiUtils.filler(), GuiUtils.cLabel("Color"),
            GuiUtils.cLabel("Alignment"), latPanel.onOffCbx,
            GuiUtils.rLabel("Latitude:"), latPanel.spacingField,
            latPanel.baseField, GuiUtils.rLabel("At Longitudes:"),
            latPanel.labelLinesField, latPanel.colorButton,
            latPanel.alignSelector, lonPanel.onOffCbx,
            GuiUtils.rLabel("Longitude:"), lonPanel.spacingField,
            lonPanel.baseField, GuiUtils.rLabel("At Latitudes:"),
            lonPanel.labelLinesField, lonPanel.colorButton,
            lonPanel.alignSelector,
        };
        GuiUtils.tmpInsets = new Insets(2, 4, 2, 4);
        JPanel settings = GuiUtils.doLayout(comps, 8, GuiUtils.WT_N,
                                            GuiUtils.WT_N);
        Component[] extraComps = {
            GuiUtils.rLabel("Font:"), latPanel.fontSelector.getComponent(),
            GuiUtils.rLabel("Format:"), latPanel.formatSelector,
            lonPanel.use360Cbx, GuiUtils.filler()
        };
        GuiUtils.tmpInsets = new Insets(2, 4, 2, 4);
        JPanel extra = GuiUtils.doLayout(extraComps, 5, GuiUtils.WT_N,
                                         GuiUtils.WT_N);
        return GuiUtils.vbox(GuiUtils.left(settings), GuiUtils.left(extra));
    }




    /**
     * Apply any of the state in the gui (e.g., spacing) to the  latLonData
     */
    public void applyStateToData() {
        // need to get the TextField values because people could type in a new value
        // without hitting return.  Other widgets should trigger a change
        latLonLabelData.setInterval(
            new Float(spacingField.getText()).floatValue());
        latLonLabelData.setBaseValue(
            new Float(baseField.getText()).floatValue());
        latLonLabelData.setLabelsLineString(labelLinesField.getText());
    }


    /**
     * Get the latlondata object
     *
     * @return The latlondata object
     */
    public LatLonLabelData getLatLonLabelData() {
        return latLonLabelData;
    }

    /**
     * Get the TwoFacedObject associated with the alignment id
     *
     * @param id  the id
     *
     * @return  the corresponding TFO or null
     */
    private TwoFacedObject getAlignSelectorItem(String id) {
        if (alignPoints == null) {
            return null;
        }
        return TwoFacedObject.findId(id, alignPoints);
    }

}
