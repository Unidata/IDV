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

import ucar.unidata.sql.SqlUtil;

import ucar.unidata.ui.ImageUtils;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.HttpServer;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;

import ucar.unidata.util.StringUtil;

import ucar.unidata.view.geoloc.NavigatedMapPanel;
import ucar.unidata.xml.XmlUtil;



import java.io.*;



import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;




/**
 * Class CatalogHarvester _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CatalogHarvester extends Harvester {

    /** _more_ */
    Group topGroup;

    /** _more_ */
    boolean recurse = false;

    /** _more_ */
    Hashtable seen = new Hashtable();

    /** _more_ */
    List groups = new ArrayList();

    /** _more_ */
    int catalogCnt = 0;

    /** _more_ */
    int entryCnt = 0;

    /** _more_ */
    int groupCnt = 0;

    /** _more_ */
    User user;

    /** _more_ */
    String topUrl;

    /** _more_ */
    List<Entry> entries = new ArrayList<Entry>();

    /**
     * _more_
     *
     * @param repository _more_
     * @param group _more_
     * @param url _more_
     * @param user _more_
     * @param recurse _more_
     */
    public CatalogHarvester(Repository repository, Group group, String url,
                            User user, boolean recurse) {
        super(repository);
        setName("Catalog harvester");
        this.recurse  = recurse;
        this.topGroup = group;
        this.topUrl   = url;
        this.user     = user;
    }


    /**
     * _more_
     *
     * @throws Exception _more_
     */
    protected void runInner() throws Exception {
        groups = new ArrayList();
        importCatalog(topUrl, topGroup, 0);
        //        repository.processEntries(this, null, entries,false);
        if (entries.size() > 0) {
            repository.processEntries(this, null, entries,false);
        }
        entries = new ArrayList<Entry>();
    }


    /**
     * _more_
     *
     * @param url _more_
     * @param parent _more_
     * @param depth _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean importCatalog(String url, Group parent, int depth)
            throws Exception {
        if ( !getActive()) {
            return true;
        }
        if (seen.get(url) != null) {
            return true;
        }
        if (depth > 10) {
            System.err.println("Catalogs go too deep:" + url);
            return true;
        }
        catalogCnt++;
        if (catalogCnt % 10 == 0) {
            System.err.print(".");
        }
        //        http://data.eol.ucar.edu/jedi/catalog/ucar.ncar.eol.project.ATLAS.thredds.xml
        //        if(catalogCnt>100) return true;
        seen.put(url, url);
        //        System.err.println(url);
        try {
            Element root = XmlUtil.getRoot(url, getClass());
            if (root == null) {
                System.err.println("Could not load catalog:" + url);
            } else {
                Node child = XmlUtil.findChild(root,
                                 CatalogOutputHandler.TAG_DATASET);
                if (child != null) {
                    recurseCatalog((Element) child, parent, url, 0, depth);
                }
            }
            return true;
        } catch (Exception exc) {
            exc.printStackTrace();
            //            log("",exc);
            return false;
        }
    }


    /**
     * _more_
     *
     * @param entry _more_
     * @param metadataList _more_
     */
    private void insertMetadata(Entry entry, List<Metadata> metadataList) {
        for (Metadata metadata : metadataList) {
            metadata.setEntryId(entry.getId());
            try {
                if (metadata.getAttr1().length() > 10000) {
                    repository.log("Too long metadata:"
                                   + metadata.getAttr1().substring(0, 100)
                                   + "...");
                    continue;
                }
                getMetadataManager().insertMetadata(metadata);
            } catch (Exception exc) {
                repository.log("Bad metadata", exc);
                System.err.println("metadata attr1" + metadata.getAttr1());
                System.err.println("metadata attr2" + metadata.getAttr2());
                System.err.println("metadata attr3" + metadata.getAttr3());
                System.err.println("metadata attr4" + metadata.getAttr4());

            }
        }

    }

    /**
     * _more_
     *
     * @param node _more_
     * @param parent _more_
     * @param catalogUrl _more_
     * @param depth _more_
     * @param catalogUrlPath _more_
     * @param xmlDepth _more_
     * @param recurseDepth _more_
     *
     * @throws Exception _more_
     */
    private void recurseCatalog(Element node, Group parent,
                                String catalogUrlPath, int xmlDepth,
                                int recurseDepth)
            throws Exception {

        String tab = "";
        for(int i=0;i<xmlDepth;i++ )
            tab  =tab+"  ";
        URL catalogUrl = new URL(catalogUrlPath);
        //        if (xmlDepth > 1) {
            //            return;
        //        }


        if ( !getActive()) {
            return;
        }
        String   name     = XmlUtil.getAttribute(node, ATTR_NAME);
        NodeList elements = XmlUtil.getElements(node);
        String urlPath = XmlUtil.getAttribute(node,
                             CatalogOutputHandler.ATTR_URLPATH,
                             (String) null);
        if (urlPath == null) {
            Element accessNode = XmlUtil.findChild(node,
                                     CatalogOutputHandler.TAG_ACCESS);
            if (accessNode != null) {
                urlPath = XmlUtil.getAttribute(accessNode,
                        CatalogOutputHandler.ATTR_URLPATH);
            }
        }



        boolean haveChildDatasets = false;
        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if (child.getTagName().equals(CatalogOutputHandler.TAG_DATASET)) {
                haveChildDatasets = true;
                break;
            }
        }

        //        System.err.println(tab+"name:" + name+"  #children:" + elements.getLength() +" depth:" + xmlDepth + " " + urlPath +" " + haveChildDatasets);
        if ( !haveChildDatasets && (xmlDepth > 0) && (urlPath != null)) {
            Element serviceNode = CatalogUtil.findServiceNodeForDataset(node,
                                      false, null);
            if (serviceNode != null) {
                String path = XmlUtil.getAttribute(serviceNode, "base");
                if (path.startsWith("/")) {
                    path = catalogUrl.getProtocol() + "://"
                           + catalogUrl.getHost() + ":"
                           + catalogUrl.getPort() + path;
                }
                //TODO: handle the base when it does not start with '/'
                urlPath = path + urlPath;
            }

            TypeHandler typeHandler =
                repository.getTypeHandler(TypeHandler.TYPE_FILE);
            entryCnt++;
            Entry  entry      = typeHandler.createEntry(repository.getGUID());
            Date   createDate = new Date();
            String ext        = IOUtil.getFileExtension(urlPath);
            if (ext.startsWith(".")) {
                ext = ext.substring(1);
            }
            if (ext.length() > 0) {
                entry.addMetadata(new Metadata(repository.getGUID(),
                        entry.getId(), EnumeratedMetadataHandler.TYPE_TAG,
                        DFLT_INHERITED, ext, "", "", ""));
            }
            entry.initEntry(name, "", parent, "", user,
                            new Resource(urlPath, Resource.TYPE_URL), "",
                            createDate.getTime(), createDate.getTime(),
                            createDate.getTime(), null);
            entries.add(entry);
            typeHandler.initializeNewEntry(entry);

            List<Metadata> metadataList = new ArrayList<Metadata>();
            CatalogOutputHandler.collectMetadata(repository, metadataList,
                    node);
            metadataList.add(new Metadata(repository.getGUID(),
                                          entry.getId(),
                                          ThreddsMetadataHandler.TYPE_LINK,
                                          DFLT_INHERITED,
                                          "Imported from catalog",
                                          catalogUrlPath, "", ""));
            System.err.println ("metadata:" + metadataList);
            for (Metadata metadata : metadataList) {
                entry.addMetadata(metadata);
            }

            if (entries.size() > 100) {
                repository.processEntries(this, null, entries,false);
                entries = new ArrayList<Entry>();
            }
            return;
        }

        name = name.replace(Group.IDDELIMITER, "--");
        name = name.replace("'", "");
        Group group    = null;
        Entry newGroup = repository.findEntryWithName(null, parent, name);
        if ((newGroup != null) && newGroup.isGroup()) {
            group = (Group) newGroup;
        }
        if (group == null) {
            //            System.err.println("Making new group:" + name);
            group = repository.makeNewGroup(parent, name, user);
            List<Metadata> metadataList = new ArrayList<Metadata>();
            CatalogOutputHandler.collectMetadata(repository, metadataList,
                    node);
            metadataList.add(new Metadata(repository.getGUID(),
                                          group.getId(),
                                          ThreddsMetadataHandler.TYPE_LINK,
                                          DFLT_INHERITED,
                                          "Imported from catalog",
                                          catalogUrlPath, "", ""));

            insertMetadata(group, metadataList);
            String crumbs = repository.getBreadCrumbs(null, group, true, 
                                                      topGroup)[1];
            crumbs = crumbs.replace("class=", "xclass=");
            groups.add(crumbs);
            groupCnt++;
            if (groups.size() > 100) {
                groups = new ArrayList();
            }
        }



        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if (child.getTagName().equals(CatalogOutputHandler.TAG_DATASET)) {
                recurseCatalog(child, group, catalogUrlPath, xmlDepth + 1,
                               recurseDepth);
            } else if (child.getTagName().equals(
                    CatalogOutputHandler.TAG_CATALOGREF)) {
                if ( !recurse) {
                    continue;
                }
                String url = XmlUtil.getAttribute(child, "xlink:href");
                if ( !url.startsWith("http")) {
                    if (url.startsWith("/")) {
                        url = catalogUrl.getProtocol() + "://"
                              + catalogUrl.getHost() + ":"
                              + catalogUrl.getPort() + url;
                    } else {
                        //                        System.err.println("url:" + url +" catalog:" +
                        //                                           catalogUrlPath +  " root: " + IOUtil.getFileRoot(catalogUrlPath));
                        url = IOUtil.getFileRoot(catalogUrlPath) + "/" + url;
                    }
                }
                if ( !importCatalog(url, group, recurseDepth + 1)) {
                    System.err.println("Could not load catalog:" + url);
                    System.err.println("Base catalog:" + catalogUrl);
                    System.err.println("Base URL:"
                                       + XmlUtil.getAttribute(child,
                                           "xlink:href"));
                }
            }
        }
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public String getExtraInfo() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("Catalog: " + topUrl + "<br>");
        sb.append("Loaded " + catalogCnt + " catalogs<br>");
        sb.append("Created " + entryCnt + " entries<br>");
        sb.append("Created " + groupCnt + " groups");

        StringBuffer groupSB = new StringBuffer();
        groupSB.append("<div class=\"scrollablediv\"><ul>");
        for (int i = 0; i < groups.size(); i++) {
            String groupLine = (String) groups.get(i);
            groupSB.append("<li>");
            groupSB.append(groupLine);
        }
        groupSB.append("</ul></div>");
        sb.append(getRepository().makeShowHideBlock(null, "Entries", groupSB,
                false));
        return sb.toString();
    }


}

