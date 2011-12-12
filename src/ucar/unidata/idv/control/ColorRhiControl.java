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

import ucar.unidata.data.radar.RadarConstants;

import ucar.unidata.idv.CrossSectionViewManager;
import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.Coord;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.ThreeDSize;

import ucar.visad.Util;



import ucar.visad.display.Contour2DDisplayable;
import ucar.visad.display.CrossSectionSelector;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.GridDisplayable;
import ucar.visad.display.MapLines;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.XSDisplay;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.Length;



import visad.*;

import visad.bom.Radar2DCoordinateSystem;
import visad.bom.Radar3DCoordinateSystem;

import visad.data.units.Parser;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;

import visad.georef.MapProjection;

import visad.java3d.DisplayImplJ3D;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.lang.Float;

import java.lang.Thread;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * Class to make displays and controls for a pseudo-RHI plot
 * of WSR-88D Level II data.
 * Also makes a control, a JFrame with gui controls of the RHI,
 * and with a 2D display of the same RHI.
 *
 * @author IDV Development Team
 * @version $Revision: 1.64 $
 */

public class ColorRhiControl extends ColorCrossSectionControl {

    /** the beam azimuth */
    protected float beamAz;

    /** the beam azimuth */
    private float lastLoadedAz = Float.NaN;

    /** flag for auto rotations */
    protected boolean autorotateOn = false;


    /** the station label */
    protected JLabel stationLabel = new JLabel("   ");

    /** center point for the radar data */
    private EarthLocation centerPoint;

    /**
     * Length of selector line in units of VisAD coords; = 1/2 width
     * of VisAD wireframe box around data
     */
    private double defaultLen       = 1.0,
                   currentCSLineLen = 0.2;

    /** timestamp for rotation */
    private int timestamp = 0;

    /** Earth radius */
    private double R = 6371.01;
    // from http://ssd.jpl.nasa.gov/phys_props_earth.html


