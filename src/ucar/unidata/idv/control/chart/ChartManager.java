/**
 * $Id: ChartManager.java,v 1.16 2007/05/09 16:44:57 jeffmc Exp $
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


import static ucar.unidata.idv.IdvPreferenceManager.DATE_FORMATS;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.plot.Plot;

import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.GraphPaperLayout;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import visad.Real;



/**
 * A time series chart
 *
 * @author MetApps Development Team
 * @version $Revision: 1.16 $
 */

public abstract class ChartManager implements ImageObserver {

    /** chart layout type */
    public static final int LAYOUT_HSTACK = 0;

    /** chart layout type */
    public static final int LAYOUT_VSTACK = 1;

    /** chart layout type */
    public static final int LAYOUT_2COLGRID = 2;

    /** chart layout type */
    public static final int LAYOUT_3COLGRID = 3;

    /** chart layout type */
    public static final int LAYOUT_GRAPH = 4;


    /** chart layout type */
    public static final int LAYOUT_TAB = 6;

    /** chart layout type */
    public static final int LAYOUT_CARD = 7;

    /** width of axis_ */
    public static final int AXIS_WIDTH = 30;


    /** for synching_ */
    protected Object MUTEX = new Object();


    /** chart layout */
    private int layout = LAYOUT_GRAPH;

    /** for chart layout */
    private int gridLayoutDimension = Integer.MAX_VALUE;

    /** the charts */
    protected List<ChartHolder> chartHolders = new ArrayList<ChartHolder>();


    /** my control */
    protected DisplayControlImpl control;

    /** gui_ */
    protected JComponent contents;


    /** for gui */
    private JTabbedPane tab = null;

    /** for gui */
    private CardLayout cardLayout;

    /** for gui */
    private JComponent cardContainer;


    /** currently setting data in chart_ */
    protected boolean settingData = false;

    /** ok to draw charts */
    protected boolean okToDraw = true;


    /** List of all fixed ranges. Keep this around for the line state dialogs to show a menu of ranges */
    private List currentRanges = new ArrayList();

    /** The line states_ */
    private List currentLineStates = new ArrayList();

    /** default chart */
    private String defaultChartName;

    /** side legend thumbnail */
    private JLabel chartThumb;

    /** flag for showing the thumbnail */
    private boolean showThumb = false;

    /** will we be updating the thumbnail image          */
    private boolean thumbUpdatePending = false;


    /**
     * ctor
     */
    public ChartManager() {}



    /**
     * ctor
     *
     * @param control the control
     */
    public ChartManager(DisplayControlImpl control) {
        this.control = control;
    }


    /**
     * Default constructor.
     *
     * @param control the control
     * @param chartName name of default chart
     */
    public ChartManager(DisplayControlImpl control, String chartName) {
        this(control);
        this.defaultChartName = chartName;
    }

    /**
     * add line state
     *
     * @param lineState line state
     */
    protected void addLineState(LineState lineState) {
        currentLineStates.add(lineState);
    }

    /**
     * Save the image
     */
    public void saveImage() {
        JComboBox publishCbx =
            control.getIdv().getPublishManager().getSelector("nc.export");
        String filename = FileManager.getWriteFile(FileManager.FILTER_IMAGE,
                              FileManager.SUFFIX_JPG, ((publishCbx != null)
                ? GuiUtils.top(publishCbx)
                : null));
        if (filename == null) {
            return;
        }
        try {
            ImageUtils.writeImageToFile(getContents(), filename);
            control.getIdv().getPublishManager().publishContent(filename,
                    null, publishCbx);
        } catch (Exception exc) {
            LogUtil.logException("Capturing image", exc);
        }
    }



    /**
     * clear all saved line states
     */
    protected void clearLineStates() {
        currentLineStates = new ArrayList();
    }


