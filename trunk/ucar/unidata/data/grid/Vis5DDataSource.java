/*
 * $Id: Vis5DDataSource.java,v 1.61 2006/12/01 20:41:35 jeffmc Exp $
 *
 * Copyright  1997-2004 Unidata Program Center/University Corporation for
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

/**
 * DataSource for Vis5D files.
 *
 * @author Don Murray
 * @version $Revision: 1.61 $ $Date: 2006/12/01 20:41:35 $
 */

package ucar.unidata.data.grid;


import ucar.unidata.data.BadDataException;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataContext;
import ucar.unidata.data.DataInstance;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoSelection;

import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;



import visad.*;

import visad.data.vis5d.Vis5DFamily;

import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A data source for Vis5D data files.  Handles grid and topography
 * files.
 *
 * @author  IDV Development Team
 * @version $Revision: 1.61 $
 */
public class Vis5DDataSource extends GridDataSource {



    /** the data from the adapter */
    private Data v5DData;

    /** 2D variables */
    private ArrayList vars2D = new ArrayList();

    /** 3D variables */
    private ArrayList vars3D = new ArrayList();

    /** Hashtable of names to types */
    private Hashtable typeNameToType = new Hashtable();

    /** debug flag to force subsetting */
    private boolean forceSubset = false;

    /**
     *  Dummy constructor so this object can get unpersisted.
     */
    public Vis5DDataSource() {}


    /**
     * Create a Vis5DDataSource from the specification given.
     *
     * @param descriptor      descriptor for the DataSource
     * @param source          source of data (filename)
     * @param properties      extra properties for initialization
     *
     * @throws VisADException  couldn't create the data
     */
    public Vis5DDataSource(DataSourceDescriptor descriptor, String source,
                           Hashtable properties)
            throws VisADException {
        super(descriptor, source, "", properties);
        initVis5dDataSource();
    }


    /**
     * The source has changed
     */
    protected void sourcesChanged() {
        vars2D  = new ArrayList();
        vars3D  = new ArrayList();
        v5DData = null;
        super.sourcesChanged();
    }




    /**
     * Read in the data and create the lists of variables
     */
    private void initVis5dDataSource() {
        try {
            if ((sources == null) || sources.isEmpty()) {
                return;
            }
            Vis5DFamily v5dForm = new Vis5DFamily("vis5d");
            String      source  = (String) sources.get(0);
            if (source.startsWith("http:")) {
                //Try it as a URL
                v5DData = v5dForm.open(new URL(source));
            } else {
                //Try it as a file
                v5DData = v5dForm.open(source);
            }
            parseMathType(v5DData.getType(), 0);
        } catch (Exception e) {
            setInError(true);
            logException("Initializing Vis5D data source", e);
        }
    }


    /**
     * Get the data associated with this source
     *
     * @return the data
     */
    private FieldImpl getV5DData() {
        if (v5DData == null) {
            initVis5dDataSource();
        }
        return (FieldImpl) v5DData;
    }



    /**
     *  Create the {@link DataChoice}s for this data source
     */
    protected void doMakeDataChoices() {
        FieldImpl grid = getV5DData();
        if (grid == null) {
            return;
        }
        boolean hasTimes = false;
        try {
            hasTimes = GridUtil.isTimeSequence(grid);
        } catch (VisADException ve) {}
        for (int nn = 0; nn < vars3D.size(); nn++) {
            RealType type = (RealType) vars3D.get(nn);
            String   name = type.getName();
            typeNameToType.put(type.toString(), type);
            addDataChoice(new DirectDataChoice(this, type.toString(), name,
                    name, (hasTimes)
                          ? getThreeDTimeSeriesCategories()
                          : getThreeDCategories()));
        }

        for (int nn = 0; nn < vars2D.size(); nn++) {
            RealType type = (RealType) vars2D.get(nn);
            String   name = type.getName();
            typeNameToType.put(type.toString(), type);
            addDataChoice(new DirectDataChoice(this, type.toString(), name,
                    name, (hasTimes)
                          ? getTwoDTimeSeriesCategories()
                          : getTwoDCategories()));
        }

    }

