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


import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.grid.GridUtil;


import ucar.unidata.data.point.*;

import ucar.unidata.gis.SpatialGrid;

import ucar.unidata.ui.TableSorter;
import ucar.unidata.ui.symbol.*;


import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.StringUtil;

import ucar.visad.UtcDate;

import ucar.visad.Util;

import visad.*;

import visad.georef.EarthLocation;

import java.awt.*;
import java.awt.event.*;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import javax.swing.*;
import javax.swing.table.*;



/**
 * A DisplayControl for station models
 *
 * @author MetApps Development Team
 * @version $Revision: 1.58 $
 */

public class ObsListControl extends ObsDisplayControl {

    /** The default fields to use */
    private static final String DEFAULT_FIELDS = PointOb.PARAM_TIME + ","
                                                 + PointOb.PARAM_LAT + ","
                                                 + PointOb.PARAM_LON + ","
                                                 + PointOb.PARAM_ALT;


    /** Holds all data */
    private List dataList;

    /** Holds the column names */
    private List colNames = new ArrayList();

    /** Holds lists which hold the cell values */
    private List tableRows = new ArrayList();


    /** User for synchronizing table things */
    private Object TABLE_MUTEX = new Object();

    /** table for output */
    private JTable obsTable;

    /** table model */
    private AbstractTableModel obsModel;

    /** The sorting table model */
    private TableSorter sorter;

    /** Hashtable to hold the list of param names to text adapter like desc */
    private Hashtable nameToDescription = new Hashtable();

    /** used in save as dialog */
    JCheckBox includeHeaderCbx;

    /** flag for when we are exporting to CSV */
    private boolean exportingToCsv = false;

    /**
     * Default ctor
     */
    public ObsListControl() {
        colString = DEFAULT_FIELDS;
    }


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     *
     * @return  true if successful
     *
     * @throws VisADException  some problem creating a VisAD object
     * @throws RemoteException  some problem creating a remote VisAD object
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {
        setContents(doMakeContents());
        return setData(dataChoice);
    }





