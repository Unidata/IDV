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

package ucar.unidata.idv.ui;




import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



import ucar.unidata.data.DataAlias;
import ucar.unidata.data.DataChoice;



import ucar.unidata.idv.*;
import ucar.unidata.idv.control.DisplayControlImpl;
import ucar.unidata.ui.ParamField;
import ucar.unidata.ui.colortable.*;



import ucar.unidata.util.ColorTable;

import ucar.unidata.util.ContourInfo;
import ucar.unidata.util.FileManager;
import ucar.unidata.util.GuiUtils;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;

import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.ObjectPair;
import ucar.unidata.util.PatternFileFilter;
import ucar.unidata.util.Range;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;

import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import ucar.visad.quantities.CommonUnits;

import visad.*;

import visad.jmet.MetUnits;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeEvent;

import java.beans.PropertyChangeListener;


import java.io.File;
import java.io.FileOutputStream;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.*;



/**
 *  This class provides 2 facilities. First, there is a set of static methods:
 *  init (XmlResourceCollection resources);
 *  Range getParamRange (String paramName);
 *  getParamColorTable (String paramName);
 *  that provide basic lookup of parameter defaults.
 *  The init method is called once. It is passed  an XmlResourceCollection
 *  that holds the list of xml files to be used. The first resource in the list
 *  is taken to be the user's "writable" resource. These resources are read in, first to
 *  last, and a static collection of  {@link ucar.unidata.idv.ui.ParamInfo}-s are is created that is used to
 *  do the subsequent param default lookups.
 *
 *  This class  is also used to provide an end-user editing facility.
 *
 *
 *   @author IDV development team
 *   @version $Revision: 1.78 $Date: 2007/06/22 13:03:56 $
 */



