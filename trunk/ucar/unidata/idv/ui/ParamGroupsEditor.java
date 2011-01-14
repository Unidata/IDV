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
import ucar.unidata.data.DataGroup;
import ucar.unidata.data.DerivedDataDescriptor;



import ucar.unidata.idv.*;
import ucar.unidata.idv.control.DisplayControlImpl;






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
 *  An editor for param groups
 *
 *
 *   @author IDV development team
 *   @version $Revision: 1.5 $Date: 2007/06/21 14:45:02 $
 */



public class ParamGroupsEditor extends IdvManager implements ActionListener {

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            ParamGroupsEditor.class.getName());





    /** The param xml tag name */
    public static final String TAG_PARAM = "param";

    /** The params xml tag name */
    public static final String TAG_PARAMGROUPS = "paramgroups";

    /** The params xml tag name */
    public static final String TAG_PARAMGROUP = "paramgroup";


    /** The list of column headers */
    private static final String[] columns = { "Parameter Group",
            "Description", "Params" };


    /** The set of resources to be displayed */
    XmlResourceCollection resources;

    /**
     *  A list of ParamGroupsTable objects (an inner class,  derived from JTable)
     */
    ArrayList myTables;



    /**
     *  The tabbed  pane which holds the JTables, one for each resource
     */
    JTabbedPane tableTabbedPane;




    /**
     *  Create the editor with the given collection of xml resources
     *
     * @param idv The IDV
     */
    public ParamGroupsEditor(IntegratedDataViewer idv) {
        super(idv);
        this.resources = getResourceManager().getXmlResources(
            IdvResourceManager.RSC_PARAMGROUPS);
        if (resources.size() == 0) {
            contents = GuiUtils.top(new JLabel("No resources defined"));
        } else {
            //Initialize the user's writable document (if not created yet)
            resources.getWritableDocument("<" + TAG_PARAMGROUPS + "/>");
            init();
        }
    }



    /**
     *  A JTable that holds a list of {@link ucar.unidata.data.DataGroup} objects and provides display and
     *  editing capabilities
     */
    public class ParamGroupsTable extends JTable {

        /**
         *  A list of {@link DataGroup} objects.
         */
        List myDataGroups;


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
         * @param infos List of  {@link DataGroup}
         * @param editable Are the {@link DataGroup}s editable
         *
         */
        public ParamGroupsTable(List infos, boolean editable) {
            this.myDataGroups = infos;
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
                    return myDataGroups.size();
                }

                public Object getValueAt(int row, int col) {
                    DataGroup dataGroup = getDataGroup(row);
                    if (col == 0) {
                        return dataGroup.getName();
                    }
                    if (col == 1) {
                        return dataGroup.getDescription();
                    }

                    if (col == 2) {
                        return StringUtil.join(", ",
                                dataGroup.getParamSets());
                    }
                    return null;
                }

                public void setValueAt(Object aValue, int rowIndex,
                                       int columnIndex) {}
            };
            setModel(tableModel);
            initMouseListener();
        }



        /**
         * Select the param info
         *
         * @param info info
         */
        public void selectDataGroup(DataGroup info) {
            int index = myDataGroups.indexOf(info);
            if (index >= 0) {
                getSelectionModel().setSelectionInterval(index, index);
            }
        }



        /**
         *  Return the list of {@link DataGroup}-s held by this table
         * @return List of param infos
         */
        public List getDataGroupList() {
            return myDataGroups;
        }

        /**
         * Get the list of selected param infos
         *
         * @return list of selected param infos
         */
        public List getSelectedDataGroupList() {
            int[] rows   = getSelectedRows();
            List  result = new ArrayList();
            for (int i = 0; i < rows.length; i++) {
                result.add(myDataGroups.get(i));
            }
            return result;
        }


        /**
         *  Return the {@link DataGroup}
         *
         * @param row
         * @return The DataGroup
         */
        public DataGroup getDataGroup(int row) {
            return (DataGroup) myDataGroups.get(row);
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
         * @param dataGroup info to edit
         *
         * @return user pressed ok
         */
        public boolean editRow(DataGroup dataGroup) {
            return editRow(dataGroup, false);
        }


        /**
         * Edit row
         *
         * @param dataGroup param info
         * @param removeOnCancel Should remove param info if user presses cancel_
         *
         * @return ok
         */
        public boolean editRow(DataGroup dataGroup, boolean removeOnCancel) {

            List       comps   = new ArrayList();
            JTextField nameFld = new JTextField(dataGroup.getName());
            JTextField descFld = new JTextField(dataGroup.getDescription());
            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            JPanel topPanel = GuiUtils.doLayout(new Component[] {
                                  GuiUtils.rLabel("Name:"),
                                  nameFld, GuiUtils.rLabel("Description:"),
                                  descFld }, 2, GuiUtils.WT_NY,
                                             GuiUtils.WT_N);
            StringBuffer paramSB   = new StringBuffer();
            List         paramSets = dataGroup.getParamSets();
            for (int i = 0; i < paramSets.size(); i++) {
                List params = (List) paramSets.get(i);
                paramSB.append(StringUtil.join(", ", params));
                paramSB.append("\n");
            }
            topPanel = GuiUtils.inset(topPanel, 5);
            comps.add(topPanel);
            final JTextArea paramsFld = new JTextArea(paramSB.toString(), 15,
                                            10);
            paramsFld.addMouseListener(new MouseAdapter() {
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        IdvUIManager.showParamsPopup(paramsFld, e, ",", true);
                    }
                }
            });

            paramsFld.setToolTipText(
                "<html>Enter parameter name<br>Right mouse to add current parameters</html>");

            JScrollPane sp =
                new JScrollPane(
                    paramsFld,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);


            comps.add(
                new JLabel(
                    "Parameters - One group per line, comma separated"));
            comps.add(sp);

            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            JComponent contents = GuiUtils.doLayout(comps, 1, GuiUtils.WT_Y,
                                      GuiUtils.WT_NY);

            contents = GuiUtils.topCenter(topPanel, contents);
            contents = GuiUtils.inset(contents, 5);
            while (true) {
                if ( !GuiUtils.showOkCancelDialog(null, "Parameter Group",
                        contents, null)) {
                    if (removeOnCancel) {
                        myDataGroups.remove(dataGroup);
                        tableChanged();
                    }
                    return false;
                }
                dataGroup.setName(nameFld.getText().trim());
                dataGroup.setDescription(descFld.getText().trim());
                List lines = StringUtil.split(paramsFld.getText(), "\n",
                                 true, true);
                List paramList = new ArrayList();
                for (int i = 0; i < lines.size(); i++) {
                    List toks = StringUtil.split(lines.get(i).toString(),
                                    ",", true, true);
                    if (toks.size() > 0) {
                        paramList.add(toks);
                    }
                }
                dataGroup.setParamSets(paramList);

                break;
            }

            repaint();
            saveData();
            return true;

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
                    DataGroup dataGroup = getDataGroup(row);
                    if ( !SwingUtilities.isRightMouseButton(e)) {
                        if ((e.getClickCount() > 1) && (dataGroup != null)) {
                            if (isEditable) {
                                editRow(dataGroup);
                            } else {
                                copyToUsers(dataGroup);
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
            DataGroup info = getDataGroup(row);
            if (isEditable) {
                makeEditableMenu(popup, row);
            } else {
                JMenuItem mi = new JMenuItem("Copy Row to Users Defaults");
                mi.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent ae) {
                        try {
                            copyToUsers(getDataGroup(row));
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
         * Find the DataGroup from the name
         *
         * @param name name
         *
         * @return param info
         */
        public DataGroup findByName(String name) {
            name = name.trim();
            for (int i = 0; i < myDataGroups.size(); i++) {
                DataGroup dataGroup = (DataGroup) myDataGroups.get(i);
                if (dataGroup.getName().equals(name)) {
                    return dataGroup;
                }
            }
            return null;
        }

        /**
         * Add the DataGroup into the table
         *
         * @param i
         */
        public void add(DataGroup i) {
            if ( !myDataGroups.contains(i)) {
                myDataGroups.add(i);
            }
            tableChanged();
        }



        /**
         * Add the DataGroup into the table
         *
         * @param i
         */
        public void addBeginning(DataGroup i) {
            myDataGroups.add(0, i);
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
            DataGroup dataGroup = getDataGroup(row);
            if (dataGroup == null) {
                return;
            }

            popup.add(GuiUtils.makeMenuItem("Edit Settings", this, "editRow",
                                            dataGroup));

            mi = new JMenuItem("Delete Parameter Group");
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
            DataGroup dataGroup = new DataGroup("");
            myDataGroups.add(dataGroup);
            tableChanged();
            editRow(dataGroup, true);
        }


        /**
         * Remove the given row
         *
         * @param row The given row
         */
        protected void removeRow(int row) {
            if ( !GuiUtils.showYesNoDialog(
                    null,
                    "Are you sure you want to delete this parameter group?",
                    "Delete Confirmation")) {
                return;
            }
            myDataGroups.remove(row);
            tableChanged();
            saveData();
        }



        /**
         * Table changed
         */
        public void tableChanged() {
            tableModel.fireTableStructureChanged();
        }



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
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "close"));


        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        helpMenu.add(GuiUtils.makeMenuItem("Parameter Defaults Help", this,
                                           "showHelp"));
        JComponent bottom = GuiUtils.wrap(GuiUtils.makeButton("Close", this,
                                "close"));
        contents = GuiUtils.topCenterBottom(menuBar,
                                            GuiUtils.inset(tableTabbedPane,
                                                2), bottom);
        setMenuBar(menuBar);
        loadResources(resources);
    }


    /**
     * Load the resources
     *
     * @param resources resources
     */
    private void loadResources(XmlResourceCollection resources) {
        for (int i = 0; i < resources.size(); i++) {
            List    groups;
            boolean isWritable = resources.isWritableResource(i);
            Element root       = resources.getRoot(i);
            if (root != null) {
                groups = DataGroup.readGroups(root, new Hashtable(), false);
            } else {
                if ( !isWritable) {
                    continue;
                }
                groups = new ArrayList();
            }

            if (groups.size() == 0) {
                //            groups.add(new DataGroup("", null, null, null, null));
            }
            JTable table = new ParamGroupsTable(groups, isWritable);
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

            tableTabbedPane.add(resources.getShortName(i), tablePanel);
            myTables.add(table);
        }
    }



    /**
     * Export the selected param infos to the plugin manager
     */
    public void exportSelectedToPlugin() {
        ParamGroupsTable table    = getCurrentTable();
        List             selected = table.getSelectedDataGroupList();
        System.err.println("selected:" + selected);
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
        ParamGroupsTable table = getCurrentTable();
        List             list  = table.getDataGroupList();
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
                ((ParamGroupsTable) myTables.get(0)));
            ((ParamGroupsTable) myTables.get(0)).addNewRow();

        }

    }

    /**
     * Get the param infos
     *
     * @param justFirst if true then just get the first table
     *
     * @return param infos
     */
    public List getDataGroups(boolean justFirst) {
        List infos = new ArrayList();
        for (int i = 0; i < myTables.size(); i++) {
            infos.addAll(
                ((ParamGroupsTable) myTables.get(i)).getDataGroupList());
            if (justFirst) {
                break;
            }
        }
        return infos;
    }



    /**
     * Return the ParamGroupsTable which is currently being shown in the tabbed pane
     * @return The current  ParamGroupsTable
     */
    public ParamGroupsTable getCurrentTable() {
        int index = tableTabbedPane.getSelectedIndex();
        return (ParamGroupsTable) myTables.get(index);
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
            //            List               infos = createDataGroupList(root);
            //            ParamGroupsTable table = getCurrentTable();
            //            table.getDataGroupList().addAll(infos);
            //            table.tableChanged();
            //            saveData();
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
        //TODO        addList(resources.getRoot(index), index);
    }


    /**
     * Handle the CLOSEANCEL, OK, HELP,  events.
     *
     * @param event The event
     */
    public void actionPerformed(ActionEvent event) {
        String cmd = event.getActionCommand();
        if (cmd.equals(GuiUtils.CMD_CLOSE)) {
            close();
        } else if (cmd.equals(GuiUtils.CMD_NEW)) {
            if (myTables.size() > 0) {
                ((ParamGroupsTable) myTables.get(0)).addNewRow();
            }
        } else if (cmd.equals(GuiUtils.CMD_OK)) {
            saveData();
            close();
        } else if (cmd.equals(GuiUtils.CMD_HELP)) {
            showHelp();
        } else if (cmd.equals(GuiUtils.CMD_SAVEAS)) {
            doSaveAs(getCurrentTable().getDataGroupList());
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
            "idv.tools.parametergroupseditor");
    }


    /**
     * Get the window title to use
     *
     * @return Window title
     */
    protected String getWindowTitle() {
        return GuiUtils.getApplicationTitle() + "Parameter Groups Editor";
    }



    /**
     * Save the list of DataGroup-s into the given file
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
        doSaveAs(getCurrentTable().getDataGroupList());
    }

    /**
     * Prompt for a file and write out the DataGroup-s from
     * the given list.
     *
     * @param infoList List of DataGroup-s
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
     * Copy the given DataGroup object into the user's editable table
     *
     * @param i the param fino object to copy
     */
    protected void copyToUsers(DataGroup i) {
        DataGroup        copy = new DataGroup(i);
        ParamGroupsTable to   = (ParamGroupsTable) myTables.get(0);
        to.add(copy);
        tableTabbedPane.setSelectedIndex(0);
        to.editRow(copy, true);
    }





    /**
     * Create xml dom from the given list of {@link ucar.unidata.idv.ui.DataGroup}-s
     *
     * @param doc The document to write to
     * @param paramGroups List of param infos
     * @return Root xml element
     */
    private Element createDom(Document doc, List paramGroups) {
        Element root = doc.createElement(TAG_PARAMGROUPS);
        for (int i = 0; i < paramGroups.size(); i++) {
            DataGroup dataGroup = (DataGroup) paramGroups.get(i);
            root.appendChild(dataGroup.getElement(doc));
        }
        return root;
    }



    /**
     * Get the first JTable in the list
     *
     * @return First table
     */
    private ParamGroupsTable getFirstTable() {
        return (ParamGroupsTable) myTables.get(0);
    }

    /**
     * Write out the user's editable param infos
     */
    private void saveData() {
        Document usersDoc = XmlUtil.makeDocument();
        Element usersRoot = createDom(usersDoc,
                                      getFirstTable().getDataGroupList());
        try {
            resources.setWritableDocument(usersDoc, usersRoot);
            resources.writeWritable();
            //Reinitialize the static state
            DataGroup.init(resources, true);
            getIdv().getJythonManager().dataGroupsChanged();
        } catch (Exception exc) {
            LogUtil.printException(log_, "writing aliases xml", exc);
        }

    }










}
