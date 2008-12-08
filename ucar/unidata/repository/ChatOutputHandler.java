/**
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


import org.w3c.dom.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.SocketConnection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.net.*;
import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class ChatOutputHandler extends OutputHandler {



    /** _more_ */
    public static final OutputType OUTPUT_CHATROOM = new OutputType("Chat Room", "chat.room");


    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public ChatOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_CHATROOM);
        Misc.run(this,"run");
    }


    List<ChatConnection> connections = new ArrayList<ChatConnection>();


    public class ChatConnection extends SocketConnection {
        public static final String ATTR_TYPE = "type";
        public static final String ATTR_SESSIONID = "sessionid";
        public static final String ATTR_CHANNEL = "channel";

        private String connectionId;
        private String entryId;
        private String session;
        private User user;

        public ChatConnection(Socket socket) throws Exception {
            super(socket);
            connectionId = getRepository().getGUID();
        }

        public boolean isValid() {
            return session!=null;
        }

        private void write(String type, String attrs, String body) throws Exception {
            write(message(type,attrs,body));
        }

        private String message(String type, String attrs, String body) throws Exception {
            StringBuffer sb = new StringBuffer();
            sb.append("<message" + XmlUtil.attr("type", type) + attrs +">\n");
            sb.append(body);
            sb.append("</message>");
            return sb.toString();
        }

        private void writeError(String msg) throws Exception  {
            write("ERROR","", msg);
        }


        public void initConnection() {
            super.initConnection();
        }

        public void handleMessage(String s) {
            try {
                Element node = XmlUtil.getRoot(s);
                String type = XmlUtil.getAttribute(node,ATTR_TYPE);
                if(session==null) {
                    String tmpSession = XmlUtil.getAttribute(node,ATTR_SESSIONID);
                    UserManager.Session session = getUserManager().getSession(tmpSession);
                    if(session==null) {
                        user = getUserManager().getAnonymousUser();
                        //                        writeError("Unknown session:" + tmpSession);
                        //                        return;
                    } else {
                        user = session.getUser();
                    }
                    String tmpEntryId = XmlUtil.getAttribute(node,ATTR_CHANNEL);
                    Entry entry = getEntryManager().getEntry(null, tmpEntryId);
                    if(entry == null) {
                        writeError("Could not find entry:" + tmpEntryId);
                        return;
                    }
                    //TODO: Check view access here on the entry
                    this.session = tmpSession;
                    this.entryId = tmpEntryId;
                    write("STATE",XmlUtil.attr("username", user.getName())+XmlUtil.attr("userid", connectionId),"");
                    StringBuffer sb = new StringBuffer();
                    sb.append("<user id=\"" + this.connectionId +"\" name=\"" + this.user.getName()+"\"/>\n");
                    writeExcept(message("USERADD","",sb.toString()),this,this);
                }
                handleMessage(type, node,s);
            } catch(Exception exc) {
                try {
                    writeError("An error has occurred:" + exc);
                } catch(Exception ignore){}
            }
        }

        public void handleMessage(String type, Element node, String msg) throws Exception {
            System.err.println("handleMessage:" + type + " " + XmlUtil.toString(node));
            if(type.equals("TEXT")) {
                node.setAttribute("FROM", connectionId);
                writeExcept(XmlUtil.toString(node),this, this);
            }  else if(type.equals("GFX")) {
                node.setAttribute("FROM", connectionId);
                writeExcept(XmlUtil.toString(node),this,this);
            }  else if(type.equals("SESSION")) {
                write("SESSION",XmlUtil.attr("id", connectionId),"");
            }  else if(type.equals("USERLIST")) {
                StringBuffer sb = new StringBuffer();
                for(ChatConnection connection: findConnectionsInRoom(this)) {
                    sb.append("<user id=\"" + connection.connectionId +"\" name=\"" + connection.user.getName()+"\"/>\n");
                }
                write("USERLIST","",sb.toString());
            }  
        }

        protected void  connectionClosed()  {
            try {
            StringBuffer sb = new StringBuffer();
            sb.append("<user id=\"" + this.connectionId +"\" name=\"" + this.user.getName()+"\"/>\n");
            writeExcept(message("USERREMOVE","",sb.toString()),this,this);
            removeConnection(this);
            } catch(Exception exc) {
            }
        }





    }


    private void writeAll(String msg,ChatConnection room) throws Exception {
        writeExcept(msg,room, null);
    }

    private List<ChatConnection> findConnectionsInRoom(ChatConnection room) {
        synchronized(connections) {
            List<ChatConnection> inRoom = new ArrayList<ChatConnection>();
            for(ChatConnection connection: connections) {
                if(!connection.isValid()) continue;
                if(!Misc.equals(room.entryId,connection.entryId)) continue;
                inRoom.add(connection);
            }
            return inRoom;
        }
    }

    private void writeExcept(String msg, ChatConnection room, ChatConnection except) throws Exception {
        synchronized(connections) {
            for(ChatConnection connection: findConnectionsInRoom(room)) {
                if(connection==except) continue;
                connection.write(msg);
            }
        }
    }

    private void removeConnection(ChatConnection connection) {
        synchronized(connections) {
            connections.remove(connection);
        }
    }


    public void run() {
        try {
            ServerSocket        serverSocket = new ServerSocket(8387);
            while (true) {
                Socket socket = serverSocket.accept();
                ChatConnection connection = new ChatConnection(socket);
                synchronized(connections) {
                    connections.add(connection);
                }
                Misc.run(connection);
            }
        } catch(Exception exc) {
            System.err.println ("Error:" + exc);
            exc.printStackTrace();
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entries _more_
     * @param state _more_
     * @param types _more_
     *
     *
     * @throws Exception _more_
     */
    protected void addOutputTypes(Request request, State state,
                                  List<OutputType> types)
            throws Exception {
        //If its a single entry then punt
        if(state.entry==null) return;
        if(state.entry.getType().equals("chatroom")) {
            types.add(OUTPUT_CHATROOM);
        }
    }


    public Result outputEntry(Request request, Entry entry) throws Exception {
        String chatAppletTemplate =
            getRepository().getResource(PROP_HTML_CHATAPPLET);
        chatAppletTemplate = chatAppletTemplate.replace("${root}",getRepository().getUrlBase());
        //    <PARAM NAME="whiteboard.bgimage"  VALUE="http://localhost:8080/repository/entry/get/image_1_2008_11_15_15_00_00Z.jpg?entryid=cc81eb7f-3ce9-4a2a-a877-603ac7525a8d">

        String session = request.getSessionId();
        if(session==null) session = "";
        chatAppletTemplate = chatAppletTemplate.replace("${username}",request.getUser().getName());
        chatAppletTemplate = chatAppletTemplate.replace("${sessionid}",session);
        chatAppletTemplate = chatAppletTemplate.replace("${parameters}","");
        chatAppletTemplate = chatAppletTemplate.replace("${channel}",entry.getId());
        return new Result("Chat", new StringBuffer(chatAppletTemplate));
    }







}

