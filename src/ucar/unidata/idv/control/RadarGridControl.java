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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.metdata.Station;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;

import ucar.visad.display.RadarGrid;
import ucar.visad.quantities.CommonUnits;



import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;
import visad.georef.MapProjection;

import java.awt.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;


import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;

import java.rmi.RemoteException;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import javax.swing.event.*;



/**
 * Display control for a "RadarGrid," radar range-rings plus radial lines.
 *
 * @version $Revision: 1.103 $
 */
public class RadarGridControl extends DisplayControlImpl implements ActionListener {

    /** command property for range ring spacing */
    static final String CMD_RR_SPACING = "rrspacing";

    /** command property for label spacing */
    static final String CMD_LBL_SPACING = "lblspacing";

    /** command property for radial spacing */
    static final String CMD_RAD_SPACING = "radspacing";


    /** command property for range ring color */
    static final String CMD_RR_COLOR = "rrcolor";

    /** command property for label color */
    static final String CMD_LBL_COLOR = "lblcolor";

    /** command property for label color */
    static final String CMD_RAD_COLOR = "radcolor";

    /** command property for range ring color */
    static final String CMD_RR_WIDTH = "rrwidth";

    /** command property for label color */
    static final String CMD_LBL_WIDTH = "lblwidth";

    /** command property for label color */
    static final String CMD_RAD_WIDTH = "radwidth";

    /** command property for range ring visibility */
    static final String CMD_RR_VIS = "rrvis";

    /** command property for label visibility */
    static final String CMD_LBL_VIS = "lblvis";

    /** command property for radial visibility */
    static final String CMD_RAD_VIS = "radvis";

    /** command property for range ring maximum radius */
    static final String CMD_RR_RADIUS = "rrmaximumradius";

    /** command property for station names */
    static final String CMD_STATIONNAMES = "stationnames";

    /** command property for center longitude */
    static final String CMD_CENTER_LON = "center_lon";

    /** command property for center latitude */
    static final String CMD_CENTER_LAT = "center_lat";

    /** command property for setting radial unit */
    private static final String CMD_RAD_UNIT = "rad_unit";

    /** command property for setting radial unit */
    private static final String CMD_LBL_UNIT = "label_unit";

    /** command property for setting radial unit */
    private static final String CMD_RR_UNIT = "rr_unit";

    /** command property for setting radial unit */
    private static final String CMD_RRMAX_UNIT = "rrmax_unit";

    /** The font for the labels */
    private Font labelFont;

    /** the range rings */
    private RadarGrid rangeRings;

    /** Do we reposition when the user clicks */
    private JCheckBox positionAtClickCbx;

    /** default radial color */
    private Color radColor = Color.gray;

    /** default range ring color */
    private Color rrColor = Color.gray;

    /** default label color */
    private Color lblColor = Color.gray;

    /** list of range ring spacings */
    static final Vector rrSpacingList = Misc.toVector(new String[] {
        "1", "2", "5", "10", "15", "20", "25", "50", "100"
    });

    /** list of label spacings */
    static final Vector lblSpacingList = Misc.toVector(new String[] {
        "1", "2", "5", "10", "15", "20", "25", "50", "100", "200"
    });

    /** list of radial spacings */
    static final Vector radSpacingList = Misc.toVector(new String[] {
        "5", "10", "15", "30", "45", "60", "90"
    });

    /** list of maximum radius */
    static final Vector rrMaxRadiusList = Misc.toVector(new String[] {
        "100", "200", "250", "300", "400", "500"
    });

    //TODO: Let user select the Units

    /** list of distance units */
    private static final Vector distanceUnits = Misc.toVector(new Unit[] {
                                                    CommonUnits.KILOMETER,
            CommonUnits.MILE, CommonUnits.NAUTICAL_MILE });

    /** list of radial units */
    private static final Vector radialUnits = Misc.toVector(new Unit[] {
                                                  CommonUnit.degree,
            CommonUnit.radian });

    /** station table name */
    private String stationTableName;


    /** range ring spacing */
    private double rrSpacing = 50;

    /** label spacing */
    private double lblSpacing = 100;

    /** radial spacing */
    private double radSpacing = 30;

    /** maximum range ring radius */
    private double rrMaxRadius = 300;

    /** range ring visible flag */
    private boolean rrVisible = true;

    /** label visible flag */
    private boolean lblVisible = true;

