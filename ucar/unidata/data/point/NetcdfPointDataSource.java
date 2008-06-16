/**
 * $Id: NetcdfPointDataSource.java,v 1.32 2007/07/31 19:29:16 jeffmc Exp $
 *
 * Copyright 1997-2005 Unidata Program Center/University Corporation for
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


import ucar.nc2.dt.PointObsDataset;
import ucar.nc2.dt.TypedDatasetFactory;

import ucar.unidata.data.*;


import ucar.unidata.geoloc.LatLonRect;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.unidata.util.WrapperException;

import visad.*;

import visad.data.netcdf.Plain;

import visad.georef.EarthLocation;
import visad.georef.EarthLocationTuple;

import java.net.MalformedURLException;

import java.net.URL;

import java.rmi.RemoteException;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * A data source for netCDF or CDM point data
 *
 * @author Don Murray
 * @version $Revision: 1.32 $ $Date: 2007/07/31 19:29:16 $
 */
public class NetcdfPointDataSource extends PointDataSource {


    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(NetcdfPointDataSource.class.getName());

    /** the dataset */
    private PointObsDataset dataset;


    /**
     * Default constructor
     *
     * @throws VisADException  problem creating the object
     */
    public NetcdfPointDataSource() throws VisADException {
        init();
    }

    /**
     * Create a new NetcdfPointDataSource
     *
     * @param descriptor    data source descriptor
     * @param source        source of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public NetcdfPointDataSource(DataSourceDescriptor descriptor,
                                 String source, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.toList(new String[] { source }), properties);
    }


    /**
     * Create a new NetcdfPointDataSource
     *
     * @param descriptor    data source descriptor
     * @param sources      sources of data (filename/URL)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public NetcdfPointDataSource(DataSourceDescriptor descriptor,
                                 String[] sources, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.toList(sources), properties);
    }


    /**
     * Create a new NetcdfPointDataSource
     *
     * @param descriptor    data source descriptor
     * @param sources        List source of data (filenames/URLs)
     * @param properties    extra properties for initialization
     *
     * @throws VisADException   problem creating the data
     *
     */
    public NetcdfPointDataSource(DataSourceDescriptor descriptor,
                                 List sources, Hashtable properties)
            throws VisADException {
        super(descriptor, sources, "Netcdf Point Data", properties);
    }



    /**
     * Can this datasource do the geoselection subsetting and decimation
     *
     * @return _can do geo subsetting
     */
    public boolean canDoGeoSelection() {
        return true;
    }

    /**
     * Used for the geo subsetting property gui as to whether to
     * show the stride or not
     *
     * @return default is true
     */
    protected boolean canDoGeoSelectionStride() {
        return false;
    }

    /**
     * Is this data source capable of saving its data to local disk
     *
     * @return Can save to local disk
     */
    public boolean canSaveDataToLocalDisk() {
        return !isFileBased();
    }




    /**
     * Check to see if this NetcdfPointDataSource is equal to the object
     * in question.
     *
     * @param o  object in question
     *
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof NetcdfPointDataSource)) {
            return false;
        }
        NetcdfPointDataSource that = (NetcdfPointDataSource) o;
        return (this == that);
    }

    /**
     * Get the hashcode for this object
     * @return  hash code
     */
    public int hashCode() {
        int hashCode = getName().hashCode();
        return hashCode;
    }

    /**
     * Initialize after we have been created.
     */
    public void initAfterCreation() {
        super.initAfterCreation();
        //Call getDataset to see if we have a valid file
        getDataset();
    }


    /**
     * Return the GridDataset associated with this DataSource.
     *
     * @return dataset
     */
    public PointObsDataset getDataset() {
        if (dataset == null) {
            Trace.call1("NetcdfPointDataSource.getDataSet",
                        " name = " + sources);
            dataset = doMakeDataset();
            Trace.call2("NetcdfPointDataSource.getDataSet");
        }
        return dataset;
    }

