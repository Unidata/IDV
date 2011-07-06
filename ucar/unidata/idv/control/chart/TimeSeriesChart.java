/*
 * $Id: TimeSeriesChart.java,v 1.12 2007/04/16 21:32:11 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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


import org.itc.idv.math.SunriseSunsetCollector;


import org.jfree.chart.*;
import org.jfree.chart.annotations.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.entity.*;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.general.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RefineryUtilities;


import ucar.unidata.data.DataAlias;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.data.point.*;
import ucar.unidata.data.storm.*;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.IdvPreferenceManager;

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.ProbeRowInfo;
import ucar.unidata.idv.control.chart.LineState;
import ucar.unidata.idv.ui.IdvTimeline;


import ucar.unidata.ui.symbol.*;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;

import ucar.visad.Util;

import ucar.visad.display.Animation;

import visad.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import java.rmi.RemoteException;

import java.text.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import javax.swing.*;


/**
 * A time series chart
 *
 * @author MetApps Development Team
 * @version $Revision: 1.12 $
 */

public class TimeSeriesChart extends XYChartManager {

    /** macro for substituting the param name into the chart line name */
    public static final String MACRO_PARAMETER = "%parameter%";

    /** Shows time */
    private XYAnnotation animationTimeAnnotation;

    /** Should show time */
    private boolean showAnimationTime;

    /** Clock to draw_ */
    private static Image clockImage;

    /** format */
    NumberFormat numberFormat;

    /** sunrise dates */
    private List sunriseDates;

    /** sunrise location */
    private LatLonPoint sunriseLocation;

    /** last start date */
    private Date lastStartDate;

    /** last end date */
    private Date lastEndDate;

    /** date format */
    private String dateFormat;


    /**
     * ctor
     */
    public TimeSeriesChart() {
        showAnimationTime(showAnimationTime);
    }


    /**
     * Default constructor.
     *
     * @param control control that this is associated with
     */
    public TimeSeriesChart(DisplayControlImpl control) {
        this(control, "Time Series");
    }


    /**
     * Default constructor.
     *
     * @param control control that this is associated with
     * @param  chartName name for the chart
     */
    public TimeSeriesChart(DisplayControlImpl control, String chartName) {
        super(control, chartName);
    }