    /**
     * This creates the list that is used to show in the table
     *
     * @param dataList List of obs
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void makeDisplay(List dataList)
            throws VisADException, RemoteException {


        MathType[] comps           = null;
        int[]      indices         = null;
        Unit[]     displayUnits    = null;
        List       cols            = null;
        List       tmpColNames     = new ArrayList();
        List       tmpRows         = new ArrayList();


        boolean    doDeclutterTime = getTimeDeclutterEnabled();
        Hashtable  seenObs         = new Hashtable();

        for (int dataIdx = 0; dataIdx < dataList.size(); dataIdx++) {
            FieldImpl data       = (FieldImpl) dataList.get(dataIdx);
            Set       set        = data.getDomainSet();
            int       shapeIndex = 0;
            for (int i = 0; i < set.getLength(); i++) {
                PointOb ob = (PointOb) data.getSample(i, false);
                if (doDeclutterTime) {
                    Object timeKey = getTimeKey(ob);
                    if (seenObs.get(timeKey) != null) {
                        continue;
                    }
                    seenObs.put(timeKey, timeKey);
                }
                Tuple         obData = (Tuple) ob.getData();
                EarthLocation el     = ob.getEarthLocation();
                if (indices == null) {
                    makeNameToDescription(ob);
                    TupleType tt = (TupleType) obData.getType();
                    cols         = getFieldsToShow(tt);
                    comps        = tt.getComponents();
                    indices      = getIndicesToShow(tt);
                    displayUnits = new Unit[obData.getLength()];
                    for (int colIdx = 0; colIdx < indices.length; colIdx++) {
                        String colName = null;
                        if (indices[colIdx] != PointOb.BAD_INDEX) {
                            colName =
                                Util.cleanTypeName(comps[indices[colIdx]]);
                            displayUnits[indices[colIdx]] = getDisplayUnit(
                                (Scalar) obData.getComponent(
                                    indices[colIdx]));
                            String unitString =
                                getDisplayUnitName(
                                    (Scalar) obData.getComponent(
                                        indices[colIdx]));




                            if (unitString != null) {
                                colName = colName + " [" + unitString + "]";
                            }
                        } else {
                            if (cols.get(colIdx).equals(PointOb.PARAM_LAT)) {
                                colName = getParamLabel(PointOb.PARAM_LAT);
                            } else if (cols.get(colIdx).equals(
                                    PointOb.PARAM_LON)) {
                                colName = getParamLabel(PointOb.PARAM_LON);
                            } else if (cols.get(colIdx).equals(
                                    PointOb.PARAM_ALT)) {
                                colName = getParamLabel(PointOb.PARAM_ALT);
                                colName = colName + " ["
                                          + el.getAltitude().getUnit() + "]";
                            } else if (cols.get(colIdx).equals(
                                    PointOb.PARAM_TIME)) {
                                colName = getParamLabel(PointOb.PARAM_TIME);
                            }
                        }
                        if (colName != null) {
                            tmpColNames.add(Util.cleanTypeName(colName));
                        }
                    }
                }
                List rowData = new ArrayList();
                tmpRows.add(rowData);
                for (int colIdx = 0; colIdx < indices.length; colIdx++) {
                    int idx = indices[colIdx];
                    if (idx != PointOb.BAD_INDEX) {
                        rowData.add(new RowData(obData.getComponent(idx),
                                comps[idx], displayUnits[idx]));
                        //                        rowData.add(""+obData.getComponent(idx));
                        //                        rowData.add(getColValue(obData.getComponent(idx),
                        //                                                comps[idx], false));
                    } else {
                        if (cols.get(colIdx).equals(PointOb.PARAM_LAT)) {
                            rowData.add(new RowData(el.getLatitude(), true));
                        } else if (cols.get(colIdx).equals(
                                PointOb.PARAM_LON)) {
                            rowData.add(new RowData(el.getLongitude(), true));
                        } else if (cols.get(colIdx).equals(
                                PointOb.PARAM_ALT)) {
                            rowData.add(new RowData(el.getAltitude(), false));
                        } else if (cols.get(colIdx).equals(
                                PointOb.PARAM_TIME)) {
                            if (getShowDataRaw()) {
                                rowData.add(formatReal(ob.getDateTime()));
                            } else {
                                rowData.add(new RowData(ob.getDateTime()));
                            }
                        }
                    }
                }
            }
        }


        synchronized (TABLE_MUTEX) {
            tableRows = new ArrayList(tmpRows);
            colNames  = new ArrayList(tmpColNames);
            if (obsModel != null) {
                obsModel.fireTableStructureChanged();
            }
        }

    }


    /**
     * Class RowData Holds the data (different types) that is displayed in a table entry
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.58 $
     */
    private class RowData {

        /** Initially null. This gets set the on the first request */
        Object value;

        /** raw data */
        Data data;

        /** type */
        MathType type;

        /** altitude */
        Real alt;

        /** lat or lon */
        Real latLon;

        /** some value */
        Real real;

        /** date */
        DateTime dttm;

        /** The display unit to use */
        Unit displayUnit;

        /**
         * ctor for date
         *
         * @param dttm  date
         */
        public RowData(DateTime dttm) {
            this.dttm = dttm;
        }

        /**
         * ctor for value
         *
         * @param value value
         * @param isLatLon is this value a latlon
         */
        public RowData(Real value, boolean isLatLon) {
            if (isLatLon) {
                latLon = value;
            } else {
                real = value;
            }
        }

        /**
         * ctor for data
         *
         * @param data data
         * @param type type
         * @param displayUnit The display unit to use
         */
        public RowData(Data data, MathType type, Unit displayUnit) {
            this.displayUnit = displayUnit;
            this.data        = data;
            this.type        = type;
        }

        /**
         * get the value. if value is null then convert the raw data to the value.
         *
         * @return the value to show in hte table
         */
        public Object getValue() {
            if (exportingToCsv && (dttm != null)) {
                return UtcDate.formatUtcDate(dttm, UtcDate.DEFAULT_PATTERN);
            }
            if (value == null) {
                if (data != null) {
                    value = getColValue(data, type, false, displayUnit);
                } else if (latLon != null) {
                    value = formatLatLon(latLon);
                } else if (dttm != null) {
                    value = dttm.toString();
                } else if (real != null) {
                    value = formatReal(real);
                }
            }

            return value;
        }

    }

