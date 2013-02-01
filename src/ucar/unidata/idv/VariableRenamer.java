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

import java.util.*;


/**
 * A class to handle remapping URLs from data sources as
 * they are unpersisted from bundles
 */

public class VariableRenamer {

    /** Reference to the IDV */
    private IntegratedDataViewer idv;

    /** Reference to the Url Map Resource Collection */
    private XmlResourceCollection varmapResourceCollection;

    /** Xml tag for url maps xml */
    public static final String TAG_VARMAP = "varalias";

    /** URL Maps (oldVar : newVar} */
    private HashMap<String, String> varMaps = new HashMap<String, String>();

    /**
     * Read in the resource xml files and store the URL maps
     */
    private void readVariableRenameResources() {

        for (int urlRemapResourceIdx = 0;
                urlRemapResourceIdx < varmapResourceCollection.size();
                urlRemapResourceIdx++) {
            Element root =
                this.varmapResourceCollection.getRoot(urlRemapResourceIdx,
                    false);

            if (root == null) {
                continue;
            }

            List nodes = XmlUtil.findChildren(root, TAG_VARMAP);

            for (Object node1 : nodes) {
                Element node   = (Element) node1;
                String  oldVar = XmlUtil.getAttribute(node, "old");
                String  newVar = XmlUtil.getAttribute(node, "new");

                if (!this.varMaps.containsKey(oldVar)) {
                    varMaps.put(oldVar, newVar);
                }
            }
        }
    }


    public String renameVar(final String oldName) {
        String newName;

        if (varMaps.containsKey(oldName)) {
            newName = varMaps.get(oldName);
        } else {
            newName = oldName;
        }

        return newName;
    }


    /**
     * initilize the ServerUrlRemapper (get remaps from resources
     */
    private void init() {

        varmapResourceCollection = idv.getResourceManager().getXmlResources(
            IdvResourceManager.RSC_VARIABLEALIASES);

        readVariableRenameResources();
    }

    /**
     * Construct a VariableRenamer
     */
    public VariableRenamer(IntegratedDataViewer idv) {
        this.idv = idv;
        init();
    }

}
