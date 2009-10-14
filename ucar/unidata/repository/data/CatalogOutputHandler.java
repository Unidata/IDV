/**
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

package ucar.unidata.repository.data;


import org.w3c.dom.*;

import ucar.unidata.repository.*;
import ucar.unidata.repository.metadata.*;
import ucar.unidata.repository.output.*;


import ucar.unidata.sql.SqlUtil;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.DateUtil;
import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.Misc;


import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;


import java.io.*;

import java.io.File;
import java.io.InputStream;



import java.net.*;


import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;



import java.util.regex.*;

import java.util.zip.*;


/**
 * Class SqlUtil _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class CatalogOutputHandler extends OutputHandler {

    /** _more_ */
    public static final String SERVICE_HTTP = "http";

    /** _more_ */
    public static final String SERVICE_SELF = "self";

    /** _more_ */
    public static final String SERVICE_OPENDAP = "opendap";

    /** _more_ */
    public static final String SERVICE_LATEST = "latest";

    /** _more_ */
    public static final String CATALOG_ATTRS =
        " xmlns=\"http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ";

    /** _more_ */
    public static final OutputType OUTPUT_CATALOG =
        new OutputType("Catalog", "thredds.catalog",
                       OutputType.TYPE_NONHTML | OutputType.TYPE_FORSEARCH,
                       "", ICON_CATALOG);






    /** _more_ */
    public static final String ARG_PATHS = "catalogoutputhandler.paths";


    /**
     * _more_
     *
     * @param repository _more_
     * @param element _more_
     * @throws Exception _more_
     */
    public CatalogOutputHandler(Repository repository, Element element)
            throws Exception {
        super(repository, element);
        addType(OUTPUT_CATALOG);
    }


    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     */
    public AuthorizationMethod getAuthorizationMethod(Request request) {
        return AuthorizationMethod.AUTH_HTTP;
    }



    /**
     * _more_
     *
     *
     * @param repository _more_
     * @param metadataList _more_
     * @param node _more_
     *
     * @throws Exception _more_
     */
    public static void collectMetadata(Repository repository,
                                       List<Metadata> metadataList,
                                       Element node)
            throws Exception {

        collectMetadata(repository, metadataList, node, "");
    }

    /**
     * _more_
     *
     * @param repository _more_
     * @param metadataList _more_
     * @param node _more_
     * @param tab _more_
     *
     * @throws Exception _more_
     */
    public static void collectMetadata(Repository repository,
                                       List<Metadata> metadataList,
                                       Element node, String tab)
            throws Exception {
        NodeList elements = XmlUtil.getElements(node);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();


        for (int i = 0; i < elements.getLength(); i++) {
            Element child = (Element) elements.item(i);
            String  tag   = child.getTagName();
            //            System.err.println(tab+ "tag:" + tag);
            if (tag.equals(CatalogUtil.TAG_METADATA)) {
                if ( !XmlUtil.getAttribute(child, "metadataType",
                                           "THREDDS").equals("THREDDS")) {
                    //                    System.err.println("Skipping: "
                    //                                       + XmlUtil.toString(child));
                    continue;
                }
                if (XmlUtil.hasAttribute(child, "xlink:href")) {
                    String url = XmlUtil.getAttribute(child, "xlink:href");
                    try {
                        Element root = XmlUtil.getRoot(url,
                                           CatalogOutputHandler.class);
                        if (root != null) {
                            collectMetadata(repository, metadataList, root,
                                            tab + "  ");
                        }
                    } catch (Exception exc) {
                        //ignore exceptions here
                        System.err.println("Error reading metadata:" + url
                                           + "\n" + exc);
                    }
                } else {
                    collectMetadata(repository, metadataList, child,
                                    tab + "  ");
                }
            } else {
                for (MetadataHandler metadataHandler : metadataHandlers) {
                    Metadata metadata =
                        metadataHandler.makeMetadataFromCatalogNode(child);
                    if (metadata != null) {
                        metadataList.add(metadata);
                        break;
                    }
                }

                //                System.err.println ("UNKNOWN:" + tag  + " " + XmlUtil.toString(node).trim());
                //                System.err.println("UNKNOWN:" + tag);
                //                throw new IllegalArgumentException("");
            }
        }
    }







    /**
     * _more_
     *
     * @param request _more_
     * @param state _more_
     * @param links _more_
     *
     *
     * @throws Exception _more_
     */
    public void getEntryLinks(Request request, State state, List<Link> links)
            throws Exception {
        if (state.getEntry() != null) {
            links.add(makeLink(request, state.getEntry(), OUTPUT_CATALOG));
        }
    }



    /**
     * _more_
     *
     * @param output _more_
     *
     * @return _more_
     */
    public String getMimeType(OutputType output) {
        if (output.equals(OUTPUT_CATALOG)) {
            return repository.getMimeTypeFromSuffix(".xml");
        } else {
            return super.getMimeType(output);
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param catalogInfo _more_
     * @param datasetNode _more_
     *
     * @throws Exception _more_
     */
    public void addMetadata(Request request, Entry entry,
                            CatalogInfo catalogInfo, Element datasetNode)
            throws Exception {
        List<Metadata> metadataList = getMetadataManager().getMetadata(entry);
        List<MetadataHandler> metadataHandlers =
            repository.getMetadataManager().getMetadataHandlers();
        for (Metadata metadata : metadataList) {
            for (MetadataHandler metadataHandler : metadataHandlers) {
                if (metadataHandler.canHandle(metadata)) {
                    metadataHandler.addMetadataToXml(request,
                            MetadataTypeBase.TEMPLATETYPE_THREDDS, entry,
                            metadata, catalogInfo.doc, datasetNode);
                    break;
                }
            }
        }

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param group _more_
     * @param subGroups _more_
     * @param entries _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public Result outputGroup(Request request, Group group,
                              List<Group> subGroups, List<Entry> entries)
            throws Exception {




        boolean justOneEntry = group.isDummy() && (entries.size() == 1)
                               && (subGroups.size() == 0);


        String   title = (justOneEntry
                          ? entries.get(0).getName()
                          : group.getFullName());
        Document doc   = XmlUtil.makeDocument();
        Element root = XmlUtil.create(doc, CatalogUtil.TAG_CATALOG, null,
                                      new String[] {
            "xmlns",
            "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0",
            "xmlns:xlink", "http://www.w3.org/1999/xlink",
            CatalogUtil.ATTR_NAME, title
        });


        /*        Element service = XmlUtil.create(doc,CatalogUtil.TAG_SERVICE,root,new String[]{
            CatalogUtil.ATTR_NAME,"all",
            CatalogUtil.ATTR_SERVICETYPE, "Compound",
            CatalogUtil.ATTR_BASE,""});*/


        Hashtable   serviceMap  = new Hashtable();

        Element     topDataset  = null;

        CatalogInfo catalogInfo = new CatalogInfo(doc, root, serviceMap);
        boolean     doingLatest = request.get(ARG_LATESTOPENDAP, false);
        if (doingLatest) {
            topDataset = root;
            boolean didone = false;
            entries = getEntryManager().sortEntriesOnDate(entries, true);
            for (Entry entry : entries) {
                if (canDataLoad(request, entry)) {
                    outputEntry(entry, request, catalogInfo, root);
                    didone = true;
                    break;
                }
            }
        } else if (justOneEntry) {
            outputEntry(entries.get(0), request, catalogInfo, root);
        } else {
            topDataset = XmlUtil.create(doc, CatalogUtil.TAG_DATASET, root,
                                        new String[] { CatalogUtil.ATTR_NAME,
                    title });
            addMetadata(request, group, catalogInfo, topDataset);
            int cnt  = subGroups.size() + entries.size();
            int max  = request.get(ARG_MAX, DB_MAX_ROWS);
            int skip = Math.max(0, request.get(ARG_SKIP, 0));
            //            System.err.println ("entries:" + entries.size() + " groups:" + subGroups.size()+ " max:" + max+" skip:" + skip);

            toCatalogInner(request, group, subGroups, catalogInfo,
                           topDataset);
            if ((cnt > 0) && ((cnt == max) || request.defined(ARG_SKIP))) {
                if (cnt >= max) {
                    String skipArg = request.getString(ARG_SKIP, null);
                    request.remove(ARG_SKIP);
                    String url = request.url(repository.URL_ENTRY_SHOW,
                                             ARG_ENTRYID, group.getId(),
                                             ARG_OUTPUT, OUTPUT_CATALOG,
                                             ARG_SKIP, "" + (skip + max),
                                             ARG_MAX, "" + max);

                    Element ref = XmlUtil.create(catalogInfo.doc,
                                      CatalogUtil.TAG_CATALOGREF, topDataset,
                                      new String[] {
                                          CatalogUtil.ATTR_XLINK_TITLE,
                                          "More...",
                                          CatalogUtil.ATTR_XLINK_HREF, url });

                    if (skipArg != null) {
                        request.put(ARG_SKIP, skipArg);
                    }
                }
            }

            toCatalogInner(request, group, entries, catalogInfo, topDataset);
            if ( !group.isDummy()
                    && (catalogInfo.serviceMap.get(SERVICE_OPENDAP)
                        != null)) {
                String urlPath = HtmlUtil.url("/latest", ARG_ENTRYID,
                                     group.getId(), ARG_LATESTOPENDAP,
                                     "true", ARG_OUTPUT, OUTPUT_CATALOG);
                addService(catalogInfo, SERVICE_LATEST,
                           getRepository().URL_ENTRY_SHOW.getFullUrl(),
                           "Resolver");

                Node firstChild = topDataset.getFirstChild();
                Element latestDataset = XmlUtil.create(catalogInfo.doc,
                                            CatalogUtil.TAG_DATASET, null,
                                            new String[] {
                                                CatalogUtil.ATTR_NAME,
                        "Latest OpenDAP Data" });
                XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_PROPERTY,
                               latestDataset,
                               new String[] { CatalogUtil.ATTR_NAME,
                        "icon", CatalogUtil.ATTR_VALUE,
                        getRepository().absoluteUrl(
                            getRepository().iconUrl(ICON_OPENDAP)) });


                topDataset.insertBefore(latestDataset, firstChild);
                Element service = XmlUtil.create(catalogInfo.doc,
                                      CatalogUtil.TAG_ACCESS, latestDataset,
                                      new String[] {
                                          CatalogUtil.ATTR_SERVICENAME,
                                          SERVICE_LATEST,
                                          CatalogUtil.ATTR_URLPATH,
                                          urlPath });
            }
        }


        StringBuffer sb = new StringBuffer(XmlUtil.XML_HEADER);
        sb.append(XmlUtil.toString(root));
        return new Result(title, sb, "text/xml");
    }


    /**
     * _more_
     *
     * @param catalogInfo _more_
     * @param service _more_
     * @param base _more_
     *
     *
     * @return _more_
     * @throws Exception _more_
     */
    private boolean addService(CatalogInfo catalogInfo, String service,
                               String base)
            throws Exception {
        return addService(catalogInfo, service, base, service);
    }


    /**
     * _more_
     *
     * @param catalogInfo _more_
     * @param service _more_
     * @param base _more_
     * @param type _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean addService(CatalogInfo catalogInfo, String service,
                               String base, String type)
            throws Exception {
        if (catalogInfo.serviceMap.get(service) != null) {
            return false;
        }

        List attrs = Misc.toList(new String[] {
            CatalogUtil.ATTR_NAME, service, CatalogUtil.ATTR_SERVICETYPE,
            type, CatalogUtil.ATTR_BASE, base
        });


        Element serviceNode = XmlUtil.create(catalogInfo.doc,
                                             CatalogUtil.TAG_SERVICE,
                                             catalogInfo.root, attrs);

        catalogInfo.serviceMap.put(service, serviceNode);
        return true;
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private boolean canDataLoad(Request request, Entry entry)
            throws Exception {
        return getRepository().getDataOutputHandler().canLoadAsCdm(entry);
    }

    /**
     * _more_
     *
     * @param entry _more_
     * @param request _more_
     * @param catalogInfo _more_
     * @param dataset _more_
     *
     * @throws Exception _more_
     */
    public void addServices(Entry entry, Request request,
                            CatalogInfo catalogInfo, Element dataset)
            throws Exception {
        File   f    = entry.getFile();
        String path = f.toString();
        path = path.replace("\\", "/");


        if (canDataLoad(request, entry)) {
            String urlPath =
                getRepository().getDataOutputHandler().getTdsUrl(entry);
            addService(catalogInfo, SERVICE_OPENDAP,
                       getRepository().URL_ENTRY_SHOW.getFullUrl());

            Element service = XmlUtil.create(catalogInfo.doc,
                                             CatalogUtil.TAG_ACCESS, dataset,
                                             new String[] {
                                                 CatalogUtil.ATTR_SERVICENAME,
                    SERVICE_OPENDAP, CatalogUtil.ATTR_URLPATH, urlPath });
        }


        if (entry.getTypeHandler().canDownload(request, entry)) {
            String urlPath =
                HtmlUtil.url("/" + getStorageManager().getFileTail(entry),
                             ARG_ENTRYID, entry.getId());
            addService(catalogInfo, SERVICE_HTTP,
                       getRepository().URL_ENTRY_GET.getFullUrl());
            Element service = XmlUtil.create(catalogInfo.doc,
                                             CatalogUtil.TAG_ACCESS, dataset,
                                             new String[] {
                                                 CatalogUtil.ATTR_SERVICENAME,
                    SERVICE_HTTP, CatalogUtil.ATTR_URLPATH, urlPath });
        }
        if (entry.getResource().isUrl()) {
            //            try {
            URL    url     = new URL(entry.getResource().getPath());
            String service = url.getProtocol() + "://" + url.getHost();
            if (url.getPort() > 0) {
                service = service + ":" + url.getPort();
            }
            addService(catalogInfo, service, service);
            String tail = url.getPath();
            if (url.getQuery() != null) {
                tail = tail + "?" + url.getQuery();
            }
            XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_ACCESS, dataset,
                           new String[] { CatalogUtil.ATTR_SERVICENAME,
                                          service, CatalogUtil.ATTR_URLPATH,
                                          tail });
            //            } catch (java.net.MalformedURLException mfe) {
            //For now
            //            }
        }
    }







    /**
     * _more_
     *
     *
     * @param entry _more_
     * @param request _more_
     * @param catalogInfo _more_
     * @param parent _more_
     *
     *
     * @throws Exception _more_
     */
    public void outputEntry(Entry entry, Request request,
                            CatalogInfo catalogInfo, Element parent)
            throws Exception {
        File   f    = entry.getFile();
        String path = f.toString();
        path = path.replace("\\", "/");
        Element dataset = XmlUtil.create(catalogInfo.doc,
                                         CatalogUtil.TAG_DATASET, parent,
                                         new String[] { CatalogUtil.ATTR_NAME,
                entry.getName() });

        String iconUrl =
            getRepository().absoluteUrl(getEntryManager().getIconUrl(request,
                entry));

        XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_PROPERTY, dataset,
                       new String[] { CatalogUtil.ATTR_NAME,
                                      "icon", CatalogUtil.ATTR_VALUE,
                                      iconUrl });


        XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_PROPERTY, dataset,
                       new String[] { CatalogUtil.ATTR_NAME,
                                      "ramadda.id", CatalogUtil.ATTR_VALUE,
                                      entry.getId() });
        XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_PROPERTY, dataset,
                       new String[] { CatalogUtil.ATTR_NAME,
                                      "ramadda.host", CatalogUtil.ATTR_VALUE,
                                      getRepository().getHostname() });

        String dataType = null;
        if (path.endsWith(".area")) {
            dataType = "Image";
        } else {
            //TODO: more types here
        }

        if (dataType != null) {
            dataset.setAttribute(CatalogUtil.ATTR_DATATYPE, dataType);
        }




        if (entry.getDataType() != null) {
            String type = entry.getDataType();
            if (false && (type != null) && (type.length() > 0)) {
                XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_PROPERTY,
                               dataset, new String[] { CatalogUtil.ATTR_NAME,
                        "idv.datatype", CatalogUtil.ATTR_VALUE, type });
            }

        }


        addServices(entry, request, catalogInfo, dataset);

        addMetadata(request, entry, catalogInfo, dataset);

        if (f.exists()) {
            XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_DATASIZE,
                           dataset, "" + f.length(),
                           new String[] { CatalogUtil.ATTR_UNITS,
                                          "bytes" });

        }

        XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_DATE, dataset,
                       formatDate(request, new Date(entry.getCreateDate())),
                       new String[] { CatalogUtil.ATTR_TYPE,
                                      "metadataCreated" });

        Element timeCoverage = XmlUtil.create(catalogInfo.doc,
                                   CatalogUtil.TAG_TIMECOVERAGE, dataset);
        XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_START, timeCoverage,
                       "" + formatDate(request,
                                       new Date(entry.getStartDate())));
        XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_END, timeCoverage,
                       "" + formatDate(request,
                                       new Date(entry.getEndDate())));
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param parentGroup _more_
     * @param entryList _more_
     * @param catalogInfo _more_
     * @param parent _more_
     *
     *
     * @throws Exception _more_
     */
    protected void toCatalogInner(Request request, Group parentGroup,
                                  List entryList, CatalogInfo catalogInfo,
                                  Element parent)
            throws Exception {

        List<Entry> entries = new ArrayList();
        List<Group> groups  = new ArrayList();
        for (int i = 0; i < entryList.size(); i++) {
            Entry entry = (Entry) entryList.get(i);
            if (entry.isGroup()) {
                groups.add((Group) entry);
            } else {
                entries.add(entry);
            }
        }
        for (Group group : groups) {
            String url =  /* "http://localhost:8080"+*/
                request.url(repository.URL_ENTRY_SHOW, ARG_ENTRYID,
                            group.getId(), ARG_OUTPUT, OUTPUT_CATALOG);

            Element ref = XmlUtil.create(catalogInfo.doc,
                                         CatalogUtil.TAG_CATALOGREF, parent,
                                         new String[] {
                                             CatalogUtil.ATTR_XLINK_TITLE,
                                             group.getName(),
                                             CatalogUtil.ATTR_XLINK_HREF,
                                             url });
        }

        EntryGroup entryGroup = new EntryGroup("");
        for (Entry entry : entries) {
            String     typeDesc = entry.getTypeHandler().getLabel();
            EntryGroup subGroup = entryGroup.find(typeDesc);
            subGroup.add(entry);
        }

        generate(request, entryGroup, catalogInfo, parent);

    }


    /**
     * _more_
     *
     * @param request _more_
     * @param parent _more_
     * @param catalogInfo _more_
     * @param datasetNode _more_
     *
     * @throws Exception _more_
     */
    protected void generate(Request request, EntryGroup parent,
                            CatalogInfo catalogInfo, Element datasetNode)
            throws Exception {

        for (int i = 0; i < parent.keys().size(); i++) {
            Object     key   = parent.keys().get(i);
            EntryGroup group = (EntryGroup) parent.getMap().get(key);
            /*            Element dataset = XmlUtil.create(catalogInfo.doc, CatalogUtil.TAG_DATASET,
                                             datasetNode,
                                             new String[] { CatalogUtil.ATTR_NAME,
                    group.key.toString() });
            */
            Element dataset = datasetNode;
            for (int j = 0; j < group.getChildren().size(); j++) {
                Object child = group.getChildren().get(j);
                if (child instanceof EntryGroup) {
                    EntryGroup subGroup = (EntryGroup) child;
                    generate(request, subGroup, catalogInfo, dataset);
                } else if (child instanceof Entry) {
                    Entry entry = (Entry) child;
                    outputEntry(entry, request, catalogInfo, dataset);
                }
            }
        }
    }


    /**
     * Class CatalogInfo _more_
     *
     *
     * @author IDV Development Team
     * @version $Revision: 1.3 $
     */
    private static class CatalogInfo {

        /** _more_ */
        Document doc;

        /** _more_ */
        Hashtable serviceMap;

        /** _more_ */
        Element root;

        /**
         * _more_
         *
         * @param doc _more_
         * @param root _more_
         * @param serviceMap _more_
         */
        public CatalogInfo(Document doc, Element root, Hashtable serviceMap) {
            this.doc        = doc;
            this.serviceMap = serviceMap;
            this.root       = root;
        }
    }



}

