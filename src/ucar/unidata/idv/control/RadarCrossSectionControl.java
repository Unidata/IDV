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


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridDataInstance;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.radar.RadarConstants;
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.DisplayInfo;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Range;

import ucar.visad.RadarMapProjection;
import ucar.visad.display.CrossSectionSelector;
import ucar.visad.display.SelectorDisplayable;

import visad.*;

import visad.bom.Radar3DCoordinateSystem;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.MapProjection;

import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;


/**
 * Class for displaying WSR-88D Level II cross sections as color shaded displays.
 * @author Unidata
 * @version $ $
 */
public class RadarCrossSectionControl extends ColorCrossSectionControl {

    /**
     * Length of selector line in units of VisAD coords; = 1/2 width
     * of VisAD wireframe box around data
     */
    private double defaultLen       = 1.0,
                   currentCSLineLen = 0.2;

    /**
     * Default constructor
     */
    public RadarCrossSectionControl() {}

    /**
     * initialize the cross section
     */
    public void initDone() {
        try {
            setRequestProperties();


            setVerticalAxisRange(new Range(0, 20000));

            loadDataFromLine();
            GridDataInstance ginst = getGridDataInstance();
            if (ginst == null) {
                return;
            }
            FieldImpl fieldImpl = (FieldImpl) (ginst.getGrid()).getSample(0);
            GriddedSet domainSet =
                (GriddedSet) GridUtil.getSpatialDomain(fieldImpl);
            Unit   xUnit     = domainSet.getSetUnits()[0];
            String unitlabel = xUnit.toString();
            if (unitlabel.equalsIgnoreCase("1000.0 m")) {
                unitlabel = "km";
            }
            //xScale.setTitle("Distance along ground (" + unitlabel + ")");
            // Do we need this here or is it already done in setData?
            updateCenterPoint();
        } catch (Exception e) {
            logException("Initializing the csSelector", e);
        }
        csSelector.addPropertyChangeListener(this);
        updatePositionWidget();
    }



    /**
     * Set the data in the control
     *
     * @param choice  choice representing the data
     * @return true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {        
        if ( !super.setData(choice)) {
            System.out.println("set data is false");
            return false;
        }
        dataIs3D = true;
        updateCenterPoint();
        return true;
    }

    /**
     * Handle property change
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                SelectorDisplayable.PROPERTY_POSITION)) {
            // if line length changed, make new RHI
            //if (setCSLineLength(defaultLen))
            crossSectionChanged();
        } else {
            super.propertyChange(evt);
        }
    }



    /**
     * Load or reload data for a cross section.
     */
    public void crossSectionChanged() {
        try {
            //  EarthLocation[] elArray = getLineCoords();
            //  startLocation = elArray[0];
            //  endLocation   = elArray[1];
            setVerticalAxisRange(new Range(0, 20000));
            loadDataFromLine();
            updateLegendLabel();
            updatePositionWidget();
            CrossSectionSelector cs = getCrossSectionSelector();
            doShare(SHARE_XSLINE, new Object[] { cs.getStartPoint(),
                    cs.getEndPoint() });
        } catch (Exception exc) {
            logException("Error in crossSectionChanged ", exc);
        }
    }

    /**
     * _more_
     *
     * @param latlon _more_
     *
     * @return _more_
     */
    private String fmt(double latlon) {
        return getDisplayConventions().formatLatLon(latlon);
    }

    /**
     *  update the position widget
     */
    private void updatePositionWidget() {
        try {
            if (startLLW == null) {
                return;
            }
            EarthLocation[] coords = getLineCoords();
            startLLW.setLatLon(fmt(coords[0].getLatitude().getValue()),
                               fmt(coords[0].getLongitude().getValue()));
            endLLW.setLatLon(fmt(coords[1].getLatitude().getValue()),
                             fmt(coords[1].getLongitude().getValue()));

        } catch (Exception exc) {
            logException("Error setting position ", exc);
        }
    }

