/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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

package ucar.unidata.idv.chooser.adde;


import edu.wisc.ssec.mcidas.*;
import edu.wisc.ssec.mcidas.adde.*;

import org.w3c.dom.Element;


import ucar.unidata.data.imagery.AddeImageDescriptor;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.*;

import ucar.unidata.gis.mcidasmap.McidasMap;

import ucar.unidata.idv.chooser.IdvChooser;
import ucar.unidata.idv.chooser.IdvChooserManager;
import ucar.unidata.idv.chooser.TimesChooser;

import ucar.unidata.metdata.Station;

import ucar.unidata.ui.ChooserList;
import ucar.unidata.ui.ChooserPanel;

import ucar.unidata.util.DateSelection;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.DatedObject;
import ucar.unidata.util.DatedThing;

import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.PreferenceList;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.view.station.StationLocationMap;
import ucar.unidata.view.station.StationLocationRenderer;

import ucar.unidata.xml.XmlResourceCollection;

import visad.CommonUnit;

import visad.DateTime;

import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.io.InputStream;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;

import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.*;
import javax.swing.border.*;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;



/**
 *
 * @author Unidata IDV Development Team
 * @version $Revision: 1.8 $
 */
public class AddeChooser extends TimesChooser {

    /** server state preference. Holds the last server/group used */
    private static final String PREF_SERVERSTATE =
        "idv.chooser.adde.serverstate";

    /** Do we remove or mark as inactive the AddeServers and Groups */
    private static final boolean MARK_AS_INACTIVE = true;


    /** My servers */
    private List addeServers;

    /** flag for relative times range */
    private static final int TIMES_RELATIVERANGE = 0;

    /** flag for absolute times */
    private static final int TIMES_ABSOLUTE = 1;

    /** flag for relative times */
    private static final int TIMES_RELATIVE = 2;


    /** Command for connecting */
    protected static final String CMD_CONNECT = "cmd.connect";


    /** Property for the PUBLIC.SRV file */
    protected static final String FILE_PUBLICSRV = "PUBLIC.SRV";

    /** ADDE request string for pointdata */
    protected static final String REQ_POINTDATA = "pointdata";

    /** ADDE request string for an image directory */
    protected static final String REQ_IMAGEDIR = "imagedir";

    /** ADDE request string for text */
    protected static final String REQ_TEXT = "text";

    /** ADDE request string for dataset information */
    protected static final String REQ_DATASETINFO = "datasetinfo";

    /** ADDE request string for image data */
    protected static final String REQ_IMAGEDATA = "imagedata";

    /** Default value for the compress property */
    protected static final String DEFAULT_COMPRESS = "gzip";

    /** Default value for the port property */
    protected static final String DEFAULT_PORT = "112";

    /** Default value for the debug property */
    protected static final String DEFAULT_DEBUG = "false";

    /** Default value for the version property */
    protected static final String DEFAULT_VERSION = "1";

    /** Default value for the user property */
    protected static final String DEFAULT_USER = "idv";

    /** Default value for the proj property */
    protected static final String DEFAULT_PROJ = "0";

    /** Property for accessing a file */
    protected static final String PROP_FILE = "file";

    /** Property for image compress */
    protected static final String PROP_COMPRESS = "compress";

    /** Property for image default value descriptor */
    protected static final String PROP_DEBUG = "debug";

    /** Property for image default value descriptor */
    protected static final String PROP_DESCR = "descr";

    /** Property for group */
    protected static final String PROP_GROUP = "group";

    /** Property for num param */
    protected static final String PROP_NUM = "num";

    /** Property for image default value param */
    protected static final String PROP_PARAM = "param";

    /** Property for the port */
    protected static final String PROP_PORT = "port";

    /** property for the ADDE port */
    public static final String PROP_ADDEPORT = "adde.port";

    /** Property for the  POS  property */
    protected static final String PROP_POS = "pos";

    /** Property for the  DAY  property */
    protected static final String PROP_DAY = "DAY";

    /** Property for the  TIME  property */
    protected static final String PROP_TIME = "TIME";

    /** Property for the  HMS  property */
    protected static final String PROP_HMS = "HMS";

    /** Property for the  CYD  property */
    protected static final String PROP_CYD = "CYD";

