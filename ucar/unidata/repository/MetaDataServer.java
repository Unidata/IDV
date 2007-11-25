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



import ucar.unidata.util.LogUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.StringUtil;

import java.net.*;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Date;
import java.util.List;



/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetaDataServer extends HttpServer {

    Repository repository;

    /**
     * _more_
     *
     * @throws Exception _more_
     */
    public MetaDataServer(String driver, String connectionURL) throws Exception {
        super(8080);
        repository = new Repository(driver, connectionURL);
    }


    protected RequestHandler doMakeRequestHandler(
                                                  final Socket socket) throws Exception {
        return new RequestHandler(this, socket) {
                protected void handleRequest(String path,   Hashtable formArgs,
                                             Hashtable httpArgs,
                                             String content) throws Exception {
                    System.err.println("request:" + path);
                    try {
                        if(path.equals("/radar/query")) {
                            StringBuffer sb = repository.processRadarQuery(formArgs);
                            writeResult(true, sb, Misc.equals("html", formArgs.get("output"))?"text/html":"text/xml");
                        } else if(path.equals("/query")) {
                            writeHtml(repository.processQuery(formArgs));                        
                        } else if(path.equals("/radar/form")) {
                            writeResult(true, repository.processRadarForm(formArgs), "text/html");
                        } else if(path.equals("/radar/liststations")) {
                            writeXml(repository.processRadarList(formArgs, "station","station"));
                        } else if(path.equals("/radar/listproducts")) {
                            writeXml(repository.processRadarList(formArgs, "product","product"));
                        } else if(path.equals("/radar/listcollections")) {
                            writeXml(repository.processRadarListCollection(formArgs));
                        } else if(path.equals("/radar/maketable")) {
                            long t1 = System.currentTimeMillis();
                            try {
                                repository.eval("DROP TABLE nids");
                            } catch(Exception exc) {
                            }
                            try {
                                repository.eval("DROP TABLE collections");
                            } catch(Exception exc) {
                            }
                            repository.makeNidsTable();
                            long t2 = System.currentTimeMillis();
                            writeResult(true, "Time:" + (t2-t1), "text/html");
                        } else {
                            writeResult(true, "Unknown url:" + path, "text/html");
                        }
                    } catch (Exception exc) {
                        System.err.println ("oops:" + exc);
                        LogUtil.logException ("",exc);
                        writeResult(true, LogUtil.getStackTrace(exc), "text/html");
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
    private void processArgs(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("maketable")) {
                try {
                    repository.eval("DROP TABLE nids");
                } catch(Exception exc) {
                }
                try {
                    repository.eval("DROP TABLE collections");
                } catch(Exception exc) {
                }

                repository.makeNidsTable();
            } else {
                repository.eval(args[i]);
            }
        }
    }



    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        String driver = "org.apache.derby.jdbc.EmbeddedDriver";
        String connectionURL = "jdbc:derby:testdb;create=true";
        //        String driver = "com.mysql.jdbc.Driver";
        //        String connectionURL = "jdbc:mysql://localhost:3306/test?zeroDateTimeBehavior=convertToNull";
        MetaDataServer mds = new MetaDataServer(driver, connectionURL);
        mds.processArgs(args);
        mds.init();
    }



}

