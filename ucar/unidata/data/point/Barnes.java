/*
 * $Id: IDV-Style.jin,v 1.5 2004/02/06 20:54:00 dmurray Exp $
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

//package au.gov.bom.aifs.osa.barnes;
package ucar.unidata.data.point;


/**
 * Title: Barnes Objective Analysis (BOA) Scheme
 * <p>
 * Description: The BOA is implemented as the static method "point2grid" and
 *              is for converting irregularly spaced data defined by
 *              (longitude, latitude, value) to a regular,
 *              but not neccessarily evenly spaced, grid whose
 *              coordinates are also defined by longitude and latitude.
 *
 *              Other methods useful to the BOA are also included in this
 *              package, such as scinex (an interpolation/extrapolation scheme).
 *
 * <p>
 * References:
 * <p>
 * Barnes, S.L., 1994a: Applications of the Barnes objective analysis scheme
 *     Part I: Effects of undersampling, wave position, and station randomness.
 *     J. Atmos. Oceanic Technol. 11, 1433-1448.
 * <p>
 * Barnes, S.L., 1994b:  Applications of the Barnes objective analysis scheme
 *     Part II: Improving derivative estimates.
 *     J. Atmos. Oceanic Technol. 11, 1449-1458.
 * <p>
 * Barnes, S.L., 1994c:  Applications of the Barnes objective analysis scheme
 *     Part III: Tuning for minimum error.
 *     J. Atmos. Oceanic Technol. 11, 1459-1479.
 *
 * @author  James Kelly
 * @version 1.0
 * Copyright:    Copyright (c) 2001 James Kelly and Australian Bureau of Meteorology
 */

public class Barnes {

    /** _more_ */
    static boolean debug = false;

    /** _more_ */
    static boolean reportRMSErrors = false;

    /**
     * Description: point2grid is for converting
     *              irregularly spaced data defined by (longitude, latitude, value)
     *              to
     *              a regular, but not neccessarily evenly spaced, grid whose
     *              coordinates are also defined by longitude and latitude
     * inputs
     * lon : the longitudes on the grid where interpolated values are desired
     *       (in degrees)
     * lat : the latitudes on the grid where interpolated values are desired
     *       (in degrees)
     * data3D : 3D array of data values where
     *          data3D[0][*] = longitudes of data values
     *          data3D[1][*] = latitudes of data values
     *          data3D[2][*] = data values
     *
     * scaleLength : the Gaussian scale length (in grid units)
     *               should be approximately equal to the average data spacing
     *               a suggested default: 10.0f (no scientific reason here)
     * gain: factor by which scaleLength is reduced for the second pass.
     *       Should be in the range 0.2 to 1.0. Data are fitted more closely with
     *       a gain of 0.2 (at the expense of less overall accuracy over the entire
     *       grid); larger values smooth more.
     *       Suggested default: 1.0f
     * iNumPasses: number of passes of the BOA to do.
     *             4 passes recommended for analysing fields where derivative
     *                      estimates are important (Ref: Barnes 1994b)
     *             3 passes recommended for all other fields (with gain set to 1.0)
     *                      (Ref: Barnes 1994c "Two pass Barnes Objective Analysis
     *                       schemes now in use probably should be replaced by
     *                       appropriately tuned 3pass or 4pass schemes")
     *             2 passes only recommended for "quick look" type analyses
     *
     * @param lon _more_
     * @param lat _more_
     * @param data3D _more_
     * @param scaleLength _more_
     * @param gain _more_
     * @param iNumPasses _more_
     *
     * @return _more_
     */

