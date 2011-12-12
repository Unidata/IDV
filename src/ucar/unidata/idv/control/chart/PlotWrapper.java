/*
 * $Id: PlotWrapper.java,v 1.15 2007/04/16 21:32:11 jeffmc Exp $
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
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.*;
import org.jfree.data.general.*;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;

import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;



import ucar.unidata.data.point.*;

import ucar.unidata.data.sounding.TrackDataSource;

import ucar.unidata.gis.SpatialGrid;



import ucar.unidata.idv.control.DisplayControlImpl;

import ucar.unidata.ui.TableSorter;
import ucar.unidata.ui.symbol.*;

import ucar.unidata.ui.symbol.StationModelManager;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.Util;



import visad.*;

import visad.georef.EarthLocation;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.rmi.RemoteException;

import java.text.SimpleDateFormat;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


//import com.lavantech.gui.comp.*;



/**
 * Abstract class for chart implementations
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.15 $
 */
public abstract class PlotWrapper extends ChartWrapper implements KeyListener {


    /** Used by the MyChartPanel event bridge */
    protected static final boolean EVENT_PASSON = false;

    /** Used by the MyChartPanel event bridge */
    protected static final boolean EVENT_DONTPASSON = true;

    /** The bg color */
    private Color backgroundColor = Color.lightGray;

    /** The color to use for the data area */
    private Color dataAreaColor = Color.white;

    /** line state */
    private LineState domainLineState = new LineState();

    /** line state */
    private LineState rangeLineState = new LineState();


    /** The main chart gui component */
    protected MyChartPanel chartPanel;

    /** The chart */
    protected JFreeChart chart;


    /** For properties dialog */
    private JComponent backgroundSwatch;

    /** For properties dialog */
    private JComponent dataAreaSwatch;

    /** properties widget */
    private JCheckBox autoRangeCbx;

    /** Do we do auto range */
    private boolean autoRange = true;

    /**
     * Default ctor
     */
    public PlotWrapper() {}



    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public PlotWrapper(String name, List dataChoices) {
        super(name, dataChoices);
    }



    /**
     * Noop
     *
     * @param e The event
     */
    public void keyReleased(KeyEvent e) {}


    /**
     * Noop
     *
     * @param e The event
     */
    public void keyTyped(KeyEvent e) {}



    /**
     * Is the annotation ok to draw
     *
     * @param annotation The annotation
     *
     * @return ok to draw
     */
    public boolean okToDraw(ChartAnnotation annotation) {
        return true;
    }



    /**
     * Can this chart do colors
     *
     * @return true
     */
    protected boolean canDoColors() {
        return true;
    }


    /**
     * The annotation changed somehow
     *
     * @param chartAnnotation The annotation
     */
    public void annotationChanged(ChartAnnotation chartAnnotation) {
        signalChartChanged();
    }




    /**
     * Get the list of tab names for the properties dialog
     *
     * @return List of tab names
     */
    public String[] getPropertyTabs() {
        return new String[] { "Data", "Display" };
    }


    /**
     * Create the properties contents
     *
     * @param comps  List of components
     * @param tabIdx Which tab
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);

        if (tabIdx != 1) {
            autoRangeCbx = new JCheckBox("", autoRange);
            comps.add(GuiUtils.rLabel("Auto Range: "));
            comps.add(autoRangeCbx);
            return;
        }
        if (canDoColors()) {
            backgroundSwatch =
                GuiUtils.makeColorSwatchWidget(backgroundColor, "")[0];
            dataAreaSwatch = GuiUtils.makeColorSwatchWidget(dataAreaColor,
                    "")[0];
            comps.add(GuiUtils.rLabel("Color: "));
            List colorComps = Misc.newList(new JLabel("Background:"),
                                           backgroundSwatch,
                                           new JLabel("Data Area:"),
                                           dataAreaSwatch);
            comps.add(GuiUtils.left(GuiUtils.hbox(colorComps, 4)));
            comps.add(GuiUtils.rLabel("Domain Lines: "));
            comps.add(domainLineState.getPropertyContents());
            comps.add(GuiUtils.rLabel("Range Lines: "));
            comps.add(rangeLineState.getPropertyContents());

        }
    }



    /**
     * Apply properties
     *
     *
     * @return Was successful
     */
    protected boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        if ( !domainLineState.applyProperties()) {
            return false;
        }
        if ( !rangeLineState.applyProperties()) {
            return false;
        }

