/**
 * $Id: IntegratedDataViewer.java,v 1.652 2007/08/22 11:55:41 jeffmc Exp $
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


package ucar.unidata.idv;


import ucar.unidata.util.HttpServer;

import java.util.Hashtable;

import java.lang.management.*;
import java.net.InetAddress;
import java.net.Socket;



/**
 * This provides http based access to a stack trace and enables the user to shutdown the IDV.
 * This only is responsive to incoming requests from localhost
 * @author IDV development team
 */

public class IdvMonitor extends HttpServer {
    IntegratedDataViewer idv;

    /** The localhost */
    InetAddress localHost;

    public IdvMonitor(IntegratedDataViewer idv, int port) {
        super(port);
        this.idv = idv;
    }

    public StringBuffer  getStackDump() {
        StringBuffer longSB = new StringBuffer();
        StringBuffer shortSB = new StringBuffer();

        ThreadMXBean threadBean =ManagementFactory.getThreadMXBean();
        long[] ids = threadBean.getAllThreadIds();
        StringBuffer blockedSB = new StringBuffer();
        StringBuffer otherSB = new StringBuffer();
        for(int i=0;i<ids.length;i++) {
            ThreadInfo info =   threadBean.getThreadInfo(ids[i],Integer.MAX_VALUE);
            if(info==null) continue;
            StackTraceElement[]stack =	info.getStackTrace();
            shortSB.append(info);
            String extra = "";
            String style="";
            StringBuffer sb= otherSB;
            if(info.getThreadState()==Thread.State.WAITING) {
                extra = " on " + info.getLockName();
            } else   if(info.getThreadState()==Thread.State.BLOCKED) {
                style="  background-color:#cccccc; ";
                extra = " on " + info.getLockName() + " held by <a href=\"#id" + info.getLockOwnerId()+"\">" + info.getLockOwnerName() + " id:" + info.getLockOwnerId()+"</a>";
                sb = blockedSB;
            }
            sb.append("<a name=\"id" +ids[i]+"\"></a>");
            sb.append("<span style=\"" + style +"\">&quot;" +info.getThreadName() +"&quot;" +  " ID:" + ids[i] +"  "+ info.getThreadState()+extra+"</span>\n");
            for(int stackIdx=0;stackIdx<stack.length;stackIdx++) {
                sb.append("    " +stack[stackIdx]+"\n");
            }
            sb.append("\n\n");
        }
        longSB.append(blockedSB);
        longSB.append(otherSB);

        //        shortSB.append("--------------------------------------------------------------------------------\n");
        //        shortSB.append("Full stack trace\n");
        //        shortSB.append("--------------------------------------------------------------------------------\n");
        //        shortSB.append(longSB);
        //        return shortSB;
        return longSB;
    }

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
    public  class MonitorRequestHandler extends HttpServer.RequestHandler {

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
            if(path.equals("/stack.html")) {
                StringBuffer stack = getStackDump();
                writeResult(true,  "<pre>" +stack.toString()+"</pre>","text/html");
            } else  if(path.equals("/shutdown.html")) {
                writeResult(true,  "Really shutdown the IDV?<br><a href=\"reallyshutdown.html\">Yes</a>","text/html");
            } else  if(path.equals("/reallyshutdown.html")) {
                writeResult(true,  "OK, IDV is shutting down","text/html");
                System.exit(0);
            } else{
                writeResult(false,  "Unknown url:" + path,"text/html");
            }
        }
    }






}

