/*
 * $Id: ScatterPlotChartWrapper.java,v 1.41 2007/04/16 21:32:11 jeffmc Exp $
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
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;



import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.TwoFacedObject;



import visad.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.rmi.RemoteException;




import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;



import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;




/**
 * Provides a scatter plot
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.41 $
 */
public class ScatterPlotChartWrapper extends PlotWrapper {



    /** The plot */
    private MyScatterPlot plot;

    /** The dataset */
    private XYSeriesCollection dataset;


    /** times */
    double[] timeValues1;

    /** times */
    double[] timeValues2;

    /**
     * Default ctor
     */
    public ScatterPlotChartWrapper() {}

    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public ScatterPlotChartWrapper(String name, List dataChoices) {
        super(name, dataChoices);
    }

    /**
     * Return the human readable name of this chart
     *
     * @return Chart type name
     */
    public String getTypeName() {
        return "Scatter Plot";
    }

    /**
     * Handle the event
     *
     * @param event the event
     *
     * @return Should we pass on this event
     */
    public boolean chartPanelMouseClicked(MouseEvent event) {
        if (SwingUtilities.isRightMouseButton(event)
                || (event.getClickCount() < 2)) {
            return super.chartPanelMousePressed(event);
        }
        List series = ((MyScatterPlot) plot).getSeries();
        //TODO: Check if click is inside data area
        double minDistance = 100;
        int    minIdx      = -1;
        double minTime     = -1;
        for (int seriesIdx = 0; seriesIdx < series.size(); seriesIdx++) {
            NumberAxis rangeAxis  = (NumberAxis) plot.getRangeAxis(seriesIdx);
            NumberAxis domainAxis =
                (NumberAxis) plot.getDomainAxis(seriesIdx);
            double[][] data = (double[][]) series.get(seriesIdx);
            for (int i = 0; i < data[0].length; i++) {
                double x = domainAxis.valueToJava2D(data[0][i],
                               getChartPanel().getScreenDataArea(),
                               plot.getDomainAxisEdge());
                double y = rangeAxis.valueToJava2D(data[1][i],
                               getChartPanel().getScreenDataArea(),
                               plot.getRangeAxisEdge());

                double distance = Math.sqrt((x - event.getX())
                                            * (x - event.getX()) + (y
                                               - event.getY()) * (y
                                                   - event.getY()));
                if (distance < minDistance) {
                    minDistance = distance;
                    minIdx      = i;
                }
            }
            if (minIdx >= 0) {
                minTime = timeValues1[minIdx];
            }
        }
        if (minIdx < 0) {
            return EVENT_PASSON;
        }
        firePropertyChange(PROP_SELECTEDTIME, null, new Double(minTime));
        return EVENT_PASSON;
    }


    /**
     * Get the data categories for choosing new data
     *
     * @return data categories
     */
    public List getCategories() {
        //Return a list of data categories for each parameter we are selecting
        List cats = DataCategory.parseCategories("trace;IMAGE-2D-*", false);
        return Misc.newList(cats, cats);
    }

    /**
     * Create the chart
     */
    private void createChart() {
        if (chartPanel != null) {
            return;
        }
        // create a dataset...
        XYSeries series1 = new XYSeries("Series 1");
        dataset = new XYSeriesCollection(series1);

        String name = getName();
        if (name == null) {
            name = "Scatter Plot";
        }
        // create the chart...
        chart = createScatterPlot(name, "Domain", "Range",
                                  PlotOrientation.VERTICAL, true, false,
                                  false);


        initXYPlot(plot);
        plot.setRenderer(new MyRenderer(LineState.SHAPE_POINT));
        doMakeChartPanel(chart);
    }


    /**
     * Create the gui
     *
     * @return The  gui
     */
    protected JComponent doMakeContents() {
        createChart();
        return chartPanel;
    }



    /**
     * Just a hook to turn off the series display in the legend and to
     * hold the shape to draw.
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.41 $
     */
    public static class MyRenderer extends XYLineAndShapeRenderer {

