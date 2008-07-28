/**
 * $Id: MetadataServlet.java,v 1.90 2008/02/21 17:02:27 oxelson Exp $
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


import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.*;


import org.apache.commons.fileupload.servlet.ServletFileUpload;

import ucar.unidata.plaza.error.ExceptionLogger;

import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;

import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WrapperException;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.StringTokenizer;

import javax.servlet.*;
import javax.servlet.http.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class RepositoryServlet extends HttpServlet {

    /** ExceptionLogger to handle any runtime exceptions */
    private static ExceptionLogger ex = new ExceptionLogger();

    /** Repository object that will be instantiated */
    private static Repository repository;



    /**
     * Create the repository
     *
     * @param request - an HttpServletRequest object that contains the request the client has made of the servlet
     *
     * @throws Exception - if an Exception occurs during the creation of the repository
     */
    private void createRepository(HttpServletRequest request)
            throws Exception {
        repository = new Repository(getInitParams(), request.getServerName(),
                                    request.getServerPort(),true);
        repository.init(null);
    }



    /**
     * Gets any initialization parameters the specified in the Web deployment descriptor (web.xml)
     * Populates the String[] args which will be passed to repository later.
     *
     * @return - an String[] containing the initialization parameters required for repository startup
     */
    private String[] getInitParams() {
        List<String> tokens = new ArrayList<String>();
        for (Enumeration params =
                this.getServletContext().getInitParameterNames();
                params.hasMoreElements(); ) {
            String paramName = (String) params.nextElement();
            if ( !paramName.equals("args")) {
                continue;
            }
            String paramValue =
                getServletContext().getInitParameter(paramName);
            tokens = StringUtil.split(paramValue, ",", true, true);
            break;
        }
        String[] args = (String[]) tokens.toArray(new String[tokens.size()]);
        return args;
    }


    /**
     * _more_
     */
    public void destroy() {
        super.destroy();
        if (repository != null) {
            try {
                repository.close();
            } catch (Exception e) {
                try {
                    ex.logException(ex.getStackTrace(e), "");
                } catch (Exception noop) {}
            }
        }
        repository = null;
    }



    /**
     * Overriding doGet method in HttpServlet. Called by the server via the service method.
     *
     * @param request - an HttpServletRequest object that contains the request the client has made of the servlet
     * @param response - an HttpServletResponse object that contains the response the servlet sends to the client
     *
     * @throws IOException - if an input or output error is detected when the servlet handles the GET request
     * @throws ServletException - if the request for the GET could not be handled
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws IOException, ServletException {

        // there can be only one
        if (repository == null) {
            try {
                createRepository(request);
            } catch (Exception e) {
                ex.logException(ex.getStackTrace(e), request.getRemoteAddr());
                response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                                   "An error has occurred:" + e.getMessage());
                return;
            }
        }

        RequestHandler handler          = new RequestHandler(request);

        Result         repositoryResult = null;
        try {
            // need to support HTTP HEAD request since we are overriding HttpServlet doGet   
            if (request.getMethod().equals("HEAD")) {
                return;
            }

            // create a ucar.unidata.repository.Request object from the relevant info from the HttpServletRequest object
            Request repositoryRequest = new Request(repository,
                                            request.getRequestURI(),
                                            handler.formArgs);
            repositoryRequest.setIp(request.getRemoteAddr());
            repositoryRequest.setOutputStream(response.getOutputStream());
            repositoryRequest.setFileUploads(handler.fileUploads);
            repositoryRequest.setHttpHeaderArgs(handler.httpArgs);
            // create a ucar.unidata.repository.Result object and transpose the relevant info into a HttpServletResponse object
            repositoryResult = repository.handleRequest(repositoryRequest);
        } catch (Throwable e) {
            e = LogUtil.getInnerException(e);
            repository.log("Error:" + e, e);
            ex.logException(ex.getStackTrace(e), request.getRemoteAddr());
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                               e.getMessage());
        }
        if (repositoryResult == null) {
            response.sendError(response.SC_INTERNAL_SERVER_ERROR,
                               "Unknown request:" + request.getRequestURI());
        }
        if (repositoryResult.getNeedToWrite()) {
            List<String> args = repositoryResult.getHttpHeaderArgs();
            if (args != null) {
                for (int i = 0; i < args.size(); i += 2) {
                    String name  = args.get(i);
                    String value = args.get(i + 1);
                    response.setHeader(name, value);
                }
            }




            if (repositoryResult.getRedirectUrl() != null) {
                try {
                    response.sendRedirect(repositoryResult.getRedirectUrl());
                } catch (Exception e) {
                    ex.logException(ex.getStackTrace(e),
                                    request.getRemoteAddr());
                }
            } else if (repositoryResult.getInputStream() != null) {
                try {
                    response.setStatus(repositoryResult.getResponseCode());
                    response.setContentType(repositoryResult.getMimeType());
                    OutputStream output = response.getOutputStream();
                    IOUtil.writeTo(repositoryResult.getInputStream(), output);
                    output.close();
                } catch (Exception e) {
                    ex.logException(ex.getStackTrace(e),
                                    request.getRemoteAddr());
                }
            } else {
                try {
                    response.setStatus(repositoryResult.getResponseCode());
                    response.setContentType(repositoryResult.getMimeType());
                    OutputStream output = response.getOutputStream();
                    output.write(repositoryResult.getContent());
                    output.close();
                } catch (Exception e) {
                    ex.logException(ex.getStackTrace(e),
                                    request.getRemoteAddr());
                }
            }
        }
    }


    /**
     * Overriding doPost method in HttpServlet. Called by the server via the service method.
     * Hands off HttpServletRequest and HttpServletResponse to doGet method.
     *
     * @param request - an HttpServletRequest object that contains the request the client has made of the servlet
     * @param response - an HttpServletResponse object that contains the response the servlet sends to the client
     *
     * @throws IOException - if an input or output error is detected when the servlet handles the GET request
     * @throws ServletException - if the request for the POST could not be handled
     */
    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
            throws IOException, ServletException {
        doGet(request, response);
    }



    /**
     * Class RequestHandler _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private class RequestHandler {

        /** _more_ */
        Hashtable formArgs = new Hashtable();

        /** _more_ */
        Hashtable httpArgs = new Hashtable();

        /** _more_ */
        Hashtable fileUploads = new Hashtable();


        /**
         * _more_
         *
         * @param request _more_
         *
         * @throws IOException _more_
         */
        public RequestHandler(HttpServletRequest request) throws IOException {
            getFormArgs(request);
            getRequestHeaders(request);
        }


        /**
         * Get parameters of this request including any uploaded files.
         *
         * @param request - an HttpServletRequest object that contains the request the client has made of the servlet
         *
         * @throws IOException _more_
         */
        public void getFormArgs(HttpServletRequest request)
                throws IOException {
            if (ServletFileUpload.isMultipartContent(request)) {
                ServletFileUpload upload =
                    new ServletFileUpload(new DiskFileItemFactory());
                try {
                    List     items = upload.parseRequest(request);
                    Iterator iter  = items.iterator();
                    while (iter.hasNext()) {
                        FileItem item = (FileItem) iter.next();
                        if (item.isFormField()) {
                            processFormField(item);
                        } else {
                            processUploadedFile(item, request);
                        }
                    }
                } catch (FileUploadException e) {
                    ex.logException(ex.getStackTrace(e),
                                    request.getRemoteAddr());
                }
            } else {
                // Map containing parameter names as keys and parameter values as map values. 
                // The keys in the parameter map are of type String. The values in the parameter map are of type String array. 
                Map      p  = request.getParameterMap();
                Iterator it = p.entrySet().iterator();
                // Convert Map values into type String. 
                while (it.hasNext()) {
                    Map.Entry    pairs = (Map.Entry) it.next();
                    String       key   = (String) pairs.getKey();
                    String[]     vals  = (String[]) pairs.getValue();
                    StringBuffer sb    = new StringBuffer();
                    if (vals.length > 0) {
                        formArgs.put(key, vals[0]);
                    }
                }
            }
        }


        /**
         * Process any form input.
         *
         * @param item - a form item that was received within a multipart/form-data POST request
         */
        public void processFormField(FileItem item) {
            String name  = item.getFieldName();
            String value = item.getString();
            formArgs.put(name, value);
        }


        /**
         * Process any files uploaded with the form input.
         *
         * @param item - a file item that was received within a multipart/form-data POST request
         * @param request - an HttpServletRequest object that contains the request the client has made of the servlet
         *
         * @throws IOException _more_
         */
        public void processUploadedFile(FileItem item,
                                        HttpServletRequest request)
                throws IOException {
            String fieldName = item.getFieldName();
            String fileName  = item.getName();
            try {
                repository.checkFilePath(fileName);
            } catch (Exception e) {
                ex.logException(ex.getStackTrace(e), request.getRemoteAddr());
                return;
            }
            String contentType = item.getContentType();
            File uploadedFile =
                new File(
                    IOUtil.joinDir(
                        repository.getStorageManager().getUploadDir(),
                        repository.getGUID() + "_" + fileName));
            try {
                item.write(uploadedFile);
            } catch (Exception e) {
                ex.logException(ex.getStackTrace(e), request.getRemoteAddr());
                return;
            }
            fileUploads.put(fieldName, uploadedFile.toString());
            formArgs.put(fieldName, fileName);
        }


        /**
         * Gets the HTTP request headers.  Populate httpArgs.
         *
         * @param request - an HttpServletRequest object that contains the request the client has made of the servlet
         */
        public void getRequestHeaders(HttpServletRequest request) {
            for (Enumeration headerNames = request.getHeaderNames();
                    headerNames.hasMoreElements(); ) {
                String name  = (String) headerNames.nextElement();
                String value = request.getHeader(name);
                httpArgs.put(name, value);
            }
        }

    }

}

