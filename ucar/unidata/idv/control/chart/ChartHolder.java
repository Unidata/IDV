/**
 * $Id: ChartHolder.java,v 1.9 2007/04/16 21:32:10 jeffmc Exp $
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
import ucar.unidata.util.FileManager;
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
 * Class ChartHolder hodls a chart
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.9 $
 */
public class ChartHolder {


    /** my manager */
    private ChartManager chartManager;

    /** last axis side */
    protected AxisLocation lastSide;


    /** how many params */
    private int paramCount = 0;

    /** am I being shown */
    private boolean beingShown = false;

    /** show thumbnail image */
    private boolean showThumbnail = true;

    /** my name */
    private String name = "Plot";

    /** The chart */
    private JFreeChart chart;

    /** my plot */
    private Plot plot;

    /** chart gui */
    private ChartPanel chartPanel;

    /** show title */
    private boolean showTitle = true;

    /** for the grid layout in the manager */
    private Rectangle layoutRect;

    /** line state */
    private LineState domainLineState = new LineState();

    /** line state */
    private LineState rangeLineState = new LineState();

    /** The bg color */
    private Color backgroundColor = null;

    /** The color to use for the data area */
    private Color dataAreaColor = Color.white;


    /** for gui */
    protected JCheckBox showThumbnailCbx;

    /** for gui */
    protected JCheckBox showTitleCbx;

    /** For properties dialog */
    protected JComponent backgroundSwatch;

    /** For properties dialog */
    protected JComponent dataAreaSwatch;

    /** for gui */
    protected JTextField nameFld;

    /** my size */
    private Dimension preferredSize;

    /**
     * ctor
     */
    public ChartHolder() {}


    /**
     * ctor
     *
     *
     * @param chartManager The chart manager
     * @param name name
     * @param preferredSize size
     */
    public ChartHolder(ChartManager chartManager, String name,
                       Dimension preferredSize) {
        this.chartManager  = chartManager;
        this.name          = name;
        this.preferredSize = preferredSize;
        init();
        resetChartPanel();
        applyPlotProperties();
    }

    /**
     * apply properties
     *
     * @return OK
     */
    protected boolean applyProperties() {
        if (backgroundSwatch != null) {
            backgroundColor = backgroundSwatch.getBackground();
        }
        if (dataAreaSwatch != null) {
            dataAreaColor = dataAreaSwatch.getBackground();
        }
        if (showThumbnailCbx != null) {
            setShowThumbnail(showThumbnailCbx.isSelected());
        }
        if (showTitleCbx != null) {
            setShowTitle(showTitleCbx.isSelected());
        }
        return true;
    }


    /**
     * get properties widgets
     *
     * @param comps widgets
     */
    protected void getPropertiesComponents(List comps) {
        //        showThumbnailCbx = new JCheckBox("Thumbnail", showThumbnail);
        showTitleCbx = new JCheckBox("Title", showTitle);
        comps.add(GuiUtils.rLabel("Show:"));
        //        comps.add(GuiUtils.left(GuiUtils.hbox(showThumbnailCbx,showTitleCbx)));
        comps.add(GuiUtils.left(showTitleCbx));
    }


    /**
     * apply props
     */
    protected void applyPlotProperties() {
        if (plot == null) {
            return;
        }
        if (dataAreaColor != null) {
            plot.setBackgroundPaint(dataAreaColor);
        }
        if (backgroundColor != null) {
            chart.setBackgroundPaint(backgroundColor);
        }
        if ((backgroundColor != null) && (chartPanel != null)) {
            chartPanel.setBackground(backgroundColor);
        }
        if (showTitle) {
            chart.setTitle(getName());
        } else {
            chart.setTitle((String) null);
        }
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
    }


    /**
     * init
     */
    protected void init() {
        if (plot == null) {
            plot = chartManager.doMakePlot();
            chart = new JFreeChart("", JFreeChart.DEFAULT_TITLE_FONT, plot,
                                   true);
            if (backgroundColor != null) {
                chart.setBackgroundPaint(backgroundColor);
            }

            applyPlotProperties();
        }
    }

    /**
     * Set the BeingShown property.
     *
     * @param value The new value for BeingShown
     */
    protected void setBeingShown(boolean value) {
        beingShown = value;
    }

    /**
     * Get the BeingShown property.
     *
     * @return The BeingShown
     */
    protected boolean getBeingShown() {
        return beingShown;
    }





