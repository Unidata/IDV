/*
 * Copyright 1997-2012 Unidata Program Center/University Corporation for
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
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.LogUtil;

import java.io.IOException;

import java.util.*;


/**
 * A class to handle remapping URLs from data sources as
 * they are unpesisted from bundles
 */

public class ServerUrlRemapper {

    /** ncIdv version */
    private String ncIdvVersion = null;

    /** Reference to the IDV */
    private IntegratedDataViewer idv;


    /**
     * _more_
     *
     * @param name _more_
     * @param dflt _more_
     *
     * @return _more_
     */
    private boolean getProperty(String name, boolean dflt) {
        return idv.getStateManager().getProperty(name, dflt);
    }

    /**
     * Construct a ServerUrlRemapper 
     */
    public ServerUrlRemapper() {}

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
        Boolean testTds = getProperty("tds.update.test", Boolean.FALSE);
        ncIdvVersion = (String) ht.get(IdvConstants.NCIDV_VERSION);
        ArrayList dataSources    = (ArrayList) ht.get("datasources");
        ArrayList newDataSources = new ArrayList();
        for (int i = 0; i < dataSources.size(); i++) {
            DataSource dataSource = (DataSource) dataSources.get(i);
            // update motherlode references to thredds
            // this should happen regardless of version of ncIDV
            DataSource remappedDataSource =
                remapMotherlodeToThredds(dataSource);

            newDataSources.add(remappedDataSource);
        }
        ht.put("datasources", newDataSources);

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
        String                  oldServer   = "motherlode.ucar.edu/";
        Boolean testTds = getProperty("tds.update.test", Boolean.FALSE);
        if (testTds) {
            LogUtil.println(
                "INFO: Forcing TDS 4.3 connections to get remote data.");
        }

        if (testTds) {
            serverRemap.put(oldServer, "thredds-test.ucar.edu/");
            serverRemap.put(oldServer.replace("/", ":8080/"),
                            "thredds-test.ucar.edu/");
            serverRemap.put("thredds.ucar.edu/", "thredds-test.ucar.edu/");

        } else {
            serverRemap.put(oldServer, "thredds.ucar.edu/");
            serverRemap.put(oldServer.replace("/", ":8080/"),
                            "thredds.ucar.edu/");
        }
        serverRemap.put(oldServer.replace("/", ":8081/"),
                        "thredds-test.ucar.edu/");
        serverRemap.put(oldServer.replace("/", ":9080/"),
                        "thredds-dev.ucar.edu/");

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
                    if ((ncIdvVersion == null) || (testTds)) {
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
        Boolean testTds = getProperty("tds.update.test", Boolean.FALSE);
        UnidataTdsDataPathRemapper remapper =
            new UnidataTdsDataPathRemapper();
        // grab dataSource URL
        if (dataSource instanceof DodsGeoGridDataSource) {
            ArrayList oldUrls  = (ArrayList) dataSource.getDataPaths();
            String    oldUrl   = newPath;
            String[]  breakUrl = null;
            if (oldUrl.contains("catalog/")) {
                breakUrl = oldUrl.split("catalog/");
            } else if (oldUrl.contains("dodsC/")) {
                breakUrl = oldUrl.split("dodsC/");
            }

            String oldUrlPath;

            if (breakUrl.length == 2) {
                oldUrlPath = breakUrl[1];
            } else {
                return dataSource;
            }
            String map = null;
            if (oldUrlPath.contains("latest.xml")) {
                oldUrlPath = oldUrlPath.split("latest.xml")[0];
                map        = "latest";
            } else if ((oldUrlPath.contains("best"))
                       || (oldUrlPath.contains(".ncd"))) {
                map = "best";
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
     * @param dataSource DataSource object whoes properties need to be updated
     *
     * @param newPath new URL string
     *
     * @return updated DataSource
     */

    private DataSource updatePropsWithRemapUrl(DataSource dataSource,
            String newPath) {

        Boolean testTds = getProperty("tds.update.test", Boolean.FALSE);

        if (newPath.contains("latest.xml")) {

            Hashtable props =
                ((DodsGeoGridDataSource) dataSource).getProperties();
            if (props.containsKey("prop.service.http")) {
                String newHttpPath = CatalogUtil.resolveUrl(newPath,
                                         null).replace("/dodsC/",
                                             "/fileServer/");
                ((DodsGeoGridDataSource) dataSource).setProperty(
                    "prop.service.http", newHttpPath);
            }

            if (props.containsKey("RESOLVERURL")) {
                ((DodsGeoGridDataSource) dataSource).setProperty(
                    "RESOLVERURL", newPath);
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