    /**
     * Make the plot
     *
     * @return The plot_
     */
    public Plot doMakePlot() {

        IdvPreferenceManager pref =
            control.getControlContext().getIdv().getPreferenceManager();
        TimeZone   timeZone  = pref.getDefaultTimeZone();
        NumberAxis valueAxis = new FixedWidthNumberAxis("");
        final SimpleDateFormat sdf = new SimpleDateFormat(((dateFormat
                                         != null)
                ? dateFormat
                : pref.getDefaultDateFormat()));
        sdf.setTimeZone(timeZone);
        DateAxis timeAxis = new DateAxis("Time (" + timeZone.getID() + ")",
                                         timeZone) {

            protected List xxxxxrefreshTicksHorizontal(Graphics2D g2,
                    Rectangle2D dataArea, RectangleEdge edge) {

                List ticks = super.refreshTicksHorizontal(g2, dataArea, edge);

                List<Tick> result        = new java.util.ArrayList<Tick>();

                Font       tickLabelFont = getTickLabelFont();
                g2.setFont(tickLabelFont);

                if (isAutoTickUnitSelection()) {
                    selectAutoTickUnit(g2, dataArea, edge);
                }

                DateTickUnit unit      = getTickUnit();
                Date         tickDate  =
                    calculateLowestVisibleTickValue(unit);
                Date         upperDate = getMaximumDate();

                Date         firstDate = null;
                while (tickDate.before(upperDate)) {

                    if ( !isHiddenValue(tickDate.getTime())) {
                        // work out the value, label and position
                        String     tickLabel;
                        DateFormat formatter = getDateFormatOverride();
                        if (firstDate == null) {
                            if (formatter != null) {
                                tickLabel = formatter.format(tickDate);
                            } else {
                                tickLabel =
                                    getTickUnit().dateToString(tickDate);
                            }
                            firstDate = tickDate;
                        } else {
                            double msdiff = tickDate.getTime()
                                            - firstDate.getTime();
                            int hours = (int) (msdiff / 1000 / 60 / 60);
                            tickLabel = hours + "H";
                        }
                        //                tickLabel = tickLabel;
                        TextAnchor anchor         = null;
                        TextAnchor rotationAnchor = null;
                        double     angle          = 0.0;
                        if (isVerticalTickLabels()) {
                            anchor         = TextAnchor.CENTER_RIGHT;
                            rotationAnchor = TextAnchor.CENTER_RIGHT;
                            if (edge == RectangleEdge.TOP) {
                                angle = Math.PI / 2.0;
                            } else {
                                angle = -Math.PI / 2.0;
                            }
                        } else {
                            if (edge == RectangleEdge.TOP) {
                                anchor         = TextAnchor.BOTTOM_CENTER;
                                rotationAnchor = TextAnchor.BOTTOM_CENTER;
                            } else {
                                anchor         = TextAnchor.TOP_CENTER;
                                rotationAnchor = TextAnchor.TOP_CENTER;
                            }
                        }

                        Tick tick = new DateTick(tickDate, tickLabel, anchor,
                                        rotationAnchor, angle);
                        result.add(tick);
                        tickDate = unit.addToDate(tickDate, getTimeZone());
                    } else {
                        tickDate = unit.rollDate(tickDate, getTimeZone());
                        continue;
                    }

                    // could add a flag to make the following correction optional...
                    switch (unit.getUnit()) {

                      case (DateTickUnit.MILLISECOND) :
                      case (DateTickUnit.SECOND) :
                      case (DateTickUnit.MINUTE) :
                      case (DateTickUnit.HOUR) :
                      case (DateTickUnit.DAY) :
                          break;

                      case (DateTickUnit.MONTH) :
                          tickDate =
                              calculateDateForPositionX(new Month(tickDate,
                                  getTimeZone()), getTickMarkPosition());
                          break;

                      case (DateTickUnit.YEAR) :
                          tickDate =
                              calculateDateForPositionX(new Year(tickDate,
                                  getTimeZone()), getTickMarkPosition());
                          break;

                      default :
                          break;

                    }

                }
                return result;

            }

            private Date calculateDateForPositionX(RegularTimePeriod period,
                    DateTickMarkPosition position) {

                if (position == null) {
                    throw new IllegalArgumentException(
                        "Null 'position' argument.");
                }
                Date result = null;
                if (position == DateTickMarkPosition.START) {
                    result = new Date(period.getFirstMillisecond());
                } else if (position == DateTickMarkPosition.MIDDLE) {
                    result = new Date(period.getMiddleMillisecond());
                } else if (position == DateTickMarkPosition.END) {
                    result = new Date(period.getLastMillisecond());
                }
                return result;

            }


        };
        timeAxis.setDateFormatOverride(sdf);

        final XYPlot[] xyPlotHolder = { null };

        xyPlotHolder[0] = new MyXYPlot(new TimeSeriesCollection(), timeAxis,
                                       valueAxis, null) {
            public void drawBackground(Graphics2D g2, Rectangle2D area) {
                super.drawBackground(g2, area);
                drawSunriseSunset(g2, xyPlotHolder[0], area);
            }
        };

        if (animationTimeAnnotation != null) {
            xyPlotHolder[0].addAnnotation(animationTimeAnnotation);
        }
        return xyPlotHolder[0];

    }

    /**
     * Show time
     *
     * @param value show time
     */
    public void showAnimationTime(boolean value) {
        this.showAnimationTime = value;
        if (animationTimeAnnotation == null) {
            animationTimeAnnotation = new XYAnnotation() {
                public void draw(Graphics2D g2, XYPlot plot,
                                 Rectangle2D dataArea, ValueAxis domainAxis,
                                 ValueAxis rangeAxis, int rendererIndex,
                                 PlotRenderingInfo info) {
                    if (showAnimationTime) {
                        drawTime(g2, plot, dataArea, domainAxis, rangeAxis,
                                 rendererIndex, info);
                    }
                }
            };
            List plots = getPlots();
            for (int plotIdx = 0; plotIdx < plots.size(); plotIdx++) {
                ChartHolder chartHolder = (ChartHolder) plots.get(plotIdx);
                ((XYPlot) chartHolder.getPlot()).addAnnotation(
                    animationTimeAnnotation);
            }
        }
    }



    /**
     * Add the series
     *
     *
     * @param series The data
     * @param lineState describes how to draw the line
     * @param paramIdx which parameter
     * @param renderer renderer
     * @param rangeVisible  do we show range axis
     *
     * @return the newly created range axis
     */
    protected Axis addSeries(TimeSeries series, LineState lineState,
                             int paramIdx, XYItemRenderer renderer,
                             boolean rangeVisible) {

        return addSeries(series, lineState, paramIdx, renderer, rangeVisible,
                         true);
    }

