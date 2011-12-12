/**
 * $Id: XYChartManager.java,v 1.4 2007/04/16 21:32:12 jeffmc Exp $
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
import org.jfree.chart.event.*;
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


import ucar.unidata.idv.ControlContext;


import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.idv.control.ProbeRowInfo;


import ucar.unidata.idv.control.chart.LineState;
import ucar.unidata.ui.GraphPaperLayout;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.ui.symbol.*;


import ucar.unidata.ui.symbol.StationModelManager;
import ucar.unidata.ui.symbol.WindBarbSymbol;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Range;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.view.geoloc.NavigatedDisplay;


import ucar.visad.ShapeUtility;

import ucar.visad.Util;



import ucar.visad.display.Animation;
import ucar.visad.display.Animation;
import ucar.visad.display.StationModelDisplayable;
import ucar.visad.quantities.CommonUnits;


import visad.*;

import visad.georef.EarthLocation;
import visad.georef.LatLonPoint;

import visad.georef.MapProjection;

import visad.util.BaseRGBMap;
import visad.util.ColorPreview;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;


import java.rmi.RemoteException;


import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import java.util.ArrayList;


import java.util.Date;
import java.util.Hashtable;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;



/**
 * A time series chart
 *
 * @author MetApps Development Team
 * @version $Revision: 1.4 $
 */

public abstract class XYChartManager extends ChartManager {

    /** For showing when chart is empty */
    private XYAnnotation emptyChartAnnotation;

    /** Label to show when chart is empty */
    private String emptyChartLabel = "";



    /**
     * ctor
     */
    public XYChartManager() {}


    /**
     * Default constructor.
     *
     * @param control my control
     */
    public XYChartManager(DisplayControlImpl control) {
        super(control);
    }

    /**
     * Default constructor.
     *
     * @param control my control
     * @param chartName my name
     */
    public XYChartManager(DisplayControlImpl control, String chartName) {
        super(control, chartName);
    }




    /**
     * Get the renderer for the given line
     *
     * @param lineState The line
     *
     * @return renderer
     */
    protected XYItemRenderer getRenderer(LineState lineState) {
        return getRenderer(lineState, true);
    }

    /**
     * Get the renderer for the given line
     *
     * @param lineState The line
     * @param showLegend And show the legend
     *
     * @return renderer
     */
    protected XYItemRenderer getRenderer(LineState lineState,
                                         boolean showLegend) {
        int            lineType = lineState.getLineType();
        XYItemRenderer renderer = null;
        if (lineType == LineState.LINETYPE_BAR) {
            return new MyXYBarRenderer();
        } else if (lineType == LineState.LINETYPE_SHAPES) {
            renderer = new MyXYAreaRenderer(lineState, XYAreaRenderer.SHAPES,
                                            showLegend);
        } else if (lineType == LineState.LINETYPE_LINES) {
            return new MyXYAreaRenderer(lineState, XYAreaRenderer.LINES,
                                        showLegend);
        } else if (lineType == LineState.LINETYPE_AREA) {
            return new MyXYAreaRenderer(lineState, XYAreaRenderer.AREA,
                                        showLegend);
        } else if (lineType == LineState.LINETYPE_AREA_AND_SHAPES) {
            renderer = new MyXYAreaRenderer(lineState,
                                            XYAreaRenderer.AREA_AND_SHAPES,
                                            showLegend);
        } else {
            renderer = new MyXYAreaRenderer(lineState,
                                            XYAreaRenderer.SHAPES_AND_LINES);
        }

        Shape shape = lineState.getPaintShape();
        if (shape != null) {
            renderer.setShape(shape);
            renderer.setBaseShape(shape);
            if (renderer instanceof XYAreaRenderer) {
                ((XYAreaRenderer) renderer).setLegendArea(shape);
            }
        }
        return renderer;

    }


    /**
     * Class MyXYAreaRenderer for rendering areas
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.4 $
     */
    protected class MyXYAreaRenderer extends XYAreaRenderer {

        /** the line */
        LineState lineState;

        /** shape to draw */
        Shape shape;

        /** Do we have a shape to draw */
        boolean hasShape;

        /** _more_          */
        boolean showLegend = true;

        /**
         * ctor
         *
         * @param lineState line
         * @param type type
         */
        public MyXYAreaRenderer(LineState lineState, int type) {
            this(lineState, type, true);
        }

