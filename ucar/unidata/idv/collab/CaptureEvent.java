/*
 * $Id: CaptureEvent.java,v 1.5 2005/05/13 18:30:36 jeffmc Exp $
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
 * This holds one colalboration event and is used to
 * capture a stream of events.
 *
 * @author IDV development team
 * @version $Revision: 1.5 $Date: 2005/05/13 18:30:36 $
 */


public class CaptureEvent {

    /** Time of event */
    private long timestamp;

    /** event message */
    private String message;

    /**
     * Dummy ctor for xml decoding
     *
     */
    public CaptureEvent() {}


    /**
     * Create the event with the given message and set the timestamp
     *
     * @param msg The message
     *
     */
    public CaptureEvent(String msg) {
        timestamp = System.currentTimeMillis();
        message   = msg;
    }


    /**
     *  Set the Timestamp property.
     *
     *  @param value The new value for Timestamp
     */
    public void setTimestamp(long value) {
        timestamp = value;
    }

    /**
     *  Get the Timestamp property.
     *
     *  @return The Timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }


    /**
     *  Set the Message property.
     *
     *  @param value The new value for Message
     */
    public void setMessage(String value) {
        message = value;
    }

    /**
     *  Get the Message property.
     *
     *  @return The Message
     */
    public String getMessage() {
        return message;
    }


}










