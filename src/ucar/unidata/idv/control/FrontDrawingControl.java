package ucar.unidata.idv.control;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import ucar.unidata.util.GuiUtils;

/**
 * Had to extend DrawingControl since JTable for Front drawing 
 * has a different structure (less columns, slightly different names).
 * 
 * @author tommyj
 *
 */

public class FrontDrawingControl extends DrawingControl {
    
    /**
     * Make the jtable panel
     *
     * @return jtable panel
     */
    protected JComponent doMakeTablePanel() {
        glyphTableModel = new FrontGlyphTableModel();
        glyphTable      = new GlyphTable(glyphTableModel);
        JScrollPane sp = GuiUtils.makeScrollPane(glyphTable, 200, 100);
        sp.setPreferredSize(new Dimension(200, 100));
        JComponent tablePanel = GuiUtils.center(sp);
        glyphTable.selectionChanged();
        return tablePanel;
    }
    
    public class FrontGlyphTableModel extends GlyphTableModel {
        private static final long serialVersionUID = 1L;

        @Override
        /**
         * col name
         *
         * @param column column
         *
         * @return col name
         */
        public String getColumnName(int column) {
            if (column == 0) {
                return "Name";
            }
            if (column == 1) {
                return "Type";
            }
            if (column == 2) {
                return "Properties";
            }
            return "";
        }
        
    }
    
}
