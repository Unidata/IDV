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
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataUtil;
import ucar.unidata.data.storm.*;

import ucar.unidata.idv.ControlContext;

import ucar.unidata.ui.TreePanel;


import java.util.Calendar;
import java.util.GregorianCalendar;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.ColorTable;
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
import java.util.Date;
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

    private Object MUTEX = new Object();

    /** _more_ */
    private static final Data DUMMY_DATA = new Real(0);


    /** _more_ */
    private CompositeDisplayable trackHolder;


    private StormInfo stormInfo;

    private boolean visible = false;
    private boolean changed = false;

    /** _more_ */
    private StormTrackCollection trackCollection;

    /** _more_ */
    private List<StormTrack> tracks;

    /** _more_ */
    private JTable trackTable;

    /** _more_ */
    private AbstractTableModel trackModel;

    private StormTrackControl stormTrackControl;

    /** _more_ */
    private TrackDisplayable obsTrackDisplay;

    private JComponent contents;

    private JComponent centerContents;

    public StormDisplayState() {}

    public StormDisplayState(StormInfo stormInfo) {
        this.stormInfo = stormInfo;
    }

    public JComponent getContents() {
        if(contents == null) {
            contents = doMakeContents();
        }
        return contents;
    }

    private JComponent doMakeContents() {
        final JCheckBox showCbx  =new JCheckBox("Show",getVisible());
        JLabel label = GuiUtils.cLabel(""+ stormInfo);
        showCbx.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    setVisible(showCbx.isSelected());
                    showStorm();
                }
            });

        JComponent top  =GuiUtils.leftCenter(showCbx, label);
        centerContents = new JPanel(new BorderLayout());
        JComponent contents = GuiUtils.topCenter(top, centerContents);
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


    private void initCenterContents() {
        centerContents.add(BorderLayout.CENTER, new JLabel("initialized"));
        centerContents.repaint();
    }

    protected void setStormTrackControl (StormTrackControl stormTrackControl) {
        this.stormTrackControl = stormTrackControl;
    }

    protected void showStorm() {
        Misc.run(new Runnable() {
            public void run() {
                try {
                    synchronized(MUTEX) {
                        stormTrackControl.stormChanged(StormDisplayState.this);
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
     * @param newStormInfo _more_
     *
     * @throws Exception _more_
     */
    private void showStormInner()
            throws Exception {
        if(trackCollection==null) {
            trackCollection = stormTrackControl.getStormDataSource().getTrackCollection(stormInfo);
            tracks          = trackCollection.getTracks();
            initCenterContents();
        }

        if(trackHolder==null) {
            trackHolder = new CompositeDisplayable();
            stormTrackControl.addDisplayable(trackHolder);
        }

        trackHolder.clearDisplayables();
        if(obsTrackDisplay==null) {
            obsTrackDisplay = new TrackDisplayable("track_" + stormInfo.getStormId());
            obsTrackDisplay.setLineWidth(3);
            stormTrackControl.addDisplayable(obsTrackDisplay);
        }

        if (!getVisible()) {
            obsTrackDisplay.setData(DUMMY_DATA);
            if(trackModel!=null)
                trackModel.fireTableStructureChanged();
            return;
        }



        if(trackModel!=null)
            trackModel.fireTableStructureChanged();


        StormTrack obsTrack = trackCollection.getObsTrack();
        obsTrackDisplay.setTrack(makeField(obsTrack, false));


        ColorTable ct =
            stormTrackControl.getControlContext().getColorTableManager().getColorTable("Red");

        float[][] colors = stormTrackControl.getColorTableForDisplayable(ct);

        Way       way    = new Way("babj");
        for (StormTrack track : tracks) {
            if (track.isObserved() || !track.getWay().equals(way)) {
                continue;
            }
            TrackDisplayable trackDisplay = new TrackDisplayable("track "
                                                + track.getTrackId());

            trackDisplay.setColorPalette(colors);
            trackHolder.addDisplayable(trackDisplay);
            trackDisplay.setTrack(makeField(track, true));
        }
    }


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
        List<EarthLocation> locs     = track.getTrackPoints();


        Unit                timeUnit = CommonUnit.secondsSinceTheEpoch;

        RealType dfltRealType = RealType.getRealType("Default_" + (cnt++));
        Real                dfltReal = new Real(dfltRealType, 1);

        RealType timeType =
            RealType.getRealType(DataUtil.cleanName("track_time" + cnt + "_"
                + timeUnit), timeUnit);
        RealTupleType rangeType =
            new RealTupleType(RealType.getRealType("trackrange_" + cnt,
                dfltReal.getUnit()), timeType);
        double[][] newRangeVals = new double[2][times.size()];
        int        numObs       = times.size();
        float[]    lats         = new float[numObs];
        float[]    lons         = new float[numObs];
        //        System.err.println("points:" + times + "\n" + locs);
        for (int i = 0; i < numObs; i++) {
            Date          dateTime = (Date) times.get(i);
            Real          value    = (fixedValue
                                      ? dfltReal
                                      : new Real(dfltRealType, i));
            EarthLocation el       = locs.get(i);
            newRangeVals[0][i] = value.getValue();
            newRangeVals[1][i] = dateTime.getTime() / 1000;
            lats[i]            = (float) el.getLatitude().getValue();
            lons[i]            = (float) el.getLongitude().getValue();
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
       Set the Visible property.

       @param value The new value for Visible
    **/
    public void setVisible (boolean value) {
	visible = value;
    }

    /**
       Get the Visible property.

       @return The Visible
    **/
    public boolean getVisible () {
	return visible;
    }

    /**
       Set the Changed property.

       @param value The new value for Changed
    **/
    public void setChanged (boolean value) {
	changed = value;
    }

    /**
       Get the Changed property.

       @return The Changed
    **/
    public boolean getChanged () {
	return changed;
    }



}

