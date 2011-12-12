/*
 * $Id: UnboundDataChoice.java,v 1.25 2006/12/01 20:41:23 jeffmc Exp $
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



import visad.*;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;



/**
 * Represents a DataChoice in a formula that is unbound.
 * @author Metapps development team
 * @version $Revision: 1.25 $
 */

public class UnboundDataChoice extends DataChoice {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            UnboundDataChoice.class.getName());

    /** label for his */
    protected String label;

    /**
     * Default constructor; does nothing
     */
    public UnboundDataChoice() {}

    /**
     * Copy constructor for creating one from another
     *
     * @param other  the other
     */
    public UnboundDataChoice(UnboundDataChoice other) {
        super(other);
        this.label = other.label;
    }

    /**
     * Create a new UnboundDataChoice.
     *
     * @param label   the label for this
     *
     */
    public UnboundDataChoice(String label) {
        this.label = label;
    }

    /**
     * Create a clone of this.  Calls the copy constructor
     *
     * @return  A new UnboundDataChoice, just like me!
     */
    public DataChoice cloneMe() {
        return new UnboundDataChoice(this);
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
     * Return a String representation of this UnboundDataChoice.
     *
     * @return String representation of this UnboundDataChoice
     */
    public String toString() {
        return label;
    }

    /**
     *  A no-op
     *
     * @param category          The data category of the request.
     * @param dataSelection     Identifies any subsetting of the data.
     * @param requestProperties Hashtable that holds any detailed request
     *                          properties.
     *
     * @return null
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
     * Remove a {@link DataChangeListener}.  Does nothing.
     *
     * @param listener  listener to remove
     */
    public void removeDataChangeListener(DataChangeListener listener) {
        //This space  intentionallyleft blank
    }


}

