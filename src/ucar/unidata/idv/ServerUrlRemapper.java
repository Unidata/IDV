/*
 * Copyright 1997-2013 Unidata Program Center/University Corporation for
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
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import thredds.util.UnidataTdsDataPathRemapper;

import ucar.unidata.data.DataSource;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A class to handle remapping URLs from data sources as
 * they are unpersisted from bundles
 */

public class ServerUrlRemapper {

    /** Reference to the IDV */
    private IntegratedDataViewer idv;

    /** Reference to the Url Map Resource Collection */
    private XmlResourceCollection urlmapResourceCollection;

    /** xml file name for the latest dataset */
    private static final String LATEST_XML_NAME = "latest.xml";

    /** property to indicate whether or not to force connections to the tds test server */
    private static final String TEST_TDS_43_UPDATE = "tds.update.test";

    /** old name for best time series on a pre 43 TDS server */
    private static final String TDS_PRE_43_BEST_NAME = "best";

    /** old suffix for the best timeseries on a pre 4.3 TDS */
    private static final String TDS_PRE_43_BEST_NAME_SUFFIX = ".ndc";

    /** key name for TDS hashmap for best time series */
    private static final String BEST_REMAP_KEY = "best";

    /** key name for TDS hashmap for latest */
    private static final String LATEST_REMAP_KEY = "latest";

    /** TDS Service name for opendap */
    private static final String TDS_DODS_SERVICE = "/dodsC/";

    /** TDS Service name for catalogs */
    private static final String TDS_CATALOG_SERVICE = "/catalog/";

    /** Datasource ID used for unpersistence */
    private static final String ID_DATASOURCES = IdvConstants.ID_DATASOURCES;

    /** string that indicates the ncIdv version is unknown (pre IDV 4.0 bundles) */
    private static final String UNKNOWN_NCIDV_VERSION = "unknown";

    /** ncIdv version */
    private String ncIdvVersion = UNKNOWN_NCIDV_VERSION;

    /** Xml tag for url maps xml */
    public static final String TAG_URLMAP = "urlmap";

    /** URL Maps (Type : {oldUrl : newUrl} */
    private HashMap<String, HashMap<String, String>> urlMaps =
        new HashMap<String, HashMap<String, String>>();

    /**
     * Construct a ServerUrlRemapper
     *
     * @param idv instance of the IDV
     */
    public ServerUrlRemapper(IntegratedDataViewer idv) {
        this.idv = idv;
        init();
    }

    /**
     * initilize the ServerUrlRemapper (get remaps from resources
     */
    private void init() {

        urlmapResourceCollection = idv.getResourceManager().getXmlResources(
            IdvResourceManager.RSC_URLMAPS);

        readUrlRemapResources();
    }

    /**
     * Read in the resource xml files and store the URL maps
     */
    private void readUrlRemapResources() {

        for (int urlRemapResourceIdx = 0;
                urlRemapResourceIdx < urlmapResourceCollection.size();
                urlRemapResourceIdx++) {
            Element root =
                this.urlmapResourceCollection.getRoot(urlRemapResourceIdx,
                    false);

            if (root == null) {
                continue;
            }

            List nodes = XmlUtil.findChildren(root, TAG_URLMAP);

            for (Object node1 : nodes) {
                Element      node      = (Element) node1;
                final String urlType   = XmlUtil.getAttribute(node, "type");
                String       tmpOldUrl = XmlUtil.getAttribute(node, "old");
                String       tmpNewUrl = XmlUtil.getAttribute(node, "new");
                if ( !tmpOldUrl.endsWith("/")) {
                    tmpOldUrl = tmpOldUrl + '/';
                }

                if ( !tmpNewUrl.endsWith("/")) {
                    tmpNewUrl = tmpNewUrl + '/';
                }
                final String oldUrl = tmpOldUrl;
                final String newUrl = tmpNewUrl;

                if (this.urlMaps.containsKey(urlType)) {
                    HashMap<String, String> tmpUrlMap = urlMaps.get(urlType);
                    if ( !tmpUrlMap.containsKey(oldUrl)) {
                        tmpUrlMap.put(oldUrl, newUrl);
                        this.urlMaps.put(urlType, tmpUrlMap);
                    }
                } else {
                    HashMap<String, String> tmpUrlMap = new HashMap<String,
                                                            String>();
                    tmpUrlMap.put(oldUrl, newUrl);
                    this.urlMaps.put(urlType, tmpUrlMap);
                }
            }
        }
    }

    /**
     * thin wrapper to get IDV property
     *
     * @param name name of property
     * @param dflt default value
     *
     * @return property
     */
    private boolean getProperty(String name, boolean dflt) {
        return this.idv.getStateManager().getProperty(name, dflt);
    }

