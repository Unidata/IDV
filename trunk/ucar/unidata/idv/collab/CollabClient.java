/*
 * $Id: CollabClient.java,v 1.5 2005/05/13 18:30:36 jeffmc Exp $
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
 * This provides IDV specific extensions to the
 * {@link ucar.unidata.collab.Client} class to
 * support the collaboration mechanism with the IDV.
 * In particular it has a name (the name of the client)
 * and a flag to show if this is the Client object
 * that represents the local user.
 * In the future this could hold more information: contact info, etc.
 *
 * @author IDV development team
 * @version $Revision: 1.5 $Date: 2005/05/13 18:30:36 $
 */

public class CollabClient extends Client {

    /**
     *  A reference to the singleton collaboration manager
     */
    private CollabManager collabManager;

    /** Name of this client */
    String name = "";

    /**
     *  The isLocal property. This is used to determine
     * if this object represents the local user or a remote user.
     */
    private boolean isLocal = false;


    /**
     * Create a dummy version of this object. This is used for doing
     * event replays. It is not connected to any server, etc.
     *
     */
    public CollabClient() {}


    /**
     * Create the client and have it try to connect to a Server
     * at the given hostname and port. It will not
     * try to read anything, that is left to the
     * {@link ucar.unidata.collab.Server}
     * You can check if the connection was successful
     * with {@link ucar.unidata.collab.Client#isConnectionOk()}
     *
     * @param collabManager Reference to the singleton CollabManager
     * @param hostName Host to  connect to
     * @param port Port on host to connect to
     *
     * @throws IOException
     *
     */
    public CollabClient(CollabManager collabManager, String hostName, int port)
            throws IOException {
        super(hostName, port);
        this.collabManager = collabManager;
    }


    /**
     * Create this client with the already existing socket.
     *
     * @param collabManager Reference to the singleton CollabManager
     * @param socket The socket
     *
     * @throws IOException
     */
    public CollabClient(CollabManager collabManager, Socket socket)
            throws IOException {
        super(socket);
        this.collabManager = collabManager;
    }


    /**
     * Route the message to the CollabManager
     *
     * @param msg The message
     */
    public void handleServerMessage(String msg) {
        collabManager.handleMessage(this, msg);
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     *  Set the isLocal property. This is used to determine if
     * this Client object represents the local client or a remote
     * client
     *
     *  @param value The new value for isLocal
     */
    public void setIsLocal(boolean value) {
        isLocal = value;
    }

    /**
     *  Get the IsLocal property.
     *
     *  @return The IsLocal
     */
    public boolean getIsLocal() {
        return isLocal;
    }


}










