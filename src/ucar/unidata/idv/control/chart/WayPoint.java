/*
 * $Id: WayPoint.java,v 1.22 2007/05/04 13:50:26 dmurray Exp $
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
import ucar.unidata.util.MidiManager;
import ucar.unidata.util.MidiProperties;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.TwoFacedObject;

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

import javax.sound.midi.*;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 * Class WayPoint is used in the time series chart to show a way point
 * along the time domain
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.22 $
 */
public class WayPoint extends ChartAnnotation {

    /** Plays sounds */
    private MidiManager midiManager;


    /** Draws the clock */
    private static Image clockImage;

    /** Draws the clock */
    private static Image noteImage;

    /** Used for property changed when waypoint is moved */
    public static final String PROP_WAYPOINTVALUE = "prop.waypointvalue";

    /** Arrow width */
    public static final int ANNOTATION_WIDTH = 10;

    /** The domain value */
    private double domainValue = 0.0;


    /** Is this the special way point used to show time animation */
    private boolean isForAnimation = false;

    /** Span */
    private double minutesSpan = 0.0;

    /** For properties */
    private JTextField spanFld;

    /** MidiProperties */
    private MidiProperties midiProperties = new MidiProperties();

    /**
     * Default ctor
     */
    public WayPoint() {}


    /**
     * Ctor
     *
     * @param domainValue The domain value
     * @param timeseries The chart I'm in
     */
    public WayPoint(double domainValue, TimeSeriesChartWrapper timeseries) {
        super(timeseries);
        this.domainValue = domainValue;
    }



    /**
     * Create, if needed, and return the midimanager that plays sounds
     *
     * @return The midi manager
     */
    public MidiManager getMidiManager() {
        if (midiManager == null) {
            midiManager = new MidiManager(midiProperties);
        }
        return midiManager;
    }


    /**
     * Get the tool tip text
     *
     * @return Tool tip text
     */
    public String getToolTipText() {
        return "<html>Way point: <b>" + new Date((long) domainValue)
               + "</b></html>";
    }


    /**
     * Type of annotation
     *
     * @return Type of annotation
     */
    public String getTypeName() {
        return "Waypoint";
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
        double domainValue = tscw.getDomainValue(x);
        setDomainValue(domainValue);
        this.x = x;
        this.y = y;
    }


    /**
     * What color should we use. If selected use the selected color
     *
     * @return The color
     */
    public Color getColorToUse() {
        if (getSelected()) {
            return COLOR_SELECTED;
        } else {
            return getColor();
        }
    }

    /**
     * Should the color swatch be shown in the properties dialog
     *
     * @return Show color in properties
     */
    protected boolean showColorInProperties() {
        return true;
    }



    /**
     * return the array of tab names for the proeprties dialog
     *
     * @return array of tab names
     */
    public String[] getPropertyTabs() {
        return new String[] { "Display", "Sound" };
    }




    /**
     * Add to properties gui
     *
     *
     * @param comps List of comps
     * @param tabIdx Which tab in the properties dialog
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx == 0) {
            comps.add(GuiUtils.rLabel("Span: "));
            comps.add(GuiUtils.left(GuiUtils.hbox(spanFld = new JTextField(""
                    + minutesSpan, 5), new JLabel(" (minutes)"))));
        } else {
            midiProperties.getPropertiesComponents(comps);
        }
    }


    /**
     * Can the waypoint play sound
     *
     * @return Can this waypoint play sound
     */
    public boolean canPlaySound() {
        return !getMidiProperties().getMuted();
    }