    public static double[][] point2grid(float[] lon, float[] lat,
                                        float[][] data3D, float scaleLength,
                                        float gain, int iNumPasses) {

        int   numLon  = lon.length;
        int   numLat  = lat.length;
        int   numData = data3D[0].length;

        float fMinLon = Barnes.min(lon);
        float fMinLat = Barnes.min(lat);

        // check dimensions of lon & lat
        //
        float fGridSpaceX = Math.abs(lon[1] - lon[0]);
        float fGridSpaceY = Math.abs(lat[1] - lat[0]);
        if (debug) {
            System.out.println("fGridSpace X = " + fGridSpaceX + " Y = "
                               + fGridSpaceY);
        }

        // create an array of longitudes/latitudes in grid units (gridSpaceX/Y)
        // corresponding to the data values
        float[] faLonValues = new float[numData];
        float[] faLatValues = new float[numData];
        float[] faValues    = new float[numData];

        for (int i = 0; i < numData; i++) {
            faLonValues[i] = (data3D[0][i] - fMinLon) / fGridSpaceX;
            if (debug) {
                System.out.println("faLonValues[" + i + "] = "
                                   + faLonValues[i]);
            }
        }

        for (int i = 0; i < numData; i++) {
            faLatValues[i] = (data3D[1][i] - fMinLat) / fGridSpaceY;
            if (debug) {
                System.out.println("faLatValues[" + i + "] = "
                                   + faLatValues[i]);
            }
        }

        for (int i = 0; i < numData; i++) {
            faValues[i] = data3D[2][i];
            if (debug) {
                System.out.println("faValues[" + i + "] = " + faValues[i]);
            }
        }

        double[][] daaGrid         = new double[numLon][numLat];
        double[][] daaWeights      = new double[numLon][numLat];
        double     dWeight         = 0.0d;
        double     dSumWeights     = 0.0d;
        double[][] daaCorrections  = new double[numLon][numLat];
        double     dCorrection     = 0.0d;
        double     dSumCorrections = 0.0d;

        double     r               = 0.0d;  // distance from grid point to location of data value
        double     r2              = 0.0d;  // r*r
        double     dx              = 0.0d;
        double     dy              = 0.0d;
        // for each grid point ....
        for (int i = 0; i < numLon; i++) {
            for (int j = 0; j < numLat; j++) {
                // .... loop over all data values and assign the
                // grid point the weighted average of all the data values
                dSumCorrections = 0.0d;
                dSumWeights     = 0.0d;
                for (int k = 0; k < numData; k++) {
                    dx = (double) faLonValues[k] - (double) i;
                    dy = (double) faLatValues[k] - (double) j;
                    if (debug) {
                        System.out.print("dx = " + dx + " ");
                    }
                    if (debug) {
                        System.out.print("dy = " + dy + " ");
                    }
                    r2 = (dx * dx) + (dy * dy);
                    r  = Math.sqrt(r2);
                    if (debug) {
                        System.out.print("r2 = " + r2 + " ");
                    }
                    if (debug) {
                        System.out.print("r = " + r + " ");
                    }
                    // assign this value a weight based on how far it is from the grid point
                    // (the weighting function is a "bell" shaped curve)
                    dWeight = Math.exp(-r2
                                       / ((double) (scaleLength
                                                    * scaleLength)));
                    dSumWeights += dWeight;
                    if (debug) {
                        System.out.print("value " + k + " = " + faValues[k]
                                         + " ");
                    }
                    if (debug) {
                        System.out.print("weight " + k + " = " + dWeight
                                         + " ");
                    }

                    dCorrection     = faValues[k] * dWeight;
                    dSumCorrections += dCorrection;
                    if (debug) {
                        System.out.println("value * weight = " + dCorrection
                                           + " dSumCorrections = "
                                           + dSumCorrections);
                    }

                }
                daaWeights[i][j]     = dSumWeights;
                daaCorrections[i][j] = dSumCorrections;
                if (debug) {
                    System.out.print("daaCorrections[" + i + "][" + j
                                     + "] = " + daaCorrections[i][j] + " ");
                }
                if (debug) {
                    System.out.println("daaWeights[" + i + "][" + j + "] = "
                                       + daaWeights[i][j]);
                }
            }
        }

        // initialise grid to the mean of all the data values
        // (safety procedure in case some grid points are a large distance
        //  from all data points)

        double gridMean = Barnes.mean(faValues);
        double epsilon  = 0.000000001d;

        for (int i = 0; i < numLon; i++) {
            for (int j = 0; j < numLat; j++) {
                daaGrid[i][j] = gridMean;
                if (daaWeights[i][j] > epsilon) {
                    daaGrid[i][j] = daaCorrections[i][j] / daaWeights[i][j];
                    if (debug) {
                        System.out.println("daaGrid[ " + i + "][" + j
                                           + "] = " + daaGrid[i][j]);
                    }
                } else {
                    if (debug) {
                        System.out.println("grid[" + i + "][" + j
                                           + "] set to mean value");
                    }
                }
            }
        }

        // now, based on the gridded values, obtain interpolated values
        // for each data point.
        // these interpolates will then be compared to each data value to
        // find out how "different" that analysis grid is from the data values

        double[] daInterpolatedData = new double[numData];
        double[] daDifferences      = new double[numData];
        double   dRMSE              = 0.0d;

        for (int k = 0; k < numData; k++) {
            daInterpolatedData[k] = Barnes.scinex(faLonValues[k],
                                                  faLatValues[k], daaGrid);
            daDifferences[k] = (double) faValues[k] - daInterpolatedData[k];

            // System.out.print(" k = " + k + " lon = " + faLonValues[k]
            //                               + " lat = " + faLatValues[k]
            //                               + " value = " + faValues[k]
            //                               + " interp = " + daInterpolatedData[k]);
        }
        // System.out.println(" ");

        if (reportRMSErrors) {
            dRMSE = Barnes.rmse(daDifferences);
            System.out.println(" Pass 1 RMSE (at data points) = " + dRMSE);
        }

        // now we are ready for pass 2
        // we have:
        // 1) a first attempt to estimate the grid (daaGrid)
        // 2) the differences between the grid point and the data values
        //    evaluated at each data point (ie an estimate of how "bad"
        //    the grid is at each data point). We will use this estimate
        //    to correct the grid on passes 2 and 3
        //    for each grid point ....



        // scaleLength may be reduced for the second pass (but not further reduced
        // for later passes
        // An aside:
        // it is better to do 3 passes, and leave scaleLength unchanged
        // ie a "gain" of 1
        // 2 passes with a gain of say 0.3 will produce a faster result, but
        // at the expense of accuracy
        // ie 2pass with gain 0.3 is ok for a "quick look" type analysis
        // but 3pass with gain 1 is better for scientific use

        scaleLength *= gain;

        for (int iPass = 2; iPass <= iNumPasses; iPass++) {
            if (debug) {
                System.out.println("Pass " + iPass);
            }
            for (int i = 0; i < numLon; i++) {
                for (int j = 0; j < numLat; j++) {
                    dSumCorrections = 0.0d;
                    dSumWeights     = 0.0d;
                    // .... loop over all data values and assign the
                    // grid point the weighted average of all the data values
                    for (int k = 0; k < numData; k++) {
                        dx = (double) faLonValues[k] - (double) i;
                        dy = (double) faLatValues[k] - (double) j;
                        r2 = Math.pow(dx, 2.0d) + Math.pow(dy, 2.0d);
                        r  = Math.sqrt(r2);
                        // assign this value a weight based on how far it is from the grid point
                        // (the weighting function is a "bell" shaped curve)
                        dWeight = Math.exp(-r2
                                           / Math.pow((double) scaleLength,
                                                      2.0d));
                        if (debug) {
                            System.out.println("weight for value " + k
                                               + " at grid[" + i + "][" + j
                                               + "] = " + daaWeights[i][j]);
                        }
                        dSumWeights += dWeight;

                        // this line different from pass 1:
                        dCorrection =
                            ((double) faValues[k] - daInterpolatedData[k])
                            * dWeight;
                        dSumCorrections += dCorrection;

                    }
                    daaWeights[i][j]     = dSumWeights;
                    daaCorrections[i][j] = dSumCorrections;
                }
            }

            for (int i = 0; i < numLon; i++) {
                for (int j = 0; j < numLat; j++) {
                    // no need for initialisation step in pass >= 2:
                    // daaGrid[i][j] = gridMean;
                    if (daaWeights[i][j] > epsilon) {
                        // this line also different for pass >= 2:
                        daaGrid[i][j] += daaCorrections[i][j]
                                         / daaWeights[i][j];
                        if (debug) {
                            System.out.println("daaGrid[ " + i + "][" + j
                                               + "] = " + daaGrid[i][j]);
                        }
                    }
                }
            }

            // now, based on the gridded values, obtain interpolated values
            // for each data point.
            // these interpolates will then be compared to each data value to
            // find out how "different" that analysis grid is from the data values

            // double[] daInterpolatedData = new double[numData];
            //
            // this step is not required on the last pass, as it is not followed by
            // a correction pass, therefore no need to interpolate..
            //
            if ((iPass < iNumPasses) || (reportRMSErrors)) {
                for (int k = 0; k < numData; k++) {
                    daInterpolatedData[k] = Barnes.scinex(faLonValues[k],
                                                          faLatValues[k],
                                                          daaGrid);
                    daDifferences[k] = (double) faValues[k]
                                       - daInterpolatedData[k];
                }
                if (reportRMSErrors) {
                    dRMSE = Barnes.rmse(daDifferences);
                    System.out.println(" Pass " + iPass
                                       + " RMSE (at data points) = " + dRMSE);
                }
            }

        }

        return (daaGrid);

        // System.out.println("Size of lon = " + numLon);
    }

