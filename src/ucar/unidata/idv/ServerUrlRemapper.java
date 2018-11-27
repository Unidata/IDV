/*
 * Copyright 1997-2019 Unidata Program Center/University Corporation for
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


import org.apache.xerces.dom.AttributeMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import org.xml.sax.SAXException;

import thredds.util.UnidataTdsDataPathRemapper;

import ucar.unidata.data.DataSource;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


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
    private static final String FILES_REMAP_KEY = "files";

    /** TDS Service name for opendap */
    private static final String TDS_DODS_SERVICE = "/dodsC/";

    /** TDS Service name for catalogs */
    private static final String TDS_CATALOG_SERVICE = "/catalog/";

    /** string that indicates the ncIdv version is unknown (pre IDV 4.0 bundles) */
    private static final String UNKNOWN_NCIDV_VERSION = "unknown";

    /** ncIdv version */
    private String ncIdvVersion = UNKNOWN_NCIDV_VERSION;

    /** Xml tag for url maps xml */
    public static final String TAG_URLMAP = "urlmap";

    /** URL Maps (Type : {oldUrl : newUrl} */
    private HashMap<String, HashMap<String, String>> urlMaps =
        new HashMap<String, HashMap<String, String>>();

    /** name for cdata tags */
    public static final String CDATA_ID = "#cdata-section";


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
     *
     * @throws IOException _more_
     * @throws SAXException _more_
     * @throws TransformerException _more_
     */

    private Node bundleWalker(String xml)
            throws SAXException, IOException, TransformerException {

        Document bundle;
        try {
            bundle = XmlUtil.getDocument(xml);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }

        DocumentTraversal docTraversal = (DocumentTraversal) bundle;

        TreeWalker walker =
            docTraversal.createTreeWalker(bundle.getDocumentElement(),
                                          NodeFilter.SHOW_ALL, null, false);

        List<String> topLevelTags = new ArrayList<String>();
        topLevelTags.add(IdvConstants.ID_VERSION);
        topLevelTags.add(IdvConstants.ID_VIEWMANAGERS);
        topLevelTags.add(IdvConstants.ID_DISPLAYCONTROLS);

        Boolean inDatasourceTag = false;
        Boolean inPropertyTag   = false;
        Boolean inSourcesTag    = false;
        Boolean inDisplayControlsTag = false;
        String  oldPath, newPath, name, value;


        // look to see if the root tag indicates a datasource only bundle
        AttributeMap propAttrs =
            (AttributeMap) walker.getRoot().getAttributes();
        String attrName = propAttrs.getNamedItem("class").getNodeValue();

        if (attrName.contains("ucar.unidata.data")) {
            inDatasourceTag = true;
        }

        if (attrName.contains("ucar.unidata.idv.control")) {
            inDisplayControlsTag = true;
        }

        Node thisNode;
        thisNode = walker.nextNode();
        while (thisNode != null) {
            name  = thisNode.getNodeName();
            value = thisNode.getNodeValue();
            if (value != null) {
                if (value.equals(IdvConstants.ID_DATASOURCES)) {
                    inDatasourceTag = true;
                } else if (value.equals(IdvConstants.ID_DISPLAYCONTROLS)) {
                    inDisplayControlsTag = true;
                } else if (topLevelTags.contains(value)) {
                    inDatasourceTag = false;
                    inDisplayControlsTag = false;
                }
            }

            if (inDatasourceTag || inDisplayControlsTag) {
                if (name.equals("property")) {
                    propAttrs = (AttributeMap) thisNode.getAttributes();
                    attrName  = propAttrs.getNamedItem("name").getNodeValue();
                    if (attrName.equals("Sources")) {
                        inSourcesTag = true;
                    } else {
                        inSourcesTag = false;
                    }
                    if (attrName.equals("Properties")) {
                        inPropertyTag = true;
                    } else {
                        inPropertyTag = false;
                    }
                }

                if (inSourcesTag) {
                    if (thisNode.getNodeName().equals(CDATA_ID)) {
                        oldPath = thisNode.getNodeValue();
                        newPath = remapMotherlodeToThredds(oldPath);
                        thisNode.setNodeValue(newPath);
                    }
                } else if (inPropertyTag) {
                    if (thisNode.getNodeName().equals(CDATA_ID)) {
                        if (thisNode.getNodeValue().equals(
                                DataSource.PROP_RESOLVERURL)) {
                            thisNode = walker.nextNode();
                            while ( !thisNode.getNodeName().equals(
                                    CDATA_ID)) {
                                thisNode = walker.nextNode();
                            }
                            oldPath = thisNode.getNodeValue();
                            newPath = remapMotherlodeToThredds(oldPath);
                            thisNode.setNodeValue(newPath);
                        }
                    }
                }
            }

            thisNode = walker.nextNode();
        }

        return walker.getRoot();

        //TransformerFactory tFactory    = TransformerFactory.newInstance();
        //Transformer        transformer = tFactory.newTransformer();
        //DOMSource          source      = new DOMSource(bundle);
        //Writer             outWriter   = new StringWriter();
        //StreamResult       result      = new StreamResult(outWriter);
        //transformer.transform(source, result);
        //return outWriter.toString();
    }

    /**
     * change urls pointing to motherlode.ucar.edu to
     * use thredds.ucar.edu. Also handles changing the
     * datasetUrlPath if required (i.e. old bundles, testTDS
     * plugin enabled).
     *
     * @param oldPath old url
     *
     * @return new url
     */
    private String remapMotherlodeToThredds(String oldPath) {
        return remapMotherlodeToThredds(oldPath, null);
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
        oldPath = oldPath.replace(":-1/", "/");
        String newPath = oldPath;

        for (Map.Entry<String, String> oldServerName :
                serverRemap.entrySet()) {
            if (oldPath.contains(oldServerName.getKey())) {
                // if old path uses an old server name, like motherlode.ucar.edu, then update with new
                newPath = oldPath.replace(oldServerName.getKey(),
                                          oldServerName.getValue());
                // if ncIdvVersion does not exists, then it was created with a pre tds 4.2 -> 4.3 transition
                // IDV and the url data path likely needs to be updated
                //uncomment next line once 8080 is running 4.3...no need to check
                // path if bundle was created after transition.

                //ToDo: enable ncIdvVersion check once TDS 4.2 no longer running at Unidata
                // for now, assume urlpath needs to be updated

                newPath = remapOldMotherlodeDatasetUrlPath(newPath,
                          oldPathType);
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
     * @return updated url
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

        map = FILES_REMAP_KEY;
        if (oldUrlPath.contains(LATEST_XML_NAME)) {
            oldUrlPath = oldUrlPath.split(LATEST_XML_NAME)[0];
        } else if ((oldUrlPath.contains(TDS_PRE_43_BEST_NAME))
                   || (oldUrlPath.contains(TDS_PRE_43_BEST_NAME_SUFFIX))) {
            map = BEST_REMAP_KEY;
        } else {
            String[] parts = oldUrlPath.split("/");
            String fileName = parts[parts.length -1];
            oldUrlPath = oldUrlPath.replace(fileName,"");
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
     * @param xml String representation of the xml bundle
     *
     * @return updated Element representation of a bundle
     *
     * @throws IOException _more_
     * @throws SAXException _more_
     * @throws TransformerException _more_
     */
    public Element remapUrlsInBundle(String xml)
            throws TransformerException, SAXException, IOException {
        Node bundleNode = bundleWalker(xml);

        return (Element) bundleNode;

    }

}