    /** Property for the project */
    protected static final String PROP_PROJ = "proj";

    /** Property for select */
    protected static final String PROP_SELECT = "select";

    /** Property for the user */
    protected static final String PROP_USER = "user";

    /** Property for image default value version */
    protected static final String PROP_VERSION = "version";

    /** Message for selecting times */
    protected static final String MSG_TIMES =
        "Please select one or more times";


    /** Label for data interval */
    protected static final String LABEL_DATAINTERVAL = "Data Interval:";

    /** Label for data set */
    protected static final String LABEL_DATASET = "Dataset:";


    /** Label for data type */
    protected static final String LABEL_DATATYPE = "Data Type:";

    /** Label for server widget */
    protected static final String LABEL_SERVER = "Server:";

    /** Label for stations widget */
    protected static final String LABEL_STATIONS = "Stations:";

    /** Label for times */
    protected static final String LABEL_TIMES = "Times:";


    /** Property for new data selection */
    public static String NEW_SELECTION = "AddeChooser.NEW_SELECTION";

    /** Not connected */
    protected static final int STATE_UNCONNECTED = 0;

    /** Trying to connet */
    protected static final int STATE_CONNECTING = 1;

    /** Have connected */
    protected static final int STATE_CONNECTED = 2;

    /** flag for OK status */
    public static final int STATUS_OK = 0;

    /** flag for status of needs login */
    public static final int STATUS_NEEDSLOGIN = 1;

    /** flag for status of error */
    public static final int STATUS_ERROR = 2;


    /** flag for ignoring combobox changes */
    protected boolean ignoreStateChangedEvents = false;

    /**
     * Property for the dataset name key.
     * @see #getDataSetName()
     */
    public static String DATASET_NAME_KEY = "name";

    /**
     * Public key for data name.
     * @see #getDataName()
     */
    public final static String DATA_NAME_KEY = "data name";

    /** data name */
    public final static String PROP_DATANAME = "dataname";

    /**
     * Used to synchronize access to widgets
     *   (eg: disabling, setting state, etc).
     */
    protected Object WIDGET_MUTEX = new Object();


    /** hashtable of passwords */
    protected Hashtable passwords = new Hashtable();

    /** What is my state */
    private int state = STATE_UNCONNECTED;



    /** UI for selecting a server */
    private JComboBox serverSelector;


    /**
     * This gets updated every time the global list of addeservers is changed. It allows us
     *   to know when to update all of the combo boxes when they are repainted
     */
    public static int serverTimeStamp = 0;

    /** This represents this chooser's current version of the adde servers */
    private int myServerTimeStamp = serverTimeStamp;


    /** Widget for selecting the data group */
    protected JComboBox groupSelector;


    /**
     * List of Component-s that rely on being connected to a server.
     * We have this here so we can enable/disable them
     */
    private List compsThatNeedServer = new ArrayList();