    /** radial visible flag */
    private boolean radVisible = true;

    /** range ring width */
    private int rrWidth = 1;

    /** label width */
    private int lblWidth = 1;

    /** radial width */
    private int radWidth = 1;

    /** range ring unit */
    private Unit rrUnit = CommonUnits.KILOMETER;

    /** range ring unit */
    private Unit rrMaxUnit = CommonUnits.KILOMETER;

    /** label unit */
    private Unit lblUnit = CommonUnits.KILOMETER;

    /** radial unit */
    private Unit radUnit = CommonUnit.degree;

    /** center lat */
    private double lat = Double.NaN;

    /** center lon */
    private double lon = Double.NaN;

    /** label for position info */
    private JLabel positionLabel;

    /** text fields for inputting latitude/longitude */
    private JTextField latField, lonField;

    /** list of stations */
    private List stationList;

    /** station selection combobox */
    private JComboBox stationCbx = null;

    /** station index in list */
    private int stationIdx = 0;

    /** The initial station to use */
    private String initStationLocation;


    /**
     * Default constructor; does nothing. See init() for creation actions.
     * Need to have a parameter-less constructor for the reflection-based
     * object creation in the IDV to call.
     */
    public RadarGridControl() {}


    /**
     * Called to make this kind of Display Control;
     * also makes the Displayable.
     * This method is called from inside
     * DisplayControlImpl.init(several args).
     * If the DataChoice arg is null, a default location is used.
     *
     * @param dataChoice the DataChoice of the moment; may be null.
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        LatLonPoint llp = new LatLonTuple(0.0, 0.0);


        // get geographic center of the data grid
        llp = findCenterPoint(dataChoice);

        // create the actual displayable
        rangeRings = new RadarGrid(llp, rrColor);

        if (labelFont != null) {
            rangeRings.setFont(labelFont);

        }


        // make visible and add it to display
        rangeRings.setVisible(getDisplayVisibility());
        //addDisplayable (rangeRings);

        rangeRings.setAzimuthLineColor(radColor);
        rangeRings.setRangeRingColor(rrColor);
        rangeRings.setLabelColor(lblColor);

        rangeRings.setRangeRingSpacing(rrSpacing, rrMaxRadius);
        rangeRings.setRadialInterval(radSpacing);
        rangeRings.setLabelSpacing(lblSpacing);

        rangeRings.setMaxRadius(rrMaxRadius);
        rangeRings.setLabelSize(getDisplayScale());

        rangeRings.setLabelLineWidth(lblWidth);
        rangeRings.setRangeRingLineWidth(rrWidth);
        rangeRings.setRadialLineWidth(radWidth);

        addDisplayable(rangeRings, FLAG_ZPOSITION);

        //Now, apply our local visiblity flags to each of the subcomponents
        applyVisibilityFlags();


        // set location label, if possible.
        if ((llp != null) && (positionLabel != null)) {
            positionLabel.setText(llp.toString());
        }

        if (initStationLocation != null) {
            NamedStationTable stationTable = setStations();
            Station           station = stationTable.get(initStationLocation);
            if (station != null) {
                setLatLon(station.getLatitude(), station.getLongitude(),
                          false);
            } else {
                System.err.println("RangeRings: Could not find station:"
                                   + initStationLocation);
            }
        }


        return true;
    }

    /**
     * Signal base class to add this as a display listener
     *
     * @return Add as display listener
     */
    protected boolean shouldAddDisplayListener() {
        return true;
    }



    /**
     * Remove the display control
     *
     * @throws RemoteException Badness
     * @throws VisADException Badness
     */
    public void doRemove() throws VisADException, RemoteException {
        rangeRings = null;
        super.doRemove();
    }

    /**
     * Set the stations in the combo box
     *
     * @return The station table
     */
    private NamedStationTable setStations() {
        NamedStationTable stationTable;
        if (stationTableName == null) {
            stationTable =
                getControlContext().getResourceManager().findLocations(
                    "NEXRAD Sites");
        } else {
            stationTable =
                getControlContext().getResourceManager().findLocations(
                    stationTableName);
        }

        if (stationTable == null) {
            stationList = new ArrayList();
        } else {
            stationList = new ArrayList(
                Misc.sort(new ArrayList(stationTable.values())));
        }
        if (stationCbx != null) {
            setStations(stationList, stationCbx);
            stationCbx.setSelectedIndex(stationIdx);
        }
        return stationTable;
    }


