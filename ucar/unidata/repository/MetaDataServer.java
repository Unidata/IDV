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

import java.net.*;
import java.io.*;
import java.util.Date;

import java.util.Hashtable;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetaDataServer extends HttpServer  implements Constants {

    /** _more_ */
    Repository repository;


    /**
     * _more_
     *
     * @param args _more_
     * @throws Throwable _more_
     */
    public MetaDataServer(String[] args) throws Throwable {
        super(8080);
        repository = new Repository(args);
        repository.init();
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
        return new MyRequestHandler(this, socket);
    }


    private class MyRequestHandler extends RequestHandler {
        boolean cache=false;
        public MyRequestHandler(MetaDataServer server,Socket socket) throws Exception {
            super(server, socket);
        }

        protected void writeHeaderArgs() throws Exception {
            super.writeHeaderArgs();
            //            if(!cache) {
                writeLine("Cache-Control: no-cache" + CRLF);
                //            }
            writeLine("Last-Modified:" + new Date() + CRLF);
        }
                
        protected void writeContent(Result result)
            throws Exception {
            cache = result.getCacheOk();
            if(result.getRedirectUrl()!=null) {
                redirect(result.getRedirectUrl());
            } else   if(result.getInputStream()!=null) {
                writeResult(result.getRequestOk(), result.getInputStream(),
                            result.getMimeType());
            } else {
                writeResult(result.getRequestOk(), result.getContent(),
                            result.getMimeType());
            }
        }


        protected void handleRequest(String path, Hashtable formArgs,
                                     Hashtable httpArgs, String content)
            throws Exception {
            path = path.trim();
            Result result=null;
            try {
                User user = repository.findUser("jdoe");
                RequestContext context = new RequestContext(user);
                Request request = new Request(repository, path, context,
                                              formArgs);
                if (user == null) {
                    result =
                        new Result("Error",
                                   new StringBuffer("Unknown request:"
                                                    + path));
                } else {
                    result = repository.handleRequest(request);
                } 
            } catch (Throwable exc) {
                exc = LogUtil.getInnerException(exc);
                repository.log("Error:" + exc, exc);
                result =
                    new Result("Error",
                               new StringBuffer(exc.getMessage()+""));
                result.putProperty(PROP_NAVLINKS,
                                   repository.getNavLinks(null));
            }
            if(result == null) {
                result =   new Result("Error",
                                      new StringBuffer("Unknown request:"
                                                       + path));
            }
            writeContent(result);
        }
    }



    /**
     * _more_
     *
     * @param msg _more_
     * @param exc _more_
     */
    protected void handleError(String msg, Exception exc) {
        repository.log(msg,exc);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Throwable _more_
     */
    public static void main(String[] args) throws Throwable {
        MetaDataServer mds = new MetaDataServer(args);
        mds.init();
    }



}

