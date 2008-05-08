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

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.data.point.*;
import ucar.unidata.data.storm.*;

import ucar.unidata.idv.ControlContext;
import ucar.unidata.idv.IdvPreferenceManager;

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.ProbeRowInfo;
import ucar.unidata.idv.control.chart.LineState;


import ucar.unidata.ui.symbol.*;
import ucar.unidata.idv.ui.IdvTimeline;

import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;

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
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import javax.swing.*;

import org.itc.idv.math.SunriseSunsetCollector;

/**
 * A time series chart
 *
 * @author MetApps Development Team
 * @version $Revision: 1.12 $
 */

public class TimeSeriesChart extends XYChartManager {

    /** Shows time */
    private XYAnnotation animationTimeAnnotation;

    /** Should show time */
    private boolean showAnimationTime;

    /** Clock to draw_ */
    private static Image clockImage;

    /** format */
    NumberFormat numberFormat;

    private List sunriseDates;
    private LatLonPoint sunriseLocation;
    private Date lastStartDate;
    private Date lastEndDate;

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
        DateAxis timeAxis = new DateAxis("Time (" + timeZone.getID() + ")",
                                         timeZone);

        SimpleDateFormat sdf =
            new SimpleDateFormat(pref.getDefaultDateFormat());
        sdf.setTimeZone(timeZone);
        timeAxis.setDateFormatOverride(sdf);

        final XYPlot[] xyPlotHolder = {null};

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


