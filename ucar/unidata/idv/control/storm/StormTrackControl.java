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
import ucar.unidata.data.storm.*;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.TreePanel;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.visad.display.CompositeDisplayable;
import visad.Real;
import visad.Unit;
import visad.VisADException;
import visad.georef.EarthLocation;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;


/**
 * A MetApps Display Control with Displayable and controls for
 * displaying a track (balloon sounding or aircraft track)
 *
 * @author Unidata Development Team
 * @version $Revision: 1.69 $
 */

public class StormTrackControl extends DisplayControlImpl {


    /** _more_ */
    final ImageIcon ICON_ON =
        GuiUtils.getImageIcon("/ucar/unidata/idv/control/storm/dot.gif");

    /** _more_ */
    final ImageIcon ICON_OFF =
        GuiUtils.getImageIcon("/ucar/unidata/idv/control/storm/blank.gif");



    /** _more_ */
    private CompositeDisplayable placeHolder;

    /** _more_ */
    private StormDataSource stormDataSource;


    /** Holds the EarthLocation of the last point clicked */
    private EarthLocation lastEarthLocation = null;


    /** _more_ */
    private Hashtable<StormInfo, StormDisplayState> stormDisplayStateMap =
        new Hashtable<StormInfo, StormDisplayState>();


    /** _more_ */
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
    public boolean init(DataChoice dataChoice) throws VisADException,
            RemoteException {

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

        return true;
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
     * @param stormInfo _more_
     *
     * @return _more_
     */
    private StormDisplayState getStormDisplayState(StormInfo stormInfo) {
        StormDisplayState stormDisplayState =
            stormDisplayStateMap.get(stormInfo);
        if (stormDisplayState == null) {
            stormDisplayState = new StormDisplayState(stormInfo);
            stormDisplayState.setStormTrackControl(this);
            stormDisplayStateMap.put(stormInfo, stormDisplayState);
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
    }

    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On Badness
     * @throws VisADException On Badness
     */
    protected Container doMakeContents() throws VisADException,
            RemoteException {

        treePanel = new TreePanel(true, 100);

        //Get the storm infos and sort them
        List<StormInfo> stormInfos = (List<StormInfo>)Misc.sort(stormDataSource.getStormInfos());
        GregorianCalendar cal = new GregorianCalendar(DateUtil.TIMEZONE_GMT);
        Hashtable years = new Hashtable();
        JComponent firstComponent = null;
        JComponent firstSelectedComponent = null;
        //Go in reverse order so we get the latest first
        for (int i=stormInfos.size()-1;i>=0;i--) {
            StormInfo stormInfo = stormInfos.get(i);
            cal.setTime(ucar.visad.Util.makeDate(stormInfo.getStartTime()));
            int year = cal.get(Calendar.YEAR);
            StormDisplayState stormDisplayState =
                getStormDisplayState(stormInfo);

            String category = ""+year;
            if(years.get(category)==null) {
                years.put(category, category);
                JComponent categoryComponent = new JLabel("Allow user to view all observed tracks for a given year");
                treePanel.addCategoryComponent(category, categoryComponent);
            }
            JComponent panelContents = stormDisplayState.getContents();
            if(stormInfo.getBasin()!=null) {
                category = category +TreePanel.CATEGORY_DELIMITER + "Basin:" + stormInfo.getBasin();
            }
            treePanel.addComponent(panelContents,
                                   category, stormInfo.toString(),
                                   stormDisplayState.getActive()
                                   ? ICON_ON
                                   : ICON_OFF);

            if(stormDisplayState.getActive() && firstSelectedComponent == null) {
                firstSelectedComponent = panelContents;
            }
            if(firstComponent==null) {
                firstComponent = panelContents;
            }
        }

        //Show the first selected component or the first component
        if(firstSelectedComponent!=null) {
            treePanel.show(firstSelectedComponent);
        } else if(firstComponent!=null) {
            treePanel.show(firstComponent);
        }

        treePanel.setPreferredSize(new Dimension(300, 400));
        JComponent contents = treePanel;

        //        JComponent contents = GuiUtils.topCenter(GuiUtils.left(box),
        //                                  scroller);
        return contents;
    }

    /**
     * _more_
     *
     * @param stormDisplayState _more_
     */
    public void stormChanged(StormDisplayState stormDisplayState) {
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
                                         int animationStep) throws Exception {

        StormTrackPoint ob             = null;

        List            result         = new ArrayList();
        List            theStormStates = getStormDisplayStates();
        if (theStormStates != null) {
            ob = findClosestOb(el, theStormStates);
        }

        // System.err.println("R = "+ r);

        if (ob != null) {
            result.add("<tr><td>" + getMenuLabel() + ":</td></tr> "
                       + formatStormTrackPoint(ob));
            // +  "</tr>");
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
    protected String formatStormTrackPoint(
            StormTrackPoint stp) throws VisADException, RemoteException {
        Unit   displayUnit = getDisplayUnit();
        double value;

        String result;
        if (stp == null) {
            result = "";
        } else {
            result = "<tr><td>" + "Storm: "
                     + stp.getStormInfo().toString() + "</td></tr>";
            result = result + "<tr><td>" + "Track Point Time: "
                     + stp.getTrackPointTime() + "</td></tr>";
            result = result + "<tr><td>"  + "Max Wind Speed: "
                     + stp.getAttribute("MaxWindSpeed") + "</td></tr>";
            result = result + "<tr><td>" + "Min Pressure: "
                     + stp.getAttribute("MinPressure") + "</td></tr>";

            int length = result.length();
            result = StringUtil.padLeft(result, 5 * (20 - length), "&nbsp;");
        }

        return result;
    }


    /**
     * Find the closest ob in the field to the particular EarthLocation
     *
     * @param el  the EarthLocation
     * @param theStates _more_
     *
     * @return the closest ob (may be null);
     *
     * @throws RemoteException   Java RMI problem
     * @throws VisADException    VisAD problem
     */
    protected StormTrackPoint findClosestOb(
            EarthLocation el,
            List<StormDisplayState> theStates) throws VisADException,
                RemoteException {

        if ((el == null) || (theStates == null)) {
            return null;
        }

        int             numStates   = theStates.size();
        StormTrackPoint closestOb   = null;


        int[]           clickPt     = boxToScreen(earthToBox(el));
        double          minDistance = 20;
        //        System.err.println ("click:" + clickPt[0]+"/"+clickPt[1] + " " +minDistance);

        for (int i = 0; i < numStates; i++) {
            StormDisplayState sds   = theStates.get(i);
            StormInfo         sinfo = sds.getStormInfo();
            Hashtable<Way, WayDisplayState> wdMap =
                sds.getWayDisplayStateMap();
            // Way obsWay = new Way(Way.OBSERVATION);

            WayDisplayState       obsWDS   = wdMap.get(Way.OBSERVATION);

            List<StormTrack>      tracks   = obsWDS.getTracks();
            StormTrack            obsTrack = tracks.get(0);

            List<StormTrackPoint> stpList  = obsTrack.getTrackPoints();
            int                   size     = stpList.size();
            for (int j = 0; j < size; j++) {
                StormTrackPoint stp      = stpList.get(j);
                EarthLocation   stpLoc   = stp.getTrackPointLocation();
                int[]           obScreen = boxToScreen(earthToBox(stpLoc));
                double distance = GuiUtils.distance(obScreen, clickPt);
                if (distance < minDistance) {
                    closestOb   = stp;
                    minDistance = distance;
                }
            }

            //            System.err.println ("\t" + obScreen[0]+"/"+obScreen[1] + " d:" + distance);

        }
        return closestOb;
    }


}

