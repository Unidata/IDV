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

package ucar.unidata.idv.control;


import ucar.unidata.collab.Sharable;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.Coord;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;



import ucar.visad.display.Contour2DDisplayable;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.GridDisplayable;
import ucar.visad.display.SelectorDisplayable;

import visad.*;

import visad.bom.Radar3DCoordinateSystem;

import visad.data.units.Parser;

import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.georef.MapProjection;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * Class to make controls and displays for WS-88D level II data
 * showing all values for one parm from a full volume scan (one data file)
 * as pixels in 3d space above the Earth.
 *
 * @author IDV Development Team
 * @version $Revision: 1.27 $
 */

public class ColorRadarVolumeControl extends GridDisplayControl {

    // Identifier for sharing 

    /** sharing property for volume */
    public static final String SHARE_VOLUME =
        "ColorRadarVolumeControl.SHARE_VOLUME";

    /** property for radar volume */
    public static final String RADAR_VOLUME = "Radar volume scan";

    /** displayable for data */
    private DisplayableData mainDisplay;

    /** flag for whether display is 3D or not */
    private boolean displayIs3D = false;

    /** the data */
    private FieldImpl fieldImpl = null;

    /** label for the station information */
    private JLabel stationLabel = new JLabel("   ");



    /**
     * Default constructor.  Sets the appropriate attribute flags.
     */
    public ColorRadarVolumeControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT | FLAG_SELECTRANGE);
    }


    /**
     * Create the <code>DisplayableData</code> that will be used
     * to depict the data in the main display.
     *
     * @return  depictor for data in main display
     *
     * @throws VisADException  unable to create depictor
     * @throws RemoteException  unable to create depictor (shouldn't happen)
     */
    protected DisplayableData createMainDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable display = new Grid2DDisplayable("radar_volume"
                                        + paramName, true);
        display.setUseRGBTypeForSelect(true);
        return display;
    }


    /**
     * Get the <code>GridDisplayable</code> used for setting the
     * data.
     *
     * @return data's <code>GridDisplayable</code>
     */
    public GridDisplayable getGridDisplayable() {
        return (GridDisplayable) mainDisplay;
    }


    /**
     * Get the <code>DisplayableData</code> used for depicting
     * data in the main display.
     *
     * @return main display depictor
     */
    public DisplayableData getMainDisplay() {
        return mainDisplay;
    }

    /**
     * Basic constructor-like actions; returns false if setData fails
     *
     * @param dataChoice  data choice for selection
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        //System.out.println("  rvc: init ");

        //Are we in 3d?
        displayIs3D = isDisplay3D();


        setRequestProperties();

        mainDisplay = createMainDisplay();
        mainDisplay.setPointSize(getPointSize());

        // set the data (which uses the displayables above).
        if ( !setData(dataChoice)) {
            return false;
        }
        addDisplayable(mainDisplay, FLAG_COLORTABLE | FLAG_SELECTRANGE);

        return true;
    }


    /**
     * Called after all initialization is finished. After
     * init() and setData(). Loads the requested data in the display.
     */
    public void setLabelFromData() {

        try {
            GriddedSet domainSet = (GriddedSet) GridUtil.getSpatialDomain(
                                       getGridDataInstance().getGrid());
            // Get location for label of control window
            Radar3DCoordinateSystem transform =
                (Radar3DCoordinateSystem) domainSet.getCoordinateSystem();
            //   get station location from the data coordinate transform
            float stationLat = (transform.getCenterPoint())[0];
            float stationLon = (transform.getCenterPoint())[1];
            float stationEl  = (transform.getCenterPoint())[2];
            List  choices    = getDataChoices();
            String staname = ((DataChoice) choices.get(0)).getName();
            EarthLocationTuple centerPoint =
                new EarthLocationTuple(stationLat, stationLon, stationEl);
            stationLabel.setText(
                staname.substring(0, 3) + " "
                + getDisplayConventions().formatEarthLocation(
                    centerPoint, true));
        } catch (Exception e) {
            logException("setLabelFromData ", e);
        }
    }


    /**
     * Used in making control window.
     * Called by doMakeWindow in DisplayControlImpl, which then calls its
     * doMakeMainButtonPanel(), which makes more buttons.
     *
     * @return  UI contents
     */
    public Container doMakeContents() {
        try {
            return GuiUtils.doLayout(new Component[] {
                doMakeWidgetComponent() }, 1, GuiUtils.WT_Y, GuiUtils.WT_YN);
        } catch (Exception exc) {
            logException("doMakeContents", exc);
        }
        return null;
    }


    /**
     * Add any specialized control widgets for this control
     * to the list.
     *
     * @param  controlWidgets  <code>List</code> to add to.
     *
     * @throws VisADException  unable to create controls
     * @throws RemoteException  unable to create controls (shouldn't happen)
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);

        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Station:"),
                                             stationLabel));
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Point Size:"),
                GuiUtils.left(doMakePointSizeWidget())));


    }


    /**
     * _more_
     *
     * @param value _more_
     */
    public void setPointSize(float value) {
        super.setPointSize(value);
        if (mainDisplay != null) {
            try {
                mainDisplay.setPointSize(getPointSize());
            } catch (Exception e) {
                logException("Setting point size", e);
            }
        }
    }




    /**
     * Get (and make if necessary)
     * the requester Hastable of properties that is carried along with
     * the data instance; this one tells Level2Adapter to call the
     * getVolume method.
     *
     * @return  hashtable of properties
     */
    protected Hashtable getRequestProperties() {
        if (requestProperties == null) {
            requestProperties =
                Misc.newHashtable(ColorRadarVolumeControl.RADAR_VOLUME,
                                  new Float(0.0f));
        }
        return requestProperties;
    }

    /**
     * Make the requester Hastable of properties that is carried along with
     * the data instance; this one tells Level2Adapter to call the
     * getVolume method.
     */
    protected void setRequestProperties() {
        if (requestProperties == null) {
            requestProperties =
                Misc.newHashtable(ColorRadarVolumeControl.RADAR_VOLUME,
                                  new Float(0.0f));
        } else {
            requestProperties.clear();
            requestProperties.put(ColorRadarVolumeControl.RADAR_VOLUME,
                                  new Float(0.0f));
        }
    }

    /**
     * Called by the init(datachoice) method.
     * Reset or set data choice's data into the displayables.
     * Do everything necessary to load in data.
     *
     * @param dataChoice   choice for the data
     * @return  true if this was successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected boolean setData(DataChoice dataChoice)
            throws VisADException, RemoteException {
        if ( !super.setData(dataChoice)) {
            return false;
        }
        //System.out.println("  rvc: set data ");

        // change the displayed units if different from actual
        Unit newUnit = getDisplayUnit();
        if ((newUnit != null) && !newUnit.equals(getRawDataUnit())
                && Unit.canConvert(newUnit, getRawDataUnit())) {
            mainDisplay.setDisplayUnit(newUnit);
        }

        loadDataFromGrid();
        setLabelFromData();
        return true;
    }



    /**
     * reset the display with this data gotten by sharing
     *
     * @param from    object that is sharing data
     * @param dataId  id for shareable data
     * @param data    the data being shared
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        //        System.out.println(this + "got share data");
        if (dataId.equals(SHARE_VOLUME)) {

            ColorRadarVolumeControl fromControl =
                (ColorRadarVolumeControl) data[0];
            try {
                loadDataFromGrid();
            } catch (Exception e) {
                logException("receiveShareData:" + dataId, e);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }


    /**
     * Create and loads a 3d FieldImpl from the existing getGridDataInstance()
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void loadDataFromGrid() throws VisADException, RemoteException {
        //System.out.println("     rvc: loaddatafromgrid ");
        // remove old data object etc, so can make new one

        //jeffmc: If we do this reinitialize then when this display is
        //created we load the data twice.
        //getGridDataInstance().reInitialize();

        // start chain that ends in 
        // Level2RadarDataSource -> Level2Adapter.getData ();
        fieldImpl = (FieldImpl) (getGridDataInstance().getGrid());
        //System.out.println("     rvc: loaddatafromgrid - have data field");

        loadData(fieldImpl);
    }


    /**
     * Load the display with this data.
     *
     * @param fieldImpl  the data as a field
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private void loadData(FieldImpl fieldImpl)
            throws VisADException, RemoteException {
        //System.out.println("  rvc: loaddata ");

        // put the data into the main IDV display window
        getGridDisplayable().loadData(fieldImpl);
    }



}
