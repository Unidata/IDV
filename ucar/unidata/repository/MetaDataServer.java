/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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



package ucar.unidata.repository;


import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TextResult;

import java.io.*;

import java.net.*;


import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetaDataServer extends HttpServer {

    /** _more_ */
    Repository repository;

    /** _more_          */
    String template;

    /**
     * _more_
     *
     *
     *
     * @param args _more_
     * @param driver _more_
     * @param connectionURL _more_
     * @throws Exception _more_
     */
    public MetaDataServer(String[] args, String driver, String connectionURL)
            throws Exception {
        super(8080);
        String user     = null;
        String password = null;
        //user ="jeff";password= "mypassword";
        for (int i = 0; i < args.length; i++) {}

        template =
            IOUtil.readContents("/ucar/unidata/repository/template.html",
                                getClass());
        repository = new Repository(driver, connectionURL, user, password);
        repository.setUrlBase("/repository");

    }




    /**
     * _more_
     *
     * @param socket _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    protected RequestHandler doMakeRequestHandler(final Socket socket)
            throws Exception {
        return new RequestHandler(this, socket) {
            private void writeContent(boolean ok, TextResult result)
                    throws Exception {
                if (result.isHtml()) {
                    template = IOUtil.readContents(
                        "/ucar/unidata/repository/template.html", getClass());
                    String html = StringUtil.replace(template, "%content%",
                                      result.getContent().toString());
                    html = StringUtil.replace(html, "%title%",
                            result.getTitle());
                    html = StringUtil.replace(html, "%root%",
                            repository.getUrlBase());
                    html = StringUtil.replace(html, "%links%",
                            repository.getNavLinks());
                    writeResult(ok, html, result.getMimeType());
                } else {
                    writeResult(ok, result.getContent(),
                                result.getMimeType());
                }
            }

            protected void writeHeaderArgs() throws Exception {
                //            writeLine("Date: Fri, 12 Jan 2007 00:02:44 GMT"+CRLF);
                writeLine("Cache-Control: no-cache" + CRLF);
                writeLine("Last-Modified: Wed, 15 Nov 1995 04:58:08 GMT"
                          + CRLF);
                //            writeLine("Last-Modified:" + new Date()+CRLF);
            }

            protected void handleRequest(String path, Hashtable formArgs,
                                         Hashtable httpArgs, String content)
                    throws Exception {
                path = path.trim();
                System.err.println("request:" + path + ":");
                try {
                    TextResult result =
                        repository.handleRequest(new Request(path,
                            new RequestContext(), formArgs));
                    if (result != null) {
                        writeContent(true, result);
                    } else {
                        //Try to serve up the file
                        String type = "text/html";
                        try {
                            path = StringUtil.replace(path,
                                    repository.getUrlBase(), "");
                            InputStream is =
                                IOUtil.getInputStream(
                                    "/ucar/unidata/repository/htdocs" + path,
                                    getClass());
                            byte[] bytes = IOUtil.readBytes(is);
                            if (path.endsWith(".html")) {
                                writeContent(true,
                                             new TextResult("",
                                                 new String(bytes)));
                            } else {
                                if (path.endsWith(".gif")) {
                                    type = "image/gif";
                                } else if (path.endsWith(".jpg")) {
                                    type = "image/jpg";
                                } else if (path.endsWith(".png")) {
                                    type = "image/png";
                                } else {
                                    type = "";
                                }
                                writeResult(true, bytes, type);
                            }
                        } catch (Exception e) {
                            System.err.println("Error:" + e);
                            String trace = LogUtil.getStackTrace(e);
                            writeResult(false, "error:" + trace, "text/html");
                        }
                    }

                    //                    } else {
                    //                        writeContent(false, new TextResult("Error", new StringBuffer("Unknown url:" + path)));
                    //                    }

                } catch (Throwable exc) {
                    System.err.println("Error:" + exc);
                    exc.printStackTrace();
                    String trace = LogUtil.getStackTrace(exc);
                    //                    System.err.println(trace);
                    writeContent(true,
                                 new TextResult("Error",
                                     new StringBuffer("<pre>" + trace
                                         + "</pre>")));
                }
            }
        };
    }






    /**
     * _more_
     *
     * @param msg _more_
     * @param exc _more_
     */
    protected void handleError(String msg, Exception exc) {
        System.err.println("Error:" + exc);
        exc.printStackTrace();
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String driver        = "org.apache.derby.jdbc.EmbeddedDriver";
        String connectionURL = "jdbc:derby:testdb;create=true";
        //        String driver = "com.mysql.jdbc.Driver";
        //        String connectionURL = "jdbc:mysql://localhost:3306/test?zeroDateTimeBehavior=convertToNull";
        MetaDataServer mds = new MetaDataServer(args, driver, connectionURL);
        mds.init();
    }



}

