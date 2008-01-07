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

import ucar.unidata.data.SqlUtil;

import ucar.unidata.ui.ImageUtils;
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




public class CatalogHarvester extends Harvester {
    Group topGroup;
    boolean recurse = false;
    Hashtable seen = new Hashtable();
    List groups = new ArrayList();
    int catalogCnt=0;
    User user;
    String topUrl;

    public CatalogHarvester(Repository repository,Group group, String url, User user, boolean recurse) {
        super(repository);
        setName("Catalog harvester");
        this.recurse = recurse;
        this.topGroup = group;
        this.topUrl = url;
        this.user = user;
    }


    protected void runInner() throws Exception {
        groups = new ArrayList();
        importCatalog(topUrl, topGroup);
    }


    private boolean importCatalog(String url, Group parent) throws Exception {
        if(!getActive()) return true;
        if(seen.get(url)!=null) return true;
        catalogCnt++;
        if(catalogCnt%10 == 0)
            System.err.print(".");
        //        http://data.eol.ucar.edu/jedi/catalog/ucar.ncar.eol.project.ATLAS.thredds.xml
        //        if(catalogCnt>100) return true;
        seen.put(url,url);
        //        System.err.println(url);
        try {
            Element root  = XmlUtil.getRoot(url, getClass());
            Node child = XmlUtil.findChild(root, CatalogOutputHandler.TAG_DATASET);
            if(child!=null) {
                recurseCatalog((Element)child,parent,url,0);
            }
            return true;
        } catch(Exception exc) {
            System.err.println ("exc:" + exc);
            //            log("",exc);
            return  false;
        }
    }

        private void recurseCatalog(Element node, Group parent,  String catalogUrl, int depth) throws Exception {
            if(!getActive()) return;
        String name = XmlUtil.getAttribute(node, ATTR_NAME);
        if(depth>1) {
            return;
        }

        /*
        if(node.getTagName().equals(CatalogOutputHandler.TAG_DATASET)) {
            Element serviceNode = ucar.unidata.idv.chooser.ThreddsHandler.findServiceNodeForDataset(node, false,
                                                                                                    null);

            if (serviceNode != null) {
                String path = ucar.unidata.idv.chooser.ThreddsHandler.getUrlPath(node);
                if(path!=null) {
                    //                    System.err.println ("got path:" + path);
                    //                    System.err.println ("full path:" + XmlUtil.getAttribute(serviceNode,"base") + path);
                    //                    return;
                }
            }
            }*/

        NodeList elements = XmlUtil.getElements(node);
        String urlPath = XmlUtil.getAttribute(node, CatalogOutputHandler.ATTR_URLPATH, (String)null);
        if(urlPath!=null) {
            return;
        }
        if(urlPath == null) {
            Element accessNode = XmlUtil.findChild(node,CatalogOutputHandler.TAG_ACCESS);
            if(accessNode!=null) {
                urlPath = XmlUtil.getAttribute(accessNode, CatalogOutputHandler.ATTR_URLPATH);
            }
        }


        if(elements.getLength()==0 && depth>0 && urlPath!=null) {
            System.err.println("skipping 2:" + urlPath + " " + catalogUrl);
            return;
        }


        name = name.replace("/","--");
        name = name.replace("'","");
        //        Group group = null;
        String groupName  = (parent==null?name:parent.getFullName()+"/"+name);
        Group group = repository.findGroupFromName(groupName);
        if(group == null) {
            group = repository.findGroupFromName(groupName, user, true);
            List<Metadata> metadataList = new ArrayList<Metadata>();
            CatalogOutputHandler.collectMetadata(metadataList, node);
            metadataList.add(new Metadata(Metadata.TYPE_URL,"Imported from catalog",
                                      catalogUrl));
            for(Metadata metadata: metadataList) {
                metadata.setId(group.getId());
                metadata.setIdType(Metadata.IDTYPE_GROUP);
                try {
                    if(metadata.getContent().length()>10000) {
                        repository.log("Too long metadata:" + metadata.getContent().substring(0,100)+"...");
                        continue;
                    }
                    repository.insertMetadata(metadata);
                } catch(Exception exc) {
                    repository.log("Bad metadata", exc);
                }
            }
            groups.add(group);
        }


        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            if(child.getTagName().equals(CatalogOutputHandler.TAG_DATASET)) {
                recurseCatalog(child, group,catalogUrl, depth+1);
            } else   if(child.getTagName().equals(CatalogOutputHandler.TAG_CATALOGREF)) {
                if(!recurse)continue;
                String url = XmlUtil.getAttribute(child, "xlink:href");
                if(!url.startsWith("http")) {
                    if(url.startsWith("/")) {
                        URL base = new URL(catalogUrl);
                        url =base.getProtocol()+"://" + base.getHost()+":"+ base.getPort()+url;
                    } else {
                        url =IOUtil.getFileRoot(catalogUrl) +"/" + url;
                    }
                }
                if(!importCatalog(url, group)) {
                    System.err.println("Could not load catalog:" + url);
                    System.err.println("Base catalog:" + catalogUrl);
                    System.err.println("Base URL:" +   XmlUtil.getAttribute(child, "xlink:href"));
                }
            }
        }
    }


    public String getExtraInfo() throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append("Catalog: " + topUrl +"<br>");
        sb.append("Loaded " + catalogCnt +" catalogs<br>");
        sb.append("Created " + groups.size() +" groups<ul>");
        for(int i=0;i<groups.size();i++) {
            Group newGroup = (Group) groups.get(i);
            sb.append("<li>");
            sb.append(repository.getBreadCrumbs(null, newGroup, true,"",topGroup)[1]);
        }
        sb.append("</ul>");
        return sb.toString();
    }


}