    /**
     * _more_
     *
     * @param daDifferences _more_
     *
     * @return _more_
     */
    private static synchronized double rmse(double[] daDifferences) {
        int    numData         = daDifferences.length;
        double sumDifferences  = 0.0d;
        double sumDifferences2 = 0.0d;
        double dRMSE           = 0.0d;

        for (int k = 0; k < numData; k++) {
            sumDifferences2 += (daDifferences[k] * daDifferences[k]);
        }

        dRMSE = Math.sqrt(sumDifferences2 / numData);
        return (dRMSE);
    }


    /**
     * Simplest possible version of point2grid where the number of passes is
     * fixed at 3 (most common case, as recommended by Barnes)
     *
     * @param fLonMin _more_
     * @param fLatMin _more_
     * @param fLonMax _more_
     * @param fLatMax _more_
     * @param data3D _more_
     *
     * @return _more_
     */

    public static double[][] point2grid(float fLonMin, float fLatMin,
                                        float fLonMax, float fLatMax,
                                        float[][] data3D) {
        return (point2grid(fLonMin, fLatMin, fLonMax, fLatMax, data3D, 3));

    }

    /**
     * Simplified version of point2grid which calculates or assigns as many of
     * the analysis parameters as possible
     *
     * All the user needs to provide is the data to be analysed,
     * and the domain over which the analysis is to be carried out
     *
     * INPUT:
     *  faa3DData:  An array (3,ndata) where ndata is the number of
     *              data points, and can be any number larger than N.
     *              each row of data should contain a longitude, a
     *              latitude, and a value to be interpolated.
     *
     *      fLonMin: Minimum Longitude of analysis area (eg 140.0 = 140.0 E)
     *
     *      fLatMin : Minimum Latitude of analysis area  (eg -40.0 =  40.0 S)
     *
     *      fLonMax: Maximum Longitude of analysis area (eg 150.0 = 150.0 E)
     *
     *      fLatMax : Maximum Latitude of analysis area  (eg -30.0 =  30.0 S)
     *
     * OUTPUTS:
     *  double[][]   gridded data
     *
     *
     * @param fLonMin _more_
     * @param fLatMin _more_
     * @param fLonMax _more_
     * @param fLatMax _more_
     * @param data3D _more_
     * @param iNumPasses _more_
     *
     * @return _more_
     */

