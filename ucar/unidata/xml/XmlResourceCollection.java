/*
 * $Id: XmlResourceCollection.java,v 1.27 2006/10/30 18:10:01 jeffmc Exp $
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



package ucar.unidata.xml;


import org.w3c.dom.Document;

import org.w3c.dom.Element;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ResourceCollection;

import java.io.FileOutputStream;



import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;




/**
 * An extension of {@link ucar.unidata.util.ResourceCollection} that handles xml documents.
 * @author Metapps development team
 * @version $Revision: 1.27 $ $Date: 2006/10/30 18:10:01 $
 */


public class XmlResourceCollection extends ResourceCollection {

    /** This is the xml document that is the xml that we can write into */
    private Document writableDocument;

    /** This is the root of the writable xml */
    private Element writableRoot;

    /**
     * A map of resource path to xml root element
     */
    private Hashtable roots = new Hashtable();


    /**
     *  Construct a new object with the given id (The id is simply a name, not one of the resources).
     *
     *  @param id The id or name of this collection.
     */
    public XmlResourceCollection(String id) {
        this(id, id);
    }

    /**
     * copy ctor
     *
     * @param id new id
     * @param that resourcecollection to copy from
     */
    public XmlResourceCollection(String id, XmlResourceCollection that) {
        super(id, that);
    }

    /**
     *  Construct a new object with the given id (The id is simply a name, not one of the resources)
     *  and description.
     *
     *  @param id The id or name of this collection.
     * @param description A human readable description of this collection
     */
    public XmlResourceCollection(String id, String description) {
        super(id, description);
    }

    /**
     *  Construct a new object with the given id and path to the user writable resource.
     *  The id is simply a name, not one of the resources.
     *
     *  @param id The id or name of this collection.
     *  @param writableResource The user writable resource.
     * @param resources
     */
    /*
    public XmlResourceCollection (String id) {
        this (id, writableResource, Misc.newList (writableResource));
        }*/


    /**
     *  Construct a new object with the given id and list of resource paths.
     *
     *  @param id The id or name of this collection.
     *  @param resources The list of resource paths.
     */

    public XmlResourceCollection(String id, List resources) {
        super(id, resources);
    }

    /**
     *  Construct a new object with the given id, user writable resource  and list of other resource paths.
     *
     *  @param id The id or name of this collection.
     *  @param writableResource The resource path that the user can write to.
     *  @param resources The list of resource paths.
     *  @deprecated not good anymore
     */
    public XmlResourceCollection(String id, String writableResource,
                                 List resources) {
        super(id, writableResource, resources);
    }




    /**
     * clear the cache
     */
    public void clearCache() {
        roots = new Hashtable();
        super.clearCache();
    }

    /**
     *  Create the xml document for user writable resource. If it does not
     *  exist then use the given dfltXml to create the document.
     *
     *  @param dfltXml The default xml text to use if the user's document does not exist.
     */
    private void createWritableXml(String dfltXml) {
        String writableXml = null;
        try {
            writableXml = read(writableIndex);
            if (writableXml == null) {
                writableXml = dfltXml;
            }
            writableDocument = XmlUtil.getDocument(writableXml);
            writableRoot     = writableDocument.getDocumentElement();
            if (writableIndex >= 0) {
                roots.put(get(writableIndex), writableRoot);
            }
        } catch (RuntimeException npe) {
            throw npe;
        } catch (Exception exc) {
            throw new IllegalArgumentException(
                "Error parsing resource xml file:" + writableResource + "\n"
                + exc);
        }
    }