    /**
     * Add the series
     *
     *
     * @param series The data
     * @param lineState describes how to draw the line
     * @param paramIdx which parameter
     * @param renderer renderer
     * @param rangeVisible  do we show range axis
     * @param addAxis include the axis
     *
     * @return the newly created range axis
     */
    protected Axis addSeries(TimeSeries series, LineState lineState,
                             int paramIdx, XYItemRenderer renderer,
                             boolean rangeVisible, boolean addAxis) {

        if (series instanceof MyTimeSeries) {
            ((MyTimeSeries) series).finish();
        }

        if (addAxis && (lineState.getRange() != null)) {
            addRange(lineState.getRange().getMin(),
                     lineState.getRange().getMax(),
                     "Fixed range from: " + lineState.getName());
        }

        if (numberFormat == null) {
            numberFormat = new DecimalFormat() {
                public StringBuffer format(double number,
                                           StringBuffer result,
                                           FieldPosition fieldPosition) {
                    String s = control.getDisplayConventions().format(number);
                    result.append(s);
                    return result;
                }
            };

        }


        String               name    = lineState.getName();
        Unit                 unit    = lineState.unit;
        TimeSeriesCollection dataset = new TimeSeriesCollection();
        dataset.setDomainIsPointsInTime(true);
        dataset.addSeries(series);


        String axisLabel = lineState.getAxisLabel();
        if (axisLabel == null) {
            axisLabel = name + ((unit != null)
                                ? " [" + unit + "]"
                                : "");
        }
        NumberAxis rangeAxis;

        if (lineState.getUseLogarithmicRange() && false) {
            rangeAxis = new FixedWidthLogarithmicAxis(axisLabel);
        } else {
            rangeAxis = new FixedWidthNumberAxis(axisLabel);
            ((NumberAxis) rangeAxis).setAutoRangeIncludesZero(
                lineState.getRangeIncludesZero());
        }

        //For now lets use the default number formatting for the range
        //        rangeAxis.setNumberFormatOverride(numberFormat);

        rangeAxis.setVisible(rangeVisible);


        ucar.unidata.util.Range r = lineState.getRange();
        if (r != null) {
            rangeAxis.setRange(new org.jfree.data.Range(r.getMin(),
                    r.getMax()));
        }

        if (renderer == null) {
            renderer = getRenderer(lineState, addAxis);
        }

        Paint c = lineState.getColor(paramIdx);
        rangeAxis.setLabelPaint(Color.black);
        renderer.setSeriesPaint(0, c);
        renderer.setSeriesStroke(0, lineState.getStroke());
        renderer.setSeriesVisibleInLegend(0, lineState.getVisibleInLegend());

        if ( !lineState.getAxisVisible()) {
            rangeAxis.setVisible(false);
        } else {
            rangeAxis.setVisible(addAxis);
        }

        ChartHolder  chartHolder = getChartHolder(lineState);


        AxisLocation side        = null;
        if (rangeAxis.isVisible()) {
            if (lineState.getSide() == LineState.SIDE_UNDEFINED) {
                if (chartHolder.lastSide == AxisLocation.TOP_OR_LEFT) {
                    side = AxisLocation.BOTTOM_OR_RIGHT;
                } else {
                    side = AxisLocation.TOP_OR_LEFT;
                }
            } else if (lineState.getSide() == LineState.SIDE_LEFT) {
                side = AxisLocation.TOP_OR_LEFT;
            } else {
                side = AxisLocation.BOTTOM_OR_RIGHT;
            }
            chartHolder.lastSide = side;
        }

        synchronized (MUTEX) {
            chartHolder.add(dataset, rangeAxis, renderer, side);
        }

        return rangeAxis;
    }



    /** for changing the data */
    long lastTime = -1;

    /** for changing the data */
    List<ProbeRowInfo> currentProbeData;

    /** for changing the data */
    boolean updatePending = false;

    /**
     * Set the samples from the probe
     *
     * @param rowInfos the data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setProbeSamples(List<ProbeRowInfo> rowInfos)
            throws VisADException, RemoteException {
        lastTime         = System.currentTimeMillis();
        currentProbeData = rowInfos;
        //        if(updatePending) return;
        updatePending = true;
        //        Misc.runInABit(500,this,"setProbeSamplesInner",null);
        synchronized (MUTEX) {
            settingData = true;
            setProbeSamplesInner();
        }
    }

    /**
     * init plot
     *
     * @param plot plot
     */
    protected void initPlot(Plot plot) {
        XYPlot xyPlot = (XYPlot) plot;
        int    count  = xyPlot.getDatasetCount();
        for (int i = 0; i < count; i++) {
            xyPlot.setDataset(i, null);
            xyPlot.setRenderer(i, null);
        }
        xyPlot.clearRangeAxes();
        XYDataset dummyDataset = getDummyDataset();
        ValueAxis rangeAxis    = new FixedWidthNumberAxis();
        xyPlot.setRangeAxis(0, rangeAxis, false);
        xyPlot.setDataset(0, dummyDataset);
        xyPlot.mapDatasetToRangeAxis(0, 0);
        xyPlot.setRenderer(0, new XYLineAndShapeRenderer());
    }