    /**
     * The chart name has changed
     *
     * @param oldName old name
     * @param newName new name
     */
    protected void chartNameChanged(String oldName, String newName) {
        if (currentLineStates == null) {
            return;
        }
        for (int i = 0; i < currentLineStates.size(); i++) {
            LineState lineState = (LineState) currentLineStates.get(i);
            if (Misc.equals(lineState.getChartName(), oldName)) {
                lineState.setChartName(newName);
            }
        }
    }

    /**
     * The chart name has changed
     *
     * @param newName new name
     */
    public void setChartName(String newName) {
        if (currentLineStates == null) {
            return;
        }
        for (int i = 0; i < currentLineStates.size(); i++) {
            LineState lineState = (LineState) currentLineStates.get(i);
            lineState.setChartName(newName);
        }
    }
    /**
     * init all charts
     */
    protected void initCharts() {
        currentRanges = new ArrayList();
        for (int plotIdx = 0; plotIdx < chartHolders.size(); plotIdx++) {
            initChart((ChartHolder) chartHolders.get(plotIdx));
        }
    }


    /**
     * get the ranges
     *
     * @return ranges
     */
    public List getCurrentRanges() {
        return currentRanges;
    }

    /**
     * add a range
     *
     * @param min min
     * @param max max
     * @param name name
     */
    protected void addRange(double min, double max, String name) {
        if ( !(min == min)) {
            return;
        }
        if ( !(max == max)) {
            return;
        }
        min = Misc.parseNumber(control.getDisplayConventions().format(min));
        max = Misc.parseNumber(control.getDisplayConventions().format(max));
        currentRanges.add(new Range(min, max, name));
    }



    /**
     * for synching the charts
     *
     * @return mutex
     */
    protected Object getMutex() {
        return MUTEX;
    }


    /**
     * init chart
     *
     * @param chartHolder The chart
     */
    protected void initChart(ChartHolder chartHolder) {
        synchronized (getMutex()) {
            chartHolder.initChart();
            initPlot(chartHolder.getPlot());
        }
    }


    /**
     * init plot
     *
     * @param plot plot
     */
    protected void initPlot(Plot plot) {}


    /**
     * factory method to make the plot
     *
     *
     * @return the new plot
     */
    public abstract Plot doMakePlot();


    /**
     * Get the plots
     *
     * @return plots_
     */
    protected List getPlots() {
        List plots = new ArrayList();
        for (ChartHolder chartHolder : chartHolders) {
            plots.add(chartHolder.getPlot());
        }
        return plots;
    }




    /**
     * update thumbnail legend image
     *
     */
    public void updateThumb() {
        updateThumb(false);

    }

    /**
     * update the thumbnail
     *
     * @param force if true then always update the thumb
     */
    private void updateThumb(boolean force) {
        if (force) {
            updateThumbInner();
            return;
        }
        if (thumbUpdatePending) {
            return;
        }
        thumbUpdatePending = true;
        //In 500 ms update the thumbnail image
        Misc.runInABit(500, this, "updateThumbInner", null);
    }


    /**
     * actually update the thumbnail image
     */
    public void updateThumbInner() {
        try {
            if ( !showThumb) {
                return;
            }
            //            if (settingData) return;
            if ((getContents().getWidth() == 0)
                    || (getContents().getHeight() == 0)) {
                //                Misc.runInABit(1000, this, "updateThumb",null);
                return;
            }
            List  images = new ArrayList();
            Image thumb  = ImageUtils.getImage(getContents());
            if (thumb == null) {
                return;
            }
            if ((thumb.getWidth(null) <= 0) || (thumb.getHeight(null) <= 0)) {
                return;
            }
            double ratio = thumb.getWidth(null)
                           / (double) thumb.getHeight(null);
            //            int width = Math.max(label.getWidth(),200);
            int width  = 200;
            int height = (int) (width / ratio);
            thumb = ImageUtils.toBufferedImage(thumb.getScaledInstance(width,
                    height,
                    Image.SCALE_AREA_AVERAGING), BufferedImage.TYPE_INT_RGB);
            boolean chartsShowingSomething = false;
            for (ChartHolder chartHolder : chartHolders) {
                if (chartHolder.getBeingShown()
                        && chartHolder.hasParameters()) {
                    chartsShowingSomething = true;
                    break;
                }
            }
            if (chartsShowingSomething) {
                getThumb().setIcon(new ImageIcon(thumb));
            } else {
                getThumb().setIcon(
                    GuiUtils.getImageIcon("/auxdata/ui/icons/OnePixel.gif"));
            }





        } catch (Exception exc) {
            //            LogUtil.logException("Showing thumbnail", exc);
        }
        thumbUpdatePending = false;
    }



