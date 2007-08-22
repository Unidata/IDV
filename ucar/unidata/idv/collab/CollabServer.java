/*
 * $Id: CollabServer.java,v 1.3 2005/05/13 18:30:37 jeffmc Exp $
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
 * This is an extension  of {@link ucar.unidata.collab.Server} class
 * that provides specific support for the collaboration mechanism
 * within the IDV.
 * <p>
 * This class does 2 things. First it overwrites the base clas
 * method createClient to create CollabClient objects.
 * Second it routes the server calls (e.g., handleIncomingMessage,
 * notifyClientAdd, etc.) to the CollabManager
 *
 * @author IDV development team
 * @version $Revision: 1.3 $Date: 2005/05/13 18:30:37 $
 */


public class CollabServer extends Server {

    /**
     *  A reference to the singelton collaboration manager
     */
    private CollabManager collabManager;

    /**
     * Create the server with the given collaboratio manager.
     * Have it open its connection on the given port
     *
     * @param collabManager The singleton collab manager
     * @param port The port to open a listening socket on
     */
    public CollabServer(CollabManager collabManager, int port) {
        super(port);
        this.collabManager = collabManager;
    }

    /**
     *  Factory method for creating our own CollabClient object. We
     * are overwriting the base class method to create the CollabClient
     *
     * @param clientSocket The socket we are connected to the client with
     * @return The new client
     * @throws IOException
     */
    protected Client createClient(Socket clientSocket) throws IOException {
        return new CollabClient(collabManager, clientSocket) {
            public void handleServerMessage(String msg) {
                collabManager.handleMessage(this, msg);
            }
        };
    }



    /**
     * Route the call to the CollabManager
     *
     * @param client The client the message came on
     * @param message The message
     */
    public void handleIncomingMessage(Client client, String message) {
        collabManager.handleMessage((CollabClient) client, message);
    }


    /**
     *  Gets called when we are done running on the server socket
     *  Route the call to the CollabManager
     */
    protected void notifyServerStop() {
        super.notifyServerStop();
        collabManager.serverStopped();
    }

    /**
     *  Gets called when we are starting to listen
     *  Route the call to the CollabManager
     */

    protected void notifyServerStart() {
        super.notifyServerStart();
        collabManager.serverStarted();
    }


    /**
     *  Gets called when we have added a new client.
     *  Route the call to the CollabManager
     *
     * @param client The new client
     */
    protected void notifyClientAdd(Client client) {
        super.notifyClientAdd(client);
        collabManager.clientAdded((CollabClient) client);
    }


    /**
     *  Gets called when we have removed a client.
     *  Route the call to the CollabManager
     *
     * @param client The removed client
     */
    protected void notifyClientRemove(Client client) {
        super.notifyClientRemove(client);
        collabManager.clientRemoved((CollabClient) client);
    }





}











