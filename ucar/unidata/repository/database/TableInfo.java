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

package ucar.unidata.repository.database;


import ucar.unidata.util.StringUtil;

import java.sql.PreparedStatement;

import java.util.ArrayList;
import java.util.List;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class TableInfo {

    /** _more_          */
    private String name;

    /** _more_          */
    private List<IndexInfo> indices;

    /** _more_          */
    private List<ColumnInfo> columns;

    /** _more_          */
    public PreparedStatement statement;

    /** _more_          */
    public int batchCnt = 0;

    /**
     * _more_
     */
    public TableInfo() {}

    /**
     * _more_
     *
     * @param name _more_
     * @param indices _more_
     * @param columns _more_
     */
    public TableInfo(String name, List<IndexInfo> indices,
                     List<ColumnInfo> columns) {
        this.name    = name;
        this.indices = indices;
        this.columns = columns;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public String[] getColumnNames() {
        String[] names = new String[columns.size()];
        int      cnt   = 0;
        for (ColumnInfo columnInfo : columns) {
            names[cnt++] = columnInfo.getName();
        }
        return names;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return this.name;
    }


    /**
     *  Set the Columns property.
     *
     *  @param value The new value for Columns
     */
    public void setColumns(List<ColumnInfo> value) {
        this.columns = value;
    }

    /**
     *  Get the Columns property.
     *
     *  @return The Columns
     */
    public List<ColumnInfo> getColumns() {
        return this.columns;
    }


    /**
     *  Set the Indices property.
     *
     *  @param value The new value for Indices
     */
    public void setIndices(List<IndexInfo> value) {
        this.indices = value;
    }

    /**
     *  Get the Indices property.
     *
     *  @return The Indices
     */
    public List<IndexInfo> getIndices() {
        return this.indices;
    }


}
