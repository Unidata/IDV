/*
 * $Id: MyScatterPlot.java,v 1.20 2007/04/16 21:32:10 jeffmc Exp $
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


import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.event.PlotChangeEvent;


import org.jfree.chart.plot.*;

import org.jfree.data.*;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.io.SerialUtilities;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.ArrayUtilities;
import org.jfree.util.ObjectUtilities;

import java.awt.*;

import java.awt.geom.*;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;


/**
 * A fast scatter plot.
 */
public class MyScatterPlot extends XYPlot implements ValueAxisPlot {



    /** The data */
    private List series = new ArrayList();




    /** The resourceBundle for the localization. */
    protected static ResourceBundle localizationResources =
        ResourceBundle.getBundle("org.jfree.chart.plot.LocalizationBundle");



    /**
     * Creates a new fast scatter plot.
     * <P>
     * The data is an array of x, y values:  data[0][i] = x, data[1][i] = y.
     *
     *
     * @param dataset The dataset
     * @param data  the data.
     * @param domainAxis  the domain (x) axis.
     * @param rangeAxis  the range (y) axis.
     */
    public MyScatterPlot(XYDataset dataset, double[][] data,
                         ValueAxis domainAxis, ValueAxis rangeAxis) {

        super(dataset, domainAxis, rangeAxis, null);
        addSeries(data);
    }

    /**
     * remove em
     */
    public void removeAllSeries() {
        series.clear();
    }

    /**
     * Get the time series
     *
     * @return Time series
     */
    public List getSeries() {
        return series;
    }

