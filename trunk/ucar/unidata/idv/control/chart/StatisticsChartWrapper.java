/*
 * $Id: StatisticsChartWrapper.java,v 1.25 2007/04/16 21:32:11 jeffmc Exp $
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



import org.python.core.*;
import org.python.util.*;

import ucar.unidata.data.DataChoice;


import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.data.sounding.TrackDataSource;


import ucar.unidata.idv.DisplayConventions;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;

import visad.*;


import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import java.rmi.RemoteException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;



/**
 * Provides a table view
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.25 $
 */
public class StatisticsChartWrapper extends ChartWrapper {



    /** the table_ */
    private JTable table;



    /** List of jython expressions to create the table values */
    private List expressions =
        Misc.newList(
            "'<html>'+name + ' [<b>' + unit.toString()+'</b>]'+'</html>'",
            "values.__len__()", "str(min)+'/'+str(max)+'/'+str(average)");

    /** List of column labels */
    private List labels = Misc.newList("Name", "# Points", "Min/Max/Avg");



    /** table model */
    private AbstractTableModel tableModel;

    /** List of Stats for the table */
    private List stats = new ArrayList();

    /** For properties */
    private JTextArea expressionText;

    /** For properties */
    private JTextArea labelText;


    /**
     * Default ctor
     */
    public StatisticsChartWrapper() {}

    /**
     * Ctor
     *
     * @param name The name
     * @param dataChoices List of data choices
     */
    public StatisticsChartWrapper(String name, List dataChoices) {
        super(name, dataChoices);
    }


    /**
     * Return the human readable name of this chart
     *
     * @return Chart type name
     */
    public String getTypeName() {
        return "Statistics Table";
    }


    /**
     * Class Stat holds stats
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.25 $
     */
    private static class Stat {

        /** The values */
        List values = new ArrayList();

        /** stats */
        Integer points;

        /** stats */
        Unit unit;

        /** stats */
        Double min;

        /** stats */
        Double max;

        /** stats */
        Double average;
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
                return dataChoiceWrappers.size();
            }

            public int getColumnCount() {
                return labels.size();
            }


            public Object getValueAt(int row, int column) {
                if ((row >= dataChoiceWrappers.size())
                        || (row >= stats.size())) {
                    return "";
                }
                Stat stat = (Stat) stats.get(row);
                if ((column >= 0) && (column < stat.values.size())) {
                    return stat.values.get(column);
                }
                return "";
            }

