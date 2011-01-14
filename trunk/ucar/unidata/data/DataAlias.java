/*
 * $Id: DataAlias.java,v 1.39 2006/12/01 20:41:20 jeffmc Exp $
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


import org.w3c.dom.Element;


import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;

import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;



/**
 * This class manages a set of data aliases - a mapping between
 * canonical names and a list of aliases for the canonical name.
 * This class is not instantiated - rather it provides a set of static
 * methods.
 *
 * It is initialized by the IDV with an
 * {@link ucar.unidata.xml.XmlResourceCollection}
 * that holds a set of aliases.xml files. The xml looks like:
 * <pre>
 * &lt;aliases&gt;
 * &lt;alias name="TEMP"     label="Temperature"   aliases="T,t,Temperature,tdry" /&gt;
 * ...
 * &lt;/aliases&gt;
 * &lt;/pre&gt;
 * Each alias tag holds a name (the canonical name), a label and a comma
 * separated list of aliases for the name.
 *
 *
 * @author IDV development team
 * @version $Revision: 1.39 $
 */

public class DataAlias implements Comparable {

    /** Xml tag for canonical name */
    public static final String TAG_ALIAS = "alias";

    /** Xml tag for aliases */
    public static final String TAG_ALIASES = "aliases";

    /** Xml attribute for aliases */
    public static final String ATTR_ALIASES = "aliases";

    /** Xml attribute for name */
    public static final String ATTR_NAME = "name";

    /** Xml attribute for label */
    public static final String ATTR_LABEL = "label";


    /** name of this alias */
    private String name;

    /** text for a label */
    private String label;

    /** list of aliases */
    private List aliases = new ArrayList();


    /**
     * Construct a new DataAlias with the given name and label.
     * @param  name  name of this <code>DataAlias</code>
     * @param  label  label of this <code>DataAlias</code>.  Used for displaying
     *                a "nice" string for this DataAlias.
     */
    public DataAlias(String name, String label) {
        this(name, label, null);
    }

    /**
     * Trusted constructor with the given name and label and
     * initial list of aliases.
     * @param  name  name of this <code>DataAlias</code>
     * @param  label  label of this <code>DataAlias</code>.  Used for displaying
     *                a "nice" string for this DataAlias.
     * @param newAliases  list of new aliases for the canonical name
     */
    private DataAlias(String name, String label, List newAliases) {
        this.name  = name;
        this.label = label;
        addAliases(newAliases);
    }

    /**
     * Add an alias for this object.
     *
     * @param alias   alias to add.
     */
    protected void addAlias(String alias) {
        if (alias == null) {
            return;
        }
        //If the alias is a regexp then add it into the special list
        if (StringUtil.containsRegExp(alias)) {
            regExpAliases.add(alias);
            regExpObjects.add(this);
        }

        if ( !aliases.contains(alias)) {
            aliases.add(alias);
            aliasToObject.put(alias, this);
        }
    }

    /**
     * Add alist of aliases to this <code>DataAlias</code>
     * @param l  <code>List</code> of names (<code>String</code>-s)
     */
    protected void addAliases(List l) {
        if (l == null) {
            return;
        }
        for (int j = 0; j < l.size(); j++) {
            addAlias((String) l.get(j));
        }
    }

    /**
     * Override of hashCode method.
     * @return  hashCode
     */
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * See if <code>o</code> is equal to this.
     *
     * @param o   object in question
     * @return true if <code>o</code> is a DataAlias and they have the same
     *         name.
     */
    public boolean equals(Object o) {
        if ( !(o instanceof DataAlias)) {
            return false;
        }
        return name.equals(((DataAlias) o).getName());
    }


    /**
     * Set the name of this <code>DataAlias</code>
     * @param value  new name.
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Get the name of this <code>DataAlias</code>
     * @return name of this.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the label of this <code>DataAlias</code>
     * @param value  new label.
     */
    public void setLabel(String value) {
        label = value;
    }

    /**
     * Get the label of this <code>DataAlias</code>
     * @return label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get aliases for the name of this <code>DataAlias</code>
     * @return <code>List</code> of names.
     */
    public List getAliases() {
        return aliases;
    }

    /**
     *  A list of the canonical names
     */
    private static List dataAliasList;

    /**
     *  A list of the alias names that are regular expressions.
     */
    private static List regExpAliases;

    /**
     *  A list of DataAlias objects that correspond to the regular exp aliases.
     */
    private static List regExpObjects;


    /**
     *  A list of the TwoFacedObjects holding id/label pairs
     */
    private static List labelIdList;


    /**
     *  A mapping from canonical name to list if aliases
     */
    private static Hashtable canonicalToObject;

    /**
     *  A mapping from an alias to the DataAlias object
     */
    private static Hashtable aliasToObject;


    /**
     * Process the given xml and return a list of DataAlias objects
     * defined within the xml.
     *
     * @param root  root element of the XML
     *
     * @return A list of DataAlias objects
     */
    public static List createDataAliases(Element root) {
        List aliases = new ArrayList();
        if (root == null) {
            return aliases;
        }
        List children = XmlUtil.findChildren(root, TAG_ALIAS);
        for (int i = 0; i < children.size(); i++) {
            Element child     = (Element) children.get(i);
            String  canonical = XmlUtil.getAttribute(child, ATTR_NAME);
            String  label = XmlUtil.getAttribute(child, ATTR_LABEL,
                                canonical);
            List aliasList = StringUtil.split(XmlUtil.getAttribute(child,
                                 TAG_ALIASES));
            aliases.add(new DataAlias(canonical, label, aliasList));
        }
        return aliases;
    }