        /**
         * _more_
         *
         * @param lineState _more_
         * @param type _more_
         * @param showLegend _more_
         */
        public MyXYAreaRenderer(LineState lineState, int type,
                                boolean showLegend) {
            super(type);
            this.showLegend = showLegend;
            this.lineState  = lineState;
            shape           = lineState.getPaintShape();
            int lineType = lineState.getLineType();
            hasShape = (lineType == LineState.LINETYPE_SHAPES)
                       || (lineType == LineState.LINETYPE_AREA_AND_SHAPES)
                       || (lineType == LineState.LINETYPE_SHAPES_AND_LINES);
        }


        /**
         * Get item for legend
         *
         * @param datasetIndex Which data set
         * @param series Which time series
         *
         * @return legend item
         */
        public LegendItem getLegendItem(int datasetIndex, int series) {
            if ( !showLegend) {
                return null;
            }
            LegendItem l = super.getLegendItem(datasetIndex, series);
            if ( !hasShape) {
                return l;
            }
            Paint p = l.getFillPaint();
            l = new LegendItem(l.getLabel(), l.getDescription(),
                               l.getToolTipText(), l.getURLText(), true,
                               shape, false, p, true, p,
                               l.getOutlineStroke(), true, shape,
                               l.getLineStroke(), p);
            return l;
        }
    }

    ;



    /**
     *   a cut and paste so we can draw bars with a fixed width
     */
    protected static class MyXYBarRenderer extends XYBarRenderer {

        /**
         * draw
         *
         * @param g2 param
         * @param state param
         * @param dataArea param
         * @param info param
         * @param plot param
         * @param domainAxis param
         * @param rangeAxis param
         * @param dataset param
         * @param series param
         * @param item param
         * @param crosshairState param
         * @param pass param
         */
        public void drawItem(Graphics2D g2, XYItemRendererState state,
                             Rectangle2D dataArea, PlotRenderingInfo info,
                             XYPlot plot, ValueAxis domainAxis,
                             ValueAxis rangeAxis, XYDataset dataset,
                             int series, int item,
                             CrosshairState crosshairState, int pass) {

            if ( !getItemVisible(series, item)) {
                return;
            }
            IntervalXYDataset intervalDataset = (IntervalXYDataset) dataset;

            double            value0;
            double            value1;
            if (this.getUseYInterval()) {
                value0 = intervalDataset.getStartYValue(series, item);
                value1 = intervalDataset.getEndYValue(series, item);
            } else {
                value0 = this.getBase();
                value1 = intervalDataset.getYValue(series, item);
            }
            if (Double.isNaN(value0) || Double.isNaN(value1)) {
                return;
            }

            double translatedValue0 = rangeAxis.valueToJava2D(value0,
                                          dataArea, plot.getRangeAxisEdge());
            double translatedValue1 = rangeAxis.valueToJava2D(value1,
                                          dataArea, plot.getRangeAxisEdge());

            RectangleEdge location = plot.getDomainAxisEdge();
            double startX = intervalDataset.getStartXValue(series, item);
            if (Double.isNaN(startX)) {
                return;
            }
            double translatedStartX = domainAxis.valueToJava2D(startX,
                                          dataArea, location);

            double endX = intervalDataset.getEndXValue(series, item);
            if (Double.isNaN(endX)) {
                return;
            }
            double translatedEndX = domainAxis.valueToJava2D(endX, dataArea,
                                        location);

            double translatedWidth = Math.max(1,
                                         Math.abs(translatedEndX
                                             - translatedStartX));
            double translatedHeight = Math.abs(translatedValue1
                                          - translatedValue0);

            if (getMargin() > 0.0) {
                double cut = translatedWidth * getMargin();
                translatedWidth  = translatedWidth - cut;
                translatedStartX = translatedStartX + cut / 2;
            }

            translatedStartX -= 4;
            translatedWidth  += 8;
            Rectangle2D     bar         = null;
            PlotOrientation orientation = plot.getOrientation();
            if (orientation == PlotOrientation.HORIZONTAL) {
                bar = new Rectangle2D.Double(Math.min(translatedValue0,
                        translatedValue1), Math.min(translatedStartX,
                            translatedEndX), translatedHeight,
                                             translatedWidth);
            } else if (orientation == PlotOrientation.VERTICAL) {
                bar = new Rectangle2D.Double(Math.min(translatedStartX,
                        translatedEndX), Math.min(translatedValue0,
                            translatedValue1), translatedWidth,
                                translatedHeight);
            }

            Paint itemPaint = getItemPaint(series, item);
            if ((getGradientPaintTransformer() != null)
                    && (itemPaint instanceof GradientPaint)) {
                GradientPaint gp = (GradientPaint) itemPaint;
                itemPaint = getGradientPaintTransformer().transform(gp, bar);
            }
            g2.setPaint(itemPaint);
            g2.fill(bar);
            if (isDrawBarOutline()
                    && (Math.abs(translatedEndX - translatedStartX) > 3)) {
                Stroke stroke = getItemOutlineStroke(series, item);
                Paint  paint  = getItemOutlinePaint(series, item);
                if ((stroke != null) && (paint != null)) {
                    g2.setStroke(stroke);
                    g2.setPaint(paint);
                    g2.draw(bar);
                }
            }

            if (isItemLabelVisible(series, item)) {
                XYItemLabelGenerator generator =
                    getItemLabelGenerator(series, item);
                drawItemLabel(g2, dataset, series, item, plot, generator,
                              bar, value1 < 0.0);
            }

            // update the crosshair point
            double x1      = (startX + endX) / 2.0;
            double y1      = dataset.getYValue(series, item);
            double transX1 = domainAxis.valueToJava2D(x1, dataArea, location);
            double transY1 = rangeAxis.valueToJava2D(y1, dataArea,
                                 plot.getRangeAxisEdge());
            updateCrosshairValues(crosshairState, x1, y1, transX1, transY1,
                                  plot.getOrientation());

            // add an entity for the item...
            if (info != null) {
                EntityCollection entities =
                    info.getOwner().getEntityCollection();
                if (entities != null) {
                    String tip = null;
                    XYToolTipGenerator generator =
                        getToolTipGenerator(series, item);
                    if (generator != null) {
                        tip = generator.generateToolTip(dataset, series,
                                item);
                    }
                    String url = null;
                    if (getURLGenerator() != null) {
                        url = getURLGenerator().generateURL(dataset, series,
                                item);
                    }
                    XYItemEntity entity = new XYItemEntity(bar, dataset,
                                              series, item, tip, url);
                    entities.add(entity);
                }
            }

        }

    }

