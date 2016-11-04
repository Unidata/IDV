/*
 * Copyright 1997-2016 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.control;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
import org.jfree.data.statistics.HistogramType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.DisplayControl;
import ucar.unidata.idv.control.chart.DataChoiceWrapper;
import ucar.unidata.idv.control.chart.HistogramWrapper;
import ucar.unidata.idv.control.chart.MyHistogramDataset;
import ucar.unidata.idv.control.multi.DisplayGroup;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;

import visad.ErrorEstimate;
import visad.FlatField;
import visad.Unit;
import visad.VisADException;



import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;


/**
 * Wraps a JFreeChart histogram to ease working with VisAD data.
 */
public class McVHistogramWrapper extends HistogramWrapper {

    /** _more_ */
    private static final Logger logger =
        LoggerFactory.getLogger(McVHistogramWrapper.class);

    /** _more_ */
    private DisplayControl imageControl;

    /** The plot */
    private XYPlot plot;

    /** _more_ */
    private double low;

    /** _more_ */
    private double high;

    /**
     * Default ctor
     */
    public McVHistogramWrapper() {}

    /**
     * Ctor
     *
     * @param name The name.
     * @param dataChoices List of data choices.
     * @param control {@literal "Parent"} control.
     */
    public McVHistogramWrapper(String name, List dataChoices,
                               DisplayControlImpl control) {
        super(name, dataChoices);
        imageControl = control;
    }

