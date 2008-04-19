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


import  ucar.unidata.ui.colortable.ColorTableDefaults;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.storm.*;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.ui.TreePanel;
import ucar.unidata.util.ColorTable;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;


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

import java.rmi.RemoteException;

import java.util.ArrayList;


import java.util.Arrays;


import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.*;



/**
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormDisplayState {

    /** _more_          */
    private Object MUTEX = new Object();

    /** _more_ */
    private static final Data DUMMY_DATA = new Real(0);


    /** _more_ */
    private CompositeDisplayable forecastHolder;


    /** _more_          */
    private StormInfo stormInfo;

    /** _more_          */
    private boolean obsVisible = true;


    /** _more_          */
    private boolean forecastVisible = false;

    /** _more_          */
    private boolean changed = false;


    /** _more_          */
    private boolean active = false;


    /** _more_ */
    private StormTrackCollection trackCollection;

    /** _more_ */
    private List<StormTrack> tracks;

    /** _more_ */
    private JTable trackTable;

    /** _more_ */
    private AbstractTableModel trackModel;

    /** _more_          */
    private StormTrackControl stormTrackControl;

    /** _more_ */
    private TrackDisplayable obsTrackDisplay;


    /** time holder */
    private DisplayableData timesHolder = null;

    /** _more_          */
    private JComponent contents;


    
    private Hashtable<Way,WayDisplayState> wayDisplayStateMap = new Hashtable<Way,WayDisplayState>();



    /**
     * _more_
     */
    public StormDisplayState() {}

    /**
     * _more_
     *
     * @param stormInfo _more_
     */
    public StormDisplayState(StormInfo stormInfo) {
        this.stormInfo = stormInfo;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent getContents() {
        if (contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }

    private List<WayDisplayState> getWayDisplayStates() {
        return  (List<WayDisplayState>)Misc.toList(wayDisplayStateMap.elements());
    }

    private WayDisplayState getWayDisplayState(Way way) {
        WayDisplayState wayState = wayDisplayStateMap.get(way);
        if(wayState == null) {
            wayDisplayStateMap.put(way,wayState = new WayDisplayState(way));
        }
        if(wayState.getColor() == null) {
            wayState.setColor(DisplayConventions.getColor());
        }
        return wayState;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private JComponent doMakeContents() {
        JButton loadBtn = new JButton("Load Tracks:");
        JLabel  label   = GuiUtils.cLabel("  " + stormInfo);
        loadBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                active = true;
                showStorm();
            }
        });

        JComponent top      = GuiUtils.hbox(loadBtn, label);
        JComponent contents = GuiUtils.top(GuiUtils.inset(top, 5));
        /*
new BorderLayout()) {
                boolean firstTime = true;
                public void paint(Graphics g) {
                    if(firstTime) {
                        firstTime = false;
                        initCenterContents();
                        repaint();
                        return;
                    }
                    super.paint(g);
                }
                };*/

        return contents;
    }


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
     */
    private void initCenterContents() {
        contents.removeAll();
        final JCheckBox showObsCbx = new JCheckBox("Show Observation Track",
                                         getObsVisible());
        JLabel label = GuiUtils.lLabel("<html><h3>Storm: " + stormInfo
                                       + "</h3></html>");
        showObsCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setObsVisible(showObsCbx.isSelected());
                showStorm();
            }
        });

        final JCheckBox showForecastCbx =
            new JCheckBox("Show Forecast Tracks", getForecastVisible());
        showForecastCbx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                setForecastVisible(showForecastCbx.isSelected());
                showStorm();
            }
        });

        JComponent top     = GuiUtils.vbox(label, showObsCbx,
                                           showForecastCbx);

        List<Way>  ways    = trackCollection.getWayList();
        List       wayCbxs = new ArrayList();
        wayCbxs.add(new JLabel("Ways: "));
        for (Way way : ways) {
            if (way.isObservation()) {
                continue;
            }
            final WayDisplayState wayDisplayState = getWayDisplayState(way);
            final JCheckBox wayCbx = new JCheckBox(way.toString(), wayDisplayState.getVisible());
            wayCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    try {
                        wayDisplayState.setVisible(wayCbx.isSelected());
                    } catch(Exception exc) {
                        stormTrackControl.logException("Toggling way visibility", exc);
                    }
                }
            });
            JComponent swatch  =GuiUtils.filler(10,10);
            swatch.setBackground(wayDisplayState.getColor());
            wayCbxs.add(GuiUtils.left(GuiUtils.hbox(GuiUtils.wrap(swatch),wayCbx)));
        }
        JComponent wayComp = GuiUtils.vbox(wayCbxs);
        JComponent inner = GuiUtils.topCenter(top,
                               GuiUtils.inset(GuiUtils.top(wayComp), 5));

        contents.add(BorderLayout.CENTER, GuiUtils.inset(inner, 5));
        contents.repaint();
    }

    /**
     * _more_
     *
     * @param way _more_
     *
     * @return _more_
     */
    protected boolean canShowWay(Way way) {
        return getWayDisplayState(way).getVisible();
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
                try {
                    synchronized (MUTEX) {
                        stormTrackControl.stormChanged(
                            StormDisplayState.this);
                        showStormInner();
                    }
                } catch (Exception exc) {
                    stormTrackControl.logException("Showing storm", exc);
                }

            }
        });
    }


    /**
     * _more_
     *
     *
     * @throws Exception _more_
     */
    private void showStormInner() throws Exception {
        if (trackCollection == null) {
            contents.removeAll();
            contents.add(
                GuiUtils.top(
                    GuiUtils.inset(new JLabel("Loading Tracks..."), 5)));
            contents.repaint();
            trackCollection =
                stormTrackControl.getStormDataSource().getTrackCollection(
                    stormInfo);
            tracks = trackCollection.getTracks();
            initCenterContents();
        }


        //Make the displayables if needed
        if (obsTrackDisplay == null) {
            obsTrackDisplay = new TrackDisplayable("track_"
                    + stormInfo.getStormId());
            obsTrackDisplay.setLineWidth(3);
            stormTrackControl.addDisplayable(obsTrackDisplay);
            StormTrack obsTrack = trackCollection.getObsTrack();
            obsTrackDisplay.setTrack(makeField(obsTrack, false));
            timesHolder = new LineDrawing("track_time" +  stormInfo.getStormId());
            timesHolder.setManipulable(false);
            timesHolder.setVisible(false);
            List times    = obsTrack.getTrackTimes();
            timesHolder.setData(ucar.visad.Util.makeTimeSet(times));
            stormTrackControl.addDisplayable(timesHolder);
        }


        obsTrackDisplay.setVisible(getObsVisible());


        if ( !getForecastVisible()) {
            if (forecastHolder != null) {
                forecastHolder.setVisible(false);
            }
            return;
        }

        if (forecastHolder == null) {
            forecastHolder = new CompositeDisplayable();
            stormTrackControl.addDisplayable(forecastHolder);
            //            ColorTable ct =
            //                stormTrackControl.getControlContext().getColorTableManager()
            //                .getColorTable("Red");




            forecastHolder.setVisible(true);
            for (StormTrack track : tracks) {
                if (track.isObservation()) {
                    continue;
                }
                //                if (!(track.getWay().getId().equals("SHTM") ||
                //                      track.getWay().getId().equals("jawt"))) 
                //continue;
                WayDisplayState wayDisplayState = getWayDisplayState(track.getWay());
                FieldImpl field = makeField(track, true);
                wayDisplayState.addTrack(track,  field);

                //                TrackDisplayable trackDisplay = new TrackDisplayable("track ");
                //                trackDisplay.setTrack(field);
                //                forecastHolder.addDisplayable(trackDisplay);
            }

            List<WayDisplayState> wayDisplayStates  = getWayDisplayStates();
            for(WayDisplayState wayDisplayState: wayDisplayStates) {
                List fields = wayDisplayState.getFields();
                //                if (!(wayDisplayState.getWay().getId().equals("SHTM"))) continue;
                //                System.err.println (wayDisplayState.getWay() +" fields=" +fields.size());
                if(fields.size() == 0) continue;
                TrackDisplayable trackDisplay = new TrackDisplayable("track ");
                trackDisplay.setColorPalette(getColor(wayDisplayState.getColor()));
                trackDisplay.setUseTimesInAnimation(false);
                wayDisplayState.addDisplayable(trackDisplay);
                if(!wayDisplayState.getVisible()) {
                    trackDisplay.setVisible(false);
                }
                forecastHolder.addDisplayable(trackDisplay);
                FieldImpl timeField = ucar.visad.Util.makeTimeField(fields,wayDisplayState.getTimes());
                trackDisplay.setTrack(timeField);
            }
        }

    }


        //        ucar.visad.Util.makeTimeField(List<Data> ranges, List times)


    private float[][]getColor(Color c) {
        if(c==null) c = Color.red;
        return ColorTableDefaults.allOneColor(c,true);
    }


    /** _more_          */
    int cnt = 0;

    /**
     * _more_
     *
     * @param track _more_
     * @param fixedValue _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private FieldImpl makeField(StormTrack track, boolean fixedValue)
            throws Exception {

        List                times    = track.getTrackTimes();
        List<StormTrackPoint> locs     = track.getTrackPoints();
        int        numPoints       = times.size();
        //        System.err.println ("times:" + times);
        //        System.err.println ("locs:" + locs);

        Unit                timeUnit = ((DateTime)times.get(0)).getUnit();

        RealType dfltRealType = RealType.getRealType("Default_" + (cnt++));
        Real                dfltReal = new Real(dfltRealType, 1);

        RealType timeType =
            RealType.getRealType(DataUtil.cleanName("track_time" + cnt + "_"
                + timeUnit), timeUnit);
        RealTupleType rangeType =
            new RealTupleType(RealType.getRealType("trackrange_" + cnt,
                dfltReal.getUnit()), timeType);
        double[][] newRangeVals = new double[2][numPoints];
        float[]    lats         = new float[numPoints];
        float[]    lons         = new float[numPoints];
        //        System.err.println("points:" + times + "\n" + locs);
        for (int i = 0; i < numPoints; i++) {
            DateTime      dateTime = (DateTime) times.get(i);
            Real          value    = (fixedValue
                                      ? dfltReal
                                      : new Real(dfltRealType, i));
            EarthLocation el       = locs.get(i).getTrackPointLocation();
            newRangeVals[0][i] = value.getValue();
            newRangeVals[1][i] = dateTime.getValue();
            lats[i]            = (float) el.getLatitude().getValue();
            lons[i]            = (float) el.getLongitude().getValue();
            //            if(Math.abs(lats[i])>90) System.err.println("bad lat:" + lats[i]);
        }
        GriddedSet llaSet = ucar.visad.Util.makeEarthDomainSet(lats, lons,
                                null);
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
     */
    public void doit() {
        trackModel = new AbstractTableModel() {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public int getRowCount() {
                if (tracks == null) {
                    return 0;
                }
                return tracks.size();
            }

            public int getColumnCount() {
                return 2;
            }

            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {}

            public Object getValueAt(int row, int column) {
                if ((tracks == null) || (row >= tracks.size())) {
                    return "";
                }
                StormTrack track = tracks.get(row);
                if (column == 0) {
                    return track.getWay();
                }
                return track.getTrackStartTime();
            }

            public String getColumnName(int column) {
                if (column == 0) {
                    return "Way";
                }
                return "Date";
            }
        };


        trackTable = new JTable(trackModel);

        int width  = 300;
        int height = 400;
        JScrollPane scroller = GuiUtils.makeScrollPane(trackTable, width,
                                   height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));

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
     *  Set the ObsVisible property.
     *
     *  @param value The new value for ObsVisible
     */
    public void setObsVisible(boolean value) {
        obsVisible = value;
    }

    /**
     *  Get the ObsVisible property.
     *
     *  @return The ObsVisible
     */
    public boolean getObsVisible() {
        return obsVisible;
    }



    /**
     *  Set the ForecastVisible property.
     *
     *  @param value The new value for ForecastVisible
     */
    public void setForecastVisible(boolean value) {
        forecastVisible = value;
    }

    /**
     *  Get the ObsVisible property.
     *
     *  @return The ForecastVisible
     */
    public boolean getForecastVisible() {
        return forecastVisible;
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
       Set the WayDisplayStateMap property.

       @param value The new value for WayDisplayStateMap
    **/
    public void setWayDisplayStateMap (Hashtable<Way,WayDisplayState> value) {
	wayDisplayStateMap = value;
    }

    /**
       Get the WayDisplayStateMap property.

       @return The WayDisplayStateMap
    **/
    public Hashtable<Way,WayDisplayState> getWayDisplayStateMap () {
	return wayDisplayStateMap;
    }




}

