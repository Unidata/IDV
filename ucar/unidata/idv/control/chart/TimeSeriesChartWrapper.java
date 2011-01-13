/*
 * $Id: TimeSeriesChartWrapper.java,v 1.51 2007/04/16 21:32:12 jeffmc Exp $
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

package ucar.unidata.idv.control.chart;


import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.event.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.urls.*;
import org.jfree.data.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;


import org.python.core.*;
import org.python.util.*;



import ucar.unidata.data.DataChoice;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionRect;


import ucar.unidata.geoloc.projection.*;



import ucar.unidata.idv.control.DisplayControlImpl;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.MidiManager;
import ucar.unidata.util.MidiProperties;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;




import ucar.visad.GeoUtils;
import ucar.visad.Util;
import ucar.visad.display.*;

import visad.*;

import visad.georef.*;

import visad.util.BaseRGBMap;

import visad.util.ColorPreview;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.*;


import java.rmi.RemoteException;


import java.text.SimpleDateFormat;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;


import javax.sound.midi.*;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 * Provides a time series chart
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.51 $
 */
public class TimeSeriesChartWrapper extends PlotWrapper {


    /** Property change id */
    public static final String PROP_TIMERANGE = "prop.timerange";


    /** Holds the dots */
    private CompositeDisplayable dotsHolder;

    /** List of way points */
    private List wayPoints = new ArrayList();

    /** The special animation showing way point */
    private WayPoint timeWayPoint;


    /** List of range filters */
    private List rangeFilters = new ArrayList();

    /** List ot track segments_ */
    private List segments = new ArrayList();


    /** Keep around the data arrays */
    List datas = new ArrayList();

    /** Keep around the min/max values of each data array */
    List ranges = new ArrayList();

    /** The full set of times in the domain */
    double[] times;

    /** The lats of each time */
    double[] lats;

    /** The lons each time */
    double[] lons;

    /** The altitudes of each time */
    double[] alts;

    /** The plot */
    private MyTimeSeriesPlot plot;


    /** The dataset */
    private TimeSeriesCollection dataset;


    /** Last time user clicked on */
    private double lastTimeClick = 0.0;

    /** Which one are we dragging */
    private ChartAnnotation draggedAnnotation;

    /** Closest annotation */
    private ChartAnnotation closestAnnotation;


    /** Are we currently dragging. Note: we may not have an annotation that we are dragging */
    private boolean dragging = false;

    /** Show the dots in the 3d display */
    private boolean showDots = true;

    /**
     *   Last wall clock time we drove the animation time in the main display.
     *   We keep this around so we don't  set the time, get the time changed event and
     *   redisplay.
     */
    private long lastTimeWeDrove = 0;


    /** Sets show the dots  in the properties dialog */
    private JCheckBox showDotsCbx;


    /** keep from infinite looping */
    private boolean inSetTime = false;

    /**
     * Default ctor
     */
    public TimeSeriesChartWrapper() {}


    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public TimeSeriesChartWrapper(String name, List dataChoices) {
        super(name, dataChoices);
    }


    /**
     * Create the chart
     */
    private void createChart() {
        if (chartPanel != null) {
            return;
        }
        dataset = new TimeSeriesCollection();
        ValueAxis timeAxis = doMakeDateAxis();
        timeAxis.setLowerMargin(0.02);
        timeAxis.setUpperMargin(0.02);
        NumberAxis valueAxis = new NumberAxis("Data");
        valueAxis.setAutoRangeIncludesZero(false);
        plot = new MyTimeSeriesPlot(this, dataset, timeAxis, valueAxis);
        plot.setRenderer(doMakeRenderer());
        chart = new JFreeChart(getName(), JFreeChart.DEFAULT_TITLE_FONT,
                               plot, true);

        addAnnotations(segments);
        addAnnotations(wayPoints);
        for (int i = 0; i < wayPoints.size(); i++) {
            WayPoint waypoint = (WayPoint) wayPoints.get(i);
            waypoint.addPropertyChangeListener(this);
        }
        addAnnotations(rangeFilters);

        initXYPlot(plot);
        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            //            renderer.setDefaultShapesVisible(false);
            //            renderer.setDefaultShapesFilled(false);
            //      renderer.setDefaultShapesFilled(true);
        }