    /**
     * Get the request properties hash table.
     * @return  table of properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        //System.out.println("  getRequestProperties PROP_VCS ");
        props.put(RadarConstants.PROP_VCS, new Boolean(true));
        if ((startLocation != null) && (endLocation != null)) {

            props.put(RadarConstants.PROP_VCS_START, startLocation);
            props.put(RadarConstants.PROP_VCS_END, endLocation);
        }
        return props;
    }



    /**
     * Set the request properties
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void setRequestProperties()
            throws VisADException, RemoteException {
        getRequestProperties().put(RadarConstants.PROP_VCS,
                                   new Boolean(true));
        if ((startLocation == null) || (endLocation == null)) {
            MapViewManager     mm = getMapViewManager();
            MapProjection      mp = mm.getMainProjection();
            RadarMapProjection rp;
            try {
                rp = (RadarMapProjection) mp;
            } catch (ClassCastException ce) {
                LogUtil.consoleMessage("Radar projection cast error\n");
                return;
            }
            LatLonPoint llp = rp.getCenterLatLon();
            initLinePosition((float) llp.getLatitude().getValue(),
                             (float) llp.getLongitude().getValue());
        }
        getRequestProperties().put(RadarConstants.PROP_VCS_START,
                                   startLocation);
        getRequestProperties().put(RadarConstants.PROP_VCS_END, endLocation);

    }

    /**
     * Set the start location of the cross section line
     *
     *
     *
     * @param sl _more_
     */
    public void setStartLocation(EarthLocation sl) {
        startLocation = sl;
    }

    /**
     * Get the start location of the cross section line
     *
     * @return earthlocation object of line
     */
    public EarthLocation getStartLocation() {
        return startLocation;
    }


    /**
     * Set the end location of the cross section line
     *
     *
     *
     * @param sl _more_
     */
    public void setEndLocation(EarthLocation sl) {
        endLocation = sl;
    }

