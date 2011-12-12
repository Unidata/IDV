/*
 * $Id: CollabManager.java,v 1.30 2006/04/04 21:19:06 jeffmc Exp $
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

package ucar.unidata.idv.collab;



import ucar.unidata.idv.*;

import ucar.unidata.collab.*;

import ucar.unidata.xml.*;

import ucar.visad.display.AnimationWidget;


import ucar.unidata.collab.Client;
import ucar.unidata.collab.Server;

import ucar.unidata.util.FileManager;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.ObjectListener;

import ucar.unidata.data.*;



import java.rmi.RemoteException;

import java.io.*;

import java.net.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import java.awt.*;
import java.awt.event.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;



/**
 * This class manages the IDV collaboration mechanism
 *
 * @author IDV development team
 * @version $Revision: 1.30 $Date: 2006/04/04 21:19:06 $
 */


public class CollabManager extends IdvManager implements SharableListener {


    /**
     *  This holds  a globally unique id for this run of the IDV
     */
    private String collabUniqueId;

    /**
     *  A count of the messages we have sent so we can have a unique id for each message.
     */
    private int uniqueCnt = 0;


    /** The object that handles the event capture/playback */
    private CaptureManager captureManager;

    /** Will identify the hostname preference. Not used at the moment */
    public static String PREF_HOSTNAME = "collab.prop.hostname";

    /** Will identify the port preference. Not used at the moment */
    public static String PREF_PORT = "collab.prop.port";

    /** Will identify the accept all. Not used at the moment */
    public static String PREF_ACCEPTALL = "collab.prop.acceptall";

    /** The de,limiter between the msg id and the body of the message */
    public static final String MSG_DELIMITER = ":";

    /** Message type for new users */
    public static final CollabMsgType MSG_NEWUSER =
        CollabMsgType.createNoRelay("collab.newuser", "Announce a new user");

    /** Message type to acknowledge the acceptance or rejection of a new user */
    public static final CollabMsgType MSG_ACKNEWUSER =
        CollabMsgType.createNoRelay("collab.acknewuser",
                                    "Acknowledge a new user");

    /** Message type to tell others we are closing */
    public static final CollabMsgType MSG_CLOSE =
        CollabMsgType.createNoRelay("collab.close", "Close this connection");

    /** Message type for xml idv bundles */
    public static final CollabMsgType MSG_BUNDLE =
        CollabMsgType.createNoRelay("collab.bundle",
                                    "Load application state");

    /** Message type for announcing new data sources */
    public static final CollabMsgType MSG_DATASOURCE =
        CollabMsgType.createRelay("collab.datasource", "Load a data source");


    /** Message type for announcing new displays */
    public static final CollabMsgType MSG_DISPLAY =
        CollabMsgType.createRelay("collab.display", "Create a display");

    /** Message type for chat text message */
    public static final CollabMsgType MSG_MESSAGE =
        CollabMsgType.createRelay("collab.message", "Chat text");

    /** Message type for when an objects state changes */
    public static final CollabMsgType MSG_STATE =
        CollabMsgType.createRelay("collab.state",
                                  "Change the state of an object");

    /** Message type for when a new window is created */
    public static final CollabMsgType MSG_NEWWINDOW =
        CollabMsgType.createRelay("collab.newwindow", "Create a new window");



    /** Message type for when a datasource is removed */
    public static final CollabMsgType MSG_REMOVEDATASOURCE =
        CollabMsgType.createRelay("collab.remove.datasource",
                                  "Remove a data source");

    /** Message type for when a display is removed */
    public static final CollabMsgType MSG_REMOVEDISPLAYCONTROL =
        CollabMsgType.createRelay("collab.remove.displaycontrol",
                                  "Remove a display control");

    /** Message type for some ui action (not used now) */
    public static final CollabMsgType MSG_ACTION =
        CollabMsgType.createRelay("collab.action", "Do an action");



    /** Used to synchronize */
    private Object MUTEX = new Object();

    /** Text area that holds the chat text */
    private JTextArea chatTextArea;

    /**
     * Has the chat text tab been shown at least once.
     * We track this so we will automatically show the chat
     * tab the first time a chat message comes in.
     */
    private boolean haveShowChatOnce = false;

    /**
     * When a chat message comes in we will show the chat tab red
     * if it is not shown in the tabbed pane. We do this to signal to the
     * user there is a chat message
     */
    private boolean chatTabIsRed = false;

    /** The default color of the chat tab */
    private Color chatTabColor;

    /**
     * Keeps track of how many log messages there are so we can flush them
     * when there gets to be too many
     */
    private int logMsgCnt = 0;

    /** Shows the log messages */
    private JTextArea logTextArea = new JTextArea(10, 30);

    /** The tabbed pane */
    private JTabbedPane tabbedPane;

    /** Tabbed pane index for the connection gui */
    int connectPaneIdx = 0;

    /** Tabbed pane index for the list of clients */
    int clientsPaneIdx = 0;

    /** Tabbed pane index for the chat pane */
    int chatPaneIdx = 0;

