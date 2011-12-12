/*
 * $Id: DataGroup.java,v 1.21 2007/06/13 16:59:16 jeffmc Exp $
 *
 * Copyright 1997-2004 Unidata Program Center/University Corporation for
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

package ucar.unidata.data;



import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.xml.XmlEncoder;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * A generic base class that represents a source of data.
 *
 * @author Jeff McWhirter
 * @version $Revision: 1.21 $
 */
public class DataGroup {

    /** logging category */
    static ucar.unidata.util.LogUtil.LogCategory log_ =
        ucar.unidata.util.LogUtil.getLogInstance(DataGroup.class.getName());

    /** xml things */
    private static final String TAG_PARAMGROUPS = "paramgroups";

    /** Parameter group XML tag */
    private static final String TAG_PARAMGROUP = "paramgroup";

    /** Parameters XML tag */
    private static final String TAG_PARAMS = "params";

    /** Name attribute for tag */
    private static final String ATTR_NAME = "name";

    /** xml things */
    private static final String ATTR_DESCRIPTION = "description";

    /** Parameter prefix attribute */
    private static final String ATTR_PARAMPREFIX = "p";





    /** List of groups */
    private static List groups;

    /** map of groups */
    private static Hashtable groupMap;

    /** name of the group */
    private String name;

    /** the desc */
    private String description = "";

    /** parameter sets */
    private List paramSets = new ArrayList();

    /** The bean ctor */
    public DataGroup() {}


    /**
     * Create a new DataGroup with the specified name
     *
     * @param groupName    name of group
     */
    public DataGroup(String groupName) {
        this.name = groupName;
    }

    /**
     * copy ctor
     *
     * @param that that
     */
    public DataGroup(DataGroup that) {
        this.name = that.name;
        if (that.paramSets != null) {
            for (int i = 0; i < that.paramSets.size(); i++) {
                List tmp = new ArrayList((List) that.paramSets.get(i));
                this.paramSets.add(tmp);
            }
        }
    }

    /**
     * Add a parameter set
     *
     * @param l   parameter set
     */
    private void addParamSet(List l) {
        if ( !paramSets.contains(l)) {
            paramSets.add(l);
        }
    }


    /**
     * merge the given param sets into ours
     *
     * @param that the data group to merge with
     */
    private void add(DataGroup that) {
        for (int i = 0; i < that.paramSets.size(); i++) {
            List params = (List) paramSets.get(i);
            addParamSet(params);
        }
    }


    /**
     * Get the DataGroup of the specified name
     *
     * @param groupName   name of group
     * @return  DataGroup for that name (or null if it does not exist).
     */
    public static DataGroup getDataGroup(String groupName) {
        return (DataGroup) groupMap.get(groupName);
    }

    /**
     * Get the list of DataGroups
     *
     * @return list of DataGroups
     */
    public static List getGroups() {
        return groups;
    }

    /**
     * Set the Name property.
     *
     * @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the Name property.
     *
     * @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     * Get the parameter set
     *
     * @return  List of parameters
     */
    public List getParamSets() {
        return paramSets;
    }


    /**
     * Set the list of param sets
     *
     * @param l list
     */
    public void setParamSets(List l) {
        paramSets = l;
    }



    /**
     * Initialize from XML.
     *
     * @param xrc  resource collection
     */
    public static void init(XmlResourceCollection xrc) {
        init(xrc, false);
    }


    /**
     * Initialize from XML.
     *
     * @param xrc  resource collection
     * @param andClear If true then clear any current state and reinitialize
     */
    public static void init(XmlResourceCollection xrc, boolean andClear) {
        xrc.clearCache();
        if (andClear || (groups == null)) {
            groups   = new ArrayList();
            groupMap = new Hashtable();
        }
        try {
            for (int i = 0; i < xrc.size(); i++) {
                Element root = xrc.getRoot(i);
                if (root == null) {
                    continue;
                }
                groups.addAll(readGroups(root, groupMap, true));
            }
        } catch (Exception exc) {
            LogUtil.printException(log_, "Initializing DataGroup", exc);
            return;
        }
    }


    /**
     * Create an xml element that represents this data group
     *
     * @param doc doc to use
     *
     * @return the element
     */
    public Element getElement(Document doc) {
        Element node = doc.createElement(TAG_PARAMGROUP);
        /*          <paramgroup name="u_and_v">    <params
                    p1="U"
                    p2="V"/>*/
        node.setAttribute(ATTR_NAME, getName());
        if (description != null) {
            node.setAttribute(ATTR_DESCRIPTION, getDescription());
        }
        for (int i = 0; i < paramSets.size(); i++) {
            List    params = (List) paramSets.get(i);
            Element child  = doc.createElement(TAG_PARAMS);
            node.appendChild(child);
            for (int j = 0; j < params.size(); j++) {
                child.setAttribute("p" + (j + 1), params.get(j).toString());
            }
        }
        return node;
    }



    /**
     * tostring
     *
     * @return tostring
     */
    public String toString() {
        if ((description != null) && (description.length() > 0)) {
            return description;
        }
        return name;
    }



    /**
     * Construct the groups from XML.
     *
     * @param root  root XML element.
     * @param groupMap group
     * @param merge Try to merge the new DataGroups into the ones defined in groupMap
     *
     * @return List of groups
     */
    public static List readGroups(Element root, Hashtable groupMap,
                                  boolean merge) {
        //It might be an xmlencoded list of DataGroup objects
        List groups = new ArrayList();
        if (root.getTagName().equals("object")) {
            Object o = new XmlEncoder().toObject(root);
            if (o == null) {
                return groups;
            }
            if (o instanceof List) {
                List l = (List) o;
                for (int i = 0; i < l.size(); i++) {
                    DataGroup dataGroup = (DataGroup) l.get(i);
                    if (merge) {
                        DataGroup other =
                            (DataGroup) groupMap.get(dataGroup.getName());
                        if (other != null) {
                            other.add(dataGroup);
                            continue;
                        }
                    }
                    groupMap.put(dataGroup.getName(), dataGroup);
                    groups.add(dataGroup);
                }
            }
            return groups;
        }
        List children = XmlUtil.findChildren(root, TAG_PARAMGROUP);
        for (int i = 0; i < children.size(); i++) {
            Element child     = (Element) children.get(i);
            String  groupName = XmlUtil.getAttribute(child, ATTR_NAME);
            String  desc = XmlUtil.getAttribute(child, ATTR_DESCRIPTION, "");
            //      System.err.println ("Group:" + groupName);
            DataGroup group = null;
            if (merge) {
                group = (DataGroup) groupMap.get(groupName);
            }
            if (group == null) {
                group = new DataGroup(groupName);
                group.setDescription(desc);
                groupMap.put(groupName, group);
                groups.add(group);
            }
            List grandChildren = XmlUtil.findChildren(child, TAG_PARAMS);
            for (int j = 0; j < grandChildren.size(); j++) {
                int       paramIdx   = 1;
                Element   grandchild = (Element) grandChildren.get(j);
                String    param;
                ArrayList paramList = new ArrayList();
                while ((param = XmlUtil.getAttribute(grandchild,
                        ATTR_PARAMPREFIX + (paramIdx++),
                        (String) null)) != null) {
                    paramList.add(param);
                }
                group.addParamSet(paramList);
            }
        }
        return groups;
    }

    /**
     * Set the Description property.
     *
     * @param value The new value for Description
     */
    public void setDescription(String value) {
        description = value;
    }

    /**
     * Get the Description property.
     *
     * @return The Description
     */
    public String getDescription() {
        return description;
    }




}

