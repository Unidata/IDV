/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

/*
 * This was originally from the MamVISAD package and has substantially been modified
 */

package ucar.unidata.idv.control;


import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.ui.TableSorter;

import visad.*;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;

import javax.swing.*;

import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.table.AbstractTableModel;



/**
 * Class FlatFieldTable Displays the data in a flat field in a jtable
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class FlatFieldTable extends JTable {

    /** The table model to use */
    private TableModel model = null;

    /** The flat field we are displaying */
    private FlatField displayedFlatField;

    /** The range data from the flat field */
    private double[][] rangeData;

    /** The lat/lon domain */
    private float[][] domainData;

    /** How many columns for the domain */
    private int numDomainCols;

    /** How many columns in the rannge */
    private int numRangeCols;

    /** The table column names */
    private String[] columnNames;

    /**
     * The ctor
     *
     * @param ff The flat field to use
     * @param showNativeCoordinates Should show native coordinates
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public FlatFieldTable(FlatField ff, boolean showNativeCoordinates)
            throws VisADException, RemoteException {

        rangeData = ff.getValues(false);

        SampledSet ss = GridUtil.getSpatialDomain(ff);

        domainData    = ss.getSamples(true);
        numDomainCols = domainData.length;
        numRangeCols  = rangeData.length;
        columnNames   = new String[numDomainCols + numRangeCols];

        if ((ss.getCoordinateSystem() != null) && !showNativeCoordinates) {
            domainData     = ss.getCoordinateSystem().toReference(domainData);
            columnNames[0] = "Latitude";
            columnNames[1] = "Longitude";
            if (domainData.length > 2) {
                columnNames[2] = "Altitude";
            }
        } else {
            SetType       t     = (SetType) ss.getType();
            RealTupleType rtt   = t.getDomain();
            MathType[]    comps = rtt.getComponents();
            columnNames[0] = ucar.visad.Util.cleanTypeName(comps[0]);
            columnNames[1] = ucar.visad.Util.cleanTypeName(comps[1]);
            if (domainData.length > 2) {
                columnNames[2] = ucar.visad.Util.cleanTypeName(comps[2]);
            }
        }



        RealType[] comps = ((FunctionType) ff.getType()).getRealComponents();
        for (int i = 0; i < comps.length; i++) {
            columnNames[numDomainCols + i] =
                ucar.visad.Util.cleanTypeName(comps[i]) + " ["
                + comps[i].getDefaultUnit() + "]";
        }

        TableSorter sorter = new TableSorter(model = new MyFlatField());
        model = sorter;
        JTableHeader header = getTableHeader();
        header.setToolTipText("Click to sort");
        sorter.setTableHeader(getTableHeader());
        setModel(model);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setPreferredScrollableViewportSize(new Dimension(400, 200));
        getTableHeader().setReorderingAllowed(false);
    }


    /**
     * Class MyFlatField The table model class
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class MyFlatField extends AbstractTableModel {

        /**
         * The ctor
         */
        MyFlatField() {}

        /**
         * number of columns
         *
         * @return number of columns
         */
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * Get number of rows
         *
         * @return Number of rows
         */
        public int getRowCount() {
            return domainData[0].length;
        }

        /**
         * Get name of column
         *
         * @param col column number
         *
         * @return name of column
         */
        public String getColumnName(int col) {
            return columnNames[col];
        }

        /**
         * Get value at column/row
         *
         * @param row the row
         * @param col the column
         *
         * @return the value
         */
        public Object getValueAt(int row, int col) {
            if (col < numDomainCols) {
                return new Float(domainData[col][row]);
            } else {
                return new Double(rangeData[col - numDomainCols][row]);
            }
        }

        /**
         * JTable uses this method to determine the default renderer
         * editor for each cell.
         * @param c  column number
         *
         * @return the Class
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }


    }

}
