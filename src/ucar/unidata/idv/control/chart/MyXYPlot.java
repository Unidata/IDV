/*
 *  ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2005, by Object Refinery Limited and Contributors.
 *
 * Project Info:  https://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * -----------
 * XYPlot.java
 * -----------
 * (C) Copyright 2000-2005, by Object Refinery Limited and Contributors.
 *
 * Original Author:  David Gilbert (for Object Refinery Limited);
 * Contributor(s):   Craig MacFarlane;
 *                   Mark Watson (www.markwatson.com);
 *                   Jonathan Nash;
 *                   Gideon Krause;
 *                   Klaus Rheinwald;
 *                   Xavier Poinsard;
 *                   Richard Atkinson;
 *                   Arnaud Lelievre;
 *                   Nicolas Brodu;
 *                   Eduardo Ramalho;
 *
 * $Id: MyXYPlot.java,v 1.4 2007/04/16 21:32:11 jeffmc Exp $
 *
 * Changes (from 21-Jun-2001)
 * --------------------------
 * 21-Jun-2001 : Removed redundant JFreeChart parameter from constructors (DG);
 * 18-Sep-2001 : Updated header and fixed DOS encoding problem (DG);
 * 15-Oct-2001 : Data source classes moved to com.jrefinery.data.* (DG);
 * 19-Oct-2001 : Removed the code for drawing the visual representation of each
 *               data point into a separate class StandardXYItemRenderer.
 *               This will make it easier to add variations to the way the
 *               charts are drawn.  Based on code contributed by Mark
 *               Watson (DG);
 * 22-Oct-2001 : Renamed DataSource.java --> Dataset.java etc. (DG);
 * 20-Nov-2001 : Fixed clipping bug that shows up when chart is displayed
 *               inside JScrollPane (DG);
 * 12-Dec-2001 : Removed unnecessary 'throws' clauses from constructor (DG);
 * 13-Dec-2001 : Added skeleton code for tooltips.  Added new constructor. (DG);
 * 16-Jan-2002 : Renamed the tooltips class (DG);
 * 22-Jan-2002 : Added DrawInfo class, incorporating tooltips and crosshairs.
 *               Crosshairs based on code by Jonathan Nash (DG);
 * 05-Feb-2002 : Added alpha-transparency setting based on code by Sylvain
 *               Vieujot (DG);
 * 26-Feb-2002 : Updated getMinimumXXX() and getMaximumXXX() methods to handle
 *               special case when chart is null (DG);
 * 28-Feb-2002 : Renamed Datasets.java --> DatasetUtilities.java (DG);
 * 28-Mar-2002 : The plot now registers with the renderer as a property change
 *               listener.  Also added a new constructor (DG);
 * 09-Apr-2002 : Removed the transRangeZero from the renderer.drawItem()
 *               method.  Moved the tooltip generator into the renderer (DG);
 * 23-Apr-2002 : Fixed bug in methods for drawing horizontal and vertical
 *               lines (DG);
 * 13-May-2002 : Small change to the draw() method so that it works for
 *               OverlaidXYPlot also (DG);
 * 25-Jun-2002 : Removed redundant import (DG);
 * 20-Aug-2002 : Renamed getItemRenderer() --> getRenderer(), and
 *               setXYItemRenderer() --> setRenderer() (DG);
 * 28-Aug-2002 : Added mechanism for (optional) plot annotations (DG);
 * 02-Oct-2002 : Fixed errors reported by Checkstyle (DG);
 * 18-Nov-2002 : Added grid settings for both domain and range axis (previously
 *               these were set in the axes) (DG);
 * 09-Jan-2003 : Further additions to the grid settings, plus integrated plot
 *               border bug fix contributed by Gideon Krause (DG);
 * 22-Jan-2003 : Removed monolithic constructor (DG);
 * 04-Mar-2003 : Added 'no data' message, see bug report 691634.  Added
 *               secondary range markers using code contributed by Klaus
 *               Rheinwald (DG);
 * 26-Mar-2003 : Implemented Serializable (DG);
 * 03-Apr-2003 : Added setDomainAxisLocation() method (DG);
 * 30-Apr-2003 : Moved annotation drawing into a separate method (DG);
 * 01-May-2003 : Added multi-pass mechanism for renderers (DG);
 * 02-May-2003 : Changed axis locations from int to AxisLocation (DG);
 * 15-May-2003 : Added an orientation attribute (DG);
 * 02-Jun-2003 : Removed range axis compatibility test (DG);
 * 05-Jun-2003 : Added domain and range grid bands (sponsored by Focus Computer
 *               Services Ltd) (DG);
 * 26-Jun-2003 : Fixed bug (757303) in getDataRange() method (DG);
 * 02-Jul-2003 : Added patch from bug report 698646 (secondary axes for
 *               overlaid plots) (DG);
 * 23-Jul-2003 : Added support for multiple secondary datasets, axes and
 *               renderers (DG);
 * 27-Jul-2003 : Added support for stacked XY area charts (RA);
 * 19-Aug-2003 : Implemented Cloneable (DG);
 * 01-Sep-2003 : Fixed bug where change to secondary datasets didn't generate
 *               change event (797466) (DG)
 * 08-Sep-2003 : Added internationalization via use of properties
 *               resourceBundle (RFE 690236) (AL);
 * 08-Sep-2003 : Changed ValueAxis API (DG);
 * 08-Sep-2003 : Fixes for serialization (NB);
 * 16-Sep-2003 : Changed ChartRenderingInfo --> PlotRenderingInfo (DG);
 * 17-Sep-2003 : Fixed zooming to include secondary domain axes (DG);
 * 18-Sep-2003 : Added getSecondaryDomainAxisCount() and
 *               getSecondaryRangeAxisCount() methods suggested by Eduardo
 *               Ramalho (RFE 808548) (DG);
 * 23-Sep-2003 : Split domain and range markers into foreground and
 *               background (DG);
 * 06-Oct-2003 : Fixed bug in clearDomainMarkers() and clearRangeMarkers()
 *               methods.  Fixed bug (815876) in addSecondaryRangeMarker()
 *               method.  Added new addSecondaryDomainMarker methods (see bug
 *               id 815869) (DG);
 * 10-Nov-2003 : Added getSecondaryDomain/RangeAxisMappedToDataset() methods
 *               requested by Eduardo Ramalho (DG);
 * 24-Nov-2003 : Removed unnecessary notification when updating axis anchor
 *               values (DG);
 * 21-Jan-2004 : Update for renamed method in ValueAxis (DG);
 * 25-Feb-2004 : Replaced CrosshairInfo with CrosshairState (DG);
 * 12-Mar-2004 : Fixed bug where primary renderer is always used to determine
 *               range type (DG);
 * 22-Mar-2004 : Fixed cloning bug (DG);
 * 23-Mar-2004 : Fixed more cloning bugs (DG);
 * 07-Apr-2004 : Fixed problem with axis range when the secondary renderer is
 *               stacked, see this post in the forum:
 *               https://www.jfree.org/phpBB2/viewtopic.php?t=8204 (DG);
 * 07-Apr-2004 : Added get/setDatasetRenderingOrder() methods (DG);
 * 26-Apr-2004 : Added option to fill quadrant areas in the background of the
 *               plot (DG);
 * 27-Apr-2004 : Removed major distinction between primary and secondary
 *               datasets, renderers and axes (DG);
 * 30-Apr-2004 : Modified to make use of the new getRangeExtent() method in the
 *               renderer interface (DG);
 * 13-May-2004 : Added optional fixedLegendItems attribute (DG);
 * 19-May-2004 : Added indexOf() method (DG);
 * 03-Jun-2004 : Fixed zooming bug (DG);
 * 18-Aug-2004 : Added removedAnnotation() method (by tkram01) (DG);
 * 05-Oct-2004 : Modified storage type for dataset-to-axis maps (DG);
 * 06-Oct-2004 : Modified getDataRange() method to use renderer to determine
 *               the x-value range (now matches behaviour for y-values).  Added
 *               getDomainAxisIndex() method (DG);
 * 12-Nov-2004 : Implemented new Zoomable interface (DG);
 * 25-Nov-2004 : Small update to clone() implementation (DG);
 * 22-Feb-2005 : Changed axis offsets from Spacer --> RectangleInsets (DG);
 * 24-Feb-2005 : Added indexOf(XYItemRenderer) method (DG);
 * 21-Mar-2005 : Register plot as change listener in setRenderer() method (DG);
 * 21-Apr-2005 : Added get/setSeriesRenderingOrder() methods (ET);
 * 26-Apr-2005 : Removed LOGGER (DG);
 * 04-May-2005 : Fixed serialization of domain and range markers (DG);
 * 05-May-2005 : Removed unused draw() method (DG);
 * 20-May-2005 : Added setDomainAxes() and setRangeAxes() methods, as per
 *               RFE 1183100 (DG);
 * 01-Jun-2005 : Upon deserialization, register plot as a listener with its
 *               axes, dataset(s) and renderer(s) - see patch 1209475 (DG);
 * 01-Jun-2005 : Added clearDomainMarkers(int) method to match
 *               clearRangeMarkers(int) (DG);
 *
 */

//package org.jfree.chart.plot;
package ucar.unidata.idv.control.chart;


import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.annotations.XYAnnotation;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.AxisCollection;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.ValueTick;
import org.jfree.chart.event.ChartChangeEventType;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.event.RendererChangeListener;


import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.data.Range;
import org.jfree.data.general.Dataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.io.SerialUtilities;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.util.ObjectList;
import org.jfree.util.ObjectUtilities;
import org.jfree.util.PublicCloneable;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;


/**
 * A general class for plotting data in the form of (x, y) pairs.  This plot can
 * use data from any class that implements the {@link XYDataset} interface.
 * <P>
 * <code>XYPlot</code> makes use of an {@link XYItemRenderer} to draw each point
 * on the plot.  By using different renderers, various chart types can be
 * produced.
 * <p>
 * The {@link org.jfree.chart.ChartFactory} class contains static methods for
 * creating pre-configured charts.
 */
