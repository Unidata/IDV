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


package ucar.unidata.idv.control;

import visad.georef.EarthLocation;

import ucar.unidata.collab.Sharable;
import ucar.unidata.collab.SharableImpl;

import ucar.unidata.data.DataUtil;

import ucar.unidata.data.storm.*;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;




import ucar.unidata.idv.ControlContext;
import ucar.unidata.ui.drawing.*;


import ucar.unidata.ui.symbol.*;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;


import ucar.visad.display.*;

import ucar.visad.Util;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.DisplayableDataRef;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.SelectRangeDisplayable;
import ucar.visad.display.SelectorPoint;
import ucar.visad.display.StationModelDisplayable;
import ucar.visad.display.TrackDisplayable;

import ucar.unidata.util.ColorTable;



import visad.*;

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
import javax.swing.table.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;



/**
 * A MetApps Display Control with Displayable and controls for
 * displaying a track (balloon sounding or aircraft track)
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormTrackControl extends DisplayControlImpl {


    private StormDataSource stormDataSource;

    private static final Data DUMMY_DATA = new Real(0);



    private StormInfo stormInfo;


    private TrackDisplayable obsTrackDisplay;

    private CompositeDisplayable trackHolder;

    private  StormTrackCollection  trackCollection;

    private List<StormTrack> tracks;

    private  JTable trackTable;

    private AbstractTableModel trackModel;

    /**
     * Create a new Track Control; set the attribute flags
     */
    public StormTrackControl() {
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


        List dataSources = new ArrayList();
        dataChoice.getDataSources(dataSources);

        if(dataSources.size()!=1) {
            userMessage("Could not find Storm Data Source");
            return false;
        }


        if(!(dataSources.get(0) instanceof StormDataSource)) {
            userMessage("Could not find Storm Data Source");
            return false;
        }

        stormDataSource = (StormDataSource) dataSources.get(0);



        obsTrackDisplay = new TrackDisplayable("track" + dataChoice);
        addDisplayable(obsTrackDisplay, getAttributeFlags());

        trackHolder = new CompositeDisplayable();
        addDisplayable(trackHolder);

        return true;
    }



    public void initDone() {
        super.initDone();
        try {
            handleNewStormInfo(stormInfo);
        }catch(Exception exc) {
            logException("Setting new storm info", exc);
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

        List<StormInfo> stormInfos = stormDataSource.getStormInfos();
        List items = new ArrayList();
        items.add("Select Storm to View");
        TwoFacedObject selected = null;
        for(StormInfo stormInfo: stormInfos) {
            TwoFacedObject tfo = new TwoFacedObject(stormInfo.getStormId() + " " + stormInfo.getStartTime(), 
                                                    stormInfo);

            items.add(tfo);
            if(this.stormInfo!=null && this.stormInfo.getStormId().equals(stormInfo.getStormId())) {
                selected = tfo;
            }
        }

        final JComboBox box = new JComboBox();
        GuiUtils.setListData(box, items);
        if(selected!=null) {
            box.setSelectedItem(selected);
        }

        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                    try {
                if(box.getSelectedIndex()==0) {
                    handleNewStormInfo(null);
                }  else {
                    TwoFacedObject tfo = (TwoFacedObject) box.getSelectedItem();
                        handleNewStormInfo((StormInfo)tfo.getId());
                }
                    }catch(Exception exc) {
                        logException("Setting new storm info", exc);
                    }


            }
        });

        trackModel = new AbstractTableModel() {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public int getRowCount() {
                if(tracks==null) return 0;
                return tracks.size();
            }

            public int getColumnCount() {
                return 2;
            }

            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {}

            public Object getValueAt(int row, int column) {
                if(tracks == null || row>= tracks.size()) return "";
                StormTrack track = tracks.get(row);
                if(column==0) return track.getWay();
                return track.getTrackStartTime();                
            }

            public String getColumnName(int column) {
                if(column==0) return "Way";
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



        JComponent contents = GuiUtils.topCenter(GuiUtils.left(box), scroller);             
        return GuiUtils.top(GuiUtils.inset(contents,5));
    }


    private void handleNewStormInfo(StormInfo newStormInfo) throws Exception {
        stormInfo = newStormInfo;
        //        trackHolder.clearDisplayables();
        if(stormInfo == null) {
            trackCollection = null;
            tracks = null;
            obsTrackDisplay.setData(DUMMY_DATA);
            trackModel.fireTableStructureChanged();
            return;
        }


        trackModel.fireTableStructureChanged();



        trackCollection = stormDataSource.getTrackCollection(stormInfo);
        tracks = trackCollection.getTracks();


        StormTrack obsTrack = trackCollection.getObsTrack();
        //        obsTrackDisplay.setTrack(makeField(obsTrack));

        ColorTable ct =
            getControlContext().getColorTableManager().getColorTable("Red");

        float[][]colors =  getColorTableForDisplayable(ct);

        Way way = new Way("babj");
        for(StormTrack track: tracks) {
            if(track.isObserved()) {
                continue;
            }
            if(!track.getWay().equals(way)) {
                continue;
            }
            TrackDisplayable trackDisplay = new TrackDisplayable("track " + track.getTrackId());
            System.err.println ("got one:" + trackDisplay);
            trackHolder.addDisplayable(trackDisplay);
            trackDisplay.setTrack(makeField(track));
            //            addDisplayable(trackDisplay);
            //            obsTrackDisplay.setTrack(makeField(track));
            //            trackDisplay.setColorPalette(colors);
            break;
        }
        trackHolder.clearDisplayables();
    }


    private FieldImpl makeField(StormTrack track) throws Exception {

        List times = track.getTrackTimes();
        List<EarthLocation> locs = track.getTrackPoints();


        Unit timeUnit = CommonUnit.secondsSinceTheEpoch;

        RealType dfltRealType = RealType.getRealType("Default");
        Real dfltReal = new Real(dfltRealType, 1);

        RealType timeType =
            RealType.getRealType(DataUtil.cleanName("track_time" + "_"
                + timeUnit), timeUnit);
        RealTupleType rangeType = new RealTupleType(
                                      ucar.visad.Util.getRealType(
                                                                  dfltReal.getUnit()), timeType);
        double[][] newRangeVals = new double[2][times.size()];
        int        numObs       = times.size();
        float[]lats =new float[numObs];
        float[]lons =new float[numObs];
        System.err.println("points:" + times + "\n" + locs);
        for (int i = 0; i < numObs; i++) {
            Date dateTime = (Date) times.get(i);
            Real     value    = dfltReal;
            EarthLocation el = locs.get(i);
            newRangeVals[0][i] = value.getValue();
            newRangeVals[1][i] = dateTime.getTime()/1000;
            lats[i] = (float) el.getLatitude().getValue();
            lons[i] = (float) el.getLongitude().getValue();
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
                                            new Unit[] {
                                                dfltReal.getUnit(),
                timeUnit });
        timeTrack.setSamples(newRangeVals, false);

        return timeTrack;
    }

    /**
       Set the StormInfo property.

       @param value The new value for StormInfo
    **/
    public void setStormInfo (StormInfo value) {
	stormInfo = value;
    }

    /**
       Get the StormInfo property.

       @return The StormInfo
    **/
    public StormInfo getStormInfo () {
	return stormInfo;
    }





}