    /**
     * Get the end location of the cross section line
     *
     * @return earthlocation object of line
     */
    public EarthLocation getEndLocation() {
        return endLocation;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public MapViewManager getMapViewManager() {
        List displayList = getDisplayInfos();
        for (int i = 0, n = displayList.size(); i < n; i++) {
            DisplayInfo info = (DisplayInfo) displayList.get(i);
            ViewManager vm   = info.getViewManager();
            if (vm instanceof MapViewManager) {
                return (MapViewManager) vm;
            }
        }
        return null;
    }

    /**
     * init positio from south to north
     *
     * @param stationLat _more_
     * @param stationLon _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void initLinePosition(float stationLat, float stationLon)
            throws VisADException, RemoteException {
        if (getVerticalAxisRange() == null) {
            setVerticalAxisRange(new Range(0, 20000));
        }
        //   get station location from the data coordinate transform

        LatLonPointImpl lp1 = Bearing.findPoint(stationLat, stationLon, 0,
                                  150.0f, null);

        LatLonPointImpl lp2 = Bearing.findPoint(stationLat, stationLon, 180,
                                  150.0f, null);

        startLocation = new EarthLocationTuple(lp2.getLatitude(),
                lp2.getLongitude(), 0.0);
        endLocation = new EarthLocationTuple(lp1.getLatitude(),
                                             lp1.getLongitude(), 0.0);


    }

    /**
     * reload the cross section data
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void loadDataFromLine() throws VisADException, RemoteException {
        if ( !getHaveInitialized()) {
            return;
        }

        setRequestProperties();
        EarthLocation[] elArray = getLineCoords();
        startLocation = elArray[0];
        endLocation   = elArray[1];
        GridDataInstance ginst = getGridDataInstance();
        if (ginst == null) {
            return;
        }
        ginst.reInitialize();

        FieldImpl grid = ginst.getGrid();
        dataIs3D = true;
        loadData(grid);


    }

    /**
     * make a Selector line which shows and controls where
     * RHI position is; uses current value of beam azimuth.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void createCrossSectionSelector()
            throws VisADException, RemoteException {

        // make a Selector line there
        csSelector = new CrossSectionSelector();
        // move z level of line to near TOP of VisAD display box
        csSelector.setZValue(0.99);


        setCSLineLength(defaultLen);

        setRequestProperties();

    }

    /**
     * If csSelector has changed in length,
     * redraw selector line, with same start point, at same azimuth "beamAz",
     * with length len in VisAD coords.
     * @param len length of line desired
     * @return boolean - true if length was changed; else false
     */
    private boolean setCSLineLength(double len) {
        if (csSelector == null) {
            return false;
        }
        RealTuple center = null;
        try {
            double x1 = 0;
            double y1 = 0;
            if (startLocation != null) {
                double[] box = earthToBox(startLocation);
                if (box != null) {
                    x1 = box[0];
                    y1 = box[1];
                }
            }
            RealTuple start =
                new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                              new double[] { x1,
                                             y1 });

            // get dx2 and dy2 - offset distances in x and y
            double x2 = 0;
            double y2 = 0;
            if (endLocation != null) {
                double[] box = earthToBox(endLocation);
                if (box != null) {
                    x2 = box[0];
                    y2 = box[1];
                }
            }
            RealTuple newend =
                new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                              new double[] { x2,
                                             y2 });
            // plot line at same angle; new length
            csSelector.setPosition(start, newend);
        } catch (Exception e) {
            logException("setCSLineLength:", e);
            return false;
        }
        return true;
    }


    /**
     * Update the center point location
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void updateCenterPoint()
            throws VisADException, RemoteException {

        GridDataInstance ginst = getGridDataInstance();
        if (ginst == null) {
            return;
        }
        FieldImpl fieldImpl = ginst.getGrid();

        GriddedSet domainSet =
            (GriddedSet) GridUtil.getSpatialDomain(fieldImpl);

        // Get location for label of control window
        Radar3DCoordinateSystem transform =
            (Radar3DCoordinateSystem) domainSet.getCoordinateSystem();
        //   get station location from the data coordinate transform
        if ((startLocation == null) || (endLocation == null)) {
            float  stationLat = (transform.getCenterPoint())[0];
            float  stationLon = (transform.getCenterPoint())[1];
            float  stationEl  = (transform.getCenterPoint())[2];

            List   choices    = getDataChoices();
            String staname    = ((DataChoice) choices.get(0)).getName();


            initLinePosition(stationLat, stationLon);
            setCSLineLength(defaultLen);
        }
    }

    /**
     * Get whether we can autoscale the vertical scale
     *
     * @return false
     */
    public boolean getAllowAutoScale() {
        return false;
    }

    /**
     * Get whether we should autoscale the Y Axis.
     *
     * @return false
     */
    public boolean getAutoScaleYAxis() {
        return false;
    }





    /**
     * Set the position of the selector
     *
     * @param startLoc    Start location
     * @param endLoc      End location
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void setPosition(EarthLocation startLoc, EarthLocation endLoc)
            throws VisADException, RemoteException {

        double x1 = 0;
        double y1 = 0;
        double[] box1 = earthToBox(startLoc);
                if (box1 != null) {
                    x1 = box1[0];
                    y1 = box1[1];
                }

         RealTuple start =
                new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                              new double[] { x1,
                                             y1 });


        double x2 = 0;
        double y2 = 0;
        double[] box2 = earthToBox(startLoc);
                if (box2 != null) {
                    x2 = box2[0];
                    y2 = box2[1];
                }
         RealTuple end =
                new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                              new double[] { x2,
                                             y2 });
        csSelector.setPosition(start, end);

        startLocation = startLoc;
        endLocation = endLoc;
        csSelector.setZValue(0.99);
        setCSLineLength(defaultLen);
        setRequestProperties();
    }



}