public class MyXYPlot extends XYPlot implements ValueAxisPlot, Zoomable,
        RendererChangeListener, Cloneable, PublicCloneable, Serializable {

    /** For serialization. */
    private static final long serialVersionUID = 7044148245716569264L;

    /** The default grid line stroke. */
    public static final Stroke DEFAULT_GRIDLINE_STROKE =
        new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                        0.0f, new float[] { 2.0f,
                                            2.0f }, 0.0f);

    /** The default grid line paint. */
    public static final Paint DEFAULT_GRIDLINE_PAINT = Color.lightGray;

    /** The default crosshair visibility. */
    public static final boolean DEFAULT_CROSSHAIR_VISIBLE = false;

    /** The default crosshair stroke. */
    public static final Stroke DEFAULT_CROSSHAIR_STROKE =
        DEFAULT_GRIDLINE_STROKE;

    /** The default crosshair paint. */
    public static final Paint DEFAULT_CROSSHAIR_PAINT = Color.blue;

    /** The resourceBundle for the localization. */
    protected static ResourceBundle localizationResources =
        ResourceBundle.getBundle("org.jfree.chart.plot.LocalizationBundle");

    /** The plot orientation. */
    private PlotOrientation orientation;

    /** The offset between the data area and the axes. */
    private RectangleInsets axisOffset;

    /** The domain axis / axes (used for the x-values). */
    private ObjectList domainAxes;

    /** The domain axis locations. */
    private ObjectList domainAxisLocations;

    /** The range axis (used for the y-values). */
    private ObjectList rangeAxes;

    /** The range axis location. */
    private ObjectList rangeAxisLocations;

    /** Storage for the datasets. */
    private ObjectList datasets;

    /** Storage for the renderers. */
    private ObjectList renderers;

    /**
     * Storage for keys that map datasets/renderers to domain axes.  If the
     * map contains no entry for a dataset, it is assumed to map to the
     * primary domain axis (index = 0).
     */
    private Map datasetToDomainAxisMap;

    /**
     * Storage for keys that map datasets/renderers to range axes. If the
     * map contains no entry for a dataset, it is assumed to map to the
     * primary domain axis (index = 0).
     */
    private Map datasetToRangeAxisMap;

    /** The origin point for the quadrants (if drawn). */
    private transient Point2D quadrantOrigin = new Point2D.Double(0.0, 0.0);

    /** The paint used for each quadrant. */
    private transient Paint[] quadrantPaint = new Paint[] { null, null, null,
            null };

    /** A flag that controls whether the domain grid-lines are visible. */
    private boolean domainGridlinesVisible;

    /** The stroke used to draw the domain grid-lines. */
    private transient Stroke domainGridlineStroke;

    /** The paint used to draw the domain grid-lines. */
    private transient Paint domainGridlinePaint;

    /** A flag that controls whether the range grid-lines are visible. */
    private boolean rangeGridlinesVisible;

    /** The stroke used to draw the range grid-lines. */
    private transient Stroke rangeGridlineStroke;

    /** The paint used to draw the range grid-lines. */
    private transient Paint rangeGridlinePaint;

    /**
     * A flag that controls whether or not the zero baseline against the range
     * axis is visible.
     */
    private boolean rangeZeroBaselineVisible;

    /** The stroke used for the zero baseline against the range axis. */
    private transient Stroke rangeZeroBaselineStroke;

    /** The paint used for the zero baseline against the range axis. */
    private transient Paint rangeZeroBaselinePaint;

    /** A flag that controls whether or not a domain crosshair is drawn.. */
    private boolean domainCrosshairVisible;

    /** The domain crosshair value. */
    private double domainCrosshairValue;

    /** The pen/brush used to draw the crosshair (if any). */
    private transient Stroke domainCrosshairStroke;

    /** The color used to draw the crosshair (if any). */
    private transient Paint domainCrosshairPaint;

    /**
     * A flag that controls whether or not the crosshair locks onto actual
     * data points.
     */
    private boolean domainCrosshairLockedOnData = true;

    /** A flag that controls whether or not a range crosshair is drawn.. */
    private boolean rangeCrosshairVisible;

    /** The range crosshair value. */
    private double rangeCrosshairValue;

    /** The pen/brush used to draw the crosshair (if any). */
    private transient Stroke rangeCrosshairStroke;

    /** The color used to draw the crosshair (if any). */
    private transient Paint rangeCrosshairPaint;

    /**
     * A flag that controls whether or not the crosshair locks onto actual
     * data points.
     */
    private boolean rangeCrosshairLockedOnData = true;

    /** A map of lists of foreground markers (optional) for the domain axes. */
    private Map foregroundDomainMarkers;

    /** A map of lists of background markers (optional) for the domain axes. */
    private Map backgroundDomainMarkers;

    /** A map of lists of foreground markers (optional) for the range axes. */
    private Map foregroundRangeMarkers;

    /** A map of lists of background markers (optional) for the range axes. */
    private Map backgroundRangeMarkers;

    /**
     * A (possibly empty) list of annotations for the plot.  The list should
     * be initialised in the constructor and never allowed to be
     * <code>null</code>.
     */
    private List annotations;

    /** The paint used for the domain tick bands (if any). */
    private transient Paint domainTickBandPaint;

    /** The paint used for the range tick bands (if any). */
    private transient Paint rangeTickBandPaint;

    /** The fixed domain axis space. */
    private AxisSpace fixedDomainAxisSpace;

    /** The fixed range axis space. */
    private AxisSpace fixedRangeAxisSpace;

    /**
     * The order of the dataset rendering (REVERSE draws the primary dataset
     * last so that it appears to be on top).
     */
    private DatasetRenderingOrder datasetRenderingOrder =
        DatasetRenderingOrder.REVERSE;

    /**
     * The order of the series rendering (REVERSE draws the primary series
     * last so that it appears to be on top).
     */
    private SeriesRenderingOrder seriesRenderingOrder =
        SeriesRenderingOrder.REVERSE;

    /**
     * The weight for this plot (only relevant if this is a subplot in a
     * combined plot).
     */
    private int weight;

    /**
     * An optional collection of legend items that can be returned by the
     * getLegendItems() method.
     */
    private LegendItemCollection fixedLegendItems;

    /**
     * Default constructor.
     */
    public MyXYPlot() {
        this(null, null, null, null);
    }

    /**
     * Creates a new plot.
     *
     * @param dataset  the dataset (<code>null</code> permitted).
     * @param domainAxis  the domain axis (<code>null</code> permitted).
     * @param rangeAxis  the range axis (<code>null</code> permitted).
     * @param renderer  the renderer (<code>null</code> permitted).
     */
    public MyXYPlot(XYDataset dataset, ValueAxis domainAxis,
                    ValueAxis rangeAxis, XYItemRenderer renderer) {

        super();

        this.orientation = PlotOrientation.VERTICAL;
        this.weight      = 1;  // only relevant when this is a subplot
        this.axisOffset  = RectangleInsets.ZERO_INSETS;

        // allocate storage for datasets, axes and renderers (all optional)
        this.domainAxes              = new ObjectList();
        this.domainAxisLocations     = new ObjectList();
        this.foregroundDomainMarkers = new HashMap();
        this.backgroundDomainMarkers = new HashMap();

        this.rangeAxes               = new ObjectList();
        this.rangeAxisLocations      = new ObjectList();
        this.foregroundRangeMarkers  = new HashMap();
        this.backgroundRangeMarkers  = new HashMap();

        this.datasets                = new ObjectList();
        this.renderers               = new ObjectList();

        this.datasetToDomainAxisMap  = new TreeMap();
        this.datasetToRangeAxisMap   = new TreeMap();

        this.datasets.set(0, dataset);
        if (dataset != null) {
            dataset.addChangeListener(this);
        }

        this.renderers.set(0, renderer);
        if (renderer != null) {
            renderer.setPlot(this);
            renderer.addChangeListener(this);
        }

        this.domainAxes.set(0, domainAxis);
        this.mapDatasetToDomainAxis(0, 0);
        if (domainAxis != null) {
            domainAxis.setPlot(this);
            domainAxis.addChangeListener(this);
        }
        this.domainAxisLocations.set(0, AxisLocation.BOTTOM_OR_LEFT);

        this.rangeAxes.set(0, rangeAxis);
        this.mapDatasetToRangeAxis(0, 0);
        if (rangeAxis != null) {
            rangeAxis.setPlot(this);
            rangeAxis.addChangeListener(this);
        }
        this.rangeAxisLocations.set(0, AxisLocation.BOTTOM_OR_LEFT);

        configureDomainAxes();
        configureRangeAxes();

        this.domainGridlinesVisible   = true;
        this.domainGridlineStroke     = DEFAULT_GRIDLINE_STROKE;
        this.domainGridlinePaint      = DEFAULT_GRIDLINE_PAINT;

        this.rangeGridlinesVisible    = true;
        this.rangeGridlineStroke      = DEFAULT_GRIDLINE_STROKE;
        this.rangeGridlinePaint       = DEFAULT_GRIDLINE_PAINT;

        this.rangeZeroBaselineVisible = false;
        this.rangeZeroBaselinePaint   = Color.black;
        this.rangeZeroBaselineStroke  = new BasicStroke(0.5f);

        this.domainCrosshairVisible   = false;
        this.domainCrosshairValue     = 0.0;
        this.domainCrosshairStroke    = DEFAULT_CROSSHAIR_STROKE;
        this.domainCrosshairPaint     = DEFAULT_CROSSHAIR_PAINT;

        this.rangeCrosshairVisible    = false;
        this.rangeCrosshairValue      = 0.0;
        this.rangeCrosshairStroke     = DEFAULT_CROSSHAIR_STROKE;
        this.rangeCrosshairPaint      = DEFAULT_CROSSHAIR_PAINT;

        this.annotations              = new java.util.ArrayList();

    }

    /**
     * Returns the plot type as a string.
     *
     * @return A short string describing the type of plot.
     */
    public String getPlotType() {
        return localizationResources.getString("XY_Plot");
    }

    /**
     * Returns the orientation of the plot.
     *
     * @return The orientation of the plot.
     */
    public PlotOrientation getOrientation() {
        return this.orientation;
    }

    /**
     * Sets the orientation for the plot.
     *
     * @param orientation  the orientation (<code>null</code> not allowed).
     */
    public void setOrientation(PlotOrientation orientation) {
        if (orientation == null) {
            throw new IllegalArgumentException(
                "Null 'orientation' argument.");
        }
        if (orientation != this.orientation) {
            this.orientation = orientation;
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Returns the axis offset.
     *
     * @return The axis offset (never <code>null</code>).
     */
    public RectangleInsets getAxisOffset() {
        return this.axisOffset;
    }

    /**
     * Sets the axis offsets (gap between the data area and the axes).
     *
     * @param offset  the offset (<code>null</code> not permitted).
     */
    public void setAxisOffset(RectangleInsets offset) {
        if (offset == null) {
            throw new IllegalArgumentException("Null 'offset' argument.");
        }
        this.axisOffset = offset;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the domain axis for the plot.  If the domain axis for this plot
     * is null, then the method will return the parent plot's domain axis (if
     * there is a parent plot).
     *
     * @return The domain axis.
     */
    public ValueAxis getDomainAxis() {
        return getDomainAxis(0);
    }

    /**
     * Returns a domain axis.
     *
     * @param index  the axis index.
     *
     * @return The axis (<code>null</code> possible).
     */
    public ValueAxis getDomainAxis(int index) {
        ValueAxis result = null;
        if (index < this.domainAxes.size()) {
            result = (ValueAxis) this.domainAxes.get(index);
        }
        if (result == null) {
            Plot parent = getParent();
            if (parent instanceof MyXYPlot) {
                MyXYPlot xy = (MyXYPlot) parent;
                result = xy.getDomainAxis(index);
            }
        }
        return result;
    }

    /**
     * Sets the domain axis for the plot and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param axis  the new axis (<code>null</code> permitted).
     */
    public void setDomainAxis(ValueAxis axis) {
        setDomainAxis(0, axis);
    }

    /**
     * Sets a domain axis and sends a {@link PlotChangeEvent} to all
     * registered listeners.
     *
     * @param index  the axis index.
     * @param axis  the axis.
     */
    public void setDomainAxis(int index, ValueAxis axis) {
        setDomainAxis(index, axis, true);
    }

    /**
     * Sets a domain axis and, if requested, sends a {@link PlotChangeEvent} to
     * all registered listeners.
     *
     * @param index  the axis index.
     * @param axis  the axis.
     * @param notify  notify listeners?
     */
    public void setDomainAxis(int index, ValueAxis axis, boolean notify) {
        ValueAxis existing = getDomainAxis(index);
        if (existing != null) {
            existing.removeChangeListener(this);
        }
        if (axis != null) {
            axis.setPlot(this);
        }
        this.domainAxes.set(index, axis);
        if (axis != null) {
            axis.configure();
            axis.addChangeListener(this);
        }
        if (notify) {
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Sets the domain axes for this plot and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param axes  the axes.
     */
    public void setDomainAxes(ValueAxis[] axes) {
        for (int i = 0; i < axes.length; i++) {
            setDomainAxis(i, axes[i], false);
        }
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the location of the primary domain axis.
     *
     * @return The location (never <code>null</code>).
     */
    public AxisLocation getDomainAxisLocation() {
        return (AxisLocation) this.domainAxisLocations.get(0);
    }

    /**
     * Sets the location of the domain axis and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param location  the location (<code>null</code> not permitted).
     */
    public void setDomainAxisLocation(AxisLocation location) {
        // defer argument checking...
        setDomainAxisLocation(location, true);
    }

    /**
     * Sets the location of the domain axis and, if requested, sends a
     * {@link PlotChangeEvent} to all registered listeners.
     *
     * @param location  the location (<code>null</code> not permitted).
     * @param notify  notify listeners?
     */
    public void setDomainAxisLocation(AxisLocation location, boolean notify) {
        if (location == null) {
            throw new IllegalArgumentException("Null 'location' argument.");
        }
        this.domainAxisLocations.set(0, location);
        if (notify) {
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Returns the edge for the primary domain axis (taking into account the
     * plot's orientation.
     *
     * @return The edge.
     */
    public RectangleEdge getDomainAxisEdge() {
        return Plot.resolveDomainAxisLocation(getDomainAxisLocation(),
                this.orientation);
    }

    /**
     * Returns the number of domain axes.
     *
     * @return The axis count.
     */
    public int getDomainAxisCount() {
        return this.domainAxes.size();
    }

    /**
     * Clears the domain axes from the plot and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     */
    public void clearDomainAxes() {
        for (int i = 0; i < this.domainAxes.size(); i++) {
            ValueAxis axis = (ValueAxis) this.domainAxes.get(i);
            if (axis != null) {
                axis.removeChangeListener(this);
            }
        }
        this.domainAxes.clear();
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Configures the domain axes.
     */
    public void configureDomainAxes() {
        for (int i = 0; i < this.domainAxes.size(); i++) {
            ValueAxis axis = (ValueAxis) this.domainAxes.get(i);
            if (axis != null) {
                axis.configure();
            }
        }
    }

    /**
     * Returns the location for a domain axis.  If this hasn't been set
     * explicitly, the method returns the location that is opposite to the
     * primary domain axis location.
     *
     * @param index  the axis index.
     *
     * @return The location (never <code>null</code>).
     */
    public AxisLocation getDomainAxisLocation(int index) {
        AxisLocation result = null;
        if (index < this.domainAxisLocations.size()) {
            result = (AxisLocation) this.domainAxisLocations.get(index);
        }
        if (result == null) {
            result = AxisLocation.getOpposite(getDomainAxisLocation());
        }
        return result;
    }

    /**
     * Sets the location for a domain axis and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param index  the axis index.
     * @param location  the location (<code>null</code> permitted).
     */
    public void setDomainAxisLocation(int index, AxisLocation location) {
        this.domainAxisLocations.set(index, location);
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the edge for a domain axis.
     *
     * @param index  the axis index.
     *
     * @return The edge.
     */
    public RectangleEdge getDomainAxisEdge(int index) {
        AxisLocation location = getDomainAxisLocation(index);
        RectangleEdge result = Plot.resolveDomainAxisLocation(location,
                                   this.orientation);
        if (result == null) {
            result = RectangleEdge.opposite(getDomainAxisEdge());
        }
        return result;
    }

    /**
     * Returns the range axis for the plot.  If the range axis for this plot is
     * null, then the method will return the parent plot's range axis (if
     * there is a parent plot).
     *
     * @return The range axis.
     */
    public ValueAxis getRangeAxis() {
        return getRangeAxis(0);
    }

    /**
     * Sets the range axis for the plot and sends a {@link PlotChangeEvent} to
     * all registered listeners.
     *
     * @param axis  the axis (<code>null</code> permitted).
     *
     */
    public void setRangeAxis(ValueAxis axis) {

        if (axis != null) {
            axis.setPlot(this);
        }

        // plot is likely registered as a listener with the existing axis...
        ValueAxis existing = getRangeAxis();
        if (existing != null) {
            existing.removeChangeListener(this);
        }

        this.rangeAxes.set(0, axis);
        if (axis != null) {
            axis.configure();
            axis.addChangeListener(this);
        }
        notifyListeners(new PlotChangeEvent(this));

    }

    /**
     * Returns the location of the primary range axis.
     *
     * @return The location (never <code>null</code>).
     */
    public AxisLocation getRangeAxisLocation() {
        return (AxisLocation) this.rangeAxisLocations.get(0);
    }

    /**
     * Sets the location of the primary range axis and sends a
     * {@link PlotChangeEvent} to all registered listeners.
     *
     * @param location  the location (<code>null</code> not permitted).
     */
    public void setRangeAxisLocation(AxisLocation location) {
        // defer argument checking...
        setRangeAxisLocation(location, true);
    }

    /**
     * Sets the location of the primary range axis and, if requested, sends a
     * {@link PlotChangeEvent} to all registered listeners.
     *
     * @param location  the location (<code>null</code> not permitted).
     * @param notify  notify listeners?
     */
    public void setRangeAxisLocation(AxisLocation location, boolean notify) {
        if (location == null) {
            throw new IllegalArgumentException("Null 'location' argument.");
        }
        this.rangeAxisLocations.set(0, location);
        if (notify) {
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the edge for the primary range axis.
     *
     * @return The range axis edge.
     */
    public RectangleEdge getRangeAxisEdge() {
        return Plot.resolveRangeAxisLocation(getRangeAxisLocation(),
                                             this.orientation);
    }

    /**
     * Returns a range axis.
     *
     * @param index  the axis index.
     *
     * @return The axis (<code>null</code> possible).
     */
    public ValueAxis getRangeAxis(int index) {
        ValueAxis result = null;
        if (index < this.rangeAxes.size()) {
            result = (ValueAxis) this.rangeAxes.get(index);
        }
        if (result == null) {
            Plot parent = getParent();
            if (parent instanceof MyXYPlot) {
                MyXYPlot xy = (MyXYPlot) parent;
                result = xy.getRangeAxis(index);
            }
        }
        return result;
    }

    /**
     * Sets a range axis and sends a {@link PlotChangeEvent} to all registered
     * listeners.
     *
     * @param index  the axis index.
     * @param axis  the axis (<code>null</code> permitted).
     */
    public void setRangeAxis(int index, ValueAxis axis) {
        setRangeAxis(index, axis, true);
    }

    /**
     * Sets a range axis and, if requested, sends a {@link PlotChangeEvent} to
     * all registered listeners.
     *
     * @param index  the axis index.
     * @param axis  the axis (<code>null</code> permitted).
     * @param notify and notify the listeners
     */
    public void setRangeAxis(int index, ValueAxis axis, boolean notify) {
        ValueAxis existing = getRangeAxis(index);
        if (existing != null) {
            existing.removeChangeListener(this);
        }
        if (axis != null) {
            axis.setPlot(this);
        }
        this.rangeAxes.set(index, axis);
        if (axis != null) {
            axis.configure();
            axis.addChangeListener(this);
        }
        if (notify) {
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Sets the range axes for this plot and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param axes  the axes.
     */
    public void setRangeAxes(ValueAxis[] axes) {
        for (int i = 0; i < axes.length; i++) {
            setRangeAxis(i, axes[i], false);
        }
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the number of range axes.
     *
     * @return The axis count.
     */
    public int getRangeAxisCount() {
        return this.rangeAxes.size();
    }

    /**
     * Clears the range axes from the plot and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     */
    public void clearRangeAxes() {
        for (int i = 0; i < this.rangeAxes.size(); i++) {
            ValueAxis axis = (ValueAxis) this.rangeAxes.get(i);
            if (axis != null) {
                axis.removeChangeListener(this);
            }
        }
        this.rangeAxes.clear();
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Configures the range axes.
     */
    public void configureRangeAxes() {
        for (int i = 0; i < this.rangeAxes.size(); i++) {
            ValueAxis axis = (ValueAxis) this.rangeAxes.get(i);
            if (axis != null) {
                axis.configure();
            }
        }
    }

    /**
     * Returns the location for a range axis.  If this hasn't been set
     * explicitly, the method returns the location that is opposite to the
     * primary range axis location.
     *
     * @param index  the axis index.
     *
     * @return The location (never <code>null</code>).
     */
    public AxisLocation getRangeAxisLocation(int index) {
        AxisLocation result = null;
        if (index < this.rangeAxisLocations.size()) {
            result = (AxisLocation) this.rangeAxisLocations.get(index);
        }
        if (result == null) {
            result = AxisLocation.getOpposite(getRangeAxisLocation());
        }
        return result;
    }

    /**
     * Sets the location for a range axis and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param index  the axis index.
     * @param location  the location (<code>null</code> permitted).
     */
    public void setRangeAxisLocation(int index, AxisLocation location) {
        this.rangeAxisLocations.set(index, location);
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the edge for a range axis.
     *
     * @param index  the axis index.
     *
     * @return The edge.
     */
    public RectangleEdge getRangeAxisEdge(int index) {
        AxisLocation location = getRangeAxisLocation(index);
        RectangleEdge result = Plot.resolveRangeAxisLocation(location,
                                   this.orientation);
        if (result == null) {
            result = RectangleEdge.opposite(getRangeAxisEdge());
        }
        return result;
    }

    /**
     * Returns the primary dataset for the plot.
     *
     * @return The primary dataset (possibly <code>null</code>).
     */
    public XYDataset getDataset() {
        return getDataset(0);
    }

    /**
     * Returns a dataset.
     *
     * @param index  the dataset index.
     *
     * @return The dataset (possibly <code>null</code>).
     */
    public XYDataset getDataset(int index) {
        XYDataset result = null;
        if (this.datasets.size() > index) {
            result = (XYDataset) this.datasets.get(index);
        }
        return result;
    }

    /**
     * Sets the primary dataset for the plot, replacing the existing dataset if
     * there is one.
     *
     * @param dataset  the dataset (<code>null</code> permitted).
     */
    public void setDataset(XYDataset dataset) {
        setDataset(0, dataset);
    }

    /**
     * Clear the data sets
     */
    public void clearDatasets() {
        for (int i = 0; i < this.datasets.size(); i++) {
            XYDataset xyDataset = (XYDataset) this.datasets.get(i);
            xyDataset.removeChangeListener(this);
        }
        this.datasets.clear();
    }


    /**
     * Sets a dataset for the plot.
     *
     * @param index  the dataset index.
     * @param dataset  the dataset (<code>null</code> permitted).
     */
    public void setDataset(int index, XYDataset dataset) {
        XYDataset existing = getDataset(index);
        if (existing != null) {
            existing.removeChangeListener(this);
        }
        this.datasets.set(index, dataset);
        if (dataset != null) {
            dataset.addChangeListener(this);
        }

        // send a dataset change event to self...
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        datasetChanged(event);
    }

    /**
     * Returns the number of datasets.
     *
     * @return The number of datasets.
     */
    public int getDatasetCount() {
        return this.datasets.size();
    }

    /**
     * Returns the index of the specified dataset, or <code>-1</code> if the
     * dataset does not belong to the plot.
     *
     * @param dataset  the dataset (<code>null</code> not permitted).
     *
     * @return The index.
     */
    public int indexOf(XYDataset dataset) {
        int result = -1;
        for (int i = 0; i < this.datasets.size(); i++) {
            if (dataset == this.datasets.get(i)) {
                result = i;
                break;
            }
        }
        return result;
    }

    /**
     * Maps a dataset to a particular domain axis.  All data will be plotted
     * against axis zero by default, no mapping is required for this case.
     *
     * @param index  the dataset index (zero-based).
     * @param axisIndex  the axis index.
     */
    public void mapDatasetToDomainAxis(int index, int axisIndex) {
        this.datasetToDomainAxisMap.put(new Integer(index),
                                        new Integer(axisIndex));
        // fake a dataset change event to update axes...
        datasetChanged(new DatasetChangeEvent(this, getDataset(index)));
    }

    /**
     * Maps a dataset to a particular range axis.  All data will be plotted
     * against axis zero by default, no mapping is required for this case.
     *
     * @param index  the dataset index (zero-based).
     * @param axisIndex  the axis index.
     */
    public void mapDatasetToRangeAxis(int index, int axisIndex) {
        this.datasetToRangeAxisMap.put(new Integer(index),
                                       new Integer(axisIndex));
        // fake a dataset change event to update axes...
        datasetChanged(new DatasetChangeEvent(this, getDataset(index)));
    }

    /**
     * Returns the renderer for the primary dataset.
     *
     * @return The item renderer (possibly <code>null</code>).
     */
    public XYItemRenderer getRenderer() {
        return getRenderer(0);
    }

    /**
     * Returns the renderer for a dataset, or <code>null</code>.
     *
     * @param index  the renderer index.
     *
     * @return The renderer (possibly <code>null</code>).
     */
    public XYItemRenderer getRenderer(int index) {
        XYItemRenderer result = null;
        if (this.renderers.size() > index) {
            result = (XYItemRenderer) this.renderers.get(index);
        }
        return result;

    }

    /**
     * Sets the renderer for the primary dataset and sends a
     * {@link PlotChangeEvent} to all registered listeners.  If the renderer
     * is set to <code>null</code>, no data will be displayed.
     *
     * @param renderer  the renderer (<code>null</code> permitted).
     */
    public void setRenderer(XYItemRenderer renderer) {
        setRenderer(0, renderer);
    }

    /**
     * Sets a renderer and sends a {@link PlotChangeEvent} is sent to all
     * registered listeners.
     *
     * @param index  the index.
     * @param renderer  the renderer.
     */
    public void setRenderer(int index, XYItemRenderer renderer) {
        XYItemRenderer existing = getRenderer(index);
        if (existing != null) {
            existing.removeChangeListener(this);
        }
        this.renderers.set(index, renderer);
        if (renderer != null) {
            renderer.setPlot(this);
            renderer.addChangeListener(this);
        }
        configureDomainAxes();
        configureRangeAxes();
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the dataset rendering order.
     *
     * @return The order (never <code>null</code>).
     */
    public DatasetRenderingOrder getDatasetRenderingOrder() {
        return this.datasetRenderingOrder;
    }

    /**
     * Sets the rendering order and sends a {@link PlotChangeEvent} to all
     * registered listeners.  By default, the plot renders the primary dataset
     * last (so that the primary dataset overlays the secondary datasets).
     * You can reverse this if you want to.
     *
     * @param order  the rendering order (<code>null</code> not permitted).
     */
    public void setDatasetRenderingOrder(DatasetRenderingOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Null 'order' argument.");
        }
        this.datasetRenderingOrder = order;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the series rendering order.
     *
     * @return the order (never <code>null</code>).
     */
    public SeriesRenderingOrder getSeriesRenderingOrder() {
        return this.seriesRenderingOrder;
    }

    /**
     * Sets the series order and sends a {@link PlotChangeEvent} to all
     * registered listeners.  By default, the plot renders the primary series
     * last (so that the primary series appears to be on top).
     * You can reverse this if you want to.
     *
     * @param order  the rendering order (<code>null</code> not permitted).
     */
    public void setSeriesRenderingOrder(SeriesRenderingOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("Null 'order' argument.");
        }
        this.seriesRenderingOrder = order;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the index of the specified renderer, or <code>-1</code> if the
     * renderer is not assigned to this plot.
     *
     * @param renderer  the renderer (<code>null</code> permitted).
     *
     * @return The renderer index.
     */
    public int getIndexOf(XYItemRenderer renderer) {
        return this.renderers.indexOf(renderer);
    }

    /**
     * Returns the renderer for the specified dataset.  The code first
     * determines the index of the dataset, then checks if there is a
     * renderer with the same index (if not, the method returns renderer(0).
     *
     * @param dataset  the dataset (<code>null</code> permitted).
     *
     * @return The renderer (possibly <code>null</code>).
     */
    public XYItemRenderer getRendererForDataset(XYDataset dataset) {
        XYItemRenderer result = null;
        for (int i = 0; i < this.datasets.size(); i++) {
            if (this.datasets.get(i) == dataset) {
                result = (XYItemRenderer) this.renderers.get(i);
                if (result == null) {
                    result = getRenderer();
                }
                break;
            }
        }
        return result;
    }

    /**
     * Returns the weight for this plot when it is used as a subplot within a
     * combined plot.
     *
     * @return The weight.
     */
    public int getWeight() {
        return this.weight;
    }

    /**
     * Sets the weight for the plot.
     *
     * @param weight  the weight.
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * Returns <code>true</code> if the domain gridlines are visible, and
     * <code>false<code> otherwise.
     *
     * @return <code>true</code> or <code>false</code>.
     */
    public boolean isDomainGridlinesVisible() {
        return this.domainGridlinesVisible;
    }

    /**
     * Sets the flag that controls whether or not the domain grid-lines are
     * visible.
     * <p>
     * If the flag value is changed, a {@link PlotChangeEvent} is sent to all
     * registered listeners.
     *
     * @param visible  the new value of the flag.
     */
    public void setDomainGridlinesVisible(boolean visible) {
        if (this.domainGridlinesVisible != visible) {
            this.domainGridlinesVisible = visible;
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Returns the stroke for the grid-lines (if any) plotted against the
     * domain axis.
     *
     * @return The stroke.
     */
    public Stroke getDomainGridlineStroke() {
        return this.domainGridlineStroke;
    }

    /**
     * Sets the stroke for the grid lines plotted against the domain axis.
     * <p>
     * If you set this to <code>null</code>, no grid lines will be drawn.
     *
     * @param stroke  the stroke (<code>null</code> permitted).
     */
    public void setDomainGridlineStroke(Stroke stroke) {
        this.domainGridlineStroke = stroke;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the paint for the grid lines (if any) plotted against the domain
     * axis.
     *
     * @return The paint.
     */
    public Paint getDomainGridlinePaint() {
        return this.domainGridlinePaint;
    }

    /**
     * Sets the paint for the grid lines plotted against the domain axis.
     * <p>
     * If you set this to <code>null</code>, no grid lines will be drawn.
     *
     * @param paint  the paint (<code>null</code> permitted).
     */
    public void setDomainGridlinePaint(Paint paint) {
        this.domainGridlinePaint = paint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns <code>true</code> if the range axis grid is visible, and
     * <code>false<code> otherwise.
     *
     * @return A boolean.
     */
    public boolean isRangeGridlinesVisible() {
        return this.rangeGridlinesVisible;
    }

    /**
     * Sets the flag that controls whether or not the range axis grid lines
     * are visible.
     * <p>
     * If the flag value is changed, a {@link PlotChangeEvent} is sent to all
     * registered listeners.
     *
     * @param visible  the new value of the flag.
     */
    public void setRangeGridlinesVisible(boolean visible) {
        if (this.rangeGridlinesVisible != visible) {
            this.rangeGridlinesVisible = visible;
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Returns the stroke for the grid lines (if any) plotted against the
     * range axis.
     *
     * @return The stroke (never <code>null</code>).
     */
    public Stroke getRangeGridlineStroke() {
        return this.rangeGridlineStroke;
    }

    /**
     * Sets the stroke for the grid lines plotted against the range axis,
     * and sends a {@link PlotChangeEvent} to all registered listeners.
     *
     * @param stroke  the stroke (<code>null</code> not permitted).
     */
    public void setRangeGridlineStroke(Stroke stroke) {
        if (stroke == null) {
            throw new IllegalArgumentException("Null 'stroke' argument.");
        }
        this.rangeGridlineStroke = stroke;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the paint for the grid lines (if any) plotted against the range
     * axis.
     *
     * @return The paint (never <code>null</code>).
     */
    public Paint getRangeGridlinePaint() {
        return this.rangeGridlinePaint;
    }

    /**
     * Sets the paint for the grid lines plotted against the range axis and
     * sends a {@link PlotChangeEvent} to all registered listeners.
     *
     * @param paint  the paint (<code>null</code> permitted).
     */
    public void setRangeGridlinePaint(Paint paint) {
        this.rangeGridlinePaint = paint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns a flag that controls whether or not a zero baseline is
     * displayed for the range axis.
     *
     * @return A boolean.
     */
    public boolean isRangeZeroBaselineVisible() {
        return this.rangeZeroBaselineVisible;
    }

    /**
     * Sets the flag that controls whether or not the zero baseline is
     * displayed for the range axis, and sends a {@link PlotChangeEvent} to
     * all registered listeners.
     *
     * @param visible  the flag.
     */
    public void setRangeZeroBaselineVisible(boolean visible) {
        this.rangeZeroBaselineVisible = visible;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the stroke used for the zero baseline against the range axis.
     *
     * @return The stroke (never <code>null</code>).
     */
    public Stroke getRangeZeroBaselineStroke() {
        return this.rangeZeroBaselineStroke;
    }

    /**
     * Sets the stroke for the zero baseline for the range axis,
     * and sends a {@link PlotChangeEvent} to all registered listeners.
     *
     * @param stroke  the stroke (<code>null</code> not permitted).
     */
    public void setRangeZeroBaselineStroke(Stroke stroke) {
        if (stroke == null) {
            throw new IllegalArgumentException("Null 'stroke' argument.");
        }
        this.rangeZeroBaselineStroke = stroke;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the paint for the zero baseline (if any) plotted against the
     * range axis.
     *
     * @return The paint (never <code>null</code>).
     */
    public Paint getRangeZeroBaselinePaint() {
        return this.rangeZeroBaselinePaint;
    }

    /**
     * Sets the paint for the zero baseline plotted against the range axis and
     * sends a {@link PlotChangeEvent} to all registered listeners.
     *
     * @param paint  the paint (<code>null</code> permitted).
     */
    public void setRangeZeroBaselinePaint(Paint paint) {
        this.rangeZeroBaselinePaint = paint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the paint used for the domain tick bands.  If this is
     * <code>null</code>, no tick bands will be drawn.
     *
     * @return The paint (possibly <code>null</code>).
     */
    public Paint getDomainTickBandPaint() {
        return this.domainTickBandPaint;
    }

    /**
     * Sets the paint for the domain tick bands.
     *
     * @param paint  the paint (<code>null</code> permitted).
     */
    public void setDomainTickBandPaint(Paint paint) {
        this.domainTickBandPaint = paint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the paint used for the range tick bands.  If this is
     * <code>null</code>, no tick bands will be drawn.
     *
     * @return The paint (possibly <code>null</code>).
     */
    public Paint getRangeTickBandPaint() {
        return this.rangeTickBandPaint;
    }

    /**
     * Sets the paint for the range tick bands.
     *
     * @param paint  the paint (<code>null</code> permitted).
     */
    public void setRangeTickBandPaint(Paint paint) {
        this.rangeTickBandPaint = paint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the origin for the quadrants that can be displayed on the plot.
     * This defaults to (0, 0).
     *
     * @return The origin point (never <code>null</code>).
     */
    public Point2D getQuadrantOrigin() {
        return this.quadrantOrigin;
    }

    /**
     * Sets the quadrant origin and sends a {@link PlotChangeEvent} to all
     * registered listeners.
     *
     * @param origin  the origin (<code>null</code> not permitted).
     */
    public void setQuadrantOrigin(Point2D origin) {
        if (origin == null) {
            throw new IllegalArgumentException("Null 'origin' argument.");
        }
        this.quadrantOrigin = origin;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the paint used for the specified quadrant.
     *
     * @param index  the quadrant index (0-3).
     *
     * @return The paint (possibly <code>null</code>).
     */
    public Paint getQuadrantPaint(int index) {
        if ((index < 0) || (index > 3)) {
            throw new IllegalArgumentException(
                "The index should be in the range 0 to 3.");
        }
        return this.quadrantPaint[index];
    }

    /**
     * Sets the paint used for the specified quadrant and sends a
     * {@link PlotChangeEvent} to all registered listeners.
     *
     * @param index  the quadrant index (0-3).
     * @param paint  the paint (<code>null</code> permitted).
     */
    public void setQuadrantPaint(int index, Paint paint) {
        if ((index < 0) || (index > 3)) {
            throw new IllegalArgumentException(
                "The index should be in the range 0 to 3.");
        }
        this.quadrantPaint[index] = paint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Adds a marker for the domain axis and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     * <P>
     * Typically a marker will be drawn by the renderer as a line perpendicular
     * to the range axis, however this is entirely up to the renderer.
     *
     * @param marker  the marker (<code>null</code> not permitted).
     */
    public void addDomainMarker(Marker marker) {
        // defer argument checking...
        addDomainMarker(marker, Layer.FOREGROUND);
    }

    /**
     * Adds a marker for the domain axis in the specified layer and sends a
     * {@link PlotChangeEvent} to all registered listeners.
     * <P>
     * Typically a marker will be drawn by the renderer as a line perpendicular
     * to the range axis, however this is entirely up to the renderer.
     *
     * @param marker  the marker (<code>null</code> not permitted).
     * @param layer  the layer (foreground or background).
     */
    public void addDomainMarker(Marker marker, Layer layer) {
        addDomainMarker(0, marker, layer);
    }

    /**
     * Clears all the (foreground and background) domain markers and sends a
     * {@link PlotChangeEvent} to all registered listeners.
     */
    public void clearDomainMarkers() {
        if (this.foregroundDomainMarkers != null) {
            this.foregroundDomainMarkers.clear();
        }
        if (this.backgroundDomainMarkers != null) {
            this.backgroundDomainMarkers.clear();
        }
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Clears the (foreground and background) domain markers for a particular
     * renderer.
     *
     * @param index  the renderer index.
     */
    public void clearDomainMarkers(int index) {
        Integer key = new Integer(index);
        if (this.backgroundDomainMarkers != null) {
            Collection markers =
                (Collection) this.backgroundDomainMarkers.get(key);
            if (markers != null) {
                markers.clear();
            }
        }
        if (this.foregroundRangeMarkers != null) {
            Collection markers =
                (Collection) this.foregroundDomainMarkers.get(key);
            if (markers != null) {
                markers.clear();
            }
        }
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Adds a marker for a renderer and sends a {@link PlotChangeEvent} to
     * all registered listeners.
     * <P>
     * Typically a marker will be drawn by the renderer as a line perpendicular
     * to the domain axis (that the renderer is mapped to), however this is
     * entirely up to the renderer.
     *
     * @param index  the renderer index.
     * @param marker  the marker.
     * @param layer  the layer (foreground or background).
     */
    public void addDomainMarker(int index, Marker marker, Layer layer) {
        Collection markers;
        if (layer == Layer.FOREGROUND) {
            markers = (Collection) this.foregroundDomainMarkers.get(
                new Integer(index));
            if (markers == null) {
                markers = new java.util.ArrayList();
                this.foregroundDomainMarkers.put(new Integer(index), markers);
            }
            markers.add(marker);
        } else if (layer == Layer.BACKGROUND) {
            markers = (Collection) this.backgroundDomainMarkers.get(
                new Integer(index));
            if (markers == null) {
                markers = new java.util.ArrayList();
                this.backgroundDomainMarkers.put(new Integer(index), markers);
            }
            markers.add(marker);
        }
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Adds a marker for the range axis and sends a {@link PlotChangeEvent} to
     * all registered listeners.
     * <P>
     * Typically a marker will be drawn by the renderer as a line perpendicular
     * to the range axis, however this is entirely up to the renderer.
     *
     * @param marker  the marker (<code>null</code> not permitted).
     */
    public void addRangeMarker(Marker marker) {
        addRangeMarker(marker, Layer.FOREGROUND);
    }

    /**
     * Adds a marker for the range axis in the specified layer and sends a
     * {@link PlotChangeEvent} to all registered listeners.
     * <P>
     * Typically a marker will be drawn by the renderer as a line perpendicular
     * to the range axis, however this is entirely up to the renderer.
     *
     * @param marker  the marker (<code>null</code> not permitted).
     * @param layer  the layer (foreground or background).
     */
    public void addRangeMarker(Marker marker, Layer layer) {
        addRangeMarker(0, marker, layer);
    }

    /**
     * Clears all the range markers and sends a {@link PlotChangeEvent} to all
     * registered listeners.
     */
    public void clearRangeMarkers() {
        if (this.foregroundRangeMarkers != null) {
            this.foregroundRangeMarkers.clear();
        }
        if (this.backgroundRangeMarkers != null) {
            this.backgroundRangeMarkers.clear();
        }
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Adds a marker for a renderer and sends a {@link PlotChangeEvent} to
     * all registered listeners.
     * <P>
     * Typically a marker will be drawn by the renderer as a line perpendicular
     * to the range axis, however this is entirely up to the renderer.
     *
     * @param index  the renderer index.
     * @param marker  the marker.
     * @param layer  the layer (foreground or background).
     */
    public void addRangeMarker(int index, Marker marker, Layer layer) {
        Collection markers;
        if (layer == Layer.FOREGROUND) {
            markers = (Collection) this.foregroundRangeMarkers.get(
                new Integer(index));
            if (markers == null) {
                markers = new java.util.ArrayList();
                this.foregroundRangeMarkers.put(new Integer(index), markers);
            }
            markers.add(marker);
        } else if (layer == Layer.BACKGROUND) {
            markers = (Collection) this.backgroundRangeMarkers.get(
                new Integer(index));
            if (markers == null) {
                markers = new java.util.ArrayList();
                this.backgroundRangeMarkers.put(new Integer(index), markers);
            }
            markers.add(marker);
        }
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Clears the (foreground and background) range markers for a particular
     * renderer.
     *
     * @param index  the renderer index.
     */
    public void clearRangeMarkers(int index) {
        Integer key = new Integer(index);
        if (this.backgroundRangeMarkers != null) {
            Collection markers =
                (Collection) this.backgroundRangeMarkers.get(key);
            if (markers != null) {
                markers.clear();
            }
        }
        if (this.foregroundRangeMarkers != null) {
            Collection markers =
                (Collection) this.foregroundRangeMarkers.get(key);
            if (markers != null) {
                markers.clear();
            }
        }
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Adds an annotation to the plot and sends a {@link PlotChangeEvent} to all
     * registered listeners.
     *
     * @param annotation  the annotation (<code>null</code> not permitted).
     */
    public void addAnnotation(XYAnnotation annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("Null 'annotation' argument.");
        }
        this.annotations.add(annotation);
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Removes an annotation from the plot and sends a {@link PlotChangeEvent}
     * to all registered listeners.
     *
     * @param annotation  the annotation (<code>null</code> not permitted).
     *
     * @return A boolean (indicates whether or not the annotation was removed).
     */
    public boolean removeAnnotation(XYAnnotation annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException("Null 'annotation' argument.");
        }
        boolean removed = this.annotations.remove(annotation);
        if (removed) {
            notifyListeners(new PlotChangeEvent(this));
        }
        return removed;
    }

    /**
     * Clears all the annotations and sends a {@link PlotChangeEvent} to all
     * registered listeners.
     */
    public void clearAnnotations() {
        this.annotations.clear();
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Calculates the space required for all the axes in the plot.
     *
     * @param g2  the graphics device.
     * @param plotArea  the plot area.
     *
     * @return The required space.
     */
    protected AxisSpace calculateAxisSpace(Graphics2D g2,
                                           Rectangle2D plotArea) {
        AxisSpace space = new AxisSpace();
        space = calculateDomainAxisSpace(g2, plotArea, space);
        space = calculateRangeAxisSpace(g2, plotArea, space);
        return space;
    }

    /**
     * Calculates the space required for the domain axis/axes.
     *
     * @param g2  the graphics device.
     * @param plotArea  the plot area.
     * @param space  a carrier for the result (<code>null</code> permitted).
     *
     * @return The required space.
     */
    protected AxisSpace calculateDomainAxisSpace(Graphics2D g2,
            Rectangle2D plotArea, AxisSpace space) {

        if (space == null) {
            space = new AxisSpace();
        }

        // reserve some space for the domain axis...
        if (this.fixedDomainAxisSpace != null) {
            if (this.orientation == PlotOrientation.HORIZONTAL) {
                space.ensureAtLeast(this.fixedDomainAxisSpace.getLeft(),
                                    RectangleEdge.LEFT);
                space.ensureAtLeast(this.fixedDomainAxisSpace.getRight(),
                                    RectangleEdge.RIGHT);
            } else if (this.orientation == PlotOrientation.VERTICAL) {
                space.ensureAtLeast(this.fixedDomainAxisSpace.getTop(),
                                    RectangleEdge.TOP);
                space.ensureAtLeast(this.fixedDomainAxisSpace.getBottom(),
                                    RectangleEdge.BOTTOM);
            }
        } else {
            // reserve space for the domain axes...
            for (int i = 0; i < this.domainAxes.size(); i++) {
                Axis axis = (Axis) this.domainAxes.get(i);
                if (axis != null) {
                    RectangleEdge edge = getDomainAxisEdge(i);
                    space = axis.reserveSpace(g2, this, plotArea, edge,
                            space);
                }
            }
        }

        return space;

    }

    /**
     * Calculates the space required for the range axis/axes.
     *
     * @param g2  the graphics device.
     * @param plotArea  the plot area.
     * @param space  a carrier for the result (<code>null</code> permitted).
     *
     * @return The required space.
     */
    protected AxisSpace calculateRangeAxisSpace(Graphics2D g2,
            Rectangle2D plotArea, AxisSpace space) {

        if (space == null) {
            space = new AxisSpace();
        }

        // reserve some space for the range axis...
        if (this.fixedRangeAxisSpace != null) {
            if (this.orientation == PlotOrientation.HORIZONTAL) {
                space.ensureAtLeast(this.fixedRangeAxisSpace.getTop(),
                                    RectangleEdge.TOP);
                space.ensureAtLeast(this.fixedRangeAxisSpace.getBottom(),
                                    RectangleEdge.BOTTOM);
            } else if (this.orientation == PlotOrientation.VERTICAL) {
                space.ensureAtLeast(this.fixedRangeAxisSpace.getLeft(),
                                    RectangleEdge.LEFT);
                space.ensureAtLeast(this.fixedRangeAxisSpace.getRight(),
                                    RectangleEdge.RIGHT);
            }
        } else {
            // reserve space for the range axes...
            for (int i = 0; i < this.rangeAxes.size(); i++) {
                Axis axis = (Axis) this.rangeAxes.get(i);
                if (axis != null) {
                    RectangleEdge edge = getRangeAxisEdge(i);
                    space = axis.reserveSpace(g2, this, plotArea, edge,
                            space);
                }
            }
        }
        return space;

    }

    /**
     * Draws the plot within the specified area on a graphics device.
     *
     * @param g2  the graphics device.
     * @param area  the plot area (in Java2D space).
     * @param anchor  an anchor point in Java2D space (<code>null</code>
     *                permitted).
     * @param parentState  the state from the parent plot, if there is one
     *                     (<code>null</code> permitted).
     * @param info  collects chart drawing information (<code>null</code>
     *              permitted).
     */
    public void draw(Graphics2D g2, Rectangle2D area, Point2D anchor,
                     PlotState parentState, PlotRenderingInfo info) {


        // if the plot area is too small, just return...
        boolean b1 = (area.getWidth() <= MINIMUM_WIDTH_TO_DRAW);
        boolean b2 = (area.getHeight() <= MINIMUM_HEIGHT_TO_DRAW);
        if (b1 || b2) {
            return;
        }


        // record the plot area...
        if (info != null) {
            info.setPlotArea(area);
        }

        // adjust the drawing area for the plot insets (if any)...
        RectangleInsets insets = getInsets();
        insets.trim(area);

        AxisSpace   space    = calculateAxisSpace(g2, area);
        Rectangle2D dataArea = space.shrink(area, null);
        this.axisOffset.trim(dataArea);

        if (info != null) {
            info.setDataArea(dataArea);
        }

        // draw the plot background and axes...
        drawBackground(g2, dataArea);
        Map axisStateMap = drawAxes(g2, area, dataArea, info);

        if ((anchor != null) && !dataArea.contains(anchor)) {
            anchor = null;
        }
        CrosshairState crosshairState = new CrosshairState();
        crosshairState.setCrosshairDistance(Double.POSITIVE_INFINITY);
        crosshairState.setAnchor(anchor);
        crosshairState.setCrosshairX(getDomainCrosshairValue());
        crosshairState.setCrosshairY(getRangeCrosshairValue());
        Shape     originalClip      = g2.getClip();
        Composite originalComposite = g2.getComposite();

        g2.clip(dataArea);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                getForegroundAlpha()));

        AxisState domainAxisState =
            (AxisState) axisStateMap.get(getDomainAxis());
        if (domainAxisState == null) {
            if (parentState != null) {
                domainAxisState =
                    (AxisState) parentState.getSharedAxisStates().get(
                        getDomainAxis());
            }
        }
        if (domainAxisState != null) {
            drawDomainTickBands(g2, dataArea, domainAxisState.getTicks());
            drawDomainGridlines(g2, dataArea, domainAxisState.getTicks());
        }

        AxisState rangeAxisState =
            (AxisState) axisStateMap.get(getRangeAxis());
        if (rangeAxisState == null) {
            if (parentState != null) {
                rangeAxisState =
                    (AxisState) parentState.getSharedAxisStates().get(
                        getRangeAxis());
            }
        }
        if (rangeAxisState != null) {
            drawRangeTickBands(g2, dataArea, rangeAxisState.getTicks());
            drawRangeGridlines(g2, dataArea, rangeAxisState.getTicks());
            drawZeroRangeBaseline(g2, dataArea);
        }

        // draw the markers that are associated with a specific renderer...
        for (int i = 0; i < this.renderers.size(); i++) {
            drawDomainMarkers(g2, dataArea, i, Layer.BACKGROUND);
        }
        for (int i = 0; i < this.renderers.size(); i++) {
            drawRangeMarkers(g2, dataArea, i, Layer.BACKGROUND);
        }

        // now draw annotations and render data items...
        boolean               foundData = false;
        DatasetRenderingOrder order     = getDatasetRenderingOrder();
        if (order == DatasetRenderingOrder.FORWARD) {

            // draw background annotations
            int rendererCount = this.renderers.size();
            for (int i = 0; i < rendererCount; i++) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis  = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                                      Layer.BACKGROUND, info);
                }
            }


            // render data items...
            for (int i = 0; i < getDatasetCount(); i++) {
                foundData = render(g2, dataArea, i, info, crosshairState)
                            || foundData;
            }


            // draw foreground annotations
            for (int i = 0; i < rendererCount; i++) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis  = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                                      Layer.FOREGROUND, info);
                }
            }

        } else if (order == DatasetRenderingOrder.REVERSE) {

            // draw background annotations
            int rendererCount = this.renderers.size();
            for (int i = rendererCount - 1; i >= 0; i--) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis  = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                                      Layer.BACKGROUND, info);
                }
            }

            ucar.unidata.util.Trace.call1("renderData");
            for (int i = getDatasetCount() - 1; i >= 0; i--) {
                foundData = render(g2, dataArea, i, info, crosshairState)
                            || foundData;
            }
            ucar.unidata.util.Trace.call2("renderData");

            // draw foreground annotations
            for (int i = rendererCount - 1; i >= 0; i--) {
                XYItemRenderer r = getRenderer(i);
                if (r != null) {
                    ValueAxis domainAxis = getDomainAxisForDataset(i);
                    ValueAxis rangeAxis  = getRangeAxisForDataset(i);
                    r.drawAnnotations(g2, dataArea, domainAxis, rangeAxis,
                                      Layer.FOREGROUND, info);
                }
            }

        }

        PlotOrientation orient = getOrientation();

        // draw domain crosshair if required...
        if ( !this.domainCrosshairLockedOnData && (anchor != null)) {
            double xx = getDomainAxis().java2DToValue(anchor.getX(),
                            dataArea, getDomainAxisEdge());
            crosshairState.setCrosshairX(xx);
        }
        setDomainCrosshairValue(crosshairState.getCrosshairX(), false);
        if (isDomainCrosshairVisible()) {
            double x      = getDomainCrosshairValue();
            Paint  paint  = getDomainCrosshairPaint();
            Stroke stroke = getDomainCrosshairStroke();
            if (orient == PlotOrientation.HORIZONTAL) {
                drawHorizontalLine(g2, dataArea, x, stroke, paint);
            } else if (orient == PlotOrientation.VERTICAL) {
                drawVerticalLine(g2, dataArea, x, stroke, paint);
            }
        }

        // draw range crosshair if required...
        if ( !this.rangeCrosshairLockedOnData && (anchor != null)) {
            double yy = getRangeAxis().java2DToValue(anchor.getY(), dataArea,
                            getRangeAxisEdge());
            crosshairState.setCrosshairX(yy);
        }
        setRangeCrosshairValue(crosshairState.getCrosshairY(), false);
        if (isRangeCrosshairVisible()
                && getRangeAxis().getRange().contains(
                    getRangeCrosshairValue())) {
            double y      = getRangeCrosshairValue();
            Paint  paint  = getRangeCrosshairPaint();
            Stroke stroke = getRangeCrosshairStroke();
            if (orient == PlotOrientation.HORIZONTAL) {
                drawVerticalLine(g2, dataArea, y, stroke, paint);
            } else if (orient == PlotOrientation.VERTICAL) {
                drawHorizontalLine(g2, dataArea, y, stroke, paint);
            }
        }

        if ( !foundData) {
            drawNoDataMessage(g2, dataArea);
        }

        for (int i = 0; i < this.renderers.size(); i++) {
            drawDomainMarkers(g2, dataArea, i, Layer.FOREGROUND);
        }
        for (int i = 0; i < this.renderers.size(); i++) {
            drawRangeMarkers(g2, dataArea, i, Layer.FOREGROUND);
        }

        drawAnnotations(g2, dataArea, info);
        g2.setClip(originalClip);
        g2.setComposite(originalComposite);

        drawOutline(g2, dataArea);

    }

    /**
     * Draws the background for the plot.
     *
     * @param g2  the graphics device.
     * @param area  the area.
     */
    public void drawBackground(Graphics2D g2, Rectangle2D area) {
        fillBackground(g2, area);
        drawQuadrants(g2, area);
        drawBackgroundImage(g2, area);
    }

    /**
     * Draws the quadrants.
     *
     * @param g2  the graphics device.
     * @param area  the area.
     */
    protected void drawQuadrants(Graphics2D g2, Rectangle2D area) {

        //  0 | 1
        //  --+--
        //  2 | 3
        boolean       somethingToDraw = false;

        ValueAxis     xAxis           = getDomainAxis();
        double        x               = this.quadrantOrigin.getX();
        double        xx = xAxis.valueToJava2D(x, area, getDomainAxisEdge());

        ValueAxis     yAxis           = getRangeAxis();
        double        y               = this.quadrantOrigin.getY();
        double        yy = yAxis.valueToJava2D(y, area, getRangeAxisEdge());

        double        xmin            = xAxis.getLowerBound();
        double xxmin = xAxis.valueToJava2D(xmin, area, getDomainAxisEdge());

        double        xmax            = xAxis.getUpperBound();
        double xxmax = xAxis.valueToJava2D(xmax, area, getDomainAxisEdge());

        double        ymin            = yAxis.getLowerBound();
        double yymin = yAxis.valueToJava2D(ymin, area, getRangeAxisEdge());

        double        ymax            = yAxis.getUpperBound();
        double yymax = yAxis.valueToJava2D(ymax, area, getRangeAxisEdge());

        Rectangle2D[] r = new Rectangle2D[] { null, null, null, null };
        if (this.quadrantPaint[0] != null) {
            if ((x > xmin) && (y < ymax)) {
                if (this.orientation == PlotOrientation.HORIZONTAL) {
                    r[0] = new Rectangle2D.Double(Math.min(yymax, yy),
                            Math.min(xxmin, xx), Math.abs(yy - yymax),
                            Math.abs(xx - xxmin));
                } else {  // PlotOrientation.VERTICAL
                    r[0] = new Rectangle2D.Double(Math.min(xxmin, xx),
                            Math.min(yymax, yy), Math.abs(xx - xxmin),
                            Math.abs(yy - yymax));
                }
                somethingToDraw = true;
            }
        }
        if (this.quadrantPaint[1] != null) {
            if ((x < xmax) && (y < ymax)) {
                if (this.orientation == PlotOrientation.HORIZONTAL) {
                    r[1] = new Rectangle2D.Double(Math.min(yymax, yy),
                            Math.min(xxmax, xx), Math.abs(yy - yymax),
                            Math.abs(xx - xxmax));
                } else {  // PlotOrientation.VERTICAL
                    r[1] = new Rectangle2D.Double(Math.min(xx, xxmax),
                            Math.min(yymax, yy), Math.abs(xx - xxmax),
                            Math.abs(yy - yymax));
                }
                somethingToDraw = true;
            }
        }
        if (this.quadrantPaint[2] != null) {
            if ((x > xmin) && (y > ymin)) {
                if (this.orientation == PlotOrientation.HORIZONTAL) {
                    r[2] = new Rectangle2D.Double(Math.min(yymin, yy),
                            Math.min(xxmin, xx), Math.abs(yy - yymin),
                            Math.abs(xx - xxmin));
                } else {  // PlotOrientation.VERTICAL
                    r[2] = new Rectangle2D.Double(Math.min(xxmin, xx),
                            Math.min(yymin, yy), Math.abs(xx - xxmin),
                            Math.abs(yy - yymin));
                }
                somethingToDraw = true;
            }
        }
        if (this.quadrantPaint[3] != null) {
            if ((x < xmax) && (y > ymin)) {
                if (this.orientation == PlotOrientation.HORIZONTAL) {
                    r[3] = new Rectangle2D.Double(Math.min(yymin, yy),
                            Math.min(xxmax, xx), Math.abs(yy - yymin),
                            Math.abs(xx - xxmax));
                } else {  // PlotOrientation.VERTICAL
                    r[3] = new Rectangle2D.Double(Math.min(xx, xxmax),
                            Math.min(yymin, yy), Math.abs(xx - xxmax),
                            Math.abs(yy - yymin));
                }
                somethingToDraw = true;
            }
        }
        if (somethingToDraw) {
            Composite originalComposite = g2.getComposite();
            g2.setComposite(
                AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, getBackgroundAlpha()));
            for (int i = 0; i < 4; i++) {
                if ((this.quadrantPaint[i] != null) && (r[i] != null)) {
                    g2.setPaint(this.quadrantPaint[i]);
                    g2.fill(r[i]);
                }
            }
            g2.setComposite(originalComposite);
        }
    }

    /**
     * Draws the domain tick bands, if any.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param ticks  the ticks.
     */
    public void drawDomainTickBands(Graphics2D g2, Rectangle2D dataArea,
                                    List ticks) {
        // draw the domain tick bands, if any...
        Paint bandPaint = getDomainTickBandPaint();
        if (bandPaint != null) {
            boolean   fillBand = false;
            ValueAxis xAxis    = getDomainAxis();
            double    previous = xAxis.getLowerBound();
            Iterator  iterator = ticks.iterator();
            while (iterator.hasNext()) {
                ValueTick tick    = (ValueTick) iterator.next();
                double    current = tick.getValue();
                if (fillBand) {
                    getRenderer().fillDomainGridBand(g2, this, xAxis,
                            dataArea, previous, current);
                }
                previous = current;
                fillBand = !fillBand;
            }
            double end = xAxis.getUpperBound();
            if (fillBand) {
                getRenderer().fillDomainGridBand(g2, this, xAxis, dataArea,
                        previous, end);
            }
        }
    }

    /**
     * Draws the range tick bands, if any.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param ticks  the ticks.
     */
    public void drawRangeTickBands(Graphics2D g2, Rectangle2D dataArea,
                                   List ticks) {

        // draw the range tick bands, if any...
        Paint bandPaint = getRangeTickBandPaint();
        if (bandPaint != null) {
            boolean   fillBand = false;
            ValueAxis axis     = getRangeAxis();
            double    previous = axis.getLowerBound();
            Iterator  iterator = ticks.iterator();
            while (iterator.hasNext()) {
                ValueTick tick    = (ValueTick) iterator.next();
                double    current = tick.getValue();
                if (fillBand) {
                    getRenderer().fillRangeGridBand(g2, this, axis, dataArea,
                            previous, current);
                }
                previous = current;
                fillBand = !fillBand;
            }
            double end = axis.getUpperBound();
            if (fillBand) {
                getRenderer().fillRangeGridBand(g2, this, axis, dataArea,
                        previous, end);
            }
        }
    }

    /**
     * A utility method for drawing the axes.
     *
     * @param g2  the graphics device (<code>null</code> not permitted).
     * @param plotArea  the plot area (<code>null</code> not permitted).
     * @param dataArea  the data area (<code>null</code> not permitted).
     * @param plotState  collects information about the plot (<code>null</code>
     *                   permitted).
     *
     * @return A map containing the state for each axis drawn.
     */
    protected Map drawAxes(Graphics2D g2, Rectangle2D plotArea,
                           Rectangle2D dataArea,
                           PlotRenderingInfo plotState) {

        AxisCollection axisCollection = new AxisCollection();

        // add domain axes to lists...
        for (int index = 0; index < this.domainAxes.size(); index++) {
            ValueAxis axis = (ValueAxis) this.domainAxes.get(index);
            if (axis != null) {
                axisCollection.add(axis, getDomainAxisEdge(index));
            }
        }

        // add range axes to lists...
        for (int index = 0; index < this.rangeAxes.size(); index++) {
            ValueAxis yAxis = (ValueAxis) this.rangeAxes.get(index);
            if (yAxis != null) {
                axisCollection.add(yAxis, getRangeAxisEdge(index));
            }
        }

        Map axisStateMap = new HashMap();

        // draw the top axes
        double cursor =
            dataArea.getMinY()
            - this.axisOffset.calculateTopOutset(dataArea.getHeight());
        Iterator iterator = axisCollection.getAxesAtTop().iterator();
        while (iterator.hasNext()) {
            ValueAxis axis = (ValueAxis) iterator.next();
            AxisState info = axis.draw(g2, cursor, plotArea, dataArea,
                                       RectangleEdge.TOP, plotState);
            cursor = info.getCursor();
            axisStateMap.put(axis, info);
        }

        // draw the bottom axes
        cursor =
            dataArea.getMaxY()
            + this.axisOffset.calculateBottomOutset(dataArea.getHeight());
        iterator = axisCollection.getAxesAtBottom().iterator();
        while (iterator.hasNext()) {
            ValueAxis axis = (ValueAxis) iterator.next();
            AxisState info = axis.draw(g2, cursor, plotArea, dataArea,
                                       RectangleEdge.BOTTOM, plotState);
            cursor = info.getCursor();
            axisStateMap.put(axis, info);
        }

        // draw the left axes
        cursor = dataArea.getMinX()
                 - this.axisOffset.calculateLeftOutset(dataArea.getWidth());
        iterator = axisCollection.getAxesAtLeft().iterator();
        while (iterator.hasNext()) {
            ValueAxis axis = (ValueAxis) iterator.next();
            AxisState info = axis.draw(g2, cursor, plotArea, dataArea,
                                       RectangleEdge.LEFT, plotState);
            cursor = info.getCursor();
            axisStateMap.put(axis, info);
        }

        // draw the right axes
        cursor = dataArea.getMaxX()
                 + this.axisOffset.calculateRightOutset(dataArea.getWidth());
        iterator = axisCollection.getAxesAtRight().iterator();
        while (iterator.hasNext()) {
            ValueAxis axis = (ValueAxis) iterator.next();
            AxisState info = axis.draw(g2, cursor, plotArea, dataArea,
                                       RectangleEdge.RIGHT, plotState);
            cursor = info.getCursor();
            axisStateMap.put(axis, info);
        }

        return axisStateMap;
    }

    /**
     * Draws a representation of the data within the dataArea region, using the
     * current renderer.
     * <P>
     * The <code>info</code> and <code>crosshairState</code> arguments may be
     * <code>null</code>.
     *
     * @param g2  the graphics device.
     * @param dataArea  the region in which the data is to be drawn.
     * @param index  the dataset index.
     * @param info  an optional object for collection dimension information.
     * @param crosshairState  collects crosshair information
     *                        (<code>null</code> permitted).
     *
     * @return A flag that indicates whether any data was actually rendered.
     */
    public boolean render(Graphics2D g2, Rectangle2D dataArea, int index,
                          PlotRenderingInfo info,
                          CrosshairState crosshairState) {

        boolean   foundData = false;
        XYDataset dataset   = getDataset(index);
        if ( !DatasetUtilities.isEmptyOrNull(dataset)) {
            foundData = true;
            ValueAxis      xAxis    = getDomainAxisForDataset(index);
            ValueAxis      yAxis    = getRangeAxisForDataset(index);
            XYItemRenderer renderer = getRenderer(index);
            if (renderer == null) {
                renderer = getRenderer();
            }

            XYItemRendererState state = renderer.initialise(g2, dataArea,
                                            this, dataset, info);
            int                  passCount   = renderer.getPassCount();

            SeriesRenderingOrder seriesOrder = getSeriesRenderingOrder();
            ucar.unidata.util.Trace.call1("render", " pass:" + passCount);
            if (seriesOrder == SeriesRenderingOrder.REVERSE) {
                //render series in reverse order
                for (int pass = 0; pass < passCount; pass++) {
                    int seriesCount = dataset.getSeriesCount();
                    for (int series = seriesCount - 1; series >= 0;
                            series--) {
                        int itemCount = dataset.getItemCount(series);
                        ucar.unidata.util.Trace.call1("series loop",
                                " items=" + itemCount + " renderer:"
                                + renderer.getClass().getName());
                        for (int item = 0; item < itemCount; item++) {
                            renderer.drawItem(g2, state, dataArea, info,
                                    this, xAxis, yAxis, dataset, series,
                                    item, crosshairState, pass);
                        }
                        ucar.unidata.util.Trace.call2("series loop");
                    }
                }
            } else {
                //render series in forward order
                for (int pass = 0; pass < passCount; pass++) {
                    int seriesCount = dataset.getSeriesCount();
                    for (int series = 0; series < seriesCount; series++) {
                        int itemCount = dataset.getItemCount(series);
                        for (int item = 0; item < itemCount; item++) {
                            renderer.drawItem(g2, state, dataArea, info,
                                    this, xAxis, yAxis, dataset, series,
                                    item, crosshairState, pass);
                        }
                    }
                }
            }
            ucar.unidata.util.Trace.call2("render");
        }
        return foundData;
    }

    /**
     * Returns the domain axis for a dataset.
     *
     * @param index  the dataset index.
     *
     * @return The axis.
     */
    public ValueAxis getDomainAxisForDataset(int index) {

        if ((index < 0) || (index >= getDatasetCount())) {
            throw new IllegalArgumentException(
                "Index 'index' out of bounds.");
        }

        ValueAxis valueAxis = null;
        Integer axisIndex =
            (Integer) this.datasetToDomainAxisMap.get(new Integer(index));
        if (axisIndex != null) {
            valueAxis = getDomainAxis(axisIndex.intValue());
        } else {
            valueAxis = getDomainAxis(0);
        }
        return valueAxis;

    }

    /**
     * Returns the range axis for a dataset.
     *
     * @param index  the dataset index.
     *
     * @return The axis.
     */
    public ValueAxis getRangeAxisForDataset(int index) {

        if ((index < 0) || (index >= getDatasetCount())) {
            throw new IllegalArgumentException(
                "Index 'index' out of bounds.");
        }

        ValueAxis valueAxis = null;
        Integer axisIndex =
            (Integer) this.datasetToRangeAxisMap.get(new Integer(index));
        if (axisIndex != null) {
            valueAxis = getRangeAxis(axisIndex.intValue());
        } else {
            valueAxis = getRangeAxis(0);
        }
        return valueAxis;

    }

    /**
     * Draws the gridlines for the plot, if they are visible.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param ticks  the ticks.
     */
    protected void drawDomainGridlines(Graphics2D g2, Rectangle2D dataArea,
                                       List ticks) {

        // no renderer, no gridlines...
        if (getRenderer() == null) {
            return;
        }

        // draw the domain grid lines, if any...
        if (isDomainGridlinesVisible()) {
            Stroke gridStroke = getDomainGridlineStroke();
            Paint  gridPaint  = getDomainGridlinePaint();
            if ((gridStroke != null) && (gridPaint != null)) {
                Iterator iterator = ticks.iterator();
                while (iterator.hasNext()) {
                    ValueTick tick = (ValueTick) iterator.next();
                    getRenderer().drawDomainGridLine(g2, this,
                            getDomainAxis(), dataArea, tick.getValue());
                }
            }
        }
    }

    /**
     * Draws the gridlines for the plot's primary range axis, if they are
     * visible.
     *
     * @param g2  the graphics device.
     * @param area  the data area.
     * @param ticks  the ticks.
     */
    protected void drawRangeGridlines(Graphics2D g2, Rectangle2D area,
                                      List ticks) {

        // draw the range grid lines, if any...
        if (isRangeGridlinesVisible()) {
            Stroke    gridStroke = getRangeGridlineStroke();
            Paint     gridPaint  = getRangeGridlinePaint();
            ValueAxis axis       = getRangeAxis();
            if (axis != null) {
                Iterator iterator = ticks.iterator();
                while (iterator.hasNext()) {
                    ValueTick tick = (ValueTick) iterator.next();
                    if ((tick.getValue() != 0.0)
                            || !isRangeZeroBaselineVisible()) {
                        getRenderer().drawRangeLine(g2, this, getRangeAxis(),
                                area, tick.getValue(), gridPaint, gridStroke);
                    }
                }
            }
        }
    }

    /**
     * Draws a base line across the chart at value zero on the range axis.
     *
     * @param g2  the graphics device.
     * @param area  the data area.
     */
    protected void drawZeroRangeBaseline(Graphics2D g2, Rectangle2D area) {
        if (isRangeZeroBaselineVisible()) {
            getRenderer().drawRangeLine(g2, this, getRangeAxis(), area, 0.0,
                                        this.rangeZeroBaselinePaint,
                                        this.rangeZeroBaselineStroke);
        }
    }

    /**
     * Draws the annotations for the plot.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param info  the chart rendering info.
     */
    public void drawAnnotations(Graphics2D g2, Rectangle2D dataArea,
                                PlotRenderingInfo info) {

        Iterator iterator = this.annotations.iterator();
        while (iterator.hasNext()) {
            XYAnnotation annotation = (XYAnnotation) iterator.next();
            ValueAxis    xAxis      = getDomainAxis();
            ValueAxis    yAxis      = getRangeAxis();
            annotation.draw(g2, this, dataArea, xAxis, yAxis, 0, info);
        }

    }

    /**
     * Draws the domain markers (if any) for an axis and layer.  This method is
     * typically called from within the draw() method.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param index  the renderer index.
     * @param layer  the layer (foreground or background).
     */
    protected void drawDomainMarkers(Graphics2D g2, Rectangle2D dataArea,
                                     int index, Layer layer) {

        XYItemRenderer r = getRenderer(index);
        if (r == null) {
            return;
        }

        Collection markers = getDomainMarkers(index, layer);
        ValueAxis  axis    = getDomainAxisForDataset(index);
        if ((markers != null) && (axis != null)) {
            Iterator iterator = markers.iterator();
            while (iterator.hasNext()) {
                Marker marker = (Marker) iterator.next();
                r.drawDomainMarker(g2, this, axis, marker, dataArea);
            }
        }

    }

    /**
     * Draws the range markers (if any) for a renderer and layer.  This method
     * is typically called from within the draw() method.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param index  the renderer index.
     * @param layer  the layer (foreground or background).
     */
    protected void drawRangeMarkers(Graphics2D g2, Rectangle2D dataArea,
                                    int index, Layer layer) {

        XYItemRenderer r = getRenderer(index);
        if (r == null) {
            return;
        }

        Collection markers = getRangeMarkers(index, layer);
        ValueAxis  axis    = getRangeAxis(index);
        // TODO: get the axis that the renderer maps to
        if ((markers != null) && (axis != null)) {
            Iterator iterator = markers.iterator();
            while (iterator.hasNext()) {
                Marker marker = (Marker) iterator.next();
                r.drawRangeMarker(g2, this, axis, marker, dataArea);
            }
        }

    }

    /**
     * Returns the list of domain markers (read only) for the specified layer.
     *
     * @param layer  the layer (foreground or background).
     *
     * @return The list of domain markers.
     */
    public Collection getDomainMarkers(Layer layer) {
        return getDomainMarkers(0, layer);
    }

    /**
     * Returns the list of range markers (read only) for the specified layer.
     *
     * @param layer  the layer (foreground or background).
     *
     * @return The list of range markers.
     */
    public Collection getRangeMarkers(Layer layer) {
        return getRangeMarkers(0, layer);
    }

    /**
     * Returns a collection of domain markers for a particular renderer and
     * layer.
     *
     * @param index  the renderer index.
     * @param layer  the layer.
     *
     * @return A collection of markers (possibly <code>null</code>).
     */
    public Collection getDomainMarkers(int index, Layer layer) {
        Collection result = null;
        Integer    key    = new Integer(index);
        if (layer == Layer.FOREGROUND) {
            result = (Collection) this.foregroundDomainMarkers.get(key);
        } else if (layer == Layer.BACKGROUND) {
            result = (Collection) this.backgroundDomainMarkers.get(key);
        }
        if (result != null) {
            result = Collections.unmodifiableCollection(result);
        }
        return result;
    }

    /**
     * Returns a collection of range markers for a particular renderer and
     * layer.
     *
     * @param index  the renderer index.
     * @param layer  the layer.
     *
     * @return A collection of markers (possibly <code>null</code>).
     */
    public Collection getRangeMarkers(int index, Layer layer) {
        Collection result = null;
        Integer    key    = new Integer(index);
        if (layer == Layer.FOREGROUND) {
            result = (Collection) this.foregroundRangeMarkers.get(key);
        } else if (layer == Layer.BACKGROUND) {
            result = (Collection) this.backgroundRangeMarkers.get(key);
        }
        if (result != null) {
            result = Collections.unmodifiableCollection(result);
        }
        return result;
    }

    /**
     * Utility method for drawing a horizontal line across the data area of the
     * plot.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param value  the coordinate, where to draw the line.
     * @param stroke  the stroke to use.
     * @param paint  the paint to use.
     */
    protected void drawHorizontalLine(Graphics2D g2, Rectangle2D dataArea,
                                      double value, Stroke stroke,
                                      Paint paint) {

        ValueAxis axis = getRangeAxis();
        if (getOrientation() == PlotOrientation.HORIZONTAL) {
            axis = getDomainAxis();
        }
        if (axis.getRange().contains(value)) {
            double yy = axis.valueToJava2D(value, dataArea,
                                           RectangleEdge.LEFT);
            Line2D line = new Line2D.Double(dataArea.getMinX(), yy,
                                            dataArea.getMaxX(), yy);
            g2.setStroke(stroke);
            g2.setPaint(paint);
            g2.draw(line);
        }

    }

    /**
     * Utility method for drawing a vertical line on the data area of the plot.
     *
     * @param g2  the graphics device.
     * @param dataArea  the data area.
     * @param value  the coordinate, where to draw the line.
     * @param stroke  the stroke to use.
     * @param paint  the paint to use.
     */
    protected void drawVerticalLine(Graphics2D g2, Rectangle2D dataArea,
                                    double value, Stroke stroke,
                                    Paint paint) {

        ValueAxis axis = getDomainAxis();
        if (getOrientation() == PlotOrientation.HORIZONTAL) {
            axis = getRangeAxis();
        }
        if (axis.getRange().contains(value)) {
            double xx = axis.valueToJava2D(value, dataArea,
                                           RectangleEdge.BOTTOM);
            Line2D line = new Line2D.Double(xx, dataArea.getMinY(), xx,
                                            dataArea.getMaxY());
            g2.setStroke(stroke);
            g2.setPaint(paint);
            g2.draw(line);
        }

    }

    /**
     * Handles a 'click' on the plot by updating the anchor values...
     *
     * @param x  the x-coordinate, where the click occurred, in Java2D space.
     * @param y  the y-coordinate, where the click occurred, in Java2D space.
     * @param info  object containing information about the plot dimensions.
     */
    public void handleClick(int x, int y, PlotRenderingInfo info) {

        Rectangle2D dataArea = info.getDataArea();
        if (dataArea.contains(x, y)) {
            // set the anchor value for the horizontal axis...
            ValueAxis da = getDomainAxis();
            if (da != null) {
                double hvalue = da.java2DToValue(x, info.getDataArea(),
                                    getDomainAxisEdge());

                setDomainCrosshairValue(hvalue);
            }

            // set the anchor value for the vertical axis...
            ValueAxis ra = getRangeAxis();
            if (ra != null) {
                double vvalue = ra.java2DToValue(y, info.getDataArea(),
                                    getRangeAxisEdge());
                setRangeCrosshairValue(vvalue);
            }
        }
    }

    /**
     * A utility method that returns a list of datasets that are mapped to a
     * particular axis.
     *
     * @param axisIndex  the axis index (<code>null</code> not permitted).
     *
     * @return A list of datasets.
     */
    private List getDatasetsMappedToDomainAxis(Integer axisIndex) {
        if (axisIndex == null) {
            throw new IllegalArgumentException("Null 'axisIndex' argument.");
        }
        List result = new ArrayList();
        for (int i = 0; i < this.datasets.size(); i++) {
            Integer mappedAxis =
                (Integer) this.datasetToDomainAxisMap.get(new Integer(i));
            if (mappedAxis == null) {
                if (axisIndex.equals(ZERO)) {
                    result.add(this.datasets.get(i));
                }
            } else {
                if (mappedAxis.equals(axisIndex)) {
                    result.add(this.datasets.get(i));
                }
            }
        }
        return result;
    }

    /**
     * A utility method that returns a list of datasets that are mapped to a
     * particular axis.
     *
     * @param axisIndex  the axis index (<code>null</code> not permitted).
     *
     * @return A list of datasets.
     */
    private List getDatasetsMappedToRangeAxis(Integer axisIndex) {
        if (axisIndex == null) {
            throw new IllegalArgumentException("Null 'axisIndex' argument.");
        }
        List result = new ArrayList();
        for (int i = 0; i < this.datasets.size(); i++) {
            Integer mappedAxis =
                (Integer) this.datasetToRangeAxisMap.get(new Integer(i));
            if (mappedAxis == null) {
                if (axisIndex.equals(ZERO)) {
                    result.add(this.datasets.get(i));
                }
            } else {
                if (mappedAxis.equals(axisIndex)) {
                    result.add(this.datasets.get(i));
                }
            }
        }
        return result;
    }

    /**
     * Returns the index of the given domain axis.
     *
     * @param axis  the axis.
     *
     * @return The axis index.
     */
    public int getDomainAxisIndex(ValueAxis axis) {
        int result = this.domainAxes.indexOf(axis);
        if (result < 0) {
            // try the parent plot
            Plot parent = getParent();
            if (parent instanceof MyXYPlot) {
                MyXYPlot p = (MyXYPlot) parent;
                result = p.getDomainAxisIndex(axis);
            }
        }
        return result;
    }

    /**
     * Returns the index of the given range axis.
     *
     * @param axis  the axis.
     *
     * @return The axis index.
     */
    public int getRangeAxisIndex(ValueAxis axis) {
        int result = this.rangeAxes.indexOf(axis);
        if (result < 0) {
            // try the parent plot
            Plot parent = getParent();
            if (parent instanceof MyXYPlot) {
                MyXYPlot p = (MyXYPlot) parent;
                result = p.getRangeAxisIndex(axis);
            }
        }
        return result;
    }

    /**
     * Returns the range for the specified axis.
     *
     * @param axis  the axis.
     *
     * @return The range.
     */
    public Range getDataRange(ValueAxis axis) {

        Range   result         = null;
        List    mappedDatasets = new ArrayList();
        boolean isDomainAxis   = true;

        // is it a domain axis?
        int domainIndex = getDomainAxisIndex(axis);
        if (domainIndex >= 0) {
            isDomainAxis = true;
            mappedDatasets.addAll(
                getDatasetsMappedToDomainAxis(new Integer(domainIndex)));
        }

        // or is it a range axis?
        int rangeIndex = getRangeAxisIndex(axis);
        if (rangeIndex >= 0) {
            isDomainAxis = false;
            mappedDatasets.addAll(
                getDatasetsMappedToRangeAxis(new Integer(rangeIndex)));
        }

        // iterate through the datasets that map to the axis and get the union
        // of the ranges.
        Iterator iterator = mappedDatasets.iterator();
        while (iterator.hasNext()) {
            XYDataset d = (XYDataset) iterator.next();
            if (d != null) {
                XYItemRenderer r = getRendererForDataset(d);
                if (isDomainAxis) {
                    if (r != null) {
                        result = Range.combine(result, r.findDomainBounds(d));
                    } else {
                        result = Range.combine(result,
                                DatasetUtilities.findDomainBounds(d));
                    }
                } else {
                    if (r != null) {
                        result = Range.combine(result, r.findRangeBounds(d));
                    } else {
                        result = Range.combine(result,
                                DatasetUtilities.findRangeBounds(d));
                    }
                }
            }
        }
        return result;

    }

    /**
     * Receives notification of a change to the plot's dataset.
     * <P>
     * The axis ranges are updated if necessary.
     *
     * @param event  information about the event (not used here).
     */
    public void datasetChanged(DatasetChangeEvent event) {
        configureDomainAxes();
        configureRangeAxes();
        if (getParent() != null) {
            getParent().datasetChanged(event);
        } else {
            PlotChangeEvent e = new PlotChangeEvent(this);
            e.setType(ChartChangeEventType.DATASET_UPDATED);
            notifyListeners(e);
        }
    }

    /**
     * Receives notification of a renderer change event.
     *
     * @param event  the event.
     */
    public void rendererChanged(RendererChangeEvent event) {
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns a flag indicating whether or not the domain crosshair is visible.
     *
     * @return The flag.
     */
    public boolean isDomainCrosshairVisible() {
        return this.domainCrosshairVisible;
    }

    /**
     * Sets the flag indicating whether or not the domain crosshair is visible.
     *
     * @param flag  the new value of the flag.
     */
    public void setDomainCrosshairVisible(boolean flag) {

        if (this.domainCrosshairVisible != flag) {
            this.domainCrosshairVisible = flag;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns a flag indicating whether or not the crosshair should "lock-on"
     * to actual data values.
     *
     * @return The flag.
     */
    public boolean isDomainCrosshairLockedOnData() {
        return this.domainCrosshairLockedOnData;
    }

    /**
     * Sets the flag indicating whether or not the domain crosshair should
     * "lock-on" to actual data values.
     *
     * @param flag  the flag.
     */
    public void setDomainCrosshairLockedOnData(boolean flag) {

        if (this.domainCrosshairLockedOnData != flag) {
            this.domainCrosshairLockedOnData = flag;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the domain crosshair value.
     *
     * @return The value.
     */
    public double getDomainCrosshairValue() {
        return this.domainCrosshairValue;
    }

    /**
     * Sets the domain crosshair value and sends a {@link PlotChangeEvent} to
     * all registered listeners (provided that the domain crosshair is visible).
     *
     * @param value  the value.
     */
    public void setDomainCrosshairValue(double value) {
        setDomainCrosshairValue(value, true);
    }

    /**
     * Sets the domain crosshair value and, if requested, sends a
     * {@link PlotChangeEvent} to all registered listeners (provided that the
     * domain crosshair is visible).
     *
     * @param value  the new value.
     * @param notify  notify listeners?
     */
    public void setDomainCrosshairValue(double value, boolean notify) {
        this.domainCrosshairValue = value;
        if (isDomainCrosshairVisible() && notify) {
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Returns the {@link Stroke} used to draw the crosshair (if visible).
     *
     * @return The crosshair stroke.
     */
    public Stroke getDomainCrosshairStroke() {
        return this.domainCrosshairStroke;
    }

    /**
     * Sets the Stroke used to draw the crosshairs (if visible) and notifies
     * registered listeners that the axis has been modified.
     *
     * @param stroke  the new crosshair stroke.
     */
    public void setDomainCrosshairStroke(Stroke stroke) {
        this.domainCrosshairStroke = stroke;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the domain crosshair color.
     *
     * @return The crosshair color.
     */
    public Paint getDomainCrosshairPaint() {
        return this.domainCrosshairPaint;
    }

    /**
     * Sets the Paint used to color the crosshairs (if visible) and notifies
     * registered listeners that the axis has been modified.
     *
     * @param paint the new crosshair paint.
     */
    public void setDomainCrosshairPaint(Paint paint) {
        this.domainCrosshairPaint = paint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns a flag indicating whether or not the range crosshair is visible.
     *
     * @return The flag.
     */
    public boolean isRangeCrosshairVisible() {
        return this.rangeCrosshairVisible;
    }

    /**
     * Sets the flag indicating whether or not the range crosshair is visible.
     *
     * @param flag  the new value of the flag.
     */
    public void setRangeCrosshairVisible(boolean flag) {

        if (this.rangeCrosshairVisible != flag) {
            this.rangeCrosshairVisible = flag;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns a flag indicating whether or not the crosshair should "lock-on"
     * to actual data values.
     *
     * @return The flag.
     */
    public boolean isRangeCrosshairLockedOnData() {
        return this.rangeCrosshairLockedOnData;
    }

    /**
     * Sets the flag indicating whether or not the range crosshair should
     * "lock-on" to actual data values.
     *
     * @param flag  the flag.
     */
    public void setRangeCrosshairLockedOnData(boolean flag) {

        if (this.rangeCrosshairLockedOnData != flag) {
            this.rangeCrosshairLockedOnData = flag;
            notifyListeners(new PlotChangeEvent(this));
        }

    }

    /**
     * Returns the range crosshair value.
     *
     * @return The value.
     */
    public double getRangeCrosshairValue() {
        return this.rangeCrosshairValue;
    }

    /**
     * Sets the domain crosshair value.
     * <P>
     * Registered listeners are notified that the plot has been modified, but
     * only if the crosshair is visible.
     *
     * @param value  the new value.
     */
    public void setRangeCrosshairValue(double value) {
        setRangeCrosshairValue(value, true);
    }

    /**
     * Sets the range crosshair value.
     * <P>
     * Registered listeners are notified that the axis has been modified, but
     * only if the crosshair is visible.
     *
     * @param value  the new value.
     * @param notify  a flag that controls whether or not listeners are
     *                notified.
     */
    public void setRangeCrosshairValue(double value, boolean notify) {
        this.rangeCrosshairValue = value;
        if (isRangeCrosshairVisible() && notify) {
            notifyListeners(new PlotChangeEvent(this));
        }
    }

    /**
     * Returns the Stroke used to draw the crosshair (if visible).
     *
     * @return The crosshair stroke.
     */
    public Stroke getRangeCrosshairStroke() {
        return this.rangeCrosshairStroke;
    }

    /**
     * Sets the Stroke used to draw the crosshairs (if visible) and notifies
     * registered listeners that the axis has been modified.
     *
     * @param stroke  the new crosshair stroke.
     */
    public void setRangeCrosshairStroke(Stroke stroke) {
        this.rangeCrosshairStroke = stroke;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the range crosshair color.
     *
     * @return The crosshair color.
     */
    public Paint getRangeCrosshairPaint() {
        return this.rangeCrosshairPaint;
    }

    /**
     * Sets the Paint used to color the crosshairs (if visible) and notifies
     * registered listeners that the axis has been modified.
     *
     * @param paint the new crosshair paint.
     */
    public void setRangeCrosshairPaint(Paint paint) {
        this.rangeCrosshairPaint = paint;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the fixed domain axis space.
     *
     * @return The fixed domain axis space (possibly <code>null</code>).
     */
    public AxisSpace getFixedDomainAxisSpace() {
        return this.fixedDomainAxisSpace;
    }

    /**
     * Sets the fixed domain axis space.
     *
     * @param space  the space.
     */
    public void setFixedDomainAxisSpace(AxisSpace space) {
        this.fixedDomainAxisSpace = space;
    }

    /**
     * Returns the fixed range axis space.
     *
     * @return The fixed range axis space.
     */
    public AxisSpace getFixedRangeAxisSpace() {
        return this.fixedRangeAxisSpace;
    }

    /**
     * Sets the fixed range axis space.
     *
     * @param space  the space.
     */
    public void setFixedRangeAxisSpace(AxisSpace space) {
        this.fixedRangeAxisSpace = space;
    }

    /**
     * Multiplies the range on the domain axis/axes by the specified factor.
     *
     * @param factor  the zoom factor.
     * @param info  the plot rendering info.
     * @param source  the source point.
     */
    public void zoomDomainAxes(double factor, PlotRenderingInfo info,
                               Point2D source) {
        for (int i = 0; i < this.domainAxes.size(); i++) {
            ValueAxis domainAxis = (ValueAxis) this.domainAxes.get(i);
            if (domainAxis != null) {
                domainAxis.resizeRange(factor);
            }
        }
    }

    /**
     * Zooms in on the domain axis/axes.  The new lower and upper bounds are
     * specified as percentages of the current axis range, where 0 percent is
     * the current lower bound and 100 percent is the current upper bound.
     *
     * @param lowerPercent  a percentage that determines the new lower bound
     *                      for the axis (e.g. 0.20 is twenty percent).
     * @param upperPercent  a percentage that determines the new upper bound
     *                      for the axis (e.g. 0.80 is eighty percent).
     * @param info  the plot rendering info.
     * @param source  the source point.
     */
    public void zoomDomainAxes(double lowerPercent, double upperPercent,
                               PlotRenderingInfo info, Point2D source) {
        for (int i = 0; i < this.domainAxes.size(); i++) {
            ValueAxis domainAxis = (ValueAxis) this.domainAxes.get(i);
            if (domainAxis != null) {
                domainAxis.zoomRange(lowerPercent, upperPercent);
            }
        }
    }

    /**
     * Multiplies the range on the range axis/axes by the specified factor.
     *
     * @param factor  the zoom factor.
     * @param info  the plot rendering info.
     * @param source  the source point.
     */
    public void zoomRangeAxes(double factor, PlotRenderingInfo info,
                              Point2D source) {
        for (int i = 0; i < this.rangeAxes.size(); i++) {
            ValueAxis rangeAxis = (ValueAxis) this.rangeAxes.get(i);
            if (rangeAxis != null) {
                rangeAxis.resizeRange(factor);
            }
        }
    }

    /**
     * Zooms in on the range axes.
     *
     * @param lowerPercent  the lower bound.
     * @param upperPercent  the upper bound.
     * @param info  the plot rendering info.
     * @param source  the source point.
     */
    public void zoomRangeAxes(double lowerPercent, double upperPercent,
                              PlotRenderingInfo info, Point2D source) {
        for (int i = 0; i < this.rangeAxes.size(); i++) {
            ValueAxis rangeAxis = (ValueAxis) this.rangeAxes.get(i);
            if (rangeAxis != null) {
                rangeAxis.zoomRange(lowerPercent, upperPercent);
            }
        }
    }

    /**
     * Returns <code>true</code>
     *
     * @return A boolean.
     */
    public boolean isDomainZoomable() {
        return true;
    }

    /**
     * Returns <code>true</code>
     *
     * @return A boolean.
     */
    public boolean isRangeZoomable() {
        return true;
    }

    /**
     * Returns the number of series in the primary dataset for this plot.  If
     * the dataset is <code>null</code>, the method returns 0.
     *
     * @return The series count.
     */
    public int getSeriesCount() {
        int       result  = 0;
        XYDataset dataset = getDataset();
        if (dataset != null) {
            result = dataset.getSeriesCount();
        }
        return result;
    }

    /**
     * Returns the fixed legend items, if any.
     *
     * @return The legend items (possibly <code>null</code>).
     */
    public LegendItemCollection getFixedLegendItems() {
        return this.fixedLegendItems;
    }

    /**
     * Sets the fixed legend items for the plot.  Leave this set to
     * <code>null</code> if you prefer the legend items to be created
     * automatically.
     *
     * @param items  the legend items (<code>null</code> permitted).
     */
    public void setFixedLegendItems(LegendItemCollection items) {
        this.fixedLegendItems = items;
        notifyListeners(new PlotChangeEvent(this));
    }

    /**
     * Returns the legend items for the plot.  Each legend item is generated by
     * the plot's renderer, since the renderer is responsible for the visual
     * representation of the data.
     *
     * @return The legend items.
     */
    public LegendItemCollection getLegendItems() {
        if (this.fixedLegendItems != null) {
            return this.fixedLegendItems;
        }
        LegendItemCollection result = new LegendItemCollection();
        int                  count  = this.datasets.size();
        for (int datasetIndex = 0; datasetIndex < count; datasetIndex++) {
            XYDataset dataset = getDataset(datasetIndex);
            if (dataset != null) {
                XYItemRenderer renderer = getRenderer(datasetIndex);
                if (renderer == null) {
                    renderer = getRenderer(0);
                }
                if (renderer != null) {
                    int seriesCount = dataset.getSeriesCount();
                    for (int i = 0; i < seriesCount; i++) {
                        if (renderer.isSeriesVisible(i)
                                && renderer.isSeriesVisibleInLegend(i)) {
                            LegendItem item =
                                renderer.getLegendItem(datasetIndex, i);
                            if (item != null) {
                                result.add(item);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Tests this plot for equality with another object.
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return <code>true</code> or <code>false</code>.
     */
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }
        if ( !(obj instanceof MyXYPlot)) {
            return false;
        }
        if ( !super.equals(obj)) {
            return false;
        }

        MyXYPlot that = (MyXYPlot) obj;
        if (this.weight != that.weight) {
            return false;
        }
        if (this.orientation != that.orientation) {
            return false;
        }
        if ( !this.domainAxes.equals(that.domainAxes)) {
            return false;
        }
        if ( !this.domainAxisLocations.equals(that.domainAxisLocations)) {
            return false;
        }
        if (this.rangeCrosshairLockedOnData
                != that.rangeCrosshairLockedOnData) {
            return false;
        }
        if (this.domainGridlinesVisible != that.domainGridlinesVisible) {
            return false;
        }
        if (this.rangeGridlinesVisible != that.rangeGridlinesVisible) {
            return false;
        }
        if (this.rangeZeroBaselineVisible != that.rangeZeroBaselineVisible) {
            return false;
        }
        if (this.domainCrosshairVisible != that.domainCrosshairVisible) {
            return false;
        }
        if (this.domainCrosshairValue != that.domainCrosshairValue) {
            return false;
        }
        if (this.domainCrosshairLockedOnData
                != that.domainCrosshairLockedOnData) {
            return false;
        }
        if (this.rangeCrosshairVisible != that.rangeCrosshairVisible) {
            return false;
        }
        if (this.rangeCrosshairValue != that.rangeCrosshairValue) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.axisOffset, that.axisOffset)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.renderers, that.renderers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.rangeAxes, that.rangeAxes)) {
            return false;
        }
        if ( !this.rangeAxisLocations.equals(that.rangeAxisLocations)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.datasetToDomainAxisMap,
                                    that.datasetToDomainAxisMap)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.datasetToRangeAxisMap,
                                    that.datasetToRangeAxisMap)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.domainGridlineStroke,
                                    that.domainGridlineStroke)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.domainGridlinePaint,
                                    that.domainGridlinePaint)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.rangeGridlineStroke,
                                    that.rangeGridlineStroke)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.rangeGridlinePaint,
                                    that.rangeGridlinePaint)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.rangeZeroBaselinePaint,
                                    that.rangeZeroBaselinePaint)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.rangeZeroBaselineStroke,
                                    that.rangeZeroBaselineStroke)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.domainCrosshairStroke,
                                    that.domainCrosshairStroke)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.domainCrosshairPaint,
                                    that.domainCrosshairPaint)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.rangeCrosshairStroke,
                                    that.rangeCrosshairStroke)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.rangeCrosshairPaint,
                                    that.rangeCrosshairPaint)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.foregroundDomainMarkers,
                                    that.foregroundDomainMarkers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.backgroundDomainMarkers,
                                    that.backgroundDomainMarkers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.foregroundRangeMarkers,
                                    that.foregroundRangeMarkers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.backgroundRangeMarkers,
                                    that.backgroundRangeMarkers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.foregroundDomainMarkers,
                                    that.foregroundDomainMarkers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.backgroundDomainMarkers,
                                    that.backgroundDomainMarkers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.foregroundRangeMarkers,
                                    that.foregroundRangeMarkers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.backgroundRangeMarkers,
                                    that.backgroundRangeMarkers)) {
            return false;
        }
        if ( !ObjectUtilities.equal(this.annotations, that.annotations)) {
            return false;
        }
        return true;

    }

    /**
     * Returns a clone of the plot.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException  this can occur if some component of
     *         the plot cannot be cloned.
     */
    public Object clone() throws CloneNotSupportedException {

        MyXYPlot clone = (MyXYPlot) super.clone();
        clone.domainAxes =
            (ObjectList) ObjectUtilities.clone(this.domainAxes);
        for (int i = 0; i < this.domainAxes.size(); i++) {
            ValueAxis axis = (ValueAxis) this.domainAxes.get(i);
            if (axis != null) {
                ValueAxis clonedAxis = (ValueAxis) axis.clone();
                clone.domainAxes.set(i, clonedAxis);
                clonedAxis.setPlot(clone);
                clonedAxis.addChangeListener(clone);
            }
        }
        clone.domainAxisLocations =
            (ObjectList) this.domainAxisLocations.clone();

        clone.rangeAxes = (ObjectList) ObjectUtilities.clone(this.rangeAxes);
        for (int i = 0; i < this.rangeAxes.size(); i++) {
            ValueAxis axis = (ValueAxis) this.rangeAxes.get(i);
            if (axis != null) {
                ValueAxis clonedAxis = (ValueAxis) axis.clone();
                clone.rangeAxes.set(i, clonedAxis);
                clonedAxis.setPlot(clone);
                clonedAxis.addChangeListener(clone);
            }
        }
        clone.rangeAxisLocations =
            (ObjectList) ObjectUtilities.clone(this.rangeAxisLocations);

        // the datasets are not cloned, but listeners need to be added...
        clone.datasets = (ObjectList) ObjectUtilities.clone(this.datasets);
        for (int i = 0; i < clone.datasets.size(); ++i) {
            XYDataset d = getDataset(i);
            if (d != null) {
                d.addChangeListener(clone);
            }
        }

        clone.datasetToDomainAxisMap = new TreeMap();
        clone.datasetToDomainAxisMap.putAll(this.datasetToDomainAxisMap);
        clone.datasetToRangeAxisMap = new TreeMap();
        clone.datasetToRangeAxisMap.putAll(this.datasetToRangeAxisMap);

        clone.renderers = (ObjectList) ObjectUtilities.clone(this.renderers);
        for (int i = 0; i < this.renderers.size(); i++) {
            XYItemRenderer renderer2 = (XYItemRenderer) this.renderers.get(i);
            if (renderer2 instanceof PublicCloneable) {
                PublicCloneable pc = (PublicCloneable) renderer2;
                clone.renderers.set(i, pc.clone());
            }
        }
        clone.foregroundDomainMarkers =
            (Map) ObjectUtilities.clone(this.foregroundDomainMarkers);
        clone.backgroundDomainMarkers =
            (Map) ObjectUtilities.clone(this.backgroundDomainMarkers);
        clone.foregroundRangeMarkers =
            (Map) ObjectUtilities.clone(this.foregroundRangeMarkers);
        clone.backgroundRangeMarkers =
            (Map) ObjectUtilities.clone(this.backgroundRangeMarkers);
        clone.annotations =
            (List) ObjectUtilities.deepClone(this.annotations);
        if (this.fixedDomainAxisSpace != null) {
            clone.fixedDomainAxisSpace =
                (AxisSpace) ObjectUtilities.clone(this.fixedDomainAxisSpace);
        }
        if (this.fixedRangeAxisSpace != null) {
            clone.fixedRangeAxisSpace =
                (AxisSpace) ObjectUtilities.clone(this.fixedRangeAxisSpace);
        }
        return clone;

    }

    /**
     * Provides serialization support.
     *
     * @param stream  the output stream.
     *
     * @throws IOException  if there is an I/O error.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        SerialUtilities.writeStroke(this.domainGridlineStroke, stream);
        SerialUtilities.writePaint(this.domainGridlinePaint, stream);
        SerialUtilities.writeStroke(this.rangeGridlineStroke, stream);
        SerialUtilities.writePaint(this.rangeGridlinePaint, stream);
        SerialUtilities.writeStroke(this.rangeZeroBaselineStroke, stream);
        SerialUtilities.writePaint(this.rangeZeroBaselinePaint, stream);
        SerialUtilities.writeStroke(this.domainCrosshairStroke, stream);
        SerialUtilities.writePaint(this.domainCrosshairPaint, stream);
        SerialUtilities.writeStroke(this.rangeCrosshairStroke, stream);
        SerialUtilities.writePaint(this.rangeCrosshairPaint, stream);
        SerialUtilities.writePaint(this.domainTickBandPaint, stream);
        SerialUtilities.writePaint(this.rangeTickBandPaint, stream);
        SerialUtilities.writePoint2D(this.quadrantOrigin, stream);
        for (int i = 0; i < 4; i++) {
            SerialUtilities.writePaint(this.quadrantPaint[i], stream);
        }
    }

    /**
     * Provides serialization support.
     *
     * @param stream  the input stream.
     *
     * @throws IOException  if there is an I/O error.
     * @throws ClassNotFoundException  if there is a classpath problem.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {

        stream.defaultReadObject();
        this.domainGridlineStroke    = SerialUtilities.readStroke(stream);
        this.domainGridlinePaint     = SerialUtilities.readPaint(stream);
        this.rangeGridlineStroke     = SerialUtilities.readStroke(stream);
        this.rangeGridlinePaint      = SerialUtilities.readPaint(stream);
        this.rangeZeroBaselineStroke = SerialUtilities.readStroke(stream);
        this.rangeZeroBaselinePaint  = SerialUtilities.readPaint(stream);
        this.domainCrosshairStroke   = SerialUtilities.readStroke(stream);
        this.domainCrosshairPaint    = SerialUtilities.readPaint(stream);
        this.rangeCrosshairStroke    = SerialUtilities.readStroke(stream);
        this.rangeCrosshairPaint     = SerialUtilities.readPaint(stream);
        this.domainTickBandPaint     = SerialUtilities.readPaint(stream);
        this.rangeTickBandPaint      = SerialUtilities.readPaint(stream);
        this.quadrantOrigin          = SerialUtilities.readPoint2D(stream);
        this.quadrantPaint           = new Paint[4];
        for (int i = 0; i < 4; i++) {
            this.quadrantPaint[i] = SerialUtilities.readPaint(stream);
        }

        // register the plot as a listener with its axes, datasets, and 
        // renderers...
        int domainAxisCount = this.domainAxes.size();
        for (int i = 0; i < domainAxisCount; i++) {
            Axis axis = (Axis) this.domainAxes.get(i);
            if (axis != null) {
                axis.setPlot(this);
                axis.addChangeListener(this);
            }
        }
        int rangeAxisCount = this.rangeAxes.size();
        for (int i = 0; i < rangeAxisCount; i++) {
            Axis axis = (Axis) this.rangeAxes.get(i);
            if (axis != null) {
                axis.setPlot(this);
                axis.addChangeListener(this);
            }
        }
        int datasetCount = this.datasets.size();
        for (int i = 0; i < datasetCount; i++) {
            Dataset dataset = (Dataset) this.datasets.get(i);
            if (dataset != null) {
                dataset.addChangeListener(this);
            }
        }
        int rendererCount = this.renderers.size();
        for (int i = 0; i < rendererCount; i++) {
            XYItemRenderer renderer = (XYItemRenderer) this.renderers.get(i);
            if (renderer != null) {
                renderer.addChangeListener(this);
            }
        }

    }

}