        if (autoRangeCbx.isSelected() != autoRange) {
            autoRange = autoRangeCbx.isSelected();
            resetPlot();
        }

        backgroundColor = backgroundSwatch.getBackground();
        dataAreaColor   = dataAreaSwatch.getBackground();
        Plot plot = chart.getPlot();
        plot.setBackgroundPaint(dataAreaColor);
        //      getContents().setBackground(backgroundColor);

        if (plot instanceof XYPlot) {
            ((XYPlot) plot).setDomainGridlinesVisible(
                domainLineState.getVisible());
            ((XYPlot) plot).setRangeGridlinesVisible(
                rangeLineState.getVisible());
            ((XYPlot) plot).setDomainGridlinePaint(
                domainLineState.getColor());
            ((XYPlot) plot).setRangeGridlinePaint(rangeLineState.getColor());
            ((XYPlot) plot).setDomainGridlineStroke(
                domainLineState.getStroke());
            ((XYPlot) plot).setRangeGridlineStroke(
                rangeLineState.getStroke());
        }
        return true;
    }


    /**
     * Get the menu items
     *
     * @param items List of menu items
     *
     * @return The items list
     */
    protected List getPopupMenuItems(List items) {
        super.getPopupMenuItems(items);
        ((MyChartPanel) chartPanel).addChartMenuItems(items);
        return items;
    }



    /**
     * Class MyChartPanel adds items to a  popup menu
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.15 $
     */
    protected class MyChartPanel extends ChartPanel {

        /** Last x */
        public int lastEventX = 0;

        /** Last y */
        public int lastEventY = 0;

        /**
         * ctor
         *
         * @param chart  the chart
         */
        public MyChartPanel(JFreeChart chart) {
            super(chart, true);
        }


        /**
         * Hook to pass through to the chartwrapper
         *
         * @param event The event
         *
         * @return The tooltip text
         */
        public String getToolTipText(MouseEvent event) {
            String fromChart = chartPanelGetToolTipText(event);
            if (fromChart == null) {
                return super.getToolTipText();
            }
            return fromChart;
        }

        /**
         * Show the popup menu. Add items if not done so.
         *
         * @param x x
         * @param y y
         */
        protected void displayPopupMenu(int x, int y) {
            showPopup(chartPanel, x, y);
        }



        /**
         * utility
         *
         * @param result The menu
         * @param label The label
         * @param cmd The command
         *
         * @return The item
         */
        protected JMenuItem addChartMenuItem(JMenu result, String label,
                                             String cmd) {
            JMenuItem mi = new JMenuItem(label);
            mi.setActionCommand(cmd);
            mi.addActionListener(this);
            result.add(mi);
            return mi;
        }

        /**
         * add menu items
         *
         *
         * @param items List of menu items
         */
        protected void addChartMenuItems(List items) {
            items.add(GuiUtils.MENU_SEPARATOR);
            JMenu chartMenu = new JMenu("Zoom");
            items.add(chartMenu);

            JMenu[] menus = { new JMenu("Zoom In"), new JMenu("Zoom Out"),
                              new JMenu("Auto Range") };
            String[][] cmds = {
                {
                    "Both Axes", ZOOM_IN_BOTH_COMMAND, "Domain Axis",
                    ZOOM_IN_DOMAIN_COMMAND, "Range Axis",
                    ZOOM_IN_RANGE_COMMAND
                }, {
                    "Both Axes", ZOOM_OUT_BOTH_COMMAND, "Domain Axis",
                    ZOOM_OUT_DOMAIN_COMMAND, "Range Axis",
                    ZOOM_OUT_RANGE_COMMAND
                }, {
                    "Both Axes", ZOOM_RESET_BOTH_COMMAND, "Domain Axis",
                    ZOOM_RESET_DOMAIN_COMMAND, "Range Axis",
                    ZOOM_RESET_RANGE_COMMAND
                }
            };
            for (int i = 0; i < menus.length; i++) {
                chartMenu.add(menus[i]);
                for (int j = 0; j < cmds[i].length; j += 2) {
                    addChartMenuItem(menus[i], cmds[i][j], cmds[i][j + 1]);
                }
            }
        }



        /**
         * Handle the event. Check with the ChartWrapper
         *
         * @param event The event
         */
        public void mouseDragged(MouseEvent event) {
            lastEventX = event.getX();
            lastEventY = event.getY();
            if (chartPanelMouseDragged(event) == EVENT_DONTPASSON) {
                return;
            }
            super.mouseDragged(event);
        }

        /**
         * Handle the event. Check with the ChartWrapper
         *
         * @param event The event
         */
        public void mouseReleased(MouseEvent event) {
            lastEventX = event.getX();
            lastEventY = event.getY();
            if (chartPanelMouseReleased(event) == EVENT_DONTPASSON) {
                return;
            }
            super.mouseReleased(event);
        }

        /**
         * Handle the event. Check with the ChartWrapper
         *
         * @param event The event
         */
        public void mouseClicked(MouseEvent event) {
            lastEventX = event.getX();
            lastEventY = event.getY();
            if (chartPanelMouseClicked(event) == EVENT_DONTPASSON) {
                return;
            }
            super.mouseClicked(event);
        }

        /**
         * Handle the event. Check with the ChartWrapper
         *
         * @param event The event
         */
        public void mousePressed(MouseEvent event) {
            lastEventX = event.getX();
            lastEventY = event.getY();
            requestFocus();
            if (chartPanelMousePressed(event) == EVENT_DONTPASSON) {
                return;
            }
            super.mousePressed(event);
        }

        /**
         * Handle the event. Check with the ChartWrapper
         *
         * @param event the action
         */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (command.equals(PROPERTIES_COMMAND)) {
                PlotWrapper.this.showProperties();
                return;
            }
            super.actionPerformed(event);
        }
    }



    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        super.setName(value);
        if (chart != null) {
            chart.setTitle(value);
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
        return null;
    }


    /**
     * Hook to intercept these calls on the chart
     *
     * @param e The event
     *
     * @return  Was this event handled by the ChartWrapper
     */
    public boolean chartPanelMousePressed(MouseEvent e) {
        return EVENT_PASSON;
    }

    /**
     * Hook to intercept these calls on the chart
     *
     * @param e The event
     *
     * @return  Was this event handled by the ChartWrapper
     */
    public boolean chartPanelMouseClicked(MouseEvent e) {
        return EVENT_PASSON;
    }

    /**
     * Hook to intercept these calls on the chart
     *
     * @param event The event
     *
     * @return  Was this event handled by the ChartWrapper
     */
    public boolean chartPanelMouseDragged(MouseEvent event) {
        if (SwingUtilities.isRightMouseButton(event)) {
            return EVENT_DONTPASSON;
        }
        return EVENT_PASSON;

    }

    /**
     * Hook to intercept these calls on the chart
     *
     *
     * @param event The event
     *
     * @return  Was this event handled by the ChartWrapper
     */
    public boolean chartPanelMouseReleased(MouseEvent event) {
        return EVENT_PASSON;
    }


    /**
     * Get the chart
     *
     * @return The chart_
     */
    protected ChartPanel getChartPanel() {
        return chartPanel;
    }


    /**
     * update chart
     */
    public void signalChartChanged() {
        if (chartPanel != null) {
            chartPanel.chartChanged(new ChartChangeEvent(this));
        }
    }


    /**
     * Make the panel for the chart
     *
     * @param chart The chart
     *
     * @return The panel
     */
    protected MyChartPanel doMakeChartPanel(JFreeChart chart) {
        if (chartPanel == null) {
            chartPanel = new MyChartPanel(chart);
            //      chartPanel.setBackground(backgroundColor);
            chartPanel.addKeyListener(this);
            chartPanel.setPreferredSize(getPreferredChartSize());
            chartPanel.setMouseZoomable(true, false);
        }
        return chartPanel;
    }




    /**
     * Pan plot
     *
     * @param right to right
     */
    protected void panPlot(boolean right) {
        panPlot(right, 0.1);
    }

    /**
     * Pan the plot
     *
     * @param right to right
     * @param percent by how much
     */
    protected void panPlot(boolean right, double percent) {
        if ( !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();
        int    cnt  = plot.getDomainAxisCount();
        for (int i = 0; i < cnt; i++) {
            ValueAxis            axis   = (ValueAxis) plot.getDomainAxis(i);
            org.jfree.data.Range range  = axis.getRange();
            double width = range.getUpperBound() - range.getLowerBound();
            double               width2 = width / 2.0;
            double               step   = (right
                                           ? width * percent
                                           : -width * percent);
            axis.centerRange(range.getLowerBound() + step + width2);
        }
    }

    /**
     * Move plot up/down.
     *
     * @param up up
     */
    private void upDownPlot(boolean up) {
        if ( !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();

        int    cnt  = plot.getRangeAxisCount();
        for (int i = 0; i < cnt; i++) {
            ValueAxis            axis   = (ValueAxis) plot.getRangeAxis(i);
            org.jfree.data.Range range  = axis.getRange();
            double width = range.getUpperBound() - range.getLowerBound();
            double               width2 = width / 2.0;
            double               step   = (up
                                           ? width * 0.1
                                           : -width * 0.1);
            axis.centerRange(range.getLowerBound() + step + width2);
        }
    }


    /**
     * reset the axis'
     */
    private void resetPlot() {
        if (chart == null) {
            return;
        }
        if ( !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();
        int    rcnt = plot.getRangeAxisCount();
        for (int i = 0; i < rcnt; i++) {
            ValueAxis axis = (ValueAxis) plot.getRangeAxis(i);
            System.err.println("range axis:" + axis);
            axis.setAutoRange(autoRange);
        }
        int dcnt = plot.getDomainAxisCount();
        for (int i = 0; i < dcnt; i++) {
            ValueAxis axis = (ValueAxis) plot.getDomainAxis(i);
            System.err.println("domain axis:" + axis);
            axis.setAutoRange(autoRange);
        }
    }


    /**
     * Handle event
     *
     * @param e The event
     */
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            panPlot(true);
        } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            panPlot(false);
        } else if (e.getKeyCode() == KeyEvent.VK_UP) {
            upDownPlot(true);
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            upDownPlot(false);
        } else if ((e.getKeyCode() == KeyEvent.VK_R) && e.isControlDown()) {
            resetPlot();
        } else {
            super.keyPressed(e);
        }
    }



    /**
     * Utility to init xy plots
     *
     * @param plot the plotx
     */
    protected void initXYPlot(XYPlot plot) {
        plot.setBackgroundPaint(dataAreaColor);
        //        plot.setAxisOffset(new RectangleInsets(6.0, 3.0, 3.0, 3.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

        plot.setDomainGridlinesVisible(domainLineState.getVisible());
        plot.setRangeGridlinesVisible(rangeLineState.getVisible());
        plot.setDomainGridlinePaint(domainLineState.getColor());
        plot.setRangeGridlinePaint(rangeLineState.getColor());
        plot.setDomainGridlineStroke(domainLineState.getStroke());
        plot.setRangeGridlineStroke(rangeLineState.getStroke());
    }


    /**
     * Get the chart size
     *
     * @return size
     */
    protected Dimension getPreferredChartSize() {
        return new java.awt.Dimension(500, 270);
    }



    /**
     * Set the BackgroundColor property.
     *
     * @param value The new value for BackgroundColor
     */
    public void setBackgroundColor(Color value) {
        backgroundColor = value;
    }

    /**
     * Get the BackgroundColor property.
     *
     * @return The BackgroundColor
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Set the DataAreaColor property.
     *
     * @param value The new value for DataAreaColor
     */
    public void setDataAreaColor(Color value) {
        dataAreaColor = value;
    }

    /**
     * Get the DataAreaColor property.
     *
     * @return The DataAreaColor
     */
    public Color getDataAreaColor() {
        return dataAreaColor;
    }




    /**
     * Set the DomainLineState property.
     *
     * @param value The new value for DomainLineState
     */
    public void setDomainLineState(LineState value) {
        domainLineState = value;
    }

    /**
     * Get the DomainLineState property.
     *
     * @return The DomainLineState
     */
    public LineState getDomainLineState() {
        return domainLineState;
    }

    /**
     * Set the RangeLineState property.
     *
     * @param value The new value for RangeLineState
     */
    public void setRangeLineState(LineState value) {
        rangeLineState = value;
    }

    /**
     * Get the RangeLineState property.
     *
     * @return The RangeLineState
     */
    public LineState getRangeLineState() {
        return rangeLineState;
    }


    /**
     *  Set the AutoRange property.
     *
     *  @param value The new value for AutoRange
     */
    public void setAutoRange(boolean value) {
        autoRange = value;
    }

    /**
     *  Get the AutoRange property.
     *
     *  @return The AutoRange
     */
    public boolean getAutoRange() {
        return autoRange;
    }



}

