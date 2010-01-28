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

package ucar.unidata.data.radar;


import ucar.ma2.Array;

import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;

import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataUtil;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.ObjectPair;

import ucar.visad.Util;

import visad.*;

import visad.bom.Radar2DCoordinateSystem;
import visad.bom.Radar3DCoordinateSystem;

import visad.data.units.*;

import visad.georef.EarthLocationTuple;

import java.io.IOException;

import java.rmi.RemoteException;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import java.util.Vector;


/**
 * Adapt a netCDF sweepfile to a VisAD data structure.
 *
 * @author MetApps Development Team
 * @version $Revision: 1.15 $ $Date: 2007/04/16 15:40:02 $
 */
public class NetcdfSweepfileAdapter implements RadarAdapter {

    /** the data */
    private DataImpl myData = null;

    /** the nominal radar time */
    private DateTime baseTime = null;

    /** netcdf file */
    private NetcdfDataset ncFile;

    /** name of file */
    private String location;

    /** the data source */
    private DataSourceImpl dataSource;

    /** list of parameters types */
    private RealType[] paramTypes;

    /** 2D domain */
    private GriddedSet radarDomain2d;

    /** 3D domain */
    private GriddedSet radarDomain3d;

    /** list of units */
    private Unit[] units;

    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(NetcdfSweepfileAdapter.class.getName());

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public NetcdfSweepfileAdapter() {}

    /**
     * Create a new NetcdfSweepfileAdapter
     *
     * @param location   location of data (filename or URL)
     *
     * @throws VisADException     VisAD problem
     */
    public NetcdfSweepfileAdapter(String location) throws VisADException {
        this(null, location);
    }

    /**
     * Construct a new DORADE adapter.
     *
     * @param source     DataSource (may be null)
     * @param location   location of data (filename or URL)
     *
     * @throws VisADException problem creating data
     */
    public NetcdfSweepfileAdapter(DataSourceImpl source, String location)
            throws VisADException {

        this.dataSource = source;
        this.location   = location;
        try {
            init();
        } catch (Exception ex) {
            LogUtil.logException("Netcdf sweep error:init", ex);
            throw new VisADException(ex.toString());
        }
    }

    /**
     * Initialize this adapter
     *
     * @throws Exception problem initializing adapter
     */
    protected void init() throws Exception {
        ncFile = NetcdfDataset.openDataset(location);
        loadData(ncFile);
    }


    /**
     *  Implement the interce.
     *  For now lets not do anything
     */
    public void clearCachedData() {}



    /**
     * Get the Data object that this adapter represents
     *
     * @return  the Data object
     */
    public DataImpl getData() {
        if (myData == null) {
            myData = getField(paramTypes[0]);
        }
        return myData;
    }

    /**
     * Get the nominal time (time of first radial)
     *
     * @return  nominal time
     */
    public DateTime getBaseTime() {
        return baseTime;
    }


    /**
     * Get the data for the type supplied.
     *
     * @param type  RealType of the moment
     *
     * @return  the representative data
     * @deprecated  Use getData(DataChoice, DataSelection, Hashtable)
     */
    public DataImpl getField(RealType type) {
        DataImpl imp = null;
        try {
            imp = makeField(type, false);
        } catch (Exception e) {}
        return imp;
    }

    /**
     * Get the data for the given DataChoice and selection criteria.
     * @param dataChoice         DataChoice for selection
     * @param subset             subsetting criteria
     * @param requestProperties  extra request properties
     * @return  the Data object for the request
     *
     * @throws RemoteException couldn't create a remote data object
     * @throws VisADException  couldn't create the data
     */
    public DataImpl getData(DataChoice dataChoice, DataSelection subset,
                            Hashtable requestProperties)
            throws VisADException, RemoteException {

        /*
        if (requestProperties != null) {
            Hashtable timeLabels =
                (Hashtable) requestProperties.get(DataSource.PROP_TIMELABELS);
            if (timeLabels == null) {
                timeLabels = new Hashtable();
                requestProperties.put(DataSource.PROP_TIMELABELS, timeLabels);
            }

            timeLabels.put(getBaseTime(),
                               "Elevation: "
                               + Misc.format(mySweep.getFixedAngle()));
        }
        */

        //
        // determine if we need 2D or 3D data, defaulting to 2D
        //
        if (requestProperties == null) {
            requestProperties = (dataChoice.getProperties() != null)
                                ? dataChoice.getProperties()
                                : new Hashtable();
        }

        String prop2dOr3d =
            (String) requestProperties.get(RadarConstants.PROP_2DOR3D);
        boolean use3d = (prop2dOr3d != null)
                        && prop2dOr3d.equals(RadarConstants.VALUE_3D);
        ObjectPair cacheKey = new ObjectPair(location,
                                             new ObjectPair(dataChoice,
                                                 new Boolean(use3d)));
        FlatField singleSweep = null;
        //TODO: Gotta figure out a better way to cache so that when the CacheManager flushes
        //its cache when memory is getting blown it also flushes  this cache.

        if (dataSource != null) {
            singleSweep = (FlatField) dataSource.getCache(cacheKey);
        }

        if (singleSweep != null) {
            return singleSweep;
        }

        RealType pType = (RealType) dataChoice.getId();

        singleSweep = makeField(pType, use3d);

        if (dataSource != null) {
            dataSource.putCache(cacheKey, singleSweep);
        }

        return singleSweep;
    }

