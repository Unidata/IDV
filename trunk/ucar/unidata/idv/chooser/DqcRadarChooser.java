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

package ucar.unidata.idv.chooser;


import org.w3c.dom.Element;


import ucar.nc2.thredds.DqcRadarDatasetCollection;
import ucar.nc2.units.DateUnit;


import ucar.unidata.data.radar.CDMRadarDataSource;
import ucar.unidata.data.radar.RadarQuery;

import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.metdata.NamedStation;
import ucar.unidata.metdata.NamedStationImpl;

import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.DatedThing;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import visad.CommonUnit;
import visad.DateTime;

import java.awt.*;
import java.awt.event.*;


import java.net.URI;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;





/**
 * A chooser for the DQC radar collection
 *
 * @author IDV development team
 * @version $Revision: 1.18 $Date: 2007/07/18 20:44:37 $
 */

public class DqcRadarChooser extends TimesChooser {

    /**
     * Holds the main gui contents. We keep this around so we can replace it with an error message
     * when the connection to the service fails.
     */
    private JComponent outerContents;


    /** The collection */
    private DqcRadarDatasetCollection collection;


    /** The currently selected station */
    private NamedStation selectedStation;

    /** The fixed (for now) url we connect to */
    private String collectionUrl =
        "http://motherlode.ucar.edu:8080/thredds/idd/radarLevel2";