    /**
     * Set the data. Redraw the plot.
     *
     * @param newData The data
     */
    public void addSeries(double[][] newData) {
        series.add(newData);
        configureDomainAxes();
        configureRangeAxes();
        if (newData != null) {
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Returns a short string describing the plot type.
     *
     * @return A short string describing the plot type.
     */
    public String getPlotType() {
        return localizationResources.getString("Fast_Scatter_Plot");
    }

    /**
     * Returns the orientation of the plot.
     *
     * @return The orientation (always {@link PlotOrientation#VERTICAL}).
     */
    public PlotOrientation getOrientation() {
        return PlotOrientation.VERTICAL;
    }



    /**
     * Draws the fast scatter plot on a Java 2D graphics device (such as the
     * screen or a printer).
     * a
     * @param g2  the graphics device.
     * @param dataArea the data area
     * @param index which data set
     * @param info  collects chart drawing information (<code>null</code>
     *              permitted).
     * @param crosshairState crosshairState
     *
     * @return did something
     */
    public boolean render(Graphics2D g2, Rectangle2D dataArea, int index,
                          PlotRenderingInfo info,
                          CrosshairState crosshairState) {

        if (index >= series.size()) {
            return false;
        }
        XYDataset dataset = getDataset(index);
        g2.setStroke(new BasicStroke());
        //                   getRendererForDataset(dataset).getSeriesStroke(0));
        ScatterPlotChartWrapper.MyRenderer renderer =
            (ScatterPlotChartWrapper.MyRenderer) getRendererForDataset(
                dataset);
        g2.setPaint(renderer.getSeriesPaint(0));
        int             shape        = renderer.shape;

        PlotOrientation orientation  = getOrientation();
        int             seenCnt      = 0;

        int             xx           = (int) dataArea.getMinX();
        int             ww           = (int) dataArea.getWidth();
        int             yy           = (int) dataArea.getMaxY();
        int             hh           = (int) dataArea.getHeight();
        ValueAxis       rangeAxis    = getRangeAxisForDataset(index);
        ValueAxis       domainAxis   = getDomainAxisForDataset(index);
        double          domainMin    = domainAxis.getLowerBound();
        double          domainLength = domainAxis.getUpperBound() - domainMin;
        double          rangeMin     = rangeAxis.getLowerBound();
        double          rangeLength  = rangeAxis.getUpperBound() - rangeMin;
        int             boxWidth     = 6;

        double[][]      data         = (double[][]) series.get(index);

        double[]        d1           = data[0];
        double[]        d2           = data[1];
        int             size         = d1.length;


        Hashtable       seen         = new Hashtable();
        int             lastX        = 0;
        int             lastY        = 0;
        //TODO: Check for clipping
        //TODO: Try to create a GeneralPath with the points
        //and cal g2.draw just once
        GeneralPath path = new GeneralPath();
        long        t1   = System.currentTimeMillis();


        for (int i = 0; i < size; i++) {
            int transX = (int) (xx + ww * (d1[i] - domainMin) / domainLength);
            int    transY = (int) (yy - hh * (d2[i] - rangeMin)
                                   / rangeLength);
            Object key    = transX + "_" + transY;
            if (seen.get(key) != null) {
                seenCnt++;
                continue;
            }
            seen.put(key, key);
            if (crosshairState != null) {
                crosshairState.updateCrosshairPoint(d1[i], d2[i], transX,
                        transY, orientation);
            }


            switch (shape) {

              case LineState.SHAPE_VLINE :
                  if (i > 1) {
                      g2.drawLine(lastX, lastY, transX, transY);
                  }
                  lastX = transX;
                  lastY = transY;

              case LineState.SHAPE_POINT :
                  path.append(new Rectangle((int) transX, (int) transY, 1,
                                            1), false);
                  break;

              case LineState.SHAPE_LARGEPOINT :
                  path.append(new Rectangle((int) transX, (int) transY, 2,
                                            2), false);
                  break;

              case LineState.SHAPE_RECTANGLE :
                  path.append(new Rectangle((int) transX - boxWidth / 2,
                                            (int) transY - boxWidth / 2,
                                            boxWidth, boxWidth), false);
                  break;


              case LineState.SHAPE_X :
                  g2.drawLine(transX - boxWidth / 2, transY - boxWidth / 2,
                              transX + boxWidth - boxWidth / 2,
                              transY + boxWidth - boxWidth / 2);
                  g2.drawLine(transX + boxWidth - boxWidth / 2,
                              transY - boxWidth / 2, transX - boxWidth / 2,
                              transY + boxWidth - boxWidth / 2);
                  break;

              case LineState.SHAPE_PLUS :
                  g2.drawLine(transX + boxWidth / 2, transY,
                              transX + boxWidth / 2, transY + boxWidth);
                  g2.drawLine(transX, transY + boxWidth / 2,
                              transX + boxWidth, transY + boxWidth / 2);
                  break;

            }
        }
        g2.fill(path);
        long t2 = System.currentTimeMillis();
        //        System.out.println ("time:" + (t2-t1));
        return true;
    }



    /**
     * Returns the range of data values to be plotted along the axis.
     *
     * @param axis  the axis.
     *
     * @return The range.
     */
    public Range getDataRange(ValueAxis axis) {
        if (series == null) {
            return new Range(0.0, 1.0);
        }
        boolean isDomainAxis = true;

        int     index        = -1;
        // is it a domain axis?
        int domainIndex = getDomainAxisIndex(axis);
        if (domainIndex >= 0) {
            isDomainAxis = true;
            index        = domainIndex;
        }

        // or is it a range axis?
        int rangeIndex = getRangeAxisIndex(axis);
        if (rangeIndex >= 0) {
            isDomainAxis = false;
            index        = rangeIndex;
        }
        if ((index < 0) || (index >= series.size())) {
            return new Range(0.0, 1.0);
        }

        double[][] data = (double[][]) series.get(index);
        if (isDomainAxis) {
            return calculateXDataRange(data);
        }
        return calculateYDataRange(data);
    }

    /**
     * Calculates the X data range.
     *
     * @param data  the data.
     *
     * @return The range.
     */
    private Range calculateXDataRange(double[][] data) {
        Range result = null;
        //      double[][] data =  (double[][]) series.get(0);
        if (data != null) {
            double lowest  = Double.POSITIVE_INFINITY;
            double highest = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < data[0].length; i++) {
                double v = data[0][i];
                if (v < lowest) {
                    lowest = v;
                }
                if (v > highest) {
                    highest = v;
                }
            }
            if (lowest <= highest) {
                result = new Range(lowest, highest);
            }
        }

        return result;

    }

    /**
     * Calculates the Y data range.
     *
     * @param data  the data.
     *
     * @return The range.
     */
    private Range calculateYDataRange(double[][] data) {

        Range result = null;
        //      double[][] data =  (double[][]) series.get(0);
        if (data != null) {
            double lowest  = Double.POSITIVE_INFINITY;
            double highest = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < data[0].length; i++) {
                double v = data[1][i];
                if (v < lowest) {
                    lowest = v;
                }
                if (v > highest) {
                    highest = v;
                }
            }
            if (lowest <= highest) {
                result = new Range(lowest, highest);
            }
        }
        return result;

    }







}