    /**
     * Set samples
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setProbeSamplesInner()
            throws VisADException, RemoteException {

        clearLineStates();
        updatePending = false;
        List<ProbeRowInfo> rowInfos = ((currentProbeData == null)
                                       ? null
                                       : new ArrayList<ProbeRowInfo>(
                                           currentProbeData));
        try {
            initCharts();
            if ((rowInfos != null) && (rowInfos.size() > 0)) {
                MyTimeSeries speedSeries    = null;
                MyTimeSeries dirSeries      = null;
                LineState    speedLineState = null;
                LineState    dirLineState   = null;
                Unit         speedUnit      = null;
                double
                    speedMin                = 0,
                    speedMax                = 0;
                double
                    dirMin                  = 0,
                    dirMax                  = 0;
                boolean polarWind           = true;

                int     speedIdx            = 0;
                int     dirIdx              = 0;


                for (int paramIdx = 0; paramIdx < rowInfos.size();
                        paramIdx++) {
                    ProbeRowInfo info      = rowInfos.get(paramIdx);
                    LineState    lineState = info.getLineState();
                    addLineState(lineState);

                    FieldImpl field = info.getPointSample();
                    if (field == null) {
                        continue;
                    }
                    boolean isTimeSequence = GridUtil.isTimeSequence(field);
                    if ( !isTimeSequence) {
                        continue;
                    }
                    lineState.unit = info.getUnit();
                    if (info.isPoint()) {
                        if ( !lineState.hasName()) {
                            lineState.setNameMacro(MACRO_PARAMETER);
                        }
                        String pointParam = info.getPointParameterName();
                        if (lineState.getNameMacro() != null) {
                            String macro = lineState.getNameMacro();
                            macro = macro.replace(MACRO_PARAMETER,
                                    pointParam);
                            lineState.setName(macro);
                        } else {
                            lineState.setName(pointParam);
                        }
                    } else {
                        if ( !lineState.hasName()) {
                            lineState.setNameMacro(MACRO_PARAMETER);
                        }
                        String paramName =
                            info.getDataInstance().getParamName();
                        if (lineState.getNameMacro() != null) {
                            String macro = lineState.getNameMacro();
                            macro = macro.replace(MACRO_PARAMETER, paramName);
                            lineState.setName(macro);
                        } else {
                            lineState.setNameIfNeeded(paramName);
                        }



                    }
                    String name      = lineState.getName();
                    String canonical = DataAlias.aliasToCanonical(name);
                    //System.err.println ("name:" + name + " canon:" + canonical);
                    if (info.getLevel() != null) {
                        name = name + "@" + Util.formatReal(info.getLevel())
                               + info.getLevel().getUnit();
                    }
                    Set        timeSet   = field.getDomainSet();
                    Unit[]     timeUnits = timeSet.getSetUnits();
                    double[][] times     = timeSet.getDoubles();
                    double[][] values    = field.getValues(false);
                    if (values == null) {
                        continue;
                    }
                    Unit[] rawUnits =
                        ucar.visad.Util.getDefaultRangeUnits(field);
                    boolean haveWinds = (values.length > 1)
                                        && checkWindUnits(rawUnits);
                    for (int j = 0; j < values.length; j++) {
                        // if not winds, don't process more than one param
                        if ((j > 0) && !haveWinds) {
                            continue;
                        }
                        // only handle U & V
                        if ((j > 1) && haveWinds) {
                            break;
                        }
                        if (haveWinds && (values.length > 1)) {
                            canonical = (j == 0)
                                        ? "U"
                                        : "V";
                        }




                        List<MyTimeSeries> timeSeriesList =
                            new ArrayList<MyTimeSeries>();

                        MyTimeSeries series = new MyTimeSeries(name,
                                                  FixedMillisecond.class);

                        //Set        timeSet   = field.getDomainSet();
                        //Unit[]     timeUnits = timeSet.getSetUnits();
                        //double[][] times     = timeSet.getDoubles();
                        //double[][] values    = field.getValues(false);
                        //if (values == null) {
                        //    continue;
                        //}
                        double[] valueArray = values[j];
                        Unit     rawUnit    = rawUnits[j];
                        //ucar.visad.Util.getDefaultRangeUnits(field)[i];
                        if ((lineState.unit != null) && (rawUnit != null)) {
                            valueArray = lineState.unit.toThis(valueArray,
                                    rawUnit);
                        }
                        int    numTimes = times[0].length;
                        double
                            min         = 0,
                            max         = 0;
                        for (int i = 0; i < numTimes; i++) {
                            Date date =
                                Util.makeDate(new DateTime(times[0][i],
                                    timeUnits[0]));

                            if (valueArray[i] != valueArray[i]) {
                                //If is winds then ignore the ignore missing
                                if (Misc.equals(canonical, "U")
                                        || Misc.equals(canonical, "UREL")
                                        || Misc.equals(canonical, "V")
                                        || Misc.equals(canonical, "VREL")
                                        || Misc.equals(canonical, "DIR")
                                        || Misc.equals(canonical, "SPEED")) {}
                                else {
                                    //MISSING
                                    //                                continue;
                                    if (series != null) {
                                        timeSeriesList.add(series);
                                        series = null;
                                    }
                                    continue;
                                }
                            }
                            if (series == null) {
                                series = new MyTimeSeries(name,
                                        FixedMillisecond.class);
                            }
                            series.add(new FixedMillisecond(date), valueArray[i]);
                            if ((i == 0) || (valueArray[i] > max)) {
                                max = valueArray[i];
                            }
                            if ((i == 0) || (valueArray[i] < min)) {
                                min = valueArray[i];
                            }
                        }


                        synchronized (MUTEX) {
                            if (Misc.equals(canonical, "U")
                                    || Misc.equals(canonical, "UREL")) {
                                speedIdx       = paramIdx;
                                speedMin       = min;
                                speedMax       = max;
                                speedUnit      = lineState.unit;
                                speedSeries    = series;
                                polarWind      = false;
                                speedLineState = lineState;
                                continue;
                            }
                            if (Misc.equals(canonical, "V")
                                    || Misc.equals(canonical, "VREL")) {
                                dirIdx       = paramIdx;
                                dirSeries    = series;
                                dirLineState = lineState;
                                dirMin       = min;
                                dirMax       = max;
                                polarWind    = false;
                                continue;
                            }
                            if (Misc.equals(canonical, "SPEED")) {
                                speedIdx       = paramIdx;
                                speedMin       = min;
                                speedMax       = max;
                                speedUnit      = lineState.unit;
                                speedSeries    = series;
                                polarWind      = true;
                                speedLineState = lineState;
                                continue;
                            }
                            if (Misc.equals(canonical, "DIR")) {
                                dirIdx       = paramIdx;
                                dirSeries    = series;
                                dirLineState = lineState;
                                dirMin       = min;
                                dirMax       = max;
                                polarWind    = true;
                                continue;
                            }
                        }

                        if (series != null) {
                            timeSeriesList.add(series);
                        }
                        boolean first = true;
                        for (MyTimeSeries tmp : timeSeriesList) {
                            addSeries(tmp, lineState, paramIdx, null, true,
                                      first);
                            first = false;
                        }
                        addRange(min, max,
                                 "Data range from: " + lineState.getName());
                    }
                }

                if ((speedSeries != null) && (dirSeries != null)) {
                    speedSeries.finish();
                    dirSeries.finish();
                    XYItemRenderer renderer =
                        new WindbarbRenderer(speedLineState, speedSeries,
                                             dirSeries, speedUnit, polarWind);
                    Axis axis = addSeries(speedSeries, speedLineState,
                                          speedIdx, renderer, true);
                    if (speedLineState.getVerticalPosition()
                            != LineState.VPOS_NONE) {
                        axis.setVisible(false);
                    }
                    speedSeries = null;
                    dirSeries   = null;
                }
                if (speedSeries != null) {
                    addSeries(speedSeries, speedLineState, speedIdx, null,
                              true);
                    addRange(speedMin, speedMax,
                             "Data range from: " + speedLineState.getName());
                }
                if (dirSeries != null) {
                    addSeries(dirSeries, dirLineState, dirIdx, null, true);
                    addRange(dirMin, dirMax,
                             "Data range from: " + dirLineState.getName());
                }





            }
            updateContents();
        } finally {
            doneLoadingData();
        }

    }

    /**
     * Check to see if the units are wind units (u &amp; v or speed and dir)
     * @param units units to check
     * @return true if the units check out.
     */
    private boolean checkWindUnits(Unit[] units) {
        if ((units == null) || (units.length < 2)) {
            return false;
        }
        // u & v
        if (Unit.canConvert(units[0], CommonUnit.meterPerSecond)
                && Unit.canConvert(units[1], CommonUnit.meterPerSecond)) {
            return true;
            // speed & dir
        } else if (Unit.canConvert(units[0], CommonUnit.meterPerSecond)
                   && Unit.canConvert(units[1], CommonUnit.degree)) {
            return true;
            // dir && speed
        } else if (Unit.canConvert(units[0], CommonUnit.degree)
                   && Unit.canConvert(units[1], CommonUnit.meterPerSecond)) {
            return true;
        }
        return false;
    }

