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

package ucar.unidata.idv.chooser.adde;


import edu.wisc.ssec.mcidas.McIDASUtil;
import edu.wisc.ssec.mcidas.adde.AddePointDataReader;
import edu.wisc.ssec.mcidas.adde.AddeURLException;

import org.w3c.dom.Element;

import ucar.unidata.data.AddeUtil;
import ucar.unidata.data.profiler.AddeProfilerDataSource;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.*;


import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.metdata.NamedStationImpl;
import ucar.unidata.metdata.NamedStationTable;

import ucar.unidata.metdata.Station;



import ucar.unidata.ui.ChooserList;
import ucar.unidata.ui.ChooserPanel;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.station.StationLocationMap;
import ucar.unidata.view.station.StationLocationRenderer;

import ucar.unidata.xml.XmlResourceCollection;

import ucar.visad.UtcDate;

import visad.DateTime;

import visad.VisADException;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.*;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;


/**
 * Selection widget for specifing data sources of
 * NOAA National Profiler Network data.
 * For selecting Profiler data source; user selects ADDE server,
 * profiler station(s),
 * and choice of data interval such as hourly or 6 minute.
 *
 * Metadata about the station (lat, lon. elevation)
 * and about the request is made available by "get" methods.
 *
 * @author Unidata IDV Development Team
 * @version $Revision: 1.6 $
 */
public class AddeProfilerDataChooser extends AddeChooser {


    /** group */
    private static final String GROUP = "RTPTSRC";

    /** 6 minute profiler data identifier */
    private static final String PROFILER_6MIN =
        AddeProfilerDataSource.PROFILER_6MIN;

    /** 12 minute profiler data identifier */
    private static final String PROFILER_12MIN =
        AddeProfilerDataSource.PROFILER_12MIN;

    /** 30 minute profiler data identifier */
    private static final String PROFILER_30MIN =
        AddeProfilerDataSource.PROFILER_30MIN;

    /** 1 hour profiler data identifier */
    private static final String PROFILER_1HR =
        AddeProfilerDataSource.PROFILER_1HR;


    /** UI for selecting data interval */
    private JComboBox dataIntervalBox;


    /** collection of station tables */
    private XmlResourceCollection stationResources;


