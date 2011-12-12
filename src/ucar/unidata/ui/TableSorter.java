/**
 * TableSorter is a decorator for TableModels; adding sorting
 * functionality to a supplied TableModel. TableSorter does
 * not store or copy the data in its TableModel; instead it maintains
 * a map from the row indexes of the view to the row indexes of the
 * model. As requests are made of the sorter (like getValueAt(row, col))
 * they are passed to the underlying model after the row numbers
 * have been translated via the internal mapping array. This way,
 * the TableSorter appears to hold another copy of the table
 * with the rows in a different order.
 * <p/>
 * TableSorter registers itself as a listener to the underlying model,
 * just as the JTable itself would. Events recieved from the model
 * are examined, sometimes manipulated (typically widened), and then
 * passed on to the TableSorter's listeners (typically the JTable).
 * If a change to the model has invalidated the order of TableSorter's
 * rows, a note of this is made and the sorter will resort the
 * rows the next time a value is requested.
 * <p/>
 * When the tableHeader property is set, either by using the
 * setTableHeader() method or the two argument constructor, the
 * table header may be used as a complete UI for TableSorter.
 * The default renderer of the tableHeader is decorated with a renderer
 * that indicates the sorting status of each column. In addition,
 * a mouse listener is installed with the following behavior:
 * <ul>
 * <li>
 * Mouse-click: Clears the sorting status of all other columns
 * and advances the sorting status of that column through three
 * values: {NOT_SORTED, ASCENDING, DESCENDING} (then back to
 * NOT_SORTED again).
 * <li>
 * SHIFT-mouse-click: Clears the sorting status of all other columns
 * and cycles the sorting status of the column through the same
 * three values, in the opposite order: {NOT_SORTED, DESCENDING, ASCENDING}.
 * <li>
 * CONTROL-mouse-click and CONTROL-SHIFT-mouse-click: as above except
 * that the changes to the column do not cancel the statuses of columns
 * that are already sorting - giving a way to initiate a compound
 * sort.
 * </ul>
 * <p/>
 * This is a long overdue rewrite of a class of the same name that
 * first appeared in the swing table demos in 1997.
 *
 * @author Philip Milne
 * @author Brendon McLean
 * @author Dan van Enckevort
 * @author Parwinder Sekhon
 * @version 2.0 02/27/04
 */


/**
 *  Included into the IDV package framework.
 */

package ucar.unidata.ui;


import java.awt.*;
import java.awt.event.*;

import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.*;


/**
 * Class TableSorter _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.7 $
 */
public class TableSorter extends AbstractTableModel {

    /** _more_ */
    protected TableModel tableModel;

    /** _more_ */
    public static final int DESCENDING = -1;

    /** _more_ */
    public static final int NOT_SORTED = 0;

    /** _more_ */
    public static final int ASCENDING = 1;

    /** _more_ */
    private static Directive EMPTY_DIRECTIVE = new Directive(-1, NOT_SORTED);

