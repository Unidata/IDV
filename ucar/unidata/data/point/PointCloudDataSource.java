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
 * 
 */

package ucar.unidata.data.point;


import ucar.unidata.data.*;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;

import ucar.unidata.geoloc.projection.UtmProjection;
import ucar.unidata.util.GuiUtils;
import ucar.unidata.util.IOUtil;


import ucar.unidata.util.LogUtil;
import ucar.unidata.util.Misc;
import ucar.unidata.util.StringUtil;
import ucar.unidata.util.Trace;
import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.NavigatedDisplay;

import ucar.visad.MapProjectionProjection;

import ucar.visad.Util;
import ucar.visad.display.RGBDisplayable;

import ucar.visad.display.VolumeDisplayable;

import visad.*;

import visad.georef.MapProjection;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.awt.geom.Rectangle2D;


import java.io.*;




import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;




/**
 * A display control for volume rendering of a 3D grid
 *
 * @author Unidata IDV Development Team
 * @version $Revision: 1.11 $
 */

public class PointCloudDataSource extends FilesDataSource {

    /** _more_ */
    public static final float GRID_MISSING = -99999.9f;

    /** _more_ */
    public static final int INDEX_ALT = 0;

    /** _more_ */
    public static final int INDEX_LON = 1;

    /** _more_ */
    public static final int INDEX_LAT = 2;


    /** _more_ */
    private int skip = 10;

    /** _more_ */
    private JTextField skipFld;

    /** _more_ */
    private String delimiter = ",";

    /** _more_ */
    private JTextField delimiterFld;

    /** _more_ */
    private JTextField colorByIndexFld;

    /** _more_ */
    private int colorByIndex = 3;

    /** _more_ */
    UtmInfo utmInfo = new UtmInfo();


    /** _more_ */
    private String header;

    /** _more_ */
    protected String fieldName;

    /** _more_ */
    private int gridWidth = 800;

    /** _more_ */
    private int gridHeight = 800;

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



    /** logging category */
    static LogUtil.LogCategory log_ =
        LogUtil.getLogInstance(PointCloudDataSource.class.getName());


    /**
     *
     * Default constructor
     *
     */
    public PointCloudDataSource() {}


    /**
     * _more_
     *
     * @param descriptor _more_
     * @param filename _more_
     * @param properties _more_
     */
    public PointCloudDataSource(DataSourceDescriptor descriptor,
                                String filename, Hashtable properties) {
        super(descriptor, filename, "Point cloud data source", properties);
        String tmp = (String) getProperty("delimiter");
        if (tmp != null) {
            delimiter = tmp;
        }
    }



    /**
     * Create a PointDataSource
     *
     * @param descriptor    descriptor for the DataSource
     * @param sources _more_
     * @param name _more_
     * @param properties    extra properties
     *
     * @throws VisADException
     *
     */
    public PointCloudDataSource(DataSourceDescriptor descriptor,
                                List sources, String name,
                                Hashtable properties)
            throws VisADException {
        super(descriptor, sources, sources.get(0).toString(), properties);
    }