        /** The shape */
        int shape;

        /**
         * ctor
         *
         * @param shape The shape
         */
        public MyRenderer(int shape) {
            super(false, true);
            this.shape = shape;
        }

        /**
         * draw series in legend
         *
         * @param i which series
         *
         * @return draw series in legend
         */
        public boolean isSeriesVisibleInLegend(int i) {
            return false;
        }
    }


    /**
     * Hook to intercept these calls on the chart
     *
     * @param event The event
     *
     * @return  Was this event handled by the ChartWrapper
     */
    public String chartPanelGetToolTipText(MouseEvent event) {
        return "Right click to show menu; Double click to set time in other charts";
    }





    /**
     * Create the scatter plit
     *
     * @param title  title
     * @param xAxisLabel  xAxisLabel
     * @param yAxisLabel  yAxisLabel
     * @param orientation  orientation
     * @param legend  legend
     * @param tooltips  tooltips
     * @param urls  urls
     *
     * @return The chart
     */
    private JFreeChart createScatterPlot(String title, String xAxisLabel,
                                         String yAxisLabel,
                                         PlotOrientation orientation,
                                         boolean legend, boolean tooltips,
                                         boolean urls) {

        if (orientation == null) {
            throw new IllegalArgumentException(
                "Null 'orientation' argument.");
        }
        NumberAxis xAxis = new NumberAxis(xAxisLabel);
        xAxis.setAutoRangeIncludesZero(false);
        NumberAxis yAxis = new NumberAxis(yAxisLabel);
        yAxis.setAutoRangeIncludesZero(false);


        plot = new MyScatterPlot(dataset, null, xAxis, yAxis);
        JFreeChart chart = new JFreeChart(title,
                                          JFreeChart.DEFAULT_TITLE_FONT,
                                          plot, legend);

        return chart;
    }


