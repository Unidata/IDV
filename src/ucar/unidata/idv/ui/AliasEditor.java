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



import ucar.unidata.data.DataAlias;



import ucar.unidata.idv.*;


import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlResourceCollection;

import ucar.unidata.xml.XmlUtil;

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
 * This class is used to edit the set of {@link ucar.unidata.data.DataAlias}-s
 * used.
 *
 *
 *
 * @author IDV development team
 * @version $Revision: 1.45 $Date: 2007/06/21 14:45:01 $
 */


public class AliasEditor extends IdvManager {

    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(AliasEditor.class.getName());



    /** This is the resource collection that points to the alias.xml files */
    XmlResourceCollection resources;

    /** The list of {@link AliasTableModel}-s, one for each resource */
    private List tableModels = new ArrayList();

    /** list of table models */
    private List<AliasTableModel> displayedTableModels =
        new ArrayList<AliasTableModel>();

    /** The list of JTable-s, one for each resource */
    private List tables = new ArrayList();

    /** The tabbed pane that holds the JTables */
    private JTabbedPane tabbedPane;


    /**
     * Create an AliasEditor with the given idv and list of resources.
     *
     * @param idv Reference to the idv
     */
    public AliasEditor(IntegratedDataViewer idv) {
        super(idv);
        this.resources = idv.getResourceManager().getXmlResources(
            IdvResourceManager.RSC_ALIASES);
        this.resources = resources;
        init();
    }

    /**
     * Get the title to use for the iwindow
     *
     * @return window title
     */
    public String getWindowTitle() {
        return GuiUtils.getApplicationTitle() + "Alias Editor";
    }


    /**
     * Add the alias to the dataalias. Find the existing alias and popup the edit dialog
     *
     * @param dataAlias data alias
     * @param alias new alias
     */
    public void addAsAlias(DataAlias dataAlias, String alias) {
        boolean         first = true;
        AliasTableModel to    = getTableModel(0);
        for (AliasTableModel model : displayedTableModels) {
            int index = model.indexOfName(dataAlias.getName());
            if (index >= 0) {
                if (first) {
                    model.addAlias(index, alias);
                    editEntry(model, index, false);
                    return;
                }
                String aliases = (String) model.aliases.get(index);
                aliases += "," + alias;
                to.add(dataAlias.getName(), dataAlias.getLabel(), aliases);
                //                tabbedPane.setSelectedIndex(0);
                editEntry(to, to.getRowCount() - 1, true);
                return;
            }
            first = false;
        }

        //        tabbedPane.setSelectedIndex(0);
        editEntry(null, 0, true, dataAlias.getName(), alias);
    }


    /**
     * Create a new alias
     */
    public void newAlias() {
        tabbedPane.setSelectedIndex(0);
        editEntry(null, 0, true);
    }

    /**
     * get the list of resources
     *
     * @return resources
     */
    public List getResources() {
        List aliases = new ArrayList();
        for (AliasTableModel model : displayedTableModels) {
            for (DataAlias dataAlias : (List<DataAlias>) model.aliases) {
                aliases.add(new ResourceViewer.ResourceWrapper(dataAlias,
                        dataAlias.toString(), model.label,
                        isEditableResource(model.resourceIdx)));
            }
        }
        return aliases;
    }