    /** debug_ */
    public static boolean debug = false;

    /**
     * A utility to format the given real. If we are showing data raw then we just return
     * a Double. If not we format it.
     *
     * @param r The real
     *
     * @return The real formatted
     */
    private Object formatReal(Real r) {
        if (getShowDataRaw()) {
            return new Double(r.getValue());
        } else {
            return new RealWrapper(
                getDisplayConventions().format(r.getValue()), r);
        }
    }



    /**
     * A utility to format the given real. If we are showing data raw then we just return
     * a Double. If not we format it.
     *
     * @param r The real
     *
     * @return The real formatted
     */
    private Object formatLatLon(Real r) {
        if (getShowDataRaw()) {
            return new Double(r.getValue());
        } else {
            return new RealWrapper(
                getDisplayConventions().formatLatLon(r.getValue()), r);
        }
    }




    /**
     * Set the data for this instance from the choice supplied.
     *
     * @param choice  <code>DataChoice</code> that describes the data to
     *                be loaded.
     *
     * @return true if load was successful.
     *
     * @throws VisADException  some problem creating a VisAD object
     * @throws RemoteException  some problem creating a remote VisAD object
     * @see DisplayControlImpl#setData(DataChoice)
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.setData(choice)) {
            return false;
        }

        loadData();
        return true;
    }



    /**
     * Make the UI contents for this control.
     *
     * @return  UI container
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {

        obsModel = new AbstractTableModel() {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public int getRowCount() {
                return tableRows.size();
            }

            public int getColumnCount() {
                return colNames.size();
            }

            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {}

            public Object getValueAt(int row, int column) {
                synchronized (TABLE_MUTEX) {
                    if (row < tableRows.size()) {
                        List rowData = (List) tableRows.get(row);
                        if (column < rowData.size()) {
                            Object value = rowData.get(column);
                            if (value instanceof RowData) {
                                value = ((RowData) value).getValue();
                                //if(!exportingToCsv) {
                                //    rowData.set(column, value);
                                //}
                            }
                            if (exportingToCsv) {
                                if (Misc.equals("missing",
                                        value.toString())) {
                                    return "NaN";
                                }
                            }
                            return value;
                        }
                    }
                    return "";
                }
            }

            public String getColumnName(int column) {
                return colNames.get(column).toString();
            }
        };


        sorter = new TableSorter(obsModel) {
            protected Comparator getComparator(int column) {
                synchronized (TABLE_MUTEX) {
                    if (tableRows.size() > 0) {
                        Object obj = ((List) tableRows.get(0)).get(column);
                        if ((obj instanceof Double)
                                || (obj instanceof RealWrapper)) {
                            return COMPARABLE_COMAPRATOR;
                        }
                    }
                    return super.getComparator(column);
                }
            }
        };
        obsTable = new JTable(sorter);
        obsTable.setColumnModel(new DefaultTableColumnModel() {
            //This is a total hack to fix a column array out of bounds exception
            //If we are going to get this error then try every 100 milliseconds
            //until we won't get the error.
            //Try at most 10 times.
            public TableColumn getColumn(int index) {
                int cnt = getColumnCount();
                if (index >= cnt) {
                    int numTries = 0;
                    while ((index >= cnt) && (numTries++ < 10)) {
                        Misc.sleep(100);
                    }
                }
                return super.getColumn(index);
            }
        });
        JTableHeader header = obsTable.getTableHeader();
        header.setToolTipText("Click to sort");
        sorter.setTableHeader(obsTable.getTableHeader());


        int width  = 300;
        int height = 400;
        JScrollPane scroller = GuiUtils.makeScrollPane(obsTable, width,
                                   height);
        scroller.setBorder(BorderFactory.createLoweredBevelBorder());
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));
        JPanel tablePanel = new JPanel();

        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.add(header);
        tablePanel.add(scroller);

        JComponent[] timeDeclutterComps = getTimeDeclutterComps();
        JPanel timeDeclutterPanel =
            GuiUtils.hbox(Misc.newList(timeDeclutterComps[0],
                                       GuiUtils.rLabel("Only show every: "),
                                       timeDeclutterComps[1],
                                       GuiUtils.lLabel(" minutes"),
                                       new JLabel("     ")));
        JPanel topPanel = GuiUtils.leftCenterRight(doMakeFieldSelector(),
                              GuiUtils.left(timeDeclutterPanel),
                              doMakeShowRawSelector());
        //rawCbx);
        return GuiUtils.topCenter(GuiUtils.inset(topPanel, 5), tablePanel);

    }


    /**
     * Handle when the time declutering state has changed
     */
    protected void timeDeclutterChanged() {
        fieldSelectorChanged();
    }



