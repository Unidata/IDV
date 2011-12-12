/*
 * $Id: TrackSegment.java,v 1.12 2007/04/16 21:32:12 jeffmc Exp $
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
import ucar.unidata.util.StringUtil;

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
 * Provides a time series chart
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.12 $
 */

public class TrackSegment extends ChartAnnotation {

    /** Keep track of where the mouse was when we are about to be moved */
    boolean lastDistanceCheckToRight = false;

    /** Keep track of where the mouse was when we are about to be moved */
    boolean lastDistanceCheckToLeft = false;

    /** waypoint - may be the left or right */
    private WayPoint wayPoint1;

    /** waypoint - may be the left or right */
    private WayPoint wayPoint2;


    /**
     * Default ctor
     */
    public TrackSegment() {}

    /**
     * Ctor
     *
     * @param wayPoint1 wayPoint1
     * @param wayPoint2 wayPoint1
     * @param plotWrapper The chart I'm in
     */
    public TrackSegment(WayPoint wayPoint1, WayPoint wayPoint2,
                        PlotWrapper plotWrapper) {
        super(plotWrapper);
        this.wayPoint1 = wayPoint1;
        this.wayPoint2 = wayPoint2;
    }


    /**
     * Get the tool tip text
     *
     * @return Tool tip text
     */
    public String getToolTipText() {
        return "<html> Track Segment: <b>"
               + new Date((long) getLeft().getDomainValue()) + " - "
               + new Date((long) getRight().getDomainValue())
               + "</b> </html>";
    }

    /**
     * Type of annotation
     *
     * @return Type of annotation
     */
    public String getTypeName() {
        return "Track Segment";
    }

    /**
     * Set the position to the given x/y screen coordinate
     *
     * @param x x
     * @param y y
     */
    public void setPosition(int x, int y) {
        if (lastDistanceCheckToLeft) {
            getLeft().setPosition(x, y);
        } else if (lastDistanceCheckToRight) {
            getRight().setPosition(x, y);
        } else {
            TimeSeriesChartWrapper tscw =
                (TimeSeriesChartWrapper) getPlotWrapper();
            double domainValue = tscw.getDomainValue(x);
            double mid         = getCenterValue();
            double delta       = domainValue - mid;
            getLeft().setDomainValue(getLeft().getDomainValue() + delta);
            getRight().setDomainValue(getRight().getDomainValue() + delta);
        }
    }



    /**
     *  Get the domain value at the center of the segment
     *
     *  @return center value
     */
    public double getCenterValue() {
        return getLeft().getDomainValue()
               + (getRight().getDomainValue() - getLeft().getDomainValue())
                 / 2.0;

    }


    /**
     * Draws the wayPoint.
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
        WayPoint leftWayPoint  = getLeft();
        WayPoint rightWayPoint = getRight();
        g2.setStroke(new BasicStroke());
        int         x1     = leftWayPoint.getXFromValue(dataArea, domainAxis);
        int         x2     = rightWayPoint.getXFromValue(dataArea,
                                 domainAxis);
        int         top    = (int) (dataArea.getY());
        int         bottom = (int) (dataArea.getY() + dataArea.getHeight());
        FontMetrics fm     = g2.getFontMetrics();
        int         width  = fm.stringWidth(getName());
        int         height = fm.getAscent() + fm.getDescent();
        if (getSelected()) {
            g2.setColor(Color.red);
        } else {
            g2.setColor(Color.black);
        }
        //      int y = bottom-3;
        y = top - 2;
        int textLeft = x1 + (x2 - x1) / 2 - width / 2;
        g2.drawString(getName(), textLeft, y);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawLine(x1, top + 1, x2, top + 1);
        g2.setStroke(new BasicStroke(1.0f));
        g2.setColor(Color.gray);
        g2.drawLine(x1, top, x1, bottom - WayPoint.ANNOTATION_WIDTH);
        g2.drawLine(x2, top, x2, bottom - WayPoint.ANNOTATION_WIDTH);
    }

    /**
     * Get the left most waypoint. The one with the smallest
     * domain value
     *
     * @return Waypoint on left
     */
    public WayPoint getLeft() {
        if (wayPoint1.getDomainValue() < wayPoint2.getDomainValue()) {
            return wayPoint1;
        }
        return wayPoint2;
    }

    /**
     * Get the right most waypoint.  the one with the largest
     * domain value
     *
     * @return Waypoint on right
     */
    public WayPoint getRight() {
        if (wayPoint1.getDomainValue() > wayPoint2.getDomainValue()) {
            return wayPoint1;
        }
        return wayPoint2;
    }


    /**
     *  Set the WayPoint1 property.
     *
     *  @param value The new value for WayPoint1
     */
    public void setWayPoint1(WayPoint value) {
        wayPoint1 = value;
    }

    /**
     *  Get the WayPoint1 property.
     *
     *  @return The WayPoint1
     */
    public WayPoint getWayPoint1() {
        return wayPoint1;
    }

    /**
     *  Set the WayPoint2 property.
     *
     *  @param value The new value for WayPoint2
     */
    public void setWayPoint2(WayPoint value) {
        wayPoint2 = value;
    }


    /**
     *  Get the WayPoint2 property.
     *
     *  @return The WayPoint2
     */
    public WayPoint getWayPoint2() {
        return wayPoint2;
    }




    /**
     * Distance to the x/y. If the mouse is left of the left
     * waypoint then use the distance to the left point. Same with
     * the right waypoint. If is is between the end points then
     * use the y distance.
     *
     * @param x mouse x
     * @param y mouse y
     *
     * @return distance
     */
    public double distance(int x, int y) {
        lastDistanceCheckToRight = false;
        lastDistanceCheckToLeft  = false;
        Point leftP = (Point) transform.transform(new Point(getLeft().getX(),
                          this.y), new Point());

        Point rightP =
            (Point) transform.transform(new Point(getRight().getX(), this.y),
                                        new Point());


        if (x <= leftP.x) {
            lastDistanceCheckToLeft = true;
            return distance(x, y, leftP.x, leftP.y);
        } else if (x >= rightP.x) {
            lastDistanceCheckToRight = true;
            return distance(x, y, rightP.x, rightP.y);
        }
        return Math.abs(leftP.y - y);
    }






}