    /**
     * Default constructor.  Sets the appropriate attribute flags
     * to determine what gui objects appear in the control window.
     */
    public ColorRhiControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_COLOR | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT);
    }


    /**
     * Called after all initialization is finished.
     * Labels plot in control window. Loads data in displays.
     */
    public void initDone() {
        try {
            setRequestProperties();
            XSDisplay xsDisplay = crossSectionView.getXSDisplay();
            getCrossSectionViewManager().setNewDisplayTitle("RHI  Azimuth: "
                    + getDisplayConventions().formatAngle(beamAz));
            // set altitude scale in plot to 0 to 20000 m
            if (getVerticalAxisRange() == null) {
                setVerticalAxisRange(new Range(0, 20000));
            }
            updateAxisLabels();

            /*
            xsDisplay.setYRange(0.0, 20000.0);
            AxisScale yScale = xsDisplay.getYAxisScale();
            yScale.setMajorTickSpacing(4000);
            yScale.setMinorTickSpacing(500);
            yScale.setTitle("Altitude MSL (m)");
            yScale.setSnapToBox(true);
            yScale.createStandardLabels(20000, 0, 0, 4000);
            yScale.setGridLinesVisible(true);
            // set x axis to cover 0 to 400 km from radar
            xsDisplay.setXRange(0.0, 400.0);
            AxisScale xScale = xsDisplay.getXAxisScale();
            xScale.setMajorTickSpacing(100);
            xScale.setMinorTickSpacing(50);
            xScale.setSnapToBox(true);
            xScale.createStandardLabels(400, 0, 0, 100);
            // xScale.setTitle("Distance along ground");  // see below with unit
            xScale.setSnapToBox(true);
            xScale.setGridLinesVisible(true);
            */
            /*
            // create a mesh for the display
            MapLines grid = new MapLines("Data grid");
            RealTupleType rtt = new RealTupleType(RealType.Altitude,
                                    RealType.XAxis);
            ArrayList setList = new ArrayList();
            for (int i = 1; i < 4; i++) {
                float[][] samples = {
                    { 0.f, 20000.f }, { 100f * i, 100f * i }
                };
                setList.add(new Gridded2DSet(rtt, samples, 2));
            }
            for (int j = 1; j < 5; j++) {
                float[][] samples = {
                    { 4000.f * j, 4000.f * j }, { 0, 400 }
                };
                setList.add(new Gridded2DSet(rtt, samples, 2));
            }
            Gridded2DSet[] xys = new Gridded2DSet[setList.size()];
            setList.toArray(xys);
            grid.setData(new UnionSet(rtt, xys));
            grid.setColor(xsDisplay.getForeground());
            xsDisplay.addDisplayable(grid);
            */
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
    }

    /**
     * Update the axis labels
     */
    private void updateAxisLabels() {
        try {
            XSDisplay xsDisplay = crossSectionView.getXSDisplay();
            AxisScale yScale    = xsDisplay.getYAxisScale();
            yScale.setMajorTickSpacing((int) (getVerticalAxisRange().getMax()
                    / 5));
            yScale.setMinorTickSpacing(yScale.getMajorTickSpacing() / 5);
            yScale.setTitle("Altitude MSL (m)");
            yScale.setSnapToBox(true);
            yScale.createStandardLabels(getVerticalAxisRange().getMax(), 0,
                                        0, yScale.getMajorTickSpacing());
            yScale.setGridLinesVisible(true);
            // set x axis to cover 0 to 400 km from radar
            xsDisplay.setXRange(0.0, 400.0);
            AxisScale xScale = xsDisplay.getXAxisScale();
            xScale.setMajorTickSpacing(100);
            xScale.setMinorTickSpacing(50);
            xScale.setSnapToBox(true);
            xScale.createStandardLabels(400, 0, 0, 100);
            // xScale.setTitle("Distance along ground");  // see below with unit
            xScale.setSnapToBox(true);
            xScale.setGridLinesVisible(true);
        } catch (Exception e) {
            logException("updating axis labels", e);
        }
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
            return false;
        }
        updateCenterPoint();
        return true;
    }

    /**
     * Get the label for the CrossSectionView
     * @return  return the name of the cross section view
     */
    protected String getCrossSectionViewLabel() {
        return "RHI";
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
        float stationLat = (transform.getCenterPoint())[0];
        float stationLon = (transform.getCenterPoint())[1];
        float stationEl  = (transform.getCenterPoint())[2];
        centerPoint = new EarthLocationTuple(stationLat, stationLon,
                                             stationEl);
        startLocation = centerPoint;
        List   choices = getDataChoices();
        String staname = ((DataChoice) choices.get(0)).getName();

        // If this isn't satisfactory, then please implement a Misc or
        // DisplayConventions method that will take an EarthLocation and
        // return a nicely formatted string. 
        stationLabel.setText(
            staname.substring(0, 4).trim() + "  ("
            + getDisplayConventions().formatEarthLocation(centerPoint, true)
            + ")");
        setCSLineLength(defaultLen);
    }


    /**
     * Load or reload data for a RHI selector line which has moved.
     */
    public void crossSectionChanged() {
        try {
            beamAz = (float) getCSSAzimuth();
            setRequestProperties();
            loadDataFromLine();
            List choices = getDataChoices();
            getCrossSectionViewManager().setNewDisplayTitle(
                ((DataChoice) choices.get(0)).getName() + " Azimuth "
                + getDisplayConventions().formatAngle(beamAz));
            doShare(SHARE_XSLINE, this);
        } catch (Exception exc) {
            logException("crossSectionChanged", exc);
        }
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
            if (centerPoint != null) {
                double[] box = earthToBox(centerPoint);
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
            double dx2 = len * Math.cos(Math.toRadians(90.0 - (beamAz)));
            double dy2 = len * Math.sin(Math.toRadians(90.0 - (beamAz)));
            RealTuple newend =
                new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                              new double[] { x1 + dx2,
                                             y1 + dy2 });
            endLocation = boxToEarth(new double[] { x1 + dx2, y1 + dy2, 0 });
            // plot line at same angle; new length
            csSelector.setPosition(start, newend);
        } catch (Exception e) {
            logException("setCSLineLength:", e);
            return false;
        }
        return true;
    }


    /**
     * Put one end of the rhi control line on the radar position (centered);
     * leave other end where it is.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    protected void centerLinePosition()
            throws VisADException, RemoteException {
        // center location in visad box is -
        RealTuple start =
            new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                          new double[] { 0.0,
                                         0.0 });
        csSelector.setPosition(start, csSelector.getEndPoint());
        beamAz = (float) getCSSAzimuth();
        // change title on display control box 
        List choices = getDataChoices();
        getCrossSectionViewManager().setNewDisplayTitle(
            ((DataChoice) choices.get(0)).getName() + " Azimuth "
            + getDisplayConventions().formatAngle(beamAz));
        setRequestProperties();
    }



    /**
     * argument to runAuto ensures each thread has an id
     */
    protected void autorotateLine() {
        Misc.run(new Runnable() {
            public void run() {
                runAuto(++timestamp);
            }
        });
    }


    /**
     * Autorotate the RHI line and displays, clockwise.
     * advance is hard coded to 1.0 degree per step; larger steps can miss
     * important features in data. Beam width is typically 1.5 degrees.
     *
     * @param ts  timestep between slices
     */
    private void runAuto(int ts) {
        int requestedSleepDelay = 500;  // could be set by entry box in gui TODO
        int  timeUsed,
             sleepDelay         = 500;  // millisecs between moves
        long millis;

        beamAz = (float) getCSSAzimuth();

        while (true && getActive()) {
            // loop over azimuths -
            // find cross section line start and end points
            try {
                // get current time
                millis = System.currentTimeMillis();
                shiftSelectorToAzimuth(beamAz + 1.0f);
                // get new beamaz 
                beamAz = (float) getCSSAzimuth();
                // plot autorotated RHI
                if ( !getActive()) {
                    break;
                }
                loadDataFromLine();
                getCrossSectionViewManager().setNewDisplayTitle(
                    ((DataChoice) getDataChoices().get(0)).getName()
                    + " Azimuth "
                    + getDisplayConventions().formatAngle(beamAz));
                // Measure time to do one cycle of this try block;
                // make sure sleepDelay is greater than that;
                // (need time to make display too)
                // this ensures will work on any computer, even slow ones.
                timeUsed = (int) (System.currentTimeMillis() - millis);
                if (timeUsed > sleepDelay) {
                    sleepDelay = timeUsed + 250;
                } else if (timeUsed < requestedSleepDelay - 250) {
                    sleepDelay = requestedSleepDelay;
                }
                //System.out.println("  autorotate data get used "+timeUsed+" ms");
            } catch (Exception exc) {}

            try {
                Thread.currentThread().sleep(sleepDelay);
            } catch (Exception exc) {}

            // Stop this thread if either the user said to stop by button click
            // changing the boolean, or if current autorotate id "timestamp"
            // has been advanced showing another autorotate thread is running.
            if ( !autorotateOn || (ts != timestamp)) {
                break;
            }
        }
    }


    /**
     * Get the length of the line
     * @return   length of the line
     */
    private double getCSLineLength() {
        System.out.println("  2. cd sel len is " + currentCSLineLen);
        try {
            if (csSelector == null) {
                return 0.0;
            }
            RealTuple start = csSelector.getStartPoint();
            RealTuple end   = csSelector.getEndPoint();
            double    x1    = ((Real) start.getComponent(0)).getValue();
            double    y1    = ((Real) start.getComponent(1)).getValue();
            double    x2    = ((Real) end.getComponent(0)).getValue();
            double    y2    = ((Real) end.getComponent(1)).getValue();
            return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
        } catch (Exception e) {
            logException("getCSLineLength", e);
            return 0;
        }
    }


    /**
     * Rotate the selector line to another azimuth;
     * remember to do next loadDataFromLine() if needed
     *
     * @param newazimuth
     */
    private void shiftSelectorToAzimuth(float newazimuth) {
        RealTuple center = null;
        try {
            if (csSelector == null) {
                return;
            }
            RealTuple start = csSelector.getStartPoint();
            RealTuple end   = csSelector.getEndPoint();
            double    x1    = ((Real) start.getComponent(0)).getValue();
            double    y1    = ((Real) start.getComponent(1)).getValue();
            double    x2    = ((Real) end.getComponent(0)).getValue();
            double    y2    = ((Real) end.getComponent(1)).getValue();
            // use radar trigonometry to angle origin due east
            double h = Math.sqrt((x2 - x1) * (x2 - x1)
                                 + (y2 - y1) * (y2 - y1));
            double x3 = h * Math.cos(Math.toRadians(90.0 - (newazimuth)));
            double y3 = h * Math.sin(Math.toRadians(90.0 - (newazimuth)));
            RealTuple newend =
                new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                              new double[] { x1 + x3,
                                             y1 + y3 });
            // plot shifted line
            csSelector.setPosition(start, newend);
        } catch (Exception e) {
            logException("shiftSelectorToAzimuth:", e);
        }
    }


    /**
     * Shift the azimuth of the RHI from radar location, 360 degrees,
     * clockwise from N
     *
     * @param c   the azimuth of the RHI from radar location,
     *            360 degrees, cw from N
     */
    public void resetBeamAz(float c) {
        beamAz = c;
        shiftSelectorToAzimuth(beamAz);
        try {
            loadDataFromLine();
        } catch (Exception exc) {}
        setRequestProperties();
        getCrossSectionViewManager().setNewDisplayTitle("Radar RHI  Azimuth "
                + getDisplayConventions().formatAngle(beamAz));
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
    protected DisplayableData createXSDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable display = new Grid2DDisplayable("vcs_col"
                                        + paramName, true);
        display.setTextureEnable(true);
        addAttributedDisplayable(display, FLAG_COLORTABLE);
        return display;
    }

    /**
     * Create the <code>DisplayableData</code> that will be used
     * to depict the data in the control's display.
     *
     * @return  depictor for data in main display
     *
     * @throws VisADException  unable to create depictor
     * @throws RemoteException  unable to create depictor (shouldn't happen)
     */
    protected DisplayableData createVCSDisplay()
            throws VisADException, RemoteException {
        Grid2DDisplayable display = new Grid2DDisplayable("vcs_" + paramName,
                                        true);
        display.setTextureEnable(true);
        addAttributedDisplayable(display, FLAG_COLORTABLE);
        return display;
    }

    /**
     * set label for button controling color of the selector line
     * to "line color:"
     * @return  the label
     */
    public String getColorWidgetLabel() {
        return "Selector Line Color";
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
        final JButton rotatebutton = new JButton("Start");
        rotatebutton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    if ( !autorotateOn) {
                        autorotateOn = true;
                        autorotateLine();
                        rotatebutton.setText("Stop");
                    } else {
                        autorotateOn = false;
                        rotatebutton.setText("Start");
                    }
                } catch (Exception ve) {
                    logException(" autorotate rhi ", ve);
                }
            }
        });
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Autorotate RHI:"),
                GuiUtils.left(rotatebutton)));
    }


    /**
     * Create the component that goes into the 'Display' tab
     *
     * @return Display tab component
     */
    protected JComponent getDisplayTabComponent_old() {
        JComponent comp = super.getDisplayTabComponent();
        return GuiUtils.centerBottom(
            comp, GuiUtils.left(GuiUtils.label("Station: ", stationLabel)));
    }

    /** _more_ */
    private Container viewContents;

    /**
     * _more_
     *
     * @return _more_
     */
    protected JComponent getDisplayTabComponent() {

        viewContents = crossSectionView.getContents();
        //If foreground is not null  then this implies we have been unpersisted
        //We do this here because the CrossSectionViewManager sets the default black on white
        //colors in its init method which might nor be called until we ask for its contents


        crossSectionView.setContentsBorder(null);
        return GuiUtils.centerBottom(
            viewContents,
            GuiUtils.left(GuiUtils.label("Station: ", stationLabel)));
    }



    /**
     * Set whether the rhi is in autorotate mode
     * Used by XML persistence.
     *
     * @param v  boolean true is rotating
     */
    public void setAutorotateOn(boolean v) {
        autorotateOn = v;
    }

    /**
     * get whether the rhi is in autorotate mode
     *
     * @return true if rotating
     */
    public boolean getAutorotateOn() {
        return autorotateOn;
    }

    /**
     * Set the azimuth of the RHI from radar location, 360 degrees, cw from N
     *
     * @param az the azimuth of the RHI from radar location,
     *   360 degrees, cw from N
     *
     * @deprecated use setBeamAzimuth
     */
    public void setBeamAz(float az) {
        beamAz = az;
    }

    /**
     * Set the value of the
     * azimuth of the RHI from radar location, 360 degrees, cw from N.
     * Does NOT move the beam; this is only for persistence. Use resetBeamAz.
     *
     * @param az      the azimuth of the RHI from radar location, 360 degrees,
     *                cw from N
     *
     */
    public void setBeamAzimuth(float az) {
        beamAz = az;
    }

    /**
     * Get the azimuth of the rhi from radar location, 360 degrees, cw from N
     *
     * @return beamAz the azimuth of the rhi from radar
     */
    public float getBeamAzimuth() {
        return beamAz;
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
        csSelector.dontShowStartPoint();
        csSelector.dontShowMiddlePoint();
        csSelector.setStartPointFixed(true);
        csSelector.setStartPointVisible(false);  //also = no manipilate
        csSelector.setMidPointVisible(false);    //false also = no manipilate
        setCSLineLength(defaultLen);

        setRequestProperties();
    }

    /**
     * using incoming shared RHI data, reset this display to match
     *
     * @param from       source of data to be shared
     * @param dataId     id of sharable data
     * @param data       sharable data
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if (dataId.equals(SHARE_XSLINE)) {
            if (csSelector == null) {
                return;
            }
            ColorRhiControl fromControl = (ColorRhiControl) data[0];
            try {
                CrossSectionSelector cs =
                    fromControl.getCrossSectionSelector();
                csSelector.setPosition(cs.getStartPoint(), cs.getEndPoint());
                loadDataFromLine();
            } catch (Exception e) {
                logException("receiveShareData:" + dataId, e);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
    }

    /**
     * Get azimuth of the cross section selector line.
     * Use convention cw 360 degrees from N, as per WSR88D Level II radar data
     *
     * @return  the azimuth
     */
    private double getCSSAzimuth() {
        RealTuple start = csSelector.getStartPoint();
        RealTuple end   = csSelector.getEndPoint();
        double    x1    = 0.0;
        double    y1    = 0.0;;
        double    x2    = 1.0;;
        double    y2    = 0.0;
        try {
            x1 = ((Real) start.getComponent(0)).getValue();
            y1 = ((Real) start.getComponent(1)).getValue();
            x2 = ((Real) end.getComponent(0)).getValue();
            y2 = ((Real) end.getComponent(1)).getValue();
        } catch (Exception e) {
            logException("az from csSelector", e);
        }
        double deltax = x2 - x1;
        double deltay = y2 - y1;
        double az     = 90.0 - Math.toDegrees(Math.atan2(deltay, deltax));
        if (az < 0.0) {
            az += 360.0;
        }
        beamAz = (float) az;
        updateLegendLabel();
        return az;
    }

    /**
     * Override the base class method to include the station name,
     * "moment" (data type as reflectivity) and
     * and the RHI azimuth in the legend label;
     *
     * @param labels List of labels
     * @param legendType The type of legend, BOTTOM_LEGEND or SIDE_LEGEND
     */
    public void getLegendLabels(List labels, int legendType) {
        super.getLegendLabels(labels, legendType);
        labels.add("Azimuth: " + getDisplayConventions().formatAngle(beamAz));
    }

    /**
     * Method to update the legend label when the RHI position changes.
     */
    protected void updateLegendLabel() {
        super.updateLegendLabel();
        CrossSectionViewManager csvm        = getCrossSectionViewManager();
        List                    dataChoices = getDataChoices();
        DisplayConventions      dc          = getDisplayConventions();
        if ((csvm != null) && (dataChoices != null)
                && (dataChoices.size() > 0) && (dc != null)) {
            csvm.setNewDisplayTitle(
                ((DataChoice) dataChoices.get(0)).getName() + " Azimuth "
                + dc.formatAngle(beamAz));
        }
    }



    /**
     * Create and loads a 2D FieldImpl from the existing getGridDataInstance()
     * at the position indicated by the controlling Selector line end points;
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void loadDataFromLine() throws VisADException, RemoteException {
        if ( !getHaveInitialized() || (lastLoadedAz == beamAz)) {
            return;
        }
        getGridDataInstance().reInitialize();
        FieldImpl grid = getGridDataInstance().getGrid();
        if (grid == null) {
            return;
        }
        loadData(grid);
        lastLoadedAz = beamAz;
    }

    /**
     * Get (and make if necessary)
     * the requester Hastable of properties that is carried along with
     * the data instance
     *
     * @return  Hashtable of request properties
     */
    protected Hashtable getRequestProperties() {
        Hashtable props = super.getRequestProperties();
        props.put(RadarConstants.PROP_AZIMUTH, new Float(beamAz));
        return props;
    }

    /**
     * Make the requester Hastable of properties that is carried along with
     * the data instance
     */
    protected void setRequestProperties() {
        getRequestProperties().put(RadarConstants.PROP_AZIMUTH,
                                   new Float(beamAz));
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
    protected FieldImpl make2DData(FieldImpl inputfieldImpl)
            throws VisADException, RemoteException {
        FieldImpl fi             = null;
        boolean   istimeSequence = GridUtil.isTimeSequence(inputfieldImpl);
        if (istimeSequence) {
            Set timeSet  = inputfieldImpl.getDomainSet();
            int numTimes = timeSet.getLength();
            MathType timeType =
                ((FunctionType) inputfieldImpl.getType()).getDomain();
            for (int ti = 0; ti < numTimes; ti++) {
                FieldImpl one3DFI = (FieldImpl) inputfieldImpl.getSample(ti);
                FieldImpl one2DFI = make2DDataAtOneTime(one3DFI);
                if (ti == 0) {
                    FunctionType fiFunction =
                        new FunctionType(timeType,
                                         (FunctionType) one2DFI.getType());
                    fi = new FieldImpl(fiFunction, timeSet);
                }
                if (one2DFI != null) {
                    fi.setSample(ti, one2DFI, false);
                }
            }
        } else {
            fi = make2DDataAtOneTime(inputfieldImpl);
        }

        return fi;
    }


    /**
     * Make a FieldImpl with one time's 2D RHI display;
     * of form (integer_index -> ((x, altitude) -> parm));
     * x axis positions are in distance along cross section from one end.
     * made from FieldImpl of (integer_index->((range,az,elev)->param))
     *
     * @param inputfieldImpl  field to munge
     *
     * @return   munged 2D field
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    private FieldImpl make2DDataAtOneTime(FieldImpl inputfieldImpl)
            throws VisADException, RemoteException {
        FieldImpl fi       = null;
        FlatField testff   = null;
        Set       indexSet = null;
        int       numBeams = 1;
        if (inputfieldImpl.getDomainDimension() == 1) {
            indexSet = inputfieldImpl.getDomainSet();
            numBeams = indexSet.getLength();
        }
        for (int bi = 0; bi < numBeams; bi++) {
            FieldImpl fieldImpl = (FieldImpl) inputfieldImpl.getSample(bi);
            if (fieldImpl.isMissing()) {
                // System.out.println("missing data at azimuth " + beamAz + " beam " + bi + " out of " + numBeams);
                continue;
            }
            float[][] signalVals = fieldImpl.getFloats(false);

            GriddedSet domainSet =
                (GriddedSet) GridUtil.getSpatialDomain(fieldImpl);

            Radar3DCoordinateSystem transform =
                (Radar3DCoordinateSystem) domainSet.getCoordinateSystem();
            float     stationElev   = (transform.getCenterPoint())[2];
            float[][] domainSamples = domainSet.getSamples(false);
            float[]   ranges        = domainSamples[0];
            float[] azimuths = domainSamples[1];  //all same or nearly so for an rhi
            float[]   elevs = domainSamples[2];
            float[][] plane = new float[2][domainSet.getLength()];
            double    range, elevation, altitude;
            for (int i = 0; i < plane[0].length; i++) {
                range     = ranges[i];
                elevation = Math.toRadians(elevs[i]);
                float dx = (float) (range * Math.cos(elevation));
                plane[0][i] = dx;
                plane[1][i] = 1000.0f * (float) (range * Math.sin(elevation))
                              + stationElev
                              + 1000.0f * (float) ((dx * dx) / 12742.0);  //1st order in dx/R
                // or + 1000.0f *(-2*R + Math.sqrt(4*R*R + 4*dx*R))/2;// 2nd order
            }

            RealType xType = null;
            if (crossSectionView != null) {
                XSDisplay xs = crossSectionView.getXSDisplay();
                xType = xs.getXAxisType();
            } else {
                xType = Length.getRealType();
            }

            RealTupleType xzRTT = new RealTupleType(xType, RealType.Altitude);
            int           sizeX = domainSet.getLengths()[0];
            int           sizeZ = domainSet.getLengths()[1];
            Unit          zUnit = (transform == null)
                                  ? domainSet.getSetUnits()[2]
                                  : transform.getReferenceUnits()[2];
            Gridded2DSet oneBeamG2DS = new Gridded2DSet(xzRTT, plane, sizeX,
                                           sizeZ, (CoordinateSystem) null,
                                           new Unit[] { CommonUnits.KILOMETER,
                    zUnit }, (ErrorEstimate[]) null, false, false);
            RealTupleType oneBeamMT =
                (RealTupleType) getGridDataInstance().getRangeType();
            FunctionType oneBeamFT = new FunctionType(xzRTT, oneBeamMT);
            FlatField    oneBeamFF = new FlatField(oneBeamFT, oneBeamG2DS);
            oneBeamFF.setSamples(signalVals, false);
            if (bi == 0) {
                RealType indexType = RealType.getRealType("integer_index");
                FunctionType fiFunction = new FunctionType(indexType,
                                              oneBeamFF.getType());
                Integer1DSet intSet = new Integer1DSet(numBeams);
                fi = new FieldImpl(fiFunction, intSet);
                fi.setSample(bi, oneBeamFF, false);
            } else {
                fi.setSample(bi, oneBeamFF, false);
            }
        }
        return fi;
    }

    /**
     * Get whether we can smooth this display
     *
     * @return false
     */
    public boolean getAllowSmoothing() {
        return false;
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
     * Get the data projection label
     *
     * @return  the data projection label
     */
    protected String getDataProjectionLabel() {
        return "Use Radar Projection";
    }

}
