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
import java.util.Enumeration;
import java.util.GregorianCalendar;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.Util;


import ucar.visad.display.*;
import ucar.visad.display.Animation;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.DisplayableDataRef;
import ucar.visad.display.LineDrawing;
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
 * A MetApps Display Control with Displayable and controls for
 * displaying a track (balloon sounding or aircraft track)
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormTrackControl extends DisplayControlImpl {


    private Object MUTEX = new Object();

    /** _more_ */
    private StormDataSource stormDataSource;




    private Hashtable<StormInfo, StormDisplayState> stormDisplayStateMap  
        = new Hashtable<StormInfo, StormDisplayState>();




    /** _more_ */
    private List<Displayable> trackDisplays = new ArrayList<Displayable>();


    private TreePanel treePanel;



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




        return true;
    }


    public StormDataSource getStormDataSource() {
        return stormDataSource;
    }


    private StormDisplayState getStormDisplayState(StormInfo stormInfo) {
        StormDisplayState stormDisplayState = stormDisplayStateMap.get(stormInfo);
        if(stormDisplayState == null) {
            stormDisplayState = new StormDisplayState(stormInfo);
            stormDisplayStateMap.put(stormInfo,stormDisplayState);
        }
        return stormDisplayState;
    }


    /**
     * _more_
     */
    public void initDone() {
        super.initDone();
        try {
            for (Enumeration keys =stormDisplayStateMap.keys(); keys.hasMoreElements(); ) {
                StormInfo key  = (StormInfo)keys.nextElement();
                StormDisplayState stormDisplayState = stormDisplayStateMap.get(key);
                if(stormDisplayState.getVisible()) {
                    showStorm(stormDisplayState);
                }
            }
        } catch (Exception exc) {
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

        treePanel = new  TreePanel(true,100);

        List<StormInfo> stormInfos = stormDataSource.getStormInfos();
        List            items      = new ArrayList();
        items.add("Select Storm to View");
        TwoFacedObject selected = null;
        //TODO: Sort the years so we  get the most recent year first
        GregorianCalendar cal =
            new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        final ImageIcon offIcon = GuiUtils.getImageIcon("/ucar/unidata/idv/control/images/Refresh16.gif");
        final ImageIcon onIcon = GuiUtils.getImageIcon("/ucar/unidata/idv/control/images/Back16.gif");
        for (StormInfo stormInfo : stormInfos) {
            cal.setTime(stormInfo.getStartTime());
            int year = cal.get(Calendar.YEAR);
            final StormDisplayState stormDisplayState = getStormDisplayState(stormInfo);
            final JCheckBox showCbx  =new JCheckBox("Show",stormDisplayState.getVisible());
            final StormInfo theStormInfo = stormInfo;
            final JComponent stormComp = new JPanel(new BorderLayout());
            showCbx.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        stormDisplayState.setVisible(showCbx.isSelected());
                        showStorm(stormDisplayState);
                        if(stormDisplayState.getVisible()) {
                            treePanel.setIcon(stormComp, onIcon);
                        } else {
                            treePanel.setIcon(stormComp, offIcon);
                        }
                   }
                });

            if(showCbx.isSelected()) {
                treePanel.setIcon(stormComp, onIcon);
            } else {
                treePanel.setIcon(stormComp, offIcon);
            }

            stormComp.add(BorderLayout.CENTER, GuiUtils.topLeft(showCbx));
            treePanel.addComponent(stormComp, ""+year,
                                   stormInfo.getStormId(),offIcon);
        }


        treePanel.setPreferredSize(new Dimension(300,400));
        JComponent contents = treePanel;

        //        JComponent contents = GuiUtils.topCenter(GuiUtils.left(box),
        //                                  scroller);
        return contents;
    }


    private void showStorm(final StormDisplayState stormDisplayState) {
        Misc.run(new Runnable() {
            public void run() {
                try {
                    synchronized(MUTEX) {
                        showStormInner(stormDisplayState);
                    }
                } catch (Exception exc) {
                    logException("Showing storm", exc);
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
    private void showStormInner(StormDisplayState stormDisplayState)
            throws Exception {
        stormDisplayState.showStorm(this);
    }




    /**
       Set the StormDisplayStates property.

       @param value The new value for StormDisplayStates
    **/
    public void setStormDisplayStates (List<StormDisplayState> value) {
        if(value!=null) {
            for(StormDisplayState stormDisplayState: value) {
                stormDisplayStateMap.put(stormDisplayState.getStormInfo(), stormDisplayState);
            }
        }
    }


    /**
       Get the StormDisplayStates property.

       @return The StormDisplayStates
    **/
    public List<StormDisplayState> getStormDisplayStates () {
        List<StormDisplayState> stormDisplayStates 
            = new ArrayList<StormDisplayState>();
        for (Enumeration keys =stormDisplayStateMap.keys(); keys.hasMoreElements(); ) {
            StormInfo key  = (StormInfo)keys.nextElement();
            StormDisplayState stormDisplayState = stormDisplayStateMap.get(key);
            //TODO: We don't want to add every state, just the ones that have been changed
            //            if(stormDisplayState.getChanged()) {
            if(stormDisplayState.getVisible()) {
                stormDisplayStates.add(stormDisplayState);
            }
        }
	return stormDisplayStates;
    }



}