    /**
     * _more_
     *
     * @param o _more_
     *
     * @return _more_
     */
    public int compareTo(Object o) {
        if ( !(o instanceof DataAlias)) {
            return 0;
        }
        DataAlias that = (DataAlias) o;
        return this.name.compareTo(that.name);
    }



    /**
     * Process the given xml. Read all of the alias tags,
     * parse and create the list of aliases from the comma
     * separated aliases attribute, build up the data structures.
     *
     * @param root  root element of the XML
     */
    private static void readAliases(Element root) {
        List children = XmlUtil.findChildren(root, TAG_ALIAS);
        for (int i = 0; i < children.size(); i++) {
            Element child     = (Element) children.get(i);
            String  canonical = XmlUtil.getAttribute(child, ATTR_NAME);
            String  label = XmlUtil.getAttribute(child, ATTR_LABEL,
                                canonical);
            List aliasList = StringUtil.split(XmlUtil.getAttribute(child,
                                 TAG_ALIASES));
            DataAlias dataAlias =
                (DataAlias) canonicalToObject.get(canonical);
            //            if (dataAlias != null) {
            //                continue;
            //            }


            if (dataAlias == null) {
                //System.err.println ("Creating new DA:" + canonical+  " list=" + aliasList);
                dataAlias = new DataAlias(canonical, label);
                canonicalToObject.put(canonical, dataAlias);
                dataAliasList.add(dataAlias);
                labelIdList.add(new TwoFacedObject(label + " (" + canonical
                        + ")", canonical));
            } else {
                //                System.err.println ("Already have DA:" + canonical+  " list=" + aliasList);

            }
            dataAlias.addAliases(aliasList);
        }
    }



    /**
     * Reinitialize the data structures  and reprocess
     * the xml. This is called by the {@link ucar.unidata.idv.ui.AliasEditor}
     * when aliases have been changed.
     *
     * @param resources  collection of XML resources
     */
    public static void reInit(XmlResourceCollection resources) {
        makeStructures(true);
        init(resources);
    }

    /**
     * Make all the structures for holding the <code>DataAlias</code>-s.
     *
     * @param force  true to force a recreation of the structures
     */
    private static void makeStructures(boolean force) {
        if ( !force && (canonicalToObject != null)) {
            return;
        }
        canonicalToObject = new Hashtable();
        aliasToObject     = new Hashtable();
        dataAliasList     = new ArrayList();
        labelIdList       = new ArrayList();
        regExpObjects     = new ArrayList();
        regExpAliases     = new ArrayList();
    }

    /**
     * Initialize the DataAlias-es with the given collection
     * of xml resources, {@link ucar.unidata.xml.XmlResourceCollection}
     *
     * @param resources  collection of XML resources
     */
    public static void init(XmlResourceCollection resources) {
        makeStructures(false);
        for (int i = 0; i < resources.size(); i++) {
            Element root = resources.getRoot(i, false);
            if (root != null) {
                readAliases(root);
            }
        }
    }


    /**
     * Return the list of canonical names (<code>String</code>-s)
     * @return  <code>List</code> of names
     */
    public static List getDataAliasList() {
        makeStructures(false);
        return new ArrayList(dataAliasList);
    }

    /**
     * Return the list of name/label pairs (<code>TwoFacedObject</code>-s)
     * @return  <code>List</code> of names
     */
    public static List getLabelIdList() {
        makeStructures(false);
        return new ArrayList(labelIdList);
    }


    /**
     * Return the canonical name for the given alias.
     * @param paramName  alias to look for
     * @return  name of <code>DataAlias</code> corresponding to
     *          <code>name</code> or <code>null</code> if no match is found.
     */
    public static String aliasToCanonical(String paramName) {
        if (paramName == null) {
            return null;
        }
        makeStructures(false);
        DataAlias dataAlias = (DataAlias) aliasToObject.get(paramName);
        //Look for regexp
        if (dataAlias == null) {
            //            System.err.println("name:" + paramName);
            dataAlias = (DataAlias) StringUtil.findMatch(paramName,
                    regExpAliases, regExpObjects, null);
            if (dataAlias != null) {
                aliasToObject.put(paramName, dataAlias);
            }

        }
        if (dataAlias != null) {
            //Check for "!"
            if (dataAlias.aliases.contains("!" + paramName)) {
                return null;
            }
            return dataAlias.getName();
        }

        return null;
    }


    /**
     * Return the list of aliases (Strings) for the given canonical.
     * @param  paramName  canonical name (name) to look for
     * @return  <code>List</code> of aliases for the canonical name.
     */
    public static List getAliasesOf(String paramName) {
        if (paramName == null) {
            return null;
        }
        makeStructures(false);
        DataAlias dataAlias = (DataAlias) canonicalToObject.get(paramName);
        if (dataAlias == null) {
            return null;
        }
        return dataAlias.getAliases();
    }


    /**
     * Return the DataAlias object the given canonical name.
     * @param paramName  alias to look for
     * @return  The  <code>DataAlias</code> corresponding to
     *          <code>name</code> or <code>null</code> if no match is found.
     */
    public static DataAlias findAlias(String paramName) {
        if (paramName == null) {
            return null;
        }
        makeStructures(false);
        return (DataAlias) aliasToObject.get(paramName);
    }

    /**
     * Returns a string representation of this DataAlias.
     * @return a string representation of the DataAlias.
     */
    public String toString() {
        return "Name=" + name + " aliases=" + aliases;
    }


}

