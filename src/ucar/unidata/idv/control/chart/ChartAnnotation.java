/*
 * $Id: ChartAnnotation.java,v 1.20 2007/04/16 21:32:10 jeffmc Exp $
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

import ucar.unidata.collab.PropertiedThing;



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
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import java.beans.*;


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
 * Class ChartAnnotation is an abstract class for the annotations
 * on charts
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.20 $
 */
public abstract class ChartAnnotation extends PropertiedThing implements XYAnnotation {

    /** Selected color */
    public static final Color COLOR_SELECTED = Color.red;

    /** Is this anno selected */
    private boolean selected = false;

    /** The color */
    private Color color = Color.blue;

    /** The chart we're in */
    private PlotWrapper plotWrapper;

    /** The name */
    private String name = "";



    /** x location. Though not all annotations have a single x/y */
    protected int x;

    /** y location. Though not all annotations have a single x/y */
    protected int y;

    /** used in property dialog */
    private JTextField nameFld;

    /** used in property dialog */
    JComponent colorSwatch;

    /** The transform from the last draw */
    protected AffineTransform transform;


    /** Is active */
    private boolean active = true;



    /**
     * Default ctor
     */
    public ChartAnnotation() {
        active = true;
    }


    /**
     * Ctro
     *
     * @param plotWrapper The chart we are in
     */
    public ChartAnnotation(PlotWrapper plotWrapper) {
        this.plotWrapper = plotWrapper;
    }


    /**
     * tuility
     *
     * @param v value
     *
     * @return formatted value
     */
    public String formatValue(double v) {
        return getPlotWrapper().formatValue(v);
    }




    /**
     * Hook for tooltip
     *
     * @return Tooltip text for this annotation
     */
    public String getToolTipText() {
        return null;
    }

    /**
     * Called by base classes when drawn. We set the transform here
     *
     * @param g2 The graphics
     */
    protected void setGraphicsState(Graphics2D g2) {
        transform = g2.getTransform();
    }



    /**
     * Distance to the given point. This transforms our x/y
     * to the display space.
     *
     * @param x Mouse x
     * @param y Mouse y
     *
     * @return Distance to x/y
     */
    public double distance(int x, int y) {
        if (transform != null) {
            Point p = (Point) transform.transform(new Point(getX(), getY()),
                          new Point());
            return distance(p.x, p.y, x, y);
        }
        return distance(getX(), getY(), x, y);
    }


    /**
     * utility method to calculate distance
     *
     * @param x1 x1
     * @param y1 y1
     * @param x2 x2
     * @param y2 y2
     *
     * @return distance
     */
    public static double distance(int x1, int y1, int x2, int y2) {
        int dy = y1 - y2;
        int dx = x1 - x2;
        return Math.sqrt(dx * dx + dy * dy);
    }


    /**
     * Type of annotation
     *
     * @return Type of annotation
     */
    public abstract String getTypeName();




    /**
     * Apply the properties
     *
     * @return success
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        setName(nameFld.getText());
        if (colorSwatch != null) {
            setColor(colorSwatch.getBackground());
        }
        return true;
    }

    /**
     * Collect the components that go into the properties dialog
     *
     *
     * @param comps List of left/right components
     * @param tabIdx Which tab
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx != 0) {
            return;
        }
        comps.add(GuiUtils.rLabel("Name: "));
        comps.add(nameFld = new JTextField(getName()));
        if (showColorInProperties()) {
            JComponent[] colorSwatchComps =
                GuiUtils.makeColorSwatchWidget(color, "Chart Color: ");
            colorSwatch = colorSwatchComps[0];
            comps.add(GuiUtils.rLabel("Color: "));
            comps.add(GuiUtils.left(GuiUtils.hbox(GuiUtils.inset(colorSwatch,
                    4), colorSwatchComps[1])));

        }
    }



    /**
     * Set the position to the given x/y screen coordinate
     *
     * @param x x
     * @param y y
     */
    public void setPosition(int x, int y) {}

    /**
     * Set the position to the x/y of the event
     *
     * @param event The event
     */
    public void setPosition(MouseEvent event) {
        setPosition(event.getX(), event.getY());
    }

    /**
     * Annotation was removed
     */
    public void doRemove() {
        active = false;
    }



    /**
     *  Set the Selected property.
     *
     *  @param value The new value for Selected
     */
    public void setSelected(boolean value) {
        selected = value;
    }

    /**
     *  Get the Selected property.
     *
     *  @return The Selected
     */
    public boolean getSelected() {
        return selected;
    }



    /**
     * Should the color widget be shown in the properties dialog
     * @return Show color widget
     */
    protected boolean showColorInProperties() {
        return false;
    }


    /**
     *  Set the Color property.
     *
     *  @param value The new value for Color
     */
    public void setColor(Color value) {
        color = value;
    }

    /**
     *  Get the Color property.
     *
     *  @return The Color
     */
    public Color getColor() {
        return color;
    }


    /**
     * Set the PlotWrapper property.
     *
     * @param value The new value for PlotWrapper
     */
    public void setPlotWrapper(PlotWrapper value) {
        plotWrapper = value;
    }

    /**
     * Get the PlotWrapper property.
     *
     * @return The PlotWrapper
     */
    public PlotWrapper getPlotWrapper() {
        return plotWrapper;
    }


    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     * Get the y position
     *
     * @return y
     */
    public int getY() {
        return y;
    }

    /**
     * Get the x position
     *
     * @return x
     */
    public int getX() {
        return x;
    }

    /**
     * Set the Active property.
     *
     * @param value The new value for Active
     */
    public void setActive(boolean value) {
        active = value;
    }

    /**
     * Get the Active property.
     *
     * @return The Active
     */
    public boolean isActive() {
        return active;
    }


    /**
     * tostring
     *
     * @return tostring
     */
    public String toString() {
        return name;
    }


    /**
     * new property change listener
     *
     * @param listener listener
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        super.addPropertyChangeListener(listener);
        plotWrapper.annotationChanged(this);
    }


    /**
     * property change listener
     *
     * @param listener  listener
     */
    public void removePropertyChangeListener(
            PropertyChangeListener listener) {
        super.removePropertyChangeListener(listener);
        plotWrapper.annotationChanged(this);
    }



}

