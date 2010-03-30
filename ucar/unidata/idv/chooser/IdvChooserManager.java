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

package ucar.unidata.idv.chooser;



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.data.DataManager;
import ucar.unidata.data.DataSource;


import ucar.unidata.idv.*;
import ucar.unidata.idv.chooser.adde.AddeChooser;


import ucar.unidata.idv.chooser.adde.AddeServer;

import ucar.unidata.idv.ui.*;





import ucar.unidata.ui.XmlUi;



import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Msg;
import ucar.unidata.util.ObjectArray;
import ucar.unidata.util.ObjectListener;
import ucar.unidata.util.Resource;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.util.TwoFacedObject;


import ucar.unidata.xml.PreferenceManager;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlObjectStore;
import ucar.unidata.xml.XmlPersistable;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.awt.*;
import java.awt.event.*;


import java.io.File;


import java.lang.reflect.Constructor;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import javax.swing.*;

import javax.swing.event.*;




/**
 * This creates and manages the set of choosers.
 * It makes the chooser GUI from an xml specification
 * e.g.: /ucar/unidata/idv/resources/choosers.xml
 * It uses the {@link ucar.unidata.ui.XmlUi} to process
 * the xml.
 * <p>
 * This class also processes the end-user created choosers.
 * This piece has always been a bit flaky
 *
 * @author IDV development team
 * @version $Revision: 1.98 $Date: 2007/07/30 19:38:22 $
 */

public class IdvChooserManager extends IdvManager {



    /** _more_          */
    public static final String PROP_CHOOSER_TREEVIEW = "idv.chooser.treeview";

    /** _more_          */
    public static final String PROP_CHOOSER_TREEVIEW_WIDTH =
        "idv.chooser.treeview.width";

    /** _more_          */
    public static final String PROP_CHOOSER_TREEVIEW_USESPLITPANE =
        "idv.chooser.treeview.usesplitpane";


    /** Use this member to log messages (through calls to LogUtil) */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(
            IdvChooserManager.class.getName());



    /** Class attribute in choosers.xml */
    public static final String ATTR_CLASS = "class";

    /** Xml label attribute name.  For user chooser */
    public final static String ATTR_LABEL = "label";


    /** Xml attribute for the help path */
    public static final String ATTR_HELPPATH = "helppath";

    /** Xml attribute to identify different choosers */
    public final static String ATTR_CHOOSERID = "id";


    /** Xml tag name for directory tags. Used for the user chooser */
    public final static String TAG_DIRECTORY = "directory";

    /** Xml tag name for the tab tags. Used for the user chooser */
    public static final String TAG_TAB = "tab";

    /** Xml tag name for the label tags. Used for the user chooser */
    public static final String TAG_LABEL = "label";

    /** Data source tag */
    public final static String ATTR_DATASOURCE = "datasource";



    /** Xml path attribute name.  For user chooser */
    public final static String ATTR_PATH = "path";

    /** Xml pattern attribute name.  For user chooser */
    public final static String ATTR_PATTERN = "pattern";

    /** Xml dopolling attribute name.  For user chooser */
    public final static String ATTR_DOPOLLING = "dopolling";

    /** Xml pollinterval attribute name.  For user chooser */
    public final static String ATTR_POLLINTERVAL = "pollinterval";

    /** Xml title attribute name. */
    public final static String ATTR_TITLE = "title";

    /** Xml menu title attribute name. */
    public final static String ATTR_MENUTITLE = "menutitle";

    /** Xml attribute to see if this chooser should go in the data menu */
    public final static String ATTR_SHOWINMENU = "showinmenu";



    /**
     * The {@link ucar.unidata.xml.XmlResourceCollection}
     * that points to the choosers.xml
     */
    XmlResourceCollection fixedChoosers;

    /** List of all of the ids from the choosers.xml choosers */
    private List chooserIds = new ArrayList();


    /** Maps chooser id to name */
    private Hashtable idToName = new Hashtable();

    /** Maps the gui component to the chooser id */
    private Hashtable componentToChooserId = new Hashtable();

    /**
     * The {@link ucar.unidata.xml.XmlResourceCollection}
     * that points to the user chooser xml
     */
    XmlResourceCollection usersResources;

    /** The {@link ucar.unidata.data.DataManager} */
    private DataManager dataManager;


    /** The user chooser xml root */
    private Element dataChooserRoot;

    /** The user chooser xml document */
    private Document dataChooserDocument;

    /**
     * Mapping of the userchooser xml tab node to
     * the JPanel that represents it
     */
    private Hashtable tabNodeToPanel = new Hashtable();


    /** List of choosers */
    private List choosersWeAreCreating;


    /**
     * Holds the XmlChooser if we have one. We keep this around if there are any catalog command
     *   line args we can show the chooser.
     */
    private JComponent xmlChooserWrapper;


    /** Have we called init */
    private boolean haveInitialized = false;


    /** All of the adde servers */
    private List addeServers = new ArrayList();

    /** _more_          */
    private List<String> selectedDataSourceIds = null;


