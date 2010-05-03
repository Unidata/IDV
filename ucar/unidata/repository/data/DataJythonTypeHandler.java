/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
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


import org.apache.commons.net.ftp.*;

import org.python.core.*;
import org.python.util.*;


import org.w3c.dom.*;

import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;

import ucar.unidata.data.DataSource;

import ucar.unidata.data.grid.GeoGridDataSource;
import ucar.unidata.repository.*;
import ucar.unidata.repository.data.*;
import ucar.unidata.repository.metadata.*;

import ucar.unidata.repository.output.*;
import ucar.unidata.repository.type.*;

import ucar.unidata.util.HtmlUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Pool;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Class TypeHandler _more_
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class DataJythonTypeHandler extends JythonTypeHandler {


    /**
     * _more_
     *
     * @param repository _more_
     * @param entryNode _more_
     *
     * @throws Exception _more_
     */
    public DataJythonTypeHandler(Repository repository, Element entryNode)
            throws Exception {
        super(repository, entryNode);
        LogUtil.setTestMode(true);
    }


    /**
     * _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    public DataOutputHandler getDataOutputHandler() throws Exception {
        return (DataOutputHandler) getRepository().getOutputHandler(
            DataOutputHandler.OUTPUT_OPENDAP.toString());
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public JythonTypeHandler.ProcessInfo doMakeProcessInfo() {
        return new DataProcessInfo();
    }

    /**
     * _more_
     *
     * @param request _more_
     * @param interp _more_
     * @param info _more_
     * @param processInfo _more_
     * @param theEntry _more_
     *
     * @throws Exception _more_
     */
    protected void processEntry(Request request, PythonInterpreter interp,
                                InputInfo info,
                                JythonTypeHandler.ProcessInfo processInfo,
                                Entry theEntry)
            throws Exception {
        super.processEntry(request, interp, info, processInfo, theEntry);
        DataOutputHandler dataOutputHandler = getDataOutputHandler();
        DataProcessInfo   dataProcessInfo   = (DataProcessInfo) processInfo;

        String            path = dataOutputHandler.getPath(theEntry);
        if (path != null) {
            //Try it as grid first
            GridDataset gds = dataOutputHandler.getGridDataset(theEntry,
                                  path);
            NetcdfDataset     ncDataset  = null;
            GeoGridDataSource dataSource = null;
            interp.set(info.id + "_griddataset", gds);
            processInfo.variables.add(info.id + "_griddataset");
            if (gds == null) {
                //Else try it as a ncdataset
                ncDataset = dataOutputHandler.getNetcdfDataset(theEntry,
                        path);
            } else {
                dataSource = new GeoGridDataSource(gds);
                dataProcessInfo.dataSources.add(dataSource);
            }
            interp.set(info.id + "_datasource", dataSource);
            interp.set(info.id + "_ncdataset", ncDataset);
            processInfo.variables.add(info.id + "_datasource");
            processInfo.variables.add(info.id + "_ncdataset");
            if (ncDataset != null) {
                dataProcessInfo.ncPaths.add(path);
                dataProcessInfo.ncData.add(ncDataset);
            }
        }
    }


    /**
     * _more_
     *
     * @param request _more_
     * @param entry _more_
     * @param interp _more_
     * @param processInfo _more_
     *
     * @throws Exception _more_
     */
    protected void cleanup(Request request, Entry entry,
                           PythonInterpreter interp,
                           JythonTypeHandler.ProcessInfo processInfo)
            throws Exception {
        super.cleanup(request, entry, interp, processInfo);
        DataOutputHandler dataOutputHandler = getDataOutputHandler();
        DataProcessInfo   dataProcessInfo   = (DataProcessInfo) processInfo;
        for (DataSource dataSource : dataProcessInfo.dataSources) {
            dataSource.doRemove();
        }
        for (int i = 0; i < dataProcessInfo.ncPaths.size(); i++) {
            dataOutputHandler.returnNetcdfDataset(
                dataProcessInfo.ncPaths.get(i),
                dataProcessInfo.ncData.get(i));
        }
        for (int i = 0; i < dataProcessInfo.gridPaths.size(); i++) {
            dataOutputHandler.returnGridDataset(
                dataProcessInfo.gridPaths.get(i),
                dataProcessInfo.gridData.get(i));
        }

    }





    /**
     * Class description
     *
     *
     * @version        Enter version here..., Mon, May 3, '10
     * @author         Enter your name here...    
     */
    public static class DataProcessInfo extends JythonTypeHandler
        .ProcessInfo {

        /** _more_          */
        List<String> ncPaths = new ArrayList<String>();

        /** _more_          */
        List<NetcdfDataset> ncData = new ArrayList<NetcdfDataset>();

        /** _more_          */
        List<String> gridPaths = new ArrayList<String>();

        /** _more_          */
        List<GridDataset> gridData = new ArrayList<GridDataset>();

        /** _more_          */
        List<DataSource> dataSources = new ArrayList<DataSource>();
    }




}
