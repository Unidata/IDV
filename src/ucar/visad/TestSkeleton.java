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

package ucar.visad;


import visad.DisplayImpl;

import visad.RemoteDisplay;

import visad.RemoteDisplayImpl;

import visad.RemoteServer;

import visad.RemoteServerImpl;

import visad.VisADException;

import visad.java2d.DisplayImplJ2D;

import visad.java3d.DisplayImplJ3D;



import java.rmi.Naming;
import java.rmi.RemoteException;


/**
 * Test skeleton for local and remote displays.
 */
public abstract class TestSkeleton extends Thread {

    /** start the server */
    boolean startServer = false;

    /** hostname for server */
    String hostName = null;

    /** client */
    RemoteServer client = null;

    /** maximum wait time for connection */
    private static final int maximumWaitTime = 60;

    /**
     * Constructs from nothing.
     */
    public TestSkeleton() {}

    /**
     * Constructs from an array of arguments.
     *
     * @param args             The arguments.
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public TestSkeleton(String args[])
            throws VisADException, RemoteException {

        if ( !processArgs(args)) {
            System.err.println("Exiting...");
            System.exit(1);
        }

        startThreads();
    }

    /**
     * Check extra options
     *
     * @param ch    character to look for
     * @param argc  argument index
     * @param args  arguments
     * @return  number of options
     */
    int checkExtraOption(char ch, int argc, String args[]) {
        return 0;
    }

    /**
     * Extra option usage (for help)
     * @return  list of extra options
     */
    String extraOptionUsage() {
        return "";
    }

    /**
     * Check for extra keywords
     *
     * @param argc   argument index
     * @param args   arguments
     * @return  number of keywords
     */
    int checkExtraKeyword(int argc, String args[]) {
        return 0;
    }

    /**
     * Check the usage for extra keywords
     * @return usage string
     */
    String extraKeywordUsage() {
        return "";
    }

    /**
     * Can this be a client/server application
     * @return  true
     */
    boolean hasClientServerMode() {
        return true;
    }

    /**
     * Processes the constructor arguments.
     *
     * @param args             The arguments.
     * @return                 true if the arguments were sucessfully processed.
     */
    public boolean processArgs(String args[]) {

        boolean usage = false;

        for (int argc = 0; argc < args.length; argc++) {
            if (args[argc].startsWith("-") && (args[argc].length() == 2)) {
                if (argc >= args.length) {
                    System.err.println("Missing argument for \"" + args[argc]
                                       + "\"\n");

                    usage = true;
                } else {
                    char   ch = args[argc].charAt(1);
                    String str, result;

                    switch (ch) {

                      case 'c' :
                          if (startServer) {
                              System.err.println(
                                  "Cannot specify both '-c' and '-s'!");

                              usage = true;
                          } else {
                              ++argc;

                              if (argc >= args.length) {
                                  System.err.println(
                                      "Missing hostname for '-c'");

                                  usage = true;
                              } else if ( !hasClientServerMode()) {
                                  System.err.println(
                                      "Client/server mode not supported"
                                      + " for this test");

                                  usage = true;
                              } else {
                                  hostName = args[argc];
                              }
                          }
                          break;

                      case 's' :
                          if (hostName != null) {
                              System.err.println(
                                  "Cannot specify both '-c' and '-s'!");

                              usage = true;
                          } else {
                              if ( !hasClientServerMode()) {
                                  System.err.println(
                                      "Client/server mode not supported"
                                      + " for this test");

                                  usage = true;
                              } else {
                                  startServer = true;
                              }
                          }
                          break;

                      default :
                          int handled = checkExtraOption(ch, argc + 1, args);

                          if (handled > 0) {
                              argc += (handled - 1);
                          } else {
                              System.err.println(getClass().getName()
                                      + ": Unknown option \"-" + ch + "\"");

                              usage = true;
                          }
                          break;
                    }
                }
            } else {
                int handled = checkExtraKeyword(argc, args);

                if (handled > 0) {
                    argc += (handled - 1);
                } else {
                    System.err.println(getClass().getName()
                                       + ": Unknown keyword \"" + args[argc]
                                       + "\"");

                    usage = true;
                }
            }
        }

        if (usage) {
            System.err.println("Usage: " + getClass().getName()
                               + (hasClientServerMode()
                                  ? " [-c(lient) hostname]"
                                  : "") + " [-d(ump display)]"
                                        + (hasClientServerMode()
                                           ? " [-s(erver)]"
                                           : "") + extraOptionUsage()
                                           + extraKeywordUsage());
        }

        return !usage;
    }