    /**
     *  Create a new IdvChooserManager.
     *
     *  @param idv The singleton IDV
     */
    public IdvChooserManager(IntegratedDataViewer idv) {
        super(idv);

        XmlResourceCollection addeServerResources =
            idv.getResourceManager().getXmlResources(
                IdvResourceManager.RSC_ADDESERVER);


        try {
            for (int resourceIdx = 0;
                    resourceIdx < addeServerResources.size(); resourceIdx++) {
                Element root = addeServerResources.getRoot(resourceIdx);
                if (root == null) {
                    continue;
                }
                List servers = AddeServer.processXml(root);
                if (addeServerResources.isWritableResource(resourceIdx)) {
                    for (int serverIdx = 0; serverIdx < servers.size();
                            serverIdx++) {
                        AddeServer addeServer =
                            (AddeServer) servers.get(serverIdx);
                        addeServer.setIsLocal(true);
                        List groups = addeServer.getGroups();
                        for (int groupIdx = 0; groupIdx < groups.size();
                                groupIdx++) {
                            AddeServer.Group group =
                                (AddeServer.Group) groups.get(groupIdx);
                            group.setIsLocal(true);
                        }
                    }

                }
                addeServers.addAll(servers);
            }
        } catch (Exception exc) {
            LogUtil.logException("Error processing adde server descriptions",
                                 exc);
        }
        addeServers = AddeServer.coalesce(addeServers);



        Object oldServers =
            getIdv().getStore().get(IdvChooser.PREF_ADDESERVERS);
        if ((oldServers != null) && (oldServers instanceof List)) {
            List prefs = (List) oldServers;
            for (int i = 0; i < prefs.size(); i++) {
                String server = (String) prefs.get(i);
                addAddeServer(server);
            }
            getIdv().getStore().remove(IdvChooser.PREF_ADDESERVERS);
            getIdv().getStore().saveIfNeeded();
            writeAddeServers();
        }


        fixedChoosers = idv.getResourceManager().getXmlResources(
            IdvResourceManager.RSC_CHOOSERS);
        usersResources = idv.getResourceManager().getXmlResources(
            IdvResourceManager.RSC_USERCHOOSER);

        if (usersResources.hasWritableResource()) {
            dataChooserDocument =
                usersResources.getWritableDocument("<tabs></tabs>");
            dataChooserRoot = usersResources.getWritableRoot("<tabs></tabs>");
        }
    }


    /**
     * Remove the group held by the server. Write out the addeservers.xml file
     *
     * @param server The server
     * @param group The group to remove
     * @param markAsInactive If true then don't really remove the group, just mark it as inactive
     */
    public void removeAddeServerGroup(AddeServer server,
                                      AddeServer.Group group,
                                      boolean markAsInactive) {
        if ( !markAsInactive) {
            server.getGroups().remove(group);
        } else {
            group.setActive(false);
            group.setIsLocal(true);
        }
        writeAddeServers();
    }

    /**
     * Make the group active
     *
     * @param server The server
     * @param group The group to remove
     */
    public void activateAddeServerGroup(AddeServer server,
                                        AddeServer.Group group) {
        group.setActive(true);
        group.setIsLocal(true);
        writeAddeServers();
    }




    /**
     * Add the given AddeServer.Group into the list of servers. Write out the addeserver.xml file
     *
     * @param server Server to add
     * @param groupName The new group name
     * @param type Its type
     *
     * @return The new Group
     */
    public AddeServer.Group addAddeServerGroup(AddeServer server,
            String groupName, String type) {
        //TODO: see if there is a group already 
        AddeServer.Group group = server.findGroup(groupName);
        if (group != null) {
            return group;
        }
        group = new AddeServer.Group(type, groupName, groupName);
        group.setIsLocal(true);
        server.addGroup(group);
        writeAddeServers();
        return group;
    }

    /**
     * Write out the addeservers.xml file
     */
    public void writeAddeServers() {
        try {
            XmlResourceCollection addeServerResources =
                getIdv().getResourceManager().getXmlResources(
                    IdvResourceManager.RSC_ADDESERVER);
            Element root = AddeServer.toXml(addeServers, true);
            addeServerResources.writeWritableResource(XmlUtil.toString(root));
            AddeChooser.serverTimeStamp++;
        } catch (Exception exc) {
            LogUtil.logException("Error writing the local addeservers file",
                                 exc);
        }

    }

    /**
     * Create a new AddeServer or find an existing one with the given name. If a new one is created then also
     * write out the addeservers.xml file
     *
     * @param name new server name
     *
     * @return The server
     */
    public AddeServer addAddeServer(String name) {
        AddeServer addeServer;
        for (int i = 0; i < addeServers.size(); i++) {
            addeServer = (AddeServer) addeServers.get(i);
            if (addeServer.getName().toLowerCase().equals(
                    name.toLowerCase())) {
                return addeServer;
            }
        }
        addeServer = new AddeServer(name);
        addeServer.setIsLocal(true);
        addeServers.add(addeServer);
        writeAddeServers();
        return addeServer;
    }


