package au.gov.bom.aifs.osa.analysis;

/*
The Barnes library is part of the Australian Integrated Forecast System (AIFS).

Copyright (C) 2008 Australian Bureau of Meteorology.

This library is free software; you can redistribute it and/or modify it under the terms of the
GNU Lesser General Public License as published by the Free Software Foundation; either version
2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See
the GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License along with this
library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
Boston, MA 02111-1307 USA
*/

/**
 * Title: Barnes Objective Analysis (BOA) Scheme
 * <p>
 * Description: The BOA is implemented as the static method "point2grid" and is
 * for converting irregularly spaced data defined by (longitude, latitude,
 * value) to a regular, but not necessarily evenly spaced, grid whose
 * coordinates are also defined by longitude and latitude.
 * 
 * Other methods useful to the BOA are also included in this package, such as
 * scinex (an interpolation/extrapolation scheme).
 * 
 * <p>
 * References:
 * <p>
 * Barnes, S.L., 1994a: Applications of the Barnes objective analysis scheme
 * Part I: Effects of undersampling, wave position, and station randomness. J.
 * Atmos. Oceanic Technol. 11, 1433-1448.
 * <p>
 * Barnes, S.L., 1994b: Applications of the Barnes objective analysis scheme
 * Part II: Improving derivative estimates. J. Atmos. Oceanic Technol. 11,
 * 1449-1458.
 * <p>
 * Barnes, S.L., 1994c: Applications of the Barnes objective analysis scheme
 * Part III: Tuning for minimum error. J. Atmos. Oceanic Technol. 11, 1459-1479.
 * 
 * @author James Kelly
 * @version 1.0 Copyright: Copyright (c) 2001 Australian Bureau of Meteorology
 */

public class Barnes
{
	private static boolean debug = false;

	private static boolean reportRMSErrors = false;

	private static float epsilon = 1.0E-11f;

	private static float[][][] radii;

	public static boolean useRadii = false;

	/**
	 * Description: point2grid is for converting irregularly spaced data
	 * defined by (longitude, latitude, value) to a regular, but not
	 * neccessarily evenly spaced, grid whose coordinates are also defined
	 * by longitude and latitude inputs
	 * 
	 * @param lon :
	 *                the longitudes on the grid where interpolated values
	 *                are desired (in degrees)
	 * @param lat :
	 *                the latitudes on the grid where interpolated values
	 *                are desired (in degrees)
	 * @param data3D :
	 *                3D array of data values where data3D[0][*] =
	 *                longitudes of data values data3D[1][*] = latitudes of
	 *                data values data3D[2][*] = data values
	 * 
	 * @param scaleLength :
	 *                the Gaussian scale length (in grid units) should be
	 *                approximately equal to the average data spacing a
	 *                suggested default: 10.0f (no scientific reason here)
	 * @param gain
	 *                factor by which scaleLength is reduced for the second
	 *                pass. Should be in the range 0.2 to 1.0. Data are
	 *                fitted more closely with a gain of 0.2 (at the expense
	 *                of less overall accuracy over the entire grid); larger
	 *                values smooth more. Suggested default: 1.0f
	 * @param iNumPasses
	 *                number of passes of the BOA to do. 4 passes
	 *                recommended for analysing fields where derivative
	 *                estimates are important (Ref: Barnes 1994b) 3 passes
	 *                recommended for all other fields (with gain set to
	 *                1.0) (Ref: Barnes 1994c "Two pass Barnes Objective
	 *                Analysis schemes now in use probably should be
	 *                replaced by appropriately tuned 3pass or 4pass
	 *                schemes") 2 passes only recommended for "quick look"
	 *                type analyses
	 * 
	 * @return float[numLon][numLat] : regular gridded data
	 * 
	 */

	public synchronized static float[][] point2grid(float[] lon,
		float[] lat, float[][] data3D, float scaleLength, float gain,
		int iNumPasses)
	{
		int numLon = lon.length;
		int numLat = lat.length;
		int numData = data3D[0].length;

		float fMinLon = Barnes.min(lon);
		float fMinLat = Barnes.min(lat);

		// check dimensions of lon & lat
		float fGridSpaceX = Math.abs(lon[1] - lon[0]);
		float fGridSpaceY = Math.abs(lat[1] - lat[0]);

		// create array of lons/lats in grid units
		float[] faLonValues = new float[numData];
		float[] faLatValues = new float[numData];
		float[] faValues = new float[numData];

		for (int i = 0; i < numData; i++) {
			faLonValues[i] = (data3D[0][i] - fMinLon) / fGridSpaceX;
		}
		for (int i = 0; i < numData; i++) {
			faLatValues[i] = (data3D[1][i] - fMinLat) / fGridSpaceY;
		}
		for (int i = 0; i < numData; i++) {
			faValues[i] = data3D[2][i];
		}

		radii =
			setupRadii(numLon, numLat, faLonValues, faLatValues,
				scaleLength * scaleLength);

		float[][] faaGrid = passOne(lon, lat, data3D, scaleLength);

		// now, based on the gridded values, obtain interpolated values
		// for each data point.
		// these interpolates will then be compared to each data
		// value to find out how "different" that analysis grid is
		// from the data values
		float[] faDifferences = new float[numData];

		for (int k = 0; k < numData; k++) {
			float fInterpolatedData =
				Barnes.scinex(faLonValues[k] + 1,
					faLatValues[k] + 1, faaGrid);
			faDifferences[k] = faValues[k] - fInterpolatedData;
		}

		// now we are ready for pass 2
		// we have:
		// 1) a first attempt to estimate the grid (daaGrid)
		// 2) the differences between the grid point and the data values
		// evaluated at each data point (ie an estimate of how "bad"
		// the grid is at each data point). We will use this estimate
		// to correct the grid on passes 2 and 3
		// for each grid point ....
		// scaleLength may be reduced for the second pass
		// (but not further reduced) for later passes
		// An aside:
		// it is better to do 3 passes, and leave scaleLength unchanged
		// ie a "gain" of 1
		// 2 passes with a gain of say 0.3 will produce a faster
		// result, but at the expense of accuracy
		// ie 2pass with gain 0.3 is ok for a "quick look" type 
		// analysis but 3pass with gain 1 is better for scientific use
		// aside: Pass 1 RMSE at data points is
		// dRMSE = Barnes.rmse(daDifferences);

		scaleLength *= gain;
		// pass 2, which is very similar to the method "passOne"
		//
		faaGrid =
			pass2toN(lon, lat, data3D, scaleLength, faaGrid,
				faDifferences, iNumPasses);

		radii = null;
		//System.gc();

		return (faaGrid);
	}

