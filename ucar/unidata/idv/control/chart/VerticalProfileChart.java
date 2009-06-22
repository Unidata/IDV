/*
 * $Id: VerticalProfileChart.java,v 1.13 2007/04/16 21:32:12 jeffmc Exp $
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

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.VerticalProfileInfo;

import ucar.unidata.idv.control.chart.LineState;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import visad.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import java.rmi.RemoteException;

import java.text.*;

import java.util.ArrayList;
import java.util.List;


import javax.swing.*;



/**
 * A time series chart
 *
 * @author MetApps Development Team
 * @version $Revision: 1.13 $
 */

public class VerticalProfileChart extends XYChartManager {

    /** number format */
    NumberFormat numberFormat;

    /** time */
    Real time;

    /** profile */
    List profiles;

    /**
     * Default ctor
     */
    public VerticalProfileChart() {}


    /**
     * Default constructor.
     *
     * @param control the control to associate with
     */
    public VerticalProfileChart(DisplayControlImpl control) {
        this(control, "Vertical Profile");
    }


    /**
     * Default constructor.
     *
     * @param control the control to associate with
     * @param chartName the name for the chart
     */
    public VerticalProfileChart(DisplayControlImpl control,
                                String chartName) {
        super(control, chartName);
    }


    /**
     *  Add a series to the charts
     *
     *
     * @param series   series
     * @param lineState line state
     * @param paramIdx  param index
     * @param renderer  renderer
     * @param rangeVisible range visible
     *
     * @return  the Axis
     */
    protected Axis addSeries(XYSeries series, LineState lineState,
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


        String             name    = lineState.getName();
        Unit               unit    = lineState.unit;
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        NumberAxis rangeAxis;
        String     axisLabel = name + ((unit != null)
                                       ? " [" + unit + "]"
                                       : "");

        if (lineState.getUseLogarithmicRange() && false) {
            rangeAxis = new FixedWidthLogarithmicAxis(axisLabel);
        } else {
            //rangeAxis = new FixedWidthNumberAxis(axisLabel);
            rangeAxis = new NumberAxis(axisLabel);
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
            renderer = getRenderer(lineState);
        }

        Paint c = lineState.getColor(paramIdx);
        rangeAxis.setLabelPaint(Color.black);
        renderer.setSeriesPaint(0, c);
        renderer.setSeriesStroke(0, lineState.getStroke());

        if ( !lineState.getAxisVisible()) {
            rangeAxis.setVisible(false);
        }

        AxisLocation side        = null;
        ChartHolder  chartHolder = getChartHolder(lineState);
        if (rangeAxis.isVisible()) {
            if (lineState.getSide() == LineState.SIDE_UNDEFINED) {
                side = AxisLocation.BOTTOM_OR_RIGHT;
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


    /**
     * A time changed method that passes in the time
     *
     * @param time  the time
     */
    public void timeChanged(Real time) {
        this.time = time;
        try {
            updateCharts();
        } catch (Exception ve) {
            control.logException("timeChanged", ve);
        }
        super.timeChanged(time);
    }

    /**
     * Set the profiles for this chart
     *
     * @param vpInfos  the list of VerticalProfileInfos
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException  VisAD problem
     */
    public void setProfiles(List vpInfos)
            throws VisADException, RemoteException {
        profiles = vpInfos;
        updateCharts();
    }

    /**
     * Update the charts with the appropriate data
     *
     * @throws RemoteException Java RMI Exception
     * @throws VisADException  VisAD problem
     */
    private void updateCharts() throws VisADException, RemoteException {

        if (profiles == null) {
            return;
        }
        clearLineStates();
        startLoadingData();
        try {
            initCharts();
            if ((profiles != null) && !profiles.isEmpty()) {

                XYSeries  speedSeries    = null;
                XYSeries  dirSeries      = null;
                LineState speedLineState = null;
                LineState dirLineState   = null;
                Unit      speedUnit      = null;
                boolean   polarWind      = true;
                int       lineIdx        = 0;
                for (int paramIdx = 0; paramIdx < profiles.size();
                        paramIdx++) {
                    VerticalProfileInfo vpInfo =
                        (VerticalProfileInfo) profiles.get(paramIdx);
                    LineState lineState = vpInfo.getLineState();
                    lineState.setUseVerticalPosition(false);
                    addLineState(lineState);
                    lineState.setName(
                        vpInfo.getDataInstance().getParamName());
                    lineState.unit = vpInfo.getUnit();
                    FieldImpl profile = vpInfo.getProfile();
                    if (profile == null) {
                        continue;
                    }
                    FlatField oneTime;
                    boolean isTimeSequence = GridUtil.isTimeSequence(profile);
                    if (isTimeSequence) {
                        if (time == null) {
                            oneTime = (FlatField) profile.getSample(0);
                        } else {
                            oneTime = (FlatField) profile.evaluate(time);
                        }
                    } else {
                        oneTime = (FlatField) profile;
                    }
                    String canonical =
                        DataAlias.aliasToCanonical(lineState.getName());
                    float[] alts =
                        oneTime.getDomainSet().getSamples(false)[0];
                    float[][] values = oneTime.getFloats(true);
                    Unit[] rawUnits =
                        ucar.visad.Util.getDefaultRangeUnits(oneTime);
                    boolean haveWinds =
                        (values.length > 1) && Unit
                            .canConvert(rawUnits[0], CommonUnit
                                .meterPerSecond) && Unit
                                    .canConvert(rawUnits[1], CommonUnit
                                        .meterPerSecond);
                    for (int j = 0; j < values.length; j++) {
                        // if not winds, don't process more than one param
                        if ((j > 0) && !haveWinds) {
                            continue;
                        }
                        // only handle U & V
                        if ((j > 1) && haveWinds) {
                            break;
                        }
                        if (haveWinds) {
                            canonical = (j == 0)
                                        ? "U"
                                        : "V";
                        }
                        XYSeries series = new XYSeries(lineState.getName());


                        //float[] vals = oneTime.getFloats(true)[0];
                        float[] vals    = values[j];
                        Unit    rawUnit = rawUnits[j];
                        //ucar.visad.Util.getDefaultRangeUnits(oneTime)[0];
                        if ((lineState.unit != null)
                                && Unit.canConvert(lineState.unit, rawUnit)) {
                            vals = lineState.unit.toThis(vals, rawUnit);
                        }
                        for (int i = 0; i < alts.length; i++) {
                            series.add(alts[i], vals[i]);
                        }
                        if (series != null) {
                            synchronized (MUTEX) {
                                XYItemRenderer renderer = null;
                                if (Misc.equals(canonical, "SPEED")) {
                                    speedUnit      = lineState.unit;
                                    speedSeries    = series;
                                    speedLineState = lineState;
                                    continue;
                                }
                                if (Misc.equals(canonical, "DIR")) {
                                    dirSeries    = series;
                                    dirLineState = lineState;
                                    continue;
                                }
                                if (Misc.equals(canonical, "U")
                                        || Misc.equals(canonical, "UREL")) {
                                    speedUnit      = lineState.unit;
                                    speedSeries    = series;
                                    polarWind      = false;
                                    speedLineState = lineState;
                                    continue;
                                }
                                if (Misc.equals(canonical, "V")
                                        || Misc.equals(canonical, "VREL")) {
                                    dirSeries    = series;
                                    dirLineState = lineState;
                                    polarWind    = false;
                                    continue;
                                }
                                addSeries(series, lineState, lineIdx,
                                          renderer, true);
                            }
                            lineIdx++;
                        }
                        //addSeries(series, lineState, paramIdx, null, true);
                    }
                }
                if ((speedSeries != null) && (dirSeries != null)) {
                    XYItemRenderer renderer =
                        new WindbarbRenderer(speedLineState, speedSeries,
                                             dirSeries, speedUnit, polarWind);
                    Axis axis = addSeries(speedSeries, speedLineState,
                                          lineIdx++, renderer, true);
                    if (speedLineState.getVerticalPosition()
                            != LineState.VPOS_NONE) {
                        axis.setVisible(false);
                    }
                    speedSeries = null;
                    dirSeries   = null;
                }
                if (speedSeries != null) {
                    addSeries(speedSeries, speedLineState, lineIdx++, null,
                              true);
                }
                if (dirSeries != null) {
                    addSeries(dirSeries, dirLineState, lineIdx, null, true);
                }
            }
            updateContents();
        } finally {
            doneLoadingData();
        }

    }

    /**
     * Initialize the plot
     *
     * @param plot the plot to initialize
     */
    protected void initPlot(Plot plot) {
        XYPlot xyPlot = (XYPlot) plot;
        xyPlot.setOrientation(PlotOrientation.HORIZONTAL);

        int count = xyPlot.getDatasetCount();
        for (int i = 0; i < count; i++) {
            xyPlot.setDataset(i, null);
            xyPlot.setRenderer(i, null);
        }
        xyPlot.clearRangeAxes();
        XYSeriesCollection dummyDataset = new XYSeriesCollection();
        //ValueAxis          rangeAxis    = new FixedWidthNumberAxis();
        ValueAxis rangeAxis = new NumberAxis();
        xyPlot.setRangeAxis(0, rangeAxis, false);
        xyPlot.setDataset(0, dummyDataset);
        xyPlot.mapDatasetToRangeAxis(0, 0);
        xyPlot.setRenderer(0, new XYLineAndShapeRenderer());
    }

    /**
     * Get the preferred chart size
     * @return the preferred size
     */
    protected Dimension getPreferredChartSize() {
        return new Dimension(100, 300);
    }


    /**
     * Make the plot
     *
     * @return the plot
     */
    public Plot doMakePlot() {
        return new MyXYPlot(new XYSeriesCollection(),
                            new NumberAxis("Altitude (m)"),
                            new NumberAxis(""), null);
    }

    /**
     * Get a dummy dataset for the plot
     *
     * @return a dummy dataset
     */
    public XYDataset getDummyDataset() {
        return new XYSeriesCollection();
    }
}