    /**
     * set chart from point data
     *
     * @param obs obs
     * @param plotVars the vars to plot
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setPointObs(List<PointOb> obs, List plotVars)
            throws VisADException, RemoteException {

        try {
            synchronized (MUTEX) {
                clearLineStates();
                settingData = true;
                int paramIdx = 0;
                initCharts();
                if ((obs != null) && (obs.size() > 0)) {
                    List<PointParam> goodVars = new ArrayList<PointParam>();
                    for (int varIdx = 0; varIdx < plotVars.size(); varIdx++) {
                        PointParam plotVar =
                            (PointParam) plotVars.get(varIdx);
                        LineState lineState = plotVar.getLineState();
                        addLineState(lineState);
                        String     var       = lineState.getName();
                        PointOb    ob        = obs.get(0);
                        Tuple      tuple     = (Tuple) ob.getData();
                        TupleType  tupleType = (TupleType) tuple.getType();
                        MathType[] types     = tupleType.getComponents();
                        boolean    isValid   = false;
                        for (int typeIdx = 0; typeIdx < types.length;
                                typeIdx++) {
                            String name = ucar.visad.Util.cleanTypeName(
                                              types[typeIdx].toString());
                            String canonical =
                                DataAlias.aliasToCanonical(name);
                            if (Misc.equals(name, var)
                                    || Misc.equals(canonical, var)) {
                                lineState.index = typeIdx;
                                Data dataElement =
                                    tuple.getComponent(lineState.index);
                                if ((dataElement instanceof Real)) {
                                    Real obsReal = (Real) dataElement;
                                    Unit displayUnit =
                                        control.getDisplayConventions()
                                            .getDisplayUnit(name,
                                                obsReal.getUnit());
                                    lineState.unit = ((displayUnit != null)
                                            ? displayUnit
                                            : obsReal.getUnit());
                                }
                                if (lineState.getVisible()) {
                                    goodVars.add(plotVar);
                                }
                                isValid = true;
                                break;
                            }
                        }
                        lineState.setValid(isValid);
                    }


                    MyTimeSeries speedSeries    = null;
                    MyTimeSeries dirSeries      = null;
                    LineState    speedLineState = null;
                    LineState    dirLineState   = null;
                    Unit         speedUnit      = null;
                    boolean      polarWind      = true;
                    for (int varIdx = 0; varIdx < goodVars.size(); varIdx++) {
                        PointParam plotVar =
                            (PointParam) goodVars.get(varIdx);
                        LineState lineState = plotVar.getLineState();
                        if ( !lineState.getVisible()) {
                            continue;
                        }
                        MyTimeSeries series   = null;
                        List<String> textList = null;
                        String canonical =
                            DataAlias.aliasToCanonical(lineState.getName());
                        //System.err.println ("var:" + lineState.getName() + " canon:" + canonical);
                        Unit   unit = null;
                        double
                            min     = 0,
                            max     = 0;
                        for (int obIdx = 0; obIdx < obs.size(); obIdx++) {
                            PointOb    ob        = (PointOb) obs.get(obIdx);
                            Tuple      tuple     = (Tuple) ob.getData();
                            TupleType  tupleType =
                                (TupleType) tuple.getType();
                            MathType[] types     = tupleType.getComponents();
                            Data dataElement =
                                tuple.getComponent(lineState.index);
                            Date dttm = Util.makeDate(ob.getDateTime());
                            if (series == null) {
                                series =
                                    new MyTimeSeries(lineState.getName(),
                                        FixedMillisecond.class);
                            }
                            if ( !(dataElement instanceof Real)) {
                                if (textList == null) {
                                    textList = new ArrayList<String>();
                                }
                                try {
                                    series.add(new FixedMillisecond(dttm), 0);
                                    textList.add(dataElement.toString());
                                } catch (Exception exc) {
                                    //noop here. Its sortof bad form but this way we keep the text list in synch with the series
                                }
                                continue;
                            }
                            Real obsReal = (Real) dataElement;

                            if (unit == null) {
                                if (lineState.unit != null) {
                                    unit = lineState.unit;
                                } else {
                                    unit = obsReal.getUnit();
                                }
                            }
                            double value = ((lineState.unit != null)
                                            ? obsReal.getValue(lineState.unit)
                                            : obsReal.getValue());
                            //NaN wind directions must be special cased b/c they cannot
                            //be thrown away lest they get out of sync with speed causing
                            //rendering problems for wind barbs.
                            if ((value == value) || Misc.equals(canonical, "DIR")) {
                                if ((obIdx == 0) || (value > max)) {
                                    max = value;
                                }
                                if ((obIdx == 0) || (value < min)) {
                                    min = value;
                                }
                                series.addOrUpdate(new FixedMillisecond(dttm),
                                        value);
                            }
                        }

                        addRange(min, max,
                                 "Data range from: " + lineState.getName());

                        if (series != null) {
                            synchronized (MUTEX) {
                                XYItemRenderer renderer = null;
                                if (Misc.equals(canonical, "SPEED")) {
                                    speedUnit      = unit;
                                    speedSeries    = series;
                                    speedLineState = lineState;
                                    continue;
                                }
                                if (Misc.equals(canonical, "DIR")) {
                                    dirSeries    = series;
                                    dirLineState = lineState;
                                    continue;
                                }
                                if (Misc.equals(canonical, "U")) {
                                    speedUnit      = unit;
                                    speedSeries    = series;
                                    polarWind      = false;
                                    speedLineState = lineState;
                                    continue;
                                }
                                if (Misc.equals(canonical, "V")) {
                                    dirSeries    = series;
                                    dirLineState = lineState;
                                    polarWind    = false;
                                    continue;
                                }
                                if (Misc.equals(canonical, "CC")) {
                                    double scale = 0;
                                    String n     = lineState.getName();
                                    if (n.equals("CC1") || n.equals("CC2")
                                            || n.equals("CC3")
                                            || n.equals("CC4")) {
                                        scale = 2.0;
                                    }
                                    renderer =
                                        new CloudCoverageRenderer(lineState,
                                            scale);
                                }
                                if (textList != null) {
                                    renderer = new TextRenderer(textList,
                                            lineState);
                                }
                                Axis axis = addSeries(series, lineState,
                                                paramIdx, renderer, true);
                                if (Misc.equals(canonical, "CC")
                                        || (textList != null)) {
                                    axis.setVisible(false);
                                }
                            }
                            paramIdx++;
                        }
                    }
                    if ((speedSeries != null) && (dirSeries != null)) {
                    	WindbarbRenderer renderer =
                            new WindbarbRenderer(speedLineState, speedSeries,
                                dirSeries, speedUnit, polarWind);
                    	renderer.isSouth = (obs != null && obs.size() > 0) ? obs.get(0).getEarthLocation().getLatitude().getValue() < 0 : false;
                        Axis axis = addSeries(speedSeries, speedLineState,
                                        paramIdx++, renderer, true);
                        if (speedLineState.getVerticalPosition()
                                != LineState.VPOS_NONE) {
                            axis.setVisible(false);
                        }
                        speedSeries = null;
                        dirSeries   = null;
                    }
                    if (speedSeries != null) {
                        addSeries(speedSeries, speedLineState, paramIdx++,
                                  null, true);
                    }
                    if (dirSeries != null) {
                        addSeries(dirSeries, dirLineState, paramIdx, null,
                                  true);
                    }
                }
            }
            updateContents();
        } finally {
            doneLoadingData();
        }

    }


    /**
     * set chart from track data
     *
     * @param lines  LineStates
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setTracks(List<LineState> lines)
            throws VisADException, RemoteException {
        try {
            synchronized (MUTEX) {
                int paramIdx = 0;
                clearLineStates();
                settingData = true;
                initCharts();
                for (LineState lineState : lines) {
                    if ( !lineState.getVisible()) {
                        continue;
                    }
                    addLineState(lineState);
                    MyTimeSeries series =
                        new MyTimeSeries(lineState.getName(),
                                         FixedMillisecond.class);
                    List<DateTime> dates  = lineState.getTimes();
                    List<Real>     values = lineState.getValues();
                    if ((dates == null) || (values == null)) {
                        continue;
                    }
                    for (int pointIdx = 0; pointIdx < dates.size();
                            pointIdx++) {
                        Date   dttm  = Util.makeDate(dates.get(pointIdx));
                        double value = values.get(pointIdx).getValue();
                        series.addOrUpdate(new FixedMillisecond(dttm), value);
                    }
                    XYItemRenderer renderer = null;
                    Axis axis = addSeries(series, lineState, paramIdx,
                                          renderer, true);
                    paramIdx++;
                }
            }
            updateContents();
        } finally {
            doneLoadingData();
        }

    }




    /**
     * Class MyTimeSeries buffers the item adds and then adds them all at once
     *
     *
     * @author IDV Development Team
     */
    private static class MyTimeSeries extends TimeSeries {