    /**
     * Make a FlatField for the particular parameter
     *
     * @param pType   RealType of the parameter
     * @param use3d   true to create a 3D field
     *
     * @return the data for that parameter
     *
     * @throws RemoteException    Java RMI error
     * @throws VisADException     VisAD error
     */
    private FlatField makeField(RealType pType, boolean use3d)
            throws VisADException, RemoteException {

        GriddedSet radarDomain = use3d
                                 ? radarDomain3d
                                 : radarDomain2d;
        log_.debug(radarDomain.toString());
        RealTupleType domainType =
            ((SetType) radarDomain.getType()).getDomain();

        //
        // extract the parameter from the DataChoice
        //
        int unitIndex = -1;
        for (int i = 0; i < paramTypes.length; i++) {
            if (paramTypes[i].equals(pType)) {
                unitIndex = i;
                break;
            }
        }
        Unit u = units[unitIndex];
        // now create the FlatField
        FunctionType ftype = new FunctionType(domainType, pType);
        FlatField singleSweep = new FlatField(ftype, radarDomain,
                                    (CoordinateSystem[]) null, (Set[]) null,
                                    new Unit[] { u });
        float[][] rangeValues = new float[0][radarDomain.getLength()];
        Array     arr         = null;
        try {
            Variable v = ncFile.findVariable((String) pType.getName());
            arr = v.read();
        } catch (IOException ioe) {
            LogUtil.logException("Netcdf sweep error", ioe);
        }
        singleSweep.setSamples(new float[][] {
            (float[]) arr.get1DJavaArray(Float.TYPE)
        }, false);
        return singleSweep;
    }