    /** Tabbed pane index for the log pane */
    int logPaneIdx = 0;



    /** Button label to use when the server is stopped */
    private static final String LBL_STARTSERVER = "Start server";

    /** Button label to use when the server is running */
    private static final String LBL_STOPSERVER = "Stop server";

    /** Label to use when the server is running */
    private static final String LBL_SERVERISRUNNING =
        "Server status: Server is listening for connections";

    /** Label to use when the server is stopped */
    private static final String LBL_SERVERISSTOPPED =
        "Server status: Server is not listening for connections";

    /** JButton to  start the server */
    private JButton stopStartBtn = new JButton(LBL_STARTSERVER);

    /** The hostname we want to connect to */
    private JTextField hostnameFld = new JTextField(20);


    /**
     * Contains the network port that this server should
     * open a listening socket on
     */
    private JTextField portFld;

    /** Shows the status of the server */
    private JLabel serverStatusLbl = new JLabel(LBL_SERVERISSTOPPED);



    /** Panel that holds the list of clients */
    private JPanel clientsPanel;

    /** Turn on/off the sending of changes */
    private JCheckBox okToSendCbx = new JCheckBox("Ok to send changes", true);

    /** Turn on/off the receipt of other's changes */
    private JCheckBox okToRcvCbx = new JCheckBox("Ok to receive changes",
                                       true);

    /** Should the IDV prompt the user to accept new connections */
    private JCheckBox acceptAllCbx =
        new JCheckBox("Accept new connections without asking");

    /**
     * When this server has other connections should it relay
     * incoming messages to them
     */
    private JCheckBox okToRelayCbx = new JCheckBox("Ok to relay changes",
                                         true);

    /**
     *  The server that listens on a port and manages a list of Client-s
     */
    private CollabServer server;

    /**
     *  The hostname we are initialized to or the last hostname connected to.
     */
    private String hostname;

    /**
     * We keep track of the message ids that we have seen so we don't get into
     * an infinite loop
     */
    private Hashtable seenMessageIds = new Hashtable();

    /**
     * Keep track of how many message ids we have stored in
     * the seenMessageIds map so we can expunge them after a
     * while
     */
    private int seenMessagesCnt = 0;


    /**
     *  The port to use.
     */
    private int port = Server.DEFAULT_PORT;


    /**
     *  Am I in the process of recieving a message from another process.
     */
    private boolean receivingCollabMessage = false;


    /**
     * Create the CollabManager
     *
     * @param idv Reference to the IDV
     */
    public CollabManager(IntegratedDataViewer idv) {
        super(idv);


        boolean doServer = getArgsManager().getDoCollabServer();
        this.hostname  = getArgsManager().getCollabHostName();
        this.port      = getArgsManager().getCollabPort();
        collabUniqueId = Misc.getUniqueId();
        captureManager = new CaptureManager(idv, this);
        if (Misc.equals(hostname, "?")) {
            hostname = GuiUtils.getInput(null, "Hostname to connect to: ",
                                         "");
        }
        //Create the server that listens for new client connections.
        server = new CollabServer(this, port);

        if (hostname != null) {
            if ( !connectTo(hostname)) {
                if (doServer) {
                    LogUtil.userMessage(
                        "The connection to: " + hostname
                        + " failed. Listening for connections.");
                } else {
                    boolean serverOk =
                        GuiUtils.showYesNoDialog(
                            null,
                            "The connect to: " + hostname
                            + " failed. Do you want to listen for connections?", "Connection Failed");
                    if (serverOk) {
                        doServer = true;
                    }
                }

            }
        }

        //Start up the server if we are in doServer mode or if we had tried to connect to another
        //host and we failed.
        if (doServer || ((hostname != null) && !server.hasClients())) {
            startServer();
        }
        SharableManager.addSharableListener(this);
    }




    /**
     *  Try to connect to the class member hostname.
     */
    public void connect() {
        String collabHostName = GuiUtils.getInput(null,
                                                  "Hostname to connect to: ",
                                                  ((hostname != null)
                                                   ? hostname
                                                   : ""));
        if (collabHostName != null) {
            hostname = collabHostName.trim();
            hostnameFld.setText(hostname);
            connectTo(collabHostName);
        }
    }


    /**
     *  Write the current state of the application to the given client.
     * This creates an application bundle which is sent to the given
     * client.
     *
     * @param client The client to write to
     */
    protected void writeState(CollabClient client) {
        try {
            client.write(
                makeMsg(
                    MSG_BUNDLE, getPersistenceManager().getBundleXml(false)));
        } catch (Exception exc) {
            logException("Writing initial state.", exc);
        }
    }


    /**
     * Parse the contents of the portFld JTextField
     *
     * @return The port to use
     */
    private int getPort() {
        if (portFld != null) {
            String s = portFld.getText().trim();
            try {
                port = new Integer(s).intValue();
            } catch (NumberFormatException nfe) {
                LogUtil.userMessage("Invalid format:" + s);
            }
        }
        return port;
    }