    /**
     * get panel
     *
     * @return panel
     */
    protected ChartPanel getChartPanel() {
        if (plot == null) {
            init();
        }
        return chartPanel;
    }


    /**
     * get plot
     *
     * @return plot
     */
    public Plot getPlot() {
        if (plot == null) {
            init();
        }
        return plot;
    }


    /**
     * write image
     */
    public void doSaveAs() {
        String filename =
            FileManager.getWriteFile(FileManager.FILTER_IMAGEWRITE,
                                     FileManager.SUFFIX_JPG);
        if (filename == null) {
            return;
        }
        try {
            ImageUtils.writeImageToFile(chartPanel, filename);
        } catch (Exception exc) {
            LogUtil.logException("Capturing image", exc);
        }
    }

    /**
     * show dialog
     */
    public void showPropertiesDialog() {
        chartManager.showPropertiesDialog(this);

    }

    /**
     * get items
     *
     * @param items items
     */
    protected void getMenuItems(List items) {
        chartManager.getPopupMenuItems(this, items);
        if (items.size() > 0) {
            items.add(GuiUtils.MENU_SEPARATOR);
        }
        addZoomMenuItems(items);
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Save Chart As...", this,
                                        "doSaveAs"));
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Properties...", this,
                                        "showPropertiesDialog"));
    }


    /**
     * show popup
     *
     * @param event event
     */
    private void showPopupMenu(MouseEvent event) {
        List items = new ArrayList();
        getMenuItems(items);
        JPopupMenu popupMenu = GuiUtils.makePopupMenu(items);
        popupMenu.show(chartPanel, event.getX(), event.getY());
    }

    /**
     * Add in the zoom menu items
     *
     * @param items items
     */
    private void addZoomMenuItems(List items) {
        JMenuItem mi;
        JMenu     menu;

        items.add(menu = new JMenu("Zoom In"));
        menu.add(mi = new JMenuItem("All Axes"));
        mi.setActionCommand(ChartPanel.ZOOM_IN_BOTH_COMMAND);
        mi.addActionListener(chartPanel);
        menu.addSeparator();

        menu.add(mi = new JMenuItem("Domain Axis"));
        mi.setActionCommand(ChartPanel.ZOOM_IN_DOMAIN_COMMAND);
        mi.addActionListener(chartPanel);

        menu.add(mi = new JMenuItem("Range Axis"));
        mi.setActionCommand(ChartPanel.ZOOM_IN_RANGE_COMMAND);
        mi.addActionListener(chartPanel);

        items.add(menu = new JMenu("Zoom Out"));

        menu.add(mi = new JMenuItem("All Axes"));
        mi.setActionCommand(ChartPanel.ZOOM_OUT_BOTH_COMMAND);
        mi.addActionListener(chartPanel);

        menu.addSeparator();

        menu.add(mi = new JMenuItem("Domain Axis"));
        mi.setActionCommand(ChartPanel.ZOOM_OUT_DOMAIN_COMMAND);
        mi.addActionListener(chartPanel);

        menu.add(mi = new JMenuItem("Range Axis"));
        mi.setActionCommand(ChartPanel.ZOOM_OUT_RANGE_COMMAND);
        mi.addActionListener(chartPanel);

        items.add(menu = new JMenu("Auto Range"));

        menu.add(mi = new JMenuItem("All Axes"));
        mi.setActionCommand(ChartPanel.ZOOM_RESET_BOTH_COMMAND);
        mi.addActionListener(chartPanel);

        menu.addSeparator();

        menu.add(mi = new JMenuItem("Domain Axis"));
        mi.setActionCommand(ChartPanel.ZOOM_RESET_DOMAIN_COMMAND);
        mi.addActionListener(chartPanel);

        menu.add(mi = new JMenuItem("Range Axis"));
        mi.setActionCommand(ChartPanel.ZOOM_RESET_RANGE_COMMAND);
        mi.addActionListener(chartPanel);
    }


    /**
     * create new chart panel
     */
    protected void resetChartPanel() {
        if (plot == null) {
            init();
        }
        if (chartPanel != null) {
            chartPanel.setChart(null);
        }

        //        chartPanel = new ChartPanel(chart, false,false,false, true,true) {
        chartPanel = new ChartPanel(chart, true) {
            public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    return;
                }
                super.mouseClicked(event);
            }
            public void mousePressed(MouseEvent event) {
                requestFocus();
                if (SwingUtilities.isRightMouseButton(event)) {
                    showPopupMenu(event);
                } else {
                    super.mousePressed(event);
                }
            }

            public void paintComponent(Graphics g) {
                synchronized (chartManager.getMutex()) {
                    if (chartManager.getOkToDraw()
                            && !chartManager.getSettingData()) {
                        super.paintComponent(g);
                    }
                }
            }


            public void chartChanged(ChartChangeEvent event) {
                synchronized (chartManager.getMutex()) {
                    if ( !chartManager.getSettingData()) {
                        super.chartChanged(event);
                    } else {
                        setRefreshBuffer(true);
                    }
                }
                Misc.runInABit(20, chartManager, "updateThumb", null);
            }
        };
        chartPanel.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                ChartHolder.this.keyPressed(e);
            }
        });

        if (preferredSize != null) {
            chartPanel.setPreferredSize(preferredSize);
        } else {
            chartPanel.setPreferredSize(new Dimension(300, 200));
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
            chartPanel.restoreAutoBounds();
        } else {
            //            super.keyPressed(e);
        }
    }

    /**
     * Move plot up/down.
     *
     * @param up up
     */
    private void upDownPlot(boolean up) {
        if ( !(plot instanceof XYPlot)) {
            return;
        }
        XYPlot xyPlot = (XYPlot) plot;

        int    cnt    = xyPlot.getRangeAxisCount();
        for (int i = 0; i < cnt; i++) {
            ValueAxis            axis   = (ValueAxis) xyPlot.getRangeAxis(i);
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
        if ( !(plot instanceof XYPlot)) {
            return;
        }
        XYPlot xyPlot = (XYPlot) plot;
        int    cnt    = xyPlot.getDomainAxisCount();
        for (int i = 0; i < cnt; i++) {
            ValueAxis            axis   = (ValueAxis) xyPlot.getDomainAxis(i);
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
     * init
     */
    protected void initChart() {
        lastSide   = null;
        paramCount = 0;
    }


    /**
     * add data set
     *
     * @param dataset dataset
     * @param rangeAxis axis
     * @param renderer renderer
     * @param side which side
     */
    protected void add(XYDataset dataset, ValueAxis rangeAxis,
                       XYItemRenderer renderer, AxisLocation side) {
        synchronized (chartManager.getMutex()) {
            XYPlot xyPlot = (XYPlot) plot;
            xyPlot.setRangeAxis(paramCount, rangeAxis, false);
            xyPlot.setDataset(paramCount, dataset);
            xyPlot.mapDatasetToRangeAxis(paramCount, paramCount);
            xyPlot.setRenderer(paramCount, renderer);
            if (side != null) {
                xyPlot.setRangeAxisLocation(paramCount, side);
            }
            paramCount++;
        }
    }




    /**
     * ahs parameters
     *
     * @return has parameters
     */
    public boolean hasParameters() {
        return paramCount > 0;
    }


    /**
     * Set the LayoutRect property.
     *
     * @param value The new value for LayoutRect
     */
    public void setLayoutRect(Rectangle value) {
        layoutRect = value;
    }

    /**
     * Get the LayoutRect property.
     *
     * @return The LayoutRect
     */
    public Rectangle getLayoutRect() {
        return layoutRect;
    }





    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the ChartManager property.
     *
     * @param value The new value for ChartManager
     */
    public void setChartManager(ChartManager value) {
        chartManager = value;
    }

    /**
     * Get the ChartManager property.
     *
     * @return The ChartManager
     */
    public ChartManager getChartManager() {
        return chartManager;
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
     * Set the ShowThumbnail property.
     *
     * @param value The new value for ShowThumbnail
     */
    public void setShowThumbnail(boolean value) {
        showThumbnail = value;
    }

    /**
     * Get the ShowThumbnail property.
     *
     * @return The ShowThumbnail
     */
    public boolean getShowThumbnail() {
        return showThumbnail;
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
     *  Set the ShowTitle property.
     *
     *  @param value The new value for ShowTitle
     */
    public void setShowTitle(boolean value) {
        showTitle = value;
    }

    /**
     *  Get the ShowTitle property.
     *
     *  @return The ShowTitle
     */
    public boolean getShowTitle() {
        return showTitle;
    }

    /**
     *  Set the PreferredSize property.
     *
     *  @param value The new value for PreferredSize
     */
    public void setPreferredSize(Dimension value) {
        preferredSize = value;
    }

    /**
     *  Get the PreferredSize property.
     *
     *  @return The PreferredSize
     */
    public Dimension getPreferredSize() {
        return preferredSize;
    }



}