    /**
     * Create VisAD objects from the netCDF data
     *
     * @param ncFile    netCDF file to use
     *
     * @throws IOException       problem opening/accessing file
     * @throws RemoteException    Java RMI problem
     * @throws VisADException     VisAD problem
     */
    private void loadData(NetcdfDataset ncFile)
            throws VisADException, RemoteException, IOException {

        // Extract latitude/longitude/altitude
        Real lat = new Real(RealType.Latitude, getValue("Latitude"));
        Real lon = new Real(RealType.Longitude, getValue("Longitude"));
        Real alt = new Real(RealType.Altitude, getValue("Altitude"));
        EarthLocationTuple location = new EarthLocationTuple(lat, lon, alt);
        log_.debug("Station at :" + location);

        // Extract the base time
        baseTime = new DateTime(getValue("base_time"));
        log_.debug("Base Time:" + baseTime);

        // get some of the metadata we need to make the Radar3DCoordinateSystem
        Variable azimuth = ncFile.findVariable("Azimuth");
        int      numRays = azimuth.getShape()[0];
        log_.debug("number of rays = " + numRays);
        Variable elevation = ncFile.findVariable("Elevation");
        float    range     = (float) getValue("Range_to_First_Cell");
        if (Float.isNaN(range)) {
            range = 0;
        }
        float cellSpacing = (float) getValue("Cell_Spacing");  // cell inc
        log_.debug("range = " + range);
        // get maxCells
        Dimension maxCells = ncFile.getRootGroup().findDimension("maxCells");
        int       numGates = maxCells.getLength();
        log_.debug("numGates = " + numGates);

        List   vi     = ncFile.getVariables();
        Vector vars2D = new Vector();  // list of 2D variable names
        for (Iterator iter = vi.iterator(); iter.hasNext(); ) {
            Variable v = (Variable) iter.next();
            if (v.getRank() == 2) {
                // make sure they are the correct ones
                int[] lengths = v.getShape();
                if ((lengths[0] == numRays) && (lengths[1] == numGates)) {
                    vars2D.add(v.getName());
                }
            }
        }

        // Now create the RealTypes for the range
        if (vars2D.isEmpty()) {
            throw new VisADException("No variables to display");
        }
        paramTypes = new RealType[vars2D.size()];
        units      = new Unit[vars2D.size()];
        for (int i = 0; i < vars2D.size(); i++) {
            Variable v2d = ncFile.findVariable((String) vars2D.get(i));
            units[i] = getUnit(v2d);
            paramTypes[i] = RealType.getRealType(v2d.getName(), units[i],
                    (Set) null);
            log_.debug("found param " + paramTypes[i]);
        }

        //
        // Create the coordinate system to translate rng, az, el to
        // lat, lon, altitude
        //
        float centerOfFirstCell = range + 0.5f * cellSpacing;  // to center of cell
        CoordinateSystem rcs3d =
            new Radar3DCoordinateSystem((float) lat.getValue(),  // station lat
            (float) lon.getValue(),  // station lon
            (float) alt.getValue(),  // station alt
            centerOfFirstCell,       // range to first gate
            cellSpacing,             // cell inc
            0.0f,                    // min azimuth (0 = north)
            1.0f,                    // azimuth precision (1 degree)
            0.0f,                    // min altitude
            1.0f);                   // altitude precision
        log_.debug(rcs3d.toString());
        CoordinateSystem rcs2d =
            new Radar2DCoordinateSystem((float) lat.getValue(),
                                        (float) lon.getValue(),
                                        centerOfFirstCell, cellSpacing, 0.0f,
                                        1.0f);  // az bias/scale
        log_.debug(rcs2d.toString());
        Unit[] domUnits3d = new Unit[] { CommonUnit.meter, CommonUnit.degree,
                                         CommonUnit.degree };
        Unit[] domUnits2d = new Unit[] { CommonUnit.meter,
                                         CommonUnit.degree };

        //
        // build an array of domain values
        // 2d case: (range + az) x (numGates * numRays) array
        // 3d case: (range + az + el) x (numGates * numRays) array
        //
        float[][] domainVals3d = new float[3][numRays * numGates];
        float[][] domainVals2d = new float[2][];

        Array     azs          = azimuth.read();
        float[]   azimuths     = (float[]) azs.get1DJavaArray(Float.TYPE);
        Array     els          = elevation.read();
        float[]   elevations   = (float[]) els.get1DJavaArray(Float.TYPE);

        for (int ray = 0; ray < numRays; ray++) {
            for (int cell = 0; cell < numGates; cell++) {
                int elem = ray * numGates + cell;
                domainVals3d[0][elem] = cell;
                domainVals3d[1][elem] = azimuths[ray];
                domainVals3d[2][elem] = elevations[ray];
            }
        }

        //
        // just ranges and azimuths for 2D
        //
        domainVals2d[0] = domainVals3d[0];
        domainVals2d[1] = domainVals3d[1];

        // radar domains, with coordinate systems
        //
        RealTupleType radar3dType = new RealTupleType(RANGE_TYPE,
                                        AZIMUTH_TYPE, ELEVATION_ANGLE_TYPE,
                                        rcs3d, (Set) null);
        RealTupleType radar2dType = new RealTupleType(RANGE_TYPE,
                                        AZIMUTH_TYPE, rcs2d, (Set) null);

        //
        // now create the domains
        //
        try {
            radarDomain3d = (GriddedSet) new Gridded3DSet(radar3dType,
                    domainVals3d, numGates, numRays, (CoordinateSystem) null,
                    domUnits3d, (ErrorEstimate[]) null, false);

            radarDomain2d = (GriddedSet) new Gridded2DSet(radar2dType,
                    domainVals2d, numGates, numRays, (CoordinateSystem) null,
                    domUnits2d, (ErrorEstimate[]) null, false, false);
        } catch (VisADException vae) {
            System.err.println("radarDomain2d: " + radarDomain2d + "\n"
                               + "radarDomain3d: " + radarDomain3d + "\n"
                               + "numGates: " + numGates + "\n" + "numRays: "
                               + numRays + "\n" + "domainVals.length "
                               + domainVals3d.length + "\n"
                               + "domainVals[0].length "
                               + domainVals3d[0].length + "\n");

            throw vae;
        }

    }

    /**
     * Check to see if this <code>NetcdfSweepfileAdapter</code> is equal to
     * the object in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof NetcdfSweepfileAdapter)) {
            return false;
        }
        NetcdfSweepfileAdapter nsa = (NetcdfSweepfileAdapter) o;
        return this.location.equals(nsa.location);
    }

    /**
     * Get the hash code for this object.
     * @return hash code.
     */
    public int hashCode() {
        int hashCode = location.hashCode();
        return hashCode;
    }

    /**
     * Get the parameters for this adapter
     *
     * @return parameters
     */
    protected RealType[] getParams() {
        return paramTypes;
    }

    /**
     * Get the value of a scalar variable
     *
     * @param name variable name
     *
     * @return scalar value.
     */
    private double getValue(String name) {
        Variable v   = ncFile.findVariable(name);
        double   val = Double.NaN;
        if (v != null) {
            try {
                Array data = v.read();
                val = data.getDouble(data.getIndex());
            } catch (IOException ioe) {
                ;
            }
        }
        return val;
    }

    /**
     * Get the name of this adapter
     *
     * @return  the source of the data
     */
    public String getName() {
        return location;
    }

    /**
     * Get the unit for a particular variable
     *
     * @param v   variable
     *
     * @return corresponding unit
     */
    private Unit getUnit(Variable v) {
        Attribute a        = v.findAttribute("units");
        String    unitName = "";
        if (a != null) {
            unitName = a.getStringValue();
        } else {
            System.out.println("no unit for variable " + v);
        }
        Unit u = DataUtil.parseUnit(unitName);
        return u;
    }

    /**
     * Clean up whatever we need to when we are removed.
     */
    public void doRemove() {}
}
