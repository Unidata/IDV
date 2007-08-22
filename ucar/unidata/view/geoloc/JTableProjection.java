/*
 * $Id: JTableProjection.java,v 1.25 2006/12/27 20:00:08 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.view.geoloc;


import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.ListenerManager;
import ucar.unidata.util.Msg;
import ucar.unidata.util.TwoFacedObject;



import java.awt.*;
import java.awt.event.*;

//import java.io.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


/**
 *  Consider this a private inner class of ProjectionManager.
 *
 * @author John Caron
 * @version $Id: JTableProjection.java,v 1.25 2006/12/27 20:00:08 jeffmc Exp $
 */

public class JTableProjection extends JTable {

    /** table model */
    private ProjectionTableModel model = null;

    /** debug flag */
    private boolean debug = false;


    /** Listener manager */
    private ListenerManager lm;

    /** The proj manager */
    private ProjectionManager projectionManager;


    /** flag for ignoring events */
    boolean ignoreEvents = false;



    /**
     * Create a new JTableProjection with the list of projections and
     * a default.
     *
     * @param manager The projection manager
     * @param list   list of projections
     */
    public JTableProjection(ProjectionManager manager, List list) {
        this.projectionManager = manager;
        model                  = new ProjectionTableModel(list);
        init();
        setToolTipText("<html>" + Msg.msg("Double-click to edit") + ";"
                       + Msg.msg("Delete key to delete") + "</html>");
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    deleteSelected();
                }
            }
        });
        addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                if (e.getClickCount() > 1) {
                    projectionManager.doEdit();
                }
            }
        });




    }



    /**
     * Set the list of projections in the table
     *
     * @param list  list of projections
     */
    public void setProjections(List list) {
        setModel(model = new ProjectionTableModel(list));
    }



    /**
     * Get the top panel which holds the export button
     * @return the top panel
     */
    public JComponent getTopPanel() {
        JButton exportBtn = new JButton("Export");
        exportBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                projectionManager.doExport();
            }
        });
        return exportBtn;
    }


    /**
     * Initialize the class
     */
    private void init() {
        setModel(model);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setPreferredScrollableViewportSize(new Dimension(400, 200));
        getTableHeader().setReorderingAllowed(true);
        model.adjustColumns(getColumnModel());

        // manage the selection
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCurrentProjection((ProjectionImpl) model.getProjections().get(0));

        // have to manage selectedRow ourselves, due to bugs
        getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if ( !e.getValueIsAdjusting()) {
                    lm.sendEvent(new NewProjectionEvent(this, getSelected()));
                }
            }
        });

        // manage NewProjectionListener's
        lm = new ucar.unidata.util.ListenerManager(
            "ucar.unidata.view.geoloc.NewProjectionListener",
            "ucar.unidata.view.geoloc.NewProjectionEvent", "actionPerformed");
    }

    /**
     * Get the projections in the table.
     * @return <code>List</code> of projections.
     */
    public List getProjections() {
        return model.getProjections();
    }




    /**
     * Add a projection to the table.
     *
     * @param proj  projection to add
     */
    public void addProjection(ProjectionImpl proj) {
        int rowno = model.addProjection(proj);
        setRowSelectionInterval(rowno, rowno);
    }

    /**
     * Replace a projection in the table.
     *
     * @param proj  projection to replace.
     */
    public void replaceProjection(ProjectionImpl proj) {
        int rowno = model.replaceProjection(proj);
        setRowSelectionInterval(rowno, rowno);
    }

    /**
     * See if a projection is in the table.
     *
     * @param proj   projection to check
     * @return true if it is in the table.
     */
    public boolean contains(ProjectionImpl proj) {
        return (model.search(proj) >= 0);
    }

    /**
     * See if the table contains the id for the projection
     *
     * @param id    id (name) to check
     * @return true if the name is there.
     */
    public boolean contains(String id) {
        return (model.search(id) >= 0);
    }

    /**
     * Get the selected projection in the table.
     * @return selected projection (may be null)
     */
    public ProjectionImpl getSelected() {
        List list        = getProjections();
        int  selectedRow = getSelectedRow();
        if ((0 > selectedRow) || (list.size() <= selectedRow)) {
            return null;
        } else {
            return (ProjectionImpl) list.get(selectedRow);
        }
    }

    /**
     * Delete the selected projection in the table.
     */
    public void deleteSelected() {
        List list        = getProjections();
        int  selectedRow = getSelectedRow();
        if ((selectedRow) < 0 || (selectedRow >= list.size())) {
            return;
        }
        model.deleteRow(selectedRow);
        list = getProjections();
        selectedRow--;
        if (selectedRow < 0) {
            selectedRow = 0;
        }
        if ((selectedRow >= 0) && (selectedRow < list.size())) {
            setRowSelectionInterval(selectedRow, selectedRow);
        }
        lm.sendEvent(new NewProjectionEvent(this, getSelected()));
        repaint();
    }

    /**
     * Check if the table is empty.
     * @return true if it is empty.
     */
    public boolean isEmpty() {
        return (model.getRowCount() == 0);
    }


    /**
     * Set the map area.
     *
     * @param bb   projection rectangle
     */
    public void setMapArea(ProjectionRect bb) {
        int selectedRow = getSelectedRow();
        if (0 > selectedRow) {
            return;
        }
        model.setMapArea(selectedRow, bb);
        if (debug) {
            System.out.println(" PTsetMapArea = " + bb + " on "
                               + selectedRow);
        }
    }


    /**
     * Set current projection if found, else deselect
     *
     * @param proj   projectiong to select
     */
    public void setCurrentProjection(ProjectionImpl proj) {
        int row;
        if (0 <= (row = model.search(proj))) {
            if (debug) {
                System.out.println(" PTsetCurrentProjection found = " + row);
            }
            setRowSelectionInterval(row, row);
        } else {
            if (debug) {
                System.out.println(" PTsetCurrentProjection not found = "
                                   + row);
            }
            clearSelection();
        }
    }

    /**
     * Add a new listener
     *
     * @param l   listener to add
     */
    public void addNewProjectionListener(NewProjectionListener l) {
        lm.addListener(l);
    }

    /**
     * Remove a listener.
     *
     * @param l   listener to remove.
     */
    public void removeNewProjectionListener(NewProjectionListener l) {
        lm.removeListener(l);
    }


    /**
     * Class ProjectionTableModel
     *
     * @author Unidata development team
     */
    private class ProjectionTableModel extends AbstractTableModel {

        /** colum name index */
        final int COLIDX_NAME = 0;


        /** projection class index */
        final int COLIDX_CLASS = 1;

        /** projection parameters index */
        final int COLIDX_PARAMS = 2;

        /** projection area index */
        final int COLIDX_AREA = 3;

        /** Array of column names */
        private String[] COLNAMES = { "Name", "Type", "Parameters",
                                      "Default Zoom" };

        /** list of projection */
        private List projections;

        /**
         * Create a new model
         *
         * @param list  list of projections
         *
         */
        ProjectionTableModel(List list) {
            if (list == null) {
                list = new ArrayList();
            }
            this.projections = list;
        }




        /**
         * Get the row count.
         * @return number of rows
         */
        public int getRowCount() {
            return projections.size();
        }

        /**
         * Get the column count
         * @return number of columns
         */
        public int getColumnCount() {
            return COLNAMES.length;
        }

        /**
         * Get the name of the column at the index
         *
         * @param col   column number (0 based)
         * @return name of the column
         */
        public String getColumnName(int col) {
            return COLNAMES[col];
        }

        /**
         * Get the value at the specified row/column position in the table.
         *
         * @param row    row number
         * @param col    column number
         * @return Object at that position
         */
        public Object getValueAt(int row, int col) {
            ProjectionImpl proj = (ProjectionImpl) projections.get(row);
            switch (col) {

              case COLIDX_NAME :
                  return proj.getName();

              case COLIDX_CLASS :
                  return proj.getProjectionTypeLabel();

              case COLIDX_PARAMS :
                  return proj.paramsToString();

              case COLIDX_AREA :
                  return proj.getDefaultMapArea();
            }
            return "error";
        }

        /**
         * Get the list of projections.
         * @return list of projections.
         */
        public List getProjections() {
            return projections;
        }

        /**
         * Check if a cell is editable.
         *
         * @param rowIndex       row index
         * @param columnIndex    column index
         * @return true if cell at row/colum is editable.
         */
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
            //            return (columnIndex == COLIDX_NAME);
        }

        /**
         * Set the value in a cell.
         *
         * @param aValue           value for cell
         * @param rowIndex         cell row
         * @param columnIndex      cell column
         */
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            ProjectionImpl proj = (ProjectionImpl) projections.get(rowIndex);
            if (columnIndex == COLIDX_NAME) {
                proj.setName(aValue.toString());
            }
        }

        // added methods

        /**
         * Add a projection to the model
         *
         * @param proj    projection to add
         * @return number of projections in table
         */
        int addProjection(ProjectionImpl proj) {
            projections.add(proj);
            int count = projections.size() - 1;
            fireTableRowsInserted(count, count);
            return count;
        }

        /**
         * Replace a projection in the model
         *
         * @param proj      projection to replace
         * @return row number of replaced projection or -1 if not in table
         */
        int replaceProjection(ProjectionImpl proj) {
            int rowno = search(proj);
            if (rowno < 0) {
                return -1;
            }
            projections.set(rowno, proj);
            return rowno;
        }

        /**
         * Adjust the columns
         *
         * @param colModel  column model
         */
        void adjustColumns(TableColumnModel colModel) {
            for (int i = 0; i < COLNAMES.length; i++) {
                colModel.getColumn(i).setMinWidth(50);
                colModel.getColumn(i).setPreferredWidth(100);
            }
        }

        /**
         * Delete a row
         *
         * @param row  row index to delete
         */
        void deleteRow(int row) {
            int len = projections.size();
            if (row < len) {
                projections.remove(row);
            } else {
                return;
            }
            fireTableRowsDeleted(row, row);
        }

        /**
         * Search for a projection in the table
         *
         * @param proj    projection to find
         * @return row number of projection or -1 if not found
         */
        int search(ProjectionImpl proj) {
            for (int row = 0; row < projections.size(); row++) {
                ProjectionImpl test = (ProjectionImpl) projections.get(row);
                if (proj.getName().equals(test.getName())) {
                    return row;
                }
            }
            return -1;
        }

        /**
         * Search for a projection by name
         *
         * @param projName   name of the projection
         * @return row number of projection or -1 if not found
         */
        int search(String projName) {
            for (int row = 0; row < projections.size(); row++) {
                ProjectionImpl test = (ProjectionImpl) projections.get(row);
                if (projName.equals(test.getName())) {
                    return row;
                }
            }
            return -1;
        }


        /**
         * Set the map area for a projection in the table
         *
         * @param row    row of projection
         * @param bb     bounding box.
         */
        void setMapArea(int row, ProjectionRect bb) {
            int len = projections.size();
            if (row >= len) {
                return;
            }
            ProjectionImpl proj = (ProjectionImpl) projections.get(row);
            proj.getDefaultMapArea().setRect(bb);
            fireTableRowsUpdated(row, row);
        }
    }
}

