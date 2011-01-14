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


import ucar.unidata.data.CompositeDataChoice;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.idv.TimeHeightViewManager;
import ucar.unidata.idv.ViewDescriptor;
import ucar.unidata.idv.ViewManager;

import ucar.unidata.metdata.NamedStation;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;


import ucar.visad.display.*;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.*;
import java.util.List;

import javax.swing.*;


/**
 * Given an earth-located VisAD Field of one or more
 * NOAA Profiler Network staiton data,
 * make a 2D plot of the wind data against height and time, and make related controls.
 *
 * Uses data as a VisAD FieldImpl with VisAD function (Time -> ((Z) -> (DIR, SPD))
 * where time values are
 * VisAD Datetime objects, Z has RealType RealType.Altitude, DIR and
 * SPD have RealType Display.Flow1Azimuth and Display.Flow1Radial,
 *
 * The data display is in its own window, not the main IDV view manager.
 *
 * @author Unidata IDV development
 * @version $Revision: 1.38 $
 */
public class ProfilerTimeHeightControl extends WindTimeHeightControl {

    /** line probe */
    private LineProbe lineProbe;

    /** stations combobox */
    private JComboBox stationsBox;

    /** foreground color */
    private Color foreground;

    /** background color */
    private Color background;


    /**
     *  Default Constructor; does nothing. See init() for creation actions.
     */
    public ProfilerTimeHeightControl() {}

    /**
     * Construct the DisplayMaster, Displayable, frame, and controls.
     *
     * @param dataChoice the DataChoice to use
     * @return boolean true if DataChoice is ok.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        boolean result = super.init(dataChoice);

        if (foreground != null) {
            timeHeightView.setColors(foreground, background);
        }
        return result;
    }

    /**
     * get data field; load in displayable; make LineProbe in place
     *
     * @param dataChoice    choice for selecting data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean initStation(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !super.initStation(dataChoice)) {
            return false;
        }

        // start new thread to get data etc.
        Misc.run(new Runnable() {
            public void run() {
                showWaitCursor();
                try {
                    if (getDataInstance() == null) {
                        return;
                    }
                    fieldImpl = (FieldImpl) getDataInstance().getData();
                    if (fieldImpl == null) {
                        return;
                    }
                    haveNewStation = true;
                    EarthLocation location = null;
                    Object        id       = currentDataChoice.getId();
                    if (id instanceof NamedStation) {
                        location = ((NamedStation) id).getNamedLocation();
                    } else if (id instanceof EarthLocation) {
                        location = (EarthLocation) id;
                    }
                    latitude = (float) location.getLatitude().getValue(
                        CommonUnit.degree);
                    longitude = (float) location.getLongitude().getValue(
                        CommonUnit.degree);
                    // set NWS wind barb style by latitude
                    if (latitude >= 0.0) {
                        // set NWS wind barb style to that used in
                        // the northern hemisphere
                        wbDisplayable.setBarbOrientation(
                            FlowControl.NH_ORIENTATION);
                    } else {
                        wbDisplayable.setBarbOrientation(
                            FlowControl.SH_ORIENTATION);
                    }
                    labelName = " " + currentDataChoice.getId().toString();
                    setLabel();
                    // put the data to display in the displayable
                    wbDisplayable.loadData(fieldImpl);
                    resetDataVerticalInterval(currentVerticalInt);
                    // Set x (time) axis limits
                    // depending on data loaded in displayable.
                    setXAxisValues(fieldImpl.getDomainSet());
                    setYAxisValues(fieldImpl);
                    // mark Profiler location on the main IDV display
                    //lineProbe.setPosition 
                    //    (getRealTupleForPoint (latitude, longitude));

                } catch (Exception excp) {
                    logException("File selection", excp);
                }

                try {
                    lineProbe.setPosition(getRealTupleForPoint(latitude,
                            longitude));
                } catch (Exception excp) {
                    logException(" line probe set position ", excp);
                }

                showNormalCursor();
                haveNewStation = false;
            }
        });

        return true;
    }

    /**
     * Initialize the displayables.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected void initDisplayables() throws VisADException, RemoteException {
        super.initDisplayables();
        lineProbe = new LineProbe();
        lineProbe.setManipulable(false);
        lineProbe.setPointSize(getDisplayScale());
        lineProbe.setAutoSize(true);
        addDisplayable(lineProbe);
    }

    /**
     * Set the label for this
     */
    protected void setLabel() {
        if (plotLabel != null) {
            plotLabel.setText(labelName + " Profiler Lat: " + latitude
                              + "  Long: " + longitude);
        }
    }