    /**
     * Apply properties
     *
     * @return success
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        minutesSpan = Misc.parseNumber(spanFld.getText().trim());
        if ( !midiProperties.applyProperties()) {
            return false;
        }
        if ( !midiProperties.getMuted()) {
            getMidiManager().setInstrument(
                midiProperties.getInstrumentName());
            getMidiManager().play(64, 500);
        }

        return true;
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
        if (false && getSelected()) {
            g2.setColor(COLOR_SELECTED);
        } else {
            g2.setColor(getColor());
        }
        x = getXFromValue(dataArea, domainAxis);


        int width2 = (int) (ANNOTATION_WIDTH / 2);
        int bottom = (int) (dataArea.getY() + dataArea.getHeight());
        y = bottom;
        int[] xs = { x - width2, x + width2, x, x - width2 };
        int[] ys = { bottom - ANNOTATION_WIDTH, bottom - ANNOTATION_WIDTH,
                     bottom, bottom - ANNOTATION_WIDTH };
        g2.fillPolygon(xs, ys, xs.length);


        if ((getName() != null) && !isForAnimation) {
            FontMetrics fm       = g2.getFontMetrics();
            int         width    = fm.stringWidth(getName());
            int         textLeft = x - width / 2;
            g2.drawString(getName(), textLeft, bottom - ANNOTATION_WIDTH - 2);
        }

        if (getSelected()) {
            g2.setColor(COLOR_SELECTED);
            g2.drawPolygon(xs, ys, xs.length);
        }

        if (getPropertyListeners().hasListeners(PROP_WAYPOINTVALUE)
                || isForAnimation) {
            g2.setColor(Color.gray);
            g2.drawLine(x, y - ANNOTATION_WIDTH, x, (int) dataArea.getY());
        }

        boolean playSound = canPlaySound();

        if (isForAnimation) {
            if (clockImage == null) {
                clockImage = GuiUtils.getImage("/auxdata/ui/icons/clock.gif");
            }
            if (playSound) {
                g2.drawImage(clockImage, x - 8, (int) dataArea.getY() + 1,
                             null);
            } else {
                g2.drawImage(clockImage, x - 8, (int) dataArea.getY() + 1,
                             null);
            }
        }


        if (canPlaySound()) {
            if (noteImage == null) {
                noteImage = GuiUtils.getImage("/auxdata/ui/icons/note.gif");
            }
            if (isForAnimation) {
                g2.drawImage(noteImage, x + 8, (int) dataArea.getY() + 1,
                             null);
            } else {
                g2.drawImage(noteImage, x, (int) dataArea.getY() + 1, null);
            }
        }

        if (minutesSpan > 0.0) {
            int left = (int) domainAxis.valueToJava2D(domainValue
                           - (minutesSpan * 60000) / 2, dataArea,
                               RectangleEdge.BOTTOM);
            int right = (int) domainAxis.valueToJava2D(domainValue
                            + (minutesSpan * 60000) / 2, dataArea,
                                RectangleEdge.BOTTOM);
            g2.setPaint(Color.black);
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawLine(left, y, right, y);
        }


    }


    /**
     * Get the x position from the domain value
     *
     * @param dataArea data area
     * @param domainAxis domain axis
     *
     * @return The x value
     */
    protected int getXFromValue(Rectangle2D dataArea, ValueAxis domainAxis) {
        return (int) domainAxis.valueToJava2D(domainValue, dataArea,
                RectangleEdge.BOTTOM);

    }



    /**
     *  Set the DomainValue property.
     *
     *  @param value The new value for DomainValue
     */
    public void setDomainValue(double value) {
        boolean different = (value != domainValue);
        domainValue = value;
        if (different) {
            firePropertyChange(PROP_WAYPOINTVALUE, null, new Double(value));
        }
    }




    /**
     *  Get the DomainValue property.
     *
     *  @return The DomainValue
     */
    public double getDomainValue() {
        return domainValue;
    }


    /**
     * Set the IsForAnimation property.
     *
     * @param value The new value for IsForAnimation
     */
    public void setIsForAnimation(boolean value) {
        isForAnimation = value;
    }

    /**
     * Get the IsForAnimation property.
     *
     * @return The IsForAnimation
     */
    public boolean getIsForAnimation() {
        return isForAnimation;
    }


    /**
     * Set the MinutesSpan property.
     *
     * @param value The new value for MinutesSpan
     */
    public void setMinutesSpan(double value) {
        minutesSpan = value;
    }

    /**
     * Get the MinutesSpan property.
     *
     * @return The MinutesSpan
     */
    public double getMinutesSpan() {
        return minutesSpan;
    }

    /**
     * Set the MidiProperties property.
     *
     * @param value The new value for MidiProperties
     */
    public void setMidiProperties(MidiProperties value) {
        midiProperties = value;
    }

    /**
     * Get the MidiProperties property.
     *
     * @return The MidiProperties
     */
    public MidiProperties getMidiProperties() {
        return midiProperties;
    }

}