    /**
     * Make the dataset
     *
     * @return the dataset
     */
    protected PointObsDataset doMakeDataset() {
        String file = getFilePath();
        if (file == null) {
            if (haveBeenUnPersisted) {
                file = getName();
            }
        }
        if (file == null) {
            return null;
        }
        if (sources == null) {
            sources = new ArrayList();
            sources.add(file);
        }
        StringBuilder    buf     = new StringBuilder();
        PointObsDataset pods    = null;
        Exception       toThrow = new Exception("Datset is null");
        try {
            pods = (PointObsDataset) TypedDatasetFactory.open(
                ucar.nc2.constants.FeatureType.POINT, file, null, buf);
        } catch (Exception exc) {
            pods = null;
        }
        if (pods == null) {
            toThrow =
                new BadDataException("Unable to make a PointObsDataset from "
                                     + file + "\nError = " + buf.toString());
            setInError(true);
            throw new WrapperException(
                "Point obs data source failed making data set: " + file,
                toThrow);
        }
        return pods;
    }


    /**
     * Make PointObs from the PointObsDataset
     *
     * @param dataChoice   choice for data (source of data)
     * @param subset       subsetting parameters
     * @param bbox bounding box. may be null
     * @return  data of the form index -> (EarthLocation, Time, Data)
     *
     * @throws Exception  problem creating data
     */
    protected FieldImpl makeObs(DataChoice dataChoice, DataSelection subset,
                                LatLonRect bbox)
            throws Exception {
        Object id = dataChoice.getId();
        String    source;
        if(id instanceof Integer) {
            source = (String)sources.get(((Integer)id).intValue());
        } else {
            source = id.toString();
        }
        FieldImpl obs    = null;
        Trace.call1("NetcdfPointDatasource:makeObs");
        if (obs == null) {
            //TODO: We are nulling out the data set to fix a bug where we cannot
            //use the same data set twice in a row
            this.dataset = null;




            PointObsDataset pods = getDataset();
            if (pods == null) {
                return null;
            }
            /*
            StringBuilder buf = new StringBuilder();
            PointObsDataset pods = (PointObsDataset) TypedDatasetFactory.open(
                                       ucar.nc2.constants.FeatureType.POINT,
                                       source, null, buf);
            if (pods == null) {
                throw new BadDataException(
                    "Unable to make a PointObsDataset from " + source
                    + "\nError = " + buf.toString());
            }
            */
            obs = PointObFactory.makePointObs(pods, getBinRoundTo(),
                    getBinWidth(), bbox);
            pods.close();
        }
        Trace.call2("NetcdfPointDatasource:makeObs");
        return obs;
    }



    /**
     * test
     *
     * @param args args
     */
    public static void main(String[] args) {
        try {
            StringBuilder buf   = new StringBuilder();
            int          cnt   = ((args.length > 1)
                                  ? new Integer(args[1]).intValue()
                                  : 1);
            long         total = 0;
            for (int i = 0; i < cnt; i++) {
                long tt1 = System.currentTimeMillis();
                PointObsDataset pods =
                    (PointObsDataset) TypedDatasetFactory.open(
                        ucar.nc2.constants.FeatureType.POINT, args[0], null, buf);
                long tt2 = System.currentTimeMillis();
                if (pods == null) {
                    throw new BadDataException(
                        "Unable to make a PointObsDataset from " + args[0]
                        + "\nError = " + buf.toString());
                }
                Trace.startTrace();
                long      t1    = System.currentTimeMillis();
                FieldImpl field = PointObFactory.makePointObs(pods, 0, 0);
                PointObFactory.makeTimeSequenceOfPointObs(field, 0);
                //                PointObFactory.makePointObsOnly(pods, 0, 0,null);
                long t2 = System.currentTimeMillis();

                if (i != 0) {
                    total += (t2 - t1);
                    System.err.println("PointObsDataset time:" + (tt2 - tt1)
                                       + " makePointObs time:" + (t2 - t1)
                                       + " avg:" + (total / i));
                }

            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }

    }



}

