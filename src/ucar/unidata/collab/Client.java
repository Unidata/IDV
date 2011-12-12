/*
 * $Id: Client.java,v 1.15 2005/09/21 17:13:21 jeffmc Exp $
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

package ucar.unidata.collab;



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


import java.io.*;

import java.net.*;



/**
 *
 * A generic client object.
 * @author Metapps development team
 * @version $Revision: 1.15 $Date: 2005/09/21 17:13:21 $
 */


public class Client {

    /** _more_ */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(Client.class.getName());


    /** _more_ */
    private boolean isListening = false;

    /**
     *  The host I am connecting to.
     */
    private String hostname;

    /**
     *  The port the host listens on.
     */
    private int port = Server.DEFAULT_PORT;


    /** _more_ */
    private Socket socket;

    /** _more_ */
    private InputStream input;

    /** _more_ */
    private Writer output;


    /**
     *  The okToSend property.
     */
    private boolean okToSend = true;


    /**
     *  The okToReceive property.
     */
    private boolean okToReceive = true;

    /**
     *  The valid property.
     */
    private boolean valid = false;


    /** _more_ */
    byte[] mainBuffer = new byte[1000000];


    /** _more_ */
    String msgBuffer = "";


    /** _more_ */
    int numCharsNeeded = 0;



    /**
     * _more_
     *
     */
    public Client() {}


    /**
     *  Create a new Client connection, connection to the given hostname which is of the form:
     *  hostname[:optional port number]. If alsoCreateServer is true
     *  then also create a server.
     *
     * @param host
     */
    public Client(String host) {
        this(host, -1);
    }



    /**
     *  Create a new Client connection, connection to the given hostname which is of the form:
     *  hostname[:optional port number]. If alsoCreateServer is true
     *  then also create a server.
     *
     * @param host
     * @param thePort
     */
    public Client(String host, int thePort) {
        //See if there is a port
        this.hostname = host;
        List l = StringUtil.split(hostname, ":");
        if (l.size() > 1) {
            hostname = (String) l.get(0);
            port     = new Integer((String) l.get(1)).intValue();
        } else {
            port = thePort;
        }

        try {
            socket = new Socket(hostname, port);
            debug("Connected with server " + socket.getInetAddress() + ":"
                  + socket.getPort());
            output = new PrintWriter(socket.getOutputStream(), true);
            input  = socket.getInputStream();
        } catch (UnknownHostException uhe) {
            socket = null;
            output = null;
            input  = null;
            //TODO            logException ("Unknown host", uhe);
        } catch (IOException ioe) {
            //TODO logException ("Connecting to server", ioe);
        }
        //        initListening ();
    }


    /**
     * _more_
     *
     * @param socket
     *
     * @throws IOException
     *
     */
    public Client(Socket socket) throws IOException {
        this.socket = socket;
        hostname    = socket.getInetAddress() + ":" + socket.getPort();
        output      = new PrintWriter(socket.getOutputStream(), true);
        input       = socket.getInputStream();
    }


    /**
     * _more_
     *
     * @param msg
     */
    public void debug(String msg) {
        System.err.println(msg);
    }

    /**
     * _more_
     */
    private void initListening() {
        if (isListening) {
            return;
        }
        Misc.run(new Runnable() {
            public void run() {
                runClient();
            }
        });
    }

