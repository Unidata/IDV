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
import ucar.unidata.util.TextResult;


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

    String  template;

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
        String user = null;
        String password = null;
        //user ="jeff";password= "mypassword";
        for (int i = 0; i < args.length; i++) {
        }

        template =
            IOUtil.readContents("/ucar/unidata/repository/template.html",
                                getClass());
        repository = new Repository(driver, connectionURL,user,password);
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
                private void writeContent(boolean ok, TextResult result) throws Exception {
                    if(result.isHtml()) {
                        String html = StringUtil.replace(template, "%content%", result.getContent().toString());
                        html = StringUtil.replace(html, "%title%", result.getTitle());
                        writeResult(ok, html,result.getMimeType());
                    } else {
                        writeResult(ok, result.getContent(),result.getMimeType());
                    }
                }


            protected void handleRequest(String path, Hashtable formArgs,
                                         Hashtable httpArgs, String content)
                    throws Exception {
                System.err.println("request:" + path);
                try {
                    if (path.equals(repository.getUrlBase()+"/query")) {
                        boolean xml  = Misc.equals("xml",
                                                   formArgs.get("output"));
                        StringBuffer result = repository.processQuery(formArgs);
                        writeContent(true, new TextResult("Query Results",result,(xml?TextResult.TYPE_XML:TextResult.TYPE_HTML)));

                    } else if (path.equals(repository.getUrlBase()+"/sql")) {
                        writeContent(true, repository.processSql(formArgs));
                    } else if (path.equals(repository.getUrlBase()+"/searchform")) {
                        writeContent(true, repository.makeQueryForm(formArgs));
                    } else if (path.equals(repository.getUrlBase()+"/radar/liststations")) {
                        writeContent(true,repository.processRadarList(formArgs,
                                                                   "station", "station"));
                    } else if (path.equals(repository.getUrlBase()+"/radar/listproducts")) {
                        writeContent(true,repository.processRadarList(formArgs,
                                "product", "product"));
                    } else if (path.equals(repository.getUrlBase()+"/listgroups")) {
                        writeContent(true,repository.listGroups(formArgs));
                    } else if (path.equals(repository.getUrlBase()+"/showgroup")) {
                        writeContent(true, repository.showGroup(formArgs));
                    } else if (path.equals(repository.getUrlBase()+"/showfile")) {
                        writeContent(true,repository.showFile(formArgs));
                    } else {
                        writeContent(false, new TextResult("Error", new StringBuffer("Unknown url:" + path)));
                    }
                } catch (Throwable exc) {
                    System.err.println("Error:" + exc);
                    String trace = LogUtil.getStackTrace(exc);
                    writeContent(true, new TextResult("Error", new StringBuffer("<pre>" + trace + "</pre>")));
                }
            }
        };
    }






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
        MetaDataServer mds = new MetaDataServer(args,driver, connectionURL);
        mds.init();
    }



}

