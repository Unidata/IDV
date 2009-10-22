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
import ucar.unidata.util.GuiUtils;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.functiontypes.AirTemperatureProfile;
import ucar.visad.functiontypes.CartesianHorizontalWindOfPressure;
import ucar.visad.functiontypes.DewPointProfile;
import ucar.visad.functiontypes.InSituAirTemperatureProfile;

import visad.*;

import visad.georef.LatLonPoint;
import visad.georef.NamedLocationTuple;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * <p>A {@link AerologicalSoundingControl} for RAOB data.</p>
 *
 * @author IDV development team
 * @version $Revision: 1.11 $Date: 2006/12/01 20:16:37 $
 */
public class RaobSoundingControl extends AerologicalSoundingControl {

    /** array of station ids */
    // private String[] stationIds;
    private List stations;

    /** _more_ */
    Hashtable<String, List> stationsTimes;

    /** _more_ */
    Hashtable<String, Tuple> stationsTuple;

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
    private JComboBox stationMenu;

    /** _more_ */
    private JComboBox stMenu;

    /**
     *  The selectedStationIndex property.
     */
    private int selectedStationIndex = -1;

    /** _more_ */
    private DisplayableData timesHolder;

    /** _more_ */
    private Component widget;

    /** _more_ */
    private SoundingDataNode dataNode;

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
    public boolean init(DataChoice dataChoice) throws VisADException,
            RemoteException {

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

        Tuple entries = (Tuple) getDataInstance().getData();


        // stationIds = new String[tempPros.length];
        int length = entries.getDimension();
        stations = new ArrayList();
        latLons  = new LatLonPoint[length];
        Hashtable<String, List> stationsTuples = new Hashtable<String,
                                                     List>();
        stationsTuple = new Hashtable<String, Tuple>();
        stationsTimes = new Hashtable<String, List>();
        int j = 0;
        for (int i = 0; i < length; i++) {
            Tuple ob = (Tuple) entries.getComponent(i);
            //            System.out.println("ob has " + ob.getDimension() + " components");

            NamedLocationTuple station =
                (NamedLocationTuple) ob.getComponent(1);
            String stName = station.getIdentifier().toString();

            if ( !stations.contains(stName)) {
                stations.add(stName);
                latLons[j++] = station.getLatLonPoint();
            }
            List<DateTime> timeList  = stationsTimes.get(stName);
            List<Data>     tupleList = stationsTuples.get(stName);
            if (tupleList == null) {
                tupleList = new ArrayList<Data>();
                stationsTuples.put(stName, tupleList);
            }
            if (timeList == null) {
                timeList = new ArrayList<DateTime>();
                stationsTimes.put(stName, timeList);
            }
            timeList.add(((DateTime) ob.getComponent(0)));
            tupleList.add((Data) ob);

        }

        for (int i = 0; i < stations.size(); i++) {
            String     st        = (String) stations.get(i);
            List<Data> tuples    = stationsTuples.get(st);
            Data[]     tpData    = tuples.toArray(new Data[tuples.size()]);
            TupleType  tupleType = Tuple.buildTupleType(tpData);
            Tuple      tp        = new Tuple(tupleType, tpData);
            stationsTuple.put(st, tp);
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
                        if ((i >= 0) && (stMenu != null)) {
                            selectedStation.setPoint((RealTuple) latLons[i]);
                            //  stationMenu.setSelectedIndex(i);
                            stMenu.setSelectedIndex(i);
                        }
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            }
        });

        setPointSize();

        String stName = (String) stations.get(0);
        //  setSoundings(tempPros, dewPros, windPros);
        addDisplayable(stationProbes, FLAG_COLOR);
        addDisplayable(selectedStation, FLAG_COLOR);
        if (getSelectedStationIndex() >= 0) {
            selectedStation.setPoint(
                (RealTuple) latLons[getSelectedStationIndex()]);
        }


        dataNode = SoundingDataNode.getInstance((new Listener()));
        Tuple tp = stationsTuple.get(stName);
        dataNode.setData(tp);
        //    List sts = stationsTimes.get(stName);
        //    dataNode.setTime((DateTime)sts.get(0));
        //    timeChanged((DateTime)sts.get(0)) ;
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
        // stationIds    = null;
        stations      = null;
        data          = null;
        latLons       = null;
        stationProbes = null;
        stationMenu   = null;
    }


    /**
     * _more_
     *
     * @param time _more_
     */
    protected void timeChanged(Real time) {
        try {
            super.timeChanged(time);
            dataNode.setTime(new DateTime(time));
        } catch (Exception ex) {
            logException("timeValueChanged", ex);
        }

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
    public int getSelectedStationIndex_Old() {
        if (stationMenu != null) {
            return stationMenu.getSelectedIndex();
        }
        return -1;
    }

    /**
     * Get the SelectedStationIndex property.
     *
     * @return The SelectedStationIndex
     */
    public int getSelectedStationIndex() {
        if (stMenu != null) {
            return stMenu.getSelectedIndex();
        }
        return 0;
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
    public void setSpatialLociVisible(boolean visible) throws VisADException,
            RemoteException {
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
    Component getSpecificWidget() {
        return widget;
    }



    /**
     * Set the station for the index
     *
     * @param index   station index.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setStation(int index) throws VisADException,
            RemoteException {
        selectedStation.setPoint((RealTuple) latLons[index]);
        //   setSounding(index);
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
    private Field addPressure(Field windField,
                              Field tempField) throws VisADException,
                                  RemoteException {
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
        Data           data    = null;
        int            timeIdx = getCurrentIdx();
        int            index   = getSelectedStationIndex();
        List<DateTime> times   = stationsTimes.get(stations.get(index));
        String         timeStr = times.get(timeIdx).toString();
        if (index >= 0) {
            String label = (String) stations.get(index) + " " + timeStr;
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
        int            timeIdx = getCurrentIdx();
        int            index   = getSelectedStationIndex();
        List<DateTime> times   = stationsTimes.get(stations.get(index));
        String         timeStr = times.get(timeIdx).toString();
        if (index >= 0) {
            headerLabel.setText(stations.get(index) + " " + timeStr);
        } else {
            super.updateHeaderLabel();
        }
    }



    /**
     * Provides support for receiving output from a {@link SoundingDataNode}.
     *
     * @author Steven R. Emmerson
     * @version $Revision: 1.14 $ $Date: 2007/03/09 11:55:55 $
     */
    private class Listener implements RaobSoundingDataNode.Listener {

        /**
         * Listener for data
         *
         * @throws RemoteException  Java RMI error
         * @throws VisADException   VisAD Error
         *
         */
        private Listener() throws VisADException, RemoteException {}

        /**
         * <p>Sets the time-index of the current profiles.</p>
         *
         * @param index              The time-index of the current profiles.
         * @param source
         * @throws VisADException    if a VisAD failure occurs.
         * @throws RemoteException   if a Java RMI failure occurs.
         */
        public void setTimeIndex(
                int index,
                SoundingDataNode source) throws VisADException,
                    RemoteException {
            setSounding(index);
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws RemoteException _more_
         * @throws VisADException _more_
         */
        public int getTimeIndex() throws VisADException, RemoteException {
            return getCurrentIdx();
        }

        /**
         * Sets the set of times of all the profiles.  The set will contain one
         * or more times as double values, in order, from earliest to latest.
         *
         * @param times              The times of all the profiles.
         * @param source
         * @throws VisADException    if a VisAD failure occurs.
         * @throws RemoteException   if a Java RMI failure occurs.
         */
        public void setTimes(SampledSet times,
                             SoundingDataNode source) throws VisADException,
                                 RemoteException {

            final Object[] ids = stations.toArray();
            RealType timeType =
                (RealType) ((SetType) times.getType()).getDomain()
                    .getComponent(0);

            // use a LineDrawing because it's the simplest DisplayableData
            if (timesHolder == null) {
                timesHolder = new LineDrawing("times ref");
            }
            stMenu = new JComboBox(ids);
            stMenu.setToolTipText("Stations");

            stMenu.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    try {
                        int    i  = stMenu.getSelectedIndex();
                        String st = (String) stations.get(i);
                        dataNode.setData(stationsTuple.get(st));
                        int index = getSelectedStationIndex();
                        List<DateTime> times =
                            stationsTimes.get(stations.get(index));
                        DateTime dt = times.get(0);
                        dataNode.setTime(dt);
                        //System.out.println("here " + st + " "+  stationsTuple.get(st).getLength()) ;
                        updateHeaderLabel();

                        setStation(stMenu.getSelectedIndex());
                    } catch (Exception ex) {
                        logException(ex);
                    }
                }
            });
            /*
             * Add a data object to the display that has the right
             * time-centers.
             */
            Field dummy = new FieldImpl(new FunctionType(timeType,
                              AirTemperatureProfile.instance()), times);

            for (int i = 0, n = times.getLength(); i < n; i++) {
                dummy.setSample(
                    i, AirTemperatureProfile.instance().missingData());
            }

            timesHolder.setData(dummy);

            if (times.getLength() == 1) {
                DateTime time = new DateTime(new Real(timeType,
                                    times.indexToDouble(new int[] {
                                        0 })[0][0], times.getSetUnits()[0]));


                widget = GuiUtils.topBottom(
                    GuiUtils.inset(GuiUtils.label("Station: ", stMenu), 8),
                    GuiUtils.wrap(new JLabel(time.toString())));
                dataNode.setTime(time);
                setSounding(0);
            } else {
                /*
                 * Set the animation.
                 */
                Animation animation = getInternalAnimation(timeType);
                getSoundingView().setExternalAnimation(animation,
                        getAnimationWidget());
                aeroDisplay.addDisplayable(animation);
                aeroDisplay.addDisplayable(timesHolder);

                Container container = Box.createHorizontalBox();
                //Wrap these components so they don't get stretched in the Y direction
                container.add(
                    GuiUtils.wrap(getAnimationWidget().getContents(false)));
                //container.add(GuiUtils.wrap (animationWidget.getIndicatorComponent()));
                widget = GuiUtils.topBottom(
                    GuiUtils.inset(GuiUtils.label("Station: ", stMenu), 8),
                    container);

            }
        }

        /**
         * Sets the location of the current profiles.
         *
         * @param loc                The current location.
         * @param source
         *
         * @throws RemoteException
         * @throws VisADException
         */
        public void setLocation(
                LatLonPoint loc,
                SoundingDataNode source) throws VisADException,
                    RemoteException {
            //setLocation11(loc);
            //location = loc;
            double lat = loc.getLatitude().getValue();

            aeroDisplay.setBarbOrientation((lat >= 0)
                                           ? FlowControl.NH_ORIENTATION
                                           : FlowControl.SH_ORIENTATION);

        }

        /**
         * <p>Sets the set of locations of the profiles.  The set will contain
         * one or more {@link visad.georef.EarthLocationTuple}-s.</p>
         *
         * <p>This implementation currently does nothing.</p>
         *
         * @param locs               The locations of the profiles.
         * @param source
         *
         * @throws RemoteException
         * @throws VisADException
         */
        public void setLocations(
                SampledSet locs,
                SoundingDataNode source) throws VisADException,
                    RemoteException {
            //setData(locs);
        }

        /**
         * Sets the set of profiles.
         *
         * @param tempPros        The temperature profiles.
         * @param dewPros         The dew-point profiles.
         * @param windPros        The wind profiles.
         * @param source          The source of the profiles.
         * @throws VisADException           if a VisAD failure occurs.
         * @throws RemoteException          if a Java RMI failure occurs.
         */
        public void setProfiles(
                Field[] tempPros, Field[] dewPros, Field[] windPros,
                SoundingDataNode source) throws VisADException,
                    RemoteException {
            setSoundings(tempPros, dewPros, windPros);
        }
    }


}

