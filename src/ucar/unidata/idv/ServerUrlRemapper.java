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


import thredds.util.UnidataTdsDataPathRemapper;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.grid.DodsGeoGridDataSource;
import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.LogUtil;

import java.io.IOException;

import java.util.*;


/**
 * A class to handle remapping URLs from data sources as
 * they are unpersisted from bundles
 */

public class ServerUrlRemapper {

    /** ncIdv version */
    private String ncIdvVersion = null;

    /** Reference to the IDV */
    private IntegratedDataViewer idv;

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

    /** Datasource ID used for unpersistance */
    private static final String ID_DATASOURCES = IdvConstants.ID_DATASOURCES;

    /** old url used to access TDS (deprecated) */
    private static final String URL_MOTHERLODE = "motherlode.ucar.edu/";

    /** url for stable Unidata TDS */
    private static final String URL_TDS = "thredds.ucar.edu/";

    /** url for test Unidata TDS */
    private static final String URL_TDS_TEST = "thredds-test.ucar.edu/";

    /** URL for development snapshot Unidata TDS */
    private static final String URL_TDS_DEV = "thredds-dev.ucar.edu/";

    /** port number for stable Unidata TDS */
    private static final String PORT_TDS = ":8080/";

    /** port number for test Unidata TDS */
    private static final String PORT_TDS_TEST = ":8081/";

    /** port number for development snapshot Unidata TDS */
    private static final String PORT_TDS_DEV = ":9080/";


    /**
     * thin wrapper to get IDV property
     *
     * @param name name of property
     * @param dflt default value
     *
     * @return property
     */
    private boolean getProperty(String name, boolean dflt) {
        return idv.getStateManager().getProperty(name, dflt);
    }

    /**
     * Construct a ServerUrlRemapper
     *
     * @param idv instance of the IDV
     */
    public ServerUrlRemapper(IntegratedDataViewer idv) {
        this.idv = idv;
    }

    /**
     * main method to handle remapping urls
     *
     * @param data Object
     *
     * @return Object
     */
    public Object remapDataSources(Object data) {

        if (data instanceof Hashtable) {
            data = remapDataSources((Hashtable) data);
        } else if (data instanceof DataSource) {
            data = (Object) remapMotherlodeToThredds((DataSource) data);
        }

        return data;
    }

    /**
     * This method handles all fo the remapping issues for URL changes related
     * to the Unidata THREDDS server (formally known as motherlode)
     *
     * @param ht Contains the unpersisted objects
     *
     * @return ht Contains unpersisted objects with remaped URLs, if needed.
     */
    private Hashtable remapDataSources(Hashtable ht) {
        Boolean testTds = getProperty(TEST_TDS_43_UPDATE, Boolean.FALSE);
        ncIdvVersion = (String) ht.get(IdvConstants.NCIDV_VERSION);
        ArrayList dataSources    = (ArrayList) ht.get(ID_DATASOURCES);
        ArrayList newDataSources = new ArrayList();
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            // update motherlode references to thredds
            // this should happen regardless of version of ncIDV
            DataSource remappedDataSource =
                remapMotherlodeToThredds(dataSource);

            newDataSources.add(remappedDataSource);
        }
        ht.put(ID_DATASOURCES, newDataSources);

        return ht;
    }

    /**
     * Method to change old urls that point to motherlode to the appropriate
     * new server using the thredds*.ucar.edu domain.
     *
     * @param dataSource DataSource object that may need to be updated
     *
     * @return updated DataSource
     */
    private DataSource remapMotherlodeToThredds(DataSource dataSource) {
        String                  newPath     = null;
        HashMap<String, String> serverRemap = new HashMap<String, String>();
        String                  oldServer   = URL_MOTHERLODE;
        Boolean testTds = getProperty(TEST_TDS_43_UPDATE, Boolean.FALSE);
        if (testTds) {
            LogUtil.println(
                "INFO: Forcing TDS 4.3 connections to get remote data.");
        }

        if (testTds) {
            serverRemap.put(oldServer, URL_TDS_TEST);
            serverRemap.put(oldServer.replace("/", PORT_TDS), URL_TDS_TEST);
            serverRemap.put(URL_TDS, URL_TDS_TEST);

        } else {
            serverRemap.put(oldServer, URL_TDS);
            serverRemap.put(oldServer.replace("/", PORT_TDS), URL_TDS);
        }
        serverRemap.put(oldServer.replace("/", PORT_TDS_TEST), URL_TDS_TEST);

        serverRemap.put(oldServer.replace("/", PORT_TDS_DEV), URL_TDS_DEV);

        if (dataSource instanceof DodsGeoGridDataSource) {
            ArrayList oldPaths = (ArrayList) dataSource.getDataPaths();

            for (Map.Entry<String, String> oldServerName :
                    serverRemap.entrySet()) {
                String oldPath = (String) oldPaths.get(0);
                if (oldPath.contains(oldServerName.getKey())) {
                    newPath = oldPath.replace(oldServerName.getKey(),
                            oldServerName.getValue());
                    // remap urlPaths that point to old unidata TDS ( < 4.3)
                    // if ncIdvVersion exists, then it was created with a post tds 4.2 -> 4.3 transition
                    // and the path likely needs to be updated
                    //uncomment next line once 8080 is running 4.3
                    //if ((ncIdvVersion == null) || (testTds)) {
                    if (testTds) {
                        dataSource =
                            remapOldMotherlodeDatasetUrlPath(dataSource,
                                newPath);
                    } else {
                        dataSource = updatePropsWithRemapUrl(dataSource,
                                newPath);
                    }
                    break;
                }
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
     * @param newPath _more_
     *
     * @return updated DataSource
     */
    private DataSource remapOldMotherlodeDatasetUrlPath(
            DataSource dataSource, String newPath) {
        // this is where the fmrc -> grib magic will happen
        UnidataTdsDataPathRemapper remapper =
            new UnidataTdsDataPathRemapper();
        // grab dataSource URL
        if (dataSource instanceof DodsGeoGridDataSource) {
            String    oldUrl   = newPath;
            String[]  breakUrl = null;
            if (oldUrl.contains(TDS_CATALOG_SERVICE)) {
                breakUrl = oldUrl.split(TDS_CATALOG_SERVICE);
            } else if (oldUrl.contains(TDS_DODS_SERVICE)) {
                breakUrl = oldUrl.split(TDS_DODS_SERVICE);
            }

            String oldUrlPath;

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
                       || (oldUrlPath.contains(TDS_PRE_43_BEST_NAME_SUFFIX))) {
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