    ;



    /** a cut and paste so we can draw bars with a fixed width */
    protected static class CloudCoverageRenderer extends AbstractXYItemRenderer {

        /** scale */
        private double scale = 0;

        /** line info */
        LineState lineState;

        /**
         * ctor
         *
         * @param lineState line
         * @param scale scale
         */
        public CloudCoverageRenderer(LineState lineState, double scale) {
            this.lineState = lineState;
            this.scale     = scale;
        }

        /**
         * Get item for legend
         *
         * @param datasetIndex Which data set
         * @param series Which time series
         *
         * @return legend item
         */
        public LegendItem getLegendItem(int datasetIndex, int series) {
            GeneralPath path = new GeneralPath();
            path.append(new Ellipse2D.Double(0, 0, 10, 10), false);
            path.moveTo(5.0f, 0.0f);
            path.lineTo(5.0f, 10.0f);
            path.closePath();
            LegendItem l = super.getLegendItem(datasetIndex, series);
            l = new LegendItem(l.getLabel(), l.getDescription(),
                               l.getToolTipText(), l.getURLText(), true,
                               path, false, Color.black, true, Color.black,
                               l.getOutlineStroke(), false, l.getLine(),
                               l.getLineStroke(), Color.black);
            return l;
        }



        /**
         * draw
         *
         * @param g2 param
         * @param state param
         * @param dataArea param
         * @param info param
         * @param plot param
         * @param domainAxis param
         * @param rangeAxis param
         * @param dataset param
         * @param series param
         * @param item param
         * @param crosshairState param
         * @param pass param
         */
        public void drawItem(Graphics2D g2, XYItemRendererState state,
                             Rectangle2D dataArea, PlotRenderingInfo info,
                             XYPlot plot, ValueAxis domainAxis,
                             ValueAxis rangeAxis, XYDataset dataset,
                             int series, int item,
                             CrosshairState crosshairState, int pass) {

            if ( !getItemVisible(series, item)) {
                return;
            }
            double x     = dataset.getXValue(series, item);
            double value = dataset.getYValue(series, item);
            if (scale != 0) {
                value = value * scale;
            }

            if (Double.isNaN(x) || Double.isNaN(value)) {
                return;
            }
            int sX = (int) domainAxis.valueToJava2D(x, dataArea,
                         plot.getDomainAxisEdge());
            try {
                int top    = (int) (dataArea.getY());
                int bottom = (int) (top + dataArea.getHeight());
                int mid    = top + (bottom - top) / 2;
                int vAnchor;
                int w                = 16;
                int w2               = w / 2;


                int verticalPosition = lineState.getVerticalPosition();
                if (verticalPosition == LineState.VPOS_TOP) {
                    vAnchor = top;
                } else if (verticalPosition == LineState.VPOS_MIDDLE) {
                    vAnchor = mid - w2;
                } else {
                    vAnchor = bottom - w;
                }

                int angle = 0;
                if (value == 0) {}
                else if (value < 2) {
                    angle = 90;
                } else if (value < 4) {
                    angle = 180;
                } else if (value < 6) {
                    angle = 270;
                } else {
                    angle = 360;
                }
                g2.setColor(Color.black);
                g2.setStroke(new BasicStroke());
                g2.fillArc(sX - w2, vAnchor, w, w, 90, -angle);
                g2.drawArc(sX - w2, vAnchor, w, w, 0, 360);
            } catch (Exception exc) {
                System.err.println("oops:" + exc);
            }

        }
    }

