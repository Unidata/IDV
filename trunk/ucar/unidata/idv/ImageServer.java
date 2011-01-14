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

import java.awt.*;

import java.io.*;

import java.net.Socket;


import java.net.URL;

import java.util.Date;

import java.util.Hashtable;



/**
 * Creates and serves up images via http
 */

public class ImageServer extends HttpServer {

    /** http image arg */
    public static final String ARG_REQUEST = "request";

    /** http image arg */
    public static final String ARG_FILE = "file";

    /** http image arg */
    public static final String ARG_BUNDLE = "bundle";

    /** http image arg */
    public static final String ARG_DATASOURCE = "datasource";

    /** http image arg */
    public static final String ARG_PARAM = "param";

    /** http image arg */
    public static final String ARG_DISPLAY = "display";

    /** http image arg */
    public static final String ARG_PROPERTIES = "properties";

    /** _more_          */
    public static final String ARG_WIDTH = "width";

    /** _more_          */
    public static final String ARG_HEIGHT = "height";

    /** http image arg */
    public static final String REQ_MAKEMOVIE = "makemovie";

    /** http image arg */
    public static final String REQ_MAKEIMAGE = "makeimage";

    /** http image arg */
    public static final String REQ_EXIT = "exit";

    /** The idv */
    IntegratedDataViewer idv;

    /**
     * ctor
     *
     * @param idv The idv
     * @param port The port
     */
    public ImageServer(IntegratedDataViewer idv, int port) {
        super(port);
        this.idv = idv;
    }


    /**
     * ctor
     *
     * @param idv The idv
     * @param propertyFile server properties
     */
    public ImageServer(IntegratedDataViewer idv, String propertyFile) {
        super(propertyFile);
        this.idv = idv;
    }

    /**
     * Create the request handler for the connection
     *
     * @param socket The connection
     *
     * @return The handler
     *
     * @throws Exception On badness
     */
    protected RequestHandler doMakeRequestHandler(Socket socket)
            throws Exception {
        return new ImageRequestHandler(idv, this, socket);
    }


    /**
     * Class ImageRequestHandler handles image server requests
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.15 $
     */
    public static class ImageRequestHandler extends HttpServer
        .RequestHandler {

        /** The idv */
        IntegratedDataViewer idv;

        /**
         * ctor
         *
         * @param idv The idv
         * @param server The server
         * @param socket The connection
         *
         * @throws Exception On badness
         */
        public ImageRequestHandler(IntegratedDataViewer idv,
                                   HttpServer server, Socket socket)
                throws Exception {
            super(server, socket);
            this.idv = idv;
        }

        /**
         * Handle the request
         *
         * @param path unused
         * @param formArgs the request args
         * @param httpArgs unused
         * @param content unused
         *
         * @throws Exception On badness
         */
        protected void handleRequest(String path, Hashtable formArgs,
                                     Hashtable httpArgs, String content)
                throws Exception {
            try {
                handleRequestInner(path, formArgs, httpArgs, content);
            } catch (Throwable exc) {
                System.err.println("error:" + exc);
                exc.printStackTrace();
                try {
                    writeResult(false,
                                "<html>Error handling request:<pre>" + exc
                                + "</pre></html>", "text/html");
                } catch (Throwable anything) {
                    //NOOP
                }
            }
        }



        /**
         * Really handle the request
         *
         * @param path unused
         * @param formArgs the request args
         * @param httpArgs unused
         * @param content unused
         *
         * @throws Exception On badness
         */
        protected void handleRequestInner(String path, Hashtable formArgs,
                                          Hashtable httpArgs, String content)
                throws Exception {
            String imageFile      = (String) formArgs.get(ARG_FILE);
            String request        = (String) formArgs.get(ARG_REQUEST);
            String dataSourceName = (String) formArgs.get(ARG_DATASOURCE);
            String paramName      = (String) formArgs.get(ARG_PARAM);
            String displayName    = (String) formArgs.get(ARG_DISPLAY);
            String properties     = (String) formArgs.get(ARG_PROPERTIES);

            int    width          = Misc.getProperty(formArgs, "width", -1);
            int    height         = Misc.getProperty(formArgs, "height", -1);
            if ((width > 0) && (height > 0)) {
                idv.getStateManager().setViewSize(new Dimension(width,
                        height));
            }

            if (request == null) {
                request = REQ_MAKEIMAGE;
            }
            if (request.equals(REQ_EXIT)) {
                writeResult(true, "<response>ok</response>", "text/xml");
                System.exit(0);
            }
            boolean doMovie  = request.equals(REQ_MAKEMOVIE);
            String  mimeType = (doMovie
                                ? "video/quicktime"
                                : "image/png");
            if (imageFile == null) {
                String uid = "image_" + Misc.getUniqueId() + (doMovie
                        ? ".mov"
                        : ".png");
                imageFile = idv.getObjectStore().getTmpFile(uid);
            }

            System.err.println("image:" + imageFile);
            String bundle = (String) server.getProperties().get("bundle");
            if (bundle == null) {
                bundle = (String) formArgs.get(ARG_BUNDLE);
            }


            long t1 = System.currentTimeMillis();
            if (bundle != null) {
                if ( !(new File(bundle)).exists()) {
                    throw new IllegalArgumentException("Bad bundle file:"
                            + bundle);
                }
                log("Loading bundle:" + bundle);
                String xml = IOUtil.readContents(bundle, getClass());
                idv.getPersistenceManager().decodeXmlInner(xml, false,
                        bundle, "", false);
                log("Done loading bundle");

            } else if ((dataSourceName != null) && (paramName != null)
                       && (displayName != null)) {
                idv.createDisplay(dataSourceName, paramName, displayName,
                                  properties, false);
            } else {
                throw new IllegalStateException(
                    "No bundle or data specified");
            }
            long t2 = System.currentTimeMillis();



            log("Waiting until displays are done");
            IdvManager.waitUntilDisplaysAreDone(idv.getIdvUIManager());
            Misc.sleep(2000);
            log("Done waiting");
            long t3 = System.currentTimeMillis();

            if (doMovie) {
                idv.getImageGenerator().captureMovie(imageFile);
            } else {
                log("Capturing image");
                idv.getImageGenerator().captureImage(imageFile);
                log("Done capturing image");
            }
            File f = new File(imageFile);
            writeBytes(new FileInputStream(f), mimeType, f.length());
            long t4 = System.currentTimeMillis();
            log("Total Time:" + (t4 - t1) + " Load bundle:" + (t2 - t1)
                + " Wait: " + (t3 - t2) + " Capture:" + (t4 - t3));

            idv.cleanup();
        }


        /**
         * _more_
         *
         * @throws Exception _more_
         */
        protected void writeHeaderArgs() throws Exception {
            super.writeHeaderArgs();
            writeLine("Cache-Control: no-cache" + CRLF);
            writeLine("Last-Modified:" + new Date() + CRLF);
        }

    }

}