    /**
     * reposition center of radar grid.
     *
     * @param llp  new center point
     * @return  true if okay
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(LatLonPoint llp)
            throws VisADException, RemoteException {
        rangeRings.setCenterPoint(llp);

        // set location label, if available.
        if (positionLabel != null) {
            positionLabel.setText(llp.toString());
        }
        return true;
    }


    /**
     * Return the RadarGrid displayable
     *
     * @return the RadarGrid displayable
     */
    public RadarGrid getRadarGrid() {
        return rangeRings;
    }


    /**
     * Create a JComboBox for selecting colors
     *
     * @param cmd   ActionCommand for this box.
     * @param color   default color
     * @return  selector box
     */
    JComponent makeColorBox(final String cmd, Color color) {

        GuiUtils.ColorSwatch swatch = new GuiUtils.ColorSwatch(color,
                                          "Set Color") {
            public void userSelectedNewColor(Color c) {
                super.userSelectedNewColor(c);
                try {
                    if (cmd.equals(CMD_RAD_COLOR)) {
                        rangeRings.setAzimuthLineColor(radColor = c);
                    } else if (cmd.equals(CMD_RR_COLOR)) {
                        rangeRings.setRangeRingColor(rrColor = c);
                    } else if (cmd.equals(CMD_LBL_COLOR)) {
                        rangeRings.setLabelColor(lblColor = c);
                    }
                } catch (Exception exc) {
                    logException("setting color", exc);
                }
            }
        };
        return swatch;
        /*
          JComboBox jcb = getDisplayConventions().makeColorSelector(color);
          jcb.addActionListener(this);
          jcb.setActionCommand(cmd);
          return jcb;
        */
    }

    /**
     * Create a JComboBox for selecting line widths
     *
     * @param cmd   ActionCommand for this box.
     * @param width  default width
     * @return  selector box
     */
    private JComboBox makeLineWidthBox(String cmd, int width) {
        JComboBox jcb = GuiUtils.createValueBox(this, cmd, width,
                            Misc.createIntervalList(1, 10, 1), false);
        return jcb;
    }

    /**
     * Create a JComboBox for selecting units
     *
     *
     * @param units Vector of units for box
     * @param cmd   ActionCommand for this box.
     * @param selected  default unit to select
     * @return  selector box
     */
    private JComboBox makeUnitBox(Vector units, String cmd, Unit selected) {
        if ( !units.contains(selected)) {
            units.add(selected);
        }
        JComboBox jcb = new JComboBox(units);
        jcb.setSelectedItem(selected);
        jcb.setEditable(false);
        jcb.addActionListener(this);
        jcb.setActionCommand(cmd);
        return jcb;
    }


    /**
     * Add the  relevant edit menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        ObjectListener listener = new ObjectListener("") {
            public void actionPerformed(ActionEvent ae, Object obj) {
                stationTableName = ((NamedStationTable) obj).getFullName();
                stationIdx       = 0;
                setStations();
            }
        };
        List stationTables = getControlContext().getLocationList();
        items.add(
            GuiUtils.makeMenu(
                "Set Locations",
                NamedStationTable.makeMenuItems(stationTables, listener)));

        super.getEditMenuItems(items, forMenuBar);
    }


    /**
     * Make a selector for selecting the spacing
     *
     * @param spacings  list of spacings to populate component
     * @param command   command to identify component
     * @param selected  selected value
     *
     * @return selector for the spacing values
     */
    JComponent makeSpacingBox(Vector spacings, String command,
                              double selected) {
        String value;
        if (selected == (int) selected) {
            value = ("" + (int) selected);
        } else {
            value = ("" + selected);
        }
        if ( !spacings.contains(value)) {
            spacings.add(value);
        }
        JComboBox jcb = new JComboBox(spacings);
        jcb.setSelectedItem(value);
        jcb.setEditable(true);
        jcb.addActionListener(this);
        jcb.setActionCommand(command);
        return GuiUtils.wrap(jcb);
    }

    /**
     * Make a JCheckbox
     *
     * @param label     label for the checkbox
     * @param command   commmand for identification
     * @param v         true if should be selected
     *
     * @return a checkbox
     */
    JCheckBox makeCbx(String label, String command, boolean v) {
        JCheckBox jcb = new JCheckBox(label, v);
        jcb.addActionListener(this);
        jcb.setActionCommand(command);
        return jcb;
    }

