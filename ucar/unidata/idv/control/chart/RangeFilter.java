/*
 * $Id: RangeFilter.java,v 1.18 2007/05/04 14:50:05 dmurray Exp $
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
import org.jfree.chart.annotations.*;
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



import ucar.unidata.data.DataChoice;
import ucar.unidata.data.sounding.TrackDataSource;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionRect;




import ucar.unidata.geoloc.projection.*;
import ucar.unidata.util.GuiUtils;


import ucar.unidata.util.LogUtil;
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
 * Class RangeFilter is a greater than or less than value filter
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.18 $
 */
public class RangeFilter extends ChartAnnotation {

    /** Type */
    public static int TYPE_LESSTHAN = 0;

    /** Type */
    public static int TYPE_GREATERTHAN = 1;


    /** Arrow width */
    public static final int ANNOTATION_WIDTH = 10;

    /** my type */
    private int type = TYPE_LESSTHAN;

    /** current value */
    private double rangeValue = 0.0;



    /** widget for properties */
    private JRadioButton lessThanButton;

    /** widget for properties */
    private JRadioButton greaterThanButton;

    /** widget for properties */
    private JTextField valueFld;

    /** my attached filter. may be null. */
    private RangeFilter attached;


    /**
     * Default ctor
     */
    public RangeFilter() {}


    /**
     * ctor
     *
     * @param rangeValue the value
     * @param timeseries the chart I'm in
     */
    public RangeFilter(double rangeValue, TimeSeriesChartWrapper timeseries) {
        super(timeseries);
        this.rangeValue = rangeValue;
    }


    /**
     * Get the tool tip text
     *
     * @return Tool tip text
     */
    public String getToolTipText() {
        if (attached == null) {
            return "<html>Range Filter: <b>" + formatValue(getRangeValue())
                   + "</b></html>";
        }
        String lessThan    = ((type == TYPE_LESSTHAN)
                              ? formatValue(getRangeValue())
                              : formatValue(attached.getRangeValue()));
        String greaterThan = ((type == TYPE_GREATERTHAN)
                              ? formatValue(getRangeValue())
                              : formatValue(attached.getRangeValue()));
        return "<html> Range Filter: <b>" + lessThan + " - " + greaterThan
               + " </b></html>";
    }


    /**
     * removed from the chart. Clear out the attached reference to me.
     */
    public void doRemove() {
        super.doRemove();
        if (attached != null) {
            attached.attached = null;
        }
    }


    /**
     * Type of annotation
     *
     * @return Type of annotation
     */
    public String getTypeName() {
        return "Range Filter";
    }

    /**
     * Does the value pass the filter
     *
     * @param value The value
     *
     * @return Passes the filter
     */
    public boolean valueOk(double value) {
        if (type == TYPE_LESSTHAN) {
            return value <= rangeValue;
        } else {
            return value >= rangeValue;
        }
    }


    /**
     * Apply the properties
     *
     * @return Success
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        if (lessThanButton.isSelected()) {
            type = TYPE_LESSTHAN;
        } else {
            type = TYPE_GREATERTHAN;
        }
        rangeValue = Misc.parseNumber(valueFld.getText().trim());
        return true;
    }

    /**
     * Create property left/right components
     *
     *
     * @param comps List of components for properties dialog
     * @param tabIdx Which tab in the gui
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx != 0) {
            return;
        }
        comps.add(GuiUtils.rLabel("Type: "));
        lessThanButton = new JRadioButton("Less Than", type == TYPE_LESSTHAN);
        greaterThanButton = new JRadioButton("Greater Than",
                                             type == TYPE_GREATERTHAN);
        GuiUtils.buttonGroup(lessThanButton, greaterThanButton);
        comps.add(GuiUtils.left(GuiUtils.hbox(lessThanButton,
                greaterThanButton)));
        comps.add(GuiUtils.rLabel("Range Value: "));
        comps.add(valueFld = new JTextField("" + rangeValue));
    }




    /**
     * Set the position to the given x/y screen coordinate
     *
     * @param x x
     * @param y y
     */
    public void setPosition(int x, int y) {
        TimeSeriesChartWrapper tscw =
            (TimeSeriesChartWrapper) getPlotWrapper();
        double rangeValue = tscw.getRangeValue(y);
        setRangeValue(rangeValue);
    }


