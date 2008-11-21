/*
 * 
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
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
 * Class FlatFieldTable _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class FlatFieldTable extends JTable {

    /** _more_          */
    private TableModel model = null;

    /** _more_          */
    private FlatField displayedFlatField;

    /** _more_          */
    private double[][] rangeData;

    /** _more_          */
    private float[][] domainData;

    /** _more_          */
    private int numDomainCols;

    /** _more_          */
    private int numRangeCols;

    /** _more_          */
    private String[] columnNames;

    /**
     * _more_
     *
     * @param ff _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public FlatFieldTable(FlatField ff, boolean showNativeCoordinates)
            throws VisADException, RemoteException {

        rangeData      = ff.getValues(false);

        SampledSet ss        = GridUtil.getSpatialDomain(ff);
        
        domainData = ss.getSamples(true);
        numDomainCols  = domainData.length;
        numRangeCols   = rangeData.length;
        columnNames    = new String[numDomainCols + numRangeCols];

        if (ss.getCoordinateSystem() != null && !showNativeCoordinates) {
            domainData = ss.getCoordinateSystem().toReference(domainData);
            columnNames[0] = "Latitude";
            columnNames[1] = "Longitude";
            if (domainData.length > 2) {
                columnNames[2] = "Altitude";
            }
        }  else {
            SetType t = (SetType)ss.getType();
            RealTupleType rtt = t.getDomain();
            MathType[] comps = rtt.getComponents();
            columnNames[0] = ucar.visad.Util.cleanTypeName(comps[0]);
            columnNames[1] = ucar.visad.Util.cleanTypeName(comps[1]);
            if (domainData.length > 2) {
                columnNames[2] = ucar.visad.Util.cleanTypeName(comps[2]);
            }
        }



        RealType[] comps = ((FunctionType) ff.getType()).getRealComponents();
        for (int i = 0; i < comps.length; i++) {
            columnNames[numDomainCols + i] =
                ucar.visad.Util.cleanTypeName(comps[i]) +" [" + comps[i].getDefaultUnit()+"]";
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
     * Class MyFlatField _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class MyFlatField extends AbstractTableModel {

        /**
         * _more_
         */
        MyFlatField() {}

        /**
         * _more_
         *
         * @return _more_
         */
        public int getColumnCount() {
            return columnNames.length;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getRowCount() {
            return domainData[0].length;
        }

        /**
         * _more_
         *
         * @param col _more_
         *
         * @return _more_
         */
        public String getColumnName(int col) {
            return columnNames[col];
        }

        /**
         * _more_
         *
         * @param row _more_
         * @param col _more_
         *
         * @return _more_
         */
        public Object getValueAt(int row, int col) {
            if (col < numDomainCols) {
                return new Float(domainData[col][row]);
            } else {
                return new Double(rangeData[col - numDomainCols][row]);
            }
        }


    }

}