    /**
     * Make the panel of controls in a vertical box.
     * @return <code>Container</code> that holds the widgets for this control
     */
    public Container doMakeContents() {

        positionAtClickCbx = new JCheckBox("Position at click", false);
        positionLabel      = new JLabel(" ", JLabel.LEFT);

        // make entry boxes for range ring center location; 
        // preload with current position.
        boolean set = false;
        if (Double.isNaN(lat)) {
            lat = rangeRings.getCenterLatitude();
        } else {
            set = true;
        }
        if (Double.isNaN(lon)) {
            lon = rangeRings.getCenterLongitude();
        } else {
            set = true;
        }
        if (set) {
            try {
                rangeRings.setCenterPoint(lat, lon);
            } catch (Exception exc) {
                logException("setting center point", exc);
            }
        }

        latField = new JTextField(getDisplayConventions().formatLatLon(lat),
                                  7);
        latField.addActionListener(this);
        latField.setActionCommand(CMD_CENTER_LAT);

        lonField = new JTextField(getDisplayConventions().formatLatLon(lon),
                                  7);
        lonField.addActionListener(this);
        lonField.setActionCommand(CMD_CENTER_LON);


        double[]    stretchy = {
            0, 0.0, 1.0, 1.0, 1.0, 0.0, 1.0
        };
        Component[] comps    = new Component[] {
            GuiUtils.filler(), GuiUtils.cLabel("Visible"),
            GuiUtils.cLabel(" Spacing "), GuiUtils.cLabel(" Unit "),
            GuiUtils.cLabel(" Color "), GuiUtils.filler(),
            GuiUtils.cLabel(" Line Width "), GuiUtils.rLabel("Range Rings: "),
            GuiUtils.hbox(new JLabel("  "),
                          makeCbx("", CMD_RR_VIS, rrVisible)),
            makeSpacingBox(rrSpacingList, CMD_RR_SPACING, rrSpacing),
            new JLabel(" (km) "), makeColorBox(CMD_RR_COLOR, rrColor),
            GuiUtils.filler(), makeLineWidthBox(CMD_RR_WIDTH, rrWidth),
            GuiUtils.rLabel("Radials: "),
            GuiUtils.hbox(new JLabel("  "),
                          makeCbx("", CMD_RAD_VIS, radVisible)),
            makeSpacingBox(radSpacingList, CMD_RAD_SPACING, radSpacing),
            new JLabel(" (deg) "), makeColorBox(CMD_RAD_COLOR, radColor),
            GuiUtils.filler(), makeLineWidthBox(CMD_RAD_WIDTH, radWidth),
            GuiUtils.rLabel("Labels: "),
            GuiUtils.hbox(new JLabel("  "),
                          makeCbx("", CMD_LBL_VIS, lblVisible)),
            makeSpacingBox(lblSpacingList, CMD_LBL_SPACING, lblSpacing),
            new JLabel(" (km) "), makeColorBox(CMD_LBL_COLOR, lblColor),
            GuiUtils.filler(), makeLineWidthBox(CMD_LBL_WIDTH, lblWidth),
            GuiUtils.rLabel(" "), new JLabel("Max. Radius: "),
            makeSpacingBox(rrMaxRadiusList, CMD_RR_RADIUS, rrMaxRadius),
            new JLabel(" (km) "), GuiUtils.filler(), GuiUtils.filler(),
            GuiUtils.filler()
        };

        GuiUtils.tmpInsets = new Insets(2, 0, 2, 0);
        JPanel top = GuiUtils.doLayout(comps, 7, stretchy, GuiUtils.WT_N);

        List   bottomComps = new ArrayList();





        stationCbx = new JComboBox();
        stationCbx.setFont(Font.decode("monospaced"));


        setStations();
        stationCbx.setSelectedIndex(stationIdx);
        stationCbx.addActionListener(this);
        stationCbx.setActionCommand(CMD_STATIONNAMES);
        bottomComps.add(GuiUtils.rLabel("Location:  "));
        bottomComps.add(GuiUtils.left(GuiUtils.wrap(stationCbx)));


        bottomComps.add(GuiUtils.rLabel("Center:"));
        bottomComps.add(
            GuiUtils.left(
                GuiUtils.hflow(
                    Misc.newList(
                        new JLabel(" Latitude: "), latField,
                        new JLabel(" Longitude: "), lonField))));
        bottomComps.add(GuiUtils.rLabel(" "));
        bottomComps.add(GuiUtils.left(positionAtClickCbx));

        bottomComps.add(GuiUtils.rLabel("Vertical Position:"));
        bottomComps.add(GuiUtils.hgrid(doMakeZPositionSlider(),
                                       GuiUtils.filler()));


        GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
        JPanel bottom = GuiUtils.doLayout(bottomComps, 2, GuiUtils.WT_NY,
                                          GuiUtils.WT_N);
        //        bottom.setBorder(BorderFactory.createTitledBorder("Location"));
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Location", GuiUtils.top(GuiUtils.inset(bottom, 5)));
        tabbedPane.add("Settings", GuiUtils.top(GuiUtils.inset(top, 5)));
        return tabbedPane;
    }





