/**
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


import ucar.unidata.data.DataChoice;

import ucar.unidata.data.point.*;
import ucar.unidata.data.storm.*;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.TreePanel;
import ucar.unidata.ui.TwoListPanel;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.ui.drawing.*;
import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.MenuUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;

import ucar.visad.display.*;

import visad.*;
import visad.Set;

import visad.georef.EarthLocation;

import java.awt.*;

import java.rmi.RemoteException;

import java.util.*;
import java.util.List;

import javax.swing.*;


/**
 * A MetApps Display Control with Displayable and controls for
 * displaying a track (balloon sounding or aircraft track)
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormTrackControl extends DisplayControlImpl {


    /** _more_          */
    private final static String PREF_STORMDISPLAYSTATE =
        "pref.stormdisplaystate";

    /** _more_          */
    private final static String PREF_OKWAYS = "pref.okways";

    /** _more_          */
    private final static String PREF_OKPARAMS = "pref.okparams";

    /** _more_ */
    private static int cnt = 0;



    /** _more_ */
    final ImageIcon ICON_ON =
        GuiUtils.getImageIcon("/ucar/unidata/idv/control/storm/dot.gif");

    /** _more_ */
    final ImageIcon ICON_OFF =
        GuiUtils.getImageIcon("/ucar/unidata/idv/control/storm/blank.gif");



    private StormDisplayState localStormDisplayState;

    /** _more_          */
    private Hashtable preferences;


    /** _more_ */
    private Hashtable<String, Boolean> okWays;

    /** _more_ */
    private Hashtable<String, Boolean> okParams;


    /** _more_ */
    private String startTime;

    /** _more_ */
    private String endTime;

    /** _more_ */
    private CompositeDisplayable placeHolder;

    /** _more_ */
    private StormDataSource stormDataSource;


    /** _more_ */
    private List<StormInfo> stormInfos;


    /** Holds the EarthLocation of the last point clicked */
    private EarthLocation lastEarthLocation = null;


    /** _more_ */
    private Hashtable<StormInfo, StormDisplayState> stormDisplayStateMap =
        new Hashtable<StormInfo, StormDisplayState>();


    /** _more_ */
    private List<StormDisplayState> activeStorms;

    /** _more_ */
    private TreePanel treePanel;

    private Hashtable<Integer,JComponent> yearToComponent = new Hashtable<Integer,JComponent>();
    /**
     * Create a new Track Control; set the attribute flags
     */
    public StormTrackControl() {
        setAttributeFlags(FLAG_COLORTABLE);
    }




    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        placeHolder = new CompositeDisplayable("Place holder");
        addDisplayable(placeHolder);

        List dataSources = new ArrayList();
        dataChoice.getDataSources(dataSources);

        if (dataSources.size() != 1) {
            userMessage("Could not find Storm Data Source");
            return false;
        }


        if ( !(dataSources.get(0) instanceof StormDataSource)) {
            userMessage("Could not find Storm Data Source");
            return false;
        }

        getColorTableWidget(getRangeForColorTable());
        stormDataSource = (StormDataSource) dataSources.get(0);

        if (okWays == null) {
            okWays = (Hashtable<String,
                                Boolean>) getPreferences().get(PREF_OKWAYS);
        }
        if (okWays == null) {
            okWays = new Hashtable<String, Boolean>();
        }

        if (okParams == null) {
            okParams =
                (Hashtable<String,
                           Boolean>) getPreferences().get(PREF_OKPARAMS);
        }
        if (okParams == null) {
            okParams = new Hashtable<String, Boolean>();
        }


        return true;
    }






    /**
     * _more_
     *
     * @param track _more_
     *
     * @param param _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected FieldImpl makeTrackField(StormTrack track, StormParam param)
            throws Exception {

        List<StormTrackPoint> points =  track.getTrackPoints();
        int                 numPoints = points.size();
        Unit                timeUnit  = points.get(0).getTrackPointTime().getUnit();

        RealType dfltRealType = RealType.getRealType("Default_" + (cnt++));
        Real                dfltReal  = new Real(dfltRealType, 1);

        RealType timeType =
            RealType.getRealType(DataUtil.cleanName("track_time" + cnt + "_"
                + timeUnit), timeUnit);
        RealTupleType rangeType    = null;
        double[][]    newRangeVals = new double[2][numPoints];
        float[]       alts         = new float[numPoints];
        float[]       lats         = new float[numPoints];
        float[]       lons         = new float[numPoints];
        Real[]        values       = ((param == null)
                                      ? null
                                      : track.getTrackAttributeValues(param));
        for (int pointIdx = 0; pointIdx < numPoints; pointIdx++) {
            StormTrackPoint stp = points.get(pointIdx);
            Real value = ((values == null)
                          ? dfltReal
                          : values[pointIdx]);
            //Set the dflt so we can use its unit later
            dfltReal = value;
            if (rangeType == null) {
                rangeType =
                    new RealTupleType(RealType.getRealType("trackrange_"
                        + cnt, value.getUnit()), timeType);
            }
            DateTime      dateTime = stp.getTrackPointTime();
            EarthLocation el       = stp.getTrackPointLocation();
            newRangeVals[0][pointIdx] = value.getValue();
            newRangeVals[1][pointIdx] = dateTime.getValue();
            lats[pointIdx]            = (float) el.getLatitude().getValue();
            lons[pointIdx]            = (float) el.getLongitude().getValue();
            //            System.err.println("\tpt:" + el + " " + dateTime);
            alts[pointIdx]            = 1;
            //            if(Math.abs(lats[i])>90) System.err.println("bad lat:" + lats[i]);
        }
        GriddedSet llaSet = ucar.visad.Util.makeEarthDomainSet(lats, lons,
                                alts);
        Set[] rangeSets = new Set[2];
        rangeSets[0] = new DoubleSet(new SetType(rangeType.getComponent(0)));
        rangeSets[1] = new DoubleSet(new SetType(rangeType.getComponent(1)));
        FunctionType newType =
            new FunctionType(((SetType) llaSet.getType()).getDomain(),
                             rangeType);
        FlatField timeTrack = new FlatField(newType, llaSet,
                                            (CoordinateSystem) null,
                                            rangeSets,
                                            new Unit[] { dfltReal.getUnit(),
                timeUnit });
        timeTrack.setSamples(newRangeVals, false);
        return timeTrack;
    }






    /**
     * _more_
     *
     * @return _more_
     */
    public DisplayMaster getDisplayMaster() {
        return getDisplayMaster(placeHolder);
    }



    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected boolean okToShowWay(Way way) {
        if (way.isObservation()) {
            return true;
        }
        if (okWays == null) {
            return true;
        }
        if ((okWays.size() > 0) && (okWays.get(way.getId()) == null)) {
            return false;
        }
        return true;
    }

    /**
     * _more_
     *
     * @param realType _more_
     *
     * @return _more_
     */
    protected boolean okToShowParam(RealType realType) {
        if (okParams == null) {
            return true;
        }
        if ((okParams.size() > 0) && (okParams.get(realType) == null)) {
            return false;
        }
        return true;
    }


    /**
     * _more_
     */
    public void showWaySelectDialog() {
        List checkBoxes = new ArrayList();
        List useWays    = new ArrayList();
        List allWays    = new ArrayList<Way>();
        for (Way way : stormDataSource.getWays()) {
            if (way.isObservation()) {
                continue;
            }
            allWays.add(way);
            if (okToShowWay(way)) {
                useWays.add(way);
            }
        }
        useWays = Misc.sort(useWays);
        allWays = Misc.sort(allWays);
        JCheckBox writeAsPreferenceCbx = new JCheckBox("Save as preference",
                                             false);
        TwoListPanel tlp = new TwoListPanel(allWays, "Don't Use", useWays,
                                            "Use", null, false);
        JComponent contents = GuiUtils.centerBottom(tlp,
                                  GuiUtils.left(writeAsPreferenceCbx));
        if ( !GuiUtils.showOkCancelDialog(null, getWayName() + " Selection",
                                          contents, null)) {
            return;
        }
        List only = tlp.getCurrentEntries();

        if (only.size() == allWays.size()) {
            onlyShowTheseWays(new ArrayList<Way>(),
                              writeAsPreferenceCbx.isSelected());
        } else {
            onlyShowTheseWays((List<Way>) only,
                              writeAsPreferenceCbx.isSelected());
        }

    }




    /**
     * _more_
     *
     * @return _more_
     */
    public StormDisplayState getCurrentStormDisplayState() {
        if(localStormDisplayState!=null)  return localStormDisplayState;
        if(treePanel == null) return null;

        Component comp = treePanel.getVisibleComponent();
        if (comp == null) {
            return null;
        }
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);
            if (stormDisplayState.getContents() == comp) {
                return stormDisplayState;
            }
        }
        return null;
    }



    /**
     * _more_
     *
     * @param ways _more_
     * @param writeAsPreference _more_
     */
    protected void onlyShowTheseWays(List<Way> ways,
                                     boolean writeAsPreference) {
        okWays = new Hashtable();
        for (Way way : ways) {
            okWays.put(way.getId(), new Boolean(true));
        }
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);
            stormDisplayState.reload();
        }
        if (writeAsPreference) {
            putPreference(PREF_OKWAYS, okWays);
        }

    }


    /**
     * _more_
     *
     * @return _more_
     */
    public StormDataSource getStormDataSource() {
        return stormDataSource;
    }


    /**
     * _more_
     *
     * @param stormDisplayState _more_
     */
    public void viewStorm(StormDisplayState stormDisplayState) {
        if(treePanel != null)
            treePanel.show(stormDisplayState.getContents());
    }

    /**
     * _more_
     */
    public void unloadAllTracks() {
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);
            if (stormDisplayState.getActive()) {
                stormDisplayState.deactivate();
            }
        }
    }


    /**
     * _more_
     *
     * @param items _more_
     * @param forMenuBar _more_
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {
        StormDisplayState current = getCurrentStormDisplayState();
        if ((current != null) && current.getActive()) {
            items.add(GuiUtils.makeMenuItem("Export to Spreadsheet", current,
                                            "writeToXls"));
        }
        super.getSaveMenuItems(items, forMenuBar);
    }

    /**
     * _more_
     *
     * @param items _more_
     * @param forMenuBar _more_
     */
    protected void getEditMenuItems(List items, boolean forMenuBar) {
        StormDisplayState current = getCurrentStormDisplayState();
        if ((current != null) && current.getActive()) {
            items.add(GuiUtils.makeMenuItem("Add Forecast Time Chart",
                                            current, "addForecastTimeChart"));
            items.add(GuiUtils.makeMenuItem("Add Forecast Hour Chart",
                                            current, "addForecastHourChart"));
            items.add(GuiUtils.makeMenuItem("Select " + getWaysName()
                                            + " To Use", this,
                                                "showWaySelectDialog"));
            items.add(
                GuiUtils.makeMenuItem(
                    "Save Storm Display as Preference", this,
                    "saveStormDisplayState"));
        }
        if (getPreferences().get(PREF_STORMDISPLAYSTATE) != null) {
            items.add(
                GuiUtils.makeMenuItem(
                    "Remove Storm Display Preference", this,
                    "deleteStormDisplayState"));
        }
        super.getEditMenuItems(items, forMenuBar);
    }


    /**
     * _more_
     *
     * @param items _more_
     * @param forMenuBar _more_
     */
    protected void getViewMenuItems(List items, boolean forMenuBar) {
        try {
            List subMenus = new ArrayList();
            GregorianCalendar cal =
                new GregorianCalendar(DateUtil.TIMEZONE_GMT);
            Hashtable menus       = new Hashtable();
            List      activeItems = new ArrayList();
            for (int i = stormInfos.size() - 1; i >= 0; i--) {
                StormInfo stormInfo = stormInfos.get(i);
                cal.setTime(
                    ucar.visad.Util.makeDate(stormInfo.getStartTime()));
                int   year     = cal.get(Calendar.YEAR);
                JMenu yearMenu = (JMenu) menus.get("" + year);
                if (yearMenu == null) {
                    yearMenu = new JMenu("" + year);
                    menus.put("" + year, yearMenu);
                    subMenus.add(yearMenu);
                }
                StormDisplayState stormDisplayState =
                    getStormDisplayState(stormInfo);
                if (stormDisplayState.getActive()) {
                    activeItems.add(
                        MenuUtil.makeMenuItem(
                            stormInfo.toString(), this, "viewStorm",
                            stormDisplayState));
                }
                if (stormInfo.getBasin() != null) {
                    JMenu basinMenu = (JMenu) menus.get(year + "Basin:"
                                          + stormInfo.getBasin());
                    if (basinMenu == null) {
                        basinMenu = new JMenu("Basin:"
                                + stormInfo.getBasin());
                        menus.put(year + "Basin:" + stormInfo.getBasin(),
                                  basinMenu);
                        yearMenu.add(basinMenu);
                    }
                    yearMenu = basinMenu;
                }
                yearMenu.add(GuiUtils.makeMenuItem(stormInfo.toString(),
                        this, "viewStorm", stormDisplayState));
            }

            JMenu trackMenu = GuiUtils.makeMenu("Storm Tracks", subMenus);
            GuiUtils.limitMenuSize(trackMenu, "Tracks:", 30);



            if (activeItems.size() > 0) {
                activeItems.add(0, GuiUtils.MENU_SEPARATOR);
                activeItems.add(0, GuiUtils.makeMenuItem("Unload all tracks",
                        this, "unloadAllTracks", null));
                trackMenu.insert(GuiUtils.makeMenu("Active Tracks",
                        activeItems), 0);
            }

            items.add(trackMenu);
            super.getViewMenuItems(items, forMenuBar);
        } catch (Exception exc) {
            logException("Making track menu", exc);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getWayName() {
        return "Way";
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String getWaysName() {
        return "Ways";
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private List<StormDisplayState> getActiveStorms() {
        if (activeStorms == null) {
            List<StormDisplayState> tmpList =
                new ArrayList<StormDisplayState>();
            List<StormDisplayState> stormDisplayStates =
                getStormDisplayStates();
            for (StormDisplayState stormDisplayState : stormDisplayStates) {
                if (stormDisplayState.getActive()) {
                    tmpList.add(stormDisplayState);
                }
            }
            activeStorms = tmpList;
        }
        return activeStorms;
    }



    /**
     * _more_
     *
     * @return _more_
     */
    private Hashtable getPreferences() {
        if (preferences == null) {
            String path = stormDataSource.getClass().getName()
                          + ".StormTrackControl.xml";
            preferences =
                (Hashtable) getIdv().getStore().getEncodedFile(path);
            if (preferences == null) {
                preferences = new Hashtable();
            }
        }
        return preferences;
    }



    /**
     * _more_
     */
    public void deleteStormDisplayState() {
        String template =
            (String) getPreferences().get(PREF_STORMDISPLAYSTATE);
        if (template != null) {
            getPreferences().remove(PREF_STORMDISPLAYSTATE);
            writePreferences();
        }
    }

    /**
     * _more_
     */
    public void saveStormDisplayState() {
        try {
            StormDisplayState current = getCurrentStormDisplayState();
            if (current == null) {
                return;
            }
            boolean wasActive = current.getActive();
            current.setActive(false);
            current.setStormTrackControl(null);
            String xml = getIdv().encodeObject(current, false);
            current.setStormTrackControl(this);
            current.setActive(wasActive);
            putPreference(PREF_STORMDISPLAYSTATE, xml);
            userMessage(
                "<html>Preference saved. <br>Note: This will take effect for new display controls</html>");
        } catch (Exception exc) {
            logException("Saving storm display", exc);
        }

    }

    /**
     * _more_
     */
    private void writePreferences() {
        String path = stormDataSource.getClass().getName()
                      + ".StormTrackControl.xml";
        getIdv().getStore().putEncodedFile(path, preferences);
    }

    /**
     * _more_
     *
     * @param key _more_
     * @param object _more_
     */
    private void putPreference(String key, Object object) {
        getPreferences().put(key, object);
        writePreferences();
    }


    /**
     * _more_
     *
     * @param stormInfo _more_
     *
     * @return _more_
     */
    private StormDisplayState getStormDisplayState(StormInfo stormInfo) {
        StormDisplayState stormDisplayState =
            stormDisplayStateMap.get(stormInfo);
        try {
            if (stormDisplayState == null) {
                String template =
                    (String) getPreferences().get(PREF_STORMDISPLAYSTATE);
                if (template != null) {
                    try {
                        stormDisplayState =
                            (StormDisplayState) getIdv().decodeObject(
                                template);
                        stormDisplayState.setStormInfo(stormInfo);
                    } catch (Exception exc) {
                        logException("Creating storm display", exc);
                        System.err.println("Error decoding preference:"
                                           + exc);
                        //noop
                    }
                }
            }
            if (stormDisplayState == null) {
                stormDisplayState = new StormDisplayState(stormInfo);
            }

            stormDisplayState.setStormTrackControl(this);
            stormDisplayStateMap.put(stormInfo, stormDisplayState);
        } catch (Exception exc) {
            logException("Creating storm display", exc);
        }

        return stormDisplayState;
    }


    /**
     * _more_
     */
    public void initDone() {
        super.initDone();
        try {
            for (Enumeration keys = stormDisplayStateMap.keys();
                    keys.hasMoreElements(); ) {
                StormInfo key = (StormInfo) keys.nextElement();
                StormDisplayState stormDisplayState =
                    stormDisplayStateMap.get(key);
                stormDisplayState.setStormTrackControl(this);
                stormDisplayState.initDone();
            }
        } catch (Exception exc) {
            logException("Setting new storm info", exc);
        }
        if(yearList.size()>0) {
            loadYears();
        }
    }



    private Hashtable yearData = new Hashtable();
    private TrackDisplayable yearTrackDisplay;
    private StationModelDisplayable yearLabelDisplay;
    private List<Integer> yearList = new ArrayList<Integer>();


    public void removeYear(Integer y) {
        if(!yearList.contains(y)) return;
        yearList.remove(y);
        final JComponent yearComponent = yearToComponent.get(y);
        if(yearComponent!=null) {
            yearComponent.removeAll();
            yearComponent.add(BorderLayout.CENTER,
                              GuiUtils.topLeft(
                                               GuiUtils.makeButton("Load Year", this, "loadYear", y)));
            yearComponent.invalidate();
            yearComponent.validate();
            yearComponent.repaint();
        }
        loadYears();
    }

    public void loadYear(Integer y) {
        if(yearList.contains(y)) return;
        yearList.add(y);
        final JComponent yearComponent = yearToComponent.get(y);
        if(yearComponent!=null) {
            yearComponent.removeAll();
            yearComponent.add(BorderLayout.CENTER,
                              GuiUtils.topLeft(
                                               GuiUtils.makeButton("Remove Year", this, "removeYear", y)));
            yearComponent.invalidate();
            yearComponent.validate();
            yearComponent.repaint();
        }
        loadYears();
    }


    public void loadYears() {
        Misc.run(new Runnable() {
                public void run() {
                    loadYearsInner();
                }
            });
    }


    private void loadYearsInner() {
        try {
            JLabel label = new JLabel();
            GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
            //Go in reverse order so we get the latest first
            List fields = new ArrayList();
            List times  = new ArrayList();
            Hashtable<String, Boolean> obsWays  = new Hashtable<String, Boolean>();
            obsWays.put(Way.OBSERVATION.toString(), new Boolean(true));
            List<PointOb> pointObs = new     ArrayList<PointOb>();
            TextType textType = TextType.getTextType("ID");
            for(Integer y: yearList) {
                int year   = y.intValue();
                for (int i = stormInfos.size() - 1; i >= 0; i--) {
                    StormInfo stormInfo = stormInfos.get(i);
                    cal.setTime(ucar.visad.Util.makeDate(stormInfo.getStartTime()));
                    int stormYear = cal.get(Calendar.YEAR);
                    if(stormYear!=year) continue;

                    Object key  = year+"_"+stormInfo.getStormId();
                    StormTrack obsTrack =  (StormTrack) yearData.get(key);
                    if(obsTrack == null) {
                        //                        label.setText("Loading " + stormInfo);
                        StormTrackCollection tracks= stormDataSource.getTrackCollection(stormInfo,
                                                                                        obsWays);
                        obsTrack = tracks.getObsTrack();
                        if(obsTrack == null) continue;
                        yearData.put(key, obsTrack);
                    }

                    FieldImpl field = makeTrackField(obsTrack, null);
                    StormTrackPoint stp = obsTrack.getTrackPoints().get(0);
                    times.add(stormInfo.getStartTime());
                    fields.add(field);

                    Tuple tuple = new Tuple(new Data[] {
                        new visad.Text(textType, stormInfo.toString()) });
                    pointObs.add(
                                 PointObFactory.makePointOb(
                                                            stp.getTrackPointLocation(), stormInfo.getStartTime(), tuple));
                }
            }

            if(times.size()==0) {
                removeDisplayable(yearTrackDisplay);
                removeDisplayable(yearLabelDisplay);
                yearTrackDisplay = null;
                yearLabelDisplay = null;
                return;
            }

            if(yearTrackDisplay == null) {
                yearTrackDisplay = new TrackDisplayable("year track ");
                addDisplayable(yearTrackDisplay);

                yearLabelDisplay =  new StationModelDisplayable("storm year labels");
                yearLabelDisplay.setScale(1.0f);
                StationModelManager smm =
                    getControlContext().getStationModelManager();
                StationModel model =
                    smm.getStationModel("Location");
                yearLabelDisplay.setStationModel(model);
                addDisplayable(yearLabelDisplay);
            }

            yearTrackDisplay.setTrack(Util.makeTimeField(fields, times));
            yearLabelDisplay.setStationData(
                                            PointObFactory.makeTimeSequenceOfPointObs(pointObs, -1, -1));

        } catch (Exception exc) {
            logException("Loading year", exc);
        }

    }


    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {


        //Get the storm infos and sort them
        stormInfos =
            (List<StormInfo>) Misc.sort(stormDataSource.getStormInfos());

        if(stormInfos.size()==1) {
            try {
                if(localStormDisplayState==null) {
                    localStormDisplayState = new StormDisplayState(stormInfos.get(0));
                }
                stormDisplayStateMap =
                    new Hashtable<StormInfo, StormDisplayState>();
                localStormDisplayState.setStormTrackControl(this);
                stormDisplayStateMap.put(stormInfos.get(0), localStormDisplayState);
                localStormDisplayState.setIsOnlyChild(true);
                JComponent comp =  localStormDisplayState.getContents();
                localStormDisplayState.loadStorm();
                return comp;
            } catch (Exception exc) {
                logException("Creating storm display", exc);
                return new JLabel("Error");
            }
        }
        localStormDisplayState = null;
        treePanel = new TreePanel(true, 150);
        Hashtable         years                  = new Hashtable();
        JComponent        firstComponent         = null;
        JComponent        firstSelectedComponent = null;
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        //Go in reverse order so we get the latest first
        for (int i = stormInfos.size() - 1; i >= 0; i--) {
            StormInfo stormInfo = stormInfos.get(i);
            cal.setTime(ucar.visad.Util.makeDate(stormInfo.getStartTime()));
            int year = cal.get(Calendar.YEAR);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);

            String category = "" + year;
            if (years.get(category) == null) {
                years.put(category, category);
                JPanel yearComponent = new JPanel(new BorderLayout());
                if(yearList.contains(new Integer(year))) {
                    yearComponent.add(BorderLayout.CENTER,
                                      GuiUtils.topLeft(
                                                       GuiUtils.makeButton("Remove Year", this, "removeYear", new Integer(year))));

                } else {
                    yearComponent.add(BorderLayout.CENTER,
                                      GuiUtils.topLeft(
                                                       GuiUtils.makeButton("Load Year", this, "loadYear", new Integer(year))));
                }
                yearToComponent.put(new Integer(year), yearComponent);
                treePanel.addCategoryComponent(category, GuiUtils.inset(yearComponent, new Insets(5,5,5,5)));
            }
            JComponent panelContents = stormDisplayState.getContents();
            if (stormInfo.getBasin() != null) {
                category = category + TreePanel.CATEGORY_DELIMITER + "Basin:"
                           + stormInfo.getBasin();
            }
            treePanel.addComponent(panelContents, category,
                                   stormInfo.toString(),
                                   stormDisplayState.getActive()
                                   ? ICON_ON
                                   : ICON_OFF);

            if (stormDisplayState.getActive()
                    && (firstSelectedComponent == null)) {
                firstSelectedComponent = panelContents;
            }
            if (firstComponent == null) {
                firstComponent = panelContents;
            }
        }

        //Show the first selected component or the first component
        if (firstSelectedComponent != null) {
            treePanel.show(firstSelectedComponent);
        } else if (firstComponent != null) {
            treePanel.show(firstComponent);
        }

        treePanel.setPreferredSize(new Dimension(500, 400));
        JComponent contents = treePanel;

        //        JComponent contents = GuiUtils.topCenter(GuiUtils.left(box),
        //                                  scroller);
        contents.setPreferredSize(new Dimension(500, 400));


        if ((startTime != null) && (endTime != null)) {
            try {

                Date[] range = DateUtil.getDateRange(startTime, endTime,
                                   new Date());
                double fromDate = range[0].getTime();
                double toDate   = range[1].getTime();
                for (StormInfo stormInfo : stormInfos) {
                    double date =
                        Util.makeDate(stormInfo.getStartTime()).getTime();
                    StormDisplayState stormDisplayState =
                        getStormDisplayState(stormInfo);
                    if ((date >= fromDate) && (date <= toDate)) {
                        stormDisplayState.loadStorm();
                    } else if (stormDisplayState.getActive()) {
                        stormDisplayState.deactivate();
                    }
                }
            } catch (java.text.ParseException pe) {
                logException("Error parsing start/end dates:" + startTime
                             + " " + endTime, pe);
            }
        }



        return contents;
    }

    /**
     * _more_
     *
     * @param stormDisplayState _more_
     */
    public void stormChanged(StormDisplayState stormDisplayState) {
        activeStorms = null;
        if(treePanel!=null)
            treePanel.setIcon(stormDisplayState.getContents(),
                              stormDisplayState.getActive()
                              ? ICON_ON
                              : ICON_OFF);
    }


    /**
     *  Set the StormDisplayStates property.
     *
     *  @param value The new value for StormDisplayStates
     */
    public void setStormDisplayStates(List<StormDisplayState> value) {
        if (value != null) {
            for (StormDisplayState stormDisplayState : value) {
                stormDisplayStateMap.put(stormDisplayState.getStormInfo(),
                                         stormDisplayState);
            }
        }
    }


    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            List<StormDisplayState> active = getActiveStorms();
            for (StormDisplayState stormDisplayState : active) {
                stormDisplayState.timeChanged(time);
            }
        } catch (Exception exc) {
            logException("changePosition", exc);
        }
        super.timeChanged(time);
    }




    /**
     *  Get the StormDisplayStates property.
     *
     *  @return The StormDisplayStates
     */
    public List<StormDisplayState> getStormDisplayStates() {
        List<StormDisplayState> stormDisplayStates =
            new ArrayList<StormDisplayState>();
        for (Enumeration keys = stormDisplayStateMap.keys();
                keys.hasMoreElements(); ) {
            StormInfo key = (StormInfo) keys.nextElement();
            StormDisplayState stormDisplayState =
                stormDisplayStateMap.get(key);
            //TODO: We don't want to add every state, just the ones that have been changed
            //            if(stormDisplayState.getChanged()) {
            if (stormDisplayState.getActive()) {
                stormDisplayStates.add(stormDisplayState);
            }
        }
        return stormDisplayStates;
    }


    /**
     * _more_
     *
     * @param el _more_
     * @param animationValue _more_
     * @param animationStep _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected List getCursorReadoutInner(EarthLocation el,
                                         Real animationValue,
                                         int animationStep)
            throws Exception {

        StormTrackPoint ob             = null;

        List            result         = new ArrayList();
        List            theStormStates = getStormDisplayStates();
        if (theStormStates != null) {
            Object[] pair = findClosestPoint(el,
                                             theStormStates,
                                             animationValue, 20);
            if(pair!=null) {
                StormTrack closestTrack = (StormTrack) pair[0];
                StormTrackPoint closestOb = (StormTrackPoint) pair[1];
                result.add("<tr><td>" + "Way: " + closestTrack.getWay() +"</td></tr> "
                           + formatStormTrackPoint(closestTrack, closestOb));

            }
        }

        return result;
    }


    /**
     * _more_
     *
     * @param stp _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected String formatStormTrackPoint(StormTrack stormTrack, StormTrackPoint stp)
            throws VisADException, RemoteException {
        Unit   displayUnit = getDisplayUnit();
        double value;
        if (stp == null) {
            return "";
        } 
        List<StormParam> params = stormTrack.getParams();
        //            result = "<tr><td>" + "Storm: "
        //                     + stp.toString() + "</td></tr>";
        String result =  "<tr><td>" + "Track Point Time:</td><td align=right>"
            + stp.getTrackPointTime() + "</td></tr>";
        for(StormParam param: params) {
            Real r = stp.getAttribute(param);
            if(r==null) continue;
            Unit unit = param.getUnit();
            result = result + "<tr><td>"
                + param.toString() + ":</td><td align=right>" + Misc.format(r.getValue()) +
                (unit!=null?("[" + unit +"]"):"")
                + "</td></tr>";
        }

        int length = result.length();
        return  StringUtil.padLeft(result, 5 * (20 - length), "&nbsp;");
    }



    /**
     * This finds the StormTrack and StormTrackPoint that is closest to the given location
     *
     * @return A 2-tuple. First element is the StormTrack. Second element is the ob.
     *      Or null if none found
     */
    protected Object[] findClosestPoint(EarthLocation el,
                                        List<StormDisplayState> theStates,
                                        Real animationValue, int distanceThresholdPixels)
        throws Exception {
        if ((el == null) || (theStates == null)) {
            return null;
        }

        int             numStates   = theStates.size();
        StormTrackPoint closestOb   = null;
        StormTrack closestTrack = null;

        int[]           clickPt     = boxToScreen(earthToBox(el));
        double          minDistance = distanceThresholdPixels;
        //        System.err.println ("click:" + clickPt[0]+"/"+clickPt[1] + " " +minDistance);

        for (int i = 0; i < numStates; i++) {
            StormDisplayState sds   = theStates.get(i);
            StormInfo         sinfo = sds.getStormInfo();
            HashMap<Way, List> wayToTracksMap =
                sds.getTrackCollection().getWayToTracksHashMap();
            // Way obsWay = new Way(Way.OBSERVATION);
            java.util.Set<Way> ways = wayToTracksMap.keySet();

            for(Way way: ways) {
                StormTrack track = null;
                if(way.equals(Way.OBSERVATION)) {
                  //  WayDisplayState       trackWDS   = wayToTracksMap.get(way); //get(Way.OBSERVATION);
                    List<StormTrack>      tracks   =  wayToTracksMap.get(way);
                    if(tracks.size()>0) {
                        track = tracks.get(0);
                    }
                } else {
                    WayDisplayState       trackWDS   = sds.getWayDisplayState(way); //get(Way.OBSERVATION);
                    boolean visible = checkTracksVisible( animationValue, trackWDS );
                    if(visible){
                        List<StormTrack>      tracks   = wayToTracksMap.get(way);
                        track = getClosestTimeForecastTrack(tracks, animationValue);
                    }
                }

                if(track == null) continue;
                //System.err.println(way + " track time is: " + track.getStartTime());
                List<StormTrackPoint> stpList  = track.getTrackPoints();
                int                   size     = stpList.size();
                for (int j = 0; j < size; j++) {
                    StormTrackPoint stp      = stpList.get(j);
                    EarthLocation   stpLoc   = stp.getTrackPointLocation();
                    int[]           obScreen = boxToScreen(earthToBox(stpLoc));
                    double distance = GuiUtils.distance(obScreen, clickPt);
                    if (distance < minDistance) {
                        closestOb   = stp;
                        minDistance = distance;
                        closestTrack = track;
                    }
                }
            }
            //            System.err.println ("\t" + obScreen[0]+"/"+obScreen[1] + " d:" + distance);

        }

        if (closestOb != null) {
            return new Object[]{closestTrack, closestOb};
        }

        return null;
    }




    private boolean checkTracksVisible( Real currentAnimationTime, WayDisplayState wds )throws Exception {
        if (currentAnimationTime == null || currentAnimationTime.isMissing()) {
            return false;
        }
       // Iterate way display states
        boolean             visible = false;
        if(wds.shouldShowTrack() && wds.hasTrackDisplay()) {
            FieldImpl field = (FieldImpl)wds.getTrackDisplay().getData();
            if(field==null)
                return false;
            Set timeSet = GridUtil.getTimeSet(field);
            if(timeSet == null)
                return false;
            if (timeSet.getLength() == 1) {
                return true;
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
                return visible = (index[0] >= 0);
            }

        }
       return  visible;
  }
    private StormTrack getClosestTimeForecastTrack(List<StormTrack> tracks, Real pTime) throws VisADException {

        DateTime dt = new DateTime(pTime); // pTime.
        double timeToLookFor = dt.getValue();
        int    numPoints     = tracks.size();
        double lastTime      = -1;

       //  for(StormTrack track: tracks){
       //      if(track.getTrackStartTime().equals(dt))
       //         return track;
       //  }
        for (int i = 0; i < numPoints; i++) {
            StormTrack st         = tracks.get(i);
            double          currentTime = st.getStartTime().getValue();
            if (timeToLookFor == currentTime) {
                return st;
            }
            if (timeToLookFor < currentTime) {
                if (i == 0) {
                    return null;
                }
                if (timeToLookFor > lastTime) {
                    return tracks.get(i - 1);
                }
            }
            lastTime = currentTime;
        }
        return null;
    }


    /**
     * Set the OkWays property.
     *
     * @param value The new value for OkWays
     */
    public void setOkWays(Hashtable<String, Boolean> value) {
        okWays = value;
    }

    /**
     * Get the OkWays property.
     *
     * @return The OkWays
     */
    public Hashtable<String, Boolean> getOkWays() {
        return okWays;
    }


    /**
     * Set the OkParams property.
     *
     * @param value The new value for OkParams
     */
    public void setOkParams(Hashtable<String, Boolean> value) {
        okParams = value;
    }

    /**
     * Get the OkParams property.
     *
     * @return The OkParams
     */
    public Hashtable<String, Boolean> getOkParams() {
        return okParams;
    }





    /**
     * Set the StartTime property.
     *
     * @param value The new value for StartTime
     */
    public void setStartTime(String value) {
        startTime = value;
    }

    /**
     * Get the StartTime property.
     *
     * @return The StartTime
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * Set the EndTime property.
     *
     * @param value The new value for EndTime
     */
    public void setEndTime(String value) {
        endTime = value;
    }

    /**
     * Get the EndTime property.
     *
     * @return The EndTime
     */
    public String getEndTime() {
        return endTime;
    }




/**
Set the YearList property.

@param value The new value for YearList
**/
public void setYearList (List<Integer> value) {
	yearList = value;
}

/**
Get the YearList property.

@return The YearList
**/
public List<Integer> getYearList () {
	return yearList;
}


/**
Set the LocalStormDisplayState property.

@param value The new value for LocalStormDisplayState
**/
public void setLocalStormDisplayState (StormDisplayState value) {
	localStormDisplayState = value;
}

/**
Get the LocalStormDisplayState property.

@return The LocalStormDisplayState
**/
public StormDisplayState getLocalStormDisplayState () {
	return localStormDisplayState;
}



}

