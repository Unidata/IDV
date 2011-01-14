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
import ucar.unidata.idv.ControlContext;

import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Range;

import ucar.visad.UtcDate;
import ucar.visad.display.Animation;


import ucar.visad.display.Displayable;
import ucar.visad.display.IndicatorPoint;
import ucar.visad.display.ProfileLine;
import ucar.visad.display.XYDisplay;

import visad.*;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import visad.util.DataUtility;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TimeZone;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;


/**
 * Given a 2D VisAD FieldImpl for one parameter,
 * make a single plot of the range data of one parm, against time,
 * for one location in the 2D field given by a probe the user can move.
 *
 * @author Stuart Wier (copying from TimeHeightControl)
 * @version $Revision: 1.52 $
 */
public class TimeSeriesControl extends LineProbeControl {

    /** ID for sharing profile location */
    public static final String SHARE_PROFILE =
        "TimeSeriesControl.SHARE_PROFILE";

    /** XY display for the data depiction */
    private XYDisplay timeSeriesDisplay;

    /** the grid */
    private FieldImpl grid;

    /** the time series display */
    private ProfileLine lineDispbl;

    /** indicator point for the bouncing ball */
    private IndicatorPoint indicator;

    /** a label for the parameter name */
    private JLabel paramLabel;

    /** a label for the position */
    private JLabel positionLabel;

    /** a label for the time */
    private JLabel timeLabel;

    /** flag for time sequence */
    private boolean isSequence = false;


    /** listener for animation property changes */
    private PropertyChangeListener animationListener;