    /**
     * Get the color from the source of the action
     *
     * @param event    event from source
     * @return   selected color
     */
    Color getColor(ActionEvent event) {
        JComboBox box = (JComboBox) event.getSource();
        return getDisplayConventions().getColor(
            (String) box.getSelectedItem());
    }

    /**
     * Get the color from the source of the action
     *
     * @param event    event from source
     * @return   selected color
     */
    private int getWidth(ActionEvent event) {
        JComboBox box = (JComboBox) event.getSource();
        return GuiUtils.getBoxValue(box);
    }


    /**
     * Get the spacing value.
     *
     * @param event   event from source
     * @param min     minimum spacing value
     * @param max     maximum spacing value
     * @return  spacing between max and min
     */
    double getSpacing(ActionEvent event, double min, double max) {
        JComboBox box     = (JComboBox) event.getSource();
        String    spacing = (String) box.getSelectedItem();
        return Math.min(max, Math.max(min, Misc.parseNumber(spacing)));
    }

    /**
     * Get the maximum range ring radius
     *
     * @param event   event from selector
     * @param min     minimum radius
     * @param max     maximum radius
     * @return   radius
     */
    double getMaxRadius(ActionEvent event, double min, double max) {
        JComboBox box    = (JComboBox) event.getSource();
        String    radius = (String) box.getSelectedItem();
        return Math.min(max, Math.max(min, Misc.parseNumber(radius)));
    }

    /**
     * Get the visibility from an event
     *
     * @param event   event from source
     * @return  true if box is selected.
     */
    boolean getVisible(ActionEvent event) {
        JCheckBox box = (JCheckBox) event.getSource();
        return box.isSelected();
    }

    /**
     * Override base class method (used for legend bar clicks)
     * so we can control
     * the visiblity of the three items - rings, radials, labels -
     * according to user-clicked gui items
     *
     * @param on   true to display
     */
    public void setDisplayVisibility(boolean on) {
        if (settingVisibility) {
            return;
        }
        super.setDisplayVisibility(on);
        applyVisibilityFlags();
    }


    /**
     * Applies the current visiblity state to each of the sub-components
     * of the rangeRings.
     */
    private void applyVisibilityFlags() {
        if (rangeRings == null) {
            return;
        }
        try {
            boolean visibility = getDisplayVisibility();
            rangeRings.setRangeRingsVisible(rrVisible && visibility);
            rangeRings.setAzimuthLinesVisible(radVisible && visibility);
            rangeRings.setLabelsVisible(lblVisible && visibility);
        } catch (Exception exc) {}
    }


