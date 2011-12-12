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


import ucar.unidata.data.BadDataException;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.gis.ShapeFileDataSource;
import ucar.unidata.data.grid.GridUtil;

import ucar.unidata.geoloc.projection.*;

import ucar.unidata.gis.shapefile.*;
import ucar.unidata.ui.PropertyFilter;

import ucar.unidata.ui.TableSorter;
import ucar.unidata.ui.TwoListPanel;

import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;

import ucar.visad.data.MapSet;

import ucar.visad.display.LineDrawing;



import visad.*;

import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;

import java.awt.*;
import java.awt.event.*;

import java.awt.geom.Rectangle2D;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;



import javax.swing.*;
import javax.swing.event.*;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.table.*;



/**
 * A MetApps Display Control with Displayable and controls for
 * one 3D isosurface display of one parameter.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.25 $
 */

public class ShapefileControl extends DisplayControlImpl {

    /** The GUI used to show and edit the filters */
    private PropertyFilter.FilterGui filterGui;

    /** Are the filters enabled */
    private boolean filtersEnabled = true;

    /** Should all filters be matched */
    private boolean matchAll = true;

    /** filters for showing data */
    protected List filters = new ArrayList();

    /** All of the field names */
    protected List allFields = new ArrayList();

    /** Field names to show in the table */
    protected List selectedFields = new ArrayList();

    /** Field names to show in the table */
    protected List uniqueFields = new ArrayList();


    /** Used in the field selector */
    private TwoListPanel fieldSelectorPanel;

    /** Used in the field selector */
    private TwoListPanel fieldUniquePanel;

    /** The dialog window that shows the field selector panel */
    private JDialog selectorWindow;

    /** The dialog window that shows the field selector panel */
    private JDialog uniqueWindow;

    /** This is the original, unfiltered data */
    private Data mainData;

    /** the display for the lines */
    LineDrawing myDisplay;

    /** width for the lines */
    int lineWidth = 1;

    /** The db file. May be null if there was no dbf file */
    private DbaseFile dbFile;

    /** The field names */
    private String[] fieldNames;

    /** The map sets */
    private List mapSets;

    /** ??? */
    private boolean hasProperties = false;

    /** Does each row pass through the filters. */
    boolean[] passTheFilter;


    /** Holds lists which hold the cell values */
    private List tableCols = new ArrayList();

    /** the visible rows */
    private List visibleRows = new ArrayList();

    /** The column names in the table */
    private List colNames = new ArrayList();


    /** table for output */
    private JTable dbTable;

    /** User for synchronizing table things */
    private Object TABLE_MUTEX = new Object();

    /** table model */
    private AbstractTableModel dbModel;

    /** The sorting table model */
    private TableSorter sorter;


    /**
     * Create a new ShapefileControl; set the attributes
     */
    public ShapefileControl() {
        setAttributeFlags(FLAG_COLOR | FLAG_DATACONTROL);
    }


