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

package ucar.unidata.data.point;


import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;

import ucar.unidata.data.*;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.DateSelection;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;

import visad.Data;
import visad.FieldImpl;
import visad.VisADException;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Hashtable;
import java.util.List;


/**
 * Created by yuanho on 9/14/16.
 */
public class WRFHDNetcdfDataSource extends NetcdfPointDataSource {

    /** var names */
    List<VariableSimpleIF> varNames = new ArrayList<VariableSimpleIF>();

    /**
     * Default constructor
     *
     * @throws VisADException  problem creating the object
     */
    public WRFHDNetcdfDataSource() throws VisADException {
        init();
    }


    /**
     * Create a new WRFHDNetcdfDataSource
     *
     *
     * @param fixedDataset  the data source
     * @param descriptor    data source descriptor
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public WRFHDNetcdfDataSource(FeatureDatasetPoint fixedDataset,
                                 DataSourceDescriptor descriptor,
                                 Hashtable properties)
            throws VisADException {

        super(descriptor, Misc.toList(new String[] { "" }), properties);
    }


    /**
     * Create a new WRFHDNetcdfDataSource
     *
     * @param descriptor    data source descriptor
     * @param source        source of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public WRFHDNetcdfDataSource(DataSourceDescriptor descriptor,
                                 String source, Hashtable properties)
            throws VisADException {
        super(descriptor, Misc.toList(new String[] { source }), properties);
    }


    /**
     * Create a new WRFHDNetcdfDataSource
     *
     * @param descriptor    data source descriptor
     * @param sources      sources of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public WRFHDNetcdfDataSource(DataSourceDescriptor descriptor,
                                 String[] sources, Hashtable properties)
            throws VisADException {
        super(descriptor, Misc.toList(sources), properties);
    }


    /**
     * Create a new WRFHDNetcdfDataSource
     *
     * @param descriptor    data source descriptor
     * @param sources        List source of data (filenames/URLs)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public WRFHDNetcdfDataSource(DataSourceDescriptor descriptor,
                                 List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, properties);
    }


    /**
     * _more_
     *
     * @param file _more_
     *
     * @return _more_
     */
    protected FeatureDatasetPoint doMakeDataset(String file) {
        FeatureDatasetPoint pods = null;
        pods     = super.doMakeDataset(file);
        varNames = new ArrayList<VariableSimpleIF>();

        return pods;
    }


    /**
     * make the obs
     *
     * @param dataChoice the datachoice
     * @param subset teh data selection
     * @param bbox the bbox
     * @param sample just a taste?
     *
     * @return the obs
     *
     * @throws Exception on badness
     */
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox, boolean sample)
            throws Exception {
        Object id = dataChoice.getId();
        String source;
        if (id instanceof Integer) {
            source = (String) sources.get(((Integer) id).intValue());
        } else if ((id instanceof List) && sample) {
            source = (String) sources.get(0);
        } else if (id instanceof String) {
            source = (String) sources.get(0);
        } else {
            source = id.toString();
        }
        //System.err.println ("reading data from:" + source);

        FieldImpl obs = null;
        Trace.call1("NetcdfPointDatasource:makeObs");
        if (obs == null) {
            FeatureDatasetPoint pods = getDataset(source);
            if (pods == null) {
                return null;
            }
            DateSelection ds =
                (DateSelection) getProperty(DataSelection.PROP_DATESELECTION);
            obs = PointObFactory.makePointObsWRF(pods, getBinRoundTo(),
                    getBinWidth(), bbox, ds, sample);
            if (super.fixedDataset == null) {
                pods.close();
            }
            varNames = pods.getDataVariables();
        }
        Trace.call2("NetcdfPointDatasource:makeObs");
        return obs;
    }

    /**
     * _more_
     */
    public void doMakeDataChoices() {
        super.doMakeDataChoices();

        try {
            if (getDataChoices().size() == 0) {
                return;
            }
            DataChoice dataChoice = (DataChoice) getDataChoices().get(0);
            //Sample the data to see if we need to show the metadata gui
            Hashtable props = Misc.newHashtable("doFilter", "true");

            List cloudCats =
                DataCategory.parseCategories("Point Cloud;pointcloud", true);
            for (VariableSimpleIF var : varNames) {
                String varname = var.getShortName();
                if (varname.equalsIgnoreCase("streamflow")) {
                    DataChoice choice = new DirectDataChoice(this,
                                            "pointcloud:" + varname, varname,
                                            varname, cloudCats, props);
                    addDataChoice(choice);
                }
            }

        } catch (Exception exc) {
            logException("Creating track choices", exc);
        }
    }

    /**
     * _more_
     *
     * @param dataChoice _more_
     * @param category _more_
     * @param dataSelection _more_
     * @param requestProperties _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {

        Object id = dataChoice.getId();

        if ((id instanceof String)
                && (id.toString().startsWith("pointcloud:"))) {
            try {
                Hashtable properties = dataChoice.getProperties();
                if (properties == null) {
                    properties = new Hashtable();
                }
                FieldImpl pointObs = null;
                List      datas    = new ArrayList();
                for (int i = 0; i < sources.size(); i++) {
                    DataChoice choice = new DirectDataChoice(this,
                                            new Integer(i), "", "",
                                            dataChoice.getCategories(),
                                            properties);
                    pointObs = (FieldImpl) getDataInner(choice, category,
                            dataSelection, requestProperties);
                    if (pointObs != null) {
                        datas.add(pointObs);
                    }
                }
                if (datas.size() == 0) {
                    return null;
                }
                pointObs = PointObFactory.mergeData(datas);
                if (pointObs == null) {
                    return null;
                }

                FieldImpl cloud = PointObFactory.makePointCloud(pointObs,
                                      id.toString().substring(11));
                return cloud;
            } catch (Exception exc) {
                logException("Creating point cloud", exc);
                return null;
            }
        }
        return (FieldImpl) super.getDataInner(dataChoice, category,
                dataSelection, requestProperties);
    }

}