    ;




    /** a cut and paste so we can draw bars with a fixed width */
    protected static class TextRenderer extends AbstractXYItemRenderer {

        /** List of strings to draw */
        List textList;

        /** line state */
        LineState lineState;

        /**
         * ctor
         *
         * @param textList List of strings to draw
         * @param lineState line state
         */
        public TextRenderer(List textList, LineState lineState) {
            this.textList  = textList;
            this.lineState = lineState;
        }

        /**
         * Get item for legend
         *
         * @param datasetIndex Which data set
         * @param series Which time series
         *
         * @return legend item
         */
        public LegendItem getLegendItem(int datasetIndex, int series) {
            LegendItem l = super.getLegendItem(datasetIndex, series);
            l = new LegendItem(l.getLabel(), l.getDescription(),
                               l.getToolTipText(), l.getURLText(), true,
                               new Rectangle(0, 0, 8, 8), true,
                               getSeriesPaint(series), false, Color.black,
                               l.getOutlineStroke(), false, l.getLine(),
                               l.getLineStroke(), Color.black);
            return l;
        }

        /**
         * draw
         *
         * @param g2 param
         * @param state param
         * @param dataArea param
         * @param info param
         * @param plot param
         * @param domainAxis param
         * @param rangeAxis param
         * @param dataset param
         * @param series param
         * @param item param
         * @param crosshairState param
         * @param pass param
         */
        public void drawItem(Graphics2D g2, XYItemRendererState state,
                             Rectangle2D dataArea, PlotRenderingInfo info,
                             XYPlot plot, ValueAxis domainAxis,
                             ValueAxis rangeAxis, XYDataset dataset,
                             int series, int item,
                             CrosshairState crosshairState, int pass) {

            if ( !getItemVisible(series, item)) {
                return;
            }
            String text = (String) textList.get(item);
            double x    = dataset.getXValue(series, item);
            if (Double.isNaN(x)) {
                return;
            }
            int sX = (int) domainAxis.valueToJava2D(x, dataArea,
                         plot.getDomainAxisEdge());
            try {
                int         top    = (int) (dataArea.getY());
                int         bottom = (int) (top + dataArea.getHeight());
                int         mid    = top + (bottom - top) / 2;
                int         vAnchor;
                FontMetrics fm               = g2.getFontMetrics();

                int         width            = fm.stringWidth(text);
                int         height = (fm.getMaxDescent() + fm.getMaxAscent());

                int         verticalPosition =
                    lineState.getVerticalPosition();
                if (verticalPosition == LineState.VPOS_TOP) {
                    vAnchor = top + height;
                } else if (verticalPosition == LineState.VPOS_MIDDLE) {
                    vAnchor = mid - height / 2;
                } else {
                    vAnchor = bottom;
                }

                g2.setPaint(getSeriesPaint(series));
                g2.drawString(text, sX - width / 2, vAnchor);
            } catch (Exception exc) {
                System.err.println("oops:" + exc);
            }

        }
    }

    ;





    /** displays windw barbs */
    protected static class WindbarbRenderer extends AbstractXYItemRenderer {

        /** drawer */
        WindBarbSymbol.WindDrawer drawer;

        /** speed data */
        Series speedSeries;

        /** direction data */
        Series dirSeries;

        /** unit */
        Unit speedUnit;

        /** line state */
        LineState lineState;

        /** Speed,Dir or U,V */
        boolean polarWind = true;

        /** Is in Southern hemisphere? */
        boolean isSouth = false;