    /** _more_ */
    public static final Comparator COMPARABLE_COMAPRATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return ((Comparable) o1).compareTo(o2);
        }
    };

    /** _more_ */
    public static final Comparator LEXICAL_COMPARATOR = new Comparator() {
        public int compare(Object o1, Object o2) {
            return o1.toString().compareTo(o2.toString());
        }
    };

    /** _more_ */
    private Row[] viewToModel;

    /** _more_ */
    private int[] modelToView;

    /** _more_ */
    private JTableHeader tableHeader;

    /** _more_ */
    private MouseListener mouseListener;

    /** _more_ */
    private TableModelListener tableModelListener;

    /** _more_ */
    private Map columnComparators = new HashMap();

    /** _more_ */
    private List sortingColumns = new ArrayList();

    /**
     * _more_
     */
    public TableSorter() {
        this.mouseListener      = new MouseHandler();
        this.tableModelListener = new TableModelHandler();
    }

    /**
     * _more_
     *
     * @param tableModel _more_
     */
    public TableSorter(TableModel tableModel) {
        this();
        setTableModel(tableModel);
    }

    /**
     * _more_
     *
     * @param tableModel _more_
     * @param tableHeader _more_
     */
    public TableSorter(TableModel tableModel, JTableHeader tableHeader) {
        this();
        setTableHeader(tableHeader);
        setTableModel(tableModel);
    }

    /**
     * _more_
     */
    private void clearSortingState() {
        viewToModel = null;
        modelToView = null;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public TableModel getTableModel() {
        return tableModel;
    }

    /**
     * _more_
     *
     * @param tableModel _more_
     */
    public void setTableModel(TableModel tableModel) {
        if (this.tableModel != null) {
            this.tableModel.removeTableModelListener(tableModelListener);
        }

        this.tableModel = tableModel;
        if (this.tableModel != null) {
            this.tableModel.addTableModelListener(tableModelListener);
        }

        clearSortingState();
        fireTableStructureChanged();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public JTableHeader getTableHeader() {
        return tableHeader;
    }

    /**
     * _more_
     *
     * @param tableHeader _more_
     */
    public void setTableHeader(JTableHeader tableHeader) {
        if (this.tableHeader != null) {
            this.tableHeader.removeMouseListener(mouseListener);
            TableCellRenderer defaultRenderer =
                this.tableHeader.getDefaultRenderer();
            if (defaultRenderer instanceof SortableHeaderRenderer) {
                this.tableHeader
                    .setDefaultRenderer(
                        ((SortableHeaderRenderer) defaultRenderer)
                            .tableCellRenderer);
            }
        }
        this.tableHeader = tableHeader;
        if (this.tableHeader != null) {
            this.tableHeader.addMouseListener(mouseListener);
            this.tableHeader.setDefaultRenderer(
                new SortableHeaderRenderer(
                    this.tableHeader.getDefaultRenderer()));
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSorting() {
        return sortingColumns.size() != 0;
    }

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    private Directive getDirective(int column) {
        for (int i = 0; i < sortingColumns.size(); i++) {
            Directive directive = (Directive) sortingColumns.get(i);
            if (directive.column == column) {
                return directive;
            }
        }
        return EMPTY_DIRECTIVE;
    }

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public int getSortingStatus(int column) {
        return getDirective(column).direction;
    }

    /**
     * _more_
     */
    private void sortingStatusChanged() {
        clearSortingState();
        fireTableDataChanged();
        if (tableHeader != null) {
            tableHeader.repaint();
        }
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param status _more_
     */
    public void setSortingStatus(int column, int status) {
        Directive directive = getDirective(column);
        if (directive != EMPTY_DIRECTIVE) {
            sortingColumns.remove(directive);
        }
        if (status != NOT_SORTED) {
            sortingColumns.add(new Directive(column, status));
        }
        sortingStatusChanged();
    }

    /**
     * _more_
     *
     * @param column _more_
     * @param size _more_
     *
     * @return _more_
     */
    protected Icon getHeaderRendererIcon(int column, int size) {
        Directive directive = getDirective(column);
        if (directive == EMPTY_DIRECTIVE) {
            return null;
        }
        return new Arrow(directive.direction == DESCENDING, size,
                         sortingColumns.indexOf(directive));
    }

    /**
     * _more_
     */
    private void cancelSorting() {
        sortingColumns.clear();
        sortingStatusChanged();
    }

    /**
     * _more_
     *
     * @param type _more_
     * @param comparator _more_
     */
    public void setColumnComparator(Class type, Comparator comparator) {
        if (comparator == null) {
            columnComparators.remove(type);
        } else {
            columnComparators.put(type, comparator);
        }
    }

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    protected Comparator getComparator(int column) {
        Class      columnType = tableModel.getColumnClass(column);
        Comparator comparator =
            (Comparator) columnComparators.get(columnType);
        if (comparator != null) {
            return comparator;
        }
        if (Comparable.class.isAssignableFrom(columnType)) {
            return COMPARABLE_COMAPRATOR;
        }
        return LEXICAL_COMPARATOR;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private Row[] getViewToModel() {
        if (viewToModel == null) {
            int tableModelRowCount = tableModel.getRowCount();
            viewToModel = new Row[tableModelRowCount];
            for (int row = 0; row < tableModelRowCount; row++) {
                viewToModel[row] = new Row(row);
            }

            if (isSorting()) {
                Arrays.sort(viewToModel);
            }
        }
        return viewToModel;
    }

    /**
     * _more_
     *
     * @param viewIndex _more_
     *
     * @return _more_
     */
    public int modelIndex(int viewIndex) {
        return getViewToModel()[viewIndex].modelIndex;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    private int[] getModelToView() {
        if (modelToView == null) {
            int n = getViewToModel().length;
            modelToView = new int[n];
            for (int i = 0; i < n; i++) {
                modelToView[modelIndex(i)] = i;
            }
        }
        return modelToView;
    }

    // TableModel interface methods 

    /**
     * _more_
     *
     * @return _more_
     */
    public int getRowCount() {
        return (tableModel == null)
               ? 0
               : tableModel.getRowCount();
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getColumnCount() {
        return (tableModel == null)
               ? 0
               : tableModel.getColumnCount();
    }

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public String getColumnName(int column) {
        return tableModel.getColumnName(column);
    }

    /**
     * _more_
     *
     * @param column _more_
     *
     * @return _more_
     */
    public Class getColumnClass(int column) {
        return tableModel.getColumnClass(column);
    }

    /**
     * _more_
     *
     * @param row _more_
     * @param column _more_
     *
     * @return _more_
     */
    public boolean isCellEditable(int row, int column) {
        return tableModel.isCellEditable(modelIndex(row), column);
    }

    /**
     * _more_
     *
     * @param row _more_
     * @param column _more_
     *
     * @return _more_
     */
    public Object getValueAt(int row, int column) {
        return tableModel.getValueAt(modelIndex(row), column);
    }

    /**
     * _more_
     *
     * @param aValue _more_
     * @param row _more_
     * @param column _more_
     */
    public void setValueAt(Object aValue, int row, int column) {
        tableModel.setValueAt(aValue, modelIndex(row), column);
    }

    // Helper classes

    /**
     * Class Row _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.7 $
     */
    private class Row implements Comparable {

        /** _more_ */
        private int modelIndex;

        /**
         * _more_
         *
         * @param index _more_
         */
        public Row(int index) {
            this.modelIndex = index;
        }

        /**
         * _more_
         *
         * @param o _more_
         *
         * @return _more_
         */
        public int compareTo(Object o) {
            int row1 = modelIndex;
            int row2 = ((Row) o).modelIndex;
            for (Iterator it = sortingColumns.iterator(); it.hasNext(); ) {
                Directive directive  = (Directive) it.next();
                int       column     = directive.column;
                Object    o1         = tableModel.getValueAt(row1, column);
                Object    o2         = tableModel.getValueAt(row2, column);
                int       comparison = 0;

                /** The following NaN handling is something I (jeffmc@unidata) added */
                if ((o1 != null) && (o1 instanceof Double) && (o2 != null)
                        && (o2 instanceof Double)) {
                    if (((Double) o1).isNaN()) {
                        if ( !((Double) o2).isNaN()) {
                            return 1;
                        } else {
                            o1 = null;
                            o2 = null;
                        }
                    } else if (((Double) o2).isNaN()) {
                        return -1;
                    }
                }

                // Define null less than everything, except null.
                if ((o1 == null) && (o2 == null)) {
                    comparison = 0;
                } else if (o1 == null) {
                    comparison = -1;
                } else if (o2 == null) {
                    comparison = 1;
                } else {
                    comparison = getComparator(column).compare(o1, o2);
                }
                if (comparison != 0) {
                    return (directive.direction == DESCENDING)
                           ? -comparison
                           : comparison;
                }
            }
            return 0;
        }
    }

    /**
     * Class TableModelHandler _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.7 $
     */
    private class TableModelHandler implements TableModelListener {

        /**
         * _more_
         *
         * @param e _more_
         */
        public void tableChanged(TableModelEvent e) {
            // If we're not sorting by anything, just pass the event along.             
            if ( !isSorting()) {
                clearSortingState();
                fireTableChanged(e);
                return;
            }

            // If the table structure has changed, cancel the sorting; the             
            // sorting columns may have been either moved or deleted from             
            // the model. 
            if (e.getFirstRow() == TableModelEvent.HEADER_ROW) {
                cancelSorting();
                fireTableChanged(e);
                return;
            }

            // We can map a cell event through to the view without widening             
            // when the following conditions apply: 
            // 
            // a) all the changes are on one row (e.getFirstRow() == e.getLastRow()) and, 
            // b) all the changes are in one column (column != TableModelEvent.ALL_COLUMNS) and,
            // c) we are not sorting on that column (getSortingStatus(column) == NOT_SORTED) and, 
            // d) a reverse lookup will not trigger a sort (modelToView != null)
            //
            // Note: INSERT and DELETE events fail this test as they have column == ALL_COLUMNS.
            // 
            // The last check, for (modelToView != null) is to see if modelToView 
            // is already allocated. If we don't do this check; sorting can become 
            // a performance bottleneck for applications where cells  
            // change rapidly in different parts of the table. If cells 
            // change alternately in the sorting column and then outside of             
            // it this class can end up re-sorting on alternate cell updates - 
            // which can be a performance problem for large tables. The last 
            // clause avoids this problem. 
            int column = e.getColumn();
            if ((e.getFirstRow() == e.getLastRow())
                    && (column != TableModelEvent.ALL_COLUMNS)
                    && (getSortingStatus(column) == NOT_SORTED)
                    && (modelToView != null)) {
                int viewIndex = getModelToView()[e.getFirstRow()];
                fireTableChanged(new TableModelEvent(TableSorter.this,
                        viewIndex, viewIndex, column, e.getType()));
                return;
            }

            // Something has happened to the data that may have invalidated the row order. 
            clearSortingState();
            fireTableDataChanged();
            return;
        }
    }

    /**
     * Class MouseHandler _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.7 $
     */
    private class MouseHandler extends MouseAdapter {

        /**
         * _more_
         *
         * @param e _more_
         */
        public void mouseClicked(MouseEvent e) {
            JTableHeader     h           = (JTableHeader) e.getSource();
            TableColumnModel columnModel = h.getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            int column = columnModel.getColumn(viewColumn).getModelIndex();
            if (column != -1) {
                int status = getSortingStatus(column);
                if ( !e.isControlDown()) {
                    cancelSorting();
                }
                // Cycle the sorting states through {NOT_SORTED, ASCENDING, DESCENDING} or 
                // {NOT_SORTED, DESCENDING, ASCENDING} depending on whether shift is pressed. 
                status = status + (e.isShiftDown()
                                   ? -1
                                   : 1);
                status = (status + 4) % 3 - 1;  // signed mod, returning {-1, 0, 1}
                setSortingStatus(column, status);
            }
        }
    }

    /**
     * Class Arrow _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.7 $
     */
    private static class Arrow implements Icon {

        /** _more_ */
        private boolean descending;

        /** _more_ */
        private int size;

        /** _more_ */
        private int priority;

        /**
         * _more_
         *
         * @param descending _more_
         * @param size _more_
         * @param priority _more_
         */
        public Arrow(boolean descending, int size, int priority) {
            this.descending = descending;
            this.size       = size;
            this.priority   = priority;
        }

        /**
         * _more_
         *
         * @param c _more_
         * @param g _more_
         * @param x _more_
         * @param y _more_
         */
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Color color = (c == null)
                          ? Color.gray
                          : c.getBackground();
            // In a compound sort, make each succesive triangle 20% 
            // smaller than the previous one. 
            int dx = (int) (size / 2 * Math.pow(0.8, priority));
            int dy = descending
                     ? dx
                     : -dx;
            // Align icon (roughly) with font baseline. 
            y = y + 5 * size / 6 + (descending
                                    ? -dy
                                    : 0);
            int shift = descending
                        ? 1
                        : -1;
            g.translate(x, y);

            // Right diagonal. 
            g.setColor(color.darker());
            g.drawLine(dx / 2, dy, 0, 0);
            g.drawLine(dx / 2, dy + shift, 0, shift);

            // Left diagonal. 
            g.setColor(color.brighter());
            g.drawLine(dx / 2, dy, dx, 0);
            g.drawLine(dx / 2, dy + shift, dx, shift);

            // Horizontal line. 
            if (descending) {
                g.setColor(color.darker().darker());
            } else {
                g.setColor(color.brighter().brighter());
            }
            g.drawLine(dx, 0, 0, 0);

            g.setColor(color);
            g.translate(-x, -y);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getIconWidth() {
            return size;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public int getIconHeight() {
            return size;
        }
    }

    /**
     * Class SortableHeaderRenderer _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.7 $
     */
    private class SortableHeaderRenderer implements TableCellRenderer {

        /** _more_ */
        private TableCellRenderer tableCellRenderer;

        /**
         * _more_
         *
         * @param tableCellRenderer _more_
         */
        public SortableHeaderRenderer(TableCellRenderer tableCellRenderer) {
            this.tableCellRenderer = tableCellRenderer;
        }

        /**
         * _more_
         *
         * @param table _more_
         * @param value _more_
         * @param isSelected _more_
         * @param hasFocus _more_
         * @param row _more_
         * @param column _more_
         *
         * @return _more_
         */
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {
            Component c =
                tableCellRenderer.getTableCellRendererComponent(table, value,
                    isSelected, hasFocus, row, column);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setHorizontalTextPosition(JLabel.LEFT);
                int modelColumn = table.convertColumnIndexToModel(column);
                l.setIcon(getHeaderRendererIcon(modelColumn,
                        l.getFont().getSize()));
            }
            return c;
        }
    }

    /**
     * Class Directive _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.7 $
     */
    private static class Directive {

        /** _more_ */
        private int column;

        /** _more_ */
        private int direction;

        /**
         * _more_
         *
         * @param column _more_
         * @param direction On badness
         */
        public Directive(int column, int direction) {
            this.column    = column;
            this.direction = direction;
        }
    }
}

