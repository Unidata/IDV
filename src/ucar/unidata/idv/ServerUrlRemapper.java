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


import org.w3c.dom.Element;

import thredds.util.UnidataTdsDataPathRemapper;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.grid.DodsGeoGridDataSource;
import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
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

    /** TDS Service name for the http fileServer */
    private static final String TDS_HTTP_SERVICE = "/fileServer/";

    /** TDS Service name for catalogs */
    private static final String TDS_CATALOG_SERVICE = "/catalog/";

    /** Datasource ID used for unpersistence */
    private static final String ID_DATASOURCES = IdvConstants.ID_DATASOURCES;

    /** old url used to access TDS (deprecated) */
    private static final String URL_MOTHERLODE = "motherlode.ucar.edu/";

    /** url for stable Unidata TDS */
    private static final String URL_TDS = "thredds.ucar.edu/";

    /** url for test Unidata TDS */
    private static final String URL_TDS_TEST = "thredds-test.ucar.edu/";

    /** port number for stable Unidata TDS */
    private static final String PORT_TDS = ":8080/";

    /** port number for stable Unidata TDS */
    private static final String PORT_BUNDLE = ":-1/";

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
     * Read in the resource xml files and store the URL maps
     */
    private void readUrlRemapResources() {

        for (int urlRemapResourceIdx = 0;
                urlRemapResourceIdx < urlmapResourceCollection.size(); urlRemapResourceIdx++) {
            Element root =
                this.urlmapResourceCollection.getRoot(urlRemapResourceIdx,
                    false);

            if (root == null) {
                continue;
            }

            List nodes = XmlUtil.findChildren(root, TAG_URLMAP);

            for (Object node1 : nodes) {
                Element node = (Element) node1;
                final String urlType = XmlUtil.getAttribute(node, "type");
                String tmpOldUrl = XmlUtil.getAttribute(node, "old");
                String tmpNewUrl = XmlUtil.getAttribute(node, "new");
                if (!tmpOldUrl.endsWith("/")) {
                    tmpOldUrl = tmpOldUrl + '/';
                }

                if (!tmpNewUrl.endsWith("/")) {
                    tmpNewUrl = tmpNewUrl + '/';
                }
                final String oldUrl = tmpOldUrl;
                final String newUrl = tmpNewUrl;

                if (this.urlMaps.containsKey(urlType)) {
                    HashMap<String, String> tmpUrlMap = urlMaps.get(urlType);
                    if (!tmpUrlMap.containsKey(oldUrl)) {
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
     * initilize the ServerUrlRemapper (get remaps from resources
     */
    private void init() {

        urlmapResourceCollection =
            idv.getResourceManager().getXmlResources(
                IdvResourceManager.RSC_URLMAPS);

        readUrlRemapResources();
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
     * Construct a ServerUrlRemapper
     *
     * @param idv instance of the IDV
     */
    public ServerUrlRemapper(IntegratedDataViewer idv) {
        this.idv = idv;
        init();
    }

    /**
     * This method handles remapping issues for data server URL changes
     *
     * @param data Object from bundle unpersistence
     *
     * @return Object
     */
    public Object remapDataSources(Object data) {

        if (data instanceof Hashtable) {
            data = remapDataSources((Hashtable) data);
        } else if (data instanceof DataSource) {
            //ToDo: Add code to handle different url types (adde, tds, etc.)
            if (data instanceof DodsGeoGridDataSource) {
                data = remapMotherlodeToThredds((DataSource) data);
            }
        }

        return data;
    }

    /**
     * This method handles remapping issues for data server URL changes
     *
     * @param ht Contains the unpersisted objects
     *
     * @return ht Contains unpersisted objects with remaped URLs, if needed.
     */
    private Hashtable remapDataSources(Hashtable ht) {
        Boolean testTds = getProperty(TEST_TDS_43_UPDATE, Boolean.FALSE);
        if (ht.containsKey(IdvConstants.ID_NCIDV_VERSION)) {
            ncIdvVersion = (String) ht.get(IdvConstants.ID_NCIDV_VERSION);
        }
        List             dataSources    = (List) ht.get(ID_DATASOURCES);
        List<DataSource> newDataSources = new ArrayList<DataSource>();
        for (Object dataSource1 : dataSources) {
            DataSource dataSource = (DataSource) dataSource1;
            //ToDo: Add code to handle different url types (adde, tds, etc.)
            if (dataSource instanceof DodsGeoGridDataSource) {
                // update motherlode references to thredds
                // this should happen regardless of version of ncIDV
                DataSource remappedDataSource =
                        remapMotherlodeToThredds(dataSource, ncIdvVersion);

                newDataSources.add(remappedDataSource);
            } else {
                //the case where remapping isn't needed or
                // remapping for a specific dataSource isn't
                // accounted for yet...
                newDataSources.add(dataSource);
            }
        }
        ht.put(ID_DATASOURCES, newDataSources);

        return ht;
    }


    /**
     * Method to change old urls that point to motherlode to the appropriate
     * new server using the thredds*.ucar.edu domain.
     *
     * This call is for the case where ncIdvVersion is unknown or not supplied.
     *
     * @param dataSource DataSource object that may need to be updated
     *
     * @return updated DataSource
     */
    private DataSource remapMotherlodeToThredds(DataSource dataSource) {
        return remapMotherlodeToThredds(dataSource, UNKNOWN_NCIDV_VERSION);
    }

    /**
     * Method to change old urls that point to motherlode to the appropriate
     * new server using the thredds*.ucar.edu domain.
     *
     * @param dataSource DataSource object that may need to be updated
     * @param ncIdvVersion Version of ncIdv.jar that was used when the bundle was created.
     *
     * @return updated DataSource
     */

    private DataSource remapMotherlodeToThredds(DataSource dataSource,
            String ncIdvVersion) {
        String                  updatedPath;
        Boolean testTds = getProperty(TEST_TDS_43_UPDATE, Boolean.FALSE);
        HashMap<String, String> serverRemap = this.urlMaps.get("opendap");

        List oldPaths = dataSource.getDataPaths();

        if (oldPaths.size() != 0) {
            LogUtil.println("multiple paths for a single dataSource...do not know how to handle!");
        }
        for (Map.Entry<String, String> oldServerName :
                serverRemap.entrySet()) {
            String oldPath = (String) oldPaths.get(0);
            oldPath = oldPath.replace(":-1/","/");

            if (oldPath.contains(oldServerName.getKey())) {
                // if old path uses an old server name, like motherlode.ucar.edu, then update with new
                updatedPath = oldPath.replace(oldServerName.getKey(),
                        oldServerName.getValue());
                // if ncIdvVersion does not exists, then it was created with a pre tds 4.2 -> 4.3 transition
                // IDV and the url data path likely needs to be updated
                //uncomment next line once 8080 is running 4.3
                //ToDo: enable ncIdvVersion check once thredds.ucar.edu -> 4.3
                //if ((ncIdvVersion != UNKNOWN_NCIDV_VERSION) || (testTds)) {
                if (testTds) {
                    dataSource = remapOldMotherlodeDatasetUrlPath(dataSource,
                            updatedPath);
                } else {
                    dataSource = updatePropsWithRemapUrl(dataSource,
                            updatedPath);
                }
                break;
            }
        }

        return dataSource;
    }

    /**
     * This method addresses changes to dataset urlPath changes between
     * Unidata TDS 4.2 and 4.3. In particular, things like naming of the
     * best datasets, frmc collections moving to grib collects, etc.
     *
     * @param dataSource DataSource object that may need to be updated
     * @param oldUrl URL whose data path may need to be updated
     *
     * @return updated DataSource
     */
    private DataSource remapOldMotherlodeDatasetUrlPath(
            DataSource dataSource, String oldUrl) {
        // this is where the fmrc -> grib magic will happen
        UnidataTdsDataPathRemapper remapper =
            new UnidataTdsDataPathRemapper();
        // grab dataSource URL
        if (dataSource instanceof DodsGeoGridDataSource) {
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
                return dataSource;
            }
            String map = null;
            if (oldUrlPath.contains(LATEST_XML_NAME)) {
                oldUrlPath = oldUrlPath.split(LATEST_XML_NAME)[0];
                map        = LATEST_REMAP_KEY;
            } else if ((oldUrlPath.contains(TDS_PRE_43_BEST_NAME))
                       || (oldUrlPath.contains(
                           TDS_PRE_43_BEST_NAME_SUFFIX))) {
                map = BEST_REMAP_KEY;
            }

            List<String> newUrlPaths = remapper.getMappedUrlPaths(oldUrlPath,
                                           map);
            if ((newUrlPaths != null) && (newUrlPaths.size() == 1)) {
                String newUrlPath = newUrlPaths.get(0);
                String newUrl     = oldUrl.replace(oldUrlPath, newUrlPath);
                dataSource = updatePropsWithRemapUrl(dataSource, newUrl);
            }
        }

        return dataSource;
    }


    /**
     * This methods updates the properties that I *think* matter when updating
     * the data url...this is questionable, but works for now.
     *
     * @param dataSource DataSource object whose properties need to be updated
     *
     * @param newPath new URL string
     *
     * @return updated DataSource
     */

    private DataSource updatePropsWithRemapUrl(DataSource dataSource,
            String newPath) {

        if (newPath.contains(LATEST_XML_NAME)) {

            Hashtable props =
                ((DodsGeoGridDataSource) dataSource).getProperties();
            if (props.containsKey(GeoGridDataSource.PROP_SERVICE_HTTP)) {
                String newHttpPath = CatalogUtil.resolveUrl(newPath,
                                         null).replace(TDS_DODS_SERVICE,
                                             TDS_HTTP_SERVICE);
                ((DodsGeoGridDataSource) dataSource).setProperty(
                    DataSource.PROP_SERVICE_HTTP, newHttpPath);
            }

            if (props.containsKey(DataSource.PROP_RESOLVERURL)) {
                ((DodsGeoGridDataSource) dataSource).setProperty(
                    DataSource.PROP_RESOLVERURL, newPath);
            }
        } else {
            Hashtable props =
                ((DodsGeoGridDataSource) dataSource).getProperties();
            DataSourceDescriptor descript =
                ((DodsGeoGridDataSource) dataSource).getDescriptor();

            try {
                dataSource = new DodsGeoGridDataSource(descript, newPath,
                        props);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return dataSource;
    }
}