    /**
     * Provide to the base class the type of the obs
     *
     * @return The tuple type
     *
     * @throws RemoteException When bad things happen
     * @throws VisADException When bad things happen
     */
    protected TupleType getTupleType()
            throws RemoteException, VisADException {
        if ((dataList == null) || (dataList.size() == 0)) {
            return null;
        }

        FieldImpl data = (FieldImpl) dataList.get(0);
        Set       set  = data.getDomainSet();
        if (set.getLength() == 0) {
            return null;
        }
        PointOb ob     = (PointOb) data.getSample(0, false);
        Tuple   obData = (Tuple) ob.getData();
        return (TupleType) obData.getType();
    }

    /**
     * Handle when the value in the column field has changed.
     */
    protected void fieldSelectorChanged() {
        Misc.run(new Runnable() {
            public void run() {
                loadData();
            }
        });
    }


    /**
     * Load data into the <code>Displayable</code>.  This is called from
     * {@link #setData(DataChoice)} and whenever the projection changes.
     * Subclasses should override this to do whatever they need to.
     * This implementation uses a
     * {@link ucar.unidata.data.point.PointDataInstance PointDataInstance}
     * to manager the data.
     * @see #doMakeDataInstance(DataChoice)
     */
    protected void loadData() {
        try {
            PointDataInstance pdi = (PointDataInstance) getDataInstance();
            if (pdi == null) {
                return;
            }
            dataList = new ArrayList();
            FieldImpl data = (FieldImpl) pdi.getData();
            if (data == null) {
                return;
            }


            if (GridUtil.isTimeSequence(data)) {
                Set timeSet = data.getDomainSet();
                for (int i = 0; i < timeSet.getLength(); i++) {
                    dataList.add(data.getSample(i, false));
                }
            } else {
                dataList.add(data);
            }
            makeDisplay(dataList);
        } catch (Exception excp) {
            logException("loading data ", excp);
        }
    }



    /**
     * Add the  relevant file menu items into the list
     *
     * @param items List of menu items
     * @param forMenuBar Is this for the menu in the window's menu bar or
     * for a popup menu in the legend
     */
    protected void getSaveMenuItems(List items, boolean forMenuBar) {

        super.getSaveMenuItems(items, forMenuBar);
        items.add(GuiUtils.makeMenuItem("Export Table to CSV...", this,
                                        "exportTable"));

        items.add(GuiUtils.makeMenuItem("Export all data to NetCDF...", this,
                                        "exportAsNetcdf", null, true));
    }



    /**
     * Export table as csv
     */
    public void exportTable() {
        if (includeHeaderCbx == null) {
            includeHeaderCbx = new JCheckBox("Include metadata header", true);
            includeHeaderCbx.setToolTipText(
                "Should the IDV metadata header be included in the CSV file?");
        }
        String filename =
            FileManager.getWriteFile(Misc.newList(FileManager.FILTER_CSV,
                FileManager.FILTER_XLS), FileManager.SUFFIX_CSV,
                                         GuiUtils.top(includeHeaderCbx));
        if (filename == null) {
            return;
        }
        exportingToCsv = true;
        GuiUtils.exportAsCsv(includeHeaderCbx.isSelected()
                             ? makeFileHeader(sorter)
                             : "", sorter, filename);
        exportingToCsv = false;
    }


