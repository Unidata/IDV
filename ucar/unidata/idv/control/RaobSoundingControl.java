/*
 * $Id: RaobSoundingControl.java,v 1.11 2006/12/01 20:16:37 jeffmc Exp $
 *
 * Copyright  1997-2006 Unidata Program Center/University Corporation for
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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.util.GuiUtils;

import ucar.visad.Util;
import ucar.visad.display.CompositeDisplayable;
import ucar.visad.display.Displayable;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.DisplayableDataRef;
import ucar.visad.display.IndicatorPoint;
import ucar.visad.display.PickableLineDrawing;
import ucar.visad.functiontypes.CartesianHorizontalWindOfPressure;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.functiontypes.InSituAirTemperatureProfile;
import ucar.visad.quantities.AirPressure;
import ucar.visad.quantities.Altitude;
import ucar.visad.quantities.GeopotentialAltitude;

import visad.*;

import visad.bom.PickManipulationRendererJ3D;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;
import visad.georef.NamedLocationTuple;



import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import javax.swing.Box;
import javax.swing.JComboBox;


/**
 * <p>A {@link AerologicalSoundingControl} for RAOB data.</p>
 *
 * @author IDV development team
 * @version $Revision: 1.11 $Date: 2006/12/01 20:16:37 $
 */
public class RaobSoundingControl extends AerologicalSoundingControl {

    /** array of station ids */
    private String[] stationIds;

    /** template math type */
    private static final MathType templateType;

    /** the data */
    private Data data;

    /** array of data lat/longs */
    private LatLonPoint[] latLons;

    /** station probes */
    private PickableLineDrawing stationProbes;

    /** selection indicator */
    private IndicatorPoint selectedStation;

    /** station selector */
    private JComboBox stationMenue;

    /**
     *  The selectedStationIndex property.
     */
    private int selectedStationIndex = -1;

