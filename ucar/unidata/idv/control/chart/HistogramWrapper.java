/*
 * $Id: HistogramWrapper.java,v 1.16 2007/04/16 21:32:10 jeffmc Exp $
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
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.*;
import org.jfree.chart.renderer.xy.*;
import org.jfree.chart.urls.*;
import org.jfree.data.*;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;
import org.jfree.data.time.*;
import org.jfree.data.xy.*;
import org.jfree.ui.*;



import ucar.unidata.data.DataChoice;


import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.sounding.TrackDataSource;


import ucar.unidata.idv.control.DisplayControlImpl;
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
 * @version $Revision: 1.16 $
 */
public class HistogramWrapper extends PlotWrapper {

    /** The plot */
    private XYPlot plot;


    /** How many bins in the histgram */
    private int bins = 100;

    /** Is the histogram stacked bars. Does not work right now */
    private boolean stacked = false;


    /** for properties dialog */
    private JTextField binFld;

    /** for properties dialog */
    private JCheckBox stackedCbx;



    /**
     * Default ctor
     */
    public HistogramWrapper() {}



    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public HistogramWrapper(String name, List dataChoices) {
        super(name, dataChoices);
    }


    /**
     * Type name
     *
     * @return Type name
     */
    public String getTypeName() {
        return "Histogram";
    }


    /**
     * Create the chart
     */
    private void createChart() {
        if (chartPanel != null) {
            return;
        }

        MyHistogramDataset dataset = new MyHistogramDataset();
        chart = ChartFactory.createHistogram("Histogram", null, null,
                                             dataset,
                                             PlotOrientation.VERTICAL, true,
                                             false, false);
        chart.getXYPlot().setForegroundAlpha(0.75f);
        plot = (XYPlot) chart.getPlot();
        initXYPlot(plot);
        chartPanel = doMakeChartPanel(chart);
    }

    /**
     *
     * Create the chart if needed
     *
     * @return The gui contents
     */
    protected JComponent doMakeContents() {
        createChart();
        return chartPanel;
    }


    /**
     * Create the charts
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() throws VisADException, RemoteException {
        createChart();
        List dataChoiceWrappers = getDataChoiceWrappers();
        try {
            for (int dataSetIdx = 0; dataSetIdx < plot.getDatasetCount();
                    dataSetIdx++) {
                MyHistogramDataset dataset =
                    (MyHistogramDataset) plot.getDataset(dataSetIdx);
                dataset.removeAllSeries();
            }

            //            dataset.removeAllSeries();
            Hashtable props = new Hashtable();
            props.put(TrackDataSource.PROP_TRACKTYPE,
                      TrackDataSource.ID_TIMETRACE);

            for (int paramIdx = 0; paramIdx < dataChoiceWrappers.size();
                    paramIdx++) {
                DataChoiceWrapper wrapper =
                    (DataChoiceWrapper) dataChoiceWrappers.get(paramIdx);

                DataChoice dataChoice = wrapper.getDataChoice();
                FlatField data =
                    getFlatField((FieldImpl) dataChoice.getData(null, props));
                Unit unit =
                    ucar.visad.Util.getDefaultRangeUnits((FlatField) data)[0];
                double[][] samples = data.getValues(false);
                double[] actualValues = filterData(samples[0],
                                            getTimeValues(samples, data))[0];
                NumberAxis domainAxis =
                    new NumberAxis(wrapper.getLabel(unit));

                XYItemRenderer renderer;
                if (stacked) {
                    renderer = new StackedXYBarRenderer();
                } else {
                    renderer = new XYBarRenderer();
                }
                plot.setRenderer(paramIdx, renderer);
                Color c = wrapper.getColor(paramIdx);
                domainAxis.setLabelPaint(c);
                renderer.setSeriesPaint(0, c);





                MyHistogramDataset dataset = new MyHistogramDataset();
                dataset.setType(HistogramType.FREQUENCY);
                dataset.addSeries(dataChoice.getName() + " [" + unit + "]",
                                  actualValues, bins);
                plot.setDomainAxis(paramIdx, domainAxis, false);
                plot.mapDatasetToDomainAxis(paramIdx, paramIdx);
                plot.setDataset(paramIdx, dataset);



            }

        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
            return;
        }
    }



    /**
     * Add components to properties dialog
     *
     * @param comps  List of components
     * @param tabIdx Which tab in properties dialog
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx != 0) {
            return;
        }
        comps.add(GuiUtils.rLabel("Histogram: "));

        comps.add(
            GuiUtils.left(
                GuiUtils.hbox(
                    Misc.newList(
                        new JLabel("Number of Bins: "),
                        binFld = new JTextField("" + bins, 6)), 4)));
        //                                             new JLabel("      Stacked: "),
        //stackedCbx = new JCheckBox("",stacked)),4)));
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
        try {
            bins = new Integer(binFld.getText().trim()).intValue();
        } catch (NumberFormatException nfe) {
            LogUtil.userErrorMessage("Bad value for bins: "
                                     + binFld.getText());
            return false;
        }
        //        stacked = stackedCbx.isSelected();
        return true;
    }


    /**
     * Can we do chart colors
     *
     * @return can do colors
     */
    protected boolean canDoColors() {
        return true;
    }

    /**
     * Can we have colors on the data chocie wrappers in the properties dialog
     *
     * @return can do wrapper color
     */
    public boolean canDoWrapperColor() {
        return true;
    }



    /**
     * Set the Bins property.
     *
     * @param value The new value for Bins
     */
    public void setBins(int value) {
        bins = value;
    }

    /**
     * Get the Bins property.
     *
     * @return The Bins
     */
    public int getBins() {
        return bins;
    }


    /**
     * Set the Stacked property.
     *
     * @param value The new value for Stacked
     */
    public void setStacked(boolean value) {
        stacked = value;
    }

    /**
     * Get the Stacked property.
     *
     * @return The Stacked
     */
    public boolean getStacked() {
        return stacked;
    }





}