    /**
     * Apply properties
     *
     *
     * @return Successful
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        return true;
    }




    /**
     * Create the charts
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() throws VisADException, RemoteException {

        try {
            createChart();
            for (int dataSetIdx = 0; dataSetIdx < plot.getDatasetCount();
                    dataSetIdx++) {
                XYSeriesCollection dataset =
                    (XYSeriesCollection) plot.getDataset(dataSetIdx);
                dataset.removeAllSeries();
            }
            ((MyScatterPlot) plot).removeAllSeries();
            Hashtable props = new Hashtable();
            props.put(TrackDataSource.PROP_TRACKTYPE,
                      TrackDataSource.ID_TIMETRACE);

            List dataChoiceWrappers = getDataChoiceWrappers();
            int  dataSetCnt         = 0;
            for (int paramIdx = 0; paramIdx < dataChoiceWrappers.size();
                    paramIdx += 2) {
                if (paramIdx + 1 >= dataChoiceWrappers.size()) {
                    break;
                }
                DataChoiceWrapper wrapper1 =
                    (DataChoiceWrapper) dataChoiceWrappers.get(paramIdx);
                DataChoiceWrapper wrapper2 =
                    (DataChoiceWrapper) dataChoiceWrappers.get(paramIdx + 1);

                DataChoice dataChoice1 = wrapper1.getDataChoice();
                DataChoice dataChoice2 = wrapper2.getDataChoice();

                FlatField data1 =
                    getFlatField((FieldImpl) dataChoice1.getData(null,
                        props));
                FlatField data2 =
                    getFlatField((FieldImpl) dataChoice2.getData(null,
                        props));
                Unit unit1 = ucar.visad.Util.getDefaultRangeUnits(
                                 (FlatField) data1)[0];
                Unit unit2 = ucar.visad.Util.getDefaultRangeUnits(
                                 (FlatField) data2)[0];

                NumberAxis rangeAxis =
                    new NumberAxis(wrapper2.getLabel(unit2));
                NumberAxis domainAxis =
                    new NumberAxis(wrapper1.getLabel(unit1));

                domainAxis.setAutoRange(getAutoRange());

                Color c = wrapper1.getColor(paramIdx);
                MyRenderer renderer =
                    new MyRenderer(wrapper1.getLineState().getShape());
                domainAxis.setLabelPaint(c);
                rangeAxis.setLabelPaint(c);
                renderer.setSeriesPaint(0, c);

                double[][] samples1    = data1.getValues(false);
                double[][] samples2    = data2.getValues(false);
                double[]   timeValues1 = getTimeValues(samples1, data1);
                double[]   timeValues2 = getTimeValues(samples2, data2);
                double[][] values1     = filterData(samples1[0], timeValues1);
                double[][] values2     = filterData(samples2[0], timeValues2);
                if (values1.length > 1) {
                    this.timeValues1 = values1[1];
                    this.timeValues2 = values2[1];
                }
                double[][] values = {
                    values1[0], values2[0]
                };
                ((MyScatterPlot) plot).addSeries(values);

                //Add in a dummy dataset
                XYSeriesCollection dataset =
                    new XYSeriesCollection(new XYSeries(""));

                if ( !getAutoRange()) {
                    NumberAxis oldRangeAxis =
                        (NumberAxis) plot.getRangeAxis(dataSetCnt);
                    NumberAxis oldDomainAxis =
                        (NumberAxis) plot.getDomainAxis(dataSetCnt);
                    if ((oldRangeAxis != null) && (oldDomainAxis != null)) {
                        rangeAxis.setRange(oldRangeAxis.getRange());
                        domainAxis.setRange(oldDomainAxis.getRange());
                    }
                }


                plot.setDataset(dataSetCnt, dataset);
                plot.setRenderer(dataSetCnt, renderer);
                plot.setRangeAxis(dataSetCnt, rangeAxis, false);
                plot.setDomainAxis(dataSetCnt, domainAxis, false);
                plot.mapDatasetToRangeAxis(dataSetCnt, dataSetCnt);
                plot.mapDatasetToDomainAxis(dataSetCnt, dataSetCnt);

                if ( !getAutoRange()) {
                    rangeAxis.setAutoRange(false);
                    domainAxis.setAutoRange(false);
                }

                dataSetCnt++;
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
        }

    }

    /**
     * Utility to make the gui widget for the wrapper in the properties list
     *
     * @param idx Which one
     * @param fieldProperty Holder of stuff
     *
     * @return The gui
     */
    protected JComponent doMakeWrapperDisplayComponent(int idx,
            FieldProperties fieldProperty) {
        if (idx % 2 != 0) {
            return new JLabel(" ");
        }
        LineState ls = fieldProperty.wrapper.getLineState();
        ls.getPropertyContents();
        List comps = new ArrayList();
        comps.add(GuiUtils.inset(ls.colorSwatch, 4));
        comps.add(ls.shapeBox);
        return GuiUtils.left(GuiUtils.hbox(comps, 4));
    }


    /**
     * Returns the list of labels used for selecting data choices.
     *
     * @return List of field labels
     */
    public List getFieldSelectionLabels() {
        return Misc.newList("X Axis Field", "Y Axis Field");
    }

    /**
     * When selecting data does the data tree support multiple selections
     *
     * @return Do multiples
     */
    public boolean doMultipleAddFields() {
        return false;
    }


    /**
     * Don't add parameters
     *
     * @return Don't add parameters
     */
    public boolean canDoParameters() {
        return true;
    }


    /**
     * canBeASourceForTimeSelectionEvents
     *
     * @return true
     */
    protected boolean canBeASourceForTimeSelectionEvents() {
        return true;
    }


    /**
     * Can we do chart colors
     *
     * @return can do colors
     */
    protected boolean canDoColors() {
        return true;
    }


    /**
     * Can we have colors on the data chocie wrappers in the properties dialog
     *
     * @return can do wrapper color
     */
    public boolean canDoWrapperColor() {
        return true;
    }






    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return "Scatter Plot: " + getName();
    }




}