    /**
     * _more_
     *
     * @return _more_
     */
    public boolean canDoGeoSelection() {
        String filePath = getFilePath();
        return (filePath.indexOf("${bbox}") >= 0)
               || (filePath.indexOf("${area.north}") >= 0);
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected boolean canDoGeoSelectionStride() {
        return false;
    }

    /**
     * _more_
     *
     * @return _more_
     */
    protected ProjectionImpl getSampleDataProjection() {
        try {
            String filePath = getFilePath();
            if (filePath.indexOf("defaultbbox=") < 0) {
                return null;
            }
            String query = new URL(filePath).getQuery();

            for (String pair : StringUtil.split(query, "&", true, true)) {
                if (pair.startsWith("defaultbbox=")) {
                    List<String> toks =
                        StringUtil.split(
                            pair.substring("defaultbbox=".length()), ",",
                            true, true);
                    //west,south,east,north   
                    if (toks.size() != 4) {
                        return null;
                    }
                    double west  = Double.parseDouble(toks.get(0));
                    double south = Double.parseDouble(toks.get(1));
                    double east  = Double.parseDouble(toks.get(2));
                    double north = Double.parseDouble(toks.get(3));
                    Rectangle2D.Double rect = new Rectangle2D.Double(west,
                                                  north, east - west,
                                                  north - south);
                    return new LatLonProjection("", new ProjectionRect(rect));
                }
            }
            return null;
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private boolean canSkip() {
        String filePath = getFilePath();
        File   f        = new File(filePath);
        if ( !f.exists()) {
            if (filePath.indexOf("${skip}") >= 0) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    private boolean serverHandlesSkip() {
        return canSkip() && !new File(getFilePath()).exists();
    }


    /**
     * _more_
     *
     * @param comps _more_
     */
    public void getPropertiesComponents(List comps) {
        super.getPropertiesComponents(comps);
        String filePath = getFilePath();
        File   f        = new File(filePath);
        skipFld             = new JTextField("" + skip, 5);
        gridWidthFld        = new JTextField("" + gridWidth, 5);
        gridHeightFld       = new JTextField("" + gridHeight, 5);
        hillShadeAzimuthFld = new JTextField("" + hillShadeAzimuth, 5);
        hillShadeAngleFld   = new JTextField("" + hillShadeAngle, 5);
        delimiterFld        = new JTextField(delimiter, 3);
        colorByIndexFld     = new JTextField("" + colorByIndex, 3);
        if (header != null) {
            comps.add(GuiUtils.rLabel("Header:"));
            comps.add(GuiUtils.lLabel(header));
        }

        if (canSkip()) {
            comps.add(GuiUtils.rLabel("Skip:"));
            comps.add(GuiUtils.left(GuiUtils.hbox(skipFld,
                    new JLabel("-1 = default"))));
        }
        comps.add(GuiUtils.rLabel("Delimiter:"));
        comps.add(GuiUtils.left(delimiterFld));
        comps.add(GuiUtils.rLabel("Color by index:"));
        comps.add(GuiUtils.left(colorByIndexFld));
        utmInfo.getPropertiesComponents(comps);

        comps.add(GuiUtils.rLabel("Grid Dimensions:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(gridWidthFld,
                new JLabel(" X "), gridHeightFld)));

        comps.add(GuiUtils.rLabel("Hill Shading:"));
        comps.add(GuiUtils.left(GuiUtils.hbox(GuiUtils.label("Azimuth:",
                hillShadeAzimuthFld), GuiUtils.label("Angle:",
                    hillShadeAngleFld))));



    }

    /**
     * _more_
     *
     * @return _more_
     */
    public boolean applyProperties() {
        if ( !super.applyProperties()) {
            return false;
        }
        utmInfo.applyProperties();
        if (skipFld == null) {
            return true;
        }
        skip = new Integer(skipFld.getText().trim()).intValue();
        gridWidth = new Integer(gridWidthFld.getText().trim()).intValue();
        gridHeight = new Integer(gridHeightFld.getText().trim()).intValue();
        hillShadeAzimuth =
            Float.parseFloat(hillShadeAzimuthFld.getText().trim());
        hillShadeAngle = Float.parseFloat(hillShadeAngleFld.getText().trim());

        colorByIndex =
            new Integer(colorByIndexFld.getText().trim()).intValue();

        delimiter = delimiterFld.getText().trim();
        if (delimiter.length() == 0) {
            delimiter = " ";
        }

        flushCache();
        return true;
    }

    /** _more_ */
    private static int typeCnt = 0;

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

        try {


            String    filePath = getFilePath();
            float[][] pts      = readPoints(filePath, dataSelection, skip);

            RealType  rt       = null;
            if (pts.length > 3) {
                rt = Util.makeRealType(((fieldName != null)
                                        ? fieldName
                                        : "field") + "_" + (typeCnt++), null);
            }

            MathType type;
            if(pts.length == 3) {
                type = new RealTupleType(RealType.Altitude,
                                         RealType.Longitude, RealType.Latitude);
            } else if(pts.length == 6) {
                type = new RealTupleType(new RealType[]{RealType.Altitude,
                                         RealType.Longitude, RealType.Latitude, 
                                         Util.makeRealType("rgb red",null),
                                         Util.makeRealType("rgb green",null),
                                                        Util.makeRealType("rgb blue",null)});
            } else {
                type = new RealTupleType(RealType.Altitude,
                                         RealType.Longitude, RealType.Latitude, rt);
            }

            if (dataChoice.getId().equals("altitudegrid")) {
                return makeGrid(pts, rt, false,false);
            }

            if (dataChoice.getId().equals("hillshadegrid")) {
                return makeGrid(pts, rt, true,false);
            }

            if (dataChoice.getId().equals("pointcount")) {
                return makeGrid(pts, rt, false,true);
            }
            return makeField(type, pts);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    /**
     * This reads the actual lat/lon/alt points and returns an array of the form:
     * <pre>float[lat/lon/alt/optional value][number pf points]</pre>
     *
     * @param filePath Path to the file or url
     * @param dataSelection data subsetting
     * @param skipToUse skip factor 0 = all points. 1 = skip 1, etc.
     *
     * @return points
     *
     * @throws IOException On badness
     */
    protected float[][] readPoints(String filePath,
                                   DataSelection dataSelection, int skipToUse)
            throws IOException {

        float[][] pts = null;
        File      f   = new File(filePath);


        if (serverHandlesSkip()) {
            filePath  = filePath.replace("${skip}", "" + skip);
            skipToUse = 0;
        }
        BufferedReader reader;
        if (f.exists()) {
            reader = new BufferedReader(new FileReader(f));
        } else {

            if (dataSelection.getGeoSelection() != null) {
                GeoLocationInfo gli =
                    dataSelection.getGeoSelection().getBoundingBox();
                if (gli != null) {
                    //west,south,east,north   
                    String bbox = gli.getMinLon() + "," + gli.getMinLat()
                                  + "," + gli.getMaxLon() + ","
                                  + gli.getMaxLat();
                    filePath = filePath.replace("${bbox}", bbox);
                }
            }

            System.err.println(filePath);

            InputStream inputStream = IOUtil.getInputStream(filePath,
                                          getClass());
            //                inputStream = new BufferedInputStream(inputStream, 10000);
            reader = new BufferedReader(new InputStreamReader(inputStream));
        }


        int     pointCnt  = 0;
        int     numFields = 3;
        boolean latLon    = true;
        //check the order
        if (Misc.equals(getProperty("latlon"), "false")) {
            latLon = false;
        }

        int  skipCnt = 0;


        long t1      = System.currentTimeMillis();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            //Check for header
            if (pointCnt == 0) {
                if (line.toLowerCase().indexOf("latitude") >= 0) {
                    header = line;
                    continue;
                }
                if (line.toLowerCase().indexOf("x") >= 0) {
                    header = line;
                    continue;
                }
            }

            if (line.startsWith("#")) {
                continue;
            }
            List<String> toks = StringUtil.split(line, delimiter, true, true);
            if (toks.size() < 3) {
                System.err.println("Bad line:" + line);
                continue;
            }

            if (skipToUse > 0) {
                if (skipCnt > 0) {
                    skipCnt--;
                    continue;
                }
                skipCnt = skipToUse;
            }

            if (pts == null) {
                numFields = Math.min(4, toks.size());
                numFields = 3;
                pts       = new float[numFields][10000];
            } else if (pointCnt >= pts[0].length) {
                pts = Misc.expand(pts);
            }

            //            if(pointCnt>50) break;
            float v1  = Float.parseFloat(toks.get(0).toString());
            float v2  = Float.parseFloat(toks.get(1).toString());
            float v3  = Float.parseFloat(toks.get(2).toString());

            float lat = latLon
                        ? v1
                        : v2;
            float lon = latLon
                        ? v2
                        : v1;
            float alt = v3;
            if (utmInfo.getIsUtm() && utmInfo.getIsUtmMeters()) {
                lon = lon / 1000;
                lat = lat / 1000;
            }
            pts[INDEX_ALT][pointCnt] = alt;
            pts[INDEX_LON][pointCnt] = lon;
            pts[INDEX_LAT][pointCnt] = lat;
            //            System.err.println(lat +"/" + lon +"/" + alt);

            if (numFields == 4) {
                pts[3][pointCnt] =
                    Float.parseFloat(toks.get(colorByIndex).toString());
            }

            pointCnt++;
        }
        if (pts == null) {
            throw new BadDataException("No points were read. Bad delimiter?");
        }
        long t2 = System.currentTimeMillis();
        System.err.println("cnt:" + pointCnt + " time:" + (t2 - t1) / 1000);
        pts = Misc.copy(pts, pointCnt);

        if (utmInfo.getIsUtm()) {
            UtmProjection utm = new UtmProjection(utmInfo.getUtmZone(),
                                    utmInfo.getIsUtmNorth());
            float[]   lats   = pts[INDEX_LAT];
            float[]   lons   = pts[INDEX_LON];
            float[][] result = utm.projToLatLon(new float[][] {
                lats, lons
            }, new float[][] {
                lats, lons
            });

            pts[INDEX_LAT] = lats;
            pts[INDEX_LON] = lons;
        }


        if (header != null) {
            List<String> tmpToks = StringUtil.split(header, delimiter, true,
                                       true);
            if ((pts.length > 3) && (tmpToks.size() > 3)) {
                fieldName = tmpToks.get(3);
            }
        }

        return pts;

    }


    /**
     * _more_
     *
     * @param grid _more_
     * @param azimuth _more_
     * @param angle _more_
     *
     * @return _more_
     */
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



    /**
     * _more_
     *
     * @param pts _more_
     * @param type _more_
     * @param hillshade _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    private FieldImpl makeGrid(float[][] pts, RealType type,
                               boolean hillshade,
                               boolean pointCount)
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
            double altitude = pts[INDEX_ALT][i];
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
                    if(cntGrid[y][x]>0) {
                        latLonGrid[y][x] =  cntGrid[y][x];
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
        } else  if (pointCount) {
            type = Util.makeRealType("pointcount" + (typeCnt++), null);
        } else {
            type = RealType.Altitude;
        }

        float[][] gridValues = GridUtil.makeGrid(latLonGrid, numCols, numRows, GRID_MISSING);

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





    /**
     * _more_
     *
     * @param rangeType _more_
     * @param pts _more_
     *
     * @return _more_
     *
     * @throws RemoteException _more_
     * @throws VisADException _more_
     */
    public static FlatField makeField(MathType rangeType, float[][] pts)
            throws VisADException, RemoteException {
        RealType     index  = RealType.getRealType("index");
        Integer1DSet domain = new Integer1DSet(index, pts[0].length);
        FunctionType ft     = new FunctionType(index, rangeType);
        FlatField    field  = new FlatField(ft, domain);
        field.setSamples(pts, false);
        return field;
    }



    /**
     * _more_
     */
    public void doMakeDataChoices() {
        addDataChoice(
            new DirectDataChoice(
                this, "pointcloud", "pointcloud", "Point Cloud Data",
                DataCategory.parseCategories("pointcloud", false),
                new Hashtable()));

        addDataChoice(
            new DirectDataChoice(
                this, "altitudegrid", "altitude", "Altitude Grid",
                DataCategory.parseCategories("GRID-2D;", false),
                new Hashtable()));

        addDataChoice(
            new DirectDataChoice(
                this, "hillshadegrid", "hillshade", "Hill Shade",
                DataCategory.parseCategories("GRID-2D;", false),
                new Hashtable()));

        addDataChoice(
            new DirectDataChoice(
                this, "pointcount", "pointcount", "Point Count",
                DataCategory.parseCategories("GRID-2D;", false),
                new Hashtable()));


    }



    /**
     * Set the UtmInfo property.
     *
     * @param value The new value for UtmInfo
     */
    public void setUtmInfo(UtmInfo value) {
        this.utmInfo = value;
    }

    /**
     * Get the UtmInfo property.
     *
     * @return The UtmInfo
     */
    public UtmInfo getUtmInfo() {
        return this.utmInfo;
    }


    /**
     * _more_
     *
     * @param args _more_
     *
     * @throws Exception _more_
     */
    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            for (int cnt = 0; cnt < 5; cnt++) {
                //                InputStream inputStream = new FileInputStream(arg);
                //                inputStream = new BufferedInputStream(inputStream, 10000);
                //                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                BufferedReader reader =
                    new BufferedReader(new FileReader(arg));
                long t1 = System.currentTimeMillis();
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    List<String> toks = StringUtil.split(line, " ", true,
                                            true);
                }
                long t2 = System.currentTimeMillis();
                System.err.println("Time:" + (t2 - t1) / 1000);
            }
        }
    }

    /**
     * Set the Delimiter property.
     *
     * @param value The new value for Delimiter
     */
    public void setDelimiter(String value) {
        this.delimiter = value;
    }

    /**
     * Get the Delimiter property.
     *
     * @return The Delimiter
     */
    public String getDelimiter() {
        return this.delimiter;
    }



    /**
     * Set the ColorByIndex property.
     *
     * @param value The new value for ColorByIndex
     */
    public void setColorByIndex(int value) {
        this.colorByIndex = value;
    }

    /**
     * Get the ColorByIndex property.
     *
     * @return The ColorByIndex
     */
    public int getColorByIndex() {
        return this.colorByIndex;
    }

    /**
     *  Set the Skip property.
     *
     *  @param value The new value for Skip
     */
    public void setSkip(int value) {
        this.skip = value;
    }

    /**
     *  Get the Skip property.
     *
     *  @return The Skip
     */
    public int getSkip() {
        return this.skip;
    }


    /**
     *  Set the Header property.
     *
     *  @param value The new value for Header
     */
    public void setHeader(String value) {
        this.header = value;
    }

    /**
     *  Get the Header property.
     *
     *  @return The Header
     */
    public String getHeader() {
        return this.header;
    }


    /**
     *  Set the GridWidth property.
     *
     *  @param value The new value for GridWidth
     */
    public void setGridWidth(int value) {
        this.gridWidth = value;
    }

    /**
     *  Get the GridWidth property.
     *
     *  @return The GridWidth
     */
    public int getGridWidth() {
        return this.gridWidth;
    }

    /**
     *  Set the GridHeight property.
     *
     *  @param value The new value for GridHeight
     */
    public void setGridHeight(int value) {
        this.gridHeight = value;
    }

    /**
     *  Get the GridHeight property.
     *
     *  @return The GridHeight
     */
    public int getGridHeight() {
        return this.gridHeight;
    }


    /**
     *  Set the HillShadeAzimuth property.
     *
     *  @param value The new value for HillShadeAzimuth
     */
    public void setHillShadeAzimuth(float value) {
        this.hillShadeAzimuth = value;
    }

    /**
     *  Get the HillShadeAzimuth property.
     *
     *  @return The HillShadeAzimuth
     */
    public float getHillShadeAzimuth() {
        return this.hillShadeAzimuth;
    }

    /**
     *  Set the HillShadeAngle property.
     *
     *  @param value The new value for HillShadeAngle
     */
    public void setHillShadeAngle(float value) {
        this.hillShadeAngle = value;
    }

    /**
     *  Get the HillShadeAngle property.
     *
     *  @return The HillShadeAngle
     */
    public float getHillShadeAngle() {
        return this.hillShadeAngle;
    }




}
