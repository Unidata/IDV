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

package ucar.unidata.idv.control;


import ucar.unidata.ui.TableSorter;
import ucar.unidata.util.Misc;

import ucar.visad.Util;
import ucar.visad.quantities.Direction;
import ucar.visad.quantities.PolarHorizontalWind;
import ucar.visad.quantities.Speed;

import visad.*;

import java.awt.Dimension;

import java.rmi.RemoteException;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import javax.swing.event.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;



/**
 * SoundingTable displays the data in a sounding field in a JTable
 *
 * @author IDV Development Team
 */
public class SoundingTable extends JTable {

    /** The table model to use */
    private AbstractTableModel model = null;

    /** The flat field we are displaying */
    private Field[] soundings;

    /** The range data from the flat field */
    private float[][] rangeData;

    /** The domain */
    private float[][] domainData;

    /** How many columns for the domain */
    private int numDomainCols;

    /** How many columns in the rannge */
    private int numRangeCols;

    /** The table column names */
    private String[] columnNames;

    /** sounding index */
    private int currSounding = 0;

    /** show winds as U and V */
    private boolean showUAndV = false;

    /** is the data UV */
    private boolean haveUV = false;

    /** flag for tranforming winds */
    private boolean transformWinds = false;

    /** CoordinateSystem for transforming winds */
    private CoordinateSystem windTransform = null;

    /** units of the input to the cs */
    private Unit[] csUnits;

    /**
     * Create a new sounding table
     * @param display  the associated DisplayControl
     */
    public SoundingTable(DisplayControlImpl display) {}

    /**
     * Set the soundings shown in this table
     *
     * @param soundings  the array of soundings
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  problem dissecting data
     */
    public void setSoundings(Field[] soundings)
            throws VisADException, RemoteException {
        this.soundings = soundings;
        if (soundings != null) {
            setupTable(soundings[0]);
            setSounding(soundings[currSounding]);
        }
    }

    /**
     * Set the current sounding to be displayed
     *
     * @param index  the index to set
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  problem dissecting data
     */
    public void setCurrentSounding(int index)
            throws VisADException, RemoteException {
        // should we throw an error?
        if ((soundings == null) || (index < 0)
                || (index >= soundings.length)) {
            return;
        }
        currSounding = index;
        setSounding(soundings[currSounding]);
    }

    /**
     * Set the sounding in the table
     *
     * @param sounding  the sounding
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  problem dissecting data
     */
    private void setSounding(Field sounding)
            throws VisADException, RemoteException {
        domainData = null;

        // domain values
        Set              domain     = sounding.getDomainSet();
        CoordinateSystem cs         = domain.getCoordinateSystem();

        float[][]        domSamples = domain.getSamples(false);
        if ((cs != null)) {
            float[][] refData = cs.toReference(Set.copyFloats(domSamples));
            domainData = new float[][] {
                domSamples[0], refData[0]
            };
        }
        // range values
        RealType[] rangeComps =
            ((FunctionType) sounding.getType()).getRealComponents();
        rangeData = sounding.getFloats(false);

        // wind
        if (rangeComps.length > 2) {
            transformWinds = (showUAndV && !haveUV)
                             || ( !showUAndV && haveUV);
            if ( !transformWinds) {
                for (int i = 2; i < 4; i++) {
                    columnNames[numDomainCols + i] =
                        makeColumnName(rangeComps[i],
                                       rangeComps[i].getDefaultUnit());
                }
            } else {
                RealTupleType refType  = windTransform.getReference();
                Unit[]        refUnits = windTransform.getReferenceUnits();
                for (int i = 0; i < 2; i++) {
                    columnNames[numDomainCols + i + 2] =
                        makeColumnName((RealType) refType.getComponent(i),
                                       refUnits[i]);
                }
                float[][] newVals =
                    windTransform.toReference(Set.copyFloats(new float[][] {
                    rangeData[2], rangeData[3]
                }));
                rangeData[2] = newVals[0];
                rangeData[3] = newVals[1];
            }
        }
        model.fireTableStructureChanged();
    }

    /**
     * Set the sounding in the table
     *
     * @param sounding  the sounding
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException  problem dissecting data
     */
    private void setupTable(Field sounding)
            throws VisADException, RemoteException {

        Set              domain = sounding.getDomainSet();
        CoordinateSystem cs     = domain.getCoordinateSystem();

        numDomainCols = domain.getDimension();
        if (cs != null) {
            numDomainCols++;
        }
        RealType[] rangeComps =
            ((FunctionType) sounding.getType()).getRealComponents();
        numRangeCols = rangeComps.length;
        columnNames  = new String[numDomainCols + numRangeCols];

        SetType       t     = (SetType) domain.getType();
        Unit[]        units = domain.getSetUnits();
        RealTupleType rtt   = t.getDomain();
        RealType[]    comps = rtt.getRealComponents();
        columnNames[0] = makeColumnName(comps[0], units[0]);
        if ((cs != null)) {
            RealTupleType refType  = cs.getReference();
            RealType[]    refComps = refType.getRealComponents();
            Unit[]        refUnits = cs.getReferenceUnits();
            columnNames[1] = makeColumnName(refComps[0], refUnits[0]);
        }

        // set for default
        for (int i = 0; i < rangeComps.length; i++) {
            columnNames[numDomainCols + i] = makeColumnName(rangeComps[i],
                    rangeComps[i].getDefaultUnit());
        }
        // wind
        if (rangeComps.length > 2) {
            csUnits = new Unit[] { rangeComps[2].getDefaultUnit(),
                                   rangeComps[3].getDefaultUnit() };
            haveUV = (Unit.canConvert(csUnits[0], CommonUnit.meterPerSecond)
                      && Unit.canConvert(csUnits[1],
                                         CommonUnit.meterPerSecond));
            if (haveUV) {
                windTransform =
                    new InverseCoordinateSystem(
                        new RealTupleType(
                            Speed.getRealType(),
                            Direction.getRealType()), new PolarHorizontalWind
                                .PolarCoordinateSystem(
                                    new RealTupleType(
                                        rangeComps[2],
                                        rangeComps[3]), CommonUnit
                                            .meterPerSecond, CommonUnit
                                            .degree));
            } else {
                windTransform =
                    new PolarHorizontalWind.PolarCoordinateSystem(csUnits[0],
                        csUnits[1]);
            }
        }
        if (model == null) {
            model = new SoundingTableModel();
            TableSorter sorter = new TableSorter(model);
            model = sorter;
            JTableHeader header = getTableHeader();
            header.setToolTipText("Click to sort");
            sorter.setTableHeader(getTableHeader());
            setModel(model);
            setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            setPreferredScrollableViewportSize(new Dimension(400, 200));
            getTableHeader().setReorderingAllowed(false);
        }
    }

    /**
     * Make a column header from the RealType and Unit
     * @param rt     the RealType
     * @param unit   the Unit
     * @return column name
     */
    private String makeColumnName(RealType rt, Unit unit) {
        return Util.cleanTypeName(rt) + " [" + unit + "]";
    }

    /**
     * Set a flag to show the wind values as U and V
     * @param showUV  true to show winds as U and V
     */
    public void setShowUVComps(boolean showUV) {
        showUAndV = showUV;
        try {
            setCurrentSounding(currSounding);
        } catch (Exception ignore) {}
    }

    /**
     * The sounding table model class
     *
     * @author IDV Development Team
     */
    public class SoundingTableModel extends AbstractTableModel {

        /**
         * The ctor
         */
        SoundingTableModel() {}

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
            if (domainData == null) {
                return 0;
            }
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
                return new Float(rangeData[col - numDomainCols][row]);
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