    public static double[][] point2grid(float fLonMin, float fLatMin,
                                        float fLonMax, float fLatMax,
                                        float[][] data3D, int iNumPasses) {


        float gain = 1.0f;


        AnalysisParameters ap = getRecommendedParameters(fLonMin, fLatMin,
                                    fLonMax, fLatMax, data3D);



        // this.point2grid(faLon, faLat, data3D, 10.0f, 0.3f, 4);
        double[][] daaGrid = point2grid(ap.faGridX, ap.faGridY, data3D,
                                        (float) ap.scaleLengthGU, gain,
                                        iNumPasses);

        return (daaGrid);
    }

    /**
     *
     * calculate the differences between the input irregular data and the analysed
     * grid at each data point (ie "How bad is the analysis?").
     *
     * input:
     *  data3D: irregular data
     *  daaGrid: analysed grid, returned from the point2grid routine
     *
     *  output:
     *  daDifferences: an array of all the differences
     *                 (between the irregular data and the value interpolated from
     *                  the analysed grid)
     *
     * @param lon _more_
     * @param lat _more_
     * @param data3D _more_
     * @param daaGrid _more_
     *
     * @return _more_
     */

    public static double[] pointDifferences(float[] lon, float[] lat,
                                            float[][] data3D,
                                            double[][] daaGrid) {

        int      numData           = data3D[0].length;
        float[]  faValues          = new float[numData];
        float[]  faLonValues       = new float[numData];
        float[]  faLatValues       = new float[numData];
        double[] daDifferences     = new double[numData];
        double   dInterpolatedData = 0.0d;

        float    fMinLon           = Barnes.min(lon);
        float    fMinLat           = Barnes.min(lat);

        // check dimensions of lon & lat
        //
        float fGridSpaceX = Math.abs(lon[1] - lon[0]);
        float fGridSpaceY = Math.abs(lat[1] - lat[0]);

        for (int i = 0; i < numData; i++) {
            faLonValues[i] = (data3D[0][i] - fMinLon) / fGridSpaceX;
            if (debug) {
                System.out.println("in pointDifferences, faLonValues[" + i
                                   + "] = " + faLonValues[i]);
            }
        }

        for (int i = 0; i < numData; i++) {
            faLatValues[i] = (data3D[1][i] - fMinLat) / fGridSpaceY;
            if (debug) {
                System.out.println("in pointDifferences, faLatValues[" + i
                                   + "] = " + faLatValues[i]);
            }
        }

        for (int i = 0; i < numData; i++) {
            faValues[i] = data3D[2][i];
            if (debug) {
                System.out.println("in pointDifferences, faValues[" + i
                                   + "] = " + faValues[i]);
            }
        }

        for (int k = 0; k < numData; k++) {
            dInterpolatedData = Barnes.scinex(faLonValues[k], faLatValues[k],
                                              daaGrid);
            daDifferences[k] = (double) faValues[k] - dInterpolatedData;
        }

        return (daDifferences);
    }

    /**
     * _more_
     *
     * @param fLonMin _more_
     * @param fLonMax _more_
     * @param gridX _more_
     *
     * @return _more_
     */
    public static float[] getRecommendedGridX(float fLonMin, float fLonMax,
                                              float gridX) {
        float fDegreesX     = fLonMax - fLonMin;
        int   numToDiscard  = 3;
        int   numToDiscard2 = numToDiscard * 2;
        if (debug) {
            System.out.println("fDegreesX, ap.gridX = " + fDegreesX + " "
                               + gridX);
        }

        int nX = (int) (fDegreesX / gridX) + 1;
        if (debug) {
            System.out.println("nX = " + nX);
        }

        float[] faLonGrid = null;
        float   fThisLon;
        /*
        if (nX > (numToDiscard2)) {
           faLonGrid = new float[nX-numToDiscard2];
           fThisLon = fLonMin + ( numToDiscard * gridX);
           for (int i = 0; i < (nX-numToDiscard2); i++) {
             faLonGrid[i] = fThisLon;
             fThisLon += gridX;
             System.out.println("faLonGrid[" + i + "] = " + faLonGrid[i]);
           }

        } else {
        */
        faLonGrid = new float[nX];
        fThisLon  = fLonMin;

        for (int i = 0; i < nX; i++) {
            faLonGrid[i] = fThisLon;
            fThisLon     += gridX;
            if (debug) {
                System.out.println("faLonGrid[" + i + "] = " + faLonGrid[i]);
            }
        }
        // }

        return (faLonGrid);
    }


    /**
     * _more_
     *
     * @param fLatMin _more_
     * @param fLatMax _more_
     * @param gridY _more_
     *
     * @return _more_
     */
    public static float[] getRecommendedGridY(float fLatMin, float fLatMax,
                                              float gridY) {
        int   numToDiscard  = 3;
        int   numToDiscard2 = numToDiscard * 2;
        float fDegreesY     = fLatMax - fLatMin;

        if (debug) {
            System.out.println("fDegreesY, gridY = " + fDegreesY + " "
                               + gridY);
        }

        int nY = (int) (fDegreesY / gridY) + 1;
        if (debug) {
            System.out.println("nY = " + nY);
        }
        //     float[] faLatGrid = new float[nY];
        //    float fThisLat = fLatMin;

        float[] faLatGrid = null;
        float   fThisLat;

        /* if (nY > (numToDiscard2)) {
           faLatGrid = new float[nY-numToDiscard2];
           fThisLat = fLatMin + ( numToDiscard * gridY);
           for (int i = 0; i < (nY-numToDiscard2); i++) {
             faLatGrid[i] = fThisLat;
             fThisLat += gridY;
             if (debug) System.out.println("faLatGrid[" + i + "] = " + faLatGrid[i]);
           }

        } else {
        */
        fThisLat  = fLatMin;
        faLatGrid = new float[nY];

        for (int i = 0; i < nY; i++) {
            faLatGrid[i] = fThisLat;
            fThisLat     += gridY;
            if (debug) {
                System.out.println("faLatGrid[" + i + "] = " + faLatGrid[i]);
            }
        }
        //}

        return (faLatGrid);
    }



