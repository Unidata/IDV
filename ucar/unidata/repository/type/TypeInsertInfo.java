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

package ucar.unidata.repository.type;



import ucar.unidata.repository.*;
import ucar.unidata.repository.database.*;
import java.sql.PreparedStatement;
import java.sql.Statement;



/**
 *
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class TypeInsertInfo {
    private String sql;
    private PreparedStatement statement;
    private TypeHandler typeHandler;

    public TypeInsertInfo(TypeHandler typeHandler, String sql) {
        this.sql = sql;
        this.typeHandler = typeHandler;
    }


/**
Set the Sql property.

@param value The new value for Sql
**/
public void setSql (String value) {
	sql = value;
}

/**
Get the Sql property.

@return The Sql
**/
public String getSql () {
	return sql;
}

/**
Set the Statement property.

@param value The new value for Statement
**/
public void setStatement (PreparedStatement value) {
	statement = value;
}

/**
Get the Statement property.

@return The Statement
**/
public PreparedStatement getStatement () {
	return statement;
}

/**
Set the TypeHandler property.

@param value The new value for TypeHandler
**/
public void setTypeHandler (TypeHandler value) {
	typeHandler = value;
}

/**
Get the TypeHandler property.

@return The TypeHandler
**/
public TypeHandler getTypeHandler () {
	return typeHandler;
}




}
