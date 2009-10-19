/**
 * $Id: ViewManager.java,v 1.401 2007/08/16 14:05:04 jeffmc Exp $
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
 * This library is distributed in the hope that it will be2 useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */






package ucar.unidata.idv.flythrough;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CompassPlot;
import org.jfree.chart.plot.DialShape;
import org.jfree.chart.plot.MeterInterval;
import org.jfree.chart.plot.MeterPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ThermometerPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.dial.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


import org.jfree.ui.RectangleInsets;


import ucar.unidata.idv.control.ReadoutInfo;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;


import java.util.ArrayList;

import java.util.HashSet;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 *
 * @author IDV development team
 */

public class ChartDecorator extends FlythroughDecorator {


    /** _more_ */
    public static final Color[] COLORS = {
        Color.blue, Color.red, Color.green, Color.orange, Color.cyan,
        Color.magenta, Color.pink, Color.yellow
    };

    /** We cache the chart image to save some time in the redraw */
    private Image lastChartImage;




    /**
     * _more_
     */
    public ChartDecorator() {}

    /**
     * _more_
     *
     * @param flythrough _more_
     */
    public ChartDecorator(Flythrough flythrough) {
        super(flythrough);
    }


    /**
     * _more_
     *
     * @param samples _more_
     *
     * @throws Exception _more_
     */
    public void handleReadout(List<ReadoutInfo> samples) throws Exception {}




    /**
     * _more_
     *
     * @return _more_
     */
    public String getName() {
        return "track chart";
    }


    /**
     * _more_
     */
    public void clearSamples() {
        lastChartImage = null;
    }

    /**
     * _more_
     *
     * @param g2 _more_
     * @param comp _more_
     *
     * @return _more_
     */
    public boolean paintDashboard(Graphics2D g2, JComponent comp) {
        try {
            List<SampleInfo> infos =
                new ArrayList<SampleInfo>(flythrough.getSamples());
            if (infos.size() == 0) {
                return false;
            }
            Rectangle          b          = comp.getBounds();
            JFrame             dummyFrame = new JFrame("");
            XYSeriesCollection dataset    = new XYSeriesCollection();
            JFreeChart         chart      = Flythrough.createChart(dataset);
            XYPlot             xyPlot     = (XYPlot) chart.getPlot();

            int chartHeight =
                b.height - flythrough.getDashboardImage().getHeight(null);
            chartHeight = Math.max(chartHeight, 50);
            int   chartWidth = Math.min(chartHeight * 4, b.width);


            int   dx         = b.width / 2 - chartWidth / 2;
            int   dy         = 0;

            Image lastImage  = lastChartImage;
            if ((lastImage != null)
                    && (lastImage.getWidth(null) == chartWidth)
                    && (lastImage.getHeight(null) == chartHeight)) {
                g2.translate(dx, dy);
                g2.drawImage(lastImage, 0, 0, null);
                g2.translate(-dx, -dy);
                return false;
            }



            for (int i = 0; i < infos.size(); i++) {
                SampleInfo info      = infos.get(i);
                ValueAxis  rangeAxis = new NumberAxis(info.getName());
                if (info.getRange() != null) {
                    rangeAxis.setRange(
                        new org.jfree.data.Range(
                            info.getRange().getMin(),
                            info.getRange().getMax()));
                }
                dataset = new XYSeriesCollection();
                dataset.addSeries(info.getSeries());
                xyPlot.setRangeAxis(i, rangeAxis, false);
                xyPlot.setDataset(i, dataset);
                xyPlot.mapDatasetToRangeAxis(i, i);
                final Color color = COLORS[i % COLORS.length];
                XYLineAndShapeRenderer renderer =
                    new XYLineAndShapeRenderer(true, false) {
                    public Paint xgetItemPaint(final int row,
                            final int column) {
                        return color;
                    }
                };
                renderer.setSeriesPaint(0, color);
                xyPlot.setRenderer(i, renderer);
            }

            ChartPanel chartPanel = new ChartPanel(chart);
            chartPanel.setPreferredSize(new Dimension(chartWidth,
                    chartHeight));
            dummyFrame.setContentPane(chartPanel);
            dummyFrame.pack();
            Image image = ImageUtils.getImage(chartPanel);
            lastChartImage = image;
            g2.translate(dx, dy);
            g2.drawImage(image, 0, 0, null);
            g2.translate(-dx, -dy);
        } catch (Exception exc) {
            logException("Painting chart", exc);

        }

        return false;


    }


}