    /**
     * _more_
     *
     * @param fa _more_
     *
     * @return _more_
     */
    private static double mean(float[] fa) {
        if (fa == null) {
            return Float.NaN;
        }
        int iLength = fa.length;

        if (iLength == 0) {
            return Float.NaN;
        }
        double sum = 0.0d;

        for (int i = 0; i < iLength; i++) {
            sum += (double) fa[i];
        }

        return (sum / iLength);
    }

    /**
     * _more_
     *
     * @param fa _more_
     *
     * @return _more_
     */
    static float max(float[] fa) {
        if (fa == null) {
            return Float.NaN;
        }
        int iLength = fa.length;

        if (iLength == 0) {
            return Float.NaN;
        }
        float fMax = fa[0];

        for (int i = 0; i < iLength; i++) {
            if (fa[i] > fMax) {
                fMax = fa[i];
            }
        }
        return fMax;

    }


    /**
     * _more_
     *
     * @param fa _more_
     *
     * @return _more_
     */
    static float min(float[] fa) {
        if (fa == null) {
            return Float.NaN;
        }
        int iLength = fa.length;

        if (iLength == 0) {
            return Float.NaN;
        }
        float fMin = fa[0];

        for (int i = 0; i < iLength; i++) {
            if (fa[i] < fMin) {
                fMin = fa[i];
            }
        }
        return fMin;

    }

    /**
     * _more_
     */
    public Barnes() {
        float[]   faLon  = { 140.0f, 150.0f, 160.0f };
        float[]   faLat  = { -40.0f, -50.0f, -60.0f };
        float[][] data3D = {
            { 140.0f, 150.0f, 160.0f }, { -40.0f, -50.0f, -60.0f },
            { 1.0f, 2.0f, 3.0f }
        };
        //                       data3D[0][*] = {lons}
        //                       data3D[1][*] = {lats}
        //                       data3D[2][*] = {values}

        float fLonMin = faLon[0];
        float fLonMax = faLon[2];
        float fLatMin = faLat[2];
        float fLatMax = faLat[0];


        AnalysisParameters ap = getRecommendedParameters(fLonMin, fLatMin,
                                    fLonMax, fLatMax, data3D);

        double[][] daaGrid = point2grid(fLonMin, fLatMin, fLonMax, fLatMax,
                                        data3D);

    }

    /**
     * _more_
     *
     * @param args _more_
     */
    public static void main(String[] args) {
        Barnes myBarnes = new Barnes();

    }

    /**
     * Inner class for use of getRecommendedParameters, to allow it to return
     * multiple values such as:
     *      gridX, gridY, scaleLengthGU, randomDataSpacing, faGridX, faGridY
     * @see #getRecommendedParameters(float, float, float, float, float[][])
     */

    public static class AnalysisParameters {

        /** _more_ */
        public double gridX;

        /** _more_ */
        public double gridY;

        /** _more_ */
        public double scaleLengthGU;

        /** _more_ */
        public double randomDataSpacing;
        // double actualStationSpacing;
        // double NUR;

        /** _more_ */
        public float[] faGridX;

        /** _more_ */
        public float[] faGridY;

        /**
         * _more_
         *
         * @param gridX _more_
         * @param gridY _more_
         * @param sl _more_
         * @param r _more_
         */
        public AnalysisParameters(double gridX, double gridY, double sl,
                                  double r) {
            this.gridX             = gridX;
            this.gridY             = gridY;
            this.scaleLengthGU     = sl;
            this.randomDataSpacing = r;
        }

        /**
         * _more_
         *
         * @param gridX _more_
         * @param gridY _more_
         * @param sl _more_
         * @param r _more_
         * @param faLonGrid _more_
         * @param faLatGrid _more_
         */
        public AnalysisParameters(double gridX, double gridY, double sl,
                                  double r, float[] faLonGrid,
                                  float[] faLatGrid) {
            this(gridX, gridY, sl, r);
            /*
            this.gridX = gridX;
            this.gridY = gridY;
            this.scaleLengthGU = sl;
            this.randomDataSpacing = r;
            */

            int lonSize = faLonGrid.length;
            int latSize = faLatGrid.length;

            this.faGridX = new float[lonSize];
            this.faGridY = new float[latSize];
            for (int i = 0; i < lonSize; i++) {
                this.faGridX[i] = faLonGrid[i];
            }

            // System.out.println("lonSize =" + lonSize);
            for (int j = 0; j < latSize; j++) {
                // System.out.println("lonSiz=" + lonSize);
                this.faGridY[j] = faLatGrid[j];
            }

        }

    }

