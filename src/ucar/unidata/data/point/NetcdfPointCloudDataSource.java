package ucar.unidata.data.point;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.unidata.data.*;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.DateSelection;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import visad.Data;
import visad.FieldImpl;
import visad.VisADException;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class NetcdfPointCloudDataSource extends NetcdfPointDataSource{
    List<VariableSimpleIF> varNames = new ArrayList<VariableSimpleIF>();
    /**
     * Default constructor
     *
     * @throws VisADException  problem creating the object
     */
    public NetcdfPointCloudDataSource() throws VisADException {
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
    public NetcdfPointCloudDataSource(FeatureDatasetPoint fixedDataset,
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
    public NetcdfPointCloudDataSource(DataSourceDescriptor descriptor,
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
    public NetcdfPointCloudDataSource(DataSourceDescriptor descriptor,
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
    public NetcdfPointCloudDataSource(DataSourceDescriptor descriptor,
                                 List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, properties);
    }

    protected FeatureDatasetPoint doMakeDataset(String file) {
        FeatureDatasetPoint pods = null;
        pods     = super.doMakeDataset(file);
        varNames = new ArrayList<VariableSimpleIF>();

        return pods;
    }

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
            obs = PointObFactory.makePointObs(pods, dataChoice, getBinRoundTo(),
                    getBinWidth(), bbox, ds, sample);
            if (super.fixedDataset == null) {
                pods.close();
            }
            varNames = pods.getDataVariables();
        }
        Trace.call2("NetcdfPointDatasource:makeObs");
        return obs;
    }


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

                DataChoice choice = new DirectDataChoice(this,
                        "pointcloud:" + varname, varname,
                        varname, cloudCats, props);
                addDataChoice(choice);

            }

        } catch (Exception exc) {
            logException("Creating track choices", exc);
        }
    }

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
                 /*   DataChoice choice = new DirectDataChoice(this,
                            new Integer(i), "", "",
                            dataChoice.getCategories(),
                            properties); */
                    pointObs = (FieldImpl) super.getDataInner(dataChoice, category,
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