    /**
     * Get the extra label used for the legend.
     *
     * @param labels   labels for legend
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        if ((lat != lat) || (lon != lon)) {
            return;
        }
        labels.add("Position: " + getDisplayConventions().formatLatLon(lat)
                   + " " + getDisplayConventions().formatLatLon(lon));
    }

    /**
     * Deal with action events from the controls made by this class.
     *
     * @param event    event to check
     */
    public void actionPerformed(ActionEvent event) {
        if ( !getOkToFireEvents()) {
            return;
        }
        try {
            String cmd = event.getActionCommand();

            // if got change of radial line color
            if (cmd.equals(CMD_RAD_COLOR)) {
                rangeRings.setAzimuthLineColor(radColor = getColor(event));
            } else if (cmd.equals(CMD_RR_COLOR)) {
                rangeRings.setRangeRingColor(rrColor = getColor(event));
            } else if (cmd.equals(CMD_RR_RADIUS)) {
                rangeRings.setMaxRadius(rrMaxRadius = getMaxRadius(event,
                        1.0, 6000.0));
            } else if (cmd.equals(CMD_LBL_COLOR)) {
                rangeRings.setLabelColor(lblColor = getColor(event));
            } else if (cmd.equals(CMD_RR_SPACING)) {
                rangeRings.setRangeRingSpacing(rrSpacing = getSpacing(event,
                        0.01, 6000.0), rrMaxRadius);
            } else if (cmd.equals(CMD_LBL_SPACING)) {
                rangeRings.setLabelSpacing(lblSpacing = getSpacing(event,
                        0.01, 6000.0));
                rangeRings.setLabelColor(lblColor);
                rangeRings.setLabelsVisible(lblVisible);
            } else if (cmd.equals(CMD_RAD_SPACING)) {
                rangeRings.setRadialInterval(radSpacing = getSpacing(event,
                        0.01, 360.0));
            } else if (cmd.equals(CMD_RR_VIS)) {
                rangeRings.setRangeRingsVisible(rrVisible =
                    getVisible(event));
            } else if (cmd.equals(CMD_RAD_VIS)) {
                rangeRings.setAzimuthLinesVisible(radVisible =
                    getVisible(event));
            } else if (cmd.equals(CMD_LBL_VIS)) {
                rangeRings.setLabelsVisible(lblVisible = getVisible(event));
            } else if (cmd.equals(CMD_RR_WIDTH)) {
                rangeRings.setRangeRingLineWidth(rrWidth = getWidth(event));
            } else if (cmd.equals(CMD_RAD_WIDTH)) {
                rangeRings.setRadialLineWidth(radWidth = getWidth(event));
            } else if (cmd.equals(CMD_LBL_WIDTH)) {
                rangeRings.setLabelLineWidth(lblWidth = getWidth(event));
            } else if (cmd.equals(CMD_STATIONNAMES)) {
                stationIdx = stationCbx.getSelectedIndex();
                NamedStationImpl nsi = getSelectedStation(stationCbx);
                if (nsi == null) {
                    return;
                }
                setLatLon(nsi.getLatitude(), nsi.getLongitude(), false);
            } else if (cmd.equals(CMD_CENTER_LAT)
                       || cmd.equals(CMD_CENTER_LON)) {
                setLatLon(
                    Math.max(
                        -90.0, Math.min(
                            90.0, Misc.parseNumber(
                                latField.getText()))), Misc.parseNumber(
                                    lonField.getText()), true);

            } else {
                super.actionPerformed(event);
            }
        } catch (Exception exc) {
            logException("RadarGridControl.actionPerformed", exc);
        }
    }


    /**
     * Set the lat/lon position of the range rings
     *
     * @param lat The lat
     * @param lon The lon
     * @param andResetStationMenu If true, reset the station menu
     */
    private void setLatLon(double lat, double lon,
                           boolean andResetStationMenu) {
        try {
            this.lat = lat;
            this.lon = lon;
            rangeRings.setCenterPoint(lat, lon);
            if (stationCbx != null) {
                if (andResetStationMenu) {
                    stationCbx.setSelectedIndex(0);
                }
                updateLegendLabel();
                latField.setText(getDisplayConventions().formatLatLon(lat));
                lonField.setText(getDisplayConventions().formatLatLon(lon));
            }

        } catch (Exception exc) {
            logException("Setting lat/lon", exc);
        }


    }

    /**
     * A hook to allow derived classes to tell us to add this
     * as a control listener
     *
     * @return Add as control listener
     */
    protected boolean shouldAddControlListener() {
        return true;
    }

    /**
     * Listen for DisplayEvents
     *
     * @param event The event
     */
    public void handleDisplayChanged(DisplayEvent event) {
        if ((positionAtClickCbx == null)
                || !positionAtClickCbx.isSelected()) {
            return;
        }

        InputEvent inputEvent = event.getInputEvent();
        if (inputEvent == null) {
            return;
        }
        if ( !getHaveInitialized()
                || (getMakeWindow() && !getWindowVisible())) {
            return;
        }

        try {
            int mods = inputEvent.getModifiers();
            int id   = event.getId();
            if (id == DisplayEvent.MOUSE_PRESSED) {
                if (((mods & InputEvent.BUTTON1_MASK) != 0)) {
                    double[] boxCoords = screenToBox(event.getX(),
                                             event.getY(), getZPosition());
                    EarthLocation el = boxToEarth(boxCoords);
                    setLatLon(el.getLatitude().getValue(),
                              el.getLongitude().getValue(), true);
                }
            }
        } catch (Exception exc) {
            logException("Handling mouse click", exc);
        }

    }