    /**
     * One of the hardest things about objective analysis is choosing the
     * correct parameters for the analysis, and this routine provides sensible
     * recommendations on these parameters.
     * <p>
     * The literature (eg Barnes 1994 a,b,c) contains a lot of guidance on how
     * to select these parameters, all of which can be derived directly from
     * the average station spacing.
     * <p>
     * Strictly speaking the actual spacing between each station and its nearest
     * neighbours should be used (Barnes used the 6 nearest neighbours), but
     * the routine below uses the simpler approach of assuming a random spacing
     * of the stations, and calculates the random data spacing via a simple formula.
     * <p>
     * INPUT:
     * <p>
     *      faa3DData:  An array (3,ndata) where ndata is the number of
     *               data points.
     *               Each row of data should contain a longitude, a
     *               latitude, and a value to be interpolated.
     * <p>
     *      fLonMin: Minimum Longitude of analysis area (eg 140.0 = 140.0 E)
     * <p>
     *      fLatMin : Minimum Latitude of analysis area  (eg -40.0 =  40.0 S)
     * <p>
     *      fLonMax: Maximum Longitude of analysis area (eg 150.0 = 150.0 E)
     * <p>
     *      fLatMax : Maximum Latitude of analysis area  (eg -30.0 =  30.0 S)
     * <p>
     * OUTPUTS:
     * <p>
     *      gridX:  the length of 1 grid space in the X direction (degrees)
     *               (set to 0.3 * random data spacing)
     * <p>
     *      gridY:  the length of 1 grid space in the Y direction (degrees)
     *               (set to 0.3 * random data spacing)
     * <p>
     *      scaleLengthGU:      Recommended Gaussian length scale (in grid units).
     *               (set to random data spacing)
     * <p>
     *      randomDataSpacing:  Average spacing between station if stations were
     *                      randomly distributed  (km)
     * <p>
     *      faGridX: the array of recommended grid points along the x axis
     * <p>
     *      faGridY: the array of recommended grid points along the y axis
     * <p>
     *      actualStationSpacing (Not implemented): The average distance between each station and its
     *                            nearest neighbour
     * <p>
     *      NUR (Not implemented):  Non Uniformity Ratio
     *              Indicates how uniform data distribution is
     *              0 = perfectly regular data grid
     *              >= 1.1 indicates a pseudo random distribution
     *
     *
     *
     *   @author James  Kelly j.kelly@bom.gov.au
     *   @author Robert Dahni rrd@bom.gov.au      30/01/97  Original Code (IDL)
     *           round grid spacings to nearest 0.05 degrees 29/3/97   R.Dahni & J.Kelly
     *           round grid spacings to nearest 0.01 degrees 15/4/97   R.Dahni & J.Kelly
     *           convert to Java                             24/1/2001 J.Kelly
     *
     *
     *
     * @param fLonMin _more_
     * @param fLatMin _more_
     * @param fLonMax _more_
     * @param fLatMax _more_
     * @param faa3DData _more_
     *
     * @return _more_
     */

    public static synchronized AnalysisParameters getRecommendedParameters(
            float fLonMin, float fLatMin, float fLonMax, float fLatMax,
            float[][] faa3DData) {

        // pro barnes_r, fLonMin, fLatMin, fLonMax, fLatMax, faa3DData, gridX, gridY, scaleLengthGU, randomDataSpacing, actualStationSpacing, NUR
        double   result            = 0.0d;
        double   dKmx              = 0.0d;
        double   dKmy              = 0.0d;
        double[] daKm              = new double[2];
        double   randomDataSpacing = 0.0d;
        double   scaleLengthDeg    = 0.0;

        double   degreesX          = (double) (fLonMax - fLonMin);
        double   degreesY          = (double) (fLatMax - fLatMin);

        //
        // determine average station spacing
        //
        daKm = deg2km(fLonMin, fLatMin, fLonMax, fLatMax);
        dKmx = Math.abs(daKm[0]);
        dKmy = Math.abs(daKm[1]);

        int    sizeOfUniqueData = faa3DData[1].length;

        double degreesPerKmY    = degreesY / dKmy;
        double degreesPerKmX    = degreesX / dKmx;
        randomDataSpacing = Math.sqrt(dKmx * dKmy)
                            * ((1.0d + Math.sqrt(sizeOfUniqueData))
                               / (sizeOfUniqueData - 1.0d));
        scaleLengthDeg = randomDataSpacing * degreesPerKmX;
        double gridSpace = randomDataSpacing * 0.3d;

        // round to nearest 0.01 deg
        double gridSpaceDegX =
            (Math.round(gridSpace * degreesPerKmX * 100.0d)) / 100.0d;
        double gridX = gridSpaceDegX;
        if (debug) {
            System.out.println("gridSpace = " + gridSpace
                               + " gridSpaceDegX = " + gridSpaceDegX);
        }


        // round to nearest 0.01 deg
        double gridSpaceDegY =
            (Math.round(gridSpace * degreesPerKmY * 100.0d)) / 100.0d;
        double gridY         = gridSpaceDegY;

        double scaleLengthGU = scaleLengthDeg / gridX;
        /*
        // later we might consider calculating the actualStationSpacing
        // this then lets us calculate the "Non-Uniformity Ratio"
        // ...
        double actualStationSpacing  = stationSpacing(faa3DData)
        NUR = (randomDataSpacing-actualStationSpacing)/ actualStationSpacing
        if (NUR lt 0.0) then NUR = 0.0
        */

        // ap.actualStationSpacing = actualStationSpacing;
        // ap.NUR = NUR;

        float[] faLonGrid = getRecommendedGridX(fLonMin, fLonMax,
                                                (float) gridX);
        float[] faLatGrid = getRecommendedGridY(fLatMin, fLatMax,
                                                (float) gridY);
        if (debug) {
            System.out.print(" in getRecParam, faLonGrid length = "
                             + faLonGrid.length);
            System.out.println(" and, faLatGrid length = "
                               + faLatGrid.length);
        }

        AnalysisParameters ap = new AnalysisParameters(gridX, gridY,
                                    scaleLengthGU, randomDataSpacing,
                                    faLonGrid, faLatGrid);

        return (ap);
    }