    /**
     * _more_
     */
    private void runClient() {
        isListening = true;
        while (isConnectionOk()) {
            try {
                String message = read();
                if (message == null) {
                    break;
                }
                if (okToReceive) {
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                debug("Client got error in runServer");
                break;
            }
        }
        //        System.err.println ("Client is closed");
        isListening = false;
        close();
    }


    /**
     *  Is this client currently connected.
     *
     *  @return  Is this client currently connected.
     */

    public boolean isConnectionOk() {
        return (output != null);
    }



    /**
     *  Handle the  message rcvd from the server.
     *
     * @param message
     */
    protected void handleServerMessage(String message) {
        //        debug ("Got message from server:" + message);
    }




    /**
     * _more_
     * @return _more_
     *
     * @throws IOException
     */
    protected String read() throws IOException {
        String errorMsg = null;
        while (isConnectionOk()) {
            if (numCharsNeeded == 0) {
                int idx = msgBuffer.indexOf(";");
                if (idx > 0) {
                    numCharsNeeded = Integer.decode(msgBuffer.substring(0,
                            idx)).intValue();
                    msgBuffer = msgBuffer.substring(idx + 1);
                }
            }

            if ((numCharsNeeded == 0)
                    || (numCharsNeeded > msgBuffer.length())) {
                int bytesRead = input.read(mainBuffer, 0, mainBuffer.length);
                if ( !isConnectionOk()) {
                    break;
                }
                if (bytesRead == 0) {
                    continue;
                }
                if (bytesRead == -1) {  //eof - socket closed
                    errorMsg =
                        "Read -1 bytes.  Connection to the server has been lost";
                    break;
                }
                //We have input - we'll now churn through it until we have processed all msgs
                msgBuffer = msgBuffer + new String(mainBuffer, 0, bytesRead);
            }

            //Start of a message, look for the "length;..." at the beginning of the message
            if (numCharsNeeded == 0) {
                int idx = msgBuffer.indexOf(";");
                //Don't have the whole length string yet,  continue
                if (idx < 0) {
                    continue;
                }
                numCharsNeeded = Integer.decode(msgBuffer.substring(0,
                        idx)).intValue();
                msgBuffer = msgBuffer.substring(idx + 1);
            }

            if (numCharsNeeded <= msgBuffer.length()) {
                String msg = msgBuffer.substring(0, numCharsNeeded);
                msgBuffer = msgBuffer.substring(numCharsNeeded);
                //Reset that we are looking for a message
                numCharsNeeded = 0;
                return msg;
            }
        }
        return null;
    }







    /**
     * _more_
     */
    public void close() {
        try {
            input  = null;
            output = null;
            socket.close();
        } catch (Exception ioe) {}

    }


    /**
     * _more_
     *
     * @param message
     */
    public void write(String message) {
        try {
            if ( !okToSend) {
                return;
            }
            int length = message.length();
            output.write(length + ";");
            output.write(message);
            output.flush();
            //            System.err.println ("Client writing message");
        } catch (Exception ioe) {
            logException("Writing message:" + message, ioe);
        }
    }


    /**
     * _more_
     *
     * @param msg
     * @param exc
     */
    protected void logException(String msg, Exception exc) {
        LogUtil.printException(log_, msg, exc);
    }


    /**
     * _more_
     * @return _more_
     */
    public String toString() {
        return hostname;
    }


    /**
     *  Set the Hostname property.
     *
     *  @param value The new value for Hostname
     */
    public void setHostname(String value) {
        hostname = value;
    }

    /**
     *  Get the Hostname property.
     *
     *  @return The Hostname
     */
    public String getHostname() {
        return hostname;
    }



    /**
     *  Set the Valid property.
     *
     *  @param value The new value for Valid
     */
    public void setValid(boolean value) {
        valid = value;
    }

    /**
     *  Get the Valid property.
     *
     *  @return The Valid
     */
    public boolean getValid() {
        return valid;
    }


    /**
     *  Set the OkToSend property.
     *
     *  @param value The new value for OkToSend
     */
    public void setOkToSend(boolean value) {
        okToSend = value;
    }

    /**
     *  Get the OkToSend property.
     *
     *  @return The OkToSend
     */
    public boolean getOkToSend() {
        return okToSend;
    }


    /**
     *  Set the OkToReceive property.
     *
     *  @param value The new value for OkToReceive
     */
    public void setOkToReceive(boolean value) {
        okToReceive = value;
    }

    /**
     *  Get the OkToReceive property.
     *
     *  @return The OkToReceive
     */
    public boolean getOkToReceive() {
        return okToReceive;
    }









    /**
     * _more_
     *
     * @param args
     */
    public static void main(String[] args) {
        try {
            Client client = new Client(args[0]);
            BufferedReader input =
                new BufferedReader(new InputStreamReader(System.in));
            while (client.isConnectionOk()) {
                String lineToBeSent = input.readLine();
                if ( !client.isConnectionOk()) {
                    System.err.println("Connection closed");
                    System.exit(0);
                }
                client.write(lineToBeSent);
            }
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
        }
    }





}