    /**
     * Call to help make this kind of Display Control; also calls code to
     * made the Displayable (empty of data thus far).
     * This method is called from inside DisplayControlImpl.init(several args).
     *
     * @param dataChoice the DataChoice of the moment.
     * @return   true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public boolean init(DataChoice dataChoice)
            throws VisADException, RemoteException {

        myDisplay = new LineDrawing("Map " + dataChoice);
        addDisplayable(myDisplay, FLAG_COLOR | FLAG_ZPOSITION);

        // Make the gui contents first because we need them in setData
        // doMakeContents calls getControlWidgets for which see below).
        boolean ok = setData(dataChoice);
        if (ok) {
            setContents(doMakeContents());
        }
        return ok;
    }

    /**
     * Make the gui
     *
     * @return The gui
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    protected Container doMakeContents()
            throws VisADException, RemoteException {
        JComponent mainContents = (JComponent) super.doMakeContents();
        if ( !hasProperties) {
            return GuiUtils.left(GuiUtils.top(mainContents));
        }
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Display", GuiUtils.topLeft(mainContents));
        tabbedPane.add("Filters", makeFilterGui());
        tabbedPane.add("Table", doMakeTable());
        return GuiUtils.left(GuiUtils.top(tabbedPane));
    }

    /**
     * Process the field selection
     *
     * @param ae The event
     */
    private void handleFieldSelectorEvent(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_APPLY) || cmd.equals(GuiUtils.CMD_OK)) {
            selectedFields =
                new ArrayList(fieldSelectorPanel.getCurrentEntries());
            populateTable();
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL) || cmd.equals(GuiUtils.CMD_OK)) {
            selectorWindow.setVisible(false);
        }
    }


    /**
     * Process the field unique selection
     *
     * @param ae The event
     */
    private void handleFieldUniqueEvent(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_APPLY) || cmd.equals(GuiUtils.CMD_OK)) {
            uniqueFields =
                new ArrayList(fieldUniquePanel.getCurrentEntries());
            populateTable();
        }
        if (cmd.equals(GuiUtils.CMD_CANCEL) || cmd.equals(GuiUtils.CMD_OK)) {
            uniqueWindow.setVisible(false);
        }
    }


    /**
     * Show the field selector window
     */
    public void showFieldSelector() {
        if (selectorWindow == null) {
            fieldSelectorPanel = new TwoListPanel(new ArrayList(),
                    "All Fields", new ArrayList(), "Current Fields", null);

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    handleFieldSelectorEvent(ae);
                }
            };
            JPanel buttons = GuiUtils.makeButtons(listener,
                                 new String[] { GuiUtils.CMD_APPLY,
                    GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL });

            JPanel contents = GuiUtils.centerBottom(fieldSelectorPanel,
                                  buttons);
            selectorWindow = GuiUtils.createDialog(null, "Field Selector",
                    false);
            selectorWindow.getContentPane().add(contents);
            selectorWindow.pack();
        }

        if (selectedFields.size() == 0) {
            selectedFields = new ArrayList(allFields);
        }
        fieldSelectorPanel.reinitialize(allFields, selectedFields);
        selectorWindow.setVisible(true);

    }

    /**
     * Signal base class to add this as a display listener
     *
     * @return Add as display listener
     */
    protected boolean shouldAddDisplayListener() {
        return true;
    }


    /**
     * Show the field selector window
     */
    public void showUniqueSelector() {
        if (uniqueWindow == null) {
            fieldUniquePanel = new TwoListPanel(new ArrayList(),
                    "All Fields", new ArrayList(), "Current Fields", null);

            ActionListener listener = new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    handleFieldUniqueEvent(ae);
                }
            };
            JPanel buttons = GuiUtils.makeButtons(listener,
                                 new String[] { GuiUtils.CMD_APPLY,
                    GuiUtils.CMD_OK, GuiUtils.CMD_CANCEL });

            JPanel contents = GuiUtils.centerBottom(fieldUniquePanel,
                                  buttons);
            uniqueWindow = GuiUtils.createDialog(null,
                    "Unique Field Selector", false);
            uniqueWindow.getContentPane().add(contents);
            uniqueWindow.pack();
        }


        fieldUniquePanel.reinitialize(allFields, uniqueFields);
        uniqueWindow.setVisible(true);

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
        List l = new ArrayList();
        if (hasProperties) {
            l.add(GuiUtils.makeMenuItem("Export Table...", this,
                                        "exportTable"));
        }
        if (mainData instanceof SampledSet) {
            l.add(GuiUtils.makeMenuItem("Export Displayed Shapes...", this,
                                        "exportShapes"));
        }
        if ( !l.isEmpty()) {
            items.addAll(l);
        }

    }

    /**
     * Export the table as csv
     */
    public void exportTable() {
        GuiUtils.exportAsCsv(sorter);
    }



    /**
     * Fill the table
     */
    private void populateTable() {
        if ((mapSets == null) || !hasProperties || (dbModel == null)) {
            return;
        }
        visibleRows = new ArrayList();
        tableCols   = new ArrayList();
        colNames    = new ArrayList();
        int       numRecords = mapSets.size();
        List      comps      = new ArrayList();
        boolean[] unique     = null;
        if (uniqueFields.size() > 0) {
            unique = new boolean[numRecords];
            Arrays.fill(unique, true);
            for (int fieldIdx = 0; fieldIdx < fieldNames.length; fieldIdx++) {
                String fieldName = fieldNames[fieldIdx];
                if ( !uniqueFields.contains(fieldName)
                        || ((selectedFields.size() > 0)
                            && !selectedFields.contains(fieldName))) {
                    continue;
                }
                DbaseData dbData    = dbFile.getField(fieldIdx);
                Hashtable uniqueMap = new Hashtable();
                List      values    = dbData.asList();
                for (int valueIdx = 0; valueIdx < values.size(); valueIdx++) {
                    if ( !unique[valueIdx] || !passTheFilter[valueIdx]) {
                        continue;
                    }
                    Object v = values.get(valueIdx);
                    if (uniqueMap.get(v) != null) {
                        unique[valueIdx] = false;
                    } else {
                        uniqueMap.put(v, v);
                    }
                }
            }
        }


        for (int fieldIdx = 0; fieldIdx < fieldNames.length; fieldIdx++) {
            String fieldName = fieldNames[fieldIdx];
            if ((selectedFields.size() > 0)
                    && !selectedFields.contains(fieldName)) {
                continue;
            }
            List data = new ArrayList();
            tableCols.add(data);
            colNames.add(fieldName);
            DbaseData dbData = dbFile.getField(fieldIdx);
            List      values = dbData.asList();
            for (int valueIdx = 0; valueIdx < values.size(); valueIdx++) {
                if ( !passTheFilter[valueIdx]) {
                    continue;
                }
                if ((unique != null) && !unique[valueIdx]) {
                    continue;
                }
                data.add(values.get(valueIdx));
            }
        }
        dbModel.fireTableStructureChanged();
    }



    /**
     * Make the table gui
     *
     * @return The  table gui
     */
    private JComponent doMakeTable() {

        List comps = new ArrayList();
        for (int fieldIdx = 0; fieldIdx < fieldNames.length; fieldIdx++) {
            allFields.add(fieldNames[fieldIdx]);
        }


        dbModel = new AbstractTableModel() {
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            public int getRowCount() {
                if (tableCols.size() > 0) {
                    List colData = (List) tableCols.get(0);
                    return colData.size();
                }
                return 0;
            }

            public int getColumnCount() {
                return colNames.size();
            }

            public void setValueAt(Object aValue, int rowIndex,
                                   int columnIndex) {}

            public Object getValueAt(int row, int column) {
                synchronized (TABLE_MUTEX) {
                    if (column < tableCols.size()) {
                        List colData = (List) tableCols.get(column);
                        if (row < colData.size()) {
                            return colData.get(row);
                        }
                    }
                    return "";
                }
            }

            public String getColumnName(int column) {
                return (String) colNames.get(column);
            }
        };


        sorter = new TableSorter(dbModel) {
            protected Comparator getComparator(int column) {
                synchronized (TABLE_MUTEX) {
                    if (dbModel.getRowCount() > 0) {
                        List rowData = (List) tableCols.get(0);
                        if (rowData.get(column) instanceof Double) {
                            return COMPARABLE_COMAPRATOR;
                        }
                    }
                    return super.getComparator(column);
                }
            }
        };


        dbTable = new JTable(sorter);
        JTableHeader header = dbTable.getTableHeader();
        sorter.setTableHeader(dbTable.getTableHeader());



        int         width    = 300;
        int         height   = 400;
        JScrollPane scroller = GuiUtils.makeScrollPane(dbTable, width,
                                   height);
        scroller.setPreferredSize(new Dimension(width, height));
        scroller.setMinimumSize(new Dimension(width, height));

        JPanel tablePanel = new JPanel();
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.add(header);
        tablePanel.add(scroller);
        populateTable();
        JPanel buttons =
            GuiUtils.left(
                GuiUtils.hbox(
                    GuiUtils.makeButton(
                        "Select Fields to Show", this,
                        "showFieldSelector"), GuiUtils.makeButton(
                            "Select Unique Fields", this,
                            "showUniqueSelector")));
        return GuiUtils.topCenter(buttons, tablePanel);

    }




    /**
     * Write into the buffer the xml of the points
     *
     * @param set The data that holds the points
     * @param sb Buffer to write to
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void getShapesXml(SampledSet set, StringBuffer sb)
            throws VisADException, RemoteException {
        if (set instanceof UnionSet) {
            SampledSet[] sets = ((UnionSet) set).getSets();
            for (int setIdx = 0; setIdx < sets.length; setIdx++) {
                getShapesXml(sets[setIdx], sb);
            }
        } else {
            sb.append("<polygon points=\"");
            float[][] samples = set.getSamples(false);
            for (int ptIdx = 0; ptIdx < samples[0].length; ptIdx++) {
                if (ptIdx > 0) {
                    sb.append(",");
                }
                sb.append("" + samples[1][ptIdx]);
                sb.append("," + samples[0][ptIdx]);
            }
            sb.append("\"/>\n");
        }
    }

    /**
     * Export the displayed shapes as our own xml format.
     */
    public void exportShapes() {
        // TODO: handle indexed files
        String filename = FileManager.getWriteFile(FileManager.FILTER_XML,
                              FileManager.SUFFIX_XML);
        if (filename == null) {
            return;
        }
        try {
            StringBuffer sb      = new StringBuffer("<shapes>\n");
            SampledSet   theData = (SampledSet) applyFilters(mainData);
            getShapesXml(theData, sb);
            sb.append("</shapes>\n");
            IOUtil.writeFile(filename, sb.toString());
        } catch (Exception exc) {
            logException("Exporting shapes", exc);
        }
    }



    /**
     * Make the filter GUI
     *
     * @return The filter gui
     */
    private JComponent makeFilterGui() {
        List filterNames = new ArrayList();
        for (int fieldIdx = 0; fieldIdx < fieldNames.length; fieldIdx++) {
            filterNames.add(fieldNames[fieldIdx]);
        }
        filterGui = new PropertyFilter.FilterGui(filters, filterNames,
                filtersEnabled, matchAll);
        return GuiUtils.topCenter(
            GuiUtils.left(
                GuiUtils.makeButton(
                    "Apply Filters", this,
                    "handleFilterApply")), filterGui.getContents());
    }


    /**
     * Handle the apply filter button
     */
    public void handleFilterApply() {
        try {
            loadData();
        } catch (Exception exc) {
            logException("Setting filters", exc);
        }
    }

    /**
     * Filter out any polygons
     *
     * @param data The original data
     *
     * @return Return the new set
     */
    private Data applyFilters(Data data) {
        if ( !hasProperties) {
            return data;
        }

        Data d = data;
        if (passTheFilter != null) {
            Arrays.fill(passTheFilter, true);
        }
        if ((filters.size() == 0) || !filtersEnabled) {
            return data;
        }
        if (data instanceof FieldImpl) {        // (index->set)
            try {
                FieldImpl fi = (FieldImpl) data;
                if (GridUtil.isSequence(fi)) {  // 1D domain
                    Set domainSet = fi.getDomainSet();
                    FieldImpl newFI =
                        new FieldImpl((FunctionType) fi.getType(), domainSet);
                    for (int i = 0; i < domainSet.getLength(); i++) {
                        SampledSet s =
                            applyFilters((SampledSet) fi.getSample(i, false));
                        if (s != null) {
                            newFI.setSample(i, s, false);
                        }
                    }
                    d = newFI;
                } else {
                    throw new BadDataException("Can't handle data of type: "
                            + fi.getType());
                }
            } catch (Exception exc) {
                logException("Applying flters", exc);
            }
        } else if (data instanceof SampledSet) {
            d = (Data) applyFilters((SampledSet) data);
        }
        return d;
    }

    /**
     * Filter out any polygons
     *
     * @param data The original data
     *
     * @return Return the new set
     */
    private SampledSet applyFilters(SampledSet data) {
        if (passTheFilter != null) {
            Arrays.fill(passTheFilter, true);
        }
        if ((filters.size() == 0) || !filtersEnabled) {
            return data;
        }
        List features = new ArrayList();
        for (int i = 0; i < filters.size(); i++) {
            PropertyFilter filter = (PropertyFilter) filters.get(i);
            DbaseData      dbData = dbFile.getField(filter.getName());
            if (dbData == null) {
                System.err.println("null dbdata:" + filter.getName());
                continue;
            }
            List values = dbData.asList();

            for (int valueIdx = 0; valueIdx < values.size(); valueIdx++) {
                boolean ok = true;
                ok = filter.ok(values.get(valueIdx));
                if (matchAll && !ok) {
                    passTheFilter[valueIdx] = false;
                } else if ( !matchAll && ok) {
                    passTheFilter[valueIdx] = true;
                    break;
                }
            }
        }
        if ( !(data instanceof UnionSet)) {
            return data;
        }
        List         okData = new ArrayList();
        SampledSet[] sets   = ((UnionSet) data).getSets();
        //TODO: Keep track of the actual indices here
        for (int i = 0; i < passTheFilter.length; i++) {
            if ( !passTheFilter[i]) {
                continue;
            }
            okData.add(sets[i]);
        }

        SampledSet[] ss = new SampledSet[okData.size()];

        System.arraycopy(okData.toArray(), 0, ss, 0, ss.length);



        if (ss.length > 0) {
            try {
                return new UnionSet(ss[0].getType(), ss, null, null, null,
                                    false);
            } catch (Exception exc) {
                logException("Applying flters", exc);
            }
        }
        //TODO: What do we return here?
        return null;
    }

    /**
     * Load the data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    private void loadData() throws VisADException, RemoteException {
        if (filterGui != null) {
            filters        = filterGui.getFilters();
            matchAll       = filterGui.getMatchAll();
            filtersEnabled = filterGui.getEnabled();
        }
        Data theData = applyFilters(mainData);
        populateTable();
        myDisplay.setData(theData);
    }



    /**
     * Set the data in this control.
     *
     * @param choice   choice describing data
     * @return  true if successful
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected boolean setData(DataChoice choice)
            throws VisADException, RemoteException {
        if ( !super.setData(choice)) {
            return false;
        }
        mainData = getDataInstance().getData();
        if (mainData == null) {
            return false;
        }
        mapSets       = null;
        hasProperties = false;
        if (mainData instanceof UnionSet) {
            SampledSet[] sets = ((UnionSet) mainData).getSets();
            if ((sets.length > 0) && (sets[0] instanceof MapSet)) {
                mapSets = Misc.toList(sets);
                List names = ((MapSet) mapSets.get(0)).getPropertyNames();
                if ((names != null) && (names.size() > 0)) {
                    hasProperties = true;
                    fieldNames =
                        (String[]) names.toArray(new String[names.size()]);
                    passTheFilter = new boolean[mapSets.size()];
                    Arrays.fill(passTheFilter, true);
                }
            }
        }


        Hashtable requestProperties = getRequestProperties();
        dbFile = (DbaseFile) requestProperties.get(
            ShapeFileDataSource.PROP_DBFILE);
        setLineWidth(lineWidth);
        loadData();
        return true;
    }


    /**
     * Get the control widgets for this control.
     *
     * @param controlWidgets   list of controls to add to
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void getControlWidgets(List controlWidgets)
            throws VisADException, RemoteException {
        super.getControlWidgets(controlWidgets);
        controlWidgets.add(
            new WrapperWidget(
                this, GuiUtils.rLabel("Set Line Width:"),
                doMakeWidthSlider()));
    }

    /**
     * Get the label for the color widget.
     * @return  text for label.
     */
    public String getColorWidgetLabel() {
        return "Line Color";
    }


    /**
     * Make a width selection widget
     * @return  slider for selecting line width
     */
    private Component doMakeWidthSlider() {
        JSlider slider = new JSlider(1, 10, getLineWidth());
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setToolTipText("Change width of map lines");
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                JSlider slide = (JSlider) e.getSource();
                if (slide.getValueIsAdjusting()) {
                    return;
                }
                setLineWidth(slide.getValue());
            }
        });
        return slider;
    }

    /**
     * Set the line width.
     *
     * @param width   line width (pixels)
     */
    public void setLineWidth(int width) {
        try {
            if (myDisplay != null) {
                myDisplay.setLineWidth(width);
            }
            lineWidth = width;
        } catch (Exception ve) {}
    }

    /**
     * Get the line width
     * @return  line width
     */
    public int getLineWidth() {
        return lineWidth;
    }



    /**
     * Set the FiltersEnabled property.
     *
     * @param value The new value for FiltersEnabled
     */
    public void setFiltersEnabled(boolean value) {
        filtersEnabled = value;
    }

    /**
     * Get the FiltersEnabled property.
     *
     * @return The FiltersEnabled
     */
    public boolean getFiltersEnabled() {
        return filtersEnabled;
    }


    /**
     *  Set the Filters property.
     *
     *  @param value The new value for Filters
     */
    public void setFilters(List value) {
        filters = value;
    }

    /**
     *  Get the Filters property.
     *
     *  @return The Filters
     */
    public List getFilters() {
        return filters;
    }

    /**
     * Set the MatchAll property.
     *
     * @param value The new value for MatchAll
     */
    public void setMatchAll(boolean value) {
        matchAll = value;
    }

    /**
     * Get the MatchAll property.
     *
     * @return The MatchAll
     */
    public boolean getMatchAll() {
        return matchAll;
    }




    /**
     * Set the SelectedFields property.
     *
     * @param value The new value for SelectedFields
     */
    public void setSelectedFields(List value) {
        selectedFields = value;
    }

    /**
     * Get the SelectedFields property.
     *
     * @return The SelectedFields
     */
    public List getSelectedFields() {
        return selectedFields;
    }


    /**
     * Set the UniqueFields property.
     *
     * @param value The new value for UniqueFields
     */
    public void setUniqueFields(List value) {
        uniqueFields = value;
    }

    /**
     * Get the UniqueFields property.
     *
     * @return The UniqueFields
     */
    public List getUniqueFields() {
        return uniqueFields;
    }



    /**
     * We do have a map projection
     *
     * @return Has map projection
     */
    public boolean hasMapProjection() {
        return (mainData instanceof UnionSet);
    }


    /**
     * get MapProjection of data to display
     *
     * @return The native projection of the data
     */
    public MapProjection getDataProjection() {
        try {
            SampledSet        projSet = null;
            Rectangle2D.Float rect    = null;
            if (mainData instanceof FieldImpl) {
                FieldImpl fi = (FieldImpl) mainData;
                if (GridUtil.isSequence(fi)) {
                    Set domain = fi.getDomainSet();
                    for (int i = 0; i < domain.getLength(); i++) {
                        UnionSet     s    = (UnionSet) fi.getSample(i, false);
                        SampledSet[] sets = s.getSets();
                        if (sets.length > 0) {
                            Rectangle2D.Float subRect = getBounds(sets);
                            if (subRect != null) {
                                if (rect == null) {
                                    rect = subRect;
                                } else {
                                    rect.add(subRect);
                                }
                            }
                        }
                    }
                }
            } else if (mainData instanceof UnionSet) {
                rect = getBounds(((UnionSet) mainData).getSets());
            } else if (mainData instanceof SampledSet) {
                rect = getBounds(new SampledSet[] { (SampledSet) mainData });
            }

            if (rect == null) {
                return null;
            } else {
                return new TrivialMapProjection(
                    RealTupleType.SpatialEarth2DTuple, rect);
            }
        } catch (Exception exc) {
            logException("Creating map projection", exc);
        }
        return null;
    }

    /**
     * Get a bounding rectangle for the sampled sets.
     * @param  sets   array of SampledSet-s
     * @return  a bounding rectangle
     *
     * @throws VisADException on badness
     */
    private Rectangle2D.Float getBounds(SampledSet[] sets)
            throws VisADException {
        if (sets.length == 0) {
            return null;
        }
        double  minX     = Double.POSITIVE_INFINITY;
        double  minY     = Double.POSITIVE_INFINITY;
        double  maxX     = Double.NEGATIVE_INFINITY;
        double  maxY     = Double.NEGATIVE_INFINITY;

        boolean flipRect = false;

        for (int i = 0; i < sets.length; i++) {
            float[] hi  = sets[i].getHi();
            float[] low = sets[i].getLow();
            if (i == 0) {
                minX = low[0];
                minY = low[1];
                maxX = hi[0];
                maxY = hi[1];
            } else {
                minX = Math.min(minX, low[0]);
                minY = Math.min(minY, low[1]);
                maxX = Math.max(maxX, hi[0]);
                maxY = Math.max(maxY, hi[1]);
            }
            if (GridUtil.isLatLonOrder(sets[i])) {
                flipRect = true;
            }
        }
        Rectangle2D.Float rect = new Rectangle2D.Float((float) minX,
                                     (float) minY, (float) (maxX - minX),
                                     (float) (maxY - minY));
        if (flipRect) {
            float tmp = rect.x;
            rect.x      = rect.y;
            rect.y      = tmp;
            tmp         = rect.width;
            rect.width  = rect.height;
            rect.height = tmp;
        }
        return rect;
    }


}