    static {
        try {
            templateType = new TupleType(new MathType[] { RealType.Time,
                    RealTupleType.LatitudeLongitudeAltitude,
                    InSituAirTemperatureProfile.instance(),
                    DewPointProfile.instance(),
                    CartesianHorizontalWindOfPressure.instance() });
        } catch (Exception ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    /**
     * Constructs from nothing.
     *
     * @throws VisADException  if a VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public RaobSoundingControl() throws VisADException, RemoteException {

        super(true);

        stationProbes = new PickableLineDrawing("RAOB Points");
        selectedStation =
            new IndicatorPoint("Selected Station",
                               RealTupleType.LatitudeLongitudeTuple);
    }

    /**
     * Initializes this instance with a {@link ucar.unidata.data.DataChoice}.
     *
     * @param dataChoice       The data for this instance.
     * @return                 <code>true</code> if and only if this instance
     *                         was correctly initialized.
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException couldn't create a remote object needed
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        /*
         * Initialize the superclass.
         */
        if ( !super.init()) {
            return false;
        }

        setSpatialLoci(stationProbes);

        if ( !setData(dataChoice)) {
            return false;
        }

        Tuple   entries  = (Tuple) getDataInstance().getData();
        Field[] tempPros = new Field[entries.getDimension()];
        Field[] dewPros  = new Field[tempPros.length];
        Field[] windPros = new Field[tempPros.length];
        float[] lats     = new float[tempPros.length];
        float[] lons     = new float[tempPros.length];
        Unit    latUnit  = RealType.Latitude.getDefaultUnit();
        Unit    lonUnit  = RealType.Longitude.getDefaultUnit();

        stationIds = new String[tempPros.length];
        latLons    = new LatLonPoint[tempPros.length];

        for (int i = 0; i < stationIds.length; i++) {
            Tuple ob = (Tuple) entries.getComponent(i);
            //            System.out.println("ob has " + ob.getDimension() + " components");

            tempPros[i] = (Field) ob.getComponent(2);
            dewPros[i]  = (Field) ob.getComponent(3);
            windPros[i] = addPressure((Field) ob.getComponent(4),
                                      tempPros[i]);

            NamedLocationTuple station =
                (NamedLocationTuple) ob.getComponent(1);

            stationIds[i] = station.getIdentifier().toString() + " "
                            + ((DateTime) ob.getComponent(0)).toString();
            latLons[i] = station.getLatLonPoint();
        }

        stationProbes.setData(Util.indexedField(latLons, false));
        stationProbes.addAction(new ActionImpl("Station Probe Action") {
            private boolean first = true;
            public void doAction() {
                if (first) {
                    first = false;
                } else {
                    try {
                        int i = stationProbes.getCloseIndex();
                        if ((i >= 0) && (stationMenue != null)) {
                            selectedStation.setPoint((RealTuple) latLons[i]);
                            stationMenue.setSelectedIndex(i);
                        }
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            }
        });
        setPointSize();

        setSoundings(tempPros, dewPros, windPros);
        addDisplayable(stationProbes, FLAG_COLOR);
        addDisplayable(selectedStation, FLAG_COLOR);
        if (getSelectedStationIndex() >= 0) {
            selectedStation.setPoint(
                (RealTuple) latLons[getSelectedStationIndex()]);
        }

        return true;
    }

    /**
     * Remove this control. Call the parent  class doRemove and clears
     * references to data, etc.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doRemove() throws VisADException, RemoteException {
        super.doRemove();
        stationIds    = null;
        data          = null;
        latLons       = null;
        stationProbes = null;
        stationMenue  = null;
    }


    /**
     *  Set the SelectedStationIndex property.
     *
     *  @param value The new value for SelectedStationIndex
     */
    public void setSelectedStationIndex(int value) {
        selectedStationIndex = value;
    }


    /**
     *  Get the SelectedStationIndex property.
     *
     *  @return The SelectedStationIndex
     */
    public int getSelectedStationIndex() {
        if (stationMenue != null) {
            return stationMenue.getSelectedIndex();
        }
        return -1;
    }


    /**
     * Override the base class method to return the relevant name
     * @return   get the label for the spatial loci selector
     */
    protected String getSpatialLociLabel() {
        return "Stations";
    }


    /**
     * Sets the visibility of the spatial loci in the main, 3D window.
     *
     * @param visible           If true, then the loci will be rendered visible;
     *                          otherwise, they will be rendered invisible.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public void setSpatialLociVisible(boolean visible)
            throws VisADException, RemoteException {
        super.setSpatialLociVisible(visible);
        selectedStation.setVisible(getDisplayVisibility() && visible);
    }

    /**
     * <p>Returns the data-specific widget for controlling the data-specific
     * aspects of the display so that it can be added to the window in the
     * appropriate place.</p>
     *
     * @return                      The data-specific control-widget.
     * @throws VisADException       if a VisAD failure occurs.
     * @throws RemoteException      if a Java RMI failure occurs.
     */
    Component getSpecificWidget() throws VisADException, RemoteException {

        stationMenue = new JComboBox(stationIds);

        if (selectedStationIndex >= 0) {
            stationMenue.setSelectedIndex(selectedStationIndex);
            setStation(selectedStationIndex);
        } else {
            setStation(0);
        }
        stationMenue.setToolTipText("Soundings");



        stationMenue.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                try {
                    setStation(stationMenue.getSelectedIndex());
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });

        return GuiUtils.top(GuiUtils.inset(GuiUtils.label("Soundings: ",
                stationMenue), 8));
    }

    /**
     * Set the station for the index
     *
     * @param index   station index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setStation(int index)
            throws VisADException, RemoteException {
        selectedStation.setPoint((RealTuple) latLons[index]);
        setSounding(index);
        setLocation(latLons[index]);
    }

    /**
     * Change the domain of the wind field which should be
     * GeopotentialAltitude with CS to Altitude, to one with
     * Pressure with CS to Altitude, using the CS in the temp field.
     *
     * @param windField windField to modify
     * @param tempField temperature field with type and CS
     *
     * @return modified wind field
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException problem creating the new set
     */
    private Field addPressure(Field windField, Field tempField)
            throws VisADException, RemoteException {
        return Util.convertDomain(
            windField, ((FunctionType) tempField.getType()).getDomain(),
            tempField.getDomainCoordinateSystem());
    }

    /**
     * Set the size of the points
     */
    private void setPointSize() {
        try {
            stationProbes.setPointSize(DEFAULT_POINT_SIZE);
            selectedStation.setPointSize(DEFAULT_POINT_SIZE * 2);
        } catch (Exception exc) {
            logException("Set point size ", exc);
        }
    }

    /**
     * Add the data to the in display legend
     *
     * @return the data for the display list displayable
     */
    protected Data getDisplayListData() {
        Data data  = null;
        int  index = getSelectedStationIndex();
        if (index >= 0) {
            String label = stationIds[index];
            try {
                TextType tt = TextType.getTextType(DISPLAY_LIST_NAME);
                data = new Text(tt, label);
            } catch (VisADException ve) {
                logException("getting display list data ", ve);
            }
        }
        return data;
    }

    /**
     * Update the location label, subclasses can override.
     */
    protected void updateHeaderLabel() {
        int index = getSelectedStationIndex();
        if (index >= 0) {
            headerLabel.setText(stationIds[index]);
        } else {
            super.updateHeaderLabel();
        }
    }
}