    /**
     * Calculate the number of kms between 2 locations specified by
     * longitude and latutide:
     * (fLon1, fLat1) and (fLon2, fLat2)
     *
     * The return value is provided as a 2 element array, giving
     * km in the x direction and km in the y direction between the 2 points:
     * daKm[0] = dKmx
     * daKm[1] = dKmy
     *
     * @param fLon1 _more_
     * @param fLat1 _more_
     * @param fLon2 _more_
     * @param fLat2 _more_
     *
     * @return _more_
     */

    public static double[] deg2km(float fLon1, float fLat1, float fLon2,
                                  float fLat2) {
        double dAvgLat = (double) ((fLat1 + fLat2)) / 2.0d * Math.PI / 180.0d;
        double dKmx = (double) ((fLon2 - fLon1)) / 1000.0d
                      * ((111415.1d * Math.cos(dAvgLat))
                         - (94.54999d * Math.cos(3.0d * dAvgLat))
                         - (0.12d * Math.cos(5.0d * dAvgLat)));
        double dKmy = (double) ((fLat2 - fLat1)) / 1000.0d
                      * (111132.1d - (566.05d * Math.cos(2.0d * dAvgLat))
                         + (1.2d * Math.cos(4.0d * dAvgLat))
                         - (0.003d * Math.cos(6.0d * dAvgLat)));
        double[] daKm = new double[2];
        daKm[0] = dKmx;
        daKm[1] = dKmy;
        AnalysisParameters ap = new AnalysisParameters(0.0d, 0.0d, 0.0d,
                                    0.0d);

        return (daKm);
    }