	/**
	 * Performs the same task as point2grid, but instead of the first step,
	 * it uses a supplied first guess field (eg. a model background)
	 */
	public synchronized static float[][] point2grid(float[] lon,
		float[] lat, float[][] data3D, float[][] firstGuess,
		float scaleLength, float gain, int iNumPasses)
	{
		int numLon = lon.length;
		int numLat = lat.length;
		int numData = data3D[0].length;

		float fMinLon = Barnes.min(lon);
		float fMinLat = Barnes.min(lat);

		// check dimensions of lon & lat
		float fGridSpaceX = Math.abs(lon[1] - lon[0]);
		float fGridSpaceY = Math.abs(lat[1] - lat[0]);

		// create array of lons/lats in grid units
		float[] faLonValues = new float[numData];
		float[] faLatValues = new float[numData];
		float[] faValues = new float[numData];

		for (int i = 0; i < numData; i++) {
			faLonValues[i] = (data3D[0][i] - fMinLon) / fGridSpaceX;
		}
		for (int i = 0; i < numData; i++) {
			faLatValues[i] = (data3D[1][i] - fMinLat) / fGridSpaceY;
		}
		for (int i = 0; i < numData; i++) {
			faValues[i] = data3D[2][i];
		}

		radii =
			setupRadii(numLon, numLat, faLonValues, faLatValues,
				scaleLength * scaleLength);

		float gridMean = (float)Barnes.mean(faValues);
		firstGuess = filterNaN(firstGuess, gridMean);

		// Set the gridded analysis to the first guess
		float[][] faaGrid = firstGuess;

		// now, based on the gridded values, obtain interpolated values
		// for each data point.
		// these interpolates will then be compared to each data
		// value to find out how "different" that analysis grid is
		// from the data values
		float[] faDifferences = new float[numData];

		for (int k = 0; k < numData; k++) {
			// NOTE Scinex is based on a fortran subroutine and
			// is expecting the arrays to be indexed from 1 ->
			// not 0 -> so add one to the coordinate arguments
			float fInterpolatedData =
				Barnes.scinex(faLonValues[k] + 1,
					faLatValues[k] + 1, faaGrid);
			faDifferences[k] = faValues[k] - fInterpolatedData;
		}

		// now we are ready for pass 2
		// we have:
		// 1) a first attempt to estimate the grid (faaGrid)
		// 2) the differences between the grid point and the data 
		// values evaluated at each data point 
		// (ie an estimate of how "bad" grid is at each data point). 
		// We will use this estimate
		// to correct the grid on passes 2 and 3
		// for each grid point ....
		// scaleLength may be reduced for the second pass
		// (but not further reduced) for later passes
		// An aside:
		// it is better to do 3 passes, and leave scaleLength unchanged
		// ie a "gain" of 1
		// 2 passes with a gain of say 0.3 will produce a faster
		// result, but at the expense of accuracy
		// ie 2pass with gain 0.3 is ok for a "quick look" type 
		// analysis but 3pass with gain 1 is better for scientific use
		// aside: Pass 1 RMSE at data points is
		// dRMSE = Barnes.rmse(daDifferences);

		// normal Barnes analysis goes like this:
		// first pass
		// reduce scale length to "pay" obs better
		// passes 2 to N

		// here we have a modified Barnes, where the firstGuess has 
		// been supplied by the NWP. So we then want to run a 
		// "first pass" with the full scale length, before reducing 
		// the scale length in subsequent
		// so the modified process goes like this:
		// 
		// zero pass (first guess)
		// first pass (which will be performed by calling pass2toN 
		//             with numPasses set to 2, to perform a single 
		//		pass)
		// passes 2 to N (call pass2toN again to do "normal" second 
		//                pass onwards)
		// So here we go...
		//		
		//
		// 
		// first pass (but by calling pass2toN as above)
		faaGrid =
			pass2toN(lon, lat, data3D, scaleLength, faaGrid,
				faDifferences, 2);

		// passes 2 to N with reduced scale length...
		scaleLength *= gain;
		faaGrid =
			pass2toN(lon, lat, data3D, scaleLength, faaGrid,
				faDifferences, iNumPasses);
		radii = null;

		return (faaGrid);
	}

