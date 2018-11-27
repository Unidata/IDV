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

package ucar.unidata.idv;


import ucar.unidata.idv.ui.ImageGenerator;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.xml.XmlUtil;

import java.net.InetAddress;
import java.net.Socket;

import java.util.Hashtable;
import java.util.List;



/**
 * This provides http based access to a stack trace and enables the user to shutdown the IDV.
 * This only is responsive to incoming requests from localhost
 * the urls this provides are:
 * http://localhost:<port>/stack.html
 * http://localhost:<port>/info.html
 * http://localhost:<port>/shutdown.html
 *
 * @author IDV development team
 */
public class IdvMonitor extends HttpServer {

    public static final String CMD_PING = "/ping";
    public static final String CMD_LOADBUNDLE = "/loadbundle";
    public static final String CMD_LOADISL= "/loadisl";

    public static final String ARG_BUNDLE = "bundle";
    public static final String ARG_ISL = "isl";
    public static final String ARG_ISLPATH = "islpath";

    /** _more_          */
    private IntegratedDataViewer idv;

    /** The localhost */
    private InetAddress localHost;

    /**
     * _more_
     *
     * @param idv _more_
     * @param port _more_
     */
    public IdvMonitor(IntegratedDataViewer idv, int port) {
        super(port);
        this.idv = idv;
    }


    /**
     * Make the handler for this request. Check if the client is coming from localhost
     * if not then return null.
     *
     * @param socket incoming socket
     * @return handler or null
     *
     * @throws Exception _more_
     */
    protected RequestHandler doMakeRequestHandler(Socket socket)
            throws Exception {
        if (localHost == null) {
            localHost = InetAddress.getLocalHost();
        }
        InetAddress inet = socket.getInetAddress();
        if ( !inet.getHostAddress().equals("127.0.0.1")) {
            return null;
        }
        return new MonitorRequestHandler(idv, this, socket);
    }


    /**
     * Class OneInstanceRequestHandler the handler
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.11 $
     */
    public class MonitorRequestHandler extends HttpServer.RequestHandler {

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
        public MonitorRequestHandler(IntegratedDataViewer idv,
                                     HttpServer server, Socket socket)
                throws Exception {
            super(server, socket);
            this.idv = idv;
        }

        /**
         * _more_
         *
         * @param sb _more_
         *
         * @throws Exception _more_
         */
        private void decorateHtml(StringBuffer sb) throws Exception {
            String header =
                "<a href=stack.html>Stack Trace</a>&nbsp;|&nbsp;"
                + "<a href=info.html>System Information</a>&nbsp;|&nbsp;"
                + "<a href=shutdown.html>Shutdown</a><hr>";
            writeResult(true, header + sb.toString(), "text/html");
        }


        /**
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
            //            System.err.println("handleRequest start:" + path);
            if (path.equals("/stack.html")) {
                StringBuffer stack = LogUtil.getStackDump(true);
                decorateHtml(stack);
            } else if (path.equals("/info.html")) {
                StringBuffer extra = idv.getIdvUIManager().getSystemInfo();
                extra.append("<H3>Data Sources</H3>");
                extra.append("<div style=\"margin-left:20px;\">");
                extra.append(idv.getDataManager().getDataSourceHtml());
                extra.append("</div>");
                extra.append(idv.getPluginManager().getPluginHtml());
                extra.append(idv.getResourceManager().getHtmlView());
                decorateHtml(extra);
            } else if (path.equals("/shutdown.html")) {
                decorateHtml(
                    new StringBuffer(
                        "Really shutdown the IDV?<br><a href=\"reallyshutdown.html\">Yes</a>"));
            } else if (path.equals("/reallyshutdown.html")) {
                writeResult(true, "OK, IDV is shutting down", "text/html");
                System.exit(0);
            } else if(path.equals(CMD_PING)) {
                writeResult(true, "ok", "text/html");
            } else if(path.equals(CMD_LOADBUNDLE)) {
                String bundle = (String) formArgs.get(ARG_BUNDLE);
                if(bundle == null) {
                    writeResult(false,"No " + ARG_BUNDLE +" argument given" , "text/html");
                    return;
                } else {
                    String xml = IOUtil.readContents(bundle, getClass());
                    idv.getPersistenceManager().decodeXmlInner(xml, false,
                                                               bundle, "", false);
                    writeResult(true, "ok", "text/html");
                }

            } else if(path.equals(CMD_LOADISL)) {
                String isl = (String) formArgs.get(ARG_ISL);
                if(isl == null) {
                    String islPath = (String) formArgs.get(ARG_ISLPATH);
                    if(islPath !=null) {
                        isl =   IOUtil.readContents(islPath, getClass());
                    }
                }


                if(isl == null) {
                    writeResult(false,getReturnXml(false, "No " + ARG_ISL +" argument given") , "text/xml");
                    return;
                } else {
                    String tmpFile = idv.getStore().getTmpFile("tmp.isl");
                    IOUtil.writeFile(tmpFile, isl);
                    ImageGenerator imageGenerator = idv.getImageGenerator();
                    //interactive = false
                    if(!imageGenerator.processScriptFile(tmpFile, new Hashtable(), false)) {
                        String error = imageGenerator.getError();
                        writeResult(true, getReturnXml(false, error), "text/xml");
                    }
                    List<String> results = imageGenerator.getResults();
                    String result = "";
                    if(results.size()>0) result = results.get(0);
                    writeResult(true, getReturnXml(true,result), "text/xml");
                }
            } else {
                decorateHtml(new StringBuffer("Unknown url:" + path));
            }
            //            System.err.println("handleRequest end:" + path);
        }
    }


    /**
     * Utility to create the return xml format
     * @param ok Was the call successful or not
     * @param msg The message to return
     */
    private String getReturnXml(boolean ok, String msg) {
        msg = XmlUtil.encodeString(msg);
        System.err.println("msg:" + msg);
        return "<result ok=\"" + ok +"\">" + msg +"</result>";
    }



}