    /**
     * Class FixedWidthLogarithmicAxis fixed width log axis
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.16 $
     */
    public static class FixedWidthLogarithmicAxis extends LogarithmicAxis {

        /**
         * ctor
         */
        public FixedWidthLogarithmicAxis() {
            this(null);
        }

        /**
         * ctor
         *
         * @param theLabel label
         */
        public FixedWidthLogarithmicAxis(String theLabel) {
            super(theLabel);
            setFixedDimension(AXIS_WIDTH);
        }

        /**
         * utility
         *
         * @param ticks param
         * @param g2 param
         * @param drawArea param
         * @param vertical param
         *
         * @return width
         */
        protected double findMaximumTickLabelWidth(List ticks, Graphics2D g2,
                Rectangle2D drawArea, boolean vertical) {
            return AXIS_WIDTH;
        }
    }



    /**
     * Class FixedWidthNumberAxis fixed width axis
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.16 $
     */
    public static class FixedWidthNumberAxis extends NumberAxis {

        /**
         * ctor
         */
        public FixedWidthNumberAxis() {
            this(null);
        }

        /**
         * ctor
         *
         * @param theLabel label
         */
        public FixedWidthNumberAxis(String theLabel) {
            super(theLabel);
            setFixedDimension(AXIS_WIDTH);
        }

        /**
         * get width
         *
         * @param ticks param
         * @param g2 param
         * @param drawArea param
         * @param vertical param
         *
         * @return width
         */
        protected double findMaximumTickLabelWidth(List ticks, Graphics2D g2,
                Rectangle2D drawArea, boolean vertical) {
            return AXIS_WIDTH;
        }
    }



    /**
     * Tell the chart holds that something changed
     */
    public void signalChartChanged() {
        for (ChartHolder chartHolder : chartHolders) {
            if (chartHolder.getChartPanel() != null) {
                chartHolder.getChartPanel().chartChanged(
                    new ChartChangeEvent(this));
            }
        }
    }

    /**
     * refresh chart holders
     */
    private void setRefresh() {
        for (ChartHolder chartHolder : chartHolders) {
            chartHolder.getChartPanel().setRefreshBuffer(true);
        }
    }

    /**
     * Call this after you are done loading data into the charts. It resets the flag
     * to ignore any intermediate chart repaints and then it signals the chart to repaint
     */
    protected void doneLoadingData() {
        settingData = false;
        signalChartChanged();
        updateThumb(false);
    }

    /**
     * Call this before you start loading data into the charts. It sets a flag
     * to ignore any intermediate chart repaints
     */
    protected void startLoadingData() {
        settingData = true;
    }


    /**
     * get menu items for chart
     *
     * @param chartHolder chart_
     * @param items items
     */
    protected void getPopupMenuItems(ChartHolder chartHolder, List items) {}


