/*
 * $Id: DataOperand.java,v 1.16 2007/06/08 21:24:48 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.data;




/**
 * This class holds a name/value pair that represents an operand for the
 * DerivedDataChoice
 *
 * @author IDV development team
 * @version $Revision: 1.16 $
 */
public class UserOperandValue {

    /** The value */
    private Object value;

    /** Is this persistent, i.e., is it saved in a bundle */
    private boolean persistent = true;


    /**
     * ctor
     *
     * @param value the value
     * @param persistent is it persistent
     */
    public UserOperandValue(Object value, boolean persistent) {
        this.value      = value;
        this.persistent = persistent;
    }

    /**
     *  Set the Value property.
     *
     *  @param value The new value for Value
     */
    public void setValue(Object value) {
        value = value;
    }

    /**
     *  Get the Value property.
     *
     *  @return The Value
     */
    public Object getValue() {
        return value;
    }

    /**
     *  Set the Persistent property.
     *
     *  @param value The new value for Persistent
     */
    public void setPersistent(boolean value) {
        persistent = value;
    }

    /**
     *  Get the Persistent property.
     *
     *  @return The Persistent
     */
    public boolean getPersistent() {
        return persistent;
    }



}

