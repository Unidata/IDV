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

    /** _more_          */
    Repository repository;

    /**
     * _more_
     *
     *
     * @param driver _more_
     * @param connectionURL _more_
     * @throws Exception _more_
     */
    public MetaDataServer(String[]args, String driver, String connectionURL)
            throws Exception {
        super(8080);
        //"jeff", "mypassword");
        for (int i = 0; i < args.length; i++) {
        }

        repository = new Repository(driver, connectionURL,null,null);
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
            protected void handleRequest(String path, Hashtable formArgs,
                                         Hashtable httpArgs, String content)
                    throws Exception {
                System.err.println("request:" + path);
                try {
                    if (path.equals("/query")) {
                        writeResult(true, repository.processRadarQuery(formArgs),
                                    Misc.equals("html",
                                        formArgs.get("output"))
                                    ? "text/html"
                                    : "text/xml");
                    } else if (path.equals("/sql")) {
                        writeHtml(repository.processQuery(formArgs));
                    } else if (path.equals("/searchform")) {
                        writeResult(true,
                                    repository.makeQueryForm(formArgs),
                                    "text/html");
                    } else if (path.equals("/radar/liststations")) {
                        writeXml(repository.processRadarList(formArgs,
                                "station", "station"));
                    } else if (path.equals("/radar/listproducts")) {
                        writeXml(repository.processRadarList(formArgs,
                                "product", "product"));
                    } else if (path.equals("/listgroups")) {
                        writeXml(repository.listGroups(formArgs));
                    } else if (path.equals("/showgroup")) {
                        writeHtml(repository.showGroup(formArgs));
                    } else if (path.equals("/showfile")) {
                        writeHtml(repository.showFile(formArgs));
                    } else {
                        writeResult(false, "Unknown url:" + path, "text/html");
                    }
                } catch (Exception exc) {
                    System.err.println("Error:" + exc);
                    String trace = LogUtil.getStackTrace(exc);
                    writeResult(true, "<pre>" + trace + "</pre>",
                                "text/html");
                }
            }
        };
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
        MetaDataServer mds = new MetaDataServer(args,driver, connectionURL);
        mds.init();
    }



}