	private static float[][] pass2toN(float[] lon, float[] lat,
		float[][] data3D, float scaleLength,
		// float[] faLonValues, float[] faLatValues,
		// float[] faValues, float scaleLength,
		float[][] faaGrid, float[] faDifferences, int iNumPasses)
	{
		int numLon = lon.length;
		int numLat = lat.length;
		int numData = data3D[0].length;

		float fMinLon = Barnes.min(lon);
		float fMinLat = Barnes.min(lat);

		// check dimensions of lon & lat
		float fGridSpaceX = Math.abs(lon[1] - lon[0]);
		float fGridSpaceY = Math.abs(lat[1] - lat[0]);

		// create array of lons/lats in grid units
		float[] faLonValues = new float[numData];
		float[] faLatValues = new float[numData];
		float[] faValues = new float[numData];

		for (int i = 0; i < numData; i++) {
			faLonValues[i] = (data3D[0][i] - fMinLon) / fGridSpaceX;
		}
		for (int i = 0; i < numData; i++) {
			faLatValues[i] = (data3D[1][i] - fMinLat) / fGridSpaceY;
		}
		for (int i = 0; i < numData; i++) {
			faValues[i] = data3D[2][i];
		}

		float[][] faaWeights = new float[numLon][numLat];
		float fWeight = 0.0f;
		float fSumWeights = 0.0f;
		float[][] faaCorrections = new float[numLon][numLat];
		float fCorrection = 0.0f;
		float fSumCorrections = 0.0f;
		float scaleLength2 = (scaleLength * scaleLength);

		float r = 0.0f; // distance from grid point to data location
		float r2 = 0.0f; // r*r
		float dx = 0.0f;
		float dy = 0.0f;

		// obs further away than "radius of influence" contribute
		// less than "epsilon" (1/1000th) of their value to a given
		// grid point. If we ignore obs further away than this,
		// we should get some performance improvement in the
		// analysis. Note: if performance is not an issue, then
		// all obs should be included.
		// radiusOfInfluence squared is:
		float radiusOfInfluence2 =
			-scaleLength2 * (float)Math.log(epsilon);

		for (int iPass = 2; iPass <= iNumPasses; iPass++) {
			for (int i = 0; i < numLon; i++) {
				for (int j = 0; j < numLat; j++) {
					fSumCorrections = 0.0f;
					fSumWeights = 0.0f;
					// .... loop over all data values and
					// assign the grid point the
					// weighted average of all the data
					// values
					for (int k = 0; k < numData; k++) {
						// assign this value a weight
						// based on how far it is from
						// the grid point
						// (the weighting function is a
						// "bell" shaped curve)

                                                if (useRadii) {
						    r2 = radii[i][j][k];
                                                } else {
                                                    dx = faLonValues[k]- (float)i;
                                                    dy = faLatValues[k]- (float)j;
                                                    r2 = dx*dx + dy*dy;
                                                }
						if ((!Float
							.isNaN(faDifferences[k]))
							&& (r2 < radiusOfInfluence2))
						{
							fWeight =
								(float)Math
									.exp(-r2
										/ scaleLength2);
							fSumWeights += fWeight;
							// this line different
							// to pass 1:
							fCorrection =
								faDifferences[k]
									* fWeight;
							fSumCorrections +=
								fCorrection;
						}

					}
					faaWeights[i][j] = (float)fSumWeights;
					faaCorrections[i][j] =
						(float)fSumCorrections;
				}
			}

			for (int i = 0; i < numLon; i++) {
				for (int j = 0; j < numLat; j++) {
					// no need for initialisation step in
					// pass >= 2:
					// daaGrid[i][j] = gridMean;
					if (faaWeights[i][j] > (float)epsilon) {
						// this line also different for
						// pass >= 2:
						faaGrid[i][j] +=
							faaCorrections[i][j]
								/ faaWeights[i][j];
					}
				}
			}

			// now, based on the gridded values, obtain
			// interpolated values for each data point.
			// these interpolates will then be compared to
			// each data value to find out how "different"
			// that analysis grid is from the data values

			// this step is not required on the last pass,
			// as it is not followed by a correction pass,
			// therefore no need to interpolate..
			if ((iPass < iNumPasses) || (reportRMSErrors)) {
				for (int k = 0; k < numData; k++) {
					// NOTE Scinex is based on a fortran
					// subroutine and
					// is expecting the arrays to be 
					// indexed from 1 ->
					// not 0 -> so add one to the 
					// coordinate arguments
					float fInterpolatedData =
						Barnes.scinex(
							faLonValues[k] + 1,
							faLatValues[k] + 1,
							faaGrid);
					faDifferences[k] =
						faValues[k] - fInterpolatedData;
				}
				// RMSE (at data points) =
				// Barnes.rmse(daDifferences);
			}
		}

		return faaGrid;
	}

	private static float[][] passOne(float[] lon, float[] lat,
		float[][] data3D, float scaleLength)
	{

		int numLon = lon.length;
		int numLat = lat.length;
		int numData = data3D[0].length;

		float fMinLon = Barnes.min(lon);
		float fMinLat = Barnes.min(lat);

		// check dimensions of lon & lat
		float fGridSpaceX = Math.abs(lon[1] - lon[0]);
		float fGridSpaceY = Math.abs(lat[1] - lat[0]);

		// create array of lons/lats in grid units
		float[] faLonValues = new float[numData];
		float[] faLatValues = new float[numData];
		float[] faValues = new float[numData];

		for (int i = 0; i < numData; i++) {
			faLonValues[i] = (data3D[0][i] - fMinLon) / fGridSpaceX;
		}
		for (int i = 0; i < numData; i++) {
			faLatValues[i] = (data3D[1][i] - fMinLat) / fGridSpaceY;
		}
		for (int i = 0; i < numData; i++) {
			faValues[i] = data3D[2][i];
		}
		float[][] faaGrid = new float[numLon][numLat];
		float[][] faaWeights = new float[numLon][numLat];
		float fWeight = 0.0f;
		float fSumWeights = 0.0f;
		float[][] faaCorrections = new float[numLon][numLat];
		float fCorrection = 0.0f;
		float fSumCorrections = 0.0f;
		float scaleLength2 = (scaleLength * scaleLength);

		float r = 0.0f; // distance from grid point to data
		// location
		float r2 = 0.0f; // r*r
                float dx = 0.0f;
                float dy = 0.0f;

		// obs further away than "radius of influence" contribute
		// less than "epsilon" (1/1000th) of their value to a given
		// grid point. If we ignore obs further away than this,
		// we should get some performance improvement in the
		// analysis. Note: if performance is not an issue, then
		// all obs should be included.
		// radiusOfInfluence squared is:
		float radiusOfInfluence2 =
			-scaleLength2 * (float)Math.log(epsilon);
		// for each grid point ....
		for (int i = 0; i < numLon; i++) {
			for (int j = 0; j < numLat; j++) {
				// .... loop over all data values and assign
				// the grid point the weighted average of all
				// the data values
				fSumCorrections = 0.0f;
				fSumWeights = 0.0f;
				for (int k = 0; k < numData; k++) {
					// assign this value a weight based on
					// how far it is from the grid point
					// (the weighting function is a "bell"
					// shaped curve)
                                    if (useRadii) {
                                        r2 = radii[i][j][k];
                                    } else {
                                        dx = faLonValues[k]- (float)i;
                                        dy = faLatValues[k]- (float)j;
                                        r2 = dx*dx + dy*dy;
                                    }
					if ((r2 < radiusOfInfluence2)
						&& (!Float.isNaN(faValues[k])))
					{
						fWeight =
							(float)Math.exp(-r2
								/ scaleLength2);
						fSumWeights += fWeight;
						fCorrection =
							faValues[k] * fWeight;
						fSumCorrections += fCorrection;
					}
				}
				faaWeights[i][j] = (float)fSumWeights;
				faaCorrections[i][j] = (float)fSumCorrections;
			}
		}

		// initialise grid to the mean of all the data values
		// (safety procedure in case some grid points are a
		// large distance from all data points)
		float gridMean = Barnes.mean(faValues);
		for (int i = 0; i < numLon; i++) {
			for (int j = 0; j < numLat; j++) {
				faaGrid[i][j] = (float)gridMean;
				if (faaWeights[i][j] > (float)epsilon) {
					faaGrid[i][j] =
						faaCorrections[i][j]
							/ faaWeights[i][j];
				}
			}
		}

		return faaGrid;
	}