public class ParamDefaultsEditor extends IdvManager implements ActionListener {

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ParamDefaultsEditor.class.getName());


    /** The param xml tag name */
    public static final String TAG_PARAM = "param";

    /** The params xml tag name */
    public static final String TAG_PARAMS = "params";

    /** The unit xml attribute name */
    public static final String ATTR_UNIT = "unit";

    /** The name xml attribute name */
    public static final String ATTR_NAME = "name";

    /** The colortable xml attribute name */
    public static final String ATTR_COLORTABLE = "table";

    /** The range min xml attribute name */
    public static final String ATTR_RANGE_MIN = "range_min";

    /** The range max xml attribute name */
    public static final String ATTR_RANGE_MAX = "range_max";

    /** The contour info interval  xml attribute name */
    public static final String ATTR_CI_INTERVAL = "ci_interval";

    /** The contour info base  xml attribute name */
    public static final String ATTR_CI_BASE = "ci_base";

    /** The contour info min  xml attribute name */
    public static final String ATTR_CI_MIN = "ci_min";

    /** The contour info max  xml attribute name */
    public static final String ATTR_CI_MAX = "ci_max";

    /** The contour info dash  xml attribute name */
    public static final String ATTR_CI_DASH = "ci_dash";

    /** The contour info label  xml attribute name */
    public static final String ATTR_CI_LABEL = "ci_label";

    /** The contour info label  xml attribute name */
    public static final String ATTR_CI_WIDTH = "ci_width";

    /** The contour info default dash value */
    public static final boolean DFLT_CI_DASH = ContourInfo.DEFAULT_DASH;

    /** The contour info default label value */
    public static final boolean DFLT_CI_LABEL = ContourInfo.DEFAULT_LABEL;

    /** The contour info default width value */
    public static final int DFLT_CI_WIDTH = ContourInfo.DEFAULT_LINE_WIDTH;


    /** The list of column headers */
    private static final String[] columns = { "Parameter", "Color table",
            "Range", "Contours (interval,base,min,max)", "Display unit" };


    /** The set of resources to be displayed */
    XmlResourceCollection resources;

    /**
     *  A list of ParamDefaultsTable objects (an inner class,  derived from JTable)
     */
    ArrayList myTables;



    /**
     *  The tabbed  pane which holds the JTables, one for each resource
     */
    JTabbedPane tableTabbedPane;


    /** Used to view color tables */
    private ColorTableEditor colorTableEditor;

    /**
     * This is the list of {@link ucar.unidata.idv.ui.ParamInfo} objects
     * that are accessed by  the get state methods in this class that are used
     * by {@link ucar.unidata.idv.DisplayConventions}
     */
    private List paramInfos = new ArrayList();

    /** A mapping of param name to ParamInfo */
    private Hashtable paramToInfo = new Hashtable();




    /**
     *  Create the editor with the given collection of xml resources
     *
     * @param idv The IDV
     */
    public ParamDefaultsEditor(IntegratedDataViewer idv) {
        super(idv);
        this.resources = getResourceManager().getXmlResources(
            IdvResourceManager.RSC_PARAMDEFAULTS);
        init(resources);
        if (resources.size() == 0) {
            contents = GuiUtils.top(new JLabel("No resources defined"));
        } else {
            //Initialize the user's writable document (if not created yet)
            resources.getWritableDocument("<" + TAG_PARAMS + "/>");
            init();
        }
    }



    /**
     *  A JTable that holds a list of {@link ParamInfo} objects and provides display and
     *  editing capabilities
     */
    public class ParamDefaultsTable extends JTable {

        /** label */
        String label;

        /**
         *  A list of {@link ParamInfo} objects.
         */
        List myParamInfos;


        /**
         *  Does this  represent the user's writable resource?
         */
        boolean isEditable;

        /**
         *  Keep the tableModel around
         */
        AbstractTableModel tableModel;


        /**
         * Create the table
         *
         * @param infos List of  {@link ParamInfo}
         * @param editable Are the {@link ParamInfo}s editable
         *
         */
        public ParamDefaultsTable(List infos, boolean editable) {
            this.myParamInfos = infos;
            this.isEditable   = editable;

            //Construct the tableModel 
            tableModel = new AbstractTableModel() {
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return false;
                }

                public int getColumnCount() {
                    return columns.length;
                }

                public String getColumnName(int col) {
                    return columns[col];
                }

                public int getRowCount() {
                    return myParamInfos.size();
                }

                public Object getValueAt(int row, int col) {
                    ParamInfo paramInfo = getInfo(row);
                    if (col == 0) {
                        return paramInfo.getName();
                    }
                    if (col == 1) {
                        if (paramInfo.hasColorTableName()) {
                            return paramInfo.getColorTableName();
                        }
                        return "";
                    }
                    if (col == 2) {
                        if (paramInfo.hasRange()) {
                            return paramInfo.getMin() + " - "
                                   + paramInfo.getMax();
                        }
                        return "";
                    }
                    if (col == 3) {
                        if (paramInfo.hasContourInfo()) {
                            ContourInfo ci = paramInfo.getContourInfo();
                            return "" + (ci.getIntervalDefined()
                                         ? String.valueOf(ci.getInterval())
                                         : ci.getLevelsString()) + ", "
                                         + ci.getBase() + ", " + ci.getMin()
                                         + ", " + ci.getMax();
                        }
                        return "";
                    }
                    if (col == 4) {
                        if (paramInfo.hasDisplayUnit()) {
                            return paramInfo.getDisplayUnit().toString();
                        }
                        return "";
                    }
                    return null;
                }

                public void setValueAt(Object aValue, int rowIndex,
                                       int columnIndex) {
                    ParamInfo paramInfo = getInfo(rowIndex);
                    if (columnIndex == 0) {
                        paramInfo.setName(aValue.toString());
                    }
                }
            };
            setModel(tableModel);
            initMouseListener();
        }



        /**
         * Select the param info
         *
         * @param info info
         */
        public void selectParamInfo(ParamInfo info) {
            int index = myParamInfos.indexOf(info);
            if (index >= 0) {
                getSelectionModel().setSelectionInterval(index, index);
            }
        }



        /**
         *  Return the list of {@link ParamInfo}-s held by this table
         * @return List of param infos
         */
        public List getParamInfoList() {
            return myParamInfos;
        }

        /**
         * Get the list of selected param infos
         *
         * @return list of selected param infos
         */
        public List getSelectedParamInfoList() {
            int[] rows   = getSelectedRows();
            List  result = new ArrayList();
            for (int i = 0; i < rows.length; i++) {
                result.add(myParamInfos.get(i));
            }
            return result;
        }


        /**
         *  Return the {@link ParamInfo}
         *
         * @param row
         * @return The ParamInfo
         */
        public ParamInfo getInfo(int row) {
            return (ParamInfo) myParamInfos.get(row);
        }



        /**
         * Utility method to add components to dialog list
         *
         * @param comps list to add to
         * @param name name
         * @param cbx enable checkbox
         * @param comp the component
         */
        private void addEditComponents(List comps, String name,
                                       final JCheckBox cbx,
                                       final JComponent comp) {
            cbx.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    GuiUtils.enableTree(comp, cbx.isSelected());
                }
            });

            GuiUtils.enableTree(comp, cbx.isSelected());
            comps.add(GuiUtils.top(GuiUtils.inset(cbx, 5)));
            comps.add(GuiUtils.top(GuiUtils.inset(GuiUtils.rLabel(name),
                    new Insets(8, 0, 0, 0))));
            JComponent right = GuiUtils.inset(comp, new Insets(3, 5, 0, 0));
            //            comps.add(GuiUtils.leftCenter(GuiUtils.top(GuiUtils.inset(cbx,
            //                    new Insets(2, 0, 0, 0))), GuiUtils.topLeft(right)));
            comps.add(GuiUtils.topLeft(right));
        }


        /**
         * Edit the param info
         *
         * @param paramInfo param info to edit
         *
         * @return user pressed ok
         */
        public boolean editRow(ParamInfo paramInfo) {
            return editRow(paramInfo, false);
        }


        /**
         * Edit row
         *
         * @param paramInfo param info
         * @param removeOnCancel Should remove param info if user presses cancel_
         *
         * @return ok
         */
        public boolean editRow(ParamInfo paramInfo, boolean removeOnCancel) {

            List       comps   = new ArrayList();
            ParamField nameFld = new ParamField(null, true);
            nameFld.setText(paramInfo.getName());
            JPanel topPanel = GuiUtils.hbox(GuiUtils.lLabel("Parameter:  "),
                                            nameFld);
            topPanel = GuiUtils.inset(topPanel, 5);

            comps.add(GuiUtils.inset(new JLabel("Defined"),
                                     new Insets(5, 0, 0, 0)));
            comps.add(GuiUtils.filler());
            comps.add(GuiUtils.filler());



            final JLabel ctPreviewLbl = new JLabel("");
            final JLabel ctLbl        = new JLabel("");
            if (paramInfo.hasColorTableName()) {
                ctLbl.setText(paramInfo.getColorTableName());
                ColorTable ct = getIdv().getColorTableManager().getColorTable(
                                    paramInfo.getColorTableName());
                if (ct != null) {
                    ctPreviewLbl.setIcon(ColorTableCanvas.getIcon(ct));
                } else {
                    ctPreviewLbl.setIcon(null);
                }

            }
            String          cbxLabel = "";
            final ArrayList menus    = new ArrayList();
            getIdv().getColorTableManager().makeColorTableMenu(
                new ObjectListener(null) {
                public void actionPerformed(ActionEvent ae, Object data) {
                    ctLbl.setText(data.toString());
                    ColorTable ct =
                        getIdv().getColorTableManager().getColorTable(
                            ctLbl.getText());
                    if (ct != null) {
                        ctPreviewLbl.setIcon(ColorTableCanvas.getIcon(ct));
                    } else {
                        ctPreviewLbl.setIcon(null);
                    }
                }
            }, menus);

            JCheckBox ctUseCbx = new JCheckBox(cbxLabel,
                                     paramInfo.hasColorTableName());
            final JButton ctPopup = new JButton("Change");
            ctPopup.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    GuiUtils.showPopupMenu(menus, ctPopup);
                }
            });
            addEditComponents(comps, "Color Table:", ctUseCbx,
                              GuiUtils.hbox(ctPopup,
                                            GuiUtils.vbox(ctLbl,
                                                ctPreviewLbl), 5));


            JCheckBox rangeUseCbx = new JCheckBox(cbxLabel,
                                        paramInfo.hasRange());
            JTextField minFld     = new JTextField("" + paramInfo.getMin(),
                                        4);
            JTextField maxFld     = new JTextField("" + paramInfo.getMax(),
                                        4);
            JPanel     rangePanel = GuiUtils.hbox(minFld, maxFld, 5);
            addEditComponents(comps, "Range:", rangeUseCbx, rangePanel);




            JCheckBox unitUseCbx = new JCheckBox(cbxLabel,
                                       paramInfo.hasDisplayUnit());
            String unitLabel = "";
            Unit   unit      = null;
            if (paramInfo.hasDisplayUnit()) {
                unit = paramInfo.getDisplayUnit();
            }



            JComboBox unitFld =
                getIdv().getDisplayConventions().makeUnitBox(unit, null);
            //            JTextField unitFld = new JTextField(unitLabel, 15);
            addEditComponents(comps, "Unit:", unitUseCbx, unitFld);


            ContourInfo ci            = paramInfo.getContourInfo();
            JCheckBox   contourUseCbx = new JCheckBox(cbxLabel, ci != null);
            if (ci == null) {
                ci = new ContourInfo();
            }
            ContourInfoDialog contDialog =
                new ContourInfoDialog("Edit Contour Defaults", false, null,
                                      false);
            contDialog.setState(ci);
            addEditComponents(comps, "Contour:", contourUseCbx,
                              contDialog.getContents());

            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            JComponent contents = GuiUtils.doLayout(comps, 3,
                                      GuiUtils.WT_NNY, GuiUtils.WT_N);

            contents = GuiUtils.topCenter(topPanel, contents);
            contents = GuiUtils.inset(contents, 5);
            while (true) {
                if ( !GuiUtils.showOkCancelDialog(null, "Parameter Defaults",
                        contents, null)) {
                    if (removeOnCancel) {
                        myParamInfos.remove(paramInfo);
                        tableChanged();
                    }
                    return false;
                }
                String what = "";
                try {
                    if (contourUseCbx.isSelected()) {
                        what = "setting contour defaults";
                        contDialog.doApply();
                        ci.set(contDialog.getInfo());
                        paramInfo.setContourInfo(ci);
                    } else {
                        paramInfo.clearContourInfo();
                    }
                    if (unitUseCbx.isSelected()) {
                        what = "setting display unit";
                        Object selected = unitFld.getSelectedItem();
                        String unitName =
                            TwoFacedObject.getIdString(selected);
                        if ((unitName == null)
                                || unitName.trim().equals("")) {
                            paramInfo.setDisplayUnit(null);
                        } else {
                            paramInfo.setDisplayUnit(
                                ucar.visad.Util.parseUnit(unitName));
                        }
                    } else {
                        paramInfo.setDisplayUnit(null);
                    }

                    if (ctUseCbx.isSelected()) {
                        paramInfo.setColorTableName(ctLbl.getText());
                    } else {
                        paramInfo.clearColorTableName();
                    }

                    if (rangeUseCbx.isSelected()) {
                        what = "setting range";
                        paramInfo.setRange(
                            new Range(
                                Misc.parseNumber(minFld.getText()),
                                Misc.parseNumber(maxFld.getText())));
                    } else {
                        paramInfo.clearRange();
                    }

                    paramInfo.setName(nameFld.getText().trim());
                    break;
                } catch (Exception exc) {
                    errorMsg("An error occurred " + what + "\n "
                             + exc.getMessage());
                    //              exc.printStackTrace();
                }
            }
            repaint();
            saveData();
            return true;

        }



        /**
         * Show the colortable in the color table editor for the given row
         *
         * @param row The given row
         */
        protected void viewColorTable(int row) {
            ColorTable ct = getColorTable(getInfo(row));
            if (colorTableEditor == null) {
                colorTableEditor =
                    new ColorTableEditor(getIdv().getColorTableManager(), ct);
            } else {
                colorTableEditor.setColorTable(ct);
            }
            colorTableEditor.show();
        }


        /**
         * Helper to show an error message
         *
         * @param msg The message
         */
        protected void errorMsg(String msg) {
            javax.swing.JOptionPane.showMessageDialog(this, msg, "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        /**
         * Tool tip for the table
         *
         * @param event The event
         * @return The tooltip text
         */
        public String getToolTipText(MouseEvent event) {
            return "Right-click to show popup menu, double click to edit row";
        }


        /**
         * Add a mouselistener to this table
         */
        private void initMouseListener() {
            addMouseListener(new MouseAdapter() {



                public void mouseReleased(MouseEvent e) {
                    final int row       = rowAtPoint(e.getPoint());
                    ParamInfo paramInfo = getInfo(row);
                    if ( !SwingUtilities.isRightMouseButton(e)) {
                        if ((e.getClickCount() > 1) && (paramInfo != null)) {
                            if (isEditable) {
                                editRow(paramInfo);
                            } else {
                                copyToUsers(paramInfo);
                            }
                        }
                        return;
                    }

                    getSelectionModel().setSelectionInterval(row, row);
                    JPopupMenu popup = new JPopupMenu();
                    makePopupMenu(popup, row);
                    popup.show((Component) e.getSource(), e.getX(), e.getY());
                }
            });
        }

        /**
         * Make the popup menu
         *
         * @param popup The popup menu
         * @param row The row the user clicked on
         */
        void makePopupMenu(JPopupMenu popup, final int row) {
            ParamInfo info = getInfo(row);
            if (isEditable) {
                makeEditableMenu(popup, row);
            } else {
                JMenuItem mi = new JMenuItem("Copy Row to Users Defaults");
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            copyToUsers(getInfo(row));
                        } catch (Exception exc) {
                            LogUtil.printException(log_,
                                    "Copying row: " + row
                                    + " to users table.", exc);
                        }
                    }
                });
                popup.add(mi);
            }
            if (info != null) {
                popup.add(GuiUtils.makeMenuItem("Export to Plugin",
                        getIdv().getPluginManager(), "addObject", info));
            }
        }




        /**
         * Find the ParamInfo from the name
         *
         * @param name name
         *
         * @return param info
         */
        public ParamInfo findByName(String name) {
            name = name.trim();
            for (int i = 0; i < myParamInfos.size(); i++) {
                ParamInfo paramInfo = (ParamInfo) myParamInfos.get(i);
                if (paramInfo.getName().equals(name)) {
                    return paramInfo;
                }
            }
            return null;
        }

        /**
         * Add the ParamInfo into the table
         *
         * @param i
         */
        public void add(ParamInfo i) {
            if ( !myParamInfos.contains(i)) {
                myParamInfos.add(i);
            }
            tableChanged();
        }



        /**
         * Add the ParamInfo into the table
         *
         * @param i
         */
        public void addBeginning(ParamInfo i) {
            myParamInfos.add(0, i);
            tableChanged();
        }


        /**
         * Make the edit menu
         *
         * @param popup The popup
         * @param row The row
         */
        void makeEditableMenu(JPopupMenu popup, final int row) {

            JMenuItem mi;
            popup.add(GuiUtils.makeMenuItem("Add New Field", this,
                                            "addNewRow"));
            ParamInfo paramInfo = getInfo(row);
            if (paramInfo == null) {
                return;
            }

            popup.add(GuiUtils.makeMenuItem("Edit Settings", this, "editRow",
                                            paramInfo));

            mi = new JMenuItem("Delete Settings For Parameter");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    removeRow(row);
                }
            });
            popup.add(mi);
        }


        /**
         * Add a new row
         */
        public void addNewRow() {
            ParamInfo paramInfo = new ParamInfo("", null, null, null, null);
            myParamInfos.add(paramInfo);
            tableChanged();
            editRow(paramInfo, true);
        }


        /**
         * Remove the given row
         *
         * @param row The given row
         */
        protected void removeRow(int row) {
            if ( !GuiUtils.showYesNoDialog(
                    null, "Are you sure you want to delete this row?",
                    "Delete Confirmation")) {
                return;
            }
            myParamInfos.remove(row);
            tableChanged();
            saveData();
        }



        /**
         * Make the color table menu items
         *
         * @param row The given row
         * @return List of menu items
         */
        public ArrayList makeCTMenuItems(final int row) {
            ArrayList menus = new ArrayList();
            getIdv().getColorTableManager().makeColorTableMenu(
                new ObjectListener(null) {
                public void actionPerformed(ActionEvent ae, Object data) {
                    getInfo(row).setColorTableName(data.toString());
                    tableChanged();
                }
            }, menus);
            return menus;
        }

        /**
         * Table changed
         */
        public void tableChanged() {
            tableModel.fireTableStructureChanged();
        }

        /**
         * Map the name of the color table to the {@link ucar.unidata.util.ColorTable}
         *
         * @param name Name of the color table
         * @return The ColorTable
         */
        protected ColorTable getColorTableFromName(String name) {
            ColorTable ct =
                getIdv().getColorTableManager().getColorTable(name);
            if (ct == null) {
                ct = getIdv().getColorTableManager().getDefaultColorTable();
            }
            return ct;
        }

    }


    /**
     * Load in the {@link ucar.unidata.idv.ui.ParamInfo}-s defined in the
     * xml from the given root Element. Create a new JTable and add it into
     * the GUI.
     *
     * @param root The xml root
     * @param i Which resource is this
     */
    private void addList(Element root, int i) {
        List    infos;
        boolean isWritable = resources.isWritableResource(i);
        if (root != null) {
            infos = createParamInfoList(root);
        } else {
            if ( !isWritable) {
                return;
            }
            infos = new ArrayList();
        }

        if (infos.size() == 0) {
            //            infos.add(new ParamInfo("", null, null, null, null));
        }
        ParamDefaultsTable table = new ParamDefaultsTable(infos, isWritable);
        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        String editableStr = "";
        if ( !isWritable) {
            editableStr = " (" + Msg.msg("non-editable") + ") ";
        }
        JLabel label = new JLabel("<html>"
                                  + Msg.msg("Path: ${param1}",
                                            resources.get(i)
                                            + editableStr) + "</html>");
        JPanel tablePanel = GuiUtils.topCenter(GuiUtils.inset(label, 4),
                                new JScrollPane(table));

        table.label = resources.getShortName(i);
        tableTabbedPane.add(resources.getShortName(i), tablePanel);
        myTables.add(table);
    }

    /**
     * Intialize me
     */
    private void init() {
        myTables        = new ArrayList();
        tableTabbedPane = new JTabbedPane();
        tableTabbedPane.setPreferredSize(new Dimension(450, 200));
        JMenuBar menuBar  = new JMenuBar();
        JMenu    fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        fileMenu.add(GuiUtils.makeMenuItem("New Row", this, "addNewRow"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Open", this, "doOpen"));
        fileMenu.add(GuiUtils.makeMenuItem("Import", this, "doImport"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Export to File", this,
                                           "doSaveAs"));
        fileMenu.add(GuiUtils.makeMenuItem("Export to Plugin", this,
                                           "exportToPlugin"));
        fileMenu.add(GuiUtils.makeMenuItem("Export Selected to Plugin", this,
                                           "exportSelectedToPlugin"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "doClose"));


        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        helpMenu.add(GuiUtils.makeMenuItem("Parameter Defaults Help", this,
                                           "showHelp"));
        JComponent bottom = GuiUtils.wrap(GuiUtils.makeButton("Close", this,
                                "doClose"));
        contents = GuiUtils.topCenterBottom(menuBar,
                                            GuiUtils.inset(tableTabbedPane,
                                                2), bottom);
        setMenuBar(menuBar);
        loadResources(resources);
    }

    /**
     * Export the selected param infos to the plugin manager
     */
    public void exportSelectedToPlugin() {
        ParamDefaultsTable table    = getCurrentTable();
        List               selected = table.getSelectedParamInfoList();
        if (selected.size() == 0) {
            LogUtil.userMessage("No rows selected");
            return;
        }
        getIdv().getPluginManager().addObject(selected);
    }

    /**
     * Export allthe param infos to the plugin manager
     */
    public void exportToPlugin() {
        ParamDefaultsTable table = getCurrentTable();
        List               list  = table.getParamInfoList();
        if (list.size() == 0) {
            LogUtil.userMessage("No rows selected");
            return;
        }
        getIdv().getPluginManager().addObject(list);
    }


    /**
     * add a new row to users table
     */
    public void addNewRow() {
        if (myTables.size() > 0) {
            GuiUtils.showComponentInTabs(
                ((ParamDefaultsTable) myTables.get(0)));
            ((ParamDefaultsTable) myTables.get(0)).addNewRow();

        }

    }

    /**
     * Get the param infos
     *
     * @param justFirst if true then just get the first table
     *
     * @return param infos
     */
    public List getParamInfos(boolean justFirst) {
        List infos = new ArrayList();
        for (int i = 0; i < myTables.size(); i++) {
            infos.addAll(
                ((ParamDefaultsTable) myTables.get(i)).getParamInfoList());
            if (justFirst) {
                break;
            }
        }
        return infos;
    }


    /**
     * Get the list of resources
     *
     * @return the list of resources
     */
    public List getResources() {
        List infos = new ArrayList();
        for (int i = 0; i < myTables.size(); i++) {
            ParamDefaultsTable paramDefaultsTable =
                (ParamDefaultsTable) myTables.get(i);
            for (ParamInfo paramInfo :
                    (List<ParamInfo>) paramDefaultsTable.getParamInfoList()) {
                infos.add(new ResourceViewer.ResourceWrapper(paramInfo,
                        paramInfo.toString(), paramDefaultsTable.label,
                        paramDefaultsTable.isEditable));
            }
        }
        return infos;
    }


    /**
     * Load in the xml resources
     *
     * @param resources The resources
     */
    public void loadResources(XmlResourceCollection resources) {
        for (int i = 0; i < resources.size(); i++) {
            Element root = resources.getRoot(i);
            addList(root, i);
        }
    }


    /**
     * Return the ParamDefaultsTable which is currently being shown in the tabbed pane
     * @return The current  ParamDefaultsTable
     */
    public ParamDefaultsTable getCurrentTable() {
        int index = tableTabbedPane.getSelectedIndex();
        return (ParamDefaultsTable) myTables.get(index);
    }



    /**
     * Import an xml param defaults file
     */
    public void doImport() {
        try {
            String filename = FileManager.getReadFile(FileManager.FILTER_XML);
            if (filename == null) {
                return;
            }
            Element root = XmlUtil.getRoot(IOUtil.readContents(filename));
            if (root == null) {
                return;
            }
            List               infos = createParamInfoList(root);
            ParamDefaultsTable table = getCurrentTable();
            table.getParamInfoList().addAll(infos);
            table.tableChanged();
            saveData();
        } catch (Exception exc) {
            LogUtil.printException(log_, "Error importing file", exc);
        }
    }


    /**
     * Open  an xml param defaults file
     */
    public void doOpen() {
        String filename = FileManager.getReadFile(FileManager.FILTER_XML);
        if (filename == null) {
            return;
        }
        resources.addResource(filename);
        int index = resources.size() - 1;
        addList(resources.getRoot(index), index);
    }


    /**
     * Handle the CLOSEANCEL, OK, HELP,  events.
     *
     * @param event The event
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_CLOSE)) {
            doClose();
        } else if (cmd.equals(GuiUtils.CMD_NEW)) {
            if (myTables.size() > 0) {
                ((ParamDefaultsTable) myTables.get(0)).addNewRow();
            }
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            saveData();
            doClose();
        } else if (cmd.equals(GuiUtils.CMD_HELP)) {
            showHelp();
        } else if (cmd.equals(GuiUtils.CMD_SAVEAS)) {
            doSaveAs(getCurrentTable().getParamInfoList());
        } else if (cmd.equals(GuiUtils.CMD_OPEN)) {
            doOpen();
        } else if (cmd.equals(GuiUtils.CMD_IMPORT)) {
            doImport();
        }

    }

    /**
     * show help
     */
    public void showHelp() {
        getIdv().getIdvUIManager().showHelp(
            "idv.tools.parameterdefaultseditor");
    }

    /**
     * Close the window (and the color table editor if it is open)
     */
    public void doClose() {
        if (colorTableEditor != null) {
            colorTableEditor.setVisible(false);
        }
        super.close();
    }

    /**
     * Get the window title to use
     *
     * @return Window title
     */
    protected String getWindowTitle() {
        return GuiUtils.getApplicationTitle() + "Parameter Defaults Editor";
    }



    /**
     * Save the list of ParamInfo-s into the given file
     *
     * @param infoList List of infos
     * @param filename The filename to write to
     */
    public void doSave(List infoList, String filename) {
        try {
            Element root = createDom(XmlUtil.makeDocument(), infoList);
            IOUtil.writeFile(filename, XmlUtil.toString(root));
        } catch (Exception exc) {
            LogUtil.printException(log_, "Error writing file", exc);
        }

    }

    /**
     * Save the param infos
     */
    public void doSaveAs() {
        doSaveAs(getCurrentTable().getParamInfoList());
    }

    /**
     * Prompt for a file and write out the ParamInfo-s from
     * the given list.
     *
     * @param infoList List of ParamInfo-s
     */
    public void doSaveAs(List infoList) {
        String filename = FileManager.getWriteFile(FileManager.FILTER_XML,
                              FileManager.SUFFIX_XML);
        if (filename == null) {
            return;
        }
        doSave(infoList, filename);
    }



    /**
     * Copy the given ParamInfo object into the user's editable table
     *
     * @param i the param fino object to copy
     */
    protected void copyToUsers(ParamInfo i) {
        ParamInfo          copy = new ParamInfo(i);
        ParamDefaultsTable to   = (ParamDefaultsTable) myTables.get(0);
        to.add(copy);
        tableTabbedPane.setSelectedIndex(0);
        to.editRow(copy, true);
    }





    /**
     * Create xml dom from the given list of {@link ucar.unidata.idv.ui.ParamInfo}-s
     *
     * @param doc The document to write to
     * @param paramInfos List of param infos
     * @return Root xml element
     */
    private Element createDom(Document doc, List paramInfos) {
        Element root = doc.createElement(TAG_PARAMS);
        for (int i = 0; i < paramInfos.size(); i++) {
            ParamInfo paramInfo = (ParamInfo) paramInfos.get(i);
            if (paramInfo.getName().trim().length() == 0) {
                continue;
            }
            Element node = doc.createElement(TAG_PARAM);
            node.setAttribute(ATTR_NAME, paramInfo.getName());
            if (paramInfo.hasColorTableName()) {
                node.setAttribute(ATTR_COLORTABLE,
                                  paramInfo.getColorTableName());
            }
            if (paramInfo.hasRange()) {
                node.setAttribute(ATTR_RANGE_MIN,
                                  "" + paramInfo.getRange().getMin());
                node.setAttribute(ATTR_RANGE_MAX,
                                  "" + paramInfo.getRange().getMax());
            }
            if (paramInfo.hasDisplayUnit()) {
                node.setAttribute(ATTR_UNIT, "" + paramInfo.getDisplayUnit());
            }
            if (paramInfo.hasContourInfo()) {
                ContourInfo ci = paramInfo.getContourInfo();
                node.setAttribute(ATTR_CI_INTERVAL,
                                  "" + ci.getIntervalString(true));
                node.setAttribute(ATTR_CI_BASE, "" + ci.getBase());
                node.setAttribute(ATTR_CI_MIN, "" + ci.getMin());
                node.setAttribute(ATTR_CI_MAX, "" + ci.getMax());
                if (ci.getDashOn() != DFLT_CI_DASH) {
                    node.setAttribute(ATTR_CI_DASH, "" + ci.getDashOn());
                }
                if (ci.getIsLabeled() != DFLT_CI_LABEL) {
                    node.setAttribute(ATTR_CI_LABEL, "" + ci.getIsLabeled());
                }
                node.setAttribute(ATTR_CI_WIDTH, "" + ci.getLineWidth());

            }
            root.appendChild(node);
        }
        return root;
    }


    /**
     *  Get the color table, range, etc, from the given display control and save them
     * as the param defaults for its data choice
     *
     * @param displayControl the display control to get state from
     */
    public void saveDefaults(DisplayControlImpl displayControl) {
        try {
            List choices = displayControl.getMyDataChoices();
            if (choices.size() != 1) {
                return;
            }
            DataChoice dc   = (DataChoice) choices.get(0);
            String     name = dc.getName();
            String     ctName = ((displayControl.getColorTable() != null)
                                 ? displayControl.getColorTable().getName()
                                 : null);
            ParamInfo newParamInfo = new ParamInfo(name, ctName,
                                         displayControl.getRange(),
                                         displayControl.getContourInfo(),
                                         displayControl.getDisplayUnit());
            ParamDefaultsTable firstTable = getFirstTable();
            if ( !firstTable.editRow(newParamInfo, false)) {
                return;
            }
            ParamInfo origParamInfo = firstTable.findByName(dc.getName());
            if (origParamInfo == null) {
                firstTable.addBeginning(newParamInfo);
                firstTable.getSelectionModel().setSelectionInterval(0, 0);
            } else {
                origParamInfo.initWith(newParamInfo);
                firstTable.tableChanged();
                firstTable.selectParamInfo(origParamInfo);
            }
            saveData();
            show();
            GuiUtils.showComponentInTabs(firstTable);
        } catch (Exception exc) {
            LogUtil.printException(log_, "copying defaults", exc);
        }
    }


    /**
     * Get the first JTable in the list
     *
     * @return First table
     */
    private ParamDefaultsTable getFirstTable() {
        return (ParamDefaultsTable) myTables.get(0);
    }

    /**
     * Write out the user's editable param infos
     */
    private void saveData() {
        Document usersDoc = XmlUtil.makeDocument();
        Element usersRoot = createDom(usersDoc,
                                      getFirstTable().getParamInfoList());
        try {
            resources.setWritableDocument(usersDoc, usersRoot);
            resources.writeWritable();
            //Reinitialize the static state
            paramInfos  = new ArrayList();
            paramToInfo = new Hashtable();
            init(resources);
        } catch (Exception exc) {
            LogUtil.printException(log_, "writing aliases xml", exc);
        }

    }





    /**
     * Create the param infos from the given xml root
     *
     * @param root The xml root
     * @param overwriteOk Ok to overwrite an existing one
     */
    private void loadParamDefaults(Element root, boolean overwriteOk) {
        List listOfInfos = createParamInfoList(root);
        for (int i = 0; i < listOfInfos.size(); i++) {
            ParamInfo newParamInfo = (ParamInfo) listOfInfos.get(i);
            String    paramName    = newParamInfo.getName();
            if ( !overwriteOk && (paramToInfo.get(paramName) != null)) {
                continue;
            }

            ParamInfo oldParamInfo = (ParamInfo) paramToInfo.get(paramName);
            if (oldParamInfo == null) {
                paramToInfo.put(paramName, newParamInfo);
                paramInfos.add(newParamInfo);
            } else {
                if ( !oldParamInfo.hasColorTableName()) {
                    oldParamInfo.setColorTableName(
                        newParamInfo.getColorTableName());
                }
                if ( !oldParamInfo.hasRange()) {
                    oldParamInfo.setRange(newParamInfo.getRange());
                }
            }
        }
    }




    /**
     * Create the param infos from the given xml root
     *
     * @param root The xml root
     * @return List of param infos
     */
    private List createParamInfoList(Element root) {

        List infos = new ArrayList();

        if ( !root.getTagName().equals(TAG_PARAMS)) {
            try {
                Object obj = getIdv().getEncoderForRead().toObject(root);
                if (obj instanceof List) {
                    infos.addAll((List) obj);
                } else {
                    System.err.println("Unknown object type: "
                                       + obj.getClass().getName());
                }
            } catch (Exception exc) {
                System.err.println("Error reading param defaults");
            }
            return infos;
        }



        List nodes = XmlUtil.findChildren(root, TAG_PARAM);


        for (int i = 0; i < nodes.size(); i++) {
            Element     child       = (Element) nodes.get(i);
            Range       range       = null;
            Unit        displayUnit = null;
            ContourInfo contourInfo = null;


            String      paramName   = XmlUtil.getAttribute(child, ATTR_NAME);
            String colorTableName = XmlUtil.getAttribute(child,
                                        ATTR_COLORTABLE, (String) null);
            String range_min = XmlUtil.getAttribute(child, ATTR_RANGE_MIN,
                                   (String) null);
            String range_max = XmlUtil.getAttribute(child, ATTR_RANGE_MAX,
                                   (String) null);

            String unitName = XmlUtil.getAttribute(child, ATTR_UNIT,
                                  (String) null);


            String ci_interval = XmlUtil.getAttribute(child,
                                     ATTR_CI_INTERVAL, (String) null);
            String ci_base = XmlUtil.getAttribute(child, ATTR_CI_BASE,
                                 (String) null);
            String ci_min = XmlUtil.getAttribute(child, ATTR_CI_MIN,
                                range_min);
            String ci_max = XmlUtil.getAttribute(child, ATTR_CI_MAX,
                                range_max);
            boolean ci_dash = XmlUtil.getAttribute(child, ATTR_CI_DASH,
                                  DFLT_CI_DASH);
            boolean ci_label = XmlUtil.getAttribute(child, ATTR_CI_LABEL,
                                   DFLT_CI_LABEL);
            String ci_width = XmlUtil.getAttribute(child, ATTR_CI_WIDTH,
                                  String.valueOf(DFLT_CI_WIDTH));


            if (unitName != null) {
                try {
                    displayUnit = ucar.visad.Util.parseUnit(unitName);
                } catch (Exception e) {
                    LogUtil.printException(log_,
                                           "Creating unit: " + unitName, e);
                }
            }

            if ((ci_interval != null) || (ci_base != null)) {
                if (ci_interval == null) {
                    ci_interval = "NaN";
                }

                if (ci_base == null) {
                    ci_base = "NaN";
                }
                if (ci_min == null) {
                    ci_min = "NaN";
                }
                if (ci_max == null) {
                    ci_max = "NaN";
                }
                if (ci_width == null) {
                    ci_width = "1";
                }
                contourInfo = new ContourInfo(ci_interval,
                        Misc.parseDouble(ci_base), Misc.parseDouble(ci_min),
                        Misc.parseDouble(ci_max), ci_label, ci_dash,
                        ContourInfo.DEFAULT_FILL, Misc.parseDouble(ci_width));
            }


            if ((ci_dash != DFLT_CI_DASH) || (ci_label != DFLT_CI_LABEL)) {
                if (contourInfo == null) {
                    contourInfo = new ContourInfo(Double.NaN, Double.NaN,
                            Double.NaN, Double.NaN);
                    contourInfo.setIsLabeled(ci_label);
                    contourInfo.setDashOn(ci_dash);
                }
            }


            if ((range_min != null) && (range_max != null)) {
                range = new Range(Misc.parseDouble(range_min),
                                  Misc.parseDouble(range_max));
            }

            ParamInfo paramInfo = new ParamInfo(paramName, colorTableName,
                                      range, contourInfo, displayUnit);
            infos.add(paramInfo);
        }
        return infos;

    }

    /**
     * Load in all of the {@link ucar.unidata.idv.ui.ParamInfo}-s
     * pointed to by the given resource collection
     *
     * @param resources The resources (e.g., the paramdefaults.xml)
     */
    private void init(XmlResourceCollection resources) {
        try {
            for (int i = 0; i < resources.size(); i++) {
                Element root = resources.getRoot(i, false);
                if (root != null) {
                    loadParamDefaults(root, false);
                }
            }
        } catch (Exception exc) {
            LogUtil.printException(
                log_, "Loading  parameter to color table properties ", exc);
        }
    }




    /**
     * Find the {@link ucar.unidata.idv.ui.ParamInfo} for the given name
     *
     * @param paramName The name to look for
     * @return The {@link ucar.unidata.idv.ui.ParamInfo} associated with the name
     */
    private ParamInfo getParamInfo(String paramName) {
        if (paramName == null) {
            return null;
        }
        ParamInfo info = (ParamInfo) StringUtil.findMatch(paramName,
                             paramInfos, null);
        if (info == null) {
            info = (ParamInfo) StringUtil.findMatch(paramName.toLowerCase(),
                    paramInfos, null);
        }

        if (info == null) {
            String canonicalName = DataAlias.aliasToCanonical(paramName);
            if (canonicalName != null) {
                info = (ParamInfo) StringUtil.findMatch(
                    canonicalName.toLowerCase(), paramInfos, null);
            }
        }
        return info;
    }

    /**
     * Find the color table name for the given param name
     *
     * @param paramName Name to look for
     * @return Color table name or null if not found
     */
    private ColorTable getColorTable(String paramName) {
        ParamInfo paramInfo = getParamInfo(paramName);
        if (paramInfo != null) {
            return getColorTable(paramInfo);
        }
        return null;
    }


    /**
     * Get the color table for a particular parameter from ParamInfo
     *
     * @param info   parameter information
     *
     * @return the associated color table.
     */
    private ColorTable getColorTable(ParamInfo info) {
        if (info.getColorTableName() != null) {
            return getIdv().getColorTableManager().getColorTable(
                info.getColorTableName());
        }
        return null;
    }


    /**
     *  Returns a Range based on the parameter name (e.g., rh, t, etc.)
     *
     * @param paramName Name to look for
     * @return The {@link ucar.unidata.util.Range} found or null
     */
    public Range getParamRange(String paramName) {
        ParamInfo paramInfo = getParamInfo(paramName);
        return ((paramInfo != null)
                ? paramInfo.getRange()
                : null);
    }

    /**
     *  Returns a ContourInfo based on the parameter name (e.g., rh, t, etc.)
     *
     * @param paramName Name to look for
     * @return The {@link ucar.unidata.util.ContourInfo} found or null
     */
    public ContourInfo getParamContourInfo(String paramName) {
        ParamInfo paramInfo = getParamInfo(paramName);
        return ((paramInfo != null)
                ? paramInfo.getContourInfo()
                : null);
    }


    /**
     *  Returns a Unit based on the parameter name (e.g., rh, t, etc.)
     *
     * @param paramName Name to look for
     * @return The Unit found or null
     */
    public Unit getParamDisplayUnit(String paramName) {
        ParamInfo paramInfo = getParamInfo(paramName);
        return ((paramInfo != null)
                ? paramInfo.getDisplayUnit()
                : null);
    }


    /**
     *  Returns a color table based on the parameter name (e.g., rh, t, etc.)
     *
     * @param paramName Name to look for
     * @return The {@link ucar.unidata.util.ColorTable} found or null
     */
    public ColorTable getParamColorTable(String paramName) {
        return getParamColorTable(paramName, true);
    }

    /**
     * Get the color table for the parameters
     *
     * @param paramName  parameter name
     * @param useDefault  true to use the default color table if not found
     *
     * @return the associated color table
     */
    public ColorTable getParamColorTable(String paramName,
                                         boolean useDefault) {
        ColorTable vc = getColorTable(paramName);
        //Try the canonical names.
        if (vc == null) {
            vc = getColorTable(DataAlias.aliasToCanonical(paramName));
        }

        if ((vc == null) && useDefault) {
            vc = getIdv().getColorTableManager().getDefaultColorTable();
        }
        return vc;
    }



}
