/*
 * $Id: ReadoutTable.java,v 1.15 2007/04/16 21:32:11 jeffmc Exp $
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
 * @version $Revision: 1.15 $
 */
public class ReadoutTable extends ChartWrapper {


    /** The table */
    private JTable table;

    /** The date label in the gui */
    private JLabel dateLabel;

    /** The top jpanel */
    private JPanel topPanel;


    /** table model */
    private AbstractTableModel tableModel;


    /** List of lists data for the table */
    private List dataList = new ArrayList();

    /** datachoice units */
    private List unitList = new ArrayList();

    /** Should show raw data */
    private boolean showRawData = false;

    /** The way point we are selecting on */
    private WayPoint wayPoint;

    /**
     * Default ctor
     */
    public ReadoutTable() {}


    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public ReadoutTable(String name, List dataChoices) {
        super(name, dataChoices);
    }


    /**
     * Return the human readable name of this chart
     *
     * @return Chart type name
     */
    public String getTypeName() {
        return "Readout Table";
    }


    /**
     * Been removed, do any cleanup.
     */
    public void doRemove() {
        super.doRemove();
        if (wayPoint != null) {
            wayPoint.removePropertyChangeListener(this);
            wayPoint = null;
        }
    }




    /**
     * Create the chart
     */
    private void createChart() {
        if (table != null) {
            return;
        }
        dateLabel  = new JLabel(" ");
        topPanel   = GuiUtils.left(GuiUtils.label("Date: ", dateLabel));

        tableModel = new AbstractTableModel() {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public int getRowCount() {
                return getDataChoiceWrappers().size();
            }

            public int getColumnCount() {
                //Name,value
                return 2;
            }

            public Object getValueAt(int row, int column) {
                if (row >= getDataChoiceWrappers().size()) {
                    return "";
                }
                if (row >= dataList.size()) {
                    return "";
                }
                if (column == 0) {
                    return "<html>"
                           + ((DataChoiceWrapper) getDataChoiceWrappers().get(
                               row)).getName() + " [<b>" + unitList.get(row)
                                   + "</b>]</html>";
                }
                if (column == 1) {
                    double     value = Double.NaN;
                    double[][] d     = (double[][]) dataList.get(row);
                    checkWayPoint();
                    if (wayPoint == null) {
                        value = d[0][0];
                    } else {
                        double time   = wayPoint.getDomainValue();
                        int    length = d[1].length;
                        for (int i = 0; i < length; i++) {
                            if ((time <= d[1][i]) || (i == length - 1)) {
                                value = d[0][i];
                                break;
                            }
                        }

                        if (showRawData) {
                            return new Double(value);
                        } else {
                            DisplayConventions dc =
                                getDisplayControl().getDisplayConventions();
                            return Misc.parseNumber(dc.format(value));
                        }
                    }
                }
                return "";
            }

            public String getColumnName(int column) {
                if (column == 0) {
                    return "Field";
                }
                if (column == 1) {
                    return "Value";
                }

                return "";
            }
        };



        table = new JTable(tableModel);


        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if ( !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                showPopup(table, e.getX(), e.getY());
            }
        });
        table.setToolTipText("Right click to edit");

    }

    /**
     * make the gui
     *
     * @return The gui contents
     */
    protected JComponent doMakeContents() {
        createChart();
        JScrollPane scrollPane = new JScrollPane(table);
        return GuiUtils.topCenter(getDisplayLabel(),
                                  GuiUtils.topCenter(topPanel, scrollPane));
    }


    /**
     * Handle when the waypoint is changed
     */
    private void wayPointChanged() {
        if (table != null) {
            table.repaint();
        }
        if (dateLabel != null) {
            if (wayPoint != null) {
                try {
                    dateLabel.setText(
                        "" + new DateTime(wayPoint.getDomainValue() / 1000.));
                } catch (VisADException ve) {  // shouldn't happen, but...
                    dateLabel.setText(
                        "" + new Date((long) wayPoint.getDomainValue()));
                }
            } else {
                dateLabel.setText(" ");
            }

        }

    }


    /**
     * Handle event
     *
     * @param event The event
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(WayPoint.PROP_WAYPOINTVALUE)) {
            try {
                wayPointChanged();
            } catch (Exception exc) {
                LogUtil.logException("Error creating data set", exc);
            }
            return;
        }
        super.propertyChange(event);
    }


    /**
     * get the menu items
     *
     * @param items List of menu items
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


        if (getDisplayGroup() != null) {
            List comps =
                getDisplayGroup().getAncestorGroup().findDisplayComponents(
                    TimeSeriesChartWrapper.class);
            JMenu selectMenu = null;

            checkWayPoint();


            if (wayPoint != null) {
                items.add(selectMenu = new JMenu("Select Way Point"));
                selectMenu.add(GuiUtils.makeMenuItem("Remove Way Point: "
                        + wayPoint, ReadoutTable.this, "removeWayPoint"));
            }


            for (int i = 0; i < comps.size(); i++) {
                TimeSeriesChartWrapper tscw =
                    (TimeSeriesChartWrapper) comps.get(i);
                if (tscw.getWayPoints().size() == 0) {
                    continue;
                }
                if (selectMenu == null) {
                    items.add(selectMenu = new JMenu("Select Way Point"));
                }
                JMenu timeSeriesMenu = new JMenu(tscw.getName());
                selectMenu.add(timeSeriesMenu);
                for (int wayPointIdx = 0;
                        wayPointIdx < tscw.getWayPoints().size();
                        wayPointIdx++) {
                    WayPoint wayPoint =
                        (WayPoint) tscw.getWayPoints().get(wayPointIdx);
                    timeSeriesMenu.add(
                        GuiUtils.makeMenuItem(
                            "# " + (wayPointIdx + 1) + " "
                            + wayPoint.getName(), ReadoutTable.this,
                                "setWayPoint", wayPoint));
                }
            }
        }

        return super.getPopupMenuItems(items);

    }




    /**
     * Is the waypoint still active. If not then remove it.
     */
    private void checkWayPoint() {
        if (wayPoint != null) {
            if ( !wayPoint.isActive()) {
                removeWayPoint();
            }
        }
    }


    /**
     * Remove the waypoint
     */
    public void removeWayPoint() {
        setWayPoint(null);
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
            List      dataChoices = getDataChoices();
            Hashtable props       = new Hashtable();
            props.put(TrackDataSource.PROP_TRACKTYPE,
                      TrackDataSource.ID_TIMETRACE);
            unitList.clear();
            //            times.clear();
            dataList.clear();
            DisplayConventions dc =
                getDisplayControl().getDisplayConventions();

            for (int paramIdx = 0; paramIdx < dataChoices.size();
                    paramIdx++) {
                DataChoice dataChoice =
                    (DataChoice) dataChoices.get(paramIdx);
                FieldImpl data =
                    getFlatField((FieldImpl) dataChoice.getData(null, props));
                Unit[] units =
                    ucar.visad.Util.getDefaultRangeUnits((FlatField) data);

                double[][] samples  = data.getValues(false);
                double[] timeValues = getTimeValues(samples,
                                          (FlatField) data);
                double[][] results = filterData(samples[0], timeValues);
                double[]   values  = results[0];
                timeValues = results[1];
                unitList.add(units[0]);
                dataList.add(new double[][] {
                    values, timeValues
                });
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
        }
        wayPointChanged();
        tableModel.fireTableStructureChanged();

    }



    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return "Readout Table: " + getName();
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




    /**
     * Set the WayPoint property.
     *
     * @param value The new value for WayPoint
     */
    public void setWayPoint(WayPoint value) {
        if (wayPoint != null) {
            wayPoint.removePropertyChangeListener(this);
        }
        wayPoint = value;
        if (wayPoint != null) {
            wayPoint.addPropertyChangeListener(this);
        }

        wayPointChanged();
    }

    /**
     * Get the WayPoint property.
     *
     * @return The WayPoint
     */
    public WayPoint getWayPoint() {
        return wayPoint;
    }



}

