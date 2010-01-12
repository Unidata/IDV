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

package ucar.unidata.idv;


import ucar.unidata.idv.ui.IdvUIManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.InetAddress;
import java.net.Socket;


import java.net.URL;
import java.net.URLEncoder;

import java.util.Hashtable;



/**
 * Used for other idv instances to connect to so we only have one running at a time.
 */

public class OneInstanceServer extends HttpServer {

    /** http arg */
    public static final String ARG_ARGS = "args";

    /** The idv_ */
    IntegratedDataViewer idv;

    /** The localhost */
    InetAddress localHost;

    /**
     * ctor
     *
     * @param idv The idv
     * @param port The port to listen on
     */
    public OneInstanceServer(IntegratedDataViewer idv, int port) {
        super(port);
        this.idv = idv;
    }

    /**
     * Handle the error
     *
     * @param msg Error message_
     * @param exc Exception
     */
    protected void handleError(String msg, Exception exc) {
        LogUtil.consoleMessage(msg);
        LogUtil.consoleMessage("Exception:" + exc);
    }

    /**
     * Create the url that we ping
     *
     * @param port The port to connect to
     * @param args The command line args
     *
     * @return The url
     *
     * @throws Exception On badness
     */
    static String assembleUrl(int port, String[] args) throws Exception {
        return "http://localhost:" + port + "?" + ARG_ARGS + "="
               + URLEncoder.encode(
                   XmlUtil.encodeBase64(Misc.serialize(args)), "UTF-8");

    }

    /**
     * Make the handler for this connection
     *
     * @param socket The connection
     *
     * @return The handler
     *
     * @throws Exception On badness
     */
    protected RequestHandler doMakeRequestHandler(Socket socket)
            throws Exception {
        if (localHost == null) {
            localHost = InetAddress.getLocalHost();
        }
        InetAddress inet = socket.getInetAddress();
        //      System.err.println("inet:" + inet.getHostAddress() + " localhost:" + localHost.getHostAddress());
        //Only accept requests from localhost
        if ( !inet.getHostAddress().equals("127.0.0.1")) {
            return null;
        }
        //        System.err.println ("got request");
        //        Misc.sleepSeconds(120);
        return new OneInstanceRequestHandler(idv, this, socket);
    }


    /**
     * Class OneInstanceRequestHandler the handler
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.11 $
     */
    public static class OneInstanceRequestHandler extends HttpServer
        .RequestHandler {

        /** The idv */
        IntegratedDataViewer idv;

        /**
         * ctor
         *
         * @param idv the idv
         * @param server the server
         * @param socket the socket we handle the connection of
         *
         * @throws Exception On badness
         */
        public OneInstanceRequestHandler(IntegratedDataViewer idv,
                                         HttpServer server, Socket socket)
                throws Exception {
            super(server, socket);
            this.idv = idv;
        }

        /**
         * Handle the request. This reads the command line arguments, writes back "ok",
         * nad has the idv process the args.
         *
         * @param path url path. ignored.
         * @param formArgs form args
         * @param httpArgs http args
         * @param content content. unused.
         *
         * @throws Exception On badness
         */
        protected void handleRequest(String path, Hashtable formArgs,
                                     Hashtable httpArgs, String content)
                throws Exception {
            String   argStr   = (String) formArgs.get(ARG_ARGS);
            byte[]   argBytes = XmlUtil.decodeBase64(argStr);
            String[] args     = (String[]) Misc.deserialize(argBytes);
            writeResult(true, "ok", "text");
            idv.getArgsManager().processInstanceArgs(args);
            idv.getIdvUIManager().toFrontMainWindows();
        }
    }

}