    /**
     * Name:
     *       scinex
     *
     * PURPOSE:
     *       This function returns the value scint of a scalar field
     *       at a point gm,gn by interpolation or extrapolation of
     *       the field scala (Lagrangrangian cubic formula).
     *
     * CATEGORY:
     *       Interpolation
     *
     * CALLING SEQUENCE:
     *       scint = scinex(gm,gn,scala)
     *
     *
     * INPUTS:
     *
     *       gm:      The x-coordinate.
     *
     *       gn:      The y-coordinate.
     *
     *       scala:   A 2-d array (scalar field).
     *
     * KEYWORD PARAMETERS:
     *
     * OUTPUTS:
     *
     *       scint:   The interpolated data value.
     *
     * EXAMPLE:
     *
     * MODIFICATION HISTORY:
     *
     *       written by:    Robert Dahni    rrd@bom.gov.au  04/01/95  (IDL version)
     *                      James Kelly J.Kelly@bom.gov.au Jan 2001 (Java version)
     *
     *
     *
     * @param gm _more_
     * @param gn _more_
     * @param scala _more_
     *
     * @return _more_
     */
    // private static double scinex(float fLon, float fLat, double[][] grid) {
    private static double scinex(float gm, float gn, double[][] scala) {

        // start converted IDL scinex
        int    msize = scala.length;
        int    nsize = scala[0].length;
        int    mmax  = msize;
        int    nmax  = nsize;

        int    mmin  = 1;
        int    nmin  = 1;
        double dgm   = Math.floor(gm);
        double dgn   = Math.floor(gn);
        /* System.out.println("msize = " + msize + " nsize = " + nsize + " gm = " + gm
                                      + " gn = " + gn + "dgm = " + dgm +
                                      " dgn = " + dgn); */

        int   igm = (int) dgm;
        int   jgn = (int) dgn;
        float fm  = gm - (float) igm;
        float fn  = gn - (float) jgn;
        if (fm < 1.e-06) {
            fm = 0.0f;
        }
        if (fn < 1.e-06) {
            fn = 0.0f;
        }
        int    ms     = mmax - 1;
        int    ns     = nmax - 1;
        int    mr     = mmin + 1;
        int    nr     = nmin + 1;

        float  e      = 0.0f;
        double t1     = 0.0d;
        double t2     = 0.0d;
        double p      = 0.0d;
        double h      = 0.0d;
        double scinto = 0.0d;

        if (gm >= mmax) {

            if (gn >= nmax) {

                e = gm - (float) mmax;
                t1 = e * (scala[mmax - 1][nmax - 1]
                          - scala[ms - 1][nmax - 1]);
                e = gn - (float) nmax;
                t2 = e * (scala[mmax - 1][nmax - 1]
                          - scala[mmax - 1][ns - 1]);
                scinto = scala[mmax - 1][nmax - 1] + t1 + t2;
                return (scinto);
            } else if (gn < nmin) {

                e = gm - (float) mmax;
                t1 = e * (scala[mmax - 1][nmin - 1]
                          - scala[ms - 1][nmin - 1]);
                e = gn - (float) nmin;
                t2 = e * (scala[mmax - 1][nmin - 1]
                          - scala[mmax - 1][nr - 1]);
                scinto = scala[mmax - 1][nmin - 1] + t1 + t2;
                return (scinto);
            } else {

                p = scala[mmax - 1][jgn - 1]
                    + fn * (scala[mmax - 1][jgn] - scala[mmax - 1][jgn - 1]);
                h = scala[ms - 1][jgn - 1]
                    + fn * (scala[ms - 1][jgn] - scala[ms - 1][jgn - 1]);
                e      = gm - (float) mmax;
                scinto = p + e * (p - h);
                return (scinto);
            }  // added 20:06 23/01/01

        } else if (gm < mmin) {

            if (gn >= nmax) {

                e = gn - (float) nmax;
                t2 = e * (scala[mmin - 1][nmax - 1]
                          - scala[mmin - 1][ns - 1]);
                e = gm - (float) mmin;
                t1 = e * (scala[mmin - 1][nmax - 1]
                          - scala[mr - 1][nmax - 1]);
                scinto = scala[mmin - 1][nmax - 1] + t1 + t2;
                return (scinto);

            } else if (gn < nmin) {

                e = gn - (float) nmin;
                t2 = e * (scala[mmin - 1][nmin - 1]
                          - scala[mmin - 1][nr - 1]);
                e = gm - (float) mmin;
                t1 = e * (scala[mmin - 1][nmin - 1]
                          - scala[mr - 1][nmin - 1]);
                scinto = scala[mmin - 1][nmin - 1] + t1 + t2;
                return (scinto);

            } else {

                e = gm - (float) mmin;
                p = scala[mmin - 1][jgn - 1]
                    + fn * (scala[mmin - 1][jgn] - scala[mmin - 1][jgn - 1]);
                h = scala[mr - 1][jgn - 1]
                    + fn * (scala[mr - 1][jgn] - scala[mr - 1][jgn - 1]);
                scinto = p - e * (h - p);
                return (scinto);

            }

        } else if (gn >= nmax) {

            e = gn - (float) nmax;
            p = scala[igm - 1][nmax - 1]
                + fm * (scala[igm][nmax - 1] - scala[igm - 1][nmax - 1]);
            h = scala[igm - 1][ns - 1]
                + fm * (scala[igm][ns - 1] - scala[igm - 1][ns - 1]);
            scinto = p + e * (p - h);
            return (scinto);

        } else if (gn < nmin) {

            e = gn - (float) nmin;
            p = scala[igm - 1][nmin - 1]
                + fm * (scala[igm][nmin - 1] - scala[igm - 1][nmin - 1]);
            h = scala[igm - 1][nr - 1]
                + fm * (scala[igm][nr - 1] - scala[igm - 1][nr - 1]);
            scinto = p - e * (h - p);
            return (scinto);

        } else if ((gm < ms) && (gm >= mr) && (gn < ns) && (gn >= nr)) {


            float s1  = fm + 1.0f;
            float s2  = fm;
            float s3  = fm - 1.0f;
            float s4  = fm - 2.0f;
            float s12 = s1 * s2;
            float s34 = s3 * s4;
            float a   = -s2 * s34;
            float b   = 3.0f * s1 * s34;
            float c   = -3.0f * s12 * s4;
            float d   = s12 * s3;
            // System.out.println("msize = " + scala[0].length + " nsize = " + scala[1].length);
            // System.out.println(" igm = " + igm + " jgn = " + jgn);
            double x1 = a * scala[igm - 2][jgn - 2]
                        + b * scala[igm - 1][jgn - 2]
                        + c * scala[igm][jgn - 2]
                        + d * scala[igm + 1][jgn - 2];
            double x2 = a * scala[igm - 2][jgn - 1]
                        + b * scala[igm - 1][jgn - 1]
                        + c * scala[igm][jgn - 1]
                        + d * scala[igm + 1][jgn - 1];
            double x3 = a * scala[igm - 2][jgn] + b * scala[igm - 1][jgn]
                        + c * scala[igm][jgn] + d * scala[igm + 1][jgn];
            double x4 = a * scala[igm - 2][jgn + 1]
                        + b * scala[igm - 1][jgn + 1]
                        + c * scala[igm][jgn + 1]
                        + d * scala[igm + 1][jgn + 1];
            s1  = fn + 1.0f;
            s2  = fn;
            s3  = fn - 1.0f;
            s4  = fn - 2.0f;
            s12 = s1 * s2;
            s34 = s3 * s4;
            a   = -s2 * s34;
            b   = 3.0f * s1 * s34;
            c   = -3.0f * s12 * s4;
            d   = s12 * s3;
            double y = a * x1 + b * x2 + c * x3 + d * x4;
            scinto = y / 36.0;
            return (scinto);

        } else {

            p = scala[igm][jgn - 1]
                + fn * (scala[igm][jgn] - scala[igm][jgn - 1]);
            h = scala[igm - 1][jgn - 1]
                + fn * (scala[igm - 1][jgn] - scala[igm - 1][jgn - 1]);
            scinto = h + fm * (p - h);
            return (scinto);

        }
    }
}
