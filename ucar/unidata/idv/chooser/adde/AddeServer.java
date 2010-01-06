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


import org.w3c.dom.Document;

import org.w3c.dom.Element;

import ucar.unidata.util.Misc;

import ucar.unidata.util.NamedThing;
import ucar.unidata.util.StringUtil;

import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;

import java.util.Hashtable;
import java.util.List;


/**
 * Holds information about the available adde servers
 *
 *
 *
 * @author IDV development team
 * @version $Revision: 1.4 $Date: 2007/07/19 18:11:33 $
 */
public class AddeServer extends NamedThing {


    /** group type */
    public static final String TYPE_NONE = "none";

    /** group type */
    public static final String TYPE_ANY = "any";

    /** image type */
    public static final String TYPE_IMAGE = "image";

    /** radar type */
    public static final String TYPE_RADAR = "radar";

    /** point type */
    public static final String TYPE_POINT = "point";

    /** text type */
    public static final String TYPE_WXTEXT = "wxtext";

    /** xml tag name */
    private final static String TAG_SERVERS = "servers";


    /** xml tag name */
    private final static String TAG_SERVER = "server";

    /** xml tag name */
    private final static String TAG_TYPE = "type";

    /** xml tag name */
    private final static String TAG_GROUP = "group";

    /** xml tag name */
    private final static String TAG_GROUPS = "groups";

    /** xml attr name */
    private final static String ATTR_NAME = "name";

    /** xml attr name */
    private final static String ATTR_ACTIVE = "active";

    /** xml attr name */
    private final static String ATTR_NAMES = "names";

    /** xml attr name */
    private final static String ATTR_DESCRIPTION = "description";

    /** xml attr name */
    private final static String ATTR_TYPE = "type";

    /** This servers groups_ */
    private List groups = new ArrayList();

    /** Is this group a local group */
    private boolean isLocal = false;

    /** Is active_ */
    private boolean active = true;


    /**
     * ctor
     */
    public AddeServer() {}

    /**
     * ctor
     *
     * @param name my name
     */
    public AddeServer(String name) {
        this(name, name);
    }

    /**
     * ctor
     *
     * @param name my name
     * @param description my description
     */
    public AddeServer(String name, String description) {
        super(name, description);
    }


    /**
     * Create the xml representation of this server
     *
     * @param doc The xml doc to create nodes with
     * @param localOnly Only look at local groups
     *
     * @return xml node
     *
     * @throws Exception On badness
     */
    private Element toXml(Document doc, boolean localOnly) throws Exception {
        Element serverNode = doc.createElement(TAG_SERVER);
        serverNode.setAttribute(ATTR_NAME, getName());
        serverNode.setAttribute(ATTR_ACTIVE, "" + getActive());
        if (getDescription() != null) {
            serverNode.setAttribute(ATTR_DESCRIPTION, getDescription());
        }
        int cnt = 0;
        for (int i = 0; i < groups.size(); i++) {
            Group g = (Group) groups.get(i);
            if (localOnly && !g.getIsLocal()) {
                continue;
            }
            cnt++;
            Element descNode = doc.createElement(TAG_GROUP);
            descNode.setAttribute(ATTR_NAMES, g.getName());
            descNode.setAttribute(ATTR_ACTIVE, "" + g.getActive());
            descNode.setAttribute(ATTR_TYPE, g.getType());
            if (g.getDescription() != null) {
                descNode.setAttribute(ATTR_DESCRIPTION, g.getDescription());
            }
            serverNode.appendChild(descNode);
        }
        if ((cnt == 0) && localOnly && !getIsLocal()) {
            return null;
        }
        return serverNode;
    }

    /**
     * Create the xml tree for all of the given servers
     *
     * @param servers List of servers
     * @param localOnly Only include servers and groups that are marked as local
     *
     * @return xml root
     *
     * @throws Exception On badness
     */
    public static Element toXml(List servers, boolean localOnly)
            throws Exception {
        Document doc  = XmlUtil.makeDocument();
        Element  root = doc.createElement(TAG_SERVERS);
        for (int i = 0; i < servers.size(); i++) {
            AddeServer server = (AddeServer) servers.get(i);
            Element    node   = server.toXml(doc, localOnly);
            if (node != null) {
                root.appendChild(node);
            }
        }
        return root;
    }




    /**
     * Process the server tag xml node
     *
     * @param server the server
     * @param root The chooser.xml node
     */
    private static void processXml(AddeServer server, Element root) {
        List typeNodes = XmlUtil.findChildren(root, TAG_TYPE);
        for (int i = 0; i < typeNodes.size(); i++) {
            Element typeNode = (Element) typeNodes.get(i);
            String  type     = XmlUtil.getAttribute(typeNode, ATTR_NAME);
            processGroups(server, typeNode, type);
        }
        processGroups(server, root, null);

    }


