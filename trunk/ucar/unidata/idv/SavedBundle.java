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

package ucar.unidata.idv;


import org.w3c.dom.Document;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ucar.unidata.util.IOUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;


import ucar.unidata.xml.XmlUtil;


import java.io.File;

import java.util.ArrayList;
import java.util.List;


/**
 * An object to handle a saved bundle.
 *
 * @author IDV development team
 * @version $Revision
 */
public class SavedBundle {

    /** The type  to specify the "Favorites" bundles */
    public static final int TYPE_FAVORITE = 0;

    /** The type to specify the display templates */
    public static final int TYPE_DISPLAY = 1;

    /** The type to specify the data */
    public static final int TYPE_DATA = 2;


    /** type of saved bundle */
    public static final String VALUE_FAVORITE = "favorite";

    /** type of saved bundle */
    public static final String VALUE_DATA = "data";

    /** type of saved bundle */
    public static final String VALUE_DISPLAY = "display";

    /** Xml tag name for the bundles element */
    public static final String TAG_BUNDLES = "bundles";

    /** Xml tag name for the bundle element */
    public static final String TAG_BUNDLE = "bundle";

    /** Xml attribute name for the url */
    public static final String ATTR_URL = "url";

    /** Xml attribute name for the name */
    public static final String ATTR_NAME = "name";

    /** Xml attribute name for the category */
    public static final String ATTR_CATEGORY = "category";

    /** xml attr */
    public static final String ATTR_TYPE = "type";

    /** The type */
    private int type = TYPE_FAVORITE;

    /** The full url of the bundle */
    private String url;

    /** The name of the bundle */
    private String name;

    /** The category of the bundle */
    private List categories;

    /**
     * Some bundles (like the display templates) get instantiated as  a
     * prototype
     */
    private Object prototype;


    /** Is it a local bundle */
    private boolean local = false;

    /** prefix_ */
    private String uniquePrefix;

    /**
     * Create the saved bundle object
     *
     * @param node The xml node that defines this object
     * @param dirRoot The directory root
     * @param resourceManager Reference to the resource manager so we add
     * the sitepath, etc., macros into the url
     * @param local Is it a local file
     */
    public SavedBundle(Element node, String dirRoot,
                       IdvResourceManager resourceManager, boolean local) {

        this.type  = type;
        this.local = local;
        String typeString = XmlUtil.getAttribute(node, ATTR_TYPE, "favorite");
        if (typeString.equals(VALUE_FAVORITE)) {
            type = TYPE_FAVORITE;
        } else if (typeString.equals(VALUE_DATA)) {
            type = TYPE_DATA;
        } else if (typeString.equals(VALUE_DISPLAY)) {
            type = TYPE_DISPLAY;
        }


        this.url = resourceManager.getResourcePath(XmlUtil.getAttribute(node,
                ATTR_URL));


        if ( !url.startsWith("/") && !url.startsWith("http:")
                && !url.startsWith("ftp:")) {
            url = dirRoot + "/" + url;
        }
        this.name = XmlUtil.getAttribute(node, ATTR_NAME);

        this.categories = IdvPersistenceManager.stringToCategories(
            XmlUtil.getAttribute(node, ATTR_CATEGORY));
    }


    /**
     * Create the saved bundle object
     *
     * @param url The url
     * @param name The name
     * @param categories List of (String) categories.
     */
    public SavedBundle(String url, String name, List categories) {
        this(url, name, categories, false);
    }

    /**
     * ctor
     *
     * @param url the url path
     * @param name name
     * @param categories categories
     * @param local is it a local file
     */
    public SavedBundle(String url, String name, List categories,
                       boolean local) {
        this(url, name, categories, null, local);
    }