    /**
     * show dialog for chart
     *
     * @param chartHolder chart
     */
    protected void showPropertiesDialog(final ChartHolder chartHolder) {
        List comps = new ArrayList();
        getPropertiesComponents(chartHolder, comps);
        GuiUtils.tmpInsets = GuiUtils.INSETS_5;
        JComponent contents = GuiUtils.doLayout(comps, 2, GuiUtils.WT_NY,
                                  GuiUtils.WT_N);
        final JDialog propertiesDialog =
            GuiUtils.createDialog(chartHolder.getName() + " Properties",
                                  true);

        ActionListener listener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String cmd = ae.getActionCommand();
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_APPLY)) {
                    if ( !applyProperties(chartHolder)) {
                        return;
                    }
                    updateThumb(true);
                }
                if (cmd.equals(GuiUtils.CMD_OK)
                        || cmd.equals(GuiUtils.CMD_CANCEL)) {
                    propertiesDialog.dispose();
                }
            }
        };
        JPanel buttons = GuiUtils.makeButtons(listener,
                             new String[] { GuiUtils.CMD_APPLY,
                                            GuiUtils.CMD_OK,
                                            GuiUtils.CMD_CANCEL });

        contents = GuiUtils.centerBottom(contents, buttons);
        contents = GuiUtils.inset(contents, 5);
        propertiesDialog.getContentPane().add(GuiUtils.top(contents));
        propertiesDialog.pack();

        propertiesDialog.setLocation(200, 200);
        propertiesDialog.setVisible(true);
    }



    /**
     * get properties widgets
     *
     * @param chartHolder the chart
     * @param comps comps
     */
    protected void getPropertiesComponents(ChartHolder chartHolder,
                                           List comps) {
        chartHolder.nameFld = new JTextField(chartHolder.getName());
        comps.add(GuiUtils.rLabel("Name: "));
        comps.add(chartHolder.nameFld);

        chartHolder.backgroundSwatch =
            GuiUtils.makeColorSwatchWidget(chartHolder.getBackgroundColor(),
                                           "")[0];
        chartHolder.dataAreaSwatch =
            GuiUtils.makeColorSwatchWidget(chartHolder.getDataAreaColor(),
                                           "")[0];
        comps.add(GuiUtils.rLabel("Color: "));
        List colorComps = Misc.newList(new JLabel("Background:"),
                                       chartHolder.backgroundSwatch,
                                       GuiUtils.filler(10, 5),
                                       new JLabel("Chart:"),
                                       chartHolder.dataAreaSwatch);
        comps.add(GuiUtils.left(GuiUtils.hbox(colorComps, 5)));

        comps.add(GuiUtils.rLabel("Domain Lines: "));
        comps.add(chartHolder.getDomainLineState().getPropertyContents());
        comps.add(GuiUtils.rLabel("Range Lines: "));
        comps.add(chartHolder.getRangeLineState().getPropertyContents());
        
        chartHolder.getPropertiesComponents(comps);
        comps.add(GuiUtils.rLabel("Date Format: "));
		final String df = getControl().getIdv().getPreferenceManager()
				.getDefaultDateFormat();
		final List<String> l = new LinkedList<String>(DATE_FORMATS);
		if (!l.contains(df)) {
			l.add(df);
		}
		chartHolder.dateFormatBox = GuiUtils.getEditableBox(
				new LinkedList<String>(DATE_FORMATS),
				chartHolder.getDateFormat() == null ? df : chartHolder
						.getDateFormat());
		comps.add(GuiUtils.leftRight(chartHolder.dateFormatBox, GuiUtils.filler())); 
    }


    /**
     * apply properties to chart
     *
     * @param chartHolder the chart
     *
     * @return OK
     */
    protected boolean applyProperties(ChartHolder chartHolder) {
        if ( !chartHolder.getDomainLineState().applyProperties()) {
            return false;
        }
        if ( !chartHolder.getRangeLineState().applyProperties()) {
            return false;
        }
        if ( !chartHolder.applyProperties()) {
            return false;
        }
        String newName = chartHolder.nameFld.getText();
        if ( !Misc.equals(newName, chartHolder.getName())) {
            chartNameChanged(chartHolder.getName(), newName);
            chartHolder.setName(newName);
        }

        chartHolder.applyPlotProperties();
        return true;
    }




    /**
     * Get the GraphPaperLayout locations
     *
     * @return locations for layout
     */
    private List getLocations() {
        List locations = new ArrayList();
        for (ChartHolder chartHolder : chartHolders) {
            if ( !chartHolder.getBeingShown()) {
                continue;
            }
            locations.add(
                new GraphPaperLayout.Location(
                    chartHolder.getChartPanel(), chartHolder,
                    chartHolder.getName(), chartHolder.getLayoutRect()));
        }
        return locations;
    }

    /**
     * apply graph paper layout locations
     *
     * @param locations locations
     */
    private void applyLocations(List locations) {
        for (int i = 0; i < locations.size(); i++) {
            GraphPaperLayout.Location loc =
                (GraphPaperLayout.Location) locations.get(i);
            ChartHolder chartHolder = (ChartHolder) loc.getObject();
            chartHolder.setLayoutRect(loc.getRect());
        }
    }

    /**
     * set layout
     *
     * @param i layout type
     */
    public void setLayoutType(Integer i) {
        layout = i.intValue();
        updateContents(true);
    }

    /**
     * change layout
     */
    public void editLayout() {
        List locations = getLocations();
        GraphPaperLayout.showDialog(locations, "Edit Chart Layout");
        applyLocations(locations);
        updateContents(true);
    }


    /**
     * add menu items
     *
     * @param items items
     */
    public void addViewMenuItems(List items) {
        JMenu layoutMenu = new JMenu("Layout");
        items.add(layoutMenu);
        JMenuItem mi;

        int[]     types = {
            LAYOUT_GRAPH, LAYOUT_VSTACK, LAYOUT_HSTACK, LAYOUT_2COLGRID,
            LAYOUT_3COLGRID, LAYOUT_TAB
        };

        String[] names = {
            "Grid", "Vertical", "Horizontal", "2 Columns", "3 Columns", "Tabs"
        };
        layoutMenu.add(mi = GuiUtils.makeMenuItem("Change Grid Layout", this,
                "editLayout"));
        layoutMenu.addSeparator();
        mi.setEnabled(layout == LAYOUT_GRAPH);

        for (int i = 0; i < types.length; i++) {
            layoutMenu.add(mi = GuiUtils.makeMenuItem(names[i], this,
                    "setLayoutType", new Integer(types[i])));
            if (types[i] == layout) {
                mi.setEnabled(false);
            }
        }

        items.add(GuiUtils.MENU_SEPARATOR);
        for (ChartHolder chartHolder : chartHolders) {
            if ( !chartHolder.getBeingShown()) {
                continue;
            }
            List chartItems = new ArrayList();
            chartHolder.getMenuItems(chartItems);
            items.add(GuiUtils.makeMenu("Chart: " + chartHolder.getName(),
                                        chartItems));
        }


    }


    /**
     * ok to draw chart
     *
     * @return ok to draw
     */
    public boolean getOkToDraw() {
        return okToDraw;
    }

    /**
     * Are we currently setting data
     *
     * @return is setting data
     */
    public boolean getSettingData() {
        return settingData;

    }



    /**
     * Stub for handling time change events.
     */
    public void timeChanged() {
        timeChanged(null);
    }

    /**
     * Stub for handling time change events.  Signals a redraw.
     * @param value  the time value (may be null);
     */
    public void timeChanged(Real value) {
        signalChartChanged();
        updateThumb(true);
        //        getContents().repaint();
    }


    /**
     * Handle the image update
     *
     * @param img img
     * @param flags flags
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     *
     * @return keep going
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width,
                               int height) {

        boolean all = (flags & ImageObserver.ALLBITS) != 0;
        if (all) {
            getContents().repaint();
            return false;
        }
        return true;
    }



    /**
     * do we have anything to show
     *
     * @return anything to show
     */
    public boolean hasStuff() {
        for (ChartHolder chartHolder : chartHolders) {
            if (chartHolder.hasParameters()) {
                return true;
            }
        }
        return false;
    }



    /**
     * get gui
     *
     * @return gui
     */
    public JComponent getContents() {
        if (contents == null) {
            doMakeContents();
            MUTEX = contents.getTreeLock();
        }
        return contents;
    }



    /**
     * get list of chart names
     *
     * @return names
     */
    public List getPlotNames() {
        List names = new ArrayList();
        for (ChartHolder chartHolder : chartHolders) {
            names.add(chartHolder.getName());
        }
        return names;
    }





    /**
     * Get the chart holder that is displaying the given line state.
     *
     * @param lineState The line state
     *
     * @return The chart holder
     */
    protected ChartHolder getChartHolder(LineState lineState) {
        String      chartName   = lineState.getChartName();
        ChartHolder chartHolder = getChartHolder(chartName);
        if (chartName == null) {
            lineState.setChartName(chartHolder.getName());
        }
        return chartHolder;

    }


    /**
     * find chart with name
     *
     * @param name name
     *
     * @return the chart or null if none found
     */
    protected ChartHolder findChartHolder(String name) {
        for (ChartHolder tmp : chartHolders) {
            //If no name then use first chart
            if ((name == null) || Misc.equals(name, tmp.getName())) {
                return tmp;
            }
        }
        return null;
    }


    /**
     * find chart with name
     *
     * @param name name
     *
     * @return the chart or create one if needed
     */
    protected ChartHolder getChartHolder(String name) {
        ChartHolder chartHolder = findChartHolder(name);
        if (chartHolder == null) {
            chartHolder = new ChartHolder(this, name,
                                          getPreferredChartSize());
            addChart(chartHolder);
        }
        return chartHolder;
    }

    /**
     * add chart
     *
     * @param chartHolder chart
     */
    protected void addChart(ChartHolder chartHolder) {
        initChart(chartHolder);
        chartHolders.add(chartHolder);
    }


    /**
     * Get the preferred chart size
     *
     * @return The preferred chart size
     */
    protected Dimension getPreferredChartSize() {
        return new Dimension(300, 200);
    }

    /**
     * make gui
     */
    protected void doMakeContents() {
        contents = new JPanel(new BorderLayout());
        //Only create a new chart if we don't have any
        ChartHolder chartHolder = findChartHolder(null);
        if (chartHolder == null) {
            makeInitialChart();
        }
        initCharts();
        updateContents(true);
    }

    /**
     * make the chart
     */
    protected void makeInitialChart() {
        getChartHolder(getDefaultChartName());
    }

    /**
     * get name to use for default chart
     *
     * @return default chart name
     */
    protected String getDefaultChartName() {
        if (defaultChartName != null) {
            return defaultChartName;
        }
        return "Chart";
    }

    /**
     * update gui
     */
    protected void updateContents() {
        updateContents(false);
    }



    /**
     * update gui
     *
     * @param force always do it
     */
    protected void updateContents(boolean force) {
        if (contents == null) {
            return;
        }
        List    comps        = new ArrayList();
        int     tabIndex     = 0;
        boolean needToUpdate = false;
        int     goodCharts   = 0;

        for (ChartHolder chartHolder : chartHolders) {
            if (chartHolder.hasParameters()) {
                goodCharts++;
            }
        }

        for (int plotIdx = 0; plotIdx < chartHolders.size(); plotIdx++) {
            ChartHolder chartHolder = (ChartHolder) chartHolders.get(plotIdx);
            if (chartHolder.hasParameters()
                    || ((plotIdx == 0) && (goodCharts == 0))) {
                if ( !chartHolder.getBeingShown()) {
                    needToUpdate = true;
                }
            } else if (chartHolder.getBeingShown()) {
                needToUpdate = true;
            }
        }

        if ( !force && !needToUpdate) {
            return;
        }

        contents.removeAll();

        if (layout == LAYOUT_TAB) {
            if (tab == null) {
                tab = new JTabbedPane();
            } else {
                tab.removeAll();
            }
        } else {
            tab = null;
        }


        for (ChartHolder chartHolder : chartHolders) {
            chartHolder.resetChartPanel();
        }

        for (int plotIdx = 0; plotIdx < chartHolders.size(); plotIdx++) {
            ChartHolder chartHolder = (ChartHolder) chartHolders.get(plotIdx);
            if ((plotIdx == 0) && (goodCharts == 0)) {
                //if we don't have any charts then always show the first one
            } else {
                if ( !chartHolder.hasParameters()) {
                    chartHolder.setBeingShown(false);
                    continue;
                }
            }
            chartHolder.setBeingShown(true);
            if (layout == LAYOUT_TAB) {
                tab.addTab(chartHolder.getName(),
                           chartHolder.getChartPanel());
            } else {
                comps.add(chartHolder.getChartPanel());
            }
        }

        JComponent inner = null;
        if (layout == LAYOUT_VSTACK) {
            inner = GuiUtils.doLayout(comps, 1, GuiUtils.WT_Y, GuiUtils.WT_Y);
        } else if (layout == LAYOUT_HSTACK) {
            inner = GuiUtils.doLayout(comps, comps.size(), GuiUtils.WT_Y,
                                      GuiUtils.WT_Y);
        } else if (layout == LAYOUT_2COLGRID) {
            inner = GuiUtils.doLayout(comps, 2, GuiUtils.WT_Y, GuiUtils.WT_Y);
        } else if (layout == LAYOUT_3COLGRID) {
            inner = GuiUtils.doLayout(comps, 3, GuiUtils.WT_Y, GuiUtils.WT_Y);
        } else if (layout == LAYOUT_GRAPH) {
            inner = GraphPaperLayout.layout(getLocations());
        } else if (layout == LAYOUT_TAB) {
            inner = tab;
        } else {
            //Default to vstack
            inner = GuiUtils.doLayout(comps, 1, GuiUtils.WT_Y, GuiUtils.WT_Y);
        }

        contents.add(BorderLayout.CENTER, inner);
        contents.invalidate();
        contents.validate();
        contents.repaint();
    }

    /**
     * get thumb label
     *
     * @return thumb
     */
    public JLabel getThumb() {
        if (chartThumb == null) {
            chartThumb = new JLabel("");
            chartThumb.setVisible(showThumb);
        }
        return chartThumb;
    }


    /**
     * Set the Layout property.
     *
     * @param value The new value for Layout
     */
    public void setLayout(int value) {
        layout = value;
    }

    /**
     * Get the Layout property.
     *
     * @return The Layout
     */
    public int getLayout() {
        return layout;
    }

    /**
     * Set the GridLayoutDimension property.
     *
     * @param value The new value for GridLayoutDimension
     */
    public void setGridLayoutDimension(int value) {
        gridLayoutDimension = value;
    }

    /**
     * Get the GridLayoutDimension property.
     *
     * @return The GridLayoutDimension
     */
    public int getGridLayoutDimension() {
        return gridLayoutDimension;
    }

    /**
     * Set the ChartHolders property.
     *
     * @param value The new value for ChartHolders
     */
    public void setChartHolders(List value) {
        chartHolders = value;
    }

    /**
     * Get the ChartHolders property.
     *
     * @return The ChartHolders
     */
    public List getChartHolders() {
        return chartHolders;
    }

    /**
     * Set the Control property.
     *
     * @param value The new value for Control
     */
    public void setControl(DisplayControlImpl value) {
        control = value;
    }

    /**
     * Get the Control property.
     *
     * @return The Control
     */
    public DisplayControlImpl getControl() {
        return control;
    }



    /**
     *  Set the ShowThumb property.
     *
     *  @param value The new value for ShowThumb
     */
    public void setShowThumb(boolean value) {
        if (showThumb == value) {
            return;
        }
        showThumb = value;
        //If we have a thumb nail then update it
        if (chartThumb != null) {
            getThumb().setVisible(value);
            updateThumb(true);
        }
    }


    /**
     *  Get the ShowThumb property.
     *
     *  @return The ShowThumb
     */
    public boolean getShowThumb() {
        return showThumb;
    }




}