    /**
     * Default constructor; set attribute flags.
     */
    public TimeSeriesControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_DATACONTROL | FLAG_DISPLAYUNIT);
    }


    /**
     * Construct a time series display of data values at one map location
     * versus time, and its control buttons, in a window.
     *
     * @param dataChoice the DataChoice
     *
     * @return  true if successful
     *
     * @throws VisADException  couldn't create a VisAD object needed
     * @throws RemoteException  couldn't create a remote object needed
     */

    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        // make the display, a DisplayMaster
        timeSeriesDisplay = new XYDisplay("timeseries_of_data",
                                          RealType.Time, RealType.YAxis);
        addDisplayMaster(timeSeriesDisplay);
        timeSeriesDisplay.showAxisScales(true);
        timeSeriesDisplay.getYAxisScale().setSnapToBox(true);
        timeSeriesDisplay.getXAxisScale().setSnapToBox(true);

        // make the Displayables
        lineDispbl = new ProfileLine("timeseries");
        addAttributedDisplayable(lineDispbl, FLAG_COLOR);
        indicator = new IndicatorPoint("indicator",
                                       new RealTupleType(RealType.Time,
                                           RealType.YAxis));
        addAttributedDisplayable(indicator, FLAG_COLOR);
        indicator.setPointSize(5);
        indicator.setVisible(true);

        // change the displayed units if different from actual
        Unit newUnit = getDisplayUnit();
        if (newUnit != null) {
            timeSeriesDisplay.setYDisplayUnit(newUnit);
        }
        lineDispbl.setVisible(true);


        timeSeriesDisplay.setDisplayables(new Displayable[] { lineDispbl,
                indicator });
        timeSeriesDisplay.draw();

        doMakeProbe();


        return setData(dataChoice);
    }



    /**
     * A hook to allow derived classes to tell us to add this
     * as an animation listener
     *
     * @return Add as animation listener
     */
    protected boolean shouldAddAnimationListener() {
        return true;
    }

    /**
     * Respond to a timeChange event
     *
     * @param time new time
     */
    protected void timeChanged(Real time) {
        try {
            updateIndicatorPosition();
        } catch (Exception exc) {
            logException("changePosition", exc);
        }
        super.timeChanged(time);
    }


    /**
     * Called after init().  Set up the axis labels and load in data
     */
    public void initDone() {
        super.initDone();
        try {
            setViewParameters();
            loadTimeSeries(getPosition());
        } catch (Exception exc) {
            logException("initDone", exc);
        }
    }

    /**
     * Set the data from the given dataChoice.
     *
     * @param dataChoice new data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice dataChoice)
            throws RemoteException, VisADException {

        if ( !super.setData(dataChoice)) {
            return false;
        }
        grid = getGridDataInstance().getGrid();

        // Check to see if this is a sequence or  not.  There is no sense in
        // creating a time CrossSection with 0 or 1 time.
        if ( !(GridUtil.isTimeSequence(grid))
                || (GridUtil.getTimeSet(grid).getLength() <= 1)) {
            throw new VisADException(
                "Need more than one time to create a time series");
        }

        if (getHaveInitialized()) {
            setViewParameters();
        }
        if (dataChoice.getProperties() != null) {
            Hashtable props = dataChoice.getProperties();
            EarthLocation elt =
                (EarthLocation) props.get(INITIAL_PROBE_EARTHLOCATION);
            if ((elt != null) && (getInitialPosition() == null)) {
                // get XYZ position of the earth location
                RealTuple rt    = earthToBoxTuple(elt);
                Real[]    reals = rt.getRealComponents();
                setProbePosition(new RealTuple(new Real[] { reals[0],
                        reals[1] }));
            }
        }
        loadTimeSeries(getPosition());
        return true;
    }



    /**
     * Set the parameters for the view.  Mostly deals with the
     * vertical scale.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setViewParameters() throws VisADException, RemoteException {
        if (getGridDataInstance() != null) {
            timeSeriesDisplay.setYAxisType(
                getGridDataInstance().getRealType(0));
            if (paramLabel != null) {
                paramLabel.setText(
                    getGridDataInstance().getDataChoice().toString());
            }
            setRange(timeSeriesDisplay);
            applyDisplayUnit();
            AxisScale yAxis = timeSeriesDisplay.getYAxisScale();
            //yAxis.setTitle(getGridDataInstance().getDataChoice().toString());
            yAxis.setTitle(paramName);
            yAxis.setSnapToBox(true);
            setXAxisLabels((SampledSet) GridUtil.getTimeSet(grid));
        }
    }

    /**
     * Methods to do the things that need to be done when the data range
     * changes.
     *
     * @param display  display to change
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void setRange(XYDisplay display)
            throws VisADException, RemoteException {
        Range range = getRange();
        display.setYRange(range.getMin(), range.getMax());
    }

    /**
     * Apply the display unit to the data depictions.  Override super
     * class method to set vertical axis.
     *
     * @throws VisADException error applying unit
     * @throws RemoteException error applying unit
     */
    protected void applyDisplayUnit() throws VisADException, RemoteException {
        super.applyDisplayUnit();
        timeSeriesDisplay.setYDisplayUnit(getDisplayUnit());
    }

    /**
     * Apply the data range to the data depictions
     *
     * @throws VisADException error applying range
     * @throws RemoteException error applying range
     */
    protected void applyRange() throws VisADException, RemoteException {
        super.applyRange();
        timeSeriesDisplay.setYDisplayUnit(getDisplayUnit());
        setRange(timeSeriesDisplay);
    }

    /**
     * Make the UI contents for the control window.
     *
     * @return  UI container
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        positionLabel = new JLabel(" ", JLabel.LEFT);
        timeLabel     = new JLabel(" ", JLabel.LEFT);
        paramLabel    = new JLabel(" ", JLabel.LEFT);
        JComponent cmp = (JComponent) timeSeriesDisplay.getComponent();
        cmp.setPreferredSize(new Dimension(450, 450));
        return GuiUtils.centerBottom(GuiUtils.topCenterBottom(paramLabel,
        //timeSeriesDisplay.getComponent(), 
        cmp, GuiUtils.vbox(positionLabel,
                           timeLabel)), doMakeWidgetComponent());
    }


    /**
     * This gets called when either the user moves the probe point or
     * when we get a sharable event to move the probe point.
     *
     * @param position probe position (NOT USED)
     */
    protected void probePositionChanged(RealTuple position) {
        try {
            loadTimeSeries(getPosition());
        } catch (Exception exc) {
            logException("probePositionChanged", exc);
        }

    }

    /**
     * Given a location where the data is to be extracted,
     * using the local grid with 2D data for >1 times,
     * get the time series at the location. And
     * create a vertical line showing where probe is, in the probe's display.
     *
     * @param position where data is to be extracted
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public void loadTimeSeries(RealTuple position)
            throws VisADException, RemoteException {

        if ( !getHaveInitialized() || (grid == null)) {
            return;
        }

        LatLonPoint llp = null;

        // Convert incoming position to a location for a probe line.

        RealTupleType rttype = (RealTupleType) position.getType();

        if (rttype.equals(RealTupleType.SpatialCartesian2DTuple)) {
            Real[] reals = position.getRealComponents();
            // get earth location of the x,y position in the VisAD display
            EarthLocationTuple elt =
                (EarthLocationTuple) boxToEarth(new double[] {
                    reals[0].getValue(),
                    reals[1].getValue(), 1.0 });
            llp = elt.getLatLonPoint();
        } else if (rttype.equals(RealTupleType.SpatialEarth2DTuple)) {
            Real[] reals = position.getRealComponents();
            llp = new LatLonTuple(reals[1], reals[0]);
        } else if (rttype.equals(RealTupleType.LatitudeLongitudeTuple)) {
            Real[] reals = position.getRealComponents();
            llp = new LatLonTuple(reals[0], reals[1]);
        } else {
            throw new VisADException(
                "Can't convert position to navigable point");
        }

        FieldImpl newFI = GridUtil.sample(grid, llp,
                                          getDefaultSamplingModeValue());

        lineDispbl.setData(newFI);
        updateIndicatorPosition();

        // set values in label showing lat and long of position.
        if (positionLabel != null) {
            positionLabel.setText(
                getDisplayConventions().formatLatLonPoint(llp));
        }
    }


    /**
     * Update the position of the bouncing ball in the display
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    private void updateIndicatorPosition()
            throws VisADException, RemoteException {
        Animation anime = getViewAnimation();
        if (anime == null) {
            return;
        }
        if ((lineDispbl == null) || !lineDispbl.hasData()) {
            return;
        }
        Set timeSet = anime.getSet();
        int pos     = 0;
        if (timeSet != null) {
            pos = anime.getCurrent();
        } else {
            timeSet = ((FieldImpl) lineDispbl.getData()).getDomainSet();
        }
        RealTuple timeTuple = DataUtility.getSample(timeSet, pos);
        RealTuple range =
            (RealTuple) ((FieldImpl) lineDispbl.getData()).evaluate(
                timeTuple);
        indicator.setPoint(new RealTuple(new Real[] {
            (Real) timeTuple.getComponent(0),
            (Real) range.getComponent(0) }));
        if (timeLabel != null) {
            timeLabel.setText("Time: " + (Real) timeTuple.getComponent(0));
        }
    }


    /**
     * Set the labels for the X Axis in the display
     *
     * @param timeSet   set of times
     *
     * @throws VisADException  VisAD error
     */
    private void setXAxisLabels(SampledSet timeSet) throws VisADException {
        try {
            if (timeSet == null) {
                return;
            }
            Real startTime = (Real) DataUtility.getSample(timeSet,
                                 0).getComponent(0);
            double start = startTime.getValue();
            Real endTime = (Real) DataUtility.getSample(timeSet,
                               timeSet.getLength() - 1).getComponent(0);
            double    end        = endTime.getValue();
            Hashtable timeLabels = new Hashtable();
            int       numSteps   = timeSet.getLength();

            int       step       = (numSteps < 5)
                                   ? 1
                                   : timeSet.getLength() / 5;  //no more than 5 labels;
            double majorTickSpacing = endTime.getValue()
                                      - startTime.getValue();
            for (int k = 0; k < timeSet.getLength(); k += step) {
                Real r = (Real) DataUtility.getSample(timeSet,
                             k).getComponent(0);
                double time = r.getValue();
                if (k == step) {
                    majorTickSpacing = (time - start);
                }
                timeLabels.put(new Double(time),
                               UtcDate.formatUtcDate(new DateTime(r),
                                   "dd'/'HH"));
            }
            // do this so we get the last one
            timeLabels.put(new Double(end),
                           UtcDate.formatUtcDate(new DateTime(endTime),
                               "dd'/'HH"));

            timeSeriesDisplay.setXRange(start, end);
            AxisScale xScale = timeSeriesDisplay.getXAxisScale();
            xScale.setTickBase(start);
            //double averageTickSpacing = (end-start)/(double)(numSteps);
            double averageTickSpacing = (end - start);
            xScale.setMajorTickSpacing(averageTickSpacing * step);
            xScale.setMinorTickSpacing(averageTickSpacing);
            xScale.setLabelTable(timeLabels);

        } catch (RemoteException re) {}  // can't happen
    }

}
