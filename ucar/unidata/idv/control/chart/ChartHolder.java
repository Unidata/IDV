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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;




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
    
    /** The date format to use for this chart */
    private String dateFormat = null;

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
    
    /** Combo box containing date formats available for selection.*/
    JComboBox dateFormatBox;

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
        if (dateFormatBox != null) {
        	dateFormat = (String)dateFormatBox.getSelectedItem();
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
        	final XYPlot p = (XYPlot) plot;
            p.setDomainGridlinesVisible(
                domainLineState.getVisible());
            p.setRangeGridlinesVisible(
                rangeLineState.getVisible());
            p.setDomainGridlinePaint(
                domainLineState.getColor());
            p.setRangeGridlinePaint(rangeLineState.getColor());
            p.setDomainGridlineStroke(
                domainLineState.getStroke());
            p.setRangeGridlineStroke(
                    rangeLineState.getStroke());
            if (p.getDomainAxis() instanceof DateAxis && dateFormat != null){
            	final DateAxis ax = (DateAxis)p.getDomainAxis();
            	final TimeZone tz = getChartManager().getControl().getIdv().getPreferenceManager().getDefaultTimeZone();
            	final DateFormat df = new SimpleDateFormat(dateFormat);
            	df.setTimeZone(tz);
            	ax.setDateFormatOverride(df);
            }
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
        	
            @Override
			public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    showPopupMenu(e);
                } 
                else { //Usual Behavior
                	super.mouseReleased(e);
                }
			}

            @Override            
			public void mouseClicked(MouseEvent event) {
                if (SwingUtilities.isRightMouseButton(event)) {
                    return;
                }
                super.mouseClicked(event);
            }
			
            @Override
            public void mousePressed(MouseEvent event) {
                requestFocus();
                if (SwingUtilities.isRightMouseButton(event)) {
                	//Do nothing, for now.
                } else {
                    super.mousePressed(event);
                }
            }

            @Override
            public void paintComponent(Graphics g) {
                synchronized (chartManager.getMutex()) {
                    if (chartManager.getOkToDraw()
                            && !chartManager.getSettingData()) {
                        super.paintComponent(g);
                    }
                }
            }

            @Override
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

    /** keeps track of which legend is visible          */
    Hashtable<Integer, Boolean> legendVisible = new Hashtable<Integer,
                                                    Boolean>();

    /**
     * is the given legend visible
     *
     * @param param which legend
     *
     * @return is visible
     */
    private boolean isLegendVisible(int param) {
        Boolean b = legendVisible.get(param);
        if (b != null) {
            return b.booleanValue();
        }
        return true;
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
     * Get the date format for this plot.
     * 
     * @return the date format for this plot.
     */
    public String getDateFormat() {
		return dateFormat;
	}

    /**
     * Set the date format for this plot.
     * 
     * @param dateFormat The date format for this plot.
     */
	public void setDateFormat(final String dateFormat) {
		this.dateFormat = dateFormat;
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


