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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.sounding.CosmicTrajectoryFeatureTypeInfo;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.data.sounding.TrajectoryFeatureTypeAdapter;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.visad.Util;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.IndicatorPoint;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.PickableLineDrawing;

import visad.*;

import visad.georef.LatLonPoint;
import visad.georef.NamedLocationTuple;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Oct 5, 2009
 * Time: 3:09:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrajectoryFeatureTypeSoundingControl extends AerologicalSoundingControl {

    /** Displayable for the track */
    private final DisplayableData track;

    /** time at bottom of profile */
    private DateTime bottomTime;

    /** station probes */
    private PickableLineDrawing stationProbes;

    /** array of data lat/longs */
    private LatLonPoint[] latLons;



    /** selection indicator */
    private IndicatorPoint selectedStation;

    /** _more_ */
    private JComboBox stationMenue;

    /** _more_          */
    private boolean ignoreStationMenuEvent = false;


    /** _more_ */
    private String[] stationIds;

    /** _more_ */
    private int selectedStationIndex = -1;

    /** _more_ */
    private List<Data[]> dataList;

    /** _more_          */
    private List<DateTime> timeList;

    /**
     * Constructs from nothing.
     *
     * @throws visad.VisADException  if a VisAD failure occurs.
     * @throws java.rmi.RemoteException if a Java RMI failure occurs.
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public TrajectoryFeatureTypeSoundingControl()
            throws VisADException, RemoteException {

        super(true);

        track = new LineDrawing("TrajectoryFeatureTypeSoundingControl track");
        track.setPointSize(2f);

        stationProbes = new PickableLineDrawing("RAOB Points");
        selectedStation =
            new IndicatorPoint("Selected Station",
                               RealTupleType.LatitudeLongitudeTuple);
    }

    /**
     * Creates the display and control buttons from a {@link
     * ucar.unidata.data.DataChoice}.
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

        setSpatialLoci(track);
        setSpatialLoci(stationProbes);

        if ( !setData(dataChoice)) {
            return false;
        }
        List<DataSource> dsList = new ArrayList();
        dataChoice.getDataSources(dsList);
        TrackDataSource ds       = (TrackDataSource) dsList.get(0);
        List            adapters = ds.getAdapters();

        try {
            initDataChoice(adapters);
        } catch (Exception e) {}
        initSounding(dataList.get(0));

        stationProbes.setData(Util.indexedField(latLons, false));
        stationProbes.addAction(new ActionImpl("Station Probe Action") {
            private boolean first = true;
            public void doAction() {
                if (first) {
                    first = false;
                } else {
                    Misc.run(new Runnable() {
                        public void run() {
                            try {
                                int i = stationProbes.getCloseIndex();
                                if ((i >= 0) && (stationMenue != null)) {
                                    ignoreStationMenuEvent = true;
                                    selectedStation.setPoint(
                                        (RealTuple) latLons[i]);
                                    stationMenue.setSelectedIndex(i);
                                    setStation(i);
                                }
                            } catch (Exception ex) {
                                logException(ex);
                            } finally {
                                ignoreStationMenuEvent = false;
                            }
                        }
                    });
                }
            }
        });

        setPointSize();

        addDisplayable(stationProbes, FLAG_COLOR);
        addDisplayable(selectedStation, FLAG_COLOR);

        if (getSelectedStationIndex() >= 0) {
            selectedStation.setPoint(
                (RealTuple) latLons[getSelectedStationIndex()]);
        }
        updateHeaderLabel();
        return true;
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
     * _more_
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
     * _more_
     *
     * @param adapters _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public boolean initDataChoice(List<TrajectoryFeatureTypeAdapter> adapters)
            throws VisADException, RemoteException, Exception {

        int len = adapters.size();
        stationIds = new String[len];
        latLons    = new LatLonPoint[len];
        dataList   = new ArrayList();
        timeList   = new ArrayList();

        for (int i = 0; i < len; i++) {
            TrajectoryFeatureTypeAdapter          cta   = adapters.get(i);
            List<CosmicTrajectoryFeatureTypeInfo> infos = cta.getTrackInfos();
            CosmicTrajectoryFeatureTypeInfo       cfti  = infos.get(0);
            Data[] sddata = cfti.getAerologicalDiagramDataArray();
            dataList.add(sddata);
            NamedLocationTuple s = cfti.getLatLonPoint();
            latLons[i]    = s.getLatLonPoint();
            stationIds[i] = s.getIdentifier().getValue();
            timeList.add(cfti.getStartTime());
        }

        return true;

    }



    /**
     * _more_
     *
     * @param sounding _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public boolean initSounding(Data[] sounding)
            throws VisADException, RemoteException {

        Field tempPro = (Field) sounding[0];
        Field dewPro  = (Field) sounding[1];

        setSoundings(new Field[] { tempPro }, new Field[] { dewPro },
                     new Field[] { null });
        //LOOK:        setSounding(0);
        //        addDisplayable(track, FLAG_COLOR);
        return true;
    }


    /**
     * Override the base class method to return the relevant name
     * @return  the text for the label
     */
    protected String getSpatialLociLabel() {
        return "Track";
    }

    /**
     * <p>Returns the data-specific widget for controlling the data-specific
     * aspects of the display.</p>
     *
     * @return                      The data-specific control-component.
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    Component getSpecificWidget() throws VisADException, RemoteException {

        stationMenue = new JComboBox(stationIds);

        //TODO: Check this
        if ((selectedStationIndex >= 0)
                && (selectedStationIndex < stationIds.length)) {
            stationMenue.setSelectedIndex(selectedStationIndex);
            setStation(selectedStationIndex);
        } else {
            setStation(0);
        }
        stationMenue.setToolTipText("Soundings");



        stationMenue.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                try {
                    //                    System.err.println("station menu changed");
                    //                    setStation(stationMenue.getSelectedIndex());
                    updateHeaderLabel();
                } catch (Exception ex) {
                    logException(ex);
                }
            }
        });


        stationMenue.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (ignoreStationMenuEvent) {
                    return;
                }
                Misc.run(new Runnable() {
                    public void run() {
                        try {
                            setStation(stationMenue.getSelectedIndex());
                            Misc.runInABit(250, new Runnable() {
                                public void run() {
                                    stationMenue.requestFocus();
                                }
                            });
                            updateHeaderLabel();
                        } catch (Exception ex) {
                            logException(ex);
                        }
                    }
                });

            }
        });

        return GuiUtils.top(GuiUtils.inset(GuiUtils.label("Soundings: ",
                stationMenue), 8));
    }

    /**
     * _more_
     *
     * @param index _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    private void setStation(int index)
            throws VisADException, RemoteException {
        selectedStation.setPoint((RealTuple) latLons[index]);
        setLocation(latLons[index]);
        initSounding(dataList.get(index));
        getSoundingView().updateDisplayList();
    }

    /**
     * Update the location label, subclasses can override.
     */
    protected void updateHeaderLabel() {
        int timeIdx = getCurrentIdx();
        int index   = getSelectedStationIndex();
        //List<DateTime> times   = stationsTimes.get(stations.get(index));
        // if(timeIdx >= times.size())
        //     timeIdx = times.size()-1;
        //  String         timeStr = times.get(timeIdx).toString();
        if (index >= 0) {
            headerLabel.setText(stationIds[index]);
        } else {
            headerLabel.setText(stationIds[0]);
        }
    }

    /**
     * Add any macro name/label pairs
     *
     * @param names List of macro names
     * @param labels List of macro labels
     */
    protected void getMacroNames(List names, List labels) {
        super.getMacroNames(names, labels);
        names.addAll(Misc.newList(MACRO_STATION));
        labels.addAll(Misc.newList("Station"));
    }

    /**
     * Add any macro name/value pairs.
     *
     *
     * @param template template
     * @param patterns The macro names
     * @param values The macro values
     */
    protected void addLabelMacros(String template, List patterns,
                                  List values) {
        super.addLabelMacros(template, patterns, values);
        int index = getSelectedStationIndex();
        if (index >= 0) {
            patterns.add(MACRO_STATION);
            values.add("" + stationIds[index]);
        }
    }



}