    /**
     * Get the data described by the selection parameters
     *
     * @param dataChoice         choice describing data
     * @param category           the data category
     * @param dataSelection      subsetting specs
     * @param requestProperties  extra request properties
     * @return  the data (a grid)
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection,
                                Hashtable requestProperties)
            throws VisADException, RemoteException {
        return getField(dataChoice, dataSelection);
    }


    /**
     * Create the list of times associated with this datasource
     * @return  list of times
     */
    protected List doMakeDateTimes() {
        List      timeList = null;
        FieldImpl grid     = getV5DData();
        try {
            if (GridUtil.isTimeSequence(grid)) {
                DateTime[] times = DateTime.timeSetToArray(
                                       (Gridded1DSet) GridUtil.getTimeSet(
                                           grid));
                timeList = Misc.toList(times);
            }
        } catch (Exception excp) {
            excp.printStackTrace();
        }
        return timeList;
    }

    /**
     * Get the grid described by the selection parameters
     *
     * @param dc              choice describing the data
     * @param dataSelection   subsetting properties
     * @return  grid (FieldImpl) of data
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public FieldImpl getField(DataChoice dc, DataSelection dataSelection)
            throws VisADException, RemoteException {
        Trace.call1("getField");
        Object   id = dc.getId();
        RealType rt = null;
        if (id instanceof RealType) {
            rt = (RealType) id;
        } else {
            rt = (RealType) typeNameToType.get(id.toString());
        }
        GeoSelection geoSelection = ((dataSelection != null)
                                     ? dataSelection.getGeoSelection()
                                     : null);


        if (forceSubset) {
            geoSelection = new GeoSelection(null, 2, 2, 2);
        }

        List times    = getTimesFromDataSelection(dataSelection, dc);
        List cacheKey = Misc.newList(rt, times);
        if (geoSelection != null) {
            cacheKey.add(geoSelection);
        }

        // Assume that structure is  always a field with outer domain t
        // and range Tuple of 3D and 2D variables
        FieldImpl fi   = null;

        FieldImpl grid = getV5DData();
        if (grid == null) {
            return null;
        }

        FieldImpl retField = (FieldImpl) getCache(cacheKey);
        if (retField != null) {
            return retField;
        }

        if (grid instanceof FieldImpl) {
            int index = findIndex((FunctionType) v5DData.getType(), rt);
            if (index != -1) {
                fi = (FieldImpl) ((FieldImpl) v5DData).extract(index);
            } else if (MathType.findScalarType(v5DData.getType(), rt)) {
                fi = (FieldImpl) v5DData;
            }

            if (fi == null) {
                throw new BadDataException("Unable to find data: " + rt
                                           + " \nin Vis5D file: " + sources);
            }

            // until we can get NN working, comment this out.
            Trace.call1("timeSubset");
            if ((times != null) && !times.isEmpty() && (fi != null)
                    && !times.equals(getAllDateTimes())) {
                SampledSet samplingSet = null;
                DateTime[] dateTimes =
                    (DateTime[]) times.toArray(new DateTime[times.size()]);
                if (dateTimes.length == 1) {
                    samplingSet = new SingletonSet(new RealTuple(new Real[] {
                        dateTimes[0] }));
                } else {
                    samplingSet = DateTime.makeTimeSet(dateTimes);
                }
                FieldImpl newFI =
                // Until this works, use W_A
                (FieldImpl) fi.resample(samplingSet, Data.NEAREST_NEIGHBOR,
                                        Data.NO_ERRORS);
                //(FieldImpl) fi.resample(samplingSet, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
                fi = newFI;
            }
            Trace.call2("timeSubset");
            // now check for 2D data and extract specific field.
            // first fudge for Altitude which is a RealType range
            if ((fi instanceof FlatField) && rt.equals(RealType.Altitude)
                    && ((FunctionType) ((FlatField) fi).getType()).getRange()
                       instanceof RealType) {
                // keep same fi;
            } else {
                Trace.call1("extract " + rt);
                fi = GridUtil.extractParam(fi, rt);
                Trace.call2("extract " + rt);
            }
            if ((geoSelection != null) && geoSelection.getHasValidState()) {
                fi = geoSubset(fi, geoSelection);
            }
            putCache(cacheKey, fi);
        }
        Trace.call2("getField");
        return fi;
    }

    /**
     * Geo subset the grid
     *
     * @param fi   The grid to subset
     * @param geoSelection  the subset parameters
     *
     * @return the subsetted grid
     *
     * @throws VisADException    Problem subsetting the data
     */
    private FieldImpl geoSubset(FieldImpl fi, GeoSelection geoSelection)
            throws VisADException {
        Trace.call1("geoSubset");

        /*
        System.err.println("subsetting using:" + geoSelection.getLatLonRect()
                           + " " + geoSelection.getXStrideToUse() +
                           " " + geoSelection.getYStrideToUse() +
                           " " + geoSelection.getZStrideToUse()
                           );
        */
        FieldImpl subSet = GridUtil.is3D(fi)
                           ? GridUtil.subset(fi,
                                             geoSelection.getXStrideToUse(),
                                             geoSelection.getYStrideToUse())
                           : GridUtil.subset(fi,
                                             geoSelection.getXStrideToUse(),
                                             geoSelection.getYStrideToUse(),
                                             geoSelection.getZStrideToUse());
        Trace.call2("geoSubset");
        return subSet;

    }