        DateAxis axis = (DateAxis) plot.getDomainAxis();
        //        axis.setDateFormatOverride(new SimpleDateFormat("HH:MM:ss"));
        chartPanel = doMakeChartPanel(chart);
    }



    /**
     * Get the plot we use
     *
     * @return The plot
     */
    public XYPlot getPlot() {
        return plot;
    }



    /**
     * Utility to make the renderer
     *
     * @return The renderer
     */
    private XYItemRenderer doMakeRenderer() {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        //        renderer.setDefaultLinesVisible(true);
        //        renderer.setDefaultShapesVisible(false);
        return renderer;

    }

    /**
     * Return the human readable name of this chart
     *
     * @return Chart type name
     */
    public String getTypeName() {
        return "Time Series";
    }


    /**
     *
     * Create the chart if needed
     *
     * @return The gui contents
     */
    protected JComponent doMakeContents() {
        createChart();
        return chartPanel;
    }


    /**
     * Get the list of displayables we use
     *
     * @return List of displayables
     */
    public List getDisplayables() {
        List l = super.getDisplayables();
        if (dotsHolder != null) {
            l.add(dotsHolder);
        }
        return l;
    }



    /**
     * Create the charts
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() throws VisADException, RemoteException {
        long t1 = System.currentTimeMillis();
        loadDatax();
        long t2 = System.currentTimeMillis();
        //        System.err.println ("t:" + (t2-t1));
    }




    /**
     * Create the charts
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadDatax() throws VisADException, RemoteException {

        createChart();
        times = null;
        List unitList           = new ArrayList();
        List dataChoiceWrappers = getDataChoiceWrappers();
        datas.clear();
        ranges.clear();
        try {
            plot.setIgnoreDataSetChanges(true);
            plot.clearRangeAxes();
            plot.setRangeAxis(0, new NumberAxis(""), false);
            for (int dataSetIdx = 0; dataSetIdx < plot.getDatasetCount();
                    dataSetIdx++) {
                TimeSeriesCollection dataset =
                    (TimeSeriesCollection) plot.getDataset(dataSetIdx);
                dataset.removeAllSeries();
            }


            //            plot.clearDatasets();
            //            dataset.setDomainIsPointsInTime(true);
            Hashtable props = new Hashtable();
            props.put(TrackDataSource.PROP_TRACKTYPE,
                      TrackDataSource.ID_TIMETRACE);

            AxisLocation lastSide = AxisLocation.BOTTOM_OR_RIGHT;
            for (int paramIdx = 0; paramIdx < dataChoiceWrappers.size();
                    paramIdx++) {
                DataChoiceWrapper wrapper =
                    (DataChoiceWrapper) dataChoiceWrappers.get(paramIdx);
                DataChoice dataChoice = wrapper.getDataChoice();
                FlatField data =
                    getFlatField((FieldImpl) dataChoice.getData(null, props));
                Set        domainSet   = data.getDomainSet();
                double[][] domain      = domainSet.getDoubles(false);
                double[][] samples     = data.getValues(false);
                double[]   var         = samples[0];


                Unit unit = ucar.visad.Util.getDefaultRangeUnits(data)[0];
                Unit       displayUnit = null;
                if (unit != null) {
                    displayUnit =
                        getDisplayControl().getDisplayConventions()
                            .getDisplayUnit(dataChoice.getName(), null);
                    if ((displayUnit != null) && !displayUnit.equals(unit)) {
                        var  = displayUnit.toThis(var, unit);
                        unit = displayUnit;

                    }
                }


                unitList.add(unit);
                double[] timeValues = getTimeValues(samples,
                                          (FlatField) data);
                double[][] result = filterData(var, timeValues);
                var        = result[0];
                timeValues = result[1];
                TimeSeries series = new TimeSeries(dataChoice.getName()
                                        + ((unit == null)
                                           ? ""
                                           : " [" + unit
                                             + "]"), FixedMillisecond.class);

                //TODO: Find the lat/lon/alt index in the domain
                times = timeValues;
                lats  = domain[0];
                lons  = domain[1];
                alts  = domain[2];
                datas.add(var);
                long   t1  = System.currentTimeMillis();
                double min = 0;
                double max = 0;
                for (int i = 0; i < var.length; i++) {
                    Date dttm = new Date((long) (timeValues[i]));
                    //                    series.addOrUpdate(new FixedMillisecond(dttm), var[i]);
                    series.add(new FixedMillisecond(dttm), var[i]);
                    if ((i == 0) || (var[i] < min)) {
                        min = var[i];
                    }
                    if ((i == 0) || (var[i] > max)) {
                        max = var[i];
                    }
                }
                ranges.add(new ucar.unidata.util.Range(min, max));

                long t2 = System.currentTimeMillis();
                //                System.err.println ("\t time to add:" + (t2-t1));


                TimeSeriesCollection dataset = new TimeSeriesCollection();
                dataset.setDomainIsPointsInTime(true);
                dataset.addSeries(series);
                NumberAxis rangeAxis = new NumberAxis(wrapper.getLabel(unit));
                plot.setRangeAxis(paramIdx, rangeAxis, false);
                plot.setDataset(paramIdx, dataset);

                XYItemRenderer renderer = doMakeRenderer();
                plot.setRenderer(paramIdx, renderer);
                plot.mapDatasetToRangeAxis(paramIdx, paramIdx);
                Color c = wrapper.getColor(paramIdx);
                rangeAxis.setLabelPaint(c);
                renderer.setSeriesPaint(0, c);
                renderer.setSeriesStroke(
                    0, wrapper.getLineState().getStroke());

                AxisLocation side;
                if (wrapper.getSide() == wrapper.SIDE_UNDEFINED) {
                    if (lastSide == AxisLocation.TOP_OR_LEFT) {
                        side = AxisLocation.BOTTOM_OR_RIGHT;
                    } else {
                        side = AxisLocation.TOP_OR_LEFT;
                    }
                } else if (wrapper.getSide() == wrapper.SIDE_LEFT) {
                    side = AxisLocation.TOP_OR_LEFT;
                } else {
                    side = AxisLocation.BOTTOM_OR_RIGHT;
                }
                lastSide = side;
                plot.setRangeAxisLocation(paramIdx, side);
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
            return;
        }

        if (dataChoiceWrappers.size() == 0) {
            NumberAxis axis = new NumberAxis("");
            plot.setRangeAxis(0, axis, false);
            ValueAxis timeAxis = doMakeDateAxis();
            plot.setDomainAxis(0, timeAxis, false);
        }

        plot.setIgnoreDataSetChanges(false);

        try {
            setLocationPositions();
        } catch (Exception exc) {
            LogUtil.logException("Error creating wayPoints", exc);
        }
    }


    /**
     * Utility to create the DataAxis with the correct time zone
     *
     * @return data axis with timezone
     */
    private DateAxis doMakeDateAxis() {
        TimeZone timeZone =
            displayControl.getControlContext().getIdv().getPreferenceManager()
                .getDefaultTimeZone();
        return new DateAxis("Time (" + timeZone.getID() + ")", timeZone);
    }

    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return "Time Series: " + getName();
    }


    /**
     * Set the given annotation as selected. The list is one of rangeFilters,
     * wayPoints or segments.
     *
     * @param annotation The annotation
     * @param list List its in
     * @param dontClear Dont clear the others in the list
     */
    public void setSelectedAnnotation(ChartAnnotation annotation, List list,
                                      boolean dontClear) {

        boolean anyWereWayPoints = false;
        if ( !dontClear) {
            for (int i = 0; i < list.size(); i++) {
                ChartAnnotation ann = (ChartAnnotation) list.get(i);
                if (ann.getSelected()) {
                    anyWereWayPoints |= (ann instanceof WayPoint);
                }
                ann.setSelected(false);
            }
        }
        if (annotation != null) {
            anyWereWayPoints |= (annotation instanceof WayPoint);
            annotation.setSelected(true);
        }

        if (anyWereWayPoints) {
            try {
                setLocationPositions();
            } catch (Exception exc) {
                LogUtil.logException("Setting locations", exc);
            }
        }
    }


    /**
     * Remove all of the annotations in the list. The list is one of rangeFilters,
     * wayPoints or segments.
     *
     * @param annotations  The list of annotations
     */
    public void removeAnnotations(List annotations) {
        List    tmp         = new ArrayList(annotations);

        boolean didWaypoint = false;
        boolean didSegment  = false;
        for (int i = 0; i < tmp.size(); i++) {
            ChartAnnotation annotation = (ChartAnnotation) tmp.get(i);
            if (annotation instanceof WayPoint) {
                didWaypoint = true;
                if (((WayPoint) annotation).getMinutesSpan() > 0) {
                    didSegment = true;
                }
            }
            if (annotation instanceof TrackSegment) {
                didSegment = true;
            }
            removeAnnotation(annotation);
        }

        if (didSegment) {
            firePropertyChange(PROP_TIMERANGE, null, segments);
        }

        try {
            if (didWaypoint) {
                setLocationPositions();
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating wayPoints", exc);
        }
    }


    /**
     * Add the annotations in the list. The list is one of rangeFilters,
     * wayPoints or segments.
     *
     * @param l List of annotations
     */
    private void addAnnotations(List l) {
        for (int i = 0; i < l.size(); i++) {
            plot.addAnnotation((ChartAnnotation) l.get(i));
        }
    }

    /**
     * Find the list that the given annotation belongs in. The list is one of rangeFilters,
     * wayPoints or segments.
     *
     * @param annotation The annotation
     *
     * @return Its list
     */
    private List getList(ChartAnnotation annotation) {
        if (annotation instanceof WayPoint) {
            return wayPoints;
        }
        if (annotation instanceof TrackSegment) {
            return segments;
        }
        return rangeFilters;
    }

    /**
     * Remove the annotation
     *
     * @param annotation The annotation
     */
    public void removeAnnotation(ChartAnnotation annotation) {
        annotation.doRemove();
        getList(annotation).remove(annotation);
        if (timeWayPoint == annotation) {
            setTimeWayPoint(null);
        }
        plot.removeAnnotation(annotation);
        if (annotation instanceof RangeFilter) {
            RangeFilter attached = ((RangeFilter) annotation).getAttached();
            if (attached != null) {
                removeAnnotation(attached);
            }
        }

        if (annotation instanceof WayPoint) {
            List tmp = new ArrayList(segments);
            for (int i = 0; i < tmp.size(); i++) {
                TrackSegment segment = (TrackSegment) tmp.get(i);
                if ((segment.getWayPoint1() == annotation)
                        || (segment.getWayPoint2() == annotation)) {
                    removeAnnotation(segment);
                }
            }
        }
    }



    /**
     * Show the properties
     *
     * @param annotation The annotation
     */
    public void editAnnotation(ChartAnnotation annotation) {
        if ( !annotation.showProperties(chartPanel, chartPanel.lastEventX,
                                        chartPanel.lastEventX)) {
            return;
        }

        signalChartChanged();


        if (annotation instanceof WayPoint) {
            try {
                setLocationPositions();
            } catch (Exception exc) {
                LogUtil.logException("Setting location positions", exc);
            }
        }
    }



    /**
     * Remove the selected annotations in the list.
     *
     * @param annotations  The list of annotations
     */
    public void removeSelectedAnnotations(List annotations) {
        removeAnnotations(getSelected(annotations));
    }

    /**
     * Callback method for receiving notification of a mouse click on a chart.
     *
     * @param event  information about the event.
     *
     * @return Did we handle this event
     */
    public boolean chartPanelMousePressed(MouseEvent event) {
        if (SwingUtilities.isRightMouseButton(event)) {
            closestAnnotation = findClosestAnnotation(getAllAnnotations(),
                    event.getX(), event.getY(), false, false);
        }
        return EVENT_PASSON;
    }



    /**
     * Callback method for receiving notification of a mouse click on a chart.
     *
     * @param event  information about the event.
     *
     * @return Did we handle this event
     */
    public boolean chartPanelMouseClicked(MouseEvent event) {
        if (SwingUtilities.isRightMouseButton(event)) {
            closestAnnotation = findClosestAnnotation(getAllAnnotations(),
                    event.getX(), event.getY(), false, false);
            return EVENT_PASSON;
        }

        if (event.getClickCount() <= 1) {
            ChartAnnotation annotation =
                findClosestAnnotation(getAllAnnotations(), event.getX(),
                                      event.getY(), true,
                                      event.isShiftDown());
            if (annotation == null) {
                return false;
            }
            signalChartChanged();
            return EVENT_DONTPASSON;
        }


        try {
            if (isOnBottomDomainAxis(event)) {
                WayPoint waypoint =
                    new WayPoint(getDomainValue(event.getX()), this);
                waypoint.addPropertyChangeListener(this);
                if ( !waypoint.showProperties(chartPanel, event.getX(),
                        event.getY())) {
                    return EVENT_DONTPASSON;
                }
                wayPoints.add(waypoint);
                setSelectedAnnotation(waypoint, wayPoints,
                                      event.isShiftDown());
                setLocationPositions();
                plot.addAnnotation(waypoint);
            } else if (isOnLeftRangeAxis(event)) {
                //                System.err.println("new x/y:" + event.getX() +"/" +event.getY());
                RangeFilter rangeFilter =
                    new RangeFilter(getRangeValue(event.getY()), this);
                if ( !rangeFilter.showProperties(chartPanel, event.getX(),
                        event.getY())) {
                    return EVENT_DONTPASSON;
                }
                rangeFilters.add(rangeFilter);


                setSelectedAnnotation(rangeFilter, rangeFilters,
                                      event.isShiftDown());
                plot.addAnnotation(rangeFilter);

                if (true || event.isShiftDown()) {
                    RangeFilter attached = rangeFilter.doMakeAttached(event);
                    rangeFilters.add(attached);
                    plot.addAnnotation(attached);
                }
                rangeFiltersChanged();
            } else {
                return EVENT_PASSON;
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating wayPoints", exc);
        }
        return EVENT_DONTPASSON;
    }

    /**
     * Get the domain value of the x position
     *
     * @param x  The x position
     *
     * @return Domain value
     */
    public double getDomainValue(int x) {
        return plot.getDomainAxis().java2DToValue(x,
                getChartPanel().getScreenDataArea(),
                plot.getDomainAxisEdge());
    }


    /**
     * Get the range value of y
     *
     * @param y  The y position
     *
     * @return Range value
     */
    public double getRangeValue(int y) {
        return plot.getRangeAxis().java2DToValue(y,
                getChartPanel().getScreenDataArea(), plot.getRangeAxisEdge());
    }


    /**
     * Set the dots in the main display
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void setLocationPositions()
            throws VisADException, RemoteException {
        if ( !showDots || (wayPoints.size() == 0)) {
            return;
        }
        if (dotsHolder == null) {
            dotsHolder = new CompositeDisplayable();
            getDisplayControl().addDisplayable(dotsHolder);
            if ( !showDots) {
                dotsHolder.setVisible(false);
            }
        }
        dotsHolder.setDisplayInactive();
        while (dotsHolder.displayableCount() < wayPoints.size()) {
            PickableLineDrawing lineDrawing = new PickableLineDrawing("Line");
            lineDrawing.setColor(Color.blue);
            lineDrawing.setPointSize(5);
            dotsHolder.addDisplayable(lineDrawing);
        }
        while (dotsHolder.displayableCount() > wayPoints.size()) {
            dotsHolder.removeDisplayable(dotsHolder.lastDisplayable());
        }



        List locs = new ArrayList();
        for (int i = 0; i < wayPoints.size(); i++) {
            WayPoint waypoint = (WayPoint) wayPoints.get(i);
            PickableLineDrawing lineDrawing =
                (PickableLineDrawing) dotsHolder.getDisplayable(i);
            lineDrawing.setColor(waypoint.getColorToUse());
            int idx = findTimeIndex(waypoint.getDomainValue());
            if (idx >= 0) {
                EarthLocationTuple[] latLonsAlts = { new EarthLocationTuple(
                                                       lats[idx], lons[idx],
                                                       alts[idx]) };
                lineDrawing.setData(Util.indexedField(latLonsAlts, false));
            }
        }

        if (wayPoints.size() == 0) {
            dotsHolder.setVisible(false);
        } else {
            dotsHolder.setVisible(true);
        }

        dotsHolder.setDisplayActive();

    }


    /**
     * Create a list of time ranges from the segments
     *
     * @return List of ranges
     */
    public List getTimeRanges() {
        if (times == null) {
            return null;
        }
        List result = new ArrayList();
        for (int i = 0; i < segments.size(); i++) {
            TrackSegment segment = (TrackSegment) segments.get(i);
            result.add(
                new ucar.unidata.util.Range(
                    segment.getLeft().getDomainValue(),
                    segment.getRight().getDomainValue()));
        }

        for (int i = 0; i < wayPoints.size(); i++) {
            WayPoint wayPoint = (WayPoint) wayPoints.get(i);
            double   span     = wayPoint.getMinutesSpan();
            if (span <= 0.0) {
                continue;
            }
            result.add(new ucar.unidata.util.Range(wayPoint.getDomainValue()
                    - span * 30000.0, wayPoint.getDomainValue()
                                      + span * 30000.0));
        }

        //        System.err.println("ranges:" + results);
        if (result.size() == 0) {
            return null;
        }
        return result;
    }



    /**
     * Animation in main display changed. Some charts show this
     *
     * @param time  the animation time
     */
    public void setTimeFromAnimation(Real time) {
        if (getShowTime()) {
            if (System.currentTimeMillis() - lastTimeWeDrove <= 1000) {
                return;
            }
        }
        setTime(time.getValue() * 1000, false);
    }



    /**
     * Set the time we're at
     *
     * @param value time
     * @param andDriveAnimation Set the time in the animation widget
     */
    protected void setTime(double value, boolean andDriveAnimation) {
        if (inSetTime) {
            return;
        }
        inSetTime = true;
        if (timeWayPoint == null) {
            setTimeWayPoint(new WayPoint(value, this));
            timeWayPoint.setIsForAnimation(true);
            timeWayPoint.setName("Time");
            wayPoints.add(timeWayPoint);
            plot.addAnnotation(timeWayPoint);
        }
        timeWayPoint.setDomainValue(value);
        ValueAxis axis = (ValueAxis) plot.getDomainAxis();
        axis.centerRange(value);
        firePropertyChange(PROP_TIMERANGE, null, segments);

        if (andDriveAnimation && getDriveTime()
                && (animationWidget != null)) {
            lastTimeWeDrove = System.currentTimeMillis();
            animationWidget.setTimeFromUser(new Real(RealType.Time,
                    value / 1000));
        }
        inSetTime = false;
    }



    /**
     * Handle the event
     *
     * @param event the event
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(WayPoint.PROP_WAYPOINTVALUE)) {
            WayPoint source = (WayPoint) event.getSource();
            if (source.canPlaySound()) {
                playSound(source);
            }
            if (getDriveTime() && (animationWidget != null)) {
                lastTimeWeDrove = System.currentTimeMillis();
                animationWidget.setTimeFromUser(new Real(RealType.Time,
                        source.getDomainValue() / 1000));
            }
        } else if (event.getPropertyName().equals(PROP_SELECTEDTIME)) {
            Double dttm = (Double) event.getNewValue();
            if (dttm != null) {
                setTime(dttm.doubleValue(), true);
            }
        }
    }


    /**
     * Zoom the domain axis
     *
     * @param segment  The track segment
     */
    public void zoomTo(TrackSegment segment) {
        ValueAxis axis = (ValueAxis) plot.getDomainAxis();
        org.jfree.data.Range range =
            new org.jfree.data.Range(segment.getLeft().getDomainValue(),
                                     segment.getRight().getDomainValue());
        axis.setRange(range);
    }

    /**
     * Zoom the range axis
     *
     * @param rangeFilter  The range filter
     */
    public void zoomTo(RangeFilter rangeFilter) {
        RangeFilter other = rangeFilter.getAttached();
        double min = Math.min(other.getRangeValue(),
                              rangeFilter.getRangeValue());
        double max = Math.max(other.getRangeValue(),
                              rangeFilter.getRangeValue());
        ValueAxis            axis  = (ValueAxis) plot.getRangeAxis();
        org.jfree.data.Range range = new org.jfree.data.Range(min, max);
        axis.setRange(range);
    }


    /**
     * Center the domain axis about
     *
     * @param wayPoint  The way point
     */
    public void centerOn(WayPoint wayPoint) {
        ValueAxis axis = (ValueAxis) plot.getDomainAxis();
        axis.centerRange(wayPoint.getDomainValue());
    }


    /**
     * Center the range axis
     *
     * @param rangeFilter  The range filter
     */
    public void centerOn(RangeFilter rangeFilter) {
        ValueAxis axis = (ValueAxis) plot.getRangeAxis();
        axis.centerRange(rangeFilter.getRangeValue());

    }



    /**
     * Find the index in the times list of the given time
     *
     * @param value The time value
     *
     * @return The closest index
     */
    private int findTimeIndex(double value) {
        int idx = -1;
        if (times == null) {
            return -1;
        }
        double minDistance = 0;
        double f           = 0.0;
        for (int i = 0; i < times.length; i++) {
            double distance = Math.abs(value - times[i]);
            if ((i == 0) || (distance < minDistance)) {
                f           = times[i];
                minDistance = distance;
                idx         = i;
            }
        }
        //      System.err.println(f+" distance:" + minDistance +" value:" + value +" idx:" + idx + " size:" + times.length);
        return idx;
    }


    /**
     * Handle event in chart
     *
     * @param event The event
     *
     * @return Did we handle this event
     */
    public boolean chartPanelMouseReleased(MouseEvent event) {


        if (SwingUtilities.isRightMouseButton(event)) {
            return EVENT_PASSON;
        }
        closestAnnotation = null;


        dragging          = false;
        if (draggedAnnotation != null) {
            if ((draggedAnnotation instanceof WayPoint)
                    || (draggedAnnotation instanceof TrackSegment)) {
                firePropertyChange(PROP_TIMERANGE, null, segments);
            }
            if (getDriveTime() && (animationWidget != null)) {
                if (draggedAnnotation instanceof WayPoint) {
                    lastTimeWeDrove = System.currentTimeMillis();
                    double value =
                        ((WayPoint) draggedAnnotation).getDomainValue();
                    animationWidget.setTimeFromUser(new Real(RealType.Time,
                            value / 1000));
                }
            }
        }
        draggedAnnotation = null;
        if (isOnAxis(event) && SwingUtilities.isRightMouseButton(event)) {
            return EVENT_DONTPASSON;
        }

        return EVENT_PASSON;
    }




    /**
     * Is mouse on an axis_
     *
     * @param mouseEvent the event
     *
     * @return On axis
     */
    private boolean isOnAxis(MouseEvent mouseEvent) {
        return isOnBottomDomainAxis(mouseEvent)
               || isOnTopDomainAxis(mouseEvent)
               || isOnLeftRangeAxis(mouseEvent);
    }

    /**
     * Is mouse on bottom axis
     *
     * @param mouseEvent the event
     *
     * @return on bottom axis
     */
    private boolean isOnBottomDomainAxis(MouseEvent mouseEvent) {
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        if (mouseEvent.getX() < dataArea.getX()) {
            return false;
        }
        if (mouseEvent.getX() > dataArea.getX() + dataArea.getWidth()) {
            return false;
        }

        double bottom = dataArea.getY() + dataArea.getHeight();
        return mouseEvent.getY() >= bottom - 20;
    }


    /**
     * Is mouse on top axis
     *
     * @param mouseEvent the event
     *
     * @return On top axis
     */
    private boolean isOnTopDomainAxis(MouseEvent mouseEvent) {
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        if (mouseEvent.getX() < dataArea.getX()) {
            return false;
        }
        if (mouseEvent.getX() > dataArea.getX() + dataArea.getWidth()) {
            return false;
        }
        double top = dataArea.getY();
        return mouseEvent.getY() <= top + 20;
    }


    /**
     * On left axis
     *
     * @param mouseEvent the event
     *
     * @return on left axis
     */
    private boolean isOnLeftRangeAxis(MouseEvent mouseEvent) {
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        if (mouseEvent.getY() < dataArea.getY()) {
            return false;
        }
        if (mouseEvent.getY() > dataArea.getY() + dataArea.getHeight()) {
            return false;
        }
        //        System.err.println("mouse:" + mouseEvent.getX() +" da:" + dataArea);

        double left = dataArea.getX();
        if (mouseEvent.getX() < left) {
            return true;
        }
        if (mouseEvent.getX() > left + 20) {
            return false;
        }
        return true;
    }




    /**
     * Add to the properties components
     *
     * @param comps  List of components
     * @param tabIdx Which tab in the properties dialog
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx != 1) {
            return;
        }
        showDotsCbx = new JCheckBox("", showDots);
        comps.add(GuiUtils.rLabel("Show Dots: "));
        comps.add(GuiUtils.left(showDotsCbx));
    }


    /**
     * Apply properties
     *
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        setShowDots(showDotsCbx.isSelected());

        return true;
    }

    /**
     * The annotaiton has changed. If its a WayPoint then fire the timerange property
     * change.
     *
     * @param chartAnnotation The annotation that changed
     */
    public void annotationChanged(ChartAnnotation chartAnnotation) {
        super.annotationChanged(chartAnnotation);
        if (chartAnnotation instanceof WayPoint) {
            firePropertyChange(PROP_TIMERANGE, null, segments);
        }
    }


    /**
     * Add a segment between the 2 waypoints
     *
     * @param wps 2 waypoints
     */
    public void addSegment(WayPoint[] wps) {
        TrackSegment segment = new TrackSegment(wps[0], wps[1], this);
        if ( !segment.showProperties(chartPanel, chartPanel.lastEventX,
                                     chartPanel.lastEventX)) {
            return;
        }

        segments.add(segment);
        plot.addAnnotation(segment);
    }

    /**
     * Is it ok to draw the annotation
     *
     * @param annotation The annotation
     *
     * @return ok to draw
     */
    public boolean okToDraw(ChartAnnotation annotation) {
        return times != null;
    }

    /**
     * Get list of selected annotations
     *
     * @param annotations  The list of annotations
     *
     * @return selected annotations in list
     */
    public List getSelected(List annotations) {
        List selected = new ArrayList();
        for (int i = 0; i < annotations.size(); i++) {
            ChartAnnotation annotation = (ChartAnnotation) annotations.get(i);
            if (annotation.getSelected()) {
                selected.add(annotation);
            }
        }
        return selected;
    }


    /**
     * Get the popup menu items
     *
     * @param items list to add to
     *
     * @return the list with items from this
     */
    protected List getPopupMenuItems(List items) {

        List selectedWaypoints = getSelected(wayPoints);


        if (closestAnnotation != null) {
            items.add(
                GuiUtils.makeMenuItem(
                    "Edit " + closestAnnotation.getTypeName() + ": "
                    + closestAnnotation.getName(), this, "editAnnotation",
                        closestAnnotation));
            items.add(
                GuiUtils.makeMenuItem(
                    "Remove " + closestAnnotation.getTypeName() + ": "
                    + closestAnnotation.getName(), this, "removeAnnotations",
                        Misc.newList(closestAnnotation)));
            if (closestAnnotation instanceof TrackSegment) {
                items.add(GuiUtils.makeMenuItem("Zoom Domain Axis To", this,
                        "zoomTo", closestAnnotation));

            }

            if (closestAnnotation instanceof WayPoint) {
                items.add(GuiUtils.makeMenuItem("Center Domain Axis To",
                        this, "centerOn", closestAnnotation));

            }
            if (closestAnnotation instanceof RangeFilter) {
                if (((RangeFilter) closestAnnotation).getAttached() != null) {
                    items.add(GuiUtils.makeMenuItem("Zoom Range Axis To",
                            this, "zoomTo", closestAnnotation));
                }
                items.add(GuiUtils.makeMenuItem("Center Range Axis To", this,
                        "centerOn", closestAnnotation));

            }

            items.add(GuiUtils.MENU_SEPARATOR);
        }

        if (selectedWaypoints.size() == 2) {
            items.add(
                GuiUtils.makeMenuItem(
                    "Add Segment", this, "addSegment",
                    new WayPoint[] { (WayPoint) selectedWaypoints.get(0),
                                     (WayPoint) selectedWaypoints.get(1) }));
            items.add(GuiUtils.MENU_SEPARATOR);
        }

        JMenu markers = null;
        if (wayPoints.size() != 0) {
            if (markers == null) {
                markers = new JMenu("Markers");
                items.add(markers);
            }
            JMenu menu = new JMenu("Way Points");
            markers.add(menu);
            menu.add(GuiUtils.makeMenuItem("Remove All Way Points", this,
                                           "removeAnnotations", wayPoints));
            if (selectedWaypoints.size() > 0) {
                menu.add(GuiUtils.makeMenuItem("Remove Selected Way Points",
                        this, "removeSelectedAnnotations", wayPoints));
            }

            JMenu editMenu = new JMenu("Edit Way Points");
            menu.add(editMenu);
            for (int i = 0; i < wayPoints.size(); i++) {
                WayPoint waypoint = (WayPoint) wayPoints.get(i);
                editMenu.add(GuiUtils.makeMenuItem("#" + (i + 1) + " "
                        + waypoint.getName() + " ", this, "editAnnotation",
                            waypoint));
            }

        }



        if (segments.size() != 0) {
            if (markers == null) {
                markers = new JMenu("Markers");
                items.add(markers);
            }
            JMenu menu = new JMenu("Segments");
            markers.add(menu);
            menu.add(GuiUtils.makeMenuItem("Remove All Segments", this,
                                           "removeAnnotations", segments));
            List selected = getSelected(segments);
            if (selected.size() > 0) {
                menu.add(GuiUtils.makeMenuItem("Remove Selected Segments",
                        this, "removeSelectedAnnotations", segments));
            }



            JMenu editMenu = new JMenu("Edit Segments");
            menu.add(editMenu);
            for (int i = 0; i < segments.size(); i++) {
                TrackSegment segment = (TrackSegment) segments.get(i);
                editMenu.add(GuiUtils.makeMenuItem("#" + (i + 1) + " "
                        + segment.getName() + " ", this, "editAnnotation",
                            segment));
            }


        }



        if (rangeFilters.size() != 0) {
            if (markers == null) {
                markers = new JMenu("Markers");
                items.add(markers);
            }
            JMenu menu = new JMenu("Range Filters");
            markers.add(menu);
            menu.add(GuiUtils.makeMenuItem("Remove All Range Filters", this,
                                           "removeAnnotations",
                                           rangeFilters));

            List selected = getSelected(rangeFilters);
            if (selected.size() > 0) {
                menu.add(
                    GuiUtils.makeMenuItem(
                        "Remove Selected Range Filters", this,
                        "removeSelectedAnnotations", rangeFilters));
            }


            JMenu editMenu = new JMenu("Edit Range Filters");
            menu.add(editMenu);
            for (int i = 0; i < rangeFilters.size(); i++) {
                ChartAnnotation annotation =
                    (ChartAnnotation) rangeFilters.get(i);
                editMenu.add(GuiUtils.makeMenuItem("#" + (i + 1) + " "
                        + annotation.getName() + " ", this, "editAnnotation",
                            annotation));
            }
        }

        return super.getPopupMenuItems(items);
    }



    /**
     * Find the closest annotation
     *
     * @param list List of annotations
     * @param x  The x position
     * @param y  The y position
     * @param andSetSelected If true set the closest as selected
     * @param addToSelected If true add to selected. Else remove selection set (if andSetSelected
     * is true.
     *
     * @return Closest annotation or null
     */
    public ChartAnnotation findClosestAnnotation(List list, int x, int y,
            boolean andSetSelected, boolean addToSelected) {
        double          minDistance = 20;
        ChartAnnotation closest     = null;
        for (int i = 0; i < list.size(); i++) {
            ChartAnnotation annotation = (ChartAnnotation) list.get(i);
            double          distance   = annotation.distance(x, y);
            if (distance < minDistance) {
                minDistance = distance;
                closest     = annotation;
            }
        }
        if (andSetSelected) {
            setSelectedAnnotation(closest, list, addToSelected);
        }
        return closest;
    }


    /**
     * We can set the time in the main display
     *
     * @return can set the time in the main display
     */
    protected boolean canDoDriveTime() {
        return true;
    }

    /**
     * Can this chart use time selects
     * This is used to determine whether the checkbox should be shown
     * in the menus
     *
     * @return     Can this chart use time select
     */
    protected boolean canDoTimeSelect() {
        return true;
    }




    /**
     * The timeseries can have jython applied to it
     *
     * @return Can this chart have jython applied to it
     */
    protected boolean canDoJython() {
        return true;
    }

    /**
     * Add the state of this chart to the interpreter
     *
     * @param interpreter The interpreter to initialize
     */
    protected void initializeJython(PythonInterpreter interpreter) {
        super.initializeJython(interpreter);
        interpreter.set("waypoints", wayPoints);
        interpreter.set("segments", segments);
        interpreter.set("rangefilters", rangeFilters);
        interpreter.set("rangefilters", rangeFilters);
    }



    /**
     * Can we do the data area colors in the properties dialog
     *
     * @return can do colors
     */
    protected boolean canDoColors() {
        return true;
    }


    /**
     * Can we add time subsetting to this chart
     *
     * @return false
     */
    protected boolean canDoTimeFilters() {
        return false;
    }

    /**
     * Show colors in fields properties
     *
     * @return can do colors
     */
    public boolean canDoWrapperColor() {
        return true;
    }

    /**
     *  SHow side menu in fields properties
     *
     * @return can do sides
     */
    public boolean canDoWrapperSide() {
        return true;
    }



    //TODO: Fix the range filters to work with multiple range axis

    /**
     * Are the values ok to render
     *
     *
     * @param dataset Which data set
     * @param domainValue The domain value
     * @param rangeValue The range value
     *
     * @return show values
     */
    public boolean valuesOk(int dataset, double domainValue,
                            double rangeValue) {
        return rangeValueOk(dataset, rangeValue);
    }



    /**
     * Get the List of range values for the given dataset
     *
     * @param dataset The data set
     *
     * @return List of range values
     */
    public List getRangeValues(int dataset) {
        List l = new ArrayList();
        for (int i = 0; i < rangeFilters.size(); i++) {
            RangeFilter rangeFilter = (RangeFilter) rangeFilters.get(i);
            l.add(rangeFilter);
        }
        return l;
    }



    /**
     * Does the value apss the range filters
     *
     *
     * @param dataset  Which dataset
     * @param value The range value
     *
     * @return show values
     */
    public boolean rangeValueOk(int dataset, double value) {
        for (int i = 0; i < rangeFilters.size(); i++) {
            if ( !((RangeFilter) rangeFilters.get(i)).valueOk(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all of the annotations from the different lists
     *
     * @return all annotations
     */
    private List getAllAnnotations() {
        List all = new ArrayList(wayPoints);
        all.addAll(segments);
        all.addAll(rangeFilters);
        return all;
    }



    /**
     * Handle event in chart
     *
     * @param event The event
     *
     * @return Did we handle this event
     */
    public boolean chartPanelMouseDragged(MouseEvent event) {


        if (SwingUtilities.isRightMouseButton(event)) {
            return EVENT_DONTPASSON;
        }

        closestAnnotation = null;
        //Ignore shift down drag.
        if (event.isShiftDown()) {
            return EVENT_PASSON;
        }

        if (dragging && (draggedAnnotation == null)) {
            return EVENT_PASSON;
        }
        dragging = true;
        if (draggedAnnotation == null) {
            draggedAnnotation = findClosestAnnotation(getAllAnnotations(),
                    event.getX(), event.getY(), true, event.isShiftDown());
        }
        if (draggedAnnotation == null) {
            return EVENT_PASSON;
        }

        draggedAnnotation.setPosition(event);
        if (draggedAnnotation instanceof RangeFilter) {
            rangeFiltersChanged();
        }
        if ((draggedAnnotation instanceof WayPoint)
                || (draggedAnnotation instanceof TrackSegment)) {
            Rectangle2D r = getChartPanel().getScreenDataArea();
            if (event.getX() < r.getX()) {
                panPlot(false, 0.02);
            } else if (event.getX() > r.getX() + r.getWidth()) {
                panPlot(true, 0.02);
            }


        }

        signalChartChanged();
        try {
            setLocationPositions();
        } catch (Exception exc) {
            LogUtil.logException("Setting locations", exc);
        }
        return EVENT_DONTPASSON;

    }



    /**
     * Play the sound for the waypoint if it is enabled
     *
     * @param wp The waypoint to play a sound with
     */
    private void playSound(WayPoint wp) {
        if ( !wp.canPlaySound()) {
            return;
        }
        MidiManager    midiManager = wp.getMidiManager();
        MidiProperties mp          = wp.getMidiProperties();
        int            idx         = findTimeIndex(wp.getDomainValue());
        double[]       darray      = (double[]) datas.get(0);
        double         d           = darray[idx];
        if ( !rangeValueOk(0, d)) {
            return;
        }
        ucar.unidata.util.Range range =
            (ucar.unidata.util.Range) ranges.get(0);
        int note = (int) (mp.getLowNote()
                          + (range.getPercent(d)
                             * (mp.getHighNote() - mp.getLowNote())));
        //      System.err.println ("note:" + note);
        midiManager.play(note, 500);
    }




    /**
     * Get the tool tip text
     *
     * @param event The event
     *
     * @return tool tip
     */
    public String chartPanelGetToolTipText(MouseEvent event) {
        ChartAnnotation annotation =
            findClosestAnnotation(getAllAnnotations(), event.getX(),
                                  event.getY(), false, false);
        if (annotation == null) {
            return null;
        }
        return annotation.getToolTipText();
    }



    /**
     * Range filters changed
     */
    public void rangeFiltersChanged() {
        if ((rangeFilters.size() == 0) || (dataChoiceWrappers.size() == 0)) {
            return;
        }
        DataChoice dataChoice =
            ((DataChoiceWrapper) dataChoiceWrappers.get(0)).getDataChoice();
        double min            = Double.NEGATIVE_INFINITY;
        double max            = Double.POSITIVE_INFINITY;
        int    greaterThanCnt = 0;
        int    lessThanCnt    = 0;

        for (int i = 0; i < rangeFilters.size(); i++) {
            RangeFilter rangeFilter = (RangeFilter) rangeFilters.get(i);
            if (rangeFilter.getType() == RangeFilter.TYPE_LESSTHAN) {
                if (lessThanCnt == 0) {
                    max = rangeFilter.getRangeValue();
                } else {
                    max = Math.max(max, rangeFilter.getRangeValue());
                }
                lessThanCnt++;
            } else {
                if (greaterThanCnt == 0) {
                    min = rangeFilter.getRangeValue();
                } else {
                    min = Math.min(min, rangeFilter.getRangeValue());
                }
                greaterThanCnt++;
            }
        }
        getDisplayControl().doShare(DisplayControlImpl.SHARE_SELECTRANGE,
                                    new Object[] { dataChoice,
                new ucar.unidata.util.Range(min, max) });
    }


    /**
     * Clear out list of annotations
     *
     * @param l list
     */
    private void clearAnnotations(List l) {
        for (int i = 0; i < l.size(); i++) {
            ChartAnnotation anno = (ChartAnnotation) l.get(i);
            anno.doRemove();
        }
        l.clear();
    }



    /**
     * remove me
     */
    public void doRemove() {
        super.doRemove();

        clearAnnotations(segments);
        clearAnnotations(wayPoints);
        clearAnnotations(rangeFilters);


        firePropertyChange(PROP_TIMERANGE, null, segments);


        if ((rangeFilters.size() == 0) || (dataChoiceWrappers.size() == 0)) {
            return;
        }
        DataChoice dataChoice =
            ((DataChoiceWrapper) dataChoiceWrappers.get(0)).getDataChoice();

        getDisplayControl().doShare(DisplayControlImpl.SHARE_SELECTRANGE,
                                    new Object[] { dataChoice,
                new ucar.unidata.util.Range(Double.NEGATIVE_INFINITY,
                                            Double.POSITIVE_INFINITY) });
    }


    /**
     * Do final initialization
     */
    public void initDone() {
        super.initDone();
        Misc.runInABit(2000, this, "rangeFiltersChanged", null);
    }



    /**
     *  Set the WayPoints property.
     *
     *  @param value The new value for WayPoints
     */
    public void setWayPoints(List value) {
        wayPoints = value;
    }

    /**
     *  Get the WayPoints property.
     *
     *  @return The WayPoints
     */
    public List getWayPoints() {
        return wayPoints;
    }


    /**
     * Set the Segments property.
     *
     * @param value The new value for Segments
     */
    public void setSegments(List value) {
        segments = value;
    }

    /**
     * Get the Segments property.
     *
     * @return The Segments
     */
    public List getSegments() {
        return segments;
    }

    /**
     * Set the RangeFilters property.
     *
     * @param value The new value for RangeFilters
     */
    public void setRangeFilters(List value) {
        rangeFilters = value;
    }

    /**
     * Get the RangeFilters property.
     *
     * @return The RangeFilters
     */
    public List getRangeFilters() {
        return rangeFilters;
    }


    /**
     * Set the TimeWayPoint property.
     *
     * @param value The new value for TimeWayPoint
     */
    public void setTimeWayPoint(WayPoint value) {
        if (timeWayPoint != null) {
            timeWayPoint.removePropertyChangeListener(this);
        }
        timeWayPoint = value;
        if (timeWayPoint != null) {
            timeWayPoint.addPropertyChangeListener(this);
        }
    }

    /**
     * Get the TimeWayPoint property.
     *
     * @return The TimeWayPoint
     */
    public WayPoint getTimeWayPoint() {
        return timeWayPoint;
    }

    /**
     * Set the ShowDots property.
     *
     * @param value The new value for ShowDots
     */
    public void setShowDots(boolean value) {
        if (value == showDots) {
            return;
        }
        showDots = value;
        if (dotsHolder != null) {
            try {
                dotsHolder.setVisible(value);
                setLocationPositions();
            } catch (Exception exc) {
                LogUtil.logException("Setting locations", exc);
            }
        }
    }

    /**
     * Get the ShowDots property.
     *
     * @return The ShowDots
     */
    public boolean getShowDots() {
        return showDots;
    }






}