    /**
     * Initialize. Load in the resources and create the GUI.
     */
    private void init() {
        tabbedPane = new JTabbedPane();


        int count = 0;
        for (int resourceIdx = 0; resourceIdx < resources.size();
                resourceIdx++) {
            Element root       = resources.getRoot(resourceIdx);
            boolean resourceOk = true;
            if ((root == null) && (resourceIdx > 0)) {
                resourceOk = false;
            }

            AliasTableModel tableModel = new AliasTableModel(this,
                                             resourceIdx);


            tableModel.label = "" + resources.get(resourceIdx);
            tableModels.add(tableModel);
            if (root != null) {
                List dataAliases = DataAlias.createDataAliases(root);
                for (int aliasIdx = 0; aliasIdx < dataAliases.size();
                        aliasIdx++) {
                    DataAlias dataAlias =
                        (DataAlias) dataAliases.get(aliasIdx);
                    String name    = dataAlias.getName();
                    List   aliases = dataAlias.getAliases();
                    tableModel.add(name, dataAlias.getLabel(),
                                   StringUtil.join(", ", aliases));
                }
            }

            JTable table = createTable(resourceIdx, tableModel);
            JScrollPane sp =
                new JScrollPane(
                    table, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

            JComponent contents    = sp;

            String     editableStr = "";
            if ( !isEditableResource(resourceIdx)) {
                table.setToolTipText(
                    "Right click or double click to copy to local alias list");
                editableStr = " (non-editable)";
            } else {
                table.setToolTipText(
                    "<html>" + Msg.msg("Right click to edit or delete")
                    + "<br>" + Msg.msg("Double click to edit") + "</html>");
            }
            String label = Msg.msg("Path: ${param1}",
                                   resources.get(resourceIdx) + editableStr);
            contents = GuiUtils.topCenter(GuiUtils.inset(new JLabel("<html>"
                    + label + "</html>"), 5), sp);


            //We go ahead and create the jtable for each resource.
            //however,  we only add it into the gui if we really have the resource
            if (resourceOk) {
                displayedTableModels.add(tableModel);
                tabbedPane.add(resources.getShortName(resourceIdx), contents);
            }
        }

        JMenuBar menuBar  = new JMenuBar();
        JMenu    fileMenu = new JMenu("File");
        JMenu    helpMenu = new JMenu("Help");
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        fileMenu.add(GuiUtils.makeMenuItem("New Alias", this, "newAlias"));
        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Export to Plugin", this,
                                           "exportToPlugin"));
        fileMenu.add(GuiUtils.makeMenuItem("Export Selected to Plugin", this,
                                           "exportSelectedToPlugin"));


        fileMenu.addSeparator();
        fileMenu.add(GuiUtils.makeMenuItem("Close", this, "close"));

        helpMenu.add(GuiUtils.makeMenuItem("Show Data Alias Help", this,
                                           "showHelp"));

        JComponent bottom = GuiUtils.wrap(GuiUtils.makeButton("Close", this,
                                "close"));
        contents = GuiUtils.topCenterBottom(menuBar, tabbedPane, bottom);

