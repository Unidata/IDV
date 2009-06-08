/*
 * $Id: HttpServer.java,v 1.12 2007/08/06 23:02:43 jeffmc Exp $
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




package ucar.unidata.util;


import org.apache.commons.fileupload.MultipartStream;


import java.io.*;

import java.lang.*;


import java.net.*;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import java.util.regex.*;


/**
 * Class HttpServer. A simple http server.
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.12 $
 */
public class HttpServer {

    /** _more_          */
    public static final int RESPONSE_OK = 200;

    /** _more_          */
    public static final int RESPONSE_NOTFOUND = 404;

    /** _more_          */
    public static final int RESPONSE_UNAUTHORIZED = 401;

    /** _more_          */
    public static final int RESPONSE_INTERNALERROR = 500;



    /** get type */
    public static final String TYPE_GET = "GET";

    /** post type */
    public static final String TYPE_POST = "POST";


    /** listening socket */
    private ServerSocket serverSocket;

    /** port */
    private int port = 8080;

    /** currently running */
    private boolean running = false;


    /** _more_          */
    protected Hashtable serverProperties = new Hashtable();


    /**
     * Create me with the given port
     *
     * @param port The port to listen on
     */
    public HttpServer(int port) {
        this.port = port;
    }

    /**
     * _more_
     *
     * @param propertyFile _more_
     */
    public HttpServer(String propertyFile) {
        serverProperties = Misc.readProperties(propertyFile, null,
                getClass());
        port = Misc.getProperty(serverProperties, "port", port);
    }


    /**
     * _more_
     *
     * @param port _more_
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getPort() {
        return port;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    public Hashtable getProperties() {
        return serverProperties;
    }

    /**
     * _more_
     */
    public void init() {
        initServer();
    }

    /**
     * _more_
     *
     * @param msg _more_
     * @param exc _more_
     */
    protected void handleError(String msg, Exception exc) {
        LogUtil.logException(msg, exc);
    }

    /**
     * _more_
     *
     * @param socket _more_
     */
    protected void initServerSocket(ServerSocket socket) {}


    /**
     * Initialize the socket and start reading
     */
    private void initServer() {
        try {
            serverSocket = new ServerSocket(port);
            initServerSocket(serverSocket);
        } catch (IOException e) {
            throw new WrapperException(e);
        }
        try {
            running = true;
            while (running) {
                Socket socket = serverSocket.accept();
                try {
                    RequestHandler request = doMakeRequestHandler(socket);
                    if (request == null) {
                        try {
                            socket.close();
                        } catch (Exception exc) {}
                        continue;
                    }
                    Misc.run(request);
                } catch (java.net.SocketException exc) {
                    //noop
                } catch (Exception exc) {
                    handleError("Error reading connection", exc);
                }
            }
            serverSocket.close();
        } catch (Exception e) {
            handleError("Error opening connection", e);
        }
    }


    /**
     * Factory method to create the request handler
     *
     * @param socket The socket
     *
     * @return The handler
     *
     * @throws Exception On badness
     */
    protected RequestHandler doMakeRequestHandler(Socket socket)
            throws Exception {
        return new RequestHandler(this, socket);
    }

    /**
     * Class RequestHandler handles requests
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.12 $
     */
    public static class RequestHandler implements Runnable {

        /** crlf */
        public final static String CRLF = "\r\n";

        /** _more_          */
        public final static char LF = '\n';

        /** The socket */
        Socket socket;

        /** Input stream */
        InputStream input;

        /** Output stream */
        OutputStream output;


        /** Back reference to server */
        protected HttpServer server;

