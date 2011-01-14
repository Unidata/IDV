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
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.display.CrossSectionSelector;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;


import ucar.visad.display.SelectorDisplayable;


import visad.*;

import visad.data.units.Parser;

import visad.georef.EarthLocation;

import visad.georef.EarthLocationTuple;

import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;



import java.awt.*;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import javax.vecmath.Point3d;



/**
 * Class to make a dragable line with end points which computes the
 * range and bearing between the end points.
 *
 * Also makes a JFrame with text display of range and bearing values, and
 * control buttoms to remove the whole thing etc.
 *
 * @author Jeff McWhirter
 * @version  $Revision: 1.87 $
 */
public class RangeAndBearingControl extends DisplayControlImpl implements ActionListener,
        PropertyChangeListener, DisplayableData.DragAdapter {

    /** Start point identifier */
    public static final int POINT_START = CrossSectionSelector.POINT_START;

    /** End point identifier */
    public static final int POINT_END = CrossSectionSelector.POINT_END;

    /** Declination property */
    public static final String PROP_DECLINATION = "declination";

    /** Command property for the station start point */
    public static final String CMD_STATION_START = "station_start";

    /** Command property for the station end point */
    public static final String CMD_STATION_END = "station_end";

    /** Command property for the text field start point */
    public static final String CMD_FLD_START = "cmdfldstart";

    /** Command property for the text field end point */
    public static final String CMD_FLD_END = "cmdfldend";

    /** Command property for the planet choice */
    public static final String CMD_PLANET_CHOICE = "cmdplanet";

    /** property for sharing transect location */
    public static final String SHARE_RBCLINE = SHARE_TRANSECT;

    /** property for sharing station start point */
    public static final String SHARE_STATION_START = "share_station_start";

    /** property for sharing station end point */
    public static final String SHARE_STATION_END = "share_station_end";

    /** property for sharing location start point */
    public static final String SHARE_LOCATION_START = "share_loc_start";

    /** property for sharing location end point */
    public static final String SHARE_LOCATION_END = "share_loc_end";

    /** the selector line */
    private CrossSectionSelector csSelector;




    /** Are we using magnetic declinatio  for the start point */
    private boolean usingMagneticNorthStart = false;

    /** Are we using magnetic declinatio  for the end point */
    private boolean usingMagneticNorthEnd = false;

    /** Do we have  location list that contains declination */
    private boolean canHaveDeclination = false;

    /** default values */
    private double range       = 0.0,
                   bearing     = 0.0,
                   backazimuth = 0.0;

    /** default planet */
    private Earth planet;

    /** starting station */
    private NamedStationImpl startStation;

    /** ending station */
    private NamedStationImpl endStation;

    /** combo box for selecting starting station */
    JComboBox stationStartCB;

    /** combo box for selecting ending station */
    JComboBox stationEndCB;

    /** combo box for selecting planet */
    JComboBox planetCB;

    /** For earth properties */
    JRadioButton useListBtn;

    /** For earth properties */
    JRadioButton useFldBtn;


    /** For earth properties */
    JTextField rad1Fld;

    /** For earth properties */
    JTextField rad2Fld;


    /** label for range value */
    private JLabel rangeLbl;

    /** label for range unit */
    private JLabel rangeUnitLbl;

    /** label for bearing value */
    private JLabel bearingValueLbl;

    /** label for bearing value */
    private JLabel bearingLbl;

    /** label for bearing value */
    private JLabel azimuthLbl;

    /** label for back azimuth */
    private JLabel azimuthValueLbl;


    /** text fields for input of start/end lat/lon values */
    private JTextField startLatFld, startLonFld, endLatFld, endLonFld;

    /** starting lat/lon positions */
    private float startLat = Float.NaN,
                  startLon = Float.NaN;

    /** ending lat/lon positions */
    private float endLat = Float.NaN,
                  endLon = Float.NaN;

    /** flag for accepting events from the selector */
    private boolean acceptEventsFromSelector = true;

    /** Station table name */
    private String stationTableName;

    /** Station list */
    private List stationList = null;

    /**
     * Default Constructor. Set the flags to tell that this display
     * control wants a color widget.
     */
    public RangeAndBearingControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_DISPLAYUNIT);
    }


    /**
     * Return the selector line with end points.
     *
     * @return a CrossSectionSelector  the selector line with end points.
     */
    public CrossSectionSelector getCrossSectionSelector() {
        return csSelector;
    }

    /**
     * Called to make this kind of Display Control; also calls code to
     * made its Displayable, the line.
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment -
     *                   not used yet; can be null.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if (getDisplayUnit() == null) {
            setDisplayUnit(getDefaultDistanceUnit());
        }

        createCrossSectionSelector();
        csSelector.setPointSize(getDisplayScale());
        csSelector.setAutoSize(true);
        addDisplayable(csSelector, FLAG_COLOR | FLAG_ZPOSITION);

        setContents(doMakeContents());

        if (Float.isNaN(startLat)) {
            // if have no persisted values for startLat then use the default selector position
            setLatLonFromSelector();
        } else {
            // move line to persisted location for line
            setSelectorPosition();
        }
        updateFields();
        setRangeAndBearing();
        return true;
    }



    /**
     * Set the stations used for the combo boxes
     */
    private void setStations() {
        NamedStationTable stationTable;
        if (stationTableName == null) {
            stationTable =
                getControlContext().getResourceManager().getDefaultStations();
        } else {
            stationTable =
                getControlContext().getResourceManager().findLocations(
                    stationTableName);
        }

        if (stationTable == null) {
            stationList = new ArrayList();
        } else {
            stationList = new ArrayList(stationTable.values());
            stationList = new ArrayList(Misc.sort(stationList));
        }
        canHaveDeclination = false;
        for (int i = 0; i < stationList.size(); i++) {
            NamedStationImpl station = (NamedStationImpl) stationList.get(i);
            if (station.getProperties().get(PROP_DECLINATION) != null) {
                canHaveDeclination = true;
                break;
            }
        }
        setStations(stationList, stationStartCB);
        setStations(stationList, stationEndCB);
    }

    /**
     * Update the combo boxes
     */
    private void updateStationBoxes() {
        if (stationStartCB != null) {
            if ((startStation != null)
                    && (stationList.indexOf(startStation) != -1)) {
                stationStartCB.setSelectedItem(
                    createStationTfo(startStation));
            } else {
                stationStartCB.setSelectedIndex(0);
            }
        }
        if (stationEndCB != null) {
            if ((endStation != null)
                    && (stationList.indexOf(endStation) != -1)) {
                stationEndCB.setSelectedItem(createStationTfo(endStation));
            } else {
                stationEndCB.setSelectedIndex(0);
            }
        }
    }

    /**
     *  Set the text value of the  lat/lon text fields to the current value.
     */
    private void updateFields() {
        if (startLatFld != null) {
            //System.err.println ("setText:" +getDisplayConventions().formatLatLon (startLat) + "--" +getDisplayConventions().formatLatLon (startLon));
            startLatFld.setText(
                getDisplayConventions().formatLatLon(startLat));
            startLonFld.setText(
                getDisplayConventions().formatLatLon(startLon));
            endLatFld.setText(getDisplayConventions().formatLatLon(endLat));
            endLonFld.setText(getDisplayConventions().formatLatLon(endLon));
        }
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
                stationTableName = ((NamedStationTable) obj).getName();
                clearStations();
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
     * Remove the display control
     *
     * @throws RemoteException Bad things
     * @throws VisADException Bad things
     */
    public void doRemove() throws VisADException, RemoteException {
        if (startLatFld != null) {
            startLatFld.removeActionListener(this);
            startLonFld.removeActionListener(this);
            endLatFld.removeActionListener(this);
            endLonFld.removeActionListener(this);
        }
        if (csSelector != null) {
            csSelector.removeStartPropertyChangeListener(this);
            csSelector.removeEndPropertyChangeListener(this);
            csSelector.removeMidPropertyChangeListener(this);
            csSelector = null;
        }
        super.doRemove();
    }


    /**
     * Create a JTextField with a particular CMD
     *
     * @param command   commmand to use for actions
     * @return   JTextField
     */
    private JTextField getField(String command) {
        JTextField fld = new JTextField(6);
        fld.addActionListener(this);
        fld.setActionCommand(command);
        return fld;
    }

    /**
     * Clear the list of stations
     */
    private void clearStations() {
        startStation = null;
        endStation   = null;
        checkStationWidgets();
        try {
            setRangeAndBearing();
        } catch (Exception exc) {
            logException("setRangeAndBearing", exc);
        }
    }


    /**
     * Clear the list of stations
     */
    private void checkStationWidgets() {
        if ((stationStartCB != null) && (startStation == null)) {
            stationStartCB.setSelectedIndex(0);
        }
        if ((stationEndCB != null) && (endStation == null)) {
            stationEndCB.setSelectedIndex(0);
        }
    }



    /**
     * Make the UI contents for this control.
     *
     * @return  UI container
     */
    public Container doMakeContents() {

        try {
            Font monoFont = Font.decode("monospaced");
            rangeUnitLbl = new JLabel(" ");
            rangeLbl     = new JLabel(" ");
            bearingValueLbl = new JLabel("<html>"
                                         + StringUtil.repeat("&nbsp;", 18)
                                         + "</html>");
            azimuthValueLbl = new JLabel("<html>"
                                         + StringUtil.repeat("&nbsp;", 18)
                                         + "</html>");
            rangeLbl.setFont(monoFont);
            bearingValueLbl.setFont(monoFont);
            azimuthValueLbl.setFont(monoFont);

            // make text fields for end point positions.

            startLatFld = getField(CMD_FLD_START);
            startLonFld = getField(CMD_FLD_START);
            endLatFld   = getField(CMD_FLD_END);
            endLonFld   = getField(CMD_FLD_END);

            // make JComboBox with station names
            //    get the list of "NamedStationImpl" in "..idv/locations.xml"

            stationStartCB = new JComboBox();
            stationEndCB   = new JComboBox();
            stationStartCB.setFont(Font.decode("monospaced"));
            stationEndCB.setFont(Font.decode("monospaced"));
            setStations();
            updateStationBoxes();
            stationStartCB.addActionListener(this);
            stationStartCB.setActionCommand(CMD_STATION_START);
            stationEndCB.addActionListener(this);
            stationEndCB.setActionCommand(CMD_STATION_END);

            // define the default earth
            if (planet == null) {
                planet = new Earth(6378137.0, 6356752.0, 0.0, "Earth");
            }
            //planet = new Earth(6378137.0 , 0., 298.257223563);

            JButton applyBtn = GuiUtils.makeApplyButton(this);
            applyBtn.setMargin(new Insets(0, 0, 0, 0));

            Hashtable fills = new Hashtable();
            fills.put(applyBtn,
                      new Integer(java.awt.GridBagConstraints.NONE));
            GuiUtils.setHFill();

            ArrayList list = new ArrayList();
            list.add(GuiUtils.rLabel("   Distance: "));
            list.add(rangeLbl);
            list.add(GuiUtils.rLabel("  "));
            list.add(rangeUnitLbl);
            list.add(bearingLbl = GuiUtils.rLabel("  Azimuth: "));
            list.add(bearingValueLbl);
            list.add(azimuthLbl = GuiUtils.rLabel("   Back Azimuth: "));
            list.add(azimuthValueLbl);
            JPanel rangePanel   = GuiUtils.hbox(list);

            JPanel startCbPanel = GuiUtils.wrap(stationStartCB);
            JPanel endCbPanel   = GuiUtils.wrap(stationEndCB);
            GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
            JPanel latLonPanel = GuiUtils.doLayout(new JPanel(),
                                     new Component[] {
                GuiUtils.filler(), new JLabel("Latitude"),
                new JLabel("Longitude"), GuiUtils.filler(),
                GuiUtils.rLabel("From:"), startLatFld, startLonFld,
                startCbPanel, GuiUtils.rLabel("To:"), endLatFld, endLonFld,
                endCbPanel, GuiUtils.filler(), applyBtn
            }, 4, GuiUtils.WT_N, GuiUtils.WT_N, null, fills, null);

            return GuiUtils.top(GuiUtils.topCenterBottom(rangePanel,
                    GuiUtils.left(latLonPanel), doMakeWidgetComponent()));
        } catch (Exception exc) {
            logException("doMakeContents", exc);
        }
        return null;

    }


    // make a Selector line which shows and controls the end points
    // used to compute range and bearing

    /**
     * Create the cross section selector.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void createCrossSectionSelector()
            throws VisADException, RemoteException {

        double[] right  = getNavigatedDisplay().getScreenUpperRight();
        double[] center = getNavigatedDisplay().getScreenCenter();
        right[1] = center[1];
        double width = right[0] - center[0];

        // make the initial selector end positions:
        // start at center of VisAD box
        RealTuple start =
            new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                          new double[] { center[0],
                                         center[1] });
        // go to 1/3 way to right edge of the VisAd box around the data area.
        RealTuple end = new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                                      new double[] { center[0] + 0.6 * width,
                right[1] });

        RealTuple[] positions =
            makeDefaultLinePosition(getNavigatedDisplay());
        // make a Selector line there
        csSelector = new CrossSectionSelector(positions[0], positions[1],
                Color.red);


        // move z level of line to near top of VisAD display box;
        // use -0.99 or so for near bottom
        //        csSelector.setZValue(0.99f);

        // when user moves position of the Selector line, 
        // call crossSectionChanged
        csSelector.addStartPropertyChangeListener(this);
        csSelector.addEndPropertyChangeListener(this);
        csSelector.addMidPropertyChangeListener(this);

        csSelector.getStartSelectorPoint().setDragAdapter(this);
        csSelector.getEndSelectorPoint().setDragAdapter(this);
        csSelector.getMiddleSelectorPoint().setDragAdapter(this);

        if (inGlobeDisplay()) {
            csSelector.setInterpolateLinePoints(true);
        }
    }


    /**
     * Add the earth tab
     *
     * @param jtp the tabbed pane
     */
    protected void addPropertiesComponents(JTabbedPane jtp) {
        super.addPropertiesComponents(jtp);
        /*
          Values taken from McIDAS-X.

          rad = [2440.,6120.,6378.137,3393.5,71400.,60330., 25900., 24750.]
          ec = [0., 0., 0.08181919, 0.1333386, 0.35412,0.41019,0.2086,0.2]
          name=['Mercury','Venus', 'Earth','Mars','Jupiter','Saturn', 'Uranus','Neptune']
        */

        Earth[] planets = {
            new Earth(6378.137 * 1000, 6356.752 * 1000, 0, "Earth"),
            new Earth(2440.0 * 1000, 2440.0 * 1000, 0, "Mercury"),
            new Earth(6120.0 * 1000, 6120.0 * 1000, 0, "Venus"),
            new Earth(3393.5 * 1000, 3363.2 * 1000, 0, "Mars"),
            new Earth(71400.0 * 1000, 66773.3 * 1000, 0, "Jupiter"),
            new Earth(60330.0 * 1000, 55021.0 * 1000, 0, "Saturn"),
            new Earth(25900.0 * 1000, 25330.2 * 1000, 0, "Uranus"),
            new Earth(24750.0 * 1000, 24249.9 * 1000, 0, "Neptune")
        };

        Vector<TwoFacedObject> tfos     = new Vector<TwoFacedObject>();
        TwoFacedObject         selected = null;
        for (Earth aPlanet : planets) {
            TwoFacedObject tfo = new TwoFacedObject(aPlanet.getName() + " ("
                                     + aPlanet.getEquatorRadius() / 1000
                                     + "x" + aPlanet.getPoleRadius() / 1000
                                     + ")", aPlanet);
            tfos.add(tfo);
            if (Misc.equals(aPlanet, planet)) {
                selected = tfo;
            }
        }
        planetCB = new JComboBox(tfos);
        if (selected != null) {
            planetCB.setSelectedItem(selected);
        }
        //        planetCB.setFont(Font.decode("monospaced"));



        useListBtn = new JRadioButton("Use predefined:", (selected != null));
        useFldBtn = new JRadioButton("Enter your own value:",
                                     (selected == null));
        GuiUtils.buttonGroup(useListBtn, useFldBtn);
        rad1Fld = new JTextField(planet.getEquatorRadius() / 1000 + "", 10);
        rad2Fld = new JTextField(planet.getPoleRadius() / 1000 + "", 10);

        JComponent contents = GuiUtils.formLayout(Misc.newList(useListBtn,
                                  GuiUtils.left(planetCB), useFldBtn,
                                  GuiUtils.formLayout(new Object[] {
                                      GuiUtils.rLabel("Equatorial Radius:"),
                                      GuiUtils.left(rad1Fld),
                                      GuiUtils.rLabel("Polar Radius:"),
                                      GuiUtils.left(rad2Fld) })));
        contents = GuiUtils.top(contents);
        jtp.add("Planet", contents);

    }

    /**
     * apply the earth properties
     *
     * @return ok
     */
    public boolean doApplyProperties() {
        if ( !super.doApplyProperties()) {
            return false;
        }

        TwoFacedObject selected = (TwoFacedObject) planetCB.getSelectedItem();

        //For now just use the combobox
        if (useListBtn.isSelected()) {
            planet = (Earth) selected.getId();
        } else {
            double plrad1 = 6378.1370;
            double plrad2 = 6356.75231;
            try {
                plrad1 = Double.parseDouble(rad1Fld.getText().trim());
                plrad2 = Double.parseDouble(rad2Fld.getText().trim());
            } catch (NumberFormatException plnfe) {
                userMessage("Invalid number format");
                return false;
            }
            planet = new Earth(1000 * plrad1, 1000 * plrad2, 0.0);
        }

        clearStations();

        return true;
    }


    /**
     * A utility method to create the initial line position
     *
     * @param mapDisplay The display
     *
     * @return An array of size 2 that holds the start and end position
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public RealTuple[] makeDefaultLinePosition(NavigatedDisplay mapDisplay)
            throws VisADException, RemoteException {

        if (inGlobeDisplay()) {
            Point3d          p          = new Point3d(0, 0, 1);
            NavigatedDisplay navDisplay = getNavigatedDisplay();
            navDisplay.applyRotation(p);
            EarthLocation pt = boxToEarth(p.x, p.y, p.z, false);
            return new RealTuple[] {
                getRealTupleForPoint((float) pt.getLatitude().getValue(),
                                     (float) pt.getLongitude().getValue()),
                getRealTupleForPoint(
                    (float) pt.getLatitude().getValue() - 10,
                    (float) pt.getLongitude().getValue()) };
        }




        double[] right  = mapDisplay.getScreenUpperRight();
        double[] center = mapDisplay.getScreenCenter();
        right[1] = center[1];
        double width = right[0] - center[0];

        EarthLocationTuple el1 =
            (EarthLocationTuple) mapDisplay.getEarthLocation(center[0],
                center[1], 0, false);
        EarthLocationTuple el2 =
            (EarthLocationTuple) mapDisplay.getEarthLocation(center[0]
                + 0.6 * width, right[1], 0, false);

        if (true) {
            return new RealTuple[] {
                getRealTupleForPoint((float) el1.getLatitude().getValue(),
                                     (float) el1.getLongitude().getValue()),
                getRealTupleForPoint((float) el2.getLatitude().getValue(),
                                     (float) el2.getLongitude().getValue()) };
        }
        // make the initial selector end positions:
        // start at center of VisAD box
        RealTuple start = new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                        new double[] { center[0],
                center[1], 0 });
        // go to 1/3 way to right edge of the VisAd box around the data area.
        RealTuple end = new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                      new double[] { center[0],
                right[1], 0 });

        return new RealTuple[] { start, end };
    }



    /**
     * Constrain the altitude of the drag point
     *
     * @param position point position
     *
     * @return continue dragging
     */
    public boolean constrainDragPoint(float[] position) {
        try {
            double altitude = getSelectorAltitude();
            EarthLocation pt = boxToEarth(position[0], position[1],
                                          position[2], false);
            double[] xyz = earthToBox(
                               new EarthLocationTuple(
                                   pt.getLatitude().getValue(),
                                   pt.getLongitude().getValue(), altitude));
            if (inGlobeDisplay()) {
                position[0] = (float) xyz[0];
                position[1] = (float) xyz[1];
                position[2] = (float) xyz[2];
            } else {
                position[2] = (float) xyz[2];
            }
            return true;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * method for dragadapter interface
     *
     * @param ray the ray
     * @param first first time
     * @param mouseModifiers event
     *
     * @return ok
     */
    public boolean handleDragDirect(VisADRay ray, boolean first,
                                    int mouseModifiers) {
        return true;
    }

    /**
     * method for dragadapter interface
     *
     * @param x points
     *
     * @return ok
     */
    public boolean handleAddPoint(float[] x) {
        return true;
    }


    /**
     * get the initial z position to use. If in globe return 0
     *
     * @return z position
     */
    protected double getInitialZPosition() {
        if (inGlobeDisplay()) {
            return 0;
        }
        return super.getInitialZPosition();
    }



    /**
     * Override this and just force a change in position topickup the new z
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected void applyZPosition() throws VisADException, RemoteException {
        setSelectorPosition();
    }


    /**
     * Get the fixed altitude for the selector
     *
     * @return altitude
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public double getSelectorAltitude()
            throws VisADException, RemoteException {
        double zPosition = getZPosition();
        if (inGlobeDisplay()) {
            //If in the globe display then offset the z position (which is in the range -1 -> 1) by +1 and a little bit
            zPosition = getZPosition() + 1 + .01;
        }
        EarthLocation el = boxToEarth(new double[] { 0, 0, zPosition });
        return el.getAltitude().getValue();
    }


    /**
     * Handle property change
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if ( !getHaveInitialized()) {
            return;
        }
        if ( !acceptEventsFromSelector) {
            return;
        }
        String prop = evt.getPropertyName();
        if (prop.equals(CrossSectionSelector.PROPERTY_STARTPOINT)) {
            startStation = null;
        } else if (prop.equals(CrossSectionSelector.PROPERTY_ENDPOINT)) {
            endStation = null;
        } else if (prop.equals(CrossSectionSelector.PROPERTY_MIDPOINT)) {
            startStation = null;
            endStation   = null;
        } else {
            return;
        }
        transectPositionChanged(true);
    }



    /**
     * Method to perform what needs to be done when transect position
     * changes.
     *
     * @param fromSelector true if from the selector
     */
    public void transectPositionChanged(boolean fromSelector) {
        try {
            if (fromSelector) {
                setLatLonFromSelector();
                checkStationWidgets();
            }
            CrossSectionSelector cs = getCrossSectionSelector();
            doShare(SHARE_RBCLINE, new Object[] { cs.getStartPoint(),
                    cs.getEndPoint() });
        } catch (Exception exc) {
            logException("Error in transectPositionChanged ", exc);
        }
    }

    /**
     * Accept and check sharable data.
     *
     * @param from      object sending shared data
     * @param dataId    id for sharable data
     * @param data      the data
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if (dataId.equals(SHARE_RBCLINE)) {
            if (csSelector == null) {
                return;
            }
            try {
                csSelector.setPosition((RealTuple) data[0],
                                       (RealTuple) data[1]);
                //Should get this from the event listener setLatLonFromSelector ();
            } catch (Exception e) {
                logException("receiveShareData:" + dataId, e);
            }
        } else if (dataId.equals(SHARE_STATION_START)) {
            NamedStationImpl station = (NamedStationImpl) data[0];
            stationStartCB.setSelectedItem(createStationTfo(station));
            updateFields();
        } else if (dataId.equals(SHARE_STATION_END)) {
            NamedStationImpl station = (NamedStationImpl) data[0];
            stationEndCB.setSelectedItem(createStationTfo(station));
            updateFields();
        } else if (dataId.equals(SHARE_LOCATION_START)) {}
        else if (dataId.equals(SHARE_LOCATION_END)) {}
        else {
            super.receiveShareData(from, dataId, data);
        }
    }



    /**
     * Create and return an EarthLocationTuple base on the given
     * x/y coordinates.
     *
     *
     * @param x     X position
     * @param y     Y position
     * @return   lat/lon/alt from xy
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private EarthLocationTuple getEarthLocation(double x, double y)
            throws VisADException, RemoteException {
        return (EarthLocationTuple) boxToEarth(new double[] { x, y, 1 });
    }

    /**
     * Convert the given x/y location to a LatLonPoint
     *
     * @param x     X position
     * @param y     Y position
     * @return   lat/lon from xy
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */

    private LatLonPoint getLatLonPoint(double x, double y)
            throws VisADException, RemoteException {
        return getEarthLocation(x, y).getLatLonPoint();
    }

    /**
     * Convert the given x/y location to a LatLonPoint
     *
     * @param rt    xy as a RealTuple
     * @return  lat/lon
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private LatLonPoint getLatLonPoint(RealTuple rt)
            throws VisADException, RemoteException {

        if (rt.getType().equals(RealTupleType.SpatialEarth3DTuple)) {
            return new LatLonTuple((Real) rt.getComponent(1),
                                   (Real) rt.getComponent(0));
        }

        return getEarthLocation(
            ((Real) rt.getComponent(0)).getValue(),
            ((Real) rt.getComponent(1)).getValue()).getLatLonPoint();
    }



    /**
     * Calculate the dis/az/backaz based on the values of the
     * start/end lat/lon positions.
     * If the JLabels are non-null then set their values
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setRangeAndBearing() throws VisADException, RemoteException {

        // get values from ellipsoidal earth
        Bearing result = Bearing.calculateBearing(planet,
                             new LatLonPointImpl(startLat, startLon),
                             new LatLonPointImpl(endLat, endLon), null);

        range                   = result.getDistance();
        bearing                 = result.getAngle();
        backazimuth             = result.getBackAzimuth();
        usingMagneticNorthStart = false;
        usingMagneticNorthEnd   = false;

        if (startStation != null) {
            if (startStation.getProperties().get(PROP_DECLINATION) != null) {
                usingMagneticNorthStart = true;
                bearing -= Misc.parseDouble(
                    (String) startStation.getProperties().get(
                        PROP_DECLINATION));
            }
        }
        if (endStation != null) {
            if (endStation.getProperties().get(PROP_DECLINATION) != null) {
                usingMagneticNorthEnd = true;
                backazimuth -= Misc.parseDouble(
                    (String) endStation.getProperties().get(
                        PROP_DECLINATION));
            }
        }

        if ((rangeLbl != null) && (bearingValueLbl != null)
                && (azimuthValueLbl != null)) {
            Unit displayUnit = getDisplayUnit();
            rangeUnitLbl.setText("" + displayUnit);
            range = displayUnit.toThis(1000.0 * range,
                                       visad.CommonUnit.meter);
            String extra1 = "";
            String extra2 = "";
            if (canHaveDeclination) {
                extra1 = (usingMagneticNorthStart
                          ? "magnetic"
                          : "absolute");
                extra2 = (usingMagneticNorthEnd
                          ? "magnetic"
                          : "absolute");
                extra1 = "&nbsp;<u><b>" + extra1 + "</b></u>";
                extra2 = "&nbsp;<u><b>" + extra2 + "</b></u>";

            } else {
                extra2 = extra1 = StringUtil.repeat("&nbsp;", 9);
            }
            rangeLbl.setText(
                StringUtil.padLeft(
                    getDisplayConventions().formatDistance(range), 8));
            bearingValueLbl.setText(
                "<html>"
                + padLabel(getDisplayConventions().formatAngle(bearing))
                + extra1 + "</html>");
            azimuthValueLbl.setText(
                "<html>"
                + padLabel(getDisplayConventions().formatAngle(backazimuth))
                + extra2 + "</html>");
            updateLegendLabel();
        }
    }


    /**
     * Utility to pad the labe
     *
     * @param l label
     *
     * @return padded label
     */
    private String padLabel(String l) {
        int diff = 7 - l.length();
        if (diff > 0) {
            l = StringUtil.repeat("&nbsp;", diff) + l;
        }
        return l;
    }

    /**
     * Override base class method toact when the display unit has changed
     *
     * @param newUnit The new unit
     * @param applyToDisplayable Some parameter
     *
     * @return Was successful
     */
    protected boolean setNewDisplayUnit(Unit newUnit,
                                        boolean applyToDisplayable) {
        if ( !super.setNewDisplayUnit(newUnit, applyToDisplayable)) {
            return false;
        }
        if (newUnit == null) {
            return true;
        }
        try {
            setRangeAndBearing();
        } catch (Exception exc) {
            logException("Setting display unit", exc);
        }
        return true;
    }



    /**
     * Get the extra label used for the legend.
     *
     * @param labels   labels to append to
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        labels.add("Distance: "
                   + getDisplayConventions().formatDistance(range) + " "
                   + getDisplayUnit());
        String label;
        label = "Azimuth: " + getDisplayConventions().formatAngle(bearing);
        if (canHaveDeclination) {
            label = label + " " + (usingMagneticNorthStart
                                   ? "magnetic"
                                   : "absolute");
        }
        labels.add(label);
        label = "Back azimuth: "
                + getDisplayConventions().formatAngle(backazimuth);
        if (canHaveDeclination) {
            label = label + " " + (usingMagneticNorthEnd
                                   ? "magnetic"
                                   : "absolute");
        }

        labels.add(label);
    }

    /**
     * Utility method to convert the text value of the given JTextField
     * into a float Latitude value.
     *
     * @param f    field to get value from
     * @return  decoded value
     */
    private float getLatitude(JTextField f) {
        try {
            float v = (float) Misc.decodeLatLon(f.getText());
            v = Math.max(-90.0f, v);
            return Math.min(90.0f, v);
        } catch (NumberFormatException nfe) {
            userErrorMessage("Incorrect format:" + f.getText());
            return 0.0f;
        }
    }

    /**
     * Utility method to convert the text value of the given JTextField
     * into a float Longitude value.
     *
     * @param f   field to get value from
     * @return  value
     */
    private float getLongitude(JTextField f) {
        try {
            float v = (float) Misc.decodeLatLon(f.getText());
            v = Math.max(-180.0f, v);
            return Math.min(360.0f, v);
        } catch (NumberFormatException nfe) {
            userErrorMessage("Incorrect format:" + f.getText());
            return 0.0f;
        }
    }



    /**
     * Deal with action events from the gui controls made by this class.
     * @param event  action event to check
     */
    public void actionPerformed(ActionEvent event) {
        if ( !getOkToFireEvents()) {
            return;
        }
        try {
            String cmd = event.getActionCommand();
            if (cmd.equals(CMD_STATION_START)
                    || cmd.equals(CMD_STATION_END)) {
                NamedStationImpl station =
                    getSelectedStation(((JComboBox) event.getSource()));
                if (station == null) {
                    return;
                }
                if (cmd.equals(CMD_STATION_START)) {
                    startStation = station;
                    setLatLon(POINT_START, (float) station.getLatitude(),
                              (float) station.getLongitude());
                    doShare(SHARE_LOCATION_START,
                            new Object[] { startStation });
                } else {
                    endStation = station;
                    setLatLon(POINT_END, (float) station.getLatitude(),
                              (float) station.getLongitude());
                    doShare(SHARE_LOCATION_END, new Object[] { endStation });
                }
                updateFields();
                return;
            } else if (cmd.equals(CMD_FLD_START)) {
                setLatLon(POINT_START, getLatitude(startLatFld),
                          getLongitude(startLonFld));
            } else if (cmd.equals(CMD_FLD_END)) {
                setLatLon(POINT_END, getLatitude(endLatFld),
                          getLongitude(endLonFld));
            } else if (cmd.equals(GuiUtils.CMD_APPLY)) {
                //the false  says don't notify the selector.
                setLatLon(POINT_START, getLatitude(startLatFld),
                          getLongitude(startLonFld), true);
                setLatLon(POINT_END, getLatitude(endLatFld),
                          getLongitude(endLonFld), true);
            } else if (cmd.equals(CMD_PLANET_CHOICE)) {}
            else {
                //Let the base class handle this
                super.actionPerformed(event);
                return;
            }
            //Calling clearStations resets the station combo boxes
            clearStations();
        } catch (Exception exc) {
            logException("RadarGridControl.actionPerformed", exc);
        }
    }


    /**
     * Return the x/y location of the given end point of the
     * CrossSectionSelector
     *
     * @param which    which end (start or end)
     * @return  position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public RealTuple getPointOfSelector(int which)
            throws VisADException, RemoteException {
        if (which == POINT_START) {
            return ((csSelector != null)
                    ? csSelector.getStartPoint()
                    : null);
        } else {
            return ((csSelector != null)
                    ? csSelector.getEndPoint()
                    : null);
        }
    }


    /**
     * Get the end points of the CrossSectionSelector and set our
     * lat/lon attributes. Update the lat/lon fields.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setLatLonFromSelector()
            throws VisADException, RemoteException {
        //      System.err.println ("setLatLonFromSelector");
        setLatLon(POINT_START,
                  getLatLonPoint(getPointOfSelector(POINT_START)), false);
        setLatLon(POINT_END, getLatLonPoint(getPointOfSelector(POINT_END)),
                  false);
        updateFields();
    }

    /**
     *  Set the value of the lat/lon point based on the
     *  given which (either POINT_START or POINT_END).
     *  Update the position of the CrossSectionSelector
     *
     * @param which    which point (start or end)
     * @param llp      lat lon point
     * @param updateSelector   true to update the selector
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setLatLon(int which, LatLonPoint llp, boolean updateSelector)
            throws VisADException, RemoteException {
        setLatLon(which, (float) llp.getLatitude().getValue(),
                  (float) llp.getLongitude().getValue(), updateSelector);
    }



    /**
     * Wrapper that calls setLatLon (which, lat, lon, true)
     * The true tells the method to notify the CrossSectionSelector
     *
     * @param which    which point (start or end)
     * @param lat      latitude value
     * @param lon      longitude value
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setLatLon(int which, float lat, float lon)
            throws VisADException, RemoteException {
        setLatLon(which, lat, lon, true);
    }


    /**
     *  Set the value of the lat/lon point based on the
     *  given which (either POINT_START or POINT_END).
     *  Update the position of the CrossSectionSelector if updateSelector is true
     *  and recalculate the range/bearing values
     *
     * @param which    which point (start or end)
     * @param lat      latitude value
     * @param lon      longitude value
     * @param updateSelector   true to update the selector
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setLatLon(int which, float lat, float lon,
                           boolean updateSelector)
            throws VisADException, RemoteException {
        if (which == POINT_END) {
            //Do nothing when no change
            if ((endLat == lat) && (endLon == lon)) {
                return;
            }
            endLat = lat;
            endLon = lon;
        } else {
            //Do nothing when no change
            if ((startLat == lat) && (startLon == lon)) {
                return;
            }
            startLat = lat;
            startLon = lon;
        }
        setRangeAndBearing();
        if (updateSelector) {
            //Tell the selector to move
            RealTuple rt = getRealTupleForPoint(lat, lon);
            if (rt != null) {
                //Block move events from selector, though these can happen later due to threading
                acceptEventsFromSelector = false;
                csSelector.setPoint(which, rt);
                transectPositionChanged(false);
                acceptEventsFromSelector = true;
            }
        }
    }

    /**
     * Sets the position of the CrossSectionSelector based on the
     * lat/lon attributes
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setSelectorPosition()
            throws VisADException, RemoteException {
        csSelector.setPosition(getRealTupleForPoint(startLat, startLon),
                               getRealTupleForPoint(endLat, endLon));
    }

    /**
     * Converts the given lat/lon locations to a RealTuple
     *
     * @param rlat    latitude
     * @param rlon    longitude
     * @return   XY position
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private RealTuple getRealTupleForPoint(float rlat, float rlon)
            throws VisADException, RemoteException {
        if (true) {
            return new RealTuple(RealTupleType.SpatialEarth3DTuple,
                                 new double[] { rlon,
                    rlat, getSelectorAltitude() });
        }


        RealTuple visadTup = earthToBoxTuple(new EarthLocationTuple(rlat,
                                 rlon, 0.0));
        Real[] reals = visadTup.getRealComponents();
        return new RealTuple(new Real[] { reals[0], reals[1] });
    }


    /**
     * Set the starting latitude property.  Used by XML persistence.
     *
     * @param value   starting latitude
     */
    public void setStartLat(float value) {
        startLat = value;
    }

    /**
     * Get the starting latitude property.  Use by XML persistence.
     *
     * @return  starting latitude
     */
    public float getStartLat() {
        return startLat;
    }

    /**
     * Set the starting longitude property.  Used by XML persistence.
     *
     * @param value   starting longitude
     */
    public void setStartLon(float value) {
        startLon = value;
    }

    /**
     * Set the starting longitude property.  Used by XML persistence.
     *
     * @return   starting longitude
     */
    public float getStartLon() {
        return startLon;
    }

    /**
     * Set the ending latitude property.  Used by XML persistence.
     *
     * @param value   ending latitude
     */
    public void setEndLat(float value) {
        endLat = value;
    }

    /**
     * Get the ending latitude property.  Used by XML persistence.
     *
     * @return   ending latitude
     */
    public float getEndLat() {
        return endLat;
    }

    /**
     * Set the ending longitude property.  Used by XML persistence.
     *
     * @param value  ending longitude
     */
    public void setEndLon(float value) {
        endLon = value;
    }

    /**
     * Get the ending longitude property.  Used by XML persistence.
     *
     * @return  ending longitude
     */
    public float getEndLon() {
        return endLon;
    }

    /**
     * Set the starting point
     *
     * @param rt   start point
     */
    public void setStartPoint(RealTuple rt) {
        //Dummy method so we don't get persistence errors
    }

    /**
     * Set the ending point
     *
     * @param rt   end point
     */
    public void setEndPoint(RealTuple rt) {
        //Dummy method so we don't get persistence errors
    }

    /**
     * Get the starting station property.
     *
     * @return  starting station
     */
    public NamedStationImpl getStartStation() {
        return startStation;
    }

    /**
     * Set the starting station property.
     *
     * @param n   starting station
     */
    public void setStartStation(NamedStationImpl n) {
        startStation = n;
    }

    /**
     * Get the ending station property.
     *
     * @return   ending station
     */
    public NamedStationImpl getEndStation() {
        return endStation;
    }

    /**
     * Set the ending station property.
     *
     * @param n   ending station
     */
    public void setEndStation(NamedStationImpl n) {
        endStation = n;
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
     *  Set the Planet property.
     *
     *  @param value The new value for Planet
     */
    public void setPlanet(Earth value) {
        planet = value;
    }

    /**
     *  Get the Planet property.
     *
     *  @return The Planet
     */
    public Earth getPlanet() {
        return planet;
    }





}
