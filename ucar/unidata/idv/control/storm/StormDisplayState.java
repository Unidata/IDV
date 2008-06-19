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


import org.apache.poi.hssf.usermodel.*;

import org.w3c.dom.*;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.gis.KmlUtil;
import ucar.unidata.data.point.PointOb;
import ucar.unidata.data.point.PointObFactory;
import ucar.unidata.data.storm.*;
import ucar.unidata.geoloc.LatLonPointImpl;



import ucar.unidata.geoloc.LatLonRect;


import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.idv.control.LayoutModelWidget;
import ucar.unidata.idv.control.chart.*;
import ucar.unidata.ui.TableSorter;
import ucar.unidata.ui.TreePanel;
import ucar.unidata.ui.colortable.ColorTableCanvas;


import ucar.unidata.ui.drawing.*;
import ucar.unidata.ui.symbol.*;


import ucar.unidata.util.ColorTable;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;


import ucar.unidata.xml.XmlUtil;

import ucar.visad.*;

import ucar.visad.Util;
import ucar.visad.display.*;
import ucar.visad.display.*;


import ucar.visad.display.*;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationLite;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.util.ColorPreview;

import java.awt.*;
import java.awt.event.*;

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
    private static String ID_OBS_LAYOUTMODEL = "id.obs.layoutmodel";


    /** _more_ */
    private static String ID_FORECAST_CONE = "id.forecast.cone";

    /** _more_ */
    private static String ID_FORECAST_RINGS = "id.forecast.rings";


    /** _more_ */
    private static String ID_FORECAST_COLOR = "id.forecast.color";

    /** _more_ */
    private static String ID_FORECAST_LAYOUTMODEL = "id.forecast.layoutmodel";


    /** _more_ */
    private static String ID_OBS_COLOR = "id.obs.color";


    /** The array of colors we cycle through */
    private static Color[] colors = {
        Color.RED, Color.PINK, Color.MAGENTA, Color.ORANGE, Color.YELLOW,
        Color.GREEN, Color.BLUE, Color.CYAN, Color.GRAY, Color.LIGHT_GRAY
    };

    /** _more_ */
    private static int[] nextColor = { 0 };


    /** _more_          */
    private JLabel obsColorTableLabel;

    /** _more_          */
    private JLabel forecastColorTableLabel;

    /** _more_ */
    private List<StormTrackChart> charts = new ArrayList<StormTrackChart>();


    /** _more_ */
    private Object MUTEX = new Object();

    /** _more_ */
    private static final Data DUMMY_DATA = new Real(0);



    /** _more_ */
    private CompositeDisplayable holder;

    /** _more_ */
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
    private StormTrackControl stormTrackControl;


    /** _more_ */
    private WayDisplayState obsDisplayState;

    /** _more_ */
    private String obsLayoutModelName = "Storm>Hurricane";

    /** _more_          */
    private String obsPointLayoutModelName = "Storm>Forecast Hour";

    /** _more_ */
    private String forecastLayoutModelName = "Storm>Forecast Hour";

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


    /**
     * _more_
     */
    private void checkVisibility() {
        List<WayDisplayState> wayDisplayStates = getWayDisplayStates();
        Color                 bgcolor          = Color.lightGray;

        boolean               rowOk =
            forecastState.getWayState().getVisible();
        forecastState.getRingsState().setBackground(rowOk
                ? null
                : bgcolor);
        forecastState.getConeState().setBackground(rowOk
                ? null
                : bgcolor);
        forecastState.getTrackState().setBackground(rowOk
                ? null
                : bgcolor);
        for (WayDisplayState wds : wayDisplayStates) {
            rowOk = wds.getWayState().getVisible();
            if (wds.getWay().isObservation()) {
                wds.getRingsState().setBackground(rowOk
                        ? null
                        : bgcolor);
                wds.getConeState().setBackground(rowOk
                        ? null
                        : bgcolor);
                wds.getTrackState().setBackground(rowOk
                        ? null
                        : bgcolor);
            } else {
                rowOk = rowOk && forecastState.getWayState().getVisible();
                wds.getWayState().setBackground(
                    forecastState.getWayState().getVisible()
                    ? null
                    : bgcolor);
                wds.getRingsState().setBackground((rowOk
                        && forecastState.getRingsState().getVisible())
                        ? null
                        : bgcolor);
                wds.getConeState().setBackground((rowOk
                        && forecastState.getConeState().getVisible())
                        ? null
                        : bgcolor);
                wds.getTrackState().setBackground((rowOk
                        && forecastState.getTrackState().getVisible())
                        ? null
                        : bgcolor);
            }
        }
    }

    /**
     * _more_
     */
    public void colorTableChanged() {
        try {
            updateDisplays();
        } catch (Exception exc) {
            stormTrackControl.logException("Changing color table", exc);
        }
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public LatLonRect getBoundingBox() {
        if (trackCollection == null) {
            return null;
        }
        double  minLon = Double.POSITIVE_INFINITY;
        double  maxLon = Double.NEGATIVE_INFINITY;
        double  minLat = Double.POSITIVE_INFINITY;
        double  maxLat = Double.NEGATIVE_INFINITY;

        boolean didone = false;
        for (StormTrack track : trackCollection.getTracks()) {
            if ( !stormTrackControl.okToShowWay(track.getWay())) {
                continue;
            }
            LatLonRect bbox = track.getBoundingBox();
            if (bbox == null) {
                continue;
            }
            minLon = Math.min(minLon, bbox.getLonMin());
            maxLon = Math.max(maxLon, bbox.getLonMax());
            minLat = Math.min(minLat, bbox.getLatMin());
            maxLat = Math.max(maxLat, bbox.getLatMax());
            didone = true;
        }
        if ( !didone) {
            return null;
        }
        return new LatLonRect(new LatLonPointImpl(maxLat, minLon),
                              new LatLonPointImpl(minLat, maxLon));
    }


    /**
     * _more_
     *
     * @param isOnlyChild _more_
     */
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
     *
     * @param sm _more_
     */
    public void setObsLayoutModel(StationModel sm) {
        obsLayoutModelName = ((sm == null)
                              ? null
                              : sm.getName());
        updateLayoutModel(true);
    }

    /**
     *  _more_
     * 
     *  @param sm _more_
     */
    public void setObsPointLayoutModel(StationModel sm) {
        obsPointLayoutModelName = ((sm == null)
                                   ? null
                                   : sm.getName());
        updateLayoutModel(true);
    }

    /**
     * _more_
     *
     * @param sm _more_
     */
    public void setForecastLayoutModel(StationModel sm) {
        forecastLayoutModelName = ((sm == null)
                                   ? null
                                   : sm.getName());
        updateLayoutModel(false);
    }



    /**
     * _more_
     *
     * @param name _more_
     */
    protected void handleChangedStationModel(String name) {
        if (Misc.equals(obsLayoutModelName, name)) {
            updateLayoutModel(true);
        }
        if (Misc.equals(forecastLayoutModelName, name)) {
            updateLayoutModel(false);
        }
    }


    /**
     * _more_
     *
     * @param forObs _more_
     */
    public void updateLayoutModel(boolean forObs) {
        List<WayDisplayState> wayDisplayStates = getWayDisplayStates();
        try {
            for (WayDisplayState wds : wayDisplayStates) {
                if (wds.getWay().isObservation() && !forObs) {
                    continue;
                }
                wds.updateLayoutModel();
            }
        } catch (Exception exc) {
            stormTrackControl.logException("Updating layout models", exc);
        }
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
        if ((stormParams == null) || (stormParams.size() == 0)) {
            return GuiUtils.filler(2, 10);
        }
        final JList list = new JList(new Vector(stormParams));
        list.setVisibleRowCount(3);
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

        list.setToolTipText(
            "<html>Parameter used for cone<br>Control-click for multiple select</html>");
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


    /**
     * _more_
     *
     * @param stormParams _more_
     * @param id _more_
     * @param tooltip _more_
     *
     * @return _more_
     */
    private JComponent makeBox(List stormParams, final Object id,
                               String tooltip) {
        if ((stormParams == null) || (stormParams.size() == 0)) {
            return GuiUtils.filler(2, 10);
        }
        final JComboBox box = new JComboBox(new Vector(stormParams));
        box.setToolTipText(tooltip);
        StormParam stormParam = (StormParam) params.get(id);
        if (stormParam != null) {
            box.setSelectedItem(stormParam);
        }
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                Object selected = box.getSelectedItem();
                if ((selected == null) || (selected instanceof String)) {
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


    /**
     * _more_
     *
     * @param params _more_
     *
     * @return _more_
     */
    private List<StormParam> getDistanceParams(List<StormParam> params) {
        if ((params == null) || (params.size() == 0)) {
            return null;
        }

        List<StormParam> attrNames = new ArrayList<StormParam>();
        for (StormParam param : params) {
            if (Unit.canConvert(param.getUnit(), CommonUnit.meter)) {

                attrNames.add(param);
            }

        }
        if (attrNames.size() == 0) {
            return null;
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


        List<StormParam> forecastParams = new ArrayList<StormParam>();
        Hashtable        seenParams     = new Hashtable();
        List<StormParam> obsParams      = new ArrayList<StormParam>();
        Hashtable        seenWays       = new Hashtable();
        for (StormTrack track : trackCollection.getTracks()) {
            //            if (seenWays.get(track.getWay()) != null) {
            //                continue;
            //            }
            //            seenWays.put(track.getWay(), track.getWay());
            List<StormParam> trackParams = track.getParams();
            if (track.getWay().isObservation()) {
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

        List<StormParam> forecastRadiusParams =
            getDistanceParams(forecastParams);
        List<StormParam> obsRadiusParams = getDistanceParams(obsParams);

        if (obsRadiusParams != null) {
            //If its not set then set it
            if (params.get(ID_OBS_RINGS) == null) {
                params.put(ID_OBS_RINGS, obsRadiusParams.get(0));
            }
            if (params.get(ID_OBS_CONE) == null) {
                params.put(ID_OBS_CONE, Misc.newList(obsRadiusParams.get(0)));
            }
        }
        if (forecastRadiusParams != null) {
            //If its not set then set it
            if (params.get(ID_FORECAST_RINGS) == null) {
                params.put(ID_FORECAST_RINGS, forecastRadiusParams.get(0));
            }
            if (params.get(ID_FORECAST_CONE) == null) {
                params.put(ID_FORECAST_CONE,
                           Misc.newList(obsRadiusParams.get(0)));
            }
        }





        //Sort them by name

        List<Way> ways             = Misc.sort(trackCollection.getWayList());
        boolean   haveDoneForecast = false;
        List<String> colLabels = (List<String>) Misc.newList("", "Show",
                                     "Track");
        if ((forecastRadiusParams != null) || (obsRadiusParams != null)) {
            colLabels.add("Rings");
            colLabels.add("Cone");
        }
        int  numCols             = colLabels.size();




        List obsColorParams      = new ArrayList(obsParams);
        List forecastColorParams = new ArrayList(forecastParams);
        obsColorParams.add(0, "Fixed");
        forecastColorParams.add(0, "Fixed");


        JComponent obsLayoutComp = new LayoutModelWidget(stormTrackControl,
                                       this, "setObsLayoutModel",
                                       getObsLayoutModel(), true);
        JComponent obsPointLayoutComp =
            new LayoutModelWidget(stormTrackControl, this,
                                  "setObsPointLayoutModel",
                                  getObsPointLayoutModel(), true);
        JComponent forecastLayoutComp =
            new LayoutModelWidget(stormTrackControl, this,
                                  "setForecastLayoutModel",
                                  getForecastLayoutModel(), true);

        JComponent obsColorByBox =
            makeBox(obsColorParams, ID_OBS_COLOR,
                    "Parameter used for coloring observation track");
        JComponent forecastColorByBox =
            makeBox(forecastColorParams, ID_FORECAST_COLOR,
                    "Parameter used for coloring forecast tracks");

        JComponent obsConeComp = ((obsRadiusParams != null)
                                  ? makeList(obsRadiusParams, ID_OBS_CONE)
                                  : (JComponent) GuiUtils.filler());
        JComponent obsRingComp = ((obsRadiusParams != null)
                                  ? makeBox(
                                      obsRadiusParams, ID_OBS_RINGS,
                                      "Parameter used for observation rings")
                                  : (JComponent) GuiUtils.filler());


        JComponent forecastConeComp = ((forecastRadiusParams != null)
                                       ? makeList(forecastRadiusParams,
                                           ID_FORECAST_CONE)
                                       : (JComponent) GuiUtils.filler());
        JComponent forecastRingComp = ((forecastRadiusParams != null)
                                       ? makeBox(forecastRadiusParams,
                                           ID_FORECAST_RINGS,
                                           "Parameter used for forecast rings")
                                       : (JComponent) GuiUtils.filler());



        List topComps = new ArrayList();

        topComps.add(new JLabel(""));
        topComps.add(
            GuiUtils.cLabel("<html><u><i>Observation</i></u></html>"));
        topComps.add(GuiUtils.cLabel("<html><u><i>Forecast</i></u></html>"));

        topComps.add(GuiUtils.rLabel("Points:"));
        topComps.add(obsPointLayoutComp);
        topComps.add(forecastLayoutComp);

        topComps.add(GuiUtils.rLabel("Animation:"));
        topComps.add(obsLayoutComp);
        topComps.add(GuiUtils.filler());


        forecastColorTableLabel = new JLabel(" ");
        forecastColorTableLabel.setToolTipText("Color table preview");
        obsColorTableLabel = new JLabel(" ");
        obsColorTableLabel.setToolTipText("Color table preview");

        topComps.add(GuiUtils.rLabel("Color By:"));
        topComps.add(GuiUtils.vbox(obsColorByBox, obsColorTableLabel));
        topComps.add(GuiUtils.vbox(forecastColorByBox,
                                   forecastColorTableLabel));


        if ((forecastRadiusParams != null) || (obsRadiusParams != null)) {
            topComps.add(GuiUtils.rLabel("Rings:"));
            topComps.add(obsRingComp);
            topComps.add(forecastRingComp);
            topComps.add(GuiUtils.rLabel("Cone:"));
            topComps.add(obsConeComp);
            topComps.add(forecastConeComp);
        }


        GuiUtils.tmpInsets = new Insets(4, 4, 2, 2);
        JComponent paramComp = GuiUtils.doLayout(topComps, 3, GuiUtils.WT_N,
                                   GuiUtils.WT_N);


        List comps = new ArrayList();

        for (Way way : ways) {
            WayDisplayState wds = getWayDisplayState(way);
            if ( !stormTrackControl.okToShowWay(wds.getWay())) {
                continue;
            }
            JComponent labelComp =
                GuiUtils.hbox(wds.getWayState().getCheckBox(),
                              new JLabel(" " + way.toString()));

            JComponent swatch = GuiUtils.wrap(wds.getColorSwatch());
            if (way.isObservation()) {
                //We put the obs in the front of the list
                int col = 0;
                comps.add(col++, swatch);
                comps.add(col++, labelComp);
                comps.add(col++,
                          GuiUtils.wrap(wds.getTrackState().getCheckBox()));
                if (obsRadiusParams != null) {
                    comps.add(
                        col++,
                        GuiUtils.wrap(wds.getRingsState().getCheckBox()));
                    comps.add(
                        col++,
                        GuiUtils.wrap(wds.getConeState().getCheckBox()));
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
                            forecastState.getWayState().getCheckBox(),
                            GuiUtils.lLabel(
                                "<html><u><i>Forecasts:</i></u></html>")));
                    comps.add(
                        GuiUtils.wrap(
                            forecastState.getTrackState().getCheckBox()));
                    if (forecastRadiusParams != null) {
                        comps.add(
                            GuiUtils.wrap(
                                forecastState.getRingsState().getCheckBox()));

                        comps.add(
                            GuiUtils.wrap(
                                forecastState.getConeState().getCheckBox()));
                    }
                }
                comps.add(swatch);
                comps.add(labelComp);
                comps.add(GuiUtils.wrap(wds.getTrackState().getCheckBox()));
                if (forecastRadiusParams != null) {
                    comps.add(
                        GuiUtils.wrap(wds.getRingsState().getCheckBox()));
                    comps.add(
                        GuiUtils.wrap(wds.getConeState().getCheckBox()));
                }
            }
        }

        for (int colIdx = 0; colIdx < numCols; colIdx++) {
            String s = colLabels.get(colIdx);
            if (s.length() > 0) {
                comps.add(colIdx,
                          new JLabel("<html><u><i>" + s + "</i></u></html>"));
            } else {
                comps.add(colIdx, new JLabel(""));
            }
        }


        GuiUtils.tmpInsets = new Insets(2, 2, 0, 2);
        JComponent wayComp = GuiUtils.topLeft(GuiUtils.doLayout(comps,
                                 numCols, GuiUtils.WT_N, GuiUtils.WT_N));
        //Put the list of ways into a scroller if there are lots of them
        if (ways.size() > 6) {
            int width  = 300;
            int height = 200;
            JScrollPane scroller = GuiUtils.makeScrollPane(wayComp, width,
                                       height);
            scroller.setBorder(BorderFactory.createLoweredBevelBorder());
            scroller.setPreferredSize(new Dimension(width, height));
            scroller.setMinimumSize(new Dimension(width, height));
            wayComp = scroller;
        }

        wayComp = GuiUtils.left(GuiUtils.doLayout(new Component[]{GuiUtils.left(paramComp),
                                                                     GuiUtils.filler(2, 10), 
                                                                     GuiUtils.left(wayComp)},
                1, GuiUtils.WT_N,GuiUtils.WT_NNY));

        wayComp = GuiUtils.inset(wayComp, new Insets(0, 5, 0, 0));
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

        checkVisibility();
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
                        stormTrackControl.showWaitCursor();
                        showStormInner();
                        stormTrackControl.stormChanged(
                            StormDisplayState.this);
                    }
                } catch (Exception exc) {
                    stormTrackControl.logException("Showing storm", exc);
                } finally {
                    stormTrackControl.showNormalCursor();
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
        if (holder != null) {
            holder.addDisplayable(displayable);
        }
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
                    stormInfo, stormTrackControl.getOkWays(),
                    stormTrackControl.getObservationWay());
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
            StormTrack     obsTrack = trackCollection.getObsTrack();

            List<DateTime> times    = new ArrayList<DateTime>();
            if (obsTrack != null) {
                times = obsTrack.getTrackTimes();
            } else {
                for (StormTrack track : trackCollection.getTracks()) {
                    times.add(track.getStartTime());
                }
            }
            if (times.size() > 0) {
                times = (List<DateTime>) Misc.sort(Misc.makeUnique(times));
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
        //        System.err.println("time:" + (t2 - t1));
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
            return (StormParam) params.get(ID_OBS_RINGS);
        }
        return (StormParam) params.get(ID_FORECAST_RINGS);
    }



    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected StormParam getColorParam(WayDisplayState way) {
        return getColorParam(way.getWay().isObservation());
    }

    /**
     * _more_
     *
     * @param forObs _more_
     *
     * @return _more_
     */
    protected StormParam getColorParam(boolean forObs) {
        if (forObs) {
            return (StormParam) params.get(ID_OBS_COLOR);
        }
        return (StormParam) params.get(ID_FORECAST_COLOR);
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
     * @param displayState _more_
     *
     * @throws Exception _more_
     */
    protected void displayStateChanged(DisplayState displayState)
            throws Exception {
        updateDisplays();
        checkVisibility();
    }

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void updateDisplays() throws Exception {
        DisplayMaster displayMaster = stormTrackControl.getDisplayMaster();
        boolean       wasActive     = displayMaster.ensureInactive();
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


        if (obsColorTableLabel != null) {
            ColorTable ct = null;

            ct = getColorTable(getColorParam(true));
            obsColorTableLabel.setIcon(((ct != null)
                                        ? ColorTableCanvas.getIcon(ct)
                                        : null));

            ct = getColorTable(getColorParam(false));
            forecastColorTableLabel.setIcon(((ct != null)
                                             ? ColorTableCanvas.getIcon(ct)
                                             : null));

            obsColorTableLabel.setToolTipText(getColorTableToolTip(true));
            forecastColorTableLabel.setToolTipText(
                getColorTableToolTip(false));
        }
    }


    /**
     * _more_
     *
     * @param forObs _more_
     *
     * @return _more_
     */
    protected String getColorTableToolTip(boolean forObs) {
        StormParam param = getColorParam(forObs);
        if (param == null) {
            return "Color table preview";
        }
        Range range =
            getStormTrackControl().getIdv().getParamDefaultsEditor()
                .getParamRange(param.getName());
        if (range == null) {
            return "Color table preview";
        }

        Unit displayUnit =
            getStormTrackControl().getIdv().getParamDefaultsEditor()
                .getParamDisplayUnit(param.getName());

        String unit = ((displayUnit != null)
                       ? "[" + displayUnit + "]"
                       : "");
        return "Range: " + range.getMin() + unit + " - " + range.getMax()
               + unit;
    }

    /**
     * _more_
     *
     * @param param _more_
     *
     * @return _more_
     */
    protected ColorTable getColorTable(StormParam param) {
        if (param == null) {
            return null;
        }
        ColorTable ct =
            getStormTrackControl().getIdv().getParamDefaultsEditor()
                .getParamColorTable(param.getName(), false);
        if (ct == null) {
            ct = getStormTrackControl().getColorTable();
        }
        return ct;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected StationModel getObsLayoutModel() {
        if ((obsLayoutModelName == null)
                || obsLayoutModelName.equals("none")) {
            return null;
        }

        StationModelManager smm =
            stormTrackControl.getControlContext().getStationModelManager();
        return smm.getStationModel(obsLayoutModelName);
        /*
        StationModel model       = new StationModel("TrackLocation");
        ShapeSymbol  shapeSymbol = new ShapeSymbol(0, 0);
        shapeSymbol.setShape(ucar.visad.ShapeUtility.HURRICANE);
        shapeSymbol.setScale(2.0f);
        shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
        shapeSymbol.setRectPoint(Glyph.PT_MM);
        shapeSymbol.setForeground(null);
        model.addSymbol(shapeSymbol);
        return model;
        */
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected StationModel getObsPointLayoutModel() {
        if ((obsPointLayoutModelName == null)
                || obsPointLayoutModelName.equals("none")) {
            return null;
        }

        StationModelManager smm =
            stormTrackControl.getControlContext().getStationModelManager();
        return smm.getStationModel(obsPointLayoutModelName);
        /*
        StationModel model       = new StationModel("TrackLocation");
        ShapeSymbol  shapeSymbol = new ShapeSymbol(0, 0);
        shapeSymbol.setShape(ucar.visad.ShapeUtility.HURRICANE);
        shapeSymbol.setScale(2.0f);
        shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
        shapeSymbol.setRectPoint(Glyph.PT_MM);
        shapeSymbol.setForeground(null);
        model.addSymbol(shapeSymbol);
        return model;
        */
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected StationModel getForecastLayoutModel() {
        if ((forecastLayoutModelName == null)
                || forecastLayoutModelName.equals("none")) {
            return null;
        }
        StationModelManager smm =
            stormTrackControl.getControlContext().getStationModelManager();
        StationModel sm = smm.getStationModel(forecastLayoutModelName);
        if (sm != null) {
            return sm;
        }
        StationModel model       = new StationModel("TrackLocation");
        ShapeSymbol  shapeSymbol = new ShapeSymbol(0, 0);
        shapeSymbol.setScale(0.3f);
        shapeSymbol.setShape(ucar.visad.ShapeUtility.CIRCLE);
        shapeSymbol.bounds = new java.awt.Rectangle(-15, -15, 30, 30);
        shapeSymbol.setRectPoint(Glyph.PT_MM);
        model.addSymbol(shapeSymbol);
        return model;
    }

    //        ucar.visad.Util.makeTimeField(List<Data> ranges, List times)

    /**
     *    Animation animation = stormTrackControl.getAnimation();
     *    if (animation == null) {
     *       return;
     *   }
     *   List<StormTrack> visibleTracks = new ArrayList<StormTrack>();
     *   Real currentAnimationTime = animation.getAniValue();
     *  if (currentAnimationTime == null || currentAnimationTime.isMissing()) {
     *       return;
     *   }
     *  Iterate way display states
     *    boolean             visible = false;
     *    if(wds.shouldShowTrack() && wds.hasTrackDisplay()) {
     *    FieldImpl field = (FieldImplt)wds.getTrackDisplay().getData()
     *    if(field==null) continue;
     *    Set timeSet = GridUtil.getTimeSet();
     *    if(timeSet == null) continue;
     *    if (timeSet.getLength() == 1) {
     *       visible = true;
     *   } else {
     *       //Else work the visad magic
     *       float timeValueFloat = (float) currentAnimationTime.getValue(
     *                                  timeSet.getSetUnits()[0]);
     *       //            System.err.println("multiple times:" + timeValueFloat);
     *       float[][] value = {
     *           { timeValueFloat }
     *       };
     *       int[]     index = timeSet.valueToIndex(value);
     *       //            System.err.println("index:" + index[0]);
     *       visible = (index[0] >= 0);
     *   }
     *   if(visible) {
     *   //Find the closest track in wds in time
     *        visibleTracks.add(..);
     *   }
     *   }
     *
     *
     *   Now search in space
     *
     * @param stormTrackChart _more_
     */







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
            TableSorter  sorter     = new TableSorter(tableModel);
            JTable       trackTable = new JTable(sorter);
            JTableHeader header     = trackTable.getTableHeader();
            header.setToolTipText("Click to sort");
            sorter.setTableHeader(trackTable.getTableHeader());

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
            JCheckBox obsCbx      = new JCheckBox("Observation", true);
            JCheckBox forecastCbx = new JCheckBox("Forecast", true);
            JCheckBox mostRecentCbx = new JCheckBox("Most Recent Forecasts",
                                          false);
            JComponent accessory = GuiUtils.top(GuiUtils.vbox(obsCbx,
                                       forecastCbx, mostRecentCbx));

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
            row.createCell((short) 0).setCellValue(stp.getTime().toString());
            row.createCell((short) 1).setCellValue(
                stp.getLocation().getLatitude().getValue());
            row.createCell((short) 2).setCellValue(
                stp.getLocation().getLongitude().getValue());
            for (int colIdx = 0; colIdx < params.size(); colIdx++) {
                Real r = stp.getAttribute(params.get(colIdx));
                cell = row.createCell((short) (colIdx + 3));
                cell.setCellValue(r.getValue());
            }
        }
    }





    /**
     * _more_
     *
     * @param docNode _more_
     * @param state _more_
     * @param doObs _more_
     * @param doForecast _more_
     * @param mostRecent _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void writeToKml(Element docNode, Hashtable state, boolean doObs,
                           boolean doForecast, boolean mostRecent)
            throws VisADException, RemoteException {
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

            Element topFolder =
                KmlUtil.folder(
                    docNode,
                    "Storm: " + stormInfo.toString() + "   "
                    + stormInfo.getStartTime().formattedString(
                        "yyyy-MM-dd", DateUtil.TIMEZONE_GMT));
            StormTrack obsTrack = trackCollection.getObsTrack();
            //Write the obs track first
            if ((obsTrack != null) && doObs) {
                Element obsFolder = KmlUtil.folder(topFolder, "Observation");
                stormTrackControl.writeToGE(
                    docNode, state, obsFolder, obsTrack,
                    getWayDisplayState(obsTrack.getWay()).getColor());
            }
            if (doForecast) {
                waysToUse = Misc.sort(waysToUse);
                for (Way way : waysToUse) {
                    if (way.isObservation()) {
                        continue;
                    }
                    Element wayNode = KmlUtil.folder(topFolder,
                                          stormTrackControl.getWayName()
                                          + ": " + way);
                    List<StormTrack> tracks =
                        (List<StormTrack>) Misc.sort(trackMap.get(way));
                    if (mostRecent) {
                        StormTrack recent = tracks.get(tracks.size() - 1);
                        stormTrackControl.writeToGE(
                            docNode, state, wayNode, recent,
                            getWayDisplayState(recent.getWay()).getColor());
                    } else {
                        for (StormTrack track : tracks) {
                            stormTrackControl.writeToGE(
                                docNode, state, wayNode, track,
                                getWayDisplayState(
                                    track.getWay()).getColor());

                        }
                    }
                }
            }

        } catch (Exception exc) {
            stormTrackControl.logException("Writing KML", exc);
        }
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
    public WayDisplayState getObservationState() {
        return obsDisplayState;
    }

    /**
     *  Set the ForecastState property.
     *
     *  @param value The new value for ForecastState
     */
    public void setObservationState(WayDisplayState value) {
        obsDisplayState = value;
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
    public static Color getNextColor(int[] nextColor) {
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


    /**
     * Set the ObsLayoutModelName property.
     *
     * @param value The new value for ObsLayoutModelName
     */
    public void setObsLayoutModelName(String value) {
        obsLayoutModelName = value;
    }

    /**
     * Get the ObsLayoutModelName property.
     *
     * @return The ObsLayoutModelName
     */
    public String getObsLayoutModelName() {
        return obsLayoutModelName;
    }

    /**
     *  Set the ObsLayoutModelName property.
     * 
     *  @param value The new value for ObsLayoutModelName
     */
    public void setObsPointLayoutModelName(String value) {
        obsPointLayoutModelName = value;
    }

    /**
     * Get the ObsLayoutModelName property.
     *
     * @return The ObsLayoutModelName
     */
    public String getObsPointLayoutModelName() {
        return obsPointLayoutModelName;
    }


    /**
     * Set the ForecastLayoutModelName property.
     *
     * @param value The new value for ForecastLayoutModelName
     */
    public void setForecastLayoutModelName(String value) {
        forecastLayoutModelName = value;
    }

    /**
     * Get the ForecastLayoutModelName property.
     *
     * @return The ForecastLayoutModelName
     */
    public String getForecastLayoutModelName() {
        return forecastLayoutModelName;
    }




}

