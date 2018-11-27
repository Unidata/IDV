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


import org.w3c.dom.Element;

import thredds.util.UnidataTdsDataPathRemapper;

import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.grid.DodsGeoGridDataSource;
import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.util.CatalogUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.ResourceCollection;
import ucar.unidata.xml.XmlResourceCollection;
import ucar.unidata.xml.XmlUtil;

import java.io.IOException;

import java.util.*;


/**
 * A class to handle remapping variable names from data sources as
 * they are unpersisted from bundles
 */

public class VariableRenamer {

    /** Reference to the IDV */
    private IdvResourceManager resourceManager;

    /** Reference to the Variable Renaming Map Resource Collection */
    private XmlResourceCollection varmapResourceCollection;

    /** Xml tag for variable name maps xml */
    private static final String TAG_VARRENAMER = "varrenamer";

    /** URL Maps (oldVar : newVar} */
    private HashMap<String, List<String>> varMaps = new HashMap<String, List<String>>();

    /**
     * Read in the resource xml files and store the URL maps
     */
    private void readVariableRenameResources() {

        for (int varRemapResourceIdx = 0;
                varRemapResourceIdx < varmapResourceCollection.size();
                varRemapResourceIdx++) {
            Element root =
                this.varmapResourceCollection.getRoot(varRemapResourceIdx,
                    false);

            if (root == null) {
                continue;
            }

            List nodes = XmlUtil.findChildren(root, TAG_VARRENAMER);

            for (Object node1 : nodes) {
                Element node   = (Element) node1;
                String  oldVar = XmlUtil.getAttribute(node, "old");
                String  newVar = XmlUtil.getAttribute(node, "new");

                if (!this.varMaps.containsKey(oldVar)) {
                    List<String> tmpList = new ArrayList<String>();
                    tmpList.add(newVar);
                    varMaps.put(oldVar, tmpList);
                } else {
                    List<String> tmpList = varMaps.get(oldVar);
                    tmpList.add(newVar);
                    varMaps.put(oldVar, tmpList);
                }
            }
        }
    }


    public List<String> renameVar(final String oldName) {
        List<String> newNames = new ArrayList<String>();

        if (varMaps.containsKey(oldName)) {
            newNames = varMaps.get(oldName);
        } else {
            newNames.add(oldName);
        }

        return newNames;
    }


    /**
     * initilize the ServerUrlRemapper (get remaps from resources
     */
    private void init() {

        varmapResourceCollection = this.resourceManager.getXmlResources(
                IdvResourceManager.RSC_VARIABLERENAMER);

        readVariableRenameResources();
    }

    /**
     * Construct a VariableRenamer
     */
    public VariableRenamer(IdvResourceManager rsm) {
        this.resourceManager = rsm;
        init();
    }

}