        setMenuBar(menuBar);
    }



    /**
     * Is the resource, identified by the given resourceIdx, editable.
     *
     * @param resourceIdx The resource to check for editability
     * @return Is the resource editable
     */
    private boolean isEditableResource(int resourceIdx) {
        //For now just the first resource is the editable one.
        return (resourceIdx == 0);
    }



    /**
     * Create a JTable for the {@link ucar.unidata.data.DataAlias}-s
     * defined by the given resource index.
     *
     * @param resourceIdx Which resource
     * @param tableModel The table model to use
     * @return The newly created JTable
     */
    private JTable createTable(final int resourceIdx,
                               AliasTableModel tableModel) {
        final JTable table = new JTable(tableModel);
        tableModel.table = table;
        tables.add(table);
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                handleMouseEvent(e, table, resourceIdx);
            }
        });


        if ( !isEditableResource(resourceIdx)) {
            return table;
        }

        table.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (GuiUtils.isDeleteEvent(e)) {
                    getTableModel(resourceIdx).remove(
                        table.getSelectedRows());
                }
            }
        });


        table.getColumnModel().getColumn(0).setPreferredWidth(25);
        table.getColumnModel().getColumn(1).setPreferredWidth(125);
        return table;
    }



    /**
     * If the given MouseEvent is a right mouse click then
     * popup the menu.
     *
     * @param e The event
     * @param table The JTable clicked on
     * @param resourceIdx Which resource
     */
    private void handleMouseEvent(MouseEvent e, JTable table,
                                  final int resourceIdx) {
        final int row = table.rowAtPoint(e.getPoint());
        if ( !SwingUtilities.isRightMouseButton(e)) {
            if (e.getClickCount() > 1) {
                if (isEditableResource(resourceIdx)) {
                    editEntry(getTableModel(resourceIdx), row, false);
                } else {
                    copyEntry(resourceIdx, row);
                }
            }
            return;
        }
        table.getSelectionModel().setSelectionInterval(row, row);
        JPopupMenu popup = new JPopupMenu();
        JMenuItem  mi    = null;
        if (isEditableResource(resourceIdx)) {
            mi = new JMenuItem("Edit Entry");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    editEntry(getTableModel(resourceIdx), row, false);
                }
            });
            popup.add(mi);
            mi = new JMenuItem("Delete Entry");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    removeEntry(resourceIdx, row);
                }
            });
            popup.add(mi);
        } else {
            mi = new JMenuItem("Edit Alias");
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    copyEntry(resourceIdx, row);
                }
            });

            popup.add(mi);
        }
        popup.show((Component) e.getSource(), e.getX(), e.getY());
    }


    /**
     * Popup the edit dialog for the given resource and alias
     *
     * @param tableModel The table model to edit
     * @param row The row to edit
     * @param deleteOnCancel delete entry if cancel is pressed
     */
    private void editEntry(AliasTableModel tableModel, int row,
                           boolean deleteOnCancel) {
        editEntry(tableModel, row, deleteOnCancel, "", "");
    }

    /**
     * Popup the edit dialog for the given resource and alias
     *
     * @param tableModel The table model to edit
     * @param row The row to edit
     * @param deleteOnCancel delete entry if cancel is pressed
     * @param name The name to use if this is a new one
     * @param aliases The aliases to use if new
     */
    private void editEntry(AliasTableModel tableModel, int row,
                           boolean deleteOnCancel, String name,
                           String aliases) {


        boolean newEntry = tableModel == null;

        String  label    = "";

        if ( !newEntry) {
            name    = tableModel.getName(row);
            label   = tableModel.getLabel(row);
            aliases = tableModel.getString(row);
        }
        //Turn the "," into "\n"
        aliases = StringUtil.join("\n",
                                  StringUtil.split(aliases, ",", true, true));
        JTextField      nameFld    = new JTextField(name, 15);
        JTextField      labelFld   = new JTextField(label, 15);
        final JTextArea aliasesFld = new JTextArea(aliases, 15, 10);
        aliasesFld.setToolTipText(
            "<html>Enter parameter name, one per line<br>Right mouse to add current parameters</html>");
        aliasesFld.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    IdvUIManager.showParamsPopup(aliasesFld, e, "\n", false);
                }
            }
        });
        JScrollPane sp =
            new JScrollPane(
                aliasesFld, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);


        sp.setPreferredSize(new Dimension(150, 100));
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel p = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Name: "), GuiUtils.left(nameFld),
            GuiUtils.rLabel("Label: "), GuiUtils.left(labelFld),
            GuiUtils.filler(), new JLabel("Enter aliases one per line:"),
            GuiUtils.filler(), sp
        }, 2, GuiUtils.WT_N, GuiUtils.WT_NNNY);

        if ( !GuiUtils.showOkCancelDialog(null, "Data Alias", p, null)) {
            if (deleteOnCancel && !newEntry) {
                removeEntry(tableModel, row);
            }
            return;
        }
        name  = nameFld.getText().trim();
        label = labelFld.getText().trim();
        //Turn the "\n" into ","
        aliases = StringUtil.join(",",
                                  StringUtil.split(aliasesFld.getText(),
                                      "\n", true, true));

        if ( !newEntry) {
            tableModel.set(row, name, label, aliases);
        } else {
            tableModel = getTableModel(0);
            tableModel.add(name, label, aliases);
        }
        saveAliases();
    }





    /**
     * show help
     */
    public void showHelp() {
        getIdvUIManager().showHelp("idv.tools.aliaseditor");
    }




    /**
     * Get the table model for the given resource
     *
     * @param index The resource index
     * @return The table model at the index
     */
    private AliasTableModel getTableModel(int index) {
        return (AliasTableModel) tableModels.get(index);
    }

    /**
     * Get the JTable for the given resource
     *
     * @param index The resource index
     * @return The JTable at the index
     */
    private JTable getTable(int index) {
        return (JTable) tables.get(index);
    }



    /**
     * Handle the Cancel event
     */
    private void doCancel() {
        removeAll();
        init();
    }

    /**
     * Copy the entry from the given resource/row into the first table
     *
     * @param resourceIdx The resource to copy from
     * @param row The  row to copy from
     */
    private void copyEntry(int resourceIdx, int row) {
        AliasTableModel from = getTableModel(resourceIdx);
        AliasTableModel to   = getTableModel(0);
        to.add((String) from.names.get(row), (String) from.labels.get(row),
               (String) from.aliases.get(row));
        tabbedPane.setSelectedIndex(0);
        editEntry(to, to.getRowCount() - 1, true);
    }

    /**
     * Remove the entry from the given resource/row
     *
     * @param resourceIdx The resource to copy from
     * @param row The  row to copy from
     */
    private void removeEntry(int resourceIdx, int row) {
        removeEntry(getTableModel(resourceIdx), row);
    }


    /**
     * remove entry
     *
     * @param from which table
     * @param row kthe row
     */
    private void removeEntry(AliasTableModel from, int row) {
        from.remove(row);
        saveAliases();
    }

    /**
     * Write out the aliases from the first table
     */
    private void saveAliases() {
        AliasTableModel from = getTableModel(0);
        from.saveAliases();
    }


    /**
     * export the current list to the plugin manager
     */
    public void exportToPlugin() {
        int index = tabbedPane.getSelectedIndex();
        AliasTableModel from =
            (AliasTableModel) displayedTableModels.get(index);
        from.exportToPlugin(getIdv().getPluginManager());
    }

    /**
     * export the selected list to the plugin manager
     */
    public void exportSelectedToPlugin() {
        int index = tabbedPane.getSelectedIndex();
        AliasTableModel from =
            (AliasTableModel) displayedTableModels.get(index);
        from.exportSelectedToPlugin(getIdv().getPluginManager());
    }


    /**
     * Class AliasTableModel. This extends AbstractTableModel and
     * manages the data for one collection of
     * {@link DataAlias}-s
     *
     * @author IDV development team
     */
    private static class AliasTableModel extends AbstractTableModel {

        /** _more_ */
        String label;

        /** The names of the data aliases */
        List names = new ArrayList();

        /** The labels of the data aliases */
        List labels = new ArrayList();

        /** List of comma separated alias names */
        List aliases = new ArrayList();

        /** What resource we represent */
        int resourceIdx;

        /** Back reference to the main editor */
        AliasEditor editor;

        /** the jtable */
        JTable table;


        /**
         * Create the table mode
         *
         * @param editor The main editor
         * @param resourceIdx The resource this table model represents
         */
        public AliasTableModel(AliasEditor editor, int resourceIdx) {
            this.editor      = editor;
            this.resourceIdx = resourceIdx;
        }

        /**
         * Add the given name, label and alias to the lists of data and
         * fire a TableStructureChanged event
         *
         * @param name
         * @param label
         * @param aliases
         */
        public void add(String name, String label, String aliases) {
            this.names.add(name);
            this.labels.add(label);
            this.aliases.add(aliases);
            fireTableStructureChanged();
        }


        /**
         * _more_
         *
         * @param row _more_
         * @param alias _more_
         */
        public void addAlias(int row, String alias) {
            String aliases = (String) this.aliases.get(row);
            aliases = aliases + "," + alias;
            this.aliases.set(row, aliases);
        }


        /**
         * Copy the given alias information into the lists.
         *
         * @param row Which row
         * @param name The alias name
         * @param label The alias label
         * @param aliases The comma separated aliases
         */
        public void set(int row, String name, String label, String aliases) {
            this.names.set(row, name);
            this.labels.set(row, label);
            this.aliases.set(row, aliases);
            fireTableStructureChanged();
        }

        /**
         * Get the data alias name for the given row
         *
         * @param row The table row
         * @return The name
         */
        public String getName(int row) {
            return (String) names.get(row);
        }

        /**
         * Get the data alias label for the given row
         *
         * @param row The table row
         * @return The label
         */
        public String getLabel(int row) {
            return (String) labels.get(row);
        }

        /**
         * Get the comma separated alias String for the given row
         *
         * @param row The table row
         * @return The alias
         */
        public String getString(int row) {
            return (String) aliases.get(row);
        }

        /**
         * Create a new list which is the given list with elements defined by the given
         * the indices array removed
         *
         * @param from The list to remove elements from
         * @param indices The indexes to remove
         * @return The new list
         */
        private List remove(List from, int[] indices) {
            List tmp = new ArrayList();
            for (int i = 0; i < from.size(); i++) {
                boolean isIndexIn = false;
                for (int j = 0; (j < indices.length) && !isIndexIn; j++) {
                    isIndexIn = (indices[j] == i);
                }
                if (isIndexIn) {
                    continue;
                }
                tmp.add(from.get(i));
            }
            return tmp;
        }

        /**
         * Remove from the data lists the indices in the given rows argument
         *
         * @param rows The indices to remove
         */
        public void remove(int[] rows) {
            if (names.size() == 0) {
                return;
            }
            names   = remove(names, rows);
            labels  = remove(labels, rows);
            aliases = remove(aliases, rows);
            fireTableStructureChanged();
            int min = Integer.MAX_VALUE;
            for (int i = 0; i < rows.length; i++) {
                if (rows[i] < min) {
                    min = rows[i];
                }
            }
            while (min >= names.size()) {
                min--;
            }
            if (min >= 0) {
                editor.getTable(resourceIdx).setRowSelectionInterval(min,
                                min);
            }

        }

        /**
         * Remvoe from the data lists the given row
         *
         * @param row The row to remove
         */
        public void remove(int row) {
            names.remove(row);
            labels.remove(row);
            aliases.remove(row);
            fireTableStructureChanged();
        }


        /**
         * _more_
         *
         * @param name _more_
         *
         * @return _more_
         */
        public int indexOfName(String name) {
            return names.indexOf(name);
        }

        /**
         * Create and write out the collection of {@link ucar.uinidata.data.DataAlias}-s
         * defined by this table model.
         */
        private void saveAliases() {
            Document doc  = XmlUtil.makeDocument();
            Element  root = doc.createElement(DataAlias.TAG_ALIASES);
            for (int i = 0; i < names.size(); i++) {
                String  name      = (String) this.names.get(i);
                String  aliases   = (String) this.aliases.get(i);
                String  label     = (String) this.labels.get(i);
                Element aliasNode = doc.createElement(DataAlias.TAG_ALIAS);
                aliasNode.setAttribute(DataAlias.ATTR_NAME, name);
                aliasNode.setAttribute(DataAlias.ATTR_LABEL, label);
                aliasNode.setAttribute(DataAlias.ATTR_ALIASES, aliases);
                root.appendChild(aliasNode);
            }
            try {
                editor.resources.setWritableDocument(doc, root);
                editor.resources.writeWritable();
                DataAlias.reInit(editor.resources);
            } catch (Exception exc) {
                LogUtil.printException(log_, "writing aliases xml", exc);
            }

        }


        /**
         * Make xml of all or just selected
         *
         * @param selected If true just use the selected
         *
         * @return xml
         */
        private Element makeXml(boolean selected) {
            Document doc     = XmlUtil.makeDocument();
            Element  root    = doc.createElement(DataAlias.TAG_ALIASES);
            int[]    indices = null;

            if (selected) {
                indices = table.getSelectedRows();
            }
            for (int i = 0; i < ((indices != null)
                                 ? indices.length
                                 : names.size()); i++) {
                int     index     = ((indices != null)
                                     ? indices[i]
                                     : i);
                String  name      = (String) this.names.get(index);
                String  aliases   = (String) this.aliases.get(index);
                String  label     = (String) this.labels.get(index);
                Element aliasNode = doc.createElement(DataAlias.TAG_ALIAS);
                aliasNode.setAttribute(DataAlias.ATTR_NAME, name);
                aliasNode.setAttribute(DataAlias.ATTR_LABEL, label);
                aliasNode.setAttribute(DataAlias.ATTR_ALIASES, aliases);
                root.appendChild(aliasNode);
            }
            return root;
        }



        /**
         *
         * @param pluginManager the plugin manager to export to
         */
        private void exportToPlugin(PluginManager pluginManager) {
            Element root = makeXml(false);
            pluginManager.addText(XmlUtil.toString(root), "aliases.xml");
        }

        /**
         *
         * @param pluginManager the plugin manager to export to
         */
        private void exportSelectedToPlugin(PluginManager pluginManager) {
            Element root = makeXml(true);
            pluginManager.addText(XmlUtil.toString(root), "aliases.xml");
        }



        /**
         * How many rows
         *
         * @return  How many rows
         */
        public int getRowCount() {
            return names.size();
        }

        /**
         * How many columns
         *
         * @return How many columns
         */
        public int getColumnCount() {
            return 3;
        }

        /**
         * Insert the given value into the appropriate data list
         *
         * @param aValue The value
         * @param rowIndex The row
         * @param columnIndex The column
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                labels.set(rowIndex, aValue.toString());
            } else {
                aliases.set(rowIndex, aValue.toString());
            }
        }

        /**
         * Return the value at the given row/column
         *
         * @param row The row
         * @param column The column
         *
         * @return The value
         */
        public Object getValueAt(int row, int column) {
            if (column == 0) {
                return names.get(row);
            }
            if (column == 1) {
                return labels.get(row);
            }
            if (column == 2) {
                return aliases.get(row);
            }
            return "";
        }

        /**
         * Get the name of the given column
         *
         * @param column The column
         * @return Its name
         */
        public String getColumnName(int column) {
            return ((column == 0)
                    ? "Name"
                    : ((column == 1)
                       ? "Description"
                       : "Aliases"));
        }

    }





}
