/*
 * $Id: MyTimeSeriesPlot.java,v 1.13 2007/04/16 21:32:11 jeffmc Exp $
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
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;

//import ucar.unidata.util.ColorTable;


import ucar.unidata.data.DataChoice;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;



import visad.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;


import java.rmi.RemoteException;

import java.text.SimpleDateFormat;



import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;




/**
 * Class MyTimeSeriesPlot
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.13 $
 */
public class MyTimeSeriesPlot extends XYPlot {

    /** The chart wrapper */
    private TimeSeriesChartWrapper timeseries;

    /** ignore changes */
    private boolean ignoreDataSetChanges = false;


    /**
     * ctor
     *
     *
     * @param timeseries timeseries
     * @param dataset dataset
     * @param timeAxis time axis
     * @param valueAxis value axis
     */
    public MyTimeSeriesPlot(TimeSeriesChartWrapper timeseries,
                            XYDataset dataset, ValueAxis timeAxis,
                            ValueAxis valueAxis) {
        super(dataset, timeAxis, valueAxis, null);
        this.timeseries = timeseries;
    }




    /**
     * draw
     *
     * @param g2 the graphics
     * @param dataArea where the data area is
     * @param index which data set
     * @param info info
     * @param crosshairState crosshairState
     *
     * @return any drawn
     */
    public boolean render(Graphics2D g2, Rectangle2D dataArea, int index,
                          PlotRenderingInfo info,
                          CrosshairState crosshairState) {

        XYDataset dataset = getDataset(index);
        if (DatasetUtilities.isEmptyOrNull(dataset)) {
            return false;
        }

        ValueAxis     rangeAxis          = getRangeAxisForDataset(index);
        ValueAxis     domainAxis         = getDomainAxisForDataset(index);
        AxisLocation  rangeAxisLocation  = getRangeAxisLocation(index);
        AxisLocation  domainAxisLocation = getDomainAxisLocation(index);
        RectangleEdge rangeEdge          = getRangeAxisEdge();
        RectangleEdge domainEdge         = getDomainAxisEdge();
        int           seriesCount        = dataset.getSeriesCount();
        //        System.out.println ("********************************");
        for (int series = seriesCount - 1; series >= 0; series--) {
            int   itemCount = dataset.getItemCount(series);
            int[] xs        = new int[itemCount];
            int[] ys        = new int[itemCount];
            g2.setStroke(
                getRendererForDataset(dataset).getSeriesStroke(series));
            g2.setPaint(
                getRendererForDataset(dataset).getSeriesPaint(series));
            int pointCnt = 0;
            for (int item = 0; item < itemCount; item++) {
                double x1 = dataset.getXValue(series, item);
                double y1 = dataset.getYValue(series, item);
                if ( !timeseries.valuesOk(index, x1, y1)) {
                    if (pointCnt > 0) {
                        g2.drawPolyline(xs, ys, pointCnt);
                        pointCnt = 0;
                    }
                    continue;
                }
                double transX = domainAxis.valueToJava2D(x1, dataArea,
                                    domainEdge);
                double transY = rangeAxis.valueToJava2D(y1, dataArea,
                                    rangeEdge);
                if ( !dataArea.contains(transX, transY)) {
                    continue;
                }


                xs[pointCnt] = (int) (transX + 0.5);
                ys[pointCnt] = (int) (transY + 0.5);
                pointCnt++;
                if (pointCnt > 10) {
                    g2.drawPolyline(xs, ys, pointCnt);
                    xs[0]    = xs[pointCnt - 1];
                    ys[0]    = ys[pointCnt - 1];
                    pointCnt = 1;
                }

            }
            if (pointCnt > 1) {
                g2.drawPolyline(xs, ys, pointCnt);
            }
            long t2 = System.currentTimeMillis();
            //            System.err.println("time:" + (t2 - t1) + "ms #" + itemCount);
        }
        return true;
    }


    /**
     * Override this method because it gets called after the graphics clip
     * has been reset.
     * TODO: We end up drawing the annotations twice. Figure something out to
     * only draw once.
     *
     * @param g2  the graphics
     * @param dataArea the data area
     */
    public void drawOutline(Graphics2D g2, Rectangle2D dataArea) {
        super.drawOutline(g2, dataArea);
        Shape  originalClip = g2.getClip();
        double y            = dataArea.getY();
        Rectangle2D.Double newClip = new Rectangle2D.Double(dataArea.getX(),
                                         0, dataArea.getWidth(),
                                         dataArea.getHeight() + y);

        g2.clip(newClip);
        drawAnnotations(g2, dataArea, null);
        g2.clip(originalClip);
    }


    /**
     * intercept event
     *
     * @param event The event
     */
    public void datasetChanged(DatasetChangeEvent event) {
        if (ignoreDataSetChanges) {
            return;
        }
        super.datasetChanged(event);
    }


    /**
     * ignore changes
     *
     * @param b ignore changes_
     */
    public void setIgnoreDataSetChanges(boolean b) {
        boolean fireChange = ((b != ignoreDataSetChanges) && !b);
        ignoreDataSetChanges = b;
        if (fireChange) {
            datasetChanged(new DatasetChangeEvent(this, getDataset()));
        }
    }




}