    /**
     *  Write the given contents into the writable resource file. This overwrites the
     * base class method to reset the writable xml document and root.
     *
     * @param contents
     *
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public void writeWritableResource(String contents)
            throws java.io.FileNotFoundException, java.io.IOException {
        super.writeWritableResource(contents);
        writableDocument = null;
        writableRoot     = null;
        if (writableIndex >= 0) {
            roots.remove(get(writableIndex));
        }
    }



    /**
     *  Write out the user's writable xml into the writable resource path.
     *
     *  @throws java.io.FileNotFoundException When the write fails.
     *  @throws java.io.IOException When the write fails.
     */
    public void writeWritable()
            throws java.io.FileNotFoundException, java.io.IOException {
        if (writableDocument == null) {
            throw new IllegalStateException("No writable document");
        }
        writeWritableResource(XmlUtil.toStringWithHeader(writableRoot));
    }


    /**
     *  Create (if not created) and return the root of the user's writable xml.
     *
     *  @param dfltXml If the user's document does not exist then use this xml to create the document.
     *  @return The root of the user's writable xml document.
     */
    public Element getWritableRoot(String dfltXml) {
        if (writableRoot == null) {
            createWritableXml(dfltXml);
        }
        return writableRoot;
    }

    /**
     *  Return the Document which is the user's writable xml document.
     *
     *  @param dfltXml If the user's document does not exist then use this xml to create the document.
     *  @return The  writable xml document.
     */
    public Document getWritableDocument(String dfltXml) {
        if (writableRoot == null) {
            createWritableXml(dfltXml);
        }
        return writableDocument;
    }


    /**
     *  Set the document and root element which represent the user's writable xml document.
     *
     *  @param d The new document.
     *  @param root The root of the writable xml.
     */
    public void setWritableDocument(Document d, Element root) {
        this.writableRoot     = root;
        this.writableDocument = d;
        if (writableIndex >= 0) {
            roots.put(get(writableIndex), writableRoot);
        }
    }


    /**
     * Remove the writable resource from the list
     */
    public void removeWritable() {
        roots.remove(get(writableIndex));
        super.removeWritable();
        writableDocument = null;
        writableRoot     = null;
    }



    /**
     *  Create a new xml document with the given xml String and resource index.
     *
     *  @param xml The xml String to use.
     *  @param resourceIndex Which resource is this xml from.
     * @param lookAtCache SHould we check the cache first
     *  @return The root of the given xml.
     */
    private Element getRoot(String xml, int resourceIndex,
                            boolean lookAtCache) {
        try {
            Element root = (Element) (lookAtCache
                                      ? roots.get(get(resourceIndex))
                                      : null);
            if (root == null) {
                Document doc = XmlUtil.getDocument(xml);
                root = doc.getDocumentElement();
                roots.put(get(resourceIndex), root);
                if (resourceIndex == writableIndex) {
                    setWritableDocument(doc, root);
                }
            }
            return root;
        } catch (Exception exc) {
            //For now log the exception and return null
            //Check if we got html, like from a file not found, etc.
            if (Misc.isHtml(xml)) {
                return null;
            }
            LogUtil.logException("Error: parsing resource xml file:"
                                 + get(resourceIndex), exc);
            return null;
            /*            throw new IllegalArgumentException(
                "Error: parsing resource xml file:" + get(resourceIndex)
                + "\n" + exc);*/
        }
    }


    /**
     *  Create (if not already) and return the xml Element root of the xml resource
     *  at the given index.
     *
     *  @param resourceIndex Which resource.
     *  @return The root Element.
     */
    public Element getRoot(int resourceIndex) {
        return getRoot(resourceIndex, true);
    }

    /**
     *  Create (if not already) and return the xml Element root of the xml resource
     *  at the given index.
     *
     *  @param resourceIndex Which resource.
     * @param lookAtCache SHould we check the cache first
     *  @return The root Element.
     */
    public Element getRoot(int resourceIndex, boolean lookAtCache) {
        if (lookAtCache) {
            Element root = (Element) roots.get(get(resourceIndex));
            if (root != null) {
                return root;
            }
        }
        String xml = read(resourceIndex, lookAtCache);
        if (xml == null) {
            return null;
        }
        xml = xml.trim();
        if (xml.length() == 0) {
            return null;
        }
        return getRoot(xml, resourceIndex, lookAtCache);
    }



}

