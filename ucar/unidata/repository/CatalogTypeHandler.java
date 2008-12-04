/**
 * $Id: TrackDataSource.java,v 1.90 2007/08/06 17:02:27 jeffmc Exp $
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

import ucar.unidata.sql.Clause;


import ucar.unidata.sql.SqlUtil;

import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.TwoFacedObject;
import ucar.unidata.xml.XmlUtil;

import java.io.File;

import java.net.URL;

import java.sql.PreparedStatement;

import java.sql.ResultSet;
import java.sql.Statement;




import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CatalogTypeHandler extends GenericTypeHandler {


    /** _more_          */
    static Hashtable<String,DomHolder> domCache = new Hashtable();

    /** _more_          */
    private Hashtable childIdToParent = new Hashtable();



    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public CatalogTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
    }


    public String getIconUrl(Entry entry) {
        if (entry.isGroup()) {
            return fileUrl(ICON_FOLDER_CLOSED);
        }
        return super.getIconUrl(entry);
    }


    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    private String[] parseId(String id) {
        if (id.startsWith("catalog:")) {
            id = id.substring("catalog:".length());
        }
        if (id.startsWith("b64:")) {
            id = id.substring("b64:".length());
            id = new String(XmlUtil.decodeBase64(id));
        }

        int idx = id.indexOf(":id:");
        if (idx < 0) {
            return new String[] { id, null };
        }
        String[] toks = new String[] { id.substring(0, idx),
                                       id.substring(idx + 4) };
        return toks;
    }


    /**
     * _more_
     *
     * @param parent _more_
     * @param ids _more_
     */
    private void walkTree(Element parent, Hashtable ids) {
        NodeList elements = XmlUtil.getElements(parent);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if ( !child.getTagName().equals(
                    CatalogOutputHandler.TAG_DATASET)) {
                continue;
            }
            ids.put(getId(child), child);
            walkTree(child, ids);
        }
    }

    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     */
    private String getId(Element node) {
        String id = XmlUtil.getAttribute(node, ATTR_ID, (String) null);
        if (id == null) {
            id = getNamePath(node);
        }
        return id;
    }

    /**
     * _more_
     *
     * @param node _more_
     *
     * @return _more_
     */
    private String getNamePath(Node node) {
        if (node == null) {
            return null;
        }
        Node parent = node.getParentNode();
        if ( !(parent instanceof Element)) {
            return null;
        }
        String parentId = getNamePath(parent);
        String name     = XmlUtil.getAttribute(node, ATTR_NAME, "");
        if (parentId == null) {
            return name;
        }
        return parentId + "::" + name;
    }




    /**
     * _more_
     *
     * @param url _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private Element getDom(String url) throws Exception {
        Element root = null;
        DomHolder holder = domCache.get(url);
        if(holder!=null) {
            if(holder.isValid()) {
                root = holder.root;
            } else {
                domCache.remove(url);
            }
        }

        if (root == null) {
            root = XmlUtil.getRoot(url, getClass());
            domCache.put(url, new DomHolder(root));
        }
        return root;
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public static String getCatalogId(String id) {
        id = XmlUtil.encodeBase64(id.getBytes());
        return "catalog:b64:" + id;
    }

    /**
     * _more_
     *
     * @param url _more_
     * @param subid _more_
     *
     * @return _more_
     */
    public static  String getId(String url, String subid) {
        if (subid == null) {
            return getCatalogId(url);
        }
        return getCatalogId(url + ":id:" + subid);
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public List<String> getSynthIds(Request request, Group parentEntry,
                                    String id)
            throws Exception {
        if(id == null) id = parentEntry.getId();
        String[]     loc        = parseId(id);
        String       catalogUrl = request.getString(ARG_CATALOG, null);
        List<String> ids        = new ArrayList<String>();
        String       url        = loc[0];
        String       nodeId     = loc[1];
        if(!id.startsWith("catalog:")) {
            url = parentEntry.getResource().getPath();
            nodeId = null;
        }
        URL          baseUrl    = new URL(url);

        Element      root       = getDom(url);
        if (root == null) {
            throw new IllegalArgumentException("Could not load catalog:"
                    + url);
        }
        Element dataset = (Element) XmlUtil.findChild(root,
                              CatalogOutputHandler.TAG_DATASET);
        if (dataset != null) {
            root = dataset;
        }

        String    parentId = getId(url, nodeId);

        Hashtable idMap    = new Hashtable();
        walkTree(dataset, idMap);
        if (nodeId != null) {
            dataset = (Element) idMap.get(nodeId);
        }
        if (dataset == null) {
            throw new IllegalArgumentException("Could not find dataset:"
                    + nodeId + " in catalog:" + url);
        }


        NodeList elements = XmlUtil.getElements(dataset);
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if (child.getTagName().equals(CatalogOutputHandler.TAG_DATASET)) {
                String datasetId = getId(child);
                String entryId   = getCatalogId(url + ":id:" + datasetId);
                childIdToParent.put(entryId, parentId);
                ids.add(entryId);
            } else if (child.getTagName().equals(
                    CatalogUtil.TAG_CATALOGREF)) {
                String href = XmlUtil.getAttribute(child,
                                  CatalogUtil.ATTR_XLINK_HREF);
                String catUrl    = new URL(baseUrl, href).toString();
                String datasetId = getCatalogId(catUrl);
                childIdToParent.put(datasetId, parentId);
                ids.add(datasetId);
            }
        }
        return ids;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isSynthType() {
        return true;
    }



    public static class DomHolder {
        Element root;
        Date dttm;
        public DomHolder(Element root) {
            this.root = root;
            dttm  = new Date();
        }
        public boolean isValid() {
            Date now = new Date();
            //Only keep around catalogs for 5 minutes
            if((now.getTime()-dttm.getTime())>1000*60*5) return false;
            return true;
        }
    }



    /**
     * _more_
     *
     * @param request _more_
     * @param parentEntry _more_
     * @param id _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Entry makeSynthEntry(Request request, Entry parentEntry, String id)
            throws Exception {
        String[] loc   = parseId(id);
        String   url   = loc[0];
        String   newId = getId(loc[0], loc[1]);
        if (parentEntry == null) {
            String parentId = (String) childIdToParent.get(newId);
            if (parentId != null) {
                //                parentEntry = getEntryManager().findGroup(request, parentId);
                parentEntry = getEntryManager().getEntry(request, parentId);
            }
        }

        if (parentEntry == null) {
            parentEntry = getEntryManager().getTopGroup();
        }

        URL     catalogUrl = new URL(url);
        Element root       = getDom(url);
        if (root == null) {
            throw new IllegalArgumentException("Could not load catalog:"
                    + url);
        }
        Element child = (Element) XmlUtil.findChild(root,
                            CatalogOutputHandler.TAG_DATASET);

        if (child != null) {
            root = child;
        }

        Hashtable idMap = new Hashtable();
        walkTree(root, idMap);
        if (loc[1] != null) {
            root = (Element) idMap.get(loc[1]);
        }

        String   name = XmlUtil.getAttribute(root, ATTR_NAME, "");
        Entry    entry;
        Resource resource;
        if (CatalogUtil.haveChildDatasets(root)
                || CatalogUtil.haveChildCatalogs(root)) {
            entry    = new Group(newId, this);
            resource = new Resource("", Resource.TYPE_URL);
        } else {
            String urlPath = XmlUtil.getAttribute(root,
                                 CatalogOutputHandler.ATTR_URLPATH,
                                 (String) null);
            if (urlPath == null) {
                Element accessNode = XmlUtil.findChild(root,
                                         CatalogOutputHandler.TAG_ACCESS);
                if (accessNode != null) {
                    urlPath = XmlUtil.getAttribute(accessNode,
                            CatalogOutputHandler.ATTR_URLPATH);
                }
            }

            if (urlPath != null) {
                Element serviceNode =
                    CatalogUtil.findServiceNodeForDataset(root, false, null);
                if (serviceNode != null) {
                    String path = XmlUtil.getAttribute(serviceNode, "base");
                    urlPath = new URL(catalogUrl, path + urlPath).toString();
                }
            }
            entry    = new Entry(newId, this);
            resource = new Resource(((urlPath != null)
                                     ? urlPath
                                     : ""), Resource.TYPE_URL);
        }

        List<Metadata> metadataList = new ArrayList<Metadata>();
        CatalogOutputHandler.collectMetadata(repository, metadataList,
                                             root);
        metadataList.add(new Metadata(repository.getGUID(),
                                      entry.getId(),
                                      ThreddsMetadataHandler.TYPE_LINK,
                                      DFLT_INHERITED,
                                      "Imported from catalog",
                                      url, "", ""));
        for (Metadata metadata : metadataList) {
            metadata.setEntryId(entry.getId());
            entry.addMetadata(metadata);
        }

        Date now = new Date();
        entry.initEntry(name, "", (Group) parentEntry,
                        getUserManager().localFileUser, resource, "",
                        now.getTime(), now.getTime(), now.getTime(), null);
        return entry;
    }

    /**
     * _more_
     *
     * @param id _more_
     *
     * @return _more_
     */
    public Entry createEntry(String id) {
        return new Group(id, this);
    }

}