    /**
     * Walk the xml tree of a bundle, and find and replace
     * urls that need to be updated before unpersistence
     *
     * @param xml string representation of the xml bundle
     *
     * @return a hash map of old -> new url transformations based on
     * urls found in the bundle
     */
    private HashMap<String, String> bundleWalker(String xml) {
        HashMap<String, String> urlNameMaps = new HashMap<String, String>();
        Document                bundle;
        try {
            bundle = XmlUtil.getDocument(xml);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return urlNameMaps;
        }

        DocumentTraversal docTraversal = (DocumentTraversal) bundle;

        TreeWalker iter =
            docTraversal.createTreeWalker(bundle.getDocumentElement(),
                                          NodeFilter.SHOW_CDATA_SECTION,
                                          null, false);
        Node         n                = null;
        List<String> checkUrlTriggers = new ArrayList<String>();
        checkUrlTriggers.add(ID_DATASOURCES);
        checkUrlTriggers.add(DataSource.PROP_RESOLVERURL);

        Boolean checkForUrl     = Boolean.FALSE;
        String  checkForUrlType = null;
        String  oldPath, newPath;

        while ((n = iter.nextNode()) != null) {
            if (checkForUrl) {
                oldPath = n.getNodeValue();
                if (!oldPath.equals("version")) {
                    newPath = remapMotherlodeToThredds(oldPath, checkForUrlType);
                    urlNameMaps.put(oldPath, newPath);
                }
                checkForUrl = Boolean.FALSE;
            }

            if (checkUrlTriggers.contains(n.getNodeValue())) {
                checkForUrl     = Boolean.TRUE;
                checkForUrlType = n.getNodeValue();
            }
        }
        return urlNameMaps;
    }

    /**
     * change urls pointing to motherlode.ucar.edu to
     * use thredds.ucar.edu. Also handles changing the
     * datasetUrlPath if required (i.e. old bundles, testTDS
     * plugin enabled).
     *
     * @param oldPath old URL
     * @param oldPathType either a RESOLVER url or a datasource URL
     *
     * @return new url string
     */
    private String remapMotherlodeToThredds(String oldPath,
                                            String oldPathType) {

        Boolean testTds = getProperty(TEST_TDS_43_UPDATE, Boolean.FALSE);
        HashMap<String, String> serverRemap = this.urlMaps.get("opendap");
        String                  newPath     = oldPath;

        for (Map.Entry<String, String> oldServerName :
                serverRemap.entrySet()) {
            oldPath = oldPath.replace(":-1/", "/");
            if (oldPath.contains(oldServerName.getKey())) {
                // if old path uses an old server name, like motherlode.ucar.edu, then update with new
                newPath = oldPath.replace(oldServerName.getKey(),
                                          oldServerName.getValue());
                // if ncIdvVersion does not exists, then it was created with a pre tds 4.2 -> 4.3 transition
                // IDV and the url data path likely needs to be updated
                //uncomment next line once 8080 is running 4.3
                //ToDo: enable ncIdvVersion check once thredds.ucar.edu -> 4.3
                //if ((ncIdvVersion != UNKNOWN_NCIDV_VERSION) || (testTds)) {
                if (testTds) {
                    newPath = remapOldMotherlodeDatasetUrlPath(newPath,
                            oldPathType);
                }
                break;
            }
        }

        return newPath;
    }

    /**
     * Updates a DatasetUrlPath from pre tds 4.3 to
     * 4.3.
     *
     * @param oldUrl old url
     * @param oldPathType RESOLVER url or datasource URL
     *
     * @return _more_
     */
    private String remapOldMotherlodeDatasetUrlPath(String oldUrl,
            String oldPathType) {
        String newUrl = oldUrl;
        // this is where the fmrc -> grib magic will happen
        UnidataTdsDataPathRemapper remapper =
            new UnidataTdsDataPathRemapper();
        // grab dataSource URL
        String[] breakUrl = null;
        if (oldUrl.contains(TDS_CATALOG_SERVICE)) {
            breakUrl = oldUrl.split(TDS_CATALOG_SERVICE);
        } else if (oldUrl.contains(TDS_DODS_SERVICE)) {
            breakUrl = oldUrl.split(TDS_DODS_SERVICE);
        }

        String oldUrlPath;

        assert breakUrl != null;
        if (breakUrl.length == 2) {
            oldUrlPath = breakUrl[1];
        } else {
            return oldUrl;
        }
        String map = null;

        if (oldUrlPath.contains(LATEST_XML_NAME)) {
            oldUrlPath = oldUrlPath.split(LATEST_XML_NAME)[0];
            map        = LATEST_REMAP_KEY;
        } else if ((oldUrlPath.contains(TDS_PRE_43_BEST_NAME))
                   || (oldUrlPath.contains(TDS_PRE_43_BEST_NAME_SUFFIX))) {
            map = BEST_REMAP_KEY;
        }

        List<String> newUrlPaths = remapper.getMappedUrlPaths(oldUrlPath,
                                       map);
        if ((newUrlPaths != null) && (newUrlPaths.size() == 1)) {
            String newUrlPath = newUrlPaths.get(0);
            newUrl = oldUrl.replace(oldUrlPath, newUrlPath);
        }

        return newUrl;
    }

    /**
     * basic method used to remap urls found in a
     * bundle xml file
     *
     * @param xml string representation of a bundle
     *
     * @return updated string representation of a bundle
     */
    public String remapUrlsInBundle(String xml) {
        HashMap<String, String> urlNameMaps = bundleWalker(xml);
        for (String oldUrl : urlNameMaps.keySet()) {
            while (xml.contains(oldUrl)) {
                xml = xml.replace(oldUrl, urlNameMaps.get(oldUrl));
            }
        }

        return xml;
    }

}