        /**
         * ctor
         *
         * @param lineState line state
         * @param speedSeries speed data
         * @param dirSeries dir data
         * @param unit speed unit
         */
        public WindbarbRenderer(LineState lineState, Series speedSeries,
                                Series dirSeries, Unit unit) {
            this(lineState, speedSeries, dirSeries, unit, true);
        }


        /**
         * ctor
         *
         * @param lineState line state
         * @param speedSeries speed data
         * @param dirSeries dir data
         * @param unit speed unit
         * @param polarWind true if polar coords
         */
        public WindbarbRenderer(LineState lineState, Series speedSeries,
                                Series dirSeries, Unit unit,
                                boolean polarWind) {
            this.lineState   = lineState;
            this.speedUnit   = unit;
            this.speedSeries = speedSeries;
            this.dirSeries   = dirSeries;
            this.polarWind   = polarWind;
        }

        /**
         * Get item for legend
         *
         * @param datasetIndex Which data set
         * @param series Which time series
         *
         * @return legend item
         */
        public LegendItem getLegendItem(int datasetIndex, int series) {
            GeneralPath path = new GeneralPath();
            path.moveTo(0.0f, 0.0f);
            path.lineTo(10.f, 10.f);

            path.moveTo(0.f, 0.f);
            path.lineTo(3.0f, -3.0f);

            path.moveTo(3.f, 3.f);
            path.lineTo(3.f + 2.0f, 3.f - 2.0f);
            path.closePath();
            LegendItem l = super.getLegendItem(datasetIndex, series);
            l = new LegendItem(l.getLabel(), l.getDescription(),
                               l.getToolTipText(), l.getURLText(), true,
                               path, false, Color.black, true, Color.black,
                               l.getOutlineStroke(), false, l.getLine(),
                               l.getLineStroke(), Color.black);
            return l;
        }



        /**
         * draw
         *
         * @param g2 param
         * @param state param
         * @param dataArea param
         * @param info param
         * @param plot param
         * @param domainAxis param
         * @param rangeAxis param
         * @param dataset param
         * @param series param
         * @param item param
         * @param crosshairState param
         * @param pass param
         */
        public void drawItem(Graphics2D g2, XYItemRendererState state,
                             Rectangle2D dataArea, PlotRenderingInfo info,
                             XYPlot plot, ValueAxis domainAxis,
                             ValueAxis rangeAxis, XYDataset dataset,
                             int series, int item,
                             CrosshairState crosshairState, int pass) {

            if ( !getItemVisible(series, item)) {
                return;
            }
            if (item >= speedSeries.getItemCount()) {
                return;
            }
            if (item >= dirSeries.getItemCount()) {
                return;
            }
            double  speed = Double.NaN;
            double  dir   = Double.NaN;
            boolean isXY  = speedSeries instanceof XYSeries;

            if (isXY) {
                // axes are switched in VertProf - X is alt, Y is value
                speed = ((XYSeries) speedSeries).getY(item).doubleValue();
                dir   = ((XYSeries) dirSeries).getY(item).doubleValue();
            } else {
                speed =
                    ((TimeSeries) speedSeries).getValue(item).doubleValue();
                dir = ((TimeSeries) dirSeries).getValue(item).doubleValue();
            }
            if ( !polarWind) {
                double u = speed;
                double v = dir;
                speed = Math.sqrt(u * u + v * v);
                dir   = Math.toDegrees(Math.atan2(-u, -v));
                if (dir < 0) {
                    dir += 360;
                }
            }
            //System.err.println("spd/dir = " + speed+ "/" + dir);

            int    xPos, yPos;
            double x = (isXY)
                       ? dataset.getYValue(series, item)
                       : dataset.getXValue(series, item);
            //System.out.println("x = " + x);

            if (Double.isNaN(x) || Double.isNaN(speed) || Double.isNaN(dir)) {
                return;
            }
            int top    = (int) (dataArea.getY());
            int bottom = (int) (top + dataArea.getHeight());
            int left   = (int) (dataArea.getX());
            int right  = (int) (left + dataArea.getWidth());
            int midW   = left + (right - left) / 2;
            int mid    = top + (bottom - top) / 2;
            int vAnchor;
            int hAnchor;
            int w      = 20;
            int w2     = w / 2;
            int wiggle = 15;


            if (isXY) {

                double y = dataset.getXValue(series, item);
                //xPos = (int) rangeAxis.valueToJava2D(x, dataArea,
                //             plot.getRangeAxisEdge());
                int horizontalPosition = lineState.getHorizontalPosition();
                if (horizontalPosition == LineState.HPOS_LEFT) {
                    hAnchor = left + wiggle;
                } else if (horizontalPosition == LineState.HPOS_MIDDLE) {
                    hAnchor = midW;
                } else if (horizontalPosition == LineState.HPOS_RIGHT) {
                    hAnchor = right - wiggle;
                } else {
                    hAnchor = midW;
                }
                xPos = hAnchor;
                yPos = (int) domainAxis.valueToJava2D(y, dataArea,
                        plot.getDomainAxisEdge());

            } else {

                int sX = (int) domainAxis.valueToJava2D(x, dataArea,
                             plot.getDomainAxisEdge());
                int sY = (int) rangeAxis.valueToJava2D(speed, dataArea,
                             plot.getRangeAxisEdge());
                //System.out.println("sX/sY = " + sX + "/" + sY);

                int verticalPosition = lineState.getVerticalPosition();
                if (verticalPosition == LineState.VPOS_TOP) {
                    vAnchor = top + wiggle;
                } else if (verticalPosition == LineState.VPOS_MIDDLE) {
                    vAnchor = mid - w2;
                } else if (verticalPosition == LineState.VPOS_BOTTOM) {
                    vAnchor = bottom - w - wiggle;
                } else {
                    vAnchor = sY;
                }
                xPos = sX - w2;
                yPos = vAnchor;
            }

            try {
                speed = CommonUnits.KNOT.toThis(speed, speedUnit);
            } catch (VisADException vex) {
                System.err.println("error:" + vex);
            }


            if (drawer == null) {
                drawer = new WindBarbSymbol.WindDrawer(isSouth);
            }
            // System.err.println ("speed: " + speed +", dir: " + dir + ", X: " + xPos + ", Y: " + yPos);
            try {
                g2.setColor(Color.black);
                g2.setStroke(new BasicStroke());
                //drawer.draw(g2, sX - w2, vAnchor, w, w, speed, dir);
                drawer.draw(g2, xPos, yPos, w, w, speed, dir);
            } catch (Exception exc) {
                System.err.println("oops:" + exc);
            }
        }
    }