    /**
     * Merge all of the similarly named servers in the list
     *
     * @param servers list of servers
     *
     * @return merged servers
     */
    public static List coalesce(List servers) {
        List      result = new ArrayList();
        Hashtable seen   = new Hashtable();
        for (int i = 0; i < servers.size(); i++) {
            AddeServer server = (AddeServer) servers.get(i);
            AddeServer gotIt  = (AddeServer) seen.get(server.getName());
            if (gotIt != null) {
                gotIt.addGroups(server.getGroups());
            } else {
                seen.put(server.getName(), server);
                result.add(server);
            }

        }
        return result;
    }


    /**
     * Process the xml node
     *
     * @param server The server
     * @param root The xml node
     * @param type the group type. May be null
     */
    private static void processGroups(AddeServer server, Element root,
                                      String type) {
        List descNodes = XmlUtil.findChildren(root, TAG_GROUP);
        for (int j = 0; j < descNodes.size(); j++) {
            Element descNode = (Element) descNodes.get(j);
            String  theType  = type;
            if (theType == null) {
                theType = XmlUtil.getAttribute(descNode, ATTR_TYPE);
            }

            String name;
            List names = StringUtil.split(XmlUtil.getAttribute(descNode,
                             ATTR_NAMES, ""), ",", true, true);
            for (int nameIdx = 0; nameIdx < names.size(); nameIdx++) {
                name = (String) names.get(nameIdx);
                if (name.length() > 0) {
                    Group group = new Group(theType, name, name);
                    group.setActive(XmlUtil.getAttribute(descNode,
                            ATTR_ACTIVE, true));
                    server.addGroup(group);
                }
            }

            //Now try the name attribute
            name = XmlUtil.getAttribute(descNode, ATTR_NAME, (String) null);
            if ((name != null) && (name.length() > 0)) {
                Group group = new Group(theType, name,
                                        XmlUtil.getAttribute(descNode,
                                            ATTR_DESCRIPTION, name));
                group.setActive(XmlUtil.getAttribute(descNode, ATTR_ACTIVE,
                        true));
                server.addGroup(group);
            }

        }
    }

    /**
     * process the xml root
     *
     * @param root The xml node
     *
     * @return List of servers
     */
    public static List processXml(Element root) {
        List servers     = new ArrayList();
        List serverNodes = XmlUtil.findChildren(root, TAG_SERVER);
        for (int i = 0; i < serverNodes.size(); i++) {
            Element serverNode = (Element) serverNodes.get(i);
            String  name       = XmlUtil.getAttribute(serverNode, ATTR_NAME);
            String desc = XmlUtil.getAttribute(serverNode, ATTR_DESCRIPTION,
                              name);
            AddeServer server = new AddeServer(name, desc);
            server.setActive(XmlUtil.getAttribute(serverNode, ATTR_ACTIVE,
                    true));
            processXml(server, serverNode);
            servers.add(server);
        }
        return servers;
    }



    /**
     * Find the server with the given name in the given list
     *
     * @param servers List of servers
     * @param name name
     *
     * @return The found server or null
     */
    public static AddeServer findServer(List servers, String name) {
        List result = new ArrayList();
        for (int i = 0; i < servers.size(); i++) {
            AddeServer server = (AddeServer) servers.get(i);
            if (Misc.equals(server.getName(), name)) {
                return server;
            }
        }
        return null;
    }


    /**
     * Find the list of servers that have at least one group with the given type
     *
     * @param type group type
     * @param servers servers
     *
     * @return servers with group of type type
     */
    public static List getServersWithType(String type, List servers) {
        List result = new ArrayList();
        for (int i = 0; i < servers.size(); i++) {
            AddeServer server = (AddeServer) servers.get(i);
            if (Misc.equals(type, TYPE_ANY) || server.hasType(type)) {
                result.add(server);
            }
        }
        return result;
    }


    /**
     *  Set the IsLocal property.
     *
     *  @param value The new value for IsLocal
     */
    public void setIsLocal(boolean value) {
        isLocal = value;
    }

    /**
     *  Get the IsLocal property.
     *
     *  @return The IsLocal
     */
    public boolean getIsLocal() {
        return isLocal;
    }



    /**
     *  Set the Active property.
     *
     *  @param value The new value for Active
     */
    public void setActive(boolean value) {
        active = value;
    }

    /**
     *  Get the Active property.
     *
     *  @return The Active
     */
    public boolean getActive() {
        return active;
    }





    /**
     * Does this server have a group with the given type
     *
     * @param type group type
     *
     * @return has type
     */
    public boolean hasType(String type) {
        return getGroupsWithType(type).size() > 0;
    }