    /**
     * Remove the given adde server. Write out the addeservers.xml file
     *
     *
     * @param server The server to remove
     * @param markAsInactive If true then don't really remove the server, just mark it as inactive
     */
    public void removeAddeServer(AddeServer server, boolean markAsInactive) {
        if ( !markAsInactive) {
            addeServers.remove(server);
        } else {
            server.setActive(false);
            server.setIsLocal(true);
        }
        writeAddeServers();
    }


    /**
     * Get AddeServers to use
     *
     * @param groupType If null return all, else return the servers that have groups of the given type
     *
     * @return List of AddeServers
     */
    public List getAddeServers(String groupType) {
        return getAddeServers(groupType, true);
    }


    /**
     * Get AddeServers to use
     *
     * @param groupType If null return all, else return the servers that have groups of the given type
     * @param onlyActive If true then only fetch the active servers
     *
     * @return List of AddeServers
     */
    public List getAddeServers(String groupType, boolean onlyActive) {
        List servers;
        if (groupType == null) {
            servers = new ArrayList(addeServers);
        } else {
            servers = AddeServer.getServersWithType(groupType, addeServers);
        }
        if ( !onlyActive) {
            return servers;
        }

        List       activeServers = new ArrayList();
        AddeServer addeServer;
        for (int i = 0; i < addeServers.size(); i++) {
            addeServer = (AddeServer) addeServers.get(i);
            if (addeServer.getActive()) {
                activeServers.add(addeServer);
            }
        }
        return activeServers;
    }


    /**
     * Get the list of current choosers.
     * This looks at all of the IdvWindows in existence and checks for their
     * component group 'choosers'.
     *
     * @return List of current choosers
     */
    public List getChoosers() {
        return getIdv().getIdvUIManager().getWindowGroup(
            IdvWindow.GROUP_CHOOSERS);
    }


    /**
     * Get the chooser contents
     *
     * @deprecated no longer in use
     * @return null
     */
    public JComponent getChooserContents() {
        return null;
    }


    /**
     * Initialize the set of choosers. Go through the chooser
     * resources and create the chooser gui.
     */
    public void init() {
        if (haveInitialized) {
            return;
        }
        haveInitialized = true;
    }


    /**
     * Create the Choosers component from the choosers.xml resources
     *
     * @param inTabs  Do we use the buttontabbedpane or the treepanel
     *
     * @return choosers gui
     */
    public JComponent createChoosers(boolean inTabs) {
        return createChoosers(inTabs, new ArrayList(), null);
    }

    /**
     * Create the Choosers component from the choosers.xml resources
     *
     * @param inTabs  Do we use the buttontabbedpane or the treepanel
     * @param theseChoosers List to add the created choosers into
     * @param choosersNode The xml node of the chooser skin
     *
     * @return choosers gui
     */
    public JComponent createChoosers(boolean inTabs, List theseChoosers,
                                     Element choosersNode) {
        JComponent   contents = null;
        StringBuffer sb       = new StringBuffer();
        StringBuffer otherXml = new StringBuffer();
        String       pre      = null;
        String       post     = null;
        if (theseChoosers == null) {
            theseChoosers = new ArrayList();
        }
        choosersWeAreCreating = theseChoosers;
        for (int resourceIdx = 0; resourceIdx < fixedChoosers.size();
                resourceIdx++) {
            Element root = fixedChoosers.getRoot(resourceIdx);
            if (root == null) {
                continue;
            }
            NodeList nodeList = XmlUtil.getElements(root);
            for (int j = 0; j < nodeList.getLength(); j++) {
                Element node = (Element) nodeList.item(j);
                if (node.getTagName().equals(XmlUi.TAG_COMPONENTS)
                        || node.getTagName().equals(XmlUi.TAG_STYLES)) {
                    otherXml.append(XmlUtil.toString(node));
                } else if (node.getTagName().equals(XmlUi.TAG_PROPERTIES)) {
                    //look for the pre property
                    if (pre == null) {
                        NodeList props = XmlUtil.getElements(node,
                                             XmlUi.TAG_PROPERTY);
                        for (int k = 0; k < props.getLength(); k++) {
                            Element prop = (Element) props.item(k);
                            if (XmlUtil.getAttribute(
                                    prop, XmlUi.ATTR_NAME).equals(
                                    "idv.chooser.toptag.open")) {
                                pre = XmlUtil.getAttribute(prop,
                                        XmlUi.ATTR_VALUE);
                            } else if (XmlUtil.getAttribute(
                                    prop, XmlUi.ATTR_NAME).equals(
                                    "idv.chooser.toptag.close")) {
                                post = XmlUtil.getAttribute(prop,
                                        XmlUi.ATTR_VALUE);
                            }
                        }
                    }
                    otherXml.append(XmlUtil.toString(node));
                } else {
                    sb.append(XmlUtil.toString(node));
                }
            }
        }
        //if no pre property then default to buttontabbedpane
        if ((pre == null) || (pre.trim().length() == 0)) {
            if (inTabs) {
                pre  = "<buttontabbedpane>";
                post = "</buttontabbedpane>";
            } else {
                String attrs = "usesplitpane=\""
                               + getIdv().getProperty(
                                   PROP_CHOOSER_TREEVIEW_USESPLITPANE,
                                   true) + "\"  ";
                attrs = attrs + "treewidth=\""
                        + getIdv().getProperty(PROP_CHOOSER_TREEVIEW_WIDTH,
                            200) + "\"";
                pre  = "<treepanel " + attrs + ">";
                post = "</treepanel>";
            }
        }
        try {
            String xml = "<skin>" + otherXml + "<ui>" + pre + sb.toString()
                         + post + "</ui></skin>";
            Element root  = XmlUtil.getRoot(xml);
            XmlUi   xmlUi = createXmlUi(root);
            collectIds(root);
            contents = (JComponent) xmlUi.getContents();
            pruneEmptyTabs(contents);
        } catch (Exception exc) {
            LogUtil.logException("Error processing chooser xml", exc);
            contents = new JPanel();
        }


        try {
            processChooserXml();
        } catch (Exception exc) {
            LogUtil.printException(log_, "Processing user chooser xml.", exc);
        }


        choosersWeAreCreating = null;
        Msg.translateTree(contents, true, false);
        return contents;

    }