        /** items */
        List<TimeSeriesDataItem> items = new ArrayList<TimeSeriesDataItem>();

        /** Keeps track of seen items */
        HashSet<TimeSeriesDataItem> seen = new HashSet<TimeSeriesDataItem>();

        /**
         * ctor
         *
         * @param name time series name
         * @param c domain type
         */
        public MyTimeSeries(String name, Class c) {
            super(name, c);
        }

        /**
         * add
         *
         * @param item item
         */
        public void add(TimeSeriesDataItem item) {
            if (seen.contains(item)) {
                return;
            }
            seen.add(item);
            items.add(item);
        }

        /**
         * add
         *
         * @param period period
         * @param value value
         */
        public void add(RegularTimePeriod period, double value) {
            TimeSeriesDataItem item = new TimeSeriesDataItem(period, value);
            add(item);
        }

        /**
         * Sort the items add add them to the list
         */
        public void finish() {
            items = new ArrayList<TimeSeriesDataItem>(Misc.sort(items));

            for (TimeSeriesDataItem item : items) {
                this.data.add(item);
            }
            fireSeriesChanged();
        }


    }





    /**
     *  Set the ShowAnimationTime property.
     *
     *  @param value The new value for ShowAnimationTime
     */
    public void setShowAnimationTime(boolean value) {
        showAnimationTime = value;
    }

