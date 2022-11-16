/*
 * Copyright 1997-2022 Unidata Program Center/University Corporation for
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

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.unidata.data.*;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.DateSelection;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Trace;
import ucar.visad.Util;
import visad.*;

import javax.swing.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class NetcdfPointCloudDataSource extends NetcdfPointDataSource{
    String source = null;
    List<VariableSimpleIF> varNames = new ArrayList<VariableSimpleIF>();
    DataSelection dataSelection;
    /** The timeList */
    protected List timeList = new ArrayList<DateTime>();
    /** _more_ */
    public static final float GRID_MISSING = -99999.9f;

    /** _more_ */
    public static final int INDEX_ALT = 0;

    /** _more_ */
    public static final int INDEX_LON = 1;

    /** _more_ */
    public static final int INDEX_LAT = 2;

    /** _more_ */
    private int gridWidth = 800;

    /** _more_ */
    private int gridHeight = 800;

    /** _more_ */
    private static int typeCnt = 0;
    /** _more_ */
    private JTextField gridWidthFld;

    /** _more_ */
    private JTextField gridHeightFld;

    /** _more_ */
    private float hillShadeAzimuth = 315;

    /** _more_ */
    private float hillShadeAngle = 45;

    /** _more_ */
    private JTextField hillShadeAzimuthFld;

    /** _more_ */
    private JTextField hillShadeAngleFld;

    /**
     * Default constructor
     *
     * @throws VisADException  problem creating the object
     */
    public NetcdfPointCloudDataSource() throws VisADException {
        init();
    }


    /**
     * Create a new NetcdfPointCloudDataSource
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
     * Create a new NetcdfPointCloudDataSource
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
     * Create a new NetcdfPointCloudDataSource
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
     * Create a new NetcdfPointCloudDataSource
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
        List<DateTime> times = new ArrayList<DateTime>();
        Trace.call1("NetcdfPointDatasource:makeObs");
        if (obs == null) {
            for(int ii = 0; ii < sources.size(); ii++) {
                FeatureDatasetPoint pods = getDataset((String)sources.get(ii));
                if (pods == null) {
                    return null;
                }
                DateSelection ds =
                        (DateSelection) getProperty(DataSelection.PROP_DATESELECTION);
                if (ds == null && subset != null) {
                    ds = new DateSelection();
                    List subsettimes = getSelectedTimes(dataChoice, subset);
                    ds.setTimes(subsettimes);
                } else {
                    NetcdfDataset dataset = (NetcdfDataset) pods.getNetcdfFile();
                    CoordinateAxis1D timeAxis0 = (CoordinateAxis1D) dataset.findCoordinateAxis(AxisType.Time);
                    Array timeArray = timeAxis0.getOriginalVariable().read();
                    double[] timeValue1D = null;
                    if (timeArray != null) {
                        int len = (int) timeArray.getSize();
                        timeValue1D = new double[len];
                        for (int i = 0; i < len; i++) {
                            timeValue1D[i] = (double) timeArray.getInt(i);
                        }
                        for (int j = 0; j < len; j++) {
                            DateTime dtt = new DateTime(timeValue1D[j], DataUtil.parseUnit(timeAxis0.getUnitsString()));
                            times.add(dtt);
                        }
                    }
                }
            }
            timeList = getUniqueTimes(times);
            //obs = PointObFactory.makePointObs(pods, dataChoice, getBinRoundTo(),
             //       getBinWidth(), bbox, ds, sample);
            FeatureDatasetPoint pods = getDataset(source);
            if (super.fixedDataset == null) {
                pods.close();
            }
            varNames = pods.getDataVariables();
            dataSelection = dataChoice.getDataSelection();
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
            if (getDataChoices().size() != 0) {
                getDataChoices().clear();  // remove point data datachoice
            }  else {
                return;
            }
            //Sample the data to see if we need to show the metadata gui
            Hashtable props = Misc.newHashtable("doFilter", "false");

            List cloudCats =
                    DataCategory.parseCategories("Point Cloud;pointcloud", true);
            List pointGridCats =
                    DataCategory.parseCategories("Point Grids;GRID-2D;", true);
            for (VariableSimpleIF var : varNames) {
                String varname = var.getShortName();

                DataChoice choice = new DirectDataChoice(this,
                        "pointcloud:" + varname, varname,
                        varname, cloudCats, props);
                addDataChoice(choice);
                DataChoice choice1 = new DirectDataChoice(
                        this, "pointgrid:" + varname, varname, varname,
                        pointGridCats, new Hashtable());
                addDataChoice(choice1);

               // choice.setDataSelection(dataSelection);
                //choice1.setDataSelection(dataSelection);
            }

        } catch (Exception exc) {
            logException("Creating track choices", exc);
        }
    }

    /**
     *  Get all the times for the given DataChoice
     *
     * @param
     */
    public List getAllDateTimes(DataChoice dataChoice) {
        if (timeList != null) {
            return timeList;
        }
        return new ArrayList();
    }

    /**
     *  Try to merge children up into parents of only one child
     *
     * @param
     */
    public List getAllDateTimes() {
        return Misc.sort(timeList);
    }

    /**
     *  get datetime of only one datachoice
     *
     * @param dataChoice
     */
    private Object getDateTime(DataChoice dataChoice) {
        //DataSelection   dataSelection  = dataChoice.getDataSelection();
       // timeList = dataSelection.getTimes();
        if(timeList == null)
            return null;
        return timeList;
    }

    public static List getSelectedTimes(DataChoice dataChoice, DataSelection subset) {
        List<DateTime> absTimes    = null;

        // figure out the time indices
        // have abs or relative times
        List allTimes = dataChoice.getAllDateTimes();
        if( subset.getTimes() == null)
            return allTimes;

        if ((allTimes != null) && !allTimes.isEmpty()) {  // have times
            List timeIdx = subset.getTimes();
            if(timeIdx != null && timeIdx.get(0) instanceof DateTime)
                return subset.getTimes();
            int ss = timeIdx.size();
            absTimes = new ArrayList<DateTime>();
            for (int i = 0; i < ss; i++) {
                absTimes.add(
                        (DateTime) allTimes.get((int)timeIdx.get(i)));
            }
        }
        return absTimes;
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


        try {
            FlatField[] grids = new FlatField[sources.size()];
            List<DateTime> times = new ArrayList<DateTime>();
            for(int ii = 0; ii < sources.size(); ii++) {
                FeatureDatasetPoint pods = getDataset((String)sources.get(ii));
                NetcdfDataset dataset = (NetcdfDataset) pods.getNetcdfFile();
                String name = dataChoice.getName();
                Variable dataVar = dataset.findVariable(name);
                String unitStr = dataVar.getUnitsString();
                if(unitStr.contains("meter^") || unitStr.contains("m^")) {
                    unitStr = unitStr.replaceAll("\\s", "");
                }

                Unit varUnit = DataUtil.parseUnit(unitStr);
                //List<ucar.ma2.Range> vrange = dataVar.getRanges();
                Array dataValueRaw = dataVar.read();
                ArrayFloat.D2 dataValue = null;
                ArrayFloat.D1 dataValue1 = null;
                if (dataValueRaw instanceof ArrayFloat.D2)
                    dataValue = (ArrayFloat.D2) dataValueRaw;
                else if (dataValueRaw instanceof ArrayFloat.D1)
                    dataValue1 = (ArrayFloat.D1) dataValueRaw;
                ArrayDouble.D1 timeValue1D = null;
                CoordinateAxis timeAxis0 = dataset.findCoordinateAxis(AxisType.Time);
                Array timeArray = timeAxis0.getOriginalVariable().read();
                List alltimes = dataChoice.getAllDateTimes();
                List subsetTimes = getSelectedTimes(dataChoice, dataSelection);
                times.addAll(subsetTimes);
                List subsetIdx = Misc.getIndexList(subsetTimes, alltimes);
                //CoordinateAxis1D timeAxis = (CoordinateAxis1D)dataset.findCoordinateAxis(AxisType.Time);
                CoordinateAxis1D latAxis = (CoordinateAxis1D) dataset.findCoordinateAxis(AxisType.Lat);
                CoordinateAxis1D lonAxis = (CoordinateAxis1D) dataset.findCoordinateAxis(AxisType.Lon);
                CoordinateAxis1D altAxis = (CoordinateAxis1D) dataset.findCoordinateAxis(AxisType.Height);
                int[] datashape = dataVar.getShape();
                double[] latValue = latAxis.getCoordValues();
                double[] lonValue = lonAxis.getCoordValues();
                double[] altValue = altAxis.getCoordValues();

                int stalen = datashape[0];
                // test real point clouds
                float[][] pts = new float[4][stalen];
                pts[0] = Misc.toFloat(altValue);
                pts[1] = Misc.toFloat(lonValue);
                pts[2] = Misc.toFloat(latValue);
                RealType rt = Util.makeRealType(name, varUnit);
                MathType type = new RealTupleType(RealType.Altitude, RealType.Longitude,
                        RealType.Latitude, rt);
                //Integer1DSet sTimes = new Integer1DSet(RealTupleType.Time1DTuple,
                //        numDays, null,
                //        new Unit[] { CLIMATE_UNITS }, null);

                if (dataValue != null && ii == 0)
                    grids = new FlatField[subsetIdx.size()];

                if ((id instanceof String)
                        && (id.toString().startsWith("pointcloud:"))) {
                    if (dataValue != null)
                        for (int i = 0; i < subsetIdx.size(); i++) {
                            int idx = (int) subsetIdx.get(i);
                            for (int j = 0; j < stalen; j++)
                                pts[3][j] = dataValue.get(j, idx);
                            grids[i] = makeField(type, pts);
                        }
                    else if (dataValue1 != null) {
                        for (int j = 0; j < stalen; j++)
                            pts[3][j] = dataValue1.get(j);
                        grids[ii] = makeField(type, pts);

                    }
                } else {
                    if (dataValue != null)
                        for (int i = 0; i < subsetIdx.size(); i++) {
                            int idx = (int) subsetIdx.get(i);
                            for (int j = 0; j < stalen; j++)
                                pts[3][j] = dataValue.get(j, idx);
                            grids[i] = (FlatField) makeGrid(pts, null, false, false);
                        }
                    else if (dataValue1 != null) {
                        for (int j = 0; j < stalen; j++)
                            pts[3][j] = dataValue1.get(j);

                        grids[ii] =(FlatField) makeGrid(pts, null, false, false);
                    }
                }
            }
            // make timeSet the domain of the final FieldImpl;
            // one for each  height-obs groupreturn grids[0];
            double[][] timesetdoubles = new double[1][grids.length];
            List<DateTime> times0 = getUniqueTimes(times);
            for (int j = 0; j < grids.length; j++) {
                timesetdoubles[0][j] =
                    ((DateTime) times0.get(j)).getReal().getValue();
            }
            QuickSort.sort(timesetdoubles[0]);
            Gridded1DDoubleSet timeset =// }
                new Gridded1DDoubleSet(RealType.Time, timesetdoubles, grids.length);

            FieldImpl retField = new FieldImpl(
                new FunctionType(
                     ((SetType) timeset.getType()).getDomain(),
                     (((FlatField) grids[0]).getType())), timeset);


            retField.setSamples(grids, false);

            return retField;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }

    }

    public static FlatField makeField(MathType rangeType, float[][] pts)
            throws VisADException, RemoteException {
        RealType     index  = RealType.getRealType("index");
        Integer1DSet domain = new Integer1DSet(index, pts[0].length);
        FunctionType ft     = new FunctionType(index, rangeType);
        FlatField    field  = new FlatField(ft, domain);
        field.setSamples(pts, false);
        return (FlatField)field.clone();
    }

    private FieldImpl makeGrid(float[][] pts, RealType type,
                               boolean hillshade, boolean pointCount)
            throws Exception {

        boolean   fillMissing = true;
        int       numCols     = gridWidth;
        int       numRows     = gridHeight;

        float     west        = Float.POSITIVE_INFINITY;
        float     south       = Float.POSITIVE_INFINITY;
        float     east        = Float.NEGATIVE_INFINITY;
        float     north       = Float.NEGATIVE_INFINITY;

        float[][] latLonGrid  = new float[numRows][numCols];
        int[][]   cntGrid     = new int[numRows][numCols];
        for (int x = 0; x < numCols; x++) {
            for (int y = 0; y < numRows; y++) {
                latLonGrid[y][x] = Float.NaN;
                cntGrid[y][x]    = 0;
            }
        }

        for (int i = 0; i < pts[0].length; i++) {
            double lat = pts[INDEX_LAT][i];
            double lon = pts[INDEX_LON][i];
            west  = (float) Math.min(west, lon);
            east  = (float) Math.max(east, lon);
            north = (float) Math.max(north, lat);
            south = (float) Math.min(south, lat);
        }

        double gridWidth  = east - west;
        double gridHeight = north - south;
        for (int i = 0; i < pts[0].length; i++) {
            double altitude = pts[3][i];
            double lat      = pts[INDEX_LAT][i];
            double lon      = pts[INDEX_LON][i];
            int latIndex = (numRows - 1)
                    - (int) ((numRows - 1) * (lat - south)
                    / gridHeight);
            int lonIndex = (int) ((numCols - 1) * (lon - west) / gridWidth);
            if (latLonGrid[latIndex][lonIndex]
                    != latLonGrid[latIndex][lonIndex]) {
                latLonGrid[latIndex][lonIndex] = 0;
            }
            latLonGrid[latIndex][lonIndex] += (float) altitude;
            cntGrid[latIndex][lonIndex]++;
        }

        if (pointCount) {
            for (int x = 0; x < numCols; x++) {
                for (int y = 0; y < numRows; y++) {
                    if (cntGrid[y][x] > 0) {
                        latLonGrid[y][x] = cntGrid[y][x];
                    }
                }
            }
        } else {
            for (int x = 0; x < numCols; x++) {
                for (int y = 0; y < numRows; y++) {
                    if (latLonGrid[y][x] == latLonGrid[y][x]) {
                        latLonGrid[y][x] = latLonGrid[y][x] / cntGrid[y][x];
                    }
                }
            }
        }

        if (fillMissing && !pointCount) {
            GridUtil.fillMissing(latLonGrid, GRID_MISSING);
        }

        if (hillshade) {
            type = Util.makeRealType("hillshade" + (typeCnt++), null);
            latLonGrid = doHillShade(latLonGrid, hillShadeAzimuth,
                    hillShadeAngle);
        } else if (pointCount) {
            type = Util.makeRealType("pointcount" + (typeCnt++), null);
        } else {
            type = RealType.Altitude;
        }

        float[][] gridValues = GridUtil.makeGrid(latLonGrid, numCols,
                numRows, GRID_MISSING);

        Linear1DSet xSet = new Linear1DSet(RealType.Longitude, west, east,
                numCols);
        Linear1DSet ySet = new Linear1DSet(RealType.Latitude, north, south,
                numRows);
        GriddedSet gdsSet =
                new LinearLatLonSet(RealTupleType.SpatialEarth2DTuple,
                        new Linear1DSet[] { xSet,
                                ySet }, (CoordinateSystem) null, (Unit[]) null,
                        (ErrorEstimate[]) null, true);
        FunctionType ftLatLon2Param =
                new FunctionType(((SetType) gdsSet.getType()).getDomain(),
                        new RealTupleType(type));
        Unit outputUnits = type.getDefaultUnit();
        FlatField retData = new FlatField(ftLatLon2Param, gdsSet,
                (CoordinateSystem) null,
                (Set[]) null,
                new Unit[] { outputUnits });
        retData.setSamples(gridValues, false);
        return retData;

    }
    private float[][] doHillShade(float[][] grid, float azimuth,
                                  float angle) {
        float     z                = 1.0f;
        float     nsres            = 1.0f;
        float     scale            = 1.0f;
        float     ewres            = 1.0f;

        float     degreesToRadians = (float) (Math.PI / 180.0);
        float     radiansToDegrees = (float) (180.0 / Math.PI);
        int       nYSize           = grid.length;
        int       nXSize           = grid[0].length;
        float[][] angles           = new float[nYSize][nXSize];
        float[]   win              = new float[9];
        Misc.fillArray(angles, Float.NaN);
        int i, j;
        /*  0 1 2
         *  3 4 5
         *  6 7 8
         */
        for (i = 0; i < nYSize; i++) {
            for (j = 0; j < nXSize; j++) {
                if ((i == 0) || (j == 0) || (i == nYSize - 1)
                        || (j == nXSize - 1)) {
                    continue;
                }
                boolean containsNull = false;
                win[0] = grid[i - 1][j - 1];
                win[1] = grid[i - 1][j];
                win[2] = grid[i - 1][j + 1];
                win[3] = grid[i][j - 1];
                win[4] = grid[i][j];
                win[5] = grid[i][j + 1];
                win[6] = grid[i + 1][j - 1];
                win[7] = grid[i + 1][j];
                win[8] = grid[i + 1][j + 1];


                for (int n = 0; n <= 8; n++) {
                    if ((win[n] != win[n]) || (win[n] == GRID_MISSING)) {
                        containsNull = true;
                        break;
                    }
                }
                if (containsNull) {
                    continue;
                }
                // First Slope ...

                float x = (float) (((z * win[0] + z * win[3] + z * win[3]
                        + z * win[6]) - (z * win[2] + z * win[5]
                        + z * win[5] + z * win[8])) / (8.0
                        * ewres * scale));

                float y = (float) (((z * win[6] + z * win[7] + z * win[7]
                        + z * win[8]) - (z * win[0] + z * win[1]
                        + z * win[1] + z * win[2])) / (8.0
                        * nsres * scale));

                float key = (float) Math.sqrt(x * x + y * y);
                float slope = (float) (90.0
                        - Math.atan(key) * radiansToDegrees);
                float slopePct = 100 * key;
                float value    = slopePct;

                // ... then aspect...
                float aspect = (float) Math.atan2(x, y);

                // ... then the shade value
                float cang =
                        (float) (Math.sin(angle * degreesToRadians)
                                * Math.sin(slope * degreesToRadians) + Math.cos(
                                angle * degreesToRadians) * Math.cos(
                                slope * degreesToRadians) * Math.cos(
                                (azimuth - 90.0) * degreesToRadians
                                        - aspect));

                if (cang <= 0.0) {
                    cang = 1.0f;
                } else {
                    cang = 1.0f + (254.0f * cang);
                }

                value        = cang;

                angles[i][j] = value;
            }
        }
        return angles;
    }

    public static List getUniqueTimes(List<DateTime> timelist)
            throws VisADException, RemoteException {

        List uniqueTimes = new ArrayList();
        int numObs = timelist.size();

        Hashtable timeToObs = new Hashtable();
        // loop through and find all the unique times

        Hashtable seenTime = new Hashtable();
        for (int i = 0; i < numObs; i++) {
            DateTime dttm = timelist.get(i);
            Double dValue = new Double(dttm.getValue());
            boolean contains = (seenTime.put(dValue, dValue) != null);
            if (!contains) {
                uniqueTimes.add(dttm);
            }
        }
        return uniqueTimes;
    }
}