	private static synchronized float rmse(float[] daDifferences)
	{
		int numData = daDifferences.length;
		float sumDifferences = 0.0f;
		float sumDifferences2 = 0.0f;
		float dRMSE = 0.0f;

		for (int k = 0; k < numData; k++) {
			sumDifferences2 +=
				(daDifferences[k] * daDifferences[k]);
		}

		dRMSE = (float)Math.sqrt(sumDifferences2 / numData);
		return (dRMSE);
	}

	/**
	 * Simplest possible version of point2grid where the number of passes 
	 * is fixed at 3 (most common case, as recommended by Barnes)
	 * 
	 * @param fLonMin :
	 *                minimum longitude for grid
	 * @param fLatMin :
	 *                minimum latitude for grid
	 * @param fLonMax :
	 *                maximum longitude for grid
	 * @param fLatMax :
	 *                maximum latitude for grid
	 * @param data3D :
	 *                3D array of data values
	 * @return float[numLon][numLat] : regular gridded data
	 */

	public static float[][] point2grid(float fLonMin, float fLatMin,
		float fLonMax, float fLatMax, float[][] data3D)
	{
		return (point2grid(fLonMin, fLatMin, fLonMax, fLatMax, data3D,
			3));
	}

	/**
	 * Simplified version of point2grid which calculates or assigns as many
	 * of the analysis parameters as possible
	 * 
	 * All the user needs to provide is the data to be analysed, and the
	 * domain over which the analysis is to be carried out
	 * 
	 * @param fLonMin :
	 *                minimum longitude for grid (eg 140.0 = 140.0 E)
	 * @param fLatMin :
	 *                minimum latitude for grid (eg -40.0 = 40.0 S)
	 * @param fLonMax :
	 *                maximum longitude for grid (eg 150.0 = 150.0 E)
	 * @param fLatMax :
	 *                maximum latitude for grid (eg -30.0 = 30.0 S)
	 * @param data3D :
	 *                3D array of data values (3, ndata) where ndata is the
	 *                number of data points, and can be any number larger
	 *                than N. each row of data should contain a longitude,
	 *                a latitude, and a value to be interpolated.
	 * @param iNumPasses
	 *                number of passes of the BOA to do.
	 * @return float[numLon][numLat] : regular gridded data
	 * 
	 */

	public static float[][] point2grid(float fLonMin, float fLatMin,
		float fLonMax, float fLatMax, float[][] data3D, int iNumPasses)
	{

		float gain = 1.0f;

		AnalysisParameters ap =
			getRecommendedParameters(fLonMin, fLatMin, fLonMax,
				fLatMax, data3D);

		float[][] faaGrid =
			point2grid(ap.getGridXArray(), ap.getGridYArray(),
				data3D, (float)ap.getScaleLengthGU(), gain,
				iNumPasses);
		return (faaGrid);
	}

	/**
	 * 
	 * calculate the differences between the input irregular data and the
	 * analysed grid at each data point (ie "How bad is the analysis?").
	 * 
	 * @param lon :
	 *                the longitudes on the grid
	 * @param lat :
	 *                the latitudes on the grid
	 * @param data3D :
	 *                3D array of irregularly spaced data values
	 * @param daaGrid :
	 *                regular gridded data
	 * @return float[] daDifferences: an array of all the differences
	 *         (between the irregular data and the value interpolated from
	 *         the analysed grid)
	 */

	public static float[] pointDifferences(float[] lon, float[] lat,
		float[][] data3D, float[][] faaGrid)
	{
		int numData = data3D[0].length;
		float[] faValues = new float[numData];
		float[] faLonValues = new float[numData];
		float[] faLatValues = new float[numData];
		float[] faDifferences = new float[numData];
		float fInterpolatedData = 0.0f;

		float fMinLon = Barnes.min(lon);
		float fMinLat = Barnes.min(lat);

		// check dimensions of lon & lat
		//
		float fGridSpaceX = Math.abs(lon[1] - lon[0]);
		float fGridSpaceY = Math.abs(lat[1] - lat[0]);

		for (int i = 0; i < numData; i++) {
			faLonValues[i] = (data3D[0][i] - fMinLon) / fGridSpaceX;
		}

		for (int i = 0; i < numData; i++) {
			faLatValues[i] = (data3D[1][i] - fMinLat) / fGridSpaceY;
		}

		for (int i = 0; i < numData; i++) {
			faValues[i] = data3D[2][i];
		}

		for (int k = 0; k < numData; k++) {
			fInterpolatedData =
				Barnes.scinex(faLonValues[k], faLatValues[k],
					faaGrid);
			faDifferences[k] = faValues[k] - fInterpolatedData;
		}

		return (faDifferences);
	}