    /**
     * Create the RadarChooser
     *
     * @param mgr The <code>IdvChooserManager</code>
     * @param root  The xml root that defines this chooser
     *
     */
    public DqcRadarChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
    }



    /**
     * Handle the update event. Just pass it through to the imageChooser
     */
    public void doUpdate() {
        if (collection == null) {
            return;
        }
        Misc.run(this, "stationChanged");
    }



    /**
     * Update the status of the gui
     */
    protected void updateStatus() {
        super.updateStatus();
        if (selectedStation == null) {
            setHaveData(false);
            setStatus("Please select a station", "stationmap");
            return;
        }
        boolean haveTimesSelected;
        if (getDoAbsoluteTimes()) {
            haveTimesSelected = getSelectedAbsoluteTimes().size() > 0;
        } else {
            haveTimesSelected = true;
        }
        setHaveData(haveTimesSelected);
        if (haveTimesSelected) {
            setStatus("Press \"" + CMD_LOAD
                      + "\" to load the selected radar data", "buttons");
        } else {
            setStatus("Please select times", "times");
        }
    }


    /**
     * Handle when there are newly selected stations
     *
     * @param stations list of newly selected stations
     */
    protected void newSelectedStations(List stations) {
        super.newSelectedStations(stations);
        if ((stations == null) || (stations.size() == 0)) {
            selectedStation = null;
        } else {
            NamedStation newStation = (NamedStation) stations.get(0);
            if (Misc.equals(newStation, selectedStation)) {
                return;
            }
            selectedStation = newStation;
        }
        Misc.run(DqcRadarChooser.this, "stationChanged");
    }

    /**
     * Make the GUI
     *
     * @return The GUI
     */
    protected JComponent doMakeContents() {
        //        getStationMap().setPreferredSize(new Dimension(200, 300));
        getStationMap().setPreferredSize(new Dimension(230, 200));
        //        getStationMap().setPreferredSize(new Dimension(300, 250));
        JComponent buttons    = getDefaultButtons();
        JComponent timesPanel = makeTimesPanel(true, true);
        GuiUtils.tmpInsets = new Insets(0, 3, 0, 3);
        JComponent contents = GuiUtils.doLayout(new Component[] {
                                  getStationMap(),
                                  timesPanel }, 1, new double[] { 3.0, 1.0 },
                                      GuiUtils.WT_Y);
        contents = GuiUtils.inset(contents, 5);
        Misc.run(this, "initializeCollection");
        outerContents =
            GuiUtils.center(GuiUtils.topCenterBottom(getStatusComponent(),
                contents, buttons));
        return outerContents;
    }


    /**
     * Make the collection. If there is an error then blow away the GUI and show a text area showing the error message
     */
    public void initializeCollection() {
        List stations = new ArrayList();
        try {
            StringBuffer errlog = new StringBuffer();
            try {
                collection =
                    DqcRadarDatasetCollection.factory("Radar Collection",
                        collectionUrl, errlog);
            } catch (Exception exc) {
                JTextArea lbl =
                    new JTextArea(
                        "There was an error connecting to the radar collection:\n"
                        + collectionUrl + "\n" + exc + "\n" + errlog);
                outerContents.removeAll();
                outerContents.add(BorderLayout.CENTER, lbl);
                outerContents.layout();
                //                LogUtil.printExceptionNoGui(null,"There was an error connecting to: " + collectionUrl, exc);
                return;
            }
            List dqcStations = collection.getStations();
            for (int i = 0; i < dqcStations.size(); i++) {
                thredds.catalog.query.Station stn =
                    (thredds.catalog.query.Station) dqcStations.get(i);
                thredds.catalog.query.Location loc = stn.getLocation();
                //TODO: need better station
                NamedStationImpl station =
                    new NamedStationImpl(stn.getValue(), stn.getValue(),
                                         loc.getLatitude(),
                                         loc.getLongitude(), 0,
                                         CommonUnit.meter);
                stations.add(station);

            }

            getStationMap().setStations(stations);
        } catch (Exception exc) {
            JTextArea lbl =
                new JTextArea(
                    "There was an error connecting to the radar collection:\n"
                    + collectionUrl + "\n" + exc);
            outerContents.removeAll();
            outerContents.add(BorderLayout.CENTER, lbl);
            outerContents.layout();
            //            LogUtil.printExceptionNoGui(null,"There was an error connecting to: " + collectionUrl, exc);
            //            logException("Reading stations", exc);
        }
    }




    /**
     * Handle when the user has selected a new station
     */
    public void stationChanged() {
        Vector times = new Vector();
        setHaveData(false);
        if (selectedStation != null) {
            Date toDate = new Date(System.currentTimeMillis()
                                   + DateUtil.daysToMillis(1));
            //Go back 10 years (or so)
            Date fromDate = new Date(System.currentTimeMillis()
                                     - DateUtil.daysToMillis(365 * 10));
            try {
                showWaitCursor();
                setAbsoluteTimes(new ArrayList());
                setStatus("Reading times for station: " + selectedStation,
                          "");
                //                LogUtil.message("Reading times for station: "
                //                                + selectedStation);
                List allTimes =
                    collection.getRadarStationTimes(selectedStation.getID(),
                        fromDate, toDate);
                for (int timeIdx = 0; timeIdx < allTimes.size(); timeIdx++) {
                    Object timeObj = allTimes.get(timeIdx);
                    Date   date;
                    if (timeObj instanceof Date) {
                        date = (Date) timeObj;
                    } else {
                        date = DateUnit.getStandardOrISO(timeObj.toString());
                    }
                    times.add(new DateTime(date));
                }
                //                LogUtil.message("");
                showNormalCursor();
            } catch (Exception exc) {
                logException("Getting times for station: " + selectedStation,
                             exc);
                setStatus("", "");
            }
        }
        setAbsoluteTimes(times);
        updateStatus();
    }





    /**
     * Load the data
     */
    public void doLoadInThread() {
        // to the AddeImageDataSource
        Hashtable ht = new Hashtable();

        try {
            DateSelection dateSelection = new DateSelection();
            RadarQuery radarQuery = new RadarQuery(collectionUrl,
                                        selectedStation.getID(),
                                        dateSelection);

            List urls = new ArrayList();

            if (getDoAbsoluteTimes()) {
                List times    = new ArrayList();
                List selected = makeDatedObjects(getSelectedAbsoluteTimes());
                for (int i = 0; i < selected.size(); i++) {
                    DatedThing datedThing = (DatedThing) selected.get(i);
                    Date       date       = datedThing.getDate();
                    times.add(date);
                    URI uri = collection.getRadarDatasetURI(
                                  selectedStation.getID(), date);
                    urls.add(uri.toString());
                }
                if (urls.size() == 0) {
                    LogUtil.userMessage("No times selected");
                    return;
                }
                dateSelection.setTimes(times);
            } else {
                int count = getRelativeTimesList().getSelectedIndex() + 1;
                if (count == 0) {
                    LogUtil.userMessage("No relative times selected");
                    return;
                }
                Date toDate = new Date();
                //Go back 10 years (or so)
                Date fromDate = new Date(System.currentTimeMillis()
                                         - DateUtil.daysToMillis(365 * 10));

                dateSelection.setStartFixedTime(fromDate);
                dateSelection.setEndFixedTime(toDate);
                dateSelection.setCount(count);
            }
            makeDataSource(radarQuery, "FILE.RADAR", ht);
        } catch (Exception exc) {
            logException("Loading radar data", exc);
        }
    }


}
