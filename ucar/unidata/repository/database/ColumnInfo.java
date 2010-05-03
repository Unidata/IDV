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
public class ColumnInfo {

    /** _more_          */
    public static final int TYPE_TIMESTAMP = 1;

    /** _more_          */
    public static final int TYPE_VARCHAR = 2;

    /** _more_          */
    public static final int TYPE_INTEGER = 3;

    /** _more_          */
    public static final int TYPE_DOUBLE = 4;

    /** _more_          */
    public static final int TYPE_CLOB = 5;


    /** _more_          */
    private String name;

    /** _more_          */
    private String typeName;

    /** _more_          */
    private int type;

    /** _more_          */
    private int size;

    /**
     * _more_
     */
    public ColumnInfo() {}

    /**
     * _more_
     *
     * @param name _more_
     * @param typeName _more_
     * @param type _more_
     * @param size _more_
     */
    public ColumnInfo(String name, String typeName, int type, int size) {
        this.name     = name;
        this.typeName = typeName;
        this.type     = convertType(type);
        this.size     = size;
    }

    /**
     * _more_
     *
     * @param type _more_
     *
     * @return _more_
     */
    public static int convertType(int type) {
        if (type == java.sql.Types.TIMESTAMP) {
            return TYPE_TIMESTAMP;
        } else if (type == java.sql.Types.VARCHAR) {
            return TYPE_VARCHAR;
        } else if (type == java.sql.Types.INTEGER) {
            return TYPE_INTEGER;
        } else if (type == java.sql.Types.DOUBLE) {
            return TYPE_DOUBLE;
        } else if (type == java.sql.Types.CLOB) {
            return TYPE_CLOB;
        } else {
            throw new IllegalArgumentException("Unknown sqltype:" + type);
        }
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
     *  Set the TypeName property.
     *
     *  @param value The new value for TypeName
     */
    public void setTypeName(String value) {
        this.typeName = value;
    }

    /**
     *  Get the TypeName property.
     *
     *  @return The TypeName
     */
    public String getTypeName() {
        return this.typeName;
    }

    /**
     *  Set the Type property.
     *
     *  @param value The new value for Type
     */
    public void setType(int value) {
        this.type = value;
    }

    /**
     *  Get the Type property.
     *
     *  @return The Type
     */
    public int getType() {
        return this.type;
    }

    /**
     *  Set the Size property.
     *
     *  @param value The new value for Size
     */
    public void setSize(int value) {
        this.size = value;
    }

    /**
     *  Get the Size property.
     *
     *  @return The Size
     */
    public int getSize() {
        return this.size;
    }



}