    /**
     * Try to connect to the given host
     *
     * @param collabHostName The host to connect to
     * @return Was the connection successful
     */
    protected boolean connectTo(String collabHostName) {
        try {
            CollabClient tmpClient = new CollabClient(this, collabHostName,
                                                      getPort());
            tmpClient.setIsLocal(true);
            if (tmpClient.isConnectionOk()) {
                //Announce me to the other IDV
                tmpClient.setValid(true);
                tmpClient.write(makeMsg(MSG_NEWUSER, ""));
                //                tmpClient.initListening ();
                server.addClient(tmpClient);
                return true;
            }
        } catch (Exception exc) {
            logException("Creating client", exc);
        }
        return false;
    }



    /**
     *  The implementation of the SharableListener interface. This gets called when
     * the state of a Sharable object (e.g., a DisplayControl, a ViewManager)  is changed.
     *
     * @param from The object that changed
     * @param dataId An identifier (typically a String name) of what changed
     * @param data Array of things that changed
     */
    public void checkShareData(Sharable from, Object dataId, Object[] data) {
        //TODO        synchronized (MUTEX) {
        if ( !isEnabled() || (from == null) || receivingCollabMessage) {
            return;
        }
        //            if (from instanceof MapViewManager) {
        //                if (!from.getSharing()) return;
        //            }
        XmlEncoder encoder = getIdv().getEncoderForWrite();
        populateEncoderWithDataSources(encoder);
        String contents = from.getUniqueId() + MSG_DELIMITER + dataId
                          + MSG_DELIMITER + encoder.toXml(data);
        write(MSG_STATE, contents);
        //TODO        }
    }



    /**
     *  Is it ok for the server to relay messages that have been sent
     * by another client to any other clients we may be connected to.
     *
     * @return OK to relay messages
     */
    public boolean getOkToRelay() {
        return getOkToReceive() && okToRelayCbx.isSelected();
    }

    /**
     *  Is it ok to send changes from the local idv to other clients.
     *
     * @return OK to send changes
     */
    private boolean getOkToSend() {
        return okToSendCbx.isSelected();
    }

    /**
     *  Is it ok to receive changes from other clients.
     *
     * @return Ok to rcv changes
     */
    private boolean getOkToReceive() {
        return okToRcvCbx.isSelected();
    }

    /**
     * Are we doing an event capture and/or do we have any connected clients
     *
     * @return Are we enabled to handle events
     */
    protected boolean isEnabled() {
        return (captureManager.doingCapture() || haveClients());
    }


    /**
     * Are there any clients we are connected to
     *
     * @return Have any clients
     */
    protected boolean haveClients() {
        return ((server != null) && server.hasClients());
    }

    /**
     * Show the connect pane
     */
    public void showConnect() {
        showWindow();
        tabbedPane.setSelectedIndex(connectPaneIdx);
    }

    /**
     * Show the clients pane
     */
    public void showClients() {
        showWindow();
        tabbedPane.setSelectedIndex(clientsPaneIdx);
    }


    /**
     * Show the chat pane
     */
    public void showChat() {
        showWindow();
        tabbedPane.setSelectedIndex(chatPaneIdx);
    }

    /**
     * Show the log pane
     */
    public void showLog() {
        showWindow();
        tabbedPane.setSelectedIndex(logPaneIdx);
    }


    /**
     * Bring up the event capture window
     */
    public void showCapture() {
        captureManager.showCaptureWindow();
    }

    /**
     * Execute the events in the given capture file
     *
     * @param filename Filename of the capture file
     */
    public void runCaptureFile(String filename) {
        captureManager.runCaptureFile(filename);
    }

    /**
     * Close down all connections
     */
    public void disconnectAll() {
        List clients = server.getClients();
        for (int i = 0; i < clients.size(); i++) {
            disconnect((CollabClient) clients.get(i));
        }
    }


    /**
     * Close down the connection to the given client
     *
     * @param client The client to close
     */
    private void disconnect(CollabClient client) {
        try {
            client.write(makeMsg(MSG_CLOSE, ""));
            client.close();
        } catch (Exception exc) {}
    }