    /**
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void doMakeColorScales()
            throws VisADException, RemoteException {
        colorScales = new ArrayList();
        if (colorScaleInfo == null) {
            colorScaleInfo = getDefaultColorScaleInfo();
        }
        ColorScale colorScale = new ColorScale(getColorScaleInfo());
        addDisplayable(colorScale, timeHeightView, FLAG_COLORTABLE);
        colorScales.add(colorScale);

    }


    /**
     * Add tabs to the properties dialog.
     *
     * @param jtp  the JTabbedPane to add to
     */
    public void addPropertiesComponents(JTabbedPane jtp) {
        super.addPropertiesComponents(jtp);

        if (timeHeightView != null) {
            jtp.add("Time Height View",
                    timeHeightView.getPropertiesComponent());
        }
    }

    /**
     * _more_
     *
     * @param menus _more_
     * @param forMenuBar _more_
     */
    protected void getViewMenuItems(List menus, boolean forMenuBar) {
        super.getViewMenuItems(menus, forMenuBar);
        menus.add(GuiUtils.MENU_SEPARATOR);
        if (forMenuBar) {
            JMenu csvMenu = timeHeightView.makeViewMenu();
            csvMenu.setText("Profiler View");
            menus.add(csvMenu);
        }
    }


    /**
     * make widgets for check box for latest data time on left of x axis, and
     * make selector for one of  the different stations.
     *
     * @param controlWidgets to fill
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);

        // make check box for latest data time on left of x axis
        JCheckBox toggle = new JCheckBox("", isLatestOnLeft);
        toggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                isLatestOnLeft = ((JCheckBox) e.getSource()).isSelected();
                try {
                    setXAxisValues();
                    setYAxisValues();
                } catch (VisADException ve) {
                    userMessage("couldn't set order");
                }
            }
        });

        // if have more than one station
        if (compositeDataChoice != null) {
            // make selector for one of  the different stations.
            stationsBox = new JComboBox(
                new Vector(compositeDataChoice.getDataChoices()));
            if (currentDataChoice != null) {
                stationsBox.setSelectedItem(currentDataChoice);
            }
            stationsBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {

                    try {
                        initStation(
                            (DataChoice) stationsBox.getSelectedItem());
                    } catch (Exception exc) {
                        logException("setting station", exc);

                    }
                }
            });

            controlWidgets.add(
                new WrapperWidget(
                    this, GuiUtils.rLabel("Stations: "), stationsBox,
                    GuiUtils.hflow(
                        Misc.newList(
                            GuiUtils.rLabel(" Latest Data on Left: "),
                            toggle))));
        } else {
            // have only one station
            controlWidgets.add(new WrapperWidget(this,
                    GuiUtils.rLabel("Latest data on left: "),
                    GuiUtils.leftCenter(toggle, GuiUtils.filler())));
        }
    }

    /**
     *  Implementation of {@link ucar.unidata.data.DataChangeListener}.
     */
    public synchronized void dataChanged() {
        super.dataChanged();
        try {
            if (stationsBox != null) {
                initStation((DataChoice) stationsBox.getSelectedItem());
            } else {
                initStation(getDataChoice());
            }
        } catch (Exception exc) {
            logException("dataChanged", exc);
        }
    }


    /**
     * Apply the preferences.  Used to pick up the date format changes.
     */
    public void applyPreferences() {
        super.applyPreferences();
        try {
            setXAxisValues();
        } catch (Exception exc) {
            logException("applyPreferences", exc);
        }
    }

    /**
     *  Set whether latest data is displayed on the left or right
     *  side of the plot.  Used by XML persistence mainly.
     *
     *  @param yesorno  <code>true</code> if want latest is on left.
     */
    public void setLatestOnLeft(boolean yesorno) {
        isLatestOnLeft = yesorno;
    }

    /**
     * Get whether latest data is displayed on the left or right
     * side of the plot.
     *
     * @return  <code>true</code> if latest is on left.
     */
    public boolean getLatestOnLeft() {
        return isLatestOnLeft;
    }

    /**
     * Get the foreground color
     *
     * @return the foreground color
     */
    public Color getForeground() {
        return timeHeightView.getForeground();
    }

    /**
     * Set the foreground color
     *
     * @param color    new color
     */
    public void setForeground(Color color) {
        this.foreground = color;
    }



    /**
     * Get the background color
     *
     * @return the background color
     */
    public Color getBackground() {
        return timeHeightView.getBackground();
    }

    /**
     * Set the background color
     *
     * @param color   new color
     */
    public void setBackground(Color color) {
        this.background = color;
    }

}