    /**
     * Create the chart.
     */
    private void createChart() {
        if (chartPanel != null) {
            return;
        }

        MyHistogramDataset dataset = new MyHistogramDataset();
        chart = ChartFactory.createHistogram("Histogram", null, null,
                                             dataset,
                                             PlotOrientation.VERTICAL, false,
                                             false, false);
        chart.getXYPlot().setForegroundAlpha(0.75f);
        plot = (XYPlot) chart.getPlot();
        initXYPlot(plot);
        chartPanel = doMakeChartPanel(chart);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JComponent doMakeContents() {
        return super.doMakeContents();
    }

    /**
     * Clear the histogram.
     */
    public void clearHistogram() {
        if (chartPanel != null) {
            XYPlot tempPlot = chartPanel.getChart().getXYPlot();
            for (int i = 0; i < tempPlot.getDatasetCount(); i++) {
                MyHistogramDataset dataset =
                    (MyHistogramDataset) tempPlot.getDataset(i);
                if (dataset != null) {
                    dataset.removeAllSeries();
                }
            }
        }
    }

    /**
     * _more_
     */
    public void saveImage() {
        JComboBox publishCbx =
            imageControl.getViewManager().getPublishManager().getSelector(
                "nc.export");
        String filename = FileManager.getWriteFile(FileManager.FILTER_IMAGE,
                              FileManager.SUFFIX_JPG, ((publishCbx != null)
                ? GuiUtils.top(publishCbx)
                : null));
        if (filename == null) {
            return;
        }
        try {
            ImageUtils.writeImageToFile(getContents(), filename);
            imageControl.getViewManager().getPublishManager().publishContent(
                filename, null, publishCbx);
        } catch (Exception exc) {
            LogUtil.logException("Capturing image", exc);
        }
    }


    /**
     * Create the histogram.
     *
     * @param data Data to use in histogram.
     *
     * @throws IllegalArgumentException if {@code data} is all NaNs.
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData(FlatField data)
            throws IllegalArgumentException, RemoteException, VisADException {
        if ((data != null) && !GridUtil.isAllMissing(data)) {
            reallyLoadData(data);
        } else {
            throw new IllegalArgumentException(
                "Nothing to show in histogram");
        }
    }

    /**
     * Assumes that {@code data} has been validated and is okay to actually try
     * loading.
     *
     * @param data Data to use in histogram. Cannot be {@code null} or all NaNs.
     *
     * @throws VisADException
     * @throws RemoteException
     */
    private void reallyLoadData(FlatField data)
            throws VisADException, RemoteException {
        createChart();
        List dataChoiceWrappers = getDataChoiceWrappers();

        try {
            clearHistogram();

            Hashtable       props  = new Hashtable();
            ErrorEstimate[] errOut = new ErrorEstimate[1];
            for (int paramIdx = 0; paramIdx < dataChoiceWrappers.size();
                    paramIdx++) {
                DataChoiceWrapper wrapper =
                    (DataChoiceWrapper) dataChoiceWrappers.get(paramIdx);

                DataChoice dataChoice = wrapper.getDataChoice();
                props = dataChoice.getProperties();
                Unit defaultUnit =
                    ucar.visad.Util.getDefaultRangeUnits((FlatField) data)[0];
                Unit unit =
                    ((DisplayControlImpl) imageControl).getDisplayUnit();
                double[][] samples = data.getValues(false);
                double[] actualValues = filterData(samples[0],
                                            getTimeValues(samples,
                                                    data))[0];
                if ((defaultUnit != null) && !defaultUnit.equals(unit)) {
                    actualValues = Unit.transformUnits(unit, errOut,
                            defaultUnit, null, actualValues);
                }
                final NumberAxis domainAxis =
                    new NumberAxis(wrapper.getLabel(unit));

                domainAxis.setAutoRangeIncludesZero(false);

                XYItemRenderer renderer;
                if (getStacked()) {
                    renderer = new StackedXYBarRenderer();
                } else {
                    renderer = new XYBarRenderer();
                }
                if ((plot == null) && (chartPanel != null)) {
                    plot = chartPanel.getChart().getXYPlot();
                }
                plot.setRenderer(paramIdx, renderer);
                Color c = wrapper.getColor(paramIdx);
                domainAxis.setLabelPaint(c);
                renderer.setSeriesPaint(0, c);

                MyHistogramDataset dataset = new MyHistogramDataset();
                dataset.setType(HistogramType.FREQUENCY);
                dataset.addSeries(dataChoice.getName() + " [" + unit + ']',
                                  actualValues, getBins());
                samples      = null;
                actualValues = null;
                plot.setDomainAxis(paramIdx, domainAxis, false);
                plot.mapDatasetToDomainAxis(paramIdx, paramIdx);
                plot.setDataset(paramIdx, dataset);

                domainAxis.addChangeListener(new AxisChangeListener() {
                            public void axisChanged(AxisChangeEvent ae) {
                                if ( !imageControl.isInitDone()) {
                                    return;
                                }

                                Range range = domainAxis.getRange();
                                double newLow =
                                    Math.floor(range.getLowerBound() + 0.5);
                                double newHigh =
                                    Math.floor(range.getUpperBound() + 0.5);
                                double prevLow  = getLow();
                                double prevHigh = getHigh();
                                try {
                                    ucar.unidata.util.Range newRange;
                                    if (prevLow > prevHigh) {
                                        newRange =
                                            new ucar.unidata.util.Range(
                                                newHigh, newLow);
                                    } else {
                                        newRange =
                                            new ucar.unidata.util.Range(
                                                newLow, newHigh);
                                    }
                                    ((DisplayControlImpl) imageControl).setRange(
                                        newRange);
                                } catch (Exception e) {
                                    logger.error("Cannot change range", e);
                                }
                            }
                        });

                Range range = domainAxis.getRange();
                low  = range.getLowerBound();
                high = range.getUpperBound();
            }

        } catch (Exception exc) {
            System.out.println("Exception exc=" + exc);
            LogUtil.logException("Error creating data set", exc);
        }
    }

    /**
     * Modify the low and high values of the domain axis.
     *
     * @param lowVal Low value.
     * @param hiVal High value.
     *
     * @return {@code false} if {@link #plot} is {@code null}. {@code true}
     * otherwise.
     */
    protected boolean modifyRange(double lowVal, double hiVal) {
        return modifyRange(lowVal, hiVal, true);
    }

    /**
     * Modify the low and high values of the domain axis.
     *
     * @param lowVal Low value.
     * @param hiVal High value.
     * @param notify Whether or not listeners should be notified.
     *
     * @return {@code false} if {@link #plot} is {@code null}. {@code true}
     * otherwise.
     */
    protected boolean modifyRange(double lowVal, double hiVal,
                                  boolean notify) {
        try {
            if (plot == null) {
                return false;
            }
            ValueAxis domainAxis = plot.getDomainAxis();
            org.jfree.data.Range newRange = new org.jfree.data.Range(lowVal,
                                                hiVal);
            domainAxis.setRange(newRange, domainAxis.isAutoRange(), notify);
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected Range getRange() {
        ValueAxis domainAxis = plot.getDomainAxis();
        return domainAxis.getRange();
    }

    /**
     * _more_
     */
    protected void doReset() {
        resetPlot();
    }

    /**
     * reset the histogram to its previous range
     */
    public void resetPlot() {
        if (chart == null) {
            return;
        }
        if ( !(chart.getPlot() instanceof XYPlot)) {
            return;
        }
        XYPlot plot = (XYPlot) chart.getPlot();
        int    rcnt = plot.getRangeAxisCount();
        for (int i = 0; i < rcnt; i++) {
            ValueAxis axis = plot.getRangeAxis(i);
            axis.setAutoRange(true);
        }
        int dcnt = plot.getDomainAxisCount();
        for (int i = 0; i < dcnt; i++) {
            ValueAxis axis = plot.getDomainAxis(i);
            try {
                axis.setRange(low, high);
            } catch (Exception e) {
                logger.warn(
                    "jfreechart does not like ranges to be high -> low", e);
            }
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getLow() {
        return low;
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    public void setLow(double val) {
        low = val;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public double getHigh() {
        return high;
    }

    /**
     * _more_
     *
     * @param val _more_
     */
    public void setHigh(double val) {
        high = val;
    }

    /**
     * SHow the popup menu
     *
     * @param where component to show near to
     * @param x x
     * @param y y
     */
    @Override
    public void showPopup(JComponent where, int x, int y) {
        List items = new ArrayList();
        items = getPopupMenuItems(items);
        if (items.isEmpty()) {
            return;
        }
        GuiUtils.makePopupMenu(items).show(where, x, y);
    }

    /**
     * Add the default menu items
     *
     * @param items List of menu items
     *
     * @return The items list
     */
    @Override
    protected List getPopupMenuItems(List items) {
        items = super.getPopupMenuItems(items);
        for (Object o : items) {
            if (o instanceof JMenuItem) {
                JMenuItem menuItem = (JMenuItem) o;
                if ("Properties...".equals(menuItem.getText())) {
                    menuItem.setActionCommand(ChartPanel.PROPERTIES_COMMAND);
                    menuItem.addActionListener(buildHistoPropsListener());
                }
            }
        }
        return items;
    }

    /**
     * @return {@link ActionListener} that listens for
     * {@link ChartPanel#PROPERTIES_COMMAND} events and shows the histogram
     * properties.
     */
    private ActionListener buildHistoPropsListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String command = event.getActionCommand();
                if (ChartPanel.PROPERTIES_COMMAND.equals(command)) {
                    McVHistogramWrapper.this.showProperties();
                    return;
                }
            }
        };
    }

    /**
     * Show the properties dialog
     *
     * @return Was it ok
     */
    @Override
    public boolean showProperties() {
        boolean result;
        if ( !hasDisplayControl()) {
            result = showProperties(null, 0, 0);
        } else {
            result = super.showProperties();
        }
        return result;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean hasDisplayControl() {
        return getDisplayControl() != null;
    }

    /**
     * Remove me
     *
     * @return was removed
     */
    public boolean removeDisplayComponent() {
        if (GuiUtils.askYesNo("Remove Display",
                              "Are you sure you want to remove: "
                              + toString())) {
            DisplayGroup displayGroup = getDisplayGroup();
            if (displayGroup != null) {
                displayGroup.removeDisplayComponent(this);
            }

            if (hasDisplayControl()) {
                getDisplayControl().removeDisplayComponent(this);
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Apply the properties
     *
     * @return Success
     */
    @Override
    protected boolean doApplyProperties() {
        applyProperties();

        //        try {
        //            // need to deal with the data being an imageseq
        //            loadData((FlatField)imageControl.getDataChoice().getData(null));
        //        } catch (RemoteException e) {
        //            logger.error("trying to reload data", e);
        //        } catch (DataCancelException e) {
        //            logger.error("trying to reload data", e);
        //        } catch (VisADException e) {
        //            logger.error("trying to reload data", e);
        //        }

        return true;
    }

    /**
     * Been removed, do any cleanup
     */
    public void doRemove() {
        isRemoved = true;
        List displayables = getDisplayables();
        if (hasDisplayControl() && !displayables.isEmpty()) {
            getDisplayControl().removeDisplayables(displayables);
        }
        firePropertyChange(PROP_REMOVED, null, this);
    }

}