        if (lineState.getRange() != null) {
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

        String     axisLabel = name + ((unit != null)
                                       ? " [" + unit + "]"
                                       : "");
        NumberAxis rangeAxis;

        if (lineState.getUseLogarithmicRange() && false) {
            rangeAxis = new FixedWidthLogarithmicAxis(axisLabel);
        } else {
            rangeAxis = new FixedWidthNumberAxis(axisLabel);
            ((NumberAxis) rangeAxis).setAutoRangeIncludesZero(
                lineState.getRangeIncludesZero());
        }

        rangeAxis.setNumberFormatOverride(numberFormat);
        rangeAxis.setVisible(rangeVisible);


        ucar.unidata.util.Range r = lineState.getRange();
        if (r != null) {
            rangeAxis.setRange(new org.jfree.data.Range(r.getMin(),
                    r.getMax()));
        }

        if (renderer == null) {
            renderer = getRenderer(lineState);
        }

        Paint c = lineState.getColor(paramIdx);
        rangeAxis.setLabelPaint(Color.black);
        renderer.setSeriesPaint(0, c);
        renderer.setSeriesStroke(0, lineState.getStroke());

        if ( !lineState.getAxisVisible()) {
            rangeAxis.setVisible(false);
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
        List<ProbeRowInfo> rowInfos = (currentProbeData==null?null:new ArrayList<ProbeRowInfo>(currentProbeData));
        try {
            initCharts();
            if ((rowInfos != null) && (rowInfos.size() > 0)) {
                for (int paramIdx = 0; paramIdx < rowInfos.size();
                        paramIdx++) {
                    ProbeRowInfo info = rowInfos.get(paramIdx);
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
                    lineState.setName(info.getDataInstance().getParamName());
                    if (info.getLevel() != null) {
                        lineState.setName(lineState.getName() + "@"
                                          + Util.formatReal(info.getLevel())
                                          + info.getLevel().getUnit() + " ");
                    }

                    TimeSeries series = new TimeSeries(lineState.getName(),
                                            Millisecond.class);

                    Set        timeSet    = field.getDomainSet();
                    Unit[]     timeUnits  = timeSet.getSetUnits();
                    double[][] times      = timeSet.getDoubles();
                    double[][] values     = field.getValues(false);
                    if(values == null) continue;
                    double[]   valueArray = values[0];
                    Unit rawUnit =
                        ucar.visad.Util.getDefaultRangeUnits(field)[0];
                    if (lineState.unit != null && rawUnit !=null) {
                        valueArray = lineState.unit.toThis(valueArray,
                                rawUnit);
                    }
                    int    numTimes = times[0].length;
                    double
                        min         = 0,
                        max         = 0;
                    for (int i = 0; i < numTimes; i++) {
                        if (valueArray[i] != valueArray[i]) {
                            continue;
                        }
                        Date date = Util.makeDate(new DateTime(times[0][i],
                                                               timeUnits[0]));
                        if ((i == 0) || (valueArray[i] > max)) {
                            max = valueArray[i];
                        }
                        if ((i == 0) || (valueArray[i] < min)) {
                            min = valueArray[i];
                        }
                        series.addOrUpdate(new Millisecond(date),
                                           valueArray[i]);
                    }
                    addSeries(series, lineState, paramIdx, null, true);
                    addRange(min, max,
                             "Data range from: " + lineState.getName());
                }
            }
            updateContents();
        } finally {
            doneLoadingData();
        }
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
                    List goodVars = new ArrayList();
                    for (int varIdx = 0; varIdx < plotVars.size(); varIdx++) {
                        PointParam plotVar = (PointParam)plotVars.get(varIdx);
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


                    TimeSeries speedSeries    = null;
                    TimeSeries dirSeries      = null;
                    LineState  speedLineState = null;
                    LineState  dirLineState   = null;
                    Unit       speedUnit      = null;
                    for (int varIdx = 0; varIdx < goodVars.size(); varIdx++) {
                        PointParam plotVar =
                            (PointParam) goodVars.get(varIdx);
                        LineState lineState = plotVar.getLineState();
                        if ( !lineState.getVisible()) {
                            continue;
                        }
                        TimeSeries series   = null;
                        List       textList = null;
                        String canonical =
                            DataAlias.aliasToCanonical(lineState.getName());
                        //                    System.err.println ("var:" + var + " canon:" + canonical);
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
                                series = new TimeSeries(lineState.getName(),
                                        Millisecond.class);
                            }
                            if ( !(dataElement instanceof Real)) {
                                if (textList == null) {
                                    textList = new ArrayList();
                                }
                                try {
                                    series.add(new Millisecond(dttm), 0);
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
                            if (value == value) {
                                if ((obIdx == 0) || (value > max)) {
                                    max = value;
                                }
                                if ((obIdx == 0) || (value < min)) {
                                    min = value;
                                }
                                series.addOrUpdate(new Millisecond(dttm),
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
                        XYItemRenderer renderer =
                            new WindbarbRenderer(speedLineState, speedSeries,
                                dirSeries, speedUnit);
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
     * @param obs obs
     * @param plotVars the vars to plot
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
                for(LineState lineState: lines) {
                    if ( !lineState.getVisible()) {
                        continue;
                    }
                    addLineState(lineState);
                    TimeSeries series   =  new TimeSeries(lineState.getName(),
                                                          Millisecond.class);
                    List<DateTime> dates = lineState.getTimes();
                    List<Real> values = lineState.getValues();
                    if(dates == null || values == null) continue;
                    for(int pointIdx=0;pointIdx<dates.size();pointIdx++) {
                        Date dttm = Util.makeDate(dates.get(pointIdx));
                        double value = values.get(pointIdx).getValue();
                        series.addOrUpdate(new Millisecond(dttm),
                                           value);
                    }
                    XYItemRenderer renderer = null;
                    Axis axis = addSeries(series, lineState,
                                          paramIdx, renderer, true);
                    paramIdx++;
                }
            }
            updateContents();
        } finally {
            doneLoadingData();
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


     
    public void setLocation(LatLonPoint llp) {
        sunriseLocation = llp;
        sunriseDates = null;
        getContents().repaint();
    }


    private void drawSunriseSunset(Graphics2D g2, XYPlot plot, Rectangle2D dataArea) {
        if(sunriseLocation == null) return;
        DateAxis domainAxis = (DateAxis)plot.getDomainAxis();
        Date startDate = ((DateAxis)domainAxis).getMinimumDate();
        Date endDate = ((DateAxis)domainAxis).getMaximumDate();
        if(sunriseDates == null || !Misc.equals(startDate, lastStartDate) ||
           !Misc.equals(endDate, lastEndDate)) {
            lastStartDate = startDate;
            lastEndDate = endDate;
            sunriseDates = IdvTimeline.makeSunriseDates(sunriseLocation, startDate, endDate);
        }
        int top    = (int) (dataArea.getY());
        int bottom = (int) (dataArea.getY() + dataArea.getHeight());
        int height = bottom-top;
        g2.setColor(Color.yellow);
        Shape     originalClip      = g2.getClip();
        g2.clip(dataArea);
        for (int i = 0; i < sunriseDates.size(); i += 2) {
            Date d1 = (Date) sunriseDates.get(i + 1);
            Date d2 = (Date) sunriseDates.get(i);
            int x1 = (int) domainAxis.valueToJava2D(d1.getTime(),
                        dataArea, RectangleEdge.BOTTOM);
            int x2 = (int) domainAxis.valueToJava2D(d2.getTime(),
                        dataArea, RectangleEdge.BOTTOM);
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
            Animation animation = control.getAnimation();
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

}