    /**
     * Set the label to use when we have an empty chart
     *
     * @param label empty chart label
     */
    public void setEmptyChartLabel(String label) {
        boolean signalChange = !Misc.equals(emptyChartLabel, label);
        if (emptyChartAnnotation == null) {
            signalChange         = true;
            emptyChartAnnotation = new XYAnnotation() {
                public void draw(Graphics2D g2, XYPlot plot,
                                 Rectangle2D dataArea, ValueAxis domainAxis,
                                 ValueAxis rangeAxis, int rendererIndex,
                                 PlotRenderingInfo info) {
                    if ( !hasStuff()) {
                        g2.setColor(Color.black);
                        g2.drawString(emptyChartLabel, 100, 50);
                    }
                }
            };
            for (int plotIdx = 0; plotIdx < chartHolders.size(); plotIdx++) {
                ChartHolder chartHolder =
                    (ChartHolder) chartHolders.get(plotIdx);
                ((XYPlot) chartHolder.getPlot()).addAnnotation(
                    emptyChartAnnotation);
            }
        }
        emptyChartLabel = label;
        if (signalChange) {
            signalChartChanged();
        }
    }



    /**
     * Add chart
     *
     * @param chartHolder new chart
     */
    protected void addChart(ChartHolder chartHolder) {
        super.addChart(chartHolder);
        if (emptyChartAnnotation != null) {
            ((XYPlot) chartHolder.getPlot()).addAnnotation(
                emptyChartAnnotation);
        }

    }



    /**
     * Class MyXYPlot is an xyplot with some special sauce to synchronize drawing
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.4 $
     */
    public class MyXYPlot extends XYPlot {

        /**
         * ctor
         *
         * @param dataset  dataset
         * @param domainAxis axis
         * @param rangeAxis axis
         * @param renderer renderer
         */
        public MyXYPlot(XYDataset dataset, ValueAxis domainAxis,
                        ValueAxis rangeAxis, XYItemRenderer renderer) {
            super(dataset, domainAxis, rangeAxis, renderer);
        }

        /**
         * draw synchronized
         *
         * @param g2 param
         * @param area param
         * @param anchor param
         * @param parentState param
         * @param info param
         */
        public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
                         PlotState parentState, PlotRenderingInfo info) {
            try {
                if ( !getSettingData()) {
                    synchronized (getMutex()) {
                        super.draw(g2, area, anchor, parentState, info);
                    }
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }

    }

    ;



}