    /**
     * Find the center point from the DataChoice.  If the choice has a
     * projection associated with it, gets the center, otherwise, use the
     * display's center point
     *
     * @param dc   data choice (can be null)
     * @return  the center lat/lon point
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private LatLonPoint findCenterPoint(DataChoice dc)
            throws RemoteException, VisADException {

        // If the DataChoice arg is null, use first location in list
        // to set initial location of range rings

        if (dc == null) {
            if (getNavigatedDisplay() != null) {
                return getNavigatedDisplay().getCenterPoint()
                    .getLatLonPoint();
            } else if (stationList != null) {
                NamedStationImpl station =
                    (NamedStationImpl) stationList.get(stationIdx);
                return station.getNamedLocation().getLatLonPoint();
            } else {  // if all else fails, use lat,lon center of 48 states
                return new LatLonTuple(39, -100);
            }
        }

        FieldImpl fi = (FieldImpl) dc.getData(null, getRequestProperties());

        MapProjection mp  = GridUtil.getNavigation(fi);

        Rectangle2D   box = (Rectangle2D) mp.getDefaultMapArea();

        // compute center of box
        double     cx    = box.getX() + box.getWidth() / 2;
        double     cy    = box.getY() + box.getHeight() / 2;
        double[][] llarr = new double[][] {
            { 0.0 }, { 0.0 }
        };

        if (mp.isLatLonOrder()) {
            //  is lat lon order
            llarr = mp.toReference(new double[][] {
                { cx }, { cy }
            });
        } else {  // is lon lat order
            llarr = mp.toReference(new double[][] {
                { cy }, { cx }
            });
        }

        //System.out.println("   center point is lat "+llarr[0][0]
        // +" lon "+llarr[1][0]);

        return new LatLonTuple(llarr[0][0], llarr[1][0]);
    }

    /**
     * Set the color of the radials.
     * (used by persistence)
     *
     * @param value <code>Color</code> of radials
     */
    public void setRadColor(Color value) {
        radColor = value;
    }

    /**
     * Get the color of the radials.
     * (used by persistence)
     *
     * @return <code>Color</code> of radials
     */
    public Color getRadColor() {
        return radColor;
    }

    /**
     * Set the color of the range rings.
     * (used by persistence)
     *
     * @param value <code>Color</code> of range rings
     */
    public void setRrColor(Color value) {
        rrColor = value;
    }

    /**
     * Get the color of the range rings.
     * (used by persistence)
     *
     * @return <code>Color</code> of range rings
     */
    public Color getRrColor() {
        return rrColor;
    }

    /**
     * Set the color of the labels.
     * (used by persistence)
     *
     * @param value <code>Color</code> of labels
     */
    public void setLblColor(Color value) {
        lblColor = value;
    }

    /**
     * Get the color of the labels.
     * (used by persistence)
     *
     * @return <code>Color</code> of labels
     */
    public Color getLblColor() {
        return lblColor;
    }

    /**
     * Set the spacing of the range rings.
     * (used by persistence)
     *
     * @param value spacing (km)
     */
    public void setRrSpacing(double value) {
        rrSpacing = value;
    }

    /**
     * Get the spacing of the range rings.
     * (used by persistence)
     *
     * @return spacing (km)
     */
    public double getRrSpacing() {
        return rrSpacing;
    }

    /**
     * Set the spacing of the labels.
     * (used by persistence)
     *
     * @param value spacing (degrees)
     */
    public void setLblSpacing(double value) {
        lblSpacing = value;
    }

    /**
     * Get the spacing of the labels.
     * (used by persistence)
     *
     * @return spacing (degrees)
     */
    public double getLblSpacing() {
        return lblSpacing;
    }

    /**
     * Set the spacing of the radials.
     * (used by persistence)
     *
     * @param value spacing (degrees)
     */
    public void setRadSpacing(double value) {
        radSpacing = value;
    }

    /**
     * Get the spacing of the radials.
     * (used by persistence)
     *
     * @return spacing (degrees)
     */
    public double getRadSpacing() {
        return radSpacing;
    }

    /**
     * Set whether range rings are visible.
     * (used by persistence)
     *
     * @param value true if visible
     */
    public void setRrVisible(boolean value) {
        rrVisible = value;
    }

