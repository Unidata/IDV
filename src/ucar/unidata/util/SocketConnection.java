/*
 * $Id: IOUtil.java,v 1.52 2007/08/14 16:06:15 jeffmc Exp $
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



package ucar.unidata.util;


import java.io.*;
import java.net.*;



public class SocketConnection implements Runnable {
    private     Socket socket;
    private     DataOutputStream out; 
    private     DataInputStream in; 
    private     boolean  running = false;

    public SocketConnection(Socket socket) throws IOException {
        this.socket = socket;
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
    }

    public void write(String message) throws IOException {
        out.writeInt(message.length());
        out.write(message.getBytes());
    }



    public void initConnection() {
    }

    public void run() {
        try {
            initConnection();
            running = true;
            byte[]buffer= new byte[1000];
            while(running) {
                int length = in.readInt();
                if(!running) break;
                //Don't blow memory
                if(length>1000000) {
                    throw new IllegalArgumentException("Bad length read:" + length);
                }
                int total = 0;
                if(buffer.length<length) {
                    buffer =new byte[length];
                }
                while(total<length) {
                    int howMany = in.read(buffer,total,length-total);
                    if(!running) break;
                    if (howMany <= 0) {
                        logError("read " + howMany +"  bytes");
                        break;
                    }
                    total+=howMany;
                }
                if(!running) break;
                if(total!=length) {
                    break;
                }
                String s = new String(buffer,0,length);
                handleMessage(s);
            }
        } catch(EOFException eofe) {
	    //ignore
        } catch(IOException ioe) {
            logMessage("Got exception: " + ioe);
        }
        connectionClosed();
    }


    public void close() {
        running = false;
        try {
            socket.close();
        } catch(Exception ignore) {}
    }

    protected void  connectionClosed() {
        logMessage("Connection closed");
    }

    public void logError(String message) {
        System.err.println(message);        
    }
    public void logMessage(String message) {
        System.err.println(message);        
    }

    public void handleMessage(String s) {
        System.err.println ("read:" + s);
    }
}
