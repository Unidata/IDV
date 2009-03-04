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

package ucar.unidata.repository.collab;


import org.w3c.dom.*;

import ucar.unidata.repository.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.SocketConnection;


import ucar.unidata.util.StringUtil;
import ucar.unidata.util.WikiUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.io.File;
import java.io.InputStream;


import java.net.*;



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
    public static final OutputType OUTPUT_CHATROOM =
        new OutputType("Chat Room", "chat.room", OutputType.TYPE_HTML, "",
                       ICON_CHAT);

    /** _more_ */
    public static final OutputType OUTPUT_CHAT = new OutputType("Chat",
                                                     "chat.chat",
                                                     OutputType.TYPE_HTML,
                                                     "", ICON_CHAT);

    /** _more_ */
    public static final OutputType OUTPUT_WHITEBOARD =
        new OutputType("Whiteboard", "chat.whiteboard", OutputType.TYPE_HTML,
                       "", ICON_CHAT);


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
        addType(OUTPUT_CHAT);
        addType(OUTPUT_CHATROOM);
        addType(OUTPUT_WHITEBOARD);
        Misc.run(this, "run");
    }


    /** _more_ */
    List<ChatConnection> connections = new ArrayList<ChatConnection>();





    /**
     * Class ChatConnection _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    public class ChatConnection extends SocketConnection {

        /** _more_ */
        public static final String ATTR_TYPE = "type";

        /** _more_ */
        public static final String ATTR_SESSIONID = "sessionid";

        /** _more_ */
        public static final String ATTR_CHANNEL = "channel";

        /** _more_ */
        private String connectionId;

        /** _more_ */
        private String entryId;

        /** _more_ */
        private Entry entry;

        /** _more_ */
        private String session;

        /** _more_ */
        private User user;

        /**
         * _more_
         *
         * @param socket _more_
         *
         * @throws Exception _more_
         */
        public ChatConnection(Socket socket) throws Exception {
            super(socket);
            connectionId = getRepository().getGUID();
        }

        /**
         * _more_
         *
         * @return _more_
         */
        public boolean isValid() {
            return session != null;
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param attrs _more_
         * @param body _more_
         *
         * @throws Exception _more_
         */
        private void writeMessage(String type, String attrs, String body)
                throws Exception {
            String message = message(type, attrs, body);
            //            System.err.println (message);
            write(message);
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param attrs _more_
         * @param body _more_
         *
         * @return _more_
         *
         * @throws Exception _more_
         */
        private String message(String type, String attrs, String body)
                throws Exception {
            StringBuffer sb = new StringBuffer();
            sb.append("<message" + XmlUtil.attr("type", type) + attrs
                      + ">\n");
            sb.append(body);
            sb.append("</message>");
            return sb.toString();
        }

        /**
         * _more_
         *
         * @param msg _more_
         *
         * @throws Exception _more_
         */
        private void writeError(String msg) throws Exception {
            writeMessage("ERROR", "", msg);
        }


        /**
         * _more_
         */
        public void initConnection() {
            super.initConnection();
        }

        /**
         * _more_
         *
         * @param s _more_
         */
        public void handleMessage(String s) {
            try {
                Element node = XmlUtil.getRoot(s);
                String  type = XmlUtil.getAttribute(node, ATTR_TYPE);
                //                System.err.println("handleMessage:" + s);
                if (session == null) {
                    String tmpSession = XmlUtil.getAttribute(node,
                                            ATTR_SESSIONID);
                    SessionManager.Session session =
                        getSessionManager().getSession(tmpSession);
                    if (session == null) {
                        user = getUserManager().getAnonymousUser();
                        //                        writeError("Unknown session:" + tmpSession);
                        //                        return;
                    } else {
                        user = session.getUser();
                    }
                    String tmpEntryId = XmlUtil.getAttribute(node,
                                            ATTR_CHANNEL);
                    entry = getEntryManager().getEntry(null, tmpEntryId);
                    if (entry == null) {
                        //                        entry = getRepository().getEntryManager().getTopGroup();
                    }
                    if (entry == null) {
                        writeError("Could not find entry:" + tmpEntryId);
                        return;
                    }
                    //TODO: Check view access here on the entry
                    this.session = tmpSession;
                    this.entryId = tmpEntryId;
                    writeMessage("STATE",
                                 XmlUtil.attr("username", user.getName())
                                 + XmlUtil.attr("userid", connectionId), "");
                    StringBuffer sb = new StringBuffer();
                    sb.append("<user id=\"" + this.connectionId
                              + "\" name=\"" + this.user.getName()
                              + "\"/>\n");
                    writeExcept(message("USERADD", "", sb.toString()), this,
                                this);
                }
                handleMessage(type, node, s);
            } catch (Exception exc) {
                try {
                    writeError("An error has occurred:" + exc);
                    exc.printStackTrace();
                } catch (Exception ignore) {}
            }
        }

        /**
         * _more_
         *
         * @param type _more_
         * @param node _more_
         * @param msg _more_
         *
         * @throws Exception _more_
         */
        public void handleMessage(String type, Element node, String msg)
                throws Exception {
            //            System.err.println("handleMessage:" + type + " "
            //                               + XmlUtil.toString(node));
            if (type.equals("TEXT")) {
                node.setAttribute("FROM", connectionId);
                writeExcept(XmlUtil.toString(node), this, this);
            } else if (type.equals("PRIVATE")) {
                String to = XmlUtil.getAttribute(node, "to", (String) null);
                ChatConnection dest = findChatConnection(to);
                if (dest != null) {
                    node.setAttribute("FROM", connectionId);
                    dest.write(XmlUtil.toString(node));
                } else {
                    writeError("Could not find private message destination");
                }

            } else if (type.equals("GFX")) {
                node.setAttribute("FROM", connectionId);
                writeExcept(XmlUtil.toString(node), this, this);
            } else if (type.equals("SESSION")) {
                writeMessage("SESSION", XmlUtil.attr("id", connectionId), "");
            } else if (type.equals("FILE")) {
                Request      request = new Request(getRepository(),
                                           this.user);
                StringBuffer sb      = new StringBuffer();
                Group        parent  = (entry.isGroup()
                                        ? (Group) entry
                                        : entry.getParentGroup());
                for (Entry entry : getEntryManager().getChildren(request,
                        parent)) {
                    if (entry.isGroup()) {
                        continue;
                    }
                    String url = null;
                    String entryType;
                    if (entry.getResource().isImage()) {
                        entryType = "image";
                        url = getEntryManager().getEntryResourceUrl(request,
                                entry);
                    } else {
                        entryType = "url";
                        url = request.entryUrl(
                            getRepository().URL_ENTRY_SHOW, entry);
                    }
                    if (url == null) {
                        continue;
                    }

                    writeMessage("FILE",
                                 XmlUtil.attr("name", entry.getName())
                                 + XmlUtil.attr("filetype", entryType)
                                 + XmlUtil.attr("url", url), "");
                }
            } else if (type.equals("USERLIST")) {
                StringBuffer sb = new StringBuffer();
                for (ChatConnection connection : findConnectionsInRoom(
                        this)) {
                    sb.append("<user id=\"" + connection.connectionId
                              + "\" name=\"" + connection.user.getName()
                              + "\"/>\n");
                }
                writeMessage("USERLIST", "", sb.toString());
            }
        }

        /**
         * _more_
         */
        protected void connectionClosed() {
            try {
                StringBuffer sb = new StringBuffer();
                sb.append("<user id=\"" + this.connectionId + "\" name=\""
                          + this.user.getName() + "\"/>\n");
                writeExcept(message("USERREMOVE", "", sb.toString()), this,
                            this);
                removeConnection(this);
            } catch (Exception exc) {}
        }





    }


    /**
     * _more_
     *
     * @param msg _more_
     * @param room _more_
     *
     * @throws Exception _more_
     */
    private void writeAll(String msg, ChatConnection room) throws Exception {
        writeExcept(msg, room, null);
    }

    /**
     * _more_
     *
     * @param room _more_
     *
     * @return _more_
     */
    private List<ChatConnection> findConnectionsInRoom(ChatConnection room) {
        synchronized (connections) {
            List<ChatConnection> inRoom = new ArrayList<ChatConnection>();
            for (ChatConnection connection : connections) {
                if ( !connection.isValid()) {
                    continue;
                }
                if ( !Misc.equals(room.entryId, connection.entryId)) {
                    continue;
                }
                inRoom.add(connection);
            }
            return inRoom;
        }
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private ChatConnection findChatConnection(String id) {
        synchronized (connections) {
            for (ChatConnection connection : connections) {
                if ( !connection.isValid()) {
                    continue;
                }
                if (Misc.equals(connection.connectionId, id)) {
                    return connection;
                }
            }
        }
        return null;
    }


    /**
     * _more_
     *
     * @param msg _more_
     * @param room _more_
     * @param except _more_
     *
     * @throws Exception _more_
     */
    private void writeExcept(String msg, ChatConnection room,
                             ChatConnection except)
            throws Exception {
        synchronized (connections) {
            for (ChatConnection connection : findConnectionsInRoom(room)) {
                if (connection == except) {
                    continue;
                }
                connection.write(msg);
            }
        }
    }

    /**
     * _more_
     *
     * @param connection _more_
     */
    private void removeConnection(ChatConnection connection) {
        synchronized (connections) {
            connections.remove(connection);
        }
    }

    /**
     * _more_
     *
     * @return _more_
     */
    public int getChatPort() {
        return getRepository().getProperty("ramadda.chat.port", 8387);
    }


    /**
     * _more_
     */
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(getChatPort());
            while (true) {
                Socket         socket     = serverSocket.accept();
                ChatConnection connection = new ChatConnection(socket);
                synchronized (connections) {
                    connections.add(connection);
                }
                Misc.run(connection);
            }
        } catch (Exception exc) {
            System.err.println("Error:" + exc);
            exc.printStackTrace();
        }
    }






    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     * @throws Exception _more_
     */
    protected void getEntryLinks(Request request, State state,
                                 List<Link> links)
            throws Exception {

        Entry entry = state.getEntry();
        if (entry != null) {
            if (entry.getResource().isImage()) {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_WHITEBOARD));
            } else if ( !entry.getType().equals("chatroom")) {
                links.add(makeLink(request, state.getEntry(), OUTPUT_CHAT));
            } else {
                links.add(makeLink(request, state.getEntry(),
                                   OUTPUT_CHATROOM));
            }
        }
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {
        return outputEntry(request, group);
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputEntry(Request request, Entry entry) throws Exception {
        String chatAppletTemplate =
            getRepository().getResource(PROP_HTML_CHATAPPLET);
        OutputType output = request.getOutput();

        String     params = "";
        chatAppletTemplate = chatAppletTemplate.replace("${root}",
                getRepository().getUrlBase());
        if (entry.getResource().isImage()) {
            String url = getEntryManager().getEntryResourceUrl(request,
                             entry);
            params += HtmlUtil.open("PARAM",
                                    HtmlUtil.attrs("NAME",
                                        "whiteboard.bgimage", "VALUE", url));

        }
        String session = request.getSessionId();
        if (session == null) {
            session = "";
        }
        chatAppletTemplate = chatAppletTemplate.replace("${port}",
                "" + getChatPort());
        chatAppletTemplate = chatAppletTemplate.replace("${username}",
                request.getUser().getName());
        chatAppletTemplate = chatAppletTemplate.replace("${sessionid}",
                session);
        chatAppletTemplate = chatAppletTemplate.replace("${parameters}",
                params);
        chatAppletTemplate = chatAppletTemplate.replace("${channel}",
                entry.getId());
        return makeLinksResult(request, msg("Chat"),
                               new StringBuffer(chatAppletTemplate),
                               new State(entry));
    }







}