        /**
         * Ctor
         *
         *
         * @param server The server
         * @param socket The socket
         *
         * @throws Exception On badness
         */
        public RequestHandler(HttpServer server, Socket socket)
                throws Exception {
            this.server = server;
            this.socket = socket;
            this.input  = socket.getInputStream();
            this.output = socket.getOutputStream();
            this.input  = new BufferedInputStream(input);
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public Socket getSocket() {
            return socket;
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public OutputStream getOutputStream() {
            return output;
        }

        /**
         * Run the read
         */
        public void run() {
            try {
                processRequest();
            } catch (java.net.SocketException se) {
                //noop
            } catch (Exception e) {
                server.handleError("exc:", e);
            }
            try {
                socket.close();
                output.close();
            } catch (Exception e) {
                //                LogUtil.logException("exc:", e);
            }
        }


        /**
         * _more_
         *
         * @param msg _more_
         */
        protected void log(String msg) {
            System.err.println(msg);
        }

        /**
         * _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        private String readLine() throws Exception {
            StringBuffer sb = new StringBuffer();
            int lineLength = 0;
            while (true) {
                char c = (char) input.read();
                if ((c == -1) || (c == LF)) {
                    return sb.toString().trim();
                }
                if(!Character.isDefined(c)) {
                    return sb.toString().trim();
                }
                sb.append(c+"");
                if(lineLength++>1000) {
                    Misc.sleep(1000);
                    throw new IllegalArgumentException("bad line");
                }
            }
        }

        /**
         * Process the request
         *
         * @throws Exception On badness
         */
        private void processRequest() throws Exception {
            String    firstLine = null;
            Hashtable props     = new Hashtable();
            int lineCnt = 0;
            while (true) {
                String headerLine = readLine();
                if(lineCnt++>500) {
                    Misc.sleep(1000);
                    throw new IllegalArgumentException("bad header");
                }
                if (headerLine.equals(CRLF) || headerLine.equals("")) {
                    break;
                }
                //                System.err.println ("hdr:" + headerLine);
                if (firstLine == null) {
                    //                    System.err.println("first line:" + headerLine);
                    firstLine = headerLine;
                    continue;
                }

                int index = headerLine.indexOf(":");
                if (index < 0) {
                    continue;
                }

                String propName  = headerLine.substring(0, index).trim();
                String propValue = headerLine.substring(index + 1).trim();
                props.put(propName, propValue);
            }

            if (firstLine == null) {
                return;
            }
            StringTokenizer s           = new StringTokenizer(firstLine);
            String          requestType = s.nextToken();
            Hashtable       args        = new Hashtable();
            String          path        = s.nextToken();
            String[]        toks        = StringUtil.split(path, "?", 2);
            if (toks != null) {
                path = toks[0];
            }

            String contentType = (String) props.get("Content-Type");

            if (requestType.equals(TYPE_POST) && (contentType != null)
                    && contentType.trim().startsWith("multipart/form-data")) {
                parseMultiPartFormData(contentType, props, args);
                handleRequest(path, args, props, null);
                return;
            }


            String contentLength = (String) props.get("Content-Length");
            String contentString = null;
            byte[] content       = null;
            if (contentLength != null) {
                content = new byte[100000];
                int    len       = new Integer(contentLength).intValue();
                byte[] buffer    = new byte[100000];
                int    totalRead = 0;
                while (totalRead < len) {
                    int howMany = input.read(buffer, 0, buffer.length);
                    if (howMany < 0) {
                        break;
                    }
                    if (totalRead + howMany > content.length) {
                        byte[] tmp = content;
                        content = new byte[content.length + 100000];
                        System.arraycopy(tmp, 0, content, 0, totalRead);
                    }
                    System.arraycopy(buffer, 0, content, totalRead, howMany);
                    totalRead += howMany;
                }
                byte[] tmp = content;
                content = new byte[totalRead];
                System.arraycopy(tmp, 0, content, 0, totalRead);
            }


            if (content != null) {
                contentString = new String(content, 0, content.length);
            }
            if (requestType.equals(TYPE_GET)) {
                if ((toks != null) && (toks.length > 1)) {
                    parseArgs(toks[1], args);
                }
            } else if (requestType.equals(TYPE_POST)) {
                if (okToParseContent(path, contentString, args)) {
                    parseArgs(contentString, args);
                }
            } else {
                throw new IllegalArgumentException("unknown type: "
                        + requestType);
            }


            handleRequest(path, args, props, contentString);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param props _more_
         * @param args _more_
         */
        private void parseMultiPartFormData(String type, Hashtable props,
                                            Hashtable args) {
            String       boundary = null;
            List<String> toks     = StringUtil.split(type, ";", true, true);
            for (String tok : toks) {
                if (tok.indexOf("=") > 0) {
                    List<String> subToks = StringUtil.split(tok, "=", true,
                                               true);
                    if (subToks.size() == 2) {
                        String name  = subToks.get(0).trim();
                        String value = subToks.get(1).trim();
                        if (name.equals("boundary")) {
                            boundary = value;
                        }
                    }
                }
            }
            if (boundary == null) {
                throw new IllegalArgumentException(
                    "Could not find multipart boundary in:" + type);
            }
            //??
            try {
                MultipartStream multipartStream = new MultipartStream(input,
                                                      boundary.getBytes(),
                                                      1000000);
                boolean nextPart = multipartStream.skipPreamble();
                int     cnt      = 1;

                Pattern namePattern =
                    Pattern.compile("[\\s;:]+name\\s*=\\s*\"([^\"]+)\"");
                Pattern filenamePattern =
                    Pattern.compile("[\\s;:]+filename\\s*=\\s*\"([^\"]+)\"");

                while (nextPart) {
                    String header = multipartStream.readHeaders();
                    List lineToks = StringUtil.split(header, "\n", true,
                                        true);
                    String attrName = null;
                    String filename = null;
                    for (int i = 0; i < lineToks.size(); i++) {
                        String line  = (String) lineToks.get(i);
                        int    index = line.indexOf(":");
                        if (index < 0) {
                            continue;
                        }
                        String propName  = line.substring(0, index).trim();
                        String propValue = line.substring(index + 1).trim();
                        if (propName.equals("Content-Disposition")) {
                            Matcher nameMatcher =
                                namePattern.matcher(propValue);
                            Matcher filenameMatcher =
                                filenamePattern.matcher(propValue);
                            if (nameMatcher.find()) {
                                attrName = nameMatcher.group(1);
                            }
                            if (filenameMatcher.find()) {
                                filename = filenameMatcher.group(1);
                            }
                        }
                        if (attrName == null) {
                            throw new IllegalArgumentException(
                                "Could not find name attribute in multipart");
                        }
                    }
                    if (filename != null) {
                        handleFileUpload(attrName, filename, props, args,
                                         multipartStream);
                    } else {
                        ByteArrayOutputStream output =
                            new ByteArrayOutputStream();
                        multipartStream.readBodyData(output);
                        String value = new String(output.toByteArray());
                        addArg(args, attrName, value);
                    }
                    if ( !multipartStream.readBoundary()) {
                        break;
                    }
                }
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }


        protected void addArg(Hashtable args, String name, String value) {
            Object obj = args.get(name);
            if(obj!=null) {
                if(obj instanceof List) {
                    ((List)obj).add(value);
                    return;
                } else {
                    List l = new ArrayList();
                    l.add(obj);
                    l.add(value);
                    args.put(name, l);
                    return;
                }
            }
            args.put(name, value);
        }


        /**
         * _more_
         *
         * @param attrName _more_
         * @param filename _more_
         * @param props _more_
         * @param args _more_
         * @param multipartStream _more_
         *
         * @throws Exception _more_
         */
        protected void handleFileUpload(String attrName, String filename,
                                        Hashtable props, Hashtable args,
                                        MultipartStream multipartStream)
                throws Exception {
            throw new IllegalArgumentException(
                "handleFileUpload not implemented");
        }


        /**
         *
         * _more_
         *
         * @param path _more_
         * @param contentString _more_
         * @param httpArgs _more_
         *
         * @return _more_
         */
        protected boolean okToParseContent(String path, String contentString,
                                           Hashtable httpArgs) {
            return true;
        }


        /**
         * Utility to parse name=value& args
         *
         * @param args Args
         * @param ht Table to put args in
         */
        protected void parseArgs(String args, Hashtable ht) {
            if (args == null) {
                return;
            }
            List argToks = StringUtil.split(args, "&", true, true);
            for (int i = 0; i < argToks.size(); i++) {
                String tok = argToks.get(i).toString();
                String[] toks = StringUtil.split(tok, "=", 2);
                if ((toks == null) || (toks.length < 1)) {
                    continue;
                }
                //                System.err.println ("\t" + toks[1]);
                addArg(ht,decode(toks[0]), decode(toks[1]));
            }

        }


        /**
         * Handle the request.
         *
         * @param path Url path
         * @param formArgs get or post args
         * @param httpArgs http  headers
         * @param content _more_
         *
         * @throws Exception On badness
         */
        protected void handleRequest(String path, Hashtable formArgs,
                                     Hashtable httpArgs, String content)
                throws Exception {
            StringBuffer sb   = new StringBuffer("<html>");
            Enumeration  keys = formArgs.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                sb.append(key + "=" + formArgs.get(key) + "<p>\n");
            }
            sb.append("</html>");

            writeResult(true, sb.toString(), "text/html");
        }

        /**
         * Utility to  decode the string
         *
         * @param s The string
         *
         * @return The decoded string
         */
        private String decode(String s) {
            try {
                return URLDecoder.decode(s, "UTF-8");
            } catch (Exception exc) {
                System.err.println("err:" + exc);
                return s;
            }
        }


        /**
         * Write the line to the output
         *
         * @param line Line of text
         *
         * @throws Exception On badness
         */
        protected void writeLine(String line) throws Exception {
            output.write(line.getBytes());
        }

        /**
         * Write header and content
         *
         * @param ok Was ok
         * @param content The content to write
         * @param type Type of content
         *
         * @throws Exception On badness
         */
        public void writeResult(boolean ok, String content, String type)
                throws Exception {
            writeResult(ok
                        ? RESPONSE_OK
                        : RESPONSE_NOTFOUND, content, type);
        }


        /**
         * _more_
         *
         * @param code _more_
         * @param content _more_
         * @param type _more_
         *
         * @throws Exception _more_
         */
        public void writeResult(int code, String content, String type)
                throws Exception {
            writeResult(code, content.getBytes(), type);
        }




        /**
         * _more_
         *
         * @param ok _more_
         * @param content _more_
         * @param type _more_
         *
         * @throws Exception _more_
         */
        public void writeResult(boolean ok, StringBuffer content, String type)
                throws Exception {
            writeResult(ok
                        ? RESPONSE_OK
                        : RESPONSE_NOTFOUND, content.toString().getBytes(),
                                             type);
        }



        /**
         * _more_
         *
         * @param code _more_
         * @param content _more_
         * @param type _more_
         *
         * @throws Exception _more_
         */
        public void writeResult(int code, StringBuffer content, String type)
                throws Exception {
            writeResult(code, content.toString().getBytes(), type);
        }



        /**
         * _more_
         *
         * @param content _more_
         *
         * @throws Exception _more_
         */
        public void writeXml(StringBuffer content) throws Exception {
            writeResult(RESPONSE_OK, content.toString().getBytes(),
                        "text/xml");
        }


        /**
         * _more_
         *
         * @param content _more_
         *
         * @throws Exception _more_
         */
        public void writeHtml(StringBuffer content) throws Exception {
            writeResult(RESPONSE_OK, content.toString().getBytes(),
                        "text/html");
        }


        /**
         * _more_
         *
         * @param ok _more_
         * @param content _more_
         * @param type _more_
         *
         * @throws Exception On badness
         */
        public void writeResult(boolean ok, byte[] content, String type)
                throws Exception {

            writeResult(ok
                        ? RESPONSE_OK
                        : RESPONSE_NOTFOUND, content, type);
        }


        /**
         * _more_
         *
         * @param code _more_
         * @param content _more_
         * @param type _more_
         *
         * @throws Exception _more_
         */
        public void writeResult(int code, byte[] content, String type)
                throws Exception {
            try {
                writeHeader(code, content.length, type);
                output.write(content);
                output.close();
            } catch (SocketException se) {}
        }


        /**
         * _more_
         *
         * @param code _more_
         * @param inputStream _more_
         * @param type _more_
         *
         * @throws Exception _more_
         */
        public void writeResult(int code, InputStream inputStream,
                                String type)
                throws Exception {
            writeHeader(code, -1, type);
            IOUtil.writeTo(inputStream, output);
            output.close();
        }



        /**
         * _more_
         *
         * @param code return code
         * @param length _more_
         * @param type _more_
         *
         * @throws Exception On badness
         */
        protected void writeHeader(int code, long length, String type)
                throws Exception {
            if (code == RESPONSE_OK) {
                writeLine("HTTP/1.0 " + code + "  OK" + CRLF);
            } else {
                writeLine("HTTP/1.0 " + code + CRLF);
            }
            if (length >= 0) {
                //See if this fixes the quirky browser problem
                //writeLine("Content-Length: " + length + CRLF);
            }
            writeLine("Content-type: " + type + CRLF);
            writeHeaderArgs();
            writeLine("\n");
        }


        /**
         * _more_
         *
         * @param url _more_
         *
         * @throws Exception _more_
         */
        public void redirect(String url) throws Exception {
            writeLine("HTTP/1.0 300 OK" + CRLF);
            writeLine("Location: " + url + CRLF);
            writeLine("Cache-Control: no-cache" + CRLF);
            writeHeaderArgs();
            writeLine("\n");
            output.close();
        }



        /**
         * _more_
         *
         * @throws Exception _more_
         */
        protected void writeHeaderArgs() throws Exception {
            //            writeLine("Date: Fri, 12 Jan 2007 00:02:44 GMT"+CRLF);
            //            writeLine("Cache-Control: no-cache"+CRLF);
            //            writeLine("Last-Modified: Fri, 12 Jan 2007 00:02:44 GMT"+CRLF);
            //            writeLine("Last-Modified:" + new Date()+CRLF);
        }


        /**
         * Transfer bytes
         *
         * @param fis input stream
         * @param type _more_
         * @param length _more_
         *
         * @throws Exception On badness
         */
        protected void writeBytes(InputStream fis, String type, long length)
                throws Exception {
            writeHeader(RESPONSE_OK, length, type);
            // Construct a 1K buffer to hold bytes on their way to the socket.
            byte[] buffer = new byte[1024];
            int    bytes  = 0;

            // Copy requested file into the socket's output stream.
            while ((bytes = fis.read(buffer)) != -1) {
                output.write(buffer, 0, bytes);
            }
        }

        /**
         * Get the content type
         *
         * @param fileName filename
         *
         * @return Content type
         */
        private static String contentType(String fileName) {
            if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
                return "text/html";
            }
            return "";

        }

    }

    /**
     * test main
     *
     * @param args args
     */
    public static void xxxxmain(String args[]) {
        int port = 80;
        try {
            if (args.length > 0) {
                port = Integer.parseInt(args[0]);
            }
        } catch (Exception e) {}
        (new HttpServer(port)).init();
    }



}