    /**
     * Add the GUI elements into the client panel
     */
    private void fillClientPanel() {
        if (clientsPanel == null) {
            clientsPanel = new JPanel();
            clientsPanel.setLayout(new BorderLayout());
            clientsPanel.setPreferredSize(new Dimension(100, 100));
        } else {
            clientsPanel.removeAll();
        }
        List clients = server.getClients();
        List comps   = new ArrayList();
        for (int i = 0; i < clients.size(); i++) {
            CollabClient client        = (CollabClient) clients.get(i);
            JButton      disconnectBtn = new JButton("Disconnect");
            disconnectBtn.addActionListener(new ObjectListener(client) {
                public void actionPerformed(ActionEvent ae) {
                    disconnect((CollabClient) theObject);
                }
            });
            comps.add(disconnectBtn);

            JCheckBox okToSendCbx = new JCheckBox("Ok to send",
                                                  client.getOkToSend());
            okToSendCbx.addActionListener(new ObjectListener(client) {
                public void actionPerformed(ActionEvent ae) {
                    Client theClient = (Client) theObject;
                    theClient.setOkToSend( !theClient.getOkToSend());
                }
            });
            comps.add(okToSendCbx);

            JCheckBox okToReceiveCbx = new JCheckBox("Ok to receive",
                                                     client.getOkToReceive());
            okToReceiveCbx.addActionListener(new ObjectListener(client) {
                public void actionPerformed(ActionEvent ae) {
                    Client theClient = (Client) theObject;
                    theClient.setOkToReceive( !theClient.getOkToReceive());
                }
            });
            comps.add(okToReceiveCbx);

            comps.add(new JLabel("   " + client.getName() + " on:" + client));
        }
        JPanel inner;
        if (clients.size() == 0) {
            inner = GuiUtils.top(new JLabel("No clients"));
        } else {
            inner = GuiUtils.doLayout(comps, 4, GuiUtils.WT_NNNY,
                                      GuiUtils.WT_N);
        }

        JScrollPane sp =
            new JScrollPane(
                GuiUtils.top(inner),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        clientsPanel.add("North", new JLabel("Clients"));
        clientsPanel.add("Center", sp);
        clientsPanel.validate();
        clientsPanel.repaint();
    }

    /**
     * We have a new client
     *
     * @param client The new client
     */
    protected void clientAdded(CollabClient client) {
        writeLog("New client:" + client);
        if ( !client.getIsLocal()) {
            if (client.getValid()) {
                writeState(client);
            }
        }
    }

    /**
     * Client is goen
     *
     * @param client The goner client
     */
    protected void clientRemoved(CollabClient client) {
        writeLog("Client gone:" + client);
        fillClientPanel();
    }

    /**
     * The server has stopped. Update the GUI labels, etc.
     */
    protected void serverStopped() {
        writeLog("Server stopped");
        setServerStateLabels();
    }

    /**
     * The server has started. Update the GUI labels, etc.
     */
    protected void serverStarted() {
        writeLog("Server started");
        setServerStateLabels();
    }

    /**
     *  Update the GUI labels, etc.
     */
    private void setServerStateLabels() {
        if (server.getIsRunning()) {
            stopStartBtn.setText(LBL_STOPSERVER);
            serverStatusLbl.setText(LBL_SERVERISRUNNING);
        } else {
            stopStartBtn.setText(LBL_STARTSERVER);
            serverStatusLbl.setText(LBL_SERVERISSTOPPED);
        }
    }




    /**
     * Create the GUI if needed and popup the window.
     */
    public void showWindow() {
        super.show();
    }

    /**
     * Humm, I wonder what this means. Perhaps start up the listening
     * server?
     */
    private void startServer() {
        try {
            server.startServer(getPort());
        } catch (java.io.IOException ioe) {
            logException("Starting the collaboration server", ioe);
        }
    }

    /**
     * Make the GUI
     *
     * @return The gui
     */
    protected JComponent doMakeContents() {

        chatTextArea = new JTextArea(10, 30) {
            public void paint(Graphics g) {
                if (chatTabIsRed) {
                    chatTabIsRed = false;
                    tabbedPane.setBackgroundAt(chatPaneIdx, chatTabColor);
                }
                super.paint(g);
            }
        };
        chatTextArea.setEditable(false);

        final JTextField inputFld     = new JTextField(30);
        ActionListener   sendListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String msg = inputFld.getText();
                sendText(msg);
                inputFld.setText("");
                writeChatText(getUserName(), msg);
                inputFld.requestFocus();
            }
        };

