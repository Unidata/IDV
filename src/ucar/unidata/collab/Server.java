/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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

package ucar.unidata.collab;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;


import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.List;



/**
 * A generic server
 *
 * @author Metapps development team
 */


public class Server {

    /** _more_ */
    public static int DEFAULT_PORT = 8080;

    /** _more_ */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(Server.class.getName());


    /**
     *  The port the host listens on.
     */
    private int port = DEFAULT_PORT;


    /** _more_ */
    private ServerSocket serverSocket;

    /** _more_ */
    private List clients = new ArrayList();

    /** _more_ */
    private boolean isRunning = false;

    /**
     *  Create a new Server connection.
     */
    public Server() {
        this(DEFAULT_PORT);
    }


    /**
     *  Create a new Server connection on the given port.
     *
     * @param port the port
     */
    public Server(int port) {
        this.port = port;
    }

    /**
     * Start server.
     *
     * @param newPort the new port
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void startServer(int newPort) throws IOException {
        port = newPort;
        if (isRunning) {
            return;
        }
        serverSocket = new ServerSocket(port);
        // server infinite loop
        Misc.run(new Runnable() {
            public void run() {
                runServer();
            }
        });
    }


    /**
     * Start server.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public void startServer() throws IOException {
        startServer(port);
    }


    /**
     * Stop server.
     */
    public void stopServer() {
        if (isRunning) {
            isRunning = false;
            //Connect to ourselves to kick the runServer out of its loop.
            try {
                //                System.err.println ("Closing server socket");
                serverSocket.close();
            } catch (Exception exc) {}
        }
    }


    /**
     * Run server.
     */
    private void runServer() {
        isRunning = true;
        notifyServerStart();
        //        System.err.println ("runServer -start");
        while (isRunning) {
            try {
                final Socket socket = serverSocket.accept();
                if ( !isRunning) {
                    //                    System.err.println ("runServer - got close");
                    break;
                }
                //Initialize the client socket in a thread
                initClient(socket);
            } catch (Exception e) {
                if (isRunning) {
                    System.out.println("Server got an exception:" + e);
                }
                break;
            }
        }
        //        System.err.println ("runServer -stop");
        try {
            serverSocket.close();
        } catch (IOException e) {}
        serverSocket = null;
        isRunning    = false;
        notifyServerStop();
    }

    /**
     *  Gets called when we are starting to listen
     */
    protected void notifyServerStart() {}


    /**
     *  Gets called when we are done running on the server socket
     */
    protected void notifyServerStop() {}


    /**
     *  Gets called when we have added a new client.
     *
     * @param client the client
     */
    protected void notifyClientAdd(Client client) {}


    /**
     *  Gets called when we have removed a client.
     *
     * @param client the client
     */
    protected void notifyClientRemove(Client client) {}



    /**
     *  Get the IsRunning property.
     *
     *  @return The IsRunning
     */
    public boolean getIsRunning() {
        return isRunning;
    }



    /**
     *  A factory method for creating a new client.
     *
     * @param clientSocket The socket we are connected to the client with.
     * @return a new Client
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected Client createClient(Socket clientSocket) throws IOException {
        return new Client(clientSocket);
    }


    /**
     *  Create a new client object and listen for input.
     *
     * @param clientSocket the client socket
     */
    protected void initClient(Socket clientSocket) {
        Client client = null;
        try {
            client = createClient(clientSocket);
            //            System.err.println ("Server runClientSocket - " + client);
            addClient(client);
        } catch (IOException e) {}
    }

    /**
     *  Create a new client object and listen for input.
     *
     * @param client the client
     */
    private void runClient(Client client) {
        try {
            while (true) {
                //                System.err.println ("Server doing client.read");
                String message = client.read();
                if (message == null) {
                    //System.err.println ("Server got null message");
                    break;
                }
                //                System.out.println ("Server got message:"+ message);
                //                System.err.println ("Calling handle client message");
                if (client.getOkToReceive()) {
                    handleIncomingMessage(client, message);
                }
            }
        } catch (IOException e) {
            //            System.out.println(e);
        }
        if (client != null) {
            removeClient(client);
        }
    }

    /**
     * Gets the clients.
     *
     * @return the clients
     */
    public List getClients() {
        return clients;
    }

    /**
     *  Does this server have any clients.
     *
     * @return true, if successful
     */
    public boolean hasClients() {
        return clients.size() > 0;
    }

    /**
     *  Add the given client to the list of clients managed by this server.
     *
     * @param client the client
     */
    public void addClient(Client client) {
        addClient(client, true);

    }

    /**
     *  Add the given client to the list of clients managed by this server.
     *
     * @param client the client
     * @param andStartListening the and start listening
     */
    public void addClient(final Client client, boolean andStartListening) {
        clients.add(client);
        notifyClientAdd(client);
        if (andStartListening) {
            Misc.run(new Runnable() {
                public void run() {
                    runClient(client);
                }
            });
        }
    }



    /**
     *  Remove the given client from the list of clients managed by this server.
     *
     * @param client the client
     */
    public void removeClient(Client client) {
        try {
            client.close();
        } catch (Exception exc) {}
        clients.remove(client);
        notifyClientRemove(client);
    }



    /**
     *  Handle the  message rcvd from the given client. The default is to just turn around
     *  and write it to each of the other clients.
     *
     * @param fromClient the from client
     * @param message the message
     */
    protected void handleIncomingMessage(Client fromClient, String message) {
        if (fromClient.getValid()) {
            write(message, fromClient);
        }
    }

    /**
     * Write.
     *
     * @param message the message
     */
    public void write(String message) {
        write(message, null);
    }

    /**
     * Write.
     *
     * @param message the message
     * @param exceptClient the except client
     */
    public void write(String message, Client exceptClient) {
        try {
            for (int i = 0; i < clients.size(); i++) {
                Client client = (Client) clients.get(i);
                //Skip over this one?
                if (Misc.equals(exceptClient, client)) {
                    continue;
                }
                //Only write to a valid client.
                if ( !client.getValid()) {
                    continue;
                }
                //                System.err.println ("Server writing to:" + client);
                client.write(message);
            }
        } catch (Exception ioe) {
            logException("Writing message:" + message, ioe);
        }
    }

    /**
     * Log exception.
     *
     * @param msg the msg
     * @param exc the exc
     */
    protected void logException(String msg, Exception exc) {
        LogUtil.printException(log_, msg, exc);
    }


    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {
        new Server();
    }
}