    /**
     *  Get the ShowAnimationTime property.
     *
     *  @return The ShowAnimationTime
     */
    public boolean getShowAnimationTime() {
        return showAnimationTime;
    }


    /**
     * utility
     *
     * @return dummy
     */
    public XYDataset getDummyDataset() {
        TimeSeriesCollection dummy = new TimeSeriesCollection();
        return dummy;
    }



    /**
     * Set the location for this chart
     *
     * @param llp  the location
     */
    public void setLocation(LatLonPoint llp) {
        sunriseLocation = llp;
        sunriseDates    = null;
        getContents().repaint();
    }


    /**
     * Draw the sunrise/sunset curves
     *
     * @param g2  the graphics area
     * @param plot   the plot
     * @param dataArea  the date range
     */
    private void drawSunriseSunset(Graphics2D g2, XYPlot plot,
                                   Rectangle2D dataArea) {
        if (sunriseLocation == null) {
            return;
        }
        DateAxis domainAxis = (DateAxis) plot.getDomainAxis();
        Date     startDate  = ((DateAxis) domainAxis).getMinimumDate();
        Date     endDate    = ((DateAxis) domainAxis).getMaximumDate();
        if ((sunriseDates == null) || !Misc.equals(startDate, lastStartDate)
                || !Misc.equals(endDate, lastEndDate)) {
            lastStartDate = startDate;
            lastEndDate   = endDate;
            sunriseDates = IdvTimeline.makeSunriseDates(sunriseLocation,
                    startDate, endDate);
        }
        int top    = (int) (dataArea.getY());
        int bottom = (int) (dataArea.getY() + dataArea.getHeight());
        int height = bottom - top;
        g2.setColor(Color.yellow);
        Shape originalClip = g2.getClip();
        g2.clip(dataArea);
        for (int i = 0; i < sunriseDates.size(); i += 2) {
            Date d1 = (Date) sunriseDates.get(i + 1);
            Date d2 = (Date) sunriseDates.get(i);
            int x1 = (int) domainAxis.valueToJava2D(d1.getTime(), dataArea,
                         RectangleEdge.BOTTOM);
            int x2 = (int) domainAxis.valueToJava2D(d2.getTime(), dataArea,
                         RectangleEdge.BOTTOM);
            g2.fillRect(x1, top, (x2 - x1), height);
        }
        g2.setClip(originalClip);
    }