    /**
     * Add the group to me
     *
     * @param group new group
     */
    public void addGroup(Group group) {
        if ( !groups.contains(group)) {
            groups.add(group);
        }
    }

    /**
     * Add the list of groups
     *
     *
     * @param groupsToAdd list of groups
     */
    public void addGroups(List groupsToAdd) {
        for (int i = 0; i < groupsToAdd.size(); i++) {
            Group g = (Group) groupsToAdd.get(i);
            addGroup(g);
        }
    }

    /**
     *  Set the Groups property.
     *
     *  @param value The new value for Groups
     */
    public void setGroups(List value) {
        groups = value;
    }


    /**
     *  Get the Groups property.
     *
     *  @return The Groups
     */
    public List getGroups() {
        return groups;
    }

    /**
     * Find the group with the given name
     *
     * @param name group name
     *
     * @return The group with name or null
     */
    public Group findGroup(String name) {
        name = name.trim();
        for (int i = 0; i < groups.size(); i++) {
            Group g = (Group) groups.get(i);
            //            if (g.getName().toLowerCase().equals(name.toLowerCase())) {
            if (g.getName().equals(name)) {
                return g;
            }
        }
        return null;
    }


    /**
     * Get the groups with the given type
     *
     * @param type group type
     *
     * @return List of groups
     */
    public List getGroupsWithType(String type) {
        return getGroupsWithType(type, true);
    }

    /**
     * Get the groups with the given type
     *
     * @param type group type
     * @param onlyActive If true then only get the active groups
     *
     * @return List of groups
     */
    public List getGroupsWithType(String type, boolean onlyActive) {
        List result = new ArrayList();
        for (int i = 0; i < groups.size(); i++) {
            Group g = (Group) groups.get(i);
            if (onlyActive && !g.getActive()) {
                continue;
            }
            if ((type == null) || type.equals(TYPE_ANY)) {
                result.add(g);
            } else if (g.getType().equals(type)) {
                result.add(g);
            }
        }
        return result;
    }



    /**
     * equals
     *
     * @param o object
     *
     * @return equals
     */
    public boolean equals(Object o) {
        if ( !super.equals(o)) {
            return false;
        }
        if ( !(o instanceof AddeServer)) {
            return false;
        }
        AddeServer that = (AddeServer) o;
        return Misc.equals(this.groups, that.groups);
    }


    /**
     * Class Group represents an adde group
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.4 $
     */
    public static class Group extends NamedThing {


        /** Is this group a local group */
        private boolean isLocal = false;


        /** The type */
        private String type = TYPE_NONE;

        /** Is active */
        private boolean active = true;


        /**
         * ctor
         */
        public Group() {}


        /**
         * ctor
         *
         * @param type type
         * @param name name
         * @param desc description
         */
        public Group(String type, String name, String desc) {
            super(name, desc);
            this.type = type;
        }


        /**
         *  Set the Type property.
         *
         *  @param value The new value for Type
         */
        public void setType(String value) {
            type = value;
        }

        /**
         *  Get the Type property.
         *
         *  @return The Type
         */
        public String getType() {
            return type;
        }

        /**
         *  Set the IsLocal property.
         *
         *  @param value The new value for IsLocal
         */
        public void setIsLocal(boolean value) {
            isLocal = value;
        }

        /**
         *  Get the IsLocal property.
         *
         *  @return The IsLocal
         */
        public boolean getIsLocal() {
            return isLocal;
        }


        /**
         *  Set the Active property.
         *
         *  @param value The new value for Active
         */
        public void setActive(boolean value) {
            active = value;
        }

        /**
         *  Get the Active property.
         *
         *  @return The Active
         */
        public boolean getActive() {
            return active;
        }



        /**
         * equals
         *
         * @param o object
         *
         * @return equals
         */
        public boolean equals(Object o) {
            if ( !super.equals(o)) {
                return false;
            }
            if ( !(o instanceof Group)) {
                return false;
            }
            Group that = (Group) o;
            return Misc.equals(this.type, that.type);
        }

        /**
         * hash code
         *
         * @return hash code
         */
        public int hashCode() {
            return super.hashCode() ^ Misc.hashcode(type);
        }

        /**
         * to string
         *
         * @return string
         */
        public String toString() {
            String s = super.toString();
            //            s = s + "(" + active +")";
            if (isLocal) {
                //                s = s + " <local>";
            }
            return s;
        }

    }


    /**
     * test
     *
     * @param args args
     */
    public static void main(String[] args) {
        try {
            for (int i = 0; i < args.length; i++) {
                List servers = processXml(XmlUtil.getRoot(args[i],
                                   AddeServer.class));
                System.err.println(XmlUtil.toString(toXml(servers, false)));
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

}
