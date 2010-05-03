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


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class IndexInfo {

    /** _more_          */
    private String name;

    /** _more_          */
    private String columnName;


    /**
     * _more_
     */
    public IndexInfo() {}

    /**
     * _more_
     *
     * @param name _more_
     * @param columnName _more_
     */
    public IndexInfo(String name, String columnName) {
        this.name       = name;
        this.columnName = columnName;

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
     * Set the ColumnName property.
     *
     * @param value The new value for ColumnName
     */
    public void setColumnName(String value) {
        this.columnName = value;
    }

    /**
     * Get the ColumnName property.
     *
     * @return The ColumnName
     */
    public String getColumnName() {
        return this.columnName;
    }


}