	/**
	 * get recommended spacing of grid in X direction
	 * 
	 * @param fLonMin :
	 *                minimum longitude for grid
	 * @param fLonMax :
	 *                maximum longitude for grid
	 * @param gridX :
	 *                the length of 1 grid space in X direction (degrees)
	 * @return float[] : array of grid points in X direction
	 */

	public static float[] getRecommendedGridX(float fLonMin, float fLonMax,
		float gridX)
	{
		float fDegreesX = fLonMax - fLonMin;

		int nX = (int)(fDegreesX / gridX) + 1;

		float[] faLonGrid = null;
		float fThisLon;
		faLonGrid = new float[nX];
		fThisLon = fLonMin;

		for (int i = 0; i < nX; i++) {
			faLonGrid[i] = fThisLon;
			fThisLon += gridX;
		}

		return (faLonGrid);
	}

	/**
	 * get recommended spacing of grid in X direction
	 * 
	 * @param fLatMin :
	 *                minimum latitude for grid
	 * @param fLatMax :
	 *                maximum latitude for grid
	 * @param gridY :
	 *                the length of 1 grid space in Y direction (degrees)
	 * @return float[] : array of grid points in Y direction
	 */

	public static float[] getRecommendedGridY(float fLatMin, float fLatMax,
		float gridY)
	{
		float fDegreesY = fLatMax - fLatMin;
		int nY = (int)(fDegreesY / gridY) + 1;

		float[] faLatGrid = null;
		float fThisLat;

		fThisLat = fLatMin;
		faLatGrid = new float[nY];

		for (int i = 0; i < nY; i++) {
			faLatGrid[i] = fThisLat;
			fThisLat += gridY;
		}

		return (faLatGrid);
	}

	private static float mean(float[] fa)
	{
		if (fa == null) {
			return Float.NaN;
		}
		int iLength = fa.length;

		if (iLength == 0) {
			return Float.NaN;
		}
		float sum = 0.0f;

		for (int i = 0; i < iLength; i++) {
			sum += fa[i];
		}

		return (sum / iLength);
	}