    /**
     * Set the position from the mouse
     *
     * @param event mouse event
     */
    public void setPosition(MouseEvent event) {
        int diff = event.getY() - y;
        setPosition(event.getX(), event.getY());
        if ((attached != null) && event.isShiftDown()) {
            attached.setPosition(event.getX(), attached.y + diff);
        }
    }



    /**
     * Draws the annotation.
     *
     * @param g2  the graphics device.
     * @param plot  the plot.
     * @param dataArea  the data area.
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param rendererIndex  the renderer index.
     * @param info  an optional info object that will be populated with
     *              entity information.
     */
    public void draw(Graphics2D g2, XYPlot plot, Rectangle2D dataArea,
                     ValueAxis domainAxis, ValueAxis rangeAxis,
                     int rendererIndex, PlotRenderingInfo info) {
        super.setGraphicsState(g2);
        if ( !getPlotWrapper().okToDraw(this)) {
            return;
        }
        g2.setStroke(new BasicStroke());
        boolean selected = getSelected();
        if (attached != null) {
            selected |= attached.getSelected();
        }


        if (selected) {
            g2.setColor(COLOR_SELECTED);
        } else {
            g2.setColor(getColor());
        }
        y = (int) rangeAxis.valueToJava2D(rangeValue, dataArea,
                                          RectangleEdge.LEFT);


        int width  = (int) ANNOTATION_WIDTH;
        int width2 = (int) (ANNOTATION_WIDTH / 2);
        x = (int) dataArea.getX();
        //        System.err.println("x/y:" + x +"/" +y);


        int[] xs;
        int[] ys;
        if (type == TYPE_LESSTHAN) {
            xs = new int[] { x, x + width, x + width2, x };
            ys = new int[] { y, y, y + width, y };
        } else {
            xs = new int[] { x, x + width, x + width2, x };
            ys = new int[] { y, y, y - width, y };


        }
        g2.fillPolygon(xs, ys, xs.length);


        g2.setColor(Color.gray);
        g2.drawLine(x + width, y,
                    (int) (dataArea.getX() + dataArea.getWidth()), y);

        if ((attached != null) && (type == TYPE_LESSTHAN)) {
            int otherY = (int) rangeAxis.valueToJava2D(attached.rangeValue,
                             dataArea, RectangleEdge.LEFT);

            g2.drawLine(x + width2, y + width, x + width2, otherY - width);
        }
    }




    /**
     *  Set the DomainValue property.
     *
     *  @param value The new value for RangeValue
     */
    public void setRangeValue(double value) {
        rangeValue = value;
    }



    /**
     *  Get the RangeValue property.
     *
     *  @return The RangeValue
     */
    public double getRangeValue() {
        return rangeValue;
    }


    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(int value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public int getType() {
        return type;
    }

    /**
     * Make the attached, but opposite,  range filter
     *
     * @param event The event
     *
     * @return The attached range filter positioned near me.
     */
    public RangeFilter doMakeAttached(MouseEvent event) {
        if (attached != null) {
            return attached;
        }
        TimeSeriesChartWrapper tscw =
            (TimeSeriesChartWrapper) getPlotWrapper();


        if (type == TYPE_GREATERTHAN) {
            attached = new RangeFilter(tscw.getRangeValue(event.getY() - 30),
                                       tscw);
            attached.setType(TYPE_LESSTHAN);
        } else {
            attached = new RangeFilter(tscw.getRangeValue(event.getY() + 30),
                                       tscw);
            attached.setType(TYPE_GREATERTHAN);
        }
        attached.setAttached(this);

        return attached;
    }


    /**
     *  Set the Attached property.
     *
     *  @param value The new value for Attached
     */
    public void setAttached(RangeFilter value) {
        attached = value;
    }

    /**
     *  Get the Attached property.
     *
     *  @return The Attached
     */
    public RangeFilter getAttached() {
        return attached;
    }



}