    /**
     * Find the index of a RealType in a FunctionType
     *
     * @param type   FunctionType to search
     * @param rt     RealType to find
     * @return  index in the type, or -1 if not found
     *
     * @throws VisADException  problem searching
     */
    private int findIndex(FunctionType type, RealType rt)
            throws VisADException {
        int      index     = -1;
        MathType rangeType = type.getRange();
        /*
          if (rangeType instanceof FunctionType
          && MathType.findScalarType(rangeType, rt)) {
          index = 0;
          } else */
        if (rangeType instanceof TupleType) {
            int n_comps = ((TupleType) rangeType).getDimension();
            for (int i = 0; i < n_comps; i++) {
                MathType test_comp = ((TupleType) rangeType).getComponent(i);
                if (test_comp.equals(rt)
                        || MathType.findScalarType(test_comp, rt)) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    /**
     * getParameters helper method.
     *
     * @param mathType   MathType to parse
     * @param dimension  dimension of data
     *
     * @throws VisADException  problem parsing the MathType
     */
    private void parseMathType(MathType mathType, int dimension)
            throws VisADException {
        if (mathType instanceof FunctionType) {
            parseFunction((FunctionType) mathType, dimension);
        } else if (mathType instanceof TupleType) {
            parseTuple((TupleType) mathType, dimension);
        } else {
            parseScalar((ScalarType) mathType, dimension);
        }
    }

    /**
     * parseMathTypes helper method.
     *
     * @param mathType     FunctionType to parse
     * @param dimension    dimension of data
     *
     * @throws VisADException
     */
    private void parseFunction(FunctionType mathType, int dimension)
            throws VisADException {
        // extract domain
        RealTupleType domain = mathType.getDomain();
        int           dim    = domain.getDimension();

        // extract range
        MathType range = mathType.getRange();
        parseMathType(range, dim);
    }

    /**
     * parseMathTypes helper method.
     *
     * @param mathType     TupleType to parse
     * @param dimension    dimension of data
     *
     * @throws VisADException
     */
    private void parseTuple(TupleType mathType, int dimension)
            throws VisADException {
        // extract components
        for (int j = 0; j < mathType.getDimension(); j++) {
            MathType cType = mathType.getComponent(j);
            if (cType != null) {
                parseMathType(cType, dimension);
            }
        }
    }

    /**
     * parseMathTypes helper method.
     *
     * @param mathType     ScalarType to parse
     * @param dimension    dimension of data
     */
    private void parseScalar(ScalarType mathType, int dimension) {
        if (mathType instanceof RealType) {
            if (dimension == 2) {  // 2D variable
                vars2D.add(mathType);
            } else if (dimension == 3) {
                vars3D.add(mathType);
            }
        }
    }

    /**
     * We can do geo selection in the properties gui
     *
     * @return can do geo selection
     */
    public boolean canDoGeoSelection() {
        return true;
    }

    /**
     * Used for the geo subsetting property gui as to whether to
     * show the map selection or not
     *
     * @return default is true
     */
    protected boolean canDoGeoSelectionMap() {
        return false;
    }


    /**
     *  Set the Source property.
     *
     *  @param value The new value for Source
     */
    public void setSource(String value) {
        oldSourceFromBundles = value;
    }


    /**
     * Test by running "java ucar.unidata.data.grid.Vis5DDataSource <filename>"
     *
     * @param args  filename
     *
     * @throws Exception  problem running this
     */
    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            System.out.println("Must supply a file name");
            System.exit(1);
        }
        Vis5DDataSource v5d = new Vis5DDataSource(null, args[0], null);
        /*
          for (Iterator iter = v5d.getDataChoices().iterator(); iter.hasNext();) {
          System.out.println(iter.next());
          }
          Data testData = v5d.getData ((DataChoice) v5d.getDataChoices().get(0), null);
          visad.python.JPythonMethods.dumpTypes (testData);
        */
        for (Iterator iter =
                v5d.getAllDateTimes().iterator(); iter.hasNext(); ) {
            System.out.println(iter.next());
        }

    }


}