	static float max(float[] fa)
	{
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

	static float min(float[] fa)
	{
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
	 * Default constructor to demonstrate use of Barnes Analysis
	 */

	public Barnes()
	{
		float[] faLon = { 140.0f, 150.0f, 160.0f };
		float[] faLat = { -40.0f, -50.0f, -60.0f };
		float[][] data3D =
			{ { 140.0f, 150.0f, 160.0f },
				{ -40.0f, -50.0f, -60.0f },
				{ 1.0f, 2.0f, 3.0f } };
		// data3D[0][*] = {lons}
		// data3D[1][*] = {lats}
		// data3D[2][*] = {values}

		float fLonMin = faLon[0];
		float fLonMax = faLon[2];
		float fLatMin = faLat[2];
		float fLatMax = faLat[0];

		AnalysisParameters ap =
			getRecommendedParameters(fLonMin, fLatMin, fLonMax,
				fLatMax, data3D);

		float[][] daaGrid =
			point2grid(fLonMin, fLatMin, fLonMax, fLatMax, data3D);

	}

	/**
	 * Example main routine for testing Barnes Analysis
	 * 
	 * @param args
	 *                any command line arguments
	 */
	// public static void main(String[] args)
	// {
	// Barnes myBarnes = new Barnes();
	// }
	/**
	 * Inner class for use of getRecommendedParameters, to allow it to
	 * return multiple values such as: gridX, gridY, scaleLengthGU,
	 * randomDataSpacing, faGridX, faGridY gridX:the length of 1 grid space
	 * in the X direction (degrees) (set to 0.3 * random data spacing)
	 * gridY: the length of 1 grid space in the Y direction (degrees) (set
	 * to 0.3 * random data spacing) scaleLengthGU: Recommended Gaussian
	 * length scale (in grid units). (set to random data spacing)
	 * randomDataSpacing: Average spacing between station if stations were
	 * randomly distributed (km) faGridX: the array of recommended grid
	 * points along the x axis faGridY: array of recommended grid points
	 * along the y axis actualStationSpacing (Not implemented): The average
	 * distance between each station and its nearest neighbour NUR (Not
	 * implemented): Non Uniformity Ratio Indicates how uniform data
	 * distribution is 0 = perfectly regular data grid >= 1.1 indicates a
	 * pseudo random distribution
	 */
	public static class AnalysisParameters
	{
		private double gridX;

		private double gridY;

		private double scaleLengthGU;

		private double randomDataSpacing;

		private float[] faGridX;

		private float[] faGridY;

		/**
		 * public accessor method for faGridX
		 * 
		 * @return grid array in X direction (longitudes)
		 */
		public float[] getGridXArray()
		{
			return faGridX;
		}

		/**
		 * public accessor method for faGridY
		 * 
		 * @return grid array in Y direction (latitudes)
		 */
		public float[] getGridYArray()
		{
			return faGridY;
		}

		/**
		 * public accessor method for gridX
		 * 
		 * @return grid spacing in X direction
		 */
		public double getGridX()
		{
			return gridX;
		}

		/**
		 * public accessor method for gridY
		 * 
		 * @return grid spacing in Y direction
		 */
		public double getGridY()
		{
			return gridY;
		}

		/**
		 * public accessor method for scaleLengthGU
		 * 
		 * @return scale length in Grid Units
		 */
		public double getScaleLengthGU()
		{
			return scaleLengthGU;
		}

		/**
		 * public accessor method for getRandomDataSpacing
		 * 
		 * @return the random data spacing
		 */
		public double getRandomDataSpacing()
		{
			return randomDataSpacing;
		}

		/**
		 * public accessor method to set scaleLengthGU
		 * 
		 * @param scaleLengthGU
		 *                scale length in Grid Units
		 */
		public void setScaleLengthGU(double scaleLengthGU)
		{
			this.scaleLengthGU = scaleLengthGU;
		}

		/**
		 * public accessor method to set the X grid points
		 * 
		 * @param faGridX
		 *                the X Grid points
		 */
		public void setGridXArray(float[] faGridX)
		{
			this.faGridX = faGridX;
		}

		/**
		 * public accessor method to set the Ygrid points
		 * 
		 * @param faGridX
		 *                the Y Grid points
		 */
		public void setGridYArray(float[] faGridY)
		{
			this.faGridY = faGridY;
		}

		/**
		 * @param gridX :
		 *                the length of 1 grid space in X direction
		 *                (degrees)
		 * @param gridY :
		 *                the length of 1 grid space in Y direction
		 *                (degrees)
		 * @param sl :
		 *                Scale Length parameter
		 * @param r :
		 *                Random Data Spacing
		 */
		public AnalysisParameters(double gridX, double gridY,
			double sl, double r)
		{
			this.gridX = gridX;
			this.gridY = gridY;
			this.scaleLengthGU = sl;
			this.randomDataSpacing = r;
		}

		/**
		 * @param gridX :
		 *                the length of 1 grid space in X direction
		 *                (degrees)
		 * @param gridY :
		 *                the length of 1 grid space in Y direction
		 *                (degrees)
		 * @param sl :
		 *                Scale Length parameter
		 * @param r :
		 *                Random Data Spacing
		 * @param faLonGrid :
		 *                array of grid points in X direction
		 * @param faLatGrid :
		 *                array of grid points in Y direction
		 */
		public AnalysisParameters(double gridX, double gridY,
			double sl, double r, float[] faLonGrid,
			float[] faLatGrid)
		{
			this(gridX, gridY, sl, r);

			int lonSize = faLonGrid.length;
			int latSize = faLatGrid.length;

			this.faGridX = new float[lonSize];
			this.faGridY = new float[latSize];
			for (int i = 0; i < lonSize; i++) {
				this.faGridX[i] = faLonGrid[i];
			}

			for (int j = 0; j < latSize; j++) {
				this.faGridY[j] = faLatGrid[j];
			}

		}

	}

	/**
	 * One of the hardest things about objective analysis is choosing the
	 * correct parameters for the analysis, and this routine provides
	 * sensible recommendations on these parameters.
	 * <p>
	 * The literature (eg Barnes 1994 a,b,c) contains a lot of guidance on
	 * how to select these parameters, all of which can be derived directly
	 * from the average station spacing.
	 * <p>
	 * Strictly speaking the actual spacing between each station and its
	 * nearest neighbours should be used (Barnes used the 6 nearest
	 * neighbours), but the routine below uses the simpler approach of
	 * assuming a random spacing of the stations, and calculates the random
	 * data spacing via a simple formula.
	 * <p>
	 * 
	 * @param fLonMin :
	 *                Minimum Longitude of analysis area (eg 140.0 = 140.0
	 *                E)
	 * @param fLatMin :
	 *                Minimum Latitude of analysis area (eg -40.0 = 40.0 S)
	 * @param fLonMax :
	 *                Maximum Longitude of analysis area (eg 150.0 = 150.0
	 *                E)
	 * @param fLatMax :
	 *                Maximum Latitude of analysis area (eg -30.0 = 30.0 S)
	 * @param faa3DData :
	 *                An array (3,ndata) where ndata is the number of data
	 *                points. Each row of data should contain a longitude,
	 *                a latitude, and a value to be interpolated.
	 * @return AnalysisParameters : see inner class for description of
	 *         member variables
	 * @author James Kelly j.kelly@bom.gov.au
	 * @author Robert Dahni rrd@bom.gov.au 30/01/97 Original Code (IDL)
	 * 
	 */

	public static synchronized AnalysisParameters getRecommendedParameters(
		float fLonMin, float fLatMin, float fLonMax, float fLatMax,
		float[][] faa3DData)
	{

		double dKmx = 0.0d;
		double dKmy = 0.0d;
		double[] daKm = new double[2];
		double randomDataSpacing = 0.0d;
		double scaleLengthDeg = 0.0;

		double degreesX = (double)(fLonMax - fLonMin);
		double degreesY = (double)(fLatMax - fLatMin);

		//
		// determine average station spacing
		//
		daKm = deg2km(fLonMin, fLatMin, fLonMax, fLatMax);
		dKmx = Math.abs(daKm[0]);
		dKmy = Math.abs(daKm[1]);

		int sizeOfUniqueData = faa3DData[1].length;

		double degreesPerKmY = degreesY / dKmy;
		double degreesPerKmX = degreesX / dKmx;

		randomDataSpacing =
			Math.sqrt(dKmx * dKmy)
				* ((1.0d + Math.sqrt(sizeOfUniqueData)) / (sizeOfUniqueData - 1.0d));
		scaleLengthDeg = randomDataSpacing * degreesPerKmX;
		double gridSpace = randomDataSpacing * 0.3d;

		// round to nearest 0.01 deg
		double gridSpaceDegX =
			(Math.round(gridSpace * degreesPerKmX * 100.0d)) / 100.0d;
		double gridX = gridSpaceDegX;

		// round to nearest 0.01 deg
		double gridSpaceDegY =
			(Math.round(gridSpace * degreesPerKmY * 100.0d)) / 100.0d;
		double gridY = gridSpaceDegY;

		double scaleLengthGU = scaleLengthDeg / gridX;
		/*
		 * // later we might consider calculating the
		 * actualStationSpacing // this then lets us calculate the
		 * "Non-Uniformity Ratio" // ... double actualStationSpacing =
		 * stationSpacing(faa3DData) NUR =
		 * (randomDataSpacing-actualStationSpacing)/
		 * actualStationSpacing if (NUR lt 0.0) then NUR = 0.0
		 */

		// ap.actualStationSpacing = actualStationSpacing;
		// ap.NUR = NUR;
		float[] faLonGrid =
			getRecommendedGridX(fLonMin, fLonMax, (float)gridX);
		float[] faLatGrid =
			getRecommendedGridY(fLatMin, fLatMax, (float)gridY);

		AnalysisParameters ap =
			new AnalysisParameters(
				gridX, gridY, scaleLengthGU, randomDataSpacing,
				faLonGrid, faLatGrid);

		return ap;
	}

	/**
	 * Calculate the number of kms between 2 locations specified by
	 * longitude and latitude: (fLon1, fLat1) and (fLon2, fLat2)
	 * 
	 * @param fLon1 :
	 *                longitude of first point
	 * @param fLat1 :
	 *                latitude of first point
	 * @param fLon2 :
	 *                longitude of second point
	 * @param fLat2 :
	 *                latitude of second point
	 * 
	 * The return value is provided as a 2 element array, 
	 * giving km in the x direction and km in the y direction 
	 * between the 2 points:
	 * @return double[] where double[0] = km in x direction (dKmx) and
	 *         double[1] = km in y direction (dKmy)
	 */

	public static double[] deg2km(float fLon1, float fLat1, float fLon2,
		float fLat2)
	{
		double dAvgLat =
			(double)((fLat1 + fLat2)) / 2.0d * Math.PI / 180.0d;
		double dKmx =
			(double)((fLon2 - fLon1))
				/ 1000.0d
				* ((111415.1d * Math.cos(dAvgLat))
					- (94.54999d * Math.cos(3.0d * dAvgLat)) - (0.12d * Math
					.cos(5.0d * dAvgLat)));
		double dKmy =
			(double)((fLat2 - fLat1))
				/ 1000.0d
				* (111132.1d
					- (566.05d * Math.cos(2.0d * dAvgLat))
					+ (1.2d * Math.cos(4.0d * dAvgLat)) - (0.003d * Math
					.cos(6.0d * dAvgLat)));
		double[] daKm = new double[2];
		daKm[0] = dKmx;
		daKm[1] = dKmy;

		// The OSA analysis has been changed to use a coordinate
		// system based on meters rather than degrees 
		// latitude/longitude.
		// To get values in kms, just divide by 1000
		daKm[0] = (fLon2 - fLon1) / 1000.0;
		daKm[1] = (fLat2 - fLat1) / 1000.0;

		AnalysisParameters ap = new AnalysisParameters(
			0.0d, 0.0d, 0.0d, 0.0d);
		return daKm;
	}

	/**
	 * Using a model field as a first guess presents a problem if the model
	 * does not completely cover the area of the analysis. Parts of the
	 * first guess will contain NaN. This method allows NaN values to be
	 * filtered out and replaced by another value
	 * 
	 * @param array
	 *                The array of values, possibly containing NaN
	 * @param nonNaN
	 *                The replacement value for NaN
	 * @returns the array of values, with NaN replaced by nonNaN
	 */
	private static float[][] filterNaN(float[][] array, float nonNaN)
	{
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[0].length; j++) {
				Float val = new Float(
					array[i][j]);
				if (val.isNaN()) {
					array[i][j] = nonNaN;
				}
			}
		}

		return array;
	}