    /**
     * Create the saved bundle object
     *
     * @param url The url
     * @param name The name
     * @param categories List of (String) categories.
     * @param prototype The object this bundle represents - may be null.
     * @param local is it a local file
     */
    public SavedBundle(String url, String name, List categories,
                       Object prototype, boolean local) {
        this(url, name, categories, prototype, local, TYPE_FAVORITE);
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param name _more_
     * @param categories _more_
     * @param prototype _more_
     * @param local _more_
     * @param type _more_
     */
    public SavedBundle(String url, String name, List categories,
                       Object prototype, boolean local, int type) {
        this.url        = url;
        this.name       = name;
        this.categories = new ArrayList(categories);
        this.prototype  = prototype;
        this.local      = local;
        this.type       = type;
    }

    /**
     * Write out the xml that defines this SavedBundle. Note: this
     * writes out the url as just the file name, not the full path
     *
     * @param doc The document to create the xml with
     * @param root The xml root
     */
    public void toXml(Document doc, Element root) {
        toXml(doc, root, false);
    }


    /**
     * Write out the xml that defines this SavedBundle. Note: this
     * writes out the url as just the file name, not the full path
     *
     * @param doc The document to create the xml with
     * @param root The xml root
     * @param includeCategoryInUrl Should the category be included in the url
     */
    public void toXml(Document doc, Element root,
                      boolean includeCategoryInUrl) {
        Element node = doc.createElement(TAG_BUNDLE);
        node.setAttribute(
            ATTR_CATEGORY,
            IdvPersistenceManager.categoriesToString(categories));
        node.setAttribute(ATTR_NAME, name);
        File    f     = new File(url);
        boolean isUrl = !f.exists();
        if (isUrl) {
            node.setAttribute(ATTR_URL, url);
        } else {
            if (includeCategoryInUrl) {
                node.setAttribute(ATTR_URL, getCategorizedName());
            } else {
                node.setAttribute(ATTR_URL, IOUtil.getFileTail(url));
            }
        }
        if (type == TYPE_DATA) {
            node.setAttribute(ATTR_TYPE, VALUE_DATA);
        } else if (type == TYPE_DISPLAY) {
            node.setAttribute(ATTR_TYPE, VALUE_DISPLAY);
        } else {
            node.setAttribute(ATTR_TYPE, VALUE_FAVORITE);
        }
        root.appendChild(node);
    }



    /**
     * set the unique prefix
     *
     * @param p prefix
     */
    protected void setUniquePrefix(String p) {
        uniquePrefix = p;
    }

    /**
     * Get the name to use with the categories as a prefix
     *
     * @return categorized name
     */
    public String getCategorizedName() {
        String catString = StringUtil.join("_", categories);
        if (uniquePrefix != null) {
            catString = uniquePrefix + catString;
        }
        return catString + IOUtil.getFileTail(url);
    }

    /**
     * Utility that created a list of SavedBundle objects
     * from the document at the given xml path
     *
     * @param root Root of the bundles xml doc.
     * @param dirRoot The directory root
     * @param resourceManager The resource manager
     * @param local is it a local file
     *
     * @return List of SavedBundle objects
     */
    public static List<SavedBundle> processBundleXml(Element root,
            String dirRoot, IdvResourceManager resourceManager,
            boolean local) {
        List<SavedBundle> bundles = new ArrayList<SavedBundle>();
        if (root == null) {
            return bundles;
        }
        List bundleNodes = XmlUtil.findChildren(root, TAG_BUNDLE);
        for (int i = 0; i < bundleNodes.size(); i++) {
            Element node = (Element) bundleNodes.get(i);
            bundles.add(new SavedBundle(node, dirRoot, resourceManager,
                                        local));
        }
        return bundles;
    }


    /**
     *  Set the Url property.
     *
     *  @param value The new value for Url
     */
    public void setUrl(String value) {
        url = value;
    }

    /**
     *  Get the Url property.
     *
     *  @return The Url
     */
    public String getUrl() {
        return url;
    }

    /**
     *  Set the Name property.
     *
     *  @param value The new value for Name
     */
    public void setName(String value) {
        name = value;
    }

    /**
     *  Get the Name property.
     *
     *  @return The Name
     */
    public String getName() {
        return name;
    }


    /**
     *  Set the Category property.
     *
     *  @param value The new value for Category
     */
    public void setCategories(List value) {
        categories = value;
    }

    /**
     *  Get the Category property.
     *
     *  @return The Category
     */
    public List getCategories() {
        return categories;
    }


    /**
     * Get the prototype - may be null.
     *
     * @return The prototype
     */
    public Object getPrototype() {
        return prototype;
    }


    /**
     * Full label
     *
     * @return The name.
     */
    public String getLabel() {
        return IdvPersistenceManager.categoriesToString(categories)
               + IdvPersistenceManager.CATEGORY_SEPARATOR + name;
    }


    /**
     * Override toString.
     *
     * @return The name.
     */
    public String toString() {
        return name;
    }

    /**
     * Set the Type property.
     *
     * @param value The new value for Type
     */
    public void setType(int value) {
        type = value;
    }

    /**
     * Get the Type property.
     *
     * @return The Type
     */
    public int getType() {
        return type;
    }


    /**
     * Set the Local property.
     *
     * @param value The new value for Local
     */
    public void setLocal(boolean value) {
        local = value;
    }

    /**
     * Get the Local property.
     *
     * @return The Local
     */
    public boolean getLocal() {
        return local;
    }


}