    /**
     * Make a hashtable of names to descriptions for writing out data
     *
     * @param ob   PointOb to use
     *
     * @throws RemoteException  Problem getting remote data
     * @throws VisADException   Problem accessing data
     */
    private void makeNameToDescription(PointOb ob)
            throws VisADException, RemoteException {
        nameToDescription.clear();
        DateTime dt = ob.getDateTime();
        nameToDescription.put(RealType.Time.getName(), makeDescription(dt));
        EarthLocation el = ob.getEarthLocation();
        nameToDescription.put(RealType.Latitude.getName(),
                              makeDescription(el.getLatitude()));
        nameToDescription.put(RealType.Longitude.getName(),
                              makeDescription(el.getLongitude()));
        nameToDescription.put(RealType.Altitude.getName(),
                              makeDescription(el.getAltitude()));
        Tuple t = (Tuple) ob.getData();
        for (int i = 0; i < t.getDimension(); i++) {
            Scalar s         = (Scalar) t.getComponent(i);
            String cleanName = Util.cleanTypeName(s.getType().prettyString());
            if (nameToDescription.get(cleanName) == null) {
                nameToDescription.put(cleanName, makeDescription(s));
            }
        }

    }

    /**
     * Make a description used for text importing for a particular Scalar
     *
     * @param s   Scalar to describe
     *
     * @return descriptive string
     */
    private String makeDescription(Scalar s) {
        StringBuffer buf  = new StringBuffer();
        String       name = ((ScalarType) s.getType()).getName();
        name = Util.cleanTypeName(name);
        if (s instanceof Text) {
            buf.append(name + ucar.visad.Util.TEXT_IDENTIFIER);
        } else if ((s instanceof DateTime) && !getShowDataRaw()) {
            DateTime d = (DateTime) s;
            buf.append(name);
            buf.append("[fmt=");
            buf.append('"' + DateTime.getFormatPattern() + '"');
            buf.append("]");
        } else {  // Real
            buf.append(name);
            buf.append("[unit=" + '"');
            if (getShowDataRaw()) {
                buf.append(((Real) s).getUnit());
            } else {
                buf.append(getDisplayConventions().getDisplayUnit(name,
                        ((Real) s).getUnit()));
            }
            buf.append('"' + "]");
        }
        return buf.toString();
    }

    /**
     * Make a header for the csv file
     *
     * @param sorter table sorter
     *
     * @return header string
     */
    private String makeFileHeader(TableSorter sorter) {
        StringBuffer buf       = new StringBuffer();
        int          numParams = sorter.getColumnCount();
        String[]     names     = new String[numParams];
        for (int i = 0; i < numParams; i++) {
            names[i] = removeUnitString(sorter.getColumnName(i));
            //    System.err.println("names[i]=" + names[i]);
            String desc = (String) nameToDescription.get(names[i]);
            if (names[i].equals(LABEL_TIME)) {
                names[i] = RealType.Time.getName();
            } else if (desc == null) {
                String key = names[i] + ucar.visad.Util.TEXT_IDENTIFIER;
                String s   = (String) nameToDescription.get(key);
                //    System.err.println("s=" + s);
                if (s != null) {
                    names[i] = key;
                }
            } else {
                //    System.err.println("desc:" + desc);
                if (desc.indexOf(ucar.visad.Util.TEXT_IDENTIFIER) >= 0) {
                    names[i] = names[i] + ucar.visad.Util.TEXT_IDENTIFIER;
                }
            }
        }
        buf.append("(index) -> (");
        buf.append(StringUtil.join(",", names));
        buf.append(")\n");
        for (int i = 0; i < numParams; i++) {
            if ((names[i] == RealType.Time.getName()) && !getShowDataRaw()) {
                StringBuffer buf2 = new StringBuffer(names[i]);
                buf2.append("[fmt=");
                buf2.append('"' + UtcDate.DEFAULT_PATTERN + '"');
                buf2.append("]");
                names[i] = buf2.toString();
            } else {
                ;
                String s = (String) nameToDescription.get(names[i]);
                //      System.err.println("x:" + s);
                if (s != null) {
                    names[i] = s;
                }
            }
        }
        buf.append(StringUtil.join(",", names));
        buf.append("\n");
        return buf.toString();
    }

    /**
     * Remove the unit string from a column header
     *
     * @param name  name of the column
     *
     * @return unit portion stripped off (if present)
     */
    private String removeUnitString(String name) {
        if ((name.indexOf("[") > -1) && (name.indexOf("]") > -1)) {
            return name.substring(0, name.indexOf("[")).trim();
        } else {
            return name;
        }
    }

}
