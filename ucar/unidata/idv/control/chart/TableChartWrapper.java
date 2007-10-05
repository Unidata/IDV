/*
 * $Id: TableChartWrapper.java,v 1.25 2007/04/16 21:32:11 jeffmc Exp $
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


import ucar.unidata.data.DataChoice;

import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.sounding.TrackDataSource;


import ucar.unidata.idv.DisplayConventions;
import ucar.unidata.ui.TableSorter;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;

import visad.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 * Provides a table view
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.25 $
 */
public class TableChartWrapper extends ChartWrapper {


    /** The table */
    private JTable table;

    /** table model */
    private AbstractTableModel tableModel;

    /** The sorting table model */
    private TableSorter sorter;

    /** List of lists data for the table */
    private List data = new ArrayList();

    /** List of times for the table */
    private List times = new ArrayList();

    /** Are we displaying times */
    boolean showTimes = true;

    /** max number of rows */
    private int maxRows = 0;

    /** datachoice units */
    private List unitList = new ArrayList();

    /** Show raw data */
    private boolean showRawData = false;


    /**
     * Default ctor
     */
    public TableChartWrapper() {}


    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public TableChartWrapper(String name, List dataChoices) {
        super(name, dataChoices);
    }





    /**
     * Return the human readable name of this chart
     *
     * @return Chart type name
     */
    public String getTypeName() {
        return "Table";
    }

    /**
     * Create the chart
     */
    private void createChart() {
        if (table != null) {
            return;
        }
        tableModel = new AbstractTableModel() {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public int getRowCount() {
                return maxRows;
            }

            public int getColumnCount() {
                return dataChoiceWrappers.size() + (showTimes
                        ? 1
                        : 0);
            }

            public Object getValueAt(int row, int column) {
                if (showTimes) {
                    if (column == 0) {
                        if (row < times.size()) {
                            return times.get(row);
                        }
                        return "";
                    }
                    column--;
                }
                List dataList = (List) data.get(column);
                if (row < dataList.size()) {
                    return dataList.get(row);
                }
                return new Float(Float.NaN);
            }

            public String getColumnName(int column) {
                if (showTimes) {
                    if (column == 0) {
                        return "Time";
                    }
                    column--;
                }
                if (column < dataChoiceWrappers.size()) {
                    String unitString = "";
                    if (column < unitList.size()) {
                        Unit unit = (Unit) unitList.get(column);
                        if (unit != null) {
                            unitString = " [" + unit + "]";
                        }
                    }
                    return dataChoiceWrappers.get(column).toString()
                           + unitString;
                }
                return "";
            }
        };

        sorter = new TableSorter(tableModel) {
            protected Comparator getComparator(int column) {
                if (showTimes && (column == 0)) {
                    return super.getComparator(column);
                }
                return COMPARABLE_COMAPRATOR;
            }
        };


        table = new JTable(sorter);

        JTableHeader header = table.getTableHeader();
        header.setToolTipText("Click to sort");
        sorter.setTableHeader(table.getTableHeader());

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ( !SwingUtilities.isRightMouseButton(e)) {
                    if ((e.getClickCount() > 1) && showTimes) {
                        int row = table.rowAtPoint(e.getPoint());
                        System.err.println("row:" + row + " " + times.size());
                        if ((row >= 0) && (row < times.size())) {
                            Date dttm = (Date) times.get(row);
                            firePropertyChange(PROP_SELECTEDTIME, null,
                                    new Double(dttm.getTime()));
                        }

                    }
                    return;
                }
                showPopup(table, e.getX(), e.getY());
            }
        });

        table.setToolTipText(
            "Right click to show menu; Double click to set time in other charts");
    }


    /**
     * make the gui
     *
     * @return The gui contents
     */
    protected JComponent doMakeContents() {
        createChart();
        JScrollPane scrollPane = new JScrollPane(table);
        return GuiUtils.topCenter(getDisplayLabel(), scrollPane);
    }


    /**
     * Get the popup menu items
     *
     * @param items menu items list
     *
     * @return The items list
     */
    protected List getPopupMenuItems(List items) {
        final JCheckBoxMenuItem mi = new JCheckBoxMenuItem("Show Raw Data",
                                         showRawData);
        mi.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                setShowRawData(mi.isSelected());
                try {
                    loadData();
                } catch (Exception exc) {
                    LogUtil.logException("Error creating data set", exc);
                }
            }
        });

        items.add(mi);
        items.add(GuiUtils.MENU_SEPARATOR);
        items.add(GuiUtils.makeMenuItem("Export Table...", this,
                                        "exportData"));


        return super.getPopupMenuItems(items);

    }




    /**
     * Export the table data as csv
     */
    public void exportData() {
        GuiUtils.exportAsCsv("", sorter);
    }





    /**
     * Create the charts
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() throws VisADException, RemoteException {
        try {
            createChart();
            List      timeRanges  = getTimeFilterRanges();
            List      dataChoices = getDataChoices();
            Hashtable props       = new Hashtable();
            props.put(TrackDataSource.PROP_TRACKTYPE,
                      TrackDataSource.ID_TIMETRACE);
            unitList.clear();
            times.clear();
            data.clear();
            showTimes = true;
            DisplayConventions dc =
                getDisplayControl().getDisplayConventions();
            maxRows = 0;
            for (int paramIdx = 0; paramIdx < dataChoices.size();
                    paramIdx++) {
                List dataList = new ArrayList();
                List timeList = new ArrayList();
                data.add(dataList);
                DataChoice dataChoice =
                    (DataChoice) dataChoices.get(paramIdx);
                FieldImpl data =
                    getFlatField((FieldImpl) dataChoice.getData(null, props));
                Unit[] units =
                    ucar.visad.Util.getDefaultRangeUnits((FlatField) data);
                unitList.add(units[0]);
                double[][] samples  = data.getValues(false);
                double[] timeValues = getTimeValues(samples,
                                          (FlatField) data);
                double[][] result = filterData(samples[0], timeValues);
                double[]   values = result[0];
                timeValues = result[1];


                for (int i = 0; i < values.length; i++) {
                    if (showRawData) {
                        dataList.add(new Double(values[i]));
                    } else {
                        dataList.add(Misc.parseNumber(dc.format(values[i])));
                    }
                    if (showTimes) {
                        timeList.add(new Date((long) timeValues[i]));
                    }
                }
                maxRows = Math.max(maxRows, dataList.size());
                //If we don't have the same times then don't do times
                if (showTimes) {
                    if (times.size() > 0) {
                        if ( !times.equals(timeList)) {
                            showTimes = false;
                        }
                    } else {
                        times = timeList;
                    }
                }
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
        }
        tableModel.fireTableStructureChanged();
    }


    /**
     * Don't do resolution in the properties dialog
     *
     * @return do resolution in the properties dialog
     */
    protected boolean canDoResolution() {
        return false;
    }


    /**
     * Can this component be a source for time selection events
     *
     * @return true
     */
    protected boolean canBeASourceForTimeSelectionEvents() {
        return true;
    }


    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return "Table Plot: " + getName();
    }

    /**
     *  Set the ShowRawData property.
     *
     *  @param value The new value for ShowRawData
     */
    public void setShowRawData(boolean value) {
        showRawData = value;
    }

    /**
     *  Get the ShowRawData property.
     *
     *  @return The ShowRawData
     */
    public boolean getShowRawData() {
        return showRawData;
    }


}