            public String getColumnName(int column) {
                if ((column >= 0) && (column < labels.size())) {
                    return (String) labels.get(column);
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
     * Make the gui
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {
        createChart();
        JScrollPane scrollPane = new JScrollPane(table);
        return GuiUtils.topCenter(getDisplayLabel(), scrollPane);
    }



    /**
     * Create the charts
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void loadData() throws VisADException, RemoteException {
        createChart();
        List dataChoices = getDataChoices();
        try {

            Hashtable props = new Hashtable();
            props.put(TrackDataSource.PROP_TRACKTYPE,
                      TrackDataSource.ID_TIMETRACE);
            stats.clear();
            DisplayConventions dc =
                getDisplayControl().getDisplayConventions();
            for (int paramIdx = 0; paramIdx < dataChoices.size();
                    paramIdx++) {
                DataChoice dataChoice =
                    (DataChoice) dataChoices.get(paramIdx);
                FieldImpl data =
                    getFlatField((FieldImpl) dataChoice.getData(null, props));
                Stat stat = new Stat();
                stats.add(stat);
                Unit[] units =
                    ucar.visad.Util.getDefaultRangeUnits((FlatField) data);

                Range[]    ranges   = GridUtil.getMinMax(data);
                double[][] samples  = data.getValues(false);

                double[] timeValues = getTimeValues(samples,
                                          (FlatField) data);
                double[][] results = filterData(samples[0], timeValues);
                double[]   values  = results[0];
                timeValues = results[1];

                Date[] timeArray = new Date[timeValues.length];
                for (int i = 0; i < timeValues.length; i++) {
                    timeArray[i] = new Date((long) timeValues[i]);
                }

                stat.min = Misc.parseNumber(dc.format(ranges[0].getMin()));
                stat.max = Misc.parseNumber(dc.format(ranges[0].getMax()));
                double total = 0;
                for (int i = 0; i < values.length; i++) {
                    total += values[i];
                }
                if (values.length > 0) {
                    stat.average = Misc.parseNumber(dc.format(total
                            / values.length));
                }

                stat.points = new Integer(values.length);
                stat.unit   = units[0];
                stat.values.clear();
                PythonInterpreter interpreter = getInterpreter();
                interpreter.set("field", data);
                interpreter.set("max", stat.max);
                interpreter.set("min", stat.min);
                interpreter.set("average", stat.average);
                interpreter.set("values", values);
                interpreter.set("times", timeArray);
                interpreter.set("name", dataChoice.getName());
                interpreter.set("description", dataChoice.getDescription());
                interpreter.set("unit", stat.unit);
                for (int i = 0; i < expressions.size(); i++) {
                    PyObject pyResult =
                        interpreter.eval((String) expressions.get(i));
                    Object resultObject = pyResult.__tojava__(Object.class);
                    stat.values.add(resultObject);
                }
            }
        } catch (Exception exc) {
            LogUtil.logException("Error creating data set", exc);
        }
        tableModel.fireTableStructureChanged();
    }

    /**
     * Apply properties
     *
     *
     * @return Was successful
     */


    protected boolean applyProperties() {
        boolean result = super.applyProperties();
        if ( !result) {
            return false;
        }

        System.err.println("text:" + expressionText.getText());
        List labelLines = StringUtil.split(labelText.getText(), "\n", true,
                                           true);
        List expressionLines = StringUtil.split(expressionText.getText(),
                                   "\n", true, true);
        if (labelLines.size() != expressionLines.size()) {
            LogUtil.userErrorMessage(
                "There must be the same number of labels as expressions");
            return false;
        }



        if ( !(Misc.equals(labelLines, labels)
                && Misc.equals(expressionLines, expressions))) {
            labels      = labelLines;
            expressions = expressionLines;
            try {
                loadData();
            } catch (Exception exc) {
                LogUtil.logException("Error loading data", exc);
                return false;
            }
        }
        return result;
    }


    /**
     * return the array of tab names for the proeprties dialog
     *
     * @return array of tab names
     */
    public String[] getPropertyTabs() {
        return new String[] { "Data", "Columns" };
    }



    /**
     * add to the properties gui
     *
     * @param comps  List of components
     * @param tabIdx which tab
     */
    protected void getPropertiesComponents(List comps, int tabIdx) {
        super.getPropertiesComponents(comps, tabIdx);
        if (tabIdx != 1) {
            return;
        }
        StringBuffer expSB   = new StringBuffer();
        StringBuffer labelSB = new StringBuffer();
        for (int i = 0; i < labels.size(); i++) {
            labelSB.append(labels.get(i) + "\n");
            expSB.append(expressions.get(i) + "\n");
        }

        labelText      = new JTextArea(labelSB.toString(), 10, 10);
        expressionText = new JTextArea(expSB.toString(), 10, 40);
        expressionText.setToolTipText(
            "<html>Available fields:<ul><li>name (field short name)<li>description (field long name)<li>unit (value unit)<li>values (array of float values)<li>times (array of Date values)<li>max<li>min<li>average</ul></html>");



        JScrollPane sp1 =
            new JScrollPane(
                labelText, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);


        JScrollPane sp2 =
            new JScrollPane(
                expressionText,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JPanel textPanel = GuiUtils.doLayout(new Component[] {
                               GuiUtils.topCenter(GuiUtils.cLabel("Labels"),
                                   sp1),
                               GuiUtils.topCenter(
                                   GuiUtils.cLabel("Expressions"), sp2) }, 2,
                                       new double[] { 1,
                3 }, GuiUtils.WT_Y);
        comps.add(GuiUtils.top(GuiUtils.rLabel("Columns: ")));
        comps.add(textPanel);
    }


    /**
     * Don't show resolution property
     *
     * @return show resolution property
     */
    protected boolean canDoResolution() {
        return false;
    }




    /**
     * to string
     *
     * @return string
     */
    public String toString() {
        return "Statistics Plot: " + getName();
    }


    /**
     * Set the Expressions property.
     *
     * @param value The new value for Expressions
     */
    public void setExpressions(List value) {
        expressions = value;
    }

    /**
     * Get the Expressions property.
     *
     * @return The Expressions
     */
    public List getExpressions() {
        return expressions;
    }

    /**
     * Set the Labels property.
     *
     * @param value The new value for Labels
     */
    public void setLabels(List value) {
        labels = value;
    }

    /**
     * Get the Labels property.
     *
     * @return The Labels
     */
    public List getLabels() {
        return labels;
    }




}