    /**
     * Factory method to create the IdvXmlUi to be used for the given chooser xml node
     *
     * @param root The chooser xml node
     *
     * @return _more_
     */
    protected XmlUi createXmlUi(Element root) {
        return new IdvXmlUi(getIdv(), root);
    }

    /**
     * Remove any tabs in tabbed panes that do not contain any choosers
     * TODO: This will probably remove any of the user defined chooser panels
     *
     * @param comp The component to look
     *
     * @return Does the component contain any choosers
     */
    private boolean pruneEmptyTabs(JComponent comp) {
        Component[] components = comp.getComponents();
        boolean anyContainedChoosers = (componentToChooserId.get(comp)
                                        != null);
        if (anyContainedChoosers) {
            return true;
        }
        for (int i = 0; i < components.length; i++) {
            if ( !(components[i] instanceof JComponent)) {
                continue;
            }
            JComponent child = (JComponent) components[i];
            anyContainedChoosers |= pruneEmptyTabs(child);
        }

        if ((comp.getParent() != null)
                && (comp.getParent() instanceof JTabbedPane)
                && !anyContainedChoosers) {
            comp.getParent().remove(comp);
        }
        return anyContainedChoosers;
    }




    /**
     * Overwrite the component factory method in
     * {@link ucar.unidata.ui.XmlUi} to handler chooser
     * xml nodes.
     *
     * @param chooserNode The chooser node
     * @return The chooser's gui
     */
    public Component createChooser(Node chooserNode) {
        String className = XmlUtil.getAttribute(chooserNode, ATTR_CLASS);
        String id        = getChooserId((Element) chooserNode);
        //        Trace.msg("create: " + className);

        IdvChooser chooser = null;
        try {
            Class theClass = Misc.findClass(className);
            if (theClass != null) {
                Class[] paramTypes = new Class[] { IdvChooserManager.class,
                        Element.class };
                Object[]    args = new Object[] { this, chooserNode };
                Constructor ctor = Misc.findConstructor(theClass, paramTypes);
                if (ctor != null) {
                    chooser = (IdvChooser) ctor.newInstance(args);
                    chooser.setHelpPath(XmlUtil.getAttribute(chooserNode,
                            ATTR_HELPPATH, "idv.data.choosers"));
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            throw new IllegalArgumentException(
                "Unable to create chooser class: " + className + " \nError: "
                + exc);
        }
        if (chooser == null) {
            throw new IllegalArgumentException(
                "Unable to create chooser class: " + className);
        }
        Component contents = chooser.getContents();
        if (choosersWeAreCreating != null) {
            choosersWeAreCreating.add(chooser);
        }
        //      Trace.msg("Done: " + className);

        JComponent chooserContents = (JComponent) contents;

        //        JComponent chooserContents =
        //            GuiUtils.topCenter(GuiUtils.inset(contents, 5),
        //                               GuiUtils.filler());
        componentToChooserId.put(chooserContents, id);

        if (chooser instanceof XmlChooser) {
            xmlChooserWrapper = chooserContents;
        }

        return chooserContents;
    }


    /**
     * Make the chooser menu items
     *
     * @param items List to put the menu items in
     *
     * @return List of chooser menu items
     */
    public List makeChooserMenus(List items) {
        List choosers = getChoosers();
        //this is a little tricky. If there aren't any choosers
        //then it might mean the user deleted the dashboard window
        //If so, we recreate the basic windows if there aren't any.
        
        if ((choosers == null) || (choosers.size() == 0)) {
            if ( !getIdv().getIdvUIManager().haveBasicWindow()) {
                getIdv().getIdvUIManager().showBasicWindow(true);
            }
            choosers = getChoosers();
        }
        if ((choosers == null) || (choosers.size() == 0)) {
            return new ArrayList();
        }
        Hashtable seen = new Hashtable();
        //        System.err.println ("choosers:" + choosers.size());
        for (int i = 0; i < choosers.size(); i++) {
            final IdvChooser chooser = (IdvChooser) choosers.get(i);
            if ( !chooser.getShowInMenu()) {
                continue;
            }
            //Check for duplicates
            if (seen.get(chooser.getId()) != null) {
                continue;
            }
            seen.put(chooser.getId(), "");
            if ( !getIdv().getPreferenceManager().shouldShowChooser(
                    chooser.getId())) {
                continue;
            }
            JMenuItem mi = new JMenuItem(chooser.getMenuTitle());
            items.add(mi);
            mi.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent ae) {
                    GuiUtils.showComponentInTabs(chooser.getContents());
                }
            });
        }
        return items;
    }


    /**
     * This tries to find some  identifier for the
     * chooser defied by the given xml node. This
     * is used when the end user can define what choosers
     * they want to see in the chooser GUI.
     *
     * @param node The chooser node
     * @return Some (hopefully) unique identifier
     */
    public static String getChooserId(Element node) {
        //First look for id, then title then class.
        String chooserId = XmlUtil.getAttribute(node, ATTR_CHOOSERID,
                               (String) null);
        if (chooserId == null) {
            chooserId = XmlUtil.getAttribute(node, ATTR_TITLE, (String) null);
            if (chooserId == null) {
                chooserId = XmlUtil.getAttribute(node, ATTR_CLASS,
                        (String) null);
            }
        }
        return chooserId;
    }


    /**
     *  Walk the xml tree, collecting any chooser ids
     *
     *  @param node The xml tree to walk.
     */
    private void collectIds(Element node) {
        String tagName = node.getTagName();
        if (tagName.equals(IdvUIManager.COMP_CHOOSER)) {
            String chooserId = getChooserId(node);
            if ( !chooserIds.contains(chooserId)) {
                chooserIds.add(chooserId);
                idToName.put(chooserId,
                             XmlUtil.getAttribute(node, ATTR_TITLE,
                                 chooserId));
            }
        }
        NodeList nodeList = XmlUtil.getElements(node);
        for (int j = 0; j < nodeList.getLength(); j++) {
            collectIds((Element) nodeList.item(j));
        }
    }




    /**
     * Return a list of the (String) chooser ids that have been
     * processed by this manager. We have this here for the
     * end user to define what choosers they want to see
     *
     *  @return The list of (String) chooser ids.
     */
    public List getChooserIds() {
        return chooserIds;
    }

    /**
     * Get the name of the given chooser
     *
     * @param chooserId The id of the chooser
     *
     * @return Its name
     */
    public String getChooserName(String chooserId) {
        String name = (String) idToName.get(chooserId);
        if (name == null) {
            return chooserId;
        }
        return name;
    }


    /**
     * Inserts menu items into the given menu for
     * creating the data sources that are defined by the
     * user chooser
     *
     * @param menu The menu
     * @param pullRight Will end up being the sub menu
     * @return  The pullRight menu
     */
    public JMenu addUserChooserToMenu(JMenu menu, JMenu pullRight) {
        if (dataChooserRoot == null) {
            return pullRight;
        }
        List tabNodes = XmlUtil.findChildren(dataChooserRoot, TAG_TAB);
        for (int tabIdx = 0; tabIdx < tabNodes.size(); tabIdx++) {
            Element tabNode = (Element) tabNodes.get(tabIdx);
            if (pullRight == null) {
                pullRight = new JMenu("Local");
                menu.add(pullRight);
            }
            JMenu tabMenu = new JMenu(XmlUtil.getAttribute(tabNode,
                                ATTR_TITLE));
            pullRight.add(tabMenu);
            NodeList nodeList = XmlUtil.getElements(tabNode);
            for (int j = 0; j < nodeList.getLength(); j++) {
                Element node = (Element) nodeList.item(j);
                if ( !node.getNodeName().equals(TAG_DIRECTORY)) {
                    continue;
                }
                JMenuItem mi = new JMenuItem(XmlUtil.getAttribute(node,
                                   ATTR_LABEL));
                mi.addActionListener(new ObjectListener(node) {
                    public void actionPerformed(ActionEvent event) {
                        makeDataSourceFromXml((Element) theObject);
                    }
                });
                tabMenu.add(mi);
            }
        }
        return pullRight;
    }




    /**
     * _more_
     *
     * @return _more_
     */
    private List<String> getSelectedDataSourceIds() {
        if (selectedDataSourceIds == null) {
            selectedDataSourceIds = (List) getIdv().getStore().getEncodedFile(
                "selecteddatasourceids.xml");
        }
        if (selectedDataSourceIds == null) {
            selectedDataSourceIds = new ArrayList();
        }
        return selectedDataSourceIds;
    }

    /**
     * _more_
     */
    private void writeSelectedDataSourceIds() {
        if (selectedDataSourceIds != null) {
            getIdv().getStore().putEncodedFile("selecteddatasourceids.xml",
                    selectedDataSourceIds);
        }
    }




    /**
     * _more_
     *
     * @param ids _more_
     *
     * @return _more_
     */
    public List sortDataSourceIds(List ids) {
        List result = new ArrayList();
        for (String id : (List<String>) getSelectedDataSourceIds()) {
            TwoFacedObject tfo = TwoFacedObject.findId(id, ids);
            if (tfo != null) {
                ids.remove(tfo);
                result.add(tfo);
            }
        }
        //      TwoFacedObject.sort(result);
        TwoFacedObject.sort(ids);
        result.addAll(ids);
        return result;
    }


    /**
     * _more_
     *
     * @param id _more_
     */
    public void dataSourceIdSelected(String id) {
        List ids = getSelectedDataSourceIds();
        ids.remove(id);
        ids.add(0, id);
        while (selectedDataSourceIds.size() > 15) {
            ids.remove(selectedDataSourceIds.size() - 1);
        }
        writeSelectedDataSourceIds();
    }



    /**
     * Create the data source from the given user chooser xml
     *
     * @param theNode User chooser xml node that defines a data source
     */
    public void makeDataSourceFromXml(Element theNode) {
        String title      = XmlUtil.getAttribute(theNode, ATTR_LABEL);
        String dataSource = XmlUtil.getAttribute(theNode, ATTR_DATASOURCE);
        String path       = XmlUtil.getAttribute(theNode, ATTR_PATH);
        String pattern = XmlUtil.getAttribute(theNode, ATTR_PATTERN,
                             (String) null);
        Hashtable properties = new Hashtable();
        properties.put(DataSource.PROP_TITLE, title);
        if (pattern != null) {
            //            properties.put(DataSource.PROP_FILEPATTERN, pattern);
        }

        getIdv().makeDataSource(path, dataSource, properties);
    }


    /**
     * Add a new pane into the GUI
     */
    private void newPane() {
        if (dataChooserRoot == null) {
            return;
        }
        String paneName = GuiUtils.getInput("New Pane Name: ");
        if (paneName == null) {
            return;
        }
        Element tabElement = dataChooserDocument.createElement(TAG_TAB);
        tabElement.setAttribute(ATTR_TITLE, paneName);
        dataChooserRoot.appendChild(tabElement);
        writeChooserXml();
        processChooserXml();
    }


    /**
     * Close me
     */
    public void closeDataChooser() {
        getIdv().getIdvUIManager().showDataSelector();
        super.close();
    }


    /**
     * Should we make a dialog
     *
     * @return true
     */
    protected boolean shouldMakeDialog() {
        return true;
    }

    /**
     * Window title
     *
     * @return window title
     */
    protected String getWindowTitle() {
        return "Data Source Chooser";
    }




    /**
     * Show the dialog for editing the user chooser entry
     *
     * @param tabNode What tab the entry is in
     * @param dirNode What entry
     */
    protected void popupXmlChooserDialog(Element tabNode, Element dirNode) {

        if (dataChooserRoot == null) {
            return;
        }

        boolean   edit          = (dirNode != null);


        List      sources       = dataManager.getFileDataSourceList();
        JComboBox dataSourceBox = new JComboBox(new Vector(sources));
        if (dirNode != null) {
            TwoFacedObject current = new TwoFacedObject("",
                                         XmlUtil.getAttribute(dirNode,
                                             ATTR_DATASOURCE));
            int index = sources.indexOf(current);
            if (index >= 0) {
                dataSourceBox.setSelectedIndex(index);
            }
        }

        JTextField patternField = new JTextField((edit
                ? XmlUtil.getAttribute(dirNode, ATTR_PATTERN, "")
                : ""), 25);
        JTextField labelField = new JTextField((edit
                ? XmlUtil.getAttribute(dirNode, ATTR_LABEL)
                : ""), 25);
        JTextField fileField = new JTextField((edit
                ? XmlUtil.getAttribute(dirNode, ATTR_PATH)
                : ""), 25);
        JCheckBox pollingCbx =
            new JCheckBox("Check this data source periodically for changes?",
                          (edit
                           ? XmlUtil.getAttribute(dirNode, ATTR_DOPOLLING,
                               false)
                           : false));
        JTextField intervalField = new JTextField((edit
                ? "" + XmlUtil.getAttribute(dirNode, ATTR_POLLINTERVAL, 10)
                : "10"), 5);
        JButton fileButton = GuiUtils.makeJButton("Select",
                                 new ObjectListener(fileField) {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser chooser =
                    new JFileChooser(((JTextField) theObject).getText());
                chooser.setFileSelectionMode(
                    JFileChooser.FILES_AND_DIRECTORIES);

                chooser.setApproveButtonText("Select");
                chooser.showOpenDialog(null);
                File file = chooser.getSelectedFile();
                if (file == null) {
                    return;
                }
                ((JTextField) theObject).setText(file.toString());
            }
        });

        JPanel intervalPanel = GuiUtils.doLayout(new Component[] {
                                   intervalField,
                                   new JLabel(" (Seconds)"),
                                   GuiUtils.filler() }, 3, GuiUtils.WT_NNY,
                                       GuiUtils.WT_N);

        JPanel dataSourcePanel = GuiUtils.doLayout(new Component[] {
                                     dataSourceBox,
                                     GuiUtils.filler() }, 2, GuiUtils.WT_NY,
                                         GuiUtils.WT_N);
        GuiUtils.setHFill();
        GuiUtils.tmpInsets = new Insets(4, 4, 4, 4);
        JPanel contents = GuiUtils.doLayout(new Component[] {
            GuiUtils.rLabel("Label: "), labelField, GuiUtils.filler(),
            GuiUtils.rLabel("File or Directory: "), fileField, fileButton,
            GuiUtils.rLabel("Data Source Type: "), dataSourcePanel,
            GuiUtils.filler(), GuiUtils.rLabel("File Pattern: "),
            patternField, GuiUtils.filler(), GuiUtils.filler(), pollingCbx,
            GuiUtils.filler(), GuiUtils.rLabel("If so, how often:"),
            intervalPanel
        }, 3, GuiUtils.WT_NYN, GuiUtils.WT_N);


        int     newPollingInterval = 0;
        boolean keepTrying         = true;
        while (keepTrying) {
            if (edit) {
                int result = GuiUtils.makeDialog(null, "Edit Chooser Entry",
                                 contents, null, new String[] { "Delete",
                        "OK", "Cancel" });
                if (result == 2) {
                    return;         //Cancel
                }
                if (result == 0) {  //Delete
                    if ( !GuiUtils.showYesNoDialog(null,
                            "Are you sure you want to delete the entry?",
                            "Delete Confirmation")) {
                        continue;
                    }
                    tabNode.removeChild(dirNode);
                    writeChooserXml();
                    processChooserXml();
                    return;
                }
            } else {
                int result = GuiUtils.makeDialog(null,
                                 "Create Chooser Entry", contents, null,
                                 new String[] { "Create",
                        "Cancel" });
                if (result == 1) {
                    return;  //Cancel
                }
            }
            try {
                newPollingInterval =
                    new Integer(intervalField.getText().trim()).intValue();
                keepTrying = false;
            } catch (Exception exc) {
                LogUtil.userErrorMessage(log_,
                                         "Bad polling interval:"
                                         + intervalField.getText());
            }
        }


        if ( !edit) {
            dirNode = dataChooserDocument.createElement(TAG_DIRECTORY);
            tabNode.appendChild(dirNode);
        }

        dirNode.setAttribute(ATTR_DATASOURCE,
                             (String) ((TwoFacedObject) dataSourceBox
                                 .getSelectedItem()).getId());
        dirNode.setAttribute(ATTR_PATH, fileField.getText());
        dirNode.setAttribute(ATTR_PATTERN, patternField.getText());
        dirNode.setAttribute(ATTR_LABEL, labelField.getText());
        dirNode.setAttribute(ATTR_DOPOLLING, "" + pollingCbx.isSelected());
        dirNode.setAttribute(ATTR_POLLINTERVAL, "" + newPollingInterval);
        writeChooserXml();
        processChooserXml();
    }


    /**
     *  Process the user chooser resources
     */
    public void processChooserXml() {
        for (int i = usersResources.size() - 1; i >= 0; i--) {
            processChooserXml(usersResources.isWritable(i),
                              usersResources.getRoot(i));
        }
    }

    /**
     *  Process the user chooser xml root
     *
     * @param writable Is this the writable resource
     * @param root The xml document root
     */
    public void processChooserXml(boolean writable, Element root) {

        if (true) {
            return;
        }

        if (root == null) {
            return;
        }
        List tabNodes = XmlUtil.findChildren(root, TAG_TAB);
        for (int tabIdx = 0; tabIdx < tabNodes.size(); tabIdx++) {
            Element   tabNode  = (Element) tabNodes.get(tabIdx);
            String    tabTitle = XmlUtil.getAttribute(tabNode, ATTR_TITLE);
            NodeList  nodeList = XmlUtil.getElements(tabNode);
            ArrayList comps    = new ArrayList();
            for (int j = 0; j < nodeList.getLength(); j++) {
                Element node = (Element) nodeList.item(j);
                if (node.getNodeName().equals(TAG_LABEL)) {
                    comps.add(new JLabel(XmlUtil.getAttribute(node,
                            ATTR_LABEL)));
                    continue;
                }
                if (node.getNodeName().equals(TAG_DIRECTORY)) {
                    XmlUtil.ensureAttributes(node,
                                             new String[] { ATTR_DATASOURCE,
                            ATTR_PATH });
                    JButton createBtn =
                        new JButton(XmlUtil.getAttribute(node, ATTR_LABEL));
                    createBtn.addActionListener(new ObjectListener(node) {
                        public void actionPerformed(ActionEvent event) {
                            makeDataSourceFromXml((Element) theObject);
                            closeDataChooser();
                        }
                    });

                    JButton editBtn =
                        GuiUtils.getImageButton(
                            "/ucar/unidata/idv/images/edit.gif", getClass());
                    editBtn.setToolTipText("Edit entry");
                    editBtn.addActionListener(
                        new ObjectListener(new ObjectArray(tabNode, node)) {
                        public void actionPerformed(ActionEvent event) {
                            ObjectArray oa = (ObjectArray) theObject;
                            popupXmlChooserDialog((Element) oa.getObject1(),
                                    (Element) oa.getObject2());
                        }
                    });

                    comps.add(GuiUtils.inset(GuiUtils.hbox(editBtn,
                            createBtn), new Insets(0, 8, 0, 0)));
                    continue;
                }
            }
            JPanel tabComponent = (JPanel) tabNodeToPanel.get(tabNode);
            if (tabComponent == null) {
                tabComponent = new JPanel(new BorderLayout());
                //                tabbedPane.add(tabTitle, tabComponent);
                tabNodeToPanel.put(tabNode, tabComponent);
            }



            JPanel buttonPanel = GuiUtils.vbox(comps);

            JButton newBtn = GuiUtils.makeJButton("New entry",
                                 new ObjectListener(tabNode) {
                public void actionPerformed(ActionEvent event) {
                    popupXmlChooserDialog((Element) theObject, null);
                }
            });
            JButton deleteBtn = GuiUtils.makeJButton("Delete pane",
                                    new ObjectListener(tabNode) {
                public void actionPerformed(ActionEvent event) {
                    if ( !GuiUtils.showYesNoDialog(null,
                            "Are you sure you want to delete the entire tab?",
                            "Delete Confirmation")) {
                        return;
                    }
                    removeTab((Element) theObject);
                }
            });

            JButton cancelBtn = GuiUtils.makeJButton("Cancel",
                                    new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    closeDataChooser();
                }
            });
            GuiUtils.tmpInsets = new Insets(5, 5, 5, 5);
            JPanel newCancelPanel;
            if (writable) {
                newCancelPanel = GuiUtils.doLayout(new Component[] { newBtn,
                        deleteBtn, cancelBtn }, 3, GuiUtils.WT_N,
                        GuiUtils.WT_N);
            } else {
                newCancelPanel = GuiUtils.doLayout(new Component[] {
                    cancelBtn }, 3, GuiUtils.WT_N, GuiUtils.WT_N);
            }

            buttonPanel = GuiUtils.doLayout(new Component[] { buttonPanel,
                    GuiUtils.filler() }, 1, GuiUtils.WT_Y, GuiUtils.WT_NY);
            Component tabContents =
                GuiUtils.inset(GuiUtils.centerBottom(buttonPanel,
                    newCancelPanel), 5);
            tabComponent.add("Center", tabContents);
            tabComponent.invalidate();
        }

    }

    /**
     * Remove the given tab node from the user chooser xml
     *
     * @param tabNode The node to remove
     */
    private void removeTab(Element tabNode) {
        if (dataChooserRoot == null) {
            return;
        }
        JPanel tabComponent = (JPanel) tabNodeToPanel.get(tabNode);
        dataChooserRoot.removeChild(tabNode);
        //        tabbedPane.remove(tabComponent);
        writeChooserXml();
        processChooserXml();

    }

    /**
     * Write out the user chooser xml document
     */
    protected void writeChooserXml() {
        if (dataChooserRoot == null) {
            return;
        }
        try {
            usersResources.writeWritable();
        } catch (Exception exc) {
            LogUtil.printException(log_, "writing chooser xml", exc);
        }

    }

    /**
     * Return the component that holds the dataSources combo box
     *
     * @param justFileSources If true then just use data sources that access files
     * @param dataManager The data manager
     * @return The GUI for the data sources selector
     */
    public JComboBox getDataSourcesComponent(boolean justFileSources,
                                             DataManager dataManager) {
        return getDataSourcesComponent(justFileSources, dataManager, true);
    }

    /**
     * Get the component for listing data source types
     *
     * @param justFileSources  true for just files
     * @param dataManager  the data manager
     * @param addLucky  true to add the "I'm Feeiling Lucky" option
     *
     * @return the component
     */
    public JComboBox getDataSourcesComponent(boolean justFileSources,
                                             DataManager dataManager,
                                             boolean addLucky) {
        JComboBox dataSourcesCbx  = new JComboBox();
        List      dataSources     = new ArrayList();
        List      fileDataSources = (justFileSources
                                     ? dataManager.getFileDataSourceList()
                                     : (List) dataManager
                                         .getAllDataSourceIds());

        dataSources.addAll(fileDataSources);
        dataSources = sortDataSourceIds(dataSources);
        if (addLucky) {
            dataSources.add(0, "I'm Feeling Lucky");

        }
        GuiUtils.setListData(dataSourcesCbx, dataSources);
        dataSourcesCbx.setToolTipText(
            "<html>Optional way to specifically select<br>the type of datasource.</html>");
        return dataSourcesCbx;
    }





}