    /**
     * Create an AddeChooser associated with an IdvChooser
     *
     *
     * @param mgr The chooser manager
     * @param root The chooser.xml node
     */
    public AddeChooser(IdvChooserManager mgr, Element root) {

        super(mgr, root);
        simpleMode = !getProperty(IdvChooser.ATTR_SHOWDETAILS, true);
        this.addeServers =
            getIdv().getIdvChooserManager().getAddeServers(getGroupType());

        serverSelector = new JComboBox(new Vector(addeServers)) {
            public void paint(Graphics g) {
                if (myServerTimeStamp != serverTimeStamp) {
                    myServerTimeStamp = serverTimeStamp;
                    Misc.runInABit(10, AddeChooser.this, "updateServerList",
                                   null);
                }
                super.paint(g);
            }
        };
        serverSelector.setEditable(true);
        serverSelector.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreStateChangedEvents) {
                    setGroups();
                }
            }
        });

        serverSelector.getEditor().getEditorComponent().addMouseListener(
            new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if ( !SwingUtilities.isRightMouseButton(e)) {
                    return;
                }
                AddeServer server = getAddeServer();
                if (server == null) {
                    return;
                }
                List items = new ArrayList();
                if (MARK_AS_INACTIVE || server.getIsLocal()) {
                    items.add(GuiUtils.makeMenuItem("Remove local server: "
                            + server.getName(), AddeChooser.this,
                                "removeServer", server));
                } else {
                    items.add(new JMenuItem("Not a local server"));
                }
                JPopupMenu popup = GuiUtils.makePopupMenu(items);
                popup.show(serverSelector, e.getX(), e.getY());
            }
        });



        groupSelector = new JComboBox();
        groupSelector.setToolTipText("Right click to remove group");
        groupSelector.setEditable(true);
        groupSelector.getEditor().getEditorComponent().addMouseListener(
            new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    Object selected = groupSelector.getSelectedItem();
                    if ((selected == null)
                            || !(selected instanceof AddeServer.Group)) {
                        return;
                    }
                    AddeServer.Group group = (AddeServer.Group) selected;
                    List             items = new ArrayList();
                    if (MARK_AS_INACTIVE || group.getIsLocal()) {
                        items.add(
                            GuiUtils.makeMenuItem(
                                "Remove local group: " + group.getName(),
                                AddeChooser.this, "removeGroup", group));
                    }

                    final AddeServer server = getAddeServer();

                    if (server != null) {
                        List groups =
                            server.getGroupsWithType(getGroupType(), false);
                        for (int i = 0; i < groups.size(); i++) {
                            final AddeServer.Group inactiveGroup =
                                (AddeServer.Group) groups.get(i);
                            if (inactiveGroup.getActive()) {
                                continue;
                            }
                            JMenuItem mi =
                                new JMenuItem("Re-activate group: "
                                    + inactiveGroup);
                            items.add(mi);
                            mi.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent ae) {
                                    getIdv().getIdvChooserManager()
                                        .activateAddeServerGroup(server,
                                            inactiveGroup);
                                    setGroups();
                                    groupSelector.setSelectedItem(
                                        inactiveGroup);

                                }
                            });
                        }
                    }

                    if (items.size() == 0) {
                        items.add(new JMenuItem("Not a local group"));
                    }


                    JPopupMenu popup = GuiUtils.makePopupMenu(items);
                    popup.show(groupSelector, e.getX(), e.getY());
                }
            }
        });
        loadServerState();
        setGroups();

    }




    /**
     * Reload the list of servers if they have changed
     */
    public void updateServerList() {
        boolean old = ignoreStateChangedEvents;
        ignoreStateChangedEvents = true;
        List newList =
            getIdv().getIdvChooserManager().getAddeServers(getGroupType());
        if (Misc.equals(newList, this.addeServers)) {
            ignoreStateChangedEvents = old;
            return;
        }
        this.addeServers =
            getIdv().getIdvChooserManager().getAddeServers(getGroupType());
        Object selected = serverSelector.getSelectedItem();
        GuiUtils.setListData(serverSelector, addeServers);
        if ((selected != null) && addeServers.contains(selected)) {
            serverSelector.setSelectedItem(selected);
        }
        setGroups();
        ignoreStateChangedEvents = old;
    }



    /**
     * get the adde server grup type to use
     *
     * @return group type
     */
    protected String getGroupType() {
        return AddeServer.TYPE_ANY;
    }


    /**
     * Remove the group from the global list
     *
     * @param group the group
     */
    public void removeGroup(AddeServer.Group group) {
        AddeServer server = getAddeServer();
        if (server == null) {
            return;
        }
        if ( !MARK_AS_INACTIVE && !group.getIsLocal()) {
            return;
        }
        getIdv().getIdvChooserManager().removeAddeServerGroup(server, group,
                MARK_AS_INACTIVE);
        setGroups();
    }


    /**
     * Remove the server
     *
     * @param server server to remove
     */
    public void removeServer(AddeServer server) {
        if ( !MARK_AS_INACTIVE && !server.getIsLocal()) {
            return;
        }
        getIdv().getIdvChooserManager().removeAddeServer(server,
                MARK_AS_INACTIVE);
        updateServerList();
    }



    /**
     * Get the selected AddeServer
     *
     * @return the server or null
     */
    private AddeServer getAddeServer() {
        Object selected = serverSelector.getSelectedItem();
        if ((selected != null) && (selected instanceof AddeServer)) {
            return (AddeServer) selected;
        }
        return null;
    }

    /**
     * Set the group list
     */
    protected void setGroups() {
        AddeServer server = getAddeServer();
        if (server != null) {
            Object selected = groupSelector.getSelectedItem();
            List   groups   = server.getGroupsWithType(getGroupType());
            GuiUtils.setListData(groupSelector, groups);
            if ((selected != null) && groups.contains(selected)) {
                groupSelector.setSelectedItem(selected);
            }

        } else {
            GuiUtils.setListData(groupSelector, new Vector());
        }
    }




    /**
     * Add a listener to the given combobox that will set the
     * state to unconnected
     *
     * @param box The box to listen to.
     */
    protected void clearOnChange(final JComboBox box) {
        box.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if ( !ignoreStateChangedEvents) {
                    setState(STATE_UNCONNECTED);
                }
            }
        });
    }


    /**
     * Handle when the user presses the connect button
     *
     * @throws Exception On badness
     */
    public void handleConnect() throws Exception {
        handleUpdate();
    }

    /**
     * Handle when the user presses the update button
     *
     * @throws Exception On badness
     */
    public void handleUpdate() throws Exception {}


    /**
     * Handle when the user presses the connect button.
     */
    public void handleConnectFromThread() {
        showWaitCursor();
        try {
            handleConnect();
        } catch (Exception exc) {
            handleConnectionError(exc);
        }
        showNormalCursor();
    }

    /**
     * Handle when the user presses the update button
     */
    public void handleUpdateFromThread() {
        showWaitCursor();
        try {
            handleUpdate();
        } catch (Exception exc) {
            handleConnectionError(exc);
        }
        showNormalCursor();
    }


    /**
     * Connect to the server. Call handleConnect in a thread
     */
    protected final void doConnect() {
        Misc.run(this, "handleConnectFromThread");
    }



    /**
     * Update the selector. Call handleUpdate in a thread
     */
    public final void doUpdate() {
        Misc.run(this, "handleUpdateFromThread");
    }


    /**
     * Handle the event
     *
     * @param ae The event
     */
    public void actionPerformed(ActionEvent ae) {
        String cmd = ae.getActionCommand();
        if (cmd.equals(CMD_CONNECT)) {
            doConnect();
        } else {
            super.actionPerformed(ae);
        }
    }



    /**
     * Load any saved server state
     */
    private void loadServerState() {
        if (addeServers == null) {
            return;
        }
        String id = getId();
        String[] serverState =
            (String[]) getIdv().getStore().get(PREF_SERVERSTATE + "." + id);
        if (serverState == null) {
            return;
        }
        AddeServer server = AddeServer.findServer(addeServers,
                                serverState[0]);
        if (server == null) {
            return;
        }
        serverSelector.setSelectedItem(server);
        setGroups();
        if (serverState[1] != null) {
            AddeServer.Group group =
                (AddeServer.Group) server.findGroup(serverState[1]);
            if (group != null) {
                groupSelector.setSelectedItem(group);
            }
        }

    }


    /**
     * Save the server state
     */
    public void saveServerState() {
        String   id          = getId();
        String   server      = getServer();
        String[] serverState = { server, getGroup() };
        getIdv().getStore().put(PREF_SERVERSTATE + "." + id, serverState);
        getIdv().getStore().save();
    }



    /**
     * Enable or disable the components in the compsThatNeedServer list
     */
    private void enableComps() {
        synchronized (WIDGET_MUTEX) {
            boolean newEnabledState = (state == STATE_CONNECTED);
            for (int i = 0; i < compsThatNeedServer.size(); i++) {
                JComponent comp = (JComponent) compsThatNeedServer.get(i);
                if (comp.isEnabled() != newEnabledState) {
                    GuiUtils.enableTree(comp, newEnabledState);
                }
            }
        }

    }


    /**
     * Add to the given comps list all the status line and server
     * components.
     *
     * @param comps List of comps to add to
     * @param label The label to add after the server selector
     * @param extra The component to add after the label (usually a combobox)
     */
    protected void addTopComponents(List comps, String label,
                                    JComponent extra) {
        addTopComponents(comps,
                         GuiUtils.hbox(padLabel(label), extra, GRID_SPACING));
    }

    /**
     * Add to the given comps list all the status line and server
     * components.
     *
     * @param comps List of comps to add to
     * @param extra The components after the server box if non-null.
     */
    protected void addTopComponents(List comps, Component extra) {
        if (extra == null) {
            extra = GuiUtils.filler();
        }
        comps.add(GuiUtils.rLabel(LABEL_SERVER));
        GuiUtils.tmpInsets = GRID_INSETS;
        JPanel right = GuiUtils.doLayout(new Component[] { serverSelector,
                extra, getConnectButton() }, 3, GuiUtils.WT_YN,
                                             GuiUtils.WT_N);
        comps.add(GuiUtils.left(right));
    }





    /**
     * Disable/enable any components that depend on the server.
     * Try to update the status labelwith what we know here.
     */
    protected void updateStatus() {
        super.updateStatus();

        //Put this in a thread to fix the enabled but shown 
        //as disabled bug
        Misc.run(new Runnable() {
            public void run() {
                enableComps();
            }
        });

        if (state != STATE_CONNECTED) {
            clearTimesList();
        }
        if (state == STATE_UNCONNECTED) {
            setStatus("Please connect to the server", "connect");
        } else if (state == STATE_CONNECTING) {
            setStatus("Connecting to server: " + getServer());
        } else if (getGoodToGo()) {
            setStatus("Press \"" + CMD_LOAD + "\" to load the selected "
                      + getDataName().toLowerCase(), "buttons");

        } else if (getState() == STATE_CONNECTED) {
            if (usingStations() && (stationMap.getStations().size() > 0)
                    && (getSelectedStations().size() == 0)) {
                if (stationMap.getMultipleSelect()) {
                    setStatus("Please select one or more stations",
                              "stations");
                } else {
                    setStatus("Please select a station", "stations");
                }
                if (stationMap.getDeclutter()) {
                    //                    setStatus(
                    //                        getStatusLabel().getText(), "stations");
                }
            } else if ( !haveTimeSelected()) {
                setStatus(MSG_TIMES);
            }
        }
        setHaveData(getGoodToGo());
    }


    /**
     * This allows derived classes to provide their own name for labeling, etc.
     *
     * @return  the dataset name
     */
    public String getDataName() {
        return "ADDE data";
    }

    /**
     * This allows derived classes to provide their own name for the dataset.
     *
     * @return  the dataset name
     */
    public String getDataSetName() {
        return "ADDE data";
    }

    /**
     * Get the data type ID
     *
     * @return  the data type
     */
    public String getDataType() {
        return "";
    }


    /**
     * Get the tooltip for the load button
     *
     * @return The tooltip for the load button
     */
    protected String getLoadToolTip() {
        return "Load the selected " + getDataName().toLowerCase();
    }


    /**
     * return the String id of the chosen server name
     *
     * @return  the server name
     */
    public String getServer() {
        Object selected = serverSelector.getSelectedItem();
        if (selected == null) {
            return null;
        }
        AddeServer server;
        if (selected instanceof AddeServer) {
            server = (AddeServer) selected;
            return server.getName();
        }
        String serverName = selected.toString();
        server = getIdv().getIdvChooserManager().addAddeServer(serverName);
        addeServers =
            getIdv().getIdvChooserManager().getAddeServers(getGroupType());

        Object           selectedGroup = groupSelector.getSelectedItem();
        AddeServer.Group group         = null;
        if (selectedGroup != null) {
            group =
                getIdv().getIdvChooserManager().addAddeServerGroup(server,
                    selectedGroup.toString(), getGroupType());
        }

        boolean old = ignoreStateChangedEvents;
        ignoreStateChangedEvents = true;
        GuiUtils.setListData(serverSelector, addeServers);
        serverSelector.setSelectedItem(server);
        setGroups();
        if (group != null) {
            groupSelector.setSelectedItem(group);
        }
        ignoreStateChangedEvents = old;
        return server.getName();
    }


    /**
     * Get the image group from the gui.
     *
     * @return The iamge group.
     */
    protected String getGroup() {
        Object selected = groupSelector.getSelectedItem();
        if (selected == null) {
            return null;
        }
        if (selected instanceof AddeServer.Group) {
            AddeServer.Group group = (AddeServer.Group) selected;
            return group.getName();
        }

        String groupName = selected.toString().trim();
        if ((groupName.length() > 0)) {
            //Force the get in case they typed a server name
            getServer();
            AddeServer server = getAddeServer();
            if (server != null) {
                AddeServer.Group group =
                    getIdv().getIdvChooserManager().addAddeServerGroup(
                        server, groupName, getGroupType());
                if ( !group.getActive()) {
                    getIdv().getIdvChooserManager().activateAddeServerGroup(
                        server, group);
                }
                //Now put the list of groups back in to the selector
                setGroups();
                groupSelector.setSelectedItem(group);
            }
        }

        return groupName;
    }



    /**
     * Get the server selector
     * @return The server selector
     */
    public JComboBox getServerSelector() {
        return serverSelector;
    }




    /**
     * Read the adde text url and return the lines of text.
     * If unsuccessful return null.
     *
     * @param url adde url to a text file
     *
     * @return List of lines or null if in error
     */
    protected List readTextLines(String url) {
        AddeTextReader reader = new AddeTextReader(url);
        if ( !reader.getStatus().equals("OK")) {
            return null;
        }
        return reader.getLinesOfText();
    }


    /**
     * Read the groups from the public.srv file on the server
     *
     * @return List of groups
     */
    protected List readGroups() {
        List groups = new ArrayList();
        try {
            String       dataType = getDataType();
            String       type     = ((dataType.length() > 0)
                                     ? "TYPE=" + dataType
                                     : "TYPE=NOTYPE");
            StringBuffer buff     = getUrl(REQ_TEXT);
            appendKeyValue(buff, PROP_FILE, FILE_PUBLICSRV);
            List lines = readTextLines(buff.toString());
            //            System.err.println ("lines:" + StringUtil.join("\n",lines));
            if (lines == null) {
                return null;
            }
            Hashtable seen = new Hashtable();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).toString();
                if (line.indexOf(type) < 0) {
                    continue;
                }
                List toks = StringUtil.split(line, ",", true, true);
                if (toks.size() == 0) {
                    continue;
                }
                String tok = (String) toks.get(0);
                int    idx = tok.indexOf("=");
                if (idx < 0) {
                    continue;
                }
                if ( !tok.substring(0, idx).trim().equals("N1")) {
                    continue;
                }
                String group = tok.substring(idx + 1).trim();
                if (seen.get(group) != null) {
                    continue;
                }
                seen.put(group, group);
                groups.add(group);
            }
        } catch (Exception e) {
            return null;
        }
        return groups;
    }




    /**
     * Handle unknown data set error
     */
    protected void handleUnknownDataSetError() {
        //Don't do this for now
        //        List groups =  readGroups();
        List groups = null;
        if (groups == null) {
            LogUtil.userErrorMessage("Dataset not found on server: "
                                     + getServer());

        } else {
            LogUtil.userErrorMessage("Dataset not found on server: "
                                     + getServer()
                                     + "\nPossible data sets:\n" + "   "
                                     + StringUtil.join("\n   ", groups));

        }
        setState(STATE_UNCONNECTED);
    }



    /**
     * Show the given error to the user. If it was an Adde exception
     * that was a bad server error then print out a nice message.
     *
     * @param excp The exception
     */
    protected void handleConnectionError(Exception excp) {
        String message = excp.getMessage();
        if (excp instanceof AddeURLException) {
            handleUnknownDataSetError();

        } else if (message.toLowerCase().indexOf("unknownhostexception")
                   >= 0) {
            LogUtil.userErrorMessage("Could not access server: "
                                     + getServer());
        } else if (message.toLowerCase().indexOf(
                "server unable to resolve this dataset") >= 0) {
            handleUnknownDataSetError();
        } else if ((message.toLowerCase().indexOf("no images satisfy") >= 0)
                   || (message.toLowerCase().indexOf(
                       "error generating list of files") >= 0)) {
            LogUtil.userErrorMessage("No data available for the selection");
            return;
        } else {
            LogUtil.logException("Error connecting to: " + getServer(), excp);
        }
        if ( !(getState() == STATE_CONNECTED)) {
            setState(STATE_UNCONNECTED);
        }
    }


    /**
     * Set the current state. This also triggers a status update
     *
     * @param newState The new state
     */
    protected void setState(int newState) {
        this.state = newState;
        updateStatus();
    }

    /**
     * Get the state
     *
     * @return The state
     */
    protected int getState() {
        return state;
    }




    /**
     * Add the given component to the list of components that depend on a connection.
     *
     * @param comp The component
     *
     * @return The same component
     */
    protected JComponent addServerComp(JComponent comp) {
        compsThatNeedServer.add(comp);
        return comp;
    }





    /**
     * Can we do a cancel
     *
     * @return true if we can
     */
    public boolean canDoCancel() {
        return false;
    }



    /**
     * Create the 'Connect' button.
     *
     * @return The connect button.
     */
    protected JComponent getConnectButton() {
        JButton connectBtn = new JButton("Connect");
        connectBtn.setActionCommand(CMD_CONNECT);
        connectBtn.addActionListener(this);
        JComponent buttonComp = connectBtn;
        registerStatusComp("connect", buttonComp);
        if (canDoCancel()) {
            cancelButton =
                GuiUtils.getImageButton("/auxdata/ui/icons/Exit16.gif",
                                        getClass());
            cancelButton.setEnabled(false);
            cancelButton.setActionCommand(GuiUtils.CMD_CANCEL);
            cancelButton.addActionListener(this);
            buttonComp = GuiUtils.hbox(buttonComp, cancelButton);
        }

        return buttonComp;
        //        return connectBtn;
    }


    /**
     *  Do what needs to be done to read in the times.  Subclasses
     *  need to implement this.
     */
    public void readTimes() {}


    /**
     * Are we all set to load data.
     *
     * @return All set to load.
     */
    protected boolean getGoodToGo() {
        if (state != STATE_CONNECTED) {
            return false;
        }
        if ((stationMap != null) && !haveStationSelected()) {
            return false;
        }
        if ( !haveTimeSelected()) {
            return false;
        }
        return true;
    }




    /**
     * A utility method to make a name=value part of the adde request string
     *
     * @param buf The buffer to append to
     * @param name The property name
     * @param value The value
     */
    protected void appendKeyValue(StringBuffer buf, String name,
                                  String value) {
        if ((buf.length() == 0) || (buf.charAt(buf.length() - 1) != '?')) {
            buf.append("&");
        }
        buf.append(name);
        buf.append("=");
        buf.append(value);
    }




    /**
     * Check if the server is ok
     *
     * @return status code
     */
    protected int checkIfServerIsOk() {
        try {
            StringBuffer buff = getUrl(REQ_TEXT);
            appendKeyValue(buff, PROP_FILE, FILE_PUBLICSRV);
            URL           url  = new URL(buff.toString());
            URLConnection urlc = url.openConnection();
            InputStream   is   = urlc.getInputStream();
            is.close();
            return STATUS_OK;
        } catch (AddeURLException ae) {
            String aes = ae.toString();
            if (aes.indexOf("Invalid project number") >= 0) {
                LogUtil.userErrorMessage("Invalid project number");
                return STATUS_NEEDSLOGIN;
            }
            if (aes.indexOf("Invalid user id") >= 0) {
                LogUtil.userErrorMessage("Invalid user ID");
                return STATUS_NEEDSLOGIN;
            }
            if (aes.indexOf("Accounting data") >= 0) {
                return STATUS_NEEDSLOGIN;
            }
            if (aes.indexOf("cannot run server 'txtgserv") >= 0) {
                return STATUS_OK;
            }
            LogUtil.userErrorMessage("Error connecting to server. "
                                     + ae.getMessage());
            return STATUS_ERROR;
        } catch (Exception exc) {
            logException("Connecting to server:" + getServer(), exc);
            return STATUS_ERROR;
        }
    }


    /**
     * This method checks if the current server is valid. If it is valid
     * then it checks if there is authentication required
     *
     * @return true if the server exists and can be accessed
     */
    protected boolean canAccessServer() {
        //Try reading the public.serv file to see if we need a username/proj
        JTextField projFld   = null;
        JTextField userFld   = null;
        JComponent contents  = null;
        JLabel     label     = null;
        boolean    firstTime = true;
        while (true) {
            int status = checkIfServerIsOk();
            if (status == STATUS_OK) {
                break;
            }
            if (status == STATUS_ERROR) {
                setState(STATE_UNCONNECTED);
                return false;
            }
            if (projFld == null) {
                projFld            = new JTextField("", 10);
                userFld            = new JTextField("", 10);
                GuiUtils.tmpInsets = GuiUtils.INSETS_5;
                contents = GuiUtils.doLayout(new Component[] {
                    GuiUtils.rLabel("User ID:"),
                    userFld, GuiUtils.rLabel("Project #:"), projFld, }, 2,
                        GuiUtils.WT_N, GuiUtils.WT_N);
                label    = new JLabel(" ");
                contents = GuiUtils.topCenter(label, contents);
                contents = GuiUtils.inset(contents, 5);
            }
            String lbl = (firstTime
                          ? "The server: " + getServer()
                            + " requires a user ID & project number for access"
                          : "Authentication for server: " + getServer()
                            + " failed. Please try again");
            label.setText(lbl);

            if ( !GuiUtils.showOkCancelDialog(null, "ADDE Project/User name",
                    contents, null)) {
                setState(STATE_UNCONNECTED);
                return false;
            }
            firstTime = false;
            String userName = userFld.getText().trim();
            String project  = projFld.getText().trim();
            if ((userName.length() > 0) && (project.length() > 0)) {
                passwords.put(getServer(),
                              new String[] { userName, project });
            }
        }
        return true;
    }




    /**
     * Create the first part of the ADDE request URL
     *
     * @param requestType     type of request
     * @return  ADDE URL prefix
     */
    protected StringBuffer getUrl(String requestType) {
        StringBuffer buff = new StringBuffer("adde://" + getServer() + "/"
                                             + requestType + "?");
        appendMiscKeyValues(buff);
        return buff;
    }


    /**
     * Create the first part of the ADDE request url and append the
     * group argument to it
     *
     * @param requestType    request type
     * @param group          image group
     * @return  ADDE URL prefix
     */
    protected StringBuffer getGroupUrl(String requestType, String group) {
        StringBuffer buff = getUrl(requestType);
        appendKeyValue(buff, PROP_GROUP, group);
        return buff;
    }



    /**
     * Get the port to use. Check the IDV for an adde.port property.
     * If not null then use the IDV property. Else use DEFAULT_PORT
     *
     * @return the port to use
     */
    protected String getPort() {
        String prop = getIdv().getProperty(PROP_ADDEPORT, (String) null);
        if (prop != null) {
            return prop;
        }
        return DEFAULT_PORT;
    }

    /**
     * Get any extra key=value pairs that are appended to all requests.
     *
     * @param buff The buffer to append onto
     */
    protected void appendMiscKeyValues(StringBuffer buff) {
        appendKeyValue(buff, PROP_COMPRESS, DEFAULT_COMPRESS);
        appendKeyValue(buff, PROP_PORT, getPort());
        appendKeyValue(buff, PROP_DEBUG, DEFAULT_DEBUG);
        appendKeyValue(buff, PROP_VERSION, DEFAULT_VERSION);
        appendKeyValue(buff, PROP_USER, DEFAULT_USER);
        appendKeyValue(buff, PROP_PROJ, DEFAULT_PROJ);
    }

    /**
     * Get the list of properties for the miscellaneous keywords
     * @return list of properties
     */
    protected String[] getMiscKeyProps() {
        return new String[] {
            PROP_COMPRESS, PROP_PORT, PROP_DEBUG, PROP_VERSION, PROP_USER,
            PROP_PROJ
        };
    }


    /**
     * Get the miscellaneous URL keywords
     * @return the key value pairs
     */
    public String getMiscKeywords() {
        StringBuffer buff = new StringBuffer();
        appendMiscKeyValues(buff);
        return buff.toString();
    }



    /**
     * Get the default selected index for the relative times list.
     *
     * @return default index
     */
    protected int getDefaultRelativeTimeIndex() {
        return 0;
    }



    /**
     * Enable or disable the GUI widgets based on what has been
     * selected.
     */
    protected void enableWidgets() {
        super.enableWidgets();
        boolean connected = (getState() == STATE_CONNECTED);
        GuiUtils.enableTree(timesContainer, connected);
        //JDM        absTimesPanel.setEnabled(getDoAbsoluteTimes() && connected);
        //       getRelativeTimesChooser().setEnabled( !getDoAbsoluteTimes()
        //                && connected);
    }


}