    /**
     * ctor
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeProfilerDataChooser(IdvChooserManager mgr, Element root) {
        super(mgr, root);
        initProfiler();
    }





    /**
     * init
     */
    private void initProfiler() {
        TwoFacedObject[] intervals = { new TwoFacedObject("Hourly",
                                         PROFILER_1HR),
                                       new TwoFacedObject("30 minute",
                                           PROFILER_30MIN),
                                       new TwoFacedObject("12 minute",
                                           PROFILER_12MIN),
                                       new TwoFacedObject("6 minute",
                                           PROFILER_6MIN) };
        // make selector box for what time interval the user wants to disply
        // (not the Profiler time interval which is only 1 hr or 6 min) nnn
        dataIntervalBox = new JComboBox();
        GuiUtils.setListData(dataIntervalBox, intervals);
        dataIntervalBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                setState(STATE_UNCONNECTED);
            }
        });


        List allComps = new ArrayList();
        getStationMap().setPreferredSize(new Dimension(200, 200));
        clearOnChange(dataIntervalBox);
        addTopComponents(allComps, LABEL_DATAINTERVAL, dataIntervalBox);
        JComponent timesPanel = makeTimesPanel();


        JComponent stationMap = getStationMap();
        stationMap.setPreferredSize(new Dimension(230, 180));
        allComps.add(GuiUtils.valignLabel("Stations:"));
        allComps.add(stationMap);
        allComps.add(GuiUtils.valignLabel("Times:"));
        allComps.add(addServerComp(timesPanel));

        GuiUtils.tmpInsets = GRID_INSETS;
        JComponent top = GuiUtils.doLayout(allComps, 2, GuiUtils.WT_NY,
                                           GuiUtils.WT_NYN);

        contents = GuiUtils.centerBottom(top, getDefaultButtons());
        updateStatus();
    }



    /**
     * Tell the AddeChooser our name
     *
     * @return  The name
     */
    public String getDataName() {
        return "Profiler Data";
    }


    /**
     * Set the list of available times;
     * use adde request to a station to find times of data available now.
     */
    public void readTimes() {
        setState(STATE_CONNECTING);
        StringBuffer buf = getGroupUrl(REQ_POINTDATA, GROUP);

        appendKeyValue(buf, PROP_DESCR, getDataSourceInterval());

        // check times from only one station, whose 4 char id is given here
        // TODO - find times from other station(s) if this is down.
        appendKeyValue(buf, PROP_SELECT, "'IDA BLMM'");
        // appendKeyValue(buf, PROP_POS, "ALL");
        appendKeyValue(buf, PROP_NUM, "ALL");
        appendKeyValue(buf, PROP_PARAM, "DAY TIME");

        SortedSet uniqueTimes =
            Collections.synchronizedSortedSet(new TreeSet());
        try {
            AddePointDataReader apr = new AddePointDataReader(buf.toString());
            int[][]             data  = apr.getData();

            String[]            units = apr.getUnits();


            if ( !units[0].equals("CYD") || !units[1].equals("HMS")) {
                throw new Exception("can't handle date/time units");
            }
            int numObs = data[0].length;
            for (int i = 0; i < numObs; i++) {
                try {
                    DateTime dt =
                        new DateTime(McIDASUtil.mcDayTimeToSecs(data[0][i],
                            data[1][i]));
                    uniqueTimes.add(dt);
                } catch (Exception e) {}
            }
            setState(STATE_CONNECTED);
        } catch (Exception excp) {
            handleConnectionError(excp);
            return;
        }

        if (getDoAbsoluteTimes()) {
            if ( !uniqueTimes.isEmpty()) {
                setAbsoluteTimes(new ArrayList(uniqueTimes));
                getTimesList().setSelectionMode(
                    ListSelectionModel.SINGLE_INTERVAL_SELECTION);
            }

            //   Select the last n hours 
            int selectedIndex = getAbsoluteTimes().size() - 1;
            int firstIndex = Math.max(0, selectedIndex
                                      - getDefaultRelativeTimeIndex());
            setSelectedAbsoluteTime(selectedIndex, firstIndex);
            /*
            int[] indices       = new int[selectedIndex - firstIndex + 1];
            for (int i = 0; i < indices.length; i++) {
                indices[i] = i + firstIndex;
            }
            setSelectedAbsoluteTimes(indices);
            */

        }
    }

    /**
     * Create the date time selection string for the "select" clause
     * of the ADDE server request URL.
     * <p>
     *
     * this version does not include and "day" value, hence by
     * defaults all times refer to time this day in the UTC time zone.
     * <p>
     *
     * To choose a day, use string like day 2003155, julian day number,
     * and several other formats possible. perhaps 2002/06/05.
     * To choose an hour and minute use 11:45; hour alone is just 11
     *
     * @return  the day and time selection strings.
     */
    private String getDayTimeSelectString() {

        StringBuffer buf = new StringBuffer();
        if (getDoAbsoluteTimes()) {
            List times = getSelectedAbsoluteTimes();

            // no time selection is permitted as a valid choice -
            // will then use all times today by default.
            if (times.size() == 0) {
                return "";
            }

            //check for the "no times available" message
            if (times.get(0) instanceof String) {
                return "";
            }

            //for (int kk=0; kk<times.size(); kk++) {
            //    DateTime dt = (DateTime) times.get(kk);
            //    System.out.println("  sel time "+dt.timeString() );
            //}

            // make the String used in the ADDE request:

            buf.append("time ");

            // if hourly, make list of comma separated hour numbers,
            // like 11,12,15,20,22 
            // need not have all values in range (free of gaps) 

            String dataInterval = getDataSourceInterval();

            if (dataInterval.equals(
                    AddeProfilerDataSource.PROFILER_SERVER_INT_HR)) {
                for (int i = 0; i < times.size(); i++) {
                    buf.append(UtcDate.getHH((DateTime) times.get(i)));
                    if (i != times.size() - 1) {
                        buf.append(",");
                    }
                }
            } else {
                // if prof6min, make string like "time 11 22"; space 
                // separation denotes an hour range; all times between needed;
                // OR
                // if you want to specify random time each one must be
                // in a comma separated list with hr:min groups like
                // 11:00,11:06,11;12, ...   21:54,22:00
                /*
                  hours-only limits:
                DateTime dt = (DateTime) times[0];
                buf.append(UtcDate.getHH((DateTime)dt));
                buf.append(" ");
                dt = (DateTime) times[times.size() - 1];
                buf.append(UtcDate.getHH(dt));
                */
                // get and show every time selected:
                for (int kk = 0; kk < times.size(); kk++) {
                    buf.append(UtcDate.getHMS((DateTime) times.get(kk)));
                    if (kk < times.size() - 1) {
                        buf.append(",");
                    }
                }
            }
        } else {
            buf.append(AddeUtil.RELATIVE_TIME);
        }

        //System.out.println("APDC.getDayTimeSelectString=" + buf);
        return buf.toString();
    }


    /**
     * Set this list of times in the time widget.
     */
    private void doSetTimes() {}



    /**
     * Get the number of times to select
     *
     * @return 5
     */
    protected int getNumTimesToSelect() {
        return 5;
    }


    /**
     * Refresh this chooser.
     * Called when user clicks on "Connect" button.
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {
        readTimes();
        updateStatus();
        showNormalCursor();
        doSetTimes();
        saveServerState();
    }



    /**
     * Overwrite base class method to create the station map
     * with the appropriate properties.
     *
     * @return The new station map
     */
    protected StationLocationMap createStationMap() {
        return new StationLocationMap(true, (String) null,
                                      StationLocationMap.TEMPLATE_NAME) {
            public void setDeclutter(boolean declutter) {
                super.setDeclutter(declutter);
                updateStatus();
            }
        };
    }



    /**
     * Overwrite base class method to return the default rectangle for
     * the station map
     *
     * @return Default proj rectangle
     */
    protected ProjectionRect getDefaultProjectionRect() {
        return new ProjectionRect(-2000, -1800, 2500, 1800);
    }



    /**
     * Initialize  the  station map
     *
     * @param stationMap The station map to initialize
     */
    protected void initStationMap(StationLocationMap stationMap) {
        super.initStationMap(stationMap);

        // get station information from the xml file listed
        if (stationResources == null) {
            List resources =
                Misc.newList(
                    "/ucar/unidata/idv/resources/stations/profilerstns.xml");
            stationResources = new XmlResourceCollection("", resources);
        }

        // create an object to hold the station info
        NamedStationTable stationTable =
            NamedStationTable.createStationTable(stationResources.getRoot(0));


        List listOfTables =
            NamedStationTable.createStationTables(stationResources);
        if (listOfTables.size() > 0) {
            NamedStationTable profStations =
                (NamedStationTable) listOfTables.get(0);
            // Take this out if we only want to init stations 
            // when we connect to the server. 
            //   each "value" is a full Station object, not the name string
            stationMap.setStations(new ArrayList(profStations.values()));
        } else {
            //What to do if there are no stations
        }
    }







    /**
     * Get String for time "select" item in ADDE data request.
     *
     * @return String for time "select" item in adde request.
     */
    public String getSelectedTimes() {
        return getDayTimeSelectString();
    }

    /**
     * Get data interval name as "30 minute" by user from this chooser gui.
     *
     * @return String data interval name as "30 minute"
     */
    public String getSelectedInterval() {
        return TwoFacedObject.getIdString(dataIntervalBox.getSelectedItem());
    }

    /**
     * Get the default selected index for the relative times list.
     *
     * @return default index
     */
    protected int getDefaultRelativeTimeIndex() {
        return 11;
    }

    /**
     * Get the increment between times for relative time requests
     *
     * @return time increment in hours
     */
    public float getRelativeTimeIncrement() {
        String currentType = getSelectedInterval();
        if (currentType.equals(PROFILER_30MIN)) {
            return .5f;
        } else if (currentType.equals(PROFILER_12MIN)) {
            return .2f;
        } else if (currentType.equals(PROFILER_6MIN)) {
            return .1f;
        } else {
            return 1;
        }
    }

    /**
     * Get data source interval (1 hour or 6 min are only possibilities)
     * of actual data available.
     *
     * @return String data interval name as "prof6min"
     */
    public String getDataSourceInterval() {
        String interval = getSelectedInterval();
        if (interval.equals(PROFILER_30MIN)
                || interval.equals(PROFILER_12MIN)
                || interval.equals(PROFILER_6MIN)) {
            return AddeProfilerDataSource.PROFILER_SERVER_INT_6MIN;
        }
        return AddeProfilerDataSource.PROFILER_SERVER_INT_HR;
    }

    /**
     * Get any extra key=value pairs that are appended to all requests.
     *
     * @param buff The buffer to append onto
     */
    protected void appendMiscKeyValues(StringBuffer buff) {
        appendKeyValue(buff, PROP_POS, getDoRelativeTimes()
                                       ? "ALL"
                                       : "0");
        super.appendMiscKeyValues(buff);
    }



    /**
     * Get the selection event from the profiler data chooser
     * and process it, creating a ADDE.PROFILER data source
     *
     */
    public void doLoadInThread() {
        showWaitCursor();
        try {
            List selectedStations = getSelectedStations();

            // make properties Hashtable to hand some 
            //  data selection metadata
            // to the AddeProfilerDataSource where it helps process 
            // the data from the server into data format this IDV
            // needs for this request.

            Hashtable profilersourceHT = new Hashtable();

            profilersourceHT.put(AddeProfilerDataSource.PROFILER_INT,
                                 getSelectedInterval());
            profilersourceHT.put(AddeProfilerDataSource.PROFILER_DATAINT,
                                 getDataSourceInterval());
            profilersourceHT.put(AddeProfilerDataSource.PROFILER_SERVER,
                                 getServer());
            profilersourceHT.put(AddeProfilerDataSource.PROFILER_TIMES,
                                 getSelectedTimes());
            profilersourceHT.put(AddeUtil.NUM_RELATIVE_TIMES,
                                 getRelativeTimeIndices());
            profilersourceHT.put(AddeUtil.RELATIVE_TIME_INCREMENT,
                                 new Float(getRelativeTimeIncrement()));
            profilersourceHT.put(AddeUtil.MISC_KEYWORDS, getMiscKeywords());


            //System.out.println("   pc time list "+getSelectedTimes());
            //System.out.println
            // ("   pc data display interval "+getSelectedDataInterval());
            //System.out.println
            // ("  pc data source interval  "+getDataSourceInterval());

            // hard-coded "ADDE.PROFILER" is in idv/resources/datasource.xml,
            // which tells IDV to use code 
            // ucar.unidata.data.profiler.AddeProfilerDataSource
            makeDataSource(selectedStations, "ADDE.PROFILER",
                           profilersourceHT);
            saveServerState();
        } catch (Exception excp) {
            logException("Unable to open Profiler dataset", excp);
        }
        showNormalCursor();
    }




}
