/*
 * $Id: UserDataChoice.java,v 1.26 2006/12/01 20:41:23 jeffmc Exp $
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


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;



import visad.*;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;



/**
 * Represents a DataChoice in a formula that is numeric.
 *
 * @author IDV development team
 * @version $Revision: 1.26 $
 */
public class UserDataChoice extends DataChoice {

    /** A hack used by the DerivedDataChoice for when we don't save the user entered choices in a bundle */
    public boolean persistent = true;

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(UserDataChoice.class.getName());

    /** DataChoice label */
    protected String label;

    /** numeric value */
    protected Object value;

    /** default value */
    private String defaultValue;


    /**
     * Default constructor; used for unpersistence
     */
    public UserDataChoice() {}

    /**
     * Copy constructor, set properties from the other.
     *
     * @param other    other UserDataChoice for properties
     */
    public UserDataChoice(UserDataChoice other) {
        super(other);
        this.label = other.label;
        this.value = other.value;
    }

    /**
     * Create a new UserDataChoice
     *
     * @param label   label for this choice
     */
    public UserDataChoice(String label) {
        this(label, null);
    }

    /**
     * Create a new UserDataChoice
     *
     * @param label   label for this choice
     * @param defaultValue The defaultValue
     */
    public UserDataChoice(String label, String defaultValue) {
        super(label, label);
        this.label        = label;
        this.defaultValue = defaultValue;
    }


    /**
     * Make a clone of me.
     *
     * @return a new me!
     */
    public DataChoice cloneMe() {
        return new UserDataChoice(this);
    }


    /**
     * Get the label for this.
     *
     * @return  the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label for this
     *
     * @param l  new label
     */
    public void setLabel(String l) {
        label = l;
    }

    /**
     * Return a String representation of this UserDataChoice.
     *
     * @return a String representation of this UserDataChoice
     */
    public String toString() {
        return label;
    }

    /**
     * Get the value for this choice.
     *
     * @return  the value
     */
    public Object getValue() {
        return value;
    }

    /**
     * Set the value for this choice.
     *
     * @param v   the value
     */
    public void setValue(Object v) {
        value = v;
    }

    /**
     * Check for equality.
     *
     * @param other   object in question
     *
     * @return true if <code>o</code> equals this
     */
    public boolean equals(Object other) {
        if ( !(other instanceof UserDataChoice)) {
            return false;
        }
        UserDataChoice that = (UserDataChoice) other;
        //     System.err.println ("equals:"  + this + "-" + that);
        return Misc.equals(label, that.label);
    }


    /**
     *  A no-op
     *
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return  null
     *
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    protected Data getData(DataCategory category,
                           DataSelection dataSelection,
                           Hashtable requestProperties)
            throws VisADException, RemoteException {
        return null;
    }


    /**
     * Add a {@link DataChangeListener}.  Does nothing
     *
     * @param listener  listener to add
     */
    public void addDataChangeListener(DataChangeListener listener) {
        //This space  intentionallyleft blank
    }

    /**
     * Remove a {@link DataChangeListener}.  Does nothing
     *
     * @param listener  listener to remove
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        //This space  intentionallyleft blank
    }



    /**
     * Set the DefaultValue property.
     *
     * @param value The new value for DefaultValue
     */
    public void setDefaultValue(String value) {
        defaultValue = value;
    }

    /**
     * Get the DefaultValue property.
     *
     * @return The DefaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }



}