	private synchronized static float[][][] setupRadii(int xSize,
		int ySize, float[] faLonValues, float[] faLatValues,
		double scaleLength2)
	{
                if (!useRadii) return null;
		int numSamples = faLonValues.length;
		float[][][] radii = new float[xSize][ySize][numSamples];

		for (int i = 0; i < xSize; i++) {
			for (int j = 0; j < ySize; j++) {
				for (int k = 0; k < numSamples; k++) {
					float fx =
						faLonValues[k]
							- (float)i;
					float fy =
						faLatValues[k]
							- (float)j;
					radii[i][j][k] = fx*fx + fy*fy;
				}
			}
		}

		return radii;
	}

	/**
	 * Name: scinex
	 * 
	 * PURPOSE: This function returns the value scint of a scalar field at
	 * a point gm,gn by interpolation or extrapolation of the field scala
	 * (Lagrangian cubic formula).
	 * 
	 * CALLING SEQUENCE: scint = scinex(gm,gn,scala)
	 * 
	 * @param gm:
	 *                The x-coordinate.
	 * @param gn:
	 *                The y-coordinate.
	 * @param scala:
	 *                A 2-d array (scalar field).
	 * @return double: The interpolated data value (scint)
	 * 
	 * MODIFICATION HISTORY: written by: Robert Dahni rrd@bom.gov.au
	 * 04/01/95 (IDL version) James Kelly J.Kelly@bom.gov.au Jan 2001 (Java
	 * version)
	 */
	public static float scinex(float gm, float gn, float[][] scala)
	{
		int msize = scala.length;
		int nsize = scala[0].length;
		int mmax = msize;
		int nmax = nsize;
		int mmin = 1;
		int nmin = 1;
		double dgm = Math.floor(gm);
		double dgn = Math.floor(gn);

		int igm = (int)dgm;
		int jgn = (int)dgn;
		float fm = gm - (float)igm;
		float fn = gn - (float)jgn;
		if (fm < 1.e-06) {
			fm = 0.0f;
		}
		if (fn < 1.e-06) {
			fn = 0.0f;
		}
		int ms = mmax - 1;
		int ns = nmax - 1;
		int mr = mmin + 1;
		int nr = nmin + 1;

		float e = 0.0f;
		float t1 = 0.0f;
		float t2 = 0.0f;
		float p = 0.0f;
		float h = 0.0f;
		float scinto = 0.0f;

		if (gm >= mmax) {
			if (gn >= nmax) {
				e = gm - (float)mmax;
				t1 =
					e
						* (scala[mmax - 1][nmax - 1] - scala[ms - 1][nmax - 1]);
				e = gn - (float)nmax;
				t2 =
					e
						* (scala[mmax - 1][nmax - 1] - scala[mmax - 1][ns - 1]);
				scinto = scala[mmax - 1][nmax - 1] + t1 + t2;
				return scinto;
			} else if (gn < nmin) {
				e = gm - (float)mmax;
				t1 =
					e
						* (scala[mmax - 1][nmin - 1] - scala[ms - 1][nmin - 1]);
				e = (float)nmin - gn;
				t2 =
					e
						* (scala[mmax - 1][nmin - 1] - scala[mmax - 1][nr - 1]);
				scinto = scala[mmax - 1][nmin - 1] + t1 + t2;
				return scinto;
			} else {
				p =
					scala[mmax - 1][jgn - 1]
						+ fn
						* (scala[mmax - 1][jgn] - scala[mmax - 1][jgn - 1]);
				h =
					scala[ms - 1][jgn - 1]
						+ fn
						* (scala[ms - 1][jgn] - scala[ms - 1][jgn - 1]);
				e = gm - (float)mmax;
				scinto = p + e * (p - h);
				return scinto;
			}
		} else if (gm < mmin) {
			if (gn >= nmax) {
				e = gn - (float)nmax;
				t2 =
					e
						* (scala[mmin - 1][nmax - 1] - scala[mmin - 1][ns - 1]);
				e = (float)mmin - gm;
				t1 =
					e
						* (scala[mmin - 1][nmax - 1] - scala[mr - 1][nmax - 1]);
				scinto = scala[mmin - 1][nmax - 1] + t1 + t2;
				return scinto;
			} else if (gn < nmin) {
				e = (float)nmin - gn;
				t2 =
					e
						* (scala[mmin - 1][nmin - 1] - scala[mmin - 1][nr - 1]);
				e = (float)mmin - gm;
				t1 =
					e
						* (scala[mmin - 1][nmin - 1] - scala[mr - 1][nmin - 1]);
				scinto = scala[mmin - 1][nmin - 1] + t1 + t2;
				return scinto;
			} else {
				e = (float)mmin - gm;
				p =
					scala[mmin - 1][jgn - 1]
						+ fn
						* (scala[mmin - 1][jgn] - scala[mmin - 1][jgn - 1]);
				h =
					scala[mr - 1][jgn - 1]
						+ fn
						* (scala[mr - 1][jgn] - scala[mr - 1][jgn - 1]);
				scinto = p - e * (h - p);
				return scinto;
			}
		} else if (gn >= nmax) {
			e = gn - (float)nmax;
			p =
				scala[igm - 1][nmax - 1]
					+ fm
					* (scala[igm][nmax - 1] - scala[igm - 1][nmax - 1]);
			h =
				scala[igm - 1][ns - 1]
					+ fm
					* (scala[igm][ns - 1] - scala[igm - 1][ns - 1]);
			scinto = p + e * (p - h);
			return scinto;
		} else if (gn < nmin) {
			e = (float)nmin - gn;
			p =
				scala[igm - 1][nmin - 1]
					+ fm
					* (scala[igm][nmin - 1] - scala[igm - 1][nmin - 1]);
			h =
				scala[igm - 1][nr - 1]
					+ fm
					* (scala[igm][nr - 1] - scala[igm - 1][nr - 1]);
			scinto = p - e * (h - p);
			return scinto;
		} else if ((gm >= ms) || (gm < mr) || (gn >= ns) || (gn < nr)) {
			p =
				scala[igm][jgn - 1]
					+ fn
					* (scala[igm][jgn] - scala[igm][jgn - 1]);
			h =
				scala[igm - 1][jgn - 1]
					+ fn
					* (scala[igm - 1][jgn] - scala[igm - 1][jgn - 1]);
			scinto = h + fm * (p - h);
			return scinto;
		} else {
			float s1 = fm + 1.0f;
			float s2 = fm;
			float s3 = fm - 1.0f;
			float s4 = fm - 2.0f;
			float s12 = s1 * s2;
			float s34 = s3 * s4;
			float a = -s2 * s34;
			float b = 3.0f * s1 * s34;
			float c = -3.0f * s12 * s4;
			float d = s12 * s3;
			float x1 =
				a * scala[igm - 2][jgn - 2] + b
					* scala[igm - 1][jgn - 2] + c
					* scala[igm][jgn - 2] + d
					* scala[igm + 1][jgn - 2];
			float x2 =
				a * scala[igm - 2][jgn - 1] + b
					* scala[igm - 1][jgn - 1] + c
					* scala[igm][jgn - 1] + d
					* scala[igm + 1][jgn - 1];
			float x3 =
				a * scala[igm - 2][jgn] + b
					* scala[igm - 1][jgn] + c
					* scala[igm][jgn] + d
					* scala[igm + 1][jgn];
			float x4 =
				a * scala[igm - 2][jgn + 1] + b
					* scala[igm - 1][jgn + 1] + c
					* scala[igm][jgn + 1] + d
					* scala[igm + 1][jgn + 1];
			s1 = fn + 1.0f;
			s2 = fn;
			s3 = fn - 1.0f;
			s4 = fn - 2.0f;
			s12 = s1 * s2;
			s34 = s3 * s4;
			a = -s2 * s34;
			b = 3.0f * s1 * s34;
			c = -3.0f * s12 * s4;
			d = s12 * s3;
			float y = a * x1 + b * x2 + c * x3 + d * x4;
			scinto = y / 36.0f;
			return scinto;
		}
	}
}
