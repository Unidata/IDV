/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import ucar.unidata.util.GuiUtils;

import java.awt.Dimension;

import javax.swing.JComponent;
import javax.swing.JScrollPane;


/**
 * Had to extend DrawingControl since JTable for Front drawing
 * has a different structure (less columns, slightly different names).
 *
 * @author tommyj
 *
 */

public class FrontDrawingControl extends DrawingControl {

    /**
     * {@inheritDoc}
     */
    @Override
    protected JComponent doMakeTablePanel() {
        glyphTableModel = new FrontGlyphTableModel();
        glyphTable      = new GlyphTable(glyphTableModel);
        JScrollPane sp = GuiUtils.makeScrollPane(glyphTable, 200, 100);
        sp.setPreferredSize(new Dimension(200, 100));
        JComponent tablePanel = GuiUtils.center(sp);
        glyphTable.selectionChanged();
        return tablePanel;
    }

    /**
     * The FrontGlyphTableModel class.
     */
    public class FrontGlyphTableModel extends GlyphTableModel {

        /**
         * {@inheritDoc}
         */
        @Override
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