    /**
     * Set up the data
     * @return an array of displays
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    abstract DisplayImpl[] setupData() throws VisADException, RemoteException;

    /**
     * Get the title for the client/server connection.
     * @return title
     */
    String getClientServerTitle() {

        if (startServer) {
            if (hostName == null) {
                return " server";
            } else {
                return " server+client";
            }
        } else {
            if (hostName == null) {
                return " standalone";
            } else {
                return " client";
            }
        }
    }

    /**
     * set up the client data references
     *
     * @throws RemoteException
     */
    void getClientDataReferences() throws RemoteException {}

    /**
     * Get the remote displays
     * @return set of RemoteDisplays
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    RemoteDisplay[] getClientDisplays()
            throws VisADException, RemoteException {

        int             loops  = 0;
        RemoteDisplay[] rmtDpy = null;

        while ((rmtDpy == null) && (loops < maximumWaitTime)) {

            // try to reconnect to the server after the first loop
            if (loops > 0) {
                try {
                    String domain = "//" + hostName + "/"
                                    + getClass().getName();

                    client = (RemoteServer) Naming.lookup(domain);
                } catch (Exception e) {
                    throw new VisADException("Cannot connect to server on \""
                                             + hostName + "\"");
                }
            }

            // try to get displays from remote server
            try {
                if (client != null) {
                    rmtDpy = client.getDisplays();
                }
            } catch (java.rmi.ConnectException ce) {
                rmtDpy = null;
            }

            // if we didn't get any displays, print a message and wait a bit
            if (rmtDpy == null) {
                if (loops == 0) {
                    System.err.print("Client waiting for server ");
                } else {
                    System.err.print(".");
                }

                try {
                    sleep(1000);
                } catch (InterruptedException ie) {}

                loops++;
            }
        }

        if (loops == maximumWaitTime) {
            System.err.println(" giving up!");
            System.exit(1);
        } else if (loops > 0) {
            System.err.println(" connected");
        }

        return rmtDpy;
    }

    /**
     * Set up the client
     * @return  set of Displays
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD error
     */
    DisplayImpl[] setupClientData() throws VisADException, RemoteException {

        RemoteDisplay[] rmtDpy = getClientDisplays();

        if (rmtDpy == null) {
            throw new VisADException("No RemoteDisplays found!");
        }

        DisplayImpl[] dpys = new DisplayImpl[rmtDpy.length];

        for (int i = 0; i < dpys.length; i++) {
            String dpyClass = rmtDpy[i].getDisplayClassName();

            if (dpyClass.endsWith(".DisplayImplJ3D")) {
                dpys[i] = new DisplayImplJ3D(rmtDpy[i]);
            } else {
                dpys[i] = new DisplayImplJ2D(rmtDpy[i]);
            }
        }

        // add any data references to server
        getClientDataReferences();

        return dpys;
    }

    /**
     * Set the server data references
     *
     * @param server  server to use
     *
     * @throws RemoteException  Java RMI error
     */
    void setServerDataReferences(RemoteServerImpl server)
            throws RemoteException {}

    /**
     * Set up the server with the set of displays.
     *
     * @param dpys   list of displays for remote server
     * @return
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    RemoteServerImpl setupServer(DisplayImpl[] dpys)
            throws VisADException, RemoteException {

        // create new server
        RemoteServerImpl server;

        try {
            server = new RemoteServerImpl();

            String domain = "//:/" + getClass().getName();

            Naming.rebind(domain, server);
        } catch (Exception e) {
            throw new VisADException("Cannot set up server"
                                     + " (rmiregistry may not be running)");
        }

        // add all displays to server
        if (dpys != null) {
            for (int i = 0; i < dpys.length; i++) {
                server.addDisplay(new RemoteDisplayImpl(dpys[i]));
            }
        }

        // add any data references to server
        setServerDataReferences(server);

        return server;
    }

    /**
     * Set up the UI
     *
     * @param dpys  list of displays to use
     *
     * @throws RemoteException   Java RMI error
     * @throws VisADException    VisAD error
     */
    void setupUI(DisplayImpl[] dpys) throws VisADException, RemoteException {}

    /**
     * Starts the client & server threads.
     *
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void startThreads() throws VisADException, RemoteException {

        DisplayImpl[] displays;

        if (hostName != null) {
            displays = setupClientData();
        } else {
            displays = setupData();
        }

        if (startServer) {
            setupServer(displays);
        }

        setupUI(displays);
    }

    /**
     * Returns a string representation of this instance.  This implementation
     * returns <code>null</code>.
     *
     * @return                 a string representation of this instance.
     */
    public String toString() {
        return null;
    }
}