        JButton sendBtn = new JButton("Send");
        sendBtn.addActionListener(sendListener);
        inputFld.addActionListener(sendListener);
        JScrollPane sp =
            new JScrollPane(
                chatTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                chatTextArea.setEditable(true);
                chatTextArea.setText("");
                chatTextArea.setEditable(false);
            }
        });

        JPanel bottom = GuiUtils.inset(
                            GuiUtils.leftCenterRight(
                                GuiUtils.rLabel(
                                    "Input:  "), inputFld, sendBtn), 4);
        JPanel chatContents =
            GuiUtils.topCenterBottom(GuiUtils.left(clearBtn), sp, bottom);



        setServerStateLabels();
        portFld = new JTextField("" + port, 5);



        stopStartBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (server.getIsRunning()) {
                    server.stopServer();
                } else {
                    startServer();
                }
                setServerStateLabels();
            }
        });

        fillClientPanel();

        ActionListener connectListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                String hostname = hostnameFld.getText().trim();
                if ( !connectTo(hostname)) {
                    LogUtil.userMessage("The connection to: " + hostname
                                        + " failed.");
                }
            }
        };

        if (hostname != null) {
            hostnameFld.setText(hostname);
        }
        JButton connectBtn = new JButton("Connect to:");
        connectBtn.addActionListener(connectListener);
        hostnameFld.addActionListener(connectListener);
        JPanel connectPanel =
            GuiUtils.left(GuiUtils.hflow(Misc.newList(connectBtn,
                hostnameFld, new JLabel(" (Enter hostname)"))));


        JButton writeStateBtn = new JButton("Write current state");
        writeStateBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                writeState();
            }
        });




        JPanel startServerPanel = GuiUtils.left(
                                      GuiUtils.hflow(
                                          Misc.newList(
                                              GuiUtils.rLabel("Port: "),
                                              portFld, stopStartBtn,
                                              acceptAllCbx)));
        JPanel serverPanel = GuiUtils.left(GuiUtils.doLayout(new Component[]{
                                 startServerPanel,
                                 serverStatusLbl }, 1, GuiUtils.WT_N,
                                                    GuiUtils.WT_N));
        serverPanel =
            GuiUtils.topCenter(
                new JLabel("Or you can start your own server so others can connect to your IDV: "),
                serverPanel);

        JPanel connectContents =
            GuiUtils.top(
                GuiUtils.vbox(
                    new JLabel("You can either connect to another IDV server: "),
                    connectPanel,
                    GuiUtils.vbox(new JLabel(" "), new JLabel(" ")),
                    serverPanel));


        JPanel clientsWidgets =
            GuiUtils.left(GuiUtils.doLayout(new Component[]{ okToSendCbx,
                                                             okToRcvCbx,
                                                             okToRelayCbx,
                                                             writeStateBtn }, 1,
                                                                 GuiUtils.WT_N,
                                                                 GuiUtils
                                                                     .WT_N));
        JPanel clientsContents = GuiUtils.topCenter(clientsWidgets,
                                                    clientsPanel);


        logTextArea.setEditable(false);
        JScrollPane logSp =
            new JScrollPane(
                logTextArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        JButton clearLogBtn = new JButton("Clear");
        clearLogBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                clearLog();
            }
        });
        JPanel logContents =
            GuiUtils.topCenterBottom(GuiUtils.cLabel("Collaboration log"),
                                     logSp, GuiUtils.wrap(clearLogBtn));




        tabbedPane = new JTabbedPane();
        int panePaneIdx = 0;
        tabbedPane.add("Connect", GuiUtils.inset(connectContents, 4));
        connectPaneIdx = panePaneIdx++;
        tabbedPane.add("Clients", GuiUtils.inset(clientsContents, 4));
        clientsPaneIdx = panePaneIdx++;
        tabbedPane.add("Chat", GuiUtils.inset(chatContents, 4));
        chatPaneIdx = panePaneIdx++;
        tabbedPane.add("Log", GuiUtils.inset(logContents, 4));
        logPaneIdx   = panePaneIdx++;
        chatTabColor = tabbedPane.getBackgroundAt(chatPaneIdx);


        JButton helpBtn = new JButton("Help");
        helpBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                showHelp();
            }
        });
        JPanel buttons = GuiUtils.center(GuiUtils.wrap(helpBtn));


        contents = GuiUtils.centerBottom(tabbedPane, buttons);
        return contents;
    }

    /**
     * Get the window title to use
     *
     * @return The window title
     */
    public String getWindowTitle() {
        return "Collaboration Window";
    }


    /**
     * Show the colalboration help
     */
    private void showHelp() {
        getIdvUIManager().showHelp("idv.collab");
    }

    /**
     * Clear the  text in the log text area
     */
    private void clearLog() {
        logTextArea.setEditable(true);
        logTextArea.setText("");
        logTextArea.setEditable(false);
        logMsgCnt = 0;

    }




    /**
     * Send the MSG_MESSAGE text message (the chat text)
     *
     * @param msg The message to send
     */
    public void sendText(String msg) {
        write(MSG_MESSAGE, msg);
    }

    /**
     * Send the MSG_REMOVEDATASOURCE message for the given data source
     *
     * @param dataSource The data source that was removed
     */
    public void writeRemoveDataSource(DataSource dataSource) {
        try {
            write(MSG_REMOVEDATASOURCE, dataSource.getUniqueId());
        } catch (Exception exc) {
            logException("Sending state", exc);
        }

    }



    /**
     * Send the MSG_REMOVEDISPLAYCONTROL message for the given display control
     *
     * @param display The display control that was removed
     */
    public void writeRemoveDisplayControl(DisplayControl display) {
        try {
            write(MSG_REMOVEDISPLAYCONTROL, display.getUniqueId());
        } catch (Exception exc) {
            logException("Sending state", exc);
        }

    }



    /**
     * Send the MSG_BUNDLE message. This creates the application state bundle
     * and sends it out to all clients.
     */
    public void writeState() {
        try {
            write(MSG_BUNDLE, getPersistenceManager().getBundleXml(false));
        } catch (Exception exc) {
            logException("Sending state", exc);
        }
    }


    /**
     * Send the MSG_NEWWINDOW message.
     *
     * @param vm The new ViewManager
     */
    public void newWindow(ViewManager vm) {
        String vmXml = getIdv().encodeObject(vm, true);
        write(MSG_NEWWINDOW, vmXml);

    }


    /**
     * Write  to all clients the given message of the
     * given message type.
     *
     * @param type The message type
     * @param message The message
     */
    public void write(CollabMsgType type, String message) {
        if (type.getBlocked()) {
            return;
        }
        //TODO        synchronized (MUTEX) {
        if ( !getOkToSend()) {
            return;
        }
        if ( !isEnabled() || receivingCollabMessage) {
            return;
        }
        String msg = makeMsg(type, message);
        if (captureManager.doingCapture()) {
            captureManager.addEvent(new CaptureEvent(msg));
        }
        if (haveClients()) {
            server.write(msg);
        }
        //TODO        }
    }


    /**
     * Get our user's name
     *
     * @return User's name
     */
    protected String getUserName() {
        return getStateManager().getUserName();
    }

    /**
     * Construct the message. The format is:
     * <pre>
     * UNIQUEID:MESSAGETYPE:USERNAME:MESSAGEBODY
     * </pre>
     *
     * @param type The type of the message
     * @param body1 The message text
     * @return The constructed message
     */
    protected String makeMsg(CollabMsgType type, String body1) {
        String msgId = collabUniqueId + "_" + (uniqueCnt++);
        haveISeenThisMessage(msgId);
        return msgId + MSG_DELIMITER + type + MSG_DELIMITER + getUserName()
               + MSG_DELIMITER + body1;
    }


    /**
     *  Process the given message from the given client.
     *
     *  @param from The {@link CollabClient} this message is from . This
     * may be the replayClient
     *  @param msg the message
     */
    protected void handleMessage(CollabClient from, String msg) {
        handleMessage(from, msg, true);
    }




    /**
     * Has the message with the given id already been processed
     *
     * @param id The id to check
     * @return Have we seen this before
     */
    private boolean haveISeenThisMessage(String id) {
        if (seenMessageIds.get(id) != null) {
            return true;
        }
        //TODO: We shouldn't dump all of the message ids, just the oldest 500 or so
        if (seenMessagesCnt > 1000) {
            seenMessageIds = new Hashtable();
        }
        seenMessagesCnt++;
        seenMessageIds.put(id, id);
        return false;
    }

    /**
     *  Process the given message from the given client. If checkIfMessageHasBeenSeen
     *  is true then if we have seen this message before we ignore it.
     *
     *  @param from The {@link CollabClient} this message is from . This may be the replayClient
     *  @param msg the message
     *  @param checkIfMessageHasBeenSeen Only process unique messages
     */

    protected void handleMessage(CollabClient from, String msg,
                                 boolean checkIfMessageHasBeenSeen) {
        //TODO        synchronized (MUTEX) {
        receivingCollabMessage = true;
        try {
            handleMessageInner(from, msg, checkIfMessageHasBeenSeen);
        } catch (Exception exc) {
            logException("Handling collab message", exc);
        }
        receivingCollabMessage = false;
        //TODO        }
    }


    /**
     * Handle the incoming message.
     *
     * @param fromClient Who sent it
     * @param msg The message
     * @param checkIfMessageHasBeenSeen Should we  check if we have seen this.
     * Normal collaboration message we always check. This flag is there
     * so the {@link CaptureManager} can replay messages even though we may
     * have already seen them.
     *
     * @throws Exception
     */
    protected void handleMessageInner(CollabClient fromClient, String msg, boolean checkIfMessageHasBeenSeen)
            throws Exception {
        int idx = msg.indexOf(MSG_DELIMITER);
        if (idx < 0) {
            return;
        }
        //Messages are of the form:
        //<source CollabManager uniqueId>:<msgTypeName>:<from user name>:<contents>

        String[] msgArray = StringUtil.split(msg, MSG_DELIMITER, 4);
        if (msgArray == null) {
            return;
        }

        //If the uniqueId is ours then we were the original creator of the message and we 
        //sent it to some client that relayed back to us (If they have more than one client 
        //connections to this server.


        String fromUniqueId = msgArray[0];
        if (checkIfMessageHasBeenSeen) {
            if (haveISeenThisMessage(fromUniqueId)) {
                return;
            }
        }



        CollabMsgType msgType = CollabMsgType.find(msgArray[1]);
        if (msgType == null) {
            writeLog("Unknown message type:" + msgArray[1]);
            return;
        }
        if (msgType.getBlocked()) {
            return;
        }

        //If  we are not accepting messages and the message is *not*
        //a chat message then quit
        if ( !getOkToReceive() && !msgType.equals(MSG_MESSAGE)) {
            return;
        }
        String from     = msgArray[2];
        String contents = msgArray[3];

        try {
            handleMessageInnerInner(fromClient, msgType, from, contents);
        } catch (Exception exc) {
            logException("Handling collab message: " + msgType, exc);
            return;
        }

        if (msgType.getShouldRelay() && getOkToRelay()) {
            server.write(msg, fromClient);
        }

    }

    /**
     * Really handle the incoming message
     *
     * @param fromClient From where
     * @param msgType Type
     * @param from From whom
     * @param contents Message body
     *
     * @throws Exception
     */
    protected void handleMessageInnerInner(CollabClient fromClient, CollabMsgType msgType, String from, String contents)
            throws Exception {

        String logMsg = null;
        if (msgType.equals(MSG_CLOSE)) {
            fromClient.close();
            return;
        }

        if (msgType.equals(MSG_NEWUSER)) {
            if ( !acceptAllCbx.isSelected()) {
                JLabel label1 = new JLabel("User: " + from + " on machine: "
                                           + fromClient.toString()
                                           + " is requesting a connection.");
                JLabel label2 = new JLabel("Do you want to connect?");
                JPanel dialogContents = GuiUtils.inset(
                                            GuiUtils.vbox(
                                                GuiUtils.left(
                                                    label1), GuiUtils.left(
                                                    label2)), 4);
                boolean ok = GuiUtils.showYesNoDialog(null,
                                                      "Connection Request",
                                                      dialogContents, null);
                if ( !ok) {
                    fromClient.write(makeMsg(MSG_ACKNEWUSER, "no"));
                    fromClient.close();
                    return;
                }
            }
            fromClient.setValid(true);
            fromClient.setName(from);
            fromClient.write(makeMsg(MSG_ACKNEWUSER, "ok"));
            writeState(fromClient);
            fillClientPanel();
            return;
        }

        if ( !fromClient.getValid()) {
            return;
        }

        if (msgType.equals(MSG_ACKNEWUSER)) {
            if ( !contents.equals("ok")) {
                LogUtil.userMessage("The connection was rejected");
                fromClient.close();
                return;
            }
            fromClient.setName(from);
            fillClientPanel();
        } else if (msgType.equals(MSG_DATASOURCE)
                   || msgType.equals(MSG_DISPLAY)) {
            XmlEncoder encoder = getIdv().getEncoderForRead();
            populateEncoderWithDataSources(encoder);
            Object object = encoder.toObject(contents);
            if (object == null) {
                return;
            }
            if (object instanceof DataSource) {
                getDataManager().addDataSource((DataSource) object);
                logMsg = "Received new data source  from " + from;
            } else if (object instanceof DisplayControl) {
                DisplayControl displayControl = (DisplayControl) object;
                //If we don't have this one already then add it in.
                if ( !haveDisplayControl(displayControl)) {
                    displayControl.initAfterUnPersistence(getIdv(),
                                                          new Hashtable());
                    logMsg = "Received new display  from " + from;
                }
            } else {
                System.err.println("Unknown object:"
                                   + object.getClass().getName());
            }
        } else if (msgType.equals(MSG_NEWWINDOW)) {
            ViewManager newViewManager =
                (ViewManager) getIdv().decodeObject(contents);
            try {
                newViewManager.initAfterUnPersistence(getIdv());
            } catch (Exception exc) {
                logException("Unpersisting VM", exc);
            }
            getIdvUIManager().createNewWindow(Misc.newList(newViewManager),
                                              false);
        } else if (msgType.equals(MSG_STATE)) {
            String[] stateArray = StringUtil.split(contents, MSG_DELIMITER,
                                                   3);
            if (stateArray == null) {
                return;
            }
            String     sharableId = stateArray[0];
            String     dataId     = stateArray[1];
            String     xml        = stateArray[2];
            XmlEncoder encoder    = getIdv().getEncoderForRead();
            populateEncoderWithDataSources(encoder);
            Object[] data = (Object[]) encoder.toObject(xml);
            if (data == null) {
                return;
            }
            Sharable sharable = findSharable(sharableId);
            if (sharable != null) {
                logMsg = "Received state change from " + from + " for "
                         + sharable.getClass().getName();
                sharable.receiveShareData(null, dataId, data);
            } else {
                logMsg = "Unknown state change recipient: " + stateArray[2];
            }
        } else if (msgType.equals(MSG_REMOVEDATASOURCE)) {
            String   sharableId = contents;
            Sharable sharable   = findSharable(sharableId);
            if (sharable == null) {
                logMsg = "Unknown data source: " + sharableId;
            } else {
                MSG_REMOVEDATASOURCE.setBlocked(true);
                getIdv().removeDataSource((DataSource) sharable);
                MSG_REMOVEDATASOURCE.setBlocked(false);
            }
        } else if (msgType.equals(MSG_REMOVEDISPLAYCONTROL)) {
            String   sharableId = contents;
            Sharable sharable   = findSharable(sharableId);
            if (sharable == null) {
                logMsg = "Unknown display control: " + sharableId;
            } else {
                MSG_REMOVEDISPLAYCONTROL.setBlocked(true);
                ((DisplayControl) sharable).doRemove();
                MSG_REMOVEDISPLAYCONTROL.setBlocked(false);
            }

        } else if (msgType.equals(MSG_BUNDLE)) {
            try {
                getPersistenceManager().decodeXml(contents, true, null,
                                                  false);
                logMsg = "Received bundle from " + from;
            } catch (Throwable exc) {
                logException("Unable to evaluate bundle", exc);
            }
        } else if (msgType.equals(MSG_MESSAGE)) {
            //            java.awt.Toolkit.getDefaultToolkit ().beep();
            if (haveShowChatOnce) {
                showWindow();
            } else {
                showChat();
            }
            haveShowChatOnce = true;
            writeChatText(from, contents);
            if ((tabbedPane.getSelectedIndex() != chatPaneIdx)
                    && !chatTabIsRed) {
                chatTabIsRed = true;
                tabbedPane.setBackgroundAt(chatPaneIdx, Color.red);
            }
        } else if (msgType.equals(MSG_ACTION)) {
            //            handleSingleAction (contents);
        } else {
            System.err.println("Unknown message type:" + msgType);
        }

        writeLog(logMsg);
    }




    /**
     * Add the given chat text to the text area
     *
     * @param from From whom
     * @param msg The text
     */
    protected void writeChatText(String from, String msg) {
        chatTextArea.setEditable(true);
        chatTextArea.append(from + ":" + msg + "\n");
        chatTextArea.setText(chatTextArea.getText());
        chatTextArea.setEditable(false);
    }

    /**
     * Is this an existing  {@link ucar.unidata.idv.DisplayControl}
     *
     * @param displayControl The display control to check
     * @return Does the display control exist in the app
     */
    public boolean haveDisplayControl(DisplayControl displayControl) {
        String uniqueId        = displayControl.getUniqueId();
        List   displayControls = getIdv().getDisplayControls();
        for (int i = 0; i < displayControls.size(); i++) {
            if (Misc.equals(
                    uniqueId,
                    ((DisplayControl) displayControls.get(
                        i)).getUniqueId())) {
                return true;
            }
        }
        return false;
    }


    /**
     * Find the {@link ucar.unidata.collab.Sharable} object
     * identified by the given id in the list of sharables
     *
     * @param id Id to search for
     * @param sharables The haystack
     * @return The found Sharable or null if not found
     */
    protected Sharable findSharable(String id, List sharables) {
        for (int i = 0; i < sharables.size(); i++) {
            Sharable sharable = (Sharable) sharables.get(i);
            if (Misc.equals(sharable.getUniqueId(), id)) {
                return sharable;
            }
        }
        return null;

    }


    /**
     *  Find the {@link ucar.unidata.collab.Sharable} object
     *  identified by the given id. This looks in the
     *  list of display controls, the data sources, the view managers
     *  and the animation widgets. (Which are the different classes of
     *  of objects that we share state for.
     *
     *  @param id Id to search for
     *  @return The found Sharable or null if not found
     */
    protected Sharable findSharable(String id) {
        Sharable sharable;

        sharable = findSharable(id, getIdv().getDisplayControls());
        if (sharable != null) {
            return sharable;
        }

        sharable = findSharable(id, getIdv().getDataSources());
        if (sharable != null) {
            return sharable;
        }

        List viewManagers = getVMManager().getViewManagers();

        for (int i = 0; i < viewManagers.size(); i++) {
            ViewManager vm = (ViewManager) viewManagers.get(i);
            if (Misc.equals(vm.getUniqueId(), id)) {
                return vm;
            }
            AnimationWidget animationWidget = vm.getAnimationWidget();
            if ((animationWidget != null)
                    && Misc.equals(animationWidget.getUniqueId(), id)) {
                return animationWidget;
            }
        }
        return null;
    }




    /**
     * Write the text to the log text area. CLear it if we
     * have more than 100 log message.
     *
     * @param logMsg The message
     */
    private void writeLog(String logMsg) {
        if (logMsg == null) {
            return;
        }
        if (logMsgCnt++ > 100) {
            clearLog();
        }
        logTextArea.setEditable(true);
        logTextArea.append(logMsg + "\n");
        logTextArea.setEditable(false);
    }


    /**
     * This gets called when the given {@link ucar.unidata.idv.DisplayControl}
     * has been initialized. When we get this, if we are enabled, we send
     * the MSG_DISPLAY message with the xml encoding of the display.
     *
     * @param control The display control that has been initialized
     */
    public void controlHasBeenInitialized(DisplayControl control) {
        if ((control != null) && isEnabled()) {
            XmlEncoder encoder = getIdv().getEncoderForWrite();
            populateEncoderWithDataSources(encoder);
            String displayXml = XmlUtil.toString(encoder.toElement(control));
            write(MSG_DISPLAY, displayXml);
        }
    }


    /**
     * This prepopulates the given encoder with all of the current
     * data sources. That way, when we encode a DisplayControl, that
     * may have references to the data sources, we don't also
     * encode the data sources
     *
     * @param encoder The encoder
     * @return Just return the encoder
     */
    private XmlEncoder populateEncoderWithDataSources(XmlEncoder encoder) {
        List currentSources = getIdv().getDataSources();
        for (int i = 0; i < currentSources.size(); i++) {
            DataSource source = (DataSource) currentSources.get(i);
            encoder.defineObjectId(source, source.getUniqueId());
        }
        return encoder;

    }




}








