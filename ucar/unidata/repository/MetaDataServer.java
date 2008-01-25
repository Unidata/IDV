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
import ucar.unidata.util.WrapperException;

import java.io.*;

import java.net.*;

import java.util.Date;
import java.util.Enumeration;
import java.util.List;


import java.util.Hashtable;


import org.apache.commons.fileupload.MultipartStream;

/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class MetaDataServer extends HttpServer implements Constants {

    /** _more_ */
    Repository repository;

    String[] args;

    int port = 8080;
    /**
     * _more_
     *
     * @param args _more_
     * @throws Throwable _more_
     */
    public MetaDataServer(String[] args) throws Throwable {
        super(8080);
        this.args = args;
        for(int i=0;i<args.length;i++) {
            if(args[i].equals("-port")) {
                port = new Integer(args[i+1]).intValue();
                setPort(port);
                break;
            }
        }
    }



    public void init()  {
        try {
            repository = new Repository(args, "http://localhost:" + port);
            repository.init();
        } catch(Exception exc) {
            exc.printStackTrace();
            throw new WrapperException(exc);
        }
        super.init();
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


    /**
     * Class MyRequestHandler _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class MyRequestHandler extends RequestHandler {

        Hashtable fileUploads = new Hashtable();

        Result result = null;

        /** _more_ */
        boolean cache = false;

        /**
         * _more_
         *
         * @param server _more_
         * @param socket _more_
         *
         * @throws Exception _more_
         */
        public MyRequestHandler(MetaDataServer server, Socket socket)
                throws Exception {
            super(server, socket);
        }


        protected void handleFileUpload(String attrName, String filename,Hashtable props, Hashtable args, MultipartStream multipartStream) throws Exception {
            Repository.checkFilePath(filename);
            int cnt = 0;
            File f= new File(IOUtil.joinDir(repository.getFileUploadDirectory(), filename));
            while(f.exists()) {
                f = new File(IOUtil.joinDir(repository.getFileUploadDirectory(), (cnt++) + "_"+filename));
            }
            //TODO: Check for security hole with the file upload
            fileUploads.put(attrName, f.toString());
            OutputStream output=new FileOutputStream(f);
            multipartStream.readBodyData(output);
        }

        /**
         * _more_
         *
         * @throws Exception _more_
         */
        protected void writeHeaderArgs() throws Exception {
            super.writeHeaderArgs();
            if ( !cache) {
                writeLine("Cache-Control: no-cache" + CRLF);
            }
            writeLine("Last-Modified:" + new Date() + CRLF);
            if(result!=null) {
                List<String> args = result.getHttpHeaderArgs ();
                if(args!=null) {
                    for (int i=0;i<args.size();i+=2) {
                        String name = args.get(i);
                        String value = args.get(i+1);
                        writeLine(name+":"+ value + CRLF);                        
                    }
                }
            }

        }

        /**
         * _more_
         *
         * @param result _more_
         *
         * @throws Exception _more_
         */
        protected void writeContent(Result result) throws Exception {
            cache = result.getCacheOk();
            if (result.getRedirectUrl() != null) {
                cache = false;
                redirect(result.getRedirectUrl());
            } else if (result.getInputStream() != null) {
                writeResult(result.getRequestOk(), result.getInputStream(),
                            result.getMimeType());
            } else {
                writeResult(result.getRequestOk(), result.getContent(),
                            result.getMimeType());
            }
        }


        /**
         * _more_
         *
         * @param path _more_
         * @param formArgs _more_
         * @param httpArgs _more_
         * @param content _more_
         *
         * @throws Exception _more_
         */
        protected void handleRequest(String path, Hashtable formArgs,
                                     Hashtable httpArgs, String content)
                throws Exception {
            path = path.trim();
            try {
                RequestContext context = new RequestContext(null);
                Request request = new Request(repository, path, context,
                                      formArgs);
                request.setFileUploads(fileUploads);
                request.setHttpHeaderArgs(httpArgs);
                result = repository.handleRequest(request);
            } catch (Throwable exc) {
                exc = LogUtil.getInnerException(exc);
                repository.log("Error:" + exc, exc);
                result = new Result("Error",
                                    new StringBuffer(exc.getMessage() + ""));
                result.putProperty(PROP_NAVLINKS,
                                   repository.getNavLinks(null));
            }
            if (result == null) {
                result = new Result("Error",
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
        repository.log(msg, exc);
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Throwable _more_
     */
    public static void main(String[] args) throws Throwable {
        try {
            MetaDataServer mds = new MetaDataServer(args);
            mds.init();
        } catch(Exception exc) {
            LogUtil.printExceptionNoGui(null,"Error in main",exc);
            System.exit(1);
        }
    }



}

