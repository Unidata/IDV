/*
 * $Id: TrackControl.java,v 1.69 2007/08/21 11:32:08 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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


package ucar.unidata.idv.control.storm;


import ucar.unidata.xml.XmlUtil;
import ucar.unidata.data.gis.KmlUtil;

import org.w3c.dom.*;

import org.apache.poi.hssf.usermodel.*;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.data.storm.*;


import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.idv.control.chart.*;
import ucar.unidata.ui.TreePanel;


import ucar.unidata.util.IOUtil;


import ucar.unidata.ui.drawing.*;
import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import ucar.visad.*;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.display.*;


import ucar.visad.display.*;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.SelectRangeDisplayable;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.StationModelDisplayable;
import ucar.visad.display.TrackDisplayable;



import visad.*;



import visad.georef.EarthLocation;

import visad.georef.EarthLocationLite;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.*;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.beans.*;

import java.io.*;

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormDisplayState {


    /** _more_ */
    private static String ID_OBS_CONE = "id.obs.cone";

    /** _more_ */
    private static String ID_OBS_RINGS = "id.obs.rings";

    /** _more_ */
    private static String ID_FORECAST_CONE = "id.forecast.cone";

    /** _more_ */
    private static String ID_FORECAST_RINGS = "id.forecast.rings";


    /** _more_ */
    private static String ID_FORECAST_COLOR = "id.forecast.color";


    /** _more_ */
    private static String ID_OBS_COLOR = "id.obs.color";


    /** The array of colors we cycle through */
    private static Color[] colors = {
        Color.RED, Color.PINK, Color.MAGENTA, Color.ORANGE, Color.YELLOW,
        Color.GREEN, Color.BLUE, Color.CYAN, Color.GRAY, Color.LIGHT_GRAY
    };

    /** _more_ */
    private static int[] nextColor = { 0 };



    /** _more_ */
    private List<StormTrackChart> charts = new ArrayList<StormTrackChart>();


    /** _more_ */
    private Object MUTEX = new Object();

    /** _more_ */
    private static final Data DUMMY_DATA = new Real(0);



    /** _more_ */
    private CompositeDisplayable holder;

    private boolean isOnlyChild = false;

    /** _more_ */
    private StormInfo stormInfo;


    /** _more_ */
    private WayDisplayState forecastState;

    /** _more_ */
    private boolean haveLoadedForecasts = false;

    /** _more_ */
    private boolean changed = false;


    /** _more_ */
    private boolean active = false;


    /** _more_ */
    private StormTrackCollection trackCollection;


    /** _more_ */
    //    private List<StormTrack> tracks;

    /** _more_ */
    private JTable trackTable;

    /** _more_ */
    private AbstractTableModel trackModel;

    /** _more_ */
    private StormTrackControl stormTrackControl;


    /** _more_ */
    private WayDisplayState obsDisplayState;


    /** time holder */
    private DisplayableData timesHolder = null;

    /** _more_ */
    private JComponent mainContents;

    /** _more_ */
    private JTabbedPane tabbedPane;

    /** _more_ */
    private JComponent originalContents;

    /** _more_ */
    private Hashtable params = new Hashtable();


    /** _more_ */
    private Hashtable<Way, WayDisplayState> wayDisplayStateMap =
        new Hashtable<Way, WayDisplayState>();



    /**
     * _more_
     */
    public StormDisplayState() {}


    /**
     * _more_
     *
     * @param stormInfo _more_
     *
     * @throws Exception _more_
     */
    public StormDisplayState(StormInfo stormInfo) throws Exception {
        this.stormInfo = stormInfo;
        forecastState  = new WayDisplayState(this, new Way("forecaststate"));
        forecastState.getWayState().setVisible(false);
        forecastState.getConeState().setVisible(true);
        forecastState.getTrackState().setVisible(true);
        forecastState.getRingsState().setVisible(true);
    }



    protected void setIsOnlyChild(boolean isOnlyChild) {
        this.isOnlyChild = isOnlyChild;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getContents() {
        if (mainContents == null) {
            mainContents = doMakeContents();
        }
        return mainContents;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected List<WayDisplayState> getWayDisplayStates() {
        return (List<WayDisplayState>) Misc.toList(
            wayDisplayStateMap.elements());
    }

    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected WayDisplayState getWayDisplayState(Way way) {
        WayDisplayState wayState = wayDisplayStateMap.get(way);
        if (wayState == null) {
            wayDisplayStateMap.put(way,
                                   wayState = new WayDisplayState(this, way));
            //        "idv.stormtrackcontrol.way.color"
            if (wayState.getColor() == null) {
                wayState.setColor(getNextColor(nextColor));
            }
        }
        return wayState;
    }



    /**
     * _more_
     */
    protected void reload() {
        if ( !active) {
            return;
        }
        deactivate();
        active = true;
        showStorm();
    }


    /**
     * _more_
     *
     * @return _more_
     */
    protected StormTrackCollection getTrackCollection() {
        return trackCollection;
    }


    /**
     * _more_
     */
    public void deactivate() {
        try {
            for (StormTrackChart stormTrackChart : charts) {
                stormTrackChart.deactivate();
            }
            trackCollection = null;
            active          = false;
            stormTrackControl.removeDisplayable(holder);
            holder = null;
            if (mainContents != null) {
                mainContents.removeAll();
                mainContents.add(BorderLayout.NORTH, originalContents);
                List<WayDisplayState> wayDisplayStates =
                    getWayDisplayStates();
                for (WayDisplayState wayDisplayState : wayDisplayStates) {
                    wayDisplayState.deactivate();
                }
                mainContents.repaint(1);
            }
            stormTrackControl.stormChanged(StormDisplayState.this);

        } catch (Exception exc) {
            stormTrackControl.logException("Deactivating storm", exc);
        }
    }

    /**
     * _more_
     */
    public void loadStorm() {
        if (active) {
            return;
        }
        active = true;
        showStorm();
    }





    /**
     * _more_
     *
     * @return _more_
     */
    private JComponent doMakeContents() {
        JButton loadBtn  = new JButton("Load Tracks:");
        JLabel  topLabel = GuiUtils.cLabel("  " + stormInfo);
        loadBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                loadStorm();
            }
        });

        JComponent top = GuiUtils.hbox(loadBtn, topLabel);
        originalContents = GuiUtils.inset(top, 5);
        JComponent contents = GuiUtils.top(originalContents);
        final int  cnt      = xcnt++;
        contents = new JPanel(new BorderLayout());
        contents.add(BorderLayout.NORTH, originalContents);
        return contents;
    }

    /** _more_ */
    static int xcnt = 0;


    /**
     * _more_
     */
    public void initDone() {
        if (getActive()) {
            showStorm();
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean getForecastVisible() {
        //        return forecastState.getVisible();
        return forecastState.getWayState().getVisible();
    }


    /**
     * _more_
     *
     * @param stormParams _more_
     * @param id _more_
     *
     * @return _more_
     */
    private JComponent makeList(List stormParams, final Object id) {
        if(stormParams==null || stormParams.size()==0) return GuiUtils.filler(2,10);
        final JList list = new JList(new Vector(stormParams));
        list.setVisibleRowCount(4);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                List<StormParam> selected = new ArrayList<StormParam>();
                selected.addAll(Misc.toList(list.getSelectedValues()));
                try {
                    params.put(id, selected);
                    updateDisplays();
                } catch (Exception exc) {
                    stormTrackControl.logException("setting cones", exc);
                }

            }
        });

        List selected = (List) params.get(id);
        if ((selected != null) && (selected.size() > 0)) {
            int[] indices = new int[selected.size()];
            for (int i = 0; i < selected.size(); i++) {
                indices[i] = stormParams.indexOf(selected.get(i));
            }
            list.setSelectedIndices(indices);
        }

        JScrollPane sp = new JScrollPane(list);
        return sp;
    }


    private JComponent makeBox(List stormParams, final Object id) {
        if(stormParams==null || stormParams.size()==0) return GuiUtils.filler(2,10);
        final JComboBox box = new JComboBox(new Vector(stormParams));
        box.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                Object  selected = box.getSelectedItem();
                if(selected == null || selected instanceof String) {
                    params.remove(id);
                } else {
                    params.put(id, selected);
                }
                try {
                    updateDisplays();
                } catch (Exception exc) {
                    stormTrackControl.logException("setting cones", exc);
                }

            }
        });
        return box;
    }


    private List<StormParam> getDistanceParams(List<StormParam> params){
        if ((params == null) || (params.size() == 0)){
            return null;
        }

        List<StormParam>  attrNames = new ArrayList<StormParam>();
        for (StormParam param : params) {
                if (Unit.canConvert(param.getUnit(), CommonUnit.meter)) {

                     attrNames.add(param);
                }

            }
        return attrNames;
    }


    /**
     * _more_
     */
    private void initCenterContents() {

        mainContents.removeAll();
        JButton unloadBtn =
            GuiUtils.makeImageButton("/auxdata/ui/icons/Cut16.gif", this,
                                     "deactivate");
        unloadBtn.setToolTipText("Remove this storm");
        String label =
            "Storm: " + stormInfo.toString() + "   "
            + stormInfo.getStartTime().formattedString("yyyy-MM-dd",
                DateUtil.TIMEZONE_GMT);

        JComponent top =
            GuiUtils.inset(GuiUtils.leftRight(GuiUtils.lLabel(label),
                unloadBtn), new Insets(0, 0, 0, 0));



        List<StormParam> forecastParams     = new ArrayList<StormParam>();
        Hashtable        seenParams = new Hashtable();
        List<StormParam> obsParams     = new ArrayList<StormParam>();
        Hashtable        seenWays   = new Hashtable();
        for (StormTrack track : trackCollection.getTracks()) {
            //            if (seenWays.get(track.getWay()) != null) {
            //                continue;
            //            }
            //            seenWays.put(track.getWay(), track.getWay());
            List<StormParam> trackParams = track.getParams();
            if(track.getWay().isObservation()){
                obsParams.addAll(trackParams);
                continue;
            }
            for (StormParam param : trackParams) {
                if (seenParams.get(param) != null) {
                    continue;
                }
                seenParams.put(param, param);
                forecastParams.add(param);
            }
        }

        List<StormParam> forecastRadiusParams = getDistanceParams(forecastParams);
        List<StormParam> obsRadiusParams = getDistanceParams(obsParams);


        //Sort them by name

        List<Way> ways             = Misc.sort(trackCollection.getWayList());
        boolean   haveDoneForecast = false;
        List<String> colLabels = (List<String>)Misc.newList("","","Track");
        if(forecastRadiusParams!=null ||obsRadiusParams != null ) {
            colLabels.add("Cone");
            colLabels.add("Rings");
        }
        int  numCols    = colLabels.size();


        List paramComps = new ArrayList();
        paramComps.add(new JLabel(""));
        paramComps.add(new JLabel("<html><u><i>Color By</i></u></html>"));
        if(forecastRadiusParams != null || obsRadiusParams != null ) {
            paramComps.add(new JLabel("<html><u><i>Cone</i></u></html>"));
            paramComps.add(new JLabel("<html><u><i>Rings</i></u></html>"));
        }

        List obsColorParams = new ArrayList(obsParams);
        List forecastColorParams = new ArrayList(forecastParams);
        obsColorParams.add(0, "Fixed");
        forecastColorParams.add(0, "Fixed");

        JComponent obsColorByBox      = makeBox(obsColorParams,ID_OBS_COLOR);
        JComponent forecastColorByBox      = makeBox(forecastColorParams,ID_FORECAST_COLOR);


        paramComps.add(
            GuiUtils.top(
                GuiUtils.inset(
                    new JLabel("Observation:"), new Insets(4, 0, 0, 0))));
        paramComps.add(GuiUtils.top(obsColorByBox));
        if(obsRadiusParams != null) {
            paramComps.add(makeList(obsRadiusParams, ID_OBS_CONE));
            paramComps.add(GuiUtils.top(makeBox(obsRadiusParams, ID_OBS_RINGS)));
        } else {
            paramComps.add(GuiUtils.filler());
            paramComps.add(GuiUtils.filler());
        }


        paramComps.add(GuiUtils.top(GuiUtils.inset(new JLabel("Forecasts:"),
                                                   new Insets(4, 0, 0, 0))));
        paramComps.add(GuiUtils.top(forecastColorByBox));

        if(forecastRadiusParams != null) {
            paramComps.add(makeList(forecastRadiusParams, ID_FORECAST_CONE));
            paramComps.add(GuiUtils.top(makeBox(forecastRadiusParams, ID_FORECAST_RINGS)));
        } else {
            paramComps.add(GuiUtils.filler());
            paramComps.add(GuiUtils.filler());
        }


        GuiUtils.tmpInsets = new Insets(4, 2, 0, 2);
        JComponent paramComp = GuiUtils.doLayout(paramComps, 4,
                                   GuiUtils.WT_N, GuiUtils.WT_N);

        List comps = new ArrayList();

        for (Way way : ways) {
            WayDisplayState wds = getWayDisplayState(way);
            if ( !stormTrackControl.okToShowWay(wds.getWay())) {
                continue;
            }
            JComponent labelComp = GuiUtils.hbox(
                                                 wds.getWayState().getCheckBox(this), new JLabel(way.toString()));

            JComponent swatch = GuiUtils.wrap(wds.getColorSwatch());
            if (way.isObservation()) {
                //We put the obs in the front of the list
                int col = 0;
                comps.add(col++, swatch);
                comps.add(col++,labelComp);
                comps.add(col++, wds.getTrackState().getCheckBox(this));
                if(obsRadiusParams != null) {
                    comps.add(col++, wds.getConeState().getCheckBox(this));
                    comps.add(col++, wds.getRingsState().getCheckBox(this));
                }

            } else {
                if ( !haveDoneForecast) {

                    //Put the forecast info here
                    haveDoneForecast = true;
                    for (int colIdx = 0; colIdx < numCols; colIdx++) {
                        comps.add(GuiUtils.filler());
                    }

                    comps.add(GuiUtils.filler());
                    comps.add(
                        GuiUtils.hbox(
                            forecastState.getWayState().getCheckBox(this),
                            GuiUtils.lLabel("Forecasts:")));
                    comps.add(
                        forecastState.getTrackState().getCheckBox(this));
                    if(forecastRadiusParams!=null) {
                        comps.add(
                                       forecastState.getConeState().getCheckBox(this));
                        comps.add(
                                       forecastState.getRingsState().getCheckBox(this));
                    }
                }
                comps.add(swatch);
                comps.add(labelComp);
                comps.add(wds.getTrackState().getCheckBox(this));
                if(forecastRadiusParams !=null) {
                    comps.add(wds.getConeState().getCheckBox(this));
                    comps.add(wds.getRingsState().getCheckBox(this));
                }
            }
        }

        for (int colIdx = 0; colIdx < numCols; colIdx++) {
            String s = colLabels.get(colIdx);
            if (s.length() > 0) {
                comps.add(colIdx,
                               new JLabel("<html><u><i>" + s
                                          + "</i></u></html>"));
            } else {
                comps.add(colIdx, new JLabel(""));
            }
        }


        GuiUtils.tmpInsets = new Insets(2, 2, 0, 2);
        JComponent wayComp = GuiUtils.topLeft(GuiUtils.doLayout(comps,
                                 numCols, GuiUtils.WT_N, GuiUtils.WT_N));
        //Put the list of ways into a scroller if there are lots of them
        if (ways.size() > 10) {
            int width  = 300;
            int height = 400;
            JScrollPane scroller = GuiUtils.makeScrollPane(wayComp, width,
                                       height);
            scroller.setBorder(BorderFactory.createLoweredBevelBorder());
            scroller.setPreferredSize(new Dimension(width, height));
            scroller.setMinimumSize(new Dimension(width, height));
            wayComp = scroller;
        }

        wayComp = GuiUtils.topLeft(GuiUtils.vbox(GuiUtils.left(paramComp),
                GuiUtils.filler(2, 10), GuiUtils.lLabel("Visibility:"),
                GuiUtils.left(wayComp)));

        wayComp    = GuiUtils.inset(wayComp, new Insets(0, 5, 0, 0));
        //        tabbedPane = GuiUtils.getNestedTabbedPane();
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Tracks", wayComp);
        tabbedPane.addTab("Table", getTrackTable());

        if (charts.size() == 0) {
            charts.add(
                new StormTrackChart(
                    this, "Storm Chart", StormTrackChart.MODE_FORECASTTIME));
        }
        for (StormTrackChart stormTrackChart : charts) {
            tabbedPane.addTab(stormTrackChart.getName(),
                              stormTrackChart.getContents());
        }



        JComponent inner = GuiUtils.topCenter(top, tabbedPane);
        inner = GuiUtils.inset(inner, 5);
        mainContents.add(BorderLayout.CENTER, inner);
        mainContents.invalidate();
        mainContents.validate();
        mainContents.repaint();

    }


    /**
     * Class ParamSelector _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class ParamSelector {

        /** _more_ */
        List<StormParam> params;

        /** _more_ */
        JList list;

        /**
         * _more_
         *
         * @param types _more_
         */
        public ParamSelector(List<StormParam> types) {}
    }



    /**
     * _more_
     *
     * @param time _more_
     */
    protected void timeChanged(Real time) {
        for (StormTrackChart stormTrackChart : charts) {
            stormTrackChart.timeChanged(time);
        }
    }





    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected boolean canShowWay(Way way) {
        return getWayDisplayState(way).getWayState().getVisible();
    }

    /**
     * _more_
     *
     * @param stormTrackControl _more_
     */
    protected void setStormTrackControl(StormTrackControl stormTrackControl) {
        this.stormTrackControl = stormTrackControl;
    }

    /**
     * _more_
     */
    protected void showStorm() {
        Misc.run(new Runnable() {
            public void run() {
                DisplayMaster displayMaster =
                    stormTrackControl.getDisplayMaster();
                boolean wasActive = displayMaster.ensureInactive();
                try {
                    synchronized (MUTEX) {
                        showStormInner();
                        stormTrackControl.stormChanged(
                            StormDisplayState.this);
                    }
                } catch (Exception exc) {
                    stormTrackControl.logException("Showing storm", exc);
                } finally {
                    if (wasActive) {
                        try {
                            displayMaster.setActive(true);
                        } catch (Exception exc) {}
                    }
                }

            }
        });
    }


    /**
     * _more_
     *
     * @param displayable _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void addDisplayable(Displayable displayable)
            throws VisADException, RemoteException {
        holder.addDisplayable(displayable);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected StormTrackControl getStormTrackControl() {
        return stormTrackControl;
    }




    /**
     * _more_
     *
     *
     * @throws Exception _more_
     */
    private void showStormInner() throws Exception {

        //Read the tracks if we haven't
        long t1 = System.currentTimeMillis();
        if (trackCollection == null) {
            mainContents.removeAll();
            mainContents.add(
                GuiUtils.top(
                    GuiUtils.inset(new JLabel("Loading Tracks..."), 5)));
            mainContents.invalidate();
            mainContents.validate();
            mainContents.repaint();
            trackCollection =
                stormTrackControl.getStormDataSource().getTrackCollection(
                    stormInfo, stormTrackControl.getOkWays());
            initCenterContents();
            stormTrackControl.addDisplayable(holder =
                new CompositeDisplayable());
            //Add the tracks
            for (StormTrack track : trackCollection.getTracks()) {
                WayDisplayState wayDisplayState =
                    getWayDisplayState(track.getWay());
                wayDisplayState.addTrack(track);
            }
            obsDisplayState = getWayDisplayState(Way.OBSERVATION);
            StormTrack obsTrack = trackCollection.getObsTrack();

            List<DateTime> times = new ArrayList<DateTime>();
            if (obsTrack != null) {
                times = obsTrack.getTrackTimes();
            } else {
                for (StormTrack track : trackCollection.getTracks()) {
                    times.add(track.getStartTime());
                }
            }
            if(times.size()>0) {
                times = (List<DateTime>)Misc.sort(Misc.makeUnique(times));
                timesHolder = new LineDrawing("track_time"
                        + stormInfo.getStormId());
                timesHolder.setManipulable(false);
                timesHolder.setVisible(false);
                Set timeSet = ucar.visad.Util.makeTimeSet(times);
                //                System.err.println("time set:" + timeSet);
                timesHolder.setData(timeSet);
                holder.addDisplayable(timesHolder);
            }
        }

        updateDisplays();
        updateCharts();



        long t2 = System.currentTimeMillis();
        System.err.println("time:" + (t2 - t1));
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    protected List<StormParam> getParams(Object id) {
        List<StormParam> l = (List<StormParam>) params.get(id);
        if (l == null) {
            l = new ArrayList<StormParam>();
            params.put(id, l);
        }
        return l;
    }


    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected List<StormParam> getConeParams(WayDisplayState way) {
        if (way.getWay().isObservation()) {
            return getParams(ID_OBS_CONE);
        }
        return getParams(ID_FORECAST_CONE);
    }


    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected StormParam getRingsParam(WayDisplayState way) {
        if (way.getWay().isObservation()) {
            return (StormParam)params.get(ID_OBS_RINGS);
        }
        return (StormParam)params.get(ID_FORECAST_RINGS);
    }



    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected StormParam getColorParam(WayDisplayState way) {
        if (way.getWay().isObservation()) {
            return (StormParam)params.get(ID_OBS_COLOR);
        }
        return (StormParam)params.get(ID_FORECAST_COLOR);
    }






    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void updateCharts() throws Exception {
        for (StormTrackChart stormTrackChart : charts) {
            stormTrackChart.updateChart();
        }
    }



    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void updateDisplays() throws Exception {
        DisplayMaster displayMaster =
            stormTrackControl.getDisplayMaster();
        boolean wasActive = displayMaster.ensureInactive();
        try {
            List<WayDisplayState> wayDisplayStates = getWayDisplayStates();
            for (WayDisplayState wds : wayDisplayStates) {
                if ( !stormTrackControl.okToShowWay(wds.getWay())) {
                    continue;
                }
                wds.updateDisplay();
            }
        } finally {
            if (wasActive) {
                try {
                    displayMaster.setActive(true);
                } catch (Exception exc) {}
            }
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    protected StationModel getObservationStationModel() {
        StationModel model       = new StationModel("TrackLocation");
        ShapeSymbol  shapeSymbol = new ShapeSymbol(0, 0);
        shapeSymbol.setShape(ucar.visad.ShapeUtility.HURRICANE);
        shapeSymbol.setScale(2.0f);
        shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
        shapeSymbol.setRectPoint(Glyph.PT_MM);
        shapeSymbol.setForeground(null);
        model.addSymbol(shapeSymbol);
        return model;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private StationModel getForecastStationModel() {
        /*
          StationModelDisplayable dots  = new StationModelDisplayable("dots");
          wayDisplayState.addDisplayable(dots);
          StationModel model = new StationModel("TrackLocation");
          TextSymbol textSymbol = new TextSymbol("label","the label");
          textSymbol.setScale(1.5f);
          textSymbol.setRectPoint(Glyph.PT_UL);
          textSymbol.bounds = new java.awt.Rectangle(10,0,21,15);
          model.addSymbol(textSymbol);

          ShapeSymbol shapeSymbol = new ShapeSymbol(0, 0);
          shapeSymbol.setScale(0.5f);
          shapeSymbol.setShape(ucar.visad.ShapeUtility.CIRCLE);
          shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
          shapeSymbol.setRectPoint(Glyph.PT_MM);
          model.addSymbol(shapeSymbol);
          forecastHolder.addDisplayable(dots);
          dots.setScale(1.0f);
          dots.setStationModel(model);
          dots.setStationData(PointObFactory.makeTimeSequenceOfPointObs( wayDisplayState.getPointObs(),
          -1,-1));

        */
        return null;
    }

    //        ucar.visad.Util.makeTimeField(List<Data> ranges, List times)

    /**
         Animation animation = stormTrackControl.getAnimation();
         if (animation == null) {
            return;
        }
        List<StormTrack> visibleTracks = new ArrayList<StormTrack>();
        Real currentAnimationTime = animation.getAniValue();
       if (currentAnimationTime == null || currentAnimationTime.isMissing()) {
            return;
        }
       Iterate way display states
         boolean             visible = false;
         if(wds.shouldShowTrack() && wds.hasTrackDisplay()) {
         FieldImpl field = (FieldImplt)wds.getTrackDisplay().getData()
         if(field==null) continue;
         Set timeSet = GridUtil.getTimeSet();
         if(timeSet == null) continue;
         if (timeSet.getLength() == 1) {
            visible = true;
        } else {
            //Else work the visad magic
            float timeValueFloat = (float) currentAnimationTime.getValue(
                                       timeSet.getSetUnits()[0]);
            //            System.err.println("multiple times:" + timeValueFloat);
            float[][] value = {
                { timeValueFloat }
            };
            int[]     index = timeSet.valueToIndex(value);
            //            System.err.println("index:" + index[0]);
            visible = (index[0] >= 0);
        }
        if(visible) {
        //Find the closest track in wds in time
             visibleTracks.add(..);
        }
        }


        Now search in space
     **/







    /**
     * _more_
     *
     * @param stormTrackChart _more_
     */
    protected void removeChart(StormTrackChart stormTrackChart) {
        charts.remove(stormTrackChart);
        tabbedPane.remove(stormTrackChart.getContents());
    }


    /**
     * _more_
     */
    public void addForecastTimeChart() {
        addForecastChart(StormTrackChart.MODE_FORECASTTIME);
    }

    /**
     * _more_
     */
    public void addForecastHourChart() {
        addForecastChart(StormTrackChart.MODE_FORECASTHOUR);
    }

    /**
     * _more_
     *
     * @param mode _more_
     */
    public void addForecastChart(int mode) {
        String chartName = GuiUtils.getInput("Please enter a chart name",
                                             "Chart Name: ", "Storm Chart");
        if (chartName == null) {
            return;
        }
        StormTrackChart stormTrackChart = new StormTrackChart(this,
                                              chartName, mode);
        charts.add(stormTrackChart);
        tabbedPane.addTab(stormTrackChart.getName(),
                          stormTrackChart.getContents());
        stormTrackChart.updateChart();

    }


    /**
     * _more_
     *
     * @return _more_
     */
    private JComponent getTrackTable() {
        TreePanel tableTreePanel = new TreePanel(true, 150);
        int       width          = 400;
        int       height         = 400;
        for (StormTrack track : trackCollection.getTracks()) {
            StormTrackTableModel tableModel = new StormTrackTableModel(this,
                                                  track);
            JTable trackTable = new JTable(tableModel);
            JScrollPane scroller = GuiUtils.makeScrollPane(trackTable, width,
                                       height);
            scroller.setBorder(BorderFactory.createLoweredBevelBorder());
            JComponent contents = scroller;
            if ( !track.getWay().isObservation()) {
                contents = GuiUtils.topCenter(
                    GuiUtils.left(
                        GuiUtils.inset(
                            new JLabel(track.getStartTime().toString()),
                            5)), contents);
            }
            tableTreePanel.addComponent(contents, track.getWay().toString(),
                                        track.getStartTime().toString(),
                                        null);
        }


        return tableTreePanel;
    }



    /**
     * _more_
     */
    public void writeToXls() {
        try {
            JCheckBox obsCbx = new JCheckBox("Observation",
                                               true);
            JCheckBox forecastCbx = new JCheckBox("Forecast", true);
            JCheckBox mostRecentCbx =
                new JCheckBox("Most Recent Forecasts", false);
            JComponent accessory =
                GuiUtils.top(GuiUtils.vbox(obsCbx,
                                           forecastCbx,
                                           mostRecentCbx));

            String filename = FileManager.getWriteFile(
                                  Misc.newList(FileManager.FILTER_XLS),
                                  FileManager.SUFFIX_XLS, accessory);
            if (filename == null) {
                return;
            }

            List<Way>            waysToUse = new ArrayList<Way>();
            Hashtable<Way, List> trackMap  = new Hashtable<Way, List>();
            for (StormTrack track : trackCollection.getTracks()) {
                List tracks = trackMap.get(track.getWay());
                if (tracks == null) {
                    tracks = new ArrayList();
                    trackMap.put(track.getWay(), tracks);
                    waysToUse.add(track.getWay());
                }
                tracks.add(track);
            }


            Hashtable    sheetNames = new Hashtable();
            HSSFWorkbook wb         = new HSSFWorkbook();
            StormTrack   obsTrack   = trackCollection.getObsTrack();
            //Write the obs track first
            if ((obsTrack != null) && obsCbx.isSelected()) {
                write(wb, obsTrack, sheetNames);
            }
            if (forecastCbx.isSelected()) {
                waysToUse = Misc.sort(waysToUse);
                for (Way way : waysToUse) {
                    if (way.isObservation()) {
                        continue;
                    }
                    List<StormTrack> tracks =
                        (List<StormTrack>) Misc.sort(trackMap.get(way));
                    if (mostRecentCbx.isSelected()) {
                        write(wb, tracks.get(tracks.size() - 1), sheetNames);
                    } else {
                        for (StormTrack track : tracks) {
                            write(wb, track, sheetNames);
                        }
                    }
                }
            }
            FileOutputStream fileOut = new FileOutputStream(filename);
            wb.write(fileOut);
            fileOut.close();
        } catch (Exception exc) {
            stormTrackControl.logException("Writing spreadsheet", exc);
        }
    }

    /**
     * _more_
     *
     * @param wb _more_
     * @param track _more_
     * @param sheetNames _more_
     */
    protected void write(HSSFWorkbook wb, StormTrack track,
                         Hashtable sheetNames) {
        int cnt = 0;
        String dateString =
            track.getStartTime().formattedString("yyyy-MM-dd hhmm",
                DateUtil.TIMEZONE_GMT);
        String sheetName = track.getWay() + " - " + dateString;
        if (sheetName.length() > 30) {
            sheetName = sheetName.substring(0, 29);
        }
        //The sheet name length is limited
        while (sheetNames.get(sheetName) != null) {
            sheetName = (cnt++) + " " + sheetName;
            if (sheetName.length() > 30) {
                sheetName = sheetName.substring(0, 29);
            }
        }
        sheetNames.put(sheetName, sheetName);
        HSSFSheet        sheet  = wb.createSheet(sheetName);

        int              rowCnt = 0;
        List<StormParam> params = track.getParams();
        HSSFCell         cell;
        HSSFRow          row;


        for (StormTrackPoint stp : track.getTrackPoints()) {
            if (rowCnt == 0) {
                row = sheet.createRow((short) rowCnt++);
                row.createCell((short) 0).setCellValue("Time");
                row.createCell((short) 1).setCellValue("Latitude");
                row.createCell((short) 2).setCellValue("Longitude");
                for (int colIdx = 0; colIdx < params.size(); colIdx++) {
                    row.createCell((short) (colIdx + 3)).setCellValue(
                        params.get(colIdx).toString());
                }
            }
            row = sheet.createRow((short) rowCnt++);
            row.createCell((short) 0).setCellValue(
                stp.getTrackPointTime().toString());
            row.createCell((short) 1).setCellValue(
                stp.getTrackPointLocation().getLatitude().getValue());
            row.createCell((short) 2).setCellValue(
                stp.getTrackPointLocation().getLongitude().getValue());
            for (int colIdx = 0; colIdx < params.size(); colIdx++) {
                Real r = stp.getAttribute(params.get(colIdx));
                cell = row.createCell((short) (colIdx + 3));
                cell.setCellValue(r.getValue());
            }
        }
    }



    /**
     * _more_
     */
    public void writeToGE() {
            JCheckBox obsCbx = new JCheckBox("Observation",
                                               true);
            JCheckBox forecastCbx = new JCheckBox("Forecast", true);
            JCheckBox mostRecentCbx =
                new JCheckBox("Most Recent Forecasts", false);
            JComponent accessory =
                GuiUtils.top(GuiUtils.vbox(obsCbx,
                                           forecastCbx,
                                           mostRecentCbx));

            String filename = FileManager.getWriteFile(
                                  Misc.newList(FileManager.FILTER_KML),
                                  FileManager.SUFFIX_KML, accessory);
            if (filename == null) {
                return;
            }

            writeToGE(filename, obsCbx.isSelected(),
                      forecastCbx.isSelected(),
                      mostRecentCbx.isSelected());
        }


    public void writeToGE(String filename, boolean doObs, boolean doForecast, boolean mostRecent) {
        try {
            List<Way>            waysToUse = new ArrayList<Way>();
            Hashtable<Way, List> trackMap  = new Hashtable<Way, List>();
            for (StormTrack track : trackCollection.getTracks()) {
                List tracks = trackMap.get(track.getWay());
                if (tracks == null) {
                    tracks = new ArrayList();
                    trackMap.put(track.getWay(), tracks);
                    waysToUse.add(track.getWay());
                }
                tracks.add(track);
            }

            Element kmlNode = KmlUtil.kml("");
            
            Element topFolder = KmlUtil.folder(kmlNode, "Storm: " + stormInfo.toString() + "   "
                                       + stormInfo.getStartTime().formattedString("yyyy-MM-dd",
                                                                                  DateUtil.TIMEZONE_GMT));
            StormTrack   obsTrack   = trackCollection.getObsTrack();
            //Write the obs track first
            if ((obsTrack != null) && doObs) {
                writeToGE(topFolder, obsTrack);
            }
            if (doForecast) {
                waysToUse = Misc.sort(waysToUse);
                for (Way way : waysToUse) {
                    if (way.isObservation()) {
                        continue;
                    }
                    Element wayNode = KmlUtil.folder(topFolder, stormTrackControl.getWayName() +": " + way);
                    List<StormTrack> tracks =
                        (List<StormTrack>) Misc.sort(trackMap.get(way));
                    if (mostRecent) {
                        writeToGE(wayNode,tracks.get(tracks.size() - 1));
                    } else {
                        for (StormTrack track : tracks) {
                            writeToGE(wayNode, track);
                        }
                    }
                }
            }
            FileOutputStream fileOut = new FileOutputStream(filename);
            IOUtil.writeBytes(new File(filename), XmlUtil.toString(kmlNode).getBytes());
        } catch (Exception exc) {
            stormTrackControl.logException("Writing KML", exc);
        }
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param track _more_

     */
    protected void writeToGE(Element parent, StormTrack track) {
        Element placemark = KmlUtil.placemark(parent,"Track",""+track.getStartTime());
        int cnt = 0;
        String dateString =
            track.getStartTime().formattedString("yyyy-MM-dd hhmm",
                DateUtil.TIMEZONE_GMT);
        String sheetName = track.getWay() + " - " + dateString;
        int              rowCnt = 0;
        List<StormParam> params = track.getParams();
        StringBuffer sb = new StringBuffer();
        for (StormTrackPoint stp : track.getTrackPoints()) {
            EarthLocation el = stp.getTrackPointLocation();
            sb.append(el.getLongitude().getValue());            sb.append(",");
            sb.append(el.getLatitude().getValue());
            sb.append(",");
            sb.append(el.getAltitude().getValue());
            sb.append("\n");
        }
        Element linestring = KmlUtil.linestring(placemark, false, false, sb.toString());
        //        KmlUtil.timestamp(linestring, track.getStartTime());
        KmlUtil.timestamp(placemark, track.getStartTime());
    }





    /**
     *  Set the StormInfo property.
     *
     *  @param value The new value for StormInfo
     */
    public void setStormInfo(StormInfo value) {
        stormInfo = value;
    }

    /**
     *  Get the StormInfo property.
     *
     *  @return The StormInfo
     */
    public StormInfo getStormInfo() {
        return stormInfo;
    }


    /**
     *  Set the Changed property.
     *
     *  @param value The new value for Changed
     */
    public void setChanged(boolean value) {
        changed = value;
    }

    /**
     *  Get the Changed property.
     *
     *  @return The Changed
     */
    public boolean getChanged() {
        return changed;
    }

    /**
     * Set the Active property.
     *
     * @param value The new value for Active
     */
    public void setActive(boolean value) {
        active = value;
    }

    /**
     * Get the Active property.
     *
     * @return The Active
     */
    public boolean getActive() {
        return active;
    }


    /**
     *  Set the WayDisplayStateMap property.
     *
     *  @param value The new value for WayDisplayStateMap
     */
    public void setWayDisplayStateMap(Hashtable<Way, WayDisplayState> value) {
        wayDisplayStateMap = value;
    }

    /**
     *  Get the WayDisplayStateMap property.
     *
     *  @return The WayDisplayStateMap
     */
    public Hashtable<Way, WayDisplayState> getWayDisplayStateMap() {
        return wayDisplayStateMap;
    }


    /**
     *  Set the ForecastState property.
     *
     *  @param value The new value for ForecastState
     */
    public void setForecastState(WayDisplayState value) {
        forecastState = value;
    }

    /**
     *  Get the ForecastState property.
     *
     *  @return The ForecastState
     */
    public WayDisplayState getForecastState() {
        return forecastState;
    }




    /**
     * Cycle through the color list.
     *
     *
     * @param nextColor _more_
     * @return The next color in the list
     */
    private static Color getNextColor(int[] nextColor) {
        if (nextColor[0] >= colors.length) {
            nextColor[0] = 0;
        }
        return colors[nextColor[0]++];
    }

    /**
     * Set the Charts property.
     *
     * @param value The new value for Charts
     */
    public void setCharts(List<StormTrackChart> value) {
        charts = value;
    }

    /**
     * Get the Charts property.
     *
     * @return The Charts
     */
    public List<StormTrackChart> getCharts() {
        return charts;
    }



    /**
     * Set the Params property.
     *
     * @param value The new value for Params
     */
    public void setParams(Hashtable value) {
        params = value;
    }

    /**
     * Get the Params property.
     *
     * @return The Params
     */
    public Hashtable getParams() {
        return params;
    }







}