    /**
     * draw the time line
     *
     * @param g2 param
     * @param plot param
     * @param dataArea param
     * @param domainAxis param
     * @param rangeAxis param
     * @param rendererIndex param
     * @param info param
     */
    private void drawTime(Graphics2D g2, XYPlot plot, Rectangle2D dataArea,
                          ValueAxis domainAxis, ValueAxis rangeAxis,
                          int rendererIndex, PlotRenderingInfo info) {
        try {
            Animation animation = control.getSomeAnimation();
            if (animation == null) {
                return;
            }
            Real dttm = animation.getAniValue();
            if (dttm == null) {
                return;
            }
            g2.setStroke(new BasicStroke());
            g2.setColor(Color.black);
            double timeValue = dttm.getValue(CommonUnit.secondsSinceTheEpoch);
            int x = (int) domainAxis.valueToJava2D(timeValue * 1000,
                        dataArea, RectangleEdge.BOTTOM);
            if ((x < dataArea.getX())
                    || (x > dataArea.getX() + dataArea.getWidth())) {
                return;
            }

            int bottom = (int) (dataArea.getY() + dataArea.getHeight());
            int top    = (int) (dataArea.getY());
            int offset = 0;
            if (false && (clockImage == null)) {
                clockImage = GuiUtils.getImage("/auxdata/ui/icons/clock.gif");
                clockImage.getHeight(this);
                offset = clockImage.getHeight(null);
            }

            //            g2.drawLine(x, (int) dataArea.getY(), x, bottom - offset);
            int   w  = 8;
            int   w2 = w / 2;
            int[] xs = { x - w2, x, x + w2, x };
            int[] ys = { top, top + w, top, top };
            //            g2.drawLine(x, top, x, top+10);
            g2.fillPolygon(xs, ys, xs.length);
            if (clockImage != null) {
                g2.drawImage(clockImage, x - clockImage.getWidth(null) / 2,
                             bottom - clockImage.getHeight(null), null);
            }
        } catch (VisADException exc) {}
        catch (RemoteException exc) {}
    }


    /**
     * Set the date format
     *
     * @param format  the date format
     */
    public void setDateFormat(String format) {
        dateFormat = format;
    }


    /**
     * Get the date format
     *
     * @return  the date format
     */
    public String getDateFormat() {
        return dateFormat;
    }



}