    /**
     * Get whether range rings are visible.
     * (used by persistence)
     *
     * @return true if visible
     */
    public boolean getRrVisible() {
        return rrVisible;
    }

    /**
     * Set whether distance labels are visible.
     * (used by persistence)
     *
     * @param value true if visible
     */
    public void setLblVisible(boolean value) {
        lblVisible = value;
    }

    /**
     * Get whether distance labels are visible.
     * (used by persistence)
     *
     * @return true if visible
     */
    public boolean getLblVisible() {
        return lblVisible;
    }

    /**
     * Set whether radii are visible.
     * (used by persistence)
     *
     * @param value true if visible
     */
    public void setRadVisible(boolean value) {
        radVisible = value;
    }

    /**
     * Get whether radii are visible.
     * (used by persistence)
     *
     * @return true if visible
     */
    public boolean getRadVisible() {
        return radVisible;
    }

    /**
     * Set the latitude (degrees) of the center point.
     * (used by persistence)
     *
     * @param value center point latitude
     */
    public void setLat(double value) {
        lat = value;
    }

    /**
     * Get the latitude (degrees) of the center point.
     * (used by persistence)
     *
     * @return center point latitude
     */
    public double getLat() {
        return lat;
    }

    /**
     * Set the longitude (degrees) of the center point.
     * (used by persistence)
     *
     * @param value center point longitude
     */
    public void setLon(double value) {
        lon = value;
    }

    /**
     * Get the longitude (degrees) of the center point.
     * (used by persistence)
     *
     * @return center point longitude
     */
    public double getLon() {
        return lon;
    }

    /**
     * Get the station index in the list.
     * (used by persistence)
     * @return station index
     *
     */
    public int getStationIdx() {
        return stationIdx;
    }

    /**
     * Set the station index in the list.
     * (used by persistence)
     *
     * @param idx  station index
     */
    public void setStationIdx(int idx) {
        stationIdx = idx;
    }

    /**
     * Set the maximum radius value for the range rings.
     * (used by persistence)
     *
     * @param value radius in kilometers
     */
    public void setMaxRadius(double value) {
        rrMaxRadius = value;
    }

    /**
     * Get the maximum radius value for the range rings.
     * (used by persistence)
     *
     * @return radius in kilometers
     */
    public double getMaxRadius() {
        return rrMaxRadius;
    }

    /**
     * Set the width of the radial lines. (used by persistence)
     *
     * @param width width in pixels
     */
    public void setRadWidth(int width) {
        radWidth = width;
    }

    /**
     * Get the width of the radial lines. (used by persistence)
     *
     * @return width in pixels
     */
    public int getRadWidth() {
        return radWidth;
    }

    /**
     * Get the width of the labels. (used by persistence)
     *
     * @return width in pixels
     */
    public int getLblWidth() {
        return lblWidth;
    }

    /**
     * Set the width of the labels. (used by persistence)
     *
     * @param width width in pixels
     */
    public void setLblWidth(int width) {
        lblWidth = width;
    }

    /**
     * Get the width of the range rings. (used by persistence)
     *
     * @return width in pixels
     */
    public int getRrWidth() {
        return rrWidth;
    }

    /**
     * Set the width of the range rings. (used by persistence)
     *
     * @param width width in pixels
     */
    public void setRrWidth(int width) {
        rrWidth = width;
    }



    /**
     * Set the VerticalValue property.
     *
     * @param value The new value for VerticalValue
     * @deprecated Keep this around for legacy bundles.
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setVerticalValue(double value)
            throws RemoteException, VisADException {
        super.setZPosition(value);
    }




    /**
     * Set the StationTableName property.
     *
     * @param value The new value for StationTableName
     */
    public void setStationTableName(String value) {
        stationTableName = value;
    }

    /**
     * Get the StationTableName property.
     *
     * @return The StationTableName
     */
    public String getStationTableName() {
        return stationTableName;
    }

    /**
     * Set the LabelFont property.
     *
     * @param value The new value for LabelFont
     */
    public void setLabelFont(Font value) {
        labelFont = value;
    }

    /**
     * Get the LabelFont property.
     *
     * @return The LabelFont
     */
    public Font getLabelFont() {
        return labelFont;
    }

    /**
     * For setting the property when this object is created
     *
     * @param l the initial station to use
     */
    public void setInitStationLocation(String l) {
        initStationLocation = l;
    }


}
