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
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.radar.RadarConstants;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Range;
import ucar.unidata.util.TwoFacedObject;

import ucar.visad.display.CrossSectionSelector;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.Grid2DDisplayable;
import ucar.visad.display.XSDisplay;
import ucar.visad.quantities.CommonUnits;
import ucar.visad.quantities.Length;

import visad.*;

import visad.bom.Radar3DCoordinateSystem;

import visad.georef.EarthLocation;

import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * Created by IntelliJ IDEA.
 * User: yuanho
 * Date: Apr 15, 2009
 * Time: 1:56:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class RadarRhiControl extends ColorCrossSectionControl {

    /** identified when sharing the rhi angle */
    public static final String SHARE_ANGLE = "RadarRhiControl.SHARE_ANGLE";

    /** the azimuth */
    protected float currentAngle = Float.NaN;

    /** the beam azimuth */
    private float lastLoadedAz = Float.NaN;


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

    /** Component to hold collections */
    JComboBox azimuthSelector;

    /** timestamp for rotation */
    private int timestamp = 0;

    /** Earth radius */
    private double R = 6371.01;
    // from http://ssd.jpl.nasa.gov/phys_props_earth.html

    /**
     *  Do we request 3d or 2d data.
     */
    private boolean use3D = true;

    /**
     * Default constructor.  Sets the appropriate attribute flags
     * to determine what gui objects appear in the control window.
     */
    public RadarRhiControl() {
        setAttributeFlags(FLAG_COLORTABLE | FLAG_COLOR | FLAG_DATACONTROL
                          | FLAG_DISPLAYUNIT);
    }

    /**
     * add the zposition slider
     *
     * @return _more_
     */
    protected boolean useZPosition() {
        return false;
    }

    /**
     * Called after all initialization is finished.
     * Labels plot in control window. Loads data in displays.
     */
    public void initDone() {
        try {
            setRequestProperties();
            //XSDisplay xsDisplay = crossSectionView.getXSDisplay();
            getCrossSectionViewManager().setNewDisplayTitle("RHI  Azimuth: "
                    + getDisplayConventions().formatAngle(currentAngle));
            // set altitude scale in plot to 0 to 20000 m
            if (getVerticalAxisRange() == null) {
                setVerticalAxisRange(new Range(0, 20000));
            }
            updateAxisLabels();

            /*      List          choices      = getDataChoices();
                  DataSelection tmpSelection =
                      new DataSelection(getDataSelection());
                  tmpSelection.setFromLevel(null);
                  tmpSelection.setToLevel(null);
                  DataChoice dc          = (DataChoice) choices.get(0);
                  List       levelsList1 = dc.getAllLevels(tmpSelection);

                  GuiUtils.setListData(azimuthSelector, levelsList1);  */
            //DataChoice dc = (DataChoice)choices.get(0);
            //List levels = dc.getAllLevels();
            //GuiUtils.setListData(azimuthSelector, levels);
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
            csSelector.clearDisplayables();
        } catch (Exception e) {
            logException("Initializing the csSelector", e);
        }
        // csSelector.addPropertyChangeListener(this);
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

        currentLevel = getDataSelection().getFromLevel();
        if (currentLevel == null) {
            List levelsList = choice.getAllLevels(getDataSelection());
            if ((levelsList != null) && (levelsList.size() > 0)) {
                currentLevel = levelsList.get(0);
            }
        }


        Real c = (Real) currentLevel;
        currentAngle = (float) c.getValue();
        getDataSelection().setLevel(currentLevel);

        DataSelection tmpSelection = new DataSelection(getDataSelection());
        tmpSelection.setFromLevel(null);
        tmpSelection.setToLevel(null);

        List                 levelsList1 = choice.getAllLevels(tmpSelection);
        List<TwoFacedObject> levels      = new ArrayList();
        if (levelsList1.size() >= 1) {
            for (int i = 0; i < levelsList1.size(); i++) {
                String azim = levelsList1.get(i).toString();
                String as = getDisplayConventions().formatAngle(
                                Float.parseFloat(azim));
                //System.out.println("azimath " + as);
                TwoFacedObject tobj = new TwoFacedObject(as, azim);
                levels.add(tobj);
            }
        }

        /*        if (levelsList1.size() >= 1) {
                   String azim = levelsList1.get(0).toString();
                   currentAngle = Float.parseFloat(azim);
               }   */
        setRequestProperties();

        if ( !super.setData(choice)) {
            return false;
        }
        azimuthSelector = new JComboBox();
        GuiUtils.setListData(azimuthSelector, levels);
        azimuthSelector.setSelectedItem(currentLevel);
        azimuthSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                try {
                    if (azimuthSelector.getSelectedItem() == null) {
                        return;
                    }
                    String azim = TwoFacedObject.getIdString(
                                      azimuthSelector.getSelectedItem());
                    currentAngle = Float.parseFloat(azim);
                    setRequestProperties();

                    crossSectionChanged();
                } catch (Exception ve) {
                    logException(" change azimath ", ve);
                }
            }
        });


        //updateCenterPoint();
        return true;
    }


    /**
     * Get the label for the CrossSectionView
     * @return  return the name of the cross section view
     */
    protected String getCrossSectionViewLabel() {
        return "RHISWEEP";
    }

    /**
     *  Set the Use3D property.
     *
     *  @param value The new value for Use3D
     */
    public void setUse3D(boolean value) {
        use3D = value;
    }

    /**
     *  Get the Use3D property.
     *
     *  @return The Use3D
     */
    public boolean getUse3D() {
        return use3D;
    }

    /**
     * Load or reload data for a RHI selector line which has moved.
     */
    protected Object currentLevel;

    /**
     * _more_
     */
    public void crossSectionChanged() {
        try {
            setRequestProperties();
            loadDataFromLine();
            List choices = getDataChoices();
            if (getCrossSectionViewManager() != null) {
                getCrossSectionViewManager().setNewDisplayTitle(
                    ((DataChoice) choices.get(0)).getName() + " Azimuth "
                    + getDisplayConventions().formatAngle(currentAngle));
                updateLegendLabel();
            }
            doShare(SHARE_XSLINE, this);

        } catch (Exception exc) {
            logException("crossSectionChanged", exc);
        }
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

        // change title on display control box
        List choices = getDataChoices();
        getCrossSectionViewManager().setNewDisplayTitle(
            ((DataChoice) choices.get(0)).getName() + " Azimuth "
            + getDisplayConventions().formatAngle(currentAngle));
        //setRequestProperties();
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


    /**
     * _more_
     *
     * @param controlWidgets _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);

        controlWidgets.add(new WrapperWidget(this,
                                             GuiUtils.rLabel("Azimuth:"),
                                             GuiUtils.left(azimuthSelector)));
    }


    /**
     * Create the component that goes into the 'Display' tab
     *
     * @return Display tab component
     */
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
     * Set the azimuth of the RHI from radar location, 360 degrees, cw from N
     *
     * @param az the azimuth of the RHI from radar location,
     *   360 degrees, cw from N
     *
     * @deprecated use setBeamAzimuth
     */
    public void setCurrentAngle(float az) {
        currentAngle = az;
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
    public void getCurrentAngle(float az) {
        currentAngle = az;
    }

    /**
     * Get the azimuth of the rhi from radar location, 360 degrees, cw from N
     *
     * @return beamAz the azimuth of the rhi from radar
     */
    public float getBeamAzimuth() {
        return currentAngle;
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

        csSelector.setVisible(false);
        // setRequestProperties();
    }

    /**
     * _more_
     *
     * @param len _more_
     *
     * @return _more_
     */
    private boolean setCSLineLength(double len) {

        return true;
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
        if ( !getHaveInitialized()) {
            return;
        }
        if (dataId.equals(SHARE_ANGLE)) {
            try {
                //      applyNewAngle(((Double) data[0]).floatValue());
            } catch (Exception exc) {
                logException("receiveShareData.angle", exc);
            }
            return;
        }
        super.receiveShareData(from, dataId, data);
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
        labels.add("Azimuth: "
                   + getDisplayConventions().formatAngle(currentAngle));
    }

    /**
     * Method to update the legend label when the RHI position changes.
     */
    protected void updateLegendLabel() {
        super.updateLegendLabel();
        getCrossSectionViewManager().setNewDisplayTitle(
            ((DataChoice) getDataChoices().get(0)).getName() + " Azimuth "
            + getDisplayConventions().formatAngle(currentAngle));
    }



    /**
     * Create and loads a 2D FieldImpl from the existing getGridDataInstance()
     * at the position indicated by the controlling Selector line end points;
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void loadDataFromLine() throws VisADException, RemoteException {
        if ( !getHaveInitialized() || (lastLoadedAz == currentAngle)) {
            return;
        }
        getRequestProperties().put(RadarConstants.PROP_AZIMUTH,
                                   new Double(currentAngle));
        getGridDataInstance().reInitialize();
        //getGridDataInstance().putRequestProperty(RadarConstants.PROP_AZIMUTH,
        //        new Double(currentAngle));

        FieldImpl grid = getGridDataInstance().getGrid();
        if (grid == null) {
            return;
        }
        loadData(grid);
        lastLoadedAz = currentAngle;
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
        props.put(RadarConstants.PROP_AZIMUTH, new Double(currentAngle));
        return props;
    }

    /**
     * Make the requester Hastable of properties that is carried along with
     * the data instance
     */
    protected void setRequestProperties() {
        getRequestProperties().put(RadarConstants.PROP_AZIMUTH,
                                   new Double(currentAngle));
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
                range     = ranges[i] / 1000.f;
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
                    zUnit }, (ErrorEstimate[]) null, false);
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
