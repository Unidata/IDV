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
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.radar.RadarConstants;
import ucar.unidata.geoloc.Bearing;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.idv.DisplayInfo;
import ucar.unidata.idv.MapViewManager;
import ucar.unidata.idv.ViewManager;
import ucar.unidata.util.Range;

import ucar.visad.RadarMapProjection;
import ucar.visad.display.CrossSectionSelector;
import ucar.visad.display.SelectorDisplayable;

import visad.*;

import visad.bom.Radar3DCoordinateSystem;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import java.beans.PropertyChangeEvent;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.List;


/**
 * Class for displaying WSR-88D Level II cross sections as color shaded displays.
 * @author Unidata
 * @version $ $
 */
public class Level2ColorCrossSectionControl extends ColorCrossSectionControl {

    /**
     * Length of selector line in units of VisAD coords; = 1/2 width
     * of VisAD wireframe box around data
     */
    private double defaultLen       = 1.0,
                   currentCSLineLen = 0.2;

    /**
     * Default constructor
     */
    public Level2ColorCrossSectionControl() {}

    /**
     * _more_
     */
    public void initDone() {
        try {
            setRequestProperties();


            setVerticalAxisRange(new Range(0, 20000));

            loadDataFromLine();
            FieldImpl fieldImpl =
                (FieldImpl) (getGridDataInstance().getGrid()).getSample(0);
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
     * Called after all initialization is finished.
     * Labels plot in control window. Loads data in displays.
     *
     * @param choice _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */

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
            //return false;
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
     * Load or reload data for a RHI selector line which has moved.
     */

    /**
     * Load or reload data for a cross section.
     */
    public void crossSectionChanged() {
        try {
            //  EarthLocation[] elArray = getLineCoords();
            //  startLocation = elArray[0];
            //  endLocation   = elArray[1];
            setVerticalAxisRange(new Range(0, 20000));
            setRequestProperties();
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
    // get (and make if necessary)
    // the requester Hastable of properties that is carried along with
    // the data instance

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

    // make the requester Hastable of properties that is carried along with
    // the data instance

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
            MapViewManager mm = getMapViewManager();
            RadarMapProjection rp =
                (RadarMapProjection) mm.getMainProjection();
            LatLonPoint llp = rp.getCenterLatLon();
            initLinePosition((float) llp.getLatitude().getValue(),
                             (float) llp.getLongitude().getValue());
        }
        getRequestProperties().put(RadarConstants.PROP_VCS_START,
                                   startLocation);
        getRequestProperties().put(RadarConstants.PROP_VCS_END, endLocation);

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
     * _more_
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
     * _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected void loadDataFromLine() throws VisADException, RemoteException {
        if ( !getHaveInitialized()) {
            return;
        }

        EarthLocation[] elArray = getLineCoords();
        startLocation = elArray[0];
        endLocation   = elArray[1];

        getGridDataInstance().reInitialize();
        FieldImpl grid = getGridDataInstance().getGrid();
        if (grid == null) {
            return;
        }
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

        // for RHI control line show only the end point away from radar;
        // make center at radar station immovable.
        //csSelector.dontShowStartPoint();
        //csSelector.dontShowMiddlePoint();
        //csSelector.setStartPointFixed(true);
        //csSelector.setStartPointVisible(false);  //also = no manipilate
        //csSelector.setMidPointVisible(false);    //false also = no manipilate
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

        GriddedSet domainSet = (GriddedSet) GridUtil.getSpatialDomain(
                                   getGridDataInstance().getGrid());

        // Get location for label of control window
        Radar3DCoordinateSystem transform =
            (Radar3DCoordinateSystem) domainSet.getCoordinateSystem();
        //   get station location from the data coordinate transform
        float  stationLat = (transform.getCenterPoint())[0];
        float  stationLon = (transform.getCenterPoint())[1];
        float  stationEl  = (transform.getCenterPoint())[2];

        List   choices    = getDataChoices();
        String staname    = ((DataChoice) choices.get(0)).getName();

        // If this isn't satisfactory, then please implement a Misc or
        // DisplayConventions method that will take an EarthLocation and
        // return a nicely formatted string.
        initLinePosition(stationLat, stationLon);
        setCSLineLength(defaultLen);
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
     * Make a FieldImpl suitable for the  2D RHI display;
     * of form (time -> (integer_index->(x,altitude) -> parm));
     * x axis positions are in distance along cross section from one end.
     * from FieldImpl (time -> (integer_index->(range,az,elev) -> parm))
     *
     * @param inputfieldImpl   The data as a Field
     * @return  a 2D version of the data
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */






}
