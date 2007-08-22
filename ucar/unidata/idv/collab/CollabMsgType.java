/*
 * $Id: CollabMsgType.java,v 1.8 2005/05/13 18:30:36 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.collab;



import ucar.unidata.idv.*;

import ucar.unidata.collab.*;

import ucar.unidata.xml.*;

import ucar.visad.display.AnimationWidget;


import ucar.unidata.collab.Client;
import ucar.unidata.collab.Server;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.data.*;




import java.rmi.RemoteException;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * Used by the CollabManager to define the different messages
 * that may be sent
 *
 * @author IDV development team
 * @version $Revision: 1.8 $Date: 2005/05/13 18:30:36 $
 */


public class CollabMsgType {

    /**
     * This is used to temporarily stop the processing of incoming
     *   messages of this type
     */
    boolean blocked = false;

    /** The identifier of this message type */
    String id;

    /** Readable description */
    String description;

    /**
     *  Should messages of this type be relayed
     */
    private boolean shouldRelay;

    /** Holds all of the message types that have been created */
    private static List messageTypes = new ArrayList();


    /**
     * Factory method to create a CollabMsgType that should be relayed.
     *
     * @param id Id of the message type
     * @param desc Its description
     * @return The newly create message type
     */
    public static CollabMsgType createRelay(String id, String desc) {
        return create(id, desc, true);
    }

    /**
     * Factory method to create a CollabMsgType that should <em>not</em> be relayed.
     *
     * @param id Id of the message type
     * @param desc Its description
     * @return The newly create message type
     */
    public static CollabMsgType createNoRelay(String id, String desc) {
        return create(id, desc, false);
    }

    /**
     * Factory method to create a CollabMsgType
     *
     * @param id Id of the message type
     * @param desc Its description
     * @param shouldRelay Should  messages of this  type be relayed
     * @return The newly create message type
     */
    private static CollabMsgType create(String id, String desc,
                                        boolean shouldRelay) {
        CollabMsgType type = new CollabMsgType(id, desc, shouldRelay);
        messageTypes.add(type);
        return type;
    }

    /**
     * Constructor
     *
     * @param id Id of the message type
     * @param desc Its description
     * @param shouldRelay Should  messages of this  type be relayed
     *
     */
    private CollabMsgType(String id, String desc, boolean shouldRelay) {
        this.id          = id;
        this.description = desc;
        this.shouldRelay = shouldRelay;
    }



    /**
     *  Find the MsgType identified by the given type typeId.
     *
     * @param typeId The
     * @return The message type object or null
     */
    public static CollabMsgType find(String typeId) {
        for (int i = 0; i < messageTypes.size(); i++) {
            CollabMsgType msgType = (CollabMsgType) messageTypes.get(i);
            if (msgType.idEquals(typeId)) {
                return msgType;
            }
        }
        return null;
    }



    /**
     * Overwrite the equals
     *
     * @param other Object to compare to
     * @return Is equals to other
     */
    public boolean equals(Object other) {
        if (other instanceof CollabMsgType) {
            return Misc.equals(id, ((CollabMsgType) other).id);
        }
        return false;
    }


    /**
     * Does this object's identifier equals the given id
     *
     * @param otherId Id to check equality on
     * @return Is id equals
     */
    public boolean idEquals(String otherId) {
        return Misc.equals(id, otherId);
    }

    /**
     * String representation of this type
     *
     * @return The toString of this object
     */
    public String toString() {
        return id;
    }

    /**
     *  Set the Blocked property. This temporarily halts the processing
     * of messages of this type.
     *
     *  @param value The new value for Blocked
     */
    public void setBlocked(boolean value) {
        blocked = value;
    }

    /**
     *  Get the Blocked property.
     *
     *  @return The Blocked
     */
    public boolean getBlocked() {
        return blocked;
    }



    /**
     * Set the ShouldRelay property. Should messages of this type be
     * relayed.
     *
     * @param value The new value for ShouldRelay
     */
    public void setShouldRelay(boolean value) {
        shouldRelay = value;
    }

    /**
     * Get the ShouldRelay property.
     *
     * @return The ShouldRelay
     */
    public boolean getShouldRelay() {
        return shouldRelay;
    }



}







